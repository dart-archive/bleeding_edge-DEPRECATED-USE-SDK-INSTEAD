// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface Visitor<R> {
  R visitBlock(Block node);
  R visitClassNode(ClassNode node);
  R visitExpressionStatement(ExpressionStatement node);
  R visitFor(For node);
  R visitFunctionExpression(FunctionExpression node);
  R visitIdentifier(Identifier node);
  R visitIf(If node);
  R visitLiteralBool(LiteralBool node);
  R visitLiteralDouble(LiteralDouble node);
  R visitLiteralInt(LiteralInt node);
  R visitLiteralString(LiteralString node);
  R visitNodeList(NodeList node);
  R visitOperator(Operator node);
  R visitReturn(Return node);
  R visitSend(Send node);
  R visitSendSet(SendSet node);
  R visitThrow(Throw node);
  R visitTypeAnnotation(TypeAnnotation node);
  R visitVariableDefinitions(VariableDefinitions node);
}

Token firstBeginToken(Node first, Node second) {
  return (first !== null) ? first.getBeginToken()
                          : second.getBeginToken();
}

/**
 * A node in a syntax tree.
 *
 * The abstract part of "abstract syntax tree" is invalidated when
 * supporting tools such as code formatting. These tools need concrete
 * syntax such as parentheses and no constant folding.
 *
 * We support these tools by storing additional references back to the
 * token stream. These references are stored in fields ending with
 * "Token".
 */
class Node implements Hashable {
  final int _hashCode;

  const Node() : _hashCode = (Math.random() * 0xFFFFFFFF).toInt();

  hashCode() => _hashCode;

  abstract accept(Visitor visitor);

  toString() => unparse();

  String getObjectDescription() => super.toString();

  String unparse() {
    Unparser unparser = new Unparser();
    try {
      return unparser.unparse(this);
    } catch (var e) {
      return '<<unparse error: ${getObjectDescription()}: ${unparser.sb}>>';
    }
  }

  abstract Token getBeginToken();

  abstract Token getEndToken();
}

class ClassNode extends Node {
  final Identifier name;
  final TypeAnnotation superclass;
  final NodeList interfaces;

  final Token beginToken;
  final Token extendsKeyword;
  final Token endToken;

  const ClassNode(this.name, this.superclass, this.interfaces,
                  this.beginToken, this.extendsKeyword, this.endToken);

  accept(Visitor visitor) => visitor.visitClassNode(this);

  bool get isInterface() => beginToken.stringValue === 'interface';

  bool get isClass() => !isInterface;

  Token getBeginToken() => beginToken;

  Token getEndToken() => endToken;
}

class Expression extends Node {
  const Expression();
}

class Statement extends Node {
  const Statement();
}

/**
 * A message send aka method invocation. In Dart, most operations can
 * (and should) be considered as message sends. Getters and setters
 * are just methods with a special syntax. Consequently, we model
 * property access, assignment, operators, and method calls with this
 * one node.
 */
class Send extends Expression {
  final Node receiver;
  final Node selector;
  final NodeList argumentsNode;
  Link<Node> get arguments() => argumentsNode.nodes;

  const Send([this.receiver, this.selector, this.argumentsNode]);

  accept(Visitor visitor) => visitor.visitSend(this);

  bool get isOperator() => selector is Operator;
  bool get isPropertyAccess() => argumentsNode === null;
  bool get isFunctionObjectInvocation() => selector === null;

  Token getBeginToken() {
    return firstBeginToken(receiver, selector);
  }

  Token getEndToken() {
    Token token;
    if (argumentsNode !== null) token = argumentsNode.getEndToken();
    if (token !== null) return token;
    if (selector !== null) {
      return selector.getEndToken();
    }
    return receiver.getBeginToken();
  }
}

class SendSet extends Send {
  final Token assignmentOperator;
  const SendSet(receiver, selector, this.assignmentOperator, argumentsNode)
    : super(receiver, selector, argumentsNode);

  accept(Visitor visitor) => visitor.visitSendSet(this);
}

class NewExpression extends Expression {
  /** The token NEW or CONST */
  final Token newToken;

  // Note: we expect that send.receiver is null.
  final Send send;

  const NewExpression([this.newToken, this.send]);

  Token getBeginToken() => newToken;

  Token getEndToken() => send.getEndToken();
}

class NodeList extends Node {
  final Link<Node> nodes;
  final Token beginToken;
  final Token endToken;
  final SourceString delimiter;

  const NodeList([this.beginToken, this.nodes, this.endToken, this.delimiter]);

  NodeList.singleton(Node node) : this(null, LinkFactory.createLink(node));

  accept(Visitor visitor) => visitor.visitNodeList(this);

  Token getBeginToken() {
    if (beginToken !== null) return beginToken;
     if (nodes !== null) {
       for (Link<Node> link = nodes; !link.isEmpty(); link = link.tail) {
         if (link.head.getBeginToken() !== null) {
           return link.head.getBeginToken();
         }
         if (link.head.getEndToken() !== null) {
           return link.head.getEndToken();
         }
       }
     }
    return endToken;
  }

  Token getEndToken() {
    if (endToken !== null) return endToken;
    if (nodes !== null) {
      Link<Node> link = nodes;
      while (!link.tail.isEmpty()) link = link.tail;
      if (link.head.getEndToken() !== null) return link.head.getEndToken();
      if (link.head.getBeginToken() !== null) return link.head.getBeginToken();
    }
    return beginToken;
  }
}

class Block extends Statement {
  final NodeList statements;

  const Block(this.statements);

  accept(Visitor visitor) => visitor.visitBlock(this);

  Token getBeginToken() => statements.getBeginToken();

  Token getEndToken() => statements.getEndToken();
}

class If extends Statement {
  final NodeList condition;
  final Statement thenPart;
  final Statement elsePart;

  final Token ifToken;
  final Token elseToken;

  If(this.condition, this.thenPart, this.elsePart,
     this.ifToken, this.elseToken);

  bool get hasElsePart() => elsePart !== null;

  void validate() {
    // TODO(ahe): Check that condition has size one.
  }

  accept(Visitor visitor) => visitor.visitIf(this);

  Token getBeginToken() => ifToken;

  Token getEndToken() {
    if (elsePart === null) return thenPart.getEndToken();
    return elsePart.getEndToken();
  }
}

class For extends Statement {
  /** Either a variable declaration or an ExpressionStatement. */
  final Statement initializer;

  final ExpressionStatement condition;

  final Node update; // TODO(ahe): Should be an expression list.

  final Statement body;

  final Token forToken;

  For(this.initializer, this.condition, this.update, this.body, this.forToken);

  accept(Visitor visitor) => visitor.visitFor(this);

  Token getBeginToken() => forToken;

  Token getEndToken() {
    return body.getEndToken();
  }
}

class FunctionExpression extends Expression {
  final Node name;
  final NodeList parameters;
  final Block body;
  final TypeAnnotation returnType;

  const FunctionExpression([this.name,
                            this.parameters,
                            this.body,
                            this.returnType]);

  accept(Visitor visitor) => visitor.visitFunctionExpression(this);

  Token getBeginToken() {
    return firstBeginToken(returnType, name);
  }

  Token getEndToken() => body.getEndToken();
}

typedef void DecodeErrorHandler(Token token, var error);

class Literal<T> extends Expression {
  final Token token;
  final DecodeErrorHandler handler;

  Literal(Token this.token, DecodeErrorHandler this.handler);

  abstract T get value();

  Token getBeginToken() => token;

  Token getEndToken() => token;
}

class LiteralInt extends Literal<int> {
  LiteralInt(Token token, DecodeErrorHandler handler) : super(token, handler);

  int get value() {
    try {
      return Math.parseInt(token.value.toString());
    } catch (BadNumberFormatException ex) {
      (this.handler)(token, ex);
    }
  }

  accept(Visitor visitor) => visitor.visitLiteralInt(this);
}

class LiteralDouble extends Literal<double> {
  LiteralDouble(Token token, DecodeErrorHandler handler)
    : super(token, handler);

  double get value() {
    try {
      return Math.parseDouble(token.value.toString());
    } catch (BadNumberFormatException ex) {
      (this.handler)(token, ex);
    }
  }

  accept(Visitor visitor) => visitor.visitLiteralDouble(this);
}

class LiteralBool extends Literal<bool> {
  LiteralBool(Token token, DecodeErrorHandler handler) : super(token, handler);

  bool get value() {
    switch (token.value) {
      case Keyword.TRUE: return true;
      case Keyword.FALSE: return false;
      default:
        (this.handler)(token, "not a bool ${token.value}");
    }
  }

  accept(Visitor visitor) => visitor.visitLiteralBool(this);
}

class LiteralString extends Literal<SourceString> {
  LiteralString(Token token) : super(token, null);

  SourceString get value() => token.value;

  accept(Visitor visitor) => visitor.visitLiteralString(this);
}

class Identifier extends Expression {
  final Token token;

  SourceString get source() => token.value;

  const Identifier(Token this.token);

  accept(Visitor visitor) => visitor.visitIdentifier(this);

  getBeginToken() => token;

  getEndToken() => token;
}

class Operator extends Identifier {
  const Operator(Token token) : super(token);

  accept(Visitor visitor) => visitor.visitOperator(this);
}

class Return extends Statement {
  final Expression expression;
  final Token beginToken;
  final Token endToken;

  const Return(this.beginToken, this.endToken, this.expression);

  bool get hasExpression() => expression !== null;

  accept(Visitor visitor) => visitor.visitReturn(this);

  Token getBeginToken() => beginToken;

  Token getEndToken() => endToken;
}

class ExpressionStatement extends Statement {
  final Expression expression;
  final Token endToken;

  const ExpressionStatement(this.expression, this.endToken);

  accept(Visitor visitor) => visitor.visitExpressionStatement(this);

  Token getBeginToken() => expression.getBeginToken();

  Token getEndToken() => endToken;
}

class Throw extends Statement {
  final Expression expression;

  final Token throwToken;
  final Token endToken;

  const Throw(this.expression, this.throwToken, this.endToken);

  accept(Visitor visitor) => visitor.visitThrow(this);

  Token getBeginToken() => throwToken;
  Token getEndToken() => endToken;
}

class TypeAnnotation extends Node {
  final Identifier typeName;

  TypeAnnotation(Identifier this.typeName);

  accept(Visitor visitor) => visitor.visitTypeAnnotation(this);

  Token getBeginToken() => typeName.getBeginToken();

  Token getEndToken() => typeName.getEndToken();
}

class VariableDefinitions extends Statement {
  final Token endToken;
  final TypeAnnotation type;
  final Modifiers modifiers;
  final NodeList definitions;
  VariableDefinitions(this.type, this.modifiers, this.definitions,
                      this.endToken);

  accept(Visitor visitor) => visitor.visitVariableDefinitions(this);

  Token getBeginToken() {
    return firstBeginToken(type, definitions);
  }

  Token getEndToken() => endToken;
}

/** Representation of modifiers such as static, abstract, final, etc. */
class Modifiers {
  final Link<Token> modifiers;
  /** Bit pattern to easy check what modifiers are present. */
  final int flags;

  const Modifiers([this.modifiers, this.flags = 0]);
}
