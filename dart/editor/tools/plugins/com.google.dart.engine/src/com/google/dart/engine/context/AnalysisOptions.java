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
package com.google.dart.engine.context;

/**
 * The interface {@code AnalysisOptions} defines the behavior of objects that provide access to a
 * set of analysis options used to control the behavior of an analysis context.
 */
public interface AnalysisOptions {
  /**
   * Return {@code true} if analysis is to analyze Angular.
   * 
   * @return {@code true} if analysis is to analyze Angular
   */
  public boolean getAnalyzeAngular();

  /**
   * Return {@code true} if analysis is to parse and analyze function bodies.
   * 
   * @return {@code true} if analysis is to parse and analyzer function bodies
   */
  public boolean getAnalyzeFunctionBodies();

  /**
   * Return {@code true} if analysis is to analyze Polymer.
   * 
   * @return {@code true} if analysis is to analyze Polymer
   */
  public boolean getAnalyzePolymer();

  /**
   * Return the maximum number of sources for which AST structures should be kept in the cache.
   * 
   * @return the maximum number of sources for which AST structures should be kept in the cache
   */
  public int getCacheSize();

  /**
   * Return {@code true} if analysis is to generate dart2js related hint results.
   * 
   * @return {@code true} if analysis is to generate dart2js related hint results
   */
  public boolean getDart2jsHint();

  /**
   * Return {@code true} if analysis is to include the new async support.
   * 
   * @return {@code true} if analysis is to include the new async support
   */
  public boolean getEnableAsync();

  /**
   * Return {@code true} if analysis is to include the new deferred loading support.
   * 
   * @return {@code true} if analysis is to include the new deferred loading support
   */
  public boolean getEnableDeferredLoading();

  /**
   * Return {@code true} if analysis is to include the new enum support.
   * 
   * @return {@code true} if analysis is to include the new enum support
   */
  public boolean getEnableEnum();

  /**
   * Return {@code true} if errors, warnings and hints should be generated for sources in the SDK.
   * The default value is {@code false}.
   * 
   * @return {@code true} if errors, warnings and hints should be generated for the SDK
   */
  public boolean getGenerateSdkErrors();

  /**
   * Return {@code true} if analysis is to generate hint results (e.g. type inference based
   * information and pub best practices).
   * 
   * @return {@code true} if analysis is to generate hint results
   */
  public boolean getHint();

  /**
   * Return {@code true} if incremental analysis should be used.
   * 
   * @return {@code true} if incremental analysis should be used
   */
  public boolean getIncremental();

  /**
   * Return {@code true} if analysis is to parse comments.
   * 
   * @return {@code true} if analysis is to parse comments
   */
  public boolean getPreserveComments();
}
