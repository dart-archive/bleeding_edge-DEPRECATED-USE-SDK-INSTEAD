
/**
 * Instances of the class {@code NodeList} represent a list of AST nodes that have a common parent.
 */
class NodeList<E extends ASTNode> extends ListWrapper<E> {
  /**
   * The node that is the parent of each of the elements in the list.
   */
  ASTNode owner;
  /**
   * The elements of the list.
   */
  List<E> elements = new List<E>();
  /**
   * Initialize a newly created list of nodes to be empty.
   * @param owner the node that is the parent of each of the elements in the list
   */
  NodeList(ASTNode this.owner);
  /**
   * Use the given visitor to visit each of the nodes in this list.
   * @param visitor the visitor to be used to visit the elements of this list
   */
  accept(ASTVisitor visitor) {
    for (E element in elements) {
      element.accept(visitor);
    }
  }
  void add(E node) {
    owner.becomeParentOf(node);
    elements.add(node);
  }
  bool addAll(Iterable<E> nodes) {
    if (nodes != null) {
      for (E node in nodes) {
        add(node);
      }
      return true;
    }
    return false;
  }
  /**
   * Return the first token included in this node's source range.
   * @return the first token included in this node's source range
   */
  Token get beginToken {
    if (elements.isEmpty) {
      return null;
    }
    return elements[0].beginToken;
  }
  /**
   * Return the last token included in this node list's source range.
   * @return the last token included in this node list's source range
   */
  Token get endToken {
    if (elements.isEmpty) {
      return null;
    }
    return elements[elements.length - 1].endToken;
  }
  /**
   * Return the node that is the parent of each of the elements in the list.
   * @return the node that is the parent of each of the elements in the list
   */
  ASTNode getOwner() {
    return owner;
  }
}
