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
package com.google.dart.tools.core.generator;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.io.FileUtilities;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGenerator {

  protected final static IWorkspace workspace = ResourcesPlugin.getWorkspace();

  /**
   * If the passed file name does not have an extension, return the file name appended with given
   * extension.
   */
  public static String appendIfNoExtension(String fileName, String extension) {
    if (fileName == null || fileName.isEmpty()) {
      return fileName;
    }
    int indexOfPeriod = fileName.indexOf('.');
    if (indexOfPeriod == -1) {
      return fileName + extension;
    }
    if (indexOfPeriod == fileName.length() - 1) {
      return fileName + extension.substring(1);
    }
    return fileName;
  }

  protected static boolean containsWhitespace(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (Character.isWhitespace(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  public abstract void execute(IProgressMonitor monitor) throws CoreException;

  public abstract IStatus validate();

  protected void execute(final String contentPath, final File file,
      final HashMap<String, String> substitutions, IProgressMonitor monitor) throws CoreException {

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        monitor.beginTask("", 2); //$NON-NLS-1$
        String source;
        try {
          source = readExpectedContent(contentPath, substitutions);
        } catch (IOException e) {
          throw new CoreException(new Status(
              IStatus.ERROR,
              DartCore.PLUGIN_ID,
              "Failed to generate source",
              e));
        }
        try {
          File parentFile = file.getParentFile();

          if (!parentFile.exists()) {
            FileUtilities.createDirectory(parentFile);
          }

          FileUtilities.setContents(file, source);
        } catch (IOException e) {
          DartCore.logError(e);
        }
//        if (!file.getParent().exists()) {
//          IPath path = file.getFullPath().removeLastSegments(1);
//          IFolder folder = workspace.getRoot().getFolder(path);
//          folder.create(false, true, new SubProgressMonitor(monitor, 1));
//        }
//        InputStream stream = new ByteArrayInputStream(source.getBytes());
//        file.create(stream, false, new SubProgressMonitor(monitor, 1));
//        monitor.done();
      }

    }, monitor);
  }

  protected void execute(final String contentPath, final File file, IProgressMonitor monitor)
      throws CoreException {
    execute(contentPath, file, new HashMap<String, String>(), monitor);
  }

  /**
   * Read content from the specified file while performing the specified text substitutions.
   * Anyplace in the raw file content where %key% appears will be replaced by a value from the
   * substitutions map. All instances of %% will be replaced by %.
   * 
   * @param fileName the path to the file to be read relative to the class
   * @param substitutions a mapping of keys that may appear in the raw content to values that should
   *          be substituted.
   * @return the file content after substitution has been performed
   */
  protected String readExpectedContent(String fileName, Map<String, String> substitutions)
      throws IOException {

    // Read content from the specified file

    InputStream stream = getClass().getResourceAsStream(fileName);
    StringBuilder result = new StringBuilder(2000);
    try {
      InputStreamReader reader = new InputStreamReader(stream);
      while (true) {
        int ch = reader.read();
        if (ch == -1) {
          break;
        }
        if (ch != '%' || substitutions == null) {
          result.append((char) ch);
          continue;
        }

        // If % is detected, the extract the key and perform a substitution

        StringBuilder key = new StringBuilder(20);
        while (true) {
          ch = reader.read();
          if (ch == -1) {
            throw new RuntimeException("Expected '%' but found EOF in " + fileName);
          }
          if (ch == '%') {
            break;
          }
          key.append((char) ch);
        }

        // If %% is detected, then substitute %
        // Otherwise lookup the value in the substitutions map

        if (key.length() == 0) {
          result.append("%"); //$NON-NLS-1$
        } else {
          String value = substitutions.get(key.toString());
          if (value == null) {
            throw new RuntimeException("Failed to find value for key " + key + " in " + fileName);
          }
          result.append(value);
        }
      }
    } finally {
      stream.close();
    }
    return result.toString();
  }

}
