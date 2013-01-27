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
 * The interface {@code OperationBuilder} defines the behavior of objects used to collect data about
 * an operation that has occurred and record that data through an instrumentation logger.
 * <p>
 * For an example of using objects that implement this interface, see {@link Instrumentation}.
 */
public interface OperationBuilder {
  /**
   * Log the data that has been collected. The operation builder should not be used after this
   * method is invoked. The behavior of any method defined on this interface that is used after this
   * method is invoked is undefined.
   */
  public void log();

  /**
   * Lazily compute and append the given data to the data being collected by this builder.
   * 
   * @param name the name used to identify the data
   * @param a function that will be executed in the background to return the value of the data to be
   *          collected
   * @return this builder
   */
  public OperationBuilder with(String name, AsyncValue valueGenerator);

  /**
   * Append the given data to the data being collected by this builder.
   * 
   * @param name the name used to identify the data
   * @param value the value of the data to be collected
   * @return this builder
   */
  public OperationBuilder with(String name, long value);

  /**
   * Append the given data to the data being collected by this builder.
   * 
   * @param name the name used to identify the data
   * @param value the value of the data to be collected
   * @return this builder
   */
  public OperationBuilder with(String name, String value);

  /**
   * Append the given data to the data being collected by this builder.
   * 
   * @param name the name used to identify the data
   * @param value the value of the data to be collected
   * @return this builder
   */
  public OperationBuilder with(String name, String[] value);

}
