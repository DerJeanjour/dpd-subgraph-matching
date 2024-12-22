import os
import pickle

import torch
import torch.nn as nn

import matching.glema.common.utils.io_utils as io_utils
import matching.misc.utils as utils


def get_device( force_cpu=False ) -> torch.device:
    return utils.get_device( force_cpu=force_cpu )


def get_dataset_name( args ) -> str:
    dataset_name = args.dataset
    if args.directed:
        dataset_name += "_directed"
    return dataset_name


def get_model_name( args, version: int ) -> str:
    if version < 1:
        print( f"model version can't be smaller then 1, but got: {version}" )
        raise ValueError

    model_name = f"{args.dataset}_{'directed' if args.directed else 'undirected'}"
    if args.anchored:
        model_name += "_anchored"
    model_name += f"_v{version}"
    return model_name


def get_latest_model_version( args ) -> int:
    version = 1
    model_name = get_model_name( args, version )
    model_ckpt_dir = io_utils.get_abs_file_path( args.ckpt_dir )

    existing_model_names = io_utils.get_filenames_in_dir( model_ckpt_dir, only_files=False )
    if model_name not in existing_model_names:
        return -1  # no model exists for args

    exists = True
    while exists:
        model_name = model_name.replace( f"v{version}", f"v{version + 1}" )
        if model_name not in existing_model_names:
            exists = False
        else:
            version += 1

    return version


def get_model_ckpt_dir( args, model_name: str ) -> str:
    model_ckpt_dir = io_utils.get_abs_file_path( args.ckpt_dir )
    return os.path.join( model_ckpt_dir, model_name )


def get_model_ckpt( args, model_name ):
    model_ckpt_dir = get_model_ckpt_dir( args, model_name )
    return os.path.join( model_ckpt_dir, "model.pt" )


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
        onehot_vector[
            0 ] = anchor_idx  # TODO idea maybe use a normalized distance to anchor?
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

def load_complexity_keys( args, train=True ):
        dataset_name = get_dataset_name( args )
        data_path = io_utils.get_abs_file_path( os.path.join( args.data_processed_dir, dataset_name ) )

        tag = "train" if train else "test"
        complexity_keys = {
            k: io_utils.get_abs_file_path( os.path.join( data_path, k_keys ) ) for k, k_keys in {
                1: f"{tag}_keys_graph_size_0_5.pkl",
                2: f"{tag}_keys_graph_size_5_10.pkl",
                3: f"{tag}_keys_graph_size_10_15.pkl",
                4: f"{tag}_keys_graph_size_15_20.pkl",
                5: f"{tag}_keys_graph_size_20_30.pkl",
                6: f"{tag}_keys_graph_size_30_40.pkl",
            }.items() }
        for k, k_keys_file in complexity_keys.items():
            with open( k_keys_file, "rb" ) as fp:
                complexity_keys[ k ] = pickle.load( fp )

        return complexity_keys