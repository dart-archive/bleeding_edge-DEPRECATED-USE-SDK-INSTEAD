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
package com.google.dart.engine.parser;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypedLiteral;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.Token;

import static com.google.dart.engine.scanner.TokenFactory.tokenFromKeyword;

/**
 * The class {@code ErrorParserTest} defines parser tests that test the parsing of code to ensure
 * that errors are correctly reported, and in some cases, not reported.
 */
public class ErrorParserTest extends ParserTestCase {
  public void fail_expectedListOrMapLiteral() throws Exception {
    // It isn't clear that this test can ever pass. The parser is currently create a synthetic list
    // literal in this case, but isSynthetic() isn't overridden for ListLiteral. The problem is that
    // the synthetic list literals that are being created are not always zero length (because they
    // could have type parameters), which violates the contract of isSynthetic().
    TypedLiteral literal = parse(
        "parseListOrMapLiteral",
        new Object[] {null},
        "1",
        ParserErrorCode.EXPECTED_LIST_OR_MAP_LITERAL);
    assertTrue(literal.isSynthetic());
  }

  public void fail_illegalAssignmentToNonAssignable_superAssigned() throws Exception {
    // TODO(brianwilkerson) When this test starts to pass, remove the test
    // test_illegalAssignmentToNonAssignable_superAssigned.
    parseExpression("super = x;", ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void fail_invalidCommentReference__new_nonIdentifier() throws Exception {
    // This test fails because the method parseCommentReference returns null.
    parse(
        "parseCommentReference",
        new Object[] {"new 42", 0},
        "",
        ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void fail_invalidCommentReference__new_tooMuch() throws Exception {
    parse(
        "parseCommentReference",
        new Object[] {"new a.b.c.d", 0},
        "",
        ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void fail_invalidCommentReference__nonNew_nonIdentifier() throws Exception {
    // This test fails because the method parseCommentReference returns null.
    parse(
        "parseCommentReference",
        new Object[] {"42", 0},
        "",
        ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void fail_invalidCommentReference__nonNew_tooMuch() throws Exception {
    parse(
        "parseCommentReference",
        new Object[] {"a.b.c.d", 0},
        "",
        ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void fail_missingClosingParenthesis() throws Exception {
    // It is possible that it is not possible to generate this error (that it's being reported in
    // code that cannot actually be reached), but that hasn't been proven yet.
    parse(
        "parseFormalParameterList",
        "(int a, int b ;",
        ParserErrorCode.MISSING_CLOSING_PARENTHESIS);
  }

  public void fail_missingFunctionParameters_local_nonVoid_block() throws Exception {
    // The parser does not recognize this as a function declaration, so it tries to parse it as an
    // expression statement. It isn't clear what the best error message is in this case.
    parseStatement("int f { return x;}", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void fail_missingFunctionParameters_local_nonVoid_expression() throws Exception {
    // The parser does not recognize this as a function declaration, so it tries to parse it as an
    // expression statement. It isn't clear what the best error message is in this case.
    parseStatement("int f => x;", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void fail_namedFunctionExpression() throws Exception {
    Expression expression = parse(
        "parsePrimaryExpression",
        "f() {}",
        ParserErrorCode.NAMED_FUNCTION_EXPRESSION);
    assertInstanceOf(FunctionExpression.class, expression);
  }

  public void fail_unexpectedToken_invalidPostfixExpression() throws Exception {
    // Note: this might not be the right error to produce, but some error should be produced
    parseExpression("f()++", ParserErrorCode.UNEXPECTED_TOKEN);
  }

  public void fail_varAndType_local() throws Exception {
    // This is currently reporting EXPECTED_TOKEN for a missing semicolon, but this would be a
    // better error message.
    parseStatement("var int x;", ParserErrorCode.VAR_AND_TYPE);
  }

  public void fail_varAndType_parameter() throws Exception {
    // This is currently reporting EXPECTED_TOKEN for a missing semicolon, but this would be a
    // better error message.
    parse("parseFormalParameterList", "(var int x)", ParserErrorCode.VAR_AND_TYPE);
  }

  public void test_abstractClassMember_constructor() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "abstract C.c();",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_field() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "abstract C f;",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_getter() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "abstract get m;",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_method() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "abstract m();",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_setter() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "abstract set m(v);",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractEnum() throws Exception {
    parseCompilationUnit("abstract enum E {ONE}", ParserErrorCode.ABSTRACT_ENUM);
  }

  public void test_abstractTopLevelFunction_function() throws Exception {
    parseCompilationUnit("abstract f(v) {}", ParserErrorCode.ABSTRACT_TOP_LEVEL_FUNCTION);
  }

  public void test_abstractTopLevelFunction_getter() throws Exception {
    parseCompilationUnit("abstract get m {}", ParserErrorCode.ABSTRACT_TOP_LEVEL_FUNCTION);
  }

  public void test_abstractTopLevelFunction_setter() throws Exception {
    parseCompilationUnit("abstract set m(v) {}", ParserErrorCode.ABSTRACT_TOP_LEVEL_FUNCTION);
  }

  public void test_abstractTopLevelVariable() throws Exception {
    parseCompilationUnit("abstract C f;", ParserErrorCode.ABSTRACT_TOP_LEVEL_VARIABLE);
  }

  public void test_abstractTypeDef() throws Exception {
    parseCompilationUnit("abstract typedef F();", ParserErrorCode.ABSTRACT_TYPEDEF);
  }

  public void test_assertDoesNotTakeAssignment() throws Exception {
    parse(
        "parseAssertStatement",
        "assert(b = true);",
        ParserErrorCode.ASSERT_DOES_NOT_TAKE_ASSIGNMENT);
  }

  public void test_assertDoesNotTakeCascades() throws Exception {
    parse(
        "parseAssertStatement",
        "assert(new A()..m());",
        ParserErrorCode.ASSERT_DOES_NOT_TAKE_CASCADE);
  }

  public void test_assertDoesNotTakeRethrow() throws Exception {
    parse("parseAssertStatement", "assert(rethrow);", ParserErrorCode.ASSERT_DOES_NOT_TAKE_RETHROW);
  }

  public void test_assertDoesNotTakeThrow() throws Exception {
    parse("parseAssertStatement", "assert(throw x);", ParserErrorCode.ASSERT_DOES_NOT_TAKE_THROW);
  }

  public void test_breakOutsideOfLoop_breakInDoStatement() throws Exception {
    parse("parseDoStatement", "do {break;} while (x);");
  }

  public void test_breakOutsideOfLoop_breakInForStatement() throws Exception {
    parse("parseForStatement", "for (; x;) {break;}");
  }

  public void test_breakOutsideOfLoop_breakInIfStatement() throws Exception {
    parse("parseIfStatement", "if (x) {break;}", ParserErrorCode.BREAK_OUTSIDE_OF_LOOP);
  }

  public void test_breakOutsideOfLoop_breakInSwitchStatement() throws Exception {
    parse("parseSwitchStatement", "switch (x) {case 1: break;}");
  }

  public void test_breakOutsideOfLoop_breakInWhileStatement() throws Exception {
    parse("parseWhileStatement", "while (x) {break;}");
  }

  public void test_breakOutsideOfLoop_functionExpression_inALoop() throws Exception {
    parseStatement("for(; x;) {() {break;};}", ParserErrorCode.BREAK_OUTSIDE_OF_LOOP);
  }

  public void test_breakOutsideOfLoop_functionExpression_withALoop() throws Exception {
    parseStatement("() {for (; x;) {break;}};");
  }

  public void test_classTypeAlias_abstractAfterEq() throws Exception {
    // This syntax has been removed from the language in favor of "abstract class A = B with C;"
    // (issue 18098).
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "class A = abstract B with C;",
        ParserErrorCode.EXPECTED_TOKEN,
        ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_constAndFinal() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "const final int x;",
        ParserErrorCode.CONST_AND_FINAL);
  }

  public void test_constAndVar() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "const var x;", ParserErrorCode.CONST_AND_VAR);
  }

  public void test_constClass() throws Exception {
    parseCompilationUnit("const class C {}", ParserErrorCode.CONST_CLASS);
  }

  public void test_constConstructorWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "const C() {}",
        ParserErrorCode.CONST_CONSTRUCTOR_WITH_BODY);
  }

  public void test_constEnum() throws Exception {
    parseCompilationUnit("const enum E {ONE}", ParserErrorCode.CONST_ENUM);
  }

  public void test_constFactory() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "const factory C() {}",
        ParserErrorCode.CONST_FACTORY);
  }

  public void test_constMethod() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "const int m() {}", ParserErrorCode.CONST_METHOD);
  }

  public void test_constructorWithReturnType() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "C C() {}",
        ParserErrorCode.CONSTRUCTOR_WITH_RETURN_TYPE);
  }

  public void test_constructorWithReturnType_var() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "var C() {}",
        ParserErrorCode.CONSTRUCTOR_WITH_RETURN_TYPE);
  }

  public void test_constTypedef() throws Exception {
    parseCompilationUnit("const typedef F();", ParserErrorCode.CONST_TYPEDEF);
  }

  public void test_continueOutsideOfLoop_continueInDoStatement() throws Exception {
    parse("parseDoStatement", "do {continue;} while (x);");
  }

  public void test_continueOutsideOfLoop_continueInForStatement() throws Exception {
    parse("parseForStatement", "for (; x;) {continue;}");
  }

  public void test_continueOutsideOfLoop_continueInIfStatement() throws Exception {
    parse("parseIfStatement", "if (x) {continue;}", ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
  }

  public void test_continueOutsideOfLoop_continueInSwitchStatement() throws Exception {
    parse("parseSwitchStatement", "switch (x) {case 1: continue a;}");
  }

  public void test_continueOutsideOfLoop_continueInWhileStatement() throws Exception {
    parse("parseWhileStatement", "while (x) {continue;}");
  }

  public void test_continueOutsideOfLoop_functionExpression_inALoop() throws Exception {
    parseStatement("for(; x;) {() {continue;};}", ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
  }

  public void test_continueOutsideOfLoop_functionExpression_withALoop() throws Exception {
    parseStatement("() {for (; x;) {continue;}};");
  }

  public void test_continueWithoutLabelInCase_error() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (x) {case 1: continue;}",
        ParserErrorCode.CONTINUE_WITHOUT_LABEL_IN_CASE);
  }

  public void test_continueWithoutLabelInCase_noError() throws Exception {
    parse("parseSwitchStatement", "switch (x) {case 1: continue a;}");
  }

  public void test_continueWithoutLabelInCase_noError_switchInLoop() throws Exception {
    parse("parseWhileStatement", "while (a) { switch (b) {default: continue;}}");
  }

  public void test_deprecatedClassTypeAlias() throws Exception {
    parseCompilationUnit("typedef C = S with M;", ParserErrorCode.DEPRECATED_CLASS_TYPE_ALIAS);
  }

  public void test_deprecatedClassTypeAlias_withGeneric() throws Exception {
    parseCompilationUnit("typedef C<T> = S<T> with M;", ParserErrorCode.DEPRECATED_CLASS_TYPE_ALIAS);
  }

  public void test_directiveAfterDeclaration_classBeforeDirective() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "class Foo{} library l;",
        ParserErrorCode.DIRECTIVE_AFTER_DECLARATION);
    assertNotNull(unit);
  }

  public void test_directiveAfterDeclaration_classBetweenDirectives() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "library l;\nclass Foo{}\npart 'a.dart';",
        ParserErrorCode.DIRECTIVE_AFTER_DECLARATION);
    assertNotNull(unit);
  }

  public void test_duplicatedModifier_const() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "const const m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_external() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external external f();",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_factory() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "factory factory C() {}",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_final() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "final final m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_static() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static static var m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_var() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "var var m;", ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicateLabelInSwitchStatement() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (e) {l1: case 0: break; l1: case 1: break;}",
        ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT);
  }

  public void test_emptyEnumBody() throws Exception {
    parse(
        "parseEnumDeclaration",
        new Object[] {emptyCommentAndMetadata()},
        "enum E {}",
        ParserErrorCode.EMPTY_ENUM_BODY);
  }

  public void test_equalityCannotBeEqualityOperand_eq_eq() throws Exception {
    parseExpression("1 == 2 == 3", ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
  }

  public void test_equalityCannotBeEqualityOperand_eq_neq() throws Exception {
    parseExpression("1 == 2 != 3", ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
  }

  public void test_equalityCannotBeEqualityOperand_neq_eq() throws Exception {
    parseExpression("1 != 2 == 3", ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
  }

  public void test_expectedCaseOrDefault() throws Exception {
    parse("parseSwitchStatement", "switch (e) {break;}", ParserErrorCode.EXPECTED_CASE_OR_DEFAULT);
  }

  public void test_expectedClassMember_inClass_afterType() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "heart 2 heart",
        ParserErrorCode.EXPECTED_CLASS_MEMBER);
  }

  public void test_expectedClassMember_inClass_beforeType() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "4 score", ParserErrorCode.EXPECTED_CLASS_MEMBER);
  }

  public void test_expectedExecutable_inClass_afterVoid() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "void 2 void",
        ParserErrorCode.EXPECTED_EXECUTABLE);
  }

  public void test_expectedExecutable_topLevel_afterType() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "heart 2 heart",
        ParserErrorCode.EXPECTED_EXECUTABLE);
  }

  public void test_expectedExecutable_topLevel_afterVoid() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "void 2 void",
        ParserErrorCode.EXPECTED_EXECUTABLE);
  }

  public void test_expectedExecutable_topLevel_beforeType() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "4 score",
        ParserErrorCode.EXPECTED_EXECUTABLE);
  }

  public void test_expectedExecutable_topLevel_eof() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "x",
        new AnalysisError(null, 0, 1, ParserErrorCode.EXPECTED_EXECUTABLE));
  }

  public void test_expectedInterpolationIdentifier() throws Exception {
    parse("parseStringLiteral", "'$x$'", ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_expectedInterpolationIdentifier_emptyString() throws Exception {
    // The scanner inserts an empty string token between the two $'s; we need to make sure that the
    // MISSING_IDENTIFIER error that is generated has a nonzero width so that it will show up in
    // the editor UI.
    parse( //
        "parseStringLiteral",
        new Object[] {},
        "'$$foo'",
        new AnalysisError(null, 2, 1, ParserErrorCode.MISSING_IDENTIFIER));
  }

  public void test_expectedStringLiteral() throws Exception {
    StringLiteral expression = parse(
        "parseStringLiteral",
        "1",
        ParserErrorCode.EXPECTED_STRING_LITERAL);
    assertTrue(expression.isSynthetic());
  }

  public void test_expectedToken_commaMissingInArgumentList() throws Exception {
    parse("parseArgumentList", "(x, y z)", ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_expectedToken_parseStatement_afterVoid() throws Exception {
    parseStatement("void}", ParserErrorCode.EXPECTED_TOKEN, ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_expectedToken_semicolonAfterClass() throws Exception {
    Token token = tokenFromKeyword(Keyword.CLASS);
    parse(
        "parseClassTypeAlias",
        new Object[] {emptyCommentAndMetadata(), null, token},
        "A = B with C",
        ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_expectedToken_semicolonMissingAfterExport() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "export '' class A {}",
        ParserErrorCode.EXPECTED_TOKEN);
    ExportDirective directive = (ExportDirective) unit.getDirectives().get(0);
    Token semicolon = directive.getSemicolon();
    assertNotNull(semicolon);
    assertTrue(semicolon.isSynthetic());
  }

  public void test_expectedToken_semicolonMissingAfterExpression() throws Exception {
    parseStatement("x", ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_expectedToken_semicolonMissingAfterImport() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "import '' class A {}",
        ParserErrorCode.EXPECTED_TOKEN);
    ImportDirective directive = (ImportDirective) unit.getDirectives().get(0);
    Token semicolon = directive.getSemicolon();
    assertNotNull(semicolon);
    assertTrue(semicolon.isSynthetic());
  }

  public void test_expectedToken_whileMissingInDoStatement() throws Exception {
    parseStatement("do {} (x);", ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_expectedTypeName_is() throws Exception {
    parseExpression("x is", ParserErrorCode.EXPECTED_TYPE_NAME);
  }

  public void test_exportDirectiveAfterPartDirective() throws Exception {
    parseCompilationUnit(
        "part 'a.dart'; export 'b.dart';",
        ParserErrorCode.EXPORT_DIRECTIVE_AFTER_PART_DIRECTIVE);
  }

  public void test_externalAfterConst() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "const external C();",
        ParserErrorCode.EXTERNAL_AFTER_CONST);
  }

  public void test_externalAfterFactory() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "factory external C();",
        ParserErrorCode.EXTERNAL_AFTER_FACTORY);
  }

  public void test_externalAfterStatic() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static external int m();",
        ParserErrorCode.EXTERNAL_AFTER_STATIC);
  }

  public void test_externalClass() throws Exception {
    parseCompilationUnit("external class C {}", ParserErrorCode.EXTERNAL_CLASS);
  }

  public void test_externalConstructorWithBody_factory() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external factory C() {}",
        ParserErrorCode.EXTERNAL_CONSTRUCTOR_WITH_BODY);
  }

  public void test_externalConstructorWithBody_named() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external C.c() {}",
        ParserErrorCode.EXTERNAL_CONSTRUCTOR_WITH_BODY);
  }

  public void test_externalEnum() throws Exception {
    parseCompilationUnit("external enum E {ONE}", ParserErrorCode.EXTERNAL_ENUM);
  }

  public void test_externalField_const() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external const A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_final() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external final A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_static() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external static A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_typed() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "external A f;", ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_untyped() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "external var f;", ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalGetterWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external int get x {}",
        ParserErrorCode.EXTERNAL_GETTER_WITH_BODY);
  }

  public void test_externalMethodWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external m() {}",
        ParserErrorCode.EXTERNAL_METHOD_WITH_BODY);
  }

  public void test_externalOperatorWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external operator +(int value) {}",
        ParserErrorCode.EXTERNAL_OPERATOR_WITH_BODY);
  }

  public void test_externalSetterWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "external set x(int value) {}",
        ParserErrorCode.EXTERNAL_SETTER_WITH_BODY);
  }

  public void test_externalTypedef() throws Exception {
    parseCompilationUnit("external typedef F();", ParserErrorCode.EXTERNAL_TYPEDEF);
  }

  public void test_factoryTopLevelDeclaration_class() throws Exception {
    parseCompilationUnit("factory class C {}", ParserErrorCode.FACTORY_TOP_LEVEL_DECLARATION);
  }

  public void test_factoryTopLevelDeclaration_typedef() throws Exception {
    parseCompilationUnit("factory typedef F();", ParserErrorCode.FACTORY_TOP_LEVEL_DECLARATION);
  }

  public void test_factoryWithoutBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "factory C();",
        ParserErrorCode.FACTORY_WITHOUT_BODY);
  }

  public void test_fieldInitializerOutsideConstructor() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "void m(this.x);",
        ParserErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR);
  }

  public void test_finalAndVar() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "final var x;", ParserErrorCode.FINAL_AND_VAR);
  }

  public void test_finalClass() throws Exception {
    parseCompilationUnit("final class C {}", ParserErrorCode.FINAL_CLASS);
  }

  public void test_finalConstructor() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "final C() {}", ParserErrorCode.FINAL_CONSTRUCTOR);
  }

  public void test_finalEnum() throws Exception {
    parseCompilationUnit("final enum E {ONE}", ParserErrorCode.FINAL_ENUM);
  }

  public void test_finalMethod() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "final int m() {}", ParserErrorCode.FINAL_METHOD);
  }

  public void test_finalTypedef() throws Exception {
    parseCompilationUnit("final typedef F();", ParserErrorCode.FINAL_TYPEDEF);
  }

  public void test_functionTypedParameter_const() throws Exception {
    parseCompilationUnit("void f(const x()) {}", ParserErrorCode.FUNCTION_TYPED_PARAMETER_VAR);
  }

  public void test_functionTypedParameter_final() throws Exception {
    parseCompilationUnit("void f(final x()) {}", ParserErrorCode.FUNCTION_TYPED_PARAMETER_VAR);
  }

  public void test_functionTypedParameter_var() throws Exception {
    parseCompilationUnit("void f(var x()) {}", ParserErrorCode.FUNCTION_TYPED_PARAMETER_VAR);
  }

  public void test_getterInFunction_block() throws Exception {
    parseStatement("get x { return _x; }", ParserErrorCode.GETTER_IN_FUNCTION);
  }

  public void test_getterInFunction_expression() throws Exception {
    parseStatement("get x => _x;", ParserErrorCode.GETTER_IN_FUNCTION);
  }

  public void test_getterWithParameters() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "int get x() {}",
        ParserErrorCode.GETTER_WITH_PARAMETERS);
  }

  public void test_illegalAssignmentToNonAssignable_postfix_minusMinus_literal() throws Exception {
    parseExpression("0--", ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void test_illegalAssignmentToNonAssignable_postfix_plusPlus_literal() throws Exception {
    parseExpression("0++", ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void test_illegalAssignmentToNonAssignable_postfix_plusPlus_parethesized()
      throws Exception {
    parseExpression("(x)++", ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void test_illegalAssignmentToNonAssignable_primarySelectorPostfix() throws Exception {
    parseExpression("x(y)(z)++", ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void test_illegalAssignmentToNonAssignable_superAssigned() throws Exception {
    // TODO(brianwilkerson) When the test fail_illegalAssignmentToNonAssignable_superAssigned starts
    // to pass, remove this test (there should only be one error generated, but we're keeping this
    // test until that time so that we can catch other forms of regressions).
    parseExpression(
        "super = x;",
        ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR,
        ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void test_implementsBeforeExtends() throws Exception {
    parseCompilationUnit(
        "class A implements B extends C {}",
        ParserErrorCode.IMPLEMENTS_BEFORE_EXTENDS);
  }

  public void test_implementsBeforeWith() throws Exception {
    parseCompilationUnit(
        "class A extends B implements C with D {}",
        ParserErrorCode.IMPLEMENTS_BEFORE_WITH);
  }

  public void test_importDirectiveAfterPartDirective() throws Exception {
    parseCompilationUnit(
        "part 'a.dart'; import 'b.dart';",
        ParserErrorCode.IMPORT_DIRECTIVE_AFTER_PART_DIRECTIVE);
  }

  public void test_initializedVariableInForEach() throws Exception {
    parse(
        "parseForStatement",
        "for (int a = 0 in foo) {}",
        ParserErrorCode.INITIALIZED_VARIABLE_IN_FOR_EACH);
  }

  public void test_invalidAwaitInFor() throws Exception {
    parse("parseForStatement", "await for (; ;) {}", ParserErrorCode.INVALID_AWAIT_IN_FOR);
  }

  public void test_invalidCodePoint() throws Exception {
    parse("parseStringLiteral", "'\\uD900'", ParserErrorCode.INVALID_CODE_POINT);
  }

  public void test_invalidHexEscape_invalidDigit() throws Exception {
    parse("parseStringLiteral", "'\\x0 a'", ParserErrorCode.INVALID_HEX_ESCAPE);
  }

  public void test_invalidHexEscape_tooFewDigits() throws Exception {
    parse("parseStringLiteral", "'\\x0'", ParserErrorCode.INVALID_HEX_ESCAPE);
  }

  public void test_invalidInterpolationIdentifier_startWithDigit() throws Exception {
    parse("parseStringLiteral", "'$1'", ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_invalidOperator() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "void operator ===(x) {}",
        ParserErrorCode.INVALID_OPERATOR);
  }

  public void test_invalidOperatorForSuper() throws Exception {
    parse("parseUnaryExpression", "++super", ParserErrorCode.INVALID_OPERATOR_FOR_SUPER);
  }

  public void test_invalidStarAfterAsync() throws Exception {
    parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "async* => 0;",
        ParserErrorCode.INVALID_STAR_AFTER_ASYNC);
  }

  public void test_invalidSync() throws Exception {
    parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "sync* => 0;",
        ParserErrorCode.INVALID_SYNC);
  }

  public void test_invalidUnicodeEscape_incomplete_noDigits() throws Exception {
    parse("parseStringLiteral", "'\\u{'", ParserErrorCode.INVALID_UNICODE_ESCAPE);
  }

  public void test_invalidUnicodeEscape_incomplete_someDigits() throws Exception {
    parse("parseStringLiteral", "'\\u{0A'", ParserErrorCode.INVALID_UNICODE_ESCAPE);
  }

  public void test_invalidUnicodeEscape_invalidDigit() throws Exception {
    parse("parseStringLiteral", "'\\u0 a'", ParserErrorCode.INVALID_UNICODE_ESCAPE);
  }

  public void test_invalidUnicodeEscape_tooFewDigits_fixed() throws Exception {
    parse("parseStringLiteral", "'\\u04'", ParserErrorCode.INVALID_UNICODE_ESCAPE);
  }

  public void test_invalidUnicodeEscape_tooFewDigits_variable() throws Exception {
    parse("parseStringLiteral", "'\\u{}'", ParserErrorCode.INVALID_UNICODE_ESCAPE);
  }

  public void test_invalidUnicodeEscape_tooManyDigits_variable() throws Exception {
    parse(
        "parseStringLiteral",
        "'\\u{12345678}'",
        ParserErrorCode.INVALID_UNICODE_ESCAPE,
        ParserErrorCode.INVALID_CODE_POINT);
  }

  public void test_libraryDirectiveNotFirst() throws Exception {
    parseCompilationUnit("import 'x.dart'; library l;", ParserErrorCode.LIBRARY_DIRECTIVE_NOT_FIRST);
  }

  public void test_libraryDirectiveNotFirst_afterPart() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "part 'a.dart';\nlibrary l;",
        ParserErrorCode.LIBRARY_DIRECTIVE_NOT_FIRST);
    assertNotNull(unit);
  }

  public void test_localFunctionDeclarationModifier_abstract() throws Exception {
    parseStatement("abstract f() {}", ParserErrorCode.LOCAL_FUNCTION_DECLARATION_MODIFIER);
  }

  public void test_localFunctionDeclarationModifier_external() throws Exception {
    parseStatement("external f() {}", ParserErrorCode.LOCAL_FUNCTION_DECLARATION_MODIFIER);
  }

  public void test_localFunctionDeclarationModifier_factory() throws Exception {
    parseStatement("factory f() {}", ParserErrorCode.LOCAL_FUNCTION_DECLARATION_MODIFIER);
  }

  public void test_localFunctionDeclarationModifier_static() throws Exception {
    parseStatement("static f() {}", ParserErrorCode.LOCAL_FUNCTION_DECLARATION_MODIFIER);
  }

  public void test_missingAssignableSelector_identifiersAssigned() throws Exception {
    parseExpression("x.y = y;");
  }

  public void test_missingAssignableSelector_prefix_minusMinus_literal() throws Exception {
    parseExpression("--0", ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
  }

  public void test_missingAssignableSelector_prefix_plusPlus_literal() throws Exception {
    parseExpression("++0", ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
  }

  public void test_missingAssignableSelector_selector() throws Exception {
    parseExpression("x(y)(z).a++");
  }

  public void test_missingAssignableSelector_superPrimaryExpression() throws Exception {
    SuperExpression expression = parse(
        "parsePrimaryExpression",
        "super",
        ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
    assertNotNull(expression.getKeyword());
  }

  public void test_missingAssignableSelector_superPropertyAccessAssigned() throws Exception {
    parseExpression("super.x = x;");
  }

  public void test_missingCatchOrFinally() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {}",
        ParserErrorCode.MISSING_CATCH_OR_FINALLY);
    assertNotNull(statement);
  }

  public void test_missingClassBody() throws Exception {
    parseCompilationUnit("class A class B {}", ParserErrorCode.MISSING_CLASS_BODY);
  }

  public void test_missingConstFinalVarOrType_static() throws Exception {
    parseCompilationUnit("class A { static f; }", ParserErrorCode.MISSING_CONST_FINAL_VAR_OR_TYPE);
  }

  public void test_missingConstFinalVarOrType_topLevel() throws Exception {
    parse(
        "parseFinalConstVarOrType",
        new Object[] {false},
        "a;",
        ParserErrorCode.MISSING_CONST_FINAL_VAR_OR_TYPE);
  }

  public void test_missingEnumBody() throws Exception {
    parse(
        "parseEnumDeclaration",
        new Object[] {emptyCommentAndMetadata()},
        "enum E;",
        ParserErrorCode.MISSING_ENUM_BODY);
  }

  public void test_missingExpressionInThrow_withCascade() throws Exception {
    parse("parseThrowExpression", "throw;", ParserErrorCode.MISSING_EXPRESSION_IN_THROW);
  }

  public void test_missingExpressionInThrow_withoutCascade() throws Exception {
    parse(
        "parseThrowExpressionWithoutCascade",
        "throw;",
        ParserErrorCode.MISSING_EXPRESSION_IN_THROW);
  }

  public void test_missingFunctionBody_emptyNotAllowed() throws Exception {
    parse(
        "parseFunctionBody",
        new Object[] {false, ParserErrorCode.MISSING_FUNCTION_BODY, false},
        ";",
        ParserErrorCode.MISSING_FUNCTION_BODY);
  }

  public void test_missingFunctionBody_invalid() throws Exception {
    parse(
        "parseFunctionBody",
        new Object[] {false, ParserErrorCode.MISSING_FUNCTION_BODY, false},
        "return 0;",
        ParserErrorCode.MISSING_FUNCTION_BODY);
  }

  public void test_missingFunctionParameters_local_void_block() throws Exception {
    parseStatement("void f { return x;}", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void test_missingFunctionParameters_local_void_expression() throws Exception {
    parseStatement("void f => x;", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void test_missingFunctionParameters_topLevel_nonVoid_block() throws Exception {
    parseCompilationUnit("int f { return x;}", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void test_missingFunctionParameters_topLevel_nonVoid_expression() throws Exception {
    parseCompilationUnit("int f => x;", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void test_missingFunctionParameters_topLevel_void_block() throws Exception {
    parseCompilationUnit("void f { return x;}", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void test_missingFunctionParameters_topLevel_void_expression() throws Exception {
    parseCompilationUnit("void f => x;", ParserErrorCode.MISSING_FUNCTION_PARAMETERS);
  }

  public void test_missingIdentifier_afterOperator() throws Exception {
    parse("parseMultiplicativeExpression", "1 *", ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_missingIdentifier_beforeClosingCurly() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "int}",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_missingIdentifier_functionDeclaration_returnTypeWithoutName() throws Exception {
    parse("parseFunctionDeclarationStatement", "A<T> () {}", ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_missingIdentifier_inEnum() throws Exception {
    parse(
        "parseEnumDeclaration",
        new Object[] {emptyCommentAndMetadata()},
        "enum E {, TWO}",
        ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_missingIdentifier_inSymbol_afterPeriod() throws Exception {
    parse("parseSymbolLiteral", "#a.", ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_missingIdentifier_inSymbol_first() throws Exception {
    parse("parseSymbolLiteral", "#", ParserErrorCode.MISSING_IDENTIFIER);
  }

  public void test_missingIdentifier_number() throws Exception {
    SimpleIdentifier expression = parse(
        "parseSimpleIdentifier",
        "1",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertTrue(expression.isSynthetic());
  }

  public void test_missingKeywordOperator() throws Exception {
    parse(
        "parseOperator",
        new Object[] {emptyCommentAndMetadata(), null, null},
        "+(x) {}",
        ParserErrorCode.MISSING_KEYWORD_OPERATOR);
  }

  public void test_missingKeywordOperator_parseClassMember() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "+() {}",
        ParserErrorCode.MISSING_KEYWORD_OPERATOR);
  }

  public void test_missingKeywordOperator_parseClassMember_afterTypeName() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "int +() {}",
        ParserErrorCode.MISSING_KEYWORD_OPERATOR);
  }

  public void test_missingKeywordOperator_parseClassMember_afterVoid() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "void +() {}",
        ParserErrorCode.MISSING_KEYWORD_OPERATOR);
  }

  public void test_missingNameInLibraryDirective() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "library;",
        ParserErrorCode.MISSING_NAME_IN_LIBRARY_DIRECTIVE);
    assertNotNull(unit);
  }

  public void test_missingNameInPartOfDirective() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "part of;",
        ParserErrorCode.MISSING_NAME_IN_PART_OF_DIRECTIVE);
    assertNotNull(unit);
  }

  public void test_missingPrefixInDeferredImport() throws Exception {
    parseCompilationUnit(
        "import 'foo.dart' deferred;",
        ParserErrorCode.MISSING_PREFIX_IN_DEFERRED_IMPORT);
  }

  public void test_missingStartAfterSync() throws Exception {
    parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "sync {}",
        ParserErrorCode.MISSING_STAR_AFTER_SYNC);
  }

  public void test_missingStatement() throws Exception {
    parseStatement("is", ParserErrorCode.MISSING_STATEMENT);
  }

  public void test_missingStatement_afterVoid() throws Exception {
    parseStatement("void;", ParserErrorCode.MISSING_STATEMENT);
  }

  public void test_missingTerminatorForParameterGroup_named() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, {b: 0)",
        ParserErrorCode.MISSING_TERMINATOR_FOR_PARAMETER_GROUP);
  }

  public void test_missingTerminatorForParameterGroup_optional() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, [b = 0)",
        ParserErrorCode.MISSING_TERMINATOR_FOR_PARAMETER_GROUP);
  }

  public void test_missingTypedefParameters_nonVoid() throws Exception {
    parseCompilationUnit("typedef int F;", ParserErrorCode.MISSING_TYPEDEF_PARAMETERS);
  }

  public void test_missingTypedefParameters_typeParameters() throws Exception {
    parseCompilationUnit("typedef F<E>;", ParserErrorCode.MISSING_TYPEDEF_PARAMETERS);
  }

  public void test_missingTypedefParameters_void() throws Exception {
    parseCompilationUnit("typedef void F;", ParserErrorCode.MISSING_TYPEDEF_PARAMETERS);
  }

  public void test_missingVariableInForEach() throws Exception {
    parse(
        "parseForStatement",
        "for (a < b in foo) {}",
        ParserErrorCode.MISSING_VARIABLE_IN_FOR_EACH);
  }

  public void test_mixedParameterGroups_namedPositional() throws Exception {
    parse("parseFormalParameterList", "(a, {b}, [c])", ParserErrorCode.MIXED_PARAMETER_GROUPS);
  }

  public void test_mixedParameterGroups_positionalNamed() throws Exception {
    parse("parseFormalParameterList", "(a, [b], {c})", ParserErrorCode.MIXED_PARAMETER_GROUPS);
  }

  public void test_mixin_application_lacks_with_clause() throws Exception {
    parseCompilationUnit("class Foo = Bar;", ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_multipleExtendsClauses() throws Exception {
    parseCompilationUnit("class A extends B extends C {}", ParserErrorCode.MULTIPLE_EXTENDS_CLAUSES);
  }

  public void test_multipleImplementsClauses() throws Exception {
    parseCompilationUnit(
        "class A implements B implements C {}",
        ParserErrorCode.MULTIPLE_IMPLEMENTS_CLAUSES);
  }

  public void test_multipleLibraryDirectives() throws Exception {
    parseCompilationUnit("library l; library m;", ParserErrorCode.MULTIPLE_LIBRARY_DIRECTIVES);
  }

  public void test_multipleNamedParameterGroups() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, {b}, {c})",
        ParserErrorCode.MULTIPLE_NAMED_PARAMETER_GROUPS);
  }

  public void test_multiplePartOfDirectives() throws Exception {
    parseCompilationUnit("part of l; part of m;", ParserErrorCode.MULTIPLE_PART_OF_DIRECTIVES);
  }

  public void test_multiplePositionalParameterGroups() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, [b], [c])",
        ParserErrorCode.MULTIPLE_POSITIONAL_PARAMETER_GROUPS);
  }

  public void test_multipleVariablesInForEach() throws Exception {
    parse(
        "parseForStatement",
        "for (int a, b in foo) {}",
        ParserErrorCode.MULTIPLE_VARIABLES_IN_FOR_EACH);
  }

  public void test_multipleWithClauses() throws Exception {
    parseCompilationUnit(
        "class A extends B with C with D {}",
        ParserErrorCode.MULTIPLE_WITH_CLAUSES);
  }

  public void test_namedParameterOutsideGroup() throws Exception {
    parse("parseFormalParameterList", "(a, b : 0)", ParserErrorCode.NAMED_PARAMETER_OUTSIDE_GROUP);
  }

  public void test_nonConstructorFactory_field() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "factory int x;",
        ParserErrorCode.NON_CONSTRUCTOR_FACTORY);
  }

  public void test_nonConstructorFactory_method() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "factory int m() {}",
        ParserErrorCode.NON_CONSTRUCTOR_FACTORY);
  }

  public void test_nonIdentifierLibraryName_library() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "library 'lib';",
        ParserErrorCode.NON_IDENTIFIER_LIBRARY_NAME);
    assertNotNull(unit);
  }

  public void test_nonIdentifierLibraryName_partOf() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "part of 'lib';",
        ParserErrorCode.NON_IDENTIFIER_LIBRARY_NAME);
    assertNotNull(unit);
  }

  public void test_nonPartOfDirectiveInPart_after() throws Exception {
    parseCompilationUnit("part of l; part 'f.dart';", ParserErrorCode.NON_PART_OF_DIRECTIVE_IN_PART);
  }

  public void test_nonPartOfDirectiveInPart_before() throws Exception {
    parseCompilationUnit("part 'f.dart'; part of m;", ParserErrorCode.NON_PART_OF_DIRECTIVE_IN_PART);
  }

  public void test_nonUserDefinableOperator() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "operator +=(int x) => x + 1;",
        ParserErrorCode.NON_USER_DEFINABLE_OPERATOR);
  }

  public void test_optionalAfterNormalParameters_named() throws Exception {
    parseCompilationUnit("f({a}, b) {}", ParserErrorCode.NORMAL_BEFORE_OPTIONAL_PARAMETERS);
  }

  public void test_optionalAfterNormalParameters_positional() throws Exception {
    parseCompilationUnit("f([a], b) {}", ParserErrorCode.NORMAL_BEFORE_OPTIONAL_PARAMETERS);
  }

  public void test_parseCascadeSection_missingIdentifier() throws Exception {
    MethodInvocation methodInvocation = parse(
        "parseCascadeSection",
        "..()",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertNull(methodInvocation.getTarget());
    assertEquals("", methodInvocation.getMethodName().getName());
    assertSizeOfList(0, methodInvocation.getArgumentList().getArguments());
  }

  public void test_positionalAfterNamedArgument() throws Exception {
    parse("parseArgumentList", "(x: 1, 2)", ParserErrorCode.POSITIONAL_AFTER_NAMED_ARGUMENT);
  }

  public void test_positionalParameterOutsideGroup() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, b = 0)",
        ParserErrorCode.POSITIONAL_PARAMETER_OUTSIDE_GROUP);
  }

  public void test_redirectionInNonFactoryConstructor() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "C() = D;",
        ParserErrorCode.REDIRECTION_IN_NON_FACTORY_CONSTRUCTOR);
  }

  public void test_setterInFunction_block() throws Exception {
    parseStatement("set x(v) {_x = v;}", ParserErrorCode.SETTER_IN_FUNCTION);
  }

  public void test_setterInFunction_expression() throws Exception {
    parseStatement("set x(v) => _x = v;", ParserErrorCode.SETTER_IN_FUNCTION);
  }

  public void test_staticAfterConst() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "final static int f;",
        ParserErrorCode.STATIC_AFTER_FINAL);
  }

  public void test_staticAfterFinal() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "const static int f;",
        ParserErrorCode.STATIC_AFTER_CONST);
  }

  public void test_staticAfterVar() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "var static f;", ParserErrorCode.STATIC_AFTER_VAR);
  }

  public void test_staticConstructor() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static C.m() {}",
        ParserErrorCode.STATIC_CONSTRUCTOR);
  }

  public void test_staticGetterWithoutBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static get m;",
        ParserErrorCode.STATIC_GETTER_WITHOUT_BODY);
  }

  public void test_staticOperator_noReturnType() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static operator +(int x) => x + 1;",
        ParserErrorCode.STATIC_OPERATOR);
  }

  public void test_staticOperator_returnType() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static int operator +(int x) => x + 1;",
        ParserErrorCode.STATIC_OPERATOR);
  }

  public void test_staticSetterWithoutBody() throws Exception {
    parse(
        "parseClassMember",
        new Object[] {"C"},
        "static set m(x);",
        ParserErrorCode.STATIC_SETTER_WITHOUT_BODY);
  }

  public void test_staticTopLevelDeclaration_class() throws Exception {
    parseCompilationUnit("static class C {}", ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION);
  }

  public void test_staticTopLevelDeclaration_function() throws Exception {
    parseCompilationUnit("static f() {}", ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION);
  }

  public void test_staticTopLevelDeclaration_typedef() throws Exception {
    parseCompilationUnit("static typedef F();", ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION);
  }

  public void test_staticTopLevelDeclaration_variable() throws Exception {
    parseCompilationUnit("static var x;", ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION);
  }

  public void test_switchHasCaseAfterDefaultCase() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (a) {default: return 0; case 1: return 1;}",
        ParserErrorCode.SWITCH_HAS_CASE_AFTER_DEFAULT_CASE);
  }

  public void test_switchHasCaseAfterDefaultCase_repeated() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (a) {default: return 0; case 1: return 1; case 2: return 2;}",
        ParserErrorCode.SWITCH_HAS_CASE_AFTER_DEFAULT_CASE,
        ParserErrorCode.SWITCH_HAS_CASE_AFTER_DEFAULT_CASE);
  }

  public void test_switchHasMultipleDefaultCases() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (a) {default: return 0; default: return 1;}",
        ParserErrorCode.SWITCH_HAS_MULTIPLE_DEFAULT_CASES);
  }

  public void test_switchHasMultipleDefaultCases_repeated() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (a) {default: return 0; default: return 1; default: return 2;}",
        ParserErrorCode.SWITCH_HAS_MULTIPLE_DEFAULT_CASES,
        ParserErrorCode.SWITCH_HAS_MULTIPLE_DEFAULT_CASES);
  }

  public void test_topLevelOperator_withoutType() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "operator +(bool x, bool y) => x | y;",
        ParserErrorCode.TOP_LEVEL_OPERATOR);
  }

  public void test_topLevelOperator_withType() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "bool operator +(bool x, bool y) => x | y;",
        ParserErrorCode.TOP_LEVEL_OPERATOR);
  }

  public void test_topLevelOperator_withVoid() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "void operator +(bool x, bool y) => x | y;",
        ParserErrorCode.TOP_LEVEL_OPERATOR);
  }

  public void test_unexpectedTerminatorForParameterGroup_named() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, b})",
        ParserErrorCode.UNEXPECTED_TERMINATOR_FOR_PARAMETER_GROUP);
  }

  public void test_unexpectedTerminatorForParameterGroup_optional() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, b])",
        ParserErrorCode.UNEXPECTED_TERMINATOR_FOR_PARAMETER_GROUP);
  }

  public void test_unexpectedToken_semicolonBetweenClassMembers() throws Exception {
    parse(
        "parseClassDeclaration",
        new Object[] {emptyCommentAndMetadata(), null},
        "class C { int x; ; int y;}",
        ParserErrorCode.UNEXPECTED_TOKEN);
  }

  public void test_unexpectedToken_semicolonBetweenCompilationUnitMembers() throws Exception {
    parseCompilationUnit("int x; ; int y;", ParserErrorCode.UNEXPECTED_TOKEN);
  }

  public void test_useOfUnaryPlusOperator() throws Exception {
    SimpleIdentifier expression = parse(
        "parseUnaryExpression",
        "+x",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression);
    assertTrue(expression.isSynthetic());
  }

  public void test_varAndType_field() throws Exception {
    parseCompilationUnit("class C { var int x; }", ParserErrorCode.VAR_AND_TYPE);
  }

  public void test_varAndType_topLevelVariable() throws Exception {
    parseCompilationUnit("var int x;", ParserErrorCode.VAR_AND_TYPE);
  }

  public void test_varAsTypeName_as() throws Exception {
    parseExpression("x as var", ParserErrorCode.VAR_AS_TYPE_NAME);
  }

  public void test_varClass() throws Exception {
    parseCompilationUnit("var class C {}", ParserErrorCode.VAR_CLASS);
  }

  public void test_varEnum() throws Exception {
    parseCompilationUnit("var enum E {ONE}", ParserErrorCode.VAR_ENUM);
  }

  public void test_varReturnType() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "var m() {}", ParserErrorCode.VAR_RETURN_TYPE);
  }

  public void test_varTypedef() throws Exception {
    parseCompilationUnit("var typedef F();", ParserErrorCode.VAR_TYPEDEF);
  }

  public void test_voidParameter() throws Exception {
    parse("parseNormalFormalParameter", "void a)", ParserErrorCode.VOID_PARAMETER);
  }

  public void test_voidVariable_parseClassMember_initializer() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "void x = 0;", ParserErrorCode.VOID_VARIABLE);
  }

  public void test_voidVariable_parseClassMember_noInitializer() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "void x;", ParserErrorCode.VOID_VARIABLE);
  }

  public void test_voidVariable_parseCompilationUnit_initializer() throws Exception {
    parseCompilationUnit("void x = 0;", ParserErrorCode.VOID_VARIABLE);
  }

  public void test_voidVariable_parseCompilationUnit_noInitializer() throws Exception {
    parseCompilationUnit("void x;", ParserErrorCode.VOID_VARIABLE);
  }

  public void test_voidVariable_parseCompilationUnitMember_initializer() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "void a = 0;",
        ParserErrorCode.VOID_VARIABLE);
  }

  public void test_voidVariable_parseCompilationUnitMember_noInitializer() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "void a;",
        ParserErrorCode.VOID_VARIABLE);
  }

  public void test_voidVariable_statement_initializer() throws Exception {
    parseStatement(
        "void x = 0;",
        ParserErrorCode.VOID_VARIABLE,
        ParserErrorCode.MISSING_CONST_FINAL_VAR_OR_TYPE);
  }

  public void test_voidVariable_statement_noInitializer() throws Exception {
    parseStatement(
        "void x;",
        ParserErrorCode.VOID_VARIABLE,
        ParserErrorCode.MISSING_CONST_FINAL_VAR_OR_TYPE);
  }

  public void test_withBeforeExtends() throws Exception {
    parseCompilationUnit("class A with B extends C {}", ParserErrorCode.WITH_BEFORE_EXTENDS);
  }

  public void test_withWithoutExtends() throws Exception {
    parse(
        "parseClassDeclaration",
        new Object[] {emptyCommentAndMetadata(), null},
        "class A with B, C {}",
        ParserErrorCode.WITH_WITHOUT_EXTENDS);
  }

  public void test_wrongSeparatorForNamedParameter() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, {b = 0})",
        ParserErrorCode.WRONG_SEPARATOR_FOR_NAMED_PARAMETER);
  }

  public void test_wrongSeparatorForPositionalParameter() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, [b : 0])",
        ParserErrorCode.WRONG_SEPARATOR_FOR_POSITIONAL_PARAMETER);
  }

  public void test_wrongTerminatorForParameterGroup_named() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, {b, c])",
        ParserErrorCode.WRONG_TERMINATOR_FOR_PARAMETER_GROUP);
  }

  public void test_wrongTerminatorForParameterGroup_optional() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, [b, c})",
        ParserErrorCode.WRONG_TERMINATOR_FOR_PARAMETER_GROUP);
  }
}
