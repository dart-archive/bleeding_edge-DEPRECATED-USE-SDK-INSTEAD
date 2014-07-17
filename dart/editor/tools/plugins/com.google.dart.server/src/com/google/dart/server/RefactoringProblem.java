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
 * The interface {@code RefactoringProblem} defines the behavior of objects that represent a
 * refactoring problem.
 * 
 * @coverage dart.server
 */
public interface RefactoringProblem {

  /**
   * An empty array of refactoring problems.
   */
  public final RefactoringProblem[] EMPTY_ARRAY = new RefactoringProblem[0];

  /**
   * The location of the problem being represented.
   * 
   * @return the location of the problem being represented
   */
  public Location getLocation();

  /**
   * A textual description of the problem being represented.
   * 
   * @return a textual description of the problem being represented
   */
  public String getMessage();

  /**
   * The severity of the problem being represented.
   * 
   * @return the severity of the problem being represented
   */
  public RefactoringProblemSeverity getSeverity();

}
