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
import com.google.dart.tools.core.utilities.net.NetUtils;
import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.DartLaunchConfigWrapper;
import com.google.dart.tools.debug.core.pubserve.PubConnection.PubConnectionListener;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Manages the pub serve process for launches. Clients should call
 * startPubServe(DartLaunchconfigWrapper) and if successful then call
 * connectToPub(PubCallback<String>) with a callback to process the communication received from pub.
 */
public class PubServeManager {

  private class ServeDirectoryCallback implements PubCallback<String> {

    private CountDownLatch latch;
    private boolean[] done;

    public ServeDirectoryCallback(CountDownLatch latch, boolean[] done) {
      this.latch = latch;
      this.done = done;
    }

    @Override
    public void handleResult(PubResult<String> result) {
      if (result.isError()) {
        done[0] = false;
      } else {
        DartCore.getConsole().println("Serving from " + result.getResult());
        done[0] = true;
      }
      latch.countDown();
    }
  }

  private static final String SERVE_COMMAND = "serve";

  private static final String LOCAL_HOST_ADDR = "127.0.0.1";

  private static final String WEBSOCKET_URL = "ws://{0}:{1}/";

  private static final String PUB_SNAPSHOT_PATH = "bin/snapshots/pub.dart.snapshot";

  private static PubServeManager manager = new PubServeManager();

  public static PubServeManager getManager() {
    return manager;
  }

  private DartLaunchConfigWrapper currentLaunch;
  private Process process = null;
  private StringBuilder stdOut;
  private StringBuilder stdError;
  private MessageConsole console;
  private PubConnection pubConnection;
  private IContainer workingDir;
  private String portNo;

  /**
   * This starts a websocket connection with pub and if successful, sends a "assetIdToUrl" command
   * to pub to get the url to launch for the resource that was specifed in the launch configuration
   * 
   * @param pubCallback - users should pass in a method to process the response for the
   *          "assetIdToUrl" command
   * @return true if the websocket connection was established and the command sent
   */
  public boolean connectToPub(PubCallback<String> pubCallback) {

    if (pubConnection != null && pubConnection.isConnected()) {
      return sendGetUrlCommand(pubCallback);
    } else {

      try {
        pubConnection = new PubConnection(new URI(NLS.bind(WEBSOCKET_URL, LOCAL_HOST_ADDR, portNo)));

        pubConnection.addConnectionListener(new PubConnectionListener() {
          @Override
          public void connectionClosed(PubConnection connection) {
            pubConnection = null;
          }
        });

        pubConnection.connect();

        return sendGetUrlCommand(pubCallback);

      } catch (URISyntaxException e) {
        DartDebugCorePlugin.logError(e);
        return false;
      } catch (IOException e) {
        DartDebugCorePlugin.logError(e);
        return false;
      }
    }
  }

  public void dispose() {
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

  public String getStdErrorString() {
    return stdError.toString();
  }

  /**
   * Starts pub serve for a given launch configuration. Checks if the current pub serve is for the
   * same pubspec.yaml, if not then starts up pub serve.
   * 
   * @param wrapper - the launch config wrapper
   * @return - true if pub serve starts
   */
  public boolean startPubServe(DartLaunchConfigWrapper wrapper) {

    // TODO(keertip): output to process console
    console = DartCore.getConsole();

    if (currentLaunch != null) {
      IResource resource = currentLaunch.getApplicationResource();
      if (resource != null) {
        // check if previous launch and new launch share the same pubspec. If so, and pub serve is
        // running, then current pub serve can be used.
        IContainer appDir = DartCore.getApplicationDirectory(resource);
        if (appDir.equals(DartCore.getApplicationDirectory(wrapper.getApplicationResource()))) {
          // TODO(keertip): make this separate checks so that new connection can be started without 
          // starting new process
          if (process != null && !isTerminated() && pubConnection != null
              && pubConnection.isConnected()) {
            console.printSeparator("Starting pub serve : " + resource.getProject().getName());
            // make sure pub is serving the directory, send serve directory command
            boolean isServed = serveDirectory(wrapper.getApplicationResource());
            if (isServed) {
              currentLaunch = wrapper;
              return true;
            }
          }
        }
      }
    }

    // terminate existing pub serve if any
    dispose();
    return runPubServe(wrapper);
  }

  private List<String> buildPubServeCommand() {
    DartSdk sdk = DartSdkManager.getManager().getSdk();
    File pubFile = sdk.getPubExecutable();
    List<String> args = new ArrayList<String>();
    // on Windows, run the pub snapshot directly instead of the pub script,
    // since process.destroy() cannot terminate children of the process.
    if (DartCore.isWindows()) {
      pubFile = new File(sdk.getDirectory().getAbsolutePath(), PUB_SNAPSHOT_PATH);
      args.add(sdk.getVmExecutable().getAbsolutePath());
      args.add(pubFile.getAbsolutePath());
    } else {
      args.add(pubFile.getAbsolutePath());
    }
    args.add(SERVE_COMMAND);
    args.add("--admin-port");
    portNo = Integer.toString(NetUtils.findUnusedPort(0));
    args.add(portNo);
    args.add("--hostname");
    args.add(LOCAL_HOST_ADDR);
    return args;
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
      DartCore.logError(ioe);
    }
  }

  /**
   * Returns the path to the resource from the directory where the pubspec resides. - myproj -
   * pubspec.yaml - web - index.html => web/index.html
   */
  private String getPathFromWorkingDir(IResource resource) {
    return resource.getFullPath().removeFirstSegments(workingDir.getFullPath().segmentCount()).toString();
  }

  /**
   * Returns the name of the directory containing the given resource that can be used as root by pub
   * serve. Pub serve uses the directories that are siblings to the pubspec as root.
   * 
   * @param container - directory which contains the pubspec.yaml
   * @param resource - the resource to launch
   * @return
   */
  private String getPubserveRootDir(IContainer container, IResource resource) {

    try {
      IResource[] folders = container.members();
      for (IResource folder : folders) {
        if (folder instanceof IFolder
            && !(folder.getName().equals(DartCore.PACKAGES_DIRECTORY_NAME) || folder.getName().equals(
                DartCore.BUILD_DIRECTORY_NAME))) {
          if (resource.getFullPath().toString().startsWith(folder.getFullPath().toString())) {
            return folder.getName();
          }
        }
      }
    } catch (CoreException e) {
      DartCore.logError(e);
    }

    return null;
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

  private boolean runPubServe(DartLaunchConfigWrapper wrapper) {

    stdOut = new StringBuilder();
    stdError = new StringBuilder();
    IResource resource = wrapper.getApplicationResource();
    console.printSeparator("Starting pub serve : " + resource.getProject().getName());

    workingDir = DartCore.getApplicationDirectory(resource);

    List<String> args = buildPubServeCommand();
    String dirName = getPubserveRootDir(workingDir, resource);
    if (dirName != null) {
      args.add(getPubserveRootDir(workingDir, resource));
    }
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

    while (!isTerminated() && !stdOut.toString().contains(LOCAL_HOST_ADDR)) {
      try {
        Thread.sleep(200);
      } catch (Exception exception) {

      }
    }

    if (isTerminated()) {
      return false;
    }
    currentLaunch = wrapper;
    return true;
  }

  private boolean sendGetUrlCommand(PubCallback<String> callback) {

    PubCommands command = pubConnection.getCommands();
    try {
      command.pathToUrl(getPathFromWorkingDir(currentLaunch.getApplicationResource()), callback);
    } catch (IOException e) {
      DartDebugCorePlugin.logError(e);
      return false;
    }
    return true;
  }

  /**
   * Send a serve directory command to the current pub serve
   * 
   * @param resource
   * @return
   */
  private boolean serveDirectory(IResource resource) {
    CountDownLatch latch = new CountDownLatch(1);
    final boolean[] done = new boolean[1];
    done[0] = false;
    try {
      pubConnection.getCommands().serveDirectory(
          getPubserveRootDir(workingDir, resource),
          new ServeDirectoryCallback(latch, done));
    } catch (IOException e) {
      DartCore.logError(e);
    }
    try {
      latch.await(3000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // do nothing
    }
    return done[0];

  }
}
