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

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartTypedLiteral;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.type.Type;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.core.buffer.Buffer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.internal.corext.SourceRangeFactory;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;

import org.eclipse.core.runtime.Assert;

/**
 * Extract Local Variable (from selected expression inside method or initializer).
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class ExtractUtils {
  /**
   * The default end-of-line marker for the current platform. This value should (almost) never be
   * used directly. The end-of-line marker should always be queried from {@link Buffer} because it
   * can differ from the platform default in some situations. The only exception is if code is being
   * constructed for a new file, in which case there is no {@link Buffer} to ask.
   */
  public static final String DEFAULT_END_OF_LINE = System.getProperty("line.separator", "\n");

  public static boolean covers(SourceRange sourceRange, DartNode astNode) {
    return covers(sourceRange, SourceRangeFactory.create(astNode));
  }

  public static boolean covers(SourceRange thisRange, SourceRange otherRange) {
    return thisRange.getOffset() <= otherRange.getOffset()
        && getEndInclusive(thisRange) >= getEndInclusive(otherRange);
  }

  public static int getEndExclusive(SourceRange sourceRange) {
    return sourceRange.getOffset() + sourceRange.getLength();
  }

  public static int getEndInclusive(SourceRange sourceRange) {
    return getEndExclusive(sourceRange) - 1;
  }

  /**
   * @return the line prefix consisting of spaces and tabs on the left from the given
   *         {@link DartNode}.
   */
  public static String getNodePrefix(Buffer buffer, DartNode node) {
    int endIndex = node.getSourceInfo().getOffset();
    int startIndex = getNodePrefixStartIndex(buffer, node);
    return buffer.getText(startIndex, endIndex - startIndex);
  }

  /**
   * @return the index of the last space or tab on the left from the given {@link DartNode}.
   */
  public static int getNodePrefixStartIndex(Buffer buffer, DartNode node) {
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
   * @return the actual type source of the given {@link DartExpression}, may be <code>null</code> if
   *         can not be resolved, should be treated as <code>Dynamic</code>.
   */
  public static String getTypeSource(DartExpression expression) {
    if (expression == null) {
      return null;
    }
    return expression.accept(new ASTVisitor<String>() {
      @Override
      public String visitBinaryExpression(DartBinaryExpression node) {
        DartExpression arg1 = node.getArg1();
        DartExpression arg2 = node.getArg2();
        String type1 = getTypeSource(arg1);
        String type2 = getTypeSource(arg2);
        if (type1 != null && type2 != null) {
          switch (node.getOperator()) {
            case NE:
            case EQ:
            case NE_STRICT:
            case EQ_STRICT:
            case LT:
            case GT:
            case LTE:
            case GTE:
            case AND:
            case OR:
              return "bool";
            case ADD:
            case SUB:
            case MUL:
            case DIV:
              if (type1.equals("int") && type2.equals("int")) {
                return type1;
              }
              if (type1.equals("double") && type2.equals("int")) {
                return type1;
              }
              if (type1.equals("int") && type2.equals("double")) {
                return type2;
              }
              if (type1.equals("double") && type2.equals("double")) {
                return type1;
              }
              return null;
            case BIT_AND:
            case BIT_OR:
            case BIT_XOR:
            case SAR:
            case SHL:
            case MOD:
              return "int";
          }
        }
        return null;
      }

      @Override
      public String visitExpression(DartExpression node) {
        // TODO(scheglov) variables as DartIdentifier
        return null;
      }

      @Override
      public String visitInvocation(DartInvocation node) {
        Element element = node.getElement();
        if (element instanceof MethodElement) {
          MethodElement methodElement = (MethodElement) element;
          Type returnType = methodElement.getReturnType();
          return getTypeSource(returnType);
        }
        return null;
      }

      @Override
      public String visitLiteral(DartLiteral node) {
        Type literalType = node.getType();
        return getTypeSource(literalType);
      }

      @Override
      public String visitParenthesizedExpression(DartParenthesizedExpression node) {
        DartExpression expr = node.getExpression();
        return getTypeSource(expr);
      }

      @Override
      public String visitTypedLiteral(DartTypedLiteral node) {
        Type literalType = node.getType();
        return getTypeSource(literalType);
      }

      @Override
      public String visitUnaryExpression(DartUnaryExpression node) {
        DartExpression arg = node.getArg();
        return getTypeSource(arg);
      }
    });
  }

  public static boolean isSingleNodeSelected(
      SelectionAnalyzer sa,
      SourceRange range,
      CompilationUnit cu) throws DartModelException {
    return sa.getSelectedNodes().length == 1
        && !rangeIncludesNonWhitespaceOutsideNode(range, sa.getFirstSelectedNode(), cu);
  }

  static boolean rangeIncludesNonWhitespaceOutsideRange(
      SourceRange selection,
      SourceRange nodes,
      Buffer buffer) {
    if (!covers(selection, nodes)) {
      return false;
    }

    //TODO: skip leading comments. Consider that leading line comment must be followed by newline!
    if (!isJustWhitespace(selection.getOffset(), nodes.getOffset(), buffer)) {
      return true;
    }
    if (!isJustWhitespaceOrComment(nodes.getOffset() + nodes.getLength(), selection.getOffset()
        + selection.getLength(), buffer)) {
      return true;
    }
    return false;
  }

  private static String getTypeSource(Type returnType) {
    String typeSource = returnType.toString();
    typeSource = StringUtils.replace(typeSource, "<dynamic>", "Dynamic");
    typeSource = StringUtils.replace(typeSource, "<Dynamic>", "");
    typeSource = StringUtils.replace(typeSource, "<Dynamic, Dynamic>", "");
    return typeSource;
  }

  private static boolean isJustWhitespace(int start, int end, Buffer buffer) {
    if (start == end) {
      return true;
    }
    Assert.isTrue(start <= end);
    return 0 == buffer.getText(start, end - start).trim().length();
  }

  private static boolean isJustWhitespaceOrComment(int start, int end, Buffer buffer) {
    if (start == end) {
      return true;
    }
    Assert.isTrue(start <= end);
    String trimmedText = buffer.getText(start, end - start).trim();
    if (0 == trimmedText.length()) {
      return true;
    } else {
      // TODO(scheglov) support comment
      return false;
//      IScanner scanner = ToolFactory.createScanner(false, false, false, null);
//      scanner.setSource(trimmedText.toCharArray());
//      try {
//        return scanner.getNextToken() == ITerminalSymbols.TokenNameEOF;
//      } catch (InvalidInputException e) {
//        return false;
//      }
    }
  }

  private static boolean rangeIncludesNonWhitespaceOutsideNode(
      SourceRange range,
      DartNode node,
      CompilationUnit cu) throws DartModelException {
    return rangeIncludesNonWhitespaceOutsideRange(
        range,
        SourceRangeFactory.create(node),
        cu.getBuffer());
  }

}
