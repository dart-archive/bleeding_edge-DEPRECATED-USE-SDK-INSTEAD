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
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

import java.io.IOException;
import java.net.URI;

public class ParseHtmlTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    ParseHtmlTask task = new ParseHtmlTask(null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    ParseHtmlTask task = new ParseHtmlTask(null, null);
    assertNull(task.getException());
  }

  public void test_getHtmlUnit() {
    ParseHtmlTask task = new ParseHtmlTask(null, null);
    assertNull(task.getHtmlUnit());
  }

  public void test_getLineInfo() {
    ParseHtmlTask task = new ParseHtmlTask(null, null);
    assertNull(task.getLineInfo());
  }

  public void test_getModificationTime() {
    ParseHtmlTask task = new ParseHtmlTask(null, null);
    assertEquals(-1L, task.getModificationTime());
  }

  public void test_getReferencedLibraries() {
    ParseHtmlTask task = new ParseHtmlTask(null, null);
    assertLength(0, task.getReferencedLibraries());
  }

  public void test_getSource() {
    Source source = new TestSource("");
    ParseHtmlTask task = new ParseHtmlTask(null, source);
    assertSame(source, task.getSource());
  }

  public void test_perform_exception() throws AnalysisException {
    final Source source = new TestSource() {
      @Override
      public void getContents(ContentReceiver receiver) throws Exception {
        throw new IOException();
      }
    };
    InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseHtmlTask task = new ParseHtmlTask(context, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
        assertNotNull(task.getException());
        return true;
      }
    });
  }

  public void test_perform_valid() throws AnalysisException {
    final Source source = new TestSource(createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart' src='test.dart'/>",
        "  <script type='application/dart'>",
        "    void buttonPressed() {}",
        "  </script>",
        "</head>",
        "<body>",
        "</body>",
        "</html>")) {
      @Override
      public Source resolveRelative(URI containedUri) {
        return new TestSource() {
          @Override
          public boolean exists() {
            return true;
          }
        };
      }
    };
    InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseHtmlTask task = new ParseHtmlTask(context, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getHtmlUnit());
        assertNotNull(task.getLineInfo());
        assertEquals(source.getModificationStamp(), task.getModificationTime());
        assertLength(1, task.getReferencedLibraries());
        assertSame(source, task.getSource());
        return true;
      }
    });
  }
}
