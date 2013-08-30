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
 * The interface {@code ErrorCode} defines the behavior common to objects representing error codes
 * associated with {@link AnalysisError analysis errors}.
 * <p>
 * Generally, we want to provide messages that consist of three sentences: 1. what is wrong, 2. why
 * is it wrong, and 3. how do I fix it. However, we combine the first two in the result of
 * {@link #getMessage()} and the last in the result of {@link #getCorrection()}.
 * 
 * @coverage dart.engine.error
 */
public interface ErrorCode {
  /**
   * Return the template used to create the correction to be displayed for this error, or
   * {@code null} if there is no correction information for this error. The correction should
   * indicate how the user can fix the error.
   * 
   * @return the template used to create the correction to be displayed for this error
   */
  public String getCorrection();

  /**
   * Return the severity of this error.
   * 
   * @return the severity of this error
   */
  public ErrorSeverity getErrorSeverity();

  /**
   * Return the template used to create the message to be displayed for this error. The message
   * should indicate what is wrong and why it is wrong.
   * 
   * @return the template used to create the message to be displayed for this error
   */
  public String getMessage();

  /**
   * Return the type of the error.
   * 
   * @return the type of the error
   */
  public ErrorType getType();
}
