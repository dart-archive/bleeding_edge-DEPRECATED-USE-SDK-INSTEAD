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
package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.analysis.model.IFileInfo;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

/**
 * A source container for Dartium that resolves package: urls to resources or files
 */
public class DartiumPackageSourceContainer extends AbstractSourceContainer {

  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.file"; //$NON-NLS-1$

  DartLaunchConfigWrapper wrapper;

  public DartiumPackageSourceContainer(ILaunchConfiguration launchConfig) {
    wrapper = new DartLaunchConfigWrapper(launchConfig);
  }

  @Override
  public Object[] findSourceElements(String name) throws CoreException {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      // This class is not used with the Analysis Server.
      return EMPTY;
    }

    if (!name.startsWith("package:")) {
      return EMPTY;
    }

    IContainer parent = wrapper.getProject();

    if (wrapper.getApplicationResource() != null) {
      parent = wrapper.getApplicationResource().getParent();
    }

    if (parent != null) {
      IFileInfo fileInfo = DartCore.getProjectManager().resolveUriToFileInfo(parent, name);

      if (fileInfo != null) {
        // check to see if there is another project or same project/subfolder with a different
        // resource for the file
        // resource : debug_sample/coolApp/packages/coolLib/cool_lib.dart
        //      => resource : debug_sample/coolLib/lib/cool_lib.dart
        // resource : coolApp/packages/coolLib/cool_lib.dart
        //      => resource : coolLib/lib/cool_lib.dart
        String filePath = fileInfo.getFile().getAbsolutePath();
        for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
          String projectLocation = project.getLocation().toString();

          if (filePath.startsWith(projectLocation)) {
            // /mydir/myproject/lib/lib.dart => lib/lib.dart
            String path = filePath.substring(projectLocation.length() + 1);
            IResource resource = project.findMember(path);
            if (resource != null && !resource.equals(fileInfo.getResource())) {
              return new Object[] {resource};
            }
          }
        }
        if (fileInfo.getResource() != null) {
          return new Object[] {fileInfo.getResource()};
        } else {
          return new Object[] {new LocalFileStorage(fileInfo.getFile())};
        }
      }
    }

    return EMPTY;
  }

  @Override
  public String getName() {
    return "Package sources";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }

}
