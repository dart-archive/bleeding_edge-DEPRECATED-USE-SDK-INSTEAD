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
package com.google.dart.engine.internal.index;

import com.google.common.collect.ImmutableSet;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

public class MemoryIndexStoreImplTest extends EngineTestCase {
  /**
   * @return the {@link SourceContainer} mock with contains given {@link Source}s.
   */
  private static SourceContainer mockSourceContainer(Source... sources) {
    final Set<Source> sourceSet = ImmutableSet.<Source> builder().add(sources).build();
    SourceContainer container = mock(SourceContainer.class);
    when(container.contains(any(Source.class))).then(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        return sourceSet.contains(invocation.getArguments()[0]);
      }
    });
    return container;
  }

  private MemoryIndexStoreImpl store = new MemoryIndexStoreImpl();
  private AnalysisContext contextA = mock(AnalysisContext.class);
  private AnalysisContext contextB = mock(AnalysisContext.class);
  private AnalysisContext contextC = mock(AnalysisContext.class);
  private ElementLocation elementLocationA = mock(ElementLocation.class);
  private ElementLocation elementLocationB = mock(ElementLocation.class);
  private ElementLocation elementLocationC = mock(ElementLocation.class);
  private ElementLocation elementLocationD = mock(ElementLocation.class);
  private Element elementA = mock(Element.class);
  private Element elementB = mock(Element.class);
  private Element elementC = mock(Element.class);
  private Element elementD = mock(Element.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);
  private Source sourceC = mock(Source.class);
  private Source sourceD = mock(Source.class);
  private CompilationUnitElement unitElementA = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementB = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementC = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementD = mock(CompilationUnitElement.class);
  private Relationship relationship = Relationship.getRelationship("test-relationship");

  private Location location = mock(Location.class);

  public void test_clear() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    assertEquals(1, store.getElementCount());
    assertEquals(1, store.getRelationshipCount());
    // clear
    store.clear();
    assertEquals(0, store.getElementCount());
    assertEquals(0, store.getRelationshipCount());
  }

  public void test_findSource_ClassElement() throws Exception {
    Source source = mock(Source.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unitElement.getSource()).thenReturn(source);
    ClassElement classElement = mock(ClassElement.class);
    when(classElement.getEnclosingElement()).thenReturn(unitElement);
    // validate
    assertSame(source, MemoryIndexStoreImpl.findSource(unitElement));
  }

  public void test_findSource_CompilationUnitElement() throws Exception {
    Source source = mock(Source.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unitElement.getSource()).thenReturn(source);
    // validate
    assertSame(source, MemoryIndexStoreImpl.findSource(unitElement));
  }

  public void test_findSource_LibraryElement_noDefiningUnit() throws Exception {
    LibraryElement libraryElement = mock(LibraryElement.class);
    // validate
    assertSame(null, MemoryIndexStoreImpl.findSource(libraryElement));
  }

  public void test_findSource_LibraryElement_withDefiningUnit() throws Exception {
    Source source = mock(Source.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    LibraryElement libraryElement = mock(LibraryElement.class);
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(source);
    // validate
    assertSame(source, MemoryIndexStoreImpl.findSource(libraryElement));
  }

  public void test_findSource_null() throws Exception {
    assertSame(null, MemoryIndexStoreImpl.findSource(null));
  }

  public void test_getElementCount() throws Exception {
    Relationship relationshipA = Relationship.getRelationship("test-A");
    Relationship relationshipB = Relationship.getRelationship("test-B");
    assertEquals(0, store.getElementCount());
    // add for A
    store.recordRelationship(elementA, relationshipA, location);
    assertEquals(1, store.getElementCount());
    // one more for A, still 1 element
    store.recordRelationship(elementA, relationshipB, location);
    assertEquals(1, store.getElementCount());
    // add for B, now 2 elements
    store.recordRelationship(elementB, relationshipA, location);
    assertEquals(2, store.getElementCount());
  }

  public void test_getRelationships_hasOne() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(location);
  }

  public void test_getRelationships_hasTwo() throws Exception {
    Location locationA = mock(Location.class);
    Location locationB = mock(Location.class);
    when(locationA.getElement()).thenReturn(elementA);
    when(locationB.getElement()).thenReturn(elementB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(locationA, locationB);
  }

  public void test_getRelationships_noRelations() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    Location[] locations = store.getRelationships(
        elementA,
        Relationship.getRelationship("no-such-relationship"));
    assertThat(locations).isEmpty();
  }

  public void test_getRelationships_twoContexts_oneSource() throws Exception {
    when(unitElementB.getSource()).thenReturn(sourceB);
    when(unitElementC.getSource()).thenReturn(sourceB);
    when(elementA.getContext()).thenReturn(contextA);
    when(elementB.getContext()).thenReturn(contextB);
    Location locationA = mock(Location.class);
    Location locationB = mock(Location.class);
    when(locationA.getElement()).thenReturn(elementA);
    when(locationB.getElement()).thenReturn(elementB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementB, relationship, locationB);
    // separate contexts
    assertEquals(1, store.getLocationCount(contextA));
    assertEquals(1, store.getLocationCount(contextB));
    // "elementA"
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationA);
    }
    // "elementB"
    {
      Location[] locations = store.getRelationships(elementB, relationship);
      assertThat(locations).containsOnly(locationB);
    }
  }

  public void test_recordRelationship() throws Exception {
    // no relationships initially
    assertEquals(0, store.getRelationshipCount());
    // record relationship
    store.recordRelationship(elementA, relationship, location);
    assertEquals(1, store.getRelationshipCount());
  }

  public void test_recordRelationship_noElement() throws Exception {
    store.recordRelationship(null, relationship, location);
    assertEquals(0, store.getRelationshipCount());
  }

  public void test_recordRelationship_noLocation() throws Exception {
    store.recordRelationship(elementA, relationship, null);
    assertEquals(0, store.getRelationshipCount());
  }

  public void test_removeContext_withDeclaration() throws Exception {
    when(elementB.getContext()).thenReturn(contextB);
    when(elementC.getContext()).thenReturn(contextC);
    // configure B and C
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      assertEquals(2, store.getDeclarationCount(contextA));
      assertEquals(1, store.getLocationCount(contextB));
      assertEquals(1, store.getLocationCount(contextC));
      // we get locations from all contexts
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove A, so no relations anymore
    // remove B, 1 relation and 1 location left
    store.removeContext(contextA);
    assertEquals(0, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount(contextB));
    assertEquals(0, store.getLocationCount(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).isEmpty();
    }
  }

  public void test_removeContext_withRelationship() throws Exception {
    when(elementB.getContext()).thenReturn(contextB);
    when(elementC.getContext()).thenReturn(contextC);
    // configure B and C
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      assertEquals(2, store.getDeclarationCount(contextA));
      assertEquals(1, store.getLocationCount(contextB));
      assertEquals(1, store.getLocationCount(contextC));
      // we get locations from all contexts
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove B, 1 relation and 1 location left
    store.removeContext(contextB);
    assertEquals(1, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount(contextB));
    assertEquals(1, store.getLocationCount(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationC);
    }
    // now remove C, empty
    store.removeContext(contextC);
    assertEquals(0, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount(contextB));
    assertEquals(0, store.getLocationCount(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).isEmpty();
    }
  }

  public void test_removeSource_withDeclaration() throws Exception {
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove A, no relations and locations
    store.removeSource(contextA, sourceA);
    assertEquals(0, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount(contextA));
  }

  public void test_removeSource_withRelationship() throws Exception {
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove B, 1 relation and 1 location left
    store.removeSource(contextA, sourceB);
    assertEquals(1, store.getRelationshipCount());
    assertEquals(1, store.getLocationCount(contextA));
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(locationC);
  }

  public void test_removeSource_withRelationship_twoContexts_oneSource() throws Exception {
    when(unitElementB.getSource()).thenReturn(sourceB);
    when(unitElementC.getSource()).thenReturn(sourceB);
    when(elementB.getContext()).thenReturn(contextB);
    when(elementC.getContext()).thenReturn(contextC);
    // configure B and C
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      assertEquals(1, store.getLocationCount(contextB));
      assertEquals(1, store.getLocationCount(contextC));
      // we get locations from all contexts
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove "B" in B, 1 relation and 1 location left
    store.removeSource(contextB, sourceB);
    assertEquals(1, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount(contextB));
    assertEquals(1, store.getLocationCount(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationC);
    }
    // now remove "B" in C, empty
    store.removeSource(contextC, sourceB);
    assertEquals(0, store.getRelationshipCount());
    assertEquals(0, store.getLocationCount(contextB));
    assertEquals(0, store.getLocationCount(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).isEmpty();
    }
  }

  public void test_removeSources_withDeclaration() throws Exception {
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A],  [C -> A] and [B -> C]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      store.recordRelationship(elementC, relationship, locationB);
      assertEquals(3, store.getRelationshipCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove container with [A], only [B -> D] left
    SourceContainer containerA = mockSourceContainer(sourceA);
    store.removeSources(contextA, containerA);
    assertEquals(1, store.getRelationshipCount());
    assertEquals(1, store.getLocationCount(contextA));
    {
      Location[] locations = store.getRelationships(elementC, relationship);
      assertThat(locations).containsOnly(locationB);
    }
  }

  public void test_removeSources_withRelationship() throws Exception {
    Location locationB = mock(Location.class);
    Location locationC = mock(Location.class);
    when(locationB.getElement()).thenReturn(elementB);
    when(locationC.getElement()).thenReturn(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.getRelationshipCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).containsOnly(locationB, locationC);
    }
    // remove container with [B], 1 relation and 1 location left
    SourceContainer containerB = mockSourceContainer(sourceB);
    store.removeSources(contextA, containerB);
    assertEquals(1, store.getRelationshipCount());
    assertEquals(1, store.getLocationCount(contextA));
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).containsOnly(locationC);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(location.getElement()).thenReturn(elementC);
    when(elementA.getContext()).thenReturn(contextA);
    when(elementB.getContext()).thenReturn(contextA);
    when(elementC.getContext()).thenReturn(contextA);
    when(elementD.getContext()).thenReturn(contextA);
    when(elementA.getLocation()).thenReturn(elementLocationA);
    when(elementB.getLocation()).thenReturn(elementLocationB);
    when(elementC.getLocation()).thenReturn(elementLocationC);
    when(elementD.getLocation()).thenReturn(elementLocationD);
    when(elementA.getEnclosingElement()).thenReturn(unitElementA);
    when(elementB.getEnclosingElement()).thenReturn(unitElementB);
    when(elementC.getEnclosingElement()).thenReturn(unitElementC);
    when(elementD.getEnclosingElement()).thenReturn(unitElementD);
    when(unitElementA.getSource()).thenReturn(sourceA);
    when(unitElementB.getSource()).thenReturn(sourceB);
    when(unitElementC.getSource()).thenReturn(sourceC);
    when(unitElementD.getSource()).thenReturn(sourceD);
  }
}
