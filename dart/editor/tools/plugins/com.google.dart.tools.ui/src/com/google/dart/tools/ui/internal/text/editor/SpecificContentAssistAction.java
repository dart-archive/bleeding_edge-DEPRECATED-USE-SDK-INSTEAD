/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.internal.text.dart.CompletionProposalCategory;
import com.google.dart.tools.ui.internal.text.dart.CompletionProposalComputerRegistry;
import com.google.dart.tools.ui.text.DartPartitions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to run content assist on a specific proposal category.
 */
final class SpecificContentAssistAction extends Action implements IUpdate {
  /**
   * The category represented by this action.
   */
  private final CompletionProposalCategory fCategory;
  /**
   * The content assist executor.
   */
  private final SpecificContentAssistExecutor fExecutor = new SpecificContentAssistExecutor(
      CompletionProposalComputerRegistry.getDefault());
  /**
   * The editor.
   */
  private DartEditor fEditor;

  /**
   * Creates a new action for a certain proposal category.
   * 
   * @param category
   */
  public SpecificContentAssistAction(CompletionProposalCategory category) {
    fCategory = category;
    setText(category.getName());
    setImageDescriptor(category.getImageDescriptor());
    setActionDefinitionId("com.google.dart.tools.ui.specific_content_assist.command"); //$NON-NLS-1$
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    ITextEditor editor = getActiveEditor();
    if (editor == null) {
      return;
    }

    fExecutor.invokeContentAssist(editor, fCategory.getId());

    return;
  }

  /**
   * Sets the active editor part.
   * 
   * @param part the editor, possibly <code>null</code>
   */
  public void setActiveEditor(IEditorPart part) {
    DartEditor editor;
    if (part instanceof DartEditor) {
      editor = (DartEditor) part;
    } else {
      editor = null;
    }
    fEditor = editor;
    setEnabled(computeEnablement(fEditor));
  }

  /*
   * @see org.eclipse.ui.texteditor.IUpdate#update()
   */
  @Override
  public void update() {
    setEnabled(computeEnablement(fEditor));
  }

  private boolean computeEnablement(ITextEditor editor) {
    if (editor == null) {
      return false;
    }
    ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
    boolean hasContentAssist = target != null
        && target.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
    if (!hasContentAssist) {
      return false;
    }

    ISelection selection = editor.getSelectionProvider().getSelection();
    return isValidSelection(selection);
  }

  private ITextEditor getActiveEditor() {
    return fEditor;
  }

  private IDocument getDocument() {
    Assert.isTrue(fEditor != null);
    IDocumentProvider provider = fEditor.getDocumentProvider();
    if (provider == null) {
      return null;
    }

    IDocument document = provider.getDocument(fEditor.getEditorInput());
    return document;
  }

  /**
   * Computes the partition type at the selection start and checks whether the proposal category has
   * any computers for this partition.
   * 
   * @param selection the selection
   * @return <code>true</code> if there are any computers for the selection
   */
  private boolean isValidSelection(ISelection selection) {
    if (!(selection instanceof ITextSelection)) {
      return false;
    }
    int offset = ((ITextSelection) selection).getOffset();

    IDocument document = getDocument();
    if (document == null) {
      return false;
    }

    String contentType;
    try {
      contentType = TextUtilities.getContentType(document, DartPartitions.DART_PARTITIONING,
          offset, true);
    } catch (BadLocationException x) {
      return false;
    }

    return fCategory.hasComputers(contentType);
  }
}
