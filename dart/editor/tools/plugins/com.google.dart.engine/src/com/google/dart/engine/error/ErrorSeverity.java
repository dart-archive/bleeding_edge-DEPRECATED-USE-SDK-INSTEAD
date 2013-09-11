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
 * Instances of the enumeration {@code ErrorSeverity} represent the severity of an {@link ErrorCode}
 * .
 * 
 * @coverage dart.engine.error
 */
public enum ErrorSeverity {
  /**
   * The severity representing a non-error. This is never used for any error code, but is useful for
   * clients.
   */
  NONE(" ", "none"),

  /**
   * The severity representing an informational level analysis issue.
   */
  INFO("I", "info"),

  /**
   * The severity representing a warning. Warnings can become errors if the {@code -Werror} command
   * line flag is specified.
   */
  WARNING("W", "warning"),

  /**
   * The severity representing an error.
   */
  ERROR("E", "error");

  /**
   * The name of the severity used when producing machine output.
   */
  private final String machineCode;

  /**
   * The name of the severity used when producing readable output.
   */
  private final String displayName;

  /**
   * Initialize a newly created severity with the given names.
   * 
   * @param machineCode the name of the severity used when producing machine output
   * @param displayName the name of the severity used when producing readable output
   */
  private ErrorSeverity(String machineCode, String displayName) {
    this.machineCode = machineCode;
    this.displayName = displayName;
  }

  /**
   * Return the name of the severity used when producing readable output.
   * 
   * @return the name of the severity used when producing readable output
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Return the name of the severity used when producing machine output.
   * 
   * @return the name of the severity used when producing machine output
   */
  public String getMachineCode() {
    return machineCode;
  }

  /**
   * Return the severity constant that represents the greatest severity.
   * 
   * @param severity the severity being compared against
   * @return the most sever of this or the given severity
   */
  public ErrorSeverity max(ErrorSeverity severity) {
    return this.ordinal() >= severity.ordinal() ? this : severity;
  }
}
