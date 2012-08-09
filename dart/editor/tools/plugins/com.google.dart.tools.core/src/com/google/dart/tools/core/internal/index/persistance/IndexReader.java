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
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.model.DartSdk;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.HashMap;

/**
 * Instances of the class <code>IndexReader</code> implement an object that can read the contents of
 * an index from a {@link Reader reader}.
 */
public class IndexReader {
  /**
   * The index to which data will be added.
   */
  private IndexStore index;

  /**
   * An array containing all of the strings that were read in.
   */
  private String[] stringTable;

  /**
   * A table mapping resource identifiers to the resources representing them. This is an
   * optimization used to reduce the number of objects created.
   */
  private HashMap<String, Resource> resourceMap = new HashMap<String, Resource>();

  /**
   * A table mapping element identifiers to the elements representing them. This is an optimization
   * used to reduce the number of objects created.
   */
  private HashMap<String, Element> elementMap = new HashMap<String, Element>();

  /**
   * Initialize a newly created index reader to add the attributes and relationships that are read
   * into the given index.
   * 
   * @param index the index to which data will be added
   */
  public IndexReader(IndexStore index) {
    this.index = index;
  }

  /**
   * Read the contents of the index from the given input stream.
   * 
   * @param input the stream from which the contents of the index are to be read
   * @return <code>true</code> if the input stream is valid and could be read
   * @throws IOException if the index could not be read
   */
  public boolean readIndex(ObjectInputStream input) throws IOException {
    int version = input.readInt(); // File version number.
    if (version == 1) {
      // Version 1 was considered unreadable if the editor's build number had changed. Given that
      // a change to the version number necessitates a new build, a file with this version can never
      // be readable once a new version was introduced.
      return false;
    } else if (version == 2) {
      return readIndexVersion2(input);
    } else {
      throw new IOException("Invalid version number in index file: " + version);
    }
  }

  /**
   * Read the contents of the index from the given input stream given that the version of the file
   * format has been determined to be <code>2</code>.
   * 
   * @param input the stream from which the contents of the index are to be read
   * @return <code>true</code> if the input stream is valid and could be read
   * @throws IOException if the index could not be read
   */
  public boolean readIndexVersion2(ObjectInputStream input) throws IOException {
    String sdkVersion = input.readUTF();
    if (!sdkVersion.equals(DartSdk.getInstance().getSdkVersion())) {
      return false;
    }
    readStringTable(input);
    readAttributeMap(input);
    readRelationshipMap(input);
    return true;
  }

  /**
   * Read an attribute from the given input stream.
   * 
   * @param input the stream from which the attribute is to be read
   * @return the attribute that was read
   * @throws IOException if the attribute could not be read
   */
  private Attribute readAttribute(ObjectInputStream input) throws IOException {
    return Attribute.getAttribute(readString(input));
  }

  /**
   * Read the attribute map from the given input stream.
   * 
   * @param input the stream from which the attribute map is to be read
   * @throws IOException if the attribute map could not be read
   */
  private void readAttributeMap(ObjectInputStream input) throws IOException {
    int elementCount = input.readInt();
    for (int i = 0; i < elementCount; i++) {
      Element element = readElement(input);
      int attributeCount = input.readInt();
      for (int j = 0; j < attributeCount; j++) {
        index.recordAttribute(element, readAttribute(input), readString(input));
      }
    }
  }

  /**
   * Read an element from the given input stream.
   * 
   * @param input the stream from which the element is to be read
   * @return the element that was read
   * @throws IOException if the element could not be read
   */
  private Element readElement(ObjectInputStream input) throws IOException {
    Resource resource = readResource(input);
    String elementId = readString(input);
    String elementKey = resource.getResourceId() + "#" + elementId;
    Element element = elementMap.get(elementKey);
    if (element == null) {
      element = new Element(resource, elementId);
      elementMap.put(elementKey, element);
    }
    return element;
  }

  /**
   * Read a location from the given input stream.
   * 
   * @param input the stream from which the location is to be read
   * @return the location that was read
   * @throws IOException if the location could not be read
   */
  private Location readLocation(ObjectInputStream input) throws IOException {
    return new Location(readElement(input), input.readInt(), input.readInt(), readString(input));
  }

  /**
   * Read a relationship from the given input stream.
   * 
   * @param input the stream from which the relationship is to be read
   * @return the relationship that was read
   * @throws IOException if the relationship could not be read
   */
  private Relationship readRelationship(ObjectInputStream input) throws IOException {
    return Relationship.getRelationship(readString(input));
  }

  /**
   * Read the relationship map from the given input stream.
   * 
   * @param input the stream from which the relationship map is to be read
   * @throws IOException if the relationship map could not be read
   */
  private void readRelationshipMap(ObjectInputStream input) throws IOException {
    int elementCount = input.readInt();
    for (int i = 0; i < elementCount; i++) {
      Element element = readElement(input);
      int relationshipCount = input.readInt();
      for (int j = 0; j < relationshipCount; j++) {
        Relationship relationship = readRelationship(input);
        int locationCount = input.readInt();
        for (int k = 0; k < locationCount; k++) {
          Resource readResource = readResource(input);
          Location readLocation = readLocation(input);
          index.recordRelationship(readResource, element, relationship, readLocation);
        }
      }
    }
  }

  /**
   * Read a resource from the given input stream.
   * 
   * @param input the stream from which the resource is to be read
   * @return the resource that was read
   * @throws IOException if the resource could not be read
   */
  private Resource readResource(ObjectInputStream input) throws IOException {
    String resourceId = readString(input);
    Resource resource = resourceMap.get(resourceId);
    if (resource == null) {
      resource = new Resource(resourceId);
      resourceMap.put(resourceId, resource);
    }
    return resource;
  }

  /**
   * Read a string from the given input stream.
   * 
   * @param input the stream from which the string is to be read
   * @return the string that was read
   * @throws IOException if the string could not be read
   */
  private String readString(ObjectInputStream input) throws IOException {
    int index = input.readInt();
    if (index == -1) {
      return null;
    }
    return stringTable[index];
  }

  /**
   * Read the string table from the given input stream.
   * 
   * @param input the stream from which the string table is to be read
   * @throws IOException if the string table could not be read
   */
  private void readStringTable(ObjectInputStream input) throws IOException {
    int length = input.readInt();
    stringTable = new String[length];
    for (int i = 0; i < length; i++) {
      stringTable[i] = input.readUTF();
    }
  }
}
