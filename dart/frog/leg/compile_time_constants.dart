// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Constant implements Hashable {
  const Constant();

  bool isNull() => false;
  bool isBool() => false;
  bool isTrue() => false;
  bool isFalse() => false;
  bool isInt() => false;
  bool isDouble() => false;
  bool isNum() => false;
  bool isString() => false;
  bool isList() => false;
  bool isMap() => false;
  bool isConstructedObject() => false;
  /** Returns true if the constant is a list, a map or a constructed object. */
  bool isObject() => false;

  abstract void writeJsCode(StringBuffer buffer, ConstantHandler handler);
}

class PrimitiveConstant extends Constant {
  abstract get value();
  const PrimitiveConstant();

  bool operator ==(var other) {
    if (other is !PrimitiveConstant) return false;
    PrimitiveConstant otherPrimitive = other;
    // We use == instead of === so that DartStrings compare correctly.
    return value == otherPrimitive.value;
  }

  String toString() => value.toString();
  abstract DartString toDartString();
}

class NullConstant extends PrimitiveConstant {
  factory NullConstant() => const NullConstant._internal();
  const NullConstant._internal();
  bool isNull() => true;
  get value() => null;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    buffer.add("(void 0)");
  }

  // The magic constant has no meaning. It is just a random value.
  int hashCode() => 785965825;
  DartString toDartString() => const LiteralDartString("null");
}

class NumConstant extends PrimitiveConstant {
  abstract num get value();
  const NumConstant();
  bool isNum() => true;
}

class IntConstant extends NumConstant {
  final int value;
  factory IntConstant(int value) {
    switch(value) {
      case 0: return const IntConstant._internal(0);
      case 1: return const IntConstant._internal(1);
      case 2: return const IntConstant._internal(2);
      case 3: return const IntConstant._internal(3);
      case 4: return const IntConstant._internal(4);
      case 5: return const IntConstant._internal(5);
      case 6: return const IntConstant._internal(6);
      case 7: return const IntConstant._internal(7);
      case 8: return const IntConstant._internal(8);
      case 9: return const IntConstant._internal(9);
      case 10: return const IntConstant._internal(10);
      case -1: return const IntConstant._internal(-1);
      case -2: return const IntConstant._internal(-2);
      default: return new IntConstant._internal(value);
    }
  }
  const IntConstant._internal(this.value);
  bool isInt() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    buffer.add("($value)");
  }

  // We have to override the equality operator so that ints and doubles are
  // treated as separate constants.
  // The is [:!IntConstant:] check at the beginning of the function makes sure
  // that we compare only equal to integer constants.
  bool operator ==(var other) {
    if (other is !IntConstant) return false;
    IntConstant otherInt = other;
    return value == otherInt.value;
  }

  int hashCode() => value.hashCode();
  DartString toDartString() => new DartString.literal(value.toString());
}

class DoubleConstant extends NumConstant {
  final double value;
  factory DoubleConstant(double value) {
    if (value.isNaN()) {
      return const DoubleConstant._internal(double.NAN);
    } else if (value == double.INFINITY) {
      return const DoubleConstant._internal(double.INFINITY);
    } else if (value == -double.INFINITY) {
      return const DoubleConstant._internal(-double.INFINITY);
    } else if (value == 0.0 && !value.isNegative()) {
      return const DoubleConstant._internal(0.0);
    } else if (value == 1.0) {
      return const DoubleConstant._internal(1.0);
    } else {
      return new DoubleConstant._internal(value);
    }
  }
  const DoubleConstant._internal(this.value);
  bool isDouble() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    if (value.isNaN()) {
      buffer.add("(0/0)");
    } else if (value == double.INFINITY) {
      buffer.add("(1/0)");
    } else if (value == -double.INFINITY) {
      buffer.add("(-1/0)");
    } else {
      buffer.add("($value)");
    }
  }

  bool operator ==(var other) {
    if (other is !DoubleConstant) return false;
    DoubleConstant otherDouble = other;
    double otherValue = otherDouble.value;
    if (value == 0.0 && otherValue == 0.0) {
      return value.isNegative() == otherValue.isNegative();
    } else if (value.isNaN()) {
      return otherValue.isNaN();
    } else {
      return value == otherValue;
    }
  }

  int hashCode() => value.hashCode();
  DartString toDartString() => new DartString.literal(value.toString());
}

class BoolConstant extends PrimitiveConstant {
  factory BoolConstant(value) {
    return value ? new TrueConstant() : new FalseConstant();
  }
  const BoolConstant._internal();
  bool isBool() => true;

  BoolConstant unaryFold(String op) {
    if (op == "!") return new BoolConstant(!value);
    return null;
  }

  abstract BoolConstant negate();
}

class TrueConstant extends BoolConstant {
  final bool value = true;

  factory TrueConstant() => const TrueConstant._internal();
  const TrueConstant._internal() : super._internal();
  bool isTrue() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    buffer.add("true");
  }

  FalseConstant negate() => new FalseConstant();

  bool operator ==(var other) => this === other;
  // The magic constant is just a random value. It does not have any
  // significance.
  int hashCode() => 499;
  DartString toDartString() => const LiteralDartString("true");
}

class FalseConstant extends BoolConstant {
  final bool value = false;

  factory FalseConstant() => const FalseConstant._internal();
  const FalseConstant._internal() : super._internal();
  bool isFalse() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    buffer.add("false");
  }

  TrueConstant negate() => new TrueConstant();

  bool operator ==(var other) => this === other;
  // The magic constant is just a random value. It does not have any
  // significance.
  int hashCode() => 536555975;
  DartString toDartString() => const LiteralDartString("false");
}

class StringConstant extends PrimitiveConstant {
  final DartString value;
  int _hashCode;

  StringConstant(this.value) {
    // TODO(floitsch): cache StringConstants.
    // TODO(floitsch): compute hashcode without calling toString() on the
    // DartString.
    _hashCode = value.slowToString().hashCode();
  }
  bool isString() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    buffer.add("'");
    ConstantHandler.writeEscapedString(value, buffer, (reason) {
      throw new CompilerCancelledException(reason);
    });
    buffer.add("'");
  }

  bool operator ==(var other) {
    if (other is !StringConstant) return false;
    StringConstant otherString = other;
    return (_hashCode == otherString._hashCode) && (value == otherString.value);
  }

  int hashCode() => _hashCode;
  DartString toDartString() => value;
}

class ObjectConstant extends Constant {
  final Type type;

  ObjectConstant(this.type);
  bool isObject() => true;
}

class ListConstant extends ObjectConstant {
  final List<Constant> entries;
  int _hashCode;

  ListConstant(Type type, this.entries) : super(type) {
    // TODO(floitsch): create a better hash.
    int hash = 0;
    for (Constant input in entries) hash ^= input.hashCode();
    _hashCode = hash;
  }
  bool isList() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    // TODO(floitsch): we should not need to go through the compiler to make
    // the list constant.
    String isolatePrototype = "${handler.compiler.namer.ISOLATE}.prototype";
    buffer.add("$isolatePrototype.makeConstantList");
    buffer.add("([");
    for (int i = 0; i < entries.length; i++) {
      if (i != 0) buffer.add(", ");
      Constant entry = entries[i];
      if (entry.isObject()) {
        String name = handler.getNameForConstant(entry);
        buffer.add("$isolatePrototype.$name");
      } else {
        entry.writeJsCode(buffer, handler);
      }
    }
    buffer.add("])");
  }

  bool operator ==(var other) {
    if (other is !ListConstant) return false;
    ListConstant otherList = other;
    if (hashCode() != otherList.hashCode()) return false;
    // TODO(floitsch): verify that the types are the same.
    if (entries.length != otherList.entries.length) return false;
    for (int i = 0; i < entries.length; i++) {
      if (entries[i] != otherList.entries[i]) return false;
    }
    return true;
  }

  int hashCode() => _hashCode;
}

class ConstructedConstant extends ObjectConstant {
  final List<Constant> fields;
  int _hashCode;

  ConstructedConstant(Type type, this.fields) : super(type) {
    assert(type !== null);
    // TODO(floitsch): create a better hash.
    int hash = 0;
    for (Constant field in fields) {
      hash ^= field.hashCode();
    }
    hash ^= type.element.hashCode();
    _hashCode = hash;
  }
  bool isConstructedObject() => true;

  void writeJsCode(StringBuffer buffer, ConstantHandler handler) {
    buffer.add("new ");
    buffer.add(handler.getJsConstructor(type.element));
    buffer.add("(");
    String isolatePrototype = "${handler.compiler.namer.ISOLATE}.prototype";
    for (int i = 0; i < fields.length; i++) {
      if (i != 0) buffer.add(", ");
      Constant field = fields[i];
      // TODO(floitsch): share this code with the ListConstant.
      if (field.isObject()) {
        String name = handler.getNameForConstant(entry);
        buffer.add("$isolatePrototype.$name");
      } else {
        field.writeJsCode(buffer, handler);
      }
    }
    buffer.add(")");
  }

  bool operator ==(var otherVar) {
    if (otherVar is !ConstructedConstant) return false;
    ConstructedConstant other = otherVar;
    if (hashCode() != other.hashCode()) return false;
    // TODO(floitsch): verify that the (generic) types are the same.
    if (type.element != other.type.element) return false;
    if (fields.length != other.fields.length) return false;
    for (int i = 0; i < fields.length; i++) {
      if (fields[i] != other.fields[i]) return false;
    }
    return true;
  }

  int hashCode() => _hashCode;
}

/**
 * The [ConstantHandler] keeps track of compile-time constants,
 * initializations of global and static fields, and default values of
 * optional parameters.
 */
class ConstantHandler extends CompilerTask {
  // Contains the initial value of fields. Must contain all static and global
  // initializations of used fields. May contain caches for instance fields.
  final Map<VariableElement, Dynamic> initialVariableValues;

  // Map from compile-time constants to their JS name.
  final Map<Constant, String> compiledConstants;

  ConstantHandler(Compiler compiler)
      : initialVariableValues = new Map<VariableElement, Dynamic>(),
        compiledConstants = new Map<Constant, String>(),
        super(compiler);
  String get name() => 'ConstantHandler';

  void registerCompileTimeConstant(Constant constant) {
    Function ifAbsentThunk = (() => compiler.namer.getFreshGlobalName("CTC"));
    compiledConstants.putIfAbsent(constant, ifAbsentThunk);
  }

  /**
   * Compiles the initial value of the given field and stores it in an internal
   * map.
   *
   * [WorkItem] must contain a [VariableElement] refering to a global or
   * static field.
   */
  void compileWorkItem(WorkItem work) {
    assert(work.element.kind == ElementKind.FIELD
           || work.element.kind == ElementKind.PARAMETER);
    VariableElement element = work.element;
    // Shortcut if it has already been compiled.
    if (initialVariableValues.containsKey(element)) return;
    compileVariableWithDefinitions(element, work.resolutionTree);
  }

  compileVariable(VariableElement element) {
    if (initialVariableValues.containsKey(element)) {
      Constant result = initialVariableValues[element];
      return result;
    }
    // TODO(floitsch): keep track of currently compiling elements so that we
    // don't end up in an infinite loop: final x = y; final y = x;
    TreeElements definitions = compiler.analyzeElement(element);
    Constant constant =  compileVariableWithDefinitions(element, definitions);
    return constant;
  }

  compileVariableWithDefinitions(VariableElement element,
                                 TreeElements definitions) {
    return measure(() {
      Node node = element.parseNode(compiler);
      assert(node !== null);
      SendSet assignment = node.asSendSet();
      var value;
      if (assignment === null) {
        // No initial value.
        value = new NullConstant();
      } else {
        Node right = assignment.arguments.head;
        CompileTimeConstantEvaluator evaluator =
            new CompileTimeConstantEvaluator(this, definitions, compiler);
        value = evaluator.evaluate(right);
      }
      initialVariableValues[element] = value;
      return value;
    });
  }

  ConstructedConstant compileObjectConstruction(Node node,
                                                Type type,
                                                List arguments) {
    if (!arguments.isEmpty()) {
      compiler.unimplemented("ConstantHandler with arguments", node: node);
    }
    ClassElement classElement = type.element;
    for (Element member in classElement.members) {
      if (Elements.isInstanceField(member)) {
        compiler.unimplemented("ConstantHandler with fields", node: node);
      }
    }
    if (classElement.superclass != compiler.coreLibrary.find(Types.OBJECT)) {
      compiler.unimplemented("ConstantHandler with super", node: node);
    }
    compiler.registerInstantiatedClass(classElement);
    Constant constant = new ConstructedConstant(type, arguments);
    registerCompileTimeConstant(constant);
    return constant;
  }

  ListConstant compileListLiteral(Node node,
                                  Type type,
                                  List<Constant> arguments) {
    Constant constant = new ListConstant(type, arguments);
    registerCompileTimeConstant(constant);
    return constant;
  }

  /**
   * Returns a [List] of static non final fields that need to be initialized.
   * The list must be evaluated in order since the fields might depend on each
   * other.
   */
  List<VariableElement> getStaticNonFinalFieldsForEmission() {
    return initialVariableValues.getKeys().filter((element) {
      return element.kind == ElementKind.FIELD
          && !element.isInstanceMember()
          && !element.modifiers.isFinal();
    });
  }

  /**
   * Returns a [List] of static final fields that need to be initialized. The
   * list must be evaluated in order since the fields might depend on each
   * other.
   */
  List<VariableElement> getStaticFinalFieldsForEmission() {
    return initialVariableValues.getKeys().filter((element) {
      return element.kind == ElementKind.FIELD
          && !element.isInstanceMember()
          && element.modifiers.isFinal();
    });
  }

  List<Constant> getConstantsForEmission() {
    return compiledConstants.getKeys();
  }

  String getNameForConstant(Constant constant) {
    return compiledConstants[constant];
  }

  StringBuffer writeJsCode(StringBuffer buffer, Constant value) {
    value.writeJsCode(buffer, this);
    return buffer;
  }

  StringBuffer writeJsCodeForVariable(StringBuffer buffer,
                                      VariableElement element) {
    if (!initialVariableValues.containsKey(element)) {
      buffer.add("(void 0)");
      return buffer;
      // TODO(floitsch): reenable the following lines, once we fixed the rest
      // of the compiler.
      /*
      compiler.internalError("No initial value for given element",
                             element: element);
      */
    }
    Constant constant = initialVariableValues[element];
    if (constant.isObject()) {
      String name = compiledConstants[constant];
      buffer.add("${compiler.namer.ISOLATE}.prototype.$name");
    } else {
      writeJsCode(buffer, constant);
    }
    return buffer;
  }

  /**
   * Write the contents of the quoted string to a [StringBuffer] in
   * a form that is valid as JavaScript string literal content.
   * The string is assumed quoted by single quote characters.
   */
  static void writeEscapedString(DartString string,
                                 StringBuffer buffer,
                                 void cancel(String reason)) {
    Iterator<int> iterator = string.iterator();
    while (iterator.hasNext()) {
      int code = iterator.next();
      if (code === $SQ) {
        buffer.add(@"\'");
      } else if (code === $LF) {
        buffer.add(@'\n');
      } else if (code === $CR) {
        buffer.add(@'\r');
      } else if (code === $LS) {
        // This Unicode line terminator and $PS are invalid in JS string
        // literals.
        buffer.add(@'\u2028');
      } else if (code === $PS) {
        buffer.add(@'\u2029');
      } else if (code === $BACKSLASH) {
        buffer.add(@'\\');
      } else {
        if (code > 0xffff) {
          cancel("Unhandled non-BMP character: U+" + code.toRadixString(16));
        }
        // TODO(lrn): Consider whether all codes above 0x7f really need to
        // be escaped. We build a Dart string here, so it should be a literal
        // stage that converts it to, e.g., UTF-8 for a JS interpreter.
        if (code < 0x20) {
          buffer.add(@'\x');
          if (code < 0x10) buffer.add('0');
          buffer.add(code.toRadixString(16));
        } else if (code >= 0x80) {
          if (code < 0x100) {
            buffer.add(@'\x');
            buffer.add(code.toRadixString(16));
          } else {
            buffer.add(@'\u');
            if (code < 0x1000) {
              buffer.add('0');
            }
            buffer.add(code.toRadixString(16));
          }
        } else {
          buffer.add(new String.fromCharCodes(<int>[code]));
        }
      }
    }
  }

  String getJsConstructor(ClassElement element) {
    return compiler.namer.isolatePropertyAccess(element);
  }
}

class CompileTimeConstantEvaluator extends AbstractVisitor {
  final ConstantHandler constantHandler;
  final TreeElements definitions;
  final Compiler compiler;

  CompileTimeConstantEvaluator(this.constantHandler,
                               this.definitions,
                               this.compiler);

  Constant evaluate(Node node) {
    return node.accept(this);
  }

  visitNode(Node node) {
    compiler.unimplemented("CompileTimeConstantEvaluator", node: node);
  }

  Constant visitLiteralBool(LiteralBool node) {
    // TODO(floitsch): make BoolConstant a factory and cache the two values
    // there.
    return new BoolConstant(node.value);
  }

  Constant visitLiteralDouble(LiteralDouble node) {
    return new DoubleConstant(node.value);
  }

  Constant visitLiteralInt(LiteralInt node) {
    return new IntConstant(node.value);
  }

  Constant visitLiteralList(LiteralList node) {
    if (!node.isConst()) error(node);
    List arguments = [];
    for (Link<Node> link = node.elements.nodes;
         !link.isEmpty();
         link = link.tail) {
      arguments.add(evaluate(link.head));
    }
    // TODO(floitsch): get type from somewhere.
    Type type = null;
    return constantHandler.compileListLiteral(node, type, arguments);
  }

  Constant visitLiteralMap(LiteralMap node) {
    compiler.unimplemented("CompileTimeConstantEvaluator map", node: node);
  }

  Constant visitLiteralNull(LiteralNull node) {
    return new NullConstant();
  }

  Constant visitLiteralString(LiteralString node) {
    return new StringConstant(node.dartString);
  }

  // TODO(floitsch): provide better error-messages.
  visitSend(Send send) {
    Element element = definitions[send];
    if (Elements.isStaticOrTopLevelField(element)) {
      if (element.modifiers === null ||
          !element.modifiers.isFinal()) {
        error(send);
      }
      return constantHandler.compileVariable(element);
    } else if (send.isPrefix) {
      assert(send.isOperator);
      Constant receiverConstant = evaluate(send.receiver);
      Operator op = send.selector;
      Constant folded;
      switch (op.source.stringValue) {
        case "!":
          folded = const NotOperation().fold(receiverConstant);
          break;
        case "-":
          folded = const NegateOperation().fold(receiverConstant);
          break;
        case "~":
          folded = const BitNotOperation().fold(receiverConstant);
          break;
        default:
          compiler.internalError("Unexpected operator.", node: op);
          break;
      }
      if (folded === null) error(send);
      return folded;
    } else if (send.isOperator && !send.isPostfix) {
      assert(send.argumentCount() == 1);
      Constant left = evaluate(send.receiver);
      Constant right = evaluate(send.argumentsNode.nodes.head);
      Operator op = send.selector.asOperator();
      Constant folded;
      switch (op.source.stringValue) {
        case "+":
          if (left.isString() && !right.isString()) {
            // At the moment only compile-time concatenation of two strings is
            // allowed.
            error(send);
          }
          folded = const AddOperation().fold(left, right);
          break;
        case "-":
          folded = const SubtractOperation().fold(left, right);
          break;
        case "*":
          folded = const MultiplyOperation().fold(left, right);
          break;
        case "/":
          folded = const DivideOperation().fold(left, right);
          break;
        case "%":
          folded = const ModuloOperation().fold(left, right);
          break;
        case "~/":
          folded = const TruncatingDivideOperation().fold(left, right);
          break;
        case "|":
          folded = const BitOrOperation().fold(left, right);
          break;
        case "&":
          folded = const BitAndOperation().fold(left, right);
          break;
        case "^":
          folded = const BitXorOperation().fold(left, right);
          break;
        case "<<":
          folded = const ShiftLeftOperation().fold(left, right);
          break;
        case ">>":
          folded = const ShiftRightOperation().fold(left, right);
          break;
        case "<":
          folded = const LessOperation().fold(left, right);
          break;
        case "<=":
          folded = const LessEqualOperation().fold(left, right);
          break;
        case ">":
          folded = const GreaterOperation().fold(left, right);
          break;
        case ">=":
          folded = const GreaterEqualOperation().fold(left, right);
          break;
        case "==":
          folded = const EqualsOperation().fold(left, right);
          break;
        case "===":
          folded = const IdentityOperation().fold(left, right);
          break;
        case "!=":
          BoolConstant areEquals = const EqualsOperation().fold(left, right);
          if (areEquals === null) {
            folded = null;
          } else {
            folded = areEquals.negate();
          }
          break;
        case "!==":
          BoolConstant areIdentical =
              const IdentityOperation().fold(left, right);
          if (areIdentical === null) {
            folded = null;
          } else {
            folded = areIdentical.negate();
          }
          break;
        default:
          compiler.internalError("Unexpected operator.", node: op);
          break;
      }
      if (folded === null) error(send);
      return folded;
    }
    return super.visitSend(send);
  }

  visitSendSet(SendSet node) {
    error(node);
  }

  visitNewExpression(NewExpression node) {
    if (!node.isConst()) error(node);
    Send send = node.send;
    List arguments;
    if (send.arguments.isEmpty()) {
      arguments = const [];
    } else {
      arguments = [];
      for (Link<Node> link = send.arguments;
           !link.isEmpty();
           link = link.tail) {
        arguments.add(evaluate(link.head));
      }
    }
    // TODO(floitsch): get the type from somewhere.
    Element constructorElement = definitions[node.send];
    ClassElement classElement = constructorElement.enclosingElement;
    Type type = new SimpleType(classElement.name, classElement);
    return constantHandler.compileObjectConstruction(node,
                                                     type,
                                                     arguments);
  }

  error(Node node) {
    // TODO(floitsch): get the list of constants that are currently compiled
    // and present some kind of stack-trace.
    MessageKind kind = MessageKind.NOT_A_COMPILE_TIME_CONSTANT;
    compiler.reportError(node, new CompileTimeConstantError(kind, const []));
  }
}
