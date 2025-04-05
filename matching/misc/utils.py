import copy
import datetime
import os
import random
import shutil
from pathlib import Path

import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
import torch


def get_abs_file_path( project_file_path: str ) -> str:
    abs_path: Path = get_project_root() / project_file_path
    # if not abs_path.exists():
    #    raise FileNotFoundError( f"The file '{abs_path}' does not exist." )
    return str( abs_path )


def get_project_root() -> Path:
    return Path( __file__ ).parent.parent


def path_exists( path ) -> bool:
    return os.path.exists( path )


def delete_path( path: str, dry_run=False ):
    if path_exists( path ):
        try:
            if os.path.isfile( path ) or os.path.islink( path ):
                if not dry_run:
                    os.remove( path )
                print( f"Deleted file: {path}" )
            elif os.path.isdir( path ):
                if not dry_run:
                    shutil.rmtree( path )
                print( f"Deleted directory: {path}" )
        except Exception as e:
            print( f"Error deleting {path}: {e}" )
    else:
        print( f"Can't delete non existing path: {path}" )


def delete_paths( paths: list[ str ], dry_run=False ):
    for path in paths:
        delete_path( path, dry_run=dry_run )


def get_filenames_in_dir( dir_path, only_files=True ):
    return [ f for f in os.listdir( dir_path ) if not only_files or os.path.isfile( os.path.join( dir_path, f ) ) ]


def get_timestamp() -> str:
    return datetime.datetime.now().strftime( "%Y-%m-%dT%H-%M" )


def set_seed( seed ):
    random.seed( seed )
    np.random.seed( seed )
    torch.manual_seed( seed )
    torch.cuda.manual_seed_all( seed )
    torch.cuda.manual_seed_all( seed )


def get_device( force_cpu=False ) -> torch.device:
    use_cuda = torch.cuda.is_available()
    use_mps = not use_cuda and torch.backends.mps.is_available()

    if not force_cpu and use_cuda:
        device = torch.device( "cuda" )  # cuda:0 ?
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
