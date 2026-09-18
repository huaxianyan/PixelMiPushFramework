"""Read standard DEX class and selected method definitions, without executing code.

The layout is the Android DEX 035-040 header/class_data_item format. This is not
an ART verifier. String decoding is limited to the ASCII identifiers inspected.
"""
import hashlib
import struct
import zlib


def read_dex(data, wanted):
    if data[:8] not in {b'dex\n035\0', b'dex\n037\0', b'dex\n038\0', b'dex\n039\0', b'dex\n040\0'}:
        raise ValueError('Unsupported DEX format')

    def u32(offset):
        return struct.unpack_from('<I', data, offset)[0]

    if u32(32) != len(data) or data[12:32] != hashlib.sha1(data[32:]).digest():
        raise ValueError('DEX length or SHA-1 mismatch')
    if u32(8) != zlib.adler32(data[12:]) & 0xffffffff:
        raise ValueError('DEX checksum mismatch')

    def uleb(offset):
        value, shift = 0, 0
        while True:
            byte = data[offset]
            offset += 1
            value |= (byte & 127) << shift
            if byte < 128:
                return value, offset
            shift += 7

    def string(offset):
        _, offset = uleb(offset)
        return data[offset:data.index(b'\0', offset)].decode('ascii', errors='replace')

    strings = [string(u32(u32(60) + 4 * i)) for i in range(u32(56))]
    types = [strings[u32(u32(68) + 4 * i)] for i in range(u32(64))]

    def method(index):
        owner, p, name = struct.unpack_from('<HHI', data, u32(92) + index * 8)
        offset = u32(76) + p * 12
        params = u32(offset + 8)
        args = [] if params == 0 else [
            types[struct.unpack_from('<H', data, params + 4 + 2 * i)[0]]
            for i in range(u32(params))
        ]
        signature = strings[name] + '(' + ''.join(args) + ')' + types[u32(offset + 4)]
        return types[owner], signature

    classes, selected = set(), {}
    for i in range(u32(96)):
        offset = u32(100) + 32 * i
        owner = types[u32(offset)]
        if owner in classes:
            raise ValueError(f'Duplicate DEX class: {owner}')
        classes.add(owner)
        if owner not in wanted:
            continue
        definitions = {}
        selected[owner] = definitions
        cursor = u32(offset + 24)
        if cursor == 0:
            continue
        sizes = []
        for _ in range(4):
            size, cursor = uleb(cursor)
            sizes.append(size)
        for _ in range(sizes[0] + sizes[1]):
            _, cursor = uleb(cursor)
            _, cursor = uleb(cursor)
        for size in sizes[2:]:
            index = 0
            for _ in range(size):
                delta, cursor = uleb(cursor)
                flags, cursor = uleb(cursor)
                code, cursor = uleb(cursor)
                index += delta
                declaring, signature = method(index)
                if declaring != owner:
                    raise ValueError(f'Incorrect declaring type: {declaring}, expected {owner}')
                definitions[signature] = {'access_flags': flags, 'has_code': code != 0}
    return data[:8].decode('ascii'), classes, selected
