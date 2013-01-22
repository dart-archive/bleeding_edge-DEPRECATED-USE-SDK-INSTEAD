
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
  factory NodeList(ASTNode owner) {
    _jtd_constructor_73_impl(owner);
  }
  _jtd_constructor_73_impl(ASTNode owner) {
    this.owner = owner;
  }
  /**
   * Use the given visitor to visit each of the nodes in this list.
   * @param visitor the visitor to be used to visit the elements of this list
   */
  accept(ASTVisitor visitor) {
    for (E element in elements) {
      element.accept(visitor);
    }
  }
//  void add(int index, E node) {
//    owner.becomeParentOf(node);
//    elements.add(index, node);
//  }
//  bool addAll(Collection<E> nodes) {
//    if (nodes != null) {
//      return super.addAll(nodes);
//    }
//    return false;
//  }
//  E get(int index) {
//    return elements[index];
//  }
  /**
   * Return the first token included in this node's source range.
   * @return the first token included in this node's source range
   */
  Token getBeginToken() {
    if (elements.isEmpty) {
      return null;
    }
    return elements[0].getBeginToken();
  }
  /**
   * Return the last token included in this node list's source range.
   * @return the last token included in this node list's source range
   */
  Token getEndToken() {
    if (elements.isEmpty) {
      return null;
    }
    return elements[elements.length - 1].getEndToken();
  }
  /**
   * Return the node that is the parent of each of the elements in the list.
   * @return the node that is the parent of each of the elements in the list
   */
  ASTNode getOwner() {
    return owner;
  }
//  E remove(int index) {
//    return elements.removeAt(index);
//  }
//  E set(int index, E node) {
//    owner.becomeParentOf(node);
//    return elements.set(index, node);
//  }
//  int size() {
//    return elements.length;
//  }
}
