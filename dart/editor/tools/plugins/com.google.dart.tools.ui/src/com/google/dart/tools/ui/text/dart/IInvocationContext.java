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
import com.google.dart.tools.core.model.CompilationUnit;

/**
 * Context information for quick fix and quick assist processors.
 * <p>
 * Note: this interface is not intended to be implemented.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IInvocationContext {

  /**
   * Returns an AST of the compilation unit, possibly only a partial AST focused on the selection
   * offset (see {@link org.eclipse.wst.jsdt.core.dom.ASTParser#setFocalPosition(int)}). The
   * returned AST is shared and therefore protected and cannot be modified. The client must check
   * the AST API level and do nothing if they are given an AST they can't handle. (see
   * {@link org.eclipse.wst.jsdt.core.dom.AST#apiLevel()}).
   * 
   * @return Returns the root of the AST corresponding to the current compilation unit.
   */
  DartUnit getASTRoot();

  /**
   * @return Returns the current compilation unit.
   */
  CompilationUnit getCompilationUnit();

  /**
   * Convenience method to evaluate the AST node that is covered by the current selection.
   * 
   * @return Returns the node that is covered by the location of the problem
   */
  DartNode getCoveredNode();

  /**
   * Convenience method to evaluate the AST node covering the current selection.
   * 
   * @return Returns the node that covers the location of the problem
   */
  DartNode getCoveringNode();

  /**
   * @return Returns the length of the current selection
   */
  int getSelectionLength();

  /**
   * @return Returns the offset of the current selection
   */
  int getSelectionOffset();

}
