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
package com.google.dart.engine.parser;

import com.google.dart.engine.ast.*;

/**
 * Instances of the class {@code IncrementalParseDispatcher} implement a dispatcher that will invoke
 * the right parse method when re-parsing a specified child of the visited node. All of the methods
 * in this class assume that the parser is positioned to parse the replacement for the node. All of
 * the methods will throw an {@link IncrementalParseException} if the node could not be parsed for
 * some reason.
 */
public class IncrementalParseDispatcher implements ASTVisitor<ASTNode> {
  /**
   * The parser used to parse the replacement for the node.
   */
  private Parser parser;

  /**
   * The node that is to be replaced.
   */
  private ASTNode oldNode;

  /**
   * Initialize a newly created dispatcher to parse a single node that will replace the given node.
   * 
   * @param parser the parser used to parse the replacement for the node
   * @param oldNode the node that is to be replaced
   */
  public IncrementalParseDispatcher(Parser parser, ASTNode oldNode) {
    this.parser = parser;
    this.oldNode = oldNode;
  }

  @Override
  public ASTNode visitAdjacentStrings(AdjacentStrings node) {
    if (node.getStrings().contains(oldNode)) {
      return parser.parseStringLiteral();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitAnnotation(Annotation node) {
    if (oldNode == node.getName()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getConstructorName()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getArguments()) {
      return parser.parseArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    if (oldNode == node.getIdentifier()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitArgumentList(ArgumentList node) {
    if (node.getArguments().contains(oldNode)) {
      return parser.parseArgument();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitAsExpression(AsExpression node) {
    if (oldNode == node.getExpression()) {
      return parser.parseBitwiseOrExpression();
    } else if (oldNode == node.getType()) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitAssertStatement(AssertStatement node) {
    if (oldNode == node.getCondition()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitAssignmentExpression(AssignmentExpression node) {
    if (oldNode == node.getLeftHandSide()) {
      // TODO(brianwilkerson) If the assignment is part of a cascade section, then we don't have a
      // single parse method that will work. Otherwise, we can parse a conditional expression, but
      // need to ensure that the resulting expression is assignable.
      // return parser.parseConditionalExpression();
      throw new InsufficientContextException();
    } else if (oldNode == node.getRightHandSide()) {
      if (isCascadeAllowed(node)) {
        return parser.parseExpression();
      }
      return parser.parseExpressionWithoutCascade();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitBinaryExpression(BinaryExpression node) {
    if (oldNode == node.getLeftOperand()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getRightOperand()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitBlock(Block node) {
    if (node.getStatements().contains(oldNode)) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitBlockFunctionBody(BlockFunctionBody node) {
    if (oldNode == node.getBlock()) {
      return parser.parseBlock();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitBooleanLiteral(BooleanLiteral node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitBreakStatement(BreakStatement node) {
    if (oldNode == node.getLabel()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitCascadeExpression(CascadeExpression node) {
    if (oldNode == node.getTarget()) {
      return parser.parseConditionalExpression();
    } else if (node.getCascadeSections().contains(oldNode)) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitCatchClause(CatchClause node) {
    if (oldNode == node.getExceptionType()) {
      return parser.parseTypeName();
    } else if (oldNode == node.getExceptionParameter()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getStackTraceParameter()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getBody()) {
      return parser.parseBlock();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitClassDeclaration(ClassDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getTypeParameters()) {
      return parser.parseTypeParameterList();
    } else if (oldNode == node.getExtendsClause()) {
      return parser.parseExtendsClause();
    } else if (oldNode == node.getWithClause()) {
      return parser.parseWithClause();
    } else if (oldNode == node.getImplementsClause()) {
      return parser.parseImplementsClause();
    } else if (node.getMembers().contains(oldNode)) {
      return parser.parseClassMember(node.getName().getName());
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitClassTypeAlias(ClassTypeAlias node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getTypeParameters()) {
      return parser.parseTypeParameterList();
    } else if (oldNode == node.getSuperclass()) {
      return parser.parseTypeName();
    } else if (oldNode == node.getWithClause()) {
      return parser.parseWithClause();
    } else if (oldNode == node.getImplementsClause()) {
      return parser.parseImplementsClause();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitComment(Comment node) {
    throw new InsufficientContextException();
  }

  @Override
  public ASTNode visitCommentReference(CommentReference node) {
    if (oldNode == node.getIdentifier()) {
      return parser.parsePrefixedIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitCompilationUnit(CompilationUnit node) {
    throw new InsufficientContextException();
  }

  @Override
  public ASTNode visitConditionalExpression(ConditionalExpression node) {
    if (oldNode == node.getCondition()) {
      return parser.parseLogicalOrExpression();
    } else if (oldNode == node.getThenExpression()) {
      return parser.parseExpressionWithoutCascade();
    } else if (oldNode == node.getElseExpression()) {
      return parser.parseExpressionWithoutCascade();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitConstructorDeclaration(ConstructorDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getReturnType()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getName()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getParameters()) {
      return parser.parseFormalParameterList();
    } else if (oldNode == node.getRedirectedConstructor()) {
      throw new InsufficientContextException();
    } else if (node.getInitializers().contains(oldNode)) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getBody()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    if (oldNode == node.getFieldName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getExpression()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitConstructorName(ConstructorName node) {
    if (oldNode == node.getType()) {
      return parser.parseTypeName();
    } else if (oldNode == node.getName()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitContinueStatement(ContinueStatement node) {
    if (oldNode == node.getLabel()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitDeclaredIdentifier(DeclaredIdentifier node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getType()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getIdentifier()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitDefaultFormalParameter(DefaultFormalParameter node) {
    if (oldNode == node.getParameter()) {
      return parser.parseNormalFormalParameter();
    } else if (oldNode == node.getDefaultValue()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitDoStatement(DoStatement node) {
    if (oldNode == node.getBody()) {
      return parser.parseStatement();
    } else if (oldNode == node.getCondition()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitDoubleLiteral(DoubleLiteral node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitEmptyFunctionBody(EmptyFunctionBody node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitEmptyStatement(EmptyStatement node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitExportDirective(ExportDirective node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getUri()) {
      return parser.parseStringLiteral();
    } else if (node.getCombinators().contains(oldNode)) {
      throw new IncrementalParseException();
      //return parser.parseCombinator();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitExpressionFunctionBody(ExpressionFunctionBody node) {
    if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitExpressionStatement(ExpressionStatement node) {
    if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitExtendsClause(ExtendsClause node) {
    if (oldNode == node.getSuperclass()) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFieldDeclaration(FieldDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getFields()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFieldFormalParameter(FieldFormalParameter node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getType()) {
      return parser.parseTypeName();
    } else if (oldNode == node.getIdentifier()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getParameters()) {
      return parser.parseFormalParameterList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitForEachStatement(ForEachStatement node) {
    if (oldNode == node.getLoopVariable()) {
      throw new InsufficientContextException();
      //return parser.parseDeclaredIdentifier();
    } else if (oldNode == node.getIdentifier()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getBody()) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFormalParameterList(FormalParameterList node) {
    // We don't know which kind of parameter to parse.
    throw new InsufficientContextException();
  }

  @Override
  public ASTNode visitForStatement(ForStatement node) {
    if (oldNode == node.getVariables()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getInitialization()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getCondition()) {
      return parser.parseExpression();
    } else if (node.getUpdaters().contains(oldNode)) {
      return parser.parseExpression();
    } else if (oldNode == node.getBody()) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFunctionDeclaration(FunctionDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getReturnType()) {
      return parser.parseReturnType();
    } else if (oldNode == node.getName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getFunctionExpression()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    if (oldNode == node.getFunctionDeclaration()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFunctionExpression(FunctionExpression node) {
    if (oldNode == node.getParameters()) {
      return parser.parseFormalParameterList();
    } else if (oldNode == node.getBody()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    if (oldNode == node.getFunction()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getArgumentList()) {
      return parser.parseArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFunctionTypeAlias(FunctionTypeAlias node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getReturnType()) {
      return parser.parseReturnType();
    } else if (oldNode == node.getName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getTypeParameters()) {
      return parser.parseTypeParameterList();
    } else if (oldNode == node.getParameters()) {
      return parser.parseFormalParameterList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getReturnType()) {
      return parser.parseReturnType();
    } else if (oldNode == node.getIdentifier()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getParameters()) {
      return parser.parseFormalParameterList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitHideCombinator(HideCombinator node) {
    if (node.getHiddenNames().contains(oldNode)) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitIfStatement(IfStatement node) {
    if (oldNode == node.getCondition()) {
      return parser.parseExpression();
    } else if (oldNode == node.getThenStatement()) {
      return parser.parseStatement();
    } else if (oldNode == node.getElseStatement()) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitImplementsClause(ImplementsClause node) {
    if (node.getInterfaces().contains(node)) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitImportDirective(ImportDirective node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getUri()) {
      return parser.parseStringLiteral();
    } else if (oldNode == node.getPrefix()) {
      return parser.parseSimpleIdentifier();
    } else if (node.getCombinators().contains(oldNode)) {
      throw new IncrementalParseException();
      //return parser.parseCombinator();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitIndexExpression(IndexExpression node) {
    if (oldNode == node.getTarget()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getIndex()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitInstanceCreationExpression(InstanceCreationExpression node) {
    if (oldNode == node.getConstructorName()) {
      return parser.parseConstructorName();
    } else if (oldNode == node.getArgumentList()) {
      return parser.parseArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitIntegerLiteral(IntegerLiteral node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitInterpolationExpression(InterpolationExpression node) {
    if (oldNode == node.getExpression()) {
      if (node.getLeftBracket() == null) {
        throw new InsufficientContextException();
        //return parser.parseThisOrSimpleIdentifier();
      }
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitInterpolationString(InterpolationString node) {
    throw new InsufficientContextException();
  }

  @Override
  public ASTNode visitIsExpression(IsExpression node) {
    if (oldNode == node.getExpression()) {
      return parser.parseBitwiseOrExpression();
    } else if (oldNode == node.getType()) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitLabel(Label node) {
    if (oldNode == node.getLabel()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitLabeledStatement(LabeledStatement node) {
    if (node.getLabels().contains(oldNode)) {
      return parser.parseLabel();
    } else if (oldNode == node.getStatement()) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitLibraryDirective(LibraryDirective node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getName()) {
      return parser.parseLibraryIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitLibraryIdentifier(LibraryIdentifier node) {
    if (node.getComponents().contains(oldNode)) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitListLiteral(ListLiteral node) {
    if (oldNode == node.getTypeArguments()) {
      return parser.parseTypeArgumentList();
    } else if (node.getElements().contains(oldNode)) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitMapLiteral(MapLiteral node) {
    if (oldNode == node.getTypeArguments()) {
      return parser.parseTypeArgumentList();
    } else if (node.getEntries().contains(oldNode)) {
      return parser.parseMapLiteralEntry();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitMapLiteralEntry(MapLiteralEntry node) {
    if (oldNode == node.getKey()) {
      return parser.parseExpression();
    } else if (oldNode == node.getValue()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitMethodDeclaration(MethodDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getReturnType()) {
      throw new InsufficientContextException();
      //return parser.parseTypeName();
      //return parser.parseReturnType();
    } else if (oldNode == node.getName()) {
      if (node.getOperatorKeyword() != null) {
        throw new InsufficientContextException();
      }
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getBody()) {
      //return parser.parseFunctionBody();
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitMethodInvocation(MethodInvocation node) {
    if (oldNode == node.getTarget()) {
      throw new IncrementalParseException();
    } else if (oldNode == node.getMethodName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getArgumentList()) {
      return parser.parseArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitNamedExpression(NamedExpression node) {
    if (oldNode == node.getName()) {
      return parser.parseLabel();
    } else if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitNativeClause(NativeClause node) {
    if (oldNode == node.getName()) {
      return parser.parseStringLiteral();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitNativeFunctionBody(NativeFunctionBody node) {
    if (oldNode == node.getStringLiteral()) {
      return parser.parseStringLiteral();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitNullLiteral(NullLiteral node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitParenthesizedExpression(ParenthesizedExpression node) {
    if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitPartDirective(PartDirective node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getUri()) {
      return parser.parseStringLiteral();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitPartOfDirective(PartOfDirective node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getLibraryName()) {
      return parser.parseLibraryIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitPostfixExpression(PostfixExpression node) {
    if (oldNode == node.getOperand()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitPrefixedIdentifier(PrefixedIdentifier node) {
    if (oldNode == node.getPrefix()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getIdentifier()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitPrefixExpression(PrefixExpression node) {
    if (oldNode == node.getOperand()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitPropertyAccess(PropertyAccess node) {
    if (oldNode == node.getTarget()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getPropertyName()) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    if (oldNode == node.getConstructorName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getArgumentList()) {
      return parser.parseArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitRethrowExpression(RethrowExpression node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitReturnStatement(ReturnStatement node) {
    if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitScriptTag(ScriptTag node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitShowCombinator(ShowCombinator node) {
    if (node.getShownNames().contains(oldNode)) {
      return parser.parseSimpleIdentifier();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSimpleFormalParameter(SimpleFormalParameter node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getType()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getIdentifier()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSimpleIdentifier(SimpleIdentifier node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitSimpleStringLiteral(SimpleStringLiteral node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitStringInterpolation(StringInterpolation node) {
    if (node.getElements().contains(oldNode)) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    if (oldNode == node.getConstructorName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getArgumentList()) {
      return parser.parseArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSuperExpression(SuperExpression node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitSwitchCase(SwitchCase node) {
    if (node.getLabels().contains(oldNode)) {
      return parser.parseLabel();
    } else if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    } else if (node.getStatements().contains(oldNode)) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSwitchDefault(SwitchDefault node) {
    if (node.getLabels().contains(oldNode)) {
      return parser.parseLabel();
    } else if (node.getStatements().contains(oldNode)) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSwitchStatement(SwitchStatement node) {
    if (oldNode == node.getExpression()) {
      return parser.parseExpression();
    } else if (node.getMembers().contains(oldNode)) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitSymbolLiteral(SymbolLiteral node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitThisExpression(ThisExpression node) {
    return notAChild(node);
  }

  @Override
  public ASTNode visitThrowExpression(ThrowExpression node) {
    if (oldNode == node.getExpression()) {
      if (isCascadeAllowed(node)) {
        return parser.parseExpression();
      }
      return parser.parseExpressionWithoutCascade();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getVariables()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitTryStatement(TryStatement node) {
    if (oldNode == node.getBody()) {
      return parser.parseBlock();
    } else if (node.getCatchClauses().contains(oldNode)) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getFinallyBlock()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitTypeArgumentList(TypeArgumentList node) {
    if (node.getArguments().contains(oldNode)) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitTypeName(TypeName node) {
    if (oldNode == node.getName()) {
      return parser.parsePrefixedIdentifier();
    } else if (oldNode == node.getTypeArguments()) {
      return parser.parseTypeArgumentList();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitTypeParameter(TypeParameter node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getName()) {
      return parser.parseSimpleIdentifier();
    } else if (oldNode == node.getBound()) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitTypeParameterList(TypeParameterList node) {
    if (node.getTypeParameters().contains(node)) {
      return parser.parseTypeParameter();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitVariableDeclaration(VariableDeclaration node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (oldNode == node.getName()) {
      throw new InsufficientContextException();
    } else if (oldNode == node.getInitializer()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitVariableDeclarationList(VariableDeclarationList node) {
    if (oldNode == node.getDocumentationComment()) {
      throw new InsufficientContextException();
    } else if (node.getMetadata().contains(oldNode)) {
      return parser.parseAnnotation();
    } else if (node.getVariables().contains(oldNode)) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    if (oldNode == node.getVariables()) {
      throw new InsufficientContextException();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitWhileStatement(WhileStatement node) {
    if (oldNode == node.getCondition()) {
      return parser.parseExpression();
    } else if (oldNode == node.getBody()) {
      return parser.parseStatement();
    }
    return notAChild(node);
  }

  @Override
  public ASTNode visitWithClause(WithClause node) {
    if (node.getMixinTypes().contains(node)) {
      return parser.parseTypeName();
    }
    return notAChild(node);
  }

  /**
   * Return {@code true} if the given assignment expression can have a cascade expression on the
   * right-hand side.
   * 
   * @param node the assignment expression being tested
   * @return {@code true} if the right-hand side can be a cascade expression
   */
  private boolean isCascadeAllowed(AssignmentExpression node) {
    // TODO(brianwilkerson) Implement this method.
    throw new InsufficientContextException();
  }

  /**
   * Return {@code true} if the given throw expression can have a cascade expression.
   * 
   * @param node the throw expression being tested
   * @return {@code true} if the expression can be a cascade expression
   */
  private boolean isCascadeAllowed(ThrowExpression node) {
    // TODO(brianwilkerson) Implement this method.
    throw new InsufficientContextException();
  }

  /**
   * Throw an exception indicating that the visited node was not the parent of the node to be
   * replaced.
   * 
   * @param visitedNode the visited node that should have been the parent of the node to be replaced
   */
  private ASTNode notAChild(ASTNode visitedNode) {
    throw new IncrementalParseException("Internal error: the visited node (a "
        + visitedNode.getClass().getSimpleName()
        + ") was not the parent of the node to be replaced (a "
        + oldNode.getClass().getSimpleName() + ")");
  }
}
