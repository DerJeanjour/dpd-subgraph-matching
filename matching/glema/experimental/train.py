import os
import pickle
import time

import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
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
import matching.glema.experimental.utils as utils
from matching.glema.experimental.dataset import BaseDataset, collate_fn
from matching.glema.experimental.evaluate import main as evaluate


def train( args ):
    misc_utils.set_seed( args.seed )

    dataset_name = model_utils.get_dataset_name( args )
    data_path = io_utils.get_abs_file_path( os.path.join( args.data_processed_dir, dataset_name ) )
    print( f"Starting training model on dataset: {dataset_name}" )

    train_key_file = io_utils.get_abs_file_path( os.path.join( data_path, args.train_keys ) )
    test_key_file = io_utils.get_abs_file_path( os.path.join( data_path, args.test_keys ) )

    version = model_utils.get_latest_model_version( args, tag=args.model_tag )
    version = max( version, 1 ) + 1
    model_name = model_utils.get_model_name( args, version, tag=args.model_tag )
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
    model = utils.build_model( args )
    print(
        "Number of parameters: ",
        sum( p.numel() for p in model.parameters() if p.requires_grad ),
    )
    torchinfo.summary( model )
    device = model_utils.get_device()

    # Train and test dataset
    uses_curriculum_training = args.curriculum_training_steps > 0
    train_complexity_start = -1
    train_complexity_keys = None
    if uses_curriculum_training:
        # setup curriculum training
        train_complexity_start = 1
        train_complexity_keys = model_utils.load_complexity_keys( args )

    train_dataset = BaseDataset( train_keys, args, k_start=train_complexity_start, k_keys=train_complexity_keys )
    test_dataset = BaseDataset( test_keys, args )

    train_dataloader = DataLoader(
        train_dataset,
        args.batch_size,
        shuffle=False,
        num_workers=args.num_workers,
        collate_fn=collate_fn,
        # multiprocessing_context='fork' if torch.backends.mps.is_available() else None
    )

    test_dataloader = DataLoader(
        test_dataset,
        args.batch_size,
        shuffle=False,
        num_workers=args.num_workers,
        collate_fn=collate_fn,
        # multiprocessing_context='fork' if torch.backends.mps.is_available() else None
    )

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

    # Optimizer
    optimizer = utils.build_optimizer( model, args )
    clf_opt = optim.Adam( model.clf_model.parameters(), lr=args.lr )

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
            pos_target, pos_query, neg_target, neg_query = sample
            emb_pos_target, emb_pos_query = model.emb_model( pos_target ), model.emb_model( pos_query )
            emb_neg_target, emb_neg_query = model.emb_model( neg_target ), model.emb_model( neg_query )

            emb_targets = torch.cat( (emb_pos_target, emb_neg_target), dim=0 )
            emb_queries = torch.cat( (emb_pos_query, emb_neg_query), dim=0 )
            labels = torch.tensor( [ 1 ] * pos_target.num_graphs + [ 0 ] * neg_target.num_graphs ).to(
                model_utils.get_device() )

            pred = model( emb_targets, emb_queries )
            loss = model.criterion( pred, None, labels )
            loss.backward()
            torch.nn.utils.clip_grad_norm_( model.parameters(), 1.0 )
            optimizer.step()

            with torch.no_grad():
                pred = model.predict( pred )

            model.clf_model.zero_grad()
            pred = model.clf_model( pred.unsqueeze( 1 ) )
            criterion = nn.NLLLoss()
            clf_loss = criterion( pred, labels )
            clf_loss.backward()
            clf_opt.step()

            pred = pred.argmax( dim=-1 )
            # batch_acc = torch.mean( (pred == labels).type( torch.float ) )

            # Print loss at the end of tqdm bar
            # loss_norm = loss.cpu().item() / pos_target.num_graphs
            pbar.set_postfix_str( "Loss: %.4f" % loss.cpu().item() )

            # Collect loss, true label and predicted label
            train_losses.append( loss.cpu().item() )
            train_true.append( labels.cpu().numpy() )
            train_pred.append( pred.cpu().numpy() )

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

        test_raw_preds = [ ]
        for sample in tqdm( test_dataloader ):
            pos_target, pos_query, neg_target, neg_query = sample
            pos_target = pos_target.to( model_utils.get_device() )
            pos_query = pos_query.to( model_utils.get_device() )
            neg_target = neg_target.to( model_utils.get_device() )
            neg_query = neg_query.to( model_utils.get_device() )
            labels = torch.tensor(
                [ 1 ] * (pos_target.num_graphs if pos_target else 0) + [ 0 ] * neg_target.num_graphs ).to(
                model_utils.get_device() )
            with torch.no_grad():

                emb_neg_a, emb_neg_b = (model.emb_model( neg_target ), model.emb_model( neg_query ))

                if pos_target:
                    emb_pos_a, emb_pos_b = (model.emb_model( pos_target ), model.emb_model( pos_query ))
                    emb_as = torch.cat( (emb_pos_a, emb_neg_a), dim=0 )
                    emb_bs = torch.cat( (emb_pos_b, emb_neg_b), dim=0 )
                else:
                    emb_as, emb_bs = emb_neg_a, emb_neg_b

                pred = model( emb_as, emb_bs )
                loss = model.criterion( pred, None, labels )
                raw_pred = model.predict( pred )

                pred = model.clf_model( raw_pred.unsqueeze( 1 ) ).argmax( dim=-1 )
                raw_pred *= -1

            # Collect loss, true label and predicted label
            # loss_norm = loss.cpu().item() / pos_target.num_graphs
            test_losses.append( loss.cpu().item() )
            test_true.append( labels.cpu().numpy() )
            test_pred.append( pred.cpu().numpy() )
            test_raw_preds.append( raw_pred.cpu().numpy() )

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

        if uses_curriculum_training:
            if epoch > 0 and epoch % args.curriculum_training_steps == 0:
                # increase complexity limit every x epochs
                train_dataset.increase_complexity()

    log_file.close()
    return version


def write_evaluation( args, version ):
    model_name = model_utils.get_model_name( args, version, tag=args.model_tag )
    args = arg_utils.load_args( args, model_name )
    evaluate( args, version )


if __name__ == "__main__":
    args = arg_utils.parse_args()
    args.dataset = "CPG_augm"
    args.directed = False
    args.anchored = True
    args.batch_size = 512
    args.curriculum_training_steps = 2  # graph complexity increase every x epochs
    args.seed = 23
    args.num_workers = 0
    args.embedding_dim = 5  # possible labels
    if args.anchored:
        args.embedding_dim += 1  # labels + 1 anchor embedding

    # new
    args.model_tag = "experimental_SAGE"
    args.conv_type = 'SAGE'
    args.method_type = 'order'
    # args.dataset = 'syn'
    args.n_layers = 8  # at least diameter of k-hop neighbourhood
    # args.batch_size = 64
    args.hidden_dim = 64
    args.skip = "learnable"
    args.dropout = 0.0
    # args.n_batches = 100000
    args.opt = 'adam'
    args.opt_scheduler = 'none'
    args.opt_restart = 100
    args.weight_decay = 0.0
    args.lr = 1e-4
    args.margin = 0.1
    # args.test_set = ''
    # args.eval_interval = 1000
    # args.n_workers = 4
    # args.model_path = "ckpt/model.pt"
    # args.model_args_path = "ckpt/model_args.json"
    # args.tag = ''
    # args.val_size = 2048
    args.node_anchored = True
    # args.test = False

    print( args )
    version = train( args )
    write_evaluation( args, version )
