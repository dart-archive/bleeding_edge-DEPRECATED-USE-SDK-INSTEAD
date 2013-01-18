library engine.java;

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