import os
import pickle

import networkx as nx
from tqdm import tqdm

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils


def read_graphs( database_file_name, args, max_subgraph=-1 ):
    graphs = dict()
    sizes = { }
    degrees = { }

    anchor = None
    with open( database_file_name, "r", encoding="utf-8" ) as f:
        lines = [ line.strip() for line in f.readlines() ]
        tgraph, graph_cnt = None, 0
        for i, line in enumerate( lines ):
            cols = line.split( " " )
            if cols[ 0 ] == "t":
                if tgraph is not None:
                    graphs[ graph_cnt ] = tgraph
                    sizes[ graph_cnt ] = tgraph.number_of_nodes()
                    degrees[ graph_cnt ] = (
                            sum( dict( tgraph.degree ).values() ) / sizes[ graph_cnt ]
                    )

                    tgraph = None
                if cols[ -1 ] == "-1":
                    break

                if args.directed:
                    tgraph = nx.DiGraph()
                else:
                    tgraph = nx.Graph()
                graph_cnt = int( cols[ 2 ] )

                if max_subgraph > 0 and graph_cnt >= max_subgraph:
                    tgraph = None
                    break

            if cols[ 0 ] == "p":
                anchor = int( cols[ -1 ] )

            elif cols[ 0 ] == "v":
                node_id = int( cols[ 1 ] )
                label_idx = int( cols[ 2 ] )
                tgraph.add_node( node_id, label=label_idx )

            elif cols[ 0 ] == "e":
                node1, node2 = int( cols[ 1 ] ), int( cols[ 2 ] )
                if node1 < node2:
                    tgraph.add_edge( node1, node2, label=int( cols[ 3 ] ) )
                else:
                    tgraph.add_edge( node2, node1, label=int( cols[ 3 ] ) )

        # adapt to input files that do not end with 't # -1'
        if tgraph is not None:
            graphs[ graph_cnt ] = tgraph
            sizes[ graph_cnt ] = tgraph.number_of_nodes()
            degrees[ graph_cnt ] = sum( dict( tgraph.degree ).values() ) / sizes[ graph_cnt ]

    return graphs, sizes, degrees, anchor


def read_mapping( filename, max_subgraph=-1 ):
    mapping = { }
    with open( filename, "r", encoding="utf-8" ) as f:
        lines = [ line.strip() for line in f.readlines() ]
        tmapping, graph_cnt = None, 0
        for i, line in enumerate( lines ):
            cols = line.split( " " )
            if cols[ 0 ] == "t":
                if tmapping is not None:
                    mapping[ graph_cnt ] = tmapping

                if cols[ -1 ] == "-1":
                    break

                tmapping = [ ]
                graph_cnt = int( cols[ 2 ] )
                if max_subgraph > 0 and graph_cnt >= max_subgraph:
                    tmapping = None
                    break

            elif cols[ 0 ] == "v":
                tmapping.append( (int( cols[ 1 ] ), int( cols[ 2 ] )) )

        if tmapping is not None:
            mapping[ graph_cnt ] = tmapping

    return mapping


def mark_anchors( graphs, source_anchor, mappings=None ):
    for graph_idx, graph in graphs.items():

        graph_anchor_id = int( source_anchor )
        if mappings is not None:
            mapping = mappings[ graph_idx ]
            mapping = { snid: gnid for gnid, snid in mapping }
            if graph_anchor_id in mapping:
                graph_anchor_id = int( mapping[ graph_anchor_id ] )
            else:
                graph_anchor_id = -1

        anchor_marked = False
        for nid, ndata in graph.nodes( data=True ):
            ndata[ "anchor" ] = 1 if nid == graph_anchor_id else 0
            if ndata[ "anchor" ] == 1:
                anchor_marked = True

        if not anchor_marked:
            # if no anchor is present (should only be true for non iso subgraph) -> set new anchor by pr score
            gen_anchor = graph_utils.top_pr_ranked_node( graph )
            graph.nodes[ gen_anchor ][ "anchor" ] = 1

        if graph_utils.get_anchor( graph ) < 0:
            raise ValueError


def load_graph_data( data_dir, source_id, args, max_subgraph=-1 ):
    source_graphs, source_sizes, source_degrees, source_anchor = read_graphs(
        "%s/%s/source.lg" % (data_dir, source_id),
        args=args )
    mark_anchors( source_graphs, source_anchor )
    source_graph = source_graphs[ int( source_id ) ]
    source_size = source_sizes[ int( source_id ) ]
    source_degree = source_degrees[ int( source_id ) ]

    iso_subgraphs_mapping = read_mapping(
        "%s/%s/iso_subgraphs_mapping.lg" % (data_dir, source_id),
        max_subgraph=max_subgraph )
    noniso_subgraphs_mapping = read_mapping(
        "%s/%s/noniso_subgraphs_mapping.lg" % (data_dir, source_id),
        max_subgraph=max_subgraph )

    iso_subgraphs, iso_sizes, iso_degrees, _ = read_graphs(
        "%s/%s/iso_subgraphs.lg" % (data_dir, source_id),
        max_subgraph=max_subgraph,
        args=args )
    mark_anchors( iso_subgraphs, source_anchor, mappings=iso_subgraphs_mapping )
    noniso_subgraphs, noniso_sizes, noniso_degrees, _ = read_graphs(
        "%s/%s/noniso_subgraphs.lg" % (data_dir, source_id),
        max_subgraph=max_subgraph,
        args=args )
    mark_anchors( noniso_subgraphs, source_anchor, mappings=noniso_subgraphs_mapping )

    return (
        source_graph,
        iso_subgraphs,
        noniso_subgraphs,
        iso_subgraphs_mapping,
        noniso_subgraphs_mapping,
        source_size,
        iso_sizes,
        noniso_sizes,
        source_degree,
        iso_degrees,
        noniso_degrees,
    )


# Load and save
def load_dataset( data_dir, list_source, save_dir, args, additional_tag="", max_subgraph=-1 ):
    source_size_dict = { }
    source_degree_dict = { }
    subgraph_size_dict = { }
    subgraph_degree_dict = { }

    for source_id in tqdm( list_source ):
        (
            source,
            iso_subgraphs,
            noniso_subgraphs,
            iso_subgraphs_mapping,
            noniso_subgraphs_mapping,
            source_size,
            iso_sizes,
            noniso_sizes,
            source_degree,
            iso_degrees,
            noniso_degrees,
        ) = load_graph_data( data_dir, source_id, max_subgraph=max_subgraph, args=args )

        for key, data in iso_subgraphs.items():
            fname = "%s_%d_iso_%s" % (source_id, key, additional_tag)
            source_size_dict[ fname ] = source_size
            source_degree_dict[ fname ] = source_degree
            subgraph_size_dict[ fname ] = iso_sizes[ key ]
            subgraph_degree_dict[ fname ] = iso_degrees[ key ]
            with open( f"{save_dir}/{fname}", "wb" ) as f:
                pickle.dump( [ data, source, iso_subgraphs_mapping[ key ] ], f )

        for key, data in noniso_subgraphs.items():
            fname = "%s_%d_non_%s" % (source_id, key, additional_tag)
            source_size_dict[ fname ] = source_size
            source_degree_dict[ fname ] = source_degree
            subgraph_size_dict[ fname ] = noniso_sizes[ key ]
            subgraph_degree_dict[ fname ] = noniso_degrees[ key ]
            with open( f"{save_dir}/{fname}", "wb" ) as f:
                pickle.dump( [ data, source, noniso_subgraphs_mapping[ key ] ], f )

    if additional_tag != "":
        pickle.dump( source_size_dict, open( f"{save_dir}/{additional_tag}_graphs_size.pkl", "wb" ) )
        pickle.dump( source_degree_dict, open( f"{save_dir}/{additional_tag}_graphs_degree.pkl", "wb" ) )
        pickle.dump( subgraph_size_dict, open( f"{save_dir}/{additional_tag}_subgraphs_size.pkl", "wb" ) )
        pickle.dump( subgraph_degree_dict, open( f"{save_dir}/{additional_tag}_subgraphs_degree.pkl", "wb" ) )

    return list( subgraph_size_dict.keys() )


def process( args ):
    misc_utils.set_seed( args.seed )

    data_proccessed_dir = os.path.join( args.data_processed_dir, args.dataset )
    data_proccessed_dir = data_proccessed_dir.replace( "_train", "" )
    data_proccessed_dir = io_utils.get_abs_file_path( data_proccessed_dir )
    if args.directed:
        data_proccessed_dir += "_directed"

    print( f"Processing {data_proccessed_dir} ..." )

    # Load data
    if not os.path.exists( data_proccessed_dir ):
        os.mkdir( data_proccessed_dir )

    if not args.real:
        if args.testonly:
            additional_tag = "test"
        else:
            additional_tag = ""

        data_dir = os.path.join( args.dataset_dir, args.dataset )
        data_dir = io_utils.get_abs_file_path( data_dir )

        list_source = os.listdir( data_dir )
        list_source = list(
            filter( lambda x: os.path.isdir( os.path.join( data_dir, x ) ), list_source )
        )

        valid_keys = load_dataset(
            data_dir,
            list_source,
            data_proccessed_dir,
            additional_tag=additional_tag,
            max_subgraph=args.max_subgraph,
            args=args
        )

        if additional_tag == "test":
            test_keys = [ k for k in valid_keys if k.split( "_" )[ 0 ] in list_source ]
            train_keys = [ ]

        else:
            # Split train test
            from sklearn.model_selection import train_test_split

            train_source, test_source = train_test_split(
                list_source, test_size=0.2, random_state=42
            )

            train_keys = [ k for k in valid_keys if k.split( "_" )[ 0 ] in train_source ]
            test_keys = [ k for k in valid_keys if k.split( "_" )[ 0 ] in test_source ]

    elif args.real:

        data_dir = os.path.join( args.dataset_dir, f"{args.dataset}_test" )
        data_dir = io_utils.get_abs_file_path( data_dir )
        list_source = os.listdir( data_dir )
        list_source = list(
            filter( lambda x: os.path.isdir( os.path.join( data_dir, x ) ), list_source )
        )

        test_keys = load_dataset(
            data_dir,
            list_source,
            data_proccessed_dir,
            additional_tag="test",
            max_subgraph=args.max_subgraph,
            args=args
        )

        if args.testonly:
            train_keys = [ ]
        else:
            data_dir_train = os.path.join( args.dataset_dir, f"{args.dataset}_train" )
            data_dir_train = io_utils.get_abs_file_path( data_dir_train )
            list_source_train = os.listdir( data_dir_train )
            list_source_train = list(
                filter(
                    lambda x: os.path.isdir( os.path.join( data_dir_train, x ) ),
                    list_source_train,
                )
            )

            train_keys = load_dataset(
                data_dir_train,
                list_source_train,
                data_proccessed_dir,
                additional_tag="train",
                max_subgraph=args.max_subgraph,
                args=args
            )

    # Notice that key which has "iso" is isomorphism, otherwise non-isomorphism

    # Save train and test keys
    with open( "%s/train_keys.pkl" % data_proccessed_dir, "wb" ) as f:
        pickle.dump( train_keys, f )

    with open( "%s/test_keys.pkl" % data_proccessed_dir, "wb" ) as f:
        pickle.dump( test_keys, f )

    if args.real:

        for tag in [ "train", "test" ]:
            keys = test_keys if tag == "test" else train_keys
            graph_size_dict = pickle.load( open( f"{data_proccessed_dir}/{tag}_graphs_size.pkl", "rb" ) )
            graph_degree_dict = pickle.load( open( f"{data_proccessed_dir}/{tag}_graphs_degree.pkl", "rb" ) )
            subgraph_size_dict = pickle.load( open( f"{data_proccessed_dir}/{tag}_subgraphs_size.pkl", "rb" ) )
            subgraph_degree_dict = pickle.load( open( f"{data_proccessed_dir}/{tag}_subgraphs_degree.pkl", "rb" ) )

            # source keys by graph size
            graph_size_0_5 = list( filter( lambda x: graph_size_dict[ x ] <= 5, keys ) )
            graph_size_5_10 = list( filter( lambda x: 5 < graph_size_dict[ x ] <= 10, keys ) )
            graph_size_10_15 = list( filter( lambda x: 10 < graph_size_dict[ x ] <= 15, keys ) )
            graph_size_15_20 = list( filter( lambda x: 15 < graph_size_dict[ x ] <= 20, keys ) )
            graph_size_20_30 = list( filter( lambda x: 20 < graph_size_dict[ x ] <= 30, keys ) )
            graph_size_30_40 = list( filter( lambda x: 30 < graph_size_dict[ x ] <= 40, keys ) )
            graph_size_40_ = list( filter( lambda x: 40 < graph_size_dict[ x ], keys ) )

            # query keys by graph size / non dense
            nondense_0_20 = list(
                filter( lambda x: subgraph_size_dict[ x ] <= 20 and subgraph_degree_dict[ x ] <= 3, keys )
            )
            nondense_20_40 = list(
                filter( lambda x: 20 < subgraph_size_dict[ x ] <= 40 and subgraph_degree_dict[ x ] <= 3, keys )
            )
            nondense_40_60 = list(
                filter( lambda x: 40 < subgraph_size_dict[ x ] <= 60 and subgraph_degree_dict[ x ] <= 3, keys )
            )
            nondense_60_ = list(
                filter( lambda x: subgraph_size_dict[ x ] >= 60 and subgraph_degree_dict[ x ] <= 3, keys )
            )

            # query keys by graph size / dense
            dense_0_20 = list(
                filter( lambda x: subgraph_size_dict[ x ] <= 20 and subgraph_degree_dict[ x ] > 3, keys )
            )
            dense_20_40 = list(
                filter( lambda x: 20 < subgraph_size_dict[ x ] <= 40 and subgraph_degree_dict[ x ] > 3, keys )
            )
            dense_40_60 = list(
                filter( lambda x: 40 < subgraph_size_dict[ x ] <= 60 and subgraph_degree_dict[ x ] > 3, keys )
            )
            dense_60_ = list(
                filter( lambda x: subgraph_size_dict[ x ] >= 60 and subgraph_degree_dict[ x ] > 3, keys )
            )

            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_0_5"), "wb" ) as f:
                pickle.dump( graph_size_0_5, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_5_10"), "wb" ) as f:
                pickle.dump( graph_size_5_10, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_10_15"), "wb" ) as f:
                pickle.dump( graph_size_10_15, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_15_20"), "wb" ) as f:
                pickle.dump( graph_size_15_20, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_20_30"), "wb" ) as f:
                pickle.dump( graph_size_20_30, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_30_40"), "wb" ) as f:
                pickle.dump( graph_size_30_40, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "graph_size_40_"), "wb" ) as f:
                pickle.dump( graph_size_40_, f )

            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "nondense_0_20"), "wb" ) as f:
                pickle.dump( nondense_0_20, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "nondense_20_40"), "wb" ) as f:
                pickle.dump( nondense_20_40, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "nondense_40_60"), "wb" ) as f:
                pickle.dump( nondense_40_60, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "nondense_60_"), "wb" ) as f:
                pickle.dump( nondense_60_, f )

            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "dense_0_20"), "wb" ) as f:
                pickle.dump( dense_0_20, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "dense_20_40"), "wb" ) as f:
                pickle.dump( dense_20_40, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "dense_40_60"), "wb" ) as f:
                pickle.dump( dense_40_60, f )
            with open( "%s/%s_keys_%s.pkl" % (data_proccessed_dir, tag, "dense_60_"), "wb" ) as f:
                pickle.dump( dense_60_, f )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    # args.dataset = "CPG"
    # args.real = True
    # args.testonly = False
    # args.directed = True
    # args.max_subgraph = -1
    # print( args )
    process( args )
