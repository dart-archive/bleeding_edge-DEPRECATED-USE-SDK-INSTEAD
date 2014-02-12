/*
 * Copyright (c) 2013, the Dart project authors.
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

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.dart.DartTextMessages;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import java.util.ArrayList;
import java.util.List;

/**
 * A method proposal with filled in argument names.
 */
public final class FilledArgumentNamesMethodProposal extends DartMethodCompletionProposal {

  private IRegion fSelectedRegion; // initialized by apply()
  private int[] fArgumentOffsets;
  private int[] fArgumentLengths;

  public FilledArgumentNamesMethodProposal(CompletionProposal proposal,
      DartContentAssistInvocationContext context) {
    super(proposal, context);
  }

  @Override
  public void apply(final IDocument document, char trigger, int offset) {
    super.apply(document, trigger, offset);
    int baseOffset = getReplacementOffset();
    String replacement = getReplacementString();

    ITextViewer textViewer = getTextViewer();
    if (fArgumentOffsets != null && textViewer != null) {
      try {
        OptionalArgumentModel model = new OptionalArgumentModel();
        buildLinkedModeModel(model, document, baseOffset);

        model.forceInstall();
        DartEditor editor = getDartEditor();
        if (editor != null) {
          model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
        }

        LinkedModeUI ui = new EditorLinkedModeUI(model, textViewer);
        ui.setExitPosition(textViewer, baseOffset + replacement.length(), 0, Integer.MAX_VALUE);
        model.exitPositionUpdater(ui, textViewer, baseOffset + replacement.length());
        ui.setExitPolicy(new ExitPolicy(')', document) {
          @Override
          public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset,
              int length) {
            maybeRewriteCommaWithTab(document, event, offset);
            return super.doExit(environment, event, offset, length);
          }
        });
        ui.setDoContextInfo(true);
        ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
        ui.enter();

        fSelectedRegion = ui.getSelectedRegion();

      } catch (BadLocationException e) {
        DartToolsPlugin.log(e);
        openErrorDialog(e);
      }
    } else {
      fSelectedRegion = new Region(baseOffset + replacement.length(), 0);
    }
  }

  @Override
  public Point getSelection(IDocument document) {
    if (fSelectedRegion == null) {
      return new Point(getReplacementOffset(), 0);
    }

    return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
  }

  protected void buildLinkedModeModel(OptionalArgumentModel model, IDocument document,
      int baseOffset) throws BadLocationException {
    List<OptionalArgumentPosition> positions = new ArrayList<OptionalArgumentPosition>();
    boolean hasNamed = getProposal().hasNamedParameters();
    int positionalCount = getProposal().getPositionalParameterCount();
    for (int i = 0; i != fArgumentOffsets.length; i++) {
      LinkedPositionGroup group = new LinkedPositionGroup();
      OptionalArgumentPosition pos;
      pos = new OptionalArgumentPosition(
          document,
          baseOffset + fArgumentOffsets[i],
          fArgumentLengths[i],
          LinkedPositionGroup.NO_STOP);
      boolean isRequired = i < positionalCount;
      pos.setIsRequired(isRequired);
      if (!isRequired && hasNamed) {
        pos.resetNameStart();
      }
      positions.add(pos);
      group.addPosition(pos);
      model.setHasNamed(hasNamed);
      model.addGroup(group);
    }
    model.setPositions(positions, document);
  }

  @Override
  protected String computeReplacementString() {

    if (!hasParameters() || !hasArgumentList()) {
      return super.computeReplacementString();
    }

    StringBuffer buffer = new StringBuffer();
    if (fProposal.getKind() != CompletionProposal.ARGUMENT_LIST) {
      appendMethodNameReplacement(buffer);
    }

    char[][] parameterNames = fProposal.findParameterNames(null);
    int count = parameterNames.length;
    fArgumentOffsets = new int[count];
    fArgumentLengths = new int[count];

    FormatterPrefs prefs = getFormatterPrefs();

    setCursorPosition(buffer.length());

    if (prefs.afterOpeningParen) {
      buffer.append(SPACE);
    }

    boolean hasNamed = fProposal.hasNamedParameters();
    int positionalCount = fProposal.getPositionalParameterCount();
    for (int i = 0; i != count; i++) {
      if (i != 0) {
        if (prefs.beforeComma) {
          buffer.append(SPACE);
        }
        buffer.append(COMMA);
        if (prefs.afterComma) {
          buffer.append(SPACE);
        }
      }

      fArgumentOffsets[i] = buffer.length();
      buffer.append(parameterNames[i]);
      fArgumentLengths[i] = parameterNames[i].length;
      if (hasNamed && i >= positionalCount) {
        buffer.append(": ");
        fArgumentLengths[i] += 2;
      }
    }

    if (prefs.beforeClosingParen) {
      buffer.append(SPACE);
    }

    if (fProposal.getKind() != CompletionProposal.ARGUMENT_LIST) {
      buffer.append(RPAREN);
    }

    return buffer.toString();
  }

  @Override
  protected boolean needsLinkedMode() {
    return false; // we handle it ourselves
  }

  /**
   * Returns the currently active editor, or <code>null</code> if it cannot be determined.
   * 
   * @return the currently active editor, or <code>null</code>
   */
  private DartEditor getDartEditor() {
    IEditorPart part = DartToolsPlugin.getActivePage().getActiveEditor();
    if (part instanceof DartEditor) {
      return (DartEditor) part;
    } else {
      return null;
    }
  }

  /**
   * We call this hackish method from {@link ExitPolicy} to check if <code>','</code> is pressed at
   * place where it could be used as <code>TAB</code>, i.e. just before <code>','</code>.
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=15798
   */
  private void maybeRewriteCommaWithTab(IDocument document, VerifyEvent event, int offset) {
    if (event.character == ',') {
      try {
        if (document.get(offset, 1).equals(",")) {
          event.character = '\t';
        }
      } catch (Throwable e) {
      }
    }
  }

  private void openErrorDialog(BadLocationException e) {
    Shell shell = getTextViewer().getTextWidget().getShell();
    MessageDialog.openError(
        shell,
        DartTextMessages.FilledArgumentNamesMethodProposal_error_msg,
        e.getMessage());
  }

}
