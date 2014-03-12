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
package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Tests for {@link HtmlUnitUtils} for Angular HTMLs.
 */
public class AngularHtmlUnitUtilsTest extends AngularTest {
  public void test_getElement_forExpression() throws Exception {
    addMyController();
    resolveSimpleCtrlFieldHtml();
    // prepare expression
    int offset = indexContent.indexOf("ctrl");
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, offset);
    // get element
    Element element = HtmlUnitUtils.getElement(expression);
    assertInstanceOf(VariableElement.class, element);
    assertEquals("ctrl", element.getName());
  }

  public void test_getElement_forExpression_null() throws Exception {
    Element element = HtmlUnitUtils.getElement(null);
    assertNull(element);
  }

  public void test_getElement_forOffset() throws Exception {
    addMyController();
    resolveSimpleCtrlFieldHtml();
    // no expression
    {
      Element element = HtmlUnitUtils.getElementAtOffset(indexUnit, 0);
      assertNull(element);
    }
    // has expression at offset
    {
      int offset = indexContent.indexOf("field");
      Element element = HtmlUnitUtils.getElementAtOffset(indexUnit, offset);
      assertInstanceOf(PropertyAccessorElement.class, element);
      assertEquals("field", element.getName());
    }
  }

  public void test_getElementToOpen_controller() throws Exception {
    addMyController();
    resolveSimpleCtrlFieldHtml();
    // prepare expression
    int offset = indexContent.indexOf("ctrl");
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, offset);
    // get element
    Element element = HtmlUnitUtils.getElementToOpen(indexUnit, expression);
    assertInstanceOf(AngularControllerElement.class, element);
    assertEquals("ctrl", element.getName());
  }

  public void test_getElementToOpen_field() throws Exception {
    addMyController();
    resolveSimpleCtrlFieldHtml();
    // prepare expression
    int offset = indexContent.indexOf("field");
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, offset);
    // get element
    Element element = HtmlUnitUtils.getElementToOpen(indexUnit, expression);
    assertInstanceOf(PropertyAccessorElement.class, element);
    assertEquals("field", element.getName());
  }

  public void test_getEnclosingTagNode() throws Exception {
    resolveIndex(createSource(//
        "<html>",
        "  <body ng-app>",
        "    <badge name='abc'> 123 </badge>",
        "  </body>",
        "</html>"));
    // no unit
    assertNull(HtmlUnitUtils.getEnclosingTagNode(null, 0));
    // wrong offset
    assertNull(HtmlUnitUtils.getEnclosingTagNode(indexUnit, -1));
    // valid offset
    XmlTagNode expected = getEnclosingTagNode("<badge");
    assertNotNull(expected);
    assertEquals("badge", expected.getTag());
    assertSame(expected, getEnclosingTagNode("badge"));
    assertSame(expected, getEnclosingTagNode("name="));
    assertSame(expected, getEnclosingTagNode("123"));
    assertSame(expected, getEnclosingTagNode("/badge"));
  }

  public void test_getExpression() throws Exception {
    addMyController();
    resolveSimpleCtrlFieldHtml();
    // try offset without expression
    assertNull(HtmlUnitUtils.getExpression(indexUnit, 0));
    // try offset with expression
    int offset = indexContent.indexOf("ctrl");
    assertNotNull(HtmlUnitUtils.getExpression(indexUnit, offset));
    assertNotNull(HtmlUnitUtils.getExpression(indexUnit, offset + 1));
    assertNotNull(HtmlUnitUtils.getExpression(indexUnit, offset + 2));
    assertNotNull(HtmlUnitUtils.getExpression(indexUnit, offset + "ctrl.field".length()));
    // try without unit
    assertNull(HtmlUnitUtils.getExpression(null, offset));
  }

  public void test_getTagNode() throws Exception {
    resolveIndex(createSource(//
        "<html>",
        "  <body ng-app>",
        "    <badge name='abc'> 123 </badge> done",
        "  </body>",
        "</html>"));
    // no unit
    assertNull(HtmlUnitUtils.getTagNode(null, 0));
    // wrong offset
    assertNull(HtmlUnitUtils.getTagNode(indexUnit, -1));
    // on tag name
    XmlTagNode expected = getTagNode("badge name=");
    assertNotNull(expected);
    assertEquals("badge", expected.getTag());
    assertSame(expected, getTagNode("badge"));
    assertSame(expected, getTagNode(" name="));
    assertSame(expected, getTagNode("adge name="));
    assertSame(expected, getTagNode("badge>"));
    assertSame(expected, getTagNode("adge>"));
    assertSame(expected, getTagNode("> done"));
    // in tag node, but not on the name token
    assertNull(getTagNode("name="));
    assertNull(getTagNode("123"));
  }

  private XmlTagNode getEnclosingTagNode(String search) {
    return HtmlUnitUtils.getEnclosingTagNode(indexUnit, indexContent.indexOf(search));
  }

  private XmlTagNode getTagNode(String search) {
    return HtmlUnitUtils.getTagNode(indexUnit, indexContent.indexOf(search));
  }

  private void resolveSimpleCtrlFieldHtml() throws Exception {
    resolveIndex(createSource(//
        "<html>",
        "  <body ng-app>",
        "    <div my-controller>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
  }
}
