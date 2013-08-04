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
import com.google.dart.engine.context.AnalysisErrorInfo;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.context.ChangeNotice;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.tools.core.analysis.model.Project;

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

  private final class MockContext extends com.google.dart.tools.core.internal.builder.MockContext {
    private final Object lock = new Object();
    private String cachedContents = null;
    private Exception exception = null;
    private CompilationUnit unit = null;
    private boolean resolved = false;
    private AnalysisErrorInfo unitInfo;
    private List<Source> priorityOrder = new ArrayList<Source>();

    public Exception getException() {
      return exception;
    }

    @Override
    public Source[] getLibrariesContaining(Source source) {
      if (mockSource.equals(source)) {
        return new Source[] {mockSource};
      }
      return super.getLibrariesContaining(source);
    }

    public CompilationUnit getUnit() {
      return unit;
    }

    @Override
    public CompilationUnit parseCompilationUnit(Source source) throws AnalysisException {
      assertSame(mockSource, source);
      synchronized (lock) {
        if (unit != null) {
          final AnalysisContextImpl context = new AnalysisContextImpl();
          context.setContents(mockSource, cachedContents);
          unit = context.parseCompilationUnit(mockSource);
          unitInfo = context.getErrors(mockSource);
        }
        return unit;
      }
    }

    @Override
    public ChangeNotice[] performAnalysisTask() {
      if (!resolved) {
        resolved = true;
        final CompilationUnit resolvedUnit;
        final AnalysisErrorInfo resolvedInfo;
        synchronized (lock) {
          try {
            resolvedUnit = parseCompilationUnit(mockSource);
            resolvedInfo = unitInfo;
          } catch (AnalysisException e) {
            exception = e;
            return super.performAnalysisTask();
          }
        }
        return new ChangeNotice[] {new ChangeNotice() {

          @Override
          public CompilationUnit getCompilationUnit() {
            return resolvedUnit;
          }

          @Override
          public AnalysisError[] getErrors() {
            if (resolvedInfo != null) {
              return resolvedInfo.getErrors();
            }
            return null;
          }

          @Override
          public LineInfo getLineInfo() {
            return resolvedInfo.getLineInfo();
          }

          @Override
          public Source getSource() {
            return mockSource;
          }
        }};
      }
      return super.performAnalysisTask();
    }

    @Override
    public void setAnalysisPriorityOrder(List<Source> sources) {
      synchronized (lock) {
        priorityOrder = sources;
        lock.notifyAll();
      }
    }

    @Override
    public void setContents(Source source, String contents) {
      assertSame(mockSource, source);
      synchronized (lock) {
        cachedContents = contents;
        resolved = false;
        unit = null;
        unitInfo = null;
        lock.notifyAll();
      }
    }

    /**
     * Wait up to the specified number of milliseconds for the cached contents.
     * 
     * @param milliseconds the maximum number of milliseconds to wait
     * @param expectedContents the expected cached contents
     * @return <code>true</code> if the contents match the expected contents
     */
    public boolean waitFoCachedContents(long milliseconds, String expectedContents) {
      synchronized (lock) {
        long end = System.currentTimeMillis() + milliseconds;
        while (true) {
          if (expectedContents == null ? cachedContents == null
              : expectedContents.equals(cachedContents)) {
            return true;
          }
          long delta = end - System.currentTimeMillis();
          if (delta <= 0) {
            return false;
          }
          try {
            lock.wait(delta);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
      }
    }

    /**
     * Wait up to the specified number of milliseconds for the priority order to be set.
     * 
     * @param expectedContents the expected cached contents
     * @param milliseconds the maximum number of milliseconds to wait
     * @return <code>true</code> if the contents match the expected contents
     */
    public boolean waitForAnalysisPriorityOrder(long milliseconds, Source... expectedOrder) {
      synchronized (lock) {
        long end = System.currentTimeMillis() + milliseconds;
        while (true) {
          if (Arrays.asList(expectedOrder).equals(priorityOrder)) {
            return true;
          }
          long delta = end - System.currentTimeMillis();
          if (delta <= 0) {
            return false;
          }
          try {
            lock.wait(delta);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
      }
    }
  }

  private final class MockEditor implements DartReconcilingEditor {
    private final Object lock = new Object();

    private DisposeListener disposeListener = null;

    private FocusListener focusListener = null;

    private CompilationUnit appliedUnit = null;
    private boolean applyCalled;

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
      synchronized (lock) {
        appliedUnit = unit;
        applyCalled = true;
        lock.notifyAll();
      }
    }

    public void clearApplyCalled() {
      applyCalled = true;
    }

    public CompilationUnit getAppliedUnit() {
      synchronized (lock) {
        return appliedUnit;
      }
    }

    public DisposeListener getDisposeListener() {
      return disposeListener;
    }

    public FocusListener getFocusListener() {
      return focusListener;
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

    /**
     * Wait up to the specified amount of time for the
     * {@link #applyCompilationUnitElement(CompilationUnit)} method to be called.
     * 
     * @param milliseconds the maximum number of milliseconds to wait
     * @return {@code true} if the method was called, else {@code false}
     */
    public boolean waitForApply(long milliseconds) {
      long end = System.currentTimeMillis() + milliseconds;
      synchronized (lock) {
        while (!applyCalled) {
          long delta = end - System.currentTimeMillis();
          if (delta <= 0) {
            return false;
          }
          try {
            lock.wait(delta);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
      }
      return true;
    }
  }

  private final static String EOL = System.getProperty("line.separator", "\n");
  private static final String INITIAL_CONTENTS = Joiner.on(EOL).join(//
      "library a;",
      "class A { foo() => 'two'; }");

  MockEditor mockEditor = new MockEditor();

  Source mockSource = new TestSource(INITIAL_CONTENTS);
  MockContext mockContext = new MockContext();
  Document mockDocument = new Document();
  DartReconcilingStrategy strategy = new DartReconcilingStrategy(mockEditor, mockSource);

  public void test_dispose() throws Exception {
    mockContext.setContents(mockSource, INITIAL_CONTENTS);
    mockContext.setAnalysisPriorityOrder(Arrays.asList(new Source[] {mockSource}));
    mockEditor.getDisposeListener().widgetDisposed(null);
    // assert reconciler clears cached contents when disposed
    assertTrue(mockContext.waitFoCachedContents(5000, null));
    assertTrue(mockContext.waitForAnalysisPriorityOrder(5000));
  }

  public void test_initialReconcile() {
    strategy.initialReconcile();
    assertTrue(mockEditor.waitForApply(5000));
    assertSame(mockContext.getUnit(), mockEditor.getAppliedUnit());
    assertTrue(mockContext.waitForAnalysisPriorityOrder(5000, mockSource));
  }

  public void test_initialReconcile_nullContext() {
    mockContext = null;
    strategy.initialReconcile();
    assertNull(mockEditor.getAppliedUnit());
  }

  // assert reconciler lazily sets cached contents and performs resolution
  public void test_initialState() throws Exception {
    assertTrue(mockContext.waitFoCachedContents(5000, null));
    assertNull(mockContext.getUnit());
    assertTrue(mockContext.waitForAnalysisPriorityOrder(5000));
  }

  public void test_reconcileDirtyRegionIRegion() {
    strategy.initialReconcile();
    assertTrue(mockEditor.waitForApply(5000));
    mockEditor.clearApplyCalled();
    strategy.reconcile(new DirtyRegion(0, 5, "ignored", "hello"), new Region(0, 5));
    assertTrue(mockEditor.waitForApply(5000));
    assertSame(mockContext.getUnit(), mockEditor.getAppliedUnit());
  }

  public void test_reconcileDirtyRegionIRegion_nullContext() {
    mockContext = null;
    strategy.initialReconcile();
    strategy.reconcile(new DirtyRegion(0, 5, "ignored", "hello"), new Region(0, 5));
    assertNull(mockEditor.getAppliedUnit());
  }

  public void test_reconcileIRegion() {
    strategy.initialReconcile();
    assertTrue(mockEditor.waitForApply(5000));
    mockEditor.clearApplyCalled();
    strategy.reconcile(new Region(0, 5));
    assertTrue(mockEditor.waitForApply(5000));
    assertSame(mockContext.getUnit(), mockEditor.getAppliedUnit());
  }

  public void test_reconcileIRegion_nullContext() {
    mockContext = null;
    strategy.initialReconcile();
    strategy.reconcile(new Region(0, 5));
    assertNull(mockEditor.getAppliedUnit());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    mockContext.setSourceFactory(sourceFactory);
    strategy.setDocument(mockDocument);
  }

  @Override
  protected void tearDown() throws Exception {
    if (mockContext != null) {
      assertNull(mockContext.getException());
    }
    // Ensure that strategy removes its AnalysisWorker listener
    if (strategy != null) {
      strategy.dispose();
    }
  }
}
