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
package com.google.dart.tools.core.utilities.download;

import org.eclipse.core.runtime.IProgressMonitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility methods used while downloading/upgrading sdk etc
 */
public class DownloadUtilities {

  /**
   * Copies a file, given the file to be copied and copy to
   * 
   * @param fromFile the file to copy
   * @param toFile the file to be copied to
   * @param monitor
   * @throws IOException
   */
  public static void copyFile(File fromFile, File toFile, IProgressMonitor monitor)
      throws IOException {
    byte[] data = new byte[4096];

    InputStream in = new FileInputStream(fromFile);

    toFile.delete();

    OutputStream out = new FileOutputStream(toFile);

    monitor.beginTask("Copy " + fromFile.toString(), (int) fromFile.length());

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

  public static void deleteDirectory(File dir) {
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
   * Downloads a zip file from the specified uri
   * 
   * @param downloadURI the uri to download from
   * @param fileSuffix the temporary file name suffix, the prefix is always "zip"
   * @param message the message to display while downloading
   * @param monitor
   * @return the download file
   * @throws IOException
   */
  public static File downloadZipFile(URI downloadURI, String fileSuffix, String message,
      IProgressMonitor monitor) throws IOException {
    File tempFile = File.createTempFile(fileSuffix, ".zip");
    tempFile.deleteOnExit();

    URLConnection connection = downloadURI.toURL().openConnection();

    int length = connection.getContentLength();

    FileOutputStream out = new FileOutputStream(tempFile);

    monitor.beginTask(message, length);

    copyStream(connection.getInputStream(), out, monitor, length);

    monitor.done();

    if (connection.getLastModified() != 0) {
      tempFile.setLastModified(connection.getLastModified());
    }

    return tempFile;
  }

  /**
   * Uzips the given zip into the specified destination
   * 
   * @param zipFile the file to unzip
   * @param destination the destination directory
   * @param monitor
   * @throws IOException
   */
  public static void unzip(File zipFile, File destination, IProgressMonitor monitor)
      throws IOException {
    monitor.beginTask("Unzip " + zipFile.getName(), (int) zipFile.length());

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

    int count = in.read(data);

    while (count != -1) {
      out.write(data, 0, count);

      if (length != -1) {
        monitor.worked(count);
      }

      count = in.read(data);
    }

    in.close();
    out.close();
  }

}
