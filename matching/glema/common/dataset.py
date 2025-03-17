import copy
import os
import pickle
import random

import networkx as nx
import numpy as np
from torch.utils.data import Dataset
from torch.utils.data.sampler import Sampler

import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.common.utils.model_utils as model_utils
import matching.misc.cpg_const as cpg_const
from matching.glema.common.encoding import encode_sample


class BaseDataset( Dataset ):

    def __init__( self, keys, args, k_start=-1, k_keys=None, max_size=-1, balanced=True ):

        dataset_name = model_utils.get_dataset_name( args )
        data_dir = os.path.join( args.data_processed_dir, dataset_name )

        self.keys = keys
        self.full_keys = keys
        self.k_keys = k_keys
        self.k = self.get_adjusted_start_k( k_start, k_keys )
        self.balanced = balanced
        self.len = len( self.full_keys ) if max_size < 0 else min( max_size, len( self.full_keys ) )
        self.set_keys_by_k()

        self.data_dir = io_utils.get_abs_file_path( data_dir )
        self.embedding_dim = args.embedding_dim
        self.anchored = args.anchored

    def __len__( self ):
        return self.len

    def get_adjusted_start_k( self, k_start, k_keys ):
        if k_keys is not None:
            k = k_start
            while k <= len( k_keys ):
                if len( k_keys[ k ] ) == 0:
                    k += 1
                else:
                    return k
        return -1

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


class DesignPatternDataset( Dataset ):

    def __init__( self, args, query_pattern=False,
                  max_sources=-1, max_pattern_examples=10, subgraph_relation=1 ):
        misc_utils.set_seed( args.seed )

        self.embedding_dim = args.embedding_dim
        self.anchored = args.anchored
        self.normalized = args.normalized
        self.query_pattern = query_pattern
        self.subgraph_relation = subgraph_relation

        self.sources = self.load_sources( args, args.dataset, max_sources=max_sources, shuffle=True )
        self.source_patterns = self.load_source_patterns( args, args.dataset, self.sources )
        self.patterns = self.load_patterns( args, args.pattern_dataset, max_pattern_examples )
        self.source_record_scopes = self.load_record_scopes( args, args.dataset )
        self.source_graph_record_scopes = self.compute_source_graph_record_scopes()
        self.pattern_record_scopes = self.load_record_scopes( args, args.pattern_dataset )
        self.samples = [ ]
        self.len = 0

    def compute_samples( self ):
        self.samples = self.construct_samples()
        self.len = len( self.samples )

    def get_sources( self ) -> dict[ int, nx.Graph ]:
        return self.sources

    def get_source_patterns( self ) -> dict[ int, str ]:
        return self.source_patterns

    def set_sources( self, sources: dict[ int, nx.Graph ] ) -> None:
        self.sources = sources

    def get_patterns( self ) -> dict[ str, list[ nx.Graph ] ]:
        return self.patterns

    def set_patterns( self, patterns: dict[ str, list[ nx.Graph ] ] ) -> None:
        self.patterns = patterns

    def __transform_record_scopes( self, record_scopes: dict[ int, str ] ) -> dict[ str, str ]:
        return { str( nid ): name for nid, name in record_scopes.items() }

    def get_source_record_scopes( self ) -> dict[ str, str ]:
        return self.__transform_record_scopes( self.source_record_scopes )

    def get_pattern_record_scopes( self ) -> dict[ str, str ]:
        return self.__transform_record_scopes( self.pattern_record_scopes )

    def construct_samples( self ):
        samples = [ ]
        for gidx, source in self.sources.items():
            source_type = self.source_patterns[ gidx ]
            record_scope = self.source_graph_record_scopes[ gidx ]
            if record_scope == "None":
                print( f"Undefined scope for gidx {gidx}" )
            for pattern_type, patterns in self.patterns.items():
                for pattern in patterns:
                    samples.append( (source, source_type,
                                     pattern, pattern_type,
                                     gidx, record_scope) )
        return samples

    def load_sources( self, args, dataset, max_sources=-1, shuffle=False ):
        print( f"Loading sources from {dataset} ..." )
        args = copy.deepcopy( args )
        args.dataset = dataset
        sources = graph_utils.load_source_graphs( args, with_loading_bar=True )
        if shuffle:
            keys = [ *sources.keys() ]
            random.shuffle( keys )
            sources = { k: sources[ k ] for k in keys }
        if 0 < max_sources < len( sources ):
            keys = [ *sources.keys() ][ :max_sources ]
            sources = { k: sources[ k ] for k in keys }
        return sources

    def compute_source_graph_record_scopes( self ):
        source_graph_record_scopes = { }
        for gidx, source in self.sources.items():
            anchor = graph_utils.get_anchor( source )
            record_scope = self.source_record_scopes[ anchor ]
            source_graph_record_scopes[ gidx ] = record_scope
        return source_graph_record_scopes

    def load_record_scopes( self, args, dataset ):
        args = copy.deepcopy( args )
        args.dataset = dataset
        return { int( nid ): name for nid, name in graph_utils.get_record_scopes( args ).items() }

    def load_patterns( self, args, dataset, max_pattern_examples, sources=None ):
        args = copy.deepcopy( args )
        args.dataset = dataset
        if sources is None:
            sources = self.load_sources( args, dataset )
        patterns = graph_utils.get_pattern_graphs( args, sources )
        if max_pattern_examples > 0:
            patterns = { dp: examples[ :max_pattern_examples ] for dp, examples in patterns.items() }
        return patterns

    def load_source_patterns( self, args, dataset, sources ):
        args = copy.deepcopy( args )
        args.dataset = dataset
        pattern_graph_idx = graph_utils.get_pattern_graphs_idxs( args, sources )
        source_patterns: dict[ int, str ] = { }
        for idx in sources.keys():
            pattern = cpg_const.NO_DESIGN_PATTERN
            for pattern_type, idxs in pattern_graph_idx.items():
                if idx in idxs:
                    pattern = pattern_type.value
            source_patterns[ idx ] = pattern
        return source_patterns

    def __len__( self ):
        return self.len

    def get_data( self, idx ):
        (source, source_type,
         pattern, pattern_type,
         gidx, record_scope) = self.samples[ idx ]

        target_source = source if self.query_pattern else pattern
        target_query = pattern if self.query_pattern else source

        max_subgraph_size = target_source.number_of_nodes() // self.subgraph_relation
        if max_subgraph_size < target_query.number_of_nodes():
            target_query = graph_utils.subgraph_from_anchor_of_size( target_query, max_subgraph_size )

        meta = {
            "source_type": source_type,
            "pattern_type": pattern_type,
            "gidx": gidx,
            "record_scope": record_scope
        }
        return target_source, target_query, meta

    def __getitem__( self, idx ):
        source, query, meta = self.get_data( idx )
        return encode_sample( query, source, self.embedding_dim,
                              anchored=self.anchored, key=meta, is_custom_key=True )


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
