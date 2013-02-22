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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexFactory;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.internal.correction.AbstractDartTest;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.services.refactoring.NullProgressMonitor;
import com.google.dart.engine.services.refactoring.ProgressMonitor;
import com.google.dart.engine.services.refactoring.RenameRefactoring;

import java.util.List;

/**
 * Abstract test for testing {@link RenameRefactoring}s.
 */
public abstract class RefactoringImplTest extends AbstractDartTest {
  protected final ProgressMonitor pm = new NullProgressMonitor();
  private Index index;
  protected SearchEngine searchEngine;

  /**
   * Assert result of applying given {@link Change} to the {@link #testCode}.
   */
  protected final void assertChangeResult(Change change, String expected) {
    String changedCode = getTestSourceChangeResult(change);
    assertEquals(expected, changedCode);
  }

  /**
   * @return the result of applying given {@link SourceChange} (casted) to the {@link #testCode}.
   */
  protected final String getTestSourceChangeResult(Change change) {
    SourceChange sourceChange = (SourceChange) change;
    List<Edit> sourceEdits = sourceChange.getEdits();
    return CorrectionUtils.applyReplaceEdits(testCode, sourceEdits);
  }

  /**
   * Parses and index given source lines.
   */
  protected final void indexTestUnit(String... lines) throws Exception {
    parseTestUnit(lines);
    index.indexUnit(testUnit);
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
