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

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.RethrowExpression;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.ShowCombinator;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.ThrowExpression;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.PropertyInducingElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.HashMap;

/**
 * Instances of the class {@code ResolverVisitor} are used to resolve the nodes within a single
 * compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class ResolverVisitor extends ScopedVisitor {
  /**
   * The manager for the inheritance mappings.
   */
  private final InheritanceManager inheritanceManager;

  /**
   * The object used to resolve the element associated with the current node.
   */
  private ElementResolver elementResolver;

  /**
   * The object used to compute the type associated with the current node.
   */
  private StaticTypeAnalyzer typeAnalyzer;

  /**
   * The class element representing the class containing the current node, or {@code null} if the
   * current node is not contained in a class.
   */
  private ClassElement enclosingClass = null;

  /**
   * The class declaration representing the class containing the current node, or {@code null} if
   * the current node is not contained in a class.
   */
  private ClassDeclaration enclosingClassDeclaration = null;

  /**
   * The function type alias representing the function type containing the current node, or
   * {@code null} if the current node is not contained in a function type alias.
   */
  private FunctionTypeAlias enclosingFunctionTypeAlias = null;

  /**
   * The element representing the function containing the current node, or {@code null} if the
   * current node is not contained in a function.
   */
  private ExecutableElement enclosingFunction = null;

  /**
   * The {@link Comment} before a {@link FunctionDeclaration} or a {@link MethodDeclaration} that
   * cannot be resolved where we visited it, because it should be resolved in the scope of the body.
   */
  private Comment commentBeforeFunction = null;

  /**
   * The object keeping track of which elements have had their types overridden.
   */
  private TypeOverrideManager overrideManager = new TypeOverrideManager();

  /**
   * The object keeping track of which elements have had their types promoted.
   */
  private TypePromotionManager promoteManager = new TypePromotionManager();

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
    this.inheritanceManager = library.getInheritanceManager();
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
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
  public ResolverVisitor(LibraryElement definingLibrary, Source source, TypeProvider typeProvider,
      InheritanceManager inheritanceManager, AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, errorListener);
    this.inheritanceManager = inheritanceManager;
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
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
  public ResolverVisitor(LibraryElement definingLibrary, Source source, TypeProvider typeProvider,
      Scope nameScope, AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, nameScope, errorListener);
    this.inheritanceManager = new InheritanceManager(definingLibrary);
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ResolverVisitor(ResolvableLibrary library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
    this.inheritanceManager = library.getInheritanceManager();
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
  }

  /**
   * Return the object keeping track of which elements have had their types overridden.
   * 
   * @return the object keeping track of which elements have had their types overridden
   */
  public TypeOverrideManager getOverrideManager() {
    return overrideManager;
  }

  /**
   * Return the object keeping track of which elements have had their types promoted.
   * 
   * @return the object keeping track of which elements have had their types promoted
   */
  public TypePromotionManager getPromoteManager() {
    return promoteManager;
  }

  @Override
  public Void visitAnnotation(Annotation node) {
    AstNode parent = node.getParent();
    if (parent == enclosingClassDeclaration || parent == enclosingFunctionTypeAlias) {
      return null;
    }
    return super.visitAnnotation(node);
  }

  @Override
  public Void visitAsExpression(AsExpression node) {
    super.visitAsExpression(node);
    overrideExpression(node.getExpression(), node.getType().getType());
    return null;
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    super.visitAssertStatement(node);
    propagateTrueState(node.getCondition());
    return null;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    TokenType operatorType = node.getOperator().getType();
    Expression leftOperand = node.getLeftOperand();
    Expression rightOperand = node.getRightOperand();
    if (operatorType == TokenType.AMPERSAND_AMPERSAND) {
      safelyVisit(leftOperand);
      if (rightOperand != null) {
        overrideManager.enterScope();
        try {
          promoteManager.enterScope();
          try {
            propagateTrueState(leftOperand);
            // Type promotion.
            promoteTypes(leftOperand);
            clearTypePromotionsIfPotentiallyMutatedIn(leftOperand);
            clearTypePromotionsIfPotentiallyMutatedIn(rightOperand);
            clearTypePromotionsIfAccessedInClosureAndProtentiallyMutated(rightOperand);
            // Visit right operand.
            rightOperand.accept(this);
          } finally {
            promoteManager.exitScope();
          }
        } finally {
          overrideManager.exitScope();
        }
      }
    } else if (operatorType == TokenType.BAR_BAR) {
      safelyVisit(leftOperand);
      if (rightOperand != null) {
        overrideManager.enterScope();
        try {
          propagateFalseState(leftOperand);
          rightOperand.accept(this);
        } finally {
          overrideManager.exitScope();
        }
      }
    } else {
      safelyVisit(leftOperand);
      safelyVisit(rightOperand);
    }
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitBlockFunctionBody(BlockFunctionBody node) {
    safelyVisit(commentBeforeFunction);
    overrideManager.enterScope();
    try {
      super.visitBlockFunctionBody(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    //
    // We do not visit the label because it needs to be visited in the context of the statement.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    //
    // Resolve the metadata in the library scope.
    //
    if (node.getMetadata() != null) {
      node.getMetadata().accept(this);
    }
    enclosingClassDeclaration = node;
    //
    // Continue the class resolution.
    //
    ClassElement outerType = enclosingClass;
    try {
      enclosingClass = node.getElement();
      typeAnalyzer.setThisType(enclosingClass == null ? null : enclosingClass.getType());
      super.visitClassDeclaration(node);
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
    } finally {
      typeAnalyzer.setThisType(outerType == null ? null : outerType.getType());
      enclosingClass = outerType;
      enclosingClassDeclaration = null;
    }
    return null;
  }

  @Override
  public Void visitComment(Comment node) {
    if (node.getParent() instanceof FunctionDeclaration
        || node.getParent() instanceof ConstructorDeclaration
        || node.getParent() instanceof MethodDeclaration) {
      if (node != commentBeforeFunction) {
        commentBeforeFunction = node;
        return null;
      }
    }
    super.visitComment(node);
    commentBeforeFunction = null;
    return null;
  }

  @Override
  public Void visitCommentReference(CommentReference node) {
    //
    // We do not visit the identifier because it needs to be visited in the context of the reference.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    //
    // TODO(brianwilkerson) The goal of the code below is to visit the declarations in such an
    // order that we can infer type information for top-level variables before we visit references
    // to them. This is better than making no effort, but still doesn't completely satisfy that
    // goal (consider for example "final var a = b; final var b = 0;"; we'll infer a type of 'int'
    // for 'b', but not for 'a' because of the order of the visits). Ideally we would create a
    // dependency graph, but that would require references to be resolved, which they are not.
    //
    overrideManager.enterScope();
    try {
      NodeList<Directive> directives = node.getDirectives();
      int directiveCount = directives.size();
      for (int i = 0; i < directiveCount; i++) {
        directives.get(i).accept(this);
      }
      NodeList<CompilationUnitMember> declarations = node.getDeclarations();
      int declarationCount = declarations.size();
      for (int i = 0; i < declarationCount; i++) {
        CompilationUnitMember declaration = declarations.get(i);
        if (!(declaration instanceof ClassDeclaration)) {
          declaration.accept(this);
        }
      }
      for (int i = 0; i < declarationCount; i++) {
        CompilationUnitMember declaration = declarations.get(i);
        if (declaration instanceof ClassDeclaration) {
          declaration.accept(this);
        }
      }
    } finally {
      overrideManager.exitScope();
    }
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    Expression condition = node.getCondition();
    safelyVisit(condition);
    Expression thenExpression = node.getThenExpression();
    if (thenExpression != null) {
      overrideManager.enterScope();
      try {
        promoteManager.enterScope();
        try {
          propagateTrueState(condition);
          // Type promotion.
          promoteTypes(condition);
          clearTypePromotionsIfPotentiallyMutatedIn(thenExpression);
          clearTypePromotionsIfAccessedInClosureAndProtentiallyMutated(thenExpression);
          // Visit "then" expression.
          thenExpression.accept(this);
        } finally {
          promoteManager.exitScope();
        }
      } finally {
        overrideManager.exitScope();
      }
    }
    Expression elseExpression = node.getElseExpression();
    if (elseExpression != null) {
      overrideManager.enterScope();
      try {
        propagateFalseState(condition);
        elseExpression.accept(this);
      } finally {
        overrideManager.exitScope();
      }
    }
    node.accept(elementResolver);
    node.accept(typeAnalyzer);

    boolean thenIsAbrupt = isAbruptTerminationExpression(thenExpression);
    boolean elseIsAbrupt = isAbruptTerminationExpression(elseExpression);
    if (elseIsAbrupt && !thenIsAbrupt) {
      propagateTrueState(condition);
      propagateState(thenExpression);
    } else if (thenIsAbrupt && !elseIsAbrupt) {
      propagateFalseState(condition);
      propagateState(elseExpression);
    }
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      super.visitConstructorDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    //
    // We visit the expression, but do not visit the field name because it needs to be visited in
    // the context of the constructor field initializer node.
    //
    safelyVisit(node.getExpression());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    //
    // We do not visit either the type name, because it won't be visited anyway, or the name,
    // because it needs to be visited in the context of the constructor name.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    //
    // We do not visit the label because it needs to be visited in the context of the statement.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    overrideManager.enterScope();
    try {
      super.visitDoStatement(node);
    } finally {
      overrideManager.exitScope();
    }
    // TODO(brianwilkerson) If the loop can only be exited because the condition is false, then
    // propagateFalseState(node.getCondition());
    return null;
  }

  @Override
  public Void visitEmptyFunctionBody(EmptyFunctionBody node) {
    safelyVisit(commentBeforeFunction);
    return super.visitEmptyFunctionBody(node);
  }

  @Override
  public Void visitEnumDeclaration(EnumDeclaration node) {
    //
    // Resolve the metadata in the library scope.
    //
    if (node.getMetadata() != null) {
      node.getMetadata().accept(this);
    }
    //
    // There is nothing else to do because everything else was resolved by the element builder.
    //
    return null;
  }

  @Override
  public Void visitExpressionFunctionBody(ExpressionFunctionBody node) {
    safelyVisit(commentBeforeFunction);
    overrideManager.enterScope();
    try {
      super.visitExpressionFunctionBody(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

//  @Override
//  public Void visitEmptyFunctionBody(EmptyFunctionBody node) {
//    overrideManager.enterScope();
//    try {
//      super.visitEmptyFunctionBody(node);
//    } finally {
//      overrideManager.exitScope();
//    }
//    return null;
//  }

  @Override
  public Void visitFieldDeclaration(FieldDeclaration node) {
    overrideManager.enterScope();
    try {
      super.visitFieldDeclaration(node);
    } finally {
      HashMap<Element, Type> overrides = overrideManager.captureOverrides(node.getFields());
      overrideManager.exitScope();
      overrideManager.applyOverrides(overrides);
    }
    return null;
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    overrideManager.enterScope();
    try {
      super.visitForEachStatement(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    overrideManager.enterScope();
    try {
      super.visitForStatement(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      SimpleIdentifier functionName = node.getName();
      enclosingFunction = (ExecutableElement) functionName.getStaticElement();
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
      enclosingFunction = node.getElement();
      overrideManager.enterScope();
      try {
        super.visitFunctionExpression(node);
      } finally {
        overrideManager.exitScope();
      }
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    safelyVisit(node.getFunction());
    node.accept(elementResolver);
    inferFunctionExpressionsParametersTypes(node.getArgumentList());
    safelyVisit(node.getArgumentList());
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    // Resolve the metadata in the library scope.
    if (node.getMetadata() != null) {
      node.getMetadata().accept(this);
    }
    FunctionTypeAlias outerAlias = enclosingFunctionTypeAlias;
    enclosingFunctionTypeAlias = node;
    try {
      super.visitFunctionTypeAlias(node);
    } finally {
      enclosingFunctionTypeAlias = outerAlias;
    }
    return null;
  }

  @Override
  public Void visitHideCombinator(HideCombinator node) {
    //
    // Combinators aren't visited by this visitor, the LibraryResolver has already resolved the
    // identifiers and there is no type analysis to be done.
    //
    return null;
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
    Expression condition = node.getCondition();
    safelyVisit(condition);
    HashMap<Element, Type> thenOverrides = null;
    Statement thenStatement = node.getThenStatement();
    if (thenStatement != null) {
      overrideManager.enterScope();
      try {
        promoteManager.enterScope();
        try {
          propagateTrueState(condition);
          // Type promotion.
          promoteTypes(condition);
          clearTypePromotionsIfPotentiallyMutatedIn(thenStatement);
          clearTypePromotionsIfAccessedInClosureAndProtentiallyMutated(thenStatement);
          // Visit "then".
          visitStatementInScope(thenStatement);
        } finally {
          promoteManager.exitScope();
        }
      } finally {
        thenOverrides = overrideManager.captureLocalOverrides();
        overrideManager.exitScope();
      }
    }
    HashMap<Element, Type> elseOverrides = null;
    Statement elseStatement = node.getElseStatement();
    if (elseStatement != null) {
      overrideManager.enterScope();
      try {
        propagateFalseState(condition);
        visitStatementInScope(elseStatement);
      } finally {
        elseOverrides = overrideManager.captureLocalOverrides();
        overrideManager.exitScope();
      }
    }
    node.accept(elementResolver);
    node.accept(typeAnalyzer);

    boolean thenIsAbrupt = isAbruptTerminationStatement(thenStatement);
    boolean elseIsAbrupt = isAbruptTerminationStatement(elseStatement);
    if (elseIsAbrupt && !thenIsAbrupt) {
      propagateTrueState(condition);
      if (thenOverrides != null) {
        overrideManager.applyOverrides(thenOverrides);
      }
    } else if (thenIsAbrupt && !elseIsAbrupt) {
      propagateFalseState(condition);
      if (elseOverrides != null) {
        overrideManager.applyOverrides(elseOverrides);
      }
    }
    // TODO(collinsn): union the [thenOverrides] and [elseOverrides] if both branches
    // are not abrupt. If both branches are abrupt, then we can mark the
    // remaining code as dead.
    return null;
  }

  @Override
  public Void visitLabel(Label node) {
    //
    // We don't visit labels or their children because they don't have a type. Instead, the element
    // resolver is responsible for resolving the labels in the context of their parent (either a
    // BreakStatement, ContinueStatement, or NamedExpression).
    //
    return null;
  }

  @Override
  public Void visitLibraryIdentifier(LibraryIdentifier node) {
    //
    // We don't visit library identifiers or their children because they have already been resolved.
    //
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      super.visitMethodDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    //
    // We visit the target and argument list, but do not visit the method name because it needs to
    // be visited in the context of the invocation.
    //
    safelyVisit(node.getTarget());
    node.accept(elementResolver);
    inferFunctionExpressionsParametersTypes(node.getArgumentList());
    safelyVisit(node.getArgumentList());
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitNode(AstNode node) {
    node.visitChildren(this);
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    //
    // We visit the prefix, but do not visit the identifier because it needs to be visited in the
    // context of the prefix.
    //
    safelyVisit(node.getPrefix());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    //
    // We visit the target, but do not visit the property name because it needs to be visited in the
    // context of the property access node.
    //
    safelyVisit(node.getTarget());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    //
    // We visit the argument list, but do not visit the optional identifier because it needs to be
    // visited in the context of the constructor invocation.
    //
    safelyVisit(node.getArgumentList());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitShowCombinator(ShowCombinator node) {
    //
    // Combinators aren't visited by this visitor, the LibraryResolver has already resolved the
    // identifiers and there is no type analysis to be done.
    //
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    //
    // We visit the argument list, but do not visit the optional identifier because it needs to be
    // visited in the context of the constructor invocation.
    //
    safelyVisit(node.getArgumentList());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    overrideManager.enterScope();
    try {
      super.visitSwitchCase(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    overrideManager.enterScope();
    try {
      super.visitSwitchDefault(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    overrideManager.enterScope();
    try {
      super.visitTopLevelVariableDeclaration(node);
    } finally {
      HashMap<Element, Type> overrides = overrideManager.captureOverrides(node.getVariables());
      overrideManager.exitScope();
      overrideManager.applyOverrides(overrides);
    }
    return null;
  }

  @Override
  public Void visitTypeName(TypeName node) {
    //
    // We don't visit type names or their children because they have already been resolved.
    //
    return null;
  }

  @Override
  public Void visitWhileStatement(WhileStatement node) {
    Expression condition = node.getCondition();
    safelyVisit(condition);
    Statement body = node.getBody();
    if (body != null) {
      overrideManager.enterScope();
      try {
        propagateTrueState(condition);
        visitStatementInScope(body);
      } finally {
        overrideManager.exitScope();
      }
    }
    // TODO(brianwilkerson) If the loop can only be exited because the condition is false, then
    // propagateFalseState(condition);
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  /**
   * Return the class element representing the class containing the current node, or {@code null} if
   * the current node is not contained in a class.
   * 
   * @return the class element representing the class containing the current node
   */
  protected ClassElement getEnclosingClass() {
    return enclosingClass;
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
   * Return the propagated element associated with the given expression whose type can be
   * overridden, or {@code null} if there is no element whose type can be overridden.
   * 
   * @param expression the expression with which the element is associated
   * @return the element associated with the given expression
   */
  protected VariableElement getOverridablePropagatedElement(Expression expression) {
    Element element = null;
    if (expression instanceof SimpleIdentifier) {
      element = ((SimpleIdentifier) expression).getPropagatedElement();
    } else if (expression instanceof PrefixedIdentifier) {
      element = ((PrefixedIdentifier) expression).getPropagatedElement();
    } else if (expression instanceof PropertyAccess) {
      element = ((PropertyAccess) expression).getPropertyName().getPropagatedElement();
    }
    if (element instanceof VariableElement) {
      return (VariableElement) element;
    }
    return null;
  }

  /**
   * Return the static element associated with the given expression whose type can be overridden, or
   * {@code null} if there is no element whose type can be overridden.
   * 
   * @param expression the expression with which the element is associated
   * @return the element associated with the given expression
   */
  protected VariableElement getOverridableStaticElement(Expression expression) {
    Element element = null;
    if (expression instanceof SimpleIdentifier) {
      element = ((SimpleIdentifier) expression).getStaticElement();
    } else if (expression instanceof PrefixedIdentifier) {
      element = ((PrefixedIdentifier) expression).getStaticElement();
    } else if (expression instanceof PropertyAccess) {
      element = ((PropertyAccess) expression).getPropertyName().getStaticElement();
    }
    if (element instanceof VariableElement) {
      return (VariableElement) element;
    }
    return null;
  }

  /**
   * Return the static element associated with the given expression whose type can be promoted, or
   * {@code null} if there is no element whose type can be promoted.
   * 
   * @param expression the expression with which the element is associated
   * @return the element associated with the given expression
   */
  protected VariableElement getPromotionStaticElement(Expression expression) {
    while (expression instanceof ParenthesizedExpression) {
      expression = ((ParenthesizedExpression) expression).getExpression();
    }
    if (!(expression instanceof SimpleIdentifier)) {
      return null;
    }
    SimpleIdentifier identifier = (SimpleIdentifier) expression;
    Element element = identifier.getStaticElement();
    if (!(element instanceof VariableElement)) {
      return null;
    }
    ElementKind kind = element.getKind();
    if (kind == ElementKind.LOCAL_VARIABLE) {
      return (VariableElement) element;
    }
    if (kind == ElementKind.PARAMETER) {
      return (VariableElement) element;
    }
    return null;
  }

  /**
   * If it is appropriate to do so, override the current type of the static and propagated elements
   * associated with the given expression with the given type. Generally speaking, it is appropriate
   * if the given type is more specific than the current type.
   * 
   * @param expression the expression used to access the static and propagated elements whose types
   *          might be overridden
   * @param potentialType the potential type of the elements
   */
  protected void overrideExpression(Expression expression, Type potentialType) {
    VariableElement element = getOverridableStaticElement(expression);
    if (element != null) {
      overrideVariable(element, potentialType);
    }
    element = getOverridablePropagatedElement(expression);
    if (element != null) {
      overrideVariable(element, potentialType);
    }
  }

  /**
   * If it is appropriate to do so, override the current type of the given element with the given
   * type. Generally speaking, it is appropriate if the given type is more specific than the current
   * type.
   * 
   * @param element the element whose type might be overridden
   * @param potentialType the potential type of the element
   */
  protected void overrideVariable(VariableElement element, Type potentialType) {
    if (potentialType == null || potentialType.isBottom()) {
      return;
    }
    Type currentType = getBestType(element);
    if (currentType == null || !currentType.isMoreSpecificThan(potentialType)) {
      if (element instanceof PropertyInducingElement) {
        PropertyInducingElement variable = (PropertyInducingElement) element;
        if (!variable.isConst() && !variable.isFinal()) {
          return;
        }
        ((PropertyInducingElementImpl) variable).setPropagatedType(potentialType);
      }
      overrideManager.setType(element, potentialType);
    }
  }

  @Override
  protected void visitForEachStatementInScope(ForEachStatement node) {
    //
    // We visit the iterator before the loop variable because the loop variable cannot be in scope
    // while visiting the iterator.
    //
    Expression iterator = node.getIterator();
    safelyVisit(iterator);
    DeclaredIdentifier loopVariable = node.getLoopVariable();
    SimpleIdentifier identifier = node.getIdentifier();
    safelyVisit(loopVariable);
    safelyVisit(identifier);
    Statement body = node.getBody();
    if (body != null) {
      overrideManager.enterScope();
      try {
        if (loopVariable != null && iterator != null) {
          LocalVariableElement loopElement = loopVariable.getElement();
          if (loopElement != null) {
            Type iteratorElementType = getIteratorElementType(iterator);
            overrideVariable(loopElement, iteratorElementType);
            recordPropagatedType(loopVariable.getIdentifier(), iteratorElementType);
          }
        } else if (identifier != null && iterator != null) {
          Element identifierElement = identifier.getStaticElement();
          if (identifierElement instanceof VariableElement) {
            Type iteratorElementType = getIteratorElementType(iterator);
            overrideVariable((VariableElement) identifierElement, iteratorElementType);
            recordPropagatedType(identifier, iteratorElementType);
          }
        }
        visitStatementInScope(body);
      } finally {
        overrideManager.exitScope();
      }
    }
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
  }

  @Override
  protected void visitForStatementInScope(ForStatement node) {
    safelyVisit(node.getVariables());
    safelyVisit(node.getInitialization());
    safelyVisit(node.getCondition());
    overrideManager.enterScope();
    try {
      propagateTrueState(node.getCondition());
      visitStatementInScope(node.getBody());
      node.getUpdaters().accept(this);
    } finally {
      overrideManager.exitScope();
    }
    // TODO(brianwilkerson) If the loop can only be exited because the condition is false, then
    // propagateFalseState(condition);
  }

  /**
   * Checks each promoted variable in the current scope for compliance with the following
   * specification statement:
   * <p>
   * If the variable <i>v</i> is accessed by a closure in <i>s<sub>1</sub></i> then the variable
   * <i>v</i> is not potentially mutated anywhere in the scope of <i>v</i>.
   */
  private void clearTypePromotionsIfAccessedInClosureAndProtentiallyMutated(AstNode target) {
    for (Element element : promoteManager.getPromotedElements()) {
      if (((VariableElementImpl) element).isPotentiallyMutatedInScope()) {
        if (isVariableAccessedInClosure(element, target)) {
          promoteManager.setType(element, null);
        }
      }
    }
  }

  /**
   * Checks each promoted variable in the current scope for compliance with the following
   * specification statement:
   * <p>
   * <i>v</i> is not potentially mutated in <i>s<sub>1</sub></i> or within a closure.
   */
  private void clearTypePromotionsIfPotentiallyMutatedIn(AstNode target) {
    for (Element element : promoteManager.getPromotedElements()) {
      if (isVariablePotentiallyMutatedIn(element, target)) {
        promoteManager.setType(element, null);
      }
    }
  }

  /**
   * Return the best type information available for the given element. If the type of the element
   * has been overridden, then return the overriding type. Otherwise, return the static type.
   * 
   * @param element the element for which type information is to be returned
   * @return the best type information available for the given element
   */
  private Type getBestType(Element element) {
    Type bestType = overrideManager.getType(element);
    if (bestType == null) {
      if (element instanceof LocalVariableElement) {
        bestType = ((LocalVariableElement) element).getType();
      } else if (element instanceof ParameterElement) {
        bestType = ((ParameterElement) element).getType();
      }
    }
    return bestType;
  }

  /**
   * The given expression is the expression used to compute the iterator for a for-each statement.
   * Attempt to compute the type of objects that will be assigned to the loop variable and return
   * that type. Return {@code null} if the type could not be determined.
   * 
   * @param iterator the iterator for a for-each statement
   * @return the type of objects that will be assigned to the loop variable
   */
  private Type getIteratorElementType(Expression iteratorExpression) {
    Type expressionType = iteratorExpression.getBestType();
    if (expressionType instanceof InterfaceType) {
      InterfaceType interfaceType = (InterfaceType) expressionType;
      FunctionType iteratorFunction = inheritanceManager.lookupMemberType(interfaceType, "iterator");
      if (iteratorFunction == null) {
        // TODO(brianwilkerson) Should we report this error?
        return null;
      }
      Type iteratorType = iteratorFunction.getReturnType();
      if (iteratorType instanceof InterfaceType) {
        InterfaceType iteratorInterfaceType = (InterfaceType) iteratorType;
        FunctionType currentFunction = inheritanceManager.lookupMemberType(
            iteratorInterfaceType,
            "current");
        if (currentFunction == null) {
          // TODO(brianwilkerson) Should we report this error?
          return null;
        }
        return currentFunction.getReturnType();
      }
    }
    return null;
  }

  /**
   * If given "mayBeClosure" is {@link FunctionExpression} without explicit parameters types and its
   * required type is {@link FunctionType}, then infer parameters types from {@link FunctionType}.
   */
  private void inferFunctionExpressionParametersTypes(Expression mayBeClosure,
      Type mayByFunctionType) {
    // prepare closure
    if (!(mayBeClosure instanceof FunctionExpression)) {
      return;
    }
    FunctionExpression closure = (FunctionExpression) mayBeClosure;
    // prepare expected closure type
    if (!(mayByFunctionType instanceof FunctionType)) {
      return;
    }
    FunctionType expectedClosureType = (FunctionType) mayByFunctionType;

    // If the expectedClosureType is not more specific than the static type, return.
    Type staticClosureType = closure.getElement() != null ? closure.getElement().getType() : null;
    if (staticClosureType != null && !expectedClosureType.isMoreSpecificThan(staticClosureType)) {
      return;
    }

    // set propagated type for the closure
    closure.setPropagatedType(expectedClosureType);
    // set inferred types for parameters
    NodeList<FormalParameter> parameters = closure.getParameters().getParameters();
    ParameterElement[] expectedParameters = expectedClosureType.getParameters();
    for (int i = 0; i < parameters.size() && i < expectedParameters.length; i++) {
      FormalParameter parameter = parameters.get(i);
      ParameterElement element = parameter.getElement();
      Type currentType = getBestType(element);
      // may be override the type
      Type expectedType = expectedParameters[i].getType();
      if (currentType == null || expectedType.isMoreSpecificThan(currentType)) {
        overrideManager.setType(element, expectedType);
      }
    }
  }

  /**
   * Try to infer types of parameters of the {@link FunctionExpression} arguments.
   */
  private void inferFunctionExpressionsParametersTypes(ArgumentList argumentList) {
    for (Expression argument : argumentList.getArguments()) {
      ParameterElement parameter = argument.getPropagatedParameterElement();
      if (parameter == null) {
        parameter = argument.getStaticParameterElement();
      }
      if (parameter != null) {
        inferFunctionExpressionParametersTypes(argument, parameter.getType());
      }
    }
  }

  /**
   * Return {@code true} if the given expression terminates abruptly (that is, if any expression
   * following the given expression will not be reached).
   * 
   * @param expression the expression being tested
   * @return {@code true} if the given expression terminates abruptly
   */
  private boolean isAbruptTerminationExpression(Expression expression) {
    // TODO(brianwilkerson) This needs to be significantly improved. Ideally we would eventually
    // turn this into a method on Expression that returns a termination indication (normal, abrupt
    // with no exception, abrupt with an exception).
    while (expression instanceof ParenthesizedExpression) {
      expression = ((ParenthesizedExpression) expression).getExpression();
    }
    return expression instanceof ThrowExpression || expression instanceof RethrowExpression;
  }

  /**
   * Return {@code true} if the given statement terminates abruptly (that is, if any statement
   * following the given statement will not be reached).
   * 
   * @param statement the statement being tested
   * @return {@code true} if the given statement terminates abruptly
   */
  private boolean isAbruptTerminationStatement(Statement statement) {
    // TODO(brianwilkerson) This needs to be significantly improved. Ideally we would eventually
    // turn this into a method on Statement that returns a termination indication (normal, abrupt
    // with no exception, abrupt with an exception).
    //
    // collinsn: it is unsound to assume that [break] and [continue] are "abrupt".
    // See: https://code.google.com/p/dart/issues/detail?id=19929#c4 (tests are
    // included in TypePropagationTest.java).
    // In general, the difficulty is loopy control flow.
    //
    // In the presence of exceptions things become much more complicated, but while
    // we only use this to propagate at [if]-statement join points, checking for [return]
    // is probably sound.
    if (statement instanceof ReturnStatement) {
      return true;
    } else if (statement instanceof ExpressionStatement) {
      return isAbruptTerminationExpression(((ExpressionStatement) statement).getExpression());
    } else if (statement instanceof Block) {
      NodeList<Statement> statements = ((Block) statement).getStatements();
      int size = statements.size();
      if (size == 0) {
        return false;
      }
      return isAbruptTerminationStatement(statements.get(size - 1));
    }
    return false;
  }

  /**
   * Return {@code true} if the given variable is accessed within a closure in the given
   * {@link AstNode} and also mutated somewhere in variable scope. This information is only
   * available for local variables (including parameters).
   * 
   * @param variable the variable to check
   * @param target the {@link AstNode} to check within
   * @return {@code true} if this variable is potentially mutated somewhere in the given ASTNode
   */
  private boolean isVariableAccessedInClosure(final Element variable, AstNode target) {
    final boolean[] result = {false};
    target.accept(new RecursiveAstVisitor<Void>() {
      private boolean inClosure = false;

      @Override
      public Void visitFunctionExpression(FunctionExpression node) {
        boolean inClosure = this.inClosure;
        try {
          this.inClosure = true;
          return super.visitFunctionExpression(node);
        } finally {
          this.inClosure = inClosure;
        }
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        if (result[0]) {
          return null;
        }
        if (inClosure && node.getStaticElement() == variable) {
          result[0] |= true;
        }
        return null;
      }
    });
    return result[0];
  }

  /**
   * Return {@code true} if the given variable is potentially mutated somewhere in the given
   * {@link AstNode}. This information is only available for local variables (including parameters).
   * 
   * @param variable the variable to check
   * @param target the {@link AstNode} to check within
   * @return {@code true} if this variable is potentially mutated somewhere in the given ASTNode
   */
  private boolean isVariablePotentiallyMutatedIn(final Element variable, AstNode target) {
    final boolean[] result = {false};
    target.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        if (result[0]) {
          return null;
        }
        if (node.getStaticElement() == variable) {
          if (node.inSetterContext()) {
            result[0] |= true;
          }
        }
        return null;
      }
    });
    return result[0];
  }

  /**
   * If it is appropriate to do so, promotes the current type of the static element associated with
   * the given expression with the given type. Generally speaking, it is appropriate if the given
   * type is more specific than the current type.
   * 
   * @param expression the expression used to access the static element whose types might be
   *          promoted
   * @param potentialType the potential type of the elements
   */
  private void promote(Expression expression, Type potentialType) {
    VariableElement element = getPromotionStaticElement(expression);
    if (element != null) {
      // may be mutated somewhere in closure
      if (((VariableElementImpl) element).isPotentiallyMutatedInClosure()) {
        return;
      }
      // prepare current variable type
      Type type = promoteManager.getType(element);
      if (type == null) {
        type = expression.getStaticType();
      }
      // Declared type should not be "dynamic".
      if (type == null || type.isDynamic()) {
        return;
      }
      // Promoted type should not be "dynamic".
      if (potentialType == null || potentialType.isDynamic()) {
        return;
      }
      // Promoted type should be more specific than declared.
      if (!potentialType.isMoreSpecificThan(type)) {
        return;
      }
      // Do promote type of variable.
      promoteManager.setType(element, potentialType);
    }
  }

  /**
   * Promotes type information using given condition.
   */
  private void promoteTypes(Expression condition) {
    if (condition instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) condition;
      if (binary.getOperator().getType() == TokenType.AMPERSAND_AMPERSAND) {
        Expression left = binary.getLeftOperand();
        Expression right = binary.getRightOperand();
        promoteTypes(left);
        promoteTypes(right);
        clearTypePromotionsIfPotentiallyMutatedIn(right);
      }
    } else if (condition instanceof IsExpression) {
      IsExpression is = (IsExpression) condition;
      if (is.getNotOperator() == null) {
        promote(is.getExpression(), is.getType().getType());
      }
    } else if (condition instanceof ParenthesizedExpression) {
      promoteTypes(((ParenthesizedExpression) condition).getExpression());
    }
  }

  /**
   * Propagate any type information that results from knowing that the given condition will have
   * been evaluated to 'false'.
   * 
   * @param condition the condition that will have evaluated to 'false'
   */
  private void propagateFalseState(Expression condition) {
    if (condition instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) condition;
      if (binary.getOperator().getType() == TokenType.BAR_BAR) {
        propagateFalseState(binary.getLeftOperand());
        propagateFalseState(binary.getRightOperand());
      }
    } else if (condition instanceof IsExpression) {
      IsExpression is = (IsExpression) condition;
      if (is.getNotOperator() != null) {
        overrideExpression(is.getExpression(), is.getType().getType());
      }
    } else if (condition instanceof PrefixExpression) {
      PrefixExpression prefix = (PrefixExpression) condition;
      if (prefix.getOperator().getType() == TokenType.BANG) {
        propagateTrueState(prefix.getOperand());
      }
    } else if (condition instanceof ParenthesizedExpression) {
      propagateFalseState(((ParenthesizedExpression) condition).getExpression());
    }
  }

  /**
   * Propagate any type information that results from knowing that the given expression will have
   * been evaluated without altering the flow of execution.
   * 
   * @param expression the expression that will have been evaluated
   */
  private void propagateState(Expression expression) {
    // TODO(brianwilkerson) Implement this.
  }

  /**
   * Propagate any type information that results from knowing that the given condition will have
   * been evaluated to 'true'.
   * 
   * @param condition the condition that will have evaluated to 'true'
   */
  private void propagateTrueState(Expression condition) {
    if (condition instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) condition;
      if (binary.getOperator().getType() == TokenType.AMPERSAND_AMPERSAND) {
        propagateTrueState(binary.getLeftOperand());
        propagateTrueState(binary.getRightOperand());
      }
    } else if (condition instanceof IsExpression) {
      IsExpression is = (IsExpression) condition;
      if (is.getNotOperator() == null) {
        overrideExpression(is.getExpression(), is.getType().getType());
      }
    } else if (condition instanceof PrefixExpression) {
      PrefixExpression prefix = (PrefixExpression) condition;
      if (prefix.getOperator().getType() == TokenType.BANG) {
        propagateFalseState(prefix.getOperand());
      }
    } else if (condition instanceof ParenthesizedExpression) {
      propagateTrueState(((ParenthesizedExpression) condition).getExpression());
    }
  }

  /**
   * Record that the propagated type of the given node is the given type.
   * 
   * @param expression the node whose type is to be recorded
   * @param type the propagated type of the node
   */
  private void recordPropagatedType(Expression expression, Type type) {
    if (type != null && !type.isDynamic()) {
      expression.setPropagatedType(type);
    }
  }
}
