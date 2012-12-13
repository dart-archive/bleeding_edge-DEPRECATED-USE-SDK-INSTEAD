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

import com.google.dart.engine.source.Source;

/**
 * The interface {@code HtmlElement} defines the behavior of elements representing an HTML file.
 */
public interface HtmlElement extends Element {
  /**
   * Return an array containing all of the libraries contained in or referenced from script tags in
   * the HTML file. This includes libraries that are defined by the content of a script file as well
   * as libraries that are referenced in the {@core src} attribute of a script tag.
   * 
   * @return the libraries referenced from script tags in the HTML file
   */
  public LibraryElement[] getLibraries();

  /**
   * Return the source that corresponds to this HTML file.
   * 
   * @return the source that corresponds to this HTML file
   */
  public Source getSource();
}
