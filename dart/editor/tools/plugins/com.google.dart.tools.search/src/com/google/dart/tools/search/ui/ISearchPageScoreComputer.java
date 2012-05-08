/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.search.ui;

/**
 * Computes a score that is used by the search dialog to find the best fitting page for a selection
 * when opened. The score has no upper limit but must be at least <code>LOWEST</code>. Higher values
 * means the page is better suited for the given selection input.
 * <p>
 * For example, a Java-specific search page score computer could test if the page is a Java search
 * page and returns high scores for Java elements as selection input. Intended to be implemented.
 * </p>
 */
public interface ISearchPageScoreComputer {

  /**
   * Invalid score value indicating a score is unknown or undecided.
   */
  public static final int UNKNOWN = -1;

  /**
   * Lowest possible valid score.
   */
  public static final int LOWEST = 0;

  /**
   * Computes and returns a score indicating how good the page with the given id can handle the
   * given input element. The search page id appears as the <code>id</code> attribute of the
   * <code>&lt;page&gt;</code> element contributed to the search pages extension point (
   * <code>"com.google.dart.tools.search.searchPages"</code>).
   * 
   * @param pageId the string id of the page for which the score is computed
   * @param input the object based on which the page should open
   * @return a score higher or equal to <code>LOWEST</code>, or <code>UNKNOWN</code> if this
   *         computer cannot decide
   */
  public int computeScore(String pageId, Object input);
}
