import argparse
import os

import networkx as nx
import numpy as np
import torch
import matching.glema.common.utils as utils
from matching.glema.common.dataset import onehot_encoding_node
from matching.glema.common.model import GLeMaNet
from scipy.spatial import distance_matrix


class InferenceGNN:
    def __init__( self, args ) -> None:
        self.model = GLeMaNet( args )
        # self.device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
        self.device = utils.get_device()
        self.model = utils.initialize_model(
            self.model, self.device, load_save_file=args.ckpt
        )

        self.model.eval()
        self.embedding_dim = args.embedding_dim

    def prepare_single_input( self, m1, m2 ):
        # Prepare subgraph
        n1 = m1.number_of_nodes()
        adj1 = nx.to_numpy_array( m1 ) + np.eye( n1 )
        H1 = onehot_encoding_node( m1, self.embedding_dim )

        # Prepare source graph
        n2 = m2.number_of_nodes()
        adj2 = nx.to_numpy_array( m2 ) + np.eye( n2 )
        H2 = onehot_encoding_node( m2, self.embedding_dim )

        # Aggregation node encoding
        agg_adj1 = np.zeros( (n1 + n2, n1 + n2) )
        agg_adj1[ :n1, :n1 ] = adj1
        agg_adj1[ n1:, n1: ] = adj2
        agg_adj2 = np.copy( agg_adj1 )
        dm = distance_matrix( H1, H2 )
        dm_new = np.zeros_like( dm )
        dm_new[ dm == 0.0 ] = 1.0
        agg_adj2[ :n1, n1: ] = np.copy( dm_new )
        agg_adj2[ n1:, :n1 ] = np.copy( np.transpose( dm_new ) )

        H1 = np.concatenate( [ H1, np.zeros( (n1, self.embedding_dim) ) ], 1 )
        H2 = np.concatenate( [ np.zeros( (n2, self.embedding_dim) ), H2 ], 1 )
        H = np.concatenate( [ H1, H2 ], 0 )

        # node indice for aggregation
        valid = np.zeros( (n1 + n2,) )
        valid[ :n1 ] = 1

        sample = {
            "H": H,
            "A1": agg_adj1,
            "A2": agg_adj2,
            "V": valid,
        }

        return sample

    def input_to_tensor( self, batch_input ):
        max_natoms = max( [ len( item[ "H" ] ) for item in batch_input if item is not None ] )
        batch_size = len( batch_input )

        H = np.zeros( (batch_size, max_natoms, batch_input[ 0 ][ "H" ].shape[ -1 ]) )
        A1 = np.zeros( (batch_size, max_natoms, max_natoms) )
        A2 = np.zeros( (batch_size, max_natoms, max_natoms) )
        V = np.zeros( (batch_size, max_natoms) )

        for i in range( batch_size ):
            natom = len( batch_input[ i ][ "H" ] )

            H[ i, :natom ] = batch_input[ i ][ "H" ]
            A1[ i, :natom, :natom ] = batch_input[ i ][ "A1" ]
            A2[ i, :natom, :natom ] = batch_input[ i ][ "A2" ]
            V[ i, :natom ] = batch_input[ i ][ "V" ]

        H = torch.from_numpy( H ).float()
        A1 = torch.from_numpy( A1 ).float()
        A2 = torch.from_numpy( A2 ).float()
        V = torch.from_numpy( V ).float()

        H, A1, A2, V = (
            H.to( self.device ),
            A1.to( self.device ),
            A2.to( self.device ),
            V.to( self.device ),
        )

        return H, A1, A2, V

    def prepare_multi_input( self, list_subgraphs, list_graphs ):
        list_inputs = [ ]
        for li, re in zip( list_subgraphs, list_graphs ):
            list_inputs.append( self.prepare_single_input( li, re ) )

        return list_inputs

    def predict_label( self, list_subgraphs, list_graphs ):
        list_inputs = self.prepare_multi_input( list_subgraphs, list_graphs )
        input_tensors = self.input_to_tensor( list_inputs )
        results = self.model( input_tensors )
        return results

    def predict_embedding( self, list_subgraphs, list_graphs ):
        list_inputs = self.prepare_multi_input( list_subgraphs, list_graphs )
        input_tensors = self.input_to_tensor( list_inputs )
        results = self.model.get_refined_adjs2( input_tensors )
        return results


if __name__ == "__main__":

    args = utils.parse_args()
    print( args )

    data_path = utils.get_abs_file_path( os.path.join( args.data_path, args.dataset ) )
    result_dir = utils.ensure_dir( args.result_dir, args )

    model = InferenceGNN( args )

    # Load subgraph
    subgraphs = utils.read_graphs(
        f"{data_path}/{args.source}/{'non' if not args.iso else ''}iso_subgraphs.lg"
    )

    subgraph = subgraphs[ args.query ]
    print( "subgraph", subgraph != None )
    print( "subgraph nodes", subgraph.number_of_nodes() )
    utils.plotGraph(
        subgraph,
        with_label=False,
        title=f"{args.result_dir}/{args.source}_{args.query}_{'iso' if args.iso else 'noniso'}_subgraph.pdf",
    )

    # Load graph
    graphs = utils.read_graphs( f"{data_path}/{args.source}/source.lg" )

    graph = graphs[ args.source ]
    print( "graph", graph != None )
    print( "graph nodes", graph.number_of_nodes() )
    # utils.plotGraph(graph, with_label=True)

    # Load mapping groundtruth
    mapping_gts = utils.read_mapping(
        f"{data_path}/{args.source}/{'non' if not args.iso else ''}iso_subgraphs_mapping.lg"
    )
    mapping_gt = mapping_gts[ args.query ]
    print( mapping_gt )

    results = model.predict_label( [ subgraph ], [ graph ] )
    print( "result", results[ 0 ] > args.confidence )

    # if results[0] > args.confidence:
    if True:
        interactions = model.predict_embedding( [ subgraph ], [ graph ] )
        # print("interactions", interactions[0])
        interactions = interactions[ 0 ].cpu().detach().numpy()
        n_subgraph_atom = subgraph.number_of_nodes()
        x_coord, y_coord = np.where( interactions > args.mapping_threshold )

        print( "Embedding: (subgraph node, graph node)" )
        interaction_dict = { }
        for x, y in zip( x_coord, y_coord ):
            if x < n_subgraph_atom and y >= n_subgraph_atom:
                interaction_dict[ (x, y - n_subgraph_atom) ] = interactions[ x ][ y ]
                # print("(", x, y-n_ligand_atom, ")")

            if (
                    x >= n_subgraph_atom
                    and y < n_subgraph_atom
                    and (y, x - n_subgraph_atom) not in interaction_dict
            ):
                interaction_dict[ (y, x - n_subgraph_atom) ] = interactions[ x ][ y ]
                # print("(", y, x-n_ligand_atom, ")")

        list_mapping = list( interaction_dict.keys() )
        mapping_dict = { }
        for node in subgraph.nodes:
            cnode_mapping = list(
                map(
                    lambda y: (y[ 1 ], interaction_dict[ y ]),
                    filter( lambda x: x[ 0 ] == node, list_mapping ),
                )
            )
            if len( cnode_mapping ) == 0:
                mapping_dict[ node ] = [ ]
                continue

            max_prob = max( cnode_mapping, key=lambda x: x[ 1 ] )[ 1 ]
            mapping_dict[ node ] = list(
                map( lambda x: x[ 0 ], filter( lambda y: y[ 1 ] == max_prob, cnode_mapping ) )
            )

        print( "Mapping:", mapping_dict )

        node_labels = { n: "" for n in graph.nodes }
        for sgn, list_gn in mapping_dict.items():
            for gn in list_gn:
                if len( node_labels[ gn ] ) == 0:
                    node_labels[ gn ] = str( sgn )
                else:
                    node_labels[ gn ] += ",%d" % sgn

        node_colors = { n: "gray" for n in graph.nodes }
        for node, nmaping in node_labels.items():
            if not nmaping:
                if mapping_gt[ node ] != -1:
                    node_colors[ node ] = "gold"
                continue

            list_nm = nmaping.split( "," )
            for nm in list_nm:
                if mapping_gt[ node ] == int( nm ):
                    node_colors[ node ] = "lime"
                    break

                if mapping_gt[ node ] != int( nm ) and node_colors[ node ] == "gray":
                    node_colors[ node ] = "pink"

        for gn, sgn in mapping_gt.items():
            if node_labels[ gn ] == "" and sgn != -1:
                node_labels[ gn ] = str( sgn )

        edge_colors = { n: "whitesmoke" for n in graph.edges }
        for edge in graph.edges:
            n1, n2 = edge
            # map node from graph to node in subgraph
            n1_sgs, n2_sgs = node_labels[ n1 ], node_labels[ n2 ]

            if node_colors[ n1 ] == "gray" or node_colors[ n2 ] == "gray":
                continue

            # Check wheather a link between n1, n2 in subgraph
            total_pair = len( n1_sgs.split( "," ) ) * len( n2_sgs.split( "," ) )
            count_pair = 0
            for n1_sg in n1_sgs.split( "," ):
                n1_sg = int( n1_sg )
                for n2_sg in n2_sgs.split( "," ):
                    n2_sg = int( n2_sg )
                    if (n1_sg, n2_sg) not in subgraph.edges and (
                            n2_sg,
                            n1_sg,
                    ) not in subgraph.edges:
                        count_pair += 1

            if count_pair != total_pair:
                if node_colors[ n1 ] == "lime" and node_colors[ n2 ] == "lime":
                    edge_colors[ edge ] = "black"
                elif node_colors[ n1 ] == "gold" or node_colors[ n2 ] == "gold":
                    edge_colors[ edge ] = "goldenrod"
                elif node_colors[ n1 ] == "pink" or node_colors[ n2 ] == "pink":
                    edge_colors[ edge ] = "palevioletred"
            else:
                if node_colors[ n1 ] == "pink" or node_colors[ n2 ] == "pink":
                    edge_colors[ edge ] = "palevioletred"

        utils.plotGraph(
            graph,
            nodeLabels=node_labels,
            nodeColors=list( node_colors.values() ),
            edgeColors=list( edge_colors.values() ),
            title=f"{args.result_dir}/{args.source}_{args.query}_{'iso' if args.iso else 'noniso'}.pdf",
        )

        with open(
                f"{args.result_dir}/mapping_{args.source}_{args.query}_{'iso' if args.iso else 'noniso'}.csv",
                "w",
                encoding="utf8",
        ) as f:
            f.write( "subgraph_node,graph_node,score\n" )
            for key, value in interaction_dict.items():
                f.write( "{:d},{:d},{:.3e}\n".format( key[ 0 ], key[ 1 ], value ) )
