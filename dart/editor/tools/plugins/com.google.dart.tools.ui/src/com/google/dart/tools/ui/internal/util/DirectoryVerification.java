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
import java.net.URI;
import java.net.URL;

/**
 * A utility class to handle checking to see if a directory is legal to open in the editor.
 */
public class DirectoryVerification {

  public static IStatus getOpenDirectoryLocationStatus(File directory) {
    // disallow parent directories of the workspace
    try {
      Location workspaceLocation = Platform.getInstanceLocation();

      File workspaceDirectory = toFile(workspaceLocation.getURL());

      String dirPath = directory.getCanonicalPath();
      String workspacePath = workspaceDirectory.getCanonicalPath();

      if (workspacePath.startsWith(dirPath)) {
        return new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, "Unable to open " + directory
            + " - it contains the Editor's workspace.");
      }
    } catch (IOException e) {

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

  /**
   * Ensures the given path string starts with exactly four leading slashes.
   */
  private static String ensureUNCPath(String path) {
    int len = path.length();
    StringBuffer result = new StringBuffer(len);
    for (int i = 0; i < 4; i++) {
      //  if we have hit the first non-slash character, add another leading slash
      if (i >= len || result.length() > 0 || path.charAt(i) != '/') {
        result.append('/');
      }
    }
    result.append(path);
    return result.toString();
  }

  /**
   * Returns the URL as a local file, or <code>null</code> if the given URL does not represent a
   * local file.
   * 
   * @param url The url to return the file for
   * @return The local file corresponding to the given url, or <code>null</code>
   */
  private static File toFile(URL url) {

    if (!"file".equalsIgnoreCase(url.getProtocol())) {
      return null;
      //assume all illegal characters have been properly encoded, so use URI class to unencode
    }

    String externalForm = url.toExternalForm();
    String pathString = externalForm.substring(5);

    try {
      if (pathString.indexOf('/') == 0) {
        if (pathString.indexOf("//") == 0) {
          externalForm = "file:" + ensureUNCPath(pathString); //$NON-NLS-1$
        }
        return new File(new URI(externalForm));
      }
      if (pathString.indexOf(':') == 1) {
        return new File(new URI("file:/" + pathString)); //$NON-NLS-1$
      }

      return new File(new URI(pathString).getSchemeSpecificPart());
    } catch (Exception e) {
      //URL contains unencoded characters
      return new File(pathString);
    }
  }

  private DirectoryVerification() {

  }

}
