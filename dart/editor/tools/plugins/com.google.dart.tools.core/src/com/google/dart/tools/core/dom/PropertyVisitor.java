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
import com.google.dart.compiler.ast.ASTVisitor;

import java.util.List;

/**
 * The abstract class <code>PropertyVisitor</code> implements behavior common to visitors that
 * traverse the type hierarchy in order to manipulate the properties of a node.
 */
public abstract class PropertyVisitor extends ASTVisitor<Object> {
  /**
   * The specification of the property being manipulated.
   */
  protected StructuralPropertyDescriptor property;

  /**
   * Initialize a newly created visitor to manipulate the given property.
   * 
   * @param property the specification of the property being manipulated
   */
  public PropertyVisitor(StructuralPropertyDescriptor property) {
    this.property = property;
  }

  @Override
  public void visit(List<? extends DartNode> nodes) {
    if (nodes != null) {
      for (DartNode node : nodes) {
        node.accept(this);
      }
    }
  }

  protected void noSuchProperty(String nodeClassName) {
    throw new RuntimeException("No property named " + property.getId()
        + " defined for instances of " + nodeClassName);
  }
}
