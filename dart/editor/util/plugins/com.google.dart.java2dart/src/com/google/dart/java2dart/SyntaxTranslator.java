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

package com.google.dart.java2dart;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.EmptyStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.util.ExecutionUtils;
import com.google.dart.java2dart.util.RunnableEx;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Translates Java AST to Dart AST.
 */
public class SyntaxTranslator extends org.eclipse.jdt.core.dom.ASTVisitor {
  /**
   * Translates given Java AST into Dart AST.
   */
  public static CompilationUnit translate(Context context,
      org.eclipse.jdt.core.dom.CompilationUnit javaUnit) {
    SyntaxTranslator translator = new SyntaxTranslator(context);
    javaUnit.accept(translator);
    return (CompilationUnit) translator.result;
  }

  /**
   * @return the {@link Method} of {@link SyntaxTranslator} to translate
   *         {@link org.eclipse.jdt.core.dom.ASTNode} of the given class.
   */
  private static Method getMostSpecificMethod(Class<?> argumentType) throws Exception {
    Method resultMethod = null;
    for (Method method : SyntaxTranslator.class.getMethods()) {
      if (method.getName().equals("visit")) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 1 && parameterTypes[0] == argumentType) {
          resultMethod = method;
          break;
        }
      }
    }
    Assert.isNotNull(resultMethod);
    return resultMethod;
  }

  private static TypeName newListType(TypeName elementType, int dimensions) {
    TypeName listType = elementType;
    for (int i = 0; i < dimensions; i++) {
      TypeArgumentList typeArguments = new TypeArgumentList(
          null,
          Lists.newArrayList(listType),
          null);
      listType = new TypeName(newSimpleIdentifier("List"), typeArguments);
    }
    return listType;
  }

  private static SimpleIdentifier newSimpleIdentifier(String name) {
    return new SimpleIdentifier(new StringToken(TokenType.IDENTIFIER, name, 0));
  }

  private final Context context;

  private ASTNode result;

  private SyntaxTranslator(Context context) {
    this.context = context;
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayAccess node) {
    Expression expression = translate(node.getArray());
    Expression index = translate(node.getIndex());
    return done(new IndexExpression(expression, null, index, null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayCreation node) {
    TypeName listType = translate(node.getType());
    TypeArgumentList typeArgs = listType.getTypeArguments();
    if (node.getInitializer() != null) {
      List<Expression> elements = translateExpressionList(node.getInitializer().expressions());
      return done(new ListLiteral(null, typeArgs, null, elements, null));
    } else {
      ConstructorName constructorName = new ConstructorName(
          (TypeName) translate(node.getType()),
          null,
          newSimpleIdentifier("fixedLength"));
      ArgumentList arguments = translateArgumentList(node.dimensions());
      return done(new InstanceCreationExpression(
          new KeywordToken(Keyword.NEW, 0),
          constructorName,
          arguments));
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayInitializer node) {
    List<Expression> elements = translateExpressionList(node.expressions());
    return done(new ListLiteral(null, null, null, elements, null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ArrayType node) {
    TypeName elementType = translate(node.getElementType());
    int dimensions = node.getDimensions();
    return done(newListType(elementType, dimensions));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.AssertStatement node) {
    return done(new AssertStatement(
        null,
        null,
        (Expression) translate(node.getExpression()),
        null,
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Assignment node) {
    Expression left = translate(node.getLeftHandSide());
    Expression right = translate(node.getRightHandSide());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.Assignment.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.ASSIGN) {
      tokenType = TokenType.EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.PLUS_ASSIGN) {
      tokenType = TokenType.PLUS_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.MINUS_ASSIGN) {
      tokenType = TokenType.MINUS_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.TIMES_ASSIGN) {
      tokenType = TokenType.STAR_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.DIVIDE_ASSIGN) {
      tokenType = TokenType.SLASH_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.REMAINDER_ASSIGN) {
      tokenType = TokenType.PERCENT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.LEFT_SHIFT_ASSIGN) {
      tokenType = TokenType.LT_LT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN
        || javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
      tokenType = TokenType.GT_GT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.BIT_XOR_ASSIGN) {
      tokenType = TokenType.CARET_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.BIT_OR_ASSIGN) {
      tokenType = TokenType.BAR_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.Assignment.Operator.BIT_AND_ASSIGN) {
      tokenType = TokenType.AMPERSAND_EQ;
    }
    Assert.isNotNull(tokenType, "No token for: " + javaOperator);
    // done
    return done(new BinaryExpression(left, new Token(tokenType, 0), right));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Block node) {
    List<Statement> statements = Lists.newArrayList();
    for (Iterator<?> I = node.statements().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Statement javaStatement = (org.eclipse.jdt.core.dom.Statement) I.next();
      if (javaStatement instanceof org.eclipse.jdt.core.dom.SuperConstructorInvocation) {
        continue;
      }
      if (javaStatement instanceof org.eclipse.jdt.core.dom.ConstructorInvocation) {
        continue;
      }
      statements.add((Statement) translate(javaStatement));
    }
    return done(new Block(null, statements, null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.BooleanLiteral node) {
    boolean value = node.booleanValue();
    Token token = value ? new KeywordToken(Keyword.TRUE, 0) : new KeywordToken(Keyword.FALSE, 0);
    return done(new BooleanLiteral(token, value));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.BreakStatement node) {
    return done(new BreakStatement(null, (SimpleIdentifier) translate(node.getLabel()), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CastExpression node) {
    Expression expression = translate(node.getExpression());
    return done(expression);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CatchClause node) {
    return done(new CatchClause(
        null,
        (TypeName) translate(node.getException().getType()),
        new StringToken(TokenType.IDENTIFIER, "catch", 0),
        null,
        translateSimpleName(node.getException().getName()),
        null,
        null,
        null,
        (Block) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CharacterLiteral node) {
    int intValue = node.charValue();
    String hexString = "0x" + Integer.toHexString(intValue);
    return done(new IntegerLiteral(new StringToken(TokenType.INT, hexString, 0), 0));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ClassInstanceCreation node) {
    return done(new InstanceCreationExpression(
        new KeywordToken(Keyword.NEW, 0),
        new ConstructorName((TypeName) translate(node.getType()), null, null),
        translateArgumentList(node.arguments())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.CompilationUnit node) {
    List<Directive> directives = Lists.newArrayList();
    List<CompilationUnitMember> declarations = Lists.newArrayList();
    for (Iterator<?> I = node.types().iterator(); I.hasNext();) {
      Object javaType = I.next();
      ClassDeclaration dartClass = translate((org.eclipse.jdt.core.dom.ASTNode) javaType);
      declarations.add(dartClass);
    }
    return done(new CompilationUnit(null, null, directives, declarations, null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ConditionalExpression node) {
    return done(new ConditionalExpression(
        (Expression) translate(node.getExpression()),
        null,
        (Expression) translate(node.getThenExpression()),
        null,
        (Expression) translate(node.getElseExpression())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ConstructorInvocation node) {
    ArgumentList argumentList = translateArgumentList(node.arguments());
    return done(new RedirectingConstructorInvocation(null, null, null, argumentList));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ContinueStatement node) {
    return done(new ContinueStatement(null, (SimpleIdentifier) translate(node.getLabel()), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.DoStatement node) {
    return done(new DoStatement(
        null,
        (Statement) translate(node.getBody()),
        null,
        null,
        (Expression) translate(node.getExpression()),
        null,
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EmptyStatement node) {
    return done(new EmptyStatement(null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EnhancedForStatement node) {
    return done(new ForEachStatement(
        null,
        null,
        (SimpleFormalParameter) translate(node.getParameter()),
        null,
        (Expression) translate(node.getExpression()),
        null,
        (Statement) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EnumConstantDeclaration node) {
    // prepare enum name
    org.eclipse.jdt.core.dom.SimpleName enumTypeName;
    {
      org.eclipse.jdt.core.dom.EnumDeclaration parentEnum = (org.eclipse.jdt.core.dom.EnumDeclaration) node.getParent();
      enumTypeName = parentEnum.getName();
    }
    // prepare field type
    TypeName type = new TypeName(translateSimpleName(enumTypeName), null);
    // prepare field variables
    List<VariableDeclaration> variables = Lists.newArrayList();
    {
      Expression init = new InstanceCreationExpression(
          new KeywordToken(Keyword.NEW, 0),
          new ConstructorName(new TypeName(translateSimpleName(enumTypeName), null), null, null),
          translateArgumentList(node.arguments()));
      variables.add(new VariableDeclaration(
          null,
          null,
          translateSimpleName(node.getName()),
          null,
          init));
    }
    Token tokenStatic = new KeywordToken(Keyword.STATIC, 0);
    Token tokenFinal = new KeywordToken(Keyword.FINAL, 0);
    return done(new FieldDeclaration(
        translateJavadoc(node),
        null,
        tokenStatic,
        new VariableDeclarationList(tokenFinal, type, variables),
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.EnumDeclaration node) {
    SimpleIdentifier name = translateSimpleName(node.getName());
    // implements
    ImplementsClause implementsClause = null;
    if (!node.superInterfaceTypes().isEmpty()) {
      List<TypeName> interfaces = Lists.newArrayList();
      for (Object javaInterface : node.superInterfaceTypes()) {
        interfaces.add((TypeName) translate((org.eclipse.jdt.core.dom.ASTNode) javaInterface));
      }
      implementsClause = new ImplementsClause(null, interfaces);
    }
    // members
    List<ClassMember> members = Lists.newArrayList();
    {
      // constants
      for (Object javaConst : node.enumConstants()) {
        members.add((FieldDeclaration) translate((org.eclipse.jdt.core.dom.EnumConstantDeclaration) javaConst));
      }
      // body declarations
      for (Iterator<?> I = node.bodyDeclarations().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.BodyDeclaration javaBodyDecl = (org.eclipse.jdt.core.dom.BodyDeclaration) I.next();
        ClassMember member = translate(javaBodyDecl);
        members.add(member);
      }
    }
    return done(new ClassDeclaration(
        translateJavadoc(node),
        null,
        null,
        null,
        name,
        null,
        null,
        null,
        implementsClause,
        null,
        members,
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ExpressionStatement node) {
    Expression expression = translate(node.getExpression());
    return done(new ExpressionStatement(expression, null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.FieldAccess node) {
    Token operator = new Token(TokenType.PERIOD, 0);
    return done(new PropertyAccess(
        (Expression) translate(node.getExpression()),
        operator,
        (SimpleIdentifier) translate(node.getName())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.FieldDeclaration node) {
    boolean isFinal = org.eclipse.jdt.core.dom.Modifier.isFinal(node.getModifiers());
    FieldDeclaration fieldDeclaration = new FieldDeclaration(
        translateJavadoc(node),
        null,
        null,
        translateVariableDeclarationList(isFinal, node.getType(), node.fragments()),
        null);
    if (org.eclipse.jdt.core.dom.Modifier.isStatic(node.getModifiers())) {
      fieldDeclaration.setKeyword(new KeywordToken(Keyword.STATIC, 0));
    }
    return done(fieldDeclaration);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ForStatement node) {
    Object javaInitializer = node.initializers().get(0);
    VariableDeclarationList variableList = null;
    Expression initializer = null;
    if (javaInitializer instanceof org.eclipse.jdt.core.dom.VariableDeclarationExpression) {
      org.eclipse.jdt.core.dom.VariableDeclarationExpression javaVDE = (org.eclipse.jdt.core.dom.VariableDeclarationExpression) javaInitializer;
      List<VariableDeclaration> variables = Lists.newArrayList();
      for (Iterator<?> I = javaVDE.fragments().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.VariableDeclarationFragment fragment = (org.eclipse.jdt.core.dom.VariableDeclarationFragment) I.next();
        variables.add((VariableDeclaration) translate(fragment));
      }
      variableList = new VariableDeclarationList(
          null,
          (TypeName) translate(javaVDE.getType()),
          variables);
    } else {
      initializer = translate((org.eclipse.jdt.core.dom.ASTNode) javaInitializer);
    }
    return done(new ForStatement(
        null,
        null,
        variableList,
        initializer,
        null,
        (Expression) translate(node.getExpression()),
        null,
        translateExpressionList(node.updaters()),
        null,
        (Statement) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.IfStatement node) {
    return done(new IfStatement(
        null,
        null,
        (Expression) translate(node.getExpression()),
        null,
        (Statement) translate(node.getThenStatement()),
        null,
        (Statement) translate(node.getElseStatement())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.InfixExpression node) {
    Expression left = translate(node.getLeftOperand());
    Expression right = translate(node.getRightOperand());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.InfixExpression.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.PLUS) {
      tokenType = TokenType.PLUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.MINUS) {
      tokenType = TokenType.MINUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.TIMES) {
      tokenType = TokenType.STAR;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.DIVIDE) {
      tokenType = TokenType.SLASH;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.REMAINDER) {
      tokenType = TokenType.PERCENT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.LEFT_SHIFT) {
      tokenType = TokenType.LT_LT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.RIGHT_SHIFT_SIGNED
        || javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
      tokenType = TokenType.GT_GT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_OR) {
      tokenType = TokenType.BAR_BAR;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.CONDITIONAL_AND) {
      tokenType = TokenType.AMPERSAND_AMPERSAND;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.XOR) {
      tokenType = TokenType.CARET;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.OR) {
      tokenType = TokenType.BAR;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.AND) {
      tokenType = TokenType.AMPERSAND;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS) {
      tokenType = TokenType.LT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER) {
      tokenType = TokenType.GT;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS_EQUALS) {
      tokenType = TokenType.LT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER_EQUALS) {
      tokenType = TokenType.GT_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.EQUALS) {
      tokenType = TokenType.EQ_EQ;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.InfixExpression.Operator.NOT_EQUALS) {
      tokenType = TokenType.BANG_EQ;
    }
    Assert.isNotNull(tokenType, "No token for: " + javaOperator);
    // create BinaryExpression
    BinaryExpression binary = new BinaryExpression(left, new Token(tokenType, 0), right);
    for (Object javaOperand : node.extendedOperands()) {
      Expression operand = translate((org.eclipse.jdt.core.dom.ASTNode) javaOperand);
      binary = new BinaryExpression(binary, new Token(tokenType, 0), operand);
    }
    return done(binary);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.InstanceofExpression node) {
    return done(new IsExpression(
        (Expression) translate(node.getLeftOperand()),
        null,
        null,
        (TypeName) translate(node.getRightOperand())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.Javadoc node) {
    StringBuilder buffer = new StringBuilder();
    {
      buffer.append("/**");
      for (Object javaTag : node.tags()) {
        buffer.append(javaTag.toString());
      }
      buffer.append("\n */\n");
    }
    StringToken commentToken = new StringToken(TokenType.STRING, buffer.toString(), 0);
    return done(Comment.createDocumentationComment(new Token[] {commentToken}));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.LabeledStatement node) {
    List<Label> labels = Lists.newArrayList();
    while (true) {
      labels.add(new Label((SimpleIdentifier) translate(node.getLabel()), null));
      if (node.getBody() instanceof org.eclipse.jdt.core.dom.LabeledStatement) {
        node = (org.eclipse.jdt.core.dom.LabeledStatement) node.getBody();
      } else {
        break;
      }
    }
    return done(new LabeledStatement(labels, (Statement) translate(node.getBody())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.MethodDeclaration node) {
    // parameters
    FormalParameterList parameterList;
    {
      List<FormalParameter> parameters = Lists.newArrayList();
      for (Iterator<?> I = node.parameters().iterator(); I.hasNext();) {
        org.eclipse.jdt.core.dom.SingleVariableDeclaration javaParameter = (org.eclipse.jdt.core.dom.SingleVariableDeclaration) I.next();
        SimpleFormalParameter parameter = translate(javaParameter);
        parameters.add(parameter);
      }
      parameterList = new FormalParameterList(null, parameters, null, null, null);
    }
    // done
    FunctionBody body;
    SuperConstructorInvocation superConstructorInvocation = null;
    RedirectingConstructorInvocation redirectingConstructorInvocation = null;
    {
      org.eclipse.jdt.core.dom.Block javaBlock = node.getBody();
      if (javaBlock != null) {
        for (Object javaStatement : javaBlock.statements()) {
          if (javaStatement instanceof org.eclipse.jdt.core.dom.SuperConstructorInvocation) {
            superConstructorInvocation = translate((org.eclipse.jdt.core.dom.SuperConstructorInvocation) javaStatement);
          }
          if (javaStatement instanceof org.eclipse.jdt.core.dom.ConstructorInvocation) {
            redirectingConstructorInvocation = translate((org.eclipse.jdt.core.dom.ConstructorInvocation) javaStatement);
          }
        }
        Block bodyBlock = (Block) translate(javaBlock);
        if (redirectingConstructorInvocation != null) {
          Assert.isLegal(
              bodyBlock.getStatements().isEmpty(),
              "Cannot translate redirecting constructor with body.");
          body = new EmptyFunctionBody(null);
        } else {
          body = new BlockFunctionBody(bodyBlock);
        }
      } else {
        body = new EmptyFunctionBody(null);
      }
    }
    if (node.isConstructor()) {
      List<ConstructorInitializer> initializers = Lists.newArrayList();
      if (superConstructorInvocation != null) {
        initializers.add(superConstructorInvocation);
      }
      if (redirectingConstructorInvocation != null) {
        initializers.add(redirectingConstructorInvocation);
      }
      return done(new ConstructorDeclaration(
          translateJavadoc(node),
          null,
          null,
          null,
          null,
          translateSimpleName(node.getName()),
          null,
          null,
          parameterList,
          null,
          initializers,
          null,
          body));
    } else {
      Token modifierKeyword = org.eclipse.jdt.core.dom.Modifier.isStatic(node.getModifiers())
          ? new KeywordToken(Keyword.STATIC, 0) : null;
      return done(new MethodDeclaration(
          translateJavadoc(node),
          null,
          null,
          modifierKeyword,
          (TypeName) translate(node.getReturnType2()),
          null,
          null,
          translateSimpleName(node.getName()),
          parameterList,
          body));
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.MethodInvocation node) {
    Expression target = (Expression) translate(node.getExpression());
    ArgumentList argumentList = translateArgumentList(node.arguments());
    SimpleIdentifier name = translateSimpleName(node.getName());
    return done(new MethodInvocation(target, null, name, argumentList));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.NullLiteral node) {
    return done(new NullLiteral(null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.NumberLiteral node) {
    String token = node.getToken();
    if (token.contains(".")
        || !StringUtils.startsWithIgnoreCase(token, "0x")
        && (StringUtils.endsWithIgnoreCase(token, "F") || StringUtils.endsWithIgnoreCase(token, "D"))) {
      token = StringUtils.removeEndIgnoreCase(token, "F");
      token = StringUtils.removeEndIgnoreCase(token, "D");
      if (!token.contains(".")) {
        token += ".0";
      }
      DoubleLiteral literal = new DoubleLiteral(new StringToken(TokenType.DOUBLE, token, 0), 0);
      return done(literal);
    } else {
      token = StringUtils.removeEndIgnoreCase(token, "L");
      IntegerLiteral literal = new IntegerLiteral(new StringToken(TokenType.INT, token, 0), 0);
      return done(literal);
    }
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ParameterizedType node) {
    return done(new TypeName(
        ((TypeName) translate(node.getType())).getName(),
        translateTypeArgumentList(node.typeArguments())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ParenthesizedExpression node) {
    Expression expression = translate(node.getExpression());
    return done(new ParenthesizedExpression(null, expression, null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PostfixExpression node) {
    Expression operand = translate(node.getOperand());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.PostfixExpression.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.PostfixExpression.Operator.INCREMENT) {
      tokenType = TokenType.PLUS_PLUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PostfixExpression.Operator.DECREMENT) {
      tokenType = TokenType.MINUS_MINUS;
    }
    // done
    return done(new PostfixExpression(operand, new Token(tokenType, 0)));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PrefixExpression node) {
    Expression operand = translate(node.getOperand());
    // operator
    TokenType tokenType = null;
    org.eclipse.jdt.core.dom.PrefixExpression.Operator javaOperator = node.getOperator();
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.PLUS) {
      return done(operand);
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.INCREMENT) {
      tokenType = TokenType.PLUS_PLUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.DECREMENT) {
      tokenType = TokenType.MINUS_MINUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.MINUS) {
      tokenType = TokenType.MINUS;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.NOT) {
      tokenType = TokenType.BANG;
    }
    if (javaOperator == org.eclipse.jdt.core.dom.PrefixExpression.Operator.COMPLEMENT) {
      tokenType = TokenType.TILDE;
    }
    Assert.isNotNull(tokenType, "No token for: " + javaOperator);
    // done
    return done(new PrefixExpression(new Token(tokenType, 0), operand));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.PrimitiveType node) {
    String name = node.toString();
    if ("boolean".equals(name)) {
      name = "bool";
    }
    if ("byte".equals(name) || "char".equals(name) || "short".equals(name) || "long".equals(name)) {
      name = "int";
    }
    if ("float".equals(name)) {
      name = "double";
    }
    TypeName type = new TypeName(newSimpleIdentifier(name), null);
    return done(type);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.QualifiedName node) {
    Token operator = new Token(TokenType.PERIOD, 0);
    return done(new PropertyAccess(
        (Expression) translate(node.getQualifier()),
        operator,
        (SimpleIdentifier) translate(node.getName())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ReturnStatement node) {
    return done(new ReturnStatement(null, (Expression) translate(node.getExpression()), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SimpleName node) {
    SimpleIdentifier result = new SimpleIdentifier(new StringToken(
        TokenType.IDENTIFIER,
        node.getIdentifier(),
        0));
    context.putReference(node.resolveBinding(), result);
    return done(result);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SimpleType node) {
    return done(new TypeName(
        translateSimpleName((org.eclipse.jdt.core.dom.SimpleName) node.getName()),
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SingleVariableDeclaration node) {
    TypeName type = (TypeName) translate(node.getType());
    type = newListType(type, node.getExtraDimensions());
    return done(new SimpleFormalParameter(
        null,
        null,
        null,
        type,
        translateSimpleName(node.getName())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.StringLiteral node) {
    String tokenValue = node.getEscapedValue();
    tokenValue = StringUtils.replace(tokenValue, "$", "\\$");
    return done(new SimpleStringLiteral(
        new StringToken(TokenType.STRING, tokenValue, 0),
        node.getLiteralValue()));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SuperConstructorInvocation node) {
    ArgumentList argumentList = translateArgumentList(node.arguments());
    return done(new SuperConstructorInvocation(null, null, null, argumentList));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SwitchStatement node) {
    List<SwitchMember> members = Lists.newArrayList();
    {
      SwitchMember switchCase = null;
      for (Iterator<?> I = node.statements().iterator(); I.hasNext();) {
        Object javaMember = I.next();
        if (javaMember instanceof org.eclipse.jdt.core.dom.SwitchCase) {
          org.eclipse.jdt.core.dom.SwitchCase javaCase = (org.eclipse.jdt.core.dom.SwitchCase) javaMember;
          Expression switchExpr = translate(javaCase.getExpression());
          if (switchExpr != null) {
            switchCase = new SwitchCase(null, null, switchExpr, null, null);
          } else {
            switchCase = new SwitchDefault(null, null, null, null);
          }
          members.add(switchCase);
        } else {
          Assert.isTrue(switchCase != null);
          switchCase.getStatements().add(
              (Statement) translate((org.eclipse.jdt.core.dom.Statement) javaMember));
        }
      }
    }
    return done(new SwitchStatement(
        null,
        null,
        (Expression) translate(node.getExpression()),
        null,
        null,
        members,
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.SynchronizedStatement node) {
    return visit(node.getBody());
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ThisExpression node) {
    return done(new ThisExpression(new KeywordToken(Keyword.THIS, 0)));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.ThrowStatement node) {
    return done(new ExpressionStatement(new ThrowExpression(
        null,
        (Expression) translate(node.getExpression())), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TryStatement node) {
    List<CatchClause> catchClauses = Lists.newArrayList();
    for (Iterator<?> I = node.catchClauses().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.CatchClause javaCatch = (org.eclipse.jdt.core.dom.CatchClause) I.next();
      catchClauses.add((CatchClause) translate(javaCatch));
    }
    return done(new TryStatement(
        null,
        (Block) translate(node.getBody()),
        catchClauses,
        null,
        (Block) translate(node.getFinally())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeDeclaration node) {
    SimpleIdentifier name = translateSimpleName(node.getName());
    // interface
    KeywordToken abstractToken = null;
    if (node.isInterface() || org.eclipse.jdt.core.dom.Modifier.isAbstract(node.getModifiers())) {
      abstractToken = new KeywordToken(Keyword.ABSTRACT, 0);
    }
    // type parameters
    TypeParameterList typeParams = null;
    {
      List<TypeParameter> typeParameters = Lists.newArrayList();
      List<?> javaTypeParameters = node.typeParameters();
      if (!javaTypeParameters.isEmpty()) {
        for (Iterator<?> I = javaTypeParameters.iterator(); I.hasNext();) {
          org.eclipse.jdt.core.dom.TypeParameter javaTypeParameter = (org.eclipse.jdt.core.dom.TypeParameter) I.next();
          TypeParameter typeParameter = translate(javaTypeParameter);
          typeParameters.add(typeParameter);
        }
        typeParams = new TypeParameterList(null, typeParameters, null);
      }
    }
    // extends
    ExtendsClause extendsClause = null;
    if (node.getSuperclassType() != null) {
      TypeName superType = translate(node.getSuperclassType());
      extendsClause = new ExtendsClause(null, superType);
    }
    // implements
    ImplementsClause implementsClause = null;
    if (!node.superInterfaceTypes().isEmpty()) {
      List<TypeName> interfaces = Lists.newArrayList();
      for (Object javaInterface : node.superInterfaceTypes()) {
        interfaces.add((TypeName) translate((org.eclipse.jdt.core.dom.ASTNode) javaInterface));
      }
      implementsClause = new ImplementsClause(null, interfaces);
    }
    // members
    List<ClassMember> members = Lists.newArrayList();
    for (Iterator<?> I = node.bodyDeclarations().iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.BodyDeclaration javaBodyDecl = (org.eclipse.jdt.core.dom.BodyDeclaration) I.next();
      ClassMember member = translate(javaBodyDecl);
      members.add(member);
    }
    return done(new ClassDeclaration(
        translateJavadoc(node),
        null,
        abstractToken,
        null,
        name,
        typeParams,
        extendsClause,
        null,
        implementsClause,
        null,
        members,
        null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeLiteral node) {
    org.eclipse.jdt.core.dom.Type javaType = node.getType();
    ASTNode result = null;
    if (javaType instanceof org.eclipse.jdt.core.dom.SimpleType) {
      result = translate(((org.eclipse.jdt.core.dom.SimpleType) javaType).getName());
    }
    return done(result);
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.TypeParameter node) {
    SimpleIdentifier name = translateSimpleName(node.getName());
    TypeName bound = null;
    {
      List<?> typeBounds = node.typeBounds();
      if (typeBounds.size() == 1) {
        org.eclipse.jdt.core.dom.Type javaBound = (org.eclipse.jdt.core.dom.Type) typeBounds.get(0);
        bound = (TypeName) translate(javaBound);
      }
    }
    return done(new TypeParameter(null, null, name, null, bound));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.VariableDeclarationFragment node) {
    return done(new VariableDeclaration(
        null,
        null,
        translateSimpleName(node.getName()),
        null,
        (Expression) translate(node.getInitializer())));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.VariableDeclarationStatement node) {
    return done(new VariableDeclarationStatement(translateVariableDeclarationList(
        false,
        node.getType(),
        node.fragments()), null));
  }

  @Override
  public boolean visit(org.eclipse.jdt.core.dom.WhileStatement node) {
    return done(new WhileStatement(
        null,
        null,
        (Expression) translate(node.getExpression()),
        null,
        (Statement) translate(node.getBody())));
  }

  /**
   * Set {@link #result} and return <code>false</code> - we don't want normal JDT visiting.
   */
  private boolean done(ASTNode node) {
    result = node;
    return false;
  }

  /**
   * Recursively translates given {@link org.eclipse.jdt.core.dom.ASTNode} to Dart {@link ASTNode}.
   * 
   * @return the corresponding Dart {@link ASTNode}, may be <code>null</code> if <code>null</code>
   *         argument was given; not <code>null</code> if argument is not <code>null</code> (if
   *         translation is not implemented, exception will be thrown).
   */
  @SuppressWarnings("unchecked")
  private <T extends ASTNode> T translate(final org.eclipse.jdt.core.dom.ASTNode node) {
    if (node == null) {
      return null;
    }
    ExecutionUtils.runRethrow(new RunnableEx() {
      @Override
      public void run() throws Exception {
        Method method = getMostSpecificMethod(node.getClass());
        try {
          method.invoke(SyntaxTranslator.this, node);
        } catch (InvocationTargetException e) {
          ExecutionUtils.propagate(e.getCause());
        }
      }
    });
    Assert.isNotNull(result, "No result for: " + node.getClass().getCanonicalName());
    T castedResult = (T) result;
    result = null;
    return castedResult;
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.Expression} to the
   * {@link ArgumentList}.
   */
  private ArgumentList translateArgumentList(List<?> javaArguments) {
    List<Expression> arguments = translateExpressionList(javaArguments);
    return new ArgumentList(null, arguments, null);
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.Expression} to the
   * {@link List} of {@link Expression}s.
   */
  private List<Expression> translateExpressionList(List<?> javaArguments) {
    List<Expression> arguments = Lists.newArrayList();
    for (Iterator<?> I = javaArguments.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Expression javaArg = (org.eclipse.jdt.core.dom.Expression) I.next();
      Expression dartArg = translate(javaArg);
      arguments.add(dartArg);
    }
    return arguments;
  }

  private Comment translateJavadoc(org.eclipse.jdt.core.dom.BodyDeclaration node) {
    return (Comment) translate(node.getJavadoc());
  }

  private SimpleIdentifier translateSimpleName(org.eclipse.jdt.core.dom.SimpleName name) {
    return translate(name);
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.Type} to the
   * {@link TypeArgumentList}.
   */
  private TypeArgumentList translateTypeArgumentList(List<?> javaArguments) {
    List<TypeName> arguments = Lists.newArrayList();
    for (Iterator<?> I = javaArguments.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.Type javaArg = (org.eclipse.jdt.core.dom.Type) I.next();
      TypeName dartArg = translate(javaArg);
      arguments.add(dartArg);
    }
    return new TypeArgumentList(null, arguments, null);
  }

  /**
   * Translates given {@link List} of {@link org.eclipse.jdt.core.dom.VariableDeclarationFragment}
   * to the {@link VariableDeclarationList}.
   */
  private VariableDeclarationList translateVariableDeclarationList(boolean isFinal,
      org.eclipse.jdt.core.dom.Type javaType, List<?> javaVars) {
    List<VariableDeclaration> variableDeclarations = Lists.newArrayList();
    for (Iterator<?> I = javaVars.iterator(); I.hasNext();) {
      org.eclipse.jdt.core.dom.VariableDeclarationFragment javaFragment = (org.eclipse.jdt.core.dom.VariableDeclarationFragment) I.next();
      VariableDeclaration var = translate(javaFragment);
      variableDeclarations.add(var);
    }
    Token tokenFinal = isFinal ? new KeywordToken(Keyword.FINAL, 0) : null;
    return new VariableDeclarationList(
        tokenFinal,
        (TypeName) translate(javaType),
        variableDeclarations);
  }
}
