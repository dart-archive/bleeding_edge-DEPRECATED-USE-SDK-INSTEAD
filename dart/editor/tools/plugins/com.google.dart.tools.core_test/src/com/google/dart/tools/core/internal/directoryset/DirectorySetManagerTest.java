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
package com.google.dart.tools.core.internal.directoryset;

import com.google.dart.tools.core.test.util.FileOperation;
import com.google.dart.tools.core.test.util.TestUtilities;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.File;

public class DirectorySetManagerTest extends TestCase {
  /**
   * Instances of the class <code>TestListener</code> implement a directory set listener that can
   * confirm the number of times an event was fired.
   */
  private static class TestListener implements DirectorySetListener {
    private int fireCount = 0;

    public void assertFireCount(int expectedCount) {
      Assert.assertEquals("incorrect number of events fired", expectedCount, fireCount);
    }

    @Override
    public void directorySetChange(DirectorySetEvent event) {
      fireCount++;
    }
  }

  public void test_DirectorySetManager_addListener() {
    DirectorySetManager manager = DirectorySetManager.getInstance();
    TestListener listener = new TestListener();
    try {
      manager.addListener(listener);
      manager.fire();
      listener.assertFireCount(1);
    } finally {
      manager.removeListener(listener);
    }
  }

  public void test_DirectorySetManager_addPath_child() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File file) throws Exception {
        DirectorySetManager manager = DirectorySetManager.getInstance();
        TestListener listener = new TestListener();
        String parentPath = file.getAbsolutePath();
        File childDir = new File(file, "childDir");
        childDir.mkdirs();
        String childPath = childDir.getAbsolutePath();
        assertFalse("invalid test: parent path already exists", manager.hasPath(parentPath));
        assertFalse("invalid test: child path already exists", manager.hasPath(childPath));
        try {
          manager.addPath(parentPath);
          manager.addListener(listener);
          boolean addResult = manager.addPath(childPath);
          boolean hasParentResult = manager.hasPath(parentPath);
          boolean hasChildResult = manager.hasPath(childPath);
          manager.removePath(parentPath);
          assertFalse("child path added", addResult);
          assertTrue("has parent path", hasParentResult);
          assertFalse("has child path", hasChildResult);
          listener.assertFireCount(1);
        } finally {
          manager.removeListener(listener);
        }
      }
    });
  }

  public void test_DirectorySetManager_addPath_nonExistent() {
    DirectorySetManager manager = DirectorySetManager.getInstance();
    TestListener listener = new TestListener();
    String basePath = "/this/path/does/not/exist";
    String path = basePath;
    int index = 1;
    while (new File(path).exists()) {
      path = basePath + index++;
    }
    assertFalse("non-existent path already exists", manager.hasPath(path));
    try {
      manager.addListener(listener);
      boolean result = manager.addPath(path);
      manager.removePath(path);
      assertFalse("non-existent path added", result);
      listener.assertFireCount(0);
    } finally {
      manager.removeListener(listener);
    }
  }

  public void test_DirectorySetManager_addPath_parent() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File file) throws Exception {
        DirectorySetManager manager = DirectorySetManager.getInstance();
        TestListener listener = new TestListener();
        String parentPath = file.getAbsolutePath();
        File childDir1 = new File(file, "childDir1");
        File childDir2 = new File(file, "childDir2");
        childDir1.mkdirs();
        childDir2.mkdirs();
        String childPath1 = childDir1.getAbsolutePath();
        String childPath2 = childDir2.getAbsolutePath();
        assertFalse("invalid test: parent path already exists", manager.hasPath(parentPath));
        assertFalse("invalid test: first child path already exists", manager.hasPath(childPath1));
        assertFalse("invalid test: second child path already exists", manager.hasPath(childPath2));
        try {
          manager.addPath(childPath1);
          manager.addPath(childPath2);
          manager.addListener(listener);
          boolean addResult = manager.addPath(parentPath);
          boolean hasParentResult = manager.hasPath(parentPath);
          boolean hasChildResult1 = manager.hasPath(childPath1);
          boolean hasChildResult2 = manager.hasPath(childPath2);
          manager.removePath(parentPath);
          assertTrue("parent path not added", addResult);
          assertTrue("has parent path", hasParentResult);
          assertFalse("has first child path", hasChildResult1);
          assertFalse("has second child path", hasChildResult2);
          listener.assertFireCount(2);
        } finally {
          manager.removeListener(listener);
        }
      }
    });
  }

  public void test_DirectorySetManager_addPath_unique() throws Exception {
    TestUtilities.runWithTempDirectory(new FileOperation() {
      @Override
      public void run(File file) throws Exception {
        DirectorySetManager manager = DirectorySetManager.getInstance();
        TestListener listener = new TestListener();
        String path = file.getAbsolutePath();
        assertFalse("unique path already exists", manager.hasPath(path));
        try {
          manager.addListener(listener);
          boolean result = manager.addPath(path);
          manager.removePath(path);
          assertTrue("valid path not added", result);
          listener.assertFireCount(2);
        } finally {
          manager.removeListener(listener);
        }
      }
    });
  }

  public void test_DirectorySetManager_getChildren() {
    DirectorySetManager manager = DirectorySetManager.getInstance();
    File[] children = manager.getChildren();
    assertNotNull(children);
    for (File file : children) {
      assertNotNull(file);
      assertTrue(file.exists());
      assertFalse(file.isHidden());
    }
  }

  public void test_DirectorySetManager_getInstance() {
    assertNotNull(DirectorySetManager.getInstance());
  }
}
