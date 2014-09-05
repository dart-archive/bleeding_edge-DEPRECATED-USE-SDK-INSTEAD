/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui;

import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.RefactorActionGroup_OLD;
import com.google.dart.tools.ui.internal.text.editor.saveactions.RemoveTrailingWhitespaceAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.emf.common.command.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IBlockTextSelection;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISelectionValidator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.ITextEditorHelpContextIds;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.help.IWorkbenchHelpSystem;
import org.eclipse.ui.internal.editors.quickdiff.RestoreAction;
import org.eclipse.ui.internal.editors.quickdiff.RevertBlockAction;
import org.eclipse.ui.internal.editors.quickdiff.RevertLineAction;
import org.eclipse.ui.internal.editors.quickdiff.RevertSelectionAction;
import org.eclipse.ui.internal.texteditor.LineNumberColumn;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.ui.texteditor.ITextEditorExtension4;
import org.eclipse.ui.texteditor.ITextEditorExtension5;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.document.IDocumentCharsetDetector;
import org.eclipse.wst.sse.core.internal.encoding.EncodingMemento;
import org.eclipse.wst.sse.core.internal.provisional.IModelStateListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.text.IExecutionDelegatable;
import org.eclipse.wst.sse.core.internal.undo.IStructuredTextUndoManager;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.ExtendedEditorActionBuilder;
import org.eclipse.wst.sse.ui.internal.ExtendedEditorDropTargetAdapter;
import org.eclipse.wst.sse.ui.internal.IExtendedContributor;
import org.eclipse.wst.sse.ui.internal.IModelProvider;
import org.eclipse.wst.sse.ui.internal.IPopupMenuContributor;
import org.eclipse.wst.sse.ui.internal.Logger;
import org.eclipse.wst.sse.ui.internal.ReadOnlyAwareDropTargetAdapter;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.StorageModelProvider;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.UnknownContentTypeDialog;
import org.eclipse.wst.sse.ui.internal.actions.ActionDefinitionIds;
import org.eclipse.wst.sse.ui.internal.actions.StructuredTextEditorActionConstants;
import org.eclipse.wst.sse.ui.internal.contentoutline.ConfigurableContentOutlinePage;
import org.eclipse.wst.sse.ui.internal.debug.BreakpointRulerAction;
import org.eclipse.wst.sse.ui.internal.debug.EditBreakpointAction;
import org.eclipse.wst.sse.ui.internal.debug.ManageBreakpointAction;
import org.eclipse.wst.sse.ui.internal.debug.ToggleBreakpointAction;
import org.eclipse.wst.sse.ui.internal.debug.ToggleBreakpointsTarget;
import org.eclipse.wst.sse.ui.internal.derived.HTMLTextPresenter;
import org.eclipse.wst.sse.ui.internal.editor.EditorModelUtil;
import org.eclipse.wst.sse.ui.internal.editor.IHelpContextIds;
import org.eclipse.wst.sse.ui.internal.editor.SelectionConvertor;
import org.eclipse.wst.sse.ui.internal.editor.StructuredModelDocumentProvider;
import org.eclipse.wst.sse.ui.internal.extension.BreakpointProviderBuilder;
import org.eclipse.wst.sse.ui.internal.handlers.AddBlockCommentHandler;
import org.eclipse.wst.sse.ui.internal.handlers.RemoveBlockCommentHandler;
import org.eclipse.wst.sse.ui.internal.handlers.ToggleLineCommentHandler;
import org.eclipse.wst.sse.ui.internal.hyperlink.OpenHyperlinkAction;
import org.eclipse.wst.sse.ui.internal.preferences.EditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.projection.AbstractStructuredFoldingStrategy;
import org.eclipse.wst.sse.ui.internal.properties.ConfigurablePropertySheetPage;
import org.eclipse.wst.sse.ui.internal.properties.ShowPropertiesAction;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ConfigurationPointCalculator;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.NullSourceEditingTextTools;
import org.eclipse.wst.sse.ui.internal.provisional.preferences.CommonEditorPreferenceNames;
import org.eclipse.wst.sse.ui.internal.quickoutline.QuickOutlineHandler;
import org.eclipse.wst.sse.ui.internal.quickoutline.QuickOutlinePopupDialog;
import org.eclipse.wst.sse.ui.internal.reconcile.DirtyRegionProcessor;
import org.eclipse.wst.sse.ui.internal.reconcile.DocumentRegionProcessor;
import org.eclipse.wst.sse.ui.internal.selection.SelectionHistory;
import org.eclipse.wst.sse.ui.internal.style.SemanticHighlightingManager;
import org.eclipse.wst.sse.ui.internal.text.DocumentRegionEdgeMatcher;
import org.eclipse.wst.sse.ui.internal.text.SourceInfoProvider;
import org.eclipse.wst.sse.ui.internal.util.Assert;
import org.eclipse.wst.sse.ui.internal.util.EditorUtility;
import org.eclipse.wst.sse.ui.quickoutline.AbstractQuickOutlineConfiguration;
import org.eclipse.wst.sse.ui.reconcile.ISourceReconcilingListener;
import org.eclipse.wst.sse.ui.typing.AbstractCharacterPairInserter;
import org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration;
import org.eclipse.wst.sse.ui.views.properties.PropertySheetConfiguration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A Text Editor for editing structured models and structured documents.
 * <p>
 * This class is not meant to be subclassed.
 * </p>
 * <p>
 * New content types may associate source viewer, content outline, and property sheet configurations
 * to extend the existing functionality.
 * </p>
 * 
 * @see org.eclipse.wst.sse.ui.StructuredTextViewerConfiguration
 * @see org.eclipse.wst.sse.ui.views.contentoutline.ContentOutlineConfiguration
 * @see org.eclipse.wst.sse.ui.views.properties.PropertySheetConfiguration
 * @since 1.0
 */
public class StructuredTextEditor extends TextEditor {
  class TimeOutExpired extends TimerTask {
    @Override
    public void run() {
      final byte[] result = new byte[1]; // Did the busy state end successfully?
      getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          if (getDisplay() != null && !getDisplay().isDisposed()) {
            endBusyStateInternal(result);
          }
        }
      });
      if (result[0] == 1) {
        fBusyTimer.cancel();
      }
    }

  }

  private static final class AccessChecker extends SecurityManager {
    @Override
    public Class[] getClassContext() {
      return super.getClassContext();
    }
  }

  /**
   * Representation of a character pairing that includes its priority based on its content type and
   * how close it is to the content type of the file in the editor.
   */
  private class CharacterPairing implements Comparable {
    int priority;
    AbstractCharacterPairInserter inserter;
    Set partitions;

    @Override
    public int compareTo(Object o) {
      if (o == this) {
        return 0;
      }
      return this.priority - ((CharacterPairing) o).priority;
    }
  }

  private class CharacterPairListener implements VerifyKeyListener {
    private CharacterPairing[] fInserters = new CharacterPairing[0];
    private ICompletionListener fCompletionListener;
    private boolean fIsCompleting = false;

    public void installCompletionListener() {
      ISourceViewer viewer = getSourceViewer();
      if (viewer instanceof StructuredTextViewer) {
        fCompletionListener = new ICompletionListener() {

          @Override
          public void assistSessionEnded(ContentAssistEvent event) {
            fIsCompleting = false;
          }

          @Override
          public void assistSessionStarted(ContentAssistEvent event) {
            fIsCompleting = true;
          }

          @Override
          public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
          }

        };
        ContentAssistantFacade facade = ((StructuredTextViewer) viewer).getContentAssistFacade();
        if (facade != null) {
          facade.addCompletionListener(fCompletionListener);
        }
      }
    }

    @Override
    public void verifyKey(final VerifyEvent event) {
      if (!event.doit || getInsertMode() != SMART_INSERT || fIsCompleting
          || isBlockSelectionModeEnabled() && isMultilineSelection()) {
        return;
      }
      final boolean[] paired = {false};
      for (int i = 0; i < fInserters.length; i++) {
        final CharacterPairing pairing = fInserters[i];
        // use a SafeRunner -- this is a critical function (typing)
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable exception) {
            // rely on default logging
          }

          @Override
          public void run() throws Exception {
            final AbstractCharacterPairInserter inserter = pairing.inserter;
            if (inserter.hasPair(event.character)) {
              if (pair(event, inserter, pairing.partitions)) {
                paired[0] = true;
              }
            }
          }
        });
        if (paired[0]) {
          return;
        }
      }
    }

    /**
     * Add the pairing to the list of inserters
     * 
     * @param pairing
     */
    void addInserter(CharacterPairing pairing) {
      List pairings = new ArrayList(Arrays.asList(fInserters));
      pairings.add(pairing);
      fInserters = (CharacterPairing[]) pairings.toArray(new CharacterPairing[pairings.size()]);
    }

    /**
     * Perform cleanup on the character pair inserters
     */
    void dispose() {
      ISourceViewer viewer = getSourceViewer();
      if (viewer instanceof StructuredTextViewer) {
        ContentAssistantFacade facade = ((StructuredTextViewer) viewer).getContentAssistFacade();
        if (facade != null) {
          facade.removeCompletionListener(fCompletionListener);
        }
      }

      for (int i = 0; i < fInserters.length; i++) {
        final AbstractCharacterPairInserter inserter = fInserters[i].inserter;
        SafeRunner.run(new ISafeRunnable() {
          @Override
          public void handleException(Throwable exception) {
            // rely on default logging
          }

          @Override
          public void run() throws Exception {
            inserter.dispose();
          }
        });
      }
    }

    void prioritize() {
      Arrays.sort(fInserters);
    }

    private boolean isMultilineSelection() {
      ISelection selection = getSelectionProvider().getSelection();
      if (selection instanceof ITextSelection) {
        ITextSelection ts = (ITextSelection) selection;
        return ts.getStartLine() != ts.getEndLine();
      }
      return false;
    }

    private boolean pair(VerifyEvent event, AbstractCharacterPairInserter inserter, Set partitions) {
      final ISourceViewer viewer = getSourceViewer();
      final IDocument document = getSourceViewer().getDocument();
      if (document != null) {
        try {
          final Point selection = viewer.getSelectedRange();
          final int offset = selection.x;
          final ITypedRegion partition = document.getPartition(offset);
          if (partitions.contains(partition.getType())) {
            // Don't modify if the editor input cannot be changed
            if (!validateEditorInputState()) {
              return false;
            }
            event.doit = !inserter.pair(viewer, event.character);
            return true;
          }
        } catch (BadLocationException e) {
        }
      }
      return false;
    }
  }

  private class ConfigurationAndTarget {
    private String fTargetId;
    private StructuredTextViewerConfiguration fConfiguration;

    public ConfigurationAndTarget(String targetId, StructuredTextViewerConfiguration config) {
      fTargetId = targetId;
      fConfiguration = config;
    }

    public StructuredTextViewerConfiguration getConfiguration() {
      return fConfiguration;
    }

    public String getTargetId() {
      return fTargetId;
    }
  }

  private class GotoMatchingBracketHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
      gotoMatchingBracket();
      return null;
    }
  }

  private class InternalModelStateListener implements IModelStateListener {
    @Override
    public void modelAboutToBeChanged(IStructuredModel model) {
      if (getTextViewer() != null) {
        // getTextViewer().setRedraw(false);
      }
    }

    @Override
    public void modelAboutToBeReinitialized(IStructuredModel structuredModel) {
      if (getTextViewer() != null) {
        // getTextViewer().setRedraw(false);
        getTextViewer().unconfigure();
        setStatusLineMessage(null);
      }
    }

    @Override
    public void modelChanged(IStructuredModel model) {
      if (getSourceViewer() != null) {
        // getTextViewer().setRedraw(true);
        // Since the model can be changed on a background
        // thread, we will update menus on display thread,
        // if we are not already on display thread,
        // and if there is not an update already pending.
        // (we can get lots of 'modelChanged' events in rapid
        // succession, so only need to do one.
        if (!fUpdateMenuTextPending) {
          runOnDisplayThreadIfNeededed(new Runnable() {
            @Override
            public void run() {
              updateMenuText();
              fUpdateMenuTextPending = false;
            }
          });
        }

      }
    }

    @Override
    public void modelDirtyStateChanged(IStructuredModel model, boolean isDirty) {
      // do nothing
    }

    @Override
    public void modelReinitialized(IStructuredModel structuredModel) {
      try {
        if (getSourceViewer() != null) {
          SourceViewerConfiguration cfg = getSourceViewerConfiguration();
          getSourceViewer().configure(cfg);
        }
      } catch (Exception e) {
        // https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=1166
        // investigate each error case post beta
        Logger.logException("problem trying to configure after model change", e); //$NON-NLS-1$
      } finally {
        // so we don't freeze workbench (eg. during page language or
        // content type change)
        ((ITextViewerExtension) getSourceViewer()).setRedraw(true);

        IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite().getService(
            IWorkbenchSiteProgressService.class);
        if (service != null) {
          service.warnOfContentChange();
        }
      }
    }

    // Note: this one should probably be used to
    // control viewer
    // instead of viewer having its own listener
    @Override
    public void modelResourceDeleted(IStructuredModel model) {
      // do nothing
    }

    @Override
    public void modelResourceMoved(IStructuredModel originalmodel, IStructuredModel movedmodel) {
      // do nothing
    }

    /**
     * This 'Runnable' should be very brief, and should not "call out" to other code especially if
     * it depends on the state of the model.
     * 
     * @param r
     */
    private void runOnDisplayThreadIfNeededed(Runnable r) {
      // if there is no Display at all (that is, running headless),
      // or if we are already running on the display thread, then
      // simply execute the runnable.
      if (getDisplay() == null || (Thread.currentThread() == getDisplay().getThread())) {
        r.run();
      } else {
        // otherwise force the runnable to run on the display thread.
        getDisplay().asyncExec(r);
      }
    }
  }

  /**
   * Listens to double-click and selection from the outline page
   */
  private class OutlinePageListener implements IDoubleClickListener, ISelectionChangedListener {
    @Override
    public void doubleClick(DoubleClickEvent event) {
      if (event.getSelection().isEmpty()) {
        return;
      }

      int start = -1;
      int length = 0;
      if (event.getSelection() instanceof IStructuredSelection) {
        ISelection currentSelection = getSelectionProvider().getSelection();
        if (currentSelection instanceof IStructuredSelection) {
          Object current = ((IStructuredSelection) currentSelection).toArray();
          Object newSelection = ((IStructuredSelection) event.getSelection()).toArray();
          if (!current.equals(newSelection)) {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            Object o = selection.getFirstElement();
            if (o instanceof IndexedRegion) {
              start = ((IndexedRegion) o).getStartOffset();
              length = ((IndexedRegion) o).getEndOffset() - start;
            } else if (o instanceof ITextRegion) {
              start = ((ITextRegion) o).getStart();
              length = ((ITextRegion) o).getEnd() - start;
            } else if (o instanceof IRegion) {
              start = ((ITextRegion) o).getStart();
              length = ((ITextRegion) o).getLength();
            }
          }
        }
      } else if (event.getSelection() instanceof ITextSelection) {
        start = ((ITextSelection) event.getSelection()).getOffset();
        length = ((ITextSelection) event.getSelection()).getLength();
      }
      if (start > -1) {
        getSourceViewer().setRangeIndication(start, length, false);
        selectAndReveal(start, length);
      }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      /*
       * Do not allow selection from other parts to affect selection in the text widget if it has
       * focus, or if we're still firing a change of selection. Selection events "bouncing" off of
       * other parts are all that we can receive if we have focus (since we forwarded our selection
       * to the service just a moment ago), and only the user should affect selection if we have
       * focus.
       */

      /* The isFiringSelection check only works if a selection listener */
      if (event.getSelection().isEmpty() || fStructuredSelectionProvider.isFiringSelection()) {
        return;
      }

      if (getSourceViewer() != null && getSourceViewer().getTextWidget() != null
          && !getSourceViewer().getTextWidget().isDisposed()
          && !getSourceViewer().getTextWidget().isFocusControl()) {
        int start = -1;
        int length = 0;
        if (event.getSelection() instanceof IStructuredSelection) {
          ISelection current = getSelectionProvider().getSelection();
          if (current instanceof IStructuredSelection) {
            Object[] currentSelection = ((IStructuredSelection) current).toArray();
            Object[] newSelection = ((IStructuredSelection) event.getSelection()).toArray();
            if (!Arrays.equals(currentSelection, newSelection)) {
              if (newSelection.length > 0) {
                /*
                 * No ordering is guaranteed for multiple selection
                 */
                Object o = newSelection[0];
                if (o instanceof IndexedRegion) {
                  start = ((IndexedRegion) o).getStartOffset();
                  int end = ((IndexedRegion) o).getEndOffset();
                  if (newSelection.length > 1) {
                    for (int i = 1; i < newSelection.length; i++) {
                      start = Math.min(start, ((IndexedRegion) newSelection[i]).getStartOffset());
                      end = Math.max(end, ((IndexedRegion) newSelection[i]).getEndOffset());
                    }
                    length = end - start;
                  }
                } else if (o instanceof ITextRegion) {
                  start = ((ITextRegion) o).getStart();
                  int end = ((ITextRegion) o).getEnd();
                  if (newSelection.length > 1) {
                    for (int i = 1; i < newSelection.length; i++) {
                      start = Math.min(start, ((ITextRegion) newSelection[i]).getStart());
                      end = Math.max(end, ((ITextRegion) newSelection[i]).getEnd());
                    }
                    length = end - start;
                  }
                } else if (o instanceof IRegion) {
                  start = ((IRegion) o).getOffset();
                  int end = start + ((IRegion) o).getLength();
                  if (newSelection.length > 1) {
                    for (int i = 1; i < newSelection.length; i++) {
                      start = Math.min(start, ((IRegion) newSelection[i]).getOffset());
                      end = Math.max(end, ((IRegion) newSelection[i]).getOffset()
                          + ((IRegion) newSelection[i]).getLength());
                    }
                    length = end - start;
                  }
                }
              }
            }
          }
        } else if (event.getSelection() instanceof ITextSelection) {
          start = ((ITextSelection) event.getSelection()).getOffset();
        }
        if (start > -1) {
          updateRangeIndication(event.getSelection());
          selectAndReveal(start, length);
        }
      }
    }
  }

  private class PartListener implements IPartListener {

    private ITextEditor fEditor;

    public PartListener(ITextEditor editor) {
      fEditor = editor;
    }

    @Override
    public void partActivated(IWorkbenchPart part) {
      if (part.getAdapter(ITextEditor.class) == fEditor) {
        SourceViewerConfiguration sourceViewerConfiguration = getSourceViewerConfiguration();
        /*
         * Guard against possible tight timing between part creation and viewer configuration
         */
        if (sourceViewerConfiguration != null) {
          IReconciler reconciler = sourceViewerConfiguration.getReconciler(getSourceViewer());
          if (reconciler instanceof DocumentRegionProcessor) {
            ((DocumentRegionProcessor) reconciler).forceReconciling();
          }
        }
      }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
    }

    @Override
    public void partOpened(IWorkbenchPart part) {
    }

  }

  private class ShowInTargetListAdapter implements IShowInTargetList {
    /**
     * Array of ID Strings that define the default show in targets for this editor.
     * 
     * @see org.eclipse.ui.part.IShowInTargetList#getShowInTargetIds()
     * @return the array of ID Strings that define the default show in targets for this editor.
     */
    @Override
    public String[] getShowInTargetIds() {
      return fShowInTargetIds;
    }
  }

  /**
   * A post selection provider that wraps the provider implemented in AbstractTextEditor to provide
   * a StructuredTextSelection to post selection listeners. Listens to selection changes from the
   * source viewer.
   */
  private static class StructuredSelectionProvider implements IPostSelectionProvider,
      ISelectionValidator {
    /**
     * A "hybrid" text and structured selection class containing the text selection and a list of
     * selected model objects. The determination of which model objects matches the text selection
     * is responsibility of the StructuredSelectionProvider which created this selection object.
     */
    private static class StructuredTextSelection extends TextSelection implements
        IStructuredSelection {
      private Object[] selectedStructured;

      StructuredTextSelection(IDocument document, int offset, int length, Object[] selectedObjects) {
        super(document, offset, length);
        selectedStructured = selectedObjects;
      }

      StructuredTextSelection(IDocument document, ITextSelection selection, Object[] selectedObjects) {
        this(document, selection.getOffset(), selection.getLength(), selectedObjects);
      }

      @Override
      public Object getFirstElement() {
        Object[] selectedStructures = getSelectedStructures();
        return selectedStructures.length > 0 ? selectedStructures[0] : null;
      }

      @Override
      public boolean isEmpty() {
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=191327
        return super.isEmpty() || getSelectedStructures().length == 0;
      }

      @Override
      public Iterator iterator() {
        return toList().iterator();
      }

      @Override
      public int size() {
        return (selectedStructured != null) ? selectedStructured.length : 0;
      }

      @Override
      public Object[] toArray() {
        return getSelectedStructures();
      }

      @Override
      public List toList() {
        return Arrays.asList(getSelectedStructures());
      }

      @Override
      public String toString() {
        return getOffset() + ":" + getLength() + "@" + getSelectedStructures(); //$NON-NLS-1$ //$NON-NLS-2$
      }

      private Object[] getSelectedStructures() {
        return (selectedStructured != null) ? selectedStructured : new Object[0];
      }
    }

    private ISelectionProvider fParentProvider = null;
    private boolean isFiringSelection = false;
    private ListenerList listeners = new ListenerList();
    private ListenerList postListeners = new ListenerList();
    private ISelection fLastSelection = null;
    private ISelectionProvider fLastSelectionProvider = null;
    private SelectionChangedEvent fLastUpdatedSelectionChangedEvent = null;
    private StructuredTextEditor fEditor;
    /*
     * Responsible for finding the selected objects within a text selection. Set/reset by the
     * StructuredTextEditor based on a per-model adapter on input.
     */
    SelectionConvertor selectionConvertor = new SelectionConvertor();

    StructuredSelectionProvider(ISelectionProvider parentProvider,
        StructuredTextEditor structuredTextEditor) {
      fParentProvider = parentProvider;
      fEditor = structuredTextEditor;
      fParentProvider.addSelectionChangedListener(new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          handleSelectionChanged(event);
        }
      });
      if (fParentProvider instanceof IPostSelectionProvider) {
        ((IPostSelectionProvider) fParentProvider).addPostSelectionChangedListener(new ISelectionChangedListener() {
          @Override
          public void selectionChanged(SelectionChangedEvent event) {
            handlePostSelectionChanged(event);
          }
        });
      }
    }

    @Override
    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
      postListeners.add(listener);
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
      listeners.add(listener);
    }

    public void dispose() {
      fEditor = null;
      listeners.clear();
      postListeners.clear();
      selectionConvertor = null;
    }

    @Override
    public ISelection getSelection() {
      fLastSelection = null;
      fLastSelectionProvider = null;
      fLastUpdatedSelectionChangedEvent = null;

      /*
       * When a client explicitly asks for selection, provide the hybrid result.
       */
      ISelection selection = getParentProvider().getSelection();
      if (!(selection instanceof IStructuredSelection) && selection instanceof ITextSelection) {
        IStructuredModel structuredModel = null;
        StructuredTextEditor localEditor = getStructuredTextEditor();
        if (localEditor != null) {
          structuredModel = localEditor.getInternalModel();
          if (structuredModel != null) {
            if (localEditor.isBlockSelectionModeEnabled()) {
              /*
               * Block selection handling - find the overlapping objects on each line, keeping in
               * mind that the selected block may not overlap actual lines or columns of the
               * document. IBlockTextSelection.getRegions() should handle that for us...
               */
              IBlockTextSelection blockSelection = (IBlockTextSelection) selection;
              IRegion[] regions = blockSelection.getRegions();
              Set blockObjects = new LinkedHashSet();
              for (int i = 0; i < regions.length; i++) {
                Object[] objects = selectionConvertor.getElements(structuredModel,
                    regions[i].getOffset(), regions[i].getLength());
                for (int j = 0; j < objects.length; j++) {
                  blockObjects.add(objects[j]);
                }
              }
              selection = new StructuredTextSelection(getDocument(), (ITextSelection) selection,
                  blockObjects.toArray());
            } else {
              int start = ((ITextSelection) selection).getOffset();
              int end = start + ((ITextSelection) selection).getLength();
              selection = new StructuredTextSelection(getDocument(), (ITextSelection) selection,
                  selectionConvertor.getElements(structuredModel, start, end));
            }
          }
        }
        if (selection == null) {
          selection = new StructuredTextSelection(getDocument(), (ITextSelection) selection,
              new Object[0]);
        }
      }

      return selection;
    }

    @Override
    public boolean isValid(ISelection selection) {
      // ISSUE: is not clear default behavior should be true?
      // But not clear is this default would apply for our editor.
      boolean result = true;
      // if editor is "gone", can not be valid
      StructuredTextEditor e = getStructuredTextEditor();
      if (e == null || e.fEditorDisposed) {
        result = false;
      }
      // else defer to parent
      else if (getParentProvider() instanceof ISelectionValidator) {
        result = ((ISelectionValidator) getParentProvider()).isValid(selection);
      }
      return result;
    }

    @Override
    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
      postListeners.remove(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
      listeners.remove(listener);
    }

    @Override
    public void setSelection(ISelection selection) {
      if (isFiringSelection()) {
        return;
      }

      fLastSelection = null;
      fLastSelectionProvider = null;
      fLastUpdatedSelectionChangedEvent = null;

      ISelection textSelection = updateSelection(selection);
      getParentProvider().setSelection(textSelection);
      StructuredTextEditor localEditor = getStructuredTextEditor();
      if (localEditor != null) {
        localEditor.updateRangeIndication(textSelection);
      }
    }

    IDocument getDocument() {
      return fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
    }

    void handlePostSelectionChanged(SelectionChangedEvent event) {
      SelectionChangedEvent updatedEvent = null;
      if (fLastSelection == event.getSelection()
          && fLastSelectionProvider == event.getSelectionProvider()) {
        updatedEvent = fLastUpdatedSelectionChangedEvent;
      } else {
        updatedEvent = updateEvent(event);
      }
      // only update the range indicator on post selection
      StructuredTextEditor localEditor = fEditor;

      if (localEditor != null) {
        localEditor.updateRangeIndication(updatedEvent.getSelection());
        fireSelectionChanged(updatedEvent, postListeners);
      }
    }

    void handleSelectionChanged(SelectionChangedEvent event) {
      SelectionChangedEvent updatedEvent = event;
      if (fLastSelection != event.getSelection()
          || fLastSelectionProvider != event.getSelectionProvider()) {
        fLastSelection = event.getSelection();
        fLastSelectionProvider = event.getSelectionProvider();
        fLastUpdatedSelectionChangedEvent = updatedEvent = updateEvent(event);
      }
      fireSelectionChanged(updatedEvent, listeners);
    }

    boolean isFiringSelection() {
      return isFiringSelection;
    }

    private void fireSelectionChanged(final SelectionChangedEvent event, ListenerList listenerList) {
      Object[] listeners = listenerList.getListeners();
      isFiringSelection = true;
      for (int i = 0; i < listeners.length; ++i) {
        final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
        SafeRunner.run(new SafeRunnable() {
          @Override
          public void run() {
            l.selectionChanged(event);
          }
        });
      }
      isFiringSelection = false;
    }

    private ISelectionProvider getParentProvider() {
      return fParentProvider;
    }

    private StructuredTextEditor getStructuredTextEditor() {
      return fEditor;
    }

    /**
     * Create a corresponding event that contains a StructuredTextselection
     * 
     * @param event
     * @return
     */
    private SelectionChangedEvent updateEvent(SelectionChangedEvent event) {
      ISelection selection = event.getSelection();
      if (selection instanceof ITextSelection && !(selection instanceof IStructuredSelection)) {
        IStructuredModel structuredModel = null;
        StructuredTextEditor localEditor = getStructuredTextEditor();
        if (localEditor != null) {
          structuredModel = localEditor.getInternalModel();
          if (structuredModel != null) {
            int start = ((ITextSelection) selection).getOffset();
            int end = start + ((ITextSelection) selection).getLength();
            selection = new StructuredTextSelection(getDocument(),
                (ITextSelection) event.getSelection(), selectionConvertor.getElements(
                    structuredModel, start, end));
          }
        }
        if (selection == null) {
          selection = new StructuredTextSelection(getDocument(),
              (ITextSelection) event.getSelection(), new Object[0]);
        }
      }
      SelectionChangedEvent newEvent = new SelectionChangedEvent(event.getSelectionProvider(),
          selection);
      return newEvent;
    }

    /**
     * Create a corresponding StructuredTextselection
     * 
     * @param selection
     * @return
     */
    private ISelection updateSelection(ISelection selection) {
      ISelection updated = selection;
      if (selection instanceof IStructuredSelection && !(selection instanceof ITextSelection)
          && !selection.isEmpty()) {
        Object[] selectedObjects = ((IStructuredSelection) selection).toArray();
        if (selectedObjects.length > 0) {
          int start = -1;
          int length = 0;

          // no ordering is guaranteed for multiple selection
          Object o = selectedObjects[0];
          if (o instanceof IndexedRegion) {
            start = ((IndexedRegion) o).getStartOffset();
            int end = ((IndexedRegion) o).getEndOffset();
            if (selectedObjects.length > 1) {
              for (int i = 1; i < selectedObjects.length; i++) {
                start = Math.min(start, ((IndexedRegion) selectedObjects[i]).getStartOffset());
                end = Math.max(end, ((IndexedRegion) selectedObjects[i]).getEndOffset());
              }
              length = end - start;
            }
          } else if (o instanceof ITextRegion) {
            start = ((ITextRegion) o).getStart();
            int end = ((ITextRegion) o).getEnd();
            if (selectedObjects.length > 1) {
              for (int i = 1; i < selectedObjects.length; i++) {
                start = Math.min(start, ((ITextRegion) selectedObjects[i]).getStart());
                end = Math.max(end, ((ITextRegion) selectedObjects[i]).getEnd());
              }
              length = end - start;
            }
          }

          if (start > -1) {
            updated = new StructuredTextSelection(getDocument(), start, length, selectedObjects);
          }
        }
      }
      return updated;
    }
  }

  /**
   * Not API. May be removed in the future.
   */
  protected final static char[] BRACKETS = {'{', '}', '(', ')', '[', ']'};
  private static final long BUSY_STATE_DELAY = 1000;
  /**
   * Not API. May be removed in the future.
   */
  protected static final String DOT = "."; //$NON-NLS-1$
  private static final String EDITOR_CONTEXT_MENU_ID = "org.eclipse.wst.sse.ui.StructuredTextEditor.EditorContext"; //$NON-NLS-1$

  private static final String EDITOR_CONTEXT_MENU_SUFFIX = ".source.EditorContext"; //$NON-NLS-1$
  /** Non-NLS strings */
  private static final String EDITOR_KEYBINDING_SCOPE_ID = "org.eclipse.wst.sse.ui.structuredTextEditorScope"; //$NON-NLS-1$

  /**
   * Not API. May be removed in the future.
   */
  public static final String GROUP_NAME_ADDITIONS = IWorkbenchActionConstants.MB_ADDITIONS; //$NON-NLS-1$
  private static final String REDO_ACTION_DESC = SSEUIMessages.Redo___0___UI_; //$NON-NLS-1$ = "Redo: {0}."
  private static final String REDO_ACTION_DESC_DEFAULT = SSEUIMessages.Redo_Text_Change__UI_; //$NON-NLS-1$ = "Redo Text Change."
  private static final String REDO_ACTION_TEXT = SSEUIMessages._Redo__0___Ctrl_Y_UI_; //$NON-NLS-1$ = "&Redo {0} @Ctrl+Y"
  private static final String REDO_ACTION_TEXT_DEFAULT = SSEUIMessages._Redo_Text_Change__Ctrl_Y_UI_; //$NON-NLS-1$ = "&Redo Text Change @Ctrl+Y"
  private static final String RULER_CONTEXT_MENU_ID = "org.eclipse.wst.sse.ui.StructuredTextEditor.RulerContext"; //$NON-NLS-1$

  private static final String RULER_CONTEXT_MENU_SUFFIX = ".source.RulerContext"; //$NON-NLS-1$
  private final static String UNDERSCORE = "_"; //$NON-NLS-1$

  /** Translatable strings */
  private static final String UNDO_ACTION_DESC = SSEUIMessages.Undo___0___UI_; //$NON-NLS-1$ = "Undo: {0}."
  private static final String UNDO_ACTION_DESC_DEFAULT = SSEUIMessages.Undo_Text_Change__UI_; //$NON-NLS-1$ = "Undo Text Change."
  private static final String UNDO_ACTION_TEXT = SSEUIMessages._Undo__0___Ctrl_Z_UI_; //$NON-NLS-1$ = "&Undo {0} @Ctrl+Z"
  private static final String UNDO_ACTION_TEXT_DEFAULT = SSEUIMessages._Undo_Text_Change__Ctrl_Z_UI_; //$NON-NLS-1$ = "&Undo Text Change @Ctrl+Z"

  private static boolean isCalledByOutline() {
    Class[] elements = new AccessChecker().getClassContext();
    return elements[4].equals(ContentOutline.class) || elements[5].equals(ContentOutline.class);
  }

  // development time/debug variables only
  private int adapterRequests;
  private long adapterTime;
  private boolean fBackgroundJobEnded;
  private boolean fBusyState;
  private Timer fBusyTimer;
  boolean fDirtyBeforeDocumentEvent = false;
  int validateEditCount = 0;
  private ExtendedEditorDropTargetAdapter fDropAdapter;
  private DropTarget fDropTarget;
  boolean fEditorDisposed = false;
  private IEditorPart fEditorPart;

  private InternalModelStateListener fInternalModelStateListener;
  private IContentOutlinePage fOutlinePage;
  private OutlinePageListener fOutlinePageListener = null;

  /** This editor's projection support */
  private ProjectionSupport fProjectionSupport;
  private IPropertySheetPage fPropertySheetPage;

  private ISourceReconcilingListener[] fReconcilingListeners = new ISourceReconcilingListener[0];
  private IPartListener fPartListener;

  /** The ruler context menu to be disposed. */
  private Menu fRulerContextMenu;

  /** The ruler context menu manager to be disposed. */
  private MenuManager fRulerContextMenuManager;
  String[] fShowInTargetIds = new String[] {
      IPageLayout.ID_RES_NAV, IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.ID_OUTLINE};
  private IAction fShowPropertiesAction = null;
  private IStructuredModel fStructuredModel;
  StructuredSelectionProvider fStructuredSelectionProvider = null;
  /** The text context menu to be disposed. */
  private Menu fTextContextMenu;
  /** The text context menu manager to be disposed. */
  private MenuManager fTextContextMenuManager;
  private String fViewerConfigurationTargetId;
  /** The selection history of the editor */
  private SelectionHistory fSelectionHistory;
  /** The information presenter. */
  private InformationPresenter fInformationPresenter;

  private boolean fUpdateMenuTextPending;
  /** The quick outline handler */
  private QuickOutlineHandler fOutlineHandler;
  private boolean shouldClose = false;
  private long startPerfTime;

  private boolean fisReleased;

  /**
   * The action group for folding.
   */
  private FoldingActionGroup fFoldingGroup;

  private ILabelProvider fStatusLineLabelProvider;

  private SemanticHighlightingManager fSemanticManager;

  private boolean fSelectionChangedFromGoto = false;

  private CharacterPairListener fPairInserter = new CharacterPairListener();

  private RemoveTrailingWhitespaceAction removeTrailingWhitespaceAction;

  /**
   * Creates a new Structured Text Editor.
   */
  public StructuredTextEditor() {
    super();
    initializeDocumentProvider(null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#close(boolean)
   */
  @Override
  public void close(final boolean save) {
    /*
     * Instead of us closing directly, we have to close with our containing (multipage) editor, if
     * it exists.
     */
    if (getSite() == null) {
      // if site hasn't been set yet, then we're not
      // completely open
      // so set a flag not to open
      shouldClose = true;
    } else {
      if (getEditorPart() != null) {
        Display display = getSite().getShell().getDisplay();
        display.asyncExec(new Runnable() {

          @Override
          public void run() {
            getSite().getPage().closeEditor(getEditorPart(), save);
          }
        });
      } else {
        super.close(save);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Use StructuredTextViewerConfiguration if a viewerconfiguration has not already been set. Also
   * initialize StructuredTextViewer.
   * </p>
   * 
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    IContextService contextService = (IContextService) getSite().getService(IContextService.class);
    if (contextService != null) {
      contextService.activateContext(EDITOR_KEYBINDING_SCOPE_ID);
    }

    if (getSourceViewerConfiguration() == null) {
      ConfigurationAndTarget cat = createSourceViewerConfiguration();
      fViewerConfigurationTargetId = cat.getTargetId();
      StructuredTextViewerConfiguration newViewerConfiguration = cat.getConfiguration();
      setSourceViewerConfiguration(newViewerConfiguration);
    }

    super.createPartControl(parent);

    // instead of calling setInput twice, use initializeSourceViewer() to
    // handle source viewer initialization previously handled by setInput
    initializeSourceViewer();

    // update editor context menu, vertical ruler context menu, infopop
    if (getInternalModel() != null) {
      updateEditorControlsForContentType(getInternalModel().getContentTypeIdentifier());
    } else {
      updateEditorControlsForContentType(null);
    }

    // used for Show Tooltip Description
    IInformationControlCreator informationControlCreator = new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell shell) {
        boolean cutDown = false;
        int style = cutDown ? SWT.NONE : (SWT.V_SCROLL | SWT.H_SCROLL);
        return new DefaultInformationControl(shell, SWT.RESIZE | SWT.TOOL, style,
            new HTMLTextPresenter(cutDown));
      }
    };

    fInformationPresenter = new InformationPresenter(informationControlCreator);
    fInformationPresenter.setSizeConstraints(60, 10, true, true);
    fInformationPresenter.install(getSourceViewer());
    addReconcilingListeners(getSourceViewerConfiguration(), getTextViewer());
    fPartListener = new PartListener(this);
    getSite().getWorkbenchWindow().getPartService().addPartListener(fPartListener);
    installSemanticHighlighting();
    if (fOutlineHandler != null) {
      IInformationPresenter presenter = configureOutlinePresenter(getSourceViewer(),
          getSourceViewerConfiguration());
      if (presenter != null) {
        presenter.install(getSourceViewer());
        fOutlineHandler.configure(presenter);
      }
    }
    installCharacterPairing();
    ISourceViewer viewer = getSourceViewer();
    if (viewer instanceof ITextViewerExtension) {
      ((ITextViewerExtension) viewer).appendVerifyKeyListener(fPairInserter);
      fPairInserter.installCompletionListener();
    }

    if (Platform.getProduct() != null) {
      String viewID = Platform.getProduct().getProperty("idPerspectiveHierarchyView"); //$NON-NLS-1$);
      if (viewID != null) {
        // make sure the specified view ID is known
        if (PlatformUI.getWorkbench().getViewRegistry().find(viewID) != null) {
          fShowInTargetIds = new String[] {
              viewID, IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.ID_RES_NAV,
              IPageLayout.ID_OUTLINE};
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IWorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    Logger.trace("Source Editor", "StructuredTextEditor::dispose entry"); //$NON-NLS-1$ //$NON-NLS-2$
    if (org.eclipse.wst.sse.core.internal.util.Debug.perfTestAdapterClassLoading) {
      System.out.println("Total calls to getAdapter: " + adapterRequests); //$NON-NLS-1$
      System.out.println("Total time in getAdapter: " + adapterTime); //$NON-NLS-1$
      System.out.println("Average time per call: " + (adapterTime / adapterRequests)); //$NON-NLS-1$
    }

    ISourceViewer viewer = getSourceViewer();
    if (viewer instanceof ITextViewerExtension) {
      ((ITextViewerExtension) viewer).removeVerifyKeyListener(fPairInserter);
    }

    // dispose of information presenter
    if (fInformationPresenter != null) {
      fInformationPresenter.dispose();
      fInformationPresenter = null;
    }

    if (fOutlineHandler != null) {
      fOutlineHandler.dispose();
    }
    // dispose of selection history
    if (fSelectionHistory != null) {
      fSelectionHistory.dispose();
      fSelectionHistory = null;
    }

    if (fProjectionSupport != null) {
      fProjectionSupport.dispose();
      fProjectionSupport = null;
    }

    if (fFoldingGroup != null) {
      fFoldingGroup.dispose();
      fFoldingGroup = null;
    }

    // dispose of menus that were being tracked
    if (fTextContextMenu != null) {
      fTextContextMenu.dispose();
    }
    if (fRulerContextMenu != null) {
      fRulerContextMenu.dispose();
      fRulerContextMenu = null;
    }
    if (fTextContextMenuManager != null) {
      fTextContextMenuManager.removeMenuListener(getContextMenuListener());
      fTextContextMenuManager.removeAll();
      fTextContextMenuManager.dispose();
    }
    if (fRulerContextMenuManager != null) {
      fRulerContextMenuManager.removeMenuListener(getContextMenuListener());
      fRulerContextMenuManager.removeAll();
      fRulerContextMenuManager.dispose();
    }

    // added this 2/20/2004 based on probe results --
    // seems should be handled by setModel(null), but
    // that's a more radical change.
    // and, technically speaking, should not be needed,
    // but makes a memory leak
    // less severe.
    if (fStructuredModel != null) {
      fStructuredModel.removeModelStateListener(getInternalModelStateListener());
    }

    // BUG155335 - if there was no document provider, there was nothing
    // added
    // to document, so nothing to remove
    if (getDocumentProvider() != null) {
      IDocument doc = getDocumentProvider().getDocument(getEditorInput());
      if (doc != null) {
        if (doc instanceof IExecutionDelegatable) {
          ((IExecutionDelegatable) doc).setExecutionDelegate(null);
        }
      }
    }

    // some things in the configuration need to clean
    // up after themselves
    if (fOutlinePage != null) {
      if (fOutlinePage instanceof ConfigurableContentOutlinePage && fOutlinePageListener != null) {
        ((ConfigurableContentOutlinePage) fOutlinePage).removeDoubleClickListener(fOutlinePageListener);
      }
      if (fOutlinePageListener != null) {
        fOutlinePage.removeSelectionChangedListener(fOutlinePageListener);
      }
      fOutlinePage = null;
    }

    fEditorDisposed = true;
    disposeModelDependentFields();

    if (fDropTarget != null) {
      fDropTarget.dispose();
    }

    if (fPartListener != null) {
      getSite().getWorkbenchWindow().getPartService().removePartListener(fPartListener);
      fPartListener = null;
    }

    uninstallSemanticHighlighting();

    if (fPairInserter != null) {
      fPairInserter.dispose();
    }

    setPreferenceStore(null);

    /*
     * Strictly speaking, but following null outs should not be needed, but in the event of a memory
     * leak, they make the memory leak less severe
     */
    fDropAdapter = null;
    fDropTarget = null;

    if (fStructuredSelectionProvider != null) {
      fStructuredSelectionProvider.dispose();
    }

    if (fStatusLineLabelProvider != null) {
      fStatusLineLabelProvider.dispose();
    }

    setStatusLineMessage(null);

    super.dispose();

    Logger.trace("Source Editor", "StructuredTextEditor::dispose exit"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#doRevertToSaved()
   */
  @Override
  public void doRevertToSaved() {
    super.doRevertToSaved();
    if (fOutlinePage != null && fOutlinePage instanceof IUpdate) {
      ((IUpdate) fOutlinePage).update();
    }
    // reset undo
    IDocument doc = getDocumentProvider().getDocument(getEditorInput());
    if (doc instanceof IStructuredDocument) {
      ((IStructuredDocument) doc).getUndoManager().getCommandStack().flush();
    }

    // update menu text
    updateMenuText();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor progressMonitor) {
    IStructuredModel model = null;
    try {
      model = aboutToSaveModel();
      updateEncodingMemento();
      super.doSave(progressMonitor);
    } finally {
      savedModel(model);
    }
  }

  /**
   * Sets up this editor's context menu before it is made visible.
   * <p>
   * Not API. May be reduced to protected method in the future.
   * </p>
   * 
   * @param menu the menu
   */
  @Override
  public void editorContextMenuAboutToShow(IMenuManager menu) {
    editorContextMenuAboutToShow2(menu);
    addContextMenuActions(menu);
    addSourceMenuActions(menu);
    addRefactorMenuActions(menu);
    addExtendedContextMenuActions(menu);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  @Override
  public Object getAdapter(Class required) {
    if (org.eclipse.wst.sse.core.internal.util.Debug.perfTestAdapterClassLoading) {
      startPerfTime = System.currentTimeMillis();
    }
    Object result = null;
    // text editor
    IStructuredModel internalModel = getInternalModel();
    if (ITextEditor.class.equals(required) || ITextEditorExtension5.class.equals(required)
        || ITextEditorExtension4.class.equals(required)
        || ITextEditorExtension3.class.equals(required)
        || ITextEditorExtension2.class.equals(required)
        || ITextEditorExtension.class.equals(required)) {
      result = this;
    } else if (IWorkbenchSiteProgressService.class.equals(required)) {
      return getEditorPart().getSite().getAdapter(IWorkbenchSiteProgressService.class);
    }
    // content outline page
    else if (IContentOutlinePage.class.equals(required)) {
      if (fOutlinePage == null && isCalledByOutline()) {
        ContentOutlineConfiguration cfg = createContentOutlineConfiguration();
        if (cfg != null) {
          ConfigurableContentOutlinePage outlinePage = new ConfigurableContentOutlinePage();
          outlinePage.setConfiguration(cfg);
          if (internalModel != null) {
            outlinePage.setInputContentTypeIdentifier(internalModel.getContentTypeIdentifier());
            outlinePage.setInput(internalModel);
          }

          if (fOutlinePageListener == null) {
            fOutlinePageListener = new OutlinePageListener();
          }

          outlinePage.addSelectionChangedListener(fOutlinePageListener);
          outlinePage.addDoubleClickListener(fOutlinePageListener);

          fOutlinePage = outlinePage;
        }
      }
      result = fOutlinePage;
    }
    // property sheet page, but only if the input's editable
    else if (IPropertySheetPage.class.equals(required) && isEditable()) {
      if (fPropertySheetPage == null || fPropertySheetPage.getControl() == null
          || fPropertySheetPage.getControl().isDisposed()) {
        PropertySheetConfiguration cfg = createPropertySheetConfiguration();
        if (cfg != null) {
          ConfigurablePropertySheetPage propertySheetPage = new ConfigurablePropertySheetPage();
          propertySheetPage.setConfiguration(cfg);
          fPropertySheetPage = propertySheetPage;
        }
      }
      result = fPropertySheetPage;
    } else if (IDocument.class.equals(required)) {
      result = getDocumentProvider().getDocument(getEditorInput());
    } else if (ISourceEditingTextTools.class.equals(required)) {
      result = createSourceEditingTextTools();
    } else if (IToggleBreakpointsTarget.class.equals(required)) {
      result = ToggleBreakpointsTarget.getInstance();
    } else if (ITextEditorExtension4.class.equals(required)) {
      result = this;
    } else if (IShowInTargetList.class.equals(required)) {
      result = new ShowInTargetListAdapter();
    } else if (IVerticalRuler.class.equals(required)) {
      return getVerticalRuler();
    } else if (SelectionHistory.class.equals(required)) {
      if (fSelectionHistory == null) {
        fSelectionHistory = new SelectionHistory(this);
      }
      result = fSelectionHistory;
    } else if (IResource.class.equals(required)) {
      IEditorInput input = getEditorInput();
      if (input != null) {
        result = input.getAdapter(required);
      }
    } else {
      if (result == null && internalModel != null) {
        result = internalModel.getAdapter(required);
      }
      // others
      if (result == null) {
        result = super.getAdapter(required);
      }
    }
    if (result == null) {
//			Logger.log(Logger.INFO_DEBUG, "StructuredTextEditor.getAdapter returning null for " + required); //$NON-NLS-1$
    }
    if (org.eclipse.wst.sse.core.internal.util.Debug.perfTestAdapterClassLoading) {
      long stop = System.currentTimeMillis();
      adapterRequests++;
      adapterTime += (stop - startPerfTime);
    }
    if (org.eclipse.wst.sse.core.internal.util.Debug.perfTestAdapterClassLoading) {
      System.out.println("Total calls to getAdapter: " + adapterRequests); //$NON-NLS-1$
      System.out.println("Total time in getAdapter: " + adapterTime); //$NON-NLS-1$
      System.out.println("Average time per call: " + (adapterTime / adapterRequests)); //$NON-NLS-1$
    }
    return result;
  }

  /**
   * Returns this editor part.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   * 
   * @return this editor part
   */
  public IEditorPart getEditorPart() {
    if (fEditorPart == null) {
      return this;
    }
    return fEditorPart;
  }

  /**
   * Returns this editor's StructuredModel.
   * <p>
   * Not API. Will be removed in the future.
   * </p>
   * 
   * @return returns this editor's IStructuredModel
   * @deprecated - This method allowed for uncontrolled access to the model instance and will be
   *             removed in the future. It is recommended that the current document provider be
   *             asked for the current document and the IModelManager then asked for the
   *             corresponding model with getExistingModelFor*(IDocument). Supported document
   *             providers ensure that the document maps to a shared structured model.
   */
  @Deprecated
  public IStructuredModel getModel() {
    IDocumentProvider documentProvider = getDocumentProvider();

    if (documentProvider == null) {
      // this indicated an error in startup sequence
      Logger.trace(getClass().getName(),
          "Program Info Only: document provider was null when model requested"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // Remember if we entered this method without a model existing
    boolean initialModelNull = (fStructuredModel == null);

    if (fStructuredModel == null && documentProvider != null) {
      // lazily set the model instance, although this is an ABNORMAL
      // CODE PATH
      if (documentProvider instanceof IModelProvider) {
        fStructuredModel = ((IModelProvider) documentProvider).getModel(getEditorInput());
        fisReleased = false;
      } else {
        IDocument doc = documentProvider.getDocument(getEditorInput());
        if (doc instanceof IStructuredDocument) {
          /*
           * Called in this manner because getExistingModel can skip some calculations always
           * performed in getModelForEdit
           */
          IStructuredModel model = StructuredModelManager.getModelManager().getExistingModelForEdit(
              doc);
          if (model == null) {
            model = StructuredModelManager.getModelManager().getModelForEdit(
                (IStructuredDocument) doc);
          }
          fStructuredModel = model;
          fisReleased = false;
        }
      }

      EditorModelUtil.addFactoriesTo(fStructuredModel);

      if (initialModelNull && fStructuredModel != null) {
        /*
         * DMW: 9/1/2002 -- why is update called here? No change has been indicated? I'd like to
         * remove, but will leave for now to avoid breaking this hack. Should measure/breakpoint to
         * see how large the problem is. May cause performance problems.
         * 
         * DMW: 9/8/2002 -- not sure why this was here initially, but the intent/hack must have been
         * to call update if this was the first time fStructuredModel was set. So, I added the logic
         * to check for that "first time" case. It would appear we don't really need. may remove in
         * future when can test more.
         */
        update();
      }
    }
    return fStructuredModel;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.IWorkbenchPartOrientation#getOrientation()
   */
  @Override
  public int getOrientation() {
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=88714
    return SWT.LEFT_TO_RIGHT;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.ITextEditor#getSelectionProvider()
   */
  @Override
  public ISelectionProvider getSelectionProvider() {
    if (fStructuredSelectionProvider == null) {
      ISelectionProvider parentProvider = super.getSelectionProvider();
      if (parentProvider != null) {
        fStructuredSelectionProvider = new StructuredSelectionProvider(parentProvider, this);
        fStructuredSelectionProvider.addPostSelectionChangedListener(new ISelectionChangedListener() {
          @Override
          public void selectionChanged(SelectionChangedEvent event) {
            updateStatusLine(event.getSelection());
          }
        });
        if (fStructuredModel != null) {
          SelectionConvertor convertor = (SelectionConvertor) fStructuredModel.getAdapter(SelectionConvertor.class);
          if (convertor != null) {
            fStructuredSelectionProvider.selectionConvertor = convertor;
          }
        }
      }
    }
    if (fStructuredSelectionProvider == null) {
      return super.getSelectionProvider();
    }
    return fStructuredSelectionProvider;
  }

  /**
   * Returns the editor's source viewer. This method was created to expose the protected final
   * getSourceViewer() method.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   * 
   * @return the editor's source viewer
   */
  public StructuredTextViewer getTextViewer() {
    return (StructuredTextViewer) getSourceViewer();
  }

  @Override
  public Annotation gotoAnnotation(boolean forward) {
    Annotation result = super.gotoAnnotation(forward);
    if (result != null) {
      fSelectionChangedFromGoto = true;
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    // if we've gotten an error elsewhere, before
    // we've actually opened, then don't open.
    if (shouldClose) {
      setSite(site);
      close(false);
    } else {
      super.init(site, input);
    }
  }

  /**
   * Set the document provider for this editor.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   * 
   * @param documentProvider documentProvider to initialize
   */
  public void initializeDocumentProvider(IDocumentProvider documentProvider) {
    if (documentProvider != null) {
      setDocumentProvider(documentProvider);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Not API. May be reduced to protected method in the future.
   * </p>
   */
  @Override
  public void rememberSelection() {
    /*
     * This method was made public for use by editors that use StructuredTextEditor (like some
     * clients)
     */
    super.rememberSelection();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Not API. May be reduced to protected method in the future.
   * </p>
   */
  @Override
  public void restoreSelection() {
    /*
     * This method was made public for use by editors that use StructuredTextEditor (like some
     * clients)
     */
    // catch odd case where source viewer has no text
    // widget (defect
    // 227670)
    if ((getSourceViewer() != null) && (getSourceViewer().getTextWidget() != null)) {
      super.restoreSelection();
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Overridden to expose part activation handling for multi-page editors.
   * </p>
   * <p>
   * Not API. May be reduced to protected method in the future.
   * </p>
   * 
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#safelySanityCheckState(org.eclipse.ui.IEditorInput)
   */
  @Override
  public void safelySanityCheckState(IEditorInput input) {
    super.safelySanityCheckState(input);
  }

  /**
   * Set editor part associated with this editor.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   * 
   * @param editorPart editor part associated with this editor
   */
  public void setEditorPart(IEditorPart editorPart) {
    fEditorPart = editorPart;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.WorkbenchPart#showBusy(boolean)
   */
  @Override
  public void showBusy(boolean busy) {
    // no-op
    super.showBusy(busy);
  }

  /**
   * Update should be called whenever the model is set or changed (as in swapped)
   * <p>
   * Not API. May be removed in the future.
   * </p>
   */
  public void update() {
    if (fOutlinePage != null && fOutlinePage instanceof ConfigurableContentOutlinePage) {
      ContentOutlineConfiguration cfg = createContentOutlineConfiguration();
      ((ConfigurableContentOutlinePage) fOutlinePage).setConfiguration(cfg);
      IStructuredModel internalModel = getInternalModel();
      ((ConfigurableContentOutlinePage) fOutlinePage).setInputContentTypeIdentifier(internalModel.getContentTypeIdentifier());
      ((ConfigurableContentOutlinePage) fOutlinePage).setInput(internalModel);
    }
    if (fPropertySheetPage != null && fPropertySheetPage instanceof ConfigurablePropertySheetPage) {
      PropertySheetConfiguration cfg = createPropertySheetConfiguration();
      ((ConfigurablePropertySheetPage) fPropertySheetPage).setConfiguration(cfg);
    }
    disposeModelDependentFields();

    fShowInTargetIds = createShowInTargetIds();

    if (getSourceViewerConfiguration() instanceof StructuredTextViewerConfiguration
        && fStatusLineLabelProvider != null) {
      fStatusLineLabelProvider.dispose();
    }

    String configurationId = fViewerConfigurationTargetId;
    updateSourceViewerConfiguration();

    /* Only reinstall if the configuration id has changed */
    if (configurationId != null && !configurationId.equals(fViewerConfigurationTargetId)) {
      uninstallSemanticHighlighting();
      installSemanticHighlighting();
    }

    if (getSourceViewerConfiguration() instanceof StructuredTextViewerConfiguration) {
      fStatusLineLabelProvider = ((StructuredTextViewerConfiguration) getSourceViewerConfiguration()).getStatusLineLabelProvider(getSourceViewer());
      updateStatusLine(null);
    }

    if (fEncodingSupport != null && fEncodingSupport instanceof EncodingSupport) {
      ((EncodingSupport) fEncodingSupport).reinitialize(getConfigurationPoints());
    }

    createModelDependentFields();
  }

  protected void addContextMenuActions(IMenuManager menu) {
    // Only offer actions that affect the text if the viewer allows
    // modification and supports any of these operations

    // Some Design editors (DTD) rely on this view for their own uses
//    menu.appendToGroup(IWorkbenchActionConstants.GROUP_ADD, fShowPropertiesAction);
  }

  protected void addExtendedRulerContextMenuActions(IMenuManager menu) {
    // none at this level
  }

  protected void addRefactorMenuActions(IMenuManager menu) {
//    IMenuManager subMenu = new MenuManager(SSEUIMessages.RefactorMenu_label,
//        IStructuredTextEditorActionConstants.REFACTOR_CONTEXT_MENU_ID);
//    menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, subMenu);
  }

  protected void addSourceMenuActions(IMenuManager menu) {
//    IMenuManager subMenu = new MenuManager(SSEUIMessages.SourceMenu_label,
//        IStructuredTextEditorActionConstants.SOURCE_CONTEXT_MENU_ID);
//    subMenu.add(new Separator(IStructuredTextEditorActionConstants.SOURCE_BEGIN));
//    subMenu.add(new Separator(IStructuredTextEditorActionConstants.SOURCE_ADDITIONS));
//    subMenu.add(new Separator(IStructuredTextEditorActionConstants.SOURCE_END));
//    menu.appendToGroup(ITextEditorActionConstants.GROUP_EDIT, subMenu);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#collectContextMenuPreferencePages()
   */
  @Override
  protected String[] collectContextMenuPreferencePages() {
    List allIds = new ArrayList(0);

    // get contributed preference pages
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] configurationIds = getConfigurationPoints();
    for (int i = 0; i < configurationIds.length; i++) {
      String[] definitions = builder.getDefinitions("preferencepages", configurationIds[i]); //$NON-NLS-1$
      for (int j = 0; j < definitions.length; j++) {
        String someIds = definitions[j];
        if (someIds != null && someIds.length() > 0) {
          // supports multiple comma-delimited page IDs in one
          // element
          String[] ids = StringUtils.unpack(someIds);
          for (int k = 0; k < ids.length; k++) {
            // trim, just to keep things clean
            String id = ids[k].trim();
            if (!allIds.contains(id)) {
              allIds.add(id);
            }
          }
        }
      }
    }

    // add pages contributed by super
    String[] superPages = super.collectContextMenuPreferencePages();
    for (int m = 0; m < superPages.length; m++) {
      // trim, just to keep things clean
      String id = superPages[m].trim();
      if (!allIds.contains(id)) {
        allIds.add(id);
      }
    }

    return (String[]) allIds.toArray(new String[0]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.ExtendedTextEditor#configureSourceViewerDecorationSupport(org.eclipse
   * .ui.texteditor.SourceViewerDecorationSupport)
   */
  @Override
  protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
    support.setCharacterPairMatcher(createCharacterPairMatcher());
    support.setMatchingCharacterPainterPreferenceKeys(EditorPreferenceNames.MATCHING_BRACKETS,
        EditorPreferenceNames.MATCHING_BRACKETS_COLOR);

    super.configureSourceViewerDecorationSupport(support);
  }

  @Override
  protected void createActions() {
    super.createActions();
    ResourceBundle resourceBundle = SSEUIMessages.getResourceBundle();
    IWorkbenchHelpSystem helpSystem = SSEUIPlugin.getDefault().getWorkbench().getHelpSystem();
    // TextView Action - moving the selected text to
    // the clipboard
    // override the cut/paste/delete action to make
    // them run on read-only
    // files
    Action action = new TextOperationAction(resourceBundle,
        "Editor_Cut_", this, ITextOperationTarget.CUT); //$NON-NLS-1$
    action.setActionDefinitionId(IWorkbenchActionDefinitionIds.CUT);
    setAction(ITextEditorActionConstants.CUT, action);
    helpSystem.setHelp(action, IAbstractTextEditorHelpContextIds.CUT_ACTION);
    // TextView Action - inserting the clipboard
    // content at the current
    // position
    // override the cut/paste/delete action to make
    // them run on read-only
    // files
    action = new TextOperationAction(resourceBundle,
        "Editor_Paste_", this, ITextOperationTarget.PASTE); //$NON-NLS-1$
    action.setActionDefinitionId(IWorkbenchActionDefinitionIds.PASTE);
    setAction(ITextEditorActionConstants.PASTE, action);
    helpSystem.setHelp(action, IAbstractTextEditorHelpContextIds.PASTE_ACTION);
    // TextView Action - deleting the selected text or
    // if selection is
    // empty the character at the right of the current
    // position
    // override the cut/paste/delete action to make
    // them run on read-only
    // files
    action = new TextOperationAction(resourceBundle,
        "Editor_Delete_", this, ITextOperationTarget.DELETE); //$NON-NLS-1$
    action.setActionDefinitionId(IWorkbenchActionDefinitionIds.DELETE);
    setAction(ITextEditorActionConstants.DELETE, action);
    helpSystem.setHelp(action, IAbstractTextEditorHelpContextIds.DELETE_ACTION);
    // SourceView Action - requesting content assist to
    // show completetion
    // proposals for the current insert position
    action = new TextOperationAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_PROPOSALS + UNDERSCORE, this,
        ISourceViewer.CONTENTASSIST_PROPOSALS);
    helpSystem.setHelp(action, IHelpContextIds.CONTMNU_CONTENTASSIST_HELPID);
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    setAction(StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_PROPOSALS, action);
    markAsStateDependentAction(
        StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_PROPOSALS, true);
    // SourceView Action - requesting content assist to
    // show the content
    // information for the current insert position
    action = new TextOperationAction(SSEUIMessages.getResourceBundle(),
        StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_CONTEXT_INFORMATION
            + UNDERSCORE, this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
    setAction(StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_CONTEXT_INFORMATION,
        action);
    markAsStateDependentAction(
        StructuredTextEditorActionConstants.ACTION_NAME_CONTENTASSIST_CONTEXT_INFORMATION, true);
    // StructuredTextViewer Action - requesting format
    // of the whole
    // document
    action = new TextOperationAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT + UNDERSCORE, this,
        StructuredTextViewer.FORMAT_DOCUMENT);
    helpSystem.setHelp(action, IHelpContextIds.CONTMNU_FORMAT_DOC_HELPID);
    action.setActionDefinitionId(ActionDefinitionIds.FORMAT_DOCUMENT);
    setAction(StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT, action);
    markAsStateDependentAction(StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT,
        true);
    markAsSelectionDependentAction(StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_DOCUMENT,
        true);
    // StructuredTextViewer Action - requesting format
    // of the active
    // elements
    action = new TextOperationAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS + UNDERSCORE, this,
        StructuredTextViewer.FORMAT_ACTIVE_ELEMENTS);
    helpSystem.setHelp(action, IHelpContextIds.CONTMNU_FORMAT_ELEMENTS_HELPID);
    action.setActionDefinitionId(ActionDefinitionIds.FORMAT_ACTIVE_ELEMENTS);
    setAction(StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS, action);
    markAsStateDependentAction(
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS, true);
    markAsSelectionDependentAction(
        StructuredTextEditorActionConstants.ACTION_NAME_FORMAT_ACTIVE_ELEMENTS, true);

    // StructuredTextEditor Action - add breakpoints (falling back to the
    // current double-click if they can't be added)
    action = new ToggleBreakpointAction(this, getVerticalRuler());
    setAction(ActionDefinitionIds.TOGGLE_BREAKPOINTS, action);
    // StructuredTextEditor Action - manage breakpoints
    action = new ManageBreakpointAction(this, getVerticalRuler());
    setAction(ActionDefinitionIds.MANAGE_BREAKPOINTS, action);
    // StructuredTextEditor Action - edit breakpoints
    action = new EditBreakpointAction(this, getVerticalRuler());
    setAction(ActionDefinitionIds.EDIT_BREAKPOINTS, action);
    // StructuredTextViewer Action - open file on selection
    action = new OpenHyperlinkAction(resourceBundle,
        StructuredTextEditorActionConstants.ACTION_NAME_OPEN_FILE + UNDERSCORE, this,
        getSourceViewer());
    action.setActionDefinitionId(ActionDefinitionIds.OPEN_FILE);
    setAction(StructuredTextEditorActionConstants.ACTION_NAME_OPEN_FILE, action);

    computeAndSetDoubleClickAction();

    //add handlers to handler service
    IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
    if (handlerService != null) {

      IHandler gotoHandler = new GotoMatchingBracketHandler();
      handlerService.activateHandler(ActionDefinitionIds.GOTO_MATCHING_BRACKET, gotoHandler);

      fOutlineHandler = new QuickOutlineHandler();
      handlerService.activateHandler(ActionDefinitionIds.SHOW_OUTLINE, fOutlineHandler);

      IHandler toggleCommentHandler = new ToggleLineCommentHandler();
      handlerService.activateHandler(ActionDefinitionIds.TOGGLE_COMMENT, toggleCommentHandler);

      IHandler addCommentBlockHandler = new AddBlockCommentHandler();
      handlerService.activateHandler(ActionDefinitionIds.ADD_BLOCK_COMMENT, addCommentBlockHandler);

      IHandler removeCommentBlockHandler = new RemoveBlockCommentHandler();
      handlerService.activateHandler(ActionDefinitionIds.REMOVE_BLOCK_COMMENT,
          removeCommentBlockHandler);
    }

    fShowPropertiesAction = new ShowPropertiesAction(getEditorPart(), getSelectionProvider());
    fFoldingGroup = new FoldingActionGroup(this, getSourceViewer());
    removeTrailingWhitespaceAction = new RemoveTrailingWhitespaceAction(getSourceViewer());
  }

  @Override
  protected LineChangeHover createChangeHover() {
    return super.createChangeHover(); //new StructuredLineChangeHover();
  }

  protected ICharacterPairMatcher createCharacterPairMatcher() {
    ICharacterPairMatcher matcher = null;
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] ids = getConfigurationPoints();
    for (int i = 0; matcher == null && i < ids.length; i++) {
      matcher = (ICharacterPairMatcher) builder.getConfiguration(DocumentRegionEdgeMatcher.ID,
          ids[i]);
    }
    if (matcher == null) {
      matcher = new DefaultCharacterPairMatcher(new char[] {
          '(', ')', '{', '}', '[', ']', '<', '>', '"', '"', '\'', '\''});
    }
    return matcher;
  }

  protected void createModelDependentFields() {
    if (fStructuredSelectionProvider != null) {
      SelectionConvertor convertor = (SelectionConvertor) fStructuredModel.getAdapter(SelectionConvertor.class);
      if (convertor != null) {
        fStructuredSelectionProvider.selectionConvertor = convertor;
      } else {
        fStructuredSelectionProvider.selectionConvertor = new SelectionConvertor();
      }
    }
  }

  protected PropertySheetConfiguration createPropertySheetConfiguration() {
    PropertySheetConfiguration cfg = null;
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] ids = getConfigurationPoints();
    for (int i = 0; cfg == null && i < ids.length; i++) {
      cfg = (PropertySheetConfiguration) builder.getConfiguration(
          ExtendedConfigurationBuilder.PROPERTYSHEETCONFIGURATION, ids[i]);
    }
    return cfg;
  }

  /**
   * Creates the source viewer to be used by this editor
   */
  @Override
  protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler verticalRuler,
      int styles) {
    fAnnotationAccess = createAnnotationAccess();
    fOverviewRuler = createOverviewRuler(getSharedColors());
    StructuredTextViewer sourceViewer = createStructedTextViewer(parent, verticalRuler, styles);
    initSourceViewer(sourceViewer);
    com.google.dart.tools.ui.internal.text.editor.EditorUtility.addGTKPasteHack(sourceViewer);
    return sourceViewer;
  }

  protected StructuredTextViewer createStructedTextViewer(Composite parent,
      IVerticalRuler verticalRuler, int styles) {
    return new StructuredTextViewer(parent, verticalRuler, getOverviewRuler(),
        isOverviewRulerVisible(), styles);
  }

  @Override
  protected void createUndoRedoActions() {
    // overridden to add icons to actions
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=111877
    super.createUndoRedoActions();
    IAction action = getAction(ITextEditorActionConstants.UNDO);
    if (action != null) {
      action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
          ISharedImages.IMG_TOOL_UNDO));
    }

    action = getAction(ITextEditorActionConstants.REDO);
    if (action != null) {
      action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
          ISharedImages.IMG_TOOL_REDO));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#disposeDocumentProvider()
   */
  @Override
  protected void disposeDocumentProvider() {
    if (fStructuredModel != null && !fisReleased
        && !(getDocumentProvider() instanceof IModelProvider)) {
      fStructuredModel.releaseFromEdit();
      fisReleased = true;
    }
    super.disposeDocumentProvider();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#doSetInput(org.eclipse.ui.IEditorInput)
   */
  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    IEditorInput oldInput = getEditorInput();
    if (oldInput != null) {
      IDocument olddoc = getDocumentProvider().getDocument(oldInput);
      if (olddoc != null && olddoc instanceof IExecutionDelegatable) {
        ((IExecutionDelegatable) olddoc).setExecutionDelegate(null);
      }
    }

    if (fStructuredModel != null && !(getDocumentProvider() instanceof IModelProvider)) {
      fStructuredModel.releaseFromEdit();
    }
    fStructuredModel = null;

    //attempt to get the model for the given input
    super.doSetInput(input);
    IStructuredModel model = tryToGetModel(input);
    
    if (model == null) {
      close(true);
      return;
    }

    /*
     * if could not get the model prompt user to update content type if preferences allow, then try
     * to get model again
     */
    if (model == null
        && SSEUIPlugin.getDefault().getPreferenceStore().getBoolean(
            EditorPreferenceNames.SHOW_UNKNOWN_CONTENT_TYPE_MSG)) {
      // display a dialog informing user of unknown content type giving them chance to update preferences
      UnknownContentTypeDialog dialog = new UnknownContentTypeDialog(getSite().getShell(),
          SSEUIPlugin.getDefault().getPreferenceStore(),
          EditorPreferenceNames.SHOW_UNKNOWN_CONTENT_TYPE_MSG);
      dialog.open();

      //try to get model again in hopes user updated preferences
      super.doSetInput(input);
      model = tryToGetModel(input);

      //still could not get the model to open this editor so log
      if (model == null) {
        logUnexpectedDocumentKind(input);
      }
    }

    if (fStructuredModel != null || model != null) {
      setModel(model);
    }

    if (getInternalModel() != null) {
      updateEditorControlsForContentType(getInternalModel().getContentTypeIdentifier());
    } else {
      updateEditorControlsForContentType(null);
    }

    // start editor with smart insert mode
    setInsertMode(SMART_INSERT);
  }

  protected void editorContextMenuAboutToShow2(IMenuManager menu) {
    // replaces call to super.editorContextMenuAboutToShow()
    menu.add(new Separator(ITextEditorActionConstants.GROUP_OPEN));
    menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
    menu.add(new Separator(RefactorActionGroup_OLD.GROUP_REORG));
    menu.add(new Separator(ITextEditorActionConstants.GROUP_RESTORE));
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    if (isEditable()) {
      addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.UNDO);
      addAction(menu, ITextEditorActionConstants.GROUP_RESTORE,
          ITextEditorActionConstants.REVERT_TO_SAVED);
      addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.CUT);
      addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.COPY);
      addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.PASTE);
      IAction action = getAction(ITextEditorActionConstants.QUICK_ASSIST);
      if (action != null && action.isEnabled()) {
        addAction(menu, RefactorActionGroup_OLD.GROUP_REORG, ITextEditorActionConstants.QUICK_ASSIST);
      }
    } else {
      addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.COPY);
    }
  }

  /**
   * added checks to overcome bug such that if we are shutting down in an error condition, then
   * viewer will have already been disposed.
   */
  @Override
  protected String getCursorPosition() {
    String result = null;
    // this may be too expensive in terms of
    // performance, to do this check
    // every time, just to gaurd against error
    // condition.
    // perhaps there's a better way?
    if (getSourceViewer() != null && getSourceViewer().getTextWidget() != null
        && !getSourceViewer().getTextWidget().isDisposed()) {
      result = super.getCursorPosition();
    } else {
      result = "0:0"; //$NON-NLS-1$
    }
    return result;
  }

  @Override
  protected SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
    /*
     * Removed workaround for Bug [206913] source annotations are not painting in source editors.
     * With the new presentation reconciler, we no longer need to force the painting. This actually
     * caused Bug [219776] Wrong annotation display on macs. We forced the Squiggles strategy, even
     * when the native problem underline was specified for annotations
     */
    return super.getSourceViewerDecorationSupport(viewer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#getStatusLineManager()
   * 
   * Overridden to use the top-level editor part's status line
   */
  @Override
  protected IStatusLineManager getStatusLineManager() {
    return getEditorPart().getEditorSite().getActionBars().getStatusLineManager();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#handleCursorPositionChanged()
   */
  @Override
  protected void handleCursorPositionChanged() {
    super.handleCursorPositionChanged();
    updateStatusField(StructuredTextEditorActionConstants.STATUS_CATEGORY_OFFSET);
  }

  @Override
  protected void handleElementContentReplaced() {
    super.handleElementContentReplaced();

    // queue a full revalidation of content
    IDocument document = getDocumentProvider().getDocument(getEditorInput());
    SourceViewerConfiguration sourceViewerConfiguration = getSourceViewerConfiguration();
    if (document != null
        && sourceViewerConfiguration != null
        && sourceViewerConfiguration.getReconciler(getSourceViewer()) instanceof DirtyRegionProcessor) {
      ((DirtyRegionProcessor) sourceViewerConfiguration.getReconciler(getSourceViewer())).processDirtyRegion(new DirtyRegion(
          0, document.getLength(), DirtyRegion.INSERT, document.get()));
    }

    /*
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=129906 - update selection to listeners
     */
    ISelectionProvider selectionProvider = getSelectionProvider();
    ISelection originalSelection = selectionProvider.getSelection();
    if (selectionProvider instanceof StructuredSelectionProvider
        && originalSelection instanceof ITextSelection) {
      ITextSelection textSelection = (ITextSelection) originalSelection;
      // make sure the old selection is actually still valid
      if (!textSelection.isEmpty()
          && (document == null || textSelection.getOffset() + textSelection.getLength() <= document.getLength())) {
        SelectionChangedEvent syntheticEvent = new SelectionChangedEvent(selectionProvider,
            new TextSelection(textSelection.getOffset(), textSelection.getLength()));
        ((StructuredSelectionProvider) selectionProvider).handleSelectionChanged(syntheticEvent);
        ((StructuredSelectionProvider) selectionProvider).handlePostSelectionChanged(syntheticEvent);
      } else {
        SelectionChangedEvent syntheticEvent = new SelectionChangedEvent(selectionProvider,
            new TextSelection(0, 0));
        ((StructuredSelectionProvider) selectionProvider).handleSelectionChanged(syntheticEvent);
        ((StructuredSelectionProvider) selectionProvider).handlePostSelectionChanged(syntheticEvent);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.AbstractTextEditor#handlePreferenceStoreChanged(org.eclipse.jface
   * .util.PropertyChangeEvent)
   */
  @Override
  protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
    String property = event.getProperty();

    if (EditorPreferenceNames.EDITOR_TEXT_HOVER_MODIFIERS.equals(property)) {
      updateHoverBehavior();
    }

    //enable or disable as you type validation
    else if (CommonEditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS.equals(property)) {
      IReconciler reconciler = this.getSourceViewerConfiguration().getReconciler(
          this.getSourceViewer());
      if (reconciler instanceof DocumentRegionProcessor) {
        ((DocumentRegionProcessor) reconciler).setValidatorStrategyEnabled(isValidationEnabled());
      }
    }

    else if (AbstractStructuredFoldingStrategy.FOLDING_ENABLED.equals(property)) {
      if (getSourceViewer() instanceof ProjectionViewer) {
        // install projection support if it has not even been
        // installed yet
        if (isFoldingEnabled() && (fProjectionSupport == null)) {
          installProjectionSupport();
        }
        ProjectionViewer pv = (ProjectionViewer) getSourceViewer();
        if (pv.isProjectionMode() != isFoldingEnabled()) {
          if (pv.canDoOperation(ProjectionViewer.TOGGLE)) {
            pv.doOperation(ProjectionViewer.TOGGLE);
          }
        }
      }
    }

    // update content assist preferences
    else if (EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND.equals(property)) {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        SourceViewerConfiguration configuration = getSourceViewerConfiguration();
        if (configuration != null) {
          IContentAssistant contentAssistant = configuration.getContentAssistant(sourceViewer);
          if (contentAssistant instanceof ContentAssistant) {
            ContentAssistant assistant = (ContentAssistant) contentAssistant;
            RGB rgb = PreferenceConverter.getColor(getPreferenceStore(),
                EditorPreferenceNames.CODEASSIST_PROPOSALS_BACKGROUND);
            Color color = EditorUtility.getColor(rgb);
            assistant.setProposalSelectorBackground(color);
          }
        }
      }
    }

    // update content assist preferences
    else if (EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND.equals(property)) {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        SourceViewerConfiguration configuration = getSourceViewerConfiguration();
        if (configuration != null) {
          IContentAssistant contentAssistant = configuration.getContentAssistant(sourceViewer);
          if (contentAssistant instanceof ContentAssistant) {
            ContentAssistant assistant = (ContentAssistant) contentAssistant;
            RGB rgb = PreferenceConverter.getColor(getPreferenceStore(),
                EditorPreferenceNames.CODEASSIST_PROPOSALS_FOREGROUND);
            Color color = EditorUtility.getColor(rgb);
            assistant.setProposalSelectorForeground(color);
          }
        }
      }
    }

    // update content assist preferences
    else if (EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND.equals(property)) {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        SourceViewerConfiguration configuration = getSourceViewerConfiguration();
        if (configuration != null) {
          IContentAssistant contentAssistant = configuration.getContentAssistant(sourceViewer);
          if (contentAssistant instanceof ContentAssistant) {
            ContentAssistant assistant = (ContentAssistant) contentAssistant;
            RGB rgb = PreferenceConverter.getColor(getPreferenceStore(),
                EditorPreferenceNames.CODEASSIST_PARAMETERS_BACKGROUND);
            Color color = EditorUtility.getColor(rgb);
            assistant.setContextInformationPopupBackground(color);
            assistant.setContextSelectorBackground(color);
          }
        }
      }
    }

    // update content assist preferences
    else if (EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND.equals(property)) {
      ISourceViewer sourceViewer = getSourceViewer();
      if (sourceViewer != null) {
        SourceViewerConfiguration configuration = getSourceViewerConfiguration();
        if (configuration != null) {
          IContentAssistant contentAssistant = configuration.getContentAssistant(sourceViewer);
          if (contentAssistant instanceof ContentAssistant) {
            ContentAssistant assistant = (ContentAssistant) contentAssistant;
            RGB rgb = PreferenceConverter.getColor(getPreferenceStore(),
                EditorPreferenceNames.CODEASSIST_PARAMETERS_FOREGROUND);
            Color color = EditorUtility.getColor(rgb);
            assistant.setContextInformationPopupForeground(color);
            assistant.setContextSelectorForeground(color);
          }
        }
      }
    }

    super.handlePreferenceStoreChanged(event);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.AbstractTextEditor#initializeDragAndDrop(org.eclipse.jface.text.source
   * .ISourceViewer)
   */
  @Override
  protected void initializeDragAndDrop(ISourceViewer viewer) {
    IPreferenceStore store = getPreferenceStore();
    if (store != null && store.getBoolean(PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED)) {
      initializeDrop(viewer);
    }
  }

  protected void initializeDrop(ITextViewer textViewer) {
    int operations = DND.DROP_COPY | DND.DROP_MOVE;
    fDropTarget = new DropTarget(textViewer.getTextWidget(), operations);
    fDropAdapter = new ReadOnlyAwareDropTargetAdapter(true);
    fDropAdapter.setTargetEditor(this);
    fDropAdapter.setTargetIDs(getConfigurationPoints());
    fDropAdapter.setTextViewer(textViewer);
    fDropTarget.setTransfer(fDropAdapter.getTransfers());
    fDropTarget.addDropListener(fDropAdapter);
    fDropTarget.addDisposeListener(new DisposeListener() {

      @Override
      public void widgetDisposed(DisposeEvent e) {
        fDropTarget.removeDropListener(fDropAdapter);
        fDropTarget.removeDisposeListener(this);
        fDropTarget.dispose();
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#initializeEditor()
   */
  @Override
  protected void initializeEditor() {
    super.initializeEditor();

    setPreferenceStore(createCombinedPreferenceStore());

    setRangeIndicator(new DefaultRangeIndicator());
    setEditorContextMenuId(EDITOR_CONTEXT_MENU_ID);
    initializeDocumentProvider(null);
    // set the infopop for source viewer
    String helpId = getHelpContextId();
    // no infopop set or using default text editor help, use default
    if (helpId == null || ITextEditorHelpContextIds.TEXT_EDITOR.equals(helpId)) {
      helpId = IHelpContextIds.XML_SOURCE_VIEW_HELPID;
    }
    setHelpContextId(helpId);
    // defect 203158 - disable ruler context menu for
    // beta
    // setRulerContextMenuId(RULER_CONTEXT_MENU_ID);
    configureInsertMode(SMART_INSERT, true);

    // enable the base source editor activity when editor opens
    try {
      // FIXME: - commented out to avoid minor dependancy during
      // transition to org.eclipse
      // WTPActivityBridge.getInstance().enableActivity(CORE_SSE_ACTIVITY_ID,
      // true);
    } catch (Exception t) {
      // if something goes wrong with enabling activity, just log the
      // error but dont
      // have it break the editor
      Logger.log(Logger.WARNING_DEBUG, t.getMessage(), t);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.editors.text.TextEditor#initializeKeyBindingScopes()
   */
  @Override
  protected void initializeKeyBindingScopes() {
    setKeyBindingScopes(new String[] {EDITOR_KEYBINDING_SCOPE_ID});
  }

  protected void initSourceViewer(StructuredTextViewer sourceViewer) {
    // ensure decoration support is configured
    getSourceViewerDecorationSupport(sourceViewer);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.editors.text.TextEditor#installEncodingSupport()
   */
  @Override
  protected void installEncodingSupport() {
    fEncodingSupport = new EncodingSupport(getConfigurationPoints());
    fEncodingSupport.initialize(this);
  }

  @Override
  protected void installTextDragAndDrop(ISourceViewer viewer) {
    // do nothing
  }

  /*
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#performRevert()
   */
  @Override
  protected void performRevert() {
    ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
    projectionViewer.setRedraw(false);
    try {

      boolean projectionMode = projectionViewer.isProjectionMode();
      if (projectionMode) {
        projectionViewer.disableProjection();
      }

      super.performRevert();

      if (projectionMode) {
        projectionViewer.enableProjection();
      }

    } finally {
      projectionViewer.setRedraw(true);
    }
  }

  @Override
  protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
    performSaveActions();
    super.performSave(overwrite, progressMonitor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.ui.texteditor.AbstractTextEditor#rulerContextMenuAboutToShow(org.eclipse.jface.
   * action.IMenuManager)
   */
  @Override
  protected void rulerContextMenuAboutToShow(IMenuManager menu) {
//    super.rulerContextMenuAboutToShow(menu);
    rulerContextMenuAboutToShow2(menu);

//    IMenuManager foldingMenu = new MenuManager(SSEUIMessages.Folding, "projection"); //$NON-NLS-1$
//    menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, foldingMenu);

    IAction action = getAction("FoldingToggle"); //$NON-NLS-1$
    menu.add(action);
    action = getAction("FoldingExpandAll"); //$NON-NLS-1$
    menu.add(action);
    action = getAction("FoldingCollapseAll"); //$NON-NLS-1$
    menu.add(action);

    IStructuredModel internalModel = getInternalModel();
    if (internalModel != null) {
      boolean debuggingAvailable = BreakpointProviderBuilder.getInstance().isAvailable(
          internalModel.getContentTypeIdentifier(),
          BreakpointRulerAction.getFileExtension(getEditorInput()));
      if (debuggingAvailable) {
        // append actions to "debug" group (created in
        // AbstractDecoratedTextEditor.rulerContextMenuAboutToShow(IMenuManager)
        menu.appendToGroup("debug", getAction(ActionDefinitionIds.TOGGLE_BREAKPOINTS)); //$NON-NLS-1$
        menu.appendToGroup("debug", getAction(ActionDefinitionIds.MANAGE_BREAKPOINTS)); //$NON-NLS-1$
        menu.appendToGroup("debug", getAction(ActionDefinitionIds.EDIT_BREAKPOINTS)); //$NON-NLS-1$
      }
      addExtendedRulerContextMenuActions(menu);
    }
  }

  protected void rulerContextMenuAboutToShow2(IMenuManager menu) {
    /*
     * XXX: workaround for reliable menu item ordering. This can be changed once the action
     * contribution story converges, see
     * http://dev.eclipse.org/viewcvs/index.cgi/~checkout~/platform
     * -ui-home/R3_1/dynamic_teams/dynamic_teams.html#actionContributions
     */
    // pre-install menus for contributions and call super
    menu.add(new Separator("debug")); //$NON-NLS-1$
    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_RESTORE));
    menu.add(new Separator("add")); //$NON-NLS-1$
    menu.add(new Separator(ITextEditorActionConstants.GROUP_RULERS));
    menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));

//    super.rulerContextMenuAboutToShow(menu); (inlined)
//    menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));
//    menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

//    for (Iterator i = getRulerContextMenuListeners().iterator(); i.hasNext();) {
//      ((IMenuListener) i.next()).menuAboutToShow(menu);
//    }

//    addAction(menu, ITextEditorActionConstants.RULER_MANAGE_BOOKMARKS);
//    addAction(menu, ITextEditorActionConstants.RULER_MANAGE_TASKS);

//    addRulerContributionActions(menu); (deleted)

    /* quick diff */
    if (isEditorInputModifiable()) {
      IAction quickdiffAction = getAction(ITextEditorActionConstants.QUICKDIFF_TOGGLE);
      quickdiffAction.setChecked(isChangeInformationShowing());
      menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, quickdiffAction);

      if (isChangeInformationShowing()) {
        TextEditorAction revertLine = new RevertLineAction(this, true);
        TextEditorAction revertSelection = new RevertSelectionAction(this, true);
        TextEditorAction revertBlock = new RevertBlockAction(this, true);
        TextEditorAction revertDeletion = new RestoreAction(this, true);

        revertSelection.update();
        revertBlock.update();
        revertLine.update();
        revertDeletion.update();

        // only add block action if selection action is not enabled
        if (revertSelection.isEnabled()) {
          menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertSelection);
        } else if (revertBlock.isEnabled()) {
          menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertBlock);
        }
        if (revertLine.isEnabled()) {
          menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertLine);
        }
        if (revertDeletion.isEnabled()) {
          menu.appendToGroup(ITextEditorActionConstants.GROUP_RESTORE, revertDeletion);
        }
      }
    }

    // revision info
    LineNumberColumn fLineColumn = getLineColumn();
//    if (fLineColumn != null && fLineColumn.isShowingRevisionInformation()) {
//      IMenuManager revisionMenu = new MenuManager(
//          TextEditorMessages.AbstractDecoratedTextEditor_revisions_menu);
//      menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, revisionMenu);
//
//      IAction hideRevisionInfoAction = getAction(ITextEditorActionConstants.REVISION_HIDE_INFO);
//      revisionMenu.add(hideRevisionInfoAction);
//      revisionMenu.add(new Separator());
//
//      String[] labels = {
//          TextEditorMessages.AbstractDecoratedTextEditor_revision_colors_option_by_date,
//          TextEditorMessages.AbstractDecoratedTextEditor_revision_colors_option_by_author,
//          TextEditorMessages.AbstractDecoratedTextEditor_revision_colors_option_by_author_and_date};
//      final RenderingMode[] modes = {
//          IRevisionRulerColumnExtension.AGE, IRevisionRulerColumnExtension.AUTHOR,
//          IRevisionRulerColumnExtension.AUTHOR_SHADED_BY_AGE};
//      final IPreferenceStore uiStore = EditorsUI.getPreferenceStore();
//      String current = uiStore.getString(AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_RENDERING_MODE);
//      for (int i = 0; i < modes.length; i++) {
//        final String mode = modes[i].name();
//        IAction action = new Action(labels[i], IAction.AS_RADIO_BUTTON) {
//          @Override
//          public void run() {
//            // set preference globally, LineNumberColumn reacts on preference change
//            uiStore.setValue(
//                AbstractDecoratedTextEditorPreferenceConstants.REVISION_RULER_RENDERING_MODE, mode);
//          }
//        };
//        action.setChecked(mode.equals(current));
//        revisionMenu.add(action);
//      }
//
//      revisionMenu.add(new Separator());
//
//      IAction action = getAction(ITextEditorActionConstants.REVISION_SHOW_AUTHOR_TOGGLE);
//      if (action instanceof IUpdate) {
//        ((IUpdate) action).update();
//      }
//      revisionMenu.add(action);
//      action = getAction(ITextEditorActionConstants.REVISION_SHOW_ID_TOGGLE);
//      if (action instanceof IUpdate) {
//        ((IUpdate) action).update();
//      }
//      revisionMenu.add(action);
//    }

    IAction lineNumberAction = getAction(ITextEditorActionConstants.LINENUMBERS_TOGGLE);
    lineNumberAction.setChecked(fLineColumn != null && fLineColumn.isShowingLineNumbers());
    menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, lineNumberAction);

//    IAction preferencesAction= getAction(ITextEditorActionConstants.RULER_PREFERENCES);
//    menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, new Separator(ITextEditorActionConstants.GROUP_SETTINGS));
//    menu.appendToGroup(ITextEditorActionConstants.GROUP_SETTINGS, preferencesAction);
  }

  @Override
  protected void sanityCheckState(IEditorInput input) {
    try {
      ++validateEditCount;
      super.sanityCheckState(input);
    } finally {
      --validateEditCount;
    }
  }

  /**
   * Ensure that the correct IDocumentProvider is used. For direct models, a special provider is
   * used. For StorageEditorInputs, use a custom provider that creates a usable
   * ResourceAnnotationModel. For everything else, use the base support.
   * 
   * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#setDocumentProvider(org.eclipse.ui.IEditorInput)
   */
  @Override
  protected void setDocumentProvider(IEditorInput input) {
    if (input instanceof IStructuredModel) {
      // largely untested
      setDocumentProvider(StructuredModelDocumentProvider.getInstance());
    } else if (input instanceof IStorageEditorInput && !(input instanceof IFileEditorInput)) {
      setDocumentProvider(StorageModelProvider.getInstance());
    } else {
      super.setDocumentProvider(input);
    }
  }

  /**
   * Sets the editor's source viewer configuration which it uses to configure it's internal source
   * viewer. This method was overwritten so that viewer configuration could be set after editor part
   * was created.
   */
  @Override
  protected void setSourceViewerConfiguration(SourceViewerConfiguration config) {
    SourceViewerConfiguration oldSourceViewerConfiguration = getSourceViewerConfiguration();
    super.setSourceViewerConfiguration(config);
    StructuredTextViewer stv = getTextViewer();
    if (stv != null) {
      /*
       * There should be no need to unconfigure before configure because configure will also
       * unconfigure before configuring
       */
      removeReconcilingListeners(oldSourceViewerConfiguration, stv);
      stv.unconfigure();
      setStatusLineMessage(null);
      stv.configure(config);
      addReconcilingListeners(config, stv);
    }
  }

  @Override
  protected void uninstallTextDragAndDrop(ISourceViewer viewer) {
    // do nothing
  }

  /**
   * Updates all content dependent actions.
   */
  @Override
  protected void updateContentDependentActions() {
    super.updateContentDependentActions();
    // super.updateContentDependentActions only updates
    // the enable/disable
    // state of all
    // the content dependent actions.
    // StructuredTextEditor's undo and redo actions
    // have a detail label and
    // description.
    // They needed to be updated.
    if (!fEditorDisposed) {
      updateMenuText();
    }
  }

  @Override
  protected void updateStatusField(String category) {
    super.updateStatusField(category);

    if (category == null) {
      return;
    }

    if (StructuredTextEditorActionConstants.STATUS_CATEGORY_OFFSET.equals(category)) {
      IStatusField field = getStatusField(category);
      ISourceViewer sourceViewer = getSourceViewer();
      if (field != null && sourceViewer != null) {
        Point selection = sourceViewer.getTextWidget().getSelection();
        int offset1 = widgetOffset2ModelOffset(sourceViewer, selection.x);
        int offset2 = widgetOffset2ModelOffset(sourceViewer, selection.y);
        String text = null;
        if (offset1 != offset2) {
          text = "[" + offset1 + "-" + offset2 + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else {
          text = "[ " + offset1 + " ]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        field.setText(text == null ? fErrorLabel : text);
      }
    }
  }

  /**
   * Starts background mode.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   */
  void beginBackgroundOperation() {
    fBackgroundJobEnded = false;
    // if already in busy state, no need to do anything
    // and, we only start, or reset, the timed busy
    // state when we get the "endBackgroundOperation" call.
    if (!inBusyState()) {
      beginBusyStateInternal();
    }
  }

  /**
   * End background mode.
   * <p>
   * Not API. May be removed in the future.
   * </p>
   */
  void endBackgroundOperation() {
    fBackgroundJobEnded = true;
    // note, we don't immediately end our 'internal busy' state,
    // since we may get many calls in a short period of
    // time. We always wait for the time out.
    resetBusyState();
  }

  Display getDisplay() {
    return PlatformUI.getWorkbench().getDisplay();
  }

  IStructuredModel getInternalModel() {
    return fStructuredModel;
  }

  /**
   * Returns the signed current selection. The length will be negative if the resulting selection is
   * right-to-left (RtoL).
   * <p>
   * The selection offset is model based.
   * </p>
   * 
   * @param sourceViewer the source viewer
   * @return a region denoting the current signed selection, for a resulting RtoL selections length
   *         is < 0
   */
  IRegion getSignedSelection(ISourceViewer sourceViewer) {
    StyledText text = sourceViewer.getTextWidget();
    Point selection = text.getSelectionRange();

    if (text.getCaretOffset() == selection.x) {
      selection.x = selection.x + selection.y;
      selection.y = -selection.y;
    }

    selection.x = widgetOffset2ModelOffset(sourceViewer, selection.x);

    return new Region(selection.x, selection.y);
  }

  /**
   * Jumps to the matching bracket.
   */
  void gotoMatchingBracket() {
    ICharacterPairMatcher matcher = createCharacterPairMatcher();
    if (matcher == null) {
      return;
    }

    ISourceViewer sourceViewer = getSourceViewer();
    IDocument document = sourceViewer.getDocument();
    if (document == null) {
      return;
    }

    IRegion selection = getSignedSelection(sourceViewer);

    int selectionLength = Math.abs(selection.getLength());
    if (selectionLength > 1) {
      setStatusLineErrorMessage(SSEUIMessages.GotoMatchingBracket_error_invalidSelection);
      sourceViewer.getTextWidget().getDisplay().beep();
      return;
    }

    int sourceCaretOffset = selection.getOffset() + selection.getLength();
    IRegion region = matcher.match(document, sourceCaretOffset);
    if (region == null) {
      setStatusLineErrorMessage(SSEUIMessages.GotoMatchingBracket_error_noMatchingBracket);
      sourceViewer.getTextWidget().getDisplay().beep();
      return;
    }

    int offset = region.getOffset();
    int length = region.getLength();

    if (length < 1) {
      return;
    }

    int anchor = matcher.getAnchor();

    // go to after the match if matching to the right
    int targetOffset = (ICharacterPairMatcher.RIGHT == anchor) ? offset : offset + length;

    boolean visible = false;
    if (sourceViewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) sourceViewer;
      visible = (extension.modelOffset2WidgetOffset(targetOffset) > -1);
    } else {
      IRegion visibleRegion = sourceViewer.getVisibleRegion();
      // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
      visible = (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset()
          + visibleRegion.getLength());
    }

    if (!visible) {
      setStatusLineErrorMessage(SSEUIMessages.GotoMatchingBracket_error_bracketOutsideSelectedElement);
      sourceViewer.getTextWidget().getDisplay().beep();
      return;
    }

    if (selection.getLength() < 0) {
      targetOffset -= selection.getLength();
    }

    if (sourceViewer != null) {
      sourceViewer.setSelectedRange(targetOffset, selection.getLength());
      sourceViewer.revealRange(targetOffset, selection.getLength());
    }
  }

  void updateRangeIndication(ISelection selection) {
    boolean rangeUpdated = false;
    if (selection instanceof IStructuredSelection && !((IStructuredSelection) selection).isEmpty()) {
      Object[] objects = ((IStructuredSelection) selection).toArray();
      if (objects.length > 0 && objects[0] instanceof IndexedRegion) {
        int start = ((IndexedRegion) objects[0]).getStartOffset();
        int end = ((IndexedRegion) objects[0]).getEndOffset();
        if (objects.length > 1) {
          for (int i = 1; i < objects.length; i++) {
            start = Math.min(start, ((IndexedRegion) objects[i]).getStartOffset());
            end = Math.max(end, ((IndexedRegion) objects[i]).getEndOffset());
          }
        }
        getSourceViewer().setRangeIndication(start, end - start, false);
        rangeUpdated = true;
      }
    }
    if (!rangeUpdated && getSourceViewer() != null) {
      if (selection instanceof ITextSelection) {
        getSourceViewer().setRangeIndication(((ITextSelection) selection).getOffset(),
            ((ITextSelection) selection).getLength(), false);
      } else {
        getSourceViewer().removeRangeIndication();
      }
    }
  }

  void updateStatusLine(ISelection selection) {
    // Bug 210481 - Don't update the status line if the selection
    // was caused by go to navigation
    if (fSelectionChangedFromGoto) {
      fSelectionChangedFromGoto = false;
      return;
    }
    IStatusLineManager statusLineManager = getEditorSite().getActionBars().getStatusLineManager();
    if (fStatusLineLabelProvider != null && statusLineManager != null) {
      String text = null;
      Image image = null;
      if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
        Object firstElement = ((IStructuredSelection) selection).getFirstElement();
        if (firstElement != null) {
          text = fStatusLineLabelProvider.getText(firstElement);
          image = fStatusLineLabelProvider.getImage((firstElement));
        }
      }
      if (image == null) {
        statusLineManager.setMessage(text);
      } else {
        statusLineManager.setMessage(image, text);
      }
    }
  }

  private IStructuredModel aboutToSaveModel() {
    IStructuredModel model = getInternalModel();
    if (model != null) {
      model.aboutToChangeModel();
    }
    return model;
  }

  private void activateContexts(IContextService service) {
    if (service == null) {
      return;
    }

    String[] definitions = getDefinitions(getConfigurationPoints());

    if (definitions != null) {
      String[] contexts = null;
      for (int i = 0; i < definitions.length; i++) {
        contexts = StringUtils.unpack(definitions[i]);
        for (int j = 0; j < contexts.length; j++) {
          service.activateContext(contexts[j].trim());
        }
      }
    }

  }

  private void addExtendedContextMenuActions(IMenuManager menu) {
    IEditorActionBarContributor c = getEditorSite().getActionBarContributor();
    if (c instanceof IPopupMenuContributor) {
      ((IPopupMenuContributor) c).contributeToPopupMenu(menu);
    } else {
      ExtendedEditorActionBuilder builder = new ExtendedEditorActionBuilder();
      IExtendedContributor pmc = builder.readActionExtensions(getConfigurationPoints());
      if (pmc != null) {
        pmc.setActiveEditor(this);
        pmc.contributeToPopupMenu(menu);
      }
    }
  }

  private void addReconcilingListeners(SourceViewerConfiguration config, StructuredTextViewer stv) {
    try {
      List reconcilingListeners = new ArrayList(fReconcilingListeners.length);
      String[] ids = getConfigurationPoints();
      for (int i = 0; i < ids.length; i++) {
        reconcilingListeners.addAll(ExtendedConfigurationBuilder.getInstance().getConfigurations(
            "sourceReconcilingListener", ids[i])); //$NON-NLS-1$
      }
      fReconcilingListeners = (ISourceReconcilingListener[]) reconcilingListeners.toArray(new ISourceReconcilingListener[reconcilingListeners.size()]);
    } catch (ClassCastException e) {
      Logger.log(Logger.ERROR,
          "Configuration has a reconciling listener that does not implement ISourceReconcilingListener."); //$NON-NLS-1$
    }

    IReconciler reconciler = config.getReconciler(stv);
    if (reconciler instanceof DocumentRegionProcessor) {
      for (int i = 0; i < fReconcilingListeners.length; i++) {
        ((DocumentRegionProcessor) reconciler).addReconcilingListener(fReconcilingListeners[i]);
      }
    }
  }

  private void beginBusyStateInternal() {

    fBusyState = true;
    startBusyTimer();

    ISourceViewer viewer = getSourceViewer();
    if (viewer instanceof StructuredTextViewer) {
      ((StructuredTextViewer) viewer).beginBackgroundUpdate();

    }
    showBusy(true);
  }

  /**
   * Calculates the priority of the target content type. The closer <code>targetType</code> is to
   * <code>type</code> the higher its priority.
   * 
   * @param type
   * @param targetType
   * @param priority
   * @return
   */
  private int calculatePriority(IContentType type, IContentType targetType, int priority) {
    if (type == null || targetType == null) {
      return -1;
    }
    if (type.getId().equals(targetType.getId())) {
      return priority;
    }
    return calculatePriority(type.getBaseType(), targetType, ++priority);
  }

  /**
   * Compute and set double-click action for the vertical ruler
   */
  private void computeAndSetDoubleClickAction() {
    /*
     * Make double-clicking on the ruler toggle a breakpoint instead of toggling a bookmark. For
     * lines where a breakpoint won't be created, create a bookmark through the contributed
     * RulerDoubleClick action.
     */
    setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK, new ToggleBreakpointAction(this,
        getVerticalRuler(), getAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK)));
  }

  private IInformationPresenter configureOutlinePresenter(ISourceViewer sourceViewer,
      SourceViewerConfiguration config) {
    InformationPresenter presenter = null;

    // Get the quick outline configuration
    AbstractQuickOutlineConfiguration cfg = null;
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] ids = getConfigurationPoints();
    for (int i = 0; cfg == null && i < ids.length; i++) {
      cfg = (AbstractQuickOutlineConfiguration) builder.getConfiguration(
          ExtendedConfigurationBuilder.QUICKOUTLINECONFIGURATION, ids[i]);
    }

    if (cfg != null) {
      presenter = new InformationPresenter(getOutlinePresenterControlCreator(cfg));
      presenter.setDocumentPartitioning(config.getConfiguredDocumentPartitioning(sourceViewer));
      presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
      IInformationProvider provider = new SourceInfoProvider(this);
      String[] contentTypes = config.getConfiguredContentTypes(sourceViewer);
      for (int i = 0; i < contentTypes.length; i++) {
        presenter.setInformationProvider(provider, contentTypes[i]);
      }
      presenter.setSizeConstraints(50, 20, true, false);
    }
    return presenter;
  }

  /**
   * Create a preference store that combines the source editor preferences with the base editor's
   * preferences.
   * 
   * @return IPreferenceStore
   */
  private IPreferenceStore createCombinedPreferenceStore() {
    IPreferenceStore sseEditorPrefs = SSEUIPlugin.getDefault().getPreferenceStore();
    IPreferenceStore baseEditorPrefs = EditorsUI.getPreferenceStore();
    return new ChainedPreferenceStore(new IPreferenceStore[] {sseEditorPrefs, baseEditorPrefs});
  }

  private ContentOutlineConfiguration createContentOutlineConfiguration() {
    ContentOutlineConfiguration cfg = null;
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] ids = getConfigurationPoints();
    for (int i = 0; cfg == null && i < ids.length; i++) {
      cfg = (ContentOutlineConfiguration) builder.getConfiguration(
          ExtendedConfigurationBuilder.CONTENTOUTLINECONFIGURATION, ids[i]);
    }
    return cfg;
  }

  /**
   * Loads the Show In Target IDs from the Extended Configuration extension point.
   * 
   * @return
   */
  private String[] createShowInTargetIds() {
    List allIds = new ArrayList(0);
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] configurationIds = getConfigurationPoints();
    for (int i = 0; i < configurationIds.length; i++) {
      String[] definitions = builder.getDefinitions("showintarget", configurationIds[i]); //$NON-NLS-1$
      for (int j = 0; j < definitions.length; j++) {
        String someIds = definitions[j];
        if (someIds != null && someIds.length() > 0) {
          String[] ids = StringUtils.unpack(someIds);
          for (int k = 0; k < ids.length; k++) {
            // trim, just to keep things clean
            String id = ids[k].trim();
            if (!allIds.contains(id)) {
              allIds.add(id);
            }
          }
        }
      }
    }

    if (!allIds.contains(IPageLayout.ID_RES_NAV)) {
      allIds.add(IPageLayout.ID_RES_NAV);
    }
    if (!allIds.contains(IPageLayout.ID_PROJECT_EXPLORER)) {
      allIds.add(IPageLayout.ID_PROJECT_EXPLORER);
    }
    if (!allIds.contains(IPageLayout.ID_OUTLINE)) {
      allIds.add(IPageLayout.ID_OUTLINE);
    }
    return (String[]) allIds.toArray(new String[0]);
  }

  /**
   * @return
   */
  private ISourceEditingTextTools createSourceEditingTextTools() {
    ISourceEditingTextTools tools = null;
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] ids = getConfigurationPoints();
    for (int i = 0; tools == null && i < ids.length; i++) {
      tools = (ISourceEditingTextTools) builder.getConfiguration(NullSourceEditingTextTools.ID,
          ids[i]);
    }
    if (tools == null) {
      tools = NullSourceEditingTextTools.getInstance();
      ((NullSourceEditingTextTools) tools).setTextEditor(this);
    }
    Method method = null; //$NON-NLS-1$
    try {
      method = tools.getClass().getMethod("setTextEditor", new Class[] {StructuredTextEditor.class}); //$NON-NLS-1$
    } catch (NoSuchMethodException e) {
    }
    if (method == null) {
      try {
        method = tools.getClass().getMethod("setTextEditor", new Class[] {ITextEditor.class}); //$NON-NLS-1$
      } catch (NoSuchMethodException e) {
      }
    }
    if (method == null) {
      try {
        method = tools.getClass().getMethod("setTextEditor", new Class[] {IEditorPart.class}); //$NON-NLS-1$
      } catch (NoSuchMethodException e) {
      }
    }
    if (method != null) {
      if (!method.isAccessible()) {
        method.setAccessible(true);
      }
      try {
        method.invoke(tools, new Object[] {this});
      } catch (Exception e) {
        Logger.logException("Problem creating ISourceEditingTextTools implementation", e); //$NON-NLS-1$
      }
    }

    return tools;
  }

  private ConfigurationAndTarget createSourceViewerConfiguration() {
    ConfigurationAndTarget cat = null;
    StructuredTextViewerConfiguration cfg = null;
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] ids = getConfigurationPoints();
    for (int i = 0; cfg == null && i < ids.length; i++) {
      cfg = (StructuredTextViewerConfiguration) builder.getConfiguration(
          ExtendedConfigurationBuilder.SOURCEVIEWERCONFIGURATION, ids[i]);
      cat = new ConfigurationAndTarget(ids[i], cfg);
    }
    if (cfg == null) {
      cfg = new StructuredTextViewerConfiguration();
      String targetid = getClass().getName() + "#default"; //$NON-NLS-1$
      cat = new ConfigurationAndTarget(targetid, cfg);
    }
    return cat;
  }

  /**
   * Disposes model specific editor helpers such as statusLineHelper. Basically any code repeated in
   * update() & dispose() should be placed here.
   */
  private void disposeModelDependentFields() {
    if (fStructuredSelectionProvider != null) {
      fStructuredSelectionProvider.selectionConvertor = new SelectionConvertor();
    }
  }

  /**
   * Note this method can be called indirectly from background job operation ... but expected to be
   * gaurded there with ILock, plus, can be called directly from timer thread, so the timer's run
   * method guards with ILock too. Set result[0] to 1 if the busy state was ended successfully
   */
  private void endBusyStateInternal(byte[] result) {
    if (fBackgroundJobEnded) {
      result[0] = 1;
      showBusy(false);

      ISourceViewer viewer = getSourceViewer();
      if (viewer instanceof StructuredTextViewer) {
        ((StructuredTextViewer) viewer).endBackgroundUpdate();
      }
      fBusyState = false;
    } else {
      // we will only be in this branch for a back ground job that is
      // taking
      // longer than our normal time-out period (meaning we got notified
      // of
      // the timeout "inbetween" calls to 'begin' and
      // 'endBackgroundOperation'.
      // (which, remember, can only happen since there are many calls to
      // begin/end in a short period of time, and we only "reset" on the
      // 'ends').
      // In this event, there's really nothing to do, we're still in
      // "busy state"
      // and should start a new reset cycle once endBackgroundjob is
      // called.
    }
  }

  private String[] getConfigurationPoints() {
    String contentTypeIdentifierID = null;
    if (getInternalModel() != null) {
      contentTypeIdentifierID = getInternalModel().getContentTypeIdentifier();
    }
    return ConfigurationPointCalculator.getConfigurationPoints(this, contentTypeIdentifierID,
        ConfigurationPointCalculator.SOURCE, StructuredTextEditor.class);
  }

  private String[] getDefinitions(String[] ids) {
    ExtendedConfigurationBuilder builder = ExtendedConfigurationBuilder.getInstance();
    String[] definitions = null;

    /*
     * Iterate through the configuration ids until one is found that has an activecontexts
     * definition
     */
    for (int i = 0; i < ids.length; i++) {
      definitions = builder.getDefinitions("activecontexts", ids[i]); //$NON-NLS-1$
      if (definitions != null && definitions.length > 0) {
        return definitions;
      }
    }
    return null;
  }

  private InternalModelStateListener getInternalModelStateListener() {
    if (fInternalModelStateListener == null) {
      fInternalModelStateListener = new InternalModelStateListener();
    }
    return fInternalModelStateListener;
  }

  private LineNumberColumn getLineColumn() {
    String var = "fLineColumn";
    return (LineNumberColumn) ReflectionUtils.getFieldObject(this, var);
  }

  /**
   * Returns the outline presenter control creator. The creator is a factory creating outline
   * presenter controls for the given source viewer.
   * 
   * @param sourceViewer the source viewer to be configured by this configuration
   * @return an information control creator
   */
  private IInformationControlCreator getOutlinePresenterControlCreator(
      final AbstractQuickOutlineConfiguration config) {
    return new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        int shellStyle = SWT.RESIZE;
        return new QuickOutlinePopupDialog(parent, shellStyle, getInternalModel(), config);
      }
    };
  }

  private List<Object> getRulerContextMenuListeners() {
    String var = "fRulerContextMenuListeners";
    return (List<Object>) ReflectionUtils.getFieldObject(this, var);
  }

  private boolean inBusyState() {
    return fBusyState;
  }

  /**
   * Initializes the editor's source viewer and other items that were source viewer-dependent.
   */
  private void initializeSourceViewer() {
    IAction openHyperlinkAction = getAction(StructuredTextEditorActionConstants.ACTION_NAME_OPEN_FILE);
    if (openHyperlinkAction instanceof OpenHyperlinkAction) {
      ((OpenHyperlinkAction) openHyperlinkAction).setHyperlinkDetectors(getSourceViewerConfiguration().getHyperlinkDetectors(
          getSourceViewer()));
    }

    // do not even install projection support until folding is actually
    // enabled
    if (isFoldingEnabled()) {
      installProjectionSupport();
    }
  }

  private void installCharacterPairing() {
    IStructuredModel model = getInternalModel();
    if (model != null) {
      IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
          SSEUIPlugin.ID, "characterPairing"); //$NON-NLS-1$
      IContentTypeManager mgr = Platform.getContentTypeManager();
      IContentType type = mgr.getContentType(model.getContentTypeIdentifier());
      if (type != null) {
        for (int i = 0; i < elements.length; i++) {
          // Create the inserter
          IConfigurationElement element = elements[i];
          try {
            IConfigurationElement[] contentTypes = element.getChildren("contentTypeIdentifier");
            for (int j = 0; j < contentTypes.length; j++) {
              String id = contentTypes[j].getAttribute("id");
              if (id != null) {
                IContentType targetType = mgr.getContentType(id);
                int priority = calculatePriority(type, targetType, 0);
                if (priority >= 0) {
                  final CharacterPairing pairing = new CharacterPairing();
                  pairing.priority = priority;
                  String[] partitions = StringUtils.unpack(contentTypes[j].getAttribute("partitions"));
                  pairing.partitions = new HashSet(partitions.length);
                  // Only add the inserter if there is at least one partition for the content type
                  for (int k = 0; k < partitions.length; k++) {
                    pairing.partitions.add(partitions[k]);
                  }

                  pairing.inserter = (AbstractCharacterPairInserter) element.createExecutableExtension("class");
                  if (pairing.inserter != null && partitions.length > 0) {
                    fPairInserter.addInserter(pairing);
                    /* use a SafeRunner since this method is also invoked during Part creation */
                    SafeRunner.run(new ISafeRunnable() {
                      @Override
                      public void handleException(Throwable exception) {
                        // rely on default logging
                      }

                      @Override
                      public void run() throws Exception {
                        pairing.inserter.initialize();
                      }
                    });
                  }
                }
              }
            }
          } catch (CoreException e) {
            Logger.logException(e);
          }
        }
        fPairInserter.prioritize();
      }
    }
  }

  /**
   * Install everything necessary to get document folding working and enable document folding
   */
  private void installProjectionSupport() {
    ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();

    fProjectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(),
        getSharedColors());
    fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error"); //$NON-NLS-1$
    fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning"); //$NON-NLS-1$
    fProjectionSupport.setHoverControlCreator(new IInformationControlCreator() {
      @Override
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent);
      }
    });
    fProjectionSupport.install();

    if (isFoldingEnabled()) {
      projectionViewer.doOperation(ProjectionViewer.TOGGLE);
    }
  }

  /**
   * Installs semantic highlighting on the editor
   */
  private void installSemanticHighlighting() {
    IStructuredModel model = getInternalModel();
    if (fSemanticManager == null && model != null) {
      fSemanticManager = new SemanticHighlightingManager();
      fSemanticManager.install(getSourceViewer(), getPreferenceStore(),
          getSourceViewerConfiguration(), model.getContentTypeIdentifier());
    }
  }

  /**
   * Return whether document folding should be enabled according to the preference store settings.
   * 
   * @return <code>true</code> if document folding should be enabled
   */
  private boolean isFoldingEnabled() {
    IPreferenceStore store = getPreferenceStore();
    // check both preference store and vm argument
    return (store.getBoolean(AbstractStructuredFoldingStrategy.FOLDING_ENABLED));
  }

  private boolean isRemoveTrailingWhitespaceEnabled() {
    return PreferenceConstants.getPreferenceStore().getBoolean(
        PreferenceConstants.EDITOR_REMOVE_TRAILING_WS);
  }

  /**
   * Determine if the user preference for as you type validation is enabled or not
   */
  private boolean isValidationEnabled() {
    return getPreferenceStore().getBoolean(CommonEditorPreferenceNames.EVALUATE_TEMPORARY_PROBLEMS);
  }

  /**
   * <p>
   * Logs a warning about how this {@link StructuredTextEditor} just opened an {@link IEditorInput}
   * it was not designed to open.
   * </p>
   * 
   * @param input the {@link IEditorInput} this {@link StructuredTextEditor} was not designed to
   *          open to log the message about.
   */
  private void logUnexpectedDocumentKind(IEditorInput input) {
    Logger.log(Logger.WARNING, "StructuredTextEditor being used without StructuredDocument"); //$NON-NLS-1$
    String name = null;
    if (input != null) {
      name = input.getName();
    } else {
      name = "input was null"; //$NON-NLS-1$
    }
    Logger.log(Logger.WARNING, "         Input Name: " + name); //$NON-NLS-1$
    String implClass = null;
    IDocument document = getDocumentProvider().getDocument(input);
    if (document != null) {
      implClass = document.getClass().getName();
    } else {
      implClass = "document was null"; //$NON-NLS-1$
    }
    Logger.log(
        Logger.WARNING,
        "        Unexpected IDocumentProvider implementation: " + getDocumentProvider().getClass().getName()); //$NON-NLS-1$
    Logger.log(Logger.WARNING, "        Unexpected IDocument implementation: " + implClass); //$NON-NLS-1$
  }

  private void performSaveActions() {
    if (isRemoveTrailingWhitespaceEnabled()) {
      try {
        removeTrailingWhitespaceAction.run();
      } catch (InvocationTargetException e) {
        Logger.logException(e);
      }
    }
  }

  private void removeReconcilingListeners(SourceViewerConfiguration config, StructuredTextViewer stv) {
    IReconciler reconciler = config.getReconciler(stv);
    if (reconciler instanceof DocumentRegionProcessor) {
      for (int i = 0; i < fReconcilingListeners.length; i++) {
        ((DocumentRegionProcessor) reconciler).removeReconcilingListener(fReconcilingListeners[i]);
      }
    }
  }

  /**
   * both starts and resets the busy state timer
   */
  private void resetBusyState() {
    // reset the "busy" timeout
    if (fBusyTimer != null) {
      fBusyTimer.cancel();
    }
    startBusyTimer();
  }

  private void savedModel(IStructuredModel model) {
    if (model != null) {
      model.changedModel();
    }
  }

  /**
   * Sets the model field within this editor.
   * 
   * @deprecated - can eventually be eliminated
   */
  @Deprecated
  private void setModel(IStructuredModel newModel) {
    Assert.isNotNull(getDocumentProvider(), "document provider can not be null when setting model"); //$NON-NLS-1$
    if (fStructuredModel != null) {
      fStructuredModel.removeModelStateListener(getInternalModelStateListener());
    }
    fStructuredModel = newModel;
    if (fStructuredModel != null) {
      fStructuredModel.addModelStateListener(getInternalModelStateListener());
    }
    // update() should be called whenever the model is
    // set or changed
    update();
  }

  private void startBusyTimer() {
    // TODO: we need a resettable timer, so not so
    // many are created
    fBusyTimer = new Timer(true);
    fBusyTimer.schedule(new TimeOutExpired(), BUSY_STATE_DELAY);
  }

  /**
   * <p>
   * Attempts to get the {@link IStructuredModel} for the given {@link IEditorInput}
   * </p>
   * 
   * @param input the {@link IEditorInput} to try and get the {@link IStructuredModel} for
   * @return The {@link IStructuredModel} associated with the given {@link IEditorInput} or
   *         <code>null</code> if no associated {@link IStructuredModel} could be found.
   */
  private IStructuredModel tryToGetModel(IEditorInput input) {
    IStructuredModel model = null;

    IDocument newDocument = getDocumentProvider().getDocument(input);
    if (newDocument instanceof IExecutionDelegatable) {
      ((IExecutionDelegatable) newDocument).setExecutionDelegate(new EditorExecutionContext(this));
    }

    // if we have a Model provider, get the model from it
    if (getDocumentProvider() instanceof IModelProvider) {
      model = ((IModelProvider) getDocumentProvider()).getModel(getEditorInput());
      if (!model.isShared()) {
        EditorModelUtil.addFactoriesTo(model);
      }
    } else if (newDocument instanceof IStructuredDocument) {
      // corresponding releaseFromEdit occurs in dispose()
      model = StructuredModelManager.getModelManager().getModelForEdit(
          (IStructuredDocument) newDocument);
      EditorModelUtil.addFactoriesTo(model);
    }

    return model;
  }

  /**
   * Uninstalls semantic highlighting on the editor and performs cleanup
   */
  private void uninstallSemanticHighlighting() {
    if (fSemanticManager != null) {
      fSemanticManager.uninstall();
      fSemanticManager = null;
    }
  }

  /**
   * Updates the editor context menu by creating a new context menu with the given menu id
   * 
   * @param contextMenuId Cannot be null
   */
  private void updateEditorContextMenuId(String contextMenuId) {
    // update editor context menu id if updating to a new id or if context
    // menu is not already set up
    if (!contextMenuId.equals(getEditorContextMenuId()) || (fTextContextMenu == null)) {
      setEditorContextMenuId(contextMenuId);

      if (getSourceViewer() != null) {
        StyledText styledText = getSourceViewer().getTextWidget();
        if (styledText != null) {
          // dispose of previous context menu
          if (fTextContextMenu != null) {
            fTextContextMenu.dispose();
          }
          if (fTextContextMenuManager != null) {
            fTextContextMenuManager.removeMenuListener(getContextMenuListener());
            fTextContextMenuManager.removeAll();
            fTextContextMenuManager.dispose();
          }

          fTextContextMenuManager = new MenuManager(getEditorContextMenuId(),
              getEditorContextMenuId());
          fTextContextMenuManager.setRemoveAllWhenShown(true);
          fTextContextMenuManager.addMenuListener(getContextMenuListener());

          fTextContextMenu = fTextContextMenuManager.createContextMenu(styledText);
          styledText.setMenu(fTextContextMenu);

          getSite().registerContextMenu(getEditorContextMenuId(), fTextContextMenuManager,
              getSelectionProvider());

          // also register this menu for source page part and
          // structured text editor ids
          String partId = getSite().getId();
          if (partId != null) {
            getSite().registerContextMenu(partId + EDITOR_CONTEXT_MENU_SUFFIX,
                fTextContextMenuManager, getSelectionProvider());
          }
          getSite().registerContextMenu(EDITOR_CONTEXT_MENU_ID, fTextContextMenuManager,
              getSelectionProvider());
        }
      }
    }
  }

  /**
   * Updates editor context menu, vertical ruler menu, help context id for new content type
   * 
   * @param contentType
   */
  private void updateEditorControlsForContentType(String contentType) {
    if (contentType == null) {
      updateEditorContextMenuId(EDITOR_CONTEXT_MENU_ID);
      updateRulerContextMenuId(RULER_CONTEXT_MENU_ID);
      updateHelpContextId(ITextEditorHelpContextIds.TEXT_EDITOR);
    } else {
      updateEditorContextMenuId(contentType + EDITOR_CONTEXT_MENU_SUFFIX);
      updateRulerContextMenuId(contentType + RULER_CONTEXT_MENU_SUFFIX);
      updateHelpContextId(contentType + "_source_HelpId"); //$NON-NLS-1$

      /* Activate the contexts defined for this editor */
      activateContexts((IContextService) getSite().getService(IContextService.class));
    }
  }

  private void updateEncodingMemento() {
    boolean failed = false;
    IStructuredModel internalModel = getInternalModel();
    if (internalModel != null) {
      IStructuredDocument doc = internalModel.getStructuredDocument();
      EncodingMemento memento = doc.getEncodingMemento();
      IDocumentCharsetDetector detector = internalModel.getModelHandler().getEncodingDetector();
      if (memento != null && detector != null) {
        detector.set(doc);
        try {
          String newEncoding = detector.getEncoding();
          if (newEncoding != null) {
            memento.setDetectedCharsetName(newEncoding);
          }
        } catch (IOException e) {
          failed = true;
        }
      }
      /**
       * Be sure to use the new value but only if no exception occurred. (we may find cases we need
       * to do more error recovery there) should be near impossible to get IOException from
       * processing the _document_
       */
      if (!failed) {
        doc.setEncodingMemento(memento);
      }
    }
  }

  /**
   * Updates the help context of the editor with the given help context id
   * 
   * @param helpContextId Cannot be null
   */
  private void updateHelpContextId(String helpContextId) {
    if (!helpContextId.equals(getHelpContextId())) {
      setHelpContextId(helpContextId);

      if (getSourceViewer() != null) {
        StyledText styledText = getSourceViewer().getTextWidget();
        if (styledText != null) {
          IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
          helpSystem.setHelp(styledText, getHelpContextId());
        }
      }
    }
  }

  /*
   * Update the hovering behavior depending on the preferences.
   */
  private void updateHoverBehavior() {
    SourceViewerConfiguration configuration = getSourceViewerConfiguration();
    String[] types = configuration.getConfiguredContentTypes(getSourceViewer());

    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer == null) {
      return;
    }

    for (int i = 0; i < types.length; i++) {

      String t = types[i];

      if (sourceViewer instanceof ITextViewerExtension2) {
        // Remove existing hovers
        ((ITextViewerExtension2) sourceViewer).removeTextHovers(t);

        int[] stateMasks = configuration.getConfiguredTextHoverStateMasks(getSourceViewer(), t);

        if (stateMasks != null) {
          for (int j = 0; j < stateMasks.length; j++) {
            int stateMask = stateMasks[j];
            ITextHover textHover = configuration.getTextHover(sourceViewer, t, stateMask);
            ((ITextViewerExtension2) sourceViewer).setTextHover(textHover, t, stateMask);
          }
        } else {
          ITextHover textHover = configuration.getTextHover(sourceViewer, t);
          ((ITextViewerExtension2) sourceViewer).setTextHover(textHover, t,
              ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
        }
      } else {
        sourceViewer.setTextHover(configuration.getTextHover(sourceViewer, t), t);
      }
    }
  }

  private void updateMenuText() {
    ITextViewer viewer = getTextViewer();
    StyledText widget = null;
    if (viewer != null) {
      widget = viewer.getTextWidget();
    }

    if (fStructuredModel != null && !fStructuredModel.isModelStateChanging() && viewer != null
        && widget != null && !widget.isDisposed()) {
      // performance: don't force an update of the action bars unless
      // required as it is expensive
      String previousUndoText = null;
      String previousUndoDesc = null;
      String previousRedoText = null;
      String previousRedoDesc = null;
      boolean updateActions = false;
      IAction undoAction = getAction(ITextEditorActionConstants.UNDO);
      IAction redoAction = getAction(ITextEditorActionConstants.REDO);
      if (undoAction != null) {
        previousUndoText = undoAction.getText();
        previousUndoDesc = undoAction.getDescription();
        updateActions = updateActions || previousUndoText == null || previousUndoDesc == null;
        undoAction.setText(UNDO_ACTION_TEXT_DEFAULT);
        undoAction.setDescription(UNDO_ACTION_DESC_DEFAULT);
      }
      if (redoAction != null) {
        previousRedoText = redoAction.getText();
        previousRedoDesc = redoAction.getDescription();
        updateActions = updateActions || previousRedoText == null || previousRedoDesc == null;
        redoAction.setText(REDO_ACTION_TEXT_DEFAULT);
        redoAction.setDescription(REDO_ACTION_DESC_DEFAULT);
      }
      if (fStructuredModel.getUndoManager() != null) {
        IStructuredTextUndoManager undoManager = fStructuredModel.getUndoManager();
        // get undo command
        Command undoCommand = undoManager.getUndoCommand();
        // set undo label and description
        if (undoAction != null) {
          undoAction.setEnabled(undoManager.undoable());
          if (undoCommand != null) {
            String label = undoCommand.getLabel();
            if (label != null) {
              String customText = MessageFormat.format(UNDO_ACTION_TEXT, new String[] {label});
              updateActions = updateActions || customText == null || previousUndoText == null
                  || !customText.equals(previousUndoText);
              undoAction.setText(customText);
            }
            String desc = undoCommand.getDescription();
            if (desc != null) {
              String customDesc = MessageFormat.format(UNDO_ACTION_DESC, new String[] {desc});
              updateActions = updateActions || customDesc == null || previousRedoDesc == null
                  || !customDesc.equals(previousUndoDesc);
              undoAction.setDescription(customDesc);
            }
          }
        }
        // get redo command
        Command redoCommand = undoManager.getRedoCommand();
        // set redo label and description
        if (redoAction != null) {
          redoAction.setEnabled(undoManager.redoable());
          if (redoCommand != null) {
            String label = redoCommand.getLabel();
            if (label != null) {
              String customText = MessageFormat.format(REDO_ACTION_TEXT, new String[] {label});
              updateActions = updateActions || customText == null || previousRedoText == null
                  || !customText.equals(previousRedoText);
              redoAction.setText(customText);
            }
            String desc = redoCommand.getDescription();
            if (desc != null) {
              String customDesc = MessageFormat.format(REDO_ACTION_DESC, new String[] {desc});
              updateActions = updateActions || customDesc == null || previousRedoDesc == null
                  || !customDesc.equals(previousRedoDesc);
              redoAction.setDescription(customDesc);
            }
          }
        }
      }
      // tell the action bars to update
      if (updateActions) {
        if (getEditorSite().getActionBars() != null) {
          getEditorSite().getActionBars().updateActionBars();
        } else if (getEditorPart() != null
            && getEditorPart().getEditorSite().getActionBars() != null) {
          getEditorPart().getEditorSite().getActionBars().updateActionBars();
        }
      }
    }
  }

  /**
   * Updates the editor vertical ruler menu by creating a new vertical ruler context menu with the
   * given menu id
   * 
   * @param rulerMenuId Cannot be null
   */
  private void updateRulerContextMenuId(String rulerMenuId) {
    // update ruler context menu id if updating to a new id or if context
    // menu is not already set up
    if (!rulerMenuId.equals(getRulerContextMenuId()) || (fRulerContextMenu == null)) {
      setRulerContextMenuId(rulerMenuId);

      if (getVerticalRuler() != null) {
        // dispose of previous ruler context menu
        if (fRulerContextMenu != null) {
          fRulerContextMenu.dispose();
        }
        if (fRulerContextMenuManager != null) {
          fRulerContextMenuManager.removeMenuListener(getContextMenuListener());
          fRulerContextMenuManager.removeAll();
          fRulerContextMenuManager.dispose();
        }

        fRulerContextMenuManager = new MenuManager(getRulerContextMenuId(), getRulerContextMenuId());
        fRulerContextMenuManager.setRemoveAllWhenShown(true);
        fRulerContextMenuManager.addMenuListener(getContextMenuListener());

        Control rulerControl = getVerticalRuler().getControl();
        fRulerContextMenu = fRulerContextMenuManager.createContextMenu(rulerControl);
        rulerControl.setMenu(fRulerContextMenu);

        getSite().registerContextMenu(getRulerContextMenuId(), fRulerContextMenuManager,
            getSelectionProvider());

        // also register this menu for source page part and structured
        // text editor ids
        String partId = getSite().getId();
        if (partId != null) {
          getSite().registerContextMenu(partId + RULER_CONTEXT_MENU_SUFFIX,
              fRulerContextMenuManager, getSelectionProvider());
        }
        getSite().registerContextMenu(RULER_CONTEXT_MENU_ID, fRulerContextMenuManager,
            getSelectionProvider());
      }
    }
  }

  private void updateSourceViewerConfiguration() {
    SourceViewerConfiguration configuration = getSourceViewerConfiguration();
    // no need to update source viewer configuration if one does not exist
    // yet
    if (configuration == null) {
      return;
    }
    // do not configure source viewer configuration twice
    boolean configured = false;

    // structuredtextviewer only works with
    // structuredtextviewerconfiguration
    if (!(configuration instanceof StructuredTextViewerConfiguration)) {
      ConfigurationAndTarget cat = createSourceViewerConfiguration();
      fViewerConfigurationTargetId = cat.getTargetId();
      configuration = cat.getConfiguration();
      setSourceViewerConfiguration(configuration);
      configured = true;
    } else {
      ConfigurationAndTarget cat = createSourceViewerConfiguration();
      StructuredTextViewerConfiguration newViewerConfiguration = cat.getConfiguration();
      if (!(cat.getTargetId().equals(fViewerConfigurationTargetId))) {
        // d282894 use newViewerConfiguration
        fViewerConfigurationTargetId = cat.getTargetId();
        configuration = newViewerConfiguration;
        setSourceViewerConfiguration(configuration);
        configured = true;
      }
    }

    if (getSourceViewer() != null) {
      // not sure if really need to reconfigure when input changes
      // (maybe only need to reset viewerconfig's document)
      if (!configured) {
        getSourceViewer().configure(configuration);
      }
      IAction openHyperlinkAction = getAction(StructuredTextEditorActionConstants.ACTION_NAME_OPEN_FILE);
      if (openHyperlinkAction instanceof OpenHyperlinkAction) {
        ((OpenHyperlinkAction) openHyperlinkAction).setHyperlinkDetectors(getSourceViewerConfiguration().getHyperlinkDetectors(
            getSourceViewer()));
      }
    }
  }
}
