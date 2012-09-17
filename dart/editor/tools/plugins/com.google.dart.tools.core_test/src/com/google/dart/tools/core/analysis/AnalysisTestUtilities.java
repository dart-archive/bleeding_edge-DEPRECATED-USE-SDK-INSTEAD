/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.DartSdkManager;

import static junit.framework.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class AnalysisTestUtilities {

  private static final File sdkLibDir = DartSdkManager.getManager().getSdk().getLibraryDirectory();

  /**
   * Wait for any background analysis to be complete
   */
  public static void waitForAnalysis() {
    waitForIdle(60000);
  }

  /**
   * Wait up to the specified amount of time for the specified analysis server to be idle. If the
   * specified number is less than or equal to zero, then this method returns immediately.
   * 
   * @param server the analysis server to be tested (not <code>null</code>)
   * @param milliseconds the maximum number of milliseconds to wait
   */
  public static void waitForIdle(AnalysisServer server, long milliseconds) {
    if (!server.waitForIdle(milliseconds)) {
      fail("AnalysisServer not idle");
    }
  }

  /**
   * Wait up to the specified amount of time for the default analysis server to be idle. If the
   * specified number is less than or equal to zero, then this method returns immediately.
   * 
   * @param milliseconds the maximum number of milliseconds to wait
   * @return <code>true</code> if the server is idle
   */
  public static void waitForIdle(long milliseconds) {
    waitForIdle(PackageLibraryManagerProvider.getDefaultAnalysisServer(), milliseconds);
  }

  static void assertCachedLibraries(AnalysisServer server, File appDir, File... expected)
      throws Exception {
    Context context;
    if (appDir == null) {
      context = server.getSavedContext();

      // SDK libraries are not explicitly listed but should only be in saved context
      // so add them to the list of expected files

      ArrayList<File> expectedLibFiles = new ArrayList<File>();
      for (File libFile : expected) {
        expectedLibFiles.add(libFile);
      }
      for (File libFile : getCachedLibraryFiles(context)) {
        if (isSdkLibrary(libFile)) {
          expectedLibFiles.add(libFile);
        }
      }
      expected = expectedLibFiles.toArray(new File[expectedLibFiles.size()]);

    } else {
      context = getPackageContexts(server).get(appDir);
      if (context == null) {
        fail("Expected package context for " + appDir);
      }
    }
    assertFiles(expected, getCachedLibraryFiles(context));
  }

  static void assertPackageContexts(AnalysisServer server, File... expected) throws Exception {
    assertFiles(expected, getPackageContextDirectories(server));
  }

  static void assertQueuedTasks(AnalysisServer server, String... expectedTaskNames)
      throws Exception {
    Task[] actualTasks = getServerTaskQueue(server).getTasks();
    int index = 0;
    for (String name : expectedTaskNames) {
      if (index >= actualTasks.length) {
        fail("Expected task(" + index + ") to be " + name + ", but end of queue");
      }
      String taskClassName = actualTasks[index].getClass().getSimpleName();
      if (!name.equals(taskClassName)) {
        fail("Expected task(" + index + ") to be " + name + ", but found " + taskClassName);
      }
      index++;
    }
    if (index < actualTasks.length) {
      String message = "Expected " + expectedTaskNames.length + " tasks, but found "
          + actualTasks.length;
      while (index < actualTasks.length) {
        message += "\n  " + actualTasks[index].getClass().getSimpleName();
        index++;
      }
      fail(message);
    }
  }

  static void assertTrackedLibraryFiles(AnalysisServer server, File... expected) throws Exception {
    assertFiles(expected, getTrackedLibraryFiles(server));
  }

  /**
   * Answer <code>true</code> if the directory equals or contains the specified file.
   * 
   * @param directory the directory (not <code>null</code>, absolute file)
   * @param file the file (not <code>null</code>, absolute file)
   */
  static boolean equalsOrContains(File directory, File file) {
    String dirPath = directory.getPath();
    String filePath = file.getPath();
    if (!filePath.startsWith(dirPath)) {
      return false;
    }
    int index = dirPath.length();
    return index == filePath.length() || filePath.charAt(index) == File.separatorChar;
  }

  static Object getCachedLibrary(Context context, File libraryFile) throws Exception {
    Method method = Context.class.getDeclaredMethod("getCachedLibrary", File.class);
    method.setAccessible(true);
    return method.invoke(context, libraryFile);
  }

  static File[] getCachedLibraryFiles(Context context) throws Exception {
    Field field = Context.class.getDeclaredField("libraryCache");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    HashMap<File, Object> cache = (HashMap<File, Object>) field.get(context);
    Set<File> directories = cache.keySet();
    return directories.toArray(new File[directories.size()]);
  }

  static File[] getPackageContextDirectories(AnalysisServer server) throws Exception {
    HashMap<File, PackageContext> packageContexts = getPackageContexts(server);
    Set<File> directories = packageContexts.keySet();
    return directories.toArray(new File[directories.size()]);
  }

  @SuppressWarnings("unchecked")
  static HashMap<File, PackageContext> getPackageContexts(AnalysisServer server)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = SavedContext.class.getDeclaredField("packageContexts");
    field.setAccessible(true);
    return (HashMap<File, PackageContext>) field.get(server.getSavedContext());
  }

  static TaskQueue getServerTaskQueue(AnalysisServer server) throws Exception {
    Field field = AnalysisServer.class.getDeclaredField("queue");
    field.setAccessible(true);
    return (TaskQueue) field.get(server);
  }

  static File[] getTrackedLibraryFiles(AnalysisServer server) throws Exception {
    Method method = AnalysisServer.class.getDeclaredMethod("getTrackedLibraryFiles");
    method.setAccessible(true);
    Object result = method.invoke(server);
    return (File[]) result;
  }

  /**
   * Answer <code>true</code> if this library resides in the "lib" directory
   */
  static boolean isSdkLibrary(File libraryFile) {
    return equalsOrContains(sdkLibDir, libraryFile);
  }

  private static void assertFiles(File[] expected, File[] actual) {
    if (actual.length == expected.length) {
      HashSet<File> files = new HashSet<File>();
      files.addAll(Arrays.asList(actual));
      if (actual.length == files.size()) {
        for (File file : expected) {
          if (!files.remove(file)) {
            break;
          }
        }
      }
      if (files.size() == 0) {
        return;
      }
    }
    String msg = "Expected:";
    for (File file : sort(expected)) {
      msg += "\n   " + file;
    }
    msg += "\nbut found:";
    for (File file : sort(actual)) {
      msg += "\n   " + file;
    }
    fail(msg);
  }

  private static File[] sort(File[] original) {
    TreeSet<File> sorted = new TreeSet<File>(new Comparator<File>() {
      @Override
      public int compare(File f1, File f2) {
        return f1.getPath().compareTo(f2.getPath());
      }
    });
    for (File file : original) {
      sorted.add(file);
    }
    return sorted.toArray(new File[sorted.size()]);
  }
}
