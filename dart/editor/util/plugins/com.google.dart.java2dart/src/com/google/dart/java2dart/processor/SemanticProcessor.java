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

import com.google.common.base.Objects;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.SyntaxTranslator;
import com.google.dart.java2dart.util.JavaUtils;

import org.eclipse.jdt.core.dom.IMethodBinding;

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
  public static void replaceNode(ASTNode node, ASTNode replacement) {
    SyntaxTranslator.replaceNode(node.getParent(), node, replacement);
  }

  protected final Context context;

  public SemanticProcessor(Context context) {
    this.context = context;
  }

  abstract public void process(CompilationUnit unit);

  /**
   * Checks if given {@link IMethodBinding} is method of given class with given name.
   */
  protected final boolean isMethodInClass(IMethodBinding binding, String reqName,
      String reqClassName) {
    return binding != null && Objects.equal(binding.getName(), reqName)
        && JavaUtils.isMethodInClass(binding, reqClassName);
  }

  /**
   * Checks if {@link IMethodBinding} of the given {@link MethodInvocation} is method of given
   * class.
   */
  protected final boolean isMethodInClass(MethodInvocation node, String reqClassName) {
    Object nodeBinding = context.getNodeBinding(node);
    if (nodeBinding instanceof IMethodBinding) {
      IMethodBinding binding = (IMethodBinding) nodeBinding;
      return JavaUtils.isMethodInClass(binding, reqClassName);
    }
    return false;
  }

  /**
   * Checks if {@link IMethodBinding} of the given {@link MethodInvocation} is method of given class
   * with given name.
   */
  protected final boolean isMethodInClass(MethodInvocation node, String reqName, String reqClassName) {
    Object nodeBinding = context.getNodeBinding(node);
    if (nodeBinding instanceof IMethodBinding) {
      IMethodBinding binding = (IMethodBinding) nodeBinding;
      return isMethodInClass(binding, reqName, reqClassName);
    }
    return false;
  }

  /**
   * Checks if given {@link IMethodBinding} is method of given class with given signature.
   */
  protected final boolean isMethodInClass2(IMethodBinding binding, String reqSignature,
      String reqClassName) {
    return JavaUtils.getMethodDeclarationSignature(binding).equals(reqSignature)
        && JavaUtils.isMethodInClass(binding, reqClassName);
  }

  /**
   * Checks if {@link IMethodBinding} of the given {@link MethodInvocation} is method of given class
   * with given signature.
   */
  protected final boolean isMethodInClass2(MethodInvocation node, String reqSignature,
      String reqClassName) {
    Object nodeBinding = context.getNodeBinding(node);
    if (nodeBinding instanceof IMethodBinding) {
      IMethodBinding binding = (IMethodBinding) nodeBinding;
      return isMethodInClass2(binding, reqSignature, reqClassName);
    }
    return false;
  }

  /**
   * Checks if given {@link IMethodBinding} is method of given class with given signature.
   */
  protected final boolean isMethodInExactClass(IMethodBinding binding, String reqSignature,
      String reqClassName) {
    return JavaUtils.getMethodDeclarationSignature(binding).equals(reqSignature)
        && JavaUtils.isTypeNamed(binding.getDeclaringClass(), reqClassName);
  }
}
