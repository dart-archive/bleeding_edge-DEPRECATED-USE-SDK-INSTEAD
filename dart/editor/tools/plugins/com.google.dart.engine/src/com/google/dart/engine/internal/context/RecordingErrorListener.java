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
import com.google.dart.engine.source.Source;

import static com.google.dart.engine.error.AnalysisError.NO_ERRORS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Instances of the class {@code RecordingErrorListener} implement an error listener that will
 * record the errors that are reported to it in a way that is appropriate for caching those errors
 * within an analysis context.
 * 
 * @coverage dart.engine
 */
public class RecordingErrorListener implements AnalysisErrorListener {

  /**
   * A HashMap of lists containing the errors that were collected, keyed by each {@link Source}.
   */
  private Map<Source, List<AnalysisError>> errors = new HashMap<Source, List<AnalysisError>>();

  /**
   * Answer the errors collected by the listener.
   * 
   * @return an array of errors (not {@code null}, contains no {@code null}s)
   */
  public AnalysisError[] getErrors() {
    Set<Entry<Source, List<AnalysisError>>> entrySet = errors.entrySet();
    if (entrySet.size() == 0) {
      return NO_ERRORS;
    }
    ArrayList<AnalysisError> resultList = new ArrayList<AnalysisError>(entrySet.size());
    for (Entry<Source, List<AnalysisError>> entry : entrySet) {
      resultList.addAll(entry.getValue());
    }
    return resultList.toArray(new AnalysisError[resultList.size()]);
  }

  /**
   * Answer the errors collected by the listener for some passed {@link Source}.
   * 
   * @param source some {@link Source} for which the caller wants the set of {@link AnalysisError}s
   *          collected by this listener
   * @return the errors collected by the listener for the passed {@link Source}
   */
  public AnalysisError[] getErrors(Source source) {
    List<AnalysisError> errorsForSource = errors.get(source);
    if (errorsForSource == null) {
      return NO_ERRORS;
    } else {
      return errorsForSource.toArray(new AnalysisError[errorsForSource.size()]);
    }
  }

  @Override
  public void onError(AnalysisError event) {
    Source source = event.getSource();
    List<AnalysisError> errorsForSource = errors.get(source);
    if (errors.get(source) == null) {
      errorsForSource = new ArrayList<AnalysisError>();
      errors.put(source, errorsForSource);
    }
    errorsForSource.add(event);
  }
}
