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
package com.google.dart.engine.internal.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
import com.google.dart.engine.internal.index.operation.ClearOperation;
import com.google.dart.engine.internal.index.operation.GetRelationshipsOperation;
import com.google.dart.engine.internal.index.operation.IndexHtmlUnitOperation;
import com.google.dart.engine.internal.index.operation.IndexUnitOperation;
import com.google.dart.engine.internal.index.operation.OperationProcessor;
import com.google.dart.engine.internal.index.operation.OperationQueue;
import com.google.dart.engine.internal.index.operation.RemoveContextOperation;
import com.google.dart.engine.internal.index.operation.RemoveSourceOperation;
import com.google.dart.engine.internal.index.operation.RemoveSourcesOperation;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;
import com.google.dart.engine.utilities.translation.DartOmit;

/**
 * Implementation of {@link Index}.
 * 
 * @coverage dart.engine.index
 */
@DartOmit
public class IndexImpl implements Index {
  private final IndexStore store;
  private final OperationQueue queue;
  private final OperationProcessor processor;

  public IndexImpl(IndexStore store, OperationQueue queue, OperationProcessor processor) {
    this.store = store;
    this.queue = queue;
    this.processor = processor;
  }

  @Override
  public void clear() {
    queue.enqueue(new ClearOperation(store));
  }

  @Override
  public void getRelationships(Element element, Relationship relationship,
      RelationshipCallback callback) {
    queue.enqueue(new GetRelationshipsOperation(store, element, relationship, callback));
  }

  @Override
  public String getStatistics() {
    return store.getStatistics();
  }

  @Override
  public void indexHtmlUnit(AnalysisContext context, HtmlUnit unit) {
    if (unit == null) {
      return;
    }
    if (unit.getElement() == null) {
      return;
    }
    if (unit.getElement().getAngularCompilationUnit() == null) {
      return;
    }
    queue.enqueue(new IndexHtmlUnitOperation(store, context, unit));
  }

  @Override
  public void indexUnit(AnalysisContext context, CompilationUnit unit) {
    if (unit == null) {
      return;
    }
    if (unit.getElement() == null) {
      return;
    }
    queue.enqueue(new IndexUnitOperation(store, context, unit));
  }

  @VisibleForTesting
  public boolean isOperationQueueEmpty() {
    return queue.size() == 0;
  }

  @Override
  public void removeContext(AnalysisContext context) {
    queue.enqueue(new RemoveContextOperation(store, context));
  }

  @Override
  public void removeSource(AnalysisContext context, Source source) {
    queue.enqueue(new RemoveSourceOperation(store, context, source));
  }

  @Override
  public void removeSources(AnalysisContext context, SourceContainer container) {
    queue.enqueue(new RemoveSourcesOperation(store, context, container));
  }

  @Override
  public void run() {
    processor.run();
  }

  @Override
  public void stop() {
    processor.stop(false);
  }
}
