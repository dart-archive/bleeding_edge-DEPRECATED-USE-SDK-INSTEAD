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
package com.google.dart.tools.core.internal.perf;

import java.io.File;
import java.util.ArrayList;

/**
 * This class manages all command line arguments that the Dart Editor can read in. Currently the set
 * of command line arguments all are performance-related, hence this class is in this package,
 * <code>com.google.dart.tools.core.internal.perf</code>.
 */
public class DartEditorCommandLineManager {

  public static String PERF_FLAG = "-perf";

  private static long startTime;

  /**
   * This boolean is set to <code>true</code> if and only if the {@value #PERF_FLAG} is passed as a
   * command line argument to the Dart Editor.
   */
  public static boolean MEASURE_PERFORMANCE = false;

  private static ArrayList<File> fileSet = null;

  public static ArrayList<File> getFileSet() {
    return fileSet;
  }

  public static long getStartTime() {
    return startTime;
  }

  public static void setFileSet(ArrayList<File> fileSet) {
    DartEditorCommandLineManager.fileSet = fileSet;
  }

  public static void setStartTime(long startTime) {
    DartEditorCommandLineManager.startTime = startTime;
  }

  private DartEditorCommandLineManager() {
  }

}
