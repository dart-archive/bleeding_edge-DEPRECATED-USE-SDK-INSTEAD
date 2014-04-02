package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.element.ParameterElement;

/**
 * @coverage com.google.dart.engine.services.completion
 */
class CompletionState {
  boolean isForMixin;
  boolean isVoidAllowed;
  boolean isDynamicAllowed;
  boolean isSourceDeclarationStatic;
  boolean isThisAllowed = true;
  boolean isVarAllowed;
  boolean areLiteralsAllowed;
  boolean areLiteralsProhibited;
  boolean areOperatorsAllowed;
  boolean areStaticReferencesProhibited;
  boolean areInstanceReferencesProhibited;
  boolean areUndefinedTypesProhibited;
  boolean isCompileTimeConstantRequired;
  boolean isOptionalArgumentRequired;
  boolean areMethodsProhibited;
  boolean areClassesRequired;
  ParameterElement targetParameter;

  public void mustBeInstantiableType() {
    areClassesRequired = true;
    prohibitsLiterals();
  }

  void includesLiterals() {
    if (!areLiteralsProhibited) {
      areLiteralsAllowed = true;
    }
  }

  void includesOperators() {
    areOperatorsAllowed = true;
  }

  void includesUndefinedDeclarationTypes() {
    if (!areUndefinedTypesProhibited) {
      isVoidAllowed = true;
      isDynamicAllowed = true;
    }
  }

  void includesUndefinedTypes() {
    isVarAllowed = true;
    isDynamicAllowed = true;
  }

  void mustBeMixin() {
    isForMixin = true;
  }

  void prohibitsInstanceReferences() {
    areInstanceReferencesProhibited = true;
  }

  void prohibitsLiterals() {
    areLiteralsAllowed = false;
    areLiteralsProhibited = true;
  }

  void prohibitsStaticReferences() {
    areStaticReferencesProhibited = true;
  }

  void prohibitThis() {
    isThisAllowed = false;
  }

  void prohibitsUndefinedTypes() {
    areUndefinedTypesProhibited = true;
  }

  void requiresConst(boolean isConst) {
    isCompileTimeConstantRequired = isConst;
  }

  void requiresOperators() {
    includesOperators();
    areMethodsProhibited = true;
  }

  void requiresOptionalArgument() {
    isOptionalArgumentRequired = true;
    prohibitsLiterals();
  }

  void setContext(AstNode base) {
    base.accept(new ContextAnalyzer(this, base));
  }

  void sourceDeclarationIsStatic(boolean state) {
    isSourceDeclarationStatic = state;
    if (state) {
      if (!areStaticReferencesProhibited) {
        prohibitsInstanceReferences();
      }
    }
  }
}
