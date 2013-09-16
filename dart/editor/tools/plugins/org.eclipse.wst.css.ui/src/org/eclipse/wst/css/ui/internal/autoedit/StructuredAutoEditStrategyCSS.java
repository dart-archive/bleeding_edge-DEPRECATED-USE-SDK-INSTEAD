/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.autoedit;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.wst.css.core.internal.CSSCorePlugin;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.preferences.CSSCorePreferenceNames;
import org.eclipse.wst.css.core.internal.util.RegionIterator;
import org.eclipse.wst.css.ui.internal.Logger;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;

public class StructuredAutoEditStrategyCSS implements IAutoEditStrategy {
  protected IStructuredDocument structuredDocument = null;

  class CompoundRegion {

    /* textRegion can be null if the offset is at the end of the document */
    CompoundRegion(IStructuredDocumentRegion documentRegion, ITextRegion textRegion) {
      super();
      this.documentRegion = documentRegion;
      this.textRegion = textRegion;
    }

    IStructuredDocumentRegion getDocumentRegion() {
      return documentRegion;
    }

    ITextRegion getTextRegion() {
      return textRegion;
    }

    int getStart() {
      return (textRegion != null) ? textRegion.getStart() : documentRegion.getStart();
    }

    int getEnd() {
      return (textRegion != null) ? textRegion.getEnd() : documentRegion.getEnd();
    }

    String getType() {
      return (textRegion != null) ? textRegion.getType() : CSSRegionContexts.CSS_UNKNOWN;
    }

    String getText() {
      return (textRegion != null) ? documentRegion.getText(textRegion) : ""; //$NON-NLS-1$
    }

    int getStartOffset() {
      return (textRegion != null) ? documentRegion.getStartOffset(textRegion)
          : documentRegion.getStartOffset();
    }

    int getEndOffset() {
      return (textRegion != null) ? documentRegion.getEndOffset(textRegion)
          : documentRegion.getEndOffset();
    }

    private IStructuredDocumentRegion documentRegion;
    private ITextRegion textRegion;

  }

  /**
	 */
  protected void autoIndentAfterClose(DocumentCommand command, String regionType) {
    if (!setRangeForClose(command))
      return;

    int position = command.offset + command.length;

    if (position == -1 || structuredDocument.getLength() == 0) {
      return;
    }

    // get open brace region
    CompoundRegion region = prevCorrespondence(position, regionType);

    // get indentation
    String str = getIndentFor(region, false);

    // append to input
    if (str != null)
      command.text = str + command.text;
  }

  /**
   * Copies the indentation of the previous line.
   */
  protected void autoIndentAfterNewLine(DocumentCommand command) {
    // select nearest white spaces to replace with new-line
    setRangeForNewLine(command);

    // get position
    int position = command.offset;

    if (position == -1 || structuredDocument.getLength() == 0) {
      return;
    }

    IStructuredDocumentRegion prev = getPreviousRegion(position);
    if (prev == null || !CSSRegionContexts.CSS_LBRACE.equals(prev.getType()))
      return;

    IStructuredDocumentRegion next = prev.getNext();

    // create text to replace
    StringBuffer buf = new StringBuffer(command.text);
    try {
      IRegion line = structuredDocument.getLineInformationOfOffset(position);
      int contentStart = findEndOfWhiteSpace(structuredDocument, position,
          line.getOffset() + line.getLength());
      command.length = Math.max(contentStart - position, 0);
      buf.append(getIndentString());
      if (next != null && CSSRegionContexts.CSS_RBRACE.equals(next.getType()) && !isOneLine(prev)) {
        command.shiftsCaret = false;
        command.caretOffset = contentStart + buf.length();
        buf.append(TextUtilities.getDefaultLineDelimiter(structuredDocument));
      }
      command.text = buf.toString();
    } catch (BadLocationException e) {
    }

  }

  private boolean isOneLine(IStructuredDocumentRegion prev) {
    return endsWith(structuredDocument.getLegalLineDelimiters(), prev.getFullText()) != -1;
  }

  private IStructuredDocumentRegion getPreviousRegion(int offset) {
    IStructuredDocumentRegion prev = null;
    if (offset > 0)
      prev = structuredDocument.getRegionAtCharacterOffset(offset - 1);
    return prev;
  }

  /**
	 */
  public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
    Object textEditor = getActiveTextEditor();
    if (!(textEditor instanceof ITextEditorExtension3 && ((ITextEditorExtension3) textEditor).getInsertMode() == ITextEditorExtension3.SMART_INSERT))
      return;

    // return;
    // /*
    structuredDocument = (IStructuredDocument) document;

    if (command.length == 0 && command.text != null) {
      if (endsWith(document.getLegalLineDelimiters(), command.text) != -1) {
        autoIndentAfterNewLine(command);
      } else if (command.text.equals("}")) {//$NON-NLS-1$
        autoIndentAfterClose(command, CSSRegionContexts.CSS_RBRACE);
      } else if (command.text.equals("]")) {//$NON-NLS-1$
        autoIndentAfterClose(command, CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END);
      } else if (command.text.equals(")")) {//$NON-NLS-1$
        autoIndentAfterClose(command, CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE);
      }
    }
    // */

    // spaces for tab character
    if (command.text != null && command.text.length() > 0 && command.text.charAt(0) == '\t')
      smartInsertForTab(command, document);
  }

  /**
	 */
  protected String getIndentFor(CompoundRegion region, boolean indentForNextRegion) {
    if (region == null)
      return null;
    IStructuredDocumentRegion flatNode = region.getDocumentRegion();
    if (flatNode == null)
      return null;

    try {
      if (region.getType() == CSSRegionContexts.CSS_LBRACE
          || region.getType() == CSSRegionContexts.CSS_DELIMITER
          || region.getType() == CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
        // get meanful flat node
        RegionIterator it = new RegionIterator(flatNode, region.getTextRegion());
        it.prev();
        while (it.hasPrev()) {
          ITextRegion r = it.prev();
          region = new CompoundRegion(it.getStructuredDocumentRegion(), r);
          if (region.getType() != CSSRegionContexts.CSS_S)
            break;
        }
        flatNode = region.getDocumentRegion();
        // get indent string
        int position = flatNode.getStart();
        int line = structuredDocument.getLineOfOffset(position);
        int start = structuredDocument.getLineOffset(line);
        int end = findEndOfWhiteSpace(structuredDocument, start, position);
        return structuredDocument.get(start, end - start);
      } else if (region.getType() == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_START
          ||
          // region.getType() == CSSRegionContexts.CSS_PARENTHESIS_OPEN ||
          region.getType() == CSSRegionContexts.CSS_DECLARATION_VALUE_FUNCTION
          || region.getType() == CSSRegionContexts.CSS_DECLARATION_SEPARATOR) {
        int position = flatNode.getStart() + region.getStart();
        int line = structuredDocument.getLineOfOffset(position);
        int start = structuredDocument.getLineOffset(line);
        int end = findEndOfWhiteSpace(structuredDocument, start, position);
        StringBuffer buf = new StringBuffer(structuredDocument.get(start, end - start));
        position += region.getText().length();
        if (indentForNextRegion) {
          int tokenStart = findEndOfWhiteSpace(structuredDocument, position,
              structuredDocument.getLineOffset(line) + structuredDocument.getLineLength(line) - 1);
          if (tokenStart < structuredDocument.getLineOffset(line)
              + structuredDocument.getLineLength(line) - 1) {
            position = tokenStart;
          }
        }
        while (position - end > 0) {
          buf.append(" ");//$NON-NLS-1$
          end++;
        }
        return buf.toString();
      } else
        return "";//$NON-NLS-1$
    } catch (BadLocationException excp) {
      Logger.logException(excp);
    }
    return null;
  }

  /**
	 */
  protected CompoundRegion getPrevKeyRegion(int position, CompoundRegion currentRegion) {
    if (currentRegion == null) {
      if (structuredDocument.getLastStructuredDocumentRegion() == null)
        return null;
    }

    if (currentRegion != null
        && (currentRegion.getType() == CSSRegionContexts.CSS_RBRACE
            || currentRegion.getType() == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END || currentRegion.getType() == CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE)) {
      return prevCorrespondence(currentRegion);
    }

    RegionIterator it = new RegionIterator(structuredDocument, position - 1);
    while (it.hasPrev()) {
      ITextRegion r = it.prev();
      CompoundRegion region = new CompoundRegion(it.getStructuredDocumentRegion(), r);
      if (region.getType() == CSSRegionContexts.CSS_LBRACE
          ||
          // region.getType() == CSSRegionContexts.CSS_RBRACE ||
          region.getType() == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_START
          ||
          // region.getType() ==
          // CSSRegionContexts.CSS_BRACKET_CLOSE ||
          // // region.getType() ==
          // CSSRegionContexts.CSS_PARENTHESIS_OPEN ||
          // region.getType() ==
          // CSSRegionContexts.CSS_PARENTHESIS_CLOSE ||
          region.getType() == CSSRegionContexts.CSS_DELIMITER
          || region.getType() == CSSRegionContexts.CSS_DECLARATION_DELIMITER ||
          // region.getType() == CSSRegionContexts.CSS_COLON ||
          // region.getType() == CSSRegionContexts.CSS_COMMENT
          // ||
          region.getType() == CSSRegionContexts.CSS_DECLARATION_VALUE_FUNCTION) {
        return region;
      } else if (region.getType() == CSSRegionContexts.CSS_RBRACE
          || region.getType() == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END
          || region.getType() == CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE) {
        // skip to LBRACE
        CompoundRegion pc = prevCorrespondence(region);
        // guard for NPE
        //https://bugs.eclipse.org/bugs/show_bug.cgi?id=111318
        if (pc == null)
          break;
        it.reset(pc.getDocumentRegion(), pc.getTextRegion());
        it.prev();
      } else if (region.getType() == CSSRegionContexts.CSS_STRING) {
        RegionIterator itTmp = new RegionIterator(structuredDocument, position);
        if (region == itTmp.prev())
          return region; // position is inside of string
      } else if (region.getType() == CSSRegionContexts.CSS_COMMENT) {
        RegionIterator itTmp = new RegionIterator(structuredDocument, position);
        if (region == itTmp.prev())
          return region; // position is inside of comment
      } else if (region.getType() == CSSRegionContexts.CSS_UNKNOWN) {
        String str = region.getText();
        if (str.charAt(str.length() - 1) == '\\')
          return region;
      } else if (region.getType() == CSSRegionContexts.CSS_DECLARATION_SEPARATOR) {
        RegionIterator itPrev = new RegionIterator(region.getDocumentRegion(),
            region.getTextRegion());
        while (itPrev.hasPrev()) {
          ITextRegion regionPrev = itPrev.prev();
          if (regionPrev.getType() == CSSRegionContexts.CSS_RBRACE) {
            break;
          } else if (regionPrev.getType() == CSSRegionContexts.CSS_DELIMITER
              || regionPrev.getType() == CSSRegionContexts.CSS_DECLARATION_DELIMITER) {
            return region;
          } else if (regionPrev.getType() == CSSRegionContexts.CSS_LBRACE) {
            while (itPrev.hasPrev()) {
              regionPrev = itPrev.prev();
              if (regionPrev.getType() == CSSRegionContexts.CSS_MEDIA)
                break;
              if (regionPrev.getType() == CSSRegionContexts.CSS_LBRACE
                  || regionPrev.getType() == CSSRegionContexts.CSS_RBRACE
                  || regionPrev.getType() == CSSRegionContexts.CSS_DELIMITER
                  || regionPrev.getType() == CSSRegionContexts.CSS_DECLARATION_DELIMITER)
                return region;
            }
            if (regionPrev.getType() == CSSRegionContexts.CSS_MEDIA)
              break;
            else
              return region;
          }
        }
      }
    }
    return null;
  }

  /**
	 */
  protected CompoundRegion getRegion(int position) {
    IStructuredDocumentRegion flatNode = structuredDocument.getRegionAtCharacterOffset(position);
    if (flatNode != null)
      return new CompoundRegion(flatNode, flatNode.getRegionAtCharacterOffset(position));
    return null;
  }

  /**
	 */
  protected int needShift(CompoundRegion region, int position) {
    int shift = 0;
    if (region == null || region.getType() == CSSRegionContexts.CSS_DELIMITER
        || region.getType() == CSSRegionContexts.CSS_DECLARATION_DELIMITER
        || region.getType() == CSSRegionContexts.CSS_LBRACE) {
      // get non space region
      CompoundRegion cr = getRegion(position - 1);
      RegionIterator it = new RegionIterator(cr.getDocumentRegion(), cr.getTextRegion());
      ITextRegion nearestRegion = null;
      while (it.hasPrev()) {
        nearestRegion = it.prev();
        if (nearestRegion.getType() != CSSRegionContexts.CSS_S
            && nearestRegion.getType() != CSSRegionContexts.CSS_COMMENT)
          break;
      }
      if (nearestRegion != null
          && (nearestRegion.getType() == CSSRegionContexts.CSS_LBRACE
              || nearestRegion.getType() == CSSRegionContexts.CSS_RBRACE
              || nearestRegion.getType() == CSSRegionContexts.CSS_DELIMITER || nearestRegion.getType() == CSSRegionContexts.CSS_DECLARATION_DELIMITER))
        shift--;
      else if (region == null)
        shift--;
      shift++;
    }
    if (region != null && region.getType() == CSSRegionContexts.CSS_LBRACE) {
      RegionIterator it = new RegionIterator(structuredDocument, position);
      if (!it.hasPrev() || it.prev().getType() != CSSRegionContexts.CSS_RBRACE)
        shift++;
      else
        shift = 0;
    }
    return shift;
  }

  /**
	 */
  protected CompoundRegion prevCorrespondence(int position, String regionType) {
    RegionIterator it = new RegionIterator(structuredDocument, position - 1);
    ITextRegion region = null;
    int nest = 1;
    if (regionType == CSSRegionContexts.CSS_RBRACE) {
      // skip to LBRACE
      while (it.hasPrev()) {
        region = it.prev();
        if (region.getType() == CSSRegionContexts.CSS_LBRACE)
          nest--;
        else if (region.getType() == CSSRegionContexts.CSS_RBRACE)
          nest++;
        if (nest <= 0)
          break;
      }
      if (nest == 0)
        return new CompoundRegion(it.getStructuredDocumentRegion(), region);
    }
    if (regionType == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END) {
      // skip to BRACKET_OPEN
      while (it.hasPrev()) {
        region = it.prev();
        if (region.getType() == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_START)
          nest--;
        else if (region.getType() == CSSRegionContexts.CSS_SELECTOR_ATTRIBUTE_END)
          nest++;
        if (nest <= 0)
          break;
      }
      if (nest == 0)
        return new CompoundRegion(it.getStructuredDocumentRegion(), region);
    }
    if (regionType == CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE) {
      // skip to PARENTHESIS_OPEN
      while (it.hasPrev()) {
        region = it.prev();
        if (// region.getType() ==
        // CSSRegionContexts.CSS_PARENTHESIS_OPEN ||
        region.getType() == CSSRegionContexts.CSS_DECLARATION_VALUE_FUNCTION)
          nest--;
        else if (region.getType() == CSSRegionContexts.CSS_DECLARATION_VALUE_PARENTHESIS_CLOSE)
          nest++;
        if (nest <= 0)
          break;
      }
      if (nest == 0)
        return new CompoundRegion(it.getStructuredDocumentRegion(), region);
    }
    return null;
  }

  /**
	 */
  protected CompoundRegion prevCorrespondence(CompoundRegion region) {
    if (region == null)
      return null;

    IStructuredDocumentRegion flatNode = region.getDocumentRegion();
    int position = flatNode.getStart() + region.getStart();
    return prevCorrespondence(position, region.getType());
  }

  /**
   * Insert the method's description here.
   * 
   * @return boolean
   * @param command org.eclipse.jface.text.DocumentCommand
   */
  protected boolean setRangeForClose(DocumentCommand command) {
    int position = command.offset;

    if (position == -1 || structuredDocument.getLength() == 0) {
      return false;
    }

    try {
      // find start of line
      int p = (position == structuredDocument.getLength() ? position - 1 : position);

      int line = structuredDocument.getLineOfOffset(p);
      int start = structuredDocument.getLineOffset(line);
      RegionIterator it = new RegionIterator(structuredDocument, start);
      boolean allWhiteSpace = false;
      // check whether the text from lStart to position is white space
      // or not
      while (it.hasNext()) {
        ITextRegion region = it.next();
        if (region.getType() != CSSRegionContexts.CSS_S)
          break;
        if (it.getStructuredDocumentRegion().getEndOffset(region) > p) {
          allWhiteSpace = true;
          break;
        }
      }
      if (allWhiteSpace) {
        command.length = command.length - (start - command.offset);
        command.offset = start;
        return true;
      }
    } catch (BadLocationException excp) {
      Logger.logException(excp);
    }
    return false;
  }

  /**
	 */
  protected void setRangeForNewLine(DocumentCommand command) {
    int position = command.offset;

    if (position == -1 || structuredDocument.getLength() == 0) {
      return;
    }

    try {
      // add pre-nearest white spaces to replace target
      if (position > 0) {
        IStructuredDocumentRegion flatNode = structuredDocument.getRegionAtCharacterOffset(position - 1);
        if (flatNode != null) {
          ITextRegion region = flatNode.getRegionAtCharacterOffset(position - 1);
          if (region.getType() == CSSRegionContexts.CSS_S) {
            int end = command.offset + command.length;
            int nLine = structuredDocument.getLineOfOffset(position);
            int nStartPos = structuredDocument.getLineOffset(nLine);
            if (nStartPos < flatNode.getStartOffset(region))
              nStartPos = flatNode.getStartOffset(region);
            command.offset = nStartPos;
            command.length = end - command.offset;
          }
        }
      }

      // add post-nearest white spaces to replace target
      if (position < structuredDocument.getLength()) {
        IStructuredDocumentRegion flatNode = structuredDocument.getRegionAtCharacterOffset(position);
        if (flatNode != null) {
          ITextRegion region = flatNode.getRegionAtCharacterOffset(position);
          if (region.getType() == CSSRegionContexts.CSS_S) {
            int nLine = structuredDocument.getLineOfOffset(position);
            String currentLineDelim = structuredDocument.getLineDelimiter(nLine);
            int nEndPos = structuredDocument.getLineOffset(nLine)
                + structuredDocument.getLineLength(nLine)
                - ((currentLineDelim != null) ? currentLineDelim.length() : 0);
            if (nEndPos > flatNode.getEndOffset(region))
              nEndPos = flatNode.getEndOffset(region);
            command.length = nEndPos - command.offset;
          }
        }
      }
    } catch (BadLocationException e) {
      // do not customize command
    }

  }

  private static int endsWith(String[] searchStrings, String text) {
    for (int i = 0; i < searchStrings.length; i++) {
      if (text.endsWith(searchStrings[i]))
        return i;
    }
    return -1;
  }

  private static int findEndOfWhiteSpace(IDocument document, int offset, int end)
      throws BadLocationException {
    while (offset < end) {
      char c = document.getChar(offset);
      if (c != ' ' && c != '\t') {
        return offset;
      }
      offset++;
    }
    return end;
  }

  private String getIndentString() {
    StringBuffer indent = new StringBuffer();

    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    if (preferences != null) {
      char indentChar = ' ';
      String indentCharPref = preferences.getString(CSSCorePreferenceNames.INDENTATION_CHAR);
      if (CSSCorePreferenceNames.TAB.equals(indentCharPref)) {
        indentChar = '\t';
      }
      int indentationWidth = preferences.getInt(CSSCorePreferenceNames.INDENTATION_SIZE);

      for (int i = 0; i < indentationWidth; i++) {
        indent.append(indentChar);
      }
    }
    return indent.toString();
  }

  /**
   * Return the active text editor if possible, otherwise the active editor part.
   * 
   * @return
   */
  private Object getActiveTextEditor() {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      IWorkbenchPage page = window.getActivePage();
      if (page != null) {
        IEditorPart editor = page.getActiveEditor();
        if (editor != null) {
          if (editor instanceof ITextEditor)
            return editor;
          ITextEditor textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
          if (textEditor != null)
            return textEditor;
          return editor;
        }
      }
    }
    return null;
  }

  /**
   * Insert spaces for tabs
   * 
   * @param command
   */
  private void smartInsertForTab(DocumentCommand command, IDocument document) {
    // tab key was pressed. now check preferences to see if need to insert
    // spaces instead of tab
    Preferences preferences = CSSCorePlugin.getDefault().getPluginPreferences();
    if (CSSCorePreferenceNames.SPACE.equals(preferences.getString(CSSCorePreferenceNames.INDENTATION_CHAR))) {
      int indentationWidth = preferences.getInt(CSSCorePreferenceNames.INDENTATION_SIZE);

      StringBuffer indent = new StringBuffer();
      if (indentationWidth != 0) {
        int indentSize = indentationWidth;
        try {
          IRegion firstLine = document.getLineInformationOfOffset(command.offset);
          int offsetInLine = command.offset - firstLine.getOffset();
          int remainder = offsetInLine % indentationWidth;

          indentSize = indentationWidth - remainder;
        } catch (BadLocationException e) {
          Logger.log(Logger.WARNING_DEBUG, e.getMessage(), e);
        }

        for (int i = 0; i < indentSize; i++)
          indent.append(' ');
      }

      // replace \t characters with spaces
      command.text = indent.toString();
    }
  }
}
