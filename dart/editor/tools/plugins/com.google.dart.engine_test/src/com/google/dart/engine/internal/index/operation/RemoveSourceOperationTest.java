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
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.source.Source;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemoveSourceOperationTest extends EngineTestCase {
  private IndexStore store = mock(IndexStore.class);
  private Source source = mock(Source.class);

  public void test_getSource() throws Exception {
    RemoveSourceOperation operation = new RemoveSourceOperation(store, source);
    assertSame(source, operation.getSource());
  }

  public void test_isQuery() throws Exception {
    RemoveSourceOperation operation = new RemoveSourceOperation(store, source);
    assertFalse(operation.isQuery());
  }

  public void test_performOperation_noPostClearRunnable() throws Exception {
    RemoveSourceOperation operation = new RemoveSourceOperation(store, source);
    operation.performOperation();
    verify(store, only()).removeSource(source);
  }

  public void test_removeWhenSourceRemoved() throws Exception {
    RemoveSourceOperation operation = new RemoveSourceOperation(store, null);
    Source source = mock(Source.class);
    assertFalse(operation.removeWhenSourceRemoved(source));
  }

  public void test_toString() throws Exception {
    RemoveSourceOperation operation = new RemoveSourceOperation(store, source);
    when(source.getFullName()).thenReturn("mySource");
    assertEquals("RemoveSource(mySource)", operation.toString());
  }
}
