// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class TreeValidatorTask extends CompilerTask {
  TreeValidatorTask(Compiler compiler) : super(compiler);

  void validate(Node tree) {
    assert(check(tree));
  }

  bool check(Node tree) {
    List<InvalidNodeError> errors = [];
    void report(node, message) {
           final error = new InvalidNodeError(node, message);
           errors.add(error);
           compiler.log(error);
         };
    final validator = new ValidatorVisitor(report);
    tree.accept(new TraversingVisitor(validator));

    return errors.isEmpty();
  }
}

class ValidatorVisitor implements Visitor {
  final Function reportInvalidNode;

  ValidatorVisitor(Function this.reportInvalidNode);

  expect(Node node, bool test, [message]) {
    if (!test) reportInvalidNode(node, message);
  }

  visitNode(Node node) {}

  visitSendSet(SendSet node) {
    final selector = node.selector;
    final name = node.assignmentOperator.source.stringValue;
    final arguments = node.arguments;

    expect(node, arguments !== null);
    expect(node, selector is Identifier, 'selector is not assignable');
    if (name === '++' || name === '--') {
      expect(node, node.assignmentOperator is Operator);
      expect(node, node.arguments.isEmpty());
    } else {
      expect(node, !node.arguments.isEmpty());
    }
  }

  // TODO(karlklose): use abstract visitor. Currently, frog throws
  //   away some default implementations when using the abstract class
  //   during compiling the leg_only tests.
  visitBlock(Block node) => visitStatement(node);
  visitClassNode(ClassNode node) => visitNode(node);
  visitDoWhile(DoWhile node) => visitLoop(node);
  visitExpression(Expression node) => visitNode(node);
  visitExpressionStatement(ExpressionStatement node) => visitStatement(node);
  visitFor(For node) => visitStatement(node);
  visitFunctionExpression(FunctionExpression node) => visitExpression(node);
  visitIdentifier(Identifier node) => visitExpression(node);
  visitIf(If node) => visitStatement(node);
  visitLiteral(Literal node) => visitExpression(node);
  visitLiteralBool(LiteralBool node) => visitLiteral(node);
  visitLiteralDouble(LiteralDouble node) => visitLiteral(node);
  visitLiteralInt(LiteralInt node) => visitLiteral(node);
  visitLiteralNull(LiteralNull node) => visitLiteral(node);
  visitLiteralString(LiteralString node) => visitLiteral(node);
  visitLoop(Loop node) => visitStatement(node);
  visitNewExpression(NewExpression node) => visitExpression(node);
  visitNodeList(NodeList node) => visitNode(node);
  visitOperator(Operator node) => visitIdentifier(node);
  visitParenthesizedExpression(ParenthesizedExpression node) {
    return visitExpression(node);
  }
  visitPostfix(Postfix node) => visitNodeList(node);
  visitPrefix(Prefix node) => visitNodeList(node);
  visitReturn(Return node) => visitStatement(node);
  visitSend(Send node) => visitExpression(node);

  visitStatement(Statement node) => visitNode(node);
  visitThrow(Throw node) => visitStatement(node);
  visitTypeAnnotation(TypeAnnotation node) => visitNode(node);
  visitVariableDefinitions(VariableDefinitions node) => visitStatement(node);
  visitWhile(While node) => visitLoop(node);
}

class InvalidNodeError {
  final Node node;
  final String message;
  InvalidNodeError(this.node, [this.message]);

  toString() {
    String nodeString = new Unparser(true).unparse(node);
    String result = 'invalid node: $nodeString';
    if (message !== null) result = '$result ($message)';
    return result;
  }
}
