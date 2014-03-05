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
import com.google.dart.engine.context.ChangeSet;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

public class GenerateDartErrorsTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    GenerateDartErrorsTask task = new GenerateDartErrorsTask(null, null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGenerateDartErrorsTask(GenerateDartErrorsTask task)
          throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    GenerateDartErrorsTask task = new GenerateDartErrorsTask(null, null, null);
    assertNull(task.getException());
  }

  public void test_getLibraryElement() {
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    LibraryElement element = library(context, "lib");
    GenerateDartErrorsTask task = new GenerateDartErrorsTask(context, null, element);
    assertSame(element, task.getLibraryElement());
  }

  public void test_getSource() {
    Source source = new FileBasedSource(createFile("/test.dart"));
    GenerateDartErrorsTask task = new GenerateDartErrorsTask(null, source, null);
    assertSame(source, task.getSource());
  }

  public void test_perform() throws AnalysisException {
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    final Source librarySource = new FileBasedSource(createFile("/test.dart"));
    ChangeSet changeSet = new ChangeSet();
    changeSet.addedSource(librarySource);
    context.applyChanges(changeSet);
    context.setContents(librarySource, createSource(//
        "library lib;",
        "class A {",
        "  int f = new A();",
        "}"));
    final LibraryElement libraryElement = context.computeLibraryElement(librarySource);

    GenerateDartErrorsTask task = new GenerateDartErrorsTask(context, librarySource, libraryElement);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGenerateDartErrorsTask(GenerateDartErrorsTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertSame(libraryElement, task.getLibraryElement());
        assertSame(librarySource, task.getSource());
        AnalysisError[] errors = task.getErrors();
        assertLength(1, errors);
        return true;
      }
    });
  }
}
