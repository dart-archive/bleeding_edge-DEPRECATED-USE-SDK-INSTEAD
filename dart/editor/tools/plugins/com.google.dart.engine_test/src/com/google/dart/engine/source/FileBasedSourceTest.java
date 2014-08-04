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
package com.google.dart.engine.source;

import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.utilities.os.OSUtilities;

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;

public class FileBasedSourceTest extends TestCase {
  public void test_equals_false_differentFiles() {
    File file1 = createFile("/does/not/exist1.dart");
    File file2 = createFile("/does/not/exist2.dart");
    FileBasedSource source1 = new FileBasedSource(file1);
    FileBasedSource source2 = new FileBasedSource(file2);
    assertFalse(source1.equals(source2));
  }

  public void test_equals_false_null() {
    File file = createFile("/does/not/exist1.dart");
    FileBasedSource source1 = new FileBasedSource(file);
    assertFalse(source1.equals(null));
  }

  public void test_equals_true() {
    File file1 = createFile("/does/not/exist.dart");
    File file2 = createFile("/does/not/exist.dart");
    FileBasedSource source1 = new FileBasedSource(file1);
    FileBasedSource source2 = new FileBasedSource(file2);
    assertTrue(source1.equals(source2));
  }

  public void test_getEncoding() {
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    String fullPath = "/does/not/exist.dart";
    File file = createFile(fullPath);
    FileBasedSource source = new FileBasedSource(file);
    assertEquals(source, factory.fromEncoding(source.getEncoding()));
  }

  public void test_getFullName() {
    String fullPath = "/does/not/exist.dart";
    File file = createFile(fullPath);
    FileBasedSource source = new FileBasedSource(file);
    assertEquals(file.getAbsolutePath(), source.getFullName());
  }

  public void test_getShortName() {
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(file);
    assertEquals("exist.dart", source.getShortName());
  }

  public void test_hashCode() {
    File file1 = createFile("/does/not/exist.dart");
    File file2 = createFile("/does/not/exist.dart");
    FileBasedSource source1 = new FileBasedSource(file1);
    FileBasedSource source2 = new FileBasedSource(file2);
    assertEquals(source1.hashCode(), source2.hashCode());
  }

  public void test_isInSystemLibrary_contagious() throws Exception {
    File sdkDirectory = DirectoryBasedDartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    DartSdk sdk = new DirectoryBasedDartSdk(sdkDirectory);
    UriResolver resolver = new DartUriResolver(sdk);
    SourceFactory factory = new SourceFactory(resolver);
    // resolve dart:core
    Source result = resolver.resolveAbsolute(new URI("dart:core"));
    assertNotNull(result);
    assertTrue(result.isInSystemLibrary());
    // system libraries reference only other system libraries
    Source partSource = factory.resolveUri(result, "num.dart");
    assertNotNull(partSource);
    assertTrue(partSource.isInSystemLibrary());
  }

  public void test_isInSystemLibrary_false() {
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(file);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertFalse(source.isInSystemLibrary());
  }

  public void test_issue14500() throws Exception {
    // see https://code.google.com/p/dart/issues/detail?id=14500
    FileBasedSource source = new FileBasedSource(createFile("/some/packages/foo:bar.dart"));
    assertNotNull(source);
    assertFalse(source.exists());
  }

  public void test_resolveRelative_dart_fileName() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("dart:test"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("lib.dart"));
    assertNotNull(relative);
    assertEquals("dart:test/lib.dart", relative.toString());
  }

  public void test_resolveRelative_dart_filePath() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("dart:test"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("c/lib.dart"));
    assertNotNull(relative);
    assertEquals("dart:test/c/lib.dart", relative.toString());
  }

  public void test_resolveRelative_dart_filePathWithParent() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("dart:test/b/test.dart"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("../c/lib.dart"));
    assertNotNull(relative);
    assertEquals("dart:test/c/lib.dart", relative.toString());
  }

  public void test_resolveRelative_file_fileName() throws Exception {
    if (OSUtilities.isWindows()) {
      // On Windows, the URI that is produced includes a drive letter, which I believe is not
      // consistent across all machines that might run this test.
      return;
    }
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("lib.dart"));
    assertNotNull(relative);
    assertEquals("file:/a/b/lib.dart", relative.toString());
  }

  public void test_resolveRelative_file_filePath() throws Exception {
    if (OSUtilities.isWindows()) {
      // On Windows, the URI that is produced includes a drive letter, which I believe is not
      // consistent across all machines that might run this test.
      return;
    }
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("c/lib.dart"));
    assertNotNull(relative);
    assertEquals("file:/a/b/c/lib.dart", relative.toString());
  }

  public void test_resolveRelative_file_filePathWithParent() throws Exception {
    if (OSUtilities.isWindows()) {
      // On Windows, the URI that is produced includes a drive letter, which I believe is not
      // consistent across all machines that might run this test.
      return;
    }
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("../c/lib.dart"));
    assertNotNull(relative);
    assertEquals("file:/a/c/lib.dart", relative.toString());
  }

  public void test_resolveRelative_package_fileName() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("package:b/test.dart"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("lib.dart"));
    assertNotNull(relative);
    assertEquals("package:b/lib.dart", relative.toString());
  }

  public void test_resolveRelative_package_fileNameWithoutPackageName() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("package:test.dart"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("lib.dart"));
    assertNotNull(relative);
    assertEquals("package:lib.dart", relative.toString());
  }

  public void test_resolveRelative_package_filePath() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("package:b/test.dart"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("c/lib.dart"));
    assertNotNull(relative);
    assertEquals("package:b/c/lib.dart", relative.toString());
  }

  public void test_resolveRelative_package_filePathWithParent() throws Exception {
    File file = createFile("/a/b/test.dart");
    FileBasedSource source = new FileBasedSource(new URI("package:a/b/test.dart"), file);
    assertNotNull(source);
    URI relative = source.resolveRelative(new URI("../c/lib.dart"));
    assertNotNull(relative);
    assertEquals("package:a/c/lib.dart", relative.toString());
  }

  public void test_system() throws Exception {
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(new URI("dart:core"), file);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertTrue(source.isInSystemLibrary());
  }
}
