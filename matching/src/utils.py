import copy
import datetime
import random

import matplotlib.pyplot as plt
import networkx as nx
import torch


def get_timestamp():
    return datetime.datetime.now().strftime( "%Y-%m-%dT%H-%M" )


def get_device( force_cpu=False ):
    use_cuda = torch.cuda.is_available()
    use_mps = not use_cuda and torch.backends.mps.is_available()

    if not force_cpu and use_cuda:
        device = torch.device( "cuda" )
    #elif not force_cpu and use_mps:
    #    device = torch.device( "mps" )
    else:
        device = torch.device( "cpu" )

    return device

def model_uses_cuda( model: torch.nn.Module ) -> bool:
    return next(model.parameters()).is_cuda


def generate_graph( size: int, directed=False ):
    return nx.binomial_graph( size, p=0.05, directed=directed )


def plot_graph( G, with_label=False ):
    plt.figure( figsize=(6, 4) )
    node_size = 200
    if not with_label:
        node_size = node_size * 0.1
    nx.draw( G,
             pos=nx.spring_layout( G, seed=42 ),
             with_labels=with_label,
             node_color="skyblue",
             node_size=node_size,
             font_size=6,
             font_color="black",
             width=0.5,
             edge_color="gray" )
    plt.title( f"Graph with {len( G.nodes )} nodes and {len( G.edges )} edges", size=10 )
    plt.show()


def random_subgraph( G, k ):
    random_node = random.choice( list( G.nodes() ) )
    return nx.ego_graph( G, random_node, radius=k )


def inject_edge_errors( G, e: int = 1 ):
    G_error = copy.deepcopy( G )
    for _ in range( e ):
        modification_type = random.choice( [ 'add', 'delete' ] )
        if modification_type == 'delete':
            if G_error.number_of_edges() > 0:
                idx_remove = random.randint( 0, len( G_error.edges() ) )
                edge_to_remove = list( G_error.edges() )[ idx_remove ]
                G_error.remove_edge( *edge_to_remove )
                # print(f"Deleted edge: {edge_to_remove}")
        elif modification_type == 'add':
            possible_edges = list( nx.non_edges( G_error ) )  # Edges that do not currently exist
            if possible_edges:
                idx_add = random.randint( 0, len( possible_edges ) )
                edge_to_add = possible_edges[ idx_add ]
                G_error.add_edge( *edge_to_add )
                # print(f"Added new edge: {edge_to_add}")
    return G_error
