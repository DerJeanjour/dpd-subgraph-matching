from unittest.mock import patch, MagicMock

import pytest

import matching.glema.data.dataset_importer as dataset_importer


@pytest.fixture
def mock_args():
    args = MagicMock()
    args.raw_dataset_dir = "/raw/dir"
    args.dataset_dir = "/dataset/dir"
    args.data_processed_dir = "/processed/dir"
    args.dataset = "test_dataset"
    args.directed = False
    args.inference = False
    args.split_data = False
    return args


class TestCleanUp:
    @patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' )
    @patch( 'matching.glema.common.utils.io_utils.delete_paths' )
    def test_clean_up( self, mock_delete_paths, mock_get_abs_path, mock_args ):
        # Setup mock return values
        mock_get_abs_path.side_effect = lambda x: f"abs_{x}"

        # Run the function
        dataset_importer.clean_up( mock_args )

        # Check path construction
        expected_paths = [
            "abs_/raw/dir/test_dataset",
            "abs_/dataset/dir/test_dataset_train",
            "abs_/dataset/dir/test_dataset_test",
            "abs_/processed/dir/test_dataset"
        ]

        # Verify get_abs_file_path was called with correct paths
        assert mock_get_abs_path.call_count == 4

        # Verify delete_paths was called with the correct paths
        mock_delete_paths.assert_called_once_with( expected_paths )

    @patch( 'matching.glema.common.utils.io_utils.get_abs_file_path' )
    @patch( 'matching.glema.common.utils.io_utils.delete_paths' )
    def test_clean_up_directed( self, mock_delete_paths, mock_get_abs_path, mock_args ):
        # Set directed to True
        mock_args.directed = True

        # Setup mock return values
        mock_get_abs_path.side_effect = lambda x: f"abs_{x}"

        # Run the function
        dataset_importer.clean_up( mock_args )

        # Verify the last path includes "_directed"
        expected_paths = [
            "abs_/raw/dir/test_dataset",
            "abs_/dataset/dir/test_dataset_train",
            "abs_/dataset/dir/test_dataset_test",
            "abs_/processed/dir/test_dataset_directed"
        ]

        mock_delete_paths.assert_called_once_with( expected_paths )


class TestProcess:
    @patch( 'matching.glema.data.process.import_dataset.import_datasets' )
    @patch( 'matching.glema.data.process.data_generator.process' )
    @patch( 'matching.glema.data.process.data_synthesis.process' )
    @patch( 'matching.glema.data.process.process_data.process' )
    def test_process_normal( self, mock_processor, mock_synthesis, mock_generator, mock_importer, mock_args ):
        # Run the function
        dataset_importer.process( mock_args )

        # Verify all steps are called
        mock_importer.assert_called_once_with( mock_args )
        mock_generator.assert_called_once_with( mock_args )
        mock_synthesis.assert_called_once_with( mock_args )
        mock_processor.assert_called_once_with( mock_args )

    @patch( 'matching.glema.data.process.import_dataset.import_datasets' )
    @patch( 'matching.glema.data.process.data_generator.process' )
    @patch( 'matching.glema.data.process.data_synthesis.process' )
    @patch( 'matching.glema.data.process.process_data.process' )
    def test_process_inference( self, mock_processor, mock_synthesis, mock_generator, mock_importer, mock_args ):
        # Set inference mode
        mock_args.inference = True

        # Run the function
        dataset_importer.process( mock_args )

        # Verify only first two steps are called
        mock_importer.assert_called_once_with( mock_args )
        mock_generator.assert_called_once_with( mock_args )
        mock_synthesis.assert_not_called()
        mock_processor.assert_not_called()

    @patch( 'matching.glema.data.process.import_dataset.import_datasets' )
    @patch( 'matching.glema.data.process.data_generator.process' )
    @patch( 'matching.glema.data.process.data_synthesis.process' )
    @patch( 'matching.glema.data.process.process_data.process' )
    def test_process_split_data( self, mock_processor, mock_synthesis, mock_generator, mock_importer, mock_args ):
        # Set split_data True
        mock_args.split_data = True

        # Run the function
        dataset_importer.process( mock_args )

        # Verify synthesis is skipped but processor is called
        mock_importer.assert_called_once_with( mock_args )
        mock_generator.assert_called_once_with( mock_args )
        mock_synthesis.assert_not_called()
        mock_processor.assert_called_once_with( mock_args )
