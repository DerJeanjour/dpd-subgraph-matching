import random

import networkx as nx
import numpy as np
import scipy.stats as stats
import torch
from deepsnap.batch import Batch
from deepsnap.graph import Graph as DSGraph

from matching.src import feature_preprocess
from matching.src import utils


def get_dataset( node_size=100 ):
    task = "graph"
    dataset = [ ]
    for i in range( 100 ):
        dataset.append( utils.generate_graph( node_size ) )

    train_len = int( 0.8 * len( dataset ) )
    train, test = [ ], [ ]
    random.shuffle( dataset )
    for i, graph in enumerate( dataset ):
        if i < train_len:
            train.append( graph )
        else:
            test.append( graph )
    return train, test, task


def gen_data_loaders( size: int, batch_size: int ):
    loaders = [ [ batch_size ] * (size // batch_size) for i in range( 3 ) ]
    return loaders


def gen_batch( dataset, a, b, c, train, node_anchored=True, filter_negs=False, max_size=15, min_size=5, batched=True ):
    batch_size = a
    train_set, test_set, task = dataset
    graphs = train_set if train else test_set

    # pos
    pos_target, pos_query = [ ], [ ]
    pos_target_anchors, pos_query_anchors = [ ], [ ]
    for i in range( batch_size // 2 ):
        # tree-pair
        size = random.randint( min_size + 1, max_size )
        graph, a = sample_neigh( graphs, size )
        b = a[ :random.randint( min_size, len( a ) - 1 ) ]
        if node_anchored:
            anchor = list( graph.nodes )[ 0 ]
            pos_target_anchors.append( anchor )
            pos_query_anchors.append( anchor )
        neigh_target, neigh_query = graph.subgraph( a ), graph.subgraph( b )
        pos_target.append( neigh_target )
        pos_query.append( neigh_query )

    # neg
    neg_target, neg_query = [ ], [ ]
    neg_target_anchors, neg_query_anchors = [ ], [ ]
    while len( neg_target ) < batch_size // 2:
        # tree-pair
        size = random.randint( min_size + 1, max_size )
        graph_a, a = sample_neigh( graphs, size )
        graph_b, b = sample_neigh( graphs, random.randint( min_size, size - 1 ) )
        if node_anchored:
            neg_target_anchors.append( list( graph_a.nodes )[ 0 ] )
            neg_query_anchors.append( list( graph_b.nodes )[ 0 ] )
        neigh_target, neigh_query = graph_a.subgraph( a ), graph_b.subgraph( b )
        if filter_negs:
            matcher = nx.algorithms.isomorphism.GraphMatcher( neigh_target, neigh_query )
            if matcher.subgraph_is_isomorphic():  # a <= b (b is subgraph of a)
                continue
        neg_target.append( neigh_target )
        neg_query.append( neigh_query )

    if not batched:
        return pos_target, pos_query, neg_target, neg_query

    # to batches
    pos_target = batch_nx_graphs( pos_target, anchors=pos_target_anchors if node_anchored else None )
    pos_query = batch_nx_graphs( pos_query, anchors=pos_query_anchors if node_anchored else None )
    neg_target = batch_nx_graphs( neg_target, anchors=neg_target_anchors if node_anchored else None )
    neg_query = batch_nx_graphs( neg_query, anchors=neg_query_anchors if node_anchored else None )
    return pos_target, pos_query, neg_target, neg_query


def sample_neigh( graphs, size ):
    ps = np.array( [ len( g ) for g in graphs ], dtype=float )
    ps /= np.sum( ps )
    dist = stats.rv_discrete( values=(np.arange( len( graphs ) ), ps) )
    while True:
        idx = dist.rvs()
        # graph = random.choice(graphs)
        graph = graphs[ idx ]
        start_node = random.choice( list( graph.nodes ) )
        neigh = [ start_node ]
        frontier = list( set( graph.neighbors( start_node ) ) - set( neigh ) )
        visited = set( [ start_node ] )
        while len( neigh ) < size and frontier:
            new_node = random.choice( list( frontier ) )
            assert new_node not in neigh
            neigh.append( new_node )
            visited.add( new_node )
            frontier += list( graph.neighbors( new_node ) )
            frontier = [ x for x in frontier if x not in visited ]
        if len( neigh ) == size:
            return graph, neigh


def batch_nx_graphs( graphs, anchors=None ):
    augmenter = feature_preprocess.FeatureAugment()

    if anchors is not None:
        for anchor, g in zip( anchors, graphs ):
            for v in g.nodes:
                g.nodes[ v ][ "node_feature" ] = torch.tensor( [ float( v == anchor ) ] )

    batch = Batch.from_data_list( [ DSGraph( g ) for g in graphs ] )
    batch = augmenter.augment( batch )
    return batch.to( utils.get_device() )
