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
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_library_CleanUp;
import com.google.dart.tools.ui.internal.cleanup.migration.Migrate_1M1_optionalNamed_CleanUp;

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
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "");
    String expected = makeSource(
        "part of Main;",
        "",
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {}",
        "");
    assertCleanUp(cleanUp, initial, expected);
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

}
