/*
 * Copyright 2014, the Dart project authors.
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

import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.source.Source;

/**
 * Instances of the {@link ClearOperation} implement an operation that removes all of the
 * information from the index.
 * 
 * @coverage dart.engine.index
 */
public class ClearOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  public ClearOperation(IndexStore indexStore) {
    this.indexStore = indexStore;
  }

  @Override
  public boolean isQuery() {
    return false;
  }

  @Override
  public void performOperation() {
    synchronized (indexStore) {
      indexStore.clear();
    }
  }

  @Override
  public boolean removeWhenSourceRemoved(Source source) {
    return false;
  }

  @Override
  public String toString() {
    return "ClearOperation()";
  }
}
