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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.resolver.scope.ClassScope;
import com.google.dart.engine.resolver.scope.EnclosedScope;
import com.google.dart.engine.resolver.scope.FunctionScope;
import com.google.dart.engine.resolver.scope.LabelScope;
import com.google.dart.engine.resolver.scope.LibraryScope;
import com.google.dart.engine.resolver.scope.Scope;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code ResolverVisitor} are used to resolve the nodes within a single
 * compilation unit.
 */
public class ResolverVisitor extends GeneralizingASTVisitor<Void> {
  /**
   * The element for the library containing the compilation unit being visited.
   */
  private LibraryElement definingLibrary;

  /**
   * The source representing the compilation unit being visited.
   */
  private Source source;

  /**
   * The error listener that will be informed of any errors that are found during resolution.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The scope used to resolve identifiers.
   */
  private Scope nameScope;

  /**
   * The object used to access the types from the core library.
   */
  private TypeProvider typeProvider;

  /**
   * The object used to resolve the element associated with the current node.
   */
  private ElementResolver elementResolver;

  /**
   * The object used to compute the type associated with the current node.
   */
  private StaticTypeAnalyzer typeAnalyzer;

  /**
   * The scope used to resolve labels for {@code break} and {@code continue} statements, or
   * {@code null} if no labels have been defined in the current context.
   */
  private LabelScope labelScope;

  /**
   * The type element representing the type most recently being visited.
   */
  private ClassElement enclosingType = null;

  /**
   * The executable element representing the method or function most recently being visited.
   */
  private ExecutableElement enclosingFunction = null;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    this.definingLibrary = library.getLibraryElement();
    this.source = source;
    LibraryScope libraryScope = library.getLibraryScope();
    this.errorListener = libraryScope.getErrorListener();
    this.nameScope = libraryScope;
    this.typeProvider = typeProvider;
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
  }

  /**
   * Return the library element for the library containing the compilation unit being resolved.
   * 
   * @return the library element for the library containing the compilation unit being resolved
   */
  public LibraryElement getDefiningLibrary() {
    return definingLibrary;
  }

  /**
   * Return the object used to access the types from the core library.
   * 
   * @return the object used to access the types from the core library
   */
  public TypeProvider getTypeProvider() {
    return typeProvider;
  }

  @Override
  public Void visitBlock(Block node) {
    Scope outerScope = nameScope;
    nameScope = new EnclosedScope(nameScope);
    try {
      super.visitBlock(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement outerType = enclosingType;
    Scope outerScope = nameScope;
    try {
      enclosingType = node.getElement();
      nameScope = new ClassScope(nameScope, enclosingType);
      typeAnalyzer.setThisType(enclosingType == null ? null : enclosingType.getType());
      super.visitClassDeclaration(node);
    } finally {
      typeAnalyzer.setThisType(outerType == null ? null : outerType.getType());
      nameScope = outerScope;
      enclosingType = outerType;
    }
    return null;
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    LabelScope outerScope = labelScope;
    labelScope = new LabelScope(outerScope, false, false);
    try {
      super.visitDoStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    LabelScope outerScope = labelScope;
    labelScope = new LabelScope(outerScope, false, false);
    try {
      super.visitForEachStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    LabelScope outerScope = labelScope;
    labelScope = new LabelScope(outerScope, false, false);
    try {
      super.visitForStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      SimpleIdentifier functionName = node.getName();
      enclosingFunction = (ExecutableElement) functionName.getElement();
      super.visitFunctionDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = (ExecutableElement) node.getElement();
      super.visitFunctionExpression(node);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    LabelScope outerScope = addScopesFor(node.getLabels());
    try {
      super.visitLabeledStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    Element element = node.getName().getElement();
    if (!(element instanceof ExecutableElement)) {
      // Internal error.
    }
    ExecutableElement outerFunction = enclosingFunction;
    Scope outerScope = nameScope;
    try {
      enclosingFunction = (ExecutableElement) element;
      nameScope = new FunctionScope(nameScope, enclosingFunction);
      super.visitMethodDeclaration(node);
    } finally {
      nameScope = outerScope;
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitNode(ASTNode node) {
    node.visitChildren(this);
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    node.getExpression().accept(this);
    LabelScope outerLabelScope = addScopesFor(node.getLabels());
    Scope outerNameScope = nameScope;
    nameScope = new EnclosedScope(nameScope);
    try {
      node.getStatements().accept(this);
    } finally {
      nameScope = outerNameScope;
      labelScope = outerLabelScope;
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    LabelScope outerLabelScope = addScopesFor(node.getLabels());
    Scope outerNameScope = nameScope;
    nameScope = new EnclosedScope(nameScope);
    try {
      node.getStatements().accept(this);
    } finally {
      nameScope = outerNameScope;
      labelScope = outerLabelScope;
    }
    return null;
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    LabelScope outerScope = labelScope;
    labelScope = new LabelScope(outerScope, true, false);
    for (SwitchMember member : node.getMembers()) {
      for (Label label : member.getLabels()) {
        SimpleIdentifier labelName = label.getLabel();
        LabelElement labelElement = (LabelElement) labelName.getElement();
        labelScope = new LabelScope(outerScope, labelName.getName(), labelElement);
      }
    }
    try {
      super.visitSwitchStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    LabelScope outerScope = labelScope;
    labelScope = new LabelScope(outerScope, false, false);
    try {
      super.visitWhileStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  /**
   * Return the element representing the function containing the current node, or {@code null} if
   * the current node is not contained in a function.
   * 
   * @return the element representing the function containing the current node
   */
  protected ExecutableElement getEnclosingFunction() {
    return enclosingFunction;
  }

  /**
   * Return the label scope in which the current node is being resolved.
   * 
   * @return the label scope in which the current node is being resolved
   */
  protected LabelScope getLabelScope() {
    return labelScope;
  }

  /**
   * Return the name scope in which the current node is being resolved.
   * 
   * @return the name scope in which the current node is being resolved
   */
  protected Scope getNameScope() {
    return nameScope;
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param node the node specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  protected void reportError(ResolverErrorCode errorCode, ASTNode node, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        node.getOffset(),
        node.getLength(),
        errorCode,
        arguments));
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param token the token specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  protected void reportError(ResolverErrorCode errorCode, Token token, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        token.getOffset(),
        token.getLength(),
        errorCode,
        arguments));
  }

  /**
   * Add scopes for each of the given labels.
   * 
   * @param labels the labels for which new scopes are to be added
   * @return the scope that was in effect before the new scopes were added
   */
  private LabelScope addScopesFor(NodeList<Label> labels) {
    LabelScope outerScope = labelScope;
    for (Label label : labels) {
      SimpleIdentifier labelNameNode = label.getLabel();
      String labelName = labelNameNode.getName();
      LabelElement labelElement = (LabelElement) labelNameNode.getElement();
      labelScope = new LabelScope(labelScope, labelName, labelElement);
    }
    return outerScope;
  }
}
