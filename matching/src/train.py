import os

import torch
import torch.multiprocessing as mp
import torch.nn as nn
import torch.optim as optim
import torchinfo
from torch.utils.tensorboard import SummaryWriter

import utils
from args import Args, save_args
from dataset import get_dataset, gen_batch, gen_data_loaders
from model import build_model, build_optimizer
from test import validate


def train( model: nn.Module, dataset, args: Args, in_queue: mp.Queue, out_queue: mp.Queue ):
    optimizer = build_optimizer( model, args )
    clf_opt = optim.Adam( model.clf_model.parameters(), lr=args.lr )

    done = False
    while not done:

        loaders = gen_data_loaders( args.eval_interval * args.batch_size, args.batch_size )
        for batch_target, batch_neg_target, batch_neg_query in zip( *loaders ):

            msg, _ = in_queue.get()
            if msg == "done":
                done = True
                break

            model.train()
            model.zero_grad()
            pos_target, pos_query, neg_target, neg_query = gen_batch( dataset, batch_target, batch_neg_target,
                                                                      batch_neg_query, True )
            emb_pos_target, emb_pos_query = model.emb_model( pos_target ), model.emb_model( pos_query )
            emb_neg_target, emb_neg_query = model.emb_model( neg_target ), model.emb_model( neg_query )

            emb_targets = torch.cat( (emb_pos_target, emb_neg_target), dim=0 )
            emb_queries = torch.cat( (emb_pos_query, emb_neg_query), dim=0 )
            labels = torch.tensor( [ 1 ] * pos_target.num_graphs + [ 0 ] * neg_target.num_graphs ).to(
                utils.get_device() )

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
            acc = torch.mean( (pred == labels).type( torch.float ) )

            out_queue.put( ("step", (loss.item(), acc)) )


def train_model():
    args = Args()
    mp.set_start_method( "spawn", force=True )
    print( f"Starting {args.n_workers} workers ..." )
    in_queue, out_queue = mp.Queue(), mp.Queue()

    if not os.path.exists( os.path.dirname( args.model_path ) ):
        os.makedirs( os.path.dirname( args.model_path ) )

    model = build_model( args )
    torchinfo.summary( model )
    model.share_memory()
    dataset = get_dataset()

    record_keys = [ "conv_type", "n_layers", "hidden_dim", "margin" ]
    args_str = ".".join( [ "{}={}".format( k, v ) for k, v in sorted( vars( args ).items() ) if k in record_keys ] )
    logger = SummaryWriter( log_dir=f'runs/{args.conv_type}{args.n_layers}_{utils.get_timestamp()}', comment=args_str )

    workers = [ ]
    for i in range( args.n_workers ):
        worker = mp.Process( target=train, args=(model, dataset, args, in_queue, out_queue) )
        worker.start()
        workers.append( worker )

    batch_n = 0
    epochs = args.n_batches // args.eval_interval
    for epoch in range( epochs ):

        print( f"--- Training Epoch {epoch}/{epochs} ---" )

        epoch_loss = 0
        epoch_acc = 0

        # train
        for i in range( args.eval_interval ):
            in_queue.put( ("step", None) )
        for i in range( args.eval_interval ):
            msg, params = out_queue.get()
            loss, acc = params
            epoch_loss += loss
            epoch_acc += acc
            batch_n += 1
            if i % 10 == 0:
                print( f"\r[Batch {batch_n}] - Loss: {loss:.4f} / Accuracy: {acc:.4f}", end="" )
        print( "\n", end="" )
        epoch_loss = epoch_loss / args.eval_interval
        epoch_acc = epoch_acc / args.eval_interval
        logger.add_scalar( 'train/loss', epoch_loss, epoch )
        logger.add_scalar( 'train/accuracy', epoch_acc, epoch )

        # validate
        print( f"--- Validation Epoch {epoch}/{epochs} ---" )
        results = validate( args, model, dataset )
        acc, prec, recall, auroc, avg_prec, tn, fp, fn, tp = results
        print( f"A: {acc:.4f} / P: {prec:.4f} / R: {recall:.4f}. AR: {auroc:.4f} / AP: {avg_prec:.4f}." )
        print( f"TN: {tn} / FP: {fp} / FN: {fn} / TP: {tp}" )
        logger.add_scalar( "test/accuracy", acc, epoch )
        logger.add_scalar( "test/precision", prec, epoch )
        logger.add_scalar( "test/recall", recall, epoch )
        logger.add_scalar( "test/auroc", auroc, epoch )
        logger.add_scalar( "test/avg_prec", avg_prec, epoch )
        logger.add_scalar( "test/TP", tp, epoch )
        logger.add_scalar( "test/TN", tn, epoch )
        logger.add_scalar( "test/FP", fp, epoch )
        logger.add_scalar( "test/FN", fn, epoch )

        torch.save( model.state_dict(), args.model_path )
        save_args( args, args.model_args_path )

    # clean up workers
    for i in range( args.n_workers ):
        in_queue.put( ("done", None) )
    for worker in workers:
        worker.join()


if __name__ == "__main__":
    train_model()
