import matching.glema.common.utils as utils
import matching.glema.data.data_real.import_dataset as importer
import matching.glema.data.data_real.data_generator as data_generator
import matching.glema.data.data_real.data_synthesis as data_synthesis
import matching.glema.data.process_data as data_processor


def process( args ):
    importer.import_datasets( args )
    data_generator.process( args )
    if not args.split_data:
        data_synthesis.process( args )
    data_processor.process( args )


if __name__ == "__main__":
    args = utils.parse_args()
    args.dataset = "CPG"
    args.seed = 42
    # args.num_workers = 1
    args.split_data = True
    args.import_subgraph_radius = 4
    args.import_subgraph_max = 40
    args.import_subgraph_min = 2
    args.num_subgraphs = 20
    args.real = True
    args.testonly = False
    args.directed = True
    args.max_subgraph = -1
    print( args )

    process( args )
