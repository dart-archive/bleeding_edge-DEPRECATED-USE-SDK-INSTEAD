/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.dom.rewrite;

import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.StructuralPropertyDescriptor;
import com.google.dart.tools.core.internal.dom.rewrite.ListRewriteEvent;
import com.google.dart.tools.core.internal.dom.rewrite.NodeInfoStore;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEvent;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEventStore;

import org.eclipse.text.edits.TextEditGroup;

import java.util.Collections;
import java.util.List;

/**
 * Instances of the class <code>ListRewrite</code> describe manipulations to a child list property
 * of an AST node.
 */
public final class ListRewrite {

  private DartNode parent;
  private StructuralPropertyDescriptor childProperty;
  private ASTRewrite rewriter;

  /* package */ListRewrite(ASTRewrite rewriter, DartNode parent,
      StructuralPropertyDescriptor childProperty) {
    this.rewriter = rewriter;
    this.parent = parent;
    this.childProperty = childProperty;
  }

  /**
   * Creates and returns a placeholder node for a true copy of a range of nodes of the current list.
   * The placeholder node can either be inserted as new or used to replace an existing node. When
   * the document is rewritten, a copy of the source code for the given node range is inserted into
   * the output document at the position corresponding to the placeholder (indentation is adjusted).
   * 
   * @param first the node that starts the range
   * @param last the node that ends the range
   * @return the new placeholder node
   * @throws IllegalArgumentException An exception is thrown if the first or last node are
   *           <code>null</code>, if a node is not a child of the current list or if the first node
   *           is not before the last node. An <code>IllegalArgumentException</code> is also thrown
   *           if the copied range is overlapping with an other moved or copied range.
   */
  public final DartNode createCopyTarget(DartNode first, DartNode last) {
    if (first == last) {
      return rewriter.createCopyTarget(first);
    } else {
      return createTargetNode(first, last, false, null, null);
    }
  }

  /**
   * Create and return a placeholder node for a move of a range of nodes of the current list. The
   * placeholder node can either be inserted as new or used to replace an existing node. When the
   * document is rewritten, a copy of the source code for the given node range is inserted into the
   * output document at the position corresponding to the placeholder (indentation is adjusted).
   * 
   * @param first the node that starts the range
   * @param last the node that ends the range
   * @return the new placeholder node
   * @throws IllegalArgumentException An exception is thrown if the first or last node are
   *           <code>null</code>, if a node is not a child of the current list or if the first node
   *           is not before the last node. An <code>IllegalArgumentException</code> is also thrown
   *           if the moved range is overlapping with an other moved or copied range.
   */
  public final DartNode createMoveTarget(DartNode first, DartNode last) {
    return createMoveTarget(first, last, null, null);
  }

  /**
   * Create and return a placeholder node for a move of a range of nodes of the current list. The
   * moved nodes can optionally be replaced by a specified node. The placeholder node can either be
   * inserted as new or used to replace an existing node. When the document is rewritten, a copy of
   * the source code for the given node range is inserted into the output document at the position
   * corresponding to the placeholder (indentation is adjusted).
   * 
   * @param first the node that starts the range
   * @param last the node that ends the range
   * @param replacingNode a node that is set at the location of the moved nodes or <code>null</code>
   *          to remove all nodes
   * @param editGroup the edit group in which to collect the corresponding text edits fro a replace,
   *          or <code>null</code> if ungrouped
   * @return the new placeholder node
   * @throws IllegalArgumentException An exception is thrown if the first or last node are
   *           <code>null</code>, if a node is not a child of the current list or if the first node
   *           is not before the last node. An <code>IllegalArgumentException
   * </code> is also thrown if the moved range is overlapping with an other moved or copied range.
   */
  public final DartNode createMoveTarget(DartNode first, DartNode last, DartNode replacingNode,
      TextEditGroup editGroup) {
    if (first == last) {
      replace(first, replacingNode, editGroup);
      return rewriter.createMoveTarget(first);
    } else {
      return createTargetNode(first, last, true, replacingNode, editGroup);
    }
  }

  /**
   * Return the ASTRewrite instance from which this ListRewriter has been created from.
   * 
   * @return the parent AST Rewriter instance
   */
  public ASTRewrite getASTRewrite() {
    return rewriter;
  }

  /**
   * Return the property of the parent node for which this list rewriter was created.
   * 
   * @return the property of the parent node for which this list rewriter was created
   * @see #getParent()
   */
  public StructuralPropertyDescriptor getLocationInParent() {
    return childProperty;
  }

  /**
   * Return the original nodes in the list property managed by this rewriter. The returned list is
   * unmodifiable.
   * 
   * @return a list of all original nodes in the list
   */
  @SuppressWarnings("unchecked")
  public List<DartNode> getOriginalList() {
    Object originalValue = getEvent().getOriginalValue();
    if (originalValue instanceof List) {
      return Collections.unmodifiableList((List<DartNode>) originalValue);
    }
    return Collections.emptyList();
  }

  /**
   * Return the parent of the list for which this list rewriter was created.
   * 
   * @return the node that contains the list for which this list rewriter was created
   * @see #getLocationInParent()
   */
  public DartNode getParent() {
    return parent;
  }

  /**
   * Return the nodes in the revised list property managed by this rewriter. The returned list is
   * unmodifiable.
   * 
   * @return a list of all nodes in the list taking into account all the described changes
   */
  @SuppressWarnings("unchecked")
  public List<DartNode> getRewrittenList() {
    Object newValue = getEvent().getNewValue();
    if (newValue instanceof List) {
      return Collections.unmodifiableList((List<DartNode>) newValue);
    }
    return Collections.emptyList();
  }

  /**
   * Inserts the given node into the list after the given element. The existing node must be in the
   * list, either as an original or as a new node that has been inserted. The inserted node must
   * either be brand new (not part of the original AST) or a placeholder node (for example, one
   * created by {@link ASTRewrite#createCopyTarget(DartNode)},
   * {@link ASTRewrite#createMoveTarget(DartNode)}, or
   * {@link ASTRewrite#createStringPlaceholder(String, int)}). The AST itself is not actually
   * modified in any way; rather, the rewriter just records a note that this node has been inserted
   * into the list.
   * 
   * @param node the node to insert
   * @param element the element after which the given node is to be inserted
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node or element is null, or if the node is not part of
   *           this rewriter's AST, or if the inserted node is not a new node (or placeholder), or
   *           if <code>element</code> is not a member of the list (original or new), or if the
   *           described modification is otherwise invalid
   */
  public void insertAfter(DartNode node, DartNode element, TextEditGroup editGroup) {
    if (node == null || element == null) {
      throw new IllegalArgumentException();
    }
    int index = getEvent().getIndex(element, ListRewriteEvent.BOTH);
    if (index == -1) {
      throw new IllegalArgumentException("Node does not exist"); //$NON-NLS-1$
    }
    internalInsertAt(node, index + 1, true, editGroup);
  }

  /**
   * Inserts the given node into the list at the given index. The index corresponds to a combined
   * list of original and new nodes; removed or replaced nodes are still in the combined list. The
   * inserted node must either be brand new (not part of the original AST) or a placeholder node
   * (for example, one created by {@link ASTRewrite#createCopyTarget(DartNode)},
   * {@link ASTRewrite#createMoveTarget(DartNode)}, or
   * {@link ASTRewrite#createStringPlaceholder(String, int)}). The AST itself is not actually
   * modified in any way; rather, the rewriter just records a note that this node has been inserted
   * into the list.
   * 
   * @param node the node to insert
   * @param index insertion index in the combined list of original and inserted nodes;
   *          <code>-1</code> indicates insertion as the last element
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the inserted node is not a new node (or placeholder), or if the
   *           described modification is otherwise invalid (not a member of this node's original
   *           list)
   * @throws IndexOutOfBoundsException if the index is negative and not -1, or if it is larger than
   *           the size of the combined list
   */
  public void insertAt(DartNode node, int index, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    internalInsertAt(node, index, isInsertBoundToPreviousByDefault(node), editGroup);
  }

  /**
   * Inserts the given node into the list before the given element. The existing node must be in the
   * list, either as an original or as a new node that has been inserted. The inserted node must
   * either be brand new (not part of the original AST) or a placeholder node (for example, one
   * created by {@link ASTRewrite#createCopyTarget(DartNode)},
   * {@link ASTRewrite#createMoveTarget(DartNode)}, or
   * {@link ASTRewrite#createStringPlaceholder(String, int)}). The AST itself is not actually
   * modified in any way; rather, the rewriter just records a note that this node has been inserted
   * into the list.
   * 
   * @param node the node to insert
   * @param element the element before which the given node is to be inserted
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node or element is null, or if the node is not part of
   *           this rewriter's AST, or if the inserted node is not a new node (or placeholder), or
   *           if <code>element</code> is not a member of the list (original or new), or if the
   *           described modification is otherwise invalid
   */
  public void insertBefore(DartNode node, DartNode element, TextEditGroup editGroup) {
    if (node == null || element == null) {
      throw new IllegalArgumentException();
    }
    int index = getEvent().getIndex(element, ListRewriteEvent.BOTH);
    if (index == -1) {
      throw new IllegalArgumentException("Node does not exist"); //$NON-NLS-1$
    }
    internalInsertAt(node, index, false, editGroup);
  }

  /**
   * Inserts the given node into the list at the start of the list. Equivalent to
   * <code>insertAt(node, 0, editGroup)</code>.
   * 
   * @param node the node to insert
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the inserted node is not a new node (or placeholder), or if the
   *           described modification is otherwise invalid (not a member of this node's original
   *           list)
   * @see #insertAt(DartNode, int, TextEditGroup)
   */
  public void insertFirst(DartNode node, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    internalInsertAt(node, 0, false, editGroup);
  }

  /**
   * Inserts the given node into the list at the end of the list. Equivalent to
   * <code>insertAt(node, -1, editGroup)</code>.
   * 
   * @param node the node to insert
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the inserted node is not a new node (or placeholder), or if the
   *           described modification is otherwise invalid (not a member of this node's original
   *           list)
   * @see #insertAt(DartNode, int, TextEditGroup)
   */
  public void insertLast(DartNode node, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    internalInsertAt(node, -1, true, editGroup);
  }

  /**
   * Removes the given node from its parent's list property in the rewriter. The node must be
   * contained in the list. The AST itself is not actually modified in any way; rather, the rewriter
   * just records a note that this node has been removed from this list.
   * 
   * @param node the node being removed. The node can either be an original node in this list or
   *          (since 3.4) a new node already inserted or used as replacement in this AST rewriter.
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the described modification is invalid (not a member of this
   *           node's original list)
   */
  public void remove(DartNode node, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    RewriteEvent event = getEvent().removeEntry(node);
    if (editGroup != null) {
      getRewriteStore().setEventEditGroup(event, editGroup);
    }
  }

  /**
   * Replaces the given node from its parent's list property in the rewriter. The node must be
   * contained in the list. The replacement node must either be brand new (not part of the original
   * AST) or a placeholder node (for example, one created by
   * {@link ASTRewrite#createCopyTarget(DartNode)}, {@link ASTRewrite#createMoveTarget(DartNode)},
   * or {@link ASTRewrite#createStringPlaceholder(String, int)}). The AST itself is not actually
   * modified in any way; rather, the rewriter just records a note that this node has been replaced
   * in this list.
   * 
   * @param node the node being removed. The node can either be an original node in this list or
   *          (since 3.4) a new node already inserted or used as replacement in this AST rewriter.
   * @param replacement the replacement node, or <code>null</code> if no replacement
   * @param editGroup the edit group in which to collect the corresponding text edits, or
   *          <code>null</code> if ungrouped
   * @throws IllegalArgumentException if the node is null, or if the node is not part of this
   *           rewriter's AST, or if the replacement node is not a new node (or placeholder), or if
   *           the described modification is otherwise invalid (not a member of this node's original
   *           list)
   */
  public void replace(DartNode node, DartNode replacement, TextEditGroup editGroup) {
    if (node == null) {
      throw new IllegalArgumentException();
    }
    RewriteEvent event = getEvent().replaceEntry(node, replacement);
    if (editGroup != null) {
      getRewriteStore().setEventEditGroup(event, editGroup);
    }
  }

  private DartNode createTargetNode(DartNode first, DartNode last, boolean isMove,
      DartNode replacingNode, TextEditGroup editGroup) {
    if (first == null || last == null) {
      throw new IllegalArgumentException();
    }

    NodeInfoStore nodeStore = rewriter.getNodeStore();
    DartCore.notYetImplemented();
    return null;
    // DartNode placeholder= nodeStore.newPlaceholderNode(first.getNodeType());
    // // revisit: could use list type
    // if (placeholder == null) {
    //      throw new IllegalArgumentException("Creating a target node is not supported for nodes of type" + first.getClass().getName()); //$NON-NLS-1$
    // }
    //
    // DartBlock internalPlaceHolder= nodeStore.createCollapsePlaceholder();
    // CopySourceInfo info= getRewriteStore().createRangeCopy(parent,
    // childProperty, first, last, isMove, internalPlaceHolder,
    // replacingNode, editGroup);
    // nodeStore.markAsCopyTarget(placeholder, info);
    //
    // return placeholder;
  }

  private ListRewriteEvent getEvent() {
    return getRewriteStore().getListEvent(parent, childProperty, true);
  }

  private RewriteEventStore getRewriteStore() {
    return rewriter.getRewriteEventStore();
  }

  private void internalInsertAt(DartNode node, int index, boolean boundToPrevious,
      TextEditGroup editGroup) {
    RewriteEvent event = getEvent().insert(node, index);
    if (boundToPrevious) {
      getRewriteStore().setInsertBoundToPrevious(node);
    }
    if (editGroup != null) {
      getRewriteStore().setEventEditGroup(event, editGroup);
    }
  }

  /*
   * Heuristic to decide if a inserted node is bound to previous or the next sibling.
   */
  private boolean isInsertBoundToPreviousByDefault(DartNode node) {
    return (node instanceof DartStatement || node instanceof DartFieldDefinition);
  }
}
