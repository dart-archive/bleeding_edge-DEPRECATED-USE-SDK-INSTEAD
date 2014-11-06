/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.functions.DartHeuristicScanner;
import com.google.dart.tools.ui.internal.text.functions.DartIndenter;
import com.google.dart.tools.ui.internal.text.functions.FastDartPartitionScanner;
import com.google.dart.tools.ui.internal.text.functions.Symbols;
import com.google.dart.tools.ui.internal.util.CodeFormatterUtil;
import com.google.dart.tools.ui.text.DartPartitions;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

/**
 * Auto indent strategy sensitive to brackets.
 * 
 * @coverage dart.editor.ui.text.dart
 */
public class DartAutoIndentStrategy_NEW extends DefaultIndentLineAutoEditStrategy {
  /** The line comment introducer. Value is "{@value} " */
  private static final String LINE_COMMENT = "//"; //$NON-NLS-1$

  /**
   * Returns the indentation of the line <code>line</code> in <code>document</code>. The returned
   * string may contain pairs of leading slashes that are considered part of the indentation. The
   * space before the asterisk in a Dart doc comment is not considered part of the indentation.
   * 
   * @param document the document
   * @param line the line
   * @return the indentation of <code>line</code> in <code>document</code>
   * @throws BadLocationException if the document is changed concurrently
   */
  private static String getCurrentIndent(Document document, int line) throws BadLocationException {
    IRegion region = document.getLineInformation(line);
    int from = region.getOffset();
    int endOffset = region.getOffset() + region.getLength();

    // go behind line comments
    int to = from;
    while (to < endOffset - 2 && document.get(to, 2).equals(LINE_COMMENT)) {
      to += 2;
    }

    while (to < endOffset) {
      char ch = document.getChar(to);
      if (!Character.isWhitespace(ch)) {
        break;
      }
      to++;
    }

    // don't count the space before Dart doc, asterisk-style comment lines
    if (to > from && to < endOffset - 1 && document.get(to - 1, 2).equals(" *")) { //$NON-NLS-1$
      String type = TextUtilities.getContentType(
          document,
          DartPartitions.DART_PARTITIONING,
          to,
          true);
      if (type.equals(DartPartitions.DART_DOC)
          || type.equals(DartPartitions.DART_MULTI_LINE_COMMENT)) {
        to--;
      }
    }

    return document.get(from, to - from);
  }

  private static IPreferenceStore getPreferenceStore() {
    return DartToolsPlugin.getDefault().getCombinedPreferenceStore();
  }

  private static String getWhitespaceRight(IDocument document, int index)
      throws BadLocationException {
    StringBuffer result = new StringBuffer();
    while (true) {
      char c = document.getChar(index++);
      if (!Character.isWhitespace(c)) {
        break;
      }
      result.append(c);
    }
    return result.toString();
  }

  /**
   * Installs a Dart partitioner with <code>document</code>.
   * 
   * @param document the document
   */
  private static void installDartStuff(Document document) {
    String[] types = new String[] {
        DartPartitions.DART_DOC, DartPartitions.DART_MULTI_LINE_COMMENT,
        DartPartitions.DART_SINGLE_LINE_COMMENT, DartPartitions.DART_SINGLE_LINE_DOC,
        DartPartitions.DART_STRING, DartPartitions.DART_MULTI_LINE_STRING,
        IDocument.DEFAULT_CONTENT_TYPE};
    FastPartitioner partitioner = new FastPartitioner(new FastDartPartitionScanner(), types);
    partitioner.connect(document);
    document.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, partitioner);
  }

  /**
   * Installs a Dart partitioner with <code>document</code>.
   * 
   * @param document the document
   */
  private static void removeDartStuff(Document document) {
    document.setDocumentPartitioner(DartPartitions.DART_PARTITIONING, null);
  }

  /**
   * Skips the scope opened by <code>token</code>.
   * 
   * @param scanner the scanner
   * @param start the start position
   * @param token the token
   * @return the position after the scope or <code>DartHeuristicScanner.NOT_FOUND</code>
   */
  private static int skipScope(DartHeuristicScanner scanner, int start, int token) {
    int openToken = token;
    int closeToken;
    switch (token) {
      case Symbols.TokenLPAREN:
        closeToken = Symbols.TokenRPAREN;
        break;
      case Symbols.TokenLBRACKET:
        closeToken = Symbols.TokenRBRACKET;
        break;
      case Symbols.TokenLBRACE:
        closeToken = Symbols.TokenRBRACE;
        break;
      default:
        Assert.isTrue(false);
        return -1; // dummy
    }

    int depth = 1;
    int p = start;

    while (true) {
      int tok = scanner.nextToken(p, DartHeuristicScanner.UNBOUND);
      p = scanner.getPosition();

      if (tok == openToken) {
        depth++;
      } else if (tok == closeToken) {
        depth--;
        if (depth == 0) {
          return p + 1;
        }
      } else if (tok == Symbols.TokenEOF) {
        return DartHeuristicScanner.NOT_FOUND;
      }
    }
  }

  private boolean fCloseBrace;
  private boolean fIsSmartMode;

  private boolean fIsSmartTab;

  private String fPartitioning;

  /**
   * The viewer.
   */
  private final ISourceViewer fViewer;

  /**
   * Creates a new Dart auto indent strategy for the given document partitioning.
   * 
   * @param partitioning the document partitioning // * @param project the project to get formatting
   *          preferences from, or null to use default // * preferences
   * @param viewer the source viewer that this strategy is attached to
   */
  public DartAutoIndentStrategy_NEW(String partitioning, ISourceViewer viewer) {
    fPartitioning = partitioning;
    fViewer = viewer;
  }

  @Override
  public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
    try {
      if (c.doit == false) {
        return;
      }

      clearCachedValues();

      if (!fIsSmartMode) {
        super.customizeDocumentCommand(d, c);
        return;
      }

      if (!fIsSmartTab && isRepresentingTab(c.text)) {
        return;
      }

      if (c.length == 0 && c.text != null && isLineDelimiter(d, c.text)) {
        smartIndentAfterNewLine(d, c);
      } else if (c.text.length() == 1) {
        smartIndentOnKeypress(d, c);
      } else if (c.text.length() > 1
          && getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_PASTE)) {
        if (fViewer == null || fViewer.getTextWidget() == null
            || !fViewer.getTextWidget().getBlockSelection()) {
          smartPaste(d, c); // no smart backspace for paste
        }
      }
    } catch (IllegalArgumentException e) {
      // ignore
    }

  }

  protected boolean computeSmartMode() {
    IWorkbenchPage page = DartToolsPlugin.getActivePage();
    if (page != null) {
      IEditorPart part = page.getActiveEditor();
      if (part instanceof ITextEditorExtension3) {
        ITextEditorExtension3 extension = (ITextEditorExtension3) part;
        return extension.getInsertMode() == ITextEditorExtension3.SMART_INSERT;
      }
    }
    return false;
  }

  /**
   * Indents line <code>line</code> in <code>document</code> with <code>indent</code>. Leaves
   * leading comment signs alone.
   * 
   * @param document the document
   * @param line the line
   * @param indent the indentation to insert
   * @param tabLength the length of a tab
   * @throws BadLocationException on concurrent document modification
   */
  private void addIndent(Document document, int line, CharSequence indent, int tabLength)
      throws BadLocationException {
    IRegion region = document.getLineInformation(line);
    int insert = region.getOffset();
    int endOffset = region.getOffset() + region.getLength();

    // Compute insert after all leading line comment markers
    int newInsert = insert;
    while (newInsert < endOffset - 2 && document.get(newInsert, 2).equals(LINE_COMMENT)) {
      newInsert += 2;
    }

    // Heuristic to check whether it is commented code or just a comment
    if (newInsert > insert) {
      int whitespaceCount = 0;
      int i = newInsert;
      while (i < endOffset - 1) {
        char ch = document.get(i, 1).charAt(0);
        if (!Character.isWhitespace(ch)) {
          break;
        }
        whitespaceCount = whitespaceCount + computeVisualLength(ch, tabLength);
        i++;
      }

      if (whitespaceCount != 0 && whitespaceCount >= CodeFormatterUtil.getIndentWidth(null)) {
        insert = newInsert;
      }
    }

    // Insert indent
    document.replace(insert, 0, indent.toString());
  }

  private void clearCachedValues() {
    IPreferenceStore preferenceStore = getPreferenceStore();
    fCloseBrace = preferenceStore.getBoolean(PreferenceConstants.EDITOR_CLOSE_BRACES);
    fIsSmartTab = preferenceStore.getBoolean(PreferenceConstants.EDITOR_SMART_TAB);
    fIsSmartMode = computeSmartMode();
  }

  private boolean closeBrace() {
    return fCloseBrace;
  }

  private String computeForcedCascadePrefix(DartIndenter indenter, IDocument document, int offset,
      String newText) throws BadLocationException {
    int currentLine = document.getLineOfOffset(offset);
    if (currentLine > 0) {
      IRegion prevLineRegion = document.getLineInformation(currentLine - 1);
      String prevLine = document.get(prevLineRegion.getOffset(), prevLineRegion.getLength());
      String prevLine2 = prevLine.trim();
      String newText2 = newText.trim();
      if (newText2.startsWith("..")) {
        String prevIndent = getWhitespaceRight(document, prevLineRegion.getOffset());
        if (prevLine2.startsWith("..")) {
          return prevIndent;
        }
        return prevIndent + indenter.getCascadeIndent();
      }
    }
    // don't force
    return null;
  }

  /**
   * Returns the visual length of a given character taking into account the visual tabulator length.
   * 
   * @param ch the character to measure
   * @param tabLength the length of a tab
   * @return the visual length of <code>ch</code>
   */
  private int computeVisualLength(char ch, int tabLength) {
    if (ch == '\t') {
      return tabLength;
    } else {
      return 1;
    }
  }

  /**
   * Returns the visual length of a given <code>CharSequence</code> taking into account the visual
   * tabulator length.
   * 
   * @param seq the string to measure
   * @param tabLength the length of a tab
   * @return the visual length of <code>seq</code>
   */
  private int computeVisualLength(CharSequence seq, int tabLength) {

    int size = 0;

    if (seq != null) {
      for (int i = 0; i < seq.length(); i++) {
        char ch = seq.charAt(i);
        if (ch == '\t') {
          if (tabLength != 0) {
            size += tabLength - size % tabLength;
            // else: size stays the same
          }
        } else {
          size++;
        }
      }
    }

    return size;
  }

  /**
   * Cuts the visual equivalent of <code>toDelete</code> characters out of the indentation of line
   * <code>line</code> in <code>document</code>. Leaves leading comment signs alone.
   * 
   * @param document the document
   * @param line the line
   * @param toDelete the number of space equivalents to delete
   * @param tabLength the length of a tab
   * @throws BadLocationException on concurrent document modification
   */
  private void cutIndent(Document document, int line, int toDelete, int tabLength)
      throws BadLocationException {
    IRegion region = document.getLineInformation(line);
    int from = region.getOffset();
    int endOffset = region.getOffset() + region.getLength();

    // go behind line comments
    while (from < endOffset - 2 && document.get(from, 2).equals(LINE_COMMENT)) {
      from += 2;
    }

    int to = from;
    while (toDelete > 0 && to < endOffset) {
      char ch = document.getChar(to);
      if (!Character.isWhitespace(ch)) {
        break;
      }
      toDelete -= computeVisualLength(ch, tabLength);
      if (toDelete >= 0) {
        to++;
      } else {
        break;
      }
    }

    document.replace(from, to - from, ""); //$NON-NLS-1$
  }

  private int getBracketCount(IDocument d, int startOffset, int endOffset,
      boolean ignoreCloseBrackets) throws BadLocationException {
    int bracketCount = 0;
    while (startOffset < endOffset) {
      char curr = d.getChar(startOffset);
      startOffset++;
      switch (curr) {
        case '/':
          if (startOffset < endOffset) {
            char next = d.getChar(startOffset);
            if (next == '*') {
              // a comment starts, advance to the comment end
              startOffset = getCommentEndBlock(d, startOffset + 1, endOffset);
            } else if (next == '/') {
              // '//'-comment: advance to the EOL comment end
              startOffset = getCommentEndEOL(d, startOffset + 1, endOffset);
            }
          }
          break;
        case '*':
          if (startOffset < endOffset) {
            char next = d.getChar(startOffset);
            if (next == '/') {
              // we have been in a comment: forget what we read before
              bracketCount = 0;
              startOffset++;
            }
          }
          break;
        case '{':
          bracketCount++;
          ignoreCloseBrackets = false;
          break;
        case '}':
          if (!ignoreCloseBrackets) {
            bracketCount--;
          }
          break;
        case '"':
        case '\'':
          startOffset = getStringEnd(d, startOffset, endOffset, curr);
          break;
        default:
      }
    }
    return bracketCount;
  }

  private int getCommentEndBlock(IDocument d, int offset, int endOffset)
      throws BadLocationException {
    while (offset < endOffset) {
      char curr = d.getChar(offset);
      offset++;
      if (curr == '*') {
        if (offset < endOffset && d.getChar(offset) == '/') {
          return offset + 1;
        }
      }
    }
    return endOffset;
  }

  private int getCommentEndEOL(IDocument d, int offset, int endOffset) throws BadLocationException {
    while (offset < endOffset) {
      char curr = d.getChar(offset);
      offset++;
      if (curr == '\n') {
        return offset + 1;
      }
    }
    return endOffset;
  }

  private String getIndentOfLine(IDocument d, int line) throws BadLocationException {
    if (line > -1) {
      int start = d.getLineOffset(line);
      int end = start + d.getLineLength(line) - 1;
      int whiteEnd = findEndOfWhiteSpace(d, start, end);
      return d.get(start, whiteEnd - start);
    } else {
      return ""; //$NON-NLS-1$
    }
  }

  private int getPeerPosition(IDocument document, DocumentCommand command) {
    if (document.getLength() == 0) {
      return 0;
    }
    /*
     * Search for scope closers in the pasted text and find their opening peers in the document.
     */
    Document pasted = new Document(command.text);
    installDartStuff(pasted);
    int firstPeer = command.offset;

    DartHeuristicScanner pScanner = new DartHeuristicScanner(pasted);
    DartHeuristicScanner dScanner = new DartHeuristicScanner(document);

    // add scope relevant after context to peer search
    int afterToken = dScanner.nextToken(
        command.offset + command.length,
        DartHeuristicScanner.UNBOUND);
    try {
      switch (afterToken) {
        case Symbols.TokenRBRACE:
          pasted.replace(pasted.getLength(), 0, "}"); //$NON-NLS-1$
          break;
        case Symbols.TokenRPAREN:
          pasted.replace(pasted.getLength(), 0, ")"); //$NON-NLS-1$
          break;
        case Symbols.TokenRBRACKET:
          pasted.replace(pasted.getLength(), 0, "]"); //$NON-NLS-1$
          break;
      }
    } catch (BadLocationException e) {
      // cannot happen
      Assert.isTrue(false);
    }

    int pPos = 0; // paste text position (increasing from 0)
    int dPos = Math.max(0, command.offset - 1); // document position (decreasing
                                                // from paste offset)
    while (true) {
      int token = pScanner.nextToken(pPos, DartHeuristicScanner.UNBOUND);
      pPos = pScanner.getPosition();
      switch (token) {
        case Symbols.TokenLBRACE:
        case Symbols.TokenLBRACKET:
        case Symbols.TokenLPAREN:
          pPos = skipScope(pScanner, pPos, token);
          if (pPos == DartHeuristicScanner.NOT_FOUND) {
            return firstPeer;
          }
          break; // closed scope -> keep searching
        case Symbols.TokenRBRACE:
          int peer = dScanner.findOpeningPeer(dPos, '{', '}');
          dPos = peer - 1;
          if (peer == DartHeuristicScanner.NOT_FOUND) {
            return firstPeer;
          }
          firstPeer = peer;
          break; // keep searching
        case Symbols.TokenRBRACKET:
          peer = dScanner.findOpeningPeer(dPos, '[', ']');
          dPos = peer - 1;
          if (peer == DartHeuristicScanner.NOT_FOUND) {
            return firstPeer;
          }
          firstPeer = peer;
          break; // keep searching
        case Symbols.TokenRPAREN:
          peer = dScanner.findOpeningPeer(dPos, '(', ')');
          dPos = peer - 1;
          if (peer == DartHeuristicScanner.NOT_FOUND) {
            return firstPeer;
          }
          firstPeer = peer;
          break; // keep searching
        case Symbols.TokenCASE:
        case Symbols.TokenDEFAULT:
          DartIndenter indenter = new DartIndenter(document, dScanner, null);
          peer = indenter.findReferencePosition(dPos, false, false, false, true);
          if (peer == DartHeuristicScanner.NOT_FOUND) {
            return firstPeer;
          }
          firstPeer = peer;
          break; // keep searching

        case Symbols.TokenEOF:
          return firstPeer;
        default:
          // keep searching
      }
    }
  }

  private int getStringEnd(IDocument d, int offset, int endOffset, char ch)
      throws BadLocationException {
    // TODO this needs to be rewritten to use DartScanner
    // and be sensitive to comments and strings in interpolation expressions
    // and to raw strings
    boolean multiline = false;
    if (offset + 2 <= endOffset) {
      if (d.getChar(offset) == ch && d.getChar(offset + 1) == ch) {
        offset += 2;
        multiline = true;
      }
    }
    while (offset < endOffset) {
      char curr = d.getChar(offset);
      offset++;
      if (curr == '\\') {
        // ignore escaped characters
        offset++;
      } else if (curr == ch) {
        if (!multiline) {
          return offset;
        } else if (offset + 2 <= endOffset) {
          if (d.getChar(offset) == ch && d.getChar(offset + 1) == ch) {
            return offset + 2;
          }
        }
      }
    }
    return endOffset;
  }

  /**
   * The preference setting for the visual tabulator display.
   * 
   * @return the number of spaces displayed for a tabulator in the editor
   */
  private int getVisualTabLengthPreference() {
    return CodeFormatterUtil.getTabWidth();
  }

//  private boolean isAfterClassPrologue(IDocument d, int p) {
//    DartHeuristicScanner scanner = new DartHeuristicScanner(d);
//    DartIndenter indenter = new DartIndenter(d, scanner, null);
//    return indenter.isAfterClassPrologue(p);
//  }

  private boolean hasMultiLineStringQuotes(String lineContent) {
    return lineContent.contains("'''") || lineContent.contains("\"\"\"");
  }

  private boolean isClosed(IDocument document, int offset, int length) {
    try {
      return getBracketCount(document, 0, document.getLength(), false) <= 0;
    } catch (BadLocationException e) {
      return true;
    }
  }

  /**
   * The preference setting that tells whether to insert spaces when pressing the Tab key.
   * 
   * @return <code>true</code> if spaces are inserted when pressing the Tab key
   */
  private boolean isInsertingSpacesForTab() {
    DartX.todo(); // Restore pref lookup
    return true;
//    return JavaScriptCore.SPACE.equals(getCoreOption(fProject,
//        DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR));
  }

  private boolean isLineDelimiter(IDocument document, String text) {
    String[] delimiters = document.getLegalLineDelimiters();
    if (delimiters != null) {
      return TextUtilities.equals(delimiters, text) > -1;
    }
    return false;
  }

//  /**
//   * If "p" is right after "{" and the next token is "," or ")" - i.e. tokens that we expect to
//   * follow a closure in an argument list.
//   */
//  private boolean looksLikeAfterLBraceForClosure(IDocument d, int p) {
//    DartHeuristicScanner scanner = new DartHeuristicScanner(d);
//    int len = d.getLength();
//    int token = scanner.nextToken(p, len);
//    if (token == Symbols.TokenCOMMA || token == Symbols.TokenRPAREN) {
//      return true;
//    }
//    return false;
//  }

  /**
   * Tells whether the given inserted string represents hitting the Tab key.
   * 
   * @param text the text to check
   * @return <code>true</code> if the text represents hitting the Tab key
   */
  private boolean isRepresentingTab(String text) {
    if (text == null) {
      return false;
    }

    if (isInsertingSpacesForTab()) {
      if (text.length() == 0 || text.length() > getVisualTabLengthPreference()) {
        return false;
      }
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) != ' ') {
          return false;
        }
      }
      return true;
    } else {
      return text.length() == 1 && text.charAt(0) == '\t';
    }
  }

  private boolean shouldCloseUnbalancedBrace(IDocument d, int p, int lineEnd) {
    DartHeuristicScanner scanner = new DartHeuristicScanner(d);
    if (scanner.findNonWhitespaceForward(p, lineEnd) == -1) {
      return true;
    }
    int token = scanner.nextToken(p, lineEnd);
    return token == Symbols.TokenCOMMA || token == Symbols.TokenRPAREN
        || token == Symbols.TokenRBRACKET;
  }

  private void smartIndentAfterClosingBracket(IDocument d, DocumentCommand c) {
    if (c.offset == -1 || d.getLength() == 0) {
      return;
    }

    try {
      int p = c.offset == d.getLength() ? c.offset - 1 : c.offset;
      int line = d.getLineOfOffset(p);
      int start = d.getLineOffset(line);
      int whiteend = findEndOfWhiteSpace(d, start, c.offset);

      DartHeuristicScanner scanner = new DartHeuristicScanner(d);
      DartIndenter indenter = new DartIndenter(d, scanner, null);

      // shift only when line does not contain any text up to the closing
      // bracket
      if (whiteend == c.offset) {
        // evaluate the line with the opening bracket that matches out closing
        // bracket
        int reference = indenter.findReferencePosition(c.offset, false, true, false, false);
        int indLine = d.getLineOfOffset(reference);
        if (indLine != -1 && indLine != line) {
          // take the indent of the found line
          StringBuffer replaceText = new StringBuffer(getIndentOfLine(d, indLine));
          // add the rest of the current line including the just added close
          // bracket
          replaceText.append(d.get(whiteend, c.offset - whiteend));
          replaceText.append(c.text);
          // modify document command
          c.length += c.offset - start;
          c.offset = start;
          c.text = replaceText.toString();
        }
      }
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void smartIndentAfterNewLine(IDocument d, DocumentCommand c) {
    DartHeuristicScanner scanner = new DartHeuristicScanner(d);
    DartIndenter indenter = new DartIndenter(d, scanner, null);
    StringBuffer indent = indenter.computeIndentation(c.offset);
    if (indent == null) {
      indent = new StringBuffer();
    }

    int docLength = d.getLength();
    if (c.offset == -1 || docLength == 0) {
      return;
    }

    try {
      int p = c.offset;
      int line = d.getLineOfOffset(p);

      StringBuffer buf = new StringBuffer(c.text + indent);

      IRegion reg = d.getLineInformation(line);
      int lineEnd = reg.getOffset() + reg.getLength();

      int contentStart = findEndOfWhiteSpace(d, c.offset, lineEnd);
      c.length = Math.max(contentStart - c.offset, 0);

      int start = reg.getOffset();
      ITypedRegion region = TextUtilities.getPartition(d, fPartitioning, start, true);
      if (DartPartitions.DART_DOC.equals(region.getType())) {
        start = d.getLineInformationOfOffset(region.getOffset()).getOffset();
      }

      // insert closing brace on new line after an unclosed opening brace
      if (getBracketCount(d, start, c.offset, true) > 0 && closeBrace()
          && !isClosed(d, c.offset, c.length)) {
        c.caretOffset = c.offset + buf.length();
        c.shiftsCaret = false;

//        // copy old content of line behind insertion point to new line
//        // unless we think we are inserting an unnamed function argument
//        if (!isAfterClassPrologue(d, p)) {
//          if (!looksLikeAfterLBraceForClosure(d, p)) {
//            if (lineEnd - contentStart > 0) {
//              c.length = lineEnd - c.offset;
//              buf.append(d.get(contentStart, lineEnd - contentStart).toCharArray());
//            }
//          }
//        }

        if (shouldCloseUnbalancedBrace(d, p, lineEnd)) {
          buf.append(TextUtilities.getDefaultLineDelimiter(d));
          StringBuffer reference = null;
          int nonWS = findEndOfWhiteSpace(d, start, lineEnd);
          if (nonWS < c.offset && d.getChar(nonWS) == '{') {
            reference = new StringBuffer(d.get(start, nonWS - start));
          } else {
            reference = indenter.getReferenceIndentation(c.offset, false);
          }
          if (reference != null) {
            buf.append(reference);
          }
          buf.append('}');
        }
      }
      // insert extra line upon new line between two braces
      else if (c.offset > start && contentStart < lineEnd && d.getChar(contentStart) == '}') {
        int firstCharPos = scanner.findNonWhitespaceBackward(c.offset - 1, start);
        if (firstCharPos != DartHeuristicScanner.NOT_FOUND && d.getChar(firstCharPos) == '{') {
          c.caretOffset = c.offset + buf.length();
          c.shiftsCaret = false;

          StringBuffer reference = null;
          int nonWS = findEndOfWhiteSpace(d, start, lineEnd);
          if (nonWS < c.offset && d.getChar(nonWS) == '{') {
            reference = new StringBuffer(d.get(start, nonWS - start));
          } else {
            reference = indenter.getReferenceIndentation(c.offset);
          }

          buf.append(TextUtilities.getDefaultLineDelimiter(d));

          if (reference != null) {
            buf.append(reference);
          }
        }
      }
      c.text = buf.toString();

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void smartIndentAfterOpeningBracket(IDocument d, DocumentCommand c) {
    if (c.offset < 1 || d.getLength() == 0) {
      return;
    }

    DartHeuristicScanner scanner = new DartHeuristicScanner(d);

    int p = c.offset == d.getLength() ? c.offset - 1 : c.offset;

    try {
      // current line
      int line = d.getLineOfOffset(p);
      int lineOffset = d.getLineOffset(line);

      // make sure we don't have any leading comments etc.
      if (d.get(lineOffset, p - lineOffset).trim().length() != 0) {
        return;
      }

      // line of last code
      int pos = scanner.findNonWhitespaceBackward(p, DartHeuristicScanner.UNBOUND);
      if (pos == -1) {
        return;
      }
      int lastLine = d.getLineOfOffset(pos);

      // only shift if the last line is further up and is a brace-less block candidate
      if (lastLine < line) {

        DartIndenter indenter = new DartIndenter(d, scanner, null);
        StringBuffer indent = indenter.computeIndentation(p, true);
        String toDelete = d.get(lineOffset, c.offset - lineOffset);
        if (indent != null && !indent.toString().equals(toDelete)) {
          c.text = indent.append(c.text).toString();
          c.length += c.offset - lineOffset;
          c.offset = lineOffset;
        }
      }

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }

  }

  private void smartIndentOnKeypress(IDocument document, DocumentCommand command) {
    switch (command.text.charAt(0)) {
      case '}':
        smartIndentAfterClosingBracket(document, command);
        break;
      case '{':
        smartIndentAfterOpeningBracket(document, command);
        break;
      case 'e':
        smartIndentUponE(document, command);
        break;
      case 't':
        smartIndentUponT(document, command);
        break;
      case ':':
        smartIndentUponColon(document, command);
    }
  }

  private void smartIndentUponColon(IDocument d, DocumentCommand c) {
    if (c.offset < 1 || d.getLength() == 0) {
      return;
    }
    DartHeuristicScanner scanner = new DartHeuristicScanner(d);
    int p = c.offset == d.getLength() ? c.offset - 1 : c.offset;
    try {
      // get current line
      int line = d.getLineOfOffset(p);
      int lineOffset = d.getLineOffset(line);
      // make sure we don't have any leading comments etc.
      String initialContent = d.get(lineOffset, p - lineOffset).trim();
      if (initialContent.length() != 0) {
        return;
      }
      // previous of last code
      int pos = scanner.findNonWhitespaceBackward(p, DartHeuristicScanner.UNBOUND);
      if (pos == -1) {
        return;
      }
      int tok = scanner.previousToken(p - 1, DartHeuristicScanner.UNBOUND);
      if (tok != DartHeuristicScanner.TokenRPAREN) {
        return;
      }
      int lparenLoc = scanner.findOpeningPeer(scanner.getPosition(), '(', ')');
      // try to identify a constructor decl name just before lparenLoc
      tok = scanner.previousToken(lparenLoc - 1, DartHeuristicScanner.UNBOUND);
      if (tok != DartHeuristicScanner.TokenIDENT) {
        return;
      }
      tok = scanner.previousToken(scanner.getPosition() - 1, DartHeuristicScanner.UNBOUND);
      if (tok == DartHeuristicScanner.TokenOTHER) { // period is Other
        tok = scanner.previousToken(lparenLoc, DartHeuristicScanner.UNBOUND);
        if (tok != DartHeuristicScanner.TokenIDENT) {
          return;
        }
      }
      DartIndenter indenter = new DartIndenter(d, scanner, null);
      int whiteend = findEndOfWhiteSpace(d, lineOffset, c.offset);
      // shift only when line does not contain any text up to the closing bracket
      if (whiteend == c.offset) {
        // adjust position of colon being inserted to 4 spaces in relative to previous line
        int reference = indenter.findReferencePosition(c.offset, false, true, false, false);
        int indLine = d.getLineOfOffset(reference);
        if (indLine != -1 && indLine != line) {
          // take the indent of the found line
          StringBuffer replaceText = new StringBuffer(getIndentOfLine(d, indLine));
          int additionalIndentLevels = 3;
          String indent = CodeFormatterUtil.createIndentString(additionalIndentLevels);
          replaceText.append(indent);
          // add the rest of the current line including the just added colon
          replaceText.append(d.get(whiteend, c.offset - whiteend));
          replaceText.append(c.text);
          // modify document command
          c.length += c.offset - lineOffset;
          c.offset = lineOffset;
          c.text = replaceText.toString();
        }
      }
    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void smartIndentUponE(IDocument d, DocumentCommand c) {
    if (c.offset < 4 || d.getLength() == 0) {
      return;
    }

    try {
      String content = d.get(c.offset - 3, 3);
      if (content.equals("els")) { //$NON-NLS-1$
        DartHeuristicScanner scanner = new DartHeuristicScanner(d);
        int p = c.offset - 3;

        // current line
        int line = d.getLineOfOffset(p);
        int lineOffset = d.getLineOffset(line);

        // make sure we don't have any leading comments etc.
        if (d.get(lineOffset, p - lineOffset).trim().length() != 0) {
          return;
        }

        // line of last code
        int pos = scanner.findNonWhitespaceBackward(p - 1, DartHeuristicScanner.UNBOUND);
        if (pos == -1) {
          return;
        }
        int lastLine = d.getLineOfOffset(pos);

        // only shift if the last line is further up and is a brace-less block candidate
        if (lastLine < line) {

          DartIndenter indenter = new DartIndenter(d, scanner, null);
          int ref = indenter.findReferencePosition(p, true, false, false, false);
          if (ref == DartHeuristicScanner.NOT_FOUND) {
            return;
          }
          int refLine = d.getLineOfOffset(ref);
          String indent = getIndentOfLine(d, refLine);

          if (indent != null) {
            c.text = indent.toString() + "else"; //$NON-NLS-1$
            c.length += c.offset - lineOffset;
            c.offset = lineOffset;
          }
        }

        return;
      }

      if (content.equals("cas")) { //$NON-NLS-1$
        smartReindentSwitchCase(d, c, "case", 3);
        return;
      }

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void smartIndentUponT(IDocument d, DocumentCommand c) {
    int numCharsBeforeT = 6;
    if (c.offset < numCharsBeforeT || d.getLength() == 0) {
      return;
    }

    try {
      String content = d.get(c.offset - numCharsBeforeT, numCharsBeforeT);

      if (content.equals("defaul")) { //$NON-NLS-1$
        smartReindentSwitchCase(d, c, "default", numCharsBeforeT); //$NON-NLS-1$
        return;
      }

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void smartPaste(IDocument document, DocumentCommand command) {
    int newOffset = command.offset;
    int newLength = command.length;
    String newText = command.text;

    try {
      DartHeuristicScanner scanner = new DartHeuristicScanner(document);
      DartIndenter indenter = new DartIndenter(document, scanner, null);
      int offset = newOffset;

      // reference position to get the indent from
      int refOffset = indenter.findReferencePosition(offset);
      if (refOffset == DartHeuristicScanner.NOT_FOUND) {
        return;
      }

      int peerOffset = getPeerPosition(document, command);
      peerOffset = indenter.findReferencePosition(peerOffset);
      if (peerOffset != DartHeuristicScanner.NOT_FOUND) {
        refOffset = Math.min(refOffset, peerOffset);
      }

      // eat any WS before the insertion to the beginning of the line
      int firstLine = 1; // don't format the first line per default, as it has
                         // other content before it
      IRegion line = document.getLineInformationOfOffset(offset);
      String notSelected = document.get(line.getOffset(), offset - line.getOffset());
      if (notSelected.trim().length() == 0) {
        newLength += notSelected.length();
        newOffset = line.getOffset();
        firstLine = 0;
      }

      // prefix: the part we need for formatting but won't paste
      IRegion refLine = document.getLineInformationOfOffset(refOffset);
      String prefix = document.get(refLine.getOffset(), newOffset - refLine.getOffset());

      // I don't see a good solution for pasting cascades.
      // For now, if we paste cascade after other cascade, just force the same indentation.
      String forcedCascadePrefix = computeForcedCascadePrefix(indenter, document, offset, newText);

      // handle the indentation computation inside a temporary document
      Document temp = new Document(prefix + newText);
      DocumentRewriteSession session = temp.startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
      scanner = new DartHeuristicScanner(temp);
      indenter = new DartIndenter(temp, scanner, null);
      installDartStuff(temp);

      // indent the first and second line
      // compute the relative indentation difference from the second line
      // (as the first might be partially selected) and use the value to
      // indent all other lines.
      StringBuffer addition = new StringBuffer();
      int insertLength = 0;
      int firstLineOriginalIndent = 0;
      int firstLineIndent = 0;
      int first = document.computeNumberOfLines(prefix) + firstLine; // don't format first line
      int lines = temp.getNumberOfLines();
      int tabLength = getVisualTabLengthPreference();

      boolean isInMultiLineString = false;
      {
        IRegion r = temp.getLineInformation(0);
        isInMultiLineString = hasMultiLineStringQuotes(temp.get(r.getOffset(), r.getLength()));
      }

      for (int l = first; l < lines; l++) { // we don't change the number of lines while adding indents

        IRegion r = temp.getLineInformation(l);
        int lineOffset = r.getOffset();
        int lineLength = r.getLength();
        String lineContent = temp.get(lineOffset, lineLength);

        if (lineLength == 0) {
          continue;
        }

        // indent the first pasted line
        String current = getCurrentIndent(temp, l);
        if (l == first) {
          firstLineOriginalIndent = computeVisualLength(current, tabLength);
        }
        // unless it is a line comment
        if (current.startsWith(LINE_COMMENT)) {
          continue;
        }
        StringBuffer correct;
        if (l == first) {
          if (forcedCascadePrefix != null) {
            correct = new StringBuffer(forcedCascadePrefix);
          } else {
            correct = indenter.computeIndentation(lineOffset);
          }
          firstLineIndent = computeVisualLength(correct, tabLength);
        } else {
          correct = new StringBuffer();
          int secondIndent = firstLineIndent + computeVisualLength(current, tabLength)
              - firstLineOriginalIndent;
          if (secondIndent > 0) {
            correct.append(StringUtils.repeat(' ', secondIndent));
          }
        }
        if (correct == null) {
          return; // bail out
        }

        insertLength = subtractIndent(correct, current, addition, tabLength);

        // relatively indent all pasted lines
        if (!isInMultiLineString) {
          if (insertLength > 0) {
            addIndent(temp, l, addition, tabLength);
          } else if (insertLength < 0) {
            cutIndent(temp, l, -insertLength, tabLength);
          }
        }

        if (hasMultiLineStringQuotes(lineContent)) {
          isInMultiLineString = !isInMultiLineString;
        }

      }

      removeDartStuff(temp);
      temp.stopRewriteSession(session);
      newText = temp.get(prefix.length(), temp.getLength() - prefix.length());

      // if a tab causes indentation to the current level, allow it to add another level
      if (!(newText.trim().isEmpty() && isRepresentingTab(command.text))) {
        command.offset = newOffset;
        command.length = newLength;
        command.text = newText;
      }

    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }

  }

  private void smartReindentSwitchCase(IDocument d, DocumentCommand c, String keyword, int delta)
      throws BadLocationException {
    DartHeuristicScanner scanner = new DartHeuristicScanner(d);
    int p = c.offset - delta;

    // current line
    int line = d.getLineOfOffset(p);
    int lineOffset = d.getLineOffset(line);

    // make sure we don't have any leading comments etc.
    if (d.get(lineOffset, p - lineOffset).trim().length() != 0) {
      return;
    }

    // line of last code
    int pos = scanner.findNonWhitespaceBackward(p - 1, DartHeuristicScanner.UNBOUND);
    if (pos == -1) {
      return;
    }
    int lastLine = d.getLineOfOffset(pos);

    // only shift if the last line is further up and is a brace-less block candidate
    if (lastLine < line) {

      DartIndenter indenter = new DartIndenter(d, scanner, null);
      int ref = indenter.findReferencePosition(p, false, false, false, true);
      if (ref == DartHeuristicScanner.NOT_FOUND) {
        return;
      }
      int refLine = d.getLineOfOffset(ref);
      int nextToken = scanner.nextToken(ref, DartHeuristicScanner.UNBOUND);
      String indent;
      if (nextToken == Symbols.TokenCASE || nextToken == Symbols.TokenDEFAULT) {
        indent = getIndentOfLine(d, refLine);
      } else {
        // at the brace of the switch
        indent = indenter.computeIndentation(p).toString();
      }

      if (indent != null) {
        c.text = indent.toString() + keyword; //$NON-NLS-1$
        c.length += c.offset - lineOffset;
        c.offset = lineOffset;
      }
    }

    return;
  }

  /**
   * Computes the difference of two indentations and returns the difference in length of current and
   * correct. If the return value is positive, <code>addition</code> is initialized with a substring
   * of that length of <code>correct</code>.
   * 
   * @param correct the correct indentation
   * @param current the current indentation (might contain non-whitespace)
   * @param difference a string buffer - if the return value is positive, it will be cleared and set
   *          to the substring of <code>current</code> of that length
   * @param tabLength the length of a tab
   * @return the difference in length of <code>correct</code> and <code>current</code>
   */
  private int subtractIndent(CharSequence correct, CharSequence current, StringBuffer difference,
      int tabLength) {
    int c1 = computeVisualLength(correct, tabLength);
    int c2 = computeVisualLength(current, tabLength);
    int diff = c1 - c2;
    if (diff <= 0) {
      return diff;
    }

    difference.setLength(0);
    int len = 0, i = 0;
    while (len < diff) {
      char c = correct.charAt(i++);
      difference.append(c);
      len += computeVisualLength(c, tabLength);
    }

    return diff;
  }
}
