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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.ExportSpecification;
import com.google.dart.engine.element.ImportCombinator;
import com.google.dart.engine.element.LibraryElement;

/**
 * Instances of the class {@code ExportSpecificationImpl} implement an {@link ExportSpecification}.
 */
public class ExportSpecificationImpl implements ExportSpecification {
  /**
   * The library that is exported from this library by this export directive.
   */
  private LibraryElement exportedLibrary;

  /**
   * The combinators that were specified as part of the export directive in the order in which they
   * were specified.
   */
  private ImportCombinator[] combinators = ImportCombinator.EMPTY_ARRAY;

  /**
   * Initialize a newly created export specification.
   */
  public ExportSpecificationImpl() {
    super();
  }

  @Override
  public ImportCombinator[] getCombinators() {
    return combinators;
  }

  @Override
  public LibraryElement getExportedLibrary() {
    return exportedLibrary;
  }

  /**
   * Set the combinators that were specified as part of the export directive to the given array of
   * combinators.
   * 
   * @param combinators the combinators that were specified as part of the export directive
   */
  public void setCombinators(ImportCombinator[] combinators) {
    this.combinators = combinators;
  }

  /**
   * Set the library that is exported from this library by this import directive to the given
   * library.
   * 
   * @param exportedLibrary the library that is exported from this library
   */
  public void setExportedLibrary(LibraryElement exportedLibrary) {
    this.exportedLibrary = exportedLibrary;
  }
}
