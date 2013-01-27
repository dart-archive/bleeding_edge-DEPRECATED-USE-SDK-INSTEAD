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
 * The class {@code Instrumentation} implements support for logging instrumentation information.
 * <p>
 * Instrumentation information consists of information about specific operations. Those operations
 * can range from user-facing operations, such as saving the changes to a file, to internal
 * operations, such as tokenizing source code. The information to be logged is gathered by an
 * {@link OperationBuilder operation builder}, created by one of the static methods on this class.
 * <p>
 * Note, however, that until an instrumentation logger is installed using the method
 * {@link #setLogger(InstrumentationLogger)}, all instrumentation data will be lost.
 * <p>
 * <b>Example</b>
 * <p>
 * To collect metrics about how long it took to save a file, you would write something like the
 * following:
 * 
 * <pre>
 * long startTime = System.currentTimeMillis();
 * // save the file
 * long endTime = System.currentTimeMillis();
 * metric("Save", endTime - startTime).with("chars", fileLength).log();
 * </pre>
 * The {@code metric} method creates an operation builder for an operation named {@code "Save"} that
 * took {@code endTime - startTime} milliseconds to run. The {@code with} method attaches additional
 * data to the operation; in this case recording that the file was {@code fileLength} characters
 * long. The {@code log} method tells the builder that all of the data has been collected and that
 * the resulting information should be logged.
 */
public final class Instrumentation {
  /**
   * An instrumentation logger that can be used when no other instrumentation logger has been
   * configured. This logger will silently ignore all data and logging requests.
   */
  private static final InstrumentationLogger NULL_LOGGER = new InstrumentationLogger() {
    /**
     * An operation builder that will silently ignore all data and logging requests.
     */
    private final OperationBuilder NULL_BUILDER = new OperationBuilder() {
      @Override
      public void log() {
      }

      @Override
      public OperationBuilder with(String name, AsyncValue valueGenerator) {
        return this;
      }

      @Override
      public OperationBuilder with(String name, long value) {
        return this;
      }

      @Override
      public OperationBuilder with(String name, String value) {
        return this;
      }

      @Override
      public OperationBuilder with(String name, String[] value) {
        return this;
      }
    };

    @Override
    public OperationBuilder createMetric(String name, long time) {
      return NULL_BUILDER;
    }

    @Override
    public OperationBuilder createOperation(String name, long time) {
      return NULL_BUILDER;
    }

  };

  /**
   * The current instrumentation logger.
   */
  private static InstrumentationLogger CURRENT_LOGGER = NULL_LOGGER;

  /**
   * Create an operation builder that can collect the data associated with an operation. The
   * operation is identified by the given name and is declared to contain only metrics data (data
   * that is not user identifiable and does not contain user intellectual property).
   * 
   * @param name the name used to uniquely identify the operation
   * @return the operation builder that was created
   */
  public static OperationBuilder metric(String name) {
    return CURRENT_LOGGER.createMetric(name, -1);
  }

  /**
   * Create an operation builder that can collect the data associated with an operation. The
   * operation is identified by the given name, is declared to contain only metrics data (data that
   * is not user identifiable and does not contain user intellectual property), and took the given
   * amount of time to complete.
   * 
   * @param name the name used to uniquely identify the operation
   * @param time the number of milliseconds required to perform the operation
   * @return the operation builder that was created
   */
  public static OperationBuilder metric(String name, long time) {
    return CURRENT_LOGGER.createMetric(name, time);
  }

  /**
   * Create an operation builder that can collect the data associated with an operation. The
   * operation is identified by the given name and is declared to potentially contain data that is
   * either user identifiable or contains user intellectual property (but is not guaranteed to
   * contain either).
   * 
   * @param name the name used to uniquely identify the operation
   * @return the operation builder that was created
   */
  public static OperationBuilder operation(String name) {
    return CURRENT_LOGGER.createOperation(name, -1);
  }

  /**
   * Create an operation builder that can collect the data associated with an operation. The
   * operation is identified by the given name, is declared to potentially contain data that is
   * either user identifiable or contains user intellectual property (but is not guaranteed to
   * contain either), and took the given amount of time to complete.
   * 
   * @param name the name used to uniquely identify the operation
   * @param time the number of milliseconds required to perform the operation
   * @return the operation builder that was created
   */
  public static OperationBuilder operation(String name, long time) {
    return CURRENT_LOGGER.createOperation(name, time);
  }

  /**
   * Set the logger that should receive instrumentation information to the given logger.
   * 
   * @param logger the logger that should receive instrumentation information
   */
  public static void setLogger(InstrumentationLogger logger) {
    CURRENT_LOGGER = logger == null ? NULL_LOGGER : logger;
  }

  /**
   * Prevent the creation of instances of this class
   */
  private Instrumentation() {
  }
}
