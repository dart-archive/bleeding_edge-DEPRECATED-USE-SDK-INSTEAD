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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dart2js.ProcessRunner;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Instance of class {@code AndroidDevBridge} represents the AndroidDevBridge (adb) in the Android
 * SDK
 */
public class AndroidDebugBridge {

  private File adb;

  private static String[] INSTALL_CMD = new String[] {"install"};
  private static String[] DEVICES_CMD = new String[] {"devices"};

  private static final String DEVICE_CONNECTED_SUFFIX = "\tdevice";
  private static final String UNAUTHORIZED_SUFFIX = "\tunauthorized";

  private static String[] LAUNCH_URL_IN_CC_CMD = new String[] {
      "shell", "am", "start", "-n", "org.chromium.content_shell_apk/.ContentShellActivity", "-d"};

  private static String[] LAUNCH_URL_IN_BROWSER_CMD = new String[] {
      "shell", "am", "start", "-n", "com.android.chrome/com.google.android.apps.chrome.Main", "-d"};

  private static String[] STOP_APP_CMD = new String[] {
      "shell", "am", "force-stop", "org.chromium.content_shell_apk"};

  private static final String CONTENT_SHELL_DEBUG_PORT = "localabstract:content_shell_devtools_remote";
  private static final String CHROME_DEBUG_PORT = "localabstract:chrome_devtools_remote";

  private static String[] PORT_FORWARD_CMD = new String[] {"forward"};

  private static String[] UNINSTALL_CMD = new String[] {
      "shell", "pm", "uninstall", "-k", "org.chromium.content_shell_apk"};

  private static String[] START_SERVER_CMD = new String[] {"start-server"};

  private ProcessRunner runner;

  /**
   * The identifiers of devices on which content shell has been installed during this session.
   */
  private static HashSet<String> contentShellInstallSet = new HashSet<String>();

  private static AndroidDebugBridge androidDebugBridge = new AndroidDebugBridge(
      AndroidSdkManager.getManager().getAdbExecutable());

  public static AndroidDebugBridge getAndroidDebugBridge() {
    return androidDebugBridge;
  }

  AndroidDebugBridge(File adbExecutable) {
    this.adb = adbExecutable;
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
      LineNumberReader reader = new LineNumberReader(new StringReader(runner.getStdOut()));
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
   * @param deviceId the identifier of the device on which to install the content shell
   * @return true if install was successful
   */
  public boolean installContentShellApk(String deviceId) {
    if (contentShellInstallSet.contains(deviceId)) {
      return true;
    }
    // TODO(keertip): process error to check if apk is already installed
    List<String> args = buildAdbCommand(INSTALL_CMD);
    if (deviceId != null) {
      args.add(1, "-s");
      args.add(2, deviceId);
    }
    args.add(AndroidSdkManager.getManager().getContentShellApkLocation());
    if (runAdb(args, "ADB: install dart content shell browser", "This could take up to 30 seconds")) {
      String message = runner.getStdOut();
      // if the apk is present, message is Failure [INSTALL_FAILED_ALREADY_EXISTS]   
      if (message.toLowerCase().contains("already_exists")) {
        // TODO(keertip): check version and reinstall
        // DartCore.getConsole().println(message);
      }
      contentShellInstallSet.add(deviceId);
      return true;
    }
    return false;
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
    runAdb(args, "");
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

  /**
   * Uninstall the content shell apk from the device
   */
  void uninstallContentShellApk() {
    List<String> args = buildAdbCommand(UNINSTALL_CMD);
    runAdb(args, "ADB: uninstall browser");
  }

  private List<String> buildAdbCommand(String[] cmds) {
    List<String> args = new ArrayList<String>();
    args.add(adb.getAbsolutePath());
    if (cmds != null) {
      for (String string : cmds) {
        args.add(string);
      }
    }
    return args;
  }

  private boolean runAdb(List<String> args, String... message) {
    int exitCode = 1;
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    runner = new ProcessRunner(builder);
    for (int index = 0; index < message.length; index++) {
      if (index == 0) {
        DartCore.getConsole().printSeparator(message[index]);
      } else {
        DartCore.getConsole().println(message[index]);
      }
    }
    try {
      exitCode = runner.runSync(null);
      if (exitCode != 0) {
        DartCore.getConsole().println(runner.getStdErr());
      }

    } catch (IOException e) {
      DartCore.logError(e);
    }
    return exitCode == 0 ? true : false;
  }

}
