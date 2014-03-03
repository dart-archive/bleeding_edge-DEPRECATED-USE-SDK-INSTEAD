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
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.source.TestSource;

import java.io.IOException;

public class GetContentTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    Source source = new TestSource("");
    GetContentTask task = new GetContentTask(null, source);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGetContentTask(GetContentTask task) throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    Source source = new TestSource("");
    GetContentTask task = new GetContentTask(null, source);
    assertNull(task.getException());
  }

  public void test_getModificationTime() {
    Source source = new TestSource("");
    GetContentTask task = new GetContentTask(null, source);
    assertEquals(-1L, task.getModificationTime());
  }

  public void test_getSource() {
    Source source = new TestSource("");
    GetContentTask task = new GetContentTask(null, source);
    assertSame(source, task.getSource());
  }

  public void test_perform_exception() throws AnalysisException {
    final Source source = new TestSource() {
      @Override
      public TimestampedData<CharSequence> getContents() throws Exception {
        throw new IOException();
      }
    };
//    final InternalAnalysisContext context = new AnalysisContextImpl();
//    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    GetContentTask task = new GetContentTask(null, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGetContentTask(GetContentTask task) throws AnalysisException {
        assertNotNull(task.getException());
        return true;
      }
    });
  }

  public void test_perform_valid() throws AnalysisException {
    String content = createSource(//
    "class A {}");
    final Source source = new TestSource(content);
    final InternalAnalysisContext context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new FileUriResolver()));
    GetContentTask task = new GetContentTask(context, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGetContentTask(GetContentTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertSame(source, task.getSource());
        return true;
      }
    });
  }
}
