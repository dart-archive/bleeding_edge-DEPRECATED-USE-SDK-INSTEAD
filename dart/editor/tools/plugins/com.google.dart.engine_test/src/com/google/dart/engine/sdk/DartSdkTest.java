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
  public void test_DartSdk() {
    DartSdk sdk = createDartSdk();
    assertNotNull(sdk);
  }

  public void test_DartSdk_getDartiumVersion() {
    DartSdk sdk = createDartSdk();
    String version = sdk.getDartiumVersion();
    assertNotNull(version);
    assertTrue(version.length() > 0);
  }

  public void test_DartSdk_getDartiumWorkingDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getDartiumWorkingDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_DartSdk_getDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_DartSdk_getDocDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getDocDirectory();
    assertNotNull(directory);
  }

  public void test_DartSdk_getLibrariesForPlatform() {
    DartSdk sdk = createDartSdk();
    Platform[] platforms = sdk.getSupportedPlatforms();
    assertNotNull(platforms);
    for (Platform platform : platforms) {
      DartSdk.LibraryMap libraries = sdk.getLibrariesForPlatform(platform);
      assertNotNull(libraries);
      String[] uris = libraries.getUris();
      assertNotNull(uris);
      assertTrue(uris.length > 0);
      for (String uri : uris) {
        File file = libraries.mapDartUri(uri);
        assertNotNull(file);
        assertTrue(file.exists());
      }
    }
  }

  public void test_DartSdk_getLibraryDirectory() {
    DartSdk sdk = createDartSdk();
    File directory = sdk.getLibraryDirectory();
    assertNotNull(directory);
    assertTrue(directory.exists());
  }

  public void test_DartSdk_getSdkVersion() {
    DartSdk sdk = createDartSdk();
    String version = sdk.getSdkVersion();
    assertNotNull(version);
    assertTrue(version.length() > 0);
  }

  public void test_DartSdk_getSupportedPlatforms() {
    DartSdk sdk = createDartSdk();
    Platform[] platforms = sdk.getSupportedPlatforms();
    assertNotNull(platforms);
    assertEquals(4, platforms.length);
  }

  public void test_DartSdk_getVmExecutable() {
    DartSdk sdk = createDartSdk();
    File executable = sdk.getVmExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  public void xtest_DartSdk_getDartiumExecutable() {
    // There is no Dartium executable in a run-time workbench
    DartSdk sdk = createDartSdk();
    File executable = sdk.getDartiumExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  public void xtest_DartSdk_getDocFileFor() {
    // Enable this test when documentation files are being produced.
    DartSdk sdk = createDartSdk();
    File docFile = sdk.getDocFileFor("html");
    assertNotNull(docFile);
  }

  private DartSdk createDartSdk() {
    File sdkDirectory = DartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    return new DartSdk(sdkDirectory);
  }
}
