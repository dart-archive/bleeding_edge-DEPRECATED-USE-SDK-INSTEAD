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
package com.google.dart.tools.core.internal.indexer.contributor;

import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.common.Symbol;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.indexer.location.MethodLocation;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.Method;

/**
 * Instances of the class <code>MethodInvocationContributor</code> implement a contributor that adds
 * a reference every time it finds an invocation of a method (or constructor).
 */
public class MethodInvocationContributor extends ScopedDartContributor {
  @Override
  public Void visitArrayAccess(DartArrayAccess node) {
    DartCore.notYetImplemented();
    return super.visitArrayAccess(node);
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    DartCore.notYetImplemented();
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    DartCore.notYetImplemented();
    return super.visitFunctionObjectInvocation(node);
  }

  @Override
  public Void visitMethodInvocation(DartMethodInvocation node) {
    Symbol symbol = node.getTargetSymbol();
    if (symbol instanceof MethodElement) {
      processMethod(node.getFunctionName(), (MethodElement) symbol);
    }
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNewExpression(DartNewExpression node) {
    DartCore.notYetImplemented();
    return super.visitNewExpression(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    DartCore.notYetImplemented();
    return super.visitSuperConstructorInvocation(node);
  }

  @Override
  public Void visitUnaryExpression(DartUnaryExpression node) {
    DartCore.notYetImplemented();
    return super.visitUnaryExpression(node);
  }

  @Override
  public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    DartCore.notYetImplemented();
    return super.visitUnqualifiedInvocation(node);
  }

  private void processMethod(DartIdentifier functionName, MethodElement binding) {
    DartFunction function = getDartElement(binding);
    if (function instanceof Method) {
      Method method = (Method) function;
      recordRelationship(new MethodLocation(method, new SourceRangeImpl(functionName)),
          peekTarget());
    }

  }
}
