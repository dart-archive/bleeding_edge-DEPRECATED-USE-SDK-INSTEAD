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

/**
 * A manager that manages nodes.
 * 
 * @coverage dart.engine.index.structure
 */
public interface NodeManager<K, V, N> {
  /**
   * Generates an identifier for a new internal node.
   */
  N createInternal();

  /**
   * Generates an identifier for a new leaf node.
   */
  N createLeaf();

  /**
   * Deletes the node with the given identifier.
   */
  void delete(N id);

  /**
   * The maximum number of keys in an internal node.
   */
  int getMaxInternalKeys();

  /**
   * The maximum number of keys in a leaf node.
   */
  int getMaxLeafKeys();

  /**
   * Checks if the node with the given identifier is an internal or a leaf node.
   */
  boolean isInternal(N id);

  /**
   * Reads information about the internal node with the given identifier.
   */
  InternalNodeData<K, N> readInternal(N id);

  /**
   * Reads information about the leaf node with the given identifier.
   */
  LeafNodeData<K, V> readLeaf(N id);

  /**
   * Writes information about the internal node with the given identifier.
   */
  void writeInternal(N id, InternalNodeData<K, N> data);

  /**
   * Writes information about the leaf node with the given identifier.
   */
  void writeLeaf(N id, LeafNodeData<K, V> data);
}
