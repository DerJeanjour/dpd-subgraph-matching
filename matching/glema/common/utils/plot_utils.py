import os

import matplotlib.pyplot as plt
import networkx as nx
from matplotlib.patches import Patch

import matching.glema.common.utils.io_utils as io_utils


def plot_graph(
        G: nx.Graph,
        nodeLabels=None,
        node_sizes=None,
        edge_width=0.5,
        with_label=True,
        edgeLabels=None,
        nodeColors=None,
        edgeColors=None,
        title=None,
        show_title=True,
        pos=None,
        font_size=6,
        figsize=(6, 4),
        ax=None,
        show=True,
        save_name=None,
        color_legend=None,
        margin=None
):
    if ax is None:
        fig, ax = plt.subplots( figsize=figsize )

    if pos is None:
        pos = nx.spring_layout( G, seed=42 )

    if margin is not None:
        ax.margins( margin )

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
             font_size=font_size,
             font_color="black",
             width=edge_width,
             edge_color=edgeColors )

    if edgeLabels is not None:
        nx.draw_networkx_edge_labels( G, pos, ax=ax, edge_labels=edgeLabels, font_color="black" )

    # Create and display a legend at the top right of the whole plot.
    if color_legend is not None:
        legend_elements = [ Patch( facecolor=color, edgecolor='black', label=label )
                            for color, label in color_legend.items() ]
        ax.legend( handles=legend_elements, loc='upper right', fontsize=font_size )

    if title is None:
        title = f"Graph with {len( G.nodes )} nodes and {len( G.edges )} edges"

    if show_title:
        ax.set_title( title, size=font_size + 4 )
    if show and ax is None and save_name is None:
        plt.show()

    if save_name is not None:
        plt.savefig( save_name, format='png', bbox_inches='tight' )

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
    GREY: str = "grey"
    GREY_LIGHT: str = "lightgrey"

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
