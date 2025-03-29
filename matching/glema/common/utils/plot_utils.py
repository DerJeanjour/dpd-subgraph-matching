import os

import matplotlib.pyplot as plt
import networkx as nx

import matching.glema.common.utils.io_utils as io_utils
import matching.misc.utils as utils


def plot_graph(
        G: nx.Graph,
        nodeLabels=None,
        node_sizes=None,
        edge_width=0.5,
        with_label=True,
        nodeColors=None,
        edgeColors=None,
        title=None,
        show_title=True,
        pos=None,
        figsize=(6, 4),
        ax=None,
        show=True
):
    if ax is None:
        fig, ax = plt.subplots( figsize=figsize )

    if pos is None:
        pos = nx.spring_layout( G, seed=42 )

    if node_sizes is None:
        node_sizes = 200
    if not nodeColors:
        nodeColors = "skyblue"
    if not edgeColors:
        edgeColors = "gray"

    nx.draw( G,
             pos=pos,
             ax=ax,
             with_labels=with_label,
             labels=nodeLabels,
             node_color=nodeColors,
             node_size=node_sizes,
             font_size=6,
             font_color="black",
             width=edge_width,
             edge_color=edgeColors )

    if title is None:
        title = f"Graph with {len( G.nodes )} nodes and {len( G.edges )} edges"

    if show_title:
        ax.set_title( title, size=10 )
    if show and ax is None:
        plt.show()
    return ax


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


class ColorScheme:
    PRIMARY: str = "#FF9F1C"
    PRIMARY_COMP: str = "#1C7CFF"
    PRIMARY_LIGHT: str = "#FFBF69"
    PRIMARY_LIGHT_COMP: str = "#69A9FF"
    PRIMARY_DARK: str = "#a76000"
    SECONDARY: str = "#2EC4B6"
    SECONDARY_COMP: str = "#C42E3C"
    SECONDARY_LIGHT: str = "#CBF3F0"
    SECONDARY_LIGHT_COMP: str = "#F3CBCE"
    SECONDARY_DARK: str = "#1B726A"
    HIGHLIGHT: str = "#69A9FF"

    @staticmethod
    def all_high_contrast() -> list[ str ]:
        return [
            ColorScheme.PRIMARY,
            ColorScheme.SECONDARY,
            ColorScheme.PRIMARY_COMP,
            ColorScheme.SECONDARY_COMP,
            ColorScheme.PRIMARY_LIGHT,
            ColorScheme.SECONDARY_LIGHT,
            ColorScheme.PRIMARY_LIGHT_COMP,
            ColorScheme.SECONDARY_LIGHT_COMP,
        ]
