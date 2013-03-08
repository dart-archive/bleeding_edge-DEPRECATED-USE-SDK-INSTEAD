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
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LabelElement;

/**
 * Instances of the class {@code LabelElementImpl} implement a {@code LabelElement}.
 * 
 * @coverage dart.engine.element
 */
public class LabelElementImpl extends ElementImpl implements LabelElement {
  /**
   * A flag indicating whether this label is associated with a {@code switch} statement.
   */
  private boolean onSwitchStatement;

  /**
   * A flag indicating whether this label is associated with a {@code switch} member ({@code case}
   * or {@code default}).
   */
  private boolean onSwitchMember;

  /**
   * An empty array of label elements.
   */
  public static final LabelElement[] EMPTY_ARRAY = new LabelElement[0];

  /**
   * Initialize a newly created label element to have the given name.
   * 
   * @param name the name of this element
   * @param onSwitchStatement {@code true} if this label is associated with a {@code switch}
   *          statement
   * @param onSwitchMember {@code true} if this label is associated with a {@code switch} member
   */
  public LabelElementImpl(Identifier name, boolean onSwitchStatement, boolean onSwitchMember) {
    super(name);
    this.onSwitchStatement = onSwitchStatement;
    this.onSwitchMember = onSwitchMember;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitLabelElement(this);
  }

  @Override
  public ExecutableElement getEnclosingElement() {
    return (ExecutableElement) super.getEnclosingElement();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.LABEL;
  }

  /**
   * Return {@code true} if this label is associated with a {@code switch} member ({@code case} or
   * {@code default}).
   * 
   * @return {@code true} if this label is associated with a {@code switch} member
   */
  public boolean isOnSwitchMember() {
    return onSwitchMember;
  }

  /**
   * Return {@code true} if this label is associated with a {@code switch} statement.
   * 
   * @return {@code true} if this label is associated with a {@code switch} statement
   */
  public boolean isOnSwitchStatement() {
    return onSwitchStatement;
  }
}
