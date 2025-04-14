from unittest.mock import patch, MagicMock

import networkx as nx

import matching.glema.data.process.import_dataset as import_dataset
import matching.misc.cpg_const as cpg_const


class TestImportDataset:
    def test_empty_node_data( self ):
        """Test when node_data has no design pattern labels."""
        node_data = { }
        result = import_dataset.get_design_pattern_types( node_data )
        assert result == [ ]

    def test_single_design_pattern( self ):
        """Test when node_data has a single design pattern label."""
        pattern_type = list( cpg_const.DesignPatternType )[ 0 ]  # Get first pattern type
        node_data = { f"labels_{pattern_type.value}": True }

        result = import_dataset.get_design_pattern_types( node_data )
        assert result == [ pattern_type.value ]

    def test_multiple_design_patterns( self ):
        """Test when node_data has multiple design pattern labels."""
        pattern_types = list( cpg_const.DesignPatternType )[ :2 ]  # Get first two pattern types
        node_data = {
            f"labels_{pattern_types[ 0 ].value}": True,
            f"labels_{pattern_types[ 1 ].value}": True
        }

        result = import_dataset.get_design_pattern_types( node_data )
        assert sorted( result ) == sorted( [ pt.value for pt in pattern_types ] )

    def test_empty_node_data( self ):
        """Test when node_data has no record labels."""
        node_data = { }
        result = import_dataset.get_record_label( node_data )
        assert result is None

    def test_single_label( self ):
        """Test when node_data has a single record label."""
        label = list( cpg_const.NodeLabel )[ 0 ]  # Get first node label
        node_data = { f"labels_{label.value}": True }

        result = import_dataset.get_record_label( node_data )
        assert result == label

    @patch( 'matching.glema.data.process.import_dataset.misc_utils.get_enum_idx' )
    def test_add_graph_to( self, mock_get_enum_idx ):
        """Test add_graph_to with multiple nodes and edges."""
        mock_get_enum_idx.return_value = 1

        # Create source and target graphs
        G_source = nx.DiGraph()
        G_target = nx.DiGraph()

        # Add nodes to source graph
        node_data1 = {
            f"labels_{cpg_const.NodeLabel.RECORD.value}": True,
            cpg_const.NodeAttr.SCOPED_RECORD_NAME.value: "scope1",
            cpg_const.NodeAttr.DATASET.value: "dataset1"
        }
        node_data2 = {
            f"labels_{cpg_const.NodeLabel.RECORD.value}": True,
            cpg_const.NodeAttr.SCOPED_RECORD_NAME.value: "scope2",
            cpg_const.NodeAttr.DATASET.value: "dataset2",
            cpg_const.NodeAttr.PATTERN_ID_NAME.value: "pattern1"
        }
        G_source.add_node( "node1", **node_data1 )
        G_source.add_node( "node2", **node_data2 )

        # Add edges in source graph
        G_source.add_edge( "node1", "node2" )

        # Initialize mappings
        node_id_mapping = { }
        anchor_mapping = { }

        # Call the function
        import_dataset.add_graph_to( G_source, G_target, node_id_mapping, anchor_mapping, "node1", 1 )

        # Verify results
        assert len( G_target.nodes ) == 2
        assert len( node_id_mapping ) == 2
        assert "1::node1" in node_id_mapping
        assert "1::node2" in node_id_mapping
        assert 1 in anchor_mapping
        assert anchor_mapping[ 1 ] == node_id_mapping[ "1::node1" ]
        assert len( G_target.edges ) == 1
        assert G_target.has_edge( node_id_mapping[ "1::node1" ], node_id_mapping[ "1::node2" ] )

    @patch( 'matching.glema.data.process.import_dataset.nx.ego_graph' )
    def test_get_k_neighbourhood( self, mock_ego_graph ):
        """Test get_k_neighbourhood with a graph that meets size requirements on first try."""
        # Create a mock graph
        G = nx.DiGraph()
        mock_subgraph = MagicMock()

        # Configure mock to return a subgraph with acceptable size
        mock_ego_graph.return_value = mock_subgraph
        mock_subgraph.number_of_nodes.return_value = 10  # Between min=2 and max=50

        # Call the function
        result = import_dataset.get_k_neighbourhood( G, "anchor_node", k=2, min_n=2, max_n=50 )

        # Verify results
        mock_ego_graph.assert_called_once_with( G, "anchor_node", radius=2, undirected=True )
        assert isinstance( result, nx.DiGraph )

    @patch( 'matching.glema.data.process.import_dataset.nx.ego_graph' )
    def test_get_k_neighbourhood_too_large( self, mock_ego_graph ):
        """Test get_k_neighbourhood with a graph that is initially too large."""
        # Create a mock graph
        G = nx.DiGraph()

        # Create mock subgraphs for different radius values
        mock_large_subgraph = MagicMock()
        mock_large_subgraph.number_of_nodes.return_value = 60  # > max_n=50

        mock_acceptable_subgraph = MagicMock()
        mock_acceptable_subgraph.number_of_nodes.return_value = 30  # Between min=2 and max=50

        # Configure mock to return different subgraphs based on radius
        mock_ego_graph.side_effect = [ mock_large_subgraph, mock_acceptable_subgraph ]

        # Call the function
        result = import_dataset.get_k_neighbourhood( G, "anchor_node", k=2, min_n=2, max_n=50 )

        # Verify results
        assert mock_ego_graph.call_count == 2
        mock_ego_graph.assert_any_call( G, "anchor_node", radius=2, undirected=True )
        mock_ego_graph.assert_any_call( G, "anchor_node", radius=1, undirected=True )
        assert isinstance( result, nx.DiGraph )

    @patch( 'matching.glema.data.process.import_dataset.nx.ego_graph' )
    def test_get_k_neighbourhood_too_small( self, mock_ego_graph ):
        """Test get_k_neighbourhood with a graph that is initially too small."""
        # Create a mock graph
        G = nx.DiGraph()

        # Create mock subgraphs for different radius values
        mock_small_subgraph = MagicMock()
        mock_small_subgraph.number_of_nodes.return_value = 1  # < min_n=2

        mock_acceptable_subgraph = MagicMock()
        mock_acceptable_subgraph.number_of_nodes.return_value = 5  # Between min=2 and max=50

        # Configure mock to return different subgraphs based on radius
        mock_ego_graph.side_effect = [ mock_small_subgraph, mock_acceptable_subgraph ]

        # Call the function
        result = import_dataset.get_k_neighbourhood( G, "anchor_node", k=2, min_n=2, max_n=50 )

        # Verify results
        assert mock_ego_graph.call_count == 2
        mock_ego_graph.assert_any_call( G, "anchor_node", radius=2, undirected=True )
        mock_ego_graph.assert_any_call( G, "anchor_node", radius=3, undirected=True )
        assert isinstance( result, nx.DiGraph )
