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

import com.google.dart.engine.utilities.io.FileUtilities;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages access to the Android SDK
 */
public class AndroidSdkManager {

  public static final String ANDROID_SDK_LOCATION_PREFERENCE = "androidSdkLocation";

  public static final String CONTENT_SHELL_APK = "content_shell-android-arm-release.apk";

  private static final String CONNECTION_TEST_APK = "com.google.dart.editor.mobile.connection.service.apk";

  public static final String ANDROID_DIRECTORY_NAME = "android";

  private static final String ADB_DIRECTORY_NAME = "adb";

  private static AndroidSdkManager manager = new AndroidSdkManager();

  public static AndroidSdkManager getManager() {
    return manager;
  }

  private List<MobileUpdateListener> listeners = new ArrayList<MobileUpdateListener>();

  public void addMobileUpdateListener(MobileUpdateListener lisener) {
    listeners.add(lisener);
  }

  public String getConnectionTestApkLocation() {
    File androidDir = getAndroidDir();
    return androidDir.getAbsolutePath() + File.separator + CONNECTION_TEST_APK;
  }

  // the apk is in installdir/android
  public String getContentShellApkLocation() {
    File androidDir = getAndroidDir();
    return androidDir.getAbsolutePath() + File.separator + CONTENT_SHELL_APK;
  }

  /**
   * The plugins build looks in the installation directory for "android".
   */
  public File getDefaultPluginsAndroidDirectory() {
    return new File(DartSdkManager.getEclipseInstallationDirectory(), ANDROID_DIRECTORY_NAME);
  }

  public String getSdkLocationPreference() {
    return DartCore.getPlugin().getPrefs().get(ANDROID_SDK_LOCATION_PREFERENCE, "");
  }

  public boolean isAdbInstalled() {
    File adbFile = getAdbExecutable();
    return adbFile != null && adbFile.exists();
  }

  public void notifyMobileUpdateListeners() {
    for (MobileUpdateListener listener : listeners) {
      listener.mobileBinariesUpdated();
    }
  }

  public void removeMobileUpdateListener(MobileUpdateListener listener) {
    listeners.remove(listener);
  }

  public void setSdkLocationPreference(String location) {
    IEclipsePreferences prefs = DartCore.getPlugin().getPrefs();
    prefs.put(ANDROID_SDK_LOCATION_PREFERENCE, location);

    try {
      prefs.flush();
    } catch (BackingStoreException exception) {
      DartCore.logError(exception);
    }
  }

  File getAdbExecutable() {
    File androidDir = getAndroidDir();
    File adbDir = new File(androidDir, ADB_DIRECTORY_NAME);
    File adbFile;
    if (DartCore.isLinux()) {
      adbFile = new File(new File(adbDir, "linux"), "adb");
    } else if (DartCore.isMac()) {
      adbFile = new File(new File(adbDir, "macosx"), "adb");
    } else if (DartCore.isWindows()) {
      adbFile = new File(new File(adbDir, "windows"), "adb.exe");
    } else {
      return null;
    }
    return FileUtilities.ensureExecutable(adbFile) ? adbFile : null;
  }

  private File getAndroidDir() {
    return new File(
        DartSdkManager.getManager().getSdk().getDirectory().getParentFile(),
        ANDROID_DIRECTORY_NAME);
  }
}
