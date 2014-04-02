// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "package:expect/expect.dart";
import "../../../sdk/lib/_internal/compiler/implementation/scanner/scannerlib.dart";
import "../../../sdk/lib/_internal/compiler/implementation/tree/tree.dart";

import "../../../sdk/lib/_internal/compiler/implementation/dart2jslib.dart"
    show DiagnosticListener,
         Script;

import "../../../sdk/lib/_internal/compiler/implementation/elements/elements.dart"
    show CompilationUnitElement,
         LibraryElement;

import "../../../sdk/lib/_internal/compiler/implementation/elements/modelx.dart"
    show CompilationUnitElementX,
         LibraryElementX;

main() {
  testClassDef();
  testClass1Field();
  testClass2Fields();
  testClass1Field1Method();
  testClass1Field2Method();
  testClassDefTypeParam();
}

testClassDef() {
  compareCode('class T{}');
}

testClass1Field() {
  compareCode('class T{var x;}');
}

testClass2Fields() {
  compareCode('class T{var x;var y;}');
}

testClass1Field1Method() {
  compareCode('class T{var x;m(){}}');
}

testClass1Field2Method() {
  compareCode('class T{a(){}b(){}}');
}

testClassDefTypeParam() {
  compareCode('class T<X>{}');
}

void compareCode(String code) {
  Expect.equals(code, doUnparse(code));
}

String doUnparse(String source) {
  MessageCollector diagnosticListener = new MessageCollector();
  Script script = new Script(null, null, null);
  LibraryElement lib = new LibraryElementX(script);
  CompilationUnitElement element = new CompilationUnitElementX(script, lib);
  StringScanner scanner = new StringScanner.fromString(source);
  Token beginToken = scanner.tokenize();
  NodeListener listener = new NodeListener(diagnosticListener, element);
  Parser parser = new Parser(listener);
  parser.parseUnit(beginToken);
  Node node = listener.popNode();
  Expect.isTrue(listener.nodes.isEmpty);
  return unparse(node);
}

class MessageCollector implements DiagnosticListener {
  List<String> messages;
  MessageCollector() {
    messages = [];
  }
  void internalError(node, String reason) {
    messages.add(reason);
    throw reason;
  }

  void log(message) {
    messages.add(message);
  }
}
