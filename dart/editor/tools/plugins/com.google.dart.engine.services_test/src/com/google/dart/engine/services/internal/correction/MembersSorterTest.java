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

package com.google.dart.engine.services.internal.correction;

import com.google.dart.engine.services.correction.MembersSorter;
import com.google.dart.engine.services.internal.refactoring.RefactoringImplTest;

public class MembersSorterTest extends RefactoringImplTest {
  public void test_classMembers_accessor() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  set c(x) {}",
        "  set a(x) {}",
        "  get a => null;",
        "  get b => null;",
        "  set b(x) {}",
        "  get c => null;",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  get a => null;",
        "  set a(x) {}",
        "  get b => null;",
        "  set b(x) {}",
        "  get c => null;",
        "  set c(x) {}",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_accessor_static() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  get a => null;",
        "  set a(x) {}",
        "  static get b => null;",
        "  static set b(x) {}",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  static get b => null;",
        "  static set b(x) {}",
        "  get a => null;",
        "  set a(x) {}",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_constructor() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  A.c() {   }",
        "  A.a() { }",
        "  A() {}",
        "  A.b();",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  A() {}",
        "  A.a() { }",
        "  A.b();",
        "  A.c() {   }",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_field() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  String c;",
        "  int a;",
        "  double b;",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  int a;",
        "  double b;",
        "  String c;",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_field_static() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  int a;",
        "  static int b;",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  static int b;",
        "  int a;",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_method() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  c() {}",
        "  a() {}",
        "  b() {}",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  a() {}",
        "  b() {}",
        "  c() {}",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_method_emptyLine() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  b() {}",
        "",
        "  a() {}",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  a() {}",
        "",
        "  b() {}",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_method_static() throws Exception {
    String initial = makeSource(//
        "class A {",
        "  static a() {}",
        "  b() {}",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  b() {}",
        "  static a() {}",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_classMembers_mix() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = makeSource(//
        "class A {",
        "  /// static field public",
        "  static int nnn;",
        "  /// static field private",
        "  static int _nnn;",
        "  /// instance getter public",
        "  int get nnn => null;",
        "  /// instance setter public",
        "  set nnn(x) {}",
        "  /// instance getter private",
        "  int get _nnn => null;",
        "  /// instance setter private",
        "  set _nnn(x) {}",
        "  /// instance method public",
        "  nnn() {}",
        "  /// instance method private",
        "  _nnn() {}",
        "  /// static method public",
        "  static nnn() {}",
        "  /// static method private",
        "  static _nnn() {}",
        "  /// static getter public",
        "  static int get nnn => null;",
        "  /// static setter public",
        "  static set nnn(x) {}",
        "  /// static getter private",
        "  static int get _nnn => null;",
        "  /// static setter private",
        "  static set _nnn(x) {}",
        "  /// instance field public",
        "  int nnn;",
        "  /// instance field private",
        "  int _nnn;",
        "  /// constructor generative unnamed",
        "  A();",
        "  /// constructor factory unnamed",
        "  factory A() => null;",
        "  /// constructor generative public",
        "  A.nnn();",
        "  /// constructor factory public",
        "  factory A.ooo() => null;",
        "  /// constructor generative private",
        "  A._nnn();",
        "  /// constructor factory private",
        "  factory A._ooo() => null;",
        "}",
        "");
    String expected = makeSource(//
        "class A {",
        "  /// static field public",
        "  static int nnn;",
        "  /// static field private",
        "  static int _nnn;",
        "  /// static getter public",
        "  static int get nnn => null;",
        "  /// static setter public",
        "  static set nnn(x) {}",
        "  /// static getter private",
        "  static int get _nnn => null;",
        "  /// static setter private",
        "  static set _nnn(x) {}",
        "  /// instance field public",
        "  int nnn;",
        "  /// instance field private",
        "  int _nnn;",
        "  /// constructor generative unnamed",
        "  A();",
        "  /// constructor factory unnamed",
        "  factory A() => null;",
        "  /// constructor generative public",
        "  A.nnn();",
        "  /// constructor factory public",
        "  factory A.ooo() => null;",
        "  /// constructor generative private",
        "  A._nnn();",
        "  /// constructor factory private",
        "  factory A._ooo() => null;",
        "  /// instance getter public",
        "  int get nnn => null;",
        "  /// instance setter public",
        "  set nnn(x) {}",
        "  /// instance getter private",
        "  int get _nnn => null;",
        "  /// instance setter private",
        "  set _nnn(x) {}",
        "  /// instance method public",
        "  nnn() {}",
        "  /// instance method private",
        "  _nnn() {}",
        "  /// static method public",
        "  static nnn() {}",
        "  /// static method private",
        "  static _nnn() {}",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_directives() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = makeSource(//
        "library lib;",
        "",
        "export 'dart:bbb';",
        "import 'dart:bbb';",
        "export 'package:bbb/bbb.dart';",
        "import 'bbb/bbb.dart';",
        "export 'dart:aaa';",
        "export 'package:aaa/aaa.dart';",
        "import 'package:bbb/bbb.dart';",
        "export 'aaa/aaa.dart';",
        "export 'bbb/bbb.dart';",
        "import 'dart:aaa';",
        "import 'package:aaa/aaa.dart';",
        "import 'aaa/aaa.dart';",
        "part 'bbb/bbb.dart';",
        "part 'aaa/aaa.dart';",
        "",
        "main() {",
        "}",
        "");
    String expected = makeSource(//
        "library lib;",
        "",
        "import 'dart:aaa';",
        "import 'dart:bbb';",
        "",
        "import 'package:aaa/aaa.dart';",
        "import 'package:bbb/bbb.dart';",
        "",
        "import 'aaa/aaa.dart';",
        "import 'bbb/bbb.dart';",
        "",
        "export 'dart:aaa';",
        "export 'dart:bbb';",
        "",
        "export 'package:aaa/aaa.dart';",
        "export 'package:bbb/bbb.dart';",
        "",
        "export 'aaa/aaa.dart';",
        "export 'bbb/bbb.dart';",
        "",
        "part 'aaa/aaa.dart';",
        "part 'bbb/bbb.dart';",
        "",
        "main() {",
        "}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_class() throws Exception {
    String initial = makeSource(//
        "class C {}",
        "class A {}",
        "class B {}",
        "");
    String expected = makeSource(//
        "class A {}",
        "class B {}",
        "class C {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_classTypeAlias() throws Exception {
    String initial = makeSource(//
        "class M {}",
        "class C = Object with M;",
        "class A = Object with M;",
        "class B = Object with M;",
        "");
    String expected = makeSource(//
        "class A = Object with M;",
        "class B = Object with M;",
        "class C = Object with M;",
        "class M {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_directive_hasDirective() throws Exception {
    String initial = makeSource(//
        "library lib;",
        "class C {}",
        "class A {}",
        "class B {}",
        "");
    String expected = makeSource(//
        "library lib;",
        "class A {}",
        "class B {}",
        "class C {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_directive_noDirective_hasComment_line() throws Exception {
    String initial = makeSource(//
        "// Some comment",
        "",
        "class B {}",
        "",
        "class A {}",
        "");
    String expected = makeSource(//
        "// Some comment",
        "",
        "class A {}",
        "",
        "class B {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_directive_noDirective_noComment() throws Exception {
    String initial = makeSource(//
        "",
        "class B {}",
        "",
        "class A {}",
        "");
    String expected = makeSource(//
        "",
        "class A {}",
        "",
        "class B {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_function() throws Exception {
    String initial = makeSource(//
        "fc() {}",
        "fa() {}",
        "fb() {}",
        "");
    String expected = makeSource(//
        "fa() {}",
        "fb() {}",
        "fc() {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_functionTypeAlias() throws Exception {
    String initial = makeSource(//
        "typedef FC();",
        "typedef FA();",
        "typedef FB();",
        "");
    String expected = makeSource(//
        "typedef FA();",
        "typedef FB();",
        "typedef FC();",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_mix() throws Exception {
    verifyNoTestUnitErrors = false;
    String initial = makeSource(//
        "_mmm() {}",
        "typedef nnn();",
        "_nnn() {}",
        "typedef mmm();",
        "typedef _nnn();",
        "typedef _mmm();",
        "class mmm {}",
        "get _nnn => null;",
        "class nnn {}",
        "class _mmm {}",
        "class _nnn {}",
        "var mmm;",
        "var nnn;",
        "var _mmm;",
        "var _nnn;",
        "set nnn(x) {}",
        "get mmm => null;",
        "set mmm(x) {}",
        "get nnn => null;",
        "get _mmm => null;",
        "set _mmm(x) {}",
        "set _nnn(x) {}",
        "mmm() {}",
        "nnn() {}",
        "");
    String expected = makeSource(//
        "var mmm;",
        "var nnn;",
        "var _mmm;",
        "var _nnn;",
        "get mmm => null;",
        "set mmm(x) {}",
        "get nnn => null;",
        "set nnn(x) {}",
        "get _mmm => null;",
        "set _mmm(x) {}",
        "get _nnn => null;",
        "set _nnn(x) {}",
        "mmm() {}",
        "nnn() {}",
        "_mmm() {}",
        "_nnn() {}",
        "typedef mmm();",
        "typedef nnn();",
        "typedef _mmm();",
        "typedef _nnn();",
        "class mmm {}",
        "class nnn {}",
        "class _mmm {}",
        "class _nnn {}",
        "");
    assertSorting(initial, expected);
  }

  public void test_unitMembers_topLevelVariable() throws Exception {
    String initial = makeSource(//
        "int c;",
        "int a;",
        "int b;",
        "");
    String expected = makeSource(//
        "int a;",
        "int b;",
        "int c;",
        "");
    assertSorting(initial, expected);
  }

  private void assertSorting(String initial, String expected) throws Exception {
    MembersSorter sorter = new MembersSorter(initial, null);
    String result = sorter.createSortedCode();
    assertEquals(expected, result);
  }
}
