import itertools
import os
import pickle
from collections.abc import Callable

import matplotlib.pyplot as plt
import networkx as nx
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
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
        common_patterns_temp = common_patterns
        common_patterns = [ ]
        for (G1, G2) in pairs:
            common_pattern = graph_utils.get_norm_graph_intersection( G1, G2 )
            # filter duplicates
            if any( graph_utils.norm_graphs_are_equal( common_pattern, G ) for G in common_patterns ):
                continue
            common_patterns.append( common_pattern )
        # filter by node size
        common_patterns = [ G for G in common_patterns if G.number_of_nodes() >= min_nodes ]
        if len( common_patterns ) == 0:
            common_patterns = common_patterns_temp
            break
        print( f"Size after iter {i}: {len( common_patterns )}" )
        i += 1
    common_patterns = [ G for G in common_patterns if G.number_of_nodes() >= min_nodes ]
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


def normalize_patterns( patterns_all: dict[ str, list[ nx.Graph ] ], max_distance=8, min_nodes=-1 ) -> dict[
    str, list[ nx.Graph ] ]:
    print( "Normalizing patterns ..." )
    patterns_all_norm = { }
    for dp_type, patterns in tqdm( patterns_all.items() ):
        norm_patterns = [ graph_utils.normalize_graph( dp, max_distance=max_distance )[ 0 ] for dp in patterns ]
        if min_nodes > 0:
            norm_patterns = [ pattern for pattern in norm_patterns if pattern.number_of_nodes() >= min_nodes ]
        patterns_all_norm[ dp_type ] = norm_patterns
    return patterns_all_norm


def normalize_sources( sources: dict[ int, nx.Graph ], max_distance=8, min_nodes=-1 ) -> dict[ int, nx.Graph ]:
    print( "Normalizing sources ..." )
    norm_sources = { }
    for gidx, source in tqdm( sources.items() ):
        norm_source, _ = graph_utils.normalize_graph( source, max_distance=max_distance )
        if min_nodes > 0 and norm_source.number_of_nodes() < min_nodes:
            continue
        norm_sources[ gidx ] = norm_source
    return norm_sources


def filter_sources( sources: dict[ int, nx.Graph ], source_patterns: dict[ int, str ],
                    max_sources_per_pattern=-1, max_na_patterns=-1 ):
    print( "Filtering sources ..." )
    pattern_idxs = { }
    for gidx, source in tqdm( sources.items() ):
        pattern = source_patterns[ gidx ]
        if pattern not in pattern_idxs:
            pattern_idxs[ pattern ] = [ ]
        pattern_idxs[ pattern ].append( gidx )

    if max_sources_per_pattern > 0:
        pattern_idxs = { p: idxs[ :max_sources_per_pattern ] for p, idxs in pattern_idxs.items() }

    if max_na_patterns > 0 and cpg_const.NO_DESIGN_PATTERN in pattern_idxs:
        pattern_idxs[ cpg_const.NO_DESIGN_PATTERN ] = pattern_idxs[ cpg_const.NO_DESIGN_PATTERN ][ :max_na_patterns ]

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


def sample_processor_path_match_weighted( source: nx.Graph, query: nx.Graph, meta: dict ) -> tuple[ list, list, list ]:
    source_paths = set( [ path[ 1 ] for path in graph_utils.get_all_norm_paths( source ) ] )
    query_paths = set( [ path[ 1 ] for path in graph_utils.get_all_norm_paths( query ) ] )
    max_match = 0
    for sp in source_paths:
        for qp in query_paths:
            for k in list( range( 1, min( len( sp ), len( qp ) ) + 1 ) ):
                qpk = qp[ :k ]
                if sp.startswith( qpk ) and len( qpk ) > max_match:
                    max_match = len( qpk )

    pred_w = max_match / max( [ len( p ) for p in source_paths ] )
    _meta = meta.copy()
    _meta[ "pred_w" ] = pred_w
    return sample_processor_default( source, query, _meta )


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
    return [ source ], [ query ], [ _meta ]


def inference( model: InferenceGNN, dataset: DesignPatternDataset, args,
               collect_graphs=False, conf=0.5,
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
        query_preds, _ = model.predict_batch( sources, queries, conf=conf, bulk_size=args.batch_size )
        preds.extend( query_preds )
    preds = [ float( p ) for p in preds ]
    return preds, metas, all_source, all_queries


def print_pattern_counts( dataset: DesignPatternDataset ):
    # source patterns
    source_pattern_counts = { }
    pattern_instance: dict[ str, set ] = { }
    for gidx, pattern in dataset.get_source_patterns().items():
        if gidx not in dataset.get_sources():
            continue
        if pattern not in source_pattern_counts:
            source_pattern_counts[ pattern ] = 0
        if pattern not in pattern_instance:
            pattern_instance[ pattern ] = set()
        source_pattern_counts[ pattern ] += 1

        record_scope = dataset.get_source_graph_record_scopes()[ gidx ]
        pattern_id = graph_utils.decode_pattern_id( record_scope )
        if pattern_id is None:
            pattern_id = gidx
        pattern_instance[ pattern ].add( pattern_id )

    print( "source_pattern_counts:", misc_utils.sort_dict_by_key( source_pattern_counts ) )
    pattern_instance_counts = { pattern: len( instances ) for pattern, instances in pattern_instance.items() }
    print( "pattern_instance_counts:", misc_utils.sort_dict_by_key( pattern_instance_counts ) )
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


def group_by_pattern_instance( metas ) -> dict[ int, dict[ str, list[ int ] ] ]:
    groups: dict[ int, dict[ str, list[ int ] ] ] = { }
    for idx, meta in enumerate( metas ):
        pattern_id = meta[ "pattern_id" ]
        pattern_type = meta[ "pattern_type" ]
        if pattern_id not in groups:
            groups[ pattern_id ] = { }
        if pattern_type not in groups[ pattern_id ]:
            groups[ pattern_id ][ pattern_type ] = [ ]
        groups[ pattern_id ][ pattern_type ].append( idx )
    return groups


def aggregate_preds_mean( preds: list[ float ] ) -> float:
    return float( np.mean( preds ) )


def aggregate_preds_max( preds: list[ float ] ) -> float:
    return float( np.max( preds ) )


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


def compute_source_types( groups_by_source: dict[ int, dict[ str, list[ float ] ] ],
                          metas: list[ dict ] ) -> dict[ int, str ]:
    source_types = { }
    for gidx, source_preds_idxs in groups_by_source.items():
        for dp, idxs in source_preds_idxs.items():
            for idx in idxs:
                source_types[ gidx ] = metas[ idx ][ "source_type" ]
                break
    return source_types


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


def compute_labels_by_instance( source_types: dict[ int, str ],
                                source_preds: dict[ int, dict[ str, float ] ],
                                groups_by_instance: dict[ int, dict[ str, list[ int ] ] ],
                                metas, conf=0.5 ) -> tuple[ list[ str ], list[ str ], list[ float ] ]:
    true_labels, pred_labels, pred_scores = [ ], [ ], [ ]
    true_labels_source, pred_labels_source, pred_scores_source = compute_labels_legacy( source_types,
                                                                                        source_preds,
                                                                                        conf=conf )

    source_to_instance_mapping = get_source_to_pattern_instance_mapping( metas )
    instance_pattern_true_labels: dict[ int, str ] = { }
    instance_pattern_pred_labels: dict[ int, list[ tuple[ str, float ] ] ] = { }
    for idx, (gidx, _) in enumerate( source_types.items() ):

        pattern_id = source_to_instance_mapping[ gidx ]
        true_label_source = true_labels_source[ idx ]
        pred_label_source = pred_labels_source[ idx ]
        pred_score_source = pred_scores_source[ idx ]

        instance_pattern_true_labels[ pattern_id ] = true_label_source
        if pattern_id not in instance_pattern_pred_labels:
            instance_pattern_pred_labels[ pattern_id ] = [ ]
        instance_pattern_pred_labels[ pattern_id ].append( (pred_label_source, pred_score_source) )

    for pattern_id in list( groups_by_instance.keys() ):
        true_label = instance_pattern_true_labels[ pattern_id ]
        true_labels.append( true_label )

        preds_by_label: [ str, list[ float ] ] = { }
        for (pred_label_source, pred_score_source) in instance_pattern_pred_labels[ pattern_id ]:
            if pred_label_source not in preds_by_label:
                preds_by_label[ pred_label_source ] = [ ]
            preds_by_label[ pred_label_source ].append( pred_score_source )

        labels_pred: dict[ str, float ] = { label: max( preds ) for label, preds in preds_by_label.items() }
        labels_count: dict[ str, int ] = { label: len( preds ) for label, preds in preds_by_label.items() }
        if true_label in labels_pred:
            pred_labels.append( true_label )
            pred_scores.append( labels_pred[ true_label ] )
            continue

        best_label = cpg_const.NO_DESIGN_PATTERN
        best_score = 0
        for label, score in labels_pred.items():
            # for label, score in labels_count.items():
            if score > best_score:
                best_label = label
                best_score = labels_pred[ label ]
        pred_labels.append( best_label )
        pred_scores.append( best_score )

    return true_labels, pred_labels, pred_scores


def get_source_to_pattern_instance_mapping( metas ) -> dict[ int, int ]:
    mapping: dict[ int, int ] = { }
    for meta in metas:
        mapping[ meta[ 'gidx' ] ] = meta[ 'pattern_id' ]
    return mapping


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
    classes = sorted( set( [ *y_labels, *x_labels ] ) )
    if len( classes ) > 2:
        y_binarized = label_binarize( y_labels, classes=classes )
        x_binarized = label_binarize( x_labels, classes=classes )
        metrics[ "acc" ] = balanced_accuracy_score( y_labels, x_labels )
        metrics[ "pre" ] = precision_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
        metrics[ "rec" ] = recall_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
        metrics[ "f1s" ] = f1_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
        metrics[ "roc" ] = roc_auc_score( y_binarized, x_binarized, average="weighted", multi_class="ovr" )
        metrics[ "avp" ] = average_precision_score( y_binarized, x_binarized, average="weighted" )
        return metrics

    label_mapping = { val: idx for idx, val in enumerate( sorted( set( [ *y_labels, *x_labels ] ) ) ) }
    x_labels_norm = [ label_mapping[ val ] for val in x_labels ]
    y_labels_norm = [ label_mapping[ val ] for val in y_labels ]

    metrics[ "acc" ] = accuracy_score( y_labels_norm, x_labels_norm )
    metrics[ "pre" ] = precision_score( y_labels_norm, x_labels_norm, zero_division=np.nan )
    metrics[ "rec" ] = recall_score( y_labels_norm, x_labels_norm, zero_division=np.nan )
    metrics[ "f1s" ] = f1_score( y_labels_norm, x_labels_norm, zero_division=np.nan )
    metrics[ "roc" ] = roc_auc_score( y_labels_norm, x_labels_norm )
    metrics[ "avp" ] = average_precision_score( y_labels_norm, x_labels_norm )
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


def get_matching_examples( pattern_types: list[ str ],
                           model: InferenceGNN,
                           sources: list[ nx.Graph ],
                           queries: list[ nx.Graph ],
                           preds: list[ float ],
                           metas: list[ dict ],
                           max_nodes=-1, min_nodes=-1,
                           save_path=None ) -> dict[ tuple[ str, str ], dict[ str, any ] ]:
    matching_examples: dict[ tuple[ str, str ], dict[ str, any ] ] = { }
    for pattern_type in pattern_types:
        pattern_sources = [ ]
        pattern_queries = [ ]
        pattern_preds = [ ]
        pattern_idxs = [ ]
        for idx, meta in enumerate( metas ):
            if meta[ "source_type" ] == pattern_type and meta[ "pattern_type" ] == pattern_type:

                if max_nodes > 0:
                    if sources[ idx ].number_of_nodes() > max_nodes or queries[ idx ].number_of_nodes() > max_nodes:
                        continue

                if min_nodes > 0:
                    if sources[ idx ].number_of_nodes() <= min_nodes or queries[
                        idx ].number_of_nodes() <= min_nodes:
                        continue

                pattern_sources.append( sources[ idx ] )
                pattern_queries.append( queries[ idx ] )
                pattern_preds.append( preds[ idx ] )
                pattern_idxs.append( idx )
        if len( pattern_sources ) == 0:
            continue
        max_idx = int( np.argsort( pattern_preds )[ -1: ][ 0 ] )
        matching_examples[ (pattern_type, pattern_type) ] = {
            "source": pattern_sources[ max_idx ],
            "query": pattern_queries[ max_idx ],
            "pred": pattern_preds[ max_idx ],
            "idx": pattern_idxs[ max_idx ]
        }

    for source_type in pattern_types:
        for query_type in pattern_types:
            if source_type == query_type:
                continue
            source_idx = matching_examples[ (source_type, source_type) ][ "idx" ]
            query_idx = matching_examples[ (query_type, query_type) ][ "idx" ]
            type_source = sources[ source_idx ]
            type_query = queries[ query_idx ]
            type_pred, _ = model.predict( type_source, type_query )
            matching_examples[ (source_type, query_type) ] = {
                "source": type_source,
                "query": type_query,
                "pred": type_pred
            }

    if save_path is not None:
        with open( save_path, 'wb' ) as handle:
            pickle.dump( matching_examples, handle, protocol=pickle.HIGHEST_PROTOCOL )
    return matching_examples


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
    sources = filter_sources( sources, dataset.get_source_patterns(), max_sources_per_pattern=-1, max_na_patterns=5 )
    sources = normalize_sources( sources, max_distance=max_norm_d, min_nodes=min_nodes )

    patterns = dataset.get_patterns()
    patterns = normalize_patterns( patterns, max_distance=max_norm_d, min_nodes=min_nodes )
    # patterns = get_common_patterns( patterns, max_node_distance=max_norm_d )

    dataset.set_sources( sources )
    dataset.set_patterns( patterns )
    dataset.compute_samples()

    # inference
    preds, metas, sources, queries = inference( model, dataset, args,
                                                # sample_processor=sample_processor_subgraph_normalized,
                                                # sample_processor=sample_processor_k_normalized,
                                                # min_d_offset=1, max_d_offset=5,
                                                collect_graphs=True )

    # aggregate
    by_source = False
    by_grouped_instance = False
    groups_by_source = group_by_source( metas )
    groups_by_instance = group_by_pattern_instance( metas )
    if by_grouped_instance:
        groups_by_source = groups_by_instance

    # evaluate
    # target_conf = 0.6
    # conf_steps = [ i / 10 for i in range( 1, 10 ) ]
    target_conf = 0.999
    conf_steps = [
        0.1,
        0.2,
        0.3,
        0.4,
        0.5,
        0.6,
        0.7,
        0.8,
        0.9,
        0.91,
        0.92,
        0.93,
        0.94,
        0.95,
        0.96,
        0.97,
        0.98,
        0.99,
        0.999,
    ]
    conf_metrics: list[ dict[ str, float ] ] = [ ]
    for conf_step in conf_steps:

        source_preds = compute_source_preds( groups_by_source, preds, metas,
                                             pred_aggregator=aggregate_preds_by_quantile,
                                             q=conf_step )
        source_types = compute_source_types( groups_by_source, metas )

        if by_source or by_grouped_instance:
            # true_labels, pred_labels, pred_scores = compute_labels( source_types, source_preds, conf=conf_step )
            true_labels, pred_labels, pred_scores = compute_labels_legacy( source_types, source_preds, conf=0.8 )
            groups = groups_by_source
        else:
            true_labels, pred_labels, pred_scores = compute_labels_by_instance( source_types, source_preds,
                                                                                groups_by_instance, metas,
                                                                                conf=0.8 )
            groups = groups_by_instance

        if conf_step == target_conf:
            # save results
            result_df = get_result_df( groups, metas, true_labels, pred_labels, pred_scores )
            result_df.to_csv( os.path.join( result_dir, "result_pattern_matching_sources.csv" ), index=False )
            # evaluate
            compute_cm( true_labels, pred_labels, pred_scores, labels=pattern_types, include_na=True,
                        save_path=os.path.join( result_dir, "result_pattern_matching_cm.png" ) )
        # compute metrics
        x_labels, y_labels = to_numeric_labels( true_labels, pred_labels )
        metrics = compute_metrics( x_labels, y_labels )
        metrics[ "conf" ] = conf_step
        conf_metrics.append( metrics )
        print(
            f"Conf=[{metrics[ 'conf' ]:.1}] Roc=[{metrics[ 'roc' ]:.3}] Acc=[{metrics[ 'acc' ]:.3}] PR=[{metrics[ 'avp' ]:.3}] "
            f"Acc=[{metrics[ 'acc' ]:.3}] Prec=[{metrics[ 'pre' ]:.3}] Rec=[{metrics[ 'rec' ]:.3}] F1=[{metrics[ 'f1s' ]:.3}] " )

    result_rows = [ [ metrics[ 'conf' ], metrics[ 'roc' ], metrics[ 'avp' ], metrics[ 'pre' ],
                      metrics[ 'rec' ], metrics[ 'f1s' ], metrics[ 'acc' ] ] for metrics in conf_metrics ]
    with open( os.path.join( result_dir, "result_pattern_matching.csv" ), "w", encoding="utf-8" ) as f:
        f.write( "Confident,ROC AUC,PR AUC,Precision,Recall,F1-Score,Accuracy\n" )
        for row in result_rows:
            f.write( ",".join( [ str( x ) for x in row ] ) )
            f.write( "\n" )
    get_matching_examples( pattern_types, model, sources, queries,
                           preds, metas, min_nodes=10, max_nodes=30,
                           save_path=os.path.join( result_dir, "matching_examples.pkl" ) )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "CPG_augm_large"
    # args.dataset = "CPG"
    args.directed = True
    args.anchored = True
    version = model_utils.get_latest_model_version( args )
    model_name = model_utils.get_model_name( args, version )
    args = arg_utils.load_args( args, model_name )

    source_dataset = "pmart_all"
    args.pattern_dataset = "dpdf"
    args.normalized = True
    args.test_data = True
    args.batch_size = 128
    args.num_workers = 1
    print( args )

    main( args, version, source_dataset )
