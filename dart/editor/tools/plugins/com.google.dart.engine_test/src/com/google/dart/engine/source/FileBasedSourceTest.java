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

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;
import java.net.URI;

public class FileBasedSourceTest extends TestCase {
  public void test_equals_false_differentFiles() {
    ContentCache contentCache = new ContentCache();
    File file1 = createFile("/does/not/exist1.dart");
    File file2 = createFile("/does/not/exist2.dart");
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    FileBasedSource source2 = new FileBasedSource(contentCache, file2);
    assertFalse(source1.equals(source2));
  }

  public void test_equals_false_null() {
    ContentCache contentCache = new ContentCache();
    File file1 = createFile("/does/not/exist1.dart");
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    assertFalse(source1.equals(null));
  }

  public void test_equals_true() {
    ContentCache contentCache = new ContentCache();
    File file1 = createFile("/does/not/exist.dart");
    File file2 = createFile("/does/not/exist.dart");
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    FileBasedSource source2 = new FileBasedSource(contentCache, file2);
    assertTrue(source1.equals(source2));
  }

  public void test_getEncoding() {
    ContentCache contentCache = new ContentCache();
    SourceFactory factory = new SourceFactory(contentCache, new FileUriResolver());
    String fullPath = "/does/not/exist.dart";
    File file = createFile(fullPath);
    FileBasedSource source = new FileBasedSource(contentCache, file);
    assertEquals(source, factory.fromEncoding(source.getEncoding()));
  }

  public void test_getFullName() {
    ContentCache contentCache = new ContentCache();
    String fullPath = "/does/not/exist.dart";
    File file = createFile(fullPath);
    FileBasedSource source = new FileBasedSource(contentCache, file);
    assertEquals(file.getAbsolutePath(), source.getFullName());
  }

  public void test_getModificationStamp() {
    ContentCache contentCache = new ContentCache();
    String fullPath = "/does/not/exist.dart";
    File file = new File(createFile(fullPath).getAbsolutePath()) {
      // Adjust the time for systems that run too quickly.
      private long modified = System.currentTimeMillis() - 1000;

      @Override
      public long lastModified() {
        return modified;
      }
    };
    FileBasedSource source = new FileBasedSource(contentCache, file);
    long firstStamp = source.getModificationStamp();
    contentCache.setContents(source, "");
    long secondStamp = source.getModificationStamp();
    assertTrue(secondStamp != firstStamp);
    contentCache.setContents(source, null);
    long thirdStamp = source.getModificationStamp();
    assertTrue(thirdStamp != secondStamp);
  }

  public void test_getShortName() {
    ContentCache contentCache = new ContentCache();
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(contentCache, file);
    assertEquals("exist.dart", source.getShortName());
  }

  public void test_hashCode() {
    ContentCache contentCache = new ContentCache();
    File file1 = createFile("/does/not/exist.dart");
    File file2 = createFile("/does/not/exist.dart");
    FileBasedSource source1 = new FileBasedSource(contentCache, file1);
    FileBasedSource source2 = new FileBasedSource(contentCache, file2);
    assertEquals(source1.hashCode(), source2.hashCode());
  }

  public void test_isInSystemLibrary_contagious() throws Exception {
    ContentCache contentCache = new ContentCache();
    File sdkDirectory = DirectoryBasedDartSdk.getDefaultSdkDirectory();
    assertNotNull(sdkDirectory);
    DartSdk sdk = new DirectoryBasedDartSdk(sdkDirectory);
    UriResolver resolver = new DartUriResolver(sdk);
    // resolve dart:core
    Source result = resolver.resolveAbsolute(contentCache, new URI("dart:core"));
    assertNotNull(result);
    assertTrue(result.isInSystemLibrary());
    // system libraries reference only other system libraries
    Source partSource = result.resolveRelative(new URI("num.dart"));
    assertNotNull(partSource);
    assertTrue(partSource.isInSystemLibrary());
  }

  public void test_isInSystemLibrary_false() {
    ContentCache contentCache = new ContentCache();
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(contentCache, file);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertFalse(source.isInSystemLibrary());
  }

  public void test_system() {
    ContentCache contentCache = new ContentCache();
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(contentCache, file, true);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertTrue(source.isInSystemLibrary());
  }
}
