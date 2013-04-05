/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.error;

/**
 * The interface {@code AnalysisErrorListener} defines the behavior of objects that listen for
 * {@link AnalysisError analysis errors} being produced by the analysis engine.
 * 
 * @coverage dart.engine.error
 */
public interface AnalysisErrorListener {
  /**
   * An error listener that ignores errors that are reported to it.
   */
  AnalysisErrorListener NULL_LISTENER = new AnalysisErrorListener() {
    @Override
    public void onError(AnalysisError event) {
      // Ignore errors
    }
  };

  /**
   * This method is invoked when an error has been found by the analysis engine.
   * 
   * @param error the error that was just found (not {@code null})
   */
  public void onError(AnalysisError error);
}
