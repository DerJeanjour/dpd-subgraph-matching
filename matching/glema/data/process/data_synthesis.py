import json
import os
from multiprocessing import Process, Queue

import networkx as nx
import numpy as np
from tqdm import tqdm

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.data.process.data_generator as generator
import matching.misc.cpg_const as cpg_const


def read_config( config_file ):
    with open( io_utils.get_abs_file_path( config_file ), "r", encoding="utf-8" ) as f:
        return json.load( f )


def ensure_path( path ):
    if not os.path.exists( path ):
        os.mkdir( path )


def add_features( graph, NN, NE, strict_edges=False ):
    # add anchor by pagerank score
    anchor = graph_utils.top_pr_ranked_node( graph )
    for _, data in graph.nodes( data=True ):
        data[ "anchor" ] = 0
    graph.nodes[ anchor ][ "anchor" ] = 1

    # add node labels
    bft_tree = nx.traversal.dfs_tree( nx.Graph( graph ), anchor )
    even_depth_label = misc_utils.get_enum_idx( cpg_const.NodeLabel.RECORD )
    odd_depth_labels = [ i for i in list( range( 1, NN + 1 ) ) if i != even_depth_label ]
    stack = [ (anchor, 0) ]
    while stack:
        current_node, depth = stack.pop()
        if depth % 2 == 0:
            graph.nodes[ current_node ][ "label" ] = even_depth_label
        else:
            graph.nodes[ current_node ][ "label" ] = np.random.choice( odd_depth_labels )
        for child in bft_tree.successors( current_node ):
            stack.append( (child, depth + 1) )

    # add edge labels and optionally remove invalid edges
    edges_to_remove = list()
    for source_nid, target_nid, edata in graph.edges( data=True ):

        source_label = graph.nodes[ source_nid ][ "label" ]
        target_label = graph.nodes[ target_nid ][ "label" ]

        if source_label == even_depth_label and target_label == even_depth_label:
            edges_to_remove.append( (source_nid, target_nid) )
            continue
        if source_label in odd_depth_labels and target_label in odd_depth_labels:
            edges_to_remove.append( (source_nid, target_nid) )
            continue

        edata[ "label" ] = np.random.randint( 1, NE + 1, 1 )[ 0 ]
    if strict_edges:
        for source_nid, target_nid in edges_to_remove:
            graph.remove_edge( source_nid, target_nid )

    return graph, anchor


def generate_connected_graph(
        avg_source_size,
        std_source_size,
        avg_degree,
        std_degree,
        directed=False ):
    graph = None
    iteration = 0
    no_of_nodes = int( np.random.normal( avg_source_size, std_source_size ) )
    while no_of_nodes < 2:
        no_of_nodes = int( np.random.normal( avg_source_size, std_source_size ) )
    degree = np.random.normal( avg_degree, std_degree )
    if degree < 1:
        degree = 1
    if degree > no_of_nodes - 1:
        degree = no_of_nodes - 1
    probability_for_edge_creation = degree / (no_of_nodes - 1)

    while (
            graph is None
            or nx.is_empty( graph )
            or not graph_utils.is_connected( graph )
    ):  # make sure the generated graph is connected
        graph = nx.erdos_renyi_graph(
            no_of_nodes, probability_for_edge_creation, directed=directed
        )
        iteration += 1
        if iteration > 5:
            probability_for_edge_creation *= 1.05
            iteration = 0

    return graph


def generate_one_sample(
        progress_queue,
        induced,
        number_subgraph_per_source,
        avg_source_size,
        std_source_size,
        avg_degree,
        std_degree,
        number_label_node,
        number_label_edge,
):
    graph = generate_connected_graph( avg_source_size, std_source_size,
                                      avg_degree, std_degree )
    graph, graph_anchor = add_features( graph,
                                        number_label_node,
                                        number_label_edge,
                                        strict_edges=True )
    graph_mapping = { node_id: node_id for node_id in graph.nodes() }

    if not nx.is_connected( graph ):
        print( "Graph is not connected!" )
        raise ValueError

    iso_subgraphs, noniso_subgraphs = generator.generate_subgraphs(
        graph,
        number_subgraph_per_source,
        progress_queue,
        graph_anchor,
        induced,
        avg_degree,
        std_degree,
        number_label_node,
        number_label_edge
    )
    return graph, graph_mapping, graph_anchor, iso_subgraphs, noniso_subgraphs


def generate_batch( start_idx, stop_idx, number_source, dataset_path, progress_queue, induced, *args, **kwargs ):
    for idx in range( start_idx, stop_idx ):
        (graph, mapping, anchor,
         iso_subgraphs, noniso_subgraphs) = generate_one_sample( progress_queue=progress_queue,
                                                                 induced=induced,
                                                                 *args, **kwargs )
        generator.save_per_source( idx, graph, mapping, anchor, iso_subgraphs, noniso_subgraphs, dataset_path )


def generate_dataset( dataset_path, number_source, num_process, induced, *args, **kwargs ):
    print( f"Generating {dataset_path} using {num_process} processes..." )
    list_processes = [ ]
    progress_queue = Queue()

    batch_size = int( number_source / num_process ) + 1
    start_idx = 0
    stop_idx = start_idx + batch_size

    for idx in range( num_process ):
        list_processes.append(
            Process(
                target=generate_batch,
                args=(start_idx, stop_idx, number_source, dataset_path, progress_queue, induced),
                kwargs=kwargs,
            )
        )

        start_idx = stop_idx
        stop_idx += batch_size
        if stop_idx > number_source:
            stop_idx = number_source

    for p in list_processes:
        p.start()

    # Track progress using tqdm
    total_subgraphs = number_source * kwargs[ "number_subgraph_per_source" ]
    with tqdm( total=total_subgraphs ) as pbar:
        processed_count = 0
        while processed_count < total_subgraphs:
            # Wait for a progress update
            progress_queue.get()
            processed_count += 1
            pbar.update()

    for p in list_processes:
        p.join()


# generate synthetic subgraphs (iso and no-iso) from config file
def process( args ):
    config_file = f"{args.config_dir}{args.dataset}.json"

    misc_utils.set_seed( args.seed )
    dataset_path = os.path.join( args.dataset_dir,
                                 os.path.basename( config_file ).split( "." )[ 0 ] + "_train" )
    dataset_path = io_utils.get_abs_file_path( dataset_path )
    ensure_path( dataset_path )
    config = read_config( config_file )
    print( config )

    generate_dataset( dataset_path=dataset_path,
                      num_process=args.num_workers,
                      induced=args.induced,
                      **config )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    # args.dataset = "CPG"
    # args.num_workers = 1
    # args.induced = True
    # print( args )
    process( args )
