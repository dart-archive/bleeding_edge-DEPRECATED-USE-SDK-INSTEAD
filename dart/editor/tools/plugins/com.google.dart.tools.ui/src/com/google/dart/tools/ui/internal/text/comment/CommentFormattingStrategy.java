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
package com.google.dart.tools.ui.internal.text.comment;

import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.internal.formatter.DefaultCodeFormatter;
import com.google.dart.tools.core.internal.formatter.InvalidInputException;
import com.google.dart.tools.core.internal.formatter.Scanner;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.util.LinkedList;
import java.util.Map;

/**
 * Formatting strategy for general source code comments.
 */
public class CommentFormattingStrategy extends ContextBasedFormattingStrategy {

  /**
   * Expands the given string's tabs according to the given tab size.
   * 
   * @param string the string
   * @param tabSize the tab size
   * @return the expanded string
   */
  private static StringBuffer expandTabs(String string, int tabSize) {
    StringBuffer expanded = new StringBuffer();
    for (int i = 0, n = string.length(), chars = 0; i < n; i++) {
      char ch = string.charAt(i);
      if (ch == '\t') {
        for (; chars < tabSize; chars++) {
          expanded.append(' ');
        }
        chars = 0;
      } else {
        expanded.append(ch);
        chars++;
        if (chars >= tabSize) {
          chars = 0;
        }
      }

    }
    return expanded;
  }

  /**
   * Returns the indentation size in space equivalents.
   * 
   * @param preferences the preferences
   * @return the indentation size in space equivalents
   */
  private static int getIndentSize(Map<String, String> preferences) {
    /*
     * FORMATTER_INDENTATION_SIZE is only used if FORMATTER_TAB_CHAR is MIXED. Otherwise, the
     * indentation size is in FORMATTER_TAB_CHAR. See CodeFormatterUtil.
     */
    String key;
    if (DefaultCodeFormatterConstants.MIXED.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR))) {
      key = DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
    } else {
      key = DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
    }

    if (preferences.containsKey(key)) {
      try {
        return Integer.parseInt(preferences.get(key));
      } catch (NumberFormatException e) {
        // use default
      }
    }
    return 4;
  }

  /**
   * Map from {@link DartPartitions}comment partition types to {@link CodeFormatter}code snippet
   * kinds.
   * 
   * @param type the partition type
   * @return the code snippet kind
   */
  private static int getKindForPartitionType(String type) {
    if (DartPartitions.DART_SINGLE_LINE_COMMENT.equals(type)) {
      return CodeFormatter.K_SINGLE_LINE_COMMENT;
    }
    if (DartPartitions.DART_MULTI_LINE_COMMENT.equals(type)) {
      return CodeFormatter.K_MULTI_LINE_COMMENT;
    }
    if (DartPartitions.DART_DOC.equals(type)) {
      return CodeFormatter.K_JAVA_DOC;
    }
    return CodeFormatter.K_UNKNOWN;
  }

  /**
   * Returns the visual tab size.
   * 
   * @param preferences the preferences
   * @return the visual tab size
   */
  private static int getTabSize(Map<String, String> preferences) {
    /*
     * If the tab-char is SPACE, FORMATTER_INDENTATION_SIZE is not used by the core formatter. We
     * piggy back the visual tab length setting in that preference in that case. See
     * CodeFormatterUtil.
     */
    String key;
    if (JavaScriptCore.SPACE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR))) {
      key = DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE;
    } else {
      key = DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
    }

    if (preferences.containsKey(key)) {
      try {
        return Integer.parseInt(preferences.get(key));
      } catch (NumberFormatException e) {
        // use default
      }
    }
    return 4;
  }

  /** Documents to be formatted by this strategy */
  private final LinkedList<IDocument> fDocuments = new LinkedList<IDocument>();

  /** Partitions to be formatted by this strategy */
  private final LinkedList<TypedPosition> fPartitions = new LinkedList<TypedPosition>();

  /** Last formatted document's hash-code. */
  private int fLastDocumentHash;

  /** Last formatted document header's hash-code. */
  private int fLastHeaderHash;

  /** End of the first class or interface token in the last document. */
  private int fLastMainTokenEnd = -1;

  /** End of the header in the last document. */
  private int fLastDocumentsHeaderEnd;

  /**
   * Calculates the <code>TextEdit</code> used to format the region with the properties indicated in
   * the formatting context previously supplied by <code>formatterStarts(IFormattingContext)</code>.
   * 
   * @see CommentFormattingStrategy#format()
   * @return A <code>TextEdit</code>, or <code>null</code> if no formating is required
   */
  public TextEdit calculateTextEdit() {
    super.format();

    final IDocument document = fDocuments.removeFirst();
    final TypedPosition position = fPartitions.removeFirst();
    if (document == null || position == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    Map<String, String> preferences = getPreferences();
    final boolean isFormattingHeader = DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_HEADER));
    int documentsHeaderEnd = computeHeaderEnd(document, preferences);

    TextEdit edit = null;
    if (position.offset >= documentsHeaderEnd) {
      // not a header
      try {
        // compute offset in document of region passed to the formatter
        int sourceOffset = document.getLineOffset(document.getLineOfOffset(position.getOffset()));

        // format region
        int partitionOffset = position.getOffset() - sourceOffset;
        int sourceLength = partitionOffset + position.getLength();
        String source = document.get(sourceOffset, sourceLength);
        CodeFormatter commentFormatter = new DefaultCodeFormatter(preferences);
        int indentationLevel = inferIndentationLevel(source.substring(0, partitionOffset),
            getTabSize(preferences), getIndentSize(preferences));
        edit = commentFormatter.format(getKindForPartitionType(position.getType()), source,
            partitionOffset, position.getLength(), indentationLevel,
            TextUtilities.getDefaultLineDelimiter(document));

        // move edit offset to match document
        if (edit != null) {
          edit.moveTree(sourceOffset);
        }
      } catch (BadLocationException x) {
        DartToolsPlugin.log(x);
      }
    } else if (isFormattingHeader) {
      boolean wasJavaDoc = DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT));
      if (!wasJavaDoc) {
        preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
            DefaultCodeFormatterConstants.TRUE);
      }

      boolean wasBlockComment = DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT));
      if (!wasBlockComment) {
        preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
            DefaultCodeFormatterConstants.TRUE);
      }

      boolean wasLineComment = DefaultCodeFormatterConstants.TRUE.equals(preferences.get(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT));
      if (!wasLineComment) {
        preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT,
            DefaultCodeFormatterConstants.TRUE);
      }

      try {
        // compute offset in document of region passed to the formatter
        int sourceOffset = document.getLineOffset(document.getLineOfOffset(position.getOffset()));

        // format region
        int partitionOffset = position.getOffset() - sourceOffset;
        int sourceLength = partitionOffset + position.getLength();
        String source = document.get(sourceOffset, sourceLength);
        CodeFormatter commentFormatter = new DefaultCodeFormatter(preferences);
        int indentationLevel = inferIndentationLevel(source.substring(0, partitionOffset),
            getTabSize(preferences), getIndentSize(preferences));
        edit = commentFormatter.format(getKindForPartitionType(position.getType()), source,
            partitionOffset, position.getLength(), indentationLevel,
            TextUtilities.getDefaultLineDelimiter(document));

        // move edit offset to match document
        if (edit != null) {
          edit.moveTree(sourceOffset);
        }
      } catch (BadLocationException x) {
        DartToolsPlugin.log(x);
      } finally {
        if (!wasJavaDoc) {
          preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT,
              DefaultCodeFormatterConstants.FALSE);
        }
        if (!wasBlockComment) {
          preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT,
              DefaultCodeFormatterConstants.FALSE);
        }
        if (!wasLineComment) {
          preferences.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_LINE_COMMENT,
              DefaultCodeFormatterConstants.FALSE);
        }
      }

    }
    return edit;
  }

  /*
   * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#format()
   */
  @Override
  public void format() {

    final IDocument document = fDocuments.getFirst();

    TextEdit edit = calculateTextEdit();
    if (edit == null) {
      return;
    }

    try {
      edit.apply(document);
    } catch (MalformedTreeException x) {
      DartToolsPlugin.log(x);
    } catch (BadLocationException x) {
      DartToolsPlugin.log(x);
    }
  }

  /*
   * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#formatterStarts
   * (org.eclipse.jface.text.formatter.IFormattingContext)
   */
  @Override
  public void formatterStarts(IFormattingContext context) {
    super.formatterStarts(context);

    fPartitions.addLast((TypedPosition) context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
    fDocuments.addLast((IDocument) context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
  }

  /*
   * @see org.eclipse.jface.text.formatter.IFormattingStrategyExtension#formatterStops ()
   */
  @Override
  public void formatterStops() {
    fPartitions.clear();
    fDocuments.clear();

    super.formatterStops();
  }

  /**
   * Returns the end offset for the document's header.
   * 
   * @param document the document
   * @param preferences the given preferences to format
   * @return the header's end offset
   */
  private int computeHeaderEnd(IDocument document, Map<String, String> preferences) {
    if (document == null) {
      return -1;
    }

    try {
      if (fLastMainTokenEnd >= 0 && document.hashCode() == fLastDocumentHash
          && fLastMainTokenEnd < document.getLength()
          && document.get(0, fLastMainTokenEnd).hashCode() == fLastHeaderHash) {
        return fLastDocumentsHeaderEnd;
      }
    } catch (BadLocationException e) {
      // should not happen -> recompute
    }

    Scanner scanner = new Scanner();
    scanner.setSource(document.get().toCharArray());

    try {
      int offset = -1;
      Token terminal = scanner.getNextToken();
      while (terminal == Token.COMMENT || terminal == Token.WHITESPACE) {

        if (terminal == Token.COMMENT) {
          DartComment.Style style = scanner.getCommentStyle();
          if (style == DartComment.Style.DART_DOC) {
            offset = scanner.getCurrentTokenStartPosition();
          }

        }
        terminal = scanner.getNextToken();
      }

      int mainTokenEnd = scanner.getCurrentTokenEndPosition();
      if (terminal != Token.EOS) {
        mainTokenEnd++;
        offset = -1;
      }
      try {
        fLastHeaderHash = document.get(0, mainTokenEnd).hashCode();
      } catch (BadLocationException e) {
        // should not happen -> recompute next time
        mainTokenEnd = -1;
      }

      fLastDocumentHash = document.hashCode();
      fLastMainTokenEnd = mainTokenEnd;
      fLastDocumentsHeaderEnd = offset;
      return offset;

    } catch (InvalidInputException ex) {
      // enable formatting
      return -1;
    }
  }

  /**
   * Infer the indentation level based on the given reference indentation and tab size.
   * 
   * @param reference the reference indentation
   * @param tabSize the tab size
   * @param indentSize the indent size in space equivalents
   * @return the inferred indentation level
   */
  private int inferIndentationLevel(String reference, int tabSize, int indentSize) {
    StringBuffer expanded = expandTabs(reference, tabSize);

    int referenceWidth = expanded.length();
    if (tabSize == 0) {
      return referenceWidth;
    }

    int level = referenceWidth / indentSize;
    if (referenceWidth % indentSize > 0) {
      level++;
    }
    return level;
  }
}
