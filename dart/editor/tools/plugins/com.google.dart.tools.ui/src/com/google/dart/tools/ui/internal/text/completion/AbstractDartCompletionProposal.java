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
package com.google.dart.tools.ui.internal.text.completion;

import com.google.dart.engine.utilities.general.CharOperation;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.dart.SmartSemicolonAutoEditStrategy;
import com.google.dart.tools.ui.internal.text.html.HTMLPrinter;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.DartTextTools;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.osgi.framework.Bundle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public abstract class AbstractDartCompletionProposal implements IDartCompletionProposal,
    ICompletionProposalExtension, ICompletionProposalExtension2, ICompletionProposalExtension3,
    ICompletionProposalExtension5, ICompletionProposalExtension6 {

  /**
   * Presenter control creator.
   */
  public static final class PresenterControlCreator extends
      AbstractReusableInformationControlCreator {
    @Override
    @SuppressWarnings("restriction")
    public IInformationControl doCreateInformationControl(Shell parent) {
      String font = PreferenceConstants.APPEARANCE_JAVADOC_FONT;
      return new org.eclipse.jface.internal.text.html.BrowserInformationControl(parent, font, true);
    }
  }

  /**
   * Allow the linked mode editor to continue running even when the exit character is typed as part
   * of a function argument. Using shift operators in a context that expects balanced angle brackets
   * is not legal syntax and will confuse the linked mode editor.
   */
  protected class ExitPolicy implements IExitPolicy {

    private int parenCount = 0;
    private int braceCount = 0;
    private int bracketCount = 0;
    private int angleBracketCount = 0;
    private char lastChar = (char) 0;

    final char fExitCharacter;
    private final IDocument fDocument;

    public ExitPolicy(char exitCharacter, IDocument document) {
      fExitCharacter = exitCharacter;
      fDocument = document;
    }

    @Override
    public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset, int length) {
      countGroupChars(event);
      if (event.character == fExitCharacter && isBalanced(fExitCharacter)) {
        if (environment.anyPositionContains(offset)) {
          return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
        } else {
          return new ExitFlags(ILinkedModeListener.UPDATE_CARET, true);
        }
      }

      switch (event.character) {
        case ';':
          return new ExitFlags(ILinkedModeListener.EXTERNAL_MODIFICATION
              | ILinkedModeListener.UPDATE_CARET | ILinkedModeListener.EXIT_ALL, true);
        case '\b':
          if (fInvocationContext != null) {
            if (fInvocationContext.getViewer().getSelectedRange().y > 0) {
              return new ExitFlags(ILinkedModeListener.EXTERNAL_MODIFICATION, true);
            }
          }
          return null;
        case SWT.CR:
          // when entering a function as a parameter, we don't want
          // to jump after the parenthesis when return is pressed
          if (offset > 0) {
            try {
              if (fDocument.getChar(offset - 1) == '{') {
                return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
              }
            } catch (BadLocationException e) {
            }
          }
          return null;
//        case ',':
//          // Making comma act like tab seems like a good idea but it requires auto-insert of matching group chars to work.
//          if (offset > 0) {
//            try {
//              if (fDocument.getChar(offset) == ',') {
//                event.character = 0x09;
//                return null;
//              }
//            } catch (BadLocationException e) {
//            }
//          }
        default:
          return null;
      }
    }

    private void countGroupChar(char ch, int inc) {
      switch (ch) {
        case '(':
          parenCount += inc;
          break;
        case ')':
          parenCount -= inc;
          break;
        case '{':
          braceCount += inc;
          break;
        case '}':
          braceCount -= inc;
          break;
        case '[':
          bracketCount += inc;
          break;
        case ']':
          bracketCount -= inc;
          break;
        case '<':
          angleBracketCount += inc;
          break;
        case '>':
          if (lastChar != '=') {
            // only decrement when not part of =>
            angleBracketCount -= inc;
          }
          break;
        case '=':
          if (lastChar == '>') {
            // deleting => should not change angleBracketCount
            angleBracketCount += inc;
          }
          break;
        default:
          break;
      }
      lastChar = ch;
    }

    private void countGroupChars(VerifyEvent event) {
      char ch = event.character;
      int inc = 1;
      if (ch == '\b') { // TODO Find correct delete chars for Linux & Windows
        inc = -1;
        if (!(event.widget instanceof StyledText)) {
          return;
        }
        Point sel = ((StyledText) event.widget).getSelection();
        try {
          if (sel.x == sel.y) {
            ch = fDocument.getChar(sel.x);
            countGroupChar(ch, inc);
          } else {
            for (int x = sel.y - 1; x >= sel.x; x--) {
              ch = fDocument.getChar(x);
              countGroupChar(ch, inc);
            }
          }
        } catch (BadLocationException ex) {
          return;
        }
      } else {
        countGroupChar(ch, inc);
      }
    }

    private boolean isBalanced(char ch) {
      switch (ch) {
        case ')':
          return parenCount == -1;
        case '}':
          return braceCount == -1;
        case ']':
          return bracketCount == -1;
        case '>':
          return angleBracketCount == -1;
        default:
          return true; // never unbalanced
      }
    }
  }

  /**
   * A class to simplify tracking a reference position in a document.
   */
  static final class ReferenceTracker {

    /** The reference position category name. */
    private static final String CATEGORY = "reference_position"; //$NON-NLS-1$
    /** The position updater of the reference position. */
    private final IPositionUpdater fPositionUpdater = new DefaultPositionUpdater(CATEGORY);
    /** The reference position. */
    private final Position fPosition = new Position(0);

    /**
     * Called after the document changed occurred. It must be preceded by a call to preReplace().
     * 
     * @param document the document on which to track the reference position.
     * @return offset after the replace
     */
    public int postReplace(IDocument document) {
      try {
        document.removePosition(CATEGORY, fPosition);
        document.removePositionUpdater(fPositionUpdater);
        document.removePositionCategory(CATEGORY);

      } catch (BadPositionCategoryException e) {
        // should not happen
        DartToolsPlugin.log(e);
      }
      return fPosition.getOffset();
    }

    /**
     * Called before document changes occur. It must be followed by a call to postReplace().
     * 
     * @param document the document on which to track the reference position.
     * @param offset the offset
     * @throws BadLocationException if the offset describes an invalid range in this document
     */
    public void preReplace(IDocument document, int offset) throws BadLocationException {
      fPosition.setOffset(offset);
      try {
        document.addPositionCategory(CATEGORY);
        document.addPositionUpdater(fPositionUpdater);
        document.addPosition(CATEGORY, fPosition);

      } catch (BadPositionCategoryException e) {
        // should not happen
        DartToolsPlugin.log(e);
      }
    }
  }

  /**
   * The control creator.
   */
  private static final class ControlCreator extends AbstractReusableInformationControlCreator {
    @Override
    @SuppressWarnings("restriction")
    public IInformationControl doCreateInformationControl(Shell parent) {
      String font = PreferenceConstants.APPEARANCE_JAVADOC_FONT;
      return new org.eclipse.jface.internal.text.html.BrowserInformationControl(
          parent,
          font,
          DartToolsPlugin.getAdditionalInfoAffordanceString()) {
        @Override
        public IInformationControlCreator getInformationPresenterControlCreator() {
          return new PresenterControlCreator();
        }
      };
    }
  }

  protected static boolean insertCompletion() {
    IPreferenceStore preference = DartToolsPlugin.getDefault().getPreferenceStore();
    return preference.getBoolean(PreferenceConstants.CODEASSIST_INSERT_COMPLETION);
  }

  private static Color getBackgroundColor() {
    IPreferenceStore preference = DartToolsPlugin.getDefault().getPreferenceStore();
    RGB rgb = PreferenceConverter.getColor(
        preference,
        PreferenceConstants.CODEASSIST_REPLACEMENT_BACKGROUND);
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    return textTools.getColorManager().getColor(rgb);
  }

  private static Color getForegroundColor() {
    IPreferenceStore preference = DartToolsPlugin.getDefault().getPreferenceStore();
    RGB rgb = PreferenceConverter.getColor(
        preference,
        PreferenceConstants.CODEASSIST_REPLACEMENT_FOREGROUND);
    DartTextTools textTools = DartToolsPlugin.getDefault().getDartTextTools();
    return textTools.getColorManager().getColor(rgb);
  }

  private StyledString fDisplayString;
  private String fReplacementString;
  private int fReplacementOffset;
  private int fReplacementLength;
  private int fReplacementLengthIdentifier;
  private int fCursorPosition;
  private Image fImage;
  private IContextInformation fContextInformation;
  private ProposalInfo fProposalInfo;

  private char[] fTriggerCharacters;

  private String fSortString;
  private int fRelevance;

  private boolean fIsInJavadoc;
  private StyleRange fRememberedStyleRange;

  private boolean fToggleEating;

  private ITextViewer fTextViewer;

  /**
   * The control creator.
   */
  private IInformationControlCreator fCreator;

  /**
   * The CSS used to format javadoc information.
   */
  private static String fgCSSStyles;

  /**
   * The invocation context of this completion proposal. Can be <code>null</code>.
   */
  protected final DartContentAssistInvocationContext fInvocationContext;

  /**
   * Cache to store last validation state.
   */
  private boolean fIsValidated = true;

  /**
   * The text presentation listener.
   */
  private ITextPresentationListener fTextPresentationListener;

  protected boolean triggerCompletionAfterApply = false;

  protected AbstractDartCompletionProposal() {
    fInvocationContext = null;
  }

  protected AbstractDartCompletionProposal(DartContentAssistInvocationContext context) {
    fInvocationContext = context;
  }

  @Override
  public final void apply(IDocument document) {
    // not used any longer
    apply(document, (char) 0, getReplacementOffset() + getReplacementLength());
  }

  @Override
  public void apply(IDocument document, char trigger, int offset) {

    InstrumentationBuilder instrumentation = Instrumentation.builder("CompletionProposal-Apply");
    instrumentation.metric("Trigger", trigger);

    try {
//      if (isSupportingRequiredProposals()) {
//        CompletionProposal coreProposal = ((MemberProposalInfo) getProposalInfo()).fProposal;
//        CompletionProposal[] requiredProposals = coreProposal.getRequiredProposals();
//        for (int i = 0; requiredProposals != null && i < requiredProposals.length; i++) {
//          int oldLen = document.getLength();
//          if (requiredProposals[i].getKind() == CompletionProposal.TYPE_REF) {
//            LazyDartCompletionProposal proposal = createRequiredTypeCompletionProposal(
//                requiredProposals[i],
//                fInvocationContext);
//            proposal.apply(document);
//            setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
//          } else {
//            /*
//             * we only support the above required proposals, see
//             * CompletionProposal#getRequiredProposals()
//             */
//            Assert.isTrue(false);
//          }
//        }
//      }

      try {
        boolean isSmartTrigger = isSmartTrigger(trigger);
        instrumentation.metric("isSmartTrigger", isSmartTrigger);

        String replacement;
        if (isSmartTrigger || trigger == (char) 0) {
          replacement = getReplacementString();
        } else {
          StringBuffer buffer = new StringBuffer(getReplacementString());

          // fix for PR #5533. Assumes that no eating takes place.
          if ((getCursorPosition() > 0 && getCursorPosition() <= buffer.length() && buffer.charAt(getCursorPosition() - 1) != trigger)) {
            buffer.insert(getCursorPosition(), trigger);
            setCursorPosition(getCursorPosition() + 1);
          }

          replacement = buffer.toString();
          setReplacementString(replacement);
        }

        instrumentation.data("Replacement", replacement);

        // reference position just at the end of the document change.
        int referenceOffset = getReplacementOffset() + getReplacementLength();
        final ReferenceTracker referenceTracker = new ReferenceTracker();
        referenceTracker.preReplace(document, referenceOffset);

        replace(document, getReplacementOffset(), getReplacementLength(), replacement);

        referenceOffset = referenceTracker.postReplace(document);
        int delta = replacement == null ? 0 : replacement.length();
        if (delta > 0 && replacement.charAt(replacement.length() - 1) == ']') {
          delta += 1;
        }
        setReplacementOffset(referenceOffset - delta);

        // PR 47097
        if (isSmartTrigger) {
          handleSmartTrigger(document, trigger, referenceOffset);
        }

      } catch (BadLocationException x) {
        instrumentation.metric("Problem", "BadLocationException");
        // ignore
      }
    } finally {
      instrumentation.log();
    }
  }

  @Override
  public void apply(final ITextViewer viewer, char trigger, int stateMask, int offset) {

    IDocument document = viewer.getDocument();
    if (fTextViewer == null) {
      fTextViewer = viewer;
    }

    // don't apply the proposal if for some reason we're not valid any longer
    if (!isInDartDoc() && !validate(document, offset, null)) {
      setCursorPosition(offset);
      if (trigger != '\0') {
        try {
          document.replace(offset, 0, String.valueOf(trigger));
          setCursorPosition(getCursorPosition() + 1);
          if (trigger == '(' && autocloseBrackets()) {
            document.replace(getReplacementOffset() + getCursorPosition(), 0, ")"); //$NON-NLS-1$
            setUpLinkedMode(document, ')');
          }
        } catch (BadLocationException x) {
          // ignore
        }
      }
      return;
    }

    // don't eat if not in preferences, XOR with Ctrl
    // but: if there is a selection, replace it!
    Point selection = viewer.getSelectedRange();
    int newLength = selection.x + selection.y - getReplacementOffset();
    fToggleEating = (stateMask & SWT.CTRL) != 0;
    if (fToggleEating) {
      newLength = getReplacementLengthIdentifier();
    }
    if (newLength >= 0) {
      setReplacementLength(newLength);
    }

    apply(document, trigger, offset);
    fToggleEating = false;

    if (triggerCompletionAfterApply) {
      if (viewer instanceof SourceViewer) {
        // run asynchronously to allow cursor to move
        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            ((SourceViewer) viewer).doOperation(SourceViewer.CONTENTASSIST_PROPOSALS);
          }
        });
      }
    }
  }

  @Override
  public String getAdditionalProposalInfo() {
    Object info = getAdditionalProposalInfo(new NullProgressMonitor());
    return info == null ? null : info.toString();
  }

  @Override
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    if (getProposalInfo() != null) {
      String info = getProposalInfo().getInfo(monitor);
      if (info != null && info.length() > 0) {
        StringBuffer buffer = new StringBuffer();
        HTMLPrinter.insertPageProlog(buffer, 0, getCSSStyles());
        buffer.append(info);
        HTMLPrinter.addPageEpilog(buffer);
        info = buffer.toString();
      }
      return info;
    }
    return null;
  }

  @Override
  public IContextInformation getContextInformation() {
    return fContextInformation;
  }

  @Override
  public int getContextInformationPosition() {
    if (getContextInformation() == null) {
      return getReplacementOffset() - 1;
    }
    return getReplacementOffset() + getCursorPosition();
  }

  @Override
  public String getDisplayString() {
    if (fDisplayString != null) {
      return fDisplayString.getString();
    }
    return ""; //$NON-NLS-1$
  }

  @Override
  public Image getImage() {
    return fImage;
  }

  @Override
  @SuppressWarnings("restriction")
  public IInformationControlCreator getInformationControlCreator() {
    // TODO(scheglov) Linux is known to crash sometimes when we create Browser.
    // https://code.google.com/p/dart/issues/detail?id=12903
    // It always was like this.
    if (DartCore.isLinux()) {
      return null;
    }
    // For luckier OSes.
    Shell shell = DartToolsPlugin.getActiveWorkbenchShell();
    if (shell == null
        || !org.eclipse.jface.internal.text.html.BrowserInformationControl.isAvailable(shell)) {
      return null;
    }

    if (fCreator == null) {
      fCreator = new ControlCreator();
    }
    return fCreator;
  }

  @Override
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    return getReplacementOffset();
  }

  @Override
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    if (!isCamelCaseMatching()) {
      return getReplacementString();
    }

    String prefix = getPrefix(document, completionOffset);
    return getCamelCaseCompound(prefix, getReplacementString());
  }

  /**
   * Gets the proposal's relevance.
   * 
   * @return Returns an int
   */
  @Override
  public int getRelevance() {
    return fRelevance;
  }

  /**
   * Gets the replacement length.
   * 
   * @return Returns an int
   */
  public int getReplacementLength() {
    return fReplacementLength;
  }

  /**
   * Gets the replacement length for identifier.
   * 
   * @return Returns an int
   */
  public int getReplacementLengthIdentifier() {
    return fReplacementLengthIdentifier;
  }

  /**
   * Gets the replacement offset.
   * 
   * @return Returns an int
   */
  public int getReplacementOffset() {
    return fReplacementOffset;
  }

  /**
   * Gets the replacement string.
   * 
   * @return Returns a String
   */
  public String getReplacementString() {
    return fReplacementString;
  }

  @Override
  public Point getSelection(IDocument document) {
    if (!fIsValidated) {
      return null;
    }
    return new Point(getReplacementOffset() + getCursorPosition(), 0);
  }

  public String getSortString() {
    return fSortString;
  }

  @Override
  public StyledString getStyledDisplayString() {
    return fDisplayString;
  }

  @Override
  public char[] getTriggerCharacters() {
    return fTriggerCharacters;
  }

  @Override
  public boolean isValidFor(IDocument document, int offset) {
    return validate(document, offset, null);
  }

  @Override
  public void selected(final ITextViewer viewer, boolean smartToggle) {
    repairPresentation(viewer);
    fRememberedStyleRange = null;

    if (!insertCompletion() ^ smartToggle) {
      StyleRange range = createStyleRange(viewer);
      if (range == null) {
        return;
      }

      fRememberedStyleRange = range;

      if (viewer instanceof ITextViewerExtension4) {
        if (fTextPresentationListener == null) {
          fTextPresentationListener = new ITextPresentationListener() {
            @Override
            public void applyTextPresentation(TextPresentation textPresentation) {
              fRememberedStyleRange = createStyleRange(viewer);
              if (fRememberedStyleRange != null) {
                textPresentation.mergeStyleRange(fRememberedStyleRange);
              }
            }
          };
          ((ITextViewerExtension4) viewer).addTextPresentationListener(fTextPresentationListener);
        }
        repairPresentation(viewer);
      } else {
        updateStyle(viewer);
      }
    }
  }

  /**
   * Sets the context information.
   * 
   * @param contextInformation The context information associated with this proposal
   */
  public void setContextInformation(IContextInformation contextInformation) {
    fContextInformation = contextInformation;
  }

  /**
   * Sets the cursor position relative to the insertion offset. By default this is the length of the
   * completion string (Cursor positioned after the completion)
   * 
   * @param cursorPosition The cursorPosition to set
   */
  public void setCursorPosition(int cursorPosition) {
    Assert.isTrue(cursorPosition >= 0);
    fCursorPosition = cursorPosition;
  }

  /**
   * Sets the image.
   * 
   * @param image The image to set
   */
  public void setImage(Image image) {
    fImage = image;
  }

  /**
   * Sets the proposal info.
   * 
   * @param proposalInfo The additional information associated with this proposal or
   *          <code>null</code>
   */
  public void setProposalInfo(ProposalInfo proposalInfo) {
    fProposalInfo = proposalInfo;
  }

  /**
   * Sets the proposal's relevance.
   * 
   * @param relevance The relevance to set
   */
  public void setRelevance(int relevance) {
    fRelevance = relevance;
  }

  /**
   * Sets the replacement length.
   * 
   * @param replacementLength The replacementLength to set
   */
  public void setReplacementLength(int replacementLength) {
    Assert.isTrue(replacementLength >= 0);
    fReplacementLength = replacementLength;
  }

  /**
   * Sets the replacement length for identifier.
   * 
   * @param length The replacementLength to set
   */
  public void setReplacementLengthIdentifier(int length) {
    Assert.isTrue(length >= 0);
    fReplacementLengthIdentifier = length;
  }

  /**
   * Sets the replacement offset.
   * 
   * @param replacementOffset The replacement offset to set
   */
  public void setReplacementOffset(int replacementOffset) {
    Assert.isTrue(replacementOffset >= 0);
    fReplacementOffset = replacementOffset;
  }

  /**
   * Sets the replacement string.
   * 
   * @param replacementString The replacement string to set
   */
  public void setReplacementString(String replacementString) {
    Assert.isNotNull(replacementString);
    fReplacementString = replacementString;
  }

  public void setStyledDisplayString(StyledString text) {
    fDisplayString = text;
  }

  /**
   * Sets the trigger characters.
   * 
   * @param triggerCharacters The set of characters which can trigger the application of this
   *          completion proposal
   */
  public void setTriggerCharacters(char[] triggerCharacters) {
    fTriggerCharacters = triggerCharacters;
  }

  @Override
  public String toString() {
    return getDisplayString();
  }

  @Override
  public void unselected(ITextViewer viewer) {
    if (fTextPresentationListener != null) {
      ((ITextViewerExtension4) viewer).removeTextPresentationListener(fTextPresentationListener);
      fTextPresentationListener = null;
    }
    repairPresentation(viewer);
    fRememberedStyleRange = null;
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {

    if (!isOffsetValid(offset)) {
      return fIsValidated = false;
    }

    fIsValidated = isValidPrefix(getPrefix(document, offset));

    if (fIsValidated && event != null) {
      // adapt replacement range to document change
      int delta = (event.fText == null ? 0 : event.fText.length()) - event.fLength;
      final int newLength = Math.max(getReplacementLength() + delta, 0);
      setReplacementLength(newLength);
    }

    return fIsValidated;
  }

  protected boolean autocloseBrackets() {
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    return preferenceStore.getBoolean(PreferenceConstants.EDITOR_CLOSE_BRACKETS);
  }

  /**
   * Matches <code>prefix</code> against <code>string</code> and replaces the matched region by
   * prefix. Case is preserved as much as possible. This method returns <code>string</code> if camel
   * case completion is disabled. Examples when camel case completion is enabled:
   * <ul>
   * <li>getCamelCompound("NuPo", "NullPointerException") -> "NuPointerException"</li>
   * <li>getCamelCompound("NuPoE", "NullPointerException") -> "NuPoException"</li>
   * <li>getCamelCompound("hasCod", "hashCode") -> "hasCode"</li>
   * </ul>
   * 
   * @param prefix the prefix to match against
   * @param string the string to match
   * @return a compound of prefix and any postfix taken from <code>string</code>
   */
  protected final String getCamelCaseCompound(String prefix, String string) {
    if (prefix.length() > string.length()) {
      return string;
    }

    // a normal prefix - no camel case logic at all
    String start = string.substring(0, prefix.length());
    if (start.equalsIgnoreCase(prefix)) {
      return string;
    }

    final char[] patternChars = prefix.toCharArray();
    final char[] stringChars = string.toCharArray();

    for (int i = 1; i <= stringChars.length; i++) {
      if (CharOperation.camelCaseMatch(patternChars, 0, patternChars.length, stringChars, 0, i)) {
        return prefix + string.substring(i);
      }
    }

    // Not a camel case match at all.
    // This should not happen -> stay with the default behavior
    return string;
  }

  /**
   * Returns the style information for displaying HTML content.
   * 
   * @return the CSS styles
   */
  protected String getCSSStyles() {
    if (fgCSSStyles == null) {
      Bundle bundle = Platform.getBundle(DartToolsPlugin.getPluginId());
      URL url = bundle.getEntry("/DartdocHoverStyleSheet.css"); //$NON-NLS-1$
      if (url != null) {
        BufferedReader reader = null;
        try {
          url = FileLocator.toFileURL(url);
          reader = new BufferedReader(new InputStreamReader(url.openStream()));
          StringBuffer buffer = new StringBuffer(200);
          String line = reader.readLine();
          while (line != null) {
            buffer.append(line);
            buffer.append('\n');
            line = reader.readLine();
          }
          fgCSSStyles = buffer.toString();
        } catch (IOException ex) {
          DartToolsPlugin.log(ex);
        } finally {
          try {
            if (reader != null) {
              reader.close();
            }
          } catch (IOException e) {
          }
        }

      }
    }
    String css = fgCSSStyles;
    if (css != null) {
      FontData fontData = JFaceResources.getFontRegistry().getFontData(
          PreferenceConstants.APPEARANCE_JAVADOC_FONT)[0];
      css = HTMLPrinter.convertTopLevelFont(css, fontData);
    }
    return css;
  }

  protected int getCursorPosition() {
    return fCursorPosition;
  }

  /**
   * Returns the text in <code>document</code> from {@link #getReplacementOffset()} to
   * <code>offset</code>. Returns the empty string if <code>offset</code> is before the replacement
   * offset or if an exception occurs when accessing the document.
   * 
   * @param document the document
   * @param offset the offset
   * @return the prefix
   */
  protected String getPrefix(IDocument document, int offset) {
    try {
      int length = offset - getReplacementOffset();
      if (length > 0) {
        return document.get(getReplacementOffset(), length);
      }
    } catch (BadLocationException x) {
    }
    return ""; //$NON-NLS-1$
  }

  /**
   * Returns the additional proposal info, or <code>null</code> if none exists.
   * 
   * @return the additional proposal info, or <code>null</code> if none exists
   */
  protected ProposalInfo getProposalInfo() {
    return fProposalInfo;
  }

  protected ITextViewer getTextViewer() {
    return fTextViewer;
  }

  /**
   * Returns true if camel case matching is enabled.
   * 
   * @return <code>true</code> if camel case matching is enabled
   */
  protected boolean isCamelCaseMatching() {
    return true;
  }

  /**
   * Returns <code>true</code> if the proposal is within Dart doc, <code>false</code> otherwise.
   * 
   * @return <code>true</code> if the proposal is within Dart doc, <code>false</code> otherwise
   */
  protected boolean isInDartDoc() {
    return fIsInJavadoc;
  }

  /**
   * Tells whether the user toggled the insert mode by pressing the 'Ctrl' key.
   * 
   * @return <code>true</code> if the insert mode is toggled, <code>false</code> otherwise
   */
  protected boolean isInsertModeToggled() {
    return fToggleEating;
  }

  /**
   * Checks whether the given offset is valid for this proposal.
   * 
   * @param offset the caret offset
   * @return <code>true</code> if the offset is valid for this proposal
   */
  protected boolean isOffsetValid(int offset) {
    return getReplacementOffset() <= offset;
  }

  /**
   * Case insensitive comparison of <code>prefix</code> with the start of <code>string</code>.
   * 
   * @param prefix the prefix
   * @param string the string to look for the prefix
   * @return <code>true</code> if the string begins with the given prefix and <code>false</code> if
   *         <code>prefix</code> is longer than <code>string</code> or the string doesn't start with
   *         the given prefix
   */
  protected boolean isPrefix(String prefix, String string) {
    if (prefix == null || string == null || prefix.length() > string.length()) {
      return false;
    }
    String start = string.substring(0, prefix.length());
    return start.equalsIgnoreCase(prefix) || isCamelCaseMatching()
        && CharOperation.camelCaseMatch(prefix.toCharArray(), string.toCharArray());
  }

  /**
   * Tells whether required proposals are supported by this proposal.
   * 
   * @return <code>true</code> if required proposals are supported by this proposal
   */
  protected boolean isSupportingRequiredProposals() {
    if (fInvocationContext == null) {
      return false;
    }

    ProposalInfo proposalInfo = getProposalInfo();
    CompletionProposal proposal = proposalInfo.getProposal();

    if (proposal == null) {
      return false;
    }

    int kind = proposal.getKind();
    return (kind == CompletionProposal.METHOD_REF || kind == CompletionProposal.ARGUMENT_LIST
        || kind == CompletionProposal.FIELD_REF || kind == CompletionProposal.TYPE_REF
//          || kind == CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION
    || kind == CompletionProposal.CONSTRUCTOR_INVOCATION);
  }

  protected boolean isToggleEating() {
    return fToggleEating;
  }

  /**
   * Checks whether <code>prefix</code> is a valid prefix for this proposal. Usually, while code
   * completion is in progress, the user types and edits the prefix in the document in order to
   * filter the proposal list. From {@link #validate(IDocument, int, DocumentEvent) }, the current
   * prefix in the document is extracted and this method is called to find out whether the proposal
   * is still valid.
   * <p>
   * The default implementation checks if <code>prefix</code> is a prefix of the proposal's
   * {@link #getDisplayString() display string} using the {@link #isPrefix(String, String) } method.
   * </p>
   * 
   * @param prefix the current prefix in the document
   * @return <code>true</code> if <code>prefix</code> is a valid prefix of this proposal
   */
  protected boolean isValidPrefix(String prefix) {
    /*
     * See http://dev.eclipse.org/bugs/show_bug.cgi?id=17667 why we do not use the replacement
     * string. String word= fReplacementString;
     * 
     * Besides that bug we also use the display string for performance reasons, as computing the
     * replacement string can be expensive.
     */
    return isPrefix(prefix, TextProcessor.deprocess(getDisplayString()));
  }

  protected final void replace(IDocument document, int offset, int length, String string)
      throws BadLocationException {
    if (!document.get(offset, length).equals(string)) {
      document.replace(offset, length, string);
    }
  }

  protected void setDisplayString(String string) {
    fDisplayString = new StyledString(string);
  }

  /**
   * Sets the Dava doc attribute.
   * 
   * @param isInJavadoc <code>true</code> if the proposal is within javadoc
   */
  protected void setInDartDoc(boolean isInJavadoc) {
    fIsInJavadoc = isInJavadoc;
  }

  protected void setSortString(String string) {
    fSortString = string;
  }

  /**
   * Sets up a simple linked mode at {@link #getCursorPosition()} and an exit policy that will exit
   * the mode when <code>closingCharacter</code> is typed and an exit position at
   * <code>getCursorPosition() + 1</code>.
   * 
   * @param document the document
   * @param closingCharacter the exit character
   */
  protected void setUpLinkedMode(IDocument document, char closingCharacter) {
    if (getTextViewer() != null && autocloseBrackets()) {
      int offset = getReplacementOffset() + getCursorPosition();
      int exit = getReplacementOffset() + getReplacementString().length();
      try {
        LinkedPositionGroup group = new LinkedPositionGroup();
        group.addPosition(new LinkedPosition(document, offset, 0, LinkedPositionGroup.NO_STOP));

        LinkedModeModel model = new LinkedModeModel();
        model.addGroup(group);
        model.forceInstall();

        LinkedModeUI ui = new EditorLinkedModeUI(model, getTextViewer());
        ui.setSimpleMode(true);
        ui.setExitPolicy(new ExitPolicy(closingCharacter, document));
        ui.setExitPosition(getTextViewer(), exit, 0, Integer.MAX_VALUE);
        ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
        ui.enter();
      } catch (BadLocationException x) {
        DartToolsPlugin.log(x);
      }
    }
  }

  /**
   * Creates a style range for the text viewer.
   * 
   * @param viewer the text viewer
   * @return the new style range for the text viewer or <code>null</code>
   */
  private StyleRange createStyleRange(ITextViewer viewer) {
    StyledText text = viewer.getTextWidget();
    if (text == null || text.isDisposed()) {
      return null;
    }

    int widgetCaret = text.getCaretOffset();

    int modelCaret = 0;
    if (viewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      modelCaret = extension.widgetOffset2ModelOffset(widgetCaret);
    } else {
      IRegion visibleRegion = viewer.getVisibleRegion();
      modelCaret = widgetCaret + visibleRegion.getOffset();
    }

    if (modelCaret >= getReplacementOffset() + getReplacementLength()) {
      return null;
    }

    int length = getReplacementOffset() + getReplacementLength() - modelCaret;

    Color foreground = getForegroundColor();
    Color background = getBackgroundColor();

    return new StyleRange(modelCaret, length, foreground, background);
  }

  @SuppressWarnings("unused")
  private IWorkbenchSite getSite() {
    IWorkbenchPage page = DartToolsPlugin.getActivePage();
    if (page != null) {
      IWorkbenchPart part = page.getActivePart();
      if (part != null) {
        return part.getSite();
      }
    }
    return null;
  }

  /**
   * Convert a document offset to the corresponding widget offset.
   * 
   * @param viewer the text viewer
   * @param documentOffset the document offset
   * @return widget offset
   */
  private int getWidgetOffset(ITextViewer viewer, int documentOffset) {
    if (viewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      return extension.modelOffset2WidgetOffset(documentOffset);
    }
    IRegion visible = viewer.getVisibleRegion();
    int widgetOffset = documentOffset - visible.getOffset();
    if (widgetOffset > visible.getLength()) {
      return -1;
    }
    return widgetOffset;
  }

  private void handleSmartTrigger(IDocument document, char trigger, int referenceOffset)
      throws BadLocationException {
    DocumentCommand cmd = new DocumentCommand() {
    };

    cmd.offset = referenceOffset;
    cmd.length = 0;
    cmd.text = Character.toString(trigger);
    cmd.doit = true;
    cmd.shiftsCaret = true;
    cmd.caretOffset = getReplacementOffset() + getCursorPosition();

    SmartSemicolonAutoEditStrategy strategy = new SmartSemicolonAutoEditStrategy(
        DartPartitions.DART_PARTITIONING);
    strategy.customizeDocumentCommand(document, cmd);

    replace(document, cmd.offset, cmd.length, cmd.text);
    setCursorPosition(cmd.caretOffset - getReplacementOffset() + cmd.text.length());
  }

  private boolean isSmartTrigger(char trigger) {
    return trigger == ';'
        && DartToolsPlugin.getDefault().getCombinedPreferenceStore().getBoolean(
            PreferenceConstants.EDITOR_SMART_SEMICOLON)
        || trigger == '{'
        && DartToolsPlugin.getDefault().getCombinedPreferenceStore().getBoolean(
            PreferenceConstants.EDITOR_SMART_OPENING_BRACE);
  }

  private void repairPresentation(ITextViewer viewer) {
    if (fRememberedStyleRange != null) {
      if (viewer instanceof ITextViewerExtension2) {
        // attempts to reduce the redraw area
        ITextViewerExtension2 viewer2 = (ITextViewerExtension2) viewer;
        viewer2.invalidateTextPresentation(
            fRememberedStyleRange.start,
            fRememberedStyleRange.length);
      } else {
        viewer.invalidateTextPresentation();
      }
    }
  }

  private void updateStyle(ITextViewer viewer) {
    StyledText text = viewer.getTextWidget();
    int widgetOffset = getWidgetOffset(viewer, fRememberedStyleRange.start);
    StyleRange range = new StyleRange(fRememberedStyleRange);
    range.start = widgetOffset;
    range.length = fRememberedStyleRange.length;
    StyleRange currentRange = text.getStyleRangeAtOffset(widgetOffset);
    if (currentRange != null) {
      range.strikeout = currentRange.strikeout;
      range.underline = currentRange.underline;
      range.fontStyle = currentRange.fontStyle;
    }

    // http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
    try {
      text.setStyleRange(range);
    } catch (IllegalArgumentException x) {
      // catching exception as offset + length might be outside of the text widget
      fRememberedStyleRange = null;
    }
  }

}
