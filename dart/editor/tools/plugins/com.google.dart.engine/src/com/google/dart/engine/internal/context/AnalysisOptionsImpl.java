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
   * The default value for enabling async support.
   */
  public static boolean DEFAULT_ENABLE_ASYNC = false;

  /**
   * The default value for enabling deferred loading.
   */
  public static boolean DEFAULT_ENABLE_DEFERRED_LOADING = true;

  /**
   * The default value for enabling enum support.
   */
  public static boolean DEFAULT_ENABLE_ENUM = false;

  /**
   * A flag indicating whether analysis is to analyze Angular.
   */
  private boolean analyzeAngular = true;

  /**
   * A flag indicating whether analysis is to parse and analyze function bodies.
   */
  private boolean analyzeFunctionBodies = true;

  /**
   * A flag indicating whether analysis is to analyze Polymer.
   */
  private boolean analyzePolymer = true;

  /**
   * The maximum number of sources for which AST structures should be kept in the cache.
   */
  private int cacheSize = DEFAULT_CACHE_SIZE;

  /**
   * A flag indicating whether analysis is to generate dart2js related hint results.
   */
  private boolean dart2jsHint = true;

  /**
   * A flag indicating whether analysis is to enable async support.
   */
  private boolean enableAsync = DEFAULT_ENABLE_ASYNC;

  /**
   * A flag indicating whether analysis is to enable deferred loading.
   */
  private boolean enableDeferredLoading = DEFAULT_ENABLE_DEFERRED_LOADING;

  /**
   * A flag indicating whether analysis is to enable enum support.
   */
  private boolean enableEnum = DEFAULT_ENABLE_ENUM;

  /**
   * A flag indicating whether errors, warnings and hints should be generated for sources in the
   * SDK.
   */
  private boolean generateSdkErrors = false;

  /**
   * A flag indicating whether analysis is to generate hint results (e.g. type inference based
   * information and pub best practices).
   */
  private boolean hint = true;

  /**
   * A flag indicating whether incremental analysis should be used.
   */
  private boolean incremental = false;

  /**
   * A flag indicating whether analysis is to parse comments.
   */
  private boolean preserveComments = true;

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
    analyzeAngular = options.getAnalyzeAngular();
    analyzeFunctionBodies = options.getAnalyzeFunctionBodies();
    analyzePolymer = options.getAnalyzePolymer();
    cacheSize = options.getCacheSize();
    dart2jsHint = options.getDart2jsHint();
    enableAsync = options.getEnableAsync();
    enableDeferredLoading = options.getEnableDeferredLoading();
    enableEnum = options.getEnableEnum();
    generateSdkErrors = options.getGenerateSdkErrors();
    hint = options.getHint();
    incremental = options.getIncremental();
    preserveComments = options.getPreserveComments();
  }

  @Override
  public boolean getAnalyzeAngular() {
    return analyzeAngular;
  }

  @Override
  public boolean getAnalyzeFunctionBodies() {
    return analyzeFunctionBodies;
  }

  @Override
  public boolean getAnalyzePolymer() {
    return analyzePolymer;
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
  public boolean getEnableAsync() {
    return enableAsync;
  }

  @Override
  public boolean getEnableDeferredLoading() {
    return enableDeferredLoading;
  }

  @Override
  public boolean getEnableEnum() {
    return enableEnum;
  }

  @Override
  public boolean getGenerateSdkErrors() {
    return generateSdkErrors;
  }

  @Override
  public boolean getHint() {
    return hint;
  }

  @Override
  public boolean getIncremental() {
    return incremental;
  }

  @Override
  public boolean getPreserveComments() {
    return preserveComments;
  }

  /**
   * Set whether analysis is to analyze Angular.
   * 
   * @param analyzeAngular {@code true} if analysis is to analyze Angular
   */
  public void setAnalyzeAngular(boolean analyzeAngular) {
    this.analyzeAngular = analyzeAngular;
  }

  /**
   * Set whether analysis is to parse and analyze function bodies.
   * 
   * @param analyzeFunctionBodies {@code true} if analysis is to parse and analyze function bodies
   */
  public void setAnalyzeFunctionBodies(boolean analyzeFunctionBodies) {
    this.analyzeFunctionBodies = analyzeFunctionBodies;
  }

  /**
   * Set whether analysis is to analyze Polymer.
   * 
   * @param analyzePolymer {@code true} if analysis is to analyze Polymer
   */
  public void setAnalyzePolymer(boolean analyzePolymer) {
    this.analyzePolymer = analyzePolymer;
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
   * Set whether async support should be enabled.
   * 
   * @param enableAsync {@code true} if async support should be enabled
   */
  public void setEnableAsync(boolean enableAsync) {
    this.enableAsync = enableAsync;
  }

  /**
   * Set whether deferred loading should be enabled.
   * 
   * @param enableDeferredLoading {@code true} if deferred loading should be enabled
   */
  public void setEnableDeferredLoading(boolean enableDeferredLoading) {
    this.enableDeferredLoading = enableDeferredLoading;
  }

  /**
   * Set whether enum support should be enabled.
   * 
   * @param enableEnum {@code true} if enum support should be enabled
   */
  public void setEnableEnum(boolean enableEnum) {
    this.enableEnum = enableEnum;
  }

  /**
   * Set whether errors, warnings and hints should be generated for sources in the SDK to match the
   * given value.
   * 
   * @param generate {@code true} if errors, warnings and hints should be generated for sources in
   *          the SDK
   */
  public void setGenerateSdkErrors(boolean generate) {
    generateSdkErrors = generate;
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

  /**
   * Set whether incremental analysis should be used.
   * 
   * @param incremental {@code true} if incremental analysis should be used
   */
  public void setIncremental(boolean incremental) {
    this.incremental = incremental;
  }

  /**
   * Set whether analysis is to parse comments.
   * 
   * @param preserveComments {@code true} if analysis is to parse comments
   */
  public void setPreserveComments(boolean preserveComments) {
    this.preserveComments = preserveComments;
  }
}
