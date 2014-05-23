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

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.server.InternalAnalysisServer;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An operation for {@link InternalAnalysisServer#getContext(String)}.
 */
public class GetContextOperation implements ContextServerOperation {
  private final CountDownLatch latch = new CountDownLatch(1);
  private final String contextId;
  private AnalysisContext context;

  public GetContextOperation(String contextId) {
    this.contextId = contextId;
  }

  public AnalysisContext getContext() {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new RuntimeException("Expected context for " + contextId);
      }
    } catch (InterruptedException e) {
      //$FALL-THROUGH$
    }
    return context;
  }

  @Override
  public String getContextId() {
    return contextId;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.SERVER;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    context = server.getContextMap().get(contextId);
//    latch.countDown();
  }
}
