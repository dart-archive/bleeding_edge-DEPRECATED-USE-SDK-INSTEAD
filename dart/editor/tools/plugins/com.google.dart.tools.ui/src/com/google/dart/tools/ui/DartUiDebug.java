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
package com.google.dart.tools.ui;


/**
 * Debug/Tracing options for the {@link DartUI} plugin.
 */
public class DartUiDebug {
  public static final boolean USE_ONLY_SEMANTIC_HIGHLIGHTER = true; //isOptionTrue("experimental/useOnlySemanticHighlighter");

//  /**
//   * @return <code>true</code> if option has value "true".
//   */
//  private static boolean isOptionTrue(String optionSuffix) {
//    return isOptionValue(optionSuffix, "true");
//  }
//
//  /**
//   * @return <code>true</code> if option has "expected" value.
//   */
//  private static boolean isOptionValue(String optionSuffix, String expected) {
//    String option = DartUI.ID_PLUGIN + "/" + optionSuffix;
//    String value = Platform.getDebugOption(option);
//    if (value == null) {
//      value = DartCore.getUserDefinedProperty(option);
//    }
//    return StringUtils.equalsIgnoreCase(value, expected);
//  }

}
