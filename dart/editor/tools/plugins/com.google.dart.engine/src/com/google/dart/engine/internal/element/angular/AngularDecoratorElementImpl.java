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

package com.google.dart.engine.internal.element.angular;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.angular.AngularDecoratorElement;
import com.google.dart.engine.element.angular.AngularPropertyElement;

/**
 * Implementation of {@code AngularDirectiveElement}.
 * 
 * @coverage dart.engine.element
 */
public class AngularDecoratorElementImpl extends AngularHasSelectorElementImpl implements
    AngularDecoratorElement {
  /**
   * The offset of the annotation that defines this directive.
   */
  private int offset;

  /**
   * The array containing all of the properties declared by this directive.
   */
  private AngularPropertyElement[] properties = AngularPropertyElement.EMPTY_ARRAY;

  /**
   * Initialize a newly created Angular directive to have the given name.
   * 
   * @param offset the offset of the annotation that defines this directive
   */
  public AngularDecoratorElementImpl(int offset) {
    super(null, -1);
    this.offset = offset;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitAngularDirectiveElement(this);
  }

  @Override
  public String getDisplayName() {
    return getSelector().getDisplayName();
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.ANGULAR_DIRECTIVE;
  }

  @Override
  public AngularPropertyElement[] getProperties() {
    return properties;
  }

  @Override
  public boolean isClass(String name) {
    Element enclosing = getEnclosingElement();
    return enclosing instanceof ClassElement && enclosing.getName().equals(name);
  }

  /**
   * Set an array containing all of the properties declared by this directive.
   * 
   * @param properties the properties to set
   */
  public void setProperties(AngularPropertyElement[] properties) {
    for (AngularPropertyElement property : properties) {
      encloseElement((AngularPropertyElementImpl) property);
    }
    this.properties = properties;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    safelyVisitChildren(properties, visitor);
    super.visitChildren(visitor);
  }

  @Override
  protected String getIdentifier() {
    return "Decorator@" + offset;
  }
}
