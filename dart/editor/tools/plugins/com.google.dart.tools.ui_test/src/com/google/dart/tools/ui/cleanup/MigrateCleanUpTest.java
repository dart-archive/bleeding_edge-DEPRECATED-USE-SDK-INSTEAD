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
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_catch_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_get_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_identical_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_library_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_optionalNamed_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_parseNum_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_rawString_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_functionLiteral_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_methods_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_removeAbstract_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_removeInterface_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M2_renameTypes_CleanUp;

/**
 * Test for {@link AbstractMigrateCleanUp}.
 */
public final class MigrateCleanUpTest extends AbstractCleanUpTest {

  public void test_1M1_catch_alreadyNewSyntax_withoutType() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_catch_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try {",
        "  } catch (e, stack) {",
        "  }",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_catch_alreadyNewSyntax_withType() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_catch_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try {",
        "  } on Exception catch (e, stack) {",
        "  }",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_catch_withExceptionType() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_catch_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try {",
        "  } catch (final Exception e, stack) {",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try {",
        "  } on Exception catch (e, stack) {",
        "  }",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_catch_withStackType() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_catch_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try {",
        "  } catch (e, Object stack) {",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try {",
        "  } catch (e, stack) {",
        "  }",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_getter() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_get_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test() => 42;",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test => 42;",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_getter_noOp() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_get_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test => 42;",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

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
    setUnitContent("Main.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Main;",
        "part 'Test.dart';",
        ""});
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
    setUnitContent("Main.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Main;",
        "part 'Test.dart';",
        ""});
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
    setUnitContent("Main.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library Main;",
        "part 'Test.dart';",
        ""});
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
    setUnitContent("Main.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "#library('dart:my-lib.new.dart');",
        "#source('Test.dart');",
        ""});
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

  public void test_1M1_parseNum() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_parseNum_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math';",
        "main() {",
        "  parseInt('123');",
        "  parseDouble('123.45');",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math';",
        "main() {",
        "  int.parse('123');",
        "  double.parse('123.45');",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_parseNum_qualifiedInvocation() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_parseNum_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as m;",
        "main() {",
        "  m.parseInt('123');",
        "  m.parseDouble('123.45');",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "import 'dart:math' as m;",
        "main() {",
        "  int.parse('123');",
        "  double.parse('123.45');",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_rawString() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_rawString_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var s1 = '111';",
        "  var s2 = \"222\";",
        "  var s3 = '333';",
        "  var s4 = r\"444\";",
        "  var s5 = r\"555\";",
        "  var s6 = @\"666\";",
        "  var s7 = @\"777\";",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var s1 = '111';",
        "  var s2 = \"222\";",
        "  var s3 = '333';",
        "  var s4 = r\"444\";",
        "  var s5 = r\"555\";",
        "  var s6 = r\"666\";",
        "  var s7 = r\"777\";",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_rawString_adjacent() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_rawString_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var s1 = '111' 'aaa';",
        "  var s2 = r'222' r'bbb';",
        "  var s3 = @'333' r'ccc';",
        "  var s4 = r'444' @'ddd';",
        "  var s5 = @'555' @'eee';",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var s1 = '111' 'aaa';",
        "  var s2 = r'222' r'bbb';",
        "  var s3 = r'333' r'ccc';",
        "  var s4 = r'444' r'ddd';",
        "  var s5 = r'555' r'eee';",
        "}",
        "");
    assertCleanUp(cleanUp, initial, expected);
  }

  public void test_1M1_rawString_interpolation() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_rawString_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var user = 'user';",
        "  var host = 'host';",
        "  var s = '$user@$host';",
        "}",
        "");
    assertNoFix(cleanUp, initial);
  }

  public void test_1M1_rawString_native() throws Exception {
    ICleanUp cleanUp = new Migrate_1M1_rawString_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() native @'abc';",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo() native r'abc';",
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
        "  Queue queue;",
        "  queue.first();",
        "  queue.last();",
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
        "  Queue queue;",
        "  queue.first;",
        "  queue.last;",
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

  public void test_1M2_methods_isEmpty() throws Exception {
    ICleanUp cleanUp = new Migrate_1M2_methods_CleanUp();
    String initial = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A_Collection implements Collection {",
        "  bool isEmpty() => false;",
        "}",
        "class A_SequenceCollection implements Collection {",
        "  bool isEmpty() => false;",
        "}",
        "main() {",
        "  {",
        "    Collection v = null;",
        "    v.isEmpty();",
        "  }",
        "  {",
        "    Map v = null;",
        "    v.isEmpty();",
        "  }",
        "  {",
        "    SequenceCollection v = null;",
        "    v.isEmpty();",
        "  }",
        "  {",
        "    String v = null;",
        "    v.isEmpty();",
        "  }",
        "  {",
        "    StringBuffer v = null;",
        "    v.isEmpty();",
        "  }",
        "}",
        "");
    String expected = makeSource(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A_Collection implements Collection {",
        "  bool get isEmpty => false;",
        "}",
        "class A_SequenceCollection implements Collection {",
        "  bool get isEmpty => false;",
        "}",
        "main() {",
        "  {",
        "    Collection v = null;",
        "    v.isEmpty;",
        "  }",
        "  {",
        "    Map v = null;",
        "    v.isEmpty;",
        "  }",
        "  {",
        "    SequenceCollection v = null;",
        "    v.isEmpty;",
        "  }",
        "  {",
        "    String v = null;",
        "    v.isEmpty;",
        "  }",
        "  {",
        "    StringBuffer v = null;",
        "    v.isEmpty;",
        "  }",
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
    setUnitContent("MyLib.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "library myLib;",
        "class InvalidArgumentException() {}",
        ""});
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

}
