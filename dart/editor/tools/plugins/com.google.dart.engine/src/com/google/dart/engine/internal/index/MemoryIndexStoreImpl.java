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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.MemoryIndexStore;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.internal.element.member.Member;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

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
      return other.relationship == relationship && Objects.equal(other.element, element);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(element, relationship);
    }

    @Override
    public String toString() {
      return element + " " + relationship;
    }
  }

  private static final Object WEAK_SET_VALUE = new Object();

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

  /**
   * We add {@link AnalysisContext} to this weak set to ensure that we don't continue to add
   * relationships after some context was removing using {@link #removeContext(AnalysisContext)}.
   */
  private final Map<AnalysisContext, Object> removedContexts = new MapMaker().weakKeys().makeMap();

  /**
   * The mapping of {@link ElementRelationKey} to the {@link Location}s, one-to-many.
   */
  final Map<ElementRelationKey, Set<Location>> keyToLocations = Maps.newHashMap();

  /**
   * The mapping of {@link Source} to the {@link ElementRelationKey}s. It is used in
   * {@link #removeSource(AnalysisContext, Source)} to identify keys to remove from
   * {@link #keyToLocations}.
   */
  final Map<AnalysisContext, Map<Source, Set<ElementRelationKey>>> contextToSourceToKeys = Maps.newHashMap();

  /**
   * The mapping of {@link Source} to the {@link Location}s existing in it. It is used in
   * {@link #clearSource(AnalysisContext, Source)} to identify locations to remove from
   * {@link #keyToLocations}.
   */
  final Map<AnalysisContext, Map<Source, List<Location>>> contextToSourceToLocations = Maps.newHashMap();

  private int sourceCount;
  private int keyCount;
  private int locationCount;

  @Override
  public void clearSource(AnalysisContext context, Source source) {
    context = unwrapContext(context);
    Map<Source, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
    Set<ElementRelationKey> keys = sourceToKeys != null ? sourceToKeys.get(source) : null;
    // remove locations within given Source
    Map<Source, List<Location>> sourceToLocations = contextToSourceToLocations.get(context);
    if (sourceToLocations != null) {
      List<Location> sourceLocations = sourceToLocations.remove(source);
      if (sourceLocations != null) {
        for (Location location : sourceLocations) {
          ElementRelationKey key = (ElementRelationKey) location.internalKey;
          Set<Location> relLocations = keyToLocations.get(key);
          if (relLocations != null) {
            relLocations.remove(location);
            locationCount--;
            // no locations with this key
            if (relLocations.isEmpty()) {
              keyToLocations.remove(key);
              keyCount--;
              // remove key
              if (keys != null) {
                keys.remove(key);
              }
            }
          }
        }
      }
    }
    // if no keys, remove from sourceToKeys
    if (keys != null && keys.isEmpty()) {
      sourceToKeys.remove(source);
      sourceCount--;
    }
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
  public int internalGetLocationCount(AnalysisContext context) {
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
    location = location.clone();
    // prepare information
    AnalysisContext elementContext = element.getContext();
    AnalysisContext locationContext = location.getElement().getContext();
    Source elementSource = element.getSource();
    Source locationSource = location.getElement().getSource();
    // sanity check
    if (locationContext == null) {
      return;
    }
    if (elementContext == null && !(element instanceof NameElementImpl)
        && !(element instanceof UniverseElementImpl)) {
      return;
    }
    // may be already removed in other thread
    if (removedContexts.containsKey(elementContext)) {
      return;
    }
    if (removedContexts.containsKey(locationContext)) {
      return;
    }
    // at the index level we don't care about Member(s)
    if (element instanceof Member) {
      element = ((Member) element).getBaseElement();
    }
    // record: key -> location(s)
    ElementRelationKey key = new ElementRelationKey(element, relationship);
    {
      Set<Location> locations = keyToLocations.remove(key);
      if (locations == null) {
        locations = Sets.newSetFromMap(new IdentityHashMap<Location, Boolean>(4));
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
    // record: element source -> keys
    {
      Map<Source, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(elementContext);
      if (sourceToKeys == null) {
        sourceToKeys = Maps.newHashMap();
        contextToSourceToKeys.put(elementContext, sourceToKeys);
      }
      Set<ElementRelationKey> keys = sourceToKeys.get(elementSource);
      if (keys == null) {
        keys = Sets.newHashSet();
        sourceToKeys.put(elementSource, keys);
        sourceCount++;
      }
      keys.remove(key);
      keys.add(key);
    }
    // record: location source -> locations
    {
      Map<Source, List<Location>> sourceToLocations = contextToSourceToLocations.get(locationContext);
      if (sourceToLocations == null) {
        sourceToLocations = Maps.newHashMap();
        contextToSourceToLocations.put(locationContext, sourceToLocations);
      }
      List<Location> locations = sourceToLocations.get(locationSource);
      if (locations == null) {
        locations = Lists.newArrayList();
        sourceToLocations.put(locationSource, locations);
      }
      locations.add(location);
    }
  }

  @Override
  public void removeContext(AnalysisContext context) {
    context = unwrapContext(context);
    removedContexts.put(context, WEAK_SET_VALUE);
    removeSources(context, null);
    // remove context
    contextToSourceToKeys.remove(context);
    contextToSourceToLocations.remove(context);
  }

  @Override
  public void removeSource(AnalysisContext context, Source source) {
    context = unwrapContext(context);
    // remove locations defined in source
    clearSource(context, source);
    // remove keys for elements defined in source
    Map<Source, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
    if (sourceToKeys != null) {
      Set<ElementRelationKey> keys = sourceToKeys.remove(source);
      if (keys != null) {
        for (ElementRelationKey key : keys) {
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

  @Override
  public void removeSources(AnalysisContext context, SourceContainer container) {
    context = unwrapContext(context);
    // remove sources #1
    Map<Source, Set<ElementRelationKey>> sourceToKeys = contextToSourceToKeys.get(context);
    if (sourceToKeys != null) {
      List<Source> sources = Lists.newArrayList(sourceToKeys.keySet());
      for (Source source : sources) {
        if (container == null || container.contains(source)) {
          removeSource(context, source);
        }
      }
    }
    // remove sources #2
    Map<Source, List<Location>> sourceToLocations = contextToSourceToLocations.get(context);
    if (sourceToLocations != null) {
      List<Source> sources = Lists.newArrayList(sourceToLocations.keySet());
      for (Source source : sources) {
        if (container == null || container.contains(source)) {
          removeSource(context, source);
        }
      }
    }
  }

  @Override
  public void writeIndex(AnalysisContext context, OutputStream output) throws IOException {
    context = unwrapContext(context);
    new MemoryIndexWriter(this, context, output).write();
  }
}
