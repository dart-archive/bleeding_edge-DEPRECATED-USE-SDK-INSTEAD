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
package com.google.dart.engine.element;

/**
 * The enumeration {@code ElementKind} defines the various kinds of elements in the element model.
 * 
 * @coverage dart.engine.element
 */
public enum ElementKind {
  CLASS("class"),
  COMPILATION_UNIT("compilation unit"),
  CONSTRUCTOR("constructor"),
  DYNAMIC("<dynamic>"),
  EMBEDDED_HTML_SCRIPT("embedded html script"),
  ERROR("<error>"),
  EXPORT("export directive"),
  EXTERNAL_HTML_SCRIPT("external html script"),
  FIELD("field"),
  FUNCTION("function"),
  GETTER("getter"),
  HTML("html"),
  IMPORT("import directive"),
  LABEL("label"),
  LIBRARY("library"),
  LOCAL_VARIABLE("local variable"),
  METHOD("method"),
  NAME("<name>"),
  PARAMETER("parameter"),
  PREFIX("import prefix"),
  SETTER("setter"),
  TOP_LEVEL_VARIABLE("top level variable"),
  FUNCTION_TYPE_ALIAS("function type alias"),
  TYPE_VARIABLE("type variable"),
  UNIVERSE("<universe>");

  private final String displayName;

  private ElementKind(String displayName) {
    this.displayName = displayName;
  }

  /**
   * @return the name of this {@link ElementKind} to display in UI.
   */
  public String getDisplayName() {
    return displayName;
  }
}
