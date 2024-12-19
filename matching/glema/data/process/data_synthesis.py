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


def read_config( config_file ):
    with open( io_utils.get_abs_file_path( config_file ), "r", encoding="utf-8" ) as f:
        return json.load( f )


def ensure_path( path ):
    if not os.path.exists( path ):
        os.mkdir( path )


def add_features( graph, NN, NE ):
    nodes = np.array( list( graph.nodes ) )
    edges = np.array( list( graph.edges ) )

    node_labels = np.random.randint( 1, NN + 1, len( nodes ) ).tolist()
    edge_labels = np.random.randint( 1, NE + 1, len( edges ) ).tolist()

    labelled_nodes = [
        (nodes[ k ], { "label": node_labels[ k ], "color": "green" })
        for k in range( len( nodes ) )
    ]
    labelled_edges = [
        (edges[ k ][ 0 ], edges[ k ][ 1 ], { "label": edge_labels[ k ], "color": "green" })
        for k in range( len( edges ) )
    ]

    G = nx.Graph()
    G.add_nodes_from( labelled_nodes )
    G.add_edges_from( labelled_edges )

    # add anchor by pagerank score
    anchor = graph_utils.top_pr_ranked_node( G )
    for _, data in G.nodes( data=True ):
        data[ "anchor" ] = 0
    G.nodes[ anchor ][ "anchor" ] = 1

    return G, anchor


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
    generated_pattern = None
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
            generated_pattern is None
            or nx.is_empty( generated_pattern )
            or not nx.is_connected( generated_pattern )
    ):  # make sure the generated graph is connected
        generated_pattern = nx.erdos_renyi_graph(
            no_of_nodes, probability_for_edge_creation, directed=False
        )
        iteration += 1
        if iteration > 5:
            probability_for_edge_creation *= 1.05
            iteration = 0

    labelled_pattern, pattern_anchor = add_features(
        generated_pattern, number_label_node, number_label_edge
    )

    pattern_mapping = { node_id: node_id for node_id in labelled_pattern.nodes() }

    iso_subgraphs, noniso_subgraphs = generator.generate_subgraphs(
        labelled_pattern,
        number_subgraph_per_source,
        progress_queue,
        pattern_anchor,
        induced,
        avg_degree,
        std_degree,
        number_label_node,
        number_label_edge
    )
    return labelled_pattern, pattern_mapping, pattern_anchor, iso_subgraphs, noniso_subgraphs


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
