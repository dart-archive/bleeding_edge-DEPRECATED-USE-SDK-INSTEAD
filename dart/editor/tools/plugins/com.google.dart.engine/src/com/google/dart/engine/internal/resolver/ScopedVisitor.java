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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionDeclarationStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.UnifyingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
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
import com.google.dart.engine.internal.scope.TypeParameterScope;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;

/**
 * The abstract class {@code ScopedVisitor} maintains name and label scopes as an AST structure is
 * being visited.
 * 
 * @coverage dart.engine.resolver
 */
public abstract class ScopedVisitor extends UnifyingAstVisitor<Void> {
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
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param definingLibrary the element for the library containing the compilation unit being
   *          visited
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   * @param nameScope the scope used to resolve identifiers in the node that will first be visited
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   */
  public ScopedVisitor(LibraryElement definingLibrary, Source source, TypeProvider typeProvider,
      Scope nameScope, AnalysisErrorListener errorListener) {
    this.definingLibrary = definingLibrary;
    this.source = source;
    this.errorListener = errorListener;
    this.nameScope = nameScope;
    this.typeProvider = typeProvider;
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ScopedVisitor(ResolvableLibrary library, Source source, TypeProvider typeProvider) {
    this.definingLibrary = library.getLibraryElement();
    this.source = source;
    LibraryScope libraryScope = library.getLibraryScope();
    this.errorListener = libraryScope.getErrorListener();
    this.nameScope = libraryScope;
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

  /**
   * Replaces the current {@link Scope} with the enclosing {@link Scope}.
   * 
   * @return the enclosing {@link Scope}.
   */
  public Scope popNameScope() {
    nameScope = nameScope.getEnclosingScope();
    return nameScope;
  }

  /**
   * Pushes a new {@link Scope} into the visitor.
   * 
   * @return the new {@link Scope}.
   */
  public Scope pushNameScope() {
    Scope newScope = new EnclosedScope(nameScope);
    nameScope = newScope;
    return nameScope;
  }

  @Override
  public Void visitBlock(Block node) {
    Scope outerScope = nameScope;
    try {
      EnclosedScope enclosedScope = new EnclosedScope(nameScope);
      hideNamesDefinedInBlock(enclosedScope, node);
      nameScope = enclosedScope;
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
      try {
        nameScope = new EnclosedScope(nameScope);
        nameScope.define(exception.getStaticElement());
        SimpleIdentifier stackTrace = node.getStackTraceParameter();
        if (stackTrace != null) {
          nameScope.define(stackTrace.getStaticElement());
        }
        super.visitCatchClause(node);
      } finally {
        nameScope = outerScope;
      }
    } else {
      super.visitCatchClause(node);
    }
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement classElement = node.getElement();
    Scope outerScope = nameScope;
    try {
      if (classElement == null) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Missing element for class declaration " + node.getName().getName() + " in "
                + getDefiningLibrary().getSource().getFullName(),
            new Exception());
        super.visitClassDeclaration(node);
      } else {
        nameScope = new TypeParameterScope(nameScope, classElement);
        visitClassDeclarationInScope(node);
        nameScope = new ClassScope(nameScope, classElement);
        visitClassMembersInScope(node);
      }
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    Scope outerScope = nameScope;
    try {
      ClassElement element = node.getElement();
      nameScope = new ClassScope(new TypeParameterScope(nameScope, element), element);
      super.visitClassTypeAlias(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorElement constructorElement = node.getElement();
    Scope outerScope = nameScope;
    try {
      if (constructorElement == null) {
        StringBuilder builder = new StringBuilder();
        builder.append("Missing element for constructor ");
        builder.append(node.getReturnType().getName());
        if (node.getName() != null) {
          builder.append(".");
          builder.append(node.getName().getName());
        }
        builder.append(" in ");
        builder.append(getDefiningLibrary().getSource().getFullName());
        AnalysisEngine.getInstance().getLogger().logInformation(builder.toString(), new Exception());
      } else {
        nameScope = new FunctionScope(nameScope, constructorElement);
      }
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
    LabelScope outerLabelScope = labelScope;
    try {
      labelScope = new LabelScope(labelScope, false, false);
      visitStatementInScope(node.getBody());
      safelyVisit(node.getCondition());
    } finally {
      labelScope = outerLabelScope;
    }
    return null;
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    Scope outerNameScope = nameScope;
    LabelScope outerLabelScope = labelScope;
    try {
      nameScope = new EnclosedScope(nameScope);
      labelScope = new LabelScope(outerLabelScope, false, false);
      visitForEachStatementInScope(node);
    } finally {
      labelScope = outerLabelScope;
      nameScope = outerNameScope;
    }
    return null;
  }

  @Override
  public Void visitFormalParameterList(FormalParameterList node) {
    super.visitFormalParameterList(node);
    // We finished resolving function signature, now include formal parameters scope.
    if (nameScope instanceof FunctionScope) {
      ((FunctionScope) nameScope).defineParameters();
    }
    if (nameScope instanceof FunctionTypeScope) {
      ((FunctionTypeScope) nameScope).defineParameters();
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    Scope outerNameScope = nameScope;
    LabelScope outerLabelScope = labelScope;
    try {
      nameScope = new EnclosedScope(nameScope);
      labelScope = new LabelScope(outerLabelScope, false, false);
      visitForStatementInScope(node);
    } finally {
      labelScope = outerLabelScope;
      nameScope = outerNameScope;
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement functionElement = node.getElement();
    Scope outerScope = nameScope;
    try {
      if (functionElement == null) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Missing element for top-level function " + node.getName().getName() + " in "
                + getDefiningLibrary().getSource().getFullName(),
            new Exception());
      } else {
        nameScope = new FunctionScope(nameScope, functionElement);
      }
      super.visitFunctionDeclaration(node);
    } finally {
      nameScope = outerScope;
    }
    if (functionElement != null
        && !(functionElement.getEnclosingElement() instanceof CompilationUnitElement)) {
      nameScope.define(functionElement);
    }
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    if (node.getParent() instanceof FunctionDeclaration) {
      // We have already created a function scope and don't need to do so again.
      super.visitFunctionExpression(node);
    } else {
      Scope outerScope = nameScope;
      try {
        ExecutableElement functionElement = node.getElement();
        if (functionElement == null) {
          StringBuilder builder = new StringBuilder();
          builder.append("Missing element for function ");
          AstNode parent = node.getParent();
          while (parent != null) {
            if (parent instanceof Declaration) {
              Element parentElement = ((Declaration) parent).getElement();
              builder.append(parentElement == null ? "<unknown> " : (parentElement.getName() + " "));
            }
            parent = parent.getParent();
          }
          builder.append("in ");
          builder.append(getDefiningLibrary().getSource().getFullName());
          AnalysisEngine.getInstance().getLogger().logInformation(
              builder.toString(),
              new Exception());
        } else {
          nameScope = new FunctionScope(nameScope, functionElement);
        }
        super.visitFunctionExpression(node);
      } finally {
        nameScope = outerScope;
      }
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
  public Void visitIfStatement(IfStatement node) {
    safelyVisit(node.getCondition());
    visitStatementInScope(node.getThenStatement());
    visitStatementInScope(node.getElseStatement());
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
      ExecutableElement methodElement = node.getElement();
      if (methodElement == null) {
        AnalysisEngine.getInstance().getLogger().logInformation(
            "Missing element for method " + node.getName().getName() + " in "
                + getDefiningLibrary().getSource().getFullName(),
            new Exception());
      } else {
        nameScope = new FunctionScope(nameScope, methodElement);
      }
      super.visitMethodDeclaration(node);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    node.getExpression().accept(this);
    Scope outerNameScope = nameScope;
    try {
      nameScope = new EnclosedScope(nameScope);
      node.getStatements().accept(this);
    } finally {
      nameScope = outerNameScope;
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    Scope outerNameScope = nameScope;
    try {
      nameScope = new EnclosedScope(nameScope);
      node.getStatements().accept(this);
    } finally {
      nameScope = outerNameScope;
    }
    return null;
  }

  @Override
  public Void visitSwitchStatement(SwitchStatement node) {
    LabelScope outerScope = labelScope;
    try {
      labelScope = new LabelScope(outerScope, true, false);
      for (SwitchMember member : node.getMembers()) {
        for (Label label : member.getLabels()) {
          SimpleIdentifier labelName = label.getLabel();
          LabelElement labelElement = (LabelElement) labelName.getStaticElement();
          labelScope = new LabelScope(labelScope, labelName.getName(), labelElement);
        }
      }
      super.visitSwitchStatement(node);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    if (!(node.getParent().getParent() instanceof TopLevelVariableDeclaration)
        && !(node.getParent().getParent() instanceof FieldDeclaration)) {
      VariableElement element = node.getElement();
      if (element != null) {
        nameScope.define(element);
      }
    }
    return null;
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    LabelScope outerScope = labelScope;
    try {
      labelScope = new LabelScope(outerScope, false, false);
      safelyVisit(node.getCondition());
      visitStatementInScope(node.getBody());
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
   * Return the source.
   * 
   * @return the source
   */
  protected Source getSource() {
    return source;
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param node the node specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  protected void reportErrorForNode(ErrorCode errorCode, AstNode node, Object... arguments) {
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
   * @param offset the offset of the location of the error
   * @param length the length of the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  protected void reportErrorForOffset(ErrorCode errorCode, int offset, int length,
      Object... arguments) {
    errorListener.onError(new AnalysisError(source, offset, length, errorCode, arguments));
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param token the token specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  protected void reportErrorForToken(ErrorCode errorCode, Token token, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        token.getOffset(),
        token.getLength(),
        errorCode,
        arguments));
  }

  /**
   * Visit the given AST node if it is not null.
   * 
   * @param node the node to be visited
   */
  protected void safelyVisit(AstNode node) {
    if (node != null) {
      node.accept(this);
    }
  }

  protected void visitClassDeclarationInScope(ClassDeclaration node) {
    safelyVisit(node.getName());
    safelyVisit(node.getTypeParameters());
    safelyVisit(node.getExtendsClause());
    safelyVisit(node.getWithClause());
    safelyVisit(node.getImplementsClause());
    safelyVisit(node.getNativeClause());
  }

  protected void visitClassMembersInScope(ClassDeclaration node) {
    safelyVisit(node.getDocumentationComment());
    node.getMetadata().accept(this);
    node.getMembers().accept(this);
  }

  /**
   * Visit the given statement after it's scope has been created. This replaces the normal call to
   * the inherited visit method so that ResolverVisitor can intervene when type propagation is
   * enabled.
   * 
   * @param node the statement to be visited
   */
  protected void visitForEachStatementInScope(ForEachStatement node) {
    //
    // We visit the iterator before the loop variable because the loop variable cannot be in scope
    // while visiting the iterator.
    //
    safelyVisit(node.getIdentifier());
    safelyVisit(node.getIterator());
    safelyVisit(node.getLoopVariable());
    visitStatementInScope(node.getBody());
  }

  /**
   * Visit the given statement after it's scope has been created. This replaces the normal call to
   * the inherited visit method so that ResolverVisitor can intervene when type propagation is
   * enabled.
   * 
   * @param node the statement to be visited
   */
  protected void visitForStatementInScope(ForStatement node) {
    safelyVisit(node.getVariables());
    safelyVisit(node.getInitialization());
    safelyVisit(node.getCondition());
    node.getUpdaters().accept(this);
    visitStatementInScope(node.getBody());
  }

  /**
   * Visit the given statement after it's scope has been created. This is used by ResolverVisitor to
   * correctly visit the 'then' and 'else' statements of an 'if' statement.
   * 
   * @param node the statement to be visited
   */
  protected void visitStatementInScope(Statement node) {
    if (node instanceof Block) {
      // Don't create a scope around a block because the block will create it's own scope.
      visitBlock((Block) node);
    } else if (node != null) {
      Scope outerNameScope = nameScope;
      try {
        nameScope = new EnclosedScope(nameScope);
        node.accept(this);
      } finally {
        nameScope = outerNameScope;
      }
    }
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
      LabelElement labelElement = (LabelElement) labelNameNode.getStaticElement();
      labelScope = new LabelScope(labelScope, labelName, labelElement);
    }
    return outerScope;
  }

  /**
   * Marks the local declarations of the given {@link Block} hidden in the enclosing scope.
   * According to the scoping rules name is hidden if block defines it, but name is defined after
   * its declaration statement.
   */
  private void hideNamesDefinedInBlock(EnclosedScope scope, Block block) {
    NodeList<Statement> statements = block.getStatements();
    int statementCount = statements.size();
    for (int i = 0; i < statementCount; i++) {
      Statement statement = statements.get(i);
      if (statement instanceof VariableDeclarationStatement) {
        VariableDeclarationStatement vds = (VariableDeclarationStatement) statement;
        NodeList<VariableDeclaration> variables = vds.getVariables().getVariables();
        int variableCount = variables.size();
        for (int j = 0; j < variableCount; j++) {
          scope.hide(variables.get(j).getElement());
        }
      } else if (statement instanceof FunctionDeclarationStatement) {
        FunctionDeclarationStatement fds = (FunctionDeclarationStatement) statement;
        scope.hide(fds.getFunctionDeclaration().getElement());
      }
    }
  }
}
