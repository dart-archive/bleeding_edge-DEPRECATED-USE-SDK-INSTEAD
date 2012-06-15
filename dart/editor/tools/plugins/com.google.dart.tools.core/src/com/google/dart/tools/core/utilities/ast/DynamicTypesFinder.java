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
package com.google.dart.tools.core.utilities.ast;

import com.google.common.collect.Lists;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.compiler.ast.Modifiers;
import com.google.dart.compiler.type.DynamicType;
import com.google.dart.compiler.type.Type;

import java.util.List;

/**
 * Instances of the class <code>DynamicTypesFinder</code> find AST nodes whose type is dynamic.
 */
public class DynamicTypesFinder extends ASTVisitor<Void> {

  /**
   * Test to see if the given identifier's type is dynamic.
   * 
   * @param node the identifier to test
   * @return <code>true</code> if the identifier has a dynamic type, <code>false</code> otherwise
   */
  public static boolean isDynamic(DartIdentifier node) {
    Type type = node.getType();
    if (type == null) {

      DartNode parent = node.getParent();

      if (parent instanceof DartField) {

        //skip getters/setters
        Modifiers modifiers = ((DartField) parent).getModifiers();
        if (modifiers.isGetter() || modifiers.isSetter()) {
          return false;
        }

        parent = parent.getParent();
        if (parent instanceof DartFieldDefinition) {
          DartTypeNode typeNode = ((DartFieldDefinition) parent).getTypeNode();
          return typeNode == null || typeNode.getType() instanceof DynamicType;
        }
      }

      if (parent instanceof DartVariable) {
        parent = parent.getParent();
        if (parent instanceof DartVariableStatement) {
          DartTypeNode typeNode = ((DartVariableStatement) parent).getTypeNode();
          return typeNode == null || typeNode.getType() instanceof DynamicType;
        }
      }

    }
    //to match inferred types as well, comment out the isInferred() test
    return /* type != null && type.isInferred() ||*/type instanceof DynamicType;
  }

  private final List<DartIdentifier> matches = Lists.newArrayList();

  /**
   * Get matched AST nodes.
   * 
   * @return matched AST nodes.
   */
  public Iterable<DartIdentifier> getMatches() {
    return matches;
  }

  /**
   * Perform the search.
   * 
   * @param ast the AST to search within
   */
  public void searchWithin(DartNode ast) {
    ast.accept(this);
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {

    if (isDynamic(node)) {
      matches.add(node);
    }

    return super.visitIdentifier(node);
  }
}
