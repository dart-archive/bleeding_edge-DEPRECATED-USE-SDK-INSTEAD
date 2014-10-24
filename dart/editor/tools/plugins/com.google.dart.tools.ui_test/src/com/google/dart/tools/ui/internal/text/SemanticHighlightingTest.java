/*
 * Copyright 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text;

import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.HighlightedPosition;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.Highlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingPresenter;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingReconciler;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link SemanticHighlightings}.
 */
public class SemanticHighlightingTest extends
    com.google.dart.engine.internal.index.AbstractDartTest {
  private static final Map<Highlighting, SemanticHighlighting> styleToHighlighting = Maps.newHashMap();
  private static final SemanticHighlighting[] highlighters = SemanticHighlightings.getSemanticHighlightings();
  private static final Highlighting[] styles = new Highlighting[highlighters.length];
  static {
    for (int i = 0; i < highlighters.length; i++) {
      styles[i] = new Highlighting(null, true);
      styleToHighlighting.put(styles[i], highlighters[i]);
    }
  }

  private List<HighlightedPosition> positions;

  public void test_annotation() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "const myAnnotation = 0;",
        "class MyAnno { const MyAnno(p); }",
        "@myAnnotation // marker",
        "var topA;",
        "@MyAnno(42) // marker",
        "var topB;",
        "");
    assertHasWordPosition(SemanticHighlightings.ANNOTATION, "@myAnnotation // marker");
    assertHasWordPosition(SemanticHighlightings.ANNOTATION, "@MyAnno(");
    assertHasWordPosition(SemanticHighlightings.ANNOTATION, ") // marker");
  }

  public void test_builtIn_abstract_class() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A {",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "abstract class");
  }

  public void test_builtIn_as_operator() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  0 as String;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "as String");
  }

  public void test_builtIn_as_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var as = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "as = 0");
  }

  public void test_builtIn_async_bodyModifier() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() async {",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "async {");
  }

  public void test_builtIn_asyncStar_bodyModifier() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() async* {",
        "}",
        "");
    assertHasPosition(SemanticHighlightings.BUILT_IN, findOffset("async*"), "async*".length());
  }

  public void test_builtIn_await_expression() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() async {",
        "  await 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "await 0");
  }

  public void test_builtIn_await_forEach() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() async {",
        "  await for (var e in []) {}",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "await for");
  }

  public void test_builtIn_const_constructor() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  const A();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "const A();");
  }

  public void test_builtIn_dynamic_type() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  dynamic v;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "dynamic v;");
  }

  public void test_builtIn_dynamic_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int dynamic = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "dynamic = 0;");
  }

  public void test_builtIn_external_constructor() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  external A();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "external A();");
  }

  public void test_builtIn_external_function() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "external f();",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "external f();");
  }

  public void test_builtIn_external_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  external m();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "external m();");
  }

  public void test_builtIn_external_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int external = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "external = 0;");
  }

  public void test_builtIn_factory_constructor() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  factory A() {}",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "factory A() {}");
  }

  public void test_builtIn_factory_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int factory = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "factory = 0;");
  }

  public void test_builtIn_get_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get value => 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "get value => 0;");
  }

  public void test_builtIn_get_method_withReturnType() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get value => 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "get value => 0;");
  }

  public void test_builtIn_get_topLevel() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get value => 0;",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "get value => 0;");
  }

  public void test_builtIn_get_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int get = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "get = 0;");
  }

  public void test_builtIn_implements() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class I {}",
        "class A implements I {",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "implements I {");
  }

  public void test_builtIn_implements_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int implements = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "implements = 0;");
  }

  public void test_builtIn_native_clause() throws Exception {
    verifyNoTestUnitErrors = false;
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A native 'B' {",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "native 'B' {");
  }

  public void test_builtIn_native_functionBody() throws Exception {
    verifyNoTestUnitErrors = false;
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() native 'z';",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "native 'z';");
  }

  public void test_builtIn_on() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class E1 {}",
        "class E2 {}",
        "main() {",
        "  try {",
        "  } on E1 catch (e) {",
        "  } on E2 catch (e) {",
        "  }",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "on E1");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "on E2");
  }

  public void test_builtIn_operator_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator +(other) => 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "operator +(other) => 0;");
  }

  public void test_builtIn_operator_method_withReturnType() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int operator +(other) => 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "operator +(other) => 0;");
  }

  public void test_builtIn_operator_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int operator = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "operator = 0;");
  }

  public void test_builtIn_set_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  set value(x) {}",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "set value(x)");
  }

  public void test_builtIn_set_method_withReturnType() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  void set value(x) {}",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "set value(x)");
  }

  public void test_builtIn_set_topLevel() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "set value(x) {}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "set value(x)");
  }

  public void test_builtIn_set_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int set = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "set = 0;");
  }

  public void test_builtIn_static_field() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var f = 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "static var f =");
  }

  public void test_builtIn_static_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static m() {}",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "static m()");
  }

  public void test_builtIn_syncStar_bodyModifier() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() sync* {",
        "}",
        "");
    assertHasPosition(SemanticHighlightings.BUILT_IN, findOffset("sync*"), "sync*".length());
  }

  public void test_builtIn_typedef() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef F();",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "typedef F();");
  }

  public void test_builtIn_typedef_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int typedef = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "typedef = 0;");
  }

  public void test_builtIn_yield() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() async* {",
        "  yield 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "yield 0");
  }

  public void test_builtIn_yieldStar() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() async* {",
        "  yield* [];",
        "}",
        "");
    assertHasPosition(SemanticHighlightings.BUILT_IN, findOffset("yield* []"), "yield*".length());
  }

  public void test_class() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static const ZERO = 0;",
        "}",
        "main() {",
        "  A a = new A ();",
        "  print(A .ZERO);",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A {");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A a =");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A ();");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A .ZERO");
  }

  public void test_constructor() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A.name () {}",
        "}",
        "main() {",
        "  new A.name ();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.CONSTRUCTOR, "name () {}");
    assertHasWordPosition(SemanticHighlightings.CONSTRUCTOR, "name ();");
  }

  public void test_deprecated() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "@deprecated",
        "class A {",
        "  @deprecated",
        "  method() {}",
        "  @deprecated",
        "  var field;",
        "  @deprecated",
        "  get myGet => null;",
        "  @deprecated",
        "  set mySet(x) {}",
        "}",
        "main() {",
        "  A a;",
        "  a.method ();",
        "  a.field ;",
        "  a.field = 0;",
        "  a.myGet ;",
        "  a.mySet = 1;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "A {");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "A a;");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "method ();");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "field ;");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "field =");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "myGet ;");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "mySet =");
  }

  public void test_deprecated_libraryImport() throws Exception {
    setFileContent(
        "ModernLib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library modernLib;",
            ""));
    setFileContent(
        "DeprecatedLib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "@deprecated",
            "library deprecatedLib;",
            "// const deprecated = 0;",
            ""));
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library App;",
        "import 'ModernLib.dart';",
        "import 'DeprecatedLib.dart';",
        "");
    String search = "'DeprecatedLib.dart'";
    assertHasPosition(SemanticHighlightings.DEPRECATED_ELEMENT, findOffset(search), search.length());
  }

  public void test_directive_import_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int import = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "import = 0;");
  }

  public void test_directive_library() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library MyLib;",
        "");
    assertHasWordPosition(SemanticHighlightings.DIRECTIVE, "library MyLib;");
  }

  public void test_directive_library_withDocumentComment() throws Exception {
    preparePositions(
        "/// filler filler filler filler filler filler filler filler filler filler",
        "library MyLib;",
        "");
    assertHasWordPosition(SemanticHighlightings.DIRECTIVE, "library MyLib;");
  }

  public void test_directive_part() throws Exception {
    setFileContent("utils.dart", "part of app;");
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library app;",
        "part 'utils.dart';",
        "");
    assertHasWordPosition(SemanticHighlightings.DIRECTIVE, "part 'utils.dart';");
  }

  public void test_directive_part_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int part = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "part = 0;");
  }

  public void test_directive_partOf() throws Exception {
    Source libSource = setFileContent(
        "App.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library app;",
            "part 'Test.dart';",
            ""));
    Source unitSource = setFileContent(
        "Test.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "/**",
            " * Some DartDoc",
            " */",
            "part of app;",
            ""));
    parseTestUnits(libSource, unitSource);
    preparePositions();
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("part of"), "part of".length());
  }

  public void test_directive_partOf2() throws Exception {
    Source libSource = setFileContent(
        "App.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library app;",
            "part 'Test.dart';",
            ""));
    Source unitSource = setFileContent(
        "Test.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "part  of app;",
            ""));
    parseTestUnits(libSource, unitSource);
    preparePositions();
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("part  of"), "part  of".length());
  }

  public void test_dynamic() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(dynParameter ) {",
        "  var dynVar = dynParameter ;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.DYNAMIC_TYPE, "dynVar = ");
    assertHasWordPosition(SemanticHighlightings.DYNAMIC_TYPE, "dynParameter ;");
  }

  public void test_field_member() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int field ;",
        "  A(this.field ); // in constructor",
        "}",
        "main() {",
        "  A a = new A(0);",
        "  a.field = 0;",
        "  print(a.field ); // reference",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field ;");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field ); // in constructor");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field =");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field ); // reference");
  }

  public void test_function() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  f ();",
        "  print(f );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.FUNCTION, "f ();");
    assertHasWordPosition(SemanticHighlightings.FUNCTION, "f );");
  }

  public void test_functionTypeAlias() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "typedef String Foo ();",
        "main(Foo f) {",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.FUNCTION_TYPE_ALIAS, "Foo (");
    assertHasWordPosition(SemanticHighlightings.FUNCTION_TYPE_ALIAS, "Foo f)");
  }

  public void test_getterDeclaration_function() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get myGetter => 0;",
        "main() {",
        "  print(myGetter );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.GETTER_DECLARATION, "myGetter => 0");
  }

  public void test_getterDeclaration_member() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get prop => 42;",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.prop );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.GETTER_DECLARATION, "prop =>");
    assertHasWordPosition(SemanticHighlightings.FIELD, "prop );");
  }

  public void test_importPrefix() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async' as asy ;",
        "main() {",
        "  asy .Future f = null;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.IMPORT_PREFIX, "asy ;");
    assertHasWordPosition(SemanticHighlightings.IMPORT_PREFIX, "asy .Future");
  }

  public void test_localVariable() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int v = 0;",
        "  v = 1;",
        "  print(v );",
        "}",
        "");
    // declaration
    assertHasWordPosition(SemanticHighlightings.LOCAL_VARIABLE_DECLARATION, "v = 0;");
    // reference
    assertHasWordPosition(SemanticHighlightings.LOCAL_VARIABLE, "v = 1;");
    assertHasWordPosition(SemanticHighlightings.LOCAL_VARIABLE, "v );");
  }

  public void test_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  m () {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.m ();",
        "  print(a.m );",
        "}",
        "");
    // declaration
    assertHasWordPosition(SemanticHighlightings.METHOD_DECLARATION, "m () {}");
    // invocation
    assertHasWordPosition(SemanticHighlightings.METHOD, "m ();"); // invocation
    // reference
    assertHasWordPosition(SemanticHighlightings.METHOD, "m );");
  }

  public void test_number() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var vi1 = 12345 ;",
        "var vi2 = -23456 ;",
        "var vd1 = 123.45 ;",
        "var vd2 = -234.56 ;",
        "");
    assertHasWordPosition(SemanticHighlightings.NUMBER, "12345 ;");
    assertHasWordPosition(SemanticHighlightings.NUMBER, "23456 ;");
    assertHasWordPosition(SemanticHighlightings.NUMBER, "123.45 ;");
    assertHasWordPosition(SemanticHighlightings.NUMBER, "234.56 ;");
  }

  public void test_parameter() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(int p ) {",
        "  p = 1;",
        "  print(p );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.PARAMETER_VARIABLE, "p ) {");
    assertHasWordPosition(SemanticHighlightings.PARAMETER_VARIABLE, "p = 1;");
    assertHasWordPosition(SemanticHighlightings.PARAMETER_VARIABLE, "p );");
  }

  public void test_parameter_namedArgument() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f({int p}) {",
        "}",
        "main() {",
        "  f(p : 0);",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.PARAMETER_VARIABLE, "p : 0);");
  }

  public void test_setterDeclaration_function() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "set mySetter (x) {}",
        "main() {",
        "  mySetter = 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.SETTER_DECLARATION, "mySetter (x)");
  }

  public void test_setterDeclaration_member() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  set prop (x) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.prop = 0;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.SETTER_DECLARATION, "prop (x)");
    assertHasWordPosition(SemanticHighlightings.FIELD, "prop = 0");
  }

  public void test_staticField_member() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static int field = 0;",
        "}",
        "main() {",
        "  A.field = 1;",
        "  print(A.field );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field = 0;");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field = 1;");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field );");
  }

  public void test_staticField_topLevel() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int field ;",
        "main() {",
        "  field = 0;",
        "  print(field );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field ;");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field =");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field );");
  }

  public void test_staticMethod() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static m () {}",
        "}",
        "main() {",
        "  A.m ();",
        "  print(A.m );",
        "}",
        "");
    // declaration
    assertHasWordPosition(SemanticHighlightings.STATIC_METHOD_DECLARATION, "m () {}");
    // invocation
    assertHasWordPosition(SemanticHighlightings.STATIC_METHOD, "m ();");
    // reference
    assertHasWordPosition(SemanticHighlightings.STATIC_METHOD, "m );");
  }

  public void test_typeVariable() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyClass<T > {",
        "  T value;",
        "  T foo(T t) => null;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.TYPE_VARIABLE, "T > {");
    assertHasWordPosition(SemanticHighlightings.TYPE_VARIABLE, "T value");
    assertHasWordPosition(SemanticHighlightings.TYPE_VARIABLE, "T foo");
    assertHasWordPosition(SemanticHighlightings.TYPE_VARIABLE, "T t)");
  }

  public void xtest_directive_export() throws Exception {
    // Timing out on build-bot; runs fine locally
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;",
            "class A {}",
            "class B {}",
            "class C {}",
            "class D {}"));
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "export 'Lib.dart' show A, B hide C, D;",
        "");
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("export "), "export".length());
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("show A, B"), "show".length());
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("hide C, D"), "hide".length());
  }

  public void xtest_directive_import() throws Exception {
    // Timing out on build-bot; runs fine locally
    setFileContent(
        "Lib.dart",
        makeSource(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library lib;"));
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart';",
        "");
    assertHasWordPosition(SemanticHighlightings.DIRECTIVE, "import 'Lib.dart';");
  }

  /**
   * Asserts that {@link #positions} contains position with given ID, offset and length.
   */
  private void assertHasPosition(String id, int offset, int length) {
    for (HighlightedPosition position : positions) {
      Highlighting style = position.getHighlighting();
      SemanticHighlighting highlighting = styleToHighlighting.get(style);
      if (highlighting != null && highlighting.getPreferenceKey().equals(id)
          && position.getOffset() == offset && position.getLength() == length) {
        return;
      }
    }
    fail("Position not found: id:" + id + " offset:" + offset + " length:" + length);
  }

  /**
   * Asserts that {@link #positions} contains position with given ID, at offset of "pattern" and
   * with length of the first space-separate word.
   */
  private void assertHasWordPosition(String id, String pattern) throws Exception {
    int offset = findOffset(pattern);
    String word = StringUtils.substringBefore(pattern, " ");
    assertHasPosition(id, offset, word.length());
  }

  /**
   * Asserts that {@link #positions} has no position with given ID, offset and length.
   */
  private void assertNoPosition(String id, int offset, int length) {
    for (HighlightedPosition position : positions) {
      Highlighting style = position.getHighlighting();
      SemanticHighlighting highlighting = styleToHighlighting.get(style);
      if (highlighting != null && highlighting.getPreferenceKey().equals(id)
          && position.getOffset() == offset && position.getLength() == length) {
        fail("Position not expected id:" + id + " offset:" + offset + " length:" + length);
      }
    }
  }

  /**
   * Asserts that {@link #positions} has no position with given ID, at offset of "pattern" and with
   * length of the first space-separate word.
   */
  private void assertNoWordPosition(String id, String pattern) throws Exception {
    int offset = findOffset(pattern);
    String word = StringUtils.substringBefore(pattern, " ");
    assertNoPosition(id, offset, word.length());
  }

  private void preparePositions() throws Exception {
    final IDocument document = new Document(testCode);
    SemanticHighlightingReconciler reconciler = new SemanticHighlightingReconciler();
    // configure
    ReflectionUtils.setField(reconciler, "fSourceViewer", Proxy.newProxyInstance(
        getClass().getClassLoader(),
        new Class[] {ISourceViewer.class},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("getDocument")) {
              return document;
            }
            return null;
          }
        }));
    ReflectionUtils.setField(reconciler, "fJobSemanticHighlightings", highlighters);
    ReflectionUtils.setField(reconciler, "fJobHighlightings", styles);
    ReflectionUtils.setField(reconciler, "fJobPresenter", new SemanticHighlightingPresenter());
    // call "reconcilePositions"
    ReflectionUtils.invokeMethod(
        reconciler,
        "reconcilePositions(com.google.dart.engine.ast.CompilationUnit)",
        testUnit);
    // get result
    positions = ReflectionUtils.getFieldObject(reconciler, "fAddedPositions");
    // touch "default"
    for (HighlightedPosition position : positions) {
      Highlighting style = position.getHighlighting();
      SemanticHighlighting highlighting = styleToHighlighting.get(style);
      highlighting.getDisplayName();
      highlighting.isEnabledByDefault();
      highlighting.isBoldByDefault();
      highlighting.isItalicByDefault();
      highlighting.isStrikethroughByDefault();
      highlighting.isUnderlineByDefault();
      highlighting.getDefaultDefaultTextColor();
      highlighting.getDefaultStyle();
      highlighting.getDefaultTextColor();
    }
  }

  private void preparePositions(String... lines) throws Exception {
    parseTestUnit(lines);
    preparePositions();
  }
}
