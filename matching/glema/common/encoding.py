import networkx as nx
import numpy as np
import torch
from scipy.spatial import distance_matrix


def onehot_encoding( label_idx, anchor_idx, embedding_dim, anchored=True ):
    onehot_vector = [ 0 ] * embedding_dim
    if anchored:
        onehot_vector[ 0 ] = anchor_idx
        if anchor_idx < 1: # TODO for testing, remove me
            onehot_vector[ label_idx ] = 1  # label start from 1
    else:
        onehot_vector[ label_idx - 1 ] = 1  # label start from 1
    return onehot_vector


def node_feature( graph, node_idx, embedding_dim, anchored=True ):
    node = graph.nodes[ node_idx ]
    label_idx = node[ "label" ]
    anchor_idx = 0
    if anchored and "anchor" in node:
        anchor_idx = node[ "anchor" ]
    return onehot_encoding( label_idx, anchor_idx, embedding_dim, anchored=anchored )


def onehot_encoding_node( graph, embedding_dim, anchored=True ):
    H = [ ]
    for node_idx in graph.nodes:
        H.append( node_feature( graph, node_idx, embedding_dim, anchored=anchored ) )
    H = np.array( H )
    return H


def encode_sample( query, source, embedding_dim, anchored=True, mapping=None, key=None ):
    # Prepare subgraph
    n_query = query.number_of_nodes()
    adj_query = nx.to_numpy_array( query ) + np.eye( n_query )
    H_query = onehot_encoding_node( query, embedding_dim, anchored=anchored )

    # Prepare source graph
    n_source = source.number_of_nodes()
    adj_source = nx.to_numpy_array( source ) + np.eye( n_source )
    H_source = onehot_encoding_node( source, embedding_dim, anchored=anchored )

    # Aggregation node encoding
    agg_adj1 = np.zeros( (n_query + n_source, n_query + n_source) )
    agg_adj1[ :n_query, :n_query ] = adj_query
    agg_adj1[ n_query:, n_query: ] = adj_source
    agg_adj2 = np.copy( agg_adj1 )
    dm = distance_matrix( H_query, H_source )
    dm_new = np.zeros_like( dm )
    dm_new[ dm == 0.0 ] = 1.0
    agg_adj2[ :n_query, n_query: ] = np.copy( dm_new )
    agg_adj2[ n_query:, :n_query ] = np.copy( np.transpose( dm_new ) )

    H_query = np.concatenate( [ H_query, np.zeros( (n_query, embedding_dim) ) ], 1 )
    H_source = np.concatenate( [ np.zeros( (n_source, embedding_dim) ), H_source ], 1 )
    H = np.concatenate( [ H_query, H_source ], 0 )

    # node indices for aggregation
    valid = np.zeros( (n_query + n_source,) )
    valid[ :n_query ] = 1

    # create mapping matrix
    mapping_matrix = np.zeros_like( agg_adj1 )
    if mapping is not None and len( mapping ) > 0:
        mapping = np.array( mapping ).T
        mapping[ 1 ] = mapping[ 1 ] + n_query
        mapping_matrix[ mapping[ 0 ], mapping[ 1 ] ] = 1.0
        mapping_matrix[ mapping[ 1 ], mapping[ 0 ] ] = 1.0

    same_label_matrix = np.zeros_like( agg_adj1 )
    same_label_matrix[ :n_query, n_query: ] = np.copy( dm_new )
    same_label_matrix[ n_query:, :n_query ] = np.copy( np.transpose( dm_new ) )

    # iso to class
    Y = -1
    if key is not None:
        Y = 1 if "iso" in key else 0

    sample = {
        "H": H,
        "A1": agg_adj1,  # intra adjacency
        "A2": agg_adj2,  # inter adjacency ("virtual edges" between target and source with same label)
        "Y": Y,
        "V": valid,
        "key": key,
        "mapping": mapping_matrix,
        "same_label": same_label_matrix,
    }

    return sample


def collate_fn( batch ):
    max_natoms = max( [ len( item[ "H" ] ) for item in batch if item is not None ] )

    H = np.zeros( (len( batch ), max_natoms, batch[ 0 ][ "H" ].shape[ -1 ]) )
    A1 = np.zeros( (len( batch ), max_natoms, max_natoms) )
    A2 = np.zeros( (len( batch ), max_natoms, max_natoms) )
    M = np.zeros( (len( batch ), max_natoms, max_natoms) )
    S = np.zeros( (len( batch ), max_natoms, max_natoms) )
    Y = np.zeros( (len( batch ),) )
    V = np.zeros( (len( batch ), max_natoms) )

    keys = [ ]

    for i in range( len( batch ) ):
        natom = len( batch[ i ][ "H" ] )

        H[ i, :natom ] = batch[ i ][ "H" ]
        A1[ i, :natom, :natom ] = batch[ i ][ "A1" ]
        A2[ i, :natom, :natom ] = batch[ i ][ "A2" ]
        M[ i, :natom, :natom ] = batch[ i ][ "mapping" ]
        S[ i, :natom, :natom ] = batch[ i ][ "same_label" ]
        Y[ i ] = batch[ i ][ "Y" ]
        V[ i, :natom ] = batch[ i ][ "V" ]
        keys.append( batch[ i ][ "key" ] )

    H = torch.from_numpy( H ).float()
    A1 = torch.from_numpy( A1 ).float()
    A2 = torch.from_numpy( A2 ).float()
    M = torch.from_numpy( M ).float()
    S = torch.from_numpy( S ).float()
    Y = torch.from_numpy( Y ).float()
    V = torch.from_numpy( V ).float()

    return H, A1, A2, M, S, Y, V, keys
