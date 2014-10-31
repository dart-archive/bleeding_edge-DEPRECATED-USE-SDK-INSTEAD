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
package com.google.dart.tools.debug.core.server;

import com.google.common.base.Charsets;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.configs.DartServerLaunchConfigurationDelegate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IDebugTarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulate a VM debug instance.
 */
public class VMDebugger {
  private int serverSocketPort;
  private Process process;
  private BufferedReader stdout;
  private BufferedReader stderr;

  private IDebugTarget debugTarget;

  public VMDebugger() {

  }

  public void connect(String scriptPath) throws IOException, CoreException {
    ProcessBuilder builder = new ProcessBuilder(
        DartSdkManager.getManager().getSdk().getVmExecutable().getPath(),
        "--debug:0",
        scriptPath);

    process = builder.start();
    stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
    stderr = new BufferedReader(new InputStreamReader(process.getErrorStream(), Charsets.UTF_8));

    // read debug server port
    // "Debugger listening on port xxx"
    final Pattern portPattern = Pattern.compile("\\d+");

    String data = readLine();
    Matcher matcher = portPattern.matcher(data);

    if (matcher.find()) {
      String portValue = matcher.group();

      try {
        serverSocketPort = Integer.parseInt(portValue);

        DartServerLaunchConfigurationDelegate delegate = new DartServerLaunchConfigurationDelegate();

        debugTarget = delegate.performRemoteConnection(null, getConnectionPort(), null, null, false);

        return;
      } catch (NumberFormatException nfe) {
        throw new IOException("bad port value for debugger port: " + portValue);
      }
    } else {
      throw new IOException("no debugger port found");
    }
  }

  public void dispose() {
    if (process != null) {
      process.destroy();
      process = null;
    }
  }

  public int getConnectionPort() {
    return serverSocketPort;
  }

  public IDebugTarget getDebugTarget() {
    return debugTarget;
  }

  public int getExitValue() throws IllegalThreadStateException {
    return process.exitValue();
  }

  public BufferedReader getStderr() {
    return stderr;
  }

  public BufferedReader getStdout() {
    return stdout;
  }

  public String readLine() throws IOException {
    return getStdout().readLine();
  }

  public int waitForExit(long millis) {
    long startTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - startTime) < millis) {
      try {
        return getExitValue();
      } catch (IllegalThreadStateException e) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e1) {

        }
      }
    }

    return getExitValue();
  }

}
