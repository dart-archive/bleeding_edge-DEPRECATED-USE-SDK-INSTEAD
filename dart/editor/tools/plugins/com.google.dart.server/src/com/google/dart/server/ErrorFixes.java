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
package com.google.dart.server;

/**
 * The interface {@code ErrorFixes} defines the behavior of objects representing fixes to some
 * error.
 * 
 * @coverage dart.server
 */
public interface ErrorFixes {
  /**
   * An empty array of errors used when no errors are expected.
   */
  public static final ErrorFixes[] EMPTY_ARRAY = new ErrorFixes[0];

  /**
   * The error with which the fixes are associated.
   * 
   * @return the error with which the fixes are associated
   */
  public AnalysisError getError();

  /**
   * The fixes associated with the error.
   * 
   * @return the fixes associated with the error
   */
  public SourceChange[] getFixes();
}
