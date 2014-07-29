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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.server.RefactoringProblem;
import com.google.dart.server.RefactoringSetOptionsConsumer;
import com.google.gson.JsonObject;

/**
 * Instances of {@code RefactoringSetOptionsProcessor} translate JSON result objects for a given
 * {@link RefactoringSetOptionsConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class RefactoringSetOptionsProcessor extends ResultProcessor {

  private final RefactoringSetOptionsConsumer consumer;

  public RefactoringSetOptionsProcessor(RefactoringSetOptionsConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    RefactoringProblem[] refactoringProblems = constructRefactoringProblemArray(resultObject.get(
        "status").getAsJsonArray());
    consumer.computedStatus(refactoringProblems);
  }
}
