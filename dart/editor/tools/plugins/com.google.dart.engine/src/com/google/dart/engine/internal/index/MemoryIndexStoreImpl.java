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
import com.google.common.collect.Lists;
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
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link IndexStore} which keeps full index in memory.
 */
public class MemoryIndexStoreImpl implements MemoryIndexStore {

  /**
   * @return the {@link Source} which contains given {@link Element}, may be <code>null</code>.
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
   * A table mapping elements to tables mapping relationships to lists of locations.
   */
  final Map<Element, Map<Relationship, List<ContributedLocation>>> relationshipMap = Maps.newHashMapWithExpectedSize(4096);

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
  final Map<AnalysisContext, Map<Source, List<ContributedLocation>>> sourceToLocations = Maps.newHashMapWithExpectedSize(64);

  @Override
  public void clear() {
    relationshipMap.clear();
  }

  @VisibleForTesting
  public int getDeclarationCount(AnalysisContext context) {
    int count = 0;
    Map<Source, List<Element>> contextDeclarations = sourceToDeclarations.get(context);
    if (contextDeclarations != null) {
      for (List<Element> sourceDeclarations : contextDeclarations.values()) {
        count += sourceDeclarations.size();
      }
    }
    return count;
  }

  @Override
  public int getElementCount() {
    return relationshipMap.size();
  }

  @VisibleForTesting
  public int getLocationCount(AnalysisContext context) {
    int count = 0;
    Map<Source, List<ContributedLocation>> contextLocations = sourceToLocations.get(context);
    if (contextLocations != null) {
      for (List<ContributedLocation> contributedLocations : contextLocations.values()) {
        count += contributedLocations.size();
      }
    }
    return count;
  }

  @Override
  public int getRelationshipCount() {
    int count = 0;
    for (Map<Relationship, List<ContributedLocation>> elementRelationshipMap : relationshipMap.values()) {
      for (List<ContributedLocation> contributedLocations : elementRelationshipMap.values()) {
        count += contributedLocations.size();
      }
    }
    return count;
  }

  @Override
  public Location[] getRelationships(Element element, Relationship relationship) {
    Map<Relationship, List<ContributedLocation>> elementRelationshipMap = relationshipMap.get(element);
    if (elementRelationshipMap != null) {
      List<ContributedLocation> contributedLocations = elementRelationshipMap.get(relationship);
      if (contributedLocations != null) {
        int count = contributedLocations.size();
        Location[] locations = new Location[count];
        for (int i = 0; i < count; i++) {
          locations[i] = contributedLocations.get(i).getLocation();
        }
        return locations;
      }
    }
    return Location.EMPTY_ARRAY;
  }

  @Override
  public boolean readIndex(AnalysisContext context, InputStream input) {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
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
    // remember sources
    addSource(elementContext, elementSource);
    addSource(locationContext, locationSource);
    recordSourceToDeclarations(elementContext, elementSource, element);
    //
    Map<Source, List<ContributedLocation>> contextLocations = sourceToLocations.get(locationContext);
    if (contextLocations == null) {
      contextLocations = Maps.newHashMapWithExpectedSize(1024);
      sourceToLocations.put(locationContext, contextLocations);
    }
    //
    List<ContributedLocation> sourceLocations = contextLocations.get(locationSource);
    if (sourceLocations == null) {
      sourceLocations = Lists.newArrayList();
      contextLocations.put(locationSource, sourceLocations);
    }
    // add ContributedLocation for "element"
    {
      Map<Relationship, List<ContributedLocation>> elementRelMap = relationshipMap.get(element);
      if (elementRelMap == null) {
        elementRelMap = Maps.newHashMap();
        relationshipMap.put(element, elementRelMap);
      }
      List<ContributedLocation> locations = elementRelMap.get(relationship);
      if (locations == null) {
        locations = Lists.newArrayList();
        elementRelMap.put(relationship, locations);
      }
      new ContributedLocation(sourceLocations, locations, location);
    }
  }

  @Override
  public void removeContext(AnalysisContext context) {
    // remove elements declared in Source(s) of removed context
    Map<Source, List<Element>> contextElements = sourceToDeclarations.remove(context);
    if (contextElements != null) {
      for (List<Element> sourceElements : contextElements.values()) {
        removeSourceDeclaredElements(sourceElements);
      }
    }
    // remove relationships in Source(s) of removed context
    Map<Source, List<ContributedLocation>> contextLocations = sourceToLocations.remove(context);
    if (contextLocations != null) {
      for (List<ContributedLocation> sourceLocations : contextLocations.values()) {
        removeSourceContributedLocations(sourceLocations);
      }
    }
  }

  @Override
  public void removeSource(AnalysisContext context, Source source) {
    sources.remove(source);
    // remove relationships with elements declared in removed source
    Map<Source, List<Element>> contextElements = sourceToDeclarations.get(context);
    if (contextElements != null) {
      List<Element> sourceElements = contextElements.remove(source);
      if (sourceElements != null) {
        removeSourceDeclaredElements(sourceElements);
      }
    }
    // remove relationships in removed source
    Map<Source, List<ContributedLocation>> contextLocations = sourceToLocations.get(context);
    if (contextLocations != null) {
      List<ContributedLocation> sourceLocations = contextLocations.remove(source);
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
      contextSources = Sets.newHashSetWithExpectedSize(256);
      sources.put(context, contextSources);
    }
    contextSources.add(source);
  }

  private void recordSourceToDeclarations(AnalysisContext context, Source elementSource,
      Element declaredElement) {
    // prepare AnalysisContext declarations
    Map<Source, List<Element>> contextElements = sourceToDeclarations.get(context);
    if (contextElements == null) {
      contextElements = Maps.newHashMapWithExpectedSize(1024);
      sourceToDeclarations.put(context, contextElements);
    }
    // remember Element in Source
    List<Element> sourceElements = contextElements.get(elementSource);
    if (sourceElements == null) {
      sourceElements = Lists.newArrayList();
      contextElements.put(elementSource, sourceElements);
    }
    sourceElements.add(declaredElement);
  }

  private void removeSourceContributedLocations(List<ContributedLocation> sourceLocations) {
    for (ContributedLocation contributedLocation : sourceLocations) {
      contributedLocation.getLocationOwner().remove(contributedLocation);
    }
  }

  private void removeSourceDeclaredElements(List<Element> sourceElements) {
    for (Element declaredElement : sourceElements) {
      Map<Relationship, List<ContributedLocation>> elementRelationshipMap = relationshipMap.remove(declaredElement);
      if (elementRelationshipMap != null) {
        for (List<ContributedLocation> contributedLocations : elementRelationshipMap.values()) {
          for (ContributedLocation contributedLocation : contributedLocations) {
            contributedLocation.getDeclarationOwner().remove(contributedLocation);
          }
        }
      }
    }
  }
}
