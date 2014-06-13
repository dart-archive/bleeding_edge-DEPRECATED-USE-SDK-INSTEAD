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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A simple B+ tree (http://en.wikipedia.org/wiki/B+_tree) implementation.
 * 
 * <pre>
 * [K] is the keys type.
 * [V] is the values type.
 * [N] is the type of node identifiers using by the [NodeManager].
 * </pre>
 * 
 * @coverage dart.engine.index.structure
 */
public class BPlusTree<K, V, N> {
  /**
   * An internal node with keys and children references.
   */
  private class InternalNode extends Node {
    List<N> children = new ArrayList<N>();
    final int maxKeys;
    final int minKeys;

    InternalNode(N id, int maxKeys) {
      super(id);
      this.maxKeys = maxKeys;
      minKeys = maxKeys / 2;
    }

    @Override
    V find(K key) {
      int index = findChildIndex(key);
      Node child = readNode(children.get(index));
      return child.find(key);
    }

    @Override
    Split insert(K key, V value) {
      // Early split.
      if (keys.size() == maxKeys) {
        int middle = (maxKeys + 1) / 2;
        K splitKey = keys.get(middle);
        // Overflow into a new sibling.
        InternalNode sibling = newInternalNode();
        sibling.keys.addAll(keys.subList(middle + 1, keys.size()));
        sibling.children.addAll(children.subList(middle + 1, children.size()));
        setListLength(keys, middle);
        setListLength(children, middle + 1);
        // Insert into this node or sibling.
        if (comparator.compare(key, splitKey) < 0) {
          insertNotFull(key, value);
        } else {
          sibling.insertNotFull(key, value);
        }
        // Prepare split.
        writeInternalNode(this);
        writeInternalNode(sibling);
        return new Split(splitKey, id, sibling.id);
      }
      // No split.
      insertNotFull(key, value);
      return null;
    }

    @Override
    Remove remove(K key, Node left, K anchor, Node right) {
      int index = findChildIndex(key);
      K thisAnchor = index == 0 ? keys.get(0) : keys.get(index - 1);
      // Prepare children.
      Node child0 = readNode(children.get(index));
      Node leftChild;
      Node rightChild;
      if (index != 0) {
        leftChild = readNode(children.get(index - 1));
      } else {
        leftChild = null;
      }
      if (index < children.size() - 1) {
        rightChild = readNode(children.get(index + 1));
      } else {
        rightChild = null;
      }
      // Ask child to remove.
      Remove result = child0.remove(key, leftChild, thisAnchor, rightChild);
      V value = result.value;
      if (value == null) {
        return new Remove(value);
      }
      // Do keys / children updates
      boolean hasUpdates = false;
      {
        // Update anchor if borrowed.
        if (result.leftAnchor != null) {
          keys.set(index - 1, result.leftAnchor);
          hasUpdates = true;
        }
        if (result.rightAnchor != null) {
          keys.set(index, result.rightAnchor);
          hasUpdates = true;
        }
        // Update keys / children if merged.
        if (result.mergedLeft) {
          keys.remove(index - 1);
          N child = children.remove(index);
          manager.delete(child);
          hasUpdates = true;
        }
        if (result.mergedRight) {
          keys.remove(index);
          N child = children.remove(index);
          manager.delete(child);
          hasUpdates = true;
        }
      }
      // Write if updated.
      if (!hasUpdates) {
        return new Remove(value);
      }
      writeInternalNode(this);
      // Perform balancing.
      if (keys.size() < minKeys) {
        // Try left sibling.
        if (left instanceof BPlusTree.InternalNode) {
          @SuppressWarnings("unchecked")
          InternalNode leftInternal = (InternalNode) left;
          // Try to redistribute.
          int leftLength = left.keys.size();
          if (leftLength > minKeys) {
            int halfExcess = (leftLength - minKeys + 1) / 2;
            int newLeftLength = leftLength - halfExcess;
            keys.add(0, anchor);
            keys.addAll(0, left.keys.subList(newLeftLength, leftLength));
            children.addAll(0, leftInternal.children.subList(newLeftLength, leftLength + 1));
            K newAnchor = left.keys.get(newLeftLength - 1);
            setListLength(left.keys, newLeftLength - 1);
            setListLength(leftInternal.children, newLeftLength);
            writeInternalNode(this);
            writeInternalNode(leftInternal);
            return new Remove(value).borrowLeft(newAnchor);
          }
          // Do merge.
          left.keys.add(anchor);
          left.keys.addAll(keys);
          leftInternal.children.addAll(children);
          writeInternalNode(this);
          writeInternalNode(leftInternal);
          return new Remove(value).mergeLeft();
        }
        // Try right sibling.
        if (right instanceof BPlusTree.InternalNode) {
          @SuppressWarnings("unchecked")
          InternalNode rightInternal = (InternalNode) right;
          // Try to redistribute.
          int rightLength = right.keys.size();
          if (rightLength > minKeys) {
            int halfExcess = (rightLength - minKeys + 1) / 2;
            keys.add(anchor);
            keys.addAll(right.keys.subList(0, halfExcess - 1));
            children.addAll(rightInternal.children.subList(0, halfExcess));
            K newAnchor = right.keys.get(halfExcess - 1);
            removeListRange(right.keys, 0, halfExcess);
            removeListRange(rightInternal.children, 0, halfExcess);
            writeInternalNode(this);
            writeInternalNode(rightInternal);
            return new Remove(value).borrowRight(newAnchor);
          }
          // Do merge.
          right.keys.add(0, anchor);
          right.keys.addAll(0, keys);
          rightInternal.children.addAll(0, children);
          writeInternalNode(this);
          writeInternalNode(rightInternal);
          return new Remove(value).mergeRight();
        }
      }
      // No balancing required.
      return new Remove(value);
    }

    @Override
    void writeOn(StringBuilder buffer, String indent) {
      buffer.append(indent);
      buffer.append("INode {\n");
      for (int i = 0; i < keys.size(); i++) {
        Node child = readNode(children.get(i));
        child.writeOn(buffer, indent + "    ");
        buffer.append(indent);
        buffer.append("  ");
        buffer.append(keys.get(i));
        buffer.append("\n");
      }
      Node child = readNode(children.get(keys.size()));
      child.writeOn(buffer, indent + "    ");
      buffer.append(indent);
      buffer.append("}\n");
    }

    /**
     * Returns the index of the child into which [key] should be inserted.
     */
    private int findChildIndex(K key) {
      int lo = 0;
      int hi = keys.size() - 1;
      while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        int compare = comparator.compare(key, keys.get(mid));
        if (compare < 0) {
          hi = mid - 1;
        } else if (compare > 0) {
          lo = mid + 1;
        } else {
          return mid + 1;
        }
      }
      return lo;
    }

    private void insertNotFull(K key, V value) {
      int index = findChildIndex(key);
      Node child = readNode(children.get(index));
      Split result = child.insert(key, value);
      if (result != null) {
        keys.add(index, result.key);
        children.set(index, result.left);
        children.add(index + 1, result.right);
        writeInternalNode(this);
      }
    }
  }

  /**
   * A leaf node with keys and values.
   */
  private class LeafNode extends Node {
    final int maxKeys;
    final int minKeys;
    List<V> values = new ArrayList<V>();

    LeafNode(N id, int maxKeys) {
      super(id);
      this.maxKeys = maxKeys;
      this.minKeys = maxKeys / 2;
    }

    @Override
    V find(K key) {
      int index = findKeyIndex(key);
      if (index >= keys.size()) {
        return null;
      }
      if (comparator.compare(keys.get(index), key) != 0) {
        return null;
      }
      return values.get(index);
    }

    @Override
    Split insert(K key, V value) {
      int index = findKeyIndex(key);
      // The node is full.
      if (keys.size() == maxKeys) {
        int middle = (maxKeys + 1) / 2;
        LeafNode sibling = newLeafNode();
        sibling.keys.addAll(keys.subList(middle, keys.size()));
        sibling.values.addAll(values.subList(middle, values.size()));
        setListLength(keys, middle);
        setListLength(values, middle);
        // Insert into the left / right sibling.
        if (index < middle) {
          insertNotFull(key, value, index);
        } else {
          sibling.insertNotFull(key, value, index - middle);
        }
        // Notify the parent about the split.
        writeLeafNode(this);
        writeLeafNode(sibling);
        return new Split(sibling.keys.get(0), id, sibling.id);
      }
      // The node was not full.
      insertNotFull(key, value, index);
      return null;
    }

    @Override
    Remove remove(K key, Node left, K anchor, Node right) {
      // Find the key.
      int index = keys.indexOf(key);
      if (index == -1) {
        return new Remove(null);
      }
      // Remove key / value.
      keys.remove(index);
      V value = values.remove(index);
      writeLeafNode(this);
      // Perform balancing.
      if (keys.size() < minKeys) {
        // Try left sibling.
        if (left instanceof BPlusTree.LeafNode) {
          @SuppressWarnings("unchecked")
          LeafNode leftLeaf = (LeafNode) left;
          // Try to redistribute.
          int leftLength = left.keys.size();
          if (leftLength > minKeys) {
            int halfExcess = (leftLength - minKeys + 1) / 2;
            int newLeftLength = leftLength - halfExcess;
            keys.addAll(0, left.keys.subList(newLeftLength, leftLength));
            values.addAll(0, leftLeaf.values.subList(newLeftLength, leftLength));
            setListLength(left.keys, newLeftLength);
            setListLength(leftLeaf.values, newLeftLength);
            writeLeafNode(this);
            writeLeafNode(leftLeaf);
            return new Remove(value).borrowLeft(keys.get(0));
          }
          // Do merge.
          left.keys.addAll(keys);
          leftLeaf.values.addAll(values);
          writeLeafNode(this);
          writeLeafNode(leftLeaf);
          return new Remove(value).mergeLeft();
        }
        // Try right sibling.
        if (right instanceof BPlusTree.LeafNode) {
          @SuppressWarnings("unchecked")
          LeafNode rightLeaf = (LeafNode) right;
          // Try to redistribute.
          int rightLength = right.keys.size();
          if (rightLength > minKeys) {
            int halfExcess = (rightLength - minKeys + 1) / 2;
            keys.addAll(right.keys.subList(0, halfExcess));
            values.addAll(rightLeaf.values.subList(0, halfExcess));
            removeListRange(right.keys, 0, halfExcess);
            removeListRange(rightLeaf.values, 0, halfExcess);
            writeLeafNode(this);
            writeLeafNode(rightLeaf);
            return new Remove(value).borrowRight(right.keys.get(0));
          }
          // Do merge.
          right.keys.addAll(0, keys);
          rightLeaf.values.addAll(0, values);
          writeLeafNode(this);
          writeLeafNode(rightLeaf);
          return new Remove(value).mergeRight();
        }
      }
      // No balancing required.
      return new Remove(value);
    }

    @Override
    void writeOn(StringBuilder buffer, String indent) {
      buffer.append(indent);
      buffer.append("LNode {");
      for (int i = 0; i < keys.size(); i++) {
        if (i != 0) {
          buffer.append(", ");
        }
        buffer.append(keys.get(i));
        buffer.append(": ");
        buffer.append(values.get(i));
      }
      buffer.append("}\n");
    }

    /**
     * Returns the index where [key] should be inserted.
     */
    private int findKeyIndex(K key) {
      int lo = 0;
      int hi = keys.size() - 1;
      while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        int compare = comparator.compare(key, keys.get(mid));
        if (compare < 0) {
          hi = mid - 1;
        } else if (compare > 0) {
          lo = mid + 1;
        } else {
          return mid;
        }
      }
      return lo;
    }

    private void insertNotFull(K key, V value, int index) {
      if (index < keys.size() && comparator.compare(keys.get(index), key) == 0) {
        values.set(index, value);
      } else {
        keys.add(index, key);
        values.add(index, value);
      }
      writeLeafNode(this);
    }
  }

  /**
   * An internal or leaf node.
   */
  private abstract class Node {
    /**
     * The identifier of this node.
     */
    final N id;

    /**
     * The list of keys.
     */
    List<K> keys = new ArrayList<K>();

    Node(N id) {
      this.id = id;
    }

    /**
     * Looks for [key]. Returns the associated value if found. Returns `null` if not found.
     */
    abstract V find(K key);

    /**
     * Inserts the [key] / [value] pair into this [Node]. Returns a [Split] object if split happens,
     * or `null` otherwise.
     */
    abstract Split insert(K key, V value);

    /**
     * Removes the association for the given [key]. Returns the [Remove] information about an
     * operation performed. It may be restructuring or merging, with [left] or [left] siblings.
     */
    abstract Remove remove(K key, Node left, K anchor, Node right);

    /**
     * Writes a textual presentation of the tree into [buffer].
     */
    abstract void writeOn(StringBuilder buffer, String indent);
  }

  /**
   * A container with information about redistribute / merge.
   */
  private class Remove {
    K leftAnchor;
    boolean mergedLeft = false;
    boolean mergedRight = false;
    K rightAnchor;
    final V value;

    Remove(V value) {
      this.value = value;
    }

    Remove borrowLeft(K leftAnchor) {
      this.leftAnchor = leftAnchor;
      return this;
    }

    Remove borrowRight(K rightAnchor) {
      this.rightAnchor = rightAnchor;
      return this;
    }

    Remove mergeLeft() {
      this.mergedLeft = true;
      return this;
    }

    Remove mergeRight() {
      this.mergedRight = true;
      return this;
    }
  }

  /**
   * A container with information about split during insert.
   */
  private class Split {
    final K key;
    final N left;
    final N right;

    Split(K key, N left, N right) {
      this.key = key;
      this.left = left;
      this.right = right;
    }
  }

  /**
   * The [Comparator] to compare keys.
   */
  final Comparator<K> comparator;

  /**
   * The [NodeManager] to manage nodes.
   */
  final NodeManager<K, V, N> manager;

  /**
   * The maximum number of keys in an index node.
   */
  final int maxInternalKeys;

  /**
   * The maximum number of keys in a leaf node.
   */
  final int maxLeafKeys;

  /**
   * The root node.
   */
  Node root;

  /**
   * Creates a new [BPlusTree] instance.
   */
  @SuppressWarnings("unchecked")
  public BPlusTree(Comparator<K> _comparator, NodeManager<K, V, N> manager) {
    this.comparator = _comparator;
    this.manager = manager;
    this.maxInternalKeys = manager.getMaxInternalKeys();
    this.maxLeafKeys = manager.getMaxLeafKeys();
    root = newLeafNode();
    writeLeafNode((LeafNode) root);
  }

  /**
   * Returns the value for [key] or `null` if [key] is not in the
   */
  public V find(K key) {
    return root.find(key);
  }

  /**
   * Associates the [key] with the given [value]. If the key was already in the tree, its associated
   * value is changed. Otherwise the key-value pair is added to the
   */
  public void insert(K key, V value) {
    Split result = root.insert(key, value);
    if (result != null) {
      InternalNode newRoot = newInternalNode();
      newRoot.keys.add(result.key);
      newRoot.children.add(result.left);
      newRoot.children.add(result.right);
      root = newRoot;
      writeInternalNode(newRoot);
    }
  }

  /**
   * Removes the association for the given [key]. Returns the value associated with [key] in the
   * tree or `null` if [key] is not in the tree.
   */
  @SuppressWarnings("unchecked")
  public V remove(K key) {
    Remove result = root.remove(key, null, null, null);
    if (root instanceof BPlusTree.InternalNode) {
      List<N> children = ((InternalNode) root).children;
      if (children.size() == 1) {
        manager.delete(root.id);
        root = readNode(children.get(0));
      }
    }
    return result.value;
  }

  /**
   * Writes a textual presentation of the tree into [buffer].
   */
  public void writeOn(StringBuilder buffer) {
    root.writeOn(buffer, "");
  }

  /**
   * Creates a new [InternalNode] instance.
   */
  private InternalNode newInternalNode() {
    N id = manager.createInternal();
    return new InternalNode(id, maxInternalKeys);
  }

  /**
   * Creates a new [LeafNode] instance.
   */
  private LeafNode newLeafNode() {
    N id = manager.createLeaf();
    return new LeafNode(id, maxLeafKeys);
  }

  /**
   * Reads the [InternalNode] with [id] from the manager.
   */
  private InternalNode readInternalNode(N id) {
    InternalNodeData<K, N> data = manager.readInternal(id);
    InternalNode node = new InternalNode(id, maxInternalKeys);
    node.keys = data.keys;
    node.children = data.children;
    return node;
  }

  /**
   * Reads the [LeafNode] with [id] from the manager.
   */
  private LeafNode readLeafNode(N id) {
    LeafNode node = new LeafNode(id, maxLeafKeys);
    LeafNodeData<K, V> data = manager.readLeaf(id);
    node.keys = data.keys;
    node.values = data.values;
    return node;
  }

  /**
   * Reads the [InternalNode] or [LeafNode] with [id] from the manager.
   */
  private Node readNode(N id) {
    if (manager.isInternal(id)) {
      return readInternalNode(id);
    } else {
      return readLeafNode(id);
    }
  }

  private void removeListRange(List<?> list, int offset, int length) {
    list.subList(offset, length).clear();
  }

  private void setListLength(List<?> list, int newLength) {
    list.subList(newLength, list.size()).clear();
  }

  /**
   * Writes [node] into the manager.
   */
  private void writeInternalNode(InternalNode node) {
    manager.writeInternal(node.id, new InternalNodeData<K, N>(node.keys, node.children));
  }

  /**
   * Writes [node] into the manager.
   */
  private void writeLeafNode(LeafNode node) {
    manager.writeLeaf(node.id, new LeafNodeData<K, V>(node.keys, node.values));
  }
}
