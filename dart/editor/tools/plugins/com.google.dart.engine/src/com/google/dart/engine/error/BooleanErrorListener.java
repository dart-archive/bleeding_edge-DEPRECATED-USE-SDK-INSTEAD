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
package com.google.dart.engine.error;

/**
 * Instances of the class {@code BooleanErrorListener} implement a listener that keeps track of
 * whether an error has been reported to it.
 */
public class BooleanErrorListener implements AnalysisErrorListener {
  /**
   * A flag indicating whether an error has been reported to this listener.
   */
  private boolean errorReported = false;

  /**
   * Initialize a newly created error listener.
   */
  public BooleanErrorListener() {
    super();
  }

  /**
   * Return {@code true} if an error has been reported to this listener.
   * 
   * @return {@code true} if an error has been reported to this listener
   */
  public boolean getErrorReported() {
    return errorReported;
  }

  @Override
  public void onError(AnalysisError error) {
    errorReported = true;
  }
}
