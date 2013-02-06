/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.services.assist;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.source.Source;

/**
 * Context for which assistance should be provided.
 */
public class AssistContext {
  private final CompilationUnit compilationUnit;
  private final int selectionOffset;
  private final int selectionLength;
  private ASTNode coveredNode;
  private ASTNode coveringNode;

  public AssistContext(CompilationUnit compilationUnit, int selectionOffset, int selectionLength) {
    this.compilationUnit = compilationUnit;
    this.selectionOffset = selectionOffset;
    this.selectionLength = selectionLength;
  }

  /**
   * @return the resolved {@link CompilationUnit} of the {@link Source}.
   */
  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * @return the {@link ASTNode} that is covered by the selection.
   */
  public ASTNode getCoveredNode() {
    if (coveredNode == null) {
      NodeLocator locator = new NodeLocator(selectionOffset, selectionOffset);
      coveredNode = locator.searchWithin(compilationUnit);
    }
    return coveredNode;
  }

  /**
   * @return the ASTNode that covers the selection.
   */
  public ASTNode getCoveringNode() {
    if (coveringNode == null) {
      NodeLocator locator = new NodeLocator(selectionOffset, selectionOffset + selectionLength);
      coveringNode = locator.searchWithin(compilationUnit);
    }
    return coveringNode;
  }

  /**
   * @return the length of the selection.
   */
  public int getSelectionLength() {
    return selectionLength;
  }

  /**
   * @return the offset of the selection.
   */
  public int getSelectionOffset() {
    return selectionOffset;
  }

  /**
   * @return the {@link Source} to provide corrections in.
   */
  public Source getSource() {
    CompilationUnitElement element = compilationUnit.getElement();
    if (element != null) {
      return element.getSource();
    }
    return null;
  }
}
