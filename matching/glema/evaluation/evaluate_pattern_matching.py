import os

import networkx as nx
import numpy as np
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
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


def normalize_patterns( dataset: DesignPatternDataset,
                        num_graphs=1, n_min=16, q=0.95, q_step=0.05 ) -> dict[ str, list[ nx.Graph ] ]:
    def filter_by_node_presence( G: nx.Graph, q=0.8 ):
        node_presence = [ data[ "presence" ] for n, data in G.nodes( data=True ) ]
        presence_thresh = np.quantile( node_presence, q )
        n_keep = [ n for n, data in G.nodes( data=True ) if data[ "presence" ] >= presence_thresh ]
        return G.subgraph( n_keep )

    patterns_all = dataset.get_patterns()
    norm_pattern_graphs = { }
    for pattern_type in patterns_all.keys():
        patterns = patterns_all[ pattern_type ]
        # TODO maybe this is not good ?
        # patterns = [ graph_utils.normalize_graph( dp, max_distance=8 )[ 0 ] for dp in patterns ]
        connected = graph_utils.connect_graphs_at_anchor( patterns, keep_radius=10 )
        normalized, _ = graph_utils.normalize_graph( connected, max_distance=8 )
        norm_pattern_graphs[ pattern_type ] = [ normalized ]

    for pattern_type in norm_pattern_graphs.keys():
        normalized = norm_pattern_graphs[ pattern_type ][ 0 ]
        """
        node_presence = [ data[ "presence" ] for n, data in normalized.nodes( data=True ) ]
        node_sizes = misc_utils.map_num_array_to_range( node_presence, r_min=5, r_max=300 )
        plot_utils.plot_graph( normalized, title=f"{pattern_type} normalized",
                               nodeLabels=graph_utils.get_node_labels( normalized ),
                               nodeColors=graph_utils.get_node_colors( normalized ),
                               with_label=False, node_sizes=node_sizes )
        """

        norm_pattern_graphs[ pattern_type ] = [ ]
        while len( norm_pattern_graphs[ pattern_type ] ) < num_graphs:
            if q < 0.5:
                break
            filtered = filter_by_node_presence( normalized, q=q )
            if filtered.number_of_nodes() > n_min:
                norm_pattern_graphs[ pattern_type ].append( filtered )
                """
                plot_utils.plot_graph( filtered, title=f"{pattern_type} filtered (q={q:.2})",
                                       nodeLabels=graph_utils.get_node_labels( filtered ),
                                       nodeColors=graph_utils.get_node_colors( filtered ) )
                """
            q -= q_step

    dataset.set_patterns( norm_pattern_graphs )
    return norm_pattern_graphs


def normalize_sources( dataset: DesignPatternDataset, max_distance=8 ) -> dict[ int, nx.Graph ]:
    sources = dataset.get_sources()
    norm_sources = { }
    for gidx, source in sources.items():
        norm_source, _ = graph_utils.normalize_graph( source, max_distance=max_distance )
        norm_sources[ gidx ] = norm_source
    dataset.set_sources( norm_sources )
    return norm_sources

def filter_sources( dataset: DesignPatternDataset, max_sources_per_pattern=-1 ):
    sources = dataset.get_sources()

    pattern_idxs = { }
    source_patterns = dataset.get_source_patterns()
    for gidx, source in sources.items():
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

    dataset.set_sources( filtered_sources )


def main( args, version ):
    # setup
    dataset_name = model_utils.get_dataset_name( args )
    model_name = model_utils.get_model_name( args, version )
    result_dir = os.path.join( args.result_dir, model_name )
    result_dir = io_utils.ensure_dir( result_dir )
    result_file = "result_pattern_matching.csv"

    # initialize
    model = InferenceGNN( args )
    dataset = DesignPatternDataset( args, max_pattern_examples=10, query_pattern=False )
    filter_sources( dataset, max_sources_per_pattern=20 )
    normalize_sources( dataset, max_distance=8 )
    normalize_patterns( dataset, num_graphs=1, n_min=16 )
    dataset.compute_samples()

    # inference
    keys, preds, sources, queries = [ ], [ ], [ ], [ ]
    for idx in tqdm( list( range( 0, dataset.__len__() ) ) ):
        if len( sources ) < args.batch_size and idx < dataset.__len__() - 1:
            source, query, key = dataset.get_data( idx )
            sources.append( source )
            queries.append( query )
            keys.append( key )
            continue
        query_preds, _ = model.predict_batch( sources, queries )
        preds.extend( query_preds )
        sources, queries = [ ], [ ]

    # aggregate
    true_labels = { }
    pattern_label_preds = { }
    for idx, key in enumerate( keys ):
        source_type, query_type, gidx, record_scope = key
        true_labels[ gidx ] = source_type
        if gidx not in pattern_label_preds:
            pattern_label_preds[ gidx ] = { }
        if query_type not in pattern_label_preds[ gidx ]:
            pattern_label_preds[ gidx ][ query_type ] = [ ]
        pattern_label_preds[ gidx ][ query_type ].append( float( preds[ idx ] ) )

    conf = 0.7
    pred_labels = { }
    for gidx, source_preds in pattern_label_preds.items():
        max_pred = 0.0
        max_pred_label = cpg_const.NO_DESIGN_PATTERN
        for dp, pattern_preds in source_preds.items():
            pattern_pred = float( np.quantile( pattern_preds, 0.8 ) )
            pattern_label_preds[ gidx ][ dp ] = pattern_pred
            if pattern_pred > max_pred and pattern_pred >= conf:
                max_pred = pattern_pred
                max_pred_label = dp
        pred_labels[ gidx ] = (max_pred_label, max_pred)

    # print( true_labels )
    # print( pred_labels )

    # evaluate
    def enum_to_numeric( value ):
        if value == cpg_const.NO_DESIGN_PATTERN:
            return 0
        enum = misc_utils.get_enum_by_value( cpg_const.DesignPatternType, value )
        return misc_utils.get_enum_idx( enum )

    x_labels = [ ]
    y_labels = [ ]
    for gidx in true_labels.keys():
        x_labels.append( enum_to_numeric( pred_labels[ gidx ][ 0 ] ) )
        y_labels.append( enum_to_numeric( true_labels[ gidx ] ) )

    cm_acc = accuracy_score( y_labels, x_labels )
    cm_pre = precision_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
    cm_rec = recall_score( y_labels, x_labels, average="weighted", zero_division=np.nan )
    cm_f1s = f1_score( y_labels, x_labels, average="weighted", zero_division=np.nan )

    classes = sorted( set( y_labels ) )
    y_binarized = label_binarize( y_labels, classes=classes )
    x_binarized = label_binarize( x_labels, classes=classes )
    cm_roc = roc_auc_score( y_binarized, x_binarized, average="weighted", multi_class="ovr" )
    cm_prc = average_precision_score( y_binarized, x_binarized, average="weighted" )

    print( f"Roc=[{cm_roc:.3}] Acc=[{cm_acc:.3}]"
           f" Prec=[{cm_pre:.3}] Rec=[{cm_rec:.3}] F1=[{cm_f1s:.3}]"
           f" AvgPrec=[{cm_prc:.3}]" )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "CPG_augm_large"
    args.directed = False
    args.anchored = True
    version = model_utils.get_latest_model_version( args )
    model_name = model_utils.get_model_name( args, version )
    args = arg_utils.load_args( args, model_name )

    args.pattern_dataset = "CPG_all"
    args.normalized = True
    args.test_data = True
    args.batch_size = 128
    args.num_workers = 1
    print( args )

    main( args, version )
