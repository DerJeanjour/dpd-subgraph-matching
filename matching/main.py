import os
import sys

project_root = os.path.abspath( os.path.join( os.path.dirname( __file__ ), '..' ) )
if project_root not in sys.path:
    sys.path.insert( 0, project_root )

import matching.glema.common.utils.arg_utils as arg_utils
import matching.misc.import_repository_datasets as data_importer
import matching.glema.data.dataset_importer as data_processor

if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.import_prefix = f"custom-{args.name}"
    args.dataset = args.name
    args.num_subgraphs = 2

    data_importer.import_dataset( args )
    data_processor.clean_up( args )
    data_processor.process( args )
    # TODO matching
