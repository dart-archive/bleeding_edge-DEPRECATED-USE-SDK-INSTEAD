// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A benchmark for the Dart parser.
 */
class ParserBench extends BaseParserBench {
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
    Listener listener = new BenchListener();
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
    final int iterations = 500;
    ProgressBar bar = new ProgressBar(iterations);
    bar.begin();
    for (int i = 0; i < iterations; i++) {
      bar.tick();
      parseAll(arguments);
    }
    bar.end();
    for (int i = 0; i < 10; i++) {
      timedParseAll(arguments);
    }
  }
}

main() {
  new ParserBench().main(argv);
}

class BenchListener extends Listener {
  int aliasCount = 0;
  int classCount = 0;
  int interfaceCount = 0;
  int libraryTagCount = 0;
  int topLevelMemberCount = 0;

  void beginTopLevelMember(Token token) {
    topLevelMemberCount++;
  }

  void beginLibraryTag(Token token) {
    libraryTagCount++;
  }

  void beginInterface(Token token) {
    interfaceCount++;
  }

  void beginClass(Token token) {
    classCount++;
  }

  void beginFunctionTypeAlias(Token token) {
    aliasCount++;
  }
}
