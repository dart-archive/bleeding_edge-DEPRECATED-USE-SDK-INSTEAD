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
  static NOT_ASSIGNABLE(Type t, Type s) => '$t is not assignable to $s';
}

interface Type {}

class SimpleType implements Type {
  final SourceString name;
  final Element element;

  const SimpleType(SourceString this.name,  this.element);

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
    for (Link<Type> link = parameterTypes; !link.isEmpty(); link = link.tail) {
      if (!first) sb.add(', ');
      first = false;
      sb.add(link.head);
    }
    sb.add(') -> ${returnType}');
    return sb.toString();
  }
}

class Types {
  Type VOID;
  Type INT;
  Type DYNAMIC;
  Type STRING;

  bool isSubtype(Type r, Type s) {
    return r === s || r === DYNAMIC || s === DYNAMIC;
  }

  Types() : VOID = new SimpleType(const SourceString('void'),
                                  new Element(const SourceString('void'))),
            INT = new SimpleType(const SourceString('int'),
                                 new Element(const SourceString('int'))),
            DYNAMIC = new SimpleType(const SourceString('Dynamic'),
                                     new Element(const SourceString('Dynamic'))),
            STRING = new SimpleType(const SourceString('String'),
                                    new Element(const SourceString('String')));

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

  Type visit(Node node) {
    Type type = node.accept(this);
    // TODO(karlklose): record type?
    return type;
  }

  Type visitBlock(Block node) {
    visit(node.statements);
    return types.VOID;
  }

  Type visitExpressionStatement(ExpressionStatement node) {
    return visit(node.expression);
  }

  Type visitFunctionExpression(FunctionExpression node) {
    Type functionType = elements[node].computeType(compiler, types);
    Type returnType = functionType.returnType;
    Type previous = expectedReturnType;
    expectedReturnType = returnType;
    visit(node.body);
    expectedReturnType = previous;
    return functionType;
  }

  Type visitIdentifier(Identifier node) {
    fail(node);
  }

  Type visitSend(Send node) {
    Element target = elements[node];
    if (target !== null) {
      // TODO(karlklose): Move that to a function that also
      // calculates FunctionTypes for other target types.
      FunctionType funType = target.computeType(compiler, types);
      Link<Type> formals = funType.parameterTypes;
      Link<Node> arguments = node.arguments;
      while ((!formals.isEmpty()) && (!arguments.isEmpty())) {
        compiler.cancel('parameters not supported.');
        var argumentType = visit(arguments.head);
        if (!types.isAssignable(formals.head, argumentType)) {
          var warning = CompilerError.NOT_ASSIGNABLE(argumentType,
                                                     formals.head);
          compiler.reportWarning(node, warning);
        }
        formals = formals.tail;
        arguments = arguments.tail;
      }

      if (!formals.isEmpty()) {
        compiler.reportWarning(node, 'missing argument');
      }
      if (!arguments.isEmpty()) {
        compiler.reportWarning(node, 'additional arguments');
      }

      return funType.returnType;
    } else {
      Identifier selector = node.selector;
      SourceString name = selector.source;
      if (name == const SourceString('print')
          || name == const SourceString('+')) {
        return types.DYNAMIC;
      }
      compiler.cancel('unresolved send $name.');
    }
  }

  Type visitLiteralInt(LiteralInt node) {
    return types.INT;
  }

  Type visitLiteralDouble(LiteralDouble node) {
    return types.DYNAMIC;
  }

  Type visitLiteralBool(LiteralBool node) {
    return types.DYNAMIC;
  }

  Type visitLiteralString(LiteralString node) {
    return types.DYNAMIC;
  }

  Type visitNodeList(NodeList node) {
    for (Link<Node> link = node.nodes; !link.isEmpty(); link = link.tail) {
      visit(link.head);
    }
  }

  Type visitOperator(Operator node) {
    return types.DYNAMIC;
  }

  Type visitParameter(Parameter node) {
    return null;
  }

  Type visitReturn(Return node) {
    Type expressionType = visit(node.expression);
    if (!types.isAssignable(expectedReturnType, expressionType)) {
      var error = CompilerError.NOT_ASSIGNABLE(expectedReturnType,
                                               expressionType);
      compiler.reportWarning(node, error);
    }
    return types.VOID;
  }

  Type visitTypeAnnotation(TypeAnnotation node) {
    if (node.typeName !== null
        && node.typeName.source != const SourceString('void')) {
      compiler.cancel('unsupported type ${node.typeName}');
    }
    return types.VOID;
  }

  Type visitVariableDefinitions(VariableDefinitions node) {
    // TODO(karlklose): Implement this.
    return types.VOID;
  }
}
