library engine.java;

class System {
  static int currentTimeMillis() {
    return (new Date.now()).millisecondsSinceEpoch;
  }
}

class Character {
  static const int MAX_VALUE = 0xffff;
  static const int MAX_CODE_POINT = 0x10ffff;
  static bool isLetter(int c) {
    return c >= 0x41 && c <= 0x5A || c >= 0x61 && c <= 0x7A;
  } 
  static bool isLetterOrDigit(int c) {
    return isLetter(c) || c >= 0x30 && c <= 0x39;
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
  }
  static String toChars(int codePoint) {
    throw new UnsupportedOperationException();
  }
}

class CharBuffer {
  final String _content;
  CharBuffer(this._content);
  int charAt(int index) => _content.charCodeAt(index);
  int length() => _content.length;
  String subSequence(int start, int end) => _content.substring(start, end);
}

class JavaString {
  static String format(String fmt, List args) {
    return fmt;
  }
}

abstract class PrintWriter {
  void print(x);
}

class PrintStringWriter extends PrintWriter {
  final StringBuffer _sb = new StringBuffer();
  
  void print(x) {
    _sb.add(x);
  }
  
  void println() {
    print('\n');
  }
  
  String toString() => _sb.toString(); 
}

class StringUtils {
  static List<String> split(String s, String pattern) => s.split(pattern);
  static String replace(String s, String from, String to) => s.replaceAll(from, to);
  static String repeat(String s, int n) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < n; i++) {
      sb.add(s);
    }
    return sb.toString();
  }
}

class IllegalStateException implements Exception {
  final String message;
  const IllegalStateException([this.message = ""]);
  String toString() => "IllegalStateException: $message";
}

class UnsupportedOperationException implements Exception {
  String toString() => "UnsupportedOperationException";
}

class NumberFormatException implements Exception {
  String toString() => "NumberFormatException";
}

class ListWrapper<E> extends Collection<E> implements List<E> {
  List<E> elements = new List<E>();
  
  Iterator<E> get iterator {
    return elements.iterator;
  }
  
  E operator [](int index) {
    return elements[index];
  }

  void operator []=(int index, E value) {
    elements[index] = value;
  }

  void set length(int newLength) {
    elements.length = newLength;
  }

  void add(E value) {
    elements.add(value);
  }

  void addLast(E value) {
    elements.addLast(value);
  }

  void addAll(Iterable<E> iterable) {
    elements.addAll(iterable);
  }

  void sort([int compare(E a, E b)]) {
    elements.sort(compare);
  }

  int indexOf(E element, [int start = 0]) {
    return elements.indexOf(element, start);
  }

  int lastIndexOf(E element, [int start]) {
    return elements.lastIndexOf(element, start);
  }

  void clear() {
    elements.clear();
  }

  void remove(Object element) {
    return elements.remove(element);
  }

  E removeAt(int index) {
    return elements.removeAt(index);
  }

  E removeLast() {
    return elements.removeLast();
  }

  List<E> get reversed => elements.reversed;

  List<E> getRange(int start, int length) {
    return elements.getRange(start, length);
  }

  void setRange(int start, int length, List<E> from, [int startFrom]) {
    elements.setRange(start, length, from, startFrom);
  }

  void removeRange(int start, int length) {
    elements.removeRange(start, length);
  }

  void insertRange(int start, int length, [E fill]) {
    elements.insertRange(start, length, fill);
  }
}
