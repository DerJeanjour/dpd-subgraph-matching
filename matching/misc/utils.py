import copy
import datetime
import random

import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
import torch


def get_timestamp():
    return datetime.datetime.now().strftime( "%Y-%m-%dT%H-%M" )


def set_seed( seed ):
    random.seed( seed )
    np.random.seed( seed )
    torch.manual_seed( seed )
    torch.cuda.manual_seed_all( seed )
    torch.cuda.manual_seed_all( seed )


def get_device( force_cpu=False ):
    use_cuda = torch.cuda.is_available()
    use_mps = not use_cuda and torch.backends.mps.is_available()

    if not force_cpu and use_cuda:
        device = torch.device( "cuda" )
    elif not force_cpu and use_mps:
        device = torch.device( "mps" )
    else:
        device = torch.device( "cpu" )

    return device


def model_uses_cuda( model: torch.nn.Module ) -> bool:
    return next( model.parameters() ).is_cuda


def generate_graph( size: int, directed=False ):
    return nx.binomial_graph( size, p=0.05, directed=directed )


def random_subgraph( G, k ):
    random_node = random.choice( list( G.nodes() ) )
    return nx.ego_graph( G, random_node, radius=k )


def plot_graph( G,
                with_label=False,
                nodeLabels=None,
                nodeColors=None,
                edgeColors=None,
                title=None ):
    plt.figure( figsize=(6, 4) )

    pos = nx.spring_layout( G, seed=42 )
    node_size = 200
    if not with_label:
        node_size *= 0.1
    if not nodeColors:
        nodeColors = "skyblue"
    if not edgeColors:
        edgeColors = "gray"

    nx.draw( G,
             pos=pos,
             with_labels=with_label,
             labels=nodeLabels,
             node_color=nodeColors,
             node_size=node_size,
             font_size=6,
             font_color="black",
             width=0.5,
             edge_color=edgeColors )

    if title is None:
        title = f"Graph with {len( G.nodes )} nodes and {len( G.edges )} edges"

    plt.title( title, size=10 )
    plt.show()


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
