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
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.internal.scope.ClassScope;
import com.google.dart.engine.internal.scope.EnclosedScope;
import com.google.dart.engine.internal.scope.FunctionScope;
import com.google.dart.engine.internal.scope.FunctionTypeScope;
import com.google.dart.engine.internal.scope.LabelScope;
import com.google.dart.engine.internal.scope.LibraryScope;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;

/**
 * The abstract class {@code ScopedVisitor} maintains name and label scopes as an AST structure is
 * being visited.
 * 
 * @coverage dart.engine.resolver
 */
public abstract class ScopedVisitor extends GeneralizingASTVisitor<Void> {
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
   * The scope used to resolve labels for {@code break} and {@code continue} statements, or
   * {@code null} if no labels have been defined in the current context.
   */
  private LabelScope labelScope;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ScopedVisitor(Library library, Source source, TypeProvider typeProvider) {
    this.definingLibrary = library.getLibraryElement();
    this.source = source;
    LibraryScope libraryScope = library.getLibraryScope();
    this.errorListener = libraryScope.getErrorListener();
    this.nameScope = libraryScope;
    this.typeProvider = typeProvider;
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param definingLibrary the element for the library containing the compilation unit being
   *          visited
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   */
  public ScopedVisitor(LibraryElement definingLibrary, Source source, TypeProvider typeProvider,
      AnalysisErrorListener errorListener) {
    this.definingLibrary = definingLibrary;
    this.source = source;
    this.errorListener = errorListener;
    this.nameScope = new LibraryScope(definingLibrary, errorListener);
    this.typeProvider = typeProvider;
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
  public Void visitCatchClause(CatchClause node) {
    SimpleIdentifier exception = node.getExceptionParameter();
    if (exception != null) {
      Scope outerScope = nameScope;
      nameScope = new EnclosedScope(nameScope);
      try {
        nameScope.define(exception.getElement());
        SimpleIdentifier stackTrace = node.getStackTraceParameter();
        if (stackTrace != null) {
          nameScope.define(stackTrace.getElement());
        }
        super.visitCatchClause(node);
      } finally {
        nameScope = outerScope;
      }
    }
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    Scope outerScope = nameScope;
    try {
      nameScope = new ClassScope(nameScope, node.getElement());
      super.visitClassDeclaration(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    Scope outerScope = nameScope;
    try {
      nameScope = new ClassScope(nameScope, node.getElement());
      super.visitClassTypeAlias(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    Scope outerScope = nameScope;
    try {
      nameScope = new FunctionScope(nameScope, node.getElement());
      super.visitConstructorDeclaration(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    VariableElement element = node.getElement();
    if (element != null) {
      nameScope.define(element);
    }
    super.visitDeclaredIdentifier(node);
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
    LabelScope outerLabelScope = labelScope;
    labelScope = new LabelScope(outerLabelScope, false, false);
    Scope outerNameScope = nameScope;
    nameScope = new EnclosedScope(nameScope);
    try {
      super.visitForEachStatement(node);
    } finally {
      nameScope = outerNameScope;
      labelScope = outerLabelScope;
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    LabelScope outerLabelScope = labelScope;
    labelScope = new LabelScope(outerLabelScope, false, false);
    Scope outerNameScope = nameScope;
    nameScope = new EnclosedScope(nameScope);
    try {
      super.visitForStatement(node);
    } finally {
      nameScope = outerNameScope;
      labelScope = outerLabelScope;
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement function = node.getElement();
    Scope outerScope = nameScope;
    try {
      nameScope = new FunctionScope(nameScope, function);
      super.visitFunctionDeclaration(node);
    } finally {
      nameScope = outerScope;
    }
    if (!(function.getEnclosingElement() instanceof CompilationUnitElement)) {
      nameScope.define(function);
    }
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    Scope outerScope = nameScope;
    try {
      ExecutableElement functionElement = node.getElement();
      if (functionElement == null) {
        // TODO(brianwilkerson) Report this internal error
      } else {
        nameScope = new FunctionScope(nameScope, functionElement);
      }
      super.visitFunctionExpression(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    Scope outerScope = nameScope;
    try {
      nameScope = new FunctionTypeScope(nameScope, node.getElement());
      super.visitFunctionTypeAlias(node);
    } finally {
      nameScope = outerScope;
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
    Scope outerScope = nameScope;
    try {
      nameScope = new FunctionScope(nameScope, node.getElement());
      super.visitMethodDeclaration(node);
    } finally {
      nameScope = outerScope;
    }
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
  public Void visitVariableDeclaration(VariableDeclaration node) {
    if (!(node.getParent().getParent() instanceof TopLevelVariableDeclaration)
        && !(node.getParent().getParent() instanceof FieldDeclaration)) {
      VariableElement element = node.getElement();
      if (element != null) {
        nameScope.define(element);
      }
    }
    super.visitVariableDeclaration(node);
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
  protected void reportError(ErrorCode errorCode, ASTNode node, Object... arguments) {
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
  protected void reportError(ErrorCode errorCode, Token token, Object... arguments) {
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
