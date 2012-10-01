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

import com.google.dart.compiler.ErrorCode;
import com.google.dart.tools.core.internal.problem.ProblemSeverities;

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
 */
public interface Problem {
  /**
   * Return the original arguments recorded into the problem.
   * 
   * @return the original arguments recorded into the problem
   */
  public String[] getArguments();

  /**
   * Return the problem id.
   * 
   * @return the problem id
   */
  public ErrorCode getID();

  /**
   * Return a localized, human-readable message string which describes the problem.
   * 
   * @return a localized, human-readable message string which describes the problem
   */
  public String getMessage();

  /**
   * Return the file name in which the problem was found.
   * 
   * @return the file name in which the problem was found
   */
  public char[] getOriginatingFileName();

  /**
   * Return the end position of the problem (inclusive), or -1 if unknown.
   * 
   * @return the end position of the problem (inclusive), or -1 if unknown
   */
  public int getSourceEnd();

  /**
   * Return the line number in source where the problem begins.
   * 
   * @return the line number in source where the problem begins
   */
  public int getSourceLineNumber();

  /**
   * Return the start position of the problem (inclusive), or -1 if unknown.
   * 
   * @return the start position of the problem (inclusive), or -1 if unknown
   */
  public int getSourceStart();

  /**
   * Checks the severity to see if the Error bit is set.
   * 
   * @return <code>true</code> if the Error bit is set for the severity
   */
  public boolean isError();

  /**
   * @return <code>true</code> if {@link ProblemSeverities#Info}
   */
  public boolean isInfo();

  /**
   * Checks the severity to see if the Error bit is not set.
   * 
   * @return <code>true</code> if the Error bit is not set for the severity
   */
  public boolean isWarning();

  /**
   * Set the end position of the problem (inclusive), or -1 if unknown. Used for shifting problem
   * positions.
   * 
   * @param sourceEnd the given end position
   */
  public void setSourceEnd(int sourceEnd);

  /**
   * Set the line number in source where the problem begins.
   * 
   * @param lineNumber the given line number
   */
  public void setSourceLineNumber(int lineNumber);

  /**
   * Set the start position of the problem (inclusive), or -1 if unknown. Used for shifting problem
   * positions.
   * 
   * @param sourceStart the given start position
   */
  public void setSourceStart(int sourceStart);
}
