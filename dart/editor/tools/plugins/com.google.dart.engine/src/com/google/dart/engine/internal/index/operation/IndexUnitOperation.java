/*
 * Copyright 2013, the Dart project authors.
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
package com.google.dart.engine.internal.index.operation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.html.angular.AngularDartIndexContributor;
import com.google.dart.engine.internal.index.IndexContributor;
import com.google.dart.engine.source.Source;

/**
 * Instances of the {@link IndexUnitOperation} implement an operation that adds data to the index
 * based on the resolved {@link CompilationUnit}.
 * 
 * @coverage dart.engine.index
 */
public class IndexUnitOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private final IndexStore indexStore;

  /**
   * The context in which compilation unit was resolved.
   */
  private final AnalysisContext context;

  /**
   * The compilation unit being indexed.
   */
  private final CompilationUnit unit;

  /**
   * The element of the compilation unit being indexed.
   */
  private final CompilationUnitElement unitElement;

  /**
   * The source being indexed.
   */
  private final Source source;

  /**
   * Initialize a newly created operation that will index the specified unit.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param context the context in which compilation unit was resolved
   * @param unit the fully resolved AST structure
   */
  public IndexUnitOperation(IndexStore indexStore, AnalysisContext context, CompilationUnit unit) {
    this.indexStore = indexStore;
    this.context = context;
    this.unit = unit;
    this.unitElement = unit.getElement();
    this.source = unitElement.getSource();
  }

  /**
   * @return the {@link Source} to be indexed.
   */
  public Source getSource() {
    return source;
  }

  /**
   * @return the {@link CompilationUnit} to be indexed.
   */
  @VisibleForTesting
  public CompilationUnit getUnit() {
    return unit;
  }

  @Override
  public boolean isQuery() {
    return false;
  }

  @Override
  public void performOperation() {
    synchronized (indexStore) {
      try {
        boolean mayIndex = indexStore.aboutToIndexDart(context, unitElement);
        if (!mayIndex) {
          return;
        }
        unit.accept(new IndexContributor(indexStore));
        unit.accept(new AngularDartIndexContributor(indexStore));
        indexStore.doneIndex();
      } catch (Throwable exception) {
        AnalysisEngine.getInstance().getLogger().logError(
            "Could not index " + unit.getElement().getLocation(),
            exception);
      }
    }
  }

  @Override
  public boolean removeWhenSourceRemoved(Source source) {
    return Objects.equal(this.source, source);
  }

  @Override
  public String toString() {
    return "IndexUnitOperation(" + source.getFullName() + ")";
  }
}
