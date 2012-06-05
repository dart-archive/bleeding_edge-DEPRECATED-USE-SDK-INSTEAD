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
package com.google.dart.tools.internal.corext.refactoring.code;

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.Token;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.internal.util.SourceRangeUtils;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.internal.corext.dom.ASTNodes;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableObjectEx;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import java.util.List;

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractUtils {

  /**
   * The default end-of-line marker for the current platform. This value should (almost) never be
   * used directly. The end-of-line marker should always be queried from {@link Buffer} because it
   * can differ from the platform default in some situations.
   */
  public static final String DEFAULT_END_OF_LINE = System.getProperty("line.separator", "\n");

  /**
   * @return <code>true</code> if given {@link SourceRange} covers given {@link DartNode}.
   */
  public static boolean covers(SourceRange r, DartNode node) {
    return SourceRangeUtils.covers(r, new SourceRangeImpl(node));
  }

  /**
   * @return the actual type source of the given {@link DartExpression}, may be <code>null</code> if
   *         can not be resolved, should be treated as <code>Dynamic</code>.
   */
  public static String getTypeSource(DartExpression expression) {
    if (expression == null) {
      return null;
    }
    Type type = expression.getType();
    String typeSource = getTypeSource(type);
    if ("Dynamic".equals(typeSource)) {
      return null;
    }
    return typeSource;
  }

  /**
   * @return the source of the given {@link Type}.
   */
  public static String getTypeSource(Type type) {
    String typeSource = type.toString();
    typeSource = StringUtils.replace(typeSource, "<dynamic>", "Dynamic");
    typeSource = StringUtils.replace(typeSource, "<Dynamic>", "");
    typeSource = StringUtils.replace(typeSource, "<Dynamic, Dynamic>", "");
    return typeSource;
  }

  /**
   * @return <code>true</code> if given {@link DartBinaryExpression} uses associative operator and
   *         its arguments are not {@link DartBinaryExpression} or are also associative.
   */
  public static boolean isAssociative(DartBinaryExpression expression) {
    if (isAssociativeOperator(expression.getOperator())) {
      DartExpression arg1 = expression.getArg1();
      DartExpression arg2 = expression.getArg2();
      if (arg1 instanceof DartBinaryExpression && !isAssociative((DartBinaryExpression) arg1)) {
        return false;
      }
      if (arg2 instanceof DartBinaryExpression && !isAssociative((DartBinaryExpression) arg2)) {
        return false;
      }
      return true;
    }
    return false;
  }

  public static boolean rangeEndsBetween(SourceRange range, DartNode first, DartNode next) {
    int pos = SourceRangeUtils.getEnd(range);
    return first.getSourceInfo().getEnd() <= pos && pos <= next.getSourceInfo().getOffset();
  }

  public static boolean rangeStartsBetween(SourceRange range, DartNode first, DartNode next) {
    int pos = range.getOffset();
    return first.getSourceInfo().getEnd() <= pos && pos <= next.getSourceInfo().getOffset();
  }

  /**
   * @return <code>true</code> if given operator {@link Token} is associative.
   */
  private static boolean isAssociativeOperator(Token operator) {
    return operator == Token.ADD || operator == Token.MUL || operator == Token.BIT_XOR
        || operator == Token.BIT_OR || operator == Token.BIT_AND || operator == Token.OR
        || operator == Token.AND;
  }

  /**
   * @return {@link com.google.dart.engine.scanner.Token}s of the given Dart source.
   */
  private static List<com.google.dart.engine.scanner.Token> tokenizeSource(final String s) {
    final List<com.google.dart.engine.scanner.Token> tokens = Lists.newArrayList();
    ExecutionUtils.runIgnore(new RunnableEx() {
      @Override
      public void run() throws Exception {
        StringScanner scanner = new StringScanner(null, s, null);
        com.google.dart.engine.scanner.Token token = scanner.tokenize();
        while (token.getType() != TokenType.EOF) {
          tokens.add(token);
          token = token.getNext();
        }
      }
    });
    return tokens;
  }

  @SuppressWarnings("unused")
  private final CompilationUnit unit;

  private final Buffer buffer;

  private final DartUnit unitNode;

  private String endOfLine;

  public ExtractUtils(CompilationUnit unit) throws DartModelException {
    this.unit = unit;
    this.buffer = unit.getBuffer();
    this.unitNode = DartCompilerUtilities.resolveUnit(unit);
  }

  /**
   * @return the EOL to use for this {@link CompilationUnit}.
   */
  public String getEndOfLine() {
    if (endOfLine == null) {
      endOfLine = ExecutionUtils.runObjectIgnore(new RunnableObjectEx<String>() {
        @Override
        public String runObject() throws Exception {
          // find first EOL
          IDocument document = new Document(buffer.getContents());
          int numberOfLines = document.getNumberOfLines();
          for (int i = 0; i < numberOfLines; i++) {
            String delimiter = document.getLineDelimiter(i);
            if (delimiter != null) {
              return delimiter;
            }
          }
          // no EOL, use default
          return DEFAULT_END_OF_LINE;
        }
      }, DEFAULT_END_OF_LINE);
    }
    return endOfLine;
  }

  /**
   * @return the line prefix consisting of spaces and tabs on the left from the given
   *         {@link DartNode}.
   */
  public String getNodePrefix(DartNode node) {
    int endIndex = node.getSourceInfo().getOffset();
    int startIndex = getNodePrefixStartIndex(node);
    return buffer.getText(startIndex, endIndex - startIndex);
  }

  /**
   * @return all occurrences of the source which matches given selection, sorted by offset. First
   *         {@link SourceRange} is same as the given selection. May be empty, but not
   *         <code>null</code>.
   */
  public List<SourceRange> getOccurrences(int selectionStart, int selectionLength)
      throws DartModelException {
    List<SourceRange> occurrences = Lists.newArrayList();
    // prepare selection
    SourceRange selectionRange = SourceRangeFactory.forStartLength(selectionStart, selectionLength);
    String selectionSource = getText(selectionStart, selectionLength);
    List<com.google.dart.engine.scanner.Token> selectionTokens = tokenizeSource(selectionSource);
    selectionSource = StringUtils.join(selectionTokens, ' ');
    // prepare enclosing function
    DartNode selectionNode = NodeFinder.perform(unitNode, selectionStart, 0);
    DartFunction function = ASTNodes.getAncestor(selectionNode, DartFunction.class);
    // ...we need function
    if (function != null) {
      SourceInfo functionSourceInfo = function.getBody().getSourceInfo();
      int functionOffset = functionSourceInfo.getOffset();
      String functionSource = getText(functionOffset, functionSourceInfo.getLength());
      // prepare function tokens
      List<com.google.dart.engine.scanner.Token> functionTokens = tokenizeSource(functionSource);
      functionSource = StringUtils.join(functionTokens, ' ');
      // find "selection" in "function" tokens
      int lastIndex = 0;
      while (true) {
        // find next occurrence
        int index = functionSource.indexOf(selectionSource, lastIndex);
        if (index == -1) {
          break;
        }
        lastIndex = index + selectionSource.length();
        // find start/end tokens
        int startTokenIndex = StringUtils.countMatches(functionSource.substring(0, index), " ");
        int endTokenIndex = StringUtils.countMatches(functionSource.substring(0, lastIndex), " ");
        com.google.dart.engine.scanner.Token startToken = functionTokens.get(startTokenIndex);
        com.google.dart.engine.scanner.Token endToken = functionTokens.get(endTokenIndex);
        // add occurrence range
        int occuStart = functionOffset + startToken.getOffset();
        int occuEnd = functionOffset + endToken.getOffset() + endToken.getLength();
        SourceRange occuRange = SourceRangeFactory.forStartEnd(occuStart, occuEnd);
        if (SourceRangeUtils.intersects(occuRange, selectionRange)) {
          occurrences.add(selectionRange);
        } else {
          occurrences.add(occuRange);
        }
      }
    }
    // done
    return occurrences;
  }

  /**
   * @return the given range of text from {@link Buffer}.
   */
  public String getText(int offset, int length) {
    return buffer.getText(offset, length);
  }

  /**
   * @return the resolved {@link DartUnit}.
   */
  public DartUnit getUnitNode() {
    return unitNode;
  }

  public boolean rangeIncludesNonWhitespaceOutsideNode(SourceRange range, DartNode node)
      throws DartModelException {
    return rangeIncludesNonWhitespaceOutsideRange(range, SourceRangeFactory.create(node));
  }

  public boolean rangeIncludesNonWhitespaceOutsideRange(SourceRange selection, SourceRange node) {
    // selection should cover node
    if (!SourceRangeUtils.covers(selection, node)) {
      return false;
    }
    // non-whitespace between selection start and node start
    if (!isJustWhitespace(selection.getOffset(), node.getOffset())) {
      return true;
    }
    // non-whitespace after node
    if (!isJustWhitespaceOrComment(node.getOffset() + node.getLength(), selection.getOffset()
        + selection.getLength())) {
      return true;
    }
    // only whitespace in selection around node
    return false;
  }

  /**
   * @return the index of the last space or tab on the left from the given {@link DartNode}.
   */
  private int getNodePrefixStartIndex(DartNode node) {
    int endIndex = node.getSourceInfo().getOffset();
    int startIndex = endIndex;
    while (true) {
      char c = buffer.getChar(startIndex - 1);
      if (c != ' ' && c != '\t') {
        break;
      }
      startIndex--;
    }
    return startIndex;
  }

  /**
   * @return <code>true</code> if selection range contains only whitespace.
   */
  private boolean isJustWhitespace(int start, int end) {
    if (start == end) {
      return true;
    }
    Assert.isTrue(start <= end);
    return getText(start, end - start).trim().length() == 0;
  }

  /**
   * @return <code>true</code> if selection range contains only whitespace or comments
   */
  private boolean isJustWhitespaceOrComment(int start, int end) {
    if (start == end) {
      return true;
    }
    // prepare text
    Assert.isTrue(start <= end);
    final String trimmedText = buffer.getText(start, end - start).trim();
    // may be whitespace
    if (trimmedText.isEmpty()) {
      return true;
    }
    // may be comment
    return ExecutionUtils.runObjectIgnore(new RunnableObjectEx<Boolean>() {
      @Override
      public Boolean runObject() throws Exception {
        DartScanner scanner = new DartScanner(trimmedText);
        return scanner.next() == Token.EOS;
      }
    }, false).booleanValue();
  }

}
