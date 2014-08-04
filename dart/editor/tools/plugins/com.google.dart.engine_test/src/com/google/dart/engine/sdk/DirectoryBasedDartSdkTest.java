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

import com.google.dart.engine.source.Source;

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

  public void test_fromFile_invalid() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    assertNull(sdk.fromFileUri(new File("/not/in/the/sdk.dart").toURI()));
  }

  public void test_fromFile_library() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    Source source = sdk.fromFileUri(new File(sdk.getLibraryDirectory(), "core/core.dart").toURI());
    assertNotNull(source);
    assertTrue(source.isInSystemLibrary());
    assertEquals("dart:core", source.getUri().toString());
  }

  public void test_fromFile_part() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    Source source = sdk.fromFileUri(new File(sdk.getLibraryDirectory(), "core/num.dart").toURI());
    assertNotNull(source);
    assertTrue(source.isInSystemLibrary());
    assertEquals("dart:core/num.dart", source.getUri().toString());
  }

  // These tests fail if Dartium is not present - I don't believe we want to test that at this level.
//  public void test_getDartiumExecutable() throws Exception {
//    DirectoryBasedDartSdk sdk = createDartSdk();
//    File file = sdk.getDartiumExecutable();
//    assertNotNull(file);
//    assertTrue(file.exists());
//  }
//
//  public void test_getDartiumWorkingDirectory() {
//    DirectoryBasedDartSdk sdk = createDartSdk();
//    File directory = sdk.getDartiumWorkingDirectory();
//    assertNotNull(directory);
//    assertTrue(directory.exists());
//  }

  public void test_getDart2JsExecutable() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File executable = sdk.getDart2JsExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
  }

  public void test_getDartFmtExecutable() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File executable = sdk.getDartFmtExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
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

  public void test_getPubExecutable() {
    DirectoryBasedDartSdk sdk = createDartSdk();
    File executable = sdk.getPubExecutable();
    assertNotNull(executable);
    assertTrue(executable.exists());
    assertTrue(executable.canExecute());
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

  private DirectoryBasedDartSdk createDartSdk() {
    File sdkDirectory = DirectoryBasedDartSdk.getDefaultSdkDirectory();
    assertNotNull(
        "No SDK configured; set the property 'com.google.dart.sdk' on the command line",
        sdkDirectory);
    return new DirectoryBasedDartSdk(sdkDirectory);
  }
}
