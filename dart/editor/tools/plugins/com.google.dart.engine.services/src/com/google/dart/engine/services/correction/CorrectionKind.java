/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.engine.services.correction;

/**
 * Identifier of {@link CorrectionProposal}.
 */
public enum CorrectionKind {
  QA_ADD_PART_DIRECTIVE(30, "Add 'part' directive"),
  QA_ADD_TYPE_ANNOTATION(30, "Add type annotation"),
  QA_ASSIGN_TO_LOCAL_VARIABLE(30, "Assign value to new local variable"),
  QA_CONVERT_INTO_BLOCK_BODY(30, "Convert into block body"),
  QA_CONVERT_INTO_EXPRESSION_BODY(30, "Convert into expression body"),
  QA_CONVERT_INTO_IS_NOT(30, "Convert into is!"),
  QA_CONVERT_INTO_IS_NOT_EMPTY(30, "Convert into 'isNotEmpty'"),
  QA_EXCHANGE_OPERANDS(30, "Exchange operands"),
  QA_EXTRACT_CLASS(30, "Extract class into file '%s'"),
  QA_IMPORT_ADD_SHOW(30, "Add explicit 'show' combinator"),
  QA_INVERT_IF_STATEMENT(30, "Invert 'if' statement"),
  QA_JOIN_IF_WITH_INNER(30, "Join 'if' statement with inner 'if' statement"),
  QA_JOIN_IF_WITH_OUTER(30, "Join 'if' statement with outer 'if' statement"),
  QA_JOIN_VARIABLE_DECLARATION(30, "Join variable declaration"),
  QA_REMOVE_TYPE_ANNOTATION(29, "Remove type annotation"),
  QA_REPLACE_CONDITIONAL_WITH_IF_ELSE(30, "Replace conditional with 'if-else'"),
  QA_REPLACE_IF_ELSE_WITH_CONDITIONAL(30, "Replace 'if-else' with conditional ('c ? x : y')"),
  QA_SPLIT_AND_CONDITION(30, "Split && condition"),
  QA_SPLIT_VARIABLE_DECLARATION(30, "Split variable declaration"),
  QA_SURROUND_WITH_BLOCK(30, "Surround with block"),
  QA_SURROUND_WITH_DO_WHILE(30, "Surround with 'do-while'"),
  QA_SURROUND_WITH_FOR(30, "Surround with 'for'"),
  QA_SURROUND_WITH_FOR_IN(30, "Surround with 'for-in'"),
  QA_SURROUND_WITH_IF(30, "Surround with 'if'"),
  QA_SURROUND_WITH_TRY_CATCH(30, "Surround with 'try-catch'"),
  QA_SURROUND_WITH_TRY_FINALLY(30, "Surround with 'try-finally'"),
  QA_SURROUND_WITH_WHILE(30, "Surround with 'while'"),
  QF_ADD_PACKAGE_DEPENDENCY(50, "Add dependency on package '%s'"),
  QF_ADD_SUPER_CONSTRUCTOR_INVOCATION(50, "Add super constructor %s invocation"),
  QF_CHANGE_TO(51, "Change to '%s'"),
  QF_CHANGE_TO_STATIC_ACCESS(50, "Change access to static using '%s'"),
  QF_CREATE_CLASS(50, CorrectionImage.IMG_CORRECTION_CLASS, "Create class '%s'"),
  QF_CREATE_CONSTRUCTOR(50, "Create constructor '%s'"),
  QF_CREATE_CONSTRUCTOR_SUPER(50, "Create constructor to call %s"),
  QF_CREATE_FUNCTION(49, "Create function '%s'"),
  QF_CREATE_METHOD(50, "Create method '%s'"),
  QF_CREATE_MISSING_OVERRIDES(50, "Create %d missing override(s)"),
  QF_CREATE_NO_SUCH_METHOD(49, "Create 'noSuchMethod' method"),
  QF_CREATE_PART(50, "Create part '%s'"),
  QF_IMPORT_LIBRARY_PREFIX(51, "Use imported library '%s' with prefix '%s'"),
  QF_IMPORT_LIBRARY_PROJECT(51, "Import library '%s'"),
  QF_IMPORT_LIBRARY_SDK(51, "Import library '%s'"),
  QF_IMPORT_LIBRARY_SHOW(51, "Update library '%s' import"),
  QF_INSERT_SEMICOLON(50, "Insert ';'"),
  QF_MAKE_CLASS_ABSTRACT(50, "Make class '%s' abstract"),
  QF_REMOVE_PARAMETERS_IN_GETTER_DECLARATION(50, "Remove parameters in getter declaration"),
  QF_REMOVE_PARENTHESIS_IN_GETTER_INVOCATION(50, "Remove parentheses in getter invocation"),
  QF_REMOVE_UNNECASSARY_CAST(50, "Remove unnecessary cast"),
  QF_REMOVE_UNUSED_IMPORT(50, "Remove unused import"),
  QF_REPLACE_BOOLEAN_WITH_BOOL(50, "Replace 'boolean' with 'bool'"),
  QF_USE_EFFECTIVE_INTEGER_DIVISION(50, "Use effective integer division ~/"),
  QF_USE_EQ_EQ_NULL(50, "Use == null instead of 'is Null'"),
  QF_USE_NOT_EQ_NULL(50, "Use != null instead of 'is! Null'");

  private final int relevance;
  private final CorrectionImage image;
  private final String name;

  private CorrectionKind(int relevance, CorrectionImage image, String name) {
    this.relevance = relevance;
    this.image = image;
    this.name = name;
  }

  private CorrectionKind(int relevance, String message) {
    this(relevance, CorrectionImage.IMG_CORRECTION_CHANGE, message);
  }

  /**
   * @return the image to be displayed in the list of correction proposals.
   */
  public CorrectionImage getImage() {
    return image;
  }

  /**
   * @return the name template used to create the name to be displayed for user.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the relevance of {@link CorrectionProposal} - greater value, higher in the list of
   *         proposals.
   */
  public int getRelevance() {
    return relevance;
  }
}
