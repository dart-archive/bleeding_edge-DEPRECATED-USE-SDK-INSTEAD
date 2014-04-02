/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.engine.internal.element.polymer;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.polymer.PolymerAttributeElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;

/**
 * Implementation of {@code PolymerTagHtmlElement}.
 * 
 * @coverage dart.engine.element
 */
public class PolymerTagHtmlElementImpl extends PolymerElementImpl implements PolymerTagHtmlElement {
  /**
   * The {@link PolymerTagDartElement} part of this Polymer custom tag. Maybe {@code null} if it has
   * not been resolved yet or there are no corresponding Dart part defined.
   */
  private PolymerTagDartElement dartElement;

  /**
   * The array containing all of the attributes declared by this tag.
   */
  private PolymerAttributeElement[] attributes = PolymerAttributeElement.EMPTY_ARRAY;

  /**
   * Initialize a newly created HTML part of a Polymer tag to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public PolymerTagHtmlElementImpl(String name, int nameOffset) {
    super(name, nameOffset);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitPolymerTagHtmlElement(this);
  }

  @Override
  public PolymerAttributeElement[] getAttributes() {
    return attributes;
  }

  @Override
  public PolymerTagDartElement getDartElement() {
    return dartElement;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.POLYMER_TAG_HTML;
  }

  /**
   * Set an array containing all of the attributes declared by this tag.
   * 
   * @param attributes the properties to set
   */
  public void setAttributes(PolymerAttributeElement[] attributes) {
    for (PolymerAttributeElement property : attributes) {
      encloseElement((PolymerAttributeElementImpl) property);
    }
    this.attributes = attributes;
  }

  /**
   * Sets the {@link PolymerTagDartElement} part of this Polymer custom tag.
   * 
   * @param dartElement the {@link PolymerTagDartElement} to set
   */
  public void setDartElement(PolymerTagDartElement dartElement) {
    this.dartElement = dartElement;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    safelyVisitChildren(attributes, visitor);
    super.visitChildren(visitor);
  }
}
