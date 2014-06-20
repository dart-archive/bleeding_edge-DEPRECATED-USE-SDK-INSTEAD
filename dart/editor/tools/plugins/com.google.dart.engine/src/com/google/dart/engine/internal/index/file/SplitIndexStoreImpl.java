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
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.UniverseElement;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An {@link IndexStore} which keeps index information in separate nodes for each unit.
 * 
 * @coverage dart.engine.index
 */
public class SplitIndexStoreImpl implements IndexStore {
  /**
   * The {@link NodeManager} to get/put {@link IndexNode}s.
   */
  private final NodeManager nodeManager;

  /**
   * The {@link ContextCodec} to encode/decode {@link AnalysisContext}s.
   */
  private final ContextCodec contextCodec;

  /**
   * The {@link ElementCodec} to encode/decode {@link Element}s.
   */
  private final ElementCodec elementCodec;

  /**
   * The {@link StringCodec} to encode/decode {@link String}s.
   */
  private final StringCodec stringCodec;

  /**
   * A table mapping element names to the node names that may have relations with elements with
   * these names.
   */
  private final IntToIntSetMap nameToNodeNames = new IntToIntSetMap(10000, 0.75f);

  /**
   * Information about "universe" elements. We need to keep them together to avoid loading of all
   * index nodes.
   * <p>
   * Order of keys: contextId, nodeId, Relationship.
   */
  private final Map<Integer, Map<Integer, Map<Relationship, List<LocationData>>>> contextNodeRelations = Maps.newHashMap();

  /**
   * The mapping of library {@link Source} to the {@link Source}s of part units.
   */
  final Map<AnalysisContext, Map<Source, Set<Source>>> contextToLibraryToUnits = Maps.newHashMap();

  /**
   * The mapping of unit {@link Source} to the {@link Source}s of libraries it is used in.
   */
  final Map<AnalysisContext, Map<Source, Set<Source>>> contextToUnitToLibraries = Maps.newHashMap();

  /**
   * The set of known {@link Source}s.
   */
  private final Set<Source> sources = Sets.newHashSet();

  private int currentContextId;
  private String currentNodeName;
  private int currentNodeNameId;
  private IndexNode currentNode;

  public SplitIndexStoreImpl(NodeManager nodeManager) {
    this.nodeManager = nodeManager;
    this.contextCodec = nodeManager.getContextCodec();
    this.elementCodec = nodeManager.getElementCodec();
    this.stringCodec = nodeManager.getStringCodec();
  }

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
    // remember library/unit relations
    recordUnitInLibrary(context, library, unit);
    recordLibraryWithUnit(context, library, unit);
    sources.add(library);
    sources.add(unit);
    // prepare node
    String libraryName = library.getFullName();
    String unitName = unit.getFullName();
    int libraryNameIndex = stringCodec.encode(libraryName);
    int unitNameIndex = stringCodec.encode(unitName);
    currentNodeName = libraryNameIndex + "_" + unitNameIndex + ".index";
    currentNodeNameId = stringCodec.encode(currentNodeName);
    currentNode = nodeManager.newNode(context);
    currentContextId = contextCodec.encode(context);
    // remove Universe information for the current node
    for (Map<Integer, ?> nodeRelations : contextNodeRelations.values()) {
      nodeRelations.remove(currentNodeNameId);
    }
    // done
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
    // remember library/unit relations
    recordUnitInLibrary(context, null, source);
    // prepare node
    String sourceName = source.getFullName();
    int sourceNameIndex = stringCodec.encode(sourceName);
    currentNodeName = sourceNameIndex + ".index";
    currentNodeNameId = stringCodec.encode(currentNodeName);
    currentNode = nodeManager.newNode(context);
    return true;
  }

  @Override
  public void clear() {
    nodeManager.clear();
    nameToNodeNames.clear();
  }

  @Override
  public void doneIndex() {
    if (currentNode != null) {
      nodeManager.putNode(currentNodeName, currentNode);
      currentNodeName = null;
      currentNodeNameId = -1;
      currentNode = null;
      currentContextId = -1;
    }
  }

  @Override
  public Location[] getRelationships(Element element, Relationship relationship) {
    // special support for UniverseElement
    if (element == UniverseElement.INSTANCE) {
      return getRelationshipsUniverse(relationship);
    }
    // prepare node names
    String name = getElementName(element);
    int nameId = stringCodec.encode(name);
    int[] nodeNameIds = nameToNodeNames.get(nameId);
    // check each node
    List<Location> locations = Lists.newArrayList();
    for (int i = 0; i < nodeNameIds.length; i++) {
      int nodeNameId = nodeNameIds[i];
      String nodeName = stringCodec.decode(nodeNameId);
      IndexNode node = nodeManager.getNode(nodeName);
      if (node != null) {
        Collections.addAll(locations, node.getRelationships(element, relationship));
      } else {
        nodeNameIds = ArrayUtils.removeElement(nodeNameIds, nodeNameId);
        i--;
      }
    }
    // done
    return locations.toArray(new Location[locations.size()]);
  }

  @Override
  public String getStatistics() {
    return "[" + nodeManager.getLocationCount() + " locations, " + sources.size() + " sources, "
        + nameToNodeNames.size() + " names]";
  }

  @Override
  public void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element == null || location == null) {
      return;
    }
    // special support for UniverseElement
    if (element == UniverseElement.INSTANCE) {
      recordRelationshipUniverse(relationship, location);
      return;
    }
    // other elements
    recordNodeNameForElement(element);
    currentNode.recordRelationship(element, relationship, location);
  }

  @Override
  public void removeContext(AnalysisContext context) {
    context = unwrapContext(context);
    if (context == null) {
      return;
    }
    // remove sources
    removeSources(context, null);
    // remove context information
    contextToLibraryToUnits.remove(context);
    contextToUnitToLibraries.remove(context);
    contextNodeRelations.remove(contextCodec.encode(context));
    // remove context from codec
    contextCodec.removeContext(context);
  }

  @Override
  public void removeSource(AnalysisContext context, Source source) {
    context = unwrapContext(context);
    if (context == null) {
      return;
    }
    // remove nodes for unit/library pairs
    Map<Source, Set<Source>> unitToLibraries = contextToUnitToLibraries.get(context);
    if (unitToLibraries != null) {
      Set<Source> libraries = unitToLibraries.remove(source);
      if (libraries != null) {
        for (Source library : libraries) {
          removeLocations(context, library, source);
        }
      }
    }
    // remove nodes for library/unit pairs
    Map<Source, Set<Source>> libraryToUnits = contextToLibraryToUnits.get(context);
    if (libraryToUnits != null) {
      Set<Source> units = libraryToUnits.remove(source);
      if (units != null) {
        for (Source unit : units) {
          removeLocations(context, source, unit);
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
    // remove nodes for unit/library pairs
    Map<Source, Set<Source>> unitToLibraries = contextToUnitToLibraries.get(context);
    if (unitToLibraries != null) {
      List<Source> units = Lists.newArrayList(unitToLibraries.keySet());
      for (Source source : units) {
        if (container == null || container.contains(source)) {
          removeSource(context, source);
        }
      }
    }
    // remove nodes for library/unit pairs
    Map<Source, Set<Source>> libraryToUnits = contextToLibraryToUnits.get(context);
    if (libraryToUnits != null) {
      List<Source> libraries = Lists.newArrayList(libraryToUnits.keySet());
      for (Source source : libraries) {
        if (container == null || container.contains(source)) {
          removeSource(context, source);
        }
      }
    }
  }

  private String getElementName(Element element) {
    return element.getName();
  }

  private Location[] getRelationshipsUniverse(Relationship relationship) {
    List<Location> locations = Lists.newArrayList();
    for (Entry<Integer, Map<Integer, Map<Relationship, List<LocationData>>>> contextEntry : contextNodeRelations.entrySet()) {
      int contextId = contextEntry.getKey();
      AnalysisContext context = contextCodec.decode(contextId);
      if (context != null) {
        for (Map<Relationship, List<LocationData>> nodeRelations : contextEntry.getValue().values()) {
          List<LocationData> nodeLocations = nodeRelations.get(relationship);
          if (nodeLocations != null) {
            for (LocationData locationData : nodeLocations) {
              Location location = locationData.getLocation(context, elementCodec);
              if (location != null) {
                locations.add(location);
              }
            }
          }
        }
      }
    }
    return locations.toArray(new Location[locations.size()]);
  }

  private void recordLibraryWithUnit(AnalysisContext context, Source library, Source unit) {
    Map<Source, Set<Source>> libraryToUnits = contextToLibraryToUnits.get(context);
    if (libraryToUnits == null) {
      libraryToUnits = Maps.newHashMap();
      contextToLibraryToUnits.put(context, libraryToUnits);
    }
    Set<Source> units = libraryToUnits.get(library);
    if (units == null) {
      units = Sets.newHashSet();
      libraryToUnits.put(library, units);
    }
    units.add(unit);
  }

  private void recordNodeNameForElement(Element element) {
    String name = getElementName(element);
    int nameId = stringCodec.encode(name);
    nameToNodeNames.add(nameId, currentNodeNameId);
  }

  private void recordRelationshipUniverse(Relationship relationship, Location location) {
    // in current context
    Map<Integer, Map<Relationship, List<LocationData>>> nodeRelations = contextNodeRelations.get(currentContextId);
    if (nodeRelations == null) {
      nodeRelations = Maps.newHashMap();
      contextNodeRelations.put(currentContextId, nodeRelations);
    }
    // in current node
    Map<Relationship, List<LocationData>> relations = nodeRelations.get(currentNodeNameId);
    if (relations == null) {
      relations = Maps.newHashMap();
      nodeRelations.put(currentNodeNameId, relations);
    }
    // for the given relationship
    List<LocationData> locations = relations.get(relationship);
    if (locations == null) {
      locations = Lists.newArrayList();
      relations.put(relationship, locations);
    }
    // record LocationData
    locations.add(new LocationData(elementCodec, location));
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
    // remove node
    String libraryName = library != null ? library.getFullName() : null;
    String unitName = unit.getFullName();
    int libraryNameIndex = stringCodec.encode(libraryName);
    int unitNameIndex = stringCodec.encode(unitName);
    String nodeName = libraryNameIndex + "_" + unitNameIndex + ".index";
    nodeManager.removeNode(nodeName);
    // remove source
    sources.remove(library);
    sources.remove(unit);
  }

  /**
   * When logging is on, {@link AnalysisEngine} actually creates
   * {@link InstrumentedAnalysisContextImpl}, which wraps {@link AnalysisContextImpl} used to create
   * actual {@link Element}s. So, in index we have to unwrap {@link InstrumentedAnalysisContextImpl}
   * when perform any operation.
   */
  private AnalysisContext unwrapContext(AnalysisContext context) {
    if (context instanceof InstrumentedAnalysisContextImpl) {
      context = ((InstrumentedAnalysisContextImpl) context).getBasis();
    }
    return context;
  }
}
