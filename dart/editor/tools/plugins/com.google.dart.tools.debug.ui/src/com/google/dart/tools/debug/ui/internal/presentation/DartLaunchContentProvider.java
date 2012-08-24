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

package com.google.dart.tools.debug.ui.internal.presentation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.internal.ui.model.elements.ElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * This custom ElementContentProvider for ILaunches only returns the DebugTargets, not the child
 * Processes. This makes for a cleaner DebuggerView.
 */
@SuppressWarnings("restriction")
public class DartLaunchContentProvider extends ElementContentProvider {

  public DartLaunchContentProvider() {

  }

  @Override
  protected int getChildCount(Object element, IPresentationContext context, IViewerUpdate monitor)
      throws CoreException {
    return getLaunchChildren((ILaunch) element).length;
  }

  @Override
  protected Object[] getChildren(Object parent, int index, int length,
      IPresentationContext context, IViewerUpdate monitor) throws CoreException {
    return getElements(getLaunchChildren((ILaunch) parent), index, length);
  }

  @Override
  protected boolean hasChildren(Object element, IPresentationContext context, IViewerUpdate monitor)
      throws CoreException {
    return getLaunchChildren((ILaunch) element).length > 0;
  }

  @Override
  protected boolean supportsContextId(String id) {
    return IDebugUIConstants.ID_DEBUG_VIEW.equals(id);
  }

  private Object[] getLaunchChildren(ILaunch launch) throws CoreException {
    return launch.getDebugTargets();

    // TODO(devoncarew): while this behavior is what we want, a selection provider is hard-coded
    // to assume a normal debug object hierarchy. We lose the selection focus on pause that we
    // want.

//    IDebugTarget debugTarget = launch.getDebugTarget();
//
//    if (debugTarget != null) {
//      return debugTarget.getThreads();
//    } else {
//      return new Object[0];
//    }
  }

}
