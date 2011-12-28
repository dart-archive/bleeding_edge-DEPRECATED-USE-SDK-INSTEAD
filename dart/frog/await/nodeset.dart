// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** Weak set of AST nodes. */
// TODO(sigmund): delete this. This is a temporary workaround to keep 'frog'
// as independent as possible of the await experimental feature. Ideally we
// should either make [Node] hashable or store information collected by analyses
// in the nodes themselves.
class NodeSet {
  Map<String, List<Node>> _hashset;
  NodeSet() : _hashset = {};

  bool add(Node n) {
    if (contains(n)) return false;
    String key = n.span.locationText;
    List<Node> nodes = _hashset[key];
    if (nodes == null) {
      _hashset[key] = [n];
    } else {
      nodes.add(n);
    }
    return true;
  }

  bool contains(Node n) {
    String key = n.span.locationText;
    List<Node> nodes = _hashset[key];
    if (nodes == null) {
      return false;
    }
    for (Node member in nodes) {
      if (n === member) return true;
    }
    return false;
  }
}
