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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.Relationship;

import java.util.List;
import java.util.Map;

/**
 * A single index file in-memory presentation.
 * 
 * @coverage dart.engine.index
 */
public class IndexNode {
  private final AnalysisContext context;
  private final ElementCodec elementCodec;
  private final RelationshipCodec relationshipCodec;
  private final Map<RelationKeyData, List<LocationData>> relations = Maps.newHashMap();

  public IndexNode(AnalysisContext context, ElementCodec elementCodec,
      RelationshipCodec relationshipCodec) {
    this.context = context;
    this.elementCodec = elementCodec;
    this.relationshipCodec = relationshipCodec;
  }

  /**
   * Returns the {@link AnalysisContext} this node is created for.
   */
  public AnalysisContext getContext() {
    return context;
  }

  /**
   * Returns number of locations in this node.
   */
  public int getLocationCount() {
    int locationCount = 0;
    for (List<LocationData> locations : relations.values()) {
      locationCount += locations.size();
    }
    return locationCount;
  }

  /**
   * Returns the recorded relations.
   */
  public Map<RelationKeyData, List<LocationData>> getRelations() {
    return relations;
  }

  /**
   * Return the locations of the elements that have the given relationship with the given element.
   * 
   * @param element the the element that has the relationship with the locations to be returned
   * @param relationship the {@link Relationship} between the given element and the locations to be
   *          returned
   */
  public Location[] getRelationships(Element element, Relationship relationship) {
    // prepare key
    RelationKeyData key = new RelationKeyData(
        elementCodec,
        relationshipCodec,
        element,
        relationship);
    // find LocationData(s)
    List<LocationData> locationDatas = relations.get(key);
    if (locationDatas == null) {
      return Location.EMPTY_ARRAY;
    }
    // convert to Location(s)
    List<Location> locations = Lists.newArrayList();
    for (LocationData locationData : locationDatas) {
      Location location = locationData.getLocation(context, elementCodec);
      if (location != null) {
        locations.add(location);
      }
    }
    return locations.toArray(new Location[locations.size()]);
  }

  /**
   * Records that the given element and location have the given relationship.
   * 
   * @param element the element that is related to the location
   * @param relationship the {@link Relationship} between the element and the location
   * @param location the {@link Location} where relationship happens
   */
  public void recordRelationship(Element element, Relationship relationship, Location location) {
    RelationKeyData key = new RelationKeyData(
        elementCodec,
        relationshipCodec,
        element,
        relationship);
    // prepare LocationData(s)
    List<LocationData> locationDatas = relations.get(key);
    if (locationDatas == null) {
      locationDatas = Lists.newArrayList();
      relations.put(key, locationDatas);
    }
    // add new LocationData
    locationDatas.add(new LocationData(elementCodec, location));
  }

  /**
   * Sets relations.
   */
  public void setRelations(Map<RelationKeyData, List<LocationData>> relations) {
    this.relations.clear();
    this.relations.putAll(relations);
  }
}
