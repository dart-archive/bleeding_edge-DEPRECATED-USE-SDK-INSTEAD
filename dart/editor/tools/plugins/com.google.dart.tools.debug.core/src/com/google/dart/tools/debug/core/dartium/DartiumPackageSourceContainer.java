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
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

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
    if (!name.startsWith("package:")) {
      return EMPTY;
    }

    if (wrapper.getProject() != null) {
      IFile file = DartCore.getProjectManager().resolvePackageUri(wrapper.getProject(), name);

      if (file != null) {
        return new Object[] {file};
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
