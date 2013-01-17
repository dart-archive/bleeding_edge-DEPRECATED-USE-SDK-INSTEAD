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

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.element.PrefixElement;

/**
 * Instances of the class {@code ImportElementImpl} implement an {@link ImportElement}.
 */
public class ImportElementImpl extends ElementImpl implements ImportElement {
  /**
   * The library that is imported into this library by this import directive.
   */
  private LibraryElement importedLibrary;

  /**
   * The combinators that were specified as part of the import directive in the order in which they
   * were specified.
   */
  private NamespaceCombinator[] combinators = NamespaceCombinator.EMPTY_ARRAY;

  /**
   * The prefix that was specified as part of the import directive, or {@code null} if there was no
   * prefix specified.
   */
  private PrefixElement prefix;

  /**
   * Initialize a newly created import element.
   */
  public ImportElementImpl() {
    super(null);
  }

  @Override
  public NamespaceCombinator[] getCombinators() {
    return combinators;
  }

  @Override
  public LibraryElement getImportedLibrary() {
    return importedLibrary;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.IMPORT;
  }

  @Override
  public PrefixElement getPrefix() {
    return prefix;
  }

  /**
   * Set the combinators that were specified as part of the import directive to the given array of
   * combinators.
   * 
   * @param combinators the combinators that were specified as part of the import directive
   */
  public void setCombinators(NamespaceCombinator[] combinators) {
    this.combinators = combinators;
  }

  /**
   * Set the library that is imported into this library by this import directive to the given
   * library.
   * 
   * @param importedLibrary the library that is imported into this library
   */
  public void setImportedLibrary(LibraryElement importedLibrary) {
    this.importedLibrary = importedLibrary;
  }

  /**
   * Set the prefix that was specified as part of the import directive to the given prefix.
   * 
   * @param prefix the prefix that was specified as part of the import directive
   */
  public void setPrefix(PrefixElement prefix) {
    this.prefix = prefix;
  }
}
