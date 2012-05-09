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

import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.RelationshipCallback;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.IndexStore;

/**
 * Instances of the class <code>GetRelationshipsOperation</code> implement an operation used to
 * access the locations that have a specified relationship with a specified element.
 */
public class GetRelationshipsOperation implements IndexOperation {
  /**
   * The index store against which this operation is being run.
   */
  private IndexStore indexStore;

  /**
   * The element that was specified.
   */
  private Element element;

  /**
   * The relationship that was specified.
   */
  private Relationship relationship;

  /**
   * The callback that will be invoked when results are available.
   */
  private RelationshipCallback callback;

  /**
   * Initialize a newly created operation that will access the locations that have a specified
   * relationship with a specified element.
   * 
   * @param indexStore the index store against which this operation is being run
   * @param element the element that was specified
   * @param relationship the relationship that was specified
   * @param callback the listener to which the results will be given
   */
  public GetRelationshipsOperation(IndexStore indexStore, Element element,
      Relationship relationship, RelationshipCallback callback) {
    this.indexStore = indexStore;
    this.element = element;
    this.relationship = relationship;
    this.callback = callback;
  }

  @Override
  public void performOperation() {
    Location[] locations;
    synchronized (indexStore) {
      locations = indexStore.getRelationships(element, relationship);
    }
    callback.hasRelationships(element, relationship, locations);
  }

  @Override
  public boolean removeWhenResourceRemoved(Resource resource) {
    return false;
  }

  @Override
  public String toString() {
    return "GetRelationships(" + element + ", " + relationship + ")";
  }
}
