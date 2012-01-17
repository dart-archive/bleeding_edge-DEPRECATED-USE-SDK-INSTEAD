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

package com.google.dart.tools.debug.ui.internal.server;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * The tab group for the Dart Server launch config. See {@link DartServerMainTab}.
 */
public class DartServerTabGroup extends AbstractLaunchConfigurationTabGroup {

  /**
   * Create a new Dart server tab group.
   */
  public DartServerTabGroup() {

  }

  @Override
  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    setTabs(new ILaunchConfigurationTab[] {new DartServerMainTab()});
  }

}
