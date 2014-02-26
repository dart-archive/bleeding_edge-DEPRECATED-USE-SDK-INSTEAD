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
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.io.IOException;

public class ResolveHtmlTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    ResolveHtmlTask task = new ResolveHtmlTask(null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getElement() {
    ResolveHtmlTask task = new ResolveHtmlTask(null, null);
    assertNull(task.getElement());
  }

  public void test_getException() {
    ResolveHtmlTask task = new ResolveHtmlTask(null, null);
    assertNull(task.getException());
  }

  public void test_getModificationTime() {
    ResolveHtmlTask task = new ResolveHtmlTask(null, null);
    assertEquals(-1L, task.getModificationTime());
  }

  public void test_getResolutionErrors() {
    ResolveHtmlTask task = new ResolveHtmlTask(null, null);
    assertLength(0, task.getResolutionErrors());
  }

  public void test_getSource() {
    Source source = new TestSource("");
    ResolveHtmlTask task = new ResolveHtmlTask(null, source);
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
    ResolveHtmlTask task = new ResolveHtmlTask(context, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException {
        assertNotNull(task.getException());
        return true;
      }
    });
  }

  public void test_perform_valid() throws AnalysisException {
    final Source source = new TestSource(createFile("/test.html"), createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart'>",
        "    void f() { x = 0; }",
        "  </script>",
        "</head>",
        "<body>",
        "</body>",
        "</html>"));
    final InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    ResolveHtmlTask task = new ResolveHtmlTask(context, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveHtmlTask(ResolveHtmlTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getElement());
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertLength(1, task.getResolutionErrors());
        assertSame(source, task.getSource());
        return true;
      }
    });
  }
}
