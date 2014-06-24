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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.visitor.NodeLocator;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.ElementLocation;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.collection.BooleanArray;
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.engine.utilities.translation.DartName;

/**
 * The abstract class {@code ElementImpl} implements the behavior common to objects that implement
 * an {@link Element}.
 * 
 * @coverage dart.engine.element
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
  private final String name;

  /**
   * The offset of the name of this element in the file that contains the declaration of this
   * element.
   */
  private int nameOffset;

  /**
   * A bit-encoded form of the modifiers associated with this element.
   */
  private int modifiers;

  /**
   * An array containing all of the metadata associated with this element.
   */
  private ElementAnnotation[] metadata = ElementAnnotationImpl.EMPTY_ARRAY;

  /**
   * A cached copy of the calculated hashCode for this element.
   */
  private int cachedHashCode;

  /**
   * Initialize a newly created element to have the given name.
   * 
   * @param name the name of this element
   */
  @DartName("forNode")
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
    this.name = StringUtilities.intern(name);
    this.nameOffset = nameOffset;
  }

  @Override
  public String computeDocumentationComment() throws AnalysisException {
    AnalysisContext context = getContext();
    if (context == null) {
      return null;
    }
    return context.computeDocumentationComment(this);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || hashCode() != object.hashCode()) {
      return false;
    }
    return object.getClass() == getClass()
        && ((Element) object).getLocation().equals(getLocation());
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
  public String getDisplayName() {
    return name;
  }

  @Override
  public Element getEnclosingElement() {
    return enclosingElement;
  }

  @Override
  public String getExtendedDisplayName(String shortName) {
    if (shortName == null) {
      shortName = getDisplayName();
    }
    Source source = getSource();
    if (source != null) {
      return shortName + " (" + source.getFullName() + ")";
    }
    return shortName;
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
  public ElementAnnotation[] getMetadata() {
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
  public AstNode getNode() throws AnalysisException {
    return getNodeMatching(AstNode.class);
  }

  @Override
  public Source getSource() {
    if (enclosingElement == null) {
      return null;
    }
    return enclosingElement.getSource();
  }

  @Override
  public CompilationUnit getUnit() throws AnalysisException {
    return getContext().resolveCompilationUnit(getSource(), getLibrary());
  }

  @Override
  public int hashCode() {
    // TODO: We might want to re-visit this optimization in the future.
    // We cache the hash code value as this is a very frequently called method.
    if (cachedHashCode == 0) {
      int hashIdentifier = getIdentifier().hashCode();
      Element enclosing = getEnclosingElement();
      if (enclosing != null) {
        cachedHashCode = hashIdentifier + enclosing.hashCode();
      } else {
        cachedHashCode = hashIdentifier;
      }
    }
    return cachedHashCode;
  }

  @Override
  public boolean isAccessibleIn(LibraryElement library) {
    if (Identifier.isPrivateName(name)) {
      return library.equals(getLibrary());
    }
    return true;
  }

  @Override
  public boolean isDeprecated() {
    for (ElementAnnotation annotation : metadata) {
      if (annotation.isDeprecated()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isOverride() {
    for (ElementAnnotation annotation : metadata) {
      if (annotation.isOverride()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isPrivate() {
    String name = getDisplayName();
    if (name == null) {
      return true;
    }
    return Identifier.isPrivateName(name);
  }

  @Override
  public boolean isPublic() {
    return !isPrivate();
  }

  @Override
  public boolean isSynthetic() {
    return hasModifier(Modifier.SYNTHETIC);
  }

  /**
   * Set the enclosing element of this element to the given element.
   * 
   * @param element the enclosing element of this element
   */
  public void setEnclosingElement(Element element) {
    enclosingElement = (ElementImpl) element;
  }

  /**
   * Set the metadata associate with this element to the given array of annotations.
   * 
   * @param metadata the metadata to be associated with this element
   */
  public void setMetadata(ElementAnnotation[] metadata) {
    this.metadata = metadata;
  }

  /**
   * Set the offset of the name of this element in the file that contains the declaration of this
   * element to the given value. This is normally done via the constructor, but this method is
   * provided to support unnamed constructors.
   * 
   * @param nameOffset the offset to the beginning of the name
   */
  public void setNameOffset(int nameOffset) {
    this.nameOffset = nameOffset;
  }

  /**
   * Set whether this element is synthetic to correspond to the given value.
   * 
   * @param isSynthetic {@code true} if the element is synthetic
   */
  public void setSynthetic(boolean isSynthetic) {
    setModifier(Modifier.SYNTHETIC, isSynthetic);
  }

  @Override
  public final String toString() {
    StringBuilder builder = new StringBuilder();
    appendTo(builder);
    return builder.toString();
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    // There are no children to visit
  }

  /**
   * Append a textual representation of this type to the given builder.
   * 
   * @param builder the builder to which the text is to be appended
   */
  protected void appendTo(StringBuilder builder) {
    if (name == null) {
      builder.append("<unnamed ");
      builder.append(getClass().getName());
      builder.append(">");
    } else {
      builder.append(name);
    }
  }

  /**
   * Set this {@link Element} as an enclosing for given.
   * 
   * @param element the element to enclose, must be {@link ElementImpl}
   */
  protected void encloseElement(ElementImpl element) {
    element.setEnclosingElement(this);
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
   * Return the resolved {@link AstNode} of the given type enclosing {@link #getNameOffset()}.
   */
  protected <T extends AstNode> T getNodeMatching(Class<T> clazz) throws AnalysisException {
    CompilationUnit unit = getUnit();
    if (unit == null) {
      return null;
    }
    int offset = getNameOffset();
    AstNode node = new NodeLocator(offset).searchWithin(unit);
    if (node == null) {
      return null;
    }
    return node.getAncestor(clazz);
  }

  /**
   * Return {@code true} if this element has the given modifier associated with it.
   * 
   * @param modifier the modifier being tested for
   * @return {@code true} if this element has the given modifier associated with it
   */
  protected boolean hasModifier(Modifier modifier) {
    return BooleanArray.getEnum(modifiers, modifier);
  }

  /**
   * If the given child is not {@code null}, use the given visitor to visit it.
   * 
   * @param child the child to be visited
   * @param visitor the visitor to be used to visit the child
   */
  protected void safelyVisitChild(Element child, ElementVisitor<?> visitor) {
    if (child != null) {
      child.accept(visitor);
    }
  }

  /**
   * Use the given visitor to visit all of the children in the given array.
   * 
   * @param children the children to be visited
   * @param visitor the visitor being used to visit the children
   */
  protected void safelyVisitChildren(Element[] children, ElementVisitor<?> visitor) {
    if (children != null) {
      for (Element child : children) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Set whether the given modifier is associated with this element to correspond to the given
   * value.
   * 
   * @param modifier the modifier to be set
   * @param value {@code true} if the modifier is to be associated with this element
   */
  protected void setModifier(Modifier modifier, boolean value) {
    modifiers = BooleanArray.setEnum(modifiers, modifier, value);
  }
}
