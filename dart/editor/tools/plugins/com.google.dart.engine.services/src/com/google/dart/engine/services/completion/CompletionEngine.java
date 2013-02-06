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
package com.google.dart.engine.services.completion;

import com.google.dart.engine.services.assist.AssistContext;

import static com.google.dart.engine.services.completion.ProposalKind.METHOD;

/**
 * The analysis engine for code completion.
 */
public class CompletionEngine {

  private CompletionRequestor requestor;
  private CompletionFactory factory;
  private AssistContext context;

  public CompletionEngine(CompletionRequestor requestor, CompletionFactory factory) {
    this.requestor = requestor;
    this.factory = factory;
  }

  /**
   * Analyze the source unit in the given context to determine completion proposals at the selection
   * offset of the context.
   */
  public void complete(AssistContext context) {
    this.context = context;
    // Temporary code for exercising test framework.
    requestor.beginReporting();
    CompletionProposal prop = factory.createCompletionProposal(METHOD);
    prop.setCompletion("toString");
    requestor.accept(prop);
    requestor.endReporting();
  }

  public AssistContext getContext() {
    return context;
  }
}
