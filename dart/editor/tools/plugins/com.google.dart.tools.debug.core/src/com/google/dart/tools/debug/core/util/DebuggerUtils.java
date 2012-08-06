/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.debug.core.util;

/**
 * This class contains static utility methods for use by the debugger.
 */
public class DebuggerUtils {

  public static final String LIBRARY_NAME = "library";

  public static final String TOP_LEVEL_NAME = "top-level";

  /**
   * The names of private fields are mangled by the VM.
   * <p>
   * _foo@652376 ==> _foo
   * 
   * @param name
   * @return
   */
  public static String demanglePrivateName(String name) {
    int atIndex = name.indexOf('@');

    while (atIndex != -1) {
      // check for _foo@76876.bar (or _Process@14117cc4._reportError@14117cc4)
      int endIndex = name.indexOf('.', atIndex);

      if (endIndex == -1) {
        name = name.substring(0, atIndex);
      } else {
        name = name.substring(0, atIndex) + name.substring(endIndex);
      }

      atIndex = name.indexOf('@');
    }

    // Also remove the trailing '.' for default constructors.
    if (name.endsWith(".")) {
      name = name.substring(0, name.length() - 1);
    }

    return name;
  }

  private DebuggerUtils() {

  }

}
