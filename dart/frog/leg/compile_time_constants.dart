// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(floitsch): finish implementation.
class Constant implements Hashable {
  // TODO(floitsch): remove the direct access to the string.
  final String jsCode;
  Constant(this.jsCode);

  int hashCode() => jsCode.hashCode();
  bool operator ==(var other) {
    if (other is !Constant) return false;
    Constant otherConstant = other;
    return jsCode == otherConstant.jsCode;
  }
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
      return initialVariableValues[element];
    }
    // TODO(floitsch): keep track of currently compiling elements so that we
    // don't end up in an infinite loop: final x = y; final y = x;
    TreeElements definitions = compiler.analyzeElement(element);
    return compileVariableWithDefinitions(element, definitions);
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
        value = null;
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

  compileObjectCreation(Node node, Element constructor, List arguments) {
    if (!arguments.isEmpty()) {
      compiler.unimplemented("CompileTimeConstantHandler with arguments",
                             node: node);
    }
    ClassElement classElement = constructor.enclosingElement;
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
    Namer namer = compiler.namer;
    String instantiation = "new ${namer.isolatePropertyAccess(classElement)}()";
    Constant constant = new Constant(instantiation);
    registerCompileTimeConstant(constant);
    return constant;
  }

  compileListLiteral(Node node, List arguments) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < arguments.length; i++) {
      if (i != 0) buffer.add(", ");
      // TODO(floitsch): canonicalize if the constant is in the
      // [compiledConstant] set.
      writeJsCode(buffer, arguments[i]);
    }
    // TODO(floitsch): do we have to register 'List' as instantiated class?
    String array = "[$buffer]";
    Constant constant = new Constant(array);
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

  StringBuffer writeJsCode(StringBuffer buffer, var value) {
    if (value === null) {
      buffer.add("(void 0)");
    } else if (value is num) {
      if (value.isNaN()) {
        buffer.add("(0/0)");
      } else if (value == double.INFINITY) {
        buffer.add("(1/0)");
      } else if (value == -double.INFINITY) {
        buffer.add("(-1/0)");
      } else {
        buffer.add("($value)");        
      }
    } else if (value === true) {
      buffer.add("true");
    } else if (value === false) {
      buffer.add("false");
    } else if (value is DartString) {
      buffer.add("'");
      writeEscapedString(value, buffer, (reason) {
        compiler.cancel("failed to write escaped string: $value");
      });
      buffer.add("'");
    } else if (value is Constant) {
      Constant constant = value;
      buffer.add(constant.jsCode);
    } else {
      // TODO(floitsch): support more values.
      compiler.unimplemented("CompileTimeConstantHandler writeJsCode",
                             element: element);
    }
    return buffer;    
  }

  StringBuffer writeJsCodeForVariable(StringBuffer buffer,
                                      VariableElement element) {
    var value = initialVariableValues[element];
    if (value is Constant) {
      String name = compiledConstants[value];
      buffer.add("${compiler.namer.ISOLATE}.prototype.$name");
    } else {
      return writeJsCode(buffer, initialVariableValues[element]);      
    }
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
}

class CompileTimeConstantEvaluator extends AbstractVisitor {
  final CompileTimeConstantHandler constantHandler;
  final TreeElements definitions;
  final Compiler compiler;

  CompileTimeConstantEvaluator(this.constantHandler,
                               this.definitions,
                               this.compiler);

  evaluate(Node node) {
    return node.accept(this);
  }

  visitNode(Node node) {
    compiler.unimplemented("CompileTimeConstantEvaluator", node: node);
  }

  visitLiteral(Literal literal) {
    if (literal is LiteralString) {
      assert(literal.asLiteralString().isValidated());
      return literal.asLiteralString().dartString;
    }
    return literal.value;
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
      var receiverValue = evaluate(send.receiver);
      Operator op = send.selector;
      switch (op.source.stringValue) {
        case "-":
          if (receiverValue is !num) error(send);
          return -receiverValue;
        case "~": 
          if (receiverValue is !int) error(send); 
          return ~receiverValue;
        case "!":
          if (receiverValue is !bool) error(send);
          return !receiverValue;
        default:
          error(send);
      }
    } else if (send.isOperator && !send.isPostfix) {
      assert(send.argumentCount() == 1);
      var left = evaluate(send.receiver);
      var right = evaluate(send.argumentsNode.nodes.head);
      String op = send.selector.asOperator().source.stringValue;

      if (op == "==" || op == "===") {
        // We use == instead of === so that non-canonicalized DartStrings can
        // use their equality operator.
        return left == right;
      } else if (op == "!=" || op == "!==") {
        return left != right;
      }
      if (left is num && right is num) {
        switch (op) {
          case "+": return left + right;
          case "-": return left - right;
          case "*": return left * right;
          case "/": return left / right;
          case "~/":
          case "%":
            if (left is int && right is int && right == 0) {
              error(send);
            }
            return op == "~/" ? left ~/ right : left % right;
          case "<": return left < right;
          case "<=": return left <= right;
          case ">": return left > right;
          case ">=": return left >= right;
        }
      }
      if (left is int && right is int) {
        switch (op) {
          case "|": return left | right;
          case "&": return left & right;
          case "<<":
            // TODO(floitsch): find a better way to guard against shifts to the
            // left.
            if (right > 100) error(send);
            if (right < 0) error(send);
            return left << right;
          case ">>":
            if (right < 0) error(send);
            return left >> right;        
          case "^": return left ^ right;        
        }
      }
      if (left is DartString && right is DartString && op == "+") {
        return new ConsDartString(left, right);
      }
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
    return constantHandler.compileObjectCreation(node, definitions[node.send],
                                                 arguments);
  }

  visitLiteralList(LiteralList node) {
    if (!node.isConst()) error(node);
    List arguments = [];
    for (Link<Node> link = node.elements.nodes;
         !link.isEmpty();
         link = link.tail) {
      arguments.add(evaluate(link.head));
    }
    return constantHandler.compileListLiteral(node, arguments);
  }

  error(Node node) {
    // TODO(floitsch): get the list of constants that are currently compiled
    // and present some kind of stack-trace.
    MessageKind kind = MessageKind.NOT_A_COMPILE_TIME_CONSTANT;
    compiler.reportError(node, new CompileTimeConstantError(kind, const []));
  }
}
