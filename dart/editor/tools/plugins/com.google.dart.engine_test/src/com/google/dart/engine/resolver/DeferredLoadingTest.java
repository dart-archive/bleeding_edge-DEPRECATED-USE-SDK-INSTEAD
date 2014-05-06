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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.context.AnalysisOptionsImpl;
import com.google.dart.engine.source.Source;

public class DeferredLoadingTest extends ResolverTestCase {
  public void test_constDeferredClass() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  const A();",
        "}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "main() {",
        "  const a.A();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_DEFERRED_CLASS);
    verify(source);
  }

  public void test_constDeferredClass_namedConstructor() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  const A.b();",
        "}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "main() {",
        "  const a.A.b();",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.CONST_DEFERRED_CLASS);
    verify(source);
  }

  public void test_constDeferredClass_new_nonTest() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  const A.b();",
        "}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "main() {",
        "  new a.A.b();",
        "}"));
    resolve(source);
    assertErrors(source);
    verify(source);
  }

  public void test_constInitializedWithNonConstValueFromDeferredClass() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const V = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "const B = a.V;"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_constInitializedWithNonConstValueFromDeferredClass_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const V = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "const B = a.V + 1;"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.CONST_INITIALIZED_WITH_NON_CONSTANT_VALUE_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_extendsDeferredClass() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class B extends a.A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DEFERRED_CLASS);
    verify(source);
  }

  public void test_extendsDeferredClass_classTypeAlias() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class M {}",
        "class C = a.A with M;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.EXTENDS_DEFERRED_CLASS);
    verify(source);
  }

  public void test_implementsDeferredClass() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class B implements a.A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DEFERRED_CLASS);
    verify(source);
  }

  public void test_implementsDeferredClass_classTypeAlias() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class B {}",
        "class M {}",
        "class C = B with M implements a.A;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.IMPLEMENTS_DEFERRED_CLASS);
    verify(source);
  }

  public void test_importDeferredLibraryWithLoadFunction() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "loadLibrary() {}",
        "f() {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as lib1;",
        "main() { lib1.f(); }"));
    resolve(source);
    assertErrors(source, HintCode.IMPORT_DEFERRED_LIBRARY_WITH_LOAD_FUNCTION);
    verify(source);
  }

  public void test_importOfNonLibrary() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "import 'part.dart' deferred as p;",
        "var a = new p.A();"));
    addNamedSource("/part.dart", createSource(//
        "part of lib;",
        "class A {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.IMPORT_OF_NON_LIBRARY);
    verify(source);
  }

  public void test_invalidAnnotationFromDeferredLibrary() throws Exception {
    // See test_invalidAnnotation_notConstantVariable
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class V { const V(); }",
        "const v = const V();"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "@a.v main () {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_invalidAnnotationFromDeferredLibrary_constructor() throws Exception {
    // See test_invalidAnnotation_notConstantVariable
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class C { const C(); }"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "@a.C() main () {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_invalidAnnotationFromDeferredLibrary_namedConstructor() throws Exception {
    // See test_invalidAnnotation_notConstantVariable
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class C { const C.name(); }"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "@a.C.name() main () {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.INVALID_ANNOTATION_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_loadLibraryDefined_nonTest() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "foo() => 22;"));
    Source source = addSource(createSource(//
        "import 'lib.dart' deferred as other;",
        "main() {",
        "  other.loadLibrary().then((_) => other.foo());",
        "}"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_mixinDeferredClass() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class B extends Object with a.A {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_DEFERRED_CLASS);
    verify(source);
  }

  public void test_mixinDeferredClass_classTypeAlias() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class B {}",
        "class C = B with a.A;"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.MIXIN_DEFERRED_CLASS);
    verify(source);
  }

  public void test_nonConstantDefaultValueFromDeferredLibrary() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const V = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f({x : a.V}) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstantDefaultValueFromDeferredLibrary_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const V = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f({x : a.V + 1}) {}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_DEFAULT_VALUE_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstCaseExpressionFromDeferredLibrary() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "main (int p) {",
        "  switch (p) {",
        "    case a.c:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstCaseExpressionFromDeferredLibrary_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "main (int p) {",
        "  switch (p) {",
        "    case a.c + 1:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_CASE_EXPRESSION_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstListElementFromDeferredLibrary() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f() {",
        "  return const [a.c];",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstListElementFromDeferredLibrary_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f() {",
        "  return const [a.c + 1];",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_LIST_ELEMENT_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstMapKeyFromDeferredLibrary() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f() {",
        "  return const {a.c : 0};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_MAP_KEY_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstMapKeyFromDeferredLibrary_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f() {",
        "  return const {a.c + 1 : 0};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_MAP_KEY_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstMapValueFromDeferredLibrary() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f() {",
        "  return const {'a' : a.c};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstMapValueFromDeferredLibrary_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int c = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f() {",
        "  return const {'a' : a.c + 1};",
        "}"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.NON_CONSTANT_MAP_VALUE_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_field() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int C = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class A {",
        "  final int x;",
        "  const A() : x = a.C;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_field_nested() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int C = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class A {",
        "  final int x;",
        "  const A() : x = a.C + 1;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_redirecting() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int C = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class A {",
        "  const A.named(p);",
        "  const A() : this.named(a.C);",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_nonConstValueInInitializerFromDeferredLibrary_super() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "const int C = 1;"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class A {",
        "  const A(p);",
        "}",
        "class B extends A {",
        "  const B() : super(a.C);",
        "}"));
    resolve(source);
    assertErrors(
        source,
        CompileTimeErrorCode.NON_CONSTANT_VALUE_IN_INITIALIZER_FROM_DEFERRED_LIBRARY);
    verify(source);
  }

  public void test_sharedDeferredPrefix() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "f1() {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "f2() {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as lib;",
        "import 'lib2.dart' as lib;",
        "main() { lib.f1(); lib.f2(); }"));
    resolve(source);
    assertErrors(source, CompileTimeErrorCode.SHARED_DEFERRED_PREFIX);
    verify(source);
  }

  public void test_sharedDeferredPrefix_nonTest() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "f1() {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "f2() {}"));
    addNamedSource("/lib3.dart", createSource(//
        "library lib3;",
        "f3() {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as lib1;",
        "import 'lib2.dart' as lib;",
        "import 'lib3.dart' as lib;",
        "main() { lib1.f1(); lib.f2(); lib.f3(); }"));
    resolve(source);
    assertNoErrors(source);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_fieldFormalParameter() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class C {",
        "  var v;",
        "  C(a.A this.v);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_functionDeclaration_returnType() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "a.A f() { return null; }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_functionTypedFormalParameter_returnType()
      throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f(a.A g()) {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_methodDeclaration_returnType() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class C {",
        "  a.A m() { return null; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_simpleFormalParameter() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "f(a.A v) {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_typeArgumentList() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class C<E> {}",
        "C<a.A> c;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_typeArgumentList2() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class C<E, F> {}",
        "C<a.A, a.A> c;"));
    resolve(source);
    assertErrors(
        source,
        StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS,
        StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_typeParameter_bound() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "class C<E extends a.A> {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  public void test_typeAnnotationDeferredClass_variableDeclarationList() throws Exception {
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    Source source = addSource(createSource(//
        "library root;",
        "import 'lib1.dart' deferred as a;",
        "a.A v;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_ANNOTATION_DEFERRED_CLASS);
    verify(source);
  }

  @Override
  protected void reset() {
    AnalysisOptionsImpl analysisOptions = new AnalysisOptionsImpl();
    analysisOptions.setEnableDeferredLoading(true);
    resetWithOptions(analysisOptions);
  }
}
