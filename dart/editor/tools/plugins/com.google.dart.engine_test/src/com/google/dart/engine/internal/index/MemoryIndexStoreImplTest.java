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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.internal.element.member.Member;
import com.google.dart.engine.source.DirectoryBasedSourceContainer;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class MemoryIndexStoreImplTest extends EngineTestCase {
  /**
   * {@link Location} has no "equals" and "hasCode", so to compare locations by value we need to
   * wrap them into such object.
   */
  private static class LocationEqualsWrapper {
    private final Location location;

    LocationEqualsWrapper(Location location) {
      this.location = location;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof LocationEqualsWrapper)) {
        return false;
      }
      LocationEqualsWrapper other = (LocationEqualsWrapper) obj;
      return other.location.getOffset() == location.getOffset()
          && other.location.getLength() == location.getLength()
          && Objects.equal(other.location.getElement(), location.getElement());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(location.getElement(), location.getOffset(), location.getLength());
    }
  }

  /**
   * Asserts that the "actual" locations have all the "expected" locations and only them.
   */
  private static void assertLocations(Location[] actual, Location... expected) {
    LocationEqualsWrapper[] actualWrappers = wrapLocations(actual);
    LocationEqualsWrapper[] expectedWrappers = wrapLocations(expected);
    assertThat(actualWrappers).containsOnly((Object[]) expectedWrappers);
  }

  /**
   * @return the new {@link Location} mock.
   */
  private static Location mockLocation(Element element) {
    Location location = mock(Location.class);
    when(location.newClone()).thenReturn(location);
    when(location.getElement()).thenReturn(element);
    return location;
  }

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

  /**
   * Wraps the given locations into {@link LocationEqualsWrapper}.
   */
  private static LocationEqualsWrapper[] wrapLocations(Location[] locations) {
    List<LocationEqualsWrapper> wrappers = Lists.newArrayList();
    for (Location location : locations) {
      wrappers.add(new LocationEqualsWrapper(location));
    }
    return wrappers.toArray(new LocationEqualsWrapper[wrappers.size()]);
  }

  private MemoryIndexStoreImpl store = new MemoryIndexStoreImpl();
  private AnalysisContext contextA = mock(AnalysisContext.class);
  private AnalysisContext contextB = mock(AnalysisContext.class);
  private AnalysisContext contextC = mock(AnalysisContext.class);
  private ElementLocation elementLocationA = new ElementLocationImpl("elementLocationA");
  private ElementLocation elementLocationB = new ElementLocationImpl("elementLocationB");
  private ElementLocation elementLocationC = new ElementLocationImpl("elementLocationC");
  private ElementLocation elementLocationD = new ElementLocationImpl("elementLocationD");
  private Element elementA = mock(Element.class);
  private Element elementB = mock(Element.class);
  private Element elementC = mock(Element.class);
  private Element elementD = mock(Element.class);
  private Source librarySource = mock(Source.class);
  private Source sourceA = mock(Source.class);
  private Source sourceB = mock(Source.class);
  private Source sourceC = mock(Source.class);
  private Source sourceD = mock(Source.class);
  private LibraryElement libraryElement = mock(LibraryElement.class);
  private CompilationUnitElement libraryUnitElement = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementA = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementB = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementC = mock(CompilationUnitElement.class);
  private CompilationUnitElement unitElementD = mock(CompilationUnitElement.class);
  private Relationship relationship = Relationship.getRelationship("test-relationship");
  private Location location = mockLocation(elementC);

  public void test_aboutToIndex_incompleteResolution_noLibrary() throws Exception {
    when(unitElementA.getLibrary()).thenReturn(null);
    boolean mayIndex = store.aboutToIndexDart(contextA, unitElementA);
    assertFalse(mayIndex);
  }

  public void test_aboutToIndex_incompleteResolution_noLibraryDefiningUnit() throws Exception {
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(null);
    boolean mayIndex = store.aboutToIndexDart(contextA, unitElementA);
    assertFalse(mayIndex);
  }

  public void test_aboutToIndex_incompleteResolution_noUnit() throws Exception {
    boolean mayIndex = store.aboutToIndexDart(contextA, (CompilationUnitElement) null);
    assertFalse(mayIndex);
  }

  public void test_aboutToIndex_removedContext() throws Exception {
    when(contextA.isDisposed()).thenReturn(true);
    store.removeContext(contextA);
    boolean mayIndex = store.aboutToIndexDart(contextA, unitElementA);
    assertFalse(mayIndex);
  }

  public void test_aboutToIndex_sharedSource_inTwoLibraries() throws Exception {
    Source librarySourceA = mock(Source.class);
    Source librarySourceB = mock(Source.class);
    LibraryElement libraryA = mock(LibraryElement.class);
    LibraryElement libraryB = mock(LibraryElement.class);
    CompilationUnitElement libraryAUnit = mock(CompilationUnitElement.class);
    CompilationUnitElement libraryBUnit = mock(CompilationUnitElement.class);
    when(libraryA.getDefiningCompilationUnit()).thenReturn(libraryAUnit);
    when(libraryB.getDefiningCompilationUnit()).thenReturn(libraryBUnit);
    when(libraryA.getSource()).thenReturn(librarySourceA);
    when(libraryB.getSource()).thenReturn(librarySourceB);
    when(libraryAUnit.getSource()).thenReturn(librarySourceA);
    when(libraryBUnit.getSource()).thenReturn(librarySourceB);
    when(libraryAUnit.getLibrary()).thenReturn(libraryA);
    when(libraryBUnit.getLibrary()).thenReturn(libraryB);
    // build 2 units in different libraries
    CompilationUnitElement unitA = mock(CompilationUnitElement.class);
    CompilationUnitElement unitB = mock(CompilationUnitElement.class);
    when(unitA.getContext()).thenReturn(contextA);
    when(unitB.getContext()).thenReturn(contextA);
    when(unitA.getSource()).thenReturn(sourceA);
    when(unitB.getSource()).thenReturn(sourceA);
    when(unitA.getLibrary()).thenReturn(libraryA);
    when(unitB.getLibrary()).thenReturn(libraryB);
    when(libraryA.getParts()).thenReturn(new CompilationUnitElement[] {unitA});
    when(libraryB.getParts()).thenReturn(new CompilationUnitElement[] {unitB});
    // record relationships in both A and B
    Location locationA = mockLocation(unitA);
    Location locationB = mockLocation(unitB);
    store.aboutToIndexDart(contextA, libraryAUnit);
    store.aboutToIndexDart(contextA, libraryBUnit);
    store.aboutToIndexDart(contextA, unitA);
    store.aboutToIndexDart(contextA, unitB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
  }

  public void test_aboutToIndex_shouldRemoveSourceKeys() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    // notify that we are going to re-index "A"
    store.aboutToIndexDart(contextA, unitElementA);
    // all keys in "A" should be removed (and we don't have any other sources)
    assertEquals(0, store.internalGetSourceKeyCount(contextA));
  }

  public void test_aboutToIndex_unitExcluded() throws Exception {
    // build library with defining unit
    Source librarySource = mock(Source.class);
    LibraryElement library = mock(LibraryElement.class);
    CompilationUnitElement libraryUnit = mock(CompilationUnitElement.class);
    when(library.getContext()).thenReturn(contextA);
    when(library.getDefiningCompilationUnit()).thenReturn(libraryUnit);
    when(library.getSource()).thenReturn(librarySource);
    when(libraryUnit.getContext()).thenReturn(contextA);
    when(libraryUnit.getSource()).thenReturn(librarySource);
    when(libraryUnit.getLibrary()).thenReturn(library);
    // build 2 library units
    CompilationUnitElement unitA = mock(CompilationUnitElement.class);
    CompilationUnitElement unitB = mock(CompilationUnitElement.class);
    when(unitA.getContext()).thenReturn(contextA);
    when(unitB.getContext()).thenReturn(contextA);
    when(unitA.getSource()).thenReturn(sourceA);
    when(unitB.getSource()).thenReturn(sourceB);
    when(unitA.getLibrary()).thenReturn(library);
    when(unitB.getLibrary()).thenReturn(library);
    // prepare locations
    Location locationA = mockLocation(unitA);
    Location locationB = mockLocation(unitB);
    // initially A and B in library
    when(library.getParts()).thenReturn(new CompilationUnitElement[] {unitA, unitB});
    store.aboutToIndexDart(contextA, libraryUnit);
    store.aboutToIndexDart(contextA, unitA);
    store.aboutToIndexDart(contextA, unitB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
    // exclude A from library
    when(library.getParts()).thenReturn(new CompilationUnitElement[] {unitA});
    boolean mayIndex = store.aboutToIndexDart(contextA, libraryUnit);
    assertTrue(mayIndex);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA);
    }
    // exclude B from library, empty now
    when(library.getParts()).thenReturn(new CompilationUnitElement[] {});
    store.aboutToIndexDart(contextA, libraryUnit);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations);
    }
  }

  public void test_aboutToIndex_withHtmlElement_removedContext() throws Exception {
    // prepare HtmlElement
    HtmlElement htmlElement = mock(HtmlElement.class);
    when(htmlElement.getContext()).thenReturn(contextA);
    when(htmlElement.getSource()).thenReturn(sourceA);
    // mark context as removed
    when(contextA.isDisposed()).thenReturn(true);
    store.removeContext(contextA);
    // cannot index
    boolean mayIndex = store.aboutToIndexHtml(contextA, htmlElement);
    assertFalse(mayIndex);
  }

  public void test_aboutToIndex_withHtmlElement_shouldRemoveLocations() throws Exception {
    HtmlElement htmlElement = mock(HtmlElement.class);
    when(htmlElement.getContext()).thenReturn(contextA);
    when(htmlElement.getSource()).thenReturn(sourceA);
    location = mockLocation(htmlElement);
    // record location
    store.recordRelationship(elementA, relationship, location);
    assertEquals(1, store.internalGetLocationCountForContext(contextA));
    // notify that we are going to re-index "A"
    store.aboutToIndexHtml(contextA, htmlElement);
    // all locations in "A" should be removed (and we don't have any other sources)
    assertEquals(0, store.internalGetLocationCountForContext(contextA));
  }

  public void test_aboutToIndex_withHtmlElement_shouldRemoveSourceKeys() throws Exception {
    // prepare HtmlElement
    HtmlElement htmlElement = mock(HtmlElement.class);
    when(htmlElement.getContext()).thenReturn(contextA);
    when(htmlElement.getSource()).thenReturn(sourceA);
    // record with HtmlElement as a key
    store.recordRelationship(htmlElement, relationship, location);
    assertEquals(1, store.internalGetSourceKeyCount(contextA));
    // notify that we are going to re-index "A"
    store.aboutToIndexHtml(contextA, htmlElement);
    // all keys in "A" should be removed (and we don't have any other sources)
    assertEquals(0, store.internalGetSourceKeyCount(contextA));
  }

  public void test_clearSource_instrumented() throws Exception {
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // clear B, 1 relation and 1 location left
    InstrumentedAnalysisContextImpl iContextA = mock(InstrumentedAnalysisContextImpl.class);
    when(iContextA.getBasis()).thenReturn(contextA);
    store.aboutToIndexDart(iContextA, unitElementB);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(1, store.internalGetLocationCountForContext(contextA));
    Location[] locations = store.getRelationships(elementA, relationship);
    assertLocations(locations, locationC);
  }

  public void test_getRelationships_hasOne() throws Exception {
    store.recordRelationship(elementA, relationship, location);
    Location[] locations = store.getRelationships(elementA, relationship);
    assertLocations(locations, location);
  }

  public void test_getRelationships_hasTwo() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    Location[] locations = store.getRelationships(elementA, relationship);
    assertLocations(locations, locationA, locationB);
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
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementB, relationship, locationB);
    // "elementA"
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA);
    }
    // "elementB"
    {
      Location[] locations = store.getRelationships(elementB, relationship);
      assertLocations(locations, locationB);
    }
  }

  public void test_getStatistics() throws Exception {
    // empty initially
    assertEquals("0 relationships in 0 keys in 0 sources", store.getStatistics());
    // record relationship
    store.recordRelationship(elementA, relationship, mockLocation(elementA));
    store.recordRelationship(elementA, relationship, mockLocation(elementB));
    store.recordRelationship(elementB, relationship, mockLocation(elementC));
    assertEquals("3 relationships in 2 keys in 2 sources", store.getStatistics());
  }

  public void test_recordRelationship() throws Exception {
    // no relationships initially
    assertEquals(0, store.internalGetLocationCount());
    // record relationship
    store.recordRelationship(elementA, relationship, location);
    assertEquals(1, store.internalGetLocationCount());
  }

  public void test_recordRelationship_member() throws Exception {
    Member member = mock(Member.class);
    when(member.getBaseElement()).thenReturn(elementA);
    // no relationships initially
    assertEquals(0, store.internalGetLocationCount());
    // record relationship
    store.recordRelationship(member, relationship, location);
    // no location for "member"
    {
      Location[] locations = store.getRelationships(member, relationship);
      assertLocations(locations);
    }
    // has location for "elementA"
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, location);
    }
  }

  public void test_recordRelationship_noElement() throws Exception {
    store.recordRelationship(null, relationship, location);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_recordRelationship_noElementContext() throws Exception {
    when(elementA.getContext()).thenReturn(null);
    store.recordRelationship(elementA, relationship, location);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_recordRelationship_noElementSource() throws Exception {
    when(elementA.getSource()).thenReturn(null);
    store.recordRelationship(elementA, relationship, location);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_recordRelationship_noLocation() throws Exception {
    store.recordRelationship(elementA, relationship, null);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_recordRelationship_noLocationContext() throws Exception {
    when(location.getElement().getContext()).thenReturn(null);
    store.recordRelationship(elementA, relationship, location);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_recordRelationship_noLocationSource() throws Exception {
    when(location.getElement().getSource()).thenReturn(null);
    store.recordRelationship(elementA, relationship, location);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_removeContext_instrumented() throws Exception {
    InstrumentedAnalysisContextImpl instrumentedContext = mock(InstrumentedAnalysisContextImpl.class);
    when(instrumentedContext.getBasis()).thenReturn(contextA);
    // configure B
    when(elementB.getContext()).thenReturn(contextA);
    Location locationB = mockLocation(elementB);
    // record: [B -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      assertEquals(1, store.internalGetLocationCount());
      assertEquals(1, store.internalGetKeyCount());
    }
    // remove _wrapper_ of context A
    InstrumentedAnalysisContextImpl iContextA = mock(InstrumentedAnalysisContextImpl.class);
    when(iContextA.getBasis()).thenReturn(contextA);
    store.removeContext(iContextA);
    assertEquals(0, store.internalGetLocationCount());
    assertEquals(0, store.internalGetKeyCount());
  }

  public void test_removeContext_null() throws Exception {
    store.removeContext(null);
  }

  public void test_removeContext_withDeclaration() throws Exception {
    when(elementB.getContext()).thenReturn(contextB);
    when(elementC.getContext()).thenReturn(contextC);
    // configure B and C
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      assertEquals(1, store.internalGetKeyCount());
      assertEquals(0, store.internalGetLocationCountForContext(contextA));
      assertEquals(1, store.internalGetLocationCountForContext(contextB));
      assertEquals(1, store.internalGetLocationCountForContext(contextC));
      // we get locations from all contexts
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove A, so no relations anymore
    // remove B, 1 relation and 1 location left
    store.removeContext(contextA);
    assertEquals(0, store.internalGetLocationCount());
    assertEquals(0, store.internalGetLocationCountForContext(contextA));
    assertEquals(0, store.internalGetLocationCountForContext(contextB));
    assertEquals(0, store.internalGetLocationCountForContext(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).isEmpty();
    }
  }

  public void test_removeContext_withRelationship() throws Exception {
    when(elementB.getContext()).thenReturn(contextB);
    when(elementC.getContext()).thenReturn(contextC);
    // configure B and C
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.aboutToIndexDart(contextB, unitElementB);
      store.aboutToIndexDart(contextC, unitElementC);
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      assertEquals(0, store.internalGetLocationCountForContext(contextA));
      assertEquals(1, store.internalGetLocationCountForContext(contextB));
      assertEquals(1, store.internalGetLocationCountForContext(contextC));
      // we get locations from all contexts
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove B, 1 relation and 1 location left
    store.removeContext(contextB);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(0, store.internalGetLocationCountForContext(contextA));
    assertEquals(0, store.internalGetLocationCountForContext(contextB));
    assertEquals(1, store.internalGetLocationCountForContext(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationC);
    }
    // now remove C, empty
    store.removeContext(contextC);
    assertEquals(0, store.internalGetLocationCountForContext(contextA));
    assertEquals(0, store.internalGetLocationCountForContext(contextB));
    assertEquals(0, store.internalGetLocationCountForContext(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).isEmpty();
    }
  }

  public void test_removeSource_null() throws Exception {
    store.removeSource(null, null);
  }

  public void test_removeSource_withDeclaration() throws Exception {
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove A, no relations
    store.removeSource(contextA, sourceA);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_removeSource_withRelationship() throws Exception {
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove B, 1 relation and 1 location left
    store.removeSource(contextA, sourceB);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(1, store.internalGetLocationCountForContext(contextA));
    Location[] locations = store.getRelationships(elementA, relationship);
    assertLocations(locations, locationC);
  }

  public void test_removeSource_withRelationship_instrumented() throws Exception {
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove B, 1 relation and 1 location left
    InstrumentedAnalysisContextImpl iContextA = mock(InstrumentedAnalysisContextImpl.class);
    when(iContextA.getBasis()).thenReturn(contextA);
    store.removeSource(iContextA, sourceB);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(1, store.internalGetLocationCountForContext(contextA));
    Location[] locations = store.getRelationships(elementA, relationship);
    assertLocations(locations, locationC);
  }

  public void test_removeSource_withRelationship_twoContexts_oneSource() throws Exception {
    when(elementB.getContext()).thenReturn(contextB);
    when(elementC.getContext()).thenReturn(contextC);
    when(elementC.getSource()).thenReturn(sourceB);
    when(unitElementC.getSource()).thenReturn(sourceB);
    // configure B and C
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.aboutToIndexDart(contextB, unitElementB);
      store.aboutToIndexDart(contextC, unitElementC);
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      assertEquals(1, store.internalGetLocationCountForContext(contextB));
      assertEquals(1, store.internalGetLocationCountForContext(contextC));
      // we get locations from all contexts
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove "B" in B, 1 relation and 1 location left
    store.removeSource(contextB, sourceB);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(0, store.internalGetLocationCountForContext(contextB));
    assertEquals(1, store.internalGetLocationCountForContext(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationC);
    }
    // now remove "B" in C, empty
    store.removeSource(contextC, sourceB);
    assertEquals(0, store.internalGetLocationCount());
    assertEquals(0, store.internalGetLocationCountForContext(contextB));
    assertEquals(0, store.internalGetLocationCountForContext(contextC));
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertThat(locations).isEmpty();
    }
  }

  public void test_removeSources_nullContext() throws Exception {
    // record
    {
      store.recordRelationship(IndexConstants.UNIVERSE, relationship, location);
      assertEquals(1, store.internalGetLocationCount());
    }
    // remove "null" context, should never happen - ignored
    SourceContainer sourceContainer = new DirectoryBasedSourceContainer("/path/");
    store.removeSources(null, sourceContainer);
    assertEquals(1, store.internalGetLocationCount());
  }

  public void test_removeSources_withDeclaration() throws Exception {
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: A, [B -> A],  [C -> A] and [B -> C]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      store.recordRelationship(elementC, relationship, locationB);
      assertEquals(3, store.internalGetLocationCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove container with [A], only [B -> C] left
    SourceContainer containerA = mockSourceContainer(sourceA);
    store.removeSources(contextA, containerA);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(1, store.internalGetLocationCountForContext(contextA));
    {
      Location[] locations = store.getRelationships(elementC, relationship);
      assertLocations(locations, locationB);
    }
  }

  public void test_removeSources_withRelationship() throws Exception {
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    // record: [B -> A] and [C -> A]
    {
      store.recordRelationship(elementA, relationship, locationB);
      store.recordRelationship(elementA, relationship, locationC);
      assertEquals(2, store.internalGetLocationCount());
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
    // remove container with [B], 1 relation and 1 location left
    SourceContainer containerB = mockSourceContainer(sourceB);
    store.removeSources(contextA, containerB);
    assertEquals(1, store.internalGetLocationCount());
    assertEquals(1, store.internalGetLocationCountForContext(contextA));
    Location[] locations = store.getRelationships(elementA, relationship);
    assertLocations(locations, locationC);
  }

  public void test_tryToRecord_afterContextRemove_element() throws Exception {
    Location locationB = mockLocation(elementB);
    // remove "A" - context of "elementA"
    when(contextA.isDisposed()).thenReturn(true);
    store.removeContext(contextA);
    // so, this record request is ignored
    store.recordRelationship(elementA, relationship, locationB);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_tryToRecord_afterContextRemove_location() throws Exception {
    Location locationB = mockLocation(elementB);
    when(elementB.getContext()).thenReturn(contextB);
    // remove "B" - context of location
    when(contextB.isDisposed()).thenReturn(true);
    store.removeContext(contextB);
    // so, this record request is ignored
    store.recordRelationship(elementA, relationship, locationB);
    assertEquals(0, store.internalGetLocationCount());
  }

  public void test_writeRead() throws Exception {
    when(contextA.getElement(eq(elementLocationA))).thenReturn(elementA);
    when(contextB.getElement(eq(elementLocationB))).thenReturn(elementB);
    when(elementA.getContext()).thenReturn(contextA);
    when(elementB.getContext()).thenReturn(contextB);
    // fill store
    Location locationA = new Location(elementA, 0, 0);
    Location locationB = new Location(elementB, 0, 0);
    store.aboutToIndexDart(contextA, unitElementA);
    store.aboutToIndexDart(contextB, unitElementB);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementB, relationship, locationB);
    assertEquals(2, store.internalGetKeyCount());
    assertEquals(2, store.internalGetLocationCount());
    assertLocations(store.getRelationships(elementA, relationship), locationA);
    assertLocations(store.getRelationships(elementB, relationship), locationB);
    // write
    byte[] content;
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      store.writeIndex(contextA, baos);
      content = baos.toByteArray();
    }
    // clear
    store.removeContext(contextA);
    store.removeContext(contextB);
    assertEquals(0, store.internalGetKeyCount());
    assertEquals(0, store.internalGetLocationCount());
    // we need to re-create AnalysisContext, current instance was marked as removed
    {
      contextA = mock(AnalysisContext.class);
      when(contextA.getElement(eq(elementLocationA))).thenReturn(elementA);
      when(elementA.getContext()).thenReturn(contextA);
    }
    // read
    {
      ByteArrayInputStream bais = new ByteArrayInputStream(content);
      store.readIndex(contextA, bais);
    }
    // validate after read
    assertEquals(1, store.internalGetKeyCount());
    assertEquals(1, store.internalGetLocationCount());
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA);
    }
  }

  public void test_writeRead_invalidVersion() throws Exception {
    // write fake content with invalid version
    byte[] content;
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeInt(-1);
      content = baos.toByteArray();
    }
    // read
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(content);
      store.readIndex(contextA, bais);
      fail();
    } catch (IOException e) {
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(contextA.toString()).thenReturn("contextA");
    when(contextB.toString()).thenReturn("contextB");
    when(contextC.toString()).thenReturn("contextC");
    when(sourceA.toString()).thenReturn("sourceA");
    when(sourceB.toString()).thenReturn("sourceB");
    when(sourceC.toString()).thenReturn("sourceC");
    when(sourceD.toString()).thenReturn("sourceD");
    when(elementA.toString()).thenReturn("elementA");
    when(elementB.toString()).thenReturn("elementB");
    when(elementC.toString()).thenReturn("elementC");
    when(elementD.toString()).thenReturn("elementD");
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
    when(elementA.getSource()).thenReturn(sourceA);
    when(elementB.getSource()).thenReturn(sourceB);
    when(elementC.getSource()).thenReturn(sourceC);
    when(elementD.getSource()).thenReturn(sourceD);
    when(elementA.getLibrary()).thenReturn(libraryElement);
    when(elementB.getLibrary()).thenReturn(libraryElement);
    when(elementC.getLibrary()).thenReturn(libraryElement);
    when(elementD.getLibrary()).thenReturn(libraryElement);
    when(unitElementA.getSource()).thenReturn(sourceA);
    when(unitElementB.getSource()).thenReturn(sourceB);
    when(unitElementC.getSource()).thenReturn(sourceC);
    when(unitElementD.getSource()).thenReturn(sourceD);
    when(unitElementA.getLibrary()).thenReturn(libraryElement);
    when(unitElementB.getLibrary()).thenReturn(libraryElement);
    when(unitElementC.getLibrary()).thenReturn(libraryElement);
    when(unitElementD.getLibrary()).thenReturn(libraryElement);
    // library
    when(librarySource.toString()).thenReturn("libSource");
    when(libraryUnitElement.getSource()).thenReturn(librarySource);
    when(libraryElement.getSource()).thenReturn(librarySource);
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(libraryUnitElement);
    // by default index all units
    store.aboutToIndexDart(contextA, unitElementA);
    store.aboutToIndexDart(contextA, unitElementB);
    store.aboutToIndexDart(contextA, unitElementC);
    store.aboutToIndexDart(contextA, unitElementD);
  }
}
