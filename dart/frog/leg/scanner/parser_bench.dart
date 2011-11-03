// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A benchmark for the Dart parser.
 */
class ParserBench extends vm.VmScannerBench {
  Token scanFileNamed(String filename) {
    Token token;
    getBytes(filename, (bytes) {
      Scanner scanner = makeScanner(bytes);
      try {
        token = scanner.tokenize();
      } catch (MalformedInputException e) {
        print("${filename}: ${e}");
      }
    });
    return token;
  }

  void timedParseAll(List<String> arguments) {
    StopWatch timer = new StopWatch();
    timer.start();
    Listener listener = parseAll(arguments);
    timer.stop();
    print("Parsing (${listener.libraryTagCount} tags, "
          + "${listener.classCount} classes, "
          + "${listener.interfaceCount} interfaces, "
          + "${listener.aliasCount} typedefs, "
          + "${listener.topLevelMemberCount} top-level members) "
          + "took ${timer.elapsedInMs()}ms");
  }

  Listener parseAll(List<String> arguments) {
    Listener listener = new Listener(new BenchCanceler());
    for (String argument in arguments) {
      parseFileNamed(argument, listener);
    }
    return listener;
  }

  void parseFileNamed(String argument, Listener listener) {
    bool failed = true;
    try {
      Parser parser = new Parser(listener);
      parser.parseUnit(scanFileNamed(argument));
      failed = false;
    } finally {
      if (failed) print('Error in ${argument}');
    }
  }

  void main(List<String> arguments) {
    for (int i = 0; i < 10; i++) {
      timedParseAll(arguments);
    }
    for (int i = 0; i < 500; i++) {
      if (i != 0 && i % 100 == 0) {
        print(i);
      }
      parseAll(arguments);
    }
    for (int i = 0; i < 10; i++) {
      timedParseAll(arguments);
    }
  }
}

main() {
  new ParserBench().main(argv);
}

class BenchCanceler implements Canceler {
  void cancel([String reason]) {
    throw reason;
  }
}
