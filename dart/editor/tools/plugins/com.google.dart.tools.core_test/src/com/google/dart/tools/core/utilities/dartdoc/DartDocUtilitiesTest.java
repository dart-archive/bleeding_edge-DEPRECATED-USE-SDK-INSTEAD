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
package com.google.dart.tools.core.utilities.dartdoc;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.visitor.ElementLocator;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.resolver.ResolverTestCase;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

public class DartDocUtilitiesTest extends ResolverTestCase {
  public void test_class_doc() throws Exception {
    AstNode id = findNodeIn("A", createSource(//
        "/// My class",
        "class A { }"));
    Element element = ElementLocator.locate(id);
    assertEquals("My class\n", DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_class_doc_2() throws Exception {
    AstNode id = findNodeIn("A", createSource(//
        "/**",
        " * My class",
        " */",
        "class A { }"));
    Element element = ElementLocator.locate(id);
    assertEquals("My class\n", DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_class_doc_none() throws Exception {
    AstNode id = findNodeIn("A", "class A { }");
    Element element = ElementLocator.locate(id);
    assertEquals(null, DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_class_param__bound_text_summary() throws Exception {
    AstNode id = findNodeIn("A", "class Z<A extends List> { }");
    Element element = ElementLocator.locate(id);
    assertEquals("<A extends List>", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_class_param_text_summary() throws Exception {
    AstNode id = findNodeIn("A", "class Z<A> { }");
    Element element = ElementLocator.locate(id);
    assertEquals("<A>", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_class_param_text_summary_2() throws Exception {
    AstNode id = findNodeIn("'foo'", createSource(//
        "x(String s){}",
        "main() { x('foo'); }"));
    Element element = ElementLocator.locate(id);
    assertEquals(null, DartDocUtilities.getTextSummary(null, element));
  }

  public void test_class_text_summary() throws Exception {
    AstNode id = findNodeIn("A", "class A { }");
    Element element = ElementLocator.locate(id);
    assertEquals("A", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_codeBlock() throws Exception {
    AstNode id = findNodeIn("A {", createSource(//
        "/**",
        " * Example:",
        " *",
        " *     var v1 = new A();",
        " *     var v2 = new A();",
        " *",
        " * Done.",
        " */",
        "class A { }"));
    Element element = ElementLocator.locate(id);
    assertEquals("Example:\n<br><br>\n\n"
        + "<pre>    var v1 = new A();</pre><pre>\n    var v2 = new A();</pre>"
        + "\n<br><br>\nDone.\n", DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_cons_named_text_summary() throws Exception {
    AstNode id = findNodeIn("A.named", createSource(//
        "class A { ",
        "  A.named(String x){}",
        "}"));
    Element element = ElementLocator.locate(id);
    assertEquals("A.named(String x)", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_cons_text_summary() throws Exception {
    AstNode id = findNodeIn("A(", createSource(//
        "class A { ",
        "  A(String x){}",
        "}"));
    Element element = ElementLocator.locate(id);
    assertEquals("A(String x)", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_formal_params_text_summary() throws Exception {
    AstNode id = findNodeIn("index", createSource(//
        "class A { ",
        "  A(this.index);",
        "  int index;",
        "}"));
    Element element = ElementLocator.locate(id);
    assertEquals("int index", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_functionTypeAlias_summary() throws Exception {
    AstNode id = findNodeIn("FFF", createSource(//
        "/// My function type",
        "typedef int FFF(int a, double b);"));
    Element element = ElementLocator.locate(id);
    assertEquals("int FFF(int a, double b)", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_method_doc() throws Exception {
    AstNode id = findNodeIn("x", createSource(//
        "/// My method",
        "int x() => 42;"));
    Element element = ElementLocator.locate(id);
    assertEquals("My method\n", DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_method_named_doc() throws Exception {
    AstNode id = findNodeIn("x", "void x({String named}) {}");
    Element element = ElementLocator.locate(id);
    assertEquals("void x({String named})", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_method_named_doc_2() throws Exception {
    AstNode id = findNodeIn("x", "void x(int unnamed, {String named}) {}");
    Element element = ElementLocator.locate(id);
    assertEquals(
        "void x(int unnamed, {String named})",
        DartDocUtilities.getTextSummary(null, element));
  }

  public void test_method_null_body() throws Exception {
    AstNode id = findNodeIn("null", createSource(//
        "List<String> x()=> null;"));
    Element element = ElementLocator.locate(id);
    assertEquals(null, DartDocUtilities.getTextSummary(null, element));
  }

  public void test_method_optional_doc() throws Exception {
    AstNode id = findNodeIn("x", "void x([bool opt = false, bool opt2 = true]) {}");
    Element element = ElementLocator.locate(id);
    assertEquals(
        "void x([bool opt: false, bool opt2: true])",
        DartDocUtilities.getTextSummary(null, element));
  }

  public void test_method_paramed_text() throws Exception {
    AstNode id = findNodeIn("x", createSource(//
        "List<String> x()=> null;"));
    Element element = ElementLocator.locate(id);
    assertEquals("List<String> x()", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_orderedList() throws Exception {
    AstNode id = findNodeIn("A {", createSource(//
        "/**",
        " * Example:",
        " *",
        " * 1. aaa",
        " * 2. bbb",
        " * 3. ccc",
        " *",
        " * Done.",
        " */",
        "class A { }"));
    Element element = ElementLocator.locate(id);
    assertEquals("Example:\n<br><br>\n\n"
        + "<pre>    </pre>1. aaa<br><pre>    </pre>2. bbb<br><pre>    </pre>3. ccc<br>"
        + "\n<br><br>\nDone.\n", DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_param__with_default_value_text_summary() throws Exception {
    AstNode id = findNodeIn("foo", createSource(//
        "class A { ",
        "  void foo({bool x: false}){}",
        "}"));
    Element element = ElementLocator.locate(id);
    assertEquals("void foo({bool x: false})", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_param_text_summary() throws Exception {
    AstNode id = findNodeIn("x", createSource(//
        "class A { ",
        "  A(String x){}",
        "}"));
    Element element = ElementLocator.locate(id);
    assertEquals("String x", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_unorderedList() throws Exception {
    AstNode id = findNodeIn("A {", createSource(//
        "/**",
        " * Example:",
        " *",
        " * * aaa",
        " * * bbb",
        " * * ccc",
        " *",
        " * Done.",
        " */",
        "class A { }"));
    Element element = ElementLocator.locate(id);
    assertEquals(
        "Example:\n<br><br>\n\n<li>aaa</li>\n<li>bbb</li>\n<li>ccc</li>\n<br><br>\n\nDone.\n",
        DartDocUtilities.getDartDocAsHtml(element));
  }

  public void test_var_text() throws Exception {
    AstNode id = findNodeIn("x", "int x;\n");
    Element element = ElementLocator.locate(id);
    assertEquals("int x", DartDocUtilities.getTextSummary(null, element));
  }

  public void test_var_text_withType() throws Exception {
    AstNode id = findNodeIn("x", "var x = 42;\n");
    Type type = ((Expression) id).getBestType();
    Element element = ElementLocator.locate(id);
    assertEquals("int x", DartDocUtilities.getTextSummary(type, element));
  }

  private AstNode findNodeIn(String nodePattern, String... lines) throws Exception {
    String contents = createSource(lines);
    CompilationUnit cu = resolve(contents);
    int start = contents.indexOf(nodePattern);
    int end = start + nodePattern.length();
    return new NodeLocator(start, end).searchWithin(cu);
  }

  private CompilationUnit resolve(String... lines) throws Exception {
    Source source = addSource(createSource(lines));
    LibraryElement library = resolve(source);
    assertNoErrors(source);
    verify(source);
    return getAnalysisContext().resolveCompilationUnit(source, library);
  }
}
