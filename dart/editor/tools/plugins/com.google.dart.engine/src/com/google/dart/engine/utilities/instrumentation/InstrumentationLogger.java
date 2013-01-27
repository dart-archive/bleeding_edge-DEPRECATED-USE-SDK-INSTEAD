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
package com.google.dart.engine.utilities.instrumentation;

/**
 * The interface {@code InstrumentationLogger} defines the behavior of objects that are used to log
 * instrumentation data.
 * <p>
 * For an example of using objects that implement this interface, see {@link Instrumentation}.
 */
public interface InstrumentationLogger {
  /**
   * Create an operation builder that can collect the data associated with an operation. The
   * operation is identified by the given name, is declared to contain only metrics data (data that
   * is not user identifiable and does not contain user intellectual property), and took the given
   * amount of time to complete.
   * 
   * @param name the name used to uniquely identify the operation
   * @param time the number of milliseconds required to perform the operation, or {@code -1} if the
   *          time is not available or not applicable to this kind of operation
   * @return the operation builder that was created
   */
  public OperationBuilder createMetric(String name, long time);

  /**
   * Create an operation builder that can collect the data associated with an operation. The
   * operation is identified by the given name, is declared to potentially contain data that is
   * either user identifiable or contains user intellectual property (but is not guaranteed to
   * contain either), and took the given amount of time to complete.
   * 
   * @param name the name used to uniquely identify the operation
   * @param time the number of milliseconds required to perform the operation, or {@code -1} if the
   *          time is not available or not applicable to this kind of operation
   * @return the operation builder that was created
   */
  public OperationBuilder createOperation(String name, long time);

}
