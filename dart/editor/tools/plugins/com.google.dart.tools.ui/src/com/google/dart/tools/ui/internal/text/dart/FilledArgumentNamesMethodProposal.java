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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

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
  public void apply(IDocument document, char trigger, int offset) {
    super.apply(document, trigger, offset);
    int baseOffset = getReplacementOffset();
    String replacement = getReplacementString();

    if (fArgumentOffsets != null && getTextViewer() != null) {
      try {
        LinkedModeModel model = new LinkedModeModel();
        for (int i = 0; i != fArgumentOffsets.length; i++) {
          LinkedPositionGroup group = new LinkedPositionGroup();
          group.addPosition(new LinkedPosition(document, baseOffset + fArgumentOffsets[i],
              fArgumentLengths[i], LinkedPositionGroup.NO_STOP));
          model.addGroup(group);
        }

        model.forceInstall();
        DartEditor editor = getDartEditor();
        if (editor != null) {
          model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
        }

        LinkedModeUI ui = new EditorLinkedModeUI(model, getTextViewer());
        ui.setExitPosition(getTextViewer(), baseOffset + replacement.length(), 0, Integer.MAX_VALUE);
        ui.setExitPolicy(new ExitPolicy(')', document));
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

  @Override
  protected String computeReplacementString() {

    if (!hasParameters() || !hasArgumentList()) {
      return super.computeReplacementString();
    }

    StringBuffer buffer = new StringBuffer();
    appendMethodNameReplacement(buffer);

    char[][] parameterNames = fProposal.findParameterNames(null);
    int count = parameterNames.length;
    fArgumentOffsets = new int[count];
    fArgumentLengths = new int[count];

    FormatterPrefs prefs = getFormatterPrefs();

    setCursorPosition(buffer.length());

    if (prefs.afterOpeningParen) {
      buffer.append(SPACE);
    }

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
    }

    if (prefs.beforeClosingParen) {
      buffer.append(SPACE);
    }

    buffer.append(RPAREN);

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

  private void openErrorDialog(BadLocationException e) {
    Shell shell = getTextViewer().getTextWidget().getShell();
    MessageDialog.openError(shell, DartTextMessages.FilledArgumentNamesMethodProposal_error_msg,
        e.getMessage());
  }

}
