import os
import shutil
import tempfile
import pytest
from matching.misc.utils import delete_path, path_exists

class TestDeletePath:
    
    def setup_method(self):
        # Create temporary directory for tests
        self.temp_dir = tempfile.mkdtemp()
        self.temp_file = os.path.join(self.temp_dir, "test_file.txt")
        with open(self.temp_file, "w") as f:
            f.write("test content")
        
        self.temp_subdir = os.path.join(self.temp_dir, "test_subdir")
        os.makedirs(self.temp_subdir)
        
        # Create a symlink if platform supports it
        self.temp_link = os.path.join(self.temp_dir, "test_link.txt")
        try:
            os.symlink(self.temp_file, self.temp_link)
            self.symlink_created = True
        except (OSError, AttributeError):
            # Skip symlink tests on platforms that don't support it
            self.symlink_created = False
    
    def teardown_method(self):
        # Clean up any remaining test files
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)
    
    def test_delete_file(self, capsys):
        # Test deleting a file
        assert path_exists(self.temp_file)
        delete_path(self.temp_file)
        assert not path_exists(self.temp_file)
        
        captured = capsys.readouterr()
        assert f"Deleted file: {self.temp_file}" in captured.out
    
    def test_delete_directory(self, capsys):
        # Test deleting a directory
        assert path_exists(self.temp_subdir)
        delete_path(self.temp_subdir)
        assert not path_exists(self.temp_subdir)
        
        captured = capsys.readouterr()
        assert f"Deleted directory: {self.temp_subdir}" in captured.out
    
    def test_delete_symlink(self, capsys):
        # Skip test if symlink creation failed
        if not self.symlink_created:
            pytest.skip("Symlink creation not supported on this platform")
        
        assert path_exists(self.temp_link)
        delete_path(self.temp_link)
        assert not path_exists(self.temp_link)
        # Original file should still exist
        assert path_exists(self.temp_file)
        
        captured = capsys.readouterr()
        assert f"Deleted file: {self.temp_link}" in captured.out
    
    def test_delete_nonexistent_path(self, capsys):
        # Test deleting a path that doesn't exist
        nonexistent_path = os.path.join(self.temp_dir, "nonexistent.txt")
        assert not path_exists(nonexistent_path)
        
        delete_path(nonexistent_path)
        
        captured = capsys.readouterr()
        assert f"Can't delete non existing path: {nonexistent_path}" in captured.out
    
    def test_dry_run_mode(self, capsys):
        # Test dry_run mode (should not delete anything)
        assert path_exists(self.temp_file)
        
        delete_path(self.temp_file, dry_run=True)
        
        # File should still exist
        assert path_exists(self.temp_file)
        
        captured = capsys.readouterr()
        assert f"Deleted file: {self.temp_file}" in captured.out