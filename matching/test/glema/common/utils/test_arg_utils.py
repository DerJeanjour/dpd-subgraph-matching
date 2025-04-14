import sys
import pytest
import os
from unittest.mock import patch
import json
import tempfile

import matching.glema.common.utils.arg_utils as arg_utils
import matching.glema.common.utils.model_utils as model_utils
import matching.glema.common.utils.io_utils as io_utils


def test_parse_args_default():
    """Test parse_args with use_default=True returns default arguments."""
    args = arg_utils.parse_args(use_default=True)
    assert args.neo4j_protocol == "bolt://"
    assert args.neo4j_host == "localhost:"
    assert args.seed == 42
    assert args.num_workers == os.cpu_count()


@pytest.mark.parametrize("args", [
    ["--model", "TestModel"],
    ["--seed", "100"],
    ["--lr", "0.001"],
    ["--epoch", "50"],
    ["--batch_size", "64"],
    ["--directed"],
    ["--iso"],
    ["--inference", "--testonly"]
])
def test_parse_args_with_parameters(args):
    """Test parse_args with various command line arguments."""
    with patch.object(sys, 'argv', ['program'] + args):
        parsed_args = arg_utils.parse_args()
        
        if "--model" in args:
            assert parsed_args.model == "TestModel"
        if "--seed" in args:
            assert parsed_args.seed == 100
        if "--lr" in args:
            assert parsed_args.lr == 0.001
        if "--epoch" in args:
            assert parsed_args.epoch == 50
        if "--batch_size" in args:
            assert parsed_args.batch_size == 64
        if "--directed" in args:
            assert parsed_args.directed is True
        if "--iso" in args:
            assert parsed_args.iso is True
        if "--testonly" in args:
            assert parsed_args.testonly is True


def test_save_and_load_args():
    """Test saving and loading args."""
    # Create temporary directory for test files
    with tempfile.TemporaryDirectory() as tmpdir:
        # Mock the paths and functions
        with patch.object(io_utils, 'get_abs_file_path', return_value=os.path.join(tmpdir, "args.json")):
            with patch.object(model_utils, 'get_model_ckpt_dir', return_value=tmpdir):
                with patch.object(model_utils, 'get_model_ckpt', return_value=os.path.join(tmpdir, "model.pt")):
                    
                    # Create args with custom values
                    args = arg_utils.parse_args(use_default=True)
                    args.model = "TestModel"
                    args.seed = 99
                    
                    # Save args
                    arg_utils.save_args(args, "TestModel")
                    
                    # Verify file was created
                    assert os.path.exists(os.path.join(tmpdir, "args.json"))
                    
                    # Load args into a new object
                    new_args = arg_utils.parse_args(use_default=True)
                    new_args = arg_utils.load_args(new_args, "TestModel")
                    
                    # Verify values match
                    assert new_args.model == "TestModel"
                    assert new_args.seed == 99
                    assert new_args.ckpt_path == os.path.join(tmpdir, "model.pt")


def test_get_model_args_path():
    """Test get_model_args_path returns correct path."""
    args = arg_utils.parse_args(use_default=True)
    with patch.object(model_utils, 'get_model_ckpt_dir', return_value="/fake/path"):
        path = arg_utils.get_model_args_path(args, "TestModel")
        assert path == "/fake/path/args.json"