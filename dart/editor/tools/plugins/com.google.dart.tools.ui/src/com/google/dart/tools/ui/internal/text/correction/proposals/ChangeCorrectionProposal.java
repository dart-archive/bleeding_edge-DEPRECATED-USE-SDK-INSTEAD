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

import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.correction.CorrectionCommandHandler;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.correction.ICommandAccess;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

/**
 * Implementation of a Java completion proposal to be used for quick fix and quick assist proposals
 * that invoke a {@link Change}. The proposal offers a proposal information but no context
 * information.
 * 
 * @coverage dart.editor.ui.correction
 */
public class ChangeCorrectionProposal implements IDartCompletionProposal, ICommandAccess,
    ICompletionProposalExtension5, ICompletionProposalExtension6 {

  private static final NullChange COMPUTING_CHANGE = new NullChange(
      "ChangeCorrectionProposal computing..."); //$NON-NLS-1$

  private Change fChange;
  private String fName;
  private int fRelevance;
  private Image fImage;
  private String fCommandId;

  /**
   * Constructs a change correction proposal.
   * 
   * @param name The name that is displayed in the proposal selection dialog.
   * @param change The change that is executed when the proposal is applied or <code>null</code> if
   *          the change will be created by implementors of {@link #createChange()}.
   * @param relevance The relevance of this proposal.
   * @param image The image that is displayed for this proposal or <code>null</code> if no image is
   *          desired.
   */
  public ChangeCorrectionProposal(String name, Change change, int relevance, Image image) {
    if (name == null) {
      throw new IllegalArgumentException("Name must not be null"); //$NON-NLS-1$
    }
    fName = name;
    fChange = change;
    fRelevance = relevance;
    fImage = image;
    fCommandId = null;
  }

  /*
   * @see ICompletionProposal#apply(IDocument)
   */
  @Override
  public void apply(IDocument document) {
    try {
      performChange(DartToolsPlugin.getActivePage().getActiveEditor(), document);
    } catch (CoreException e) {
      ExceptionHandler.handle(
          e,
          CorrectionMessages.ChangeCorrectionProposal_error_title,
          CorrectionMessages.ChangeCorrectionProposal_error_message);
    }
  }

  /*
   * @see ICompletionProposal#getAdditionalProposalInfo()
   */
  @Override
  public String getAdditionalProposalInfo() {
    Object info = getAdditionalProposalInfo(new NullProgressMonitor());
    return info == null ? null : info.toString();
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    StringBuffer buf = new StringBuffer();
    buf.append("<p>"); //$NON-NLS-1$
    try {
      Change change = getChange();
      if (change != null) {
        String name = change.getName();
        if (name.length() == 0) {
          return null;
        }
        buf.append(name);
      } else {
        return null;
      }
    } catch (CoreException e) {
      buf.append("Unexpected error when accessing this proposal:<p><pre>"); //$NON-NLS-1$
      buf.append(e.getLocalizedMessage());
      buf.append("</pre>"); //$NON-NLS-1$
    }
    buf.append("</p>"); //$NON-NLS-1$
    return buf.toString();
  }

  /**
   * Returns the change that will be executed when the proposal is applied.
   * 
   * @return returns the change for this proposal.
   * @throws CoreException thrown when the change could not be created
   */
  public final Change getChange() throws CoreException {
    if (Util.isGtk()) {
      // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=293995 :
      // [Widgets] Deadlock while UI thread displaying/computing a change proposal and non-UI thread creating image

      // Solution is to create the change outside a 'synchronized' block.
      // Synchronization is achieved by polling fChange, using "fChange == COMPUTING_CHANGE" as barrier.
      // Timeout of 10s for safety reasons (should not be reached).
      long end = System.currentTimeMillis() + 10000;
      do {
        boolean computing;
        synchronized (this) {
          computing = fChange == COMPUTING_CHANGE;
        }
        if (computing) {
          try {
            Display display = Display.getCurrent();
            if (display != null) {
              while (!display.isDisposed() && display.readAndDispatch()) {
              }
              display.sleep();
            } else {
              Thread.sleep(100);
            }
          } catch (InterruptedException e) {
            //continue
          }
        } else {
          synchronized (this) {
            if (fChange == COMPUTING_CHANGE) {
              continue;
            } else if (fChange != null) {
              return fChange;
            } else {
              fChange = COMPUTING_CHANGE;
            }
          }
          Change change = createChange();
          synchronized (this) {
            fChange = change;
          }
          return change;
        }
      } while (System.currentTimeMillis() < end);

      synchronized (this) {
        if (fChange == COMPUTING_CHANGE) {
          return null; //failed
        }
      }

    } else {
      synchronized (this) {
        if (fChange == null) {
          fChange = createChange();
        }
      }
    }
    return fChange;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.internal.ui.text.correction.IShortcutProposal#getProposalId()
   */
  @Override
  public String getCommandId() {
    return fCommandId;
  }

  /*
   * @see ICompletionProposal#getContextInformation()
   */
  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  /*
   * @see ICompletionProposal#getDisplayString()
   */
  @Override
  public String getDisplayString() {
    String shortCutString = CorrectionCommandHandler.getShortCutString(getCommandId());
    if (shortCutString != null) {
      return Messages.format(
          CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut,
          new String[] {getName(), shortCutString});
    }
    return getName();
  }

  /*
   * @see ICompletionProposal#getImage()
   */
  @Override
  public Image getImage() {
    return fImage;
  }

  /**
   * Returns the name of the proposal.
   * 
   * @return return the name of the proposal
   */
  public String getName() {
    return fName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposal#getRelevance()
   */
  @Override
  public int getRelevance() {
    return fRelevance;
  }

  /*
   * @see ICompletionProposal#getSelection(IDocument)
   */
  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension6#getStyledDisplayString()
   */
  @Override
  public StyledString getStyledDisplayString() {
    StyledString str = new StyledString(getName());

    String shortCutString = CorrectionCommandHandler.getShortCutString(getCommandId());
    if (shortCutString != null) {
      String decorated = Messages.format(
          CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut,
          new String[] {getName(), shortCutString});
      return StyledCellLabelProvider.styleDecoratedString(
          decorated,
          StyledString.QUALIFIER_STYLER,
          str);
    }
    return str;
  }

  /**
   * Set the proposal id to allow assigning a shortcut to the correction proposal.
   * 
   * @param commandId The proposal id for this proposal or <code>null</code> if no command should be
   *          assigned to this proposal.
   */
  public void setCommandId(String commandId) {
    fCommandId = commandId;
  }

  /**
   * Sets the display name.
   * 
   * @param name the name to set
   */
  public void setDisplayName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name must not be null"); //$NON-NLS-1$
    }
    fName = name;
  }

  /**
   * Sets the proposal's image or <code>null</code> if no image is desired.
   * 
   * @param image the desired image.
   */
  public void setImage(Image image) {
    fImage = image;
  }

  /**
   * Sets the relevance.
   * 
   * @param relevance the relevance to set
   */
  public void setRelevance(int relevance) {
    fRelevance = relevance;
  }

  /**
   * Creates the text change for this proposal. This method is only called once and only when no
   * text change has been passed in {@link #ChangeCorrectionProposal(String, Change, int, Image)}.
   * 
   * @return returns the created change.
   * @throws CoreException thrown if the creation of the change failed.
   */
  protected Change createChange() throws CoreException {
    return new NullChange();
  }

  /**
   * Performs the change associated with this proposal.
   * 
   * @param activeEditor The editor currently active or <code>null</code> if no editor is active.
   * @param document The document of the editor currently active or <code>null</code> if no editor
   *          is visible.
   * @throws CoreException Thrown when the invocation of the change failed.
   */
  protected void performChange(IEditorPart activeEditor, IDocument document) throws CoreException {
    StyledText disabledStyledText = null;
    TraverseListener traverseBlocker = null;

    Change change = null;
    IRewriteTarget rewriteTarget = null;
    try {
      change = getChange();
      if (change != null) {
        if (document != null) {
          LinkedModeModel.closeAllModels(document);
        }
        if (activeEditor != null) {
          rewriteTarget = (IRewriteTarget) activeEditor.getAdapter(IRewriteTarget.class);
          if (rewriteTarget != null) {
            rewriteTarget.beginCompoundChange();
          }
          /*
           * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=195834#c7 : During change
           * execution, an EventLoopProgressMonitor can process the event queue while the text
           * widget has focus. When that happens and the user e.g. pressed a key, the event is
           * prematurely delivered to the text widget and screws up the document. Change execution
           * fails or performs wrong changes.
           * 
           * The fix is to temporarily disable the text widget.
           */
          Object control = activeEditor.getAdapter(Control.class);
          if (control instanceof StyledText) {
            disabledStyledText = (StyledText) control;
            if (disabledStyledText.getEditable()) {
              disabledStyledText.setEditable(false);
              traverseBlocker = new TraverseListener() {
                @Override
                public void keyTraversed(TraverseEvent e) {
                  e.doit = true;
                  e.detail = SWT.TRAVERSE_NONE;
                }
              };
              disabledStyledText.addTraverseListener(traverseBlocker);
            } else {
              disabledStyledText = null;
            }
          }
        }

        change.initializeValidationData(new NullProgressMonitor());
        RefactoringStatus valid = change.isValid(new NullProgressMonitor());
        if (valid.hasFatalError()) {
          IStatus status = new Status(
              IStatus.ERROR,
              DartToolsPlugin.getPluginId(),
              IStatus.ERROR,
              valid.getMessageMatchingSeverity(RefactoringStatus.FATAL),
              null);
          throw new CoreException(status);
        } else {
          IUndoManager manager = RefactoringCore.getUndoManager();
          Change undoChange;
          boolean successful = false;
          try {
            manager.aboutToPerformChange(change);
            undoChange = change.perform(new NullProgressMonitor());
            successful = true;
          } finally {
            manager.changePerformed(change, successful);
          }
          if (undoChange != null) {
            undoChange.initializeValidationData(new NullProgressMonitor());
            manager.addUndo(getName(), undoChange);
          }
        }
      }
    } finally {
      if (disabledStyledText != null) {
        disabledStyledText.setEditable(true);
        disabledStyledText.removeTraverseListener(traverseBlocker);
      }
      if (rewriteTarget != null) {
        rewriteTarget.endCompoundChange();
      }

      if (change != null) {
        change.dispose();
      }
    }
  }

}
