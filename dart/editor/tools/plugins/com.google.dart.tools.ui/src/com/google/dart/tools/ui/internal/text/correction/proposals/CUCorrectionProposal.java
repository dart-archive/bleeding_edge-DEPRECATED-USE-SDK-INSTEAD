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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.refactoring.CompilationUnitChange;
import com.google.dart.tools.internal.corext.fix.LinkedProposalModel;
import com.google.dart.tools.internal.corext.fix.LinkedProposalPositionGroup;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.StubUtility;
import com.google.dart.tools.ui.internal.DartUiStatus;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.viewsupport.LinkedProposalModelPresenter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditVisitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A proposal for quick fixes and quick assist that work on a single compilation unit. Either a
 * {@link TextChange text change} is directly passed in the constructor or method
 * {@link #addEdits(IDocument, TextEdit)} is overridden to provide the text edits that are applied
 * to the document when the proposal is evaluated.
 * <p>
 * The proposal takes care of the preview of the changes as proposal information.
 * </p>
 * 
 * @coverage dart.editor.ui.correction
 */
public class CUCorrectionProposal extends ChangeCorrectionProposal {

  private final CompilationUnit fCompilationUnit;
  private LinkedProposalModel fLinkedProposalModel;
  private boolean fSwitchedEditor;

  private final int surroundLines = 1;

  /**
   * Constructs a correction proposal working on a compilation unit with a given text change
   * 
   * @param name the name that is displayed in the proposal selection dialog.
   * @param cu the compilation unit on that the change works.
   * @param change the change that is executed when the proposal is applied or <code>null</code> if
   *          implementors override {@link #addEdits(IDocument, TextEdit)} to provide the text edits
   *          or {@link #createTextChange()} to provide a text change.
   * @param relevance the relevance of this proposal.
   * @param image the image that is displayed for this proposal or <code>null</code> if no image is
   *          desired.
   */
  public CUCorrectionProposal(String name, CompilationUnit cu, TextChange change, int relevance,
      Image image) {
    super(name, change, relevance, image);
    if (cu == null) {
      throw new IllegalArgumentException("Compilation unit must not be null"); //$NON-NLS-1$
    }
    fCompilationUnit = cu;
    fLinkedProposalModel = null;
  }

  /**
   * Constructs a correction proposal working on a compilation unit.
   * <p>
   * Users have to override {@link #addEdits(IDocument, TextEdit)} to provide the text edits or
   * {@link #createTextChange()} to provide a text change.
   * </p>
   * 
   * @param name The name that is displayed in the proposal selection dialog.
   * @param cu The compilation unit on that the change works.
   * @param relevance The relevance of this proposal.
   * @param image The image that is displayed for this proposal or <code>null</code> if no image is
   *          desired.
   */
  protected CUCorrectionProposal(String name, CompilationUnit cu, int relevance, Image image) {
    this(name, cu, null, relevance, image);
  }

  @Override
  public void apply(IDocument document) {
    try {
      CompilationUnit unit = getCompilationUnit();
      IEditorPart part = null;
      if (unit.getResource().exists()) {
        boolean canEdit = performValidateEdit(unit);
        if (!canEdit) {
          return;
        }
        part = EditorUtility.isOpenInEditor(unit);
        if (part == null) {
          part = DartUI.openInEditor(unit);
          if (part != null) {
            fSwitchedEditor = true;
            document = DartUI.getDocumentProvider().getDocument(part.getEditorInput());
          }
        }
        IWorkbenchPage page = DartToolsPlugin.getActivePage();
        if (page != null && part != null) {
          page.bringToTop(part);
        }
        if (part != null) {
          part.setFocus();
        }
      }
      performChange(part, document);
    } catch (CoreException e) {
      ExceptionHandler.handle(
          e,
          CorrectionMessages.CUCorrectionProposal_error_title,
          CorrectionMessages.CUCorrectionProposal_error_message);
    }
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {

    final StringBuffer buf = new StringBuffer();

    try {
      final TextChange change = getTextChange();

      change.setKeepPreviewEdits(true);
      final IDocument previewContent = change.getPreviewDocument(monitor);
      final TextEdit rootEdit = change.getPreviewEdit(change.getEdit());

      class EditAnnotator extends TextEditVisitor {
        private int fWrittenToPos = 0;

        public void unchangedUntil(int pos) {
          if (pos > fWrittenToPos) {
            appendContent(previewContent, fWrittenToPos, pos, buf, true);
            fWrittenToPos = pos;
          }
        }

        @Override
        public boolean visit(CopyTargetEdit edit) {
          return true; //return rangeAdded(edit);
        }

        @Override
        public boolean visit(DeleteEdit edit) {
          return rangeRemoved(edit);
        }

        @Override
        public boolean visit(InsertEdit edit) {
          return rangeAdded(edit);
        }

        @Override
        public boolean visit(MoveSourceEdit edit) {
          return rangeRemoved(edit);
        }

        @Override
        public boolean visit(MoveTargetEdit edit) {
          return true; //rangeAdded(edit);
        }

        @Override
        public boolean visit(ReplaceEdit edit) {
          if (edit.getLength() > 0) {
            return rangeAdded(edit);
          }
          return rangeRemoved(edit);
        }

        private boolean rangeAdded(TextEdit edit) {
          unchangedUntil(edit.getOffset());
          buf.append("<b>"); //$NON-NLS-1$
          appendContent(previewContent, edit.getOffset(), edit.getExclusiveEnd(), buf, false);
          buf.append("</b>"); //$NON-NLS-1$
          fWrittenToPos = edit.getExclusiveEnd();
          return false;
        }

        private boolean rangeRemoved(TextEdit edit) {
          unchangedUntil(edit.getOffset());
          return false;
        }
      }
      EditAnnotator ea = new EditAnnotator();
      rootEdit.accept(ea);

      // Final pre-existing region
      ea.unchangedUntil(previewContent.getLength());
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
    return buf.toString();
  }

  /**
   * The compilation unit on that the change works.
   * 
   * @return the compilation unit on that the change works.
   */
  public final CompilationUnit getCompilationUnit() {
    return fCompilationUnit;
  }

  /**
   * Creates a preview of the content of the compilation unit after applying the change.
   * 
   * @return returns the preview of the changed compilation unit.
   * @throws CoreException thrown if the creation of the change failed.
   */
  public String getPreviewContent() throws CoreException {
    return getTextChange().getPreviewContent(new NullProgressMonitor());
  }

  /**
   * Gets the text change that is invoked when the change is applied.
   * 
   * @return returns the text change that is invoked when the change is applied.
   * @throws CoreException throws an exception if accessing the change failed
   */
  public final TextChange getTextChange() throws CoreException {
    return (TextChange) getChange();
  }

  public void setLinkedProposalModel(LinkedProposalModel model) {
    fLinkedProposalModel = model;
  }

  @Override
  public String toString() {
    try {
      return getPreviewContent();
    } catch (CoreException e) {
    }
    return super.toString();
  }

  /**
   * Called when the {@link CompilationUnitChange} is initialized. Subclasses can override to add
   * text edits to the root edit of the change. Implementors must not access the proposal, e.g
   * getting the change.
   * <p>
   * The default implementation does not add any edits
   * </p>
   * 
   * @param document content of the underlying compilation unit. To be accessed read only.
   * @param editRoot The root edit to add all edits to
   * @throws CoreException can be thrown if adding the edits is failing.
   */
  protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
  }

  @Override
  protected final Change createChange() throws CoreException {
    return createTextChange(); // make sure that only text changes are allowed here
  }

  /**
   * Creates the text change for this proposal. This method is only called once and only when no
   * text change has been passed in
   * {@link #CUCorrectionProposal(String, CompilationUnit, TextChange, int, Image)}.
   * 
   * @return returns the created text change.
   * @throws CoreException thrown if the creation of the text change failed.
   */
  protected TextChange createTextChange() throws CoreException {
    CompilationUnit cu = getCompilationUnit();
    String name = getName();
    TextChange change;
    if (!cu.getResource().exists()) {
      String source;
      try {
        source = cu.getSource();
      } catch (DartModelException e) {
        DartToolsPlugin.log(e);
        source = new String(); // empty
      }
      Document document = new Document(source);
      document.setInitialLineDelimiter(StubUtility.getLineDelimiterUsed(cu));
      change = new DocumentChange(name, document);
    } else {
      CompilationUnitChange cuChange = new CompilationUnitChange(name, cu);
      cuChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
      change = cuChange;
    }
    TextEdit rootEdit = new MultiTextEdit();
    change.setEdit(rootEdit);

    // initialize text change
    IDocument document = change.getCurrentDocument(new NullProgressMonitor());
    addEdits(document, rootEdit);
    return change;
  }

  protected LinkedProposalModel getLinkedProposalModel() {
    if (fLinkedProposalModel == null) {
      fLinkedProposalModel = new LinkedProposalModel();
    }
    return fLinkedProposalModel;
  }

  @Override
  protected void performChange(IEditorPart part, IDocument document) throws CoreException {
    try {
      super.performChange(part, document);
      if (part == null) {
        return;
      }

      if (fLinkedProposalModel != null) {
        if (fLinkedProposalModel.hasLinkedPositions() && part instanceof DartEditor) {
          // enter linked mode
          ITextViewer viewer = ((DartEditor) part).getViewer();
          new LinkedProposalModelPresenter().enterLinkedMode(
              viewer,
              part,
              fSwitchedEditor,
              fLinkedProposalModel);
        } else if (part instanceof ITextEditor) {
          LinkedProposalPositionGroup.PositionInformation endPosition = fLinkedProposalModel.getEndPosition();
          if (endPosition != null) {
            // select a result
            int pos = endPosition.getOffset() + endPosition.getLength();
            ((ITextEditor) part).selectAndReveal(pos, 0);
          }
        }
      }
    } catch (BadLocationException e) {
      throw new CoreException(DartUiStatus.createError(IStatus.ERROR, e));
    }
  }

  private void appendContent(
      IDocument text,
      int startOffset,
      int endOffset,
      StringBuffer buf,
      boolean surroundLinesOnly) {
    try {
      int startLine = text.getLineOfOffset(startOffset);
      int endLine = text.getLineOfOffset(endOffset);

      boolean dotsAdded = false;
      if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
        startLine = Math.max(endLine - surroundLines, 0);
        buf.append("...<br>"); //$NON-NLS-1$
        dotsAdded = true;
      }

      for (int i = startLine; i <= endLine; i++) {
        if (surroundLinesOnly) {
          if (i - startLine > surroundLines && endLine - i > surroundLines) {
            if (!dotsAdded) {
              buf.append("...<br>"); //$NON-NLS-1$
              dotsAdded = true;
            } else if (endOffset == text.getLength()) {
              return; // no surround lines for the bottom no-change range
            }
            continue;
          }
        }

        IRegion lineInfo = text.getLineInformation(i);
        int start = lineInfo.getOffset();
        int end = start + lineInfo.getLength();

        int from = Math.max(start, startOffset);
        int to = Math.min(end, endOffset);
        String content = text.get(from, to - from);
        if (surroundLinesOnly && from == start && StringUtils.isBlank(content)) {
          continue; // ignore empty lines except when range started in the middle of a line
        }
        for (int k = 0; k < content.length(); k++) {
          char ch = content.charAt(k);
          if (ch == '<') {
            buf.append("&lt;"); //$NON-NLS-1$
          } else if (ch == '>') {
            buf.append("&gt;"); //$NON-NLS-1$
          } else {
            buf.append(ch);
          }
        }
        if (to == end && to != endOffset) { // new line when at the end of the line, and not end of range
          buf.append("<br>"); //$NON-NLS-1$
        }
      }
    } catch (BadLocationException e) {
      // ignore
    }
  }

  @SuppressWarnings("restriction")
  private boolean performValidateEdit(CompilationUnit unit) {
    IStatus status = org.eclipse.ltk.internal.core.refactoring.Resources.makeCommittable(
        unit.getResource(),
        DartToolsPlugin.getActiveWorkbenchShell());
    if (!status.isOK()) {
      String label = CorrectionMessages.CUCorrectionProposal_error_title;
      String message = CorrectionMessages.CUCorrectionProposal_error_message;
      ErrorDialog.openError(DartToolsPlugin.getActiveWorkbenchShell(), label, message, status);
      return false;
    }
    return true;
  }
}
