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


def test_get_enum_idx():
    assert misc_utils.get_enum_idx( cpg_const.NodeLabel.RECORD ) == 1


def test_get_enum_by_value_success():
    expected = cpg_const.NodeLabel.RECORD
    actual = misc_utils.get_enum_by_value( cpg_const.NodeLabel, expected.value )
    assert expected == actual


def test_get_enum_by_value_error():
    with pytest.raises( ValueError ):
        misc_utils.get_enum_by_value( cpg_const.NodeLabel, "non_existent_value" )


def test_flip_key_values():
    original = { "a": 1, "b": 2, "c": 3 }
    expected = { 1: "a", 2: "b", 3: "c" }
    assert misc_utils.flip_key_values( original ) == expected


def test_zip_merge():
    lists = [ [ 1, 2 ], [ 3, 4, 5 ], [ 6 ] ]
    expected = [ 1, 3, 6, 2, 4, 5 ]
    assert misc_utils.zip_merge( lists ) == expected


def test_partition_list():
    lst = [ 1, 2, 3, 4, 5, 6, 7 ]
    batch_size = 3
    expected = [ [ 1, 2, 3 ], [ 4, 5, 6 ], [ 7 ] ]
    assert misc_utils.partition_list( lst, batch_size ) == expected


def test_map_num_array_to_range():
    arr = [ 0, 5, 10 ]
    expected = [ 0, 0.5, 1 ]
    result = misc_utils.map_num_array_to_range( arr )
    assert all( abs( a - b ) < 1e-10 for a, b in zip( result, expected ) )


def test_sort_dict_by_key():
    d = { "c": 3, "a": 1, "b": 2 }
    expected = { "a": 1, "b": 2, "c": 3 }
    assert misc_utils.sort_dict_by_key( d ) == expected


def test_sort_dict_by_key_reverse():
    d = { "a": 1, "b": 2, "c": 3 }
    expected = { "c": 3, "b": 2, "a": 1 }
    assert misc_utils.sort_dict_by_key( d, reverse=True ) == expected


def test_sort_dict_by_value():
    d = { "c": 3, "a": 1, "b": 2 }
    expected = { "a": 1, "b": 2, "c": 3 }
    assert misc_utils.sort_dict_by_value( d ) == expected


def test_sort_dict_by_value_reverse():
    d = { "a": 1, "b": 2, "c": 3 }
    expected = { "c": 3, "b": 2, "a": 1 }
    assert misc_utils.sort_dict_by_value( d, reverse=True ) == expected
