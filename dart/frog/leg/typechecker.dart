// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class TypeCheckerTask extends CompilerTask {
  TypeCheckerTask(Compiler compiler) : types = new Types(), super(compiler);
  String get name() => "Type checker";
  Types types;

  void check(Node tree, Map<Node, Element> elements) {
    measure(() {
      Visitor visitor =
          new TypeCheckerVisitor(compiler, elements, types);
      try {
        tree.accept(visitor);
      } catch (CancelTypeCheckException e) {
        compiler.reportWarning(e.node, e.reason);
      }
    });
  }
}

class CompilerError {
  static String notAssignable(Type t, Type s) => '$t is not assignable to $s';
  static String voidExpression() => 'expression does not yield a value';
  static String voidVariable() => 'variable cannot be declared void';
  static String returnValueInVoid() => 'cannot return value from void function';
  static String returnNothing(Type t) => 'value of type $t expected';
}

interface Type {}

class SimpleType implements Type {
  final SourceString name;
  final Element element;

  const SimpleType(SourceString this.name, Element this.element);
  SimpleType.named(SourceString name)
    : this.name = name, element = new Element(name, null, null);

  String toString() => name.toString();
}

class FunctionType implements Type {
  final Type returnType;
  final Link<Type> parameterTypes;

  const FunctionType(Type this.returnType, Link<Type> this.parameterTypes);

  toString() {
    StringBuffer sb = new StringBuffer();
    bool first = true;
    sb.add('(');
    parameterTypes.printOn(sb, ', ');
    sb.add(') -> ${returnType}');
    return sb.toString();
  }
}

class Types {
  static final VOID = const SourceString('void');
  static final INT = const SourceString('int');
  static final DYNAMIC = const SourceString('Dynamic');
  static final STRING = const SourceString('String');

  final SimpleType voidType;
  final SimpleType intType;
  final SimpleType dynamicType;
  final SimpleType stringType;

  Types() : voidType = new SimpleType.named(VOID),
            intType = new SimpleType.named(INT),
            dynamicType = new SimpleType.named(DYNAMIC),
            stringType = new SimpleType.named(STRING);

  Type lookup(SourceString s) {
    if (VOID == s) {
      return voidType;
    } else if (INT == s) {
      return intType;
    } else if (DYNAMIC == s || s.stringValue === 'var') {
      return dynamicType;
    } else if (STRING == s) {
      return stringType;
    }
    return null;
  }

  bool isSubtype(Type r, Type s) {
    return r === s || r === dynamicType || s === dynamicType;
  }

  bool isAssignable(Type r, Type s) {
    return isSubtype(r, s) || isSubtype(s, r);
  }
}

class CancelTypeCheckException {
  final Node node;
  final String reason;

  CancelTypeCheckException(this.node, this.reason);
}

class TypeCheckerVisitor implements Visitor<Type> {
  Compiler compiler;
  Map elements;
  Type expectedReturnType;  // TODO(karlklose): put into a context.
  Types types;

  TypeCheckerVisitor(Compiler this.compiler, Map this.elements,
                     Types this.types);

  Type fail(node, [reason]) {
    String message = 'cannot type-check';
    if (reason !== null) {
      message = '$message: $reason';
    }
    throw new CancelTypeCheckException(node, message);
  }

  Type nonVoidType(Node node) {
    Type type = type(node);
    if (type == types.voidType) {
      compiler.reportWarning(node, CompilerError.voidExpression());
    }
    return type;
  }

  Type typeWithDefault(Node node, Type defaultValue) {
    return node !== null ? type(node) : defaultValue;
  }

  Type type(Node node) {
    if (node === null) fail(null, 'unexpected node: null');
    Type result = node.accept(this);
    // TODO(karlklose): record type?
    return result;
  }

  checkAssignable(Node node, Type s, Type t) {
    if (!types.isAssignable(s, t)) {
      var error = CompilerError.notAssignable(s, t);
      compiler.reportWarning(node, error);
    }
  }

  Type visitBlock(Block node) {
    type(node.statements);
    return types.voidType;
  }

  Type visitClassNode(ClassNode node) {
    fail(node);
  }

  Type visitExpressionStatement(ExpressionStatement node) {
    return type(node.expression);
  }

  Type visitFor(For node) {
    fail(node);
  }

  Type visitFunctionExpression(FunctionExpression node) {
    FunctionType functionType =
        elements[node.name].computeType(compiler, types);
    Type returnType = functionType.returnType;
    Type previous = expectedReturnType;
    expectedReturnType = returnType;
    type(node.body);
    expectedReturnType = previous;
    return functionType;
  }

  Type visitIdentifier(Identifier node) {
    fail(node);
  }

  Type visitIf(If node) {
    type(node.condition);
    type(node.thenPart);
    if (node.hasElsePart) type(node.elsePart);
    return types.voidType;
  }

  Type visitSend(Send node) {
    final target = elements[node];
    Identifier selector = node.selector;
    if (target !== null) {
      SourceString name = selector.source;
      if (name == const SourceString('+')
          || name == const SourceString('=')
          || name == const SourceString('-')
          || name == const SourceString('*')
          || name == const SourceString('/')
          || name == const SourceString('<')
          || name == const SourceString('~/')) {
        return types.dynamicType;
      }
      final targetType = target.computeType(compiler, types);
      if (node.isPropertyAccess) {
        return targetType;
      } else if (node.isFunctionObjectInvocation) {
        // TODO(karlklose): Function object invocations are not
        // yet implemented.
        fail(node);
      } else {
        if (targetType is !FunctionType) {
          // TODO(karlklose): handle dynamic target types.
          if (target is ForeignElement) {
            //TODO(karlklose): we cannot report errors on foreigns.
            return types.dynamicType;
          }
          fail(node, 'can only handle function types');
        }
        FunctionType funType = targetType;
        Link<Type> formals = funType.parameterTypes;
        Link<Node> arguments = node.arguments;
        while ((!formals.isEmpty()) && (!arguments.isEmpty())) {
          final Node argument = arguments.head;
          final Type argumentType = type(argument);
          checkAssignable(argument, argumentType, formals.head);
          formals = formals.tail;
          arguments = arguments.tail;
        }

        if (!formals.isEmpty()) {
          compiler.reportWarning(node, 'missing argument');
        }
        if (!arguments.isEmpty()) {
          compiler.reportWarning(arguments.head, 'additional arguments');
        }

        return funType.returnType;
      }
    } else {
      // TODO(karlklose): Implement method lookup for unresolved targets.
      fail(node, 'unresolved send ${selector.source}');
    }
  }

  visitSendSet(SendSet node) {
    compiler.ensure(node.arguments !== null && !node.arguments.isEmpty());
    Type targetType = elements[node].computeType(compiler, types);
    Node value = node.arguments.head;
    checkAssignable(value, type(value), targetType);
    return targetType;
  }

  Type visitLiteralInt(LiteralInt node) {
    return types.intType;
  }

  Type visitLiteralDouble(LiteralDouble node) {
    return types.dynamicType;
  }

  Type visitLiteralBool(LiteralBool node) {
    return types.dynamicType;
  }

  Type visitLiteralString(LiteralString node) {
    return types.stringType;
  }

  Type visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      type(link.head);
    }
    return null;
  }

  Type visitOperator(Operator node) {
    return types.dynamicType;
  }

  /** Dart Programming Language Specification: 11.10 Return */
  Type visitReturn(Return node) {
    final expression = node.expression;
    final isVoidFunction = (expectedReturnType === types.voidType);

    // Executing a return statement return e; [...] It is a static type warning
    // if the type of e may not be assigned to the declared return type of the
    // immediately enclosing function.
    if (expression !== null) {
      final expressionType = type(expression);
      if (isVoidFunction
          && !types.isAssignable(expressionType, types.voidType)) {
        compiler.reportWarning(expression, CompilerError.returnValueInVoid());
      } else {
        checkAssignable(expression, expressionType, expectedReturnType);
      }

    // Let f be the function immediately enclosing a return statement of the
    // form 'return;' It is a static warning if both of the following conditions
    // hold:
    // - f is not a generative constructor.
    // - The return type of f may not be assigned to void.
    } else if (!types.isAssignable(expectedReturnType, types.voidType)) {
      final error = CompilerError.returnNothing(expectedReturnType);
      compiler.reportWarning(node, error);
    }
    return null;
  }

  Type visitThrow(Throw node) {
    if (node.expression !== null) type(node.expression);
    return types.voidType;
  }

  Type visitTypeAnnotation(TypeAnnotation node) {
    if (node.typeName === null) return types.dynamicType;
    final name = node.typeName.source;
    final type = types.lookup(name);
    if (type === null) fail(node, 'unsupported type ${name}');
    return type;
  }

  Type visitVariableDefinitions(VariableDefinitions node) {
    Type type = typeWithDefault(node.type, types.dynamicType);
    if (type == types.voidType) {
      compiler.reportWarning(node.type, CompilerError.voidVariable());
      type = types.dynamicType;
    }
    for (Link<Node> link = node.definitions.nodes; !link.isEmpty();
         link = link.tail) {
      Node initialization = link.head;
      compiler.ensure(initialization is Identifier
          || initialization is Send);
      if (initialization is Send) {
        Type initializer = nonVoidType(link.head);
        checkAssignable(node, type, initializer);
      }
    }
    return null;
  }
}
