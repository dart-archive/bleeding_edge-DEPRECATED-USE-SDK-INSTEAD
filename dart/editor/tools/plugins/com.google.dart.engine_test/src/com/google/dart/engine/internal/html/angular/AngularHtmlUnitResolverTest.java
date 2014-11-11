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
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.angular.AngularControllerElementImpl;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.compilationUnit;
import static com.google.dart.engine.element.ElementFactory.functionElement;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

public class AngularHtmlUnitResolverTest extends AngularTest {
  public void test_analysisContext_changeDart_invalidateApplication() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "}"));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.noMethod()}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    // there are some errors in my_template.html
    {
      AnalysisError[] errors = context.getErrors(indexSource).getErrors();
      assertTrue(errors.length != 0);
    }
    // change main.dart, there are no MyComponent anymore
    context.setContents(mainSource, "");
    // ...errors in my_template.html should be removed
    {
      AnalysisError[] errors = context.getErrors(indexSource).getErrors();
      assertTrue(errors.length == 0);
    }
  }

  public void test_analysisContext_changeEntryPoint_clearAngularErrors_inDart() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'no-such-template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "}"));
    Source entrySource = contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    // there are some errors in MyComponent
    {
      AnalysisError[] errors = context.getErrors(mainSource).getErrors();
      assertTrue(errors.length != 0);
    }
    // make entry-point.html non-Angular
    context.setContents(entrySource, "<html/>");
    // ...errors in MyComponent should be removed
    {
      AnalysisError[] errors = context.getErrors(mainSource).getErrors();
      assertTrue(errors.length == 0);
    }
  }

  public void test_analysisContext_changeEntryPoint_clearAngularErrors_inTemplate()
      throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "}"));
    Source entrySource = contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.noMethod()}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    // there are some errors in my_template.html
    {
      AnalysisError[] errors = context.getErrors(indexSource).getErrors();
      assertTrue(errors.length != 0);
    }
    // make entry-point.html non-Angular
    context.setContents(entrySource, "<html/>");
    // ...errors in my_template.html should be removed
    {
      AnalysisError[] errors = context.getErrors(indexSource).getErrors();
      assertTrue(errors.length == 0);
    }
  }

  public void test_analysisContext_removeEntryPoint_clearAngularErrors_inDart() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'no-such-template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "}"));
    Source entrySource = contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    // there are some errors in MyComponent
    {
      AnalysisError[] errors = context.getErrors(mainSource).getErrors();
      assertTrue(errors.length != 0);
    }
    // remove entry-point.html
    {
      ChangeSet changeSet = new ChangeSet();
      changeSet.removedSource(entrySource);
      context.applyChanges(changeSet);
    }
    // ...errors in MyComponent should be removed
    {
      AnalysisError[] errors = context.getErrors(mainSource).getErrors();
      assertTrue(errors.length == 0);
    }
  }

  public void test_contextProperties() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithAngular(//
        "<div>",
        "  {{$id}}",
        "  {{$parent}}",
        "  {{$root}}",
        "</div>"));
    assertResolvedIdentifier("$id");
    assertResolvedIdentifier("$parent");
    assertResolvedIdentifier("$root");
  }

  public void test_getAngularElement_isAngular() throws Exception {
    // prepare local variable "name" in compilation unit
    CompilationUnitElementImpl unit = compilationUnit("test.dart");
    FunctionElementImpl function = functionElement("main");
    unit.setFunctions(new FunctionElement[] {function});
    LocalVariableElementImpl local = localVariableElement("name");
    function.setLocalVariables(new LocalVariableElement[] {local});
    // set AngularElement
    AngularElement angularElement = new AngularControllerElementImpl("ctrl", 0);
    local.setToolkitObjects(new AngularElement[] {angularElement});
    assertSame(angularElement, AngularHtmlUnitResolver.getAngularElement(local));
  }

  public void test_getAngularElement_notAngular() throws Exception {
    Element element = localVariableElement("name");
    assertNull(AngularHtmlUnitResolver.getAngularElement(element));
  }

  public void test_getAngularElement_notLocal() throws Exception {
    Element element = classElement("Test");
    assertNull(AngularHtmlUnitResolver.getAngularElement(element));
  }

  /**
   * Test that we resolve "ng-click" expression.
   */
  public void test_ngClick() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("<button ng-click='ctrl.doSomething($event)'/>"));
    assertResolvedIdentifier("doSomething");
  }

  public void test_NgComponent_resolveTemplateFile() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "  String field;",
        "}"));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
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

  public void test_NgComponent_updateDartFile() throws Exception {
    Source componentSource = contextHelper.addSource("/my_component.dart", createSource(//
        "library my.component;",
        "import 'angular.dart';",
        "@Component(selector: 'myComponent')",
        "class MyComponent {",
        "}"));
    contextHelper.addSource(
        "/my_module.dart",
        createSource("library my.module;", "import 'my_component.dart';"));
    addMainSource(createSource("library main;", "import 'my_module.dart';"));
    resolveIndexNoErrors(createHtmlWithMyController("<myComponent/>"));
    // "myComponent" tag was resolved
    {
      XmlTagNode tagNode = HtmlUnitUtils.getTagNode(indexUnit, findOffset("myComponent"));
      AngularSelectorElement tagElement = (AngularSelectorElement) tagNode.getElement();
      assertNotNull(tagElement);
      assertEquals("myComponent", tagElement.getName());
    }
    // replace "myComponent" with "myComponent2" in my_component.dart and index.html
    {
      context.setContents(
          componentSource,
          getSourceContent(componentSource).replace("myComponent", "myComponent2"));
      indexContent = getSourceContent(indexSource).replace("myComponent", "myComponent2");
      context.setContents(indexSource, indexContent);
    }
    contextHelper.runTasks();
    resolveIndex();
    // "myComponent2" tag should be resolved
    {
      XmlTagNode tagNode = HtmlUnitUtils.getTagNode(indexUnit, findOffset("myComponent2"));
      AngularSelectorElement tagElement = (AngularSelectorElement) tagNode.getElement();
      assertNotNull(tagElement);
      assertEquals("myComponent2", tagElement.getName());
    }
  }

  public void test_NgComponent_use_resolveAttributes() throws Exception {
    contextHelper.addSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.field}}",
        "    </div>"));
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {'attrA' : '=>setA', 'attrB' : '@setB'})",
        "class MyComponent {",
        "  set setA(value) {}",
        "  set setB(value) {}",
        "}"));
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<input type='text' ng-model='someModel'/>",
        "<myComponent attrA='someModel' attrB='bbb'/>"));
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

  public void test_NgDirective_noAttribute() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(selector: '[my-directive]', map: const {'foo': '=>input'})",
        "class MyDirective {",
        "  set input(value) {}",
        "}"));
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<div my-directive>",
        "</div>"));
    // no "foo" attribute, but it is OK
  }

  public void test_NgDirective_noExpression() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(selector: '[my-directive]', map: const {'.': '=>input'})",
        "class MyDirective {",
        "  set input(value) {}",
        "}"));
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<div my-directive>",
        "</div>"));
  }

  public void test_NgDirective_resolvedExpression() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Decorator(selector: '[my-directive]')",
        "class MyDirective {",
        "  @NgOneWay('my-property')",
        "  String condition;",
        "}"));
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<div my-directive my-property='name != null'>",
        "</div>"));
    resolveMainNoErrors();
    // "my-directive" attribute was resolved
    {
      AngularSelectorElement selector = findMainElement(
          ElementKind.ANGULAR_SELECTOR,
          "my-directive");
      XmlAttributeNode attrNodeSelector = HtmlUnitUtils.getAttributeNode(
          indexUnit,
          findOffset("my-directive"));
      assertNotNull(attrNodeSelector);
      assertSame(selector, attrNodeSelector.getElement());
    }
    // "my-property" attribute was resolved
    {
      XmlAttributeNode attrNodeProperty = HtmlUnitUtils.getAttributeNode(
          indexUnit,
          findOffset("my-property='"));
      AngularPropertyElement propertyElement = (AngularPropertyElement) attrNodeProperty.getElement();
      assertNotNull(propertyElement);
      assertSame(AngularPropertyKind.ONE_WAY, propertyElement.getPropertyKind());
      assertEquals("condition", propertyElement.getField().getName());
    }
    // "name" expression was resolved
    assertNotNull(findIdentifier("name != null"));
  }

  public void test_NgDirective_resolvedExpression_attrString() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@NgDirective(selector: '[my-directive])",
        "class MyDirective {",
        "  @NgAttr('my-property')",
        "  String property;",
        "}"));
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<div my-directive my-property='name != null'>",
        "</div>"));
    resolveMain();
    // @NgAttr means "string attribute", which we don't parse
    assertNull(findIdentifierMaybe("name != null"));
  }

  public void test_NgDirective_resolvedExpression_dotAsName() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Decorator(",
        "    selector: '[my-directive]',",
        "    map: const {'.' : '=>condition'})",
        "class MyDirective {",
        "  set condition(value) {}",
        "}"));
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<div my-directive='name != null'>",
        "</div>"));
    // "name" attribute was resolved
    assertNotNull(findIdentifier("name != null"));
  }

  /**
   * Test that we resolve "ng-if" expression.
   */
  public void test_ngIf() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("<div ng-if='ctrl.field != null'/>"));
    assertResolvedIdentifier("field");
  }

  public void test_ngModel_modelAfterUsage() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<h3>Hello {{name}}!</h3>",
        "<input type='text' ng-model='name'>"));
    assertResolvedIdentifier("name}}!", "String");
    assertResolvedIdentifier("name'>", "String");
  }

  public void test_ngModel_modelBeforeUsage() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<input type='text' ng-model='name'>",
        "<h3>Hello {{name}}!</h3>"));
    assertResolvedIdentifier("name}}!", "String");
    Element element = assertResolvedIdentifier("name'>", "String");
    assertEquals("name", element.getName());
    assertEquals(findOffset("name'>"), element.getNameOffset());
  }

  public void test_ngModel_notIdentifier() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("<input type='text' ng-model='ctrl.field'>"));
    assertResolvedIdentifier("field'>", "String");
  }

  /**
   * Test that we resolve "ng-mouseout" expression.
   */
  public void test_ngMouseOut() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("<button ng-mouseout='ctrl.doSomething($event)'/>"));
    assertResolvedIdentifier("doSomething");
  }

  public void test_ngRepeat_additionalVariables() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat='name in ctrl.names'>",
        "  {{$index}} {{$first}} {{$middle}} {{$last}} {{$even}} {{$odd}}",
        "</li>"));
    assertResolvedIdentifier("$index", "int");
    assertResolvedIdentifier("$first", "bool");
    assertResolvedIdentifier("$middle", "bool");
    assertResolvedIdentifier("$last", "bool");
    assertResolvedIdentifier("$even", "bool");
    assertResolvedIdentifier("$odd", "bool");
  }

  public void test_ngRepeat_bad_expectedIdentifier() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat='name + 42 in ctrl.names'>",
        "</li>"));
    assertErrors(indexSource, AngularCode.INVALID_REPEAT_ITEM_SYNTAX);
  }

  public void test_ngRepeat_bad_expectedIn() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat='name : ctrl.names'>",
        "</li>"));
    assertErrors(indexSource, AngularCode.INVALID_REPEAT_SYNTAX);
  }

  public void test_ngRepeat_filters_filter_literal() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat='item in ctrl.items | filter:42:null'/>",
        "</li>"));
    // filter "filter" is resolved
    Element filterElement = assertResolvedIdentifier("filter");
    assertInstanceOf(AngularFormatterElement.class, filterElement);
  }

  public void test_ngRepeat_filters_filter_propertyMap() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat='item in ctrl.items | filter:{name:null, done:false}'/>",
        "</li>"));
    assertResolvedIdentifier("name:", "String");
    assertResolvedIdentifier("done:", "bool");
  }

  public void test_ngRepeat_filters_missingColon() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'' true\"/>",
        "</li>"));
    assertErrors(indexSource, AngularCode.MISSING_FORMATTER_COLON);
  }

  public void test_ngRepeat_filters_noArgs() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy\"/>",
        "</li>"));
    // filter "orderBy" is resolved
    Element filterElement = assertResolvedIdentifier("orderBy");
    assertInstanceOf(AngularFormatterElement.class, filterElement);
  }

  public void test_ngRepeat_filters_orderBy_emptyString() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'':true\"/>",
        "</li>"));
    // filter "orderBy" is resolved
    Element filterElement = assertResolvedIdentifier("orderBy");
    assertInstanceOf(AngularFormatterElement.class, filterElement);
  }

  public void test_ngRepeat_filters_orderBy_propertyList() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:['name', 'done']\"/>",
        "</li>"));
    assertResolvedIdentifier("name'", "String");
    assertResolvedIdentifier("done'", "bool");
  }

  public void test_ngRepeat_filters_orderBy_propertyName() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'name'\"/>",
        "</li>"));
    assertResolvedIdentifier("name'", "String");
  }

  public void test_ngRepeat_filters_orderBy_propertyName_minus() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'-name'\"/>",
        "</li>"));
    assertResolvedIdentifier("name'", "String");
  }

  public void test_ngRepeat_filters_orderBy_propertyName_plus() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'+name'\"/>",
        "</li>"));
    assertResolvedIdentifier("name'", "String");
  }

  public void test_ngRepeat_filters_orderBy_propertyName_untypedItems() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.untypedItems | orderBy:'name'\"/>",
        "</li>"));
    assertResolvedIdentifier("name'", "dynamic");
  }

  public void test_ngRepeat_filters_two() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'+' | orderBy:'-'\"/>",
        "</li>"));
    assertInstanceOf(AngularFormatterElement.class, assertResolvedIdentifier("orderBy:'+'"));
    assertInstanceOf(AngularFormatterElement.class, assertResolvedIdentifier("orderBy:'-'"));
  }

  public void test_ngRepeat_resolvedExpressions() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat='name in ctrl.names'>",
        "  {{name}}",
        "</li>"));
    assertResolvedIdentifier("name in", "String");
    assertResolvedIdentifier("ctrl.", "MyController");
    assertResolvedIdentifier("names'", "List<String>");
    assertResolvedIdentifier("name}}", "String");
  }

  public void test_ngRepeat_trackBy() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController(//
        "<li ng-repeat='name in ctrl.names track by name.length'/>",
        "</li>"));
    assertResolvedIdentifier("length'", "int");
  }

  /**
   * Test that we resolve "ng-show" expression.
   */
  public void test_ngShow() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("<div ng-show='ctrl.field != null'/>"));
    assertResolvedIdentifier("field");
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
    addIndexSource(createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div not-my-marker>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    contextHelper.runTasks();
    resolveIndex();
    // no errors, because we decided to ignore them at the moment
    assertNoErrors();
    // "ctrl" is not resolved
    SimpleIdentifier identifier = findIdentifier("ctrl");
    assertNull(identifier.getBestElement());
  }

  public void test_resolveExpression_evenWithout_ngBootstrap() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Controller(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  String field;",
        "}"));
    resolveIndexNoErrors(createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div my-controller>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    assertResolvedIdentifier("ctrl.", "MyController");
  }

  public void test_resolveExpression_ignoreUnresolved() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Controller(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  Map map;",
        "  Object obj;",
        "}"));
    resolveIndex(createSource(//
        "<html ng-app>",
        "  <body>",
        "    <div my-controller>",
        "      {{ctrl.map.property}}",
        "      {{ctrl.obj.property}}",
        "      {{invisibleScopeProperty}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    assertNoErrors();
    // "ctrl.map" and "ctrl.obj" are resolved
    assertResolvedIdentifier("map", "Map<dynamic, dynamic>");
    assertResolvedIdentifier("obj", "Object");
    // ...but not "invisibleScopeProperty"
    {
      SimpleIdentifier identifier = findIdentifier("invisibleScopeProperty");
      assertNull(identifier.getBestElement());
    }
  }

  public void test_resolveExpression_inAttribute() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("<button title='{{ctrl.field}}'></button>"));
    assertResolvedIdentifier("ctrl", "MyController");
  }

  public void test_resolveExpression_ngApp_onBody() throws Exception {
    addMyController();
    resolveIndexNoErrors(createSource(//
        "<html>",
        "  <body ng-app>",
        "    <div my-controller>",
        "      {{ctrl.field}}",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>"));
    assertResolvedIdentifier("ctrl", "MyController");
  }

  public void test_resolveExpression_withFilter() throws Exception {
    addMyController();
    resolveIndexNoErrors(createHtmlWithMyController("{{ctrl.field | uppercase}}"));
    assertResolvedIdentifier("ctrl", "MyController");
    assertResolvedIdentifier("uppercase");
  }

  public void test_resolveExpression_withFilter_missingColon() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController("{{ctrl.field | uppercase, lowercase}}"));
    assertErrors(indexSource, AngularCode.MISSING_FORMATTER_COLON);
  }

  public void test_resolveExpression_withFilter_notSimpleIdentifier() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController("{{ctrl.field | not.supported}}"));
    assertErrors(indexSource, AngularCode.INVALID_FORMATTER_NAME);
  }

  public void test_scopeProperties() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "  String field;",
        "  MyComponent(Scope scope) {",
        "    scope.context['scopeProperty'] = 'abc';",
        "  }",
        "}",
        ""));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", createSource(//
        "    <div>",
        "      {{scopeProperty}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveIndex();
    assertNoErrors();
    // "scopeProperty" is resolved
    Element element = assertResolvedIdentifier("scopeProperty}}", "String");
    assertInstanceOf(
        AngularScopePropertyElement.class,
        AngularHtmlUnitResolver.getAngularElement(element));
  }

  public void test_scopeProperties_hideWithComponent() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent')",
        "class MyComponent {",
        "}",
        "",
        "void setScopeProperties(Scope scope) {",
        "  scope.context['ctrl'] = 1;",
        "}",
        ""));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveIndex();
    assertNoErrors();
    // "ctrl" is resolved
    LocalVariableElement element = (LocalVariableElement) assertResolvedIdentifier("ctrl}}");
    ToolkitObjectElement[] toolkitObjects = element.getToolkitObjects();
    assertInstanceOf(AngularComponentElement.class, toolkitObjects[0]);
  }

  public void test_view_resolveTemplateFile() throws Exception {
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Controller(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  String field;",
        "}",
        "",
        "class MyRouteInitializer {",
        "  init(ViewFactory view) {",
        "    view('my_template.html');",
        "  }",
        "}"));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", createSource(//
        "    <div my-controller>",
        "      {{ctrl.field}}",
        "    </div>"));
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveIndex();
    assertNoErrors();
    assertResolvedIdentifier("ctrl.", "MyController");
    assertResolvedIdentifier("field}}", "String");
  }

  private String getSourceContent(Source source) throws Exception {
    return context.getContents(source).getData().toString();
  }

  private void resolveIndexNoErrors(String content) throws Exception {
    resolveIndex(content);
    assertNoErrors();
    verify(indexSource);
  }
}
