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

import com.google.dart.engine.context.AnalysisOptions;
import com.google.dart.server.AnalysisServer;

/**
 * An operation for {@link AnalysisServer#setOptions(String, AnalysisOptions)}.
 */
public class ShutdownOperation implements ServerOperation {
  /**
   * The unique instance of this class.
   */
  public static final ShutdownOperation INSTANCE = new ShutdownOperation();

  private ShutdownOperation() {
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.SHUTDOWN;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) {
    // nothing
  }
}
