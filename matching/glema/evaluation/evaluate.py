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
from matching.glema.common.dataset import BaseDataset
from matching.glema.common.model import GLeMaNet
from matching.glema.common.encoding import collate_fn


def main( args, version ):
    dataset_name = model_utils.get_dataset_name( args)
    data_path = io_utils.get_abs_file_path( os.path.join( args.data_processed_dir, dataset_name ) )
    model_name = model_utils.get_model_name( args, version )
    result_dir = os.path.join( args.result_dir, model_name )
    result_dir = io_utils.ensure_dir( result_dir )
    result_file = "result.csv"
    args.train_keys = io_utils.get_abs_file_path( os.path.join( data_path, args.train_keys ) )
    args.test_keys = io_utils.get_abs_file_path( os.path.join( data_path, args.test_keys ) )

    with open( args.test_keys, "rb" ) as fp:
        test_keys = pickle.load( fp )

    print( f"Number of test data: {len( test_keys )}" )

    # Initialize model
    model = GLeMaNet( args )
    print(
        "Number of parameters: ",
        sum( p.numel() for p in model.parameters() if p.requires_grad ),
    )
    # device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    device = model_utils.get_device()
    model = model_utils.initialize_model( model, device, load_save_file=args.ckpt_path )

    test_dataset = BaseDataset( test_keys, args, balanced=False )
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
        H, A1, A2, M, S, Y, V, _ = sample
        H, A1, A2, M, S, Y, V = (
            H.to( device ),
            A1.to( device ),
            A2.to( device ),
            M.to( device ),
            S.to( device ),
            Y.to( device ),
            V.to( device ),
        )

        # Test neural network
        with torch.no_grad():
            pred = model( (H, A1, A2, V) )

        # Collect true label and predicted label
        test_true.append( Y.data.cpu().numpy() )
        test_pred.append( pred.data.cpu().numpy() )

    end = time.time()

    test_pred = np.concatenate( test_pred, 0 )
    test_true = np.concatenate( test_true, 0 )
    result_rows = [ ]

    for conf_step in [
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
    args.dataset = "dpdf"
    args.directed = False
    args.anchored = True
    version = model_utils.get_latest_model_version( args )
    model_name = model_utils.get_model_name( args, version )
    args = arg_utils.load_args( args, model_name )

    args.batch_size = 128
    print( args )

    main( args, version )
