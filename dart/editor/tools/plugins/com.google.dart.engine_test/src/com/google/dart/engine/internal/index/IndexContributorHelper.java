/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.index;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

public class IndexContributorHelper extends TestCase {
  public static class ExpectedLocation {
    Element element;
    int offset;
    String name;

    public ExpectedLocation(Element element, int offset, String name) {
      this.element = element;
      this.offset = offset;
      this.name = name;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue(element).addValue(offset).addValue(name.length()).toString();
    }
  }

  /**
   * Information about single relation recorded into {@link IndexStore}.
   */
  public static class RecordedRelation {
    final Element element;
    final Relationship relation;
    final Location location;

    public RecordedRelation(Element element, Relationship relation, Location location) {
      this.element = element;
      this.relation = relation;
      this.location = location;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue(element).addValue(relation).addValue(location).toString();
    }
  }

  /**
   * Asserts that actual {@link Location} has given properties.
   */
  public static void assertLocation(Location actual, Element expectedElement, int expectedOffset,
      String expectedNameForLength) {
    assertEquals(expectedElement, actual.getElement());
    assertEquals(expectedOffset, actual.getOffset());
    assertEquals(expectedNameForLength.length(), actual.getLength());
  }

  /**
   * Asserts that given list of {@link RecordedRelation} has no item with specified properties.
   */
  public static void assertNoRecordedRelation(List<RecordedRelation> recordedRelations,
      Element element, Relationship relationship, ExpectedLocation location) {
    for (RecordedRelation recordedRelation : recordedRelations) {
      if (equalsRecordedRelation(recordedRelation, element, relationship, location)) {
        fail("not expected: " + recordedRelation);
      }
    }
  }

  /**
   * Asserts that given list of {@link RecordedRelation} has item with expected properties.
   */
  public static Location assertRecordedRelation(List<RecordedRelation> recordedRelations,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    for (RecordedRelation recordedRelation : recordedRelations) {
      if (equalsRecordedRelation(
          recordedRelation,
          expectedElement,
          expectedRelationship,
          expectedLocation)) {
        return recordedRelation.location;
      }
    }
    fail("not found " + expectedElement + " " + expectedRelationship + " in " + expectedLocation
        + " in\n" + Joiner.on("\n").join(recordedRelations));
    return null;
  }

  /**
   * Asserts that there are two relations with same location.
   */
  public static void assertRecordedRelations(List<RecordedRelation> relations, Element element,
      Relationship r1, Relationship r2, ExpectedLocation expectedLocation) {
    assertRecordedRelation(relations, element, r1, expectedLocation);
    assertRecordedRelation(relations, element, r2, expectedLocation);
  }

  public static List<RecordedRelation> captureRelations(IndexStore store) {
    ArgumentCaptor<Element> argElement = ArgumentCaptor.forClass(Element.class);
    ArgumentCaptor<Relationship> argRel = ArgumentCaptor.forClass(Relationship.class);
    ArgumentCaptor<Location> argLocation = ArgumentCaptor.forClass(Location.class);
    verify(store, atLeast(0)).recordRelationship(
        argElement.capture(),
        argRel.capture(),
        argLocation.capture());
    List<RecordedRelation> relations = Lists.newArrayList();
    int count = argElement.getAllValues().size();
    for (int i = 0; i < count; i++) {
      relations.add(new RecordedRelation(
          argElement.getAllValues().get(i),
          argRel.getAllValues().get(i),
          argLocation.getAllValues().get(i)));
    }
    return relations;
  }

  public static <T extends Element> T mockElement(Class<T> clazz, ElementLocation location,
      int offset, String name) {
    T element = mock(clazz);
    when(element.getLocation()).thenReturn(location);
    when(element.getNameOffset()).thenReturn(offset);
    when(element.getDisplayName()).thenReturn(name);
    return element;
  }

  /**
   * @return {@code true} if given {@link Location} has specified expected properties.
   */
  private static boolean equalsLocation(Location actual, Element expectedElement,
      int expectedOffset, String expectedNameForLength) {
    return Objects.equal(expectedElement, actual.getElement())
        && Objects.equal(expectedOffset, actual.getOffset())
        && Objects.equal(expectedNameForLength.length(), actual.getLength());
  }

  /**
   * @return {@code true} if given {@link Location} has specified expected properties.
   */
  private static boolean equalsLocation(Location actual, ExpectedLocation expected) {
    return equalsLocation(actual, expected.element, expected.offset, expected.name);
  }

  private static boolean equalsRecordedRelation(RecordedRelation recordedRelation,
      Element expectedElement, Relationship expectedRelationship, ExpectedLocation expectedLocation) {
    return Objects.equal(expectedElement, recordedRelation.element)
        && expectedRelationship == recordedRelation.relation
        && (expectedLocation == null || equalsLocation(recordedRelation.location, expectedLocation));
  }
}
