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

package com.google.dart.java2dart.processor;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.ExecutionUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * {@link SemanticProcessor} subclasses perform semantic translation of some specific syntax or
 * library.
 */
public abstract class SemanticProcessor {

  /**
   * @return the {@link ASTNode} of given {@link Class} which is given {@link ASTNode} itself, or
   *         one of its parents.
   */
  @SuppressWarnings("unchecked")
  public static <E extends ASTNode> E getAncestor(ASTNode node, Class<E> enclosingClass) {
    while (node != null && !enclosingClass.isInstance(node)) {
      node = node.getParent();
    };
    return (E) node;
  }

  /**
   * Replaces "node" with "replacement" in parent of "node".
   */
  protected static void replaceNode(ASTNode node, ASTNode replacement) {
    ASTNode parent = node.getParent();
    Class<? extends ASTNode> parentClass = parent.getClass();
    // try get/set methods
    try {
      for (Method getMethod : parentClass.getMethods()) {
        String getName = getMethod.getName();
        if (getName.startsWith("get") && getMethod.getParameterTypes().length == 0
            && getMethod.invoke(parent) == node) {
          String setName = "set" + getName.substring(3);
          Method setMethod = parentClass.getMethod(setName, getMethod.getReturnType());
          setMethod.invoke(parent, replacement);
          return;
        }
      }
    } catch (Throwable e) {
      ExecutionUtils.propagate(e);
    }
    // special cases
    if (parent instanceof ListLiteral) {
      List<Expression> elements = ((ListLiteral) parent).getElements();
      int index = elements.indexOf(node);
      if (index != -1) {
        elements.set(index, (Expression) replacement);
        return;
      }
    }
    if (parent instanceof ArgumentList) {
      List<Expression> arguments = ((ArgumentList) parent).getArguments();
      int index = arguments.indexOf(node);
      if (index != -1) {
        arguments.set(index, (Expression) replacement);
        return;
      }
    }
    if (parent instanceof TypeArgumentList) {
      List<TypeName> arguments = ((TypeArgumentList) parent).getArguments();
      int index = arguments.indexOf(node);
      if (index != -1) {
        arguments.set(index, (TypeName) replacement);
        return;
      }
    }
    // not found
    throw new UnsupportedOperationException("" + parentClass);
//    if (parent instanceof ConditionalExpression) {
//      ConditionalExpression p = (ConditionalExpression) parent;
//      if (p.getThenExpression() == node) {
//        p.setThenExpression((Expression) replacement);
//        return;
//      }
//    }
//    if (parent instanceof ReturnStatement) {
//      ReturnStatement p = (ReturnStatement) parent;
//      p.setExpression((Expression) replacement);
//      return;
//    }
//    if (parent instanceof BinaryExpression) {
//      BinaryExpression p = (BinaryExpression) parent;
//      if (p.getLeftOperand() == node) {
//        p.setLeftOperand((Expression) replacement);
//        return;
//      }
//      if (p.getRightOperand() == node) {
//        p.setRightOperand((Expression) replacement);
//        return;
//      }
//    }
//    if (parent instanceof VariableDeclaration) {
//      VariableDeclaration p = (VariableDeclaration) parent;
//      if (p.getInitializer() == node) {
//        p.setInitializer((Expression) replacement);
//        return;
//      }
//    }
  }

  abstract public void process(Context context, CompilationUnit unit);
}
