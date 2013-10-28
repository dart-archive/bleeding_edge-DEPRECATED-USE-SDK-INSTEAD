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
package com.google.dart.engine.parser;

/**
 * Instances of the class {@code IncrementalParseException} represent an exception that occurred
 * while attempting to parse a replacement for a specified node in an existing AST structure.
 */
public class IncrementalParseException extends RuntimeException {
  /**
   * Initialize a newly created exception to have no message and to be its own cause.
   */
  public IncrementalParseException() {
    super();
  }

  /**
   * Initialize a newly created exception to have the given message and to be its own cause.
   * 
   * @param message the message describing the reason for the exception
   */
  public IncrementalParseException(String message) {
    super(message);
  }

  /**
   * Initialize a newly created exception to have no message and to have the given cause.
   * 
   * @param cause the exception that caused this exception
   */
  public IncrementalParseException(Throwable cause) {
    super(cause);
  }
}
