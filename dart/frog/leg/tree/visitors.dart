// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class AbstractVisitor<R> implements Visitor<R> {
  abstract R visitNode(Node node);

  R visitBlock(Block node) => visitStatement(node);
  R visitClassNode(ClassNode node) => visitNode(node);
  R visitDoWhile(DoWhile node) => visitLoop(node);
  R visitExpression(Expression node) => visitNode(node);
  R visitExpressionStatement(ExpressionStatement node) => visitStatement(node);
  R visitFor(For node) => visitStatement(node);
  R visitFunctionExpression(FunctionExpression node) => visitExpression(node);
  R visitIdentifier(Identifier node) => visitExpression(node);
  R visitIf(If node) => visitStatement(node);
  R visitLiteral(Literal node) => visitExpression(node);
  R visitLiteralBool(LiteralBool node) => visitLiteral(node);
  R visitLiteralDouble(LiteralDouble node) => visitLiteral(node);
  R visitLiteralInt(LiteralInt node) => visitLiteral(node);
  R visitLiteralList(LiteralList node) => visitExpression(node);
  R visitLiteralNull(LiteralNull node) => visitLiteral(node);
  R visitLiteralString(LiteralString node) => visitLiteral(node);
  R visitLoop(Loop node) => visitStatement(node);
  R visitNewExpression(NewExpression node) => visitExpression(node);
  R visitNodeList(NodeList node) => visitNode(node);
  R visitOperator(Operator node) => visitIdentifier(node);
  R visitParenthesizedExpression(ParenthesizedExpression node) {
    return visitExpression(node);
  }
  R visitPostfix(Postfix node) => visitNodeList(node);
  R visitPrefix(Prefix node) => visitNodeList(node);
  R visitReturn(Return node) => visitStatement(node);
  R visitSend(Send node) => visitExpression(node);
  R visitSendSet(SendSet node) => visitSend(node);
  R visitStatement(Statement node) => visitNode(node);
  R visitThrow(Throw node) => visitStatement(node);
  R visitTypeAnnotation(TypeAnnotation node) => visitNode(node);
  R visitVariableDefinitions(VariableDefinitions node) => visitStatement(node);
  R visitWhile(While node) => visitLoop(node);
}

/**
 * This visitor takes another visitor and applies it to every
 * node in the tree. There is currently no way to control the
 * traversal.
 */
class TraversingVisitor implements Visitor {
  final Visitor visitor;

  TraversingVisitor(Visitor this.visitor);

  visitNode(Node node) {
    node.accept(visitor);
    node.visitChildren(this);
  }

  // TODO(karlklose): use abstract visitor. Currently, frog throws
  //   away some default implementation when usings the abstract class
  //   during compiling the leg_only tests.
  visitBlock(Block node) {}
  visitClassNode(ClassNode node) {}
  visitDoWhile(DoWhile node) {}
  visitExpression(Expression node) {}
  visitExpressionStatement(ExpressionStatement node) {}
  visitFor(For node) {}
  visitFunctionExpression(FunctionExpression node) {}
  visitIdentifier(Identifier node) {}
  visitIf(If node) {}
  visitLiteral(Literal node) {}
  visitLiteralBool(LiteralBool node) {}
  visitLiteralDouble(LiteralDouble node) {}
  visitLiteralInt(LiteralInt node) {}
  visitLiteralList(LiteralList node) {}
  visitLiteralNull(LiteralNull node) {}
  visitLiteralString(LiteralString node) {}
  visitLoop(Loop node) {}
  visitNewExpression(NewExpression node) {}
  visitNodeList(NodeList node) {}
  visitOperator(Operator node) {}
  visitParenthesizedExpression(ParenthesizedExpression node) {}
  visitPostfix(Postfix node) {}
  visitPrefix(Prefix node) {}
  visitReturn(Return node) {}
  visitSend(Send node) {}
  visitSendSet(SendSet node) {}
  visitStatement(Statement node) {}
  visitThrow(Throw node) {}
  visitTypeAnnotation(TypeAnnotation node) {}
  visitVariableDefinitions(VariableDefinitions node) {}
  visitWhile(While node) {}
}
