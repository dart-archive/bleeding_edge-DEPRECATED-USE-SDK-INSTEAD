// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class TypeCheckerTask extends CompilerTask {
  TypeCheckerTask(Compiler compiler) : types = new Types(), super(compiler);
  String get name() => "Type checker";
  Types types;

  void check(Node tree, TreeElements elements) {
    measure(() {
      Visitor visitor =
          new TypeCheckerVisitor(compiler, elements, types);
      try {
        tree.accept(visitor);
      } catch (CancelTypeCheckException e) {
        compiler.reportWarning(e.node, new TypeWarning(MessageKind.GENERIC,
                                                       [e.reason]));
      }
    });
  }
}

interface Type {
  String get name();
  Element get element();
}

class SimpleType implements Type {
  final SourceString name;
  final Element element;

  const SimpleType(SourceString this.name, Element this.element);
  SimpleType.named(SourceString name)
    : this.name = name, element = new Element(name, null, null);

  String toString() => name.toString();
}

class FunctionType implements Type {
  final Element element;
  final Type returnType;
  final Link<Type> parameterTypes;

  const FunctionType(Type this.returnType, Link<Type> this.parameterTypes,
                     Element this.element);

  toString() {
    StringBuffer sb = new StringBuffer();
    bool first = true;
    sb.add('(');
    parameterTypes.printOn(sb, ', ');
    sb.add(') -> ${returnType}');
    return sb.toString();
  }

  String get name() => 'Function';
}

class Types {
  static final VOID = const SourceString('void');
  static final INT = const SourceString('int');
  static final DOUBLE = const SourceString('double');
  static final DYNAMIC = const SourceString('Dynamic');
  static final STRING = const SourceString('String');
  static final BOOL = const SourceString('bool');
  static final OBJECT = const SourceString('Object');

  final SimpleType voidType;
  final SimpleType dynamicType;

  Types() : voidType = new SimpleType.named(VOID),
            dynamicType = new SimpleType.named(DYNAMIC);

  Type lookup(SourceString s) {
    if (VOID == s) {
      return voidType;
    } else if (DYNAMIC == s || s.stringValue === 'var') {
      return dynamicType;
    }
    return null;
  }

  bool isSubtype(Type r, Type s) {
    return r === s || r === dynamicType || s === dynamicType ||
           s.name == Types.OBJECT;
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

Type lookupType(SourceString name, compiler, types) {
  Element element = compiler.universe.find(name);
  if (element !== null && element.kind === ElementKind.CLASS) {
    return element.computeType(compiler, types);
  }
  return types.lookup(name);
}

class TypeCheckerVisitor implements Visitor<Type> {
  final Compiler compiler;
  final TreeElements elements;
  Type expectedReturnType;  // TODO(karlklose): put into a context.
  final Types types;
  Type intType;
  Type doubleType;
  Type boolType;
  Type stringType;

  TypeCheckerVisitor(Compiler this.compiler, TreeElements this.elements,
                     Types this.types) {
    intType = lookupType(Types.INT, compiler, types);
    doubleType = lookupType(Types.DOUBLE, compiler, types);
    boolType = lookupType(Types.BOOL, compiler, types);
    stringType = lookupType(Types.STRING, compiler, types);
  }

  Type fail(node, [reason]) {
    String message = 'cannot type-check';
    if (reason !== null) {
      message = '$message: $reason';
    }
    throw new CancelTypeCheckException(node, message);
  }

  reportTypeWarning(Node node, MessageKind kind, [List arguments = const []]) {
    compiler.reportWarning(node, new TypeWarning(kind, arguments));
  }

  Type nonVoidType(Node node) {
    Type type = type(node);
    if (type == types.voidType) {
      reportTypeWarning(node, MessageKind.VOID_EXPRESSION);
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

  /**
   * Check if a value of type t can be assigned to a variable,
   * parameter or return value of type s.
   */
  checkAssignable(Node node, Type s, Type t) {
    if (!types.isAssignable(s, t)) {
      reportTypeWarning(node, MessageKind.NOT_ASSIGNABLE, [s, t]);
    }
  }

  checkCondition(Expression condition) {
    checkAssignable(condition, boolType, type(condition));
  }

  Type visitBlock(Block node) {
    type(node.statements);
    return types.voidType;
  }

  Type visitClassNode(ClassNode node) {
    fail(node);
  }

  Type visitDoWhile(DoWhile node) {
    type(node.body);
    checkCondition(node.condition);
    return types.voidType;
  }

  Type visitExpressionStatement(ExpressionStatement node) {
    type(node.expression);
    return types.voidType;
  }

  /** Dart Programming Language Specification: 11.5.1 For Loop */
  Type visitFor(For node) {
    type(node.initializer);
    checkCondition(node.condition);
    type(node.update);
    type(node.body);
    return types.voidType;
  }

  Type visitFunctionExpression(FunctionExpression node) {
    final element = elements[node];
    FunctionType functionType = computeType(element);
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

  Type visitLoop(Loop node) {
    final conditionNode = node.condition;
    Type conditionType = nonVoidType(conditionNode);
    checkAssignable(conditionNode, boolType, conditionType);
    type(node.body);
    return types.voidType;
  }

  Type lookupMethodType(Node node, ClassElement classElement,
                        SourceString name) {
    Element member = classElement.lookupLocalElement(name);
    if (member !== null && member.kind == ElementKind.FUNCTION) {
      return computeType(member);
    }
    reportTypeWarning(node, MessageKind.METHOD_NOT_FOUND,
                      [classElement.name, name]);
    return types.dynamicType;
  }

  Link<Type> typeArguments(Link<Node> arguments) {
    LinkBuilder<Type> builder = new LinkBuilder<Type>();
    while(!arguments.isEmpty()) {
      builder.addLast(type(arguments.head));
      arguments = arguments.tail;
    }
    return builder.toLink();
  }

  Type visitSend(Send node) {
    Identifier selector = node.selector;
    String name = selector.source.stringValue;

    if (node.isOperator) {
      final Node firstArgument = node.receiver;
      final Type firstArgumentType = type(node.receiver);
      final arguments = node.arguments;
      final Node secondArgument = arguments.isEmpty() ? null : arguments.head;
      final Type secondArgumentType = typeWithDefault(secondArgument, null);

      if (name === '+' || name === '=' || name === '-'
          || name === '*' || name === '/' || name === '%'
          || name === '~/' || name === '|' || name ==='&'
          || name === '^' || name === '~'|| name === '<<'
          || name === '>>' || name === '[]') {
        return types.dynamicType;
      } else if (name === '<' || name === '>' || name === '<='
                 || name === '>=' || name === '==') {
        return boolType;
      } else if (name === '||' || name === '&&' || name === '!') {
        checkAssignable(firstArgument, boolType, firstArgumentType);
        if (!arguments.isEmpty()) {
          // TODO(karlklose): check number of arguments in validator.
          checkAssignable(secondArgument, boolType, secondArgumentType);
        }
        return boolType;
      }
      fail(selector, 'unexpected operator ${name}');

    } else if (node.isPropertyAccess) {
      if (node.receiver !== null) fail(node, 'cannot handle fields');
      Element element = elements[node];
      if (element === null) fail(node.selector, 'unresolved property');
      return element.computeType(compiler, types);

    } else if (node.isFunctionObjectInvocation) {
      fail(node.receiver, 'function object invocation unimplemented');

    } else {
      Link<Type> argumentTypes = typeArguments(node.arguments);
      FunctionType funType;
      if (node.receiver !== null) {
        Type receiverType = type(node.receiver);
        if (receiverType === types.dynamicType) return types.dynamicType;
        if (receiverType === null) {
          fail(node.receiver, 'receivertype is null');
        }
        ClassElement classElement = receiverType.element;
        // TODO(karlklose): substitute type arguments.
        Type memberType = lookupMethodType(node, classElement, selector.source);
        if (memberType === types.dynamicType) return types.dynamicType;
        if (memberType is !FunctionType) {
          fail(node, 'can only handle function types');
        }
        funType = memberType;
      } else {
        Element element = elements[node];
        if (element.kind === ElementKind.FUNCTION) {
          funType = element.computeType(compiler, types);
        } else if (element.kind === ElementKind.FOREIGN) {
          return types.dynamicType;
        } else {
          fail(node, 'unexpected element kind ${element.kind}');
        }
      }
      Link<Type> parameterTypes = funType.parameterTypes;
      Link<Node> argumentNodes = node.arguments;
      while (!argumentTypes.isEmpty() && !parameterTypes.isEmpty()) {
        checkAssignable(argumentNodes.head, parameterTypes.head,
                        argumentTypes.head);
        argumentTypes = argumentTypes.tail;
        parameterTypes = parameterTypes.tail;
        argumentNodes = argumentNodes.tail;
      }
      if (!argumentTypes.isEmpty()) {
        reportTypeWarning(argumentNodes.head, MessageKind.ADDITIONAL_ARGUMENT);
      } else if (!parameterTypes.isEmpty()) {
        reportTypeWarning(node, MessageKind.MISSING_ARGUMENT,
                          [parameterTypes.head]);
      }
      return funType.returnType;
    }
  }

  visitSendSet(SendSet node) {
    Identifier selector = node.selector;
    final name = node.assignmentOperator.source.stringValue;
    if (name === '++' || name === '--') {
      final Element element = elements[node.selector];
      final Type receiverType = computeType(element);
      // TODO(karlklose): this should be the return type instead of int.
      return node.isPrefix ? intType : receiverType;
    } else {
      Type targetType = computeType(elements[node]);
      Node value = node.arguments.head;
      checkAssignable(value, targetType, type(value));
      return targetType;
    }
  }

  Type visitLiteralInt(LiteralInt node) {
    return intType;
  }

  Type visitLiteralDouble(LiteralDouble node) {
    return doubleType;
  }

  Type visitLiteralBool(LiteralBool node) {
    return boolType;
  }

  Type visitLiteralString(LiteralString node) {
    return stringType;
  }

  Type visitLiteralNull(LiteralNull node) {
    return types.dynamicType;
  }

  Type visitNewExpression(NewExpression node) {
    // TODO(karlklose): return the type.
    return types.dynamicType;
  }

  Type visitLiteralList(LiteralList node) {
    return types.dynamicType;
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
        reportTypeWarning(expression, MessageKind.RETURN_VALUE_IN_VOID,
                          [expressionType]);
      } else {
        checkAssignable(expression, expectedReturnType, expressionType);
      }

    // Let f be the function immediately enclosing a return statement of the
    // form 'return;' It is a static warning if both of the following conditions
    // hold:
    // - f is not a generative constructor.
    // - The return type of f may not be assigned to void.
    } else if (!types.isAssignable(expectedReturnType, types.voidType)) {
      reportTypeWarning(node, MessageKind.RETURN_NOTHING, [expectedReturnType]);
    }
    return null;
  }

  Type visitThrow(Throw node) {
    if (node.expression !== null) type(node.expression);
    return types.voidType;
  }

  Type computeType(Element element) {
    if (element === null) return types.dynamicType;
    return element.computeType(compiler, types);
  }

  Type visitTypeAnnotation(TypeAnnotation node) {
    if (node.typeName === null) return types.dynamicType;
    final name = node.typeName.source;
    Type type = computeType(elements[node]);
    if (type === null) type = types.lookup(name);
    if (type === null) {
      // The type name cannot be resolved, but the resolver
      // already gave a warning, so we continue checking.
      return types.dynamicType;
    }
    return type;
  }

  Type visitVariableDefinitions(VariableDefinitions node) {
    Type type = typeWithDefault(node.type, types.dynamicType);
    if (type == types.voidType) {
      reportTypeWarning(node.type, MessageKind.VOID_VARIABLE);
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

  Type visitWhile(While node) {
    checkCondition(node.condition);
    type(node.body);
  }

  Type visitParenthesizedExpression(ParenthesizedExpression node) {
    return type(node.expression);
  }
}
