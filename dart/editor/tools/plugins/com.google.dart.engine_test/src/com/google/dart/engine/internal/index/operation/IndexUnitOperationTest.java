/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.internal.index.operation;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.internal.index.IndexContributor;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.logging.Logger;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexUnitOperationTest extends EngineTestCase {
  private AnalysisContext context = mock(AnalysisContext.class);
  private final IndexStore store = mock(IndexStore.class);
  private final Source unitSource = mock(Source.class);
  private final CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
  private final CompilationUnit unit = mock(CompilationUnit.class);
  private IndexUnitOperation operation;

  public void test_getSource() throws Exception {
    assertSame(unitSource, operation.getSource());
  }

  public void test_getUnit() throws Exception {
    assertSame(unit, operation.getUnit());
  }

  public void test_isQuery() throws Exception {
    assertFalse(operation.isQuery());
  }

  public void test_performOperation() throws Exception {
    operation.performOperation();
    verify(store).removeSource(context, unitSource);
    verify(unit).accept(isA(IndexContributor.class));
  }

  public void test_performOperation_whenException() throws Exception {
    Logger oldLogger = AnalysisEngine.getInstance().getLogger();
    try {
      Error myException = new Error();
      when(unit.accept(isA(IndexContributor.class))).thenThrow(myException);
      // set mock Logger
      Logger logger = mock(Logger.class);
      AnalysisEngine.getInstance().setLogger(logger);
      // run operation
      operation.performOperation();
      // verify that "myException" was logged
      verify(logger).logError(anyString(), same(myException));
    } finally {
      AnalysisEngine.getInstance().setLogger(oldLogger);
    }
  }

  public void test_removeWhenSourceRemoved() throws Exception {
    Source someSource = mock(Source.class);
    assertFalse(operation.removeWhenSourceRemoved(someSource));
    assertTrue(operation.removeWhenSourceRemoved(unitSource));
  }

  public void test_toString() throws Exception {
    when(unitSource.getFullName()).thenReturn("mySource");
    assertEquals("IndexResource(mySource)", operation.toString());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    when(unit.getElement()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(unitSource);
    operation = new IndexUnitOperation(store, context, unit);
  }
}
