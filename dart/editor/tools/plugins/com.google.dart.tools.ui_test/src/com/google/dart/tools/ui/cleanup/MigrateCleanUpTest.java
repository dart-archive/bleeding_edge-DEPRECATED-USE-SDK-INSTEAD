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
package com.google.dart.tools.ui.cleanup;

import com.google.dart.tools.ui.internal.cleanup.migration.AbstractMigrateCleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_identical_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_library_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_optionalNamed_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_functionLiteral_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_removeAbstract_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_removeInterface_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_renameTypes_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M3_Future_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M3_corelib_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M4_CleanUp;

/**
 * Test for {@link AbstractMigrateCleanUp}.
 */
public final class MigrateCleanUpTest extends AbstractCleanUpTest {

  public void test_1M1_identical() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_identical_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "  a === null;",
        "  a === 0;",
        "  a === 0.0;",
        "  null === a;",
        "  0 === a;",
        "  0.0 === a;",
        "  a === b;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "  a == null;",
        "  a == 0;",
        "  a == 0.0;",
        "  null == a;",
        "  0 == a;",
        "  0.0 == a;",
        "  identical(a, b);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_identical_complex() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_identical_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "  a === -1;",
        "  a === (1);",
        "  a === 1 + 2;",
        "  a === 1 - 2;",
        "  a === 1 * 2;",
        "  a === ~1;",
        "  a === 1 + b;",
        "  a === b + 1;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "  a == -1;",
        "  a == (1);",
        "  a == 1 + 2;",
        "  a == 1 - 2;",
        "  a == 1 * 2;",
        "  a == ~1;",
        "  identical(a, 1 + b);",
        "  identical(a, b + 1);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_identicalNot() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_identical_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "  a !== null;",
        "  a !== 0;",
        "  a !== 0.0;",
        "  null !== a;",
        "  0 !== a;",
        "  0.0 !== a;",
        "  a !== b;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "  a != null;",
        "  a != 0;",
        "  a != 0.0;",
        "  null != a;",
        "  0 != a;",
        "  0.0 != a;",
        "  !identical(a, b);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('myLib');",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library myLib;",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library_empty() throws Exception {
    setUnitContent(
        "Main.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library Main;",
            "part 'Test.dart';",
            ""));
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource("");
    String expected = makeSource("part of Main;", "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library_import() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#import('A.dart');",
        "#import('B.dart', prefix: 'bbb');",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'A.dart';",
        "import 'B.dart' as bbb;",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library_isScript() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_library_partOf() throws Exception {
    setUnitContent(
        "Main.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library Main;",
            "part 'Test.dart';",
            ""));
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// copyright copyright copyright copyright copyright copyright copyright",
        "// copyright copyright copyright copyright copyright copyright copyright",
        "// copyright copyright copyright copyright copyright copyright copyright",
        "",
        "// documentation documentation documentation documentation documentation",
        "// documentation documentation documentation documentation documentation",
        "",
        "class A {}",
        "");
    String expected = makeSource(
        "// copyright copyright copyright copyright copyright copyright copyright",
        "// copyright copyright copyright copyright copyright copyright copyright",
        "// copyright copyright copyright copyright copyright copyright copyright",
        "",
        "part of Main;",
        "",
        "// documentation documentation documentation documentation documentation",
        "// documentation documentation documentation documentation documentation",
        "",
        "class A {}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library_partOf_already() throws Exception {
    setUnitContent(
        "Main.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library Main;",
            "part 'Test.dart';",
            ""));
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "",
        "part of Main;",
        "",
        "class A {}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_library_source() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#source('A.dart');",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "part 'A.dart';",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library_specialCharacters_lib() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('dart:my-lib.new.dart');",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "library dart_my_lib_new;",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_library_specialCharacters_part() throws Exception {
    setUnitContent(
        "Main.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "#library('dart:my-lib.new.dart');",
            "#source('Test.dart');",
            ""));
    ICleanUp cleanUp = new Migrate_1M1_library_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "",
        "part of dart_my_lib_new;",
        "",
        "class A {}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_optionalNamed_noOp_alreadyNewSyntax() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, {b: 2})",
        "}",
        "main() {",
        "  A.foo(1, b: 2);",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_optionalNamed_noOp_function_mixOptionalPositionalNamed() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(a, [b = 2, c = 3])",
        "main() {",
        "  foo(10, 20, c: 30);",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_optionalNamed_noOp_method_mixOptionalPositionalNamed() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "main() {",
        "  A.foo(10, 20, c: 30);",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_optionalNamed_noOp_noInvocations() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_optionalNamed_noOp_onlyOptionalPositional() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "main() {",
        "  A.foo(10, 20, 30);",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_optionalNamed_OK_method() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "main() {",
        "  A.foo(10, b: 20, c: 30);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, {b: 2, c: 3})",
        "}",
        "main() {",
        "  A.foo(10, b: 20, c: 30);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_optionalNamed_OK_method_differentOrderOfArguments() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "main() {",
        "  A.foo(10, c: 30, b: 20);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, {b: 2, c: 3})",
        "}",
        "main() {",
        "  A.foo(10, c: 30, b: 20);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_optionalNamed_OK_method_noDefault() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b, c])",
        "}",
        "main() {",
        "  A.foo(10, b: 20, c: 30);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, {b, c})",
        "}",
        "main() {",
        "  A.foo(10, b: 20, c: 30);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_optionalNamed_OK_method_noOptionalArguments() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "main() {",
        "  A.foo(10);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, {b: 2, c: 3})",
        "}",
        "main() {",
        "  A.foo(10);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_optionalNamed_OK_method_onlyOneNamedArgument() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, [b = 2, c = 3])",
        "}",
        "main() {",
        "  A.foo(10, c: 30);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static foo(a, {b: 2, c: 3})",
        "}",
        "main() {",
        "  A.foo(10, c: 30);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_optionalNamed_OK_topLevelFunction() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_optionalNamed_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(a, [b = 2, c = 3])",
        "main() {",
        "  foo(10, b: 20, c: 30);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(a, {b: 2, c: 3})",
        "main() {",
        "  foo(10, b: 20, c: 30);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_functionLiteral_name() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_functionLiteral_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        " print(foo(x) {});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        " print((x) {});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_functionLiteral_returnAndName() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_functionLiteral_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        " print(int foo(x) {});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        " print((x) {});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_functionLiteral_statement() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_functionLiteral_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        " int foo(x) {}",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M2_methods_collections() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  List list;",
        "  list.last();",
        "",
        "  Map map;",
        "  map.getKeys();",
        "  map.getValues();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  List list;",
        "  list.last;",
        "",
        "  Map map;",
        "  map.keys;",
        "  map.values;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_Element_elements_children() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:html';",
        "main() {",
        "  Element e;",
        "  e.elements;",
        "  e.children;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:html';",
        "main() {",
        "  Element e;",
        "  e.children;",
        "  e.children;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_File() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:io';",
        "main() {",
        "  File f;",
        "  f.readAsText();",
        "  f.readAsString();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:io';",
        "main() {",
        "  File f;",
        "  f.readAsString();",
        "  f.readAsString();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_interface() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class _JustForInternalTest {",
        "  int foo() => 0;",
        "}",
        "class A implements _JustForInternalTest {",
        "  int foo() => 1;",
        "  int bar() => 2;",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.foo());",
        "  print(a.bar());",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class _JustForInternalTest {",
        "  int get foo => 0;",
        "}",
        "class A implements _JustForInternalTest {",
        "  int get foo => 1;",
        "  int bar() => 2;",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.foo);",
        "  print(a.bar());",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_Iterator_hasNext() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Iterator v = null;",
        "  v.hasNext();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Iterator v = null;",
        "  v.hasNext;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_num() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  num v = 0;",
        "  v.isNaN();",
        "  v.isInfinite();",
        "  v.isNegative();",
        "  v.abs();",
        "",
        "  int i = 0;",
        "  i.isEven();",
        "  i.isOdd();",
        "",
        "  double d = 0.0;",
        "  d.isNaN();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  num v = 0;",
        "  v.isNaN;",
        "  v.isInfinite;",
        "  v.isNegative;",
        "  v.abs();",
        "",
        "  int i = 0;",
        "  i.isEven;",
        "  i.isOdd;",
        "",
        "  double d = 0.0;",
        "  d.isNaN;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_Object_hashCode() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int hashCode() => 0;",
        "  int foo() => 0;",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.hashCode());",
        "  print(a.foo());",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  int get hashCode => 0;",
        "  int foo() => 0;",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.hashCode);",
        "  print(a.foo());",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_methods_Stopwatch() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Stopwatch sw = new Stopwatch();",
        "  sw.frequency();",
        "  sw.elapsedInMs();",
        "  sw.elapsedInUs();",
        "  sw.elapsed();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Stopwatch sw = new Stopwatch();",
        "  sw.frequency;",
        "  sw.elapsedMilliseconds;",
        "  sw.elapsedMicroseconds;",
        "  sw.elapsedTicks;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_removeAbstract() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_removeAbstract_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A {",
        "  abstract foo();",
        "  abstract void bar();",
        "  baz();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A {",
        "  foo();",
        "  void bar();",
        "  baz();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_removeInterface() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_removeInterface_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "interface I default B {",
        "  I();",
        "  I.name();",
        "  foo();",
        "}",
        "",
        "class B implements I {",
        "  B() {}",
        "  B.name() {}",
        "  foo() {}",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class I {",
        "  factory I() = B;",
        "  factory I.name() = B.name;",
        "  foo();",
        "}",
        "",
        "class B implements I {",
        "  B() {}",
        "  B.name() {}",
        "  foo() {}",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_renameExceptions() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_renameTypes_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  throw new InvalidArgumentException();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  throw new ArgumentError();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M2_renameExceptions_qualifiedName() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_renameTypes_CleanUp();
    setUnitContent(
        "MyLib.dart",
        formatLines(
            "// filler filler filler filler filler filler filler filler filler filler",
            "library myLib;",
            "class InvalidArgumentException() {}",
            ""));
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'MyLib.dart' as pref;",
        "main() {",
        "  throw new pref.InvalidArgumentException();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'MyLib.dart' as pref;",
        "main() {",
        "  throw new pref.ArgumentError();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_Date_to_DateTime() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Date myDate;",
        "  new Date();",
        "  new Date.fromString('foo');",
        "}",
        "class Foo {",
        "  main() {",
        "    Date myDate;",
        "    new Date();",
        "    new Date.fromString('bar');",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  DateTime myDate;",
        "  new DateTime();",
        "  DateTime.parse('foo');",
        "}",
        "class Foo {",
        "  main() {",
        "    DateTime myDate;",
        "    new DateTime();",
        "    DateTime.parse('bar');",
        "  }",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_DateTime_parse() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new DateTime();",
        "  new DateTime.fromString('foo');",
        "}",
        "class Foo {",
        "  main() {",
        "    new DateTime.fromString('bar');",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new DateTime();",
        "  DateTime.parse('foo');",
        "}",
        "class Foo {",
        "  main() {",
        "    DateTime.parse('bar');",
        "  }",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_implementList() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A<E> implements List<E> {",
        "  Collection<E> filter(bool f(E element)) {}",
        "  Collection map(f(E element)) {}",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A<E> implements List<E> {",
        "  Iterable<E> where(bool f(E element)) => new WhereIterable<E>(this, f);",
        "  Iterable map(f(E element)) => new MappedIterable<E, dynamic>(this, f);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_implementSet() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A<E> implements Set<E> {",
        "  Collection<E> filter(bool f(E element)) {}",
        "  Collection map(f(E element)) {}",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A<E> implements Set<E> {",
        "  Iterable<E> where(bool f(E element)) => new WhereIterable<E>(this, f);",
        "  Iterable map(f(E element)) => new MappedIterable<E, dynamic>(this, f);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  /**
   * In some cases the class already has a super-class, so we cannot just replace "implements" with
   * "extends", and then we would use mixins. Currently that's not possible :(
   */
  public void test_1M3_corelib_iterableDeclaration_hasExtend() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A extends Object implements Iterable {",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  /**
   * In general we want people to extend the Iterable interface and not just implement it. We have
   * added tons of methods to the Iterable base class and by extending it the user doesn't need to
   * reimplement these methods.
   */
  public void test_1M3_corelib_iterableDeclaration_noExtend() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A implements Iterable {",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A extends Iterable {",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_iteratorAccess_alreadyMigrated() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  Iterator get iterator => null;",
        "}",
        "main() {",
        "  var a = new A();",
        "  var it = a.iterator;",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M3_corelib_iteratorDeclaration() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyIterator<E> implements Iterator<E> {",
        "  bool get hasNext => true;",
        "  E next() => 42;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyIterator<E> implements Iterator<E> {",
        "  bool get _hasNext => true;",
        "  E _next() => 42;",
        "  E _current;",
        "  bool moveNext() {",
        "    if (_hasNext) {",
        "      _current = _next();",
        "      return true;",
        "    }",
        "    _current = null;",
        "    return false;",
        "  }",
        "  E current => _current;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_iteratorDeclaration_alreadyMigrated() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class MyIterator<E> implements Iterator<E> {",
        "  bool moveNext() {",
        "    return false;",
        "  }",
        "  E current => null;",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M3_corelib_iteratorUsage_notGeneric() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  Iterator iterator() => null;",
        "  m() {",
        "    var it1 = iterator();",
        "    Iterator it2 = iterator();",
        "  }",
        "}",
        "main() {",
        "  var a = new A();",
        "  for (var item in a) {}",
        "  var it1 = a.iterator();",
        "  Iterator it2 = a.iterator();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  Iterator get iterator => null;",
        "  m() {",
        "    var it1 = new HasNextIterator(iterator);",
        "    HasNextIterator it2 = new HasNextIterator(iterator);",
        "  }",
        "}",
        "main() {",
        "  var a = new A();",
        "  for (var item in a) {}",
        "  var it1 = new HasNextIterator(a.iterator);",
        "  HasNextIterator it2 = new HasNextIterator(a.iterator);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_iteratorUsage_withGeneric() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  Iterator<String> iterator() => null;",
        "  m() {",
        "    var it1 = iterator();",
        "    Iterator<String> it2 = iterator();",
        "  }",
        "}",
        "main() {",
        "  var a = new A();",
        "  for (var item in a) {}",
        "  var it1 = a.iterator();",
        "  Iterator it2 = a.iterator();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  Iterator<String> get iterator => null;",
        "  m() {",
        "    var it1 = new HasNextIterator<String>(iterator);",
        "    HasNextIterator<String> it2 = new HasNextIterator<String>(iterator);",
        "  }",
        "}",
        "main() {",
        "  var a = new A();",
        "  for (var item in a) {}",
        "  var it1 = new HasNextIterator<String>(a.iterator);",
        "  HasNextIterator<String> it2 = new HasNextIterator<String>(a.iterator);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_List_fixedLength() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new List();",
        "  new List(5);",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M3_corelib_mapList() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new List();",
        "  var v1 = src.map((e) => true);",
        "  print(v1[0]);",
        "  List v2 = src.map((e) => true);",
        "  for (var v3 in src.map((e) => true)) {}",
        "  src.map((e) => true).forEach((e) {print(e);});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new List();",
        "  var v1 = src.map((e) => true).toList();",
        "  print(v1[0]);",
        "  List v2 = src.map((e) => true).toList();",
        "  for (var v3 in src.map((e) => true)) {}",
        "  src.map((e) => true).forEach((e) {print(e);});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_Strings_join() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  Strings.join(['a', 'b', 'c'], ' ');",
        "  Strings.concatAll(['a', 'b', 'c']);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  ['a', 'b', 'c'].join(' ');",
        "  ['a', 'b', 'c'].join();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_Uri_methodToGetter() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:uri';",
        "main() {",
        "  Uri uri = null;",
        "  uri.isAbsolute();",
        "  uri.hasAuthority();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:uri';",
        "main() {",
        "  Uri uri = null;",
        "  uri.isAbsolute;",
        "  uri.hasAuthority;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_Uri_parse() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Uri();",
        "  Uri uri = new Uri.fromString('foo');",
        "}",
        "class Foo {",
        "  main() {",
        "    new Uri.fromString('bar');",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  new Uri();",
        "  Uri uri = Uri.parse('foo');",
        "}",
        "class Foo {",
        "  main() {",
        "    Uri.parse('bar');",
        "  }",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_whereList() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new List();",
        "  var v1 = src.filter((e) => true);",
        "  print(v1[0]);",
        "  List v2 = src.filter((e) => true);",
        "  for (var v3 in src.filter((e) => true)) {}",
        "  src.filter((e) => true).forEach((e) {print(e);});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new List();",
        "  var v1 = src.where((e) => true).toList();",
        "  print(v1[0]);",
        "  List v2 = src.where((e) => true).toList();",
        "  for (var v3 in src.where((e) => true)) {}",
        "  src.where((e) => true).forEach((e) {print(e);});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_whereList_2() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new List();",
        "  var v = src.filter((e) => true).filter((e) => true);",
        "  print(v[0]);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new List();",
        "  var v = src.where((e) => true).where((e) => true).toList();",
        "  print(v[0]);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  /**
   * <p>
   * http://code.google.com/p/dart/issues/detail?id=8073
   */
  public void test_1M3_corelib_whereList_temporaryAssigned_usedAsIterableOnly() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var t = [1, 2].filter((x) => x < 2);",
        "  for (var x in t) {}",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var t = [1, 2].where((x) => x < 2);",
        "  for (var x in t) {}",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_whereSet() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new Set();",
        "  var v1 = src.filter((e) => true);",
        "  v1.containsAll([]);",
        "  Set v2 = src.filter((e) => true);",
        "  for (var v3 in src.filter((e) => true)) {}",
        "  src.filter((e) => true).forEach((e) {print(e);});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new Set();",
        "  var v1 = src.where((e) => true).toSet();",
        "  v1.containsAll([]);",
        "  Set v2 = src.where((e) => true).toSet();",
        "  for (var v3 in src.where((e) => true)) {}",
        "  src.where((e) => true).forEach((e) {print(e);});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_corelib_whereSet_2() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new Set();",
        "  var v = src.filter((e) => true).filter((e) => true);",
        "  v.containsAll([]);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var src = new Set();",
        "  var v = src.where((e) => true).where((e) => true).toSet();",
        "  v.containsAll([]);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_Future() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_Future_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Future future = null;",
        "  future.chain(null).transform(null).then(null);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Future future = null;",
        "  future.then(null).then(null).then(null);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_Future_cascade() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_Future_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Future future = null;",
        "  future..chain(null)..transform(null)..then(null);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Future future = null;",
        "  future..then(null)..then(null)..then(null);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_Future_useDuration() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "foo(int delay) {",
        "  new Future.delayed(100);",
        "  new Future.delayed(delay);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "foo(int delay) {",
        "  new Future.delayed(const Duration(milliseconds: 100));",
        "  new Future.delayed(const Duration(milliseconds: delay));",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_Future_useDuration_already() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "foo(int delay) {",
        "  new Future.delayed(const Duration(milliseconds: 100));",
        "  new Future.delayed(const Duration(milliseconds: delay));",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  /**
   * Class <code>Futures</code> was removed, its methods are moved into <code>Future</code>.
   */
  public void test_1M3_Futures_methods() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_Future_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Futures.wait(null);",
        "  Futures.forEach(null, null);",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Future.wait(null);",
        "  Future.forEach(null, null);",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_onEvent() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:html';",
        "main() {",
        "  ButtonElement button;",
        "  button.on.click.add((e) {});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:html';",
        "main() {",
        "  ButtonElement button;",
        "  button.onClick.listen((e) {});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_renameExceptions() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  throw new IllegalJSRegExpException();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  throw new FormatException();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_Timer_useDuration() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  new Timer(0, (t) {});",
        "  new Timer(100, (t) {});",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Timer.run((t) {});",
        "  new Timer(const Duration(milliseconds: 100), (t) {});",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M3_Timer_useDuration_already() throws Exception {
    ICleanUp cleanUp = new Migrate_1M3_corelib_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main() {",
        "  Timer.run((t) {});",
        "  new Timer(const Duration(milliseconds: 100), (t) {});",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M4_xMatching_to_xWhere() throws Exception {
    ICleanUp cleanUp = new Migrate_1M4_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main2(Iterable iter, Stream stream, Collection collection) {",
        "  iter.firstMatching();",
        "  iter.lastMatching();",
        "  iter.singleMatching();",
        "  stream.firstMatching();",
        "  stream.lastMatching();",
        "  stream.singleMatching();",
        "  collection.removeMatching();",
        "  collection.retainMatching();",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:async';",
        "main2(Iterable iter, Stream stream, Collection collection) {",
        "  iter.firstWhere();",
        "  iter.lastWhere();",
        "  iter.singleWhere();",
        "  stream.firstWhere();",
        "  stream.lastWhere();",
        "  stream.singleWhere();",
        "  collection.removeWhere();",
        "  collection.retainWhere();",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

}
