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
import com.google.dart.engine.index.UniverseElement;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.internal.element.ElementLocationImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

public class SplitIndexStoreImplTest extends EngineTestCase {
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
      return other.location.getOffset() == other.location.getOffset()
          && other.location.getLength() == other.location.getLength()
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
   * Wraps the given locations into {@link LocationEqualsWrapper}.
   */
  private static LocationEqualsWrapper[] wrapLocations(Location[] locations) {
    List<LocationEqualsWrapper> wrappers = Lists.newArrayList();
    for (Location location : locations) {
      wrappers.add(new LocationEqualsWrapper(location));
    }
    return wrappers.toArray(new LocationEqualsWrapper[wrappers.size()]);
  }

  private MemoryNodeManager nodeManager = new MemoryNodeManager();
  private SplitIndexStoreImpl store = new SplitIndexStoreImpl(nodeManager);

  private AnalysisContext contextA = mock(AnalysisContext.class);
  private AnalysisContext contextB = mock(AnalysisContext.class);
  private AnalysisContext contextC = mock(AnalysisContext.class);
  private ElementLocation elementLocationA = new ElementLocationImpl(new String[] {
      "/home/user/sourceA.dart", "ClassA"});
  private ElementLocation elementLocationB = new ElementLocationImpl(new String[] {
      "/home/user/sourceB.dart", "ClassB"});
  private ElementLocation elementLocationC = new ElementLocationImpl(new String[] {
      "/home/user/sourceC.dart", "ClassC"});
  private ElementLocation elementLocationD = new ElementLocationImpl(new String[] {
      "/home/user/sourceD.dart", "ClassD"});
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
  private HtmlElement htmlElementA = mock(HtmlElement.class);
  private HtmlElement htmlElementB = mock(HtmlElement.class);
  private Relationship relationship = Relationship.getRelationship("test-relationship");

  public void test_aboutToIndexDart_disposedContext() throws Exception {
    when(contextA.isDisposed()).thenReturn(true);
    assertEquals(false, store.aboutToIndexDart(contextA, unitElementA));
  }

  public void test_aboutToIndexDart_disposedContext_wrapped() throws Exception {
    when(contextA.isDisposed()).thenReturn(true);
    InstrumentedAnalysisContextImpl instrumentedContext = mock(InstrumentedAnalysisContextImpl.class);
    when(instrumentedContext.getBasis()).thenReturn(contextA);
    assertEquals(false, store.aboutToIndexDart(instrumentedContext, unitElementA));
  }

  public void test_aboutToIndexDart_library_first() throws Exception {
    when(libraryElement.getParts()).thenReturn(
        new CompilationUnitElement[] {unitElementA, unitElementB});
    {
      store.aboutToIndexDart(contextA, libraryUnitElement);
      store.doneIndex();
    }
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations);
    }
  }

  public void test_aboutToIndexDart_library_secondWithoutOneUnit() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
    // apply "libraryUnitElement", only with "B"
    when(libraryElement.getParts()).thenReturn(new CompilationUnitElement[] {unitElementB});
    {
      store.aboutToIndexDart(contextA, libraryUnitElement);
      store.doneIndex();
    }
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB);
    }
  }

  public void test_aboutToIndexDart_nullLibraryElement() throws Exception {
    when(unitElementA.getLibrary()).thenReturn(null);
    assertEquals(false, store.aboutToIndexDart(contextA, unitElementA));
  }

  public void test_aboutToIndexDart_nullLibraryUnitElement() throws Exception {
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(null);
    assertEquals(false, store.aboutToIndexDart(contextA, unitElementA));
  }

  public void test_aboutToIndexDart_nullUnitElement() throws Exception {
    assertEquals(false, store.aboutToIndexDart(contextA, null));
  }

  public void test_aboutToIndexHtml_() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexHtml(contextA, htmlElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexHtml(contextA, htmlElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
  }

  public void test_aboutToIndexHtml_disposedContext() throws Exception {
    when(contextA.isDisposed()).thenReturn(true);
    assertEquals(false, store.aboutToIndexHtml(contextA, htmlElementA));
  }

  public void test_clear() throws Exception {
    Location locationA = mockLocation(elementA);
    store.aboutToIndexDart(contextA, unitElementA);
    store.recordRelationship(elementA, relationship, locationA);
    store.doneIndex();
    assertFalse(nodeManager.isEmpty());
    // clear
    store.clear();
    assertTrue(nodeManager.isEmpty());
  }

  public void test_getRelationships_empty() throws Exception {
    Location[] locations = store.getRelationships(elementA, relationship);
    assertThat(locations).isEmpty();
  }

  public void test_getStatistics() throws Exception {
    // empty initially
    assertThat(store.getStatistics()).contains("0 locations").contains("0 sources");
    // add 2 locations
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    assertThat(store.getStatistics()).contains("2 locations").contains("3 sources");
  }

  public void test_recordRelationship_nullElement() throws Exception {
    Location locationA = mockLocation(elementA);
    store.recordRelationship(null, relationship, locationA);
    store.doneIndex();
    assertTrue(nodeManager.isEmpty());
  }

  public void test_recordRelationship_nullLocation() throws Exception {
    store.recordRelationship(elementA, relationship, null);
    store.doneIndex();
    assertTrue(nodeManager.isEmpty());
  }

  public void test_recordRelationship_oneElement_twoNodes() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
  }

  public void test_recordRelationship_oneLocation() throws Exception {
    Location locationA = mockLocation(elementA);
    store.aboutToIndexDart(contextA, unitElementA);
    store.recordRelationship(elementA, relationship, locationA);
    store.doneIndex();
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA);
    }
  }

  public void test_recordRelationship_twoLocations() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementA);
    store.aboutToIndexDart(contextA, unitElementA);
    store.recordRelationship(elementA, relationship, locationA);
    store.recordRelationship(elementA, relationship, locationB);
    store.doneIndex();
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
  }

  public void test_removeContext() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
    // remove "A" context
    store.removeContext(contextA);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations);
    }
  }

  public void test_removeContext_nullContext() throws Exception {
    store.removeContext(null);
  }

  public void test_removeSource_library() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementC);
      store.recordRelationship(elementA, relationship, locationC);
      store.doneIndex();
    }
    // "A", "B" and "C" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB, locationC);
    }
    // remove "librarySource"
    store.removeSource(contextA, librarySource);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations);
    }
  }

  public void test_removeSource_nullContext() throws Exception {
    store.removeSource(null, sourceA);
  }

  public void test_removeSource_unit() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementC);
      store.recordRelationship(elementA, relationship, locationC);
      store.doneIndex();
    }
    // "A", "B" and "C" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB, locationC);
    }
    // remove "A" source
    store.removeSource(contextA, sourceA);
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
  }

  public void test_removeSources_library() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    // "A" and "B" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB);
    }
    // remove "librarySource"
    store.removeSources(contextA, new SourceContainer() {
      @Override
      public boolean contains(Source source) {
        return source == librarySource;
      }
    });
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations);
    }
  }

  public void test_removeSources_nullContext() throws Exception {
    store.removeSources(null, null);
  }

  public void test_removeSources_unit() throws Exception {
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    Location locationC = mockLocation(elementC);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(elementA, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementB);
      store.recordRelationship(elementA, relationship, locationB);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextA, unitElementC);
      store.recordRelationship(elementA, relationship, locationC);
      store.doneIndex();
    }
    // "A", "B" and "C" locations
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationA, locationB, locationC);
    }
    // remove "A" source
    store.removeSources(contextA, new SourceContainer() {
      @Override
      public boolean contains(Source source) {
        return source == sourceA;
      }
    });
    {
      Location[] locations = store.getRelationships(elementA, relationship);
      assertLocations(locations, locationB, locationC);
    }
  }

  public void test_universe_aboutToIndex() throws Exception {
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextB.getElement(elementLocationB)).thenReturn(elementB);
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(UniverseElement.INSTANCE, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextB, unitElementB);
      store.recordRelationship(UniverseElement.INSTANCE, relationship, locationB);
      store.doneIndex();
    }
    {
      Location[] locations = store.getRelationships(UniverseElement.INSTANCE, relationship);
      assertLocations(locations, locationA, locationB);
    }
    // re-index "unitElementA"
    store.aboutToIndexDart(contextA, unitElementA);
    store.doneIndex();
    {
      Location[] locations = store.getRelationships(UniverseElement.INSTANCE, relationship);
      assertLocations(locations, locationB);
    }
  }

  public void test_universe_removeContext() throws Exception {
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextB.getElement(elementLocationB)).thenReturn(elementB);
    Location locationA = mockLocation(elementA);
    Location locationB = mockLocation(elementB);
    {
      store.aboutToIndexDart(contextA, unitElementA);
      store.recordRelationship(UniverseElement.INSTANCE, relationship, locationA);
      store.doneIndex();
    }
    {
      store.aboutToIndexDart(contextB, unitElementB);
      store.recordRelationship(UniverseElement.INSTANCE, relationship, locationB);
      store.doneIndex();
    }
    {
      Location[] locations = store.getRelationships(UniverseElement.INSTANCE, relationship);
      assertLocations(locations, locationA, locationB);
    }
    // remove "contextA"
    store.removeContext(contextA);
    {
      Location[] locations = store.getRelationships(UniverseElement.INSTANCE, relationship);
      assertLocations(locations, locationB);
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(contextA.toString()).thenReturn("contextA");
    when(contextB.toString()).thenReturn("contextB");
    when(contextC.toString()).thenReturn("contextC");
    when(contextA.getElement(elementLocationA)).thenReturn(elementA);
    when(contextA.getElement(elementLocationB)).thenReturn(elementB);
    when(contextA.getElement(elementLocationC)).thenReturn(elementC);
    when(contextA.getElement(elementLocationD)).thenReturn(elementD);
    when(sourceA.toString()).thenReturn("sourceA");
    when(sourceB.toString()).thenReturn("sourceB");
    when(sourceC.toString()).thenReturn("sourceC");
    when(sourceD.toString()).thenReturn("sourceD");
    when(sourceA.getFullName()).thenReturn("/home/user/sourceA.dart");
    when(sourceB.getFullName()).thenReturn("/home/user/sourceB.dart");
    when(sourceC.getFullName()).thenReturn("/home/user/sourceC.dart");
    when(sourceD.getFullName()).thenReturn("/home/user/sourceD.dart");
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
    when(htmlElementA.getSource()).thenReturn(sourceA);
    when(htmlElementB.getSource()).thenReturn(sourceB);
    // library
    when(librarySource.toString()).thenReturn("libSource");
    when(libraryUnitElement.getLibrary()).thenReturn(libraryElement);
    when(libraryUnitElement.getSource()).thenReturn(librarySource);
    when(libraryElement.getSource()).thenReturn(librarySource);
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(libraryUnitElement);
  }
}
