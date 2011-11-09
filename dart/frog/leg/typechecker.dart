// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class TypeCheckerTask extends CompilerTask {
  TypeCheckerTask(Compiler compiler) : super(compiler);
  String get name() => "Type checker";

  void check(Node tree, Map<Node, Element> elements) {
    measure(() {
      Visitor visitor = new TypeCheckerVisitor(compiler, elements, new Types());
      tree.accept(visitor);
    });
  }
}

class CompilerError {
  static String notAssignable(Type t, Type s) => '$t is not assignable to $s';
  static String voidExpression() => 'expression does not yield a value';
  static String voidVariable() => 'variable cannot be declared void';
}

interface Type {}

class SimpleType implements Type {
  final SourceString name;
  final Element element;

  const SimpleType(SourceString this.name,  this.element);
  const SimpleType.named(SourceString name)
    : this.name = name, this.element = new Element(name, null, null);

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
  final SimpleType voidType;
  final SimpleType intType;
  final SimpleType dynamicType;
  final SimpleType stringType;

  Types() : voidType = new SimpleType.named(const SourceString('void')),
            intType = new SimpleType.named(const SourceString('int')),
            dynamicType = new SimpleType.named(const SourceString('Dynamic')),
            stringType = new SimpleType.named(const SourceString('String'));

  Type lookup(SourceString s) {
    if (voidType.name == s) {
      return voidType;
    } else if (intType.name == s) {
      return intType;
    } else if (dynamicType.name == s || s.stringValue === 'var') {
      return dynamicType;
    } else if (stringType.name == s) {
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

class TypeCheckerVisitor implements Visitor<Type> {
  Compiler compiler;
  Map elements;
  Type expectedReturnType;  // TODO(karlklose): put into a context.
  Types types;

  TypeCheckerVisitor(Compiler this.compiler, Map this.elements,
                     Types this.types);

  fail(node) {
    compiler.cancel('cannot type-check $node');
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
    if (node === null) compiler.cancel('unexpected node: null');
    Type result = node.accept(this);
    // TODO(karlklose): record type?
    return result;
  }

  Type visitBlock(Block node) {
    type(node.statements);
    return types.voidType;
  }

  Type visitExpressionStatement(ExpressionStatement node) {
    return type(node.expression);
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
    if (target !== null) {
      final targetType = target.computeType(compiler, types);
      if (node.isPropertyAccess) {
        return targetType;
      } else if (node.isFunctionObjectInvocation) {
        // TODO(karlklose): Function object invocations are not
        // yet implemented.
        fail(node);
      } else {
        if (targetType is !FunctionType) {
          // TODO(karlklose): this is probably not needed here.
          compiler.cancel('can only handle function types. ($node)');
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
      Identifier selector = node.selector;
      SourceString name = selector.source;
      if (name == const SourceString('print')
          || name == const SourceString('+')
          || name == const SourceString('=')) {
        return types.dynamicType;
      }
      // TODO(karlklose): Implement method lookup for unresolved targets.
      compiler.cancel('unresolved send $name.');
    }
  }

  visitSendSet(SendSet node) {
    if (node.arguments === null || node.arguments.isEmpty()) {
      compiler.cancel('internal error: argument expected.');
    }
    Type targetType = elements[node].computeType(compiler, types);
    Node value = node.arguments.head;
    checkAssignable(value, type(value), targetType);
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
  }

  Type visitOperator(Operator node) {
    return types.dynamicType;
  }

  checkAssignable(Node node, Type s, Type t) {
    if (!types.isAssignable(s, t)) {
      var error = CompilerError.notAssignable(s, t);
      compiler.reportWarning(node, error);
    }
  }

  Type visitReturn(Return node) {
    final expression = node.expression;
    final expressionType = type(expression);
    checkAssignable(expression, expressionType, expectedReturnType);
    return types.voidType;
  }

  Type visitThrow(Throw node) {
    if (node.expression !== null) type(node.expression);
    return types.voidType;
  }

  Type visitTypeAnnotation(TypeAnnotation node) {
    if (node.typeName === null) return types.dynamicType;
    final name = node.typeName.source;
    final type = types.lookup(name);
    if (type === null) compiler.cancel('unsupported type ${name}');
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
      if (initialization is Send) {
        Type initializer = nonVoidType(link.head);
        checkAssignable(node, type, initializer);
      } else if (initialization is !Identifier) {
        compiler.cancel('unexpected node type for variable initialization');
      }
    }
  }
}
