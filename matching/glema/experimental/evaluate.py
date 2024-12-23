import os
import pickle
import time

import numpy as np
import torch
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)
from torch.utils.data import DataLoader
from tqdm import tqdm

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.model_utils as model_utils
import matching.glema.experimental.utils as utils
from matching.glema.experimental.dataset import BaseDataset, collate_fn


def main( args, version ):
    dataset_name = model_utils.get_dataset_name( args )
    data_path = io_utils.get_abs_file_path( os.path.join( args.data_processed_dir, dataset_name ) )
    model_name = model_utils.get_model_name( args, version, tag=args.model_tag )
    result_dir = os.path.join( args.result_dir, model_name )
    result_dir = io_utils.ensure_dir( result_dir )
    result_file = "result.csv"
    args.train_keys = io_utils.get_abs_file_path( os.path.join( data_path, args.train_keys ) )
    args.test_keys = io_utils.get_abs_file_path( os.path.join( data_path, args.test_keys ) )

    with open( args.test_keys, "rb" ) as fp:
        test_keys = pickle.load( fp )

    print( f"Number of test data: {len( test_keys )}" )

    # Initialize model
    model = utils.build_model( args )
    print(
        "Number of parameters: ",
        sum( p.numel() for p in model.parameters() if p.requires_grad ),
    )
    # device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    device = model_utils.get_device()

    test_dataset = BaseDataset( test_keys, args )
    test_dataloader = DataLoader(
        test_dataset,
        args.batch_size,
        shuffle=False,
        num_workers=args.num_workers,
        collate_fn=collate_fn,
    )

    # Starting evaluation
    test_true = [ ]
    test_pred = [ ]

    model.eval()
    st_eval = time.time()

    for sample in tqdm( test_dataloader ):
        pos_target, pos_query, neg_target, neg_query = sample
        pos_target = pos_target.to( model_utils.get_device() )
        pos_query = pos_query.to( model_utils.get_device() )
        neg_target = neg_target.to( model_utils.get_device() )
        neg_query = neg_query.to( model_utils.get_device() )
        labels = torch.tensor(
            [ 1 ] * (pos_target.num_graphs if pos_target else 0) + [ 0 ] * neg_target.num_graphs ).to(
            model_utils.get_device() )

        # Test neural network
        with torch.no_grad():
            emb_neg_a, emb_neg_b = (model.emb_model( neg_target ), model.emb_model( neg_query ))

            if pos_target:
                emb_pos_a, emb_pos_b = (model.emb_model( pos_target ), model.emb_model( pos_query ))
                emb_as = torch.cat( (emb_pos_a, emb_neg_a), dim=0 )
                emb_bs = torch.cat( (emb_pos_b, emb_neg_b), dim=0 )
            else:
                emb_as, emb_bs = emb_neg_a, emb_neg_b

            pred = model( emb_as, emb_bs )
            raw_pred = model.predict( pred )
            pred = model.clf_model( raw_pred.unsqueeze( 1 ) ).argmax( dim=-1 )

        # Collect true label and predicted label
        test_true.append( labels.cpu().numpy() )
        test_pred.append( pred.cpu().numpy() )

    end = time.time()

    test_pred = np.concatenate( test_pred, 0 )
    test_true = np.concatenate( test_true, 0 )
    result_rows = [ ]

    for conf_step in [
        0.5,
    ]:
        test_pred_by_conf = test_pred.copy()
        test_pred_by_conf[ test_pred_by_conf < conf_step ] = 0
        test_pred_by_conf[ test_pred_by_conf > 0 ] = 1

        test_roc = roc_auc_score( test_true, test_pred_by_conf )
        test_acc = accuracy_score( test_true, test_pred_by_conf )
        test_pre = precision_score( test_true, test_pred_by_conf, zero_division=np.nan )
        test_rec = recall_score( test_true, test_pred_by_conf, zero_division=np.nan )
        test_f1s = f1_score( test_true, test_pred_by_conf, zero_division=np.nan )
        test_prc = average_precision_score( test_true, test_pred_by_conf )
        test_time = (end - st_eval) / len( test_dataset )

        result_rows.append(
            [
                conf_step,
                test_time,
                test_roc,
                test_prc,
                test_pre,
                test_rec,
                test_f1s,
                test_acc,
            ]
        )

        print( f"Conf=[{conf_step}] Roc=[{test_roc:.3}] Acc=[{test_acc:.3}]"
               f" Prec=[{test_pre:.3}] Rec=[{test_rec:.3}] F1=[{test_f1s:.3}]"
               f" AvgPrec=[{test_prc:.3}]" )

    with open( os.path.join( result_dir, result_file ), "w", encoding="utf-8" ) as f:
        f.write(
            "Confident,Execution Time,ROC AUC,PR AUC,Precision,Recall,F1-Score,Accuracy\n"
        )
        for row in result_rows:
            f.write( ",".join( [ str( x ) for x in row ] ) )
            f.write( "\n" )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "CPG_augm"
    args.model_tag = "experimental_SAGE"
    args.directed = False
    args.anchored = True
    version = model_utils.get_latest_model_version( args, tag=args.model_tag )
    model_name = model_utils.get_model_name( args, version, tag=args.model_tag )
    args = arg_utils.load_args( args, model_name )

    args.num_workers = 0
    print( args )

    main( args, version )
