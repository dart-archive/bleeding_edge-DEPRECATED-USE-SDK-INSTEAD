library engine.java;

class System {
  static int currentTimeMillis() {
    return (new Date.now()).millisecondsSinceEpoch;
  }
}

class Character {
  static bool isLetter(int c) {
    return c >= 0x41 && c <= 0x5A || c >= 61 && c <= 0x7A;
  } 
  static bool isLetterOrDigit(int c) {
    return isLetter(c) || c >= 0x30 && c <= 0x39;
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
  
  String toString() => _sb.toString(); 
}

class IllegalStateException implements Exception {
  final String message;
  const IllegalStateException([this.message = ""]);
  String toString() => "IllegalStateException: $message";
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
