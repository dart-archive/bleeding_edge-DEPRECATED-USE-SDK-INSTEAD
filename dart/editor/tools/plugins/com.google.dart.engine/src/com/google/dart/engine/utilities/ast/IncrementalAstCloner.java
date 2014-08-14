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
package com.google.dart.engine.utilities.ast;

import com.google.dart.engine.ast.*;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.utilities.collection.TokenMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class {@code IncrementalAstCloner} implement an object that will clone any AST
 * structure that it visits. The cloner will clone the structure, replacing the specified ASTNode
 * with a new ASTNode, mapping the old token stream to a new token stream, and preserving resolution
 * results.
 */
public class IncrementalAstCloner implements AstVisitor<AstNode> {

  /**
   * The node to be replaced during the cloning process.
   */
  private AstNode oldNode;

  /**
   * The replacement node used during the cloning process.
   */
  private AstNode newNode;

  /**
   * A mapping of old tokens to new tokens used during the cloning process.
   */
  private TokenMap tokenMap;

  /**
   * Construct a new instance that will replace {@code oldNode} with {@code newNode} in the process
   * of cloning an existing AST structure.
   * 
   * @param oldNode the node to be replaced
   * @param newNode the replacement node
   * @param tokenMap a mapping of old tokens to new tokens (not {@code null})
   */
  public IncrementalAstCloner(AstNode oldNode, AstNode newNode, TokenMap tokenMap) {
    this.oldNode = oldNode;
    this.newNode = newNode;
    this.tokenMap = tokenMap;
  }

  @Override
  public AdjacentStrings visitAdjacentStrings(AdjacentStrings node) {
    return new AdjacentStrings(cloneNodeList(node.getStrings()));
  }

  @Override
  public Annotation visitAnnotation(Annotation node) {
    Annotation copy = new Annotation(
        mapToken(node.getAtSign()),
        cloneNode(node.getName()),
        mapToken(node.getPeriod()),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArguments()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ArgumentList visitArgumentList(ArgumentList node) {
    return new ArgumentList(
        mapToken(node.getLeftParenthesis()),
        cloneNodeList(node.getArguments()),
        mapToken(node.getRightParenthesis()));
  }

  @Override
  public AsExpression visitAsExpression(AsExpression node) {
    AsExpression copy = new AsExpression(
        cloneNode(node.getExpression()),
        mapToken(node.getAsOperator()),
        cloneNode(node.getType()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public AstNode visitAssertStatement(AssertStatement node) {
    return new AssertStatement(
        mapToken(node.getKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getCondition()),
        mapToken(node.getRightParenthesis()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public AssignmentExpression visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression copy = new AssignmentExpression(
        cloneNode(node.getLeftHandSide()),
        mapToken(node.getOperator()),
        cloneNode(node.getRightHandSide()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public AwaitExpression visitAwaitExpression(AwaitExpression node) {
    return new AwaitExpression(mapToken(node.getAwaitKeyword()), cloneNode(node.getExpression()));
  }

  @Override
  public BinaryExpression visitBinaryExpression(BinaryExpression node) {
    BinaryExpression copy = new BinaryExpression(
        cloneNode(node.getLeftOperand()),
        mapToken(node.getOperator()),
        cloneNode(node.getRightOperand()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public Block visitBlock(Block node) {
    return new Block(
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getStatements()),
        mapToken(node.getRightBracket()));
  }

  @Override
  public BlockFunctionBody visitBlockFunctionBody(BlockFunctionBody node) {
    return new BlockFunctionBody(
        mapToken(node.getKeyword()),
        mapToken(node.getStar()),
        cloneNode(node.getBlock()));
  }

  @Override
  public BooleanLiteral visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral copy = new BooleanLiteral(mapToken(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public BreakStatement visitBreakStatement(BreakStatement node) {
    return new BreakStatement(
        mapToken(node.getKeyword()),
        cloneNode(node.getLabel()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public CascadeExpression visitCascadeExpression(CascadeExpression node) {
    CascadeExpression copy = new CascadeExpression(
        cloneNode(node.getTarget()),
        cloneNodeList(node.getCascadeSections()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public CatchClause visitCatchClause(CatchClause node) {
    return new CatchClause(
        mapToken(node.getOnKeyword()),
        cloneNode(node.getExceptionType()),
        mapToken(node.getCatchKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getExceptionParameter()),
        mapToken(node.getComma()),
        cloneNode(node.getStackTraceParameter()),
        mapToken(node.getRightParenthesis()),
        cloneNode(node.getBody()));
  }

  @Override
  public ClassDeclaration visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration copy = new ClassDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getAbstractKeyword()),
        mapToken(node.getClassKeyword()),
        cloneNode(node.getName()),
        cloneNode(node.getTypeParameters()),
        cloneNode(node.getExtendsClause()),
        cloneNode(node.getWithClause()),
        cloneNode(node.getImplementsClause()),
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getMembers()),
        mapToken(node.getRightBracket()));
    copy.setNativeClause(cloneNode(node.getNativeClause()));
    return copy;
  }

  @Override
  public ClassTypeAlias visitClassTypeAlias(ClassTypeAlias node) {
    return new ClassTypeAlias(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getName()),
        cloneNode(node.getTypeParameters()),
        mapToken(node.getEquals()),
        mapToken(node.getAbstractKeyword()),
        cloneNode(node.getSuperclass()),
        cloneNode(node.getWithClause()),
        cloneNode(node.getImplementsClause()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public Comment visitComment(Comment node) {
    if (node.isDocumentation()) {
      return Comment.createDocumentationCommentWithReferences(
          mapTokens(node.getTokens()),
          cloneNodeList(node.getReferences()));
    } else if (node.isBlock()) {
      return Comment.createBlockComment(mapTokens(node.getTokens()));
    }
    return Comment.createEndOfLineComment(mapTokens(node.getTokens()));
  }

  @Override
  public CommentReference visitCommentReference(CommentReference node) {
    return new CommentReference(mapToken(node.getNewKeyword()), cloneNode(node.getIdentifier()));
  }

  @Override
  public CompilationUnit visitCompilationUnit(CompilationUnit node) {
    CompilationUnit copy = new CompilationUnit(
        mapToken(node.getBeginToken()),
        cloneNode(node.getScriptTag()),
        cloneNodeList(node.getDirectives()),
        cloneNodeList(node.getDeclarations()),
        mapToken(node.getEndToken()));
    copy.setLineInfo(node.getLineInfo());
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ConditionalExpression visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression copy = new ConditionalExpression(
        cloneNode(node.getCondition()),
        mapToken(node.getQuestion()),
        cloneNode(node.getThenExpression()),
        mapToken(node.getColon()),
        cloneNode(node.getElseExpression()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ConstructorDeclaration visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration copy = new ConstructorDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getExternalKeyword()),
        mapToken(node.getConstKeyword()),
        mapToken(node.getFactoryKeyword()),
        cloneNode(node.getReturnType()),
        mapToken(node.getPeriod()),
        cloneNode(node.getName()),
        cloneNode(node.getParameters()),
        mapToken(node.getSeparator()),
        cloneNodeList(node.getInitializers()),
        cloneNode(node.getRedirectedConstructor()),
        cloneNode(node.getBody()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ConstructorFieldInitializer visitConstructorFieldInitializer(
      ConstructorFieldInitializer node) {
    return new ConstructorFieldInitializer(
        mapToken(node.getKeyword()),
        mapToken(node.getPeriod()),
        cloneNode(node.getFieldName()),
        mapToken(node.getEquals()),
        cloneNode(node.getExpression()));
  }

  @Override
  public ConstructorName visitConstructorName(ConstructorName node) {
    ConstructorName copy = new ConstructorName(
        cloneNode(node.getType()),
        mapToken(node.getPeriod()),
        cloneNode(node.getName()));
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public ContinueStatement visitContinueStatement(ContinueStatement node) {
    return new ContinueStatement(
        mapToken(node.getKeyword()),
        cloneNode(node.getLabel()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public DeclaredIdentifier visitDeclaredIdentifier(DeclaredIdentifier node) {
    return new DeclaredIdentifier(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getType()),
        cloneNode(node.getIdentifier()));
  }

  @Override
  public DefaultFormalParameter visitDefaultFormalParameter(DefaultFormalParameter node) {
    return new DefaultFormalParameter(
        cloneNode(node.getParameter()),
        node.getKind(),
        mapToken(node.getSeparator()),
        cloneNode(node.getDefaultValue()));
  }

  @Override
  public DoStatement visitDoStatement(DoStatement node) {
    return new DoStatement(
        mapToken(node.getDoKeyword()),
        cloneNode(node.getBody()),
        mapToken(node.getWhileKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getCondition()),
        mapToken(node.getRightParenthesis()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public DoubleLiteral visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral copy = new DoubleLiteral(mapToken(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public EmptyFunctionBody visitEmptyFunctionBody(EmptyFunctionBody node) {
    return new EmptyFunctionBody(mapToken(node.getSemicolon()));
  }

  @Override
  public EmptyStatement visitEmptyStatement(EmptyStatement node) {
    return new EmptyStatement(mapToken(node.getSemicolon()));
  }

  @Override
  public AstNode visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    return new EnumConstantDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getName()));
  }

  @Override
  public AstNode visitEnumDeclaration(EnumDeclaration node) {
    return new EnumDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getName()),
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getConstants()),
        mapToken(node.getRightBracket()));
  }

  @Override
  public ExportDirective visitExportDirective(ExportDirective node) {
    ExportDirective copy = new ExportDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getUri()),
        cloneNodeList(node.getCombinators()),
        mapToken(node.getSemicolon()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ExpressionFunctionBody visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return new ExpressionFunctionBody(
        mapToken(node.getKeyword()),
        mapToken(node.getFunctionDefinition()),
        cloneNode(node.getExpression()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public ExpressionStatement visitExpressionStatement(ExpressionStatement node) {
    return new ExpressionStatement(cloneNode(node.getExpression()), mapToken(node.getSemicolon()));
  }

  @Override
  public ExtendsClause visitExtendsClause(ExtendsClause node) {
    return new ExtendsClause(mapToken(node.getKeyword()), cloneNode(node.getSuperclass()));
  }

  @Override
  public FieldDeclaration visitFieldDeclaration(FieldDeclaration node) {
    return new FieldDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getStaticKeyword()),
        cloneNode(node.getFields()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public FieldFormalParameter visitFieldFormalParameter(FieldFormalParameter node) {
    return new FieldFormalParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getType()),
        mapToken(node.getThisToken()),
        mapToken(node.getPeriod()),
        cloneNode(node.getIdentifier()),
        cloneNode(node.getParameters()));
  }

  @Override
  public ForEachStatement visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.getLoopVariable();
    if (loopVariable == null) {
      return new ForEachStatement(
          mapToken(node.getAwaitKeyword()),
          mapToken(node.getForKeyword()),
          mapToken(node.getLeftParenthesis()),
          cloneNode(node.getIdentifier()),
          mapToken(node.getInKeyword()),
          cloneNode(node.getIterator()),
          mapToken(node.getRightParenthesis()),
          cloneNode(node.getBody()));
    }
    return new ForEachStatement(
        mapToken(node.getAwaitKeyword()),
        mapToken(node.getForKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(loopVariable),
        mapToken(node.getInKeyword()),
        cloneNode(node.getIterator()),
        mapToken(node.getRightParenthesis()),
        cloneNode(node.getBody()));
  }

  @Override
  public FormalParameterList visitFormalParameterList(FormalParameterList node) {
    return new FormalParameterList(
        mapToken(node.getLeftParenthesis()),
        cloneNodeList(node.getParameters()),
        mapToken(node.getLeftDelimiter()),
        mapToken(node.getRightDelimiter()),
        mapToken(node.getRightParenthesis()));
  }

  @Override
  public ForStatement visitForStatement(ForStatement node) {
    return new ForStatement(
        mapToken(node.getForKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getVariables()),
        cloneNode(node.getInitialization()),
        mapToken(node.getLeftSeparator()),
        cloneNode(node.getCondition()),
        mapToken(node.getRightSeparator()),
        cloneNodeList(node.getUpdaters()),
        mapToken(node.getRightParenthesis()),
        cloneNode(node.getBody()));
  }

  @Override
  public FunctionDeclaration visitFunctionDeclaration(FunctionDeclaration node) {
    return new FunctionDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getExternalKeyword()),
        cloneNode(node.getReturnType()),
        mapToken(node.getPropertyKeyword()),
        cloneNode(node.getName()),
        cloneNode(node.getFunctionExpression()));
  }

  @Override
  public FunctionDeclarationStatement visitFunctionDeclarationStatement(
      FunctionDeclarationStatement node) {
    return new FunctionDeclarationStatement(cloneNode(node.getFunctionDeclaration()));
  }

  @Override
  public FunctionExpression visitFunctionExpression(FunctionExpression node) {
    FunctionExpression copy = new FunctionExpression(
        cloneNode(node.getParameters()),
        cloneNode(node.getBody()));
    copy.setElement(node.getElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public FunctionExpressionInvocation visitFunctionExpressionInvocation(
      FunctionExpressionInvocation node) {
    FunctionExpressionInvocation copy = new FunctionExpressionInvocation(
        cloneNode(node.getFunction()),
        cloneNode(node.getArgumentList()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public FunctionTypeAlias visitFunctionTypeAlias(FunctionTypeAlias node) {
    return new FunctionTypeAlias(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getReturnType()),
        cloneNode(node.getName()),
        cloneNode(node.getTypeParameters()),
        cloneNode(node.getParameters()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public FunctionTypedFormalParameter visitFunctionTypedFormalParameter(
      FunctionTypedFormalParameter node) {
    return new FunctionTypedFormalParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getReturnType()),
        cloneNode(node.getIdentifier()),
        cloneNode(node.getParameters()));
  }

  @Override
  public HideCombinator visitHideCombinator(HideCombinator node) {
    return new HideCombinator(mapToken(node.getKeyword()), cloneNodeList(node.getHiddenNames()));
  }

  @Override
  public IfStatement visitIfStatement(IfStatement node) {
    return new IfStatement(
        mapToken(node.getIfKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getCondition()),
        mapToken(node.getRightParenthesis()),
        cloneNode(node.getThenStatement()),
        mapToken(node.getElseKeyword()),
        cloneNode(node.getElseStatement()));
  }

  @Override
  public ImplementsClause visitImplementsClause(ImplementsClause node) {
    return new ImplementsClause(mapToken(node.getKeyword()), cloneNodeList(node.getInterfaces()));
  }

  @Override
  public ImportDirective visitImportDirective(ImportDirective node) {
    return new ImportDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getUri()),
        mapToken(node.getDeferredToken()),
        mapToken(node.getAsToken()),
        cloneNode(node.getPrefix()),
        cloneNodeList(node.getCombinators()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public IndexExpression visitIndexExpression(IndexExpression node) {
    Token period = mapToken(node.getPeriod());
    IndexExpression copy;
    if (period == null) {
      copy = new IndexExpression(
          cloneNode(node.getTarget()),
          mapToken(node.getLeftBracket()),
          cloneNode(node.getIndex()),
          mapToken(node.getRightBracket()));
    } else {
      copy = new IndexExpression(
          period,
          mapToken(node.getLeftBracket()),
          cloneNode(node.getIndex()),
          mapToken(node.getRightBracket()));
    }
    copy.setAuxiliaryElements(node.getAuxiliaryElements());
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public InstanceCreationExpression visitInstanceCreationExpression(InstanceCreationExpression node) {
    InstanceCreationExpression copy = new InstanceCreationExpression(
        mapToken(node.getKeyword()),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArgumentList()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public IntegerLiteral visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral copy = new IntegerLiteral(mapToken(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public InterpolationExpression visitInterpolationExpression(InterpolationExpression node) {
    return new InterpolationExpression(
        mapToken(node.getLeftBracket()),
        cloneNode(node.getExpression()),
        mapToken(node.getRightBracket()));
  }

  @Override
  public InterpolationString visitInterpolationString(InterpolationString node) {
    return new InterpolationString(mapToken(node.getContents()), node.getValue());
  }

  @Override
  public IsExpression visitIsExpression(IsExpression node) {
    IsExpression copy = new IsExpression(
        cloneNode(node.getExpression()),
        mapToken(node.getIsOperator()),
        mapToken(node.getNotOperator()),
        cloneNode(node.getType()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public Label visitLabel(Label node) {
    return new Label(cloneNode(node.getLabel()), mapToken(node.getColon()));
  }

  @Override
  public LabeledStatement visitLabeledStatement(LabeledStatement node) {
    return new LabeledStatement(cloneNodeList(node.getLabels()), cloneNode(node.getStatement()));
  }

  @Override
  public LibraryDirective visitLibraryDirective(LibraryDirective node) {
    return new LibraryDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getLibraryToken()),
        cloneNode(node.getName()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public LibraryIdentifier visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier copy = new LibraryIdentifier(cloneNodeList(node.getComponents()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ListLiteral visitListLiteral(ListLiteral node) {
    ListLiteral copy = new ListLiteral(
        mapToken(node.getConstKeyword()),
        cloneNode(node.getTypeArguments()),
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getElements()),
        mapToken(node.getRightBracket()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public MapLiteral visitMapLiteral(MapLiteral node) {
    MapLiteral copy = new MapLiteral(
        mapToken(node.getConstKeyword()),
        cloneNode(node.getTypeArguments()),
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getEntries()),
        mapToken(node.getRightBracket()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public MapLiteralEntry visitMapLiteralEntry(MapLiteralEntry node) {
    return new MapLiteralEntry(
        cloneNode(node.getKey()),
        mapToken(node.getSeparator()),
        cloneNode(node.getValue()));
  }

  @Override
  public MethodDeclaration visitMethodDeclaration(MethodDeclaration node) {
    return new MethodDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getExternalKeyword()),
        mapToken(node.getModifierKeyword()),
        cloneNode(node.getReturnType()),
        mapToken(node.getPropertyKeyword()),
        mapToken(node.getOperatorKeyword()),
        cloneNode(node.getName()),
        cloneNode(node.getParameters()),
        cloneNode(node.getBody()));
  }

  @Override
  public MethodInvocation visitMethodInvocation(MethodInvocation node) {
    MethodInvocation copy = new MethodInvocation(
        cloneNode(node.getTarget()),
        mapToken(node.getPeriod()),
        cloneNode(node.getMethodName()),
        cloneNode(node.getArgumentList()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public NamedExpression visitNamedExpression(NamedExpression node) {
    NamedExpression copy = new NamedExpression(
        cloneNode(node.getName()),
        cloneNode(node.getExpression()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public AstNode visitNativeClause(NativeClause node) {
    return new NativeClause(mapToken(node.getKeyword()), cloneNode(node.getName()));
  }

  @Override
  public NativeFunctionBody visitNativeFunctionBody(NativeFunctionBody node) {
    return new NativeFunctionBody(
        mapToken(node.getNativeToken()),
        cloneNode(node.getStringLiteral()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public NullLiteral visitNullLiteral(NullLiteral node) {
    NullLiteral copy = new NullLiteral(mapToken(node.getLiteral()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ParenthesizedExpression visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression copy = new ParenthesizedExpression(
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getExpression()),
        mapToken(node.getRightParenthesis()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PartDirective visitPartDirective(PartDirective node) {
    PartDirective copy = new PartDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getPartToken()),
        cloneNode(node.getUri()),
        mapToken(node.getSemicolon()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public PartOfDirective visitPartOfDirective(PartOfDirective node) {
    PartOfDirective copy = new PartOfDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getPartToken()),
        mapToken(node.getOfToken()),
        cloneNode(node.getLibraryName()),
        mapToken(node.getSemicolon()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public PostfixExpression visitPostfixExpression(PostfixExpression node) {
    PostfixExpression copy = new PostfixExpression(
        cloneNode(node.getOperand()),
        mapToken(node.getOperator()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PrefixedIdentifier visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier copy = new PrefixedIdentifier(
        cloneNode(node.getPrefix()),
        mapToken(node.getPeriod()),
        cloneNode(node.getIdentifier()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PrefixExpression visitPrefixExpression(PrefixExpression node) {
    PrefixExpression copy = new PrefixExpression(
        mapToken(node.getOperator()),
        cloneNode(node.getOperand()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PropertyAccess visitPropertyAccess(PropertyAccess node) {
    PropertyAccess copy = new PropertyAccess(
        cloneNode(node.getTarget()),
        mapToken(node.getOperator()),
        cloneNode(node.getPropertyName()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public RedirectingConstructorInvocation visitRedirectingConstructorInvocation(
      RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation copy = new RedirectingConstructorInvocation(
        mapToken(node.getKeyword()),
        mapToken(node.getPeriod()),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArgumentList()));
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public RethrowExpression visitRethrowExpression(RethrowExpression node) {
    RethrowExpression copy = new RethrowExpression(mapToken(node.getKeyword()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ReturnStatement visitReturnStatement(ReturnStatement node) {
    return new ReturnStatement(
        mapToken(node.getKeyword()),
        cloneNode(node.getExpression()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public ScriptTag visitScriptTag(ScriptTag node) {
    return new ScriptTag(mapToken(node.getScriptTag()));
  }

  @Override
  public ShowCombinator visitShowCombinator(ShowCombinator node) {
    return new ShowCombinator(mapToken(node.getKeyword()), cloneNodeList(node.getShownNames()));
  }

  @Override
  public SimpleFormalParameter visitSimpleFormalParameter(SimpleFormalParameter node) {
    return new SimpleFormalParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getType()),
        cloneNode(node.getIdentifier()));
  }

  @Override
  public SimpleIdentifier visitSimpleIdentifier(SimpleIdentifier node) {
    Token mappedToken = mapToken(node.getToken());
    if (mappedToken == null) {
      // This only happens for SimpleIdentifiers created by the parser as part of scanning
      // documentation comments (the tokens for those identifiers are not in the original token
      // stream and hence do not get copied). This extra check can be removed if the scanner is
      // changed to scan documentation comments for the parser.
      mappedToken = node.getToken();
    }
    SimpleIdentifier copy = new SimpleIdentifier(mappedToken);
    copy.setAuxiliaryElements(node.getAuxiliaryElements());
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public SimpleStringLiteral visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral copy = new SimpleStringLiteral(mapToken(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public StringInterpolation visitStringInterpolation(StringInterpolation node) {
    StringInterpolation copy = new StringInterpolation(cloneNodeList(node.getElements()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public SuperConstructorInvocation visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation copy = new SuperConstructorInvocation(
        mapToken(node.getKeyword()),
        mapToken(node.getPeriod()),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArgumentList()));
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public SuperExpression visitSuperExpression(SuperExpression node) {
    SuperExpression copy = new SuperExpression(mapToken(node.getKeyword()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public SwitchCase visitSwitchCase(SwitchCase node) {
    return new SwitchCase(
        cloneNodeList(node.getLabels()),
        mapToken(node.getKeyword()),
        cloneNode(node.getExpression()),
        mapToken(node.getColon()),
        cloneNodeList(node.getStatements()));
  }

  @Override
  public SwitchDefault visitSwitchDefault(SwitchDefault node) {
    return new SwitchDefault(
        cloneNodeList(node.getLabels()),
        mapToken(node.getKeyword()),
        mapToken(node.getColon()),
        cloneNodeList(node.getStatements()));
  }

  @Override
  public SwitchStatement visitSwitchStatement(SwitchStatement node) {
    return new SwitchStatement(
        mapToken(node.getKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getExpression()),
        mapToken(node.getRightParenthesis()),
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getMembers()),
        mapToken(node.getRightBracket()));
  }

  @Override
  public AstNode visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral copy = new SymbolLiteral(
        mapToken(node.getPoundSign()),
        mapTokens(node.getComponents()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ThisExpression visitThisExpression(ThisExpression node) {
    ThisExpression copy = new ThisExpression(mapToken(node.getKeyword()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ThrowExpression visitThrowExpression(ThrowExpression node) {
    ThrowExpression copy = new ThrowExpression(
        mapToken(node.getKeyword()),
        cloneNode(node.getExpression()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public TopLevelVariableDeclaration visitTopLevelVariableDeclaration(
      TopLevelVariableDeclaration node) {
    return new TopLevelVariableDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getVariables()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public TryStatement visitTryStatement(TryStatement node) {
    return new TryStatement(
        mapToken(node.getTryKeyword()),
        cloneNode(node.getBody()),
        cloneNodeList(node.getCatchClauses()),
        mapToken(node.getFinallyKeyword()),
        cloneNode(node.getFinallyBlock()));
  }

  @Override
  public TypeArgumentList visitTypeArgumentList(TypeArgumentList node) {
    return new TypeArgumentList(
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getArguments()),
        mapToken(node.getRightBracket()));
  }

  @Override
  public TypeName visitTypeName(TypeName node) {
    TypeName copy = new TypeName(cloneNode(node.getName()), cloneNode(node.getTypeArguments()));
    copy.setType(node.getType());
    return copy;
  }

  @Override
  public TypeParameter visitTypeParameter(TypeParameter node) {
    return new TypeParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getName()),
        mapToken(node.getKeyword()),
        cloneNode(node.getBound()));
  }

  @Override
  public TypeParameterList visitTypeParameterList(TypeParameterList node) {
    return new TypeParameterList(
        mapToken(node.getLeftBracket()),
        cloneNodeList(node.getTypeParameters()),
        mapToken(node.getRightBracket()));
  }

  @Override
  public VariableDeclaration visitVariableDeclaration(VariableDeclaration node) {
    return new VariableDeclaration(
        null,
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getName()),
        mapToken(node.getEquals()),
        cloneNode(node.getInitializer()));
  }

  @Override
  public VariableDeclarationList visitVariableDeclarationList(VariableDeclarationList node) {
    return new VariableDeclarationList(
        null,
        cloneNodeList(node.getMetadata()),
        mapToken(node.getKeyword()),
        cloneNode(node.getType()),
        cloneNodeList(node.getVariables()));
  }

  @Override
  public VariableDeclarationStatement visitVariableDeclarationStatement(
      VariableDeclarationStatement node) {
    return new VariableDeclarationStatement(
        cloneNode(node.getVariables()),
        mapToken(node.getSemicolon()));
  }

  @Override
  public WhileStatement visitWhileStatement(WhileStatement node) {
    return new WhileStatement(
        mapToken(node.getKeyword()),
        mapToken(node.getLeftParenthesis()),
        cloneNode(node.getCondition()),
        mapToken(node.getRightParenthesis()),
        cloneNode(node.getBody()));
  }

  @Override
  public WithClause visitWithClause(WithClause node) {
    return new WithClause(mapToken(node.getWithKeyword()), cloneNodeList(node.getMixinTypes()));
  }

  @Override
  public YieldStatement visitYieldStatement(YieldStatement node) {
    return new YieldStatement(
        mapToken(node.getYieldKeyword()),
        mapToken(node.getStar()),
        cloneNode(node.getExpression()),
        mapToken(node.getSemicolon()));
  }

  @SuppressWarnings("unchecked")
  private <E extends AstNode> E cloneNode(E node) {
    if (node == null) {
      return null;
    }
    if (node == oldNode) {
      return (E) newNode;
    }
    return (E) node.accept(this);
  }

  private <E extends AstNode> List<E> cloneNodeList(NodeList<E> nodes) {
    ArrayList<E> clonedNodes = new ArrayList<E>();
    for (E node : nodes) {
      clonedNodes.add(cloneNode(node));
    }
    return clonedNodes;
  }

  private Token mapToken(Token oldToken) {
    if (oldToken == null) {
      return null;
    }
    return tokenMap.get(oldToken);
  }

  private Token[] mapTokens(Token[] oldTokens) {
    Token[] newTokens = new Token[oldTokens.length];
    for (int index = 0; index < newTokens.length; index++) {
      newTokens[index] = mapToken(oldTokens[index]);
    }
    return newTokens;
  }
}
