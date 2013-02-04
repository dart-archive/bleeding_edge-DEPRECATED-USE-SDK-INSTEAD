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

package com.google.dart.engine.services.assist;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.source.Source;

/**
 * Context for which corrections should be provided.
 */
public interface AssistContext {
  /**
   * @return the resolved {@link CompilationUnit} of the {@link Source}.
   */
  CompilationUnit getCompilationUnit();

  /**
   * @return the {@link ASTNode} that is covered by the selection.
   */
  ASTNode getCoveredNode();

  /**
   * @return the ASTNode that covers the selection.
   */
  ASTNode getCoveringNode();

  /**
   * @return the length of the selection.
   */
  int getSelectionLength();

  /**
   * @return the offset of the selection.
   */
  int getSelectionOffset();

  /**
   * @return the {@link Source} to provide corrections in.
   */
  Source getSource();
}
