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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.resolver.IncrementalResolver;

/**
 * Instances of the class {@code ScopeBuilder} build the scope for a given node in an AST structure.
 * At the moment, this class only handles top-level and class-level declarations.
 */
public final class ScopeBuilder {
  /**
   * Return the scope in which the given AST structure should be resolved.
   * 
   * @param node the root of the AST structure to be resolved
   * @param errorListener the listener to which analysis errors will be reported
   * @return the scope in which the given AST structure should be resolved
   * @throws AnalysisException if the AST structure has not been resolved or is not part of a
   *           {@link CompilationUnit}
   */
  public static Scope scopeFor(AstNode node, AnalysisErrorListener errorListener)
      throws AnalysisException {
    if (node == null) {
      throw new AnalysisException("Cannot create scope: node is null");
    } else if (node instanceof CompilationUnit) {
      ScopeBuilder builder = new ScopeBuilder(errorListener);
      return builder.scopeForAstNode(node);
    }
    AstNode parent = node.getParent();
    if (parent == null) {
      throw new AnalysisException("Cannot create scope: node is not part of a CompilationUnit");
    }
    ScopeBuilder builder = new ScopeBuilder(errorListener);
    return builder.scopeForAstNode(parent);
  }

  /**
   * The listener to which analysis errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * Initialize a newly created scope builder to generate a scope that will report errors to the
   * given listener.
   * 
   * @param errorListener the listener to which analysis errors will be reported
   */
  private ScopeBuilder(AnalysisErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /**
   * Return the scope in which the given AST structure should be resolved.
   * <p>
   * <b>Note:</b> This method needs to be kept in sync with
   * {@link IncrementalResolver#canBeResolved(AstNode)}.
   * 
   * @param node the root of the AST structure to be resolved
   * @return the scope in which the given AST structure should be resolved
   * @throws AnalysisException if the AST structure has not been resolved or is not part of a
   *           {@link CompilationUnit}
   */
  private Scope scopeForAstNode(AstNode node) throws AnalysisException {
    if (node instanceof CompilationUnit) {
      return scopeForCompilationUnit((CompilationUnit) node);
    }
    AstNode parent = node.getParent();
    if (parent == null) {
      throw new AnalysisException("Cannot create scope: node is not part of a CompilationUnit");
    }
    Scope scope = scopeForAstNode(parent);
    if (node instanceof ClassDeclaration) {
      ClassElement element = ((ClassDeclaration) node).getElement();
      if (element == null) {
        throw new AnalysisException("Cannot build a scope for an unresolved class");
      }
      scope = new ClassScope(new TypeParameterScope(scope, element), element);
    } else if (node instanceof ClassTypeAlias) {
      ClassElement element = ((ClassTypeAlias) node).getElement();
      if (element == null) {
        throw new AnalysisException("Cannot build a scope for an unresolved class type alias");
      }
      scope = new ClassScope(new TypeParameterScope(scope, element), element);
    } else if (node instanceof ConstructorDeclaration) {
      ConstructorElement element = ((ConstructorDeclaration) node).getElement();
      if (element == null) {
        throw new AnalysisException("Cannot build a scope for an unresolved constructor");
      }
      FunctionScope functionScope = new FunctionScope(scope, element);
      functionScope.defineParameters();
      scope = functionScope;
    } else if (node instanceof FunctionDeclaration) {
      ExecutableElement element = ((FunctionDeclaration) node).getElement();
      if (element == null) {
        throw new AnalysisException("Cannot build a scope for an unresolved function");
      }
      FunctionScope functionScope = new FunctionScope(scope, element);
      functionScope.defineParameters();
      scope = functionScope;
    } else if (node instanceof FunctionTypeAlias) {
      scope = new FunctionTypeScope(scope, ((FunctionTypeAlias) node).getElement());
    } else if (node instanceof MethodDeclaration) {
      ExecutableElement element = ((MethodDeclaration) node).getElement();
      if (element == null) {
        throw new AnalysisException("Cannot build a scope for an unresolved method");
      }
      FunctionScope functionScope = new FunctionScope(scope, element);
      functionScope.defineParameters();
      scope = functionScope;
    }
    return scope;
  }

  private Scope scopeForCompilationUnit(CompilationUnit node) throws AnalysisException {
    CompilationUnitElement unitElement = node.getElement();
    if (unitElement == null) {
      throw new AnalysisException("Cannot create scope: compilation unit is not resolved");
    }
    LibraryElement libraryElement = unitElement.getLibrary();
    if (libraryElement == null) {
      throw new AnalysisException("Cannot create scope: compilation unit is not part of a library");
    }
    return new LibraryScope(libraryElement, errorListener);
  }
}
