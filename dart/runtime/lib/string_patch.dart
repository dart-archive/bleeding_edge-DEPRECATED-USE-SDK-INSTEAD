// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

patch class String {
  /* patch */ factory String.fromCharCodes(Iterable<int> charCodes) {
    return _StringBase.createFromCharCodes(charCodes);
  }

  /* patch */ const factory String.fromEnvironment(String name,
                                                   {String defaultValue})
      native "String_fromEnvironment";
}


/**
 * [_StringBase] contains common methods used by concrete String
 * implementations, e.g., _OneByteString.
 */
class _StringBase {

  factory _StringBase._uninstantiable() {
    throw new UnsupportedError(
        "_StringBase can't be instaniated");
  }

  Type get runtimeType => String;

  int get hashCode native "String_getHashCode";

  /**
   *  Create the most efficient string representation for specified
   *  [codePoints].
   */
  static String createFromCharCodes(Iterable<int> charCodes) {
    if (charCodes != null) {
      // TODO(srdjan): Also skip copying of typed arrays.
      final ccid = charCodes._cid;
      if ((ccid != _List._classId) &&
          (ccid != _GrowableList._classId) &&
          (ccid != _ImmutableList._classId)) {
        charCodes = new List<int>.from(charCodes, growable: false);
      }

      bool isOneByteString = true;
      for (int i = 0; i < charCodes.length; i++) {
        int e = charCodes[i];
        if (e is! _Smi) throw new ArgumentError(e);
        // Is e Latin1?
        if ((e < 0) || (e > 0xFF)) {
          isOneByteString = false;
          break;
        }
      }
      if (isOneByteString) {
        var s = _OneByteString._allocate(charCodes.length);
        for (int i = 0; i < charCodes.length; i++) {
          s._setAt(i, charCodes[i]);
        }
        return s;
      }
    }
    return _createFromCodePoints(charCodes);
  }

  static String _createFromCodePoints(List<int> codePoints)
      native "StringBase_createFromCodePoints";

  String operator [](int index) native "String_charAt";

  int codeUnitAt(int index) native "String_codeUnitAt";

  int get length native "String_getLength";

  bool get isEmpty {
    return this.length == 0;
  }

  bool get isNotEmpty => !isEmpty;

  String operator +(String other) native "String_concat";

  String toString() {
    return this;
  }

  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    if ((other is! String) ||
        (this.length != other.length)) {
      return false;
    }
    final len = this.length;
    for (int i = 0; i < len; i++) {
      if (this.codeUnitAt(i) != other.codeUnitAt(i)) {
        return false;
      }
    }
    return true;
  }


  int compareTo(String other) {
    int thisLength = this.length;
    int otherLength = other.length;
    int len = (thisLength < otherLength) ? thisLength : otherLength;
    for (int i = 0; i < len; i++) {
      int thisCodePoint = this.codeUnitAt(i);
      int otherCodePoint = other.codeUnitAt(i);
      if (thisCodePoint < otherCodePoint) {
        return -1;
      }
      if (thisCodePoint > otherCodePoint) {
        return 1;
      }
    }
    if (thisLength < otherLength) return -1;
    if (thisLength > otherLength) return 1;
    return 0;
  }

  bool _substringMatches(int start, String other) {
    if (other.isEmpty) return true;
    final len = other.length;
    if ((start < 0) || (start + len > this.length)) {
      return false;
    }
    for (int i = 0; i < len; i++) {
      if (this.codeUnitAt(i + start) != other.codeUnitAt(i)) {
        return false;
      }
    }
    return true;
  }

  bool endsWith(String other) {
    return _substringMatches(this.length - other.length, other);
  }

  bool startsWith(Pattern pattern, [int index = 0]) {
    if ((index < 0) || (index > this.length)) {
      throw new RangeError.range(index, 0, this.length);
    }
    if (pattern is String) {
      return _substringMatches(index, pattern);
    }
    return pattern.matchAsPrefix(this, index) != null;
  }

  int indexOf(Pattern pattern, [int start = 0]) {
    if ((start < 0) || (start > this.length)) {
      throw new RangeError.range(start, 0, this.length);
    }
    if (pattern is String) {
      String other = pattern;
      int maxIndex = this.length - other.length;
      // TODO: Use an efficient string search (e.g. BMH).
      for (int index = start; index <= maxIndex; index++) {
        if (_substringMatches(index, other)) {
          return index;
        }
      }
      return -1;
    }
    for (int i = start; i <= this.length; i++) {
      // TODO(11276); This has quadratic behavior because matchAsPrefix tries
      // to find a later match too. Optimize matchAsPrefix to avoid this.
      if (pattern.matchAsPrefix(this, i) != null) return i;
    }
    return -1;
  }

  int lastIndexOf(Pattern pattern, [int start = null]) {
    if (start == null) {
      start = this.length;
    } else if (start < 0 || start > this.length) {
      throw new RangeError.range(start, 0, this.length);
    }
    if (pattern is String) {
      String other = pattern;
      int maxIndex = this.length - other.length;
      if (maxIndex < start) start = maxIndex;
      for (int index = start; index >= 0; index--) {
        if (_substringMatches(index, other)) {
          return index;
        }
      }
      return -1;
    }
    for (int i = start; i >= 0; i--) {
      // TODO(11276); This has quadratic behavior because matchAsPrefix tries
      // to find a later match too. Optimize matchAsPrefix to avoid this.
      if (pattern.matchAsPrefix(this, i) != null) return i;
    }
    return -1;
  }

  String substring(int startIndex, [int endIndex]) {
    if (endIndex == null) endIndex = this.length;

    if ((startIndex < 0) || (startIndex > this.length)) {
      throw new RangeError.value(startIndex);
    }
    if ((endIndex < 0) || (endIndex > this.length)) {
      throw new RangeError.value(endIndex);
    }
    if (startIndex > endIndex) {
      throw new RangeError.value(startIndex);
    }
    return _substringUnchecked(startIndex, endIndex);
  }

  String _substringUnchecked(int startIndex, int endIndex) {
    assert(endIndex != null);
    assert((startIndex >= 0) && (startIndex <= this.length));
    assert((endIndex >= 0) && (endIndex <= this.length));
    assert(startIndex <= endIndex);

    if (startIndex == endIndex) {
      return "";
    }
    if ((startIndex + 1) == endIndex) {
      return this[startIndex];
    }
    return _substringUncheckedNative(startIndex, endIndex);
  }

  String _substringUncheckedNative(int startIndex, int endIndex)
      native "StringBase_substringUnchecked";

  // Checks for one-byte whitespaces only.
  static bool _isOneByteWhitespace(int codePoint) {
    return
      (codePoint == 32) || // Space.
      ((9 <= codePoint) && (codePoint <= 13)) || // CR, LF, TAB, etc.
      (codePoint == 0x85) ||  // NEL
      (codePoint == 0xA0);  // NBSP
  }

  // Characters with Whitespace property (Unicode 6.2).
  // 0009..000D    ; White_Space # Cc       <control-0009>..<control-000D>
  // 0020          ; White_Space # Zs       SPACE
  // 0085          ; White_Space # Cc       <control-0085>
  // 00A0          ; White_Space # Zs       NO-BREAK SPACE
  // 1680          ; White_Space # Zs       OGHAM SPACE MARK
  // 180E          ; White_Space # Zs       MONGOLIAN VOWEL SEPARATOR
  // 2000..200A    ; White_Space # Zs       EN QUAD..HAIR SPACE
  // 2028          ; White_Space # Zl       LINE SEPARATOR
  // 2029          ; White_Space # Zp       PARAGRAPH SEPARATOR
  // 202F          ; White_Space # Zs       NARROW NO-BREAK SPACE
  // 205F          ; White_Space # Zs       MEDIUM MATHEMATICAL SPACE
  // 3000          ; White_Space # Zs       IDEOGRAPHIC SPACE
  //
  // BOM: 0xFEFF
  static bool _isTwoByteWhitespace(int codePoint) {
    if (codePoint < 256) return _isOneByteWhitespace(codePoint);
    return (codePoint == 0x1680) ||
        (codePoint == 0x180E) ||
        ((0x2000 <= codePoint) && (codePoint <= 0x200A)) ||
        (codePoint == 0x2028) ||
        (codePoint == 0x2029) ||
        (codePoint == 0x202F) ||
        (codePoint == 0x205F) ||
        (codePoint == 0x3000) ||
        (codePoint == 0xFEFF);
  }

  String trim() {
    final len = this.length;
    int first = 0;
    for (; first < len; first++) {
      if (!_isWhitespace(this.codeUnitAt(first))) {
        break;
      }
    }
    if (len == first) {
      // String contains only whitespaces.
      return "";
    }
    int last = len - 1;
    for (; last >= first; last--) {
      if (!_isWhitespace(this.codeUnitAt(last))) {
        break;
      }
    }
    if ((first == 0) && (last == (len - 1))) {
      // Returns this string if it does not have leading or trailing
      // whitespaces.
      return this;
    } else {
      return _substringUnchecked(first, last + 1);
    }
  }

  bool contains(Pattern pattern, [int startIndex = 0]) {
    if (pattern is String) {
      if (startIndex < 0 || startIndex > this.length) {
        throw new RangeError.range(startIndex, 0, this.length);
      }
      return indexOf(pattern, startIndex) >= 0;
    }
    return pattern.allMatches(this.substring(startIndex)).isNotEmpty;
  }

  String replaceFirst(Pattern pattern, String replacement) {
    if (pattern is! Pattern) {
      throw new ArgumentError("${pattern} is not a Pattern");
    }
    if (replacement is! String) {
      throw new ArgumentError("${replacement} is not a String");
    }
    StringBuffer buffer = new StringBuffer();
    int startIndex = 0;
    Iterator iterator = pattern.allMatches(this).iterator;
    if (iterator.moveNext()) {
      Match match = iterator.current;
      buffer..write(this.substring(startIndex, match.start))
            ..write(replacement);
      startIndex = match.end;
    }
    return (buffer..write(this.substring(startIndex))).toString();
  }

  String replaceAll(Pattern pattern, String replacement) {
    if (pattern is! Pattern) {
      throw new ArgumentError("${pattern} is not a Pattern");
    }
    if (replacement is! String) {
      throw new ArgumentError(
          "${replacement} is not a String or Match->String function");
    }
    StringBuffer buffer = new StringBuffer();
    int startIndex = 0;
    for (Match match in pattern.allMatches(this)) {
      buffer..write(this.substring(startIndex, match.start))
            ..write(replacement);
      startIndex = match.end;
    }
    return (buffer..write(this.substring(startIndex))).toString();
  }

  String replaceAllMapped(Pattern pattern, String replace(Match match)) {
    return splitMapJoin(pattern, onMatch: replace);
  }

  static String _matchString(Match match) => match[0];
  static String _stringIdentity(String string) => string;

  String _splitMapJoinEmptyString(String onMatch(Match match),
                                  String onNonMatch(String nonMatch)) {
    // Pattern is the empty string.
    StringBuffer buffer = new StringBuffer();
    int length = this.length;
    int i = 0;
    buffer.write(onNonMatch(""));
    while (i < length) {
      buffer.write(onMatch(new _StringMatch(i, this, "")));
      // Special case to avoid splitting a surrogate pair.
      int code = this.codeUnitAt(i);
      if ((code & ~0x3FF) == 0xD800 && length > i + 1) {
        // Leading surrogate;
        code = this.codeUnitAt(i + 1);
        if ((code & ~0x3FF) == 0xDC00) {
          // Matching trailing surrogate.
          buffer.write(onNonMatch(this.substring(i, i + 2)));
          i += 2;
          continue;
        }
      }
      buffer.write(onNonMatch(this[i]));
      i++;
    }
    buffer.write(onMatch(new _StringMatch(i, this, "")));
    buffer.write(onNonMatch(""));
    return buffer.toString();
  }

  String splitMapJoin(Pattern pattern,
                      {String onMatch(Match match),
                       String onNonMatch(String nonMatch)}) {
    if (pattern is! Pattern) {
      throw new ArgumentError("${pattern} is not a Pattern");
    }
    if (onMatch == null) onMatch = _matchString;
    if (onNonMatch == null) onNonMatch = _stringIdentity;
    if (pattern is String) {
      String stringPattern = pattern;
      if (stringPattern.isEmpty) {
        return _splitMapJoinEmptyString(onMatch, onNonMatch);
      }
    }
    StringBuffer buffer = new StringBuffer();
    int startIndex = 0;
    for (Match match in pattern.allMatches(this)) {
      buffer.write(onNonMatch(this.substring(startIndex, match.start)));
      buffer.write(onMatch(match).toString());
      startIndex = match.end;
    }
    buffer.write(onNonMatch(this.substring(startIndex)));
    return buffer.toString();
  }


  /**
   * Convert all objects in [values] to strings and concat them
   * into a result string.
   */
  static String _interpolate(List<String> values) {
    final numValues = values.length;
    _List stringList = new List<String>(numValues);
    bool isOneByteString = true;
    int totalLength = 0;
    for (int i = 0; i < numValues; i++) {
      var s = values[i].toString();
      if (isOneByteString && (s._cid == _OneByteString._classId)) {
        totalLength += s.length;
      } else {
        isOneByteString = false;
        if (s is! String) {
          throw new ArgumentError(s);
        }
      }
      stringList[i] = s;
    }
    if (isOneByteString) {
      return _OneByteString._concatAll(stringList, totalLength);
    }
    return _concatRangeNative(stringList, 0, stringList.length);
  }

  Iterable<Match> allMatches(String str) {
    List<Match> result = new List<Match>();
    int length = str.length;
    int patternLength = this.length;
    int startIndex = 0;
    while (true) {
      int position = str.indexOf(this, startIndex);
      if (position == -1) {
        break;
      }
      result.add(new _StringMatch(position, str, this));
      int endIndex = position + patternLength;
      if (endIndex == length) {
        break;
      } else if (position == endIndex) {
        ++startIndex;  // empty match, advance and restart
      } else {
        startIndex = endIndex;
      }
    }
    return result;
  }

  Match matchAsPrefix(String string, [int start = 0]) {
    if (start < 0 || start > string.length) {
      throw new RangeError.range(start, 0, string.length);
    }
    if (start + this.length > string.length) return null;
    for (int i = 0; i < this.length; i++) {
      if (string.codeUnitAt(start + i) != this.codeUnitAt(i)) {
        return null;
      }
    }
    return new _StringMatch(start, string, this);
  }

  List<String> split(Pattern pattern) {
    if ((pattern is String) && pattern.isEmpty) {
      List<String> result = new List<String>(this.length);
      for (int i = 0; i < this.length; i++) {
        result[i] = this[i];
      }
      return result;
    }
    int length = this.length;
    Iterator iterator = pattern.allMatches(this).iterator;
    if (length == 0 && iterator.moveNext()) {
      // A matched empty string input returns the empty list.
      return <String>[];
    }
    List<String> result = new List<String>();
    int startIndex = 0;
    int previousIndex = 0;
    while (true) {
      if (startIndex == length || !iterator.moveNext()) {
        result.add(this._substringUnchecked(previousIndex, length));
        break;
      }
      Match match = iterator.current;
      if (match.start == length) {
        result.add(this._substringUnchecked(previousIndex, length));
        break;
      }
      int endIndex = match.end;
      if (startIndex == endIndex && endIndex == previousIndex) {
        ++startIndex;  // empty match, advance and restart
        continue;
      }
      result.add(this._substringUnchecked(previousIndex, match.start));
      startIndex = previousIndex = endIndex;
    }
    return result;
  }

  List<int> get codeUnits => new _CodeUnits(this);

  Runes get runes => new Runes(this);

  String toUpperCase() native "String_toUpperCase";

  String toLowerCase() native "String_toLowerCase";

  // Concatenate ['start', 'end'[ elements of 'strings'. 'strings' must contain
  // String elements. TODO(srdjan): optimize it.
  static String _concatRange(List<String> strings, int start, int end) {
    if ((end - start) == 1) {
      return strings[start];
    }
    return _concatRangeNative(strings, start, end);
  }

  // Call this method if not all list elements are known to be OneByteString(s).
  // 'strings' must be an _List or _GrowableList.
  static String _concatRangeNative(List<String> strings, int start, int end)
      native "String_concatRange";
}


class _OneByteString extends _StringBase implements String {
  static final int _classId = "A"._cid;

  factory _OneByteString._uninstantiable() {
    throw new UnsupportedError(
        "_OneByteString can only be allocated by the VM");
  }

  int get hashCode native "String_getHashCode";

  bool _isWhitespace(int codePoint) {
    return _StringBase._isOneByteWhitespace(codePoint);
  }

  bool operator ==(Object other) {
    return super == other;
  }

  String _substringUncheckedNative(int startIndex, int endIndex)
      native "OneByteString_substringUnchecked";

  List<String> _splitWithCharCode(int charCode)
      native "OneByteString_splitWithCharCode";

  List<String> split(Pattern pattern) {
    if ((pattern._cid == _OneByteString._classId) && (pattern.length == 1)) {
      return _splitWithCharCode(pattern.codeUnitAt(0));
    }
    return super.split(pattern);
  }

  // All element of 'strings' must be OneByteStrings.
  static _concatAll(List<String> strings, int totalLength) {
    // TODO(srdjan): Improve code below and raise or eliminate the limit.
    if (totalLength > 128) {
      // Native is quicker.
      return _StringBase._concatRangeNative(strings, 0, strings.length);
    }
    var res = _OneByteString._allocate(totalLength);
    final stringsLength = strings.length;
    int rIx = 0;
    for (int i = 0; i < stringsLength; i++) {
      _OneByteString e = strings[i];
      final eLength = e.length;
      for (int s = 0; s < eLength; s++) {
        res._setAt(rIx++, e.codeUnitAt(s));
      }
    }
    return res;
  }

  int indexOf(Pattern pattern, [int start = 0]) {
    // Specialize for single character pattern.
    final pCid = pattern._cid;
    if ((pCid == _OneByteString._classId) ||
        (pCid == _TwoByteString._classId) ||
        (pCid == _ExternalOneByteString._classId)) {
      final len = this.length;
      if ((pattern.length == 1) && (start >= 0) && (start < len)) {
        final patternCu0 = pattern.codeUnitAt(0);
        if (patternCu0 > 0xFF) {
          return -1;
        }
        for (int i = start; i < len; i++) {
          if (this.codeUnitAt(i) == patternCu0) {
            return i;
          }
        }
        return -1;
      }
    }
    return super.indexOf(pattern, start);
  }

  bool contains(Pattern pattern, [int start = 0]) {
    final pCid = pattern._cid;
    if ((pCid == _OneByteString._classId) ||
        (pCid == _TwoByteString._classId) ||
        (pCid == _ExternalOneByteString._classId)) {
      final len = this.length;
      if ((pattern.length == 1) && (start >= 0) && (start < len)) {
        final patternCu0 = pattern.codeUnitAt(0);
        if (patternCu0 > 0xFF) {
          return false;
        }
        for (int i = start; i < len; i++) {
          if (this.codeUnitAt(i) == patternCu0) {
            return true;
          }
        }
        return false;
      }
    }
    return super.contains(pattern, start);
  }

  // Allocates a string of given length, expecting its content to be
  // set using _setAt.
  static _OneByteString _allocate(int length) native "OneByteString_allocate";

  // This is internal helper method. Code point value must be a valid
  // Latin1 value (0..0xFF), index must be valid.
  void _setAt(int index, int codePoint) native "OneByteString_setAt";
}


class _TwoByteString extends _StringBase implements String {
  static final int _classId = "\u{FFFF}"._cid;

  factory _TwoByteString._uninstantiable() {
    throw new UnsupportedError(
        "_TwoByteString can only be allocated by the VM");
  }

  bool _isWhitespace(int codePoint) {
    return _StringBase._isTwoByteWhitespace(codePoint);
  }

  bool operator ==(Object other) {
    return super == other;
  }
}


class _ExternalOneByteString extends _StringBase implements String {
  static final int _classId = _getCid();

  factory _ExternalOneByteString._uninstantiable() {
    throw new UnsupportedError(
        "_ExternalOneByteString can only be allocated by the VM");
  }

  bool _isWhitespace(int codePoint) {
    return _StringBase._isOneByteWhitespace(codePoint);
  }

  bool operator ==(Object other) {
    return super == other;
  }

  static int _getCid() native "ExternalOneByteString_getCid";
}


class _ExternalTwoByteString extends _StringBase implements String {
  factory _ExternalTwoByteString._uninstantiable() {
    throw new UnsupportedError(
        "_ExternalTwoByteString can only be allocated by the VM");
  }

  bool _isWhitespace(int codePoint) {
    return _StringBase._isTwoByteWhitespace(codePoint);
  }

  bool operator ==(Object other) {
    return super == other;
  }
}


class _StringMatch implements Match {
  const _StringMatch(int this.start,
                     String this.input,
                     String this.pattern);

  int get end => start + pattern.length;
  String operator[](int g) => group(g);
  int get groupCount => 0;

  String group(int group) {
    if (group != 0) {
      throw new RangeError.value(group);
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

  final int start;
  final String input;
  final String pattern;
}

/**
 * An [Iterable] of the UTF-16 code units of a [String] in index order.
 */
class _CodeUnits extends Object with ListMixin<int>,
                                     UnmodifiableListMixin<int> {
  /** The string that this is the code units of. */
  String _string;

  _CodeUnits(this._string);

  int get length => _string.length;
  int operator[](int i) => _string.codeUnitAt(i);
}
