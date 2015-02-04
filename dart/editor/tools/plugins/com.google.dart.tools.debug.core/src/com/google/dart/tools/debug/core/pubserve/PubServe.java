/*
 * Copyright (c) 2014, the Dart project authors.
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

import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.MessageConsole;
import com.google.dart.tools.core.model.DartSdkManager;
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.pubserve.PubConnection.PubConnectionListener;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents a pub serve process
 */
public class PubServe {

  private class ServeDirectoryCallback implements PubCallback<String> {

    private CountDownLatch latch;
    private String[] message;

    public ServeDirectoryCallback(CountDownLatch latch, String[] message) {
      this.latch = latch;
      this.message = message;
    }

    @Override
    public void handleResult(PubResult<String> result) {
      if (result.isError()) {
        message[0] = result.getErrorMessage();
      } else {
        DartCore.getConsole().println("Serving from " + result.getResult());
      }
      latch.countDown();
    }
  }

  private static final String SERVE_COMMAND = "serve";

  private static final String LOCAL_HOST_ADDR = "localhost";

  private static final String WEBSOCKET_URL = "ws://{0}:{1}/";

  private static final String PUB_SNAPSHOT_PATH = "bin/snapshots/pub.dart.snapshot";

  private Process process = null;
  private StringBuilder stdOut;
  private StringBuilder stdError;
  private MessageConsole console;
  private PubConnection pubConnection;
  private String portNumber;
  private IContainer workingDir;

  private List<String> servedDirs = new ArrayList<String>();

  /**
   * Starts a pub serve process with the given working directory and directory to be served and
   * connects to the pub admin server.
   * 
   * @param workingDir - working directory for the pub serve process
   * @param arguments - a list of user arguments for pub serve
   * @param dirToServeName - name of the directory to be served
   * @throws Exception
   */
  PubServe(IContainer workingDir, String directoryToServe, String[] arguments) throws Exception {
    console = DartCore.getConsole();
    this.workingDir = workingDir;
    runPubServe(directoryToServe, arguments);
    connectToPub();
  }

  void dispose() {
    // TODO(keertip): stop pub serve first when api available
    if (pubConnection != null) {
      try {
        pubConnection.close();
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
      } finally {
        pubConnection = null;
      }
    }
    if (process != null) {
      try {
        process.destroy();
      } catch (Exception e) {

      } finally {
        process = null;
      }
    }

  }

  String getPortNumber() {
    return portNumber;
  }

  String getStdErrorString() {
    return stdError.toString();
  }

  IContainer getWorkingDir() {
    return workingDir;
  }

  boolean isAlive() {
    return process != null && !isTerminated() && pubConnection != null
        && pubConnection.isConnected();
  }

  /**
   * Sends an exit on close command to pub, indicating pub serve should shut down when connection is
   * lost.
   * 
   * @throws IOException
   */
  void sendExitOnCloseCommand() throws IOException {
    PubCommands command = pubConnection.getCommands();
    command.exitOnClose();
  }

  /**
   * Send a urlToAssetId command to the current pub serve
   * 
   * @param url
   * @param callback
   * @throws IOException
   */
  void sendGetAssetIdCommand(String url, PubCallback<PubAsset> callback) throws IOException {
    PubCommands command = pubConnection.getCommands();
    command.urlToAssetId(url, callback);
  }

  /**
   * Send a pathToUrl command to the current pub serve
   * 
   * @param path
   * @param callback
   * @return
   * @throws IOException
   */
  void sendGetUrlCommand(String path, PubCallback<String> callback) throws IOException {

    PubCommands command = pubConnection.getCommands();
    command.pathToUrl(path, callback);
  }

  /**
   * Send a serve directory command to the current pub serve
   * 
   * @throws IOException
   */
  void serveDirectory(String dirName) throws Exception {

    if (!servedDirs.contains(dirName)) {

      CountDownLatch latch = new CountDownLatch(1);
      String[] message = new String[1];
      message[0] = "";

      pubConnection.getCommands().serveDirectory(
          dirName,
          new ServeDirectoryCallback(latch, message));
      try {
        latch.await(5000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // do nothing
      }
      if (message[0].isEmpty()) {
        servedDirs.add(dirName);
      } else {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartDebugCorePlugin.PLUGIN_ID,
            "Could not serve directory\n" + message[0]));
      }
    }
  }

  private List<String> buildPubServeCommand(String directoryToServe, String[] args) {
    DirectoryBasedDartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = sdk.getPubExecutable();
    List<String> command = new ArrayList<String>();
    List<String> pubServeArgs = Arrays.asList(args);

    pubFile = new File(sdk.getDirectory().getAbsolutePath(), PUB_SNAPSHOT_PATH);
    command.add(sdk.getVmExecutable().getAbsolutePath());
    command.add(pubFile.getAbsolutePath());
    command.add(SERVE_COMMAND);
    command.add(directoryToServe);
    int pubport;
    if (!pubServeArgs.contains("--port")) {
      pubport = NetUtils.getUnusedPort(8080, 8100);
      if (pubport != -1) {
        command.add("--port");
        command.add(Integer.toString(pubport));
      }
    }
    command.add("--admin-port");
    portNumber = Integer.toString(NetUtils.findUnusedPort(0));
    command.add(portNumber);
    if (!pubServeArgs.contains("--hostname")) {
      command.add("--hostname");
      command.add(LOCAL_HOST_ADDR);
    }
    command.addAll(pubServeArgs);
    return command;
  }

  /**
   * This starts a websocket connection with pub
   */
  private void connectToPub() throws Exception {

    pubConnection = new PubConnection(new URI(NLS.bind(WEBSOCKET_URL, LOCAL_HOST_ADDR, portNumber)));

    pubConnection.addConnectionListener(new PubConnectionListener() {
      @Override
      public void connectionClosed(PubConnection connection) {
        pubConnection = null;
      }
    });

    pubConnection.connect();
    sendExitOnCloseCommand();
  }

  private void copyStream(InputStream in, StringBuilder stringBuilder, boolean toConsole) {
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
      DartCore.logInformation("Exception when reading from pub serve process stream", ioe);
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

  private boolean runPubServe(String directoryToServe, String[] arguments) {

    stdOut = new StringBuilder();
    stdError = new StringBuilder();

    List<String> args = buildPubServeCommand(directoryToServe, arguments);

    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    builder.directory(workingDir.getLocation().toFile());

    try {
      process = builder.start();
    } catch (IOException e) {
      DartCore.logError(e);
      return false;
    }

    Thread stdoutThread = new Thread(new Runnable() {
      @Override
      public void run() {
        copyStream(process.getInputStream(), stdOut, true);
      }
    });
    stdoutThread.start();

    Thread stderrThread = new Thread(new Runnable() {
      @Override
      public void run() {
        copyStream(process.getErrorStream(), stdError, true);
      }
    });
    stderrThread.start();

    // TODO(keertip): maybe use http:// expr instead?
    while (!isTerminated() && !stdOut.toString().contains("http://localhost")) {
      try {
        Thread.sleep(200);
      } catch (Exception exception) {

      }
    }

    if (isTerminated()) {
      return false;
    }
    servedDirs.add(directoryToServe);

    return true;
  }
}
