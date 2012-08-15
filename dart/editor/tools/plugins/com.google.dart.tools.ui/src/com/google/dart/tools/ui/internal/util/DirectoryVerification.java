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

package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * A utility class to handle checking to see if a directory is legal to open in the editor.
 */
public class DirectoryVerification {

  public static IStatus getOpenDirectoryLocationStatus(File directory) {
    // disallow parent directories of the workspace
    try {
      Location workspaceLocation = Platform.getInstanceLocation();
      File workspaceDirectory = new File(workspaceLocation.getURL().toURI());

      String dirPath = directory.getCanonicalPath();
      String workspacePath = workspaceDirectory.getCanonicalPath();

      if (workspacePath.startsWith(dirPath)) {
        return new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, "Unable to open " + directory
            + " - it contains the Editor's workspace.");
      }
    } catch (IOException e) {

    } catch (URISyntaxException e) {

    }

    // discourage user home
    File userHomeDir = new File(System.getProperty("user.home"));

    if (directory.equals(userHomeDir)) {
      return new Status(
          IStatus.WARNING,
          DartToolsPlugin.PLUGIN_ID,
          "Opening the user home directory is not recommended.");
    }

    return new Status(IStatus.OK, DartToolsPlugin.PLUGIN_ID, null);
  }

  public static boolean validateOpenDirectoryLocation(Shell shell, File directory) {
    IStatus status = getOpenDirectoryLocationStatus(directory);

    if (status.getSeverity() == IStatus.ERROR) {
      MessageDialog.openError(shell, "Unable to Open Directory", status.getMessage());

      return false;
    } else if (status.getSeverity() == IStatus.WARNING) {
      return MessageDialog.openConfirm(shell, "Confirm Open Directory", status.getMessage()
          + "\nAre you sure you want to open this directory?");
    }

    return status.isOK();
  }

  private DirectoryVerification() {

  }

}
