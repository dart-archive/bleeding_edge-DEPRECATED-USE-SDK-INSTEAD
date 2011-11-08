// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class DebugUnparser implements Visitor {
  String separator;
  StringBuffer sb;

  String unparse(Node node) {
    separator = '';
    sb = new StringBuffer();
    visit(node);
    return sb.toString();
  }

  visit(Node node, [withSeparator]) {
    final previous = separator;
    separator = (withSeparator !== null) ? withSeparator : separator;
    if (node !== null) node.accept(this);
    separator = previous;
  }

  visitBlock(Block node) {
    visit(node.statements);
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
    sb.add(';');
  }

  visitFor(For node) {
    node.forToken.value.printOn(sb);
    sb.add('(');
    visit(node.initializer);
    visit(node.condition);
    visit(node.update);
    sb.add(')');
    visit(node.body);
  }

  visitFunctionExpression(FunctionExpression node) {
    if (node.returnType !== null) {
      visit(node.returnType);
      sb.add(' ');
    }
    visit(node.name);
    visit(node.parameters, ', ');
    visit(node.body);
  }

  visitIdentifier(Identifier node) {
    node.source.printOn(sb);
  }

  visitIf(If node) {
    node.ifToken.value.printOn(sb);
    visit(node.condition);
    visit(node.thenPart);
    if (node.hasElsePart) {
      node.elseToken.value.printOn(sb);
      visit(node.elsePart);
    }
  }

  visitLiteralBool(LiteralBool node) {
    node.token.value.printOn(sb);
  }

  visitLiteralDouble(LiteralDouble node) {
    node.token.value.printOn(sb);
  }

  visitLiteralInt(LiteralInt node) {
    node.token.value.printOn(sb);
  }

  visitLiteralString(LiteralString node) {
    node.token.value.printOn(sb);
  }

  visitNodeList(NodeList node) {
    bool first = true;
    if (node.beginToken !== null) sb.add(node.beginToken);
    if (node.nodes !== null) {
      // TODO(karlklose): remove delimiter from NodeList and use separator?
      SourceString delimiter = node.delimiter;
      if (delimiter == null) delimiter = new SourceString(separator);
      for (Node element in node.nodes) {
        if (!first) delimiter.printOn(sb);
        first = false;
        visit(element);
      }
    }
    if (node.endToken !== null) sb.add(node.endToken);
  }

  visitOperator(Operator node) {
    visitIdentifier(node);
  }

  visitReturn(Return node) {
    node.beginToken.value.printOn(sb);
    if (node.hasExpression) {
      sb.add(' ');
      visit(node.expression);
    }
    node.endToken.value.printOn(sb);
  }

  visitSend(Send node) {
    if (node.receiver !== null) {
      visit(node.receiver);
      if (node.selector is !Operator) sb.add('.');
    }
    visit(node.selector);
    visit(node.argumentsNode, ', ');
  }

  visitSendSet(SendSet node) {
    if (node.receiver !== null) {
      visit(node.receiver);
      sb.add('.');
    }
    visit(node.selector);
    node.assignmentOperator.value.printOn(sb);
    visit(node.argumentsNode, ', ');
  }

  visitTypeAnnotation(TypeAnnotation node) {
    visit(node.typeName);
  }

  visitVariableDefinitions(VariableDefinitions node) {
    if (node.type !== null) {
      visit(node.type);
      sb.add(' ');
    }
    // TODO(karlklose): print modifiers.
    visit(node.definitions, ', ');
    sb.add('; ');
  }
}
