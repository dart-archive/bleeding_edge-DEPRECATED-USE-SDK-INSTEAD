/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.utilities.io;

import com.google.dart.engine.utilities.os.OSUtilities;

import junit.framework.TestCase;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.File;
import java.io.IOException;

/**
 * The class {@code FileUtilities2} implements utility methods used to create and manipulate files.
 */
public final class FileUtilities2 {
  /**
   * A temporary directory used during testing and cleared via {@link #deleteTempDir()}.
   */
  private static final File TEMP_DIR = new File(
      System.getProperty("java.io.tmpdir"),
      "AnalysisEngineTestTmp");

  /**
   * Create a file with the given path after replacing any forward slashes ('/') in the path with
   * the current file separator.
   * 
   * @param path the path of the file to be created
   * @return the file representing the path
   */
  public static File createFile(String path) {
    return new File(convertPath(path)).getAbsoluteFile();
  }

  /**
   * Create a new symlink or throw an exception if creation of symlinks is not supported. Use
   * {@link #isSymLinkSupported()} to determine if this method will work on the current platform.
   * 
   * @param existingFile the existing file to which the new symlink should point (not {@code null},
   *          and must exist)
   * @param linkFile the symlink to be created (not {@code null}, but must not exist)
   */
  public static void createSymLink(File existingFile, File linkFile) throws IOException {
    assertTrue("Creation of symlinks is not supported", isSymLinkSupported());
    assertTrue("Target file does not exist", existingFile.exists());
    assertFalse("Link already exists", linkFile.exists());

    ProcessRunner runner = new ProcessRunner(new String[] {
        "ln", "-s", existingFile.getPath(), linkFile.getPath()});
    int exitCode = runner.runSync(10000);
    if (exitCode != 0) {
      fail("Symlink creation failed [" + exitCode + "] " + linkFile);
    }
  }

  /**
   * Create a temporary directory. Call {@link #deleteTempDir()} in the {@link TestCase} tearDown
   * method to delete all temporary files and directories.
   * 
   * @param name the name of the temporary directory (not {@code null}, not empty)
   * @return the directory created (not {@code null})
   */
  public static File createTempDir(String name) throws IOException {
    File dir = new File(TEMP_DIR, name);
    if (dir.mkdirs()) {
      return dir;
    }
    throw new IOException("Failed to create directory " + dir);
  }

  /**
   * Create a temporary file. Call {@link #deleteTempDir()} in the {@link TestCase} tearDown method
   * to delete all temporary files and directories.
   * 
   * @param name the name of the temporary file (not {@code null}, not empty)
   * @return the file (not {@code null})
   */
  public static File createTempFile(String name, String content) throws IOException {
    File file = new File(TEMP_DIR, name);
    if (file.createNewFile()) {
      return file;
    }
    throw new IOException("Failed to create file " + file);
  }

  /**
   * Delete the contents of the given directory, given that we know it is a directory.
   * 
   * @param dir the directory whose contents are to be deleted
   */
  public static void deleteDirectory(File dir) throws IOException {
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else {
        if (!file.delete()) {
          throw new IOException("Failed to delete " + file);
        }
      }
    }
    if (!dir.delete()) {
      throw new IOException("Failed to delete " + dir);
    }
  }

  /**
   * Delete symlink or throw an exception if creation of symlinks is not supported. Use
   * {@link #isSymLinkSupported()} to determine if this method will work on the current platform.
   * 
   * @param linkFile the symlink to be deleted (not {@code null}, and must exist)
   */
  public static void deleteSymLink(File linkFile) throws IOException {
    assertTrue("Creation of symlinks is not supported", isSymLinkSupported());
    assertTrue("Link does not exist", linkFile.exists());

    ProcessRunner runner = new ProcessRunner(new String[] {"rm", linkFile.getPath()});
    int exitCode = runner.runSync(10000);
    if (exitCode != 0) {
      fail("Symlink deletion failed [" + exitCode + "] " + linkFile);
    }
  }

  /**
   * Delete the temporary directory. This should called from the {@link TestCase} tearDown method of
   * any test case which calls {@link #createTempDir(String)} or {@link #createTempFile(String)}.
   */
  public static void deleteTempDir() throws IOException {
    if (TEMP_DIR.exists()) {
      deleteDirectory(TEMP_DIR);
    }
  }

  /**
   * Determine if creation of symlinks via {@link #createSymLink(File, File)} is supported.
   * 
   * @return {@code true} if symlinks can be created, else false
   */
  public static boolean isSymLinkSupported() {
    return !OSUtilities.isWindows();
  }

  /**
   * Convert all forward slashes in the given path to the current file separator.
   * 
   * @param path the path to be converted
   * @return the converted path
   */
  private static String convertPath(String path) {
    if (File.separator.equals("/")) {
      // We're on a unix-ish OS.
      return path;
    } else {
      // On windows, the path separator is '\'.
      return path.replaceAll("/", "\\\\");
    }
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private FileUtilities2() {
  }
}
