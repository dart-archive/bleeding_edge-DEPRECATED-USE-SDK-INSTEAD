/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.tools.core.utilities.general;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utilities for running scripts from the <code>scripts.properties</code> file.
 */
public class ScriptUtils {
  /**
   * @return the absolute path to the scripts file.
   */
  public static File getPropertiesFile() {
    File installDirectory = DartCore.getEclipseInstallationDirectory();
    return new File(installDirectory, "scripts.properties");
  }

  /**
   * @return the content of the scripts file.
   */
  public static Properties getScriptProperties() {
    Properties properties = new Properties();
    File file = getPropertiesFile();
    if (file.exists()) {
      try {
        properties.load(new FileReader(file));
      } catch (FileNotFoundException e) {
        DartCore.logError(e);
      } catch (IOException e) {
        DartCore.logError(e);
      }
    }
    return properties;
  }

  /**
   * Runs the specified script.
   */
  public static IStatus runScript(String scriptLocation, String fileLocation,
      IProgressMonitor monitor) {
    ProcessBuilder builder = new ProcessBuilder();
    builder.redirectErrorStream(true);
    List<String> args = new ArrayList<String>();
    args.add(scriptLocation);
    if (fileLocation != null) {
      args.add(fileLocation);
    }
    builder.command(args);

    ProcessRunner runner = new ProcessRunner(builder);

    try {
      runner.runSync(monitor);
    } catch (IOException e) {
      String message = "Failed to run script " + scriptLocation + e.toString();
      return new Status(IStatus.CANCEL, DartCore.PLUGIN_ID, message, e);
    }

    StringBuilder stringBuilder = new StringBuilder();

    if (!runner.getStdOut().isEmpty()) {
      stringBuilder.append(runner.getStdOut().trim() + "\n"); //$NON-NLS-1$
    }

    int exitCode = runner.getExitCode();

    if (exitCode != 0) {
      String output = "[" + exitCode + "] " + stringBuilder.toString();
      String message = "Failed to run script " + scriptLocation + output;
      DartCore.getConsole().print(message);
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, message);
    }

    DartCore.getConsole().print(stringBuilder.toString());
    return new Status(IStatus.OK, DartCore.PLUGIN_ID, stringBuilder.toString());
  }
}
