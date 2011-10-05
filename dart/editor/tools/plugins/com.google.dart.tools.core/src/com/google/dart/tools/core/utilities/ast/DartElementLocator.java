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
package com.google.dart.tools.core.utilities.ast;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.type.Type;
import com.google.dart.tools.core.dom.visitor.ChildVisitor;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

import org.eclipse.jface.text.IRegion;

/**
 * Instances of the class <code>DartElementLocator</code> locate the {@link DartElement Dart
 * element(s)} associated with a source range, given the AST structure built from the source.
 */
public class DartElementLocator extends DartNodeTraverser<Void> {
  /**
   * Instances of the class <code>DartElementFoundException</code> are used to cancel visiting after
   * an element has been found.
   */
  private class DartElementFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * The compilation unit containing the element to be found.
   */
  private final CompilationUnit compilationUnit;

  /**
   * The start offset of the range used to identify the element.
   */
  private final int startOffset;

  /**
   * The end offset of the range used to identify the element.
   */
  private final int endOffset;

  /**
   * The found element or <code>null</code> if there is none.
   */
  private DartElement foundElement;

  /**
   * The region within the element that needs to be highlighted, or <code>null</code> if the element
   * can be used to determine the region.
   */
  private IRegion candidateRegion;

  /**
   * A visitor that will visit all of the children of the node being visited.
   */
  private ChildVisitor<Void> childVisitor = new ChildVisitor<Void>(this);

  /**
   * Initialize a newly create locator to locate one or more {@link DartElement Dart elements} by
   * locating the node within the given compilation unit that corresponds to the given range of
   * characters in the source.
   * 
   * @param input the compilation unit containing the element to be found
   * @param start the start offset of the range used to identify the element
   * @param end the end offset of the range used to identify the element
   */
  public DartElementLocator(CompilationUnit input, int start, int end) {
    this.compilationUnit = input;
    this.startOffset = start;
    this.endOffset = end;
  }

  /**
   * Return the region within the element that needs to be highlighted, or <code>null</code> if
   * either there is no element or if the element can be used to determine the region.
   * 
   * @return the region within the element that needs to be highlighted
   */
  public IRegion getCandidateRegion() {
    return candidateRegion;
  }

  /**
   * Return the element that was found, or <code>null</code> if there is none.
   * 
   * @return the element that was found
   */
  public DartElement getFoundElement() {
    return foundElement;
  }

  /**
   * Search within the given AST node for an identifier representing a {@link DartElement Dart
   * element} in the specified source range. Return the element that was found, or <code>null</code>
   * if no element was found.
   * 
   * @param node the AST node within which to search
   * @return the element that was found
   */
  public DartElement searchWithin(DartNode node) {
    try {
      node.accept(childVisitor);
    } catch (DartElementFoundException exception) {
      // A node with the right source position was found.
    }
    return foundElement;
  }

  /**
   * Determine whether the given node is within the specified range.
   * 
   * @param node the node being tested
   * @throws DartElementFoundException if the node matches the target range
   */
  @Override
  public Void visitIdentifier(DartIdentifier node) {
    if (foundElement == null) {
      int start = node.getSourceStart();
      int end = start + node.getSourceLength();
      if (start <= startOffset && endOffset <= end) {
        Element targetSymbol = node.getTargetSymbol();
        if (targetSymbol == null) {
          DartNode parent = node.getParent();
          if (parent instanceof DartTypeNode) {
            Type type = DartAstUtilities.getType((DartTypeNode) parent);
            if (type != null) {
              targetSymbol = type.getElement();
            }
          } else if (parent instanceof DartMethodInvocation) {
            DartMethodInvocation invocation = (DartMethodInvocation) parent;
            if (node == invocation.getFunctionName()) {
              targetSymbol = (Element) invocation.getTargetSymbol();
            }
          }
        }
        if (targetSymbol != null) {
          foundElement = BindingUtils.getDartElement(compilationUnit.getLibrary(), targetSymbol);
        } else {
          foundElement = null;
        }
        throw new DartElementFoundException();
      }
    }
    return null;
  }

  @Override
  public Void visitNode(DartNode node) {
    node.accept(childVisitor);
    return null;
  }
}
