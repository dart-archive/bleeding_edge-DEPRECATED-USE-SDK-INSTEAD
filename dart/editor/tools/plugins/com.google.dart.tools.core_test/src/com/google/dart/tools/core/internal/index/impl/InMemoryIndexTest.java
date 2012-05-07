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
package com.google.dart.tools.core.internal.index.impl;

import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Index;
import com.google.dart.tools.core.index.IndexTestUtilities;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.IndexStore;

import junit.framework.TestCase;

import java.lang.reflect.Field;

public class InMemoryIndexTest extends TestCase {
  public void test_InMemoryIndex_creation() {
    Index index = InMemoryIndex.getInstance();
    assertNotNull(index);
  }

  public void test_InMemoryIndex_getAttribute_defined() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Attribute attribute = Attribute.getAttribute("attribute");
    String value = "value";

    store.clear();
    store.recordAttribute(element, attribute, value);

    assertEquals(value, IndexTestUtilities.getAttribute(index, element, attribute));
  }

  public void test_InMemoryIndex_getAttribute_undefined() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Attribute attribute = Attribute.getAttribute("attribute");

    store.clear();
    String result = IndexTestUtilities.getAttribute(index, element, attribute);
    assertNull(result);
  }

  public void test_InMemoryIndex_getRelationships_multiple() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location location1 = new Location(element, 23, 14);
    Location location2 = new Location(element, 45, 14);
    Location location3 = new Location(element, 67, 14);

    store.clear();
    store.recordRelationship(resource, element, relationship, location1);
    store.recordRelationship(resource, element, relationship, location2);
    store.recordRelationship(resource, element, relationship, location3);

    Location[] locations = IndexTestUtilities.getRelationships(index, element, relationship);
    assertEquals(3, locations.length);
  }

  public void test_InMemoryIndex_getRelationships_none() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");

    store.clear();
    Location[] locations = IndexTestUtilities.getRelationships(index, element, relationship);
    assertEquals(0, locations.length);
  }

  public void test_InMemoryIndex_getRelationships_one() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location location = new Location(element, 23, 14);

    store.clear();
    store.recordRelationship(resource, element, relationship, location);

    Location[] locations = IndexTestUtilities.getRelationships(index, element, relationship);
    assertEquals(1, locations.length);
  }

  public void test_InMemoryIndex_removeResource_backward() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource1 = new Resource("resource1");
    Resource resource2 = new Resource("resource2");
    Element element1 = new Element(resource1, "element1");
    Element element2 = new Element(resource2, "element2");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location location1 = new Location(element1, 100, 6);
    Location location2 = new Location(element2, 100, 6);

    store.clear();
    store.recordRelationship(resource1, element1, relationship, location1);
    store.recordRelationship(resource1, element1, relationship, location2);

    store.removeResource(resource2);

    Location[] locations = IndexTestUtilities.getRelationships(index, element1, relationship);
    assertEquals(1, locations.length);
    assertEquals(location1, locations[0]);
  }

  public void test_InMemoryIndex_removeResource_forward() throws Exception {
    Index index = getIndex();
    IndexStore store = getIndexStore(index);
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location firstLocation = new Location(element, 100, 6);

    store.clear();
    store.recordRelationship(resource, element, relationship, firstLocation);
    Attribute attribute = Attribute.getAttribute("attribute");
    String value = "value";
    store.recordAttribute(element, attribute, value);

    store.removeResource(resource);

    Location[] locations = IndexTestUtilities.getRelationships(index, element, relationship);
    assertEquals(0, locations.length);
    String result = IndexTestUtilities.getAttribute(index, element, attribute);
    assertNull(result);
  }

  private Index getIndex() {
    final InMemoryIndex index = InMemoryIndex.getInstance();
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          index.getOperationProcessor().run();
        } catch (Exception exception) {
          // Ignored
        }
      }
    }).start();
    return index;
  }

  /**
   * Access the index store associated with the specified index.
   * 
   * @param index the index whose index store is to be accessed
   * @return the index store associated with the specified index
   * @throws Exception if the index store could not be accessed
   */
  private IndexStore getIndexStore(Index index) throws Exception {
    Field field = index.getClass().getDeclaredField("indexStore");
    field.setAccessible(true);
    return (IndexStore) field.get(index);
  }
}
