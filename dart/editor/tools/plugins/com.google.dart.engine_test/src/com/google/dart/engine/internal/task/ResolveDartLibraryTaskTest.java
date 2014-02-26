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

import java.io.IOException;

public class ResolveDartLibraryTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(null, null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartLibraryTask(ResolveDartLibraryTask task)
          throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(null, null, null);
    assertNull(task.getException());
  }

  public void test_getLibraryResolver() {
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(null, null, null);
    assertNull(task.getLibraryResolver());
  }

  public void test_getLibrarySource() {
    Source source = new TestSource("");
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(null, null, source);
    assertSame(source, task.getLibrarySource());
  }

  public void test_getUnitSource() {
    Source source = new TestSource("");
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(null, source, null);
    assertSame(source, task.getUnitSource());
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
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(context, source, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartLibraryTask(ResolveDartLibraryTask task)
          throws AnalysisException {
        assertNotNull(task.getException());
        return true;
      }
    });
  }

  public void test_perform_library() throws AnalysisException {
    final Source source = new TestSource(createSource(//
        "library lib;",
        "class A {}"));
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    ResolveDartLibraryTask task = new ResolveDartLibraryTask(context, source, source);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartLibraryTask(ResolveDartLibraryTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getLibraryResolver());
        assertSame(source, task.getLibrarySource());
        assertSame(source, task.getUnitSource());
        return true;
      }
    });
  }
}
