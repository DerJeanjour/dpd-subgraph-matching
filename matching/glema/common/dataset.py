import os
import pickle
import random

import numpy as np
from torch.utils.data import Dataset
from torch.utils.data.sampler import Sampler

import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.common.utils.model_utils as model_utils
from matching.glema.common.encoding import encode_sample


class BaseDataset( Dataset ):

    def __init__( self, keys, args, k_start=-1, k_keys=None, max_size=-1, balanced=True ):

        dataset_name = model_utils.get_dataset_name( args )
        data_dir = os.path.join( args.data_processed_dir, dataset_name )

        self.keys = keys
        self.full_keys = keys
        self.k_keys = k_keys
        self.k = k_start if k_keys is not None else -1
        self.balanced = balanced
        self.len = len( self.full_keys ) if max_size < 0 else min( max_size, len( self.full_keys ) )
        self.set_keys_by_k()

        self.data_dir = io_utils.get_abs_file_path( data_dir )
        self.embedding_dim = args.embedding_dim
        self.anchored = args.anchored

    def __len__( self ):
        return self.len

    def balance_keys( self, keys, shuffle=True ):
        """
        Balances the data keys by ensuring an equal number of isomorphic and non-isomorphic keys.
        """
        iso_keys = [ key for key in keys if "iso" in key ]
        non_iso_keys = [ key for key in keys if "non" in key ]

        if shuffle:
            random.shuffle( iso_keys )
            random.shuffle( non_iso_keys )

        data_limit = min( len( iso_keys ), len( non_iso_keys ), self.len // 2 )
        iso_keys = iso_keys[ :data_limit ]
        non_iso_keys = non_iso_keys[ :data_limit ]

        balanced_keys = misc_utils.zip_merge( [ iso_keys, non_iso_keys ] )
        if shuffle:
            random.shuffle( balanced_keys )

        return balanced_keys

    def set_keys_by_k( self ):
        if self.k_keys is not None and self.k in self.k_keys.keys():

            keys = list()
            k = self.k
            # merge keys by complexity <= k
            while k in self.k_keys.keys():
                keys.append( self.k_keys[ k ] )
                k -= 1

            keys = misc_utils.zip_merge( keys )
            self.keys = keys
        else:
            self.keys = self.full_keys
            self.k = -1

        if self.balanced:
            self.keys = self.balance_keys( self.keys )

    def get_key_split( self ):
        iso_keys = [ key for key in self.keys if "iso" in key ]
        non_iso_keys = [ key for key in self.keys if "non" in key ]
        return iso_keys, non_iso_keys

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
        return encode_sample( query, source, self.embedding_dim,
                              anchored=self.anchored, key=key, mapping=mapping )


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
