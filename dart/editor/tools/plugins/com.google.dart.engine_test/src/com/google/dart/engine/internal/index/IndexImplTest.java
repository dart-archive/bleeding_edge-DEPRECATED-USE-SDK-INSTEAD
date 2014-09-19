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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.index.RelationshipCallback;
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

import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IndexImplTest extends EngineTestCase {
  private AnalysisContext context = mock(AnalysisContext.class);
  private IndexStore store = mock(IndexStore.class);
  private OperationQueue queue = mock(OperationQueue.class);
  private OperationProcessor processor = mock(OperationProcessor.class);
  private IndexImpl index = new IndexImpl(store, queue, processor);

  public void test_getIndexStatistics() throws Exception {
    String stats = "40 relationships in 20 elements in 10 sources";
    when(store.getStatistics()).thenReturn(stats);
    assertEquals(stats, index.getStatistics());
  }

  public void test_getRelationships() throws Exception {
    Element element = mock(Element.class);
    Relationship relationship = Relationship.getRelationship("test-relationship");
    RelationshipCallback callback = mock(RelationshipCallback.class);
    index.getRelationships(element, relationship, callback);
    // verify
    ArgumentCaptor<GetRelationshipsOperation> argument = ArgumentCaptor.forClass(GetRelationshipsOperation.class);
    verify(queue).enqueue(argument.capture());
    assertSame(element, argument.getValue().getElement());
    assertSame(relationship, argument.getValue().getRelationship());
    assertSame(callback, argument.getValue().getCallback());
  }

  public void test_indexHtmlUnit() throws Exception {
    Source unitSource = mock(Source.class);
    // Dart CompilationUnitElement
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    // HtmlElement
    HtmlElement htmlElement = mock(HtmlElement.class);
    when(htmlElement.getSource()).thenReturn(unitSource);
    when(htmlElement.getAngularCompilationUnit()).thenReturn(unitElement);
    // HtmlUnit
    HtmlUnit unit = mock(HtmlUnit.class);
    when(unit.getElement()).thenReturn(htmlElement);
    // call index
    index.indexHtmlUnit(context, unit);
    // verify
    ArgumentCaptor<IndexHtmlUnitOperation> argument = ArgumentCaptor.forClass(IndexHtmlUnitOperation.class);
    verify(queue).enqueue(argument.capture());
    assertSame(unit, argument.getValue().getUnit());
  }

  public void test_indexHtmlUnit_noHtmlElement() throws Exception {
    HtmlUnit unit = mock(HtmlUnit.class);
    index.indexHtmlUnit(context, unit);
    // verify
    verifyZeroInteractions(queue);
  }

  public void test_indexHtmlUnit_noUnitElement() throws Exception {
    HtmlUnit unit = mock(HtmlUnit.class);
    HtmlElement htmlElement = mock(HtmlElement.class);
    when(unit.getElement()).thenReturn(htmlElement);
    index.indexHtmlUnit(context, unit);
    // verify
    verifyZeroInteractions(queue);
  }

  public void test_indexHtmlUnit_null() throws Exception {
    index.indexHtmlUnit(context, null);
    // verify
    verifyZeroInteractions(queue);
  }

  public void test_indexUnit() throws Exception {
    Source unitSource = mock(Source.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    CompilationUnit unit = mock(CompilationUnit.class);
    when(unit.getElement()).thenReturn(unitElement);
    when(unitElement.getSource()).thenReturn(unitSource);
    // call index
    index.indexUnit(context, unit);
    // verify
    ArgumentCaptor<IndexUnitOperation> argument = ArgumentCaptor.forClass(IndexUnitOperation.class);
    verify(queue).enqueue(argument.capture());
    assertSame(unit, argument.getValue().getUnit());
  }

  public void test_indexUnit_notResolved() throws Exception {
    CompilationUnit unit = mock(CompilationUnit.class);
    index.indexUnit(context, unit);
    // verify
    verifyZeroInteractions(queue);
  }

  public void test_indexUnit_null() throws Exception {
    index.indexUnit(context, null);
    // verify
    verifyZeroInteractions(queue);
  }

  public void test_removeContext() throws Exception {
    index.removeContext(context);
    // verify
    ArgumentCaptor<RemoveContextOperation> argument = ArgumentCaptor.forClass(RemoveContextOperation.class);
    verify(queue).enqueue(argument.capture());
    assertSame(context, argument.getValue().getContext());
  }

  public void test_removeSource() throws Exception {
    Source source = mock(Source.class);
    index.removeSource(context, source);
    // verify
    ArgumentCaptor<RemoveSourceOperation> argument = ArgumentCaptor.forClass(RemoveSourceOperation.class);
    verify(queue).enqueue(argument.capture());
    assertSame(source, argument.getValue().getSource());
  }

  public void test_removeSources() throws Exception {
    SourceContainer container = mock(SourceContainer.class);
    index.removeSources(context, container);
    // verify
    ArgumentCaptor<RemoveSourcesOperation> argument = ArgumentCaptor.forClass(RemoveSourcesOperation.class);
    verify(queue).enqueue(argument.capture());
    assertSame(container, argument.getValue().getContainer());
  }

  public void test_run() throws Exception {
    index.run();
    verify(processor).run();
  }

  public void test_stop() throws Exception {
    index.stop();
    verify(processor).stop(false);
  }

  @Override
  protected void tearDown() throws Exception {
    context = null;
    store = null;
    queue = null;
    processor = null;
    index = null;
    super.tearDown();
  }
}
