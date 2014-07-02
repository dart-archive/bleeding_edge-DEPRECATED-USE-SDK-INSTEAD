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
package com.google.dart.server.error;

import com.google.dart.server.AnalysisError;

/**
 * The interface {@code ErrorCode} defines the behavior common to objects representing error codes
 * associated with {@link AnalysisError analysis errors}.
 * <p>
 * Generally, we want to provide messages that consist of three sentences: 1. what is wrong, 2. why
 * is it wrong, and 3. how do I fix it. However, we combine the first two in the result of
 * {@link #getMessage()} and the last in the result of {@link #getCorrection()}.
 * 
 * @coverage dart.server.error
 */
public interface ErrorCode {

  /**
   * Return a unique name for this error code.
   * 
   * @return a unique name for this error code
   */
  public String getUniqueName();
}
