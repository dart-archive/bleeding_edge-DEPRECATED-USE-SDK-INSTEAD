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

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchScope;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import java.util.Collection;
import java.util.Map;

public class CalleeAnalyzerVisitor extends DartNodeTraverser<Void> {

  private static Method findIncludingSupertypes(MethodElement method, Type type, IProgressMonitor pm)
      throws DartModelException {
//    Method inThisType = Bindings.findMethod(method, type);
//    if (inThisType != null) {
//      return inThisType;
//    }
//    Type[] superTypes = DartModelUtil.getAllSuperTypes(type, pm);
//    for (int i = 0; i < superTypes.length; i++) {
//      Method m = Bindings.findMethod(method, superTypes[i]);
//      if (m != null) {
//        return m;
//      }
//    }
    return null;
  }

  private final CallSearchResultCollector fSearchResults;
  private final DartElement fMember;
  private final DartUnit fCompilationUnit;
  private final IProgressMonitor fProgressMonitor;
  private int fMethodEndPosition;

  private int fMethodStartPosition;

  CalleeAnalyzerVisitor(DartElement member, DartUnit compilationUnit,
      IProgressMonitor progressMonitor) {
    fSearchResults = new CallSearchResultCollector();
    this.fMember = member;
    this.fCompilationUnit = compilationUnit;
    this.fProgressMonitor = progressMonitor;

    try {
      SourceRange sourceRange = ((SourceReference) member).getSourceRange();
      this.fMethodStartPosition = sourceRange.getOffset();
      this.fMethodEndPosition = fMethodStartPosition + sourceRange.getLength();
    } catch (DartModelException jme) {
      DartCore.logError(jme);
    }
  }

  /**
   * @return a map from handle identifier ({@link String}) to {@link MethodCall}
   */
  public Map<String, MethodCall> getCallees() {
    return fSearchResults.getCallers();
  }

//  @Override
//  public boolean visit(AbstractTypeDeclaration node) {
//    progressMonitorWorked(1);
//    if (!isFurtherTraversalNecessary(node)) {
//      return false;
//    }
//
//    if (isNodeWithinMethod(node)) {
//      List<BodyDeclaration> bodyDeclarations = node.bodyDeclarations();
//      for (Iterator<BodyDeclaration> iter = bodyDeclarations.iterator(); iter.hasNext();) {
//        BodyDeclaration bodyDeclaration = iter.next();
//        if (bodyDeclaration instanceof MethodDeclaration) {
//          MethodDeclaration child = (MethodDeclaration) bodyDeclaration;
//          if (child.isConstructor()) {
//            addMethodCall(child.resolveBinding(), child.getName());
//          }
//        }
//      }
//      return false;
//    }
//
//    return true;
//  }
//
//  /**
//   * When an anonymous class declaration is reached, the traversal should not go further since it's
//   * not supposed to consider calls inside the anonymous inner class as calls from the outer method.
//   * 
//   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnonymousClassDeclaration)
//   */
//  @Override
//  public boolean visit(AnonymousClassDeclaration node) {
//    return isNodeEnclosingMethod(node);
//  }
//
//  @Override
//  public boolean visit(ClassInstanceCreation node) {
//    progressMonitorWorked(1);
//    if (!isFurtherTraversalNecessary(node)) {
//      return false;
//    }
//
//    if (isNodeWithinMethod(node)) {
//      addMethodCall(node.resolveConstructorBinding(), node);
//    }
//
//    return true;
//  }
//
//  /**
//   * Find all constructor invocations (<code>this(...)</code>) from the called method. Since we only
//   * traverse into the AST on the wanted method declaration, this method should not hit on more
//   * constructor invocations than those in the wanted method.
//   * 
//   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ConstructorInvocation)
//   */
//  @Override
//  public boolean visit(ConstructorInvocation node) {
//    progressMonitorWorked(1);
//    if (!isFurtherTraversalNecessary(node)) {
//      return false;
//    }
//
//    if (isNodeWithinMethod(node)) {
//      addMethodCall(node.resolveConstructorBinding(), node);
//    }
//
//    return true;
//  }
//
//  @Override
//  public boolean visit(MethodDeclaration node) {
//    progressMonitorWorked(1);
//    return isFurtherTraversalNecessary(node);
//  }
//
//  /**
//   * Find all method invocations from the called method. Since we only traverse into the AST on the
//   * wanted method declaration, this method should not hit on more method invocations than those in
//   * the wanted method.
//   * 
//   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
//   */
//  @Override
//  public boolean visit(MethodInvocation node) {
//    progressMonitorWorked(1);
//    if (!isFurtherTraversalNecessary(node)) {
//      return false;
//    }
//
//    if (isNodeWithinMethod(node)) {
//      addMethodCall(node.resolveMethodBinding(), node);
//    }
//
//    return true;
//  }
//
//  /**
//   * Find invocations of the supertype's constructor from the called method (=constructor). Since we
//   * only traverse into the AST on the wanted method declaration, this method should not hit on more
//   * method invocations than those in the wanted method.
//   * 
//   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SuperConstructorInvocation)
//   */
//  @Override
//  public boolean visit(SuperConstructorInvocation node) {
//    progressMonitorWorked(1);
//    if (!isFurtherTraversalNecessary(node)) {
//      return false;
//    }
//
//    if (isNodeWithinMethod(node)) {
//      addMethodCall(node.resolveConstructorBinding(), node);
//    }
//
//    return true;
//  }
//
//  /**
//   * Find all method invocations from the called method. Since we only traverse into the AST on the
//   * wanted method declaration, this method should not hit on more method invocations than those in
//   * the wanted method.
//   * 
//   * @param node node to visit
//   * @return whether children should be visited
//   * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
//   */
//  @Override
//  public boolean visit(SuperMethodInvocation node) {
//    progressMonitorWorked(1);
//    if (!isFurtherTraversalNecessary(node)) {
//      return false;
//    }
//
//    if (isNodeWithinMethod(node)) {
//      addMethodCall(node.resolveMethodBinding(), node);
//    }
//
//    return true;
//  }

  /**
   * Adds the specified method binding to the search results.
   * 
   * @param calledMethodBinding
   * @param node
   */
  protected void addMethodCall(MethodElement calledMethodBinding, DartNode node) {
//    try {
//      if (calledMethodBinding != null) {
//        fProgressMonitor.worked(1);
//
//        ClassElement calledTypeBinding = calledMethodBinding.getDeclaringClass();
//        Type calledType = null;
//
//        calledType = (Type) calledTypeBinding.getType();
//
//        Method calledMethod = findIncludingSupertypes(calledMethodBinding, calledType,
//            fProgressMonitor);
//
//        TypeMember referencedMember = null;
//        if (calledMethod == null) {
//          if (calledMethodBinding.isConstructor()
//              && calledMethodBinding.getParameters().size() == 0) {
//            referencedMember = calledType;
//          }
//        } else {
//          if (calledType.isInterface()) {
//            calledMethod = findImplementingMethods(calledMethod);
//          }
//
//          if (!isIgnoredBySearchScope(calledMethod)) {
//            referencedMember = calledMethod;
//          }
//        }
//        final int position = node.getStartPosition();
//        final int number = fCompilationUnit.getLineNumber(position);
//        fSearchResults.addMember(fMember, referencedMember, position, position + node.getLength(),
//            number < 1 ? 1 : number);
//      }
//    } catch (DartModelException jme) {
//      DartToolsPlugin.log(jme);
//    }
  }

  private Method findImplementingMethods(Method calledMethod) {
    Collection<DartElement> implementingMethods = CallHierarchy.getDefault().getImplementingMethods(
        calledMethod);

    if ((implementingMethods.size() == 0) || (implementingMethods.size() > 1)) {
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
    int nodeStartPosition = node.getSourceStart();
    int nodeEndPosition = nodeStartPosition + node.getSourceLength();

    if (nodeStartPosition < fMethodStartPosition && nodeEndPosition > fMethodEndPosition) {
      // Is the method completely enclosed by the node?
      return true;
    }
    return false;
  }

  private boolean isNodeWithinMethod(DartNode node) {
    int nodeStartPosition = node.getSourceStart();
    int nodeEndPosition = nodeStartPosition + node.getSourceLength();

    if (nodeStartPosition < fMethodStartPosition) {
      return false;
    }

    if (nodeEndPosition > fMethodEndPosition) {
      return false;
    }

    return true;
  }

  private void progressMonitorWorked(int work) {
    if (fProgressMonitor != null) {
      fProgressMonitor.worked(work);
      if (fProgressMonitor.isCanceled()) {
        throw new OperationCanceledException();
      }
    }
  }
}
