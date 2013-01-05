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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.MemoryIndexStore;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.source.Source;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link IndexStore} which keeps full index in memory.
 */
public class MemoryIndexStoreImpl implements MemoryIndexStore {
//  /**
//   * @return the {@link Source} which contains given {@link Element}, may be <code>null</code>.
//   */
//  private static Source getSource(Element element) {
//    while (element != null) {
//      if (element instanceof CompilationUnitElement) {
//        return ((CompilationUnitElement) element).getSource();
//      }
//      element = element.getEnclosingElement();
//    }
//    return null;
//  }

  /**
   * A table mapping elements to tables mapping relationships to lists of locations.
   */
  private Map<ElementLocation, Map<Relationship, List<ContributedLocation>>> relationshipMap = Maps.newHashMapWithExpectedSize(4096);

  /**
   * A table mapping the resources that are known to the index to a set of the elements known to be
   * in those resources.
   */
  private Map<Source, Set<ElementLocation>> sourceToElementsMap = Maps.newHashMapWithExpectedSize(1024);

  /**
   * Contains {@link ContributedLocation}s contributed by their
   * {@link ContributedLocation#getContributor()}.
   */
  private Map<Source, List<ContributedLocation>> contributorToContributedLocations = Maps.newHashMap();

  @Override
  public void clear() {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public int getElementCount() {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public int getRelationshipCount() {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public Location[] getRelationships(ElementLocation elementLocation, Relationship relationship) {
    Map<Relationship, List<ContributedLocation>> elementRelationshipMap = relationshipMap.get(elementLocation);
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
  public int getResourceCount() {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean readIndex(InputStream input) {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public void recordRelationship(Source contributor, ElementLocation element,
      Relationship relationship, Location location) {
    if (contributor == null || element == null || location == null) {
      return;
    }
//    recordElement(element);
//    recordElement(location.getElement());
    // add ContributedLocation for "element"
    ContributedLocation contributedLocation;
    {
      Map<Relationship, List<ContributedLocation>> elementRelationshipMap = relationshipMap.get(element);
      if (elementRelationshipMap == null) {
        elementRelationshipMap = Maps.newHashMap();
        relationshipMap.put(element, elementRelationshipMap);
      }
      List<ContributedLocation> locations = elementRelationshipMap.get(relationship);
      if (locations == null) {
        locations = Lists.newArrayList();
        elementRelationshipMap.put(relationship, locations);
      }
      contributedLocation = new ContributedLocation(locations, contributor, location);
    }
    // add to "contributor" -> "locations" map
//    recordContributorToLocation(contributor, contributedLocation);
  }

  @Override
  public void regenerateResource(Source source) {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeResource(Source source) {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeIndex(ObjectOutputStream output) throws IOException {
    // TODO(scheglov)
    throw new UnsupportedOperationException();
  }

//  /**
//   * Initialize a newly created index to be empty.
//   */
//  public MemoryIndexStoreImpl() {
//    super();
//  }
//
//  // TODO(scheglov) read/write
////  /**
////   * Return a reader that can read the contents of this index from a stream.
////   * 
////   * @return a reader that can read the contents of this index from a stream
////   */
////  public IndexReader createIndexReader() {
////    return new IndexReader(this);
////  }
////
////  /**
////   * Return a writer that can write the contents of this index to a stream.
////   * 
////   * @return a writer that can write the contents of this index to a stream
////   */
////  public IndexWriter createIndexWriter() {
////    return new IndexWriter(attributeMap, relationshipMap);
////  }
//
//  /**
//   * Remove all data from this index.
//   */
//  @Override
//  public void clear() {
//    sourceToElementsMap.clear();
//    relationshipMap.clear();
//  }
//
//  /**
//   * Return the number of elements that are currently recorded in this index.
//   * 
//   * @return the number of elements that are currently recorded in this index
//   */
//  @Override
//  public int getElementCount() {
//    int count = 0;
//    for (Set<Element> elementSet : sourceToElementsMap.values()) {
//      count += elementSet.size();
//    }
//    return count;
//  }
//
//  /**
//   * Return the number of relationships that are currently recorded in this index.
//   * 
//   * @return the number of relationships that are currently recorded in this index
//   */
//  @Override
//  public int getRelationshipCount() {
//    int count = 0;
//    for (Map<Relationship, List<ContributedLocation>> elementRelationshipMap : relationshipMap.values()) {
//      for (List<ContributedLocation> contributedLocations : elementRelationshipMap.values()) {
//        count += contributedLocations.size();
//      }
//    }
//    return count;
//  }
//
//  /**
//   * Return the locations of the elements that have the given relationship with the given element.
//   * For example, if the element represents a method and the relationship is the is-referenced-by
//   * relationship, then the returned locations will be all of the places where the method is
//   * invoked.
//   * 
//   * @param element the element that has the relationship with the locations to be returned
//   * @param relationship the relationship between the given element and the locations to be returned
//   * @return the locations of the elements that have the given relationship with the given element
//   */
//  @Override
//  public Location[] getRelationships(Element element, Relationship relationship) {
//    Map<Relationship, List<ContributedLocation>> elementRelationshipMap = relationshipMap.get(element);
//    if (elementRelationshipMap != null) {
//      List<ContributedLocation> contributedLocations = elementRelationshipMap.get(relationship);
//      if (contributedLocations != null) {
//        int count = contributedLocations.size();
//        Location[] locations = new Location[count];
//        for (int i = 0; i < count; i++) {
//          locations[i] = contributedLocations.get(i).getLocation();
//        }
//        return locations;
//      }
//    }
//    return Location.EMPTY_ARRAY;
//  }
//
//  /**
//   * Return the number of resources that are currently recorded in this index.
//   * 
//   * @return the number of resources that are currently recorded in this index
//   */
//  @Override
//  public int getResourceCount() {
//    return sourceToElementsMap.size();
//  }
//
//  /**
//   * Record that the given element and location have the given relationship. For example, if the
//   * relationship is the is-referenced-by relationship, then the element would be the element being
//   * referenced and the location would be the point at which it is referenced. Each element can have
//   * the same relationship with multiple locations. In other words, if the following code were
//   * executed
//   * 
//   * <pre>
//   *   recordRelationship(element, isReferencedBy, location1);
//   *   recordRelationship(element, isReferencedBy, location2);
//   * </pre>
//   * 
//   * then both relationships would be maintained in the index and the result of executing
//   * 
//   * <pre>
//   *   getRelationship(element, isReferencedBy);
//   * </pre>
//   * 
//   * would be an array containing both <code>location1</code> and <code>location2</code>.
//   * 
//   * @param contributor the source that was being analyzed when this relationship was contributed
//   * @param element the element that is related to the location
//   * @param relationship the relationship between the element and the location
//   * @param location the location that is related to the element
//   */
//  @Override
//  public void recordRelationship(Source contributor, Element element, Relationship relationship,
//      Location location) {
//    if (contributor == null || element == null || location == null) {
//      return;
//    }
//    recordElement(element);
//    recordElement(location.getElement());
//    // add ContributedLocation for "element"
//    ContributedLocation contributedLocation;
//    {
//      Map<Relationship, List<ContributedLocation>> elementRelationshipMap = relationshipMap.get(element);
//      if (elementRelationshipMap == null) {
//        elementRelationshipMap = Maps.newHashMap();
//        relationshipMap.put(element, elementRelationshipMap);
//      }
//      List<ContributedLocation> locations = elementRelationshipMap.get(relationship);
//      if (locations == null) {
//        locations = Lists.newArrayList();
//        elementRelationshipMap.put(relationship, locations);
//      }
//      contributedLocation = new ContributedLocation(locations, contributor, location);
//    }
//    // add to "contributor" -> "locations" map
//    recordContributorToLocation(contributor, contributedLocation);
//  }
//
//  /**
//   * Remove from the index all of the information associated that was contribute as a result of
//   * analyzing the given source. This includes relationships between an element in the given source
//   * and any other locations and relationships between any other elements and a location within the
//   * given source.
//   * <p>
//   * This method should be invoked when a source is about to be re-analyzed.
//   * 
//   * @param source the source being re-analyzed
//   */
//  @Override
//  public void regenerateResource(Source source) {
//    sourceToElementsMap.remove(source);
//
//    List<ContributedLocation> locations = contributorToContributedLocations.remove(source);
//    if (locations != null) {
//      for (ContributedLocation location : locations) {
//        location.getOwner().remove(location);
//      }
//    }
//  }
//
//  /**
//   * Remove from the index all of the information associated with elements or locations in the given
//   * source. This includes relationships between an element in the given source and any other
//   * locations, relationships between any other elements and a location within the given source.
//   * <p>
//   * This method should be invoked when a source is no longer part of the code base.
//   * 
//   * @param source the source being removed
//   */
//  @Override
//  public void removeResource(Source source) {
//    Set<Element> elements = sourceToElementsMap.remove(source);
//    if (elements != null) {
//      for (Element element : elements) {
//        removeElement(element);
//      }
//    }
//
//    Set<Entry<Element, Map<Relationship, List<ContributedLocation>>>> relationshipSet = relationshipMap.entrySet();
//    Iterator<Entry<Element, Map<Relationship, List<ContributedLocation>>>> relationshipSetIterator = relationshipSet.iterator();
//    while (relationshipSetIterator.hasNext()) {
//      Set<Entry<Relationship, List<ContributedLocation>>> elementRelationshipMap = relationshipSetIterator.next().getValue().entrySet();
//      Iterator<Entry<Relationship, List<ContributedLocation>>> relationshipIterator = elementRelationshipMap.iterator();
//      while (relationshipIterator.hasNext()) {
//        List<ContributedLocation> locations = relationshipIterator.next().getValue();
//        Iterator<ContributedLocation> locationIterator = locations.iterator();
//        while (locationIterator.hasNext()) {
//          ContributedLocation location = locationIterator.next();
//          if (location.getContributor().equals(source)
//              || Objects.equal(getSource(location.getLocation().getElement()), source)) {
//            locationIterator.remove();
//          }
//        }
//        if (locations.isEmpty()) {
//          relationshipIterator.remove();
//        }
//      }
//      if (elementRelationshipMap.isEmpty()) {
//        relationshipSetIterator.remove();
//      }
//    }
//  }
//
////  private int getRelationshipCount() {
////    int count = 0;
////    for (HashMap<Relationship, ArrayList<Location>> elementMap : relationshipMap.values()) {
////      for (ArrayList<Location> locationList : elementMap.values()) {
////        count += locationList.size();
////      }
////    }
////    return count;
////  }
//
//  @Override
//  public String toString() {
//    PrintStringWriter writer = new PrintStringWriter();
//    writeIndex(writer);
//    return writer.toString();
//  }
//
//  private void recordContributorToLocation(Source contributor,
//      ContributedLocation contributedLocation) {
//    List<ContributedLocation> locations = contributorToContributedLocations.get(contributor);
//    if (locations == null) {
//      locations = Lists.newArrayList();
//      contributorToContributedLocations.put(contributor, locations);
//    }
//    locations.add(contributedLocation);
//  }
//
//  /**
//   * Add the given element to the source-to-element map.
//   * 
//   * @param contributor the source which contributes the element
//   * @param element the element to be added to the map
//   */
//  private void recordElement(Element element) {
//    Source contributor = getSource(element);
//    Set<Element> elementSet = sourceToElementsMap.get(contributor);
//    if (elementSet == null) {
//      elementSet = Sets.newHashSet();
//      sourceToElementsMap.put(contributor, elementSet);
//    }
//    elementSet.add(element);
//  }
//
//  /**
//   * Remove from the index all of the information associated with the given element. This includes
//   * relationships between the element and any locations, relationships between any other elements
//   * and a location within the given element, and any values of any attributes defined the element.
//   * 
//   * @param element the element being removed
//   */
//  private void removeElement(Element element) {
//    relationshipMap.remove(element);
//  }
//
//  /**
//   * Write the contents of this index to the given print writer. This is intended to be used for
//   * debugging purposes, not as something that would be displayed to users.
//   * 
//   * @param writer the writer to which the contents of the index will be written
//   */
//  private void writeIndex(PrintWriter writer) {
//    writer.println("Relationship Map");
//    writer.println();
//    if (relationshipMap.isEmpty()) {
//      writer.println("  -- empty --");
//    } else {
//      for (Map.Entry<Element, Map<Relationship, List<ContributedLocation>>> elementEntry : relationshipMap.entrySet()) {
//        writer.print("  ");
//        writer.println(elementEntry.getKey());
//        for (Map.Entry<Relationship, List<ContributedLocation>> relationshipEntry : elementEntry.getValue().entrySet()) {
//          String relationship = relationshipEntry.getKey().toString();
//          for (ContributedLocation location : relationshipEntry.getValue()) {
//            writer.print("    ");
//            writer.print(relationship);
//            writer.print(" ");
//            writer.print(location.getLocation());
//            writer.print(" (contributed by ");
//            writer.print(location.getContributor());
//            writer.print(")");
//          }
//        }
//      }
//    }
//
//    writer.println();
//    writer.println("Resource to Element Map");
//    writer.println();
//    if (sourceToElementsMap.isEmpty()) {
//      writer.println("  -- empty --");
//    } else {
//      for (Map.Entry<Source, Set<Element>> resourceEntry : sourceToElementsMap.entrySet()) {
//        writer.print("  ");
//        writer.println(resourceEntry.getKey());
//        for (Element element : resourceEntry.getValue()) {
//          writer.print("    ");
//          writer.println(element);
//        }
//      }
//    }
//  }
}
