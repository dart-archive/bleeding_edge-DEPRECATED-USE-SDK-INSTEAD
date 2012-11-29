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
 * The interface {@code ExportSpecification} defines the behavior of objects representing
 * information about a single export directive within a library.
 */
public interface ExportSpecification {
  /**
   * An empty array of export specifications.
   */
  public static final ExportSpecification[] EMPTY_ARRAY = new ExportSpecification[0];

  /**
   * Return an array containing the combinators that were specified as part of the export directive
   * in the order in which they were specified.
   * 
   * @return the combinators specified in the export directive
   */
  public ImportCombinator[] getCombinators();

  /**
   * Return the library that is exported from this library by this export directive.
   * 
   * @return the library that is exported from this library
   */
  public LibraryElement getExportedLibrary();
}
