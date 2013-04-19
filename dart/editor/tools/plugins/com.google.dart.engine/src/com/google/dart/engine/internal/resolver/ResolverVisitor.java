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
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ClassDeclaration;
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
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.HideCombinator;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ParenthesizedExpression;
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
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.type.BottomTypeImpl;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code ResolverVisitor} are used to resolve the nodes within a single
 * compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class ResolverVisitor extends ScopedVisitor {
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
   * The element representing the function containing the current node, or {@code null} if the
   * current node is not contained in a function.
   */
  private ExecutableElement enclosingFunction = null;

  /**
   * The object keeping track of which elements have had their types overridden.
   */
  private TypeOverrideManager overrideManager = new TypeOverrideManager();

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
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
      AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, errorListener);
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

  @Override
  public Void visitAsExpression(AsExpression node) {
    super.visitAsExpression(node);
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      VariableElement element = getOverridableElement(node.getExpression());
      if (element != null) {
        Type type = node.getType().getType();
        if (type != null) {
          override(element, getType(element), type);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitAssertStatement(AssertStatement node) {
    super.visitAssertStatement(node);
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      propagateTrueState(node.getCondition());
    }
    return null;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      TokenType operatorType = node.getOperator().getType();
      if (operatorType == TokenType.AMPERSAND_AMPERSAND) {
        Expression leftOperand = node.getLeftOperand();
        leftOperand.accept(this);
        Expression rightOperand = node.getRightOperand();
        if (rightOperand != null) {
          try {
            overrideManager.enterScope();
            propagateTrueState(leftOperand);
            rightOperand.accept(this);
          } finally {
            overrideManager.exitScope();
          }
        }
      } else if (operatorType == TokenType.BAR_BAR) {
        Expression leftOperand = node.getLeftOperand();
        leftOperand.accept(this);
        Expression rightOperand = node.getRightOperand();
        if (rightOperand != null) {
          try {
            overrideManager.enterScope();
            propagateFalseState(leftOperand);
            rightOperand.accept(this);
          } finally {
            overrideManager.exitScope();
          }
        }
      } else {
        node.getLeftOperand().accept(this);
        node.getRightOperand().accept(this);
      }
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
    } else {
      super.visitBinaryExpression(node);
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
    ClassElement outerType = enclosingClass;
    try {
      enclosingClass = node.getElement();
      typeAnalyzer.setThisType(enclosingClass == null ? null : enclosingClass.getType());
      super.visitClassDeclaration(node);
    } finally {
      typeAnalyzer.setThisType(outerType == null ? null : outerType.getType());
      enclosingClass = outerType;
    }
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
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      //
      // TODO(brianwilkerson) The goal of the code below is to visit the declarations in such an
      // order that we can infer type information for top-level variables before we visit references
      // to them. This is better than making no effort, but still doesn't completely satisfy that
      // goal (consider for example "final var a = b; final var b = 0;"; we'll infer a type of 'int'
      // for 'b', but not for 'a' because of the order of the visits). It should be improved.
      //
      try {
        overrideManager.enterScope();
        for (Directive directive : node.getDirectives()) {
          directive.accept(this);
        }
        ArrayList<CompilationUnitMember> classes = new ArrayList<CompilationUnitMember>();
        for (CompilationUnitMember declaration : node.getDeclarations()) {
          if (declaration instanceof ClassDeclaration) {
            classes.add(declaration);
          } else {
            declaration.accept(this);
          }
        }
        for (CompilationUnitMember declaration : classes) {
          declaration.accept(this);
        }
      } finally {
        overrideManager.exitScope();
      }
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
    } else {
      super.visitCompilationUnit(node);
    }
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      Expression condition = node.getCondition();
      condition.accept(this);
      Expression thenExpression = node.getThenExpression();
      if (thenExpression != null) {
        try {
          overrideManager.enterScope();
          propagateTrueState(condition);
          thenExpression.accept(this);
        } finally {
          overrideManager.exitScope();
        }
      }
      Expression elseExpression = node.getElseExpression();
      if (elseExpression != null) {
        try {
          overrideManager.enterScope();
          propagateFalseState(condition);
          elseExpression.accept(this);
        } finally {
          overrideManager.exitScope();
        }
      }
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
      boolean thenIsAbrupt = thenExpression != null && isAbruptTermination(thenExpression);
      boolean elseIsAbrupt = elseExpression != null && isAbruptTermination(elseExpression);
      if (elseIsAbrupt && !thenIsAbrupt) {
        // TODO(brianwilkerson) This is sub-optimal because it only preserves information inferred
        // from the condition and looses information inferred from the then and else expressions.
        propagateTrueState(condition);
      } else if (thenIsAbrupt && !elseIsAbrupt) {
        propagateFalseState(condition);
      }
    } else {
      super.visitConditionalExpression(node);
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
  public Void visitFieldDeclaration(FieldDeclaration node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitFieldDeclaration(node);
      } finally {
        HashMap<Element, Type> overrides = captureOverrides(node.getFields());
        overrideManager.exitScope();
        applyOverrides(overrides);
      }
    } else {
      super.visitFieldDeclaration(node);
    }
    return null;
  }

  @Override
  public Void visitForEachStatement(ForEachStatement node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitForEachStatement(node);
      } finally {
        overrideManager.exitScope();
      }
    } else {
      super.visitForEachStatement(node);
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitForStatement(node);
      } finally {
        overrideManager.exitScope();
      }
    } else {
      super.visitForStatement(node);
    }
    return null;
  }

  @Override
  public Void visitFunctionBody(FunctionBody node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitFunctionBody(node);
      } finally {
        overrideManager.exitScope();
      }
    } else {
      super.visitFunctionBody(node);
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
      enclosingFunction = node.getElement();
      if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
        overrideManager.enterScope();
      }
      super.visitFunctionExpression(node);
    } finally {
      if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
        overrideManager.exitScope();
      }
      enclosingFunction = outerFunction;
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
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      Expression condition = node.getCondition();
      condition.accept(this);
      Statement thenStatement = node.getThenStatement();
      if (thenStatement != null) {
        try {
          overrideManager.enterScope();
          propagateTrueState(condition);
          thenStatement.accept(this);
        } finally {
          overrideManager.exitScope();
        }
      }
      Statement elseStatement = node.getElseStatement();
      if (elseStatement != null) {
        try {
          overrideManager.enterScope();
          propagateFalseState(condition);
          elseStatement.accept(this);
        } finally {
          overrideManager.exitScope();
        }
      }
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
      boolean thenIsAbrupt = thenStatement != null && isAbruptTermination(thenStatement);
      boolean elseIsAbrupt = elseStatement != null && isAbruptTermination(elseStatement);
      if (elseIsAbrupt && !thenIsAbrupt) {
        // TODO(brianwilkerson) This is sub-optimal because it only preserves information inferred
        // from the condition and looses information inferred from the then and else statements.
        propagateTrueState(condition);
      } else if (thenIsAbrupt && !elseIsAbrupt) {
        propagateFalseState(condition);
      }
    } else {
      super.visitIfStatement(node);
    }
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
    safelyVisit(node.getArgumentList());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
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
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitSwitchCase(node);
      } finally {
        overrideManager.exitScope();
      }
    } else {
      super.visitSwitchCase(node);
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitSwitchDefault(node);
      } finally {
        overrideManager.exitScope();
      }
    } else {
      super.visitSwitchDefault(node);
    }
    return null;
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      try {
        overrideManager.enterScope();
        super.visitTopLevelVariableDeclaration(node);
      } finally {
        HashMap<Element, Type> overrides = captureOverrides(node.getVariables());
        overrideManager.exitScope();
        applyOverrides(overrides);
      }
    } else {
      super.visitTopLevelVariableDeclaration(node);
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
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      Expression condition = node.getCondition();
      condition.accept(this);
      Statement body = node.getBody();
      if (body != null) {
        try {
          overrideManager.enterScope();
          propagateTrueState(condition);
          body.accept(this);
        } finally {
          overrideManager.exitScope();
        }
      }
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
    } else {
      super.visitWhileStatement(node);
    }
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
   * Return the element associated with the given expression whose type can be overridden, or
   * {@code null} if there is no element whose type can be overridden.
   * 
   * @param expression the expression with which the element is associated
   * @return the element associated with the given expression
   */
  protected VariableElement getOverridableElement(Expression expression) {
    if (expression instanceof SimpleIdentifier) {
      Element element = ((SimpleIdentifier) expression).getElement();
      if (element instanceof VariableElement) {
        return (VariableElement) element;
      }
    }
    return null;
  }

  @Override
  protected void visitForEachStatementInScope(ForEachStatement node) {
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      DeclaredIdentifier loopVariable = node.getLoopVariable();
      safelyVisit(loopVariable);
      Expression iterator = node.getIterator();
      if (iterator != null) {
        iterator.accept(this);
        if (loopVariable != null) {
          LocalVariableElement loopElement = loopVariable.getElement();
          override(loopElement, loopElement.getType(), getIteratorElementType(iterator));
        }
      }
      safelyVisit(node.getBody());
      node.accept(elementResolver);
      node.accept(typeAnalyzer);
    } else {
      super.visitForEachStatementInScope(node);
    }
  }

  /**
   * Apply a set of overrides that were previously captured.
   * 
   * @param overrides the overrides to be applied
   */
  private void applyOverrides(HashMap<Element, Type> overrides) {
    for (Map.Entry<Element, Type> entry : overrides.entrySet()) {
      overrideManager.setType(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Return a map from the elements for the variables in the given list that have their types
   * overridden to the overriding type.
   * 
   * @param variableList the list of variables whose overriding types are to be captured
   * @return a table mapping elements to their overriding types
   */
  private HashMap<Element, Type> captureOverrides(VariableDeclarationList variableList) {
    HashMap<Element, Type> overrides = new HashMap<Element, Type>();
    if (StaticTypeAnalyzer.USE_TYPE_PROPAGATION) {
      if (variableList.isConst() || variableList.isFinal()) {
        for (VariableDeclaration variable : variableList.getVariables()) {
          Element element = variable.getElement();
          if (element != null) {
            Type type = overrideManager.getType(element);
            if (type != null) {
              overrides.put(element, type);
            }
          }
        }
      }
    }
    return overrides;
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
    Type expressionType = iteratorExpression.getStaticType();
    if (expressionType instanceof InterfaceType) {
      PropertyAccessorElement iterator = ((InterfaceType) expressionType).lookUpGetter(
          "iterator",
          getDefiningLibrary());
      if (iterator == null) {
        // TODO(brianwilkerson) Should we report this error?
        return null;
      }
      Type iteratorType = iterator.getType().getReturnType();
      if (iteratorType instanceof InterfaceType) {
        PropertyAccessorElement current = ((InterfaceType) iteratorType).lookUpGetter(
            "current",
            getDefiningLibrary());
        if (current == null) {
          // TODO(brianwilkerson) Should we report this error?
          return null;
        }
        return current.getType().getReturnType();
      }
    }
    return null;
  }

  /**
   * Return the type of the given (overridable) element.
   * 
   * @param element the element whose type is to be returned
   * @return the type of the given element
   */
  private Type getType(Element element) {
    if (element instanceof LocalVariableElement) {
      return ((LocalVariableElement) element).getType();
    } else if (element instanceof ParameterElement) {
      return ((ParameterElement) element).getType();
    }
    return null;
  }

  /**
   * Return {@code true} if the given expression terminates abruptly (that is, if any expression
   * following the given expression will not be reached).
   * 
   * @param expression the expression being tested
   * @return {@code true} if the given expression terminates abruptly
   */
  private boolean isAbruptTermination(Expression expression) {
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
  private boolean isAbruptTermination(Statement statement) {
    // TODO(brianwilkerson) This needs to be significantly improved. Ideally we would eventually
    // turn this into a method on Statement that returns a termination indication (normal, abrupt
    // with no exception, abrupt with an exception).
    if (statement instanceof ReturnStatement) {
      return true;
    } else if (statement instanceof ExpressionStatement) {
      return isAbruptTermination(((ExpressionStatement) statement).getExpression());
    } else if (statement instanceof Block) {
      NodeList<Statement> statements = ((Block) statement).getStatements();
      int size = statements.size();
      if (size == 0) {
        return false;
      }
      return isAbruptTermination(statements.get(size - 1));
    }
    return false;
  }

  /**
   * If it is appropriate to do so, override the type of the given element. Use the static type and
   * inferred type of the element to determine whether or not it is appropriate.
   * 
   * @param element the element whose type might be overridden
   * @param staticType the static type of the element
   * @param inferredType the inferred type of the element
   */
  private void override(VariableElement element, Type staticType, Type inferredType) {
    if (inferredType == BottomTypeImpl.getInstance()) {
      return;
    }
    if (element instanceof PropertyInducingElement) {
      PropertyInducingElement variable = (PropertyInducingElement) element;
      if (!variable.isConst() && !variable.isFinal()) {
        return;
      }
    }
    if (staticType == null || (inferredType != null && inferredType.isMoreSpecificThan(staticType))) {
      overrideManager.setType(element, inferredType);
    }
  }

  /**
   * Propagate any type information that results from knowing that the given condition will have
   * evaluated to 'false'.
   * 
   * @param condition the condition that will have evaluated to 'false'
   */
  private void propagateFalseState(Expression condition) {
    while (condition instanceof ParenthesizedExpression) {
      condition = ((ParenthesizedExpression) condition).getExpression();
    }
    if (condition instanceof IsExpression) {
      IsExpression is = (IsExpression) condition;
      if (is.getNotOperator() != null) {
        VariableElement element = getOverridableElement(is.getExpression());
        if (element != null) {
          Type type = is.getType().getType();
          if (type != null) {
            override(element, getType(element), type);
          }
        }
      }
    } else if (condition instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) condition;
      if (binary.getOperator().getType() == TokenType.BAR_BAR) {
        propagateFalseState(binary.getLeftOperand());
        propagateFalseState(binary.getRightOperand());
      }
    }
  }

  /**
   * Propagate any type information that results from knowing that the given condition will have
   * evaluated to 'true'.
   * 
   * @param condition the condition that will have evaluated to 'true'
   */
  private void propagateTrueState(Expression condition) {
    while (condition instanceof ParenthesizedExpression) {
      condition = ((ParenthesizedExpression) condition).getExpression();
    }
    if (condition instanceof IsExpression) {
      IsExpression is = (IsExpression) condition;
      if (is.getNotOperator() == null) {
        VariableElement element = getOverridableElement(is.getExpression());
        if (element != null) {
          Type type = is.getType().getType();
          if (type != null) {
            override(element, getType(element), type);
          }
        }
      }
    } else if (condition instanceof BinaryExpression) {
      BinaryExpression binary = (BinaryExpression) condition;
      if (binary.getOperator().getType() == TokenType.AMPERSAND_AMPERSAND) {
        propagateTrueState(binary.getLeftOperand());
        propagateTrueState(binary.getRightOperand());
      }
    }
  }

  /**
   * Visit the given AST node if it is not null.
   * 
   * @param node the node to be visited
   */
  private void safelyVisit(ASTNode node) {
    if (node != null) {
      node.accept(this);
    }
  }
}
