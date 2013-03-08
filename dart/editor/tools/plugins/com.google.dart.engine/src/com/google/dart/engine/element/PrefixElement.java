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
 * The interface {@code PrefixElement} defines the behavior common to elements that represent a
 * prefix used to import one or more libraries into another library.
 * 
 * @coverage dart.engine.element
 */
public interface PrefixElement extends Element {
  /**
   * Return the library into which other libraries are imported using this prefix.
   * 
   * @return the library into which other libraries are imported using this prefix
   */
  @Override
  public LibraryElement getEnclosingElement();

  /**
   * Return an array containing all of the libraries that are imported using this prefix.
   * 
   * @return the libraries that are imported using this prefix
   */
  public LibraryElement[] getImportedLibraries();
}
