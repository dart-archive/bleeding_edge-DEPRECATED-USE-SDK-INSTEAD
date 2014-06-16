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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.internal.index.IndexImpl;
import com.google.dart.engine.internal.index.MemoryIndexStoreImpl;
import com.google.dart.engine.internal.index.file.ContextCodec;
import com.google.dart.engine.internal.index.file.ElementCodec;
import com.google.dart.engine.internal.index.file.FileManager;
import com.google.dart.engine.internal.index.file.FileNodeManager;
import com.google.dart.engine.internal.index.file.NodeManager;
import com.google.dart.engine.internal.index.file.RelationshipCodec;
import com.google.dart.engine.internal.index.file.SplitIndexStoreImpl;
import com.google.dart.engine.internal.index.file.StringCodec;
import com.google.dart.engine.internal.index.file.SeparateFileManager;
import com.google.dart.engine.internal.index.operation.OperationProcessor;
import com.google.dart.engine.internal.index.operation.OperationQueue;
import com.google.dart.engine.utilities.translation.DartOmit;

import java.io.File;

/**
 * Factory for {@link Index} and {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
@DartOmit
public class IndexFactory {
  /**
   * Returns an instance of {@link IndexStore} that stores data on disk in the given directory.
   */
  public static IndexStore newFileIndexStore(File directory) {
    StringCodec stringCodec = new StringCodec();
    ContextCodec contextCodec = new ContextCodec();
    ElementCodec elementCodec = new ElementCodec(stringCodec);
    RelationshipCodec relationshipCodec = new RelationshipCodec(stringCodec);
    FileManager fileManager = new SeparateFileManager(directory);
    NodeManager nodeManager = new FileNodeManager(
        fileManager,
        AnalysisEngine.getInstance().getLogger(),
        stringCodec,
        contextCodec,
        elementCodec,
        relationshipCodec);
    return newSplitIndexStore(nodeManager);
  }

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

  /**
   * Returns an instance of {@link IndexStore} that stores data in the given {@link NodeManager}.
   */
  public static IndexStore newSplitIndexStore(NodeManager nodeManager) {
    return new SplitIndexStoreImpl(nodeManager);
  }
}
