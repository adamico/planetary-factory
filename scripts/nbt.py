"""A minimal NBT writer, enough to emit a Minecraft structure template.

The pack authors its worldgen as data rather than in-game, and a structure template is the
one worldgen file that is not JSON. Rather than build the starting area by hand in a creative
world and export it with a structure block -- which makes the layout unreviewable in a diff
and unregenerable after a tuning change -- the templates are generated, and this is the
writer they go through.

Only the tags a structure template uses are implemented: byte, int, string, list and
compound. No reader: nothing in the pack reads NBT back.
"""

import gzip
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10


class Int(int):
    """An int that must be written as TAG_Int even where a bare int would fit a byte."""


def _utf8(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _tag_id(value):
    if isinstance(value, Int):
        return TAG_INT
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, list):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError("no NBT tag for %r" % (value,))


def _payload(value):
    tag = _tag_id(value)
    if tag == TAG_BYTE:
        return struct.pack(">b", 1 if value else 0)
    if tag == TAG_INT:
        return struct.pack(">i", int(value))
    if tag == TAG_STRING:
        return _utf8(value)
    if tag == TAG_LIST:
        # An empty list is TAG_End-typed, which is what vanilla writes and reads back as
        # empty regardless of the type the field expects.
        element = _tag_id(value[0]) if value else TAG_END
        for item in value:
            if _tag_id(item) != element:
                raise TypeError("heterogeneous NBT list")
        body = b"".join(_payload(item) for item in value)
        return struct.pack(">Bi", element, len(value)) + body
    if tag == TAG_COMPOUND:
        body = b""
        for name, item in value.items():
            body += struct.pack(">B", _tag_id(item)) + _utf8(name) + _payload(item)
        return body + struct.pack(">B", TAG_END)
    raise TypeError("no NBT payload for %r" % (value,))


def write(path, root):
    """Write `root` (a dict) as a gzipped, unnamed root compound -- the structure format."""
    data = struct.pack(">B", TAG_COMPOUND) + _utf8("") + _payload(root)
    with gzip.GzipFile(path, "wb", mtime=0) as handle:
        handle.write(data)
