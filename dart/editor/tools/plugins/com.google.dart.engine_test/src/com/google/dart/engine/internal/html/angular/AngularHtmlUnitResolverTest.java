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
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;

public class AngularHtmlUnitResolverTest extends AngularTest {
  public void test_NgComponent_resolveTemplateFile() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "  String field;",
        "}"));
    addIndexSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.field}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveIndex();
    assertNoErrors();
    assertResolvedIdentifier("ctrl.", "MyComponent");
    assertResolvedIdentifier("field}}", "String");
  }

  public void test_NgComponent_use_resolveAttributes() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {'attrA' : '=>setA', 'attrB' : '@setB'})",
        "class MyComponent {",
        "  set setA(value) {}",
        "  set setB(value) {}",
        "}"));
    resolveIndex(createHtmlWithMyController(//
        "<input type='text' ng-model='someModel'/>",
        "<myComponent attrA='someModel' attrB='bbb'/>"));
    assertNoErrors();
    verify(indexSource);
    // "attrA" attribute expression was resolved
    assertNotNull(findIdentifier("someModel"));
    // "myComponent" tag was resolved
    XmlTagNode tagNode = HtmlUnitUtils.getTagNode(indexUnit, findOffset("myComponent"));
    AngularSelectorElement tagElement = (AngularSelectorElement) tagNode.getElement();
    assertNotNull(tagElement);
    assertEquals("myComponent", tagElement.getName());
    assertEquals(findMainOffset("myComponent', // selector"), tagElement.getNameOffset());
    // "attrA" attribute was resolved
    {
      XmlAttributeNode node = HtmlUnitUtils.getAttributeNode(indexUnit, findOffset("attrA='"));
      AngularPropertyElement element = (AngularPropertyElement) node.getElement();
      assertNotNull(element);
      assertEquals("attrA", element.getName());
      assertEquals("setA", element.getField().getName());
    }
    // "attrB" attribute was resolved, even if it @binding
    {
      XmlAttributeNode node = HtmlUnitUtils.getAttributeNode(indexUnit, findOffset("attrB='"));
      AngularPropertyElement element = (AngularPropertyElement) node.getElement();
      assertNotNull(element);
      assertEquals("attrB", element.getName());
      assertEquals("setB", element.getField().getName());
    }
  }

  public void test_NgDirective_resolvedExpression_attrString() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(",
        "    selector: '[my-directive]',",
        "    map: const {'my-directive' : '@condition'})",
        "class MyDirective {",
        "  set condition(value) {}",
        "}"));
    resolveIndex(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<div my-directive='name != null'>",
        "</div>"));
    assertNoErrors();
    verify(indexSource);
    // @condition means "string attribute", which we don't parse
    assertNull(findIdentifierMaybe("name != null"));
    // "my-directive" attribute was resolved
    XmlAttributeNode attrNode = HtmlUnitUtils.getAttributeNode(
        indexUnit,
        findOffset("my-directive='"));
    assertNotNull(attrNode);
    AngularPropertyElement propertyElement = (AngularPropertyElement) attrNode.getElement();
    assertNotNull(propertyElement);
    assertSame(AngularPropertyKind.ATTR, propertyElement.getPropertyKind());
  }

  public void test_NgDirective_resolvedExpression_dotAsName() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(",
        "    selector: '[my-directive]',",
        "    map: const {'.' : '=>condition'})",
        "class MyDirective {",
        "  set condition(value) {}",
        "}"));
    resolveIndex(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<div my-directive='name != null'>",
        "</div>"));
    assertNoErrors();
    verify(indexSource);
    // "name" attribute was resolved
    assertNotNull(findIdentifier("name != null"));
  }

  public void test_NgDirective_resolvedExpression_oneWay() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(",
        "    selector: '[my-directive]',",
        "    map: const {'my-directive' : '=>condition'})",
        "class MyDirective {",
        "  set condition(value) {}",
        "}"));
    resolveIndex(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<div my-directive='name != null'>",
        "</div>"));
    assertNoErrors();
    verify(indexSource);
    // "name" expression was resolved
    assertNotNull(findIdentifier("name != null"));
    // "my-directive" attribute was resolved
    XmlAttributeNode attrNode = HtmlUnitUtils.getAttributeNode(
        indexUnit,
        findOffset("my-directive='"));
    assertNotNull(attrNode);
    AngularPropertyElement propertyElement = (AngularPropertyElement) attrNode.getElement();
    assertNotNull(propertyElement);
    assertSame(AngularPropertyKind.ONE_WAY, propertyElement.getPropertyKind());
    assertEquals("condition", propertyElement.getField().getName());
  }

  public void test_ngModel_modelAfterUsage() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<h3>Hello {{name}}!</h3>",
        "<input type='text' ng-model='name'>"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("name}}!", "String");
    assertResolvedIdentifier("name'>", "String");
  }

  public void test_ngModel_modelBeforeUsage() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<h3>Hello {{name}}!</h3>"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("name}}!", "String");
    Element element = assertResolvedIdentifier("name'>", "String");
    assertEquals("name", element.getName());
    assertEquals(findOffset("name'>"), element.getNameOffset());
  }

  public void test_ngModel_notIdentifier() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController("<input type='text' ng-model='ctrl.field'>"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("field'>", "String");
  }

  public void test_ngRepeat_bad_expectedIdentifier() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat='name + 42 in ctrl.names'>",
        "</li>"));
    assertErrors(indexSource, AngularCode.EXPECTED_IDENTIFIER);
  }

  public void test_ngRepeat_bad_expectedIn() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat='name : ctrl.names'>",
        "</li>"));
    assertErrors(indexSource, AngularCode.EXPECTED_IN);
  }

  public void test_ngRepeat_resolvedExpressions() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat='name in ctrl.names'>",
        "  {{name}}",
        "</li>"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("name in", "String");
    assertResolvedIdentifier("ctrl.", "MyController");
    assertResolvedIdentifier("names'", "List<String>");
    assertResolvedIdentifier("name}}", "String");
  }

  public void test_notResolved_noDartScript() throws Exception {
    resolveIndex(createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "  </body>",
        "</html>"));
    assertNoErrors();
    // Angular is not initialized, so "ctrl" is not parsed
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, findOffset("ctrl"));
    assertNull(expression);
  }

  public void test_notResolved_notAngular() throws Exception {
    resolveIndex(createSource(//
        "<html no-ng-app>",
        "  <body>",
        "    <div my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "  </body>",
        "</html>"));
    assertNoErrors();
    // Angular is not initialized, so "ctrl" is not parsed
    Expression expression = HtmlUnitUtils.getExpression(indexUnit, findOffset("ctrl"));
    assertNull(expression);
  }

  public void test_notResolved_wrongControllerMarker() throws Exception {
    addMyController();
    resolveIndex(createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div not-my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    assertErrors(indexSource, StaticWarningCode.UNDEFINED_IDENTIFIER);
    // "ctrl" is not resolved
    SimpleIdentifier identifier = findIdentifier("ctrl");
    assertNull(identifier.getBestElement());
  }

  public void test_resolveExpression_evenWithout_ngBootstrap() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgController(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  String field;",
        "}"));
    resolveIndex(createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div my-controller>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    assertNoErrors();
    assertResolvedIdentifier("ctrl.", "MyController");
  }

  public void test_resolveExpression_inAttribute() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController("<button title='{{ctrl.field}}'></button>"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("ctrl", "MyController");
  }

  public void test_resolveExpression_inTag() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController("{{ctrl.field}}"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("ctrl", "MyController");
  }

  public void test_resolveExpression_ngApp_onBody() throws Exception {
    addMyController();
    resolveIndex(createSource(//
        "<html>",
        "  <body ng-app>",
        "    <div my-controller>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    assertNoErrors();
    verify(indexSource);
    assertResolvedIdentifier("ctrl", "MyController");
  }
}
