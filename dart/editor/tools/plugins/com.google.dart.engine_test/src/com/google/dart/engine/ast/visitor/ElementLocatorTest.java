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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.internal.index.AbstractDartTest;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElementLocatorTest extends ResolverTestCase {

  private static final String SRC_FILE_NAME = "/test.dart";

  private static int getOffsetOfMatch(String contents, String pattern, int matchIndex) {

    if (matchIndex == 0) {
      return contents.indexOf(pattern);
    }

    Matcher matcher = Pattern.compile(pattern).matcher(contents);
    int count = 0;
    while (matcher.find()) {
      if (count == matchIndex) {
        return matcher.start();
      }
      ++count;
    }

    return -1;
  }

  public void test_arrayAccess() throws Exception {
    ASTNode id = findNodeIndexedIn("\\[", 1, //
        "void main() {",
        "  List x = [1, 2];",
        "  var y = x[0];",
        "}");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(MethodElement.class, element);
  }

  public void test_binaryOp() throws Exception {
    ASTNode id = findNodeIn("+", "var x = 3 + 4;");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(MethodElement.class, element);
  }

  public void test_classElement() throws Exception {
    ASTNode id = findNodeIn("A", "class A { }");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(ClassElement.class, element);
  }

  public void test_compilationUnit() throws Exception {
    CompilationUnit cu = resolve("// only comment");
    assertNotNull(cu.getElement());
    Element element = ElementLocator.locate(cu);
    assertSame(cu.getElement(), element);
  }

  public void test_ConstructorDeclaration() throws Exception {
    ASTNode id = findNodeIndexedIn("bar", 0, //
        "class A {",
        "  A.bar() {}",
        "}");
    ConstructorDeclaration declaration = id.getAncestor(ConstructorDeclaration.class);
    Element element = ElementLocator.locate(declaration);
    assertInstanceOf(ConstructorElement.class, element);
  }

  public void test_fieldElement() throws Exception {
    ASTNode id = findNodeIn("x", "class A { var x; }");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(FieldElement.class, element);
  }

  public void test_functionElement() throws Exception {
    ASTNode id = findNodeIndexedIn("bar", 1, //
        "int bar() => 42;",
        "void main() {",
        " var f = bar();",
        "}");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(FunctionElement.class, element);
  }

  public void test_importElement() throws Exception {

    addSource("/foo.dart", "library 'foo';");

    ASTNode id = findNodeIn("'foo.dart'", //
        "import 'foo.dart';",
        "class A { }");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(ImportElement.class, element);
  }

  public void test_InstanceCreationExpression() throws Exception {
    ASTNode node = findNodeIndexedIn("A(", 0, //
        "class A {}",
        "void main() {",
        " new A();",
        "}");
    Element element = ElementLocator.locate(node);
    assertInstanceOf(ConstructorElement.class, element);
  }

  public void test_methodElement() throws Exception {
    ASTNode id = findNodeIndexedIn("bar", 1, //
        "class A {",
        "  int bar() => 42;",
        "}",
        "void main() {",
        " var f = new A().bar();",
        "}");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(MethodElement.class, element);
  }

  public void test_MethodInvocation() throws Exception {
    String contents = createSource("foo(x) {}", //
        "void main() {",
        " foo(0);",
        "}");
    CompilationUnit cu = resolve(contents);
    MethodInvocation node = AbstractDartTest.findNode(
        cu,
        contents.indexOf("foo(0)"),
        MethodInvocation.class);
    Element element = ElementLocator.locate(node);
    assertInstanceOf(FunctionElement.class, element);
  }

  public void test_postfixOp() throws Exception {
    ASTNode id = findNodeIn("++", "int addOne(int x) => x++;");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(MethodElement.class, element);
  }

  public void test_prefixOp() throws Exception {
    ASTNode id = findNodeIn("++", "int addOne(int x) => ++x;");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(MethodElement.class, element);
  }

  public void test_propertAccessElement() throws Exception {
    ASTNode id = findNodeIn("length", //
        "void main() {",
        " int x = 'foo'.length;",
        "}");
    Element element = ElementLocator.locate(id);
    assertInstanceOf(PropertyAccessorElement.class, element);
  }

  /**
   * Find the first AST node matching a pattern in the resolved AST for the given source.
   * 
   * @param nodePattern the (unique) pattern used to identify the node of interest
   * @param lines the lines to be merged into a single source string
   * @return the matched node in the resolved AST for the given source lines
   * @throws Exception if source cannot be verified
   */
  private ASTNode findNodeIn(String nodePattern, String... lines) throws Exception {
    return findNodeIndexedIn(nodePattern, 0, lines);
  }

  /**
   * Find the AST node matching the given indexed occurrence of a pattern in the resolved AST for
   * the given source.
   * 
   * @param nodePattern the pattern used to identify the node of interest
   * @param index the index of the pattern match of interest
   * @param lines the lines to be merged into a single source string
   * @return the matched node in the resolved AST for the given source lines
   * @throws Exception if source cannot be verified
   */
  private ASTNode findNodeIndexedIn(String nodePattern, int index, String... lines)
      throws Exception {
    String contents = createSource(lines);
    CompilationUnit cu = resolve(contents);
    int start = getOffsetOfMatch(contents, nodePattern, index);
    int end = start + nodePattern.length();
    return new NodeLocator(start, end).searchWithin(cu);
  }

  /**
   * Parse, resolve and verify the given source lines to produce a fully resolved AST.
   * 
   * @param lines the lines to be merged into a single source string
   * @return the result of resolving the AST structure representing the content of the source
   * @throws Exception if source cannot be verified
   */
  private CompilationUnit resolve(String... lines) throws Exception {
    Source source = addSource(SRC_FILE_NAME, createSource(lines));
    LibraryElement library = resolve(source);
    assertNoErrors();
    verify(source);
    return getAnalysisContext().resolveCompilationUnit(source, library);
  }
}
