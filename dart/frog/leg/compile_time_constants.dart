// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The [CompileTimeConstantHandler] keeps track of compile-time constants, and
 * initializations of global and static fields.
 */
class CompileTimeConstantHandler extends CompilerTask {
  // Contains the initial value of fields. Must contain all static and global
  // initializations of used fields. May contain caches for instance fields.
  final Map<VariableElement, Dynamic> initialFieldValues;

  CompileTimeConstantHandler(Compiler compiler)
      : initialFieldValues = new Map<VariableElement, Dynamic>(),
        super(compiler);
  String get name() => 'CompileTimeConstantHandler';

  /**
   * Compiles the initial value of the given field and stores it in an internal
   * map.
   *
   * [WorkItem] must contain a [VariableElement] refering to a global or
   * static field.
   */
  void compileWorkItem(WorkItem work) {
    assert(work.element.kind == ElementKind.FIELD);
    VariableElement element = work.element;
    // Shortcut if it has already been compiled.
    if (initialFieldValues.containsKey(element)) return;
    compileFieldWithDefinitions(element, work.resolutionTree);
  }

  compileField(VariableElement element) {
    if (initialFieldValues.containsKey(element)) {
      return initialFieldValues[element];
    }
    // TODO(floitsch): keep track of currently compiling elements so that we
    // don't end up in an infinite loop: final x = y; final y = x;
    TreeElements definitions = compiler.analyzeElement(element);
    return compileFieldWithDefinitions(element, definitions);
  }

  compileFieldWithDefinitions(VariableElement element,
                              TreeElements definitions) {
    return measure(() {
      Node node = element.parseNode(compiler, compiler);
      assert(node !== null);
      SendSet assignment = node.asSendSet();
      var value;
      if (assignment === null) {
        // No initial value.
        value = null;
      } else {
        Node right = node.arguments.head;
        CompileTimeConstantEvaluator evaluator =
            new CompileTimeConstantEvaluator(this, definitions, compiler);
        value = evaluator.evaluate(right);
      }
      initialFieldValues[element] = value;
      return value;
    });
  }

  /**
   * Returns a [List] of static non final fields that need to be initialized.
   * The list must be evaluated in order since the fields might depend on each
   * other.
   */
  List<VariableElement> getStaticNonFinalFieldsForEmission() {
    return initialFieldValues.getKeys().filter((element) {
      return !element.isInstanceMember() && !element.modifiers.isFinal();
    });
  }

  /**
   * Returns a [List] of static final fields that need to be initialized. The
   * list must be evaluated in order since the fields might depend on each
   * other.
   */
  List<VariableElement> getStaticFinalFieldsForEmission() {
    return initialFieldValues.getKeys().filter((element) {
      return !element.isInstanceMember() && element.modifiers.isFinal();
    });
  }

  void emitJsCodeForField(VariableElement element, StringBuffer buffer) {
    var value = initialFieldValues[element];
    if (value === null) {
      buffer.add("(void 0)");
    } else if (value === true) {
      buffer.add("true");
    } else if (value === false) {
      buffer.add("false");
    } else if (value is num) {
      buffer.add("$value");
    } else {
      // TODO(floitsch): support more values.
      compiler.unimplemented("CompileTimeConstantHandler.emitJsCodeForField",
                             node: element.parseNode(compiler, compiler));
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
    return literal.value;
  }

  visitSend(Send send) {
    Element element = definitions[send];
    if (element !== null && element.kind == ElementKind.FIELD) {
      if (element.isInstanceMember() ||
          element.modifiers === null ||
          !element.modifiers.isFinal()) {
        error(element);
      }
      return constantHandler.compileField(element);
    }
    return super.visitSend(send);
  }

  error(Element element) {
    // TODO(floitsch): get the list of constants that are currently compiled
    // and present some kind of stack-trace.
    MessageKind kind = MessageKind.NOT_A_COMPILE_TIME_CONSTANT;
    List arguments = [element.name];
    Node node = element.parseNode(compiler, compiler);
    compiler.reportError(node, new CompileTimeConstantError(kind, arguments));
  }
}
