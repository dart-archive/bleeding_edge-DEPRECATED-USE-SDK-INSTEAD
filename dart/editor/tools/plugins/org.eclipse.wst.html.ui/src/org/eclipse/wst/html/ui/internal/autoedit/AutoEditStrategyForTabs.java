/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM - Initial API and implementation Jens
 * Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/

package org.eclipse.wst.html.ui.internal.autoedit;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.core.internal.preferences.HTMLCorePreferenceNames;
import org.eclipse.wst.html.ui.internal.Logger;

/**
 * AutoEditStrategy to handle characters inserted when Tab key is pressed
 */
public class AutoEditStrategyForTabs implements IAutoEditStrategy {
  private final String TAB_CHARACTER = "\t"; //$NON-NLS-1$

  public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
    // if not in smart insert mode just ignore
    if (!isSmartInsertMode())
      return;

    // spaces for tab character
    if (command.length == 0 && command.text != null && command.text.length() > 0
        && command.text.indexOf(TAB_CHARACTER) != -1)
      smartInsertForTab(command, document);
  }

  /**
   * Insert spaces for tabs
   * 
   * @param command
   */
  private void smartInsertForTab(DocumentCommand command, IDocument document) {
    // tab key was pressed. now check preferences to see if need to insert
    // spaces instead of tab
    int indentationWidth = getIndentationWidth();
    if (indentationWidth > -1) {
      String originalText = command.text;
      StringBuffer newText = new StringBuffer(originalText);

      // determine where in line this command begins
      int lineOffset = -1;
      try {
        IRegion lineInfo = document.getLineInformationOfOffset(command.offset);
        lineOffset = command.offset - lineInfo.getOffset();
      } catch (BadLocationException e) {
        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
      }

      ILineTracker lineTracker = getLineTracker(document, originalText);

      int startIndex = 0;
      int index = newText.indexOf(TAB_CHARACTER);
      while (index != -1) {
        String indent = getIndentString(indentationWidth, lineOffset, lineTracker, index);

        // replace \t character with spaces
        newText.replace(index, index + 1, indent);
        if (lineTracker != null) {
          try {
            lineTracker.replace(index, 1, indent);
          } catch (BadLocationException e) {
            // if something goes wrong with replacing text, just
            // reset to current string
            lineTracker.set(newText.toString());
            Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
          }
        }

        startIndex = index + indent.length();
        index = newText.indexOf(TAB_CHARACTER, startIndex);
      }
      command.text = newText.toString();
    }
  }

  /**
   * Calculate number of spaces for next tab stop
   */
  private String getIndentString(int indentationWidth, int lineOffset, ILineTracker lineTracker,
      int index) {
    int indentSize = indentationWidth;
    int offsetInLine = -1;
    if (lineTracker != null) {
      try {
        IRegion lineInfo = lineTracker.getLineInformationOfOffset(index);
        if (lineInfo.getOffset() == 0 && lineOffset > -1)
          offsetInLine = lineOffset + index;
        else
          offsetInLine = index - lineInfo.getOffset();
      } catch (BadLocationException e) {
        Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
      }
    } else {
      if (lineOffset > -1) {
        offsetInLine = lineOffset + index;
      }
    }
    if (offsetInLine > -1 && indentationWidth > 0) {
      int remainder = offsetInLine % indentationWidth;
      indentSize = indentationWidth - remainder;
    }

    StringBuffer indent = new StringBuffer();
    for (int i = 0; i < indentSize; i++)
      indent.append(' ');
    return indent.toString();
  }

  /**
   * Set up a line tracker for text within command if text is multi-line
   */
  private ILineTracker getLineTracker(IDocument document, String originalText) {
    ConfigurableLineTracker lineTracker = null;
    int[] delims = TextUtilities.indexOf(document.getLegalLineDelimiters(), originalText, 0);
    if (delims[0] != -1 || delims[1] != -1) {
      lineTracker = new ConfigurableLineTracker(document.getLegalLineDelimiters());
      lineTracker.set(originalText);
    }
    return lineTracker;
  }

  /**
   * Return true if active editor is in smart insert mode, false otherwise
   * 
   * @return
   */
  private boolean isSmartInsertMode() {
    boolean isSmartInsertMode = false;

    ITextEditor textEditor = null;
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        IEditorPart editor = page.getActiveEditor();
        if (editor != null) {
          if (editor instanceof ITextEditor)
            textEditor = (ITextEditor) editor;
          else
            textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
        }
      }
    }

    // check if smart insert mode
    if (textEditor instanceof ITextEditorExtension3
        && ((ITextEditorExtension3) textEditor).getInsertMode() == ITextEditorExtension3.SMART_INSERT)
      isSmartInsertMode = true;
    return isSmartInsertMode;
  }

  /**
   * Returns indentation width if using spaces for indentation, -1 otherwise
   * 
   * @return
   */
  private int getIndentationWidth() {
    int width = -1;

    Preferences preferences = HTMLCorePlugin.getDefault().getPluginPreferences();
    if (HTMLCorePreferenceNames.SPACE.equals(preferences.getString(HTMLCorePreferenceNames.INDENTATION_CHAR)))
      width = preferences.getInt(HTMLCorePreferenceNames.INDENTATION_SIZE);

    return width;
  }
}
