/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.formatter;

/**
 * Exception thrown by a scanner when encountering lexical errors.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class InvalidInputException extends Exception {
  private static final long serialVersionUID = 2909732853499731592L;

  /**
   * Creates a new exception with no detail message.
   */
  public InvalidInputException() {
    super();
  }

  /**
   * Creates a new exception with the given detail message.
   * 
   * @param message the detail message
   */
  public InvalidInputException(String message) {
    super(message);
  }
}
