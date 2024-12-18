import torch
import torch.nn as nn

import matching.glema.common.utils.io_utils as io_utils
import matching.misc.utils as utils


def get_device( force_cpu=False ) -> torch.device:
    return utils.get_device( force_cpu=force_cpu )


def model_uses_cuda( model: torch.nn.Module ) -> bool:
    return utils.model_uses_cuda( model )


def initialize_model( model, device, load_save_file: str = None ):
    if not load_save_file is None:
        print( f"Loading model from {load_save_file} ..." )
        model.load_state_dict(
            torch.load( io_utils.get_abs_file_path( load_save_file ), map_location=device, weights_only=True )
        )
    else:
        print( f"Init default model ..." )
        for param in model.parameters():
            if param.dim() == 1:
                continue
            else:
                nn.init.xavier_normal_( param )

    model.to( device )
    return model


def onehot_encoding( label_idx, anchor_idx, embedding_dim, anchored=True ):
    onehot_vector = [ 0 ] * embedding_dim
    if anchored:
        onehot_vector[ 0 ] = anchor_idx  # TODO put it to the end ?!
        onehot_vector[ label_idx ] = 1  # label start from 1
    else:
        onehot_vector[ label_idx - 1 ] = 1  # label start from 1
    return onehot_vector


def one_of_k_encoding( x, allowable_set ):
    if x not in allowable_set:
        raise Exception( "input {0} not in allowable set{1}:".format( x, allowable_set ) )

    return list( map( lambda s: x == s, allowable_set ) )


def one_of_k_encoding_unk( x, allowable_set ):
    """Maps inputs not in the allowable set to the last element."""
    if x not in allowable_set:
        x = allowable_set[ -1 ]
    return list( map( lambda s: x == s, allowable_set ) )


def node_feature( graph, node_idx, embedding_dim, anchored=True ):
    node = graph.nodes[ node_idx ]
    label_idx = node[ "label" ]
    anchor_idx = 0
    if anchored and "anchor" in node:
        anchor_idx = node[ "anchor" ]
    return onehot_encoding( label_idx, anchor_idx, embedding_dim, anchored=anchored )


def get_shape_of_tensors( input_tensors ):
    return [ tuple( tensor.shape ) for tensor in input_tensors ]
