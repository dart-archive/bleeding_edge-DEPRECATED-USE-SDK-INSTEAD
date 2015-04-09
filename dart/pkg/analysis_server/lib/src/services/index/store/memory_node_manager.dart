// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library services.src.index.store.store_memory_node_manager;

import 'dart:async';
import 'dart:collection';

import 'package:analysis_server/src/services/index/store/codec.dart';
import 'package:analysis_server/src/services/index/store/split_store.dart';
import 'package:analyzer/src/generated/engine.dart';

class MemoryNodeManager implements NodeManager {
  @override
  StringCodec stringCodec = new StringCodec();

  @override
  ContextCodec contextCodec = new ContextCodec();

  @override
  ElementCodec elementCodec;

  RelationshipCodec _relationshipCodec;

  int _locationCount = 0;
  final Map<String, int> _nodeLocationCounts = new HashMap<String, int>();
  final Map<String, IndexNode> _nodes = new HashMap<String, IndexNode>();

  MemoryNodeManager() {
    elementCodec = new ElementCodec(stringCodec);
    _relationshipCodec = new RelationshipCodec(stringCodec);
  }

  @override
  int get locationCount {
    return _locationCount;
  }

  @override
  void clear() {
    _nodes.clear();
  }

  int getLocationCount(String name) {
    int locationCount = _nodeLocationCounts[name];
    return locationCount != null ? locationCount : 0;
  }

  @override
  Future<IndexNode> getNode(String name) {
    return new Future.value(_nodes[name]);
  }

  bool isEmpty() {
    for (IndexNode node in _nodes.values) {
      Map<RelationKeyData, List<LocationData>> relations = node.relations;
      if (!relations.isEmpty) {
        return false;
      }
    }
    return true;
  }

  @override
  IndexNode newNode(AnalysisContext context) {
    return new IndexNode(context, elementCodec, _relationshipCodec);
  }

  @override
  void putNode(String name, IndexNode node) {
    // update location count
    {
      _locationCount -= getLocationCount(name);
      int nodeLocationCount = node.locationCount;
      _nodeLocationCounts[name] = nodeLocationCount;
      _locationCount += nodeLocationCount;
    }
    // remember the node
    _nodes[name] = node;
  }

  @override
  void removeNode(String name) {
    _nodes.remove(name);
  }
}
