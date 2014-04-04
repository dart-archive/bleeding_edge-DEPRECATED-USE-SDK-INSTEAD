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

import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;

/**
 * The interface {@code HtmlElement} defines the behavior of elements representing an HTML file.
 * 
 * @coverage dart.engine.element
 */
public interface HtmlElement extends Element {
  /**
   * Return the {@link CompilationUnitElement} associated with this Angular HTML file, maybe
   * {@code null} if not an Angular file.
   */
  public CompilationUnitElement getAngularCompilationUnit();

  /**
   * Return an array containing all of the {@link PolymerTagHtmlElement}s defined in the HTML file.
   * 
   * @return the {@link PolymerTagHtmlElement}s elements in the HTML file (not {@code null},
   *         contains no {@code null}s)
   */
  public PolymerTagHtmlElement[] getPolymerTags();

  /**
   * Return an array containing all of the script elements contained in the HTML file. This includes
   * scripts with libraries that are defined by the content of a script tag as well as libraries
   * that are referenced in the {@core source} attribute of a script tag.
   * 
   * @return the script elements in the HTML file (not {@code null}, contains no {@code null}s)
   */
  public HtmlScriptElement[] getScripts();
}
