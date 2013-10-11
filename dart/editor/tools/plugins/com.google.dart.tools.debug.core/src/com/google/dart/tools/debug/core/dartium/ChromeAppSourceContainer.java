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

import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

/**
 * An ISourceContainer implementation used to locate Chrome app and extension sources.
 */
public class ChromeAppSourceContainer extends AbstractSourceContainer {
  public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier()
      + ".containerType.workspace";

  private static final String CHROME_PREFIX = "chrome-extension://";

  public ChromeAppSourceContainer() {

  }

  @Override
  public Object[] findSourceElements(String name) throws CoreException {
    if (name == null) {
      return EMPTY;
    }

    // chrome-extension://ciinnplnpkjlnbjklingpjkakiapmmpm/foo/bar/baz.dart
    if (!name.startsWith(CHROME_PREFIX)) {
      return EMPTY;
    }

    String path = name.substring(CHROME_PREFIX.length());

    if (path.indexOf('/') != -1) {
      path = path.substring(path.indexOf('/') + 1);
    }

    ILaunchConfiguration launch = getDirector().getLaunchConfiguration();
    DartLaunchConfigWrapper wrapper = new DartLaunchConfigWrapper(launch);

    IResource launchResource = wrapper.getApplicationResource();

    if (launchResource != null) {
      IContainer parent = launchResource.getParent();
      IResource target = parent.findMember(path);

      if (target != null) {
        return new Object[] {target};
      }
    }

    return EMPTY;
  }

  @Override
  public String getName() {
    return "Chrome extension sources";
  }

  @Override
  public ISourceContainerType getType() {
    return getSourceContainerType(TYPE_ID);
  }

}
