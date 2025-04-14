import networkx as nx
import numpy as np
import torch

import matching.misc.cpg_const as cpg_const
from matching.glema.common.encoding import (
    onehot_encoding,
    node_feature,
    onehot_encoding_node,
    encode_sample,
    collate_fn
)


def test_onehot_encoding_anchored():
    # Test normal case with anchoring
    result = onehot_encoding( label_idx=2, anchor_idx=3, embedding_dim=5, anchored=True )
    expected = [ 3, 0, 1, 0, 0 ]
    assert result == expected

    # Test when label_idx exceeds embedding_dim
    result = onehot_encoding( label_idx=10, anchor_idx=4, embedding_dim=5, anchored=True )
    expected = [ 4, 0, 0, 0, 0 ]
    assert result[ 0 ] == 4  # anchor should be set
    assert result[ cpg_const.DEFAULT_INTERACTION_IDX + 1 ] == 1  # default label should be set


def test_onehot_encoding_not_anchored():
    # Test normal case without anchoring
    result = onehot_encoding( label_idx=2, anchor_idx=3, embedding_dim=5, anchored=False )
    expected = [ 0, 1, 0, 0, 0 ]
    assert result == expected

    # Test when label_idx exceeds embedding_dim
    result = onehot_encoding( label_idx=10, anchor_idx=4, embedding_dim=5, anchored=False )
    assert result[ cpg_const.DEFAULT_INTERACTION_IDX ] == 1  # default label should be set


def test_node_feature():
    # Create a test graph
    G = nx.Graph()
    G.add_node( 0, label=1, anchor=2 )
    G.add_node( 1, label=2 )  # No anchor

    # Test with anchoring
    result = node_feature( G, 0, embedding_dim=5, anchored=True )
    expected = [ 2, 1, 0, 0, 0 ]
    assert result == expected

    # Test without anchor attribute
    result = node_feature( G, 1, embedding_dim=5, anchored=True )
    expected = [ 0, 0, 1, 0, 0 ]
    assert result == expected

    # Test without anchoring
    result = node_feature( G, 0, embedding_dim=5, anchored=False )
    expected = [ 1, 0, 0, 0, 0 ]
    assert result == expected


def test_onehot_encoding_node():
    # Create a test graph
    G = nx.Graph()
    G.add_node( 0, label=1, anchor=2 )
    G.add_node( 1, label=2 )

    # Test with anchoring
    result = onehot_encoding_node( G, embedding_dim=5, anchored=True )
    expected = np.array( [ [ 2, 1, 0, 0, 0 ], [ 0, 0, 1, 0, 0 ] ] )
    assert np.array_equal( result, expected )

    # Test without anchoring
    result = onehot_encoding_node( G, embedding_dim=5, anchored=False )
    expected = np.array( [ [ 1, 0, 0, 0, 0 ], [ 0, 1, 0, 0, 0 ] ] )
    assert np.array_equal( result, expected )


def test_encode_sample():
    # Create sample graphs
    query = nx.Graph()
    query.add_node( 0, label=1 )
    query.add_node( 1, label=2 )
    query.add_edge( 0, 1 )

    source = nx.Graph()
    source.add_node( 0, label=1 )
    source.add_node( 1, label=2 )
    source.add_node( 2, label=3 )
    source.add_edge( 0, 1 )
    source.add_edge( 1, 2 )

    # Test encoding
    embedding_dim = 5
    result = encode_sample( query, source, embedding_dim, anchored=False )

    # Verify basic properties
    assert isinstance( result, dict )
    assert "H" in result
    assert "A1" in result
    assert "A2" in result
    assert "Y" in result
    assert "V" in result
    assert "mapping" in result
    assert "same_label" in result

    # Check shapes
    n_query = query.number_of_nodes()
    n_source = source.number_of_nodes()
    total_nodes = n_query + n_source

    assert result[ "H" ].shape == (total_nodes, 2 * embedding_dim)
    assert result[ "A1" ].shape == (total_nodes, total_nodes)
    assert result[ "A2" ].shape == (total_nodes, total_nodes)
    assert result[ "mapping" ].shape == (total_nodes, total_nodes)
    assert result[ "same_label" ].shape == (total_nodes, total_nodes)
    assert result[ "V" ].shape == (total_nodes,)

    # Test with mapping
    mapping = [ (0, 0), (1, 1) ]  # Map query nodes to source nodes
    result_with_mapping = encode_sample( query, source, embedding_dim, mapping=mapping, key="iso" )
    assert result_with_mapping[ "Y" ] == 1  # Should be isomorphic


def test_collate_fn():
    # Create two sample encodings
    query1 = nx.Graph()
    query1.add_node( 0, label=1 )
    source1 = nx.Graph()
    source1.add_node( 0, label=1 )
    source1.add_node( 1, label=2 )

    query2 = nx.Graph()
    query2.add_node( 0, label=1 )
    query2.add_node( 1, label=2 )
    source2 = nx.Graph()
    source2.add_node( 0, label=1 )

    embedding_dim = 5
    sample1 = encode_sample( query1, source1, embedding_dim, key="iso" )
    sample2 = encode_sample( query2, source2, embedding_dim, key="not_iso" )

    batch = [ sample1, sample2 ]
    H, A1, A2, M, S, Y, V, keys = collate_fn( batch )

    # Check that all are torch tensors
    assert torch.is_tensor( H )
    assert torch.is_tensor( A1 )
    assert torch.is_tensor( A2 )
    assert torch.is_tensor( M )
    assert torch.is_tensor( S )
    assert torch.is_tensor( Y )
    assert torch.is_tensor( V )

    # Check shapes
    max_atoms = max( len( sample1[ "H" ] ), len( sample2[ "H" ] ) )
    assert H.shape == (2, max_atoms, 2 * embedding_dim)
    assert A1.shape == (2, max_atoms, max_atoms)
    assert A2.shape == (2, max_atoms, max_atoms)
    assert M.shape == (2, max_atoms, max_atoms)
    assert S.shape == (2, max_atoms, max_atoms)
    assert Y.shape == (2,)
    assert V.shape == (2, max_atoms)

    # Check keys
    assert keys == [ "iso", "not_iso" ]
