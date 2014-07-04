/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server;

/**
 * The interface {@code TypeHierarchyItem} defines the behavior of objects representing an item in a
 * type hierarchy.
 * 
 * @coverage dart.server
 */
public interface HoverInformation {

  /**
   * The name of the library in which the referenced element is declared. This data is omitted if
   * there is no referenced element.
   */
  public String getContainingLibraryName();

  /**
   * The path to the defining compilation unit of the library in which the referenced element is
   * declared. This data is omitted if there is no referenced element.
   */
  public String getContainingLibraryPath();

  /**
   * The dartdoc associated with the referenced element. Other than the removal of the comment
   * delimiters, including leading asterisks in the case of a block comment, the dartdoc is
   * unprocessed markdown. This data is omitted if there is no referenced element.
   */
  public String getDartdoc();

  /**
   * A textual description of the element being referenced. This data is omitted if there is no
   * referenced element.
   */
  public String getElementDescription();

  /**
   * A textual description of the parameter corresponding to the expression being hovered over. This
   * data is omitted if the location is not in an argument to a function.
   */
  public String getParameter();

  /**
   * The name of the propagated type of the expression. This data is omitted if the location does
   * not correspond to an expression or if there is no propagated type information.
   */
  public String getPropagatedType();

  /**
   * The name of the static type of the expression. This data is omitted if the location does not
   * correspond to an expression.
   */
  public String getStaticType();
}
