/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.indexer.storage.paged.store;

/**
 * A data page is a byte buffer that contains persistent data of a page.
 */
public class Data {
  /**
   * The space required for the checksum and additional fillers.
   */
  public static final int LENGTH_FILLER = 2;

  /**
   * The length of an integer value.
   */
  public static final int LENGTH_INT = 4;

  /**
   * The length of a long value.
   */
  public static final int LENGTH_LONG = 8;

  /**
   * Create a new data page using the given data for the given handler. The handler will decide what
   * type of buffer is created.
   * 
   * @param handler the data handler
   * @param buff the data
   * @return the data page
   */
  public static Data create(DataHandler handler, byte[] buff) {
    return new Data(handler, buff);
  }

  /**
   * Create a new data page for the given handler. The handler will decide what type of buffer is
   * created.
   * 
   * @param handler the data handler
   * @param capacity the initial capacity of the buffer
   * @return the data page
   */
  public static Data create(DataHandler handler, int capacity) {
    return new Data(handler, new byte[capacity]);
  }

  public static String decodeString(byte[] buff, int length) {
    int len = computeStringLength(buff, length);
    char[] chars = new char[len];
    int i = 0;
    for (int p = 0; p < length;) {
      int x = buff[p++] & 0xff;
      if (x < 0x80) {
        chars[i++] = (char) x;
      } else if (x >= 0xe0) {
        chars[i++] = (char) (((x & 0xf) << 12) + ((buff[p++] & 0x3f) << 6) + (buff[p++] & 0x3f));
      } else {
        chars[i++] = (char) (((x & 0x1f) << 6) + (buff[p++] & 0x3f));
      }
    }
    return new String(chars);
  }

  public static int encodeString(String s, byte[] buff) {
    int len = s.length();
    int p = 0;
    if (buff == null) {
      for (int i = 0; i < len; i++) {
        int c = s.charAt(i);
        if (c > 0 && c < 0x80) {
          p++;
        } else if (c >= 0x800) {
          p += 3;
        } else {
          p += 2;
        }
      }
    } else {
      for (int i = 0; i < len; i++) {
        int c = s.charAt(i);
        if (c > 0 && c < 0x80) {
          buff[p++] = (byte) c;
        } else if (c >= 0x800) {
          buff[p++] = (byte) (0xe0 | (c >> 12));
          buff[p++] = (byte) (0x80 | ((c >> 6) & 0x3f));
          buff[p++] = (byte) (0x80 | (c & 0x3f));
        } else {
          buff[p++] = (byte) (0xc0 | (c >> 6));
          buff[p++] = (byte) (0x80 | (c & 0x3f));
        }
      }
    }
    return p;
  }

  public static byte[] encodeStringAndCopy(String s, byte[] tempBuff) {
    int size = encodeString(s, tempBuff);
    byte[] result = new byte[size];
    System.arraycopy(tempBuff, 0, result, 0, size);
    return result;
  }

  private static int computeStringLength(byte[] buff, int length) {
    int len = 0;
    for (int p = 0; p < length;) {
      int x = buff[p] & 0xff;
      if (x < 0x80) {
        len++;
        p += 1;
      } else if (x >= 0xe0) {
        len++;
        p += 3;
      } else {
        len++;
        p += 2;
      }
    }
    return len;
  }

  private static int getStringLenUTF8(String s) {
    int plus = 4, len = s.length();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c >= 0x800) {
        plus += 2;
      } else if (c == 0 || c >= 0x80) {
        plus++;
      }
    }
    return len + plus;
  }

  /**
   * The data handler responsible for lob objects.
   */
  protected DataHandler handler;

  /**
   * The data itself.
   */
  protected byte[] data;

  /**
   * The current write or read position.
   */
  protected int pos;

  private Data(DataHandler handler, byte[] data) {
    this.handler = handler;
    this.data = data;
  }

  public int byteLength() {
    return data.length;
  }

  /**
   * Increase the size to the given length. The current position is set to the given value.
   * 
   * @param len the new length
   */
  public void fill(int len) {
    if (pos > len) {
      pos = len;
    }
    pos = len;
  }

  /**
   * Get the byte array used for this page.
   * 
   * @return the byte array
   */
  public byte[] getBytes() {
    return data;
  }

  public int getPos() {
    return pos;
  }

  /**
   * Get the length of a String value.
   * 
   * @param s the value
   * @return the length
   */
  public int getStringLen(String s) {
    return getStringLenUTF8(s);
  }

  /**
   * Get the current write position of this data page, which is the current length.
   * 
   * @return the length
   */
  public int length() {
    return pos;
  }

  public void move(int source, int destination, int length) {
    System.arraycopy(data, source, data, destination, length);
  }

  public void offset(int source, int offset, int length) {
    move(source, source + offset, length);
  }

  /**
   * Copy a number of bytes to the given buffer from the current position. The current position is
   * incremented accordingly.
   * 
   * @param buff the output buffer
   * @param off the offset in the output buffer
   * @param len the number of bytes to copy
   */
  public void read(byte[] buff, int off, int len) {
    System.arraycopy(data, pos, buff, off, len);
    pos += len;
  }

  /**
   * Read one single byte.
   * 
   * @return the value
   */
  public int readByte() {
    return data[pos++];
  }

  /**
   * Read an integer at the current position. The current position is incremented.
   * 
   * @return the value
   */
  public int readInt() {
    byte[] buff = data;
    return (buff[pos++] << 24) + ((buff[pos++] & 0xff) << 16) + ((buff[pos++] & 0xff) << 8)
        + (buff[pos++] & 0xff);
  }

  /**
   * Read a long value. This method reads two int values and combines them.
   * 
   * @return the long value
   */
  public long readLong() {
    return ((long) (readInt()) << 32) + (readInt() & 0xffffffffL);
  }

  /**
   * Read an short integer at the current position. The current position is incremented.
   * 
   * @return the value
   */
  public int readShortInt() {
    byte[] buff = data;
    return ((buff[pos++] & 0xff) << 8) + (buff[pos++] & 0xff);
  }

  /**
   * Read a String value. The current position is incremented.
   * 
   * @return the value
   */
  public String readString() {
    byte[] buff = data;
    int p = pos;
    int len = ((buff[p++] & 0xff) << 24) + ((buff[p++] & 0xff) << 16) + ((buff[p++] & 0xff) << 8)
        + (buff[p++] & 0xff);
    char[] chars = new char[len];
    for (int i = 0; i < len; i++) {
      int x = buff[p++] & 0xff;
      if (x < 0x80) {
        chars[i] = (char) x;
      } else if (x >= 0xe0) {
        chars[i] = (char) (((x & 0xf) << 12) + ((buff[p++] & 0x3f) << 6) + (buff[p++] & 0x3f));
      } else {
        chars[i] = (char) (((x & 0x1f) << 6) + (buff[p++] & 0x3f));
      }
    }
    pos = p;
    return new String(chars);
  }

  /**
   * Set the position to 0.
   */
  public void reset() {
    pos = 0;
  }

  public Data seek(int delta) {
    pos += delta;
    return this;
  }

  /**
   * Update an integer at the given position. The current position is not change.
   * 
   * @param pos the position
   * @param x the value
   */
  public void setInt(int pos, int x) {
    byte[] buff = data;
    buff[pos] = (byte) (x >> 24);
    buff[pos + 1] = (byte) (x >> 16);
    buff[pos + 2] = (byte) (x >> 8);
    buff[pos + 3] = (byte) x;
  }

  // public void writeValue(Value v) throws PagedMemoryException {
  // // TODO text output: could be in the Value... classes
  // if (v == ValueNull.INSTANCE) {
  // data[pos++] = '-';
  // return;
  // }
  // int start = pos;
  // data[pos++] = (byte) (v.getType() + 'a');
  // switch (v.getType()) {
  // case Value.BOOLEAN:
  // case Value.BYTE:
  // case Value.SHORT:
  // case Value.INT:
  // writeInt(v.getInt());
  // break;
  // case Value.LONG:
  // writeLong(v.getLong());
  // break;
  // case Value.DECIMAL:
  // String s = v.getString();
  // writeString(s);
  // break;
  // case Value.TIME:
  // writeLong(v.getTimeNoCopy().getTime());
  // break;
  // case Value.DATE:
  // writeLong(v.getDateNoCopy().getTime());
  // break;
  // case Value.TIMESTAMP: {
  // Timestamp ts = v.getTimestampNoCopy();
  // writeLong(ts.getTime());
  // writeInt(ts.getNanos());
  // break;
  // }
  // case Value.JAVA_OBJECT:
  // case Value.BYTES: {
  // byte[] b = v.getBytesNoCopy();
  // writeInt(b.length);
  // write(b, 0, b.length);
  // break;
  // }
  // case Value.UUID: {
  // ValueUuid uuid = (ValueUuid) v;
  // writeLong(uuid.getHigh());
  // writeLong(uuid.getLow());
  // break;
  // }
  // case Value.STRING:
  // case Value.STRING_IGNORECASE:
  // case Value.STRING_FIXED:
  // writeString(v.getString());
  // break;
  // case Value.DOUBLE:
  // writeLong(Double.doubleToLongBits(v.getDouble()));
  // break;
  // case Value.FLOAT:
  // writeInt(Float.floatToIntBits(v.getFloat()));
  // break;
  // case Value.BLOB:
  // case Value.CLOB: {
  // ValueLob lob = (ValueLob) v;
  // lob.convertToFileIfRequired(handler);
  // byte[] small = lob.getSmall();
  // if (small == null) {
  // // -2 for historical reasons (-1 didn't store precision)
  // int type = -2;
  // if (!lob.isLinked()) {
  // type = -3;
  // }
  // writeInt(type);
  // writeInt(lob.getTableId());
  // writeInt(lob.getObjectId());
  // writeLong(lob.getPrecision());
  // writeByte((byte) (lob.useCompression() ? 1 : 0));
  // if (type == -3) {
  // writeString(lob.getFileName());
  // }
  // } else {
  // writeInt(small.length);
  // write(small, 0, small.length);
  // }
  // break;
  // }
  // case Value.ARRAY: {
  // Value[] list = ((ValueArray) v).getList();
  // writeInt(list.length);
  // for (Value x : list) {
  // writeValue(x);
  // }
  // break;
  // }
  // default:
  // Message.throwInternalError("type=" + v.getType());
  // }
  // if (SysProperties.CHECK2) {
  // if (pos - start != getValueLen(v)) {
  // throw Message
  // .throwInternalError("value size error: got " + (pos - start) + " expected "
  // + getValueLen(v));
  // }
  // }
  // }

  // /**
  // * Calculate the number of bytes required to encode the given value.
  // *
  // * @param v the value
  // * @return the number of bytes required to store this value
  // */
  // public int getValueLen(Value v) throws PagedMemoryException {
  // if (v == ValueNull.INSTANCE) {
  // return 1;
  // }
  // switch (v.getType()) {
  // case Value.BOOLEAN:
  // case Value.BYTE:
  // case Value.SHORT:
  // case Value.INT:
  // return 1 + LENGTH_INT;
  // case Value.LONG:
  // return 1 + LENGTH_LONG;
  // case Value.DOUBLE:
  // return 1 + LENGTH_LONG;
  // case Value.FLOAT:
  // return 1 + LENGTH_INT;
  // case Value.STRING:
  // case Value.STRING_IGNORECASE:
  // case Value.STRING_FIXED:
  // return 1 + getStringLen(v.getString());
  // case Value.DECIMAL:
  // return 1 + getStringLen(v.getString());
  // case Value.JAVA_OBJECT:
  // case Value.BYTES: {
  // int len = v.getBytesNoCopy().length;
  // return 1 + LENGTH_INT + len;
  // }
  // case Value.UUID:
  // return 1 + LENGTH_LONG + LENGTH_LONG;
  // case Value.TIME:
  // return 1 + LENGTH_LONG;
  // case Value.DATE:
  // return 1 + LENGTH_LONG;
  // case Value.TIMESTAMP:
  // return 1 + LENGTH_LONG + LENGTH_INT;
  // case Value.BLOB:
  // case Value.CLOB: {
  // int len = 1;
  // ValueLob lob = (ValueLob) v;
  // lob.convertToFileIfRequired(handler);
  // byte[] small = lob.getSmall();
  // if (small != null) {
  // len += LENGTH_INT + small.length;
  // } else {
  // len += LENGTH_INT + LENGTH_INT + LENGTH_INT + LENGTH_LONG + 1;
  // if (!lob.isLinked()) {
  // len += getStringLen(lob.getFileName());
  // }
  // }
  // return len;
  // }
  // case Value.ARRAY: {
  // Value[] list = ((ValueArray) v).getList();
  // int len = 1 + LENGTH_INT;
  // for (Value x : list) {
  // len += getValueLen(x);
  // }
  // return len;
  // }
  // default:
  // throw Message.throwInternalError("type=" + v.getType());
  // }
  // }

  // /**
  // * Read a value.
  // *
  // * @return the value
  // */
  // public Value readValue() throws PagedMemoryException {
  // int dataType = data[pos++];
  // if (dataType == '-') {
  // return ValueNull.INSTANCE;
  // }
  // dataType = dataType - 'a';
  // switch (dataType) {
  // case Value.BOOLEAN:
  // return ValueBoolean.get(readInt() == 1);
  // case Value.BYTE:
  // return ValueByte.get((byte) readInt());
  // case Value.SHORT:
  // return ValueShort.get((short) readInt());
  // case Value.INT:
  // return ValueInt.get(readInt());
  // case Value.LONG:
  // return ValueLong.get(readLong());
  // case Value.DECIMAL:
  // return ValueDecimal.get(new BigDecimal(readString()));
  // case Value.DATE:
  // return ValueDate.getNoCopy(new Date(readLong()));
  // case Value.TIME:
  // // need to normalize the year, month and day
  // return ValueTime.get(new Time(readLong()));
  // case Value.TIMESTAMP: {
  // Timestamp ts = new Timestamp(readLong());
  // ts.setNanos(readInt());
  // return ValueTimestamp.getNoCopy(ts);
  // }
  // case Value.JAVA_OBJECT: {
  // int len = readInt();
  // byte[] b = MemoryUtils.newBytes(len);
  // read(b, 0, len);
  // return ValueJavaObject.getNoCopy(b);
  // }
  // case Value.BYTES: {
  // int len = readInt();
  // byte[] b = MemoryUtils.newBytes(len);
  // read(b, 0, len);
  // return ValueBytes.getNoCopy(b);
  // }
  // case Value.UUID:
  // return ValueUuid.get(readLong(), readLong());
  // case Value.STRING:
  // return ValueString.get(readString());
  // case Value.STRING_IGNORECASE:
  // return ValueStringIgnoreCase.get(readString());
  // case Value.STRING_FIXED:
  // return ValueStringFixed.get(readString());
  // case Value.DOUBLE:
  // return ValueDouble.get(Double.longBitsToDouble(readLong()));
  // case Value.FLOAT:
  // return ValueFloat.get(Float.intBitsToFloat(readInt()));
  // case Value.BLOB:
  // case Value.CLOB: {
  // int smallLen = readInt();
  // if (smallLen >= 0) {
  // byte[] small = MemoryUtils.newBytes(smallLen);
  // read(small, 0, smallLen);
  // return ValueLob.createSmallLob(dataType, small);
  // }
  // int tableId = readInt();
  // int objectId = readInt();
  // long precision = 0;
  // boolean compression = false;
  // // -1: historical (didn't store precision)
  // // -2: regular
  // // -3: regular, but not linked (in this case: including file name)
  // if (smallLen == -2 || smallLen == -3) {
  // precision = readLong();
  // compression = readByte() == 1;
  // }
  // ValueLob lob = ValueLob.open(dataType, handler, tableId, objectId,
  // precision, compression);
  // if (smallLen == -3) {
  // lob.setFileName(readString(), false);
  // }
  // return lob;
  // }
  // case Value.ARRAY: {
  // int len = readInt();
  // Value[] list = new Value[len];
  // for (int i = 0; i < len; i++) {
  // list[i] = readValue();
  // }
  // return ValueArray.get(list);
  // }
  // default:
  // throw Message.throwInternalError("type=" + dataType);
  // }
  // }

  /**
   * Set the current read / write position.
   * 
   * @param pos the new position
   */
  public void setPos(int pos) {
    this.pos = pos;
  }

  /**
   * Shrink the array to this size.
   * 
   * @param size the new size
   */
  public void truncate(int size) {
    if (pos > size) {
      byte[] buff = new byte[size];
      System.arraycopy(data, 0, buff, 0, size);
      this.pos = size;
      data = buff;
    }
  }

  public void write(byte[] buff) {
    write(buff, 0, buff.length);
  }

  /**
   * Append a number of bytes to this data page.
   * 
   * @param buff the data
   * @param off the offset in the data
   * @param len the length in bytes
   */
  public void write(byte[] buff, int off, int len) {
    System.arraycopy(buff, off, data, pos, len);
    pos += len;
  }

  /**
   * Append one single byte.
   * 
   * @param x the value
   */
  public void writeByte(byte x) {
    data[pos++] = x;
  }

  /**
   * Append the contents of the given data page to this page. The filler is not appended.
   * 
   * @param page the page that will be appended
   */
  public void writeDataPageNoSize(Data page) {
    // don't write filler
    int len = page.pos - LENGTH_FILLER;
    System.arraycopy(page.data, 0, data, pos, len);
    pos += len;
  }

  /**
   * Write an integer at the current position. The current position is incremented.
   * 
   * @param x the value
   */
  public void writeInt(int x) {
    byte[] buff = data;
    buff[pos++] = (byte) (x >> 24);
    buff[pos++] = (byte) (x >> 16);
    buff[pos++] = (byte) (x >> 8);
    buff[pos++] = (byte) x;
  }

  /**
   * Append a long value. This method writes two int values.
   * 
   * @param x the value
   */
  public void writeLong(long x) {
    writeInt((int) (x >>> 32));
    writeInt((int) x);
  }

  /**
   * Write a short integer at the current position. The current position is incremented.
   * 
   * @param x the value
   */
  public void writeShortInt(int x) {
    byte[] buff = data;
    buff[pos++] = (byte) (x >> 8);
    buff[pos++] = (byte) x;
  }

  /**
   * Write a String value. The current position is incremented.
   * 
   * @param s the value
   */
  public void writeString(String s) {
    int len = s.length();
    int p = pos;
    byte[] buff = data;
    buff[p++] = (byte) (len >> 24);
    buff[p++] = (byte) (len >> 16);
    buff[p++] = (byte) (len >> 8);
    buff[p++] = (byte) len;
    for (int i = 0; i < len; i++) {
      int c = s.charAt(i);
      if (c > 0 && c < 0x80) {
        buff[p++] = (byte) c;
      } else if (c >= 0x800) {
        buff[p++] = (byte) (0xe0 | (c >> 12));
        buff[p++] = (byte) (0x80 | ((c >> 6) & 0x3f));
        buff[p++] = (byte) (0x80 | (c & 0x3f));
      } else {
        buff[p++] = (byte) (0xc0 | (c >> 6));
        buff[p++] = (byte) (0x80 | (c & 0x3f));
      }
    }
    pos = p;
  }

  public void zeroFill(int offset, int additionalData) {
    for (int i = offset; i < offset + additionalData; i++) {
      data[i] = 0;
    }
  }
}
