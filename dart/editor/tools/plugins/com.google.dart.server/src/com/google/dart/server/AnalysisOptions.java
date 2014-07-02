/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server;


/**
 * A set of options controlling what kind of analysis is to be performed.
 * 
 * @coverage dart.server
 */
public class AnalysisOptions {
  /**
   * True if the client wants Angular code to be analyzed.
   */
  private Boolean analyzeAngular;

  /**
   * True if the client wants Polymer code to be analyzed.
   */
  private Boolean analyzePolymer;

  /**
   * True if the client wants to enable support for the proposed async feature.
   */
  private Boolean enableAsync;

  /**
   * True if the client wants to enable support for the proposed deferred loading feature.
   */
  private Boolean enableDeferredLoading;

  /**
   * True if the client wants to enable support for the proposed enum feature.
   */
  private Boolean enableEnums;

  /**
   * True if hints that are specific to dart2js should be generated. This option is ignored if
   * either provideErrors or generateHints is false.
   */
  private Boolean generateDart2jsHints;

  /**
   * True is hints should be generated as part of generating errors and warnings. This option is
   * ignored if provideErrors is false.
   */
  private Boolean generateHints;

  /**
   * Return the analyze angular option, true if the client wants Angular code to be analyzed.
   * 
   * @return the analyze angular option, true if the client wants Angular code to be analyzed
   */
  public Boolean getAnalyzeAngular() {
    return analyzeAngular;
  }

  /**
   * Return the analyze polymer option, true if the client wants Polymer code to be analyzed.
   * 
   * @return the analyze polymer option, true if the client wants Polymer code to be analyzed
   */
  public Boolean getAnalyzePolymer() {
    return analyzePolymer;
  }

  /**
   * Return the enable async option, true if the client wants to enable support for the proposed
   * async feature.
   * 
   * @return the enable async option, true if the client wants to enable support for the proposed
   *         async feature
   */
  public Boolean getEnableAsync() {
    return enableAsync;
  }

  /**
   * Return the enable deferred loading option, true if the client wants to enable support for the
   * proposed deferred loading feature.
   * 
   * @return the enable deferred loading option, true if the client wants to enable support for the
   *         proposed deferred loading feature
   */
  public Boolean getEnableDeferredLoading() {
    return enableDeferredLoading;
  }

  /**
   * Return the enable enums option, true if the client wants to enable support for the proposed
   * enum feature
   * 
   * @return the enable enums option, true if the client wants to enable support for the proposed
   *         enum feature
   */
  public Boolean getEnableEnums() {
    return enableEnums;
  }

  /**
   * Return the generate dart2js hints option, true if hints that are specific to dart2js should be
   * generated. This option is ignored if either provideErrors or generateHints is false.
   * 
   * @return the generate dart2js hints option, true if hints that are specific to dart2js should be
   *         generated
   */
  public Boolean getGenerateDart2jsHints() {
    return generateDart2jsHints;
  }

  /**
   * Return the generate hints option, true is hints should be generated as part of generating
   * errors and warnings. This option is ignored if provideErrors is false.
   * 
   * @return the generate hints option, true is hints should be generated as part of generating
   *         errors and warnings
   */
  public Boolean getGenerateHints() {
    return generateHints;
  }

  /**
   * Set the analyze angular option.
   * 
   * @param analyzeAngular the new value for the analyze angular option
   */
  public void setAnalyzeAngular(Boolean analyzeAngular) {
    this.analyzeAngular = analyzeAngular;
  }

  /**
   * Set the analyze polymer option.
   * 
   * @param analyzePolymer the new value for the analyze polymer option
   */
  public void setAnalyzePolymer(Boolean analyzePolymer) {
    this.analyzePolymer = analyzePolymer;
  }

  /**
   * Set the enable async option.
   * 
   * @param enableAsync the new value for the enable async option
   */
  public void setEnableAsync(Boolean enableAsync) {
    this.enableAsync = enableAsync;
  }

  /**
   * Set the enable deferred loading option.
   * 
   * @param enableDeferredLoading the new value for the enable deferred loading option
   */
  public void setEnableDeferredLoading(Boolean enableDeferredLoading) {
    this.enableDeferredLoading = enableDeferredLoading;
  }

  /**
   * Set the enable enums option.
   * 
   * @param enableEnums the new value for the enable enums option
   */
  public void setEnableEnums(Boolean enableEnums) {
    this.enableEnums = enableEnums;
  }

  /**
   * Set the generate dart2js hints option.
   * 
   * @param generateDart2jsHints the new value for the generate dart2js hints option
   */
  public void setGenerateDart2jsHints(Boolean generateDart2jsHints) {
    this.generateDart2jsHints = generateDart2jsHints;
  }

  /**
   * Set the generate hints option.
   * 
   * @param generateHints the new value for the generate hints option
   */
  public void setGenerateHints(Boolean generateHints) {
    this.generateHints = generateHints;
  }
}
