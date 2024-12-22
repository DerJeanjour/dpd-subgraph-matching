import torch
from deepsnap.batch import Batch
from deepsnap.graph import Graph as DSGraph

import matching.glema.common.utils.model_utils as model_utils
import matching.glema.experimental.feature_preprocess as feature_preprocess


def batch_nx_graphs( graphs, anchors=None ):
    augmenter = feature_preprocess.FeatureAugment()
    if anchors is not None:
        for anchor, g in zip( anchors, graphs ):
            for v in g.nodes:
                g.nodes[ v ][ "node_feature" ] = torch.tensor( [ float( v == anchor ) ] )

    batch = Batch.from_data_list( [ DSGraph( g ) for g in graphs ] )
    batch = augmenter.augment( batch )
    batch = batch.to( model_utils.get_device() )
    return batch
