import pytest

import matching.glema.common.utils.misc_utils as misc_utils
import matching.misc.cpg_const as cpg_const


def test_get_enum_by_idx_success():
    expected = cpg_const.NodeLabel.RECORD
    actual = misc_utils.get_enum_by_idx( cpg_const.NodeLabel, 1 )
    assert expected == actual


def test_get_enum_by_idx_error():
    with pytest.raises( IndexError ):
        misc_utils.get_enum_by_idx( cpg_const.NodeLabel, 0 )
