import os

import networkx as nx
from tqdm import tqdm

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.misc.cpg_const as cpg_const


def import_datasets( args ):
    misc_utils.set_seed( args.seed )

    import_dir = io_utils.get_abs_file_path( args.import_dir, with_subproject=False )
    list_datasets = os.listdir( import_dir )

    G = nx.DiGraph()
    node_id_mapping: dict[ str, int ] = { }
    anchor_mapping: dict[ int, int ] = { }

    graph_idx = 1  # graph index starts from 1
    for dataset_filename in list_datasets:
        if not dataset_filename.endswith( args.import_format ):
            continue
        dataset_name = dataset_filename.split( "." )[ 0 ]
        graph_idx = read_dataset_to( args, dataset_name, G, node_id_mapping, anchor_mapping, graph_idx )

    print( f"Collected all datasets: Nodes[{len( G.nodes )}] / Edges[{len( G.edges )}]" )
    if len( G.nodes ) == 0:
        return

    write_files( args, G, node_id_mapping, anchor_mapping )


def read_dataset_to( args,
                     dataset_name: str,
                     G_target: nx.DiGraph,
                     node_id_mapping: dict[ str, int ],
                     anchor_mapping: dict[ int, int ],
                     graph_idx: int ) -> int:
    import_file = io_utils.get_abs_file_path( f"{args.import_dir}{dataset_name}{args.import_format}",
                                              with_subproject=False )

    print( f"Reading dataset from file: {import_file}" )
    G = nx.MultiDiGraph()
    if args.import_format == ".gml":
        G = read_gml( import_file )

    if len( G.nodes ) == 0:
        print( "Couldn't read graph or graph is empty ..." )
        return

    print( f"Processing dataset: Nodes[{G.number_of_nodes()}] / Edges[{G.number_of_edges()}]" )
    for anchor_node_id, node_data in tqdm( list( G.nodes( data=True ) ) ):
        record_label = get_record_label( node_data )
        if record_label is None or record_label != cpg_const.NodeLabel.RECORD:
            # use only records as anchor for subgraphs
            continue

        G_sub = get_k_neighbourhood( G, anchor_node_id, args.import_subgraph_radius,
                                     min_n=args.import_subgraph_min,
                                     max_n=args.import_subgraph_max )

        if G_sub.number_of_nodes() == 0:
            continue

        # plot_utils.save_graph_debug( G_sub, f"sub_{graph_idx}.png" )
        add_graph_to( G_sub, G_target,
                      node_id_mapping, anchor_mapping,
                      anchor_node_id, graph_idx )  # subgraph around record
        graph_idx += 1

    return graph_idx


def get_k_neighbourhood( G: nx.DiGraph, anchor_node_id, k: int, min_n: int = 2, max_n: int = 50 ):
    resize_tries = 0
    max_resize_tries = 10
    subgraph_radius = k
    G_sub = nx.ego_graph( G, anchor_node_id, radius=subgraph_radius, undirected=True )

    # reduce subgraph size until it matches max subgraph size
    while G_sub.number_of_nodes() > max_n and resize_tries < max_resize_tries:
        subgraph_radius -= 1
        G_sub = nx.ego_graph( G, anchor_node_id, radius=subgraph_radius, undirected=True )
        resize_tries += 1

    # reduce subgraph size until it matches min subgraph size
    while G_sub.number_of_nodes() < min_n and resize_tries < max_resize_tries:
        subgraph_radius += 1
        G_sub = nx.ego_graph( G, anchor_node_id, radius=subgraph_radius, undirected=True )
        resize_tries += 1

    if G_sub.number_of_nodes() > max_n or G_sub.number_of_nodes() < min_n:
        return nx.DiGraph()

    return nx.DiGraph( G_sub )


def add_graph_to( G_source: nx.DiGraph, G_target: nx.DiGraph,
                  node_id_mapping: dict[ str, int ], anchor_mapping: dict[ int, int ],
                  anchor_node_id: str, graph_idx: int ):
    # process nodes
    for node_idx, node in enumerate( G_source.nodes( data=True ) ):

        node_id, node_data = node
        record_label = get_record_label( node_data )
        if record_label is None:
            continue

        node_id_key = f"{graph_idx}::{node_id}"
        node_id_mapping[ node_id_key ] = G_target.number_of_nodes() + 1  # node_ids starts from 1
        if node_id == anchor_node_id:
            anchor_mapping[ graph_idx ] = node_id_mapping[ node_id_key ]

        record_label_idx = misc_utils.get_enum_idx( record_label )
        pattern_types = get_design_pattern_types( node_data )
        record_scope_attr = cpg_const.NodeAttr.SCOPED_RECORD_NAME.value
        record_scope = node_data[ record_scope_attr ] if record_scope_attr in node_data else "None"

        G_target.add_node( node_id_mapping[ node_id_key ],
                           label=record_label_idx,
                           graph_idx=graph_idx,
                           pattern_types=pattern_types,
                           record_scope=record_scope )

    # process edges
    for edge in list( G_source.edges( data=True ) ):
        source_id, target_id, edge_data = edge
        source_id = f"{graph_idx}::{source_id}"
        target_id = f"{graph_idx}::{target_id}"
        if source_id not in node_id_mapping or target_id not in node_id_mapping:
            continue
        G_target.add_edge( node_id_mapping[ source_id ], node_id_mapping[ target_id ] )


def write_files( args, G: nx.DiGraph, node_id_mapping: dict[ str, int ], anchor_mapping: dict[ int, int ], ):
    output_dir = ensure_output_dir( args )

    node_label_file = os.path.join( output_dir, f"{args.dataset}.node_labels" )
    with open( node_label_file, "w", encoding="utf-8" ) as file:
        for node_id, node_data in list( G.nodes( data=True ) ):
            file.write( f"{node_data[ 'label' ]}\n" )

    edge_file = os.path.join( output_dir, f"{args.dataset}.edges" )
    with open( edge_file, "w", encoding="utf-8" ) as file:
        for source_id, target_id in list( G.edges() ):
            file.write( f"{source_id}, {target_id}\n" )

    graph_idx_file = os.path.join( output_dir, f"{args.dataset}.graph_idx" )
    with open( graph_idx_file, "w", encoding="utf-8" ) as file:
        for _, node_data in list( G.nodes( data=True ) ):
            file.write( f"{node_data[ 'graph_idx' ]}\n" )

    node_id_mapping_file = os.path.join( output_dir, f"{args.dataset}.node_id_mapping" )
    with open( node_id_mapping_file, "w", encoding="utf-8" ) as file:
        for repo_id, node_id in node_id_mapping.items():
            file.write( f"{node_id} {repo_id}\n" )

    anchor_id_file = os.path.join( output_dir, f"{args.dataset}.anchors" )
    with open( anchor_id_file, "w", encoding="utf-8" ) as file:
        for graph_idx, anchor_node_id in anchor_mapping.items():
            file.write( f"{graph_idx} {anchor_node_id}\n" )

    record_scope_file = os.path.join( output_dir, f"{args.dataset}.record_scopes" )
    with open( record_scope_file, "w", encoding="utf-8" ) as file:
        for _, node_data in list( G.nodes( data=True ) ):
            file.write( f"{node_data[ 'record_scope' ]}\n" )

    # pattern_types
    pattern_type_file = os.path.join( output_dir, f"{args.dataset}.pattern_types" )
    with open( pattern_type_file, "w", encoding="utf-8" ) as file:
        for node_id, node_data in list( G.nodes( data=True ) ):
            pattern_types = node_data[ 'pattern_types' ]
            for pattern_type in pattern_types:
                file.write( f"{node_id} {pattern_type}\n" )

    print( "Dataset files written ..." )


def read_gml( import_file: str ) -> nx.MultiDiGraph:
    return nx.read_gml( path=import_file )


def ensure_output_dir( args ) -> str:
    output_dir = io_utils.get_abs_file_path( os.path.join( args.raw_dataset_dir, args.dataset ) )
    if not os.path.exists( output_dir ):
        os.mkdir( output_dir )
    return output_dir


def get_design_pattern_types( node_data ) -> list[ str ]:
    types = list()
    for dp_type in cpg_const.DesignPatternType:
        if f"labels_{dp_type.value}" in node_data:
            types.append( dp_type.value )
    return types


def get_record_label( node_data ) -> cpg_const.NodeLabel:
    for label in cpg_const.NodeLabel:
        if f"labels_{label.value}" in node_data:
            return label
    return None


if __name__ == "__main__":
    args = arg_utils.parse_args()
    # args.import_subgraph_radius = 4
    # args.import_subgraph_max = 40
    # args.import_subgraph_min = 2
    # args.dataset = "CPG"
    # print( args )

    import_datasets( args )
