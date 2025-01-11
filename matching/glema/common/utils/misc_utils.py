from enum import Enum
from itertools import zip_longest

import numpy as np

import matching.misc.utils as utils


def get_timestamp() -> str:
    return utils.get_timestamp()


def set_seed( seed ):
    utils.set_seed( seed )


def get_enum_idx( enum_member: Enum ) -> int:
    enum_class = enum_member.__class__
    return list( enum_class ).index( enum_member ) + 1  # idx starts with 1


def get_enum_by_idx( enum_class: Enum, idx: int ) -> Enum:
    members = list( enum_class )
    if 1 <= idx <= len( members ):
        return members[ idx - 1 ]  # Convert 1-based index to 0-based
    raise IndexError( "Index out of range for the Enum class." )


def get_enum_by_value( enum_class: Enum, value ) -> Enum:
    members = list( enum_class )
    for member in members:
        if member.value == value:
            return member
    raise ValueError( f"Value is not present in enum: {value}." )


def flip_key_values( data: dict[ any, any ] ) -> dict[ any, any ]:
    flipped_data = { }
    for key, value in data.items():
        flipped_data[ value ] = key
    return flipped_data


def zip_merge( lists ):
    return [ item for group in zip_longest( *lists ) for item in group if item is not None ]


def partition_list( lst, batch_size ):
    """Partition a list into smaller lists of a specified batch size."""
    return [ lst[ i:i + batch_size ] for i in range( 0, len( lst ), batch_size ) ]


def map_num_array_to_range( arr: list, r_min: int = 0, r_max: int = 1 ):
    arr = np.array( arr )
    x = arr.max()
    return (arr / x) * (r_max - r_min) + r_min

def sort_dict_by_key(d, reverse=False):
    return dict(sorted(d.items(), key=lambda item: item[0], reverse=reverse))
