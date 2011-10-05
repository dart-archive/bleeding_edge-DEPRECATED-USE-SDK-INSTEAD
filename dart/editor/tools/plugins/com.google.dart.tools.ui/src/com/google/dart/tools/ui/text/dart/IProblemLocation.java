/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text.dart;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;

/**
 * Problem information for quick fix and quick assist processors.
 * <p>
 * Note: this interface is not intended to be implemented.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IProblemLocation {

  /**
   * Convenience method to evaluate the AST node covered by this problem.
   * 
   * @param astRoot The root node of the current AST
   * @return Returns the node that is covered by the location of the problem
   */
  DartNode getCoveredNode(DartUnit astRoot);

  /**
   * Convenience method to evaluate the AST node covering this problem.
   * 
   * @param astRoot The root node of the current AST
   * @return Returns the node that covers the location of the problem
   */
  DartNode getCoveringNode(DartUnit astRoot);

  /**
   * Returns the length of the problem.
   * 
   * @return the length of the problem
   */
  int getLength();

  /**
   * Returns the marker type of this problem.
   * 
   * @return The marker type of the problem.
   */
  String getMarkerType();

  /**
   * Returns the start offset of the problem.
   * 
   * @return the start offset of the problem
   */
  int getOffset();

  /**
   * Returns the original arguments recorded into the problem.
   * 
   * @return String[] Returns the problem arguments.
   */
  String[] getProblemArguments();

  /**
   * Returns the id of problem. Note that problem ids are defined per problem marker type. See
   * {@link org.eclipse.Problem.jsdt.core.compiler.IProblem} for id definitions for problems of type
   * <code>org.eclipse.wst.jsdt.core.problem</code> and <code>org.eclipse.wst.jsdt.core.task</code>.
   * 
   * @return The id of the problem.
   */
  int getProblemId();

  /**
   * Returns if the problem has error severity.
   * 
   * @return <code>true</code> if the problem has error severity
   */
  boolean isError();

}
