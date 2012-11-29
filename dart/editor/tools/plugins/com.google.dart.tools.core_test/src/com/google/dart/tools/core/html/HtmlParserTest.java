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

package com.google.dart.tools.core.html;

import junit.framework.TestCase;

public class HtmlParserTest extends TestCase {

  public void test_parse1() {
    XmlDocument root = new XmlDocument();
    XmlElement htmlNode = new XmlElement("html");
    root.addChild(htmlNode);
    htmlNode.addChild(new XmlElement("p"));
    htmlNode.addChild(new XmlElement("p"));

    verifyParseTree("<html><p></p><p></p></html>", root);
  }

  public void test_parse2() {
    XmlDocument root = new XmlDocument();
    XmlElement htmlNode = new XmlElement("html");
    root.addChild(htmlNode);
    htmlNode.addChild(new XmlElement("p"));
    htmlNode.addChild(new XmlElement("br"));
    htmlNode.addChild(new XmlElement("p"));

    verifyParseTree("<html><p>foo</p><br/><p></p></html>", root);
  }

  public void test_parse3() {
    XmlDocument root = new XmlDocument();
    XmlElement htmlNode = new XmlElement("html");
    root.addChild(htmlNode);
    htmlNode.addChild(new XmlElement("foo"));
    htmlNode.addChild(new XmlElement("p"));

    verifyParseTree("<html><foo <p></p></html>", root);
  }

  public void test_parse4() {
    XmlDocument root = new XmlDocument();
    XmlElement htmlNode = new XmlElement("html");
    root.addChild(htmlNode);
    htmlNode.addChild(new XmlElement("script"));
    htmlNode.addChild(new XmlElement("p"));

    XmlDocument actual = verifyParseTree(
        "<html><script >here is < some</script><p></p></html>",
        root);

    XmlElement script = (XmlElement) actual.getChildren().get(0).getChildren().get(0);
    assertEquals("here is < some", script.getContents());
  }

  private void validate(XmlNode expectedNode, XmlNode actualNode) {
    assertEquals(expectedNode.getLabel(), actualNode.getLabel());
    assertEquals(expectedNode.getChildren().size(), actualNode.getChildren().size());

    for (int i = 0; i < expectedNode.getChildren().size(); i++) {
      validate(expectedNode.getChildren().get(i), actualNode.getChildren().get(i));
    }
  }

  private XmlDocument verifyParseTree(String input, XmlDocument expectedRoot) {
    HtmlParser parser = new HtmlParser(input);

    XmlDocument root = parser.parse();

    validate(expectedRoot, root);

    return root;
  }

}
