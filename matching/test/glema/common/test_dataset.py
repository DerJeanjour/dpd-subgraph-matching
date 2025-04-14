from unittest.mock import patch, MagicMock, mock_open

import networkx as nx
import pytest

from matching.glema.common.dataset import BaseDataset


class TestBaseDataset:
    @pytest.fixture
    def mock_args( self ):
        args = MagicMock()
        args.data_processed_dir = "test_dir"
        args.embedding_dim = 64
        args.anchored = True
        args.dataset = "test_dataset"
        return args

    @pytest.fixture
    def mock_keys( self ):
        return [ 'iso_1.pkl', 'iso_2.pkl', 'non_1.pkl', 'non_2.pkl' ]

    @pytest.fixture
    def mock_k_keys( self ):
        return { 1: [ 'iso_1.pkl', 'non_1.pkl' ], 2: [ 'iso_2.pkl', 'non_2.pkl' ] }

    def test_init( self, mock_args, mock_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path',
                                             return_value='abs_path' ):
            dataset = BaseDataset( mock_keys, mock_args )
            assert dataset.full_keys == mock_keys
            assert dataset.k == -1
            assert dataset.len == len( mock_keys )
            assert dataset.embedding_dim == mock_args.embedding_dim
            assert dataset.anchored == mock_args.anchored

    def test_balance_keys( self, mock_args, mock_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ):
            dataset = BaseDataset( mock_keys, mock_args )
            balanced = dataset.balance_keys( mock_keys, shuffle=False )
            assert len( balanced ) == 4
            assert balanced.count( 'iso_1.pkl' ) == 1
            assert balanced.count( 'iso_2.pkl' ) == 1
            assert balanced.count( 'non_1.pkl' ) == 1
            assert balanced.count( 'non_2.pkl' ) == 1

    def test_get_key_split( self, mock_args, mock_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ):
            dataset = BaseDataset( mock_keys, mock_args )
            iso, non_iso = dataset.get_key_split()
            assert len( iso ) == 2
            assert len( non_iso ) == 2
            assert all( 'iso' in key for key in iso )
            assert all( 'non' in key for key in non_iso )

    def test_increase_complexity( self, mock_args, mock_keys, mock_k_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ):
            dataset = BaseDataset( mock_keys, mock_args, k_start=1, k_keys=mock_k_keys )
            assert dataset.k == 1
            dataset.increase_complexity()
            assert dataset.k == 2
            dataset.increase_complexity()
            assert dataset.k == -1

    def test_get_data( self, mock_args, mock_keys ):
        mock_data = (nx.Graph(), nx.Graph(), [ ])

        with patch( 'os.path.join', return_value='test_path' ), \
                patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ), \
                patch( 'builtins.open', mock_open() ), \
                patch( 'pickle.load', return_value=mock_data ):
            dataset = BaseDataset( mock_keys, mock_args )
            query, source, mapping = dataset.get_data( 0 )

            assert query == mock_data[ 0 ]
            assert source == mock_data[ 1 ]
            assert mapping == mock_data[ 2 ]

    def test_set_keys_by_k( self, mock_args, mock_keys, mock_k_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ):
            # Test with k=1
            dataset = BaseDataset( mock_keys, mock_args, k_start=1, k_keys=mock_k_keys, balanced=False )
            assert len( dataset.keys ) == 2
            assert all( key in mock_k_keys[ 1 ] for key in dataset.keys )

            # Test with k=2 (should include both k=1 and k=2 keys)
            dataset = BaseDataset( mock_keys, mock_args, k_start=2, k_keys=mock_k_keys, balanced=False )
            assert len( dataset.keys ) == 4

    def test_remove_complexity_limit( self, mock_args, mock_keys, mock_k_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ), \
                patch( 'builtins.print' ) as mock_print:
            dataset = BaseDataset( mock_keys, mock_args, k_start=1, k_keys=mock_k_keys )
            assert dataset.k == 1

            dataset.remove_complexity_limit()
            assert dataset.k == -1
            assert dataset.keys != mock_k_keys[ 1 ]  # Should be using full_keys now
            mock_print.assert_called_once_with( "Removed graph sample complexity limit" )

    def test_get_key( self, mock_args, mock_keys ):
        with patch( 'os.path.join' ), patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ):
            dataset = BaseDataset( mock_keys, mock_args )

            # Test normal case
            assert dataset.get_key( 0 ) in mock_keys
            assert dataset.get_key( 1 ) in mock_keys

            # Test wrapping around
            assert dataset.get_key( len( mock_keys ) ) in mock_keys
            assert dataset.get_key( len( mock_keys ) + 1 ) in mock_keys

    def test_getitem( self, mock_args, mock_keys ):
        mock_data = (nx.Graph(), nx.Graph(), [ ])
        mock_sample = { 'key': 'sample' }

        with patch( 'os.path.join', return_value='test_path' ), \
                patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' ), \
                patch( 'builtins.open', mock_open() ), \
                patch( 'pickle.load', return_value=mock_data ), \
                patch( 'matching.glema.common.dataset.encode_sample', return_value=mock_sample ):
            dataset = BaseDataset( mock_keys, mock_args )
            result = dataset[ 0 ]

            assert result == mock_sample
