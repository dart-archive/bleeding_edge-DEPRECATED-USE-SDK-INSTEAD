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
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.SourceRange;

/**
 * Context for which assistance should be provided.
 */
public class AssistContext {
  private final SearchEngine searchEngine;
  private final CompilationUnit compilationUnit;
  private final int selectionOffset;
  private final int selectionLength;
  private ASTNode coveredNode;
  private ASTNode coveringNode;
  private Element coveredElement;
  private boolean coveredElementFound;

  public AssistContext(SearchEngine searchEngine, CompilationUnit compilationUnit,
      int selectionOffset, int selectionLength) {
    this.searchEngine = searchEngine;
    this.compilationUnit = compilationUnit;
    this.selectionOffset = selectionOffset;
    this.selectionLength = selectionLength;
  }

  public AssistContext(SearchEngine searchEngine, CompilationUnit compilationUnit,
      SourceRange selectionRange) {
    this(searchEngine, compilationUnit, selectionRange.getOffset(), selectionRange.getLength());
  }

  /**
   * @return the resolved {@link CompilationUnit} of the {@link Source}.
   */
  public CompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * @return the {@link Element} of the {@link #coveredNode}, may be <code>null</code>.
   */
  public Element getCoveredElement() {
    if (!coveredElementFound) {
      coveredElementFound = true;
      ASTNode coveredNode = getCoveredNode();
      if (coveredNode == null) {
        return null;
      }
      coveredElement = ElementLocator.locate(coveredNode);
    }
    return coveredElement;
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
   * @return the {@link SearchEngine} which should be used in this context.
   */
  public SearchEngine getSearchEngine() {
    return searchEngine;
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
   * @return the {@link SourceRange} of the selection.
   */
  public SourceRange getSelectionRange() {
    return new SourceRange(selectionOffset, selectionLength);
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
