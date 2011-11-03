// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ScannerTask extends CompilerTask {
  ScannerTask(Compiler compiler) : super(compiler);
  String get name() => 'Scanner';

  void scan(Script script) {
    measure(() {
      Link<Element> elements = scanElements(script.text);
      for (Link<Element> link = elements; !link.isEmpty(); link = link.tail) {
        compiler.universe.define(link.head);
      }
    });
  }

  Link<Element> scanElements(String text) {
    Token tokens = new StringScanner(text).tokenize();
    Listener listener = new Listener(compiler);
    Parser parser = new Parser(listener);
    parser.parseUnit(tokens);
    return listener.topLevelElements;
  }
}
