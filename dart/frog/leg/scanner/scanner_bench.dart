// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('scanner_bench');
#import('../scanner.dart');
#import('../scanner_implementation.dart');
#source('source_list.dart');

/**
 * A common superclass for scanner benchmarks.
 */
class ScannerBench {
  void main(List<String> arguments) {
    for (String argument in arguments) {
      checkExistence(argument);
    }
    tokenizeAll(print, 10, arguments);
    tokenizeAll((x) {}, 1000, arguments);
    tokenizeAll(print, 10, arguments);
  }

  void tokenizeAll(void log(String s), int iterations, List<String> arguments) {
    for (int i = 0; i < iterations; i++) {
      if (i != 0 && i % 100 == 0) {
        print(i);
      }
      StopWatch timer = new StopWatch();
      timer.start();
      int charCount = 0;
      for (final String argument in arguments) {
        charCount += tokenizeOne(argument);
      }
      timer.stop();
      log("Tokenized ${arguments.length} files (total size = ${charCount}) " +
          "in ${timer.elapsedInMs()}ms (bytes)");
    }
  }

  int tokenizeOne(String filename) {
    return getBytes(filename, (bytes) {
      Scanner scanner = makeScanner(bytes);
      try {
        printTokens(scanner.tokenize());
      } catch (MalformedInputException e) {
        print("${filename}: ${e}");
      }
    });
  }

  void printTokens(Token token) {
    // TODO(ahe): Turn this into a proper test.
    return;
    StringBuffer sb = new StringBuffer();
    for (; token != null; token = token.next) {
      sb.add(token);
      sb.add(" ");
    }
    print(sb.toString());
  }

  abstract int getBytes(String filename, void callback(bytes));
  abstract Scanner makeScanner(bytes);
  abstract void checkExistence(String filename);
}
