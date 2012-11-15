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
package com.google.dart.engine.utilities.dart;

/**
 * The enumeration {@code ParameterKind} defines the different kinds of parameters. There are two
 * basic kinds of parameters: required and optional. Optional parameters are further divided into
 * two kinds: positional optional and named optional.
 */
public enum ParameterKind {
  REQUIRED(false),
  POSITIONAL(true),
  NAMED(true);

  /**
   * A flag indicating whether this is an optional parameter.
   */
  private boolean isOptional;

  /**
   * Initialize a newly created kind with the given state.
   * 
   * @param isOptional {@code true} if this is an optional parameter
   */
  private ParameterKind(boolean isOptional) {
    this.isOptional = isOptional;
  }

  /**
   * Return {@code true} if this is an optional parameter.
   * 
   * @return {@code true} if this is an optional parameter
   */
  public boolean isOptional() {
    return isOptional;
  }
}
