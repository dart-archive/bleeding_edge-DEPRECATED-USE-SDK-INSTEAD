/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.core.internal.util.ResourceUtil;
import com.google.dart.tools.internal.corext.fix.LinkedProposalModel;
import com.google.dart.tools.internal.corext.fix.LinkedProposalPositionGroup;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.DartUiStatus;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorUtility;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.internal.viewsupport.LinkedProposalModelPresenter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditVisitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import java.io.File;

/**
 * A proposal for quick fixes and quick assist that work on a single compilation unit.
 * <p>
 * The proposal takes care of the preview of the changes as proposal information.
 * </p>
 * 
 * @coverage dart.editor.ui.correction
 */
public class CUCorrectionProposal_NEW extends ChangeCorrectionProposal {

  private final File file;
  private LinkedProposalModel linkedProposalModel;
  private boolean switchedEditor;

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
  public CUCorrectionProposal_NEW(String name, String filePath, TextChange change, int relevance,
      Image image) {
    super(name, change, relevance, image);
    if (filePath == null) {
      throw new IllegalArgumentException("File must not be null"); //$NON-NLS-1$
    }
    this.file = new File(filePath);
    this.linkedProposalModel = null;
  }

  @Override
  public void apply(IDocument document) {
    try {
      IResource resource = ResourceUtil.getResource(file);
      if (resource != null && resource.exists()) {
        boolean canEdit = performValidateEdit(resource);
        if (!canEdit) {
          return;
        }
        // ensure DartEditor is open
        IEditorPart part;
        {
          part = EditorUtility.isOpenInEditor(resource);
          IWorkbenchPage page = DartToolsPlugin.getActivePage();
          if (page != null && part != null) {
            page.bringToTop(part);
          }
          if (part != null) {
            part.setFocus();
          }
        }
        // do apply change
        performChange(part, document);
      }
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
    linkedProposalModel = model;
  }

  @Override
  public String toString() {
    try {
      return getPreviewContent();
    } catch (CoreException e) {
    }
    return super.toString();
  }

  protected LinkedProposalModel getLinkedProposalModel() {
    if (linkedProposalModel == null) {
      linkedProposalModel = new LinkedProposalModel();
    }
    return linkedProposalModel;
  }

  @Override
  protected void performChange(IEditorPart part, IDocument document) throws CoreException {
    try {
      super.performChange(part, document);
      if (part == null) {
        return;
      }

      if (linkedProposalModel != null) {
        if (linkedProposalModel.hasLinkedPositions() && part instanceof DartEditor) {
          // enter linked mode
          ITextViewer viewer = ((DartEditor) part).getViewer();
          new LinkedProposalModelPresenter().enterLinkedMode(
              viewer,
              part,
              switchedEditor,
              linkedProposalModel);
        } else if (part instanceof ITextEditor) {
          LinkedProposalPositionGroup.PositionInformation endPosition = linkedProposalModel.getEndPosition();
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

  private void appendContent(IDocument text, int startOffset, int endOffset, StringBuffer buf,
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
        // NOTE(scheglov) now we show tabs/spaces, so don't skip blank content,
        // it may be prefix before some change.
//        if (surroundLinesOnly && from == start && StringUtils.isBlank(content)) {
//          continue; // ignore empty lines except when range started in the middle of a line
//        }
        for (int k = 0; k < content.length(); k++) {
          char ch = content.charAt(k);
          if (ch == '<') {
            buf.append("&lt;"); //$NON-NLS-1$
          } else if (ch == '>') {
            buf.append("&gt;"); //$NON-NLS-1$
          } else if (ch == ' ') {
            // DefaultInformationControl, i.e. HTML2TextReader handles this correctly,
            // even if this is not quite correct in normal HTML (where we would use &nbsp;).
            buf.append("<pre> </pre>"); //$NON-NLS-1$
          } else if (ch == '\t') {
            buf.append("<pre>  </pre>"); //$NON-NLS-1$
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
  private boolean performValidateEdit(IResource resource) {
    IStatus status = org.eclipse.ltk.internal.core.refactoring.Resources.makeCommittable(
        resource,
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
