/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.utilities.io;

import junit.framework.TestCase;

import java.io.File;

public class FileUtilitiesTest extends TestCase {
  public void test_FileUtilities_deriveFileName_allInvalid() {
    assertEquals("", FileUtilities.deriveFileName(":\\:"));
  }

  public void test_FileUtilities_deriveFileName_allValid() {
    String fileName = "money.dart";
    assertEquals(fileName, FileUtilities.deriveFileName(fileName));
  }

  public void test_FileUtilities_deriveFileName_empty() {
    assertEquals("", FileUtilities.deriveFileName(""));
  }

  public void test_FileUtilities_deriveFileName_mixed() {
    assertEquals("shipIt", FileUtilities.deriveFileName("s:/hi/p?<I t>"));
  }

  public void test_FileUtilities_deriveFileName_null() {
    assertEquals(null, FileUtilities.deriveFileName(null));
  }

  public void test_FileUtilities_getBaseName_extension() {
    String baseName = "money";
    assertEquals(baseName, FileUtilities.getBaseName(new File(baseName + ".dart")));
  }

  public void test_FileUtilities_getBaseName_noExtension() {
    String baseName = "money";
    assertEquals(baseName, FileUtilities.getBaseName(new File(baseName)));
  }

  public void test_FileUtilities_getExtension_extension() {
    String extension = "dart";
    assertEquals(extension, FileUtilities.getExtension(new File("money." + extension)));
  }

  public void test_FileUtilities_getExtension_noExtension() {
    assertEquals("", FileUtilities.getExtension(new File("money")));
  }

  public void test_FileUtilities_getUniqueFile() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    String prefix = "README";
    String suffix = ".txt";
    File uniqueFile = FileUtilities.getUniqueFile(tempDir, prefix, suffix);
    assertNotNull(uniqueFile);
    assertFalse(uniqueFile.exists());
    String fileName = uniqueFile.getName();
    assertTrue(fileName.startsWith(prefix));
    assertTrue(fileName.endsWith(suffix));
  }
}
