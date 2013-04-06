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

public class DirectoryBasedDartSdkTest extends TestCase {
  public void fail_getDocFileFor() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File docFile = sdk.getDocFileFor("html");
    assertNotNull(docFile);
  }

  public void test_creation() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    assertNotNull(sdk);
  }

  public void test_getDartiumExecutable() throws Exception {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File file = sdk.getDartiumExecutable();
    assertNotNull(file);
    assertTrue(file.exists());
  }

  public void test_getDartiumWorkingDirectory() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File directory = sdk.getDartiumWorkingDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getDirectory() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File directory = sdk.getDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getDocDirectory() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File directory = sdk.getDocDirectory();
    assertNotNull(directory);
  }

  public void test_getLibraryDirectory() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File directory = sdk.getLibraryDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_getSdkVersion() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    String version = sdk.getSdkVersion();
    assertNotNull(version);
    assertTrue(version.length() > 0);
  }

  public void test_getVmExecutable() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File executable = sdk.getVmExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  public void xtest_getDartiumExecutable() {
    // There is no Dartium executable in a run-time workbench
    DirectoryBasedDartSdk sdk = createDartSdk();
    File executable = sdk.getDartiumExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  private DirectoryBasedDartSdk createDartSdk() {
    File sdkDirectory = DirectoryBasedDartSdk.getDefaultSdkDirectory();
    assertNotNull(
        "No SDK configured; set the property 'com.google.dart.sdk' on the command line",
        sdkDirectory);
    return new DirectoryBasedDartSdk(sdkDirectory);
  }
}
