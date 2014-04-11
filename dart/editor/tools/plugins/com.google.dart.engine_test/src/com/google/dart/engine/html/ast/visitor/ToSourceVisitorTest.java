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
package com.google.dart.engine.html.ast.visitor;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import static com.google.dart.engine.html.HtmlFactory.attribute;
import static com.google.dart.engine.html.HtmlFactory.list;
import static com.google.dart.engine.html.HtmlFactory.scriptTag;
import static com.google.dart.engine.html.HtmlFactory.scriptTagWithContent;
import static com.google.dart.engine.html.HtmlFactory.tagNode;

import java.util.ArrayList;

/**
 * Instances of the class {@code ToSourceVisitorTest}
 */
public class ToSourceVisitorTest extends EngineTestCase {
  public void fail_visitHtmlScriptTagNode_attributes_content() {
    assertSource(
        "<script type='application/dart'>f() {}</script>",
        scriptTagWithContent("f() {}", attribute("type", "'application/dart'")));
  }

  public void fail_visitHtmlScriptTagNode_noAttributes_content() {
    assertSource("<script>f() {}</script>", scriptTagWithContent("f() {}"));
  }

  public void test_visitHtmlScriptTagNode_attributes_noContent() {
    assertSource(
        "<script type='application/dart'/>",
        scriptTag(attribute("type", "'application/dart'")));
  }

  public void test_visitHtmlScriptTagNode_noAttributes_noContent() {
    assertSource("<script/>", scriptTag());
  }

  public void test_visitHtmlUnit_empty() {
    assertSource("", new HtmlUnit(null, new ArrayList<XmlTagNode>(), null));
  }

  public void test_visitHtmlUnit_nonEmpty() {
    assertSource("<html/>", new HtmlUnit(null, list(tagNode("html")), null));
  }

  public void test_visitXmlAttributeNode() {
    assertSource("x=y", attribute("x", "y"));
  }

  /**
   * Assert that a {@code ToSourceVisitor} will produce the expected source when visiting the given
   * node.
   * 
   * @param expectedSource the source string that the visitor is expected to produce
   * @param node the AST node being visited to produce the actual source
   */
  private void assertSource(String expectedSource, XmlNode node) {
    PrintStringWriter writer = new PrintStringWriter();
    node.accept(new ToSourceVisitor(writer));
    assertEquals(expectedSource, writer.toString());
  }
}
