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
 * The severity of an {@link ErrorCode}.
 */
public enum ErrorSeverity {
  /**
   * The severity representing an error.
   */
  ERROR("E"),

  /**
   * The severity representing a warning. Warnings can become errors if the <code>-Werror</code>
   * command line flag is specified.
   */
  WARNING("W");

  final String name;

  ErrorSeverity(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
