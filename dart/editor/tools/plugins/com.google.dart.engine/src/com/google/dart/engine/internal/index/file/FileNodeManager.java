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

import com.google.common.collect.Maps;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.utilities.logging.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A {@link FileManager} based {@link NodeManager}.
 * 
 * @coverage dart.engine.index
 */
public class FileNodeManager implements NodeManager {
  private static int VERSION = 1;

  private final FileManager fileManager;
  private final Logger logger;
  private final StringCodec stringCodec;
  private final ContextCodec contextCodec;
  private final ElementCodec elementCodec;
  private final RelationshipCodec relationshipCodec;
  private final Map<String, Integer> nodeLocationCounts = Maps.newHashMap();
  private int locationCount = 0;

  public FileNodeManager(FileManager fileManager, Logger logger, StringCodec stringCodec,
      ContextCodec contextCodec, ElementCodec elementCodec, RelationshipCodec relationshipCodec) {
    this.fileManager = fileManager;
    this.logger = logger;
    this.stringCodec = stringCodec;
    this.contextCodec = contextCodec;
    this.elementCodec = elementCodec;
    this.relationshipCodec = relationshipCodec;
  }

  @Override
  public void clear() {
    fileManager.clear();
  }

  @Override
  public ContextCodec getContextCodec() {
    return contextCodec;
  }

  @Override
  public ElementCodec getElementCodec() {
    return elementCodec;
  }

  @Override
  public int getLocationCount() {
    return locationCount;
  }

  @Override
  public IndexNode getNode(String name) {
    try {
      InputStream inputStream = fileManager.openInputStream(name);
      if (inputStream != null) {
        try {
          DataInputStream stream = new DataInputStream(inputStream);
          // check version
          {
            int version = stream.readInt();
            if (version != VERSION) {
              throw new IllegalStateException("Version " + VERSION + " expected, but " + version
                  + " found.");
            }
          }
          // context
          int contextId = stream.readInt();
          AnalysisContext context = contextCodec.decode(contextId);
          if (context == null) {
            return null;
          }
          // relations
          Map<RelationKeyData, List<LocationData>> relations = Maps.newHashMap();
          int numRelations = stream.readInt();
          for (int i = 0; i < numRelations; i++) {
            RelationKeyData key = readElementRelationKey(stream);
            int numLocations = stream.readInt();
            List<LocationData> locations = new ArrayList<LocationData>(numLocations);
            for (int j = 0; j < numLocations; j++) {
              locations.add(readLocationData(stream));
            }
            relations.put(key, locations);
          }
          // create IndexNode
          IndexNode node = new IndexNode(context, elementCodec, relationshipCodec);
          node.setRelations(relations);
          return node;
        } finally {
          inputStream.close();
        }
      }
    } catch (Throwable e) {
      logger.logError("Exception during reading index file " + name, e);
    }
    return null;
  }

  @Override
  public StringCodec getStringCodec() {
    return stringCodec;
  }

  @Override
  public IndexNode newNode(AnalysisContext context) {
    return new IndexNode(context, elementCodec, relationshipCodec);
  }

  @Override
  public void putNode(String name, IndexNode node) {
    // update location count
    {
      locationCount -= getLocationCount(name);
      int nodeLocationCount = node.getLocationCount();
      nodeLocationCounts.put(name, nodeLocationCount);
      locationCount += nodeLocationCount;
    }
    // write the node
    try {
      OutputStream stream = fileManager.openOutputStream(name);
      try {
        writeNode(node, stream);
      } finally {
        stream.close();
      }
    } catch (Throwable e) {
      logger.logError("Exception during writing index file " + name, e);
    }
  }

  @Override
  public void removeNode(String name) {
    // update location count
    locationCount -= getLocationCount(name);
    nodeLocationCounts.remove(name);
    // remove node
    fileManager.delete(name);
  }

  private int getLocationCount(String name) {
    Integer locationCount = nodeLocationCounts.get(name);
    return locationCount != null ? locationCount : 0;
  }

  private RelationKeyData readElementRelationKey(DataInputStream stream) throws Exception {
    int elementId = stream.readInt();
    int relationshipId = stream.readInt();
    return new RelationKeyData(elementId, relationshipId);
  }

  private LocationData readLocationData(DataInputStream stream) throws Exception {
    int elementId = stream.readInt();
    int offset = stream.readInt();
    int length = stream.readInt();
    return new LocationData(elementId, offset, length);
  }

  private void writeElementRelationKey(DataOutputStream stream, RelationKeyData key)
      throws Exception {
    stream.writeInt(key.elementId);
    stream.writeInt(key.relationshipId);
  }

  private void writeNode(IndexNode node, OutputStream outputStream) throws Exception {
    DataOutputStream stream = new DataOutputStream(outputStream);
    // version
    stream.writeInt(VERSION);
    // context
    {
      AnalysisContext context = node.getContext();
      int contextId = contextCodec.encode(context);
      stream.writeInt(contextId);
    }
    // relations
    Map<RelationKeyData, List<LocationData>> relations = node.getRelations();
    stream.writeInt(relations.size());
    for (Entry<RelationKeyData, List<LocationData>> entry : relations.entrySet()) {
      RelationKeyData key = entry.getKey();
      List<LocationData> locations = entry.getValue();
      writeElementRelationKey(stream, key);
      stream.writeInt(locations.size());
      for (LocationData location : locations) {
        stream.writeInt(location.elementId);
        stream.writeInt(location.offset);
        stream.writeInt(location.length);
      }
    }
  }
}
