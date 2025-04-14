from unittest.mock import patch

import networkx as nx
import pytest

import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.data.process.process_data as process_data


def test_mark_anchors_without_mappings():
    # Create test graphs
    g1 = nx.Graph()
    g1.add_nodes_from( [ (0, { 'label': 0 }), (1, { 'label': 1 }), (2, { 'label': 2 }) ] )
    g1.add_edges_from( [ (0, 1), (1, 2) ] )

    g2 = nx.Graph()
    g2.add_nodes_from( [ (0, { 'label': 0 }), (1, { 'label': 1 }), (2, { 'label': 2 }) ] )
    g2.add_edges_from( [ (0, 1), (1, 2) ] )

    graphs = { 0: g1, 1: g2 }
    source_anchor = 1

    # Test marking anchors without mappings
    process_data.mark_anchors( graphs, source_anchor )

    # Verify anchors are correctly marked
    assert graphs[ 0 ].nodes[ 1 ][ 'anchor' ] == 1
    assert graphs[ 0 ].nodes[ 0 ][ 'anchor' ] == 0
    assert graphs[ 0 ].nodes[ 2 ][ 'anchor' ] == 0

    assert graphs[ 1 ].nodes[ 1 ][ 'anchor' ] == 1
    assert graphs[ 1 ].nodes[ 0 ][ 'anchor' ] == 0
    assert graphs[ 1 ].nodes[ 2 ][ 'anchor' ] == 0


def test_mark_anchors_with_mappings():
    # Create test graphs
    g1 = nx.Graph()
    g1.add_nodes_from( [ (0, { 'label': 0 }), (1, { 'label': 1 }), (2, { 'label': 2 }) ] )
    g1.add_edges_from( [ (0, 1), (1, 2) ] )

    g2 = nx.Graph()
    g2.add_nodes_from( [ (0, { 'label': 0 }), (1, { 'label': 1 }), (2, { 'label': 2 }) ] )
    g2.add_edges_from( [ (0, 1), (1, 2) ] )

    graphs = { 0: g1, 1: g2 }
    source_anchor = 1

    # Create mappings: source node ID -> graph node ID
    mappings = {
        0: [ (0, 2), (1, 0), (2, 1) ],
        1: [ (0, 1), (1, 2), (2, 0) ]
    }

    # Test marking anchors with mappings
    process_data.mark_anchors( graphs, source_anchor, mappings )

    # Verify anchors are correctly marked according to mappings
    assert graphs[ 0 ].nodes[ 0 ][ 'anchor' ] == 0
    assert graphs[ 0 ].nodes[ 1 ][ 'anchor' ] == 0
    assert graphs[ 0 ].nodes[ 2 ][ 'anchor' ] == 1

    assert graphs[ 1 ].nodes[ 2 ][ 'anchor' ] == 0
    assert graphs[ 1 ].nodes[ 0 ][ 'anchor' ] == 1
    assert graphs[ 1 ].nodes[ 1 ][ 'anchor' ] == 0


def test_mark_anchors_with_missing_mapping():
    # Create test graph
    g1 = nx.Graph()
    g1.add_nodes_from( [ (0, { 'label': 0 }), (1, { 'label': 1 }), (2, { 'label': 2 }) ] )
    g1.add_edges_from( [ (0, 1), (1, 2) ] )

    graphs = { 0: g1 }
    source_anchor = 3  # This node doesn't exist in the mapping

    # Create mappings that don't include the source_anchor
    mappings = {
        0: [ (0, 2), (1, 0), (2, 1) ]  # source node 3 is not here
    }

    # Mock the top_pr_ranked_node function to return a predictable value
    with patch.object( graph_utils, 'top_pr_ranked_node', return_value=1 ):
        with patch.object( graph_utils, 'get_anchor', return_value=1 ):  # Ensure get_anchor returns valid value
            # Test marking anchors with missing mappings
            process_data.mark_anchors( graphs, source_anchor, mappings )

            # Verify fallback anchor is set using PR score
            assert graphs[ 0 ].nodes[ 1 ][ 'anchor' ] == 1
            assert graphs[ 0 ].nodes[ 0 ][ 'anchor' ] == 0
            assert graphs[ 0 ].nodes[ 2 ][ 'anchor' ] == 0


def test_mark_anchors_raises_error():
    # Create test graph
    g1 = nx.Graph()
    g1.add_nodes_from( [ (0, { 'label': 0 }), (1, { 'label': 1 }), (2, { 'label': 2 }) ] )
    g1.add_edges_from( [ (0, 1), (1, 2) ] )

    graphs = { 0: g1 }
    source_anchor = 3  # This node doesn't exist

    # Mock functions to simulate a scenario where no valid anchor can be found
    with patch.object( graph_utils, 'top_pr_ranked_node', return_value=1 ):
        with patch.object( graph_utils, 'get_anchor', return_value=-1 ):  # Simulate invalid anchor
            # Expect ValueError when no valid anchor is found
            with pytest.raises( ValueError ):
                process_data.mark_anchors( graphs, source_anchor )
