import os

import matplotlib.pyplot as plt
import networkx as nx

import matching.glema.common.utils.io_utils as io_utils
import matching.misc.utils as utils


def plot_graph(
        graph: nx.Graph,
        nodeLabels=None,
        with_label=True,
        nodeColors=None,
        edgeColors=None,
        title=None,
        pos=None
):
    utils.plot_graph( graph, nodeLabels=nodeLabels, with_label=with_label, nodeColors=nodeColors, edgeColors=edgeColors,
                      title=title, pos=pos )


def save_graph_debug( G, file_name ):
    file_path = io_utils.get_abs_file_path( "debug/" )
    if not os.path.exists( file_path ):
        os.mkdir( file_path )
    file_path = os.path.join( file_path, file_name )
    try:
        # Set up the plot
        plt.figure( figsize=(8, 8) )
        plt.axis( 'off' )  # Turn off axis

        # Draw the graph without labels
        pos = nx.spring_layout( G )  # Compute layout
        nx.draw( G, pos, with_labels=False, node_color="lightblue", edge_color="gray", node_size=500 )

        # Save to file
        plt.savefig( file_path, format='png', bbox_inches='tight' )
        print( f"Graph rendered and saved to {file_path}" )
    except Exception as e:
        print( f"An error occurred while rendering the graph: {e}" )
    finally:
        plt.close()  # Ensure the plot is closed to free memory
