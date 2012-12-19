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
package com.google.dart.tools.core.refresh;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;

public class ResourceRefreshManagerTest extends TestCase {
  public void test_ResourceRefreshManager_creation_existingFile() throws Exception {
    File timeStore = getTimeStore();
    timeStore.delete();
    ResourceRefreshManager manager = new ResourceRefreshManager();
    assertFalse(timeStore.exists());
    manager.shutdown();
    assertTrue(timeStore.exists());
    manager = new ResourceRefreshManager();
    manager.shutdown();
  }

  public void test_ResourceRefreshManager_creation_noFile() throws Exception {
    File timeStore = getTimeStore();
    timeStore.delete();
    ResourceRefreshManager manager = new ResourceRefreshManager();
    assertFalse(timeStore.exists());
    manager.shutdown();
    assertTrue(timeStore.exists());
  }

  // TODO(devoncarew): flaky test (https://code.google.com/p/dart/issues/detail?id=7504)
//  public void test_ResourceRefreshManager_refresh() throws Exception {
//    TestUtilities.runWithTempDirectory(new FileOperation() {
//      @Override
//      public void run(File tempDirectory) throws Exception {
//        File dartFile = new File(tempDirectory, "test.dart");
//        PrintWriter writer = null;
//        try {
//          writer = new PrintWriter(new FileWriter(dartFile));
//          writer.println("#library('test');");
//          writer.println();
//          writer.println("class test {}");
//        } finally {
//          if (writer != null) {
//            writer.flush();
//            writer.close();
//          }
//        }
//        final IFile[][] addedFiles = new IFile[1][];
//        final IFile[][] modifiedFiles = new IFile[1][];
//        final IFile[][] deletedFiles = new IFile[1][];
//        ResourceRefreshManager manager = new ResourceRefreshManager();
//        manager.addResourceChangeListener(new ResourceChangeListener() {
//          @Override
//          public void resourcesChanged(ResourceChangeEvent event) {
//            addedFiles[0] = event.getAddedFiles().toArray(new IFile[0]);
//            modifiedFiles[0] = event.getModifiedFiles().toArray(new IFile[0]);
//            deletedFiles[0] = event.getDeletedFiles().toArray(new IFile[0]);
//          }
//        });
//        IProject project = DartCore.openLibrary(dartFile, null).getDartProject().getProject();
//        manager.refresh();
//
//        updateModificationTime(dartFile);
//        manager.refresh();
//        assertNotNull(addedFiles[0]);
//        assertEquals(0, addedFiles[0].length);
//        assertNotNull(modifiedFiles[0]);
//        assertEquals(1, modifiedFiles[0].length);
//        assertEquals(dartFile.getName(), modifiedFiles[0][0].getName());
//        assertNotNull(deletedFiles[0]);
//        assertEquals(0, deletedFiles[0].length);
//
//        project.delete(true, null);
//        manager.shutdown();
//      }
//    });
//  }

  // TODO(devoncarew): flaky test (https://code.google.com/p/dart/issues/detail?id=7504)
//  public void test_ResourceRefreshManager_updateWithoutNotification() throws Exception {
//    TestUtilities.runWithTempDirectory(new FileOperation() {
//      @Override
//      public void run(File tempDirectory) throws Exception {
//        File dartFile = new File(tempDirectory, "test.dart");
//        PrintWriter writer = null;
//        try {
//          writer = new PrintWriter(new FileWriter(dartFile));
//          writer.println("#library('test');");
//          writer.println();
//          writer.println("class test {}");
//        } finally {
//          if (writer != null) {
//            writer.flush();
//            writer.close();
//          }
//        }
//        final IFile[][] addedFiles = new IFile[1][];
//        final IFile[][] modifiedFiles = new IFile[1][];
//        final IFile[][] deletedFiles = new IFile[1][];
//        ResourceRefreshManager manager = new ResourceRefreshManager();
//        manager.addResourceChangeListener(new ResourceChangeListener() {
//          @Override
//          public void resourcesChanged(ResourceChangeEvent event) {
//            addedFiles[0] = event.getAddedFiles().toArray(new IFile[0]);
//            modifiedFiles[0] = event.getModifiedFiles().toArray(new IFile[0]);
//            deletedFiles[0] = event.getDeletedFiles().toArray(new IFile[0]);
//          }
//        });
//        IProject project = DartCore.openLibrary(dartFile, null).getDartProject().getProject();
//        //
//        // The manager should have been notified of the new files and updated it's state, so there
//        // should not be anything left to refresh.
//        //
//        manager.refresh();
//        assertNotNull(addedFiles[0]);
//        assertEquals(0, addedFiles[0].length);
//        assertNotNull(modifiedFiles[0]);
//        assertEquals(0, modifiedFiles[0].length);
//        assertNotNull(deletedFiles[0]);
//        assertEquals(0, deletedFiles[0].length);
//
//        project.delete(true, null);
//        manager.shutdown();
//      }
//    });
//  }

  /**
   * Return the file in which the resource refresh manager will store the modification times of
   * resources.
   * 
   * @return the file in which the manager will store the modification times of resources
   * @throws Exception if the file could not be accessed for some reason
   */
  private File getTimeStore() throws Exception {
    Method method = ResourceRefreshManager.class.getDeclaredMethod("getTimeStore");
    method.setAccessible(true);
    return (File) method.invoke(null);
  }

  @SuppressWarnings("unused")
  private void updateModificationTime(File dartFile) throws Exception {
    long initialTime = dartFile.lastModified();
    long currentTime = initialTime;
    while (currentTime == initialTime) {
      PrintWriter writer = null;
      try {
        writer = new PrintWriter(new FileWriter(dartFile));
        writer.println("#library('test');");
        writer.println();
        writer.println("class test {");
        writer.println("int count;");
        writer.println("}");
      } finally {
        if (writer != null) {
          writer.flush();
          writer.close();
        }
      }
      currentTime = dartFile.lastModified();
      if (currentTime == initialTime) {
        Thread.sleep(10);
      }
    }
  }
}
