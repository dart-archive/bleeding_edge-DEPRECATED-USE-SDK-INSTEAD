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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;

import java.util.EnumSet;

/**
 * The abstract class {@code ElementImpl} implements the behavior common to objects that implement
 * an {@link Element}.
 */
public abstract class ElementImpl implements Element {
  /**
   * The enclosing element of this element, or {@code null} if this element is at the root of the
   * element structure.
   */
  private ElementImpl enclosingElement;

  /**
   * The name of this element.
   */
  private String name;

  /**
   * A bit-encoded form of the modifiers associated with this element.
   */
  private EnumSet<Modifier> modifiers;

  /**
   * Initialize a newly created element to have the given name.
   * 
   * @param name the name of this element
   */
  public ElementImpl(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object object) {
    if (this.getClass() != object.getClass()) {
      return false;
    }
    ElementImpl other = (ElementImpl) object;
    return name.equals(other.getName()) && enclosingElement.equals(other.getEnclosingElement());
  }

//  @Override
  @Override
  @SuppressWarnings("unchecked")
  public <E extends Element> E getAncestor(Class<E> elementClass) {
    Element ancestor = enclosingElement;
    while (ancestor != null && !elementClass.isInstance(ancestor)) {
      ancestor = ancestor.getEnclosingElement();
    }
    return (E) ancestor;
  }

  @Override
  public Element getEnclosingElement() {
    return enclosingElement;
  }

  @Override
  public LibraryElement getLibrary() {
    return getAncestor(LibraryElement.class);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean isSynthetic() {
    return hasModifier(Modifier.SYNTHETIC);
  }

  /**
   * Set whether this element is synthetic to correspond to the given value.
   * 
   * @param isSynthetic {@code true} if the element is synthetic
   */
  public void setSynthetic(boolean isSynthetic) {
    setModifier(Modifier.SYNTHETIC, isSynthetic);
  }

  /**
   * Return {@code true} if this element has the given modifier associated with it.
   * 
   * @param modifier the modifier being tested for
   * @return {@code true} if this element has the given modifier associated with it
   */
  protected boolean hasModifier(Modifier modifier) {
    return modifiers.contains(modifier);
  }

  /**
   * Set the enclosing element of this element to the given element.
   * 
   * @param element the enclosing element of this element
   */
  protected void setEnclosingElement(ElementImpl element) {
    enclosingElement = element;
  }

  /**
   * Set whether the given modifier is associated with this element to correspond to the given
   * value.
   * 
   * @param modifier the modifier to be set
   * @param value {@code true} if the modifier is to be associated with this element
   */
  protected void setModifier(Modifier modifier, boolean value) {
    if (value) {
      modifiers.add(modifier);
    } else {
      modifiers.remove(modifier);
    }
  }
}
