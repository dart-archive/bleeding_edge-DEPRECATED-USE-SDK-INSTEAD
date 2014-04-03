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
 * The interface {@code ImportElement} defines the behavior of objects representing information
 * about a single import directive within a library.
 * 
 * @coverage dart.engine.element
 */
public interface ImportElement extends Element, UriReferencedElement {
  /**
   * An empty array of import elements.
   */
  public static final ImportElement[] EMPTY_ARRAY = new ImportElement[0];

  /**
   * Return an array containing the combinators that were specified as part of the import directive
   * in the order in which they were specified.
   * 
   * @return the combinators specified in the import directive
   */
  public NamespaceCombinator[] getCombinators();

  /**
   * Return the library that is imported into this library by this import directive.
   * 
   * @return the library that is imported into this library
   */
  public LibraryElement getImportedLibrary();

  /**
   * Return the prefix that was specified as part of the import directive, or {@code null} if there
   * was no prefix specified.
   * 
   * @return the prefix that was specified as part of the import directive
   */
  public PrefixElement getPrefix();

  /**
   * Return the offset of the prefix of this import in the file that contains this import directive,
   * or {@code -1} if this import is synthetic, does not have a prefix, or otherwise does not have
   * an offset.
   * 
   * @return the offset of the prefix of this import
   */
  public int getPrefixOffset();

  /**
   * Return {@code true} if this import is for a deferred library.
   * 
   * @return {@code true} if this import is for a deferred library
   */
  public boolean isDeferred();
}
