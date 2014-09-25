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
package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.source.DartSdkSourceContainer;
import com.google.dart.tools.debug.core.source.WorkspaceSourceContainer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

/**
 * Compute and return the default source lookup path (set of source containers that should be
 * considered) for a launch configuration.
 */
public class DartiumSourcePathComputerDelegate implements ISourcePathComputerDelegate {

  public DartiumSourcePathComputerDelegate() {

  }

  @Override
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration launchConfig,
      IProgressMonitor monitor) throws CoreException {

    return new ISourceContainer[] {
        new WorkspaceSourceContainer(), new DartSdkSourceContainer(),
        new DartiumPackageSourceContainer(launchConfig), new ChromeAppSourceContainer(),
        new DartiumRemoteScriptSourceContainer(), new DartiumUrlScriptSourceContainer()};
  }

}
