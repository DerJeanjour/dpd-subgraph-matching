import os
import pickle
import random

from torch.utils.data import Dataset

import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.common.utils.model_utils as model_utils
import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.experimental.utils as utils


class BaseDataset( Dataset ):

    def __init__( self, keys, args, k_start=-1, k_keys=None ):

        dataset_name = model_utils.get_dataset_name( args )
        data_dir = os.path.join( args.data_processed_dir, dataset_name )

        self.keys = self.split_keys( keys )
        self.full_keys = self.split_keys( keys )
        self.k_keys = None
        if k_keys is not None:
            self.k_keys = { k: self.split_keys( k_key ) for k, k_key in k_keys.items() }
        self.k = k_start if k_keys is not None else -1
        self.set_keys_by_k()

        self.data_dir = io_utils.get_abs_file_path( data_dir )
        self.anchored = args.anchored

    def __len__( self ):
        return len( self.full_keys[0] )

    def split_keys( self, keys ):
        # TODO maybe guarantee pairs of same source ???
        pos_keys = [ key for key in keys if "iso" in key ]
        neg_keys = [ key for key in keys if "non" in key ]
        return pos_keys, neg_keys

    def set_keys_by_k( self ):
        if self.k_keys is not None and self.k in self.k_keys.keys():

            pos_keys = list()
            neg_keys = list()
            k = self.k
            # merge keys by complexity <= k
            while k in self.k_keys.keys():
                pos_keys.append( self.k_keys[ k ][ 0 ] )
                neg_keys.append( self.k_keys[ k ][ 1 ] )
                k -= 1

            pos_keys = misc_utils.zip_merge( pos_keys )
            neg_keys = misc_utils.zip_merge( neg_keys )
            # TODO maybe guarantee pairs of same source ???
            random.shuffle( pos_keys )
            random.shuffle( neg_keys )
            self.keys = (pos_keys, neg_keys)
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
        pos_keys = self.keys[ 0 ]
        neg_keys = self.keys[ 1 ]
        pos_key = pos_keys[ idx % len( pos_keys ) ]
        neg_key = neg_keys[ idx % len( neg_keys ) ]
        return pos_key, neg_key

    def get_data( self, idx ):
        pos_key, neg_key = self.get_key( idx )

        with open( os.path.join( self.data_dir, pos_key ), "rb" ) as f:
            data = pickle.load( f )
            if len( data ) == 3:
                pos_query, pos_source, pos_mapping = data
            else:
                pos_query, pos_source = data
                pos_mapping = []

        with open( os.path.join( self.data_dir, neg_key ), "rb" ) as f:
            data = pickle.load( f )
            if len( data ) == 3:
                neg_query, neg_source, neg_mapping = data
            else:
                neg_query, neg_source = data
                neg_mapping = []

        return pos_source, pos_query, pos_mapping, neg_source, neg_query, neg_mapping

    def __getitem__( self, idx ):
        pos_source, pos_query, _, neg_source, neg_query, _ = self.get_data( idx )
        return pos_source, pos_query, neg_source, neg_query


def collate_fn( batch ):
    pos_sources, pos_queries, neg_sources, neg_queries = [ ], [ ], [ ], [ ]
    for sample in batch:
        pos_source, pos_query, neg_source, neg_query = sample
        pos_sources.append( pos_source )
        pos_queries.append( pos_query )
        neg_sources.append( neg_source )
        neg_queries.append( neg_query )

    def process_batch( graphs ):
        anchors = []
        for G in graphs:
            anchors.append( graph_utils.get_anchor( G ) )
        return utils.batch_nx_graphs( graphs, anchors=anchors )

    pos_sources = process_batch( pos_sources )
    pos_queries = process_batch( pos_queries )
    neg_sources = process_batch( neg_sources )
    neg_queries = process_batch( neg_queries )

    return pos_sources, pos_queries, neg_sources, neg_queries
