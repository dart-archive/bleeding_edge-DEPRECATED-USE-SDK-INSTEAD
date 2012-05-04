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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>MethodInvocationContributor</code> implement a contributor that adds
 * a reference every time it finds an invocation of a method (or constructor).
 */
public class MethodInvocationContributor extends ScopedDartContributor {
  @Override
  public Void visitArrayAccess(DartArrayAccess node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      // TODO(brianwilkerson) Find the source range associated with the operator.
      processMethod(null, (MethodElement) element);
    }
    return super.visitArrayAccess(node);
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      // TODO(brianwilkerson) Find the source range associated with the operator.
      processMethod(null, (MethodElement) element);
    }
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      processMethod(getRange(node.getTarget()), (MethodElement) element);
    }
    return super.visitFunctionObjectInvocation(node);
  }

  @Override
  public Void visitMethodInvocation(DartMethodInvocation node) {
    Element element = node.getElement();
    if (element == null) {
      element = node.getElement();
    }
    if (element instanceof MethodElement) {
      processMethod(getRange(node.getFunctionName()), (MethodElement) element);
    }
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNewExpression(DartNewExpression node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      DartNode className = node.getConstructor();
      if (className instanceof DartPropertyAccess) {
        className = ((DartPropertyAccess) className).getName();
      } else if (className instanceof DartTypeNode) {
        className = ((DartTypeNode) className).getIdentifier();
      }
      processMethod(getRange(className), (MethodElement) element);
    }
    return super.visitNewExpression(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      processMethod(getRange(node.getName()), (MethodElement) element);
    }
    return super.visitSuperConstructorInvocation(node);
  }

  @Override
  public Void visitUnaryExpression(DartUnaryExpression node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) element;
      // TODO(brianwilkerson) Find the source range associated with the operator.
      processMethod(null, methodElement);
    }
    return super.visitUnaryExpression(node);
  }

  @Override
  public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    Element element = node.getElement();
    if (element instanceof MethodElement) {
      processMethod(getRange(node.getTarget()), (MethodElement) element);
    }
    return super.visitUnqualifiedInvocation(node);
  }

  private SourceRange getRange(DartNode node) {
    return node == null ? null : new SourceRangeImpl(node);
  }

  /**
   * Record the invocation of a method or function.
   * 
   * @param sourceRange the source range of the name of the method at the invocation site, or
   *          <code>null</code> if the source range is unknown
   * @param binding the element representing the method being invoked
   */
  private void processMethod(SourceRange sourceRange, MethodElement binding) {
    DartFunction function = getDartElement(binding);
    if (function instanceof Method) {
      Method method = (Method) function;
      try {
        recordRelationship(
            peekTarget(sourceRange),
            new MethodLocation(method, method.getNameRange()));
      } catch (DartModelException exception) {
        DartCore.logError("Could not get range for method " + method.getElementName(), exception);
      }
    }
  }
}
