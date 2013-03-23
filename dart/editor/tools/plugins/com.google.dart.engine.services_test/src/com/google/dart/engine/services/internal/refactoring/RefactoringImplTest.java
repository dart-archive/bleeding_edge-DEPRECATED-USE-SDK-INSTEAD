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

package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.NullProgressMonitor;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * Abstract test for testing {@link RenameRefactoring}s.
 */
public abstract class RefactoringImplTest extends AbstractDartTest {
  /**
   * @return the result of applying given {@link SourceChange} to the {@link #testCode}.
   */
  private static String getChangeResult(Source source, SourceChange change) throws Exception {
    String sourceCode = CorrectionUtils.getSourceContent(source);
    List<Edit> sourceEdits = change.getEdits();
    return CorrectionUtils.applyReplaceEdits(sourceCode, sourceEdits);
  }

  /**
   * @return the {@link SourceChange} for the given {@link Source}.
   */
  private static SourceChange getSourceChange(Change change, Source source) {
    // may be SourceChange
    if (change instanceof SourceChange) {
      SourceChange sourceChange = (SourceChange) change;
      if (sourceChange.getSource() == source) {
        return sourceChange;
      }
    }
    // may be CompositeChange
    if (change instanceof CompositeChange) {
      CompositeChange compositeChange = (CompositeChange) change;
      for (Change child : compositeChange.getChildren()) {
        SourceChange sourceChange = getSourceChange(child, source);
        if (sourceChange != null) {
          return sourceChange;
        }
      }
    }
    // not found
    return null;
  }

  protected final ProgressMonitor pm = new NullProgressMonitor();

  private Index index;

  protected SearchEngine searchEngine;

  /**
   * Assert result of applying given {@link Change} to the "source".
   */
  protected final void assertChangeResult(Change compositeChange, Source source, String expected)
      throws Exception {
    SourceChange sourceChange = getSourceChange(compositeChange, source);
    String sourceResult = getChangeResult(source, sourceChange);
    assertEquals(expected, sourceResult);
  }

  /**
   * Assert result of applying given {@link Change} to the {@link #testCode}.
   */
  protected final void assertTestChangeResult(Change compositeChange, String expected)
      throws Exception {
    assertChangeResult(compositeChange, testSource, expected);
  }

  /**
   * Parses and index given source lines.
   */
  protected final void indexTestUnit(String... lines) throws Exception {
    parseTestUnit(lines);
    AnalysisContext context = testUnit.getElement().getContext();
    index.indexUnit(context, testUnit);
  }

  /**
   * Parses and index given source code.
   */
  protected final CompilationUnit indexUnit(String path, String code) throws Exception {
    CompilationUnit unit = parseUnit(path, code);
    AnalysisContext context = unit.getElement().getContext();
    index.indexUnit(context, unit);
    return unit;
  }

  /**
   * Prints lines of result applying {@link Refactoring} to the the {@link #testSource}.
   */
  protected final void printRefactoringTestSourceResult(Refactoring refactoring) throws Exception {
    Change refactoringChange = refactoring.createChange(pm);
    SourceChange testChange = getSourceChange(refactoringChange, testSource);
    String testResult = getChangeResult(testSource, testChange);
    printSourceLines(testResult);
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
    searchEngine.searchReferences((ClassElement) null, null, null);
  }

  @Override
  protected void tearDown() throws Exception {
    index.stop();
    super.tearDown();
  }
}
