// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Constant implements Hashable {
  const Constant();

  bool isNull() => false;
  /** [isInt] implies [isNum]. */
  bool isInt() => false;
  /** [isDouble] implies [isNum]. */
  bool isDouble() => false;
  bool isBool() => false;
  bool isString() => false;
  /** [isList] implies [isObject]. */
  bool isList() => false;
  /** [isMap] implies [isObject]. */
  bool isMap() => false;
  bool isConstructedObject() => false;

  bool isNum() => isInt() || isDouble();
  bool isObject() => isList() || isMap() || isConstructedObject();

  /**
   * Returns [:null:] if the operation is not supported on this constant.
   * The [op] operator is assumed to be a prefix operator.
   */
  Constant unaryFold(String op) => null;

  /**
   * Returns [:null:] if the operation is not supported on this constant, or
   * if the operation would have thrown an exception.
   */
  Constant binaryFold(String op, Constant other) {
    if (op == "==" || op == "===") {
      return new BoolConstant(this == other);
    } else if (op == "!=" || op == "!==") {
      return new BoolConstant(this != other);
    }
  }

  abstract void writeJsCode(StringBuffer buffer,
                            CompileTimeConstantHandler handler);
}

class PrimitiveConstant extends Constant {
  // TODO(floitsch): this should be an abstract getter, but there is a bug in
  // the VM.
  get value() => null;
  const PrimitiveConstant();

  bool operator ==(var other) {
    if (other is !PrimitiveConstant) return false;
    PrimitiveConstant otherPrimitive = other;
    // We use == instead of === so that DartStrings compare correctly. 
    return value == otherPrimitive.value;
  }
}

class NullConstant extends PrimitiveConstant {
  const NullConstant();
  bool isNull() => true;
  get value() => null;

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
    buffer.add("(void 0)");
  }

  // The magic constant has no meaning. It is just a random value.
  int hashCode() => 785965825;
}

class IntConstant extends PrimitiveConstant {
  final int value;
  // TODO(floitsch): cache the most common integer values.
  const IntConstant(this.value);
  bool isInt() => true;

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
    buffer.add("($value)");
  }

  IntConstant unaryFold(String op) {
    if (op == "-") return new IntConstant(-value);
    if (op == "~") return new IntConstant(~value);
    return null;
  }

  Constant binaryFold(String op, Constant other) {
    if (other.isNum()) {
      PrimitiveConstant otherPrimitive = other;
      num rightNum = otherPrimitive.value;
      switch (op) {
        case "<": return new BoolConstant(value < rightNum);
        case "<=": return new BoolConstant(value <= rightNum);
        case ">": return new BoolConstant(value > rightNum);
        case ">=": return new BoolConstant(value >= rightNum);
        case "/": return new DoubleConstant(value / rightNum);
        // We have to treat '==' and '!=' here in case rightNum is a double.
        case "==": return new BoolConstant(value == rightNum);
        case "!=": return new BoolConstant(value != rightNum);
      }
      if (other.isInt()) {
        int right = rightNum;
        switch (op) {
          case "+": return new IntConstant(value + right);
          case "-": return new IntConstant(value - right);
          case "*": return new IntConstant(value * right);
          case "%": return new IntConstant(value % right);
          case "~/": return new IntConstant(value ~/ right);
          case "|": return new IntConstant(value | right);
          case "&": return new IntConstant(value & right);
          case "^": return new IntConstant(value ^ right);        
          case "<<":
            // TODO(floitsch): find a better way to guard against shifts to the
            // left.
            if (right > 100) null;
            if (right < 0) null;
            return new IntConstant(value << right);
          case ">>":
            if (right < 0) return null;
            return new IntConstant(value >> right);
        }
      } else if (other.isDouble()) {
        double right = rightNum;
        switch (op) {
          case "+": return new DoubleConstant(value + right);
          case "-": return new DoubleConstant(value - right);
          case "*": return new DoubleConstant(value * right);
          case "~/": return new DoubleConstant(value ~/ right);
          case "%": return new DoubleConstant(value % right);
        }
      }
    }
    // Visit super in case the [op] was "==", "===", "!=" or "!==".
    return super.binaryFold(op, other);
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
}

class DoubleConstant extends PrimitiveConstant {
  final double value;
  const DoubleConstant(this.value);
  bool isDouble() => true;

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
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

  DoubleConstant unaryFold(String op) {
    if (op == "-") return new DoubleConstant(-value);
    return null;
  }

  Constant binaryFold(String op, Constant other) {
    if (other.isNum()) {
      PrimitiveConstant otherPrimitive = other;
      num right = otherPrimitive.value;
      switch (op) {
        case "<": return new BoolConstant(value < right);
        case "<=": return new BoolConstant(value <= right);
        case ">": return new BoolConstant(value > right);
        case ">=": return new BoolConstant(value >= right);
        case "+": return new DoubleConstant(value + right);
        case "-": return new DoubleConstant(value - right);
        case "*": return new DoubleConstant(value * right);
        case "~/": return new DoubleConstant(value ~/ right);
        case "/": return new DoubleConstant(value / right);
        case "%": return new DoubleConstant(value % right);
        // We have to handle '==' and '!=' here in case right is an integer,
        // or one of the operands is NaN, -0.0 or 0.0.
        case "==": return new BoolConstant(value == right);
        case "!=": return new BoolConstant(value != right);
      }
    }
    // Visit super in case the [op] was "==", "===", "!=" or "!===".
    return super.binaryFold(op, other);
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
}

class BoolConstant extends PrimitiveConstant {
  final bool value;
  const BoolConstant(this.value);
  bool isBool() => true;

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
    buffer.add(value ? "true" : "false");
  }

  BoolConstant unaryFold(String op) {
    if (op == "!") return new BoolConstant(!value);
    return null;
  }

  bool operator ==(var other) {
    if (other is !BoolConstant) return false;
    BoolConstant otherBool = other;
    return value == otherBool.value;
  }

  // The magic constants are just random values. They don't have any
  // significance.
  int hashCode() => value ? 499 : 536555975;
}

class StringConstant extends PrimitiveConstant {
  final DartString value;
  int _hashCode;

  StringConstant(this.value) {
    // TODO(floitsch): compute hashcode without calling toString() on the
    // DartString.
    _hashCode = value.toString().hashCode();
  }
  bool isString() => true;

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
    buffer.add("'");
    CompileTimeConstantHandler.writeEscapedString(value, buffer, (reason) {
      throw new CompilerCancelledException(reason);
    });
    buffer.add("'");
  }

  Constant binaryFold(String op, Constant other) {
    if (other.isString() && op == "+") {
      StringConstant otherString = other;
      DartString right = otherString.value;
      return new StringConstant(new ConsDartString(value, right));
    }
    // Visit super in case the [op] was "==", "===", "!=" or "!===".
    return super.binaryFold(op, other);
  }

  bool operator ==(var other) {
    if (other is !StringConstant) return false;
    StringConstant otherString = other;
    return (_hashCode == otherString._hashCode) && (value == otherString.value);
  }

  int hashCode() => _hashCode;
}

class ObjectConstant extends Constant {
  final Type type;

  ObjectConstant(this.type);
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

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
    // TODO(floitsch): we should not need to go through the compiler to make
    // the list constant.
    buffer.add(handler.compiler.namer.ISOLATE);
    buffer.add(".prototype.makeConstantList");
    buffer.add("([");
    for (int i = 0; i < entries.length; i++) {
      if (i != 0) buffer.add(", ");
      Constant entry = entries[i];
      if (entry.isObject()) {
        handler.getNameForConstant(entry);
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

  void writeJsCode(StringBuffer buffer, CompileTimeConstantHandler handler) {
    buffer.add("new ");
    buffer.add(handler.getJsConstructor(type.element));
    buffer.add("(");
    for (int i = 0; i < fields.length; i++) {
      if (i != 0) buffer.add(", ");
      Constant field = fields[i];
      // TODO(floitsch): share this code with the ListConstant.
      if (field.isObject()) {
        handler.getNameForConstant(field);
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
 * The [CompileTimeConstantHandler] keeps track of compile-time constants,
 * initializations of global and static fields, and default values of
 * optional parameters.
 */
class CompileTimeConstantHandler extends CompilerTask {
  // Contains the initial value of fields. Must contain all static and global
  // initializations of used fields. May contain caches for instance fields.
  final Map<VariableElement, Dynamic> initialVariableValues;

  // Map from compile-time constants to their JS name.
  final Map<Constant, String> compiledConstants;

  CompileTimeConstantHandler(Compiler compiler)
      : initialVariableValues = new Map<VariableElement, Dynamic>(),
        compiledConstants = new Map<Constant, String>(),
        super(compiler);
  String get name() => 'CompileTimeConstantHandler';

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
      // TODO(floitsch): remove the following line once the rest of the
      // compiler has been adapted.
      if (!result.isObject()) return result.dynamic.value;
      return result;
    }
    // TODO(floitsch): keep track of currently compiling elements so that we
    // don't end up in an infinite loop: final x = y; final y = x;
    TreeElements definitions = compiler.analyzeElement(element);
    Constant constant =  compileVariableWithDefinitions(element, definitions);
    // TODO(floitsch): remove the following line once the rest of the
    // compiler has been adapted.
    if (!constant.isObject()) return constant.dynamic.value;
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
        value = const NullConstant();
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
      compiler.unimplemented("CompileTimeConstantHandler with arguments",
                             node: node);
    }
    ClassElement classElement = type.element;
    for (Element member in classElement.members) {
      if (Elements.isInstanceField(member)) {
        compiler.unimplemented("CompileTimeConstantHandler with fields",
                               node: node);
      }
    }
    if (classElement.superclass != compiler.coreLibrary.find(Types.OBJECT)) {
      compiler.unimplemented("CompileTimeConstantHandler with super",
                             node: node);
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
  final CompileTimeConstantHandler constantHandler;
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
    return node.value ? const BoolConstant(true) : const BoolConstant(false);
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
    return const NullConstant();
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
      // TODO(floitsch): compileVariable temporarily returns primitives, so
      // that the rest of the compiler can be adapted incrementally. Therefore
      // we have to get the constant from the hashtable instead of using the
      // returned result directly.
      constantHandler.compileVariable(element);
      return constantHandler.initialVariableValues[element];
    } else if (send.isPrefix) {
      assert(send.isOperator);
      Constant receiverConstant = evaluate(send.receiver);
      Operator op = send.selector;
      Constant folded = receiverConstant.unaryFold(op.source.stringValue);
      if (folded === null) error(send);
      return folded;
    } else if (send.isOperator && !send.isPostfix) {
      assert(send.argumentCount() == 1);
      Constant left = evaluate(send.receiver);
      Constant right = evaluate(send.argumentsNode.nodes.head);
      String op = send.selector.asOperator().source.stringValue;
      Constant folded = left.binaryFold(op, right);
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
