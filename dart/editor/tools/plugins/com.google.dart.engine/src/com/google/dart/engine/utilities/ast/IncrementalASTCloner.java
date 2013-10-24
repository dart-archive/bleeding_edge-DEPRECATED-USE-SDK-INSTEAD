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
 * Instances of the class {@code IncrementalASTCloner} implement an object that will clone any AST
 * structure that it visits. The cloner will clone the structure, replacing the specified ASTNode
 * with a new ASTNode, mapping the old token stream to a new token stream, and preserving resolution
 * results.
 */
public class IncrementalASTCloner implements ASTVisitor<ASTNode> {

  /**
   * The node to be replaced during the cloning process.
   */
  private ASTNode oldNode;

  /**
   * The replacement node used during the cloning process.
   */
  private ASTNode newNode;

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
  public IncrementalASTCloner(ASTNode oldNode, ASTNode newNode, TokenMap tokenMap) {
    this.oldNode = oldNode;
    this.newNode = newNode;
    this.tokenMap = tokenMap;
  }

  @Override
  public AdjacentStrings visitAdjacentStrings(AdjacentStrings node) {
    return new AdjacentStrings(clone(node.getStrings()));
  }

  @Override
  public Annotation visitAnnotation(Annotation node) {
    Annotation copy = new Annotation(
        map(node.getAtSign()),
        clone(node.getName()),
        map(node.getPeriod()),
        clone(node.getConstructorName()),
        clone(node.getArguments()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ArgumentDefinitionTest visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    ArgumentDefinitionTest copy = new ArgumentDefinitionTest(
        map(node.getQuestion()),
        clone(node.getIdentifier()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ArgumentList visitArgumentList(ArgumentList node) {
    return new ArgumentList(
        map(node.getLeftParenthesis()),
        clone(node.getArguments()),
        map(node.getRightParenthesis()));
  }

  @Override
  public AsExpression visitAsExpression(AsExpression node) {
    AsExpression copy = new AsExpression(
        clone(node.getExpression()),
        map(node.getAsOperator()),
        clone(node.getType()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ASTNode visitAssertStatement(AssertStatement node) {
    return new AssertStatement(
        map(node.getKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getCondition()),
        map(node.getRightParenthesis()),
        map(node.getSemicolon()));
  }

  @Override
  public AssignmentExpression visitAssignmentExpression(AssignmentExpression node) {
    AssignmentExpression copy = new AssignmentExpression(
        clone(node.getLeftHandSide()),
        map(node.getOperator()),
        clone(node.getRightHandSide()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public BinaryExpression visitBinaryExpression(BinaryExpression node) {
    BinaryExpression copy = new BinaryExpression(
        clone(node.getLeftOperand()),
        map(node.getOperator()),
        clone(node.getRightOperand()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public Block visitBlock(Block node) {
    return new Block(
        map(node.getLeftBracket()),
        clone(node.getStatements()),
        map(node.getRightBracket()));
  }

  @Override
  public BlockFunctionBody visitBlockFunctionBody(BlockFunctionBody node) {
    return new BlockFunctionBody(clone(node.getBlock()));
  }

  @Override
  public BooleanLiteral visitBooleanLiteral(BooleanLiteral node) {
    BooleanLiteral copy = new BooleanLiteral(map(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public BreakStatement visitBreakStatement(BreakStatement node) {
    return new BreakStatement(
        map(node.getKeyword()),
        clone(node.getLabel()),
        map(node.getSemicolon()));
  }

  @Override
  public CascadeExpression visitCascadeExpression(CascadeExpression node) {
    CascadeExpression copy = new CascadeExpression(
        clone(node.getTarget()),
        clone(node.getCascadeSections()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public CatchClause visitCatchClause(CatchClause node) {
    return new CatchClause(
        map(node.getOnKeyword()),
        clone(node.getExceptionType()),
        map(node.getCatchKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getExceptionParameter()),
        map(node.getComma()),
        clone(node.getStackTraceParameter()),
        map(node.getRightParenthesis()),
        clone(node.getBody()));
  }

  @Override
  public ClassDeclaration visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration copy = new ClassDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getAbstractKeyword()),
        map(node.getClassKeyword()),
        clone(node.getName()),
        clone(node.getTypeParameters()),
        clone(node.getExtendsClause()),
        clone(node.getWithClause()),
        clone(node.getImplementsClause()),
        map(node.getLeftBracket()),
        clone(node.getMembers()),
        map(node.getRightBracket()));
    copy.setNativeClause(clone(node.getNativeClause()));
    return copy;
  }

  @Override
  public ClassTypeAlias visitClassTypeAlias(ClassTypeAlias node) {
    return new ClassTypeAlias(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getName()),
        clone(node.getTypeParameters()),
        map(node.getEquals()),
        map(node.getAbstractKeyword()),
        clone(node.getSuperclass()),
        clone(node.getWithClause()),
        clone(node.getImplementsClause()),
        map(node.getSemicolon()));
  }

  @Override
  public Comment visitComment(Comment node) {
    if (node.isDocumentation()) {
      return Comment.createDocumentationComment(map(node.getTokens()), clone(node.getReferences()));
    } else if (node.isBlock()) {
      return Comment.createBlockComment(map(node.getTokens()));
    }
    return Comment.createEndOfLineComment(map(node.getTokens()));
  }

  @Override
  public CommentReference visitCommentReference(CommentReference node) {
    return new CommentReference(map(node.getNewKeyword()), clone(node.getIdentifier()));
  }

  @Override
  public CompilationUnit visitCompilationUnit(CompilationUnit node) {
    CompilationUnit copy = new CompilationUnit(
        map(node.getBeginToken()),
        clone(node.getScriptTag()),
        clone(node.getDirectives()),
        clone(node.getDeclarations()),
        map(node.getEndToken()));
    copy.setLineInfo(node.getLineInfo());
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ConditionalExpression visitConditionalExpression(ConditionalExpression node) {
    ConditionalExpression copy = new ConditionalExpression(
        clone(node.getCondition()),
        map(node.getQuestion()),
        clone(node.getThenExpression()),
        map(node.getColon()),
        clone(node.getElseExpression()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ConstructorDeclaration visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorDeclaration copy = new ConstructorDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getExternalKeyword()),
        map(node.getConstKeyword()),
        map(node.getFactoryKeyword()),
        clone(node.getReturnType()),
        map(node.getPeriod()),
        clone(node.getName()),
        clone(node.getParameters()),
        map(node.getSeparator()),
        clone(node.getInitializers()),
        clone(node.getRedirectedConstructor()),
        clone(node.getBody()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ConstructorFieldInitializer visitConstructorFieldInitializer(
      ConstructorFieldInitializer node) {
    return new ConstructorFieldInitializer(
        map(node.getKeyword()),
        map(node.getPeriod()),
        clone(node.getFieldName()),
        map(node.getEquals()),
        clone(node.getExpression()));
  }

  @Override
  public ConstructorName visitConstructorName(ConstructorName node) {
    ConstructorName copy = new ConstructorName(
        clone(node.getType()),
        map(node.getPeriod()),
        clone(node.getName()));
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public ContinueStatement visitContinueStatement(ContinueStatement node) {
    return new ContinueStatement(
        map(node.getKeyword()),
        clone(node.getLabel()),
        map(node.getSemicolon()));
  }

  @Override
  public DeclaredIdentifier visitDeclaredIdentifier(DeclaredIdentifier node) {
    return new DeclaredIdentifier(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getType()),
        clone(node.getIdentifier()));
  }

  @Override
  public DefaultFormalParameter visitDefaultFormalParameter(DefaultFormalParameter node) {
    return new DefaultFormalParameter(
        clone(node.getParameter()),
        node.getKind(),
        map(node.getSeparator()),
        clone(node.getDefaultValue()));
  }

  @Override
  public DoStatement visitDoStatement(DoStatement node) {
    return new DoStatement(
        map(node.getDoKeyword()),
        clone(node.getBody()),
        map(node.getWhileKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getCondition()),
        map(node.getRightParenthesis()),
        map(node.getSemicolon()));
  }

  @Override
  public DoubleLiteral visitDoubleLiteral(DoubleLiteral node) {
    DoubleLiteral copy = new DoubleLiteral(map(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public EmptyFunctionBody visitEmptyFunctionBody(EmptyFunctionBody node) {
    return new EmptyFunctionBody(map(node.getSemicolon()));
  }

  @Override
  public EmptyStatement visitEmptyStatement(EmptyStatement node) {
    return new EmptyStatement(map(node.getSemicolon()));
  }

  @Override
  public ExportDirective visitExportDirective(ExportDirective node) {
    ExportDirective copy = new ExportDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getUri()),
        clone(node.getCombinators()),
        map(node.getSemicolon()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public ExpressionFunctionBody visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return new ExpressionFunctionBody(
        map(node.getFunctionDefinition()),
        clone(node.getExpression()),
        map(node.getSemicolon()));
  }

  @Override
  public ExpressionStatement visitExpressionStatement(ExpressionStatement node) {
    return new ExpressionStatement(clone(node.getExpression()), map(node.getSemicolon()));
  }

  @Override
  public ExtendsClause visitExtendsClause(ExtendsClause node) {
    return new ExtendsClause(map(node.getKeyword()), clone(node.getSuperclass()));
  }

  @Override
  public FieldDeclaration visitFieldDeclaration(FieldDeclaration node) {
    return new FieldDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getStaticKeyword()),
        clone(node.getFields()),
        map(node.getSemicolon()));
  }

  @Override
  public FieldFormalParameter visitFieldFormalParameter(FieldFormalParameter node) {
    return new FieldFormalParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getType()),
        map(node.getThisToken()),
        map(node.getPeriod()),
        clone(node.getIdentifier()),
        clone(node.getParameters()));
  }

  @Override
  public ForEachStatement visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.getLoopVariable();
    if (loopVariable == null) {
      return new ForEachStatement(
          map(node.getForKeyword()),
          map(node.getLeftParenthesis()),
          clone(node.getIdentifier()),
          map(node.getInKeyword()),
          clone(node.getIterator()),
          map(node.getRightParenthesis()),
          clone(node.getBody()));
    }
    return new ForEachStatement(
        map(node.getForKeyword()),
        map(node.getLeftParenthesis()),
        clone(loopVariable),
        map(node.getInKeyword()),
        clone(node.getIterator()),
        map(node.getRightParenthesis()),
        clone(node.getBody()));
  }

  @Override
  public FormalParameterList visitFormalParameterList(FormalParameterList node) {
    return new FormalParameterList(
        map(node.getLeftParenthesis()),
        clone(node.getParameters()),
        map(node.getLeftDelimiter()),
        map(node.getRightDelimiter()),
        map(node.getRightParenthesis()));
  }

  @Override
  public ForStatement visitForStatement(ForStatement node) {
    return new ForStatement(
        map(node.getForKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getVariables()),
        clone(node.getInitialization()),
        map(node.getLeftSeparator()),
        clone(node.getCondition()),
        map(node.getRightSeparator()),
        clone(node.getUpdaters()),
        map(node.getRightParenthesis()),
        clone(node.getBody()));
  }

  @Override
  public FunctionDeclaration visitFunctionDeclaration(FunctionDeclaration node) {
    return new FunctionDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getExternalKeyword()),
        clone(node.getReturnType()),
        map(node.getPropertyKeyword()),
        clone(node.getName()),
        clone(node.getFunctionExpression()));
  }

  @Override
  public FunctionDeclarationStatement visitFunctionDeclarationStatement(
      FunctionDeclarationStatement node) {
    return new FunctionDeclarationStatement(clone(node.getFunctionDeclaration()));
  }

  @Override
  public FunctionExpression visitFunctionExpression(FunctionExpression node) {
    FunctionExpression copy = new FunctionExpression(
        clone(node.getParameters()),
        clone(node.getBody()));
    copy.setElement(node.getElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public FunctionExpressionInvocation visitFunctionExpressionInvocation(
      FunctionExpressionInvocation node) {
    FunctionExpressionInvocation copy = new FunctionExpressionInvocation(
        clone(node.getFunction()),
        clone(node.getArgumentList()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public FunctionTypeAlias visitFunctionTypeAlias(FunctionTypeAlias node) {
    return new FunctionTypeAlias(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getReturnType()),
        clone(node.getName()),
        clone(node.getTypeParameters()),
        clone(node.getParameters()),
        map(node.getSemicolon()));
  }

  @Override
  public FunctionTypedFormalParameter visitFunctionTypedFormalParameter(
      FunctionTypedFormalParameter node) {
    return new FunctionTypedFormalParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        clone(node.getReturnType()),
        clone(node.getIdentifier()),
        clone(node.getParameters()));
  }

  @Override
  public HideCombinator visitHideCombinator(HideCombinator node) {
    return new HideCombinator(map(node.getKeyword()), clone(node.getHiddenNames()));
  }

  @Override
  public IfStatement visitIfStatement(IfStatement node) {
    return new IfStatement(
        map(node.getIfKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getCondition()),
        map(node.getRightParenthesis()),
        clone(node.getThenStatement()),
        map(node.getElseKeyword()),
        clone(node.getElseStatement()));
  }

  @Override
  public ImplementsClause visitImplementsClause(ImplementsClause node) {
    return new ImplementsClause(map(node.getKeyword()), clone(node.getInterfaces()));
  }

  @Override
  public ImportDirective visitImportDirective(ImportDirective node) {
    return new ImportDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getUri()),
        map(node.getAsToken()),
        clone(node.getPrefix()),
        clone(node.getCombinators()),
        map(node.getSemicolon()));
  }

  @Override
  public IndexExpression visitIndexExpression(IndexExpression node) {
    Token period = map(node.getPeriod());
    IndexExpression copy;
    if (period == null) {
      copy = new IndexExpression(
          clone(node.getTarget()),
          map(node.getLeftBracket()),
          clone(node.getIndex()),
          map(node.getRightBracket()));
    } else {
      copy = new IndexExpression(
          period,
          map(node.getLeftBracket()),
          clone(node.getIndex()),
          map(node.getRightBracket()));
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
        map(node.getKeyword()),
        clone(node.getConstructorName()),
        clone(node.getArgumentList()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public IntegerLiteral visitIntegerLiteral(IntegerLiteral node) {
    IntegerLiteral copy = new IntegerLiteral(map(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public InterpolationExpression visitInterpolationExpression(InterpolationExpression node) {
    return new InterpolationExpression(
        map(node.getLeftBracket()),
        clone(node.getExpression()),
        map(node.getRightBracket()));
  }

  @Override
  public InterpolationString visitInterpolationString(InterpolationString node) {
    return new InterpolationString(map(node.getContents()), node.getValue());
  }

  @Override
  public IsExpression visitIsExpression(IsExpression node) {
    IsExpression copy = new IsExpression(
        clone(node.getExpression()),
        map(node.getIsOperator()),
        map(node.getNotOperator()),
        clone(node.getType()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public Label visitLabel(Label node) {
    return new Label(clone(node.getLabel()), map(node.getColon()));
  }

  @Override
  public LabeledStatement visitLabeledStatement(LabeledStatement node) {
    return new LabeledStatement(clone(node.getLabels()), clone(node.getStatement()));
  }

  @Override
  public LibraryDirective visitLibraryDirective(LibraryDirective node) {
    return new LibraryDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getLibraryToken()),
        clone(node.getName()),
        map(node.getSemicolon()));
  }

  @Override
  public LibraryIdentifier visitLibraryIdentifier(LibraryIdentifier node) {
    LibraryIdentifier copy = new LibraryIdentifier(clone(node.getComponents()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ListLiteral visitListLiteral(ListLiteral node) {
    ListLiteral copy = new ListLiteral(
        map(node.getConstKeyword()),
        clone(node.getTypeArguments()),
        map(node.getLeftBracket()),
        clone(node.getElements()),
        map(node.getRightBracket()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public MapLiteral visitMapLiteral(MapLiteral node) {
    MapLiteral copy = new MapLiteral(
        map(node.getConstKeyword()),
        clone(node.getTypeArguments()),
        map(node.getLeftBracket()),
        clone(node.getEntries()),
        map(node.getRightBracket()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public MapLiteralEntry visitMapLiteralEntry(MapLiteralEntry node) {
    return new MapLiteralEntry(
        clone(node.getKey()),
        map(node.getSeparator()),
        clone(node.getValue()));
  }

  @Override
  public MethodDeclaration visitMethodDeclaration(MethodDeclaration node) {
    return new MethodDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getExternalKeyword()),
        map(node.getModifierKeyword()),
        clone(node.getReturnType()),
        map(node.getPropertyKeyword()),
        map(node.getOperatorKeyword()),
        clone(node.getName()),
        clone(node.getParameters()),
        clone(node.getBody()));
  }

  @Override
  public MethodInvocation visitMethodInvocation(MethodInvocation node) {
    MethodInvocation copy = new MethodInvocation(
        clone(node.getTarget()),
        map(node.getPeriod()),
        clone(node.getMethodName()),
        clone(node.getArgumentList()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public NamedExpression visitNamedExpression(NamedExpression node) {
    NamedExpression copy = new NamedExpression(clone(node.getName()), clone(node.getExpression()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ASTNode visitNativeClause(NativeClause node) {
    return new NativeClause(map(node.getKeyword()), clone(node.getName()));
  }

  @Override
  public NativeFunctionBody visitNativeFunctionBody(NativeFunctionBody node) {
    return new NativeFunctionBody(
        map(node.getNativeToken()),
        clone(node.getStringLiteral()),
        map(node.getSemicolon()));
  }

  @Override
  public NullLiteral visitNullLiteral(NullLiteral node) {
    NullLiteral copy = new NullLiteral(map(node.getLiteral()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ParenthesizedExpression visitParenthesizedExpression(ParenthesizedExpression node) {
    ParenthesizedExpression copy = new ParenthesizedExpression(
        map(node.getLeftParenthesis()),
        clone(node.getExpression()),
        map(node.getRightParenthesis()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PartDirective visitPartDirective(PartDirective node) {
    PartDirective copy = new PartDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getPartToken()),
        clone(node.getUri()),
        map(node.getSemicolon()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public PartOfDirective visitPartOfDirective(PartOfDirective node) {
    PartOfDirective copy = new PartOfDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getPartToken()),
        map(node.getOfToken()),
        clone(node.getLibraryName()),
        map(node.getSemicolon()));
    copy.setElement(node.getElement());
    return copy;
  }

  @Override
  public PostfixExpression visitPostfixExpression(PostfixExpression node) {
    PostfixExpression copy = new PostfixExpression(
        clone(node.getOperand()),
        map(node.getOperator()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PrefixedIdentifier visitPrefixedIdentifier(PrefixedIdentifier node) {
    PrefixedIdentifier copy = new PrefixedIdentifier(
        clone(node.getPrefix()),
        map(node.getPeriod()),
        clone(node.getIdentifier()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PrefixExpression visitPrefixExpression(PrefixExpression node) {
    PrefixExpression copy = new PrefixExpression(map(node.getOperator()), clone(node.getOperand()));
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public PropertyAccess visitPropertyAccess(PropertyAccess node) {
    PropertyAccess copy = new PropertyAccess(
        clone(node.getTarget()),
        map(node.getOperator()),
        clone(node.getPropertyName()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public RedirectingConstructorInvocation visitRedirectingConstructorInvocation(
      RedirectingConstructorInvocation node) {
    RedirectingConstructorInvocation copy = new RedirectingConstructorInvocation(
        map(node.getKeyword()),
        map(node.getPeriod()),
        clone(node.getConstructorName()),
        clone(node.getArgumentList()));
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public RethrowExpression visitRethrowExpression(RethrowExpression node) {
    RethrowExpression copy = new RethrowExpression(map(node.getKeyword()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ReturnStatement visitReturnStatement(ReturnStatement node) {
    return new ReturnStatement(
        map(node.getKeyword()),
        clone(node.getExpression()),
        map(node.getSemicolon()));
  }

  @Override
  public ScriptTag visitScriptTag(ScriptTag node) {
    return new ScriptTag(map(node.getScriptTag()));
  }

  @Override
  public ShowCombinator visitShowCombinator(ShowCombinator node) {
    return new ShowCombinator(map(node.getKeyword()), clone(node.getShownNames()));
  }

  @Override
  public SimpleFormalParameter visitSimpleFormalParameter(SimpleFormalParameter node) {
    return new SimpleFormalParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getType()),
        clone(node.getIdentifier()));
  }

  @Override
  public SimpleIdentifier visitSimpleIdentifier(SimpleIdentifier node) {
    SimpleIdentifier copy = new SimpleIdentifier(map(node.getToken()));
    copy.setAuxiliaryElements(node.getAuxiliaryElements());
    copy.setPropagatedElement(node.getPropagatedElement());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticElement(node.getStaticElement());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public SimpleStringLiteral visitSimpleStringLiteral(SimpleStringLiteral node) {
    SimpleStringLiteral copy = new SimpleStringLiteral(map(node.getLiteral()), node.getValue());
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public StringInterpolation visitStringInterpolation(StringInterpolation node) {
    StringInterpolation copy = new StringInterpolation(clone(node.getElements()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public SuperConstructorInvocation visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    SuperConstructorInvocation copy = new SuperConstructorInvocation(
        map(node.getKeyword()),
        map(node.getPeriod()),
        clone(node.getConstructorName()),
        clone(node.getArgumentList()));
    copy.setStaticElement(node.getStaticElement());
    return copy;
  }

  @Override
  public SuperExpression visitSuperExpression(SuperExpression node) {
    SuperExpression copy = new SuperExpression(map(node.getKeyword()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public SwitchCase visitSwitchCase(SwitchCase node) {
    return new SwitchCase(
        clone(node.getLabels()),
        map(node.getKeyword()),
        clone(node.getExpression()),
        map(node.getColon()),
        clone(node.getStatements()));
  }

  @Override
  public SwitchDefault visitSwitchDefault(SwitchDefault node) {
    return new SwitchDefault(
        clone(node.getLabels()),
        map(node.getKeyword()),
        map(node.getColon()),
        clone(node.getStatements()));
  }

  @Override
  public SwitchStatement visitSwitchStatement(SwitchStatement node) {
    return new SwitchStatement(
        map(node.getKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getExpression()),
        map(node.getRightParenthesis()),
        map(node.getLeftBracket()),
        clone(node.getMembers()),
        map(node.getRightBracket()));
  }

  @Override
  public ASTNode visitSymbolLiteral(SymbolLiteral node) {
    SymbolLiteral copy = new SymbolLiteral(map(node.getPoundSign()), map(node.getComponents()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ThisExpression visitThisExpression(ThisExpression node) {
    ThisExpression copy = new ThisExpression(map(node.getKeyword()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public ThrowExpression visitThrowExpression(ThrowExpression node) {
    ThrowExpression copy = new ThrowExpression(map(node.getKeyword()), clone(node.getExpression()));
    copy.setPropagatedType(node.getPropagatedType());
    copy.setStaticType(node.getStaticType());
    return copy;
  }

  @Override
  public TopLevelVariableDeclaration visitTopLevelVariableDeclaration(
      TopLevelVariableDeclaration node) {
    return new TopLevelVariableDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        clone(node.getVariables()),
        map(node.getSemicolon()));
  }

  @Override
  public TryStatement visitTryStatement(TryStatement node) {
    return new TryStatement(
        map(node.getTryKeyword()),
        clone(node.getBody()),
        clone(node.getCatchClauses()),
        map(node.getFinallyKeyword()),
        clone(node.getFinallyBlock()));
  }

  @Override
  public TypeArgumentList visitTypeArgumentList(TypeArgumentList node) {
    return new TypeArgumentList(
        map(node.getLeftBracket()),
        clone(node.getArguments()),
        map(node.getRightBracket()));
  }

  @Override
  public TypeName visitTypeName(TypeName node) {
    TypeName copy = new TypeName(clone(node.getName()), clone(node.getTypeArguments()));
    copy.setType(node.getType());
    return copy;
  }

  @Override
  public TypeParameter visitTypeParameter(TypeParameter node) {
    return new TypeParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        clone(node.getName()),
        map(node.getKeyword()),
        clone(node.getBound()));
  }

  @Override
  public TypeParameterList visitTypeParameterList(TypeParameterList node) {
    return new TypeParameterList(
        map(node.getLeftBracket()),
        clone(node.getTypeParameters()),
        map(node.getRightBracket()));
  }

  @Override
  public VariableDeclaration visitVariableDeclaration(VariableDeclaration node) {
    return new VariableDeclaration(
        null,
        clone(node.getMetadata()),
        clone(node.getName()),
        map(node.getEquals()),
        clone(node.getInitializer()));
  }

  @Override
  public VariableDeclarationList visitVariableDeclarationList(VariableDeclarationList node) {
    return new VariableDeclarationList(
        null,
        clone(node.getMetadata()),
        map(node.getKeyword()),
        clone(node.getType()),
        clone(node.getVariables()));
  }

  @Override
  public VariableDeclarationStatement visitVariableDeclarationStatement(
      VariableDeclarationStatement node) {
    return new VariableDeclarationStatement(clone(node.getVariables()), map(node.getSemicolon()));
  }

  @Override
  public WhileStatement visitWhileStatement(WhileStatement node) {
    return new WhileStatement(
        map(node.getKeyword()),
        map(node.getLeftParenthesis()),
        clone(node.getCondition()),
        map(node.getRightParenthesis()),
        clone(node.getBody()));
  }

  @Override
  public WithClause visitWithClause(WithClause node) {
    return new WithClause(map(node.getWithKeyword()), clone(node.getMixinTypes()));
  }

  @SuppressWarnings("unchecked")
  private <E extends ASTNode> E clone(E node) {
    if (node == null) {
      return null;
    }
    if (node == oldNode) {
      return (E) newNode;
    }
    return (E) node.accept(this);
  }

  private <E extends ASTNode> List<E> clone(NodeList<E> nodes) {
    ArrayList<E> clonedNodes = new ArrayList<E>();
    for (E node : nodes) {
      clonedNodes.add(clone(node));
    }
    return clonedNodes;
  }

  private Token map(Token oldToken) {
    if (oldToken == null) {
      return null;
    }
    return tokenMap.get(oldToken);
  }

  private Token[] map(Token[] oldTokens) {
    Token[] newTokens = new Token[oldTokens.length];
    for (int index = 0; index < newTokens.length; index++) {
      newTokens[index] = map(oldTokens[index]);
    }
    return newTokens;
  }
}
