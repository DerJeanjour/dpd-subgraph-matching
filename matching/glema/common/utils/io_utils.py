import os
from pathlib import Path

import matching.misc.utils as utils


def get_abs_file_path( project_file_path: str, with_subproject=True ) -> str:
    local_path: Path = Path( project_file_path )
    if with_subproject:
        local_path = "glema" / local_path
    return utils.get_abs_file_path( str( local_path ) )


def get_project_root() -> Path:
    return utils.get_project_root()


def path_exists( path ):
    return utils.path_exists( path )


def delete_path( path: str, dry_run=False ):
    utils.delete_path( path, dry_run=dry_run )


def delete_paths( paths: list[ str ], dry_run=False ):
    utils.delete_paths( paths, dry_run=dry_run )


def get_filenames_in_dir( dir_path, only_files=True ):
    return utils.get_filenames_in_dir( dir_path, only_files=only_files )


def ensure_dir( dir, args ):
    dir = os.path.join( dir, f"{args.dataset}_{args.tactic}" )
    if args.tactic == "static":
        dir += f"{args.nhop}"

    if args.nhead > 1:
        dir += f"_nhead{args.nhead}"

    if args.branch != "both":
        dir += "_" + args.branch

    if args.directed:
        dir += "_directed"

    if args.tag != "":
        dir += "_" + args.tag

    dir = get_abs_file_path( dir )

    # make save dir if it doesn't exist
    if not os.path.isdir( dir ):
        os.system( "mkdir -p " + dir )

    return dir
