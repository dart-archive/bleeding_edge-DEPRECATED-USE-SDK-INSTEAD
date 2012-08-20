/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.persistance.IndexReader;
import com.google.dart.tools.core.internal.index.persistance.IndexWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Instances of the class <code>IndexStore</code> store information computed by the index. There are
 * two kinds of information that can be stored: relationships between elements and data associated
 * with elements.
 */
public class IndexStore {
  /**
   * A table mapping elements to tables mapping attributes to values.
   */
  private HashMap<Element, HashMap<Attribute, String>> attributeMap = new HashMap<Element, HashMap<Attribute, String>>(
      1024);

  /**
   * A table mapping elements to tables mapping relationships to lists of locations.
   */
  private HashMap<Element, HashMap<Relationship, ArrayList<ContributedLocation>>> relationshipMap = new HashMap<Element, HashMap<Relationship, ArrayList<ContributedLocation>>>(
      1024);

  /**
   * A table mapping the resources that are known to the index to a set of the elements known to be
   * in those resources.
   */
  private HashMap<Resource, Set<Element>> resourceToElementMap = new HashMap<Resource, Set<Element>>(
      256);

  /**
   * Contains {@link ContributedLocation}s contributed by their
   * {@link ContributedLocation#getContributor()}.
   */
  private Map<Resource, List<ContributedLocation>> contributorToContributedLocations = Maps.newHashMap();

  /**
   * Initialize a newly created index to be empty.
   */
  public IndexStore() {
    super();
  }

  /**
   * Remove all data from this index.
   */
  public void clear() {
    resourceToElementMap.clear();
    attributeMap.clear();
    relationshipMap.clear();
  }

  /**
   * Return a reader that can read the contents of this index from a stream.
   * 
   * @return a reader that can read the contents of this index from a stream
   */
  public IndexReader createIndexReader() {
    return new IndexReader(this);
  }

  /**
   * Return a writer that can write the contents of this index to a stream.
   * 
   * @return a writer that can write the contents of this index to a stream
   */
  public IndexWriter createIndexWriter() {
    return new IndexWriter(attributeMap, relationshipMap);
  }

  /**
   * Return the value of the given attribute that is associated with the given element, or
   * <code>null</code> if there is no value for the attribute.
   * 
   * @param element the element with which the attribute is associated
   * @param attribute the attribute whose value is to be returned
   * @return the value of the given attribute that is associated with the given element
   */
  public String getAttribute(Element element, Attribute attribute) {
    HashMap<Attribute, String> elementAttributeMap = attributeMap.get(element);
    if (elementAttributeMap != null) {
      return elementAttributeMap.get(attribute);
    }
    return null;
  }

  /**
   * Return the number of attributes that are currently recorded in this index.
   * 
   * @return the number of attributes that are currently recorded in this index
   */
  public int getAttributeCount() {
    int count = 0;
    for (HashMap<Attribute, String> elementAttributeMap : attributeMap.values()) {
      count += elementAttributeMap.size();
    }
    return count;
  }

  /**
   * Return the number of elements that are currently recorded in this index.
   * 
   * @return the number of elements that are currently recorded in this index
   */
  public int getElementCount() {
    int count = 0;
    for (Set<Element> elementSet : resourceToElementMap.values()) {
      count += elementSet.size();
    }
    return count;
  }

  /**
   * Return the number of relationships that are currently recorded in this index.
   * 
   * @return the number of relationships that are currently recorded in this index
   */
  public int getRelationshipCount() {
    int count = 0;
    for (HashMap<Relationship, ArrayList<ContributedLocation>> elementRelationshipMap : relationshipMap.values()) {
      for (ArrayList<ContributedLocation> contributedLocations : elementRelationshipMap.values()) {
        count += contributedLocations.size();
      }
    }
    return count;
  }

  /**
   * Return the locations of the elements that have the given relationship with the given element.
   * For example, if the element represents a method and the relationship is the is-referenced-by
   * relationship, then the returned locations will be all of the places where the method is
   * invoked.
   * 
   * @param element the element that has the relationship with the locations to be returned
   * @param relationship the relationship between the given element and the locations to be returned
   * @return the locations of the elements that have the given relationship with the given element
   */
  public Location[] getRelationships(Element element, Relationship relationship) {
    HashMap<Relationship, ArrayList<ContributedLocation>> elementRelationshipMap = relationshipMap.get(element);
    if (elementRelationshipMap != null) {
      ArrayList<ContributedLocation> contributedLocations = elementRelationshipMap.get(relationship);
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

  /**
   * Return the number of resources that are currently recorded in this index.
   * 
   * @return the number of resources that are currently recorded in this index
   */
  public int getResourceCount() {
    return resourceToElementMap.size();
  }

  /**
   * Associate the given value with the given attribute of the given element. Each element can have
   * only a single value associated with a given attribute. In other words, if the following code
   * were executed
   * 
   * <pre>
   *   recordAttribute(element, attribute, "one");
   *   recordAttribute(element, attribute, "two");
   * </pre>
   * 
   * then the value of the given attribute for the given element would be <code>"two"</code>.
   * 
   * @param element the element on which the attribute is defined
   * @param attribute the attribute of the element that will be given a value
   * @param value the value of the attribute for the given element
   */
  public void recordAttribute(Element element, Attribute attribute, String value) {
    recordElement(element);
    HashMap<Attribute, String> elementAttributeMap = attributeMap.get(element);
    if (elementAttributeMap == null) {
      elementAttributeMap = new HashMap<Attribute, String>();
      attributeMap.put(element, elementAttributeMap);
    }
    elementAttributeMap.put(attribute, value);
  }

  /**
   * Record that the given element and location have the given relationship. For example, if the
   * relationship is the is-referenced-by relationship, then the element would be the element being
   * referenced and the location would be the point at which it is referenced. Each element can have
   * the same relationship with multiple locations. In other words, if the following code were
   * executed
   * 
   * <pre>
   *   recordRelationship(element, isReferencedBy, location1);
   *   recordRelationship(element, isReferencedBy, location2);
   * </pre>
   * 
   * then both relationships would be maintained in the index and the result of executing
   * 
   * <pre>
   *   getRelationship(element, isReferencedBy);
   * </pre>
   * 
   * would be an array containing both <code>location1</code> and <code>location2</code>.
   * 
   * @param contributor the resource that was being analyzed when this relationship was contributed
   * @param element the element that is related to the location
   * @param relationship the relationship between the element and the location
   * @param location the location that is related to the element
   */
  public void recordRelationship(Resource contributor, Element element, Relationship relationship,
      Location location) {
    if (contributor == null || element == null || location == null) {
      return;
    }
    recordElement(element);
    recordElement(location.getElement());
    // add ContributedLocationfor "element"
    ContributedLocation contributedLocation;
    {
      HashMap<Relationship, ArrayList<ContributedLocation>> elementRelationshipMap = relationshipMap.get(element);
      if (elementRelationshipMap == null) {
        elementRelationshipMap = new HashMap<Relationship, ArrayList<ContributedLocation>>();
        relationshipMap.put(element, elementRelationshipMap);
      }
      ArrayList<ContributedLocation> locations = elementRelationshipMap.get(relationship);
      if (locations == null) {
        locations = new ArrayList<ContributedLocation>();
        elementRelationshipMap.put(relationship, locations);
      }
      contributedLocation = new ContributedLocation(locations, contributor, location);
    }
    // add to "contributor" -> "locations" map
    {
      List<ContributedLocation> locations = contributorToContributedLocations.get(contributor);
      if (locations == null) {
        locations = Lists.newArrayList();
        contributorToContributedLocations.put(contributor, locations);
      }
      locations.add(contributedLocation);
    }
  }

  /**
   * Remove from the index all of the information associated that was contribute as a result of
   * analyzing the given resource. This includes relationships between an element in the given
   * resource and any other locations and relationships between any other elements and a location
   * within the given resource.
   * <p>
   * This method should be invoked when a resource is about to be re-analyzed.
   * 
   * @param resource the resource being re-analyzed
   */
  public void regenerateResource(Resource resource) {
    resourceToElementMap.remove(resource);

    List<ContributedLocation> locations = contributorToContributedLocations.remove(resource);
    if (locations != null) {
      for (ContributedLocation location : locations) {
        location.getOwner().remove(location);
      }
    }
  }

  /**
   * Remove from the index all of the information associated with elements or locations in the given
   * resource. This includes relationships between an element in the given resource and any other
   * locations, relationships between any other elements and a location within the given resource,
   * and any values of any attributes defined on elements in the given resource.
   * <p>
   * This method should be invoked when a resource is no longer part of the code base.
   * 
   * @param resource the resource being removed
   */
  public void removeResource(Resource resource) {
    Set<Element> elements = resourceToElementMap.get(resource);
    if (elements != null) {
      for (Element element : elements) {
        removeElement(element);
      }
      resourceToElementMap.remove(resource);
    }

    Set<Entry<Element, HashMap<Relationship, ArrayList<ContributedLocation>>>> relationshipSet = relationshipMap.entrySet();
    Iterator<Entry<Element, HashMap<Relationship, ArrayList<ContributedLocation>>>> relationshipSetIterator = relationshipSet.iterator();
    while (relationshipSetIterator.hasNext()) {
      Set<Entry<Relationship, ArrayList<ContributedLocation>>> elementRelationshipMap = relationshipSetIterator.next().getValue().entrySet();
      Iterator<Entry<Relationship, ArrayList<ContributedLocation>>> relationshipIterator = elementRelationshipMap.iterator();
      while (relationshipIterator.hasNext()) {
        ArrayList<ContributedLocation> locations = relationshipIterator.next().getValue();
        Iterator<ContributedLocation> locationIterator = locations.iterator();
        while (locationIterator.hasNext()) {
          ContributedLocation location = locationIterator.next();
          if (location.getContributor().equals(resource)
              || location.getLocation().getElement().getResource().equals(resource)) {
            locationIterator.remove();
          }
        }
        if (locations.isEmpty()) {
          relationshipIterator.remove();
        }
      }
      if (elementRelationshipMap.isEmpty()) {
        relationshipSetIterator.remove();
      }
    }
  }

//  private int getRelationshipCount() {
//    int count = 0;
//    for (HashMap<Relationship, ArrayList<Location>> elementMap : relationshipMap.values()) {
//      for (ArrayList<Location> locationList : elementMap.values()) {
//        count += locationList.size();
//      }
//    }
//    return count;
//  }

  @Override
  public String toString() {
    PrintStringWriter writer = new PrintStringWriter();
    writeIndex(writer);
    return writer.toString();
  }

  /**
   * Add the given element to the resource-to-element map.
   * 
   * @param element the element to be added to the map
   */
  private void recordElement(Element element) {
    Resource resource = element.getResource();
    Set<Element> elementList = resourceToElementMap.get(resource);
    if (elementList == null) {
      elementList = new HashSet<Element>();
      resourceToElementMap.put(resource, elementList);
    }
    elementList.add(element);
  }

  /**
   * Remove from the index all of the information associated with the given element. This includes
   * relationships between the element and any locations, relationships between any other elements
   * and a location within the given element, and any values of any attributes defined the element.
   * 
   * @param element the element being removed
   */
  private void removeElement(Element element) {
    attributeMap.remove(element);
    relationshipMap.remove(element);
  }

  /**
   * Write the contents of this index to the given print writer. This is intended to be used for
   * debugging purposes, not as something that would be displayed to users.
   * 
   * @param writer the writer to which the contents of the index will be written
   */
  private void writeIndex(PrintWriter writer) {
    writer.println("Attribute Map");
    writer.println();
    if (attributeMap.isEmpty()) {
      writer.println("  -- empty --");
    } else {
      for (Map.Entry<Element, HashMap<Attribute, String>> resourceEntry : attributeMap.entrySet()) {
        writer.print("  ");
        writer.println(resourceEntry.getKey());
        for (Map.Entry<Attribute, String> attributeEntry : resourceEntry.getValue().entrySet()) {
          writer.print("    ");
          writer.print(attributeEntry.getKey());
          writer.print(" = \"");
          writer.print(attributeEntry.getValue());
          writer.println("\"");
        }
      }
    }

    writer.println();
    writer.println("Relationship Map");
    writer.println();
    if (relationshipMap.isEmpty()) {
      writer.println("  -- empty --");
    } else {
      for (Map.Entry<Element, HashMap<Relationship, ArrayList<ContributedLocation>>> elementEntry : relationshipMap.entrySet()) {
        writer.print("  ");
        writer.println(elementEntry.getKey());
        for (Map.Entry<Relationship, ArrayList<ContributedLocation>> relationshipEntry : elementEntry.getValue().entrySet()) {
          String relationship = relationshipEntry.getKey().toString();
          for (ContributedLocation location : relationshipEntry.getValue()) {
            writer.print("    ");
            writer.print(relationship);
            writer.print(" ");
            writer.print(location.getLocation());
            writer.print(" (contributed by ");
            writer.print(location.getContributor());
            writer.print(")");
          }
        }
      }
    }

    writer.println();
    writer.println("Resource to Element Map");
    writer.println();
    if (resourceToElementMap.isEmpty()) {
      writer.println("  -- empty --");
    } else {
      for (Map.Entry<Resource, Set<Element>> resourceEntry : resourceToElementMap.entrySet()) {
        writer.print("  ");
        writer.println(resourceEntry.getKey());
        for (Element element : resourceEntry.getValue()) {
          writer.print("    ");
          writer.println(element);
        }
      }
    }
  }
}
