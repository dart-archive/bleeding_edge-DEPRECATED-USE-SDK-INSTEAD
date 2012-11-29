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
package com.google.dart.engine.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.resolver.scope.ClassScope;
import com.google.dart.engine.resolver.scope.EnclosedScope;
import com.google.dart.engine.resolver.scope.FunctionScope;
import com.google.dart.engine.resolver.scope.LabelScope;
import com.google.dart.engine.resolver.scope.LibraryScope;
import com.google.dart.engine.resolver.scope.Scope;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;

import java.util.Map;

/**
 * Instances of the class {@code ResolverVisitor} are used to resolve the nodes within a single
 * compilation unit.
 */
public class ResolverVisitor extends RecursiveASTVisitor<Void> {
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
   * The scope used to resolve labels for {@code break} and {@code continue} statements, or
   * {@code null} if no labels have been defined in the current context.
   */
  private LabelScope labelScope;

  /**
   * A table mapping the identifiers of declared elements to the element that was declared.
   */
  private Map<ASTNode, Element> declaredElementMap;

  /**
   * A table mapping the AST nodes that have been resolved to the element to which they were
   * resolved.
   */
  private Map<ASTNode, Element> resolvedElementMap;

  /**
   * The type element representing the type most recently being visited.
   */
  private ClassElement enclosingType = null;

  /**
   * The executable element representing the method or function most recently being visited.
   */
  private ExecutableElement enclosingFunction = null;

  /**
   * A flag indicating whether we are currently
   */
  private boolean isLHS = false;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param definingLibrary the element for the library containing the compilation unit
   * @param source the source representing the compilation unit being visited
   * @param nameScope the name scope for the library containing the compilation unit
   * @param declaredElementMap a table mapping the identifiers of declared elements to the element
   *          that was declared
   * @param resolvedElementMap a table mapping the AST nodes that have been resolved to the element
   *          to which they were resolved
   */
  public ResolverVisitor(LibraryElement definingLibrary, Source source, LibraryScope nameScope,
      Map<ASTNode, Element> declaredElementMap, Map<ASTNode, Element> resolvedElementMap) {
    this.definingLibrary = definingLibrary;
    this.source = source;
    this.errorListener = nameScope.getErrorListener();
    this.nameScope = nameScope;
    this.declaredElementMap = declaredElementMap;
    this.resolvedElementMap = resolvedElementMap;
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    boolean wasLHS = isLHS;
    isLHS = true;
    try {
      node.getLeftHandSide().accept(this);
    } finally {
      isLHS = wasLHS;
    }
    node.getRightHandSide().accept(this);
    return null;
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    Token operator = node.getOperator();
    if (operator.isUserDefinableOperator()) {
      // TODO(brianwilkerson) Resolve the binary operator
//      TypeElement leftType = getType(node.getLeftOperand());
//      Element member = lookupInType(leftType, operator.getLexeme());
//      if (member == null) {
//        reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, operator, operator.getLexeme());
//      } else {
//        recordResolution(node, member);
//      }
    }
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitBlock(Block node) {
    Scope outerScope = nameScope;
    nameScope = new EnclosedScope(nameScope);
    try {
      node.visitChildren(this);
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    SimpleIdentifier labelNode = node.getLabel();
    LabelElementImpl labelElement = lookupLabel(labelNode);
    if (labelElement != null && labelElement.isOnSwitchMember()) {
      reportError(ResolverErrorCode.BREAK_LABEL_ON_SWITCH_MEMBER, labelNode);
    }
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    Element element = nameScope.lookup(node.getName(), definingLibrary);
    if (!(element instanceof ClassElement)) {
      // Internal error.
    }
    ClassElement outerType = enclosingType;
    Scope outerScope = nameScope;
    try {
      enclosingType = (ClassElement) element;
      nameScope = new ClassScope(nameScope, enclosingType);
      node.visitChildren(this);
    } finally {
      nameScope = outerScope;
      enclosingType = outerType;
    }
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    SimpleIdentifier labelNode = node.getLabel();
    LabelElementImpl labelElement = lookupLabel(labelNode);
    if (labelElement != null && labelElement.isOnSwitchStatement()) {
      reportError(ResolverErrorCode.CONTINUE_LABEL_ON_SWITCH, labelNode);
    }
    return null;
  }

  @Override
  public Void visitDoStatement(DoStatement node) {
    LabelScope outerScope = labelScope;
    labelScope = new LabelScope(outerScope, false, false);
    try {
      node.visitChildren(this);
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
      node.visitChildren(this);
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
      node.visitChildren(this);
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
      enclosingFunction = (ExecutableElement) declaredElementMap.get(functionName);
      recordResolution(functionName, enclosingFunction);
      node.visitChildren(this);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      // TODO(brianwilkerson) Figure out how to handle un-named functions
      SimpleIdentifier functionName = null;
      enclosingFunction = (ExecutableElement) declaredElementMap.get(functionName);
      recordResolution(functionName, enclosingFunction);
      node.visitChildren(this);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    // TODO(brianwilkerson) Resolve the function being invoked
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    // TODO(brianwilkerson) Resolve the uri, and names in combinators
    SimpleIdentifier prefixNode = node.getPrefix();
    if (prefixNode != null) {
      String prefixName = prefixNode.getName();
      for (PrefixElement prefixElement : definingLibrary.getPrefixes()) {
        if (prefixElement.getName().equals(prefixName)) {
          recordResolution(prefixNode, prefixElement);
        }
        return null;
      }
    }
    return null;
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    // TODO(brianwilkerson) Resolve the index operator
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    LabelScope outerScope = addScopesFor(node.getLabels());
    try {
      node.visitChildren(this);
    } finally {
      labelScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    recordResolution(node, definingLibrary);
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    Identifier methodName = node.getName();
    Element element = declaredElementMap.get(methodName);
    recordResolution(methodName, element);
    if (!(element instanceof ExecutableElement)) {
      // Internal error.
    }
    ExecutableElement outerFunction = enclosingFunction;
    Scope outerScope = nameScope;
    try {
      enclosingFunction = (ExecutableElement) element;
      nameScope = new FunctionScope(nameScope, enclosingFunction);
      node.visitChildren(this);
    } finally {
      nameScope = outerScope;
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    // TODO(brianwilkerson) Resolve the method being invoked
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    StringLiteral partUri = node.getPartUri();
    recordResolution(partUri, declaredElementMap.get(partUri));
    return null;
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    recordResolution(node, definingLibrary);
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    if (node.getOperator().isUserDefinableOperator()) {
      // TODO(brianwilkerson) Resolve the unary operator
    }
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    SimpleIdentifier prefix = node.getPrefix();
    SimpleIdentifier identifier = node.getIdentifier();

    Element prefixElement = nameScope.lookup(prefix, definingLibrary);
    recordResolution(prefix, prefixElement);
    if (prefixElement instanceof PrefixElement) {
      // TODO(brianwilkerson) Look up identifier.getName() in the prefixed libraries
    } else if (prefixElement instanceof ClassElement) {
      Element memberElement = lookupInType((ClassElement) prefixElement, identifier.getName());
      if (memberElement == null) {
        reportError(ResolverErrorCode.CANNOT_BE_RESOLVED, identifier, identifier.getName());
//      } else if (!element.isStatic()) {
//        reportError(ResolverErrorCode.STATIC_ACCESS_TO_INSTANCE_MEMBER, identifier, identifier.getName());
      } else {
        recordResolution(identifier, memberElement);
      }
    } else if (prefixElement instanceof VariableElement) {
      // TODO(brianwilkerson) Look for a member of the given variable's type. The problem is that we
      // have not yet done type analysis, so we might not know the type of the variable.
//      TypeElement variableType = ((VariableElement) prefixElement).getType().getElement();
//      Element memberElement = lookupInType(variableType, identifier.getName());
    } else {
      // reportError(ResolverErrorCode.UNDEFINED_PREFIX);
    }
    return null;
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    if (node.getOperator().isUserDefinableOperator()) {
      // TODO(brianwilkerson) Resolve the unary operator
    }
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element element = nameScope.lookup(node, definingLibrary);
    recordResolution(node, element);
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
        LabelElement labelElement = (LabelElement) declaredElementMap.get(labelName);
        recordResolution(labelName, labelElement);
        labelScope = new LabelScope(outerScope, labelName.getName(), labelElement);
      }
    }
    try {
      node.visitChildren(this);
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
      node.visitChildren(this);
    } finally {
      labelScope = outerScope;
    }
    return null;
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
      LabelElement labelElement = (LabelElement) declaredElementMap.get(labelNameNode);
      recordResolution(labelNameNode, labelElement);
      labelScope = new LabelScope(labelScope, labelName, labelElement);
    }
    return outerScope;
  }

//    private CompilationUnitElement unitElement;
//    @Override
//    public Void visitCompilationUnit(CompilationUnit node) {
//      unitElement = null; // TODO(brianwilkerson) Get the element for the given unit.
//      try {
//        
//      } finally {
//        unitElement = null;
//      }
//      return null;
//    }

  /**
   * Look up the name of a member in the given type. Return the element representing the member that
   * was found, or {@code null} if there is no member with the given name.
   * 
   * @param type the element representing the type in which the member is defined
   * @param memberName the name of the member being looked up
   * @return the element representing the member that was found
   */
  private Element lookupInType(ClassElement type, String memberName) {
//    if (type.isDynamic()) {
//      return ?;
//    }
    for (FieldElement element : type.getFields()) {
      if (element.getName().equals(memberName)) {
        return element;
      }
    }
    for (MethodElement element : type.getMethods()) {
      if (element.getName().equals(memberName)) {
        return element;
      }
    }
    for (TypeVariableElement element : type.getTypeVariables()) {
      if (element.getName().equals(memberName)) {
        return element;
      }
    }
    return null;
  }

  private LabelElementImpl lookupLabel(SimpleIdentifier labelNode) {
    LabelElementImpl labelElement = null;
    if (labelNode == null) {
      if (labelScope == null) {
        // TODO(brianwilkerson) Do we need to report this error, or is this condition always caught in the parser?
        // reportError(ResolverErrorCode.BREAK_OUTSIDE_LOOP);
      } else {
        labelElement = (LabelElementImpl) labelScope.lookup(LabelScope.EMPTY_LABEL);
        if (labelElement == null) {
          // TODO(brianwilkerson) Do we need to report this error, or is this condition always caught in the parser?
          // reportError(ResolverErrorCode.BREAK_OUTSIDE_LOOP);
        }
      }
    } else {
      if (labelScope == null) {
        reportError(ResolverErrorCode.UNDEFINED_LABEL, labelNode, labelNode.getName());
      } else {
        labelElement = (LabelElementImpl) labelScope.lookup(labelNode);
        if (labelElement == null) {
          reportError(ResolverErrorCode.UNDEFINED_LABEL, labelNode, labelNode.getName());
        } else {
          recordResolution(labelNode, labelElement);
        }
      }
    }
    if (labelElement != null) {
      ExecutableElement labelContainer = labelElement.getAncestor(ExecutableElement.class);
      if (labelContainer != enclosingFunction) {
        reportError(ResolverErrorCode.LABEL_IN_OUTER_SCOPE, labelNode, labelNode.getName());
        labelElement = null;
      }
    }
    return labelElement;
  }

  /**
   * Record the fact that the given AST node was resolved to the given element.
   * 
   * @param node the AST node that was resolved
   * @param element the element to which the AST node was resolved
   */
  private void recordResolution(ASTNode node, Element element) {
    if (element != null) {
      resolvedElementMap.put(node, element);
    }
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param identifier the identifier specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  private void reportError(ResolverErrorCode errorCode, SimpleIdentifier identifier,
      Object... arguments) {
    reportError(errorCode, identifier.getToken(), arguments);
  }

  /**
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param token the token specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  private void reportError(ResolverErrorCode errorCode, Token token, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        token.getOffset(),
        token.getLength(),
        errorCode,
        arguments));
  }
}
