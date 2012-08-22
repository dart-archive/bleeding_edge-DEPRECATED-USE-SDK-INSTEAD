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
package com.google.dart.eclipse.ui.internal.jobs;

import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * A job that verifies that a current SDK is installed (and takes appropriate action).
 */
public class ValidateSDKJob extends Job {

  public ValidateSDKJob() {
    super(UIJobMessages.ValidateSDKJob_name);
    setSystem(true);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {

    DartSdk sdk = DartSdkManager.getManager().getSdk();

    if (sdk == null) {
      handeMissingSDK();
    }

    return null;
  }

  private String getEclipseHome() {
    return Platform.getInstallLocation().getURL().getPath();
  }

  private void handeMissingSDK() {

    //TODO(pquitslund): replace with an action to kick off download/install of the SDK

    Display.getDefault().asyncExec(new Runnable() {

      @Override
      public void run() {
        //TODO(pquitslund): add correct SDK download link
        //TODO(pquitslund): migrate to a control that supports hyperlinks/selectable text
        MessageDialog.openWarning(
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            UIJobMessages.ValidateSDKJob_missing_sdk_popup_title,
            NLS.bind(UIJobMessages.ValidateSDKJob_missing_sdk_desc, getEclipseHome()));
      }

    });
  }

}
