// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Mocks of classes and interfaces that Leg cannot read directly.

// TODO(ahe): Remove this file.

class JSSyntaxRegExp implements RegExp {
  JSSyntaxRegExp(String pattern,
                 [bool multiLine = false,
                  bool ignoreCase = false]) {
    throw 'JSSyntaxRegExp is not implemented';
  }
}

class StringBufferImpl {
  StringBufferImpl() {
    throw 'StringBufferImpl is not implemented';
  }
}

class DateImplementation {}
class ReceivePortFactory {}

class StringBase {
  static String createFromCharCodes(List<int> charCodes) {
    throw "StringBase.createFromCharCodes is not implemented";
  }

  static String join(List<String> strings, String separator) {
    throw "StringBase.join is not implemented";
  }

  static String concatAll(List<String> strings) {
    throw "StringBase.concatAll is not implemented";
  }
}

class MathNatives {
  static int parseInt(String str) {
    throw 'MathNatives.parseInt is not implemented';
  }

  static double parseDouble(String str) {
    throw 'MathNatives.parseDouble is not implemented';
  }

  static double sqrt(num value) {
    throw 'MathNatives.sqrt is not implemented';
  }

  static double sin(num value) {
    throw 'MathNatives.sin is not implemented';
  }

  static double cos(num value) {
    throw 'MathNatives.cos is not implemented';
  }

  static double tan(num value) {
    throw 'MathNatives.tan is not implemented';
  }

  static double acos(num value) {
    throw 'MathNatives.acos is not implemented';
  }

  static double asin(num value) {
    throw 'MathNatives.asin is not implemented';
  }

  static double atan(num value) {
    throw 'MathNatives.atan is not implemented';
  }

  static double atan2(num a, num b) {
    throw 'MathNatives.atan2 is not implemented';
  }

  static double exp(num value) {
    throw 'MathNatives.exp is not implemented';
  }

  static double log(num value) {
    throw 'MathNatives.log is not implemented';
  }

  static num pow(num value, num exponent) {
    throw 'MathNatives.pow is not implemented';
  }

  static double random() {
    throw 'MathNatives.random is not implemented';
  }
}
