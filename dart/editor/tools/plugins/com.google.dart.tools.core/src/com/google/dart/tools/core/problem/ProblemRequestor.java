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
package com.google.dart.tools.core.problem;

/**
 * The interface <code>ProblemRequestor</code> defines the behavior or objects that want to receive
 * Dart compilation problem as they are discovered by some Dart operation.
 */
public interface ProblemRequestor {
  /**
   * Notification of a Dart problem.
   * 
   * @param problem the Dart problem that was discovered
   */
  public void acceptProblem(Problem problem);

  /**
   * Notification sent before starting the problem detection process. Typically, this would tell a
   * problem collector to clear previously recorded problems.
   */
  public void beginReporting();

  /**
   * Notification sent after having completed problem detection process. Typically, this would tell
   * a problem collector that no more problems should be expected in this iteration.
   */
  public void endReporting();

  /**
   * Predicate allowing the problem requestor to signal whether or not it is currently interested by
   * problem reports. When answering <code>false</code>, problem will not be discovered any more
   * until the next iteration.
   * <p>
   * This predicate will be invoked once prior to each problem detection iteration.
   * 
   * @return <code>true</code> if the requestor is currently interested by problems
   */
  public boolean isActive();
}
