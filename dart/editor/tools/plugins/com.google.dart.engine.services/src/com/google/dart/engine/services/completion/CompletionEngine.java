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
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
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
import com.google.dart.engine.ast.SwitchCase;
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
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchFilter;
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
import com.google.dart.engine.utilities.ast.ScopedNameFinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The analysis engine for code completion.
 * <p>
 * Note: During development package-private methods are used to group element-specific completion
 * utilities.
 * <p>
 * TODO: Recognize when completion is requested in the middle of a multi-character operator.
 * Re-write the AST as it would be if an identifier were present at the completion point then
 * restart the analysis.
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

  class ContainmentFilter implements SearchFilter {
    ExecutableElement containingElement;

    ContainmentFilter(ExecutableElement element) {
      containingElement = element;
    }

    @Override
    public boolean passes(SearchMatch match) {
      Element baseElement = match.getElement();
      if (containingElement == null) {
        return baseElement.getEnclosingElement() instanceof CompilationUnitElement;
      }
      return true;
    }
  }

  class NameCollector {
    private Map<String, List<Element>> uniqueNames = new HashMap<String, List<Element>>();
    private Set<Element> potentialMatches;

    public void addAll(Collection<SimpleIdentifier> values) {
      for (SimpleIdentifier id : values) {
        mergeName(id.getBestElement());
      }
    }

    public void addLocalNames(SimpleIdentifier identifier) {
      ASTNode node = identifier;
      Declaration decl;
      while ((decl = node.getAncestor(Declaration.class)) != null) {
        Element declElement = decl.getElement();
        if (declElement instanceof ExecutableElement) {
          addNamesDefinedByExecutable((ExecutableElement) declElement);
        } else {
          return;
        }
        node = decl.getParent();
      }
    }

    void addNamesDefinedByExecutable(ExecutableElement execElement) {
      mergeNames(execElement.getParameters());
      mergeNames(execElement.getLocalVariables());
      mergeNames(execElement.getFunctions());
    }

    void addNamesDefinedByHierarchy(ClassElement classElement) {
      addNamesDefinedByTypes(allSuperTypes(classElement));
      // Collect names defined by subtypes separately so they can be identified later.
      NameCollector potentialMatchCollector = new NameCollector();
      potentialMatchCollector.addNamesDefinedByTypes(allSubtypes(classElement));
      potentialMatches = new HashSet<Element>(potentialMatchCollector.uniqueNames.size());
      for (List<Element> matches : potentialMatchCollector.uniqueNames.values()) {
        for (Element match : matches) {
          mergeName(match);
          potentialMatches.add(match);
        }
      }
    }

    void addNamesDefinedByType(InterfaceType type) {
      if (inPrivateLibrary(type)) {
        return;
      }
      PropertyAccessorElement[] accessors = type.getAccessors();
      mergeNames(accessors);
      MethodElement[] methods = type.getMethods();
      mergeNames(methods);
      mergeNames(type.getElement().getTypeVariables());
      filterStaticRefs(accessors);
      filterStaticRefs(methods);
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

    boolean isPotentialMatch(Element element) {
      return potentialMatches != null && potentialMatches.contains(element);
    }

    void remove(Element element) {
      String name = element.getDisplayName();
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

    private boolean inPrivateLibrary(InterfaceType type) {
      LibraryElement lib = type.getElement().getLibrary();
      if (!(lib.getName().startsWith("_") || type.getDisplayName().startsWith("_"))) {
        return false;
      }
      return lib != getCurrentLibrary();
    }

    private void mergeName(Element element) {
      String name = element.getDisplayName();
      if (!filter.isPrivateDisallowed && Identifier.isPrivateName(name)) {
        if (!isInCurrentLibrary(element)) {
          return;
        }
      }
      List<Element> dups = uniqueNames.get(name);
      if (dups == null) {
        dups = new ArrayList<Element>();
        uniqueNames.put(name, dups);
      }
      dups.add(element);
    }

    private void mergeNames(Element[] elements) {
      for (Element element : elements) {
        mergeName(element);
      }
    }
  }

  private class Filter {
    String prefix;
    String originalPrefix;
    Pattern pattern;
    boolean isPrivateDisallowed = true;

    Filter(SimpleIdentifier ident) {
      this(ident, context.getSelectionOffset());
    }

    Filter(SimpleIdentifier ident, int loc) {
      int pos = ident.getOffset();
      int len = loc - pos;
      if (len > 0) {
        String name = ident.getName();
        if (len <= name.length()) {
          prefix = name.substring(0, len);
        } else {
          prefix = name;
        }
      } else {
        prefix = "";
      }
      if (prefix.length() >= 1) {
        isPrivateDisallowed = !Identifier.isPrivateName(prefix);
      }
      originalPrefix = prefix;
      prefix = prefix.toLowerCase();
    }

    boolean isPermitted(String name) {
      if (isPrivateDisallowed) {
        if (name.length() > 0 && Identifier.isPrivateName(name)) {
          return false;
        }
      }
      return true;
    }

    String makePattern() {
      String source = filter.originalPrefix;
      if (source == null || source.length() < 2) {
        return "*";
      }
      int index = 0;
      StringBuffer regex = new StringBuffer();
      StringBuffer pattern = new StringBuffer();
      regex.append(source.charAt(index));
      pattern.append(source.charAt(index++));
      while (index < source.length()) {
        char ch = source.charAt(index++);
        if (Character.isUpperCase(ch)) {
          pattern.append('*');
          regex.append("\\p{javaLowerCase}*");
        }
        pattern.append(ch);
        regex.append(ch);
      }
      pattern.append('*');
      regex.append("\\p{javaLowerCase}*");
      String result = pattern.toString();
      this.pattern = Pattern.compile(regex.toString(), 0);
      return result;
    }

    boolean match(Element elem) {
      return match(elem.getDisplayName());
    }

    boolean match(String name) {
      // Return true if the filter passes. Return false for private elements that should not be visible
      // in the current context.
      return isPermitted(name)
          && (name.toLowerCase().startsWith(prefix) || pattern != null
              && pattern.matcher(name).matches());
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
    public Void visitAnnotation(Annotation node) {
      return null;
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
    public Void visitExpressionFunctionBody(ExpressionFunctionBody node) {
      if (completionNode == node.getExpression()) {
        analyzeLocalName(completionNode);
      }
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
          dispatchPrefixAnalysis(node);
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
    public Void visitSwitchCase(SwitchCase node) {
      if (completionNode == node.getExpression()) {
        analyzeLocalName(completionNode);
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
        if (node.getParent() instanceof MethodInvocation) {
          // or node.getParent().accept(this); ?
          MethodInvocation invokeNode = (MethodInvocation) node.getParent();
          SimpleIdentifier methodName = invokeNode.getMethodName();
          ProposalCollector proposalRequestor = new ProposalCollector(requestor);
          try {
            requestor = proposalRequestor;
            dispatchPrefixAnalysis(invokeNode);
          } finally {
            requestor = proposalRequestor.getRequestor();
          }
          int offset = methodName.getOffset();
          int len = node.getRightParenthesis().getEnd() - offset;
          String name = methodName.getName();
          for (CompletionProposal proposal : proposalRequestor.getProposals()) {
            if (proposal.getCompletion().equals(name)) {
              pArgumentList(proposal, offset, len);
            }
          }
        } else if (node.getParent() instanceof InstanceCreationExpression) {
          InstanceCreationExpression invokeNode = (InstanceCreationExpression) node.getParent();
          ConstructorName methodName = invokeNode.getConstructorName();
          ProposalCollector proposalRequestor = new ProposalCollector(requestor);
          try {
            requestor = proposalRequestor;
            dispatchPrefixAnalysis(invokeNode);
          } finally {
            requestor = proposalRequestor.getRequestor();
          }
          int offset = methodName.getOffset();
          int len = node.getRightParenthesis().getEnd() - offset;
          for (CompletionProposal proposal : proposalRequestor.getProposals()) {
            pArgumentList(proposal, offset, len);
          }
        }
        analyzeLocalName(new Ident(node));
      }
      return null;
    }

    @Override
    public Void visitAsExpression(AsExpression node) {
      if (isCompletionAfter(node.getAsOperator().getEnd())) {
        state.isDynamicAllowed = false;
        state.isVoidAllowed = false;
        analyzeTypeName(new Ident(node), null);
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
    public Void visitConstructorName(ConstructorName node) {
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
      if (node.getExpression() != null && node.getSemicolon() != null) {
        if (isCompletionBetween(node.getExpression().getEnd(), node.getSemicolon().getOffset())) {
          operatorAccess(node.getExpression(), new Ident(node));
        }
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
    public Void visitIsExpression(IsExpression node) {
      Ident ident;
      if (node.getIsOperator().getEnd() == completionLocation()) {
        int offset = 0;
        Token isToken = node.getIsOperator();
        if (isToken != null) {
          offset = isToken.getOffset();
        }
        if (node.getExpression() instanceof PrefixedIdentifier) {
          PrefixedIdentifier prefIdent = (PrefixedIdentifier) node.getExpression();
          if (prefIdent.getLength() == 0) {
            Type type = typeOf(prefIdent.getPrefix());
            analyzePrefixedAccess(type, new Ident(node, "is", offset));
          } else {
            pKeyword(node.getIsOperator());
          }
          return null;
        } else {
          ident = new Ident(node, "is", offset);
        }
      } else {
        ident = new Ident(node);
      }
      analyzeLocalName(ident);
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocation node) {
      Token period = node.getPeriod();
      if (period != null && isCompletionAfter(period.getEnd())) {
        // { x.!y() }
        dispatchPrefixAnalysis(node);
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
    public Void visitAsExpression(AsExpression node) {
      if (node.getType() == typeName) {
        state.isDynamicAllowed = false;
        state.isVoidAllowed = false;
        analyzeTypeName(identifier, null);
      }
      return null;
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
            Element element = identifier.getBestElement();
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
        SimpleIdentifier ident;
        Token isToken = node.getIsOperator();
        if (completionLocation() == isToken.getEnd()) {
          // { is! } possible name completion
          int offset = isToken.getOffset();
          if (node.getExpression() instanceof PrefixedIdentifier) {
            PrefixedIdentifier prefIdent = (PrefixedIdentifier) node.getExpression();
            if (prefIdent.getLength() == 0) {
              Type type = typeOf(prefIdent.getPrefix());
              analyzePrefixedAccess(type, new Ident(node, "is", offset));
            } else {
              pKeyword(node.getIsOperator());
            }
            return null;
          } else {
            ident = new Ident(node, "is", offset);
          }
          analyzeLocalName(ident);
        } else {
          analyzeTypeName((SimpleIdentifier) node.getType().getName(), null);
        }
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
    VariableDeclarationList varList = (VariableDeclarationList) varDecl.getParent();
    TypeName type = varList.getType();
    if (identifier.getLength() > 0) {
      pName(identifier);
    }
    if (type == null) {
      if (varList.getKeyword() == null) {
        // Interpret as the type name of a typed variable declaration { DivE!; }
        analyzeLocalName(identifier);
      }
    } else {
      pParamName(type.getName().getName().toLowerCase());
    }
  }

  void analyzeDirectAccess(Type receiverType, SimpleIdentifier completionNode) {
    if (receiverType != null) {
      // Complete this.!y where this is absent
      Element rcvrTypeElem = receiverType.getElement();
      if (receiverType.isDynamic()) {
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
      pName(field.getDisplayName(), ProposalKind.FIELD);
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
    NameCollector names = collectIdentifiersVisibleAt(identifier);
    for (List<Element> uniques : names.getNames()) {
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
      proposeName(candidate, identifier, names);
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
      if (receiverType.isDynamic()) {
        rcvrTypeElem = getObjectClassElement();
      }
      if (rcvrTypeElem instanceof ClassElement) {
        prefixedAccess((ClassElement) rcvrTypeElem, completionNode);
      } else if (rcvrTypeElem instanceof TypeVariableElement) {
        TypeVariableElement typeVarElem = (TypeVariableElement) rcvrTypeElem;
        analyzePrefixedAccess(typeVarElem.getBound(), completionNode);
      }
    }
  }

  void analyzeReceiver(SimpleIdentifier identifier) {
    // Completion x!.y
    filter = new Filter(identifier);
    NameCollector names = collectIdentifiersVisibleAt(identifier);
    for (List<Element> uniques : names.getNames()) {
      Element candidate = uniques.get(0);
      proposeName(candidate, identifier, names);
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
      if (type.getDisplayName().equals(name)) {
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
        pExecutable(cons, identifier, false);
      }
    }
  }

  void directAccess(ClassElement classElement, SimpleIdentifier identifier) {
    filter = new Filter(identifier);
    NameCollector names = new NameCollector();
    names.addLocalNames(identifier);
    names.addNamesDefinedByHierarchy(classElement);
    names.addTopLevelNames();
    proposeNames(names, identifier);
  }

  void dispatchPrefixAnalysis(InstanceCreationExpression node) {
    ClassElement classElement = (ClassElement) typeOf(node).getElement();
    SimpleIdentifier identifier = node.getConstructorName().getName();
    identifier = (SimpleIdentifier) node.getConstructorName().getType().getName();
    if (identifier == null) {
      identifier = new Ident(node);
    }
    analyzeConstructorTypeName(identifier);
    constructorReference(classElement, identifier);
  }

  void dispatchPrefixAnalysis(MethodInvocation node) {
    // This might be a library prefix on a top-level function
    Expression expr = node.getTarget();
    if (expr instanceof SimpleIdentifier) {
      SimpleIdentifier ident = (SimpleIdentifier) expr;
      if (ident.getBestElement() instanceof PrefixElement) {
        prefixedAccess(ident, node.getMethodName());
        return;
      } else if (ident.getBestElement() instanceof ClassElement) {
        state.areInstanceReferencesProhibited = true;
        state.areStaticReferencesProhibited = false;
      } else {
        state.areInstanceReferencesProhibited = false;
        state.areStaticReferencesProhibited = true;
      }
    }
    if (expr == null) {
      analyzeLocalName(new Ident(node));
    } else {
      Type receiverType = typeOf(expr);
      analyzePrefixedAccess(receiverType, node.getMethodName());
    }
  }

  void dispatchPrefixAnalysis(PrefixedIdentifier node, SimpleIdentifier identifier) {
    SimpleIdentifier receiverName = node.getPrefix();
    Element receiver = receiverName.getBestElement();
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
        Type receiverType;
        Type propType = typeOf(receiverName);
        if (propType == null || propType.isDynamic()) {
          receiverType = typeOf(receiver);
        } else {
          Type declType = typeOf(receiver);
          if (propType.isMoreSpecificThan(declType)) {
            receiverType = propType;
          } else {
            receiverType = declType;
          }
        }
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
      String name = lib.getDisplayName();
      name = name.substring(5);
      if (filter.isPermitted(name)) {
        pName("dart:" + name, ProposalKind.IMPORT);
      }
    }
  }

  void namedConstructorReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a named constructor defined in classElement.
    if (filter == null) {
      filter = new Filter(identifier);
    }
    for (ConstructorElement cons : classElement.getConstructors()) {
      if ((state.isCompileTimeConstantRequired ? cons.isConst() : true)
          && filter.isPermitted(cons.getDisplayName())) {
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
    names.addNamesDefinedByHierarchy(classElement);
    proposeNames(names, identifier);
  }

  void prefixedAccess(SimpleIdentifier libName, SimpleIdentifier identifier) {
    if (filter == null) {
      filter = new Filter(identifier);
    }
    libraries = librariesImportedByName(libName);
    NameCollector names = new NameCollector();
    names.addTopLevelNames();
    proposeNames(names, identifier);
  }

  private InterfaceType[] allSubtypes(final ClassElement classElement) {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = SearchScopeFactory.createUniverseScope();
    SearchFilter directSubsOnly = new SearchFilter() {
      @Override
      public boolean passes(SearchMatch match) {
        Element element = match.getElement();
        if (element instanceof ClassElement) {
          ClassElement clElem = (ClassElement) element;
          while (clElem != null) {
            InterfaceType ifType = clElem.getSupertype();
            if (ifType == null) {
              return false;
            }
            clElem = ifType.getElement();
            if (clElem == classElement) {
              return true;
            }
          }
        }
        return false;
      }
    };
    List<SearchMatch> matches = engine.searchSubtypes(classElement, scope, directSubsOnly);
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

  private NameCollector collectIdentifiersVisibleAt(ASTNode ident) {
    NameCollector names = new NameCollector();
    ScopedNameFinder finder = new ScopedNameFinder(completionLocation());
    ident.accept(finder);
    names.addAll(finder.getLocals().values());
    Declaration decl = finder.getDeclaration();
    if (decl != null && decl.getParent() instanceof ClassDeclaration) {
      ClassElement classElement = ((ClassDeclaration) decl.getParent()).getElement();
      names.addNamesDefinedByHierarchy(classElement);
    }
    names.addTopLevelNames();
    return names;
  }

  private int completionLocation() {
    return context.getSelectionOffset();
  }

  private int completionTokenOffset() {
    return completionLocation() - filter.prefix.length();
  }

  private SearchScope constructSearchScope() {
    if (libraries == null) {
      libraries = currentLibraryList();
    }
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

  private LibraryElement[] currentLibraryList() {
    Set<LibraryElement> libraries = new HashSet<LibraryElement>();
    LibraryElement curLib = getCurrentLibrary();
    libraries.add(curLib);
    List<LibraryElement> queue = new LinkedList<LibraryElement>();
    Collections.addAll(queue, curLib.getImportedLibraries());
    currentLibraryLister(queue, libraries);
    return libraries.toArray(new LibraryElement[libraries.size()]);
  }

  private void currentLibraryLister(List<LibraryElement> queue, Set<LibraryElement> libraries) {
    while (!queue.isEmpty()) {
      LibraryElement sourceLib = queue.remove(0);
      libraries.add(sourceLib);
      LibraryElement[] expLibs = sourceLib.getExportedLibraries();
      for (LibraryElement lib : expLibs) {
        if (!libraries.contains(lib)) {
          queue.add(lib);
        }
      }
    }
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
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern(makeSearchPattern(), false);
    SearchFilter filter = new ContainmentFilter(null);
    List<SearchMatch> matches = engine.searchFunctionDeclarations(scope, pattern, filter);
    return extractElementsFromSearchMatches(matches);
  }

  private Element[] findAllPrefixes() {
    LibraryElement lib = context.getCompilationUnit().getElement().getEnclosingElement();
    return lib.getPrefixes();
  }

  private Element[] findAllTypes() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern(makeSearchPattern(), false);
    List<SearchMatch> matches = engine.searchTypeDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private Element[] findAllVariables() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern(makeSearchPattern(), false);
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

  private boolean isDeprecated(Element element) {
    ElementAnnotation[] annos = element.getMetadata();
    for (ElementAnnotation anno : annos) {
      if ("deprecated".equals(anno.getElement().getName())) {
        return true;
      }
    }
    return false;
  }

  private boolean isInCurrentLibrary(Element element) {
    LibraryElement libElement = getCurrentLibrary();
    return element.getLibrary() == libElement;
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
        String impName = prefix.getDisplayName();
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

  private String makeSearchPattern() {
    if (filter == null) {
      return "*";
    }
    return filter.makePattern();
  }

  private void pArgumentList(CompletionProposal proposal, int offset, int len) {
    CompletionProposal prop = createProposal(ProposalKind.ARGUMENT_LIST);
    prop.setCompletion(proposal.getCompletion()).setReturnType(proposal.getReturnType());
    prop.setParameterNames(proposal.getParameterNames());
    prop.setParameterTypes(proposal.getParameterTypes());
    prop.setParameterStyle(
        proposal.getPositionalParameterCount(),
        proposal.hasNamed(),
        proposal.hasPositional());
    prop.setReplacementLength(0).setLocation(completionLocation());
    prop.setRelevance(10);
    requestor.accept(prop);
  }

  private void pDynamic() {
    pWord(C_DYNAMIC, ProposalKind.VARIABLE);
  }

  private void pExecutable(ExecutableElement element, SimpleIdentifier identifier,
      boolean isPotentialMatch) {
    // Create a completion proposal for the element: function, method, getter, setter, constructor.
    String name = element.getDisplayName();
    if (name.isEmpty() || filterDisallows(element)) {
      return; // Simple constructors are not handled here
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setDeprecated(isDeprecated(element)).setPotentialMatch(isPotentialMatch);
    if (isPotentialMatch) {
      prop.setRelevance(0);
    }
    setParameterInfo(element, prop);
    prop.setCompletion(name).setReturnType(element.getType().getReturnType().getName());
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getDisplayName());
    }
    requestor.accept(prop);
  }

  private void pExecutable(VariableElement element, SimpleIdentifier identifier) {
    // Create a completion proposal for the element: top-level variable.
    String name = element.getDisplayName();
    if (name.isEmpty() || filterDisallows(element)) {
      return; // Simple constructors are not handled here
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setDeprecated(isDeprecated(element));
    prop.setCompletion(name);
    if (element.getType() != null) {
      prop.setReturnType(element.getType().getName());
    }
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getDisplayName());
    }
    requestor.accept(prop);
  }

  private void pFalse() {
    pWord(C_FALSE, ProposalKind.VARIABLE);
  }

  private void pField(FieldElement element, SimpleIdentifier identifier, ClassElement classElement) {
    // Create a completion proposal for the element: field only.
    String name = element.getDisplayName();
    if (filterDisallows(element)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setDeprecated(isDeprecated(element));
    prop.setCompletion(name);
    Element container = element.getEnclosingElement();
    prop.setDeclaringType(container.getDisplayName());
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
    String name = element.getDisplayName();
    if (filterDisallows(element)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(name);
    prop.setDeprecated(isDeprecated(element));
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getDisplayName());
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
    String name = classElement.getDisplayName();
    if (!element.getDisplayName().isEmpty()) {
      name += "." + element.getDisplayName();
    }
    if (filterDisallows(name)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setDeprecated(isDeprecated(element));
    setParameterInfo(element, prop);
    prop.setCompletion(name).setReturnType(element.getType().getReturnType().getName());
    Element container = element.getEnclosingElement();
    prop.setDeclaringType(container.getDisplayName());
    requestor.accept(prop);
  }

  private void pNull() {
    pWord(C_NULL, ProposalKind.VARIABLE);
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

  private void proposeName(Element element, SimpleIdentifier identifier, NameCollector names) {
    switch (element.getKind()) {
      case FUNCTION:
      case GETTER:
      case METHOD:
      case SETTER:
        ExecutableElement candidate = (ExecutableElement) element;
        pExecutable(candidate, identifier, names.isPotentialMatch(candidate));
        break;
      case LOCAL_VARIABLE:
      case PARAMETER:
      case TOP_LEVEL_VARIABLE:
        VariableElement var = (VariableElement) element;
        pExecutable(var, identifier);
        break;
      case CLASS:
        pName(element);
        break;
      default:
        break;
    }
  }

  private void proposeNames(NameCollector names, SimpleIdentifier identifier) {
    for (List<Element> uniques : names.getNames()) {
      Element element = uniques.get(0);
      proposeName(element, identifier, names);
    }
  }

  private void pTrue() {
    pWord(C_TRUE, ProposalKind.VARIABLE);
  }

  private void pVar() {
    pWord(C_VAR, ProposalKind.VARIABLE);
  }

  private void pVoid() {
    pWord(C_VOID, ProposalKind.VARIABLE);
  }

  private void pWord(String word, ProposalKind kind) {
    if (filterDisallows(word)) {
      return;
    }
    CompletionProposal prop = factory.createCompletionProposal(kind, completionTokenOffset());
    prop.setCompletion(word);
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
        params.add(param.getDisplayName());
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
      case GETTER:
        PropertyAccessorElement accessor = (PropertyAccessorElement) receiver;
        if (accessor.isSynthetic()) {
          PropertyInducingElement inducer = accessor.getVariable();
          if (inducer.getType().isDynamic()) {
            receiverType = typeSearch(inducer);
            if (receiverType != null) {
              break;
            }
          }
        }
        FunctionType accType = accessor.getType();
        receiverType = accType == null ? null : accType.getReturnType();
        break;
      case CONSTRUCTOR:
      case FUNCTION:
      case METHOD:
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
        receiverType = DynamicTypeImpl.getInstance();
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
    Type type = expr.getBestType();
    if (type.isDynamic()) {
      final Type[] result = new Type[1];
      AstNodeClassifier visitor = new AstNodeClassifier() {
        @Override
        public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
          return visitSimpleIdentifier(node.getIdentifier());
        }

        @Override
        public Void visitSimpleIdentifier(SimpleIdentifier node) {
          Element elem = node.getBestElement();
          if (elem != null && elem.getKind() == ElementKind.GETTER) {
            PropertyAccessorElement accessor = (PropertyAccessorElement) elem;
            if (accessor.isSynthetic()) {
              PropertyInducingElement var = accessor.getVariable();
              result[0] = typeSearch(var);
            }
          }
          return null;
        }
      };
      expr.accept(visitor);
      if (result[0] != null) {
        return result[0];
      }
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

  private Type typeSearch(PropertyInducingElement var) {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    Set<Type> matches = engine.searchAssignedTypes(var, scope);
    if (matches.isEmpty()) {
      return null;
    }
    Iterator<Type> iter = matches.iterator();
    Type result = iter.next();
    while (iter.hasNext()) {
      result = result.getLeastUpperBound(iter.next());
    }
    return result;
  }
}
