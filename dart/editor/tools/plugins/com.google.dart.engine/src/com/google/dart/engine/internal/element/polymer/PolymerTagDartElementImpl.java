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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;

/**
 * Implementation of {@code PolymerTagDartElement}.
 * 
 * @coverage dart.engine.element
 */
public class PolymerTagDartElementImpl extends PolymerElementImpl implements PolymerTagDartElement {
  /**
   * The {@link ClassElement} that is associated with this Polymer custom tag.
   */
  private final ClassElement classElement;

  /**
   * The {@link PolymerTagHtmlElement} part of this Polymer custom tag. Maybe {@code null} if it has
   * not been resolved yet or there are no corresponding Dart part defined.
   */
  private PolymerTagHtmlElement htmlElement;

  /**
   * Initialize a newly created Dart part of a Polymer tag to have the given name.
   * 
   * @param name the name of this element
   * @param nameOffset the offset of the name of this element in the file that contains the
   *          declaration of this element
   */
  public PolymerTagDartElementImpl(String name, int nameOffset, ClassElement classElement) {
    super(name, nameOffset);
    this.classElement = classElement;
    // TODO(scheglov) why do we resolve Dart library when HTML is saved?
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitPolymerTagDartElement(this);
  }

  @Override
  public ClassElement getClassElement() {
    return classElement;
  }

  @Override
  public PolymerTagHtmlElement getHtmlElement() {
    return htmlElement;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.POLYMER_TAG_DART;
  }

  /**
   * Sets the {@link PolymerTagHtmlElement} part of this Polymer custom tag.
   * 
   * @param htmlElement the {@link PolymerTagHtmlElement} to set
   */
  public void setHtmlElement(PolymerTagHtmlElement htmlElement) {
    this.htmlElement = htmlElement;
  }
}
