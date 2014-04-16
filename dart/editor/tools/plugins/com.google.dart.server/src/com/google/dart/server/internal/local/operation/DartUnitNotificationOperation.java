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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.source.Source;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.NotificationKind;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

/**
 * An operation for sending a notification to {@link AnalysisServerListener}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitNotificationOperation implements ServerOperation {
  private final String contextId;
  private final Source source;
  private final NotificationKind kind;
  private CompilationUnit unit;

  public DartUnitNotificationOperation(String contextId, Source source, NotificationKind kind,
      CompilationUnit unit) {
    this.contextId = contextId;
    this.source = source;
    this.kind = kind;
    this.unit = unit;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.CONTEXT_NOTIFICATION;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) throws Exception {
    server.internalDartUnitNotification(contextId, source, kind, unit);
  }
}
