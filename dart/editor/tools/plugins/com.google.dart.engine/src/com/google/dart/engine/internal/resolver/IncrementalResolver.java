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
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.internal.scope.ScopeBuilder;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code IncrementalResolver} resolve the smallest portion of an AST
 * structure that we currently know how to resolve.
 */
public class IncrementalResolver {
  /**
   * The element for the library containing the compilation unit being visited.
   */
  private LibraryElement definingLibrary;

  /**
   * The source representing the compilation unit being visited.
   */
  private Source source;

  /**
   * The object used to access the types from the core library.
   */
  private TypeProvider typeProvider;

  /**
   * The error listener that will be informed of any errors that are found during resolution.
   */
  private AnalysisErrorListener errorListener;

  /**
   * Initialize a newly created incremental resolver to resolve a node in the given source in the
   * given library, reporting errors to the given error listener.
   * 
   * @param definingLibrary the element for the library containing the compilation unit being
   *          visited
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   */
  public IncrementalResolver(LibraryElement definingLibrary, Source source,
      TypeProvider typeProvider, AnalysisErrorListener errorListener) {
    this.definingLibrary = definingLibrary;
    this.source = source;
    this.typeProvider = typeProvider;
    this.errorListener = errorListener;
  }

  /**
   * Resolve the given node, reporting any errors or warnings to the given listener.
   * 
   * @param node the root of the AST structure to be resolved
   * @throws AnalysisException if the node could not be resolved
   */
  public void resolve(AstNode node) throws AnalysisException {
    AstNode rootNode = findResolutionRoot(node);
    Scope scope = ScopeBuilder.scopeFor(rootNode, errorListener);
    if (elementModelChanged(rootNode.getParent())) {
      throw new AnalysisException("Cannot resolve node: element model changed");
    }
    resolveTypes(node, scope);
    resolveVariables(node, scope);
    resolveReferences(node, scope);
  }

  /**
   * Return {@code true} if the given node can be resolved independently of any other nodes.
   * <p>
   * <b>Note:</b> This method needs to be kept in sync with {@link ScopeBuilder#scopeForAstNode}.
   * 
   * @param node the node being tested
   * @return {@code true} if the given node can be resolved independently of any other nodes
   */
  private boolean canBeResolved(AstNode node) {
    return node instanceof ClassDeclaration || node instanceof ClassTypeAlias
        || node instanceof CompilationUnit || node instanceof ConstructorDeclaration
        || node instanceof FunctionDeclaration || node instanceof FunctionTypeAlias
        || node instanceof MethodDeclaration;
  }

  /**
   * Return {@code true} if the portion of the element model defined by the given node has changed.
   * 
   * @param node the node defining the portion of the element model being tested
   * @return {@code true} if the element model defined by the given node has changed
   * @throws AnalysisException if the correctness of the element model cannot be determined
   */
  private boolean elementModelChanged(AstNode node) throws AnalysisException {
    Element element = getElement(node);
    if (element == null) {
      throw new AnalysisException("Cannot resolve node: a " + node.getClass().getSimpleName()
          + " does not define an element");
    }
    DeclarationMatcher matcher = new DeclarationMatcher();
    return !matcher.matches(node, element);
  }

  /**
   * Starting at the given node, find the smallest AST node that can be resolved independently of
   * any other nodes. Return the node that was found.
   * 
   * @param node the node at which the search is to begin
   * @return the smallest AST node that can be resolved independently of any other nodes
   * @throws AnalysisException if there is no such node
   */
  private AstNode findResolutionRoot(AstNode node) throws AnalysisException {
    AstNode result = node;
    AstNode parent = result.getParent();
    while (parent != null && !canBeResolved(parent)) {
      result = parent;
      parent = result.getParent();
    }
    if (parent == null) {
      throw new AnalysisException("Cannot resolve node: no resolvable node");
    }
    return result;
  }

  /**
   * Return the element defined by the given node, or {@code null} if the node does not define an
   * element.
   * 
   * @param node the node defining the element to be returned
   * @return the element defined by the given node
   */
  private Element getElement(AstNode node) {
    if (node instanceof Declaration) {
      return ((Declaration) node).getElement();
    } else if (node instanceof CompilationUnit) {
      return ((CompilationUnit) node).getElement();
    }
    return null;
  }

  private void resolveReferences(AstNode node, Scope scope) {
    ResolverVisitor visitor = new ResolverVisitor(
        definingLibrary,
        source,
        typeProvider,
        scope,
        errorListener);
    node.accept(visitor);
  }

  private void resolveTypes(AstNode node, Scope scope) {
    TypeResolverVisitor visitor = new TypeResolverVisitor(
        definingLibrary,
        source,
        typeProvider,
        scope,
        errorListener);
    node.accept(visitor);
  }

  private void resolveVariables(AstNode node, Scope scope) {
    VariableResolverVisitor visitor = new VariableResolverVisitor(
        definingLibrary,
        source,
        typeProvider,
        scope,
        errorListener);
    node.accept(visitor);
  }
}
