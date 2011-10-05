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
package com.google.dart.indexer.index;

import com.google.dart.indexer.exceptions.IndexRequiresFullRebuild;
import com.google.dart.indexer.exceptions.IndexTemporarilyNonOperational;
import com.google.dart.indexer.index.configuration.IndexConfigurationInstance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Provides utility method to read and write a file that describes the index configuration that the
 * index uses.
 */
public class VersionFile {
  private static final String ENCODING = "utf-8";

  /**
   * Checks that the index configuration file exists in the given folder and represents exactly the
   * given index configuration.
   */
  public static void check(File folder, IndexConfigurationInstance configuration)
      throws IndexRequiresFullRebuild {
    File versionFile = getFile(folder);
    if (!versionFile.exists()) {
      throw new IndexRequiresFullRebuild(
          "Error reading index version: index version file does not exist");
    }
    try {
      FileInputStream in = new FileInputStream(versionFile);
      try {
        String correct = configuration.describe();
        byte[] correctBytes = correct.getBytes(ENCODING);
        byte[] actualBytes = new byte[correctBytes.length + 1];
        int actualLength = in.read(actualBytes);
        if (actualLength == -1) {
          throw new IndexRequiresFullRebuild(
              "Error reading index version: index version file is empty");
        }
        String actual = new String(actualBytes, 0, actualLength, ENCODING);
        if (!correct.equals(actual)) {
          throw new IndexRequiresFullRebuild("Index configuration has changed", false);
        }
      } finally {
        in.close();
      }
    } catch (IOException exception) {
      throw new IndexRequiresFullRebuild("Error reading index version", exception);
    }
  }

  /**
   * Creates or overwrites an index configuration file in the given folder, so that it will
   * correspond to the given index configuration.
   */
  public static void write(File folder, IndexConfigurationInstance configuration)
      throws IndexTemporarilyNonOperational {
    folder.mkdirs();
    File versionFile = getFile(folder);
    try {
      FileOutputStream out = new FileOutputStream(versionFile);
      try {
        String correct = configuration.describe();
        byte[] correctBytes = correct.getBytes(ENCODING);
        out.write(correctBytes);
      } finally {
        out.close();
      }
    } catch (IOException exception) {
      throw new IndexTemporarilyNonOperational("Error writing index version", exception);
    }
  }

  private static File getFile(File folder) {
    return new File(folder, "version-info");
  }
}
