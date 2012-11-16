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
package com.google.dart.engine.formatter;

import com.google.dart.engine.error.AnalysisError;

import java.util.List;

//TODO (pquitslund): document typical sources of exceptions

/**
 * Thrown when an error occurs in formatting.
 */
public class FormatterException extends Exception {

  /**
   * Create an exception describing the given analysis error(s).
   * 
   * @param errors the errors triggering the exception
   * @return a FormatterException describing the given error cause.
   */
  public static FormatterException forError(List<AnalysisError> errors) {
    //TODO (pquitslund): consider adding description details
    return new FormatterException("an analysis error occured during format");
  }

  /**
   * Constructs a new exception with the specified detail message.
   * 
   * @param msg the detail message. The detail message is saved for later retrieval by the
   *          {@link #getMessage()} method.
   */
  public FormatterException(String msg) {
    super(msg);
  }

  /**
   * Constructs a new exception with the specified detail message.
   * 
   * @param msg the detail message. The detail message is saved for later retrieval by the
   *          {@link #getMessage()} method.
   * @param cause the cause (which is saved for later retrieval by the Throwable.getCause() method).
   *          (A {@code null} value is permitted, and indicates that the cause is nonexistent or
   *          unknown.)
   */
  public FormatterException(String msg, Exception cause) {
    super(msg, cause);
  }
}
