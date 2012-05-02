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
package com.google.dart.tools.update.core.internal;

import com.google.dart.tools.update.core.Revision;
import com.google.dart.tools.update.core.UpdateCore;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.internal.Library;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Update utilities.
 */
@SuppressWarnings("restriction")
public class UpdateUtils {

  private static enum Arch {
    x32("x86"), x64("x86_64"), UNKNOWN(null);

    private final String qualifier;

    Arch(String archQualifier) {
      this.qualifier = archQualifier;
    }
  }

  private static enum OS {
    WIN("win32.win32"), OSX("macosx.cocoa"), LINUX("linux.gtk"), UNKNOWN(null);

    private final String qualifier;

    OS(String binaryQualifier) {
      this.qualifier = binaryQualifier;
    }
  }

  private static final Arch ARCH = getArch();
  private static final OS OS = getOS();

  /**
   * Copy the contents of one directory to another directory recursively.
   * 
   * @param fromDir the source
   * @param toDir the target
   * @param overwriteFilter a filter to determine if a given file should be overwritten in a copy
   * @param monitor a monitor for providing progress feedback
   * @throws IOException
   */
  public static void copyDirectory(File fromDir, File toDir, FileFilter overwriteFilter,
      IProgressMonitor monitor) throws IOException {
    for (File f : fromDir.listFiles()) {
      File toFile = new File(toDir, f.getName());
      if (f.isFile()) {
        if (!toFile.exists() || overwriteFilter.accept(toFile)) {
          copyFile(f, toFile, monitor);
        } 
//        else {
//          System.out.println("skipping + " + toFile.getPath());
//          UpdateCore.logWarning("skipping + " + toFile.getPath());
//        }

      } else {
        if (!toFile.isDirectory()) {
          toFile.delete();
        }
        if (!toFile.exists()) {
          toFile.mkdir();
        }

        copyDirectory(f, toFile, overwriteFilter, monitor);
      }
    }

  }

  /**
   * Copy a file from one place to another, providing progress along the way.
   */
  public static void copyFile(File fromFile, File toFile, IProgressMonitor monitor)
      throws IOException {

//    System.out.println("copying " + fromFile.getName());

    byte[] data = new byte[4096];

    InputStream in = new FileInputStream(fromFile);

    toFile.delete();

    OutputStream out = new FileOutputStream(toFile);

    int count = in.read(data);

    while (count != -1) {
      out.write(data, 0, count);

      monitor.worked(count);

      count = in.read(data);
    }

    in.close();
    out.close();

    toFile.setLastModified(fromFile.lastModified());

    monitor.done();
  }

  /**
   * Delete the given file. If the file is a directory, recurse and delete contents.
   * 
   * @param file the file to delete
   */
  public static void delete(File file) {
    if (file.isFile()) {
      file.delete();
    } else {
      deleteDirectory(file);
    }
  }

  /**
   * Recurse and delete the given directory contents.
   * 
   * @param dir the directory to delete
   */
  public static void deleteDirectory(File dir) {
    if (dir == null || !dir.exists()) {
      return;
    }
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else {
        file.delete();
      }
    }

    dir.delete();
  }

  /**
   * Download a URL to a local file, notifying the given monitor along the way.
   */
  public static void downloadFile(URL downloadUrl, File destFile, String taskName,
      IProgressMonitor monitor) throws IOException {

    URLConnection connection = downloadUrl.openConnection();
    FileOutputStream out = new FileOutputStream(destFile);

    int length = connection.getContentLength();

    monitor.beginTask(taskName, length);
    copyStream(connection.getInputStream(), out, monitor, length);
    monitor.done();
  }

  /**
   * Parse the latest revision from the revision listing at the given url.
   * 
   * @param url the url to check
   * @return the latest revision, or <code>null</code> if none were found
   * @throws IOException if an exception occured in retrieving the revision
   */
  public static Revision getLatestRevision(String url) throws IOException {
    List<Revision> revisions = parseRevisions(url);
    if (revisions.isEmpty()) {
      return null;
    }
    Collections.sort(revisions);
//    //TODO (pquitslund): for testing continuous, roll back one rev
//    return revisions.get(revisions.size() - 2);
    return revisions.get(revisions.size() - 1);
  }

  /**
   * Convert the given revision to a path where it should be staged in the editor's updates/
   * directory in the local filesystem.
   * 
   * @param revision the revision
   * @return the local file system path
   */
  public static IPath getPath(Revision revision) {
    IPath updateDirPath = UpdateCore.getUpdateDirPath();
    return updateDirPath.append(revision.toString()).addFileExtension("zip");
  }

  /**
   * Get the update directory.
   * 
   * @return the update directory
   */
  public static File getUpdateDir() {
    IPath updateDirPath = UpdateCore.getUpdateDirPath();

    File updateDir = updateDirPath.toFile();
    if (!updateDir.exists()) {
      updateDir.mkdirs();
    }
    return updateDir;
  }

  /**
   * Get the target update install directory.
   * 
   * @return the install directory
   */
  public static File getUpdateInstallDir() {

    try {
      URL url = FileLocator.find(UpdateCore.getInstance().getBundle(), Path.EMPTY, null);
      if (url != null) {
        File bundle = new File(FileLocator.resolve(url).toURI());
        //dart/plugins/XXXX.jar
        return bundle.getParentFile().getParentFile();
      }
    } catch (Exception e) {
      return null;
    }

    if (UpdateCore.DEBUGGING_IN_RUNTIME_WS) {
      //TODO (pquitslund): for local testing
      return new File(getUpdateTempDir().getParentFile(), "install");
    }
    return getUpdateDir().getParentFile();

  }

  /**
   * Get the temporary update data directory.
   * 
   * @return the temporary update data directory
   */
  public static File getUpdateTempDir() {
    File updateDir = getUpdateDir();
    File tmpDir = new File(updateDir, "tmp");
    if (!tmpDir.isDirectory()) {
      tmpDir.delete();
      tmpDir = new File(updateDir, "tmp");
    }
    if (!tmpDir.exists()) {
      tmpDir.mkdir();
    }
    return tmpDir;
  }

  /**
   * Build a platform-aware download URL for the given revision.
   * 
   * @param revision the revision
   * @return a download url
   * @throws MalformedURLException
   */
  public static URL getUrl(Revision revision) throws MalformedURLException {
    return new URL(UpdateCore.getUpdateUrl() + revision.toString() + "/" + getBinaryName());
  }

  /**
   * Check if the given zip file is valid.
   * 
   * @param zip the zip to check
   * @return <code>true</code> if it's valid, <code>false</code> otherwise.
   */
  public static boolean isZipValid(final File zip) {
    ZipFile zipfile = null;
    try {
      zipfile = new ZipFile(zip);
      return true;
    } catch (ZipException e) {
      return false;
    } catch (IOException e) {
      return false;
    } finally {
      try {
        if (zipfile != null) {
          zipfile.close();
          zipfile = null;
        }
      } catch (IOException e) {
      }
    }
  }

  /**
   * Read, as a string, the stream at the given url string.
   */
  public static String readUrlStream(String urlString) throws MalformedURLException, IOException {
    URL url = new URL(urlString);
    InputStream stream = url.openStream();
    return toString(stream);
  }

  /**
   * Unzip a zip file, notifying the given monitor along the way.
   */
  public static void unzip(File zipFile, File destination, String taskName, IProgressMonitor monitor)
      throws IOException {

    monitor.beginTask(taskName, (int) zipFile.length());

    final int BUFFER_SIZE = 4096;

    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
    ZipEntry entry;

    while ((entry = zis.getNextEntry()) != null) {
      int count;
      byte data[] = new byte[BUFFER_SIZE];

      File outFile = new File(destination, entry.getName());

      if (entry.isDirectory()) {
        if (!outFile.exists()) {
          outFile.mkdirs();
        }
      } else {
        if (!outFile.getParentFile().exists()) {
          outFile.getParentFile().mkdirs();
        }

        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

        while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
          out.write(data, 0, count);

          monitor.worked(count);
        }

        out.flush();
        out.close();
      }
    }

    zis.close();

    monitor.done();
  }

  private static void copyStream(InputStream in, FileOutputStream out, IProgressMonitor monitor,
      int length) throws IOException {
    byte[] data = new byte[4096];

    try {
      int count = in.read(data);
      while (count != -1) {
        if (monitor.isCanceled()) {
          throw new IOException("job cancelled");
        }
        out.write(data, 0, count);
        if (length != -1) {
          monitor.worked(count);
        }
        count = in.read(data);
      }
    } finally {
      in.close();
      out.close();
    }
  }

  private static Arch getArch() {
    try {
      return is64bitSWT() ? Arch.x64 : Arch.x32;
    } catch (Exception e) {
      UpdateCore.logError(e);
      return Arch.UNKNOWN;
    }
  }

  private static String getBinaryName() {
    return "dart-editor-" + OS.qualifier + "." + ARCH.qualifier + ".zip";
  }

  @SuppressWarnings("static-access")
  private static OS getOS() {
    if (Util.isMac()) {
      return OS.OSX;
    }
    if (Util.isLinux()) {
      return OS.LINUX;
    }
    if (Util.isWindows()) {
      return OS.WIN;
    }
    return OS.UNKNOWN;
  }

  private static boolean is64bitSWT() throws Exception {
    Class<Library> swtLibraryClass = Library.class;
    Field is64 = swtLibraryClass.getDeclaredField("IS_64");
    is64.setAccessible(true);
    return is64.getBoolean(swtLibraryClass);
  }

  private static boolean isNumeric(String str) {
    for (char c : str.toCharArray()) {
      if (!Character.isDigit(c)) {
        return false;
      }
    }
    return true;
  }

  private static List<Revision> parseRevisions(String urlString) throws IOException {

    String str = readUrlStream(urlString);

    Pattern linkPattern = Pattern.compile("<a[^>]+href=[\"']?([\"'>]+)[\"']?[^>]*>(.+?)</a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Matcher matcher = linkPattern.matcher(str);
    ArrayList<Revision> revisions = new ArrayList<Revision>();
    while (matcher.find()) {
      if (matcher.group(0).contains("dart-editor-archive")) {
        String revision = matcher.group(2);
        //drop trailing slash
        revision = revision.replace("/", "");
        //drop symbolic links (like "latest")
        if (isNumeric(revision)) {
          revisions.add(Revision.forValue(revision));
        }
      }
    }

    return revisions;
  }

  private static String toString(InputStream is) throws IOException {
    final char[] buffer = new char[0x10000];
    StringBuilder out = new StringBuilder();
    Reader in = new InputStreamReader(is, "UTF-8");
    int read;
    do {
      read = in.read(buffer, 0, buffer.length);
      if (read > 0) {
        out.append(buffer, 0, read);
      }
    } while (read >= 0);
    return out.toString();
  }
}
