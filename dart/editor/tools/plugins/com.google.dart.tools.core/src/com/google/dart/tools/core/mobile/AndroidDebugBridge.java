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
package com.google.dart.tools.core.mobile;

import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Instance of class {@code AndroidDevBridge} represents the AndroidDevBridge (adb) in the Android
 * SDK
 */
public class AndroidDebugBridge {

  /**
   * Specialized process runner for ADB logcat
   */
  class LogcatRunner extends ProcessRunner {

    private final String msgPrefix;
    private final PrintStringWriter output = new PrintStringWriter();
    private final Object lock = new Object();
    private IStatus result = null;

    public LogcatRunner(ProcessBuilder processBuilder, String msgPrefix) {
      super(processBuilder);
      this.msgPrefix = msgPrefix;
    }

    public String getOutput() {
      return output.toString();
    }

    /**
     * Block until a result is available or the specified timeout expires.
     * 
     * @return the status or {@code null} if the wait timed out
     */
    public IStatus waitForResult(long timeout) {
      synchronized (lock) {
        long endTime = System.currentTimeMillis() + timeout;
        while (result == null) {
          long delta = endTime - System.currentTimeMillis();
          if (delta <= 0) {
            break;
          }
          try {
            lock.wait(delta);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
        }
        return result;
      }
    }

    @Override
    protected void pipeStdout(InputStream in, StringBuilder builder) {
      try {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(in, "UTF-8"));
        while (true) {
          String line = reader.readLine();
          if (line == null) {
            break;
          }
          int index = line.indexOf(msgPrefix);
          if (index >= 0) {
            String msg = line.substring(index + msgPrefix.length() + 1).trim();
            output.println(msg);
            if (msg.equals("Success")) {
              setResult(Status.OK_STATUS);
            } else if (msg.startsWith("Error:")) {
              setResult(new Status(IStatus.ERROR, DartCore.PLUGIN_ID, msg.substring(6).trim()));
            }
          }
        }
      } catch (UnsupportedEncodingException e) {
        DartCore.logError(e);
      } catch (IOException e) {
        // This exception is expected.
      }
    }

    private void setResult(IStatus status) {
      synchronized (lock) {
        result = status;
        lock.notifyAll();
      }
    }
  }

  private static final String CONTENT_SHELL_APK_ID = "org.chromium.content_shell_apk";
  private static final String CONNECTION_TEST_APK_ID = "com.google.dart.editor.mobile.connection.service";

  private static final String[] LIST_PACKAGES_CMD = new String[] {
      "shell", "pm", "list", "packages", "-3"};
  private static final String[] INSTALL_CMD = new String[] {"install", "-r"};
  private static final String[] DEVICES_CMD = new String[] {"devices"};

  private static final String DEVICE_CONNECTED_SUFFIX = "\tdevice";
  private static final String UNAUTHORIZED_SUFFIX = "\tunauthorized";

  private static final String[] START_SERVICE = new String[] {"shell", "am", "startservice"};

  private static String[] LAUNCH_URL_IN_CC_CMD = new String[] {
      "shell", "am", "start", "-n", "org.chromium.content_shell_apk/.ContentShellActivity", "-d"};

  private static String[] LAUNCH_URL_IN_BROWSER_CMD = new String[] {
      "shell", "am", "start", "-n", "com.android.chrome/com.google.android.apps.chrome.Main", "-d"};

  private static String[] STOP_APP_CMD = new String[] {
      "shell", "am", "force-stop", CONTENT_SHELL_APK_ID};

  private static final String CONTENT_SHELL_DEBUG_PORT = "localabstract:content_shell_devtools_remote";
  private static final String CHROME_DEBUG_PORT = "localabstract:chrome_devtools_remote";

  private static String[] PORT_FORWARD_CMD = new String[] {"forward"};

  private static String[] UNINSTALL_CMD = new String[] {"shell", "pm", "uninstall", "-k"};

  private static String[] START_SERVER_CMD = new String[] {"start-server"};

  private static final String[] LOGCAT_CMD = new String[] {"logcat"};

  private static AndroidDebugBridge androidDebugBridge = new AndroidDebugBridge(
      AndroidSdkManager.getManager().getAdbExecutable());

  public static AndroidDebugBridge getAndroidDebugBridge() {
    return androidDebugBridge;
  }

  private File adbExecutable;
  private ProcessRunner adbRunner;
  private final HashMap<String, HashSet<String>> installSet = new HashMap<String, HashSet<String>>();

  private AndroidDebugBridge(File adbExecutable) {
    this.adbExecutable = adbExecutable;
  }

  /**
   * Gets the first device connected and detected by adb
   * 
   * @return the device or {@code null} if no device detected
   */
  public AndroidDevice getConnectedDevice() {
    List<String> args = buildAdbCommand(DEVICES_CMD);
    if (runAdb(args)) {
      //List of devices attached 
      //04f5385f95d80610  device
      //T062873654  unauthorized
      String unauthorized = null;
      LineNumberReader reader = new LineNumberReader(new StringReader(adbRunner.getStdOut()));
      try {
        while (true) {
          String line = reader.readLine();
          if (line == null) {
            break;
          }
          line = line.trim();
          if (line.endsWith(DEVICE_CONNECTED_SUFFIX)) {
            String id = line.substring(0, line.length() - DEVICE_CONNECTED_SUFFIX.length()).trim();
            return new AndroidDevice(id, true);
          }
          if (line.endsWith(UNAUTHORIZED_SUFFIX)) {
            unauthorized = line.substring(0, line.length() - UNAUTHORIZED_SUFFIX.length()).trim();
          }
        }
      } catch (IOException e) {
        //$FALL-THROUGH$
      }
      if (unauthorized != null) {
        return new AndroidDevice(unauthorized, false);
      }
    }
    return null;
  }

  /**
   * Install the apk for the content shell onto the connected phone
   * <p>
   * adb install path/to/apk
   * </p>
   * 
   * @param device the device on which to install the content shell
   * @return true if install was successful
   */
  public boolean installContentShellApk(AndroidDevice device) {
    return install(
        device,
        "dart content shell browser",
        CONTENT_SHELL_APK_ID,
        AndroidSdkManager.getManager().getContentShellApkLocation());
  }

  /**
   * Determine if a mobile device is connected and authorized.
   */
  public boolean isDeviceConnectedAndAuthorized() {
    AndroidDevice device = getConnectedDevice();
    return device != null && device.isAuthorized();
  }

  /**
   * Determine if the given URL is accessible from the mobile device.
   */
  public IStatus isHtmlPageAccessible(AndroidDevice device, String pageUrl) {
    if (!installConnectionTestApk(device)) {
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Failed to install connection test");
    }

    String msgPrefix = "Connection test (" + System.currentTimeMillis() + ")";
    LogcatRunner logcatRunner = new LogcatRunner(
        new ProcessBuilder(buildAdbCommand(LOGCAT_CMD)),
        msgPrefix);
    try {
      logcatRunner.runAsync();
    } catch (IOException e) {
      DartCore.logError(e);
      DartCore.getConsole().println("Failed to launch ADB logcat");
      return new Status(IStatus.ERROR, DartCore.PLUGIN_ID, "Failed to launch ADB logcat", e);
    }
    // Launch the service that tests the connection from mobile device to developer machine
    List<String> args = buildAdbCommand(START_SERVICE);
    args.add("-n");
    args.add("com.google.dart.editor.mobile.connection.service/.ConnectionService");
    args.add("-d");
    args.add(pageUrl);
    args.add("-e");
    args.add("prefix");
    args.add(msgPrefix);

    // wait for dialog for connection to browser on mobile, and then
    // start the check for port forwarding. 
    while (DartCore.allowConnectionDialogOpen == true) {
      threadSleep(500);
    }

    try {
      if (!runAdb(args, "ADB: check port forwarding")) {
        return new Status(
            IStatus.ERROR,
            DartCore.PLUGIN_ID,
            "Failed to launch port forwarding detection");
      }

      IStatus result = logcatRunner.waitForResult(3500);
      if (result != null) {
        if (!result.isOK()) {
          //DartCore.getConsole().println(logcatRunner.getOutput());
          DartCore.getConsole().println(result.getMessage());
        }
        return result;
      }
      return new Status(
          IStatus.ERROR,
          DartCore.PLUGIN_ID,
          "Timeout waiting for port forwarding detection");
    } finally {
      logcatRunner.dispose();
    }
  }

  /**
   * Open the url in the chrome browser on the device
   * <p>
   * adb shell am start com.android.chrome/com.google.android.apps.chrome.Main -d url
   * </p>
   */
  public boolean launchChromeBrowser(String url) {
    List<String> args = buildAdbCommand(LAUNCH_URL_IN_BROWSER_CMD);
    args.add(url);
    return runAdb(args, "ADB: launch browser");
  }

  /**
   * Launch the browser on the phone and open url
   * <p>
   * adb shell am start -n org.chromium.content_shell_apk/.ContentShellActivity -d
   * http://www.cheese.com
   * </p>
   * 
   * @return true if launch was successful
   */
  public boolean launchContentShell(String deviceId, String url) {

    List<String> args = buildAdbCommand(LAUNCH_URL_IN_CC_CMD);
    if (deviceId != null) {
      args.add(1, "-s");
      args.add(2, deviceId);
    }
    args.add(url);
    return runAdb(args, "ADB: launch dart content shell browser");
  }

  public void scanForDevices() {
    // TODO(keertip): implement this
  }

  /**
   * Setup port forwarding from machine to phone
   * <p>
   * adb forward tcp:<local-port> tcp:<remote-port>
   * </p>
   */
  public boolean setupPortForwarding(String port) {
    List<String> args = buildAdbCommand(PORT_FORWARD_CMD);
    args.add("tcp:" + port);
    args.add(CONTENT_SHELL_DEBUG_PORT);
    return runAdb(args, "");
  }

  /**
   * Starts the adb server
   */
  public void startAdbServer() {
    List<String> args = buildAdbCommand(START_SERVER_CMD);
    runAdb(args);
  }

  /**
   * Force stop (close) the content shell on the connected phone
   * <p>
   * adb shell am force-stop org.chromium.content_shell_apk
   * </p>
   * 
   * @return true if stop was successful
   */
  public boolean stopApplication() {
    List<String> args = buildAdbCommand(STOP_APP_CMD);
    return runAdb(args, "ADB: stop application");
  }

  public void uninstallConnectionTestApk(AndroidDevice device) {
    if (!uninstall(device, CONNECTION_TEST_APK_ID)) {
      DartCore.logError("Failed to uninstall " + CONNECTION_TEST_APK_ID);
    }
  }

  /**
   * Uninstall the content shell
   */
  public void uninstallContentShellApk(AndroidDevice device) {
    uninstall(device, CONTENT_SHELL_APK_ID);
  }

  private List<String> buildAdbCommand(String... cmds) {
    List<String> args = new ArrayList<String>();
    args.add(adbExecutable.getAbsolutePath());
    if (cmds != null) {
      for (String string : cmds) {
        args.add(string);
      }
    }
    return args;
  }

  /**
   * Install the specified APK onto the mobile device if this is the first installation during this
   * sessions --or-- if the APK has been removed since the beginning of this session.
   * 
   * @param device the device onto which the APK will be installed
   * @param apkName the human readable name of the APK
   * @param apkId TODO
   * @param apkLocation the APK file to be installed
   * @return {@code true} if the APK was installed or already resides on the mobile device
   */
  private boolean install(AndroidDevice device, String apkName, String apkId, String apkLocation) {

    // Check if the APK has been installed and still resides on the mobile device
    if (device != null) {
      HashSet<String> apkIds = installSet.get(device.getDeviceId());
      if (apkIds == null) {
        apkIds = new HashSet<String>();
        installSet.put(device.getDeviceId(), apkIds);
      }
      if (apkIds.contains(apkId) && isInstalled(device, apkId)) {
        return true;
      }
      apkIds.add(apkId);
    }

    // Uninstall the old APK if it exists
    //uninstall(device, apkId);

    // Install the APK
    // adb install -r <path> to replace previously installed APK if present
    List<String> args = buildAdbCommand(INSTALL_CMD);
    if (device != null) {
      args.add(1, "-s");
      args.add(2, device.getDeviceId());
    }
    args.add(apkLocation);
    return runAdb(args, "ADB: install " + apkName, "This could take up to 30 seconds");
  }

  private boolean installConnectionTestApk(AndroidDevice device) {
    return install(
        device,
        "connection test",
        CONNECTION_TEST_APK_ID,
        AndroidSdkManager.getManager().getConnectionTestApkLocation());
  }

  /**
   * Query the mobile device to determine if the specified APK is installed.
   * 
   * @param device the device
   * @param apkId the APK identifier
   * @return {@code true} if installed, else {@code false}
   */
  private boolean isInstalled(AndroidDevice device, String apkId) {
    List<String> args = buildAdbCommand(LIST_PACKAGES_CMD);
    if (device != null) {
      args.add(1, "-s");
      args.add(2, device.getDeviceId());
    }
    if (runAdb(args)) {
      // Output is list of apk identifiers prefixed by "package:"
      //package:com.google.dart.editor.mobile.connection.service
      //package:org.chromium.content_shell_apk
      String target = "package:" + apkId;
      LineNumberReader reader = new LineNumberReader(new StringReader(adbRunner.getStdOut()));
      try {
        while (true) {
          String line = reader.readLine();
          if (line == null) {
            break;
          }
          line = line.trim();
          if (line.equals(target)) {
            return true;
          }
        }
      } catch (IOException e) {
        //$FALL-THROUGH$
      }
    }
    return false;
  }

  private boolean runAdb(List<String> args, String... message) {
    int exitCode = 1;
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    adbRunner = new ProcessRunner(builder);
    for (int index = 0; index < message.length; index++) {
      if (index == 0) {
        DartCore.getConsole().printSeparator(message[index]);
      } else {
        DartCore.getConsole().println(message[index]);
      }
    }
    try {
      exitCode = adbRunner.runSync(null);
      if (exitCode != 0) {
        DartCore.getConsole().println(adbRunner.getStdErr());
      }

    } catch (IOException e) {
      DartCore.logError(e);
    }
    return exitCode == 0 ? true : false;
  }

  private void threadSleep(long millisecs) {
    try {
      Thread.sleep(millisecs);
    } catch (InterruptedException e) {

    }
  }

  private boolean uninstall(AndroidDevice device, String apkId) {
    List<String> args = buildAdbCommand(UNINSTALL_CMD);
    if (device != null) {
      args.add(1, "-s");
      args.add(2, device.getDeviceId());
    }
    args.add(apkId);
    return runAdb(args, "ADB: uninstall " + apkId);
  }
}
