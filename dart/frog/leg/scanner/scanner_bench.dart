// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('scanner_bench');
#import('scannerlib.dart');
#import('scanner_implementation.dart');
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
    ProgressBar bar = new ProgressBar(iterations);
    bar.begin();
    for (int i = 0; i < iterations; i++) {
      bar.tick();
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
    bar.end();
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

class ProgressBar {
  static final String hashes = "##############################################";
  static final String spaces = "                                              ";

  final String esc;
  final String up;
  final String clear;
  final int total;
  int ticks = 0;

  ProgressBar(int total) : this.escape(total, new String.fromCharCodes([27]));

  ProgressBar.escape(this.total, int esc)
    : esc = esc, up = "$esc[1A", clear = "$esc[K";

  void begin() {
    if (total > 10) {
      print("[$spaces] 0%");
      print("$up[${hashes.substring(0, ticks * spaces.length ~/ total)}");
    }
  }

  void tick() {
    if (total > 10 && ticks % 5 == 0) {
      print("$up$clear[$spaces] ${ticks * 100 ~/ total}%");
      print("$up[${hashes.substring(0, ticks * spaces.length ~/ total)}");
    }
    ++ticks;
  }

  void end() {
    if (total > 10) {
      print("$up$clear[$hashes] 100%");
    }
  }
}