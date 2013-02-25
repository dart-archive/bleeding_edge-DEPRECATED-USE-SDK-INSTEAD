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
package com.google.dart.indexer.utils;

import java.io.File;

@SuppressWarnings("dep-ann")
public class FileUtil {
  /**
   * @deprecated use FileUtilities.delete(File)
   */
  public static boolean recursiveDelete(File path) {
    if (path.isDirectory()) {
      File[] files = path.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          if (files[i].isDirectory()) {
            recursiveDelete(files[i]);
          } else {
            files[i].delete();
          }
        }
      }
    }
    return path.delete();
  }
}
