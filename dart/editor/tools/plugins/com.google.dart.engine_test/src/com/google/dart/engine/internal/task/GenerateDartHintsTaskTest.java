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
import com.google.dart.engine.internal.context.TimestampedData;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import java.util.HashMap;

public class GenerateDartHintsTaskTest extends EngineTestCase {
  public void test_accept() throws AnalysisException {
    GenerateDartHintsTask task = new GenerateDartHintsTask(null, null);
    assertTrue(task.accept(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGenerateDartHintsTask(GenerateDartHintsTask task)
          throws AnalysisException {
        return true;
      }
    }));
  }

  public void test_getException() {
    GenerateDartHintsTask task = new GenerateDartHintsTask(null, null);
    assertNull(task.getException());
  }

  public void test_getHintMap() {
    GenerateDartHintsTask task = new GenerateDartHintsTask(null, null);
    assertNull(task.getHintMap());
  }

  public void test_getLibraryElement() {
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    LibraryElement element = library(context, "lib");
    GenerateDartHintsTask task = new GenerateDartHintsTask(context, element);
    assertSame(element, task.getLibraryElement());
  }

  public void test_perform() throws AnalysisException {
    InternalAnalysisContext context = AnalysisContextFactory.contextWithCore();
    ChangeSet changeSet = new ChangeSet();
    final Source librarySource = new FileBasedSource(createFile("/test.dart"));
    changeSet.addedSource(librarySource);
    Source unusedSource = new FileBasedSource(createFile("/unused.dart"));
    changeSet.addedSource(unusedSource);
    final Source partSource = new FileBasedSource(createFile("/part.dart"));
    changeSet.addedSource(partSource);
    context.applyChanges(changeSet);

    context.setContents(librarySource, createSource(//
        "library lib;",
        "import 'unused.dart';",
        "part 'part.dart';"));
    context.setContents(unusedSource, createSource(//
        "library unused;"));
    context.setContents(partSource, createSource(//
        "part of lib;"));

    GenerateDartHintsTask task = new GenerateDartHintsTask(
        context,
        context.computeLibraryElement(librarySource));
    task.perform(new TestTaskVisitor<Boolean>() {
      @Override
      public Boolean visitGenerateDartHintsTask(GenerateDartHintsTask task)
          throws AnalysisException {
        AnalysisException exception = task.getException();
        if (exception != null) {
          throw exception;
        }
        assertNotNull(task.getLibraryElement());
        HashMap<Source, TimestampedData<AnalysisError[]>> hintMap = task.getHintMap();
        assertSizeOfMap(2, hintMap);
        assertLength(1, hintMap.get(librarySource).getData());
        assertLength(0, hintMap.get(partSource).getData());
        return true;
      }
    });
  }
}
