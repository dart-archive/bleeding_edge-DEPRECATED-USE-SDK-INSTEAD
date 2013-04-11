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
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
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
import com.google.dart.engine.utilities.collection.FastRemoveList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
      if (other.relationship != relationship) {
        return false;
      }
      if (element instanceof NameElementImpl) {
        return Objects.equal(other.element, element);
      }
      return other.element == element;
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

  /**
   * When logging is on, {@link AnalysisEngine} actually creates
   * {@link InstrumentedAnalysisContextImpl}, which wraps {@link AnalysisContextImpl} used to create
   * actual {@link Element}s. So, in index we have to unwrap {@link InstrumentedAnalysisContextImpl}
   * when perform any operation.
   */
  private static AnalysisContext unwrapContext(AnalysisContext context) {
    if (context instanceof InstrumentedAnalysisContextImpl) {
      context = ((InstrumentedAnalysisContextImpl) context).getBasis();
    }
    return context;
  }

  private final Map<AnalysisContext, Object> removedContexts = new MapMaker().weakKeys().makeMap();
  private int sourceCount;
  private int elementCount;
  private int relationshipCount;

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
  final Map<AnalysisContext, Map<Source, List<Element>>> sourceToDeclarations = Maps.newHashMapWithExpectedSize(64);

  /**
   * {@link ContributedLocation}s by {@link Source} where they are contributed.
   */
  final Map<AnalysisContext, Map<Source, FastRemoveList<ContributedLocation>>> sourceToLocations = Maps.newHashMapWithExpectedSize(64);

  @VisibleForTesting
  public int getDeclarationCount(AnalysisContext context) {
    context = unwrapContext(context);
    int count = 0;
    Map<Source, List<Element>> contextDeclarations = sourceToDeclarations.get(context);
    if (contextDeclarations != null) {
      for (List<Element> sourceDeclarations : contextDeclarations.values()) {
        count += sourceDeclarations.size();
      }
    }
    return count;
  }

  @VisibleForTesting
  public int getLocationCount(AnalysisContext context) {
    context = unwrapContext(context);
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
  public String getStatistics() {
    return relationshipCount + " relationships in " + elementCount + " elements in " + sourceCount
        + " sources";
  }

  public int internalGetElementCount() {
    Set<Element> elements = Sets.newHashSet();
    for (ElementRelationKey key : relationshipMap.keySet()) {
      elements.add(key.element);
    }
    return elements.size();
  }

  public int internalGetRelationshipCount() {
    int count = 0;
    for (FastRemoveList<ContributedLocation> contributedLocations : relationshipMap.values()) {
      count += contributedLocations.size();
    }
    return count;
  }

  public int internalGetSourceCount() {
    return sourceCount;
  }

  @Override
  public void readIndex(AnalysisContext context, InputStream input) throws IOException {
    context = unwrapContext(context);
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
      relationshipCount++;
    }
  }

  @Override
  public void recordSourceElements(AnalysisContext context, Source source, List<Element> elements) {
    if (removedContexts.containsKey(context)) {
      return;
    }
    // prepare AnalysisContext declarations
    Map<Source, List<Element>> contextElements = sourceToDeclarations.get(context);
    if (contextElements == null) {
      contextElements = Maps.newHashMap();
      sourceToDeclarations.put(context, contextElements);
    }
    // remember Element in Source
    List<Element> sourceElements = contextElements.get(source);
    if (sourceElements == null) {
      sourceElements = Lists.newArrayList();
      contextElements.put(source, sourceElements);
    }
    sourceElements.addAll(elements);
    elementCount += elements.size();
  }

  @Override
  public void removeContext(AnalysisContext context) {
    context = unwrapContext(context);
    removedContexts.put(context, WEAK_SET_VALUE);
    // remove context sources
    {
      Set<Source> contextSources = sources.remove(context);
      if (contextSources != null) {
        sourceCount -= contextSources.size();
      }
    }
    // remove elements declared in Source(s) of removed context
    Map<Source, List<Element>> contextElements = sourceToDeclarations.remove(context);
    if (contextElements != null) {
      for (List<Element> sourceElements : contextElements.values()) {
        elementCount -= sourceElements.size();
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
    context = unwrapContext(context);
    {
      Set<Source> contextSources = sources.get(context);
      if (contextSources != null) {
        boolean removed = contextSources.remove(source);
        if (removed) {
          sourceCount--;
        }
      }
    }
    // remove relationships with elements declared in removed source
    Map<Source, List<Element>> contextElements = sourceToDeclarations.get(context);
    if (contextElements != null) {
      List<Element> sourceElements = contextElements.remove(source);
      if (sourceElements != null) {
        elementCount -= sourceElements.size();
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
    context = unwrapContext(context);
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
    context = unwrapContext(context);
    new MemoryIndexWriter(this, context, output).write();
  }

  private void addSource(AnalysisContext context, Source source) {
    Set<Source> contextSources = sources.get(context);
    if (contextSources == null) {
      contextSources = Sets.newHashSet();
      sources.put(context, contextSources);
    }
    boolean added = contextSources.add(source);
    if (added) {
      sourceCount++;
    }
  }

  private void removeSourceContributedLocations(FastRemoveList<ContributedLocation> sourceLocations) {
    for (ContributedLocation contributedLocation : sourceLocations) {
      contributedLocation.removeFromLocationOwner();
      relationshipCount--;
    }
  }

  private void removeSourceDeclaredElements(List<Element> sourceElements) {
    for (Element sourceElement : sourceElements) {
      for (Relationship relationship : Relationship.values()) {
        ElementRelationKey relKey = new ElementRelationKey(sourceElement, relationship);
        FastRemoveList<ContributedLocation> contributedLocations = relationshipMap.remove(relKey);
        if (contributedLocations != null) {
          for (ContributedLocation contributedLocation : contributedLocations) {
            contributedLocation.removeFromDeclarationOwner();
            relationshipCount--;
          }
        }
      }
    }
  }
}
