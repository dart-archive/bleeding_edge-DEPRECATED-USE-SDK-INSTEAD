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
package com.google.dart.tools.ui.swtbot.util;

import com.google.dart.compiler.util.apache.FileUtils;
import com.google.dart.tools.ui.swtbot.DartLib;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AntRunner {

  /**
   * Answer a runner for the specified target in the build_rcp.xml file
   */
  public static AntRunner buildTarget(String target) {
    AntRunner runner = new AntRunner();
    runner.target = target;
    return runner;
  }

  private String target;
  private Map<String, String> properties;

  private AntRunner() {
  }

  /**
   * Execute the Ant script
   */
  public void run() throws IOException {
    List<String> args = new ArrayList<String>();
    args.add("java");
    args.add("-jar");
    args.add(getAntLauncherPath());
    args.add("-f");
    args.add("build_rcp.xml");
    if (properties != null) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        args.add("-D" + entry.getKey() + "=" + entry.getValue());
      }
    }
    args.add(target);
    ProcessBuilder builder = new ProcessBuilder(args);
    builder.directory(DartLib.getDartTrunk().append(
        "editor/tools/features/com.google.dart.tools.deploy.feature_releng").toFile());

    // Echo the arguments 

    for (String arg : args) {
      System.out.print(arg);
      System.out.print(" ");
    }
    System.out.println();

    // Start the external process

    builder.redirectErrorStream(true);
    Process process = builder.start();
    InputStream processIn = new BufferedInputStream(process.getInputStream());

    // Copy output from the process to the console

    int result;
    while (true) {
      while (processIn.available() > 0) {
        int ch = processIn.read();
        System.out.print((char) ch);
      }
      try {
        result = process.exitValue();
        if (result != 0) {
          System.out.println(">>> Exit Value: " + result);
        }
        break;
      } catch (IllegalThreadStateException e) {
        // Process still running... fall through
      }
    }

    // Cleanup

    System.out.println();
    System.out.flush();
  }

  /**
   * Set the value of the specified Ant property
   */
  public void setProperty(String key, String value) {
    if (properties == null) {
      properties = new HashMap<String, String>();
    }
    properties.put(key, value);
  }

  /**
   * Answer the path to the Ant launcher jar file
   */
  private String getAntLauncherPath() {
    Bundle bundle = Platform.getBundle("org.apache.ant");
    if (bundle == null) {
      throw new RuntimeException("Failed to find plugin org.apache.ant");
    }
    URL entry = bundle.getEntry("lib/ant-launcher.jar");
    if (entry == null) {
      throw new RuntimeException("Failed to find lib/ant-launcher.jar in plugin org.apache.ant");
    }
    try {
      File file = FileUtils.toFile(FileLocator.toFileURL(entry));
      return file.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
