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
package com.google.dart.tools.debug.ui.internal.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut;

/**
 * A launch shortcut with a better notion of which resource it can launch and which launch
 * configurations are associated with it.
 */
public interface ILaunchShortcutExt extends ILaunchShortcut {

  /**
   * @return whether this launch shortcut can launch the given resource
   */
  public boolean canLaunch(IResource resource);

  /**
   * @return the launch configurations associated with this launch shortcut
   */
  public ILaunchConfiguration[] getAssociatedLaunchConfigurations(IResource resource);

}
