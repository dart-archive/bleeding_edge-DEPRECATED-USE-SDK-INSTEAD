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

package com.google.dart.tools.debug.core.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * A ILaunchConfigurationDelegate that can connect to remote debug agents.
 */
public interface IRemoteConnectionDelegate extends ILaunchConfigurationDelegate {
  /**
   * Open a debug connection to a remote host.
   * 
   * @param host
   * @param port
   * @param container
   * @param monitor
   * @param usePubServe
   * @throws CoreException
   */
  public IDebugTarget performRemoteConnection(String host, int port, IContainer container,
      IProgressMonitor monitor, boolean usePubServe) throws CoreException;

}
