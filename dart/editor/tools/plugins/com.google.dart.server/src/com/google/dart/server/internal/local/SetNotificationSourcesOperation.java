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

import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.NotificationKind;

/**
 * An operation for
 * {@link AnalysisServer#setNotificationSources(String, NotificationKind, Source[])}.
 * 
 * @coverage dart.server.local
 */
public class SetNotificationSourcesOperation implements ContextServerOperation, MergeableOperation {
  private final String contextId;
  private final NotificationKind kind;
  private Source[] sources;

  public SetNotificationSourcesOperation(String contextId, NotificationKind kind, Source[] sources) {
    this.contextId = contextId;
    this.kind = kind;
    this.sources = sources;
  }

  @Override
  public String getContextId() {
    return contextId;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.CONTEXT_NOTIFICATION;
  }

  @Override
  public boolean mergeWith(ServerOperation operation) {
    if (operation instanceof SetNotificationSourcesOperation) {
      SetNotificationSourcesOperation other = (SetNotificationSourcesOperation) operation;
      if (contextId.equals(other.contextId) && kind == other.kind) {
        sources = other.sources;
        return true;
      }
    }
    return false;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) {
    server.internalSetNotificationSources(contextId, kind, sources);
  }
}
