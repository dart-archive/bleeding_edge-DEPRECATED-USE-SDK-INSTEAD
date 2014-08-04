/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.engine.internal.index.file;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

public class IndexNodeTest extends TestCase {
  private AnalysisContext context = mock(AnalysisContext.class);
  private StringCodec stringCodec = new StringCodec();
  private ElementCodec elementCodec = mock(ElementCodec.class);
  private int nextElementId = 0;
  private RelationshipCodec relationshipCodec = new RelationshipCodec(stringCodec);
  private IndexNode node = new IndexNode(context, elementCodec, relationshipCodec);

  public void test_getContext() throws Exception {
    assertSame(context, node.getContext());
  }

  public void test_recordRelationship() throws Exception {
    Element elementA = mockElement();
    Element elementB = mockElement();
    Element elementC = mockElement();
    Relationship relationship = Relationship.getRelationship("my-relationship");
    Location locationA = new Location(elementB, 1, 2);
    Location locationB = new Location(elementC, 10, 20);
    // empty initially
    assertEquals(0, node.getLocationCount());
    // record
    node.recordRelationship(elementA, relationship, locationA);
    assertEquals(1, node.getLocationCount());
    node.recordRelationship(elementA, relationship, locationB);
    assertEquals(2, node.getLocationCount());
    // get relations
    assertThat(node.getRelationships(elementB, relationship)).isEmpty();
    {
      Location[] locations = node.getRelationships(elementA, relationship);
      assertThat(locations).hasSize(2);
      boolean hasLocationA = false;
      boolean hasLocationB = false;
      for (Location location : locations) {
        hasLocationA |= location.getOffset() == 1 && location.getLength() == 2;
        hasLocationB |= location.getOffset() == 10 && location.getLength() == 20;
      }
      assertTrue(hasLocationA);
      assertTrue(hasLocationB);
    }
    // verify relations map
    {
      Map<RelationKeyData, List<LocationData>> relations = node.getRelations();
      assertEquals(1, relations.size());
      List<LocationData> locations = relations.values().iterator().next();
      assertEquals(2, locations.size());
    }
  }

  public void test_setRelations() throws Exception {
    Element elementA = mockElement();
    Element elementB = mockElement();
    Element elementC = mockElement();
    Relationship relationship = Relationship.getRelationship("my-relationship");
    // record
    {
      int elementIdA = 0;
      int elementIdB = 1;
      int elementIdC = 2;
      int relationshipId = relationshipCodec.encode(relationship);
      RelationKeyData key = new RelationKeyData(elementIdA, relationshipId);
      List<LocationData> locations = Lists.newArrayList(
          new LocationData(elementIdB, 1, 10),
          new LocationData(elementIdC, 2, 20));
      Map<RelationKeyData, List<LocationData>> relations = ImmutableMap.of(key, locations);
      node.setRelations(relations);
    }
    // request
    Location[] locations = node.getRelationships(elementA, relationship);
    assertThat(locations).hasSize(2);
    assertHasLocation(locations, elementB, 1, 10);
    assertHasLocation(locations, elementC, 2, 20);
  }

  private void assertHasLocation(Location[] locations, Element element, int offset, int length) {
    for (Location location : locations) {
      if (Objects.equal(location.getElement(), element) && location.getOffset() == offset
          && location.getLength() == length) {
        return;
      }
    }
    fail("Expected to find Location(element=" + element + ", offset=" + offset + ", length="
        + length + ")");
  }

  private Element mockElement() {
    int elementId = nextElementId++;
    Element element = mock(Element.class);
    when(elementCodec.encode(element)).thenReturn(elementId);
    when(elementCodec.decode(context, elementId)).thenReturn(element);
    return element;
  }
}
