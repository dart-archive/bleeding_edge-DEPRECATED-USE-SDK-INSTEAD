/*
 * Copyright (c) 2015, the Dart project authors.
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
package com.google.dart.tools.update.core.internal.jobs;

import com.google.dart.tools.core.DartCore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Merges editor.properties files
 */
public class PropertiesRewriter {

  public static Properties merge(Properties orig, Properties latest) {

    Properties merged = new Properties();
    merged.putAll(latest);
    merged.putAll(orig);
    return merged;
  }

  public static void mergeAndWrite(File curentProperties, File latestProperties) throws IOException {

    Properties latest = readFile(latestProperties);
    Properties current = readFile(curentProperties);

    Properties merged = merge(current, latest);

    if (!current.equals(merged)) {
      writeTo(merged, curentProperties);
    }

  }

  public static Properties readFile(File propertiesFile) throws IOException {

    Properties properties = new Properties();
    if (propertiesFile.exists()) {
      try {
        properties.load(new FileReader(propertiesFile));
      } catch (FileNotFoundException e) {
        DartCore.logError(e);
      }
    }
    return properties;
  }

  private static void writeTo(Properties properties, File file) throws IOException {

    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    try {
      properties.store(out, null);

    } finally {
      out.close();
    }
  }

}
