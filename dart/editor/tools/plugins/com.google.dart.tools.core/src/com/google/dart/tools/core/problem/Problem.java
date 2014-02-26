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
package com.google.dart.tools.core.problem;

/**
 * The interface <code>Problem</code> defines the behavior of objects that represent a Dart problem,
 * as detected by the compiler or some of the underlying technology reusing the compiler. A problem
 * provides access to:
 * <ul>
 * <li>its location (originating source file name, source position, line number),</li>
 * <li>its message description and a predicate to check its severity (warning or error).</li>
 * <li>its ID : a number identifying the very nature of this problem. All possible IDs are listed as
 * constants on this interface.</li>
 * </ul>
 * Note: the compiler produces Problems internally, which are turned into markers by the DartBuilder
 * so as to persist problem descriptions. This explains why there is no API allowing to reach
 * Problem detected when compiling. However, the Dart problem markers carry equivalent information
 * to Problem, in particular their ID (attribute "id") is set to one of the IDs defined on this
 * interface.
 * 
 * @coverage dart.tools.core.problem
 */
public interface Problem {
  /**
   * Return the original arguments recorded into the problem.
   * 
   * @return the original arguments recorded into the problem
   */
  public String[] getArguments();

  /**
   * Return a localized, human-readable message string which describes the problem.
   * 
   * @return a localized, human-readable message string which describes the problem
   */
  public String getMessage();
}
