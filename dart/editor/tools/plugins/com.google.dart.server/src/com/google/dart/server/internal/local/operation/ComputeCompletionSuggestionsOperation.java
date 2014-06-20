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
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.CompletionIdConsumer;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

/**
 * An operation for {@link AnalysisServer#computeCompletionSuggestions}.
 * 
 * @coverage dart.server.local
 */
public class ComputeCompletionSuggestionsOperation implements ContextServerOperation {
  private final String contextId;
  private final Source source;
  private final int offset;
  private final int length;
  private final CompletionIdConsumer consumer;

  public ComputeCompletionSuggestionsOperation(String contextId, Source source, int offset,
      int length, CompletionIdConsumer consumer) {
    this.contextId = contextId;
    this.source = source;
    this.offset = offset;
    this.length = length;
    this.consumer = consumer;
  }

  @Override
  public String getContextId() {
    return contextId;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.SEARCH;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    server.internalComputeCompletionSuggestions(contextId, source, offset, length, consumer);
  }
}
