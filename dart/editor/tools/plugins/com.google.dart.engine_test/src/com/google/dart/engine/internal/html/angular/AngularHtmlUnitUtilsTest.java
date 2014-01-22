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
      Element element = HtmlUnitUtils.getElement(indexUnit, 0);
      assertNull(element);
    }
    // has expression at offset
    {
      int offset = indexContent.indexOf("field");
      Element element = HtmlUnitUtils.getElement(indexUnit, offset);
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
    // try without unit
    assertNull(HtmlUnitUtils.getExpression(null, offset));
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
