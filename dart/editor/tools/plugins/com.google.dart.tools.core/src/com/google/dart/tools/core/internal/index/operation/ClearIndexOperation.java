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

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.impl.InMemoryIndex;
import com.google.dart.tools.core.internal.index.store.IndexStore;

/**
 * Instances of the class {@code ClearIndexOperation} implement an operation that removes all of the
 * data from the index.
 */
public class ClearIndexOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The runnable to be run after the index has been cleared, or {@code null} if nothing needs to be
   * done.
   */
  private Runnable postClearRunnable;

  /**
   * Initialize a newly created operation that will remove all of the data from the given index.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param postClearRunnable the runnable to be run after the index has been cleared
   */
  public ClearIndexOperation(IndexStore indexStore, Runnable postClearRunnable) {
    this.indexStore = indexStore;
    this.postClearRunnable = postClearRunnable;
  }

  @Override
  public boolean isQuery() {
    return false;
  }

  @Override
  public void performOperation() {
    synchronized (indexStore) {
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        InMemoryIndex.getInstance().logIndexStats("Before clearing the index");
      }
      indexStore.clear();
      if (postClearRunnable != null) {
        postClearRunnable.run();
      }
      if (DartCoreDebug.TRACE_INDEX_STATISTICS) {
        InMemoryIndex.getInstance().logIndexStats("After clearing the index");
      }
    }
  }

  @Override
  public boolean removeWhenResourceRemoved(Resource resource) {
    return false;
  }

  @Override
  public String toString() {
    return "ClearIndex()";
  }
}
