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
package com.google.dart.engine.services.internal.correction;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.services.internal.util.ExecutionUtils;
import com.google.dart.engine.services.internal.util.RunnableObjectEx;
import com.google.dart.engine.services.internal.util.TokenUtils;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNodes;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

import org.apache.commons.lang3.StringUtils;

import java.nio.CharBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utilities for analyzing {@link CompilationUnit}, its parts and source.
 */
public class CorrectionUtils {

  /**
   * Describes where to insert new directive or top-level declaration at the top of file.
   */
  public class TopInsertDesc {
    public int offset;
    public boolean insertEmptyLineBefore;
    public boolean insertEmptyLineAfter;
  }

  /**
   * The default end-of-line marker for the current platform.
   */
  public static final String DEFAULT_END_OF_LINE = System.getProperty("line.separator", "\n");

  /**
   * @return the updated {@link String} with applied {@link Edit}s.
   */
  public static String applyReplaceEdits(String s, List<Edit> edits) {
    // sort edits
    edits = Lists.newArrayList(edits);
    Collections.sort(edits, new Comparator<Edit>() {
      @Override
      public int compare(Edit o1, Edit o2) {
        return o1.offset - o2.offset;
      }
    });
    // apply edits
    int delta = 0;
    for (Edit edit : edits) {
      int editOffset = edit.offset + delta;
      String beforeEdit = s.substring(0, editOffset);
      String afterEdit = s.substring(editOffset + edit.length);
      s = beforeEdit + edit.replacement + afterEdit;
      delta += getDeltaOffset(edit);
    }
    // done
    return s;
  }

  /**
   * @return <code>true</code> if given {@link SourceRange} covers given {@link ASTNode}.
   */
  public static boolean covers(SourceRange r, ASTNode node) {
    SourceRange nodeRange = rangeNode(node);
    return r.covers(nodeRange);
  }

  /**
   * @return the number of characters this {@link Edit} will move offsets after its range.
   */
  public static int getDeltaOffset(Edit edit) {
    return edit.replacement.length() - edit.length;
  }

  /**
   * @return given {@link Statement} if not {@link Block}, first child {@link Statement} if
   *         {@link Block}, or <code>null</code> if more than one child.
   */
  public static Statement getSingleStatement(Statement statement) {
    if (statement instanceof Block) {
      List<Statement> blockStatements = ((Block) statement).getStatements();
      if (blockStatements.size() != 1) {
        return null;
      }
      return blockStatements.get(0);
    }
    return statement;
  }

  /**
   * @return given {@link DartStatement} if not {@link DartBlock}, all children
   *         {@link DartStatement}s if {@link DartBlock}.
   */
  public static List<Statement> getStatements(Statement statement) {
    if (statement instanceof Block) {
      return ((Block) statement).getStatements();
    }
    return ImmutableList.of(statement);
  }

  /**
   * @return the whitespace prefix of the given {@link String}.
   */
  public static String getStringPrefix(String s) {
    int index = CharMatcher.WHITESPACE.negate().indexIn(s);
    if (index == -1) {
      return s;
    }
    return s.substring(0, index);
  }

//  /**
//   * @return <code>true</code> if given {@link BinaryExpression} uses associative operator and its
//   *         arguments are not {@link BinaryExpression} or are also associative.
//   */
//  public static boolean isAssociative(BinaryExpression expression) {
//    if (expression.getOperator().getType().isAssociativeOperator()) {
//      Expression left = expression.getLeftOperand();
//      Expression right = expression.getRightOperand();
//      if (left instanceof BinaryExpression && !isAssociative((BinaryExpression) left)) {
//        return false;
//      }
//      if (right instanceof BinaryExpression && !isAssociative((BinaryExpression) right)) {
//        return false;
//      }
//      return true;
//    }
//    return false;
//  }
//
//  public static boolean rangeEndsBetween(SourceRange range, ASTNode first, ASTNode next) {
//    int pos = range.getEnd();
//    return first.getEnd() <= pos && pos <= next.getOffset();
//  }
//
//  public static boolean rangeStartsBetween(SourceRange range, ASTNode first, ASTNode next) {
//    int pos = range.getOffset();
//    return first.getEnd() <= pos && pos <= next.getOffset();
//  }
//
//  /**
//   * @return {@link Expression}s from <code>operands</code> which are completely covered by given
//   *         {@link SourceRange}. Range should start and end between given {@link Expression}s.
//   */
//  private static List<Expression> getOperandsForSourceRange(List<Expression> operands,
//      SourceRange range) {
//    assert !operands.isEmpty();
//    List<Expression> subOperands = Lists.newArrayList();
//    // track range enter/exit
//    boolean entered = false;
//    boolean exited = false;
//    // may be range starts exactly on first operand
//    if (range.getOffset() == operands.get(0).getOffset()) {
//      entered = true;
//    }
//    // iterate over gaps between operands
//    for (int i = 0; i < operands.size() - 1; i++) {
//      Expression operand = operands.get(i);
//      Expression nextOperand = operands.get(i + 1);
//      // add operand, if already entered range
//      if (entered) {
//        subOperands.add(operand);
//        // may be last operand in range
//        if (CorrectionUtils.rangeEndsBetween(range, operand, nextOperand)) {
//          exited = true;
//        }
//      } else {
//        // may be first operand in range
//        if (CorrectionUtils.rangeStartsBetween(range, operand, nextOperand)) {
//          entered = true;
//        }
//      }
//    }
//    // check if last operand is in range
//    Expression lastGroupMember = operands.get(operands.size() - 1);
//    if (range.getEnd() == lastGroupMember.getEnd()) {
//      subOperands.add(lastGroupMember);
//      exited = true;
//    }
//    // we expect that range covers only given operands
//    if (!exited) {
//      return Lists.newArrayList();
//    }
//    // done
//    return subOperands;
//  }
//
//  /**
//   * @return all operands of the given {@link BinaryExpression} and its children with the same
//   *         operator.
//   */
//  private static List<Expression> getOperandsInOrderFor(final BinaryExpression groupRoot) {
//    final List<Expression> operands = Lists.newArrayList();
//    groupRoot.accept(new GeneralizingASTVisitor<Void>() {
//      @Override
//      public Void visitExpression(Expression node) {
//        if (node instanceof BinaryExpression
//            && ((BinaryExpression) node).getOperator() == groupRoot.getOperator()) {
//          return super.visitNode(node);
//        }
//        operands.add(node);
//        return null;
//      }
//    });
//    return operands;
//  }

  /**
   * @return the actual type source of the given {@link Expression}, may be <code>null</code> if can
   *         not be resolved, should be treated as <code>Dynamic</code>.
   */
  public static String getTypeSource(Expression expression) {
    if (expression == null) {
      return null;
    }
    Type type = expression.getStaticType();
    String typeSource = getTypeSource(type);
    if ("dynamic".equals(typeSource)) {
      return null;
    }
    return typeSource;
  }

  /**
   * @return the source of the given {@link Type}.
   */
  public static String getTypeSource(Type type) {
    String typeSource = type.toString();
    typeSource = StringUtils.substringBefore(typeSource, "<");
    return typeSource;
  }

  private final CompilationUnit unit;

  private String buffer;

  private String endOfLine;

  public CorrectionUtils(CompilationUnit unit) throws Exception {
    this.unit = unit;
    unit.getElement().getSource().getContents(new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents) {
        buffer = contents.toString();
      }

      @Override
      public void accept(String contents) {
        buffer = contents;
      }
    });
  }

  /**
   * @return the source of the given {@link SourceRange} with indentation changed from "oldIndent"
   *         to "newIndent", keeping indentation of the lines relative to each other.
   */
  public Edit createIndentEdit(SourceRange range, String oldIndent, String newIndent) {
    String newSource = getIndentSource(range, oldIndent, newIndent);
    return new Edit(range.getOffset(), range.getLength(), newSource);
  }

  /**
   * @return the EOL to use for this {@link CompilationUnit}.
   */
  public String getEndOfLine() {
    if (endOfLine == null) {
      endOfLine = ExecutionUtils.runObjectIgnore(new RunnableObjectEx<String>() {
        @Override
        public String runObject() throws Exception {
          // try to find Windows
          if (buffer.contains("\r\n")) {
            return "\r\n";
          }
          // use default
          return DEFAULT_END_OF_LINE;
        }
      }, DEFAULT_END_OF_LINE);
    }
    return endOfLine;
  }

  /**
   * @return the default indentation with given level.
   */
  public String getIndent(int level) {
    return StringUtils.repeat("  ", level);
  }

  /**
   * @return the source of the given {@link SourceRange} with indentation changed from "oldIndent"
   *         to "newIndent", keeping indentation of the lines relative to each other.
   */
  public String getIndentSource(SourceRange range, String oldIndent, String newIndent) {
    String oldSource = getText(range);
    return getIndentSource(oldSource, oldIndent, newIndent);
  }

  /**
   * @return the source with indentation changed from "oldIndent" to "newIndent", keeping
   *         indentation of the lines relative to each other.
   */
  public String getIndentSource(String source, String oldIndent, String newIndent) {
    StringBuilder sb = new StringBuilder();
    String eol = getEndOfLine();
    String[] lines = StringUtils.splitPreserveAllTokens(source, eol);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      // last line, stop if empty
      if (i == lines.length - 1 && StringUtils.isEmpty(line)) {
        break;
      }
      // line should have new indent
      line = newIndent + StringUtils.removeStart(line, oldIndent);
      // append line
      sb.append(line);
      sb.append(eol);
    }
    return sb.toString();
  }

  /**
   * Skips whitespace characters and single EOL on the right from the given position. If from
   * statement or method end, then this is in the most cases start of the next line.
   */
  public int getLineContentEnd(int index) {
    int length = buffer.length();
    // skip whitespace characters
    while (index < length) {
      char c = buffer.charAt(index);
      if (!Character.isWhitespace(c) || c == '\r' || c == '\n') {
        break;
      }
      index++;
    }
    // skip single \r
    if (index < length && buffer.charAt(index) == '\r') {
      index++;
    }
    // skip single \n
    if (index < length && buffer.charAt(index) == '\n') {
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the index of the last space or tab on the left from the given one, if from statement or
   *         method start, then this is in most cases start of the line.
   */
  public int getLineContentStart(int index) {
    while (index > 0) {
      char c = buffer.charAt(index - 1);
      if (c != ' ' && c != '\t') {
        break;
      }
      index--;
    }
    return index;
  }

  /**
   * @return the start index of the next line after the line which contains given index.
   */
  public int getLineNext(int index) {
    int length = buffer.length();
    // skip whitespace characters
    while (index < length) {
      char c = buffer.charAt(index);
      if (c == '\r' || c == '\n') {
        break;
      }
      index++;
    }
    // skip single \r
    if (index < length && buffer.charAt(index) == '\r') {
      index++;
    }
    // skip single \n
    if (index < length && buffer.charAt(index) == '\n') {
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the whitespace prefix of the line which contains given offset.
   */
  public String getLinePrefix(int index) {
    int lineStart = getLineThis(index);
    int length = buffer.length();
    int lineNonWhitespace = lineStart;
    while (lineNonWhitespace < length) {
      char c = buffer.charAt(lineNonWhitespace);
      if (c == '\r' || c == '\n') {
        break;
      }
      if (!Character.isWhitespace(c)) {
        break;
      }
      lineNonWhitespace++;
    }
    return getText(lineStart, lineNonWhitespace - lineStart);
  }

  /**
   * @return the {@link #getLinesRange(SourceRange)} for given {@link DartStatement}s.
   */
  public SourceRange getLinesRange(List<Statement> statements) {
    SourceRange range = rangeNodes(statements);
    return getLinesRange(range);
  }

  /**
   * @return the {@link SourceRange} which starts at the start of the line of "offset" and ends at
   *         the start of the next line after "end" of the given {@link SourceRange}, i.e. basically
   *         complete lines of the source for given {@link SourceRange}.
   */
  public SourceRange getLinesRange(SourceRange range) {
    // start
    int startOffset = range.getOffset();
    int startLineOffset = getLineContentStart(startOffset);
    // end
    int endOffset = range.getEnd();
    int afterEndLineOffset = getLineContentEnd(endOffset);
    // range
    return rangeStartEnd(startLineOffset, afterEndLineOffset);
  }

  /**
   * @return the start index of the line which contains given index.
   */
  public int getLineThis(int index) {
    while (index > 0) {
      char c = buffer.charAt(index - 1);
      if (c == '\r' || c == '\n') {
        break;
      }
      index--;
    }
    return index;
  }

  /**
   * @return the line prefix consisting of spaces and tabs on the left from the given
   *         {@link ASTNode}.
   */
  public String getNodePrefix(ASTNode node) {
    int offset = node.getOffset();
    // function literal is special, it uses offset of enclosing line
    if (node instanceof FunctionExpression) {
      return getLinePrefix(offset);
    }
    // use just prefix directly before node
    return getPrefix(offset);
  }

  /**
   * @return the index of the first non-whitespace character after given index.
   */
  public int getNonWhitespaceForward(int index) {
    int length = buffer.length();
    // skip whitespace characters
    while (index < length) {
      char c = buffer.charAt(index);
      if (!Character.isWhitespace(c)) {
        break;
      }
      index++;
    }
    // done
    return index;
  }

  /**
   * @return the line prefix consisting of spaces and tabs on the left from the given offset.
   */
  public String getPrefix(int endIndex) {
    int startIndex = getLineContentStart(endIndex);
    return buffer.substring(startIndex, endIndex);
  }

  /**
   * @return the full text of unit.
   */
  public String getText() {
    return buffer;
  }

  /**
   * @return the given range of text from unit.
   */
  public String getText(ASTNode node) {
    return getText(node.getOffset(), node.getLength());
  }

  /**
   * @return the given range of text from unit.
   */
  public String getText(int offset, int length) {
    return buffer.substring(offset, offset + length);
  }

//  /**
//   * @return {@link TopInsertDesc}, description where to insert new directive or top-level
//   *         declaration at the top of file.
//   */
//  public TopInsertDesc getTopInsertDesc() {
//    // skip leading line comments
//    int offset = 0;
//    boolean insertEmptyLineBefore = false;
//    boolean insertEmptyLineAfter = false;
//    String source = getText();
//    // skip hash-bang
//    if (offset < source.length() - 2) {
//      String linePrefix = getText(offset, 2);
//      if (linePrefix.equals("#!")) {
//        insertEmptyLineBefore = true;
//        offset = getLineNext(offset);
//        // skip empty lines to first line comment
//        int emptyOffset = offset;
//        while (emptyOffset < source.length() - 2) {
//          int nextLineOffset = getLineNext(emptyOffset);
//          String line = source.substring(emptyOffset, nextLineOffset);
//          if (line.trim().isEmpty()) {
//            emptyOffset = nextLineOffset;
//            continue;
//          } else if (line.startsWith("//")) {
//            offset = emptyOffset;
//            break;
//          } else {
//            break;
//          }
//        }
//      }
//    }
//    // skip line comments
//    while (offset < source.length() - 2) {
//      String linePrefix = getText(offset, 2);
//      if (linePrefix.equals("//")) {
//        insertEmptyLineBefore = true;
//        offset = getLineNext(offset);
//      } else {
//        break;
//      }
//    }
//    // determine if empty line required
//    int nextLineOffset = getLineNext(offset);
//    String insertLine = source.substring(offset, nextLineOffset);
//    if (!insertLine.trim().isEmpty()) {
//      insertEmptyLineAfter = true;
//    }
//    // fill TopInsertDesc
//    TopInsertDesc desc = new TopInsertDesc();
//    desc.offset = offset;
//    desc.insertEmptyLineBefore = insertEmptyLineBefore;
//    desc.insertEmptyLineAfter = insertEmptyLineAfter;
//    return desc;
//  }

  /**
   * @return the given range of text from unit.
   */
  public String getText(SourceRange range) {
    return getText(range.getOffset(), range.getLength());
  }

//  /**
//   * @return the offset of the token on the right from given "offset" on the same line or offset of
//   *         the next line.
//   */
//  public int getTokenOrNextLineOffset(int offset) {
//    int nextOffset = getLineContentEnd(offset);
//    String sourceToNext = getText(offset, nextOffset - offset);
//    List<Token> tokens = TokenUtils.getTokens(sourceToNext);
//    if (tokens.isEmpty()) {
//      return nextOffset;
//    }
//    return tokens.get(0).getOffset();
//  }

  /**
   * @return the underlying {@link CompilationUnit}.
   */
  public CompilationUnit getUnit() {
    return unit;
  }

//  public boolean rangeIncludesNonWhitespaceOutsideNode(SourceRange range, ASTNode node) {
//    return rangeIncludesNonWhitespaceOutsideRange(range, rangeNode(node));
//  }
//
//  public boolean rangeIncludesNonWhitespaceOutsideRange(SourceRange selection, SourceRange node) {
//    // selection should cover node
//    if (!selection.covers(node)) {
//      return false;
//    }
//    // non-whitespace between selection start and node start
//    if (!isJustWhitespace(rangeStartStart(selection, node))) {
//      return true;
//    }
//    // non-whitespace after node
//    if (!isJustWhitespaceOrComment(rangeEndEnd(node, selection))) {
//      return true;
//    }
//    // only whitespace in selection around node
//    return false;
//  }
//
//  /**
//   * @return <code>true</code> if given range of {@link BinaryExpression} can be extracted.
//   */
//  public boolean validateBinaryExpressionRange(BinaryExpression binaryExpression, SourceRange range) {
//    // only parts of associative expression are safe to extract
//    if (!isAssociative(binaryExpression)) {
//      return false;
//    }
//    // prepare selected operands
//    List<Expression> operands = getOperandsInOrderFor(binaryExpression);
//    List<Expression> subOperands = getOperandsForSourceRange(operands, range);
//    // if empty, then something wrong with selection
//    if (subOperands.isEmpty()) {
//      return false;
//    }
//    // may be some punctuation included into selection - operators, braces, etc
//    if (selectionIncludesNonWhitespaceOutsideOperands(subOperands, range)) {
//      return false;
//    }
//    // OK
//    return true;
//  }
//
//  private boolean selectionIncludesNonWhitespaceOutsideOperands(List<Expression> operands,
//      SourceRange selectionRange) {
//    return rangeIncludesNonWhitespaceOutsideRange(selectionRange, rangeNodes(operands));
//  }

  /**
   * @return <code>true</code> if selection range contains only whitespace.
   */
  public boolean isJustWhitespace(SourceRange range) {
    return getText(range).trim().length() == 0;
  }

  /**
   * @return <code>true</code> if selection range contains only whitespace or comments
   */
  public boolean isJustWhitespaceOrComment(SourceRange range) {
    final String trimmedText = getText(range).trim();
    // may be whitespace
    if (trimmedText.isEmpty()) {
      return true;
    }
    // may be comment
    return TokenUtils.getTokens(trimmedText).isEmpty();
  }

}
