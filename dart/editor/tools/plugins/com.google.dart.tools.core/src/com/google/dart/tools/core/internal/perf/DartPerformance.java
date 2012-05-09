/*
 * Copyright (c) 2012, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.internal.perf;

import java.io.File;
import java.util.ArrayList;

public class DartPerformance {

  public static String PERF_FLAG = "-perf";

  /**
   * This boolean is set to <code>true</code> if and only if
   */
  public static boolean MEASURE_PERFORMANCE = false;

  private static ArrayList<File> fileSet;

  // TODO (jwren) move this method to a different class?
  public static void openCommandLineFilesAndFolders() {
    if (fileSet == null || fileSet.isEmpty()) {
      return;
    }
    for (File file : fileSet) {
      // verify that this file is not null, and exists
      if (file == null || !file.exists()) {
        continue;
      }
      // TODO (jwren), remove this System.out, it is temporary and only for testing purposes
      //System.out.println("file = " + file.toURI() + " !!!");
    }
  }

  public static void setFileSet(ArrayList<File> fileSet) {
    DartPerformance.fileSet = fileSet;
  }

}
