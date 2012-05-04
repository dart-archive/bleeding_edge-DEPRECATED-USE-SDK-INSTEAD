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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.compiler.ast.DartComment;
import com.google.dart.compiler.ast.DartComment.Style;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.internal.formatter.CodeSnippetParsingUtil.RecordedParsingInformation;
import com.google.dart.tools.core.internal.formatter.align.Alignment;
import com.google.dart.tools.core.internal.formatter.align.AlignmentException;
import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.internal.util.Util;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Instances of the class <code>Scribe</code> produce formatted source.
 */
@SuppressWarnings({"deprecation", "unused"})
public class Scribe {
  // Class to store previous line comment information
  static class LineComment {
    boolean contiguous = false;
    int currentIndentation, indentation;
    int lines;
    char[] leadingSpaces;
  }

  static {
    // Remove both @SuppressWarnings when finished
    // "Finished" means Dart doc is properly supported
    DartCore.notYetImplemented();
  }

  /** BLOCK COMMENTS */
  private static final String BLOCK_HEADER = "/*"; //$NON-NLS-1$
  private static final int BLOCK_HEADER_LENGTH = BLOCK_HEADER.length();
  private static final String JAVADOC_HEADER = "/**"; //$NON-NLS-1$
  private static final int JAVADOC_HEADER_LENGTH = JAVADOC_HEADER.length();
  private static final String BLOCK_LINE_PREFIX = " * "; //$NON-NLS-1$
  private static final int BLOCK_LINE_PREFIX_LENGTH = BLOCK_LINE_PREFIX.length();
  private static final String BLOCK_FOOTER = "*/"; //$NON-NLS-1$
  private static final int BLOCK_FOOTER_LENGTH = BLOCK_FOOTER.length();

  /** LINE COMMENTS */
  private static final String LINE_COMMENT_PREFIX = "// "; //$NON-NLS-1$
  private static final int LINE_COMMENT_PREFIX_LENGTH = LINE_COMMENT_PREFIX.length();

  private static final int INITIAL_SIZE = 100;
  private static final Token SKIP_FIRST_WHITESPACE_TOKEN = Token.NON_TOKEN;

  // "const" not in Parser.PSEUDO_KEYWORDS
  // "native" only appears in block position
  /** Alphabetized list of modifier names. */
  private static final String[] MODIFIER_KEYWORDS = {
      "abstract", "const", "factory", "get", "native", "operator", "set", "static"};

  private boolean checkLineWrapping;

  /** one-based column */
  public int column;

  private int[][] commentPositions;
  // Most specific alignment.
  public Alignment currentAlignment;

  public Token currentToken;
  // edits management
  private OptimizedReplaceEdit[] edits;

  public int editsIndex;
  public CodeFormatterVisitor formatter;
  public int indentationLevel;
  public int lastNumberOfNewLines;
  private boolean preserveLineBreakIndentation = false;

  public int line;
  private int[] lineEnds;
  private int maxLines;
  public Alignment memberAlignment;

  public boolean needSpace = false;
  // Line separator infos
  final private String lineSeparator;
  final private String lineSeparatorAndSpace;
  final private char firstLS;

  final private int lsLength;
  public int nlsTagCounter;
  public int pageWidth;

  public boolean pendingSpace = false;
  public Scanner scanner;
  public int scannerEndPosition;
  public int tabLength;
  public int indentationSize;
  private final IRegion[] regions;
  private IRegion[] adaptedRegions;
  public int tabChar;
  public int numberOfIndentations;

  private boolean useTabsOnlyForLeadingIndents;
  /** empty lines */
  private final boolean indentEmptyLines;

  int blank_lines_between_import_groups = -1;
  // Preserve empty lines constants
  public static final int DO_NOT_PRESERVE_EMPTY_LINES = -1;
  public static final int PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION = 1;
  public static final int PRESERVE_EMPTY_LINES_IN_FORMAT_LEFT_CURLY_BRACE = 2;
  public static final int PRESERVE_EMPTY_LINES_IN_STRING_LITERAL_CONCATENATION = 3;
  public static final int PRESERVE_EMPTY_LINES_IN_CLOSING_ARRAY_INITIALIZER = 4;
  public static final int PRESERVE_EMPTY_LINES_IN_FORMAT_OPENING_BRACE = 5;
  public static final int PRESERVE_EMPTY_LINES_IN_BINARY_EXPRESSION = 6;
  public static final int PRESERVE_EMPTY_LINES_IN_EQUALITY_EXPRESSION = 7;
  public static final int PRESERVE_EMPTY_LINES_BEFORE_ELSE = 8;
  public static final int PRESERVE_EMPTY_LINES_IN_SWITCH_CASE = 9;
  public static final int PRESERVE_EMPTY_LINES_AT_END_OF_METHOD_DECLARATION = 10;
  public static final int PRESERVE_EMPTY_LINES_AT_END_OF_BLOCK = 11;
  final static int PRESERVE_EMPTY_LINES_DO_NOT_USE_ANY_INDENTATION = -1;
  final static int PRESERVE_EMPTY_LINES_USE_CURRENT_INDENTATION = 0;

  final static int PRESERVE_EMPTY_LINES_USE_TEMPORARY_INDENTATION = 1;
  /** disabling */
  boolean editsEnabled;
//  boolean useTags;
//  int tagsKind;

  /* Comments formatting */
//  private static final int INCLUDE_BLOCK_COMMENTS = CodeFormatter.F_INCLUDE_COMMENTS
//      | CodeFormatter.K_MULTI_LINE_COMMENT;
//  private static final int INCLUDE_JAVA_DOC = CodeFormatter.F_INCLUDE_COMMENTS
//      | CodeFormatter.K_JAVA_DOC;
//  private static final int INCLUDE_LINE_COMMENTS = CodeFormatter.F_INCLUDE_COMMENTS
//      | CodeFormatter.K_SINGLE_LINE_COMMENT;
//  private static final int SKIP_FIRST_WHITESPACE_TOKEN = -2;
  private static final int INVALID_TOKEN = 2000;
  static final int NO_TRAILING_COMMENT = 0x0000;
  static final int BASIC_TRAILING_COMMENT = 0x0100;
  static final int COMPLEX_TRAILING_COMMENT = 0x0200;
  static final int IMPORT_TRAILING_COMMENT = COMPLEX_TRAILING_COMMENT | 0x0001;
  static final int UNMODIFIABLE_TRAILING_COMMENT = 0x0400;
  private int formatComments = 0;
  // private int headerEndPosition = -1;

  String commentIndentation; // indentation requested in comments (usually in
                             // javadoc root tags description)
  final LineComment lastLineComment = new LineComment();

  // New way to format javadoc
  // specialized parser to format comments
  static {
    DartCore.notYetImplemented();
  }
  // private FormatterCommentParser formatterCommentParser;

  // Disabling and enabling tags
  OptimizedReplaceEdit previousDisabledEdit;
  private char[] disablingTag, enablingTag;

  // Well known strings
  private String[] newEmptyLines = new String[10];
  private static String[] COMMENT_INDENTATIONS = new String[20];

  // final string buffers
  private final StringBuffer tempBuffer = new StringBuffer();

  private final StringBuffer blockCommentBuffer = new StringBuffer();
  private final StringBuffer blockCommentTokensBuffer = new StringBuffer();

// private final StringBuffer codeSnippetBuffer = new StringBuffer();
// private final StringBuffer javadocBlockRefBuffer = new StringBuffer();
// private final StringBuffer javadocGapLinesBuffer = new StringBuffer();
// private StringBuffer[] javadocHtmlTagBuffers = new StringBuffer[5];
// private final StringBuffer javadocTextBuffer = new StringBuffer();
// private final StringBuffer javadocTokensBuffer = new StringBuffer();

  Scribe(CodeFormatterVisitor formatter, IRegion[] regions,
      CodeSnippetParsingUtil codeSnippetParsingUtil, boolean includeComments) {
    initializeScanner(formatter.preferences);
    this.formatter = formatter;
    pageWidth = formatter.preferences.page_width;
    tabLength = formatter.preferences.tab_size;
    indentationLevel = 0; // initialize properly
    numberOfIndentations = 0;
    useTabsOnlyForLeadingIndents = formatter.preferences.use_tabs_only_for_leading_indentations;
    indentEmptyLines = formatter.preferences.indent_empty_lines;
    tabChar = formatter.preferences.tab_char;
    if (tabChar == DefaultCodeFormatterOptions.MIXED) {
      indentationSize = formatter.preferences.indentation_size;
    } else {
      indentationSize = tabLength;
    }
    lineSeparator = formatter.preferences.line_separator;
    lineSeparatorAndSpace = lineSeparator + ' ';
    firstLS = lineSeparator.charAt(0);
    lsLength = lineSeparator.length();
    indentationLevel = formatter.preferences.initial_indentation_level * indentationSize;
    this.regions = regions;
    if (codeSnippetParsingUtil != null) {
      final RecordedParsingInformation information = codeSnippetParsingUtil.recordedParsingInformation;
      if (information != null) {
        lineEnds = information.lineEnds;
        commentPositions = information.commentPositions;
      }
    }
    if (formatter.preferences.comment_format_line_comment) {
      formatComments |= CodeFormatter.K_SINGLE_LINE_COMMENT;
    }
    if (formatter.preferences.comment_format_block_comment) {
      formatComments |= CodeFormatter.K_MULTI_LINE_COMMENT;
    }
    if (formatter.preferences.comment_format_javadoc_comment) {
      formatComments |= CodeFormatter.K_JAVA_DOC;
    }
    if (includeComments) {
      formatComments |= CodeFormatter.F_INCLUDE_COMMENTS;
    }
    reset();
  }

  public final void addInsertEdit(int insertPosition, String insertedString) {
    if (edits.length == editsIndex) {
      // resize
      resize();
    }
    addOptimizedReplaceEdit(insertPosition, 0, insertedString);
  }

  public final void addReplaceEdit(int start, int end, String replacement) {
    if (edits.length == editsIndex) {
      // resize
      resize();
    }
    addOptimizedReplaceEdit(start, end - start + 1, replacement);
  }

  public void alignFragment(Alignment alignment, int fragmentIndex) {
    alignment.fragmentIndex = fragmentIndex;
    alignment.checkColumn();
    alignment.performFragmentEffect();
  }

  public void checkNLSTag(int sourceStart) {
    if (hasNLSTag(sourceStart)) {
      nlsTagCounter++;
    }
  }

  /**
   * After formatting an embedded expression in string interpolation we have to restore some scanner
   * state.
   */
  public void continueStringInterpolation() {
    scanner.continueStringInterpolation();
  }

  public Alignment createAlignment(int kind, int mode, int count, int sourceRestart) {
    return createAlignment(kind, mode, Alignment.R_INNERMOST, count, sourceRestart);
  }

  public Alignment createAlignment(int kind, int mode, int tieBreakRule, int count,
      int sourceRestart) {
    return createAlignment(
        kind,
        mode,
        tieBreakRule,
        count,
        sourceRestart,
        formatter.preferences.continuation_indentation,
        false);
  }

  public Alignment createAlignment(int kind, int mode, int count, int sourceRestart,
      int continuationIndent, boolean adjust) {
    return createAlignment(
        kind,
        mode,
        Alignment.R_INNERMOST,
        count,
        sourceRestart,
        continuationIndent,
        adjust);
  }

  public Alignment createAlignment(int kind, int mode, int tieBreakRule, int count,
      int sourceRestart, int continuationIndent, boolean adjust) {
    Alignment alignment = new Alignment(
        kind,
        mode,
        tieBreakRule,
        this,
        count,
        sourceRestart,
        continuationIndent);
    // specific break indentation for message arguments inside binary
    // expressions
    if (currentAlignment == null
        && formatter.expressionsDepth >= 0
        || currentAlignment != null
        && currentAlignment.kind == Alignment.BINARY_EXPRESSION
        && (formatter.expressionsPos & CodeFormatterVisitor.EXPRESSIONS_POS_MASK) == CodeFormatterVisitor.EXPRESSIONS_POS_BETWEEN_TWO) {
      switch (kind) {
        case Alignment.CONDITIONAL_EXPRESSION:
        case Alignment.MESSAGE_ARGUMENTS:
        case Alignment.MESSAGE_SEND:
          if (formatter.lastBinaryExpressionAlignmentBreakIndentation == alignment.breakIndentationLevel) {
            alignment.breakIndentationLevel += indentationSize;
            alignment.shiftBreakIndentationLevel += indentationSize;
            formatter.lastBinaryExpressionAlignmentBreakIndentation = 0;
          }
          break;
      }
    }
    // adjust break indentation
    if (adjust && memberAlignment != null) {
      Alignment current = memberAlignment;
      while (current.enclosing != null) {
        current = current.enclosing;
      }
      if ((current.mode & Alignment.M_MULTICOLUMN) != 0) {
        final int indentSize = indentationSize;
        switch (current.chunkKind) {
          case Alignment.CHUNK_METHOD:
          case Alignment.CHUNK_TYPE:
            if ((mode & Alignment.M_INDENT_BY_ONE) != 0) {
              alignment.breakIndentationLevel = indentationLevel + indentSize;
            } else {
              alignment.breakIndentationLevel = indentationLevel + continuationIndent * indentSize;
            }
            alignment.update();
            break;
          case Alignment.CHUNK_FIELD:
            if ((mode & Alignment.M_INDENT_BY_ONE) != 0) {
              alignment.breakIndentationLevel = current.originalIndentationLevel + indentSize;
            } else {
              alignment.breakIndentationLevel = current.originalIndentationLevel
                  + continuationIndent * indentSize;
            }
            alignment.update();
            break;
        }
      } else {
        switch (current.mode & Alignment.SPLIT_MASK) {
          case Alignment.M_COMPACT_SPLIT:
          case Alignment.M_COMPACT_FIRST_BREAK_SPLIT:
          case Alignment.M_NEXT_PER_LINE_SPLIT:
          case Alignment.M_NEXT_SHIFTED_SPLIT:
          case Alignment.M_ONE_PER_LINE_SPLIT:
            final int indentSize = indentationSize;
            switch (current.chunkKind) {
              case Alignment.CHUNK_METHOD:
              case Alignment.CHUNK_TYPE:
                if ((mode & Alignment.M_INDENT_BY_ONE) != 0) {
                  alignment.breakIndentationLevel = indentationLevel + indentSize;
                } else {
                  alignment.breakIndentationLevel = indentationLevel + continuationIndent
                      * indentSize;
                }
                alignment.update();
                break;
              case Alignment.CHUNK_FIELD:
                if ((mode & Alignment.M_INDENT_BY_ONE) != 0) {
                  alignment.breakIndentationLevel = current.originalIndentationLevel + indentSize;
                } else {
                  alignment.breakIndentationLevel = current.originalIndentationLevel
                      + continuationIndent * indentSize;
                }
                alignment.update();
                break;
            }
            break;
        }
      }
    }
    return alignment;
  }

  public Alignment createMemberAlignment(int kind, int mode, int count, int sourceRestart) {
    Alignment mAlignment = createAlignment(kind, mode, Alignment.R_INNERMOST, count, sourceRestart);
    mAlignment.breakIndentationLevel = indentationLevel;
    return mAlignment;
  }

  public void enterAlignment(Alignment alignment) {
    alignment.enclosing = currentAlignment;
    alignment.location.lastLocalDeclarationSourceStart = formatter.lastLocalDeclarationSourceStart;
    currentAlignment = alignment;
  }

  public void enterMemberAlignment(Alignment alignment) {
    alignment.enclosing = memberAlignment;
    alignment.location.lastLocalDeclarationSourceStart = formatter.lastLocalDeclarationSourceStart;
    memberAlignment = alignment;
  }

  public void exitAlignment(Alignment alignment, boolean discardAlignment) {
    Alignment current = currentAlignment;
    while (current != null) {
      if (current == alignment) {
        break;
      }
      current = current.enclosing;
    }
    if (current == null) {
      throw new AbortFormatting("could not find matching alignment: " + alignment); //$NON-NLS-1$
    }
    indentationLevel = alignment.location.outputIndentationLevel;
    numberOfIndentations = alignment.location.numberOfIndentations;
    formatter.lastLocalDeclarationSourceStart = alignment.location.lastLocalDeclarationSourceStart;
    if (discardAlignment) {
      currentAlignment = alignment.enclosing;
      if (currentAlignment == null) {
        formatter.lastBinaryExpressionAlignmentBreakIndentation = 0;
      }
    }
  }

  public void exitMemberAlignment(Alignment alignment) {
    Alignment current = memberAlignment;
    while (current != null) {
      if (current == alignment) {
        break;
      }
      current = current.enclosing;
    }
    if (current == null) {
      throw new AbortFormatting("could not find matching alignment: " + alignment); //$NON-NLS-1$
    }
    indentationLevel = current.location.outputIndentationLevel;
    numberOfIndentations = current.location.numberOfIndentations;
    formatter.lastLocalDeclarationSourceStart = alignment.location.lastLocalDeclarationSourceStart;
    memberAlignment = current.enclosing;
  }

  /**
   * Answer actual indentation level based on true column position
   * 
   * @return int
   */
  public int getColumnIndentationLevel() {
    return column - 1;
  }

  public final int getCommentIndex(int position) {
    if (commentPositions == null) {
      return -1;
    }
    int length = commentPositions.length;
    if (length == 0) {
      return -1;
    }
    int g = 0, d = length - 1;
    int m = 0;
    while (g <= d) {
      m = g + (d - g) / 2;
      int bound = commentPositions[m][1];
      if (bound < 0) {
        bound = -bound;
      }
      if (bound < position) {
        g = m + 1;
      } else if (bound > position) {
        d = m - 1;
      } else {
        return m;
      }
    }
    return -(g + 1);
  }

  public String getEmptyLines(int linesNumber) {
    if (nlsTagCounter > 0) {
      return Util.EMPTY_STRING;
    }
    String emptyLines;
    if (lastNumberOfNewLines == 0) {
      linesNumber++; // add an extra line breaks
      if (indentEmptyLines) {
        tempBuffer.setLength(0);
        for (int i = 0; i < linesNumber; i++) {
          printIndentationIfNecessary(tempBuffer);
          tempBuffer.append(lineSeparator);
          column = 1;
        }
        emptyLines = tempBuffer.toString();
      } else {
        emptyLines = getNewLineString(linesNumber);
      }
      lastNumberOfNewLines += linesNumber;
      line += linesNumber;
      column = 1;
      needSpace = false;
      pendingSpace = false;
    } else if (lastNumberOfNewLines == 1) {
      if (indentEmptyLines) {
        tempBuffer.setLength(0);
        for (int i = 0; i < linesNumber; i++) {
          printIndentationIfNecessary(tempBuffer);
          tempBuffer.append(lineSeparator);
          column = 1;
        }
        emptyLines = tempBuffer.toString();
      } else {
        emptyLines = getNewLineString(linesNumber);
      }
      lastNumberOfNewLines += linesNumber;
      line += linesNumber;
      column = 1;
      needSpace = false;
      pendingSpace = false;
    } else {
      if (lastNumberOfNewLines - 1 >= linesNumber) {
        // there is no need to add new lines
        return Util.EMPTY_STRING;
      }
      final int realNewLineNumber = linesNumber - lastNumberOfNewLines + 1;
      if (indentEmptyLines) {
        tempBuffer.setLength(0);
        for (int i = 0; i < realNewLineNumber; i++) {
          printIndentationIfNecessary(tempBuffer);
          tempBuffer.append(lineSeparator);
          column = 1;
        }
        emptyLines = tempBuffer.toString();
      } else {
        emptyLines = getNewLineString(realNewLineNumber);
      }
      lastNumberOfNewLines += realNewLineNumber;
      line += realNewLineNumber;
      column = 1;
      needSpace = false;
      pendingSpace = false;
    }
    return emptyLines;
  }

  public OptimizedReplaceEdit getLastEdit() {
    if (editsIndex > 0) {
      return edits[editsIndex - 1];
    }
    return null;
  }

  public final int getLineEnd(int lineNumber) {
    if (lineEnds == null) {
      return -1;
    }
    if (lineNumber >= lineEnds.length + 1) {
      return scannerEndPosition;
    }
    if (lineNumber <= 0) {
      return -1;
    }
    return lineEnds[lineNumber - 1]; // next line start one character
                                     // behind the lineEnd of the previous
                                     // line
  }

  public String getNewLine() {
    if (nlsTagCounter > 0) {
      return Util.EMPTY_STRING;
    }
    if (lastNumberOfNewLines >= 1) {
      column = 1; // ensure that the scribe is at the beginning of a new
                  // line
      return Util.EMPTY_STRING;
    }
    line++;
    lastNumberOfNewLines = 1;
    column = 1;
    needSpace = false;
    pendingSpace = false;
    return lineSeparator;
  }

  /**
   * Answer next indentation level based on column estimated position (if column is not indented,
   * then use indentationLevel)
   */
  public int getNextIndentationLevel(int someColumn) {
    int indent = someColumn - 1;
    if (indent == 0) {
      return indentationLevel;
    }
    if (tabChar == DefaultCodeFormatterOptions.TAB) {
      if (useTabsOnlyForLeadingIndents) {
        return indent;
      }
      if (indentationSize == 0) {
        return indent;
      }
      int rem = indent % indentationSize;
      int addition = rem == 0 ? 0 : indentationSize - rem; // round to
                                                           // superior
      return indent + addition;
    }
    return indent;
  }

  public TextEdit getRootEdit() {
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=208541
    adaptRegions();
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=234583
    adaptEdits();

    MultiTextEdit edit = null;
    int regionsLength = adaptedRegions.length;
    int textRegionStart;
    int textRegionEnd;
    if (regionsLength == 1) {
      IRegion lastRegion = adaptedRegions[0];
      textRegionStart = lastRegion.getOffset();
      textRegionEnd = textRegionStart + lastRegion.getLength();
    } else {
      textRegionStart = adaptedRegions[0].getOffset();
      IRegion lastRegion = adaptedRegions[regionsLength - 1];
      textRegionEnd = lastRegion.getOffset() + lastRegion.getLength();
    }

    int length = textRegionEnd - textRegionStart + 1;
    if (textRegionStart <= 0) {
      if (length <= 0) {
        edit = new MultiTextEdit(0, 0);
      } else {
        edit = new MultiTextEdit(0, textRegionEnd);
      }
    } else {
      edit = new MultiTextEdit(textRegionStart, length - 1);
    }
    for (int i = 0, max = editsIndex; i < max; i++) {
      OptimizedReplaceEdit currentEdit = edits[i];
      if (currentEdit.offset >= 0 && currentEdit.offset <= scannerEndPosition) {
        if (currentEdit.length == 0 || currentEdit.offset != scannerEndPosition
            && isMeaningfulEdit(currentEdit)) {
          try {
            edit.addChild(new ReplaceEdit(
                currentEdit.offset,
                currentEdit.length,
                currentEdit.replacement));
          } catch (MalformedTreeException ex) {
            // log exception in case of error
            DartCore.notYetImplemented();
            // CommentFormatterUtil.log(ex);
            throw ex;
          }
        }
      }
    }
    edits = null;
    return edit;
  }

  public void handleLineTooLong() {
    if (formatter.preferences.wrap_outer_expressions_when_nested) {
      handleLineTooLongSmartly();
      return;
    }
    // search for closest breakable alignment, using tiebreak rules
    // look for outermost breakable one
    int relativeDepth = 0, outerMostDepth = -1;
    Alignment targetAlignment = currentAlignment;
    while (targetAlignment != null) {
      if (targetAlignment.tieBreakRule == Alignment.R_OUTERMOST && targetAlignment.couldBreak()) {
        outerMostDepth = relativeDepth;
      }
      targetAlignment = targetAlignment.enclosing;
      relativeDepth++;
    }
    if (outerMostDepth >= 0) {
      throw new AlignmentException(AlignmentException.LINE_TOO_LONG, outerMostDepth);
    }
    // look for innermost breakable one
    relativeDepth = 0;
    targetAlignment = currentAlignment;
    while (targetAlignment != null) {
      if (targetAlignment.couldBreak()) {
        throw new AlignmentException(AlignmentException.LINE_TOO_LONG, relativeDepth);
      }
      targetAlignment = targetAlignment.enclosing;
      relativeDepth++;
    }
    // did not find any breakable location - proceed
  }

  public void indent() {
    indentationLevel += indentationSize;
    numberOfIndentations++;
  }

  public void printArrayQualifiedReference(int numberOfTokens, int sourceEnd) {
    int currentTokenStartPosition = scanner.currentPosition;
    DartScanner.State scannerState = scanner.getState();
    int numberOfIdentifiers = 0;
    try {
      do {
        printComment(CodeFormatter.K_UNKNOWN, NO_TRAILING_COMMENT);
        switch (currentToken = scanner.getNextToken()) {
          case EOS:
            return;
          case WHITESPACE:
            addDeleteEdit(
                scanner.getCurrentTokenStartPosition(),
                scanner.getCurrentTokenEndPosition());
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case COMMENT:
            Style style = getCommentStyle(currentTokenStartPosition);
            switch (style) {
              case BLOCK:
                printBlockComment(false);
                break;
              case DART_DOC:
                printBlockComment(true);
                break;
              case END_OF_LINE:
                printLineComment();
                break;
            }
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case IDENTIFIER:
            print(scanner.currentPosition - scanner.startPosition, false);
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            if (++numberOfIdentifiers == numberOfTokens) {
              scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
              scanner.restoreState(scannerState);
              return;
            }
            break;
          case PERIOD:
            print(scanner.currentPosition - scanner.startPosition, false);
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case RPAREN:
            currentTokenStartPosition = scanner.startPosition;
            scannerState = scanner.getState();
            // $FALL-THROUGH$ - fall through default case...
          default:
            scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
            scanner.restoreState(scannerState);
            return;
        }
      } while (scanner.currentPosition <= sourceEnd);
    } catch (InvalidInputException e) {
      throw new AbortFormatting(e);
    }
  }

  public void printEmptyLines(int linesNumber) {
    this.printEmptyLines(linesNumber, scanner.getCurrentTokenEndPosition() + 1);
  }

  public void printEndOfCompilationUnit() {
    try {
      // if we have a space between two tokens we ensure it will be dumped in
      // the formatted string
      int currentTokenStartPosition = scanner.currentPosition;
      DartScanner.State scannerState = scanner.getState();
      boolean hasComment = false;
      boolean hasLineComment = false;
      boolean hasWhitespace = false;
      int count = 0;
      while (true) {
        currentToken = scanner.getNextToken();
        int currentTokenEndPosition = scanner.getCurrentTokenEndPosition();
        switch (currentToken) {
          case WHITESPACE:
            char[] whiteSpaces;
            if (currentTokenEndPosition < currentTokenStartPosition) {
              whiteSpaces = scanner.getLastTokenString().toCharArray();
              currentTokenEndPosition++;
            } else {
              whiteSpaces = scanner.getCurrentTokenString().toCharArray();
            }
            count = 0;
            for (int i = 0, max = whiteSpaces.length; i < max; i++) {
              switch (whiteSpaces[i]) {
                case '\r':
                  if (i + 1 < max) {
                    if (whiteSpaces[i + 1] == '\n') {
                      i++;
                    }
                  }
                  count++;
                  break;
                case '\n':
                  count++;
              }
            }
            if (count == 0) {
              hasWhitespace = true;
              addDeleteEdit(scanner.getCurrentTokenStartPosition(), currentTokenEndPosition);
            } else if (hasLineComment) {
              preserveEmptyLines(count, scanner.getCurrentTokenStartPosition());
              addDeleteEdit(scanner.getCurrentTokenStartPosition(), currentTokenEndPosition);
            } else if (hasComment) {
              if (count == 1) {
                printNewLine(scanner.getCurrentTokenStartPosition());
              } else {
                preserveEmptyLines(count - 1, scanner.getCurrentTokenStartPosition());
              }
              addDeleteEdit(scanner.getCurrentTokenStartPosition(), currentTokenEndPosition);
            } else {
              addDeleteEdit(scanner.getCurrentTokenStartPosition(), currentTokenEndPosition);
            }
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case COMMENT:
            switch (getCommentStyle(currentTokenStartPosition)) {
              case END_OF_LINE:
                if (count >= 1) {
                  if (count > 1) {
                    preserveEmptyLines(count - 1, scanner.getCurrentTokenStartPosition());
                  } else if (count == 1) {
                    printNewLine(scanner.getCurrentTokenStartPosition());
                  }
                } else if (hasWhitespace) {
                  space();
                }
                hasWhitespace = false;
                printLineComment();
                currentTokenStartPosition = scanner.currentPosition;
                scannerState = scanner.getState();
                hasLineComment = true;
                count = 0;
                break;
              case BLOCK:
                if (count >= 1) {
                  if (count > 1) {
                    preserveEmptyLines(count - 1, scanner.getCurrentTokenStartPosition());
                  } else if (count == 1) {
                    printNewLine(scanner.getCurrentTokenStartPosition());
                  }
                } else if (hasWhitespace) {
                  space();
                }
                hasWhitespace = false;
                printBlockComment(false);
                currentTokenStartPosition = scanner.currentPosition;
                scannerState = scanner.getState();
                hasLineComment = false;
                hasComment = true;
                count = 0;
                break;
              case DART_DOC:
                if (count >= 1) {
                  if (count > 1) {
                    preserveEmptyLines(count - 1, scanner.startPosition);
                  } else if (count == 1) {
                    printNewLine(scanner.startPosition);
                  }
                } else if (hasWhitespace) {
                  space();
                }
                hasWhitespace = false;
                if (includesJavadocComments()) {
                  printJavadocComment(scanner.startPosition, scanner.currentPosition);
                } else {
                  printBlockComment(true);
                }
                printNewLine();
                currentTokenStartPosition = scanner.currentPosition;
                scannerState = scanner.getState();
                hasLineComment = false;
                hasComment = true;
                count = 0;
                break;
            }
            break;
          case SEMICOLON:
            print(
                scanner.currentPosition - scanner.startPosition,
                formatter.preferences.insert_space_before_semicolon);
            break;
          case EOS:
            if (count >= 1 || formatter.preferences.insert_new_line_at_end_of_file_if_missing) {
              if (!hasLineComment) {
                printNewLine(scannerEndPosition);
              }
            }
            return;
          default:
            // step back one token
            scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
            scanner.restoreState(scannerState);
            return;
        }
      }
    } catch (InvalidInputException e) {
      throw new AbortFormatting(e);
    }
  }

  public boolean printModifiers() {
    boolean hasModifiers = false;
    try {
      boolean isFirstModifier = true;
      int currentTokenStartPosition = scanner.currentPosition;
      DartScanner.State scannerState = scanner.getState();
      boolean hasComment = false;
      String[] mods = MODIFIER_KEYWORDS;
      boolean[] visited = new boolean[mods.length];
      while ((currentToken = scanner.getNextToken()) != Token.EOS) {
        switch (currentToken) {
          case WHITESPACE:
            addDeleteEdit(
                scanner.getCurrentTokenStartPosition(),
                scanner.getCurrentTokenEndPosition());
            // TODO scan for newlines, print a newline if any && hasComment
            hasComment = false;
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case COMMENT:
            Style style = getCommentStyle(currentTokenStartPosition);
            switch (style) {
              case BLOCK:
                printBlockComment(false);
                break;
              case DART_DOC:
                printBlockComment(true);
                break;
              case END_OF_LINE:
                printLineComment();
                break;
            }
            hasComment = true;
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          default:
            String tokenString = scanner.getCurrentTokenString();
            int idx = Arrays.binarySearch(mods, tokenString);
            if (idx < 0 || visited[idx]) {
              if (hasModifiers) {
                space();
              }
              // step back one token
              scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
              scanner.restoreState(scannerState);
              return hasModifiers;
            }
            visited[idx] = true;
            hasModifiers = true;
            print(scanner.currentPosition - scanner.startPosition, !isFirstModifier);
            isFirstModifier = false;
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
        }
      }
    } catch (InvalidInputException ex) {
      // ignore it
    }
    return hasModifiers;
  }

  public void printNewLine() {
    this.printNewLine(scanner.getCurrentTokenEndPosition() + 1);
  }

  public void printNewLine(int insertPosition) {
    if (nlsTagCounter > 0) {
      return;
    }
    if (lastNumberOfNewLines >= 1) {
      // ensure that the scribe is at the beginning of a new line
      // only if no specific indentation has been previously set
      if (!preserveLineBreakIndentation) {
        column = 1;
      }
      preserveLineBreakIndentation = false;
      return;
    }
    addInsertEdit(insertPosition, lineSeparator);
    line += 1;
    lastNumberOfNewLines = 1;
    column = 1;
    needSpace = false;
    pendingSpace = false;
    preserveLineBreakIndentation = false;
    lastLineComment.contiguous = false;
  }

  public void printNextToken(Token expectedTokenType) {
    printNextToken(expectedTokenType, false);
  }

  public void printNextToken(Token expectedTokenType, boolean considerSpaceIfAny) {
    printNextToken(
        expectedTokenType,
        considerSpaceIfAny,
        PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION);
  }

  public void printNextToken(Token expectedTokenType, boolean considerSpaceIfAny, int emptyLineRules) {
    // Set brace flag, it's useful for the scribe while preserving line breaks
    printComment(CodeFormatter.K_UNKNOWN, NO_TRAILING_COMMENT, emptyLineRules);
    printNextTokenRaw(expectedTokenType, considerSpaceIfAny);
  }

  public void printNextToken(Token[] expectedTokenTypes) {
    printNextToken(expectedTokenTypes, false);
  }

  public void printNextToken(Token[] expectedTokenTypes, boolean considerSpaceIfAny) {
    printComment(CodeFormatter.K_UNKNOWN, NO_TRAILING_COMMENT);
    try {
      currentToken = scanner.getNextToken();
      if (Arrays.binarySearch(expectedTokenTypes, currentToken) < 0) {
        StringBuffer expectations = new StringBuffer(5);
        for (int i = 0; i < expectedTokenTypes.length; i++) {
          if (i > 0) {
            expectations.append(',');
          }
          expectations.append(expectedTokenTypes[i]);
        }
        throw new AbortFormatting(
            "unexpected token type, expecting:[" + expectations.toString() + "], actual:" + currentToken);//$NON-NLS-1$//$NON-NLS-2$
      }
      print(scanner.currentPosition - scanner.startPosition, considerSpaceIfAny);
    } catch (InvalidInputException e) {
      throw new AbortFormatting(e);
    }
  }

  /**
   * Prior to the final token of string interpolation it is not only illegal to have comments,
   * actually checking for the existence of a comment is harmful. It changes scanner state that we
   * don't have easy access to restore. So don't try to print comments when printing string
   * interpolation end tokens.
   */
  public void printNextTokenNoComment(Token expectedTokenType, boolean considerSpaceIfAny) {
    printNextTokenRaw(expectedTokenType, considerSpaceIfAny);
  }

  public void printQualifiedReference(int sourceEnd, boolean expectParenthesis) {
    int currentTokenStartPosition = scanner.currentPosition;
    DartScanner.State scannerState = scanner.getState();
    try {
      do {
        printComment(CodeFormatter.K_UNKNOWN, NO_TRAILING_COMMENT);
        switch (currentToken = scanner.getNextToken()) {
          case EOS:
            return;
          case WHITESPACE:
            addDeleteEdit(
                scanner.getCurrentTokenStartPosition(),
                scanner.getCurrentTokenEndPosition());
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case COMMENT:
            Style style = getCommentStyle(currentTokenStartPosition);
            switch (style) {
              case BLOCK:
                printBlockComment(false);
                break;
              case DART_DOC:
                printBlockComment(true);
                break;
              case END_OF_LINE:
                printLineComment();
                break;
            }
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case IDENTIFIER:
          case PERIOD:
            print(scanner.currentPosition - scanner.startPosition, false);
            currentTokenStartPosition = scanner.currentPosition;
            scannerState = scanner.getState();
            break;
          case RPAREN:
            if (expectParenthesis) {
              currentTokenStartPosition = scanner.startPosition;
              // use previous scannerState
            }
            // $FALL-THROUGH$ - fall through default case...
          default:
            scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
            scanner.restoreState(scannerState);
            return;
        }
      } while (scanner.currentPosition <= sourceEnd);
    } catch (InvalidInputException e) {
      throw new AbortFormatting(e);
    }
  }

  public void reset() {
    checkLineWrapping = true;
    line = 0;
    column = 1;
    editsIndex = 0;
    nlsTagCounter = 0;
  }

  /**
   * @param compilationUnitSource
   */
  public void resetScanner(char[] compilationUnitSource) {
    scanner.setSource(compilationUnitSource);
    scannerEndPosition = compilationUnitSource.length;
    scanner.resetTo(0, scannerEndPosition - 1);
    edits = new OptimizedReplaceEdit[INITIAL_SIZE];
    maxLines = lineEnds == null ? -1 : lineEnds.length - 1;
// this.scanner.lineEnds = this.lineEnds;
// this.scanner.linePtr = this.maxLines;
// initFormatterCommentParser();
  }

  public void space() {
    if (!needSpace) {
      return;
    }
    lastNumberOfNewLines = 0;
    pendingSpace = true;
    column++;
    needSpace = false;
  }

  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("(page width = " + pageWidth + ") - (tabChar = ");//$NON-NLS-1$//$NON-NLS-2$
    switch (tabChar) {
      case DefaultCodeFormatterOptions.TAB:
        stringBuffer.append("TAB");//$NON-NLS-1$
        break;
      case DefaultCodeFormatterOptions.SPACE:
        stringBuffer.append("SPACE");//$NON-NLS-1$
        break;
      default:
        stringBuffer.append("MIXED");//$NON-NLS-1$
    }
    stringBuffer.append(") - (tabSize = " + tabLength + ")")//$NON-NLS-1$//$NON-NLS-2$
    .append(lineSeparator).append(
        "(line = " + line + ") - (column = " + column + ") - (identationLevel = " + indentationLevel + ")") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    .append(lineSeparator).append(
        "(needSpace = " + needSpace + ") - (lastNumberOfNewLines = " + lastNumberOfNewLines + ") - (checkLineWrapping = " + checkLineWrapping + ")") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    .append(lineSeparator).append(
        "==================================================================================") //$NON-NLS-1$
    .append(lineSeparator);
    if (tabLength > 0) {
      printRule(stringBuffer);
    }
    return stringBuffer.toString();
  }

  public void unIndent() {
    indentationLevel -= indentationSize;
    numberOfIndentations--;
  }

  int getCurrentIndentation(char[] whitespaces, int offset) {
    if (whitespaces == null) {
      return offset;
    }
    int length = whitespaces.length;
    if (tabLength == 0) {
      return length;
    }
    int indentation = offset;
    for (int i = 0; i < length; i++) {
      char ch = whitespaces[i];
      switch (ch) {
        case '\t':
          int reminder = indentation % tabLength;
          if (reminder == 0) {
            indentation += tabLength;
          } else {
            indentation = (indentation / tabLength + 1) * tabLength;
          }
          break;
        case '\r':
        case '\n':
          indentation = 0;
          break;
        default:
          indentation++;
          break;
      }
    }
    return indentation;
  }

  int getCurrentIndentation(int start) {
    int linePtr = Arrays.binarySearch(lineEnds, start);
    if (linePtr < 0) {
      linePtr = -linePtr - 1;
    }
    int indentation = 0;
    int beginningOfLine = getLineEnd(linePtr) + 1;
    if (beginningOfLine == -1) {
      beginningOfLine = 0;
    }
    char[] source = scanner.source;

    for (int i = beginningOfLine; i < start; i++) {
      char currentCharacter = source[i];
      switch (currentCharacter) {
        case '\t':
          if (tabLength != 0) {
            int reminder = indentation % tabLength;
            if (reminder == 0) {
              indentation += tabLength;
            } else {
              indentation = (indentation / tabLength + 1) * tabLength;
            }
          }
          break;
        case '\r':
        case '\n':
          indentation = 0;
          break;
        case ' ':
          indentation++;
          break;
        default:
          return indentation;
      }
    }
    return indentation;
  }

  Alignment getMemberAlignment() {
    return memberAlignment;
  }

  boolean includesComments() {
    return (formatComments & CodeFormatter.F_INCLUDE_COMMENTS) != 0;
  }

  void printComment() {
    printComment(
        CodeFormatter.K_UNKNOWN,
        NO_TRAILING_COMMENT,
        PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION);
  }

  void printComment(int emptyLinesRules) {
    printComment(CodeFormatter.K_UNKNOWN, NO_TRAILING_COMMENT, emptyLinesRules);
  }

  void printComment(int kind, int trailing) {
    printComment(kind, trailing, PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION);
  }

  /*
   * Main method to print and format comments (dart doc, block and single line comments)
   */
  void printComment(int kind, int trailing, int emptyLinesRules) {
    final boolean rejectLineComment = kind == CodeFormatter.K_MULTI_LINE_COMMENT
        || kind == CodeFormatter.K_JAVA_DOC;
    final boolean rejectBlockComment = kind == CodeFormatter.K_SINGLE_LINE_COMMENT
        || kind == CodeFormatter.K_JAVA_DOC;
    final boolean rejectJavadocComment = kind == CodeFormatter.K_SINGLE_LINE_COMMENT
        || kind == CodeFormatter.K_MULTI_LINE_COMMENT;
    try {
      // if we have a space between two tokens we ensure it will be dumped in
      // the formatted string
      int currentTokenStartPosition = scanner.currentPosition;
      DartScanner.State state = scanner.getState();
      DartScanner.State tokenState = state;
      boolean hasComment = false;
      boolean hasLineComment = false;
      boolean hasWhitespaces = false;
      int lines = 0;
      while (true) {
        tokenState = scanner.getState();
        if ((currentToken = scanner.getNextToken()) == Token.EOS) {
          break;
        }
//        int foundTaskCount = scanner.foundTaskCount;
        int tokenStartPosition = scanner.getCurrentTokenStartPosition();
        switch (currentToken) {
          case WHITESPACE:
            int whitespacesEndPosition = scanner.getCurrentTokenEndPosition();
            char[] whiteSpaces;
            if (whitespacesEndPosition < tokenStartPosition) {
              whitespacesEndPosition++;
              whiteSpaces = scanner.getLastTokenString().toCharArray();
            } else {
              whiteSpaces = scanner.getCurrentTokenString().toCharArray();
            }
            lines = 0;
            for (int i = 0, max = whiteSpaces.length; i < max; i++) {
              switch (whiteSpaces[i]) {
                case '\r':
                  if (i + 1 < max) {
                    if (whiteSpaces[i + 1] == '\n') {
                      i++;
                    }
                  }
                  lines++;
                  break;
                case '\n':
                  lines++;
              }
            }
            // If following token is a line comment on the same line or the line
            // just after, then it might be not really formatted as a trailing
            // comment
            boolean realTrailing = trailing > NO_TRAILING_COMMENT;
            if (realTrailing
                && scanner.currentCharacter == '/'
                && (lines == 0 || lines == 1 && !hasLineComment
                    && trailing == IMPORT_TRAILING_COMMENT)) {
              // sometimes changing the trailing may not be the best idea
              // for complex trailing comment, it's basically a good idea
              boolean canChangeTrailing = (trailing & COMPLEX_TRAILING_COMMENT) != 0;
              // for basic trailing comment preceded by a line comment, then it
              // depends on the comments relative position
              // when following comment column (after having been rounded) is
              // below the preceding one,
              // then it becomes not a good idea to change the trailing flag
              if (trailing == BASIC_TRAILING_COMMENT && hasLineComment) {
                int currentCommentIndentation = getCurrentIndentation(whiteSpaces, 0);
                int relativeIndentation = currentCommentIndentation
                    - lastLineComment.currentIndentation;
                if (tabLength == 0) {
                  canChangeTrailing = relativeIndentation == 0;
                } else {
                  canChangeTrailing = relativeIndentation > -tabLength;
                }
              }
              // if the trailing can be changed, then look at the following
              // tokens
              if (canChangeTrailing) {
                int currentPosition = scanner.currentPosition;
                DartScanner.State cmtState = scanner.getState();
                if (scanner.getNextToken() == Token.COMMENT) {
                  if (getCommentStyle(currentPosition) == DartComment.Style.END_OF_LINE) {
                    realTrailing = !hasLineComment;
                    switch (scanner.getNextToken()) {
                      case COMMENT:
                        // at least two contiguous line comments
                        // the formatter should not consider comments as
                        // trailing ones
                        if (getCommentStyle(currentPosition) == DartComment.Style.END_OF_LINE) {
                          realTrailing = false;
                        }
                        break;
                      case WHITESPACE:
                        if (scanner.getNextToken() == Token.COMMENT) {
                          // at least two contiguous line comments
                          // the formatter should not consider comments as
                          // trailing ones
                          if (getCommentStyle(currentPosition) == DartComment.Style.END_OF_LINE) {
                            realTrailing = false;
                          }
                        }
                        break;
                    }
                  }
                }
                scanner.resetTo(currentPosition, scannerEndPosition - 1);
                scanner.restoreState(cmtState);
              }
            }
            // Look whether comments line may be contiguous or not.
            // Note that when preceding token is a comment line, then only one
            // line is enough to have an empty line as the line end is included
            // in the comment line.
            // If comments are contiguous, store the white spaces to be able to
            // compute the current comment indentation.
            if (lines > 1 || lines == 1 && hasLineComment) {
              lastLineComment.contiguous = false;
            }
            lastLineComment.leadingSpaces = whiteSpaces;
            lastLineComment.lines = lines;
            // Strategy to consume spaces and eventually leave at this stage
            // depends on the fact that a trailing comment is expected or not.
            if (realTrailing) {
              // if a line comment is consumed, no other comment can be on the
              // same line after
              if (hasLineComment) {
                if (lines >= 1) {
                  currentTokenStartPosition = tokenStartPosition;
                  state = tokenState;
                  preserveEmptyLines(lines, currentTokenStartPosition);
                  addDeleteEdit(currentTokenStartPosition, whitespacesEndPosition);
//                  scanner.resetTo(scanner.currentPosition,
//                      scannerEndPosition - 1);
                  return;
                }
                scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
                scanner.restoreState(state);
                return;
              }
              // if one or several new lines are consumed, following comments
              // cannot be considered as trailing ones
              if (lines >= 1) {
                if (hasComment) {
                  this.printNewLine(tokenStartPosition);
                }
                scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
                scanner.restoreState(state);
                return;
              }
              // delete consumed white spaces
              hasWhitespaces = true;
              currentTokenStartPosition = scanner.currentPosition;
              state = scanner.getState();
              addDeleteEdit(tokenStartPosition, whitespacesEndPosition);
            } else {
              if (lines == 0) {
                hasWhitespaces = true;
                if (hasLineComment
                    && emptyLinesRules != PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION) {
                  addReplaceEdit(
                      tokenStartPosition,
                      whitespacesEndPosition,
                      getPreserveEmptyLines(0, emptyLinesRules));
                } else {
                  addDeleteEdit(tokenStartPosition, whitespacesEndPosition);
                }
              } else if (hasLineComment) {
                useAlignmentBreakIndentation(emptyLinesRules);
                currentTokenStartPosition = tokenStartPosition;
                state = tokenState;
                preserveEmptyLines(lines, currentTokenStartPosition);
                addDeleteEdit(currentTokenStartPosition, whitespacesEndPosition);
              } else if (hasComment) {
                useAlignmentBreakIndentation(emptyLinesRules);
                if (lines == 1) {
                  printNewLine(tokenStartPosition);
                } else {
                  preserveEmptyLines(lines - 1, tokenStartPosition);
                }
                addDeleteEdit(tokenStartPosition, whitespacesEndPosition);
              } else if (lines != 0
                  && (!formatter.preferences.join_wrapped_lines
                      || formatter.preferences.number_of_empty_lines_to_preserve != 0 || blank_lines_between_import_groups > 0)) {
                addReplaceEdit(
                    tokenStartPosition,
                    whitespacesEndPosition,
                    getPreserveEmptyLines(lines - 1, emptyLinesRules));
              } else {
                useAlignmentBreakIndentation(emptyLinesRules);
                addDeleteEdit(tokenStartPosition, whitespacesEndPosition);
              }
            }
            currentTokenStartPosition = scanner.currentPosition;
            state = scanner.getState();
            break;
          case COMMENT:
            switch (getCommentStyle(tokenStartPosition)) {
              case END_OF_LINE:
//                if (useTags && editsEnabled) {
//                  boolean turnOff = false;
//                  if (foundTaskCount > 0) {
//                    setEditsEnabled(foundTaskCount);
//                    turnOff = true;
//                  } else if (tagsKind == currentToken
//                      && CharOperation.fragmentEquals(disablingTag,
//                          scanner.source, tokenStartPosition, true)) {
//                    editsEnabled = false;
//                    turnOff = true;
//                  }
//                  if (turnOff) {
//                    if (!editsEnabled && editsIndex > 1) {
//                      OptimizedReplaceEdit currentEdit = edits[editsIndex - 1];
//                      if (scanner.startPosition == currentEdit.offset
//                          + currentEdit.length) {
//                        printNewLinesBeforeDisablingComment();
//                      }
//                    }
//                  }
//                }
                if (rejectLineComment) {
                  break;
                }
                if (lines >= 1) {
                  if (lines > 1) {
                    preserveEmptyLines(lines - 1, scanner.getCurrentTokenStartPosition());
                  } else if (lines == 1) {
                    printNewLine(scanner.getCurrentTokenStartPosition());
                  }
                } else if (hasWhitespaces) {
                  space();
                }
                hasWhitespaces = false;
                printLineComment();
                currentTokenStartPosition = scanner.currentPosition;
                state = scanner.getState();
                hasLineComment = true;
                lines = 0;
//                if (useTags && !editsEnabled) {
//                  if (foundTaskCount > 0) {
//                    setEditsEnabled(foundTaskCount);
//                  } else if (tagsKind == currentToken) {
//                    editsEnabled = CharOperation.fragmentEquals(enablingTag,
//                        scanner.source, tokenStartPosition, true);
//                  }
//                }
                break;
              case BLOCK:
//                if (useTags && editsEnabled) {
//                  boolean turnOff = false;
//                  if (foundTaskCount > 0) {
//                    setEditsEnabled(foundTaskCount);
//                    turnOff = true;
//                  } else if (tagsKind == currentToken
//                      && CharOperation.fragmentEquals(disablingTag,
//                          scanner.source, tokenStartPosition, true)) {
//                    editsEnabled = false;
//                    turnOff = true;
//                  }
//                  if (turnOff) {
//                    if (!editsEnabled && editsIndex > 1) {
//                      OptimizedReplaceEdit currentEdit = edits[editsIndex - 1];
//                      if (scanner.startPosition == currentEdit.offset
//                          + currentEdit.length) {
//                        printNewLinesBeforeDisablingComment();
//                      }
//                    }
//                  }
//                }
                if (trailing > NO_TRAILING_COMMENT && lines >= 1) {
                  // a block comment on next line means that there's no trailing
                  // comment
//                  scanner.resetTo(scanner.getCurrentTokenStartPosition(),
//                      scannerEndPosition - 1);
                  return;
                }
                lastLineComment.contiguous = false;
                if (rejectBlockComment) {
                  break;
                }
                if (lines >= 1) {
                  if (lines > 1) {
                    preserveEmptyLines(lines - 1, scanner.getCurrentTokenStartPosition());
                  } else if (lines == 1) {
                    printNewLine(scanner.getCurrentTokenStartPosition());
                  }
                } else if (hasWhitespaces) {
                  space();
                }
                hasWhitespaces = false;
                printBlockComment(false);
                currentTokenStartPosition = scanner.currentPosition;
                state = scanner.getState();
                hasLineComment = false;
                hasComment = true;
                lines = 0;
//                if (useTags && !editsEnabled) {
//                  if (foundTaskCount > 0) {
//                    setEditsEnabled(foundTaskCount);
//                  } else if (tagsKind == currentToken) {
//                    editsEnabled = CharOperation.fragmentEquals(enablingTag,
//                        scanner.source, tokenStartPosition, true);
//                  }
//                }
                break;
              case DART_DOC:
//                if (useTags && editsEnabled && foundTaskCount > 0) {
//                  setEditsEnabled(foundTaskCount);
//                  if (!editsEnabled && editsIndex > 1) {
//                    OptimizedReplaceEdit currentEdit = edits[editsIndex - 1];
//                    if (scanner.startPosition == currentEdit.offset
//                        + currentEdit.length) {
//                      printNewLinesBeforeDisablingComment();
//                    }
//                  }
//                }
                if (trailing > NO_TRAILING_COMMENT) {
                  // a javadoc comment should not be considered as a trailing
                  // comment
//                  scanner.resetTo(scanner.getCurrentTokenStartPosition(),
//                      scannerEndPosition - 1);
                  return;
                }
                lastLineComment.contiguous = false;
                if (rejectJavadocComment) {
                  break;
                }
                if (lines >= 1) {
                  if (lines > 1) {
                    preserveEmptyLines(lines - 1, scanner.getCurrentTokenStartPosition());
                  } else if (lines == 1) {
                    printNewLine(scanner.getCurrentTokenStartPosition());
                  }
                } else if (hasWhitespaces) {
                  space();
                }
                hasWhitespaces = false;
                if (includesJavadocComments()) {
                  printJavadocComment(scanner.startPosition, scanner.currentPosition);
                } else {
                  printBlockComment(true);
                }
//                if (useTags && !editsEnabled && foundTaskCount > 0) {
//                  setEditsEnabled(foundTaskCount);
//                }
                printNewLine();
                currentTokenStartPosition = scanner.currentPosition;
                state = scanner.getState();
                hasLineComment = false;
                hasComment = true;
                lines = 0;
                break;
            }
            break;
          default:
            lastLineComment.contiguous = false;
            // step back one token
            scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
            scanner.restoreState(state);
            return;
        }
      }
    } catch (InvalidInputException e) {
      throw new AbortFormatting(e);
    }
  }

  void printComment(int kind, String source, int start, int end, int level) {

    // Set scanner
    resetScanner(source.toCharArray());
    scanner.resetTo(start, end);
    // Put back 3.4RC2 code => comment following line as it has an impact on
    // Linux tests
    // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=234336
    // TODO (frederic) Need more investigations and a better fix in
    // isAdaptableRegion(int) and adaptRegions()
    // this.scannerEndPosition = end;

    // Set indentation level
    numberOfIndentations = level;
    indentationLevel = level * indentationSize;
    column = indentationLevel + 1;

    // Print corresponding comment
    switch (kind) {
      case CodeFormatter.K_SINGLE_LINE_COMMENT:
        printComment(kind, NO_TRAILING_COMMENT);
        break;
      case CodeFormatter.K_MULTI_LINE_COMMENT:
        printComment(kind, NO_TRAILING_COMMENT);
        break;
      case CodeFormatter.K_JAVA_DOC:
        printJavadocComment(start, end);
        break;
    }
  }

  void printIndentationIfNecessary() {
    tempBuffer.setLength(0);
    printIndentationIfNecessary(tempBuffer);
    if (tempBuffer.length() > 0) {
      addInsertEdit(scanner.getCurrentTokenStartPosition(), tempBuffer.toString());
      pendingSpace = false;
    }
  }

  /*
   * Print and formats a javadoc comments
   */
  void printJavadocComment(int start, int end) {
    DartCore.notYetImplemented();
    printBlockComment(false);
//    int lastIndentationLevel = indentationLevel;
    // try {
    // // parse the comment on the fly
    // this.scanner.resetTo(start, end - 1);
    // if (!this.formatterCommentParser.parse(start, end - 1)) {
    // // problem occurred while parsing the javadoc, early abort formatting
    // return;
    // }
    //
    // FormatJavadoc javadoc = (FormatJavadoc)
    // this.formatterCommentParser.docComment;
    //
    // // handle indentation
    // if (this.indentationLevel != 0) {
    // printIndentationIfNecessary();
    // }
    //
    // // handle pending space if any
    // if (this.pendingSpace) {
    //        addInsertEdit(start, " "); //$NON-NLS-1$
    // }
    //
    // if (javadoc.blocks == null) {
    // // no FormatJavadocTags in this this javadoc
    // return;
    // }
    //
    // // init properly
    // this.needSpace = false;
    // this.pendingSpace = false;
    // int length = javadoc.blocks.length;
    //
    // // format empty lines between before the first block
    // FormatJavadocBlock previousBlock = javadoc.blocks[0];
    // this.lastNumberOfNewLines = 0;
    // int currentLine = this.line;
    // int firstBlockStart = previousBlock.sourceStart;
    // printIndentationIfNecessary(null);
    // this.column += JAVADOC_HEADER_LENGTH; // consider that the header is
    // // already scanned
    //
    // // If there are several blocks in the javadoc
    // int index = 1;
    // if (length > 1) {
    // // format the description if any
    // if (previousBlock.isDescription()) {
    // printJavadocBlock(previousBlock);
    // FormatJavadocBlock block = javadoc.blocks[index++];
    // int newLines =
    // this.formatter.preferences.comment_insert_empty_line_before_root_tags
    // ? 2 : 1;
    // printJavadocGapLines(
    // previousBlock.sourceEnd + 1,
    // block.sourceStart - 1,
    // newLines,
    // this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment,
    // false, null);
    // previousBlock = block;
    // }
    //
    // // format all tags but the last one composing this comment
    // while (index < length) {
    // printJavadocBlock(previousBlock);
    // FormatJavadocBlock block = javadoc.blocks[index++];
    // printJavadocGapLines(
    // previousBlock.sourceEnd + 1,
    // block.sourceStart - 1,
    // 1,
    // this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment,
    // false, null);
    // previousBlock = block;
    // }
    // }
    //
    // // format the last block
    // printJavadocBlock(previousBlock);
    //
    // // format the header and footer empty spaces
    // int newLines =
    // (this.formatter.preferences.comment_new_lines_at_javadoc_boundaries &&
    // (this.line > currentLine || javadoc.isMultiLine()))
    // ? 1 : 0;
    // printJavadocGapLines(
    // javadoc.textStart,
    // firstBlockStart - 1,
    // newLines,
    // this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment,
    // false, null);
    // printJavadocGapLines(
    // previousBlock.sourceEnd + 1,
    // javadoc.textEnd,
    // newLines,
    // this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment,
    // true, null);
    // } finally {
    // // reset the scanner
    // this.scanner.resetTo(end, this.scannerEndPosition - 1);
    // this.needSpace = false;
    // this.indentationLevel = lastIndentationLevel;
    // this.lastNumberOfNewLines = 0;
    // }
  }

  void redoAlignment(AlignmentException e) {
    if (e.relativeDepth > 0) { // if exception targets a distinct context
      e.relativeDepth--; // record fact that current context got traversed
      currentAlignment = currentAlignment.enclosing; // pop
                                                     // currentLocation
      throw e; // rethrow
    }
    // reset scribe/scanner to restart at this given location
    resetAt(currentAlignment.location);
    scanner.resetTo(currentAlignment.location.inputOffset, scanner.source.length - 1);
    scanner.restoreState(currentAlignment.location.scannerState);
    // clean alignment chunkKind so it will think it is a new chunk again
    currentAlignment.chunkKind = 0;
  }

  void redoMemberAlignment(AlignmentException e) {
    // reset scribe/scanner to restart at this given location
    resetAt(memberAlignment.location);
    scanner.resetTo(memberAlignment.location.inputOffset, scanner.source.length - 1);
    scanner.restoreState(memberAlignment.location.scannerState);
    // clean alignment chunkKind so it will think it is a new chunk again
    memberAlignment.chunkKind = 0;
  }

  void setHeaderComment(int position) {
    DartCore.notYetImplemented();
//    this.headerEndPosition = position;
  }

  void setIncludeComments(boolean on) {
    if (on) {
      formatComments |= CodeFormatter.F_INCLUDE_COMMENTS;
    } else {
      formatComments &= ~CodeFormatter.F_INCLUDE_COMMENTS;
    }
  }

  void setIndentation(int level, int n) {
    indentationLevel = level + n * indentationSize;
    numberOfIndentations = indentationLevel / indentationSize;
  }

  /*
   * Search whether a region overlap edit(s) at its start and/or at its end. If so, modify the
   * concerned edits to keep only the modifications which are inside the given region. The edit
   * modification is done as follow: 1) start it from the region start if it overlaps the region's
   * start 2) end it at the region end if it overlaps the region's end 3) remove from the
   * replacement string the number of lines which are outside the region: before when overlapping
   * region's start and after when overlapping region's end. Note that the trailing indentation of
   * the replacement string is not kept when the region's end is overlapped because it's always
   * outside the region.
   */
  private int adaptEdit(OptimizedReplaceEdit[] sortedEdits, int start, int regionStart,
      int regionEnd) {
    int initialStart = start == -1 ? 0 : start;
    int bottom = initialStart, top = sortedEdits.length - 1;
    int topEnd = top;
    int i = 0;
    OptimizedReplaceEdit edit = null;
    int overlapIndex = -1;

    // Look for an edit overlapping the region start
    while (bottom <= top) {
      i = bottom + (top - bottom) / 2;
      edit = sortedEdits[i];
      int editStart = edit.offset;
      int editEnd = editStart + edit.length;
      if (editStart > regionStart) { // the edit starts after the region's
        // start
        // => no possible overlap of region's start
        // top = i - 1;
        if (editStart > regionEnd) { // the edit starts after the region's end
          // => no possible overlap of region's end
          topEnd = top;
        }
      } else {
        if (editEnd < regionStart) { // the edit ends before the region's start
          // => no possible overlap of region's start
          bottom = i + 1;
        } else {
          // Count the lines of the edit which are outside the region
          int linesOutside = 0;
          StringBuffer spacesOutside = new StringBuffer();
          scanner.resetTo(editStart, editEnd - 1);
          linesOutside = scanner.countLinesBetween(editStart, regionStart - 1);
          int charCount = scanner.charsAfterLastLineEnd();
          while (charCount-- > 0) {
            spacesOutside.append(' ');
          }

          // Restart the edit at the beginning of the line where the region
          // start
          edit.offset = regionStart;
          int editLength = edit.length;
          edit.length -= edit.offset - editStart;

          // Cut replacement string if necessary
          int length = edit.replacement.length();
          if (length > 0) {

            // Count the lines in replacement string
            int linesReplaced = 0;
            for (int idx = 0; idx < length; idx++) {
              if (edit.replacement.charAt(idx) == '\n') {
                linesReplaced++;
              }
            }

            // If the edit was a replacement but become an insertion due to the
            // length reduction
            // and if the edit finishes just before the region starts and if
            // there's no line to replace
            // then there's no replacement to do...
            if (editLength > 0 && edit.length == 0 && editEnd == regionStart && linesReplaced == 0
                && linesOutside == 0) {
              edit.offset = -1;
            } else {

              // As the edit starts outside the region, remove first lines from
              // edit string if any
              if (linesReplaced > 0) {
                int linesCount = linesOutside >= linesReplaced ? linesReplaced : linesOutside;
                if (linesCount > 0) {
                  int idx = 0;
                  loop : while (idx < length) {
                    char ch = edit.replacement.charAt(idx);
                    switch (ch) {
                      case '\n':
                        linesCount--;
                        if (linesCount == 0) {
                          idx++;
                          break loop;
                        }
                        break;
                      case '\r':
                      case ' ':
                      case '\t':
                        break;
                      default:
                        break loop;
                    }
                    idx++;
                  }
                  // Compare spaces outside the region and the beginning
                  // of the replacement string to remove the common part
                  int spacesOutsideLength = spacesOutside.length();
                  int replacementStart = idx;
                  for (int o = 0, r = 0; o < spacesOutsideLength && r < length - idx; o++) {
                    char rch = edit.replacement.charAt(idx + r);
                    char och = spacesOutside.charAt(o);
                    if (rch == och) {
                      replacementStart++;
                      r++;
                    } else if (rch == '\t' && tabLength > 0 && och == ' ') {
                      if ((o + 1) % tabLength == 0) {
                        replacementStart++;
                        r++;
                      }
                    } else {
                      break;
                    }
                  }
                  // Update the replacement string
                  if (replacementStart > length || replacementStart == length
                      && spacesOutsideLength > 0) {
                    edit.offset = -1;
                  } else if (spacesOutsideLength == 0 && replacementStart == length) {
                    edit.replacement = ""; //$NON-NLS-1$
                  } else {
                    edit.replacement = edit.replacement.substring(replacementStart);
                  }
                }
              }
            }
          }
          overlapIndex = i;
          break;
        }
      }
    }
    int validIndex = overlapIndex != -1 ? overlapIndex : bottom;

    // Look for an edit overlapping the region end
    if (overlapIndex != -1) {
      bottom = overlapIndex;
    }
    while (bottom <= topEnd) {
      i = bottom + (topEnd - bottom) / 2;
      edit = sortedEdits[i];
      int editStart = edit.offset;
      int editEnd = editStart + edit.length;
      if (regionEnd < editStart) { // the edit starts after the region's end =>
        // no possible overlap of region's end
        topEnd = i - 1;
      } else if (regionEnd == editStart) { // special case when the edit starts
        // just after the region's end...
        // ...we got the last index of the edit inside the region
        topEnd = i - 1;
        // this last edit is valid only if it's an insertion and if it has
        // indentation
        if (edit.length == 0) {
          int nrLength = 0;
          int rLength = edit.replacement.length();
          if (nrLength < rLength) {
            int ch = edit.replacement.charAt(nrLength);
            loop : while (nrLength < rLength) {
              switch (ch) {
                case ' ':
                case '\t':
                  nrLength++;
                  break;
                default:
                  break loop;
              }
            }
          }
          if (nrLength > 0) {
            topEnd++;
            if (nrLength < rLength) {
              edit.replacement = edit.replacement.substring(0, nrLength);
            }
          }
        }
        break;
      } else if (editEnd <= regionEnd) { // the edit ends before the region's
        // end => no possible overlap of
        // region's end
        bottom = i + 1;
      } else {
        // Count the lines of the edit which are outside the region
        int linesOutside = 0;
        scanner.resetTo(editStart, editEnd - 1);
        linesOutside = scanner.countLinesBetween(regionEnd, editEnd - 1);

        // Cut replacement string if necessary
        int length = edit.replacement.length();
        if (length > 0) {

          // Count the lines in replacement string
          int linesReplaced = 0;
          for (int idx = 0; idx < length; idx++) {
            if (edit.replacement.charAt(idx) == '\n') {
              linesReplaced++;
            }
          }

          // Set the replacement string to the number of missing new lines
          // As the end of the edit is out of the region, the possible trailing
          // indentation should not be added...
          if (linesReplaced == 0) {
            edit.replacement = ""; //$NON-NLS-1$
          } else {
            int linesCount = linesReplaced > linesOutside ? linesReplaced - linesOutside : 0;
            if (linesCount == 0) {
              edit.replacement = ""; //$NON-NLS-1$
            } else {
              edit.replacement = getNewLineString(linesCount);
            }
          }
        }
        edit.length = regionEnd - editStart;

        // We got the last edit of the regions, give up
        topEnd = i;
        break;
      }
    }

    // Set invalid all edits outside the region
    for (int e = initialStart; e < validIndex; e++) {
      sortedEdits[e].offset = -1;
    }

    // Return the index of next edit to look at
    return topEnd + 1;
  }

  /*
   * Adapt edits to regions.
   * 
   * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=234583" for more details
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void adaptEdits() {

    // See if adapting edits is really necessary
    int max = regions.length;
    if (max == 1) {
      if (regions[0].getOffset() == 0 && regions[0].getLength() == scannerEndPosition) {
        // No need to adapt as the regions covers the whole source
        return;
      }
    }

    // Sort edits
    OptimizedReplaceEdit[] sortedEdits = new OptimizedReplaceEdit[editsIndex];
    System.arraycopy(edits, 0, sortedEdits, 0, editsIndex);
    Arrays.sort(sortedEdits, new Comparator() {
      @Override
      public int compare(Object o1, Object o2) {
        OptimizedReplaceEdit edit1 = (OptimizedReplaceEdit) o1;
        OptimizedReplaceEdit edit2 = (OptimizedReplaceEdit) o2;
        return edit1.offset - edit2.offset;
      }
    });

    // Adapt overlapping edits
    int currentEdit = -1;
    for (int i = 0; i < max; i++) {
      IRegion region = adaptedRegions[i];
      int offset = region.getOffset();
      int length = region.getLength();

      // modify overlapping edits on the region (if any)
      int index = adaptEdit(sortedEdits, currentEdit, offset, offset + length);
      if (index != -1) {
        currentEdit = index;
      }
    }

    // Set invalid all edits outside the region
    if (currentEdit != -1) {
      int length = sortedEdits.length;
      for (int e = currentEdit; e < length; e++) {
        sortedEdits[e].offset = -1;
      }
    }
  }

  /**
   * This method will adapt the selected regions if needed. If a region should be adapted (see
   * isAdaptableRegion(IRegion)) retrieve correct upper and lower bounds and replace the region.
   */
  private void adaptRegions() {
    int max = regions.length;
    if (max == 1) {
      // It's not necessary to adapt the single region which covers all the
      // source
      if (regions[0].getOffset() == 0 && regions[0].getLength() == scannerEndPosition) {
        adaptedRegions = regions;
        return;
      }
    }
    adaptedRegions = new IRegion[max];
    int commentIndex = 0;
    for (int i = 0; i < max; i++) {
      IRegion aRegion = regions[i];
      int offset = aRegion.getOffset();
      int length = aRegion.getLength();

      // First look if the region starts or ends inside a comment
      int index = getCommentIndex(commentIndex, offset);
      int adaptedOffset = offset;
      int adaptedLength = length;
      if (index >= 0) {
        // the offset of the region is inside a comment => restart the region
        // from the comment start
        adaptedOffset = commentPositions[index][0];
        if (adaptedOffset >= 0) {
          // adapt only javadoc or block comments. Since fix for bug
          // https://bugs.eclipse.org/bugs/show_bug.cgi?id=238210
          // edits in line comments only concerns whitespaces hence can be
          // treated as edits in code
          adaptedLength = length + offset - adaptedOffset;
          commentIndex = index;
        }
      }
      index = getCommentIndex(commentIndex, offset + length - 1);
      if (index >= 0 && commentPositions[index][0] >= 0) {
        // only javadoc or block comment
        // the region end is inside a comment => set the region end at the
        // comment end
        int commentEnd = commentPositions[index][1];
        if (commentEnd < 0) {
          commentEnd = -commentEnd;
        }
        adaptedLength = commentEnd - adaptedOffset;
        commentIndex = index;
      }
      if (adaptedLength != length) {
        // adapt the region and jump to next one
        adaptedRegions[i] = new Region(adaptedOffset, adaptedLength);
      } else {
        adaptedRegions[i] = aRegion;
      }
    }
  }

  private final void addDeleteEdit(int start, int end) {
    if (edits.length == editsIndex) {
      // resize
      resize();
    }
    addOptimizedReplaceEdit(start, end - start + 1, Util.EMPTY_STRING);
  }

  private final void addOptimizedReplaceEdit(int offset, int length, String replacement) {
    if (!editsEnabled) {
      if (previousDisabledEdit != null && previousDisabledEdit.offset == offset) {
        replacement = previousDisabledEdit.replacement;
      }
      previousDisabledEdit = null;
      if (replacement.indexOf(lineSeparator) >= 0) {
        if (length == 0 || printNewLinesCharacters(offset, length)) {
          previousDisabledEdit = new OptimizedReplaceEdit(offset, length, replacement);
        }
      }
      return;
    }
    if (editsIndex > 0) {
      // try to merge last two edits
      final OptimizedReplaceEdit previous = edits[editsIndex - 1];
      final int previousOffset = previous.offset;
      final int previousLength = previous.length;
      final int endOffsetOfPreviousEdit = previousOffset + previousLength;
      final int replacementLength = replacement.length();
      final String previousReplacement = previous.replacement;
      final int previousReplacementLength = previousReplacement.length();
      if (previousOffset == offset && previousLength == length
          && (replacementLength == 0 || previousReplacementLength == 0)) {
        if (currentAlignment != null) {
          final Location location = currentAlignment.location;
          if (location.editsIndex == editsIndex) {
            location.editsIndex--;
            location.textEdit = previous;
          }
        }
        editsIndex--;
        return;
      }
      if (endOffsetOfPreviousEdit == offset) {
        if (length != 0) {
          if (replacementLength != 0) {
            edits[editsIndex - 1] = new OptimizedReplaceEdit(previousOffset, previousLength
                + length, previousReplacement + replacement);
          } else if (previousLength + length == previousReplacementLength) {
            // check the characters. If they are identical, we can get rid of
            // the previous edit
            boolean canBeRemoved = true;
            loop : for (int i = previousOffset; i < previousOffset + previousReplacementLength; i++) {
              if (scanner.source[i] != previousReplacement.charAt(i - previousOffset)) {
                edits[editsIndex - 1] = new OptimizedReplaceEdit(
                    previousOffset,
                    previousReplacementLength,
                    previousReplacement);
                canBeRemoved = false;
                break loop;
              }
            }
            if (canBeRemoved) {
              if (currentAlignment != null) {
                final Location location = currentAlignment.location;
                if (location.editsIndex == editsIndex) {
                  location.editsIndex--;
                  location.textEdit = previous;
                }
              }
              editsIndex--;
            }
          } else {
            edits[editsIndex - 1] = new OptimizedReplaceEdit(previousOffset, previousLength
                + length, previousReplacement);
          }
        } else {
          if (replacementLength != 0) {
            edits[editsIndex - 1] = new OptimizedReplaceEdit(
                previousOffset,
                previousLength,
                previousReplacement + replacement);
          }
        }
      } else if (offset + length == previousOffset
          && previousLength + length == replacementLength + previousReplacementLength) {
        // check if both edits corresponds to the orignal source code
        boolean canBeRemoved = true;
        String totalReplacement = replacement + previousReplacement;
        loop : for (int i = 0; i < previousLength + length; i++) {
          if (scanner.source[i + offset] != totalReplacement.charAt(i)) {
            edits[editsIndex - 1] = new OptimizedReplaceEdit(
                offset,
                previousLength + length,
                totalReplacement);
            canBeRemoved = false;
            break loop;
          }
        }
        if (canBeRemoved) {
          if (currentAlignment != null) {
            final Location location = currentAlignment.location;
            if (location.editsIndex == editsIndex) {
              location.editsIndex--;
              location.textEdit = previous;
            }
          }
          editsIndex--;
        }
      } else {
        edits[editsIndex++] = new OptimizedReplaceEdit(offset, length, replacement);
      }
    } else {
      edits[editsIndex++] = new OptimizedReplaceEdit(offset, length, replacement);
    }
  }

  private Token consumeInvalidToken(int end) {
    int startOfBadToken = scanner.startPosition;
    scanner.resetTo(scanner.startPosition, end);
    // In case of invalid unicode character, consume the current backslash
    // character before continuing
//    if (scanner.currentCharacter == '\\') {
//      // this only works because getNextChar() doesn't do tokenization
//      scanner.currentPosition = scanner.startPosition + 1;
//    }
    char ch = (char) scanner.getNextChar();
    if (scanner.atEnd()) {
      // avoid infinite loop
      return Token.ILLEGAL;
    }
    int previousPosition = scanner.currentPosition;
    while (!scanner.atEnd() && ch != '*' && !Character.isWhitespace(ch)) {
      previousPosition = scanner.currentPosition;
      ch = (char) scanner.getNextChar();
    }
    // restore last whitespace
    scanner.resetTo(previousPosition, scanner.getEndPos());
    scanner.startPosition = startOfBadToken;
    return Token.ILLEGAL;
  }

  /*
   * Returns the index of the comment including the given offset position starting the search from
   * the given start index.
   * 
   * @param start The start index for the research
   * 
   * @param position The position
   * 
   * @return The index of the comment if the given position is located inside it, -1 otherwise
   */
  private int getCommentIndex(int start, int position) {
    int commentsLength = commentPositions == null ? 0 : commentPositions.length;
    if (commentsLength == 0) {
      return -1;
    }
    if (position == 0) {
      if (commentsLength > 0 && commentPositions[0][0] == 0) {
        return 0;
      }
      return -1;
    }
    int bottom = start, top = commentsLength - 1;
    int i = 0;
    int[] comment = null;
    while (bottom <= top) {
      i = bottom + (top - bottom) / 2;
      comment = commentPositions[i];
      int commentStart = comment[0];
      if (commentStart < 0) {
        commentStart = -commentStart;
      }
      if (position < commentStart) {
        top = i - 1;
      } else {
        int commentEnd = comment[1];
        if (commentEnd < 0) {
          commentEnd = -commentEnd;
        }
        if (position >= commentEnd) {
          bottom = i + 1;
        } else {
          return i;
        }
      }
    }
    return -1;
  }

  private DartComment.Style getCommentStyle(int start) {
    return scanner.getCommentStyle(start);
  }

  private int getCurrentCommentIndentation(int start) {
    int linePtr = -Arrays.binarySearch(lineEnds, start);
    int indentation = 0;
    int beginningOfLine = getLineEnd(linePtr - 1) + 1;
    if (beginningOfLine == -1) {
      beginningOfLine = 0;
    }
    int currentStartPosition = start;
    char[] source = scanner.source;

    // find the position of the beginning of the line containing the comment
    while (beginningOfLine > currentStartPosition) {
      if (linePtr > 0) {
        beginningOfLine = getLineEnd(--linePtr) + 1;
      } else {
        beginningOfLine = 0;
        break;
      }
    }
    for (int i = beginningOfLine; i < currentStartPosition; i++) {
      char currentCharacter = source[i];
      switch (currentCharacter) {
        case '\t':
          if (tabLength != 0) {
            int reminder = indentation % tabLength;
            if (reminder == 0) {
              indentation += tabLength;
            } else {
              indentation = (indentation / tabLength + 1) * tabLength;
            }
          }
          break;
        case '\r':
        case '\n':
          indentation = 0;
          break;
        default:
          indentation++;
          break;
      }
    }
    return indentation;
  }

  private String getNewLineString(int linesCount) {
    int length = newEmptyLines.length;
    if (linesCount > length) {
      System.arraycopy(newEmptyLines, 0, newEmptyLines = new String[linesCount + 10], 0, length);
    }
    String newLineString = newEmptyLines[linesCount - 1];
    if (newLineString == null) {
      tempBuffer.setLength(0);
      for (int j = 0; j < linesCount; j++) {
        tempBuffer.append(lineSeparator);
      }
      newLineString = tempBuffer.toString();
      newEmptyLines[linesCount - 1] = newLineString;
    }
    return newLineString;
  }

  /*
   * Preserve empty lines depending on given count and preferences.
   */
  private String getPreserveEmptyLines(int count, int emptyLinesRules) {
    if (count == 0) {
      int currentIndentationLevel = indentationLevel;
      int useAlignmentBreakIndentation = useAlignmentBreakIndentation(emptyLinesRules);
      switch (useAlignmentBreakIndentation) {
        case PRESERVE_EMPTY_LINES_DO_NOT_USE_ANY_INDENTATION:
          return Util.EMPTY_STRING;
        default:
          // Return the new indented line
          StringBuffer buffer = new StringBuffer(getNewLine());
          printIndentationIfNecessary(buffer);
          if (useAlignmentBreakIndentation == PRESERVE_EMPTY_LINES_USE_TEMPORARY_INDENTATION) {
            indentationLevel = currentIndentationLevel;
          }
          return buffer.toString();
      }
    }
    if (blank_lines_between_import_groups >= 0) {
      useAlignmentBreakIndentation(emptyLinesRules);
      return getEmptyLines(blank_lines_between_import_groups);
    }
    if (formatter.preferences.number_of_empty_lines_to_preserve != 0) {
      useAlignmentBreakIndentation(emptyLinesRules);
      int linesToPreserve = Math.min(count, formatter.preferences.number_of_empty_lines_to_preserve);
      return getEmptyLines(linesToPreserve);
    }
    return getNewLine();
  }

// private int getTextLength(FormatJavadocBlock block, FormatJavadocText text) {
//
// // Special case for immutable tags
// if (text.isImmutable()) {
// this.scanner.resetTo(text.sourceStart, text.sourceEnd);
// int textLength = 0;
// while (!this.scanner.atEnd()) {
// try {
// int token = this.scanner.getNextToken();
// if (token == TerminalTokens.TokenNameWHITESPACE) {
// if (CharOperation.indexOf('\n', this.scanner.source,
// this.scanner.startPosition, this.scanner.currentPosition) >= 0) {
// textLength = 0;
// this.scanner.getNextChar();
// if (this.scanner.currentCharacter == '*') {
// this.scanner.getNextChar();
// if (this.scanner.currentCharacter != ' ') {
// textLength++;
// }
// } else {
// textLength++;
// }
// continue;
// }
// }
// textLength += (this.scanner.atEnd() ? this.scanner.eofPosition
// : this.scanner.currentPosition) - this.scanner.startPosition;
// } catch (InvalidInputException e) {
// // maybe an unterminated string or comment
// textLength += (this.scanner.atEnd() ? this.scanner.eofPosition
// : this.scanner.currentPosition) - this.scanner.startPosition;
// }
// }
// return textLength;
// }
//
// // Simple for one line tags
// if (block.isOneLineTag()) {
// return text.sourceEnd - text.sourceStart + 1;
// }
//
// // Find last line
// int startLine = Util.getLineNumber(text.sourceStart, this.lineEnds, 0,
// this.maxLines);
// int endLine = startLine;
// int previousEnd = -1;
// for (int i = 0; i <= text.separatorsPtr; i++) {
// int end = (int) (text.separators[i] >>> 32);
// endLine = Util.getLineNumber(end, this.lineEnds, endLine - 1,
// this.maxLines);
// if (endLine > startLine) {
// return previousEnd - text.sourceStart + 1;
// }
// previousEnd = end;
// }
//
// // This was a one line text
// return text.sourceEnd - text.sourceStart + 1;
// }

  private void handleLineTooLongSmartly() {
    // search for closest breakable alignment, using tiebreak rules
    // look for outermost breakable one
    int relativeDepth = 0, outerMostDepth = -1;
    Alignment targetAlignment = currentAlignment;
    int previousKind = -1;
    int insideMessage = 0;
    boolean insideStringConcat = false;
    while (targetAlignment != null) {
      boolean couldBreak = targetAlignment.tieBreakRule == Alignment.R_OUTERMOST
          || !insideStringConcat && insideMessage > 0
          && targetAlignment.kind == Alignment.MESSAGE_ARGUMENTS
          && (!targetAlignment.wasReset() || previousKind != Alignment.MESSAGE_SEND);
      if (couldBreak && targetAlignment.couldBreak()) {
        outerMostDepth = relativeDepth;
      }
      switch (targetAlignment.kind) {
        case Alignment.MESSAGE_ARGUMENTS:
        case Alignment.MESSAGE_SEND:
          insideMessage++;
          break;
        case Alignment.STRING_CONCATENATION:
          insideStringConcat = true;
          break;
      }
      previousKind = targetAlignment.kind;
      targetAlignment = targetAlignment.enclosing;
      relativeDepth++;
    }
    if (outerMostDepth >= 0) {
      throw new AlignmentException(AlignmentException.LINE_TOO_LONG, outerMostDepth);
    }
    // look for innermost breakable one
    relativeDepth = 0;
    targetAlignment = currentAlignment;
    AlignmentException alignmentException = null;
    int msgArgsDepth = -1;
    while (targetAlignment != null) {
      if (targetAlignment.kind == Alignment.MESSAGE_ARGUMENTS) {
        msgArgsDepth = relativeDepth;
      }
      if (alignmentException == null) {
        if (targetAlignment.couldBreak()) {
          // do not throw the exception immediately to have a chance to reset
          // previously broken alignments (see bug 203588)
          alignmentException = new AlignmentException(
              AlignmentException.LINE_TOO_LONG,
              relativeDepth);
          if (insideStringConcat) {
            throw alignmentException;
          }
        }
      } else if (targetAlignment.wasSplit) {
        // reset the nearest already broken outermost alignment.
        // Note that it's not done twice to avoid infinite loop while raising
        // the exception on an innermost alignment...
        if (!targetAlignment.wasReset()) {
          targetAlignment.reset();
          if (msgArgsDepth > alignmentException.relativeDepth) {
            alignmentException.relativeDepth = msgArgsDepth;
          }
          throw alignmentException;
        }
      }
      targetAlignment = targetAlignment.enclosing;
      relativeDepth++;
    }
    if (alignmentException != null) {
      throw alignmentException;
    }
    // did not find any breakable location - proceed
    if (currentAlignment != null) {
      currentAlignment.blockAlign = false;
      currentAlignment.tooLong = true;
    }
  }

  /*
   * Check if there is a NLS tag on this line. If yes, return true, returns false otherwise.
   */
  private boolean hasNLSTag(int sourceStart) {
    // search the last comment where commentEnd < current lineEnd
    if (lineEnds == null) {
      return false;
    }
    int index = Arrays.binarySearch(lineEnds, sourceStart);
    int currentLineEnd = getLineEnd(-index);
    if (currentLineEnd != -1) {
      int commentIndex = getCommentIndex(currentLineEnd);
      if (commentIndex < 0) {
        commentIndex = -commentIndex - 2;
      }
      if (commentIndex >= 0 && commentIndex < commentPositions.length) {
        int start = commentPositions[commentIndex][0];
        if (start < 0) {
          start = -start;
          // check that we are on the same line
          int lineIndexForComment = Arrays.binarySearch(lineEnds, start);
          if (lineIndexForComment == index) {
            return CharOperation.indexOf(
                Scanner.TAG_PREFIX,
                scanner.source,
                true,
                start,
                currentLineEnd) != -1;
          }
        }
      }
    }
    return false;
  }

  private boolean includesBlockComments() {
    DartCore.notYetImplemented();
    return true;
    // return ((this.formatComments & INCLUDE_BLOCK_COMMENTS) ==
    // INCLUDE_BLOCK_COMMENTS && this.headerEndPosition <
    // this.scanner.currentPosition)
    // || (this.formatter.preferences.comment_format_header &&
    // this.headerEndPosition >= this.scanner.currentPosition);
  }

  private boolean includesJavadocComments() {
    DartCore.notYetImplemented();
    return true;
    // return ((this.formatComments & INCLUDE_JAVA_DOC) == INCLUDE_JAVA_DOC &&
    // this.headerEndPosition < this.scanner.currentPosition)
    // || (this.formatter.preferences.comment_format_header &&
    // this.headerEndPosition >= this.scanner.currentPosition);
  }

  private boolean includesLineComments() {
    DartCore.notYetImplemented();
    return true;
    // return ((this.formatComments & INCLUDE_LINE_COMMENTS) ==
    // INCLUDE_LINE_COMMENTS && this.headerEndPosition <
    // this.scanner.currentPosition)
    // || (this.formatter.preferences.comment_format_header &&
    // this.headerEndPosition >= this.scanner.currentPosition);
  }

  private void initFormatterCommentParser() {
    DartCore.notYetImplemented();
    // if (this.formatterCommentParser == null) {
    // this.formatterCommentParser = new FormatterCommentParser(
    // this.scanner.sourceLevel);
    // }
    // this.formatterCommentParser.scanner.setSource(this.scanner.source);
    // this.formatterCommentParser.source = this.scanner.source;
    // this.formatterCommentParser.scanner.lineEnds = this.lineEnds;
    // this.formatterCommentParser.scanner.linePtr = this.maxLines;
    // this.formatterCommentParser.parseHtmlTags =
    // this.formatter.preferences.comment_format_html;
  }

  private void initializeScanner(DefaultCodeFormatterOptions preferences) {
//    useTags = preferences.use_tags;
//    this.tagsKind = 0;
//    char[][] taskTags = null;
//    if (useTags) {
//      disablingTag = preferences.disabling_tag;
//      enablingTag = preferences.enabling_tag;
//      if (disablingTag == null) {
//        if (enablingTag != null) {
//          taskTags = new char[][]{enablingTag};
//        }
//      } else if (enablingTag == null) {
//        taskTags = new char[][]{disablingTag};
//      } else {
//        taskTags = new char[][]{disablingTag, enablingTag};
//      }
//    }
//    if (taskTags != null) {
//      loop : for (int i = 0, length = taskTags.length; i < length; i++) {
//        if (taskTags[i].length > 2 && taskTags[i][0] == '/') {
//          switch (taskTags[i][1]) {
//            case '/':
//              this.tagsKind = TerminalTokens.TokenNameCOMMENT_LINE;
//              break loop;
//            case '*':
//              if (taskTags[i][2] != '*') {
//                this.tagsKind = TerminalTokens.TokenNameCOMMENT_BLOCK;
//                break loop;
//              }
//              break;
//          }
//        }
//      }
//    }
    scanner = new Scanner();
// this.scanner = new Scanner(true, true, false/* nls */,
// sourceLevel/* sourceLevel */, taskTags, null/* taskPriorities */, true/*
// taskCaseSensitive */);
    editsEnabled = true;
  }

  private boolean isMeaningfulEdit(OptimizedReplaceEdit edit) {
    final int editLength = edit.length;
    final int editReplacementLength = edit.replacement.length();
    final int editOffset = edit.offset;
    if (editReplacementLength != 0 && editLength == editReplacementLength) {
      for (int i = editOffset, max = editOffset + editLength; i < max; i++) {
        if (scanner.source[i] != edit.replacement.charAt(i - editOffset)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean isOnFirstColumn(int start) {
    if (start == 0) {
      return true;
    }
    if (lineEnds == null) {
      return false;
    }
    int index = Arrays.binarySearch(lineEnds, start);
    // we want the line end of the previous line
    int previousLineEnd = getLineEnd(-index - 1);
    return previousLineEnd != -1 && previousLineEnd == start - 1;
  }

  private void preserveEmptyLines(int count, int insertPosition) {
    if (count > 0) {
      if (blank_lines_between_import_groups >= 0) {
        printEmptyLines(blank_lines_between_import_groups, insertPosition);
      } else if (formatter.preferences.number_of_empty_lines_to_preserve != 0) {
        int linesToPreserve = Math.min(
            count,
            formatter.preferences.number_of_empty_lines_to_preserve);
        printEmptyLines(linesToPreserve, insertPosition);
      } else {
        printNewLine(insertPosition);
      }
    }
  }

  private void print(int length, boolean considerSpaceIfAny) {
    if (checkLineWrapping && length + column > pageWidth) {
      handleLineTooLong();
    }
    lastNumberOfNewLines = 0;
    if (indentationLevel != 0) {
      printIndentationIfNecessary();
    }
    if (considerSpaceIfAny) {
      space();
    }
    if (pendingSpace) {
      addInsertEdit(scanner.getCurrentTokenStartPosition(), " "); //$NON-NLS-1$
    }
    pendingSpace = false;
    needSpace = false;
    column += length;
    needSpace = true;
  }

  private void printBlockComment(boolean isJavadoc) {
    int currentTokenStartPosition = scanner.getCurrentTokenStartPosition();
    int currentTokenEndPosition = scanner.getCurrentTokenEndPosition() + 1;
    boolean includesBlockComments = !isJavadoc && includesBlockComments();

    DartScanner.State state = scanner.getState();
    scanner.resetTo(currentTokenStartPosition, currentTokenEndPosition - 1);
    scanner.restoreState(state);
    int currentCharacter;
    boolean isNewLine = false;
    int start = currentTokenStartPosition;
    int nextCharacterStart = currentTokenStartPosition;
    int previousStart = currentTokenStartPosition;
    boolean onFirstColumn = isOnFirstColumn(start);

    boolean indentComment = false;
    if (indentationLevel != 0) {
      if (isJavadoc || !formatter.preferences.never_indent_block_comments_on_first_column
          || !onFirstColumn) {
        indentComment = true;
        printIndentationIfNecessary();
      }
    }
    if (pendingSpace) {
      addInsertEdit(currentTokenStartPosition, " "); //$NON-NLS-1$
    }
    needSpace = false;
    pendingSpace = false;

    int commentColumn = column;
    if (includesBlockComments) {
      if (printBlockComment(currentTokenStartPosition, currentTokenEndPosition)) {
        scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
        scanner.restoreState(state);
        try {
          scanner.getNextToken();
        } catch (InvalidInputException ignore) {
          // ignore it
        }
        return;
      }
    }

    int currentIndentationLevel = indentationLevel;
    if (commentColumn - 1 > indentationLevel) {
      indentationLevel = commentColumn - 1;
    }
    int currentCommentIndentation = onFirstColumn ? 0 : getCurrentCommentIndentation(start);
    boolean formatComment = isJavadoc && (formatComments & CodeFormatter.K_JAVA_DOC) != 0
        || !isJavadoc && (formatComments & CodeFormatter.K_MULTI_LINE_COMMENT) != 0;

    try {
      while (nextCharacterStart <= currentTokenEndPosition
          && (currentCharacter = scanner.getNextChar()) != -1) {
        nextCharacterStart = scanner.currentPosition;

        switch (currentCharacter) {
          case '\r':
            start = previousStart;
            isNewLine = true;
            if (scanner.getNextChar('\n')) {
              currentCharacter = '\n';
              nextCharacterStart = scanner.currentPosition;
            }
            break;
          case '\n':
            start = previousStart;
            isNewLine = true;
            nextCharacterStart = scanner.currentPosition;
            break;
          default:
            if (isNewLine) {
              column = 1;
              line++;
              isNewLine = false;

              boolean addSpace = false;
              if (onFirstColumn) {
                if (formatComment) {
                  if (Character.isWhitespace((char) currentCharacter)) {
                    int previousStartPosition = scanner.currentPosition;
                    while (currentCharacter != -1 && currentCharacter != '\r'
                        && currentCharacter != '\n'
                        && Character.isWhitespace((char) currentCharacter)) {
                      previousStart = nextCharacterStart;
                      previousStartPosition = scanner.currentPosition;
                      currentCharacter = scanner.getNextChar();
                      nextCharacterStart = scanner.currentPosition;
                    }
                    if (currentCharacter == '\r' || currentCharacter == '\n') {
                      nextCharacterStart = previousStartPosition;
                    }
                  }
                  if (currentCharacter != '\r' && currentCharacter != '\n') {
                    addSpace = true;
                  }
                }
              } else {
                if (Character.isWhitespace((char) currentCharacter)) {
                  int previousStartPosition = scanner.currentPosition;
                  int currentIndentation = 0;
                  loop : while (currentCharacter != -1 && currentCharacter != '\r'
                      && currentCharacter != '\n'
                      && Character.isWhitespace((char) currentCharacter)) {
                    if (currentIndentation >= currentCommentIndentation) {
                      break loop;
                    }
                    previousStart = nextCharacterStart;
                    previousStartPosition = scanner.currentPosition;
                    switch (currentCharacter) {
                      case '\t':
                        if (tabLength != 0) {
                          int reminder = currentIndentation % tabLength;
                          if (reminder == 0) {
                            currentIndentation += tabLength;
                          } else {
                            currentIndentation = (currentIndentation / tabLength + 1) * tabLength;
                          }
                        }
                        break;
                      default:
                        currentIndentation++;
                    }
                    currentCharacter = scanner.getNextChar();
                    nextCharacterStart = scanner.currentPosition;
                  }
                  if (currentCharacter == '\r' || currentCharacter == '\n') {
                    nextCharacterStart = previousStartPosition;
                  }
                }
                if (formatComment) {
                  int previousStartTemp = previousStart;
                  int nextCharacterStartTemp = nextCharacterStart;
                  while (currentCharacter != -1 && currentCharacter != '\r'
                      && currentCharacter != '\n'
                      && Character.isWhitespace((char) currentCharacter)) {
                    previousStart = nextCharacterStart;
                    currentCharacter = scanner.getNextChar();
                    nextCharacterStart = scanner.currentPosition;
                  }
                  if (currentCharacter == '*') {
                    addSpace = true;
                  } else {
                    previousStart = previousStartTemp;
                    nextCharacterStart = nextCharacterStartTemp;
                  }
                  scanner.resetTo(scanner.startPosition, nextCharacterStart);
//                  scanner.currentPosition = nextCharacterStart;
                }
              }
              String replacement;
              if (indentComment) {
                tempBuffer.setLength(0);
                tempBuffer.append(lineSeparator);
                if (indentationLevel > 0) {
                  printIndentationIfNecessary(tempBuffer);
                }
                if (addSpace) {
                  tempBuffer.append(' ');
                }
                replacement = tempBuffer.toString();
              } else {
                replacement = addSpace ? lineSeparatorAndSpace : lineSeparator;
              }
              addReplaceEdit(start, previousStart - 1, replacement);
            } else {
              column += nextCharacterStart - previousStart;
            }
        }
        previousStart = nextCharacterStart;
        scanner.resetTo(nextCharacterStart, scanner.getEndPos() - 1);
//        scanner.resetTo(scanner.startPosition, nextCharacterStart);
//        scanner.currentPosition = nextCharacterStart;
      }
    } finally {
      indentationLevel = currentIndentationLevel;
    }
    lastNumberOfNewLines = 0;
    needSpace = false;
    scanner.resetTo(currentTokenStartPosition, scannerEndPosition - 1);
    scanner.restoreState(state);
    try {
      scanner.getNextToken();
    } catch (InvalidInputException ignore) {
      // ignore it
    }
  }

  private boolean printBlockComment(int currentTokenStartPosition, int currentTokenEndPosition) {
    // Compute indentation
    int maxColumn = formatter.preferences.comment_line_length + 1;
    int indentLevel = indentationLevel;
    int indentations = numberOfIndentations;
    switch (tabChar) {
      case DefaultCodeFormatterOptions.TAB:
        switch (tabLength) {
          case 0:
            indentationLevel = 0;
            column = 1;
            numberOfIndentations = 0;
            break;
          case 1:
            indentationLevel = column - 1;
            numberOfIndentations = indentationLevel;
            break;
          default:
            indentationLevel = column / tabLength * tabLength;
            column = indentationLevel + 1;
            numberOfIndentations = indentationLevel / tabLength;
        }
        break;
      case DefaultCodeFormatterOptions.MIXED:
        if (tabLength == 0) {
          indentationLevel = 0;
          column = 1;
          numberOfIndentations = 0;
        } else {
          indentationLevel = column - 1;
          numberOfIndentations = indentationLevel / tabLength;
        }
        break;
      case DefaultCodeFormatterOptions.SPACE:
        if (indentationSize == 0) {
          indentationLevel = 0;
          column = 1;
          numberOfIndentations = 0;
        } else {
          indentationLevel = column - 1;
        }
        break;
    }

    // Consume the comment prefix
    DartComment.Style style = scanner.getCommentStyle();
    blockCommentBuffer.setLength(0);
    scanner.getNextChar();
    scanner.getNextChar();
    column += 2;
    if (style == DartComment.Style.DART_DOC) {
      // TODO remove this once dart doc is supported
      scanner.getNextChar();
      column += 1;
    }
    scanner.skipComments = true;
    blockCommentTokensBuffer.setLength(0);
    int editStart = scanner.currentPosition;
    int editEnd = -1;

    // Consume text token per token
    Token previousToken = null;
    boolean newLine = false;
    boolean multiLines = false;
    boolean hasMultiLines = false;
    boolean hasTokens = false;
    boolean bufferHasTokens = false;
    boolean bufferHasNewLine = false;
    boolean lineHasTokens = false;
    int hasTextOnFirstLine = 0;
    boolean firstWord = true;
    boolean clearBlankLines = formatter.preferences.comment_clear_blank_lines_in_block_comment;
    boolean joinLines = formatter.preferences.join_lines_in_comments;
    boolean newLinesAtBoundaries = formatter.preferences.comment_new_lines_at_block_boundaries;
    int scannerLine = Util.getLineNumber(scanner.currentPosition, lineEnds, 0, maxLines);
    int firstLine = scannerLine;
    int lineNumber = scannerLine;
    int lastTextLine = -1;
    while (!scanner.atEnd()) {

      // Consume token
      Token token;
      try {
        token = scanner.getNextToken();
      } catch (InvalidInputException iie) {
        token = consumeInvalidToken(currentTokenEndPosition - 1);
        newLine = false;
      }

      // Look at specific tokens
      boolean insertSpace = previousToken == Token.WHITESPACE && (!firstWord || !hasTokens);
      boolean isTokenStar = false;
      switch (token) {
        case WHITESPACE:
          if (blockCommentTokensBuffer.length() > 0) {
            if (hasTextOnFirstLine == 1 && multiLines) {
              printBlockCommentHeaderLine(blockCommentBuffer);
              hasTextOnFirstLine = -1;
            }
            blockCommentBuffer.append(blockCommentTokensBuffer);
            column += blockCommentTokensBuffer.length();
            blockCommentTokensBuffer.setLength(0);
            bufferHasTokens = true;
            bufferHasNewLine = false;
          }
          if (previousToken == null) {
            // do not remember the first whitespace
            previousToken = SKIP_FIRST_WHITESPACE_TOKEN;
          } else {
            previousToken = token;
          }
          lineNumber = Util.getLineNumber(scanner.currentPosition, lineEnds, scannerLine > 1
              ? scannerLine - 2 : 0, maxLines);
          if (lineNumber > scannerLine) {
            hasMultiLines = true;
            newLine = true;
          }
          scannerLine = lineNumber;
          continue;
        case MUL:
          isTokenStar = true;
          lineNumber = Util.getLineNumber(scanner.currentPosition, lineEnds, scannerLine > 1
              ? scannerLine - 2 : 0, maxLines);
          if (lineNumber == firstLine && previousToken == SKIP_FIRST_WHITESPACE_TOKEN) {
            blockCommentBuffer.append(' ');
          }
          previousToken = token;
          if (scanner.currentCharacter == '/') {
            editEnd = scanner.startPosition - 1;
            // Add remaining buffered tokens
            if (blockCommentTokensBuffer.length() > 0) {
              blockCommentBuffer.append(blockCommentTokensBuffer);
              column += blockCommentTokensBuffer.length();
            }
            // end of comment
            if (newLinesAtBoundaries) {
              if (multiLines || hasMultiLines) {
                blockCommentBuffer.append(lineSeparator);
                column = 1;
                printIndentationIfNecessary(blockCommentBuffer);
              }
            }
            blockCommentBuffer.append(' ');
            column += BLOCK_FOOTER_LENGTH + 1;
            scanner.getNextChar(); // reach the end of scanner
            continue;
          }
          if (newLine) {
            scannerLine = lineNumber;
            newLine = false;
            continue;
          }
          break;
        case ASSIGN_MUL:
          if (newLine) {
            scanner.resetTo(scanner.startPosition, currentTokenEndPosition - 1);
            scanner.getNextChar(); // consume the multiply
            previousToken = Token.MUL;
            scannerLine = Util.getLineNumber(scanner.currentPosition, lineEnds, scannerLine > 1
                ? scannerLine - 2 : 0, maxLines);
            continue;
          }
          break;
        case SUB:
        case DEC:
          if (previousToken == null) {
            // Do not format comment starting with /*-
            indentationLevel = indentLevel;
            numberOfIndentations = indentations;
            lastNumberOfNewLines = 0;
            needSpace = false;
            scanner.skipComments = false;
            scanner.resetTo(currentTokenStartPosition, currentTokenEndPosition - 1);
            return false;
          }
          break;
        default:
          // do nothing
          break;
      }

      // Look at gap and insert corresponding lines if necessary
      int linesGap;
      int max;
      lineNumber = Util.getLineNumber(scanner.currentPosition, lineEnds, scannerLine > 1
          ? scannerLine - 2 : 0, maxLines);
      if (lastTextLine == -1) {
        linesGap = newLinesAtBoundaries ? lineNumber - firstLine : 0;
        max = 0;
      } else {
        linesGap = lineNumber - lastTextLine;
//        if (token == TerminalTokens.TokenNameAT && linesGap == 1) {
//          // insert one blank line before root tags
//          linesGap = 2;
//        }
        max = joinLines && lineHasTokens ? 1 : 0;
      }
      if (linesGap > max) {
        if (clearBlankLines) {
          // TODO (frederic) see if there's a bug for the unremoved blank line
          // for root tags
//          if (token == TerminalTokens.TokenNameAT) {
//            linesGap = 1;
//          } else {
          linesGap = max == 0 || !joinLines ? 1 : 0;
//          }
        }
        for (int i = 0; i < linesGap; i++) {
          // Add remaining buffered tokens
          if (blockCommentTokensBuffer.length() > 0) {
            if (hasTextOnFirstLine == 1) {
              printBlockCommentHeaderLine(blockCommentBuffer);
              hasTextOnFirstLine = -1;
            }
            blockCommentBuffer.append(blockCommentTokensBuffer);
            blockCommentTokensBuffer.setLength(0);
            bufferHasTokens = true;
          }
          blockCommentBuffer.append(lineSeparator);
          column = 1;
          printIndentationIfNecessary(blockCommentBuffer);
          blockCommentBuffer.append(BLOCK_LINE_PREFIX);
          column += BLOCK_LINE_PREFIX_LENGTH;
          firstWord = true;
          multiLines = true;
          bufferHasNewLine = true;
        }
        insertSpace = insertSpace && linesGap == 0;
      }
      if (newLine) {
        lineHasTokens = false;
      }

      // Increment column
      int tokenStart = scanner.getCurrentTokenStartPosition();
      int tokenLength = scanner.atEnd() ? 0 : scanner.currentPosition - tokenStart;
      hasTokens = true;
      if (!isTokenStar) {
        lineHasTokens = true;
      }
      if (hasTextOnFirstLine == 0 && !isTokenStar) {
        if (firstLine == lineNumber) {
          hasTextOnFirstLine = 1;
          column++; // include first space
        } else {
          hasTextOnFirstLine = -1;
        }
      }
      int lastColumn = column + blockCommentTokensBuffer.length() + tokenLength;
      if (insertSpace) {
        lastColumn++;
      }

      // Append next token inserting a new line if max line is reached
      if (lineHasTokens && !firstWord && lastColumn > maxColumn) {
        String tokensString = blockCommentTokensBuffer.toString().trim();
        int tokensStringLength = tokensString.length();
        // not enough space on the line
        if (hasTextOnFirstLine == 1) {
          printBlockCommentHeaderLine(blockCommentBuffer);
        }
        if (indentationLevel + tokensStringLength + tokenLength > maxColumn) {
          // there won't be enough room even if we break the line before the
          // buffered tokens
          // So add the buffered tokens now
          blockCommentBuffer.append(blockCommentTokensBuffer);
          column += blockCommentTokensBuffer.length();
          blockCommentTokensBuffer.setLength(0);
          bufferHasNewLine = false;
          bufferHasTokens = true;
        }
        if (bufferHasTokens && !bufferHasNewLine) {
          blockCommentBuffer.append(lineSeparator);
          column = 1;
          printIndentationIfNecessary(blockCommentBuffer);
          blockCommentBuffer.append(BLOCK_LINE_PREFIX);
          column += BLOCK_LINE_PREFIX_LENGTH;
        }
        if (blockCommentTokensBuffer.length() > 0) {
          blockCommentBuffer.append(tokensString);
          column += tokensStringLength;
          blockCommentTokensBuffer.setLength(0);
        }
        blockCommentBuffer.append(scanner.source, tokenStart, tokenLength);
        bufferHasTokens = true;
        bufferHasNewLine = false;
        column += tokenLength;
        multiLines = true;
        hasTextOnFirstLine = -1;
      } else {
        // append token to the line
        if (insertSpace) {
          blockCommentTokensBuffer.append(' ');
        }
        blockCommentTokensBuffer.append(scanner.source, tokenStart, tokenLength);
      }
      previousToken = token;
      newLine = false;
      firstWord = false;
      scannerLine = lineNumber;
      lastTextLine = lineNumber;
    }

    // Replace block comment text
    if (nlsTagCounter == 0 || !multiLines) {
      if (hasTokens || multiLines) {
        StringBuffer replacement;
        if (hasTextOnFirstLine == 1) {
          blockCommentTokensBuffer.setLength(0);
          replacement = blockCommentTokensBuffer;
          if (hasMultiLines || multiLines) {
            int col = column;
            replacement.append(lineSeparator);
            column = 1;
            printIndentationIfNecessary(replacement);
            replacement.append(BLOCK_LINE_PREFIX);
            column = col;
          } else if (blockCommentBuffer.length() == 0 || blockCommentBuffer.charAt(0) != ' ') {
            replacement.append(' ');
          }
          replacement.append(blockCommentBuffer);
        } else {
          replacement = blockCommentBuffer;
        }
        addReplaceEdit(editStart, editEnd, replacement.toString());
      }
    }

    // Reset
    indentationLevel = indentLevel;
    numberOfIndentations = indentations;
    lastNumberOfNewLines = 0;
    needSpace = false;
    scanner.resetTo(currentTokenEndPosition, scannerEndPosition - 1);
    scanner.skipComments = false;
    return true;
  }

  private void printBlockCommentHeaderLine(StringBuffer buffer) {
    if (!formatter.preferences.comment_new_lines_at_block_boundaries) {
      buffer.insert(0, ' ');
      column++;
    } else if (buffer.length() == 0) {
      buffer.append(lineSeparator);
      column = 1;
      printIndentationIfNecessary(buffer);
      buffer.append(BLOCK_LINE_PREFIX);
      column += BLOCK_LINE_PREFIX_LENGTH;
    } else {
      tempBuffer.setLength(0);
      tempBuffer.append(lineSeparator);
      column = 1;
      printIndentationIfNecessary(tempBuffer);
      tempBuffer.append(BLOCK_LINE_PREFIX);
      column += BLOCK_LINE_PREFIX_LENGTH;
      buffer.insert(0, tempBuffer.toString());
    }
  }

  /*
   * prints a code snippet
   */
  private void printCodeSnippet(int startPosition, int endPosition, int linesGap) {
    DartCore.notYetImplemented();
    // String snippet = new String(this.scanner.source, startPosition,
    // endPosition
    // - startPosition + 1);
    //
    // // 1 - strip content prefix (@see JavaDocRegion#preprocessCodeSnippet)
    // int firstLine = Util.getLineNumber(startPosition, this.lineEnds, 0,
    // this.maxLines) - 1;
    // int lastLine = Util.getLineNumber(endPosition, this.lineEnds, firstLine >
    // 1
    // ? firstLine - 2 : 0, this.maxLines) - 1;
    // this.codeSnippetBuffer.setLength(0);
    // if (firstLine == lastLine && linesGap == 0) {
    // this.codeSnippetBuffer.append(snippet);
    // } else {
    // boolean hasCharsAfterStar = false;
    // if (linesGap == 0) {
    // this.codeSnippetBuffer.append(this.scanner.source, startPosition,
    // this.lineEnds[firstLine] + 1 - startPosition);
    // firstLine++;
    // }
    // int initialLength = this.codeSnippetBuffer.length();
    // for (int currentLine = firstLine; currentLine <= lastLine; currentLine++)
    // {
    // this.scanner.resetTo(this.lineEnds[currentLine - 1] + 1,
    // this.lineEnds[currentLine]);
    // int lineStart = this.scanner.currentPosition;
    // boolean hasStar = false;
    // loop : while (!this.scanner.atEnd()) {
    // char ch = (char) this.scanner.getNextChar();
    // switch (ch) {
    // case ' ':
    // case '\t':
    // case '\u000c':
    // break;
    // case '\r':
    // case '\n':
    // break loop;
    // case '*':
    // hasStar = true;
    // break loop;
    // default:
    // if (ScannerHelper.isWhitespace(ch)) {
    // break;
    // }
    // break loop;
    // }
    // }
    // if (hasStar) {
    // lineStart = this.scanner.currentPosition;
    // if (!hasCharsAfterStar && !this.scanner.atEnd()) {
    // char ch = (char) this.scanner.getNextChar();
    // boolean atEnd = this.scanner.atEnd();
    // switch (ch) {
    // case ' ':
    // case '\t':
    // case '\u000c':
    // break;
    // case '\r':
    // case '\n':
    // atEnd = true;
    // break;
    // default:
    // if (!ScannerHelper.isWhitespace(ch)) {
    // if (hasStar) {
    // // A non whitespace character is just after the star
    // // then we need to restart from the beginning without
    // // consuming the space after the star
    // hasCharsAfterStar = true;
    // currentLine = firstLine - 1;
    // this.codeSnippetBuffer.setLength(initialLength);
    // continue;
    // }
    // }
    // break;
    // }
    // if (!hasCharsAfterStar && !atEnd) {
    // // Until then, there's always a whitespace after each star
    // // of the comment, hence we need to consume it as it will
    // // be rewritten while reindenting the snippet lines
    // lineStart = this.scanner.currentPosition;
    // }
    // }
    // }
    // int end = currentLine == lastLine ? endPosition
    // : this.lineEnds[currentLine];
    // this.codeSnippetBuffer.append(this.scanner.source, lineStart, end + 1
    // - lineStart);
    // }
    // }
    //
    // // 2 - convert HTML to Java (@see JavaDocRegion#convertHtml2Java)
    // HTMLEntity2JavaReader reader = new HTMLEntity2JavaReader(new
    // StringReader(
    // this.codeSnippetBuffer.toString()));
    // char[] buf = new char[this.codeSnippetBuffer.length()]; // html2text
    // never
    // // gets longer, only
    // // shorter!
    // String convertedSnippet;
    // try {
    // int read = reader.read(buf);
    // convertedSnippet = new String(buf, 0, read);
    // } catch (IOException e) {
    // // should not happen
    // CommentFormatterUtil.log(e);
    // return;
    // }
    //
    // // 3 - format snippet (@see JavaDocRegion#formatCodeSnippet)
    // // include comments in case of line comments are present in the snippet
    // String formattedSnippet = convertedSnippet;
    // Map options = this.formatter.preferences.getMap();
    // if (this.scanner.sourceLevel > ClassFileConstants.JDK1_3) {
    // options.put(JavaCore.COMPILER_SOURCE,
    // CompilerOptions.versionFromJdkLevel(this.scanner.sourceLevel));
    // }
    // TextEdit edit = CommentFormatterUtil.format2(CodeFormatter.K_UNKNOWN
    // | CodeFormatter.F_INCLUDE_COMMENTS, convertedSnippet, 0,
    // this.lineSeparator, options);
    // if (edit == null) {
    // // 3.a - not a valid code to format, keep initial buffer
    // formattedSnippet = this.codeSnippetBuffer.toString();
    // } else {
    // // 3.b - valid code formatted
    // // 3.b.i - get the result
    // formattedSnippet = CommentFormatterUtil.evaluateFormatterEdit(
    // convertedSnippet, edit, null);
    //
    // // 3.b.ii- convert back to HTML (@see JavaDocRegion#convertJava2Html)
    // Java2HTMLEntityReader javaReader = new Java2HTMLEntityReader(
    // new StringReader(formattedSnippet));
    // buf = new char[256];
    // this.codeSnippetBuffer.setLength(0);
    // int l;
    // try {
    // do {
    // l = javaReader.read(buf);
    // if (l != -1)
    // this.codeSnippetBuffer.append(buf, 0, l);
    // } while (l > 0);
    // formattedSnippet = this.codeSnippetBuffer.toString();
    // } catch (IOException e) {
    // // should not happen
    // CommentFormatterUtil.log(e);
    // return;
    // }
    // }
    //
    // // 4 - add the content prefix (@see JavaDocRegion#postprocessCodeSnippet)
    // this.codeSnippetBuffer.setLength(0);
    // ILineTracker tracker = new DefaultLineTracker();
    // this.column = 1;
    // printIndentationIfNecessary(this.codeSnippetBuffer); // append
    // indentation
    // this.codeSnippetBuffer.append(BLOCK_LINE_PREFIX);
    // String linePrefix = this.codeSnippetBuffer.toString();
    // this.codeSnippetBuffer.setLength(0);
    // String replacement = formattedSnippet;
    // tracker.set(formattedSnippet);
    // int numberOfLines = tracker.getNumberOfLines();
    // if (numberOfLines > 1) {
    // int lastLineOffset = -1;
    // for (int i = 0; i < numberOfLines - 1; i++) {
    // if (i > 0)
    // this.codeSnippetBuffer.append(linePrefix);
    // try {
    // lastLineOffset = tracker.getLineOffset(i + 1);
    // this.codeSnippetBuffer.append(formattedSnippet.substring(
    // tracker.getLineOffset(i), lastLineOffset));
    // } catch (BadLocationException e) {
    // // should not happen
    // CommentFormatterUtil.log(e);
    // return;
    // }
    // }
    // this.codeSnippetBuffer.append(linePrefix);
    // this.codeSnippetBuffer.append(formattedSnippet.substring(lastLineOffset));
    // replacement = this.codeSnippetBuffer.toString();
    // }
    //
    // // 5 - replace old text with the formatted snippet
    // addReplaceEdit(startPosition, endPosition, replacement);
  }

  private void printEmptyLines(int linesNumber, int insertPosition) {
    final String buffer = getEmptyLines(linesNumber);
    if (Util.EMPTY_STRING == buffer) {
      return;
    }
    addInsertEdit(insertPosition, buffer);
  }

  private void printIndentationIfNecessary(StringBuffer buffer) {
    switch (tabChar) {
      case DefaultCodeFormatterOptions.TAB:
        boolean useTabsForLeadingIndents = useTabsOnlyForLeadingIndents;
        int numberOfLeadingIndents = numberOfIndentations;
        int indentationsAsTab = 0;
        if (useTabsForLeadingIndents) {
          while (column <= indentationLevel) {
            if (tabLength > 0 && indentationsAsTab < numberOfLeadingIndents) {
              if (buffer != null) {
                buffer.append('\t');
              }
              indentationsAsTab++;
              // amount of space
              int complement = tabLength - (column - 1) % tabLength;
              column += complement;
            } else {
              if (buffer != null) {
                buffer.append(' ');
              }
              column++;
            }
            needSpace = false;
          }
        } else if (tabLength > 0) {
          while (column <= indentationLevel) {
            if (buffer != null) {
              buffer.append('\t');
            }
            // amount of space
            int complement = tabLength - (column - 1) % tabLength;
            column += complement;
            needSpace = false;
          }
        }
        break;
      case DefaultCodeFormatterOptions.SPACE:
        while (column <= indentationLevel) {
          if (buffer != null) {
            buffer.append(' ');
          }
          column++;
          needSpace = false;
        }
        break;
      case DefaultCodeFormatterOptions.MIXED:
        useTabsForLeadingIndents = useTabsOnlyForLeadingIndents;
        numberOfLeadingIndents = numberOfIndentations;
        indentationsAsTab = 0;
        if (useTabsForLeadingIndents) {
          final int columnForLeadingIndents = numberOfLeadingIndents * indentationSize;
          while (column <= indentationLevel) {
            if (column <= columnForLeadingIndents) {
              if (tabLength > 0 && column - 1 + tabLength <= indentationLevel) {
                if (buffer != null) {
                  buffer.append('\t');
                }
                column += tabLength;
              } else if (column - 1 + indentationSize <= indentationLevel) {
                // print one indentation
                // note that this.indentationSize > 0 when entering in the
                // following loop
                // hence this.column will be incremented and then avoid endless
                // loop
                for (int i = 0, max = indentationSize; i < max; i++) {
                  if (buffer != null) {
                    buffer.append(' ');
                  }
                  column++;
                }
              } else {
                if (buffer != null) {
                  buffer.append(' ');
                }
                column++;
              }
            } else {
              for (int i = column, max = indentationLevel; i <= max; i++) {
                if (buffer != null) {
                  buffer.append(' ');
                }
                column++;
              }
            }
            needSpace = false;
          }
        } else {
          while (column <= indentationLevel) {
            if (tabLength > 0 && column - 1 + tabLength <= indentationLevel) {
              if (buffer != null) {
                buffer.append('\t');
              }
              column += tabLength;
            } else if (indentationSize > 0 && column - 1 + indentationSize <= indentationLevel) {
              // print one indentation
              for (int i = 0, max = indentationSize; i < max; i++) {
                if (buffer != null) {
                  buffer.append(' ');
                }
                column++;
              }
            } else {
              if (buffer != null) {
                buffer.append(' ');
              }
              column++;
            }
            needSpace = false;
          }
        }
        break;
    }
  }

  // private void printJavadocBlock(FormatJavadocBlock block) {
  // if (block == null)
  // return;
  //
  // // Init positions
  // int previousEnd = block.tagEnd;
  // int maxNodes = block.nodesPtr;
  //
  // // Compute indentation
  // boolean headerLine = block.isHeaderLine() && this.lastNumberOfNewLines ==
  // 0;
  // int maxColumn = this.formatter.preferences.comment_line_length + 1;
  // if (headerLine) {
  // maxColumn++;
  // }
  //
  // // format tag section if necessary
  // if (!block.isInlined()) {
  // this.lastNumberOfNewLines = 0;
  // }
  // if (block.isDescription()) {
  // if (!block.isInlined()) {
  // this.commentIndentation = null;
  // }
  // } else {
  // int tagLength = previousEnd - block.sourceStart + 1;
  // this.column += tagLength;
  // if (!block.isInlined()) {
  // boolean indentRootTags =
  // this.formatter.preferences.comment_indent_root_tags
  // && !block.isInDescription();
  // int commentIndentationLevel = 0;
  // if (indentRootTags) {
  // commentIndentationLevel = tagLength + 1;
  // boolean indentParamTag =
  // this.formatter.preferences.comment_indent_parameter_description
  // && block.isInParamTag();
  // if (indentParamTag) {
  // commentIndentationLevel += this.indentationSize;
  // }
  // }
  // setCommentIndentation(commentIndentationLevel);
  // }
  // FormatJavadocReference reference = block.reference;
  // if (reference != null) {
  // // format reference
  // printJavadocBlockReference(block, reference);
  // previousEnd = reference.sourceEnd;
  // }
  //
  // // Nothing else to do if the tag has no node
  // if (maxNodes < 0) {
  // if (block.isInlined()) {
  // this.column++;
  // }
  // return;
  // }
  // }
  //
  // // tag section: iterate through the blocks composing this tag but the last
  // // one
  // int previousLine = Util.getLineNumber(previousEnd, this.lineEnds, 0,
  // this.maxLines);
  // boolean clearBlankLines =
  // this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment;
  // boolean joinLines = this.formatter.preferences.join_lines_in_comments;
  // for (int i = 0; i <= maxNodes; i++) {
  // FormatJavadocNode node = block.nodes[i];
  // int nodeStart = node.sourceStart;
  //
  // // Print empty lines before the node
  // int newLines;
  // if (i == 0) {
  // newLines = this.formatter.preferences.comment_insert_new_line_for_parameter
  // && block.isParamTag() ? 1 : 0;
  // if (nodeStart > (previousEnd + 1)) {
  // if (!clearBlankLines || !joinLines) {
  // int startLine = Util.getLineNumber(nodeStart, this.lineEnds,
  // previousLine - 1, this.maxLines);
  // int gapLine = previousLine;
  // if (joinLines)
  // gapLine++; // if not preserving line break then gap must be at
  // // least of one line
  // if (startLine > gapLine) {
  // newLines = startLine - previousLine;
  // }
  // if (clearBlankLines) {
  // // clearing blank lines in this block means that break lines
  // // should be preserved, hence only keep one new line
  // if (newLines > 0)
  // newLines = 1;
  // }
  // }
  // if (newLines == 0 && (!node.isImmutable() || block.reference != null)) {
  // newLines = printJavadocBlockNodesNewLines(block, node, previousEnd);
  // }
  // if (block.isImmutable()) {
  // printJavadocGapLinesForImmutableBlock(block);
  // } else {
  // printJavadocGapLines(previousEnd + 1, nodeStart - 1, newLines,
  // clearBlankLines, false, null);
  // }
  // } else {
  // this.tempBuffer.setLength(0);
  // if (newLines > 0) {
  // for (int j = 0; j < newLines; j++) {
  // printJavadocNewLine(this.tempBuffer);
  // }
  // addInsertEdit(nodeStart, this.tempBuffer.toString());
  // }
  // }
  // } else {
  // newLines = this.column > maxColumn ? 1 : 0;
  // if (!clearBlankLines && node.lineStart > (previousLine + 1))
  // newLines = node.lineStart - previousLine;
  // if (newLines < node.linesBefore)
  // newLines = node.linesBefore;
  // if (newLines == 0) {
  // newLines = printJavadocBlockNodesNewLines(block, node, previousEnd);
  // }
  // if (newLines > 0 || nodeStart > (previousEnd + 1)) {
  // printJavadocGapLines(previousEnd + 1, nodeStart - 1, newLines,
  // clearBlankLines, false, null);
  // }
  // }
  // if (headerLine && newLines > 0) {
  // headerLine = false;
  // maxColumn--;
  // }
  //
  // // Print node
  // if (node.isText()) {
  // FormatJavadocText text = (FormatJavadocText) node;
  // if (text.isImmutable()) {
  // // Indent if new line was added
  // if (text.isImmutableHtmlTag() && newLines > 0
  // && this.commentIndentation != null) {
  // addInsertEdit(node.sourceStart, this.commentIndentation);
  // this.column += this.commentIndentation.length();
  // }
  // printJavadocImmutableText(text, block, newLines > 0);
  // this.column += getTextLength(block, text);
  // } else if (text.isHtmlTag()) {
  // printJavadocHtmlTag(text, block, newLines > 0);
  // } else {
  // printJavadocText(text, block, newLines > 0);
  // }
  // } else {
  // if (newLines > 0 && this.commentIndentation != null) {
  // addInsertEdit(node.sourceStart, this.commentIndentation);
  // this.column += this.commentIndentation.length();
  // }
  // printJavadocBlock((FormatJavadocBlock) node);
  // }
  //
  // // Print empty lines before the node
  // previousEnd = node.sourceEnd;
  // previousLine = Util.getLineNumber(previousEnd, this.lineEnds,
  // node.lineStart > 1 ? node.lineStart - 2 : 0, this.maxLines);
  // }
  // this.lastNumberOfNewLines = 0;
  // }

  // private int printJavadocBlockNodesNewLines(FormatJavadocBlock block,
  // FormatJavadocNode node, int previousEnd) {
  // int maxColumn = this.formatter.preferences.comment_line_length + 1;
  // int nodeStart = node.sourceStart;
  // try {
  // this.scanner.resetTo(nodeStart, node.sourceEnd);
  // int length = 0;
  // boolean newLine = false;
  // boolean headerLine = block.isHeaderLine()
  // && this.lastNumberOfNewLines == 0;
  // int firstColumn = 1 + this.indentationLevel + BLOCK_LINE_PREFIX_LENGTH;
  // if (this.commentIndentation != null)
  // firstColumn += this.commentIndentation.length();
  // if (headerLine)
  // maxColumn++;
  // FormatJavadocText text = null;
  // boolean isImmutableNode = node.isImmutable();
  // boolean nodeIsText = node.isText();
  // if (nodeIsText) {
  // text = (FormatJavadocText) node;
  // } else {
  // FormatJavadocBlock inlinedBlock = (FormatJavadocBlock) node;
  // if (isImmutableNode) {
  // text = (FormatJavadocText) inlinedBlock.getLastNode();
  // if (text != null) {
  // length += inlinedBlock.tagEnd - inlinedBlock.sourceStart + 1; // tag
  // // length
  // if (nodeStart > (previousEnd + 1)) {
  // length++; // include space between nodes
  // }
  // this.scanner.resetTo(text.sourceStart, node.sourceEnd);
  // }
  // }
  // }
  // if (text != null) {
  // if (isImmutableNode) {
  // if (nodeStart > (previousEnd + 1)) {
  // length++; // include space between nodes
  // }
  // int lastColumn = this.column + length;
  // while (!this.scanner.atEnd()) {
  // try {
  // int token = this.scanner.getNextToken();
  // switch (token) {
  // case TerminalTokens.TokenNameWHITESPACE:
  // if (CharOperation.indexOf('\n', this.scanner.source,
  // this.scanner.startPosition, this.scanner.currentPosition) >= 0) {
  // return 0;
  // }
  // lastColumn = getCurrentIndentation(
  // this.scanner.getCurrentTokenSource(), lastColumn);
  // break;
  // case TerminalTokens.TokenNameMULTIPLY:
  // if (newLine) {
  // newLine = false;
  // continue;
  // }
  // lastColumn++;
  // break;
  // default:
  // lastColumn += (this.scanner.atEnd()
  // ? this.scanner.eofPosition : this.scanner.currentPosition)
  // - this.scanner.startPosition;
  // break;
  // }
  // } catch (InvalidInputException iie) {
  // // maybe an unterminated string or comment
  // lastColumn += (this.scanner.atEnd() ? this.scanner.eofPosition
  // : this.scanner.currentPosition) - this.scanner.startPosition;
  // }
  // if (lastColumn > maxColumn) {
  // return 1;
  // }
  // }
  // return 0;
  // }
  // if (text.isHtmlTag()) {
  // if (text.getHtmlTagID() == JAVADOC_SINGLE_BREAK_TAG_ID) {
  // // never break before single break tag
  // return 0;
  // }
  // // read the html tag
  // this.scanner.getNextToken();
  // if (this.scanner.getNextToken() == TerminalTokens.TokenNameDIVIDE) {
  // length++;
  // this.scanner.getNextToken();
  // }
  // length += (this.scanner.atEnd() ? this.scanner.eofPosition
  // : this.scanner.currentPosition) - this.scanner.startPosition;
  // this.scanner.getNextToken(); // '>'
  // length++;
  // } else {
  // while (true) {
  // int token = this.scanner.getNextToken();
  // if (token == TerminalTokens.TokenNameWHITESPACE
  // || token == TerminalTokens.TokenNameEOF)
  // break;
  // int tokenLength = (this.scanner.atEnd() ? this.scanner.eofPosition
  // : this.scanner.currentPosition) - this.scanner.startPosition;
  // length += tokenLength;
  // if ((this.column + length) >= maxColumn) {
  // break;
  // }
  // }
  // }
  // } else {
  // FormatJavadocBlock inlinedBlock = (FormatJavadocBlock) node;
  // length += inlinedBlock.tagEnd - inlinedBlock.sourceStart + 1; // tag
  // // length
  // if (inlinedBlock.reference != null) {
  // length++; // space between tag and reference
  // this.scanner.resetTo(inlinedBlock.reference.sourceStart,
  // inlinedBlock.reference.sourceEnd);
  // int previousToken = -1;
  // loop : while (!this.scanner.atEnd()) {
  // int token = this.scanner.getNextToken();
  // int tokenLength = (this.scanner.atEnd() ? this.scanner.eofPosition
  // : this.scanner.currentPosition) - this.scanner.startPosition;
  // switch (token) {
  // case TerminalTokens.TokenNameWHITESPACE:
  // if (previousToken == TerminalTokens.TokenNameCOMMA) { // space
  // // between
  // // method
  // // arguments
  // length++;
  // }
  // break;
  // case TerminalTokens.TokenNameMULTIPLY:
  // break;
  // default:
  // length += tokenLength;
  // if ((this.column + length) > maxColumn) {
  // break loop;
  // }
  // break;
  // }
  // previousToken = token;
  // }
  // }
  // length++; // one more for closing brace
  // }
  // if (nodeStart > (previousEnd + 1)) {
  // length++; // include space between nodes
  // }
  // if ((firstColumn + length) >= maxColumn && node == block.nodes[0]) {
  // // Do not split in this peculiar case as length would be also over the
  // // max
  // // length on next line
  // return 0;
  // }
  // if ((this.column + length) > maxColumn) {
  // return 1;
  // }
  // } catch (InvalidInputException iie) {
  // // Assume length is one
  // int tokenLength = 1;
  // if (nodeStart > (previousEnd + 1)) {
  // tokenLength++; // include space between nodes
  // }
  // if ((this.column + tokenLength) > maxColumn) {
  // return 1;
  // }
  // }
  // return 0;
  // }

  // private void printJavadocBlockReference(FormatJavadocBlock block,
  // FormatJavadocReference reference) {
  // int maxColumn = this.formatter.preferences.comment_line_length + 1;
  // boolean headerLine = block.isHeaderLine();
  // boolean inlined = block.isInlined();
  // if (headerLine)
  // maxColumn++;
  //
  // // First we need to know what is the indentation
  // this.scanner.resetTo(block.tagEnd + 1, reference.sourceEnd);
  // this.javadocBlockRefBuffer.setLength(0);
  // boolean needFormat = false;
  // int previousToken = -1;
  // int spacePosition = -1;
  // String newLineString = null;
  // int firstColumn = -1;
  // while (!this.scanner.atEnd()) {
  // int token;
  // try {
  // token = this.scanner.getNextToken();
  // int tokenLength = (this.scanner.atEnd() ? this.scanner.eofPosition
  // : this.scanner.currentPosition) - this.scanner.startPosition;
  // switch (token) {
  // case TerminalTokens.TokenNameWHITESPACE:
  // if (previousToken != -1 || tokenLength > 1
  // || this.scanner.currentCharacter != ' ')
  // needFormat = true;
  // switch (previousToken) {
  // case TerminalTokens.TokenNameMULTIPLY:
  // case TerminalTokens.TokenNameLPAREN:
  // break;
  // default: // space between method arguments
  // spacePosition = this.javadocBlockRefBuffer.length();
  // // $FALL-THROUGH$ - fall through next case
  // case -1:
  // this.javadocBlockRefBuffer.append(' ');
  // this.column++;
  // break;
  // }
  // break;
  // case TerminalTokens.TokenNameMULTIPLY:
  // break;
  // default:
  // if (!inlined && spacePosition > 0
  // && (this.column + tokenLength) > maxColumn) {
  // // not enough space on the line
  // this.lastNumberOfNewLines++;
  // this.line++;
  // if (newLineString == null) {
  // this.tempBuffer.setLength(0);
  // this.tempBuffer.append(this.lineSeparator);
  // this.column = 1;
  // printIndentationIfNecessary(this.tempBuffer);
  // this.tempBuffer.append(BLOCK_LINE_PREFIX);
  // this.column += BLOCK_LINE_PREFIX_LENGTH;
  // if (this.commentIndentation != null) {
  // this.tempBuffer.append(this.commentIndentation);
  // this.column += this.commentIndentation.length();
  // }
  // newLineString = this.tempBuffer.substring(0,
  // this.tempBuffer.length() - 1); // remove last space as
  // // buffer will be inserted
  // // before a space
  // firstColumn = this.column;
  // } else {
  // this.column = firstColumn;
  // }
  // this.column = firstColumn + this.javadocBlockRefBuffer.length()
  // - spacePosition - 1;
  // this.javadocBlockRefBuffer.insert(spacePosition, newLineString);
  // if (headerLine) {
  // headerLine = false;
  // maxColumn--;
  // }
  // spacePosition = -1;
  // }
  // this.javadocBlockRefBuffer.append(this.scanner.source,
  // this.scanner.startPosition, tokenLength);
  // this.column += tokenLength;
  // break;
  // }
  // previousToken = token;
  // } catch (InvalidInputException iie) {
  // // does not happen as syntax is correct
  // }
  // }
  // if (needFormat) {
  // addReplaceEdit(block.tagEnd + 1, reference.sourceEnd,
  // this.javadocBlockRefBuffer.toString());
  // }
  // }

  /*
   * prints the empty javadoc line between the 2 given positions. May insert new '*' before each new
   * line
   */
  private void printJavadocGapLines(int textStartPosition, int textEndPosition, int newLines,
      boolean clearBlankLines, boolean footer, StringBuffer output) {
    DartCore.notYetImplemented();
    // try {
    // // If no lines to set in the gap then just insert a space if there's
    // // enough room to
    // if (newLines == 0) {
    // if (output == null) {
    //          addReplaceEdit(textStartPosition, textEndPosition, " "); //$NON-NLS-1$
    // } else {
    // output.append(' ');
    // }
    // this.column++;
    // return;
    // }
    //
    // // if there's no enough room to replace text, then insert the gap
    // if (textStartPosition > textEndPosition) {
    // if (newLines > 0) {
    // this.javadocGapLinesBuffer.setLength(0);
    // for (int i = 0; i < newLines; i++) {
    // this.javadocGapLinesBuffer.append(this.lineSeparator);
    // this.column = 1;
    // printIndentationIfNecessary(this.javadocGapLinesBuffer);
    // if (footer) {
    // this.javadocGapLinesBuffer.append(' ');
    // this.column++;
    // } else {
    // this.javadocGapLinesBuffer.append(BLOCK_LINE_PREFIX);
    // this.column += BLOCK_LINE_PREFIX_LENGTH;
    // }
    // }
    // if (output == null) {
    // addInsertEdit(textStartPosition,
    // this.javadocGapLinesBuffer.toString());
    // } else {
    // output.append(this.javadocGapLinesBuffer);
    // }
    // }
    // return;
    // }
    //
    // // There's enough room and some lines to set...
    // // Skip the text token per token to keep existing stars when possible
    // this.scanner.resetTo(textStartPosition, textEndPosition);
    // this.scanner.recordLineSeparator = true;
    // this.scanner.linePtr = Util.getLineNumber(textStartPosition,
    // this.lineEnds, 0, this.maxLines) - 2;
    // int linePtr = this.scanner.linePtr;
    // int lineCount = 0;
    // int start = textStartPosition;
    // boolean endsOnMultiply = false;
    // while (!this.scanner.atEnd()) {
    // switch (this.scanner.getNextToken()) {
    // case TerminalTokens.TokenNameMULTIPLY:
    // // we just need to replace each lines between '*' with the javadoc
    // // formatted ones
    // int linesGap = this.scanner.linePtr - linePtr;
    // if (linesGap > 0) {
    // this.javadocGapLinesBuffer.setLength(0);
    // if (lineCount > 0) {
    // // TODO https://bugs.eclipse.org/bugs/show_bug.cgi?id=49619
    // this.javadocGapLinesBuffer.append(' ');
    // }
    // for (int i = 0; i < linesGap; i++) {
    // if (clearBlankLines && lineCount >= newLines) {
    // // leave as the required new lines have been inserted
    // // so remove any remaining blanks and leave
    // if (textEndPosition >= start) {
    // if (output == null) {
    // addReplaceEdit(start, textEndPosition,
    // this.javadocGapLinesBuffer.toString());
    // } else {
    // output.append(this.javadocGapLinesBuffer);
    // }
    // }
    // return;
    // }
    // this.javadocGapLinesBuffer.append(this.lineSeparator);
    // this.column = 1;
    // printIndentationIfNecessary(this.javadocGapLinesBuffer);
    // if (i == (linesGap - 1)) {
    // this.javadocGapLinesBuffer.append(' ');
    // this.column++;
    // } else {
    // this.javadocGapLinesBuffer.append(BLOCK_LINE_PREFIX);
    // this.column += BLOCK_LINE_PREFIX_LENGTH;
    // }
    // lineCount++;
    // }
    // int currentTokenStartPosition =
    // this.scanner.getCurrentTokenStartPosition();
    // int tokenLength = this.scanner.currentPosition
    // - currentTokenStartPosition;
    // if (output == null) {
    // addReplaceEdit(start, currentTokenStartPosition - 1,
    // this.javadocGapLinesBuffer.toString());
    // } else {
    // output.append(this.javadocGapLinesBuffer);
    // output.append(this.scanner.source, currentTokenStartPosition,
    // tokenLength);
    // }
    // this.column += tokenLength;
    // if (footer && clearBlankLines && lineCount == newLines) {
    // if (textEndPosition >= currentTokenStartPosition) {
    // if (output == null) {
    // addDeleteEdit(currentTokenStartPosition, textEndPosition);
    // }
    // }
    // return;
    // }
    // }
    // // next start is just after the current token
    // start = this.scanner.currentPosition;
    // linePtr = this.scanner.linePtr;
    // endsOnMultiply = true;
    // break;
    // default:
    // endsOnMultiply = false;
    // break;
    // }
    // }
    //
    // // Format the last whitespaces
    // if (lineCount < newLines) {
    // // Insert new lines as not enough was encountered while scanning the
    // // whitespaces
    // this.javadocGapLinesBuffer.setLength(0);
    // if (lineCount > 0) {
    // // TODO https://bugs.eclipse.org/bugs/show_bug.cgi?id=49619
    // this.javadocGapLinesBuffer.append(' ');
    // }
    // for (int i = lineCount; i < newLines - 1; i++) {
    // printJavadocNewLine(this.javadocGapLinesBuffer);
    // }
    // this.javadocGapLinesBuffer.append(this.lineSeparator);
    // this.column = 1;
    // printIndentationIfNecessary(this.javadocGapLinesBuffer);
    // if (footer) {
    // this.javadocGapLinesBuffer.append(' ');
    // this.column++;
    // } else {
    // this.javadocGapLinesBuffer.append(BLOCK_LINE_PREFIX);
    // this.column += BLOCK_LINE_PREFIX_LENGTH;
    // }
    // if (output == null) {
    // if (textEndPosition >= start) {
    // addReplaceEdit(start, textEndPosition,
    // this.javadocGapLinesBuffer.toString());
    // } else {
    // addInsertEdit(textEndPosition + 1,
    // this.javadocGapLinesBuffer.toString());
    // }
    // } else {
    // output.append(this.javadocGapLinesBuffer);
    // }
    // } else {
    // // Replace all remaining whitespaces by a single space
    // if (textEndPosition >= start) {
    // this.javadocGapLinesBuffer.setLength(0);
    // if (this.scanner.linePtr > linePtr) {
    // if (lineCount > 0) {
    // // TODO https://bugs.eclipse.org/bugs/show_bug.cgi?id=49619
    // this.javadocGapLinesBuffer.append(' ');
    // }
    // this.javadocGapLinesBuffer.append(this.lineSeparator);
    // this.column = 1;
    // printIndentationIfNecessary(this.javadocGapLinesBuffer);
    // }
    // this.javadocGapLinesBuffer.append(' ');
    // if (output == null) {
    // addReplaceEdit(start, textEndPosition,
    // this.javadocGapLinesBuffer.toString());
    // } else {
    // output.append(this.javadocGapLinesBuffer);
    // }
    // this.needSpace = false;
    // } else if (endsOnMultiply) {
    // if (output == null) {
    //            addInsertEdit(textEndPosition + 1, " "); //$NON-NLS-1$
    // } else {
    // output.append(' ');
    // }
    // this.needSpace = false;
    // }
    // this.column++;
    // }
    // } catch (InvalidInputException iie) {
    // // there's nothing to do if this exception happens
    // } finally {
    // this.scanner.recordLineSeparator = false;
    // this.needSpace = false;
    // this.scanner.resetTo(textEndPosition + 1, this.scannerEndPosition - 1);
    // this.lastNumberOfNewLines += newLines;
    // this.line += newLines;
    // }
  }

  /*
   * Print the gap lines for an immutable block. That's needed to be specific as the formatter needs
   * to keep white spaces if possible except those which are indentation ones. Note that in the
   * peculiar case of a two lines immutable tag (multi lines block), the formatter will join the two
   * lines.
   */
//  private void printJavadocGapLinesForImmutableBlock(FormatJavadocBlock block) {
//
//    // Init
//    int firstLineEnd = -1; // not initialized
//    int newLineStart = -1; // not initialized
//    int secondLineStart = -1; // not initialized
//    int starPosition = -1; // not initialized
//    int offset = 0;
//    int start = block.tagEnd + 1;
//    int end = block.nodes[0].sourceStart - 1;
//    this.scanner.resetTo(start, end);
//    int lineStart = block.lineStart;
//    int lineEnd = Util.getLineNumber(block.nodes[0].sourceEnd, this.lineEnds,
//        lineStart - 1, this.maxLines);
//    boolean multiLinesBlock = lineEnd > (lineStart + 1);
//    int previousPosition = this.scanner.currentPosition;
//    String newLineString = null;
//    int indentationColumn = 0;
//    int leadingSpaces = -1;
//
//    // Scan the existing gap
//    while (!this.scanner.atEnd()) {
//      char ch = (char) this.scanner.getNextChar();
//      switch (ch) {
//        case '\t':
//          // increase the corresponding counter from the appropriate tab value
//          if (secondLineStart > 0 || firstLineEnd < 0) {
//            int reminder = this.tabLength == 0 ? 0 : offset % this.tabLength;
//            if (reminder == 0) {
//              offset += this.tabLength;
//            } else {
//              offset = ((offset / this.tabLength) + 1) * this.tabLength;
//            }
//          } else if (leadingSpaces >= 0) {
//            int reminder = this.tabLength == 0 ? 0 : offset % this.tabLength;
//            if (reminder == 0) {
//              leadingSpaces += this.tabLength;
//            } else {
//              leadingSpaces = ((offset / this.tabLength) + 1) * this.tabLength;
//            }
//          }
//          break;
//        case '\r':
//        case '\n':
//          // new line, store the end of the first one
//          if (firstLineEnd < 0) {
//            firstLineEnd = previousPosition;
//          }
//          // print indentation if there were spaces without any star on the line
//          if (leadingSpaces > 0 && multiLinesBlock) {
//            if (newLineString == null) {
//              this.column = 1;
//              this.tempBuffer.setLength(0);
//              printIndentationIfNecessary(this.tempBuffer);
//              this.tempBuffer.append(BLOCK_LINE_PREFIX);
//              this.column += BLOCK_LINE_PREFIX_LENGTH;
//              newLineString = this.tempBuffer.toString();
//              indentationColumn = this.column;
//            } else {
//              this.column = indentationColumn;
//            }
//            addReplaceEdit(newLineStart, newLineStart + indentationColumn - 2,
//                newLineString);
//          }
//          // store line start and reset positions
//          newLineStart = this.scanner.currentPosition;
//          leadingSpaces = 0;
//          starPosition = -1;
//          if (multiLinesBlock) {
//            offset = 0;
//            secondLineStart = -1;
//          }
//          break;
//        case '*':
//          // store line start position if this is the first star of the line
//          if (starPosition < 0 && firstLineEnd > 0) {
//            secondLineStart = this.scanner.currentPosition;
//            starPosition = this.scanner.currentPosition;
//            leadingSpaces = -1;
//          }
//          break;
//        default:
//          // increment offset if line has started
//          if (secondLineStart > 0) {
//            // skip first white space after the first '*'
//            if (secondLineStart == starPosition) {
//              secondLineStart = this.scanner.currentPosition;
//            } else {
//              // print indentation before the following characters
//              if (offset == 0 && multiLinesBlock) {
//                if (newLineString == null) {
//                  this.tempBuffer.setLength(0);
//                  this.column = 1;
//                  printIndentationIfNecessary(this.tempBuffer);
//                  this.tempBuffer.append(BLOCK_LINE_PREFIX);
//                  this.column += BLOCK_LINE_PREFIX_LENGTH;
//                  indentationColumn = this.column;
//                  newLineString = this.tempBuffer.toString();
//                } else {
//                  this.column = indentationColumn;
//                }
//                addReplaceEdit(newLineStart, secondLineStart - 1, newLineString);
//              }
//              offset++;
//            }
//          } else if (firstLineEnd < 0) {
//            // no new line yet, increment the offset
//            offset++;
//          } else if (leadingSpaces >= 0) {
//            // no star yet, increment the leading spaces
//            leadingSpaces++;
//          }
//          break;
//      }
//      previousPosition = this.scanner.currentPosition;
//    }
//
//    // Increment the columns from the numbers of characters counted on the line
//    if (multiLinesBlock) {
//      this.column += offset;
//    } else {
//      this.column++;
//    }
//
//    // Replace the new line with a single space when there's only one separator
//    // or, if necessary, print the indentation on the last line
//    if (!multiLinesBlock) {
//      if (firstLineEnd > 0) {
//        addReplaceEdit(firstLineEnd, end, " "); //$NON-NLS-1$
//      }
//    } else if (secondLineStart > 0) {
//      if (newLineString == null) {
//        this.tempBuffer.setLength(0);
//        this.column = 1;
//        printIndentationIfNecessary(this.tempBuffer);
//        this.tempBuffer.append(BLOCK_LINE_PREFIX);
//        this.column += BLOCK_LINE_PREFIX_LENGTH;
//        newLineString = this.tempBuffer.toString();
//        indentationColumn = this.column;
//      } else {
//        this.column = indentationColumn;
//      }
//      addReplaceEdit(newLineStart, secondLineStart - 1, newLineString);
//    } else if (leadingSpaces > 0) {
//      if (newLineString == null) {
//        this.tempBuffer.setLength(0);
//        this.column = 1;
//        printIndentationIfNecessary(this.tempBuffer);
//        this.tempBuffer.append(BLOCK_LINE_PREFIX);
//        this.column += BLOCK_LINE_PREFIX_LENGTH;
//        newLineString = this.tempBuffer.toString();
//        indentationColumn = this.column;
//      } else {
//        this.column = indentationColumn;
//      }
//      addReplaceEdit(newLineStart, newLineStart + indentationColumn - 2,
//          newLineString);
//    }
//
//    // Reset
//    this.needSpace = false;
//    this.scanner.resetTo(end + 1, this.scannerEndPosition - 1);
//  }

//  private int printJavadocHtmlTag(FormatJavadocText text,
//      FormatJavadocBlock block, boolean textOnNewLine) {
//
//    // Compute indentation if necessary
//    boolean clearBlankLines = this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment;
//
//    // Local variables init
//    int textStart = text.sourceStart;
//    int nextStart = textStart;
//    int startLine = Util.getLineNumber(textStart, this.lineEnds, 0,
//        this.maxLines);
//    int htmlTagID = text.getHtmlTagID();
//    if (text.depth >= this.javadocHtmlTagBuffers.length) {
//      int length = this.javadocHtmlTagBuffers.length;
//      System.arraycopy(this.javadocHtmlTagBuffers, 0,
//          this.javadocHtmlTagBuffers = new StringBuffer[text.depth + 6], 0,
//          length);
//    }
//    StringBuffer buffer = this.javadocHtmlTagBuffers[text.depth];
//    if (buffer == null) {
//      buffer = new StringBuffer();
//      this.javadocHtmlTagBuffers[text.depth] = buffer;
//    } else {
//      buffer.setLength(0);
//    }
//
//    // New line will be added before next node
//    int max = text.separatorsPtr;
//    int linesAfter = 0;
//    int previousEnd = -1;
//    boolean isHtmlBreakTag = htmlTagID == JAVADOC_SINGLE_BREAK_TAG_ID;
//    boolean isHtmlSeparatorTag = htmlTagID == JAVADOC_SEPARATOR_TAGS_ID;
//    if (isHtmlBreakTag) {
//      return 1;
//    }
//
//    // Iterate on text line separators
//    boolean isCode = htmlTagID == JAVADOC_CODE_TAGS_ID;
//    for (int idx = 0, ptr = 0; idx <= max
//        || (text.htmlNodesPtr != -1 && ptr <= text.htmlNodesPtr); idx++) {
//
//      // append text to buffer realigning with the line length
//      int end = (idx > max) ? text.sourceEnd
//          : (int) (text.separators[idx] >>> 32);
//      int nodeKind = 0; // text break
//      if (text.htmlNodesPtr >= 0 && ptr <= text.htmlNodesPtr
//          && end > text.htmlNodes[ptr].sourceStart) {
//        FormatJavadocNode node = text.htmlNodes[ptr];
//        FormatJavadocText htmlTag = node.isText() ? (FormatJavadocText) node
//            : null;
//        int newLines = htmlTag == null ? 0 : htmlTag.linesBefore;
//        if (linesAfter > newLines) {
//          newLines = linesAfter;
//          if (newLines > 1 && clearBlankLines) {
//            if (idx < 2
//                || (text.htmlIndexes[idx - 2] & JAVADOC_TAGS_ID_MASK) != JAVADOC_CODE_TAGS_ID) {
//              newLines = 1;
//            }
//          }
//        }
//        if (textStart < previousEnd) {
//          addReplaceEdit(textStart, previousEnd, buffer.toString());
//        }
//        boolean immutable = node.isImmutable();
//        if (newLines == 0) {
//          newLines = printJavadocBlockNodesNewLines(block, node, previousEnd);
//        }
//        int nodeStart = node.sourceStart;
//        if (newLines > 0 || (idx > 1 && nodeStart > (previousEnd + 1))) {
//          printJavadocGapLines(previousEnd + 1, nodeStart - 1, newLines,
//              clearBlankLines, false, null);
//        }
//        if (newLines > 0)
//          textOnNewLine = true;
//        buffer.setLength(0);
//        if (node.isText()) {
//          if (immutable) {
//            // do not change immutable tags, just increment column
//            if (textOnNewLine && this.commentIndentation != null) {
//              addInsertEdit(node.sourceStart, this.commentIndentation);
//              this.column += this.commentIndentation.length();
//            }
//            printJavadocImmutableText(htmlTag, block, textOnNewLine);
//            this.column += getTextLength(block, htmlTag);
//            linesAfter = 0;
//          } else {
//            linesAfter = printJavadocHtmlTag(htmlTag, block, textOnNewLine);
//          }
//          nodeKind = 1; // text
//        } else {
//          if (textOnNewLine && this.commentIndentation != null) {
//            addInsertEdit(node.sourceStart, this.commentIndentation);
//            this.column += this.commentIndentation.length();
//          }
//          printJavadocBlock((FormatJavadocBlock) node);
//          linesAfter = 0;
//          nodeKind = 2; // block
//        }
//        textStart = node.sourceEnd + 1;
//        ptr++;
//        if (idx > max) {
//          return linesAfter;
//        }
//      } else {
//        if (idx > 0 && linesAfter > 0) {
//          printJavadocGapLines(previousEnd + 1, nextStart - 1, linesAfter,
//              clearBlankLines, false, buffer);
//          textOnNewLine = true;
//        }
//        boolean needIndentation = textOnNewLine;
//        if (idx > 0) {
//          if (!needIndentation && text.isTextAfterHtmlSeparatorTag(idx - 1)) {
//            needIndentation = true;
//          }
//        }
//        /*
//         * There's no space between text and html tag or inline block => do not
//         * insert space a the beginning of the text
//         */
//        this.needSpace = idx > 1 && (previousEnd + 1) < nextStart;
//        printJavadocTextLine(buffer, nextStart, end, block, idx == 0,
//            needIndentation, idx == 0/* opening html tag? */
//                || text.htmlIndexes[idx - 1] != -1);
//        linesAfter = 0;
//        if (idx == 0) {
//          if (isHtmlSeparatorTag) {
//            linesAfter = 1;
//          }
//        } else if (text.htmlIndexes[idx - 1] == JAVADOC_SINGLE_BREAK_TAG_ID) {
//          linesAfter = 1;
//        }
//      }
//
//      // Replace with current buffer if there are several empty lines between
//      // text lines
//      nextStart = (int) text.separators[idx];
//      int endLine = Util.getLineNumber(end, this.lineEnds, startLine - 1,
//          this.maxLines);
//      startLine = Util.getLineNumber(nextStart, this.lineEnds, endLine - 1,
//          this.maxLines);
//      int linesGap = startLine - endLine;
//      if (linesGap > 0) {
//        if (clearBlankLines) {
//          // keep previously computed lines after
//        } else {
//          if (idx == 0
//              || linesGap > 1
//              || (idx < max && nodeKind == 1 && (text.htmlIndexes[idx - 1] & JAVADOC_TAGS_ID_MASK) != JAVADOC_IMMUTABLE_TAGS_ID)) {
//            if (linesAfter < linesGap) {
//              linesAfter = linesGap;
//            }
//          }
//        }
//      }
//      textOnNewLine = linesAfter > 0;
//
//      // print <pre> tag
//      if (isCode) {
//        int codeEnd = (int) (text.separators[max] >>> 32);
//        if (codeEnd > end) {
//          if (this.formatter.preferences.comment_format_source) {
//            if (textStart < end)
//              addReplaceEdit(textStart, end, buffer.toString());
//            // See whether there's a space before the code
//            if (linesGap > 0) {
//              int lineStart = this.scanner.getLineStart(startLine);
//              if (nextStart > lineStart) { // if code starts at the line, then
//                // no leading space is needed
//                this.scanner.resetTo(lineStart, nextStart - 1);
//                try {
//                  int token = this.scanner.getNextToken();
//                  if (token == TerminalTokens.TokenNameWHITESPACE) {
//                    // skip indentation
//                    token = this.scanner.getNextToken();
//                  }
//                  if (token == TerminalTokens.TokenNameMULTIPLY) {
//                    nextStart = this.scanner.currentPosition;
//                  }
//                } catch (InvalidInputException iie) {
//                  // skip
//                }
//              }
//            }
//            // Format gap lines before code
//            int newLines = linesGap;
//            if (newLines == 0)
//              newLines = 1;
//            this.needSpace = false;
//            /*
//             * clear first blank lines inside <pre> tag as done by old formatter
//             */
//            printJavadocGapLines(end + 1, nextStart - 1, newLines, false,
//                false, null);
//            // Format the code
//            printCodeSnippet(nextStart, codeEnd, linesGap);
//            // Format the gap lines after the code
//            nextStart = (int) text.separators[max];
//            /*
//             * clear blank lines inside <pre> tag as done by old formatter
//             */
//            printJavadocGapLines(codeEnd + 1, nextStart - 1, 1, false,
//                false, null);
//            return 2;
//          }
//        } else {
//          nextStart = (int) text.separators[max];
//          if ((nextStart - 1) > (end + 1)) {
//            int line1 = Util.getLineNumber(end + 1, this.lineEnds,
//                startLine - 1, this.maxLines);
//            int line2 = Util.getLineNumber(nextStart - 1, this.lineEnds,
//                line1 - 1, this.maxLines);
//            int gapLines = line2 - line1 - 1;
//            /*
//             * never clear blank lines inside <pre> tag
//             */
//            printJavadocGapLines(end + 1, nextStart - 1, gapLines, false,
//                false, null);
//            if (gapLines > 0)
//              textOnNewLine = true;
//          }
//        }
//        return 1;
//      }
//
//      // store previous end
//      previousEnd = end;
//    }
//
//    // Insert last gap
//    boolean closingTag = isHtmlBreakTag
//        || (text.htmlIndexes != null && (text.htmlIndexes[max] & JAVADOC_TAGS_ID_MASK) == htmlTagID);
//    boolean isValidHtmlSeparatorTag = max > 0 && isHtmlSeparatorTag
//        && closingTag;
//    if (previousEnd != -1) {
//      if (isValidHtmlSeparatorTag) {
//        if (linesAfter == 0)
//          linesAfter = 1;
//      }
//      if (linesAfter > 0) {
//        printJavadocGapLines(previousEnd + 1, nextStart - 1, linesAfter,
//            clearBlankLines, false, buffer);
//        textOnNewLine = linesAfter > 0;
//      }
//    }
//
//    // Print closing tag
//    boolean needIndentation = textOnNewLine;
//    if (!needIndentation && !isHtmlBreakTag && text.htmlIndexes != null
//        && text.isTextAfterHtmlSeparatorTag(max)) {
//      needIndentation = true;
//    }
//    this.needSpace = !closingTag && max > 0 // not a single or not closed tag
//        // (e.g. <br>)
//        && (previousEnd + 1) < nextStart; // There's no space between text and
//    // html tag or inline block => do not
//    // insert space a the beginning of the
//    // text
//    printJavadocTextLine(buffer, nextStart, text.sourceEnd, block, max <= 0,
//        needIndentation, closingTag/* closing html tag */);
//    if (textStart < text.sourceEnd) {
//      addReplaceEdit(textStart, text.sourceEnd, buffer.toString());
//    }
//
//    // Reset
//    this.needSpace = false;
//    this.scanner.resetTo(text.sourceEnd + 1, this.scannerEndPosition - 1);
//
//    // Return the new lines to insert after
//    return isValidHtmlSeparatorTag ? 1 : 0;
//  }

//  private void printJavadocImmutableText(FormatJavadocText text,
//      FormatJavadocBlock block, boolean textOnNewLine) {
//
//    try {
//      // Iterate on text line separators
//      int textLineStart = text.lineStart;
//      this.scanner.tokenizeWhiteSpace = false;
//      String newLineString = null;
//      for (int idx = 0, max = text.separatorsPtr; idx <= max; idx++) {
//        int start = (int) text.separators[idx];
//        int lineStart = Util.getLineNumber(start, this.lineEnds,
//            textLineStart - 1, this.maxLines);
//        while (textLineStart < lineStart) {
//          int end = this.lineEnds[textLineStart - 1];
//          this.scanner.resetTo(end, start);
//          int token = this.scanner.getNextToken();
//          switch (token) {
//            case TerminalTokens.TokenNameMULTIPLY:
//            case TerminalTokens.TokenNameMULTIPLY_EQUAL:
//              break;
//            default:
//              return;
//          }
//          if (this.scanner.currentCharacter == ' ') {
//            this.scanner.getNextChar();
//          }
//          if (newLineString == null) {
//            this.tempBuffer.setLength(0);
//            this.column = 1;
//            printIndentationIfNecessary(this.tempBuffer);
//            this.tempBuffer.append(BLOCK_LINE_PREFIX);
//            this.column += BLOCK_LINE_PREFIX_LENGTH;
//            newLineString = this.tempBuffer.toString();
//          }
//          addReplaceEdit(end + 1, this.scanner.getCurrentTokenEndPosition(),
//              newLineString);
//          textLineStart = Util.getLineNumber(this.scanner.currentPosition - 1,
//              this.lineEnds, textLineStart, this.maxLines);
//        }
//      }
//    } catch (InvalidInputException iie) {
//      // leave
//    } finally {
//      // Reset
//      this.needSpace = false;
//      this.scanner.tokenizeWhiteSpace = true;
//      this.scanner.resetTo(text.sourceEnd + 1, this.scannerEndPosition - 1);
//    }
//  }

  private void printJavadocNewLine(StringBuffer buffer) {
    DartCore.notYetImplemented();
//    buffer.append(this.lineSeparator);
//    this.column = 1;
//    printIndentationIfNecessary(buffer);
//    buffer.append(BLOCK_LINE_PREFIX);
//    this.column += BLOCK_LINE_PREFIX_LENGTH;
//    this.line++;
//    this.lastNumberOfNewLines++;
  }

//  private void printJavadocText(FormatJavadocText text,
//      FormatJavadocBlock block, boolean textOnNewLine) {
//
//    boolean clearBlankLines = this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment;
//    boolean joinLines = this.formatter.preferences.join_lines_in_comments;
//    this.javadocTextBuffer.setLength(0);
//    int textStart = text.sourceStart;
//    int nextStart = textStart;
//    int startLine = Util.getLineNumber(textStart, this.lineEnds, 0,
//        this.maxLines);
//
//    // Iterate on text line separators
//    for (int idx = 0, max = text.separatorsPtr; idx <= max; idx++) {
//
//      // append text to buffer realigning with the line length
//      int end = (int) (text.separators[idx] >>> 32);
//      boolean needIndentation = textOnNewLine;
//      if (idx > 0) {
//        if (!needIndentation && text.isTextAfterHtmlSeparatorTag(idx - 1)) {
//          needIndentation = true;
//        }
//      }
//      this.needSpace = idx > 0;
//      printJavadocTextLine(this.javadocTextBuffer, nextStart, end, block,
//          idx == 0 || (!joinLines && textOnNewLine)/* first text? */,
//          needIndentation, false /* not an html tag */);
//      textOnNewLine = false;
//
//      // Replace with current buffer if there are several empty lines between
//      // text lines
//      nextStart = (int) text.separators[idx];
//      if (!clearBlankLines || !joinLines) {
//        int endLine = Util.getLineNumber(end, this.lineEnds, startLine - 1,
//            this.maxLines);
//        startLine = Util.getLineNumber(nextStart, this.lineEnds, endLine - 1,
//            this.maxLines);
//        int gapLine = endLine;
//        if (joinLines)
//          gapLine++; // if not preserving line break then gap must be at least
//        // of one line
//        if (startLine > gapLine) {
//          addReplaceEdit(textStart, end, this.javadocTextBuffer.toString());
//          textStart = nextStart;
//          this.javadocTextBuffer.setLength(0);
//          int newLines = startLine - endLine;
//          if (clearBlankLines)
//            newLines = 1;
//          printJavadocGapLines(
//              end + 1,
//              nextStart - 1,
//              newLines,
//              this.formatter.preferences.comment_clear_blank_lines_in_javadoc_comment,
//              false, null);
//          textOnNewLine = true;
//        } else if (startLine > endLine) {
//          textOnNewLine = !joinLines;
//        }
//      }
//    }
//
//    // Replace remaining line
//    boolean needIndentation = textOnNewLine;
//    this.needSpace = text.separatorsPtr >= 0;
//    printJavadocTextLine(this.javadocTextBuffer, nextStart, text.sourceEnd,
//        block, text.separatorsPtr == -1 /* first text? */, needIndentation,
//        false /* not an html tag */);
//    // TODO Bring back following optimization
//    // if (lastNewLines != this.lastNumberOfNewLines || (this.column -
//    // currentColumn) != (text.sourceEnd - text.sourceStart + 1)) {
//    addReplaceEdit(textStart, text.sourceEnd, this.javadocTextBuffer.toString());
//    // }
//
//    // Reset
//    this.needSpace = false;
//    this.scanner.resetTo(text.sourceEnd + 1, this.scannerEndPosition - 1);
//  }

  /*
   * Returns whether the text has been modified or not.
   */
//  private void printJavadocTextLine(StringBuffer buffer, int textStart,
//      int textEnd, FormatJavadocBlock block, boolean firstText,
//      boolean needIndentation, boolean isHtmlTag) {
//
//    boolean headerLine = block.isHeaderLine() && this.lastNumberOfNewLines == 0;
//
//    // First we need to know what is the indentation
//    this.javadocTokensBuffer.setLength(0);
//    int firstColumn = 1 + this.indentationLevel + BLOCK_LINE_PREFIX_LENGTH;
//    int maxColumn = this.formatter.preferences.comment_line_length + 1;
//    if (headerLine) {
//      firstColumn++;
//      maxColumn++;
//    }
//    if (needIndentation && this.commentIndentation != null) {
//      buffer.append(this.commentIndentation);
//      this.column += this.commentIndentation.length();
//      firstColumn += this.commentIndentation.length();
//    }
//    if (this.column < firstColumn) {
//      this.column = firstColumn;
//    }
//
//    // Scan the text token per token to compact it and size it the max line
//    // length
//    String newLineString = null;
//    try {
//      this.scanner.resetTo(textStart, textEnd);
//      this.scanner.skipComments = true;
//      int previousToken = -1;
//      boolean textOnNewLine = needIndentation;
//
//      // Consume text token per token
//      while (!this.scanner.atEnd()) {
//        int token;
//        try {
//          token = this.scanner.getNextToken();
//        } catch (InvalidInputException iie) {
//          token = consumeInvalidToken(textEnd);
//        }
//        int tokensBufferLength = this.javadocTokensBuffer.length();
//        int tokenStart = this.scanner.getCurrentTokenStartPosition();
//        int tokenLength = (this.scanner.atEnd() ? this.scanner.eofPosition
//            : this.scanner.currentPosition) - tokenStart;
//        boolean insertSpace = (previousToken == TerminalTokens.TokenNameWHITESPACE || this.needSpace)
//            && !textOnNewLine;
//        String tokensBufferString = this.javadocTokensBuffer.toString().trim();
//        switch (token) {
//          case TerminalTokens.TokenNameWHITESPACE:
//            if (tokensBufferLength > 0) {
//              boolean shouldSplit = (this.column + tokensBufferLength) > maxColumn
//                  /*
//                   * the max length is reached
//                   */
//                  && !isHtmlTag && (insertSpace || tokensBufferLength > 1)
//                  /*
//                   * allow to split at the beginning only when starting with an
//                   * identifier or a token with a length > 1
//                   */
//                  && tokensBufferString.charAt(0) != '@'; // avoid to split just
//              // before a '@'
//              if (shouldSplit) {
//                this.lastNumberOfNewLines++;
//                this.line++;
//                if (newLineString == null) {
//                  this.tempBuffer.setLength(0);
//                  this.tempBuffer.append(this.lineSeparator);
//                  this.column = 1;
//                  printIndentationIfNecessary(this.tempBuffer);
//                  this.tempBuffer.append(BLOCK_LINE_PREFIX);
//                  this.column += BLOCK_LINE_PREFIX_LENGTH;
//                  if (this.commentIndentation != null) {
//                    this.tempBuffer.append(this.commentIndentation);
//                    this.column += this.commentIndentation.length();
//                  }
//                  firstColumn = this.column;
//                  newLineString = this.tempBuffer.toString();
//                } else {
//                  this.column = firstColumn;
//                }
//                buffer.append(newLineString);
//                buffer.append(tokensBufferString);
//                this.column += tokensBufferString.length();
//                if (headerLine) {
//                  firstColumn--;
//                  maxColumn--;
//                  headerLine = false;
//                }
//              } else {
//                buffer.append(this.javadocTokensBuffer);
//                this.column += tokensBufferLength;
//              }
//              this.javadocTokensBuffer.setLength(0);
//            }
//            textOnNewLine = false;
//            previousToken = token;
//            continue;
//          case TerminalTokens.TokenNameCharacterLiteral:
//            if (this.scanner.currentPosition > this.scanner.eofPosition) {
//              this.scanner.resetTo(this.scanner.startPosition, textEnd);
//              this.scanner.getNextChar();
//              token = 1;
//            }
//            break;
//        }
//        int lastColumn = this.column + tokensBufferLength + tokenLength;
//        if (insertSpace)
//          lastColumn++;
//        boolean shouldSplit = lastColumn > maxColumn // the max length is
//                                                     // reached
//            && (!isHtmlTag || previousToken == -1) // not an html tag or just at
//                                                   // the beginning of it
//            && token != TerminalTokens.TokenNameAT
//            // avoid to split just before a '@'
//            && (tokensBufferLength == 0 || this.javadocTokensBuffer.charAt(tokensBufferLength - 1) != '@');
//        if (shouldSplit) {
//          // not enough space on the line
//          if ((tokensBufferLength > 0 || tokenLength < maxColumn) && !isHtmlTag
//              && tokensBufferLength > 0
//              && (firstColumn + tokensBufferLength + tokenLength) >= maxColumn) {
//            // there won't be enough room even if we break the line before the
//            // buffered tokens
//            // So add the buffered tokens now
//            buffer.append(this.javadocTokensBuffer);
//            this.column += tokensBufferLength;
//            this.javadocTokensBuffer.setLength(0);
//            tokensBufferLength = 0;
//            textOnNewLine = false;
//          }
//          if ((tokensBufferLength > 0 || /*
//                                          * (firstColumn+tokenLength) <
//                                          * maxColumn || (insertSpace &&
//                                          */this.column > firstColumn)
//              && (!textOnNewLine || !firstText)) {
//            this.lastNumberOfNewLines++;
//            this.line++;
//            if (newLineString == null) {
//              this.tempBuffer.setLength(0);
//              this.tempBuffer.append(this.lineSeparator);
//              this.column = 1;
//              printIndentationIfNecessary(this.tempBuffer);
//              this.tempBuffer.append(BLOCK_LINE_PREFIX);
//              this.column += BLOCK_LINE_PREFIX_LENGTH;
//              if (this.commentIndentation != null) {
//                this.tempBuffer.append(this.commentIndentation);
//                this.column += this.commentIndentation.length();
//              }
//              firstColumn = this.column;
//              newLineString = this.tempBuffer.toString();
//            } else {
//              this.column = firstColumn;
//            }
//            buffer.append(newLineString);
//          }
//          if (tokensBufferLength > 0) {
//            String tokensString = tokensBufferString;
//            buffer.append(tokensString);
//            this.column += tokensString.length();
//            this.javadocTokensBuffer.setLength(0);
//            tokensBufferLength = 0;
//          }
//          buffer.append(this.scanner.source, tokenStart, tokenLength);
//          this.column += tokenLength;
//          textOnNewLine = false;
//          if (headerLine) {
//            firstColumn--;
//            maxColumn--;
//            headerLine = false;
//          }
//        } else {
//          // append token to the line
//          if (insertSpace) {
//            this.javadocTokensBuffer.append(' ');
//          }
//          this.javadocTokensBuffer.append(this.scanner.source, tokenStart,
//              tokenLength);
//        }
//        previousToken = token;
//        this.needSpace = false;
//        if (headerLine && lastColumn == maxColumn && this.scanner.atEnd()) {
//          this.lastNumberOfNewLines++;
//          this.line++;
//        }
//      }
//    } finally {
//      this.scanner.skipComments = false;
//      // Add remaining buffered tokens
//      if (this.javadocTokensBuffer.length() > 0) {
//        buffer.append(this.javadocTokensBuffer);
//        this.column += this.javadocTokensBuffer.length();
//      }
//    }
//  }

  private void printLineComment() {
    int currentTokenStartPosition = scanner.getCurrentTokenStartPosition();
    int currentTokenEndPosition = scanner.getCurrentTokenEndPosition() + 1;
    boolean includesLineComments = includesLineComments();
    boolean isNlsTag = false;
    if (CharOperation.indexOf(
        Scanner.TAG_PREFIX,
        scanner.source,
        true,
        currentTokenStartPosition,
        currentTokenEndPosition) != -1) {
      nlsTagCounter = 0;
      isNlsTag = true;
    }
    DartScanner.State state = scanner.getState();
    scanner.resetTo(currentTokenStartPosition, currentTokenEndPosition - 1);
    scanner.restoreState(state);
    int currentCharacter;
    int start = currentTokenStartPosition;
    int nextCharacterStart = currentTokenStartPosition;

    // Print comment line indentation
    int commentIndentationLevel;
    boolean onFirstColumn = isOnFirstColumn(start);
    if (indentationLevel == 0) {
      commentIndentationLevel = column - 1;
    } else {
      if (onFirstColumn
          && (includesLineComments
              && !formatter.preferences.comment_format_line_comment_starting_on_first_column || formatter.preferences.never_indent_line_comments_on_first_column)) {
        commentIndentationLevel = column - 1;
      } else {
        // Indentation may be specific for contiguous comment
        // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=293300
        if (lastLineComment.contiguous) {
          // The leading spaces have been set while looping in the
          // printComment(int) method
          int currentCommentIndentation = getCurrentIndentation(lastLineComment.leadingSpaces, 0);
          // Keep the current comment indentation when over the previous
          // contiguous line comment and the previous comment has not been
          // reindented
          int relativeIndentation = currentCommentIndentation - lastLineComment.currentIndentation;
          boolean similarCommentsIndentation = false;
          if (tabLength == 0) {
            similarCommentsIndentation = relativeIndentation == 0;
          } else if (relativeIndentation > -tabLength) {
            similarCommentsIndentation = relativeIndentation == 0 || currentCommentIndentation != 0
                && lastLineComment.currentIndentation != 0;
          }
          if (similarCommentsIndentation && lastLineComment.indentation != indentationLevel) {
            int currentIndentationLevel = indentationLevel;
            indentationLevel = lastLineComment.indentation;
            printIndentationIfNecessary();
            indentationLevel = currentIndentationLevel;
            commentIndentationLevel = lastLineComment.indentation;
          } else {
            printIndentationIfNecessary();
            commentIndentationLevel = column - 1;
          }
        } else {
          if (currentAlignment != null && currentAlignment.kind == Alignment.ARRAY_INITIALIZER
              && currentAlignment.fragmentCount > 0
              && indentationLevel < currentAlignment.breakIndentationLevel
              && lastLineComment.lines > 0) {
            int currentIndentationLevel = indentationLevel;
            indentationLevel = currentAlignment.breakIndentationLevel;
            printIndentationIfNecessary();
            indentationLevel = currentIndentationLevel;
            commentIndentationLevel = currentAlignment.breakIndentationLevel;
          } else {
            printIndentationIfNecessary();
            commentIndentationLevel = column - 1;
          }
        }
      }
    }

    // Store line comment information
    lastLineComment.contiguous = true;
    lastLineComment.currentIndentation = getCurrentCommentIndentation(currentTokenStartPosition);
    lastLineComment.indentation = commentIndentationLevel;

    // Add pending space if necessary
    if (pendingSpace) {
      addInsertEdit(currentTokenStartPosition, " "); //$NON-NLS-1$
    }
    needSpace = false;
    pendingSpace = false;
    int previousStart = currentTokenStartPosition;

    if (!isNlsTag
        && includesLineComments
        && (!onFirstColumn || formatter.preferences.comment_format_line_comment_starting_on_first_column)) {
      printLineComment(currentTokenStartPosition, currentTokenEndPosition - 1);
//      currentTokenEndPosition += 1;
    } else {
      // do nothing!?
      loop : while (nextCharacterStart <= currentTokenEndPosition
          && (currentCharacter = scanner.getNextChar()) != -1) {
        nextCharacterStart = scanner.currentPosition;

        switch (currentCharacter) {
          case '\r':
            start = previousStart;
            break loop;
          case '\n':
            start = previousStart;
            break loop;
        }
        previousStart = nextCharacterStart;
      }
      if (start != currentTokenStartPosition) {
        // this means that the line comment doesn't end the file
        addReplaceEdit(start, currentTokenEndPosition - 1, lineSeparator);
        line++;
        column = 1;
        lastNumberOfNewLines = 1;
      }
    }
    needSpace = false;
    pendingSpace = false;
    // realign to the proper value
    if (currentAlignment != null) {
      if (memberAlignment != null) {
        // select the last alignment
        if (currentAlignment.location.inputOffset > memberAlignment.location.inputOffset) {
          if (currentAlignment.couldBreak() && currentAlignment.wasSplit) {
            currentAlignment.performFragmentEffect();
          }
        } else {
          indentationLevel = Math.max(indentationLevel, memberAlignment.breakIndentationLevel);
        }
      } else if (currentAlignment.couldBreak() && currentAlignment.wasSplit) {
        currentAlignment.performFragmentEffect();
      }
      if (currentAlignment.kind == Alignment.BINARY_EXPRESSION
          && currentAlignment.enclosing != null
          && currentAlignment.enclosing.kind == Alignment.BINARY_EXPRESSION
          && indentationLevel < currentAlignment.breakIndentationLevel) {
        indentationLevel = currentAlignment.breakIndentationLevel;
      }
    }
    scanner.resetTo(currentTokenEndPosition, scannerEndPosition - 1);
    scanner.skipNewline();
    scanner.restoreState(state);
  }

  private void printLineComment(int commentStart, int commentEnd) {
    // Compute indentation
    int firstColumn = column;
    int indentLevel = indentationLevel;
    int indentations = numberOfIndentations;
    indentationLevel = getNextIndentationLevel(firstColumn);
    if (indentationSize != 0) {
      numberOfIndentations = indentationLevel / indentationSize;
    } else {
      numberOfIndentations = 0;
    }

    // Consume the comment prefix
    scanner.resetTo(commentStart, commentEnd);
    scanner.getNextChar();
    scanner.getNextChar();
    column += 2;

    // Scan the text token per token to compact it and size it the max line
    // length
    int maxColumn = formatter.preferences.comment_line_length + 1;
    Token previousToken = null;
    int lastTokenEndPosition = commentStart + 2; // skip delimiter
    int spaceStartPosition = -1;
    int spaceEndPosition = -1;
    scanner.skipComments = true;
    String newLineString = null;
    commentIndentation = null;

    // Consume text token per token
    while (!scanner.atEnd()) {
      Token token;
      try {
        token = scanner.getNextToken();
      } catch (InvalidInputException iie) {
        token = consumeInvalidToken(commentEnd);
      }
      switch (token) {
        case WHITESPACE:
          if (previousToken == null) {
            // do not remember the first whitespace
            previousToken = SKIP_FIRST_WHITESPACE_TOKEN;
          } else {
            previousToken = token;
          }
          // Remember space position
          spaceStartPosition = scanner.getCurrentTokenStartPosition();
          spaceEndPosition = scanner.getCurrentTokenEndPosition();
          continue;
        case EOS:
          continue;
        case IDENTIFIER:
          if (previousToken == null || previousToken == SKIP_FIRST_WHITESPACE_TOKEN) {
            char[] identifier = scanner.getCurrentTokenString().toCharArray();
            int startPosition = scanner.getCurrentTokenStartPosition();
            int restartPosition = scanner.currentPosition;
            // length of string "$FALL" = 5
            if (CharOperation.equals(identifier, Scanner.FALL_THROUGH_TAG, 0, 5)
                && scanner.currentCharacter == '-') {
              try {
                scanner.getNextToken(); // consume the '-'
                token = scanner.getNextToken(); // consume the "THROUGH"
                if (token == Token.IDENTIFIER) {
                  identifier = scanner.getCurrentTokenString().toCharArray();
                  if (CharOperation.endsWith(Scanner.FALL_THROUGH_TAG, identifier)) {
                    // the comment starts with a fall through
                    if (previousToken == SKIP_FIRST_WHITESPACE_TOKEN) {
                      addReplaceEdit(spaceStartPosition, startPosition - 1, " "); //$NON-NLS-1$
                    }
                    scanner.startPosition = startPosition;
                    previousToken = token;
                    break;
                  }
                }
              } catch (InvalidInputException iie) {
                // skip
              }
            }
            // this was not a valid fall-through tag, hence continue to process
            // the comment normally
            scanner.resetTo(startPosition, scanner.getEndPos() - 1);
            scanner.skipComments = true;
          }
          break;
      }
      int tokenStart = scanner.getCurrentTokenStartPosition();
      int tokenLength = scanner.getCurrentTokenEndPosition() + 1
          - scanner.getCurrentTokenStartPosition();

      // insert space at the beginning if not present
      if (previousToken == null) {
        addInsertEdit(scanner.startPosition, " "); //$NON-NLS-1$
        column++;
      }
      // replace space at the beginning if present
      else if (previousToken == SKIP_FIRST_WHITESPACE_TOKEN) {
        addReplaceEdit(spaceStartPosition, scanner.startPosition - 1, " "); //$NON-NLS-1$
        column++;
        spaceStartPosition = -1; // do not use this position to split the
                                 // comment
      } else {
        // not on the first token
        boolean insertSpace = previousToken == Token.WHITESPACE;
        if (insertSpace) {
          // count inserted space if any in token length
          tokenLength++;
        }
        // insert new line if max line width is reached and a space was
        // previously encountered
        if (spaceStartPosition > 0 && column + tokenLength > maxColumn) {
          lastNumberOfNewLines++;
          line++;
          if (newLineString == null) {
            tempBuffer.setLength(0);
            tempBuffer.append(lineSeparator);
            column = 1;
            if (!formatter.preferences.never_indent_line_comments_on_first_column) {
              printIndentationIfNecessary(tempBuffer);
            }
            tempBuffer.append(LINE_COMMENT_PREFIX);
            column += LINE_COMMENT_PREFIX_LENGTH;
            newLineString = tempBuffer.toString();
            firstColumn = column;
          } else {
            column = firstColumn;
          }
          if (lastTokenEndPosition > spaceEndPosition) {
            // add all previous tokens lengths since last space
            column += lastTokenEndPosition - (spaceEndPosition + 1);
          }
          if (edits[editsIndex - 1].offset == spaceStartPosition) {
            // previous space was already edited, so remove it
            editsIndex--;
          }
          addReplaceEdit(spaceStartPosition, spaceEndPosition, newLineString);
          spaceStartPosition = -1;
          if (insertSpace) {
            tokenLength--; // reduce token length as the space will be replaced
                           // by the new line
          }
        }
        // replace space if needed
        else if (insertSpace) {
          addReplaceEdit(spaceStartPosition, scanner.startPosition - 1, " "); //$NON-NLS-1$
        }
      }
      // update column position and store info of the current token
      column += tokenLength;
      previousToken = token;
      lastTokenEndPosition = scanner.currentPosition;
    }
    scanner.skipComments = false;

    // Skip separator if the comment is not at the end of file
    indentationLevel = indentLevel;
    numberOfIndentations = indentations;
    lastNumberOfNewLines = 0;
    scanner.resetTo(lastTokenEndPosition, commentEnd + 1);// was - 1
    while (!scanner.atEnd()) {
      spaceEndPosition = scanner.currentPosition;
      int ch = scanner.getNextChar();
      if (ch == '\n' || ch == '\r') {
        // line comment is normally ended with new line
        column = 1;
        line++;
        lastNumberOfNewLines++;
        commentEnd++;
        break;
      }
    }

    // Replace the line separator at the end of the comment if any...
    int startReplace = previousToken == SKIP_FIRST_WHITESPACE_TOKEN ? spaceStartPosition
        : lastTokenEndPosition;
    if (column == 1 && commentEnd >= startReplace) {
      addReplaceEdit(startReplace, commentEnd, formatter.preferences.line_separator);
    }
  }

  /*
   * Print the indentation of a disabling comment
   */
  private void printNewLinesBeforeDisablingComment() {
    // Get the beginning of comment line
    int linePtr = Arrays.binarySearch(lineEnds, scanner.startPosition);
    if (linePtr < 0) {
      linePtr = -linePtr - 1;
    }
    int indentation = 0;
    int beginningOfLine = getLineEnd(linePtr) + 1;
    if (beginningOfLine == -1) {
      beginningOfLine = 0;
    }

    // If the comment is in the middle of the line, then there's nothing to do
    OptimizedReplaceEdit currentEdit = edits[editsIndex - 1];
    int offset = currentEdit.offset;
    if (offset >= beginningOfLine) {
      return;
    }

    // Compute the comment indentation
    int scannerStartPosition = scanner.startPosition;
    int scannerEndPosition = scanner.source.length - 1;
// int scannerEofPosition = this.scanner.eofPosition;
// int scannerCurrentPosition = this.scanner.currentPosition;
// char scannerCurrentChar = this.scanner.currentCharacter;
    int length = currentEdit.length;
    scanner.resetTo(beginningOfLine, offset + length - 1);
    String ws = scanner.peekWhitespace();
    try {
//       while (!this.scanner.atEnd()) {
//       char ch = (char) this.scanner.getNextChar();
      for (char ch : ws.toCharArray()) {
        switch (ch) {
          case '\t':
            if (tabLength != 0) {
              int reminder = indentation % tabLength;
              if (reminder == 0) {
                indentation += tabLength;
              } else {
                indentation = (indentation / tabLength + 1) * tabLength;
              }
            }
            break;
          case ' ':
            indentation++;
            break;
          default:
            // Should not happen as the offset of the edit is before the
            // beginning of line
            return;
        }
      }

      // Split the existing edit to keep the change before the beginning of the
      // last line but change the indentation after. Note that at this stage,
      // the add*Edit methods cannot be longer used as the edits are disabled
      String indentationString;
      int currentIndentation = getCurrentIndentation(scanner.currentPosition);
      if (currentIndentation > 0 && indentationLevel > 0) {
        int col = column;
        tempBuffer.setLength(0);
        printIndentationIfNecessary(tempBuffer);
        indentationString = tempBuffer.toString();
        column = col;
      } else {
        indentationString = Util.EMPTY_STRING;
      }
      String replacement = currentEdit.replacement;
      if (replacement.length() == 0) {
        // previous edit was a delete, as we're sure to have a new line before
        // the comment, then the edit needs to be either replaced entirely with
        // the expected indentation
        edits[editsIndex - 1] = new OptimizedReplaceEdit(beginningOfLine, offset + length
            - beginningOfLine, indentationString);
      } else {
        int idx = replacement.lastIndexOf(lineSeparator);
        if (idx >= 0) {
          // replace current edit if it contains a line separator
          int start = idx + lsLength;
          tempBuffer.setLength(0);
          tempBuffer.append(replacement.substring(0, start));
          if (indentationString != Util.EMPTY_STRING) {
            tempBuffer.append(indentationString);
          }
          edits[editsIndex - 1] = new OptimizedReplaceEdit(offset, length, tempBuffer.toString());
        }
      }
    } finally {
      scanner.resetTo(scannerStartPosition, scannerEndPosition);
// this.scanner.startPosition = scannerStartPosition;
// this.scanner.eofPosition = scannerEofPosition;
// this.scanner.currentPosition = scannerCurrentPosition;
// this.scanner.currentCharacter = scannerCurrentChar;
    }
  }

  /*
   * Print new lines characters when the edits are disabled. In this case, only the line separator
   * is replaced if necessary, the other white spaces are untouched.
   */
  private boolean printNewLinesCharacters(int offset, int length) {
    boolean foundNewLine = false;
    scanner.resetTo(offset, offset + length - 1);
    int lineCount = scanner.countLinesBetween(offset, offset + length);
    // we are not actually changing anything, just detecting a newline
    return lineCount > 0;
  }

  private void printNextTokenRaw(Token expectedTokenType, boolean considerSpaceIfAny) {
    try {
      currentToken = scanner.getNextToken();
      if (expectedTokenType != currentToken) {
        throw new AbortFormatting(
            "unexpected token type, expecting:" + expectedTokenType + ", actual:" + currentToken);//$NON-NLS-1$//$NON-NLS-2$
      }
      print(scanner.currentPosition - scanner.startPosition, considerSpaceIfAny);
    } catch (InvalidInputException e) {
      throw new AbortFormatting(e);
    }
  }

  private void printRule(StringBuffer stringBuffer) {
    // only called if this.tabLength > 0
    for (int i = 0; i < pageWidth; i++) {
      if (i % tabLength == 0) {
        stringBuffer.append('+');
      } else {
        stringBuffer.append('-');
      }
    }
    stringBuffer.append(lineSeparator);

    for (int i = 0; i < pageWidth / tabLength; i++) {
      stringBuffer.append(i);
      stringBuffer.append('\t');
    }
  }

  private void resetAt(Location location) {
    line = location.outputLine;
    column = location.outputColumn;
    indentationLevel = location.outputIndentationLevel;
    numberOfIndentations = location.numberOfIndentations;
    lastNumberOfNewLines = location.lastNumberOfNewLines;
    needSpace = location.needSpace;
    pendingSpace = location.pendingSpace;
    editsIndex = location.editsIndex;
    nlsTagCounter = location.nlsTagCounter;
    if (editsIndex > 0) {
      edits[editsIndex - 1] = location.textEdit;
    }
    formatter.lastLocalDeclarationSourceStart = location.lastLocalDeclarationSourceStart;
  }

  private void resize() {
    System.arraycopy(edits, 0, (edits = new OptimizedReplaceEdit[editsIndex * 2]), 0, editsIndex);
  }

  private void setCommentIndentation(int commentIndentationLevel) {
    if (commentIndentationLevel == 0) {
      commentIndentation = null;
    } else {
      int length = COMMENT_INDENTATIONS.length;
      if (commentIndentationLevel > length) {
        System.arraycopy(
            COMMENT_INDENTATIONS,
            0,
            COMMENT_INDENTATIONS = new String[commentIndentationLevel + 10],
            0,
            length);
      }
      commentIndentation = COMMENT_INDENTATIONS[commentIndentationLevel - 1];
      if (commentIndentation == null) {
        tempBuffer.setLength(0);
        for (int i = 0; i < commentIndentationLevel; i++) {
          tempBuffer.append(' ');
        }
        commentIndentation = tempBuffer.toString();
        COMMENT_INDENTATIONS[commentIndentationLevel - 1] = commentIndentation;
      }
    }
  }

  /*
   * Look for the tags identified by the scanner to see whether some of them may change the status
   * of the edition for the formatter. Do not return as soon as a match is found, as there may have
   * several disabling/enabling tags in a comment, hence the last one will be the one really
   * changing the formatter behavior...
   */
  private void setEditsEnabled(int count) {
    DartCore.notYetImplemented();
    // for (int i = 0; i < count; i++) {
    // if (this.disablingTag != null
    // && CharOperation.equals(this.scanner.foundTaskTags[i],
    // this.disablingTag)) {
    // this.editsEnabled = false;
    // }
    // if (this.enablingTag != null
    // && CharOperation.equals(this.scanner.foundTaskTags[i],
    // this.enablingTag)) {
    // this.editsEnabled = true;
    // }
    // }
  }

  private int useAlignmentBreakIndentation(int emptyLinesRules) {
    // preserve line breaks in wrapping if specified
    // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=198074
    boolean specificEmptyLinesRule = emptyLinesRules != PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION;
    if ((currentAlignment != null || specificEmptyLinesRule)
        && !formatter.preferences.join_wrapped_lines) {
      // insert a new line only if it has not been already done before
      // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=283476
      if (lastNumberOfNewLines == 0 || specificEmptyLinesRule
          || formatter.arrayInitializersDepth >= 0) {

        // Do not use alignment break indentation in specific circumstances
        boolean useAlignmentBreakIndentation;
        boolean useAlignmentShiftBreakIndentation = false;
        boolean useLastBinaryExpressionAlignmentBreakIndentation = false;
        switch (emptyLinesRules) {
          case DO_NOT_PRESERVE_EMPTY_LINES:
          case PRESERVE_EMPTY_LINES_IN_SWITCH_CASE:
          case PRESERVE_EMPTY_LINES_AT_END_OF_METHOD_DECLARATION:
          case PRESERVE_EMPTY_LINES_AT_END_OF_BLOCK:
            return PRESERVE_EMPTY_LINES_DO_NOT_USE_ANY_INDENTATION;
          case PRESERVE_EMPTY_LINES_IN_BINARY_EXPRESSION:
            useAlignmentBreakIndentation = true;
            if ((formatter.expressionsPos & CodeFormatterVisitor.EXPRESSIONS_POS_MASK) == CodeFormatterVisitor.EXPRESSIONS_POS_BETWEEN_TWO) {
              // we're just before the left expression, try to use the last
              // binary expression break indentation if any
              useLastBinaryExpressionAlignmentBreakIndentation = true;
            }
            break;
          case PRESERVE_EMPTY_LINES_IN_EQUALITY_EXPRESSION:
            useAlignmentShiftBreakIndentation = currentAlignment == null
                || currentAlignment.kind == Alignment.BINARY_EXPRESSION;
            useAlignmentBreakIndentation = !useAlignmentShiftBreakIndentation;
            break;
          case PRESERVE_EMPTY_LINES_IN_FORMAT_OPENING_BRACE:
            useAlignmentBreakIndentation = formatter.arrayInitializersDepth <= 1
                && currentAlignment != null && currentAlignment.kind == Alignment.ARRAY_INITIALIZER;
            break;
          case PRESERVE_EMPTY_LINES_IN_FORMAT_LEFT_CURLY_BRACE:
            useAlignmentBreakIndentation = false;
            break;
          default:
            if ((emptyLinesRules & 0xFFFF) == PRESERVE_EMPTY_LINES_IN_CLOSING_ARRAY_INITIALIZER
                && scanner.currentCharacter == '}') {
              // last array initializer closing brace
              indentationLevel = emptyLinesRules >> 16;
              preserveLineBreakIndentation = true;
              return PRESERVE_EMPTY_LINES_USE_CURRENT_INDENTATION;
            }
            useAlignmentBreakIndentation = true;
            break;
        }

        // If there's an alignment try to align on its break indentation level
        Alignment alignment = currentAlignment;
        if (alignment == null) {
          if (useLastBinaryExpressionAlignmentBreakIndentation) {
            if (indentationLevel < formatter.lastBinaryExpressionAlignmentBreakIndentation) {
              indentationLevel = formatter.lastBinaryExpressionAlignmentBreakIndentation;
            }
          }
          if (useAlignmentShiftBreakIndentation && memberAlignment != null) {
            if (indentationLevel < memberAlignment.shiftBreakIndentationLevel) {
              indentationLevel = memberAlignment.shiftBreakIndentationLevel;
            }
          }
        } else {
          // Use the member alignment break indentation level when
          // it's closer from the wrapped line than the current alignment
          if (memberAlignment != null
              && memberAlignment.location.inputOffset > alignment.location.inputOffset) {
            alignment = memberAlignment;
          }

          // Use the break indentation level if possible...
          if (useLastBinaryExpressionAlignmentBreakIndentation) {
            if (indentationLevel < formatter.lastBinaryExpressionAlignmentBreakIndentation) {
              indentationLevel = formatter.lastBinaryExpressionAlignmentBreakIndentation;
            }
          }
          if (useAlignmentBreakIndentation) {
            if (indentationLevel < alignment.breakIndentationLevel) {
              indentationLevel = alignment.breakIndentationLevel;
            }
          } else if (useAlignmentShiftBreakIndentation) {
            if (indentationLevel < alignment.shiftBreakIndentationLevel) {
              indentationLevel = alignment.shiftBreakIndentationLevel;
            }
          }
        }
        preserveLineBreakIndentation = true;
        if (useLastBinaryExpressionAlignmentBreakIndentation || useAlignmentShiftBreakIndentation) {
          return PRESERVE_EMPTY_LINES_USE_TEMPORARY_INDENTATION;
        }
        return PRESERVE_EMPTY_LINES_USE_CURRENT_INDENTATION;
      }
    }
    return PRESERVE_EMPTY_LINES_DO_NOT_USE_ANY_INDENTATION;
  }
}
