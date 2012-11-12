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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypedLiteral;
import com.google.dart.engine.internal.parser.CommentAndMetadata;
import com.google.dart.engine.scanner.Token;

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
        new Class[] {Token.class},
        new Object[] {null},
        "1",
        ParserErrorCode.EXPECTED_LIST_OR_MAP_LITERAL);
    assertTrue(literal.isSynthetic());
  }

  public void fail_illegalAssignmentToNonAssignable_superAssigned() throws Exception {
    // TODO(brianwilkerson) When this test starts to pass, remove the test
    // test_illegalAssignmentToNonAssignable_superAssigned.
    parse("parseExpression", "super = x;", ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void fail_invalidCommentReference__new_nonIdentifier() throws Exception {
    // This test fails because the parse method returns null.
    parse(
        "parseCommentReference",
        new Class[] {String.class, int.class},
        new Object[] {"new 42", 0},
        "",
        ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void fail_invalidCommentReference__nonNew_nonIdentifier() throws Exception {
    // This test fails because the parse method returns null.
    parse(
        "parseCommentReference",
        new Class[] {String.class, int.class},
        new Object[] {"42", 0},
        "",
        ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void fail_unexpectedToken_invalidPostfixExpression() throws Exception {
    // Note: this might not be the right error to produce, but some error should be produced
    parse("parseExpression", "f()++", ParserErrorCode.UNEXPECTED_TOKEN);
  }

  public void test_abstractClassMember_constructor() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "abstract C.c();",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_field() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "abstract C f;",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_getter() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "abstract get m;",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_method() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "abstract m();",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
  }

  public void test_abstractClassMember_setter() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "abstract set m(v);",
        ParserErrorCode.ABSTRACT_CLASS_MEMBER);
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
    parse("parseStatement", "for(; x;) {() {break;};}", ParserErrorCode.BREAK_OUTSIDE_OF_LOOP);
  }

  public void test_breakOutsideOfLoop_functionExpression_withALoop() throws Exception {
    parse("parseStatement", "() {for (; x;) {break;}};");
  }

  public void test_builtInIdentifierAsTypeDefName() throws Exception {
    parse(
        "parseTypeAlias",
        new Class[] {CommentAndMetadata.class},
        new Object[] {emptyCommentAndMetadata()},
        "typedef as();",
        ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
  }

  public void test_builtInIdentifierAsTypeName() throws Exception {
    parse(
        "parseClassDeclaration",
        new Class[] {CommentAndMetadata.class},
        new Object[] {emptyCommentAndMetadata()},
        "class as {}",
        ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
  }

  public void test_builtInIdentifierAsTypeVariableName() throws Exception {
    parse("parseTypeParameter", "as", ParserErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
  }

  public void test_constAndFactory() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const factory C() {}",
        ParserErrorCode.CONST_AND_FACTORY);
  }

  public void test_constAndFinal() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const final int x;",
        ParserErrorCode.CONST_AND_FINAL);
  }

  public void test_constAndVar() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const var x;",
        ParserErrorCode.CONST_AND_VAR);
  }

  public void test_constMethod() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const int m() {}",
        ParserErrorCode.CONST_METHOD);
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
    parse("parseStatement", "for(; x;) {() {continue;};}", ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
  }

  public void test_continueOutsideOfLoop_functionExpression_withALoop() throws Exception {
    parse("parseStatement", "() {for (; x;) {continue;}};");
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

  public void test_directiveAfterDeclaration_classBeforeDirective() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "class Foo{} library l;",
        ParserErrorCode.DIRECTIVE_AFTER_DECLARATION);
    assertNotNull(unit);
  }

  public void test_directiveAfterDeclaration_classBetweenDirectives() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "library l;\nclass Foo{}\npart 'a.dart';",
        ParserErrorCode.DIRECTIVE_AFTER_DECLARATION);
    assertNotNull(unit);
  }

  public void test_duplicatedModifier_const() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const const m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_external() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external external f();",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_factory() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "factory factory C() {}",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_final() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "final final m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_static() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "static static m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicatedModifier_var() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "var var m;",
        ParserErrorCode.DUPLICATED_MODIFIER);
  }

  public void test_duplicateLabelInSwitchStatement() throws Exception {
    parse(
        "parseSwitchStatement",
        "switch (e) {l1: case 0: break; l1: case 1: break;}",
        ParserErrorCode.DUPLICATE_LABEL_IN_SWITCH_STATEMENT);
  }

  public void test_expectedCaseOrDefault() throws Exception {
    parse("parseSwitchStatement", "switch (e) {break;}", ParserErrorCode.EXPECTED_CASE_OR_DEFAULT);
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

  public void test_expectedToken_semicolonMissingAfterExpression() throws Exception {
    parse("parseStatement", "x", ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_expectedToken_whileMissingInDoStatement() throws Exception {
    parse("parseStatement", "do {} (x);", ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_exportDirectiveAfterPartDirective() throws Exception {
    parse(
        "parseCompilationUnit",
        "part 'a.dart'; export 'b.dart';",
        ParserErrorCode.EXPORT_DIRECTIVE_AFTER_PART_DIRECTIVE);
  }

  public void test_externalAfterConst() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const external C();",
        ParserErrorCode.EXTERNAL_AFTER_CONST);
  }

  public void test_externalAfterFactory() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "factory external C();",
        ParserErrorCode.EXTERNAL_AFTER_FACTORY);
  }

  public void test_externalAfterStatic() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "static external int m();",
        ParserErrorCode.EXTERNAL_AFTER_STATIC);
  }

  public void test_externalConstructorWithBody_factory() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external factory C() {}",
        ParserErrorCode.EXTERNAL_CONSTRUCTOR_WITH_BODY);
  }

  public void test_externalConstructorWithBody_named() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external C.c() {}",
        ParserErrorCode.EXTERNAL_CONSTRUCTOR_WITH_BODY);
  }

  public void test_externalField_const() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external const A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_final() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external final A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_static() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external static A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_typed() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external A f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalField_untyped() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external var f;",
        ParserErrorCode.EXTERNAL_FIELD);
  }

  public void test_externalGetterWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external int get x {}",
        ParserErrorCode.EXTERNAL_GETTER_WITH_BODY);
  }

  public void test_externalMethodWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external m() {}",
        ParserErrorCode.EXTERNAL_METHOD_WITH_BODY);
  }

  public void test_externalOperatorWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external operator +(int value) {}",
        ParserErrorCode.EXTERNAL_OPERATOR_WITH_BODY);
  }

  public void test_externalSetterWithBody() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "external set x(int value) {}",
        ParserErrorCode.EXTERNAL_SETTER_WITH_BODY);
  }

  public void test_finalAndVar() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "final var x;",
        ParserErrorCode.FINAL_AND_VAR);
  }

  public void test_finalConstructor() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "final C() {}",
        ParserErrorCode.FINAL_CONSTRUCTOR);
  }

  public void test_finalMethod() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "final int m() {}",
        ParserErrorCode.FINAL_METHOD);
  }

  public void test_getterWithParameters() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "int get x() {}",
        ParserErrorCode.GETTER_WITH_PARAMETERS);
  }

  public void test_illegalAssignmentToNonAssignable_superAssigned() throws Exception {
    // TODO(brianwilkerson) When the test fail_illegalAssignmentToNonAssignable_superAssigned starts
    // to pass, remove this test (there should only be one error generated, but we're keeping this
    // test until that time so that we can catch other forms of regressions.
    parse(
        "parseExpression",
        "super = x;",
        ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR,
        ParserErrorCode.ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE);
  }

  public void test_importDirectiveAfterPartDirective() throws Exception {
    parse(
        "parseCompilationUnit",
        "part 'a.dart'; import 'b.dart';",
        ParserErrorCode.IMPORT_DIRECTIVE_AFTER_PART_DIRECTIVE);
  }

  public void test_initializedVariableInForEach() throws Exception {
    parse(
        "parseForStatement",
        "for (int a = 0 in foo) {}",
        ParserErrorCode.INITIALIZED_VARIABLE_IN_FOR_EACH);
  }

  public void test_invalidCodePoint() throws Exception {
    parse("parseStringLiteral", "'\\uD900'", ParserErrorCode.INVALID_CODE_POINT);
  }

  public void test_invalidCommentReference__new_tooMuch() throws Exception {
    parse("parseCommentReference", new Class[] {String.class, int.class}, new Object[] {
        "new a.b.c.d", 0}, "", ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void test_invalidCommentReference__nonNew_tooMuch() throws Exception {
    parse("parseCommentReference", new Class[] {String.class, int.class}, new Object[] {
        "a.b.c.d", 0}, "", ParserErrorCode.INVALID_COMMENT_REFERENCE);
  }

  public void test_invalidHexEscape_invalidDigit() throws Exception {
    parse("parseStringLiteral", "'\\x0 a'", ParserErrorCode.INVALID_HEX_ESCAPE);
  }

  public void test_invalidHexEscape_tooFewDigits() throws Exception {
    parse("parseStringLiteral", "'\\x0'", ParserErrorCode.INVALID_HEX_ESCAPE);
  }

  public void test_invalidOperatorForSuper() throws Exception {
    parse("parseUnaryExpression", "++super", ParserErrorCode.INVALID_OPERATOR_FOR_SUPER);
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
    parse(
        "parseCompilationUnit",
        "import 'x.dart'; library l;",
        ParserErrorCode.LIBRARY_DIRECTIVE_NOT_FIRST);
  }

  public void test_libraryDirectiveNotFirst_afterPart() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "part 'a.dart';\nlibrary l;",
        ParserErrorCode.LIBRARY_DIRECTIVE_NOT_FIRST);
    assertNotNull(unit);
  }

  public void test_missingAssignableSelector_identifiersAssigned() throws Exception {
    parse("parseExpression", "x.y = y;");
  }

  public void test_missingAssignableSelector_primarySelectorPostfix() throws Exception {
    parse("parseExpression", "x(y)(z)++", ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
  }

  public void test_missingAssignableSelector_selector() throws Exception {
    parse("parseExpression", "x(y)(z).a++");
  }

  public void test_missingAssignableSelector_superPrimaryExpression() throws Exception {
    SuperExpression expression = parse(
        "parsePrimaryExpression",
        "super",
        ParserErrorCode.MISSING_ASSIGNABLE_SELECTOR);
    assertNotNull(expression.getKeyword());
  }

  public void test_missingAssignableSelector_superPropertyAccessAssigned() throws Exception {
    parse("parseExpression", "super.x = x;");
  }

  public void test_missingCatchOrFinally() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {}",
        ParserErrorCode.MISSING_CATCH_OR_FINALLY);
    assertNotNull(statement);
  }

  public void test_missingConstFinalVarOrType() throws Exception {
    parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "a;",
        ParserErrorCode.MISSING_CONST_FINAL_VAR_OR_TYPE);
  }

  public void test_missingFunctionBody_emptyNotAllowed() throws Exception {
    parse("parseFunctionBody", new Class[] {boolean.class, boolean.class}, new Object[] {
        false, false}, ";", ParserErrorCode.MISSING_FUNCTION_BODY);
  }

  public void test_missingFunctionBody_invalid() throws Exception {
    parse("parseFunctionBody", new Class[] {boolean.class, boolean.class}, new Object[] {
        false, false}, "return 0;", ParserErrorCode.MISSING_FUNCTION_BODY);
  }

  public void test_missingIdentifier_number() throws Exception {
    SimpleIdentifier expression = parse(
        "parseSimpleIdentifier",
        "1",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertTrue(expression.isSynthetic());
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

  public void test_multipleLibraryDirectives() throws Exception {
    parse(
        "parseCompilationUnit",
        "library l; library m;",
        ParserErrorCode.MULTIPLE_LIBRARY_DIRECTIVES);
  }

  public void test_multipleNamedParameterGroups() throws Exception {
    parse(
        "parseFormalParameterList",
        "(a, {b}, {c})",
        ParserErrorCode.MULTIPLE_NAMED_PARAMETER_GROUPS);
  }

  public void test_multiplePartOfDirectives() throws Exception {
    parse(
        "parseCompilationUnit",
        "part of l; part of m;",
        ParserErrorCode.MULTIPLE_PART_OF_DIRECTIVES);
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

  public void test_namedParameterOutsideGroup() throws Exception {
    parse("parseFormalParameterList", "(a, b : 0)", ParserErrorCode.NAMED_PARAMETER_OUTSIDE_GROUP);
  }

  public void test_nonConstructorFactory_field() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "factory int x;",
        ParserErrorCode.NON_CONSTRUCTOR_FACTORY);
  }

  public void test_nonConstructorFactory_method() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "factory int m() {}",
        ParserErrorCode.NON_CONSTRUCTOR_FACTORY);
  }

  public void test_nonPartOfDirectiveInPart_after() throws Exception {
    parse(
        "parseCompilationUnit",
        "part of l; part 'f.dart';",
        ParserErrorCode.NON_PART_OF_DIRECTIVE_IN_PART);
  }

  public void test_nonPartOfDirectiveInPart_before() throws Exception {
    parse(
        "parseCompilationUnit",
        "part 'f.dart'; part of m;",
        ParserErrorCode.NON_PART_OF_DIRECTIVE_IN_PART);
  }

  public void test_nonUserDefinableOperator() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "operator +=(int x) => x + 1;",
        ParserErrorCode.NON_USER_DEFINABLE_OPERATOR);
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

  public void test_staticAfterConst() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "final static int f;",
        ParserErrorCode.STATIC_AFTER_FINAL);
  }

  public void test_staticAfterFinal() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "const static int f;",
        ParserErrorCode.STATIC_AFTER_CONST);
  }

  public void test_staticAfterVar() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "var static f;",
        ParserErrorCode.STATIC_AFTER_VAR);
  }

  public void test_staticConstructor() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "static C.m() {}",
        ParserErrorCode.STATIC_CONSTRUCTOR);
  }

  public void test_staticOperator_noReturnType() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "static operator +(int x) => x + 1;",
        ParserErrorCode.STATIC_OPERATOR);
  }

  public void test_staticOperator_returnType() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "static int operator +(int x) => x + 1;",
        ParserErrorCode.STATIC_OPERATOR);
  }

  public void test_staticTopLevelDeclaration() throws Exception {
    parse(
        "parseCompilationUnitMember",
        new Class[] {CommentAndMetadata.class},
        new Object[] {emptyCommentAndMetadata()},
        "static var x;",
        ParserErrorCode.STATIC_TOP_LEVEL_DECLARATION);
  }

  public void test_unexpectedToken_semicolonBetweenClassMembers() throws Exception {
    parse(
        "parseClassDeclaration",
        new Class[] {CommentAndMetadata.class},
        new Object[] {emptyCommentAndMetadata()},
        "class C { int x; ; int y;}",
        ParserErrorCode.UNEXPECTED_TOKEN);
  }

  public void test_unexpectedToken_semicolonBetweenCompilationUnitMembers() throws Exception {
    parse("parseCompilationUnit", "int x; ; int y;", ParserErrorCode.UNEXPECTED_TOKEN);
  }

  public void test_useOfUnaryPlusOperator() throws Exception {
    parse("parseUnaryExpression", "+x", ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
  }

  public void test_varConstructor() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "var C() {}",
        ParserErrorCode.CONSTRUCTOR_WITH_RETURN_TYPE);
  }

  public void test_varReturnType() throws Exception {
    parse(
        "parseClassMember",
        new Class[] {String.class},
        new Object[] {"C"},
        "var m() {}",
        ParserErrorCode.VAR_RETURN_TYPE);
  }

  public void test_voidParameter() throws Exception {
    parse("parseNormalFormalParameter", "void a)", ParserErrorCode.VOID_PARAMETER);
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
}
