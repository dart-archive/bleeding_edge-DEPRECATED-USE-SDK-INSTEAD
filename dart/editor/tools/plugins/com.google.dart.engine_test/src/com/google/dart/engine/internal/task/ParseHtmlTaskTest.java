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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.logging.Logger;
import com.google.dart.engine.utilities.logging.TestLogger;

import java.net.URI;
import java.net.URISyntaxException;

public class ParseHtmlTaskTest extends EngineTestCase {
  public void test_accept() throws Exception {
    ParseHtmlTask task = new ParseHtmlTask(null, null, 0L, "");
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() throws Exception {
    ParseHtmlTask task = new ParseHtmlTask(null, null, 0L, "");
    assertNull(task.getException());
  }

  public void test_getHtmlUnit() throws Exception {
    ParseHtmlTask task = new ParseHtmlTask(null, null, 0L, "");
    assertNull(task.getHtmlUnit());
  }

  public void test_getLineInfo() throws Exception {
    ParseHtmlTask task = new ParseHtmlTask(null, null, 0L, "");
    assertNull(task.getLineInfo());
  }

  public void test_getReferencedLibraries() throws Exception {
    ParseHtmlTask task = new ParseHtmlTask(null, null, 0L, "");
    assertLength(0, task.getReferencedLibraries());
  }

  public void test_getSource() throws Exception {
    Source source = new TestSource("");
    ParseHtmlTask task = new ParseHtmlTask(null, source, 0L, "");
    assertSame(source, task.getSource());
  }

  public void test_perform_embedded_source() throws Exception {
    String contents = createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart'>",
        "    void buttonPressed() {}",
        "  </script>",
        "</head>",
        "<body>",
        "</body>",
        "</html>");
    TestLogger testLogger = new TestLogger();
    ParseHtmlTask task = parseContents(contents, testLogger);
    assertLength(0, task.getReferencedLibraries());
    assertEquals(0, testLogger.getErrorCount());
    assertEquals(0, testLogger.getInfoCount());
  }

  public void test_perform_empty_source_reference() throws Exception {
    String contents = createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart' src=''/>",
        "</head>",
        "<body>",
        "</body>",
        "</html>");
    TestLogger testLogger = new TestLogger();
    ParseHtmlTask task = parseContents(contents, testLogger);
    assertLength(0, task.getReferencedLibraries());
    assertEquals(0, testLogger.getErrorCount());
    assertEquals(0, testLogger.getInfoCount());
  }

  public void test_perform_invalid_source_reference() throws Exception {
    String contents = createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart' src='an;invalid:[]uri'/>",
        "</head>",
        "<body>",
        "</body>",
        "</html>");
    TestLogger testLogger = new TestLogger();
    ParseHtmlTask task = parseContents(contents, testLogger);
    assertLength(0, task.getReferencedLibraries());
    assertEquals(0, testLogger.getErrorCount());
    assertEquals(0, testLogger.getInfoCount());
  }

  public void test_perform_non_existing_source_reference() throws Exception {
    String contents = createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart' src='does/not/exist.dart'/>",
        "</head>",
        "<body>",
        "</body>",
        "</html>");
    TestLogger testLogger = new TestLogger();
    ParseHtmlTask task = parseSource(new TestSource(contents) {
      @Override
      public URI resolveRelative(URI containedUri) {
        try {
          return new URI("file:/does/not/exist.dart");
        } catch (URISyntaxException exception) {
          return null;
        }
      }
    }, contents, testLogger);
    assertLength(0, task.getReferencedLibraries());
    assertEquals(0, testLogger.getErrorCount());
    assertEquals(0, testLogger.getInfoCount());
  }

  public void test_perform_referenced_source() throws Exception {
    String contents = createSource(//
        "<html>",
        "<head>",
        "  <script type='application/dart' src='test.dart'/>",
        "</head>",
        "<body>",
        "</body>",
        "</html>");
    TestLogger testLogger = new TestLogger();
    ParseHtmlTask task = parseContents(contents, testLogger);
    assertLength(1, task.getReferencedLibraries());
    assertEquals(0, testLogger.getErrorCount());
    assertEquals(0, testLogger.getInfoCount());
  }

  private ParseHtmlTask parseContents(String contents, TestLogger testLogger) throws Exception {
    return parseSource(new TestSource(contents), contents, testLogger);
  }

  private ParseHtmlTask parseSource(final Source source, String contents, TestLogger testLogger)
      throws Exception {
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setContents(source, contents);
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    ParseHtmlTask task = new ParseHtmlTask(
        context,
        source,
        context.getModificationStamp(source),
        contents);
    Logger oldLogger = AnalysisEngine.getInstance().getLogger();
    try {
      AnalysisEngine.getInstance().setLogger(testLogger);
      task.perform(new TestTaskVisitor<Boolean>() {
        @Override
        public Boolean visitParseHtmlTask(ParseHtmlTask task) throws AnalysisException {
          AnalysisException exception = task.getException();
          if (exception != null) {
            throw exception;
          }
          assertNotNull(task.getHtmlUnit());
          assertNotNull(task.getLineInfo());
          assertEquals(context.getModificationStamp(source), task.getModificationTime());
          assertSame(source, task.getSource());
          return true;
        }
      });
    } finally {
      AnalysisEngine.getInstance().setLogger(oldLogger);
    }
    return task;
  }
}
