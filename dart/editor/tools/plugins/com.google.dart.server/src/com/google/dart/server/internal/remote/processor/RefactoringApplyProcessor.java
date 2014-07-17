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

import com.google.dart.server.RefactoringApplyConsumer;
import com.google.dart.server.RefactoringProblem;
import com.google.dart.server.RefactoringProblemSeverity;
import com.google.dart.server.SourceChange;
import com.google.dart.server.SourceEdit;
import com.google.dart.server.SourceFileEdit;
import com.google.dart.server.internal.RefactoringProblemImpl;
import com.google.dart.server.internal.SourceChangeImpl;
import com.google.dart.server.internal.SourceEditImpl;
import com.google.dart.server.internal.SourceFileEditImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Instances of {@code RefactoringApplyProcessor} translate JSON result objects for a given
 * {@link RefactoringApplyConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class RefactoringApplyProcessor extends ResultProcessor {

  private final RefactoringApplyConsumer consumer;

  public RefactoringApplyProcessor(RefactoringApplyConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    RefactoringProblem[] problems = constructRefactoringProblemArray(resultObject.get("status").getAsJsonArray());
    SourceChange change = constructSourceChange(resultObject.get("change").getAsJsonObject());
    consumer.computed(problems, change);
  }

  private RefactoringProblem[] constructRefactoringProblemArray(JsonArray problemsArray) {
    ArrayList<RefactoringProblem> problems = new ArrayList<RefactoringProblem>();
    Iterator<JsonElement> iter = problemsArray.iterator();
    while (iter.hasNext()) {
      JsonElement problemElement = iter.next();
      if (problemElement instanceof JsonObject) {
        JsonObject problemObject = (JsonObject) problemElement;
        problems.add(new RefactoringProblemImpl(
            RefactoringProblemSeverity.valueOf(problemObject.get("severity").getAsString()),
            problemObject.get("message").getAsString(),
            constructLocation(problemObject.get("location").getAsJsonObject())));
      }
    }
    return problems.toArray(new RefactoringProblem[problems.size()]);
  }

  private SourceChange constructSourceChange(JsonObject sourceChangeObject) {
    String message = sourceChangeObject.get("message").getAsString();
    ArrayList<SourceFileEdit> sourceFileEdits = new ArrayList<SourceFileEdit>();
    Iterator<JsonElement> iter = sourceChangeObject.get("edits").getAsJsonArray().iterator();
    while (iter.hasNext()) {
      JsonElement sourceFileEditElement = iter.next();
      if (sourceFileEditElement instanceof JsonObject) {
        sourceFileEdits.add(constructSourceFileEdit((JsonObject) sourceFileEditElement));
      }
    }
    return new SourceChangeImpl(
        message,
        sourceFileEdits.toArray(new SourceFileEdit[sourceFileEdits.size()]));
  }

  private SourceEdit constructSourceEdit(JsonObject sourceEditObject) {
    return new SourceEditImpl(sourceEditObject.get("offset").getAsInt(), sourceEditObject.get(
        "length").getAsInt(), sourceEditObject.get("replacement").getAsString());
  }

  private SourceFileEdit constructSourceFileEdit(JsonObject sourceFileEditObject) {
    String file = sourceFileEditObject.get("file").getAsString();
    ArrayList<SourceEdit> sourceEdits = new ArrayList<SourceEdit>();
    Iterator<JsonElement> iter = sourceFileEditObject.get("edits").getAsJsonArray().iterator();
    while (iter.hasNext()) {
      JsonElement sourceEditElement = iter.next();
      if (sourceEditElement instanceof JsonObject) {
        sourceEdits.add(constructSourceEdit((JsonObject) sourceEditElement));
      }
    }
    return new SourceFileEditImpl(file, sourceEdits.toArray(new SourceEdit[sourceEdits.size()]));
  }
}
