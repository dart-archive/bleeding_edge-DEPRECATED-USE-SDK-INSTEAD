// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.


// TODO(jmesserly): this should be in the corelib somewhere. They were taken
// from samples/chat/http_impl.dart. There are actually various UTF8 decoders
// in the codebase but this one has the API we need (StringStream not quite
// flexible enough). I couldn't find another encoder.

String decodeUtf8(List<int> bytes) {
  var d = new UTF8Decoder();
  d.writeList(bytes);
  return d.toString();
}

List<int> encodeUtf8(String str) => UTF8Encoder.encodeString(str);

// Utility class for decoding UTF-8 from data delivered as a stream of
// bytes.
class UTF8Decoder {
  UTF8Decoder()
      : _bufferList = new BufferList(),
        _result = new StringBuffer();

  // Add UTF-8 encoded data.
  void writeList(List<int> buffer) {
    _bufferList.add(buffer);
    // Only process as much data as we know is safe.
    while (_bufferList.length >= 4) {
      _processNext();
    }
  }

  // Return the decoded string.
  String toString() {
    // Process any leftover data.
    while (_bufferList.length > 0) {
      _processNext();
    }
    return _result.toString();
  }

  // Process the next UTF-8 encoded character.
  void _processNext() {
    int value = _bufferList.next() & 0xFF;
    if ((value & 0x80) == 0x80) {
      int additionalBytes;
      if ((value & 0xe0) == 0xc0) {  // 110xxxxx
        value = value & 0x1F;
        additionalBytes = 1;
      } else if ((value & 0xf0) == 0xe0) {  // 1110xxxx
        value = value & 0x0F;
        additionalBytes = 2;
      } else {  // 11110xxx
        value = value & 0x07;
        additionalBytes = 3;
      }
      for (int i = 0; i < additionalBytes; i++) {
        int byte = _bufferList.next();
        value = value << 6 | (byte & 0x3F);
      }
    }
    _result.addCharCode(value);
  }

  BufferList _bufferList;
  StringBuffer _result;
}

// Utility class which can deliver bytes one by one from a number of
// buffers added.
class BufferList {
  BufferList() : _index = 0, _length = 0, _buffers = new Queue<List<int>>();

  void add(List<int> buffer) {
    _buffers.addLast(buffer);
    _length += buffer.length;
  }

  int next() {
    int value = _buffers.first()[_index++];
    _length--;
    if (_index == _buffers.first().length) {
      _buffers.removeFirst();
      _index = 0;
    }
    return value;
  }

  int get length() => _length;

  int _length;
  Queue<List<int>> _buffers;
  int _index;
}


// Utility class for encoding a string into UTF-8 byte stream.
class UTF8Encoder {
  static List<int> encodeString(String string) {
    int size = _encodingSize(string);
    List result = new List<int>(size);
    _encodeString(string, result);
    return result;
  }

  static int _encodingSize(String string) => _encodeString(string, null);

  static int _encodeString(String string, List<int> buffer) {
    int pos = 0;
    int length = string.length;
    for (int i = 0; i < length; i++) {
      int additionalBytes;
      int charCode = string.charCodeAt(i);
      if (charCode <= 0x007F) {
        additionalBytes = 0;
        if (buffer != null) buffer[pos] = charCode;
      } else if (charCode <= 0x07FF) {
        // 110xxxxx (xxxxx is top 5 bits).
        if (buffer != null) buffer[pos] = ((charCode >> 6) & 0x1F) | 0xC0;
        additionalBytes = 1;
      } else if (charCode <= 0xFFFF) {
        // 1110xxxx (xxxx is top 4 bits)
        if (buffer != null) buffer[pos] = ((charCode >> 12) & 0x0F)| 0xE0;
        additionalBytes = 2;
      } else {
        // 11110xxx (xxx is top 3 bits)
        if (buffer != null) buffer[pos] = ((charCode >> 18) & 0x07) | 0xF0;
        additionalBytes = 3;
      }
      pos++;
      if (buffer != null) {
        for (int i = additionalBytes; i > 0; i--) {
          // 10xxxxxx (xxxxxx is next 6 bits from the top).
          buffer[pos++] = ((charCode >> (6 * (i - 1))) & 0x3F) | 0x80;
        }
      } else {
        pos += additionalBytes;
      }
    }
    return pos;
  }
}
