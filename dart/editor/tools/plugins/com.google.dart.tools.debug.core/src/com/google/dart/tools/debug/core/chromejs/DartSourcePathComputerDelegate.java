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
package com.google.dart.tools.debug.core.chromejs;

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Compute and return the default source lookup path (set of source containers that should be
 * considered) for a launch configuration.
 */
@Deprecated
public class DartSourcePathComputerDelegate implements ISourcePathComputerDelegate {

  public DartSourcePathComputerDelegate() {

  }

  @Override
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration launchConfig,
      IProgressMonitor monitor) throws CoreException {

    List<ISourceContainer> containers = new ArrayList<ISourceContainer>();

    SystemLibraryManager libraryManager = SystemLibraryManagerProvider.getSystemLibraryManager();

    URI domUri = libraryManager.resolveDartUri(URI.create("dart://dom/dart_dom.lib"));
    IPath path = new Path(domUri.getPath()).removeLastSegments(1);
    DirectorySourceContainer domContainer = new DirectorySourceContainer(path, true);
    containers.add(domContainer);

    URI coreUri = libraryManager.resolveDartUri(URI.create("dart://core/corelib.lib"));
    IPath coreUriPath = new Path(coreUri.getPath()).removeLastSegments(1);
    DirectorySourceContainer coreContainer = new DirectorySourceContainer(coreUriPath, true);
    containers.add(coreContainer);

    URI coreImplUri = libraryManager.resolveDartUri(URI.create("dart://core/corelib_impl.lib"));
    IPath coreImplPath = new Path(coreImplUri.getPath()).removeLastSegments(1);
    DirectorySourceContainer coreImplContainer = new DirectorySourceContainer(coreImplPath, true);
    containers.add(coreImplContainer);

    DartLaunchConfigWrapper launchWraper = new DartLaunchConfigWrapper(launchConfig);

    DartProject dartProject = DartCore.create(launchWraper.getProject());

    WorkspaceSourceContainer workspaceSourceContainer = new WorkspaceSourceContainer();

    if (dartProject != null) {
      ProjectSourceContainer projectSourceContainer = new ProjectSourceContainer(
          dartProject.getProject(),
          false);
      containers.add(projectSourceContainer);
    } else {
      containers.add(workspaceSourceContainer);
    }
    return containers.toArray(new ISourceContainer[containers.size()]);
  }

}
