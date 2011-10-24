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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.internal.text.Selection;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;

public abstract class StructureSelectionAction extends Action {

  public static final String NEXT = "SelectNextElement"; //$NON-NLS-1$
  public static final String PREVIOUS = "SelectPreviousElement"; //$NON-NLS-1$
  public static final String ENCLOSING = "SelectEnclosingElement"; //$NON-NLS-1$
  public static final String HISTORY = "RestoreLastSelection"; //$NON-NLS-1$

  protected static SourceRange getLastCoveringNodeRange(SourceRange oldSourceRange,
      SourceReference sr, SelectionAnalyzer selAnalyzer) throws DartModelException {
    if (selAnalyzer.getLastCoveringNode() == null) {
      return oldSourceRange;
    } else {
      return getSelectedNodeSourceRange(sr, selAnalyzer.getLastCoveringNode());
    }
  }

  protected static SourceRange getSelectedNodeSourceRange(SourceReference sr, DartNode nodeToSelect)
      throws DartModelException {
    int offset = nodeToSelect.getSourceStart();
    int end = Math.min(sr.getSourceRange().getLength(), nodeToSelect.getSourceStart()
        + nodeToSelect.getSourceLength() - 1);
    return createSourceRange(offset, end);
  }

  static SourceRange createSourceRange(int offset, int end) {
    int length = end - offset + 1;
    if (length == 0) {
      length = 1;
    }
    return newSourceRange(Math.max(0, offset), length);
  }

  static int findIndex(Object[] array, Object o) {
    for (int i = 0; i < array.length; i++) {
      Object object = array[i];
      if (object == o) {
        return i;
      }
    }
    return -1;
  }

  static DartNode[] getSiblingNodes(DartNode node) {
    DartX.notYet();
    // DartNode parent = node.getParent();
    // StructuralPropertyDescriptor locationInParent =
    // node.getLocationInParent();
    // if (locationInParent.isChildListProperty()) {
    // List siblings = (List) parent.getStructuralProperty(locationInParent);
    // return (DartNode[]) siblings.toArray(new DartNode[siblings.size()]);
    // }
    return null;
  }

  static SourceRange newSourceRange(final int offset, final int length) {
    DartX.todo();
    return new SourceRange() {
      @Override
      public int getLength() {
        return length;
      }

      @Override
      public int getOffset() {
        return offset;
      }
    };
  }

  private static SourceRange createSourceRange(ITextSelection ts) {
    return newSourceRange(ts.getOffset(), ts.getLength());
  }

  private static DartUnit getAST(SourceReference sr) {
    return ASTProvider.getASTProvider().getAST((DartElement) sr, ASTProvider.WAIT_YES, null);
  }

  private DartEditor fEditor;

  private SelectionHistory fSelectionHistory;

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
    fEditor = editor;
    fSelectionHistory = history;
  }

  public final SourceRange getNewSelectionRange(SourceRange oldSourceRange, SourceReference sr) {
    try {
      DartUnit root = getAST(sr);
      if (root == null) {
        return oldSourceRange;
      }
      Selection selection = Selection.createFromStartLength(oldSourceRange.getOffset(),
          oldSourceRange.getLength());
      SelectionAnalyzer selAnalyzer = new SelectionAnalyzer(selection, true);
      root.accept(selAnalyzer);
      return internalGetNewSelectionRange(oldSourceRange, sr, selAnalyzer);
    } catch (DartModelException e) {
      DartToolsPlugin.log(e); // dialog would be too heavy here
      return newSourceRange(oldSourceRange.getOffset(), oldSourceRange.getLength());
    }
  }

  /*
   * Method declared in IAction.
   */
  @Override
  public final void run() {
    DartElement inputElement = EditorUtility.getEditorInputJavaElement(fEditor, false);
    if (!(inputElement instanceof SourceReference && inputElement.exists())) {
      return;
    }

    SourceReference source = (SourceReference) inputElement;
    SourceRange sourceRange;
    try {
      sourceRange = source.getSourceRange();
      if (sourceRange == null || sourceRange.getLength() == 0) {
        MessageDialog.openInformation(fEditor.getEditorSite().getShell(),
            SelectionActionMessages.StructureSelect_error_title,
            SelectionActionMessages.StructureSelect_error_message);
        return;
      }
    } catch (DartModelException e) {
    }
    ITextSelection selection = getTextSelection();
    SourceRange newRange = getNewSelectionRange(createSourceRange(selection), source);
    // Check if new selection differs from current selection
    if (selection.getOffset() == newRange.getOffset()
        && selection.getLength() == newRange.getLength()) {
      return;
    }
    fSelectionHistory.remember(newSourceRange(selection.getOffset(), selection.getLength()));
    try {
      fSelectionHistory.ignoreSelectionChanges();
      fEditor.selectAndReveal(newRange.getOffset(), newRange.getLength());
    } finally {
      fSelectionHistory.listenToSelectionChanges();
    }
  }

  protected final ITextSelection getTextSelection() {
    return (ITextSelection) fEditor.getSelectionProvider().getSelection();
  }

  /**
   * Subclasses determine the actual new selection.
   */
  abstract SourceRange internalGetNewSelectionRange(SourceRange oldSourceRange, SourceReference sr,
      SelectionAnalyzer selAnalyzer) throws DartModelException;

}
