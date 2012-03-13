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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import java.util.Collection;
import java.util.Map;

public class CalleeAnalyzerVisitor extends ASTVisitor<Void> {

  private final CallSearchResultCollector searchResults;
  private final DartElement memberToAnalyze;
  private final IProgressMonitor progressMonitor;
  private int methodEndPosition;
  private int methodStartPosition;

  CalleeAnalyzerVisitor(DartElement member, IProgressMonitor progressMonitor) {
    searchResults = new CallSearchResultCollector();
    this.memberToAnalyze = member;
    this.progressMonitor = progressMonitor;

    try {
      SourceRange sourceRange = ((SourceReference) member).getSourceRange();
      this.methodStartPosition = sourceRange.getOffset();
      this.methodEndPosition = methodStartPosition + sourceRange.getLength();
    } catch (DartModelException jme) {
      DartCore.logError(jme);
    }
  }

  /**
   * Return the result of analyzing a method.
   * 
   * @return a map from handle identifier ({@link String}) to call site {@link MethodCall}
   */
  public Map<String, MethodCall> getCallees() {
    return searchResults.getCallers();
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitMethodInvocation(DartMethodInvocation node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitNewExpression(DartNewExpression node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitPropertyAccess(DartPropertyAccess node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    return visitInvocationExpression(node);
  }

  @Override
  public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    return visitInvocationExpression(node);
  }

  /**
   * Adds the specified method element to the search results.
   */
  private void addMethodCall(Element calledMethodBinding, DartNode node) {
    if (calledMethodBinding == null) {
      return;
    }
    if (calledMethodBinding instanceof MethodElement) {
      progressMonitor.worked(1);
      MethodElement calledElement = (MethodElement) calledMethodBinding;
      Element classElement = calledElement.getEnclosingElement();
      Method calledMethod = (Method) BindingUtils.getDartElement(calledElement);
      TypeMember referencedMember = null;
      if ((classElement instanceof ClassElement) && ((ClassElement) classElement).isInterface()) {
        calledMethod = findImplementingMethods(calledMethod);
      }

      if (!isIgnoredBySearchScope(calledMethod)) {
        referencedMember = calledMethod;
      }
      final int position = node.getSourceInfo().getOffset();
      final int number = node.getSourceInfo().getLine();
      searchResults.addMember(memberToAnalyze, referencedMember, position, position
          + node.getSourceInfo().getLength(), number < 1 ? 1 : number);
    }
  }

  private Method findImplementingMethods(Method calledMethod) {
    Collection<DartElement> implementingMethods = CallHierarchy.getDefault().getImplementingMethods(
        calledMethod);
    if (implementingMethods.size() == 0 || implementingMethods.size() > 1) {
      return calledMethod;
    } else {
      return (Method) implementingMethods.iterator().next();
    }
  }

  private SearchScope getSearchScope() {
    return CallHierarchy.getDefault().getSearchScope();
  }

  private boolean isFurtherTraversalNecessary(DartNode node) {
    return isNodeWithinMethod(node) || isNodeEnclosingMethod(node);
  }

  private boolean isIgnoredBySearchScope(Method enclosingElement) {
    if (enclosingElement != null) {
      return !getSearchScope().encloses(enclosingElement);
    } else {
      return false;
    }
  }

  private boolean isNodeEnclosingMethod(DartNode node) {
    int nodeStartPosition = node.getSourceInfo().getOffset();
    int nodeEndPosition = nodeStartPosition + node.getSourceInfo().getLength();

    if (nodeStartPosition < methodStartPosition && nodeEndPosition > methodEndPosition) {
      // Is the method completely enclosed by the node?
      return true;
    }
    return false;
  }

  private boolean isNodeWithinMethod(DartNode node) {
    int nodeStartPosition = node.getSourceInfo().getOffset();
    int nodeEndPosition = nodeStartPosition + node.getSourceInfo().getLength();

    if (nodeStartPosition < methodStartPosition) {
      return false;
    }

    if (nodeEndPosition > methodEndPosition) {
      return false;
    }

    return true;
  }

  private void progressMonitorWorked(int work) {
    if (progressMonitor != null) {
      progressMonitor.worked(work);
      if (progressMonitor.isCanceled()) {
        throw new OperationCanceledException();
      }
    }
  }

  private Void visitInvocationExpression(DartExpression node) {
    progressMonitorWorked(1);
    if (isFurtherTraversalNecessary(node)) {
      if (isNodeWithinMethod(node)) {
        addMethodCall(node.getElement(), node);
      }
    }
    return visitExpression(node);
  }
}
