from enum import Enum

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
