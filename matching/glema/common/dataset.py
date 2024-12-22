import os
import pickle
import random

import networkx as nx
import numpy as np
import torch
from scipy.spatial import distance_matrix
from torch.utils.data import Dataset
from torch.utils.data.sampler import Sampler

import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.model_utils as model_utils
import matching.glema.common.utils.misc_utils as misc_utils

def onehot_encoding_node( graph, embedding_dim, anchored=True ):
    H = [ ]
    for node_idx in graph.nodes:
        H.append( model_utils.node_feature( graph, node_idx, embedding_dim, anchored=anchored ) )
    H = np.array( H )
    return H


class BaseDataset( Dataset ):

    def __init__( self, keys, args, k_start=-1, k_keys=None ):

        dataset_name = model_utils.get_dataset_name( args )
        data_dir = os.path.join( args.data_processed_dir, dataset_name )

        self.keys = keys
        self.full_keys = keys
        self.k_keys = k_keys
        self.k = k_start if k_keys is not None else -1
        self.set_keys_by_k()

        self.data_dir = io_utils.get_abs_file_path( data_dir )
        self.embedding_dim = args.embedding_dim
        self.anchored = args.anchored

    def __len__( self ):
        return len( self.full_keys )

    def set_keys_by_k( self ):
        if self.k_keys is not None and self.k in self.k_keys.keys():

            keys = list()
            k = self.k
            # merge keys by complexity <= k
            while k in self.k_keys.keys():
                keys.append( self.k_keys[ k ] )
                k -= 1

            keys = misc_utils.zip_merge( keys )
            random.shuffle( keys )
            self.keys = keys
        else:
            self.keys = self.full_keys
            self.k = -1

    def increase_complexity( self, k_inc=1 ):
        if self.k >= 0:
            self.k += k_inc
            if self.k > max( self.k_keys.keys() ):
                self.remove_complexity_limit()
            else:
                print( f"Increased graph sample complexity limit from {self.k - k_inc} to {self.k}" )
                self.set_keys_by_k()

    def remove_complexity_limit( self ):
        self.k = -1
        self.set_keys_by_k()
        print( f"Removed graph sample complexity limit" )

    def get_complexity_limit( self ):
        return self.k

    def get_key( self, idx ):
        return self.keys[ idx % len( self.keys ) ]

    def get_data( self, idx ):
        key = self.get_key( idx )
        with open( os.path.join( self.data_dir, key ), "rb" ) as f:
            data = pickle.load( f )
            if len( data ) == 3:
                query, source, mapping = data
            else:
                query, source = data
                mapping = [ ]

        return query, source, mapping

    def __getitem__( self, idx ):
        # idx = 0
        key = self.get_key( idx )
        query, source, mapping = self.get_data( idx )

        # Prepare subgraph
        n_query = query.number_of_nodes()
        adj_query = nx.to_numpy_array( query ) + np.eye( n_query )
        H_query = onehot_encoding_node( query, self.embedding_dim, anchored=self.anchored )

        # Prepare source graph
        n_source = source.number_of_nodes()
        adj_source = nx.to_numpy_array( source ) + np.eye( n_source )
        H_source = onehot_encoding_node( source, self.embedding_dim, anchored=self.anchored )

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

        H_query = np.concatenate( [ H_query, np.zeros( (n_query, self.embedding_dim) ) ], 1 )
        H_source = np.concatenate( [ np.zeros( (n_source, self.embedding_dim) ), H_source ], 1 )
        H = np.concatenate( [ H_query, H_source ], 0 )

        # node indices for aggregation
        valid = np.zeros( (n_query + n_source,) )
        valid[ :n_query ] = 1

        # create mapping matrix
        mapping_matrix = np.zeros_like( agg_adj1 )
        if len( mapping ) > 0:
            mapping = np.array( mapping ).T
            mapping[ 1 ] = mapping[ 1 ] + n_query
            mapping_matrix[ mapping[ 0 ], mapping[ 1 ] ] = 1.0
            mapping_matrix[ mapping[ 1 ], mapping[ 0 ] ] = 1.0

        same_label_matrix = np.zeros_like( agg_adj1 )
        same_label_matrix[ :n_query, n_query: ] = np.copy( dm_new )
        same_label_matrix[ n_query:, :n_query ] = np.copy( np.transpose( dm_new ) )

        # iso to class
        Y = 1 if "iso" in key else 0

        # if n1+n2 > 300 : return None
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


class UnderSampler( Sampler ):
    def __init__( self, weights, num_samples, replacement=True ):
        weights = np.array( weights ) / np.sum( weights )
        self.weights = weights
        self.num_samples = num_samples
        self.replacement = replacement

    def __iter__( self ):
        retval = np.random.choice(
            len( self.weights ),
            self.num_samples,
            replace=self.replacement,
            p=self.weights,
        )
        return iter( retval.tolist() )

    def __len__( self ):
        return self.num_samples


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


# experimental curriculum training sampling, dont use!
def get_data( self, idx, relabel_internal=False ):
    key = self.keys[ idx ]
    is_iso = "non" not in key
    with open( os.path.join( self.data_dir, key ), "rb" ) as f:
        data = pickle.load( f )
        if len( data ) == 3:
            query, source, mapping = data
        else:
            query, source = data
            mapping = [ ]

    if self.k > 0:
        query, source, mapping = self.reduce_sample_complexity(
            is_iso, query, source, mapping, relabel_internal=relabel_internal )
    return query, source, mapping


def reduce_sample_complexity( self, is_iso, query, source, mapping, relabel_internal=False ):
    source_anchor = graph_utils.get_anchor( source )
    source = graph_utils.subgraph( source.copy(), source_anchor, self.k )
    query_anchor = graph_utils.get_anchor( query )
    # TODO non iso ones have to be non iso as subgraph ...
    query = graph_utils.subgraph( query.copy(), query_anchor, self.k )
    if not is_iso:
        query = self.assure_non_iso_1k_neighbourhood( query, source, source_anchor, mapping )
    # TODO double check if this is a correct mapping strategy
    mapping = [ (qid, sid) for qid, sid in mapping if qid in query.nodes and sid in source.nodes ]
    if relabel_internal:
        query, source, mapping = self.relabel_to_interal_ids( query, source, mapping )
    return query, source, mapping


def assure_non_iso_1k_neighbourhood( self, query, source, source_anchor, mapping ):
    random.seed( 42 )

    query_mapping = { sid: qid for qid, sid in mapping }
    if source_anchor not in query_mapping.keys():
        return query

    query_anchor = query_mapping[ source_anchor ]
    if query_anchor not in query.nodes:
        return query

    """
    # Compute 1-k ego graphs for the anchor node
    ego_source = graph_utils.subgraph( source.copy(), source_anchor, 1 )
    ego_query = graph_utils.subgraph( query.copy(), query_anchor, 1 )

    # Check if the ego graphs are isometric
    if not graph_utils.is_iso_subgraph( ego_source, ego_query ):
        return query
    """

    source_neighbors = list( source.neighbors( source_anchor ) )
    query_neighbors = list( query.neighbors( query_anchor ) )
    if len( source_neighbors ) < len( query_neighbors ):
        return query

    diff = list( set( query_neighbors ).difference( query_mapping.values() ) )
    if len( diff ) > 0:
        degrees_by_node = sorted( list( query.degree( diff ) ), key=lambda x: x[ 1 ] )
        node_to_modify = degrees_by_node[ 0 ][ 0 ]
        current_label = query.nodes[ node_to_modify ][ "label" ]
        possible_labels = list( range( 1, 6 ) )
        possible_labels.remove( current_label )
        new_label = random.choice( possible_labels )
        query.nodes[ node_to_modify ][ "label" ] = new_label
    else:
        # TODO this is wrong, a query can't be bigger then the source!
        for i in list( range( (len( source_neighbors ) - len( query_neighbors )) + 1 ) ):
            nid = len( source.nodes() ) + i + 1
            label = random.choice( list( range( 1, 6 ) ) )
            query.add_node( nid, label=label, anchor=0 )
            query.add_edge( query_anchor, nid, label=1 )

    return query


def relabel_to_interal_ids( self, query, source, mapping ):
    # map node ids of reduced graphs, so that max(ids) == len(query) + len(source)
    query_reduced_mapping = { nid: idx for idx, nid in enumerate( query.nodes ) }
    source_reduced_mapping = { nid: idx for idx, nid in enumerate( source.nodes ) }
    query = nx.relabel_nodes( query, query_reduced_mapping )
    source = nx.relabel_nodes( source, source_reduced_mapping )
    mapping = [ (query_reduced_mapping[ qid ], source_reduced_mapping[ sid ]) for qid, sid in mapping ]
    return query, source, mapping
