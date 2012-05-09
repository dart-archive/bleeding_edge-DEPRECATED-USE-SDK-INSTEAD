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

import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.IndexStore;

/**
 * Instances of the class <code>RemoveResourceOperation</code> implement an operation that removes
 * from the index any data based on the content of a specified resource.
 */
public class RemoveResourceOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The resource being indexed.
   */
  private Resource resource;

  /**
   * Initialize a newly created operation that will index the specified resource.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param resource the resource being indexed
   */
  public RemoveResourceOperation(IndexStore indexStore, Resource resource) {
    this.indexStore = indexStore;
    this.resource = resource;
  }

  /**
   * Return the resource that was removed.
   * 
   * @return the resource that was removed
   */
  public Resource getResource() {
    return resource;
  }

  @Override
  public void performOperation() {
    synchronized (indexStore) {
      indexStore.removeResource(resource);
    }
  }

  @Override
  public boolean removeWhenResourceRemoved(Resource resource) {
    return false;
  }

  @Override
  public String toString() {
    return "RemoveResource(" + resource + ")";
  }
}
