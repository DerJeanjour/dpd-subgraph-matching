import argparse
import json
import os

import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.model_utils as model_utils


def parse_args( use_default=False ):
    parser = argparse.ArgumentParser()

    # general
    parser.add_argument( "--seed", help="seed",
                         type=int, default=42 )
    parser.add_argument( "--lr", help="learning rate",
                         type=float, default=0.0001 )
    parser.add_argument( "--ngpu", help="number of gpu",
                         type=int, default=0 )
    parser.add_argument( "--num_workers", help="number of workers",
                         type=int, default=os.cpu_count() )
    parser.add_argument( "--dataset", help="dataset",
                         type=str, default="CPG" )
    parser.add_argument( "--directed", action="store_true", help="directed graph" )
    parser.add_argument( "--iso", action="store_true", help="wheather using iso/noniso" )
    parser.add_argument( "--anchored", action="store_true",
                         default=False, help="wheather embeddings of graphs are anchored" )
    parser.add_argument( "--device", help="torch device",
                         type=str, default=str( model_utils.get_device() ) )

    # training parameter
    parser.add_argument( "--epoch", help="epoch",
                         type=int, default=30 )
    parser.add_argument( "--batch_size", help="batch_size",
                         type=int, default=32 )
    parser.add_argument( "--tactic", help="tactic of defining number of hops",
                         type=str, default="static", choices=[ "static", "cont", "jump" ] )
    parser.add_argument( "--branch", help="choosing branch",
                         type=str, default="both", choices=[ "both", "left", "right" ] )
    parser.add_argument( "--nhop", help="number of hops",
                         type=int, default=1 )
    parser.add_argument( "--nhead", help="number of attention heads",
                         type=int, default=1 )
    parser.add_argument( "--embedding_dim", help="node embedding dim aka number of distinct node label",
                         type=int, default=20 )
    parser.add_argument( "--dropout_rate", help="dropout_rate",
                         type=float, default=0.0 )
    parser.add_argument( "--al_scale", help="attn_loss scale",
                         type=float, default=1.0 )
    parser.add_argument( "--n_graph_layer", help="number of GNN layer",
                         type=int, default=4 )
    parser.add_argument( "--d_graph_layer", help="dimension of GNN layer",
                         type=int, default=140 )
    parser.add_argument( "--n_FC_layer", help="number of FC layer",
                         type=int, default=4 )
    parser.add_argument( "--d_FC_layer", help="dimension of FC layer",
                         type=int, default=128 )

    # evaluation
    parser.add_argument( "--confidence", help="isomorphism threshold",
                         type=float, default=0.5 )
    parser.add_argument( "--mapping_threshold", help="mapping threshold",
                         type=float, default=1e-5 )

    # analysis
    parser.add_argument( "--test_data", help="If true, test dataset is used",
                         action="store_true", default=False )
    parser.add_argument( "--source", help="source graph idx for analysis",
                         type=int, default=0 )
    parser.add_argument( "--query", help="query graph idx for analysis",
                         type=int, default=0 )

    # file paths
    parser.add_argument( "--data_processed_dir", help="path to the data",
                         type=str, default="data/data_processed" )
    parser.add_argument( "--ckpt_dir", help="save directory of model parameter",
                         type=str, default="training/save/" )
    parser.add_argument( "--log_dir", help="logging directory",
                         type=str, default="training/runs/" )
    parser.add_argument( "--result_dir", help="save directory of model parameter",
                         type=str, default="evaluation/results/" )
    parser.add_argument( "--config_dir", help="Dataset config directory",
                         type=str, default="data/configs/" )
    parser.add_argument( "--dataset_dir", help="Dataset directory",
                         type=str, default="data/datasets/" )
    parser.add_argument( "--raw_dataset_dir", help="Raw dataset directory",
                         type=str, default="data/raw_datasets/" )
    parser.add_argument( "--ckpt_path", help="Path to model ckpt file",
                         type=str, default=None )
    parser.add_argument( "--train_keys", help="train keys",
                         type=str, default="train_keys.pkl" )
    parser.add_argument( "--test_keys", help="test keys",
                         type=str, default="test_keys.pkl" )
    parser.add_argument( "--tag", help="Additional tag for saving and logging folder",
                         type=str, default="" )
    parser.add_argument( "--import_dir", help="Import folder of datasets",
                         type=str, default="datasets/" )

    # generating
    parser.add_argument( "--num_subgraphs", default=2000, type=int, help="Number of subgraphs" )
    parser.add_argument( "--real", action="store_true" )
    parser.add_argument( "--testonly", action="store_true" )
    parser.add_argument( "--max_subgraph", type=int, default=-1 )
    parser.add_argument( "--import_format", help="Graph file format",
                         type=str, default=".gml" )
    parser.add_argument( "--import_subgraph_radius", help="Radius of k neighbourhood for subgraph.",
                         type=int, default=3 )
    parser.add_argument( "--import_subgraph_max", help="Max number of nodes per subgraph.",
                         type=int, default=40 )
    parser.add_argument( "--import_subgraph_min", help="Min number of nodes per subgraph.",
                         type=int, default=2 )
    parser.add_argument( "--split_data",
                         help="If true, the dataset will be split into train and test without without generating train algorithmically.",
                         action="store_true", default=False )
    parser.add_argument( "--induced",
                         help="Generate induced subgraphs. An induced subgraph S of G preserves all edges between the nodes of the graph G",
                         action="store_true", default=True )

    return parser.parse_args( "" ) if use_default else parser.parse_args()


def save_args( args, filename: str ) -> None:
    # Convert Args class attributes to a dictionary
    args_dict = { k: v for k, v in args.__dict__.items() if not k.startswith( '__' ) and not callable( v ) }

    if not filename.endswith( ".json" ):
        filename += ".json"

    # Write dictionary to JSON file
    with open( io_utils.get_abs_file_path( filename ), 'w' ) as f:
        json.dump( args_dict, f, indent=4 )


def load_args( args, filename: str ):
    if not filename.endswith( ".json" ):
        filename += ".json"

    # Read JSON file
    with open( io_utils.get_abs_file_path( filename ), 'r' ) as f:
        args_dict = json.load( f )

    # Set the attributes of Args from the dictionary
    for key, value in args_dict.items():
        setattr( args, key, value )
        # args[ key ] = value

    return args
