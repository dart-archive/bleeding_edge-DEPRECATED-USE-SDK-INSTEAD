/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.ASTNode;

/**
 * Instances of the class {@code NodeLocator} locate the {@link ASTNode AST node} associated with a
 * source range, given the AST structure built from the source. More specifically, they will return
 * the {@link ASTNode AST node} with the shortest length whose source range completely encompasses
 * the specified range.
 */
public class NodeLocator extends GeneralizingASTVisitor<Void> {
  /**
   * Instances of the class {@code NodeFoundException} are used to cancel visiting after a node has
   * been found.
   */
  private class NodeFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * The start offset of the range used to identify the node.
   */
  private final int startOffset;

  /**
   * The end offset of the range used to identify the node.
   */
  private final int endOffset;

  /**
   * The element that was found that corresponds to the given source range, or {@code null} if there
   * is no such element.
   */
  private ASTNode foundNode;

  /**
   * Initialize a newly created locator to locate one or more {@link ASTNode AST nodes} by locating
   * the node within an AST structure that corresponds to the given offset in the source.
   * 
   * @param offset the offset used to identify the node
   */
  public NodeLocator(int offset) {
    this(offset, offset);
  }

  /**
   * Initialize a newly created locator to locate one or more {@link ASTNode AST nodes} by locating
   * the node within an AST structure that corresponds to the given range of characters in the
   * source.
   * 
   * @param start the start offset of the range used to identify the node
   * @param end the end offset of the range used to identify the node
   */
  public NodeLocator(int start, int end) {
    this.startOffset = start;
    this.endOffset = end;
  }

  /**
   * Return the node that was found that corresponds to the given source range, or {@code null} if
   * there is no such node.
   * 
   * @return the node that was found
   */
  public ASTNode getFoundNode() {
    return foundNode;
  }

  /**
   * Search within the given AST node for an identifier representing a {@link DartElement Dart
   * element} in the specified source range. Return the element that was found, or {@code null} if
   * no element was found.
   * 
   * @param node the AST node within which to search
   * @return the element that was found
   */
  public ASTNode searchWithin(ASTNode node) {
    try {
      node.accept(this);
    } catch (NodeFoundException exception) {
      // A node with the right source position was found.
    } catch (Exception exception) {
//      DartCore.logInformation("Unable to locate element at offset (" + startOffset + " - "
//          + endOffset + ")", exception);
      return null;
    }
    return foundNode;
  }

  @Override
  public Void visitNode(ASTNode node) {
    try {
      node.visitChildren(this);
    } catch (NodeFoundException exception) {
      throw exception;
    } catch (Exception exception) {
      // Ignore the exception and proceed in order to visit the rest of the structure.
//      DartCore.logInformation(
//          "Exception caught while traversing an AST structure. Please report to the editor team.",
//          exception);
    }
    int start = node.getOffset();
    int end = start + node.getLength();
    if (start <= startOffset && endOffset <= end) {
      foundNode = node;
      throw new NodeFoundException();
    }
    return null;
  }
}
