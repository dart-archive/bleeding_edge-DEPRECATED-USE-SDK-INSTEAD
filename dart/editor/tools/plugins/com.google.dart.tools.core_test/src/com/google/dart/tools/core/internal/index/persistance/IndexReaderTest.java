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
package com.google.dart.tools.core.internal.index.persistance;

import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.IndexStore;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class IndexReaderTest extends TestCase {
  private static final String ATTRIBUTE_ID_1 = "attribute-1";
  private static final String ATTRIBUTE_VALUE_1 = "attributeValue";
  private static final String ELEMENT_ID_1 = "element1";
  private static final String ELEMENT_ID_2 = "element2";
  private static final String ELEMENT_ID_3 = "element3";
  private static final String RELATIONSHIP_ID_1 = "relationship-1";
  private static final String RELATIONSHIP_ID_2 = "relationship-2";
  private static final String RESOURCE_ID_1 = "file://resource1";
  private static final String RESOURCE_ID_2 = "file://resource2";

  public void test_IndexReader_readIndex_empty() throws Exception {
    IndexStore index = writeAndReadIndex(createEmptyIndex());
    assertTrue(getAttributeMap(index).isEmpty());
    assertTrue(getRelationshipMap(index).isEmpty());
  }

  public void test_IndexReader_readIndex_nonEmpty() throws Exception {
    IndexStore index = writeAndReadIndex(createNonEmptyIndex());

    HashMap<Element, HashMap<Attribute, String>> attributeMap = getAttributeMap(index);
    assertNotNull(attributeMap);
    assertEquals(1, attributeMap.size());
    Element element1 = attributeMap.keySet().iterator().next();
    assertNotNull(element1);
    Resource resource = element1.getResource();
    assertNotNull(resource);
    assertEquals(RESOURCE_ID_1, resource.getResourceId());
    assertEquals(ELEMENT_ID_1, element1.getElementId());
    String value = index.getAttribute(element1, Attribute.getAttribute(ATTRIBUTE_ID_1));
    assertEquals(ATTRIBUTE_VALUE_1, value);

    HashMap<Element, HashMap<Relationship, ArrayList<Location>>> relationshipMap = getRelationshipMap(index);
    assertNotNull(relationshipMap);
    assertEquals(2, relationshipMap.size());
    assertTrue(relationshipMap.containsKey(element1));

    HashMap<Resource, Set<Element>> resourceToElementMap = getResourceToElementMap(index);
    assertNotNull(resourceToElementMap);
    assertEquals(2, resourceToElementMap.size());
  }

  private IndexStore createEmptyIndex() {
    return new IndexStore();
  }

  private IndexStore createNonEmptyIndex() {
    IndexStore index = new IndexStore();
    Resource resource1 = new Resource(RESOURCE_ID_1);
    Resource resource2 = new Resource(RESOURCE_ID_2);
    Element element1 = new Element(resource1, ELEMENT_ID_1);
    Element element2 = new Element(resource1, ELEMENT_ID_2);
    Element element3 = new Element(resource2, ELEMENT_ID_3);
    Relationship relationship1 = Relationship.getRelationship(RELATIONSHIP_ID_1);
    Relationship relationship2 = Relationship.getRelationship(RELATIONSHIP_ID_2);

    index.recordAttribute(element1, Attribute.getAttribute(ATTRIBUTE_ID_1), ATTRIBUTE_VALUE_1);

    index.recordRelationship(resource1, element1, relationship1, new Location(element2, 12, 5, null));
    index.recordRelationship(resource1, element1, relationship2, new Location(element2, 32, 9, null));
    index.recordRelationship(resource2, element2, relationship1, new Location(element3, 12, 5, null));
    return index;
  }

  /**
   * Access the attribute map associated with the specified index.
   * 
   * @param index the index whose attribute map is to be accessed
   * @return the attribute map associated with the specified index
   * @throws Exception if the attribute map could not be accessed
   */
  @SuppressWarnings("unchecked")
  private HashMap<Element, HashMap<Attribute, String>> getAttributeMap(IndexStore index)
      throws Exception {
    Field field = index.getClass().getDeclaredField("attributeMap");
    field.setAccessible(true);
    return (HashMap<Element, HashMap<Attribute, String>>) field.get(index);
  }

  /**
   * Access the relationship map associated with the specified index.
   * 
   * @param index the index whose relationship map is to be accessed
   * @return the relationship map associated with the specified index
   * @throws Exception if the relationship map could not be accessed
   */
  @SuppressWarnings("unchecked")
  private HashMap<Element, HashMap<Relationship, ArrayList<Location>>> getRelationshipMap(
      IndexStore index) throws Exception {
    Field field = index.getClass().getDeclaredField("relationshipMap");
    field.setAccessible(true);
    return (HashMap<Element, HashMap<Relationship, ArrayList<Location>>>) field.get(index);
  }

  /**
   * Access the resource-to-element map associated with the specified index.
   * 
   * @param index the index whose resource-to-element map is to be accessed
   * @return the resource-to-element map associated with the specified index
   * @throws Exception if the resource-to-element map could not be accessed
   */
  @SuppressWarnings("unchecked")
  private HashMap<Resource, Set<Element>> getResourceToElementMap(IndexStore index)
      throws Exception {
    Field field = index.getClass().getDeclaredField("resourceToElementMap");
    field.setAccessible(true);
    return (HashMap<Resource, Set<Element>>) field.get(index);
  }

  private IndexStore writeAndReadIndex(IndexStore originalIndex) throws IOException {
    //
    // Create an external format representing the index.
    //
    IndexWriter writer = originalIndex.createIndexWriter();
    ByteArrayOutputStream baseStream = new ByteArrayOutputStream();
    ObjectOutputStream output = new ObjectOutputStream(baseStream);
    writer.writeIndex(output);
    output.flush();
    output.close();
    byte[] externalFormat = baseStream.toByteArray();
    //
    // Read the external format that was created.
    //
    IndexStore newIndex = new IndexStore();
    IndexReader reader = newIndex.createIndexReader();
    reader.readIndex(new ObjectInputStream(new ByteArrayInputStream(externalFormat)));
    return newIndex;
  }
}
