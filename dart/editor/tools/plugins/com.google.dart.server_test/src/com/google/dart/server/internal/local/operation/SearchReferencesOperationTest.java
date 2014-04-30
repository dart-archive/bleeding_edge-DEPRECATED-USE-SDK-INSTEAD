/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.operation;

import com.google.dart.engine.source.Source;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SearchReferencesOperationTest extends TestCase {
  private LocalAnalysisServerImpl server = mock(LocalAnalysisServerImpl.class);
  private Source source = mock(Source.class);

  public void test_perform() throws Exception {
    SearchResultsConsumer consumer = mock(SearchResultsConsumer.class);
    SearchReferencesOperation operation = new SearchReferencesOperation("id", source, 42, consumer);
    assertSame(ServerOperationPriority.SEARCH, operation.getPriority());
    assertEquals("id", operation.getContextId());
    // perform
    operation.performOperation(server);
    verify(server, times(1)).internalSearchReferences("id", source, 42, consumer);
  }
}
