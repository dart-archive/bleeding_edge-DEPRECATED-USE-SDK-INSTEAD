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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.actions.GenerateActionGroup;
import com.google.dart.tools.ui.actions.RefactorActionGroup_NEW;
import com.google.dart.tools.ui.actions.RefactorActionGroup_OLD;
import com.google.dart.tools.ui.internal.actions.NewSelectionConverter;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.DartStatusConstants;
import com.google.dart.tools.ui.internal.text.comment.CommentFormattingContext;
import com.google.dart.tools.ui.internal.text.dart.DartReconcilingStrategy;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener;
import com.google.dart.tools.ui.internal.text.dart.IDartReconcilingListener_OLD;
import com.google.dart.tools.ui.internal.text.functions.ContentAssistPreference;
import com.google.dart.tools.ui.internal.text.functions.DartHeuristicScanner;
import com.google.dart.tools.ui.internal.text.functions.SmartBackspaceManager;
import com.google.dart.tools.ui.internal.text.functions.Symbols;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension7;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TabsToSpacesConverter;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ResourceAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Dart code editor.
 */
public class CompilationUnitEditor extends DartEditor implements IDartReconcilingListener_OLD {
  public class AdaptedSourceViewer extends DartSourceViewer {

    public AdaptedSourceViewer(Composite parent, IVerticalRuler verticalRuler,
        IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles,
        IPreferenceStore store) {
      super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles, store);
    }

    @Override
    public IFormattingContext createFormattingContext() {
      IFormattingContext context = new CommentFormattingContext();

      Map<String, String> preferences = new HashMap<String, String>(DartCore.getOptions());

      context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, preferences);

      return context;
    }

    @Override
    public void doOperation(int operation) {

      if (getTextWidget() == null) { //|| !isEditorInputModifiable()) {
        return;
      }

      switch (operation) {
        case CONTENTASSIST_PROPOSALS:
          long time = CODE_ASSIST_DEBUG ? System.currentTimeMillis() : 0;
          String msg = fContentAssistant.showPossibleCompletions();
          if (CODE_ASSIST_DEBUG) {
            long delta = System.currentTimeMillis() - time;
            System.err.println("Code Assist (total): " + delta); //$NON-NLS-1$
          }
          setStatusLineErrorMessage(msg);
          return;
        case QUICK_ASSIST:
          /*
           * XXX: We can get rid of this once the SourceViewer has a way to update the status line
           * https://bugs.eclipse.org/bugs/show_bug.cgi?id=133787
           */
          msg = fQuickAssistAssistant.showPossibleQuickAssists();
          setStatusLineErrorMessage(msg);
          return;
        case CUT:
          boolean success = doCut_fix18161();
          if (success) {
            return;
          }
          // use default implementation from ProjectionViewer
          break;
      }

      super.doOperation(operation);
    }

    public IContentAssistant getContentAssistant() {
      return fContentAssistant;
    }

    public CompilationUnitEditor getEditor() {
      return CompilationUnitEditor.this;
    }

    @Override
    public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
      if (PlatformUI.getWorkbench().getHelpSystem().isContextHelpDisplayed()) {
        return false;
      }
      return super.requestWidgetToken(requester);
    }

    @Override
    public boolean requestWidgetToken(IWidgetTokenKeeper requester, int priority) {
      if (PlatformUI.getWorkbench().getHelpSystem().isContextHelpDisplayed()) {
        return false;
      }
      return super.requestWidgetToken(requester, priority);
    }

    @Override
    protected void ensureAnnotationHoverManagerInstalled() {
      super.ensureAnnotationHoverManagerInstalled();
      // Hack to force ANCHOR_TOP instead of default ANCHOR_RIGHT.
      // https://code.google.com/p/dart/issues/detail?id=17109
      try {
        AbstractInformationControlManager manager = ReflectionUtils.getFieldObject(
            this,
            "fVerticalRulerHoveringController");
        manager.setAnchor(AbstractInformationControlManager.ANCHOR_TOP);
      } catch (Throwable e) {
      }
    }

    @Override
    protected int getEmptySelectionChangedEventDelay() {
      return 10; // reduced from 500 to speed up mark occurrences
    }

    /**
     * This method fixes https://code.google.com/p/dart/issues/detail?id=18161
     * <p>
     * We need to copy implementation form {@link ProjectionViewer} because of
     * <p>
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=75222
     */
    private boolean doCut_fix18161() {
      ITextSelection selection = (ITextSelection) getSelection();
      if (selection.getLength() == 0) {
        copyMarkedRegion(true);
        return true;
      } else {
        StyledText textWidget = getTextWidget();
        try {
          ReflectionUtils.invokeMethod(
              this,
              "copyToClipboard(org.eclipse.jface.text.ITextSelection,boolean,org.eclipse.swt.custom.StyledText)",
              selection,
              true,
              textWidget);
          Point range = textWidget.getSelectionRange();
          fireSelectionChanged(range.x, range.y);
          return true;
        } catch (Throwable e) {
          return false;
        }
      }
    }
  }

  interface ITextConverter {
    void customizeDocumentCommand(IDocument document, DocumentCommand command);
  }

  private class BracketInserter implements VerifyKeyListener, ILinkedModeListener {

    private boolean fCloseBrackets = true;
    private boolean fCloseStrings = true;
    private boolean fCloseAngularBrackets = true;
    private final String CATEGORY = toString();
    private final IPositionUpdater fUpdater = new ExclusivePositionUpdater(CATEGORY);
    private final Stack<BracketLevel> fBracketLevelStack = new Stack<BracketLevel>();

    @Override
    public void left(LinkedModeModel environment, int flags) {

      final BracketLevel level = fBracketLevelStack.pop();

      if (flags != ILinkedModeListener.EXTERNAL_MODIFICATION) {
        return;
      }

      // remove brackets
      final ISourceViewer sourceViewer = getSourceViewer();
      final IDocument document = sourceViewer.getDocument();
      if (document instanceof IDocumentExtension) {
        IDocumentExtension extension = (IDocumentExtension) document;
        extension.registerPostNotificationReplace(null, new IDocumentExtension.IReplace() {

          @Override
          public void perform(IDocument d, IDocumentListener owner) {
            if ((level.fFirstPosition.isDeleted || level.fFirstPosition.length == 0)
                && !level.fSecondPosition.isDeleted
                && level.fSecondPosition.offset == level.fFirstPosition.offset) {
              try {
                document.replace(level.fSecondPosition.offset, level.fSecondPosition.length, ""); //$NON-NLS-1$
              } catch (BadLocationException e) {
                DartToolsPlugin.log(e);
              }
            }

            if (fBracketLevelStack.size() == 0) {
              document.removePositionUpdater(fUpdater);
              try {
                document.removePositionCategory(CATEGORY);
              } catch (BadPositionCategoryException e) {
                DartToolsPlugin.log(e);
              }
            }
          }
        });
      }

    }

    @Override
    public void resume(LinkedModeModel environment, int flags) {
    }

    public void setCloseAngularBracketsEnabled(boolean enabled) {
      fCloseAngularBrackets = enabled;
    }

    public void setCloseBracketsEnabled(boolean enabled) {
      fCloseBrackets = enabled;
    }

    public void setCloseStringsEnabled(boolean enabled) {
      fCloseStrings = enabled;
    }

    @Override
    public void suspend(LinkedModeModel environment) {
    }

    @Override
    public void verifyKey(VerifyEvent event) {
      if (!isEditable()) {
        return;
      }

      // early pruning to slow down normal typing as little as possible
      if (!event.doit || getInsertMode() != SMART_INSERT || isBlockSelectionModeEnabled()
          && isMultilineSelection()) {
        return;
      }
//      boolean checkWrappingThenReturn = false;
      switch (event.character) {
        case '(':
        case '<':
        case '[':
        case '\'':
        case '\"':
          break;
//        case '{':
//          checkWrappingThenReturn = true;
//          break;
        default:
          return;
      }

      final ISourceViewer sourceViewer = getSourceViewer();
      IDocument document = sourceViewer.getDocument();

      final Point selection = sourceViewer.getSelectedRange();
      final int offset = selection.x;
      final int length = selection.y;
//      if (length > 0) {
//        IRewriteTarget target = ((ITextViewerExtension) sourceViewer).getRewriteTarget();
//        if (couldWrapWithGroup(event.character, document, offset, length, target)) {
//          event.doit = false;
//          return;
//        }
//        if (checkWrappingThenReturn) {
//          return;
//        }
//      }
      LinkedModeModel existingModel = LinkedModeModel.getModel(document, offset);
      if (existingModel != null && existingModel.anyPositionContains(offset)) {
        if (LinkedModeModel.class.isAssignableFrom(existingModel.getClass().getSuperclass())) {
          // Adding a bracket matcher while completion proposal editing is active causes problems
          return;
        }
      }
      try {
        IRegion startLine = document.getLineInformationOfOffset(offset);
        IRegion endLine = document.getLineInformationOfOffset(offset + length);

        DartHeuristicScanner scanner = new DartHeuristicScanner(document);
        int nextToken = scanner.nextToken(
            offset + length,
            endLine.getOffset() + endLine.getLength());
        String next = nextToken == Symbols.TokenEOF ? null : document.get(
            offset,
            scanner.getPosition() - offset).trim();
        int prevToken = scanner.previousToken(offset - 1, startLine.getOffset() - 1);
        int prevTokenOffset = scanner.getPosition() + 1;
        String previous = prevToken == Symbols.TokenEOF ? null : document.get(
            prevTokenOffset,
            offset - prevTokenOffset).trim();

        switch (event.character) {
          case '(':
            if (!fCloseBrackets || nextToken == Symbols.TokenLPAREN
                || nextToken == Symbols.TokenIDENT || next != null && next.length() > 1) {
              return;
            }
            break;

          case '<':
            if (!(fCloseAngularBrackets && fCloseBrackets) || nextToken == Symbols.TokenLESSTHAN
                || nextToken == Symbols.TokenQUESTIONMARK || nextToken == Symbols.TokenIDENT
                && isTypeArgumentStart(next) || prevToken != Symbols.TokenLBRACE
                && prevToken != Symbols.TokenRBRACE && prevToken != Symbols.TokenSEMICOLON
                && prevToken != Symbols.TokenSTATIC
                && (prevToken != Symbols.TokenIDENT || !isAngularIntroducer(previous))
                && prevToken != Symbols.TokenEOF) {
              return;
            }
            break;

          case '[':
            if (!fCloseBrackets || nextToken == Symbols.TokenIDENT || next != null
                && next.length() > 1) {
              return;
            }
            break;

          case '\'':
          case '"':
            if (!fCloseStrings) {
              return;
            }
            if (prevToken == Symbols.TokenIDENT && "r".equals(previous)) {
              break; // handle raw strings
            }
            if (prevToken == Symbols.TokenRBRACE) {
              return; // "${expression}^
            }
            if (nextToken == Symbols.TokenIDENT || prevToken == Symbols.TokenIDENT || next != null
                && next.length() > 1
                || (previous != null && previous.length() > 1 && !previous.equals("import"))) {
              return;
            }
            break;

          case '{':
            if (prevToken == Symbols.TokenIDENT && previous.equals("$")) {
              // does not support defining functions within interpolation
              break;
            } else {
              return;
            }
          default:
            return;
        }

        ITypedRegion partition = TextUtilities.getPartition(
            document,
            DartPartitions.DART_PARTITIONING,
            offset,
            true);
        if (!IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())) {
          return;
        }

        if (!validateEditorInputState()) {
          return;
        }

        final char character = event.character;
        final char closingCharacter = getPeerCharacter(character);
        final StringBuffer buffer = new StringBuffer();
        buffer.append(character);
        buffer.append(closingCharacter);

        document.replace(offset, length, buffer.toString());

        BracketLevel level = new BracketLevel();
        fBracketLevelStack.push(level);

        LinkedPositionGroup group = new LinkedPositionGroup();
        group.addPosition(new LinkedPosition(document, offset + 1, 0, LinkedPositionGroup.NO_STOP));

        LinkedModeModel model = new LinkedModeModel();
        model.addLinkingListener(this);
        model.addGroup(group);
        model.forceInstall();

        // set up position tracking for our magic peers
        if (fBracketLevelStack.size() == 1) {
          document.addPositionCategory(CATEGORY);
          document.addPositionUpdater(fUpdater);
        }
        level.fFirstPosition = new Position(offset, 1);
        level.fSecondPosition = new Position(offset + 1, 1);
        document.addPosition(CATEGORY, level.fFirstPosition);
        document.addPosition(CATEGORY, level.fSecondPosition);

        level.fUI = new EditorLinkedModeUI(model, sourceViewer);
        level.fUI.setSimpleMode(true);
        level.fUI.setExitPolicy(new ExitPolicy(
            closingCharacter,
            getEscapeCharacter(closingCharacter),
            fBracketLevelStack));
        level.fUI.setExitPosition(sourceViewer, offset + 2, 0, Integer.MAX_VALUE);
        level.fUI.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
        level.fUI.enter();

        IRegion newSelection = level.fUI.getSelectedRegion();
        sourceViewer.setSelectedRange(newSelection.getOffset(), newSelection.getLength());

        event.doit = false;

      } catch (BadLocationException e) {
        DartToolsPlugin.log(e);
      } catch (BadPositionCategoryException e) {
        DartToolsPlugin.log(e);
      }
    }

//    private boolean couldWrapWithGroup(char startCh, IDocument document, int offset, int length,
//        IRewriteTarget target) {
//      int end = offset + length;
//      if (offset < 0 || (document.getLength() <= end)) {
//        return false;
//      }
//      char endCh;
//      switch (startCh) {
//        case '(':
//          endCh = ')';
//          break;
//        case '<':
//          endCh = '>';
//          break;
//        case '[':
//          endCh = ']';
//          break;
//        case '{':
//          endCh = '}';
//          break;
////        case '\'':
////        case '\"':
////          endCh = startCh;
////          break;
//        default:
//          return false;
//      }
//      MultiTextEdit textEdit = new MultiTextEdit();
//      textEdit.addChild(new InsertEdit(offset, String.valueOf(startCh)));
//      textEdit.addChild(new InsertEdit(offset + length, String.valueOf(endCh)));
//      try {
//        target.beginCompoundChange();
//        textEdit.apply(document);
//      } catch (BadLocationException ex) {
//        return false;
//      } finally {
//        target.endCompoundChange();
//      }
//      return true;
//    }

    private boolean isAngularIntroducer(String identifier) {
      return identifier.length() > 0
          && (Character.isUpperCase(identifier.charAt(0)) || identifier.startsWith("final")); //$NON-NLS-1$
    }

    private boolean isMultilineSelection() {
      ISelection selection = getSelectionProvider().getSelection();
      if (selection instanceof ITextSelection) {
        ITextSelection ts = (ITextSelection) selection;
        return ts.getStartLine() != ts.getEndLine();
      }
      return false;
    }

    private boolean isTypeArgumentStart(String identifier) {
      return identifier.length() > 0 && Character.isUpperCase(identifier.charAt(0));
    }
  }

  private static class BracketLevel {
    @SuppressWarnings("unused")
    int fOffset;
    @SuppressWarnings("unused")
    int fLength;
    LinkedModeUI fUI;
    Position fFirstPosition;
    Position fSecondPosition;
  }

  /**
   * Position updater that takes any changes at the borders of a position to not belong to the
   * position.
   */
  private static class ExclusivePositionUpdater implements IPositionUpdater {

    /** The position category. */
    private final String fCategory;

    /**
     * Creates a new updater for the given <code>category</code>.
     * 
     * @param category the new category.
     */
    public ExclusivePositionUpdater(String category) {
      fCategory = category;
    }

    @Override
    public void update(DocumentEvent event) {

      int eventOffset = event.getOffset();
      int eventOldLength = event.getLength();
      int eventNewLength = event.getText() == null ? 0 : event.getText().length();
      int deltaLength = eventNewLength - eventOldLength;

      try {
        Position[] positions = event.getDocument().getPositions(fCategory);

        for (int i = 0; i != positions.length; i++) {

          Position position = positions[i];

          if (position.isDeleted()) {
            continue;
          }

          int offset = position.getOffset();
          int length = position.getLength();
          int end = offset + length;

          if (offset >= eventOffset + eventOldLength) {
            // position comes after change - shift
            position.setOffset(offset + deltaLength);
          } else if (end <= eventOffset) {
            // position comes way before change - leave alone
          } else if (offset <= eventOffset && end >= eventOffset + eventOldLength) {
            // event completely internal to the position - adjust length
            position.setLength(length + deltaLength);
          } else if (offset < eventOffset) {
            // event extends over end of position - adjust length
            int newEnd = eventOffset;
            position.setLength(newEnd - offset);
          } else if (end > eventOffset + eventOldLength) {
            // event extends from before position into it - adjust offset and length
            // offset becomes end of event, length adjusted accordingly
            int newOffset = eventOffset + eventNewLength;
            position.setOffset(newOffset);
            position.setLength(end - newOffset);
          } else {
            // event consumes the position - delete it
            position.delete();
          }
        }
      } catch (BadPositionCategoryException e) {
        // ignore and return
      }
    }

  }

  private class ExitPolicy implements IExitPolicy {

    final char fExitCharacter;
    final char fEscapeCharacter;
    final Stack<BracketLevel> fStack;
    final int fSize;

    public ExitPolicy(char exitCharacter, char escapeCharacter, Stack<BracketLevel> stack) {
      fExitCharacter = exitCharacter;
      fEscapeCharacter = escapeCharacter;
      fStack = stack;
      fSize = fStack.size();
    }

    @Override
    public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {

      if (fSize == fStack.size() && !isMasked(offset)) {
        if (event.character == fExitCharacter) {
          BracketLevel level = fStack.peek();
          if (level.fFirstPosition.offset > offset || level.fSecondPosition.offset < offset) {
            return null;
          }
          if (level.fSecondPosition.offset == offset && length == 0) {
            // don't enter the character if if its the closing peer
            return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
          }
        }
        // when entering an anonymous class between the parenthesis', we don't want
        // to jump after the closing parenthesis when return is pressed
        if (event.character == SWT.CR && offset > 0) {
          IDocument document = getSourceViewer().getDocument();
          try {
            if (document.getChar(offset - 1) == '{') {
              return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
            }
          } catch (BadLocationException e) {
          }
        }
      }
      return null;
    }

    private boolean isMasked(int offset) {
      IDocument document = getSourceViewer().getDocument();
      try {
        return fEscapeCharacter == document.getChar(offset - 1);
      } catch (BadLocationException e) {
      }
      return false;
    }
  }

  /**
   * Remembers additional data for a given offset to be able restore it later.
   */
  private class RememberedOffset {
    /**
     * Store visual properties of the given offset.
     * 
     * @param offset Offset in the document
     */
    public void setOffset(int offset) {
    }

  }

  /**
   * Remembers data related to the current selection to be able to restore it later.
   */
  private class RememberedSelection {
    /** The remembered selection start. */
    private RememberedOffset fStartOffset = new RememberedOffset();
    /** The remembered selection end. */
    private RememberedOffset fEndOffset = new RememberedOffset();

    /**
     * Remember current selection.
     */
    public void remember() {
      /*
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=52257 This method may be called inside an
       * asynchronous call posted to the UI thread, so protect against intermediate disposal of the
       * editor.
       */
      ISourceViewer viewer = getSourceViewer();
      if (viewer != null) {
        Point selection = viewer.getSelectedRange();
        int startOffset = selection.x;
        int endOffset = startOffset + selection.y;

        fStartOffset.setOffset(startOffset);
        fEndOffset.setOffset(endOffset);
      }
    }

    /**
     * Restore remembered selection.
     */
    public void restore() {
      /*
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=52257 This method may be called inside an
       * asynchronous call posted to the UI thread, so protect against intermediate disposal of the
       * editor.
       */
      if (getSourceViewer() == null) {
        return;
      }

      //TODO (pquitslund): implement restore for new elements
      return;

//      try {
//
//        int startOffset, endOffset;
//        int revealStartOffset, revealEndOffset;
//        if (showsHighlightRangeOnly()) {
//          DartElement newStartElement = (DartElement) fStartOffset.getElement();
//          startOffset = fStartOffset.getRememberedOffset(newStartElement);
//          revealStartOffset = fStartOffset.getRevealOffset(newStartElement, startOffset);
//          if (revealStartOffset == -1) {
//            startOffset = -1;
//          }
//
//          DartElement newEndElement = (DartElement) fEndOffset.getElement();
//          endOffset = fEndOffset.getRememberedOffset(newEndElement);
//          revealEndOffset = fEndOffset.getRevealOffset(newEndElement, endOffset);
//          if (revealEndOffset == -1) {
//            endOffset = -1;
//          }
//        } else {
//          startOffset = fStartOffset.getOffset();
//          revealStartOffset = startOffset;
//          endOffset = fEndOffset.getOffset();
//          revealEndOffset = endOffset;
//        }
//
//        if (startOffset == -1) {
//          startOffset = endOffset; // fallback to caret offset
//          revealStartOffset = revealEndOffset;
//        }
//
//        if (endOffset == -1) {
//          endOffset = startOffset; // fallback to other offset
//          revealEndOffset = revealStartOffset;
//        }
//
//        DartElement element;
//        if (endOffset == -1) {
//          // fallback to element selection
//          element = (DartElement) fEndOffset.getElement();
//          if (element == null) {
//            element = (DartElement) fStartOffset.getElement();
//          }
//          if (element != null) {
//            setSelection(element);
//          }
//          return;
//        }
//
//        if (isValidSelection(revealStartOffset, revealEndOffset - revealStartOffset)
//            && isValidSelection(startOffset, endOffset - startOffset)) {
//          selectAndReveal(startOffset, endOffset - startOffset, revealStartOffset, revealEndOffset
//              - revealStartOffset);
//        }
//      } finally {
//        fStartOffset.clear();
//        fEndOffset.clear();
//      }
    }

//    private boolean isValidSelection(int offset, int length) {
//      IDocumentProvider provider = getDocumentProvider();
//      if (provider != null) {
//        IDocument document = provider.getDocument(getEditorInput());
//        if (document != null) {
//          int end = offset + length;
//          int documentLength = document.getLength();
//          return 0 <= offset && offset <= documentLength && 0 <= end && end <= documentLength
//              && length >= 0;
//        }
//      }
//      return false;
//    }

  }

  private static final boolean CODE_ASSIST_DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("com.google.dart.tools.ui/debug/ResultCollector")); //$NON-NLS-1$//$NON-NLS-2$

  /**
   * Text operation code for requesting common prefix completion.
   */
  public static final int CONTENTASSIST_COMPLETE_PREFIX = 60;

  /** Preference key for code formatter tab size */
  private final static String CODE_FORMATTER_TAB_SIZE = DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
  /** Preference key for inserting spaces rather than tabs */
  private final static String SPACES_FOR_TABS = DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR;
  /** Preference key for automatically closing strings */
  private final static String CLOSE_STRINGS = PreferenceConstants.EDITOR_CLOSE_STRINGS;
  /** Preference key for automatically closing brackets and parenthesis */
  private final static String CLOSE_BRACKETS = PreferenceConstants.EDITOR_CLOSE_BRACKETS;

  private static char getEscapeCharacter(char character) {
    switch (character) {
      case '"':
      case '\'':
        return '\\';
      default:
        return 0;
    }
  }

  private static char getPeerCharacter(char character) {
    switch (character) {
      case '(':
        return ')';

      case ')':
        return '(';

      case '<':
        return '>';

      case '>':
        return '<';

      case '[':
        return ']';

      case ']':
        return '[';

      case '{':
        return '}';

      case '}':
        return '{';

      case '"':
        return character;

      case '\'':
        return character;

      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Listener to annotation model changes that updates the error tick in the tab image
   */
  private DartEditorErrorTickUpdater fJavaEditorErrorTickUpdater;

  /**
   * The remembered selection.
   */
  private RememberedSelection fRememberedSelection = new RememberedSelection();
  /** The bracket inserter. */
  private BracketInserter fBracketInserter = new BracketInserter();

  /** The standard action groups added to the menu */
  private GenerateActionGroup fGenerateActionGroup;
  private ActionGroup fRefactorActionGroup;
  //
  private CompositeActionGroup fContextMenuGroup;
  //
  // private CorrectionCommandInstaller fCorrectionCommands;

  /**
   * Reconciling listeners.
   */
  private ListenerList fReconcilingListeners_OLD = new ListenerList(ListenerList.IDENTITY);
  private ListenerList fReconcilingListeners = new ListenerList(ListenerList.IDENTITY);

  /**
   * Mutex for the reconciler. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=63898 for a
   * description of the problem.
   * <p>
   * XXX remove once the underlying problem (https://bugs.eclipse.org/bugs/show_bug.cgi?id=66176) is
   * solved.
   * </p>
   */
  private final Object fReconcilerLock = new Object();

  /** The reconciling strategy */
  private DartReconcilingStrategy dartReconcilingStrategy;

  /**
   * Creates a new compilation unit editor.
   */
  public CompilationUnitEditor() {
    super();
    setDocumentProvider(DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider());
    setEditorContextMenuId("#DartEditorContext"); //$NON-NLS-1$
    setRulerContextMenuId("#DartRulerContext"); //$NON-NLS-1$
    setOutlinerContextMenuId("#JavaScriptOutlinerContext"); //$NON-NLS-1$
    scheduleReconcileAfterBuild();

    fJavaEditorErrorTickUpdater = new DartEditorErrorTickUpdater(this);
    DartX.todo("actions");
    // fCorrectionCommands = null;
  }

  /*
   * @see com.google.dart.tools.ui.functions.java.IJavaReconcilingListener# aboutToBeReconciled()
   */
  @Override
  public void aboutToBeReconciled() {
    // Notify listeners
    Object[] listeners = fReconcilingListeners_OLD.getListeners();
    for (int i = 0, length = listeners.length; i < length; ++i) {
      ((IDartReconcilingListener_OLD) listeners[i]).aboutToBeReconciled();
    }
  }

  /**
   * Adds the given listener. Has no effect if an identical listener was not already registered.
   * 
   * @param listener The reconcile listener to be added
   */
  public void addReconcileListener(IDartReconcilingListener listener) {
    synchronized (fReconcilingListeners) {
      fReconcilingListeners.add(listener);
    }
  }

  @Override
  public void addViewerDisposeListener(DisposeListener listener) {
    getViewer().getTextWidget().addDisposeListener(listener);
  }

  @Override
  public void applyResolvedUnit(com.google.dart.engine.ast.CompilationUnit unit) {
    super.applyResolvedUnit(unit);
    if (unit != null) {
      // notify listeners
      {
        Object[] listeners = fReconcilingListeners.getListeners();
        for (int i = 0, length = listeners.length; i < length; ++i) {
          ((IDartReconcilingListener) listeners[i]).reconciled(unit);
        }
      }
    }
  }

  /*
   * @see AbstractTextEditor#createPartControl(Composite)
   */
  @Override
  public void createPartControl(Composite parent) {

    super.createPartControl(parent);

    IPreferenceStore preferenceStore = getPreferenceStore();
    boolean closeBrackets = preferenceStore.getBoolean(CLOSE_BRACKETS);
    boolean closeStrings = preferenceStore.getBoolean(CLOSE_STRINGS);
    boolean closeAngularBrackets = closeBrackets;

    fBracketInserter.setCloseBracketsEnabled(closeBrackets);
    fBracketInserter.setCloseStringsEnabled(closeStrings);
    fBracketInserter.setCloseAngularBracketsEnabled(closeAngularBrackets);

    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer instanceof ITextViewerExtension) {
      ((ITextViewerExtension) sourceViewer).prependVerifyKeyListener(fBracketInserter);
    }

    if (isMarkingOccurrences()) {
      installOccurrencesFinder(false);
    }
  }

  @Override
  public void dispose() {

    ISourceViewer sourceViewer = getSourceViewer();
    if (sourceViewer instanceof ITextViewerExtension) {
      ((ITextViewerExtension) sourceViewer).removeVerifyKeyListener(fBracketInserter);
    }

    if (fJavaEditorErrorTickUpdater != null) {
      fJavaEditorErrorTickUpdater.dispose();
      fJavaEditorErrorTickUpdater = null;
    }
    DartX.todo("actions");
    // if (fCorrectionCommands != null) {
    // fCorrectionCommands.deregisterCommands();
    // fCorrectionCommands = null;
    // }
    fReconcilingListeners = new ListenerList(ListenerList.IDENTITY);
    super.dispose();
  }

  /*
   * @see AbstractTextEditor#doSave(IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor progressMonitor) {

    IDocumentProvider p = getDocumentProvider();
    if (p == null) {
      // editor has been closed
      return;
    }

    if (p.isDeleted(getEditorInput())) {

      if (isSaveAsAllowed()) {

        /*
         * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors. Changed
         * Behavior to make sure that if called inside a regular save (because of deletion of input
         * element) there is a way to report back to the caller.
         */
        performSaveAs(progressMonitor);

      } else {

        /*
         * 1GF5YOX: ITPJUI:ALL - Save of delete file claims it's still there Missing resources.
         */
        Shell shell = getSite().getShell();
        MessageDialog.openError(
            shell,
            DartEditorMessages.CompilationUnitEditor_error_saving_title1,
            DartEditorMessages.CompilationUnitEditor_error_saving_message1);
      }

    } else {
      setStatusLineErrorMessage(null);

      updateState(getEditorInput());
      validateState(getEditorInput());

      performSave(false, progressMonitor);
    }
  }

  /*
   * @see AbstractTextEditor#editorContextMenuAboutToShow(IMenuManager)
   */
  @Override
  public void editorContextMenuAboutToShow(IMenuManager menu) {
    super.editorContextMenuAboutToShow(menu);
//    addAction(menu, "ToggleComment");
    // add Organize Imports action to menu
//    menu.add(new Separator());
//    addAction(menu, "OrganizeImports");
  }

  /*
   * @see com.google.dart.tools.ui.editor.JavaEditor#getAdapter(java.lang.Class)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object getAdapter(Class required) {
    if (SmartBackspaceManager.class.equals(required)) {
      if (getSourceViewer() instanceof DartSourceViewer) {
        return ((DartSourceViewer) getSourceViewer()).getBackspaceManager();
      }
    }

    return super.getAdapter(required);
  }

  @Override
  public DartReconcilingStrategy getDartReconcilingStrategy() {
    return dartReconcilingStrategy;
  }

  /**
   * Returns the mutex for the reconciler. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=63898
   * for a description of the problem.
   * <p>
   * XXX remove once the underlying problem (https://bugs.eclipse.org/bugs/show_bug.cgi?id=66176) is
   * solved.
   * </p>
   * 
   * @return the lock reconcilers may use to synchronize on
   */
  public Object getReconcilerLock() {
    return fReconcilerLock;
  }

  public ActionGroup getRefactorActionGroup() {
    return fRefactorActionGroup;
  }

  @Override
  public Object getViewPartInput() {
    return null;
  }

  @Override
  public boolean isSaveAsAllowed() {
    return true;
  }

  @Override
  public void reconciled(boolean forced, IProgressMonitor progressMonitor) {
    AutoSaveHelper.reconciled(getEditorInput(), getSourceViewer(), getTextSelectionRange());

    // see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=58245
    DartToolsPlugin dartPlugin = DartToolsPlugin.getDefault();
    if (dartPlugin == null) {
      return;
    }

    // Always notify AST provider
    dartPlugin.getASTProvider().reconciled(progressMonitor);

    // Notify listeners
    Object[] listeners = fReconcilingListeners_OLD.getListeners();
    for (int i = 0, length = listeners.length; i < length; ++i) {
      ((IDartReconcilingListener_OLD) listeners[i]).reconciled(forced, progressMonitor);
    }

    // Update Outline page selection
    if (!forced && !progressMonitor.isCanceled()) {
      Shell shell = getSite().getShell();
      if (shell != null && !shell.isDisposed()) {
        ExecutionUtils.runLogAsync(new RunnableEx() {
          @Override
          public void run() {
            selectionChanged();
          }
        });
      }
    }
  }

  /**
   * Removes the given listener. Has no effect if an identical listener was not already registered.
   * 
   * @param listener the reconcile listener to be removed
   */
  public void removeReconcileListener(IDartReconcilingListener listener) {
    synchronized (fReconcilingListeners) {
      fReconcilingListeners.remove(listener);
    }
  }

  @Override
  public void setDartReconcilingStrategy(DartReconcilingStrategy dartReconcilingStrategy) {
    this.dartReconcilingStrategy = dartReconcilingStrategy;
  }

  /*
   * @see AbstractTextEditor#canHandleMove(IEditorInput, IEditorInput)
   */
  @Override
  protected boolean canHandleMove(IEditorInput originalElement, IEditorInput movedElement) {

    String oldExtension = ""; //$NON-NLS-1$
    if (originalElement instanceof IFileEditorInput) {
      IFile file = ((IFileEditorInput) originalElement).getFile();
      if (file != null) {
        String ext = file.getFileExtension();
        if (ext != null) {
          oldExtension = ext;
        }
      }
    }

    String newExtension = ""; //$NON-NLS-1$
    if (movedElement instanceof IFileEditorInput) {
      IFile file = ((IFileEditorInput) movedElement).getFile();
      if (file != null) {
        newExtension = file.getFileExtension();
      }
    }

    return oldExtension.equals(newExtension);
  }

  /*
   * @see AbstractTextEditor#createActions()
   */
  @Override
  protected void createActions() {

    super.createActions();
    DartX.todo("actions");

    IAction action = new ContentAssistAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "ContentAssistProposal.", this); //$NON-NLS-1$
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    setAction("ContentAssistProposal", action); //$NON-NLS-1$
    markAsStateDependentAction("ContentAssistProposal", true); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        action,
        DartHelpContextIds.CONTENT_ASSIST_ACTION);

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "ContentAssistContextInformation.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION); //$NON-NLS-1$
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
    setAction("ContentAssistContextInformation", action); //$NON-NLS-1$
    markAsStateDependentAction("ContentAssistContextInformation", true); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        action,
        DartHelpContextIds.PARAMETER_HINTS_ACTION);

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Comment.", this, ITextOperationTarget.PREFIX); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.COMMENT);
    setAction("Comment", action); //$NON-NLS-1$
    markAsStateDependentAction("Comment", true); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(action, DartHelpContextIds.COMMENT_ACTION);

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Uncomment.", this, ITextOperationTarget.STRIP_PREFIX); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.UNCOMMENT);
    setAction("Uncomment", action); //$NON-NLS-1$
    markAsStateDependentAction("Uncomment", true); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(action, DartHelpContextIds.UNCOMMENT_ACTION);

    action = new ToggleCommentAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "ToggleComment.", this); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.TOGGLE_COMMENT);
    setAction("ToggleComment", action); //$NON-NLS-1$
    markAsStateDependentAction("ToggleComment", true); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        action,
        DartHelpContextIds.TOGGLE_COMMENT_ACTION);
    configureToggleCommentAction();

    action = new TextOperationAction(
        DartEditorMessages.getBundleForConstructedKeys(),
        "Format.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
    action.setActionDefinitionId(DartEditorActionDefinitionIds.FORMAT);
    setAction("Format", action); //$NON-NLS-1$
    markAsStateDependentAction("Format", true); //$NON-NLS-1$
    markAsSelectionDependentAction("Format", true); //$NON-NLS-1$
    PlatformUI.getWorkbench().getHelpSystem().setHelp(action, DartHelpContextIds.FORMAT_ACTION);

//    action = new OrganizeImportsAction(this);
//    action.setActionDefinitionId(DartEditorActionDefinitionIds.ORGANIZE_IMPORTS);
//    setAction("OrganizeImports", action);

    //     action = new AddBlockCommentAction(
    //     DartEditorMessages.getBundleForConstructedKeys(),
    //            "AddBlockComment.", this); //$NON-NLS-1$
    //     action.setActionDefinitionId(IJavaEditorActionDefinitionIds.ADD_BLOCK_COMMENT);
    //        setAction("AddBlockComment", action); //$NON-NLS-1$
    //        markAsStateDependentAction("AddBlockComment", true); //$NON-NLS-1$
    //        markAsSelectionDependentAction("AddBlockComment", true); //$NON-NLS-1$
    //     PlatformUI.getWorkbench().getHelpSystem().setHelp(action,
    //     DartHelpContextIds.ADD_BLOCK_COMMENT_ACTION);

    //     action = new RemoveBlockCommentAction(
    //     DartEditorMessages.getBundleForConstructedKeys(),
    //            "RemoveBlockComment.", this); //$NON-NLS-1$
    //     action.setActionDefinitionId(IJavaEditorActionDefinitionIds.REMOVE_BLOCK_COMMENT);
    //        setAction("RemoveBlockComment", action); //$NON-NLS-1$
    //        markAsStateDependentAction("RemoveBlockComment", true); //$NON-NLS-1$
    //        markAsSelectionDependentAction("RemoveBlockComment", true); //$NON-NLS-1$
    //     PlatformUI.getWorkbench().getHelpSystem().setHelp(action,
    //     DartHelpContextIds.REMOVE_BLOCK_COMMENT_ACTION);

    // override the text editor actions with indenting move line actions
    DartMoveLinesAction[] moveLinesActions = DartMoveLinesAction.createMoveCopyActionSet(
        DartEditorMessages.getBundleForConstructedKeys(),
        this);
    ResourceAction rAction = moveLinesActions[0];
    rAction.setHelpContextId(IAbstractTextEditorHelpContextIds.MOVE_LINES_ACTION);
    rAction.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
    setAction(ITextEditorActionConstants.MOVE_LINE_UP, rAction);

    rAction = moveLinesActions[1];
    rAction.setHelpContextId(IAbstractTextEditorHelpContextIds.MOVE_LINES_ACTION);
    rAction.setActionDefinitionId(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
    setAction(ITextEditorActionConstants.MOVE_LINE_DOWN, rAction);

    rAction = moveLinesActions[2];
    rAction.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_LINES_ACTION);
    rAction.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY_LINES_UP);
    setAction(ITextEditorActionConstants.COPY_LINE_UP, rAction);

    rAction = moveLinesActions[3];
    rAction.setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_LINES_ACTION);
    rAction.setActionDefinitionId(ITextEditorActionDefinitionIds.COPY_LINES_DOWN);
    setAction(ITextEditorActionConstants.COPY_LINE_DOWN, rAction);

    if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TAB)) {
      // don't replace Shift Right - have to make sure their enablement is
      // mutually exclusive
      // removeActionActivationCode(ITextEditorActionConstants.SHIFT_RIGHT);
      setActionActivationCode("IndentOnTab", '\t', -1, SWT.NONE); //$NON-NLS-1$
    }

    fGenerateActionGroup = new GenerateActionGroup(this, ITextEditorActionConstants.GROUP_EDIT);
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      fRefactorActionGroup = new RefactorActionGroup_NEW(this);
    } else {
      fRefactorActionGroup = new RefactorActionGroup_OLD(this);
      //    ActionGroup surroundWith = new SurroundWithActionGroup(this,
      //        ITextEditorActionConstants.GROUP_EDIT);
    }

    //    fActionGroups.addGroup(surroundWith);
    fActionGroups.addGroup(fRefactorActionGroup);
    fActionGroups.addGroup(fGenerateActionGroup);

    // We have to keep the context menu group separate to have better control
    // over positioning
    fContextMenuGroup = new CompositeActionGroup(new ActionGroup[] {
//        fGenerateActionGroup,
    fRefactorActionGroup,
//             surroundWith,
//             new LocalHistoryActionGroup(this,
//             ITextEditorActionConstants.GROUP_EDIT)
        });

    // allow shortcuts for quick fix/assist
    //     fCorrectionCommands = new CorrectionCommandInstaller();
    //     fCorrectionCommands.registerCommands(this);
  }

  /*
   * @see com.google.dart.tools.ui.editor.DartEditor#createDartSourceViewer(org.eclipse
   * .swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler,
   * org.eclipse.jface.text.source.IOverviewRuler, boolean, int)
   */
  @Override
  protected ISourceViewer createDartSourceViewer(Composite parent, IVerticalRuler verticalRuler,
      IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles,
      IPreferenceStore store) {
    return new AdaptedSourceViewer(
        parent,
        verticalRuler,
        overviewRuler,
        isOverviewRulerVisible,
        styles,
        store);
  }

  /*
   * @see com.google.dart.tools.ui.editor.JavaEditor#createNavigationActions()
   */
  @Override
  protected void createNavigationActions() {
    super.createNavigationActions();

    final StyledText textWidget = getSourceViewer().getTextWidget();

    IAction action = new DeletePreviousSubWordAction();
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD);
    setAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, action);
    textWidget.setKeyBinding(SWT.CTRL | SWT.BS, SWT.NULL);
    markAsStateDependentAction(ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD, true);

    action = new DeleteNextSubWordAction();
    action.setActionDefinitionId(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD);
    setAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, action);
    textWidget.setKeyBinding(SWT.CTRL | SWT.DEL, SWT.NULL);
    markAsStateDependentAction(ITextEditorActionDefinitionIds.DELETE_NEXT_WORD, true);
  }

  /*
   * @see AbstractTextEditor#doSetInput(IEditorInput)
   */
  @Override
  protected void doSetInput(IEditorInput input) throws CoreException {
    super.doSetInput(input);
    configureToggleCommentAction();
    if (fJavaEditorErrorTickUpdater != null) {
      checkEditableState();
      if (input instanceof FileStoreEditorInput) {
        fJavaEditorErrorTickUpdater.updateEditorImage(input);
      }
    }
  }

  @Override
  protected Object getElementAt(int offset) {
    return getElementAt(offset, true);
  }

  /**
   * Returns the most narrow element including the given offset. If <code>reconcile</code> is
   * <code>true</code> the editor's input element is reconciled in advance. If it is
   * <code>false</code> this method only returns a result if the editor's input element does not
   * need to be reconciled.
   * 
   * @param offset the offset included by the retrieved element
   * @param reconcile <code>true</code> if working copy should be reconciled
   * @return the most narrow element which includes the given offset
   */
  @Override
  protected Object getElementAt(int offset, boolean reconcile) {
    return NewSelectionConverter.getElementAtOffset(this, offset);
  }

  /*
   * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
   */
  @Override
  protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

    try {

      AdaptedSourceViewer asv = (AdaptedSourceViewer) getSourceViewer();
      if (asv != null) {

        String p = event.getProperty();

        if (CLOSE_BRACKETS.equals(p)) {
          fBracketInserter.setCloseBracketsEnabled(getPreferenceStore().getBoolean(p));
          return;
        }

        if (CLOSE_STRINGS.equals(p)) {
          fBracketInserter.setCloseStringsEnabled(getPreferenceStore().getBoolean(p));
          return;
        }

        if (JavaScriptCore.COMPILER_SOURCE.equals(p)) {
          boolean closeAngularBrackets = JavaScriptCore.VERSION_1_5.compareTo(getPreferenceStore().getString(
              p)) <= 0;
          fBracketInserter.setCloseAngularBracketsEnabled(closeAngularBrackets);
        }

        if (SPACES_FOR_TABS.equals(p)) {
          if (isTabsToSpacesConversionEnabled()) {
            installTabsToSpacesConverter();
          } else {
            uninstallTabsToSpacesConverter();
          }
          return;
        }

        if (PreferenceConstants.EDITOR_SMART_TAB.equals(p)) {
          if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TAB)) {
            setActionActivationCode("IndentOnTab", '\t', -1, SWT.NONE); //$NON-NLS-1$
          } else {
            removeActionActivationCode("IndentOnTab"); //$NON-NLS-1$
          }
        }

        IContentAssistant c = asv.getContentAssistant();
        if (c instanceof ContentAssistant) {
          ContentAssistPreference.changeConfiguration(
              (ContentAssistant) c,
              getPreferenceStore(),
              event);
        }

        if (CODE_FORMATTER_TAB_SIZE.equals(p) && isTabsToSpacesConversionEnabled()) {
          uninstallTabsToSpacesConverter();
          installTabsToSpacesConverter();
        }
      }

    } finally {
      super.handlePreferenceStoreChanged(event);
    }
  }

  /*
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#installTabsToSpacesConverter()
   */
  @Override
  protected void installTabsToSpacesConverter() {
    ISourceViewer sourceViewer = getSourceViewer();
    SourceViewerConfiguration config = getSourceViewerConfiguration();
    if (config != null && sourceViewer instanceof ITextViewerExtension7) {
      int tabWidth = config.getTabWidth(sourceViewer);
      TabsToSpacesConverter tabToSpacesConverter = new TabsToSpacesConverter();
      tabToSpacesConverter.setNumberOfSpacesPerTab(tabWidth);
      IDocumentProvider provider = getDocumentProvider();
      if (provider instanceof ICompilationUnitDocumentProvider) {
        ICompilationUnitDocumentProvider cup = (ICompilationUnitDocumentProvider) provider;
        tabToSpacesConverter.setLineTracker(cup.createLineTracker(getEditorInput()));
      } else {
        tabToSpacesConverter.setLineTracker(new DefaultLineTracker());
      }
      ((ITextViewerExtension7) sourceViewer).setTabsToSpacesConverter(tabToSpacesConverter);
      updateIndentPrefixes();
    }
  }

  /**
   * Tells whether this is the active editor in the active page.
   * 
   * @return <code>true</code> if this is the active editor in the active page
   * @see IWorkbenchPage#getActiveEditor
   */
  protected final boolean isActiveEditor() {
    IWorkbenchWindow window = getSite().getWorkbenchWindow();
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return false;
    }
    IEditorPart activeEditor = page.getActiveEditor();
    return activeEditor != null && activeEditor.equals(this);
  }

  /*
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#openSaveErrorDialog(java.lang .String,
   * java.lang.String, org.eclipse.core.runtime.CoreException)
   */
  @Override
  protected void openSaveErrorDialog(String title, String message, CoreException exception) {
    IStatus status = exception.getStatus();
    if (DartUI.ID_PLUGIN.equals(status.getPlugin())
        && status.getCode() == DartStatusConstants.EDITOR_POST_SAVE_NOTIFICATION) {
      int mask = IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR;
      ErrorDialog dialog = new ErrorDialog(getSite().getShell(), title, message, status, mask) {
        @Override
        protected Control createDialogArea(Composite parent) {
          parent = (Composite) super.createDialogArea(parent);
          Link link = new Link(parent, SWT.NONE);
          link.setText(DartEditorMessages.CompilationUnitEditor_error_saving_saveParticipant);
          link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              PreferencesUtil.createPreferenceDialogOn(
                  getShell(),
                  "com.google.dart.tools.ui.internal.preferences.SaveParticipantPreferencePage", null, null).open(); //$NON-NLS-1$
            }
          });
          GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
          link.setLayoutData(gridData);
          return parent;
        }
      };
      dialog.open();
    } else {
      super.openSaveErrorDialog(title, message, exception);
    }
  }

  /*
   * @see org.eclipse.ui.texteditor.AbstractTextEditor#performSave(boolean,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
    IDocumentProvider p = getDocumentProvider();
    try {
      super.performSave(overwrite, progressMonitor);
    } finally {
      if (p instanceof ICompilationUnitDocumentProvider) {
        checkEditableState();
      }
    }
  }

  /*
   * @see AbstractTextEditor#rememberSelection()
   */
  @Override
  protected void rememberSelection() {
    fRememberedSelection.remember();
  }

  /*
   * @see AbstractTextEditor#restoreSelection()
   */
  @Override
  protected void restoreSelection() {
    fRememberedSelection.restore();
  }

  @Override
  protected void setContextMenuContext(IMenuManager menu, ActionContext context) {
    super.setContextMenuContext(menu, context);
    fContextMenuGroup.setContext(context);
    fContextMenuGroup.fillContextMenu(menu);
    fContextMenuGroup.setContext(null);
  }

  @Override
  protected void updateStateDependentActions() {
    super.updateStateDependentActions();
    DartX.todo("actions");
    fGenerateActionGroup.editorStateChanged();
  }

  @Override
  protected void updateStatusField(String category) {
    super.updateStatusField(category);

    if (ITextEditorActionConstants.STATUS_CATEGORY_INPUT_POSITION.equals(category)) {
      IStatusField field = getStatusField(IDartEditorActionConstants.STATUS_CATEGORY_OFFSET);
      if (field != null) {
        ISourceViewer sourceViewer = getSourceViewer();
        Point selection = sourceViewer.getTextWidget().getSelection();
        int offset1 = widgetOffset2ModelOffset(sourceViewer, selection.x);
        int offset2 = widgetOffset2ModelOffset(sourceViewer, selection.y);
        String text = null;
        if (offset1 != offset2) {
          text = "[" + offset1 + "-" + offset2 + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else {
          text = "[ " + offset1 + " ]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        field.setText(text);
      }
    }
  }

  /**
   * Adds the given listener. Has no effect if an identical listener was not already registered.
   * 
   * @param listener The reconcile listener to be added
   */
  final void addReconcileListener_OLD(IDartReconcilingListener_OLD listener) {
    synchronized (fReconcilingListeners_OLD) {
      fReconcilingListeners_OLD.add(listener);
    }
  }

  /**
   * Removes the given listener. Has no effect if an identical listener was not already registered.
   * 
   * @param listener the reconcile listener to be removed
   */
  final void removeReconcileListener_OLD(IDartReconcilingListener_OLD listener) {
    synchronized (fReconcilingListeners_OLD) {
      fReconcilingListeners_OLD.remove(listener);
    }
  }

  /**
   * Configures the toggle comment action
   */
  private void configureToggleCommentAction() {
    IAction action = getAction("ToggleComment"); //$NON-NLS-1$
    if (action instanceof ToggleCommentAction) {
      ISourceViewer sourceViewer = getSourceViewer();
      SourceViewerConfiguration configuration = getSourceViewerConfiguration();
      ((ToggleCommentAction) action).configure(sourceViewer, configuration);
    }
  }

  /**
   * We need to force reconcile each time when builder reanalyzes underlying file. For example: use
   * imports from saved unit; update problems-as-you-type after external change, such as reanalyze
   * all.
   */
  private void scheduleReconcileAfterBuild() {
    //TODO (pquitslund): investigate whether we need hooks for reconcile on build
  }
}
