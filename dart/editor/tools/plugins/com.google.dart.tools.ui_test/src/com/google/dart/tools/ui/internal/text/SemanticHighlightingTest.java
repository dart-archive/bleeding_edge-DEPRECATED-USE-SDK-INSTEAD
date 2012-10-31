/*
 * Copyright 2012, the Dart project authors.
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
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractUtils;
import com.google.dart.tools.internal.corext.refactoring.util.ReflectionUtils;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.HighlightedPosition;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingManager.Highlighting;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingPresenter;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightingReconciler;
import com.google.dart.tools.ui.internal.text.editor.SemanticHighlightings;
import com.google.dart.tools.ui.refactoring.AbstractDartTest;

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
public class SemanticHighlightingTest extends AbstractDartTest {
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

  public void test_builtIn_abstract_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  abstract m();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "abstract m()");
  }

  public void test_builtIn_as_operator() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  0 as Foo;",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "as Foo");
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

  public void test_builtIn_operator_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator +(other) => 0;",
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

  public void test_builtIn_set_variableName() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int set = 0;",
        "}",
        "");
    assertNoWordPosition(SemanticHighlightings.BUILT_IN, "set = 0;");
  }

  public void test_builtIn_static_method() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static m();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.BUILT_IN, "static m()");
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

  public void test_class() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "main() {",
        "  A a = new A ();",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A {}");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A a =");
    assertHasWordPosition(SemanticHighlightings.CLASS, "A ();");
  }

  public void test_deprecated() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  @deprecated",
        "  m () {}",
        "}",
        "main() {",
        "  A.m ();",
        "  print(A.m );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "m () {}");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "m ();");
    assertHasWordPosition(SemanticHighlightings.DEPRECATED_ELEMENT, "m );");
  }

  public void test_directive_export() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "export 'Lib.dart' show A, B hide C, D;",
        "");
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("export "), "export".length());
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("show A, B"), "show".length());
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("hide C, D"), "hide".length());
  }

  public void test_directive_import() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'Lib.dart';",
        "");
    assertHasWordPosition(SemanticHighlightings.DIRECTIVE, "import 'Lib.dart';");
  }

  public void test_directive_import_deprecated() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('Lib.dart');",
        "");
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("#import"), "#import".length());
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

  public void test_directive_library_deprecated() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('MyLib');",
        "");
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("#library"), "#library".length());
  }

  public void test_directive_part() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part 'utils.dart';",
        "");
    assertHasWordPosition(SemanticHighlightings.DIRECTIVE, "part 'utils.dart';");
  }

  public void test_directive_part_deprecated() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#source('utils.dart');",
        "");
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("#source"), "#source".length());
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
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part of MyLib;",
        "");
    assertHasPosition(SemanticHighlightings.DIRECTIVE, findOffset("part of"), "part of".length());
  }

  public void test_directive_partOf2() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part  of MyLib;",
        "");
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
        "}",
        "main() {",
        "  A a = new A();",
        "  a.field = 0;",
        "  print(a.field );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field ;");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field =");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field );");
  }

  public void test_field_topLevel() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int field ;",
        "main() {",
        "  field = 0;",
        "  print(field );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field ;");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field =");
    assertHasWordPosition(SemanticHighlightings.FIELD, "field );");
  }

  public void test_getterDeclaration() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get myGetter => 0;",
        "main() {",
        "  print(myGetter );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.GETTER_DECLARATION, "myGetter => 0");
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
        "int vi1 = 12345 ;",
        "int vi2 = -23456 ;",
        "int vd1 = 123.45 ;",
        "int vd2 = -234.56 ;",
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

  public void test_staticField_member() throws Exception {
    preparePositions(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static int field = 0;",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.field = 1;",
        "  print(a.field );",
        "}",
        "");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field = 0;");
    assertHasWordPosition(SemanticHighlightings.STATIC_FIELD, "field = 1;");
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

  /**
   * Asserts that {@link #positions} contains position with given ID, offset and length.
   */
  private void assertHasPosition(String id, int offset, int length) {
    for (HighlightedPosition position : positions) {
      Highlighting style = position.getHighlighting();
      SemanticHighlighting highlighting = styleToHighlighting.get(style);
      if (highlighting != null
          && highlighting.getPreferenceKey().equals(id)
          && position.getOffset() == offset
          && position.getLength() == length) {
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
      if (highlighting != null
          && highlighting.getPreferenceKey().equals(id)
          && position.getOffset() == offset
          && position.getLength() == length) {
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
    final IDocument document = new Document(testUnit.getSource());
    ExtractUtils utils = new ExtractUtils(testUnit);
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
    Object args = new DartNode[] {utils.getUnitNode()};
    ReflectionUtils.invokeMethod(
        reconciler,
        "reconcilePositions(com.google.dart.compiler.ast.DartNode[])",
        args);
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
    setTestUnitContent(lines);
    preparePositions();
  }
}
