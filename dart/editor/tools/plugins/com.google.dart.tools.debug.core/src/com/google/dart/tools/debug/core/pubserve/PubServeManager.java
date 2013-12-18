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
package com.google.dart.tools.debug.core.pubserve;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.model.DartSdk;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;

import org.eclipse.core.resources.IResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the pub serve process for launches
 */
public class PubServeManager {

  private static final String SERVE_COMMAND = "serve";
  public static final String PORT_NUMBER = "3031";

  private static PubServeManager manager = new PubServeManager();

  public static PubServeManager getManager() {
    return manager;
  }

  private DartLaunchConfigWrapper currentLaunch;
  private Process process = null;
  private StringBuilder stdOut;
  private StringBuilder stdError;
  private MessageConsole console;

  public void dispose() {
    if (process != null) {
      process.destroy();
      process = null;
    }
  }

  public String getStdErrorString() {
    return stdError.toString();
  }

  public boolean startPubServe(DartLaunchConfigWrapper wrapper) {

    console = DartCore.getConsole();

    if (currentLaunch != null) {
      IResource resource = currentLaunch.getApplicationResource();
      if (resource != null && resource.equals(wrapper.getApplicationResource())) {
        if (process != null && !isTerminated()) {
          console.printSeparator("Starting pub serve : " + resource.getProject().getName());
          return true;
        }
      }
    }

    currentLaunch = wrapper;
    // terminate existing pub serve if any
    dispose();
    return runPubServe();
  }

  private List<String> buildPubServeCommand() {
    DartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = sdk.getPubExecutable();
    List<String> args = new ArrayList<String>();
    args.add(pubFile.getAbsolutePath());
    args.add(SERVE_COMMAND);
    args.add("--port");
    args.add(PORT_NUMBER);
    return args;
  }

  private void copyStream(InputStream in, StringBuilder stringBuilder, boolean toConsole)
      throws IOException {
    byte[] buffer = new byte[2048];
    try {
      int count = in.read(buffer);
      while (count != -1) {
        if (count > 0) {
          String str = new String(buffer, 0, count);
          stringBuilder.append(str);
          if (toConsole) {
            console.print(str);
          }
        }
        count = in.read(buffer);
      }
      in.close();
    } catch (IOException ioe) {

    }
  }

  private boolean isTerminated() {
    try {
      if (process != null) {
        process.exitValue();
      }
    } catch (IllegalThreadStateException exception) {
      return false;
    }
    return true;
  }

  private boolean runPubServe() {

    stdOut = new StringBuilder();
    stdError = new StringBuilder();
    IResource resource = currentLaunch.getApplicationResource();
    console.printSeparator("Starting pub serve : " + resource.getProject().getName());

    List<String> args = buildPubServeCommand();

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    File workingDir = DartCore.getApplicationDirectory(resource.getParent().getLocation().toFile());
    builder.directory(workingDir);
    try {
      process = builder.start();
    } catch (IOException e) {
      DartCore.logError(e);
    }

    Thread stdoutThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          copyStream(process.getInputStream(), stdOut, true);
        } catch (IOException e) {

        }
      }
    });
    stdoutThread.start();

    Thread stderrThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          copyStream(process.getErrorStream(), stdError, true);
        } catch (IOException e) {

        }
      }
    });
    stderrThread.start();

    while (!isTerminated()
        && !stdOut.toString().contains("http://localhost:" + PubServeManager.PORT_NUMBER)) {
      try {
        Thread.sleep(200);
      } catch (Exception exception) {

      }
    }
    if (isTerminated()) {
      return false;
    }
    return true;
  }

}
