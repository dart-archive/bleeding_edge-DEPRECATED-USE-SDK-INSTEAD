/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.sdk;

import junit.framework.TestCase;

import java.io.File;

public class DartSdkTest extends TestCase {
  public void fail_getDocFileFor() {
    DartSdk sdk = createDartSdk();
    File docFile = sdk.getDocFileFor("html");
    assertNotNull(docFile);
  }

  public void test_creation() {
    DartSdk sdk = createDartSdk();
    assertNotNull(sdk);
  }

  public void test_getDartiumWorkingDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getDartiumWorkingDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getDocDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getDocDirectory();
    assertNotNull(directory);
  }

  public void test_getLibraryDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getLibraryDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getPackageDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getPackageDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getSdkVersion() {
    DartSdk sdk = createDartSdk();
    String version = sdk.getSdkVersion();
    assertNotNull(version);
    assertTrue(version.length() > 0);
  }

  public void test_getVmExecutable() {
    DartSdk sdk = createDartSdk();
    File executable = sdk.getVmExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  public void xtest_getDartiumExecutable() {
    // There is no Dartium executable in a run-time workbench
    DartSdk sdk = createDartSdk();
    File executable = sdk.getDartiumExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  private DartSdk createDartSdk() {
    File sdkDirectory = DartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    return new DartSdk(sdkDirectory);
  }
}
