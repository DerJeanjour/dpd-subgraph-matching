import torch
import networkx as nx


def get_device(force_cpu=True):
    use_cuda = torch.cuda.is_available()
    use_mps = not use_cuda and torch.backends.mps.is_available()

    if not force_cpu and use_cuda:
        device = torch.device("cuda")
        print("Using: CUDA")
    elif not force_cpu and use_mps:
        device = torch.device("mps")
        print("Using: METAL")
    else:
        device = torch.device("cpu")
        print("Using: CPU")

    return device


def generate_graph(size: int):
    return nx.binomial_graph(size, p=0.05, directed=False)
