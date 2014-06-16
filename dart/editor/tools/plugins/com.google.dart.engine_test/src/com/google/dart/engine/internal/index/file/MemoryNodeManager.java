package com.google.dart.engine.internal.index.file;

import com.google.common.collect.Maps;
import com.google.dart.engine.context.AnalysisContext;

import java.util.List;
import java.util.Map;

/**
 * A {@link NodeManager} that keeps {@link IndexNode}s in memory.
 */
public class MemoryNodeManager implements NodeManager {
  public final StringCodec stringCodec = new StringCodec();
  public final ElementCodec elementCodec = new ElementCodec(stringCodec);
  public final RelationshipCodec relationshipCodec = new RelationshipCodec(stringCodec);

  private final Map<String, IndexNode> nodes = Maps.newHashMap();
  private final Map<String, Integer> nodeLocationCounts = Maps.newHashMap();
  private int locationCount = 0;

  @Override
  public void clear() {
    nodes.clear();
  }

  @Override
  public int getLocationCount() {
    return locationCount;
  }

  @Override
  public IndexNode getNode(String name) {
    return nodes.get(name);
  }

  @Override
  public StringCodec getStringCodec() {
    return stringCodec;
  }

  public boolean isEmpty() {
    for (IndexNode node : nodes.values()) {
      Map<RelationKeyData, List<LocationData>> relations = node.getRelations();
      if (!relations.isEmpty()) {
        return false;
      }
    }
    return true;
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
    // remember the node
    nodes.put(name, node);
  }

  @Override
  public void removeNode(String name) {
    nodes.remove(name);
  }

  private int getLocationCount(String name) {
    Integer locationCount = nodeLocationCounts.get(name);
    return locationCount != null ? locationCount : 0;
  }
}
