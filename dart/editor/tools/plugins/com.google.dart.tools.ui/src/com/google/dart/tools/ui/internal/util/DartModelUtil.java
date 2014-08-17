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
package com.google.dart.tools.ui.internal.util;

import org.eclipse.core.resources.IStorage;

/**
 * Utility methods for the Dart Model.
 */
public final class DartModelUtil {

  /**
   * Concatenates two names. Uses a dot for separation. Both strings can be empty or
   * <code>null</code>.
   */
  public static String concatenateName(String name1, String name2) {
    StringBuffer buf = new StringBuffer();
    if (name1 != null && name1.length() > 0) {
      buf.append(name1);
    }
    if (name2 != null && name2.length() > 0) {
      if (buf.length() > 0) {
        buf.append('.');
      }
      buf.append(name2);
    }
    return buf.toString();
  }

  public static boolean isOpenableStorage(Object storage) {
//    if (storage instanceof IJarEntryResource) {
//      return ((IJarEntryResource) storage).isFile();
//    } else {
    return storage instanceof IStorage;
//    }
  }
}
