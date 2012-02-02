// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ScannerTask extends CompilerTask {
  ScannerTask(Compiler compiler) : super(compiler);
  String get name() => 'Scanner';

  void scan(CompilationUnitElement compilationUnit) {
    measure(() {
      scanElements(compilationUnit);
      if (compilationUnit.kind === ElementKind.LIBRARY) {
        processScriptTags(compilationUnit);
      }
    });
  }

  void processScriptTags(LibraryElement library) {
    for (ScriptTag tag in library.tags) {
      compiler.reportWarning(tag, "library tags are not implemented");
    }
    if (library !== compiler.coreLibrary) {
      importLibrary(library, compiler.coreLibrary, null);
    }
  }

  void scanElements(CompilationUnitElement compilationUnit) {
    compiler.log("scanning $compilationUnit");
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

  void importLibrary(LibraryElement library, LibraryElement imported,
                     LiteralString prefix) {
    if (prefix !== null) {
      withCurrentElement(library, () {
          compiler.cancel("prefixes are not implemented", node: prefix);
        });
    } else {
      for (Link<Element> link = imported.topLevelElements; !link.isEmpty();
           link = link.tail) {
        compiler.withCurrentElement(link.head, () {
            library.define(link.head, compiler);
          });
      }
    }
  }
}
