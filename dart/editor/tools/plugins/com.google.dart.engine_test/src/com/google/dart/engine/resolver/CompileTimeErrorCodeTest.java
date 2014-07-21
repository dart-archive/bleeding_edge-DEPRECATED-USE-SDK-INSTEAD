/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;

public class CompileTimeErrorCodeTest extends ResolverTestCase {
  public void fail_accessPrivateEnumField() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "String name(E e) {",
        "  return e._name;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.ACCESS_PRIVATE_ENUM_FIELD);
    verify(source);
  }

  public void fail_compileTimeConstantRaisesException() throws Exception {
    Source source = addSource(createSource(//
    // TODO Find an expression that would raise an exception
    ));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.COMPILE_TIME_CONSTANT_RAISES_EXCEPTION);
    verify(source);
  }

  public void fail_constEvalThrowsException() throws Exception { // Not compile-time constant
    Source source = addSource(createSource(//
        "class C {",
        "  const C();",
        "}",
        "f() { return const C(); }"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_CONSTRUCTOR_THROWS_EXCEPTION);
    verify(source);
  }

  public void fail_extendsEnum() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "class A extends E {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_ENUM);
    verify(source);
  }

  public void fail_implementsEnum() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "class A implements E {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_ENUM);
    verify(source);
  }

  public void fail_instantiateEnum_const() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "E e(String name) {",
        "  const E(0, name);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANTIATE_ENUM);
    verify(source);
  }

  public void fail_instantiateEnum_new() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "E e(String name) {",
        "  new E(0, name);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANTIATE_ENUM);
    verify(source);
  }

  public void fail_missingEnumConstantInSwitch() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE, TWO, THREE, FOUR }",
        "bool odd(E e) {",
        "  switch (e) {",
        "    case ONE:",
        "    case THREE: return true;",
        "  }",
        "  return false;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.MISSING_ENUM_CONSTANT_IN_SWITCH,
        CompileTimeErrorCode.MISSING_ENUM_CONSTANT_IN_SWITCH);
    verify(source);
  }

  public void fail_mixinDeclaresConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "class B extends Object mixin A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR);
    verify(source);
  }

  public void fail_mixinOfEnum() throws Exception {
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "class A with E {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_ENUM);
    verify(source);
  }

  public void fail_mixinOfNonClass() throws Exception {
    // TODO(brianwilkerson) Compare with MIXIN_WITH_NON_CLASS_SUPERCLASS.
    Source source = addSource(createSource(//
        "var A;",
        "class B extends Object mixin A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
    verify(source);
  }

  public void fail_objectCannotExtendAnotherClass() throws Exception {
    Source source = addSource(createSource(//
    // TODO(brianwilkerson) Figure out how to mock Object
    ));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.OBJECT_CANNOT_EXTEND_ANOTHER_CLASS);
    verify(source);
  }

  public void fail_recursiveCompileTimeConstant() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "  final m = const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_COMPILE_TIME_CONSTANT);
    verify(source);
  }

  public void fail_recursiveCompileTimeConstant_cycle() throws Exception {
    Source source = addSource(createSource(//
        "const x = y + 1;",
        "const y = x + 1;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_COMPILE_TIME_CONSTANT);
    verify(source);
  }

  public void fail_superInitializerInObject() throws Exception {
    Source source = addSource(createSource(//
    // TODO(brianwilkerson) Figure out how to mock Object
    ));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_INITIALIZER_IN_OBJECT);
    verify(source);
  }

  public void test_ambiguousExport() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.AMBIGUOUS_EXPORT);
    verify(source);
  }

  public void test_builtInIdentifierAsMixinName_classTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class as = A with B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    verify(source);
  }

  public void test_builtInIdentifierAsType_formalParameter_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var x;",
        "  A(static this.x);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE);
    verify(source);
  }

  public void test_builtInIdentifierAsType_formalParameter_simple() throws Exception {
    Source source = addSource(createSource(//
        "f(static x) {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE);
    verify(source);
  }

  public void test_builtInIdentifierAsType_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  typedef x;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE);
    verify(source);
  }

  public void test_builtInIdentifierAsTypedefName_functionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
    "typedef bool as();"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    verify(source);
  }

  public void test_builtInIdentifierAsTypeName() throws Exception {
    Source source = addSource(createSource(//
    "class as {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
    verify(source);
  }

  public void test_builtInIdentifierAsTypeParameterName() throws Exception {
    Source source = addSource(createSource(//
    "class A<as> {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_PARAMETER_NAME);
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals() throws Exception {
    Source source = addSource(createSource(//
        "class IntWrapper {",
        "  final int value;",
        "  const IntWrapper(this.value);",
        "  bool operator ==(IntWrapper x) {",
        "    return value == x.value;",
        "  }",
        "  get hashCode => value;",
        "}",
        "",
        "f(var a) {",
        "  switch(a) {",
        "    case(const IntWrapper(1)) : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS);
    verify(source);
  }

  public void test_conflictingConstructorNameAndMember_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A.x() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD);
    verify(source);
  }

  public void test_conflictingConstructorNameAndMember_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A.x();",
        "  void x() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD);
    verify(source);
  }

  public void test_conflictingGetterAndMethod_field_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int m = 0;",
        "}",
        "class B extends A {",
        "  m() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_GETTER_AND_METHOD);
    verify(source);
  }

  public void test_conflictingGetterAndMethod_getter_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get m => 0;",
        "}",
        "class B extends A {",
        "  m() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_GETTER_AND_METHOD);
    verify(source);
  }

  public void test_conflictingGetterAndMethod_method_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  int m;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_METHOD_AND_GETTER);
    verify(source);
  }

  public void test_conflictingGetterAndMethod_method_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  get m => 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_METHOD_AND_GETTER);
    verify(source);
  }

  public void test_conflictingTypeVariableAndClass() throws Exception {
    Source source = addSource(createSource(//
        "class T<T> {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_CLASS);
    verify(source);
  }

  public void test_conflictingTypeVariableAndMember_field() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  var T;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER);
    verify(source);
  }

  public void test_conflictingTypeVariableAndMember_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  get T => null;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER);
    verify(source);
  }

  public void test_conflictingTypeVariableAndMember_method() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  T() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER);
    verify(source);
  }

  public void test_conflictingTypeVariableAndMember_method_static() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  static T() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER);
    verify(source);
  }

  public void test_conflictingTypeVariableAndMember_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  set T(x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONFLICTING_TYPE_VARIABLE_AND_MEMBER);
    verify(source);
  }

  public void test_consistentCaseExpressionTypes_dynamic() throws Exception {
    // Even though A.S and S have a static type of "dynamic", we should see
    // that they match 'abc', because they are constant strings.
    Source source = addSource(createSource(//
        "class A {",
        "  static const S = 'A.S';",
        "}",
        "",
        "const S = 'S';",
        "",
        "foo(var p) {",
        "  switch (p) {",
        "    case S:",
        "      break;",
        "    case A.S:",
        "      break;",
        "    case 'abc':",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_constConstructorWithFieldInitializedByNonConst() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int i = f();",
        "  const A();",
        "}",
        "int f() {",
        "  return 3;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_FIELD_INITIALIZED_BY_NON_CONST);
    verify(source);
  }

  public void test_constConstructorWithFieldInitializedByNonConst_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static final int i = f();",
        "  const A();",
        "}",
        "int f() {",
        "  return 3;",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_constConstructorWithMixin() throws Exception {
    Source source = addSource(createSource(//
        "class M {",
        "}",
        "class A extends Object with M {",
        "  const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_MIXIN);
    verify(source);
  }

  public void test_constConstructorWithNonConstSuper_explicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A();",
        "}",
        "class B extends A {",
        "  const B(): super();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER);
    verify(source);
  }

  public void test_constConstructorWithNonConstSuper_implicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A();",
        "}",
        "class B extends A {",
        "  const B();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER);
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_mixin() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var a;",
        "}",
        "class B extends Object with A {",
        "  const B();",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_MIXIN,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD);
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var a;",
        "}",
        "class B extends A {",
        "  const B();",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD,
        CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_CONST_SUPER);
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_this() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD);
    verify(source);
  }

  public void test_constDeferredClass() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {",
            "  const A();",
            "}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "main() {",
                "  const a.A();",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.CONST_DEFERRED_CLASS});
  }

  public void test_constDeferredClass_namedConstructor() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {",
            "  const A.b();",
            "}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "main() {",
                "  const a.A.b();",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.CONST_DEFERRED_CLASS});
  }

  public void test_constEval_newInstance_constConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "const a = new A();"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    verify(source);
  }

  public void test_constEval_newInstance_externalFactoryConstConstructor() throws Exception {
    // We can't evaluate "const A()" because its constructor is external.  But
    // the code is correct--we shouldn't report an error.
    Source source = addSource(createSource(//
        "class A {",
        "  external factory const A();",
        "}",
        "const x = const A();"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_constEval_propertyExtraction_targetNotConst() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "  m() {}",
        "}",
        "final a = const A();",
        "const C = a.m;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    verify(source);
  }

  public void test_constEvalThrowsException_binaryMinus_null() throws Exception {
    check_constEvalThrowsException_binary_null("null - 5", false);
    check_constEvalThrowsException_binary_null("5 - null", true);
  }

  public void test_constEvalThrowsException_binaryPlus_null() throws Exception {
    check_constEvalThrowsException_binary_null("null + 5", false);
    check_constEvalThrowsException_binary_null("5 + null", true);
  }

  public void test_constEvalThrowsException_divisionByZero() throws Exception {
    Source source = addSource("const C = 1 ~/ 0;");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_EVAL_THROWS_IDBZE);
    verify(source);
  }

  public void test_constEvalThrowsException_unaryBitNot_null() throws Exception {
    Source source = addSource("const C = ~null;");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
    // no verify(), '~null' is not resolved
  }

  public void test_constEvalThrowsException_unaryNegated_null() throws Exception {
    Source source = addSource("const C = -null;");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
    // no verify(), '-null' is not resolved
  }

  public void test_constEvalThrowsException_unaryNot_null() throws Exception {
    Source source = addSource("const C = !null;");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
    verify(source);
  }

  public void test_constEvalTypeBool_binary() throws Exception {
    check_constEvalTypeBool_withParameter_binary("p && ''");
    check_constEvalTypeBool_withParameter_binary("p || ''");
  }

  public void test_constEvalTypeBool_binary_leftTrue() throws Exception {
    Source source = addSource("const C = (true || 0);");
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL,
        StaticTypeWarningCode.NON_BOOL_OPERAND,
        HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_constEvalTypeBoolNumString_equal() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "class B {",
        "  final a;",
        "  const B(num p) : a = p == const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING);
    verify(source);
  }

  public void test_constEvalTypeBoolNumString_notEqual() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "class B {",
        "  final a;",
        "  const B(String p) : a = p != const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL_NUM_STRING);
    verify(source);
  }

  public void test_constEvalTypeInt_binary() throws Exception {
    check_constEvalTypeInt_withParameter_binary("p ^ ''");
    check_constEvalTypeInt_withParameter_binary("p & ''");
    check_constEvalTypeInt_withParameter_binary("p | ''");
    check_constEvalTypeInt_withParameter_binary("p >> ''");
    check_constEvalTypeInt_withParameter_binary("p << ''");
  }

  public void test_constEvalTypeNum_binary() throws Exception {
    check_constEvalTypeNum_withParameter_binary("p + ''");
    check_constEvalTypeNum_withParameter_binary("p - ''");
    check_constEvalTypeNum_withParameter_binary("p * ''");
    check_constEvalTypeNum_withParameter_binary("p / ''");
    check_constEvalTypeNum_withParameter_binary("p ~/ ''");
    check_constEvalTypeNum_withParameter_binary("p > ''");
    check_constEvalTypeNum_withParameter_binary("p < ''");
    check_constEvalTypeNum_withParameter_binary("p >= ''");
    check_constEvalTypeNum_withParameter_binary("p <= ''");
    check_constEvalTypeNum_withParameter_binary("p % ''");
  }

  public void test_constFormalParameter_fieldFormalParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var x;",
        "  A(const this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_FORMAL_PARAMETER);
    verify(source);
  }

  public void test_constFormalParameter_simpleFormalParameter() throws Exception {
    Source source = addSource(createSource(//
    "f(const x) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_FORMAL_PARAMETER);
    verify(source);
  }

  public void test_constInitializedWithNonConstValue() throws Exception {
    Source source = addSource(createSource(//
        "f(p) {",
        "  const C = p;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    verify(source);
  }

  public void test_constInitializedWithNonConstValue_missingConstInListLiteral() throws Exception {
    Source source = addSource("const List L = [0];");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    verify(source);
  }

  public void test_constInitializedWithNonConstValue_missingConstInMapLiteral() throws Exception {
    Source source = addSource("const Map M = {'a' : 0};");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    verify(source);
  }

  public void test_constInitializedWithNonConstValueFromDeferredClass() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const V = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "const B = a.V;")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE_FROM_DEFERRED_LIBRARY});
  }

  public void test_constInitializedWithNonConstValueFromDeferredClass_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const V = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "const B = a.V + 1;")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE_FROM_DEFERRED_LIBRARY});
  }

  public void test_constInstanceField() throws Exception {
    Source source = addSource(createSource(//
        "class C {",
        "  const int f = 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_INSTANCE_FIELD);
    verify(source);
  }

  public void test_constMapKeyTypeImplementsEquals_direct() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "  operator ==(other) => false;",
        "}",
        "main() {",
        "  const {const A() : 0};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS);
    verify(source);
  }

  public void test_constMapKeyTypeImplementsEquals_dynamic() throws Exception {
    // Note: static type of B.a is "dynamic", but actual type of the const
    // object is A.  We need to make sure we examine the actual type when
    // deciding whether there is a problem with operator==.
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "  operator ==(other) => false;",
        "}",
        "class B {",
        "  static const a = const A();",
        "}",
        "main() {",
        "  const {B.a : 0};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS);
    verify(source);
  }

  public void test_constMapKeyTypeImplementsEquals_factory() throws Exception {
    Source source = addSource(createSource(//
        "class A { const factory A() = B; }",
        "",
        "class B implements A {",
        "  const B();",
        "",
        "  operator ==(o) => true;",
        "}",
        "",
        "main() {",
        "  var m = const { const A(): 42 };",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS);
    verify(source);
  }

  public void test_constMapKeyTypeImplementsEquals_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "  operator ==(other) => false;",
        "}",
        "class B extends A {",
        "  const B();",
        "}",
        "main() {",
        "  const {const B() : 0};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_MAP_KEY_EXPRESSION_TYPE_IMPLEMENTS_EQUALS);
    verify(source);
  }

  public void test_constWithInvalidTypeParameters() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "f() { return const A<A>(); }"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_constWithInvalidTypeParameters_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C<K, V> {",
        "  const C();",
        "}",
        "f(p) {",
        "  return const C<A>();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_constWithInvalidTypeParameters_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C<E> {",
        "  const C();",
        "}",
        "f(p) {",
        "  return const C<A, A>();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_constWithNonConst() throws Exception {
    Source source = addSource(createSource(//
        "class T {",
        "  T(a, b, {c, d}) {}",
        "}",
        "f() { return const T(0, 1, c: 2, d: 3); }"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_NON_CONST);
    verify(source);
  }

  public void test_constWithNonConstantArgument_annotation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(int p);",
        "}",
        "var v = 42;",
        "@A(v)",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_NON_CONSTANT_ARGUMENT);
    verify(source);
  }

  public void test_constWithNonConstantArgument_instanceCreation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(a);",
        "}",
        "f(p) { return const A(p); }"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_NON_CONSTANT_ARGUMENT);
    verify(source);
  }

  public void test_constWithNonType() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "f() {",
        "  return const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_NON_TYPE);
    verify(source);
  }

  public void test_constWithNonType_fromLibrary() throws Exception {
    Source source1 = addNamedSource("lib.dart", "");
    Source source2 = addNamedSource("lib2.dart", createSource(//
        "import 'lib.dart' as lib;",
        "void f() {",
        "  const lib.A();",
        "}"));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, CompileTimeErrorCode.CONST_WITH_NON_TYPE);
    verify(source1);
  }

  public void test_constWithTypeParameters_direct() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  static const V = const A<T>();",
        "  const A();",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_WITH_TYPE_PARAMETERS,
        StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_constWithTypeParameters_indirect() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  static const V = const A<List<T>>();",
        "  const A();",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_WITH_TYPE_PARAMETERS,
        StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_constWithUndefinedConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "f() {",
        "  return const A.noSuchConstructor();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR);
    // no verify(), 'noSuchConstructor' is not resolved
  }

  public void test_constWithUndefinedConstructorDefault() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A.name();",
        "}",
        "f() {",
        "  return const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT);
    verify(source);
  }

  public void test_defaultValueInFunctionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
    "typedef F([x = 0]);"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS);
    verify(source);
  }

  public void test_defaultValueInFunctionTypedParameter_named() throws Exception {
    Source source = addSource(createSource(//
    "f(g({p: null})) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER);
    verify(source);
  }

  public void test_defaultValueInFunctionTypedParameter_optional() throws Exception {
    Source source = addSource(createSource(//
    "f(g([p = null])) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPED_PARAMETER);
    verify(source);
  }

  public void test_defaultValueInRedirectingFactoryConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A([int x = 0]) = B;",
        "}",
        "",
        "class B implements A {",
        "  B([int x = 1]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DEFAULT_VALUE_IN_REDIRECTING_FACTORY_CONSTRUCTOR);
    verify(source);
  }

  public void test_duplicateConstructorName_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.a() {}",
        "  A.a() {}",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_NAME,
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_NAME);
    verify(source);
  }

  public void test_duplicateConstructorName_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "  A() {}",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_DEFAULT,
        CompileTimeErrorCode.DUPLICATE_CONSTRUCTOR_DEFAULT);
    verify(source);
  }

  public void test_duplicateDefinition() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  int m = 0;",
        "  m(a) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinition_acrossLibraries() throws Exception {
    Source librarySource = addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "",
        "part 'a.dart';",
        "part 'b.dart';"));
    Source sourceA = addNamedSource("/a.dart", createSource(//
        "part of lib;",
        "",
        "class A {}"));
    Source sourceB = addNamedSource("/b.dart", createSource(//
        "part of lib;",
        "",
        "class A {}"));
    resolve(librarySource);
    assertErrors(sourceB, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(librarySource, sourceA, sourceB);
  }

  public void test_duplicateDefinition_classMembers_fields() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int a;",
        "  int a;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinition_classMembers_fields_oneStatic() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  static int x;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinition_classMembers_methods() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "  m() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinition_localFields() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {",
        "    int a;",
        "    int a;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinition_parameterWithFunctionName_local() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  f(f) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinition_parameterWithFunctionName_topLevel() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  f(f) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void test_duplicateDefinitionInheritance_instanceGetter_staticGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get x => 0;",
        "}",
        "class B extends A {",
        "  static int get x => 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE);
    verify(source);
  }

  public void test_duplicateDefinitionInheritance_instanceGetterAbstract_staticGetter()
      throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int get x;",
        "}",
        "class B extends A {",
        "  static int get x => 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE);
    verify(source);
  }

  public void test_duplicateDefinitionInheritance_instanceMethod_staticMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  x() {}",
        "}",
        "class B extends A {",
        "  static x() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE);
    verify(source);
  }

  public void test_duplicateDefinitionInheritance_instanceMethodAbstract_staticMethod()
      throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  x();",
        "}",
        "abstract class B extends A {",
        "  static x() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE);
    verify(source);
  }

  public void test_duplicateDefinitionInheritance_instanceSetter_staticSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x(value) {}",
        "}",
        "class B extends A {",
        "  static set x(value) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE);
    verify(source);
  }

  public void test_duplicateDefinitionInheritance_instanceSetterAbstract_staticSetter()
      throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  set x(value);",
        "}",
        "class B extends A {",
        "  static set x(value) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_DEFINITION_INHERITANCE);
    verify(source);
  }

  public void test_duplicateNamedArgument() throws Exception {
    Source source = addSource(createSource(//
        "f({a, b}) {}",
        "main() {",
        "  f(a: 1, a: 2);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.DUPLICATE_NAMED_ARGUMENT);
    verify(source);
  }

  public void test_exportInternalLibrary() throws Exception {
    Source source = addSource(createSource(//
    "export 'dart:_interceptors';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXPORT_INTERNAL_LIBRARY);
    verify(source);
  }

  public void test_exportOfNonLibrary() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';"));
    addNamedSource("/lib1.dart", createSource(//
        "part of lib;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXPORT_OF_NON_LIBRARY);
    verify(source);
  }

  public void test_extendsDeferredClass() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class B extends a.A {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.EXTENDS_DEFERRED_CLASS});
  }

  public void test_extendsDeferredClass_classTypeAlias() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class M {}",
                "class C = a.A with M;")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.EXTENDS_DEFERRED_CLASS});
  }

  public void test_extendsDisallowedClass_class_bool() throws Exception {
    Source source = addSource(createSource(//
    "class A extends bool {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_extendsDisallowedClass_class_double() throws Exception {
    Source source = addSource(createSource(//
    "class A extends double {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_extendsDisallowedClass_class_int() throws Exception {
    Source source = addSource(createSource(//
    "class A extends int {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_extendsDisallowedClass_class_Null() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Null {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_extendsDisallowedClass_class_num() throws Exception {
    Source source = addSource(createSource(//
    "class A extends num {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_extendsDisallowedClass_class_String() throws Exception {
    Source source = addSource(createSource(//
    "class A extends String {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS,
        CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_extendsDisallowedClass_classTypeAlias_bool() throws Exception {
    Source source = addSource(createSource(//
        "class M {}",
        "class C = bool with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsDisallowedClass_classTypeAlias_double() throws Exception {
    Source source = addSource(createSource(//
        "class M {}",
        "class C = double with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsDisallowedClass_classTypeAlias_int() throws Exception {
    Source source = addSource(createSource(//
        "class M {}",
        "class C = int with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsDisallowedClass_classTypeAlias_Null() throws Exception {
    Source source = addSource(createSource(//
        "class M {}",
        "class C = Null with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsDisallowedClass_classTypeAlias_num() throws Exception {
    Source source = addSource(createSource(//
        "class M {}",
        "class C = num with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsDisallowedClass_classTypeAlias_String() throws Exception {
    Source source = addSource(createSource(//
        "class M {}",
        "class C = String with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsNonClass_class() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B extends A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_NON_CLASS);
    verify(source);
  }

  public void test_extendsNonClass_dynamic() throws Exception {
    Source source = addSource(createSource(//
    "class B extends dynamic {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_NON_CLASS);
    verify(source);
  }

  public void test_extraPositionalArguments_const() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "main() {",
        "  const A(0);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTRA_POSITIONAL_ARGUMENTS);
    verify(source);
  }

  public void test_extraPositionalArguments_const_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "class B extends A {",
        "  const B() : super(0);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTRA_POSITIONAL_ARGUMENTS);
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A() : x = 0, x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers_multipleInits() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A() : x = 0, x = 1, x = 2 {}",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers_multipleNames() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  int y;",
        "  A() : x = 0, x = 1, y = 0, y = 1 {}",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  public void test_fieldInitializedInParameterAndInitializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A(this.x) : x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER);
    verify(source);
  }

  public void test_fieldInitializerFactoryConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  factory A(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZER_FACTORY_CONSTRUCTOR);
    verify(source);
  }

  public void test_fieldInitializerNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x;",
        "  const A() : x = '';",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_FIELD_INITIALIZER_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor() throws Exception {
    // TODO(brianwilkerson) Fix the duplicate error messages.
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  m(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(
        source,
        ParserErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR,
        CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR);
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor_defaultParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  m([this.x]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR);
    verify(source);
  }

  public void test_fieldInitializerRedirectingConstructor_afterRedirection() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A.named() {}",
        "  A() : this.named(), x = 42;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR);
    verify(source);
  }

  public void test_fieldInitializerRedirectingConstructor_beforeRedirection() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A.named() {}",
        "  A() : x = 42, this.named();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR);
    verify(source);
  }

  public void test_fieldInitializingFormalRedirectingConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A.named() {}",
        "  A(this.x) : this.named();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZER_REDIRECTING_CONSTRUCTOR);
    verify(source);
  }

  public void test_finalInitializedMultipleTimes_initializers() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x;",
        "  A() : x = 0, x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  /**
   * This test doesn't test the FINAL_INITIALIZED_MULTIPLE_TIMES code, but tests the
   * FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER code instead. It is provided here to show
   * coverage over all of the permutations of initializers in constructor declarations.
   * <p>
   * Note: FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER covers a subset of
   * FINAL_INITIALIZED_MULTIPLE_TIMES, since it more specific, we use it instead of the broader code
   */
  public void test_finalInitializedMultipleTimes_initializingFormal_initializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x;",
        "  A(this.x) : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER);
    verify(source);
  }

  public void test_finalInitializedMultipleTimes_initializingFormals() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x;",
        "  A(this.x, this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.FINAL_INITIALIZED_MULTIPLE_TIMES);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_const_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static const F;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_library_const() throws Exception {
    Source source = addSource(createSource(//
    "const F;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_local_const() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  const int x;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_NOT_INITIALIZED);
    verify(source);
  }

  public void test_fromEnvironment_bool_badArgs() throws Exception {
    Source source = addSource(createSource(//
        "var b1 = const bool.fromEnvironment(1);",
        "var b2 = const bool.fromEnvironment('x', defaultValue: 1);"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE,
        CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_fromEnvironment_bool_badDefault_whenDefined() throws Exception {
    // The type of the defaultValue needs to be correct even when the default value
    // isn't used (because the variable is defined in the environment).
    analysisContext.getDeclaredVariables().define("x", "true");
    Source source = addSource(createSource(//
    "var b = const bool.fromEnvironment('x', defaultValue: 1);"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_getterAndMethodWithSameName() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  x(y) {}",
        "  get x => 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.GETTER_AND_METHOD_WITH_SAME_NAME);
    verify(source);
  }

  public void test_implementsDeferredClass() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class B implements a.A {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.IMPLEMENTS_DEFERRED_CLASS});
  }

  public void test_implementsDeferredClass_classTypeAlias() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class B {}",
                "class M {}",
                "class C = B with M implements a.A;")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.IMPLEMENTS_DEFERRED_CLASS});
  }

  public void test_implementsDisallowedClass_class_bool() throws Exception {
    Source source = addSource(createSource(//
    "class A implements bool {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_class_double() throws Exception {
    Source source = addSource(createSource(//
    "class A implements double {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_class_int() throws Exception {
    Source source = addSource(createSource(//
    "class A implements int {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_class_Null() throws Exception {
    Source source = addSource(createSource(//
    "class A implements Null {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_class_num() throws Exception {
    Source source = addSource(createSource(//
    "class A implements num {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_class_String() throws Exception {
    Source source = addSource(createSource(//
    "class A implements String {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_class_String_num() throws Exception {
    Source source = addSource(createSource(//
    "class A implements String, num {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS,
        CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_bool() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements bool;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_double() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements double;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_int() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements int;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_Null() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements Null;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_num() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements num;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_String() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements String;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDisallowedClass_classTypeAlias_String_num() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class C = A with M implements String, num;"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS,
        CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_implementsDynamic() throws Exception {
    Source source = addSource(createSource(//
    "class A implements dynamic {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DYNAMIC);
    verify(source);
  }

  public void test_implementsNonClass_class() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B implements A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_NON_CLASS);
    verify(source);
  }

  public void test_implementsNonClass_typeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "int B;",
        "class C = A with M implements B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_NON_CLASS);
    verify(source);
  }

  public void test_implementsRepeated() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B implements A, A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_REPEATED);
    verify(source);
  }

  public void test_implementsRepeated_3times() throws Exception {
    Source source = addSource(createSource(//
        "class A {} class C{}",
        "class B implements A, A, A, A {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED);
    verify(source);
  }

  public void test_implementsSuperClass() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A implements A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_SUPER_CLASS);
    verify(source);
  }

  public void test_implementsSuperClass_Object() throws Exception {
    Source source = addSource(createSource(//
    "class A implements Object {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_SUPER_CLASS);
    verify(source);
  }

  public void test_implicitThisReferenceInInitializer_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var v;",
        "  A() : v = f;",
        "  var f;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER);
    verify(source);
  }

  public void test_implicitThisReferenceInInitializer_field2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x = 0;",
        "  final y = x;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER);
    verify(source);
  }

  public void test_implicitThisReferenceInInitializer_invocation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var v;",
        "  A() : v = f();",
        "  f() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER);
    verify(source);
  }

  public void test_implicitThisReferenceInInitializer_invocationInStatic() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static var F = m();",
        "  m() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER);
    verify(source);
  }

  public void test_implicitThisReferenceInInitializer_redirectingConstructorInvocation()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(p) {}",
        "  A.named() : this(f);",
        "  var f;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER);
    verify(source);
  }

  public void test_implicitThisReferenceInInitializer_superConstructorInvocation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(p) {}",
        "}",
        "class B extends A {",
        "  B() : super(f);",
        "  var f;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLICIT_THIS_REFERENCE_IN_INITIALIZER);
    verify(source);
  }

  public void test_importInternalLibrary() throws Exception {
    Source source = addSource(createSource(//
    "import 'dart:_interceptors';"));
    resolve(source);
    // Note, in these error cases we may generate an UNUSED_IMPORT hint, while we could prevent
    // the hint from being generated by testing the import directive for the error, this is such a
    // minor corner case that we don't think we should add the additional computation time to figure
    // out such cases.
    assertErrors(source, CompileTimeErrorCode.IMPORT_INTERNAL_LIBRARY, HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_importInternalLibrary_js_helper() throws Exception {
    Source source = addSource(createSource(//
    "import 'dart:_js_helper';"));
    resolve(source);
    // Note, in these error cases we may generate an UNUSED_IMPORT hint, while we could prevent
    // the hint from being generated by testing the import directive for the error, this is such a
    // minor corner case that we don't think we should add the additional computation time to figure
    // out such cases.
    assertErrors(source, CompileTimeErrorCode.IMPORT_INTERNAL_LIBRARY, HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_importOfNonLibrary() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "import 'part.dart';",
        "A a;"));
    addNamedSource("/part.dart", createSource(//
        "part of lib;",
        "class A{}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPORT_OF_NON_LIBRARY);
    verify(source);
  }

  public void test_inconsistentCaseExpressionTypes() throws Exception {
    Source source = addSource(createSource(//
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 'a':",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES);
    verify(source);
  }

  public void test_inconsistentCaseExpressionTypes_dynamic() throws Exception {
    // Even though A.S and S have a static type of "dynamic", we should see
    // that they fail to match 3, because they are constant strings.
    Source source = addSource(createSource(//
        "class A {",
        "  static const S = 'A.S';",
        "}",
        "",
        "const S = 'S';",
        "",
        "foo(var p) {",
        "  switch (p) {",
        "    case 3:",
        "      break;",
        "    case S:",
        "      break;",
        "    case A.S:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES);
    verify(source);
  }

  public void test_inconsistentCaseExpressionTypes_repeated() throws Exception {
    Source source = addSource(createSource(//
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 'a':",
        "      break;",
        "    case 'b':",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES);
    verify(source);
  }

  public void test_initializerForNonExistant_initializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZER_FOR_NON_EXISTANT_FIELD);
  }

  public void test_initializerForStaticField() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int x;",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZER_FOR_STATIC_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField_notInEnclosingClass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "int x;",
        "}",
        "class B extends A {",
        "  B(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField_optional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A([this.x]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField_synthetic() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get x => 1;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForStaticField() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int x;",
        "  A([this.x]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_STATIC_FIELD);
    verify(source);
  }

  public void test_instanceMemberAccessFromFactory_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "  A();",
        "  factory A.make() {",
        "    m();",
        "    return new A();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_FACTORY);
    verify(source);
  }

  public void test_instanceMemberAccessFromFactory_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "  A._();",
        "  factory A() {",
        "    m();",
        "    return new A._();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_FACTORY);
    verify(source);
  }

  public void test_instanceMemberAccessFromStatic_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int f;",
        "  static foo() {",
        "    f;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC);
    verify(source);
  }

  public void test_instanceMemberAccessFromStatic_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get g => null;",
        "  static foo() {",
        "    g;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC);
    verify(source);
  }

  public void test_instanceMemberAccessFromStatic_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "  static foo() {",
        "    m();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INSTANCE_MEMBER_ACCESS_FROM_STATIC);
    verify(source);
  }

  public void test_invalidAnnotation_getter() throws Exception {
    Source source = addSource(createSource(//
        "get V => 0;",
        "@V",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_importWithPrefix_getter() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "get V => 0;"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "@p.V",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_importWithPrefix_notConstantVariable() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "final V = 0;"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "@p.V",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_importWithPrefix_notVariableOrConstructorInvocation()
      throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "typedef V();"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "@p.V",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_notConstantVariable() throws Exception {
    Source source = addSource(createSource(//
        "final V = 0;",
        "@V",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_notVariableOrConstructorInvocation() throws Exception {
    Source source = addSource(createSource(//
        "typedef V();",
        "@V",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_staticMethodReference() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static f() {}",
        "}",
        "@A.f",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
    verify(source);
  }

  public void test_invalidAnnotation_unresolved_identifier() throws Exception {
    Source source = addSource(createSource(//
        "@unresolved",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
  }

  public void test_invalidAnnotation_unresolved_invocation() throws Exception {
    Source source = addSource(createSource(//
        "@Unresolved()",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
  }

  public void test_invalidAnnotation_unresolved_prefixedIdentifier() throws Exception {
    Source source = addSource(createSource(//
        "import 'dart:math' as p;",
        "@p.unresolved",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
  }

  public void test_invalidAnnotation_useLibraryScope() throws Exception {
    Source source = addSource(createSource(//
        "@foo",
        "class A {",
        "  static const foo = null;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION);
  }

  public void test_invalidAnnotationFromDeferredLibrary() throws Exception {
    // See test_invalidAnnotation_notConstantVariable
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class V { const V(); }",
            "const v = const V();"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "@a.v main () {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY});
  }

  public void test_invalidAnnotationFromDeferredLibrary_constructor() throws Exception {
    // See test_invalidAnnotation_notConstantVariable
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class C { const C(); }"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "@a.C() main () {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY});
  }

  public void test_invalidAnnotationFromDeferredLibrary_namedConstructor() throws Exception {
    // See test_invalidAnnotation_notConstantVariable
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class C { const C.name(); }"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "@a.C.name() main () {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY});
  }

  public void test_invalidConstructorName_notEnclosingClassName_defined() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  B() : super();", // add ": super()" to force parsing as constructor
        "}",
        "class B {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME);
    // no verify() call, "B" is not resolved
  }

  public void test_invalidConstructorName_notEnclosingClassName_undefined() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  B() : super();", // add ": super()" to force parsing as constructor
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME);
    // no verify() call, "B" is not resolved
  }

  public void test_invalidFactoryNameNotAClass_notClassName() throws Exception {
    Source source = addSource(createSource(//
        "int B;",
        "class A {",
        "  factory B() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_FACTORY_NAME_NOT_A_CLASS);
    verify(source);
  }

  public void test_invalidFactoryNameNotAClass_notEnclosingClassName() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory B() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_FACTORY_NAME_NOT_A_CLASS);
    // no verify() call, "B" is not resolved
  }

  public void test_invalidReferenceToThis_factoryConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A() { return this; }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_instanceVariableInitializer_inConstructor()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var f;",
        "  A() : f = this;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_instanceVariableInitializer_inDeclaration()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var f = this;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_staticMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static m() { return this; }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_staticVariableInitializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static A f = this;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_superInitializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(var x) {}",
        "}",
        "class B extends A {",
        "  B() : super(this);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_topLevelFunction() throws Exception {
    Source source = addSource("f() { return this; }");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidReferenceToThis_variableInitializer() throws Exception {
    Source source = addSource("int x = this;");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void test_invalidTypeArgumentInConstList() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  m() {",
        "    return const <E>[];",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_LIST);
    verify(source);
  }

  public void test_invalidTypeArgumentInConstMap() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  m() {",
        "    return const <String, E>{};",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_MAP);
    verify(source);
  }

  public void test_invalidUri_export() throws Exception {
    Source source = addSource(createSource(//
    "export 'ht:';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_URI);
  }

  public void test_invalidUri_import() throws Exception {
    Source source = addSource(createSource(//
    "import 'ht:';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_URI);
  }

  public void test_invalidUri_part() throws Exception {
    Source source = addSource(createSource(//
    "part 'ht:';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_URI);
  }

  public void test_labelInOuterScope() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m(int i) {",
        "    l: while (i > 0) {",
        "      void f() {",
        "        break l;",
        "      };",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.LABEL_IN_OUTER_SCOPE);
    // We cannot verify resolution with unresolvable labels
  }

  public void test_labelUndefined_break() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  x: while (true) {",
        "    break y;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.LABEL_UNDEFINED);
    // We cannot verify resolution with undefined labels
  }

  public void test_labelUndefined_continue() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  x: while (true) {",
        "    continue y;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.LABEL_UNDEFINED);
    // We cannot verify resolution with undefined labels
  }

  public void test_listElementTypeNotAssignable() throws Exception {
    Source source = addSource(createSource(//
    "var v = const <String> [42];"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.LIST_ELEMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_mapKeyTypeNotAssignable() throws Exception {
    Source source = addSource(createSource(//
    "var v = const <String, int > {1 : 2};"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MAP_KEY_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_mapValueTypeNotAssignable() throws Exception {
    Source source = addSource(createSource(//
    "var v = const <String, String> {'a' : 2};"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MAP_VALUE_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_memberWithClassName_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int A = 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME);
    verify(source);
  }

  public void test_memberWithClassName_field2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int z, A, b = 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME);
    verify(source);
  }

  public void test_memberWithClassName_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get A => 0;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME);
    verify(source);
  }

  public void test_memberWithClassName_method() throws Exception {
    // no test because indistinguishable from constructor
  }

  public void test_methodAndGetterWithSameName() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get x => 0;",
        "  x(y) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.METHOD_AND_GETTER_WITH_SAME_NAME);
    verify(source);
  }

  public void test_mixinDeclaresConstructor_classDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "class B extends Object with A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR);
    verify(source);
  }

  public void test_mixinDeclaresConstructor_typeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "class B = Object with A;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR);
    verify(source);
  }

  public void test_mixinDeferredClass() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class B extends Object with a.A {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.MIXIN_DEFERRED_CLASS});
  }

  public void test_mixinDeferredClass_classTypeAlias() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "class A {}"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class B {}",
                "class C = B with a.A;")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.MIXIN_DEFERRED_CLASS});
  }

  public void test_mixinInheritsFromNotObject_classDeclaration_extends() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "class C extends Object with B {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT);
    verify(source);
  }

  public void test_mixinInheritsFromNotObject_classDeclaration_with() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends Object with A {}",
        "class C extends Object with B {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT);
    verify(source);
  }

  public void test_mixinInheritsFromNotObject_typeAlias_extends() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "class C = Object with B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT);
    verify(source);
  }

  public void test_mixinInheritsFromNotObject_typeAlias_with() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends Object with A {}",
        "class C = Object with B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_class_bool() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Object with bool {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_class_double() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Object with double {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_class_int() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Object with int {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_class_Null() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Object with Null {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_class_num() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Object with num {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_class_String() throws Exception {
    Source source = addSource(createSource(//
    "class A extends Object with String {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_bool() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with bool;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_double() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with double;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_int() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with int;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_Null() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with Null;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_num() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with num;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_String() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with String;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfDisallowedClass_classTypeAlias_String_num() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C = A with String, num;"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS,
        CompileTimeErrorCode.MIXIN_OF_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_mixinOfNonClass_class() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B extends Object with A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
    verify(source);
  }

  public void test_mixinOfNonClass_typeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "int B;",
        "class C = A with B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
    verify(source);
  }

  public void test_mixinReferencesSuper() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  toString() => super.toString();",
        "}",
        "class B extends Object with A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_REFERENCES_SUPER);
    verify(source);
  }

  public void test_mixinWithNonClassSuperclass_class() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B {}",
        "class C extends A with B {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS);
    verify(source);
  }

  public void test_mixinWithNonClassSuperclass_typeAlias() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B {}",
        "class C = A with B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS);
    verify(source);
  }

  public void test_multipleRedirectingConstructorInvocations() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() : this.a(), this.b();",
        "  A.a() {}",
        "  A.b() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MULTIPLE_REDIRECTING_CONSTRUCTOR_INVOCATIONS);
    verify(source);
  }

  public void test_multipleSuperInitializers() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  B() : super(), super() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MULTIPLE_SUPER_INITIALIZERS);
    verify(source);
  }

  public void test_nativeClauseInNonSDKCode() throws Exception {
    // TODO(jwren) Move this test somewhere else: This test verifies a parser error code is generated
    // through the ErrorVerifier, it is not a CompileTimeErrorCode.
    Source source = addSource(createSource(//
    "class A native 'string' {}"));
    resolve(source);
    assertErrors(source, ParserErrorCode.NATIVE_CLAUSE_IN_NON_SDK_CODE);
    verify(source);
  }

  public void test_nativeFunctionBodyInNonSDKCode_function() throws Exception {
    // TODO(jwren) Move this test somewhere else: This test verifies a parser error code is generated
    // through the ErrorVerifier, it is not a CompileTimeErrorCode.
    Source source = addSource(createSource(//
    "int m(a) native 'string';"));
    resolve(source);
    assertErrors(source, ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE);
    verify(source);
  }

  public void test_nativeFunctionBodyInNonSDKCode_method() throws Exception {
    // TODO(jwren) Move this test somewhere else: This test verifies a parser error code is generated
    // through the ErrorVerifier, it is not a CompileTimeErrorCode.
    Source source = addSource(createSource(//
        "class A{",
        "  static int m(a) native 'string';",
        "}"));
    resolve(source);
    assertErrors(source, ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE);
    verify(source);
  }

  public void test_noAnnotationConstructorArguments() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "@A",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NO_ANNOTATION_CONSTRUCTOR_ARGUMENTS);
    verify(source);
  }

  public void test_noDefaultSuperConstructorExplicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(p);",
        "}",
        "class B extends A {",
        "  B() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT);
    verify(source);
  }

  public void test_noDefaultSuperConstructorImplicit_superHasParameters() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(p);",
        "}",
        "class B extends A {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_noDefaultSuperConstructorImplicit_superOnlyNamed() throws Exception {
    Source source = addSource(createSource(//
        "class A { A.named() {} }",
        "class B extends A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_nonConstantAnnotationConstructor_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.fromInt() {}",
        "}",
        "@A.fromInt()",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_ANNOTATION_CONSTRUCTOR);
    verify(source);
  }

  public void test_nonConstantAnnotationConstructor_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "@A()",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_ANNOTATION_CONSTRUCTOR);
    verify(source);
  }

  public void test_nonConstantDefaultValue_function_named() throws Exception {
    Source source = addSource(createSource(//
        "int y;",
        "f({x : y}) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void test_nonConstantDefaultValue_function_positional() throws Exception {
    Source source = addSource(createSource(//
        "int y;",
        "f([x = y]) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void test_nonConstantDefaultValue_inConstructor_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int y;",
        "  A({x : y}) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void test_nonConstantDefaultValue_inConstructor_positional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int y;",
        "  A([x = y]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void test_nonConstantDefaultValue_method_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int y;",
        "  m({x : y}) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void test_nonConstantDefaultValue_method_positional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int y;",
        "  m([x = y]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void test_nonConstantDefaultValueFromDeferredLibrary() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const V = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f({x : a.V}) {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstantDefaultValueFromDeferredLibrary_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const V = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f({x : a.V + 1}) {}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstCaseExpression() throws Exception {
    Source source = addSource(createSource(//
        "f(int p, int q) {",
        "  switch (p) {",
        "    case 3 + q:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION);
    verify(source);
  }

  public void test_nonConstCaseExpressionFromDeferredLibrary() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "main (int p) {",
                "  switch (p) {",
                "    case a.c:",
                "      break;",
                "  }",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstCaseExpressionFromDeferredLibrary_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "main (int p) {",
                "  switch (p) {",
                "    case a.c + 1:",
                "      break;",
                "  }",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstListElement() throws Exception {
    Source source = addSource(createSource(//
        "f(a) {",
        "  return const [a];",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT);
    verify(source);
  }

  public void test_nonConstListElementFromDeferredLibrary() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f() {",
                "  return const [a.c];",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstListElementFromDeferredLibrary_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f() {",
                "  return const [a.c + 1];",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstMapAsExpressionStatement_begin() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  {'a' : 0, 'b' : 1}.length;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONST_MAP_AS_EXPRESSION_STATEMENT);
    verify(source);
  }

  public void test_nonConstMapAsExpressionStatement_only() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  {'a' : 0, 'b' : 1};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONST_MAP_AS_EXPRESSION_STATEMENT);
    verify(source);
  }

  public void test_nonConstMapKey() throws Exception {
    Source source = addSource(createSource(//
        "f(a) {",
        "  return const {a : 0};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_MAP_KEY);
    verify(source);
  }

  public void test_nonConstMapKeyFromDeferredLibrary() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f() {",
                "  return const {a.c : 0};",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_MAP_KEY_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstMapKeyFromDeferredLibrary_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f() {",
                "  return const {a.c + 1 : 0};",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_MAP_KEY_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstMapValue() throws Exception {
    Source source = addSource(createSource(//
        "f(a) {",
        "  return const {'a' : a};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE);
    verify(source);
  }

  public void test_nonConstMapValueFromDeferredLibrary() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f() {",
                "  return const {'a' : a.c};",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstMapValueFromDeferredLibrary_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "f() {",
                "  return const {'a' : a.c + 1};",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstValueInInitializer_binary_notBool_left() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final bool a;",
        "  const A(String p) : a = p && true;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL,
        StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
  }

  public void test_nonConstValueInInitializer_binary_notBool_right() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final bool a;",
        "  const A(String p) : a = true && p;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL,
        StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
  }

  public void test_nonConstValueInInitializer_binary_notInt() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int a;",
        "  const A(String p) : a = 5 & p;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_INT,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_nonConstValueInInitializer_binary_notNum() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int a;",
        "  const A(String p) : a = 5 + p;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_NUM,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_nonConstValueInInitializer_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int C;",
        "  final int a;",
        "  const A() : a = C;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER);
    verify(source);
  }

  public void test_nonConstValueInInitializer_redirecting() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static var C;",
        "  const A.named(p);",
        "  const A() : this.named(C);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER);
    verify(source);
  }

  public void test_nonConstValueInInitializer_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(p);",
        "}",
        "class B extends A {",
        "  static var C;",
        "  const B() : super(C);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER);
    verify(source);
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_field() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class A {",
                "  final int x;",
                "  const A() : x = a.c;",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_field_nested() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class A {",
                "  final int x;",
                "  const A() : x = a.c + 1;",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_redirecting() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class A {",
                "  const A.named(p);",
                "  const A() : this.named(a.c);",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_super() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "const int c = 1;"), //
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as a;",
                "class A {",
                "  const A(p);",
                "}",
                "class B extends A {",
                "  const B() : super(a.c);",
                "}")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY});
  }

  public void test_nonGenerativeConstructor_explicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A.named() {}",
        "}",
        "class B extends A {",
        "  B() : super.named();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void test_nonGenerativeConstructor_implicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A() {}",
        "}",
        "class B extends A {",
        "  B();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void test_nonGenerativeConstructor_implicit2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A() {}",
        "}",
        "class B extends A {",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void test_notEnoughRequiredArguments_const() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(int p);",
        "}",
        "main() {",
        "  const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NOT_ENOUGH_REQUIRED_ARGUMENTS);
    verify(source);
  }

  public void test_notEnoughRequiredArguments_const_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(int p);",
        "}",
        "class B extends A {",
        "  const B() : super();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NOT_ENOUGH_REQUIRED_ARGUMENTS);
    verify(source);
  }

  public void test_optionalParameterInOperator_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator +({p}) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.OPTIONAL_PARAMETER_IN_OPERATOR);
    verify(source);
  }

  public void test_optionalParameterInOperator_positional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator +([p]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.OPTIONAL_PARAMETER_IN_OPERATOR);
    verify(source);
  }

  public void test_partOfNonPart() throws Exception {
    Source source = addSource(createSource(//
        "library l1;",
        "part 'l2.dart';"));
    addNamedSource("/l2.dart", createSource(//
        "library l2;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PART_OF_NON_PART);
    verify(source);
  }

  public void test_prefixCollidesWithTopLevelMembers_functionTypeAlias() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A{}"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "typedef p();",
        "p.A a;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER);
    verify(source);
  }

  public void test_prefixCollidesWithTopLevelMembers_topLevelFunction() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A{}"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "p() {}",
        "p.A a;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER);
    verify(source);
  }

  public void test_prefixCollidesWithTopLevelMembers_topLevelVariable() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A{}"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "var p = null;",
        "p.A a;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER);
    verify(source);
  }

  public void test_prefixCollidesWithTopLevelMembers_type() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A{}"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as p;",
        "class p {}",
        "p.A a;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER);
    verify(source);
  }

  public void test_privateOptionalParameter() throws Exception {
    Source source = addSource(createSource(//
    "f({var _p}) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER);
    verify(source);
  }

  public void test_privateOptionalParameter_fieldFormal() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var _p;",
        "  A({this._p: 0});",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER);
    verify(source);
  }

  public void test_privateOptionalParameter_withDefaultValue() throws Exception {
    Source source = addSource(createSource(//
    "f({_p : 0}) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER);
    verify(source);
  }

  public void test_recursiveConstructorRedirect() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.a() : this.b();",
        "  A.b() : this.a();",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT);
    verify(source);
  }

  public void test_recursiveConstructorRedirect_directSelfReference() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() : this();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_CONSTRUCTOR_REDIRECT);
    verify(source);
  }

  public void test_recursiveFactoryRedirect() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B {",
        "  factory A() = C;",
        "}",
        "class B implements C {",
        "  factory B() = A;",
        "}",
        "class C implements A {",
        "  factory C() = B;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveFactoryRedirect_directSelfReference() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A() = A;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT);
    verify(source);
  }

  public void test_recursiveFactoryRedirect_generic() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> implements B<T> {",
        "  factory A() = C;",
        "}",
        "class B<T> implements C<T> {",
        "  factory B() = A;",
        "}",
        "class C<T> implements A<T> {",
        "  factory C() = B;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveFactoryRedirect_named() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B {",
        "  factory A.nameA() = C.nameC;",
        "}",
        "class B implements C {",
        "  factory B.nameB() = A.nameA;",
        "}",
        "class C implements A {",
        "  factory C.nameC() = B.nameB;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  /**
   * "A" references "C" which has cycle with "B". But we should not report problem for "A" - it is
   * not the part of a cycle.
   */
  public void test_recursiveFactoryRedirect_outsideCycle() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A() = C;",
        "}",
        "class B implements C {",
        "  factory B() = C;",
        "}",
        "class C implements A, B {",
        "  factory C() = B;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_extends() throws Exception {
    Source source = addSource(createSource(//
        "class A extends B {}",
        "class B extends A {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_extends_implements() throws Exception {
    Source source = addSource(createSource(//
        "class A extends B {}",
        "class B implements A {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_implements() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B {}",
        "class B implements A {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_mixin() throws Exception {
    Source source = addSource(createSource(//
        "class M1 = Object with M2;",
        "class M2 = Object with M1;"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_tail() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A implements A {}",
        "class B implements A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_tail2() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A implements B {}",
        "abstract class B implements A {}",
        "class C implements A {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritance_tail3() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A implements B {}",
        "abstract class B implements C {}",
        "abstract class C implements A {}",
        "class D implements A {}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE,
        CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void test_recursiveInterfaceInheritanceBaseCaseExtends() throws Exception {
    Source source = addSource(createSource(//
    "class A extends A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_EXTENDS);
    verify(source);
  }

  public void test_recursiveInterfaceInheritanceBaseCaseImplements() throws Exception {
    Source source = addSource(createSource(//
    "class A implements A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS);
    verify(source);
  }

  public void test_recursiveInterfaceInheritanceBaseCaseImplements_typeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class B = A with M implements B;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_IMPLEMENTS);
    verify(source);
  }

  public void test_recursiveInterfaceInheritanceBaseCaseWith() throws Exception {
    Source source = addSource(createSource(//
    "class M = Object with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE_BASE_CASE_WITH);
    verify(source);
  }

  public void test_redirectGenerativeToMissingConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() : this.noSuchConstructor();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_GENERATIVE_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectGenerativeToNonGenerativeConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() : this.x();",
        "  factory A.x() => null;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_GENERATIVE_TO_NON_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void test_redirectToMissingConstructor_named() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B{",
        "  A() {}",
        "}",
        "class B {",
        "  const factory B() = A.name;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectToMissingConstructor_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B{",
        "  A.name() {}",
        "}",
        "class B {",
        "  const factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectToNonClass_notAType() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B {",
        "  const factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void test_redirectToNonClass_undefinedIdentifier() throws Exception {
    Source source = addSource(createSource(//
        "class B {",
        "  const factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void test_redirectToNonConstConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.a() {}",
        "  const factory A.b() = A.a;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REDIRECT_TO_NON_CONST_CONSTRUCTOR);
    verify(source);
  }

  public void test_referencedBeforeDeclaration_hideInBlock_function() throws Exception {
    Source source = addSource(createSource(//
        "var v = 1;",
        "main() {",
        "  print(v);",
        "  v() {}",
        "}",
        "print(x) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION);
  }

  public void test_referencedBeforeDeclaration_hideInBlock_local() throws Exception {
    Source source = addSource(createSource(//
        "var v = 1;",
        "main() {",
        "  print(v);",
        "  var v = 2;",
        "}",
        "print(x) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION);
  }

  public void test_referencedBeforeDeclaration_hideInBlock_subBlock() throws Exception {
    Source source = addSource(createSource(//
        "var v = 1;",
        "main() {",
        "  {",
        "    print(v);",
        "  }",
        "  var v = 2;",
        "}",
        "print(x) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION);
  }

  public void test_referencedBeforeDeclaration_inInitializer_closure() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  var v = () => v;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION);
  }

  public void test_referencedBeforeDeclaration_inInitializer_directly() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  var v = v;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.REFERENCED_BEFORE_DECLARATION);
  }

  public void test_rethrowOutsideCatch() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  rethrow;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RETHROW_OUTSIDE_CATCH);
    verify(source);
  }

  public void test_returnInGenerativeConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() { return 0; }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void test_returnInGenerativeConstructor_expressionFunctionBody() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() => null;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void test_sharedDeferredPrefix() throws Exception {
    resolveWithAndWithoutExperimental(
        new String[] {createSource(//
            "library lib1;",
            "f1() {}"), //
            createSource("library lib2;", "f2() {}"),//
            createSource(//
                "library root;",
                "import 'lib1.dart' deferred as lib;",
                "import 'lib2.dart' as lib;",
                "main() { lib.f1(); lib.f2(); }")},
        new ErrorCode[] {ParserErrorCode.DEFERRED_IMPORTS_NOT_SUPPORTED},
        new ErrorCode[] {CompileTimeErrorCode.SHARED_DEFERRED_PREFIX});
  }

  public void test_superInInvalidContext_binaryExpression() throws Exception {
    Source source = addSource(createSource(//
    "var v = super + 0;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.v' is not resolved
  }

  public void test_superInInvalidContext_constructorFieldInitializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  var f;",
        "  B() : f = super.m();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.m' is not resolved
  }

  public void test_superInInvalidContext_factoryConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "class B extends A {",
        "  factory B() {",
        "    super.m();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.m' is not resolved
  }

  public void test_superInInvalidContext_instanceVariableInitializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var a;",
        "}",
        "class B extends A {",
        " var b = super.a;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.a' is not resolved
  }

  public void test_superInInvalidContext_staticMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static m() {}",
        "}",
        "class B extends A {",
        "  static n() { return super.m(); }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.m' is not resolved
  }

  public void test_superInInvalidContext_staticVariableInitializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int a = 0;",
        "}",
        "class B extends A {",
        "  static int b = super.a;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.a' is not resolved
  }

  public void test_superInInvalidContext_topLevelFunction() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  super.f();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.f' is not resolved
  }

  public void test_superInInvalidContext_topLevelVariableInitializer() throws Exception {
    Source source = addSource(createSource(//
    "var v = super.y;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    // no verify(), 'super.y' is not resolved
  }

  public void test_superInRedirectingConstructor_redirectionSuper() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {",
        "  B() : this.name(), super();",
        "  B.name() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_REDIRECTING_CONSTRUCTOR);
    verify(source);
  }

  public void test_superInRedirectingConstructor_superRedirection() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {",
        "  B() : super(), this.name();",
        "  B.name() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SUPER_IN_REDIRECTING_CONSTRUCTOR);
    verify(source);
  }

  public void test_symbol_constructor_badArgs() throws Exception {
    Source source = addSource(createSource(//
        "var s1 = const Symbol('3');", // illegal symbol
        "var s2 = const Symbol(3);", // wrong type
        "var s3 = const Symbol();", // too few args
        "var s4 = const Symbol('x', 'y');", // too any args
        "var s5 = const Symbol('x', foo: 'x');" // unexpected named arg
    ));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION,
        CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE,
        CompileTimeErrorCode.NOT_ENOUGH_REQUIRED_ARGUMENTS,
        CompileTimeErrorCode.EXTRA_POSITIONAL_ARGUMENTS,
        CompileTimeErrorCode.UNDEFINED_NAMED_PARAMETER);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_11987() throws Exception {
    Source source = addSource(createSource(//
        "typedef void F(List<G> l);",
        "typedef void G(List<F> l);",
        "main() {",
        "  F foo(G g) => g;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF,
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF,
        StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_parameterType_named() throws Exception {
    Source source = addSource(createSource(//
    "typedef A({A a});"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_parameterType_positional() throws Exception {
    Source source = addSource(createSource(//
    "typedef A([A a]);"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_parameterType_required() throws Exception {
    Source source = addSource(createSource(//
    "typedef A(A a);"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_parameterType_typeArgument() throws Exception {
    Source source = addSource(createSource(//
    "typedef A(List<A> a);"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_returnClass_withTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "typedef C A();",
        "typedef A B();",
        "class C {",
        "  B a;",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_returnType() throws Exception {
    Source source = addSource(createSource(//
    "typedef A A();"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_returnType_indirect() throws Exception {
    Source source = addSource(createSource(//
        "typedef B A();",
        "typedef A B();"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF,
        CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeAliasCannotReferenceItself_typeVariableBounds() throws Exception {
    Source source = addSource(createSource(//
    "typedef A<T extends A>();"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ALIAS_CANNOT_REFERENCE_ITSELF);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_const() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {",
        "  const G();",
        "}",
        "f() { return const G<B>(); }"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_undefinedClass_const() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  return const A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.UNDEFINED_CLASS);
    verify(source);
  }

  public void test_undefinedConstructorInInitializer_explicit_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  B() : super.named();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER);
    // no verify(), "super.named()" is not resolved
  }

  public void test_undefinedConstructorInInitializer_explicit_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.named() {}",
        "}",
        "class B extends A {",
        "  B() : super();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT);
    verify(source);
  }

  public void test_undefinedConstructorInInitializer_implicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.named() {}",
        "}",
        "class B extends A {",
        "  B();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER_DEFAULT);
    verify(source);
  }

  public void test_undefinedNamedParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "}",
        "main() {",
        "  const A(p: 0);",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.UNDEFINED_NAMED_PARAMETER);
    // no verify(), 'p' is not resolved
  }

  public void test_uriDoesNotExist_export() throws Exception {
    Source source = addSource(createSource(//
    "export 'unknown.dart';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.URI_DOES_NOT_EXIST);
  }

  public void test_uriDoesNotExist_import() throws Exception {
    Source source = addSource(createSource(//
    "import 'unknown.dart';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.URI_DOES_NOT_EXIST);
  }

  public void test_uriDoesNotExist_part() throws Exception {
    Source source = addSource(createSource(//
    "part 'unknown.dart';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.URI_DOES_NOT_EXIST);
  }

  public void test_uriWithInterpolation_constant() throws Exception {
    Source source = addSource(createSource(//
    "import 'stuff_$platform.dart';"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.URI_WITH_INTERPOLATION,
        StaticWarningCode.UNDEFINED_IDENTIFIER);
    // We cannot verify resolution with an unresolvable URI: 'stuff_$platform.dart'
  }

  public void test_uriWithInterpolation_nonConstant() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "part '${'a'}.dart';"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.URI_WITH_INTERPOLATION);
    // We cannot verify resolution with an unresolvable URI: '${'a'}.dart'
  }

  public void test_wrongNumberOfParametersForOperator_minus() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator -(a, b) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR_MINUS);
    verify(source);
    reset();
  }

  public void test_wrongNumberOfParametersForOperator_tilde() throws Exception {
    check_wrongNumberOfParametersForOperator("~", "a");
    check_wrongNumberOfParametersForOperator("~", "a, b");
  }

  public void test_wrongNumberOfParametersForOperator1() throws Exception {
    check_wrongNumberOfParametersForOperator1("<");
    check_wrongNumberOfParametersForOperator1(">");
    check_wrongNumberOfParametersForOperator1("<=");
    check_wrongNumberOfParametersForOperator1(">=");
    check_wrongNumberOfParametersForOperator1("+");
    check_wrongNumberOfParametersForOperator1("/");
    check_wrongNumberOfParametersForOperator1("~/");
    check_wrongNumberOfParametersForOperator1("*");
    check_wrongNumberOfParametersForOperator1("%");
    check_wrongNumberOfParametersForOperator1("|");
    check_wrongNumberOfParametersForOperator1("^");
    check_wrongNumberOfParametersForOperator1("&");
    check_wrongNumberOfParametersForOperator1("<<");
    check_wrongNumberOfParametersForOperator1(">>");
    check_wrongNumberOfParametersForOperator1("[]");
  }

  public void test_wrongNumberOfParametersForSetter_function_named() throws Exception {
    Source source = addSource("set x({p}) {}");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_function_optional() throws Exception {
    Source source = addSource("set x([p]) {}");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_function_tooFew() throws Exception {
    Source source = addSource("set x() {}");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_function_tooMany() throws Exception {
    Source source = addSource("set x(a, b) {}");
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_method_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x({p}) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_method_optional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x([p]) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_method_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x() {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void test_wrongNumberOfParametersForSetter_method_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x(a, b) {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  private void check_constEvalThrowsException_binary_null(String expr, boolean resolved)
      throws Exception {
    Source source = addSource("const C = " + expr + ";");
    resolve(source);
    if (resolved) {
      assertErrors(source, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
      verify(source);
    } else {
      assertErrors(source, CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
      // no verify(), 'null x' is not resolved
    }
    reset();
  }

  private void check_constEvalTypeBool_withParameter_binary(String expr) throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final a;",
        "  const A(bool p) : a = " + expr + ";",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_BOOL,
        StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
    reset();
  }

  private void check_constEvalTypeInt_withParameter_binary(String expr) throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final a;",
        "  const A(int p) : a = " + expr + ";",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_INT,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
    reset();
  }

  private void check_constEvalTypeNum_withParameter_binary(String expr) throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final a;",
        "  const A(num p) : a = " + expr + ";",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_EVAL_TYPE_NUM,
        StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
    reset();
  }

  private void check_wrongNumberOfParametersForOperator(String name, String parameters)
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator " + name + "(" + parameters + ") {}",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR);
    verify(source);
    reset();
  }

  private void check_wrongNumberOfParametersForOperator1(String name) throws Exception {
    check_wrongNumberOfParametersForOperator(name, "");
    check_wrongNumberOfParametersForOperator(name, "a, b");
  }
}
