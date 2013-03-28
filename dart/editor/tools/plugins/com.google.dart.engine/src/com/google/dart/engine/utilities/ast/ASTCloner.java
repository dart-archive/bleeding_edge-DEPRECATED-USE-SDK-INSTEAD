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

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class {@code ASTCloner} implement an object that will clone any AST structure
 * that it visits. The cloner will only clone the structure, it will not preserve any resolution
 * results or properties associated with the nodes.
 */
public class ASTCloner implements ASTVisitor<ASTNode> {
  @Override
  public AdjacentStrings visitAdjacentStrings(AdjacentStrings node) {
    return new AdjacentStrings(clone(node.getStrings()));
  }

  @Override
  public Annotation visitAnnotation(Annotation node) {
    return new Annotation(
        node.getAtSign(),
        clone(node.getName()),
        node.getPeriod(),
        clone(node.getConstructorName()),
        clone(node.getArguments()));
  }

  @Override
  public ArgumentDefinitionTest visitArgumentDefinitionTest(ArgumentDefinitionTest node) {
    return new ArgumentDefinitionTest(node.getQuestion(), clone(node.getIdentifier()));
  }

  @Override
  public ArgumentList visitArgumentList(ArgumentList node) {
    return new ArgumentList(
        node.getLeftParenthesis(),
        clone(node.getArguments()),
        node.getRightParenthesis());
  }

  @Override
  public AsExpression visitAsExpression(AsExpression node) {
    return new AsExpression(
        clone(node.getExpression()),
        node.getAsOperator(),
        clone(node.getType()));
  }

  @Override
  public ASTNode visitAssertStatement(AssertStatement node) {
    return new AssertStatement(
        node.getKeyword(),
        node.getLeftParenthesis(),
        clone(node.getCondition()),
        node.getRightParenthesis(),
        node.getSemicolon());
  }

  @Override
  public AssignmentExpression visitAssignmentExpression(AssignmentExpression node) {
    return new AssignmentExpression(
        clone(node.getLeftHandSide()),
        node.getOperator(),
        clone(node.getRightHandSide()));
  }

  @Override
  public BinaryExpression visitBinaryExpression(BinaryExpression node) {
    return new BinaryExpression(
        clone(node.getLeftOperand()),
        node.getOperator(),
        clone(node.getRightOperand()));
  }

  @Override
  public Block visitBlock(Block node) {
    return new Block(node.getLeftBracket(), clone(node.getStatements()), node.getRightBracket());
  }

  @Override
  public BlockFunctionBody visitBlockFunctionBody(BlockFunctionBody node) {
    return new BlockFunctionBody(clone(node.getBlock()));
  }

  @Override
  public BooleanLiteral visitBooleanLiteral(BooleanLiteral node) {
    return new BooleanLiteral(node.getLiteral(), node.getValue());
  }

  @Override
  public BreakStatement visitBreakStatement(BreakStatement node) {
    return new BreakStatement(node.getKeyword(), clone(node.getLabel()), node.getSemicolon());
  }

  @Override
  public CascadeExpression visitCascadeExpression(CascadeExpression node) {
    return new CascadeExpression(clone(node.getTarget()), clone(node.getCascadeSections()));
  }

  @Override
  public CatchClause visitCatchClause(CatchClause node) {
    return new CatchClause(
        node.getOnKeyword(),
        clone(node.getExceptionType()),
        node.getCatchKeyword(),
        node.getLeftParenthesis(),
        clone(node.getExceptionParameter()),
        node.getComma(),
        clone(node.getStackTraceParameter()),
        node.getRightParenthesis(),
        clone(node.getBody()));
  }

  @Override
  public ClassDeclaration visitClassDeclaration(ClassDeclaration node) {
    return new ClassDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getAbstractKeyword(),
        node.getClassKeyword(),
        clone(node.getName()),
        clone(node.getTypeParameters()),
        clone(node.getExtendsClause()),
        clone(node.getWithClause()),
        clone(node.getImplementsClause()),
        node.getLeftBracket(),
        clone(node.getMembers()),
        node.getRightBracket());
  }

  @Override
  public ClassTypeAlias visitClassTypeAlias(ClassTypeAlias node) {
    return new ClassTypeAlias(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getName()),
        clone(node.getTypeParameters()),
        node.getEquals(),
        node.getAbstractKeyword(),
        clone(node.getSuperclass()),
        clone(node.getWithClause()),
        clone(node.getImplementsClause()),
        node.getSemicolon());
  }

  @Override
  public Comment visitComment(Comment node) {
    if (node.isDocumentation()) {
      return Comment.createDocumentationComment(node.getTokens(), clone(node.getReferences()));
    } else if (node.isBlock()) {
      return Comment.createBlockComment(node.getTokens());
    }
    return Comment.createEndOfLineComment(node.getTokens());
  }

  @Override
  public CommentReference visitCommentReference(CommentReference node) {
    return new CommentReference(node.getNewKeyword(), clone(node.getIdentifier()));
  }

  @Override
  public CompilationUnit visitCompilationUnit(CompilationUnit node) {
    CompilationUnit clone = new CompilationUnit(
        node.getBeginToken(),
        clone(node.getScriptTag()),
        clone(node.getDirectives()),
        clone(node.getDeclarations()),
        node.getEndToken());
    clone.setLineInfo(node.getLineInfo());
    clone.setParsingErrors(node.getParsingErrors());
    clone.setResolutionErrors(node.getResolutionErrors());
    return clone;
  }

  @Override
  public ConditionalExpression visitConditionalExpression(ConditionalExpression node) {
    return new ConditionalExpression(
        clone(node.getCondition()),
        node.getQuestion(),
        clone(node.getThenExpression()),
        node.getColon(),
        clone(node.getElseExpression()));
  }

  @Override
  public ConstructorDeclaration visitConstructorDeclaration(ConstructorDeclaration node) {
    return new ConstructorDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getExternalKeyword(),
        node.getConstKeyword(),
        node.getFactoryKeyword(),
        clone(node.getReturnType()),
        node.getPeriod(),
        clone(node.getName()),
        clone(node.getParameters()),
        node.getSeparator(),
        clone(node.getInitializers()),
        clone(node.getRedirectedConstructor()),
        clone(node.getBody()));
  }

  @Override
  public ConstructorFieldInitializer visitConstructorFieldInitializer(
      ConstructorFieldInitializer node) {
    return new ConstructorFieldInitializer(
        node.getKeyword(),
        node.getPeriod(),
        clone(node.getFieldName()),
        node.getEquals(),
        clone(node.getExpression()));
  }

  @Override
  public ConstructorName visitConstructorName(ConstructorName node) {
    return new ConstructorName(clone(node.getType()), node.getPeriod(), clone(node.getName()));
  }

  @Override
  public ContinueStatement visitContinueStatement(ContinueStatement node) {
    return new ContinueStatement(node.getKeyword(), clone(node.getLabel()), node.getSemicolon());
  }

  @Override
  public DeclaredIdentifier visitDeclaredIdentifier(DeclaredIdentifier node) {
    return new DeclaredIdentifier(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getType()),
        clone(node.getIdentifier()));
  }

  @Override
  public DefaultFormalParameter visitDefaultFormalParameter(DefaultFormalParameter node) {
    return new DefaultFormalParameter(
        clone(node.getParameter()),
        node.getKind(),
        node.getSeparator(),
        clone(node.getDefaultValue()));
  }

  @Override
  public DoStatement visitDoStatement(DoStatement node) {
    return new DoStatement(
        node.getDoKeyword(),
        clone(node.getBody()),
        node.getWhileKeyword(),
        node.getLeftParenthesis(),
        clone(node.getCondition()),
        node.getRightParenthesis(),
        node.getSemicolon());
  }

  @Override
  public DoubleLiteral visitDoubleLiteral(DoubleLiteral node) {
    return new DoubleLiteral(node.getLiteral(), node.getValue());
  }

  @Override
  public EmptyFunctionBody visitEmptyFunctionBody(EmptyFunctionBody node) {
    return new EmptyFunctionBody(node.getSemicolon());
  }

  @Override
  public EmptyStatement visitEmptyStatement(EmptyStatement node) {
    return new EmptyStatement(node.getSemicolon());
  }

  @Override
  public ExportDirective visitExportDirective(ExportDirective node) {
    return new ExportDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getUri()),
        clone(node.getCombinators()),
        node.getSemicolon());
  }

  @Override
  public ExpressionFunctionBody visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return new ExpressionFunctionBody(
        node.getFunctionDefinition(),
        clone(node.getExpression()),
        node.getSemicolon());
  }

  @Override
  public ExpressionStatement visitExpressionStatement(ExpressionStatement node) {
    return new ExpressionStatement(clone(node.getExpression()), node.getSemicolon());
  }

  @Override
  public ExtendsClause visitExtendsClause(ExtendsClause node) {
    return new ExtendsClause(node.getKeyword(), clone(node.getSuperclass()));
  }

  @Override
  public FieldDeclaration visitFieldDeclaration(FieldDeclaration node) {
    return new FieldDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getFields()),
        node.getSemicolon());
  }

  @Override
  public FieldFormalParameter visitFieldFormalParameter(FieldFormalParameter node) {
    return new FieldFormalParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getType()),
        node.getThisToken(),
        node.getPeriod(),
        clone(node.getIdentifier()));
  }

  @Override
  public ForEachStatement visitForEachStatement(ForEachStatement node) {
    return new ForEachStatement(
        node.getForKeyword(),
        node.getLeftParenthesis(),
        clone(node.getLoopVariable()),
        node.getInKeyword(),
        clone(node.getIterator()),
        node.getRightParenthesis(),
        clone(node.getBody()));
  }

  @Override
  public FormalParameterList visitFormalParameterList(FormalParameterList node) {
    return new FormalParameterList(
        node.getLeftParenthesis(),
        clone(node.getParameters()),
        node.getLeftDelimiter(),
        node.getRightDelimiter(),
        node.getRightParenthesis());
  }

  @Override
  public ForStatement visitForStatement(ForStatement node) {
    return new ForStatement(
        node.getForKeyword(),
        node.getLeftParenthesis(),
        clone(node.getVariables()),
        clone(node.getInitialization()),
        node.getLeftSeparator(),
        clone(node.getCondition()),
        node.getRightSeparator(),
        clone(node.getUpdaters()),
        node.getRightParenthesis(),
        clone(node.getBody()));
  }

  @Override
  public FunctionDeclaration visitFunctionDeclaration(FunctionDeclaration node) {
    return new FunctionDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getExternalKeyword(),
        clone(node.getReturnType()),
        node.getPropertyKeyword(),
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
    return new FunctionExpression(clone(node.getParameters()), clone(node.getBody()));
  }

  @Override
  public FunctionExpressionInvocation visitFunctionExpressionInvocation(
      FunctionExpressionInvocation node) {
    return new FunctionExpressionInvocation(
        clone(node.getFunction()),
        clone(node.getArgumentList()));
  }

  @Override
  public FunctionTypeAlias visitFunctionTypeAlias(FunctionTypeAlias node) {
    return new FunctionTypeAlias(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getReturnType()),
        clone(node.getName()),
        clone(node.getTypeParameters()),
        clone(node.getParameters()),
        node.getSemicolon());
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
    return new HideCombinator(node.getKeyword(), clone(node.getHiddenNames()));
  }

  @Override
  public IfStatement visitIfStatement(IfStatement node) {
    return new IfStatement(
        node.getIfKeyword(),
        node.getLeftParenthesis(),
        clone(node.getCondition()),
        node.getRightParenthesis(),
        clone(node.getThenStatement()),
        node.getElseKeyword(),
        clone(node.getElseStatement()));
  }

  @Override
  public ImplementsClause visitImplementsClause(ImplementsClause node) {
    return new ImplementsClause(node.getKeyword(), clone(node.getInterfaces()));
  }

  @Override
  public ImportDirective visitImportDirective(ImportDirective node) {
    return new ImportDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getUri()),
        node.getAsToken(),
        clone(node.getPrefix()),
        clone(node.getCombinators()),
        node.getSemicolon());
  }

  @Override
  public IndexExpression visitIndexExpression(IndexExpression node) {
    Token period = node.getPeriod();
    if (period == null) {
      return new IndexExpression(
          clone(node.getArray()),
          node.getLeftBracket(),
          clone(node.getIndex()),
          node.getRightBracket());
    } else {
      return new IndexExpression(
          period,
          node.getLeftBracket(),
          clone(node.getIndex()),
          node.getRightBracket());
    }
  }

  @Override
  public InstanceCreationExpression visitInstanceCreationExpression(InstanceCreationExpression node) {
    return new InstanceCreationExpression(
        node.getKeyword(),
        clone(node.getConstructorName()),
        clone(node.getArgumentList()));
  }

  @Override
  public IntegerLiteral visitIntegerLiteral(IntegerLiteral node) {
    return new IntegerLiteral(node.getLiteral(), node.getValue());
  }

  @Override
  public InterpolationExpression visitInterpolationExpression(InterpolationExpression node) {
    return new InterpolationExpression(
        node.getLeftBracket(),
        clone(node.getExpression()),
        node.getRightBracket());
  }

  @Override
  public InterpolationString visitInterpolationString(InterpolationString node) {
    return new InterpolationString(node.getContents(), node.getValue());
  }

  @Override
  public IsExpression visitIsExpression(IsExpression node) {
    return new IsExpression(
        clone(node.getExpression()),
        node.getIsOperator(),
        node.getNotOperator(),
        clone(node.getType()));
  }

  @Override
  public Label visitLabel(Label node) {
    return new Label(clone(node.getLabel()), node.getColon());
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
        node.getLibraryToken(),
        clone(node.getName()),
        node.getSemicolon());
  }

  @Override
  public LibraryIdentifier visitLibraryIdentifier(LibraryIdentifier node) {
    return new LibraryIdentifier(clone(node.getComponents()));
  }

  @Override
  public ListLiteral visitListLiteral(ListLiteral node) {
    return new ListLiteral(
        node.getModifier(),
        clone(node.getTypeArguments()),
        node.getLeftBracket(),
        clone(node.getElements()),
        node.getRightBracket());
  }

  @Override
  public MapLiteral visitMapLiteral(MapLiteral node) {
    return new MapLiteral(
        node.getModifier(),
        clone(node.getTypeArguments()),
        node.getLeftBracket(),
        clone(node.getEntries()),
        node.getRightBracket());
  }

  @Override
  public MapLiteralEntry visitMapLiteralEntry(MapLiteralEntry node) {
    return new MapLiteralEntry(clone(node.getKey()), node.getSeparator(), clone(node.getValue()));
  }

  @Override
  public MethodDeclaration visitMethodDeclaration(MethodDeclaration node) {
    return new MethodDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getExternalKeyword(),
        node.getModifierKeyword(),
        clone(node.getReturnType()),
        node.getPropertyKeyword(),
        node.getOperatorKeyword(),
        clone(node.getName()),
        clone(node.getParameters()),
        clone(node.getBody()));
  }

  @Override
  public MethodInvocation visitMethodInvocation(MethodInvocation node) {
    return new MethodInvocation(
        clone(node.getTarget()),
        node.getPeriod(),
        clone(node.getMethodName()),
        clone(node.getArgumentList()));
  }

  @Override
  public NamedExpression visitNamedExpression(NamedExpression node) {
    return new NamedExpression(clone(node.getName()), clone(node.getExpression()));
  }

  @Override
  public NativeFunctionBody visitNativeFunctionBody(NativeFunctionBody node) {
    return new NativeFunctionBody(
        node.getNativeToken(),
        clone(node.getStringLiteral()),
        node.getSemicolon());
  }

  @Override
  public NullLiteral visitNullLiteral(NullLiteral node) {
    return new NullLiteral(node.getLiteral());
  }

  @Override
  public ParenthesizedExpression visitParenthesizedExpression(ParenthesizedExpression node) {
    return new ParenthesizedExpression(
        node.getLeftParenthesis(),
        clone(node.getExpression()),
        node.getRightParenthesis());
  }

  @Override
  public PartDirective visitPartDirective(PartDirective node) {
    return new PartDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getPartToken(),
        clone(node.getUri()),
        node.getSemicolon());
  }

  @Override
  public PartOfDirective visitPartOfDirective(PartOfDirective node) {
    return new PartOfDirective(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getPartToken(),
        node.getOfToken(),
        clone(node.getLibraryName()),
        node.getSemicolon());
  }

  @Override
  public PostfixExpression visitPostfixExpression(PostfixExpression node) {
    return new PostfixExpression(clone(node.getOperand()), node.getOperator());
  }

  @Override
  public PrefixedIdentifier visitPrefixedIdentifier(PrefixedIdentifier node) {
    return new PrefixedIdentifier(
        clone(node.getPrefix()),
        node.getPeriod(),
        clone(node.getIdentifier()));
  }

  @Override
  public PrefixExpression visitPrefixExpression(PrefixExpression node) {
    return new PrefixExpression(node.getOperator(), clone(node.getOperand()));
  }

  @Override
  public PropertyAccess visitPropertyAccess(PropertyAccess node) {
    return new PropertyAccess(
        clone(node.getTarget()),
        node.getOperator(),
        clone(node.getPropertyName()));
  }

  @Override
  public RedirectingConstructorInvocation visitRedirectingConstructorInvocation(
      RedirectingConstructorInvocation node) {
    return new RedirectingConstructorInvocation(
        node.getKeyword(),
        node.getPeriod(),
        clone(node.getConstructorName()),
        clone(node.getArgumentList()));
  }

  @Override
  public RethrowExpression visitRethrowExpression(RethrowExpression node) {
    return new RethrowExpression(node.getKeyword());
  }

  @Override
  public ReturnStatement visitReturnStatement(ReturnStatement node) {
    return new ReturnStatement(node.getKeyword(), clone(node.getExpression()), node.getSemicolon());
  }

  @Override
  public ScriptTag visitScriptTag(ScriptTag node) {
    return new ScriptTag(node.getScriptTag());
  }

  @Override
  public ShowCombinator visitShowCombinator(ShowCombinator node) {
    return new ShowCombinator(node.getKeyword(), clone(node.getShownNames()));
  }

  @Override
  public SimpleFormalParameter visitSimpleFormalParameter(SimpleFormalParameter node) {
    return new SimpleFormalParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getType()),
        clone(node.getIdentifier()));
  }

  @Override
  public SimpleIdentifier visitSimpleIdentifier(SimpleIdentifier node) {
    return new SimpleIdentifier(node.getToken());
  }

  @Override
  public SimpleStringLiteral visitSimpleStringLiteral(SimpleStringLiteral node) {
    return new SimpleStringLiteral(node.getLiteral(), node.getValue());
  }

  @Override
  public StringInterpolation visitStringInterpolation(StringInterpolation node) {
    return new StringInterpolation(clone(node.getElements()));
  }

  @Override
  public SuperConstructorInvocation visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return new SuperConstructorInvocation(
        node.getKeyword(),
        node.getPeriod(),
        clone(node.getConstructorName()),
        clone(node.getArgumentList()));
  }

  @Override
  public SuperExpression visitSuperExpression(SuperExpression node) {
    return new SuperExpression(node.getKeyword());
  }

  @Override
  public SwitchCase visitSwitchCase(SwitchCase node) {
    return new SwitchCase(
        clone(node.getLabels()),
        node.getKeyword(),
        clone(node.getExpression()),
        node.getColon(),
        clone(node.getStatements()));
  }

  @Override
  public SwitchDefault visitSwitchDefault(SwitchDefault node) {
    return new SwitchDefault(
        clone(node.getLabels()),
        node.getKeyword(),
        node.getColon(),
        clone(node.getStatements()));
  }

  @Override
  public SwitchStatement visitSwitchStatement(SwitchStatement node) {
    return new SwitchStatement(
        node.getKeyword(),
        node.getLeftParenthesis(),
        clone(node.getExpression()),
        node.getRightParenthesis(),
        node.getLeftBracket(),
        clone(node.getMembers()),
        node.getRightBracket());
  }

  @Override
  public ThisExpression visitThisExpression(ThisExpression node) {
    return new ThisExpression(node.getKeyword());
  }

  @Override
  public ThrowExpression visitThrowExpression(ThrowExpression node) {
    return new ThrowExpression(node.getKeyword(), clone(node.getExpression()));
  }

  @Override
  public TopLevelVariableDeclaration visitTopLevelVariableDeclaration(
      TopLevelVariableDeclaration node) {
    return new TopLevelVariableDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        clone(node.getVariables()),
        node.getSemicolon());
  }

  @Override
  public TryStatement visitTryStatement(TryStatement node) {
    return new TryStatement(
        node.getTryKeyword(),
        clone(node.getBody()),
        clone(node.getCatchClauses()),
        node.getFinallyKeyword(),
        clone(node.getFinallyClause()));
  }

  @Override
  public TypeArgumentList visitTypeArgumentList(TypeArgumentList node) {
    return new TypeArgumentList(
        node.getLeftBracket(),
        clone(node.getArguments()),
        node.getRightBracket());
  }

  @Override
  public TypeName visitTypeName(TypeName node) {
    return new TypeName(clone(node.getName()), clone(node.getTypeArguments()));
  }

  @Override
  public TypeParameter visitTypeParameter(TypeParameter node) {
    return new TypeParameter(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        clone(node.getName()),
        node.getKeyword(),
        clone(node.getBound()));
  }

  @Override
  public TypeParameterList visitTypeParameterList(TypeParameterList node) {
    return new TypeParameterList(
        node.getLeftBracket(),
        clone(node.getTypeParameters()),
        node.getRightBracket());
  }

  @Override
  public VariableDeclaration visitVariableDeclaration(VariableDeclaration node) {
    return new VariableDeclaration(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        clone(node.getName()),
        node.getEquals(),
        clone(node.getInitializer()));
  }

  @Override
  public VariableDeclarationList visitVariableDeclarationList(VariableDeclarationList node) {
    return new VariableDeclarationList(
        clone(node.getDocumentationComment()),
        clone(node.getMetadata()),
        node.getKeyword(),
        clone(node.getType()),
        clone(node.getVariables()));
  }

  @Override
  public VariableDeclarationStatement visitVariableDeclarationStatement(
      VariableDeclarationStatement node) {
    return new VariableDeclarationStatement(clone(node.getVariables()), node.getSemicolon());
  }

  @Override
  public WhileStatement visitWhileStatement(WhileStatement node) {
    return new WhileStatement(
        node.getKeyword(),
        node.getLeftParenthesis(),
        clone(node.getCondition()),
        node.getRightParenthesis(),
        clone(node.getBody()));
  }

  @Override
  public WithClause visitWithClause(WithClause node) {
    return new WithClause(node.getWithKeyword(), clone(node.getMixinTypes()));
  }

  @SuppressWarnings("unchecked")
  private <E extends ASTNode> E clone(E node) {
    if (node == null) {
      return null;
    }
    return (E) node.accept(this);
  }

  @SuppressWarnings("unchecked")
  private <E extends ASTNode> List<E> clone(NodeList<E> nodes) {
    ArrayList<E> clonedNodes = new ArrayList<E>();
    for (E node : nodes) {
      clonedNodes.add((E) node.accept(this));
    }
    return clonedNodes;
  }
}
