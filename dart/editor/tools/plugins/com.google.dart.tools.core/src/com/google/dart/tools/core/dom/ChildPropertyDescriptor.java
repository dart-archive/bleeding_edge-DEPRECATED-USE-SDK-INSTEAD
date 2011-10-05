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

import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;

/**
 * Instances of the class <code>ChildPropertyDescriptor</code> describe a child property of an AST
 * node. A child property is one whose value is an {@link DartNode}.
 */
public final class ChildPropertyDescriptor extends StructuralPropertyDescriptor {
  /**
   * Child type. For example, for a node type like CompilationUnit, the "package" property is
   * PackageDeclaration.class
   */
  private final Class<? extends DartNode> childClass;

  /**
   * Indicates whether the child is mandatory. A child property is allowed to be <code>null</code>
   * only if it is not mandatory.
   */
  private final boolean mandatory;

  /**
   * Indicates whether a cycle is possible. Field is private, but marked package-visible for fast
   * access from ASTNode.
   */
  final boolean cycleRisk;

  /**
   * Creates a new child property descriptor with the given property id. Note that this constructor
   * is declared package-private so that property descriptors can only be created by the AST
   * implementation.
   * 
   * @param nodeClass concrete AST node type that owns this property
   * @param propertyId the property id
   * @param childType the child type of this property
   * @param mandatory <code>true</code> if the property is mandatory, and <code>false</code> if it
   *          is may be <code>null</code>
   * @param cycleRisk <code>true</code> if this property is at risk of cycles, and
   *          <code>false</code> if there is no worry about cycles
   */
  ChildPropertyDescriptor(Class<? extends DartNode> nodeClass, String propertyId,
      Class<? extends DartNode> childType, boolean mandatory, boolean cycleRisk) {
    super(nodeClass, propertyId);
    if (childType == null || !DartNode.class.isAssignableFrom(childType)) {
      throw new IllegalArgumentException();
    }
    this.childClass = childType;
    this.mandatory = mandatory;
    this.cycleRisk = cycleRisk;
  }

  /**
   * Return <code>true</code> if this property is vulnerable to cycles.
   * <p>
   * A property is vulnerable to cycles if a node of the owning type (that is, the type that owns
   * this property) could legally appear in the AST subtree below this property. For example, the
   * body property of a {@link DartMethodDefinition} node admits a body which might include
   * statement that embeds another {@link DartMethodDefinition} node. On the other hand, the name
   * property of a {@link DartMethodDefinition} node admits only names, and thereby excludes another
   * {@link DartMethodDefinition} node.
   * 
   * @return <code>true</code> if cycles are possible, and <code>false</code> if cycles are
   *         impossible
   */
  public final boolean cycleRisk() {
    return cycleRisk;
  }

  /**
   * Return the child type of this property.
   * <p>
   * For example, for a node type like CompilationUnit, the "package" property returns
   * <code>PackageDeclaration.class</code>.
   * 
   * @return the child type of the property
   */
  public final Class<? extends DartNode> getChildType() {
    return childClass;
  }

  /**
   * Return <code>true</code> if this property is mandatory. A property value is not allowed to be
   * <code>null</code> if it is mandatory.
   * 
   * @return <code>true</code> if the property is mandatory, and <code>false</code> if it is may be
   *         <code>null</code>
   */
  public final boolean isMandatory() {
    return mandatory;
  }
}
