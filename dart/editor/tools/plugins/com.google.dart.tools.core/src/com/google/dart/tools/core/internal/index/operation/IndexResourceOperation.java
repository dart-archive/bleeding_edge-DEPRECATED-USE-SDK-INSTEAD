/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.operation;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.contributor.IndexContributor;
import com.google.dart.tools.core.internal.index.impl.IndexPerformanceRecorder;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;

/**
 * Instances of the class <code>IndexResourceOperation</code> implement an operation that adds data
 * to the index based on the content of a specified resource.
 */
public class IndexResourceOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The resource being indexed.
   */
  private Resource resource;

  /**
   * The compilation unit being indexed.
   */
  private CompilationUnit compilationUnit;

  /**
   * The fully resolved AST structure representing the contents of the resource.
   */
  private DartUnit unit;

  /**
   * The object used to record performance information about this operation, or <code>null</code> if
   * performance information is not suppose to be recorded.
   */
  private IndexPerformanceRecorder performanceRecorder;

  /**
   * Initialize a newly created operation that will index the specified resource.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param resource the resource being indexed
   * @param compilationUnit the compilation unit being indexed
   * @param unit the fully resolved AST structure representing the contents of the resource
   * @param performanceRecorder the object used to record performance information about this
   *          operation
   */
  public IndexResourceOperation(IndexStore indexStore, Resource resource,
      CompilationUnit compilationUnit, DartUnit unit, IndexPerformanceRecorder performanceRecorder) {
    this.indexStore = indexStore;
    this.resource = resource;
    this.compilationUnit = compilationUnit;
    this.unit = unit;
    this.performanceRecorder = performanceRecorder;
  }

  @Override
  public void performOperation() {
    if (!compilationUnit.exists()) {
      return;
    }
    long indexStart = 0L;
    long indexEnd = 0L;
    long bindingTime = 0L;
    synchronized (indexStore) {
      indexStart = System.currentTimeMillis();
      indexStore.regenerateResource(resource);
      try {
        IndexContributor contributor = new IndexContributor(indexStore, compilationUnit);
        unit.accept(contributor);
        indexEnd = System.currentTimeMillis();
        bindingTime = contributor.getBindingTime();
        if (!(compilationUnit instanceof ExternalCompilationUnitImpl)) {
          contributor.logTrace();
        }
      } catch (DartModelException exception) {
        DartCore.logError(
            "Could not index " + compilationUnit.getResource().getLocation(),
            exception);
      }
    }
    if (performanceRecorder != null && indexEnd > 0L) {
      performanceRecorder.recordIndexingTime(indexEnd - indexStart, bindingTime);
    }
  }

  @Override
  public boolean removeWhenResourceRemoved(Resource resource) {
    return this.resource.equals(resource);
  }

  @Override
  public String toString() {
    return "IndexResource(" + resource + ")";
  }
}
