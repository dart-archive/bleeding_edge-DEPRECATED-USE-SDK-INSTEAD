// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class StringBuffer {

  // Constructor. Optional argument [content] takes initial content.
  StringBuffer([String content = ""]) {
    init(content);
  }

  // Returns the length of the buffer.
  int get length() {
    return length_;
  }

  // Appends item to the buffer.
  void append(String str) {
    list_.add(str);
    length_ += str.length;
  }

  // Appends all items in strings to the buffer.
  void appendAll(Collection<String> strings) {
    strings.forEach((str) { append(str); });
  }

  // Clears the string buffer.
  void clear() {
    list_ = [];
    length_ = 0;
  }

  // Sets contents of buffer to str.
  void init(String str) {
    if (str.isEmpty()) {
      clear();
    } else {
      list_ = [str];
      length_ += str.length;
    }
  }

  // Returns contents of buffer as a concatenated string.
  String toString() {
    String result = String.join(list_, "");
    list_ = [result];
    return result;
  }

  List list_;
  int length_;
}
