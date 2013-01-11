/*
 * Copyright (c) 2012, the Dart project authors.
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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.source.Source;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetRelationshipsOperationTest extends EngineTestCase {
  private IndexStore store = mock(IndexStore.class);
  private Element elementLocation = mock(Element.class);
  private RelationshipCallback callback = mock(RelationshipCallback.class);
  private Relationship relationship = Relationship.getRelationship("test-relationship");
  private GetRelationshipsOperation operation = new GetRelationshipsOperation(
      store,
      elementLocation,
      relationship,
      callback);

  public void test_isQuery() throws Exception {
    assertTrue(operation.isQuery());
  }

  public void test_performOperation() throws Exception {
    Location locations[] = new Location[2];
    when(store.getRelationships(elementLocation, relationship)).thenReturn(locations);
    operation.performOperation();
    verify(callback).hasRelationships(elementLocation, relationship, locations);
  }

  public void test_removeWhenSourceRemoved() throws Exception {
    Source source = mock(Source.class);
    assertFalse(operation.removeWhenSourceRemoved(source));
  }

  public void test_toString() throws Exception {
    when(elementLocation.toString()).thenReturn("myElement");
    assertEquals("GetRelationships(myElement, test-relationship)", operation.toString());
  }

}
