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
  ANGULAR_FORMATTER("Angular formatter"),
  ANGULAR_COMPONENT("Angular component"),
  ANGULAR_CONTROLLER("Angular controller"),
  ANGULAR_DIRECTIVE("Angular directive"),
  ANGULAR_PROPERTY("Angular property"),
  ANGULAR_SCOPE_PROPERTY("Angular scope property"),
  ANGULAR_SELECTOR("Angular selector"),
  ANGULAR_VIEW("Angular view"),
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
  POLYMER_ATTRIBUTE("Polymer attribute"),
  POLYMER_TAG_DART("Polymer Dart tag"),
  POLYMER_TAG_HTML("Polymer HTML tag"),
  PREFIX("import prefix"),
  SETTER("setter"),
  TOP_LEVEL_VARIABLE("top level variable"),
  FUNCTION_TYPE_ALIAS("function type alias"),
  TYPE_PARAMETER("type parameter"),
  UNIVERSE("<universe>");

  /**
   * Return the kind of the given element, or {@link #ERROR} if the element is {@code null}. This is
   * a utility method that can reduce the need for null checks in other places.
   * 
   * @param element the element whose kind is to be returned
   * @return the kind of the given element
   */
  public static ElementKind of(Element element) {
    if (element == null) {
      return ERROR;
    }
    return element.getKind();
  }

  /**
   * The name displayed in the UI for this kind of element.
   */
  private final String displayName;

  /**
   * Initialize a newly created element kind to have the given display name.
   * 
   * @param displayName the name displayed in the UI for this kind of element
   */
  private ElementKind(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Return the name displayed in the UI for this kind of element.
   * 
   * @return the name of this {@link ElementKind} to display in UI.
   */
  public String getDisplayName() {
    return displayName;
  }
}
