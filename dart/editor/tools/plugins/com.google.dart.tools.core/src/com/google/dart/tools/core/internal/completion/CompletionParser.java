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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartIfStatement;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartReturnStatement;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartSyntheticErrorIdentifier;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.parser.DartParser;
import com.google.dart.compiler.parser.DartScanner;
import com.google.dart.compiler.parser.DartScanner.Location;
import com.google.dart.compiler.parser.ParserContext;
import com.google.dart.compiler.parser.Token;
import com.google.dart.tools.core.internal.completion.ast.BlockCompleter;
import com.google.dart.tools.core.internal.completion.ast.ClassCompleter;
import com.google.dart.tools.core.internal.completion.ast.CompletionNode;
import com.google.dart.tools.core.internal.completion.ast.FieldCompleter;
import com.google.dart.tools.core.internal.completion.ast.FunctionCompleter;
import com.google.dart.tools.core.internal.completion.ast.IdentifierCompleter;
import com.google.dart.tools.core.internal.completion.ast.IfCompleter;
import com.google.dart.tools.core.internal.completion.ast.MethodInvocationCompleter;
import com.google.dart.tools.core.internal.completion.ast.NewExpressionCompleter;
import com.google.dart.tools.core.internal.completion.ast.ParameterCompleter;
import com.google.dart.tools.core.internal.completion.ast.PropertyAccessCompleter;
import com.google.dart.tools.core.internal.completion.ast.ReturnCompleter;
import com.google.dart.tools.core.internal.completion.ast.SuperConstructorInvocationCompleter;
import com.google.dart.tools.core.internal.completion.ast.TypeCompleter;
import com.google.dart.tools.core.internal.completion.ast.TypeParameterCompleter;
import com.google.dart.tools.core.internal.completion.ast.VariableStatementCompleter;

import static com.google.dart.tools.core.internal.completion.Mark.ArrayLiteral;
import static com.google.dart.tools.core.internal.completion.Mark.BinaryExpression;
import static com.google.dart.tools.core.internal.completion.Mark.Block;
import static com.google.dart.tools.core.internal.completion.Mark.BreakStatement;
import static com.google.dart.tools.core.internal.completion.Mark.CatchClause;
import static com.google.dart.tools.core.internal.completion.Mark.CatchParameter;
import static com.google.dart.tools.core.internal.completion.Mark.ClassBody;
import static com.google.dart.tools.core.internal.completion.Mark.ClassMember;
import static com.google.dart.tools.core.internal.completion.Mark.CompilationUnit;
import static com.google.dart.tools.core.internal.completion.Mark.ConditionalExpression;
import static com.google.dart.tools.core.internal.completion.Mark.ConstExpression;
import static com.google.dart.tools.core.internal.completion.Mark.ConstructorName;
import static com.google.dart.tools.core.internal.completion.Mark.ContinueStatement;
import static com.google.dart.tools.core.internal.completion.Mark.DoStatement;
import static com.google.dart.tools.core.internal.completion.Mark.EmptyStatement;
import static com.google.dart.tools.core.internal.completion.Mark.Expression;
import static com.google.dart.tools.core.internal.completion.Mark.ExpressionList;
import static com.google.dart.tools.core.internal.completion.Mark.ExpressionStatement;
import static com.google.dart.tools.core.internal.completion.Mark.FieldInitializerOrRedirectedConstructor;
import static com.google.dart.tools.core.internal.completion.Mark.FinalDeclaration;
import static com.google.dart.tools.core.internal.completion.Mark.ForInitialization;
import static com.google.dart.tools.core.internal.completion.Mark.ForStatement;
import static com.google.dart.tools.core.internal.completion.Mark.FormalParameter;
import static com.google.dart.tools.core.internal.completion.Mark.FunctionDeclaration;
import static com.google.dart.tools.core.internal.completion.Mark.FunctionLiteral;
import static com.google.dart.tools.core.internal.completion.Mark.FunctionStatementBody;
import static com.google.dart.tools.core.internal.completion.Mark.FunctionTypeInterface;
import static com.google.dart.tools.core.internal.completion.Mark.Identifier;
import static com.google.dart.tools.core.internal.completion.Mark.IfStatement;
import static com.google.dart.tools.core.internal.completion.Mark.Initializer;
import static com.google.dart.tools.core.internal.completion.Mark.Label;
import static com.google.dart.tools.core.internal.completion.Mark.Literal;
import static com.google.dart.tools.core.internal.completion.Mark.MapLiteral;
import static com.google.dart.tools.core.internal.completion.Mark.MapLiteralEntry;
import static com.google.dart.tools.core.internal.completion.Mark.MethodName;
import static com.google.dart.tools.core.internal.completion.Mark.Native;
import static com.google.dart.tools.core.internal.completion.Mark.NewExpression;
import static com.google.dart.tools.core.internal.completion.Mark.OperatorName;
import static com.google.dart.tools.core.internal.completion.Mark.Parameters;
import static com.google.dart.tools.core.internal.completion.Mark.ParenthesizedExpression;
import static com.google.dart.tools.core.internal.completion.Mark.PostfixExpression;
import static com.google.dart.tools.core.internal.completion.Mark.QualifiedIdentifier;
import static com.google.dart.tools.core.internal.completion.Mark.ReturnStatement;
import static com.google.dart.tools.core.internal.completion.Mark.SelectorExpression;
import static com.google.dart.tools.core.internal.completion.Mark.SpreadExpression;
import static com.google.dart.tools.core.internal.completion.Mark.StringInterpolation;
import static com.google.dart.tools.core.internal.completion.Mark.StringSegment;
import static com.google.dart.tools.core.internal.completion.Mark.SuperExpression;
import static com.google.dart.tools.core.internal.completion.Mark.SuperInitializer;
import static com.google.dart.tools.core.internal.completion.Mark.SwitchMember;
import static com.google.dart.tools.core.internal.completion.Mark.SwitchStatement;
import static com.google.dart.tools.core.internal.completion.Mark.ThisExpression;
import static com.google.dart.tools.core.internal.completion.Mark.ThrowStatement;
import static com.google.dart.tools.core.internal.completion.Mark.TopLevelElement;
import static com.google.dart.tools.core.internal.completion.Mark.TryStatement;
import static com.google.dart.tools.core.internal.completion.Mark.TypeAnnotation;
import static com.google.dart.tools.core.internal.completion.Mark.TypeArguments;
import static com.google.dart.tools.core.internal.completion.Mark.TypeExpression;
import static com.google.dart.tools.core.internal.completion.Mark.TypeFunctionOrVariable;
import static com.google.dart.tools.core.internal.completion.Mark.TypeParameter;
import static com.google.dart.tools.core.internal.completion.Mark.UnaryExpression;
import static com.google.dart.tools.core.internal.completion.Mark.VarDeclaration;
import static com.google.dart.tools.core.internal.completion.Mark.VariableDeclaration;
import static com.google.dart.tools.core.internal.completion.Mark.WhileStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class CompletionParser extends DartParser {
  private Stack<Mark> stack;
  private int completionPosition;
  private Token completionToken;

  public CompletionParser(ParserContext ctx) {
    super(ctx, false);
    stack = new Stack<Mark>();
  }

  public CompletionParser(ParserContext ctx, Set<String> prefixes) {
    super(ctx, false, prefixes);
    stack = new Stack<Mark>();
  }

  public void setCompletionPosition(int completionPosition) {
    this.completionPosition = completionPosition;
  }

  @Override
  protected void beginArrayLiteral() {
    super.beginArrayLiteral();
    pushMark(ArrayLiteral);
  }

  @Override
  protected void beginBinaryExpression() {
    super.beginBinaryExpression();
    pushMark(BinaryExpression);
  }

  @Override
  protected void beginBlock() {
    super.beginBlock();
    pushMark(Block);
  }

  @Override
  protected void beginBreakStatement() {
    super.beginBreakStatement();
    pushMark(BreakStatement);
  }

  @Override
  protected void beginCatchClause() {
    super.beginCatchClause();
    pushMark(CatchClause);
  }

  @Override
  protected void beginCatchParameter() {
    super.beginCatchParameter();
    pushMark(CatchParameter);
  }

  @Override
  protected void beginClassBody() {
    super.beginClassBody();
    pushMark(ClassBody);
  }

  @Override
  protected void beginClassMember() {
    super.beginClassMember();
    pushMark(ClassMember);
  }

  @Override
  protected void beginCompilationUnit() {
    super.beginCompilationUnit();
    pushMark(CompilationUnit);
  }

  @Override
  protected void beginConditionalExpression() {
    super.beginConditionalExpression();
    pushMark(ConditionalExpression);
  }

  @Override
  protected void beginConstExpression() {
    super.beginConstExpression();
    pushMark(ConstExpression);
  }

  @Override
  protected void beginConstructor() {
    super.beginConstructor();
    pushMark(ConstructorName);
  }

  @Override
  protected void beginContinueStatement() {
    super.beginContinueStatement();
    pushMark(ContinueStatement);
  }

  @Override
  protected void beginDoStatement() {
    super.beginDoStatement();
    pushMark(DoStatement);
  }

  @Override
  protected void beginEmptyStatement() {
    super.beginEmptyStatement();
    pushMark(EmptyStatement);
  }

  @Override
  protected void beginExpression() {
    super.beginExpression();
    pushMark(Expression);
  }

  @Override
  protected void beginExpressionList() {
    super.beginExpressionList();
    pushMark(ExpressionList);
  }

  @Override
  protected void beginExpressionStatement() {
    super.beginExpressionStatement();
    pushMark(ExpressionStatement);
  }

  @Override
  protected void beginFieldInitializerOrRedirectedConstructor() {
    super.beginFieldInitializerOrRedirectedConstructor();
    pushMark(FieldInitializerOrRedirectedConstructor);
  }

  @Override
  protected void beginFinalDeclaration() {
    super.beginFinalDeclaration();
    pushMark(FinalDeclaration);
  }

  @Override
  protected void beginForInitialization() {
    super.beginForInitialization();
    pushMark(ForInitialization);
  }

  @Override
  protected void beginFormalParameter() {
    super.beginFormalParameter();
    pushMark(FormalParameter);
  }

  @Override
  protected void beginFormalParameterList() {
    super.beginFormalParameterList();
    pushMark(Parameters);
  }

  @Override
  protected void beginForStatement() {
    super.beginForStatement();
    pushMark(ForStatement);
  }

  @Override
  protected void beginFunctionDeclaration() {
    super.beginFunctionDeclaration();
    pushMark(FunctionDeclaration);
  }

  @Override
  protected void beginFunctionLiteral() {
    super.beginFunctionLiteral();
    pushMark(FunctionLiteral);
  }

  @Override
  protected void beginFunctionStatementBody() {
    super.beginFunctionStatementBody();
    pushMark(FunctionStatementBody);
  }

  @Override
  protected void beginFunctionTypeInterface() {
    super.beginFunctionTypeInterface();
    pushMark(FunctionTypeInterface);
  }

  @Override
  protected void beginIdentifier() {
    super.beginIdentifier();
    pushMark(Identifier);
  }

  @Override
  protected void beginIfStatement() {
    super.beginIfStatement();
    pushMark(IfStatement);
  }

  @Override
  protected void beginInitializer() {
    super.beginInitializer();
    pushMark(Initializer);
  }

  @Override
  protected void beginLabel() {
    super.beginLabel();
    pushMark(Label);
  }

  @Override
  protected void beginLiteral() {
    super.beginLiteral();
    pushMark(Literal);
  }

  @Override
  protected void beginMapLiteral() {
    super.beginMapLiteral();
    pushMark(MapLiteral);
  }

  @Override
  protected void beginMapLiteralEntry() {
    super.beginMapLiteralEntry();
    pushMark(MapLiteralEntry);
  }

  @Override
  protected void beginMethodName() {
    super.beginMethodName();
    pushMark(MethodName);
  }

  @Override
  protected void beginNativeBody() {
    super.beginNativeBody();
    pushMark(Native);
  }

  @Override
  protected void beginNewExpression() {
    super.beginNewExpression();
    pushMark(NewExpression);
  }

  @Override
  protected void beginOperatorName() {
    super.beginOperatorName();
    pushMark(OperatorName);
  }

  @Override
  protected void beginParenthesizedExpression() {
    super.beginParenthesizedExpression();
    pushMark(ParenthesizedExpression);
  }

  @Override
  protected void beginPostfixExpression() {
    super.beginPostfixExpression();
    pushMark(PostfixExpression);
  }

  @Override
  protected void beginQualifiedIdentifier() {
    super.beginQualifiedIdentifier();
    pushMark(QualifiedIdentifier);
  }

  @Override
  protected void beginReturnStatement() {
    super.beginReturnStatement();
    pushMark(ReturnStatement);
  }

  @Override
  protected void beginSelectorExpression() {
    super.beginSelectorExpression();
    pushMark(SelectorExpression);
  }

  @Override
  protected void beginSpreadExpression() {
    super.beginSpreadExpression();
    pushMark(SpreadExpression);
  }

  @Override
  protected void beginStringInterpolation() {
    super.beginStringInterpolation();
    pushMark(StringInterpolation);
  }

  @Override
  protected void beginStringSegment() {
    super.beginStringSegment();
    pushMark(StringSegment);
  }

  @Override
  protected void beginSuperExpression() {
    super.beginSuperExpression();
    pushMark(SuperExpression);
  }

  @Override
  protected void beginSuperInitializer() {
    super.beginSuperInitializer();
    pushMark(SuperInitializer);
  }

  @Override
  protected void beginSwitchMember() {
    super.beginSwitchMember();
    pushMark(SwitchMember);
  }

  @Override
  protected void beginSwitchStatement() {
    super.beginSwitchStatement();
    pushMark(SwitchStatement);
  }

  @Override
  protected void beginThisExpression() {
    super.beginThisExpression();
    pushMark(ThisExpression);
  }

  @Override
  protected void beginThrowStatement() {
    super.beginThrowStatement();
    pushMark(ThrowStatement);
  }

  @Override
  protected void beginTopLevelElement() {
    super.beginTopLevelElement();
    pushMark(TopLevelElement);
  }

  @Override
  protected void beginTryStatement() {
    super.beginTryStatement();
    pushMark(TryStatement);
  }

  @Override
  protected void beginTypeAnnotation() {
    super.beginTypeAnnotation();
    pushMark(TypeAnnotation);
  }

  @Override
  protected void beginTypeArguments() {
    super.beginTypeArguments();
    pushMark(TypeArguments);
  }

  @Override
  protected void beginTypeExpression() {
    super.beginTypeExpression();
    pushMark(TypeExpression);
  }

  @Override
  protected void beginTypeFunctionOrVariable() {
    super.beginTypeFunctionOrVariable();
    pushMark(TypeFunctionOrVariable);
  }

  @Override
  protected void beginTypeParameter() {
    super.beginTypeParameter();
    pushMark(TypeParameter);
  }

  @Override
  protected void beginUnaryExpression() {
    super.beginUnaryExpression();
    pushMark(UnaryExpression);
  }

  @Override
  protected void beginVarDeclaration() {
    super.beginVarDeclaration();
    pushMark(VarDeclaration);
  }

  @Override
  protected void beginVariableDeclaration() {
    super.beginVariableDeclaration();
    pushMark(VariableDeclaration);
  }

  @Override
  protected void beginWhileStatement() {
    super.beginWhileStatement();
    pushMark(WhileStatement);
  }

  @Override
  protected <T> T done(T originalResult) {
    T original = super.done(originalResult);
    T result = convertNode(original);
    popMark();
    return result;
  }

  @Override
  protected <T> T doneWithoutConsuming(T originalResult) {
    T original = super.doneWithoutConsuming(originalResult);
    T result = convertNode(original);
    return result;
  }

  @Override
  protected boolean expect(Token token) {
    Mark tos = peekMark();
    Token tok = completionToken;
    boolean found = super.expect(token);
    if (!found) {
      if (tos == Identifier && token == Token.IDENTIFIER && tok == Token.PERIOD
          && tok == completionToken) {
        // this causes a bogus DartIdentifier to be created
        // but it get immediately replaced by an IndentifierCompleter
        // which is used to form another completer node
        return true;
      }
    }
    return found;
  }

  @Override
  protected Token next() {
    Token next = super.next();
    Location loc = ctx.getTokenLocation();
    int start = loc.getBegin().getPos();
    int end = loc.getEnd().getPos();
    if (start <= completionPosition && completionPosition <= end) {
      completionToken = next;
    } else {
      completionToken = null;
    }
    return next;
  }

  @Override
  protected void reportError(DartScanner.Position position, ErrorCode errorCode,
      Object... arguments) {
    // TODO completion analysis
    peekMark();
    if (completionToken != null) {
      Location loc = ctx.getTokenLocation();
      int start = loc.getBegin().getPos();
      int end = loc.getEnd().getPos();
    }
  }

  @Override
  protected void reportUnexpectedToken(DartScanner.Position position, Token expected, Token actual) {
    super.reportUnexpectedToken(position, expected, actual);
  }

  @Override
  protected void rollback() {
    popMark();
    super.rollback();
  }

  @SuppressWarnings("unchecked")
  private <T> T convertNode(T originalResult) {
    if (originalResult == null) {
      return originalResult;
    }
    if (originalResult instanceof List) {
      List<T> result = new ArrayList<T>();
      for (T element : (List<T>) originalResult) {
        result.add(convertNode(element));
      }
      return (T) result;
    }
    T result = originalResult;
    if (completionToken != null) {
      result = (T) makeCompletionNode((DartNode) result);
    } else if (result instanceof DartNode) {
      DartNode node = (DartNode) result;
      int start = node.getSourceInfo().getOffset();
      int end = start + node.getSourceInfo().getLength();
      if (start <= completionPosition && completionPosition <= end) {
        result = (T) makeCompletionNode(node);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private DartNode makeCompletionNode(DartNode node) {
    Class<? extends DartNode> nodeClass = node.getClass();
    CompletionNode newNode;
    if (nodeClass == DartMethodInvocation.class) {
      newNode = MethodInvocationCompleter.from((DartMethodInvocation) node);
    } else if (nodeClass == DartIdentifier.class || nodeClass == DartSyntheticErrorIdentifier.class) {
      newNode = IdentifierCompleter.from((DartIdentifier) node);
    } else if (nodeClass == DartPropertyAccess.class) {
      newNode = PropertyAccessCompleter.from((DartPropertyAccess) node);
    } else if (nodeClass == DartNewExpression.class) {
      newNode = NewExpressionCompleter.from((DartNewExpression) node);
    } else if (nodeClass == DartBlock.class) {
      newNode = BlockCompleter.from((DartBlock) node);
    } else if (nodeClass == DartTypeNode.class) {
      newNode = TypeCompleter.from((DartTypeNode) node);
    } else if (nodeClass == DartParameter.class) {
      newNode = ParameterCompleter.from((DartParameter) node);
    } else if (nodeClass == DartFunction.class) {
      newNode = FunctionCompleter.from((DartFunction) node);
    } else if (nodeClass == DartIfStatement.class) {
      newNode = IfCompleter.from((DartIfStatement) node);
    } else if (nodeClass == DartReturnStatement.class) {
      newNode = ReturnCompleter.from((DartReturnStatement) node);
    } else if (nodeClass == DartClass.class) {
      newNode = ClassCompleter.from((DartClass) node);
    } else if (nodeClass == DartSuperConstructorInvocation.class) {
      newNode = SuperConstructorInvocationCompleter.from((DartSuperConstructorInvocation) node);
    } else if (nodeClass == DartVariableStatement.class) {
      newNode = VariableStatementCompleter.from((DartVariableStatement) node);
    } else if (nodeClass == DartField.class) {
      newNode = FieldCompleter.from((DartField) node);
    } else if (nodeClass == DartTypeParameter.class) {
      newNode = TypeParameterCompleter.from((DartTypeParameter) node);
    } else {
      return node;
    }
    newNode.setCompletionParsingContext((Stack<Mark>) stack.clone());
    return (DartNode) newNode;
  }

  private Mark peekMark() {
    if (stack.isEmpty()) {
      return null;
    }
    return stack.peek();
  }

  private Mark popMark() {
    if (stack.isEmpty()) {
//      System.out.println("pop  <empty>");
      return null;
    }
//    System.out.println("pop  " + stack.peek());
    return stack.pop();
  }

  private void pushMark(Mark mark) {
//    System.out.println("push " + mark);
    stack.push(mark);
  }
}
