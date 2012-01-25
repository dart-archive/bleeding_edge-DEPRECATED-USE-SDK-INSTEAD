// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ScannerTask extends CompilerTask {
  ScannerTask(Compiler compiler) : super(compiler);
  String get name() => 'Scanner';

  void scan(CompilationUnitElement compilationUnit) {
    measure(() {
      scanElements(compilationUnit);
      for (Element element in compilationUnit.topLevelElements) {
        compiler.universe.define(element, compiler);
      }
      for (ScriptTag tag in compilationUnit.tags) {
        compiler.reportWarning(tag, "library tags are not implemented");
      }
    });
  }

  void scanElements(CompilationUnitElement compilationUnit) {
    Script script = compilationUnit.script;
    Token tokens;
    try {
      tokens = new StringScanner(script.text).tokenize();
    } catch (MalformedInputException ex) {
      Token token;
      var message;
      if (ex.position is num) {
        // TODO(ahe): Always use tokens in MalformedInputException.
        token = new Token(EOF_INFO, ex.position);
      } else {
        token = ex.position;
      }
      compiler.cancel(ex.message, token: token);
    }
    ElementListener listener = new ElementListener(compiler, compilationUnit);
    PartialParser parser = new PartialParser(listener);
    parser.parseUnit(tokens);
  }
}
