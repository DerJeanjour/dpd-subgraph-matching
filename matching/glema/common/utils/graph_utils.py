import os
from collections import defaultdict

import networkx as nx
import numpy as np
from tqdm import tqdm

import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.common.utils.plot_utils as plot_utils
import matching.misc.cpg_const as cpg_const
import matching.misc.utils as utils


def generate_graph( size: int, directed: bool ):
    return utils.generate_graph( size, directed )


def random_subgraph( G, k: int ):
    return utils.random_subgraph( G, k )


def subgraph( G, n, k: int ):
    return nx.ego_graph( G, n, radius=k, undirected=True )


def subgraph_from_anchor_of_size( G, max_n ):
    anchor = get_anchor( G )
    bfs = nx.bfs_tree( G.to_undirected(), source=anchor )
    subset = list( bfs.nodes )[ :max_n ]
    return G.subgraph( subset ).copy()


def top_pr_ranked_node( G ):
    pr = nx.pagerank( G )
    pr = dict( sorted( pr.items(), key=lambda item: item[ 1 ], reverse=True ) )
    return list( pr.keys() )[ 0 ]


def is_connected( G, weak=True ) -> bool:
    if G.is_directed():
        return nx.is_weakly_connected( G ) if weak else nx.is_strongly_connected( G )
    return nx.is_connected( G )


def is_iso_subgraph( graph, subgraph ):
    def node_match( first_node, second_node ):
        anchor_match = first_node[ "anchor" ] == second_node[ "anchor" ]
        label_match = first_node[ "label" ] == second_node[ "label" ]
        return anchor_match and label_match

    def edge_match( first_edge, second_edge ):
        return first_edge[ "label" ] == second_edge[ "label" ]

    """
    return nx.is_isomorphic( graph, subgraph,
                             node_match=node_match,
                             edge_match=edge_match )
    """
    matcher = nx.algorithms.isomorphism.GraphMatcher( graph, subgraph,
                                                      node_match=node_match,
                                                      edge_match=edge_match )
    return matcher.subgraph_is_isomorphic()


def max_spanning_radius( G, start_node ):
    bft_tree = nx.traversal.bfs_tree( nx.Graph( G ), start_node )
    stack = [ (start_node, 0) ]
    max_radius = 0
    while stack:
        current_node, depth = stack.pop()
        max_radius = max( max_radius, depth )
        for child in bft_tree.successors( current_node ):
            stack.append( (child, depth + 1) )
    return max_radius


def normalize_graph( G, max_distance=4, force_directed=False ):
    if G.is_directed():
        return normalize_di_graph( G, max_distance=max_distance )

    G_norm = nx.DiGraph() if G.is_directed() or force_directed else nx.Graph()
    anchor = get_anchor( G )
    paths = { }
    stack = [ (anchor, ("", [ ]), None, None) ]
    furthest_distance = 0
    while len( stack ) > 0:

        current_node, (label_path, node_path), parent, parent_source = stack.pop( 0 )
        if (current_node == anchor and parent is not None) or len( label_path ) >= max_distance:
            continue

        current_node_norm = G_norm.number_of_nodes()
        label_path += str( G.nodes[ current_node ][ "label" ] )
        distance = len( label_path ) - 1
        if label_path not in paths.keys():
            paths[ label_path ] = [ *node_path, current_node_norm ]
            n_data: dict = G.nodes[ current_node ]
            n_data[ "presence" ] = 0
            n_data[ "distance" ] = distance
            G_norm.add_node( current_node_norm, **n_data )
            if parent is not None:
                G_norm.add_edge( parent, current_node_norm )
        else:
            current_node_norm = paths[ label_path ][ -1 ]

        node_path = paths[ label_path ]
        for n in node_path:
            G_norm.nodes[ n ][ "presence" ] += 1

        if distance > furthest_distance:
            furthest_distance = distance

        for child in G.neighbors( current_node ):
            if child == parent_source:
                continue
            stack.append( (child, (label_path, node_path), current_node_norm, current_node) )

    return G_norm, furthest_distance


def normalize_di_graph( G: nx.DiGraph, max_distance=4):
    G_norm = nx.DiGraph()
    anchor = get_anchor( G )
    paths = { }
    stack = [ (anchor, ("", [ ]), None, None, None) ]
    furthest_distance = 0
    while len( stack ) > 0:

        current_node, (label_path, node_path), parent, parent_source, is_successor = stack.pop( 0 )
        if (current_node == anchor and parent is not None) or len( label_path ) >= max_distance:
            continue

        current_node_norm = G_norm.number_of_nodes()
        label_path += f"{'>' if is_successor else '<'}{str( G.nodes[ current_node ][ 'label' ] )}"
        distance = (len( label_path ) / 2) - 1
        if label_path not in paths.keys():
            paths[ label_path ] = [ *node_path, current_node_norm ]
            n_data: dict = G.nodes[ current_node ]
            n_data[ "presence" ] = 0
            n_data[ "distance" ] = distance
            G_norm.add_node( current_node_norm, **n_data )
            if parent is not None:
                if is_successor:
                    G_norm.add_edge( parent, current_node_norm )
                else:
                     G_norm.add_edge( current_node_norm, parent )
        else:
            current_node_norm = paths[ label_path ][ -1 ]

        node_path = paths[ label_path ]
        for n in node_path:
            G_norm.nodes[ n ][ "presence" ] += 1

        if distance > furthest_distance:
            furthest_distance = distance

        for successor in G.successors( current_node ):
            if successor == parent_source:
                continue
            stack.append( (successor, (label_path, node_path), current_node_norm, current_node, True) )

        for predecessor in G.predecessors( current_node ):
            if predecessor == parent_source:
                continue
            stack.append( (predecessor, (label_path, node_path), current_node_norm, current_node, False) )

    return G_norm, furthest_distance


def norm_graphs_are_equal( G1, G2 ):
    G1_paths = [ p[ 1 ] for p in get_all_norm_paths( G1 ) ]
    G2_paths = [ p[ 1 ] for p in get_all_norm_paths( G2 ) ]
    return set( G1_paths ) == set( G2_paths )


def get_norm_graph_intersection( G1, G2 ):
    combined, node_matching, _ = combine_normalized( G1, G2 )
    n_keep = [ ]
    for idx, nid in enumerate( combined.nodes() ):
        if node_matching[ idx ] > 0:
            n_keep.append( nid )
    return combined.subgraph( n_keep )


def connect_graphs_at_anchor( graphs, keep_radius=-1 ):
    if len( graphs ) == 0:
        return nx.Graph()

    G = nx.DiGraph() if graphs[ 0 ].is_directed() else nx.Graph()

    anchor = None
    for G_source in graphs:
        source_anchor = get_anchor( G_source )
        if keep_radius > 0:
            G_source = subgraph( G_source, source_anchor, keep_radius )
        mapping = { }
        for nid, ndata in G_source.nodes( data=True ):
            new_nid = G.number_of_nodes()
            if nid == source_anchor:
                if anchor is not None:
                    mapping[ nid ] = anchor
                    continue
                anchor = new_nid
            G.add_node( G.number_of_nodes(), **ndata )
            mapping[ nid ] = new_nid
        for source_nid, target_nid, edata in G_source.edges( data=True ):
            G.add_edge( mapping[ source_nid ], mapping[ target_nid ], **edata )

    if not is_connected( G ):
        raise AssertionError( f"Graphs couldn't be connected at anchor!" )

    return G


def read_mapping( mapping_file, sg2g=False ):
    mapping = dict()
    with open( io_utils.get_abs_file_path( mapping_file ), "r", encoding="utf-8" ) as f:
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
    with open( io_utils.get_abs_file_path( database_file_name ), "r", encoding="utf-8" ) as f:
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


def mark_anchor( args, G, source_graph_idx, mapping: [ int, int ] = None ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    database_file = f"{args.dataset_dir}/{dataset}/{source_graph_idx}/source.lg"
    with open( io_utils.get_abs_file_path( database_file ), "r", encoding="utf-8" ) as f:
        lines = [ line.strip() for line in f.readlines() ]
        for i, line in enumerate( lines ):
            cols = line.split( " " )
            if i > 2:
                return
            if cols[ 0 ] == "p":
                anchor = int( cols[ -1 ] )
                if mapping is not None:
                    anchor = mapping[ anchor ]
                for node_id, node_data in G.nodes( data=True ):
                    node_data[ "anchor" ] = 1 if int( node_id ) == anchor else 0


def get_anchor( G ):
    anchor_nid = -1
    for n, n_data in G.nodes( data=True ):
        if "anchor" in n_data and n_data[ "anchor" ] == 1:
            anchor_nid = n
            break
    return anchor_nid


def load_source_mapping( args, source_graph_idx, flip=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    mapping = read_mapping(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/source_mapping.lg" )[ source_graph_idx ]
    mapping = { original_id + 1: source_id for original_id, source_id in mapping.items() }
    return misc_utils.flip_key_values( mapping ) if flip else mapping


def relabel_nodes( G, mapping ):
    complete_mapping = { }
    for k, v in mapping.items():
        if k in G.nodes:
            complete_mapping[ k ] = v

    ids = sorted( [ *complete_mapping.values(), *G.nodes() ] )
    for n in G.nodes():
        if n not in mapping.keys():
            complete_mapping[ n ] = ids[ -1 ] + 1
            """
            if n not in ids:
                # complete_mapping[ n ] = n
                complete_mapping[ n ] = -n
            else:
                # complete_mapping[ n ] = ids[ -1 ] + 1
                complete_mapping[ n ] = ids[ 0 ] - 1
            """
            ids = sorted( complete_mapping.values() )

    return nx.relabel_nodes( G, complete_mapping )


def load_source_graph( args, source_graph_idx, relabel=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"

    source = read_graphs(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/source.lg",
        directed=args.directed )[ source_graph_idx ]

    mark_anchor( args, source, source_graph_idx )
    if relabel:
        source_mapping = load_source_mapping( args, source_graph_idx )
        source = relabel_nodes( source, source_mapping )
    return source


def load_source_graphs( args, relabel=True, with_loading_bar=False ):
    dataset_type = "test" if args.test_data else "train"
    dataset_path = os.path.join( args.dataset_dir, f"{args.dataset}_{dataset_type}" )
    dataset_path = io_utils.get_abs_file_path( dataset_path )

    source_graph_idxs = io_utils.get_filenames_in_dir( dataset_path, only_files=False )
    source_graph_idxs = sorted( [ int( idx ) for idx in source_graph_idxs ] )
    if with_loading_bar:
        source_graph_idxs = tqdm( source_graph_idxs )

    source_graphs = { }
    for source_graph_idx in source_graph_idxs:
        source_graph_idx = int( source_graph_idx )
        source_graphs[ source_graph_idx ] = load_source_graph( args, source_graph_idx, relabel=relabel )
    return source_graphs


def load_query_id_mappings( args, source_graph_idx, flip=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    mappings = read_mapping(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/{'non' if not args.iso else ''}iso_subgraphs_mapping.lg" )
    if flip:
        mappings = { query_graph_idx: misc_utils.flip_key_values( mapping ) for query_graph_idx, mapping in
                     mappings.items() }
    return mappings


def load_query_id_mapping( args, source_graph_idx, query_subgraph_idx, flip=True ):
    return load_query_id_mappings( args, source_graph_idx, flip=flip )[ query_subgraph_idx ]


def load_query_graphs( args, source_graph_idx, relabel=True ):
    dataset_type = "test" if args.test_data else "train"
    dataset = f"{args.dataset}_{dataset_type}"
    queries = read_graphs(
        f"{args.dataset_dir}/{dataset}/{source_graph_idx}/{'non' if not args.iso else ''}iso_subgraphs.lg",
        directed=args.directed )
    if relabel:
        query_id_mappings = load_query_id_mappings( args, source_graph_idx )
        source_id_mapping = load_source_mapping( args, source_graph_idx )
        for query_graph_idx, query in queries.items():
            query = relabel_nodes( query, query_id_mappings[ query_graph_idx ] )
            mark_anchor( args, query, source_graph_idx )
            query = relabel_nodes( query, source_id_mapping )
            queries[ query_graph_idx ] = query
    return queries


def load_query_graph( args, source_graph_idx, query_subgraph_idx, relabel=True ):
    return load_query_graphs( args, source_graph_idx, relabel=relabel )[ query_subgraph_idx ]


def get_record_scopes( args ) -> dict[ str, str ]:
    record_scopes = { }
    record_scope_filepath = os.path.join( args.raw_dataset_dir, args.dataset, args.dataset + ".record_scopes" )
    record_scope_filepath = io_utils.get_abs_file_path( record_scope_filepath )
    record_scope_file = open( record_scope_filepath, "r" )
    for idx, record_scope in enumerate( record_scope_file.read().strip().split( "\n" ) ):
        record_scopes[ str( idx + 1 ) ] = record_scope
    return record_scopes


def get_record_datasets( args ) -> dict[ str, str ]:
    record_datasets = { }
    record_dataset_filepath = os.path.join( args.raw_dataset_dir, args.dataset, args.dataset + ".record_datasets" )
    record_dataset_filepath = io_utils.get_abs_file_path( record_dataset_filepath )
    record_dataset_file = open( record_dataset_filepath, "r" )
    for idx, record_dataset in enumerate( record_dataset_file.read().strip().split( "\n" ) ):
        record_datasets[ str( idx + 1 ) ] = record_dataset
    return record_datasets


def get_design_patterns( args ) -> dict[ str, str ]:
    design_patterns = { }
    pattern_type_filepath = os.path.join( args.raw_dataset_dir, args.dataset, args.dataset + ".pattern_types" )
    pattern_type_filepath = io_utils.get_abs_file_path( pattern_type_filepath )
    pattern_type_file = open( pattern_type_filepath, "r" )
    for row in list( pattern_type_file.read().strip().split( "\n" ) ):
        if len( row.split( " " ) ) < 2:
            continue
        node_id = row.split( " " )[ 0 ]
        pattern_type = row.split( " " )[ 1 ]
        design_patterns[ str( int( node_id ) ) ] = pattern_type
    return design_patterns


def map_node_label_idx( node_id, node_data,
                        record_scopes=None,
                        design_patterns=None ):
    node_label_idx = node_data[ "label" ]
    record_type = misc_utils.get_enum_by_idx( cpg_const.NodeLabel, node_label_idx )
    label = f"<{str( node_id )}>"

    if "anchor" in node_data and node_data[ "anchor" ] == 1:
        label = f"{label}\n[ANCHOR]"

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


def get_node_colors( G, anchor_color="purple", node_color="grey" ):
    anchor = get_anchor( G )
    colors = [ anchor_color if n == anchor else node_color for n in G.nodes() ]
    return colors


def get_pattern_graphs_idxs( args, graphs ):
    design_patterns = get_design_patterns( args )
    pattern_graphs_idxs: dict[ cpg_const.DesignPatternType, list[ int ] ] = { }

    for graph_idx, graph in graphs.items():

        anchor_nid = get_anchor( graph )
        if str( anchor_nid ) in design_patterns:

            dp = misc_utils.get_enum_by_value(
                cpg_const.DesignPatternType,
                design_patterns[ str( anchor_nid ) ] )

            if dp not in pattern_graphs_idxs:
                pattern_graphs_idxs[ dp ] = list()
            pattern_graphs_idxs[ dp ].append( graph_idx )

    return pattern_graphs_idxs


def filter_pattern_graphs_with_idx( graphs, pattern_graphs_idxs ):
    return { dp.value: [ graphs[ int( gidx ) ] for gidx in gidxs ] for dp, gidxs in pattern_graphs_idxs.items() }


def filter_no_pattern_graphs_with_idx( graphs, pattern_graphs_idxs, max_size=-1 ):
    pattern_idxs = [ value for values in pattern_graphs_idxs.values() for value in values ]
    graphs_w_o_patterns = [ graph for gidx, graph in graphs.items() if gidx not in pattern_idxs ]
    if max_size > 0:
        graphs_w_o_patterns = graphs_w_o_patterns[ :max_size ]
    return graphs_w_o_patterns


def get_pattern_graphs( args, graphs, include_w_o_pattern=False ):
    pattern_graphs_idxs = get_pattern_graphs_idxs( args, graphs )
    pattern_graphs = filter_pattern_graphs_with_idx( graphs, pattern_graphs_idxs )
    if include_w_o_pattern:
        pattern_graphs[ cpg_const.NO_DESIGN_PATTERN ] = filter_no_pattern_graphs_with_idx( graphs, pattern_graphs_idxs )
    return pattern_graphs


def combine_graph( source, query, anchor=None, matching_colors: dict[ int, str ] = None ):
    # Create a combined graph
    combined_graph = nx.compose( source, query )

    # Determine node colors
    node_matching = [ ]
    for node_id, node_data in combined_graph.nodes( data=True ):
        if node_id in query.nodes and node_id in source.nodes:
            if anchor is not None and node_id == anchor:
                node_matching.append( 2 )
            elif "anchor" in node_data and node_data[ "anchor" ] == 1:
                node_matching.append( 2 )  # Node is anchor
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


def get_all_norm_paths( graph ) -> list[ tuple[ list[ int ], str ] ]:
    paths = [ ]
    root = get_anchor( graph )
    dfs_tree = nx.dfs_tree( graph.to_undirected(), root )

    def dfs( node, parent, path_ids, path_labels ):
        path_ids.append( node )

        prefix = ""
        if graph.is_directed() and parent is not None:
            prefix = ">" if node in graph.successors( parent ) else "<"

        path_labels.append( f"{prefix}{str( graph.nodes[ node ][ 'label' ] )}" )
        children = list( dfs_tree.successors( node ) )
        if not children:
            paths.append( (list( path_ids ), "".join( path_labels )) )
        else:
            for child in children:
                dfs( child, node, path_ids, path_labels )
        path_ids.pop()
        path_labels.pop()

    dfs( root, None, [ ], [ ] )
    return paths


def combine_normalized( source: nx.Graph, query: nx.Graph, matching_colors: dict[ int, str ] = None ):
    source = source.copy()
    query = query.copy()

    def find_longest_overlap( query_label, source_labels ):
        for length in range( len( query_label ), 0, -1 ):
            prefix = query_label[ :length ]
            for source_label, source_ids in source_labels.items():
                if source_label.startswith( prefix ):
                    return prefix, source_ids[ :length ]
        return None, None

    source_paths = get_all_norm_paths( source )
    query_paths = get_all_norm_paths( query )
    source_labels = { labels: ids for ids, labels in source_paths }
    # source_node_offset = len( source.nodes )
    source_node_offset = max( source.nodes() )
    mapping = { }
    for query_ids, query_label in query_paths:
        matched_prefix, matched_ids = find_longest_overlap( query_label, source_labels )
        if matched_prefix:
            for qid, sid in zip( query_ids[ :len( matched_prefix ) ], matched_ids ):
                mapping[ qid ] = sid
            # If there are unmatched query nodes, assign new IDs
            for qid in query_ids[ len( matched_prefix ): ]:
                mapping[ qid ] = qid + source_node_offset + 1
        else:
            # If no match at all, assign new IDs
            for qid in query_ids:
                mapping[ qid ] = qid + source_node_offset + 1

    query = nx.relabel_nodes( query, mapping )
    return combine_graph( source, query, get_anchor( source ), matching_colors=matching_colors )


def compute_interactions( model, source_graph, query_graph, threshold ):
    """Compute high-confidence interaction scores between nodes of the query graph and source graph."""
    interaction_matrix = model.predict_embedding( [ query_graph ], [ source_graph ] )[ 0 ].cpu().detach().numpy()
    x_coord, y_coord = np.where( interaction_matrix > threshold )
    n_query_nodes = query_graph.number_of_nodes()
    interaction_dict = { }
    for x, y in zip( x_coord, y_coord ):
        if x < n_query_nodes <= y:
            interaction_dict[ (x, y - n_query_nodes) ] = interaction_matrix[ x ][ y ]
        elif x >= n_query_nodes > y and (y, x - n_query_nodes) not in interaction_dict:
            interaction_dict[ (y, x - n_query_nodes) ] = interaction_matrix[ x ][ y ]

    interaction_dict = { (int( query ), int( source )): p for (query, source), p in interaction_dict.items() }
    return interaction_dict


def map_query_interactions( interactions, query_graph ):
    """Map query graph nodes to source graph nodes based on interaction probabilities."""
    mapping = { }
    for q_node in query_graph.nodes:
        matches = [ (g_node, score) for (q, g_node), score in interactions.items() if q == q_node ]
        if matches:
            max_score = max( matches, key=lambda x: x[ 1 ] )[ 1 ]
            mapping[ q_node ] = [ g for g, s in matches if s == max_score ]
        else:
            mapping[ q_node ] = [ ]
    return mapping


def plot_interactions( args, model, src_idx, query_idx, threshold=0.5 ):
    def get_node_labels_and_colors( graph, mappings, ground_truth ):
        """Get labels and colors for graph nodes based on predicted and ground-truth mappings."""
        labels = { node: "" for node in graph.nodes }
        for q_node, g_nodes in mappings.items():
            for g_node in g_nodes:
                labels[ g_node ] = f"{labels[ g_node ]},{q_node}" if labels[ g_node ] else str( q_node )

        colors = { node: "gray" for node in graph.nodes }
        for node, label in labels.items():
            if not label:
                # The node does not have any predicted label (its label is empty), yet it has a valid ground-truth mapping (i.e. ground_truth[node] != -1). This suggests that a prediction was expected (according to the ground truth) but the algorithm did not predict any mapping for that node.
                colors[ node ] = "gold" if ground_truth[ node ] != -1 else "gray"
                continue
            if any( ground_truth[ node ] == int( q ) for q in label.split( "," ) ):
                # The node has one or more predicted mappings, and at least one of these predictions matches the ground-truth mapping. In other words, the prediction is correct.
                colors[ node ] = "lime"
            elif colors[ node ] == "gray":
                # The node has one or more predicted mappings, but none of them match the ground-truth value. This indicates an incorrect prediction.
                colors[ node ] = "pink"

        for g_node, q_node in ground_truth.items():
            if not labels[ g_node ] and q_node != -1:
                labels[ g_node ] = str( q_node )

        return labels, colors

    def get_edge_colors( graph, query_graph, labels, colors ):
        """Assign colors to graph edges based on node mapping relationships."""
        edge_colors = { edge: "whitesmoke" for edge in graph.edges }
        for edge in graph.edges:
            n1, n2 = edge
            n1_labels, n2_labels = labels[ n1 ], labels[ n2 ]

            if colors[ n1 ] == "gray" or colors[ n2 ] == "gray":
                continue

            pairs = [
                (int( a ), int( b ))
                for a in n1_labels.split( "," ) for b in n2_labels.split( "," )
                if (int( a ), int( b )) not in query_graph.edges and (int( b ), int( a )) not in query_graph.edges
            ]
            if len( pairs ) < len( n1_labels.split( "," ) ) * len( n2_labels.split( "," ) ):
                if colors[ n1 ] == "lime" and colors[ n2 ] == "lime":
                    edge_colors[ edge ] = "black"
                elif "gold" in { colors[ n1 ], colors[ n2 ] }:
                    edge_colors[ edge ] = "goldenrod"
                elif "pink" in { colors[ n1 ], colors[ n2 ] }:
                    edge_colors[ edge ] = "palevioletred"
            elif "pink" in { colors[ n1 ], colors[ n2 ] }:
                edge_colors[ edge ] = "palevioletred"

        return edge_colors

    """Compute interactions and plot the graph with matched subgraph highlights."""
    src_graph = load_source_graph( args, src_idx, relabel=False )
    query_graph = load_query_graph( args, src_idx, query_idx, relabel=False )
    ground_truth = load_query_id_mapping( args, src_idx, query_idx, flip=False )

    interactions = compute_interactions( model, src_graph, query_graph, threshold )
    mappings = map_query_interactions( interactions, query_graph )
    labels, colors = get_node_labels_and_colors( src_graph, mappings, ground_truth )
    edges = get_edge_colors( src_graph, query_graph, labels, colors )

    plot_utils.plot_graph(
        src_graph,
        nodeLabels=labels,
        nodeColors=list( colors.values() ),
        edgeColors=list( edges.values() ),
        title="Matching",
        with_label=True,
    )


def encode_pattern_id( name: str, pid: str ):
    if name == "None" or name is None or pid is None:
        return name
    return f"{name}#{pid}"


def decode_pattern_id( name: str ):
    if name is None:
        return
    parts = name.split( "#", 1 )
    if len( parts ) < 2:
        return None
    return parts[ 1 ]
