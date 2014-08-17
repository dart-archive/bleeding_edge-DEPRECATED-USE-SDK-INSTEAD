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
package com.google.dart.tools.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;

/**
 * Interface for accessing working copies of <code>CompilationUnit</code> objects. The original
 * compilation unit is only given indirectly by means of an <code>IEditorInput</code>. The life
 * cycle is as follows:
 * <ul>
 * <li> <code>connect</code> creates and remembers a working copy of the compilation unit which is
 * encoded in the given editor input</li>
 * <li> <code>getWorkingCopy</code> returns the working copy remembered on <code>connect</code></li>
 * <li> <code>disconnect</code> destroys the working copy remembered on <code>connect</code></li>
 * </ul>
 * <p>
 * In order to provide backward compatibility for clients of <code>IWorkingCopyManager</code>,
 * extension interfaces are used to provide a means of evolution. The following extension interfaces
 * exist:
 * <ul>
 * </ul>
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 * 
 * @see DartUI#getWorkingCopyManager()
 */
public interface IWorkingCopyManager {

  /**
   * Connects the given editor input to this manager. After calling this method, a working copy will
   * be available for the compilation unit encoded in the given editor input (does nothing if there
   * is no encoded compilation unit).
   * 
   * @param input the editor input
   * @exception CoreException if the working copy cannot be created for the compilation unit
   */
  void connect(IEditorInput input) throws CoreException;

  /**
   * Disconnects the given editor input from this manager. After calling this method, a working copy
   * for the compilation unit encoded in the given editor input will no longer be available. Does
   * nothing if there is no encoded compilation unit, or if there is no remembered working copy for
   * the compilation unit.
   * 
   * @param input the editor input
   */
  void disconnect(IEditorInput input);

  /**
   * Shuts down this working copy manager. All working copies still remembered by this manager are
   * destroyed.
   */
  void shutdown();
}
