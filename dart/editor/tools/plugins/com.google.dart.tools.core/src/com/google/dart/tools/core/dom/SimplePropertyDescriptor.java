/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.dom;

import com.google.dart.compiler.ast.DartNode;

/**
 * Instances of the class <code>SimplePropertyDescriptor</code>
 */
public final class SimplePropertyDescriptor extends StructuralPropertyDescriptor {
  /**
   * Value type. For example, for a node type like SingleVariableDeclaration, the modifiers property
   * is int.class
   */
  private final Class<?> valueType;

  /**
   * Indicates whether a value is mandatory. A property value is allowed to be <code>null</code>
   * only if it is not mandatory.
   */
  private final boolean mandatory;

  /**
   * Creates a new simple property descriptor with the given property id. Note that this constructor
   * is declared package-private so that property descriptors can only be created by the AST
   * implementation.
   * 
   * @param nodeClass concrete AST node type that owns this property
   * @param propertyId the property id
   * @param valueType the value type of this property
   * @param mandatory <code>true</code> if the property is mandatory, and <code>false</code> if it
   *          is may be <code>null</code>
   */
  SimplePropertyDescriptor(Class<? extends DartNode> nodeClass, String propertyId,
      Class<?> valueType, boolean mandatory) {
    super(nodeClass, propertyId);
    if (valueType == null || DartNode.class.isAssignableFrom(valueType)) {
      throw new IllegalArgumentException();
    }
    this.valueType = valueType;
    this.mandatory = mandatory;
  }

  /**
   * Return the value type of this property.
   * <p>
   * For example, for a node type like SingleVariableDeclaration, the "modifiers" property returns
   * <code>int.class</code>.
   * 
   * @return the value type of the property
   */
  public Class<?> getValueType() {
    return valueType;
  }

  /**
   * Return <code>true</code> if this property is mandatory. A property value is not allowed to be
   * <code>null</code> if it is mandatory.
   * 
   * @return <code>true</code> if the property is mandatory, and <code>false</code> if it is may be
   *         <code>null</code>
   */
  public boolean isMandatory() {
    return mandatory;
  }
}
