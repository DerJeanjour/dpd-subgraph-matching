import os
from unittest.mock import patch, MagicMock

import pytest
import torch

import matching.glema.common.utils.model_utils as model_utils


class TestModelUtils:
    def test_get_device( self ):
        # Test with default
        device = model_utils.get_device()
        assert isinstance( device, torch.device )

        # Test with force_cpu=True
        device = model_utils.get_device( force_cpu=True )
        assert device.type == 'cpu'

    def test_get_dataset_name( self ):
        # Test undirected
        args = MagicMock( dataset='test_dataset', directed=False )
        assert model_utils.get_dataset_name( args ) == 'test_dataset'

        # Test directed
        args = MagicMock( dataset='test_dataset', directed=True )
        assert model_utils.get_dataset_name( args ) == 'test_dataset_directed'

    def test_get_model_name( self ):
        # Test basic model name
        args = MagicMock( dataset='test_dataset', directed=False, anchored=False )
        assert model_utils.get_model_name( args, 1 ) == 'test_dataset_undirected_v1'

        # Test with directed
        args = MagicMock( dataset='test_dataset', directed=True, anchored=False )
        assert model_utils.get_model_name( args, 1 ) == 'test_dataset_directed_v1'

        # Test with anchored
        args = MagicMock( dataset='test_dataset', directed=False, anchored=True )
        assert model_utils.get_model_name( args, 1 ) == 'test_dataset_undirected_anchored_v1'

        # Test with tag
        args = MagicMock( dataset='test_dataset', directed=False, anchored=False )
        assert model_utils.get_model_name( args, 1, tag='test_tag' ) == 'test_dataset_undirected_test_tag_v1'

        # Test with version
        args = MagicMock( dataset='test_dataset', directed=False, anchored=False )
        assert model_utils.get_model_name( args, 2 ) == 'test_dataset_undirected_v2'

        # Test invalid version
        with pytest.raises( ValueError ):
            model_utils.get_model_name( args, 0 )

    @patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' )
    @patch( 'matching.glema.common.utils.io_utils.get_filenames_in_dir' )
    def test_get_latest_model_version( self, mock_get_filenames, mock_get_abs_path ):
        args = MagicMock( dataset='test_dataset', directed=False, anchored=False, ckpt_dir='test_dir' )
        mock_get_abs_path.return_value = '/abs/test_dir'

        # Test when no model exists
        mock_get_filenames.return_value = [ ]
        assert model_utils.get_latest_model_version( args ) == -1

        # Test when v1 exists
        mock_get_filenames.return_value = [ 'test_dataset_undirected_v1' ]
        assert model_utils.get_latest_model_version( args ) == 1

        # Test when v1 and v2 exist
        mock_get_filenames.return_value = [ 'test_dataset_undirected_v1', 'test_dataset_undirected_v2' ]
        assert model_utils.get_latest_model_version( args ) == 2

    @patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' )
    def test_get_model_ckpt_dir( self, mock_get_abs_path ):
        args = MagicMock( ckpt_dir='test_dir' )
        mock_get_abs_path.return_value = '/abs/test_dir'

        model_name = 'test_model'
        expected_path = os.path.join( '/abs/test_dir', model_name )
        assert model_utils.get_model_ckpt_dir( args, model_name ) == expected_path

    def test_onehot_encoding( self ):
        # Test with anchored=True
        result = model_utils.onehot_encoding( label_idx=2, anchor_idx=1, embedding_dim=5, anchored=True )
        assert result == [ 1, 0, 1, 0, 0 ]

        # Test with anchored=False
        result = model_utils.onehot_encoding( label_idx=2, anchor_idx=1, embedding_dim=5, anchored=False )
        assert result == [ 0, 1, 0, 0, 0 ]

    def test_one_of_k_encoding( self ):
        # Test valid input
        result = model_utils.one_of_k_encoding( 2, [ 1, 2, 3 ] )
        assert result == [ False, True, False ]

        # Test invalid input
        with pytest.raises( Exception ):
            model_utils.one_of_k_encoding( 4, [ 1, 2, 3 ] )

    def test_one_of_k_encoding_unk( self ):
        # Test valid input
        result = model_utils.one_of_k_encoding_unk( 2, [ 1, 2, 3 ] )
        assert result == [ False, True, False ]

        # Test invalid input (maps to last element)
        result = model_utils.one_of_k_encoding_unk( 4, [ 1, 2, 3 ] )
        assert result == [ False, False, True ]

    def test_get_shape_of_tensors( self ):
        tensors = [ torch.zeros( 2, 3 ), torch.zeros( 4, 5, 6 ) ]
        shapes = model_utils.get_shape_of_tensors( tensors )
        assert shapes == [ (2, 3), (4, 5, 6) ]

    @patch( 'builtins.open', MagicMock() )
    @patch( 'pickle.load' )
    @patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' )
    def test_load_complexity_keys( self, mock_get_abs_path, mock_pickle_load ):
        args = MagicMock( dataset='test_dataset', directed=False, data_processed_dir='test_data_dir' )
        mock_get_abs_path.return_value = '/abs/test_path'
        mock_pickle_load.return_value = [ 'key1', 'key2' ]

        # Test train=True
        result = model_utils.load_complexity_keys( args, train=True )
        assert len( result ) == 6
        assert 1 in result and result[ 1 ] == [ 'key1', 'key2' ]

        # Test train=False
        result = model_utils.load_complexity_keys( args, train=False )
        assert len( result ) == 6
        assert 1 in result and result[ 1 ] == [ 'key1', 'key2' ]
