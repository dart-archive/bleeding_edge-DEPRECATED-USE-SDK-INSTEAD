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

import static com.google.dart.engine.utilities.io.FileUtilities2.createFile;

import junit.framework.TestCase;

import java.io.File;

public class FileBasedSourceTest extends TestCase {
  public void test_equals_false() {
    SourceFactory factory = new SourceFactory();
    File file1 = createFile("/does/not/exist1.dart");
    File file2 = createFile("/does/not/exist2.dart");
    FileBasedSource source1 = new FileBasedSource(factory, file1);
    FileBasedSource source2 = new FileBasedSource(factory, file2);
    assertFalse(source1.equals(source2));
  }

  public void test_equals_true() {
    SourceFactory factory = new SourceFactory();
    File file1 = createFile("/does/not/exist.dart");
    File file2 = createFile("/does/not/exist.dart");
    FileBasedSource source1 = new FileBasedSource(factory, file1);
    FileBasedSource source2 = new FileBasedSource(factory, file2);
    assertTrue(source1.equals(source2));
  }

  public void test_getFullName() {
    SourceFactory factory = new SourceFactory();
    String fullPath = "/does/not/exist.dart";
    File file = createFile(fullPath);
    FileBasedSource source = new FileBasedSource(factory, file);
    assertEquals(file.getAbsolutePath(), source.getFullName());
  }

  public void test_getShortName() {
    SourceFactory factory = new SourceFactory();
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(factory, file);
    assertEquals("exist.dart", source.getShortName());
  }

  public void test_hashCode() {
    SourceFactory factory = new SourceFactory();
    File file1 = createFile("/does/not/exist.dart");
    File file2 = createFile("/does/not/exist.dart");
    FileBasedSource source1 = new FileBasedSource(factory, file1);
    FileBasedSource source2 = new FileBasedSource(factory, file2);
    assertEquals(source1.hashCode(), source2.hashCode());
  }

  public void test_isInSystemLibrary_false() {
    SourceFactory factory = new SourceFactory();
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(factory, file);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertFalse(source.isInSystemLibrary());
  }

  public void test_resolve_absolute() {
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    File file = createFile("/does/not/exist1.dart");
    FileBasedSource source = new FileBasedSource(factory, file);
    Source result = source.resolve("file:///invalid/path.dart");
    assertEquals(createFile("/invalid/path.dart").getAbsolutePath(), result.getFullName());
  }

  public void test_resolve_relative() {
    SourceFactory factory = new SourceFactory(new FileUriResolver());
    File file = createFile("/does/not/exist1.dart");
    FileBasedSource source = new FileBasedSource(factory, file);
    Source result = source.resolve("exist2.dart");
    assertNotNull(result);
    assertEquals(createFile("/does/not/exist2.dart").getAbsolutePath(), result.getFullName());
  }

  public void test_system() {
    SourceFactory factory = new SourceFactory();
    File file = createFile("/does/not/exist.dart");
    FileBasedSource source = new FileBasedSource(factory, file, true);
    assertNotNull(source);
    assertEquals(file.getAbsolutePath(), source.getFullName());
    assertTrue(source.isInSystemLibrary());
  }
}
