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
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.angular.AngularModuleElement;
import com.google.dart.engine.internal.element.ClassElementImpl;

/**
 * Implementation of {@code AngularModuleElement}.
 * 
 * @coverage dart.engine.element
 */
public class AngularModuleElementImpl extends AngularElementImpl implements AngularModuleElement {
  /**
   * The array containing all of the child modules.
   */
  private AngularModuleElement[] childModules = AngularModuleElement.EMPTY_ARRAY;

  /**
   * The array containing all of the types used as injection keys.
   */
  private ClassElement[] keyTypes = ClassElementImpl.EMPTY_ARRAY;

  /**
   * Initialize a newly created Angular module.
   */
  public AngularModuleElementImpl() {
    super(null, -1);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitAngularModuleElement(this);
  }

  @Override
  public AngularModuleElement[] getChildModules() {
    return childModules;
  }

  @Override
  public ClassElement[] getKeyTypes() {
    return keyTypes;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.ANGULAR_MODULE;
  }

  /**
   * Sets the child modules installed into this module using <code>install</code>.
   * 
   * @param childModules the child modules to set
   */
  public void setChildModules(AngularModuleElement[] childModules) {
    this.childModules = childModules;
  }

  /**
   * Sets the keys injected into this module using <code>type()</code> and <code>value()</code>
   * invocations.
   * 
   * @param keyTypes the key types to set
   */
  public void setKeyTypes(ClassElement[] keyTypes) {
    this.keyTypes = keyTypes;
  }
}
