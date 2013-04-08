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
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.MemoryIndexStore;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.element.member.Member;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.utilities.collection.FastRemoveList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
      return Objects.equal(other.element, element) && other.relationship == relationship;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(element, relationship);
    }
  }

  private static final Object WEAK_SET_VALUE = new Object();

  /**
   * @return the {@link Source} which contains given {@link Element}, may be {@code null}.
   */
  @VisibleForTesting
  public static Source findSource(Element element) {
    while (element != null) {
      if (element instanceof LibraryElement) {
        element = ((LibraryElement) element).getDefiningCompilationUnit();
        // something wrong with this library
        if (element == null) {
          return null;
        }
      }
      if (element instanceof CompilationUnitElement) {
        return ((CompilationUnitElement) element).getSource();
      }
      element = element.getEnclosingElement();
    }
    return null;
  }

  private final Map<AnalysisContext, Object> removedContexts = new MapMaker().weakKeys().makeMap();

  /**
   * A table mapping elements to tables mapping relationships to lists of locations.
   */
  final Map<ElementRelationKey, FastRemoveList<ContributedLocation>> relationshipMap = Maps.newHashMapWithExpectedSize(4096);

  /**
   * A set of all {@link Source}s with elements or relationships.
   */
  final Map<AnalysisContext, Set<Source>> sources = Maps.newHashMapWithExpectedSize(64);

  /**
   * {@link Element}s by {@link Source} where they are declared.
   */
  final Map<AnalysisContext, Map<Source, Set<Element>>> sourceToDeclarations = Maps.newHashMapWithExpectedSize(64);

  /**
   * {@link ContributedLocation}s by {@link Source} where they are contributed.
   */
  final Map<AnalysisContext, Map<Source, FastRemoveList<ContributedLocation>>> sourceToLocations = Maps.newHashMapWithExpectedSize(64);

  @VisibleForTesting
  public int getDeclarationCount(AnalysisContext context) {
    int count = 0;
    Map<Source, Set<Element>> contextDeclarations = sourceToDeclarations.get(context);
    if (contextDeclarations != null) {
      for (Set<Element> sourceDeclarations : contextDeclarations.values()) {
        count += sourceDeclarations.size();
      }
    }
    return count;
  }

  @Override
  public int getElementCount() {
    Set<Element> elements = Sets.newHashSet();
    for (ElementRelationKey key : relationshipMap.keySet()) {
      elements.add(key.element);
    }
    return elements.size();
  }

  @VisibleForTesting
  public int getLocationCount(AnalysisContext context) {
    int count = 0;
    Map<Source, FastRemoveList<ContributedLocation>> contextLocations = sourceToLocations.get(context);
    if (contextLocations != null) {
      for (FastRemoveList<ContributedLocation> contributedLocations : contextLocations.values()) {
        count += contributedLocations.size();
      }
    }
    return count;
  }

  @Override
  public int getRelationshipCount() {
    int count = 0;
    for (FastRemoveList<ContributedLocation> contributedLocations : relationshipMap.values()) {
      count += contributedLocations.size();
    }
    return count;
  }

  @Override
  public Location[] getRelationships(Element element, Relationship relationship) {
    ElementRelationKey relKey = new ElementRelationKey(element, relationship);
    FastRemoveList<ContributedLocation> contributedLocations = relationshipMap.get(relKey);
    if (contributedLocations != null) {
      int count = contributedLocations.size();
      Location[] locations = new Location[count];
      int locationIndex = 0;
      for (ContributedLocation contributedLocation : contributedLocations) {
        locations[locationIndex++] = contributedLocation.getLocation();
      }
      return locations;
    }
    return Location.EMPTY_ARRAY;
  }

  @Override
  public int getSourceCount() {
    int count = 0;
    for (Set<Source> sourceSet : sources.values()) {
      count += sourceSet.size();
    }
    return count;
  }

  @Override
  public void readIndex(AnalysisContext context, InputStream input) throws IOException {
    new MemoryIndexReader(this, context, input).read();
  }

  @Override
  public void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element == null || location == null) {
      return;
    }
    // prepare information
    AnalysisContext elementContext = element.getContext();
    AnalysisContext locationContext = location.getElement().getContext();
    Source elementSource = findSource(element);
    Source locationSource = findSource(location.getElement());
    // may be already removed in other thread
    if (removedContexts.containsKey(elementContext)) {
      return;
    }
    if (removedContexts.containsKey(locationContext)) {
      return;
    }
    // TODO(scheglov) we started to resolve some elements as Member(s),
    // but at the index level we don't care.
    if (element instanceof Member) {
      element = ((Member) element).getBaseElement();
    }
    // TODO(scheglov) remove after fix in resolver
    if (elementContext == null && !(element instanceof NameElementImpl)
        && !(element instanceof UniverseElementImpl)) {
      return;
    }
    // remember sources
    addSource(elementContext, elementSource);
    addSource(locationContext, locationSource);
    recordSourceToDeclarations(elementContext, elementSource, element);
    //
    Map<Source, FastRemoveList<ContributedLocation>> contextLocations = sourceToLocations.get(locationContext);
    if (contextLocations == null) {
      contextLocations = Maps.newHashMap();
      sourceToLocations.put(locationContext, contextLocations);
    }
    //
    FastRemoveList<ContributedLocation> sourceLocations = contextLocations.get(locationSource);
    if (sourceLocations == null) {
      sourceLocations = FastRemoveList.newInstance();
      contextLocations.put(locationSource, sourceLocations);
    }
    // add ContributedLocation for "element"
    {
      ElementRelationKey relKey = new ElementRelationKey(element, relationship);
      FastRemoveList<ContributedLocation> locations = relationshipMap.get(relKey);
      if (locations == null) {
        locations = FastRemoveList.newInstance();
        relationshipMap.put(relKey, locations);
      }
      new ContributedLocation(sourceLocations, locations, location);
    }
  }

  @Override
  public void removeContext(AnalysisContext context) {
    removedContexts.put(context, WEAK_SET_VALUE);
    // remove context sources
    sources.remove(context);
    // remove elements declared in Source(s) of removed context
    Map<Source, Set<Element>> contextElements = sourceToDeclarations.remove(context);
    if (contextElements != null) {
      for (Set<Element> sourceElements : contextElements.values()) {
        removeSourceDeclaredElements(sourceElements);
      }
    }
    // remove relationships in Source(s) of removed context
    Map<Source, FastRemoveList<ContributedLocation>> contextLocations = sourceToLocations.remove(context);
    if (contextLocations != null) {
      for (FastRemoveList<ContributedLocation> sourceLocations : contextLocations.values()) {
        removeSourceContributedLocations(sourceLocations);
      }
    }
  }

  @Override
  public void removeSource(AnalysisContext context, Source source) {
    {
      Set<Source> contextSources = sources.get(context);
      if (contextSources != null) {
        contextSources.remove(source);
      }
    }
    // remove relationships with elements declared in removed source
    Map<Source, Set<Element>> contextElements = sourceToDeclarations.get(context);
    if (contextElements != null) {
      Set<Element> sourceElements = contextElements.remove(source);
      if (sourceElements != null) {
        removeSourceDeclaredElements(sourceElements);
      }
    }
    // remove relationships in removed source
    Map<Source, FastRemoveList<ContributedLocation>> contextLocations = sourceToLocations.get(context);
    if (contextLocations != null) {
      FastRemoveList<ContributedLocation> sourceLocations = contextLocations.remove(source);
      if (sourceLocations != null) {
        removeSourceContributedLocations(sourceLocations);
      }
    }
  }

  @Override
  public void removeSources(AnalysisContext context, SourceContainer container) {
    // prepare sources to remove
    Set<Source> sourcesToRemove = Sets.newHashSet();
    {
      Set<Source> contextSources = sources.get(context);
      if (contextSources != null) {
        for (Source source : contextSources) {
          if (container.contains(source)) {
            sourcesToRemove.add(source);
          }
        }
      }
    }
    // do remove sources
    for (Source source : sourcesToRemove) {
      removeSource(context, source);
    }
  }

  @Override
  public void writeIndex(AnalysisContext context, OutputStream output) throws IOException {
    new MemoryIndexWriter(this, context, output).write();
  }

  private void addSource(AnalysisContext context, Source source) {
    Set<Source> contextSources = sources.get(context);
    if (contextSources == null) {
      contextSources = Sets.newHashSet();
      sources.put(context, contextSources);
    }
    contextSources.add(source);
  }

  private void recordSourceToDeclarations(AnalysisContext context, Source elementSource,
      Element declaredElement) {
    // prepare AnalysisContext declarations
    Map<Source, Set<Element>> contextElements = sourceToDeclarations.get(context);
    if (contextElements == null) {
      contextElements = Maps.newHashMap();
      sourceToDeclarations.put(context, contextElements);
    }
    // remember Element in Source
    Set<Element> sourceElements = contextElements.get(elementSource);
    if (sourceElements == null) {
      sourceElements = Sets.newHashSet();
      contextElements.put(elementSource, sourceElements);
    }
    sourceElements.add(declaredElement);
  }

  private void removeSourceContributedLocations(FastRemoveList<ContributedLocation> sourceLocations) {
    for (ContributedLocation contributedLocation : sourceLocations) {
      contributedLocation.removeFromLocationOwner();
    }
  }

  private void removeSourceDeclaredElements(Set<Element> sourceElements) {
    for (Element sourceElement : sourceElements) {
      for (Relationship relationship : Relationship.values()) {
        ElementRelationKey relKey = new ElementRelationKey(sourceElement, relationship);
        FastRemoveList<ContributedLocation> contributedLocations = relationshipMap.remove(relKey);
        if (contributedLocations != null) {
          for (ContributedLocation contributedLocation : contributedLocations) {
            contributedLocation.removeFromDeclarationOwner();
          }
        }
      }
    }
  }
}
