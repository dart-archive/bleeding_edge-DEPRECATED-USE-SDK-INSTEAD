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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.MemoryIndexStore;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.internal.element.member.Member;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.utilities.translation.DartExpressionBody;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link IndexStore} which keeps full index in memory.
 * 
 * @coverage dart.engine.index
 */
public class MemoryIndexStoreImpl implements MemoryIndexStore {
  static class ElementRelationKey {
    final Element element;
    final Relationship relationship;

    public ElementRelationKey(Element element, Relationship relationship) {
      this.element = element;
      this.relationship = relationship;
    }

    @Override
    public boolean equals(Object obj) {
      ElementRelationKey other = (ElementRelationKey) obj;
      Element otherElement = other.element;
      return other.relationship == relationship
          && otherElement.getNameOffset() == element.getNameOffset()
          && otherElement.getKind() == element.getKind()
          && Objects.equal(otherElement.getDisplayName(), element.getDisplayName())
          && Objects.equal(otherElement.getSource(), element.getSource());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(
          element.getSource(),
          element.getNameOffset(),
          element.getKind(),
          element.getDisplayName(),
          relationship);
    }

    @Override
    public String toString() {
      return element + " " + relationship;
    }
  }

  static class Source2 {
    final Source librarySource;
    final Source unitSource;

    public Source2(Source librarySource, Source unitSource) {
      this.librarySource = librarySource;
      this.unitSource = unitSource;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Source2)) {
        return false;
      }
      Source2 other = (Source2) obj;
      return Objects.equal(other.librarySource, librarySource)
          && Objects.equal(other.unitSource, unitSource);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(librarySource, unitSource);
    }

    @Override
    public String toString() {
      return librarySource + " " + unitSource;
    }
  }

  /**
   * When logging is on, {@link AnalysisEngine} actually creates
   * {@link InstrumentedAnalysisContextImpl}, which wraps {@link AnalysisContextImpl} used to create
   * actual {@link Element}s. So, in index we have to unwrap {@link InstrumentedAnalysisContextImpl}
   * when perform any operation.
   */
  public static AnalysisContext unwrapContext(AnalysisContext context) {
    if (context instanceof InstrumentedAnalysisContextImpl) {
      context = ((InstrumentedAnalysisContextImpl) context).getBasis();
    }
    return context;
  }

  /**
   * @return the {@link Source} of the enclosing {@link LibraryElement}, may be {@code null}.
   */
  private static Source getLibrarySourceOrNull(Element element) {
    LibraryElement library = element.getLibrary();
    if (library == null) {
      return null;
    }
    if (library.isAngularHtml()) {
      return null;
    }
    return library.getSource();
  }

  /**
   * This map is used to canonicalize equal keys.
   */
  private final Map<ElementRelationKey, ElementRelationKey> canonicalKeys = Maps.newHashMap();

  /**
   * The mapping of {@link ElementRelationKey} to the {@link Location}s, one-to-many.
   */
  final Map<ElementRelationKey, Set<Location>> keyToLocations = Maps.newHashMap();

  /**
   * The mapping of {@link Source} to the {@link ElementRelationKey}s. It is used in
   * {@link #removeSource(AnalysisContext, Source)} to identify keys to remove from
   * {@link #keyToLocations}.
   */
  final Map<AnalysisContext, Map<Source2, Set<ElementRelationKey>>> contextToSourceToKeys = Maps.newHashMap();

  /**
   * The mapping of {@link Source} to the {@link Location}s existing in it. It is used in
   * {@link #clearSource0(AnalysisContext, Source)} to identify locations to remove from
   * {@link #keyToLocations}.
   */
  final Map<AnalysisContext, Map<Source2, List<Location>>> contextToSourceToLocations = Maps.newHashMap();

  /**
   * The mapping of library {@link Source} to the {@link Source}s of part units.
   */
  final Map<AnalysisContext, Map<Source, Set<Source>>> contextToLibraryToUnits = Maps.newHashMap();

  /**
   * The mapping of unit {@link Source} to the {@link Source}s of libraries it is used in.
   */
  final Map<AnalysisContext, Map<Source, Set<Source>>> contextToUnitToLibraries = Maps.newHashMap();

  private int sourceCount;
  private int keyCount;
  private int locationCount;

  @Override
  public boolean aboutToIndexDart(AnalysisContext context, CompilationUnitElement unitElement) {
    context = unwrapContext(context);
    // may be already disposed in other thread
    if (context.isDisposed()) {
      return false;
    }
    // validate unit
    if (unitElement == null) {
      return false;
    }
    LibraryElement libraryElement = unitElement.getLibrary();
    if (libraryElement == null) {
      return false;
    }
    CompilationUnitElement definingUnitElement = libraryElement.getDefiningCompilationUnit();
    if (definingUnitElement == null) {
      return false;
    }
    // prepare sources
    Source library = definingUnitElement.getSource();
    Source unit = unitElement.getSource();
    // special handling for the defining library unit
    if (unit.equals(library)) {
      // prepare new parts
      Set<Source> newParts = Sets.newHashSet();
      for (CompilationUnitElement part : libraryElement.getParts()) {
        newParts.add(part.getSource());
      }
      // prepare old parts
      Map<Source, Set<Source>> libraryToUnits = contextToLibraryToUnits.get(context);
      if (libraryToUnits == null) {
        libraryToUnits = Maps.newHashMap();
        contextToLibraryToUnits.put(context, libraryToUnits);
      }
      Set<Source> oldParts = libraryToUnits.get(library);
      // check if some parts are not in the library now
      if (oldParts != null) {
        Set<Source> noParts = Sets.difference(oldParts, newParts);
        for (Source noPart : noParts) {
          removeLocations(context, library, noPart);
        }
      }
      // remember new parts
      libraryToUnits.put(library, newParts);
    }
    // remember libraries in which unit is used
    recordUnitInLibrary(context, library, unit);
    // remove locations
    removeLocations(context, library, unit);
    // remove keys
    {
      Map<Source2, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
      if (sourceToKeys != null) {
        Source2 source2 = new Source2(library, unit);
        boolean hadSource = sourceToKeys.remove(source2) != null;
        if (hadSource) {
          sourceCount--;
        }
      }
    }
    // OK, we can index
    return true;
  }

  @Override
  public boolean aboutToIndexHtml(AnalysisContext context, HtmlElement htmlElement) {
    context = unwrapContext(context);
    // may be already disposed in other thread
    if (context.isDisposed()) {
      return false;
    }
    // remove locations
    Source source = htmlElement.getSource();
    removeLocations(context, null, source);
    // remove keys
    {
      Map<Source2, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
      if (sourceToKeys != null) {
        Source2 source2 = new Source2(null, source);
        boolean hadSource = sourceToKeys.remove(source2) != null;
        if (hadSource) {
          sourceCount--;
        }
      }
    }
    // remember libraries in which unit is used
    recordUnitInLibrary(context, null, source);
    // OK, we can index
    return true;
  }

  @Override
  public void clear() {
    canonicalKeys.clear();
    keyToLocations.clear();
    contextToSourceToKeys.clear();
    contextToSourceToLocations.clear();
    contextToLibraryToUnits.clear();
    contextToUnitToLibraries.clear();
  }

  @Override
  public void doneIndex() {
  }

  @Override
  public Location[] getRelationships(Element element, Relationship relationship) {
    ElementRelationKey key = new ElementRelationKey(element, relationship);
    Set<Location> locations = keyToLocations.get(key);
    if (locations != null) {
      return locations.toArray(new Location[locations.size()]);
    }
    return Location.EMPTY_ARRAY;
  }

  @Override
  public String getStatistics() {
    return locationCount + " relationships in " + keyCount + " keys in " + sourceCount + " sources";
  }

  @VisibleForTesting
  public int internalGetKeyCount() {
    return keyToLocations.size();
  }

  @VisibleForTesting
  public int internalGetLocationCount() {
    int count = 0;
    for (Set<Location> locations : keyToLocations.values()) {
      count += locations.size();
    }
    return count;
  }

  @VisibleForTesting
  public int internalGetLocationCountForContext(AnalysisContext context) {
    context = unwrapContext(context);
    int count = 0;
    for (Set<Location> locations : keyToLocations.values()) {
      for (Location location : locations) {
        if (location.getElement().getContext() == context) {
          count++;
        }
      }
    }
    return count;
  }

  @VisibleForTesting
  public int internalGetSourceKeyCount(AnalysisContext context) {
    int count = 0;
    Map<Source2, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
    if (sourceToKeys != null) {
      for (Set<ElementRelationKey> keys : sourceToKeys.values()) {
        count += keys.size();
      }
    }
    return count;
  }

  @Override
  @DartOmit
  public void readIndex(AnalysisContext context, InputStream input) throws IOException {
    context = unwrapContext(context);
    new MemoryIndexReader(this, context, input).read();
  }

  @Override
  public void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element == null || location == null) {
      return;
    }
    location = location.newClone();
    // at the index level we don't care about Member(s)
    if (element instanceof Member) {
      element = ((Member) element).getBaseElement();
    }
//    System.out.println(element + " " + relationship + " " + location);
    // prepare information
    AnalysisContext elementContext = element.getContext();
    AnalysisContext locationContext = location.getElement().getContext();
    Source elementSource = element.getSource();
    Source locationSource = location.getElement().getSource();
    Source elementLibrarySource = getLibrarySourceOrNull(element);
    Source locationLibrarySource = getLibrarySourceOrNull(location.getElement());
    // sanity check
    if (locationContext == null) {
      return;
    }
    if (locationSource == null) {
      return;
    }
    if (elementContext == null && !(element instanceof NameElementImpl)
        && !(element instanceof UniverseElementImpl)) {
      return;
    }
    if (elementSource == null && !(element instanceof NameElementImpl)
        && !(element instanceof UniverseElementImpl)) {
      return;
    }
    // may be already disposed in other thread
    if (elementContext != null && elementContext.isDisposed()) {
      return;
    }
    if (locationContext.isDisposed()) {
      return;
    }
    // record: key -> location(s)
    ElementRelationKey key = getCanonicalKey(element, relationship);
    {
      Set<Location> locations = keyToLocations.remove(key);
      if (locations == null) {
        locations = createLocationIdentitySet();
      } else {
        keyCount--;
      }
      keyToLocations.put(key, locations);
      keyCount++;
      locations.add(location);
      locationCount++;
    }
    // record: location -> key
    location.internalKey = key;
    // prepare source pairs
    Source2 elementSource2 = new Source2(elementLibrarySource, elementSource);
    Source2 locationSource2 = new Source2(locationLibrarySource, locationSource);
    // record: element source -> keys
    {
      Map<Source2, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(elementContext);
      if (sourceToKeys == null) {
        sourceToKeys = Maps.newHashMap();
        contextToSourceToKeys.put(elementContext, sourceToKeys);
      }
      Set<ElementRelationKey> keys = sourceToKeys.get(elementSource2);
      if (keys == null) {
        keys = Sets.newHashSet();
        sourceToKeys.put(elementSource2, keys);
        sourceCount++;
      }
      keys.remove(key);
      keys.add(key);
    }
    // record: location source -> locations
    {
      Map<Source2, List<Location>> sourceToLocations = contextToSourceToLocations.get(locationContext);
      if (sourceToLocations == null) {
        sourceToLocations = Maps.newHashMap();
        contextToSourceToLocations.put(locationContext, sourceToLocations);
      }
      List<Location> locations = sourceToLocations.get(locationSource2);
      if (locations == null) {
        locations = Lists.newArrayList();
        sourceToLocations.put(locationSource2, locations);
      }
      locations.add(location);
    }
  }

  @Override
  public void removeContext(AnalysisContext context) {
    context = unwrapContext(context);
    if (context == null) {
      return;
    }
    // remove sources
    removeSources(context, null);
    // remove context
    contextToSourceToKeys.remove(context);
    contextToSourceToLocations.remove(context);
    contextToLibraryToUnits.remove(context);
    contextToUnitToLibraries.remove(context);
  }

  @Override
  public void removeSource(AnalysisContext context, Source unit) {
    context = unwrapContext(context);
    if (context == null) {
      return;
    }
    // remove locations defined in source
    Map<Source, Set<Source>> unitToLibraries = contextToUnitToLibraries.get(context);
    if (unitToLibraries != null) {
      Set<Source> libraries = unitToLibraries.remove(unit);
      if (libraries != null) {
        for (Source library : libraries) {
          Source2 source2 = new Source2(library, unit);
          // remove locations defined in source
          removeLocations(context, library, unit);
          // remove keys for elements defined in source
          Map<Source2, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
          if (sourceToKeys != null) {
            Set<ElementRelationKey> keys = sourceToKeys.remove(source2);
            if (keys != null) {
              for (ElementRelationKey key : keys) {
                canonicalKeys.remove(key);
                Set<Location> locations = keyToLocations.remove(key);
                if (locations != null) {
                  keyCount--;
                  locationCount -= locations.size();
                }
              }
              sourceCount--;
            }
          }
        }
      }
    }
  }

  @Override
  public void removeSources(AnalysisContext context, SourceContainer container) {
    context = unwrapContext(context);
    if (context == null) {
      return;
    }
    // remove sources #1
    Map<Source2, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
    if (sourceToKeys != null) {
      List<Source2> sources = Lists.newArrayList(sourceToKeys.keySet());
      for (Source2 source2 : sources) {
        Source source = source2.unitSource;
        if (container == null || container.contains(source)) {
          removeSource(context, source);
        }
      }
    }
    // remove sources #2
    Map<Source2, List<Location>> sourceToLocations = contextToSourceToLocations.get(context);
    if (sourceToLocations != null) {
      List<Source2> sources = Lists.newArrayList(sourceToLocations.keySet());
      for (Source2 source2 : sources) {
        Source source = source2.unitSource;
        if (container == null || container.contains(source)) {
          removeSource(context, source);
        }
      }
    }
  }

  @Override
  @DartOmit
  public void writeIndex(AnalysisContext context, OutputStream output) throws IOException {
    context = unwrapContext(context);
    new MemoryIndexWriter(this, context, output).write();
  }

  /**
   * Creates new {@link Set} that uses object identity instead of equals.
   */
  @DartExpressionBody("new Set<Location>.identity()")
  private Set<Location> createLocationIdentitySet() {
    return Sets.newSetFromMap(new IdentityHashMap<Location, Boolean>(4));
  }

  /**
   * @return the canonical {@link ElementRelationKey} for given {@link Element} and
   *         {@link Relationship}, i.e. unique instance for this combination.
   */
  private ElementRelationKey getCanonicalKey(Element element, Relationship relationship) {
    ElementRelationKey key = new ElementRelationKey(element, relationship);
    ElementRelationKey canonicalKey = canonicalKeys.get(key);
    if (canonicalKey == null) {
      canonicalKey = key;
      canonicalKeys.put(key, canonicalKey);
    }
    return canonicalKey;
  }

  private void recordUnitInLibrary(AnalysisContext context, Source library, Source unit) {
    Map<Source, Set<Source>> unitToLibraries = contextToUnitToLibraries.get(context);
    if (unitToLibraries == null) {
      unitToLibraries = Maps.newHashMap();
      contextToUnitToLibraries.put(context, unitToLibraries);
    }
    Set<Source> libraries = unitToLibraries.get(unit);
    if (libraries == null) {
      libraries = Sets.newHashSet();
      unitToLibraries.put(unit, libraries);
    }
    libraries.add(library);
  }

  /**
   * Removes locations recorded in the given library/unit pair.
   */
  private void removeLocations(AnalysisContext context, Source library, Source unit) {
    Source2 source2 = new Source2(library, unit);
    Map<Source2, List<Location>> sourceToLocations = contextToSourceToLocations.get(context);
    if (sourceToLocations != null) {
      List<Location> sourceLocations = sourceToLocations.remove(source2);
      if (sourceLocations != null) {
        for (Location location : sourceLocations) {
          ElementRelationKey key = (ElementRelationKey) location.internalKey;
          Set<Location> relLocations = keyToLocations.get(key);
          if (relLocations != null) {
            relLocations.remove(location);
            locationCount--;
            // no locations with this key
            if (relLocations.isEmpty()) {
              canonicalKeys.remove(key);
              keyToLocations.remove(key);
              keyCount--;
            }
          }
        }
      }
    }
  }
}
