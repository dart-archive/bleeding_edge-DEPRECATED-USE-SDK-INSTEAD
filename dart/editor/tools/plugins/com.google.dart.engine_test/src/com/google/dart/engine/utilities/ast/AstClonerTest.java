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
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.ast.AstFactory.*;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromKeyword;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromString;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromType;

public class AstClonerTest extends EngineTestCase {
  public void test_visitAdjacentStrings() {
    assertClone(adjacentStrings(string("a"), string("b")));
  }

  public void test_visitAnnotation_constant() {
    assertClone(annotation(identifier("A")));
  }

  public void test_visitAnnotation_constructor() {
    assertClone(annotation(identifier("A"), identifier("c"), argumentList()));
  }

  public void test_visitArgumentList() {
    assertClone(argumentList(identifier("a"), identifier("b")));
  }

  public void test_visitAsExpression() {
    assertClone(asExpression(identifier("e"), typeName("T")));
  }

  public void test_visitAssertStatement() {
    assertClone(assertStatement(identifier("a")));
  }

  public void test_visitAssignmentExpression() {
    assertClone(assignmentExpression(identifier("a"), TokenType.EQ, identifier("b")));
  }

  public void test_visitAwaitExpression() {
    assertClone(awaitExpression(identifier("a")));
  }

  public void test_visitBinaryExpression() {
    assertClone(binaryExpression(identifier("a"), TokenType.PLUS, identifier("b")));
  }

  public void test_visitBlock_empty() {
    assertClone(block());
  }

  public void test_visitBlock_nonEmpty() {
    assertClone(block(breakStatement(), breakStatement()));
  }

  public void test_visitBlockFunctionBody() {
    assertClone(blockFunctionBody());
  }

  public void test_visitBooleanLiteral_false() {
    assertClone(booleanLiteral(false));
  }

  public void test_visitBooleanLiteral_true() {
    assertClone(booleanLiteral(true));
  }

  public void test_visitBreakStatement_label() {
    assertClone(breakStatement("l"));
  }

  public void test_visitBreakStatement_noLabel() {
    assertClone(breakStatement());
  }

  public void test_visitCascadeExpression_field() {
    assertClone(cascadeExpression(
        identifier("a"),
        cascadedPropertyAccess("b"),
        cascadedPropertyAccess("c")));
  }

  public void test_visitCascadeExpression_index() {
    assertClone(cascadeExpression(
        identifier("a"),
        cascadedIndexExpression(integer(0L)),
        cascadedIndexExpression(integer(1L))));
  }

  public void test_visitCascadeExpression_method() {
    assertClone(cascadeExpression(
        identifier("a"),
        cascadedMethodInvocation("b"),
        cascadedMethodInvocation("c")));
  }

  public void test_visitCatchClause_catch_noStack() {
    assertClone(catchClause("e"));
  }

  public void test_visitCatchClause_catch_stack() {
    assertClone(catchClause("e", "s"));
  }

  public void test_visitCatchClause_on() {
    assertClone(catchClause(typeName("E")));
  }

  public void test_visitCatchClause_on_catch() {
    assertClone(catchClause(typeName("E"), "e"));
  }

  public void test_visitClassDeclaration_abstract() {
    assertClone(classDeclaration(Keyword.ABSTRACT, "C", null, null, null, null));
  }

  public void test_visitClassDeclaration_empty() {
    assertClone(classDeclaration(null, "C", null, null, null, null));
  }

  public void test_visitClassDeclaration_extends() {
    assertClone(classDeclaration(null, "C", null, extendsClause(typeName("A")), null, null));
  }

  public void test_visitClassDeclaration_extends_implements() {
    assertClone(classDeclaration(
        null,
        "C",
        null,
        extendsClause(typeName("A")),
        null,
        implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_extends_with() {
    assertClone(classDeclaration(
        null,
        "C",
        null,
        extendsClause(typeName("A")),
        withClause(typeName("M")),
        null));
  }

  public void test_visitClassDeclaration_extends_with_implements() {
    assertClone(classDeclaration(
        null,
        "C",
        null,
        extendsClause(typeName("A")),
        withClause(typeName("M")),
        implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_implements() {
    assertClone(classDeclaration(null, "C", null, null, null, implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_multipleMember() {
    assertClone(classDeclaration(
        null,
        "C",
        null,
        null,
        null,
        null,
        fieldDeclaration(false, Keyword.VAR, variableDeclaration("a")),
        fieldDeclaration(false, Keyword.VAR, variableDeclaration("b"))));
  }

  public void test_visitClassDeclaration_parameters() {
    assertClone(classDeclaration(null, "C", typeParameterList("E"), null, null, null));
  }

  public void test_visitClassDeclaration_parameters_extends() {
    assertClone(classDeclaration(
        null,
        "C",
        typeParameterList("E"),
        extendsClause(typeName("A")),
        null,
        null));
  }

  public void test_visitClassDeclaration_parameters_extends_implements() {
    assertClone(classDeclaration(
        null,
        "C",
        typeParameterList("E"),
        extendsClause(typeName("A")),
        null,
        implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_parameters_extends_with() {
    assertClone(classDeclaration(
        null,
        "C",
        typeParameterList("E"),
        extendsClause(typeName("A")),
        withClause(typeName("M")),
        null));
  }

  public void test_visitClassDeclaration_parameters_extends_with_implements() {
    assertClone(classDeclaration(
        null,
        "C",
        typeParameterList("E"),
        extendsClause(typeName("A")),
        withClause(typeName("M")),
        implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_parameters_implements() {
    assertClone(classDeclaration(
        null,
        "C",
        typeParameterList("E"),
        null,
        null,
        implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_singleMember() {
    assertClone(classDeclaration(
        null,
        "C",
        null,
        null,
        null,
        null,
        fieldDeclaration(false, Keyword.VAR, variableDeclaration("a"))));
  }

  public void test_visitClassDeclaration_withMetadata() {
    ClassDeclaration declaration = classDeclaration(null, "C", null, null, null, null);
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitClassTypeAlias_abstract() {
    assertClone(classTypeAlias(
        "C",
        null,
        Keyword.ABSTRACT,
        typeName("S"),
        withClause(typeName("M1")),
        null));
  }

  public void test_visitClassTypeAlias_abstract_implements() {
    assertClone(classTypeAlias(
        "C",
        null,
        Keyword.ABSTRACT,
        typeName("S"),
        withClause(typeName("M1")),
        implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_generic() {
    assertClone(classTypeAlias(
        "C",
        typeParameterList("E"),
        null,
        typeName("S", typeName("E")),
        withClause(typeName("M1", typeName("E"))),
        null));
  }

  public void test_visitClassTypeAlias_implements() {
    assertClone(classTypeAlias(
        "C",
        null,
        null,
        typeName("S"),
        withClause(typeName("M1")),
        implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_minimal() {
    assertClone(classTypeAlias("C", null, null, typeName("S"), withClause(typeName("M1")), null));
  }

  public void test_visitClassTypeAlias_parameters_abstract() {
    assertClone(classTypeAlias(
        "C",
        typeParameterList("E"),
        Keyword.ABSTRACT,
        typeName("S"),
        withClause(typeName("M1")),
        null));
  }

  public void test_visitClassTypeAlias_parameters_abstract_implements() {
    assertClone(classTypeAlias(
        "C",
        typeParameterList("E"),
        Keyword.ABSTRACT,
        typeName("S"),
        withClause(typeName("M1")),
        implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_parameters_implements() {
    assertClone(classTypeAlias(
        "C",
        typeParameterList("E"),
        null,
        typeName("S"),
        withClause(typeName("M1")),
        implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_withMetadata() {
    ClassTypeAlias declaration = classTypeAlias(
        "C",
        null,
        null,
        typeName("S"),
        withClause(typeName("M1")),
        null);
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitComment() {
    assertClone(Comment.createBlockComment(new Token[] {tokenFromString("/* comment */")}));
  }

  public void test_visitCommentReference() {
    assertClone(new CommentReference(null, identifier("a")));
  }

  public void test_visitCompilationUnit_declaration() {
    assertClone(compilationUnit(topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a"))));
  }

  public void test_visitCompilationUnit_directive() {
    assertClone(compilationUnit(libraryDirective("l")));
  }

  public void test_visitCompilationUnit_directive_declaration() {
    assertClone(compilationUnit(
        list((Directive) libraryDirective("l")),
        list((CompilationUnitMember) topLevelVariableDeclaration(
            Keyword.VAR,
            variableDeclaration("a")))));
  }

  public void test_visitCompilationUnit_empty() {
    assertClone(compilationUnit());
  }

  public void test_visitCompilationUnit_script() {
    assertClone(compilationUnit("!#/bin/dartvm"));
  }

  public void test_visitCompilationUnit_script_declaration() {
    assertClone(compilationUnit(
        "!#/bin/dartvm",
        topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a"))));
  }

  public void test_visitCompilationUnit_script_directive() {
    assertClone(compilationUnit("!#/bin/dartvm", libraryDirective("l")));
  }

  public void test_visitCompilationUnit_script_directives_declarations() {
    assertClone(compilationUnit(
        "!#/bin/dartvm",
        list((Directive) libraryDirective("l")),
        list((CompilationUnitMember) topLevelVariableDeclaration(
            Keyword.VAR,
            variableDeclaration("a")))));
  }

  public void test_visitConditionalExpression() {
    assertClone(conditionalExpression(identifier("a"), identifier("b"), identifier("c")));
  }

  public void test_visitConstructorDeclaration_const() {
    assertClone(constructorDeclaration(
        Keyword.CONST,
        null,
        identifier("C"),
        null,
        formalParameterList(),
        null,
        blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_external() {
    assertClone(constructorDeclaration(identifier("C"), null, formalParameterList(), null));
  }

  public void test_visitConstructorDeclaration_minimal() {
    assertClone(constructorDeclaration(
        null,
        null,
        identifier("C"),
        null,
        formalParameterList(),
        null,
        blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_multipleInitializers() {
    assertClone(constructorDeclaration(
        null,
        null,
        identifier("C"),
        null,
        formalParameterList(),
        list(
            (ConstructorInitializer) constructorFieldInitializer(false, "a", identifier("b")),
            constructorFieldInitializer(false, "c", identifier("d"))),
        blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_multipleParameters() {
    assertClone(constructorDeclaration(
        null,
        null,
        identifier("C"),
        null,
        formalParameterList(
            simpleFormalParameter(Keyword.VAR, "a"),
            simpleFormalParameter(Keyword.VAR, "b")),
        null,
        blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_named() {
    assertClone(constructorDeclaration(
        null,
        null,
        identifier("C"),
        "m",
        formalParameterList(),
        null,
        blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_singleInitializer() {
    assertClone(constructorDeclaration(
        null,
        null,
        identifier("C"),
        null,
        formalParameterList(),
        list((ConstructorInitializer) constructorFieldInitializer(false, "a", identifier("b"))),
        blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_withMetadata() {
    ConstructorDeclaration declaration = constructorDeclaration(
        null,
        null,
        identifier("C"),
        null,
        formalParameterList(),
        null,
        blockFunctionBody());
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitConstructorFieldInitializer_withoutThis() {
    assertClone(constructorFieldInitializer(false, "a", identifier("b")));
  }

  public void test_visitConstructorFieldInitializer_withThis() {
    assertClone(constructorFieldInitializer(true, "a", identifier("b")));
  }

  public void test_visitConstructorName_named_prefix() {
    assertClone(constructorName(typeName("p.C.n"), null));
  }

  public void test_visitConstructorName_unnamed_noPrefix() {
    assertClone(constructorName(typeName("C"), null));
  }

  public void test_visitConstructorName_unnamed_prefix() {
    assertClone(constructorName(typeName(identifier("p", "C")), null));
  }

  public void test_visitContinueStatement_label() {
    assertClone(continueStatement("l"));
  }

  public void test_visitContinueStatement_noLabel() {
    assertClone(continueStatement());
  }

  public void test_visitDefaultFormalParameter_named_noValue() {
    assertClone(namedFormalParameter(simpleFormalParameter("p"), null));
  }

  public void test_visitDefaultFormalParameter_named_value() {
    assertClone(namedFormalParameter(simpleFormalParameter("p"), integer(0)));
  }

  public void test_visitDefaultFormalParameter_positional_noValue() {
    assertClone(positionalFormalParameter(simpleFormalParameter("p"), null));
  }

  public void test_visitDefaultFormalParameter_positional_value() {
    assertClone(positionalFormalParameter(simpleFormalParameter("p"), integer(0)));
  }

  public void test_visitDoStatement() {
    assertClone(doStatement(block(), identifier("c")));
  }

  public void test_visitDoubleLiteral() {
    assertClone(doubleLiteral(4.2));
  }

  public void test_visitEmptyFunctionBody() {
    assertClone(emptyFunctionBody());
  }

  public void test_visitEmptyStatement() {
    assertClone(emptyStatement());
  }

  public void test_visitExportDirective_combinator() {
    assertClone(exportDirective("a.dart", (Combinator) showCombinator(identifier("A"))));
  }

  public void test_visitExportDirective_combinators() {
    assertClone(exportDirective(
        "a.dart",
        showCombinator(identifier("A")),
        hideCombinator(identifier("B"))));
  }

  public void test_visitExportDirective_minimal() {
    assertClone(exportDirective("a.dart"));
  }

  public void test_visitExportDirective_withMetadata() {
    ExportDirective directive = exportDirective("a.dart");
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(directive);
  }

  public void test_visitExpressionFunctionBody() {
    assertClone(expressionFunctionBody(identifier("a")));
  }

  public void test_visitExpressionStatement() {
    assertClone(expressionStatement(identifier("a")));
  }

  public void test_visitExtendsClause() {
    assertClone(extendsClause(typeName("C")));
  }

  public void test_visitFieldDeclaration_instance() {
    assertClone(fieldDeclaration(false, Keyword.VAR, variableDeclaration("a")));
  }

  public void test_visitFieldDeclaration_static() {
    assertClone(fieldDeclaration(true, Keyword.VAR, variableDeclaration("a")));
  }

  public void test_visitFieldDeclaration_withMetadata() {
    FieldDeclaration declaration = fieldDeclaration(false, Keyword.VAR, variableDeclaration("a"));
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitFieldFormalParameter_functionTyped() {
    assertClone(fieldFormalParameter(
        null,
        typeName("A"),
        "a",
        formalParameterList(simpleFormalParameter("b"))));
  }

  public void test_visitFieldFormalParameter_keyword() {
    assertClone(fieldFormalParameter(Keyword.VAR, null, "a"));
  }

  public void test_visitFieldFormalParameter_keywordAndType() {
    assertClone(fieldFormalParameter(Keyword.FINAL, typeName("A"), "a"));
  }

  public void test_visitFieldFormalParameter_type() {
    assertClone(fieldFormalParameter(null, typeName("A"), "a"));
  }

  public void test_visitForEachStatement_declared() {
    assertClone(forEachStatement(declaredIdentifier("a"), identifier("b"), block()));
  }

  public void test_visitForEachStatement_variable() {
    assertClone(new ForEachStatement(
        null,
        tokenFromKeyword(Keyword.FOR),
        tokenFromType(TokenType.OPEN_PAREN),
        identifier("a"),
        tokenFromKeyword(Keyword.IN),
        identifier("b"),
        tokenFromType(TokenType.CLOSE_PAREN),
        block()));
  }

  public void test_visitForEachStatement_variable_await() {
    assertClone(new ForEachStatement(
        tokenFromString("await"),
        tokenFromKeyword(Keyword.FOR),
        tokenFromType(TokenType.OPEN_PAREN),
        identifier("a"),
        tokenFromKeyword(Keyword.IN),
        identifier("b"),
        tokenFromType(TokenType.CLOSE_PAREN),
        block()));
  }

  public void test_visitFormalParameterList_empty() {
    assertClone(formalParameterList());
  }

  public void test_visitFormalParameterList_n() {
    assertClone(formalParameterList(namedFormalParameter(simpleFormalParameter("a"), integer(0L))));
  }

  public void test_visitFormalParameterList_nn() {
    assertClone(formalParameterList(
        namedFormalParameter(simpleFormalParameter("a"), integer(0L)),
        namedFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_p() {
    assertClone(formalParameterList(positionalFormalParameter(
        simpleFormalParameter("a"),
        integer(0L))));
  }

  public void test_visitFormalParameterList_pp() {
    assertClone(formalParameterList(
        positionalFormalParameter(simpleFormalParameter("a"), integer(0L)),
        positionalFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_r() {
    assertClone(formalParameterList(simpleFormalParameter("a")));
  }

  public void test_visitFormalParameterList_rn() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        namedFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_rnn() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        namedFormalParameter(simpleFormalParameter("b"), integer(1L)),
        namedFormalParameter(simpleFormalParameter("c"), integer(2L))));
  }

  public void test_visitFormalParameterList_rp() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        positionalFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_rpp() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        positionalFormalParameter(simpleFormalParameter("b"), integer(1L)),
        positionalFormalParameter(simpleFormalParameter("c"), integer(2L))));
  }

  public void test_visitFormalParameterList_rr() {
    assertClone(formalParameterList(simpleFormalParameter("a"), simpleFormalParameter("b")));
  }

  public void test_visitFormalParameterList_rrn() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        simpleFormalParameter("b"),
        namedFormalParameter(simpleFormalParameter("c"), integer(3L))));
  }

  public void test_visitFormalParameterList_rrnn() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        simpleFormalParameter("b"),
        namedFormalParameter(simpleFormalParameter("c"), integer(3L)),
        namedFormalParameter(simpleFormalParameter("d"), integer(4L))));
  }

  public void test_visitFormalParameterList_rrp() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        simpleFormalParameter("b"),
        positionalFormalParameter(simpleFormalParameter("c"), integer(3L))));
  }

  public void test_visitFormalParameterList_rrpp() {
    assertClone(formalParameterList(
        simpleFormalParameter("a"),
        simpleFormalParameter("b"),
        positionalFormalParameter(simpleFormalParameter("c"), integer(3L)),
        positionalFormalParameter(simpleFormalParameter("d"), integer(4L))));
  }

  public void test_visitForStatement_c() {
    assertClone(forStatement((Expression) null, identifier("c"), null, block()));
  }

  public void test_visitForStatement_cu() {
    assertClone(forStatement(
        (Expression) null,
        identifier("c"),
        list((Expression) identifier("u")),
        block()));
  }

  public void test_visitForStatement_e() {
    assertClone(forStatement(identifier("e"), null, null, block()));
  }

  public void test_visitForStatement_ec() {
    assertClone(forStatement(identifier("e"), identifier("c"), null, block()));
  }

  public void test_visitForStatement_ecu() {
    assertClone(forStatement(
        identifier("e"),
        identifier("c"),
        list((Expression) identifier("u")),
        block()));
  }

  public void test_visitForStatement_eu() {
    assertClone(forStatement(identifier("e"), null, list((Expression) identifier("u")), block()));
  }

  public void test_visitForStatement_i() {
    assertClone(forStatement(
        variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
        null,
        null,
        block()));
  }

  public void test_visitForStatement_ic() {
    assertClone(forStatement(
        variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
        identifier("c"),
        null,
        block()));
  }

  public void test_visitForStatement_icu() {
    assertClone(forStatement(
        variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
        identifier("c"),
        list((Expression) identifier("u")),
        block()));
  }

  public void test_visitForStatement_iu() {
    assertClone(forStatement(
        variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
        null,
        list((Expression) identifier("u")),
        block()));
  }

  public void test_visitForStatement_u() {
    assertClone(forStatement((Expression) null, null, list((Expression) identifier("u")), block()));
  }

  public void test_visitFunctionDeclaration_getter() {
    assertClone(functionDeclaration(null, Keyword.GET, "f", functionExpression()));
  }

  public void test_visitFunctionDeclaration_normal() {
    assertClone(functionDeclaration(null, null, "f", functionExpression()));
  }

  public void test_visitFunctionDeclaration_setter() {
    assertClone(functionDeclaration(null, Keyword.SET, "f", functionExpression()));
  }

  public void test_visitFunctionDeclaration_withMetadata() {
    FunctionDeclaration declaration = functionDeclaration(null, null, "f", functionExpression());
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitFunctionDeclarationStatement() {
    assertClone(functionDeclarationStatement(null, null, "f", functionExpression()));
  }

  public void test_visitFunctionExpression() {
    assertClone(functionExpression());
  }

  public void test_visitFunctionExpressionInvocation() {
    assertClone(functionExpressionInvocation(identifier("f")));
  }

  public void test_visitFunctionTypeAlias_generic() {
    assertClone(typeAlias(typeName("A"), "F", typeParameterList("B"), formalParameterList()));
  }

  public void test_visitFunctionTypeAlias_nonGeneric() {
    assertClone(typeAlias(typeName("A"), "F", null, formalParameterList()));
  }

  public void test_visitFunctionTypeAlias_withMetadata() {
    FunctionTypeAlias declaration = typeAlias(typeName("A"), "F", null, formalParameterList());
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitFunctionTypedFormalParameter_noType() {
    assertClone(functionTypedFormalParameter(null, "f"));
  }

  public void test_visitFunctionTypedFormalParameter_type() {
    assertClone(functionTypedFormalParameter(typeName("T"), "f"));
  }

  public void test_visitIfStatement_withElse() {
    assertClone(ifStatement(identifier("c"), block(), block()));
  }

  public void test_visitIfStatement_withoutElse() {
    assertClone(ifStatement(identifier("c"), block()));
  }

  public void test_visitImplementsClause_multiple() {
    assertClone(implementsClause(typeName("A"), typeName("B")));
  }

  public void test_visitImplementsClause_single() {
    assertClone(implementsClause(typeName("A")));
  }

  public void test_visitImportDirective_combinator() {
    assertClone(importDirective("a.dart", null, showCombinator(identifier("A"))));
  }

  public void test_visitImportDirective_combinators() {
    assertClone(importDirective(
        "a.dart",
        null,
        showCombinator(identifier("A")),
        hideCombinator(identifier("B"))));
  }

  public void test_visitImportDirective_minimal() {
    assertClone(importDirective("a.dart", null));
  }

  public void test_visitImportDirective_prefix() {
    assertClone(importDirective("a.dart", "p"));
  }

  public void test_visitImportDirective_prefix_combinator() {
    assertClone(importDirective("a.dart", "p", showCombinator(identifier("A"))));
  }

  public void test_visitImportDirective_prefix_combinators() {
    assertClone(importDirective(
        "a.dart",
        "p",
        showCombinator(identifier("A")),
        hideCombinator(identifier("B"))));
  }

  public void test_visitImportDirective_withMetadata() {
    ImportDirective directive = importDirective("a.dart", null);
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(directive);
  }

  public void test_visitImportHideCombinator_multiple() {
    assertClone(hideCombinator(identifier("a"), identifier("b")));
  }

  public void test_visitImportHideCombinator_single() {
    assertClone(hideCombinator(identifier("a")));
  }

  public void test_visitImportShowCombinator_multiple() {
    assertClone(showCombinator(identifier("a"), identifier("b")));
  }

  public void test_visitImportShowCombinator_single() {
    assertClone(showCombinator(identifier("a")));
  }

  public void test_visitIndexExpression() {
    assertClone(indexExpression(identifier("a"), identifier("i")));
  }

  public void test_visitInstanceCreationExpression_const() {
    assertClone(instanceCreationExpression(Keyword.CONST, typeName("C")));
  }

  public void test_visitInstanceCreationExpression_named() {
    assertClone(instanceCreationExpression(Keyword.NEW, typeName("C"), "c"));
  }

  public void test_visitInstanceCreationExpression_unnamed() {
    assertClone(instanceCreationExpression(Keyword.NEW, typeName("C")));
  }

  public void test_visitIntegerLiteral() {
    assertClone(integer(42L));
  }

  public void test_visitInterpolationExpression_expression() {
    assertClone(interpolationExpression(identifier("a")));
  }

  public void test_visitInterpolationExpression_identifier() {
    assertClone(interpolationExpression("a"));
  }

  public void test_visitInterpolationString() {
    assertClone(interpolationString("'x", "x"));
  }

  public void test_visitIsExpression_negated() {
    assertClone(isExpression(identifier("a"), true, typeName("C")));
  }

  public void test_visitIsExpression_normal() {
    assertClone(isExpression(identifier("a"), false, typeName("C")));
  }

  public void test_visitLabel() {
    assertClone(label("a"));
  }

  public void test_visitLabeledStatement_multiple() {
    assertClone(labeledStatement(list(label("a"), label("b")), returnStatement()));
  }

  public void test_visitLabeledStatement_single() {
    assertClone(labeledStatement(list(label("a")), returnStatement()));
  }

  public void test_visitLibraryDirective() {
    assertClone(libraryDirective("l"));
  }

  public void test_visitLibraryDirective_withMetadata() {
    LibraryDirective directive = libraryDirective("l");
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(directive);
  }

  public void test_visitLibraryIdentifier_multiple() {
    assertClone(libraryIdentifier(identifier("a"), identifier("b"), identifier("c")));
  }

  public void test_visitLibraryIdentifier_single() {
    assertClone(libraryIdentifier(identifier("a")));
  }

  public void test_visitListLiteral_const() {
    assertClone(listLiteral(Keyword.CONST, null));
  }

  public void test_visitListLiteral_empty() {
    assertClone(listLiteral());
  }

  public void test_visitListLiteral_nonEmpty() {
    assertClone(listLiteral(identifier("a"), identifier("b"), identifier("c")));
  }

  public void test_visitMapLiteral_const() {
    assertClone(mapLiteral(Keyword.CONST, null));
  }

  public void test_visitMapLiteral_empty() {
    assertClone(mapLiteral());
  }

  public void test_visitMapLiteral_nonEmpty() {
    assertClone(mapLiteral(
        mapLiteralEntry("a", identifier("a")),
        mapLiteralEntry("b", identifier("b")),
        mapLiteralEntry("c", identifier("c"))));
  }

  public void test_visitMapLiteralEntry() {
    assertClone(mapLiteralEntry("a", identifier("b")));
  }

  public void test_visitMethodDeclaration_external() {
    assertClone(methodDeclaration(null, null, null, null, identifier("m"), formalParameterList()));
  }

  public void test_visitMethodDeclaration_external_returnType() {
    assertClone(methodDeclaration(
        null,
        typeName("T"),
        null,
        null,
        identifier("m"),
        formalParameterList()));
  }

  public void test_visitMethodDeclaration_getter() {
    assertClone(methodDeclaration(
        null,
        null,
        Keyword.GET,
        null,
        identifier("m"),
        null,
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_getter_returnType() {
    assertClone(methodDeclaration(
        null,
        typeName("T"),
        Keyword.GET,
        null,
        identifier("m"),
        null,
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_getter_seturnType() {
    assertClone(methodDeclaration(
        null,
        typeName("T"),
        Keyword.SET,
        null,
        identifier("m"),
        formalParameterList(simpleFormalParameter(Keyword.VAR, "v")),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_minimal() {
    assertClone(methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_multipleParameters() {
    assertClone(methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("m"),
        formalParameterList(
            simpleFormalParameter(Keyword.VAR, "a"),
            simpleFormalParameter(Keyword.VAR, "b")),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_operator() {
    assertClone(methodDeclaration(
        null,
        null,
        null,
        Keyword.OPERATOR,
        identifier("+"),
        formalParameterList(),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_operator_returnType() {
    assertClone(methodDeclaration(
        null,
        typeName("T"),
        null,
        Keyword.OPERATOR,
        identifier("+"),
        formalParameterList(),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_returnType() {
    assertClone(methodDeclaration(
        null,
        typeName("T"),
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_setter() {
    assertClone(methodDeclaration(
        null,
        null,
        Keyword.SET,
        null,
        identifier("m"),
        formalParameterList(simpleFormalParameter(Keyword.VAR, "v")),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_static() {
    assertClone(methodDeclaration(
        Keyword.STATIC,
        null,
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_static_returnType() {
    assertClone(methodDeclaration(
        Keyword.STATIC,
        typeName("T"),
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_withMetadata() {
    MethodDeclaration declaration = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier("m"),
        formalParameterList(),
        blockFunctionBody());
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitMethodInvocation_noTarget() {
    assertClone(methodInvocation("m"));
  }

  public void test_visitMethodInvocation_target() {
    assertClone(methodInvocation(identifier("t"), "m"));
  }

  public void test_visitNamedExpression() {
    assertClone(namedExpression("a", identifier("b")));
  }

  public void test_visitNamedFormalParameter() {
    assertClone(namedFormalParameter(simpleFormalParameter(Keyword.VAR, "a"), integer(0L)));
  }

  public void test_visitNativeClause() {
    assertClone(nativeClause("code"));
  }

  public void test_visitNativeFunctionBody() {
    assertClone(nativeFunctionBody("str"));
  }

  public void test_visitNullLiteral() {
    assertClone(nullLiteral());
  }

  public void test_visitParenthesizedExpression() {
    assertClone(parenthesizedExpression(identifier("a")));
  }

  public void test_visitPartDirective() {
    assertClone(partDirective("a.dart"));
  }

  public void test_visitPartDirective_withMetadata() {
    PartDirective directive = partDirective("a.dart");
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(directive);
  }

  public void test_visitPartOfDirective() {
    assertClone(partOfDirective(libraryIdentifier("l")));
  }

  public void test_visitPartOfDirective_withMetadata() {
    PartOfDirective directive = partOfDirective(libraryIdentifier("l"));
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(directive);
  }

  public void test_visitPositionalFormalParameter() {
    assertClone(positionalFormalParameter(simpleFormalParameter(Keyword.VAR, "a"), integer(0L)));
  }

  public void test_visitPostfixExpression() {
    assertClone(postfixExpression(identifier("a"), TokenType.PLUS_PLUS));
  }

  public void test_visitPrefixedIdentifier() {
    assertClone(identifier("a", "b"));
  }

  public void test_visitPrefixExpression() {
    assertClone(prefixExpression(TokenType.MINUS, identifier("a")));
  }

  public void test_visitPropertyAccess() {
    assertClone(propertyAccess(identifier("a"), "b"));
  }

  public void test_visitRedirectingConstructorInvocation_named() {
    assertClone(redirectingConstructorInvocation("c"));
  }

  public void test_visitRedirectingConstructorInvocation_unnamed() {
    assertClone(redirectingConstructorInvocation());
  }

  public void test_visitRethrowExpression() {
    assertClone(rethrowExpression());
  }

  public void test_visitReturnStatement_expression() {
    assertClone(returnStatement(identifier("a")));
  }

  public void test_visitReturnStatement_noExpression() {
    assertClone(returnStatement());
  }

  public void test_visitScriptTag() {
    String scriptTag = "!#/bin/dart.exe";
    assertClone(scriptTag(scriptTag));
  }

  public void test_visitSimpleFormalParameter_keyword() {
    assertClone(simpleFormalParameter(Keyword.VAR, "a"));
  }

  public void test_visitSimpleFormalParameter_keyword_type() {
    assertClone(simpleFormalParameter(Keyword.FINAL, typeName("A"), "a"));
  }

  public void test_visitSimpleFormalParameter_type() {
    assertClone(simpleFormalParameter(typeName("A"), "a"));
  }

  public void test_visitSimpleIdentifier() {
    assertClone(identifier("a"));
  }

  public void test_visitSimpleStringLiteral() {
    assertClone(string("a"));
  }

  public void test_visitStringInterpolation() {
    assertClone(string(
        interpolationString("'a", "a"),
        interpolationExpression(identifier("e")),
        interpolationString("b'", "b")));
  }

  public void test_visitSuperConstructorInvocation() {
    assertClone(superConstructorInvocation());
  }

  public void test_visitSuperConstructorInvocation_named() {
    assertClone(superConstructorInvocation("c"));
  }

  public void test_visitSuperExpression() {
    assertClone(superExpression());
  }

  public void test_visitSwitchCase_multipleLabels() {
    assertClone(switchCase(list(label("l1"), label("l2")), identifier("a"), block()));
  }

  public void test_visitSwitchCase_multipleStatements() {
    assertClone(switchCase(identifier("a"), block(), block()));
  }

  public void test_visitSwitchCase_noLabels() {
    assertClone(switchCase(identifier("a"), block()));
  }

  public void test_visitSwitchCase_singleLabel() {
    assertClone(switchCase(list(label("l1")), identifier("a"), block()));
  }

  public void test_visitSwitchDefault_multipleLabels() {
    assertClone(switchDefault(list(label("l1"), label("l2")), block()));
  }

  public void test_visitSwitchDefault_multipleStatements() {
    assertClone(switchDefault(block(), block()));
  }

  public void test_visitSwitchDefault_noLabels() {
    assertClone(switchDefault(block()));
  }

  public void test_visitSwitchDefault_singleLabel() {
    assertClone(switchDefault(list(label("l1")), block()));
  }

  public void test_visitSwitchStatement() {
    assertClone(switchStatement(
        identifier("a"),
        switchCase(string("b"), block()),
        switchDefault(block())));
  }

  public void test_visitSymbolLiteral_multiple() {
    assertClone(symbolLiteral("a", "b", "c"));
  }

  public void test_visitSymbolLiteral_single() {
    assertClone(symbolLiteral("a"));
  }

  public void test_visitThisExpression() {
    assertClone(thisExpression());
  }

  public void test_visitThrowStatement() {
    assertClone(throwExpression(identifier("e")));
  }

  public void test_visitTopLevelVariableDeclaration_multiple() {
    assertClone(topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a")));
  }

  public void test_visitTopLevelVariableDeclaration_single() {
    assertClone(topLevelVariableDeclaration(
        Keyword.VAR,
        variableDeclaration("a"),
        variableDeclaration("b")));
  }

  public void test_visitTryStatement_catch() {
    assertClone(tryStatement(block(), catchClause(typeName("E"))));
  }

  public void test_visitTryStatement_catches() {
    assertClone(tryStatement(block(), catchClause(typeName("E")), catchClause(typeName("F"))));
  }

  public void test_visitTryStatement_catchFinally() {
    assertClone(tryStatement(block(), list(catchClause(typeName("E"))), block()));
  }

  public void test_visitTryStatement_finally() {
    assertClone(tryStatement(block(), block()));
  }

  public void test_visitTypeArgumentList_multiple() {
    assertClone(typeArgumentList(typeName("E"), typeName("F")));
  }

  public void test_visitTypeArgumentList_single() {
    assertClone(typeArgumentList(typeName("E")));
  }

  public void test_visitTypeName_multipleArgs() {
    assertClone(typeName("C", typeName("D"), typeName("E")));
  }

  public void test_visitTypeName_nestedArg() {
    assertClone(typeName("C", typeName("D", typeName("E"))));
  }

  public void test_visitTypeName_noArgs() {
    assertClone(typeName("C"));
  }

  public void test_visitTypeName_singleArg() {
    assertClone(typeName("C", typeName("D")));
  }

  public void test_visitTypeParameter_withExtends() {
    assertClone(typeParameter("E", typeName("C")));
  }

  public void test_visitTypeParameter_withMetadata() {
    TypeParameter parameter = typeParameter("E");
    parameter.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(parameter);
  }

  public void test_visitTypeParameter_withoutExtends() {
    assertClone(typeParameter("E"));
  }

  public void test_visitTypeParameterList_multiple() {
    assertClone(typeParameterList("E", "F"));
  }

  public void test_visitTypeParameterList_single() {
    assertClone(typeParameterList("E"));
  }

  public void test_visitVariableDeclaration_initialized() {
    assertClone(variableDeclaration("a", identifier("b")));
  }

  public void test_visitVariableDeclaration_uninitialized() {
    assertClone(variableDeclaration("a"));
  }

  public void test_visitVariableDeclaration_withMetadata() {
    VariableDeclaration declaration = variableDeclaration("a");
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declaration);
  }

  public void test_visitVariableDeclarationList_const_type() {
    assertClone(variableDeclarationList(
        Keyword.CONST,
        typeName("C"),
        variableDeclaration("a"),
        variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationList_final_noType() {
    assertClone(variableDeclarationList(
        Keyword.FINAL,
        variableDeclaration("a"),
        variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationList_final_withMetadata() {
    VariableDeclarationList declarationList = variableDeclarationList(
        Keyword.FINAL,
        variableDeclaration("a"),
        variableDeclaration("b"));
    declarationList.setMetadata(list(annotation(identifier("deprecated"))));
    assertClone(declarationList);
  }

  public void test_visitVariableDeclarationList_type() {
    assertClone(variableDeclarationList(
        null,
        typeName("C"),
        variableDeclaration("a"),
        variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationList_var() {
    assertClone(variableDeclarationList(
        Keyword.VAR,
        variableDeclaration("a"),
        variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationStatement() {
    assertClone(variableDeclarationStatement(null, typeName("C"), variableDeclaration("c")));
  }

  public void test_visitWhileStatement() {
    assertClone(whileStatement(identifier("c"), block()));
  }

  public void test_visitWithClause_multiple() {
    assertClone(withClause(typeName("A"), typeName("B"), typeName("C")));
  }

  public void test_visitWithClause_single() {
    assertClone(withClause(typeName("A")));
  }

  public void test_visitYieldStatement() {
    assertClone(yieldStatement(identifier("A")));
  }

  /**
   * Assert that an {@code AstCloner} will produce the expected ASt structure when visiting the
   * given node.
   * 
   * @param node the AST node being visited to produce the cloned structure
   * @throws AFE if the visitor does not produce the expected source for the given node
   */
  private void assertClone(AstNode node) {
    AstNode clone = node.accept(new AstCloner());
    if (!AstComparator.equalNodes(node, clone)) {
      fail("Failed to clone " + node.getClass().getSimpleName());
    }
  }
}
