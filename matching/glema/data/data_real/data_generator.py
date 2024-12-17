import argparse
import json
import os
import signal
import random
from contextlib import contextmanager
from functools import reduce
from multiprocessing import Process, Queue
from random import choice, seed, shuffle

import networkx as nx
import numpy as np

from tqdm import tqdm

import matching.glema.common.utils as utils


@contextmanager
def time_limit( seconds ):
    def signal_handler( signum, frame ):
        raise Exception( "Timed out!" )

    signal.signal( signal.SIGALRM, signal_handler )
    signal.alarm( seconds )
    try:
        yield
    finally:
        signal.alarm( 0 )


def read_config( config_file ):
    with open( utils.get_abs_file_path( config_file ), "r", encoding="utf-8" ) as f:
        return json.load( f )


def ensure_path( path ):
    if not os.path.exists( path ):
        os.mkdir( path )


def read_dataset( path, ds_name ):
    ds_dir = os.path.join( path, ds_name )
    ds_dir = utils.get_abs_file_path( ds_dir )

    node_labels_file = open( os.path.join( ds_dir, ds_name + ".node_labels" ), "r" )
    edges_file = open( os.path.join( ds_dir, ds_name + ".edges" ), "r" )
    graph_idx_file = open( os.path.join( ds_dir, ds_name + ".graph_idx" ), "r" )
    pivot_file = open( os.path.join( ds_dir, ds_name + ".pivots" ), "r" )

    total_graph = nx.Graph()
    transactions_by_nid = { }

    node_labels = node_labels_file.read().strip().split( "\n" )
    label_set = sorted( set( node_labels ) )
    # Label start from 1
    label_mapping = { x: i + 1 for i, x in enumerate( label_set ) }
    node_labels = [ (i, { "label": label_mapping[ x ] }) for i, x in enumerate( node_labels ) ]
    total_graph.add_nodes_from( node_labels )

    edges = edges_file.read().strip().split( "\n" )
    edges = [
        (int( sn ) - 1, int( en ) - 1, { "label": 1 })
        for line in edges
        for sn, en in [ line.split( "," ) ]
    ]
    total_graph.add_edges_from( edges )

    nid_to_transaction = graph_idx_file.read().strip().split( "\n" )
    nid_to_transaction = { i: int( x ) - 1 for i, x in enumerate( nid_to_transaction ) }

    transaction_ids = set( nid_to_transaction.values() )
    print( "Processing transactions..." )
    for tid in tqdm( transaction_ids ):
        filtered_nid_by_transaction = list(
            y[ 0 ] for y in filter( lambda x: x[ 1 ] == tid, nid_to_transaction.items() )
        )
        transactions_by_nid[ tid ] = filtered_nid_by_transaction

    pivots_by_transaction: dict[ int, int ] = { }
    for entry in pivot_file.read().strip().split( "\n" ):
        tid = int( entry.split( " " )[ 0 ] ) - 1
        nid = int( entry.split( " " )[ 1 ] ) - 1
        pivots_by_transaction[ tid ] = nid

    return total_graph, transactions_by_nid, pivots_by_transaction


def save_per_source( graph_id, H, source_graph_mapping, source_graph_pivot, iso_subgraphs, noniso_subgraphs,
                     dataset_path ):
    # Ensure path
    subgraph_path = os.path.join( dataset_path, str( graph_id ) )
    ensure_path( subgraph_path )

    # Save source graphs
    source_graph_file = os.path.join( subgraph_path, "source.lg" )
    with open( source_graph_file, "w", encoding="utf-8" ) as file:
        file.write( "t # {0}\n".format( graph_id ) )
        file.write( "p # {0}\n".format( source_graph_pivot ) )
        for node in H.nodes:
            file.write( "v {} {}\n".format( node, H.nodes[ node ][ "label" ] ) )
        for edge in H.edges:
            file.write(
                "e {} {} {}\n".format(
                    edge[ 0 ], edge[ 1 ], H.edges[ (edge[ 0 ], edge[ 1 ]) ][ "label" ]
                )
            )

    # Save source graph mapping
    source_graph_mapping_file = os.path.join( subgraph_path, "source_mapping.lg" )
    with open( source_graph_mapping_file, "w", encoding="utf-8" ) as file:
        file.write( "t # {0}\n".format( graph_id ) )
        for original_id, source_id in source_graph_mapping.items():
            file.write( "v {} {}\n".format( source_id, original_id ) )

    # Save subgraphs
    iso_subgraph_file = os.path.join( subgraph_path, "iso_subgraphs.lg" )
    noniso_subgraph_file = os.path.join( subgraph_path, "noniso_subgraphs.lg" )
    iso_subgraph_mapping_file = os.path.join( subgraph_path, "iso_subgraphs_mapping.lg" )
    noniso_subgraph_mapping_file = os.path.join(
        subgraph_path, "noniso_subgraphs_mapping.lg"
    )

    isf = open( iso_subgraph_file, "w", encoding="utf-8" )
    ismf = open( iso_subgraph_mapping_file, "w", encoding="utf-8" )

    for subgraph_id, S in enumerate( iso_subgraphs ):
        isf.write( "t # {0}\n".format( subgraph_id ) )
        ismf.write( "t # {0}\n".format( subgraph_id ) )
        node_mapping = { }
        list_nodes = list( S.nodes )
        shuffle( list_nodes )

        for node_idx, node_emb in enumerate( list_nodes ):
            isf.write( "v {} {}\n".format( node_idx, S.nodes[ node_emb ][ "label" ] ) )
            ismf.write( "v {} {}\n".format( node_idx, node_emb ) )
            node_mapping[ node_emb ] = node_idx

        for edge in S.edges:
            edge_0 = node_mapping[ edge[ 0 ] ]
            edge_1 = node_mapping[ edge[ 1 ] ]
            isf.write(
                "e {} {} {}\n".format(
                    edge_0, edge_1, S.edges[ (edge[ 0 ], edge[ 1 ]) ][ "label" ]
                )
            )

    isf.close()
    ismf.close()

    nisf = open( noniso_subgraph_file, "w", encoding="utf-8" )
    nismf = open( noniso_subgraph_mapping_file, "w", encoding="utf-8" )
    for subgraph_id, S in enumerate( noniso_subgraphs ):
        nisf.write( "t # {0}\n".format( subgraph_id ) )
        nismf.write( "t # {0}\n".format( subgraph_id ) )
        node_mapping = { }
        list_nodes = list( S.nodes )
        shuffle( list_nodes )

        for node_idx, node_emb in enumerate( list_nodes ):
            nisf.write( "v {} {}\n".format( node_idx, S.nodes[ node_emb ][ "label" ] ) )
            if not S.nodes[ node_emb ][ "modified" ]:
                nismf.write( "v {} {}\n".format( node_idx, node_emb ) )
            node_mapping[ node_emb ] = node_idx

        for edge in S.edges:
            edge_0 = node_mapping[ edge[ 0 ] ]
            edge_1 = node_mapping[ edge[ 1 ] ]
            nisf.write(
                "e {} {} {}\n".format(
                    edge_0, edge_1, S.edges[ (edge[ 0 ], edge[ 1 ]) ][ "label" ]
                )
            )

    nisf.close()
    nismf.close()


def node_match( first_node, second_node ):
    return first_node[ "label" ] == second_node[ "label" ]


def edge_match( first_edge, second_edge ):
    return first_edge[ "label" ] == second_edge[ "label" ]


def generate_iso_subgraph( graph, pivot, no_of_nodes, avg_degree, std_degree, *args, **kwargs ):
    graph_nodes = graph.number_of_nodes()
    node_ratio = no_of_nodes / graph_nodes
    if node_ratio > 1:
        node_ratio = 1

    min_edges = int( no_of_nodes * (avg_degree - std_degree) / 2 )
    max_edges = int( no_of_nodes * (avg_degree + std_degree) / 2 )
    subgraph = None
    iteration = 0

    while (
            subgraph is None
            or subgraph.number_of_nodes() < 2
            or not nx.is_connected( subgraph )
    ):
        chose_nodes = np.random.choice(
            [ 0, 1 ], size=graph_nodes, replace=True, p=[ 1 - node_ratio, node_ratio ]
        )
        remove_nodes = list( np.where( chose_nodes == 0 )[ 0 ] )
        if pivot is not None and pivot in remove_nodes:
            remove_nodes.remove( pivot )
        subgraph = graph.copy()
        subgraph.remove_nodes_from( remove_nodes )

        iteration += 1
        if iteration > 5:
            node_ratio *= 1.05
            if node_ratio > 1:
                node_ratio = 1
            iteration = 0

    return subgraph


def remove_random_node( graph ):
    new_graph = None

    while new_graph is None or not nx.is_connected( new_graph ):
        delete_node = np.random.choice( graph.nodes )
        new_graph = graph.copy()
        new_graph.remove_node( delete_node )

    return new_graph


def remove_random_nodes( graph, num_nodes ):
    while graph.number_of_nodes() > num_nodes:
        graph = remove_random_node( graph )

    return graph


def remove_random_edge( graph ):
    new_graph = None

    while new_graph is None or not nx.is_connected( new_graph ):
        delete_edge = choice( list( graph.edges ) )
        new_graph = graph.copy()
        new_graph.remove_edge( *delete_edge )

    return new_graph


def add_random_edges( current_graph, NE, min_edges=61, max_edges=122 ):
    """
    randomly adds edges between nodes with no existing edges.
    based on: https://stackoverflow.com/questions/42591549/add-and-delete-a-random-edge-in-networkx
    :param probability_of_new_connection:
    :return: None
    """
    if current_graph:
        connected = [ ]
        for i in current_graph.nodes:
            # find the other nodes this one is connected to
            connected = connected + [ to for (fr, to) in current_graph.edges( i ) ]
            connected = list( dict.fromkeys( connected ) )
            # and find the remainder of nodes, which are candidates for new edges

        unconnected = [ j for j in current_graph.nodes if j not in connected ]
        # print('Connected:', connected)
        # print('Unconnected', unconnected)
        is_connected = nx.is_connected( current_graph )
        while not is_connected:  # randomly add edges until the graph is connected
            if len( unconnected ) == 0:
                break
            new = choice( unconnected )
            if not connected:
                old = choice( unconnected )
                while old == new:
                    old = choice( unconnected )
            else:
                old = choice( connected )
            edge_label = np.random.randint( 1, NE + 1 )

            # for visualise only
            current_graph.add_edges_from( [ (old, new, { "label": edge_label }) ] )
            current_graph.nodes[ old ][ "modified" ] = True
            # book-keeping, in case both add and remove done in same cycle
            if not connected:
                unconnected.remove( old )
                connected.append( old )

            unconnected.remove( new )
            connected.append( new )

            is_connected = nx.is_connected( current_graph )
            # print('Connected:', connected)
            # print('Unconnected', unconnected

        num_edges = np.random.randint( min_edges, max_edges + 1 )
        # target num_edges can be higher than max possible edges in a directed graph
        num_nodes = max( 0, current_graph.number_of_nodes() )
        max_num_edges = int( num_nodes * (num_nodes - 1) / 2 )
        num_edges = min( num_edges, max_num_edges )

        while current_graph.number_of_edges() < num_edges:
            old_1, old_2 = np.random.choice( current_graph.nodes, 2, replace=False )
            while current_graph.has_edge( old_1, old_2 ):
                old_1, old_2 = np.random.choice( current_graph.nodes, 2, replace=False )
            edge_label = np.random.randint( 1, NE + 1 )
            current_graph.add_edges_from( [ (old_1, old_2, { "label": edge_label }) ] )
            current_graph.nodes[ old_1 ][ "modified" ] = True
            current_graph.nodes[ old_2 ][ "modified" ] = True

    return current_graph


def add_random_nodes(
        graph,
        num_nodes,
        id_node_start,
        number_label_node,
        number_label_edge,
        min_edges,
        max_edges,
):
    graph_nodes = graph.number_of_nodes()
    number_of_possible_nodes_to_add = num_nodes - graph_nodes

    # start node_id from the number of nodes already in the common graph (note that the node ids are numbered from 0)
    node_id = id_node_start
    # so if there were 5 nodes in the common graph (0,1,2,3,4) start adding new nodes from node 5 on wards
    added_nodes = [ ]
    for i in range( number_of_possible_nodes_to_add ):
        node_label = np.random.randint( 1, number_label_node + 1 )
        added_nodes.append( (node_id, { "label": node_label, "modified": True }) )
        node_id += 1

    # add all nodes to current graph
    graph.add_nodes_from( added_nodes )
    graph = add_random_edges( graph, number_label_edge, min_edges, max_edges )
    return graph, node_id


def random_modify( graph, NN, NE, node_start_id, min_edges, max_edges ):
    num_steps_max = max( graph.number_of_nodes() + graph.number_of_edges(), 2 )
    num_steps = np.random.randint( 1, num_steps_max )
    modify_type = None

    while num_steps > 0:
        modify_type = np.random.randint( 0, 3 )

        if modify_type == 0:  # Change node label
            chose_node = np.random.choice( graph.nodes )
            origin_label = graph.nodes[ chose_node ][ "label" ]
            new_label = np.random.randint( 1, NN + 1 )
            while new_label == origin_label:
                new_label = np.random.randint( 1, NN + 1 )

            graph.nodes[ chose_node ][ "label" ] = new_label
            graph.nodes[ chose_node ][ "modified" ] = True

        # elif modify_type == 1:
        #     chose_edge = np.random.choice(graph.nodes, size=2, replace=False)
        #     while not graph.has_edge(*chose_edge):
        #         chose_edge = np.random.choice(graph.nodes, size=2, replace=False)

        #     origin_label = graph[chose_edge[0]][chose_edge[1]]["label"]
        #     new_label = np.random.randint(1, NE+1)
        #     while new_label == origin_label:
        #         new_label = np.random.randint(1, NE+1)

        #     graph[chose_edge[0]][chose_edge[1]]["label"] = new_label
        #     graph.nodes[chose_edge[0]]["modified"] = True
        #     graph.nodes[chose_edge[1]]["modified"] = True

        elif modify_type == 1:  # Remove & add random node
            graph, node_start_id = add_random_nodes(
                graph,
                graph.number_of_nodes() + 1,
                node_start_id,
                NN,
                NE,
                min_edges,
                max_edges,
            )
            graph = remove_random_nodes( graph, graph.number_of_nodes() - 1 )

        elif modify_type == 2:  # Remove & add random edge
            n_nodes = graph.number_of_nodes()
            n_edges = graph.number_of_edges()

            if n_nodes * (n_nodes - 1) / 2 > n_edges:
                graph = add_random_edges( graph, NE, n_edges + 1, n_edges + 1 )
            if graph.number_of_edges() >= n_nodes:
                graph = remove_random_edge( graph )

        num_steps -= 1

    return graph, node_start_id


def generate_noniso_subgraph(
        graph,
        pivot,
        no_of_nodes,
        avg_degree,
        std_degree,
        number_label_node,
        number_label_edge,
        *args,
        **kwargs
):
    graph_nodes = graph.number_of_nodes()
    node_ratio = no_of_nodes / graph_nodes
    if node_ratio > 1:
        node_ratio = 1

    min_edges = int( no_of_nodes * min( no_of_nodes - 1, avg_degree - std_degree ) / 2 )
    max_edges = int( no_of_nodes * min( no_of_nodes - 1, avg_degree + std_degree ) / 2 )
    subgraph = None
    iteration = 0

    while (
            subgraph is None
            or subgraph.number_of_nodes() < 2
            or not nx.is_connected( subgraph )
    ):
        chose_nodes = np.random.choice(
            [ 0, 1 ], size=graph_nodes, replace=True, p=[ 1 - node_ratio, node_ratio ]
        )
        remove_nodes = np.where( chose_nodes == 0 )[ 0 ]
        subgraph = graph.copy()
        subgraph.remove_nodes_from( remove_nodes )

        iteration += 1
        if iteration > 5:
            node_ratio *= 1.05
            if node_ratio > 1:
                node_ratio = 1
            iteration = 0
            # utils.save_graph_debug( graph, "noniso_debug.png" )

    for nid in subgraph.nodes:
        subgraph.nodes[ nid ][ "modified" ] = False

    if subgraph.number_of_nodes() > no_of_nodes:
        subgraph = remove_random_nodes( subgraph, no_of_nodes )
    elif subgraph.number_of_nodes() < no_of_nodes:
        subgraph, graph_nodes = add_random_nodes(
            subgraph,
            no_of_nodes,
            graph_nodes,
            number_label_node,
            number_label_edge,
            min_edges,
            max_edges,
        )

    subgraph, graph_nodes = random_modify(
        subgraph,
        number_label_node,
        number_label_edge,
        graph_nodes,
        min_edges,
        max_edges,
    )
    graph_matcher = nx.algorithms.isomorphism.GraphMatcher(
        graph, subgraph, node_match=node_match, edge_match=edge_match
    )

    retry = 0
    while True:
        try:
            with time_limit( 10 ):
                subgraph_is_isomorphic = graph_matcher.subgraph_is_isomorphic()
        except:
            subgraph_is_isomorphic = False

        if subgraph_is_isomorphic:
            subgraph, graph_nodes = random_modify(
                subgraph,
                number_label_node,
                number_label_edge,
                graph_nodes,
                min_edges,
                max_edges,
            )
            graph_matcher = nx.algorithms.isomorphism.GraphMatcher(
                graph, subgraph, node_match=node_match, edge_match=edge_match
            )
            retry += 1
            if retry > 2:
                return None
        else:
            break

    return subgraph


def generate_subgraphs( graph, number_subgraph_per_source, progress_queue, source_graph_pivot, *args, **kwargs ):
    list_iso_subgraphs = [ ]
    list_noniso_subgraphs = [ ]

    # for _ in tqdm( range( number_subgraph_per_source ) ):
    for i in range( number_subgraph_per_source ):

        generated_subgraph = None
        while generated_subgraph is None:
            no_of_nodes = np.random.randint( 2, graph.number_of_nodes() + 1 )
            prob = np.random.randint( 0, 2 )
            if prob == 1:
                # print( f"Generate iso subgraph {i}/{number_subgraph_per_source}" )
                generated_subgraph = generate_iso_subgraph(
                    graph, source_graph_pivot, no_of_nodes, *args, **kwargs
                )
            else:
                # print( f"Generate non iso subgraph {i}/{number_subgraph_per_source}" )
                generated_subgraph = generate_noniso_subgraph(
                    graph, source_graph_pivot, no_of_nodes, *args, **kwargs
                )

        if prob == 1:
            list_iso_subgraphs.append( generated_subgraph )
        else:
            list_noniso_subgraphs.append( generated_subgraph )

        progress_queue.put( 1 )

    return list_iso_subgraphs, list_noniso_subgraphs


def generate_one_sample( idx, progress_queue, number_subgraph_per_source,
                         source_graphs, source_graph_mappings, source_graph_pivots,
                         *arg, **kwarg ):
    source_graph = source_graphs[ idx ]
    source_graph_mapping = source_graph_mappings[ idx ]
    source_graph_pivot = source_graph_pivots[ idx ]
    iso_subgraphs, noniso_subgraphs = generate_subgraphs(
        source_graph, number_subgraph_per_source, progress_queue, source_graph_pivot, *arg, **kwarg
    )

    return source_graph, source_graph_mapping, source_graph_pivot, iso_subgraphs, noniso_subgraphs


def generate_batch( start_idx, stop_idx, number_source, dataset_path, progress_queue, *args, **kwargs ):
    for idx in range( start_idx, stop_idx ):
        # print( "SAMPLE %d/%d" % (idx + 1, number_source) )
        graph, mapping, pivot, iso_subgraphs, noniso_subgraphs = generate_one_sample(
            idx, progress_queue, *args, **kwargs
        )
        save_per_source( idx, graph, mapping, pivot, iso_subgraphs, noniso_subgraphs, dataset_path )


def generate_dataset( dataset_path, is_continue, number_source, num_process, num_subgraphs, *args, **kwargs ):
    print( "Generating..." )
    list_processes = [ ]
    progress_queue = Queue()

    if is_continue is not False:
        print( "Continue generating..." )
        generated_sample = os.listdir( dataset_path )
        generated_sample = [ int( x ) for x in generated_sample ]
        remaining_sample = np.array(
            sorted( set( range( number_source ) ) - set( generated_sample ) )
        )
        gap_list = remaining_sample[ 1: ] - remaining_sample[ :-1 ]
        gap_idx = np.where( gap_list > 1 )[ 0 ] + 1
        if len( gap_idx ) < 1:
            list_idx = [ (remaining_sample[ 0 ], remaining_sample[ -1 ] + 1) ]
        else:
            list_idx = (
                    [ (remaining_sample[ 0 ], remaining_sample[ gap_idx[ 0 ] ]) ]
                    + [
                        (remaining_sample[ gap_idx[ i ] ], remaining_sample[ gap_idx[ i + 1 ] ])
                        for i in range( gap_idx.shape[ 0 ] - 1 )
                    ]
                    + [ (remaining_sample[ gap_idx[ -1 ] ], remaining_sample[ -1 ] + 1) ]
            )

        for start_idx, stop_idx in list_idx:
            list_processes.append(
                Process(
                    target=generate_batch,
                    args=(start_idx, stop_idx, number_source, dataset_path, progress_queue),
                    kwargs=kwargs,
                )
            )

    else:
        batch_size = int( number_source / num_process ) + 1
        start_idx = 0
        stop_idx = start_idx + batch_size

        for idx in range( num_process ):
            list_processes.append(
                Process(
                    target=generate_batch,
                    args=(start_idx, stop_idx, number_source, dataset_path, progress_queue),
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
    total_subgraphs = number_source * num_subgraphs
    with tqdm( total=total_subgraphs ) as pbar:
        processed_count = 0
        while processed_count < total_subgraphs:
            # Wait for a progress update
            progress_queue.get()
            processed_count += 1
            pbar.update()

    for p in list_processes:
        p.join()


def separate_graphs( total_graph, transaction_by_id, pivots_by_transaction ):
    separeted_graphs = { }
    separeted_graphs_mapping = { }
    separeted_graphs_pivot = { }
    for gid in transaction_by_id:
        G = total_graph.subgraph( transaction_by_id[ gid ] )
        mapping = dict( zip( G, range( G.number_of_nodes() ) ) )
        source_graph = nx.relabel_nodes( G, mapping )
        # source_graph = G
        unique_labels = set( nx.get_node_attributes( source_graph, "label" ).values() )
        label_mapping = { x: i + 1 for i, x in enumerate( unique_labels ) }
        for nid in source_graph.nodes:
            source_graph.nodes[ nid ][ "label" ] = label_mapping[
                source_graph.nodes[ nid ][ "label" ]
            ]

        separeted_graphs[ gid ] = source_graph
        separeted_graphs_mapping[ gid ] = mapping
        separeted_graphs_pivot[ gid ] = mapping[ pivots_by_transaction[ gid ] ]
        # utils.save_graph_debug( source_graph, f"source_{gid}.png" )

    return separeted_graphs, separeted_graphs_mapping, separeted_graphs_pivot


def calculate_ds_attr( graph_ds, total_graph, num_subgraphs ):
    """
    "number_source": 1000,
    "avg_source_size": 60,
    "std_source_size": 10,
    "avg_degree": 3.5,
    "std_degree": 0.5,
    "number_label_node": 20
    """

    attr_dict = { }
    attr_dict[ "number_source" ] = len( graph_ds )
    list_source_node = [ g.number_of_nodes() for _, g in graph_ds.items() ]
    total_label = reduce(
        lambda x, y: x + [ y.nodes[ nid ][ "label" ] for nid in y.nodes ],
        graph_ds.values(),
        [ ],
    )
    list_source_edge = [ g.number_of_edges() for _, g in graph_ds.items() ]

    mean_size, std_size = np.mean( list_source_node, axis=0 ), np.std(
        list_source_node, axis=0
    )
    attr_dict[ "avg_source_size" ] = mean_size
    attr_dict[ "std_source_size" ] = std_size

    list_avg_degree = [ e * 2 / n for n, e in zip( list_source_node, list_source_edge ) ]
    mean_degree, std_degree = np.mean( list_avg_degree, axis=0 ), np.std(
        list_avg_degree, axis=0
    )
    attr_dict[ "avg_degree" ] = mean_degree
    attr_dict[ "std_degree" ] = std_degree

    # total_label = [total_graph.nodes[n]["label"] for n in total_graph.nodes]
    attr_dict[ "number_label_node" ] = len( set( total_label ) )
    attr_dict[ "number_label_edge" ] = 1  # TO_REMOVE

    attr_dict[ "number_subgraph_per_source" ] = num_subgraphs  # TO_REMOVE
    return attr_dict


def save_config_for_synthesis( config_path, ds_name, configs ):
    configs[ "number_source" ] *= 4  # generate train data 4 times the test size
    with open( utils.get_abs_file_path( f"{config_path}{ds_name}.json" ), "w" ) as f:
        json.dump( configs, f, indent=4 )


def split_source_graphs( source_graphs, source_graph_mappings, source_graph_pivots, train_split_ration=0.8 ):
    # Shuffle keys and split them
    keys = list( source_graphs.keys() )
    random.shuffle( keys )
    split_index = int( len( keys ) * train_split_ration )

    train_keys = keys[ :split_index ]
    test_keys = keys[ split_index: ]

    # Create train and test dictionaries with reindexed keys
    train_data = { i: source_graphs[ k ] for i, k in enumerate( train_keys ) }
    test_data = { i: source_graphs[ k ] for i, k in enumerate( test_keys ) }
    train_data_mapping = { i: source_graph_mappings[ k ] for i, k in enumerate( train_keys ) }
    test_data_mapping = { i: source_graph_mappings[ k ] for i, k in enumerate( test_keys ) }
    train_data_pivots = { i: source_graph_pivots[ k ] for i, k in enumerate( train_keys ) }
    test_data_pivots = { i: source_graph_pivots[ k ] for i, k in enumerate( test_keys ) }

    return train_data, test_data, train_data_mapping, test_data_mapping, train_data_pivots, test_data_pivots


def process_dataset( args, path, ds_name, is_continue, num_subgraphs ):
    total_graph, transaction_by_nid, pivots_by_transaction = read_dataset( path, ds_name )
    (source_graphs,
     source_graph_mapping,
     source_graph_pivots) = separate_graphs( total_graph, transaction_by_nid, pivots_by_transaction )
    config = calculate_ds_attr( source_graphs, total_graph, num_subgraphs )
    dataset_name = os.path.basename( ds_name ).split( "." )[ 0 ]

    del total_graph
    del transaction_by_nid

    source_graphs_test = source_graphs
    source_graphs_test_mapping = source_graph_mapping
    source_graphs_test_pivots = source_graph_pivots
    if args.split_data:
        print( f"Splitting dataset with size {len( source_graphs )} into test and train data ..." )
        (source_graphs_train, source_graphs_test,
         source_graphs_train_mapping, source_graphs_test_mapping,
         source_graphs_train_pivots, source_graphs_test_pivots) = split_source_graphs( source_graphs,
                                                                                       source_graph_mapping,
                                                                                       source_graph_pivots )

        config[ "number_source" ] = len( source_graphs_train )
        print( f"Processing train data of size {len( source_graphs_train )} ..." )
        dataset_path_train = os.path.join( args.dataset_dir, dataset_name + "_train" )
        dataset_path_train = utils.get_abs_file_path( dataset_path_train )
        ensure_path( dataset_path_train )
        generate_dataset(
            dataset_path=dataset_path_train,
            is_continue=is_continue,
            source_graphs=source_graphs_train,
            source_graph_mappings=source_graphs_train_mapping,
            source_graph_pivots=source_graphs_train_pivots,
            num_process=args.num_workers,
            num_subgraphs=num_subgraphs,
            **config
        )

    config[ "number_source" ] = len( source_graphs_test )
    print( f"Processing test data of size {len( source_graphs_test )} ..." )
    dataset_path_test = os.path.join( args.dataset_dir, dataset_name + "_test" )
    dataset_path_test = utils.get_abs_file_path( dataset_path_test )
    ensure_path( dataset_path_test )
    generate_dataset(
        dataset_path=dataset_path_test,
        is_continue=is_continue,
        source_graphs=source_graphs_test,
        source_graph_mappings=source_graphs_test_mapping,
        source_graph_pivots=source_graphs_test_pivots,
        num_process=args.num_workers,
        num_subgraphs=num_subgraphs,
        **config
    )

    save_config_for_synthesis( args.config_dir, ds_name, config )


def process( args ):
    utils.set_seed( args.seed )

    raw_dataset_dir = utils.get_abs_file_path( args.raw_dataset_dir )
    list_datasets = os.listdir( raw_dataset_dir )

    for dataset in list_datasets:
        if dataset != args.dataset:
            continue  # TO_TEST

        print( "Processing dataset:", dataset )
        process_dataset(
            args=args,
            path=raw_dataset_dir,
            ds_name=dataset,
            is_continue=args.cont,
            num_subgraphs=args.num_subgraphs,
        )


if __name__ == "__main__":
    args = utils.parse_args()
    # args.dataset = "CPG"
    # args.split_data = True
    # args.num_workers = 1
    # args.num_subgraphs = 20  # 2000
    # print( args )
    process( args )
