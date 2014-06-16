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
package com.google.dart.engine.internal.index.file;

import com.google.common.io.Files;
import com.google.dart.engine.utilities.io.FileUtilities2;

import junit.framework.TestCase;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class SeparateFileManagerTest extends TestCase {
  private File tempDir;
  private SeparateFileManager fileManager;

  public void test_clear() throws Exception {
    String name = "42.index";
    // create a file
    fileManager.openOutputStream(name).close();
    // check that that file exists
    assertTrue(new File(tempDir, name).isFile());
    // clear
    fileManager.clear();
    assertFalse(new File(tempDir, name).isFile());
  }

  public void test_outputInput() throws Exception {
    String name = "42.index";
    // create a file
    {
      OutputStream stream = fileManager.openOutputStream(name);
      assertNotNull(stream);
      try {
        stream.write(0x01);
        stream.write(0x02);
        stream.write(0x03);
        stream.write(0x04);
      } finally {
        stream.close();
      }
    }
    // check that that file exists
    assertTrue(new File(tempDir, name).isFile());
    // get InputStream
    {
      InputStream stream = fileManager.openInputStream(name);
      assertNotNull(stream);
      try {
        assertEquals(0x01, stream.read());
        assertEquals(0x02, stream.read());
        assertEquals(0x03, stream.read());
        assertEquals(0x04, stream.read());
      } finally {
        stream.close();
      }
    }
    // delete
    fileManager.delete(name);
    // no InputStream anymore
    assertNull(fileManager.openInputStream(name));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    tempDir = Files.createTempDir();
    fileManager = new SeparateFileManager(tempDir);
  }

  @Override
  protected void tearDown() throws Exception {
    FileUtilities2.deleteDirectory(tempDir);
    super.tearDown();
  }
}
