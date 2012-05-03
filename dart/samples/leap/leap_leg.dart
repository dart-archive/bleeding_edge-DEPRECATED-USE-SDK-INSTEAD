// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

compilerIsolate(port) {
  Runner runner = new Runner();
  runner.init();

  port.receive((msg, replyTo) {
    replyTo.send(runner.update(msg));
  });
}

main() {
  dom.document.getElementById('status').innerHTML = 'Initializing compiler';
  final iframe = dom.document.getElementById('isolate');
  setOutline(msg) {
    dom.document.getElementById('out').innerHTML = msg;
  }
  dom.spawnDomIsolate(iframe.contentWindow, 'compilerIsolate').then((sendPort) {
    update(_) {
      String text = dom.document.getElementById('code').innerText;
      sendPort.call(text).then(setOutline);
      dom.document.getElementById('status').innerHTML = 'Ready';
    }
    update(null);
    final code = dom.document.getElementById('code');
    code.addEventListener('DOMSubtreeModified', update);
  });
}

class Runner {
  final LeapCompiler compiler;

  Runner() : compiler = new LeapCompiler();

  String init() {
    Stopwatch sw = new Stopwatch.start();
    compiler.scanBuiltinLibraries();
    sw.stop();
    return 'Scanned core libraries in ${sw.elapsedInMs()}ms';
  }

  String update(String codeText) {
    StringBuffer sb = new StringBuffer();

    Stopwatch sw = new Stopwatch.start();

    LibraryElement e = compile(new LeapScript(codeText));

    void printFunction(FunctionElement fe, [String indentation = ""]) {
      var paramAcc = [];

      FunctionType ft = fe.computeType(compiler);

      sb.add("<div>${indentation}");
      ft.returnType.name.printOn(sb);
      sb.add(" ");
      fe.name.printOn(sb);
      sb.add("(");
      ft.parameterTypes.printOn(sb, ", ");
      sb.add(");</div>");

    }

    void printField(FieldElement fe, [String indentation = ""]) {
      sb.add("<div>${indentation}var ");
      fe.name.printOn(sb);
      sb.add(";</div>");
    }

    void printClass(ClassElement ce) {
      ce.parseNode(compiler);

      sb.add("<div>class ");
      ce.name.printOn(sb);
      sb.add(" {");

      for (Element e in ce.members.reverse()) {
        switch(e.kind) {
        case ElementKind.FUNCTION:

          printFunction(e, "&nbsp; ");
          break;

        case ElementKind.FIELD:

          printField(e, "&nbsp; ");
          break;
        }
      }
      sb.add("}</div>");
    }

    for (Element c in e.topLevelElements.reverse()) {
      switch (c.kind) {
      case ElementKind.FUNCTION:
        printFunction (c);
        break;

      case ElementKind.CLASS:
        printClass(c);
        break;

      case ElementKind.FIELD:
        printField (c);
        break;
      }
    }

    compiler.log("Outline ${sw.elapsedInMs()}");
    return sb.toString();
  }

  Element compile(String script) {
    return compiler.runSelective(script);
  }
}

class LeapCompiler extends Compiler {
  HTTPFileSystem fs;

  final bool throwOnError = false;

  final libDir = "../..";

  LeapCompiler() : super() {
    fs = new HTTPFileSystem();
    tasks = [scanner, dietParser, parser, resolver, checker];
  }

  void log(message) { print(message); }

  String get legDirectory() => libDir;

  LibraryElement scanBuiltinLibrary(String path) {
    Uri base = new Uri.fromString(dom.window.location.toString());
    Uri libraryRoot = base.resolve(libDir);
    Uri resolved = libraryRoot.resolve(DART2JS_LIBRARY_MAP[path]);
    LibraryElement library = scanner.loadLibrary(resolved, null);
    return library;
  }

  currentScript() {
    if (currentElement === null) return null;
    CompilationUnitElement compilationUnit =
      currentElement.getCompilationUnit();
    if (compilationUnit === null) return null;
    return compilationUnit.script;
  }

  Script readScript(Uri uri, [ScriptTag node]) {
    String text = "";
    try {
      text = fs.readAll(uri.path.toString());
    } catch (var exception) {
      cancel("${uri.path}: $exception", node: node);
    }
    SourceFile sourceFile = new SourceFile(uri.toString(), text);
    return new Script(uri, sourceFile);
  }

  reportWarning(Node node, var message) {
    print(message);
  }

  reportError(Node node, var message) {
    cancel(message.toString(), node);
  }

  void cancel([String reason, Node node, token, instruction, element]) {
    print(reason);
  }

  Element runSelective(Script script) {
    Stopwatch sw = new Stopwatch.start();
    Element e;
    try {
      e = runCompilerSelective(script);
    } catch (CompilerCancelledException exception) {
      log(exception.toString());
      log('compilation failed');
      return null;
    }
    log('compilation succeeded: ${sw.elapsedInMs()}ms');
    return e;
  }

  LibraryElement runCompilerSelective(Script script) {
    mainApp = new LibraryElement(script);

    universe.libraries.remove(script.uri.toString());
    Element element;
    withCurrentElement(mainApp, () {
        scanner.scan(mainApp);
      });
    return mainApp;
  }
}
