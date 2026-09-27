"""Minimal NBT writer (gzip), enough for structure templates."""
import gzip
import struct


class Byte(int):
    pass


class Short(int):
    pass


class Long(int):
    pass


class Float(float):
    pass


class IntList(list):
    pass


class DoubleList(list):
    pass


def _tag_id(v):
    if isinstance(v, bool) or isinstance(v, Byte):
        return 1
    if isinstance(v, Short):
        return 2
    if isinstance(v, Long):
        return 4
    if isinstance(v, int):
        return 3
    if isinstance(v, Float):
        return 5
    if isinstance(v, float):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, (list, IntList, DoubleList)):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(type(v))


def _payload(v, out):
    t = _tag_id(v)
    if t == 1:
        out += struct.pack(">b", int(v))
    elif t == 2:
        out += struct.pack(">h", v)
    elif t == 3:
        out += struct.pack(">i", v)
    elif t == 4:
        out += struct.pack(">q", v)
    elif t == 5:
        out += struct.pack(">f", v)
    elif t == 6:
        out += struct.pack(">d", v)
    elif t == 8:
        b = v.encode("utf-8")
        out += struct.pack(">H", len(b)) + b
    elif t == 9:
        if isinstance(v, IntList):
            et = 3
        elif isinstance(v, DoubleList):
            et = 6
        else:
            et = _tag_id(v[0]) if v else 0
        out += struct.pack(">bi", et, len(v))
        for item in v:
            if et == 6:
                out += struct.pack(">d", float(item))
            elif et == 3:
                out += struct.pack(">i", int(item))
            else:
                _payload(item, out)
    elif t == 10:
        for k, item in v.items():
            out += struct.pack(">b", _tag_id(item))
            kb = k.encode("utf-8")
            out += struct.pack(">H", len(kb)) + kb
            _payload(item, out)
        out += b"\x00"


def write(path, root):
    out = bytearray()
    out += struct.pack(">b", 10)
    out += struct.pack(">H", 0)
    _payload(root, out)
    with gzip.open(path, "wb") as fh:
        fh.write(bytes(out))
