// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ScannerTask extends CompilerTask {
  ScannerTask(Compiler compiler) : super(compiler);
  String get name() => 'Scanner';

  void scan(CompilationUnitElement compilationUnit) {
    measure(() {
      Link<Element> elements = scanElements(compilationUnit);
      for (Link<Element> link = elements; !link.isEmpty(); link = link.tail) {
        Element existing = compiler.universe.find(link.head.name);
        if (existing !== null) {
          Node node = link.head.parseNode(compiler, compiler);
          // TODO(ahe): Is this the right place to handle this?
          compiler.cancel('Duplicate definition', node: node);
        }
        compiler.universe.define(link.head);
      }
    });
  }

  Link<Element> scanElements(CompilationUnitElement compilationUnit) {
    Script script = compilationUnit.script;
    Token tokens = new StringScanner(script.text).tokenize();
    ElementListener listener = new ElementListener(compiler, compilationUnit);
    PartialParser parser = new PartialParser(listener);
    parser.parseUnit(tokens);
    return listener.topLevelElements;
  }
}
