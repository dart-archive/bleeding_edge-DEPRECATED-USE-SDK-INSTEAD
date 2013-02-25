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
public final class StringUtils {
  private static final String INDENTS[] = new String[] {"", "  ", "    ", "      "};

  public static String indent(int level) {
    if (level < 0) {
      return "";
    } else if (level < INDENTS.length) {
      return INDENTS[level];
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < level; i++) {
      builder.append("  ");
    }
    return builder.toString();
  }

  public static String join(String[] path) {
    return join(path, 0, -1, "//");
  }

  public static String join(String[] path, int start, int end, String delimiter) {
    int length = path.length;
    if (end < 0) {
      end = length;
    } else if (end > length) {
      end = length;
    }
    StringBuilder builder = new StringBuilder();
    for (int i = start; i < end; i++) {
      if (i > start) {
        builder.append(delimiter);
      }
      builder.append(path[i]);
    }
    return builder.toString();
  }

  public static String join(String[] path, String delimiter) {
    return join(path, 0, -1, delimiter);
  }

  private StringUtils() {
    // utility class
  }
}
