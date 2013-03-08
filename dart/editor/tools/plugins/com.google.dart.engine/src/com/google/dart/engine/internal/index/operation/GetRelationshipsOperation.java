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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.source.Source;

/**
 * Instances of the {@link GetRelationshipsOperation} implement an operation used to access the
 * locations that have a specified relationship with a specified element.
 * 
 * @coverage dart.engine.index
 */
public class GetRelationshipsOperation implements IndexOperation {
  private final IndexStore indexStore;
  private final Element element;
  private final Relationship relationship;
  private RelationshipCallback callback;

  /**
   * Initialize a newly created operation that will access the locations that have a specified
   * relationship with a specified element.
   */
  public GetRelationshipsOperation(IndexStore indexStore, Element element,
      Relationship relationship, RelationshipCallback callback) {
    this.indexStore = indexStore;
    this.element = element;
    this.relationship = relationship;
    this.callback = callback;
  }

  @VisibleForTesting
  public RelationshipCallback getCallback() {
    return callback;
  }

  @VisibleForTesting
  public Element getElement() {
    return element;
  }

  @VisibleForTesting
  public Relationship getRelationship() {
    return relationship;
  }

  @Override
  public boolean isQuery() {
    return true;
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
  public boolean removeWhenSourceRemoved(Source source) {
    return false;
  }

  @Override
  public String toString() {
    return "GetRelationships(" + element + ", " + relationship + ")";
  }
}
