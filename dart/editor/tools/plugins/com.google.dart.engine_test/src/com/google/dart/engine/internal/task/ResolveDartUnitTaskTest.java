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
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;

import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.library;

public class ResolveDartUnitTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    ResolveDartUnitTask task = new ResolveDartUnitTask(null, null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    ResolveDartUnitTask task = new ResolveDartUnitTask(null, null, null);
    assertNull(task.getException());
  }

  public void test_getLibrarySource() {
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    LibraryElementImpl element = library(context, "lib");
    Source source = element.getSource();
    ResolveDartUnitTask task = new ResolveDartUnitTask(null, null, element);
    assertSame(source, task.getLibrarySource());
  }

  public void test_getModificationTime() {
    ResolveDartUnitTask task = new ResolveDartUnitTask(null, null, null);
    assertEquals(-1L, task.getModificationTime());
  }

  public void test_getResolvedUnit() {
    ResolveDartUnitTask task = new ResolveDartUnitTask(null, null, null);
    assertNull(task.getResolvedUnit());
  }

  public void test_getSource() {
    Source source = new TestSource("");
    ResolveDartUnitTask task = new ResolveDartUnitTask(null, source, null);
    assertSame(source, task.getSource());
  }

  public void test_perform_exception() throws AnalysisException {
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    LibraryElementImpl element = library(context, "lib");
    final TestSource source = new TestSource();
    source.setGenerateExceptionOnRead(true);
    ((CompilationUnitElementImpl) element.getDefiningCompilationUnit()).setSource(source);
    ResolveDartUnitTask task = new ResolveDartUnitTask(context, source, element);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException {
        assertNotNull(task.getException());
        return true;
      }
    });
  }

  public void test_perform_library() throws AnalysisException {
    final InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    LibraryElementImpl libraryElement = library(context, "lib");
    CompilationUnitElementImpl unitElement = (CompilationUnitElementImpl) libraryElement.getDefiningCompilationUnit();
    ClassElementImpl classElement = classElement("A");
    classElement.setNameOffset(19);
    ConstructorElementImpl constructorElement = constructorElement(classElement, null);
    constructorElement.setSynthetic(true);
    classElement.setConstructors(new ConstructorElement[] {constructorElement});
    unitElement.setTypes(new ClassElement[] {classElement});
    final Source source = unitElement.getSource();
    context.setContents(source, createSource(//
        "library lib;",
        "class A {}"));
    ResolveDartUnitTask task = new ResolveDartUnitTask(context, source, libraryElement);
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitResolveDartUnitTask(ResolveDartUnitTask task) throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertSame(source, task.getLibrarySource());
        assertEquals(context.getModificationStamp(source), task.getModificationTime());
        assertNotNull(task.getResolvedUnit());
        assertSame(source, task.getSource());
        return true;
      }
    });
  }
}
