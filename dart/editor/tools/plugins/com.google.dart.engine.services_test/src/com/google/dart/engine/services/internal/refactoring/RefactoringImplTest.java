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

import com.google.common.base.Objects;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.index.file.MemoryNodeManager;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.CompositeChange;
import com.google.dart.engine.services.change.CreateFileChange;
import com.google.dart.engine.services.change.Edit;
import com.google.dart.engine.services.change.MergeCompositeChange;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.NullProgressMonitor;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.Refactoring;
import com.google.dart.engine.services.refactoring.RenameRefactoring;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;
import com.google.dart.engine.source.Source;

import java.util.List;

/**
 * Abstract test for testing {@link RenameRefactoring}s.
 */
public abstract class RefactoringImplTest extends AbstractDartTest {
  /**
   * Assert result of applying given {@link Change} to the "source".
   */
  public static void assertChangeResult(AnalysisContext context, Change compositeChange,
      Source source, String expected) throws Exception {
    SourceChange sourceChange = getSourceChange(compositeChange, source);
    assertNotNull("No change for: " + source.toString(), sourceChange);
    String sourceResult = getChangeResult(context, source, sourceChange);
    assertEquals(expected, sourceResult);
  }

  /**
   * Asserts that "refactoring" status is OK.
   */
  public static void assertRefactoringStatusOK(RenameRefactoring refactoring) throws Exception {
    ProgressMonitor pm = new NullProgressMonitor();
    assertRefactoringStatus(
        refactoring.checkInitialConditions(pm),
        RefactoringStatusSeverity.OK,
        null);
    assertRefactoringStatus(
        refactoring.checkFinalConditions(pm),
        RefactoringStatusSeverity.OK,
        null);
  }

  /**
   * @return the {@link CreateFileChange} for the file with the given name (not full path).
   */
  public static CreateFileChange findCreateFileChange(Change change, String fileName) {
    if (change instanceof CreateFileChange) {
      CreateFileChange fileChange = (CreateFileChange) change;
      if (fileChange.getFile().getName().equals(fileName)) {
        return fileChange;
      }
    }
    if (change instanceof CompositeChange) {
      CompositeChange compositeChange = (CompositeChange) change;
      for (Change childChange : compositeChange.getChildren()) {
        CreateFileChange fileChange = findCreateFileChange(childChange, fileName);
        if (fileChange != null) {
          return fileChange;
        }
      }
    }
    return null;
  }

  /**
   * @return the {@link SourceChange} for the given {@link Source}.
   */
  protected static SourceChange getSourceChange(Change change, Source source) {
    // may be SourceChange
    if (change instanceof SourceChange) {
      SourceChange sourceChange = (SourceChange) change;
      if (Objects.equal(sourceChange.getSource(), source)) {
        return sourceChange;
      }
    }
    // may be MergeCompositeChange
    if (change instanceof MergeCompositeChange) {
      MergeCompositeChange mergeChange = (MergeCompositeChange) change;
      SourceChange executeChange = getSourceChange(mergeChange.getExecuteChange(), source);
      SourceChange previewChange = getSourceChange(mergeChange.getPreviewChange(), source);
      if (executeChange != null && previewChange == null) {
        return executeChange;
      }
      if (executeChange == null && previewChange != null) {
        return previewChange;
      }
      return mergeSourceChanges(executeChange, previewChange);
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

  /**
   * @return the result of applying given {@link SourceChange} to the {@link #testCode}.
   */
  private static String getChangeResult(AnalysisContext context, Source source, SourceChange change)
      throws Exception {
    String sourceCode = CorrectionUtils.getSourceContent(context, source);
    List<Edit> sourceEdits = change.getEdits();
    return CorrectionUtils.applyReplaceEdits(sourceCode, sourceEdits);
  }

  /**
   * Returns new {@link SourceChange} that consists of the merged {@link Edit}s from the given
   * {@link SourceChange}. Note, that edit groups are not supported.
   */
  private static SourceChange mergeSourceChanges(SourceChange executeChange,
      SourceChange previewChange) {
    List<Edit> edits = executeChange.getEdits();
    List<Edit> edits2 = previewChange.getEdits();
    SourceChange merged = new SourceChange(executeChange.getName(), executeChange.getSource());
    for (Edit edit : edits) {
      merged.addEdit(edit);
    }
    for (Edit edit : edits2) {
      merged.addEdit(edit);
    }
    return merged;
  }

  protected final ProgressMonitor pm = new NullProgressMonitor();
  protected Index index;
  protected SearchEngine searchEngine;

  /**
   * Assert result of applying given {@link Change} to the given {@link Source}.
   */
  protected final void assertChangeResult(Change compositeChange, Source source, String expected)
      throws Exception {
    AnalysisContext context = getAnalysisContext();
    assertChangeResult(context, compositeChange, source, expected);
  }

  /**
   * Assert result of applying given {@link Change} to the {@link #testCode}.
   */
  protected final void assertTestChangeResult(Change compositeChange, String expected)
      throws Exception {
    assertChangeResult(compositeChange, testSource, expected);
  }

  /**
   * Resolve and index the given source.
   */
  protected final void indexTestUnit(Source source) throws Exception {
    parseTestUnit(source);
    AnalysisContext context = testUnit.getElement().getContext();
    index.indexUnit(context, testUnit);
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
   * Index the given {@link CompilationUnit}.
   */
  protected final void indexUnit(CompilationUnit unit) {
    AnalysisContext context = unit.getElement().getContext();
    index.indexUnit(context, unit);
  }

  /**
   * Resolve and index the given source.
   */
  protected final CompilationUnit indexUnit(Source source) throws Exception {
    CompilationUnit unit = parseUnit(source);
    indexUnit(unit);
    return unit;
  }

  /**
   * Parses and index given source code.
   */
  protected final CompilationUnit indexUnit(String path, String code) throws Exception {
    CompilationUnit unit = parseUnit(path, code);
    indexUnit(unit);
    return unit;
  }

  /**
   * Prints lines of result applying {@link Refactoring} to the the {@link #testSource}.
   */
  protected final void printRefactoringTestSourceResult(AnalysisContext context,
      Refactoring refactoring) throws Exception {
    Change refactoringChange = refactoring.createChange(pm);
    SourceChange testChange = getSourceChange(refactoringChange, testSource);
    String testResult = getChangeResult(context, testSource, testChange);
    printSourceLines(testResult);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // run Index
    IndexStore indexStore = IndexFactory.newSplitIndexStore(new MemoryNodeManager());
    index = IndexFactory.newIndex(indexStore);
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
    index = null;
    searchEngine = null;
    super.tearDown();
  }
}
