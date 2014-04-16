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

import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

/**
 * Instances of {@link PerformAnalysisOperation} perform a single analysis task.
 * 
 * @coverage dart.server.local
 */
public class PerformAnalysisOperation implements ContextServerOperation, MergeableOperation {
  private final String contextId;
  private final boolean isPriority;
  private final boolean isContinue;

  public PerformAnalysisOperation(String contextId) {
    this(contextId, false, false);
  }

  public PerformAnalysisOperation(String contextId, boolean isPriority, boolean isContinue) {
    this.contextId = contextId;
    this.isPriority = isPriority;
    this.isContinue = isContinue;
  }

  @Override
  public String getContextId() {
    return contextId;
  }

  @Override
  public ServerOperationPriority getPriority() {
    if (isPriority) {
      if (isContinue) {
        return ServerOperationPriority.CONTEXT_ANALYSIS_PRIORITY_CONTINUE;
      } else {
        return ServerOperationPriority.CONTEXT_ANALYSIS_PRIORITY;
      }
    }
    if (isContinue) {
      return ServerOperationPriority.CONTEXT_ANALYSIS_CONTINUE;
    } else {
      return ServerOperationPriority.CONTEXT_ANALYSIS;
    }
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
  public void performOperation(LocalAnalysisServerImpl server) throws Exception {
    server.internalPerformAnalysis(contextId);
  }
}
