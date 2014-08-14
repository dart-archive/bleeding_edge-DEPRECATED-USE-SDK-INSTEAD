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
 * Instances of the class {@code AstCloner} implement an object that will clone any AST structure
 * that it visits. The cloner will only clone the structure, it will not preserve any resolution
 * results or properties associated with the nodes.
 */
public class AstCloner implements AstVisitor<AstNode> {
  @SuppressWarnings("unchecked")
  public <E extends AstNode> List<E> cloneNodeList(NodeList<E> nodes) {
    int count = nodes.size();
    ArrayList<E> clonedNodes = new ArrayList<E>(count);
    for (int i = 0; i < count; i++) {
      clonedNodes.add((E) (nodes.get(i)).accept(this));
    }
    return clonedNodes;
  }

  @Override
  public AdjacentStrings visitAdjacentStrings(AdjacentStrings node) {
    return new AdjacentStrings(cloneNodeList(node.getStrings()));
  }

  @Override
  public Annotation visitAnnotation(Annotation node) {
    return new Annotation(
        node.getAtSign(),
        cloneNode(node.getName()),
        node.getPeriod(),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArguments()));
  }

  @Override
  public ArgumentList visitArgumentList(ArgumentList node) {
    return new ArgumentList(
        node.getLeftParenthesis(),
        cloneNodeList(node.getArguments()),
        node.getRightParenthesis());
  }

  @Override
  public AsExpression visitAsExpression(AsExpression node) {
    return new AsExpression(
        cloneNode(node.getExpression()),
        node.getAsOperator(),
        cloneNode(node.getType()));
  }

  @Override
  public AstNode visitAssertStatement(AssertStatement node) {
    return new AssertStatement(
        node.getKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getCondition()),
        node.getRightParenthesis(),
        node.getSemicolon());
  }

  @Override
  public AssignmentExpression visitAssignmentExpression(AssignmentExpression node) {
    return new AssignmentExpression(
        cloneNode(node.getLeftHandSide()),
        node.getOperator(),
        cloneNode(node.getRightHandSide()));
  }

  @Override
  public AwaitExpression visitAwaitExpression(AwaitExpression node) {
    return new AwaitExpression(node.getAwaitKeyword(), node.getExpression());
  }

  @Override
  public BinaryExpression visitBinaryExpression(BinaryExpression node) {
    return new BinaryExpression(
        cloneNode(node.getLeftOperand()),
        node.getOperator(),
        cloneNode(node.getRightOperand()));
  }

  @Override
  public Block visitBlock(Block node) {
    return new Block(
        node.getLeftBracket(),
        cloneNodeList(node.getStatements()),
        node.getRightBracket());
  }

  @Override
  public BlockFunctionBody visitBlockFunctionBody(BlockFunctionBody node) {
    return new BlockFunctionBody(node.getKeyword(), node.getStar(), cloneNode(node.getBlock()));
  }

  @Override
  public BooleanLiteral visitBooleanLiteral(BooleanLiteral node) {
    return new BooleanLiteral(node.getLiteral(), node.getValue());
  }

  @Override
  public BreakStatement visitBreakStatement(BreakStatement node) {
    return new BreakStatement(node.getKeyword(), cloneNode(node.getLabel()), node.getSemicolon());
  }

  @Override
  public CascadeExpression visitCascadeExpression(CascadeExpression node) {
    return new CascadeExpression(
        cloneNode(node.getTarget()),
        cloneNodeList(node.getCascadeSections()));
  }

  @Override
  public CatchClause visitCatchClause(CatchClause node) {
    return new CatchClause(
        node.getOnKeyword(),
        cloneNode(node.getExceptionType()),
        node.getCatchKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getExceptionParameter()),
        node.getComma(),
        cloneNode(node.getStackTraceParameter()),
        node.getRightParenthesis(),
        cloneNode(node.getBody()));
  }

  @Override
  public ClassDeclaration visitClassDeclaration(ClassDeclaration node) {
    ClassDeclaration copy = new ClassDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getAbstractKeyword(),
        node.getClassKeyword(),
        cloneNode(node.getName()),
        cloneNode(node.getTypeParameters()),
        cloneNode(node.getExtendsClause()),
        cloneNode(node.getWithClause()),
        cloneNode(node.getImplementsClause()),
        node.getLeftBracket(),
        cloneNodeList(node.getMembers()),
        node.getRightBracket());
    copy.setNativeClause(cloneNode(node.getNativeClause()));
    return copy;
  }

  @Override
  public ClassTypeAlias visitClassTypeAlias(ClassTypeAlias node) {
    return new ClassTypeAlias(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getName()),
        cloneNode(node.getTypeParameters()),
        node.getEquals(),
        node.getAbstractKeyword(),
        cloneNode(node.getSuperclass()),
        cloneNode(node.getWithClause()),
        cloneNode(node.getImplementsClause()),
        node.getSemicolon());
  }

  @Override
  public Comment visitComment(Comment node) {
    if (node.isDocumentation()) {
      return Comment.createDocumentationCommentWithReferences(
          node.getTokens(),
          cloneNodeList(node.getReferences()));
    } else if (node.isBlock()) {
      return Comment.createBlockComment(node.getTokens());
    }
    return Comment.createEndOfLineComment(node.getTokens());
  }

  @Override
  public CommentReference visitCommentReference(CommentReference node) {
    return new CommentReference(node.getNewKeyword(), cloneNode(node.getIdentifier()));
  }

  @Override
  public CompilationUnit visitCompilationUnit(CompilationUnit node) {
    CompilationUnit clone = new CompilationUnit(
        node.getBeginToken(),
        cloneNode(node.getScriptTag()),
        cloneNodeList(node.getDirectives()),
        cloneNodeList(node.getDeclarations()),
        node.getEndToken());
    clone.setLineInfo(node.getLineInfo());
    return clone;
  }

  @Override
  public ConditionalExpression visitConditionalExpression(ConditionalExpression node) {
    return new ConditionalExpression(
        cloneNode(node.getCondition()),
        node.getQuestion(),
        cloneNode(node.getThenExpression()),
        node.getColon(),
        cloneNode(node.getElseExpression()));
  }

  @Override
  public ConstructorDeclaration visitConstructorDeclaration(ConstructorDeclaration node) {
    return new ConstructorDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getExternalKeyword(),
        node.getConstKeyword(),
        node.getFactoryKeyword(),
        cloneNode(node.getReturnType()),
        node.getPeriod(),
        cloneNode(node.getName()),
        cloneNode(node.getParameters()),
        node.getSeparator(),
        cloneNodeList(node.getInitializers()),
        cloneNode(node.getRedirectedConstructor()),
        cloneNode(node.getBody()));
  }

  @Override
  public ConstructorFieldInitializer visitConstructorFieldInitializer(
      ConstructorFieldInitializer node) {
    return new ConstructorFieldInitializer(
        node.getKeyword(),
        node.getPeriod(),
        cloneNode(node.getFieldName()),
        node.getEquals(),
        cloneNode(node.getExpression()));
  }

  @Override
  public ConstructorName visitConstructorName(ConstructorName node) {
    return new ConstructorName(
        cloneNode(node.getType()),
        node.getPeriod(),
        cloneNode(node.getName()));
  }

  @Override
  public ContinueStatement visitContinueStatement(ContinueStatement node) {
    return new ContinueStatement(node.getKeyword(), cloneNode(node.getLabel()), node.getSemicolon());
  }

  @Override
  public DeclaredIdentifier visitDeclaredIdentifier(DeclaredIdentifier node) {
    return new DeclaredIdentifier(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getType()),
        cloneNode(node.getIdentifier()));
  }

  @Override
  public DefaultFormalParameter visitDefaultFormalParameter(DefaultFormalParameter node) {
    return new DefaultFormalParameter(
        cloneNode(node.getParameter()),
        node.getKind(),
        node.getSeparator(),
        cloneNode(node.getDefaultValue()));
  }

  @Override
  public DoStatement visitDoStatement(DoStatement node) {
    return new DoStatement(
        node.getDoKeyword(),
        cloneNode(node.getBody()),
        node.getWhileKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getCondition()),
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
  public AstNode visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    return new EnumConstantDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getName()));
  }

  @Override
  public EnumDeclaration visitEnumDeclaration(EnumDeclaration node) {
    return new EnumDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getName()),
        node.getLeftBracket(),
        cloneNodeList(node.getConstants()),
        node.getRightBracket());
  }

  @Override
  public ExportDirective visitExportDirective(ExportDirective node) {
    ExportDirective directive = new ExportDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getUri()),
        cloneNodeList(node.getCombinators()),
        node.getSemicolon());
    directive.setSource(node.getSource());
    directive.setUriContent(node.getUriContent());
    return directive;
  }

  @Override
  public ExpressionFunctionBody visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return new ExpressionFunctionBody(
        node.getKeyword(),
        node.getFunctionDefinition(),
        cloneNode(node.getExpression()),
        node.getSemicolon());
  }

  @Override
  public ExpressionStatement visitExpressionStatement(ExpressionStatement node) {
    return new ExpressionStatement(cloneNode(node.getExpression()), node.getSemicolon());
  }

  @Override
  public ExtendsClause visitExtendsClause(ExtendsClause node) {
    return new ExtendsClause(node.getKeyword(), cloneNode(node.getSuperclass()));
  }

  @Override
  public FieldDeclaration visitFieldDeclaration(FieldDeclaration node) {
    return new FieldDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getStaticKeyword(),
        cloneNode(node.getFields()),
        node.getSemicolon());
  }

  @Override
  public FieldFormalParameter visitFieldFormalParameter(FieldFormalParameter node) {
    return new FieldFormalParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getType()),
        node.getThisToken(),
        node.getPeriod(),
        cloneNode(node.getIdentifier()),
        cloneNode(node.getParameters()));
  }

  @Override
  public ForEachStatement visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.getLoopVariable();
    if (loopVariable == null) {
      return new ForEachStatement(
          node.getAwaitKeyword(),
          node.getForKeyword(),
          node.getLeftParenthesis(),
          cloneNode(node.getIdentifier()),
          node.getInKeyword(),
          cloneNode(node.getIterator()),
          node.getRightParenthesis(),
          cloneNode(node.getBody()));
    }
    return new ForEachStatement(
        node.getAwaitKeyword(),
        node.getForKeyword(),
        node.getLeftParenthesis(),
        cloneNode(loopVariable),
        node.getInKeyword(),
        cloneNode(node.getIterator()),
        node.getRightParenthesis(),
        cloneNode(node.getBody()));
  }

  @Override
  public FormalParameterList visitFormalParameterList(FormalParameterList node) {
    return new FormalParameterList(
        node.getLeftParenthesis(),
        cloneNodeList(node.getParameters()),
        node.getLeftDelimiter(),
        node.getRightDelimiter(),
        node.getRightParenthesis());
  }

  @Override
  public ForStatement visitForStatement(ForStatement node) {
    return new ForStatement(
        node.getForKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getVariables()),
        cloneNode(node.getInitialization()),
        node.getLeftSeparator(),
        cloneNode(node.getCondition()),
        node.getRightSeparator(),
        cloneNodeList(node.getUpdaters()),
        node.getRightParenthesis(),
        cloneNode(node.getBody()));
  }

  @Override
  public FunctionDeclaration visitFunctionDeclaration(FunctionDeclaration node) {
    return new FunctionDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getExternalKeyword(),
        cloneNode(node.getReturnType()),
        node.getPropertyKeyword(),
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
    return new FunctionExpression(cloneNode(node.getParameters()), cloneNode(node.getBody()));
  }

  @Override
  public FunctionExpressionInvocation visitFunctionExpressionInvocation(
      FunctionExpressionInvocation node) {
    return new FunctionExpressionInvocation(
        cloneNode(node.getFunction()),
        cloneNode(node.getArgumentList()));
  }

  @Override
  public FunctionTypeAlias visitFunctionTypeAlias(FunctionTypeAlias node) {
    return new FunctionTypeAlias(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getReturnType()),
        cloneNode(node.getName()),
        cloneNode(node.getTypeParameters()),
        cloneNode(node.getParameters()),
        node.getSemicolon());
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
    return new HideCombinator(node.getKeyword(), cloneNodeList(node.getHiddenNames()));
  }

  @Override
  public IfStatement visitIfStatement(IfStatement node) {
    return new IfStatement(
        node.getIfKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getCondition()),
        node.getRightParenthesis(),
        cloneNode(node.getThenStatement()),
        node.getElseKeyword(),
        cloneNode(node.getElseStatement()));
  }

  @Override
  public ImplementsClause visitImplementsClause(ImplementsClause node) {
    return new ImplementsClause(node.getKeyword(), cloneNodeList(node.getInterfaces()));
  }

  @Override
  public ImportDirective visitImportDirective(ImportDirective node) {
    ImportDirective directive = new ImportDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getUri()),
        node.getDeferredToken(),
        node.getAsToken(),
        cloneNode(node.getPrefix()),
        cloneNodeList(node.getCombinators()),
        node.getSemicolon());
    directive.setSource(node.getSource());
    directive.setUriContent(node.getUriContent());
    return directive;
  }

  @Override
  public IndexExpression visitIndexExpression(IndexExpression node) {
    Token period = node.getPeriod();
    if (period == null) {
      return new IndexExpression(
          cloneNode(node.getTarget()),
          node.getLeftBracket(),
          cloneNode(node.getIndex()),
          node.getRightBracket());
    } else {
      return new IndexExpression(
          period,
          node.getLeftBracket(),
          cloneNode(node.getIndex()),
          node.getRightBracket());
    }
  }

  @Override
  public InstanceCreationExpression visitInstanceCreationExpression(InstanceCreationExpression node) {
    return new InstanceCreationExpression(
        node.getKeyword(),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArgumentList()));
  }

  @Override
  public IntegerLiteral visitIntegerLiteral(IntegerLiteral node) {
    return new IntegerLiteral(node.getLiteral(), node.getValue());
  }

  @Override
  public InterpolationExpression visitInterpolationExpression(InterpolationExpression node) {
    return new InterpolationExpression(
        node.getLeftBracket(),
        cloneNode(node.getExpression()),
        node.getRightBracket());
  }

  @Override
  public InterpolationString visitInterpolationString(InterpolationString node) {
    return new InterpolationString(node.getContents(), node.getValue());
  }

  @Override
  public IsExpression visitIsExpression(IsExpression node) {
    return new IsExpression(
        cloneNode(node.getExpression()),
        node.getIsOperator(),
        node.getNotOperator(),
        cloneNode(node.getType()));
  }

  @Override
  public Label visitLabel(Label node) {
    return new Label(cloneNode(node.getLabel()), node.getColon());
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
        node.getLibraryToken(),
        cloneNode(node.getName()),
        node.getSemicolon());
  }

  @Override
  public LibraryIdentifier visitLibraryIdentifier(LibraryIdentifier node) {
    return new LibraryIdentifier(cloneNodeList(node.getComponents()));
  }

  @Override
  public ListLiteral visitListLiteral(ListLiteral node) {
    return new ListLiteral(
        node.getConstKeyword(),
        cloneNode(node.getTypeArguments()),
        node.getLeftBracket(),
        cloneNodeList(node.getElements()),
        node.getRightBracket());
  }

  @Override
  public MapLiteral visitMapLiteral(MapLiteral node) {
    return new MapLiteral(
        node.getConstKeyword(),
        cloneNode(node.getTypeArguments()),
        node.getLeftBracket(),
        cloneNodeList(node.getEntries()),
        node.getRightBracket());
  }

  @Override
  public MapLiteralEntry visitMapLiteralEntry(MapLiteralEntry node) {
    return new MapLiteralEntry(
        cloneNode(node.getKey()),
        node.getSeparator(),
        cloneNode(node.getValue()));
  }

  @Override
  public MethodDeclaration visitMethodDeclaration(MethodDeclaration node) {
    return new MethodDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getExternalKeyword(),
        node.getModifierKeyword(),
        cloneNode(node.getReturnType()),
        node.getPropertyKeyword(),
        node.getOperatorKeyword(),
        cloneNode(node.getName()),
        cloneNode(node.getParameters()),
        cloneNode(node.getBody()));
  }

  @Override
  public MethodInvocation visitMethodInvocation(MethodInvocation node) {
    return new MethodInvocation(
        cloneNode(node.getTarget()),
        node.getPeriod(),
        cloneNode(node.getMethodName()),
        cloneNode(node.getArgumentList()));
  }

  @Override
  public NamedExpression visitNamedExpression(NamedExpression node) {
    return new NamedExpression(cloneNode(node.getName()), cloneNode(node.getExpression()));
  }

  @Override
  public AstNode visitNativeClause(NativeClause node) {
    return new NativeClause(node.getKeyword(), cloneNode(node.getName()));
  }

  @Override
  public NativeFunctionBody visitNativeFunctionBody(NativeFunctionBody node) {
    return new NativeFunctionBody(
        node.getNativeToken(),
        cloneNode(node.getStringLiteral()),
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
        cloneNode(node.getExpression()),
        node.getRightParenthesis());
  }

  @Override
  public PartDirective visitPartDirective(PartDirective node) {
    PartDirective directive = new PartDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getPartToken(),
        cloneNode(node.getUri()),
        node.getSemicolon());
    directive.setSource(node.getSource());
    directive.setUriContent(node.getUriContent());
    return directive;
  }

  @Override
  public PartOfDirective visitPartOfDirective(PartOfDirective node) {
    return new PartOfDirective(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getPartToken(),
        node.getOfToken(),
        cloneNode(node.getLibraryName()),
        node.getSemicolon());
  }

  @Override
  public PostfixExpression visitPostfixExpression(PostfixExpression node) {
    return new PostfixExpression(cloneNode(node.getOperand()), node.getOperator());
  }

  @Override
  public PrefixedIdentifier visitPrefixedIdentifier(PrefixedIdentifier node) {
    return new PrefixedIdentifier(
        cloneNode(node.getPrefix()),
        node.getPeriod(),
        cloneNode(node.getIdentifier()));
  }

  @Override
  public PrefixExpression visitPrefixExpression(PrefixExpression node) {
    return new PrefixExpression(node.getOperator(), cloneNode(node.getOperand()));
  }

  @Override
  public PropertyAccess visitPropertyAccess(PropertyAccess node) {
    return new PropertyAccess(
        cloneNode(node.getTarget()),
        node.getOperator(),
        cloneNode(node.getPropertyName()));
  }

  @Override
  public RedirectingConstructorInvocation visitRedirectingConstructorInvocation(
      RedirectingConstructorInvocation node) {
    return new RedirectingConstructorInvocation(
        node.getKeyword(),
        node.getPeriod(),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArgumentList()));
  }

  @Override
  public RethrowExpression visitRethrowExpression(RethrowExpression node) {
    return new RethrowExpression(node.getKeyword());
  }

  @Override
  public ReturnStatement visitReturnStatement(ReturnStatement node) {
    return new ReturnStatement(
        node.getKeyword(),
        cloneNode(node.getExpression()),
        node.getSemicolon());
  }

  @Override
  public ScriptTag visitScriptTag(ScriptTag node) {
    return new ScriptTag(node.getScriptTag());
  }

  @Override
  public ShowCombinator visitShowCombinator(ShowCombinator node) {
    return new ShowCombinator(node.getKeyword(), cloneNodeList(node.getShownNames()));
  }

  @Override
  public SimpleFormalParameter visitSimpleFormalParameter(SimpleFormalParameter node) {
    return new SimpleFormalParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getType()),
        cloneNode(node.getIdentifier()));
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
    return new StringInterpolation(cloneNodeList(node.getElements()));
  }

  @Override
  public SuperConstructorInvocation visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return new SuperConstructorInvocation(
        node.getKeyword(),
        node.getPeriod(),
        cloneNode(node.getConstructorName()),
        cloneNode(node.getArgumentList()));
  }

  @Override
  public SuperExpression visitSuperExpression(SuperExpression node) {
    return new SuperExpression(node.getKeyword());
  }

  @Override
  public SwitchCase visitSwitchCase(SwitchCase node) {
    return new SwitchCase(
        cloneNodeList(node.getLabels()),
        node.getKeyword(),
        cloneNode(node.getExpression()),
        node.getColon(),
        cloneNodeList(node.getStatements()));
  }

  @Override
  public SwitchDefault visitSwitchDefault(SwitchDefault node) {
    return new SwitchDefault(
        cloneNodeList(node.getLabels()),
        node.getKeyword(),
        node.getColon(),
        cloneNodeList(node.getStatements()));
  }

  @Override
  public SwitchStatement visitSwitchStatement(SwitchStatement node) {
    return new SwitchStatement(
        node.getKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getExpression()),
        node.getRightParenthesis(),
        node.getLeftBracket(),
        cloneNodeList(node.getMembers()),
        node.getRightBracket());
  }

  @Override
  public SymbolLiteral visitSymbolLiteral(SymbolLiteral node) {
    return new SymbolLiteral(node.getPoundSign(), node.getComponents());
  }

  @Override
  public ThisExpression visitThisExpression(ThisExpression node) {
    return new ThisExpression(node.getKeyword());
  }

  @Override
  public ThrowExpression visitThrowExpression(ThrowExpression node) {
    return new ThrowExpression(node.getKeyword(), cloneNode(node.getExpression()));
  }

  @Override
  public TopLevelVariableDeclaration visitTopLevelVariableDeclaration(
      TopLevelVariableDeclaration node) {
    return new TopLevelVariableDeclaration(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getVariables()),
        node.getSemicolon());
  }

  @Override
  public TryStatement visitTryStatement(TryStatement node) {
    return new TryStatement(
        node.getTryKeyword(),
        cloneNode(node.getBody()),
        cloneNodeList(node.getCatchClauses()),
        node.getFinallyKeyword(),
        cloneNode(node.getFinallyBlock()));
  }

  @Override
  public TypeArgumentList visitTypeArgumentList(TypeArgumentList node) {
    return new TypeArgumentList(
        node.getLeftBracket(),
        cloneNodeList(node.getArguments()),
        node.getRightBracket());
  }

  @Override
  public TypeName visitTypeName(TypeName node) {
    return new TypeName(cloneNode(node.getName()), cloneNode(node.getTypeArguments()));
  }

  @Override
  public TypeParameter visitTypeParameter(TypeParameter node) {
    return new TypeParameter(
        cloneNode(node.getDocumentationComment()),
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getName()),
        node.getKeyword(),
        cloneNode(node.getBound()));
  }

  @Override
  public TypeParameterList visitTypeParameterList(TypeParameterList node) {
    return new TypeParameterList(
        node.getLeftBracket(),
        cloneNodeList(node.getTypeParameters()),
        node.getRightBracket());
  }

  @Override
  public VariableDeclaration visitVariableDeclaration(VariableDeclaration node) {
    return new VariableDeclaration(
        null,
        cloneNodeList(node.getMetadata()),
        cloneNode(node.getName()),
        node.getEquals(),
        cloneNode(node.getInitializer()));
  }

  @Override
  public VariableDeclarationList visitVariableDeclarationList(VariableDeclarationList node) {
    return new VariableDeclarationList(
        null,
        cloneNodeList(node.getMetadata()),
        node.getKeyword(),
        cloneNode(node.getType()),
        cloneNodeList(node.getVariables()));
  }

  @Override
  public VariableDeclarationStatement visitVariableDeclarationStatement(
      VariableDeclarationStatement node) {
    return new VariableDeclarationStatement(cloneNode(node.getVariables()), node.getSemicolon());
  }

  @Override
  public WhileStatement visitWhileStatement(WhileStatement node) {
    return new WhileStatement(
        node.getKeyword(),
        node.getLeftParenthesis(),
        cloneNode(node.getCondition()),
        node.getRightParenthesis(),
        cloneNode(node.getBody()));
  }

  @Override
  public WithClause visitWithClause(WithClause node) {
    return new WithClause(node.getWithKeyword(), cloneNodeList(node.getMixinTypes()));
  }

  @Override
  public YieldStatement visitYieldStatement(YieldStatement node) {
    return new YieldStatement(
        node.getYieldKeyword(),
        node.getStar(),
        node.getExpression(),
        node.getSemicolon());
  }

  @SuppressWarnings("unchecked")
  protected <E extends AstNode> E cloneNode(E node) {
    if (node == null) {
      return null;
    }
    return (E) node.accept(this);
  }
}
