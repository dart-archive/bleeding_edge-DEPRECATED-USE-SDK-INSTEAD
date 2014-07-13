/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.*;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.ast.AstFactory.adjacentStrings;
import static com.google.dart.engine.ast.AstFactory.annotation;
import static com.google.dart.engine.ast.AstFactory.argumentList;
import static com.google.dart.engine.ast.AstFactory.asExpression;
import static com.google.dart.engine.ast.AstFactory.assertStatement;
import static com.google.dart.engine.ast.AstFactory.assignmentExpression;
import static com.google.dart.engine.ast.AstFactory.binaryExpression;
import static com.google.dart.engine.ast.AstFactory.block;
import static com.google.dart.engine.ast.AstFactory.blockFunctionBody;
import static com.google.dart.engine.ast.AstFactory.booleanLiteral;
import static com.google.dart.engine.ast.AstFactory.breakStatement;
import static com.google.dart.engine.ast.AstFactory.cascadeExpression;
import static com.google.dart.engine.ast.AstFactory.catchClause;
import static com.google.dart.engine.ast.AstFactory.classDeclaration;
import static com.google.dart.engine.ast.AstFactory.classTypeAlias;
import static com.google.dart.engine.ast.AstFactory.compilationUnit;
import static com.google.dart.engine.ast.AstFactory.conditionalExpression;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.constructorFieldInitializer;
import static com.google.dart.engine.ast.AstFactory.constructorName;
import static com.google.dart.engine.ast.AstFactory.continueStatement;
import static com.google.dart.engine.ast.AstFactory.declaredIdentifier;
import static com.google.dart.engine.ast.AstFactory.doStatement;
import static com.google.dart.engine.ast.AstFactory.emptyFunctionBody;
import static com.google.dart.engine.ast.AstFactory.emptyStatement;
import static com.google.dart.engine.ast.AstFactory.enumDeclaration;
import static com.google.dart.engine.ast.AstFactory.exportDirective;
import static com.google.dart.engine.ast.AstFactory.expressionFunctionBody;
import static com.google.dart.engine.ast.AstFactory.expressionStatement;
import static com.google.dart.engine.ast.AstFactory.extendsClause;
import static com.google.dart.engine.ast.AstFactory.fieldDeclaration;
import static com.google.dart.engine.ast.AstFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.AstFactory.forEachStatement;
import static com.google.dart.engine.ast.AstFactory.forStatement;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.functionDeclaration;
import static com.google.dart.engine.ast.AstFactory.functionDeclarationStatement;
import static com.google.dart.engine.ast.AstFactory.functionExpression;
import static com.google.dart.engine.ast.AstFactory.functionExpressionInvocation;
import static com.google.dart.engine.ast.AstFactory.functionTypedFormalParameter;
import static com.google.dart.engine.ast.AstFactory.hideCombinator;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.ifStatement;
import static com.google.dart.engine.ast.AstFactory.implementsClause;
import static com.google.dart.engine.ast.AstFactory.importDirective;
import static com.google.dart.engine.ast.AstFactory.indexExpression;
import static com.google.dart.engine.ast.AstFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.interpolationExpression;
import static com.google.dart.engine.ast.AstFactory.isExpression;
import static com.google.dart.engine.ast.AstFactory.label;
import static com.google.dart.engine.ast.AstFactory.labeledStatement;
import static com.google.dart.engine.ast.AstFactory.libraryDirective;
import static com.google.dart.engine.ast.AstFactory.libraryIdentifier;
import static com.google.dart.engine.ast.AstFactory.list;
import static com.google.dart.engine.ast.AstFactory.listLiteral;
import static com.google.dart.engine.ast.AstFactory.mapLiteral;
import static com.google.dart.engine.ast.AstFactory.mapLiteralEntry;
import static com.google.dart.engine.ast.AstFactory.methodDeclaration;
import static com.google.dart.engine.ast.AstFactory.methodInvocation;
import static com.google.dart.engine.ast.AstFactory.namedExpression;
import static com.google.dart.engine.ast.AstFactory.nativeClause;
import static com.google.dart.engine.ast.AstFactory.nativeFunctionBody;
import static com.google.dart.engine.ast.AstFactory.nullLiteral;
import static com.google.dart.engine.ast.AstFactory.parenthesizedExpression;
import static com.google.dart.engine.ast.AstFactory.partDirective;
import static com.google.dart.engine.ast.AstFactory.partOfDirective;
import static com.google.dart.engine.ast.AstFactory.positionalFormalParameter;
import static com.google.dart.engine.ast.AstFactory.postfixExpression;
import static com.google.dart.engine.ast.AstFactory.prefixExpression;
import static com.google.dart.engine.ast.AstFactory.propertyAccess;
import static com.google.dart.engine.ast.AstFactory.redirectingConstructorInvocation;
import static com.google.dart.engine.ast.AstFactory.returnStatement;
import static com.google.dart.engine.ast.AstFactory.showCombinator;
import static com.google.dart.engine.ast.AstFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.AstFactory.string;
import static com.google.dart.engine.ast.AstFactory.superConstructorInvocation;
import static com.google.dart.engine.ast.AstFactory.switchCase;
import static com.google.dart.engine.ast.AstFactory.switchDefault;
import static com.google.dart.engine.ast.AstFactory.switchStatement;
import static com.google.dart.engine.ast.AstFactory.throwExpression;
import static com.google.dart.engine.ast.AstFactory.topLevelVariableDeclaration;
import static com.google.dart.engine.ast.AstFactory.tryStatement;
import static com.google.dart.engine.ast.AstFactory.typeAlias;
import static com.google.dart.engine.ast.AstFactory.typeArgumentList;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.ast.AstFactory.typeParameter;
import static com.google.dart.engine.ast.AstFactory.typeParameterList;
import static com.google.dart.engine.ast.AstFactory.variableDeclaration;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationList;
import static com.google.dart.engine.ast.AstFactory.variableDeclarationStatement;
import static com.google.dart.engine.ast.AstFactory.whileStatement;
import static com.google.dart.engine.ast.AstFactory.withClause;

public class NodeReplacerTest extends EngineTestCase {
  private interface Getter<P, C> {
    public C get(P parent);
  }

  private static abstract class ListGetter<P extends AstNode, C extends AstNode> implements
      Getter<P, C> {
    private int index;

    public ListGetter(int index) {
      this.index = index;
    }

    @Override
    public C get(P parent) {
      NodeList<C> list = getList(parent);
      if (list.isEmpty()) {
        return null;
      }
      return list.get(index);
    }

    abstract protected NodeList<C> getList(P parent);
  }

  public void test_adjacentStrings() {
    AdjacentStrings node = adjacentStrings(string("a"), string("b"));

    assertReplace(node, new ListGetter<AdjacentStrings, StringLiteral>(0) {
      @Override
      protected NodeList<StringLiteral> getList(AdjacentStrings node) {
        return node.getStrings();
      }
    });
    assertReplace(node, new ListGetter<AdjacentStrings, StringLiteral>(1) {
      @Override
      protected NodeList<StringLiteral> getList(AdjacentStrings node) {
        return node.getStrings();
      }
    });
  }

  public void test_annotation() {
    Annotation node = annotation(identifier("C"), identifier("c"), argumentList(integer(0)));

    assertReplace(node, new Getter<Annotation, ArgumentList>() {
      @Override
      public ArgumentList get(Annotation node) {
        return node.getArguments();
      }
    });
    assertReplace(node, new Getter<Annotation, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(Annotation node) {
        return node.getConstructorName();
      }
    });
    assertReplace(node, new Getter<Annotation, Identifier>() {
      @Override
      public Identifier get(Annotation node) {
        return node.getName();
      }
    });
  }

  public void test_argumentList() {
    ArgumentList node = argumentList(integer(0));

    assertReplace(node, new ListGetter<ArgumentList, Expression>(0) {
      @Override
      protected NodeList<Expression> getList(ArgumentList node) {
        return node.getArguments();
      }
    });
  }

  public void test_asExpression() {
    AsExpression node = asExpression(integer(0), typeName(identifier("a"), typeName("C")));

    assertReplace(node, new Getter<AsExpression, Expression>() {
      @Override
      public Expression get(AsExpression node) {
        return node.getExpression();
      }
    });
    assertReplace(node, new Getter<AsExpression, TypeName>() {
      @Override
      public TypeName get(AsExpression node) {
        return node.getType();
      }
    });
  }

  public void test_assertStatement() {
    AssertStatement node = assertStatement(booleanLiteral(true));

    assertReplace(node, new Getter<AssertStatement, Expression>() {
      @Override
      public Expression get(AssertStatement node) {
        return node.getCondition();
      }
    });
  }

  public void test_assignmentExpression() {
    AssignmentExpression node = assignmentExpression(identifier("l"), TokenType.EQ, identifier("r"));

    assertReplace(node, new Getter<AssignmentExpression, Expression>() {
      @Override
      public Expression get(AssignmentExpression node) {
        return node.getLeftHandSide();
      }
    });
    assertReplace(node, new Getter<AssignmentExpression, Expression>() {
      @Override
      public Expression get(AssignmentExpression node) {
        return node.getRightHandSide();
      }
    });
  }

  public void test_binaryExpression() {
    BinaryExpression node = binaryExpression(identifier("l"), TokenType.PLUS, identifier("r"));

    assertReplace(node, new Getter<BinaryExpression, Expression>() {
      @Override
      public Expression get(BinaryExpression node) {
        return node.getLeftOperand();
      }
    });
    assertReplace(node, new Getter<BinaryExpression, Expression>() {
      @Override
      public Expression get(BinaryExpression node) {
        return node.getRightOperand();
      }
    });
  }

  public void test_block() {
    Block node = block(emptyStatement());

    assertReplace(node, new ListGetter<Block, Statement>(0) {
      @Override
      protected NodeList<Statement> getList(Block node) {
        return node.getStatements();
      }
    });
  }

  public void test_blockFunctionBody() {
    BlockFunctionBody node = blockFunctionBody(block());

    assertReplace(node, new Getter<BlockFunctionBody, Block>() {
      @Override
      public Block get(BlockFunctionBody node) {
        return node.getBlock();
      }
    });
  }

  public void test_breakStatement() {
    BreakStatement node = breakStatement("l");

    assertReplace(node, new Getter<BreakStatement, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(BreakStatement node) {
        return node.getLabel();
      }
    });
  }

  public void test_cascadeExpression() {
    CascadeExpression node = cascadeExpression(integer(0), propertyAccess(null, identifier("b")));

    assertReplace(node, new Getter<CascadeExpression, Expression>() {
      @Override
      public Expression get(CascadeExpression node) {
        return node.getTarget();
      }
    });
    assertReplace(node, new ListGetter<CascadeExpression, Expression>(0) {
      @Override
      protected NodeList<Expression> getList(CascadeExpression node) {
        return node.getCascadeSections();
      }
    });
  }

  public void test_catchClause() {
    CatchClause node = catchClause(typeName("E"), "e", "s", emptyStatement());

    assertReplace(node, new Getter<CatchClause, TypeName>() {
      @Override
      public TypeName get(CatchClause node) {
        return node.getExceptionType();
      }
    });
    assertReplace(node, new Getter<CatchClause, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(CatchClause node) {
        return node.getExceptionParameter();
      }
    });
    assertReplace(node, new Getter<CatchClause, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(CatchClause node) {
        return node.getStackTraceParameter();
      }
    });
  }

  public void test_classDeclaration() {
    ClassDeclaration node = classDeclaration(
        null,
        "A",
        typeParameterList("E"),
        extendsClause(typeName("B")),
        withClause(typeName("C")),
        implementsClause(typeName("D")),
        fieldDeclaration(false, null, variableDeclaration("f")));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));
    node.setNativeClause(nativeClause(""));

    assertReplace(node, new Getter<ClassDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ClassDeclaration node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<ClassDeclaration, TypeParameterList>() {
      @Override
      public TypeParameterList get(ClassDeclaration node) {
        return node.getTypeParameters();
      }
    });
    assertReplace(node, new Getter<ClassDeclaration, ExtendsClause>() {
      @Override
      public ExtendsClause get(ClassDeclaration node) {
        return node.getExtendsClause();
      }
    });
    assertReplace(node, new Getter<ClassDeclaration, WithClause>() {
      @Override
      public WithClause get(ClassDeclaration node) {
        return node.getWithClause();
      }
    });
    assertReplace(node, new Getter<ClassDeclaration, ImplementsClause>() {
      @Override
      public ImplementsClause get(ClassDeclaration node) {
        return node.getImplementsClause();
      }
    });
    assertReplace(node, new Getter<ClassDeclaration, NativeClause>() {
      @Override
      public NativeClause get(ClassDeclaration node) {
        return node.getNativeClause();
      }
    });
    assertReplace(node, new ListGetter<ClassDeclaration, ClassMember>(0) {
      @Override
      protected NodeList<ClassMember> getList(ClassDeclaration node) {
        return node.getMembers();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_classTypeAlias() {
    ClassTypeAlias node = classTypeAlias(
        "A",
        typeParameterList("E"),
        null,
        typeName("B"),
        withClause(typeName("C")),
        implementsClause(typeName("D")));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<ClassTypeAlias, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ClassTypeAlias node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<ClassTypeAlias, TypeParameterList>() {
      @Override
      public TypeParameterList get(ClassTypeAlias node) {
        return node.getTypeParameters();
      }
    });
    assertReplace(node, new Getter<ClassTypeAlias, TypeName>() {
      @Override
      public TypeName get(ClassTypeAlias node) {
        return node.getSuperclass();
      }
    });
    assertReplace(node, new Getter<ClassTypeAlias, WithClause>() {
      @Override
      public WithClause get(ClassTypeAlias node) {
        return node.getWithClause();
      }
    });
    assertReplace(node, new Getter<ClassTypeAlias, ImplementsClause>() {
      @Override
      public ImplementsClause get(ClassTypeAlias node) {
        return node.getImplementsClause();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_comment() {
    Comment node = Comment.createEndOfLineComment(new Token[0]);
    node.getReferences().add(new CommentReference(null, identifier("x")));

    assertReplace(node, new ListGetter<Comment, CommentReference>(0) {
      @Override
      protected NodeList<CommentReference> getList(Comment node) {
        return node.getReferences();
      }
    });
  }

  public void test_commentReference() {
    CommentReference node = new CommentReference(null, identifier("x"));

    assertReplace(node, new Getter<CommentReference, Identifier>() {
      @Override
      public Identifier get(CommentReference node) {
        return node.getIdentifier();
      }
    });
  }

  public void test_compilationUnit() {
    CompilationUnit node = compilationUnit(
        "",
        list((Directive) libraryDirective("lib")),
        list((CompilationUnitMember) topLevelVariableDeclaration(null, variableDeclaration("X"))));

    assertReplace(node, new Getter<CompilationUnit, ScriptTag>() {
      @Override
      public ScriptTag get(CompilationUnit node) {
        return node.getScriptTag();
      }
    });
    assertReplace(node, new ListGetter<CompilationUnit, Directive>(0) {
      @Override
      protected NodeList<Directive> getList(CompilationUnit node) {
        return node.getDirectives();
      }
    });
    assertReplace(node, new ListGetter<CompilationUnit, CompilationUnitMember>(0) {
      @Override
      protected NodeList<CompilationUnitMember> getList(CompilationUnit node) {
        return node.getDeclarations();
      }
    });
  }

  public void test_conditionalExpression() {
    ConditionalExpression node = conditionalExpression(booleanLiteral(true), integer(0), integer(1));

    assertReplace(node, new Getter<ConditionalExpression, Expression>() {
      @Override
      public Expression get(ConditionalExpression node) {
        return node.getCondition();
      }
    });
    assertReplace(node, new Getter<ConditionalExpression, Expression>() {
      @Override
      public Expression get(ConditionalExpression node) {
        return node.getThenExpression();
      }
    });
    assertReplace(node, new Getter<ConditionalExpression, Expression>() {
      @Override
      public Expression get(ConditionalExpression node) {
        return node.getElseExpression();
      }
    });
  }

  public void test_constructorDeclaration() {
    ConstructorDeclaration node = constructorDeclaration(
        null,
        null,
        identifier("C"),
        "d",
        formalParameterList(),
        list((ConstructorInitializer) constructorFieldInitializer(false, "x", integer(0))),
        emptyFunctionBody());
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));
    node.setRedirectedConstructor(constructorName(typeName("B"), "a"));

    assertReplace(node, new Getter<ConstructorDeclaration, Identifier>() {
      @Override
      public Identifier get(ConstructorDeclaration node) {
        return node.getReturnType();
      }
    });
    assertReplace(node, new Getter<ConstructorDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ConstructorDeclaration node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<ConstructorDeclaration, FormalParameterList>() {
      @Override
      public FormalParameterList get(ConstructorDeclaration node) {
        return node.getParameters();
      }
    });
    assertReplace(node, new Getter<ConstructorDeclaration, ConstructorName>() {
      @Override
      public ConstructorName get(ConstructorDeclaration node) {
        return node.getRedirectedConstructor();
      }
    });
    assertReplace(node, new Getter<ConstructorDeclaration, FunctionBody>() {
      @Override
      public FunctionBody get(ConstructorDeclaration node) {
        return node.getBody();
      }
    });
    assertReplace(node, new ListGetter<ConstructorDeclaration, ConstructorInitializer>(0) {
      @Override
      protected NodeList<ConstructorInitializer> getList(ConstructorDeclaration node) {
        return node.getInitializers();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_constructorFieldInitializer() {
    ConstructorFieldInitializer node = constructorFieldInitializer(false, "f", integer(0));

    assertReplace(node, new Getter<ConstructorFieldInitializer, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ConstructorFieldInitializer node) {
        return node.getFieldName();
      }
    });
    assertReplace(node, new Getter<ConstructorFieldInitializer, Expression>() {
      @Override
      public Expression get(ConstructorFieldInitializer node) {
        return node.getExpression();
      }
    });
  }

  public void test_constructorName() {
    ConstructorName node = constructorName(typeName("C"), "n");

    assertReplace(node, new Getter<ConstructorName, TypeName>() {
      @Override
      public TypeName get(ConstructorName node) {
        return node.getType();
      }
    });
    assertReplace(node, new Getter<ConstructorName, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ConstructorName node) {
        return node.getName();
      }
    });
  }

  public void test_continueStatement() {
    ContinueStatement node = continueStatement("l");

    assertReplace(node, new Getter<ContinueStatement, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ContinueStatement node) {
        return node.getLabel();
      }
    });
  }

  public void test_declaredIdentifier() {
    DeclaredIdentifier node = declaredIdentifier(typeName("C"), "i");
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<DeclaredIdentifier, TypeName>() {
      @Override
      public TypeName get(DeclaredIdentifier node) {
        return node.getType();
      }
    });
    assertReplace(node, new Getter<DeclaredIdentifier, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(DeclaredIdentifier node) {
        return node.getIdentifier();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_defaultFormalParameter() {
    DefaultFormalParameter node = positionalFormalParameter(simpleFormalParameter("p"), integer(0));

    assertReplace(node, new Getter<DefaultFormalParameter, NormalFormalParameter>() {
      @Override
      public NormalFormalParameter get(DefaultFormalParameter node) {
        return node.getParameter();
      }
    });
    assertReplace(node, new Getter<DefaultFormalParameter, Expression>() {
      @Override
      public Expression get(DefaultFormalParameter node) {
        return node.getDefaultValue();
      }
    });
  }

  public void test_doStatement() {
    DoStatement node = doStatement(block(), booleanLiteral(true));

    assertReplace(node, new Getter<DoStatement, Statement>() {
      @Override
      public Statement get(DoStatement node) {
        return node.getBody();
      }
    });
    assertReplace(node, new Getter<DoStatement, Expression>() {
      @Override
      public Expression get(DoStatement node) {
        return node.getCondition();
      }
    });
  }

  public void test_enumConstantDeclaration() {
    EnumConstantDeclaration node = new EnumConstantDeclaration(
        Comment.createEndOfLineComment(new Token[0]),
        list(annotation(identifier("a"))),
        identifier("C"));

    assertReplace(node, new Getter<EnumConstantDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(EnumConstantDeclaration node) {
        return node.getName();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_enumDeclaration() {
    EnumDeclaration node = enumDeclaration("E", "ONE", "TWO");
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<EnumDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(EnumDeclaration node) {
        return node.getName();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_exportDirective() {
    ExportDirective node = exportDirective("", hideCombinator("C"));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    testNamespaceDirective(node);
  }

  public void test_expressionFunctionBody() {
    ExpressionFunctionBody node = expressionFunctionBody(integer(0));

    assertReplace(node, new Getter<ExpressionFunctionBody, Expression>() {
      @Override
      public Expression get(ExpressionFunctionBody node) {
        return node.getExpression();
      }
    });
  }

  public void test_expressionStatement() {
    ExpressionStatement node = expressionStatement(integer(0));

    assertReplace(node, new Getter<ExpressionStatement, Expression>() {
      @Override
      public Expression get(ExpressionStatement node) {
        return node.getExpression();
      }
    });
  }

  public void test_extendsClause() {
    ExtendsClause node = extendsClause(typeName("S"));

    assertReplace(node, new Getter<ExtendsClause, TypeName>() {
      @Override
      public TypeName get(ExtendsClause node) {
        return node.getSuperclass();
      }
    });
  }

  public void test_fieldDeclaration() {
    FieldDeclaration node = fieldDeclaration(false, null, typeName("C"), variableDeclaration("c"));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<FieldDeclaration, VariableDeclarationList>() {
      @Override
      public VariableDeclarationList get(FieldDeclaration node) {
        return node.getFields();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_fieldFormalParameter() {
    FieldFormalParameter node = fieldFormalParameter(
        null,
        typeName("C"),
        "f",
        formalParameterList());
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<FieldFormalParameter, TypeName>() {
      @Override
      public TypeName get(FieldFormalParameter node) {
        return node.getType();
      }
    });
    assertReplace(node, new Getter<FieldFormalParameter, FormalParameterList>() {
      @Override
      public FormalParameterList get(FieldFormalParameter node) {
        return node.getParameters();
      }
    });
    testNormalFormalParameter(node);
  }

  public void test_forEachStatement_withIdentifier() {
    ForEachStatement node = forEachStatement(identifier("i"), identifier("l"), block());

    assertReplace(node, new Getter<ForEachStatement, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ForEachStatement node) {
        return node.getIdentifier();
      }
    });
    assertReplace(node, new Getter<ForEachStatement, Expression>() {
      @Override
      public Expression get(ForEachStatement node) {
        return node.getIterator();
      }
    });
    assertReplace(node, new Getter<ForEachStatement, Statement>() {
      @Override
      public Statement get(ForEachStatement node) {
        return node.getBody();
      }
    });
  }

  public void test_forEachStatement_withLoopVariable() {
    ForEachStatement node = forEachStatement(declaredIdentifier("e"), identifier("l"), block());

    assertReplace(node, new Getter<ForEachStatement, DeclaredIdentifier>() {
      @Override
      public DeclaredIdentifier get(ForEachStatement node) {
        return node.getLoopVariable();
      }
    });
    assertReplace(node, new Getter<ForEachStatement, Expression>() {
      @Override
      public Expression get(ForEachStatement node) {
        return node.getIterator();
      }
    });
    assertReplace(node, new Getter<ForEachStatement, Statement>() {
      @Override
      public Statement get(ForEachStatement node) {
        return node.getBody();
      }
    });
  }

  public void test_formalParameterList() {
    FormalParameterList node = formalParameterList(simpleFormalParameter("p"));

    assertReplace(node, new ListGetter<FormalParameterList, FormalParameter>(0) {
      @Override
      protected NodeList<FormalParameter> getList(FormalParameterList node) {
        return node.getParameters();
      }
    });
  }

  public void test_forStatement_withInitialization() {
    ForStatement node = forStatement(
        identifier("a"),
        booleanLiteral(true),
        list((Expression) integer(0)),
        block());

    assertReplace(node, new Getter<ForStatement, Expression>() {
      @Override
      public Expression get(ForStatement node) {
        return node.getInitialization();
      }
    });
    assertReplace(node, new Getter<ForStatement, Expression>() {
      @Override
      public Expression get(ForStatement node) {
        return node.getCondition();
      }
    });
    assertReplace(node, new Getter<ForStatement, Statement>() {
      @Override
      public Statement get(ForStatement node) {
        return node.getBody();
      }
    });
    assertReplace(node, new ListGetter<ForStatement, Expression>(0) {
      @Override
      protected NodeList<Expression> getList(ForStatement node) {
        return node.getUpdaters();
      }
    });
  }

  public void test_forStatement_withVariables() {
    ForStatement node = forStatement(
        variableDeclarationList(null, variableDeclaration("i")),
        booleanLiteral(true),
        list((Expression) integer(0)),
        block());

    assertReplace(node, new Getter<ForStatement, VariableDeclarationList>() {
      @Override
      public VariableDeclarationList get(ForStatement node) {
        return node.getVariables();
      }
    });
    assertReplace(node, new Getter<ForStatement, Expression>() {
      @Override
      public Expression get(ForStatement node) {
        return node.getCondition();
      }
    });
    assertReplace(node, new Getter<ForStatement, Statement>() {
      @Override
      public Statement get(ForStatement node) {
        return node.getBody();
      }
    });
    assertReplace(node, new ListGetter<ForStatement, Expression>(0) {
      @Override
      protected NodeList<Expression> getList(ForStatement node) {
        return node.getUpdaters();
      }
    });
  }

  public void test_functionDeclaration() {
    FunctionDeclaration node = functionDeclaration(
        typeName("R"),
        null,
        "f",
        functionExpression(formalParameterList(), blockFunctionBody(block())));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<FunctionDeclaration, TypeName>() {
      @Override
      public TypeName get(FunctionDeclaration node) {
        return node.getReturnType();
      }
    });
    assertReplace(node, new Getter<FunctionDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(FunctionDeclaration node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<FunctionDeclaration, FunctionExpression>() {
      @Override
      public FunctionExpression get(FunctionDeclaration node) {
        return node.getFunctionExpression();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_functionDeclarationStatement() {
    FunctionDeclarationStatement node = functionDeclarationStatement(
        typeName("R"),
        null,
        "f",
        functionExpression(formalParameterList(), blockFunctionBody(block())));

    assertReplace(node, new Getter<FunctionDeclarationStatement, FunctionDeclaration>() {
      @Override
      public FunctionDeclaration get(FunctionDeclarationStatement node) {
        return node.getFunctionDeclaration();
      }
    });
  }

  public void test_functionExpression() {
    FunctionExpression node = functionExpression(formalParameterList(), blockFunctionBody(block()));

    assertReplace(node, new Getter<FunctionExpression, FormalParameterList>() {
      @Override
      public FormalParameterList get(FunctionExpression node) {
        return node.getParameters();
      }
    });
    assertReplace(node, new Getter<FunctionExpression, FunctionBody>() {
      @Override
      public FunctionBody get(FunctionExpression node) {
        return node.getBody();
      }
    });
  }

  public void test_functionExpressionInvocation() {
    FunctionExpressionInvocation node = functionExpressionInvocation(identifier("f"), integer(0));

    assertReplace(node, new Getter<FunctionExpressionInvocation, Expression>() {
      @Override
      public Expression get(FunctionExpressionInvocation node) {
        return node.getFunction();
      }
    });
    assertReplace(node, new Getter<FunctionExpressionInvocation, ArgumentList>() {
      @Override
      public ArgumentList get(FunctionExpressionInvocation node) {
        return node.getArgumentList();
      }
    });
  }

  public void test_functionTypeAlias() {
    FunctionTypeAlias node = typeAlias(
        typeName("R"),
        "F",
        typeParameterList("E"),
        formalParameterList());
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<FunctionTypeAlias, TypeName>() {
      @Override
      public TypeName get(FunctionTypeAlias node) {
        return node.getReturnType();
      }
    });
    assertReplace(node, new Getter<FunctionTypeAlias, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(FunctionTypeAlias node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<FunctionTypeAlias, TypeParameterList>() {
      @Override
      public TypeParameterList get(FunctionTypeAlias node) {
        return node.getTypeParameters();
      }
    });
    assertReplace(node, new Getter<FunctionTypeAlias, FormalParameterList>() {
      @Override
      public FormalParameterList get(FunctionTypeAlias node) {
        return node.getParameters();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_functionTypedFormalParameter() {
    FunctionTypedFormalParameter node = functionTypedFormalParameter(
        typeName("R"),
        "f",
        simpleFormalParameter("p"));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<FunctionTypedFormalParameter, TypeName>() {
      @Override
      public TypeName get(FunctionTypedFormalParameter node) {
        return node.getReturnType();
      }
    });
    assertReplace(node, new Getter<FunctionTypedFormalParameter, FormalParameterList>() {
      @Override
      public FormalParameterList get(FunctionTypedFormalParameter node) {
        return node.getParameters();
      }
    });
    testNormalFormalParameter(node);
  }

  public void test_hideCombinator() {
    HideCombinator node = hideCombinator("A", "B");

    assertReplace(node, new ListGetter<HideCombinator, SimpleIdentifier>(0) {
      @Override
      protected NodeList<SimpleIdentifier> getList(HideCombinator node) {
        return node.getHiddenNames();
      }
    });
  }

  public void test_ifStatement() {
    IfStatement node = ifStatement(booleanLiteral(true), block(), block());

    assertReplace(node, new Getter<IfStatement, Expression>() {
      @Override
      public Expression get(IfStatement node) {
        return node.getCondition();
      }
    });
    assertReplace(node, new Getter<IfStatement, Statement>() {
      @Override
      public Statement get(IfStatement node) {
        return node.getThenStatement();
      }
    });
    assertReplace(node, new Getter<IfStatement, Statement>() {
      @Override
      public Statement get(IfStatement node) {
        return node.getElseStatement();
      }
    });
  }

  public void test_implementsClause() {
    ImplementsClause node = implementsClause(typeName("I"), typeName("J"));

    assertReplace(node, new ListGetter<ImplementsClause, TypeName>(0) {
      @Override
      protected NodeList<TypeName> getList(ImplementsClause node) {
        return node.getInterfaces();
      }
    });
  }

  public void test_importDirective() {
    ImportDirective node = importDirective("", "p", showCombinator("A"), hideCombinator("B"));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<ImportDirective, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(ImportDirective node) {
        return node.getPrefix();
      }
    });
    testNamespaceDirective(node);
  }

  public void test_indexExpression() {
    IndexExpression node = indexExpression(identifier("a"), identifier("i"));

    assertReplace(node, new Getter<IndexExpression, Expression>() {
      @Override
      public Expression get(IndexExpression node) {
        return node.getTarget();
      }
    });
    assertReplace(node, new Getter<IndexExpression, Expression>() {
      @Override
      public Expression get(IndexExpression node) {
        return node.getIndex();
      }
    });
  }

  public void test_instanceCreationExpression() {
    InstanceCreationExpression node = instanceCreationExpression(
        null,
        typeName("C"),
        "c",
        integer(2));

    assertReplace(node, new Getter<InstanceCreationExpression, ConstructorName>() {
      @Override
      public ConstructorName get(InstanceCreationExpression node) {
        return node.getConstructorName();
      }
    });
    assertReplace(node, new Getter<InstanceCreationExpression, ArgumentList>() {
      @Override
      public ArgumentList get(InstanceCreationExpression node) {
        return node.getArgumentList();
      }
    });
  }

  public void test_interpolationExpression() {
    InterpolationExpression node = interpolationExpression("x");

    assertReplace(node, new Getter<InterpolationExpression, Expression>() {
      @Override
      public Expression get(InterpolationExpression node) {
        return node.getExpression();
      }
    });
  }

  public void test_isExpression() {
    IsExpression node = isExpression(identifier("v"), false, typeName("T"));

    assertReplace(node, new Getter<IsExpression, Expression>() {
      @Override
      public Expression get(IsExpression node) {
        return node.getExpression();
      }
    });
    assertReplace(node, new Getter<IsExpression, TypeName>() {
      @Override
      public TypeName get(IsExpression node) {
        return node.getType();
      }
    });
  }

  public void test_label() {
    Label node = label("l");

    assertReplace(node, new Getter<Label, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(Label node) {
        return node.getLabel();
      }
    });
  }

  public void test_labeledStatement() {
    LabeledStatement node = labeledStatement(list(label("l")), block());

    assertReplace(node, new ListGetter<LabeledStatement, Label>(0) {
      @Override
      protected NodeList<Label> getList(LabeledStatement node) {
        return node.getLabels();
      }
    });
    assertReplace(node, new Getter<LabeledStatement, Statement>() {
      @Override
      public Statement get(LabeledStatement node) {
        return node.getStatement();
      }
    });
  }

  public void test_libraryDirective() {
    LibraryDirective node = libraryDirective("lib");
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<LibraryDirective, LibraryIdentifier>() {
      @Override
      public LibraryIdentifier get(LibraryDirective node) {
        return node.getName();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_libraryIdentifier() {
    LibraryIdentifier node = libraryIdentifier("lib");

    assertReplace(node, new ListGetter<LibraryIdentifier, SimpleIdentifier>(0) {
      @Override
      protected NodeList<SimpleIdentifier> getList(LibraryIdentifier node) {
        return node.getComponents();
      }
    });
  }

  public void test_listLiteral() {
    ListLiteral node = listLiteral(null, typeArgumentList(typeName("E")), identifier("e"));

    assertReplace(node, new ListGetter<ListLiteral, Expression>(0) {
      @Override
      protected NodeList<Expression> getList(ListLiteral node) {
        return node.getElements();
      }
    });
    testTypedLiteral(node);
  }

  public void test_mapLiteral() {
    MapLiteral node = mapLiteral(
        null,
        typeArgumentList(typeName("E")),
        mapLiteralEntry("k", identifier("v")));

    assertReplace(node, new ListGetter<MapLiteral, MapLiteralEntry>(0) {
      @Override
      protected NodeList<MapLiteralEntry> getList(MapLiteral node) {
        return node.getEntries();
      }
    });
    testTypedLiteral(node);
  }

  public void test_mapLiteralEntry() {
    MapLiteralEntry node = mapLiteralEntry("k", identifier("v"));

    assertReplace(node, new Getter<MapLiteralEntry, Expression>() {
      @Override
      public Expression get(MapLiteralEntry node) {
        return node.getKey();
      }
    });
    assertReplace(node, new Getter<MapLiteralEntry, Expression>() {
      @Override
      public Expression get(MapLiteralEntry node) {
        return node.getValue();
      }
    });
  }

  public void test_methodDeclaration() {
    MethodDeclaration node = methodDeclaration(
        null,
        typeName("A"),
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody(block()));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<MethodDeclaration, TypeName>() {
      @Override
      public TypeName get(MethodDeclaration node) {
        return node.getReturnType();
      }
    });
    assertReplace(node, new Getter<MethodDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(MethodDeclaration node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<MethodDeclaration, FormalParameterList>() {
      @Override
      public FormalParameterList get(MethodDeclaration node) {
        return node.getParameters();
      }
    });
    assertReplace(node, new Getter<MethodDeclaration, FunctionBody>() {
      @Override
      public FunctionBody get(MethodDeclaration node) {
        return node.getBody();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_methodInvocation() {
    MethodInvocation node = methodInvocation(identifier("t"), "m", integer(0));

    assertReplace(node, new Getter<MethodInvocation, Expression>() {
      @Override
      public Expression get(MethodInvocation node) {
        return node.getTarget();
      }
    });
    assertReplace(node, new Getter<MethodInvocation, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(MethodInvocation node) {
        return node.getMethodName();
      }
    });
    assertReplace(node, new Getter<MethodInvocation, ArgumentList>() {
      @Override
      public ArgumentList get(MethodInvocation node) {
        return node.getArgumentList();
      }
    });
  }

  public void test_namedExpression() {
    NamedExpression node = namedExpression("l", identifier("v"));

    assertReplace(node, new Getter<NamedExpression, Label>() {
      @Override
      public Label get(NamedExpression node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<NamedExpression, Expression>() {
      @Override
      public Expression get(NamedExpression node) {
        return node.getExpression();
      }
    });
  }

  public void test_nativeClause() {
    NativeClause node = nativeClause("");

    assertReplace(node, new Getter<NativeClause, StringLiteral>() {
      @Override
      public StringLiteral get(NativeClause node) {
        return node.getName();
      }
    });
  }

  public void test_nativeFunctionBody() {
    NativeFunctionBody node = nativeFunctionBody("m");

    assertReplace(node, new Getter<NativeFunctionBody, StringLiteral>() {
      @Override
      public StringLiteral get(NativeFunctionBody node) {
        return node.getStringLiteral();
      }
    });
  }

  public void test_parenthesizedExpression() {
    ParenthesizedExpression node = parenthesizedExpression(integer(0));

    assertReplace(node, new Getter<ParenthesizedExpression, Expression>() {
      @Override
      public Expression get(ParenthesizedExpression node) {
        return node.getExpression();
      }
    });
  }

  public void test_partDirective() {
    PartDirective node = partDirective("");
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    testUriBasedDirective(node);
  }

  public void test_partOfDirective() {
    PartOfDirective node = partOfDirective(libraryIdentifier("lib"));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<PartOfDirective, LibraryIdentifier>() {
      @Override
      public LibraryIdentifier get(PartOfDirective node) {
        return node.getLibraryName();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_postfixExpression() {
    PostfixExpression node = postfixExpression(identifier("x"), TokenType.MINUS_MINUS);

    assertReplace(node, new Getter<PostfixExpression, Expression>() {
      @Override
      public Expression get(PostfixExpression node) {
        return node.getOperand();
      }
    });
  }

  public void test_prefixedIdentifier() {
    PrefixedIdentifier node = identifier("a", "b");

    assertReplace(node, new Getter<PrefixedIdentifier, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(PrefixedIdentifier node) {
        return node.getPrefix();
      }
    });
    assertReplace(node, new Getter<PrefixedIdentifier, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(PrefixedIdentifier node) {
        return node.getIdentifier();
      }
    });
  }

  public void test_prefixExpression() {
    PrefixExpression node = prefixExpression(TokenType.PLUS_PLUS, identifier("y"));

    assertReplace(node, new Getter<PrefixExpression, Expression>() {
      @Override
      public Expression get(PrefixExpression node) {
        return node.getOperand();
      }
    });
  }

  public void test_propertyAccess() {
    PropertyAccess node = propertyAccess(identifier("x"), "y");

    assertReplace(node, new Getter<PropertyAccess, Expression>() {
      @Override
      public Expression get(PropertyAccess node) {
        return node.getTarget();
      }
    });
    assertReplace(node, new Getter<PropertyAccess, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(PropertyAccess node) {
        return node.getPropertyName();
      }
    });
  }

  public void test_redirectingConstructorInvocation() {
    RedirectingConstructorInvocation node = redirectingConstructorInvocation("c", integer(0));

    assertReplace(node, new Getter<RedirectingConstructorInvocation, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(RedirectingConstructorInvocation node) {
        return node.getConstructorName();
      }
    });
    assertReplace(node, new Getter<RedirectingConstructorInvocation, ArgumentList>() {
      @Override
      public ArgumentList get(RedirectingConstructorInvocation node) {
        return node.getArgumentList();
      }
    });
  }

  public void test_returnStatement() {
    ReturnStatement node = returnStatement(integer(0));

    assertReplace(node, new Getter<ReturnStatement, Expression>() {
      @Override
      public Expression get(ReturnStatement node) {
        return node.getExpression();
      }
    });
  }

  public void test_showCombinator() {
    ShowCombinator node = showCombinator("X", "Y");

    assertReplace(node, new ListGetter<ShowCombinator, SimpleIdentifier>(0) {
      @Override
      protected NodeList<SimpleIdentifier> getList(ShowCombinator node) {
        return node.getShownNames();
      }
    });
  }

  public void test_simpleFormalParameter() {
    SimpleFormalParameter node = simpleFormalParameter(typeName("T"), "p");
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<SimpleFormalParameter, TypeName>() {
      @Override
      public TypeName get(SimpleFormalParameter node) {
        return node.getType();
      }
    });
    testNormalFormalParameter(node);
  }

  public void test_stringInterpolation() {
    StringInterpolation node = string(interpolationExpression("a"));

    assertReplace(node, new ListGetter<StringInterpolation, InterpolationElement>(0) {
      @Override
      protected NodeList<InterpolationElement> getList(StringInterpolation node) {
        return node.getElements();
      }
    });
  }

  public void test_superConstructorInvocation() {
    SuperConstructorInvocation node = superConstructorInvocation("s", integer(1));

    assertReplace(node, new Getter<SuperConstructorInvocation, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(SuperConstructorInvocation node) {
        return node.getConstructorName();
      }
    });
    assertReplace(node, new Getter<SuperConstructorInvocation, ArgumentList>() {
      @Override
      public ArgumentList get(SuperConstructorInvocation node) {
        return node.getArgumentList();
      }
    });
  }

  public void test_switchCase() {
    SwitchCase node = switchCase(list(label("l")), integer(0), block());

    assertReplace(node, new Getter<SwitchCase, Expression>() {
      @Override
      public Expression get(SwitchCase node) {
        return node.getExpression();
      }
    });
    testSwitchMember(node);
  }

  public void test_switchDefault() {
    SwitchDefault node = switchDefault(list(label("l")), block());

    testSwitchMember(node);
  }

  public void test_switchStatement() {
    SwitchStatement node = switchStatement(
        identifier("x"),
        switchCase(list(label("l")), integer(0), block()),
        switchDefault(list(label("l")), block()));

    assertReplace(node, new Getter<SwitchStatement, Expression>() {
      @Override
      public Expression get(SwitchStatement node) {
        return node.getExpression();
      }
    });
    assertReplace(node, new ListGetter<SwitchStatement, SwitchMember>(0) {
      @Override
      protected NodeList<SwitchMember> getList(SwitchStatement node) {
        return node.getMembers();
      }
    });
  }

  public void test_throwExpression() {
    ThrowExpression node = throwExpression(identifier("e"));

    assertReplace(node, new Getter<ThrowExpression, Expression>() {
      @Override
      public Expression get(ThrowExpression node) {
        return node.getExpression();
      }
    });
  }

  public void test_topLevelVariableDeclaration() {
    TopLevelVariableDeclaration node = topLevelVariableDeclaration(
        null,
        typeName("T"),
        variableDeclaration("t"));
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<TopLevelVariableDeclaration, VariableDeclarationList>() {
      @Override
      public VariableDeclarationList get(TopLevelVariableDeclaration node) {
        return node.getVariables();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_tryStatement() {
    TryStatement node = tryStatement(block(), list(catchClause("e", block())), block());

    assertReplace(node, new Getter<TryStatement, Block>() {
      @Override
      public Block get(TryStatement node) {
        return node.getBody();
      }
    });
    assertReplace(node, new Getter<TryStatement, Block>() {
      @Override
      public Block get(TryStatement node) {
        return node.getFinallyBlock();
      }
    });
    assertReplace(node, new ListGetter<TryStatement, CatchClause>(0) {
      @Override
      protected NodeList<CatchClause> getList(TryStatement node) {
        return node.getCatchClauses();
      }
    });
  }

  public void test_typeArgumentList() {
    TypeArgumentList node = typeArgumentList(typeName("A"));

    assertReplace(node, new ListGetter<TypeArgumentList, TypeName>(0) {
      @Override
      protected NodeList<TypeName> getList(TypeArgumentList node) {
        return node.getArguments();
      }
    });
  }

  public void test_typeName() {
    TypeName node = typeName("T", typeName("E"), typeName("F"));

    assertReplace(node, new Getter<TypeName, Identifier>() {
      @Override
      public Identifier get(TypeName node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<TypeName, TypeArgumentList>() {
      @Override
      public TypeArgumentList get(TypeName node) {
        return node.getTypeArguments();
      }
    });
  }

  public void test_typeParameter() {
    TypeParameter node = typeParameter("E", typeName("B"));

    assertReplace(node, new Getter<TypeParameter, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(TypeParameter node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<TypeParameter, TypeName>() {
      @Override
      public TypeName get(TypeParameter node) {
        return node.getBound();
      }
    });
  }

  public void test_typeParameterList() {
    TypeParameterList node = typeParameterList("A", "B");

    assertReplace(node, new ListGetter<TypeParameterList, TypeParameter>(0) {
      @Override
      protected NodeList<TypeParameter> getList(TypeParameterList node) {
        return node.getTypeParameters();
      }
    });
  }

  public void test_variableDeclaration() {
    VariableDeclaration node = variableDeclaration("a", nullLiteral());
    node.setDocumentationComment(Comment.createEndOfLineComment(new Token[0]));
    node.setMetadata(list(annotation(identifier("a"))));

    assertReplace(node, new Getter<VariableDeclaration, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(VariableDeclaration node) {
        return node.getName();
      }
    });
    assertReplace(node, new Getter<VariableDeclaration, Expression>() {
      @Override
      public Expression get(VariableDeclaration node) {
        return node.getInitializer();
      }
    });
    testAnnotatedNode(node);
  }

  public void test_variableDeclarationList() {
    VariableDeclarationList node = variableDeclarationList(
        null,
        typeName("T"),
        variableDeclaration("a"));

    assertReplace(node, new Getter<VariableDeclarationList, TypeName>() {
      @Override
      public TypeName get(VariableDeclarationList node) {
        return node.getType();
      }
    });
    assertReplace(node, new ListGetter<VariableDeclarationList, VariableDeclaration>(0) {
      @Override
      protected NodeList<VariableDeclaration> getList(VariableDeclarationList node) {
        return node.getVariables();
      }
    });
  }

  public void test_variableDeclarationStatement() {
    VariableDeclarationStatement node = variableDeclarationStatement(
        null,
        typeName("T"),
        variableDeclaration("a"));

    assertReplace(node, new Getter<VariableDeclarationStatement, VariableDeclarationList>() {
      @Override
      public VariableDeclarationList get(VariableDeclarationStatement node) {
        return node.getVariables();
      }
    });
  }

  public void test_whileStatement() {
    WhileStatement node = whileStatement(booleanLiteral(true), block());

    assertReplace(node, new Getter<WhileStatement, Expression>() {
      @Override
      public Expression get(WhileStatement node) {
        return node.getCondition();
      }
    });
    assertReplace(node, new Getter<WhileStatement, Statement>() {
      @Override
      public Statement get(WhileStatement node) {
        return node.getBody();
      }
    });
  }

  public void test_withClause() {
    WithClause node = withClause(typeName("M"));

    assertReplace(node, new ListGetter<WithClause, TypeName>(0) {
      @Override
      protected NodeList<TypeName> getList(WithClause node) {
        return node.getMixinTypes();
      }
    });
  }

  private <P extends AstNode, C extends AstNode> void assertReplace(P parent, Getter<P, C> getter) {
    AstNode child = getter.get(parent);
    if (child != null) {
      AstNode clone = child.accept(new AstCloner());
      NodeReplacer.replace(child, clone);
      assertEquals(clone, getter.get(parent));
      assertEquals(parent, clone.getParent());
    }
  }

  private void testAnnotatedNode(AnnotatedNode node) {
    assertReplace(node, new Getter<AnnotatedNode, Comment>() {
      @Override
      public Comment get(AnnotatedNode node) {
        return node.getDocumentationComment();
      }
    });
    assertReplace(node, new ListGetter<AnnotatedNode, Annotation>(0) {
      @Override
      protected NodeList<Annotation> getList(AnnotatedNode node) {
        return node.getMetadata();
      }
    });
  }

  private void testNamespaceDirective(NamespaceDirective node) {
    assertReplace(node, new ListGetter<NamespaceDirective, Combinator>(0) {
      @Override
      protected NodeList<Combinator> getList(NamespaceDirective node) {
        return node.getCombinators();
      }
    });
    testUriBasedDirective(node);
  }

  private void testNormalFormalParameter(NormalFormalParameter node) {
    assertReplace(node, new Getter<NormalFormalParameter, Comment>() {
      @Override
      public Comment get(NormalFormalParameter node) {
        return node.getDocumentationComment();
      }
    });
    assertReplace(node, new Getter<NormalFormalParameter, SimpleIdentifier>() {
      @Override
      public SimpleIdentifier get(NormalFormalParameter node) {
        return node.getIdentifier();
      }
    });
    assertReplace(node, new ListGetter<NormalFormalParameter, Annotation>(0) {
      @Override
      protected NodeList<Annotation> getList(NormalFormalParameter node) {
        return node.getMetadata();
      }
    });
  }

  private void testSwitchMember(SwitchMember node) {
    assertReplace(node, new ListGetter<SwitchMember, Label>(0) {
      @Override
      protected NodeList<Label> getList(SwitchMember node) {
        return node.getLabels();
      }
    });
    assertReplace(node, new ListGetter<SwitchMember, Statement>(0) {
      @Override
      protected NodeList<Statement> getList(SwitchMember node) {
        return node.getStatements();
      }
    });
  }

  private void testTypedLiteral(TypedLiteral node) {
    assertReplace(node, new Getter<TypedLiteral, TypeArgumentList>() {
      @Override
      public TypeArgumentList get(TypedLiteral node) {
        return node.getTypeArguments();
      }
    });
  }

  private void testUriBasedDirective(UriBasedDirective node) {
    assertReplace(node, new Getter<UriBasedDirective, StringLiteral>() {
      @Override
      public StringLiteral get(UriBasedDirective node) {
        return node.getUri();
      }
    });
    testAnnotatedNode(node);
  }
}
