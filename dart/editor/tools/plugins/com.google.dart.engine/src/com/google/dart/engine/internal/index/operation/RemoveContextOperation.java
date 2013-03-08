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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.source.Source;

/**
 * Instances of the {@link RemoveContextOperation} implement an operation that removes from the
 * index any data based on the specified {@link AnalysisContext}.
 * 
 * @coverage dart.engine.index
 */
public class RemoveContextOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The context being removed.
   */
  private final AnalysisContext context;

  /**
   * Initialize a newly created operation that will remove the specified resource.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param context the {@link AnalysisContext} to remove
   */
  public RemoveContextOperation(IndexStore indexStore, AnalysisContext context) {
    this.indexStore = indexStore;
    this.context = context;
  }

  @VisibleForTesting
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public boolean isQuery() {
    return false;
  }

  @Override
  public void performOperation() {
    synchronized (indexStore) {
      indexStore.removeContext(context);
    }
  }

  @Override
  public boolean removeWhenSourceRemoved(Source source) {
    return false;
  }

  @Override
  public String toString() {
    return "RemoveContext(" + context + ")";
  }
}
