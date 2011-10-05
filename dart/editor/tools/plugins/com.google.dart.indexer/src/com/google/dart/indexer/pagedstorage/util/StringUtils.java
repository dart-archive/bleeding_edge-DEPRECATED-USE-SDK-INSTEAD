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
package com.google.dart.indexer.pagedstorage.util;

/**
 * A few String utility functions.
 */
public class StringUtils {
  private static final String INDENTS[] = new String[] {"", "  ", "    ", "      "};

  public static String indent(int level) {
    return INDENTS[level];
  }

  public static String join(String[] path) {
    return join(path, "//");
  }

  public static String join(String[] path, String delimiter) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < path.length; i++) {
      String component = path[i];
      if (i > 0) {
        builder.append(delimiter);
      }
      builder.append(component);
    }
    return builder.toString();
  }

  private StringUtils() {
    // utility class
  }
}
