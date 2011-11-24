// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Unparser implements Visitor {
  StringBuffer sb;
  final bool printDebugInfo;

  Unparser([this.printDebugInfo = false]);

  String unparse(Node node) {
    sb = new StringBuffer();
    visit(node);
    return sb.toString();
  }

  void add(SourceString string) {
    string.printOn(sb);
  }

  visit(Node node) {
    if (node !== null) {
      if (printDebugInfo) sb.add('[${node.getObjectDescription()}: ');
      node.accept(this);
      if (printDebugInfo) sb.add(']');
    } else if (printDebugInfo) {
      sb.add('[null]');
    }
  }

  visitBlock(Block node) {
    visit(node.statements);
  }

  visitClassNode(ClassNode node) {
    node.beginToken.value.printOn(sb);
    sb.add(' ');
    visit(node.name);
    sb.add(' ');
    if (node.extendsKeyword !== null) {
      node.extendsKeyword.value.printOn(sb);
      sb.add(' ');
      visit(node.superclass);
      sb.add(' ');
    }
    visit(node.interfaces);
    sb.add('{\n');
    sb.add('}\n');
  }

  visitExpressionStatement(ExpressionStatement node) {
    visit(node.expression);
    add(node.endToken.value);
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
    visit(node.parameters);
    visit(node.body);
  }

  visitIdentifier(Identifier node) {
    add(node.token.value);
  }

  visitIf(If node) {
    add(node.ifToken.value);
    visit(node.condition);
    visit(node.thenPart);
    if (node.hasElsePart) {
      add(node.elseToken.value);
      visit(node.elsePart);
    }
  }

  visitLiteralBool(LiteralBool node) {
    add(node.token.value);
  }

  visitLiteralDouble(LiteralDouble node) {
    add(node.token.value);
  }

  visitLiteralInt(LiteralInt node) {
    add(node.token.value);
  }

  visitLiteralString(LiteralString node) {
    add(node.token.value);
  }

  visitLiteralNull(LiteralNull node) {
    add(node.token.value);
  }

  visitNodeList(NodeList node) {
    if (node.beginToken !== null) add(node.beginToken.value);
    if (node.nodes !== null) {
      node.nodes.printOn(sb, node.delimiter);
    }
    if (node.endToken !== null) add(node.endToken.value);
  }

  visitOperator(Operator node) {
    visitIdentifier(node);
  }

  visitReturn(Return node) {
    add(node.beginToken.value);
    if (node.hasExpression) {
      sb.add(' ');
      visit(node.expression);
    }
    add(node.endToken.value);
  }

  visitSend(Send node) {
    if (node.isPrefix) {
      visit(node.selector);
    }
    if (node.receiver !== null) {
      visit(node.receiver);
      if (node.selector is !Operator) sb.add('.');
    }
    if (!node.isPrefix) {
      visit(node.selector);
    }
    visit(node.argumentsNode);
  }

  visitSendSet(SendSet node) {
    if (node.receiver !== null) {
      visit(node.receiver);
      sb.add('.');
    }
    visit(node.selector);
    add(node.assignmentOperator.token.value);
    visit(node.argumentsNode);
  }

  visitThrow(Throw node) {
    node.throwToken.value.printOn(sb);
    if (node.expression !== null) {
      visit(node.expression);
    }
    node.endToken.value.printOn(sb);
  }

  visitTypeAnnotation(TypeAnnotation node) {
    visit(node.typeName);
  }

  visitVariableDefinitions(VariableDefinitions node) {
    if (node.type !== null) {
      visit(node.type);
    } else {
      sb.add('var');
    }
    sb.add(' ');
    // TODO(karlklose): print modifiers.
    visit(node.definitions);
    if (node.endToken.value == const SourceString(';')) {
      add(node.endToken.value);
    }
  }
}
