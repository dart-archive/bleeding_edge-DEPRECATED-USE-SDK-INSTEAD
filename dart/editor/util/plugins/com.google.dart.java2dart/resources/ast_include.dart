
/**
 * Instances of the class {@code NodeList} represent a list of AST nodes that have a common parent.
 */
class NodeList<E extends AstNode> extends Object with ListMixin<E> {
  /**
   * Create an empty list with the given owner. This is a convenience method that allows the
   * compiler to determine the correct value of the type argument [E] without needing to
   * explicitly specify it.
   *
   * @param owner the node that is the parent of each of the elements in the list
   * @return the list that was created
   */
  static NodeList create(AstNode owner) => new NodeList(owner);

  /**
   * The node that is the parent of each of the elements in the list.
   */
  AstNode owner;

  /**
   * The elements contained in the list.
   */
  List<E> _elements = <E> [];

  /**
   * Initialize a newly created list of nodes to be empty.
   *
   * @param owner the node that is the parent of each of the elements in the list
   */
  NodeList(this.owner);

  /**
   * Use the given visitor to visit each of the nodes in this list.
   *
   * @param visitor the visitor to be used to visit the elements of this list
   */
  accept(AstVisitor visitor) {
    var length = _elements.length;
    for (var i = 0; i < length; i++) {
      _elements[i].accept(visitor);
    }
  }
  void add(E node) {
    insert(length, node);
  }
  void insert(int index, E node) {
    int length = _elements.length;
    if (index < 0 || index > length) {
      throw new RangeError("Index: ${index}, Size: ${_elements.length}");
    }
    owner.becomeParentOf(node);
    if (length == 0) {
      _elements = <E> [node];
    } else {
      _elements.insert(index, node);
    }
  }
  bool addAll(Iterable<E> nodes) {
    if (nodes != null && !nodes.isEmpty) {
      _elements.addAll(nodes);
      for (E node in nodes) {
        owner.becomeParentOf(node);
      }
      return true;
    }
    return false;
  }
  E operator[](int index) {
    if (index < 0 || index >= _elements.length) {
      throw new RangeError("Index: ${index}, Size: ${_elements.length}");
    }
    return _elements[index] as E;
  }

  /**
   * Return the first token included in this node's source range.
   *
   * @return the first token included in this node's source range
   */
  Token get beginToken {
    if (_elements.length == 0) {
      return null;
    }
    return _elements[0].beginToken;
  }

  /**
   * Return the last token included in this node list's source range.
   *
   * @return the last token included in this node list's source range
   */
  Token get endToken {
    if (_elements.length == 0) {
      return null;
    }
    return _elements[_elements.length - 1].endToken;
  }
  E removeAt(int index) {
    if (index < 0 || index >= _elements.length) {
      throw new RangeError("Index: ${index}, Size: ${_elements.length}");
    }
    E removedNode = _elements[index] as E;
    int length = _elements.length;
    if (length == 1) {
      _elements = AstNode.EMPTY_ARRAY;
      return removedNode;
    }
    _elements.removeAt(index);
    return removedNode;
  }
  void operator[]=(int index, E node) {
    if (index < 0 || index >= _elements.length) {
      throw new RangeError("Index: ${index}, Size: ${_elements.length}");
    }
    owner.becomeParentOf(node);
    _elements[index] = node;
  }
  int get length => _elements.length;
  void set length(int value) {
    throw new UnsupportedError("Cannot resize NodeList.");
  }
}
