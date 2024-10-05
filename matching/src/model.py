import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
import torch_geometric.nn as pyg_nn

import utils
from args import Args


def build_model( args: Args ) -> nn.Module:
    model = OrderEmbedder( 1, args.hidden_dim, args )
    model.to( utils.get_device() )
    if args.test and args.model_path:
        model.load_state_dict( torch.load( args.model_path, map_location=utils.get_device(), weights_only=False ) )
    return model


def build_optimizer( model: nn.Module, args: Args ) -> optim.Optimizer:
    weight_decay = args.weight_decay
    filter_fn = filter( lambda p: p.requires_grad, model.parameters() )
    if args.opt == 'adam':
        optimizer = optim.Adam( filter_fn, lr=args.lr, weight_decay=weight_decay )
    return optimizer


class OrderEmbedder( nn.Module ):
    def __init__( self, input_dim, hidden_dim, args: Args ):
        super( OrderEmbedder, self ).__init__()
        self.emb_model = SkipLastGNN( input_dim, hidden_dim, hidden_dim, args )
        self.margin = args.margin
        self.use_intersection = False

        self.clf_model = nn.Sequential( nn.Linear( 1, 2 ), nn.LogSoftmax( dim=-1 ) )

    def forward( self, emb_as, emb_bs ):
        return emb_as, emb_bs

    def predict( self, pred ):
        """Predict if b is a subgraph of a (batched), where emb_as, emb_bs = pred.

        pred: list (emb_as, emb_bs) of embeddings of graph pairs

        Returns: list of bools (whether a is subgraph of b in the pair)
        """
        emb_as, emb_bs = pred
        e = torch.sum( torch.max( torch.zeros_like( emb_as, device=emb_as.device ), emb_bs - emb_as ) ** 2, dim=1 )
        return e

    def criterion( self, pred, intersect_embs, labels ):
        """Loss function for order emb.
        The e term is the amount of violation (if b is a subgraph of a).
        For positive examples, the e term is minimized (close to 0);
        for negative examples, the e term is trained to be at least greater than self.margin.

        pred: lists of embeddings outputted by forward
        intersect_embs: not used
        labels: subgraph labels for each entry in pred
        """
        emb_as, emb_bs = pred
        e = torch.sum( torch.max( torch.zeros_like( emb_as, device=utils.get_device() ), emb_bs - emb_as ) ** 2, dim=1 )

        margin = self.margin
        e[ labels == 0 ] = torch.max( torch.tensor( 0.0, device=utils.get_device() ), margin - e )[ labels == 0 ]

        relation_loss = torch.sum( e )

        return relation_loss


class SkipLastGNN( nn.Module ):
    def __init__( self, input_dim, hidden_dim, output_dim, args: Args ):
        super( SkipLastGNN, self ).__init__()
        self.dropout = args.dropout
        self.n_layers = args.n_layers

        self.feat_preprocess = None  # TODO check for reason

        self.pre_mp = nn.Sequential( nn.Linear( input_dim, hidden_dim ) )

        conv_model = self.build_conv_model( args.conv_type, 1 )
        self.convs = nn.ModuleList()

        if args.skip == 'learnable':  # TODO check for reason
            self.learnable_skip = nn.Parameter( torch.ones( self.n_layers, self.n_layers ) )

        for layer in range( args.n_layers ):
            if args.skip == 'all' or args.skip == 'learnable':
                hidden_input_dim = hidden_dim * (layer + 1)
            else:
                hidden_input_dim = hidden_dim

            self.convs.append( conv_model( hidden_input_dim, hidden_dim ) )

        post_input_dim = hidden_dim * (args.n_layers + 1)
        self.post_mp = nn.Sequential(
            nn.Linear( post_input_dim, hidden_dim ), nn.Dropout( args.dropout ),
            nn.LeakyReLU( 0.1 ),
            nn.Linear( hidden_dim, output_dim ),
            nn.ReLU(),
            nn.Linear( hidden_dim, 256 ), nn.ReLU(),
            nn.Linear( 256, hidden_dim ) )

        self.skip = args.skip
        self.conv_type = args.conv_type

    def build_conv_model( self, model_type, n_inner_layers ):
        if model_type == "GCN":
            return pyg_nn.GCNConv
        elif model_type == "graph":
            return pyg_nn.GraphConv
        elif model_type == "GAT":
            return pyg_nn.GATConv
        else:
            print( "unrecognized model type" )

    def forward( self, data ):
        if self.feat_preprocess is not None:
            if not hasattr( data, "preprocessed" ):
                data = self.feat_preprocess( data )
                data.preprocessed = True
        x, edge_index, batch = data.node_feature, data.edge_index, data.batch
        x = self.pre_mp( x )

        all_emb = x.unsqueeze( 1 )
        emb = x
        for i in range( len( self.convs ) ):
            if self.skip == 'learnable':
                skip_vals = self.learnable_skip[ i, :i + 1 ].unsqueeze( 0 ).unsqueeze( -1 )
                curr_emb = all_emb * torch.sigmoid( skip_vals )
                curr_emb = curr_emb.view( x.size( 0 ), -1 )
                x = self.convs[ i ]( curr_emb, edge_index )
            elif self.skip == 'all':
                x = self.convs[ i ]( emb, edge_index )
            else:
                x = self.convs[ i ]( x, edge_index )
            x = F.relu( x )
            x = F.dropout( x, p=self.dropout, training=self.training )
            emb = torch.cat( (emb, x), 1 )
            if self.skip == 'learnable':
                all_emb = torch.cat( (all_emb, x.unsqueeze( 1 )), 1 )

        emb = pyg_nn.global_add_pool( emb, batch )
        emb = self.post_mp( emb )
        return emb

    def loss( self, pred, label ):
        return F.nll_loss( pred, label )
