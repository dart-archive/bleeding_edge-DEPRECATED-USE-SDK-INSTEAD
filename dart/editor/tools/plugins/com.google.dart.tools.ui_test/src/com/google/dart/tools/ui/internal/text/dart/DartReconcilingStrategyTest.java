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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.common.base.Joiner;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;
import com.google.dart.tools.core.internal.model.DartIgnoreManager;
import com.google.dart.tools.core.internal.model.MockIgnoreFile;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.swt.events.DisposeListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DartReconcilingStrategyTest extends TestCase {

  /**
   * AnalysisManager that does not perform background analysis.
   */
  private final class MockAnalysisManager extends AnalysisManager {
    private int backgroundAnalysisCount = 0;

    public void assertBackgroundAnalysis(int expected) {
      assertEquals(expected, backgroundAnalysisCount);
      AnalysisWorker[] workers = getQueueWorkers();
      assertEquals(expected > 0 ? 1 : 0, workers.length);
      if (expected > 0) {
        assertSame(mockContext, workers[0].getContext());
      }
    }

    @Override
    public void startBackgroundAnalysis() {
      backgroundAnalysisCount++;
    }
  }

  /**
   * Analysis context that can wait for priority order and resolution.
   */
  private class MockContext extends AnalysisContextImpl {
    private List<Source> priorityOrder = new ArrayList<Source>();
    private int setContentsCount = 0;
    private int setChangedContentsCount = 0;
    private int changedOffset = 0;
    private int changedOldLength = 0;
    private int changedNewLength = 0;

    public List<Source> getPriorityOrder() {
      return priorityOrder;
    }

    @Override
    public void setAnalysisPriorityOrder(List<Source> sources) {
      super.setAnalysisPriorityOrder(sources);
      priorityOrder = sources;
    }

    @Override
    public void setChangedContents(Source source, String contents, int offset, int oldLength,
        int newLength) {
      if (source == mockSource) {
        setChangedContentsCount++;
        changedOffset = offset;
        changedOldLength = oldLength;
        changedNewLength = newLength;
      }
      super.setChangedContents(source, contents, offset, oldLength, newLength);
    }

    @Override
    public void setContents(Source source, String contents) {
      if (source == mockSource) {
        setContentsCount++;
      }
      setContentsForTest(source, contents);
    }

    void assertSetChangedContentsCount(int expectedCount, int expectedOffset,
        int expectedOldLength, int expectedNewLength) {
      assertEquals(expectedCount, setChangedContentsCount);
      assertEquals(expectedOffset, changedOffset);
      assertEquals(expectedOldLength, changedOldLength);
      assertEquals(expectedNewLength, changedNewLength);
    }

    void assertSetContentsCount(int expected) {
      assertEquals(expected, setContentsCount);
    }

    void setContentsForTest(Source source, String contents) {
      super.setContents(source, contents);
    }
  }

  /**
   * Mock editor for testing {@link DartReconcilingStrategy}
   */
  private final class MockEditor implements DartReconcilingEditor {
    private CompilationUnit appliedUnit = null;
    private DisposeListener disposeListener = null;

    @Override
    public void addViewerDisposeListener(DisposeListener listener) {
      if (disposeListener != null) {
        throw new RuntimeException("dispose listener already added");
      }
      disposeListener = listener;
    }

    @Override
    public void applyResolvedUnit(CompilationUnit unit) {
      appliedUnit = unit;
    }

    public CompilationUnit getAppliedCompilationUnit() {
      return appliedUnit;
    }

    public DisposeListener getDisposeListener() {
      return disposeListener;
    }

    @Override
    public AnalysisContext getInputAnalysisContext() {
      return mockContext;
    }

    @Override
    public String getInputFilePath() {
      // TODO(scheglov) Analysis Server
      throw new UnsupportedOperationException();
    }

    @Override
    public Project getInputProject() {
      // TODO (danrubel): test null and non-null project
      return null;
    }

    @Override
    public Source getInputSource() {
      return mockSource;
    }

    @Override
    public String getTitle() {
      return mockSource.getShortName();
    }

    @Override
    public void setDartReconcilingStrategy(DartReconcilingStrategy dartReconcilingStrategy) {
      // ignored
    }
  }

  private final static String EOL = System.getProperty("line.separator", "\n");
  private static final String INITIAL_CONTENTS = Joiner.on(EOL).join(//
      "library a;",
      "class A { foo() => this; }");

  MockEditor mockEditor = new MockEditor();
  Source mockSource = new TestSource(createFile("/test.dart"), INITIAL_CONTENTS);
  MockContext mockContext = new MockContext();
  Document mockDocument = new Document(INITIAL_CONTENTS);
  MockAnalysisManager analysisManager = new MockAnalysisManager();
  DartIgnoreManager ignoreManager = new DartIgnoreManager(new MockIgnoreFile());
  DartReconcilingStrategy strategy = new DartReconcilingStrategy(
      mockEditor,
      analysisManager,
      ignoreManager);

  /**
   * Assert reconciler clears cached contents when disposed
   */
  public void test_dispose() throws Exception {
    mockContext.setContentsForTest(mockSource, INITIAL_CONTENTS);
    mockContext.setAnalysisPriorityOrder(Arrays.asList(new Source[] {mockSource}));

    mockEditor.getDisposeListener().widgetDisposed(null);

    waitForBackgroundThread();
    analysisManager.assertBackgroundAnalysis(1);
  }

  /**
   * Assert that a "." triggers immediate analysis
   */
  public void test_docChange_period() throws Exception {
    String insertedText = ".";
    int offset = INITIAL_CONTENTS.indexOf("this") + 4;
    assert offset > 5;

    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(offset, 0, insertedText);
    waitForBackgroundThread();

    // assert "." causes immediate update of the context

    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);
    mockContext.assertSetChangedContentsCount(1, offset, 0, 1);
    mockContext.assertSetContentsCount(0);
  }

  /**
   * Assert unit resolved, applied, and order set during initialReconcile
   */
  public void test_initialReconcile() {
    strategy.initialReconcile();

    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert unit resolved, applied, and order set during initialReconcile
   */
  public void test_initialReconcile_cachedUnit() {
    analysisManager.performAnalysis(null);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.initialReconcile();

    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert ignored source is not analyzed
   */
  public void test_initialReconcile_ignoredSource() throws Exception {
    ignoreManager.addToIgnores(mockSource.getFullName());
    strategy.initialReconcile();

    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(0);

    analysisManager.performAnalysis(null);

    assertNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert editor with no context does not throw exception
   */
  public void test_initialReconcile_nullContext() {
    mockContext = null;
    assertNull(mockEditor.getInputAnalysisContext());

    strategy.initialReconcile();

    // test infrastructure asserts no exceptions
  }

  /**
   * Assert reconciler lazily sets cached contents and performs resolution
   */
  public void test_initialState() throws Exception {
    assertNull(mockContext.getResolvedCompilationUnit(mockSource, mockSource));
    assertEquals(0, mockContext.getPriorityOrder().size());
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcile() throws Exception {
    String insertedText = "//comment\n";

    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(3, 7, insertedText);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile();
    waitForBackgroundThread();

    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);
    mockContext.assertSetChangedContentsCount(1, 3, 7, insertedText.length());
    mockContext.assertSetContentsCount(0);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcile_delete() throws Exception {
    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 10, "");

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile();
    waitForBackgroundThread();
    mockContext.assertSetChangedContentsCount(1, 0, 10, 0);
    mockContext.assertSetContentsCount(0);

    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcile_ignoredSource() throws Exception {
    ignoreManager.addToIgnores(mockSource.getFullName());
    String insertedText = "//comment\n";

    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(0);
    analysisManager.performAnalysis(null);

    assertNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(3, 7, insertedText);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile();
    waitForBackgroundThread();

    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(1);
    mockContext.assertSetChangedContentsCount(1, 3, 7, insertedText.length());
    mockContext.assertSetContentsCount(0);

    analysisManager.performAnalysis(null);

    assertNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert editor with no context does not throw exception
   */
  public void test_reconcile_nullContext() throws Exception {
    String insertedText = "//comment\n";

    mockContext = null;
    strategy.initialReconcile();
    mockDocument.replace(0, 0, insertedText);
    strategy.reconcile();
    analysisManager.performAnalysis(null);

    // test infrastructure asserts no exceptions
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcileDirtyRegionIRegion() throws Exception {
    String insertedText = "//comment\n";

    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 0, insertedText);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, insertedText), new Region(0, 0));
    waitForBackgroundThread();

    mockContext.assertSetChangedContentsCount(1, 0, 0, insertedText.length());
    mockContext.assertSetContentsCount(0);
    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcileDirtyRegionIRegion_delete() throws Exception {
    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 10, "");

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new DirtyRegion(0, 10, DirtyRegion.REMOVE, null), new Region(0, 0));
    waitForBackgroundThread();

    mockContext.assertSetChangedContentsCount(1, 0, 10, 0);
    mockContext.assertSetContentsCount(0);
    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert editor with no context does not throw exception
   */
  public void test_reconcileDirtyRegionIRegion_nullContext() throws Exception {
    String insertedText = "//comment\n";

    mockContext = null;
    strategy.initialReconcile();
    mockDocument.replace(0, 0, insertedText);
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, insertedText), new Region(0, 0));
    analysisManager.performAnalysis(null);

    // test infrastructure asserts no exceptions
  }

  public void test_reconcileIRegion() throws Exception {
    String insertedText = "//comment\n";

    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 0, insertedText);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new Region(0, insertedText.length()));
    waitForBackgroundThread();

    mockContext.assertSetChangedContentsCount(1, 0, 0, insertedText.length());
    mockContext.assertSetContentsCount(0);
    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  public void test_reconcileIRegion_delete() throws Exception {
    strategy.initialReconcile();
    mockContext.assertSetChangedContentsCount(0, 0, 0, 0);
    mockContext.assertSetContentsCount(0);
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 10, "");

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new Region(0, 10));
    waitForBackgroundThread();

    mockContext.assertSetChangedContentsCount(1, 0, 10, 0);
    mockContext.assertSetContentsCount(0);
    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  /**
   * Assert editor with no context does not throw exception
   */
  public void test_reconcileIRegion_nullContext() throws Exception {
    String insertedText = "//comment\n";

    mockContext = null;
    strategy.initialReconcile();
    mockDocument.replace(0, 0, insertedText);
    strategy.reconcile(new Region(0, insertedText.length()));

    // test infrastructure asserts no exceptions
  }

  @Override
  protected void setUp() throws Exception {
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    assertNotNull(sdk);
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    mockContext.setSourceFactory(sourceFactory);
    strategy.setDocument(mockDocument);
  }

  @Override
  protected void tearDown() throws Exception {
    // Ensure that strategy removes its AnalysisWorker listener
    if (strategy != null) {
      strategy.dispose();
    }
    mockEditor = null;
    mockSource = null;
    mockContext = null;
    mockDocument = null;
    analysisManager = null;
    ignoreManager = null;
    strategy = null;
  }

  /**
   * We cannot talk to {@link AnalysisContext} on the UI thread, so we push update/analyze requests
   * and execute them in background. But in test we want to wait until these requests are executed.
   */
  private void waitForBackgroundThread() {
    DartUpdateSourceHelper.getInstance().waitForEmptyQueue();
  }
}
