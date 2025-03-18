import itertools
import os
from collections.abc import Callable

import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.metrics import confusion_matrix, ConfusionMatrixDisplay
from sklearn.preprocessing import label_binarize
from tqdm import tqdm

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.graph_utils as graph_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.common.utils.model_utils as model_utils
import matching.misc.cpg_const as cpg_const
from matching.glema.common.dataset import DesignPatternDataset
from matching.glema.common.model import InferenceGNN


def get_common_patterns( patterns_by_type: dict[ str, list[ nx.Graph ] ],
                         min_nodes: int = 8, max_node_distance=6,
                         max_graphs: int = 5, max_iter: int = 10 ) -> dict[
    str, list[ nx.Graph ] ]:
    # normalize patterns
    patterns_normalized_by_type = { }
    print( "Normalize patterns ..." )
    for pattern_type in tqdm( patterns_by_type.keys() ):
        patterns = patterns_by_type[ pattern_type ]
        patterns_normalized_by_type[ pattern_type ] = [
            graph_utils.normalize_graph( p, max_distance=max_node_distance )[ 0 ]
            for p in patterns ]

    # compute common graphs
    common_patterns_by_type = { }
    for pattern_type in patterns_normalized_by_type.keys():
        print( f"Compute common patterns for {pattern_type} ..." )
        norm_patterns = patterns_normalized_by_type[ pattern_type ]
        common_patterns_by_type[ pattern_type ] = get_common_patterns_for_type( norm_patterns, min_nodes,
                                                                                max_graphs, max_iter )
    return common_patterns_by_type


def get_common_patterns_for_type( patterns_normalized_of_type: list[ nx.Graph ],
                                  min_nodes: int = 8, max_graphs: int = 5, max_iter: int = 10 ) -> list[ nx.Graph ]:
    common_patterns = patterns_normalized_of_type
    i = 0
    # iteratively compute pairwise common graphs
    while len( common_patterns ) > max_graphs and i < max_iter:
        pairs = list( itertools.combinations( common_patterns, 2 ) )
        common_patterns = [ ]
        for (G1, G2) in pairs:
            common_pattern = graph_utils.get_norm_graph_intersection( G1, G2 )
            # filter duplicates
            if any( graph_utils.norm_graphs_are_equal( common_pattern, G ) for G in common_patterns ):
                continue
            common_patterns.append( common_pattern )
        # filter by node size
        common_patterns = [ G for G in common_patterns if G.number_of_nodes() >= min_nodes ]
        print( f"Size after iter {i}: {len( common_patterns )}" )
        i += 1
    return common_patterns


def normalize_pattern_to_connected( patterns_all: dict[ str, list[ nx.Graph ] ],
                                    max_distance=8 ) -> dict[ str, nx.Graph ]:
    print( "Normalizing patterns to connected ..." )
    norm_connected_pattern_graphs = { }
    for pattern_type in tqdm( patterns_all.keys() ):
        patterns = patterns_all[ pattern_type ]
        connected = graph_utils.connect_graphs_at_anchor( patterns, keep_radius=max_distance )
        normalized, _ = graph_utils.normalize_graph( connected, max_distance=max_distance )
        norm_connected_pattern_graphs[ pattern_type ] = normalized
    return norm_connected_pattern_graphs


def filter_normalized_by_presence( norm_connected_pattern_graphs: dict[ str, nx.Graph ],
                                   num_graphs=1, n_start=12, n_decay=2 ):
    print( "Filter connected patterns by presence ..." )
    norm_pattern_graphs = { }
    for pattern_type in tqdm( norm_connected_pattern_graphs.keys() ):
        normalized = norm_connected_pattern_graphs[ pattern_type ]
        norm_pattern_graphs[ pattern_type ] = [ ]
        top_n = n_start
        while len( norm_pattern_graphs[ pattern_type ] ) < num_graphs:
            n_presence = [ data[ "presence" ] for n, data in normalized.nodes( data=True ) ]
            presence_thresh = min( sorted( n_presence )[ -top_n: ] )
            n_keep = [ nid for nid, data in normalized.nodes( data=True ) if data[ "presence" ] >= presence_thresh ]
            filtered = normalized.subgraph( n_keep )
            norm_pattern_graphs[ pattern_type ].append( filtered )
            top_n -= n_decay
    return norm_pattern_graphs


def normalize_patterns_by_presence( patterns_all: dict[ str, list[ nx.Graph ] ],
                                    num_graphs=1, max_distance=8,
                                    n_start=12, n_decay=2 ) -> dict[ str, list[ nx.Graph ] ]:
    norm_connected_pattern_graphs = normalize_pattern_to_connected( patterns_all,
                                                                    max_distance=max_distance )
    norm_pattern_graphs = filter_normalized_by_presence( norm_connected_pattern_graphs, num_graphs=num_graphs,
                                                         n_start=n_start, n_decay=n_decay )
    return norm_pattern_graphs


def normalize_patterns( patterns_all: dict[ str, list[ nx.Graph ] ], max_distance=8 ) -> dict[ str, list[ nx.Graph ] ]:
    print( "Normalizing patterns ..." )
    patterns_all_norm = { }
    for dp_type, patterns in tqdm( patterns_all.items() ):
        norm_patterns = [ graph_utils.normalize_graph( dp, max_distance=max_distance )[ 0 ] for dp in patterns ]
        patterns_all_norm[ dp_type ] = norm_patterns
    return patterns_all_norm


def normalize_sources( sources: dict[ int, nx.Graph ], max_distance=8 ) -> dict[ int, nx.Graph ]:
    print( "Normalizing sources ..." )
    norm_sources = { }
    for gidx, source in tqdm( sources.items() ):
        norm_source, _ = graph_utils.normalize_graph( source, max_distance=max_distance )
        norm_sources[ gidx ] = norm_source
    return norm_sources


def filter_sources( sources: dict[ int, nx.Graph ], source_patterns: dict[ int, str ], max_sources_per_pattern=-1 ):
    print( "Filtering sources ..." )
    pattern_idxs = { }
    for gidx, source in tqdm( sources.items() ):
        pattern = source_patterns[ gidx ]
        if pattern not in pattern_idxs:
            pattern_idxs[ pattern ] = [ ]
        pattern_idxs[ pattern ].append( gidx )

    if max_sources_per_pattern > 0:
        pattern_idxs = { p: idxs[ :max_sources_per_pattern ] for p, idxs in pattern_idxs.items() }

    filtered_sources = { }
    for idxs in pattern_idxs.values():
        for gidx in idxs:
            filtered_sources[ gidx ] = sources[ gidx ]
    return filtered_sources


def sample_processor_k_normalized( source: nx.Graph, query: nx.Graph, meta: dict,
                                   min_d_offset=1, max_d_offset=5 ) -> tuple[ list, list, list ]:
    sources, queries, metas = [ ], [ ], [ ]
    source, source_d = graph_utils.normalize_graph( source, max_distance=8 )
    if source_d < 3:
        return sources, queries, metas

    start_d = source_d
    last_d = -1
    for i in list( range( min_d_offset, max_d_offset + 1 ) ):
        query_max_d = start_d - i
        if query_max_d < 2:
            continue
        query, query_d = graph_utils.normalize_graph( query, max_distance=query_max_d + 1 )
        if last_d == query_d:
            continue
        last_d = query_d
        query = graph_utils.subgraph_from_anchor_of_size( query, source.number_of_nodes() - 1 )
        queries.append( query )
        sources.append( source )
        # pred_w = query_d / source_d
        # TODO weight the relation to the original size of the query instead to the source
        pred_w = query.number_of_nodes() / source.number_of_nodes()
        _meta = meta.copy()
        _meta[ "pred_w" ] = pred_w
        metas.append( _meta )
    return sources, queries, metas


def sample_processor_subgraph_normalized( source: nx.Graph, query: nx.Graph, meta: dict ) -> tuple[ list, list, list ]:
    combined, node_matches, _ = graph_utils.combine_normalized( source, query )
    n_match = [ nid for nid, match in zip( combined.nodes(), node_matches ) if match == 1 ]
    n_not_match = [ nid for nid, match in zip( combined.nodes(), node_matches ) if match < 0 ]

    _meta = meta.copy()
    _meta[ "n_match" ] = len( n_match )
    _meta[ "n_not_match" ] = len( n_not_match )
    if len( n_match ) == 0:
        _meta[ "pred_w" ] = 0.0
    else:
        _meta[ "pred_w" ] = max( len( n_match ) - len( n_not_match ), 0 ) / len( n_match )
        _meta[ "pred_w" ] = _meta[ "pred_w" ] * (3 / 4) + (1 / 4)

    return sample_processor_default( source, query, _meta )


def sample_processor_default( source: nx.Graph, query: nx.Graph, meta: dict ) -> tuple[ list, list, list ]:
    _meta = meta.copy()
    _meta[ "pred_w" ] = _meta.get( "pred_w", 1.0 )
    # _meta[ "pred_w" ] = float( cpg_const.NO_DESIGN_PATTERN not in [ meta[ "source_type" ], meta[ "pattern_type" ] ] )
    # _meta[ "pred_w" ] = float( meta[ "source_type" ] == meta[ "pattern_type" ] )
    """
    if cpg_const.NO_DESIGN_PATTERN in [ meta[ "source_type" ], meta[ "pattern_type" ] ]:
        _meta[ "pred_w" ] *= 0.1
    elif meta[ "source_type" ] != meta[ "pattern_type" ]:
        _meta[ "pred_w" ] *= 0.99
    elif meta[ "source_type" ] == meta[ "pattern_type" ]:
        _meta[ "pred_w" ] = min( _meta[ "pred_w" ] * 3.0, 1.0 )
        _meta[ "pred_r" ] = 0.9
    """
    return [ source ], [ query ], [ _meta ]


def inference( model: InferenceGNN, dataset: DesignPatternDataset, args, collect_graphs=False,
               sample_processor: Callable[
                   [ nx.Graph, nx.Graph, dict, dict ],
                   tuple[ list[ nx.Graph ], list[ nx.Graph ], list[ dict ] ]
               ] = sample_processor_default, **kwargs ) -> tuple[ list, list, list, list ]:
    print( "Inference of dataset ..." )
    preds, metas, all_source, all_queries = [ ], [ ], [ ], [ ]
    batches = misc_utils.partition_list( list( range( 0, dataset.__len__() ) ), args.batch_size )
    for batche_idxs in tqdm( batches ):
        sources, queries = [ ], [ ]
        for idx in batche_idxs:
            source, query, meta = dataset.get_data( idx )
            source, query, meta = sample_processor( source, query, meta, **kwargs )
            sources.extend( source )
            queries.extend( query )
            metas.extend( meta )
        if collect_graphs:
            all_source.extend( sources )
            all_queries.extend( queries )
        query_preds, _ = model.predict_batch( sources, queries, bulk_size=args.batch_size )
        preds.extend( query_preds )
    preds = [ float( p ) for p in preds ]
    return preds, metas, all_source, all_queries


def print_pattern_counts( dataset: DesignPatternDataset ):
    # source patterns
    source_pattern_counts = { }
    for gidx, pattern in dataset.get_source_patterns().items():
        if gidx not in dataset.get_sources():
            continue
        if pattern not in source_pattern_counts:
            source_pattern_counts[ pattern ] = 0
        source_pattern_counts[ pattern ] += 1
    print( "source_pattern_counts:", misc_utils.sort_dict_by_key( source_pattern_counts ) )
    # query patterns
    pattern_example_counts = { dp: len( l ) for dp, l in dataset.get_patterns().items() }
    print( "pattern_example_counts:", misc_utils.sort_dict_by_key( pattern_example_counts ) )


def group_by_source( metas ) -> dict[ int, dict[ str, list[ int ] ] ]:
    groups_by_source: dict[ int, dict[ str, list[ int ] ] ] = { }
    for idx, meta in enumerate( metas ):
        gidx = meta[ "gidx" ]
        pattern_type = meta[ "pattern_type" ]
        if gidx not in groups_by_source:
            groups_by_source[ gidx ] = { }
        if pattern_type not in groups_by_source[ gidx ]:
            groups_by_source[ gidx ][ pattern_type ] = [ ]
        groups_by_source[ gidx ][ pattern_type ].append( idx )
    return groups_by_source


def aggregate_preds_mean( preds: list[ float ] ) -> float:
    return float( np.mean( preds ) )


def aggregate_preds_by_quantile( preds: list[ float ], q=0.8 ) -> float:
    return float( np.quantile( preds, q ) )


def compute_source_preds( groups_by_source: dict[ int, dict[ str, list[ float ] ] ],
                          preds: list[ float ], metas: list[ dict ],
                          pred_aggregator: Callable[ [ list[ float ], dict ], float ] = aggregate_preds_mean,
                          **kwargs ) -> dict[ int, dict[ str, float ] ]:
    source_preds = { }
    for gidx, source_preds_idxs in groups_by_source.items():
        source_pattern_preds = { }
        for dp, idxs in source_preds_idxs.items():
            pattern_preds = [ preds[ idx ] for idx in idxs ]
            pattern_metas = [ metas[ idx ] for idx in idxs ]
            pattern_preds_weighted = [ ]
            for p, m in zip( pattern_preds, pattern_metas ):
                p = m.get( "pred_r", p )
                p *= m.get( "pred_w", 1.0 )
                pattern_preds_weighted.append( p )
            source_pattern_preds[ dp ] = pred_aggregator( pattern_preds_weighted, **kwargs )
        source_preds[ gidx ] = source_pattern_preds
    return source_preds


def compute_source_types( metas: list[ dict ] ) -> dict[ int, str ]:
    return { m[ "gidx" ]: m[ "source_type" ] for m in metas }


def compute_labels_legacy( source_types: dict[ int, str ],
                           source_preds: dict[ int, dict[ str, float ] ],
                           conf=0.5, top_k=1 ) -> tuple[ list[ str ], list[ str ], list[ float ] ]:
    true_labels = [ ]
    pred_labels = [ ]
    pred_scores = [ ]
    for gidx, source_type in source_types.items():
        source_pattern_preds = source_preds[ gidx ]
        source_pred_scores = [ ]
        source_pred_types = [ ]
        for dp_type, dp_pred in source_pattern_preds.items():
            source_pred_scores.append( dp_pred )
            source_pred_types.append( dp_type if dp_pred > conf else cpg_const.NO_DESIGN_PATTERN )
        top_indices = np.argsort( source_pred_scores )[ -top_k: ][ ::-1 ]
        for i in top_indices:
            true_labels.append( source_type )
            pred_labels.append( source_pred_types[ i ] )
            pred_scores.append( source_pred_scores[ i ] )
    return true_labels, pred_labels, pred_scores


def compute_labels( source_types: dict[ int, str ],
                    source_preds: dict[ int, dict[ str, float ] ],
                    conf=0.5 ) -> tuple[ list[ str ], list[ str ], list[ float ] ]:
    true_labels = [ ]
    pred_labels = [ ]
    pred_scores = [ ]
    for gidx, source_type in source_types.items():
        source_pattern_preds = source_preds[ gidx ]
        source_pred_type = cpg_const.NO_DESIGN_PATTERN
        source_pred_score = 0.0
        for dp_type, dp_pred in source_pattern_preds.items():
            if dp_pred > conf:
                if dp_pred > source_pred_score:
                    source_pred_score = dp_pred
                    source_pred_type = dp_type
                if source_type == dp_type:
                    source_pred_type = dp_type
                    source_pred_score = dp_pred
                    break

        true_labels.append( source_type )
        pred_labels.append( source_pred_type )
        pred_scores.append( source_pred_score )

    return true_labels, pred_labels, pred_scores


def to_numeric_labels( true_labels: list[ str ], pred_labels: list[ str ] ) -> tuple[ list[ int ], list[ int ] ]:
    def enum_to_numeric( value: str ) -> int:
        if value == cpg_const.NO_DESIGN_PATTERN:
            return 0
        enum = misc_utils.get_enum_by_value( cpg_const.DesignPatternType, value )
        return misc_utils.get_enum_idx( enum )

    x_labels = [ enum_to_numeric( l ) for l in pred_labels ]
    y_labels = [ enum_to_numeric( l ) for l in true_labels ]
    return x_labels, y_labels


def compute_metrics( x_labels: list[ int ], y_labels: list[ int ] ) -> dict[ str: float ]:
    metrics: dict[ str: float ] = { }
    metrics[ "acc" ] = accuracy_score( y_labels, x_labels )
    metrics[ "pre" ] = precision_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
    metrics[ "rec" ] = recall_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
    metrics[ "f1s" ] = f1_score( y_labels, x_labels, average="weighted", zero_division=np.nan )

    classes = sorted( set( y_labels ) )
    y_binarized = label_binarize( y_labels, classes=classes )
    x_binarized = label_binarize( x_labels, classes=classes )
    metrics[ "roc" ] = roc_auc_score( y_binarized, x_binarized, average="weighted", multi_class="ovr" )
    metrics[ "avp" ] = average_precision_score( y_binarized, x_binarized, average="weighted" )
    return metrics

def get_result_df( source_groups, metas, true_labels, pred_labels, pred_scores ) -> pd.DataFrame:
    data = [ ]
    for idx, (gidx, sample_groups) in enumerate( source_groups.items() ):
        sample_id = -1
        for sample_group in sample_groups.values():
            if len( sample_group ) > 0:
                sample_id = sample_group[ 0 ]
                break
        if sample_id < 0:
            print( f"Could not find valid sample for gidx {gidx} ..." )
            continue
        meta = metas[ sample_id ]
        dataset = meta[ "record_dataset" ]
        record = meta[ "record_scope" ]
        true_type = true_labels[ idx ]
        pred_type = pred_labels[ idx ]
        pred_score = pred_scores[ idx ]
        data.append( [ gidx, dataset, record, true_type, pred_type, pred_score ] )
    return pd.DataFrame( data, columns=[ "gidx", "dataset", "record", "true_type", "pred_type", "pred_score" ] )

def compute_cm( true_labels: list[ str ], pred_labels: list[ str ], pred_scores: list[ float ],
                save_path=None, labels=None, include_na=True ):
    if labels is None:
        labels = [ dp.value for dp in cpg_const.DesignPatternType ]
    if include_na:
        labels = [ *labels, cpg_const.NO_DESIGN_PATTERN ]
    cm = confusion_matrix( true_labels, pred_labels, labels=labels )

    confidence_matrix = np.zeros_like( cm, dtype=float )
    count_matrix = np.zeros_like( cm, dtype=int )
    for true, pred, score in zip( true_labels, pred_labels, pred_scores ):
        i = labels.index( true )
        j = labels.index( pred )
        confidence_matrix[ i, j ] += score
        count_matrix[ i, j ] += 1
    average_confidence = np.divide( confidence_matrix, count_matrix,
                                    out=np.zeros_like( confidence_matrix ),
                                    where=count_matrix != 0 )

    fig, ax = plt.subplots( figsize=(6, 6) )
    disp = ConfusionMatrixDisplay( confusion_matrix=cm, display_labels=labels )
    disp.plot( cmap=plt.cm.Blues, ax=ax, colorbar=False )

    for i in range( len( labels ) ):
        for j in range( len( labels ) ):
            if cm[ i, j ] > 0:
                ax.text( j, i + 0.3, f"⌀={average_confidence[ i, j ]:.2f}",
                         ha='center', va='center',
                         color='black', fontsize=8 )

    plt.title( "Confusion Matrix", fontsize=16 )
    plt.xlabel( "Predicted Labels", fontsize=14 )
    plt.ylabel( "True Labels", fontsize=14 )
    plt.xticks( fontsize=10, rotation=90 )
    plt.yticks( fontsize=10 )

    if save_path is not None:
        plt.savefig( save_path, bbox_inches='tight' )


def main( args, version, source_dataset ):
    # setup
    model_name = model_utils.get_model_name( args, version )
    result_dir = os.path.join( args.result_dir, model_name )
    result_dir = io_utils.ensure_dir( result_dir )
    """
    pattern_types = [
        cpg_const.DesignPatternType.BUILDER.value,
        cpg_const.DesignPatternType.DECORATOR.value ]
    """
    pattern_types = None

    # initialize
    model = InferenceGNN( args )
    args.dataset = source_dataset
    dataset = DesignPatternDataset( args,
                                    max_pattern_examples=30,
                                    query_pattern=False,
                                    pattern_types=pattern_types )

    sources = dataset.get_sources()
    # sources = filter_sources( sources, dataset.get_source_patterns(), max_sources_per_pattern=20 )
    sources = normalize_sources( sources, max_distance=6 )

    patterns = dataset.get_patterns()
    patterns = get_common_patterns( patterns, max_node_distance=5 )

    dataset.set_sources( sources )
    dataset.set_patterns( patterns )
    dataset.compute_samples()

    # inference
    preds, metas, _, _ = inference( model, dataset, args,
                                    # sample_processor=sample_processor_subgraph_normalized,
                                    sample_processor=sample_processor_k_normalized,
                                    min_d_offset=1, max_d_offset=5 )

    # aggregate
    groups_by_source = group_by_source( metas )
    source_preds = compute_source_preds( groups_by_source, preds, metas,
                                         pred_aggregator=aggregate_preds_by_quantile,
                                         q=0.99 )
    source_types = compute_source_types( metas )
    true_labels, pred_labels, pred_scores = compute_labels( source_types, source_preds, conf=0.2 )

    # save results
    result_df = get_result_df( groups_by_source, metas, true_labels, pred_labels, pred_scores )
    result_df.to_csv( os.path.join( result_dir, "result_pattern_matching_sources.csv" ), index=False )

    # evaluate
    compute_cm( true_labels, pred_labels, pred_scores, labels=pattern_types, include_na=True,
                save_path=os.path.join( result_dir, "result_pattern_matching_cm.png" ) )
    x_labels, y_labels = to_numeric_labels( true_labels, pred_labels )
    metrics = compute_metrics( x_labels, y_labels )
    print( f"Roc=[{metrics[ 'roc' ]:.3}] Acc=[{metrics[ 'acc' ]:.3}] "
           f"Prec=[{metrics[ 'pre' ]:.3}] Rec=[{metrics[ 'rec' ]:.3}] F1=[{metrics[ 'f1s' ]:.3}] "
           f"AvgPrec=[{metrics[ 'avp' ]:.3}]" )

    result_rows = [ [ metrics[ 'roc' ], metrics[ 'avp' ], metrics[ 'pre' ],
                      metrics[ 'rec' ], metrics[ 'f1s' ], metrics[ 'acc' ] ] ]
    with open( os.path.join( result_dir, "result_pattern_matching.csv" ), "w", encoding="utf-8" ) as f:
        f.write( "ROC AUC,PR AUC,Precision,Recall,F1-Score,Accuracy\n" )
        for row in result_rows:
            f.write( ",".join( [ str( x ) for x in row ] ) )
            f.write( "\n" )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "dpdf"
    args.directed = False
    args.anchored = True
    version = model_utils.get_latest_model_version( args )
    model_name = model_utils.get_model_name( args, version )
    args = arg_utils.load_args( args, model_name )

    source_dataset = "pmart"
    args.pattern_dataset = "dpdf"
    args.normalized = True
    args.test_data = True
    args.batch_size = 128
    args.num_workers = 1
    print( args )

    main( args, version, source_dataset )
