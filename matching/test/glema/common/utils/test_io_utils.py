import os
import shutil
import tempfile
from pathlib import Path
from unittest.mock import patch

from matching.glema.common.utils.io_utils import (
    get_abs_file_path,
    get_project_root,
    path_exists,
    delete_path,
    delete_paths,
    get_filenames_in_dir
)


class TestIOUtils:

    def setup_method( self ):
        self.temp_dir = tempfile.mkdtemp()
        self.test_file_path = os.path.join( self.temp_dir, "test_file.txt" )
        with open( self.test_file_path, "w" ) as f:
            f.write( "test content" )

        self.test_subdir = os.path.join( self.temp_dir, "subdir" )
        os.makedirs( self.test_subdir )

    def teardown_method( self ):
        shutil.rmtree( self.temp_dir )

    @patch( "matching.misc.utils.get_abs_file_path" )
    def test_get_abs_file_path_with_subproject( self, mock_get_abs_file_path ):
        mock_get_abs_file_path.return_value = "/abs/path/glema/test/path"
        result = get_abs_file_path( "test/path" )
        mock_get_abs_file_path.assert_called_once_with( "glema/test/path" )
        assert result == "/abs/path/glema/test/path"

    @patch( "matching.misc.utils.get_abs_file_path" )
    def test_get_abs_file_path_without_subproject( self, mock_get_abs_file_path ):
        mock_get_abs_file_path.return_value = "/abs/path/test/path"
        result = get_abs_file_path( "test/path", with_subproject=False )
        mock_get_abs_file_path.assert_called_once_with( "test/path" )
        assert result == "/abs/path/test/path"

    @patch( "matching.misc.utils.get_project_root" )
    def test_get_project_root( self, mock_get_project_root ):
        expected_path = Path( "/project/root" )
        mock_get_project_root.return_value = expected_path
        result = get_project_root()
        assert result == expected_path

    @patch( "matching.misc.utils.path_exists" )
    def test_path_exists( self, mock_path_exists ):
        mock_path_exists.return_value = True
        assert path_exists( "some/path" ) is True
        mock_path_exists.return_value = False
        assert path_exists( "some/path" ) is False

    @patch( "matching.misc.utils.delete_path" )
    def test_delete_path( self, mock_delete_path ):
        delete_path( "some/path", dry_run=False )
        mock_delete_path.assert_called_once_with( "some/path", dry_run=False )

        mock_delete_path.reset_mock()
        delete_path( "some/path", dry_run=True )
        mock_delete_path.assert_called_once_with( "some/path", dry_run=True )

    @patch( "matching.misc.utils.delete_paths" )
    def test_delete_paths( self, mock_delete_paths ):
        paths = [ "path1", "path2" ]
        delete_paths( paths, dry_run=False )
        mock_delete_paths.assert_called_once_with( paths, dry_run=False )

        mock_delete_paths.reset_mock()
        delete_paths( paths, dry_run=True )
        mock_delete_paths.assert_called_once_with( paths, dry_run=True )

    @patch( "matching.misc.utils.get_filenames_in_dir" )
    def test_get_filenames_in_dir( self, mock_get_filenames ):
        expected_files = [ "file1.txt", "file2.txt" ]
        mock_get_filenames.return_value = expected_files

        result = get_filenames_in_dir( "some/dir", only_files=True )
        assert result == expected_files
        mock_get_filenames.assert_called_once_with( "some/dir", only_files=True )

        mock_get_filenames.reset_mock()
        mock_get_filenames.return_value = [ "dir1", "file1.txt" ]
        result = get_filenames_in_dir( "some/dir", only_files=False )
        assert result == [ "dir1", "file1.txt" ]
        mock_get_filenames.assert_called_once_with( "some/dir", only_files=False )
