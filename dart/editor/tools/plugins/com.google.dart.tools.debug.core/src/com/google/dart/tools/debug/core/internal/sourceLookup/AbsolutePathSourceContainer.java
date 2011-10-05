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
package com.google.dart.tools.debug.core.internal.sourceLookup;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

/**
 * A source path container used to locate source files given absolute paths. Not using it right now,
 * maybe be of use later
 */
public class AbsolutePathSourceContainer extends AbstractSourceContainer {
  private ISourceContainer delegateContainer;

  public AbsolutePathSourceContainer(ISourceContainer delegateContainer) {
    this.delegateContainer = delegateContainer;
  }

  @Override
  public Object[] findSourceElements(String name) throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

    String rootPath = root.getRawLocation().toOSString();

    if (name.startsWith(rootPath)) {
      name = name.substring(rootPath.length());

      IResource resource = root.findMember(name);

      if (resource.exists()) {
        return new Object[] {resource};
      } else {
        return delegateContainer.findSourceElements(name);
      }
    } else {
      return new Object[0];
    }
  }

  @Override
  public String getName() {
    return "Absolute Path";
  }

  @Override
  public ISourceContainerType getType() {
    return delegateContainer.getType();
  }

}
