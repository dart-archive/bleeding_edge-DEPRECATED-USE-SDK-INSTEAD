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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.ui.IEditorInput;

/**
 * Extension interface for {@link IWorkingCopyManager}.
 * <p>
 * Introduces API to set and remove the working copy for a given editor input.
 * <p>
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public interface IWorkingCopyManagerExtension {

  /**
   * Removes the working copy set for the given editor input. If there is no working copy set for
   * this input or this input is not connected to this working copy manager, this call has no
   * effect.
   * 
   * @param input the editor input
   */
  void removeWorkingCopy(IEditorInput input);

  /**
   * Sets the given working copy for the given editor input. If the given editor input is not
   * connected to this working copy manager, this call has no effect.
   * <p>
   * This working copy manager does not assume the ownership of this working copy, i.e., the given
   * working copy is not automatically be freed when this manager is shut down.
   * 
   * @param input the editor input
   * @param workingCopy the working copy
   */
  void setWorkingCopy(IEditorInput input, CompilationUnit workingCopy);
}
