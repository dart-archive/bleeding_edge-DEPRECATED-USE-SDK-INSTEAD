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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementFactory;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ToolkitObjectElement;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularDirectiveElement;
import com.google.dart.engine.element.angular.AngularElement;
import com.google.dart.engine.element.angular.AngularFilterElement;
import com.google.dart.engine.element.angular.AngularModuleElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularPropertyKind;
import com.google.dart.engine.element.angular.AngularSelector;
import com.google.dart.engine.error.AngularCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.angular.HasAttributeSelector;
import com.google.dart.engine.internal.element.angular.IsTagSelector;
import com.google.dart.engine.internal.html.angular.AngularTest;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

public class AngularCompilationUnitBuilderTest extends AngularTest {
  private static void assertHasAttributeSelector(AngularSelector selector, String name) {
    assertInstanceOf(HasAttributeSelector.class, selector);
    assertEquals(name, ((HasAttributeSelector) selector).getAttributeName());
  }

  private static void assertIsTagSelector(AngularSelector selector, String name) {
    assertInstanceOf(IsTagSelector.class, selector);
    assertEquals(name, ((IsTagSelector) selector).getName());
  }

  private static String createAngularModuleSource(String[] lines, String[] types) {
    String source = "import 'angular.dart';\n";
    source += createSource(lines);
    source += "\n";
    source += createSource(//
        "class MyModule extends Module {",
        "  MyModule() {");
    for (String type : types) {
      source += "    type(" + type + ");";
    }
    source += createSource(//
        "  }",
        "}",
        "",
        "main() {",
        "  ngBootstrap(module: new MyModule());",
        "}");
    return source;
  }

  /**
   * Function to force formatter to put every string on separate line.
   */
  private static String[] formatLines(String... lines) {
    return lines;
  }

  public void test_bad_notConstructorAnnotation() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "const MY_ANNOTATION = null;",
            "@MY_ANNOTATION",
            "class MyFilter {",
            "}"),
        formatLines("MyFilter"));
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularFilterElement
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFilterElement filter = getAngularElement(classElement, AngularFilterElement.class);
    assertNull(filter);
  }

  public void test_isModule_Module() throws Exception {
    InterfaceType moduleType = ElementFactory.classElement("Module").getType();
    assertTrue(AngularCompilationUnitBuilder.isModule(moduleType));
  }

  public void test_isModule_Module_subtype() throws Exception {
    InterfaceType moduleType = ElementFactory.classElement("Module").getType();
    ClassElementImpl myModuleElement = ElementFactory.classElement("MyModule", moduleType);
    InterfaceType myModuleType = myModuleElement.getType();
    assertTrue(AngularCompilationUnitBuilder.isModule(myModuleType));
  }

  public void test_isModule_notInterfaceType() throws Exception {
    Type type = DynamicTypeImpl.getInstance();
    assertFalse(AngularCompilationUnitBuilder.isModule(type));
  }

  public void test_isModule_null() throws Exception {
    assertFalse(AngularCompilationUnitBuilder.isModule(null));
  }

  public void test_isModule_recursion() throws Exception {
    ClassElementImpl myModuleElement = ElementFactory.classElement("MyModule");
    InterfaceType myModuleType = myModuleElement.getType();
    myModuleElement.setSupertype(myModuleType);
    assertFalse(AngularCompilationUnitBuilder.isModule(myModuleType));
  }

  public void test_module_asClass() throws Exception {
    String mainContent = createSource(//
        "import 'angular.dart';",
        "class ChildModuleA extends Module {}",
        "class ChildModuleB extends Module {}",
        "class MyModule extends Module {",
        "  MyModule() {",
        "    install(new ChildModuleA());",
        "    install(new ChildModuleB());",
        "    type(String);",
        "    value(int, 42);",
        "  }",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare MyModule
    ClassElement classElement = mainUnitElement.getType("MyModule");
    AngularModuleElement module = getAngularElement(classElement, AngularModuleElement.class);
    assertNotNull(module);
    // check child modules
    AngularModuleElement[] childModules = module.getChildModules();
    assertLength(2, childModules);
    assertEquals("ChildModuleA", childModules[0].getEnclosingElement().getName());
    assertEquals("ChildModuleB", childModules[1].getEnclosingElement().getName());
    // check key types
    ClassElement[] keyTypes = module.getKeyTypes();
    assertLength(2, keyTypes);
    assertEquals("String", keyTypes[0].getName());
    assertEquals("int", keyTypes[1].getName());
  }

  public void test_module_asClass_notInterestingInvocation() throws Exception {
    String mainContent = createSource(//
        "import 'angular.dart';",
        "class MyModule extends Module {",
        "  MyModule() {",
        "    foo();",
        "  }",
        "  foo() {}",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare MyModule
    ClassElement variable = mainUnitElement.getType("MyModule");
    AngularModuleElement module = getAngularElement(variable, AngularModuleElement.class);
    assertNotNull(module);
    // no properties
    assertLength(0, module.getChildModules());
    assertLength(0, module.getKeyTypes());
  }

  public void test_module_asLocalVariable_cascade() throws Exception {
    String mainContent = createSource(//
        "import 'angular.dart';",
        "class ChildModuleA extends Module {}",
        "class ChildModuleB extends Module {}",
        "main() {",
        "  var module = new Module()",
        "    ..install(new ChildModuleA())",
        "    ..install(new ChildModuleB())",
        "    ..type(String)",
        "    ..value(int, 42);",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare "module"
    LocalVariableElement variable = (LocalVariableElement) findMainElement("module");
    AngularModuleElement module = getAngularElement(variable, AngularModuleElement.class);
    assertNotNull(module);
    // check child modules
    AngularModuleElement[] childModules = module.getChildModules();
    assertLength(2, childModules);
    assertEquals("ChildModuleA", childModules[0].getEnclosingElement().getName());
    assertEquals("ChildModuleB", childModules[1].getEnclosingElement().getName());
    // check key types
    ClassElement[] keyTypes = module.getKeyTypes();
    assertLength(2, keyTypes);
    assertEquals("String", keyTypes[0].getName());
    assertEquals("int", keyTypes[1].getName());
  }

  public void test_module_asLocalVariable_otherInvocation() throws Exception {
    String mainContent = createSource(//
        "import 'angular.dart';",
        "main() {",
        "  var module = new Module();",
        "  module.type(String);",
        "  123.abs();",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // don't verify "module", just check the fact that there were no resolution problems
  }

  public void test_module_asLocalVariable_statements() throws Exception {
    String mainContent = createSource(//
        "import 'angular.dart';",
        "class ChildModuleA extends Module {}",
        "class ChildModuleB extends Module {}",
        "main() {",
        "  var module = new Module();",
        "  module.install(new ChildModuleA());",
        "  module.install(new ChildModuleB());",
        "  module.type(String);",
        "  module.value(int, 42);",
        "}");
    resolveMainSourceNoErrors(mainContent);
    // prepare "module"
    LocalVariableElement classElement = (LocalVariableElement) findMainElement("module");
    AngularModuleElement module = getAngularElement(classElement, AngularModuleElement.class);
    assertNotNull(module);
    // check child modules
    AngularModuleElement[] childModules = module.getChildModules();
    assertLength(2, childModules);
    assertEquals("ChildModuleA", childModules[0].getEnclosingElement().getName());
    assertEquals("ChildModuleB", childModules[1].getEnclosingElement().getName());
    // check key types
    ClassElement[] keyTypes = module.getKeyTypes();
    assertLength(2, keyTypes);
    assertEquals("String", keyTypes[0].getName());
    assertEquals("int", keyTypes[1].getName());
  }

  public void test_NgComponent_bad_cannotParseSelector() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: '~myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.CANNOT_PARSE_SELECTOR);
  }

  public void test_NgComponent_bad_missingCss() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html'/*, cssUrl: 'my_styles.css'*/)",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_CSS_URL);
  }

  public void test_NgComponent_bad_missingPublishAs() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(/*publishAs: 'ctrl',*/ selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_PUBLISH_AS);
  }

  public void test_NgComponent_bad_missingSelector() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', /*selector: 'myComp',*/",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_SELECTOR);
  }

  public void test_NgComponent_bad_missingTemplate() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             /*templateUrl: 'my_template.html',*/ cssUrl: 'my_styles.css')",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_TEMPLATE_URL);
  }

  public void test_NgComponent_bad_properties_invalidBinding() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
            "             map: const {'name' : '?field'})",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_KIND);
  }

  public void test_NgComponent_bad_properties_nameNotStringLiteral() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
            "             map: const {null : 'field'})",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_NAME);
  }

  public void test_NgComponent_bad_properties_noSuchField() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
            "             map: const {'name' : '=>field'})",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_FIELD);
  }

  public void test_NgComponent_bad_properties_notMapLiteral() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
            "             map: null)",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_MAP);
  }

  public void test_NgComponent_bad_properties_specNotStringLiteral() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
            "             map: const {'name' : null})",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.INVALID_PROPERTY_SPEC);
  }

  public void test_NgComponent_properties_fromFields() throws Exception {
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    resolveMainSourceNoErrors(createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
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
            "}"),
        formatLines("MyComponent")));
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
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    resolveMainSourceNoErrors(createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
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
            "}"),
        formatLines("MyComponent")));
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
    contextHelper.addSource("my_template.html", "");
    contextHelper.addSource("my_styles.css", "");
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgComponent(publishAs: 'ctrl', selector: 'myComp',",
            "             templateUrl: 'my_template.html', cssUrl: 'my_styles.css')",
            "class MyComponent {",
            "}"),
        formatLines("MyComponent"));
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

  public void test_NgController() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgController(publishAs: 'ctrl', selector: '[myApp]')",
            "class MyController {",
            "}"),
        formatLines("MyController"));
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
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgController(publishAs: 'ctrl', selector: '~unknown')",
            "class MyController {",
            "}"),
        formatLines("MyController"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.CANNOT_PARSE_SELECTOR);
  }

  public void test_NgController_missingPublishAs() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgController(selector: '[myApp]')",
            "class MyController {",
            "}"),
        formatLines("MyController"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_PUBLISH_AS);
  }

  public void test_NgController_missingSelector() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgController(publishAs: 'ctrl')",
            "class MyController {",
            "}"),
        formatLines("MyController"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_SELECTOR);
  }

  public void test_NgDirective() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgDirective(selector: '[my-dir]',",
            "             map: const {",
            "               'my-dir' : '=>myPropA',",
            "               '.' : '&myPropB',",
            "             })",
            "class MyDirective {",
            "  set myPropA(value) {}",
            "  set myPropB(value) {}",
            "}"),
        formatLines("MyDirective"));
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularDirectiveElement
    ClassElement classElement = mainUnitElement.getType("MyDirective");
    AngularDirectiveElement directive = getAngularElement(
        classElement,
        AngularDirectiveElement.class);
    assertNotNull(directive);
    // verify
    assertEquals(null, directive.getName());
    assertEquals(-1, directive.getNameOffset());
    assertHasAttributeSelector(directive.getSelector(), "my-dir");
    // verify properties
    AngularPropertyElement[] properties = directive.getProperties();
    assertLength(2, properties);
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
  }

  public void test_NgDirective_bad_cannotParseSelector() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgDirective(selector: '~bad-selector',",
            "             map: const {",
            "               'my-dir' : '=>myPropA',",
            "               '.' : '&myPropB',",
            "             })",
            "class MyDirective {",
            "  set myPropA(value) {}",
            "  set myPropB(value) {}",
            "}"),
        formatLines("MyDirective"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.CANNOT_PARSE_SELECTOR);
  }

  public void test_NgDirective_bad_missingSelector() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgDirective(/*selector: '[my-dir]',*/",
            "             map: const {",
            "               'my-dir' : '=>myPropA',",
            "               '.' : '&myPropB',",
            "             })",
            "class MyDirective {",
            "  set myPropA(value) {}",
            "  set myPropB(value) {}",
            "}"),
        formatLines("MyDirective"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_SELECTOR);
  }

  public void test_NgFilter() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgFilter(name: 'myFilter')",
            "class MyFilter {",
            "  call(p1, p2) {}",
            "}"),
        formatLines("MyFilter"));
    resolveMainSourceNoErrors(mainContent);
    // prepare AngularFilterElement
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFilterElement filter = getAngularElement(classElement, AngularFilterElement.class);
    assertNotNull(filter);
    // verify
    assertEquals("myFilter", filter.getName());
    assertEquals(findOffset(mainContent, "myFilter'"), filter.getNameOffset());
  }

  public void test_NgFilter_missingName() throws Exception {
    String mainContent = createAngularModuleSource(//
        formatLines(//
            "@NgFilter()",
            "class MyFilter {",
            "  call(p1, p2) {}",
            "}"),
        formatLines("MyFilter"));
    resolveMainSource(mainContent);
    // has error
    assertMainErrors(AngularCode.MISSING_NAME);
    // no filter
    ClassElement classElement = mainUnitElement.getType("MyFilter");
    AngularFilterElement filter = getAngularElement(classElement, AngularFilterElement.class);
    assertNull(filter);
  }

  public void test_parseSelector_hasAttribute() throws Exception {
    AngularSelector selector = AngularCompilationUnitBuilder.parseSelector("[name]");
    assertHasAttributeSelector(selector, "name");
  }

  public void test_parseSelector_unknown() throws Exception {
    AngularSelector selector = AngularCompilationUnitBuilder.parseSelector("~unknown");
    assertNull(selector);
  }

  private void assertMainErrors(ErrorCode... expectedErrorCodes) throws AnalysisException {
    assertErrors(mainSource, expectedErrorCodes);
  }

  private void assertNoErrors(Source source) throws AnalysisException {
    assertErrors(source);
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

  private int findMainOffset(String search) {
    return findOffset(mainContent, search);
  }

  @SuppressWarnings("unchecked")
  private <T extends AngularElement> T getAngularElement(Element element,
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

  private void resolveMainSource(String content) throws Exception {
    mainContent = content;
    mainSource = contextHelper.addSource("/main.dart", content);
    CompilationUnit unit = contextHelper.resolveDefiningUnit(mainSource);
    mainUnitElement = unit.getElement();
  }

  private void resolveMainSourceNoErrors(String content) throws Exception {
    resolveMainSource(content);
    assertNoErrors(mainSource);
  }
}
