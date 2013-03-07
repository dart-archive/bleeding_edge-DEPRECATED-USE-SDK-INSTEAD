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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.source.Source;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RemoveContextOperationTest extends EngineTestCase {
  private IndexStore store = mock(IndexStore.class);
  private AnalysisContext context = mock(AnalysisContext.class);

  public void test_getContext() throws Exception {
    RemoveContextOperation operation = new RemoveContextOperation(store, context);
    assertSame(context, operation.getContext());
  }

  public void test_isQuery() throws Exception {
    RemoveContextOperation operation = new RemoveContextOperation(store, context);
    assertFalse(operation.isQuery());
  }

  public void test_performOperation() throws Exception {
    RemoveContextOperation operation = new RemoveContextOperation(store, context);
    operation.performOperation();
    verify(store).removeContext(context);
  }

  public void test_removeWhenSourceRemoved() throws Exception {
    RemoveContextOperation operation = new RemoveContextOperation(store, context);
    Source source = mock(Source.class);
    assertFalse(operation.removeWhenSourceRemoved(source));
  }

  public void test_toString() throws Exception {
    RemoveContextOperation operation = new RemoveContextOperation(store, context);
    assertThat(operation.toString()).startsWith("RemoveContext(");
  }
}
