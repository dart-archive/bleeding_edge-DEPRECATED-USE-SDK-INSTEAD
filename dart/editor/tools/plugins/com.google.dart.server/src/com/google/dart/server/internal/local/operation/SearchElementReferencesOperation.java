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

import com.google.dart.server.AnalysisServer;
import com.google.dart.server.Element;
import com.google.dart.server.SearchResultsConsumer;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

/**
 * An operation for
 * {@link AnalysisServer#searchElementReferences(Element, boolean, SearchResultsConsumer)}.
 * 
 * @coverage dart.server.local
 */
public class SearchElementReferencesOperation implements ContextServerOperation {
//  private final String contextId;
  private final Element element;
  private final boolean withPotential;
  private final SearchResultsConsumer consumer;

  public SearchElementReferencesOperation(Element element, boolean withPotential,
      SearchResultsConsumer consumer) {
    // Element API has changed
//    this.contextId = element.getContextId();
    this.element = element;
    this.withPotential = withPotential;
    this.consumer = consumer;
  }

  @Override
  public String getContextId() {
//    return contextId;
    return "";
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.SEARCH;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    server.internalSearchElementReferences(contextId, element, withPotential, consumer);
  }
}
