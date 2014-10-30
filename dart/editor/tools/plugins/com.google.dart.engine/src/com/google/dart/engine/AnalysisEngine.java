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
package com.google.dart.engine;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.cache.PartitionManager;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.engine.utilities.logging.Logger;

/**
 * The unique instance of the class {@code AnalysisEngine} serves as the entry point for the
 * functionality provided by the analysis engine.
 * 
 * @coverage dart.engine
 */
public final class AnalysisEngine {
  /**
   * The suffix used for Dart source files.
   */
  public static final String SUFFIX_DART = "dart";

  /**
   * The short suffix used for HTML files.
   */
  public static final String SUFFIX_HTM = "htm";

  /**
   * The long suffix used for HTML files.
   */
  public static final String SUFFIX_HTML = "html";

  /**
   * The unique instance of this class.
   */
  private static final AnalysisEngine UniqueInstance = new AnalysisEngine();

  /**
   * {@code true} if analysis engine is disabled and throws {@link RuntimeException} when used.
   */
  private static boolean engineDisabled = false;

  /**
   * If engine is disabled, this method throws a {@link RuntimeException} indicating that analysis
   * engine is disabled and should not be called.
   */
  public static void failIfEngineDisabled() {
    if (engineDisabled) {
      throw new RuntimeException("AnalysisEngine is disabled and should not be called");
    }
  }

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static AnalysisEngine getInstance() {
    failIfEngineDisabled();
    return UniqueInstance;
  }

  /**
   * Return {@code true} if the given file name is assumed to contain Dart source code.
   * 
   * @param fileName the name of the file being tested
   * @return {@code true} if the given file name is assumed to contain Dart source code
   */
  public static boolean isDartFileName(String fileName) {
    if (fileName == null) {
      return false;
    }
    return FileUtilities.getExtension(fileName).equalsIgnoreCase(SUFFIX_DART);
  }

  /**
   * Return {@code true} if the given file name is assumed to contain HTML.
   * 
   * @param fileName the name of the file being tested
   * @return {@code true} if the given file name is assumed to contain HTML
   */
  public static boolean isHtmlFileName(String fileName) {
    if (fileName == null) {
      return false;
    }
    String extension = FileUtilities.getExtension(fileName);
    return extension.equalsIgnoreCase(SUFFIX_HTML) || extension.equalsIgnoreCase(SUFFIX_HTM);
  }

  /**
   * Set whether analysis engine is enabled or disabled. If disabled, then analysis engine will
   * throw runtime exceptions when clients try to use it. Analysis engine is enabled by default.
   * 
   * @param engineDisabled {@code true} if analysis engine should be disabled
   */
  public static void setDisableEngine(boolean engineDisabled) {
    AnalysisEngine.engineDisabled = engineDisabled;
  }

  /**
   * The logger that should receive information about errors within the analysis engine.
   */
  private Logger logger = Logger.NULL;

  /**
   * The partition manager being used to manage the shared partitions.
   */
  private PartitionManager partitionManager = new PartitionManager();

  /**
   * A flag indicating whether union types should be used.
   */
  private boolean enableUnionTypes = false;

  /**
   * A flag indicating whether union types should have strict semantics. This option has no effect
   * when {@code enabledUnionTypes} is {@code false}.
   */
  private boolean strictUnionTypes = false;

  /**
   * Prevent the creation of instances of this class.
   */
  private AnalysisEngine() {
    super();
  }

  /**
   * Clear any caches holding on to analysis results so that a full re-analysis will be performed
   * the next time an analysis context is created.
   */
  public void clearCaches() {
    partitionManager.clearCache();
  }

  /**
   * Create a new context in which analysis can be performed.
   * 
   * @return the analysis context that was created
   */
  public AnalysisContext createAnalysisContext() {
    //
    // If instrumentation is ignoring data, return an uninstrumented analysis context.
    //
    if (Instrumentation.isNullLogger()) {
      return new AnalysisContextImpl();
    }
    return new InstrumentedAnalysisContextImpl(new AnalysisContextImpl());
  }

  /**
   * Return {@code true} if union types are enabled.
   * 
   * @return {@code true} if union types are enabled.
   */
  public boolean getEnableUnionTypes() {
    return enableUnionTypes;
  }

  /**
   * Return the logger that should receive information about errors within the analysis engine.
   * 
   * @return the logger that should receive information about errors within the analysis engine
   */
  public Logger getLogger() {
    return logger;
  }

  /**
   * Return the partition manager being used to manage the shared partitions.
   * 
   * @return the partition manager being used to manage the shared partitions
   */
  public PartitionManager getPartitionManager() {
    return partitionManager;
  }

  /**
   * Return {@code true} if union types have strict semantics.
   * 
   * @return {@code true} if union types have strict semantics.
   */
  public boolean getStrictUnionTypes() {
    return strictUnionTypes;
  }

  /**
   * Set whether union types should be enabled.
   * 
   * @param enableUnionTypes {@code true} if union types should be enabled.
   */
  public void setEnableUnionTypes(boolean enableUnionTypes) {
    this.enableUnionTypes = enableUnionTypes;
  }

  /**
   * Set the logger that should receive information about errors within the analysis engine to the
   * given logger.
   * 
   * @param logger the logger that should receive information about errors within the analysis
   *          engine
   */
  public void setLogger(Logger logger) {
    this.logger = logger == null ? Logger.NULL : logger;
  }

  /**
   * Set whether union types should be "strict". For example, for strict union types the subtyping
   * relation is {A1,...,An} <: A iff Ai <: i for all i, and the assignment compatibility relation
   * is {A1,...,An} <=> A iff Ai <=> A for all i. For non-strict (permissive) union types, in
   * contrast, the "for all"s are replaced by "there exists".
   * 
   * @param strictUnionTypes
   */
  public void setStrictUnionTypes(boolean strictUnionTypes) {
    this.strictUnionTypes = strictUnionTypes;
  }
}
