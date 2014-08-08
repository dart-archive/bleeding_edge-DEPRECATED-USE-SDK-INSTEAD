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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.ObjectUtilities;

/**
 * Instances of the class {@code VariableResolverVisitor} are used to resolve
 * {@link SimpleIdentifier}s to local variables and formal parameters.
 * 
 * @coverage dart.engine.resolver
 */
public class VariableResolverVisitor extends ScopedVisitor {
  /**
   * The method or function that we are currently visiting, or {@code null} if we are not inside a
   * method or function.
   */
  private ExecutableElement enclosingFunction;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public VariableResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in an AST node.
   * 
   * @param definingLibrary the element for the library containing the node being visited
   * @param source the source representing the compilation unit containing the node being visited
   * @param typeProvider the object used to access the types from the core library
   * @param nameScope the scope used to resolve identifiers in the node that will first be visited
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   */
  public VariableResolverVisitor(LibraryElement definingLibrary, Source source,
      TypeProvider typeProvider, Scope nameScope, AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, nameScope, errorListener);
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public VariableResolverVisitor(ResolvableLibrary library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      return super.visitFunctionDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    if (!(node.getParent() instanceof FunctionDeclaration)) {
      ExecutableElement outerFunction = enclosingFunction;
      try {
        enclosingFunction = node.getElement();
        return super.visitFunctionExpression(node);
      } finally {
        enclosingFunction = outerFunction;
      }
    } else {
      return super.visitFunctionExpression(node);
    }
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    // Ignore if already resolved - declaration or type.
    if (node.getStaticElement() != null) {
      return null;
    }
    // Ignore if qualified.
    AstNode parent = node.getParent();
    if (parent instanceof PrefixedIdentifier
        && ((PrefixedIdentifier) parent).getIdentifier() == node) {
      return null;
    }
    if (parent instanceof PropertyAccess && ((PropertyAccess) parent).getPropertyName() == node) {
      return null;
    }
    if (parent instanceof MethodInvocation && ((MethodInvocation) parent).getMethodName() == node) {
      return null;
    }
    if (parent instanceof ConstructorName) {
      return null;
    }
    if (parent instanceof Label) {
      return null;
    }
    // Prepare VariableElement.
    Element element = getNameScope().lookup(node, getDefiningLibrary());
    if (!(element instanceof VariableElement)) {
      return null;
    }
    // Must be local or parameter.
    ElementKind kind = element.getKind();
    if (kind == ElementKind.LOCAL_VARIABLE) {
      node.setStaticElement(element);
      if (node.inSetterContext()) {
        LocalVariableElementImpl variableImpl = (LocalVariableElementImpl) element;
        variableImpl.markPotentiallyMutatedInScope();
        if (!ObjectUtilities.equals(element.getEnclosingElement(), enclosingFunction)) {
          variableImpl.markPotentiallyMutatedInClosure();
        }
      }
    } else if (kind == ElementKind.PARAMETER) {
      node.setStaticElement(element);
      if (node.inSetterContext()) {
        ParameterElementImpl parameterImpl = (ParameterElementImpl) element;
        parameterImpl.markPotentiallyMutatedInScope();
        // If we are in some closure, check if it is not the same as where variable is declared.
        if (enclosingFunction != null
            && !ObjectUtilities.equals(element.getEnclosingElement(), enclosingFunction)) {
          parameterImpl.markPotentiallyMutatedInClosure();
        }
      }
    }
    return null;
  }
}
