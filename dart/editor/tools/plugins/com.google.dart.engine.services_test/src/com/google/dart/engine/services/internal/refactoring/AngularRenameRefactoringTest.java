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

  public void test_dart_renameField_updateHtml() throws Exception {
    addMyController();
    resolveIndex(//
        "<html>",
        "  <body ng-app>",
        "    <div my-marker>",
        "      <button title='{{ctrl.field}}'> {{ctrl.field}} </button>",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
    indexUnit(mainUnit);
    indexUnit(indexUnit);
    // prepare refactoring
    Element field = findMainElement("field");
    prepareRenameChange(field, "newName");
    // check results
    assertIndexChangeResult(//
        "<html>",
        "  <body ng-app>",
        "    <div my-marker>",
        "      <button title='{{ctrl.newName}}'> {{ctrl.newName}} </button>",
        "    </div>",
        "    <script type='application/dart' src='main.dart'></script>",
        "  </body>",
        "</html>");
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

  private void assertIndexChangeResult(String... lines) throws Exception {
    assertChangeResult(refactoringChange, indexSource, createSource(lines));
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

  private void prepareRenameChange(Element element, String newName) throws Exception {
    refactoring = RefactoringFactory.createRenameRefactoring(searchEngine, element);
    refactoring.setNewName(newName);
    // validate status
    assertRefactoringStatusOK(refactoring);
    // prepare result
    refactoringChange = refactoring.createChange(PM);
  }
}
