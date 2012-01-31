/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.ui.internal.dialogs;

import com.google.dart.tools.debug.ui.internal.DartUtil;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * A content provider for launch configurations.
 */
public class LaunchConfigContentProvider implements IStructuredContentProvider {

  public LaunchConfigContentProvider() {

  }

  @Override
  public void dispose() {

  }

  @Override
  public Object[] getElements(Object inputElement) {
    try {
      return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
    } catch (CoreException exception) {
      DartUtil.logError(exception);

      return new Object[0];
    }
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

  }

}
