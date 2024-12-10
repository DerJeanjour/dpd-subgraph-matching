import torch

import matching.misc.utils as utils


def get_timestamp():
    return utils.get_timestamp()


def get_device( force_cpu=True ):
    return utils.get_device( force_cpu )


def model_uses_cuda( model: torch.nn.Module ) -> bool:
    return utils.model_uses_cuda( model )


def generate_graph( size: int, directed=False ):
    return utils.generate_graph( size, directed )


def plot_graph( G, with_label=False ):
    utils.plot_graph( G, with_label=with_label )


def random_subgraph( G, k ):
    return utils.random_subgraph( G, k )


def inject_edge_errors( G, e: int = 1 ):
    return utils.inject_edge_errors( G, e )
