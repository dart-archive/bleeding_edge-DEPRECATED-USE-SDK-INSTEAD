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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.IncrementalAnalysisCache;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code IncrementalAnalysisTask} incrementally update existing analysis.
 */
public class IncrementalAnalysisTask extends AnalysisTask {

  /**
   * The information used to perform incremental analysis.
   */
  private final IncrementalAnalysisCache cache;

  /**
   * The compilation unit that was produced by incrementally updating the existing unit.
   */
  private CompilationUnit updatedUnit;

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param cache the incremental analysis cache used to perform the analysis
   */
  public IncrementalAnalysisTask(InternalAnalysisContext context, IncrementalAnalysisCache cache) {
    super(context);
    this.cache = cache;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitIncrementalAnalysisTask(this);
  }

  /**
   * Return the compilation unit that was produced by incrementally updating the existing
   * compilation unit, or {@code null} if the task has not yet been performed, could not be
   * performed, or if an exception occurred.
   * 
   * @return the compilation unit
   */
  public CompilationUnit getCompilationUnit() {
    return updatedUnit;
  }

  /**
   * Return the source that is to be incrementally analyzed.
   * 
   * @return the source
   */
  public Source getSource() {
    return cache != null ? cache.getSource() : null;
  }

  @Override
  protected String getTaskDescription() {
    return "incremental analysis " + (cache != null ? cache.getSource() : "null");
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    if (cache == null) {
      return;
    }
    //TODO (danrubel): replace the following with updated AST
    updatedUnit = cache.getResolvedUnit();
  }
}
