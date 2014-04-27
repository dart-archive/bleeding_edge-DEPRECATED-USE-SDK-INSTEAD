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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

public class ParseDartTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    ParseDartTask task = new ParseDartTask(null, null, 0L, null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitParseDartTask(ParseDartTask task) throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getCompilationUnit() {
    ParseDartTask task = new ParseDartTask(null, null, 0L, null, null);
    assertNull(task.getCompilationUnit());
  }

  public void test_getErrors() {
    ParseDartTask task = new ParseDartTask(null, null, 0L, null, null);
    assertLength(0, task.getErrors());
  }

  public void test_getException() {
    ParseDartTask task = new ParseDartTask(null, null, 0L, null, null);
    assertNull(task.getException());
  }

  public void test_getModificationTime() {
    long modificationTime = 26L;
    ParseDartTask task = new ParseDartTask(null, null, modificationTime, null, null);
    assertEquals(modificationTime, task.getModificationTime());
  }

  public void test_getSource() {
    Source source = new TestSource("");
    ParseDartTask task = new ParseDartTask(null, source, 0L, null, null);
    assertSame(source, task.getSource());
  }

  public void test_hasNonPartOfDirective() {
    ParseDartTask task = new ParseDartTask(null, null, 0L, null, null);
    assertFalse(task.hasNonPartOfDirective());
  }

  public void test_hasPartOfDirective() {
    ParseDartTask task = new ParseDartTask(null, null, 0L, null, null);
    assertFalse(task.hasPartOfDirective());
  }

  public void test_perform_exception() throws AnalysisException {
    final TestSource source = new TestSource();
    source.setGenerateExceptionOnRead(true);
    InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseDartTask task = new ParseDartTask(context, source, 0L, null, null);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitParseDartTask(ParseDartTask task) throws AnalysisException {
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
        "class A {");
    final Source source = new TestSource(content);
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseDartTask task = createParseTask(context, source, content);
    task.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitParseDartTask(ParseDartTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getCompilationUnit());
        assertLength(1, task.getErrors());
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertSame(source, task.getSource());
        assertTrue(task.hasNonPartOfDirective());
        assertFalse(task.hasPartOfDirective());
        return null;
      }
    });
  }

  public void test_perform_part() throws AnalysisException {
    String content = createSource(//
        "part of lib;",
        "class B {}");
    final Source source = new TestSource(content);
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseDartTask task = createParseTask(context, source, content);
    task.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitParseDartTask(ParseDartTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getCompilationUnit());
        assertLength(0, task.getErrors());
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertSame(source, task.getSource());
        assertFalse(task.hasNonPartOfDirective());
        assertTrue(task.hasPartOfDirective());
        return null;
      }
    });
  }

  public void test_perform_validateDirectives() throws AnalysisException {
    String content = createSource(//
        "library lib;",
        "import '/does/not/exist.dart';",
        "import 'invaliduri^.dart';",
        "export '${a}lib3.dart';",
        "part 'part.dart';",
        "class A {}");
    final Source source = new TestSource(content);
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseDartTask task = createParseTask(context, source, content);
    task.perform(new TestTaskVisitor<Void>() {
      @Override
      public Void visitParseDartTask(ParseDartTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getCompilationUnit());
        GatheringErrorListener errorListener = new GatheringErrorListener();
        errorListener.addAll(task.getErrors());
        errorListener.assertErrorsWithCodes(
            CompileTimeErrorCode.URI_WITH_INTERPOLATION,
            CompileTimeErrorCode.INVALID_URI);
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertSame(source, task.getSource());
        assertTrue(task.hasNonPartOfDirective());
        assertFalse(task.hasPartOfDirective());
        return null;
      }
    });
  }

  /**
   * Create and return a task that will parse the given content from the given source in the given
   * context.
   * 
   * @param context the context to be passed to the task
   * @param source the source to be parsed
   * @param content the content of the source to be parsed
   * @return the task that was created
   * @throws AnalysisException if the task could not be created
   */
  private ParseDartTask createParseTask(final InternalAnalysisContext context, final Source source,
      String content) throws AnalysisException {
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
    return new ParseDartTask(
        context,
        source,
        scanTask.getModificationTime(),
        scanTask.getTokenStream(),
        scanTask.getLineInfo());
  }
}
