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
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.ContentCache;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;
import com.google.dart.tools.core.analysis.model.AnalysisEvent;
import com.google.dart.tools.core.analysis.model.AnalysisListener;
import com.google.dart.tools.core.analysis.model.Project;
import com.google.dart.tools.core.analysis.model.ResolvedEvent;
import com.google.dart.tools.core.internal.builder.AnalysisWorker;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class DartReconcilingStrategyTest extends TestCase {

  /**
   * Content cache that can wait for contents to be set.
   * 
   * @see #expectSetContentsFor(Source)
   * @see #waitForSetContents(Source, long)
   */
  private class MockCache extends ContentCache {
    private Source expectedSource = null;
    private CountDownLatch expectedLatch = null;

    public void expectSetContentsFor(Source source) {
      expectedSource = source;
      expectedLatch = new CountDownLatch(1);
    }

    @Override
    public boolean setContents(Source source, String contents) {
      boolean result = super.setContents(source, contents);
      if (source == expectedSource) {
        expectedLatch.countDown();
      }
      return result;
    }

    public String waitForSetContents(Source source, long milliseconds) {
      if (source != expectedSource) {
        throw new IllegalArgumentException("Should call expectSetContentsFor");
      }
      try {
        assertTrue(expectedLatch.await(milliseconds, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        assertEquals(0, expectedLatch.getCount());
      }
      expectedSource = null;
      expectedLatch = null;
      return getContents(source);
    }
  }

  /**
   * Analysis context that can wait for priority order and resolution.
   */
  private class MockContext extends AnalysisContextImpl {
    private CountDownLatch priorityOrderLatch = null;
    private List<Source> priorityOrder = null;
    private long performAnalysisTaskTime = 0;
    private long setPriorityOrderTime = 0;
    private CountDownLatch resolvedLatch = null;
    private Source resolvedSource = null;
    private CompilationUnit resolvedUnit = null;
    private int flushCount = 0;
    private int sourceChangedCount = 0;

    private final AnalysisListener backgroundAnalysisListener = new AnalysisListener() {
      @Override
      public void complete(AnalysisEvent event) {
        synchronized (this) {
          if (resolvedLatch != null && resolvedLatch.getCount() > 0) {
            if (event.getContext() == MockContext.this) {
              resolvedUnit = getResolvedCompilationUnit(resolvedSource, resolvedSource);
              resolvedLatch.countDown();
            }
          }
        }
      }

      @Override
      public void resolved(ResolvedEvent event) {
        synchronized (this) {
          if (resolvedLatch != null && resolvedLatch.getCount() > 0) {
            if (event.getContext() == MockContext.this && event.getSource() == resolvedSource) {
              resolvedUnit = event.getUnit();
              resolvedLatch.countDown();
            }
          }
        }
      }
    };

    public void assertPrioritySetBeforeBackgroundAnalysis() {
      assertTrue(setPriorityOrderTime > 0);
      assertTrue(setPriorityOrderTime < performAnalysisTaskTime || performAnalysisTaskTime == 0);
    }

    public void expectResolved(Source source) {
      synchronized (backgroundAnalysisListener) {
        resolvedSource = source;
        resolvedLatch = new CountDownLatch(1);
        resolvedUnit = null;
        AnalysisWorker.addListener(backgroundAnalysisListener);
      }
    }

    public void expectSetPriorityOrder() {
      priorityOrderLatch = new CountDownLatch(1);
    }

    /**
     * Flush the resolved compilation unit for the specified source by creating and resolving other
     * sources.
     * 
     * @param source the source of the compilation unit to be flushed (not <code>null</code>)
     */
    public void flushCompilationUnit(Source source) throws Exception {
      for (int i = 0; i < 128; i++) {
        File newFile = createFile("/test_" + ++flushCount + ".dart");
        Source newSource = new TestSource(mockCache, newFile, INITIAL_CONTENTS);
        assertNotNull(resolveCompilationUnit(newSource, newSource));
      }
    }

    public List<Source> getPriorityOrder() {
      return priorityOrder;
    }

    public int getSourceChangedCount() {
      return sourceChangedCount;
    }

    @Override
    public ChangeNotice[] performAnalysisTask() {
      if (performAnalysisTaskTime == 0) {
        performAnalysisTaskTime = System.currentTimeMillis();
        if (performAnalysisTaskTime == setPriorityOrderTime) {
          performAnalysisTaskTime++;
        }
      }
      return super.performAnalysisTask();
    }

    @Override
    public void setAnalysisPriorityOrder(List<Source> sources) {
      super.setAnalysisPriorityOrder(sources);
      if (setPriorityOrderTime == 0) {
        setPriorityOrderTime = System.currentTimeMillis();
        if (setPriorityOrderTime == performAnalysisTaskTime) {
          setPriorityOrderTime++;
        }
      }
      if (priorityOrderLatch != null) {
        priorityOrder = sources;
        priorityOrderLatch.countDown();
      }
    }

    @Override
    public void setContents(Source source, String contents) {
      if (source == mockSource) {
        sourceChangedCount++;
      }
      super.setContents(source, contents);
    }

    public CompilationUnit waitForResolution(Source source, long milliseconds) {
      if (source != resolvedSource) {
        throw new IllegalArgumentException("Call expectResolved");
      }
      try {
        assertTrue(resolvedLatch.await(milliseconds, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        assertEquals(0, resolvedLatch.getCount());
      }
      synchronized (backgroundAnalysisListener) {
        AnalysisWorker.removeListener(backgroundAnalysisListener);
        resolvedLatch = null;
        resolvedSource = null;
        return resolvedUnit;
      }
    }

    public List<Source> waitForSetPriorityOrder(long milliseconds) {
      if (priorityOrderLatch == null) {
        throw new IllegalStateException("Call expectSetPriorityOrder");
      }
      try {
        assertTrue(priorityOrderLatch.await(milliseconds, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        assertEquals(0, priorityOrderLatch.getCount());
      }
      priorityOrderLatch = null;
      return priorityOrder;
    }
  }

  /**
   * Mock editor for testing {@link DartReconcilingStrategy}
   */
  private final class MockEditor implements DartReconcilingEditor {
    private Semaphore applied = null;
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
    public void applyCompilationUnitElement(CompilationUnit unit) {
      appliedUnit = unit;
      if (applied != null) {
        applied.release();
      }
    }

    public void expectApply() {
      applied = new Semaphore(0);
      appliedUnit = null;
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

    public CompilationUnit waitForApply(long milliseconds) {
      if (applied == null) {
        throw new IllegalStateException("Call expectApply");
      }
      try {
        assertTrue(applied.tryAcquire(milliseconds, TimeUnit.MILLISECONDS));
      } catch (InterruptedException e) {
        assertTrue(applied.tryAcquire());
      }
      return appliedUnit;
    }
  }

  private final static String EOL = System.getProperty("line.separator", "\n");
  private static final String INITIAL_CONTENTS = Joiner.on(EOL).join(//
      "library a;",
      "class A { foo() => 'two'; }");

  MockEditor mockEditor = new MockEditor();
  MockCache mockCache = new MockCache();
  Source mockSource = new TestSource(mockCache, createFile("/test.dart"), INITIAL_CONTENTS);
  MockContext mockContext = new MockContext();
  Document mockDocument = new Document(INITIAL_CONTENTS);
  DartReconcilingStrategy strategy = new DartReconcilingStrategy(mockEditor) {
    @Override
    public void reconcile(DirtyRegion dirtyRegion, org.eclipse.jface.text.IRegion subRegion) {
      super.reconcile(dirtyRegion, subRegion);
    };
  };

  /**
   * Assert reconciler clears cached contents when disposed
   */
  public void test_dispose() throws Exception {
    mockContext.setContents(mockSource, INITIAL_CONTENTS);
    mockContext.setAnalysisPriorityOrder(Arrays.asList(new Source[] {mockSource}));
    mockCache.expectSetContentsFor(mockSource);
    mockContext.expectSetPriorityOrder();

    mockEditor.getDisposeListener().widgetDisposed(null);

    assertEquals(0, mockContext.waitForSetPriorityOrder(5000).size());
    assertNull(mockCache.waitForSetContents(mockSource, 5000));
  }

  /**
   * Assert unit resolved, applied, and order set during initialReconcile
   */
  public void test_initialReconcile() {
    mockEditor.expectApply();
    mockContext.expectResolved(mockSource);
    mockContext.expectSetPriorityOrder();

    strategy.initialReconcile();

    assertNotNull(mockEditor.waitForApply(5000));
    CompilationUnit unit = mockContext.waitForResolution(mockSource, 15000);
    assertNotNull(unit);
    unit = mockEditor.waitForApply(5000);
    assertNotNull(unit);
    List<Source> priorityOrder = mockContext.waitForSetPriorityOrder(15000);
    assertEquals(1, priorityOrder.size());
    assertSame(mockSource, priorityOrder.get(0));
    mockContext.assertPrioritySetBeforeBackgroundAnalysis();
  }

  /**
   * Assert unit resolved, applied, and order set during initialReconcile after AST has been removed
   * from the cache
   */
  public void test_initialReconcile_afterFlush() throws Exception {
    assertNotNull(mockContext.resolveCompilationUnit(mockSource, mockSource));
    mockContext.flushCompilationUnit(mockSource);
    assertNull(mockContext.getResolvedCompilationUnit(mockSource, mockSource));

    mockEditor.expectApply();
    mockContext.expectResolved(mockSource);
    mockContext.expectSetPriorityOrder();

    strategy.initialReconcile();

    assertNotNull(mockEditor.waitForApply(5000));
    CompilationUnit unit = mockContext.waitForResolution(mockSource, 15000);
    assertNotNull(unit);
    unit = mockEditor.waitForApply(5000);
    assertNotNull(unit);
    List<Source> priorityOrder = mockContext.waitForSetPriorityOrder(15000);
    assertEquals(1, priorityOrder.size());
    assertSame(mockSource, priorityOrder.get(0));
    mockContext.assertPrioritySetBeforeBackgroundAnalysis();
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
    assertNull(mockContext.getPriorityOrder());
  }

  /**
   * Assert that a document change clears the cached unit and a resolve resets it
   */
  public void test_reconcileDirtyRegionIRegion() throws Exception {
    String newText = "//comment\n";

    mockEditor.expectApply();
    strategy.initialReconcile();
    assertNotNull(mockEditor.waitForApply(5000));

    mockEditor.expectApply();
    mockDocument.replace(0, 0, newText);
    assertNull(mockEditor.waitForApply(5000));

    mockEditor.expectApply();
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));
    CompilationUnit unit = mockEditor.waitForApply(5000);
    assertNotNull(unit);
  }

  /**
   * Assert editor with no context does not throw exception
   */
  public void test_reconcileDirtyRegionIRegion_nullContext() throws Exception {
    String newText = "//comment\n";

    mockContext = null;
    strategy.initialReconcile();
    mockDocument.replace(0, 0, newText);
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));

    // test infrastructure asserts no exceptions
  }

  /**
   * Assert that multiple document changes in quick succession only result in a single analysis
   */
  public void test_reconcileDirtyRegionIRegion_twice() throws Exception {
    String newText = "//comment\n";

    mockEditor.expectApply();
    strategy.initialReconcile();
    assertNotNull(mockEditor.waitForApply(5000));

    mockDocument.replace(0, 0, newText);
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));
    assertEquals(1, mockContext.getSourceChangedCount());

    mockDocument.replace(0, 0, newText);
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));
    assertEquals(2, mockContext.getSourceChangedCount());

    mockDocument.replace(0, 0, newText);
    mockDocument.replace(0, 0, newText);
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));
    assertEquals(3, mockContext.getSourceChangedCount());
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));
    assertEquals(3, mockContext.getSourceChangedCount());

    mockDocument.replace(0, 0, newText);
    strategy.reconcile(new DirtyRegion(0, 0, DirtyRegion.INSERT, newText), new Region(0, 0));
    assertEquals(4, mockContext.getSourceChangedCount());
  }

  public void test_reconcileIRegion() throws Exception {
    String newText = "//comment\n";

    mockEditor.expectApply();
    strategy.initialReconcile();
    assertNotNull(mockEditor.waitForApply(5000));

    mockEditor.expectApply();
    mockDocument.replace(0, 0, newText);
    assertNull(mockEditor.waitForApply(5000));

    mockEditor.expectApply();
    strategy.reconcile(new Region(0, newText.length()));
    CompilationUnit unit = mockEditor.waitForApply(5000);
    assertNotNull(unit);
  }

  /**
   * Assert editor with no context does not throw exception
   */
  public void test_reconcileIRegion_nullContext() throws Exception {
    String newText = "//comment\n";

    mockContext = null;
    strategy.initialReconcile();
    mockDocument.replace(0, 0, newText);
    strategy.reconcile(new Region(0, newText.length()));

    // test infrastructure asserts no exceptions
  }

  /**
   * Assert unit resolved, applied, and order set during initialReconcile
   */
  public void xtest_initialReconcile_alreadyCached() throws Exception {
    CompilationUnit unit = mockContext.resolveCompilationUnit(mockSource, mockSource);
    assertNotNull(unit);
    mockEditor.expectApply();
    mockContext.expectResolved(mockSource);
    mockContext.expectSetPriorityOrder();

    strategy.initialReconcile();

    // TODO(brianwilkerson) There is no reason to expect that the returned AST will be identical.
    assertSame(unit, mockEditor.waitForApply(5000));
    List<Source> priorityOrder = mockContext.waitForSetPriorityOrder(5000);
    assertEquals(1, priorityOrder.size());
    assertSame(mockSource, priorityOrder.get(0));
    mockContext.assertPrioritySetBeforeBackgroundAnalysis();
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
