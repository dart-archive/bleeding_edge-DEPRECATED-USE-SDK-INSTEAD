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
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.internal.builder.AnalysisManager;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;

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
      assertEquals(1, workers.length);
      assertSame(mockContext, workers[0].getContext());
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

    void assertSetChangedContentsCount(int expected) {
      assertEquals(expected, setChangedContentsCount);
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
    private FocusListener focusListener = null;

    @Override
    public void addViewerDisposeListener(DisposeListener listener) {
      if (disposeListener != null) {
        throw new RuntimeException("dispose listener already added");
      }
      disposeListener = listener;
    }

    @Override
    public void addViewerFocusListener(FocusListener listener) {
      if (focusListener != null) {
        throw new RuntimeException("focus listener already added");
      }
      focusListener = listener;
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
  }

  private final static String EOL = System.getProperty("line.separator", "\n");
  private static final String INITIAL_CONTENTS = Joiner.on(EOL).join(//
      "library a;",
      "class A { foo() => this; }");

  MockEditor mockEditor = new MockEditor();
  ContentCache mockCache = new ContentCache();
  Source mockSource = new TestSource(mockCache, createFile("/test.dart"), INITIAL_CONTENTS);
  MockContext mockContext = new MockContext();
  Document mockDocument = new Document(INITIAL_CONTENTS);
  MockAnalysisManager analysisManager = new MockAnalysisManager();
  List<Source> visibleSources = Arrays.asList(mockSource);
  DartReconcilingStrategy strategy = new DartReconcilingStrategy(mockEditor, analysisManager) {
    @Override
    protected List<Source> getVisibleSourcesForContext(AnalysisContext context) {
      return new ArrayList<Source>(visibleSources);
    };

    @Override
    protected void updateAnalysisPriorityOrder(boolean isOpen) {
      updateAnalysisPriorityOrderOnUiThread(isOpen);
    };
  };

  /**
   * Assert reconciler clears cached contents when disposed
   */
  public void test_dispose() throws Exception {
    mockContext.setContentsForTest(mockSource, INITIAL_CONTENTS);
    mockContext.setAnalysisPriorityOrder(Arrays.asList(new Source[] {mockSource}));

    visibleSources = Arrays.asList();
    mockEditor.getDisposeListener().widgetDisposed(null);

    // Assert reconciler requested background analysis
    analysisManager.assertBackgroundAnalysis(1);

    assertEquals(0, mockContext.getPriorityOrder().size());
    assertNull(mockCache.getContents(mockSource));
  }

  /**
   * Assert unit resolved, applied, and order set during initialReconcile
   */
  public void test_initialReconcile() {
    strategy.initialReconcile();

    assertEquals(1, mockContext.getPriorityOrder().size());
    assertSame(mockSource, mockContext.getPriorityOrder().get(0));
    assertNotNull(mockEditor.getAppliedCompilationUnit());
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

    assertEquals(1, mockContext.getPriorityOrder().size());
    assertSame(mockSource, mockContext.getPriorityOrder().get(0));
    assertNotNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(1);
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
    assertNull(mockCache.getContents(mockSource));
    assertNull(mockContext.getResolvedCompilationUnit(mockSource, mockSource));
    assertEquals(0, mockContext.getPriorityOrder().size());
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcileDirtyRegionIRegion() throws Exception {
    String insertedText = "//comment\n";

    strategy.initialReconcile();
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 0, insertedText);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, insertedText), new Region(0, 0));

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
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 10, "");

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new DirtyRegion(0, 10, DirtyRegion.REMOVE, null), new Region(0, 0));

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

  /**
   * Assert that a "." triggers immediate analysis
   */
  public void test_reconcileDirtyRegionIRegion_period() throws Exception {
    String insertedText = ".";
    int offset = INITIAL_CONTENTS.indexOf("this") + 4;
    assert offset > 5;
    String newText = INITIAL_CONTENTS.substring(0, offset) + insertedText
        + INITIAL_CONTENTS.substring(offset);

    strategy.initialReconcile();
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(offset, 0, insertedText);

    // assert "." causes immediate update of the context
    assertEquals(newText, mockCache.getContents(mockSource));
    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);
  }

  public void test_reconcileIRegion() throws Exception {
    String insertedText = "//comment\n";

    strategy.initialReconcile();
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 0, insertedText);

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new Region(0, insertedText.length()));

    assertNull(mockEditor.getAppliedCompilationUnit());
    analysisManager.assertBackgroundAnalysis(2);

    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());
  }

  public void test_reconcileIRegion_delete() throws Exception {
    strategy.initialReconcile();
    analysisManager.assertBackgroundAnalysis(1);
    analysisManager.performAnalysis(null);

    assertNotNull(mockEditor.getAppliedCompilationUnit());

    mockDocument.replace(0, 10, "");

    assertNull(mockEditor.getAppliedCompilationUnit());

    strategy.reconcile(new Region(0, 10));

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
    SourceFactory sourceFactory = new SourceFactory(
        mockCache,
        new DartUriResolver(sdk),
        new FileUriResolver());
    mockContext.setSourceFactory(sourceFactory);
    strategy.setDocument(mockDocument);
  }

  @Override
  protected void tearDown() throws Exception {
    // Ensure that strategy removes its AnalysisWorker listener
    if (strategy != null) {
      strategy.dispose();
    }
  }
}
