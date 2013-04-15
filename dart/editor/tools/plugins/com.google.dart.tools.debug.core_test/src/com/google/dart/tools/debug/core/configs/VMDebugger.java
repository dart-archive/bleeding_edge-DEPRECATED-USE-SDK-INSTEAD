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
package com.google.dart.tools.debug.core.configs;

import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCoreTestPlugin;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Encapsulate a VM debug instance.
 */
public class VMDebugger {
  private static final int DEFAULT_PORT = 5859;

  private String scriptPath;
  private int serverSocketPort;

  private ProcessRunner processRunner;

  public VMDebugger() {
    this("data/scripts/test.dart");
  }

  public VMDebugger(String scriptPath) {
    this.scriptPath = scriptPath;

    serverSocketPort = NetUtils.findUnusedPort(DEFAULT_PORT);

    try {
      // Give the temporary server socket (from NetUtils.findUnusedPort()) time to close.
      Thread.sleep(1);
    } catch (InterruptedException e) {

    }
  }

  public void dispose() {
    processRunner.dispose();
  }

  public int getConnectionPort() {
    return serverSocketPort;
  }

  public String getOutput() {
    return processRunner.getStdOut();
  }

  public void start() throws IOException {
    ProcessBuilder builder = new ProcessBuilder(
        DartSdkManager.getManager().getSdk().getVmExecutable().getPath(),
        "--debug:" + getConnectionPort(),
        getScriptFilePath());

    processRunner = new ProcessRunner(builder);
    processRunner.runAsync();

    try {
      // Give the VM time to start up.
      Thread.sleep(100);
    } catch (InterruptedException e) {

    }
  }

  private String getScriptFilePath() throws IOException {
    try {
      URL bundleURL = FileLocator.find(DartDebugCoreTestPlugin.getPlugin().getBundle(), new Path(
          scriptPath), null);

      if (bundleURL != null) {
        URL fileURL = FileLocator.toFileURL(bundleURL);

        if (fileURL != null) {
          File file = new File(fileURL.toURI());

          return file.getAbsolutePath();
        }
      }
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

    return null;
  }

}
