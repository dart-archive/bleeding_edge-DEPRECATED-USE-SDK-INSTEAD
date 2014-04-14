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

import com.google.dart.engine.context.ChangeSet;
import com.google.dart.server.AnalysisServer;

/**
 * An operation for {@link AnalysisServer#applyChanges(String, ChangeSet)}.
 */
public class ApplyChangesOperation implements ContextServerOperation {
  private final String contextId;
  private final ChangeSet changeSet;

  public ApplyChangesOperation(String contextId, ChangeSet changeSet) {
    this.contextId = contextId;
    this.changeSet = changeSet;
  }

  @Override
  public String getContextId() {
    return contextId;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.CONTEXT_CHANGE;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) {
    server.internalApplyChanges(contextId, changeSet);
  }
}
