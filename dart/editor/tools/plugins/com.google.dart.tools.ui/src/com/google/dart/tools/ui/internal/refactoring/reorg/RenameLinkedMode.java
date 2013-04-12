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
package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.refactoring.NamingConventions;
import com.google.dart.tools.internal.corext.refactoring.util.DartElementUtil;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.refactoring.RefactoringUtils;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;
import com.google.dart.tools.ui.internal.text.correction.proposals.LinkedNamesAssistProposal.DeleteBlockingExitPolicy;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewerExtension6;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IUndoManagerExtension;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameLinkedMode {

  private class EditorSynchronizer implements ILinkedModeListener {
    @Override
    public void left(LinkedModeModel model, int flags) {
      linkedModeLeft();
      if ((flags & ILinkedModeListener.UPDATE_CARET) != 0) {
        doRename(fShowPreview);
      }
    }

    @Override
    public void resume(LinkedModeModel model, int flags) {
    }

    @Override
    public void suspend(LinkedModeModel model) {
    }
  }

  private class ExitPolicy extends DeleteBlockingExitPolicy {
    public ExitPolicy(IDocument document) {
      super(document);
    }

    @Override
    public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
      fShowPreview = (event.stateMask & SWT.CTRL) != 0
          && (event.character == SWT.CR || event.character == SWT.LF);
      return super.doExit(model, event, offset, length);
    }
  }

  private class FocusEditingSupport implements IEditingSupport {
    @Override
    public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
      return false; //leave on external modification outside positions
    }

    @Override
    public boolean ownsFocusShell() {
      if (fInfoPopup == null) {
        return false;
      }
      if (fInfoPopup.ownsFocusShell()) {
        return true;
      }

      Shell editorShell = fEditor.getSite().getShell();
      Shell activeShell = editorShell.getDisplay().getActiveShell();
      if (editorShell == activeShell) {
        return true;
      }
      return false;
    }
  }

  private static RenameLinkedMode fgActiveLinkedMode;

  public static RenameLinkedMode getActiveLinkedMode() {
    if (fgActiveLinkedMode != null) {
      ISourceViewer viewer = fgActiveLinkedMode.fEditor.getViewer();
      if (viewer != null) {
        StyledText textWidget = viewer.getTextWidget();
        if (textWidget != null && !textWidget.isDisposed()) {
          return fgActiveLinkedMode;
        }
      }
      // make sure we don't hold onto the active linked mode if anything went wrong with canceling:
      fgActiveLinkedMode = null;
    }
    return null;
  }

  /**
   * Given {@link Element} provided by resolver, returns {@link Element} which should be renamed.
   */
  private static Element getCanonicalElement(Element element) {
    element = DartElementUtil.getFieldIfFieldFormalParameter(element);
    element = DartElementUtil.getBaseIfMember(element);
    element = DartElementUtil.getVariableIfAccessor(element);
    return element;
  }

  private final DartEditor fEditor;
  private AssistContext context;
  private SimpleIdentifier nameNode;

  private Element fDartElement;
  private RenameInformationPopup fInfoPopup;

  private Point fOriginalSelection;
  private String fOriginalName;
  private LinkedPosition fNamePosition;
  private LinkedModeModel fLinkedModeModel;
  private LinkedPositionGroup fLinkedPositionGroup;

  private final FocusEditingSupport fFocusEditingSupport;

  private boolean fShowPreview;

  /**
   * The operation on top of the undo stack when the rename is {@link #start()}ed, or
   * <code>null</code> if rename has not been started or the undo stack was empty.
   * 
   * @since 3.5
   */
  private IUndoableOperation fStartingUndoOperation;

  public RenameLinkedMode(DartEditor editor, AssistContext context) {
    this.context = context;
    Assert.isNotNull(editor);
    fEditor = editor;
    fFocusEditingSupport = new FocusEditingSupport();
  }

//	private void startAnimation() {
//		//TODO:
//		// - switch off if animations disabled
//		// - show rectangle around target for 500ms after animation
//		Shell shell= fEditor.getSite().getShell();
//		StyledText textWidget= fEditor.getViewer().getTextWidget();
//
//		// from popup:
//		Rectangle startRect= fPopup.getBounds();
//
//		// from editor:
////		Point startLoc= textWidget.getParent().toDisplay(textWidget.getLocation());
////		Point startSize= textWidget.getSize();
////		Rectangle startRect= new Rectangle(startLoc.x, startLoc.y, startSize.x, startSize.y);
//
//		// from hell:
////		Rectangle startRect= shell.getClientArea();
//
//		Point caretLocation= textWidget.getLocationAtOffset(textWidget.getCaretOffset());
//		Point displayLocation= textWidget.toDisplay(caretLocation);
//		Rectangle targetRect= new Rectangle(displayLocation.x, displayLocation.y, 0, 0);
//
//		RectangleAnimation anim= new RectangleAnimation(shell, startRect, targetRect);
//		anim.schedule();
//	}

  public void cancel() {
    if (fLinkedModeModel != null) {
      fLinkedModeModel.exit(ILinkedModeListener.NONE);
    }
    linkedModeLeft();
  }

  public LinkedPosition getCurrentLinkedPosition() {
    int start = context.getSelectionOffset();
    int end = start + context.getSelectionLength();
    LinkedPosition[] positions = fLinkedPositionGroup.getPositions();
    for (int i = 0; i < positions.length; i++) {
      LinkedPosition position = positions[i];
      if (position.includes(start) && position.includes(end)) {
        return position;
      }
    }
    return null;
  }

  public boolean isCaretInLinkedPosition() {
    return getCurrentLinkedPosition() != null;
  }

  public boolean isEnabled() {
    try {
      String newName = fNamePosition.getContent();
      if (fOriginalName.equals(newName)) {
        return false;
      }
      // TODO(scheglov) we may be rename not only variable, but also method, type, etc
      return NamingConventions.validateVariableName(newName).isOK();
    } catch (BadLocationException e) {
      return false;
    }

  }

  public boolean isOriginalName() {
    try {
      String newName = fNamePosition.getContent();
      return fOriginalName.equals(newName);
    } catch (BadLocationException e) {
      return false;
    }
  }

  public void start() {
    if (getActiveLinkedMode() != null) {
      // for safety; should already be handled in RenameDartElementAction
      fgActiveLinkedMode.startFullDialog();
      return;
    }

    ISourceViewer viewer = fEditor.getViewer();
    IDocument document = viewer.getDocument();
    fOriginalSelection = viewer.getSelectedRange();
    int offset = fOriginalSelection.x;

    try {
      fLinkedPositionGroup = new LinkedPositionGroup();
      prepareElement();
      if (fDartElement == null) {
        return;
      }

      if (viewer instanceof ITextViewerExtension6) {
        IUndoManager undoManager = ((ITextViewerExtension6) viewer).getUndoManager();
        if (undoManager instanceof IUndoManagerExtension) {
          IUndoManagerExtension undoManagerExtension = (IUndoManagerExtension) undoManager;
          IUndoContext undoContext = undoManagerExtension.getUndoContext();
          IOperationHistory operationHistory = OperationHistoryFactory.getOperationHistory();
          fStartingUndoOperation = operationHistory.getUndoOperation(undoContext);
        }
      }

      fOriginalName = nameNode.getName();
      final int pos = nameNode.getOffset();
      final List<ASTNode> sameNodes = Lists.newArrayList();
      nameNode.getRoot().accept(new RecursiveASTVisitor<Void>() {
        @Override
        public Void visitSimpleIdentifier(SimpleIdentifier node) {
          Element element = node.getElement();
          element = getCanonicalElement(element);
          if (Objects.equal(element, fDartElement)) {
            sameNodes.add(node);
          }
          return super.visitSimpleIdentifier(node);
        }
      });

      //TODO: copied from LinkedNamesAssistProposal#apply(..):
      // sort for iteration order, starting with the node @ offset
      Collections.sort(sameNodes, new Comparator<ASTNode>() {
        @Override
        public int compare(ASTNode o1, ASTNode o2) {
          return rank(o1) - rank(o2);
        }

        /**
         * Returns the absolute rank of an <code>ASTNode</code>. Nodes preceding <code>pos</code>
         * are ranked last.
         * 
         * @param node the node to compute the rank for
         * @return the rank of the node with respect to the invocation offset
         */
        private int rank(ASTNode node) {
          int relativeRank = node.getOffset() + node.getLength() - pos;
          if (relativeRank < 0) {
            return Integer.MAX_VALUE + relativeRank;
          } else {
            return relativeRank;
          }
        }
      });
      for (int i = 0; i < sameNodes.size(); i++) {
        ASTNode elem = sameNodes.get(i);
        LinkedPosition linkedPosition = new LinkedPosition(
            document,
            elem.getOffset(),
            elem.getLength(),
            i);
        if (i == 0) {
          fNamePosition = linkedPosition;
        }
        fLinkedPositionGroup.addPosition(linkedPosition);
      }

      fLinkedModeModel = new LinkedModeModel();
      fLinkedModeModel.addGroup(fLinkedPositionGroup);
      fLinkedModeModel.forceInstall();
      fLinkedModeModel.addLinkingListener(new EditorHighlightingSynchronizer(fEditor));
      fLinkedModeModel.addLinkingListener(new EditorSynchronizer());

      LinkedModeUI ui = new EditorLinkedModeUI(fLinkedModeModel, viewer);
      ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
      ui.setExitPolicy(new ExitPolicy(document));
      ui.enter();

      viewer.setSelectedRange(fOriginalSelection.x, fOriginalSelection.y); // by default, full word is selected; restore original selection

      if (viewer instanceof IEditingSupportRegistry) {
        IEditingSupportRegistry registry = (IEditingSupportRegistry) viewer;
        registry.register(fFocusEditingSupport);
      }

      openSecondaryPopup();
//			startAnimation();
      fgActiveLinkedMode = this;

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  public void startFullDialog() {
    cancel();

    try {
      String newName = fNamePosition.getContent();
      RenameSupport renameSupport = undoAndCreateRenameSupport(newName);
      if (renameSupport != null) {
        renameSupport.openDialog(fEditor.getSite().getShell());
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  void doRename(boolean showPreview) {
    cancel();

    Image image = null;
    Label label = null;

    fShowPreview |= showPreview;
    try {
      ISourceViewer viewer = fEditor.getViewer();
      if (viewer instanceof SourceViewer) {
        SourceViewer sourceViewer = (SourceViewer) viewer;
        Control viewerControl = sourceViewer.getControl();
        if (viewerControl instanceof Composite) {
          Composite composite = (Composite) viewerControl;
          Display display = composite.getDisplay();

          // Flush pending redraw requests:
          while (!display.isDisposed() && display.readAndDispatch()) {
          }

          // Copy editor area:
          GC gc = new GC(composite);
          Point size;
          try {
            size = composite.getSize();
            image = new Image(gc.getDevice(), size.x, size.y);
            gc.copyArea(image, 0, 0);
          } finally {
            gc.dispose();
            gc = null;
          }

          // Persist editor area while executing refactoring:
          label = new Label(composite, SWT.NONE);
          label.setImage(image);
          label.setBounds(0, 0, size.x, size.y);
          label.moveAbove(null);
        }
      }

      String newName = fNamePosition.getContent();
      if (fOriginalName.equals(newName)) {
        return;
      }
      RenameSupport renameSupport = undoAndCreateRenameSupport(newName);
      if (renameSupport == null) {
        return;
      }

      Shell shell = fEditor.getSite().getShell();
      if (renameSupport.hasUnresolvedNameReferences()) {
        fShowPreview = true;
      }
      boolean executed;
      if (fShowPreview) { // could have been updated by undoAndCreateRenameSupport(..)
        executed = renameSupport.openDialog(shell, true);
      } else {
        renameSupport.perform(shell, fEditor.getSite().getWorkbenchWindow());
        executed = true;
      }
      if (executed) {
        restoreFullSelection();
      }
    } catch (CoreException ex) {
      DartToolsPlugin.log(ex);
    } catch (InterruptedException ex) {
      // canceling is OK -> redo text changes in that case?
    } catch (InvocationTargetException ex) {
      DartToolsPlugin.log(ex);
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    } finally {
      if (label != null) {
        label.dispose();
      }
      if (image != null) {
        image.dispose();
      }
    }
  }

  private void linkedModeLeft() {
    fgActiveLinkedMode = null;
    if (fInfoPopup != null) {
      fInfoPopup.close();
    }

    ISourceViewer viewer = fEditor.getViewer();
    if (viewer instanceof IEditingSupportRegistry) {
      IEditingSupportRegistry registry = (IEditingSupportRegistry) viewer;
      registry.unregister(fFocusEditingSupport);
    }
  }

  private void openSecondaryPopup() {
    fInfoPopup = new RenameInformationPopup(fEditor, this);
    fInfoPopup.open();
  }

  /**
   * Uses {@link AssistContext} form {@link DartEditor} to prepare {@link #fDartElement}.
   */
  private void prepareElement() {
    fDartElement = null;
    context = fEditor.getAssistContext();
    if (context == null) {
      return;
    }
    ASTNode selectedNode = context.getCoveredNode();
    if (!(selectedNode instanceof SimpleIdentifier)) {
      return;
    }
    nameNode = (SimpleIdentifier) selectedNode;
    fDartElement = nameNode.getElement();
    fDartElement = getCanonicalElement(fDartElement);
  }

  private void restoreFullSelection() {
    if (fOriginalSelection.y != 0) {
      int originalOffset = fOriginalSelection.x;
      LinkedPosition[] positions = fLinkedPositionGroup.getPositions();
      for (int i = 0; i < positions.length; i++) {
        LinkedPosition position = positions[i];
        if (!position.isDeleted() && position.includes(originalOffset)) {
          fEditor.getViewer().setSelectedRange(position.offset, position.length);
          return;
        }
      }
    }
  }

  private RenameSupport undoAndCreateRenameSupport(String newName) throws CoreException {
    // Assumption: the linked mode model should be shut down by now.

    final ISourceViewer viewer = fEditor.getViewer();

    try {
      if (!fOriginalName.equals(newName)) {
        IWorkbenchWindow workbenchWindow = fEditor.getSite().getWorkbenchWindow();
        // undo
        workbenchWindow.run(false, true, new IRunnableWithProgress() {
          @Override
          public void run(IProgressMonitor monitor) throws InvocationTargetException,
              InterruptedException {
            if (viewer instanceof ITextViewerExtension6) {
              IUndoManager undoManager = ((ITextViewerExtension6) viewer).getUndoManager();
              if (undoManager instanceof IUndoManagerExtension) {
                IUndoManagerExtension undoManagerExtension = (IUndoManagerExtension) undoManager;
                IUndoContext undoContext = undoManagerExtension.getUndoContext();
                IOperationHistory operationHistory = OperationHistoryFactory.getOperationHistory();
                while (undoManager.undoable()) {
                  if (fStartingUndoOperation != null
                      && fStartingUndoOperation.equals(operationHistory.getUndoOperation(undoContext))) {
                    return;
                  }
                  undoManager.undo();
                }
              }
            }
          }
        });
        // wait for analysis
        IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
        progressService.busyCursorWhile(new IRunnableWithProgress() {
          @Override
          public void run(IProgressMonitor monitor) throws InvocationTargetException,
              InterruptedException {
            monitor.beginTask("Waiting for background analysis...", IProgressMonitor.UNKNOWN);
            try {
              RefactoringUtils.waitResolvedCompilationUnit(fEditor, monitor);
              RefactoringUtils.waitReadyForRefactoring(monitor);
            } finally {
              monitor.done();
            }
          }
        });
        // by some reason "busyCursorWhile" looses focus, so restore it
        fEditor.setFocus();
        // get new Element, after background build finished
        prepareElement();
        if (fDartElement == null) {
          return null;
        }
      }
    } catch (InvocationTargetException e) {
      throw new CoreException(new Status(
          IStatus.ERROR,
          DartToolsPlugin.getPluginId(),
          ReorgMessages.RenameLinkedMode_error_saving_editor,
          e));
    } catch (InterruptedException e) {
      // canceling is OK
      return null;
    }

    viewer.setSelectedRange(fOriginalSelection.x, fOriginalSelection.y);

    if (newName.length() == 0) {
      return null;
    }

    return RenameSupport.create(fDartElement, newName);
  }
}
