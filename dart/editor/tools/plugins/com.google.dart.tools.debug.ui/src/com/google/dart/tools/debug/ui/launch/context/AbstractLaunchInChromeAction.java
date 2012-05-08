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
package com.google.dart.tools.debug.ui.launch.context;

import com.google.dart.tools.debug.ui.internal.dartium.DartiumLaunchShortcut;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * An Abstract parent for launch in Chrome context actions
 */
public abstract class AbstractLaunchInChromeAction implements IObjectActionDelegate {

  private String mode;
  private ISelection selection;

  public AbstractLaunchInChromeAction(String mode) {
    this.mode = mode;
  }

  @Override
  public void run(IAction action) {

    DartiumLaunchShortcut launchShorcut = new DartiumLaunchShortcut();
    launchShorcut.launch(selection, mode);
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection;
  }

  @Override
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

}
