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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.angular.AngularComponentElement;
import com.google.dart.engine.element.angular.AngularControllerElement;
import com.google.dart.engine.element.angular.AngularFormatterElement;
import com.google.dart.engine.element.angular.AngularHasAttributeSelectorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.element.angular.AngularScopePropertyElement;
import com.google.dart.engine.element.angular.AngularTagSelectorElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.html.angular.AngularTest;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.NullProgressMonitor;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.RefactoringFactory;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

import static com.google.dart.engine.services.internal.correction.AbstractDartTest.assertRefactoringStatus;
import static com.google.dart.engine.services.internal.correction.AbstractDartTest.assertRefactoringStatusOK;
import static com.google.dart.engine.services.internal.refactoring.RefactoringImplTest.assertChangeResult;
import static com.google.dart.engine.services.internal.refactoring.RefactoringImplTest.assertRefactoringStatusOK;

/**
 * Test for Angular related rename refactorings - for both Dart and Angular elements.
 */
public class AngularRenameRefactoringTest extends AngularTest {
  private static final ProgressMonitor PM = new NullProgressMonitor();
  private Index index;
  private SearchEngine searchEngine;
  private RenameRefactoring refactoring;
  private Change refactoringChange;

  public void test_angular_renameAttributeSelector() throws Exception {
    contextHelper.addSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.field}}",
        "    </div>"));
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Decorator(",
        "    selector: '[my-dir]',",
        "    map: const {'my-dir' : '@field'})",
        "class MyDirective {",
        "  String field;",
        "}"));
    addIndexSource(createHtmlWithAngular("<div my-dir/>"));
    contextHelper.runTasks();
    resolveMain();
    resolveIndex();
    indexUnit(indexUnit);
    // prepare refactoring
    AngularHasAttributeSelectorElement selector = findMainElement("my-dir");
    prepareRenameChange(selector, "new-name");
    // check results
    assertIndexChangeResult(createHtmlWithAngular("<div new-name/>"));
    assertMainChangeResult(mainContent.replace("my-dir", "new-name"));
  }

  public void test_angular_renameAttributeSelector_whenRenameProperty() throws Exception {
    resolveMainSource(createSource(//
        "import 'angular.dart';",
        "",
        "@Decorator(",
        "    selector: '[test]',",
        "    map: const {'test' : '@field'})",
        "class MyDirective {",
        "  set field(value) {}",
        "}"));
    resolveIndex(createHtmlWithAngular("<div test='null'/>"));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularPropertyElement property = findMainElement(ElementKind.ANGULAR_PROPERTY, "test");
    prepareRenameChange(property, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithAngular("<div newName='null'/>"));
    assertMainChangeResult(mainContent.replace("test", "newName"));
  }

  public void test_angular_renameComponentDecl() throws Exception {
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", createSource("<div> {{ctrl.field}} </div>"));
    prepareMyComponent();
    contextHelper.runTasks();
    resolveIndex();
    indexUnit(indexUnit);
    // prepare refactoring
    AngularComponentElement component = findMainElement("ctrl");
    prepareRenameChange(component, "newName");
    // check results
    assertIndexChangeResult(createSource("<div> {{newName.field}} </div>"));
    assertMainChangeResult(mainContent.replace("'ctrl',", "'newName',"));
  }

  public void test_angular_renameComponentDecl_checkNewName() throws Exception {
    addIndexSource("/my_template.html", createSource("<div> {{ctrl.field}} </div>"));
    prepareMyComponent();
    contextHelper.runTasks();
    resolveIndex();
    indexUnit(indexUnit);
    // prepare refactoring
    AngularComponentElement component = findMainElement("ctrl");
    createRenameRefactoring(component);
    // "newName"
    {
      RefactoringStatus status = refactoring.checkNewName("newName");
      assertRefactoringStatusOK(status);
    }
    // "new-name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new-name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Component name must not contain '-'.");
    }
    // "new.name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new.name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Component name must not contain '.'.");
    }
  }

  public void test_angular_renameController() throws Exception {
    prepareMyController();
    resolveIndex(createHtmlWithMyController("<div> {{test.name}} </div>"));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularControllerElement controller = findMainElement("test");
    prepareRenameChange(controller, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithMyController("<div> {{newName.name}} </div>"));
    assertMainChangeResult(mainContent.replace("'test')", "'newName')"));
  }

  public void test_angular_renameController_checkNewName() throws Exception {
    prepareMyController();
    resolveIndex(createHtmlWithMyController("<div> {{test.name}} </div>"));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularControllerElement controller = findMainElement("test");
    createRenameRefactoring(controller);
    // "newName"
    {
      RefactoringStatus status = refactoring.checkNewName("newName");
      assertRefactoringStatusOK(status);
    }
    // "new-name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new-name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Controller name must not contain '-'.");
    }
    // "new.name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new.name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Controller name must not contain '.'.");
    }
    // there is already "otherController", but that's OK
    {
      RefactoringStatus status = refactoring.checkNewName("otherController");
      assertRefactoringStatusOK(status);
    }
  }

  public void test_angular_renameFormatter() throws Exception {
    prepareMyFormatter();
    resolveIndex(createHtmlWithMyController(//
        "  <li ng-repeat=\"item in ctrl.items | test:true\">",
        "  </li>",
        ""));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularFormatterElement formatter = findMainElement("test");
    prepareRenameChange(formatter, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithMyController(//
        "  <li ng-repeat=\"item in ctrl.items | newName:true\">",
        "  </li>",
        ""));
    assertMainChangeResult(mainContent.replace("'test')", "'newName')"));
  }

  public void test_angular_renameFormatter_checkNewName() throws Exception {
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    prepareMyFormatter();
    resolveIndex(createHtmlWithMyController(//
        "  <li ng-repeat=\"item in ctrl.items | test:true\">",
        "  </li>",
        ""));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularFormatterElement formatter = findMainElement("test");
    createRenameRefactoring(formatter);
    // "newName"
    {
      RefactoringStatus status = refactoring.checkNewName("newName");
      assertRefactoringStatusOK(status);
    }
    // "new-name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new-name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Formatter name must not contain '-'.");
    }
    // "new.name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new.name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Formatter name must not contain '.'.");
    }
    // there is already "existingFormatter" formatter
    {
      RefactoringStatus status = refactoring.checkNewName("existingFormatter");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Application already defines formatter with name 'existingFormatter'.");
    }
  }

  public void test_angular_renameProperty_checkNewName() throws Exception {
    prepareMyComponent();
    resolveIndex(createHtmlWithAngular("<myComponent test='null'/>"));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularPropertyElement property = findMainElement("test");
    createRenameRefactoring(property);
    // "newName"
    {
      RefactoringStatus status = refactoring.checkNewName("newName");
      assertRefactoringStatusOK(status);
    }
    // "new-name"
    {
      RefactoringStatus status = refactoring.checkNewName("new-name");
      assertRefactoringStatusOK(status);
    }
    // "new.name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new.name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Property name must not contain '.'.");
    }
    // there is already "other" property
    {
      RefactoringStatus status = refactoring.checkNewName("other");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Component already defines property with name 'other'.");
    }
  }

  public void test_angular_renameProperty_inComponent() throws Exception {
    prepareMyComponent();
    resolveIndex(createHtmlWithAngular("<myComponent test='null'/>"));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularPropertyElement property = findMainElement("test");
    prepareRenameChange(property, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithAngular("<myComponent newName='null'/>"));
    assertMainChangeResult(mainContent.replace("'test' :", "'newName' :"));
  }

  public void test_angular_renameScopeProperty() throws Exception {
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
        "    scope.context['test'] = 'abc';",
        "  }",
        "}"));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", "<div>{{test}}</div>");
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveMain();
    resolveIndex();
    indexUnit(mainUnit);
    indexUnit(indexUnit);
    // prepare refactoring
    AngularScopePropertyElement property = findMainElement("test");
    prepareRenameChange(property, "newName");
    // check results
    assertIndexChangeResult("<div>{{newName}}</div>");
    assertMainChangeResult(mainContent.replace("'test'] =", "'newName'] ="));
  }

  public void test_angular_renameScopeProperty_checkNewName() throws Exception {
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
        "    scope.context['existingScopeProperty'] = 42;",
        "    scope.context['test'] = 'abc';",
        "  }",
        "}"));
    contextHelper.addSource("/entry-point.html", createHtmlWithAngular());
    addIndexSource("/my_template.html", "<div>{{test}}</div>");
    contextHelper.addSource("/my_styles.css", "");
    contextHelper.runTasks();
    resolveMain();
    resolveIndex();
    indexUnit(mainUnit);
    indexUnit(indexUnit);
    // prepare refactoring
    AngularScopePropertyElement property = findMainElement("test");
    createRenameRefactoring(property);
    // "newName"
    {
      RefactoringStatus status = refactoring.checkNewName("newName");
      assertRefactoringStatusOK(status);
    }
    // "new-name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new-name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Scope property name must not contain '-'.");
    }
    // "new.name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new.name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Scope property name must not contain '.'.");
    }
    // there is already "existingScopeProperty" property
    {
      RefactoringStatus status = refactoring.checkNewName("existingScopeProperty");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Component already defines scope property with name 'existingScopeProperty'.");
    }
  }

  public void test_angular_renameTagSelector() throws Exception {
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
        "    selector: 'myComponent' // selector)",
        "class MyComponent {",
        "}"));
    addIndexSource(createHtmlWithAngular("<myComponent>abcd</myComponent>"));
    contextHelper.runTasks();
    resolveMain();
    resolveIndex();
    indexUnit(indexUnit);
    // prepare refactoring
    AngularTagSelectorElement selector = findMainElement("myComponent");
    prepareRenameChange(selector, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithAngular("<newName>abcd</newName>"));
    assertMainChangeResult(mainContent.replace("'myComponent'", "'newName'"));
  }

  public void test_angular_renameTagSelector_checkNewName() throws Exception {
    contextHelper.addSource("/my_template.html", createSource(//
        "    <div>",
        "      {{ctrl.field}}",
        "    </div>"));
    addMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Component(selector: 'existingSelector')",
        "class OtherComponent {}",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent' // selector)",
        "class MyComponent {",
        "}"));
    addIndexSource(createHtmlWithAngular("<myComponent>abcd</myComponent>"));
    contextHelper.runTasks();
    resolveMain();
    resolveIndex();
    indexUnit(indexUnit);
    // prepare refactoring
    AngularTagSelectorElement selector = findMainElement("myComponent");
    createRenameRefactoring(selector);
    // "new-name"
    {
      RefactoringStatus status = refactoring.checkNewName("new-name");
      assertRefactoringStatusOK(status);
    }
    // "new name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Tag selector name must not contain ' '.");
    }
    // "new.name" - bad
    {
      RefactoringStatus status = refactoring.checkNewName("new.name");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Tag selector name must not contain '.'.");
    }
    // there is already "existingSelector" selector
    {
      RefactoringStatus status = refactoring.checkNewName("existingSelector");
      assertRefactoringStatus(
          status,
          RefactoringStatusSeverity.ERROR,
          "Application already defines component with tag selector 'existingSelector'.");
    }
  }

  public void test_dart_renameField_updateFormatterArg_orderBy() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'-name'\"/>",
        "</li>"));
    indexUnit(mainUnit);
    indexUnit(indexUnit);
    // prepare refactoring
    Element field = findMainElement("name");
    prepareRenameChange(field, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithMyController(//
        "<li ng-repeat=\"item in ctrl.items | orderBy:'-newName'\"/>",
        "</li>"));
  }

  public void test_dart_renameField_updateHtmlExpression() throws Exception {
    addMyController();
    resolveIndex(createHtmlWithMyController(//
        "<button title='{{ctrl.field}}'> {{ctrl.field}} </button>",
        ""));
    indexUnit(mainUnit);
    indexUnit(indexUnit);
    // prepare refactoring
    Element field = findMainElement("field");
    prepareRenameChange(field, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithMyController(//
        "<button title='{{ctrl.newName}}'> {{ctrl.newName}} </button>",
        ""));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // run Index
    index = IndexFactory.newIndex(IndexFactory.newSplitIndexStore(new MemoryNodeManager()));
    new Thread() {
      @Override
      public void run() {
        index.run();
      }
    }.start();
    searchEngine = SearchEngineFactory.createSearchEngine(index);
    // search for something, ensure that Index is running before we will try to stop it
    searchEngine.searchDeclarations("no-such-name", null, null);
  }

  @Override
  protected void tearDown() throws Exception {
    index.stop();
    super.tearDown();
  }

  private void assertIndexChangeResult(String expected) throws Exception {
    assertChangeResult(context, refactoringChange, indexSource, expected);
  }

  private void assertMainChangeResult(String expected) throws Exception {
    assertChangeResult(context, refactoringChange, mainSource, expected);
  }

  private void createRenameRefactoring(Element element) {
    refactoring = RefactoringFactory.createRenameRefactoring(searchEngine, element);
  }

  /**
   * Index the given {@link CompilationUnit}.
   */
  private void indexUnit(CompilationUnit unit) {
    AnalysisContext context = unit.getElement().getContext();
    index.indexUnit(context, unit);
  }

  /**
   * Index the given {@link HtmlUnit}.
   */
  private void indexUnit(HtmlUnit unit) {
    AnalysisContext context = unit.getElement().getContext();
    index.indexHtmlUnit(context, unit);
  }

  private void prepareMyComponent() throws Exception {
    resolveMainSource(createSource(//
        "import 'angular.dart';",
        "",
        "@Component(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {'test' : '=>field', 'other' : '=>field'})",
        "class MyComponent {",
        "  set field(value) {}",
        "}"));
  }

  private void prepareMyController() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Controller(",
        "    selector: '[other-controller]',",
        "    publishAs: 'otherController')",
        "class OtherController {",
        "  String name;",
        "}",
        "",
        "@Controller(",
        "    selector: '[my-controller]',",
        "    publishAs: 'test')",
        "class MyController {",
        "  String name;",
        "}"));
  }

  private void prepareMyFormatter() throws Exception {
    resolveMainSource(createSource("",//
        "import 'angular.dart';",
        "",
        "@Formatter(name: 'test')",
        "class MyFormatter {",
        "}",
        "",
        "@Formatter(name: 'existingFormatter')",
        "class ExistingFormatter {",
        "}",
        "",
        "class Item {",
        "  String name;",
        "  bool done;",
        "}",
        "",
        "@Controller(",
        "    selector: '[my-controller]',",
        "    publishAs: 'ctrl')",
        "class MyController {",
        "  List<Item> items;",
        "}"));
  }

  private void prepareRenameChange(Element element, String newName) throws Exception {
    createRenameRefactoring(element);
    refactoring.setNewName(newName);
    // validate status
    assertRefactoringStatusOK(refactoring);
    // prepare result
    refactoringChange = refactoring.createChange(PM);
  }
}
