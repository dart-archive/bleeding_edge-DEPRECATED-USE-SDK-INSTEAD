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

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartArrayLiteral;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartBooleanLiteral;
import com.google.dart.compiler.ast.DartBreakStatement;
import com.google.dart.compiler.ast.DartCase;
import com.google.dart.compiler.ast.DartCatchBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartConditional;
import com.google.dart.compiler.ast.DartContinueStatement;
import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartDefault;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartDoWhileStatement;
import com.google.dart.compiler.ast.DartDoubleLiteral;
import com.google.dart.compiler.ast.DartEmptyStatement;
import com.google.dart.compiler.ast.DartExprStmt;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartForInStatement;
import com.google.dart.compiler.ast.DartForStatement;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartGotoStatement;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInitializer;
import com.google.dart.compiler.ast.DartIntegerLiteral;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartLibraryDirective;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartMapLiteral;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNativeBlock;
import com.google.dart.compiler.ast.DartNativeDirective;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNullLiteral;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartParameterizedTypeNode;
import com.google.dart.compiler.ast.DartParenthesizedExpression;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartResourceDirective;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartStringInterpolation;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperExpression;
import com.google.dart.compiler.ast.DartSwitchMember;
import com.google.dart.compiler.ast.DartSwitchStatement;
import com.google.dart.compiler.ast.DartSyntheticErrorExpression;
import com.google.dart.compiler.ast.DartSyntheticErrorStatement;
import com.google.dart.compiler.ast.DartThisExpression;
import com.google.dart.compiler.ast.DartThrowStatement;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartTypeExpression;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.DartWhileStatement;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.internal.formatter.align.Alignment;
import com.google.dart.tools.core.internal.formatter.align.AlignmentException;
import com.google.dart.tools.core.utilities.ast.DartAstUtilities;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Instances of the class <code>CodeFormatterVisitor</code> format Dart source code.
 * <p>
 * The visitor has changed quite a bit. JDT uses traverse() to fire off the visitor. This code uses
 * accept(). It isn't certain they are equivalent.
 */
@SuppressWarnings({"deprecation", "unused"})
public class CodeFormatterVisitor extends ASTVisitor<DartNode> {
  static {
    // Remove @SuppressWarnings when finished
    DartCore.notYetImplemented();
  }

  // Binary expression positions storage
  final static long EXPRESSIONS_POS_ENTER_EQUALITY = 1;
  final static long EXPRESSIONS_POS_ENTER_TWO = 2;
  final static long EXPRESSIONS_POS_BETWEEN_TWO = 3;

  final static long EXPRESSIONS_POS_MASK = EXPRESSIONS_POS_BETWEEN_TWO;

  private static final Token[] CLOSING_GENERICS_EXPECTEDTOKENS = new Token[] {Token.SAR, Token.GT};
  private static final Token[] BOOLEAN_LITERAL_EXPECTEDTOKENS = new Token[] {
      Token.TRUE_LITERAL, Token.FALSE_LITERAL};
  // IDENTIFIER is included to make "negate" legal
  // NE, EQ_STRICT, and NE_STRICT are not legal but why should the formatter
  // disallow them?
  private static final Token[] DEFINABLE_OPERATOR_EXPECTEDTOKENS = new Token[] {
      Token.BIT_OR, Token.BIT_XOR, Token.BIT_AND, Token.SHL, Token.SAR, Token.ADD, Token.SUB,
      Token.MUL, Token.DIV, Token.TRUNC, Token.MOD, Token.EQ, Token.NE, Token.EQ_STRICT,
      Token.NE_STRICT, Token.LT, Token.GT, Token.LTE, Token.GTE, Token.BIT_NOT, Token.INDEX,
      Token.ASSIGN_INDEX, Token.IDENTIFIER};

  public DefaultCodeFormatterOptions preferences;
  public Scribe scribe;
  public int lastLocalDeclarationSourceStart;

  int lastBinaryExpressionAlignmentBreakIndentation;
  int expressionsDepth = -1;
  long expressionsPos;
  int arrayInitializersDepth = -1;

  private final Scanner localScanner;

  // be careful to preserve comparison order
  private static Token[] TYPEREFERENCE_EXPECTEDTOKENS = {Token.VOID, Token.IDENTIFIER};

  public CodeFormatterVisitor(DefaultCodeFormatterOptions preferences, IRegion[] regions,
      CodeSnippetParsingUtil codeSnippetParsingUtil, boolean includeComments) {
    this.preferences = preferences;
    scribe = new Scribe(this, regions, codeSnippetParsingUtil, includeComments);
    localScanner = new Scanner();
  }

  public TextEdit format(String string, DartExpression expression) {
    // reset the scribe
    scribe.reset();

    final char[] compilationUnitSource = string.toCharArray();

    localScanner.setSource(compilationUnitSource);
    scribe.resetScanner(compilationUnitSource);

    if (expression == null) {
      return null;
    }

    lastLocalDeclarationSourceStart = -1;
    try {
      expression.accept(this);
      scribe.printComment();
    } catch (AbortFormatting e) {
      return failedToFormat(e);
    }
    return scribe.getRootEdit();
  }

  public TextEdit format(String string, DartMethodDefinition constructorDeclaration) {
    // reset the scribe
    scribe.reset();

    long startTime = 0;

    final char[] compilationUnitSource = string.toCharArray();

    localScanner.setSource(compilationUnitSource);
    scribe.resetScanner(compilationUnitSource);

    if (constructorDeclaration == null) {
      return null;
    }

    lastLocalDeclarationSourceStart = -1;
    try {
      DartCore.notYetImplemented();
//      List<DartInitializer> initializers = constructorDeclaration.getInitializers();
//      if (initializers != null) {
//        formatInitializers(initializers);
//      }
//      DartFunction function = constructorDeclaration.getFunction();
//      if (function != null) {
//        formatStatements(null, statements, false);
//      }
//      if (hasComments()) {
//        this.scribe.printNewLine();
//      }
      scribe.printComment();
    } catch (AbortFormatting e) {
      return failedToFormat(e);
    }
    return scribe.getRootEdit();
  }

  public TextEdit format(String string, DartNode[] nodes) {
    // reset the scribe
    scribe.reset();

    final char[] compilationUnitSource = string.toCharArray();

    localScanner.setSource(compilationUnitSource);
    scribe.resetScanner(compilationUnitSource);

    if (nodes == null) {
      return null;
    }

    lastLocalDeclarationSourceStart = -1;
    try {
      formatClassBodyDeclarations(nodes);
    } catch (AbortFormatting e) {
      return failedToFormat(e);
    }
    return scribe.getRootEdit();
  }

  public TextEdit format(String string, DartUnit compilationUnitDeclaration) {
    // reset the scribe
    scribe.reset();

    if (compilationUnitDeclaration == null) {
      return failedToFormat(null);
    }

    final char[] compilationUnitSource = string.toCharArray();

    localScanner.setSource(compilationUnitSource);
    scribe.resetScanner(compilationUnitSource);

    lastLocalDeclarationSourceStart = -1;
    try {
      compilationUnitDeclaration.accept(this);
    } catch (AbortFormatting e) {
      return failedToFormat(e);
    }
    return scribe.getRootEdit();
  }

  public void formatComment(int kind, String source, int start, int end, int indentationLevel) {
    if (source == null) {
      return;
    }
    scribe.printComment(kind, source, start, end, indentationLevel);
  }

  @Override
  public DartNode visitArrayAccess(DartArrayAccess arrayReference) {
    final int numberOfParens = printOpenParens(arrayReference);
    arrayReference.getTarget().accept(this);
    scribe.printNextToken(
        Token.LBRACK,
        preferences.insert_space_before_opening_bracket_in_array_reference);
    if (preferences.insert_space_after_opening_bracket_in_array_reference) {
      scribe.space();
    }
    arrayReference.getKey().accept(this);
    scribe.printNextToken(
        Token.RBRACK,
        preferences.insert_space_before_closing_bracket_in_array_reference);
    printCloseParens(arrayReference, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitArrayLiteral(DartArrayLiteral arrayInitializer) {
    final int numberOfParens = printOpenParens(arrayInitializer);
    if (arrayInitializersDepth < 0) {
      arrayInitializersDepth = 0;
    } else {
      arrayInitializersDepth++;
    }
    int arrayInitializerIndentationLevel = scribe.indentationLevel;
    try {
      final List<DartExpression> expressions = arrayInitializer.getExpressions();
      if (expressions != null) {
        String array_initializer_brace_position = preferences.brace_position_for_array_initializer;
        formatOpeningBracket(
            array_initializer_brace_position,
            preferences.insert_space_before_opening_brace_in_array_initializer);

        int expressionsLength = expressions.size();
        final boolean insert_new_line_after_opening_brace = preferences.insert_new_line_after_opening_brace_in_array_initializer;
        boolean ok = false;
        Alignment arrayInitializerAlignment = null;
        if (expressionsLength > 1) {
          if (insert_new_line_after_opening_brace) {
            scribe.printNewLine();
          }
          arrayInitializerAlignment = scribe.createAlignment(
              Alignment.ARRAY_INITIALIZER,
              preferences.alignment_for_expressions_in_array_initializer,
              Alignment.R_OUTERMOST,
              expressionsLength,
              scribe.scanner.currentPosition,
              preferences.continuation_indentation_for_array_initializer,
              true);

          if (insert_new_line_after_opening_brace) {
            arrayInitializerAlignment.fragmentIndentations[0] = arrayInitializerAlignment.breakIndentationLevel;
          }

          scribe.enterAlignment(arrayInitializerAlignment);
          do {
            try {
              scribe.alignFragment(arrayInitializerAlignment, 0);
              if (preferences.insert_space_after_opening_brace_in_array_initializer) {
                scribe.space();
              }
              expressions.get(0).accept(this);
              for (int i = 1; i < expressionsLength; i++) {
                scribe.printNextToken(
                    Token.COMMA,
                    preferences.insert_space_before_comma_in_array_initializer);
                scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
                scribe.alignFragment(arrayInitializerAlignment, i);
                if (preferences.insert_space_after_comma_in_array_initializer) {
                  scribe.space();
                }
                expressions.get(i).accept(this);
                if (i == expressionsLength - 1) {
                  if (isNextToken(Token.COMMA)) {
                    scribe.printNextToken(
                        Token.COMMA,
                        preferences.insert_space_before_comma_in_array_initializer);
                    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
                  }
                }
              }
              ok = true;
            } catch (AlignmentException e) {
              scribe.redoAlignment(e);
            }
          } while (!ok);
          scribe.exitAlignment(arrayInitializerAlignment, true);
        } else {
          // Use an alignment with no break in case when the array initializer
          // is not inside method arguments alignments
          if (scribe.currentAlignment == null
              || scribe.currentAlignment.kind != Alignment.MESSAGE_ARGUMENTS) {
            arrayInitializerAlignment = scribe.createAlignment(
                Alignment.ARRAY_INITIALIZER,
                preferences.alignment_for_expressions_in_array_initializer,
                Alignment.R_OUTERMOST,
                0,
                scribe.scanner.currentPosition,
                preferences.continuation_indentation_for_array_initializer,
                true);
            scribe.enterAlignment(arrayInitializerAlignment);
          }
          do {
            try {
              if (insert_new_line_after_opening_brace) {
                scribe.printNewLine();
                scribe.indent();
              }
              // we don't need to use an alignment
              if (preferences.insert_space_after_opening_brace_in_array_initializer) {
                scribe.space();
              } else {
                scribe.needSpace = false;
              }
              expressions.get(0).accept(this);
              if (isNextToken(Token.COMMA)) {
                scribe.printNextToken(
                    Token.COMMA,
                    preferences.insert_space_before_comma_in_array_initializer);
                scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
              }
              if (insert_new_line_after_opening_brace) {
                scribe.unIndent();
              }
              ok = true;
            } catch (AlignmentException e) {
              if (arrayInitializerAlignment == null) {
                throw e;
              }
              scribe.redoAlignment(e);
            }
          } while (!ok);
          if (arrayInitializerAlignment != null) {
            scribe.exitAlignment(arrayInitializerAlignment, true);
          }
        }
        if (preferences.insert_new_line_before_closing_brace_in_array_initializer) {
          scribe.printNewLine();
        } else if (preferences.insert_space_before_closing_brace_in_array_initializer) {
          scribe.space();
        }
        scribe.printNextToken(
            Token.RBRACK,
            false,
            Scribe.PRESERVE_EMPTY_LINES_IN_CLOSING_ARRAY_INITIALIZER
                + (arrayInitializerIndentationLevel << 16));
        if (array_initializer_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
          scribe.unIndent();
        }
      } else {
        boolean keepEmptyArrayInitializerOnTheSameLine = preferences.keep_empty_array_initializer_on_one_line;
        String array_initializer_brace_position = preferences.brace_position_for_array_initializer;
        if (keepEmptyArrayInitializerOnTheSameLine) {
          scribe.printNextToken(
              Token.LBRACK,
              preferences.insert_space_before_opening_brace_in_array_initializer);
          if (isNextToken(Token.COMMA)) {
            scribe.printNextToken(
                Token.COMMA,
                preferences.insert_space_before_comma_in_array_initializer);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
          }
          scribe.printNextToken(
              Token.RBRACK,
              preferences.insert_space_between_empty_braces_in_array_initializer);
        } else {
          formatOpeningBrace(
              array_initializer_brace_position,
              preferences.insert_space_before_opening_brace_in_array_initializer);
          if (isNextToken(Token.COMMA)) {
            scribe.printNextToken(
                Token.COMMA,
                preferences.insert_space_before_comma_in_array_initializer);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
          }
          scribe.printNextToken(
              Token.RBRACK,
              preferences.insert_space_between_empty_braces_in_array_initializer);
          if (array_initializer_brace_position.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
            scribe.unIndent();
          }
        }
      }
    } finally {
      arrayInitializersDepth--;
    }

    printCloseParens(arrayInitializer, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitBinaryExpression(DartBinaryExpression binaryExpression) {
    final int numberOfParens = printOpenParens(binaryExpression);
    BinaryExpressionFragmentBuilder builder = buildFragments(binaryExpression);
    final int fragmentsSize = builder.size();

    if (expressionsDepth < 0) {
      expressionsDepth = 0;
    } else {
      expressionsDepth++;
      expressionsPos <<= 2;
    }
    try {
      lastBinaryExpressionAlignmentBreakIndentation = 0;
      if ((builder.realFragmentsSize() > 1 || fragmentsSize > 4) && numberOfParens == 0) {
        int scribeLine = scribe.line;
        scribe.printComment();
        Alignment binaryExpressionAlignment = scribe.createAlignment(
            Alignment.BINARY_EXPRESSION,
            preferences.alignment_for_binary_expression,
            Alignment.R_OUTERMOST,
            fragmentsSize,
            scribe.scanner.currentPosition);
        scribe.enterAlignment(binaryExpressionAlignment);
        boolean ok = false;
        DartNode[] fragments = builder.fragments();
        Token[] operators = builder.operators();
        do {
          try {
            final int max = fragmentsSize - 1;
            for (int i = 0; i < max; i++) {
              DartNode fragment = fragments[i];
              fragment.accept(this);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
              if (scribe.lastNumberOfNewLines == 1) {
                // a new line has been inserted while printing the comment
                // hence we need to use the break indentation level before
                // printing next token...
                scribe.indentationLevel = binaryExpressionAlignment.breakIndentationLevel;
              }
              if (preferences.wrap_before_binary_operator) {
                scribe.alignFragment(binaryExpressionAlignment, i);
                scribe.printNextToken(operators[i], preferences.insert_space_before_binary_operator);
              } else {
                scribe.printNextToken(operators[i], preferences.insert_space_before_binary_operator);
                scribe.alignFragment(binaryExpressionAlignment, i);
              }
              if (operators[i] == Token.SUB && isNextToken(Token.SUB)) {
                // the next character is a minus (unary operator)
                scribe.space();
              }
              if (preferences.insert_space_after_binary_operator) {
                scribe.space();
              }
            }
            fragments[max].accept(this);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            ok = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!ok);
        scribe.exitAlignment(binaryExpressionAlignment, true);
        if (scribe.line == scribeLine) {
          // The expression was not broken => reset last break indentation
          lastBinaryExpressionAlignmentBreakIndentation = 0;
        } else {
          lastBinaryExpressionAlignmentBreakIndentation = binaryExpressionAlignment.breakIndentationLevel;
        }
      } else {
        expressionsPos |= EXPRESSIONS_POS_ENTER_TWO;
        binaryExpression.getArg1().accept(this);
        expressionsPos &= ~EXPRESSIONS_POS_MASK;
        expressionsPos |= EXPRESSIONS_POS_BETWEEN_TWO;
        scribe.printNextToken(
            binaryExpression.getOperator(),
            preferences.insert_space_before_binary_operator,
            Scribe.PRESERVE_EMPTY_LINES_IN_BINARY_EXPRESSION);
        if (binaryExpression.getOperator() == Token.SUB && isNextToken(Token.SUB)) {
          // the next character is a minus (unary operator)
          scribe.space();
        }
        if (preferences.insert_space_after_binary_operator) {
          scribe.space();
        }
        binaryExpression.getArg2().accept(this);
      }
    } finally {
      expressionsDepth--;
      expressionsPos >>= 2;
      if (expressionsDepth < 0) {
        lastBinaryExpressionAlignmentBreakIndentation = 0;
      }
    }
    printCloseParens(binaryExpression, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitBlock(DartBlock node) {
    formatBlock(
        node,
        preferences.brace_position_for_block,
        preferences.insert_space_before_opening_brace_in_block);
    return null;
  }

  @Override
  public DartNode visitBooleanLiteral(DartBooleanLiteral node) {
    final int numberOfParens = printOpenParens(node);
    scribe.printNextToken(BOOLEAN_LITERAL_EXPECTEDTOKENS);
    printCloseParens(node, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitBreakStatement(DartBreakStatement node) {
    scribe.printNextToken(Token.BREAK);
    if (node.getLabel() != null) {
      scribe.printNextToken(Token.IDENTIFIER, true);
    }
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitCase(DartCase caseStatement) {
    formatLabel(caseStatement.getLabel());
    scribe.printNextToken(Token.CASE);
    scribe.space();
    caseStatement.getExpr().accept(this);
    scribe.printNextToken(Token.COLON, preferences.insert_space_before_colon_in_case);
    formatCaseStatements(caseStatement);
    return null;
  }

  @Override
  public DartNode visitCatchBlock(DartCatchBlock node) {
    if (preferences.insert_new_line_before_catch_in_try_statement) {
      scribe.printNewLine();
    }
    scribe.printNextToken(Token.CATCH, preferences.insert_space_after_closing_brace_in_block);
    final int line = scribe.line;
    scribe.printNextToken(Token.LPAREN, preferences.insert_space_before_opening_paren_in_catch);
    if (preferences.insert_space_after_opening_paren_in_catch) {
      scribe.space();
    }
    node.getException().accept(this);
    scribe.printNextToken(Token.RPAREN, preferences.insert_space_before_closing_paren_in_catch);
    formatLeftCurlyBrace(line, preferences.brace_position_for_block);
    node.getBlock().accept(this);
    return null;
  }

  @Override
  public DartNode visitClass(DartClass node) {
    return format(node);
  }

  @Override
  public DartNode visitClassMember(DartClassMember<?> node) {
    // this method should not be invoked
    return reportMissingNodeFormatter(DartNode.class, node);
  }

  @Override
  public DartNode visitConditional(DartConditional conditionalExpression) {
    final int numberOfParens = printOpenParens(conditionalExpression);
    conditionalExpression.getCondition().accept(this);

    Alignment conditionalExpressionAlignment = scribe.createAlignment(
        Alignment.CONDITIONAL_EXPRESSION,
        preferences.alignment_for_conditional_expression,
        2,
        scribe.scanner.currentPosition);

    scribe.enterAlignment(conditionalExpressionAlignment);
    boolean ok = false;
    do {
      try {
        scribe.alignFragment(conditionalExpressionAlignment, 0);
        scribe.printNextToken(
            Token.CONDITIONAL,
            preferences.insert_space_before_question_in_conditional);

        if (preferences.insert_space_after_question_in_conditional) {
          scribe.space();
        }
        conditionalExpression.getThenExpression().accept(this);
        scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
        scribe.alignFragment(conditionalExpressionAlignment, 1);
        scribe.printNextToken(Token.COLON, preferences.insert_space_before_colon_in_conditional);

        if (preferences.insert_space_after_colon_in_conditional) {
          scribe.space();
        }
        conditionalExpression.getElseExpression().accept(this);

        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(conditionalExpressionAlignment, true);

    printCloseParens(conditionalExpression, numberOfParens);
    return null;
  }

  public DartNode visitConstructorDefinition(DartMethodDefinition node) {
    scribe.printComment();
    int line = scribe.line;
    scribe.printModifiers();
    if (scribe.line > line) {
      line = scribe.line;
    }
    scribe.space();
    /*
     * Print the method name
     */
    scribe.printNextToken(Token.IDENTIFIER, true);
    DartExpression expr = node.getName();
    if (expr instanceof DartPropertyAccess) {
      // named constructor
      scribe.printNextToken(Token.PERIOD);
      scribe.printNextToken(Token.IDENTIFIER);
    }
    formatMethodArguments(
        node.getFunction().getParameters(),
        preferences.insert_space_before_opening_paren_in_constructor_declaration,
        preferences.insert_space_between_empty_parens_in_constructor_declaration,
        preferences.insert_space_before_closing_paren_in_constructor_declaration,
        preferences.insert_space_after_opening_paren_in_constructor_declaration,
        preferences.insert_space_before_comma_in_constructor_declaration_parameters,
        preferences.insert_space_after_comma_in_constructor_declaration_parameters,
        preferences.alignment_for_parameters_in_constructor_declaration);
    List<DartInitializer> inits = node.getInitializers();
    if (inits != null && !inits.isEmpty()) {
      // print the initializers
      // TODO spaces around the colon are not tied to preferences
      scribe.printNextToken(Token.COLON, true);
      scribe.space();
      if (hasComplexInitializers(inits)) {
        formatInitializers(
            inits,
            preferences.insert_space_before_comma_in_constructor_declaration_parameters,
            preferences.insert_space_after_comma_in_constructor_declaration_parameters,
            Alignment.M_FORCE | preferences.alignment_for_initializers_in_constructor_declaration);
      } else if (inits.size() > 3) {
        formatInitializers(
            inits,
            preferences.insert_space_before_comma_in_constructor_declaration_parameters,
            preferences.insert_space_after_comma_in_constructor_declaration_parameters,
            preferences.alignment_for_initializers_in_constructor_declaration);
      } else {
        for (int i = 0; i < inits.size(); i++) {
          if (i > 0) {
            scribe.printNextToken(
                Token.COMMA,
                preferences.insert_space_before_comma_in_constructor_declaration_parameters);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            if (preferences.insert_space_after_comma_in_constructor_declaration_parameters) {
              scribe.space();
            }
          }
          DartInitializer init = inits.get(i);
          init.accept(this);
        }
      }
    }
    DartBlock body = node.getFunction().getBody();
    /*
     * Method body
     */
    if (body != null) {
      // may be native
      if (node.getModifiers().isNative()) {
        body.accept(this);
        scribe.printComment();
        return null;
      }
      String constructor_declaration_brace = preferences.brace_position_for_constructor_declaration;
      formatLeftCurlyBrace(line, constructor_declaration_brace);
      formatOpeningBrace(
          constructor_declaration_brace,
          preferences.insert_space_before_opening_brace_in_constructor_declaration);
      final int numberOfBlankLinesAtBeginningOfMethodBody = preferences.blank_lines_at_beginning_of_method_body;
      if (numberOfBlankLinesAtBeginningOfMethodBody > 0) {
        scribe.printEmptyLines(numberOfBlankLinesAtBeginningOfMethodBody);
      }
      final List<DartStatement> statements = body.getStatements();
      if (statements != null) {
        scribe.printNewLine();
        if (preferences.indent_statements_compare_to_body) {
          scribe.indent();
        }
        formatStatements(statements, true);
        scribe.printComment();
        if (preferences.indent_statements_compare_to_body) {
          scribe.unIndent();
        }
      } else {
        if (preferences.insert_new_line_in_empty_method_body) {
          scribe.printNewLine();
        }
        if (preferences.indent_statements_compare_to_body) {
          scribe.indent();
        }
        scribe.printComment();
        if (preferences.indent_statements_compare_to_body) {
          scribe.unIndent();
        }
      }
      scribe.printNextToken(Token.RBRACE);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
      if (constructor_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
        scribe.unIndent();
      }
    } else {
      // no method body for const
      scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    }
    return null;
  }

  @Override
  public DartNode visitContinueStatement(DartContinueStatement node) {
    scribe.printNextToken(Token.CONTINUE);
    if (node.getLabel() != null) {
      scribe.printNextToken(Token.IDENTIFIER, true);
    }
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitDeclaration(DartDeclaration<?> node) {

    return null;
  }

  @Override
  public DartNode visitDefault(DartDefault node) {
    formatLabel(node.getLabel());
    scribe.printNextToken(Token.DEFAULT);
    scribe.printNextToken(Token.COLON, preferences.insert_space_before_colon_in_default);
    formatCaseStatements(node);
    return null;
  }

  @Override
  public DartNode visitDirective(DartDirective node) {

    return null;
  }

  @Override
  public DartNode visitDoubleLiteral(DartDoubleLiteral doubleLiteral) {
    final int numberOfParens = printOpenParens(doubleLiteral);
    Double constant = doubleLiteral.getValue();
    scribe.printNextToken(Token.DOUBLE_LITERAL);
    printCloseParens(doubleLiteral, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitDoWhileStatement(DartDoWhileStatement doStatement) {
    scribe.printNextToken(Token.DO);
    final int line = scribe.line;

    final DartStatement action = doStatement.getBody();
    if (action != null) {
      if (action instanceof DartBlock) {
        formatLeftCurlyBrace(line, preferences.brace_position_for_block);
        action.accept(this);
      } else if (action instanceof DartEmptyStatement) {
        /*
         * This is an empty statement
         */
        formatNecessaryEmptyStatement();
      } else {
        scribe.printNewLine();
        scribe.indent();
        action.accept(this);
        scribe.printNewLine();
        scribe.unIndent();
      }
    } else {
      /*
       * This is an empty statement
       */
      formatNecessaryEmptyStatement();
    }

    if (preferences.insert_new_line_before_while_in_do_statement) {
      scribe.printNewLine();
    }
    scribe.printNextToken(Token.WHILE, preferences.insert_space_after_closing_brace_in_block);
    scribe.printNextToken(Token.LPAREN, preferences.insert_space_before_opening_paren_in_while);

    if (preferences.insert_space_after_opening_paren_in_while) {
      scribe.space();
    }

    doStatement.getCondition().accept(this);

    scribe.printNextToken(Token.RPAREN, preferences.insert_space_before_closing_paren_in_while);
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitEmptyStatement(DartEmptyStatement node) {
    if (preferences.put_empty_statement_on_new_line) {
      scribe.printNewLine();
    }
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitExpression(DartExpression node) {
    // this method should not be invoked
    return reportMissingNodeFormatter(DartExpression.class, node);
  }

  @Override
  public DartNode visitExprStmt(DartExprStmt node) {
    node.getExpression().accept(this);
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitField(DartField node) {

    return null;
  }

  @Override
  public DartNode visitFieldDefinition(DartFieldDefinition node) {
    format(node, this, false, false);
    return null;
  }

  @Override
  public DartNode visitForInStatement(DartForInStatement node) {

    return null;
  }

  @Override
  public DartNode visitForStatement(DartForStatement forStatement) {

    scribe.printNextToken(Token.FOR);
    final int line = scribe.line;
    scribe.printNextToken(Token.LPAREN, preferences.insert_space_before_opening_paren_in_for);

    if (preferences.insert_space_after_opening_paren_in_for) {
      scribe.space();
    }
    final DartStatement initializations = forStatement.getInit();
    // DartExprStmt or DartVariableStmt
    if (initializations != null) {
      if (initializations instanceof DartVariableStatement) {
        formatLocalDeclaration(
            (DartVariableStatement) initializations,
            preferences.insert_space_before_comma_in_for_inits,
            preferences.insert_space_after_comma_in_for_inits);
      } else {
        initializations.accept(this);
      }
    }
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon_in_for);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    final DartExpression condition = forStatement.getCondition();
    if (condition != null) {
      if (preferences.insert_space_after_semicolon_in_for) {
        scribe.space();
      }
      condition.accept(this);
    }
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon_in_for);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    final DartExpression increments = forStatement.getIncrement();
    if (increments != null) {
      if (preferences.insert_space_after_semicolon_in_for) {
        scribe.space();
      }
      increments.accept(this);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    }
    scribe.printNextToken(Token.RPAREN, preferences.insert_space_before_closing_paren_in_for);

    final DartStatement action = forStatement.getBody();
    if (action != null) {
      if (action instanceof DartBlock) {
        formatLeftCurlyBrace(line, preferences.brace_position_for_block);
        action.accept(this);
      } else if (action instanceof DartEmptyStatement) {
        /*
         * This is an empty statement
         */
        formatNecessaryEmptyStatement();
      } else {
        scribe.indent();
        scribe.printNewLine();
        action.accept(this);
        scribe.unIndent();
      }
    } else {
      /*
       * This is an empty statement
       */
      formatNecessaryEmptyStatement();
    }
    return null;
  }

  @Override
  public DartNode visitFunction(DartFunction node) {
    // this method should not be invoked
    // the return type needs to be printed before the name (if any)
    // so the function is printed in the context of its parent
    return visitNode(node);
  }

  @Override
  public DartNode visitFunctionExpression(DartFunctionExpression node) {
    // TODO consider NOT reusing method spacing options
    // doing so means adding a dozen or so options, not worth the trouble now
    scribe.indent();
    scribe.printNewLine();
    int line = scribe.line;
    // Create alignment
    Alignment functionDeclAlignment = scribe.createAlignment(
        Alignment.FUNCTION_DEFINITION,
        preferences.alignment_for_function_declaration,
        Alignment.R_INNERMOST,
        3,
        scribe.scanner.currentPosition);
    scribe.enterAlignment(functionDeclAlignment);
    boolean ok = false;
    do {
      try {
        int fragmentIndex = 0;
        scribe.alignFragment(functionDeclAlignment, fragmentIndex);
        if (scribe.line > line) {
          line = scribe.line;
        }
//        scribe.space();

        /*
         * Print the method return type
         */
        final DartTypeNode returnType = node.getFunction().getReturnTypeNode();

        if (returnType != null) {
          returnType.accept(this);
          scribe.space();
        }
        scribe.alignFragment(functionDeclAlignment, ++fragmentIndex);

        scribe.printComment();
        if (node.getName() != null) {
          scribe.space();
          node.getName().accept(this);
        }
        // Format arguments
        formatMethodArguments(
            node.getFunction().getParameters(),
            preferences.insert_space_before_opening_paren_in_method_declaration,
            preferences.insert_space_between_empty_parens_in_method_declaration,
            preferences.insert_space_before_closing_paren_in_method_declaration,
            preferences.insert_space_after_opening_paren_in_method_declaration,
            preferences.insert_space_before_comma_in_method_declaration_parameters,
            preferences.insert_space_after_comma_in_method_declaration_parameters,
            preferences.alignment_for_parameters_in_function_declaration);

        /*
         * Check for extra dimensions
         */
        int extraDimensions = getDimensions();
        if (extraDimensions != 0) {
          for (int i = 0; i < extraDimensions; i++) {
            scribe.printNextToken(Token.LBRACK);
            scribe.printNextToken(Token.RBRACK);
          }
        }
        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(functionDeclAlignment, true);

    DartBlock body = node.getFunction().getBody();
    /*
     * Method body
     */
    Alignment almt = scribe.currentAlignment;
    scribe.currentAlignment = null;
    String method_declaration_brace = preferences.brace_position_for_function_declaration;
    formatLeftCurlyBrace(line, method_declaration_brace);
    formatOpeningBrace(
        method_declaration_brace,
        preferences.insert_space_before_opening_brace_in_method_declaration);
    final int numberOfBlankLinesAtBeginningOfMethodBody = preferences.blank_lines_at_beginning_of_method_body;
    if (numberOfBlankLinesAtBeginningOfMethodBody > 0) {
      scribe.printEmptyLines(numberOfBlankLinesAtBeginningOfMethodBody);
    }
    final List<DartStatement> statements = body.getStatements();
    if (statements != null) {
      scribe.printNewLine();
      if (preferences.indent_statements_compare_to_body) {
        scribe.indent();
      }
      formatStatements(statements, true);
      scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_METHOD_DECLARATION);
      if (preferences.indent_statements_compare_to_body) {
        scribe.unIndent();
      }
    } else {
      if (preferences.insert_new_line_in_empty_method_body) {
        scribe.printNewLine();
      }
      if (preferences.indent_statements_compare_to_body) {
        scribe.indent();
      }
      scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_METHOD_DECLARATION);
      if (preferences.indent_statements_compare_to_body) {
        scribe.unIndent();
      }
    }
    scribe.printNextToken(Token.RBRACE);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    scribe.currentAlignment = almt;
    if (method_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
      scribe.unIndent();
    }
    scribe.unIndent();
    return null;
  }

  @Override
  public DartNode visitFunctionObjectInvocation(DartFunctionObjectInvocation invocation) {

    final int numberOfParens = printOpenParens(invocation);
    CascadingInvocationFragmentBuilder builder = buildFragments(invocation);

    if (builder.size() >= 3 && numberOfParens == 0) {
      formatCascadingInvocations(builder);
    } else {
      Alignment messageAlignment = null;
      if (!(invocation.getTarget() instanceof DartThisExpression)) {
        invocation.getTarget().accept(this);
        int alignmentMode = preferences.alignment_for_selector_in_method_invocation;
        messageAlignment = scribe.createAlignment(
            Alignment.MESSAGE_SEND,
            alignmentMode,
            1,
            scribe.scanner.currentPosition);
        scribe.enterAlignment(messageAlignment);
        boolean ok = false;
        do {
          switch (alignmentMode & Alignment.SPLIT_MASK) {
            case Alignment.M_COMPACT_SPLIT:
            case Alignment.M_NEXT_PER_LINE_SPLIT:
              messageAlignment.startingColumn = scribe.column;
              break;
          }
          try {
            formatInvocation(invocation, messageAlignment);
            ok = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!ok);
        scribe.exitAlignment(messageAlignment, true);
      } else {
        formatInvocation(invocation, null);
      }
    }
    printCloseParens(invocation, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    /*
     * Print comments to get proper line number
     */
    scribe.printComment();
    scribe.printNextToken(Token.IDENTIFIER, true); // "interface"
    if (node.getReturnTypeNode() == null) {
      scribe.printNextToken(Token.IDENTIFIER, true); // void
    } else {
      node.getReturnTypeNode().accept(this); // print return type
      scribe.space();
    }
    scribe.printNextToken(Token.IDENTIFIER, true); // alias name
    formatMethodArguments(
        node.getParameters(),
        preferences.insert_space_before_opening_paren_in_constructor_declaration,
        preferences.insert_space_between_empty_parens_in_constructor_declaration,
        preferences.insert_space_before_closing_paren_in_constructor_declaration,
        preferences.insert_space_after_opening_paren_in_constructor_declaration,
        preferences.insert_space_before_comma_in_constructor_declaration_parameters,
        preferences.insert_space_after_comma_in_constructor_declaration_parameters,
        preferences.alignment_for_parameters_in_constructor_declaration);
    return null;
  }

  @Override
  public DartNode visitGotoStatement(DartGotoStatement node) {
    // this method should not be invoked
    return visitStatement(node);
  }

  @Override
  public DartNode visitIdentifier(DartIdentifier node) {
    final int numberOfParens = printOpenParens(node);
    scribe.printNextToken(TYPEREFERENCE_EXPECTEDTOKENS);
    printCloseParens(node, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitIfStatement(DartIfStatement ifStatement) {
    scribe.printNextToken(Token.IF);
    final int line = scribe.line;
    scribe.printNextToken(Token.LPAREN, preferences.insert_space_before_opening_paren_in_if);
    if (preferences.insert_space_after_opening_paren_in_if) {
      scribe.space();
    }
    ifStatement.getCondition().accept(this);
    scribe.printNextToken(Token.RPAREN, preferences.insert_space_before_closing_paren_in_if);

    final DartStatement thenStatement = ifStatement.getThenStatement();
    final DartStatement elseStatement = ifStatement.getElseStatement();

    boolean thenStatementIsBlock = false;
    if (thenStatement != null) {
      if (thenStatement instanceof DartBlock) {
        thenStatementIsBlock = true;
        if (isGuardClause((DartBlock) thenStatement) && elseStatement == null
            && preferences.keep_guardian_clause_on_one_line) {
          /*
           * Need a specific formatting for guard clauses guard clauses are block with a single
           * return or throw statement
           */
          formatGuardClauseBlock((DartBlock) thenStatement);
        } else {
          formatLeftCurlyBrace(line, preferences.brace_position_for_block);
          thenStatement.accept(this);
          if (elseStatement != null && preferences.insert_new_line_before_else_in_if_statement) {
            scribe.printNewLine();
          }
        }
      } else if (elseStatement == null && preferences.keep_simple_if_on_one_line) {
        Alignment compactIfAlignment = scribe.createAlignment(
            Alignment.COMPACT_IF,
            preferences.alignment_for_compact_if,
            Alignment.R_OUTERMOST,
            1,
            scribe.scanner.currentPosition,
            1,
            false);
        scribe.enterAlignment(compactIfAlignment);
        boolean ok = false;
        do {
          try {
            scribe.alignFragment(compactIfAlignment, 0);
            scribe.space();
            thenStatement.accept(this);
            ok = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!ok);
        scribe.exitAlignment(compactIfAlignment, true);
      } else if (preferences.keep_then_statement_on_same_line) {
        scribe.space();
        thenStatement.accept(this);
        if (elseStatement != null) {
          scribe.printNewLine();
        }
      } else {
        scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
        scribe.printNewLine();
        scribe.indent();
        thenStatement.accept(this);
        if (elseStatement != null) {
          scribe.printNewLine();
        }
        scribe.unIndent();
      }
    }

    if (elseStatement != null) {
      if (thenStatementIsBlock) {
        scribe.printNextToken(
            Token.ELSE,
            preferences.insert_space_after_closing_brace_in_block,
            Scribe.PRESERVE_EMPTY_LINES_BEFORE_ELSE);
      } else {
        scribe.printNextToken(Token.ELSE, true, Scribe.PRESERVE_EMPTY_LINES_BEFORE_ELSE);
      }
      if (elseStatement instanceof DartBlock) {
        elseStatement.accept(this);
      } else if (elseStatement instanceof DartIfStatement) {
        if (!preferences.compact_else_if) {
          scribe.printNewLine();
          scribe.indent();
        }
        scribe.space();
        elseStatement.accept(this);
        if (!preferences.compact_else_if) {
          scribe.unIndent();
        }
      } else if (preferences.keep_else_statement_on_same_line) {
        scribe.space();
        elseStatement.accept(this);
      } else {
        scribe.printNewLine();
        scribe.indent();
        elseStatement.accept(this);
        scribe.unIndent();
      }
    }
    return null;
  }

  @Override
  public DartNode visitImportDirective(DartImportDirective node) {
    scribe.printNextToken(Token.IMPORT);
//    System.out.println("nextToken: " + scribe.scanner.peekNextToken());
    scribe.printNextToken(Token.LPAREN);
//    System.out.println("nextToken: " + scribe.scanner.peekNextToken());
    scribe.printNextToken(Token.STRING, true);
    scribe.printNextToken(Token.RPAREN);

    return null;
  }

  @Override
  public DartNode visitInitializer(DartInitializer node) {
    scribe.printNextToken(Token.IDENTIFIER);
    scribe.printNextToken(Token.ASSIGN, preferences.insert_space_before_assignment_operator);
    if (preferences.insert_space_after_assignment_operator) {
      scribe.space();
    }
    Alignment assignmentAlignment = scribe.createAlignment(
        Alignment.LOCAL_DECLARATION_ASSIGNMENT,
        preferences.alignment_for_assignment,
        Alignment.R_OUTERMOST,
        1,
        scribe.scanner.currentPosition);
    scribe.enterAlignment(assignmentAlignment);
    boolean ok = false;
    do {
      try {
        scribe.alignFragment(assignmentAlignment, 0);
        node.getValue().accept(this);
        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(assignmentAlignment, true);
    return null;
  }

  @Override
  public DartNode visitIntegerLiteral(DartIntegerLiteral node) {
    final int numberOfParens = printOpenParens(node);
    BigInteger constant = node.getValue();
    scribe.printNextToken(Token.HEX_LITERAL);
    printCloseParens(node, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitInvocation(DartInvocation node) {
    // this method should not be invoked
    return visitExpression(node);
  }

  @Override
  public DartNode visitLabel(DartLabel node) {
    scribe.printNextToken(Token.IDENTIFIER);
    scribe.printNextToken(Token.COLON, preferences.insert_space_before_colon_in_labeled_statement);
    if (preferences.insert_space_after_colon_in_labeled_statement) {
      scribe.space();
    }
    if (preferences.insert_new_line_after_label) {
      scribe.printNewLine();
    }
    final DartStatement statement = node.getStatement();
    statement.accept(this);
    return null;
  }

  @Override
  public DartNode visitLibraryDirective(DartLibraryDirective node) {

    return null;
  }

  @Override
  public DartNode visitLiteral(DartLiteral node) {
    // this method should not be invoked
    return reportMissingNodeFormatter(DartLiteral.class, node);
  }

  @Override
  public DartNode visitMapLiteral(DartMapLiteral node) {
    // TODO fixup array literals to handle map literals, and vice versa
    // does ast have type info from new stmt?
    return visitExpression(node);
  }

  @Override
  public DartNode visitMapLiteralEntry(DartMapLiteralEntry node) {
    return visitNode(node);
  }

  @Override
  public DartNode visitMethodDefinition(DartMethodDefinition node) {
    if (DartAstUtilities.isConstructor(node)) {
      // TODO update for new constructor syntax
      visitConstructorDefinition(node);
      return null;
    }
    scribe.printComment();
    int line = scribe.line;
    // Create alignment
    Alignment methodDeclAlignment = scribe.createAlignment(
        Alignment.METHOD_DECLARATION,
        preferences.alignment_for_method_declaration,
        Alignment.R_INNERMOST,
        3,
        scribe.scanner.currentPosition);
    scribe.enterAlignment(methodDeclAlignment);
    boolean ok = false;
    do {
      try {
        scribe.printModifiers();
        int fragmentIndex = 0;
        scribe.alignFragment(methodDeclAlignment, fragmentIndex);
        if (scribe.line > line) {
          line = scribe.line;
        }
        scribe.space();

        Modifiers mods = node.getModifiers();
        /*
         * Print the method return type
         */
        final DartTypeNode returnType = node.getFunction().getReturnTypeNode();

        if (returnType != null) {
          returnType.accept(this);
        }
        scribe.alignFragment(methodDeclAlignment, ++fragmentIndex);

        /*
         * Print the method name
         */
        scribe.printNextToken(Token.IDENTIFIER, true);
        DartExpression expr = node.getName();
        if (mods.isFactory() && expr instanceof DartPropertyAccess) {
          // named constructor
          scribe.printNextToken(Token.PERIOD);
          scribe.printNextToken(Token.IDENTIFIER);
        } else if (mods.isOperator()) {
          scribe.space();
          // TODO negate is not an operator token, so needs special case
          scribe.printNextToken(DEFINABLE_OPERATOR_EXPECTEDTOKENS);
        } else if (mods.isGetter() || mods.isSetter()) {
          scribe.printNextToken(Token.IDENTIFIER, true);
        }
        // Format arguments
        formatMethodArguments(
            node.getFunction().getParameters(),
            preferences.insert_space_before_opening_paren_in_method_declaration,
            preferences.insert_space_between_empty_parens_in_method_declaration,
            preferences.insert_space_before_closing_paren_in_method_declaration,
            preferences.insert_space_after_opening_paren_in_method_declaration,
            preferences.insert_space_before_comma_in_method_declaration_parameters,
            preferences.insert_space_after_comma_in_method_declaration_parameters,
            preferences.alignment_for_parameters_in_method_declaration);

        /*
         * Check for extra dimensions
         */
        int extraDimensions = getDimensions();
        if (extraDimensions != 0) {
          for (int i = 0; i < extraDimensions; i++) {
            scribe.printNextToken(Token.LBRACK);
            scribe.printNextToken(Token.RBRACK);
          }
        }
        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(methodDeclAlignment, true);

    DartBlock body = node.getFunction().getBody();
    if (body != null) {
      // may be native
      if (node.getModifiers().isNative()) {
        body.accept(this);
        scribe.printComment();
        return null;
      }
      /*
       * Method body
       */
      String method_declaration_brace = preferences.brace_position_for_method_declaration;
      formatLeftCurlyBrace(line, method_declaration_brace);
      formatOpeningBrace(
          method_declaration_brace,
          preferences.insert_space_before_opening_brace_in_method_declaration);
      final int numberOfBlankLinesAtBeginningOfMethodBody = preferences.blank_lines_at_beginning_of_method_body;
      if (numberOfBlankLinesAtBeginningOfMethodBody > 0) {
        scribe.printEmptyLines(numberOfBlankLinesAtBeginningOfMethodBody);
      }
      final List<DartStatement> statements = body.getStatements();
      if (statements != null) {
        scribe.printNewLine();
        if (preferences.indent_statements_compare_to_body) {
          scribe.indent();
        }
        formatStatements(statements, true);
        scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_METHOD_DECLARATION);
        if (preferences.indent_statements_compare_to_body) {
          scribe.unIndent();
        }
      } else {
        if (preferences.insert_new_line_in_empty_method_body) {
          scribe.printNewLine();
        }
        if (preferences.indent_statements_compare_to_body) {
          scribe.indent();
        }
        scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_METHOD_DECLARATION);
        if (preferences.indent_statements_compare_to_body) {
          scribe.unIndent();
        }
      }
      scribe.printNextToken(Token.RBRACE);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
      if (method_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
        scribe.unIndent();
      }
    } else {
      // no method body
      scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.COMPLEX_TRAILING_COMMENT);
    }
    return null;
  }

  @Override
  public DartNode visitMethodInvocation(DartMethodInvocation invocation) {

    final int numberOfParens = printOpenParens(invocation);
    CascadingInvocationFragmentBuilder builder = buildFragments(invocation);

    if (builder.size() >= 3 && numberOfParens == 0) {
      formatCascadingInvocations(builder);
    } else {
      Alignment messageAlignment = null;
      if (!(invocation.getTarget() instanceof DartThisExpression)) {
        invocation.getTarget().accept(this);
        int alignmentMode = preferences.alignment_for_selector_in_method_invocation;
        messageAlignment = scribe.createAlignment(
            Alignment.MESSAGE_SEND,
            alignmentMode,
            1,
            scribe.scanner.currentPosition);
        scribe.enterAlignment(messageAlignment);
        boolean ok = false;
        do {
          switch (alignmentMode & Alignment.SPLIT_MASK) {
            case Alignment.M_COMPACT_SPLIT:
            case Alignment.M_NEXT_PER_LINE_SPLIT:
              messageAlignment.startingColumn = scribe.column;
              break;
          }
          try {
            formatInvocation(invocation, messageAlignment);
            ok = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!ok);
        scribe.exitAlignment(messageAlignment, true);
      } else {
        invocation.getTarget().accept(this);
        scribe.printNextToken(Token.PERIOD);
        formatInvocation(invocation, null);
      }
    }
    printCloseParens(invocation, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitNamedExpression(DartNamedExpression node) {

    return null;
  }

  @Override
  public DartNode visitNativeBlock(DartNativeBlock node) {
    scribe.space();
    scribe.printNextToken(Token.IDENTIFIER);
    scribe.printNextToken(Token.SEMICOLON);
    return null;
  }

  @Override
  public DartNode visitNativeDirective(DartNativeDirective node) {

    return null;
  }

  @Override
  public DartNode visitNewExpression(DartNewExpression allocationExpression) {
    final int numberOfParens = printOpenParens(allocationExpression);
    scribe.printNextToken(Token.NEW);
    scribe.space();

    allocationExpression.getConstructor().accept(this);

    scribe.printNextToken(
        Token.LPAREN,
        preferences.insert_space_before_opening_paren_in_method_invocation);

    final List<DartExpression> arguments = allocationExpression.getArguments();
    if (arguments != null) {
      if (preferences.insert_space_after_opening_paren_in_method_invocation) {
        scribe.space();
      }
      int argumentLength = arguments.size();
      Alignment argumentsAlignment = scribe.createAlignment(
          Alignment.ALLOCATION,
          preferences.alignment_for_arguments_in_allocation_expression,
          argumentLength,
          scribe.scanner.currentPosition);
      scribe.enterAlignment(argumentsAlignment);
      boolean ok = false;
      do {
        try {
          for (int i = 0; i < argumentLength; i++) {
            if (i > 0) {
              scribe.printNextToken(
                  Token.COMMA,
                  preferences.insert_space_before_comma_in_allocation_expression);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            }
            scribe.alignFragment(argumentsAlignment, i);
            if (i > 0 && preferences.insert_space_after_comma_in_allocation_expression) {
              scribe.space();
            }
            arguments.get(i).accept(this);
          }
          ok = true;
        } catch (AlignmentException e) {
          scribe.redoAlignment(e);
        }
      } while (!ok);
      scribe.exitAlignment(argumentsAlignment, true);
      scribe.printNextToken(
          Token.RPAREN,
          preferences.insert_space_before_closing_paren_in_method_invocation);
    } else {
      scribe.printNextToken(
          Token.RPAREN,
          preferences.insert_space_between_empty_parens_in_method_invocation);
    }

    printCloseParens(allocationExpression, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitNode(DartNode node) {
    // this method should not be invoked
    return reportMissingNodeFormatter(DartNode.class, node);
  }

  @Override
  public DartNode visitNullLiteral(DartNullLiteral node) {
    final int numberOfParens = printOpenParens(node);
    scribe.printNextToken(Token.NULL_LITERAL);
    printCloseParens(node, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitParameter(DartParameter param) {
    // TODO use alignment, esp for default vals & fn params
    if (param.getModifiers().isConstant()) {
      scribe.printComment();
      if (scribe.printModifiers()) {
        scribe.space();
      }
    }

    /*
     * Argument type
     */
    if (param.getTypeNode() != null) {
      param.getTypeNode().accept(this);
      scribe.space();
    }

    /*
     * Print the argument name
     */
    param.getName().accept(this);

    if (param.getFunctionParameters() != null) {
      // TODO make a new pref for pre/post spaces in function params
      formatMethodArguments(
          param.getFunctionParameters(),
          preferences.insert_space_before_opening_paren_in_method_declaration,
          preferences.insert_space_between_empty_parens_in_method_declaration,
          preferences.insert_space_before_closing_paren_in_method_declaration,
          preferences.insert_space_after_opening_paren_in_method_declaration,
          preferences.insert_space_before_comma_in_method_declaration_parameters,
          preferences.insert_space_after_comma_in_method_declaration_parameters,
          preferences.alignment_for_parameters_in_method_declaration);
    }

    if (param.getDefaultExpr() != null) {
      scribe.printNextToken(Token.ASSIGN, preferences.insert_space_before_assignment_operator);
      if (preferences.insert_space_after_assignment_operator) {
        scribe.space();
      }
      param.getDefaultExpr().accept(this);
    }
    return null;
  }

  @Override
  public DartNode visitParameterizedTypeNode(DartParameterizedTypeNode node) {

    return null;
  }

  @Override
  public DartNode visitParenthesizedExpression(DartParenthesizedExpression node) {
    if (node.getExpression() != null) {
      node.getExpression().accept(this);
    } else {
      scribe.printNextToken(Token.LPAREN);
      scribe.printNextToken(Token.RPAREN);
    }
    return null;
  }

  @Override
  public DartNode visitPropertyAccess(DartPropertyAccess node) {
    node.getQualifier().accept(this);
    scribe.printNextToken(Token.PERIOD);
    node.getName().accept(this);
    return null;
  }

  @Override
  public DartNode visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {

    return null;
  }

  @Override
  public DartNode visitResourceDirective(DartResourceDirective node) {

    return null;
  }

  @Override
  public DartNode visitReturnStatement(DartReturnStatement node) {
    scribe.printNextToken(Token.RETURN);
    final DartExpression expression = node.getValue();
    if (expression != null) {
      final int numberOfParens = countParens(node);
      if (numberOfParens != 0 && preferences.insert_space_before_parenthesized_expression_in_return
          || numberOfParens == 0) {
        scribe.space();
      }
      expression.accept(this);
    }
    /*
     * Print the semi-colon
     */
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitSourceDirective(DartSourceDirective node) {

    return null;
  }

  @Override
  public DartNode visitStatement(DartStatement node) {
    // this method should not be invoked
    return reportMissingNodeFormatter(DartStatement.class, node);
  }

  @Override
  public DartNode visitStringInterpolation(DartStringInterpolation node) {
    Iterator<DartExpression> iter = node.getExpressions().iterator();
    List<DartStringLiteral> strings = node.getStrings();
    for (int i = 0; i < strings.size() - 1; i++) {
      DartStringLiteral lit = strings.get(i);
      if (lit.getValue().length() > 0) {
        scribe.printNextToken(Token.STRING_SEGMENT);
      }
      scribe.printNextToken(Token.STRING_EMBED_EXP_START); // "${"
      iter.next().accept(this);
      scribe.printNextToken(Token.STRING_EMBED_EXP_END, false); // "}"
    }
    scribe.continueStringInterpolation();
    scribe.printNextTokenNoComment(Token.STRING_LAST_SEGMENT, false);
    return null;
  }

  @Override
  public DartNode visitStringLiteral(DartStringLiteral node) {
    scribe.printNextToken(Token.STRING);
    return null;
  }

  @Override
  public DartNode visitSuperConstructorInvocation(DartSuperConstructorInvocation sup) {
    DartIdentifier supName = sup.getName();
    scribe.printNextToken(Token.SUPER);
    if (supName != null) {
      scribe.printNextToken(Token.PERIOD);
      scribe.printNextToken(Token.IDENTIFIER);
    }
    formatMethodArguments(
        sup.getArguments(),
        preferences.insert_space_before_opening_paren_in_constructor_declaration,
        preferences.insert_space_between_empty_parens_in_constructor_declaration,
        preferences.insert_space_before_closing_paren_in_constructor_declaration,
        preferences.insert_space_after_opening_paren_in_constructor_declaration,
        preferences.insert_space_before_comma_in_constructor_declaration_parameters,
        preferences.insert_space_after_comma_in_constructor_declaration_parameters,
        preferences.alignment_for_parameters_in_constructor_declaration);
    return null;
  }

  @Override
  public DartNode visitSuperExpression(DartSuperExpression node) {
    scribe.printNextToken(Token.SUPER);
    return null;
  }

  @Override
  public DartNode visitSwitchMember(DartSwitchMember node) {
    // this method should not be invoked
    return visitNode(node);
  }

  @Override
  public DartNode visitSwitchStatement(DartSwitchStatement switchStatement) {
    scribe.printNextToken(Token.SWITCH);
    scribe.printNextToken(Token.LPAREN, preferences.insert_space_before_opening_paren_in_switch);

    if (preferences.insert_space_after_opening_paren_in_switch) {
      scribe.space();
    }
    switchStatement.getExpression().accept(this);
    scribe.printNextToken(Token.RPAREN, preferences.insert_space_before_closing_paren_in_switch);
    /*
     * Type body
     */
    String switch_brace = preferences.brace_position_for_switch;
    formatOpeningBrace(switch_brace, preferences.insert_space_before_opening_brace_in_switch);
    scribe.printNewLine();

    final List<DartSwitchMember> cases = switchStatement.getMembers();
    int switchIndentationLevel = scribe.indentationLevel;
    int caseIndentation = 0;
    int statementIndentation = 0;
    int breakIndentation = 0;
    if (preferences.indent_switchstatements_compare_to_switch) {
      caseIndentation++;
      statementIndentation++;
      breakIndentation++;
    }
    if (preferences.indent_switchstatements_compare_to_cases) {
      statementIndentation++;
    }
    if (preferences.indent_breaks_compare_to_cases) {
      breakIndentation++;
    }
    for (DartSwitchMember member : cases) {
      member.accept(this);
    }
    scribe.setIndentation(switchIndentationLevel, 0);
    scribe.printComment();
    scribe.printNextToken(Token.RBRACE);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    if (switch_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
      scribe.unIndent();
    }
    return null;
  }

  @Override
  public DartNode visitSyntheticErrorExpression(DartSyntheticErrorExpression node) {

    return null;
  }

  @Override
  public DartNode visitSyntheticErrorStatement(DartSyntheticErrorStatement node) {

    return null;
  }

  @Override
  public DartNode visitThisExpression(DartThisExpression node) {
    scribe.printNextToken(Token.THIS);
    return null;
  }

  @Override
  public DartNode visitThrowStatement(DartThrowStatement throwStatement) {
    scribe.printNextToken(Token.THROW);
    DartExpression expression = throwStatement.getException();
    final int numberOfParens = countParens(expression);
    if (numberOfParens > 0 && preferences.insert_space_before_parenthesized_expression_in_throw
        || numberOfParens == 0) {
      scribe.space();
    }
    expression.accept(this);
    /*
     * Print the semi-colon
     */
    scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    return null;
  }

  @Override
  public DartNode visitTryStatement(DartTryStatement node) {
    DartBlock tryBlock = node.getTryBlock();
    scribe.printNextToken(Token.TRY);
    tryBlock.accept(this);
    List<DartCatchBlock> catches = node.getCatchBlocks();
    for (DartCatchBlock catchBlock : catches) {
      if (preferences.insert_new_line_before_catch_in_try_statement) {
        scribe.printNewLine();
      }
      catchBlock.accept(this);
    }
    DartBlock finallyBlock = node.getFinallyBlock();
    if (finallyBlock != null) {
      if (preferences.insert_new_line_before_finally_in_try_statement) {
        scribe.printNewLine();
      }
      scribe.printNextToken(Token.FINALLY);
      finallyBlock.accept(this);
    }
    return null;
  }

  @Override
  public DartNode visitTypeExpression(DartTypeExpression node) {
    // in theory, expressions can have parens; in practice, can it happen here?
    final int numberOfParens = printOpenParens(node);
    node.getTypeNode().accept(this);
    printCloseParens(node, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitTypeNode(DartTypeNode node) {
    final int numberOfParens = printOpenParens(node);
    scribe.printNextToken(TYPEREFERENCE_EXPECTEDTOKENS);
    DartNode expr = node.getIdentifier();
    if (expr instanceof DartPropertyAccess) {
      // named constructor
      scribe.printNextToken(Token.PERIOD);
      scribe.printNextToken(TYPEREFERENCE_EXPECTEDTOKENS);
    }
    List<DartTypeNode> types = node.getTypeArguments();
    if (types != null && !types.isEmpty()) {
      scribe.printNextToken(Token.LT);
      if (preferences.insert_space_after_opening_angle_bracket_in_type_arguments) {
        scribe.space();
      }
      types.get(0).accept(this);
      for (int i = 1; i < types.size(); i++) {
        scribe.printNextToken(Token.COMMA, preferences.insert_space_before_comma_in_type_arguments);
        if (preferences.insert_space_after_comma_in_type_arguments) {
          scribe.space();
        }
        types.get(i).accept(this);
      }
      if (isClosingGenericToken()) {
        scribe.printNextToken(
            CLOSING_GENERICS_EXPECTEDTOKENS,
            preferences.insert_space_before_closing_angle_bracket_in_type_arguments);
      }
    }
    printCloseParens(node, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitTypeParameter(DartTypeParameter node) {
    node.getName().accept(this);
    return null;
  }

  @Override
  public DartNode visitUnaryExpression(DartUnaryExpression unaryExpression) {
    final int numberOfParens = printOpenParens(unaryExpression);

    DartExpression expr = unaryExpression.getArg();
    if (!unaryExpression.isPrefix()) {
      expr.accept(this);
    }
    scribe.printNextToken(
        unaryExpression.getOperator(),
        preferences.insert_space_before_unary_operator);
    if (preferences.insert_space_after_unary_operator) {
      scribe.space();
    }
    if (unaryExpression.isPrefix()) {
      expr.accept(this);
    }

    printCloseParens(unaryExpression, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitUnit(DartUnit compilationUnitDeclaration) {

    // fake new line to handle empty lines at beginning
    scribe.lastNumberOfNewLines = 1;

    // Set header end position
    final List<DartDirective> directives = compilationUnitDeclaration.getDirectives();
    final List<DartNode> types = compilationUnitDeclaration.getTopLevelNodes();
    int headerEndPosition = types == null || types.isEmpty()
        ? compilationUnitDeclaration.getSourceInfo().getOffset()
            + compilationUnitDeclaration.getSourceInfo().getLength()
        : types.get(0).getSourceInfo().getOffset();
    scribe.setHeaderComment(headerEndPosition);
    scribe.printComment();

    formatEmptyTypeDeclaration(true);

    /*
     * Directives
     */
    if (preferences.blank_lines_before_imports != 0) {
      scribe.printEmptyLines(preferences.blank_lines_before_imports);
    }

    if (directives != null && !directives.isEmpty()) {
      for (DartDirective directive : directives) {
        directive.accept(this);
      }
    }

    if (preferences.blank_lines_after_imports != 0) {
      scribe.printEmptyLines(preferences.blank_lines_after_imports);
    } else {
      scribe.printNewLine();
    }

    /*
     * Top-level declarations
     */
    int blankLineBetweenTypeDeclarations = preferences.blank_lines_between_type_declarations;
    if (types != null && !types.isEmpty()) {
      int typesLength = types.size();
      for (int i = 0; i < typesLength - 1; i++) {
        types.get(i).accept(this);
        formatEmptyTypeDeclaration(false);
        if (blankLineBetweenTypeDeclarations != 0) {
          scribe.printEmptyLines(blankLineBetweenTypeDeclarations);
        } else {
          scribe.printNewLine();
        }
      }
      types.get(typesLength - 1).accept(this);
    }
    scribe.printEndOfCompilationUnit();
    return null;
  }

  @Override
  public DartNode visitUnqualifiedInvocation(DartUnqualifiedInvocation invocation) {

    final int numberOfParens = printOpenParens(invocation);
    CascadingInvocationFragmentBuilder builder = buildFragments(invocation);

    if (builder.size() >= 3 && numberOfParens == 0) {
      formatCascadingInvocations(builder);
    } else {
      Alignment messageAlignment = null;
      invocation.getTarget().accept(this);
      int alignmentMode = preferences.alignment_for_selector_in_method_invocation;
      messageAlignment = scribe.createAlignment(
          Alignment.MESSAGE_SEND,
          alignmentMode,
          1,
          scribe.scanner.currentPosition);
      scribe.enterAlignment(messageAlignment);
      boolean ok = false;
      do {
        switch (alignmentMode & Alignment.SPLIT_MASK) {
          case Alignment.M_COMPACT_SPLIT:
          case Alignment.M_NEXT_PER_LINE_SPLIT:
            messageAlignment.startingColumn = scribe.column;
            break;
        }
        try {
          formatInvocation(invocation, messageAlignment);
          ok = true;
        } catch (AlignmentException e) {
          scribe.redoAlignment(e);
        }
      } while (!ok);
      scribe.exitAlignment(messageAlignment, true);
    }
    printCloseParens(invocation, numberOfParens);
    return null;
  }

  @Override
  public DartNode visitVariable(DartVariable node) {
    node.getName().accept(this);
    if (node.getValue() != null) {
      scribe.printNextToken(Token.ASSIGN, preferences.insert_space_before_assignment_operator);
      if (preferences.insert_space_after_assignment_operator) {
        scribe.space();
      }
      node.getValue().accept(this);
    }
    return null;
  }

  @Override
  public DartNode visitVariableStatement(DartVariableStatement node) {
    // TODO no way currently to distinguish const decls
    DartTypeNode type = node.getTypeNode();
    List<DartVariable> vars = node.getVariables();
    if (type == null) { // var stmt
      scribe.printNextToken(Token.VAR);
    } else {
      type.accept(this);
    }
    // TODO use alignment
    scribe.space();
    vars.get(0).accept(this);
    for (int i = 1; i < vars.size(); i++) {
      scribe.printNextToken(
          Token.COMMA,
          preferences.insert_space_before_comma_in_multiple_local_declarations);
      if (preferences.insert_space_after_comma_in_multiple_local_declarations) {
        scribe.space();
      }
      vars.get(i).accept(this);
    }
    return null;
  }

  @Override
  public DartNode visitWhileStatement(DartWhileStatement whileStatement) {

    scribe.printNextToken(Token.WHILE);
    final int line = scribe.line;
    scribe.printNextToken(Token.LPAREN, preferences.insert_space_before_opening_paren_in_while);

    if (preferences.insert_space_after_opening_paren_in_while) {
      scribe.space();
    }
    whileStatement.getCondition().accept(this);

    scribe.printNextToken(Token.RPAREN, preferences.insert_space_before_closing_paren_in_while);

    final DartStatement action = whileStatement.getBody();
    if (action != null) {
      if (action instanceof DartBlock) {
        formatLeftCurlyBrace(line, preferences.brace_position_for_block);
        action.accept(this);
      } else if (action instanceof DartEmptyStatement) {
        /*
         * This is an empty statement
         */
        formatNecessaryEmptyStatement();
      } else {
        scribe.printNewLine();
        scribe.indent();
        action.accept(this);
        scribe.unIndent();
      }
    } else {
      /*
       * This is an empty statement
       */
      formatNecessaryEmptyStatement();
    }
    return null;
  }

  private BinaryExpressionFragmentBuilder buildFragments(DartBinaryExpression binaryExpression) {
    BinaryExpressionFragmentBuilder builder = new BinaryExpressionFragmentBuilder();
    switch (binaryExpression.getOperator()) {
      case MUL:
      case ADD:
      case DIV:
      case TRUNC:
      case MOD:
      case BIT_XOR:
      case SUB:
      case OR:
      case AND:
      case BIT_AND:
      case BIT_OR:
        builder.accum(binaryExpression);
        break;
    }
    return builder;
  }

  private CascadingInvocationFragmentBuilder buildFragments(DartInvocation invocation) {
    CascadingInvocationFragmentBuilder builder = new CascadingInvocationFragmentBuilder();
    invocation.accept(builder);
    return builder;
  }

  private boolean commentStartsBlock(int start, int end) {
    localScanner.resetTo(start, end);
    try {
      if (localScanner.peekNextToken() == Token.WHITESPACE) {
        localScanner.getNextToken();
      }
      if (localScanner.getNextToken() == Token.LBRACE) {
        if (localScanner.peekNextToken() == Token.WHITESPACE) {
          localScanner.getNextToken();
        }
        if (localScanner.getNextToken() == Token.COMMENT) {
          return true;
        }
      }
    } catch (InvalidInputException e) {
      // ignore
    }
    return false;
  }

  private DartNode[] computeMergedMemberDeclarations(DartNode[] nodes) {
    // TODO remove
    ArrayList<DartNode> mergedNodes = new ArrayList<DartNode>();
    for (int i = 0, max = nodes.length; i < max; i++) {
      DartNode currentNode = nodes[i];
      if (currentNode instanceof DartFieldDefinition) {
        DartFieldDefinition currentField = (DartFieldDefinition) currentNode;
        if (mergedNodes.size() == 0) {
          // first node
          mergedNodes.add(currentNode);
        } else {
          // we need to check if the previous merged node is a field declaration
          DartNode previousMergedNode = mergedNodes.get(mergedNodes.size() - 1);
        }
      } else {
        mergedNodes.add(currentNode);
      }
    }
    if (mergedNodes.size() != nodes.length) {
      DartNode[] result = new DartNode[mergedNodes.size()];
      mergedNodes.toArray(result);
      return result;
    } else {
      return nodes;
    }
  }

  private int countParens(DartNode node) {
    DartNode p = node.getParent();
    int n = 0;
    while (p instanceof DartParenthesizedExpression) {
      n += 1;
      p = p.getParent();
    }
    return n;
  }

  private TextEdit failedToFormat(AbortFormatting ex) {
    System.out.println("COULD NOT FORMAT \n" + scribe.scanner); //$NON-NLS-1$
    if (ex != null) {
      System.out.println(ex.getMessage());
    }
    System.out.println(scribe);
    if (ex != null) {
      ex.printStackTrace();
    }
    return null;
  }

  private DartNode format(DartClass typeDef) {
    /*
     * Print comments to get proper line number
     */
    scribe.printComment();
    int line = scribe.line;
    /*
     * Type name
     */
    scribe.printNextToken(Token.IDENTIFIER, true); // "class" or "interface"
    scribe.printNextToken(Token.IDENTIFIER, true); // print type name
    List<DartTypeParameter> typeParameters = typeDef.getTypeParameters();
    if (typeParameters != null && !typeParameters.isEmpty()) {
      scribe.printNextToken(
          Token.LT,
          preferences.insert_space_before_opening_angle_bracket_in_type_parameters);
      if (preferences.insert_space_after_opening_angle_bracket_in_type_parameters) {
        scribe.space();
      }
      int length = typeParameters.size();
      for (int i = 0; i < length - 1; i++) {
        typeParameters.get(i).accept(this);
        scribe.printNextToken(Token.COMMA, preferences.insert_space_before_comma_in_type_parameters);
        if (preferences.insert_space_after_comma_in_type_parameters) {
          scribe.space();
        }
      }
      typeParameters.get(length - 1).accept(this);
      if (isClosingGenericToken()) {
        scribe.printNextToken(
            CLOSING_GENERICS_EXPECTEDTOKENS,
            preferences.insert_space_before_closing_angle_bracket_in_type_parameters);
      }
      if (preferences.insert_space_after_closing_angle_bracket_in_type_parameters) {
        scribe.space();
      }
    }
    /*
     * Superclass
     */
    final DartTypeNode superclass = typeDef.getSuperclass();
    if (superclass != null) {
      Alignment superclassAlignment = scribe.createAlignment(
          Alignment.SUPER_CLASS,
          preferences.alignment_for_superclass_in_type_declaration,
          2,
          scribe.scanner.currentPosition);
      scribe.enterAlignment(superclassAlignment);
      boolean ok = false;
      do {
        try {
          scribe.alignFragment(superclassAlignment, 0);
          scribe.printNextToken(Token.IDENTIFIER, true); // "extends"
          scribe.alignFragment(superclassAlignment, 1);
          scribe.space();
          superclass.accept(this);
          ok = true;
        } catch (AlignmentException e) {
          scribe.redoAlignment(e);
        }
      } while (!ok);
      scribe.exitAlignment(superclassAlignment, true);
    }
    /*
     * Super Interfaces
     */
    final List<DartTypeNode> superInterfaces = typeDef.getInterfaces();
    if (superInterfaces != null) {
      int alignment_for_superinterfaces = preferences.alignment_for_superinterfaces_in_type_declaration;
      int superInterfaceLength = superInterfaces.size();
      Alignment interfaceAlignment = scribe.createAlignment(
          Alignment.SUPER_INTERFACES,
          alignment_for_superinterfaces,
          superInterfaceLength + 1, // implements token is first fragment
          scribe.scanner.currentPosition);
      scribe.enterAlignment(interfaceAlignment);
      boolean ok = false;
      do {
        try {
          scribe.alignFragment(interfaceAlignment, 0);
          // print "extends" or "implements"
          scribe.printNextToken(Token.IDENTIFIER, true);
          for (int i = 0; i < superInterfaceLength; i++) {
            if (i > 0) {
              scribe.printNextToken(
                  Token.COMMA,
                  preferences.insert_space_before_comma_in_superinterfaces);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
              scribe.alignFragment(interfaceAlignment, i + 1);
              if (preferences.insert_space_after_comma_in_superinterfaces) {
                scribe.space();
              }
              superInterfaces.get(i).accept(this);
            } else {
              scribe.alignFragment(interfaceAlignment, i + 1);
              scribe.space();
              superInterfaces.get(i).accept(this);
            }
          }
          ok = true;
        } catch (AlignmentException e) {
          scribe.redoAlignment(e);
        }
      } while (!ok);
      scribe.exitAlignment(interfaceAlignment, true);
    }
    /*
     * Default class (for interfaces)
     */
    DartParameterizedTypeNode defaultClass = typeDef.getDefaultClass();
    if (defaultClass != null) {
      // TODO consider not reusing superclass alignment
      Alignment defaultClassAlignment = scribe.createAlignment(
          Alignment.SUPER_CLASS,
          preferences.alignment_for_superclass_in_type_declaration,
          2,
          scribe.scanner.currentPosition);
      scribe.enterAlignment(defaultClassAlignment);
      boolean ok = false;
      do {
        try {
          scribe.alignFragment(defaultClassAlignment, 0);
          scribe.printNextToken(Token.DEFAULT, true); // "default"
          scribe.alignFragment(defaultClassAlignment, 1);
          scribe.space();
          defaultClass.accept(this);
          ok = true;
        } catch (AlignmentException e) {
          scribe.redoAlignment(e);
        }
      } while (!ok);
      scribe.exitAlignment(defaultClassAlignment, true);
    }
    /*
     * Type body
     */
    String class_declaration_brace = preferences.brace_position_for_type_declaration;
    boolean space_before_opening_brace = preferences.insert_space_before_opening_brace_in_type_declaration;
    formatLeftCurlyBrace(line, class_declaration_brace);
    formatTypeOpeningBrace(class_declaration_brace, space_before_opening_brace, typeDef);

    boolean indent_body_declarations_compare_to_header = preferences.indent_body_declarations_compare_to_type_header;
    if (indent_body_declarations_compare_to_header) {
      scribe.indent();
    }

    formatTypeMembers(typeDef);

    if (indent_body_declarations_compare_to_header) {
      scribe.unIndent();
    }

    if (preferences.insert_new_line_in_empty_type_declaration) {
      scribe.printNewLine();
    }
    scribe.printNextToken(Token.RBRACE);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    if (class_declaration_brace.equals(DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED)) {
      scribe.unIndent();
    }
    if (hasComments()) {
      scribe.printNewLine();
    }
    return null;
  }

  private void format(DartFieldDefinition multiFieldDeclaration, ASTVisitor<DartNode> visitor,
      boolean isChunkStart, boolean isFirstClassBodyDeclaration) {

    if (isFirstClassBodyDeclaration) {
      int newLinesBeforeFirstClassBodyDeclaration = preferences.blank_lines_before_first_class_body_declaration;
      if (newLinesBeforeFirstClassBodyDeclaration > 0) {
        scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
      }
    } else {
      int newLineBeforeChunk = isChunkStart ? preferences.blank_lines_before_new_chunk : 0;
      if (newLineBeforeChunk > 0) {
        scribe.printEmptyLines(newLineBeforeChunk);
      }
      final int newLinesBeforeField = preferences.blank_lines_before_field;
      if (newLinesBeforeField > 0) {
        scribe.printEmptyLines(newLinesBeforeField);
      }
    }
    Alignment fieldAlignment = scribe.getMemberAlignment();

    scribe.printComment();
    scribe.printModifiers();
    scribe.space();

    DartTypeNode type = multiFieldDeclaration.getTypeNode();
    if (type == null) {
      scribe.printComment(
          CodeFormatter.K_UNKNOWN,
          Scribe.NO_TRAILING_COMMENT,
          Scribe.PRESERVE_EMPTY_LINES_KEEP_LAST_NEW_LINES_INDENTATION);
      if (scribe.scanner.peekNextToken() == Token.VAR) {
        scribe.printNextToken(Token.VAR);
      }
    } else {
      type.accept(this);
    }

    final int multipleFieldDeclarationsLength = multiFieldDeclaration.getFields().size();

    Alignment multiFieldDeclarationsAlignment = scribe.createAlignment(
        Alignment.MULTIPLE_FIELD,
        preferences.alignment_for_multiple_fields,
        multipleFieldDeclarationsLength - 1,
        scribe.scanner.currentPosition);
    scribe.enterAlignment(multiFieldDeclarationsAlignment);

    boolean ok = false;
    do {
      try {
        for (int i = 0, length = multipleFieldDeclarationsLength; i < length; i++) {
          DartField fieldDeclaration = multiFieldDeclaration.getFields().get(i);
          /*
           * Field name
           */
          if (i == 0) {
            scribe.alignFragment(fieldAlignment, 0);
            scribe.printNextToken(Token.IDENTIFIER, true);
          } else {
            scribe.printNextToken(Token.IDENTIFIER, false);
          }

          /*
           * Check for extra dimensions
           */
          int extraDimensions = getDimensions();
          if (extraDimensions != 0) {
            for (int index = 0; index < extraDimensions; index++) {
              scribe.printNextToken(Token.LBRACK);
              scribe.printNextToken(Token.RBRACK);
            }
          }

          /*
           * Field initialization
           */
          final DartExpression initialization = fieldDeclaration.getValue();
          if (initialization != null) {
            if (i == 0) {
              scribe.alignFragment(fieldAlignment, 1);
            }
            scribe.printNextToken(Token.ASSIGN, preferences.insert_space_before_assignment_operator);
            if (preferences.insert_space_after_assignment_operator) {
              scribe.space();
            }
            initialization.accept(this);
          }

          if (i != length - 1) {
            scribe.printNextToken(
                Token.COMMA,
                preferences.insert_space_before_comma_in_multiple_field_declarations);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            scribe.alignFragment(multiFieldDeclarationsAlignment, i);

            if (preferences.insert_space_after_comma_in_multiple_field_declarations) {
              scribe.space();
            }
          } else {
            scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
            scribe.alignFragment(fieldAlignment, 2);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
          }
        }
        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(multiFieldDeclarationsAlignment, true);
  }

  private void format(DartMethodDefinition methodDeclaration, boolean isChunkStart,
      boolean isFirstClassBodyDeclaration) {

    if (isFirstClassBodyDeclaration) {
      int newLinesBeforeFirstClassBodyDeclaration = preferences.blank_lines_before_first_class_body_declaration;
      if (newLinesBeforeFirstClassBodyDeclaration > 0) {
        scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
      }
    } else {
      final int newLineBeforeChunk = isChunkStart ? preferences.blank_lines_before_new_chunk : 0;
      if (newLineBeforeChunk > 0) {
        scribe.printEmptyLines(newLineBeforeChunk);
      }
    }
    final int newLinesBeforeMethod = preferences.blank_lines_before_method;
    if (newLinesBeforeMethod > 0 && !isFirstClassBodyDeclaration) {
      scribe.printEmptyLines(newLinesBeforeMethod);
    } else if (scribe.line != 0 || scribe.column != 1) {
      scribe.printNewLine();
    }
    methodDeclaration.accept(this);
  }

  private void formatBlock(DartBlock block, String block_brace_position,
      boolean insertSpaceBeforeOpeningBrace) {
    formatOpeningBrace(block_brace_position, insertSpaceBeforeOpeningBrace);
    final List<DartStatement> statements = block.getStatements();
    if (!statements.isEmpty()) {
      scribe.printNewLine();
      if (preferences.indent_statements_compare_to_block) {
        scribe.indent();
      }
      formatStatements(statements, true);
      scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_BLOCK);

      if (preferences.indent_statements_compare_to_block) {
        scribe.unIndent();
      }
    } else if (preferences.insert_new_line_in_empty_block) {
      scribe.printNewLine();
      if (preferences.indent_statements_compare_to_block) {
        scribe.indent();
      }
      scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_BLOCK);

      if (preferences.indent_statements_compare_to_block) {
        scribe.unIndent();
      }
    } else {
      if (preferences.indent_statements_compare_to_block) {
        scribe.indent();
      }
      scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_AT_END_OF_BLOCK);

      if (preferences.indent_statements_compare_to_block) {
        scribe.unIndent();
      }
    }
    scribe.printNextToken(Token.RBRACE);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    if (DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED.equals(block_brace_position)) {
      scribe.unIndent();
    }
  }

  private void formatCascadingInvocations(CascadingInvocationFragmentBuilder builder) {
    int size = builder.size();
    DartInvocation[] fragments = builder.fragments();
    DartExpression fragment = null;
    boolean implicitThis = false;;
    if (fragments[0] instanceof DartMethodInvocation) {
      fragment = ((DartMethodInvocation) fragments[0]).getTarget();
    } else if (fragments[0] instanceof DartUnqualifiedInvocation) {
      implicitThis = true;
      fragment = ((DartUnqualifiedInvocation) fragments[0]).getTarget();
    } else {
      fragment = ((DartFunctionObjectInvocation) fragments[0]).getTarget();
      implicitThis = fragment instanceof DartIdentifier;
    }
    int startingPositionInCascade = 1;
    if (!implicitThis) {
      // not implicit this
      fragment.accept(this);
    } else {
      DartInvocation currentInvocation = fragments[1];
      final int numberOfParens = printOpenParens(currentInvocation);
      List<DartExpression> arguments = currentInvocation.getArguments();
      scribe.printNextToken(Token.IDENTIFIER); // selector
      scribe.printNextToken(
          Token.LPAREN,
          preferences.insert_space_before_opening_paren_in_method_invocation);
      if (arguments != null) {
        if (preferences.insert_space_after_opening_paren_in_method_invocation) {
          scribe.space();
        }
        int argumentLength = arguments.size();
        Alignment argumentsAlignment = scribe.createAlignment(
            Alignment.MESSAGE_ARGUMENTS,
            preferences.alignment_for_arguments_in_method_invocation,
            Alignment.R_OUTERMOST,
            argumentLength,
            scribe.scanner.currentPosition);
        scribe.enterAlignment(argumentsAlignment);
        boolean okForArguments = false;
        do {
          try {
            for (int j = 0; j < argumentLength; j++) {
              if (j > 0) {
                scribe.printNextToken(
                    Token.COMMA,
                    preferences.insert_space_before_comma_in_method_invocation_arguments);
                scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
              }
              scribe.alignFragment(argumentsAlignment, j);
              if (j > 0 && preferences.insert_space_after_comma_in_method_invocation_arguments) {
                scribe.space();
              }
              arguments.get(j).accept(this);
            }
            okForArguments = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!okForArguments);
        scribe.exitAlignment(argumentsAlignment, true);
        scribe.printNextToken(
            Token.RPAREN,
            preferences.insert_space_before_closing_paren_in_method_invocation);
      } else {
        scribe.printNextToken(
            Token.RPAREN,
            preferences.insert_space_between_empty_parens_in_method_invocation);
      }
      printCloseParens(currentInvocation, numberOfParens);
      startingPositionInCascade = 2;
    }
    int tieBreakRule = preferences.wrap_outer_expressions_when_nested
        && size - startingPositionInCascade > 2 ? Alignment.R_OUTERMOST : Alignment.R_INNERMOST;
    Alignment cascadingInvocationAlignment = scribe.createAlignment(
        Alignment.CASCADING_MESSAGE_SEND,
        preferences.alignment_for_selector_in_method_invocation,
        tieBreakRule,
        size,
        scribe.scanner.currentPosition);
    scribe.enterAlignment(cascadingInvocationAlignment);
    boolean ok = false;
    boolean setStartingColumn = true;
    switch (preferences.alignment_for_arguments_in_method_invocation & Alignment.SPLIT_MASK) {
      case Alignment.M_COMPACT_FIRST_BREAK_SPLIT:
      case Alignment.M_NEXT_SHIFTED_SPLIT:
      case Alignment.M_ONE_PER_LINE_SPLIT:
        setStartingColumn = false;
        break;
    }
    do {
      if (setStartingColumn) {
        cascadingInvocationAlignment.startingColumn = scribe.column;
      }
      try {
        scribe.alignFragment(cascadingInvocationAlignment, 0);
        scribe.printNextToken(Token.PERIOD);
        for (int i = startingPositionInCascade; i < size; i++) {
          DartInvocation currentInvocation = fragments[i];
          final int numberOfParens = printOpenParens(currentInvocation);
          List<DartExpression> arguments = currentInvocation.getArguments();
          if (currentInvocation instanceof DartMethodInvocation) {
            scribe.printNextToken(Token.IDENTIFIER); // selector
          }
          scribe.printNextToken(
              Token.LPAREN,
              preferences.insert_space_before_opening_paren_in_method_invocation);
          if (arguments != null) {
            if (preferences.insert_space_after_opening_paren_in_method_invocation) {
              scribe.space();
            }
            int argumentLength = arguments.size();
            int alignmentMode = preferences.alignment_for_arguments_in_method_invocation;
            Alignment argumentsAlignment = scribe.createAlignment(
                Alignment.MESSAGE_ARGUMENTS,
                alignmentMode,
                Alignment.R_OUTERMOST,
                argumentLength,
                scribe.scanner.currentPosition);
            scribe.enterAlignment(argumentsAlignment);
            boolean okForArguments = false;
            do {
              switch (alignmentMode & Alignment.SPLIT_MASK) {
                case Alignment.M_COMPACT_SPLIT:
                case Alignment.M_NEXT_PER_LINE_SPLIT:
                  argumentsAlignment.startingColumn = scribe.column;
                  break;
              }
              try {
                for (int j = 0; j < argumentLength; j++) {
                  if (j > 0) {
                    scribe.printNextToken(
                        Token.COMMA,
                        preferences.insert_space_before_comma_in_method_invocation_arguments);
                    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
                  }
                  scribe.alignFragment(argumentsAlignment, j);
                  if (j == 0) {
                    int fragmentIndentation = argumentsAlignment.fragmentIndentations[j];
                    if ((argumentsAlignment.mode & Alignment.M_INDENT_ON_COLUMN) != 0
                        && fragmentIndentation > 0) {
                      scribe.indentationLevel = fragmentIndentation;
                    }
                  } else if (preferences.insert_space_after_comma_in_method_invocation_arguments) {
                    scribe.space();
                  }
                  arguments.get(j).accept(this);
                  argumentsAlignment.startingColumn = -1;
                }
                okForArguments = true;
              } catch (AlignmentException e) {
                scribe.redoAlignment(e);
              }
            } while (!okForArguments);
            scribe.exitAlignment(argumentsAlignment, true);
            scribe.printNextToken(
                Token.RPAREN,
                preferences.insert_space_before_closing_paren_in_method_invocation);
          } else {
            scribe.printNextToken(
                Token.RPAREN,
                preferences.insert_space_between_empty_parens_in_method_invocation);
          }
          printCloseParens(currentInvocation, numberOfParens);
          cascadingInvocationAlignment.startingColumn = -1;
          if (i < size - 1) {
            scribe.alignFragment(cascadingInvocationAlignment, i);
            scribe.printNextToken(Token.PERIOD);
          }
        }
        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(cascadingInvocationAlignment, true);
  }

  private void formatCaseStatements(DartSwitchMember node) {
    scribe.printComment();
    List<DartStatement> statements = node.getStatements();
    if (!statements.isEmpty()) {
      int switchIndentationLevel = scribe.indentationLevel;
      int caseIndentation = 0;
      int statementIndentation = 0;
      if (preferences.indent_switchstatements_compare_to_switch) {
        caseIndentation++;
        statementIndentation++;
      }
      if (preferences.indent_switchstatements_compare_to_cases) {
        statementIndentation++;
      }
      scribe.setIndentation(switchIndentationLevel, caseIndentation);
      for (DartStatement statement : statements) {
        statement.accept(this);
      }
      scribe.setIndentation(switchIndentationLevel, statementIndentation);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.COMPLEX_TRAILING_COMMENT);
    }
  }

  private void formatClassBodyDeclarations(DartNode[] nodes) {
    final int FIELD = 1, METHOD = 2, TYPE = 3;
    scribe.lastNumberOfNewLines = 1;
    DartNode[] mergedNodes = nodes;// computeMergedMemberDeclarations(nodes);
    Alignment memberAlignment = scribe.createMemberAlignment(
        Alignment.TYPE_MEMBERS,
        preferences.align_type_members_on_columns ? Alignment.M_MULTICOLUMN
            : Alignment.M_NO_ALIGNMENT,
        4,
        scribe.scanner.currentPosition);
    scribe.enterMemberAlignment(memberAlignment);
    boolean isChunkStart = false;
    boolean ok = false;
    int startIndex = 0;
    do {
      try {
        for (int i = startIndex, max = mergedNodes.length; i < max; i++) {
          DartNode member = mergedNodes[i];
          if (member instanceof DartFieldDefinition) {
            isChunkStart = memberAlignment.checkChunkStart(FIELD, i, scribe.scanner.currentPosition);
            format((DartFieldDefinition) member, this, isChunkStart, i == 0);
          } else if (member instanceof DartMethodDefinition) {
            isChunkStart = memberAlignment.checkChunkStart(
                METHOD,
                i,
                scribe.scanner.currentPosition);
            format((DartMethodDefinition) member, isChunkStart, i == 0);
          }
          while (isNextToken(Token.SEMICOLON)) {
            scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
          }
          if (i != max - 1) {
            scribe.printNewLine();
          }
        }
        ok = true;
      } catch (AlignmentException e) {
        startIndex = memberAlignment.chunkStartIndex;
        scribe.redoMemberAlignment(e);
      }
    } while (!ok);
    scribe.exitMemberAlignment(memberAlignment);
    if (hasComments()) {
      scribe.printNewLine();
    }
    scribe.printComment();
  }

  private void formatEmptyTypeDeclaration(boolean isFirst) {
    // TODO remove, not legal in Dart
    boolean hasSemiColon = isNextToken(Token.SEMICOLON);
    while (isNextToken(Token.SEMICOLON)) {
      scribe.printComment();
      scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    }
    if (hasSemiColon && isFirst) {
      scribe.printNewLine();
    }
  }

  private void formatGuardClauseBlock(DartBlock block) {
    scribe.printNextToken(Token.LBRACE, preferences.insert_space_before_opening_brace_in_block);
    scribe.space();
    final List<DartStatement> statements = block.getStatements();
    statements.get(0).accept(this);
    scribe.printNextToken(Token.RBRACE, true);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
  }

  private void formatInitializers(List<? extends DartNode> arguments, boolean spaceBeforeComma,
      boolean spaceAfterComma, int methodDeclarationParametersAlignment) {

    int argumentLength = arguments.size();
    Alignment argumentsAlignment = scribe.createAlignment(
        Alignment.METHOD_ARGUMENTS,
        methodDeclarationParametersAlignment,
        argumentLength,
        scribe.scanner.currentPosition,
        preferences.continuation_indentation,
        true);
    scribe.enterAlignment(argumentsAlignment);
    boolean ok = false;
    do {
      switch (methodDeclarationParametersAlignment & Alignment.SPLIT_MASK) {
        case Alignment.M_COMPACT_SPLIT:
        case Alignment.M_NEXT_PER_LINE_SPLIT:
          argumentsAlignment.startingColumn = scribe.column;
          break;
      }
      try {
        for (int i = 0; i < argumentLength; i++) {
          if (i > 0) {
            scribe.printNextToken(Token.COMMA, spaceBeforeComma);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            if (scribe.lastNumberOfNewLines == 1) {
              // a new line has been inserted while printing the comment
              // hence we need to use the break indentation level before
              // printing next token...
              scribe.indentationLevel = argumentsAlignment.breakIndentationLevel;
            }
          }
          scribe.alignFragment(argumentsAlignment, i);
          if (i == 0) {
            int fragmentIndentation = argumentsAlignment.fragmentIndentations[0];
            if ((argumentsAlignment.mode & Alignment.M_INDENT_ON_COLUMN) != 0
                && fragmentIndentation > 0) {
              scribe.indentationLevel = fragmentIndentation;
            }
          } else if (spaceAfterComma) {
            scribe.space();
          }
          arguments.get(i).accept(this);
          argumentsAlignment.startingColumn = -1;
        }
        ok = true;
      } catch (AlignmentException e) {
        scribe.redoAlignment(e);
      }
    } while (!ok);
    scribe.exitAlignment(argumentsAlignment, true);
  }

  private void formatInvocation(DartInvocation invocation, Alignment messageAlignment) {

    if (invocation instanceof DartMethodInvocation) {
      if (messageAlignment != null) {
        if (!preferences.wrap_outer_expressions_when_nested || messageAlignment.canAlign()) {
          scribe.alignFragment(messageAlignment, 0);
        }
        scribe.printNextToken(Token.PERIOD);
      }
      scribe.printNextToken(Token.IDENTIFIER); // selector
    }
    scribe.printNextToken(
        Token.LPAREN,
        preferences.insert_space_before_opening_paren_in_method_invocation);

    final List<DartExpression> arguments = invocation.getArguments();
    if (arguments != null && !arguments.isEmpty()) {
      if (preferences.insert_space_after_opening_paren_in_method_invocation) {
        scribe.space();
      }
      int argumentsLength = arguments.size();
      if (argumentsLength > 1) {
        int alignmentMode = preferences.alignment_for_arguments_in_method_invocation;
        Alignment argumentsAlignment = scribe.createAlignment(
            Alignment.MESSAGE_ARGUMENTS,
            alignmentMode,
            argumentsLength,
            scribe.scanner.currentPosition);
        scribe.enterAlignment(argumentsAlignment);
        boolean ok = false;
        do {
          switch (alignmentMode & Alignment.SPLIT_MASK) {
            case Alignment.M_COMPACT_SPLIT:
            case Alignment.M_NEXT_PER_LINE_SPLIT:
              argumentsAlignment.startingColumn = scribe.column;
              break;
          }
          try {
            for (int i = 0; i < argumentsLength; i++) {
              if (i > 0) {
                scribe.printNextToken(
                    Token.COMMA,
                    preferences.insert_space_before_comma_in_method_invocation_arguments);
                scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
                if (scribe.lastNumberOfNewLines == 1) {
                  // a new line has been inserted while printing the comment
                  // hence we need to use the break indentation level before
                  // printing next token
                  scribe.indentationLevel = argumentsAlignment.breakIndentationLevel;
                }
              }
              scribe.alignFragment(argumentsAlignment, i);
              if (i > 0 && preferences.insert_space_after_comma_in_method_invocation_arguments) {
                scribe.space();
              }
              int fragmentIndentation = 0;
              if (i == 0) {
                int wrappedIndex = argumentsAlignment.wrappedIndex();
                if (wrappedIndex >= 0) {
                  fragmentIndentation = argumentsAlignment.fragmentIndentations[wrappedIndex];
                  if ((argumentsAlignment.mode & Alignment.M_INDENT_ON_COLUMN) != 0
                      && fragmentIndentation > 0) {
                    scribe.indentationLevel = fragmentIndentation;
                  }
                }
              }
              arguments.get(i).accept(this);
              argumentsAlignment.startingColumn = -1;
            }
            ok = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!ok);
        scribe.exitAlignment(argumentsAlignment, true);
      } else {
        arguments.get(0).accept(this);
      }
      scribe.printNextToken(
          Token.RPAREN,
          preferences.insert_space_before_closing_paren_in_method_invocation);
    } else {
      scribe.printNextToken(
          Token.RPAREN,
          preferences.insert_space_between_empty_parens_in_method_invocation);
    }
  }

  private void formatLabel(DartLabel label) {
    if (label == null) {
      return;
    }
    scribe.printNextToken(Token.IDENTIFIER);
    scribe.printNextToken(Token.COLON, preferences.insert_space_before_colon_in_labeled_statement);
    scribe.printComment();
    scribe.printNewLine();
  }

  private void formatLeftCurlyBrace(final int line, final String bracePosition) {
    // deal with (quite unexpected) comments right before LBRACE
    scribe.printComment(Scribe.PRESERVE_EMPTY_LINES_IN_FORMAT_LEFT_CURLY_BRACE);
    if (DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP.equals(bracePosition)
        && (scribe.line > line || scribe.column >= preferences.page_width)) {
      scribe.printNewLine();
    }
  }

  private void formatLocalDeclaration(DartVariableStatement localDeclaration,
      boolean insertSpaceBeforeComma, boolean insertSpaceAfterComma) {
    List<DartVariable> vars = localDeclaration.getVariables();
    if (localDeclaration.getTypeNode() != null) {
      localDeclaration.getTypeNode().accept(this);
    } else {
      scribe.printNextToken(Token.VAR, true);
    }
    boolean first = true;
    for (DartVariable var : vars) {
      if (!first) {
        scribe.printNextToken(Token.COMMA, insertSpaceBeforeComma);
        if (insertSpaceAfterComma) {
          scribe.space();
        }
        scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
      }
      scribe.printNextToken(Token.IDENTIFIER, first);
      int extraDimensions = getDimensions();
      if (extraDimensions != 0) {
        for (int index = 0; index < extraDimensions; index++) {
          scribe.printNextToken(Token.LBRACK);
          scribe.printNextToken(Token.RBRACK);
        }
      }
      final DartExpression initialization = var.getValue();
      if (initialization != null) {
        scribe.printNextToken(Token.ASSIGN, preferences.insert_space_before_assignment_operator);
        if (preferences.insert_space_after_assignment_operator) {
          scribe.space();
        }
        Alignment assignmentAlignment = scribe.createAlignment(
            Alignment.LOCAL_DECLARATION_ASSIGNMENT,
            preferences.alignment_for_assignment,
            Alignment.R_OUTERMOST,
            1,
            scribe.scanner.currentPosition);
        scribe.enterAlignment(assignmentAlignment);
        boolean ok = false;
        do {
          try {
            scribe.alignFragment(assignmentAlignment, 0);
            initialization.accept(this);
            ok = true;
          } catch (AlignmentException e) {
            scribe.redoAlignment(e);
          }
        } while (!ok);
        scribe.exitAlignment(assignmentAlignment, true);
      }
      first = false;
    }
  }

  private void formatMethodArguments(List<? extends DartNode> arguments,
      boolean spaceBeforeOpenParen, boolean spaceBetweenEmptyParameters,
      boolean spaceBeforeClosingParen, boolean spaceBeforeFirstParameter, boolean spaceBeforeComma,
      boolean spaceAfterComma, int methodDeclarationParametersAlignment) {

    scribe.printNextToken(Token.LPAREN, spaceBeforeOpenParen);
    if (arguments != null) {
      if (spaceBeforeFirstParameter) {
        scribe.space();
      }
      int argumentLength = arguments.size();
      Alignment argumentsAlignment = scribe.createAlignment(
          Alignment.METHOD_ARGUMENTS,
          methodDeclarationParametersAlignment,
          argumentLength,
          scribe.scanner.currentPosition);
      scribe.enterAlignment(argumentsAlignment);
      boolean ok = false;
      do {
        switch (methodDeclarationParametersAlignment & Alignment.SPLIT_MASK) {
          case Alignment.M_COMPACT_SPLIT:
          case Alignment.M_NEXT_PER_LINE_SPLIT:
            argumentsAlignment.startingColumn = scribe.column;
            break;
        }
        try {
          for (int i = 0; i < argumentLength; i++) {
            if (i > 0) {
              scribe.printNextToken(Token.COMMA, spaceBeforeComma);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
              if (scribe.lastNumberOfNewLines == 1) {
                // a new line has been inserted while printing the comment
                // hence we need to use the break indentation level before
                // printing next token...
                scribe.indentationLevel = argumentsAlignment.breakIndentationLevel;
              }
            }
            scribe.alignFragment(argumentsAlignment, i);
            if (i == 0) {
              int fragmentIndentation = argumentsAlignment.fragmentIndentations[0];
              if ((argumentsAlignment.mode & Alignment.M_INDENT_ON_COLUMN) != 0
                  && fragmentIndentation > 0) {
                scribe.indentationLevel = fragmentIndentation;
              }
            } else if (spaceAfterComma) {
              scribe.space();
            }
            arguments.get(i).accept(this);
            argumentsAlignment.startingColumn = -1;
          }
          ok = true;
        } catch (AlignmentException e) {
          scribe.redoAlignment(e);
        }
      } while (!ok);
      scribe.exitAlignment(argumentsAlignment, true);
      scribe.printNextToken(Token.RPAREN, spaceBeforeClosingParen);
    } else {
      scribe.printNextToken(Token.RPAREN, spaceBetweenEmptyParameters);
    }
  }

  private void formatNecessaryEmptyStatement() {
    if (preferences.put_empty_statement_on_new_line) {
      scribe.printNewLine();
      scribe.indent();
      scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
      scribe.unIndent();
    } else {
      scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    }
  }

  private void formatOpeningBrace(String bracePosition, boolean insertSpaceBeforeBrace) {
    formatOpeningGroup(Token.LBRACE, bracePosition, insertSpaceBeforeBrace);
  }

  private void formatOpeningBracket(String bracePosition, boolean insertSpaceBeforeBracket) {
    formatOpeningGroup(Token.LBRACK, bracePosition, insertSpaceBeforeBracket);
  }

  private void formatOpeningGroup(Token token, String bracePosition, boolean insertSpaceBeforeBrace) {
    if (DefaultCodeFormatterConstants.NEXT_LINE.equals(bracePosition)) {
      scribe.printNewLine();
    } else if (DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED.equals(bracePosition)) {
      scribe.printNewLine();
      scribe.indent();
    }
    scribe.printNextToken(
        token,
        insertSpaceBeforeBrace,
        Scribe.PRESERVE_EMPTY_LINES_IN_FORMAT_OPENING_BRACE);
    scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.UNMODIFIABLE_TRAILING_COMMENT);
  }

  private void formatStatements(final List<DartStatement> statements,
      boolean insertNewLineAfterLastStatement) {
    int statementsLength = statements.size();
    for (int i = 0; i < statementsLength; i++) {
      final DartStatement statement = statements.get(i);
      if (i > 0 && statements.get(i - 1) instanceof DartEmptyStatement
          && !(statement instanceof DartEmptyStatement)) {
        scribe.printNewLine();
      }
      statement.accept(this);
      if (statement instanceof DartVariableStatement) {
        DartVariableStatement currentLocal = (DartVariableStatement) statement;
        if (i < statementsLength - 1) {
          /*
           * Special handling for a series of local variable declarations.
           */
          if (statements.get(i + 1) instanceof DartVariableStatement) {
            DartVariableStatement nextLocal = (DartVariableStatement) statements.get(i + 1);
            if (currentLocal.getSourceInfo().getOffset() != nextLocal.getSourceInfo().getOffset()) {
              scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
              if (i != statementsLength - 1) {
                if (!(statement instanceof DartEmptyStatement)
                    && !(statements.get(i + 1) instanceof DartEmptyStatement)) {
                  scribe.printNewLine();
                }
              } else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
                scribe.printNewLine();
              }
            }
          } else {
            scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
            scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            if (i != statementsLength - 1) {
              if (!(statement instanceof DartEmptyStatement)
                  && !(statements.get(i + 1) instanceof DartEmptyStatement)) {
                scribe.printNewLine();
              }
            } else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
              scribe.printNewLine();
            }
          }
        } else {
          scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
          scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
          if (i != statementsLength - 1) {
            if (!(statement instanceof DartEmptyStatement)
                && !(statements.get(i + 1) instanceof DartEmptyStatement)) {
              scribe.printNewLine();
            }
          } else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
            scribe.printNewLine();
          }
        }
      } else if (i != statementsLength - 1) {
        if (!(statement instanceof DartEmptyStatement)
            && !(statements.get(i + 1) instanceof DartEmptyStatement)) {
          scribe.printNewLine();
        }
      } else if (i == statementsLength - 1 && insertNewLineAfterLastStatement) {
        scribe.printNewLine();
      }
    }
  }

  private void formatTypeMembers(DartClass typeDeclaration) {
    Alignment memberAlignment = scribe.createMemberAlignment(
        Alignment.TYPE_MEMBERS,
        preferences.align_type_members_on_columns ? Alignment.M_MULTICOLUMN
            : Alignment.M_NO_ALIGNMENT,
        3,
        scribe.scanner.currentPosition);
    scribe.enterMemberAlignment(memberAlignment);
    List<DartNode> members = typeDeclaration.getMembers();
    boolean isChunkStart = false;
    boolean ok = false;
    int membersLength = members.size();
    if (membersLength > 0) {
      int startIndex = 0;
      do {
        try {
          for (int i = startIndex, max = members.size(); i < max; i++) {
            while (isNextToken(Token.SEMICOLON)) {
              scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            }
            scribe.printNewLine();
            DartNode member = members.get(i);
            if (member instanceof DartFieldDefinition) {
              isChunkStart = memberAlignment.checkChunkStart(
                  Alignment.CHUNK_FIELD,
                  i,
                  scribe.scanner.currentPosition);
              DartFieldDefinition field = (DartFieldDefinition) member;
              format(field, this, isChunkStart, i == 0);
            } else if (member instanceof DartMethodDefinition) {
              isChunkStart = memberAlignment.checkChunkStart(
                  Alignment.CHUNK_METHOD,
                  i,
                  scribe.scanner.currentPosition);
              format((DartMethodDefinition) member, isChunkStart, i == 0);
            }
            while (isNextToken(Token.SEMICOLON)) {
              scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
              scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
            }
            scribe.printNewLine();
            // realign to the proper value
            if (scribe.memberAlignment != null) {
              // select the last alignment
              scribe.indentationLevel = scribe.memberAlignment.originalIndentationLevel;
            }
          }
          ok = true;
        } catch (AlignmentException e) {
          startIndex = memberAlignment.chunkStartIndex;
          scribe.redoMemberAlignment(e);
        }
      } while (!ok);
    } else if (isNextToken(Token.SEMICOLON)) {
      // the only body declaration is an empty declaration (';')
      scribe.printNextToken(Token.SEMICOLON, preferences.insert_space_before_semicolon);
      scribe.printComment(CodeFormatter.K_UNKNOWN, Scribe.BASIC_TRAILING_COMMENT);
    }
    scribe.printComment(Scribe.DO_NOT_PRESERVE_EMPTY_LINES);
    scribe.exitMemberAlignment(memberAlignment);
  }

  private void formatTypeOpeningBrace(String bracePosition, boolean insertSpaceBeforeBrace,
      DartClass typeDeclaration) {
    final int memberLength = typeDeclaration.getMembers() == null ? 0
        : typeDeclaration.getMembers().size();

    boolean insertNewLine = memberLength > 0;

    if (!insertNewLine) {
      insertNewLine = preferences.insert_new_line_in_empty_type_declaration;
    }

    formatOpeningBrace(bracePosition, insertSpaceBeforeBrace);

    if (insertNewLine) {
      scribe.printNewLine();
    }
  }

  private int getDimensions() {
    localScanner.resetTo(scribe.scanner.currentPosition, scribe.scannerEndPosition - 1);
    int dimensions = 0;
    int balance = 0;
    try {
      Token token;
      loop : while ((token = localScanner.getNextToken()) != Token.EOS) {
        switch (token) {
          case RBRACK:
            dimensions++;
            balance--;
            break;
          case COMMENT:
            break;
          case LBRACK:
            balance++;
            break;
          case WHITESPACE:
            break;
          default:
            break loop;
        }
      }
    } catch (InvalidInputException e) {
      // ignore
    }
    if (balance == 0) {
      return dimensions;
    }
    return 0;
  }

  private boolean hasComments() {
    localScanner.resetTo(scribe.scanner.startPosition, scribe.scannerEndPosition - 1);
    try {
      if (localScanner.peekNextToken() == Token.WHITESPACE) {
        localScanner.getNextToken();
      }
      return localScanner.getNextToken() == Token.COMMENT;
    } catch (InvalidInputException e) {
      return false;
    }
  }

  private boolean hasComplexInitializers(List<DartInitializer> inits) {
    for (DartInitializer init : inits) {
      DartExpression expr = init.getValue();
      if (!(expr instanceof DartIdentifier || expr instanceof DartLiteral)) {
        return true;
      }
    }
    return false;
  }

  private boolean isClosingGenericToken() {
    localScanner.resetTo(scribe.scanner.currentPosition, scribe.scannerEndPosition - 1);
    try {
      Token token = localScanner.getNextToken();
      loop : while (true) {
        switch (token) {
          case COMMENT:
          case WHITESPACE:
            token = localScanner.getNextToken();
            continue loop;
          default:
            break loop;
        }
      }
      switch (token) {
        case SAR:
        case GT:
          return true;
      }
    } catch (InvalidInputException e) {
      // ignore
    }
    return false;
  }

  private boolean isGuardClause(DartBlock block) {
    return !commentStartsBlock(block.getSourceInfo().getOffset(), block.getSourceInfo().getLength()
        + block.getSourceInfo().getOffset())
        && block.getStatements() != null
        && block.getStatements().size() == 1
        && (block.getStatements().get(0) instanceof DartReturnStatement || block.getStatements().get(
            0) instanceof DartThrowStatement);
  }

  private boolean isMultipleLocalDeclaration(DartVariableStatement localDeclaration) {
    if (localDeclaration.getSourceInfo().getOffset() == lastLocalDeclarationSourceStart) {
      return true;
    }
    lastLocalDeclarationSourceStart = localDeclaration.getSourceInfo().getOffset();
    return false;
  }

  private boolean isNextToken(Token tokenName) {
    localScanner.resetTo(scribe.scanner.currentPosition, scribe.scannerEndPosition - 1);
    try {
      Token token = localScanner.getNextToken();
      loop : while (true) {
        switch (token) {
          case COMMENT:
          case WHITESPACE:
            token = localScanner.getNextToken();
            continue loop;
          default:
            break loop;
        }
      }
      return token == tokenName;
    } catch (InvalidInputException e) {
      // ignore
    }
    return false;
  }

  private void manageClosingParenthesizedExpression(DartNode expression, int numberOfParens) {
    if (!(expression instanceof DartExpression || expression instanceof DartTypeNode)) {
      reportMissingNodeFormatter(expression.getClass(), expression);
      return;
    }
    for (int i = 0; i < numberOfParens; i++) {
      scribe.printNextToken(
          Token.RPAREN,
          preferences.insert_space_before_closing_paren_in_parenthesized_expression);
    }
  }

  private void manageOpeningParenthesizedExpression(DartNode expression, int numberOfParens) {
    if (!(expression instanceof DartExpression || expression instanceof DartTypeNode)) {
      reportMissingNodeFormatter(expression.getClass(), expression);
      return;
    }
    for (int i = 0; i < numberOfParens; i++) {
      scribe.printNextToken(
          Token.LPAREN,
          preferences.insert_space_before_opening_paren_in_parenthesized_expression);
      if (preferences.insert_space_after_opening_paren_in_parenthesized_expression) {
        scribe.space();
      }
    }
  }

  private void printCloseParens(DartNode node, int numberOfParens) {
    if (numberOfParens > 0) {
      manageClosingParenthesizedExpression(node, numberOfParens);
    }
  }

  private int printOpenParens(DartNode node) {
    final int numberOfParens = countParens(node);
    if (numberOfParens > 0) {
      manageOpeningParenthesizedExpression(node, numberOfParens);
    }
    return numberOfParens;
  }

  private DartNode reportMissingNodeFormatter(Class<? extends DartNode> cls, DartNode node) {
    System.out.println("MISSING formatter for " + cls.getSimpleName() + " to handle " + node);
    return null;
  }
}
