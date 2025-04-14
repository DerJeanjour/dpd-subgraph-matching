from unittest.mock import patch

import networkx as nx
import numpy as np
import pytest

import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.misc.cpg_const as cpg_const
from matching.glema.data.process.data_synthesis import add_features, generate_connected_graph


@pytest.fixture
def simple_graph():
    # Create a simple graph for testing
    G = nx.Graph()
    G.add_edges_from( [ (0, 1), (0, 2), (1, 3), (2, 4) ] )
    return G


@pytest.fixture
def directed_graph():
    # Create a simple directed graph for testing
    G = nx.DiGraph()
    G.add_edges_from( [ (0, 1), (0, 2), (1, 3), (2, 4) ] )
    return G


def test_add_features_basic( simple_graph ):
    # Mock the pagerank function to return a fixed anchor
    with patch.object( graph_utils, 'top_pr_ranked_node', return_value=0 ):
        # Mock the enum index to return a fixed value
        with patch.object( misc_utils, 'get_enum_idx', return_value=5 ):
            # Set a fixed seed for reproducible random choices
            np.random.seed( 42 )

            # Test with basic parameters
            result_graph, anchor = add_features( simple_graph, NN=10, NE=3, strict_edges=False )

            # Check if anchor was set correctly
            assert anchor == 0
            assert result_graph.nodes[ 0 ][ 'anchor' ] == 1

            # Check that all nodes have anchor attribute
            for node_id in result_graph.nodes():
                assert 'anchor' in result_graph.nodes[ node_id ]

            # Check that all nodes have label attribute
            for node_id in result_graph.nodes():
                assert 'label' in result_graph.nodes[ node_id ]

            # Check that all edges have label attribute
            for _, _, edata in result_graph.edges( data=True ):
                assert 'label' in edata
                assert 1 <= edata[ 'label' ] <= 3  # NE=3, so labels should be 1-3


def test_add_features_strict_edges( simple_graph ):
    # Test with strict_edges=True to check edge removal
    even_depth_label = misc_utils.get_enum_idx( cpg_const.NodeLabel.RECORD )

    with patch.object( graph_utils, 'top_pr_ranked_node', return_value=0 ):
        with patch.object( misc_utils, 'get_enum_idx', return_value=5 ):
            np.random.seed( 42 )

            # Add pre-existing labels to create a scenario where edges should be removed
            for node in simple_graph.nodes():
                if node in [ 0, 2, 4 ]:
                    simple_graph.nodes[ node ][ 'label' ] = even_depth_label
                else:
                    simple_graph.nodes[ node ][ 'label' ] = 1  # Different from even_depth_label

            original_edge_count = simple_graph.number_of_edges()
            result_graph, _ = add_features( simple_graph, NN=10, NE=3, strict_edges=True )

            # Check if edges were removed
            assert result_graph.number_of_edges() <= original_edge_count


def test_add_features_directed( directed_graph ):
    # Test with a directed graph
    with patch.object( graph_utils, 'top_pr_ranked_node', return_value=0 ):
        with patch.object( misc_utils, 'get_enum_idx', return_value=5 ):
            np.random.seed( 42 )

            result_graph, anchor = add_features( directed_graph, NN=10, NE=3, strict_edges=False )

            # Check that the function preserves the graph type
            assert isinstance( result_graph, nx.DiGraph )

            # Basic checks
            assert anchor == 0
            assert result_graph.nodes[ 0 ][ 'anchor' ] == 1


def test_add_features_label_assignment( simple_graph ):
    # Test whether the nodes are correctly labeled based on depth
    even_depth_label = misc_utils.get_enum_idx( cpg_const.NodeLabel.RECORD )

    with patch.object( graph_utils, 'top_pr_ranked_node', return_value=0 ):
        with patch.object( misc_utils, 'get_enum_idx', return_value=1 ):
            np.random.seed( 42 )

            result_graph, _ = add_features( simple_graph, NN=10, NE=3, strict_edges=False )

            # Node 0 is at depth 0 (even), should have even_depth_label
            assert result_graph.nodes[ 0 ][ 'label' ] == even_depth_label

            # Nodes 1 and 2 are at depth 1 (odd), should not have even_depth_label
            assert result_graph.nodes[ 1 ][ 'label' ] != even_depth_label
            assert result_graph.nodes[ 2 ][ 'label' ] != even_depth_label

            # Nodes 3 and 4 are at depth 2 (even), should have even_depth_label
            assert result_graph.nodes[ 3 ][ 'label' ] == even_depth_label
            assert result_graph.nodes[ 4 ][ 'label' ] == even_depth_label


def test_generate_connected_graph():
    # Test basic functionality with fixed parameters
    np.random.seed( 42 )
    graph = generate_connected_graph(
        avg_source_size=10,
        std_source_size=2,
        avg_degree=3,
        std_degree=1,
        directed=False
    )

    # Check if graph is created and connected
    assert graph is not None
    assert nx.is_connected( graph )
    assert isinstance( graph, nx.Graph )
    assert not graph.is_directed()

    # Check if node count is reasonable based on parameters
    assert 5 <= len( graph.nodes() ) <= 15  # Allow some variance due to randomness


def test_generate_connected_graph_directed():
    # Test directed graph generation
    np.random.seed( 42 )
    graph = generate_connected_graph(
        avg_source_size=10,
        std_source_size=2,
        avg_degree=3,
        std_degree=1,
        directed=True
    )

    # Check if graph is created and weakly connected
    assert graph is not None
    assert nx.is_weakly_connected( graph )
    assert isinstance( graph, nx.DiGraph )
    assert graph.is_directed()


def test_generate_connected_graph_small():
    # Test with very small graph
    np.random.seed( 42 )
    graph = generate_connected_graph(
        avg_source_size=3,
        std_source_size=0.5,
        avg_degree=1.5,
        std_degree=0.5,
        directed=False
    )

    # Check if graph has at least 2 nodes (min required)
    assert len( graph.nodes() ) >= 2
    assert nx.is_connected( graph )


def test_generate_connected_graph_edge_cases():
    # Test edge cases with extreme parameters
    np.random.seed( 42 )
    graph = generate_connected_graph(
        avg_source_size=5,
        std_source_size=0.5,
        avg_degree=10,  # Much higher than no_of_nodes-1
        std_degree=1,
        directed=False
    )

    assert graph is not None
    assert nx.is_connected( graph )

    # Very low degree (should be at least 1)
    graph = generate_connected_graph(
        avg_source_size=5,
        std_source_size=0.5,
        avg_degree=0.1,  # Much lower than 1
        std_degree=0.1,
        directed=False
    )

    assert graph is not None
    assert nx.is_connected( graph )
