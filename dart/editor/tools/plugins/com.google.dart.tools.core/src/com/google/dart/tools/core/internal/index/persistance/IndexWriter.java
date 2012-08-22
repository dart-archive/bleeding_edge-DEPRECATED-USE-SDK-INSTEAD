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
package com.google.dart.tools.core.internal.index.persistance;

import com.google.dart.tools.core.index.Attribute;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.ContributedLocation;
import com.google.dart.tools.core.model.DartSdkManager;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class <code>IndexWriter</code> implement an object that can write the contents
 * of an index to a {@link ObjectOutputStream stream}.
 */
public class IndexWriter {
  /**
   * The table mapping elements to the values of attributes associated with those elements.
   */
  private HashMap<Element, HashMap<Attribute, String>> attributeMap;

  /**
   * The table mapping elements to the relationships associated with those elements.
   */
  private HashMap<Element, HashMap<Relationship, ArrayList<ContributedLocation>>> relationshipMap;

  /**
   * An array containing all of the strings to be written out.
   */
  private String[] stringTable = new String[1024];

  /**
   * The index of the next index in the string table that can be assigned to a string.
   */
  private int nextStringIndex = 0;

  /**
   * A table mapping strings to the index that was assigned to the string.
   */
  private HashMap<String, Integer> stringMap = new HashMap<String, Integer>();

  /**
   * The version number of the file format being generated.
   */
  public static int FILE_VERSION_NUMBER = 2;

  /**
   * Initialize a newly created index writer to write the attributes and relationships in the given
   * maps.
   * 
   * @param attributeMap the table mapping elements to the values of attributes associated with
   *          those elements
   * @param relationshipMap the table mapping elements to the relationships associated with those
   *          elements
   */
  public IndexWriter(HashMap<Element, HashMap<Attribute, String>> attributeMap,
      HashMap<Element, HashMap<Relationship, ArrayList<ContributedLocation>>> relationshipMap) {
    this.attributeMap = attributeMap;
    this.relationshipMap = relationshipMap;
  }

  /**
   * Write the contents of the index to the given output stream.
   * 
   * @param output the stream to which the contents of the index are to be written
   * @throws IOException if the index could not be written
   */
  public void writeIndex(ObjectOutputStream output) throws IOException {
    buildStringTable();

    writeFileVersionNumber(output);
    writeSDKVersionNumber(output);
    writeStringTable(output);
    writeAttributeMap(output);
    writeRelationshipMap(output);
  }

  /**
   * Add the strings reachable from the attribute map to the string table.
   */
  private void addAttributeMapToStringTable() {
    for (Map.Entry<Element, HashMap<Attribute, String>> elementEntry : attributeMap.entrySet()) {
      addElementToStringTable(elementEntry.getKey());
      for (Map.Entry<Attribute, String> attributeEntry : elementEntry.getValue().entrySet()) {
        addAttributeToStringTable(attributeEntry.getKey());
        addStringToStringTable(attributeEntry.getValue());
      }
    }
  }

  /**
   * Add the strings reachable from the given attribute to the string table.
   * 
   * @param attribute the attribute whose strings are to be added
   */
  private void addAttributeToStringTable(Attribute attribute) {
    addStringToStringTable(attribute.getIdentifier());
  }

  /**
   * Add the strings reachable from the given contributed location to the string table.
   * 
   * @param location the contributed location whose strings are to be added
   */
  private void addContributedLocationToStringTable(ContributedLocation location) {
    addResourceToStringTable(location.getContributor());
    addLocationToStringTable(location.getLocation());
  }

  /**
   * Add the strings reachable from the given element to the string table.
   * 
   * @param element the element whose strings are to be added
   */
  private void addElementToStringTable(Element element) {
    addResourceToStringTable(element.getResource());
    addStringToStringTable(element.getElementId());
  }

  /**
   * Add the strings reachable from the given location to the string table.
   * 
   * @param location the location whose strings are to be added
   */
  private void addLocationToStringTable(Location location) {
    addElementToStringTable(location.getElement());
    addStringToStringTable(location.getImportPrefix());
  }

  /**
   * Add the strings reachable from the relationship map to the string table.
   */
  private void addRelationshipMapToStringTable() {
    for (Map.Entry<Element, HashMap<Relationship, ArrayList<ContributedLocation>>> elementEntry : relationshipMap.entrySet()) {
      addElementToStringTable(elementEntry.getKey());
      for (Map.Entry<Relationship, ArrayList<ContributedLocation>> relationshipEntry : elementEntry.getValue().entrySet()) {
        addRelationshipToStringTable(relationshipEntry.getKey());
        for (ContributedLocation location : relationshipEntry.getValue()) {
          addContributedLocationToStringTable(location);
        }
      }
    }
  }

  /**
   * Add the strings reachable from the given relationship to the string table.
   * 
   * @param relationship the relationship whose strings are to be added
   */
  private void addRelationshipToStringTable(Relationship relationship) {
    addStringToStringTable(relationship.getIdentifier());
  }

  /**
   * Add the strings reachable from the given resource to the string table.
   * 
   * @param resource the resource whose strings are to be added
   */
  private void addResourceToStringTable(Resource resource) {
    addStringToStringTable(resource.getResourceId());
  }

  /**
   * Add the given string to the string table.
   * 
   * @param string the string to be added
   */
  private void addStringToStringTable(String string) {
    // "null" is handled as -1
    if (string == null) {
      return;
    }
    // generate String index
    Integer index = stringMap.get(string);
    if (index == null) {
      int currentLength = stringTable.length;
      if (nextStringIndex == currentLength) {
        String[] newStringTable = new String[currentLength + 1024];
        System.arraycopy(stringTable, 0, newStringTable, 0, currentLength);
        stringTable = newStringTable;
      }
      stringTable[nextStringIndex] = string;
      index = new Integer(nextStringIndex++);
      stringMap.put(string, index);
    }
  }

  /**
   * Build the string table.
   */
  private void buildStringTable() {
    addAttributeMapToStringTable();
    addRelationshipMapToStringTable();
  }

  /**
   * Write the given attribute to the given output stream.
   * 
   * @param output the stream to which the attribute is to be written
   * @param attribute the attribute to be written
   * @throws IOException if the attribute could not be written
   */
  private void writeAttribute(ObjectOutputStream output, Attribute attribute) throws IOException {
    writeString(output, attribute.getIdentifier());
  }

  /**
   * Write the attribute map to the given output stream.
   * 
   * @param output the stream to which the attribute map is to be written
   * @throws IOException if the attribute map could not be written
   */
  private void writeAttributeMap(ObjectOutputStream output) throws IOException {
    output.writeInt(attributeMap.size());
    for (Map.Entry<Element, HashMap<Attribute, String>> elementEntry : attributeMap.entrySet()) {
      writeElement(output, elementEntry.getKey());
      output.writeInt(elementEntry.getValue().size());
      for (Map.Entry<Attribute, String> attributeEntry : elementEntry.getValue().entrySet()) {
        writeAttribute(output, attributeEntry.getKey());
        writeString(output, attributeEntry.getValue());
      }
    }
  }

  /**
   * Write the given contributed location to the given output stream.
   * 
   * @param output the stream to which the contributed location is to be written
   * @param location the contributed location to be written
   * @throws IOException if the contributed location could not be written
   */
  private void writeContributedLocation(ObjectOutputStream output, ContributedLocation location)
      throws IOException {
    writeResource(output, location.getContributor());
    writeLocation(output, location.getLocation());
  }

  /**
   * Write the given element to the given output stream.
   * 
   * @param output the stream to which the element is to be written
   * @param element the element to be written
   * @throws IOException if the element could not be written
   */
  private void writeElement(ObjectOutputStream output, Element element) throws IOException {
    writeResource(output, element.getResource());
    writeString(output, element.getElementId());
  }

  /**
   * Write the version number of the file format being generated.
   * 
   * @param output the stream to which the contents of the index are to be written
   * @throws IOException if the index could not be written
   */
  private void writeFileVersionNumber(ObjectOutputStream output) throws IOException {
    output.writeInt(FILE_VERSION_NUMBER);
  }

  /**
   * Write the given location to the given output stream.
   * 
   * @param output the stream to which the location is to be written
   * @param location the location to be written
   * @throws IOException if the location could not be written
   */
  private void writeLocation(ObjectOutputStream output, Location location) throws IOException {
    writeElement(output, location.getElement());
    output.writeInt(location.getOffset());
    output.writeInt(location.getLength());
    writeString(output, location.getImportPrefix());
  }

  /**
   * Write the given relationship to the given output stream.
   * 
   * @param output the stream to which the relationship is to be written
   * @param relationship the relationship to be written
   * @throws IOException if the relationship could not be written
   */
  private void writeRelationship(ObjectOutputStream output, Relationship relationship)
      throws IOException {
    writeString(output, relationship.getIdentifier());
  }

  /**
   * Write the relationship map to the given output stream.
   * 
   * @param output the stream to which the relationship map is to be written
   * @throws IOException if the relationship map could not be written
   */
  private void writeRelationshipMap(ObjectOutputStream output) throws IOException {
    output.writeInt(relationshipMap.size());
    for (Map.Entry<Element, HashMap<Relationship, ArrayList<ContributedLocation>>> elementEntry : relationshipMap.entrySet()) {
      writeElement(output, elementEntry.getKey());
      output.writeInt(elementEntry.getValue().size());
      for (Map.Entry<Relationship, ArrayList<ContributedLocation>> relationshipEntry : elementEntry.getValue().entrySet()) {
        writeRelationship(output, relationshipEntry.getKey());
        output.writeInt(relationshipEntry.getValue().size());
        for (ContributedLocation location : relationshipEntry.getValue()) {
          writeContributedLocation(output, location);
        }
      }
    }
  }

  /**
   * Write the given resource to the given output stream.
   * 
   * @param output the stream to which the resource is to be written
   * @param resource the resource to be written
   * @throws IOException if the resource could not be written
   */
  private void writeResource(ObjectOutputStream output, Resource resource) throws IOException {
    writeString(output, resource.getResourceId());
  }

  /**
   * Write the version number of the SDK that was indexed.
   * 
   * @param output the stream to which the SDK version is to be written
   * @throws IOException if the SDK version could not be written
   */
  private void writeSDKVersionNumber(ObjectOutputStream output) throws IOException {
    output.writeUTF(DartSdkManager.getManager().getSdk().getSdkVersion());
  }

  /**
   * Write the given string to the given output stream.
   * 
   * @param output the stream to which the string is to be written
   * @param string the string to be written
   * @throws IOException if the string could not be written
   */
  private void writeString(ObjectOutputStream output, String string) throws IOException {
    if (string == null) {
      output.writeInt(-1);
    } else {
      Integer index = stringMap.get(string);
      if (index == null) {
        throw new IllegalStateException(
            "Attempting to write a string that was not in the string table: \"" + string + "\"");
      }
      output.writeInt(index.intValue());
    }
  }

  /**
   * Write the string table to the given output stream.
   * 
   * @param output the stream to which the string table is to be written
   * @throws IOException if the string table could not be written
   */
  private void writeStringTable(ObjectOutputStream output) throws IOException {
    int length = nextStringIndex;
    output.writeInt(length);
    for (int i = 0; i < length; i++) {
      output.writeUTF(stringTable[i]);
    }
  }
}
