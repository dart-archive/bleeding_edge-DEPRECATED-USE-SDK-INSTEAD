// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("../../../leg/leg.dart");
#import("../../../leg/scanner.dart");
#import("../../../leg/tree.dart");
#import("../../../leg/util.dart");

Node parse(String text) {
  Token tokens = scan(text + ';');
  BodyListener listener = new BodyListener(null, null);
  BodyParser parser = new BodyParser(listener);
  Token endToken = parser.parseExpression(tokens);
  assert(endToken.value == const SourceString(';'));
  return listener.popNode();
}

Token scan(String text) => new StringScanner(text).tokenize();

buildIdentifier(String name) {
  return new Identifier(scan(name));
}

buildInitialization(String name) {
  var receiver = buildIdentifier(name);
  var selector = buildIdentifier('=');
  var arguments = parse('x(1)').argumentsNode;
  return new Send(receiver, selector, arguments);
}

createLocals(List variables) {
  var locals = [];
  for (final variable in variables) {
    String name = variable[0];
    bool init = variable[1];
    if (init) {
      locals.add(buildInitialization(name));
    } else {
      locals.add(buildIdentifier(name));
    }
  }
  var definitions = new NodeList(null, new Link.fromList(locals), null, null);
  return new VariableDefinitions(null, null, definitions, null);
}

testLocals(List variables) {
  ResolverVisitor visitor =
      new ResolverVisitor(new Compiler(null), new Types());
  Map map = visitor.visit(createLocals(variables));
  // A VariableDefinitions does not have an element.
  Expect.equals(null, map);
  Expect.equals(variables.length, visitor.context.elements.length);

  for (final variable in variables) {
    final name = variable[0];
    Identifier id = buildIdentifier(name);
    final element = visitor.visit(id);
    Expect.equals(element, visitor.context.elements[new SourceString(name)]);
  }
}

main() {
  testLocals([["foo", false]]);
  testLocals([["foo", false], ["bar", false]]);
  testLocals([["foo", false], ["bar", false], ["foobar", false]]);

  testLocals([["foo", true]]);
  testLocals([["foo", false], ["bar", true]]);
  testLocals([["foo", true], ["bar", true]]);

  testLocals([["foo", false], ["bar", false], ["foobar", true]]);
  testLocals([["foo", false], ["bar", true], ["foobar", true]]);
  testLocals([["foo", true], ["bar", true], ["foobar", true]]);

  // TODO(ngeoffray): Does not break yet.
  // testLocals([["foo", false], ["foo", false]]);
}
