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
package com.google.dart.engine.index;

import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.engine.internal.index.MemoryIndexStoreImpl;
import com.google.dart.engine.internal.index.operation.OperationProcessor;
import com.google.dart.engine.internal.index.operation.OperationQueue;
import com.google.dart.engine.utilities.translation.DartOmit;

/**
 * Factory for {@link Index} and {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
@DartOmit
public class IndexFactory {
  /**
   * @return the new instance of {@link Index} which uses given {@link IndexStore}.
   */
  public static Index newIndex(IndexStore store) {
    OperationQueue queue = new OperationQueue();
    OperationProcessor processor = new OperationProcessor(queue);
    return new IndexImpl(store, queue, processor);
  }

  /**
   * @return the new instance of {@link MemoryIndexStore}.
   */
  public static MemoryIndexStore newMemoryIndexStore() {
    return new MemoryIndexStoreImpl();
  }
}
