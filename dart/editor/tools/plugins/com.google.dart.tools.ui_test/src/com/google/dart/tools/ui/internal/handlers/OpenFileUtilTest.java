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
package com.google.dart.tools.ui.internal.handlers;

import com.google.common.io.CharStreams;
import com.google.dart.tools.core.OpenFileUtil;
import com.google.dart.tools.core.internal.model.DartModelImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.util.Extensions;
import com.google.dart.tools.core.internal.util.ResourceUtil;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Random;

public class OpenFileUtilTest extends TestCase {
  private static File USER_HOME = new File(System.getProperty("user.home"));
  private File tempDir = null;

//  public void testOpenFileUtil_getFirstAppOrLibFile_app() throws Exception {
//    File file = makeFile("someApp" + Extensions.DOT_APP, "random-content");
//    File actual = OpenFileUtil.getFirstAppOrLib(file);
//    assertNotNull(actual);
//    assertEquals(file.getAbsolutePath(), actual.getAbsolutePath());
//  }

  public void testOpenFileUtil_getFirstAppOrLibFile_doesNotExist() {
    File file = new File(USER_HOME, "doesNotExist" + Extensions.DOT_DART);
    File actual = OpenFileUtil.getFirstAppOrLib(file);
    assertNull(actual);
  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_lib() throws Exception {
//    File file = makeFile("someLib" + Extensions.DOT_LIB, "random-content");
//    File actual = OpenFileUtil.getFirstAppOrLib(file);
//    assertNotNull(actual);
//    assertEquals(file.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_notPartOfLibrary() throws Exception {
//    File dartFile = makeFile("SomeFile" + Extensions.DOT_DART, "class SomeFile { }");
//    makeFile("SomeApp" + Extensions.DOT_APP, "library { }");
//    File actual = OpenFileUtil.getFirstAppOrLib(dartFile);
//    assertNull(actual);
//  }

  public void testOpenFileUtil_getFirstAppOrLibFile_null() {
    File actual = OpenFileUtil.getFirstAppOrLib(null);
    assertNull(actual);
  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_oddPath1() throws Exception {
//    String resRelPath = "SomeResource.txt";
//    File resFile = makeFile(resRelPath, "a resource file");
//    File libFile = makeFile("SomeLib" + Extensions.DOT_LIB, "library { resource = ['src/../"
//        + resRelPath + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(resFile);
//    assertNotNull(actual);
//    assertEquals(libFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_oddPath2() throws Exception {
//    String resRelPath = "SomeResource.txt";
//    File resFile = makeFile(resRelPath, "a resource file");
//    File libFile = makeFile("SomeLib" + Extensions.DOT_LIB, "library { resource = ['./"
//        + resRelPath + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(resFile);
//    assertNotNull(actual);
//    assertEquals(libFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_oddPath3() throws Exception {
//    String resRelPath = "SomeResource.txt";
//    File resFile = makeFile(resRelPath, "a resource file");
//    File libFile = makeFile("SomeLib" + Extensions.DOT_LIB, "library { resource = ['./src/../"
//        + resRelPath + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(resFile);
//    assertNotNull(actual);
//    assertEquals(libFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_oddPath4() throws Exception {
//    String dartRelPath = "src/SomeFile" + Extensions.DOT_DART;
//    File dartFile = makeFile(dartRelPath, "class SomeFile { }");
//    File appFile = makeFile("SomeApp" + Extensions.DOT_APP, "library { source = ['./src/../"
//        + dartRelPath + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(dartFile);
//    assertNotNull(actual);
//    assertEquals(appFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_resourcePartOfLibInSameDir() throws Exception {
//    String resRelPath = "SomeResource.txt";
//    File resFile = makeFile(resRelPath, "a resource file");
//    File libFile = makeFile("SomeLib" + Extensions.DOT_LIB, "library { resource = ['" + resRelPath
//        + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(resFile);
//    assertNotNull(actual);
//    assertEquals(libFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_sourcePartOfAppInParentDir() throws Exception {
//    String dartRelPath = "src/SomeFile" + Extensions.DOT_DART;
//    File dartFile = makeFile(dartRelPath, "class SomeFile { }");
//    File appFile = makeFile("SomeApp" + Extensions.DOT_APP, "library { source = ['" + dartRelPath
//        + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(dartFile);
//    assertNotNull(actual);
//    assertEquals(appFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

//  public void testOpenFileUtil_getFirstAppOrLibFile_sourcePartOfLibInSameDir() throws Exception {
//    String dartRelPath = "SomeFile" + Extensions.DOT_DART;
//    File dartFile = makeFile(dartRelPath, "class SomeFile { }");
//    File libFile = makeFile("SomeLib" + Extensions.DOT_LIB, "library { source = ['" + dartRelPath
//        + "']}");
//    File actual = OpenFileUtil.getFirstAppOrLib(dartFile);
//    assertNotNull(actual);
//    assertEquals(libFile.getAbsolutePath(), actual.getAbsolutePath());
//  }

  public void testOpenFileUtil_getOrCreateResource_1() throws Exception {
    File file = makeFile("file.txt", "a file");
    DartModelImpl model = DartModelManager.getInstance().getDartModel();
    int originalCount = model.getDartProjects().length;

    IFile res = ResourceUtil.getResource(file);
    assertNull(res);
    res = OpenFileUtil.getOrCreateResource(file, new NullProgressMonitor());
    assertNotNull(res);
    assertTrue(res.exists());
    assertEquals(file.getAbsolutePath(), res.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);
  }

  public void testOpenFileUtil_getOrCreateResource_2() throws Exception {
    File file1 = makeFile("file1.txt", "first file");
    File file2 = makeFile("file2.txt", "second file");
    DartModelImpl model = DartModelManager.getInstance().getDartModel();
    int originalCount = model.getDartProjects().length;

    IFile res1 = ResourceUtil.getResource(file1);
    assertNull(res1);
    res1 = OpenFileUtil.getOrCreateResource(file1, new NullProgressMonitor());
    assertNotNull(res1);
    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);

    IFile res2 = ResourceUtil.getResource(file2);
    assertNotNull(res2);
    assertTrue(res2.exists());
    assertEquals(file2.getAbsolutePath(), res2.getLocation().toOSString());
    res2 = OpenFileUtil.getOrCreateResource(file2, new NullProgressMonitor());
    assertNotNull(res2);
    assertTrue(res2.exists());
    assertEquals(file2.getAbsolutePath(), res2.getLocation().toOSString());

    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);
  }

  public void testOpenFileUtil_getOrCreateResource_overlapping() throws Exception {
    File file1 = makeFile("src/file1.txt", "first file");
    File file2 = makeFile("file2.txt", "second file");
    DartModelImpl model = DartModelManager.getInstance().getDartModel();
    int originalCount = model.getDartProjects().length;

    IFile res1 = ResourceUtil.getResource(file1);
    assertNull(res1);
    res1 = OpenFileUtil.getOrCreateResource(file1, new NullProgressMonitor());
    assertNotNull(res1);
    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);

    IFile res2 = ResourceUtil.getResource(file2);
    assertNull(res2);
    res2 = OpenFileUtil.getOrCreateResource(file2, new NullProgressMonitor());
    assertNotNull(res2);
    assertTrue(res2.exists());
    assertEquals(file2.getAbsolutePath(), res2.getLocation().toOSString());

    // overlapping linked folders are removed, so file1 exists within the newly linked folder
    assertFalse(res1.exists());
    res1 = OpenFileUtil.getOrCreateResource(file1, new NullProgressMonitor());
    assertNotNull(res1);
    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);
  }

  public void testOpenFileUtil_getOrCreateResource_overlapping2() throws Exception {
    File file1 = makeFile("src1/file1.txt", "first file");
    File file2 = makeFile("src2/file2.txt", "second file");
    File file3 = makeFile("file3.txt", "third file");
    DartModelImpl model = DartModelManager.getInstance().getDartModel();
    int originalCount = model.getDartProjects().length;

    IFile res1 = ResourceUtil.getResource(file1);
    assertNull(res1);
    res1 = OpenFileUtil.getOrCreateResource(file1, new NullProgressMonitor());
    assertNotNull(res1);
    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);

    IFile res2 = ResourceUtil.getResource(file2);
    assertNull(res2);
    res2 = OpenFileUtil.getOrCreateResource(file2, new NullProgressMonitor());
    assertNotNull(res2);
    assertTrue(res2.exists());
    assertEquals(file2.getAbsolutePath(), res2.getLocation().toOSString());

    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    assertEquals(originalCount + 2, model.getDartProjects().length);

    IFile res3 = ResourceUtil.getResource(file3);
    assertNull(res3);
    res3 = OpenFileUtil.getOrCreateResource(file3, new NullProgressMonitor());
    assertNotNull(res3);
    assertTrue(res3.exists());
    assertEquals(file3.getAbsolutePath(), res3.getLocation().toOSString());

    // overlapping linked folders are removed, so file1 exists within the newly linked folder
    assertFalse(res1.exists());
    res1 = OpenFileUtil.getOrCreateResource(file1, new NullProgressMonitor());
    assertNotNull(res1);
    assertTrue(res1.exists());
    assertEquals(file1.getAbsolutePath(), res1.getLocation().toOSString());

    // overlapping linked folders are removed, so file2 exists within the newly linked folder
    assertFalse(res2.exists());
    res2 = OpenFileUtil.getOrCreateResource(file2, new NullProgressMonitor());
    assertNotNull(res2);
    assertTrue(res2.exists());
    assertEquals(file2.getAbsolutePath(), res2.getLocation().toOSString());

    assertEquals(originalCount + 1, model.getDartProjects().length);
  }

  @Override
  protected void tearDown() throws Exception {
    recursiveDelete(tempDir);
  }

  private File makeFile(String relPath, String content) throws IOException {
    if (tempDir == null) {
      do {
        tempDir = new File(System.getProperty("user.home"), getClass().getSimpleName() + "-"
            + new Random().nextLong());
      } while (tempDir.exists());
      if (!tempDir.mkdirs()) {
        fail("Failed to create temp directory: " + tempDir);
      }
    }
    File file = new File(tempDir, relPath);
    File dir = file.getParentFile();
    if (!dir.exists() && !dir.mkdirs()) {
      fail("Failed to create temp directory: " + dir);
    }
    FileWriter writer = new FileWriter(file);
    try {
      CharStreams.copy(new StringReader(content), writer);
    } finally {
      writer.close();
    }
    return file;
  }

  private void recursiveDelete(File file) {
    if (file == null || !file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        recursiveDelete(child);
      }
    }
    if (!file.delete()) {
      file.deleteOnExit();
    }
  }
}
