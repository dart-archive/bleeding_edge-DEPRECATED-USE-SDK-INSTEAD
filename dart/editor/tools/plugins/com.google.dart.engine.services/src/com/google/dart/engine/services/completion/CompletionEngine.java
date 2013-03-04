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
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.EphemeralIdentifier;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.internal.element.DynamicElementImpl;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.resolver.TypeProviderImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchListener;
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

  static class SearchCollector implements SearchListener {
    ArrayList<Element> results = new ArrayList<Element>();
    boolean isComplete = false;

    @Override
    public void matchFound(SearchMatch match) {
      Element element = match.getElement();
      results.add(element);
    }

    @Override
    public void searchComplete() {
      isComplete = true;
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

    boolean match(Element elem) {
      // Return true if the filter passes. Return false for private elements that should not be visible
      // in the current context, or for library elements that are not accessible in the context (NYI).
      String name = elem.getName();
      if (isPrivateDisallowed) {
        if (name.length() > 0 && Identifier.isPrivateName(name)) {
          return false;
        }
      }
      return name.startsWith(prefix);
    }
  }

  private class Ident extends EphemeralIdentifier {
    Ident(ASTNode parent) {
      super(parent, completionLocation());
    }
  }

  private class IdentifierCompleter extends GeneralizingASTVisitor<Void> {
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
    public Void visitMethodInvocation(MethodInvocation node) {
      if (node.getMethodName() == completionNode) {
        // { x.!y() }
        Expression expr = node.getTarget();
        Type receiverType = typeOf(expr);
        analyzePrefixedAccess(receiverType, node.getMethodName());
      } else if (node.getTarget() == completionNode) {
        // { x!.y() } -- only reached when node.getTarget() is a simple identifier. (TODO: verify)
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
        node.getIdentifier(); // TODO: verify that this case occurs
      } else {
        // { v.! }
        SimpleIdentifier receiverName = node.getPrefix();
        Element receiver = receiverName.getElement();
        switch (receiver.getKind()) {
          case PREFIX: {
            // TODO: remove this case if/when prefix resolution changes
            PrefixElement prefixElement = (PrefixElement) receiver;
            // Complete lib_prefix.name
            prefixedAccess(prefixElement, node.getIdentifier());
            break;
          }
          case IMPORT: {
            ImportElement importElement = (ImportElement) receiver;
            // Complete lib_prefix.name
            prefixedAccess(importElement, node.getIdentifier());
            break;
          }
          default: {
            Type receiverType = typeOf(receiver);
            analyzePrefixedAccess(receiverType, node.getIdentifier());
            break;
          }
        }
      }
      return null;
    }

    @Override
    public Void visitPropertyAccess(PropertyAccess node) {
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
    public Void visitVariableDeclaration(VariableDeclaration node) {
      if (node.getName() == completionNode) {
        analyzeLocalName(node.getName());
      } else if (node.getInitializer() == completionNode) {
        analyzeLocalName((SimpleIdentifier) node.getInitializer());
      }
      return null;
    }
  }

  private class TerminalNodeCompleter extends GeneralizingASTVisitor<Void> {

    @Override
    public Void visitClassDeclaration(ClassDeclaration node) {
      if (isCompletingKeyword(node.getClassKeyword())) {
        pKeyword(node.getClassKeyword()); // Other keywords are legal but not handled here.
        return null;
      }
      if (isCompletingKeyword(node.getAbstractKeyword())) {
        pKeyword(node.getAbstractKeyword());
        return null;
      }
      // TODO { abstract ! class ! A ! extends B implements C, D ! {}}
      return null; // visitCompilationUnitMember(node);
    }

    @Override
    public Void visitClassTypeAlias(ClassTypeAlias node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
        return null;
      }
      // TODO { typedef ! A ! = ! B ! with C, D !; }
      return null; // TODO visitTypeAlias(node);
    }

    @Override
    public Void visitExtendsClause(ExtendsClause node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
        return null;
      } else if (node.getSuperclass() == null) {
        // { X extends ! }
        analyzeTypeName(new Ident(node), false, typeDeclarationName(node));
        return null;
      } else {
        // { X extends ! Y }
        analyzeTypeName(new Ident(node), false, typeDeclarationName(node));
        return null;
      }
    }

    @Override
    public Void visitImplementsClause(ImplementsClause node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
        return null;
      } else if (node.getInterfaces().isEmpty()) {
        // { X implements ! }
        analyzeTypeName(new Ident(node), false, typeDeclarationName(node));
        return null;
      } else {
        // { X implements ! Y }
        analyzeTypeName(new Ident(node), false, typeDeclarationName(node));
        return null;
      }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocation node) {
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
    public Void visitWithClause(WithClause node) {
      if (isCompletingKeyword(node.getWithKeyword())) {
        pKeyword(node.getWithKeyword());
        return null;
      } else if (node.getMixinTypes().isEmpty()) {
        // { X with ! }
        analyzeTypeName(new Ident(node), true, typeDeclarationName(node));
        return null;
      } else {
        // { X with ! Y }
        analyzeTypeName(new Ident(node), true, typeDeclarationName(node));
        return null;
      }
    }
  }

  private class TypeNameCompleter extends GeneralizingASTVisitor<Void> {
    SimpleIdentifier identifier;
    TypeName typeName;

    TypeNameCompleter(SimpleIdentifier identifier, TypeName typeName) {
      this.identifier = identifier;
      this.typeName = typeName;
    }

    @Override
    public Void visitClassTypeAlias(ClassTypeAlias node) {
      analyzeTypeName(identifier, false, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitExtendsClause(ExtendsClause node) {
      analyzeTypeName(identifier, false, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitImplementsClause(ImplementsClause node) {
      analyzeTypeName(identifier, false, typeDeclarationName(node));
      return null;
    }

    @Override
    public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
      analyzeTypeName(identifier, false, null);
      return null;
    }

    @Override
    public Void visitVariableDeclarationList(VariableDeclarationList node) {
      analyzeLocalName(identifier);
      return null;
    }

    @Override
    public Void visitWithClause(WithClause node) {
      analyzeTypeName(identifier, true, typeDeclarationName(node));
      return null;
    }

  }

  private CompletionRequestor requestor;
  private CompletionFactory factory;
  private AssistContext context;
  private Filter filter;

  public CompletionEngine(CompletionRequestor requestor, CompletionFactory factory) {
    this.requestor = requestor;
    this.factory = factory;
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
      TerminalNodeCompleter visitor = new TerminalNodeCompleter();
      completionNode.accept(visitor);
    }
    requestor.endReporting();
  }

  void analyzeLocalName(SimpleIdentifier identifier) {
    // Completion x!
    filter = new Filter(identifier);
    Map<String, List<Element>> uniqueNames = collectIdentifiersVisibleAt(identifier);
    for (List<Element> uniques : uniqueNames.values()) {
      Element candidate = uniques.get(0);
      pName(candidate, identifier);
    }
  }

  void analyzePrefixedAccess(Type receiverType, SimpleIdentifier completionNode) {
    if (receiverType != null) {
      // Complete x.!y
      Element rcvrTypeElem = receiverType.getElement();
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
    Map<String, List<Element>> uniqueNames = collectIdentifiersVisibleAt(identifier);
    for (List<Element> uniques : uniqueNames.values()) {
      Element candidate = uniques.get(0);
      pName(candidate, identifier);
    }
  }

  void analyzeTypeName(SimpleIdentifier identifier, boolean isMixin, SimpleIdentifier nameIdent) {
    filter = new Filter(identifier);
    String name = nameIdent == null ? "" : nameIdent.getName();
    Element[] types = findAllTypes();
    for (Element type : types) {
      if (isMixin) {
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
      pName(type, identifier);
    }
  }

  void constructorReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a constructor defined in classElement.
    filter = new Filter(identifier);
    for (ConstructorElement cons : classElement.getConstructors()) {
      if (filterAllows(cons)) {
        pExecutable(cons, identifier, classElement);
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

  void prefixedAccess(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to field or method in classElement.
    filter = new Filter(identifier);
    InterfaceType[] allTypes = allTypes(classElement);
    Map<String, List<ExecutableElement>> uniqueNames = new HashMap<String, List<ExecutableElement>>();
    for (InterfaceType type : allTypes) {
      mergeNames(uniqueNames, type.getElement().getAccessors());
      mergeNames(uniqueNames, type.getElement().getMethods());
    }
    allTypes = allSubtypes(classElement);
    for (InterfaceType type : allTypes) {
      mergeNames(uniqueNames, type.getElement().getAccessors());
      mergeNames(uniqueNames, type.getElement().getMethods());
    }
    for (List<ExecutableElement> uniques : uniqueNames.values()) {
      ExecutableElement candidate = uniques.get(0);
      pExecutable(candidate, identifier, classElement);
    }
  }

  void prefixedAccess(ImportElement libElement, SimpleIdentifier identifier) {
    // TODO: Complete identifier when it refers to a member defined in the libraryElement.
    filter = new Filter(identifier);
  }

  void prefixedAccess(PrefixElement libElement, SimpleIdentifier identifier) {
    // TODO: Complete identifier when it refers to a member defined in the libraryElement, or remove.
    filter = new Filter(identifier);
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

  private InterfaceType[] allTypes(ClassElement classElement) {
    InterfaceType[] supertypes = classElement.getAllSupertypes();
    InterfaceType[] allTypes = new InterfaceType[supertypes.length + 1];
    allTypes[0] = classElement.getType();
    System.arraycopy(supertypes, 0, allTypes, 1, supertypes.length);
    return allTypes;
  }

  private Map<String, List<Element>> collectIdentifiersVisibleAt(ASTNode ident) {
    Map<String, List<Element>> uniqueNames = new HashMap<String, List<Element>>();
    Declaration decl = ident.getAncestor(Declaration.class);
    if (decl != null) {
      Element element = decl.getElement();
      Element localDef = null;
      if (element instanceof LocalVariableElement) { // TODO: Also applies to local functions...
        decl = decl.getParent().getAncestor(Declaration.class);
        localDef = element;
        element = decl.getElement();
      }
      if (element instanceof ExecutableElement) {
        ExecutableElement execElement = (ExecutableElement) element;
        ParameterElement[] params = execElement.getParameters();
        mergeNames(uniqueNames, params);
        VariableElement[] vars = execElement.getLocalVariables();
        mergeNames(uniqueNames, vars);
        for (VariableElement var : vars) {
          // Remove local vars defined after ident.
          if (var.getNameOffset() >= ident.getOffset()) {
            mergedNameRemove(uniqueNames, var);
          }
          // If ident is part of the initializer for a local var, remove that local var.
          if (localDef != null) {
            mergedNameRemove(uniqueNames, localDef);
          }
        }
        decl = decl.getParent().getAncestor(Declaration.class);
        if (decl != null) {
          element = decl.getElement();
        }
      }
      if (element instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element;
        // TODO: inherited fields
        mergeNames(uniqueNames, classElement.getAccessors());
        decl = decl.getAncestor(Declaration.class);
        if (decl != null) {
          element = decl.getElement();
        }
      }
      mergeNames(uniqueNames, findAllTypes());
      mergeNames(uniqueNames, findAllFunctions());
      mergeNames(uniqueNames, findAllVariables());
    }
    return uniqueNames;
  }

  private int completionLocation() {
    return context.getSelectionOffset();
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

  private Element[] findAllFunctions() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = SearchScopeFactory.createUniverseScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("*", false);
    List<SearchMatch> matches = engine.searchFunctionDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private Element[] findAllTypes() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = SearchScopeFactory.createUniverseScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("*", false);
    List<SearchMatch> matches = engine.searchTypeDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private Element[] findAllVariables() {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = SearchScopeFactory.createUniverseScope();
    SearchPattern pattern = SearchPatternFactory.createWildcardPattern("*", false);
    List<SearchMatch> matches = engine.searchVariableDeclarations(scope, pattern, null);
    return extractElementsFromSearchMatches(matches);
  }

  private ClassElement getObjectClassElement() {
    return getTypeProvider().getObjectType().getElement();
  }

  private TypeProvider getTypeProvider() {
    AnalysisContext ctxt = context.getCompilationUnit().getElement().getContext();
    Source coreSource = ctxt.getSourceFactory().forUri(DartSdk.DART_CORE);
    LibraryElement coreLibrary = ctxt.getLibraryElement(coreSource);
    TypeProvider provider = new TypeProviderImpl(coreLibrary);
    return provider;
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

  private <X extends Element> void mergedNameRemove(Map<String, List<X>> uniqueNames, X element) {
    String name = element.getName();
    List<X> list = uniqueNames.get(name);
    if (list == null) {
      return;
    }
    list.remove(element);
    if (list.isEmpty()) {
      uniqueNames.remove(name);
    }
  }

  private <X extends Element> void mergeNames(Map<String, List<X>> uniqueNames, X[] elements) {
    for (X element : elements) {
      String name = element.getName();
      List<X> dups = uniqueNames.get(name);
      if (dups == null) {
        dups = new ArrayList<X>();
        uniqueNames.put(name, dups);
      }
      dups.add(element);
    }
  }

  private void pExecutable(ExecutableElement element, SimpleIdentifier identifier,
      ClassElement classElement) {
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
    if (container != null) { // TODO: may be null for functions ??
      prop.setDeclaringType(container.getName());
    }
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
    if (container != null) { // TODO: never null ??
      prop.setDeclaringType(container.getName());
    }
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

  private void pName(Element element, SimpleIdentifier identifier) {
    // Create a completion proposal for the element: variable, field, class, function.
    String name = element.getName();
    if (filterDisallows(element)) {
      return;
    }
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(name);
    Element container = element.getEnclosingElement();
    if (container != null) { // TODO: may be null for functions ??
      prop.setDeclaringType(container.getName());
    }
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
      case TYPE_ALIAS:
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
        types.add(param.getType().getName());
      }
    }
    prop.setParameterNames(params.toArray(new String[params.size()]));
    prop.setParameterTypes(types.toArray(new String[types.size()]));
    prop.setParameterStyle(posCount, named, positional);
  }

  private SimpleIdentifier typeDeclarationName(ASTNode node) {
    ClassDeclaration classDecl = node.getAncestor(ClassDeclaration.class);
    if (classDecl != null) {
      return classDecl.getName();
    }
    ClassTypeAlias classType = node.getAncestor(ClassTypeAlias.class);
    if (classType != null) {
      return classType.getName();
    }
    FunctionTypeAlias funcType = node.getAncestor(FunctionTypeAlias.class);
    if (funcType != null) {
      return funcType.getName();
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
        receiverType = funType.getReturnType();
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
      case TYPE_ALIAS: {
        TypeAliasElement receiverElement = (TypeAliasElement) receiver;
        FunctionType funType = receiverElement.getType();
        receiverType = funType.getReturnType();
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
}
