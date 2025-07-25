import os
import pickle
import time

import numpy as np
import torch
import torch.nn as nn
import torchinfo
from sklearn.metrics import accuracy_score
from sklearn.metrics import roc_auc_score
from torch.utils.data import DataLoader
from torch.utils.tensorboard import SummaryWriter
from tqdm import tqdm

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.io_utils as io_utils
import matching.glema.common.utils.misc_utils as misc_utils
import matching.glema.common.utils.model_utils as model_utils
from matching.glema.common.dataset import BaseDataset
from matching.glema.common.encoding import collate_fn
from matching.glema.common.model import GLeMaNet
from matching.glema.evaluation.evaluate import main as evaluate
from matching.glema.evaluation.evaluate_matching import main as evaluate_matching


def train( args ):
    misc_utils.set_seed( args.seed )

    dataset_name = model_utils.get_dataset_name( args )
    data_path = io_utils.get_abs_file_path( os.path.join( args.data_processed_dir, dataset_name ) )
    print( f"Starting training model on dataset: {dataset_name}" )

    train_key_file = io_utils.get_abs_file_path( os.path.join( data_path, args.train_keys ) )
    test_key_file = io_utils.get_abs_file_path( os.path.join( data_path, args.test_keys ) )

    version = model_utils.get_latest_model_version( args )
    version = max( version, 0 ) + 1
    model_name = model_utils.get_model_name( args, version )
    save_dir = model_utils.get_model_ckpt_dir( args, model_name )
    save_dir = io_utils.ensure_dir( save_dir )
    log_dir = os.path.join( args.log_dir, dataset_name )
    log_dir = io_utils.ensure_dir( log_dir )

    # Read data. data is stored in format of dictionary. Each key has information about protein-ligand complex.
    with open( train_key_file, "rb" ) as fp:
        train_keys = pickle.load( fp )
    with open( test_key_file, "rb" ) as fp:
        test_keys = pickle.load( fp )

    # Print simple statistics about dude data and pdbbind data
    print( f"Number of train data: {len( train_keys )}" )
    print( f"Number of test data: {len( test_keys )}" )

    # Initialize model
    model = GLeMaNet( args )
    print(
        "Number of parameters: ",
        sum( p.numel() for p in model.parameters() if p.requires_grad ),
    )
    torchinfo.summary( model )
    device = model_utils.get_device()
    model = model_utils.initialize_model( model, device )

    # Train and test dataset
    uses_curriculum_training = args.curriculum_training_steps > 0
    train_complexity_start = -1
    train_complexity_keys = None
    if uses_curriculum_training:
        # setup curriculum training
        train_complexity_start = 1
        train_complexity_keys = model_utils.load_complexity_keys( args )

    max_train_data_size = -1 if args.max_train_data < 0 else args.max_train_data
    max_test_data_size = -1 if args.max_test_data < 0 else args.max_test_data

    train_dataset = BaseDataset( train_keys, args,
                                 k_start=train_complexity_start, k_keys=train_complexity_keys,
                                 max_size=max_train_data_size )
    test_dataset = BaseDataset( test_keys, args, max_size=max_test_data_size )

    train_dataloader = DataLoader(
        train_dataset,
        args.batch_size,
        shuffle=False,
        num_workers=args.num_workers,
        collate_fn=collate_fn,
        # sampler = train_sampler
    )

    test_dataloader = DataLoader(
        test_dataset,
        args.batch_size,
        shuffle=False,
        num_workers=args.num_workers,
        collate_fn=collate_fn,
    )

    # Optimizer
    optimizer = torch.optim.Adam( model.parameters(), lr=args.lr )

    # Loss function
    loss_fn = nn.BCELoss()

    timestamp = misc_utils.get_timestamp()

    # Logging file
    log_file = open( os.path.join( log_dir, f"log_{timestamp}.csv" ), "w", encoding="utf-8" )
    log_file.write(
        "epoch,train_losses,test_losses,train_acc,test_acc,train_roc,test_roc,train_time,test_time\n"
    )
    logger = SummaryWriter( log_dir=f'{log_dir}/{model_name}_{timestamp}' )
    arg_utils.save_args( args, model_name )

    best_roc = 0
    early_stop_count = 0

    batch_count = 0
    snapshot_interval = 20  # TODO maybe relative ???
    confidence_thresh = 0.75

    for epoch in range( args.epoch ):
        epoch_info = f"Starting epoch {epoch}"
        if uses_curriculum_training:
            train_k = train_dataset.get_complexity_limit()
            epoch_info += f" [k={train_k if train_k > 0 else 'unlimited'}]"
        print( epoch_info )
        st = time.time()
        # Collect losses of each iteration
        train_losses = [ ]
        test_losses = [ ]

        # Collect true label of each iteration
        train_true = [ ]
        test_true = [ ]

        # Collect predicted label of each iteration
        train_pred = [ ]
        test_pred = [ ]

        model.train()
        pbar = tqdm( train_dataloader )

        for sample in pbar:
            model.zero_grad()
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

            # Train neural network
            pred, attn_loss = model(
                X=(H, A1, A2, V), attn_masking=(M, S), training=True
            )

            loss = loss_fn( pred, Y ) + attn_loss
            loss.backward()
            optimizer.step()

            # Print loss at the end of tqdm bar
            pbar.set_postfix_str( "Loss: %.4f" % loss.data.cpu().item() )

            # Collect loss, true label and predicted label
            train_losses.append( loss.data.cpu().item() )
            train_true.append( Y.data.cpu().numpy() )
            train_pred.append( pred.data.cpu().numpy() )

            batch_count += 1
            if len( train_losses ) >= snapshot_interval and batch_count % snapshot_interval == 0:
                train_loss_snap = np.mean( np.array( train_losses[ -snapshot_interval: ] ) )
                logger.add_scalar( 'batch/loss', train_loss_snap, batch_count )

                train_true_snap = np.concatenate( train_true[ -snapshot_interval: ].copy(), 0 )
                train_pred_snap = np.concatenate( train_pred[ -snapshot_interval: ].copy(), 0 )
                train_pred_snap[ train_pred_snap < confidence_thresh ] = 0
                train_pred_snap[ train_pred_snap > 0 ] = 1
                train_acc_snap = accuracy_score( train_true_snap, train_pred_snap )
                logger.add_scalar( 'batch/acc', train_acc_snap, batch_count )

        pbar.close()
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
                pred, attn_loss = model(
                    X=(H, A1, A2, V), attn_masking=(M, S), training=True
                )

            loss = loss_fn( pred, Y ) + attn_loss

            # Collect loss, true label and predicted label
            test_losses.append( loss.data.cpu().item() )
            test_true.append( Y.data.cpu().numpy() )
            test_pred.append( pred.data.cpu().numpy() )

        end = time.time()

        train_losses = np.mean( np.array( train_losses ) )
        test_losses = np.mean( np.array( test_losses ) )

        train_pred = np.concatenate( train_pred, 0 )
        test_pred = np.concatenate( test_pred, 0 )

        train_true = np.concatenate( train_true, 0 )
        test_true = np.concatenate( test_true, 0 )

        train_roc = roc_auc_score( train_true, train_pred )
        test_roc = roc_auc_score( test_true, test_pred )

        test_pred_by_conf = test_pred.copy()
        test_pred_by_conf[ test_pred_by_conf < confidence_thresh ] = 0
        test_pred_by_conf[ test_pred_by_conf > 0 ] = 1
        test_acc = accuracy_score( test_true, test_pred_by_conf )

        train_pred_by_conf = train_pred.copy()
        train_pred_by_conf[ train_pred_by_conf < confidence_thresh ] = 0
        train_pred_by_conf[ train_pred_by_conf > 0 ] = 1
        train_acc = accuracy_score( train_true, train_pred_by_conf )

        time_train = st_eval - st
        time_test = end - st_eval
        time_total = time_train + time_test

        print( f"Epoch=[{epoch}] - "
               f"Train_Loss=[{train_losses:.3f}] - "
               f"Test_Loss=[{test_losses:.3f}] - "
               f"Train_Acc=[{train_acc:.3f}] - "
               f"Test_Acc=[{test_acc:.3f}] - "
               f"Train_Roc=[{train_roc:.3f}] - "
               f"Test_Roc=[{test_roc:.3f}] - "
               f"Train_Time=[{time_train:.3f}s] - "
               f"Test_Time=[{time_test:.3f}s] - "
               f"Total_Time=[{time_total:.3f}s]" )

        log_file.write(
            "%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n"
            % (
                epoch,
                train_losses,
                test_losses,
                train_acc,
                test_acc,
                train_roc,
                test_roc,
                time_train,
                time_test,
            )
        )
        log_file.flush()

        logger.add_scalar( 'train/loss', train_losses, epoch )
        logger.add_scalar( 'train/roc', train_roc, epoch )
        logger.add_scalar( 'train/acc', train_acc, epoch )
        logger.add_scalar( 'test/loss', test_losses, epoch )
        logger.add_scalar( 'test/roc', test_roc, epoch )
        logger.add_scalar( 'test/acc', test_acc, epoch )
        logger.add_scalar( 'time/train', time_train, epoch )
        logger.add_scalar( 'time/test', time_test, epoch )
        logger.add_scalar( 'time/total', time_total, epoch )

        is_pretrain = uses_curriculum_training and train_dataset.get_complexity_limit() > 0
        if test_roc > best_roc:
            early_stop_count = 0
            best_roc = test_roc
            ckpt_name = model_utils.get_model_ckpt( args, model_name )
            torch.save( model.state_dict(), ckpt_name )
        elif not is_pretrain:
            early_stop_count += 1
            print( f"Increases early stop counter to {early_stop_count}/{3}" )
            if early_stop_count >= 3:
                # Early stopping
                break

        if is_pretrain:
            if (epoch + 1) % args.curriculum_training_steps == 0:
                # increase complexity limit every x epochs
                train_dataset.increase_complexity()
                # if max complexity is reached, reset early stop values
                if train_dataset.get_complexity_limit() < 0:
                    early_stop_count = 0
                    best_roc = 0

    log_file.close()
    return version


def write_evaluation( args, version ):
    model_name = model_utils.get_model_name( args, version )
    args = arg_utils.load_args( args, model_name )
    evaluate( args, version )
    evaluate_matching( args, version )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "dpdf"
    args.directed = False
    args.anchored = True
    args.tactic = "jump"
    args.batch_size = 128
    args.max_test_data = 50_000
    args.max_train_data = args.max_test_data * 10
    args.curriculum_training_steps = 1  # graph complexity increase every x epochs
    args.nhead = 1
    args.embedding_dim = 6  # possible labels
    if args.anchored:
        args.embedding_dim += 1  # labels + 1 anchor embedding
    args.seed = 23
    print( args )

    version = train( args )
    write_evaluation( args, version )
