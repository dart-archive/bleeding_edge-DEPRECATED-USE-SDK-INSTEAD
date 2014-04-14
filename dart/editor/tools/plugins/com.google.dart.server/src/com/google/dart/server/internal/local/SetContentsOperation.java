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

/**
 * An operation for {@link AnalysisServer#setContents(String, Source, String)}.
 * <p>
 * TODO(scheglov) move into ChangeSet
 */
public class SetContentsOperation implements ContextServerOperation, MergeableOperation {
  private final String contextId;
  private final Source source;
  private String contents;

  public SetContentsOperation(String contextId, Source source, String contents) {
    this.contextId = contextId;
    this.source = source;
    this.contents = contents;
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
  public boolean mergeWith(ServerOperation operation) {
    if (operation instanceof SetContentsOperation) {
      SetContentsOperation other = (SetContentsOperation) operation;
      if (contextId.equals(other.contextId) && source.equals(other.source)) {
        contents = other.contents;
        return true;
      }
    }
    return false;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) {
    server.internalSetContents(contextId, source, contents);
  }
}
