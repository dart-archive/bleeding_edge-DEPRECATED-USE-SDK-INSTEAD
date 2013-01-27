/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;

import static com.google.dart.engine.error.AnalysisError.NO_ERRORS;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class {@code RecordingErrorListener} implement an error listener that will
 * record the errors that are reported to it in a way that is appropriate for caching those errors
 * within an analysis context.
 */
public class RecordingErrorListener implements AnalysisErrorListener {

  /**
   * A list containing the errors that were collected.
   */
  private List<AnalysisError> errors = null;

  /**
   * Answer the errors collected by the listener.
   * 
   * @return an array of errors (not {@code null}, contains no {@code null}s)
   */
  public AnalysisError[] getErrors() {
    return errors != null ? errors.toArray(new AnalysisError[errors.size()]) : NO_ERRORS;
  }

  @Override
  public void onError(AnalysisError event) {
    if (errors == null) {
      errors = new ArrayList<AnalysisError>();
    }
    errors.add(event);
  }
}
