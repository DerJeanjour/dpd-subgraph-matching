import os

import networkx as nx
from neo4j import GraphDatabase

import matching.glema.common.utils.arg_utils as arg_utils
import matching.misc.utils as utils


def import_dataset( args ):
    print( "Importing datasets from neo4j ..." )

    uri = f"{args.neo4j_protocol}{args.neo4j_host}{args.neo4j_port}"

    driver = GraphDatabase.driver( uri, auth=(args.neo4j_user, args.neo4j_pw) )

    graphs = { }
    with driver.session() as session:
        results = session.execute_read( fetch_graph_data )
        for record in results:
            dataset = record[ "source_properties" ][ "node.dataset" ]
            if dataset not in graphs:
                print( "Found dataset:", dataset )
                graphs[ dataset ] = nx.MultiDiGraph()
            G = graphs[ dataset ]
            add_record_to_graph( G, record )

    driver.close()

    for dataset in graphs:
        G = graphs[ dataset ]
        prepare_graph_attributes( G )
        # print( f"Nodes of {dataset}:", G.nodes( data=True ) )
        write_as_gml( G, dataset )


def fetch_graph_data( tx ):
    query = """
    MATCH (n)-[r]->(m)
    RETURN 

        elementId(n) AS source_id,
        labels(n) AS source_labels,
        properties(n) AS source_properties, 

        elementId(m) AS target_id, 
        labels(m) AS target_labels,
        properties(m) AS target_properties, 

        type(r) AS edge_type
    """
    return list( tx.run( query ) )


def add_record_to_graph( G: nx.MultiDiGraph, record ):
    source_id = record[ "source_id" ]
    source_labels = record[ "source_labels" ]
    source_properties = record[ "source_properties" ]
    target_id = record[ "target_id" ]
    target_labels = record[ "target_labels" ]
    target_properties = record[ "target_properties" ]
    relationship_type = record[ "edge_type" ]

    G.add_node( source_id, labels=source_labels, **source_properties )
    G.add_node( target_id, labels=target_labels, **target_properties )
    G.add_edge( source_id, target_id, type=relationship_type )


def write_as_gml( G: nx.MultiDiGraph, name ):
    file_name = utils.get_abs_file_path( f"datasets/{name}.gml" )
    print( f"Writing dataset [ nodes: {len( G.nodes )} / edges: {len( G.edges )} ] to file {file_name}" )
    if not os.path.exists( os.path.dirname( file_name ) ):
        os.makedirs( os.path.dirname( file_name ) )
    nx.write_gml( G, file_name )


def prepare_graph_attributes( G: nx.MultiDiGraph ):
    for node, data in G.nodes( data=True ):
        transform_list_to_flags( data, "labels" )
        transform_list_attributes( data )
        replace_dots( data )
    for u, v, key, data in G.edges( data=True, keys=True ):
        transform_list_attributes( data )
        replace_dots( data )


def transform_list_to_flags( data, key ):
    # Create new boolean attributes for list values
    if key in data and isinstance( data[ key ], list ):
        for entry in data[ key ]:
            data[ f"{key}.{entry}" ] = True
        del data[ key ]


def transform_list_attributes( data ):
    # Convert lists to comma-separated string
    for key, value in list( data.items() ):
        if isinstance( value, list ):
            data[ key ] = ",".join( map( str, value ) )


def replace_dots( data ):
    new_data = { }
    for key, value in data.items():
        new_key = key.replace( ".", "_" )
        new_data[ new_key ] = value
    data.clear()
    data.update( new_data )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    import_dataset( args )
