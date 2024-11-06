import json


class Args:
    def __init__( self ):
        self.conv_type = 'graph'
        self.method_type = 'order'
        self.dataset = 'syn'
        self.n_layers = 8 # at least diameter of k-hop neighbourhood
        self.batch_size = 64
        self.hidden_dim = 64
        self.skip = "learnable"
        self.dropout = 0.0
        self.n_batches = 100000
        self.opt = 'adam'
        self.opt_scheduler = 'none'
        self.opt_restart = 100
        self.weight_decay = 0.0
        self.lr = 1e-4
        self.margin = 0.1
        self.test_set = ''
        self.eval_interval = 1000
        self.n_workers = 4
        self.model_path = "ckpt/model.pt"
        self.model_args_path = "ckpt/model_args.json"
        self.tag = ''
        self.val_size = 2048
        self.node_anchored = True
        self.test = False


# Function to save Args to JSON
def save_args( args: Args, filename: str ) -> None:
    # Convert Args class attributes to a dictionary
    args_dict = { k: v for k, v in args.__dict__.items() if not k.startswith( '__' ) and not callable( v ) }

    # Write dictionary to JSON file
    with open( filename, 'w' ) as f:
        json.dump( args_dict, f, indent=4 )


# Function to load Args from JSON
def load_args( filename: str ) -> Args:
    # Create a new instance of Args
    args = Args()

    # Read JSON file
    with open( filename, 'r' ) as f:
        args_dict = json.load( f )

    # Set the attributes of Args from the dictionary
    for key, value in args_dict.items():
        setattr( args, key, value )

    return args
