// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('mock_compiler');

#import("../../../leg/leg.dart");
#import("../../../leg/elements/elements.dart");
#import("../../../leg/tree/tree.dart");
#import("../../../leg/util/util.dart");
#import("parser_helper.dart");

class WarningMessage {
  Node node;
  Message message;
  WarningMessage(this.node, this.message);
}

final String DEFAULT_CORELIB = @'''
  lt() {} add(var a, var b) {} sub() {} mul() {} div() {} tdiv() {} mod() {}
  neg() {} shl() {} shr() {} eq() {} le() {} gt() {} ge() {}
  or() {} and() {} not() {} print(var obj) {}
  guard$num(x) { return true; }
  class int {}
  class double {}
  class bool {}
  class String {}
  class Object {}''';

class MockCompiler extends Compiler {
  List warnings;
  List errors;
  Node parsedTree;

  MockCompiler([String corelib = DEFAULT_CORELIB])
      : super(null), warnings = [], errors = [] {
    parseScript(corelib);
  }

  void reportWarning(Node node, var message) {
    warnings.add(new WarningMessage(node, message.message));
  }

  void reportError(Node node, var message) {
    errors.add(new WarningMessage(node, message.message));
  }

  void clearWarnings() {
    warnings = [];
  }

  void clearErrors() {
    errors = [];
  }

  TreeElements resolveStatement(String text) {
    parsedTree = parseStatement(text);
    return resolveNodeStatement(parsedTree);
  }

  TreeElements resolveNodeStatement(Node tree) {
    ResolverVisitor visitor = resolverVisitor();
    visitor.visit(tree);
    // Resolve the type annotations encountered in the code.
    while (!resolver.toResolve.isEmpty()) {
      resolver.toResolve.removeFirst().resolve(this);
    }
    return visitor.mapping;
  }

  resolverVisitor() {
    Element mockElement =
        new Element(buildSourceString(''), ElementKind.FUNCTION, null);
    ResolverVisitor visitor = new FullResolverVisitor(this, mockElement);
    visitor.context = new BlockScope(visitor.context);
    return visitor;
  }

  parseScript(String text) {
    for (Link<Element> link = parseUnit(text, this);
         !link.isEmpty();
         link = link.tail) {
      universe.define(link.head);
    }
  }

  resolve(ClassElement element) {
    return resolver.resolveType(element);
  }
}

