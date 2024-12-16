import argparse
import os
from collections import defaultdict
from pathlib import Path
import json
from enum import Enum
import matplotlib.pyplot as plt

import networkx as nx
import torch
import torch.nn as nn

import matching.misc.utils as utils
import matching.misc.cpg_const as cpg_const


def get_abs_file_path( project_file_path: str, with_subproject=True ) -> str:
    local_path: Path = Path( project_file_path )
    if with_subproject:
        local_path = "glema" / local_path
    return utils.get_abs_file_path( str( local_path ) )


def get_project_root() -> Path:
    return utils.get_project_root()


def path_exists( path ):
    return utils.path_exists( path )


def delete_path( path: str, dry_run=False ):
    utils.delete_path( path, dry_run=dry_run )


def delete_paths( paths: list[ str ], dry_run=False ):
    utils.delete_paths( paths, dry_run=dry_run )


def get_filenames_in_dir( dir_path ):
    return utils.get_filenames_in_dir( dir_path )


def get_timestamp() -> str:
    return utils.get_timestamp()


def set_seed( seed ):
    utils.set_seed( seed )


def get_device( force_cpu=False ) -> torch.device:
    return utils.get_device( force_cpu=force_cpu )


def model_uses_cuda( model: torch.nn.Module ) -> bool:
    return utils.model_uses_cuda( model )


def generate_graph( size: int, directed: bool ):
    return utils.generate_graph( size, directed )


def random_subgraph( G, k: int ):
    return utils.random_subgraph( G, k )


def inject_edge_errors( G, e: int = 1 ):
    return utils.inject_edge_errors( G, e )


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
    parser.add_argument( "--device", help="torch device",
                         type=str, default=str( get_device() ) )

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
                         type=str, default="data/data_real/configs/" )
    parser.add_argument( "--dataset_dir", help="Dataset directory",
                         type=str, default="data/data_real/datasets/" )
    parser.add_argument( "--raw_dataset_dir", help="Raw dataset directory",
                         type=str, default="data/data_real/raw_datasets/" )
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
    parser.add_argument( "--cont", action="store_true", help="Continue generating" )
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

    return parser.parse_args( "" ) if use_default else parser.parse_args()


def ensure_dir( dir, args ):
    dir = os.path.join( dir, f"{args.dataset}_{args.tactic}" )
    if args.tactic == "static":
        dir += f"{args.nhop}"

    if args.nhead > 1:
        dir += f"_nhead{args.nhead}"

    if args.branch != "both":
        dir += "_" + args.branch

    if args.directed:
        dir += "_directed"

    if args.tag != "":
        dir += "_" + args.tag

    dir = get_abs_file_path( dir )

    # make save dir if it doesn't exist
    if not os.path.isdir( dir ):
        os.system( "mkdir -p " + dir )

    return dir


def plot_graph(
        graph: nx.Graph,
        nodeLabels=None,
        with_label=True,
        nodeColors=None,
        edgeColors=None,
        title=None,
):
    utils.plot_graph( graph, nodeLabels=nodeLabels, with_label=with_label, nodeColors=nodeColors, edgeColors=edgeColors,
                      title=title )


def write_graphs( graphs, out_file_name ):
    with open( get_abs_file_path( out_file_name ), "w", encoding="utf-8" ) as f:
        for i, g in enumerate( graphs ):
            f.write( "t # %d\n" % i )
            node_mapping = { }
            for nid, nod in enumerate( g.nodes ):
                f.write( "v %d %d\n" % (nid, g.nodes[ nod ][ "label" ]) )
                node_mapping[ nod ] = nid

            for nod1, nod2 in g.edges:
                nid1 = node_mapping[ nod1 ]
                nid2 = node_mapping[ nod2 ]
                f.write( "e %d %d %d\n" % (nid1, nid2, g.edges[ (nod1, nod2) ][ "label" ]) )


def read_mapping( mapping_file, sg2g=False ):
    mapping = dict()
    with open( get_abs_file_path( mapping_file ), "r", encoding="utf-8" ) as f:
        lines = [ line.strip() for line in f.readlines() ]
        tmapping, graph_cnt = None, 0
        for i, line in enumerate( lines ):
            cols = line.split( " " )
            if cols[ 0 ] == "t":
                if tmapping is not None:
                    mapping[ graph_cnt ] = tmapping
                    tmapping = None
                if cols[ -1 ] == "-1":
                    break

                tmapping = defaultdict( lambda: -1 )
                graph_cnt = int( cols[ 2 ] )

            elif cols[ 0 ] == "v":
                if sg2g:
                    tmapping[ int( cols[ 1 ] ) ] = int( cols[ 2 ] )
                else:
                    tmapping[ int( cols[ 2 ] ) ] = int( cols[ 1 ] )

        # adapt to input files that do not end with 't # -1'
        if tmapping is not None:
            mapping[ graph_cnt ] = tmapping

    return mapping


def read_graphs( database_file_name, directed=False ):
    graphs = dict()
    max_size = 0
    with open( get_abs_file_path( database_file_name ), "r", encoding="utf-8" ) as f:
        lines = [ line.strip() for line in f.readlines() ]
        tgraph, graph_cnt = None, 0
        graph_size = 0
        for i, line in enumerate( lines ):
            cols = line.split( " " )
            if cols[ 0 ] == "t":
                if tgraph is not None:
                    graphs[ graph_cnt ] = tgraph
                    if max_size < graph_size:
                        max_size = graph_size
                    graph_size = 0
                    tgraph = None
                if cols[ -1 ] == "-1":
                    break

                tgraph = nx.DiGraph() if directed else nx.Graph()
                graph_cnt = int( cols[ 2 ] )

            elif cols[ 0 ] == "v":
                tgraph.add_node( int( cols[ 1 ] ), label=int( cols[ 2 ] ) )
                graph_size += 1

            elif cols[ 0 ] == "e":
                tgraph.add_edge( int( cols[ 1 ] ), int( cols[ 2 ] ), label=int( cols[ 3 ] ) )

        # adapt to input files that do not end with 't # -1'
        if tgraph is not None:
            graphs[ graph_cnt ] = tgraph
            if max_size < graph_size:
                max_size = graph_size

    return graphs


def initialize_model( model, device, load_save_file: str = None ):
    if not load_save_file is None:
        print( f"Loading model from {load_save_file} ..." )
        model.load_state_dict(
            torch.load( get_abs_file_path( load_save_file ), map_location=device, weights_only=True )
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


def onehot_encoding( label_idx, max_labels ):
    onehot_vector = [ 0 ] * max_labels
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


def node_feature( graph, node_idx, max_nodes ):
    node = graph.nodes[ node_idx ]
    return onehot_encoding( node[ "label" ], max_nodes )


# Function to save Args to JSON
def save_args( args, filename: str ) -> None:
    # Convert Args class attributes to a dictionary
    args_dict = { k: v for k, v in args.__dict__.items() if not k.startswith( '__' ) and not callable( v ) }

    if not filename.endswith( ".json" ):
        filename += ".json"

    # Write dictionary to JSON file
    with open( get_abs_file_path( filename ), 'w' ) as f:
        json.dump( args_dict, f, indent=4 )


# Function to load Args from JSON
def load_args( args, filename: str ):
    if not filename.endswith( ".json" ):
        filename += ".json"

    # Read JSON file
    with open( get_abs_file_path( filename ), 'r' ) as f:
        args_dict = json.load( f )

    # Set the attributes of Args from the dictionary
    for key, value in args_dict.items():
        setattr( args, key, value )
        # args[ key ] = value

    return args


def get_enum_idx( enum_member: Enum ) -> int:
    """
    Gets the index (ordinal position) of an enum member in its Enum class.

    Args:
        enum_member (Enum): The enum member to find the index for.

    Returns:
        int: The index of the enum member in the Enum.
    """
    enum_class = enum_member.__class__
    return list( enum_class ).index( enum_member ) + 1  # idx starts with 1


def get_enum_by_idx( enum_class: Enum, idx: int ) -> Enum:
    """
    Gets the enum member corresponding to the given index (1-based) in the Enum class.

    Args:
        enum_class (Enum): The Enum class to search.
        idx (int): The 1-based index of the enum member.

    Returns:
        Enum: The enum member corresponding to the index.

    Raises:
        IndexError: If the index is out of range.
    """
    # Enum members as a list
    members = list( enum_class )
    if 1 <= idx <= len( members ):
        return members[ idx - 1 ]  # Convert 1-based index to 0-based
    raise IndexError( "Index out of range for the Enum class." )


def save_graph_debug( G, file_name ):
    file_path = get_abs_file_path( "debug/" )
    if not os.path.exists( file_path ):
        os.mkdir( file_path )
    file_path = os.path.join( file_path, file_name )
    try:
        # Set up the plot
        plt.figure( figsize=(8, 8) )
        plt.axis( 'off' )  # Turn off axis

        # Draw the graph without labels
        pos = nx.spring_layout( G )  # Compute layout
        nx.draw( G, pos, with_labels=False, node_color="lightblue", edge_color="gray", node_size=500 )

        # Save to file
        plt.savefig( file_path, format='png', bbox_inches='tight' )
        print( f"Graph rendered and saved to {file_path}" )
    except Exception as e:
        print( f"An error occurred while rendering the graph: {e}" )
    finally:
        plt.close()  # Ensure the plot is closed to free memory


def mark_pivot( args, G, source_graph_idx, mapping: [ int, int ] = None ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    database_file = f"{args.dataset_dir}/{dataset}/{source_graph_idx}/source.lg"
    with open( get_abs_file_path( database_file ), "r", encoding="utf-8" ) as f:
        lines = [ line.strip() for line in f.readlines() ]
        for i, line in enumerate( lines ):
            cols = line.split( " " )
            if i > 2:
                return
            if cols[ 0 ] == "p":
                pivot = int( cols[ -1 ] )
                if mapping is not None:
                    pivot = mapping[ pivot ]
                for node_id, node_data in G.nodes( data=True ):
                    node_data[ "pivot" ] = int( node_id ) == pivot


def load_source_mapping( args, source_graph_idx, flip=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    mapping = read_mapping(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/source_mapping.lg" )[ source_graph_idx ]
    return flip_key_values( mapping ) if flip else mapping


def load_source_graph( args, source_graph_idx, relabel=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"

    source = read_graphs(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/source.lg",
        directed=args.directed )[ source_graph_idx ]

    mark_pivot( args, source, source_graph_idx )
    if relabel:
        source_mapping = load_source_mapping( args, source_graph_idx )
        source = nx.relabel_nodes( source, source_mapping )
    return source


def load_query_id_mapping( args, source_graph_idx, query_subgraph_idx, flip=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    mapping = read_mapping(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/{'non' if not args.iso else ''}iso_subgraphs_mapping.lg" )[
        query_subgraph_idx ]
    return flip_key_values( mapping ) if flip else mapping


def load_query( args, source_graph_idx, query_subgraph_idx, relabel=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    query = read_graphs(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/{'non' if not args.iso else ''}iso_subgraphs.lg",
        directed=args.directed )[ query_subgraph_idx ]

    if relabel:
        query_id_mapping = load_query_id_mapping( args, source_graph_idx, query_subgraph_idx )
        source_id_mapping = load_source_mapping( args, source_graph_idx )
        query = nx.relabel_nodes( query, query_id_mapping )
        mark_pivot( args, query, source_graph_idx )
        query = nx.relabel_nodes( query, source_id_mapping )
    return query


def get_record_scopes( args ) -> dict[ str, str ]:
    record_scopes = { }
    record_scope_filepath = os.path.join( args.raw_dataset_dir, args.dataset, args.dataset + ".record_scopes" )
    record_scope_filepath = get_abs_file_path( record_scope_filepath )
    record_scope_file = open( record_scope_filepath, "r" )
    for idx, record_scope in enumerate( record_scope_file.read().strip().split( "\n" ) ):
        record_scopes[ str( idx ) ] = record_scope
    return record_scopes


def get_design_patterns( args ) -> dict[ str, str ]:
    design_patterns = { }
    pattern_type_filepath = os.path.join( args.raw_dataset_dir, args.dataset, args.dataset + ".pattern_types" )
    pattern_type_filepath = get_abs_file_path( pattern_type_filepath )
    pattern_type_file = open( pattern_type_filepath, "r" )
    for row in list( pattern_type_file.read().strip().split( "\n" ) ):
        node_id = row.split( " " )[ 0 ]
        pattern_type = row.split( " " )[ 1 ]
        design_patterns[ str( int( node_id ) - 1 ) ] = pattern_type
    return design_patterns


def map_node_label_idx( node_id, node_data,
                        record_scopes=None,
                        design_patterns=None ):
    node_label_idx = node_data[ "label" ]
    record_type = get_enum_by_idx( cpg_const.NodeLabel, node_label_idx )
    label = f"<{str( node_id )}>"

    if "pivot" in node_data and bool( node_data[ "pivot" ] ):
        label = f"{label}\n[PIVOT]"

    if record_type == cpg_const.NodeLabel.RECORD:
        node_id = str( node_id )
        if record_scopes is not None:
            label = f"{label}\n[{record_scopes[ node_id ]}]"
        if design_patterns is not None and node_id in design_patterns:
            label = f"{label}\n[{design_patterns[ node_id ]}]"
    else:
        label = f"{label}\n[{record_type.value}]"

    return label


def get_node_labels( G, record_scopes=None, design_patterns=None ):
    label_args = {
        "record_scopes": record_scopes,
        "design_patterns": design_patterns
    }
    return { node_id: map_node_label_idx( node_id, data, **label_args ) for node_id, data in G.nodes( data=True ) }


def combine_graph( source, query, matching_colors: dict[ int, str ] = None ):
    # Create a combined graph
    combined_graph = nx.compose( source, query )

    # Determine node colors
    node_matching = [ ]
    for node_id, node_data in combined_graph.nodes( data=True ):
        if node_id in query.nodes and node_id in source.nodes:
            if "pivot" in node_data and bool( node_data["pivot"] ):
                node_matching.append( 2 ) # Node is pivot
            else:
                node_matching.append( 1 )  # Nodes in both source and query
        elif node_id in query.nodes:
            node_matching.append( -1 )  # Nodes only in query
        else:
            node_matching.append( 0 )  # Nodes only in source

    # Determine edge colors
    edge_matching = [ ]
    for edge in combined_graph.edges():
        if edge in query.edges and edge in source.edges:
            edge_matching.append( 1 )  # Edges in both source and query
        elif edge in query.edges:
            edge_matching.append( -1 )  # Edges only in query
        else:
            edge_matching.append( 0 )  # Edges only in source
    if matching_colors is None:
        return combined_graph, node_matching, edge_matching

    node_colors = [ matching_colors[ matching ] for matching in node_matching ]
    edge_colors = [ matching_colors[ matching ] for matching in edge_matching ]
    return combined_graph, node_colors, edge_colors


def get_shape_of_tensors( input_tensors ):
    return [ tuple( tensor.shape ) for tensor in input_tensors ]


def flip_key_values( data: dict[ any, any ] ) -> dict[ any, any ]:
    flipped_data = { }
    for key, value in data.items():
        flipped_data[ value ] = key
    return flipped_data
