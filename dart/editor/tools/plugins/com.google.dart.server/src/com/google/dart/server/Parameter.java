/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code Parameter} defines the behavior of objects representing a parameter.
 * 
 * @coverage dart.server
 */
public interface Parameter {
  /**
   * An empty array of parameters.
   */
  public static final Parameter[] EMPTY_ARRAY = new Parameter[0];

  /**
   * The type that should be given to the parameter.
   * 
   * @return the type that should be given to the parameter
   */
  public String getName();

  /**
   * The name that should be given to the parameter.
   * 
   * @return the name that should be given to the parameter
   */
  public String getType();

}
