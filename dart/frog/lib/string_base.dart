// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class StringBase {
  // TODO(jmesserly): this array copy is really unfortunate
  // TODO(jmesserly): check the performance of String.fromCharCode.apply
  static String createFromCharCodes(List<int> charCodes) native @'''
if (Object.getPrototypeOf(charCodes) !== Array.prototype) {
  var length = charCodes.length;
  var tmp = new Array(length);
  for (var i = 0; i < length; i++) {
    tmp[i] = charCodes.$index(i);
  }
  charCodes = tmp;
}
return String.fromCharCode.apply(null, charCodes);
''';

  static String join(List<String> strings, String separator) {
    if (strings.length == 0) return '';
    String s = strings[0];
    for (int i = 1; i < strings.length; i++) {
      s = s + separator + strings[i];
    }
    return s;
  }

  static String concatAll(List<String> strings) => join(strings, "");
}
