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
package com.google.dart.tools.internal.corext.refactoring;


/**
 * This class defines a set of reusable static checks methods.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class Checks {
  public static boolean startsWithLowerCase(String s) {
    if (s == null) {
      return false;
    } else if (s.isEmpty()) {
      return false;
    } else {
      return s.charAt(0) == Character.toLowerCase(s.charAt(0));
    }
  }

  /**
   * no instances
   */
  private Checks() {
  }
}
