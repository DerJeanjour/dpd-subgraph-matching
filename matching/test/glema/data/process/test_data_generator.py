from unittest.mock import patch, MagicMock

import networkx as nx
import numpy as np

from matching.glema.data.process.data_generator import (
    generate_iso_subgraph,
    generate_noniso_subgraph,
    remove_random_edge
)


class TestDataGenerator:

    def test_generate_iso_subgraph_basic( self ):
        # Create a simple graph for testing
        G = nx.Graph()
        for i in range( 10 ):
            G.add_node( i, label=1 )
        for i in range( 9 ):
            G.add_edge( i, i + 1, label=1 )

        # Test with basic parameters
        result = generate_iso_subgraph( G, None, True, 5, 2, 0.5 )

        # Check results
        assert result is not None
        assert nx.is_connected( result )
        assert result.number_of_nodes() >= 2
        assert isinstance( result, nx.Graph )

    def test_generate_iso_subgraph_with_anchor( self ):
        # Create a simple graph for testing
        G = nx.Graph()
        for i in range( 10 ):
            G.add_node( i, label=1 )
        for i in range( 9 ):
            G.add_edge( i, i + 1, label=1 )

        # Set anchor node to ensure it's included in the subgraph
        anchor = 5
        result = generate_iso_subgraph( G, anchor, True, 4, 2, 0.5 )

        # Check anchor is present
        assert anchor in result.nodes

    def test_generate_iso_subgraph_node_ratio_capping( self ):
        # Create a small graph
        G = nx.Graph()
        for i in range( 5 ):
            G.add_node( i, label=1 )
        for i in range( 4 ):
            G.add_edge( i, i + 1, label=1 )

        # Request more nodes than exist in the graph
        result = generate_iso_subgraph( G, None, True, 10, 2, 0.5 )

        # Should still return a valid subgraph
        assert result is not None
        assert nx.is_connected( result )
        assert result.number_of_nodes() <= G.number_of_nodes()

    def test_generate_iso_subgraph_increases_node_ratio( self ):
        # Create a graph with a structure that would likely fail to create a connected
        # subgraph on first attempts (star graph)
        G = nx.star_graph( 20 )
        # Add labels to match the expected format
        for node in G.nodes():
            G.nodes[ node ][ 'label' ] = 1
        for edge in G.edges():
            G.edges[ edge ][ 'label' ] = 1

        with patch( 'numpy.random.choice' ) as mock_choice:
            # First 6 calls return arrays that would remove the central node
            # causing disconnected subgraphs
            mock_choice.side_effect = [
                np.array( [ 0 ] + [ 1 ] * 20 ),  # This would remove the central node
                np.array( [ 0 ] + [ 1 ] * 20 ),
                np.array( [ 0 ] + [ 1 ] * 20 ),
                np.array( [ 0 ] + [ 1 ] * 20 ),
                np.array( [ 0 ] + [ 1 ] * 20 ),
                np.array( [ 0 ] + [ 1 ] * 20 ),
                np.array( [ 1 ] * 21 )  # Finally return a valid selection
            ]

            result = generate_iso_subgraph( G, None, True, 5, 2, 0.5 )

            # Should still produce a valid result
            assert result is not None
            assert nx.is_connected( result )
            assert mock_choice.call_count > 6  # Should have tried multiple times

    def test_generate_noniso_subgraph_basic( self ):
        # Create a simple graph for testing
        G = nx.Graph()
        for i in range( 10 ):
            G.add_node( i, label=1 )
        for i in range( 9 ):
            G.add_edge( i, i + 1, label=1 )

        # Test with basic parameters
        with patch( "matching.glema.data.process.data_generator.time_limit" ) as mock_time_limit:
            mock_context = MagicMock()
            mock_time_limit.return_value = mock_context

            result = generate_noniso_subgraph(
                G, None, True, 5, 2, 0.5, 2, 2
            )

        # Check results
        assert result is not None
        assert nx.is_connected( result )
        assert result.number_of_nodes() >= 2
        assert isinstance( result, nx.Graph )
        # Verify it's not isomorphic to a subgraph of G
        matcher = nx.algorithms.isomorphism.GraphMatcher(
            G, result, node_match=lambda n1, n2: n1[ "label" ] == n2[ "label" ],
            edge_match=lambda e1, e2: e1[ "label" ] == e2[ "label" ]
        )
        assert not matcher.subgraph_is_isomorphic()
