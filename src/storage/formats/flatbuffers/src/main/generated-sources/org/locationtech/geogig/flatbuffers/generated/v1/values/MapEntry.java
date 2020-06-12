// automatically generated by the FlatBuffers compiler, do not modify

package org.locationtech.geogig.flatbuffers.generated.v1.values;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

@SuppressWarnings("unused")
public final class MapEntry extends Table {
  public static MapEntry getRootAsMapEntry(ByteBuffer _bb) { return getRootAsMapEntry(_bb, new MapEntry()); }
  public static MapEntry getRootAsMapEntry(ByteBuffer _bb, MapEntry obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public MapEntry __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String key() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer keyAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer keyInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public Value value() { return value(new Value()); }
  public Value value(Value obj) { int o = __offset(6); return o != 0 ? obj.__assign(__indirect(o + bb_pos), bb) : null; }

  public static int createMapEntry(FlatBufferBuilder builder,
      int keyOffset,
      int valueOffset) {
    builder.startObject(2);
    MapEntry.addValue(builder, valueOffset);
    MapEntry.addKey(builder, keyOffset);
    return MapEntry.endMapEntry(builder);
  }

  public static void startMapEntry(FlatBufferBuilder builder) { builder.startObject(2); }
  public static void addKey(FlatBufferBuilder builder, int keyOffset) { builder.addOffset(0, keyOffset, 0); }
  public static void addValue(FlatBufferBuilder builder, int valueOffset) { builder.addOffset(1, valueOffset, 0); }
  public static int endMapEntry(FlatBufferBuilder builder) {
    int o = builder.endObject();
    builder.required(o, 4);  // key
    return o;
  }

  @Override
  protected int keysCompare(Integer o1, Integer o2, ByteBuffer _bb) { return compareStrings(__offset(4, o1, _bb), __offset(4, o2, _bb), _bb); }

  public static MapEntry __lookup_by_key(MapEntry obj, int vectorLocation, String key, ByteBuffer bb) {
    byte[] byteKey = key.getBytes(Table.UTF8_CHARSET.get());
    int span = bb.getInt(vectorLocation - 4);
    int start = 0;
    while (span != 0) {
      int middle = span / 2;
      int tableOffset = __indirect(vectorLocation + 4 * (start + middle), bb);
      int comp = compareStrings(__offset(4, bb.capacity() - tableOffset, bb), byteKey, bb);
      if (comp > 0) {
        span = middle;
      } else if (comp < 0) {
        middle++;
        start += middle;
        span -= middle;
      } else {
        return (obj == null ? new MapEntry() : obj).__assign(tableOffset, bb);
      }
    }
    return null;
  }
}
