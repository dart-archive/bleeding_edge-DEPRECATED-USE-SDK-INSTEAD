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
package com.google.dart.tools.update.core.internal.jobs;

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges INI files (handles custom "-vm" arg).
 */
public class INIRewriter {

  private static final String VM_FLAG = "-vm";

  private static final String AGENT_FLAG = "-XX:-GoogleAgent";

  private static final String VM_ARGS_FLAG = "-vmargs";
  private static String[] EMPTY = new String[0];

  public static void insert(List<String> list, String value) {
    list.add(value);
  }

  public static void insertAfter(List<String> list, String key, String value) {
    int index = list.indexOf(key);
    list.add(index + 1, value);
  }

  public static void insertBefore(List<String> list, String key, String value) {
    int index = list.indexOf(key);
    list.add(index, value);
  }

  public static List<String> merge(List<String> orig, List<String> latest) {

    ArrayList<String> merged = Lists.newArrayList(latest);

    if (orig.contains(VM_FLAG) && !latest.contains(VM_FLAG)) {
      insertBefore(merged, VM_ARGS_FLAG, VM_FLAG);
      insertAfter(merged, VM_FLAG, orig.get(orig.indexOf(VM_FLAG) + 1));
    }

    if (orig.contains(AGENT_FLAG) && !latest.contains(AGENT_FLAG)) {
      insert(merged, AGENT_FLAG);
    }

    return merged;
  }

  public static String[] merge(String[] orig, String[] latest) {
    return merge(Lists.newArrayList(orig), Lists.newArrayList(latest)).toArray(EMPTY);
  }

  public static void mergeAndWrite(File currentIni, File latestIni) throws IOException {

    List<String> latest = INIRewriter.readFile(latestIni);
    List<String> current = INIRewriter.readFile(currentIni);

    List<String> merged = INIRewriter.merge(current, latest);

    if (!current.equals(merged)) {
      writeTo(merged, currentIni);
    }

  }

  public static List<String> readFile(File iniFile) throws IOException {

    BufferedReader reader = new BufferedReader(new FileReader(iniFile));
    String line = null;

    ArrayList<String> lines = Lists.newArrayList();
    while ((line = reader.readLine()) != null) {
      lines.add(line.trim());
    }

    return lines;
  }

  private static void writeTo(List<String> lines, File file) throws IOException {

    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    try {
      for (String line : lines) {
        out.write(line);
        out.newLine();
      }
    } finally {
      out.close();
    }
  }

}
