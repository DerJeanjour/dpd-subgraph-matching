from unittest.mock import patch, MagicMock

import networkx as nx
import numpy as np
import pytest

import matching.glema.evaluation.evaluate_pattern_matching as epm
import matching.misc.cpg_const as cpg_const


class TestEvaluatePatternMatching:

    def test_get_source_to_pattern_instance_mapping( self ):
        # Prepare test data
        test_metas = [
            { "gidx": 1, "pattern_id": 100 },
            { "gidx": 2, "pattern_id": 200 },
            { "gidx": 3, "pattern_id": 300 }
        ]

        # Call the function
        result = epm.get_source_to_pattern_instance_mapping( test_metas )

        # Verify the result
        expected = { 1: 100, 2: 200, 3: 300 }
        assert result == expected

        # Test with empty input
        assert epm.get_source_to_pattern_instance_mapping( [ ] ) == { }

    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.misc_utils' )
    def test_to_numeric_labels( self, mock_misc_utils ):
        # Mock the enum functions
        mock_enum1 = MagicMock()
        mock_enum2 = MagicMock()

        mock_misc_utils.get_enum_by_value.side_effect = [ mock_enum1, mock_enum2 ]
        mock_misc_utils.get_enum_idx.side_effect = [ 1, 2 ]

        # Set up test data
        true_labels = [ cpg_const.NO_DESIGN_PATTERN, "PATTERN1" ]
        pred_labels = [ "PATTERN2", cpg_const.NO_DESIGN_PATTERN ]

        # Call the function
        x_labels, y_labels = epm.to_numeric_labels( true_labels, pred_labels )

        # Verify the results
        assert y_labels == [ 0, 2 ]
        assert x_labels == [ 1, 0 ]

        # Verify the mock calls
        mock_misc_utils.get_enum_by_value.assert_any_call( cpg_const.DesignPatternType, "PATTERN1" )
        mock_misc_utils.get_enum_by_value.assert_any_call( cpg_const.DesignPatternType, "PATTERN2" )
        mock_misc_utils.get_enum_idx.assert_any_call( mock_enum1 )
        mock_misc_utils.get_enum_idx.assert_any_call( mock_enum2 )

    def test_compute_metrics( self ):
        # Test binary classification
        x_labels = [ 0, 1, 0, 1 ]
        y_labels = [ 0, 1, 1, 0 ]
        metrics = epm.compute_metrics( x_labels, y_labels )

        assert "acc" in metrics
        assert "pre" in metrics
        assert "rec" in metrics
        assert "f1s" in metrics
        assert "roc" in metrics
        assert "avp" in metrics

        # Test multiclass classification
        x_labels = [ 0, 1, 2, 0 ]
        y_labels = [ 0, 1, 1, 2 ]
        metrics = epm.compute_metrics( x_labels, y_labels )

        assert "acc" in metrics
        assert "pre" in metrics
        assert "rec" in metrics
        assert "f1s" in metrics
        assert "roc" in metrics
        assert "avp" in metrics

    def test_compute_source_preds( self ):
        # Test data
        groups_by_source = {
            1: { "PATTERN1": [ 0, 1 ], "PATTERN2": [ 2 ] },
            2: { "PATTERN3": [ 3, 4 ] }
        }
        preds = [ 0.7, 0.8, 0.6, 0.9, 0.5 ]
        metas = [
            { "pred_w": 0.9 },
            { "pred_r": 0.6, "pred_w": 1.2 },
            { "pred_r": 0.8 },
            { "pred_w": 0.8 },
            { }
        ]

        # Test with default aggregator (mean)
        result = epm.compute_source_preds( groups_by_source, preds, metas )

        # Validate output structure
        assert isinstance( result, dict )
        assert set( result.keys() ) == { 1, 2 }
        assert set( result[ 1 ].keys() ) == { "PATTERN1", "PATTERN2" }
        assert set( result[ 2 ].keys() ) == { "PATTERN3" }

        # Verify calculations
        expected_pattern1 = (0.7 * 0.9 + 0.6 * 1.2) / 2
        expected_pattern2 = 0.8 * 1.0
        expected_pattern3 = (0.9 * 0.8 + 0.5 * 1.0) / 2

        assert abs( result[ 1 ][ "PATTERN1" ] - expected_pattern1 ) < 1e-6
        assert abs( result[ 1 ][ "PATTERN2" ] - expected_pattern2 ) < 1e-6
        assert abs( result[ 2 ][ "PATTERN3" ] - expected_pattern3 ) < 1e-6

        # Test with custom aggregator
        def max_aggregator( vals, **kwargs ):
            return max( vals )

        result_max = epm.compute_source_preds( groups_by_source, preds, metas, pred_aggregator=max_aggregator )

        # Verify max aggregation
        assert abs( result_max[ 1 ][ "PATTERN1" ] - max( 0.7 * 0.9, 0.6 * 1.2 ) ) < 1e-6
        assert abs( result_max[ 1 ][ "PATTERN2" ] - 0.8 ) < 1e-6
        assert abs( result_max[ 2 ][ "PATTERN3" ] - max( 0.9 * 0.8, 0.5 ) ) < 1e-6

    def test_compute_source_types( self ):
        # Test data
        groups_by_source = {
            1: { "PATTERN1": [ 0, 1 ], "PATTERN2": [ 2 ] },
            2: { "PATTERN3": [ 3 ] }
        }
        metas = [
            { "source_type": "TYPE_A" },
            { "source_type": "TYPE_A" },
            { "source_type": "TYPE_A" },
            { "source_type": "TYPE_B" }
        ]

        # Call the function
        result = epm.compute_source_types( groups_by_source, metas )

        # Verify the result
        expected = { 1: "TYPE_A", 2: "TYPE_B" }
        assert result == expected

        # Test with empty input
        assert epm.compute_source_types( { }, [ ] ) == { }

    def test_compute_labels_legacy( self ):
        # Test data
        source_types = { 1: "TYPE_A", 2: "TYPE_B", 3: "TYPE_C" }
        source_preds = {
            1: { "PATTERN1": 0.8, "PATTERN2": 0.3 },
            2: { "PATTERN1": 0.2, "PATTERN2": 0.7 },
            3: { "PATTERN1": 0.4, "PATTERN2": 0.6 }
        }

        # Call with default parameters (conf=0.5, top_k=1)
        true_labels, pred_labels, pred_scores = epm.compute_labels_legacy( source_types, source_preds )

        # Verify results
        assert true_labels == [ "TYPE_A", "TYPE_B", "TYPE_C" ]
        assert pred_labels == [ "PATTERN1", "PATTERN2", "PATTERN2" ]
        assert pred_scores == [ 0.8, 0.7, 0.6 ]

        # Test with higher confidence threshold
        true_labels, pred_labels, pred_scores = epm.compute_labels_legacy( source_types, source_preds, conf=0.7 )

        # Verify that only predictions above conf threshold are considered as pattern
        assert true_labels == [ "TYPE_A", "TYPE_B", "TYPE_C" ]
        assert pred_labels == [ "PATTERN1", cpg_const.NO_DESIGN_PATTERN, cpg_const.NO_DESIGN_PATTERN ]
        assert pred_scores == [ 0.8, 0.7, 0.6 ]

        # Test with top_k=2
        true_labels, pred_labels, pred_scores = epm.compute_labels_legacy( source_types, source_preds, top_k=2 )

        # Should return 2 predictions for each source
        assert len( true_labels ) == 6
        assert len( pred_labels ) == 6
        assert len( pred_scores ) == 6

        # First two are for source 1, next two for source 2, last two for source 3
        assert true_labels[ :2 ] == [ "TYPE_A", "TYPE_A" ]
        assert true_labels[ 2:4 ] == [ "TYPE_B", "TYPE_B" ]
        assert true_labels[ 4: ] == [ "TYPE_C", "TYPE_C" ]

    def test_aggregate_preds_mean( self ):
        # Test with a list of positive values
        preds = [ 0.1, 0.5, 0.9 ]
        result = epm.aggregate_preds_mean( preds )
        assert result == 0.5
        assert isinstance( result, float )

        # Test with a mix of positive and negative values
        preds = [ -0.5, 0.0, 0.5 ]
        result = epm.aggregate_preds_mean( preds )
        assert result == 0.0

        # Test with a single value
        result = epm.aggregate_preds_mean( [ 0.7 ] )
        assert result == 0.7

        # Test with empty list (should handle numpy's warning about empty array)
        with patch( 'numpy.mean', return_value=np.nan ):
            result = epm.aggregate_preds_mean( [ ] )
            assert np.isnan( result )

    def test_aggregate_preds_max( self ):
        # Test with a list of positive values
        preds = [ 0.1, 0.5, 0.9 ]
        result = epm.aggregate_preds_max( preds )
        assert result == 0.9
        assert isinstance( result, float )

        # Test with a mix of positive and negative values
        preds = [ -0.5, 0.0, 0.5 ]
        result = epm.aggregate_preds_max( preds )
        assert result == 0.5

        # Test with a single value
        result = epm.aggregate_preds_max( [ 0.7 ] )
        assert result == 0.7

        # Test with empty list (should raise ValueError)
        with pytest.raises( ValueError ):
            epm.aggregate_preds_max( [ ] )

    def test_aggregate_preds_by_quantile( self ):
        # Test with default quantile (q=0.8)
        preds = [ 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 ]
        result = epm.aggregate_preds_by_quantile( preds )
        assert abs( result - 0.81 ) < 0.1
        assert isinstance( result, float )

        # Test with custom quantile
        result = epm.aggregate_preds_by_quantile( preds, q=0.5 )
        assert abs( result - 0.55 ) < 0.1

        # Test with extreme quantiles
        result = epm.aggregate_preds_by_quantile( preds, q=0.0 )
        assert abs( result - 0.1 ) < 0.1

        result = epm.aggregate_preds_by_quantile( preds, q=1.0 )
        assert abs( result - 1.0 ) < 0.1

        # Test with a single value
        result = epm.aggregate_preds_by_quantile( [ 0.7 ], q=0.5 )
        assert abs( result - 0.7 ) < 0.1

        # Test with empty list (should raise IndexError or ValueError)
        with pytest.raises( IndexError ):
            epm.aggregate_preds_by_quantile( [ ] )

    def test_group_by_source( self ):
        # Prepare test data
        test_metas = [
            { "gidx": 1, "pattern_type": "PATTERN1" },
            { "gidx": 1, "pattern_type": "PATTERN2" },
            { "gidx": 2, "pattern_type": "PATTERN1" },
            { "gidx": 3, "pattern_type": "PATTERN3" },
            { "gidx": 1, "pattern_type": "PATTERN1" }
        ]

        # Call the function
        result = epm.group_by_source( test_metas )

        # Verify the result
        expected = {
            1: {
                "PATTERN1": [ 0, 4 ],
                "PATTERN2": [ 1 ]
            },
            2: {
                "PATTERN1": [ 2 ]
            },
            3: {
                "PATTERN3": [ 3 ]
            }
        }
        assert result == expected

        # Test with empty input
        assert epm.group_by_source( [ ] ) == { }

    def test_group_by_pattern_instance( self ):
        # Prepare test data
        test_metas = [
            { "pattern_id": 100, "pattern_type": "PATTERN1" },
            { "pattern_id": 100, "pattern_type": "PATTERN2" },
            { "pattern_id": 200, "pattern_type": "PATTERN1" },
            { "pattern_id": 300, "pattern_type": "PATTERN3" },
            { "pattern_id": 100, "pattern_type": "PATTERN1" }
        ]

        # Call the function
        result = epm.group_by_pattern_instance( test_metas )

        # Verify the result
        expected = {
            100: {
                "PATTERN1": [ 0, 4 ],
                "PATTERN2": [ 1 ]
            },
            200: {
                "PATTERN1": [ 2 ]
            },
            300: {
                "PATTERN3": [ 3 ]
            }
        }
        assert result == expected

        # Test with empty input
        assert epm.group_by_pattern_instance( [ ] ) == { }

    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.tqdm' )
    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.graph_utils' )
    def test_normalize_patterns( self, mock_graph_utils, mock_tqdm ):
        # Setup mock behavior
        mock_graph_utils.normalize_graph.side_effect = lambda g, max_distance: (
            MagicMock( number_of_nodes=lambda: len( g ) ), 0)
        mock_tqdm.side_effect = lambda x: x

        # Test data
        patterns_all = {
            "PATTERN1": [ "graph1", "graph2", "graph3" ],
            "PATTERN2": [ "graph4", "graph5" ]
        }

        # Test with default parameters
        result = epm.normalize_patterns( patterns_all )

        # Verify normalize_graph was called for each graph
        assert mock_graph_utils.normalize_graph.call_count == 5

        # Verify result structure
        assert set( result.keys() ) == { "PATTERN1", "PATTERN2" }
        assert len( result[ "PATTERN1" ] ) == 3
        assert len( result[ "PATTERN2" ] ) == 2

        # Test with min_nodes filter
        mock_graph_utils.normalize_graph.reset_mock()
        mock_graph_utils.normalize_graph.side_effect = lambda g, max_distance: (
            MagicMock( number_of_nodes=lambda: len( g ) ), 0)

        result = epm.normalize_patterns( patterns_all, min_nodes=5 )

        # All calls should still happen, but filtering occurs after
        assert mock_graph_utils.normalize_graph.call_count == 5

        # Verify custom max_distance parameter
        mock_graph_utils.normalize_graph.reset_mock()
        epm.normalize_patterns( patterns_all, max_distance=10 )

        # Check max_distance was passed correctly
        for call in mock_graph_utils.normalize_graph.call_args_list:
            assert call[ 1 ][ 'max_distance' ] == 10

    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.tqdm' )
    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.graph_utils' )
    def test_normalize_sources( self, mock_graph_utils, mock_tqdm ):
        # Setup mock behavior
        mock_graph_utils.normalize_graph.side_effect = lambda g, max_distance: (
            MagicMock( number_of_nodes=lambda: 5 if g == "small" else 10 ), 0)
        mock_tqdm.side_effect = lambda x: x

        # Test data
        sources = {
            1: "graph1",
            2: "small",
            3: "graph3"
        }

        # Test with default parameters
        result = epm.normalize_sources( sources )

        # Verify normalize_graph was called for each graph
        assert mock_graph_utils.normalize_graph.call_count == 3

        # Verify result structure
        assert set( result.keys() ) == { 1, 2, 3 }
        assert len( result ) == 3

        # Test with min_nodes filter
        mock_graph_utils.normalize_graph.reset_mock()
        result = epm.normalize_sources( sources, min_nodes=7 )

        # All calls should still happen, but filtering occurs after
        assert mock_graph_utils.normalize_graph.call_count == 3

        # Small graph should be filtered out
        assert set( result.keys() ) == { 1, 3 }
        assert len( result ) == 2

        # Verify custom max_distance parameter
        mock_graph_utils.normalize_graph.reset_mock()
        epm.normalize_sources( sources, max_distance=10 )

        # Check max_distance was passed correctly
        for call in mock_graph_utils.normalize_graph.call_args_list:
            assert call[ 1 ][ 'max_distance' ] == 10

    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.tqdm' )
    def test_filter_sources( self, mock_tqdm ):
        # Setup mock behavior
        mock_tqdm.side_effect = lambda x: x

        # Test data
        sources = {
            1: "graph1",
            2: "graph2",
            3: "graph3",
            4: "graph4",
            5: "graph5"
        }
        source_patterns = {
            1: "PATTERN1",
            2: "PATTERN1",
            3: "PATTERN2",
            4: cpg_const.NO_DESIGN_PATTERN,
            5: cpg_const.NO_DESIGN_PATTERN
        }

        # Test with default parameters (no filtering)
        result = epm.filter_sources( sources, source_patterns )

        # Should return all sources
        assert set( result.keys() ) == { 1, 2, 3, 4, 5 }
        assert len( result ) == 5

        # Test with max_sources_per_pattern
        result = epm.filter_sources( sources, source_patterns, max_sources_per_pattern=1 )

        # Should only include first source of each pattern
        assert set( result.keys() ) == { 1, 3, 4 }
        assert len( result ) == 3

        # Test with max_na_patterns
        result = epm.filter_sources( sources, source_patterns, max_na_patterns=1 )

        # Should limit NO_DESIGN_PATTERN sources
        assert set( result.keys() ) == { 1, 2, 3, 4 }
        assert len( result ) == 4

        # Test with both filters
        result = epm.filter_sources( sources, source_patterns,
                                     max_sources_per_pattern=1,
                                     max_na_patterns=1 )

        # Should apply both limits
        assert set( result.keys() ) == { 1, 3, 4 }
        assert len( result ) == 3

    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.tqdm' )
    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.graph_utils' )
    def test_get_common_patterns_for_type( self, mock_graph_utils, mock_tqdm ):
        # Setup mocks
        mock_tqdm.side_effect = lambda x: x

        # Create mock graphs
        mock_graph1 = MagicMock( spec=nx.Graph )
        mock_graph2 = MagicMock( spec=nx.Graph )
        mock_graph3 = MagicMock( spec=nx.Graph )

        # Mock node count
        mock_graph1.number_of_nodes.return_value = 10
        mock_graph2.number_of_nodes.return_value = 12
        mock_graph3.number_of_nodes.return_value = 8

        # Mock graph intersection to return a new graph
        mock_intersection = MagicMock( spec=nx.Graph )
        mock_intersection.number_of_nodes.return_value = 9
        mock_graph_utils.get_norm_graph_intersection.return_value = mock_intersection

        # Mock graph equality to avoid duplicates
        mock_graph_utils.norm_graphs_are_equal.return_value = False

        # Test with inputs below max_graphs
        patterns = [ mock_graph1, mock_graph2 ]
        result = epm.get_common_patterns_for_type( patterns, min_nodes=5, max_graphs=3 )

        # Should return original patterns
        assert result == patterns

        # Test with inputs above max_graphs, requiring iterations
        patterns = [ mock_graph1, mock_graph2, mock_graph3, MagicMock( spec=nx.Graph ) ]
        result = epm.get_common_patterns_for_type( patterns, min_nodes=5, max_graphs=2, max_iter=3 )

        # Should call graph_utils.get_norm_graph_intersection for pairs
        assert mock_graph_utils.get_norm_graph_intersection.called

        # Test when intersection graphs don't meet min_nodes
        mock_graph_utils.get_norm_graph_intersection.return_value = MagicMock( spec=nx.Graph )
        mock_graph_utils.get_norm_graph_intersection.return_value.number_of_nodes.return_value = 4

        patterns = [ mock_graph1, mock_graph2, mock_graph3 ]
        result = epm.get_common_patterns_for_type( patterns, min_nodes=5, max_graphs=2, max_iter=1 )

        # Should return original patterns as fallback
        assert len( result ) > 0

    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.tqdm' )
    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.graph_utils' )
    @patch( 'matching.glema.evaluation.evaluate_pattern_matching.get_common_patterns_for_type' )
    def test_get_common_patterns( self, mock_get_common_patterns_for_type, mock_graph_utils, mock_tqdm ):
        # Setup mocks
        mock_tqdm.side_effect = lambda x: x

        # Create mock graphs and normalized graphs
        mock_graph1 = MagicMock( spec=nx.Graph )
        mock_graph2 = MagicMock( spec=nx.Graph )
        mock_normalized1 = MagicMock( spec=nx.Graph )
        mock_normalized2 = MagicMock( spec=nx.Graph )

        # Setup normalize_graph to return normalized graphs
        mock_graph_utils.normalize_graph.side_effect = [
            (mock_normalized1, 0),
            (mock_normalized2, 0)
        ]

        # Setup get_common_patterns_for_type mock
        mock_common_patterns = [ MagicMock( spec=nx.Graph ) ]
        mock_get_common_patterns_for_type.return_value = mock_common_patterns

        # Test input
        patterns_by_type = {
            "PATTERN1": [ mock_graph1 ],
            "PATTERN2": [ mock_graph2 ]
        }

        # Run the function
        result = epm.get_common_patterns(
            patterns_by_type,
            min_nodes=6,
            max_node_distance=5,
            max_graphs=3,
            max_iter=2
        )

        # Verify normalize_graph was called with correct parameters
        mock_graph_utils.normalize_graph.assert_any_call( mock_graph1, max_distance=5 )
        mock_graph_utils.normalize_graph.assert_any_call( mock_graph2, max_distance=5 )

        # Verify get_common_patterns_for_type was called for each pattern type
        assert mock_get_common_patterns_for_type.call_count == 2
        mock_get_common_patterns_for_type.assert_any_call( [ mock_normalized1 ], 6, 3, 2 )
        mock_get_common_patterns_for_type.assert_any_call( [ mock_normalized2 ], 6, 3, 2 )

        # Verify result structure
        assert "PATTERN1" in result
        assert "PATTERN2" in result
        assert result[ "PATTERN1" ] == mock_common_patterns
        assert result[ "PATTERN2" ] == mock_common_patterns
