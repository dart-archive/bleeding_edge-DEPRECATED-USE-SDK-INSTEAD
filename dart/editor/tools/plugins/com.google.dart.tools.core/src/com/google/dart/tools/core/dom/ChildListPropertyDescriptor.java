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
 * Instances of the class <code>ChildListPropertyDescriptor</code> implement a property descriptor
 * for a child list property of an AST node. A child list property is one whose value is a list of
 * {@link DartNode}.
 */
public final class ChildListPropertyDescriptor extends StructuralPropertyDescriptor {
  /**
   * Element type. For example, for a node type like CompilationUnit, the "imports" property is
   * ImportDeclaration.class.
   * <p>
   * Field is private, but marked package-visible for fast access from DartNode.
   */
  final Class<? extends DartNode> elementType;

  /**
   * Indicates whether a cycle is possible.
   * <p>
   * Field is private, but marked package-visible for fast access from DartNode.
   */
  final boolean cycleRisk;

  /**
   * Initialize a newly created child list property descriptor with the given property id. Note that
   * this constructor is declared package-private so that property descriptors can only be created
   * by the AST implementation.
   * 
   * @param nodeClass concrete AST node type that owns this property
   * @param propertyId the property id
   * @param elementType the element type of this property
   * @param cycleRisk <code>true</code> if this property is at risk of cycles, and
   *          <code>false</code> if there is no worry about cycles
   */
  ChildListPropertyDescriptor(Class<? extends DartNode> nodeClass, String propertyId,
      Class<? extends DartNode> elementType, boolean cycleRisk) {
    super(nodeClass, propertyId);
    if (elementType == null) {
      throw new IllegalArgumentException();
    }
    this.elementType = elementType;
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
   * Return the element type of this list property.
   * <p>
   * For example, for a node type like CompilationUnit, the "imports" property returns
   * <code>ImportDeclaration.class</code>.
   * 
   * @return the element type of the property
   */
  public final Class<? extends DartNode> getElementType() {
    return elementType;
  }
}
