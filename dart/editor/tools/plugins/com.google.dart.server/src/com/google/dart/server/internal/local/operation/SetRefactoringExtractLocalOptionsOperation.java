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
import com.google.dart.server.RefactoringOptionsValidationConsumer;
import com.google.dart.server.internal.local.LocalAnalysisServerImpl;

/**
 * An operation for {@link AnalysisServer#setRefactoringExtractLocalOptions}.
 * 
 * @coverage dart.server.local
 */
public class SetRefactoringExtractLocalOptionsOperation implements ServerOperation {
  private final String refactoringId;
  private final boolean allOccurrences;
  private final String name;
  private final RefactoringOptionsValidationConsumer consumer;

  public SetRefactoringExtractLocalOptionsOperation(String refactoringId, boolean allOccurrences,
      String name, RefactoringOptionsValidationConsumer consumer) {
    this.refactoringId = refactoringId;
    this.allOccurrences = allOccurrences;
    this.name = name;
    this.consumer = consumer;
  }

  @Override
  public ServerOperationPriority getPriority() {
    return ServerOperationPriority.REFACTORING;
  }

  @Override
  public void performOperation(LocalAnalysisServerImpl server) throws Exception {
    // TODO(scheglov) restore or remove for the new API
//    server.internalSetRefactoringExtractLocalOptions(refactoringId, allOccurrences, name, consumer);
  }
}
