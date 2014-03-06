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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

import java.io.IOException;

public class ResolveDartDependenciesTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(null, null, 0L, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartDependenciesTask(ResolveDartDependenciesTask task)
          throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(null, null, 0L, null);
    assertNull(task.getException());
  }

  public void test_getExportedSources() {
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(null, null, 0L, null);
    assertLength(0, task.getExportedSources());
  }

  public void test_getImportedSources() {
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(null, null, 0L, null);
    assertLength(0, task.getImportedSources());
  }

  public void test_getIncludedSources() {
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(null, null, 0L, null);
    assertLength(0, task.getIncludedSources());
  }

  public void test_getModificationTime() {
    long modificationTime = 12L;
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(
        null,
        null,
        modificationTime,
        null);
    assertEquals(modificationTime, task.getModificationTime());
  }

  public void test_getSource() {
    Source source = new TestSource("");
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(null, source, 0L, null);
    assertSame(source, task.getSource());
  }

  public void test_perform_exception() throws AnalysisException {
    final Source source = new TestSource() {
      @Override
      public TimestampedData<CharSequence> getContents() throws Exception {
        throw new IOException();
      }
    };
    InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ResolveDartDependenciesTask task = new ResolveDartDependenciesTask(context, source, 0L, null);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartDependenciesTask(ResolveDartDependenciesTask task)
          throws AnalysisException {
        assertNotNull(task.getException());
        return true;
      }
    });
  }

  public void test_perform_library() throws AnalysisException {
    String content = createSource(//
        "library lib;",
        "import 'lib2.dart';",
        "export 'lib3.dart';",
        "part 'part.dart';",
        "class A {}",
        ";");
    final Source source = new TestSource(content);
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ResolveDartDependenciesTask task = createResolveTask(context, source, content);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartDependenciesTask(ResolveDartDependenciesTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertLength(1, task.getExportedSources());
        assertLength(1, task.getImportedSources());
        assertLength(1, task.getIncludedSources());
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertSame(source, task.getSource());
        return true;
      }
    });
  }

  public void test_perform_part() throws AnalysisException {
    String content = createSource(//
        "library lib;",
        "import 'a.dart';",
        "export 'b.dart';",
        "part 'c.dart';",
        "class D {}");
    final Source source = new TestSource(content);
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ResolveDartDependenciesTask task = createResolveTask(context, source, content);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartDependenciesTask(ResolveDartDependenciesTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertLength(1, task.getExportedSources());
        assertLength(1, task.getImportedSources());
        assertLength(1, task.getIncludedSources());
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertSame(source, task.getSource());
        return true;
      }
    });
  }

  /**
   * Create and return a task that will resolve dependencies in the given content from the given
   * source in the given context.
   * 
   * @param content the content of the source to be resolved
   * @param source the source to be resolved
   * @param context the context to be passed to the task
   * @return the task that was created
   * @throws AnalysisException if the task could not be created
   */
  private ResolveDartDependenciesTask createResolveTask(final InternalAnalysisContext context,
      final Source source, String content) throws AnalysisException {
    ScanDartTask scanTask = new ScanDartTask(
        context,
        source,
        context.getModificationStamp(source),
        content);
    scanTask.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitScanDartTask(ScanDartTask task) throws AnalysisException {
        return null;
      }
    });
    ParseDartTask parseTask = new ParseDartTask(
        context,
        source,
        scanTask.getModificationTime(),
        scanTask.getTokenStream());
    parseTask.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitParseDartTask(ParseDartTask task) throws AnalysisException {
        return null;
      }
    });
    return new ResolveDartDependenciesTask(
        context,
        source,
        parseTask.getModificationTime(),
        parseTask.getCompilationUnit());
  }
}
