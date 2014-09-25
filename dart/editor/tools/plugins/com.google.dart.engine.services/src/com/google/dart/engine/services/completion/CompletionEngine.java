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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AssertStatement;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Declaration;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NamespaceDirective;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TopLevelVariableElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.SdkLibrary;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.search.SearchScopeFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.internal.correction.CorrectionUtils;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.type.UnionType;
import com.google.dart.engine.utilities.ast.ScopedNameFinder;
import com.google.dart.engine.utilities.dart.ParameterKind;
import com.google.dart.engine.utilities.translation.DartBlockBody;
import com.google.dart.engine.utilities.translation.DartOmit;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  abstract class AstNodeClassifier extends GeneralizingAstVisitor<Void> {
    @Override
    public Void visitNode(AstNode node) {
      return null;
    }
  }

  class CommentReferenceCompleter extends AstNodeClassifier {
    private final SimpleIdentifier identifier;
    private final NameCollector names;
    private final Set<Element> enclosingElements = Sets.newHashSet();

    public CommentReferenceCompleter(SimpleIdentifier identifier) {
      this.identifier = identifier;
      filter = createFilter(identifier);
      names = collectTopLevelElementVisibleAt(identifier);
    }

    @Override
    public Void visitClassDeclaration(ClassDeclaration node) {
      ClassElement classElement = node.getElement();
      names.addNamesDefinedByHierarchy(classElement, false);
      enclosingElements.add(classElement);
      return null;
    }

    @Override
    public Void visitComment(Comment node) {
      node.getParent().accept(this);
      // propose names
      for (Element element : names.getUniqueElements()) {
        CompletionProposal proposal = createProposal(element, identifier);
        if (proposal != null) {
          // we don't want to add arguments, just names
          if (element instanceof MethodElement || element instanceof FunctionElement) {
            proposal.setKind(ProposalKind.METHOD_NAME);
          }
          // elevate priority for local elements
          if (enclosingElements.contains(element.getEnclosingElement())) {
            proposal.setRelevance(CompletionProposal.RELEVANCE_HIGH);
          }
          // propose
          requestor.accept(proposal);
        }
      }
      // done
      return null;
    }

    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node) {
      visitExecutableDeclaration(node);
      // pass through
      return node.getParent().accept(this);
    }

    @Override
    public Void visitFunctionDeclaration(FunctionDeclaration node) {
      visitExecutableDeclaration(node);
      return null;
    }

    @Override
    public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
      FunctionTypeAliasElement element = node.getElement();
      names.mergeNames(element.getParameters());
      enclosingElements.add(element);
      return null;
    }

    @Override
    public Void visitMethodDeclaration(MethodDeclaration node) {
      visitExecutableDeclaration(node);
      // pass through
      return node.getParent().accept(this);
    }

    private void visitExecutableDeclaration(Declaration node) {
      ExecutableElement element = (ExecutableElement) node.getElement();
      names.mergeNames(element.getParameters());
      enclosingElements.add(element);
    }
  }

//  class ContainmentFilter implements SearchFilter {
//    ExecutableElement containingElement;
//
//    ContainmentFilter(ExecutableElement element) {
//      containingElement = element;
//    }
//
//    @Override
//    public boolean passes(SearchMatch match) {
//      Element baseElement = match.getElement();
//      if (containingElement == null) {
//        return baseElement.getEnclosingElement() instanceof CompilationUnitElement;
//      }
//      return true;
//    }
//  }

  class NameCollector {
    private Map<String, List<Element>> uniqueNames = new HashMap<String, List<Element>>();

    private Set<Element> potentialMatches;

    public NameCollector() {
    }

    public void addAll(Collection<SimpleIdentifier> values) {
      for (SimpleIdentifier id : values) {
        mergeName(id.getBestElement());
      }
    }

    public void addLocalNames(SimpleIdentifier identifier) {
      AstNode node = identifier;
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

    void addNamesDefinedByHierarchy(ClassElement classElement, boolean forSuper) {
      addNamesDefinedByTypeHierarchy(classElement.getType(), forSuper);
    }

    void addNamesDefinedByType(Type type) {
      mergeNames(namesDefinedByType(type));
    }

    void addNamesDefinedByTypeHierarchy(Type type, boolean forSuper) {
      mergeNames(namesDefinedByTypeHierarchy(type, forSuper));
      // Collect names defined by subtypes separately so they can be identified later.
      NameCollector potentialMatchCollector = createNameCollector();
      potentialMatchCollector.mergeNames(potentialNamesDefinedByTypeHierarchy(type));
      potentialMatches = new HashSet<Element>(potentialMatchCollector.uniqueNames.size());
      for (List<Element> matches : potentialMatchCollector.uniqueNames.values()) {
        for (Element match : matches) {
          mergeName(match);
          potentialMatches.add(match);
        }
      }
    }

    void addTopLevelNames(ImportElement[] imports, TopLevelNamesKind topKind) {
      for (ImportElement imp : imports) {
        Collection<Element> elementsCollection = CorrectionUtils.getImportNamespace(imp).values();
        List<Element> elements = Lists.newArrayList(elementsCollection);
        addTopLevelNames(elements);
      }
    }

    void addTopLevelNames(LibraryElement library, TopLevelNamesKind topKind) {
      List<Element> elements = findTopLevelElements(library, topKind);
      addTopLevelNames(elements);
    }

    void addTopLevelNames(LibraryElement[] libraries, TopLevelNamesKind topKind) {
      for (LibraryElement library : libraries) {
        addTopLevelNames(library, topKind);
      }
    }

    Collection<List<Element>> getNames() {
      return uniqueNames.values();
    }

    Collection<Element> getUniqueElements() {
      List<Element> uniqueElements = Lists.newArrayList();
      for (List<Element> uniques : uniqueNames.values()) {
        Element element = uniques.get(0);
        uniqueElements.add(element);
      }
      return uniqueElements;
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

    private void addTopLevelNames(List<Element> elements) {
      mergeNames(findAllTypes(elements));
      if (!state.areClassesRequired) {
        mergeNames(findAllNotTypes(elements));
        mergeNames(findAllPrefixes());
      }
    }

    private Set<Element> filterStaticRefs(ExecutableElement[] elements) {
      Set<Element> filteredElements = new HashSet<Element>();
      for (ExecutableElement execElem : elements) {
        if (!(state.areInstanceReferencesProhibited && !execElem.isStatic()
            || state.areStaticReferencesProhibited && execElem.isStatic()
            || !state.areOperatorsAllowed && execElem.isOperator() || state.areMethodsProhibited
            && !execElem.isOperator())) {
          filteredElements.add(execElem);
        }
      }
      return filteredElements;
    }

    private boolean inPrivateLibrary(InterfaceType type) {
      LibraryElement lib = type.getElement().getLibrary();
      if (!lib.getName().startsWith("_")) {
        return false;
      }
      // allow completion in the same library
      if (lib == getCurrentLibrary()) {
        return false;
      }
      // eliminate types defined in private libraries
      return true;
    }

    /**
     * Return the set of elements whose names occur in sets in {@code elementSets}.
     * 
     * @param elementSets
     * @return
     */
    private Set<Element> intersection(Collection<Set<Element>> elementSets) {
      if (elementSets.isEmpty()) {
        return Collections.emptySet();
      }

      Iterator<Set<Element>> i = elementSets.iterator();
      Set<String> commonNames = namesOfElements(i.next());
      // Compute names common to all element collections.
      while (i.hasNext()) {
        // Intersection.
        commonNames.retainAll(namesOfElements(i.next()));
      }
      // Compute elements with common names.
      Set<Element> elements = new HashSet<Element>();
      for (Collection<Element> es : elementSets) {
        for (Element e : es) {
          if (commonNames.contains(e.getName())) {
            elements.add(e);
          }
        }
      }
      return elements;
    }

    private void mergeName(Element element) {
      if (element == null) {
        return;
      }
      // ignore private
      String name = element.getDisplayName();
      if (Identifier.isPrivateName(name)) {
        if (!isInCurrentLibrary(element)) {
          return;
        }
      }
      // add to other Element(s) with such name
      List<Element> dups = uniqueNames.get(name);
      if (dups == null) {
        dups = new ArrayList<Element>();
        uniqueNames.put(name, dups);
      }
      dups.add(element);
    }

    private void mergeNames(Collection<Element> elements) {
      for (Element element : elements) {
        mergeName(element);
      }
    }

    private void mergeNames(Element[] elements) {
      mergeNames(Arrays.asList(elements));
    }

    private Set<Element> namesDefinedByInterfaceType(InterfaceType type) {
      if (inPrivateLibrary(type)) {
        return Collections.emptySet();
      }
      Set<Element> elements = new HashSet<Element>();
      PropertyAccessorElement[] accessors = type.getAccessors();
      elements.addAll(filterStaticRefs(accessors));
      MethodElement[] methods = type.getMethods();
      elements.addAll(filterStaticRefs(methods));
      elements.addAll(Arrays.asList(type.getElement().getTypeParameters()));
      return elements;
    }

    private Set<Element> namesDefinedByInterfaceTypeHierarchy(InterfaceType type, boolean forSuper) {
      InterfaceType[] superTypes = type.getElement().getAllSupertypes();
      if (!forSuper) {
        superTypes = ArrayUtils.add(superTypes, 0, type);
      }
      return namesDefinedByInterfaceTypes(superTypes);
    }

    private Set<Element> namesDefinedByInterfaceTypes(InterfaceType[] types) {
      Set<Element> elements = new HashSet<Element>();
      for (InterfaceType type : types) {
        elements.addAll(namesDefinedByType(type));
      }
      return elements;
    }

    // The functions [namesDefinedByType], [namesDefinedByTypeHierarchy],
    // and [potentialNamesDefinedByTypeHierarchy] all have the same
    // structure, but they are not easy to combine in Java w/o higher
    // order functions ([union], [intersection], [*namesDefinedByInterfaceType*]).
    private Set<Element> namesDefinedByType(Type type) {
      if (type instanceof InterfaceType) {
        return namesDefinedByInterfaceType((InterfaceType) type);
      } else if (type instanceof UnionType) {
        List<Set<Element>> nameSets = new ArrayList<Set<Element>>();
        for (Type t : ((UnionType) type).getElements()) {
          nameSets.add(namesDefinedByType(t));
        }
        // For strict union types a field/method is only defined on the
        // union if it's defined on *all* member types. For non-strict union types
        // a field/method is defined if it's defined on *any* member type.
        if (AnalysisEngine.getInstance().getStrictUnionTypes()) {
          // TODO(collinsn): fix the case where multiple members define the same
          // name with different types.
          return intersection(nameSets);
        } else {
          // TODO(collinsn): this doesn't quite do the right thing, since
          // other code in this class uniquifies this list by name. See usage
          // of the [uniqueNames] field.
          return union(nameSets);
        }
      } else {
        // Or should we just raise an exception?
        AnalysisEngine.getInstance().getLogger().logError(
            "Unexpected type in [NameCollector.namesDefinedByType]: " + type);
        return Collections.emptySet();
      }
    }

    // See [namesDefinedByType].
    private Set<Element> namesDefinedByTypeHierarchy(Type type, boolean forSuper) {
      if (type instanceof InterfaceType) {
        return namesDefinedByInterfaceTypeHierarchy((InterfaceType) type, forSuper);
      } else if (type instanceof UnionType) {
        List<Set<Element>> nameSets = new ArrayList<Set<Element>>();
        for (Type t : ((UnionType) type).getElements()) {
          nameSets.add(namesDefinedByTypeHierarchy(t, forSuper));
        }
        if (AnalysisEngine.getInstance().getStrictUnionTypes()) {
          return intersection(nameSets);
        } else {
          return union(nameSets);
        }
      } else {
        AnalysisEngine.getInstance().getLogger().logError(
            "Unexpected type in [NameCollector.namesDefinedByTypeHierarchy]: " + type);
        return Collections.emptySet();
      }
    }

    private Set<String> namesOfElements(Collection<Element> elements) {
      Set<String> names = new HashSet<String>();
      for (Element e : elements) {
        names.add(e.getName());
      }
      return names;
    }

    private Set<Element> potentialNamesDefinedByInterfaceTypeHierarchy(InterfaceType type) {
      if (!type.isObject()) {
        return namesDefinedByInterfaceTypes(allSubtypes(type.getElement()));
      } else {
        return Collections.emptySet();
      }
    }

    // See [namesDefinedByType].
    //
    // The use of [union] and [intersection] here is dual to the usage in
    // the related methods. The point is that marking a method as potential
    // is more conservative, so e.g. we mark a method as potential on a
    // union type if it's potential on *any* member type.
    private Set<Element> potentialNamesDefinedByTypeHierarchy(Type type) {
      if (type instanceof InterfaceType) {
        return potentialNamesDefinedByInterfaceTypeHierarchy((InterfaceType) type);
      } else if (type instanceof UnionType) {
        List<Set<Element>> nameSets = new ArrayList<Set<Element>>();
        for (Type t : ((UnionType) type).getElements()) {
          nameSets.add(potentialNamesDefinedByTypeHierarchy(t));
        }
        if (AnalysisEngine.getInstance().getStrictUnionTypes()) {
          return union(nameSets);
        } else {
          return intersection(nameSets);
        }
      } else {
        AnalysisEngine.getInstance().getLogger().logError(
            "Unexpected type in [NameCollector.potentialNamesDefinedByTypeHierarchy]: " + type);
        return Collections.emptySet();
      }
    }

    private Set<Element> union(Collection<Set<Element>> elementSets) {
      Set<Element> elements = new HashSet<Element>();
      for (Set<Element> es : elementSets) {
        elements.addAll(es);
      }
      return elements;
    }
  }

  enum TopLevelNamesKind {
    DECLARED_AND_IMPORTS,
    DECLARED_AND_EXPORTS
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
      if (completionNode instanceof SimpleIdentifier) {
        analyzeAnnotationName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitArgumentList(ArgumentList node) {
      if (completionNode instanceof SimpleIdentifier) {
        if (isCompletionBetween(
            node.getLeftParenthesis().getEnd(),
            node.getRightParenthesis().getOffset())) {
          analyzeLocalName(completionNode);
          analyzePositionalArgument(node, completionNode);
          analyzeNamedParameter(node, completionNode);
        }
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
    public Void visitCombinator(Combinator node) {
      proposeCombinator(node, completionNode);
      return null;
    }

    @Override
    public Void visitCommentReference(CommentReference node) {
      AstNode comment = node.getParent();
      CommentReferenceCompleter visitor = new CommentReferenceCompleter(completionNode);
      return comment.accept(visitor);
    }

    @Override
    public Void visitConstructorDeclaration(ConstructorDeclaration node) {
      if (node.getReturnType() == completionNode) {
        filter = createFilter(completionNode);
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
        ident = createIdent(node);
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
        ident = createIdent(node);
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
    public Void visitLibraryIdentifier(LibraryIdentifier node) {
      // Library identifiers are always unique, so don't complete them.
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
        Expression expr = node.getRealTarget();
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
    public Void visitParenthesizedExpression(ParenthesizedExpression node) {
      // Incomplete closure: foo((Str!)); We check if "()" is argument for function typed parameter.
      if (node.getParent() instanceof ArgumentList) {
        ParameterElement parameterElement = node.getBestParameterElement();
        if (parameterElement != null && parameterElement.getType() instanceof FunctionType) {
          Ident ident = createIdent(completionNode);
          analyzeTypeName(completionNode, ident);
        }
      }
      return super.visitParenthesizedExpression(node);
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
        analyzePrefixedAccess(node.getRealTarget(), node.getPropertyName());
      }
      return null;
    }

    @Override
    public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
      // { A.Fac() : this.!b(); }
      if (node.getConstructorName() == completionNode) {
        ClassElement classElement = node.getStaticElement().getEnclosingElement();
        constructorReference(classElement, node.getConstructorName());
      }
      return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement node) {
      if (completionNode instanceof SimpleIdentifier) {
        analyzeLocalName(completionNode);
      }
      return null;
    }

    @Override
    public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
      if (node.getIdentifier() == completionNode) {
        if (node.getKeyword() == null && node.getType() == null) {
          Ident ident = createIdent(node);
          analyzeTypeName(node.getIdentifier(), ident);
        }
      }
      return null;
    }

    @Override
    public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
      analyzeSuperConstructorInvocation(node);
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
      AstNode parent = node.getParent();
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
    public Void visitNamespaceDirective(NamespaceDirective node) {
      if (completionNode == node.getUri()) {
        namespaceReference(node, completionNode);
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
            if (Objects.equal(proposal.getElement(), invokeNode.getStaticElement())) {
              pArgumentList(proposal, offset, len);
            }
          }
        } else if (node.getParent() instanceof Annotation) {
          Annotation annotation = (Annotation) node.getParent();
          Element annotationElement = annotation.getElement();
          if (annotationElement instanceof ConstructorElement) {
            ConstructorElement constructorElement = (ConstructorElement) annotationElement;
            // we don't need any filter
            filter = new Filter("", -1, 0);
            // fill parameters for "pArgumentList"
            CompletionProposal prop = createProposal(constructorElement);
            setParameterInfo(constructorElement.getType(), prop);
            prop.setCompletion(constructorElement.getEnclosingElement().getName());
            // propose the whole parameters list
            pArgumentList(prop, 0, 0);
          }
        }
      }
      if (isCompletionBetween(
          node.getLeftParenthesis().getEnd(),
          node.getRightParenthesis().getOffset())) {
        Ident ident = createIdent(node);
        analyzeLocalName(ident);
        analyzePositionalArgument(node, ident);
        analyzeNamedParameter(node, ident);
      }
      return null;
    }

    @Override
    public Void visitAsExpression(AsExpression node) {
      if (isCompletionAfter(node.getAsOperator().getEnd())) {
        state.isDynamicAllowed = false;
        state.isVoidAllowed = false;
        analyzeTypeName(createIdent(node), null);
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
        analyzeLocalName(createIdent(node));
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
              analyzeLocalName(createIdent(node));
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
        operatorAccess(node.getCondition(), createIdent(node));
      }
      return null;
    }

    @Override
    public Void visitDoubleLiteral(DoubleLiteral node) {
      return null;
    }

    @Override
    public Void visitExportDirective(ExportDirective node) {
      visitNamespaceDirective(node);
      return null;
    }

    @Override
    public Void visitExpression(Expression node) {
      analyzeLocalName(createIdent(node));
      return null;
    }

    @Override
    public Void visitExpressionFunctionBody(ExpressionFunctionBody node) {
      if (node.getExpression() != null && node.getSemicolon() != null) {
        if (isCompletionBetween(node.getExpression().getEnd(), node.getSemicolon().getOffset())) {
          operatorAccess(node.getExpression(), createIdent(node));
        }
      }
      return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement node) {
      analyzeLocalName(createIdent(node));
      return null;
    }

    @Override
    public Void visitExtendsClause(ExtendsClause node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (node.getSuperclass() == null) {
        // { X extends ! }
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
      } else {
        // { X extends ! Y }
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
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
            Ident ident = createIdent(node);
            analyzeTypeName(ident, ident);
          }
        } else {
          Ident ident = createIdent(node);
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
        operatorAccess(node.getCondition(), createIdent(node));
      }
      return null;
    }

    @Override
    public Void visitImplementsClause(ImplementsClause node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
      } else if (node.getInterfaces().isEmpty()) {
        // { X implements ! }
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
      } else {
        // { X implements ! Y }
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
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
        Ident ident = createIdent(node);
        analyzeConstructorTypeName(ident);
      }
      return null;
    }

    @Override
    public Void visitIsExpression(IsExpression node) {
      Ident ident;
      Token isToken = node.getIsOperator();
      int isTokenEnd = isToken.getEnd();
      if (isTokenEnd == completionLocation()) {
        Expression expression = node.getExpression();
        int offset = isToken.getOffset();
        // { target.is! } possible name completion, parsed as "target.{synthetic} is!"
        if (expression instanceof PrefixedIdentifier) {
          PrefixedIdentifier prefIdent = (PrefixedIdentifier) expression;
          if (prefIdent.getIdentifier().isSynthetic()) {
            analyzePrefixedAccess(prefIdent.getPrefix(), new Ident(node, "is", offset));
          } else {
            pKeyword(isToken);
          }
          return null;
        }
        // { expr is! }
        if (!isSyntheticIdentifier(expression)) {
          pKeyword(node.getIsOperator());
          return null;
        }
        // { is! } possible name completion
        ident = new Ident(node, "is", offset);
      } else if (isCompletionAfter(isTokenEnd)) {
        state.isDynamicAllowed = false;
        state.isVoidAllowed = false;
        analyzeTypeName(createIdent(node), null);
        return null;
      } else {
        ident = createIdent(node);
      }
      analyzeLocalName(ident);
      return null;
    }

    @Override
    public Void visitLibraryIdentifier(LibraryIdentifier node) {
      // Library identifiers are always unique, so don't complete them.
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
    public Void visitNamespaceDirective(NamespaceDirective node) {
      StringLiteral uri = node.getUri();
      if (uri != null && uri.isSynthetic()
          && node.getKeyword().getEnd() <= context.getSelectionOffset()) {
        uri.accept(this);
      }
      return super.visitNamespaceDirective(node);
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
      Expression target = node.getRealTarget();
      // The "1 + str.!.length" is parsed as "(1 + str).!.length",
      // but actually user wants "1 + (str.!).length".
      // So, if completion inside of period-period ".!." then it is not really a cascade completion.
      Token operator = node.getOperator();
      if (operator.getType() == TokenType.PERIOD_PERIOD) {
        int completionLocation = completionLocation();
        if (completionLocation > operator.getOffset() && completionLocation < operator.getEnd()) {
          while (target instanceof BinaryExpression) {
            target = ((BinaryExpression) target).getRightOperand();
          }
        }
      }
      // do prefixed completion
      analyzePrefixedAccess(target, node.getPropertyName());
      return null;
    }

    @Override
    public Void visitReturnStatement(ReturnStatement node) {
      if (isCompletingKeyword(node.getKeyword())) {
        pKeyword(node.getKeyword());
        return null;
      }
      Expression expression = node.getExpression();
      // return !
      if (expression instanceof SimpleIdentifier) {
        SimpleIdentifier identifier = (SimpleIdentifier) expression;
        analyzeLocalName(identifier);
        return null;
      }
      // return expression ! ;
      Token semicolon = node.getSemicolon();
      if (expression != null && semicolon != null
          && isCompletionBetween(expression.getEnd(), semicolon.getOffset())) {
        operatorAccess(expression, createIdent(node));
        return null;
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
      AstNode parent = node.getParent();
      if (parent != null) {
        IdentifierCompleter visitor = new IdentifierCompleter(node);
        return parent.accept(visitor);
      }
      return null;
    }

    @Override
    public Void visitSimpleStringLiteral(SimpleStringLiteral node) {
      AstNode parent = node.getParent();
      if (parent instanceof Directive) {
        StringCompleter visitor = new StringCompleter(node);
        return parent.accept(visitor);
      }
      return null;
    }

    @Override
    public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
      analyzeSuperConstructorInvocation(node);
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
        analyzeTypeName(createIdent(node), null);
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
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
      }
      return null;
    }

    @Override
    public Void visitVariableDeclaration(VariableDeclaration node) {
      if (isCompletionAfter(node.getEquals().getEnd())) {
        // { var x =! ...}
        analyzeLocalName(createIdent(node));
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
        operatorAccess(node.getCondition(), createIdent(node));
      }
      return null;
    }

    @Override
    public Void visitWithClause(WithClause node) {
      if (isCompletingKeyword(node.getWithKeyword())) {
        pKeyword(node.getWithKeyword());
      } else if (node.getMixinTypes().isEmpty()) {
        // { X with ! }
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
      } else {
        // { X with ! Y }
        analyzeTypeName(createIdent(node), typeDeclarationName(node));
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
        Token isToken = node.getIsOperator();
        if (completionLocation() == isToken.getEnd()) {
          Expression expression = node.getExpression();
          int offset = isToken.getOffset();
          // { target.is! } possible name completion, parsed as "target.{synthetic} is!"
          if (expression instanceof PrefixedIdentifier) {
            PrefixedIdentifier prefIdent = (PrefixedIdentifier) expression;
            if (prefIdent.getIdentifier().isSynthetic()) {
              analyzePrefixedAccess(prefIdent.getPrefix(), new Ident(node, "is", offset));
            } else {
              pKeyword(node.getIsOperator());
            }
            return null;
          }
          // { expr is! }
          if (!isSyntheticIdentifier(expression)) {
            pKeyword(node.getIsOperator());
            return null;
          }
          // { is! } possible name completion
          analyzeLocalName(new Ident(node, "is", offset));
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

  private static boolean isPrivate(Element element) {
    String name = element.getDisplayName();
    return Identifier.isPrivateName(name);
  }

  private static boolean isSyntheticIdentifier(Expression expression) {
    return expression instanceof SimpleIdentifier && ((SimpleIdentifier) expression).isSynthetic();
  }

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
    AstNode completionNode = context.getCoveredNode();
    if (completionNode != null) {
      state.setContext(completionNode);
      TerminalNodeCompleter visitor = new TerminalNodeCompleter();
      completionNode.accept(visitor);
    }
    requestor.endReporting();
  }

  void analyzeAnnotationName(SimpleIdentifier identifier) {
    filter = createFilter(identifier);
    NameCollector names = collectTopLevelElementVisibleAt(identifier);
    for (Element element : names.getUniqueElements()) {
      if (element instanceof PropertyAccessorElement) {
        element = ((PropertyAccessorElement) element).getVariable();
      }
      if (element instanceof TopLevelVariableElement) {
        TopLevelVariableElement variable = (TopLevelVariableElement) element;
        if (state.isCompileTimeConstantRequired && !variable.isConst()) {
          continue;
        }
        proposeName(element, identifier, names);
      }
      if (element instanceof ClassElement) {
        ClassElement classElement = (ClassElement) element;
        for (ConstructorElement constructor : classElement.getConstructors()) {
          pNamedConstructor(classElement, constructor, identifier);
        }
      }
    }
  }

  void analyzeConstructorTypeName(SimpleIdentifier identifier) {
    filter = createFilter(identifier);
    Element[] types = findAllTypes(getCurrentLibrary(), TopLevelNamesKind.DECLARED_AND_IMPORTS);
    for (Element type : types) {
      if (type instanceof ClassElement) {
        namedConstructorReference((ClassElement) type, identifier);
      }
    }
    Element[] prefixes = findAllPrefixes();
    for (Element prefix : prefixes) {
      pName(prefix, identifier);
    }
  }

  void analyzeDeclarationName(VariableDeclaration varDecl) {
    // We might want to propose multiple names for a declaration based on types someday.
    // For now, just use whatever is already there.
    SimpleIdentifier identifier = varDecl.getName();
    filter = createFilter(identifier);
    VariableDeclarationList varList = (VariableDeclarationList) varDecl.getParent();
    TypeName type = varList.getType();
    if (identifier.getLength() > 0) {
      pName(identifier.getName(), ProposalKind.VARIABLE);
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
    filter = createFilter(fieldName);
    ClassDeclaration classDecl = fieldName.getAncestor(ClassDeclaration.class);
    ClassElement classElement = classDecl.getElement();
    for (FieldElement field : classElement.getFields()) {
      pName(field.getDisplayName(), ProposalKind.FIELD);
    }
  }

  void analyzeLiteralReference(BooleanLiteral literal) {
//    state.setContext(literal);
    Ident ident = createIdent(literal.getParent());
    ident.setToken(literal.getLiteral());
    filter = createFilter(ident);
    analyzeLocalName(ident);
  }

  void analyzeLocalName(SimpleIdentifier identifier) {
    // Completion x!
    filter = createFilter(identifier);
    // TODO Filter out types that have no static members.
    NameCollector names = collectIdentifiersVisibleAt(identifier);
    for (Element element : names.getUniqueElements()) {
      if (state.isSourceDeclarationStatic) {
        if (element instanceof FieldElement) {
          if (!((FieldElement) element).isStatic()) {
            continue;
          }
        } else if (element instanceof PropertyAccessorElement) {
          if (!((PropertyAccessorElement) element).isStatic()) {
            continue;
          }
        }
      }
      if (state.isOptionalArgumentRequired) {
        if (!(element instanceof ParameterElement)) {
          continue;
        }
        ParameterElement param = (ParameterElement) element;
        if (!param.getParameterKind().isOptional()) {
          continue;
        }
      }
      proposeName(element, identifier, names);
    }
    if (state.areLiteralsAllowed) {
      pNull();
      pTrue();
      pFalse();
    }
  }

  void analyzeNamedParameter(ArgumentList args, SimpleIdentifier identifier) {
    // Completion x!
    filter = createFilter(identifier);
    // prepare parameters
    ParameterElement[] parameters = getParameterElements(args);
    if (parameters == null) {
      return;
    }
    // remember already used names
    Set<String> usedNames = Sets.newHashSet();
    for (Expression arg : args.getArguments()) {
      if (arg instanceof NamedExpression) {
        NamedExpression namedExpr = (NamedExpression) arg;
        String name = namedExpr.getName().getLabel().getName();
        usedNames.add(name);
      }
    }
    // propose named parameters
    for (ParameterElement parameterElement : parameters) {
      // should be named
      if (parameterElement.getParameterKind() != ParameterKind.NAMED) {
        continue;
      }
      // filter by name
      if (filterDisallows(parameterElement)) {
        continue;
      }
      // may be already used
      String parameterName = parameterElement.getName();
      if (usedNames.contains(parameterName)) {
        continue;
      }
      // OK, add proposal
      CompletionProposal prop = createProposal(ProposalKind.NAMED_ARGUMENT);
      prop.setCompletion(parameterName);
      prop.setParameterName(parameterName);
      prop.setParameterType(parameterElement.getType().getDisplayName());
      prop.setLocation(identifier.getOffset());
      prop.setReplacementLength(identifier.getLength());
      prop.setRelevance(CompletionProposal.RELEVANCE_HIGH);
      requestor.accept(prop);
    }
  }

  void analyzeNewParameterName(List<FormalParameter> params, SimpleIdentifier typeIdent,
      String identifierName) {
    String typeName = typeIdent.getName();
    filter = createFilter(createIdent(typeIdent));
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

  void analyzePositionalArgument(ArgumentList args, SimpleIdentifier identifier) {
    // Show parameter name only if there is nothing to complete, so that if there is only
    // one match, we won't to force user to choose.
    if (!StringUtils.isEmpty(identifier.getName())) {
      return;
    }
    // prepare parameters
    ParameterElement[] parameters = getParameterElements(args);
    if (parameters == null) {
      return;
    }
    // show current parameter
    int argIndex = args.getArguments().indexOf(identifier);
    if (argIndex == -1) {
      argIndex = 0;
    }
    if (argIndex >= 0 && argIndex < parameters.length) {
      ParameterElement parameter = parameters[argIndex];
      if (parameter.getParameterKind() != ParameterKind.NAMED) {
        String parameterName = parameter.getDisplayName();
        CompletionProposal prop = createProposal(ProposalKind.OPTIONAL_ARGUMENT);
        prop.setCompletion(parameterName);
        prop.setParameterName(parameterName);
        prop.setParameterType(parameter.getType().getDisplayName());
        prop.setLocation(identifier.getOffset());
        prop.setReplacementLength(identifier.getLength());
        prop.setRelevance(CompletionProposal.RELEVANCE_HIGH);
        requestor.accept(prop);
      }
    }
  }

  void analyzePrefixedAccess(Expression receiver, SimpleIdentifier completionNode) {
    if (receiver instanceof ThisExpression && !state.isThisAllowed) {
      return;
    }
    Type receiverType = typeOf(receiver);
    boolean forSuper = receiver instanceof SuperExpression;
    analyzePrefixedAccess(receiverType, forSuper, completionNode);
  }

  void analyzePrefixedAccess(Type receiverType, boolean forSuper, SimpleIdentifier completionNode) {
    if (receiverType != null) {
      // Complete x.!y
      Element rcvrTypeElem = receiverType.getElement();
      if (receiverType.isBottom() || receiverType.isDynamic()) {
        receiverType = getObjectType();
      }
      if (receiverType instanceof InterfaceType || receiverType instanceof UnionType) {
        prefixedAccess(receiverType, forSuper, completionNode);
      } else if (rcvrTypeElem instanceof TypeParameterElement) {
        TypeParameterElement typeParamElem = (TypeParameterElement) rcvrTypeElem;
        analyzePrefixedAccess(typeParamElem.getBound(), false, completionNode);
      }
    }
  }

  void analyzeReceiver(SimpleIdentifier identifier) {
    // Completion x!.y
    filter = createFilter(identifier);
    NameCollector names = collectIdentifiersVisibleAt(identifier);
    for (Element element : names.getUniqueElements()) {
      proposeName(element, identifier, names);
    }
  }

  void analyzeSuperConstructorInvocation(SuperConstructorInvocation node) {
    ClassDeclaration enclosingClassNode = node.getAncestor(ClassDeclaration.class);
    if (enclosingClassNode != null) {
      ClassElement enclosingClassElement = enclosingClassNode.getElement();
      if (enclosingClassElement != null) {
        ClassElement superClassElement = enclosingClassElement.getSupertype().getElement();
        constructorReference(superClassElement, node.getConstructorName());
      }
    }
  }

  void analyzeTypeName(SimpleIdentifier identifier, SimpleIdentifier nameIdent) {
    filter = createFilter(identifier);
    String name = nameIdent == null ? "" : nameIdent.getName();
    Element[] types = findAllTypes(getCurrentLibrary(), TopLevelNamesKind.DECLARED_AND_IMPORTS);
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
      pName(type, nameIdent);
    }
    if (!state.isForMixin) {
      ClassDeclaration classDecl = identifier.getAncestor(ClassDeclaration.class);
      if (classDecl != null) {
        ClassElement classElement = classDecl.getElement();
        for (TypeParameterElement param : classElement.getTypeParameters()) {
          pName(param, nameIdent);
        }
      }
    }
    Element[] prefixes = findAllPrefixes();
    for (Element prefix : prefixes) {
      pName(prefix, nameIdent);
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
    filter = createFilter(identifier);
    for (ConstructorElement cons : classElement.getConstructors()) {
      if (state.isCompileTimeConstantRequired == cons.isConst() && filterAllows(cons)) {
        pExecutable(cons, identifier, false);
      }
    }
  }

  void directAccess(ClassElement classElement, SimpleIdentifier identifier) {
    filter = createFilter(identifier);
    NameCollector names = createNameCollector();
    names.addLocalNames(identifier);
    names.addNamesDefinedByHierarchy(classElement, false);
    names.addTopLevelNames(getCurrentLibrary(), TopLevelNamesKind.DECLARED_AND_IMPORTS);
    proposeNames(names, identifier);
  }

  void dispatchPrefixAnalysis(InstanceCreationExpression node) {
    // prepare ClassElement
    ClassElement classElement;
    {
      Element typeElement = typeOf(node).getElement();
      if (!(typeElement instanceof ClassElement)) {
        return;
      }
      classElement = (ClassElement) typeElement;
    }
    // prepare constructor name
    Identifier typeName = node.getConstructorName().getType().getName();
    SimpleIdentifier identifier = null;
    if (typeName instanceof SimpleIdentifier) {
      identifier = (SimpleIdentifier) typeName;
    } else if (typeName instanceof PrefixedIdentifier) {
      identifier = ((PrefixedIdentifier) typeName).getIdentifier();
    }
    if (identifier == null) {
      identifier = createIdent(node);
    }
    // analyze constructor name
    analyzeConstructorTypeName(identifier);
    constructorReference(classElement, identifier);
  }

  void dispatchPrefixAnalysis(MethodInvocation node) {
    // This might be a library prefix on a top-level function
    Expression expr = node.getRealTarget();
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
      analyzeLocalName(createIdent(node));
    } else {
      analyzePrefixedAccess(expr, node.getMethodName());
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
        analyzePrefixedAccess(receiverType, false, identifier);
        break;
      }
    }
  }

  void fieldReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a constructor defined in classElement.
    filter = createFilter(identifier);
    for (FieldElement cons : classElement.getFields()) {
      if (filterAllows(cons)) {
        pField(cons, identifier, classElement);
      }
    }
  }

  void namedConstructorReference(ClassElement classElement, SimpleIdentifier identifier) {
    // Complete identifier when it refers to a named constructor defined in classElement.
    if (filter == null) {
      filter = createFilter(identifier);
    }
    for (ConstructorElement cons : classElement.getConstructors()) {
      if (!isVisible(cons)) {
        continue;
      }
      if (state.isCompileTimeConstantRequired && !cons.isConst()) {
        continue;
      }
      pNamedConstructor(classElement, cons, identifier);
    }
  }

  void namespacePubReference(NamespaceDirective node, Set<String> packageUris) {
    // no import URI or package:
    String prefix = filter.prefix;
    String[] prefixStrings = prefix.split(":");
    if (!prefix.isEmpty() && !"package:".startsWith(prefixStrings[0])) {
      return;
    }
    // if no URI yet, propose package:
    if (prefix.isEmpty()) {
      pImportUriWithScheme(node, "package:");
      return;
    }
    // check "packages" folder for package libraries that are not added to AnalysisContext
    {
      Source contextSource = context.getSource();
      if (contextSource instanceof FileBasedSource) {
        FileBasedSource contextFileSource = (FileBasedSource) contextSource;
        String contextFilePath = contextFileSource.getFullName();
        File contextFile = new File(contextFilePath);
        File contextFolder = contextFile.getParentFile();
        File contextPackages = new File(contextFolder, "packages");
        if (contextPackages.isDirectory()) {
          for (File packageFolder : contextPackages.listFiles()) {
            String packageName = packageFolder.getName();
            String packageLibName = packageName + ".dart";
            File packageFile = new File(packageFolder, packageLibName);
            if (packageFile.exists() && packageFile.isFile()) {
              packageUris.add("package:" + packageName + "/" + packageLibName);
            }
          }
        }
      }
    }
    // add known package: URIs
    for (String uri : packageUris) {
      if (filterDisallows(uri)) {
        continue;
      }
      CompletionProposal prop = createProposal(ProposalKind.IMPORT);
      prop.setCompletion(uri);
      // put "lib" before "lib/src"
      if (!uri.contains("/src/")) {
        prop.setRelevance(CompletionProposal.RELEVANCE_HIGH);
      }
      // done
      requestor.accept(prop);
    }
  }

  void namespaceReference(NamespaceDirective node, SimpleStringLiteral literal) {
    String lit = literal.getLiteral().getLexeme();
    if (!lit.isEmpty()) {
      lit = lit.substring(1, Math.max(lit.length() - 1, 0));
    }
    filter = createFilter(new Ident(node, lit, literal.getOffset() + 1));
    Set<String> packageUris = Sets.newHashSet();
    List<LibraryElement> libraries = new ArrayList<LibraryElement>();
    List<LibraryElement> librariesInLib = new ArrayList<LibraryElement>();
    String currentLibraryName = getCurrentLibrary().getSource().getFullName();
    AnalysisContext ac = getAnalysisContext();
    Source[] sources = ac.getLibrarySources();
    for (Source s : sources) {
      String sName = s.getFullName();
      // skip current library
      if (currentLibraryName.equals(sName)) {
        continue;
      }
      // ".pub-cache/..../unittest-0.8.8/lib/unittest.dart" -> "package:unittest/unittest.dart"
      {
        URI uri = ac.getSourceFactory().restoreUri(s);
        if (uri != null) {
          String uriString = uri.toString();
          if (uriString.startsWith("package:")) {
            packageUris.add(uriString);
          }
        }
      }
      LibraryElement lib = ac.getLibraryElement(s);
      if (lib == null) {
        continue;
      } else if (isUnitInLibFolder(lib.getDefiningCompilationUnit())) {
        librariesInLib.add(lib);
      } else {
        libraries.add(lib);
      }
    }
    namespaceSdkReference(node);
    namespacePubReference(node, packageUris);
//    importPackageReference(node, libraries, librariesInLib);
  }

  void namespaceSdkReference(NamespaceDirective node) {
    String prefix = filter.prefix;
    String[] prefixStrings = prefix.split(":");
    if (!prefix.isEmpty() && !"dart:".startsWith(prefixStrings[0])) {
      return;
    }
    if (prefix.isEmpty()) {
      pImportUriWithScheme(node, "dart:");
      return;
    }
    // add DartSdk libraries
    DartSdk dartSdk = getAnalysisContext().getSourceFactory().getDartSdk();
    for (SdkLibrary library : dartSdk.getSdkLibraries()) {
      String name = library.getShortName();
      // ignore internal
      if (library.isInternal()) {
        continue;
      }
      // ignore implementation
      if (library.isImplementation()) {
        continue;
      }
      // standard libraries name name starting with "dart:"
      name = StringUtils.removeStart(name, "dart:");
      // ignore private libraries
      if (Identifier.isPrivateName(name)) {
        continue;
      }
      // add with "dart:" prefix
      pName("dart:" + name, ProposalKind.IMPORT);
    }
  }

  void operatorAccess(Expression expr, SimpleIdentifier identifier) {
    state.requiresOperators();
    analyzePrefixedAccess(expr, identifier);
  }

  void prefixedAccess(InterfaceType type, boolean forSuper, SimpleIdentifier identifier) {
    // Complete identifier when it refers to field or method in classElement.
    filter = createFilter(identifier);
    NameCollector names = createNameCollector();
    if (state.areInstanceReferencesProhibited) {
      names.addNamesDefinedByType(type);
    } else {
      names.addNamesDefinedByTypeHierarchy(type, forSuper);
    }
    proposeNames(names, identifier);
  }

  void prefixedAccess(SimpleIdentifier prefixName, SimpleIdentifier identifier) {
    if (filter == null) {
      filter = createFilter(identifier);
    }
    NameCollector names = createNameCollector();
    ImportElement[] prefixImports = importsWithName(prefixName);
    // Library prefixes do not have a unique AST representation so we need to fudge state vars.
    boolean litsAllowed = state.areLiteralsAllowed;
    state.areLiteralsAllowed = false;
    names.addTopLevelNames(prefixImports, TopLevelNamesKind.DECLARED_AND_EXPORTS);
    state.areLiteralsAllowed = litsAllowed;
    proposeNames(names, identifier);
  }

  void prefixedAccess(Type type, boolean forSuper, SimpleIdentifier identifier) {
    filter = createFilter(identifier);
    NameCollector names = createNameCollector();
    if (state.areInstanceReferencesProhibited) {
      names.addNamesDefinedByType(type);
    } else {
      names.addNamesDefinedByTypeHierarchy(type, forSuper);
    }
    proposeNames(names, identifier);
  }

  @DartBlockBody({"// TODO(scheglov) translate it", "return [];"})
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

  private NameCollector collectIdentifiersVisibleAt(AstNode ident) {
    NameCollector names = createNameCollector();
    ScopedNameFinder finder = new ScopedNameFinder(completionLocation());
    ident.accept(finder);
    names.addAll(finder.getLocals().values());
    Declaration decl = finder.getDeclaration();
    if (decl != null && decl.getParent() instanceof ClassDeclaration) {
      ClassElement classElement = ((ClassDeclaration) decl.getParent()).getElement();
      names.addNamesDefinedByHierarchy(classElement, false);
    }
    names.addTopLevelNames(getCurrentLibrary(), TopLevelNamesKind.DECLARED_AND_IMPORTS);
    return names;
  }

  private NameCollector collectTopLevelElementVisibleAt(AstNode ident) {
    NameCollector names = createNameCollector();
    names.addTopLevelNames(getCurrentLibrary(), TopLevelNamesKind.DECLARED_AND_IMPORTS);
    return names;
  }

  private int completionLocation() {
    return context.getSelectionOffset();
  }

  private int completionTokenOffset() {
    return completionLocation() - filter.prefix.length();
  }

  @DartOmit
  private SearchScope constructSearchScope() {
    if (libraries == null) {
      libraries = currentLibraryList();
    }
    if (libraries != null) {
      return SearchScopeFactory.createLibraryScope(libraries);
    }
    return SearchScopeFactory.createUniverseScope();
  }

  private <X extends AstNode> List<FormalParameter> copyWithout(NodeList<X> oldList,
      final AstNode deletion) {
    final List<FormalParameter> newList = new ArrayList<FormalParameter>(oldList.size() - 1);
    oldList.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitNode(AstNode node) {
        if (node != deletion) {
          newList.add((FormalParameter) node);
        }
        return null;
      }
    });
    return newList;
  }

  private Filter createFilter(SimpleIdentifier ident) {
    return new Filter(ident, context.getSelectionOffset());
  }

  private Ident createIdent(AstNode node) {
    return new Ident(node, completionLocation());
  }

  private NameCollector createNameCollector() {
    return new NameCollector();
  }

  private CompletionProposal createProposal(Element element) {
    String completion = element.getDisplayName();
    return createProposal(element, completion);
  }

  private CompletionProposal createProposal(Element element, SimpleIdentifier identifier) {
    // Create a completion proposal for the element: variable, field, class, function.
    if (filterDisallows(element)) {
      return null;
    }
    CompletionProposal prop = createProposal(element);
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getDisplayName());
    }
    Type type = typeOf(element);
    if (type != null) {
      prop.setReturnType(type.getName());
    }
    if (identifier != null) {
      prop.setReplacementLengthIdentifier(identifier.getLength());
    }
    return prop;
  }

  private CompletionProposal createProposal(Element element, String completion) {
    ProposalKind kind = proposalKindOf(element);
    CompletionProposal prop = createProposal(kind);
    prop.setElement(element);
    prop.setCompletion(completion);
    prop.setDeprecated(isDeprecated(element));
    if (isPrivate(element)) {
      prop.setRelevance(CompletionProposal.RELEVANCE_LOW);
    }
    if (filter.isSameCasePrefix(element.getName())) {
      prop.incRelevance();
    }
    return prop;
  }

  private CompletionProposal createProposal(ProposalKind kind) {
    return factory.createCompletionProposal(kind, completionTokenOffset());
  }

  private LibraryElement[] currentLibraryList() {
    Set<LibraryElement> libraries = new HashSet<LibraryElement>();
    LibraryElement curLib = getCurrentLibrary();
    libraries.add(curLib);
    LinkedList<LibraryElement> queue = new LinkedList<LibraryElement>();
    Collections.addAll(queue, curLib.getImportedLibraries());
    currentLibraryLister(queue, libraries);
    return libraries.toArray(new LibraryElement[libraries.size()]);
  }

  private void currentLibraryLister(LinkedList<LibraryElement> queue, Set<LibraryElement> libraries) {
    while (!queue.isEmpty()) {
      LibraryElement sourceLib = queue.removeFirst();
      libraries.add(sourceLib);
      LibraryElement[] expLibs = sourceLib.getExportedLibraries();
      for (LibraryElement lib : expLibs) {
        if (!libraries.contains(lib)) {
          queue.add(lib);
        }
      }
    }
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

  private Element[] findAllNotTypes(List<Element> elements) {
    elements = Lists.newArrayList(elements);
    for (Iterator<Element> I = elements.iterator(); I.hasNext();) {
      Element element = I.next();
      ElementKind kind = element.getKind();
      if (kind == ElementKind.FUNCTION || kind == ElementKind.TOP_LEVEL_VARIABLE
          || kind == ElementKind.GETTER || kind == ElementKind.SETTER) {
        continue;
      }
      I.remove();
    }
    return elements.toArray(new Element[elements.size()]);
  }

  private Element[] findAllPrefixes() {
    LibraryElement lib = context.getCompilationUnitElement().getEnclosingElement();
    return lib.getPrefixes();
  }

  private Element[] findAllTypes(LibraryElement library, TopLevelNamesKind topKind) {
    List<Element> elements = findTopLevelElements(library, topKind);
    return findAllTypes(elements);
  }

  private Element[] findAllTypes(List<Element> elements) {
    elements = Lists.newArrayList(elements);
    for (Iterator<Element> I = elements.iterator(); I.hasNext();) {
      Element element = I.next();
      ElementKind kind = element.getKind();
      if (kind == ElementKind.CLASS || kind == ElementKind.FUNCTION_TYPE_ALIAS) {
        continue;
      }
      I.remove();
    }
    return elements.toArray(new Element[elements.size()]);
  }

  private List<Element> findTopLevelElements(LibraryElement library, TopLevelNamesKind topKind) {
    List<Element> elements = Lists.newArrayList();
    if (topKind == TopLevelNamesKind.DECLARED_AND_IMPORTS) {
      elements.addAll(CorrectionUtils.getTopLevelElements(library));
      for (ImportElement imp : library.getImports()) {
        elements.addAll(CorrectionUtils.getImportNamespace(imp).values());
      }
      removeNotMatchingFilter(elements);
    }
    if (topKind == TopLevelNamesKind.DECLARED_AND_EXPORTS) {
      elements.addAll(CorrectionUtils.getExportNamespace(library).values());
      removeNotMatchingFilter(elements);
    }
    return elements;
  }

  private AnalysisContext getAnalysisContext() {
    return context.getCompilationUnitElement().getContext();
  }

  private LibraryElement getCurrentLibrary() {
    return context.getCompilationUnitElement().getEnclosingElement();
  }

  private FunctionType getFunctionType(Element element) {
    if (element instanceof ExecutableElement) {
      ExecutableElement executableElement = (ExecutableElement) element;
      return executableElement.getType();
    }
    if (element instanceof VariableElement) {
      VariableElement variableElement = (VariableElement) element;
      Type type = variableElement.getType();
      if (type instanceof FunctionType) {
        return (FunctionType) type;
      }
    }
    return null;
  }

  private ClassElement getObjectClassElement() {
    return getTypeProvider().getObjectType().getElement();
  }

  private InterfaceType getObjectType() {
    return getTypeProvider().getObjectType();
  }

  private ParameterElement[] getParameterElements(ArgumentList args) {
    ParameterElement[] parameters = null;
    AstNode argsParent = args.getParent();
    if (argsParent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) argsParent;
      Element nameElement = invocation.getMethodName().getStaticElement();
      FunctionType functionType = getFunctionType(nameElement);
      if (functionType != null) {
        parameters = functionType.getParameters();
      }
    }
    if (argsParent instanceof InstanceCreationExpression) {
      InstanceCreationExpression creation = (InstanceCreationExpression) argsParent;
      ConstructorElement element = creation.getStaticElement();
      if (element != null) {
        parameters = ((ExecutableElement) element).getParameters();
      }
    }
    if (argsParent instanceof Annotation) {
      Annotation annotation = (Annotation) argsParent;
      Element element = annotation.getElement();
      if (element instanceof ConstructorElement) {
        parameters = ((ConstructorElement) element).getParameters();
      }
    }
    return parameters;
  }

  private TypeProvider getTypeProvider() {
    AnalysisContext analysisContext = context.getCompilationUnitElement().getContext();
    try {
      return ((InternalAnalysisContext) analysisContext).getTypeProvider();
    } catch (AnalysisException exception) {
      // TODO(brianwilkerson) Figure out the right thing to do if the core cannot be resolved.
      return null;
    }
  }

  private boolean hasErrorBeforeCompletionLocation() {
    AnalysisError[] errors = context.getErrors();
    if (errors == null || errors.length == 0) {
      return false;
    }
    return errors[0].getOffset() <= completionLocation();
  }

  private ImportElement[] importsWithName(SimpleIdentifier libName) {
    String name = libName.getName();
    List<ImportElement> imports = Lists.newArrayList();
    for (ImportElement imp : getCurrentLibrary().getImports()) {
      PrefixElement prefix = imp.getPrefix();
      if (prefix != null) {
        String impName = prefix.getDisplayName();
        if (name.equals(impName)) {
          imports.add(imp);
        }
      }
    }
    return imports.toArray(new ImportElement[imports.size()]);
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
    return element != null && element.isDeprecated();
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

  private boolean isVisible(Element element) {
    return !isPrivate(element) || isInCurrentLibrary(element);
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

  private void pArgumentList(CompletionProposal proposal, int offset, int len) {
    // prepare parameters
    String[] parameterNames = proposal.getParameterNames();
    if (parameterNames.length == 0) {
      return;
    }
    // fill arguments proposal
    CompletionProposal prop = createProposal(ProposalKind.ARGUMENT_LIST);
    prop.setElement(proposal.getElement());
    prop.setCompletion(proposal.getCompletion()).setReturnType(proposal.getReturnType());
    prop.setParameterNames(parameterNames);
    prop.setParameterTypes(proposal.getParameterTypes());
    prop.setParameterStyle(
        proposal.getPositionalParameterCount(),
        proposal.hasNamed(),
        proposal.hasPositional());
    prop.setReplacementLength(0).setLocation(completionLocation());
    prop.setRelevance(CompletionProposal.RELEVANCE_HIGH);
    requestor.accept(prop);
  }

  private void pDynamic() {
    pWord(C_DYNAMIC, ProposalKind.VARIABLE);
  }

  private void pExecutable(Element element, FunctionType functionType, SimpleIdentifier identifier,
      boolean isPotentialMatch) {
    // Create a completion proposal for the element: function, method, getter, setter, constructor.
    String name = element.getDisplayName();
    if (name.isEmpty()) {
      return; // Simple constructors are not handled here
    }
    if (filterDisallows(element)) {
      return;
    }
    if (!isVisible(element)) {
      return;
    }

    // May be we are in argument of function type parameter, propose function reference.
    if (state.targetParameter != null) {
      Type parameterType = state.targetParameter.getType();
      if (parameterType instanceof FunctionType) {
        if (functionType.isAssignableTo(parameterType)) {
          pName(name, element, CompletionProposal.RELEVANCE_HIGH, ProposalKind.METHOD_NAME);
        }
      }
    }

    CompletionProposal prop = createProposal(element);

    prop.setPotentialMatch(isPotentialMatch);
    if (isPotentialMatch) {
      prop.setRelevance(CompletionProposal.RELEVANCE_LOW);
    }

    setParameterInfo(functionType, prop);
    prop.setCompletion(name).setReturnType(functionType.getReturnType().getDisplayName());

    // If there is already argument list, then update only method name.
    if (identifier.getParent() instanceof MethodInvocation
        && ((MethodInvocation) identifier.getParent()).getArgumentList() != null) {
      prop.setKind(ProposalKind.METHOD_NAME);
    }

    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getDisplayName());
    }

    requestor.accept(prop);
  }

  private void pExecutable(ExecutableElement element, SimpleIdentifier identifier,
      boolean isPotentialMatch) {
    pExecutable(element, element.getType(), identifier, isPotentialMatch);
  }

  private void pExecutable(VariableElement element, SimpleIdentifier identifier) {
    // Create a completion proposal for the element: top-level variable.
    String name = element.getDisplayName();
    if (name.isEmpty() || filterDisallows(element)) {
      return; // Simple constructors are not handled here
    }
    CompletionProposal prop = createProposal(element);
    if (element.getType() != null) {
      prop.setReturnType(element.getType().getName());
    }
    Element container = element.getEnclosingElement();
    if (container != null) {
      prop.setDeclaringType(container.getDisplayName());
    }
    if (identifier != null) {
      prop.setReplacementLengthIdentifier(identifier.getLength());
    }
    requestor.accept(prop);
  }

  private void pFalse() {
    pWord(C_FALSE, ProposalKind.VARIABLE);
  }

  private void pField(FieldElement element, SimpleIdentifier identifier, ClassElement classElement) {
    // Create a completion proposal for the element: field only.
    if (filterDisallows(element)) {
      return;
    }
    CompletionProposal prop = createProposal(element);
    Element container = element.getEnclosingElement();
    prop.setDeclaringType(container.getDisplayName());
    requestor.accept(prop);
  }

  /**
   * Proposes URI with the given scheme for the given {@link NamespaceDirective}.
   */
  private void pImportUriWithScheme(NamespaceDirective node, String uriScheme) {
    String newUri = uriScheme + CompletionProposal.CURSOR_MARKER;
    if (node.getUri().isSynthetic()) {
      newUri = "'" + newUri + "'";
      if (node.getSemicolon() == null || node.getSemicolon().isSynthetic()) {
        newUri += ";";
      }
    }
    if (context.getSelectionOffset() == node.getKeyword().getEnd()) {
      newUri = " " + newUri;
    }
    pName(newUri, ProposalKind.IMPORT);
  }

  private void pKeyword(Token keyword) {
    filter = new Filter(keyword.getLexeme(), keyword.getOffset(), completionLocation());
    // This isn't as useful as it might seem. It only works in the case that completion
    // is requested on an existing recognizable keyword.
    // TODO: Add keyword proposal kind
    CompletionProposal prop = createProposal(ProposalKind.LIBRARY_PREFIX);
    prop.setCompletion(keyword.getLexeme());
    requestor.accept(prop);
  }

  private void pName(Element element, SimpleIdentifier identifier) {
    CompletionProposal prop = createProposal(element, identifier);
    if (prop != null) {
      requestor.accept(prop);
    }
  }

  private void pName(String name, Element element, int relevance, ProposalKind kind) {
    if (filterDisallows(name)) {
      return;
    }
    CompletionProposal prop = createProposal(kind);
    prop.setRelevance(relevance);
    prop.setCompletion(name);
    prop.setElement(element);
    requestor.accept(prop);
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
    CompletionProposal prop = createProposal(element, name);
    setParameterInfo(element.getType(), prop);
    prop.setReturnType(element.getType().getReturnType().getName());
    Element container = element.getEnclosingElement();
    prop.setDeclaringType(container.getDisplayName());
    if (identifier != null) {
      prop.setReplacementLengthIdentifier(identifier.getLength());
    }
    requestor.accept(prop);
  }

  private void pNull() {
    pWord(C_NULL, ProposalKind.VARIABLE);
  }

  private void pParamName(String name) {
    if (filterDisallows(name)) {
      return;
    }
    CompletionProposal prop = createProposal(ProposalKind.PARAMETER);
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
      case TYPE_PARAMETER:
        kind = ProposalKind.TYPE_PARAMETER;
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

  private void proposeCombinator(Combinator node, SimpleIdentifier identifier) {
    filter = createFilter(identifier);
    NamespaceDirective directive = (NamespaceDirective) node.getParent();
    LibraryElement libraryElement = directive.getUriElement();
    if (libraryElement != null) {
      // prepare Elements with unique names
      NameCollector nameCollector = createNameCollector();
      Collection<Element> elements = CorrectionUtils.getExportNamespace(libraryElement).values();
      for (Element element : elements) {
        if (filterDisallows(element)) {
          continue;
        }
        nameCollector.mergeName(element);
      }
      // propose each Element
      for (Element element : nameCollector.getUniqueElements()) {
        CompletionProposal proposal = createProposal(element);
        if (proposal.getKind() == ProposalKind.FUNCTION) {
          proposal.setKind(ProposalKind.METHOD_NAME);
        }
        requestor.accept(proposal);
      }
    }
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
        FunctionType functionType = getFunctionType(element);
        if (functionType != null) {
          pExecutable(element, functionType, identifier, names.isPotentialMatch(element));
        } else {
          VariableElement var = (VariableElement) element;
          pExecutable(var, identifier);
        }
        break;
      case CLASS:
        pName(element, identifier);
        break;
      default:
        break;
    }
  }

  private void proposeNames(NameCollector names, SimpleIdentifier identifier) {
    for (Element element : names.getUniqueElements()) {
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
    CompletionProposal prop = createProposal(kind);
    prop.setCompletion(word);
    requestor.accept(prop);
  }

  private void removeNotMatchingFilter(List<Element> elements) {
    if (filter == null) {
      return;
    }
    filter.makePattern();
    filter.removeNotMatching(elements);
  }

  private void setParameterInfo(FunctionType functionType, CompletionProposal prop) {
    List<String> params = new ArrayList<String>();
    List<String> types = new ArrayList<String>();
    boolean named = false, positional = false;
    int posCount = 0;
    for (ParameterElement param : functionType.getParameters()) {
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
  private SimpleIdentifier typeDeclarationName(AstNode node) {
    AstNode parent = node;
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
          Type inducerType = inducer.getType();
          if (inducerType == null || inducerType.isDynamic()) {
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
    // Use static type if known.
    {
      Type staticType = expr.getStaticType();
      if (staticType != null && !staticType.isDynamic()) {
        return staticType;
      }
    }
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

  private Type typeOfContainingClass(AstNode node) {
    AstNode parent = node;
    while (parent != null) {
      if (parent instanceof ClassDeclaration) {
        return ((ClassDeclaration) parent).getElement().getType();
      }
      parent = parent.getParent();
    }
    return DynamicTypeImpl.getInstance();
  }

  @DartBlockBody({"// TODO(scheglov) translate it", "return null;"})
  private Type typeSearch(PropertyInducingElement varElement) {
    SearchEngine engine = context.getSearchEngine();
    SearchScope scope = constructSearchScope();
    Set<Type> matches = engine.searchAssignedTypes(varElement, scope);
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
