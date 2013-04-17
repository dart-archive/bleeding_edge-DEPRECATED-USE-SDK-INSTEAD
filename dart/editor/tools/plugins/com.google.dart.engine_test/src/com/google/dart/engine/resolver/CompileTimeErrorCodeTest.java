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
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.source.Source;

public class CompileTimeErrorCodeTest extends ResolverTestCase {
  public void fail_ambiguousExport() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"));
    addSource("/lib1.dart", createSource(//
        "class N {}"));
    addSource("/lib2.dart", createSource(//
        "class N {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.AMBIGUOUS_EXPORT);
    verify(source);
  }

  public void fail_ambiguousImport_function() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "g() { return f(); }"));
    addSource("/lib1.dart", createSource(//
        "f() {}"));
    addSource("/lib2.dart", createSource(//
        "f() {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.AMBIGUOUS_IMPORT);
    verify(source);
  }

  public void fail_ambiguousImport_typeAnnotation() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A extends N {}"));
    addSource("/lib1.dart", createSource(//
        "class N {}"));
    addSource("/lib2.dart", createSource(//
        "class N {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.AMBIGUOUS_IMPORT);
    verify(source);
  }

  public void fail_compileTimeConstantRaisesException() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO Find an expression that would raise an exception
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.COMPILE_TIME_CONSTANT_RAISES_EXCEPTION);
    verify(source);
  }

  public void fail_constWithNonConstantArgument() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class T {",
        "  T(a) {};",
        "}",
        "f(p) { return const T(p); }"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_WITH_NON_CONSTANT_ARGUMENT);
    verify(source);
  }

  public void fail_constWithNonType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int A;",
        "f() {",
        "  return const A();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_WITH_NON_TYPE);
    verify(source);
  }

  public void fail_constWithTypeParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_WITH_TYPE_PARAMETERS);
    verify(source);
  }

  public void fail_constWithUndefinedConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A(x) {}",
        "}",
        "f() {",
        "  return const A(0);",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_WITH_UNDEFINED_CONSTRUCTOR);
    verify(source);
  }

  public void fail_duplicateDefinition() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  int m = 0;",
        "  m(a) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(source);
  }

  public void fail_duplicateMemberName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x = 0;",
        "  int x() {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.DUPLICATE_MEMBER_NAME);
    verify(source);
  }

  public void fail_duplicateMemberNameInstanceStatic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  static int x;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.DUPLICATE_MEMBER_NAME_INSTANCE_STATIC);
    verify(source);
  }

  public void fail_duplicateNamedArgument() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f({a, a}) {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.DUPLICATE_NAMED_ARGUMENT);
    verify(source);
  }

  public void fail_extendsOrImplementsDisallowedClass_extends_null() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends Null {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void fail_extendsOrImplementsDisallowedClass_implements_null() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements Null {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void fail_finalNotInitialized_inConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final int x;",
        "  A() {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void fail_getterAndMethodWithSameName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  get x -> 0;",
        "  x(y) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.GETTER_AND_METHOD_WITH_SAME_NAME);
    verify(source);
  }

  public void fail_importDuplicatedLibraryName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library test;",
        "import 'lib1.dart';",
        "import 'lib2.dart';"));
    addSource("/lib1.dart", createSource(//
        "library lib;"));
    addSource("/lib2.dart", createSource(//
        "library lib;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPORT_DUPLICATED_LIBRARY_NAME);
    verify(source);
  }

  public void fail_initializerForNonExistant_initializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INITIALIZER_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void fail_initializerForStaticField() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static x;",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INITIALIZER_FOR_STATIC_FIELD);
    verify(source);
  }

  public void fail_invalidConstructorName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_CONSTRUCTOR_NAME);
    verify(source);
  }

  public void fail_invalidFactoryNameNotAClass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_FACTORY_NAME_NOT_A_CLASS);
    verify(source);
  }

  public void fail_invalidOverrideDefaultValue() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m([a = 0]) {}",
        "}",
        "class B extends A {",
        "  m([a = 1]) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_OVERRIDE_DEFAULT_VALUE);
    verify(source);
  }

  public void fail_invalidOverrideNamed() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m({a, b}) {}",
        "}",
        "class B extends A {",
        "  m({a}) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_OVERRIDE_NAMED);
    verify(source);
  }

  public void fail_invalidOverridePositional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m([a, b]) {}",
        "}",
        "class B extends A {",
        "  m([a]) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_OVERRIDE_POSITIONAL);
    verify(source);
  }

  public void fail_invalidOverrideRequired() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m(a) {}",
        "}",
        "class B extends A {",
        "  m(a, b) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_OVERRIDE_REQUIRED);
    verify(source);
  }

  public void fail_invalidReferenceToThis_staticMethod() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static m() { return this; }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void fail_invalidReferenceToThis_topLevelFunction() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() { return this; }"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void fail_invalidReferenceToThis_variableInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int x = this;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_REFERENCE_TO_THIS);
    verify(source);
  }

  public void fail_invalidTypeArgumentForKey() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m() {",
        "    return const <int, int>{}",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_FOR_KEY);
    verify(source);
  }

  public void fail_invalidTypeArgumentInConstList() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A<E> {",
        "  m() {",
        "    return const <E>[]",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_LIST);
    verify(source);
  }

  public void fail_invalidTypeArgumentInConstMap() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A<E> {",
        "  m() {",
        "    return const <String, E>{}",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_TYPE_ARGUMENT_IN_CONST_MAP);
    verify(source);
  }

  public void fail_memberWithClassName() throws Exception { // field, getter, setter, method
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int A = 0;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MEMBER_WITH_CLASS_NAME);
    verify(source);
  }

  public void fail_mixinDeclaresConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A() {}",
        "}",
        "class B extends Object mixin A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_DECLARES_CONSTRUCTOR);
    verify(source);
  }

  public void fail_mixinInheritsFromNotObject() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "class C extends Object mixin B {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_INHERITS_FROM_NOT_OBJECT);
    verify(source);
  }

  public void fail_mixinOfNonClass() throws Exception {
    // TODO(brianwilkerson) Compare with MIXIN_WITH_NON_CLASS_SUPERCLASS.
    Source source = addSource("/test.dart", createSource(//
        "var A;",
        "class B extends Object mixin A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
    verify(source);
  }

  public void fail_mixinOfNonMixin() throws Exception {
    // TODO(brianwilkerson) This might be covered by more specific errors.
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_OF_NON_MIXIN);
    verify(source);
  }

  public void fail_mixinReferencesSuper() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  toString() -> super.toString();",
        "}",
        "class B extends Object mixin A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_REFERENCES_SUPER);
    verify(source);
  }

  public void fail_mixinWithNonClassSuperclass() throws Exception {
    // TODO(brianwilkerson) Compare with MIXIN_OF_NON_CLASS.
    Source source = addSource("/test.dart", createSource(//
        "int A;",
        "class B extends Object mixin A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS);
    verify(source);
  }

  public void fail_multipleSuperInitializers() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {",
        "  B() : super(), super() {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MULTIPLE_SUPER_INITIALIZERS);
    verify(source);
  }

  public void fail_nonConstantDefaultValue_named() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f({x : 2 + 3}) {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void fail_nonConstantDefaultValue_positional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f([x = 2 + 3]) {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE);
    verify(source);
  }

  public void fail_nonConstMapAsExpressionStatement() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  {'a' : 0, 'b' : 1};",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONST_MAP_AS_EXPRESSION_STATEMENT);
    verify(source);
  }

  public void fail_nonConstMapKey() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(a) {",
        "  return const {a : 0};",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_MAP_KEY);
    verify(source);
  }

  public void fail_nonConstValueInInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static C;",
        "  int a;",
        "  A() : a = C {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER);
    verify(source);
  }

  public void fail_objectCannotExtendAnotherClass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO(brianwilkerson) Figure out how to mock Object
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.OBJECT_CANNOT_EXTEND_ANOTHER_CLASS);
    verify(source);
  }

  public void fail_optionalParameterInOperator() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  operator +([p]) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.OPTIONAL_PARAMETER_IN_OPERATOR);
    verify(source);
  }

  public void fail_overrideMissingNamedParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m(a, {b}) {}",
        "}",
        "class B extends A {",
        "  m(a) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.OVERRIDE_MISSING_NAMED_PARAMETERS);
    verify(source);
  }

  public void fail_overrideMissingRequiredParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m(a) {}",
        "}",
        "class B extends A {",
        "  m(a, b) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.OVERRIDE_MISSING_REQUIRED_PARAMETERS);
    verify(source);
  }

  public void fail_prefixCollidesWithTopLevelMembers() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "import 'dart:uri' as uri;",
        "var uri = null;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.PREFIX_COLLIDES_WITH_TOP_LEVEL_MEMBER);
    verify(source);
  }

  public void fail_privateOptionalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f({_p : 0}) {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.PRIVATE_OPTIONAL_PARAMETER);
    verify(source);
  }

  public void fail_recursiveCompileTimeConstant() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "const x = y + 1;",
        "const y = x + 1;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RECURSIVE_COMPILE_TIME_CONSTANT);
    verify(source);
  }

  public void fail_recursiveFactoryRedirect() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RECURSIVE_FACTORY_REDIRECT);
    verify(source);
  }

  public void fail_recursiveFunctionTypeAlias_direct() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "typedef F(F f);"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RECURSIVE_FUNCTION_TYPE_ALIAS);
    verify(source);
  }

  public void fail_recursiveFunctionTypeAlias_indirect() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "typedef F(G g);",
        "typedef G(F f);"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RECURSIVE_FUNCTION_TYPE_ALIAS);
    verify(source);
  }

  public void fail_recursiveInterfaceInheritance_direct() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void fail_recursiveInterfaceInheritance_indirect() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements B {}",
        "class B implements A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RECURSIVE_INTERFACE_INHERITANCE);
    verify(source);
  }

  public void fail_redirectToNonConstConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.REDIRECT_TO_NON_CONST_CONSTRUCTOR);
    verify(source);
  }

  public void fail_referenceToDeclaredVariableInInitializer_getter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  int x = x + 1;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.REFERENCE_TO_DECLARED_VARIABLE_IN_INITIALIZER);
    verify(source);
  }

  public void fail_referenceToDeclaredVariableInInitializer_setter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  int x = x++;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.REFERENCE_TO_DECLARED_VARIABLE_IN_INITIALIZER);
    verify(source);
  }

  public void fail_reservedWordAsIdentifier() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int class = 2;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RESERVED_WORD_AS_IDENTIFIER);
    verify(source);
  }

  public void fail_returnInGenerativeConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A() { return 0; }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RETURN_IN_GENERATIVE_CONSTRUCTOR);
    verify(source);
  }

  public void fail_staticTopLevelFunction_topLevel() throws Exception {
    // I think this is more general than the error name implies.
    Source source = addSource("/test.dart", createSource(//
        "static f() {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.STATIC_TOP_LEVEL_FUNCTION);
    verify(source);
  }

  public void fail_staticTopLevelVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "static int x;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.STATIC_TOP_LEVEL_VARIABLE);
    verify(source);
  }

  public void fail_superInInvalidContext_factoryConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    verify(source);
  }

  public void fail_superInInvalidContext_instanceVariableInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  var a;",
        "}",
        "class B extends A {",
        " var b = super.a;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    verify(source);
  }

  public void fail_superInInvalidContext_staticMethod() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static m() {}",
        "}",
        "class B extends A {",
        "  static n() { return super.m(); }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    verify(source);
  }

  public void fail_superInInvalidContext_staticVariableInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static a = 0;",
        "}",
        "class B extends A {",
        "  static b = super.a;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    verify(source);
  }

  public void fail_superInInvalidContext_topLevelFunction() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  super.f();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    verify(source);
  }

  public void fail_superInInvalidContext_variableInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var v = super.v;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_IN_INVALID_CONTEXT);
    verify(source);
  }

  public void fail_superInitializerInObject() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO(brianwilkerson) Figure out how to mock Object
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.SUPER_INITIALIZER_IN_OBJECT);
    verify(source);
  }

  public void fail_typeArgumentsForNonGenericClass_creation_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "f(p) {",
        "  return const A<int>();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.TYPE_ARGUMENTS_FOR_NON_GENERIC_CLASS);
    verify(source);
  }

  public void fail_typeArgumentsForNonGenericClass_creation_new() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "f(p) {",
        "  return new A<int>();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.TYPE_ARGUMENTS_FOR_NON_GENERIC_CLASS);
    verify(source);
  }

  public void fail_typeArgumentsForNonGenericClass_typeCast() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "f(p) {",
        "  return p as A<int>;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.TYPE_ARGUMENTS_FOR_NON_GENERIC_CLASS);
    verify(source);
  }

  public void fail_undefinedConstructorInInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(CompileTimeErrorCode.UNDEFINED_CONSTRUCTOR_IN_INITIALIZER);
    verify(source);
  }

  public void fail_uninitializedFinalField() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final int i;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.UNINITIALIZED_FINAL_FIELD);
    verify(source);
  }

  public void fail_wrongNumberOfParametersForOperator() throws Exception {
    // Do we need _tooMany and _tooFew variants for every operator?
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  operator []=(i) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_OPERATOR);
    verify(source);
  }

  public void fail_wrongNumberOfParametersForSetter_tooFew() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "set x() {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void fail_wrongNumberOfParametersForSetter_tooMany() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "set x(a, b) {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_PARAMETERS_FOR_SETTER);
    verify(source);
  }

  public void fail_wrongNumberOfTypeArguments_creation_const_tooFew() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return const C<A>();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void fail_wrongNumberOfTypeArguments_creation_const_tooMany() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return const C<A, A>();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void fail_wrongNumberOfTypeArguments_creation_new_tooFew() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return new C<A>();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void fail_wrongNumberOfTypeArguments_creation_new_tooMany() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return new C<A, A>();",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void fail_wrongNumberOfTypeArguments_typeTest_tooFew() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return p is C<A>;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void fail_wrongNumberOfTypeArguments_typeTest_tooMany() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return p is C<A, A>;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        " var v = 0;",
        " return ?v;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.ARGUMENT_DEFINITION_TEST_NON_PARAMETER);
    verify(source);
  }

  public void test_builtInIdentifierAsType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  typedef x;",
        "}"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE,
        StaticWarningCode.UNDEFINED_CLASS);
    verify(source);
  }

  public void test_builtInIdentifierAsTypedefName_classTypeAlias() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B {}",
        "typedef as = A with B;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    verify(source);
  }

  public void test_builtInIdentifierAsTypedefName_functionTypeAlias() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "typedef bool as();"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME);
    verify(source);
  }

  public void test_builtInIdentifierAsTypeName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class as {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_NAME);
    verify(source);
  }

  public void test_builtInIdentifierAsTypeVariableName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A<as> {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME);
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class IntWrapper {",
        "  final int value;",
        "  const IntWrapper(this.value);",
        "  bool operator ==(IntWrapper x) {",
        "    return value == x.value;",
        "  }",
        "}",
        "",
        "f(IntWrapper a) {",
        "  switch(a) {",
        "    case(const IntWrapper(1)) : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CASE_EXPRESSION_TYPE_IMPLEMENTS_EQUALS);
    verify(source);
  }

  public void test_conflictingConstructorNameAndMember_field() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A.x() {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_FIELD);
    verify(source);
  }

  public void test_conflictingConstructorNameAndMember_method() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const A.x() {}",
        "  void x() {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONFLICTING_CONSTRUCTOR_NAME_AND_METHOD);
    verify(source);
  }

  public void test_constConstructorWithNonFinalField() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_CONSTRUCTOR_WITH_NON_FINAL_FIELD);
    verify(source);
  }

  public void test_constEvalThrowsException() throws Exception { // Not compile-time constant
    Source source = addSource("/test.dart", createSource(//
        "class C {",
        "  const C() { throw null; }",
        "}",
        "f() { return const C(); }"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_EVAL_THROWS_EXCEPTION);
    verify(source);
  }

  public void test_constFormalParameter_fieldFormalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  var x;",
        "  A(const this.x) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_FORMAL_PARAMETER);
    verify(source);
  }

  public void test_constFormalParameter_simpleFormalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(const x) {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_FORMAL_PARAMETER);
    verify(source);
  }

  public void test_constInitializedWithNonConstValue() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(p) {",
        "  const C = p;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE);
    verify(source);
  }

  public void test_constWithInvalidTypeParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const A() {}",
        "}",
        "f() { return const A<A>(); }"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_constWithNonConst() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class T {",
        "  T(a, b, {c, d}) {}",
        "}",
        "f() { return const T(0, 1, c: 2, d: 3); }"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.CONST_WITH_NON_CONST);
    verify(source);
  }

  public void test_defaultValueInFunctionTypeAlias() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "typedef F([x = 0]);"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.DEFAULT_VALUE_IN_FUNCTION_TYPE_ALIAS);
    verify(source);
  }

  public void test_duplicateMemberError() throws Exception {
    Source librarySource = addSource("/lib.dart", createSource(//
        "library lib;",
        "",
        "part 'a.dart';",
        "part 'b.dart';"));
    Source sourceA = addSource("/a.dart", createSource(//
        "part of lib;",
        "",
        "class A {}"));
    Source sourceB = addSource("/b.dart", createSource(//
        "part of lib;",
        "",
        "class A {}"));
    resolve(librarySource, sourceA, sourceB);
    assertErrors(CompileTimeErrorCode.DUPLICATE_DEFINITION);
    verify(librarySource, sourceA, sourceB);
  }

  public void test_exportOfNonLibrary() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "export 'lib1.dart';"));
    addSource("/lib1.dart", createSource(//
        "part of lib;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXPORT_OF_NON_LIBRARY);
    verify(source);
  }

  public void test_extendsNonClass_class() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int A;",
        "class B extends A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_NON_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_extends_bool() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends bool {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_extends_double() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends double {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_extends_int() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends int {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_extends_num() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends num {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_extends_String() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends String {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.EXTENDS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_implements_bool() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements bool {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_implements_double() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements double {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_implements_int() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements int {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_implements_num() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements num {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_extendsOrImplementsDisallowedClass_implements_String() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements String {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DISALLOWED_CLASS);
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A() : x = 0, x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers_multipleInits() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A() : x = 0, x = 1, x = 2 {}",
        "}"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers_multipleNames() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  int y;",
        "  A() : x = 0, x = 1, y = 0, y = 1 {}",
        "}"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS,
        CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const int x = 0;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION);
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_final() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final int x = 0;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION);
    verify(source);
  }

  public void test_fieldInitializedInParameterAndInitializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A(this.x) : x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER);
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  m(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR);
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor_defaultParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  m([this.x]) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZER_OUTSIDE_CONSTRUCTOR);
    verify(source);
  }

  /**
   * This test doesn't test the FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR code, but tests the
   * FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION code instead. It is provided here to show
   * coverage over all of the permutations of initializers in constructor declarations.
   * <p>
   * Note: FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION covers a subset of
   * FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR, since it more specific, we use it instead of
   * the broader code
   */
  public void test_finalInitializedInDeclarationAndConstructor_initializers() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final x = 0;",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION);
    verify(source);
  }

  public void test_finalInitializedInDeclarationAndConstructor_initializingFormal()
      throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final x = 0;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR);
    verify(source);
  }

  public void test_finalInitializedMultipleTimes_initializers() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final x;",
        "  A() : x = 0, x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_BY_MULTIPLE_INITIALIZERS);
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
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final x;",
        "  A(this.x) : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FIELD_INITIALIZED_IN_PARAMETER_AND_INITIALIZER);
    verify(source);
  }

  public void test_finalInitializedMultipleTimes_initializingFormals() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final x;",
        "  A(this.x, this.x) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_INITIALIZED_MULTIPLE_TIMES);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const F;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_const_static() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static const F;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_final() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final F;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_final_static() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static final F;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_library_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "const F;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_library_final() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "final F;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_local_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  const int x;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_local_final() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  final int x;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_implementsDynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements dynamic {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_DYNAMIC);
    verify(source);
  }

  public void test_implementsNonClass_class() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int A;",
        "class B implements A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_NON_CLASS);
    verify(source);
  }

  public void test_implementsNonClass_typedef() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "int B;",
        "typedef C = A implements B;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_NON_CLASS);
    verify(source);
  }

  public void test_implementsRepeated() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B implements A, A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_REPEATED);
    verify(source);
  }

  public void test_implementsRepeated_3times() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {} class C{}",
        "class B implements A, A, A, A {}"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.IMPLEMENTS_REPEATED,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED,
        CompileTimeErrorCode.IMPLEMENTS_REPEATED);
    verify(source);
  }

  public void test_implementsSelf() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A implements A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPLEMENTS_SELF);
    verify(source);
  }

  public void test_importOfNonLibrary() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library lib;",
        "import 'part.dart';"));
    addSource("/part.dart", createSource(//
        "part of lib;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.IMPORT_OF_NON_LIBRARY);
    verify(source);
  }

  public void test_inconsistentCaseExpressionTypes() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 'a':",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES);
    verify(source);
  }

  public void test_inconsistentCaseExpressionTypes_repeated() throws Exception {
    Source source = addSource("/test.dart", createSource(//
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
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES,
        CompileTimeErrorCode.INCONSISTENT_CASE_EXPRESSION_TYPES);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField_notInEnclosingClass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "int x;",
        "}",
        "class B extends A {",
        "  B(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField_optional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A([this.x]) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_NON_EXISTANT_FIELD);
    verify(source);
  }

  public void test_initializingFormalForNonExistantField_static() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static x;",
        "  A([this.x]) {}",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INITIALIZING_FORMAL_FOR_STATIC_FIELD);
    verify(source);
  }

  public void test_invalidUri_export() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "export 'unknown.dart';"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_URI);
  }

  public void test_invalidUri_import() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "import 'unknown.dart';"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_URI);
  }

  public void test_invalidUri_part() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library L;",
        "part 'unknown.dart';"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.INVALID_URI);
  }

  public void test_labelInOuterScope() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class int {}",
        "",
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
    // TODO (jwren) situations where a LABEL_IN_OUTER_SCOPE is generated, a UNDEFINED_OPERATOR will
    // also always be generated, we should revisit this situation so that there is only the more
    // accurate error code generated.
    assertErrors(CompileTimeErrorCode.LABEL_IN_OUTER_SCOPE, StaticWarningCode.UNDEFINED_OPERATOR);
    // We cannot verify resolution with unresolvable labels
  }

  public void test_labelUndefined_break() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  x: while (true) {",
        "    break y;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.LABEL_UNDEFINED);
    // We cannot verify resolution with undefined labels
  }

  public void test_labelUndefined_continue() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  x: while (true) {",
        "    continue y;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.LABEL_UNDEFINED);
    // We cannot verify resolution with undefined labels
  }

  public void test_mixinOfNonClass_class() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int A;",
        "class B with A {}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
    verify(source);
  }

  public void test_mixinOfNonClass_typedef() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "int B;",
        "typedef C = A with B;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
    verify(source);
  }

  // TODO(jwren) Move this test somewhere else: This test verifies a parser error code is generated
  // through the ErrorVerifier, it is not a CompileTimeErrorCode.
  public void test_nativeFunctionBodyInNonSDKCode_function() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int m(a) native 'string';"));
    resolve(source);
    assertErrors(ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE);
    verify(source);
  }

  // TODO(jwren) Move this test somewhere else: This test verifies a parser error code is generated
  // through the ErrorVerifier, it is not a CompileTimeErrorCode.
  public void test_nativeFunctionBodyInNonSDKCode_method() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A{",
        "  static int m(a) native 'string';",
        "}"));
    resolve(source);
    assertErrors(ParserErrorCode.NATIVE_FUNCTION_BODY_IN_NON_SDK_CODE);
    verify(source);
  }

  public void test_newWithInvalidTypeParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "f() { return new A<A>(); }"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NEW_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_nonConstCaseExpression() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(int p, int q) {",
        "  switch (p) {",
        "    case 3 + q:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION);
    verify(source);
  }

  public void test_nonConstListElement() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(a) {",
        "  return const [a];",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT);
    verify(source);
  }

  public void test_nonConstMapValue() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(a) {",
        "  return const {'a' : a};",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE);
    verify(source);
  }

  public void test_partOfNonPart() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library l1;",
        "part 'l2.dart';"));
    addSource("/l2.dart", createSource(//
        "library l2;"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.PART_OF_NON_PART);
    verify(source);
  }

  public void test_rethrowOutsideCatch() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  rethrow;",
        "}"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.RETHROW_OUTSIDE_CATCH);
    verify(source);
  }

  public void test_uriWithInterpolation_constant() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "import 'stuff_$platform.dart';"));
    resolve(source);
    assertErrors(
        CompileTimeErrorCode.URI_WITH_INTERPOLATION,
        StaticWarningCode.UNDEFINED_IDENTIFIER);
    // We cannot verify resolution with an unresolvable URI: 'stuff_$platform.dart'
  }

  public void test_uriWithInterpolation_nonConstant() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library lib;",
        "part '${'a'}.dart';"));
    resolve(source);
    assertErrors(CompileTimeErrorCode.URI_WITH_INTERPOLATION);
    // We cannot verify resolution with an unresolvable URI: '${'a'}.dart'
  }
}
