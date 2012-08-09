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
package com.google.dart.tools.core.internal.index.store;

import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;

import junit.framework.TestCase;

public class IndexStoreTest extends TestCase {
  public void test_IndexStore_creation() {
    IndexStore index = new IndexStore();
    assertNotNull(index);
  }

  public void test_IndexStore_getAttribute() {
    IndexStore index = new IndexStore();
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Attribute attribute = Attribute.getAttribute("attribute");
    assertNull(index.getAttribute(element, attribute));
  }

  public void test_IndexStore_getRelationship() {
    IndexStore index = new IndexStore();
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location[] locations = index.getRelationships(element, relationship);
    assertNotNull(locations);
    assertEquals(0, locations.length);
  }

  public void test_IndexStore_recordAttribute() {
    IndexStore index = new IndexStore();
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Attribute attribute = Attribute.getAttribute("attribute");
    String value = "first value";
    index.recordAttribute(element, attribute, value);
    assertEquals(value, index.getAttribute(element, attribute));

    value = "second value";
    index.recordAttribute(element, attribute, value);
    assertEquals(value, index.getAttribute(element, attribute));
  }

  public void test_IndexStore_recordRelationship() {
    IndexStore index = new IndexStore();
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location firstLocation = new Location(element, 100, 6, null);
    index.recordRelationship(resource, element, relationship, firstLocation);
    Location[] locations = index.getRelationships(element, relationship);
    assertNotNull(locations);
    assertEquals(1, locations.length);
    assertEquals(firstLocation, locations[0]);

    Location secondLocation = new Location(element, 120, 6, null);
    index.recordRelationship(resource, element, relationship, secondLocation);
    locations = index.getRelationships(element, relationship);
    assertNotNull(locations);
    assertEquals(2, locations.length);
    assertTrue((locations[0] == firstLocation && locations[1] == secondLocation)
        || (locations[0] == secondLocation && locations[1] == firstLocation));
  }

  public void test_IndexStore_removeResource_backward() {
    IndexStore index = new IndexStore();
    Resource resource1 = new Resource("resource1");
    Resource resource2 = new Resource("resource2");
    Element element1 = new Element(resource1, "element1");
    Element element2 = new Element(resource2, "element2");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location location1 = new Location(element1, 100, 6, null);
    Location location2 = new Location(element2, 100, 6, null);
    index.recordRelationship(resource1, element1, relationship, location1);
    index.recordRelationship(resource1, element1, relationship, location2);

    index.removeResource(resource2);

    Location[] locations = index.getRelationships(element1, relationship);
    assertNotNull(locations);
    assertEquals(1, locations.length);
    assertEquals(location1, locations[0]);
  }

  public void test_IndexStore_removeResource_forward() {
    IndexStore index = new IndexStore();
    Resource resource = new Resource("resource");
    Element element = new Element(resource, "element");
    Relationship relationship = Relationship.getRelationship("relationship");
    Location firstLocation = new Location(element, 100, 6, null);
    index.recordRelationship(resource, element, relationship, firstLocation);
    Attribute attribute = Attribute.getAttribute("attribute");
    String value = "value";
    index.recordAttribute(element, attribute, value);

    index.removeResource(resource);

    Location[] locations = index.getRelationships(element, relationship);
    assertNotNull(locations);
    assertEquals(0, locations.length);
    assertNull(index.getAttribute(element, attribute));
  }
}
