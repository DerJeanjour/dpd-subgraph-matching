import os
import networkx as nx
from tqdm import tqdm

import matching.glema.common.utils as utils
import matching.misc.cpg_const as cpg_const


def import_datasets( args ):
    import_dir = utils.get_abs_file_path( args.import_dir, with_subproject=False )
    list_datasets = os.listdir( import_dir )

    G = nx.DiGraph()
    node_id_mapping: dict[ str, int ] = { }

    for dataset_filename in list_datasets:
        if not dataset_filename.endswith( args.import_format ):
            continue
        dataset_name = dataset_filename.split( "." )[ 0 ]
        read_dataset_to( args, dataset_name, G, node_id_mapping )

    print( f"Collected all datasets: Nodes[{len( G.nodes )}] / Edges[{len( G.edges )}]" )
    if len( G.nodes ) == 0:
        return

    write_files( args, G, node_id_mapping )


def read_dataset_to( args, dataset_name: str, G_target: nx.DiGraph, node_id_mapping: dict[ str, int ] ):
    import_file = utils.get_abs_file_path( f"{args.import_dir}{dataset_name}{args.import_format}",
                                           with_subproject=False )

    print( f"Reading dataset from file: {import_file}" )
    G = nx.MultiDiGraph()
    if args.import_format == ".gml":
        G = read_gml( import_file )

    if len( G.nodes ) == 0:
        print( "Couldn't read graph or graph is empty ..." )
        return

    print( f"Processing dataset: Nodes[{len( G.nodes )}] / Edges[{len( G.edges )}]" )

    graph_idx = 1  # graph index start at 1
    for node_id, node_data in tqdm( list( G.nodes( data=True ) ) ):
        record_label = get_record_label( node_data )
        if record_label is None or record_label != cpg_const.NodeLabel.RECORD:
            # use only records as pivot for subgraphs
            continue
        G_sub = nx.ego_graph( G, node_id, radius=args.import_subgraph_radius, undirected=True )
        # TODO find smarter way to partition graph without duplicates !!!
        add_subgraph_to( G_sub, G_target, node_id_mapping, graph_idx ) # subgraph around record
        graph_idx += 1


def add_subgraph_to( G_source: nx.DiGraph, G_target: nx.DiGraph, node_id_mapping: dict[ str, int ], graph_idx: int ):
    # process nodes
    for node_idx, node in enumerate( G_source.nodes( data=True ) ):

        node_id, node_data = node
        record_label = get_record_label( node_data )
        if record_label is None:
            continue

        node_id_key = f"{graph_idx}::{node_id}"
        node_id_mapping[ node_id_key ] = G_target.number_of_nodes() + 1  # node indices start with 1

        record_label_idx = utils.get_enum_idx( record_label )
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


def write_files( args, G: nx.DiGraph, node_id_mapping: dict[ str, int ] ):
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
        for key, value in node_id_mapping.items():
            file.write( f"{value} {key}\n" )

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
    output_dir = utils.get_abs_file_path( os.path.join( args.raw_dataset_dir, args.dataset ) )
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
    args = utils.parse_args()
    args.import_subgraph_radius = 5
    args.dataset = "CPG"
    print( args )

    import_datasets( args )
