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
import java.util.ArrayList;
import java.util.List;

/**
 * Instance of class {@code AndroidDevBridge} represents the AndroidDevBridge (adb) in the Android
 * SDK
 */
public class AndroidDebugBridge {

  private File adb;

  private static String[] INSTALL_CMD = new String[] {"install"};
  private static String[] DEVICES_CMD = new String[] {"devices"};

  private static String[] LAUNCH_URL_IN_CC_CMD = new String[] {
      "shell", "am", "start", "-n", "org.chromium.content_shell_apk/.ContentShellActivity", "-d"};

  private static String[] LAUNCH_URL_IN_BROWSER_CMD = new String[] {
      "shell", "am", "start", "-n", "com.android.chrome/com.google.android.apps.chrome.Main", "-d"};

  private static String[] STOP_APP_CMD = new String[] {
      "shell", "am", "force-stop", "org.chromium.content_shell_apk"};

  private static String[] PORT_FORWARD_CMD = new String[] {"forward", "tcp:9222", "tcp:9222"};

  private static String[] UNINSTALL_CMD = new String[] {
      "shell", "pm", "uninstall", "-k", "org.chromium.content_shell_apk"};

  public AndroidDebugBridge() {
    this(AndroidSdkManager.getManager().getAdbExecutable());
  }

  AndroidDebugBridge(File adbExecutable) {
    this.adb = adbExecutable;
  }

  /**
   * Install the apk for the content shell onto the connected phone
   * <p>
   * adb install path/to/apk
   * </p>
   * 
   * @return true if install was successful
   */
  public boolean installContentShellApk() {
    // TODO(keertip): process error to check if apk is already installed
    // Do we need to remove and reinstall?

    List<String> args = buildAdbCommand(INSTALL_CMD);
    args.add(AndroidSdkManager.getManager().getContentShellApkLocation());
    return runAdb(args, "ADB: install dart content shell browser");
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
  public boolean launchContentShell(String url) {

    setupPortForwarding();

    List<String> args = buildAdbCommand(LAUNCH_URL_IN_CC_CMD);
    args.add(url);
    return runAdb(args, "ADB: launch dart content shell browser");
  }

  public void scanForDevices() {
    // TODO(keertip): implement this
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
   * Setup port forwarding from machine to phone
   * <p>
   * adb forward tcp:<local-port> tcp:<remote-port>
   * </p>
   */
  void setupPortForwarding() {
    List<String> args = buildAdbCommand(PORT_FORWARD_CMD);
    runAdb(args, "ADB: setup port forwarding");
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

  private boolean runAdb(List<String> args, String message) {
    int exitCode = 1;
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(args);
    ProcessRunner runner = new ProcessRunner(builder);
    DartCore.getConsole().printSeparator(message);
    try {
      exitCode = runner.runSync(null);
      if (exitCode != 0) {
        DartCore.getConsole().println(runner.getStdErr());
      } else {
        // TODO(keertip): filter messages 
        DartCore.getConsole().printSeparator(runner.getStdOut());
      }

    } catch (IOException e) {
      DartCore.logError(e);
    }
    return exitCode == 0 ? true : false;
  }

}
