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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.InclusivePositionUpdater;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

/**
 * This is a {@link com.google.dart.tools.ui.internal.text.dart.DartCompletionProposal} which
 * includes templates that represent the best guess completion for each parameter of a method.
 */
public final class ParameterGuessingProposal extends DartMethodCompletionProposal {

  /** Tells whether this class is in debug mode. */
  private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("com.google.dart.tools.ui/debug/ResultCollector")); //$NON-NLS-1$//$NON-NLS-2$

  private ICompletionProposal[][] fChoices; // initialized by guessParameters()
  private Position[] fPositions; // initialized by guessParameters()

  private IRegion fSelectedRegion; // initialized by apply()
  private IPositionUpdater fUpdater;

  public ParameterGuessingProposal(CompletionProposal proposal,
      DartContentAssistInvocationContext context) {
    super(proposal, context);
  }

  /*
   * @see ICompletionProposalExtension#apply(IDocument, char)
   */
  @Override
  public void apply(IDocument document, char trigger, int offset) {
    try {
      super.apply(document, trigger, offset);

      int baseOffset = getReplacementOffset();
      String replacement = getReplacementString();

      if (fPositions != null && getTextViewer() != null) {

        LinkedModeModel model = new LinkedModeModel();

        for (int i = 0; i < fPositions.length; i++) {
          LinkedPositionGroup group = new LinkedPositionGroup();
          int positionOffset = fPositions[i].getOffset();
          int positionLength = fPositions[i].getLength();

          if (fChoices[i].length < 2) {
            group.addPosition(new LinkedPosition(document, positionOffset, positionLength,
                LinkedPositionGroup.NO_STOP));
          } else {
            ensurePositionCategoryInstalled(document, model);
            document.addPosition(getCategory(), fPositions[i]);
            group.addPosition(new ProposalPosition(document, positionOffset, positionLength,
                LinkedPositionGroup.NO_STOP, fChoices[i]));
          }
          model.addGroup(group);
        }

        model.forceInstall();
        DartEditor editor = getJavaEditor();
        if (editor != null) {
          model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
        }

        LinkedModeUI ui = new EditorLinkedModeUI(model, getTextViewer());
        ui.setExitPosition(getTextViewer(), baseOffset + replacement.length(), 0, Integer.MAX_VALUE);
        ui.setExitPolicy(new ExitPolicy(')', document));
        ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
        ui.setDoContextInfo(true);
        ui.enter();
        fSelectedRegion = ui.getSelectedRegion();

      } else {
        fSelectedRegion = new Region(baseOffset + replacement.length(), 0);
      }

    } catch (BadLocationException e) {
      ensurePositionCategoryRemoved(document);
      DartToolsPlugin.log(e);
      openErrorDialog(e);
    } catch (BadPositionCategoryException e) {
      ensurePositionCategoryRemoved(document);
      DartToolsPlugin.log(e);
      openErrorDialog(e);
    }
  }

  /*
   * @see ICompletionProposal#getSelection(IDocument)
   */
  @Override
  public Point getSelection(IDocument document) {
    if (fSelectedRegion == null) {
      return new Point(getReplacementOffset(), 0);
    }

    return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartMethodCompletionProposal#
   * computeReplacementString()
   */
  @Override
  protected String computeReplacementString() {

    if (!hasParameters() || !hasArgumentList()) {
      return super.computeReplacementString();
    }

    long millis = DEBUG ? System.currentTimeMillis() : 0;
    String replacement;
    try {
      replacement = computeGuessingCompletion();
    } catch (DartModelException x) {
      fPositions = null;
      fChoices = null;
      DartToolsPlugin.log(x);
      openErrorDialog(x);
      return super.computeReplacementString();
    }
    if (DEBUG) {
      System.err.println("Parameter Guessing: " + (System.currentTimeMillis() - millis)); //$NON-NLS-1$ 
    }

    return replacement;
  }

  /*
   * @see com.google.dart.tools.ui.internal.text.dart.DartMethodCompletionProposal #needsLinkedMode
   * ()
   */
  @Override
  protected boolean needsLinkedMode() {
    return false; // we handle it ourselves
  }

  /**
   * Creates the completion string. Offsets and Lengths are set to the offsets and lengths of the
   * parameters.
   */
  private String computeGuessingCompletion() throws DartModelException {

    StringBuffer buffer = new StringBuffer(String.valueOf(fProposal.getName()));

    FormatterPrefs prefs = getFormatterPrefs();
    if (prefs.beforeOpeningParen) {
      buffer.append(SPACE);
    }
    buffer.append(LPAREN);

    setCursorPosition(buffer.length());

    if (prefs.afterOpeningParen) {
      buffer.append(SPACE);
    }

    fChoices = guessParameters();
    int count = fChoices.length;
    int replacementOffset = getReplacementOffset();

    for (int i = 0; i < count; i++) {
      if (i != 0) {
        if (prefs.beforeComma) {
          buffer.append(SPACE);
        }
        buffer.append(COMMA);
        if (prefs.afterComma) {
          buffer.append(SPACE);
        }
      }

      ICompletionProposal proposal = fChoices[i][0];
      String argument = proposal.getDisplayString();
      Position position = fPositions[i];
      position.setOffset(replacementOffset + buffer.length());
      position.setLength(argument.length());
      if (proposal instanceof DartCompletionProposal) {
        // case where we only insert a proposal.
        ((DartCompletionProposal) proposal).setReplacementOffset(replacementOffset
            + buffer.length());
      }
      buffer.append(argument);
    }

    if (prefs.beforeClosingParen) {
      buffer.append(SPACE);
    }

    buffer.append(RPAREN);

    return buffer.toString();
  }

  private void ensurePositionCategoryInstalled(final IDocument document, LinkedModeModel model) {
    if (!document.containsPositionCategory(getCategory())) {
      document.addPositionCategory(getCategory());
      fUpdater = new InclusivePositionUpdater(getCategory());
      document.addPositionUpdater(fUpdater);

      model.addLinkingListener(new ILinkedModeListener() {

        /*
         * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.
         * jface.text.link.LinkedModeModel, int)
         */
        @Override
        public void left(LinkedModeModel environment, int flags) {
          ensurePositionCategoryRemoved(document);
        }

        @Override
        public void resume(LinkedModeModel environment, int flags) {
        }

        @Override
        public void suspend(LinkedModeModel environment) {
        }
      });
    }
  }

  private void ensurePositionCategoryRemoved(IDocument document) {
    if (document.containsPositionCategory(getCategory())) {
      try {
        document.removePositionCategory(getCategory());
      } catch (BadPositionCategoryException e) {
        // ignore
      }
      document.removePositionUpdater(fUpdater);
    }
  }

  private String getCategory() {
    return "ParameterGuessingProposal_" + toString(); //$NON-NLS-1$
  }

  /**
   * Returns the currently active java editor, or <code>null</code> if it cannot be determined.
   * 
   * @return the currently active java editor, or <code>null</code>
   */
  private DartEditor getJavaEditor() {
    IEditorPart part = DartToolsPlugin.getActivePage().getActiveEditor();
    if (part instanceof DartEditor) {
      return (DartEditor) part;
    } else {
      return null;
    }
  }

  private String[][] getParameterSignatures() {
    char[] signature = fProposal.getSignature();
    char[][] types = Signature.getParameterTypes(signature);
    String[][] ret = new String[types.length][2];

    for (int i = 0; i < types.length; i++) {
      char[] type = types[i];
      ret[i][0] = String.valueOf(Signature.getSignatureQualifier(type));
      ret[i][1] = String.valueOf(Signature.getSignatureSimpleName(type));
    }
    return ret;
  }

  private ICompletionProposal[][] guessParameters() throws DartModelException {
    // find matches in reverse order. Do this because people tend to declare the
// variable meant for the last
    // parameter last. That is, local variables for the last parameter in the
// method completion are more
    // likely to be closer to the point of code completion. As an example
// consider a "delegation" completion:
    //
    // public void myMethod(int param1, int param2, int param3) {
    // someOtherObject.yourMethod(param1, param2, param3);
    // }
    //
    // The other consideration is giving preference to variables that have not
// previously been used in this
    // code completion (which avoids
// "someOtherObject.yourMethod(param1, param1, param1)";

    char[][] parameterNames = fProposal.findParameterNames(null);
    int count = parameterNames.length;
    fPositions = new Position[count];
    fChoices = new ICompletionProposal[count][];

    IDocument document = fInvocationContext.getDocument();
    CompilationUnit cu = fInvocationContext.getCompilationUnit();
    DartModelUtil.reconcile(cu);
    String[][] parameterTypes = getParameterSignatures();
    ParameterGuesser guesser = new ParameterGuesser(fProposal.getCompletionLocation() + 1, cu);

    for (int i = count - 1; i >= 0; i--) {
      String paramName = new String(parameterNames[i]);
      Position position = new Position(0, 0);

      ICompletionProposal[] argumentProposals = guesser.parameterProposals(parameterTypes[i][0],
          parameterTypes[i][1], paramName, position, document);
      if (argumentProposals.length == 0) {
        argumentProposals = new ICompletionProposal[] {new DartCompletionProposal(paramName, 0,
            paramName.length(), null, paramName, 0)};
      }

      fPositions[i] = position;
      fChoices[i] = argumentProposals;
    }

    return fChoices;
  }

  private void openErrorDialog(Exception e) {
    Shell shell = getTextViewer().getTextWidget().getShell();
    MessageDialog.openError(shell, DartTextMessages.ParameterGuessingProposal_error_msg,
        e.getMessage());
  }

}
