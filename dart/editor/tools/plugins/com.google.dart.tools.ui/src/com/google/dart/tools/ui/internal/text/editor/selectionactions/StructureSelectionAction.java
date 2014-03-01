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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.services.util.SelectionAnalyzer;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;

import java.util.ArrayList;
import java.util.List;

public abstract class StructureSelectionAction extends Action {

  public static final String NEXT = "SelectNextElement"; //$NON-NLS-1$
  public static final String PREVIOUS = "SelectPreviousElement"; //$NON-NLS-1$
  public static final String ENCLOSING = "SelectEnclosingElement"; //$NON-NLS-1$
  public static final String HISTORY = "RestoreLastSelection"; //$NON-NLS-1$

  protected static SourceRange getLastCoveringNodeRange(SourceRange oldSourceRange, AstNode sr,
      SelectionAnalyzer selAnalyzer) {
    if (selAnalyzer.getCoveringNode() == null) {
      return oldSourceRange;
    } else {
      return getSelectedNodeSourceRange(sr, selAnalyzer.getCoveringNode());
    }
  }

  protected static SourceRange getSelectedNodeSourceRange(AstNode sr, AstNode nodeToSelect) {
    int offset = nodeToSelect.getOffset();
    int end = Math.min(sr.getLength(), nodeToSelect.getOffset() + nodeToSelect.getLength());
    return createSourceRange(offset, end);
  }

  static SourceRange createSourceRange(int offset, int end) {
    int length = end - offset;
    if (length == 0) {
      length = 1;
    }
    return new SourceRange(Math.max(0, offset), length);
  }

  static List<AstNode> getSiblingNodes(AstNode node) {
    final List<AstNode> children = new ArrayList<AstNode>();
    if (node.getParent() == null) {
      children.add(node);
      return children;
    }
    GeneralizingAstVisitor<Void> childVisitor = new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitNode(AstNode node) {
        children.add(node);
        return null;
      }
    };
    node.getParent().visitChildren(childVisitor);
    return children;
  }

  private DartEditor editor;
  private SelectionHistory selectionHistory;

  /*
   * This constructor is for testing purpose only.
   */
  protected StructureSelectionAction() {
    super(""); //$NON-NLS-1$
  }

  protected StructureSelectionAction(String text, DartEditor editor, SelectionHistory history) {
    super(text);
    Assert.isNotNull(editor);
    Assert.isNotNull(history);
    this.editor = editor;
    this.selectionHistory = history;
  }

  public final SourceRange getNewSelectionRange(SourceRange oldSourceRange, AstNode node) {
    CompilationUnit compilationUnit = editor.getInputUnit();
    if (compilationUnit == null) {
      return oldSourceRange;
    }
    SelectionAnalyzer selAnalyzer = new SelectionAnalyzer(oldSourceRange);
    compilationUnit.accept(selAnalyzer);
    return internalGetNewSelectionRange(oldSourceRange, node, selAnalyzer);
  }

  /*
   * Method declared in IAction.
   */
  @Override
  public final void run() {
    CompilationUnit compilationUnit = editor.getInputUnit();
    if (compilationUnit == null) {
      return;
    }
    ITextSelection selection = getTextSelection();
    SourceRange selectionRange = new SourceRange(selection.getOffset(), selection.getLength());
    SourceRange newRange = getNewSelectionRange(selectionRange, compilationUnit);
    // Check if new selection differs from current selection
    if (selectionRange.equals(newRange)) {
      return;
    }
    selectionHistory.remember(new SourceRange(selection.getOffset(), selection.getLength()));
    try {
      selectionHistory.ignoreSelectionChanges();
      changeSelection(newRange.getOffset(), newRange.getLength());
    } finally {
      selectionHistory.listenToSelectionChanges();
    }
  }

  protected void changeSelection(int offset, int len) {
    editor.selectAndReveal(offset, len);
  }

  protected final ITextSelection getTextSelection() {
    return (ITextSelection) editor.getSelectionProvider().getSelection();
  }

  /**
   * Subclasses determine the actual new selection.
   */
  abstract SourceRange internalGetNewSelectionRange(SourceRange oldSourceRange, AstNode node,
      SelectionAnalyzer selAnalyzer);

}
