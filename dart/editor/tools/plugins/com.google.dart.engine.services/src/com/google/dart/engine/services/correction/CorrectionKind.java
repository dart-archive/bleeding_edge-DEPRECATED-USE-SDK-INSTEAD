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
  QA_ADD_TYPE_ANNOTATION(30, "Add type annotation"),
  QA_CONVERT_INTO_BLOCK_BODY(30, "Convert into block body"),
  QA_CONVERT_INTO_EXPRESSION_BODY(30, "Convert into expression body"),
  QA_EXCHANGE_OPERANDS(30, "Exchange operands"),
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
  QF_INSERT_SEMICOLON(50, "Insert ';'"),
  QF_REPLACE_BOOLEAN_WITH_BOOL(50, "Replace 'boolean' with 'bool'");

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
   * @return the name to display for user.
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
