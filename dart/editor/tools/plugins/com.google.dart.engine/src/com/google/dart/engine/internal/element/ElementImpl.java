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
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Annotation;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementLocation;
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
   * The offset of the name of this element in the file that contains the declaration of this
   * element.
   */
  private int nameOffset;

  /**
   * A bit-encoded form of the modifiers associated with this element.
   */
  private EnumSet<Modifier> modifiers;

  /**
   * An array containing all of the metadata associated with this element.
   */
  private Annotation[] metadata = AnnotationImpl.EMPTY_ARRAY;

  /**
   * Initialize a newly created element to have the given name.
   * 
   * @param name the name of this element
   */
  public ElementImpl(Identifier name) {
    this(name == null ? "" : name.getName(), name == null ? -1 : name.getOffset());
  }

  /**
   * Initialize a newly created element to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public ElementImpl(String name, int nameOffset) {
    this.name = name;
    this.nameOffset = nameOffset;
    this.modifiers = EnumSet.noneOf(Modifier.class);
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof Element && ((Element) object).getLocation().equals(getLocation());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Element> E getAncestor(Class<E> elementClass) {
    Element ancestor = enclosingElement;
    while (ancestor != null && !elementClass.isInstance(ancestor)) {
      ancestor = ancestor.getEnclosingElement();
    }
    return (E) ancestor;
  }

  /**
   * Return the child of this element that is uniquely identified by the given identifier, or
   * {@code null} if there is no such child.
   * 
   * @param identifier the identifier used to select a child
   * @return the child of this element with the given identifier
   */
  public ElementImpl getChild(String identifier) {
    return null;
  }

  @Override
  public AnalysisContext getContext() {
    if (enclosingElement == null) {
      return null;
    }
    return enclosingElement.getContext();
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
  public ElementLocation getLocation() {
    return new ElementLocationImpl(this);
  }

  @Override
  public Annotation[] getMetadata() {
    return metadata;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getNameOffset() {
    return nameOffset;
  }

  @Override
  public int hashCode() {
    return getLocation().hashCode();
  }

  @Override
  public boolean isSynthetic() {
    return hasModifier(Modifier.SYNTHETIC);
  }

  /**
   * Set the metadata associate with this element to the given array of annotations.
   * 
   * @param metadata the metadata to be associated with this element
   */
  public void setMetadata(Annotation[] metadata) {
    this.metadata = metadata;
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
   * Return an identifier that uniquely identifies this element among the children of this element's
   * parent.
   * 
   * @return an identifier that uniquely identifies this element relative to its parent
   */
  protected String getIdentifier() {
    return getName();
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
