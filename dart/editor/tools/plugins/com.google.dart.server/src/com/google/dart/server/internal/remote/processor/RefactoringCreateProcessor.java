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

import com.google.dart.server.RefactoringCreateConsumer;
import com.google.dart.server.RefactoringProblem;
import com.google.gson.JsonObject;

/**
 * Instances of {@code RefactoringCreateProcessor} translate JSON result objects for a given
 * {@link RefactoringCreateConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class RefactoringCreateProcessor extends ResultProcessor {

  private final RefactoringCreateConsumer consumer;

  public RefactoringCreateProcessor(RefactoringCreateConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    String refactoringId = resultObject.get("id").getAsString();
    RefactoringProblem[] problems = constructRefactoringProblemArray(resultObject.get("status").getAsJsonArray());
    consumer.computed(refactoringId, problems);
  }
}
