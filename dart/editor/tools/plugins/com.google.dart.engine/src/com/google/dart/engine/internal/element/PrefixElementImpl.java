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

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * Instances of the class {@code PrefixElementImpl} implement a {@code PrefixElement}.
 * 
 * @coverage dart.engine.element
 */
public class PrefixElementImpl extends ElementImpl implements PrefixElement {
  /**
   * An array containing all of the libraries that are imported using this prefix.
   */
  private LibraryElement[] importedLibraries = LibraryElementImpl.EMPTY_ARRAY;

  /**
   * An empty array of prefix elements.
   */
  public static final PrefixElement[] EMPTY_ARRAY = new PrefixElement[0];

  /**
   * Initialize a newly created prefix element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
  public PrefixElementImpl(Identifier name) {
    super(name);
  }

  /**
   * Initialize a newly created method element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public PrefixElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitPrefixElement(this);
  }

  @Override
  public LibraryElement getEnclosingElement() {
    return (LibraryElement) super.getEnclosingElement();
  }

  @Override
  public LibraryElement[] getImportedLibraries() {
    return importedLibraries;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.PREFIX;
  }

  /**
   * Set the libraries that are imported using this prefix to the given libraries.
   * 
   * @param importedLibraries the libraries that are imported using this prefix
   */
  public void setImportedLibraries(LibraryElement[] importedLibraries) {
    for (LibraryElement library : importedLibraries) {
      ((LibraryElementImpl) library).setEnclosingElement(this);
    }
    this.importedLibraries = importedLibraries;
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append("as ");
    super.appendTo(builder);
  }

  @Override
  protected String getIdentifier() {
    // Force additional character because prefix location is just library + this identifier,
    // and when we compare locations we ignore first character of first two components - we expect
    // that they are URIs with URI kind character.
    return "_" + super.getIdentifier();
  }
}
