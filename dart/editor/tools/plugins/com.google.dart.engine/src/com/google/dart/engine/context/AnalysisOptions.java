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
package com.google.dart.engine.context;

/**
 * The interface {@code AnalysisOptions} defines the behavior of objects that provide access to a
 * set of analysis options used to control the behavior of an analysis context.
 */
public interface AnalysisOptions {
  /**
   * Return {@code true} if analysis is to generate dart2js related hint results.
   * 
   * @return {@code true} if analysis is to generate dart2js related hint results
   */
  public boolean getDart2jsHint();

  /**
   * Return {@code true} if analysis is to generate hint results (e.g. type inference based
   * information and pub best practices).
   * 
   * @return {@code true} if analysis is to generate hint results
   */
  public boolean getHint();

  /**
   * Return {@code true} if analysis is to use strict mode. In strict mode, error reporting is based
   * exclusively on the static type information.
   * 
   * @return {@code true} if analysis is to use strict mode
   */
  public boolean getStrictMode();
}
