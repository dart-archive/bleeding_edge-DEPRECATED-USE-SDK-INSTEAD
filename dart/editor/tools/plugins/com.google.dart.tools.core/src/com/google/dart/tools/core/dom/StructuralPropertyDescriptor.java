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

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;

/**
 * The abstract class <code>StructuralPropertyDescriptor</code> defines the behavior of property
 * descriptors of AST nodes. There are three kinds of properties:
 * <ul>
 * <li>simple properties ({@link SimplePropertyDescriptor}) - properties where the value is a
 * primitive (int, boolean) or simple (String, InfixExprsssion.Operator) type other than an AST
 * node; for example, the identifier of a {@link DartIdentifier}</li>
 * <li>child properties ({@link ChildPropertyDescriptor}) - properties whose value is another AST
 * node; for example, the name of a {@link DartMethodDefinition}</li>
 * <li>child list properties ({@link ChildListPropertyDescriptor}) - properties where the value is a
 * list of AST nodes; for example, the statements of a {@link DartBlock}</li>
 * </ul>
 */
public abstract class StructuralPropertyDescriptor {
  /**
   * Property id.
   */
  private final String propertyId;

  /**
   * The concrete AST node type that owns this property.
   */
  private final Class<? extends DartNode> nodeClass;

  /**
   * Internal convenience constant indicating that there is definite risk of cycles.
   */
  public static final boolean CYCLE_RISK = true;

  /**
   * Internal convenience constant indicating that there is no risk of cycles.
   */
  public static final boolean NO_CYCLE_RISK = false;

  /**
   * Internal convenience constant indicating that a structural property is mandatory.
   */
  public static final boolean MANDATORY = true;

  /**
   * Internal convenience constant indicating that a structural property is optional.
   */
  public static final boolean OPTIONAL = false;

  /**
   * Creates a new property descriptor for the given node type with the given property id. Note that
   * this constructor is declared package-private so that property descriptors can only be created
   * by the AST implementation.
   * 
   * @param nodeClass concrete AST node type that owns this property
   * @param propertyId the property id
   */
  StructuralPropertyDescriptor(Class<? extends DartNode> nodeClass, String propertyId) {
    if (nodeClass == null || propertyId == null) {
      throw new IllegalArgumentException();
    }
    this.propertyId = propertyId;
    this.nodeClass = nodeClass;
  }

  /**
   * Return the id of this property.
   * 
   * @return the property id
   */
  public final String getId() {
    return propertyId;
  }

  /**
   * Return the AST node type that owns this property.
   * <p>
   * For example, for all properties of the node type DartClass, this method returns
   * <code>DartClass.class</code>.
   * 
   * @return the node type that owns this property
   */
  public final Class<? extends DartNode> getNodeClass() {
    return nodeClass;
  }

  /**
   * Return <code>true</code> if this property is a child list property (instance of
   * {@link ChildListPropertyDescriptor}.
   * 
   * @return <code>true</code> if this is a child list property, and <code>false</code> otherwise
   */
  public final boolean isChildListProperty() {
    return (this instanceof ChildListPropertyDescriptor);
  }

  /**
   * Return <code>true</code> if this property is a child property (instance of
   * {@link ChildPropertyDescriptor}.
   * 
   * @return <code>true</code> if this is a child property, and <code>false</code> otherwise
   */
  public final boolean isChildProperty() {
    return (this instanceof ChildPropertyDescriptor);
  }

  /**
   * Return <code>true</code> if this property is a simple property (instance of
   * {@link SimplePropertyDescriptor}.
   * 
   * @return <code>true</code> if this is a simple property, and <code>false</code> otherwise
   */
  public final boolean isSimpleProperty() {
    return (this instanceof SimplePropertyDescriptor);
  }

  /**
   * Return a string suitable for debug purposes.
   * 
   * @return {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuffer b = new StringBuffer();
    if (isChildListProperty()) {
      b.append("ChildList"); //$NON-NLS-1$
    }
    if (isChildProperty()) {
      b.append("Child"); //$NON-NLS-1$
    }
    if (isSimpleProperty()) {
      b.append("Simple"); //$NON-NLS-1$
    }
    b.append("Property["); //$NON-NLS-1$
    if (nodeClass != null) {
      b.append(nodeClass.getName());
    }
    b.append(","); //$NON-NLS-1$
    if (propertyId != null) {
      b.append(propertyId);
    }
    b.append("]"); //$NON-NLS-1$
    return b.toString();
  }
}
