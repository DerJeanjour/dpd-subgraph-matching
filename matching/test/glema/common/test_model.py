from unittest.mock import patch, MagicMock

import pytest
import torch

from matching.glema.common.model import GLeMa, GLeMaNet, InferenceGNN


class TestGLeMa:
    def test_init( self ):
        model = GLeMa( n_in_feature=10, n_out_feature=20, nhop=2, nhead=4, aggregation="mean" )
        assert model.nhop == 2
        assert model.nhead == 4
        assert model.hidden_dim == 20
        assert not model.directed

    def test_forward( self ):
        batch_size = 2
        num_nodes = 5
        n_in_feature = 10
        n_out_feature = 20
        nhop = 2
        nhead = 4

        model = GLeMa( n_in_feature=n_in_feature, n_out_feature=n_out_feature, nhop=nhop, nhead=nhead )
        x = torch.randn( batch_size, num_nodes, n_in_feature )
        adj = torch.ones( batch_size, num_nodes, num_nodes )

        output = model( x, adj )
        assert output.shape == (batch_size, num_nodes, n_out_feature)

        output, attention = model( x, adj, get_attention=True )
        assert output.shape == (batch_size, num_nodes, n_out_feature)
        assert attention.shape == (batch_size, num_nodes, num_nodes)


class TestGLeMaNet:
    @pytest.fixture
    def args( self ):
        args = MagicMock()
        args.n_graph_layer = 2
        args.d_graph_layer = 64
        args.n_FC_layer = 3
        args.d_FC_layer = 128
        args.dropout_rate = 0.2
        args.branch = "balanced"
        args.tactic = "static"
        args.nhop = 2
        args.nhead = 4
        args.directed = False
        args.embedding_dim = 32
        args.al_scale = 0.1
        args.ngpu = 0
        return args

    def test_init( self, args ):
        model = GLeMaNet( args )
        assert len( model.gconv1 ) == args.n_graph_layer
        assert len( model.FC ) == args.n_FC_layer
        assert model.dropout_rate == args.dropout_rate


class TestInferenceGNN:
    @pytest.fixture
    def args( self ):
        args = MagicMock()
        args.n_graph_layer = 2
        args.d_graph_layer = 64
        args.n_FC_layer = 3
        args.d_FC_layer = 128
        args.dropout_rate = 0.2
        args.branch = "balanced"
        args.tactic = "static"
        args.nhop = 2
        args.nhead = 4
        args.directed = False
        args.embedding_dim = 32
        args.al_scale = 0.1
        args.ngpu = 0
        args.ckpt_path = None
        args.anchored = False
        return args

    @patch( 'matching.glema.common.model.model_utils.initialize_model' )
    @patch( 'matching.glema.common.model.model_utils.get_device' )
    def test_init( self, mock_get_device, mock_initialize_model, args ):
        mock_get_device.return_value = "cpu"
        mock_initialize_model.return_value = MagicMock()

        inference_gnn = InferenceGNN( args )

        assert inference_gnn.embedding_dim == args.embedding_dim
        assert inference_gnn.anchored == args.anchored
        mock_initialize_model.assert_called_once()

    @patch( 'matching.glema.common.model.encode_sample' )
    def test_prepare_single_input( self, mock_encode_sample, args ):
        mock_encode_sample.return_value = "encoded_sample"

        inference_gnn = InferenceGNN( args )
        m1, m2 = MagicMock(), MagicMock()

        result = inference_gnn.prepare_single_input( m1, m2 )

        assert result == "encoded_sample"
        mock_encode_sample.assert_called_once_with( m1, m2, args.embedding_dim, anchored=args.anchored )

    @patch( 'matching.glema.common.model.InferenceGNN.prepare_single_input' )
    def test_prepare_multi_input( self, mock_prepare_single_input, args ):
        mock_prepare_single_input.side_effect = lambda x, y: f"{x}_{y}"

        inference_gnn = InferenceGNN( args )
        list_subgraphs = [ "sub1", "sub2" ]
        list_graphs = [ "graph1", "graph2" ]

        result = inference_gnn.prepare_multi_input( list_subgraphs, list_graphs )

        assert result == [ "sub1_graph1", "sub2_graph2" ]
        assert mock_prepare_single_input.call_count == 2

    @patch( 'matching.glema.common.model.InferenceGNN.prepare_multi_input' )
    @patch( 'matching.glema.common.model.InferenceGNN.input_to_tensor' )
    def test_predict_label( self, mock_input_to_tensor, mock_prepare_multi_input, args ):
        mock_prepare_multi_input.return_value = "multi_input"
        mock_input_to_tensor.return_value = "tensor_input"

        inference_gnn = InferenceGNN( args )
        inference_gnn.model = MagicMock()
        inference_gnn.model.return_value = "prediction_result"

        result = inference_gnn.predict_label( [ "sub1" ], [ "graph1" ] )

        assert result == "prediction_result"
        mock_prepare_multi_input.assert_called_once_with( [ "sub1" ], [ "graph1" ] )
        mock_input_to_tensor.assert_called_once_with( "multi_input" )
        inference_gnn.model.assert_called_once_with( "tensor_input" )
