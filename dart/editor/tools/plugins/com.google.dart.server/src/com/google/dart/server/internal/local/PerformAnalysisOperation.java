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

package com.google.dart.server.internal.local;

/**
 * Instances of {@link PerformAnalysisOperation} perform a single analysis task.
 * 
 * @coverage dart.server.local
 */
public class PerformAnalysisOperation implements ContextServerOperation, MergeableOperation {
  private final String contextId;

  public PerformAnalysisOperation(String contextId) {
    this.contextId = contextId;
  }

  @Override
  public String getContextId() {
    return contextId;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.CONTEXT_ANALYSIS;
  }

  @Override
  public boolean mergeWith(ServerOperation operation) {
    if (operation instanceof PerformAnalysisOperation) {
      PerformAnalysisOperation other = (PerformAnalysisOperation) operation;
      return contextId.equals(other.contextId);
    }
    return false;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) {
    server.internalPerformAnalysis(contextId);
  }
}
