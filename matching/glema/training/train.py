import os
import pickle
import random
import time

import numpy as np
import torch
import torch.nn as nn
import matching.glema.common.utils as utils
from matching.glema.common.dataset import BaseDataset, collate_fn, UnderSampler
from matching.glema.common.model import GLeMaNet
from sklearn.metrics import roc_auc_score
from sklearn.metrics import accuracy_score
from torch.utils.data import DataLoader
from tqdm import tqdm
import torchinfo
from torch.utils.tensorboard import SummaryWriter


def main( args ):
    data_path = utils.get_abs_file_path( os.path.join( args.data_path, args.dataset ) )
    if args.directed:
        data_path += "_directed"
    args.train_keys = utils.get_abs_file_path( os.path.join( data_path, args.train_keys ) )
    args.test_keys = utils.get_abs_file_path( os.path.join( data_path, args.test_keys ) )
    save_dir = utils.ensure_dir( args.save_dir, args )
    log_dir = utils.ensure_dir( args.log_dir, args )

    # Read data. data is stored in format of dictionary. Each key has information about protein-ligand complex.
    with open( args.train_keys, "rb" ) as fp:
        train_keys = pickle.load( fp )
    with open( args.test_keys, "rb" ) as fp:
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
    # device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    device = utils.get_device()
    model = utils.initialize_model( model, device, load_save_file=args.ckpt )

    # Train and test dataset
    train_dataset = BaseDataset( train_keys, data_path, embedding_dim=args.embedding_dim )
    test_dataset = BaseDataset( test_keys, data_path, embedding_dim=args.embedding_dim )

    # num_train_iso = len([0 for k in train_keys if 'iso' in k])
    # num_train_non = len([0 for k in train_keys if 'non' in k])
    # train_weights = [1/num_train_iso if 'iso' in k else 1/num_train_non for k in train_keys]
    # train_sampler = UnderSampler(train_weights, len(train_weights), replacement=True)

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

    timestamp = utils.get_timestamp()

    # Logging file
    log_file = open( os.path.join( log_dir, f"log_{timestamp}.csv" ), "w", encoding="utf-8" )
    log_file.write(
        "epoch,train_losses,test_losses,train_acc,test_acc,train_roc,test_roc,train_time,test_time\n"
    )
    logger = SummaryWriter( log_dir=f'{log_dir}/{args.dataset}_{args.tactic}_{timestamp}' )
    utils.save_args( args, f"{log_dir}/args_{timestamp}.json" )

    best_roc = 0
    early_stop_count = 0

    batch_count = 0
    snapshot_interval = 100
    confidence_thresh = 0.75
    for epoch in range( args.epoch ):
        print( "EPOCH", epoch )
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

        if test_roc > best_roc:
            early_stop_count = 0
            best_roc = test_roc
            ckpt_name = save_dir + "/best_model.pt"
            torch.save( model.state_dict(), ckpt_name )
            utils.save_args( args, ckpt_name )
        else:
            early_stop_count += 1
            if early_stop_count >= 3:
                # Early stopping
                break

    log_file.close()


if __name__ == "__main__":
    args = utils.parse_args()
    args.directed = True
    #args.dataset = "KKI"
    args.dataset = "SYNTHETIC_TINY"
    args.batch_size = 128
    args.tactic = "jump"
    args.embedding_dim = 4
    args.seed = 23
    print( args )

    utils.set_seed( args.seed )
    main( args )
