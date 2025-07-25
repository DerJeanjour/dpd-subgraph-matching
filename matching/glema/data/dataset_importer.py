import os.path

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.data.process.data_generator as data_generator
import matching.glema.data.process.data_synthesis as data_synthesis
import matching.glema.data.process.import_dataset as importer
import matching.glema.data.process.process_data as data_processor


def clean_up( args ):
    data_paths = [
        os.path.join( args.raw_dataset_dir, f"{args.dataset}" ),
        os.path.join( args.dataset_dir, f"{args.dataset}_train" ),
        os.path.join( args.dataset_dir, f"{args.dataset}_test" ),
        os.path.join( args.data_processed_dir, f"{args.dataset}{'_directed' if args.directed else ''}" )
    ]
    data_paths = [ io_utils.get_abs_file_path( path ) for path in data_paths ]
    io_utils.delete_paths( data_paths )


def process( args ):
    importer.import_datasets( args )
    data_generator.process( args )
    if not args.inference:
        if not args.split_data:
            data_synthesis.process( args )
        data_processor.process( args )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "pmart_all"
    args.import_prefix = "p_mart"
    args.seed = 42
    # args.num_workers = 1
    args.inference = True
    args.split_data = False
    args.induced = True
    args.import_subgraph_radius = 5
    args.import_subgraph_max = 42
    args.import_subgraph_min = 7
    #args.num_subgraphs = 64
    args.num_subgraphs = 2
    args.real = True
    args.testonly = False
    args.directed = False
    args.max_subgraph = -1
    print( args )

    clean_up( args )
    process( args )
