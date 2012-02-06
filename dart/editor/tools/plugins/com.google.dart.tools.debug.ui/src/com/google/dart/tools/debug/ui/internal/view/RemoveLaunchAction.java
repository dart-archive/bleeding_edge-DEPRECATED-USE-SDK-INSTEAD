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

package com.google.dart.tools.debug.ui.internal.view;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * An action to remove terminated launches from the DebuggerView.
 */
@SuppressWarnings("restriction")
public class RemoveLaunchAction extends Action implements IDebugContextListener {
  private IDebugContextService contextService;

  public RemoveLaunchAction(IDebugContextService contextService) {
    super("Remove Launch");

    this.contextService = contextService;

    setToolTipText("Remove Launch");

    setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE));
    setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE));
    setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_REMOVE));

    contextService.addDebugContextListener(this);

    setEnabled(false);
  }

  @Override
  public void debugContextChanged(DebugContextEvent event) {
    update(event.getContext());
  }

  public void dispose() {
    contextService.removeDebugContextListener(this);
  }

  @Override
  public synchronized void run() {
    ILaunch terminatable = getTerminatable();

    if (terminatable != null) {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

      launchManager.removeLaunch(terminatable);
    }
  }

  private ILaunch getTerminatable() {
    return getTerminatable(contextService.getActiveContext());
  }

  private ILaunch getTerminatable(ISelection sel) {
    if (sel instanceof IStructuredSelection) {
      Object obj = ((IStructuredSelection) sel).getFirstElement();

      if (obj instanceof ILaunch) {
        return (ILaunch) obj;
      } else if (obj instanceof IAdaptable) {
        return (ILaunch) ((IAdaptable) obj).getAdapter(ILaunch.class);
      }
    }

    return null;
  }

  private void update(ISelection sel) {
    ILaunch terminatable = getTerminatable(sel);

    if (terminatable != null) {
      setEnabled(terminatable.isTerminated());
    } else {
      setEnabled(false);
    }
  }

}
