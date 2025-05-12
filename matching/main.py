import os
import sys

project_root = os.path.abspath( os.path.join( os.path.dirname( __file__ ), '..' ) )
if project_root not in sys.path:
    sys.path.insert( 0, project_root )

import matching.glema.common.utils.arg_utils as arg_utils
import matching.misc.import_repository_datasets as data_importer
import matching.glema.data.dataset_importer as data_processor
import matching.glema.evaluation.evaluate_pattern_matching as epm
import matching.glema.common.utils.model_utils as model_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.misc.cpg_const as cpg_const
from matching.glema.common.dataset import DesignPatternDataset
from matching.glema.common.model import InferenceGNN
import matching.glema.common.utils.io_utils as io_utils


def detect_patterns( args, model_name, source_dataset, pattern_dataset ):
    args.dataset = model_name
    version = model_utils.get_latest_model_version( args )
    model_name = model_utils.get_model_name( args, version )
    args = arg_utils.load_args( args, model_name )
    args.pattern_dataset = pattern_dataset
    args.test_data = True
    args.batch_size = 128

    pattern_types = [
        # cpg_const.DesignPatternType.ABSTRACT_FACTORY.value,
        cpg_const.DesignPatternType.ADAPTER.value,
        cpg_const.DesignPatternType.BUILDER.value,
        # cpg_const.DesignPatternType.FACADE.value,
        cpg_const.DesignPatternType.FACTORY_METHOD.value,
        cpg_const.DesignPatternType.OBSERVER.value,
        cpg_const.DesignPatternType.SINGLETON.value,
        cpg_const.DesignPatternType.DECORATOR.value,
        # cpg_const.DesignPatternType.MEMENTO.value,
        # cpg_const.DesignPatternType.PROXY.value,
        # cpg_const.DesignPatternType.VISITOR.value
    ]

    # initialize
    model = InferenceGNN( args )
    args.dataset = source_dataset
    dataset = DesignPatternDataset( args,
                                    max_pattern_examples=30,
                                    query_pattern=True,
                                    pattern_types=pattern_types )

    min_nodes = 5
    max_norm_d = 12
    sources = dataset.get_sources()
    # sources = epm.filter_sources( sources, dataset.get_source_patterns(), max_sources_per_pattern=-1, max_na_patterns=5 )
    sources = epm.normalize_sources( sources, max_distance=max_norm_d, min_nodes=min_nodes )

    patterns = dataset.get_patterns()
    patterns = epm.normalize_patterns( patterns, max_distance=max_norm_d, min_nodes=min_nodes )
    # patterns = get_common_patterns( patterns, max_node_distance=max_norm_d )

    dataset.set_sources( sources )
    dataset.set_patterns( patterns )
    dataset.compute_samples()

    # inference
    preds, metas, sources, queries = epm.inference( model, dataset, args,
                                                    collect_graphs=True )

    # aggregation
    groups_by_source = epm.group_by_source( metas )
    source_preds = epm.compute_source_preds( groups_by_source, preds, metas,
                                             pred_aggregator=epm.aggregate_preds_by_quantile,
                                             q=0.9 )

    print( f"{'-' * 8} DETECTION RESULTS [{source_dataset}] {'-' * 8}" )
    conf = 0.5
    for idx, (gidx, sample_groups) in enumerate( groups_by_source.items() ):
        sample_id = -1
        for sample_group in sample_groups.values():
            if len( sample_group ) > 0:
                sample_id = sample_group[ 0 ]
                break
        if sample_id < 0:
            print( f"Could not find valid sample for gidx {gidx} ..." )
            continue
        meta = metas[ sample_id ]
        record_dataset = meta[ "record_dataset" ]
        record_scope = meta[ "record_scope" ]
        source_pred = misc_utils.sort_dict_by_value( source_preds[ gidx ], reverse=True )
        source_pred = { pt: f"{pred:.3f}" for pt, pred in source_pred.items() if pred >= conf }
        print( f"{record_dataset.replace( 'custom-', '' )}::{record_scope} = {source_pred}" )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    print( args )

    args.import_prefix = f"custom-{args.name}"
    args.dataset = args.name
    args.num_subgraphs = 2

    import_dir = io_utils.get_abs_file_path( args.import_dir, with_subproject=False )
    cached_imports = io_utils.get_filenames_in_dir( import_dir )
    use_cache = args.use_cache and any( cache.startswith( args.import_prefix ) for cache in cached_imports )
    if not use_cache:
        data_importer.import_dataset( args )
        data_processor.clean_up( args )
        data_processor.process( args )
    else:
        print( f"Use cache for dataset {args.dataset}" )

    source_dataset = args.name
    pattern_dataset = args.pattern_dataset
    model_name = args.model
    detect_patterns( args, model_name, source_dataset, pattern_dataset )
