/*
 * Copyright (c) 2013, the Dart project authors.
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
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.scanner.Keyword;

/**
 * Instances of the class {@code DynamicElementImpl} represent the synthetic element representing
 * the declaration of the type {@code dynamic}.
 * 
 * @coverage dart.engine.element
 */
public class DynamicElementImpl extends ElementImpl {
  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static DynamicElementImpl getInstance() {
    return (DynamicElementImpl) DynamicTypeImpl.getInstance().getElement();
  }

  /**
   * The type defined by this element.
   */
  private DynamicTypeImpl type;

  /**
   * Initialize a newly created instance of this class. Instances of this class should <b>not</b> be
   * created except as part of creating the type associated with this element. The single instance
   * of this class should be accessed through the method {@link #getInstance()}.
   */
  public DynamicElementImpl() {
    super(Keyword.DYNAMIC.getSyntax(), -1);
    setModifier(Modifier.SYNTHETIC, true);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    // Visitors currently can't visit this kind of node. To 'fix' this, we would need to add an
    // interface for this element kind.
    return null;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.DYNAMIC;
  }

  /**
   * Return the type defined by this element.
   * 
   * @return the type defined by this element
   */
  public DynamicTypeImpl getType() {
    return type;
  }

  /**
   * Set the type defined by this element to the given type.
   * 
   * @param type the type defined by this element
   */
  public void setType(DynamicTypeImpl type) {
    this.type = type;
  }
}
