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
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.NamespaceCombinator;
import com.google.dart.engine.element.PrefixElement;

/**
 * Instances of the class {@code ImportElementImpl} implement an {@link ImportElement}.
 * 
 * @coverage dart.engine.element
 */
public class ImportElementImpl extends UriReferencedElementImpl implements ImportElement {
  /**
   * The offset of the prefix of this import in the file that contains the this import directive, or
   * {@code -1} if this import is synthetic.
   */
  private int prefixOffset;

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
   * 
   * @param offset the directive offset, may be {@code -1} if synthetic.
   */
  public ImportElementImpl(int offset) {
    super(null, offset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitImportElement(this);
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

  @Override
  public int getPrefixOffset() {
    return prefixOffset;
  }

  @Override
  public boolean isDeferred() {
    return hasModifier(Modifier.DEFERRED);
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
   * Set whether this import is for a deferred library to correspond to the given value.
   * 
   * @param isDeferred {@code true} if this import is for a deferred library
   */
  public void setDeferred(boolean isDeferred) {
    setModifier(Modifier.DEFERRED, isDeferred);
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

  /**
   * Set the offset of the prefix of this import in the file that contains the this import
   * directive.
   */
  public void setPrefixOffset(int prefixOffset) {
    this.prefixOffset = prefixOffset;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(prefix, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    builder.append("import ");
    ((LibraryElementImpl) importedLibrary).appendTo(builder);
  }

  @Override
  protected String getIdentifier() {
    return ((LibraryElementImpl) importedLibrary).getIdentifier() + "@" + getNameOffset();
  }
}
