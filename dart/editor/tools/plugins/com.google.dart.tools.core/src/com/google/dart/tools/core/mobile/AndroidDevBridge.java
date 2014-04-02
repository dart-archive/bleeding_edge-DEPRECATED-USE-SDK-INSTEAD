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

import java.io.File;

/**
 * Instance of class {@code AndroidDevBridge} represents the AndroidDevBridge (adb) in the Android
 * SDK
 */
public class AndroidDevBridge {

  // private File adb;

  public AndroidDevBridge() {
    this(AndroidSdkManager.getManager().getAdbExecutable());
  }

  AndroidDevBridge(File adbExecutable) {
//    this.adb = adbExecutable;
  }

  public void installContentShellApk() {
    // TODO(keertip): implement this
  }

  public void launchContentShell(String url) {
    // TODO(keertip): implement this
  }

  public void scanForDevices() {
    // TODO(keertip): implement this
  }

  public void stopApplication() {
    // TODO(keertip): implement this
  }

  void setupPortForwarding() {
    // TODO(keertip): implement this
  }

}
