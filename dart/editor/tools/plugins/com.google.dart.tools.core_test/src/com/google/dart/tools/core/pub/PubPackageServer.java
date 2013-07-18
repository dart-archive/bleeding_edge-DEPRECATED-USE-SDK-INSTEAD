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
package com.google.dart.tools.core.pub;

import com.google.dart.tools.core.dart2js.ProcessRunner;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.test.util.TestUtilities;
import com.google.dart.tools.core.utilities.net.NetUtils;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the dart HTTP server that serves the packages for pub requests during tests. Users should
 * call stop() at the end of test/test suite to terminate the server process.
 */
public class PubPackageServer {

  private static final String PUB_SERVER_PATH = "src-dart/pub_package_server.dart";

  private static final String PUB_SERVER_DATA_PATH = "test_data/PubServerData";

  public static int pubServerPort = 0;

  private static ProcessRunner runner;

  public static boolean isRunning() {
    return runner != null;
  }

  /**
   * Start the dart pub package server
   */
  public static void start() {

    // if server has been started
    if (isRunning()) {
      return;
    }

    URL dataUrl = FileLocator.find(Platform.getBundle(TestUtilities.CORE_TEST_PLUGIN_ID), new Path(
        PUB_SERVER_DATA_PATH), null);

    URL serverUrl = FileLocator.find(
        Platform.getBundle(TestUtilities.CORE_TEST_PLUGIN_ID),
        new Path(PUB_SERVER_PATH),
        null);

    String testDataLocation = null;
    String pubDartServerPath = null;
    if (dataUrl != null && serverUrl != null) {
      try {
        testDataLocation = FileLocator.resolve(dataUrl).toURI().getPath();
        pubDartServerPath = FileLocator.resolve(serverUrl).toURI().getPath();
      } catch (URISyntaxException e) {

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (testDataLocation != null && pubDartServerPath != null) {
      pubServerPort = NetUtils.findUnusedPort(0);
      startServer(pubDartServerPath, testDataLocation);
    }
  }

  /**
   * Terminate the server process. Should be called at end of tests.
   */
  public static void stop() {
    if (runner != null) {
      runner.dispose();
    }
    runner = null;
  }

  private static void startServer(String pubDartServerPath, String testDataLocation) {
    String vmExecPath = null;
    if (DartSdkManager.getManager().hasSdk()) {
      File vmExec = DartSdkManager.getManager().getSdk().getVmExecutable();
      if (vmExec != null) {
        vmExecPath = vmExec.getAbsolutePath().toString();
      }
    }

    if (vmExecPath.length() != 0) {

      List<String> commandsList = new ArrayList<String>();
      commandsList.add(vmExecPath);
      commandsList.add(pubDartServerPath);
      commandsList.add(Integer.toString(pubServerPort));
      commandsList.add(testDataLocation);

      String[] commands = commandsList.toArray(new String[commandsList.size()]);
      ProcessBuilder processBuilder = new ProcessBuilder(commands);
      runner = new ProcessRunner(processBuilder);
      try {
        runner.runAsync();
      } catch (IOException e) {

      }

      // check if process has started and is running 
      //int exitcode = runner.getExitCode();

    }

  }

}
