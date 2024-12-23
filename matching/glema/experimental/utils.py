import torch
from deepsnap.batch import Batch
from deepsnap.graph import Graph as DSGraph
import torch.nn as nn
import torch.optim as optim

import matching.glema.common.utils.model_utils as model_utils
import matching.glema.experimental.feature_preprocess as feature_preprocess
import matching.glema.experimental.models as models


def build_model( args ):
    model = models.OrderEmbedder( args.embedding_dim, args.hidden_dim, args )
    model.to( model_utils.get_device() )
    if args.ckpt_path:
        model = model_utils.initialize_model( model,
                                              model_utils.get_device(),
                                              args.ckpt_path )
    return model


def build_optimizer( model: nn.Module, args ) -> optim.Optimizer:
    weight_decay = args.weight_decay
    filter_fn = filter( lambda p: p.requires_grad, model.parameters() )
    if args.opt == 'adam':
        optimizer = optim.Adam( filter_fn, lr=args.lr, weight_decay=weight_decay )
    return optimizer


def prepare_node_features( graph, embedding_dim, anchored=True ):
    for n in graph.nodes():
        if embedding_dim == 1 and anchored:
            feature_vec = torch.tensor( [ float( graph.nodes[ n ][ "anchor" ] ) ] )
        else:
            feature_vec = model_utils.node_feature( graph, n, embedding_dim, anchored=anchored )
            feature_vec = torch.tensor( [ float( v ) for v in feature_vec ] )
        graph.nodes[ n ][ "node_feature" ] = feature_vec
    return graph


def batch_nx_graphs( graphs, anchors=None ):
    augmenter = feature_preprocess.FeatureAugment()
    """
    if anchors is not None:
        for anchor, g in zip( anchors, graphs ):
            for v in g.nodes:
                # TODO add one-hot vector with labels here (embedding_dim = input_dim of model)
                g.nodes[ v ][ "node_feature" ] = torch.tensor( [ float( v == anchor ) ] )
    """
    batch = Batch.from_data_list( [ DSGraph( g ) for g in graphs ] )
    batch = augmenter.augment( batch )
    batch = batch.to( model_utils.get_device() )
    return batch
