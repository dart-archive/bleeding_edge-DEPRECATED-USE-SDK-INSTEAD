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

package com.google.dart.java2dart;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.java2dart.util.ToFormattedSourceVisitor;

import junit.framework.TestCase;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Test for general Java semantics to Dart translation.
 */
public class SemanticTest extends TestCase {

  /**
   * @return the formatted Dart source dump of the given {@link ASTNode}.
   */
  private static String getFormattedSource(ASTNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToFormattedSourceVisitor(writer));
    return writer.toString();
  }

  /**
   * @return the single {@link String} with "\n" separated lines.
   */
  private static String toString(String... lines) {
    return Joiner.on("\n").join(lines);
  }

  private File tmpFolder;

  public void test_buildSingleDartUnit() throws Exception {
    setFileLines(
        "test/Main.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Main {",
            "  static void foo() {}",
            "}",
            ""));
    setFileLines(
        "test/Second.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Second {",
            "  static void bar() {",
            "    Main.foo();",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Main {",
            "  static void foo() {",
            "  }",
            "}",
            "class Second {",
            "  static void bar() {",
            "    Main.foo();",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_configureRenameField() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  int foo;",
            "  static void foo() {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  static void bar() {",
            "    print(A.foo);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/A;.foo", "myField");
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  int myField = 0;",
            "  static void foo() {",
            "  }",
            "}",
            "class B {",
            "  static void bar() {",
            "    print(A.myField);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_configureRenameMethod() throws Exception {
    setFileLines(
        "test/A.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class A {",
            "  static void foo() {}",
            "  static void foo(int p) {}",
            "}",
            ""));
    setFileLines(
        "test/B.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class B {",
            "  static void bar() {",
            "    A.foo(42);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    context.addRename("Ltest/A;.foo(I)", "fooWithInt");
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class A {",
            "  static void foo() {",
            "  }",
            "  static void fooWithInt(int p) {",
            "  }",
            "}",
            "class B {",
            "  static void bar() {",
            "    A.fooWithInt(42);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_ensureFieldInitializer() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  boolean booleanF;",
            "  byte byteF;",
            "  char charF;",
            "  short shortF;",
            "  int intF;",
            "  long longF;",
            "  float floatF;",
            "  double doubleF;",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  bool booleanF = false;",
            "  int byteF = 0;",
            "  int charF = 0;",
            "  int shortF = 0;",
            "  int intF = 0;",
            "  int longF = 0;",
            "  double floatF = 0.0;",
            "  double doubleF = 0.0;",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_methodField() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  private int value;",
            "  public int value() {",
            "    return value;",
            "  }",
            "  public void bar() {",
            "    value();",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  int value2 = 0;",
            "  int value() {",
            "    return value2;",
            "  }",
            "  void bar() {",
            "    value();",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_methods() throws Exception {
    setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  static void foo() {}",
            "  static void foo(int p) {}",
            "  static void foo(double p) {}",
            "  static void bar() {",
            "    foo(42);",
            "  }",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFiles(tmpFolder);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  static void foo() {",
            "  }",
            "  static void foo2(int p) {",
            "  }",
            "  static void foo3(double p) {",
            "  }",
            "  static void bar() {",
            "    foo2(42);",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  public void test_giveUniqueName_variableInitializer() throws Exception {
    File file = setFileLines(
        "test/Test.java",
        toString(
            "// filler filler filler filler filler filler filler filler filler filler",
            "package test;",
            "public class Test {",
            "  static int foo() {return 42;}",
            "  static void bar() {",
            "    int foo = foo();",
            "    baz(foo);",
            "  }",
            "  static void baz(int p) {}",
            "}",
            ""));
    Context context = new Context();
    context.addSourceFolder(tmpFolder);
    context.addSourceFile(file);
    CompilationUnit unit = context.translate();
    assertEquals(
        toString(
            "class Test {",
            "  static int foo() {",
            "    return 42;",
            "  }",
            "  static void bar() {",
            "    int foo2 = foo();",
            "    baz(foo2);",
            "  }",
            "  static void baz(int p) {",
            "  }",
            "}"),
        getFormattedSource(unit));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tmpFolder = Files.createTempDir();
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtils.deleteDirectory(tmpFolder);
    super.tearDown();
  }

//  /**
//   * Translates {@link #javaUnit} into {@link #dartUnit} and check that it produces given Dart
//   * source.
//   */
//  private void assertDartSource(String... lines) {
//    dartUnit = SyntaxTranslator.translate(javaUnit);
//    String actualDartSource = getFormattedSource(dartUnit);
//    String expectedDartSource = toString(lines);
//    assertEquals(expectedDartSource, actualDartSource);
//  }
//
//  /**
//   * Parse Java source into {@link #javaUnit}.
//   */
//  private void parseJavaSource(String path) throws Exception {
//    String unitName = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
//    String source = Files.toString(new File(tmpFolder, path), Charsets.UTF_8);
//    ASTParser parser = ASTParser.newParser(AST.JLS4);
//    parser.setEnvironment(null, new String[] {tmpFolderPath}, null, true);
//    parser.setResolveBindings(true);
//    parser.setCompilerOptions(ImmutableMap.of(
//        JavaCore.COMPILER_SOURCE,
//        JavaCore.VERSION_1_5,
//        JavaCore.COMPILER_DOC_COMMENT_SUPPORT,
//        JavaCore.ENABLED));
//    parser.setUnitName(unitName);
//    parser.setSource(source.toCharArray());
//    javaUnit = (CompilationUnit) parser.createAST(null);
//    assertThat(javaUnit.getProblems()).isEmpty();
//  }

  /**
   * Sets the content of the file with given path relative to {@link #tmpFolder}.
   */
  private File setFileLines(String path, String content) throws Exception {
    File toFile = new File(tmpFolder, path);
    Files.createParentDirs(toFile);
    Files.write(content, toFile, Charsets.UTF_8);
    return toFile;
  }
}
