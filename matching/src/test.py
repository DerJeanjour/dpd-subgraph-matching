import torch
import torch.nn as nn
from sklearn.metrics import roc_auc_score, confusion_matrix, average_precision_score

import utils
from args import Args, load_args
from dataset import gen_batch, gen_data_loaders, get_dataset
from model import build_model


def validate( args: Args, model: nn.Module, dataset ):
    model = model.to(utils.get_device())
    loaders = gen_data_loaders( args.val_size, args.batch_size )
    test_pts = [ ]
    for batch_target, batch_neg_target, batch_neg_query in zip( *loaders ):
        test_pts.append( gen_batch( dataset, batch_target, batch_neg_target, batch_neg_query, False ) )

    model.eval()
    all_raw_preds, all_preds, all_labels = [ ], [ ], [ ]
    for pos_a, pos_b, neg_a, neg_b in test_pts:
        if pos_a:
            pos_a = pos_a.to( utils.get_device() )
            pos_b = pos_b.to( utils.get_device() )
        neg_a = neg_a.to( utils.get_device() )
        neg_b = neg_b.to( utils.get_device() )
        labels = torch.tensor( [ 1 ] * (pos_a.num_graphs if pos_a else 0) + [ 0 ] * neg_a.num_graphs ).to(
            utils.get_device() )
        with torch.no_grad():

            emb_neg_a, emb_neg_b = (model.emb_model( neg_a ), model.emb_model( neg_b ))

            if pos_a:
                emb_pos_a, emb_pos_b = (model.emb_model( pos_a ), model.emb_model( pos_b ))
                emb_as = torch.cat( (emb_pos_a, emb_neg_a), dim=0 )
                emb_bs = torch.cat( (emb_pos_b, emb_neg_b), dim=0 )
            else:
                emb_as, emb_bs = emb_neg_a, emb_neg_b

            pred = model( emb_as, emb_bs )
            raw_pred = model.predict( pred )

            pred = model.clf_model( raw_pred.unsqueeze( 1 ) ).argmax( dim=-1 )
            raw_pred *= -1

        all_raw_preds.append( raw_pred )
        all_preds.append( pred )
        all_labels.append( labels )

    pred = torch.cat( all_preds, dim=-1 )
    labels = torch.cat( all_labels, dim=-1 )
    raw_pred = torch.cat( all_raw_preds, dim=-1 )
    acc = torch.mean( (pred == labels).type( torch.float ) )
    prec = (torch.sum( pred * labels ).item() / torch.sum( pred ).item() if torch.sum( pred ) > 0 else float( "NaN" ))
    recall = (
        torch.sum( pred * labels ).item() / torch.sum( labels ).item() if torch.sum( labels ) > 0 else float( "NaN" ))
    labels = labels.detach().cpu().numpy()
    raw_pred = raw_pred.detach().cpu().numpy()
    pred = pred.detach().cpu().numpy()
    auroc = roc_auc_score( labels, raw_pred )
    avg_prec = average_precision_score( labels, raw_pred )
    tn, fp, fn, tp = confusion_matrix( labels, pred ).ravel()

    return acc, prec, recall, auroc, avg_prec, tn, fp, fn, tp


if __name__ == "__main__":
    args = load_args( Args().model_args_path )
    args.test = True
    model = build_model( args )
    dataset = get_dataset()
    results = validate( args, model, dataset )
    acc, prec, recall, auroc, avg_prec, tn, fp, fn, tp = results
    print( f"A: {acc:.4f} / P: {prec:.4f} / R: {recall:.4f}. AR: {auroc:.4f} / AP: {avg_prec:.4f}." )
    print( f"TN: {tn} / FP: {fp} / FN: {fn} / TP: {tp}" )
