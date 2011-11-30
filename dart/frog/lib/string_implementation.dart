// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

//String.prototype.get$length = function() {
//  return this.length;
//}

// TODO(jimhug): Unify with code from compiler/lib/implementation.
class StringImplementation implements String native "String" {

  String operator[](int index) native;

  int charCodeAt(int index) native;

  final int length; //native since it's on a native type.

  bool operator ==(var other) native;

  bool endsWith(String other) native
  '''if (other.length > this.length) return false;
  return other == this.substring(this.length - other.length);''';

  bool startsWith(String other) native
  '''if (other.length > this.length) return false;
  return other == this.substring(0, other.length);''';

  int indexOf(String other, [int start]) native;
  int lastIndexOf(String other, [int start]) native;

  bool isEmpty() => length == 0;

  String concat(String other) native;

  String operator +(Object obj) native { obj.toString(); }

  String substring(int startIndex, [int endIndex = null]) native;

  String trim() native;

  // TODO(jmesserly): should support pattern too.
  bool contains(Pattern pattern, int startIndex) native
    "return this.indexOf(pattern, startIndex) >= 0;";

  String replaceFirst(Pattern from, String to) native
    "return this.replace(from, to);";

  String replaceAll(Pattern from, String to) native @"""
if (typeof(from) == 'string' || from instanceof String) {
  from = new RegExp(from.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&"), 'g');
}
return this.replace(from, to);""";

  List<String> split(Pattern pattern) native;

  /*
  Iterable<Match> allMatches(String str) {
    List<Match> result = [];
    if (this.isEmpty()) return result;
    int length = this.length;

    int ix = 0;
    while (ix < str.length) {
      int foundIx = str.indexOf(this, ix);
      if (foundIx < 0) break;
      // Call "toString" to coerce the "this" back to a primitive string.
      result.add(new _StringMatch(foundIx, str, this.toString()));
      ix = foundIx + length;
    }
    return result;
  }
  */

  // TODO(jimhug): Get correct reified generic list here.
  List<String> splitChars() native "return this.split('');";

  List<int> charCodes() {
    int len = length;
    List<int> result = new List<int>(len);
    for (int i = 0; i < len; i++) {
      result[i] = charCodeAt(i);
    }
    return result;
  }

  String toLowerCase() native;
  String toUpperCase() native;

  int hashCode() native '''if (this.hash_ === undefined) {
    for (var i = 0; i < this.length; i++) {
      var ch = this.charCodeAt(i);
      this.hash_ += ch;
      this.hash_ += this.hash_ << 10;
      this.hash_ ^= this.hash_ >> 6;
    }

    this.hash_ += this.hash_ << 3;
    this.hash_ ^= this.hash_ >> 11;
    this.hash_ += this.hash_ << 15;
    this.hash_ = this.hash_ & ((1 << 29) - 1);
  }
  return this.hash_;''';

  int compareTo(String other) native
    "return this == other ? 0 : this < other ? -1 : 1;";
}

/*
class _StringMatch implements Match {
  const _StringMatch(int this._start,
                     String this.str,
                     String this.pattern);

  int start() => _start;
  int end() => _start + pattern.length;
  String operator[](int g) => group(g);
  int groupCount() => 0;

  String group(int group) {
    if (group != 0) {
      throw new IndexOutOfRangeException(group);
    }
    return pattern;
  }

  List<String> groups(List<int> groups) {
    List<String> result = new List<String>();
    for (int g in groups) {
      result.add(group(g));
    }
    return result;
  }

  final int _start;
  final String str;
  final String pattern;
}
*/
