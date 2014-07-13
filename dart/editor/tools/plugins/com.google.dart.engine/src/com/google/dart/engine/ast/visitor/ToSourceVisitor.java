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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.*;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.utilities.dart.ParameterKind;

import java.io.PrintWriter;

/**
 * Instances of the class {@code ToSourceVisitor} write a source representation of a visited AST
 * node (and all of it's children) to a writer.
 * 
 * @coverage dart.engine.ast
 */
public class ToSourceVisitor implements AstVisitor<Void> {
  /**
   * The writer to which the source is to be written.
   */
  private PrintWriter writer;

  /**
   * Initialize a newly created visitor to write source code representing the visited nodes to the
   * given writer.
   * 
   * @param writer the writer to which the source is to be written
   */
  public ToSourceVisitor(PrintWriter writer) {
    this.writer = writer;
  }

  @Override
  public Void visitAdjacentStrings(AdjacentStrings node) {
    visitNodeListWithSeparator(node.getStrings(), " ");
    return null;
  }

  @Override
  public Void visitAnnotation(Annotation node) {
    writer.print('@');
    visitNode(node.getName());
    visitNodeWithPrefix(".", node.getConstructorName());
    visitNode(node.getArguments());
    return null;
  }

  @Override
  public Void visitArgumentList(ArgumentList node) {
    writer.print('(');
    visitNodeListWithSeparator(node.getArguments(), ", ");
    writer.print(')');
    return null;
  }

  @Override
  public Void visitAsExpression(AsExpression node) {
    visitNode(node.getExpression());
    writer.print(" as ");
    visitNode(node.getType());
    return null;
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    writer.print("assert (");
    visitNode(node.getCondition());
    writer.print(");");
    return null;
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    visitNode(node.getLeftHandSide());
    writer.print(' ');
    writer.print(node.getOperator().getLexeme());
    writer.print(' ');
    visitNode(node.getRightHandSide());
    return null;
  }

  @Override
  public Void visitAwaitExpression(AwaitExpression node) {
    writer.print("await ");
    visitNode(node.getExpression());
    writer.print(";"); // TODO (brianwilkerson) This line probably needs to be removed.
    return null;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    visitNode(node.getLeftOperand());
    writer.print(' ');
    writer.print(node.getOperator().getLexeme());
    writer.print(' ');
    visitNode(node.getRightOperand());
    return null;
  }

  @Override
  public Void visitBlock(Block node) {
    writer.print('{');
    visitNodeListWithSeparator(node.getStatements(), " ");
    writer.print('}');
    return null;
  }

  @Override
  public Void visitBlockFunctionBody(BlockFunctionBody node) {
    Token keyword = node.getKeyword();
    if (keyword != null) {
      writer.print(keyword.getLexeme());
      if (node.getStar() != null) {
        writer.print('*');
      }
      writer.print(' ');
    }
    visitNode(node.getBlock());
    return null;
  }

  @Override
  public Void visitBooleanLiteral(BooleanLiteral node) {
    writer.print(node.getLiteral().getLexeme());
    return null;
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    writer.print("break");
    visitNodeWithPrefix(" ", node.getLabel());
    writer.print(";");
    return null;
  }

  @Override
  public Void visitCascadeExpression(CascadeExpression node) {
    visitNode(node.getTarget());
    visitNodeList(node.getCascadeSections());
    return null;
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    visitNodeWithPrefix("on ", node.getExceptionType());
    if (node.getCatchKeyword() != null) {
      if (node.getExceptionType() != null) {
        writer.print(' ');
      }
      writer.print("catch (");
      visitNode(node.getExceptionParameter());
      visitNodeWithPrefix(", ", node.getStackTraceParameter());
      writer.print(") ");
    } else {
      writer.print(" ");
    }
    visitNode(node.getBody());
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitTokenWithSuffix(node.getAbstractKeyword(), " ");
    writer.print("class ");
    visitNode(node.getName());
    visitNode(node.getTypeParameters());
    visitNodeWithPrefix(" ", node.getExtendsClause());
    visitNodeWithPrefix(" ", node.getWithClause());
    visitNodeWithPrefix(" ", node.getImplementsClause());
    writer.print(" {");
    visitNodeListWithSeparator(node.getMembers(), " ");
    writer.print("}");
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    if (node.getAbstractKeyword() != null) {
      writer.print("abstract ");
    }
    writer.print("class ");
    visitNode(node.getName());
    visitNode(node.getTypeParameters());
    writer.print(" = ");
    visitNode(node.getSuperclass());
    visitNodeWithPrefix(" ", node.getWithClause());
    visitNodeWithPrefix(" ", node.getImplementsClause());
    writer.print(";");
    return null;
  }

  @Override
  public Void visitComment(Comment node) {
    // We don't print comments.
    return null;
  }

  @Override
  public Void visitCommentReference(CommentReference node) {
    // We don't print comment references.
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    ScriptTag scriptTag = node.getScriptTag();
    NodeList<Directive> directives = node.getDirectives();
    visitNode(scriptTag);
    String prefix = scriptTag == null ? "" : " ";
    visitNodeListWithSeparatorAndPrefix(prefix, directives, " ");
    prefix = scriptTag == null && directives.isEmpty() ? "" : " ";
    visitNodeListWithSeparatorAndPrefix(prefix, node.getDeclarations(), " ");
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    visitNode(node.getCondition());
    writer.print(" ? ");
    visitNode(node.getThenExpression());
    writer.print(" : ");
    visitNode(node.getElseExpression());
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitTokenWithSuffix(node.getExternalKeyword(), " ");
    visitTokenWithSuffix(node.getConstKeyword(), " ");
    visitTokenWithSuffix(node.getFactoryKeyword(), " ");
    visitNode(node.getReturnType());
    visitNodeWithPrefix(".", node.getName());
    visitNode(node.getParameters());
    visitNodeListWithSeparatorAndPrefix(" : ", node.getInitializers(), ", ");
    visitNodeWithPrefix(" = ", node.getRedirectedConstructor());
    visitFunctionWithPrefix(" ", node.getBody());
    return null;
  }

  @Override
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    visitTokenWithSuffix(node.getKeyword(), ".");
    visitNode(node.getFieldName());
    writer.print(" = ");
    visitNode(node.getExpression());
    return null;
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    visitNode(node.getType());
    visitNodeWithPrefix(".", node.getName());
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    writer.print("continue");
    visitNodeWithPrefix(" ", node.getLabel());
    writer.print(";");
    return null;
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitTokenWithSuffix(node.getKeyword(), " ");
    visitNodeWithSuffix(node.getType(), " ");
    visitNode(node.getIdentifier());
    return null;
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    visitNode(node.getParameter());
    if (node.getSeparator() != null) {
      writer.print(" ");
      writer.print(node.getSeparator().getLexeme());
      visitNodeWithPrefix(" ", node.getDefaultValue());
    }
    return null;
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    writer.print("do ");
    visitNode(node.getBody());
    writer.print(" while (");
    visitNode(node.getCondition());
    writer.print(");");
    return null;
  }

  @Override
  public Void visitDoubleLiteral(DoubleLiteral node) {
    writer.print(node.getLiteral().getLexeme());
    return null;
  }

  @Override
  public Void visitEmptyFunctionBody(EmptyFunctionBody node) {
    writer.print(';');
    return null;
  }

  @Override
  public Void visitEmptyStatement(EmptyStatement node) {
    writer.print(';');
    return null;
  }

  @Override
  public Void visitEnumConstantDeclaration(EnumConstantDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitNode(node.getName());
    return null;
  }

  @Override
  public Void visitEnumDeclaration(EnumDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("enum ");
    visitNode(node.getName());
    writer.print(" {");
    visitNodeListWithSeparator(node.getConstants(), ", ");
    writer.print("}");
    return null;
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("export ");
    visitNode(node.getUri());
    visitNodeListWithSeparatorAndPrefix(" ", node.getCombinators(), " ");
    writer.print(';');
    return null;
  }

  @Override
  public Void visitExpressionFunctionBody(ExpressionFunctionBody node) {
    Token keyword = node.getKeyword();
    if (keyword != null) {
      writer.print(keyword.getLexeme());
      writer.print(' ');
    }
    writer.print("=> ");
    visitNode(node.getExpression());
    if (node.getSemicolon() != null) {
      writer.print(';');
    }
    return null;
  }

  @Override
  public Void visitExpressionStatement(ExpressionStatement node) {
    visitNode(node.getExpression());
    writer.print(';');
    return null;
  }

  @Override
  public Void visitExtendsClause(ExtendsClause node) {
    writer.print("extends ");
    visitNode(node.getSuperclass());
    return null;
  }

  @Override
  public Void visitFieldDeclaration(FieldDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitTokenWithSuffix(node.getStaticKeyword(), " ");
    visitNode(node.getFields());
    writer.print(";");
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    visitTokenWithSuffix(node.getKeyword(), " ");
    visitNodeWithSuffix(node.getType(), " ");
    writer.print("this.");
    visitNode(node.getIdentifier());
    visitNode(node.getParameters());
    return null;
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    DeclaredIdentifier loopVariable = node.getLoopVariable();
    if (node.getAwaitKeyword() != null) {
      writer.print("await ");
    }
    writer.print("for (");
    if (loopVariable == null) {
      visitNode(node.getIdentifier());
    } else {
      visitNode(loopVariable);
    }
    writer.print(" in ");
    visitNode(node.getIterator());
    writer.print(") ");
    visitNode(node.getBody());
    return null;
  }

  @Override
  public Void visitFormalParameterList(FormalParameterList node) {
    String groupEnd = null;
    writer.print('(');
    NodeList<FormalParameter> parameters = node.getParameters();
    int size = parameters.size();
    for (int i = 0; i < size; i++) {
      FormalParameter parameter = parameters.get(i);
      if (i > 0) {
        writer.print(", ");
      }
      if (groupEnd == null && parameter instanceof DefaultFormalParameter) {
        if (parameter.getKind() == ParameterKind.NAMED) {
          groupEnd = "}";
          writer.print('{');
        } else {
          groupEnd = "]";
          writer.print('[');
        }
      }
      parameter.accept(this);
    }
    if (groupEnd != null) {
      writer.print(groupEnd);
    }
    writer.print(')');
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    Expression initialization = node.getInitialization();
    writer.print("for (");
    if (initialization != null) {
      visitNode(initialization);
    } else {
      visitNode(node.getVariables());
    }
    writer.print(";");
    visitNodeWithPrefix(" ", node.getCondition());
    writer.print(";");
    visitNodeListWithSeparatorAndPrefix(" ", node.getUpdaters(), ", ");
    writer.print(") ");
    visitNode(node.getBody());
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitNodeWithSuffix(node.getReturnType(), " ");
    visitTokenWithSuffix(node.getPropertyKeyword(), " ");
    visitNode(node.getName());
    visitNode(node.getFunctionExpression());
    return null;
  }

  @Override
  public Void visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    visitNode(node.getFunctionDeclaration());
    writer.print(';');
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    visitNode(node.getParameters());
    writer.print(' ');
    visitNode(node.getBody());
    return null;
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    visitNode(node.getFunction());
    visitNode(node.getArgumentList());
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("typedef ");
    visitNodeWithSuffix(node.getReturnType(), " ");
    visitNode(node.getName());
    visitNode(node.getTypeParameters());
    visitNode(node.getParameters());
    writer.print(";");
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    visitNodeWithSuffix(node.getReturnType(), " ");
    visitNode(node.getIdentifier());
    visitNode(node.getParameters());
    return null;
  }

  @Override
  public Void visitHideCombinator(HideCombinator node) {
    writer.print("hide ");
    visitNodeListWithSeparator(node.getHiddenNames(), ", ");
    return null;
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    writer.print("if (");
    visitNode(node.getCondition());
    writer.print(") ");
    visitNode(node.getThenStatement());
    visitNodeWithPrefix(" else ", node.getElseStatement());
    return null;
  }

  @Override
  public Void visitImplementsClause(ImplementsClause node) {
    writer.print("implements ");
    visitNodeListWithSeparator(node.getInterfaces(), ", ");
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("import ");
    visitNode(node.getUri());
    if (node.getDeferredToken() != null) {
      writer.print(" deferred");
    }
    visitNodeWithPrefix(" as ", node.getPrefix());
    visitNodeListWithSeparatorAndPrefix(" ", node.getCombinators(), " ");
    writer.print(';');
    return null;
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    if (node.isCascaded()) {
      writer.print("..");
    } else {
      visitNode(node.getTarget());
    }
    writer.print('[');
    visitNode(node.getIndex());
    writer.print(']');
    return null;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    visitTokenWithSuffix(node.getKeyword(), " ");
    visitNode(node.getConstructorName());
    visitNode(node.getArgumentList());
    return null;
  }

  @Override
  public Void visitIntegerLiteral(IntegerLiteral node) {
    writer.print(node.getLiteral().getLexeme());
    return null;
  }

  @Override
  public Void visitInterpolationExpression(InterpolationExpression node) {
    if (node.getRightBracket() != null) {
      writer.print("${");
      visitNode(node.getExpression());
      writer.print("}");
    } else {
      writer.print("$");
      visitNode(node.getExpression());
    }
    return null;
  }

  @Override
  public Void visitInterpolationString(InterpolationString node) {
    writer.print(node.getContents().getLexeme());
    return null;
  }

  @Override
  public Void visitIsExpression(IsExpression node) {
    visitNode(node.getExpression());
    if (node.getNotOperator() == null) {
      writer.print(" is ");
    } else {
      writer.print(" is! ");
    }
    visitNode(node.getType());
    return null;
  }

  @Override
  public Void visitLabel(Label node) {
    visitNode(node.getLabel());
    writer.print(":");
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    visitNodeListWithSeparatorAndSuffix(node.getLabels(), " ", " ");
    visitNode(node.getStatement());
    return null;
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("library ");
    visitNode(node.getName());
    writer.print(';');
    return null;
  }

  @Override
  public Void visitLibraryIdentifier(LibraryIdentifier node) {
    writer.print(node.getName());
    return null;
  }

  @Override
  public Void visitListLiteral(ListLiteral node) {
    if (node.getConstKeyword() != null) {
      writer.print(node.getConstKeyword().getLexeme());
      writer.print(' ');
    }
    visitNodeWithSuffix(node.getTypeArguments(), " ");
    writer.print("[");
    visitNodeListWithSeparator(node.getElements(), ", ");
    writer.print("]");
    return null;
  }

  @Override
  public Void visitMapLiteral(MapLiteral node) {
    if (node.getConstKeyword() != null) {
      writer.print(node.getConstKeyword().getLexeme());
      writer.print(' ');
    }
    visitNodeWithSuffix(node.getTypeArguments(), " ");
    writer.print("{");
    visitNodeListWithSeparator(node.getEntries(), ", ");
    writer.print("}");
    return null;
  }

  @Override
  public Void visitMapLiteralEntry(MapLiteralEntry node) {
    visitNode(node.getKey());
    writer.print(" : ");
    visitNode(node.getValue());
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitTokenWithSuffix(node.getExternalKeyword(), " ");
    visitTokenWithSuffix(node.getModifierKeyword(), " ");
    visitNodeWithSuffix(node.getReturnType(), " ");
    visitTokenWithSuffix(node.getPropertyKeyword(), " ");
    visitTokenWithSuffix(node.getOperatorKeyword(), " ");
    visitNode(node.getName());
    if (!node.isGetter()) {
      visitNode(node.getParameters());
    }
    visitFunctionWithPrefix(" ", node.getBody());
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    if (node.isCascaded()) {
      writer.print("..");
    } else {
      visitNodeWithSuffix(node.getTarget(), ".");
    }
    visitNode(node.getMethodName());
    visitNode(node.getArgumentList());
    return null;
  }

  @Override
  public Void visitNamedExpression(NamedExpression node) {
    visitNode(node.getName());
    visitNodeWithPrefix(" ", node.getExpression());
    return null;
  }

  @Override
  public Void visitNativeClause(NativeClause node) {
    writer.print("native ");
    visitNode(node.getName());
    return null;
  }

  @Override
  public Void visitNativeFunctionBody(NativeFunctionBody node) {
    writer.print("native ");
    visitNode(node.getStringLiteral());
    writer.print(';');
    return null;
  }

  @Override
  public Void visitNullLiteral(NullLiteral node) {
    writer.print("null");
    return null;
  }

  @Override
  public Void visitParenthesizedExpression(ParenthesizedExpression node) {
    writer.print('(');
    visitNode(node.getExpression());
    writer.print(')');
    return null;
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("part ");
    visitNode(node.getUri());
    writer.print(';');
    return null;
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    writer.print("part of ");
    visitNode(node.getLibraryName());
    writer.print(';');
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    visitNode(node.getOperand());
    writer.print(node.getOperator().getLexeme());
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    visitNode(node.getPrefix());
    writer.print('.');
    visitNode(node.getIdentifier());
    return null;
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    writer.print(node.getOperator().getLexeme());
    visitNode(node.getOperand());
    return null;
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    if (node.isCascaded()) {
      writer.print("..");
    } else {
      visitNode(node.getTarget());
      writer.print('.');
    }
    visitNode(node.getPropertyName());
    return null;
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    writer.print("this");
    visitNodeWithPrefix(".", node.getConstructorName());
    visitNode(node.getArgumentList());
    return null;
  }

  @Override
  public Void visitRethrowExpression(RethrowExpression node) {
    writer.print("rethrow");
    return null;
  }

  @Override
  public Void visitReturnStatement(ReturnStatement node) {
    Expression expression = node.getExpression();
    if (expression == null) {
      writer.print("return;");
    } else {
      writer.print("return ");
      expression.accept(this);
      writer.print(";");
    }
    return null;
  }

  @Override
  public Void visitScriptTag(ScriptTag node) {
    writer.print(node.getScriptTag().getLexeme());
    return null;
  }

  @Override
  public Void visitShowCombinator(ShowCombinator node) {
    writer.print("show ");
    visitNodeListWithSeparator(node.getShownNames(), ", ");
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    visitTokenWithSuffix(node.getKeyword(), " ");
    visitNodeWithSuffix(node.getType(), " ");
    visitNode(node.getIdentifier());
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    writer.print(node.getToken().getLexeme());
    return null;
  }

  @Override
  public Void visitSimpleStringLiteral(SimpleStringLiteral node) {
    writer.print(node.getLiteral().getLexeme());
    return null;
  }

  @Override
  public Void visitStringInterpolation(StringInterpolation node) {
    visitNodeList(node.getElements());
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    writer.print("super");
    visitNodeWithPrefix(".", node.getConstructorName());
    visitNode(node.getArgumentList());
    return null;
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    writer.print("super");
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    visitNodeListWithSeparatorAndSuffix(node.getLabels(), " ", " ");
    writer.print("case ");
    visitNode(node.getExpression());
    writer.print(": ");
    visitNodeListWithSeparator(node.getStatements(), " ");
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    visitNodeListWithSeparatorAndSuffix(node.getLabels(), " ", " ");
    writer.print("default: ");
    visitNodeListWithSeparator(node.getStatements(), " ");
    return null;
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    writer.print("switch (");
    visitNode(node.getExpression());
    writer.print(") {");
    visitNodeListWithSeparator(node.getMembers(), " ");
    writer.print("}");
    return null;
  }

  @Override
  public Void visitSymbolLiteral(SymbolLiteral node) {
    writer.print("#");
    Token[] components = node.getComponents();
    for (int i = 0; i < components.length; i++) {
      if (i > 0) {
        writer.print(".");
      }
      writer.print(components[i].getLexeme());
    }
    return null;
  }

  @Override
  public Void visitThisExpression(ThisExpression node) {
    writer.print("this");
    return null;
  }

  @Override
  public Void visitThrowExpression(ThrowExpression node) {
    writer.print("throw ");
    visitNode(node.getExpression());
    return null;
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    visitNodeWithSuffix(node.getVariables(), ";");
    return null;
  }

  @Override
  public Void visitTryStatement(TryStatement node) {
    writer.print("try ");
    visitNode(node.getBody());
    visitNodeListWithSeparatorAndPrefix(" ", node.getCatchClauses(), " ");
    visitNodeWithPrefix(" finally ", node.getFinallyBlock());
    return null;
  }

  @Override
  public Void visitTypeArgumentList(TypeArgumentList node) {
    writer.print('<');
    visitNodeListWithSeparator(node.getArguments(), ", ");
    writer.print('>');
    return null;
  }

  @Override
  public Void visitTypeName(TypeName node) {
    visitNode(node.getName());
    visitNode(node.getTypeArguments());
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitNode(node.getName());
    visitNodeWithPrefix(" extends ", node.getBound());
    return null;
  }

  @Override
  public Void visitTypeParameterList(TypeParameterList node) {
    writer.print('<');
    visitNodeListWithSeparator(node.getTypeParameters(), ", ");
    writer.print('>');
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitNode(node.getName());
    visitNodeWithPrefix(" = ", node.getInitializer());
    return null;
  }

  @Override
  public Void visitVariableDeclarationList(VariableDeclarationList node) {
    visitNodeListWithSeparatorAndSuffix(node.getMetadata(), " ", " ");
    visitTokenWithSuffix(node.getKeyword(), " ");
    visitNodeWithSuffix(node.getType(), " ");
    visitNodeListWithSeparator(node.getVariables(), ", ");
    return null;
  }

  @Override
  public Void visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    visitNode(node.getVariables());
    writer.print(";");
    return null;
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    writer.print("while (");
    visitNode(node.getCondition());
    writer.print(") ");
    visitNode(node.getBody());
    return null;
  }

  @Override
  public Void visitWithClause(WithClause node) {
    writer.print("with ");
    visitNodeListWithSeparator(node.getMixinTypes(), ", ");
    return null;
  }

  @Override
  public Void visitYieldStatement(YieldStatement node) {
    if (node.getStar() != null) {
      writer.print("yield* ");
    } else {
      writer.print("yield ");
    }
    visitNode(node.getExpression());
    writer.print(";");
    return null;
  }

  /**
   * Visit the given function body, printing the prefix before if given body is not empty.
   * 
   * @param prefix the prefix to be printed if there is a node to visit
   * @param body the function body to be visited
   */
  private void visitFunctionWithPrefix(String prefix, FunctionBody body) {
    if (!(body instanceof EmptyFunctionBody)) {
      writer.print(prefix);
    }
    visitNode(body);
  }

  /**
   * Safely visit the given node.
   * 
   * @param node the node to be visited
   */
  private void visitNode(AstNode node) {
    if (node != null) {
      node.accept(this);
    }
  }

  /**
   * Print a list of nodes without any separation.
   * 
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   */
  private void visitNodeList(NodeList<? extends AstNode> nodes) {
    visitNodeListWithSeparator(nodes, "");
  }

  /**
   * Print a list of nodes, separated by the given separator.
   * 
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   */
  private void visitNodeListWithSeparator(NodeList<? extends AstNode> nodes, String separator) {
    if (nodes != null) {
      int size = nodes.size();
      for (int i = 0; i < size; i++) {
        if (i > 0) {
          writer.print(separator);
        }
        nodes.get(i).accept(this);
      }
    }
  }

  /**
   * Print a list of nodes, separated by the given separator.
   * 
   * @param prefix the prefix to be printed if the list is not empty
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   */
  private void visitNodeListWithSeparatorAndPrefix(String prefix,
      NodeList<? extends AstNode> nodes, String separator) {
    if (nodes != null) {
      int size = nodes.size();
      if (size > 0) {
        writer.print(prefix);
        for (int i = 0; i < size; i++) {
          if (i > 0) {
            writer.print(separator);
          }
          nodes.get(i).accept(this);
        }
      }
    }
  }

  /**
   * Print a list of nodes, separated by the given separator.
   * 
   * @param nodes the nodes to be printed
   * @param separator the separator to be printed between adjacent nodes
   * @param suffix the suffix to be printed if the list is not empty
   */
  private void visitNodeListWithSeparatorAndSuffix(NodeList<? extends AstNode> nodes,
      String separator, String suffix) {
    if (nodes != null) {
      int size = nodes.size();
      if (size > 0) {
        for (int i = 0; i < size; i++) {
          if (i > 0) {
            writer.print(separator);
          }
          nodes.get(i).accept(this);
        }
        writer.print(suffix);
      }
    }
  }

  /**
   * Safely visit the given node, printing the prefix before the node if it is non-{@code null}.
   * 
   * @param prefix the prefix to be printed if there is a node to visit
   * @param node the node to be visited
   */
  private void visitNodeWithPrefix(String prefix, AstNode node) {
    if (node != null) {
      writer.print(prefix);
      node.accept(this);
    }
  }

  /**
   * Safely visit the given node, printing the suffix after the node if it is non-{@code null}.
   * 
   * @param suffix the suffix to be printed if there is a node to visit
   * @param node the node to be visited
   */
  private void visitNodeWithSuffix(AstNode node, String suffix) {
    if (node != null) {
      node.accept(this);
      writer.print(suffix);
    }
  }

  /**
   * Safely visit the given node, printing the suffix after the node if it is non-{@code null}.
   * 
   * @param suffix the suffix to be printed if there is a node to visit
   * @param node the node to be visited
   */
  private void visitTokenWithSuffix(Token token, String suffix) {
    if (token != null) {
      writer.print(token.getLexeme());
      writer.print(suffix);
    }
  }
}
