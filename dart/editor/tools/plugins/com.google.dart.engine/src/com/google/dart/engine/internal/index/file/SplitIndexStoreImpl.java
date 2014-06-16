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
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InstrumentedAnalysisContextImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
   * The {@link StringCodec} to encode/decode {@link String}s.
   */
  private final StringCodec stringCodec;

  /**
   * A table mapping element names to the node names that may have relations with elements with
   * these names.
   */
  private final Map<Integer, int[]> nameToNodeNames = Maps.newHashMap();

  /**
   * The mapping of library {@link Source} to the {@link Source}s of part units.
   */
  final Map<AnalysisContext, Map<Source, Set<Source>>> contextToLibraryToUnits = Maps.newHashMap();

  /**
   * The mapping of unit {@link Source} to the {@link Source}s of libraries it is used in.
   */
  final Map<AnalysisContext, Map<Source, Set<Source>>> contextToUnitToLibraries = Maps.newHashMap();

  private final Set<Source> sources = Sets.newHashSet();

  private String currentNodeName;
  private int currentNodeNameId;
  private IndexNode currentNode;

  public SplitIndexStoreImpl(NodeManager nodeManager) {
    this.nodeManager = nodeManager;
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
      currentNode = null;
      currentNodeName = null;
    }
  }

  @Override
  public Location[] getRelationships(Element element, Relationship relationship) {
    // prepare node names
    String name = getElementName(element);
    int nameId = stringCodec.encode(name);
    int[] nodeNameIds = nameToNodeNames.get(nameId);
    if (nodeNameIds == null) {
      return Location.EMPTY_ARRAY;
    }
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
    int[] nodeNameIds = nameToNodeNames.get(nameId);
    if (nodeNameIds == null) {
      nodeNameIds = new int[] {currentNodeNameId};
      nameToNodeNames.put(nameId, nodeNameIds);
      return;
    }
    if (ArrayUtils.indexOf(nodeNameIds, currentNodeNameId) == -1) {
      nodeNameIds = ArrayUtils.add(nodeNameIds, currentNodeNameId);
      nameToNodeNames.put(nameId, nodeNameIds);
    }
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
