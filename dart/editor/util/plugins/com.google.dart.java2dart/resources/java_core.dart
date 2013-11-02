library java.core;

import "dart:math" as math;

class JavaSystem {
  static int currentTimeMillis() {
    return (new DateTime.now()).millisecondsSinceEpoch;
  }

  static void arraycopy(List src, int srcPos, List dest, int destPos, int length) {
    for (int i = 0; i < length; i++) {
      dest[destPos + i] = src[srcPos + i];
    }
  }
}

/**
 * Limited implementation of "o is instanceOfType", see
 * http://code.google.com/p/dart/issues/detail?id=8184
 */
bool isInstanceOf(o, Type t) {
  if (o == null) {
    return false;
  }
  if (o.runtimeType == t) {
    return true;
  }
  String oTypeName = o.runtimeType.toString();
  String tTypeName = t.toString();
  if (oTypeName == tTypeName) {
    return true;
  }
  if (oTypeName.startsWith("List") && tTypeName == "List") {
    return true;
  }
  if (tTypeName == "Map" && o is Map) {
    return true;
  }
  // Dart Analysis Engine specific
  if (oTypeName == "${tTypeName}Impl") {
    return true;
  }
  if (tTypeName == "FormalParameter") {
    return
        oTypeName == "DefaultFormalParameter" ||
        oTypeName == "FieldNormalParameter" ||
        oTypeName == "FunctionTypedFormalParameter" ||
        oTypeName == "SimpleFormalParameter";
  }
  if (tTypeName == "MethodElement") {
    if (oTypeName == "MethodMember") {
      return true;
    }
  }
  if (tTypeName == "ExecutableElement") {
    if (oTypeName == "MethodElementImpl" ||
        oTypeName == "FunctionElementImpl" ||
        oTypeName == "PropertyAccessorElementImpl") {
      return true;
    }
  }
  if (tTypeName == "ParameterElement") {
    if (oTypeName == "FieldFormalParameterElementImpl" ||
        oTypeName == "DefaultFieldFormalParameterElementImpl" ||
        oTypeName == "DefaultParameterElementImpl") {
      return true;
    }
  }
  if (tTypeName == "VariableElement") {
    if (oTypeName == "LocalVariableElementImpl" ||
        oTypeName == "ConstLocalVariableElementImpl" ||
        oTypeName == "FieldElementImpl" ||
        oTypeName == "ConstFieldElementImpl" ||
        oTypeName == "TopLevelVariableElementImpl" ||
        oTypeName == "ConstTopLevelVariableElementImpl") {
      return true;
    }
  }
  // no
  return false;
}

class JavaArrays {
  static bool equals(List a, List b) {
    if (a.length != b.length) {
      return false;
    }
    var len = a.length;
    for (int i = 0; i < len; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }
    return true;
  }
  static int makeHashCode(List a) {
    if (a == null) {
      return 0;
    }
    int result = 1;
    for (var element in a) {
      result = 31 * result + (element == null ? 0 : element.hashCode);
    }
    return result;
  }
  static List asList(List list) => list;
}

class Character {
  static const int MAX_VALUE = 0xffff;
  static const int MAX_CODE_POINT = 0x10ffff;
  static const int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
  static const int MIN_LOW_SURROGATE  = 0xDC00;
  static const int MIN_HIGH_SURROGATE = 0xD800;
  static bool isLetter(int c) {
    return c >= 0x41 && c <= 0x5A || c >= 0x61 && c <= 0x7A;
  }
  static bool isLetterOrDigit(int c) {
    return isLetter(c) || c >= 0x30 && c <= 0x39;
  }
  static bool isWhitespace(int c) {
    return c == 0x09 || c == 0x20 || c == 0x0A || c == 0x0D;
  }
  static int digit(int codePoint, int radix) {
    if (radix != 16) {
      throw new ArgumentError("only radix == 16 is supported");
    }
    if (0x30 <= codePoint && codePoint <= 0x39) {
      return codePoint - 0x30;
    }
    if (0x41 <= codePoint && codePoint <= 0x46) {
      return 0xA + (codePoint - 0x41);
    }
    if (0x61 <= codePoint && codePoint <= 0x66) {
      return 0xA + (codePoint - 0x61);
    }
    return -1;
  }
  static String toChars(int codePoint) {
    if (codePoint < 0 || codePoint > MAX_CODE_POINT) {
      throw new IllegalArgumentException();
    }
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
      return new String.fromCharCode(codePoint);
    }
    int offset = codePoint - MIN_SUPPLEMENTARY_CODE_POINT;
    int c0 = ((offset & 0x7FFFFFFF) >> 10) + MIN_HIGH_SURROGATE;
    int c1 = (offset & 0x3ff) + MIN_LOW_SURROGATE;
    return new String.fromCharCodes([c0, c1]);
  }
}

class CharSequence {
  final String _content;
  CharSequence(this._content);
  static CharSequence wrap(String content) => new CharBuffer(content);
  int charAt(int index) => _content.codeUnitAt(index);
  int length() => _content.length;
  String subSequence(int start, int end) => _content.substring(start, end);
}

class CharBuffer extends CharSequence {
  CharBuffer(String content) : super(content);
  static CharBuffer wrap(String content) => new CharBuffer(content);
}

class JavaString {
  static String format(String fmt, List args) {
    var index = 0;
    return fmt.replaceAllMapped(new RegExp(r'%(.)'), (match) {
      switch (match.group(1)) {
        case '%': return '%';
        case 'd':
        case 's':
          if (index >= args.length) {
            throw new MissingFormatArgumentException(match.group(0));
          }
          return args[index++].toString();
        default: return match.group(1);
      }
    });
  }
  static int indexOf(String target, String str, int fromIndex) {
    if (fromIndex > target.length) return -1;
    if (fromIndex < 0) fromIndex = 0;
    return target.indexOf(str, fromIndex);
  }
  static int lastIndexOf(String target, String str, int fromIndex) {
    if (fromIndex > target.length) return -1;
    if (fromIndex < 0) fromIndex = 0;
    return target.lastIndexOf(str, fromIndex);
  }
  static bool startsWithBefore(String s, String other, int start) {
    return s.indexOf(other, start) != -1;
  }
}

/**
 * Very limited printf implementation, supports only %s and %d.
 */
String _printf(String fmt, List args) {
  StringBuffer sb = new StringBuffer();
  bool markFound = false;
  int argIndex = 0;
  for (int i = 0; i < fmt.length; i++) {
    int c = fmt.codeUnitAt(i);
    if (c == 0x25) {
      if (markFound) {
        sb.writeCharCode(c);
        markFound = false;
      } else {
        markFound = true;
      }
      continue;
    }
    if (markFound) {
      markFound = false;
      // %d
      if (c == 0x64) {
        sb.writeCharCode(args[argIndex++]);
        continue;
      }
      // %s
      if (c == 0x73) {
        sb.writeCharCode(args[argIndex++]);
        continue;
      }
      // unknown
      throw new IllegalArgumentException('[$fmt][$i] = 0x${c.toRadixString(16)}');
    } else {
      sb.writeCharCode(c);
    }
  }
  return sb.toString();
}

abstract class PrintWriter {
  void print(x);

  void newLine() {
    this.print('\n');
  }

  void println(String s) {
    this.print(s);
    this.newLine();
  }

  void printf(String fmt, List args) {
    this.print(_printf(fmt, args));
  }
}

class PrintStringWriter extends PrintWriter {
  final StringBuffer _sb = new StringBuffer();

  void print(x) {
    _sb.write(x);
  }

  String toString() => _sb.toString();
}

class StringUtils {
  static List<String> split(String s, String pattern) => s.split(pattern);
  static String replace(String s, String from, String to) => s.replaceAll(from, to);
  static String repeat(String s, int n) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < n; i++) {
      sb.write(s);
    }
    return sb.toString();
  }
}

class Math {
  static num max(num a, num b) => math.max(a, b);
  static num min(num a, num b) => math.min(a, b);
}

class RuntimeException extends JavaException {
  RuntimeException({String message: "", Exception cause: null}) :
    super(message, cause);
}

class JavaException implements Exception {
  final String message;
  final Exception cause;
  JavaException([this.message = "", this.cause = null]);
  JavaException.withCause(this.cause) : message = null;
  String toString() => "${runtimeType}: $message $cause";
}

class JavaIOException extends JavaException {
  JavaIOException([message = "", cause = null]) : super(message, cause);
}

class IllegalArgumentException extends JavaException {
  IllegalArgumentException([message = "", cause = null]) : super(message, cause);
}

class StringIndexOutOfBoundsException extends JavaException {
  StringIndexOutOfBoundsException(int index) : super('$index');
}

class IllegalStateException extends JavaException {
  IllegalStateException([message = ""]) : super(message);
}

class UnsupportedOperationException extends JavaException {
  String toString() => "UnsupportedOperationException";
}

class NumberFormatException extends JavaException {
  String toString() => "NumberFormatException";
}

/// Parses given string to [Uri], throws [URISyntaxException] if invalid.
Uri parseUriWithException(String str) {
  Uri uri = Uri.parse(str);
  if (uri.path.isEmpty) {
    throw new URISyntaxException();
  }
  return uri;
}

class URISyntaxException implements Exception {
  String toString() => "URISyntaxException";
}

class MissingFormatArgumentException implements Exception {
  final String s;

  String toString() => "MissingFormatArgumentException: $s";

  MissingFormatArgumentException(this.s);
}

class JavaIterator<E> {
  Iterable<E> _iterable;
  List<E> _elements = new List<E>();
  int _coPos = 0;
  int _elPos = 0;
  E _current = null;
  JavaIterator(this._iterable) {
    Iterator iterator = _iterable.iterator;
    while (iterator.moveNext()) {
      _elements.add(iterator.current);
    }
  }

  bool get hasNext {
    return _elPos < _elements.length;
  }

  E next() {
    _current = _elements[_elPos];
    _coPos++;
    _elPos++;
    return _current;
  }

  void remove() {
    if (_iterable is List) {
      _coPos--;
      (_iterable as List).remove(_coPos);
    } else if (_iterable is Set) {
      (_iterable as Set).remove(_current);
    } else {
      throw new StateError("Unsupported iterable ${_iterable.runtimeType}");
    }
  }
}

class MapEntry<K, V> {
  final Map<K, V> _map;
  final K _key;
  V _value;
  MapEntry(this._map, this._key, this._value);
  K getKey() => _key;
  V getValue() => _value;
  V setValue(V v) {
    V prevValue = _value;
    _value = v;
    _map[_key] = v;
    return prevValue;
  }
}

Iterable<MapEntry> getMapEntrySet(Map m) {
  List<MapEntry> result = [];
  m.forEach((k, v) {
    result.add(new MapEntry(m, k, v));
  });
  return result;
}

javaListSet(List list, int index, newValue) {
  var oldValue = list[index];
  list[index] = newValue;
  return oldValue;
}

bool javaSetAdd(Set s, o) {
  if (!s.contains(o)) {
    s.add(o);
    return true;
  }
  return false;
}

bool javaCollectionContainsAll(Iterable list, Iterable c) {
  return c.fold(true, (bool prev, e) => prev && list.contains(e));
}

javaMapPut(Map target, key, value) {
  var oldValue = target[key];
  target[key] = value;
  return oldValue;
}

void javaMapPutAll(Map target, Map source) {
  source.forEach((k, v) {
    target[k] = v;
  });
}

bool javaStringEqualsIgnoreCase(String a, String b) {
  return a.toLowerCase() == b.toLowerCase();
}

bool javaStringRegionMatches(String t, int toffset, String o, int ooffset, int len) {
  if (toffset < 0) return false;
  if (ooffset < 0) return false;
  var tend = toffset + len;
  var oend = ooffset + len;
  if (tend > t.length) return false;
  if (oend > o.length) return false;
  return t.substring(toffset, tend) == o.substring(ooffset, oend);
}

bool javaBooleanOr(bool a, bool b) {
  return a || b;
}

bool javaBooleanAnd(bool a, bool b) {
  return a && b;
}

class JavaStringBuilder {
  StringBuffer sb = new StringBuffer();
  String toString() => sb.toString();
  JavaStringBuilder append(x) {
    sb.write(x);
    return this;
  }
  JavaStringBuilder appendChar(int c) {
    sb.writeCharCode(c);
    return this;
  }
  int get length => sb.length;
  void set length(int newLength) {
    if (newLength < 0) {
      throw new StringIndexOutOfBoundsException(newLength);
    }
    if (sb.length < newLength) {
      while (sb.length < newLength) {
        sb.writeCharCode(0);
      }
    } else if (sb.length > newLength) {
      var s = sb.toString().substring(0, newLength);
      sb = new StringBuffer(s);
    }
  }
  void clear() {
    sb = new StringBuffer();
  }
}

abstract class Enum<E extends Enum> implements Comparable<E> {
  /// The name of this enum constant, as declared in the enum declaration.
  final String name;
  /// The position in the enum declaration.
  final int ordinal;
  Enum(this.name, this.ordinal);
  int get hashCode => ordinal;
  String toString() => name;
  int compareTo(E other) => ordinal - other.ordinal;
}

class JavaPatternMatcher {
  Iterator<Match> _matches;
  Match _match;
  JavaPatternMatcher(RegExp re, String input) {
    _matches = re.allMatches(input).iterator;
  }
  bool find() {
    if (!_matches.moveNext()) {
      return false;
    }
    _match = _matches.current;
    return true;
  }
  String group(int i) => _match[i];
  int start() => _match.start;
  int end() => _match.end;
}
