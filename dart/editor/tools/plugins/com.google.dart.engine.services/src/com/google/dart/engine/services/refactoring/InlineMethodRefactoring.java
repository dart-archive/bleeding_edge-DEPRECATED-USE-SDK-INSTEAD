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

package com.google.dart.engine.services.refactoring;

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.MethodElement;

/**
 * {@link Refactoring} to inline {@link MethodElement} or {@link FunctionElement}.
 */
public interface InlineMethodRefactoring extends Refactoring {
  public enum Mode {
    INLINE_ALL,
    INLINE_SINGLE;
  }

  /**
   * @return <code>true</code> if it is possible to inline all references and remove declaration.
   */
  boolean canDeleteSource();

  /**
   * @return the {@link ExecutableElement} to inline.
   */
  ExecutableElement getElement();

  /**
   * @return the initial inlining {@link Mode}.
   */
  Mode getInitialMode();

  /**
   * Specifies current inlining {@link Mode}.
   */
  void setCurrentMode(Mode currentMode);

  /**
   * Specifies if inlined element should be removed. Only if {@link Mode#INLINE_ALL}.
   */
  void setDeleteSource(boolean delete);
}
