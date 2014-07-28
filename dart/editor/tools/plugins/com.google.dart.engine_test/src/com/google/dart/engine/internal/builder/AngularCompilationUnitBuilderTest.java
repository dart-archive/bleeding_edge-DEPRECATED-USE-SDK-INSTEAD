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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.ast.AstFactory;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularSelectorElement;
import com.google.dart.engine.element.angular.AngularViewElement;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.internal.element.angular.AngularHasClassSelectorElementImpl;
import com.google.dart.engine.internal.element.angular.AngularScopePropertyElementImpl;
import com.google.dart.engine.internal.element.angular.AngularTagSelectorElementImpl;
import com.google.dart.engine.internal.element.angular.HasAttributeSelectorElementImpl;
import com.google.dart.engine.internal.element.angular.IsTagHasAttributeSelectorElementImpl;
import com.google.dart.engine.internal.html.angular.AngularTest;

import static com.google.dart.engine.html.HtmlFactory.attribute;
import static com.google.dart.engine.html.HtmlFactory.tagNode;

public class AngularCompilationUnitBuilderTest extends AngularTest {
  @SuppressWarnings("unchecked")
  protected static <T extends AngularElement> T getAngularElement(Element element,
      Class<T> angularElementType) {
    ToolkitObjectElement[] toolkitObjects = null;
    if (element instanceof ClassElement) {
      ClassElement classElement = (ClassElement) element;
      toolkitObjects = classElement.getToolkitObjects();
    }
    if (element instanceof LocalVariableElement) {
      LocalVariableElement variableElement = (LocalVariableElement) element;
      toolkitObjects = variableElement.getToolkitObjects();
    }
    if (toolkitObjects != null) {
      for (ToolkitObjectElement toolkitObject : toolkitObjects) {
        if (angularElementType.isInstance(toolkitObject)) {
          return (T) toolkitObject;
        }
      }
    }
    return null;
  }

  private static void assertHasAttributeSelector(AngularSelectorElement selector, String name) {
    assertInstanceOf(HasAttributeSelectorElementImpl.class, selector);
    assertEquals(name, ((HasAttributeSelectorElementImpl) selector).getName());
  }

  private static void assertIsTagSelector(AngularSelectorElement selector, String name) {
    assertInstanceOf(AngularTagSelectorElementImpl.class, selector);
    assertEquals(name, ((AngularTagSelectorElementImpl) selector).getName());
  }

  private static String createAngularSource(String... lines) {
    String source = "import 'angular.dart';\n";
    source += createSource(lines);
    return source;
  }

  public void test_bad_notConstructorAnnotation() throws Exception {
    String mainContent = createSource(//
        "const MY_ANNOTATION = null;",
        "@MY_ANNOTATION()",
        "class MyFilter {",
        "}");
    resolveMainSource(mainContent);
    // prepare AngularFilterElement
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFormatterElement filter = getAngularElement(classElement, AngularFormatterElement.class);
    assertNull(filter);
  }

  public void test_Decorator() throws Exception {
    String mainContent = createAngularSource(//
        "@Decorator(selector: '[my-dir]',",
        "             map: const {",
        "               'my-dir' : '=>myPropA',",
        "               '.' : '&myPropB',",
        "             })",
        "class MyDirective {",
        "  set myPropA(value) {}",
        "  set myPropB(value) {}",
        "  @NgTwoWay('my-prop-c')",
        "  String myPropC;",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularDirectiveElement
    ClassElement classElement = mainUnitElement.getType("MyDirective");
    AngularDecoratorElement directive = getAngularElement(
        classElement,
        AngularDecoratorElement.class);
    assertNotNull(directive);
    // verify
    assertEquals(null, directive.getName());
    assertEquals(-1, directive.getNameOffset());
    assertHasAttributeSelector(directive.getSelector(), "my-dir");
    // verify properties
    AngularPropertyElement[] properties = directive.getProperties();
    assertLength(3, properties);
    assertProperty(
        properties[0],
        "my-dir",
        findMainOffset("my-dir' :"),
        AngularPropertyKind.ONE_WAY,
        "myPropA",
        findMainOffset("myPropA'"));
    assertProperty(
        properties[1],
        ".",
        findMainOffset(".' :"),
        AngularPropertyKind.CALLBACK,
        "myPropB",
        findMainOffset("myPropB'"));
    assertProperty(
        properties[2],
        "my-prop-c",
        findMainOffset("my-prop-c'"),
        AngularPropertyKind.TWO_WAY,
        "myPropC",
        -1);
  }

  public void test_Decorator_bad_cannotParseSelector() throws Exception {
    String mainContent = createAngularSource(//
        "@Decorator(selector: '~bad-selector',",
        "             map: const {",
        "               'my-dir' : '=>myPropA',",
        "               '.' : '&myPropB',",
        "             })",
        "class MyDirective {",
        "  set myPropA(value) {}",
        "  set myPropB(value) {}",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.CANNOT_PARSE_SELECTOR);
  }

  public void test_Decorator_bad_missingSelector() throws Exception {
    String mainContent = createAngularSource(//
        "@Decorator(/*selector: '[my-dir]',*/",
        "             map: const {",
        "               'my-dir' : '=>myPropA',",
        "               '.' : '&myPropB',",
        "             })",
        "class MyDirective {",
        "  set myPropA(value) {}",
        "  set myPropB(value) {}",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_SELECTOR);
  }

  public void test_Formatter() throws Exception {
    String mainContent = createAngularSource(//
        "@Formatter(name: 'myFilter')",
        "class MyFilter {",
        "  call(p1, p2) {}",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularFilterElement
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFormatterElement filter = getAngularElement(classElement, AngularFormatterElement.class);
    assertNotNull(filter);
    // verify
    assertEquals("myFilter", filter.getName());
    assertEquals(findOffset(mainContent, "myFilter'"), filter.getNameOffset());
  }

  public void test_Formatter_missingName() throws Exception {
    String mainContent = createAngularSource(//
        "@Formatter()",
        "class MyFilter {",
        "  call(p1, p2) {}",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_NAME);
    // no filter
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFormatterElement filter = getAngularElement(classElement, AngularFormatterElement.class);
    assertNull(filter);
  }

  public void test_getElement_component_name() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {}"));
    SimpleStringLiteral node = findMainNode("ctrl'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // find AngularComponentElement
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertInstanceOf(AngularComponentElement.class, element);
  }

  public void test_getElement_component_property_fromFieldAnnotation() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "  @NgOneWay('prop')",
        "  var field;",
        "}"));
    // prepare node
    SimpleStringLiteral node = findMainNode("prop'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // prepare Element
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertNotNull(element);
    // check AngularPropertyElement
    AngularPropertyElement property = (AngularPropertyElement) element;
    assertEquals("prop", property.getName());
  }

  public void test_getElement_component_property_fromMap() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {",
        "               'prop' : '@field',",
        "             })",
        "class MyComponent {",
        "  var field;",
        "}"));
    // AngularPropertyElement
    {
      SimpleStringLiteral node = findMainNode("prop'", SimpleStringLiteral.class);
      int offset = node.getOffset();
      // prepare Element
      Element element = AngularCompilationUnitBuilder.getElement(node, offset);
      assertNotNull(element);
      // check AngularPropertyElement
      AngularPropertyElement property = (AngularPropertyElement) element;
      assertEquals("prop", property.getName());
    }
    // FieldElement
    {
      SimpleStringLiteral node = findMainNode("@field'", SimpleStringLiteral.class);
      int offset = node.getOffset();
      // prepare Element
      Element element = AngularCompilationUnitBuilder.getElement(node, offset);
      assertNotNull(element);
      // check FieldElement
      FieldElement field = (FieldElement) element;
      assertEquals("field", field.getName());
    }
  }

  public void test_getElement_component_selector() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {}"));
    SimpleStringLiteral node = findMainNode("myComp'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // find AngularSelectorElement
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertInstanceOf(AngularSelectorElement.class, element);
  }

  public void test_getElement_controller_name() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Controller(publishAs: 'ctrl', selector: '[myApp]')",
        "class MyController {",
        "}"));
    SimpleStringLiteral node = findMainNode("ctrl'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // find AngularControllerElement
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertInstanceOf(AngularControllerElement.class, element);
  }

  public void test_getElement_directive_property() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Decorator(selector: '[my-dir]',",
        "             map: const {",
        "               'my-dir' : '=>field'",
        "             })",
        "class MyDirective {",
        "  set field(value) {}",
        "}"));
    // prepare node
    SimpleStringLiteral node = findMainNode("my-dir'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // prepare Element
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertNotNull(element);
    // check AngularPropertyElement
    AngularPropertyElement property = (AngularPropertyElement) element;
    assertEquals("my-dir", property.getName());
  }

  public void test_getElement_directive_selector() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Decorator(selector: '[my-dir]')",
        "class MyDirective {}"));
    SimpleStringLiteral node = findMainNode("my-dir]'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // find AngularSelectorElement
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertInstanceOf(AngularSelectorElement.class, element);
  }

  public void test_getElement_filter_name() throws Exception {
    resolveMainSource(createAngularSource(//
        "@Formatter(name: 'myFilter')",
        "class MyFilter {",
        "  call(p1, p2) {}",
        "}"));
    SimpleStringLiteral node = findMainNode("myFilter'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // find FilterElement
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertInstanceOf(AngularFormatterElement.class, element);
  }

  public void test_getElement_noClassDeclaration() throws Exception {
    resolveMainSource("var foo = 'bar';");
    SimpleStringLiteral node = findMainNode("bar'", SimpleStringLiteral.class);
    Element element = AngularCompilationUnitBuilder.getElement(node, 0);
    assertNull(element);
  }

  public void test_getElement_noClassElement() throws Exception {
    resolveMainSource(createSource(//
        "class A {",
        "  const A(p);",
        "}",
        "",
        "@A('bar')",
        "class B {}"));
    SimpleStringLiteral node = findMainNode("bar'", SimpleStringLiteral.class);
    // reset B element
    node.getAncestor(ClassDeclaration.class).getName().setStaticElement(null);
    // class is not resolved - no element
    Element element = AngularCompilationUnitBuilder.getElement(node, 0);
    assertNull(element);
  }

  public void test_getElement_noNode() throws Exception {
    Element element = AngularCompilationUnitBuilder.getElement(null, 0);
    assertNull(element);
  }

  public void test_getElement_notFound() throws Exception {
    resolveMainSource(createSource(//
        "class MyComponent {",
        "  var str = 'some string';",
        "}"));
    // prepare node
    SimpleStringLiteral node = findMainNode("some string'", SimpleStringLiteral.class);
    int offset = node.getOffset();
    // no Element
    Element element = AngularCompilationUnitBuilder.getElement(node, offset);
    assertNull(element);
  }

  public void test_getElement_SimpleStringLiteral_withToolkitElement() throws Exception {
    SimpleStringLiteral literal = AstFactory.string("foo");
    Element element = new AngularScopePropertyElementImpl("foo", 0, null);
    literal.setToolkitElement(element);
    assertSame(element, AngularCompilationUnitBuilder.getElement(literal, -1));
  }

  public void test_NgComponent_bad_cannotParseSelector() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: '~myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.CANNOT_PARSE_SELECTOR);
  }

  public void test_NgComponent_bad_missingSelector() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', /*selector: 'myComp',*/",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_SELECTOR);
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=16346
   */
  public void test_NgComponent_bad_notHtmlTemplate() throws Exception {
    contextHelper.addSource("/my_template", "");
    contextHelper.addSource("/my_styles.css", "");
    addMainSource(createAngularSource(//
        "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "}"));
    contextHelper.runTasks();
    // no exceptions, even though "my_template" is not an HTML file
  }

  public void test_NgComponent_bad_properties_invalidBinding() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {'name' : '?field'})",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_KIND);
  }

  public void test_NgComponent_bad_properties_nameNotStringLiteral() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {null : 'field'})",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_NAME);
  }

  public void test_NgComponent_bad_properties_noSuchField() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {'name' : '=>field'})",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_FIELD);
  }

  public void test_NgComponent_bad_properties_notMapLiteral() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: null)",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_MAP);
  }

  public void test_NgComponent_bad_properties_specNotStringLiteral() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {'name' : null})",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_SPEC);
  }

  public void test_NgComponent_no_cssUrl() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html'/*, cssUrl: 'my_styles.css'*/)",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // no CSS
    assertEquals(null, component.getStyleUri());
    assertEquals(-1, component.getStyleUriOffset());
  }

  public void test_NgComponent_no_publishAs() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(/*publishAs: 'ctrl',*/ selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // no name
    assertEquals(null, component.getName());
    assertEquals(-1, component.getNameOffset());
  }

  public void test_NgComponent_no_templateUrl() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             /*templateUrl: 'my_template.html',*/ cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // no template
    assertEquals(null, component.getTemplateUri());
    assertEquals(null, component.getTemplateSource());
    assertEquals(-1, component.getTemplateUriOffset());
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=19023
   */
  public void test_NgComponent_notAngular() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createSource(//
        "class Component {",
        "  const Component(a, b);",
        "}",
        "",
        "@Component('foo', 42)",
        "class MyComponent {",
        "}");
    resolveMainSource(mainContent);
    assertNoMainErrors();
  }

  public void test_NgComponent_properties_fieldFromSuper() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    resolveMainSourceNoErrors(createAngularSource(//
        "class MySuper {",
        "  var myPropA;",
        "}",
        "",
        "",
        "",
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {",
        "               'prop-a' : '@myPropA'",
        "             })",
        "class MyComponent extends MySuper {",
        "}"));
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // verify
    AngularPropertyElement[] properties = component.getProperties();
    assertLength(1, properties);
    assertProperty(
        properties[0],
        "prop-a",
        findMainOffset("prop-a' :"),
        AngularPropertyKind.ATTR,
        "myPropA",
        findMainOffset("myPropA'"));
  }

  public void test_NgComponent_properties_fromFields() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    resolveMainSourceNoErrors(createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "  @NgAttr('prop-a')",
        "  var myPropA;",
        "  @NgCallback('prop-b')",
        "  var myPropB;",
        "  @NgOneWay('prop-c')",
        "  var myPropC;",
        "  @NgOneWayOneTime('prop-d')",
        "  var myPropD;",
        "  @NgTwoWay('prop-e')",
        "  var myPropE;",
        "}"));
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // verify
    AngularPropertyElement[] properties = component.getProperties();
    assertLength(5, properties);
    assertProperty(
        properties[0],
        "prop-a",
        findMainOffset("prop-a')"),
        AngularPropertyKind.ATTR,
        "myPropA",
        -1);
    assertProperty(
        properties[1],
        "prop-b",
        findMainOffset("prop-b')"),
        AngularPropertyKind.CALLBACK,
        "myPropB",
        -1);
    assertProperty(
        properties[2],
        "prop-c",
        findMainOffset("prop-c')"),
        AngularPropertyKind.ONE_WAY,
        "myPropC",
        -1);
    assertProperty(
        properties[3],
        "prop-d",
        findMainOffset("prop-d')"),
        AngularPropertyKind.ONE_WAY_ONE_TIME,
        "myPropD",
        -1);
    assertProperty(
        properties[4],
        "prop-e",
        findMainOffset("prop-e')"),
        AngularPropertyKind.TWO_WAY,
        "myPropE",
        -1);
  }

  public void test_NgComponent_properties_fromMap() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    resolveMainSourceNoErrors(createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "             map: const {",
        "               'prop-a' : '@myPropA',",
        "               'prop-b' : '&myPropB',",
        "               'prop-c' : '=>myPropC',",
        "               'prop-d' : '=>!myPropD',",
        "               'prop-e' : '<=>myPropE'",
        "             })",
        "class MyComponent {",
        "  var myPropA;",
        "  var myPropB;",
        "  var myPropC;",
        "  var myPropD;",
        "  var myPropE;",
        "}"));
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // verify
    AngularPropertyElement[] properties = component.getProperties();
    assertLength(5, properties);
    assertProperty(
        properties[0],
        "prop-a",
        findMainOffset("prop-a' :"),
        AngularPropertyKind.ATTR,
        "myPropA",
        findMainOffset("myPropA'"));
    assertProperty(
        properties[1],
        "prop-b",
        findMainOffset("prop-b' :"),
        AngularPropertyKind.CALLBACK,
        "myPropB",
        findMainOffset("myPropB'"));
    assertProperty(
        properties[2],
        "prop-c",
        findMainOffset("prop-c' :"),
        AngularPropertyKind.ONE_WAY,
        "myPropC",
        findMainOffset("myPropC'"));
    assertProperty(
        properties[3],
        "prop-d",
        findMainOffset("prop-d' :"),
        AngularPropertyKind.ONE_WAY_ONE_TIME,
        "myPropD",
        findMainOffset("myPropD'"));
    assertProperty(
        properties[4],
        "prop-e",
        findMainOffset("prop-e' :"),
        AngularPropertyKind.TWO_WAY,
        "myPropE",
        findMainOffset("myPropE'"));
  }

  public void test_NgComponent_properties_no() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // verify
    assertEquals("ctrl", component.getName());
    assertEquals(findOffset(mainContent, "ctrl'"), component.getNameOffset());
    assertIsTagSelector(component.getSelector(), "myComp");
    assertEquals("my_template.html", component.getTemplateUri());
    assertEquals(findOffset(mainContent, "my_template.html'"), component.getTemplateUriOffset());
    assertEquals("my_styles.css", component.getStyleUri());
    assertEquals(findOffset(mainContent, "my_styles.css'"), component.getStyleUriOffset());
    assertLength(0, component.getProperties());
  }

  public void test_NgComponent_scopeProperties() throws Exception {
    contextHelper.addSource("/my_template.html", "");
    contextHelper.addSource("/my_styles.css", "");
    String mainContent = createAngularSource(//
        "@Component(publishAs: 'ctrl', selector: 'myComp',",
        "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
        "class MyComponent {",
        "  MyComponent(Scope scope) {",
        "    scope.context['boolProp'] = true;",
        "    scope.context['intProp'] = 42;",
        "    scope.context['stringProp'] = 'foo';",
        "    // duplicate is ignored",
        "    scope.context['boolProp'] = true;",
        "    // LHS is not an IndexExpression",
        "    var v1;",
        "    v1 = 1;",
        "    // LHS is not a Scope access",
        "    var v2;",
        "    v2['name'] = 2;",
        "  }",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularComponentElement
    ClassElement classElement = mainUnitElement.getType("MyComponent");
    AngularComponentElement component = getAngularElement(
        classElement,
        AngularComponentElement.class);
    assertNotNull(component);
    // verify
    AngularScopePropertyElement[] scopeProperties = component.getScopeProperties();
    assertLength(3, scopeProperties);
    {
      AngularScopePropertyElement property = scopeProperties[0];
      assertSame(property, findMainElement("boolProp"));
      assertEquals("boolProp", property.getName());
      assertEquals(findOffset(mainContent, "boolProp'"), property.getNameOffset());
      assertEquals("bool", property.getType().getName());
    }
    {
      AngularScopePropertyElement property = scopeProperties[1];
      assertSame(property, findMainElement("intProp"));
      assertEquals("intProp", property.getName());
      assertEquals(findOffset(mainContent, "intProp'"), property.getNameOffset());
      assertEquals("int", property.getType().getName());
    }
    {
      AngularScopePropertyElement property = scopeProperties[2];
      assertSame(property, findMainElement("stringProp"));
      assertEquals("stringProp", property.getName());
      assertEquals(findOffset(mainContent, "stringProp'"), property.getNameOffset());
      assertEquals("String", property.getType().getName());
    }
  }

  public void test_NgController() throws Exception {
    String mainContent = createAngularSource(//
        "@Controller(publishAs: 'ctrl', selector: '[myApp]')",
        "class MyController {",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularControllerElement
    ClassElement classElement = mainUnitElement.getType("MyController");
    AngularControllerElement controller = getAngularElement(
        classElement,
        AngularControllerElement.class);
    assertNotNull(controller);
    // verify
    assertEquals("ctrl", controller.getName());
    assertEquals(findOffset(mainContent, "ctrl'"), controller.getNameOffset());
    assertHasAttributeSelector(controller.getSelector(), "myApp");
  }

  public void test_NgController_cannotParseSelector() throws Exception {
    String mainContent = createAngularSource(//
        "@Controller(publishAs: 'ctrl', selector: '~unknown')",
        "class MyController {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.CANNOT_PARSE_SELECTOR);
  }

  public void test_NgController_missingPublishAs() throws Exception {
    String mainContent = createAngularSource(//
        "@Controller(selector: '[myApp]')",
        "class MyController {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_PUBLISH_AS);
  }

  public void test_NgController_missingSelector() throws Exception {
    String mainContent = createAngularSource(//
        "@Controller(publishAs: 'ctrl')",
        "class MyController {",
        "}");
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_SELECTOR);
  }

  public void test_NgController_noAnnotationArguments() throws Exception {
    String mainContent = createAngularSource(//
        "@NgController",
        "class MyController {",
        "}");
    resolveMainSource(mainContent);
    // ignore errors, but there should be no exceptions
  }

  public void test_parseSelector_hasAttribute() throws Exception {
    AngularSelectorElement selector = AngularCompilationUnitBuilder.parseSelector(42, "[name]");
    assertHasAttributeSelector(selector, "name");
    assertEquals(42 + 1, selector.getNameOffset());
  }

  public void test_parseSelector_hasClass() throws Exception {
    AngularSelectorElement selector = AngularCompilationUnitBuilder.parseSelector(42, ".my-class");
    AngularHasClassSelectorElementImpl classSelector = (AngularHasClassSelectorElementImpl) selector;
    assertEquals("my-class", classSelector.getName());
    assertEquals(".my-class", classSelector.toString());
    assertEquals(42 + 1, selector.getNameOffset());
    // test apply()
    {
      XmlTagNode node = tagNode("div", attribute("class", "one two"));
      assertFalse(classSelector.apply(node));
    }
    {
      XmlTagNode node = tagNode("div", attribute("class", "one my-class two"));
      assertTrue(classSelector.apply(node));
    }
  }

  public void test_parseSelector_isTag() throws Exception {
    AngularSelectorElement selector = AngularCompilationUnitBuilder.parseSelector(42, "name");
    assertIsTagSelector(selector, "name");
    assertEquals(42, selector.getNameOffset());
  }

  public void test_parseSelector_isTag_hasAttribute() throws Exception {
    AngularSelectorElement selector = AngularCompilationUnitBuilder.parseSelector(42, "tag[attr]");
    assertInstanceOf(IsTagHasAttributeSelectorElementImpl.class, selector);
    assertEquals("tag[attr]", selector.getName());
    assertEquals(-1, selector.getNameOffset());
    assertEquals("tag", ((IsTagHasAttributeSelectorElementImpl) selector).getTagName());
    assertEquals("attr", ((IsTagHasAttributeSelectorElementImpl) selector).getAttributeName());
  }

  public void test_parseSelector_unknown() throws Exception {
    AngularSelectorElement selector = AngularCompilationUnitBuilder.parseSelector(0, "~unknown");
    assertNull(selector);
  }

  public void test_view() throws Exception {
    contextHelper.addSource("/wrong.html", "");
    contextHelper.addSource("/my_templateA.html", "");
    contextHelper.addSource("/my_templateB.html", "");
    String mainContent = createAngularSource(//
        "class MyRouteInitializer {",
        "  init(ViewFactory view, foo) {",
        "    foo.view('wrong.html');   // has target",
        "    foo();                    // less than one argument",
        "    foo('wrong.html', 'bar'); // more than one argument",
        "    foo('wrong' + '.html');   // not literal",
        "    foo('wrong.html');        // not ViewFactory",
        "    view('my_templateA.html');",
        "    view('my_templateB.html');",
        "  }",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularViewElement(s)
    AngularViewElement[] views = mainUnitElement.getAngularViews();
    assertLength(2, views);
    {
      AngularViewElement view = views[0];
      assertEquals("my_templateA.html", view.getTemplateUri());
      assertEquals(null, view.getName());
      assertEquals(-1, view.getNameOffset());
      assertEquals(findOffset(mainContent, "my_templateA.html'"), view.getTemplateUriOffset());
    }
    {
      AngularViewElement view = views[1];
      assertEquals("my_templateB.html", view.getTemplateUri());
      assertEquals(null, view.getName());
      assertEquals(-1, view.getNameOffset());
      assertEquals(findOffset(mainContent, "my_templateB.html'"), view.getTemplateUriOffset());
    }
  }

  private void assertProperty(AngularPropertyElement property, String expectedName,
      int expectedNameOffset, AngularPropertyKind expectedKind, String expectedFieldName,
      int expectedFieldOffset) {
    assertEquals(expectedName, property.getName());
    assertEquals(expectedNameOffset, property.getNameOffset());
    assertSame(expectedKind, property.getPropertyKind());
    assertEquals(expectedFieldName, property.getField().getName());
    assertEquals(expectedFieldOffset, property.getFieldNameOffset());
  }

  /**
   * Find {@link AstNode} of the given type in {@link #mainUnit}.
   */
  private <T extends AstNode> T findMainNode(String search, Class<T> clazz) {
    return findNode(mainUnit, mainContent, search, clazz);
  }
}
