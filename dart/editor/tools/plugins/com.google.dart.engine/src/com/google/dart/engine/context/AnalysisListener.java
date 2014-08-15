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
package com.google.dart.engine.context;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.source.Source;

/**
 * The interface {@code AnalysisListener} defines the behavior of objects that are listening for
 * results being produced by an analysis context.
 */
public interface AnalysisListener {
  /**
   * Reports that a task is about to be performed by the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param taskDescription a human readable description of the task that is about to be performed
   */
  public void aboutToPerformTask(AnalysisContext context, String taskDescription);

  /**
   * Reports that the errors associated with the given source in the given context has been updated
   * to the given errors.
   * 
   * @param context the context in which the new list of errors was produced
   * @param source the source containing the errors that were computed
   * @param errors the errors that were computed
   */
  public void computedErrors(AnalysisContext context, Source source, AnalysisError[] errors);

  /**
   * Reports that the given source is no longer included in the set of sources that are being
   * analyzed by the given analysis context.
   * 
   * @param context the context in which the source is being analyzed
   * @param source the source that is no longer being analyzed
   */
  public void excludedSource(AnalysisContext context, Source source);

  /**
   * Reports that the given source is now included in the set of sources that are being analyzed by
   * the given analysis context.
   * 
   * @param context the context in which the source is being analyzed
   * @param source the source that is now being analyzed
   */
  public void includedSource(AnalysisContext context, Source source);

  /**
   * Reports that the given Dart source was parsed in the given context.
   * 
   * @param context the context in which the source was parsed
   * @param source the source that was parsed
   * @param unit the result of parsing the source in the given context
   */
  public void parsedDart(AnalysisContext context, Source source, CompilationUnit unit);

  /**
   * Reports that the given HTML source was parsed in the given context.
   * 
   * @param context the context in which the source was parsed
   * @param source the source that was parsed
   * @param unit the result of parsing the source in the given context
   */
  public void parsedHtml(AnalysisContext context, Source source, HtmlUnit unit);

  /**
   * Reports that the given Dart source was resolved in the given context.
   * 
   * @param context the context in which the source was resolved
   * @param source the source that was resolved
   * @param unit the result of resolving the source in the given context
   */
  public void resolvedDart(AnalysisContext context, Source source, CompilationUnit unit);

  /**
   * Reports that the given HTML source was resolved in the given context.
   * 
   * @param context the context in which the source was resolved
   * @param source the source that was resolved
   * @param unit the result of resolving the source in the given context
   */
  public void resolvedHtml(AnalysisContext context, Source source, HtmlUnit unit);
}
