/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.debug.core.internal;

import com.google.dart.tools.core.utilities.compiler.DartJsUtilities;

/**
 * Various methods used to abstract away compiler specific knowledge.
 */
public class CompilerUtilities {
  private static final String FIELD_ID = "$field";
  private static final String SYNTHETIC_LOCAL_VARIABLE = "temp$";
  private static final String SYNTHETIC_LOCAL_VARIABLE2 = "tmp$";

  public static String demangleFunctionName(String name) {
    return DartJsUtilities.demangleJsFunctionName(name);
  }

  public static String getDartFieldName(String jsFieldName) {
    if (jsFieldName.endsWith(FIELD_ID)) {
      return jsFieldName.substring(0, jsFieldName.length() - FIELD_ID.length());
    } else {
      return jsFieldName;
    }
  }

  public static boolean isDartFieldName(String name) {
    // TODO(devoncarew): return name.indexOf(FIELD_ID) != -1

    return true;
  }

  public static boolean isDartLocalVariable(String name) {
    // TODO(devoncarew): we're depending on an implicit contract from the compiler here
    return name.indexOf(SYNTHETIC_LOCAL_VARIABLE) == -1
        && name.indexOf(SYNTHETIC_LOCAL_VARIABLE2) == -1;
  }

  private CompilerUtilities() {

  }

}
