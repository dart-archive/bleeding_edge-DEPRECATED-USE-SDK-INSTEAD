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
package com.google.dart.engine.internal.index.structure.btree;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * An implementation of [NodeManager] that keeps node information in memory.
 * 
 * @coverage dart.engine.index.structure
 */
public class MemoryNodeManager<K, V> implements NodeManager<K, V, Integer> {
  final int maxInternalKeys;
  final int maxLeafKeys;
  Map<Integer, InternalNodeData<K, Integer>> _internalDataMap = Maps.newHashMap();
  Map<Integer, LeafNodeData<K, V>> _leafDataMap = Maps.newHashMap();

  int _nextPageInternalId = 0;
  int _nextPageLeafId = 1;

  public MemoryNodeManager(int maxInternalKeys, int maxLeafKeys) {
    this.maxInternalKeys = maxInternalKeys;
    this.maxLeafKeys = maxLeafKeys;
  }

  @Override
  public Integer createInternal() {
    int id = _nextPageInternalId;
    _nextPageInternalId += 2;
    return id;
  }

  @Override
  public Integer createLeaf() {
    int id = _nextPageLeafId;
    _nextPageLeafId += 2;
    return id;
  }

  @Override
  public void delete(Integer id) {
    if (isInternal(id)) {
      _internalDataMap.remove(id);
    } else {
      _leafDataMap.remove(id);
    }
  }

  @Override
  public int getMaxInternalKeys() {
    return maxInternalKeys;
  }

  @Override
  public int getMaxLeafKeys() {
    return maxLeafKeys;
  }

  @Override
  public boolean isInternal(Integer id) {
    return (id % 2) == 0;
  }

  @Override
  public InternalNodeData<K, Integer> readInternal(Integer id) {
    return _internalDataMap.get(id);
  }

  @Override
  public LeafNodeData<K, V> readLeaf(Integer id) {
    return _leafDataMap.get(id);
  }

  @Override
  public void writeInternal(Integer id, InternalNodeData<K, Integer> data) {
    _internalDataMap.put(id, data);
  }

  @Override
  public void writeLeaf(Integer id, LeafNodeData<K, V> data) {
    _leafDataMap.put(id, data);
  }
}
