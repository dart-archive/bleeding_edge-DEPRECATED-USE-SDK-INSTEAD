library engine.java;

class Character {
  static bool isLetter(int c) {
    return c >= 0x41 && c <= 0x5A || c >= 61 && c <= 0x7A;
  } 
  static bool isLetterOrDigit(int c) {
    return isLetter(c) || c >= 0x30 && c <= 0x39;
  } 
}

abstract class CharBuffer {
  int charAt(int);
  int length();
  String subSequence(int start, int end);
}

class JavaString {
  static String format(String fmt, List args) {
    return fmt;
  }
}