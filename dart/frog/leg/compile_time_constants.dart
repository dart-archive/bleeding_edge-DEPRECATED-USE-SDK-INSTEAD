// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The [CompileTimeConstantHandler] keeps track of compile-time constants,
 * initializations of global and static fields, and default values of
 * optional parameters.
 */
class CompileTimeConstantHandler extends CompilerTask {
  // Contains the initial value of fields. Must contain all static and global
  // initializations of used fields. May contain caches for instance fields.
  final Map<VariableElement, Dynamic> initialVariableValues;

  CompileTimeConstantHandler(Compiler compiler)
      : initialVariableValues = new Map<VariableElement, Dynamic>(),
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

  String getJsCodeForVariable(VariableElement element) {
    var value = initialVariableValues[element];
    if (value === null) return "(void 0)";
    if (value is num) return "$value";
    if (value === true) return "true";
    if (value === false) return "false";

    // TODO(floitsch): support more values.
    compiler.unimplemented("CompileTimeConstantHandler.getJsCodeForVariable",
                           node: element.parseNode(compiler));
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
      return constantHandler.compileVariable(element);
    }
    return super.visitSend(send);
  }

  error(Element element) {
    // TODO(floitsch): get the list of constants that are currently compiled
    // and present some kind of stack-trace.
    MessageKind kind = MessageKind.NOT_A_COMPILE_TIME_CONSTANT;
    List arguments = [element.name];
    Node node = element.parseNode(compiler);
    compiler.reportError(node, new CompileTimeConstantError(kind, arguments));
  }
}
