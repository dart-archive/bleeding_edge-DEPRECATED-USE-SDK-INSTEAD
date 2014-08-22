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
import com.google.dart.engine.ast.FunctionDeclarationStatement;
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
import com.google.dart.engine.utilities.io.PrintStringWriter;

import static com.google.dart.engine.ast.AstFactory.*;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromKeyword;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromString;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromType;

public class ToSourceVisitorTest extends EngineTestCase {
  public void test_visitAdjacentStrings() {
    assertSource("'a' 'b'", adjacentStrings(string("a"), string("b")));
  }

  public void test_visitAnnotation_constant() {
    assertSource("@A", annotation(identifier("A")));
  }

  public void test_visitAnnotation_constructor() {
    assertSource("@A.c()", annotation(identifier("A"), identifier("c"), argumentList()));
  }

  public void test_visitArgumentList() {
    assertSource("(a, b)", argumentList(identifier("a"), identifier("b")));
  }

  public void test_visitAsExpression() {
    assertSource("e as T", asExpression(identifier("e"), typeName("T")));
  }

  public void test_visitAssertStatement() {
    assertSource("assert (a);", assertStatement(identifier("a")));
  }

  public void test_visitAssignmentExpression() {
    assertSource("a = b", assignmentExpression(identifier("a"), TokenType.EQ, identifier("b")));
  }

  public void test_visitAwaitExpression() {
    assertSource("await e;", awaitExpression(identifier("e")));
  }

  public void test_visitBinaryExpression() {
    assertSource("a + b", binaryExpression(identifier("a"), TokenType.PLUS, identifier("b")));
  }

  public void test_visitBlock_empty() {
    assertSource("{}", block());
  }

  public void test_visitBlock_nonEmpty() {
    assertSource("{break; break;}", block(breakStatement(), breakStatement()));
  }

  public void test_visitBlockFunctionBody_async() {
    assertSource("async {}", asyncBlockFunctionBody());
  }

  public void test_visitBlockFunctionBody_async_star() {
    assertSource("async* {}", asyncGeneratorBlockFunctionBody());
  }

  public void test_visitBlockFunctionBody_simple() {
    assertSource("{}", blockFunctionBody());
  }

  public void test_visitBlockFunctionBody_sync() {
    assertSource("sync {}", syncBlockFunctionBody());
  }

  public void test_visitBlockFunctionBody_sync_star() {
    assertSource("sync* {}", syncGeneratorBlockFunctionBody());
  }

  public void test_visitBooleanLiteral_false() {
    assertSource("false", booleanLiteral(false));
  }

  public void test_visitBooleanLiteral_true() {
    assertSource("true", booleanLiteral(true));
  }

  public void test_visitBreakStatement_label() {
    assertSource("break l;", breakStatement("l"));
  }

  public void test_visitBreakStatement_noLabel() {
    assertSource("break;", breakStatement());
  }

  public void test_visitCascadeExpression_field() {
    assertSource(
        "a..b..c",
        cascadeExpression(identifier("a"), cascadedPropertyAccess("b"), cascadedPropertyAccess("c")));
  }

  public void test_visitCascadeExpression_index() {
    assertSource(
        "a..[0]..[1]",
        cascadeExpression(
            identifier("a"),
            cascadedIndexExpression(integer(0L)),
            cascadedIndexExpression(integer(1L))));
  }

  public void test_visitCascadeExpression_method() {
    assertSource(
        "a..b()..c()",
        cascadeExpression(
            identifier("a"),
            cascadedMethodInvocation("b"),
            cascadedMethodInvocation("c")));
  }

  public void test_visitCatchClause_catch_noStack() {
    assertSource("catch (e) {}", catchClause("e"));
  }

  public void test_visitCatchClause_catch_stack() {
    assertSource("catch (e, s) {}", catchClause("e", "s"));
  }

  public void test_visitCatchClause_on() {
    assertSource("on E {}", catchClause(typeName("E")));
  }

  public void test_visitCatchClause_on_catch() {
    assertSource("on E catch (e) {}", catchClause(typeName("E"), "e"));
  }

  public void test_visitClassDeclaration_abstract() {
    assertSource(
        "abstract class C {}",
        classDeclaration(Keyword.ABSTRACT, "C", null, null, null, null));
  }

  public void test_visitClassDeclaration_empty() {
    assertSource("class C {}", classDeclaration(null, "C", null, null, null, null));
  }

  public void test_visitClassDeclaration_extends() {
    assertSource(
        "class C extends A {}",
        classDeclaration(null, "C", null, extendsClause(typeName("A")), null, null));
  }

  public void test_visitClassDeclaration_extends_implements() {
    assertSource(
        "class C extends A implements B {}",
        classDeclaration(
            null,
            "C",
            null,
            extendsClause(typeName("A")),
            null,
            implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_extends_with() {
    assertSource(
        "class C extends A with M {}",
        classDeclaration(
            null,
            "C",
            null,
            extendsClause(typeName("A")),
            withClause(typeName("M")),
            null));
  }

  public void test_visitClassDeclaration_extends_with_implements() {
    assertSource(
        "class C extends A with M implements B {}",
        classDeclaration(
            null,
            "C",
            null,
            extendsClause(typeName("A")),
            withClause(typeName("M")),
            implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_implements() {
    assertSource(
        "class C implements B {}",
        classDeclaration(null, "C", null, null, null, implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_multipleMember() {
    assertSource(
        "class C {var a; var b;}",
        classDeclaration(
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
    assertSource(
        "class C<E> {}",
        classDeclaration(null, "C", typeParameterList("E"), null, null, null));
  }

  public void test_visitClassDeclaration_parameters_extends() {
    assertSource(
        "class C<E> extends A {}",
        classDeclaration(
            null,
            "C",
            typeParameterList("E"),
            extendsClause(typeName("A")),
            null,
            null));
  }

  public void test_visitClassDeclaration_parameters_extends_implements() {
    assertSource(
        "class C<E> extends A implements B {}",
        classDeclaration(
            null,
            "C",
            typeParameterList("E"),
            extendsClause(typeName("A")),
            null,
            implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_parameters_extends_with() {
    assertSource(
        "class C<E> extends A with M {}",
        classDeclaration(
            null,
            "C",
            typeParameterList("E"),
            extendsClause(typeName("A")),
            withClause(typeName("M")),
            null));
  }

  public void test_visitClassDeclaration_parameters_extends_with_implements() {
    assertSource(
        "class C<E> extends A with M implements B {}",
        classDeclaration(
            null,
            "C",
            typeParameterList("E"),
            extendsClause(typeName("A")),
            withClause(typeName("M")),
            implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_parameters_implements() {
    assertSource(
        "class C<E> implements B {}",
        classDeclaration(
            null,
            "C",
            typeParameterList("E"),
            null,
            null,
            implementsClause(typeName("B"))));
  }

  public void test_visitClassDeclaration_singleMember() {
    assertSource(
        "class C {var a;}",
        classDeclaration(
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
    assertSource("@deprecated class C {}", declaration);
  }

  public void test_visitClassTypeAlias_abstract() {
    assertSource(
        "abstract class C = S with M1;",
        classTypeAlias("C", null, Keyword.ABSTRACT, typeName("S"), withClause(typeName("M1")), null));
  }

  public void test_visitClassTypeAlias_abstract_implements() {
    assertSource(
        "abstract class C = S with M1 implements I;",
        classTypeAlias(
            "C",
            null,
            Keyword.ABSTRACT,
            typeName("S"),
            withClause(typeName("M1")),
            implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_generic() {
    assertSource(
        "class C<E> = S<E> with M1<E>;",
        classTypeAlias(
            "C",
            typeParameterList("E"),
            null,
            typeName("S", typeName("E")),
            withClause(typeName("M1", typeName("E"))),
            null));
  }

  public void test_visitClassTypeAlias_implements() {
    assertSource(
        "class C = S with M1 implements I;",
        classTypeAlias(
            "C",
            null,
            null,
            typeName("S"),
            withClause(typeName("M1")),
            implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_minimal() {
    assertSource(
        "class C = S with M1;",
        classTypeAlias("C", null, null, typeName("S"), withClause(typeName("M1")), null));
  }

  public void test_visitClassTypeAlias_parameters_abstract() {
    assertSource(
        "abstract class C<E> = S with M1;",
        classTypeAlias(
            "C",
            typeParameterList("E"),
            Keyword.ABSTRACT,
            typeName("S"),
            withClause(typeName("M1")),
            null));
  }

  public void test_visitClassTypeAlias_parameters_abstract_implements() {
    assertSource(
        "abstract class C<E> = S with M1 implements I;",
        classTypeAlias(
            "C",
            typeParameterList("E"),
            Keyword.ABSTRACT,
            typeName("S"),
            withClause(typeName("M1")),
            implementsClause(typeName("I"))));
  }

  public void test_visitClassTypeAlias_parameters_implements() {
    assertSource(
        "class C<E> = S with M1 implements I;",
        classTypeAlias(
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
    assertSource("@deprecated class C = S with M1;", declaration);
  }

  public void test_visitComment() {
    assertSource("", Comment.createBlockComment(new Token[] {tokenFromString("/* comment */")}));
  }

  public void test_visitCommentReference() {
    assertSource("", new CommentReference(null, identifier("a")));
  }

  public void test_visitCompilationUnit_declaration() {
    assertSource(
        "var a;",
        compilationUnit(topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a"))));
  }

  public void test_visitCompilationUnit_directive() {
    assertSource("library l;", compilationUnit(libraryDirective("l")));
  }

  public void test_visitCompilationUnit_directive_declaration() {
    assertSource(
        "library l; var a;",
        compilationUnit(
            list((Directive) libraryDirective("l")),
            list((CompilationUnitMember) topLevelVariableDeclaration(
                Keyword.VAR,
                variableDeclaration("a")))));
  }

  public void test_visitCompilationUnit_empty() {
    assertSource("", compilationUnit());
  }

  public void test_visitCompilationUnit_script() {
    assertSource("!#/bin/dartvm", compilationUnit("!#/bin/dartvm"));
  }

  public void test_visitCompilationUnit_script_declaration() {
    assertSource(
        "!#/bin/dartvm var a;",
        compilationUnit(
            "!#/bin/dartvm",
            topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a"))));
  }

  public void test_visitCompilationUnit_script_directive() {
    assertSource(
        "!#/bin/dartvm library l;",
        compilationUnit("!#/bin/dartvm", libraryDirective("l")));
  }

  public void test_visitCompilationUnit_script_directives_declarations() {
    assertSource(
        "!#/bin/dartvm library l; var a;",
        compilationUnit(
            "!#/bin/dartvm",
            list((Directive) libraryDirective("l")),
            list((CompilationUnitMember) topLevelVariableDeclaration(
                Keyword.VAR,
                variableDeclaration("a")))));
  }

  public void test_visitConditionalExpression() {
    assertSource(
        "a ? b : c",
        conditionalExpression(identifier("a"), identifier("b"), identifier("c")));
  }

  public void test_visitConstructorDeclaration_const() {
    assertSource(
        "const C() {}",
        constructorDeclaration(
            Keyword.CONST,
            null,
            identifier("C"),
            null,
            formalParameterList(),
            null,
            blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_external() {
    assertSource(
        "external C();",
        constructorDeclaration(identifier("C"), null, formalParameterList(), null));
  }

  public void test_visitConstructorDeclaration_minimal() {
    assertSource(
        "C() {}",
        constructorDeclaration(
            null,
            null,
            identifier("C"),
            null,
            formalParameterList(),
            null,
            blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_multipleInitializers() {
    assertSource(
        "C() : a = b, c = d {}",
        constructorDeclaration(
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
    assertSource(
        "C(var a, var b) {}",
        constructorDeclaration(
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
    assertSource(
        "C.m() {}",
        constructorDeclaration(
            null,
            null,
            identifier("C"),
            "m",
            formalParameterList(),
            null,
            blockFunctionBody()));
  }

  public void test_visitConstructorDeclaration_singleInitializer() {
    assertSource(
        "C() : a = b {}",
        constructorDeclaration(
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
    assertSource("@deprecated C() {}", declaration);
  }

  public void test_visitConstructorFieldInitializer_withoutThis() {
    assertSource("a = b", constructorFieldInitializer(false, "a", identifier("b")));
  }

  public void test_visitConstructorFieldInitializer_withThis() {
    assertSource("this.a = b", constructorFieldInitializer(true, "a", identifier("b")));
  }

  public void test_visitConstructorName_named_prefix() {
    assertSource("p.C.n", constructorName(typeName("p.C.n"), null));
  }

  public void test_visitConstructorName_unnamed_noPrefix() {
    assertSource("C", constructorName(typeName("C"), null));
  }

  public void test_visitConstructorName_unnamed_prefix() {
    assertSource("p.C", constructorName(typeName(identifier("p", "C")), null));
  }

  public void test_visitContinueStatement_label() {
    assertSource("continue l;", continueStatement("l"));
  }

  public void test_visitContinueStatement_noLabel() {
    assertSource("continue;", continueStatement());
  }

  public void test_visitDefaultFormalParameter_named_noValue() {
    assertSource("p", namedFormalParameter(simpleFormalParameter("p"), null));
  }

  public void test_visitDefaultFormalParameter_named_value() {
    assertSource("p : 0", namedFormalParameter(simpleFormalParameter("p"), integer(0)));
  }

  public void test_visitDefaultFormalParameter_positional_noValue() {
    assertSource("p", positionalFormalParameter(simpleFormalParameter("p"), null));
  }

  public void test_visitDefaultFormalParameter_positional_value() {
    assertSource("p = 0", positionalFormalParameter(simpleFormalParameter("p"), integer(0)));
  }

  public void test_visitDoStatement() {
    assertSource("do {} while (c);", doStatement(block(), identifier("c")));
  }

  public void test_visitDoubleLiteral() {
    assertSource("4.2", doubleLiteral(4.2));
  }

  public void test_visitEmptyFunctionBody() {
    assertSource(";", emptyFunctionBody());
  }

  public void test_visitEmptyStatement() {
    assertSource(";", emptyStatement());
  }

  public void test_visitEnumDeclaration_multiple() {
    assertSource("enum E {ONE, TWO}", enumDeclaration("E", "ONE", "TWO"));
  }

  public void test_visitEnumDeclaration_single() {
    assertSource("enum E {ONE}", enumDeclaration("E", "ONE"));
  }

  public void test_visitExportDirective_combinator() {
    assertSource(
        "export 'a.dart' show A;",
        exportDirective("a.dart", (Combinator) showCombinator(identifier("A"))));
  }

  public void test_visitExportDirective_combinators() {
    assertSource(
        "export 'a.dart' show A hide B;",
        exportDirective("a.dart", showCombinator(identifier("A")), hideCombinator(identifier("B"))));
  }

  public void test_visitExportDirective_minimal() {
    assertSource("export 'a.dart';", exportDirective("a.dart"));
  }

  public void test_visitExportDirective_withMetadata() {
    ExportDirective directive = exportDirective("a.dart");
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated export 'a.dart';", directive);
  }

  public void test_visitExpressionFunctionBody_async() {
    assertSource("async => a;", asyncExpressionFunctionBody(identifier("a")));
  }

  public void test_visitExpressionFunctionBody_simple() {
    assertSource("=> a;", expressionFunctionBody(identifier("a")));
  }

  public void test_visitExpressionStatement() {
    assertSource("a;", expressionStatement(identifier("a")));
  }

  public void test_visitExtendsClause() {
    assertSource("extends C", extendsClause(typeName("C")));
  }

  public void test_visitFieldDeclaration_instance() {
    assertSource("var a;", fieldDeclaration(false, Keyword.VAR, variableDeclaration("a")));
  }

  public void test_visitFieldDeclaration_static() {
    assertSource("static var a;", fieldDeclaration(true, Keyword.VAR, variableDeclaration("a")));
  }

  public void test_visitFieldDeclaration_withMetadata() {
    FieldDeclaration declaration = fieldDeclaration(false, Keyword.VAR, variableDeclaration("a"));
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated var a;", declaration);
  }

  public void test_visitFieldFormalParameter_functionTyped() {
    assertSource(
        "A this.a(b)",
        fieldFormalParameter(
            null,
            typeName("A"),
            "a",
            formalParameterList(simpleFormalParameter("b"))));
  }

  public void test_visitFieldFormalParameter_keyword() {
    assertSource("var this.a", fieldFormalParameter(Keyword.VAR, null, "a"));
  }

  public void test_visitFieldFormalParameter_keywordAndType() {
    assertSource("final A this.a", fieldFormalParameter(Keyword.FINAL, typeName("A"), "a"));
  }

  public void test_visitFieldFormalParameter_type() {
    assertSource("A this.a", fieldFormalParameter(null, typeName("A"), "a"));
  }

  public void test_visitForEachStatement_declared() {
    assertSource(
        "for (a in b) {}",
        forEachStatement(declaredIdentifier("a"), identifier("b"), block()));
  }

  public void test_visitForEachStatement_variable() {
    assertSource("for (a in b) {}", new ForEachStatement(
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
    assertSource("await for (a in b) {}", new ForEachStatement(
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
    assertSource("()", formalParameterList());
  }

  public void test_visitFormalParameterList_n() {
    assertSource(
        "({a : 0})",
        formalParameterList(namedFormalParameter(simpleFormalParameter("a"), integer(0L))));
  }

  public void test_visitFormalParameterList_nn() {
    assertSource(
        "({a : 0, b : 1})",
        formalParameterList(
            namedFormalParameter(simpleFormalParameter("a"), integer(0L)),
            namedFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_p() {
    assertSource(
        "([a = 0])",
        formalParameterList(positionalFormalParameter(simpleFormalParameter("a"), integer(0L))));
  }

  public void test_visitFormalParameterList_pp() {
    assertSource(
        "([a = 0, b = 1])",
        formalParameterList(
            positionalFormalParameter(simpleFormalParameter("a"), integer(0L)),
            positionalFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_r() {
    assertSource("(a)", formalParameterList(simpleFormalParameter("a")));
  }

  public void test_visitFormalParameterList_rn() {
    assertSource(
        "(a, {b : 1})",
        formalParameterList(
            simpleFormalParameter("a"),
            namedFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_rnn() {
    assertSource(
        "(a, {b : 1, c : 2})",
        formalParameterList(
            simpleFormalParameter("a"),
            namedFormalParameter(simpleFormalParameter("b"), integer(1L)),
            namedFormalParameter(simpleFormalParameter("c"), integer(2L))));
  }

  public void test_visitFormalParameterList_rp() {
    assertSource(
        "(a, [b = 1])",
        formalParameterList(
            simpleFormalParameter("a"),
            positionalFormalParameter(simpleFormalParameter("b"), integer(1L))));
  }

  public void test_visitFormalParameterList_rpp() {
    assertSource(
        "(a, [b = 1, c = 2])",
        formalParameterList(
            simpleFormalParameter("a"),
            positionalFormalParameter(simpleFormalParameter("b"), integer(1L)),
            positionalFormalParameter(simpleFormalParameter("c"), integer(2L))));
  }

  public void test_visitFormalParameterList_rr() {
    assertSource(
        "(a, b)",
        formalParameterList(simpleFormalParameter("a"), simpleFormalParameter("b")));
  }

  public void test_visitFormalParameterList_rrn() {
    assertSource(
        "(a, b, {c : 3})",
        formalParameterList(
            simpleFormalParameter("a"),
            simpleFormalParameter("b"),
            namedFormalParameter(simpleFormalParameter("c"), integer(3L))));
  }

  public void test_visitFormalParameterList_rrnn() {
    assertSource(
        "(a, b, {c : 3, d : 4})",
        formalParameterList(
            simpleFormalParameter("a"),
            simpleFormalParameter("b"),
            namedFormalParameter(simpleFormalParameter("c"), integer(3L)),
            namedFormalParameter(simpleFormalParameter("d"), integer(4L))));
  }

  public void test_visitFormalParameterList_rrp() {
    assertSource(
        "(a, b, [c = 3])",
        formalParameterList(
            simpleFormalParameter("a"),
            simpleFormalParameter("b"),
            positionalFormalParameter(simpleFormalParameter("c"), integer(3L))));
  }

  public void test_visitFormalParameterList_rrpp() {
    assertSource(
        "(a, b, [c = 3, d = 4])",
        formalParameterList(
            simpleFormalParameter("a"),
            simpleFormalParameter("b"),
            positionalFormalParameter(simpleFormalParameter("c"), integer(3L)),
            positionalFormalParameter(simpleFormalParameter("d"), integer(4L))));
  }

  public void test_visitForStatement_c() {
    assertSource("for (; c;) {}", forStatement((Expression) null, identifier("c"), null, block()));
  }

  public void test_visitForStatement_cu() {
    assertSource(
        "for (; c; u) {}",
        forStatement(
            (Expression) null,
            identifier("c"),
            list((Expression) identifier("u")),
            block()));
  }

  public void test_visitForStatement_e() {
    assertSource("for (e;;) {}", forStatement(identifier("e"), null, null, block()));
  }

  public void test_visitForStatement_ec() {
    assertSource("for (e; c;) {}", forStatement(identifier("e"), identifier("c"), null, block()));
  }

  public void test_visitForStatement_ecu() {
    assertSource(
        "for (e; c; u) {}",
        forStatement(identifier("e"), identifier("c"), list((Expression) identifier("u")), block()));
  }

  public void test_visitForStatement_eu() {
    assertSource(
        "for (e;; u) {}",
        forStatement(identifier("e"), null, list((Expression) identifier("u")), block()));
  }

  public void test_visitForStatement_i() {
    assertSource(
        "for (var i;;) {}",
        forStatement(
            variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
            null,
            null,
            block()));
  }

  public void test_visitForStatement_ic() {
    assertSource(
        "for (var i; c;) {}",
        forStatement(
            variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
            identifier("c"),
            null,
            block()));
  }

  public void test_visitForStatement_icu() {
    assertSource(
        "for (var i; c; u) {}",
        forStatement(
            variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
            identifier("c"),
            list((Expression) identifier("u")),
            block()));
  }

  public void test_visitForStatement_iu() {
    assertSource(
        "for (var i;; u) {}",
        forStatement(
            variableDeclarationList(Keyword.VAR, variableDeclaration("i")),
            null,
            list((Expression) identifier("u")),
            block()));
  }

  public void test_visitForStatement_u() {
    assertSource(
        "for (;; u) {}",
        forStatement((Expression) null, null, list((Expression) identifier("u")), block()));
  }

  public void test_visitFunctionDeclaration_getter() {
    assertSource("get f() {}", functionDeclaration(null, Keyword.GET, "f", functionExpression()));
  }

  public void test_visitFunctionDeclaration_local_blockBody() {
    FunctionDeclaration f = functionDeclaration(null, null, "f", functionExpression());
    FunctionDeclarationStatement fStatement = new FunctionDeclarationStatement(f);
    assertSource(
        "main() {f() {} 42;}",
        functionDeclaration(
            null,
            null,
            "main",
            functionExpression(
                formalParameterList(),
                blockFunctionBody(fStatement, expressionStatement(integer(42))))));
  }

  public void test_visitFunctionDeclaration_local_expressionBody() {
    FunctionDeclaration f = functionDeclaration(
        null,
        null,
        "f",
        functionExpression(formalParameterList(), expressionFunctionBody(integer(1))));
    FunctionDeclarationStatement fStatement = new FunctionDeclarationStatement(f);
    assertSource(
        "main() {f() => 1; 2;}",
        functionDeclaration(
            null,
            null,
            "main",
            functionExpression(
                formalParameterList(),
                blockFunctionBody(fStatement, expressionStatement(integer(2))))));
  }

  public void test_visitFunctionDeclaration_normal() {
    assertSource("f() {}", functionDeclaration(null, null, "f", functionExpression()));
  }

  public void test_visitFunctionDeclaration_setter() {
    assertSource("set f() {}", functionDeclaration(null, Keyword.SET, "f", functionExpression()));
  }

  public void test_visitFunctionDeclaration_withMetadata() {
    FunctionDeclaration declaration = functionDeclaration(null, null, "f", functionExpression());
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated f() {}", declaration);
  }

  public void test_visitFunctionDeclarationStatement() {
    assertSource("f() {}", functionDeclarationStatement(null, null, "f", functionExpression()));
  }

  public void test_visitFunctionExpression() {
    assertSource("() {}", functionExpression());
  }

  public void test_visitFunctionExpressionInvocation() {
    assertSource("f()", functionExpressionInvocation(identifier("f")));
  }

  public void test_visitFunctionTypeAlias_generic() {
    assertSource(
        "typedef A F<B>();",
        typeAlias(typeName("A"), "F", typeParameterList("B"), formalParameterList()));
  }

  public void test_visitFunctionTypeAlias_nonGeneric() {
    assertSource("typedef A F();", typeAlias(typeName("A"), "F", null, formalParameterList()));
  }

  public void test_visitFunctionTypeAlias_withMetadata() {
    FunctionTypeAlias declaration = typeAlias(typeName("A"), "F", null, formalParameterList());
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated typedef A F();", declaration);
  }

  public void test_visitFunctionTypedFormalParameter_noType() {
    assertSource("f()", functionTypedFormalParameter(null, "f"));
  }

  public void test_visitFunctionTypedFormalParameter_type() {
    assertSource("T f()", functionTypedFormalParameter(typeName("T"), "f"));
  }

  public void test_visitIfStatement_withElse() {
    assertSource("if (c) {} else {}", ifStatement(identifier("c"), block(), block()));
  }

  public void test_visitIfStatement_withoutElse() {
    assertSource("if (c) {}", ifStatement(identifier("c"), block()));
  }

  public void test_visitImplementsClause_multiple() {
    assertSource("implements A, B", implementsClause(typeName("A"), typeName("B")));
  }

  public void test_visitImplementsClause_single() {
    assertSource("implements A", implementsClause(typeName("A")));
  }

  public void test_visitImportDirective_combinator() {
    assertSource(
        "import 'a.dart' show A;",
        importDirective("a.dart", null, showCombinator(identifier("A"))));
  }

  public void test_visitImportDirective_combinators() {
    assertSource(
        "import 'a.dart' show A hide B;",
        importDirective(
            "a.dart",
            null,
            showCombinator(identifier("A")),
            hideCombinator(identifier("B"))));
  }

  public void test_visitImportDirective_deferred() {
    assertSource("import 'a.dart' deferred as p;", importDirective("a.dart", true, "p"));
  }

  public void test_visitImportDirective_minimal() {
    assertSource("import 'a.dart';", importDirective("a.dart", null));
  }

  public void test_visitImportDirective_prefix() {
    assertSource("import 'a.dart' as p;", importDirective("a.dart", "p"));
  }

  public void test_visitImportDirective_prefix_combinator() {
    assertSource(
        "import 'a.dart' as p show A;",
        importDirective("a.dart", "p", showCombinator(identifier("A"))));
  }

  public void test_visitImportDirective_prefix_combinators() {
    assertSource(
        "import 'a.dart' as p show A hide B;",
        importDirective(
            "a.dart",
            "p",
            showCombinator(identifier("A")),
            hideCombinator(identifier("B"))));
  }

  public void test_visitImportDirective_withMetadata() {
    ImportDirective directive = importDirective("a.dart", null);
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated import 'a.dart';", directive);
  }

  public void test_visitImportHideCombinator_multiple() {
    assertSource("hide a, b", hideCombinator(identifier("a"), identifier("b")));
  }

  public void test_visitImportHideCombinator_single() {
    assertSource("hide a", hideCombinator(identifier("a")));
  }

  public void test_visitImportShowCombinator_multiple() {
    assertSource("show a, b", showCombinator(identifier("a"), identifier("b")));
  }

  public void test_visitImportShowCombinator_single() {
    assertSource("show a", showCombinator(identifier("a")));
  }

  public void test_visitIndexExpression() {
    assertSource("a[i]", indexExpression(identifier("a"), identifier("i")));
  }

  public void test_visitInstanceCreationExpression_const() {
    assertSource("const C()", instanceCreationExpression(Keyword.CONST, typeName("C")));
  }

  public void test_visitInstanceCreationExpression_named() {
    assertSource("new C.c()", instanceCreationExpression(Keyword.NEW, typeName("C"), "c"));
  }

  public void test_visitInstanceCreationExpression_unnamed() {
    assertSource("new C()", instanceCreationExpression(Keyword.NEW, typeName("C")));
  }

  public void test_visitIntegerLiteral() {
    assertSource("42", integer(42L));
  }

  public void test_visitInterpolationExpression_expression() {
    assertSource("${a}", interpolationExpression(identifier("a")));
  }

  public void test_visitInterpolationExpression_identifier() {
    assertSource("$a", interpolationExpression("a"));
  }

  public void test_visitInterpolationString() {
    assertSource("'x", interpolationString("'x", "x"));
  }

  public void test_visitIsExpression_negated() {
    assertSource("a is! C", isExpression(identifier("a"), true, typeName("C")));
  }

  public void test_visitIsExpression_normal() {
    assertSource("a is C", isExpression(identifier("a"), false, typeName("C")));
  }

  public void test_visitLabel() {
    assertSource("a:", label("a"));
  }

  public void test_visitLabeledStatement_multiple() {
    assertSource("a: b: return;", labeledStatement(list(label("a"), label("b")), returnStatement()));
  }

  public void test_visitLabeledStatement_single() {
    assertSource("a: return;", labeledStatement(list(label("a")), returnStatement()));
  }

  public void test_visitLibraryDirective() {
    assertSource("library l;", libraryDirective("l"));
  }

  public void test_visitLibraryDirective_withMetadata() {
    LibraryDirective directive = libraryDirective("l");
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated library l;", directive);
  }

  public void test_visitLibraryIdentifier_multiple() {
    assertSource("a.b.c", libraryIdentifier(identifier("a"), identifier("b"), identifier("c")));
  }

  public void test_visitLibraryIdentifier_single() {
    assertSource("a", libraryIdentifier(identifier("a")));
  }

  public void test_visitListLiteral_const() {
    assertSource("const []", listLiteral(Keyword.CONST, null));
  }

  public void test_visitListLiteral_empty() {
    assertSource("[]", listLiteral());
  }

  public void test_visitListLiteral_nonEmpty() {
    assertSource("[a, b, c]", listLiteral(identifier("a"), identifier("b"), identifier("c")));
  }

  public void test_visitMapLiteral_const() {
    assertSource("const {}", mapLiteral(Keyword.CONST, null));
  }

  public void test_visitMapLiteral_empty() {
    assertSource("{}", mapLiteral());
  }

  public void test_visitMapLiteral_nonEmpty() {
    assertSource(
        "{'a' : a, 'b' : b, 'c' : c}",
        mapLiteral(
            mapLiteralEntry("a", identifier("a")),
            mapLiteralEntry("b", identifier("b")),
            mapLiteralEntry("c", identifier("c"))));
  }

  public void test_visitMapLiteralEntry() {
    assertSource("'a' : b", mapLiteralEntry("a", identifier("b")));
  }

  public void test_visitMethodDeclaration_external() {
    assertSource(
        "external m();",
        methodDeclaration(null, null, null, null, identifier("m"), formalParameterList()));
  }

  public void test_visitMethodDeclaration_external_returnType() {
    assertSource(
        "external T m();",
        methodDeclaration(null, typeName("T"), null, null, identifier("m"), formalParameterList()));
  }

  public void test_visitMethodDeclaration_getter() {
    assertSource(
        "get m {}",
        methodDeclaration(null, null, Keyword.GET, null, identifier("m"), null, blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_getter_returnType() {
    assertSource(
        "T get m {}",
        methodDeclaration(
            null,
            typeName("T"),
            Keyword.GET,
            null,
            identifier("m"),
            null,
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_getter_seturnType() {
    assertSource(
        "T set m(var v) {}",
        methodDeclaration(
            null,
            typeName("T"),
            Keyword.SET,
            null,
            identifier("m"),
            formalParameterList(simpleFormalParameter(Keyword.VAR, "v")),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_minimal() {
    assertSource(
        "m() {}",
        methodDeclaration(
            null,
            null,
            null,
            null,
            identifier("m"),
            formalParameterList(),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_multipleParameters() {
    assertSource(
        "m(var a, var b) {}",
        methodDeclaration(
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
    assertSource(
        "operator +() {}",
        methodDeclaration(
            null,
            null,
            null,
            Keyword.OPERATOR,
            identifier("+"),
            formalParameterList(),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_operator_returnType() {
    assertSource(
        "T operator +() {}",
        methodDeclaration(
            null,
            typeName("T"),
            null,
            Keyword.OPERATOR,
            identifier("+"),
            formalParameterList(),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_returnType() {
    assertSource(
        "T m() {}",
        methodDeclaration(
            null,
            typeName("T"),
            null,
            null,
            identifier("m"),
            formalParameterList(),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_setter() {
    assertSource(
        "set m(var v) {}",
        methodDeclaration(
            null,
            null,
            Keyword.SET,
            null,
            identifier("m"),
            formalParameterList(simpleFormalParameter(Keyword.VAR, "v")),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_static() {
    assertSource(
        "static m() {}",
        methodDeclaration(
            Keyword.STATIC,
            null,
            null,
            null,
            identifier("m"),
            formalParameterList(),
            blockFunctionBody()));
  }

  public void test_visitMethodDeclaration_static_returnType() {
    assertSource(
        "static T m() {}",
        methodDeclaration(
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
    assertSource("@deprecated m() {}", declaration);
  }

  public void test_visitMethodInvocation_noTarget() {
    assertSource("m()", methodInvocation("m"));
  }

  public void test_visitMethodInvocation_target() {
    assertSource("t.m()", methodInvocation(identifier("t"), "m"));
  }

  public void test_visitNamedExpression() {
    assertSource("a: b", namedExpression("a", identifier("b")));
  }

  public void test_visitNamedFormalParameter() {
    assertSource(
        "var a : 0",
        namedFormalParameter(simpleFormalParameter(Keyword.VAR, "a"), integer(0L)));
  }

  public void test_visitNativeClause() {
    assertSource("native 'code'", nativeClause("code"));
  }

  public void test_visitNativeFunctionBody() {
    assertSource("native 'str';", nativeFunctionBody("str"));
  }

  public void test_visitNullLiteral() {
    assertSource("null", nullLiteral());
  }

  public void test_visitParenthesizedExpression() {
    assertSource("(a)", parenthesizedExpression(identifier("a")));
  }

  public void test_visitPartDirective() {
    assertSource("part 'a.dart';", partDirective("a.dart"));
  }

  public void test_visitPartDirective_withMetadata() {
    PartDirective directive = partDirective("a.dart");
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated part 'a.dart';", directive);
  }

  public void test_visitPartOfDirective() {
    assertSource("part of l;", partOfDirective(libraryIdentifier("l")));
  }

  public void test_visitPartOfDirective_withMetadata() {
    PartOfDirective directive = partOfDirective(libraryIdentifier("l"));
    directive.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated part of l;", directive);
  }

  public void test_visitPositionalFormalParameter() {
    assertSource(
        "var a = 0",
        positionalFormalParameter(simpleFormalParameter(Keyword.VAR, "a"), integer(0L)));
  }

  public void test_visitPostfixExpression() {
    assertSource("a++", postfixExpression(identifier("a"), TokenType.PLUS_PLUS));
  }

  public void test_visitPrefixedIdentifier() {
    assertSource("a.b", identifier("a", "b"));
  }

  public void test_visitPrefixExpression() {
    assertSource("-a", prefixExpression(TokenType.MINUS, identifier("a")));
  }

  public void test_visitPropertyAccess() {
    assertSource("a.b", propertyAccess(identifier("a"), "b"));
  }

  public void test_visitRedirectingConstructorInvocation_named() {
    assertSource("this.c()", redirectingConstructorInvocation("c"));
  }

  public void test_visitRedirectingConstructorInvocation_unnamed() {
    assertSource("this()", redirectingConstructorInvocation());
  }

  public void test_visitRethrowExpression() {
    assertSource("rethrow", rethrowExpression());
  }

  public void test_visitReturnStatement_expression() {
    assertSource("return a;", returnStatement(identifier("a")));
  }

  public void test_visitReturnStatement_noExpression() {
    assertSource("return;", returnStatement());
  }

  public void test_visitScriptTag() {
    String scriptTag = "!#/bin/dart.exe";
    assertSource(scriptTag, scriptTag(scriptTag));
  }

  public void test_visitSimpleFormalParameter_keyword() {
    assertSource("var a", simpleFormalParameter(Keyword.VAR, "a"));
  }

  public void test_visitSimpleFormalParameter_keyword_type() {
    assertSource("final A a", simpleFormalParameter(Keyword.FINAL, typeName("A"), "a"));
  }

  public void test_visitSimpleFormalParameter_type() {
    assertSource("A a", simpleFormalParameter(typeName("A"), "a"));
  }

  public void test_visitSimpleIdentifier() {
    assertSource("a", identifier("a"));
  }

  public void test_visitSimpleStringLiteral() {
    assertSource("'a'", string("a"));
  }

  public void test_visitStringInterpolation() {
    assertSource(
        "'a${e}b'",
        string(
            interpolationString("'a", "a"),
            interpolationExpression(identifier("e")),
            interpolationString("b'", "b")));
  }

  public void test_visitSuperConstructorInvocation() {
    assertSource("super()", superConstructorInvocation());
  }

  public void test_visitSuperConstructorInvocation_named() {
    assertSource("super.c()", superConstructorInvocation("c"));
  }

  public void test_visitSuperExpression() {
    assertSource("super", superExpression());
  }

  public void test_visitSwitchCase_multipleLabels() {
    assertSource(
        "l1: l2: case a: {}",
        switchCase(list(label("l1"), label("l2")), identifier("a"), block()));
  }

  public void test_visitSwitchCase_multipleStatements() {
    assertSource("case a: {} {}", switchCase(identifier("a"), block(), block()));
  }

  public void test_visitSwitchCase_noLabels() {
    assertSource("case a: {}", switchCase(identifier("a"), block()));
  }

  public void test_visitSwitchCase_singleLabel() {
    assertSource("l1: case a: {}", switchCase(list(label("l1")), identifier("a"), block()));
  }

  public void test_visitSwitchDefault_multipleLabels() {
    assertSource("l1: l2: default: {}", switchDefault(list(label("l1"), label("l2")), block()));
  }

  public void test_visitSwitchDefault_multipleStatements() {
    assertSource("default: {} {}", switchDefault(block(), block()));
  }

  public void test_visitSwitchDefault_noLabels() {
    assertSource("default: {}", switchDefault(block()));
  }

  public void test_visitSwitchDefault_singleLabel() {
    assertSource("l1: default: {}", switchDefault(list(label("l1")), block()));
  }

  public void test_visitSwitchStatement() {
    assertSource(
        "switch (a) {case 'b': {} default: {}}",
        switchStatement(identifier("a"), switchCase(string("b"), block()), switchDefault(block())));
  }

  public void test_visitSymbolLiteral_multiple() {
    assertSource("#a.b.c", symbolLiteral("a", "b", "c"));
  }

  public void test_visitSymbolLiteral_single() {
    assertSource("#a", symbolLiteral("a"));
  }

  public void test_visitThisExpression() {
    assertSource("this", thisExpression());
  }

  public void test_visitThrowStatement() {
    assertSource("throw e", throwExpression(identifier("e")));
  }

  public void test_visitTopLevelVariableDeclaration_multiple() {
    assertSource("var a;", topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a")));
  }

  public void test_visitTopLevelVariableDeclaration_single() {
    assertSource(
        "var a, b;",
        topLevelVariableDeclaration(Keyword.VAR, variableDeclaration("a"), variableDeclaration("b")));
  }

  public void test_visitTryStatement_catch() {
    assertSource("try {} on E {}", tryStatement(block(), catchClause(typeName("E"))));
  }

  public void test_visitTryStatement_catches() {
    assertSource(
        "try {} on E {} on F {}",
        tryStatement(block(), catchClause(typeName("E")), catchClause(typeName("F"))));
  }

  public void test_visitTryStatement_catchFinally() {
    assertSource(
        "try {} on E {} finally {}",
        tryStatement(block(), list(catchClause(typeName("E"))), block()));
  }

  public void test_visitTryStatement_finally() {
    assertSource("try {} finally {}", tryStatement(block(), block()));
  }

  public void test_visitTypeArgumentList_multiple() {
    assertSource("<E, F>", typeArgumentList(typeName("E"), typeName("F")));
  }

  public void test_visitTypeArgumentList_single() {
    assertSource("<E>", typeArgumentList(typeName("E")));
  }

  public void test_visitTypeName_multipleArgs() {
    assertSource("C<D, E>", typeName("C", typeName("D"), typeName("E")));
  }

  public void test_visitTypeName_nestedArg() {
    assertSource("C<D<E>>", typeName("C", typeName("D", typeName("E"))));
  }

  public void test_visitTypeName_noArgs() {
    assertSource("C", typeName("C"));
  }

  public void test_visitTypeName_singleArg() {
    assertSource("C<D>", typeName("C", typeName("D")));
  }

  public void test_visitTypeParameter_withExtends() {
    assertSource("E extends C", typeParameter("E", typeName("C")));
  }

  public void test_visitTypeParameter_withMetadata() {
    TypeParameter parameter = typeParameter("E");
    parameter.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated E", parameter);
  }

  public void test_visitTypeParameter_withoutExtends() {
    assertSource("E", typeParameter("E"));
  }

  public void test_visitTypeParameterList_multiple() {
    assertSource("<E, F>", typeParameterList("E", "F"));
  }

  public void test_visitTypeParameterList_single() {
    assertSource("<E>", typeParameterList("E"));
  }

  public void test_visitVariableDeclaration_initialized() {
    assertSource("a = b", variableDeclaration("a", identifier("b")));
  }

  public void test_visitVariableDeclaration_uninitialized() {
    assertSource("a", variableDeclaration("a"));
  }

  public void test_visitVariableDeclaration_withMetadata() {
    VariableDeclaration declaration = variableDeclaration("a");
    declaration.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated a", declaration);
  }

  public void test_visitVariableDeclarationList_const_type() {
    assertSource(
        "const C a, b",
        variableDeclarationList(
            Keyword.CONST,
            typeName("C"),
            variableDeclaration("a"),
            variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationList_final_noType() {
    assertSource(
        "final a, b",
        variableDeclarationList(Keyword.FINAL, variableDeclaration("a"), variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationList_final_withMetadata() {
    VariableDeclarationList declarationList = variableDeclarationList(
        Keyword.FINAL,
        variableDeclaration("a"),
        variableDeclaration("b"));
    declarationList.setMetadata(list(annotation(identifier("deprecated"))));
    assertSource("@deprecated final a, b", declarationList);
  }

  public void test_visitVariableDeclarationList_type() {
    assertSource(
        "C a, b",
        variableDeclarationList(
            null,
            typeName("C"),
            variableDeclaration("a"),
            variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationList_var() {
    assertSource(
        "var a, b",
        variableDeclarationList(Keyword.VAR, variableDeclaration("a"), variableDeclaration("b")));
  }

  public void test_visitVariableDeclarationStatement() {
    assertSource(
        "C c;",
        variableDeclarationStatement(null, typeName("C"), variableDeclaration("c")));
  }

  public void test_visitWhileStatement() {
    assertSource("while (c) {}", whileStatement(identifier("c"), block()));
  }

  public void test_visitWithClause_multiple() {
    assertSource("with A, B, C", withClause(typeName("A"), typeName("B"), typeName("C")));
  }

  public void test_visitWithClause_single() {
    assertSource("with A", withClause(typeName("A")));
  }

  public void test_visitYieldStatement() {
    assertSource("yield e;", yieldStatement(identifier("e")));
  }

  public void test_visitYieldStatement_each() {
    assertSource("yield* e;", yieldEachStatement(identifier("e")));
  }

  /**
   * Assert that a {@code ToSourceVisitor} will produce the expected source when visiting the given
   * node.
   * 
   * @param expectedSource the source string that the visitor is expected to produce
   * @param node the AST node being visited to produce the actual source
   * @throws AFE if the visitor does not produce the expected source for the given node
   */
  private void assertSource(String expectedSource, AstNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToSourceVisitor(writer));
    assertEquals(expectedSource, writer.toString());
  }
}
