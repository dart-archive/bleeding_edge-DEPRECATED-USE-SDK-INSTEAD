// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('parser');

#import('../scanner/scanner_implementation.dart');
#import('../scanner/scannerlib.dart');

#source('../../source.dart');
#source('../scanner/byte_strings.dart');
#source('../scanner/byte_array_scanner.dart');

// Hack to allow satisfy sourcing in ../source.dart.
var world;

void main() {
  Stopwatch stopwatch = new Stopwatch();
  stopwatch.start();
  List<String> filenames = new Options().arguments;
  bool diet = false;
  bool throwOnError = false;
  bool scanOnly = false;
  bool readOnly = false;
  bool decodeOnly = false;
  int charCount = 0;
  for (String filename in filenames) {
    if (filename == "--diet") {
      diet = true;
      continue;
    }
    if (filename == "--throw") {
      throwOnError = true;
      continue;
    }
    if (filename == "--scan-only") {
      scanOnly = true;
      continue;
    }
    if (filename == "--read-only") {
      readOnly = true;
      continue;
    }
    if (filename == "--decode-only") {
      decodeOnly = true;
      continue;
    }
    List<int> bytes = read(filename);
    if (readOnly) continue;
    String source = new String.fromCharCodes(bytes);
    if (decodeOnly) continue;
    charCount += source.length;
    SourceFile file = new SourceFile(filename, source);
    Listener listener = new MyListener(file);
    Parser parser = diet ? new PartialParser(listener) : new Parser(listener);
    try {
      Token token = scan(file);
      if (!scanOnly) parser.parseUnit(token);
    } catch (ParserError ex) {
      if (throwOnError) {
        throw;
      } else {
        print(ex);
      }
    }
  }
  stopwatch.stop();
  int kb = (charCount/1024).round().toInt();
  String stats =
      '$classCount classes (${kb}Kb) in ${stopwatch.elapsedInMs()}ms';
  if (errorCount != 0) {
    stats += ' with $errorCount errors';
  }
  if (diet) {
    print('Diet parsed $stats.');
  } else {
    print('Parsed $stats.');
  }
}

Token scan(SourceFile source) {
  Scanner scanner = new StringScanner(source.text);
  try {
    return scanner.tokenize();
  } catch (MalformedInputException ex) {
    var message = ex.message;
    if (message is !String) message = "unexpected character";
    Token fakeToken = new Token($QUESTION, scanner.charOffset);
    new MyListener(source).error(message, fakeToken);
    throw;
  }
}

List<int> read(String filename) {
  File file = new File(filename);
  file.openSync();
  bool threw = true;
  try {
    int size = file.lengthSync();
    List<int> bytes = new List<int>(size + 1);
    file.readListSync(bytes, 0, size);
    bytes[size] = $EOF;
    threw = false;
    return bytes;
  } finally {
    try {
      file.closeSync();
    } catch (var ex) {
      if (!threw) throw;
    }
  }
}

int classCount = 0;
int errorCount = 0;

class MyListener extends Listener {
  final SourceFile file;

  MyListener(this.file);

  void beginClassDeclaration(Token token) {
    classCount++;
  }

  void error(String message, Token token) {
    ++errorCount;
    if (token !== null) {
      String tokenString = token.toString();
      int begin = token.charOffset;
      int end = begin + tokenString.length;
      throw new ParserError(file.getLocationMessage(message, begin, end, true));
    }
    throw new ParserError(message);
  }
}
