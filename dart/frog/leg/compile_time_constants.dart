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
        compiler.unimplemented("CTC for static initialized fields.",
                               node: node);
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
    // TODO(floitsch): support more values.
    assert(value === null);
    buffer.add("(void 0)");
  }
}
