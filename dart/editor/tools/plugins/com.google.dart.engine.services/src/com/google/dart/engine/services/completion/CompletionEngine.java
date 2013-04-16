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
package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.EphemeralIdentifier;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.element.DynamicElementImpl;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchPattern;
import com.google.dart.engine.search.SearchPatternFactory;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.search.SearchScopeFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The analysis engine for code completion.
 * <p>
 * Note: During development package-private methods are used to group element-specific completion
 * utilities.
 * 
 * @coverage com.google.dart.engine.services.completion
 */
public class CompletionEngine {

  abstract class AstNodeClassifier extends GeneralizingASTVisitor<Void> {
    @Override
    public Void visitNode(ASTNode node) {
      return null;
    }
  }

  class NameCollector {
    private Map<String, List<Element>> uniqueNames = new HashMap<String, List<Element>>();

    void addNamesDefinedByExecutable(ExecutableElement execElement) {
      mergeNames(execElement.getParameters());
      mergeNames(execElement.getLocalVariables());
    }

    void addNamesDefinedByType(InterfaceType type) {
      mergeNames(type.getElement().getAccessors());
      mergeNames(type.getElement().getMethods());
      mergeNames(type.getElement().getTypeVariables());
      filterStaticRefs(type.getElement().getAccessors());
      filterStaticRefs(type.getElement().getMethods());
    }

    void addNamesDefinedByTypes(InterfaceType[] types) {
      for (InterfaceType type : types) {
        addNamesDefinedByType(type);
      }
    }

    void addTopLevelNames() {
      if (!state.areLiteralsAllowed) {
        mergeNames(findAllTypes());
      }
      if (!state.areClassesRequired) {
        mergeNames(findAllVariables());
        mergeNames(findAllFunctions());
        mergeNames(findAllPrefixes());
      }
    }

    Collection<List<Element>> getNames() {
      return uniqueNames.values();
    }

    void remove(Element element) {
      String name = element.getName();
      List<Element> list = uniqueNames.get(name);
      if (list == null) {
        return;
      }
      list.remove(element);
      if (list.isEmpty()) {
        uniqueNames.remove(name);
      }
    }

    private void filterStaticRefs(ExecutableElement[] elements) {
      for (ExecutableElement execElem : elements) {
        if (state.areInstanceReferencesProhibited && !execElem.isStatic()) {
          remove(execElem);
        } else if (state.areStaticReferencesProhibited && execElem.isStatic()) {
          remove(execElem);
        } else if (!state.areOperatorsAllowed && execElem.isOperator()) {
          remove(execElem);
        } else if (state.areMethodsProhibited && !execElem.isOperator()) {
          remove(execElem);
        }
      }
    }

    private void mergeNames(Element[] elements) {
      for (Element element : elements) {
        String name = element.getName();
        List<Element> dups = uniqueNames.get(name);
        if (dups == null) {
          dups = new ArrayList<Element>();
          uniqueNames.put(name, dups);
        }
        dups.add(element);
      }
    }
  }

  private class Filter {
    String prefix;
    boolean isPrivateDisallowed = true;

    Filter(SimpleIdentifier ident) {
      int loc = context.getSelectionOffset();
      int pos = ident.getOffset();
      int len = loc - pos;
      if (len > 0) {
        String name = ident.getName();
        if (len <= name.length()) {
          prefix = name.substring(0, len);
        } else {
          prefix = "";
        }
      } else {
        prefix = "";
      }
      if (prefix.length() >= 1) {
        isPrivateDisallowed = !Identifier.isPrivateName(prefix);
      }
    }

    boolean isPermitted(String name) {
      if (isPrivateDisallowed) {
        if (name.length() > 0 && Identifier.isPrivateName(name)) {
          return false;
        }
      }
      return true;
    }

    boolean match(Element elem) {
      return match(elem.getName());
    }

    boolean match(String name) {
      // Return true if the filter passes. Return false for private elements that should not be visible
      // in the current context, or for library elements that are not accessible in the context (NYI).
      return isPermitted(name) && name.startsWith(prefix);
    }
  }

  /**
   * An Ident is a wrapper for a String that provides type equivalence with SimpleIdentifier.
   */
  private class Ident extends EphemeralIdentifier {
    private String name;

    Ident(ASTNode parent) {
      super(parent, completionLocation());
    }

    Ident(ASTNode parent, String name, int offset) {
      super(parent, offset);
      this.name = name;
    }

    Ident(ASTNode parent, Token name) {
      super(parent, name.getOffset());
      this.name = name.getLexeme();
    }

    @Override
    public String getName() {
      if (name != null) {
        return name;
      }
      String n = super.getName();
      if (n != null) {
        return n;
      }
      return "";
    }
  }

  /**
   * An IdentifierCompleter is used to classify the parent of the completion node when it has
   * previously been determined that the completion node is a SimpleIdentifier.
   */
  private class IdentifierCompleter extends AstNodeClassifier {
    SimpleIdentifier completionNode;

    IdentifierCompleter(SimpleIdentifier node) {
      completionNode = node;
    }

    @Override
    public Void visitArgumentList(ArgumentList node) {
      if (completionNode instanceof SimpleIdentifier) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitAssignmentExpression(AssignmentExpression node) {
      if (completionNode instanceof SimpleIdentifier) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitBinaryExpression(BinaryExpression node) {
      if (node.getLeftOperand() == completionNode) {
        analyzeLocalName(completionNode);
      } else if (node.getRightOperand() == completionNode) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node) {
      if (node.getReturnType() == completionNode) {
        filter = new Filter(completionNode);
        pName(completionNode.getName(), ProposalKind.CONSTRUCTOR);
      }
      return null;
    }

    @Override
    public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
      // { A() : this.!x = 1; }
      if (node.getFieldName() == completionNode) {
        ClassElement classElement = ((ConstructorDeclaration) node.getParent()).getElement().getEnclosingElement();
        fieldReference(classElement, node.getFieldName());
      }
      return null;
    }

    @Override
    public Void visitConstructorName(ConstructorName node) {
      if (node.getName() == completionNode) {
        // { new A.!c(); }
        TypeName typeName = node.getType();
        if (typeName != null) {
          Type type = typeName.getType();
          Element typeElement = type.getElement();
          if (typeElement instanceof ClassElement) {
            ClassElement classElement = (ClassElement) typeElement;
            constructorReference(classElement, node.getName());
          }
        }
      }
      return null;
    }

    @Override
    public Void visitDoStatement(DoStatement node) {
      if (node.getCondition() == completionNode) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitExpression(Expression node) {
      SimpleIdentifier ident;
      if (completionNode instanceof SimpleIdentifier) {
        ident = completionNode;
      } else {
        ident = new Ident(node);
      }
      analyzeLocalName(ident);
      return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node) {
      SimpleIdentifier ident;
      if (completionNode instanceof SimpleIdentifier) {
        ident = completionNode;
      } else {
        ident = new Ident(node);
      }
      analyzeLocalName(ident);
      return null;
    }

    @Override
    public Void visitFieldFormalParameter(FieldFormalParameter node) {
      if (completionNode == node.getIdentifier()) {
        analyzeImmediateField(node.getIdentifier());
      }
      return null;
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node) {
      if (node.getIterator() == completionNode) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
      if (node.getName() == completionNode) {
        if (node.getReturnType() == null) {
          // This may be an incomplete class type alias
          state.includesUndefinedTypes();
          analyzeTypeName(node.getName(), typeDeclarationName(node));
        }
      }
      return null;
    }

    @Override
    public Void visitIfStatement(IfStatement node) {
      if (node.getCondition() == completionNode) {
        // { if (!) }
        analyzeLocalName(new Ident(node, completionNode.getToken()));
      }
      return null;
    }

    @Override
    public Void visitInterpolationExpression(InterpolationExpression node) {
      if (node.getExpression() instanceof SimpleIdentifier) {
        SimpleIdentifier ident = (SimpleIdentifier) node.getExpression();
        analyzeLocalName(ident);
      }
      return null;
    }

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node) {
      if (completionNode == node.getName()) {
        if (node.getReturnType() == null) {
          // class Foo {const F!(); }
          analyzeLocalName(completionNode); // TODO: This is too general; need to restrict to types when following const
        }
      }
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocation node) {
      if (node.getMethodName() == completionNode) {
        // { x.!y() }
        Expression expr = node.getTarget();
        Type receiverType;
        if (expr == null) { // use this
          receiverType = typeOfContainingClass(node);
          analyzeDirectAccess(receiverType, node.getMethodName());
        } else {
          receiverType = typeOf(expr);
          analyzePrefixedAccess(receiverType, node.getMethodName());
        }
      } else if (node.getTarget() == completionNode) {
        // { x!.y() } -- only reached when node.getTarget() is a simple identifier.
        if (completionNode instanceof SimpleIdentifier) {
          SimpleIdentifier ident = completionNode;
          analyzeReceiver(ident);
        }
      }
      return null;
    }

    @Override
    public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
      if (node.getPrefix() == completionNode) {
        // { x!.y }
        analyzeLocalName(node.getPrefix());
      } else {
        // { v.! }
        dispatchPrefixAnalysis(node, node.getIdentifier());
      }
      return null;
    }

    @Override
    public Void visitPropertyAccess(PropertyAccess node) {
      if (node.getTarget() != null && node.getTarget().getLength() == 0) {
        return null; // { . }
      }
      // { o.!hashCode }
      if (node.getPropertyName() == completionNode) {
        Type receiverType = typeOf(node.getRealTarget());
        analyzePrefixedAccess(receiverType, node.getPropertyName());
      }
      return null;
    }

    @Override
    public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
      // { A.Fac() : this.!b(); }
      if (node.getConstructorName() == completionNode) {
        ClassElement classElement = node.getElement().getEnclosingElement();
        constructorReference(classElement, node.getConstructorName());
      }
      return null;
    }

    @Override
    public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
      if (node.getIdentifier() == completionNode) {
        if (node.getKeyword() == null && node.getType() == null) {
          Ident ident = new Ident(node);
          analyzeTypeName(node.getIdentifier(), ident);
        }
      }
      return null;
    }

    @Override
    public Void visitSwitchStatement(SwitchStatement node) {
      if (node.getExpression() == completionNode) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitTypeName(TypeName node) {
      ASTNode parent = node.getParent();
      if (parent != null) {
        TypeNameCompleter visitor = new TypeNameCompleter(completionNode, node);
        return parent.accept(visitor);
      }
      return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameter node) {
      // { X<!Y> }
      if (isCompletionBetween(node.getOffset(), node.getEnd())) {
        analyzeTypeName(completionNode, typeDeclarationName(node));
      }
      return null;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclaration node) {
      if (node.getName() == completionNode) {
        analyzeDeclarationName(node);
      } else if (node.getInitializer() == completionNode) {
        analyzeLocalName((SimpleIdentifier) node.getInitializer());
      }
      return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement node) {
      if (node.getCondition() == completionNode) {
        analyzeLocalName(completionNode);
      }
      return null;
    }
  }

  /**
   * An StringCompleter is used to classify the parent of the completion node when it has previously
   * been determined that the completion node is a SimpleStringLiteral.
   */
  private class StringCompleter extends AstNodeClassifier {
    SimpleStringLiteral completionNode;

    StringCompleter(SimpleStringLiteral node) {
      completionNode = node;
    }

    @Override
    public Void visitImportDirective(ImportDirective node) {
      if (completionNode == node.getUri()) {
        importReference(node, completionNode);
      }
      return null;
    }
  }

  /**
   * A TerminalNodeCompleter is used to classify the completion node when nothing else is known
   * about it.
   */
  private class TerminalNodeCompleter extends AstNodeClassifier {

    @Override
    public Void visitArgumentList(ArgumentList node) {
      if (node.getArguments().isEmpty()
          && isCompletionBetween(
              node.getLeftParenthesis().getEnd(),
              node.getRightParenthesis().getOffset())) {
        analyzeLocalName(new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitAssertStatement(AssertStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitBlock(Block node) {
      if (isCompletionBetween(node.getLeftBracket().getEnd(), node.getRightBracket().getOffset())) {
        // { {! stmt; !} }
        analyzeLocalName(new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitBooleanLiteral(BooleanLiteral node) {
      analyzeLiteralReference(node);
      return null;
    }

    @Override
    public Void visitBreakStatement(BreakStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitCatchClause(CatchClause node) {
      if (isCompletingKeyword(node.getOnKeyword())) {
        pKeyword(node.getOnKeyword());
      } else if (isCompletingKeyword(node.getCatchKeyword())) {
        pKeyword(node.getCatchKeyword());
      }
      return null;
    }

    @Override
    public Void visitClassDeclaration(ClassDeclaration node) {
      if (isCompletingKeyword(node.getClassKeyword())) {
        pKeyword(node.getClassKeyword()); // Other keywords are legal but not handled here.
      } else if (isCompletingKeyword(node.getAbstractKeyword())) {
        pKeyword(node.getAbstractKeyword());
      } else if (!node.getLeftBracket().isSynthetic()) {
        if (isCompletionAfter(node.getLeftBracket().getEnd())) {
          if (node.getRightBracket().isSynthetic()
              || isCompletionBefore(node.getRightBracket().getOffset())) {
            if (!hasErrorBeforeCompletionLocation()) {
              analyzeLocalName(new Ident(node));
            }
          }
        }
      }
      // TODO { abstract ! class ! A ! extends B implements C, D ! {}}
      return null;
    }

    @Override
    public Void visitClassTypeAlias(ClassTypeAlias node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      // TODO { typedef ! A ! = ! B ! with C, D !; }
      return null;
    }

    @Override
    public Void visitCombinator(Combinator node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnit node) {
      // This is not a good terminal node...
      return null;
    }

    @Override
    public Void visitContinueStatement(ContinueStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitDirective(Directive node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitDoStatement(DoStatement node) {
      if (isCompletingKeyword(node.getDoKeyword())) {
        pKeyword(node.getDoKeyword());
      } else if (isCompletingKeyword(node.getWhileKeyword())) {
        pKeyword(node.getWhileKeyword());
      } else if (isCompletionBetween(
          node.getCondition().getEnd(),
          node.getRightParenthesis().getOffset())) {
        operatorAccess(node.getCondition(), new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitExpression(Expression node) {
      analyzeLocalName(new Ident(node));
      return null;
    }

    @Override
    public Void visitExpressionFunctionBody(ExpressionFunctionBody node) {
      if (isCompletionBetween(node.getExpression().getEnd(), node.getSemicolon().getOffset())) {
        operatorAccess(node.getExpression(), new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node) {
      analyzeLocalName(new Ident(node));
      return null;
    }

    @Override
    public Void visitExtendsClause(ExtendsClause node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (node.getSuperclass() == null) {
        // { X extends ! }
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      } else {
        // { X extends ! Y }
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      }
      return null;
    }

    @Override
    public Void visitForEachStatement(ForEachStatement node) {
      if (isCompletingKeyword(node.getForKeyword())) {
        pKeyword(node.getForKeyword());
      } else if (isCompletingKeyword(node.getInKeyword())) {
        pKeyword(node.getInKeyword());
      }
      return null;
    }

    @Override
    public Void visitFormalParameterList(FormalParameterList node) {
      if (isCompletionBetween(
          node.getLeftParenthesis().getEnd(),
          node.getRightParenthesis().getOffset())) {
        NodeList<FormalParameter> params = node.getParameters();
        if (!params.isEmpty()) {
          FormalParameter last = params.get(params.size() - 1);
          if (isCompletionBetween(last.getEnd(), node.getRightParenthesis().getOffset())) {
            List<FormalParameter> newParams = copyWithout(params, last);
            analyzeNewParameterName(newParams, last.getIdentifier(), null);
          } else {
            Ident ident = new Ident(node);
            analyzeTypeName(ident, ident);
          }
        } else {
          Ident ident = new Ident(node);
          analyzeTypeName(ident, ident);
        }
      }
      return null;
    }

    @Override
    public Void visitForStatement(ForStatement node) {
      if (isCompletingKeyword(node.getForKeyword())) {
        pKeyword(node.getForKeyword());
      }
      return null;
    }

    @Override
    public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitIfStatement(IfStatement node) {
      if (isCompletingKeyword(node.getIfKeyword())) {
        pKeyword(node.getIfKeyword());
      } else if (isCompletingKeyword(node.getElseKeyword())) {
        pKeyword(node.getElseKeyword());
      } else if (isCompletionBetween(
          node.getCondition().getEnd(),
          node.getRightParenthesis().getOffset())) {
        operatorAccess(node.getCondition(), new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitImplementsClause(ImplementsClause node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (node.getInterfaces().isEmpty()) {
        // { X implements ! }
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      } else {
        // { X implements ! Y }
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      }
      return null;
    }

    @Override
    public Void visitImportDirective(ImportDirective node) {
      if (isCompletingKeyword(node.getAsToken())) {
        pKeyword(node.getAsToken());
      } else {
        visitNamespaceDirective(node);
      }
      return null;
    }

    @Override
    public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
        Ident ident = new Ident(node, node.getKeyword());
        analyzeLocalName(ident);
      } else {
        Ident ident = new Ident(node);
        analyzeConstructorTypeName(ident);
      }
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocation node) {
      Token period = node.getPeriod();
      if (period != null && isCompletionAfter(period.getEnd())) {
        // { x.!y() }
        Expression expr = node.getTarget();
        Type receiverType = typeOf(expr);
        analyzePrefixedAccess(receiverType, node.getMethodName());
      }
      return null;
    }

    @Override
    public Void visitPartOfDirective(PartOfDirective node) {
      if (isCompletingKeyword(node.getOfToken())) {
        pKeyword(node.getOfToken());
      } else {
        visitDirective(node);
      }
      return null;
    }

    @Override
    public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
      if (isCompletionAfter(node.getPeriod().getEnd())) {
        if (isCompletionBefore(node.getIdentifier().getOffset())) {
          // { x.! } or { x.!  y } Note missing/implied semicolon before y; this looks like an
          // obscure case but it occurs frequently when editing existing code.
          dispatchPrefixAnalysis(node, node.getIdentifier());
        }
      }
      return null;
    }

    @Override
    public Void visitPropertyAccess(PropertyAccess node) {
      if (node.getTarget() != null && node.getTarget().getLength() == 0) {
        return null; // { . }
      }
      Type receiverType = typeOf(node.getRealTarget());
      analyzePrefixedAccess(receiverType, node.getPropertyName());
      return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (isCompletionBetween(node.getExpression().getEnd(), node.getSemicolon().getOffset())) {
        operatorAccess(node.getExpression(), new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
      if (node.getKeyword() != null && isCompletionBefore(node.getKeyword().getEnd())) {
        // f() { g(var! z) }
        final Token token = node.getKeyword();
        Ident ident = new Ident(node, token);
        analyzeTypeName(ident, ident);
      }
      return null;
    }

    @Override
    public Void visitSimpleIdentifier(SimpleIdentifier node) {
      ASTNode parent = node.getParent();
      if (parent != null) {
        IdentifierCompleter visitor = new IdentifierCompleter(node);
        return parent.accept(visitor);
      }
      return null;
    }

    @Override
    public Void visitSimpleStringLiteral(SimpleStringLiteral node) {
      ASTNode parent = node.getParent();
      if (parent instanceof Directive) {
        StringCompleter visitor = new StringCompleter(node);
        return parent.accept(visitor);
      }
      return null;
    }

    @Override
    public Void visitSwitchMember(SwitchMember node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitSwitchStatement(SwitchStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      }
      return null;
    }

    @Override
    public Void visitTryStatement(TryStatement node) {
      if (isCompletingKeyword(node.getTryKeyword())) {
        pKeyword(node.getTryKeyword());
      }
      return null;
    }

    @Override
    public Void visitTypeArgumentList(TypeArgumentList node) {
      if (isCompletionBetween(node.getLeftBracket().getEnd(), node.getRightBracket().getOffset())) {
        analyzeTypeName(new Ident(node), null);
      }
      return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameter node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (node.getName().getName().isEmpty()
          && isCompletionBefore(node.getKeyword().getOffset())) {
        // { < ! extends X> }
        analyzeTypeName(node.getName(), typeDeclarationName(node));
      }
      // { <! X ! extends ! Y !> }
      return null;
    }

    @Override
    public Void visitTypeParameterList(TypeParameterList node) {
      // { <X extends A,! B,! > }
      if (isCompletionBetween(node.getLeftBracket().getEnd(), node.getRightBracket().getOffset())) {
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      }
      return null;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclaration node) {
      if (isCompletionAfter(node.getEquals().getEnd())) {
        // { var x =! ...}
        analyzeLocalName(new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitVariableDeclarationList(VariableDeclarationList node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
        analyzeTypeName(new Ident(node, node.getKeyword()), null);
      }
      return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (isCompletionBetween(
          node.getCondition().getEnd(),
          node.getRightParenthesis().getOffset())) {
        operatorAccess(node.getCondition(), new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitWithClause(WithClause node) {
      if (isCompletingKeyword(node.getWithKeyword())) {
        pKeyword(node.getWithKeyword());
      } else if (node.getMixinTypes().isEmpty()) {
        // { X with ! }
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      } else {
        // { X with ! Y }
        analyzeTypeName(new Ident(node), typeDeclarationName(node));
      }
      return null;
    }
  }

  /**
   * A TypeNameCompleter is used to classify the parent of a SimpleIdentifier after it has been
   * identified as a TypeName by the IdentifierCompleter.
   */
  private class TypeNameCompleter extends AstNodeClassifier {
    SimpleIdentifier identifier;
    TypeName typeName;

    TypeNameCompleter(SimpleIdentifier identifier, TypeName typeName) {
      this.identifier = identifier;
      this.typeName = typeName;
    }

    @Override
    public Void visitCatchClause(CatchClause node) {
      if (node.getExceptionType() == typeName) {
        analyzeTypeName(identifier, null);
      }
      return null;
    }

    @Override
    public Void visitClassTypeAlias(ClassTypeAlias node) {
      analyzeTypeName(identifier, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitConstructorName(ConstructorName node) {
      if (typeName == node.getType()) {
        if (node.getPeriod() != null) {
          if (isCompletionAfter(node.getPeriod().getEnd())) {
            // Is this branch reachable? Probably only in IdentifierCompleter.
            "".toString(); // TODO This currently is just a place-holder for a breakpoint.
          } else {
            // { new Cla!ss.cons() }
            Element element = identifier.getElement();
            if (element instanceof ClassElement) {
              namedConstructorReference((ClassElement) element, identifier);
            }
          }
        } else {
          // { new ! } { new Na!me(); } { new js!on. }
          analyzeConstructorTypeName(identifier);
        }
      }
      return null;
    }

    @Override
    public Void visitExtendsClause(ExtendsClause node) {
      analyzeTypeName(identifier, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
      analyzeTypeName(identifier, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitImplementsClause(ImplementsClause node) {
      analyzeTypeName(identifier, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitIsExpression(IsExpression node) {
      if (typeName == node.getType()) {
        // TODO Confirm that this path always has simple identifiers
        analyzeTypeName((SimpleIdentifier) node.getType().getName(), null);
      }
      return null;
    }

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node) {
      if (node.getReturnType() == typeName) {
        analyzeTypeName(identifier, null);
      }
      return null;
    }

    @Override
    public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
      analyzeTypeName(identifier, null);
      return null;
    }

    @Override
    public Void visitTypeArgumentList(TypeArgumentList node) {
      if (isCompletionBetween(node.getLeftBracket().getEnd(), node.getRightBracket().getOffset())) {
        analyzeTypeName(identifier, null);
      }
      return null;
    }

    @Override
    public Void visitTypeParameter(TypeParameter node) {
      if (node.getBound() == typeName) {
        // { X<A extends !Y> }
        analyzeTypeName(identifier, typeDeclarationName(node));
      }
      return null;
    }

    @Override
    public Void visitVariableDeclarationList(VariableDeclarationList node) {
      if (node.getParent() instanceof Statement) {
        analyzeLocalName(identifier);
      } else {
        analyzeTypeName(identifier, null);
      }
      return null;
    }

    @Override
    public Void visitWithClause(WithClause node) {
      analyzeTypeName(identifier, typeDeclarationName(node));
      return null;
    }

  }

  // Review note: It may look like literals (pNull, etc) are coded redundantly, but that's because
  // all the code hasn't been written yet.
  private static final String C_DYNAMIC = "dynamic";
  private static final String C_FALSE = "false";
  private static final String C_NULL = "null";
  private static final String C_PARAMNAME = "arg";
  private static final String C_TRUE = "true";
  private static final String C_VAR = "var";
  private static final String C_VOID = "void";

  private CompletionRequestor requestor;
  private CompletionFactory factory;
  private AssistContext context;
  private Filter filter;
  private CompletionState state;
  private LibraryElement[] libraries;

  public CompletionEngine(CompletionRequestor requestor, CompletionFactory factory) {
    this.requestor = requestor;
    this.factory = factory;
    this.state = new CompletionState();
  }

  /**
   * Analyze the source unit in the given context to determine completion proposals at the selection
   * offset of the context.
   * 
   * @throws Exception
   */
  public void complete(AssistContext context) {
    this.context = context;
    requestor.beginReporting();
    ASTNode completionNode = context.getCoveredNode();
    if (completionNode != null) {
      state.setContext(completionNode);
      TerminalNodeCompleter visitor = new TerminalNodeCompleter();
      completionNode.accept(visitor);
    }
    requestor.endReporting();
  }

  void analyzeConstructorTypeName(SimpleIdentifier identifier) {
    filter = new Filter(identifier);
    Element[] types = findAllTypes();
    for (Element type : types) {
      if (type instanceof ClassElement) {
        namedConstructorReference((ClassElement) type, identifier);
      }
    }
    Element[] prefixes = findAllPrefixes();
    for (Element prefix : prefixes) {
      pName(prefix);
    }
  }

  void analyzeDeclarationName(VariableDeclaration varDecl) {
    // We might want to propose multiple names for a declaration based on types someday.
    // For now, just use whatever is already there.
    SimpleIdentifier identifier = varDecl.getName();
    filter = new Filter(identifier);
    TypeName type = ((VariableDeclarationList) varDecl.getParent()).getType();
    if (identifier.getLength() > 0) {
      pName(identifier);
    }
    if (type != null) {
      pParamName(type.getName().getName().toLowerCase());
    }
  }

  void analyzeDirectAccess(Type receiverType, SimpleIdentifier completionNode) {
    if (receiverType != null) {
      // Complete this.!y where this is absent
      Element rcvrTypeElem = receiverType.getElement();
      if (rcvrTypeElem.equals(DynamicElementImpl.getInstance())) {
        rcvrTypeElem = getObjectClassElement();
      }
      if (rcvrTypeElem instanceof ClassElement) {
        directAccess((ClassElement) rcvrTypeElem, completionNode);
      }
    }
  }

  void analyzeImmediateField(SimpleIdentifier fieldName) {
    filter = new Filter(fieldName);
    ClassDeclaration classDecl = fieldName.getAncestor(ClassDeclaration.class);
    ClassElement classElement = classDecl.getElement();
    for (FieldElement field : classElement.getFields()) {
      pName(field.getName(), ProposalKind.FIELD);
    }
  }

  void analyzeLiteralReference(BooleanLiteral literal) {
//    state.setContext(literal);
    Ident ident = new Ident(literal.getParent());
    ident.setToken(literal.getLiteral());
    filter = new Filter(ident);
    analyzeLocalName(ident);
  }

  void analyzeLocalName(SimpleIdentifier identifier) {
    // Completion x!
    filter = new Filter(identifier);
    // TODO Filter out types that have no static members.
    Collection<List<Element>> uniqueNames = collectIdentifiersVisibleAt(identifier);
    for (List<Element> uniques : uniqueNames) {
      Element candidate = uniques.get(0);
      if (state.isSourceDeclarationStatic) {
        if (candidate instanceof FieldElement) {
          if (!((FieldElement) candidate).isStatic()) {
            continue;
          }
        } else if (candidate instanceof PropertyAccessorElement) {
          if (!((PropertyAccessorElement) candidate).isStatic()) {
            continue;
          }
        }
      }
      if (state.isOptionalArgumentRequired) {
        if (!(candidate instanceof ParameterElement)) {
          continue;
        }
        ParameterElement param = (ParameterElement) candidate;
        if (!param.getParameterKind().isOptional()) {
          continue;
        }
      }
      pName(candidate);
    }
    if (state.areLiteralsAllowed) {
      pNull();
      pTrue();
      pFalse();
    }
  }

  void analyzeNewParameterName(List<FormalParameter> params, SimpleIdentifier typeIdent,
      String identifierName) {
    String typeName = typeIdent.getName();
    filter = new Filter(new Ident(typeIdent));
    List<String> names = new ArrayList<String>(params.size());
    for (FormalParameter node : params) {
      names.add(node.getIdentifier().getName());
    }
    // Find name similar to typeName not in names, ditto for identifierName.
    if (identifierName == null || identifierName.isEmpty()) {
      String candidate = typeName == null || typeName.isEmpty() ? C_PARAMNAME
          : typeName.toLowerCase();
      pParamName(makeNonconflictingName(candidate, names));
    } else {
      pParamName(makeNonconflictingName(identifierName, names));
      if (typeName != null && !typeName.isEmpty()) {
        pParamName(makeNonconflictingName(typeName.toLowerCase(), names));
      }
    }
  }

  void analyzePrefixedAccess(Type receiverType, SimpleIdentifier completionNode) {
    if (receiverType != null) {
      // Complete x.!y
      Element rcvrTypeElem = receiverType.getElement();
      if (rcvrTypeElem == null) {
        rcvrTypeElem = getObjectClassElement(); // { f() => null.! }
      }
      if (rcvrTypeElem.equals(DynamicElementImpl.getInstance())) {
        rcvrTypeElem = getObjectClassElement();
      }
      if (rcvrTypeElem instanceof ClassElement) {
        prefixedAccess((ClassElement) rcvrTypeElem, completionNode);
      }
    }
  }

  void analyzeReceiver(SimpleIdentifier identifier) {
    // Completion x!.y
    filter = new Filter(identifier);
    Collection<List<Element>> uniqueNames = collectIdentifiersVisibleAt(identifier);
    for (List<Element> uniques : uniqueNames) {
      Element candidate = uniques.get(0);
      pName(candidate);
    }
  }

  void analyzeTypeName(SimpleIdentifier identifier, SimpleIdentifier nameIdent) {
    filter = new Filter(identifier);
    String name = nameIdent == null ? "" : nameIdent.getName();
    Element[] types = findAllTypes();
    for (Element type : types) {
      if (state.isForMixin) {
        if (!(type instanceof ClassElement)) {
          continue;
        }
        ClassElement classElement = (ClassElement) type;
        if (!classElement.isValidMixin()) {
          continue;
        }
      }
      if (type.getName().equals(name)) {
        continue;
      }
      pName(type);
    }
    if (!state.isForMixin) {
      ClassDeclaration classDecl = identifier.getAncestor(ClassDeclaration.class);
      if (classDecl != null) {
        ClassElement classElement = classDecl.getElement();
        for (TypeVariableElement var : classElement.getTypeVariables()) {
          pName(var);
        }
      }
    }
    Element[] prefixes = findAllPrefixes();
    for (Element prefix : prefixes) {
      pName(prefix);
    }
    if (state.isDynamicAllowed) {
      pDynamic();
    }
    if (state.isVarAllowed) {
      pVar();
    }
    if (state.isVoidAllowed) {
      pVoid();
    }
  }

  void constructorReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a constructor defined in classElement.
    filter = new Filter(identifier);
    for (ConstructorElement cons : classElement.getConstructors()) {
      if (state.isCompileTimeConstantRequired == cons.isConst() && filterAllows(cons)) {
        pExecutable(cons, identifier);
      }
    }
  }

  void directAccess(ClassElement classElement, SimpleIdentifier identifier) {
    filter = new Filter(identifier);
    NameCollector names = new NameCollector();
    names.addNamesDefinedByTypes(allSuperTypes(classElement));
    names.addNamesDefinedByTypes(allSubtypes(classElement));
    names.addTopLevelNames();
    proposeNames(names, identifier);
  }

  void dispatchPrefixAnalysis(PrefixedIdentifier node, SimpleIdentifier identifier) {
    SimpleIdentifier receiverName = node.getPrefix();
    Element receiver = receiverName.getElement();
    if (receiver == null) {
      prefixedAccess(receiverName, identifier);
      return;
    }
    switch (receiver.getKind()) {
      case PREFIX:
      case IMPORT:
        // Complete lib_prefix.name
        prefixedAccess(receiverName, identifier);
        break;
      default: {
        Type receiverType = typeOf(receiver);
        analyzePrefixedAccess(receiverType, identifier);
        break;
      }
    }
  }

  void fieldReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a constructor defined in classElement.
    filter = new Filter(identifier);
    for (FieldElement cons : classElement.getFields()) {
      if (filterAllows(cons)) {
        pField(cons, identifier, classElement);
      }
    }
  }

  void importPackageReference(ImportDirective node, List<LibraryElement> libraries,
      List<LibraryElement> librariesInLib) {
    String prefix = filter.prefix;
    if (prefix.startsWith("dart:") || prefix.startsWith("package:")) {
      return;
    }
    if (isUnitInLibFolder(context.getCompilationUnit().getElement())) {
      importPackageReferenceFromList(node, prefix, librariesInLib);
    } else {
      importPackageReferenceFromList(node, prefix, libraries);
    }
  }

  void importPackageReferenceFromList(ImportDirective node, String prefix,
      List<LibraryElement> libraries) {
//    context.getCompilationUnit().getElement().getSource().getFullName();
//    URI baseUri = currentCompilationUnit.getUnderlyingResource().getParent().getLocationURI();
//    for (LibraryElement library : libraries) {
//      String name = URIUtilities.relativize(baseUri, library.getUri()).toString();
//      if (name.startsWith(prefix)) {
//        pName(name, ProposalKind.IMPORT);
//      }
//    }
  }

  void importPubReference(ImportDirective node, List<LibraryElement> packages,
      List<LibraryElement> librariesInLib) {

  }

  void importReference(ImportDirective node, SimpleStringLiteral literal) {
    String lit = literal.getLiteral().getLexeme();
    if (!lit.isEmpty()) {
      lit = lit.substring(1, Math.max(lit.length() - 1, 0));
    }
    filter = new Filter(new Ident(node, lit, literal.getOffset() + 1));
    List<LibraryElement> packages = new ArrayList<LibraryElement>();
    List<LibraryElement> libraries = new ArrayList<LibraryElement>();
    List<LibraryElement> librariesInLib = new ArrayList<LibraryElement>();
    String currentLibraryName = getCurrentLibrary().getSource().getFullName();
    AnalysisContext ac = getAnalysisContext();
    Source[] sources = ac.getLibrarySources();
    for (Source s : sources) {
      String sName = s.getFullName();
      if (currentLibraryName.equals(sName)) {
        continue;
      }
      LibraryElement lib = ac.getLibraryElement(s);
      if (lib == null) {
        continue;
      } else if (sName.contains("/packages/")) {
        packages.add(lib);
      } else if (isUnitInLibFolder(lib.getDefiningCompilationUnit())) {
        librariesInLib.add(lib);
      } else {
        libraries.add(lib);
      }
    }
    importSdkReference(node);
    importPubReference(node, packages, librariesInLib);
    importPackageReference(node, libraries, librariesInLib);
  }

  void importSdkReference(ImportDirective node) {
    String prefix = filter.prefix;
    String[] prefixStrings = prefix.split(":");
    if (!prefix.isEmpty() && !"dart:".startsWith(prefixStrings[0])) {
      return;
    }
    if (prefix.isEmpty()) {
      pName("dart:", ProposalKind.IMPORT);
      return;
    }
    List<LibraryElement> libs = getSystemLibraries();
    for (LibraryElement lib : libs) {
      String name = lib.getName();
      name = name.substring(5);
      if (filter.isPermitted(name)) {
        pName("dart:" + name, ProposalKind.IMPORT);
      }
    }
  }

  void namedConstructorReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a named constructor defined in classElement.
    filter = new Filter(identifier);
    for (ConstructorElement cons : classElement.getConstructors()) {
      if (state.isCompileTimeConstantRequired == cons.isConst()
          && filter.isPermitted(cons.getName())) {
        pNamedConstructor(classElement, cons, identifier);
      }
    }
  }

  void operatorAccess(Expression expr, SimpleIdentifier identifier) {
    state.requiresOperators();
    Type receiverType = typeOf(expr);
    analyzePrefixedAccess(receiverType, identifier);
  }

  void prefixedAccess(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to field or method in classElement.
    filter = new Filter(identifier);
    NameCollector names = new NameCollector();
    names.addNamesDefinedByTypes(allSuperTypes(classElement));
    names.addNamesDefinedByTypes(allSubtypes(classElement));
    proposeNames(names, identifier);
  }

  void prefixedAccess(SimpleIdentifier libName, SimpleIdentifier identifier) {
    filter = new Filter(identifier);
    libraries = librariesImportedByName(libName);
    NameCollector names = new NameCollector();
    names.addTopLevelNames();
    proposeNames(names, identifier);
  }

  private InterfaceType[] allSubtypes(ClassElement classElement) {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = SearchScopeFactory.createUniverseScope();
    List<SearchMatch> matches = engine.searchSubtypes(classElement, scope, null);
    InterfaceType[] subtypes = new InterfaceType[matches.size()];
    int i = 0;
    for (SearchMatch match : matches) {
      Element element = match.getElement();
      if (element instanceof ClassElement) {
        subtypes[i++] = ((ClassElement) element).getType();
      }
    }
    return subtypes;
  }

  private InterfaceType[] allSuperTypes(ClassElement classElement) {
    InterfaceType[] supertypes = classElement.getAllSupertypes();
    InterfaceType[] allTypes = new InterfaceType[supertypes.length + 1];
    allTypes[0] = classElement.getType();
    System.arraycopy(supertypes, 0, allTypes, 1, supertypes.length);
    return allTypes;
  }

  private Collection<List<Element>> collectIdentifiersVisibleAt(ASTNode ident) {
    NameCollector names = new NameCollector();
    Declaration decl = ident.getAncestor(Declaration.class);
    if (decl != null) {
      Element element = decl.getElement();
      if (element == null) {
        decl = decl.getParent().getAncestor(Declaration.class);
        if (decl != null) {
          element = decl.getElement();
        }
      }
      Element localDef = null;
      if (element instanceof LocalVariableElement) {
        decl = decl.getParent().getAncestor(Declaration.class);
        localDef = element;
        element = decl.getElement();
      }
      Element topLevelDef = null;
      if (element instanceof TopLevelVariableElement) {
        topLevelDef = element;
      }
      if (element instanceof ExecutableElement) {
        ExecutableElement execElement = (ExecutableElement) element;
        names.addNamesDefinedByExecutable(execElement);
        VariableElement[] vars = execElement.getLocalVariables();
        for (VariableElement var : vars) {
          // Remove local vars defined after ident.
          if (var.getNameOffset() >= ident.getOffset()) {
            names.remove(var);
          }
          // If ident is part of the initializer for a local var, remove that local var.
          if (localDef != null) {
            names.remove(localDef);
          }
        }
        decl = decl.getParent().getAncestor(Declaration.class);
        if (decl != null) {
          element = decl.getElement();
        }
      }
      if (element instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element;
        names.addNamesDefinedByTypes(allSuperTypes(classElement));
        names.addNamesDefinedByTypes(allSubtypes(classElement));
        decl = decl.getAncestor(Declaration.class);
        if (decl != null) {
          element = decl.getElement();
        }
      }
      names.addTopLevelNames();
      if (topLevelDef != null) {
        names.remove(topLevelDef);
      }
    }
    return names.getNames();
  }

  private int completionLocation() {
    return context.getSelectionOffset();
  }

  private int completionTokenOffset() {
    return completionLocation() - filter.prefix.length();
  }

  private SearchScope constructSearchScope() {
    if (libraries != null) {
      return SearchScopeFactory.createLibraryScope(libraries);
    }
    return SearchScopeFactory.createUniverseScope();
  }

  private <X extends ASTNode> List<FormalParameter> copyWithout(NodeList<X> oldList,
      final ASTNode deletion) {
    final List<FormalParameter> newList = new ArrayList<FormalParameter>(oldList.size() - 1);
    oldList.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitNode(ASTNode node) {
        if (node != deletion) {
          newList.add((FormalParameter) node);
        }
        return null;
      }
    });
    return newList;
  }

  private CompletionProposal createProposal(ProposalKind kind) {
    return factory.createCompletionProposal(kind, completionLocation() - filter.prefix.length());
  }

  private Element[] extractElementsFromSearchMatches(List<SearchMatch> matches) {
    Element[] funcs = new Element[matches.size()];
    int i = 0;
    for (SearchMatch match : matches) {
      funcs[i++] = match.getElement();
    }
    return funcs;
  }

  private boolean filterAllows(Element element) {
    return filter.match(element);
  }

  private boolean filterDisallows(Element element) {
    return !filter.match(element);
  }

  private boolean filterDisallows(String name) {
    return !filter.match(name);
  }

  private Element[] findAllFunctions() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("*", false);
    List<SearchMatch> matches = engine.searchFunctionDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private Element[] findAllPrefixes() {
    LibraryElement lib = context.getCompilationUnit().getElement().getEnclosingElement();
    return lib.getPrefixes();
  }

  private Element[] findAllTypes() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("*", false);
    List<SearchMatch> matches = engine.searchTypeDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private Element[] findAllVariables() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("*", false);
    List<SearchMatch> matches = engine.searchVariableDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private AnalysisContext getAnalysisContext() {
    return context.getCompilationUnit().getElement().getContext();
  }

  private LibraryElement getCurrentLibrary() {
    return context.getCompilationUnit().getElement().getEnclosingElement();
  }

  private ClassElement getObjectClassElement() {
    return getTypeProvider().getObjectType().getElement();
  }

  private List<LibraryElement> getSystemLibraries() {
    // TODO Get ALL system libraries, not just the ones that have been loaded already.
    AnalysisContext ac = getAnalysisContext();
    Source[] ss = ac.getLibrarySources();
    List<LibraryElement> sl = new ArrayList<LibraryElement>();
    for (Source s : ss) {
      if (s.isInSystemLibrary()) {
//        sl.add(ac.getLibraryElement(s));
      }
    }
    return sl;
  }

  private TypeProvider getTypeProvider() {
    AnalysisContext ctxt = context.getCompilationUnit().getElement().getContext();
    Source coreSource = ctxt.getSourceFactory().forUri(DartSdk.DART_CORE);
    LibraryElement coreLibrary;
    try {
      coreLibrary = ctxt.computeLibraryElement(coreSource);
    } catch (AnalysisException exception) {
      // TODO(brianwilkerson) Figure out the right thing to do if the core cannot be resolved.
      return null;
    }
    TypeProvider provider = new TypeProviderImpl(coreLibrary);
    return provider;
  }

  private boolean hasErrorBeforeCompletionLocation() {
    AnalysisError[] errors = context.getCompilationUnit().getErrors();
    if (errors == null || errors.length == 0) {
      return false;
    }
    return errors[0].getOffset() <= completionLocation();
  }

  private boolean isCompletingKeyword(Token keyword) {
    if (keyword == null) {
      return false;
    }
    int completionLoc = context.getSelectionOffset();
    if (completionLoc >= keyword.getOffset() && completionLoc <= keyword.getEnd()) {
      return true;
    }
    return false;
  }

  private boolean isCompletionAfter(int loc) {
    return loc <= completionLocation();
  }

  private boolean isCompletionBefore(int loc) {
    return completionLocation() <= loc;
  }

  private boolean isCompletionBetween(int firstLoc, int secondLoc) {
    return isCompletionAfter(firstLoc) && isCompletionBefore(secondLoc);
  }

  private boolean isUnitInLibFolder(CompilationUnitElement cu) {
    String pathString = cu.getSource().getFullName();
    if (pathString.indexOf("/lib/") == -1) {
      return false;
    }
    return true;
  }

  private LibraryElement[] librariesImportedByName(SimpleIdentifier libName) {
    ImportElement[] imps = getCurrentLibrary().getImports();
    String name = libName.getName();
    List<LibraryElement> libs = new ArrayList<LibraryElement>();
    for (ImportElement imp : imps) {
      PrefixElement prefix = imp.getPrefix();
      if (prefix != null) {
        String impName = prefix.getName();
        if (name.equals(impName)) {
          libs.add(imp.getImportedLibrary());
        }
      }
    }
    return libs.toArray(new LibraryElement[libs.size()]);
  }

  private String makeNonconflictingName(String candidate, List<String> names) {
    String possibility = candidate;
    int count = 0;
    loop : while (true) {
      String name = count == 0 ? possibility : possibility + count;
      for (String conflict : names) {
        if (name.equals(conflict)) {
          count += 1;
          continue loop;
        }
      }
      return name;
    }
  }

  private void pDynamic() {
    if (filterDisallows(C_DYNAMIC)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.VARIABLE,
        completionTokenOffset());
    prop.setCompletion(C_DYNAMIC);
    requestor.accept(prop);
  }

  private void pExecutable(ExecutableElement element, SimpleIdentifier identifier) {
    // Create a completion proposal for the element: function, method, getter, setter, constructor.
    String name = element.getName();
    if (name.isEmpty() || filterDisallows(element)) {
      return; // Simple constructors are not handled here
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    setParameterInfo(element, prop);
    prop.setCompletion(name).setReturnType(element.getType().getReturnType().getName());
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getName());
    }
    requestor.accept(prop);
  }

  private void pExecutable(TopLevelVariableElement element, SimpleIdentifier identifier) {
    // Create a completion proposal for the element: top-level variable.
    String name = element.getName();
    if (name.isEmpty() || filterDisallows(element)) {
      return; // Simple constructors are not handled here
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(name).setReturnType(element.getType().getName());
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getName());
    }
    requestor.accept(prop);
  }

  private void pFalse() {
    if (filterDisallows(C_FALSE)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.VARIABLE,
        completionTokenOffset());
    prop.setCompletion(C_FALSE);
    requestor.accept(prop);
  }

  private void pField(FieldElement element, SimpleIdentifier identifier, ClassElement classElement) {
    // Create a completion proposal for the element: field only.
    String name = element.getName();
    if (filterDisallows(element)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(name);
    Element container = element.getEnclosingElement();
    prop.setDeclaringType(container.getName());
    requestor.accept(prop);
  }

  private void pKeyword(Token keyword) {
    // This isn't as useful as it might seem. It only works in the case that completion
    // is requested on an existing recognizable keyword.
    CompletionProposal prop = factory.createCompletionProposal( // TODO: Add keyword proposal kind
        ProposalKind.LIBRARY_PREFIX,
        keyword.getOffset());
    prop.setCompletion(keyword.getLexeme());
    requestor.accept(prop);
  }

  private void pName(Element element) {
    // Create a completion proposal for the element: variable, field, class, function.
    String name = element.getName();
    if (filterDisallows(element)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(name);
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getName());
    }
    Type type = typeOf(element);
    if (type != null) {
      prop.setReturnType(type.getName());
    }
    requestor.accept(prop);
  }

  private void pName(SimpleIdentifier identifier) {
    pName(identifier.getName(), ProposalKind.VARIABLE);
  }

  private void pName(String name, ProposalKind kind) {
    if (filterDisallows(name)) {
      return;
    }
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(name);
    requestor.accept(prop);
  }

  private void pNamedConstructor(ClassElement classElement, ConstructorElement element,
      SimpleIdentifier identifier) {
    // Create a completion proposal for the named constructor.
    String name = classElement.getName();
    if (!element.getName().isEmpty()) {
      name += "." + element.getName();
    }
    if (filterDisallows(name)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    setParameterInfo(element, prop);
    prop.setCompletion(name).setReturnType(element.getType().getReturnType().getName());
    Element container = element.getEnclosingElement();
    prop.setDeclaringType(container.getName());
    requestor.accept(prop);
  }

  private void pNull() {
    if (filterDisallows(C_NULL)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.VARIABLE,
        completionTokenOffset());
    prop.setCompletion(C_NULL);
    requestor.accept(prop);
  }

  private void pParamName(String name) {
    if (filterDisallows(name)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.PARAMETER,
        completionTokenOffset());
    prop.setCompletion(name);
    requestor.accept(prop);
  }

  private ProposalKind proposalKindOf(Element element) {
    ProposalKind kind;
    switch (element.getKind()) {
      case CONSTRUCTOR:
        kind = ProposalKind.CONSTRUCTOR;
        break;
      case FUNCTION:
        kind = ProposalKind.FUNCTION;
        break;
      case METHOD:
        kind = ProposalKind.METHOD;
        break;
      case GETTER:
        kind = ProposalKind.GETTER;
        break;
      case SETTER:
        kind = ProposalKind.SETTER;
        break;
      case CLASS:
        kind = ProposalKind.CLASS;
        break;
      case FIELD:
        kind = ProposalKind.FIELD;
        break;
      case IMPORT:
        kind = ProposalKind.IMPORT;
        break;
      case PARAMETER:
        kind = ProposalKind.PARAMETER;
        break;
      case PREFIX:
        kind = ProposalKind.LIBRARY_PREFIX;
        break;
      case FUNCTION_TYPE_ALIAS:
        kind = ProposalKind.CLASS_ALIAS;
        break;
      case TYPE_VARIABLE:
        kind = ProposalKind.TYPE_VARIABLE;
        break;
      case LOCAL_VARIABLE:
      case TOP_LEVEL_VARIABLE:
        kind = ProposalKind.VARIABLE;
        break;
      default:
        throw new IllegalArgumentException();
    }
    return kind;
  }

  private void proposeNames(NameCollector names, SimpleIdentifier identifier) {
    for (List<Element> uniques : names.getNames()) {
      Element element = uniques.get(0);
      switch (element.getKind()) {
        case PARAMETER:
        case FUNCTION:
        case GETTER:
        case LOCAL_VARIABLE:
        case METHOD:
        case SETTER:
          ExecutableElement candidate = (ExecutableElement) uniques.get(0);
          pExecutable(candidate, identifier);
          break;
        case TOP_LEVEL_VARIABLE:
          TopLevelVariableElement var = (TopLevelVariableElement) uniques.get(0);
          pExecutable(var, identifier);
          break;
        case CLASS:
          pName(element);
          break;
        default:
          break;
      }
    }
  }

  private void pTrue() {
    if (filterDisallows(C_TRUE)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.VARIABLE,
        completionTokenOffset());
    prop.setCompletion(C_TRUE);
    requestor.accept(prop);
  }

  private void pVar() {
    if (filterDisallows(C_VAR)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.VARIABLE,
        completionTokenOffset());
    prop.setCompletion(C_VAR);
    requestor.accept(prop);
  }

  private void pVoid() {
    if (filterDisallows(C_VOID)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(
        ProposalKind.VARIABLE,
        completionTokenOffset());
    prop.setCompletion(C_VOID);
    requestor.accept(prop);
  }

  private void setParameterInfo(ExecutableElement cons, CompletionProposal prop) {
    List<String> params = new ArrayList<String>();
    List<String> types = new ArrayList<String>();
    boolean named = false, positional = false;
    int posCount = 0;
    for (ParameterElement param : cons.getParameters()) {
      if (!param.isSynthetic()) {
        switch (param.getParameterKind()) {
          case REQUIRED:
            posCount += 1;
            break;
          case NAMED:
            named = true;
            break;
          case POSITIONAL:
            positional = true;
            break;
        }
        params.add(param.getName());
        types.add(param.getType().toString());
      }
    }
    prop.setParameterNames(params.toArray(new String[params.size()]));
    prop.setParameterTypes(types.toArray(new String[types.size()]));
    prop.setParameterStyle(posCount, named, positional);
  }

  // Find the parent declaration of the given node and extract the name of the type it is defining.
  private SimpleIdentifier typeDeclarationName(ASTNode node) {
    ASTNode parent = node;
    while (parent != null) {
      if (parent instanceof ClassDeclaration) {
        return ((ClassDeclaration) parent).getName();
      }
      if (parent instanceof ClassTypeAlias) {
        return ((ClassTypeAlias) parent).getName();
      }
      if (parent instanceof FunctionTypeAlias) {
        return ((FunctionTypeAlias) parent).getName();
      }
      parent = parent.getParent();
    }
    return null;
  }

  private Type typeOf(Element receiver) {
    Type receiverType;
    switch (receiver.getKind()) {
      case FIELD:
      case PARAMETER:
      case LOCAL_VARIABLE:
      case TOP_LEVEL_VARIABLE: {
        VariableElement receiverElement = (VariableElement) receiver;
        receiverType = receiverElement.getType();
        break;
      }
      case CONSTRUCTOR:
      case FUNCTION:
      case METHOD:
      case GETTER:
      case SETTER: {
        ExecutableElement receiverElement = (ExecutableElement) receiver;
        FunctionType funType = receiverElement.getType();
        receiverType = funType == null ? null : funType.getReturnType();
        break;
      }
      case CLASS: {
        ClassElement receiverElement = (ClassElement) receiver;
        receiverType = receiverElement.getType();
        break;
      }
      case DYNAMIC: {
        DynamicElementImpl receiverElement = (DynamicElementImpl) receiver;
        receiverType = receiverElement.getType();
        break;
      }
      case FUNCTION_TYPE_ALIAS: {
        FunctionTypeAliasElement receiverElement = (FunctionTypeAliasElement) receiver;
        FunctionType funType = receiverElement.getType();
        receiverType = funType == null ? null : funType.getReturnType();
        break;
      }
      default: {
        receiverType = null;
        break;
      }
    }
    return receiverType;
  }

  private Type typeOf(Expression expr) {
    Type type = expr.getPropagatedType();
    if (type == null) {
      type = expr.getStaticType();
    }
    if (type == null) {
      type = DynamicTypeImpl.getInstance();
    }
    return type;
  }

  private Type typeOfContainingClass(ASTNode node) {
    ASTNode parent = node;
    while (parent != null) {
      if (parent instanceof ClassDeclaration) {
        return ((ClassDeclaration) parent).getElement().getType();
      }
      parent = parent.getParent();
    }
    return DynamicTypeImpl.getInstance();
  }
}
