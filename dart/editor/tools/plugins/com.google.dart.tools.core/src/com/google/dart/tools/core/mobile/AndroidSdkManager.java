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
import com.google.dart.tools.core.model.DartSdkManager;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import java.io.File;

/**
 * Manages access to the Android SDK
 */
public class AndroidSdkManager {

  public static final String ANDROID_SDK_LOCATION_PREFERENCE = "androidSdkLocation";

  private static final String CONTENT_SHELL_APK = "content_shell-android-arm-release.apk";

  private static final String ANDROID_DIRECTORY_NAME = "android";

  private static AndroidSdkManager manager = new AndroidSdkManager();

  public static AndroidSdkManager getManager() {
    return manager;
  }

  // the apk is in installdir/android
  public String getContentShellApkLocation() {
    File androidDir = new File(
        DartSdkManager.getManager().getSdk().getDirectory().getParentFile(),
        ANDROID_DIRECTORY_NAME);
    return androidDir.getAbsolutePath() + File.separator + CONTENT_SHELL_APK;
  }

  public String getSdkLocationPreference() {
    return DartCore.getPlugin().getPrefs().get(ANDROID_SDK_LOCATION_PREFERENCE, "");
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
    String sdkLocation = getSdkLocationPreference();
    if (!sdkLocation.isEmpty()) {
      return new File(sdkLocation, "platform-tools" + File.separator + "adb");
    }
    return null;
  }

}
