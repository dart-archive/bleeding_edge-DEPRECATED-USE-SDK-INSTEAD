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
import com.google.dart.engine.element.angular.AngularPropertyElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.internal.html.angular.AngularTest;
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

  public void test_angular_renameProperty_inDirective() throws Exception {
    resolveMainSource(createSource(//
        "import 'angular.dart';",
        "",
        "@NgDirective(",
        "    selector: '[test]',",
        "    map: const {'test' : '@field'})",
        "class MyDirective {",
        "  set field(value) {}",
        "}",
        "",
        "main() {",
        "  var module = new Module();",
        "  module.type(MyDirective);",
        "  ngBootstrap(module: module);",
        "}"));
    resolveIndex(createHtmlWithAngular("<div test='null'/>"));
    indexUnit(indexUnit);
    // prepare refactoring
    AngularPropertyElement property = findMainElement(ElementKind.ANGULAR_PROPERTY, "test");
    prepareRenameChange(property, "newName");
    // check results
    assertIndexChangeResult(createHtmlWithAngular("<div newName='null'/>"));
    assertMainChangeResult(mainContent.replace("'test' :", "'newName' :"));
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
    index = IndexFactory.newIndex(IndexFactory.newMemoryIndexStore());
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
    assertChangeResult(refactoringChange, indexSource, expected);
  }

  private void assertMainChangeResult(String expected) throws Exception {
    assertChangeResult(refactoringChange, mainSource, expected);
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
        "@NgComponent(",
        "    templateUrl: 'my_template.html', cssUrl: 'my_styles.css',",
        "    publishAs: 'ctrl',",
        "    selector: 'myComponent', // selector",
        "    map: const {'test' : '=>field', 'other' : '=>field'})",
        "class MyComponent {",
        "  set field(value) {}",
        "}",
        "",
        "main() {",
        "  var module = new Module();",
        "  module.type(MyComponent);",
        "  ngBootstrap(module: module);",
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
