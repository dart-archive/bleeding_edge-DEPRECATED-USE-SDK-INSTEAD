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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.context.AnalysisOptions;

/**
 * Instances of the class {@code AnalysisOptions} represent a set of analysis options used to
 * control the behavior of an analysis context.
 */
public class AnalysisOptionsImpl implements AnalysisOptions {

  /**
   * The maximum number of sources for which data should be kept in the cache.
   */
  public static final int DEFAULT_CACHE_SIZE = 64;

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  private int cacheSize = DEFAULT_CACHE_SIZE;

  /**
   * A flag indicating whether analysis is to generate dart2js related hint results.
   */
  private boolean dart2jsHint = true;

  /**
   * A flag indicating whether analysis is to generate hint results (e.g. type inference based
   * information and pub best practices).
   */
  private boolean hint = true;

  /**
   * Initialize a newly created set of analysis options to have their default values.
   */
  public AnalysisOptionsImpl() {
  }

  /**
   * Initialize a newly created set of analysis options to have the same values as those in the
   * given set of analysis options.
   * 
   * @param options the analysis options whose values are being copied
   */
  public AnalysisOptionsImpl(AnalysisOptions options) {
    cacheSize = options.getCacheSize();
    dart2jsHint = options.getDart2jsHint();
    hint = options.getHint();
  }

  @Override
  public int getCacheSize() {
    return cacheSize;
  }

  @Override
  public boolean getDart2jsHint() {
    return dart2jsHint;
  }

  @Override
  public boolean getHint() {
    return hint;
  }

  /**
   * Set the maximum number of sources for which AST structures should be kept in the cache to the
   * given size.
   * 
   * @param cacheSize the maximum number of sources for which AST structures should be kept in the
   *          cache
   */
  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  /**
   * Set whether analysis is to generate dart2js related hint results.
   * 
   * @param hint {@code true} if analysis is to generate dart2js related hint results
   */
  public void setDart2jsHint(boolean dart2jsHints) {
    this.dart2jsHint = dart2jsHints;
  }

  /**
   * Set whether analysis is to generate hint results (e.g. type inference based information and pub
   * best practices).
   * 
   * @param hint {@code true} if analysis is to generate hint results
   */
  public void setHint(boolean hint) {
    this.hint = hint;
  }
}
