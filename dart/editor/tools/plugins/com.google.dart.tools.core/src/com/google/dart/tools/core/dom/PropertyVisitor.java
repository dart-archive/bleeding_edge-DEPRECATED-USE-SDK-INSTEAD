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

import com.google.dart.compiler.ast.DartClassMember;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartGotoStatement;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLiteral;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPlainVisitor;
import com.google.dart.compiler.ast.DartStatement;
import com.google.dart.compiler.ast.DartSwitchMember;

import java.util.List;

/**
 * The abstract class <code>PropertyVisitor</code> implements behavior common to visitors that
 * traverse the type hierarchy in order to manipulate the properties of a node.
 */
public abstract class PropertyVisitor implements DartPlainVisitor<Object> {
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

  public abstract <N extends DartExpression> Object visitClassMember(DartClassMember<N> node);

  public abstract Object visitExpression(DartExpression node);

  public abstract Object visitGotoStatement(DartGotoStatement node);

  public abstract Object visitInvocation(DartInvocation node);

  public abstract Object visitLiteral(DartLiteral node);

  public abstract Object visitNode(DartNode node);

  public abstract Object visitStatement(DartStatement node);

  public abstract Object visitSwitchMember(DartSwitchMember node);

  protected void noSuchProperty(String nodeClassName) {
    throw new RuntimeException("No property named " + property.getId()
        + " defined for instances of " + nodeClassName);
  }
}
