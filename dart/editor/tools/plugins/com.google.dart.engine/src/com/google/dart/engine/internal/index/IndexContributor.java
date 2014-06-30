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

package com.google.dart.engine.internal.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.Combinator;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.index.IndexStore;
import com.google.dart.engine.index.Location;
import com.google.dart.engine.index.LocationWithData;
import com.google.dart.engine.index.Relationship;
import com.google.dart.engine.internal.scope.Namespace;
import com.google.dart.engine.internal.scope.NamespaceBuilder;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.general.StringUtilities;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Visits resolved AST and adds relationships into {@link IndexStore}.
 * 
 * @coverage dart.engine.index
 */
public class IndexContributor extends GeneralizingAstVisitor<Void> {
  /**
   * Information about {@link ImportElement} and place where it is referenced using
   * {@link PrefixElement}.
   */
  private static class ImportElementInfo {
    ImportElement element;
    int periodEnd;
  }

  /**
   * @return the {@link Location} representing location of the {@link Element}.
   */
  @VisibleForTesting
  public static Location createLocation(Element element) {
    if (element != null) {
      int offset = element.getNameOffset();
      int length = element.getDisplayName().length();
      return new Location(element, offset, length);
    }
    return null;
  }

  /**
   * @return the {@link ImportElement} that is referenced by this node with {@link PrefixElement},
   *         may be {@code null}.
   */
  public static ImportElement getImportElement(SimpleIdentifier prefixNode) {
    ImportElementInfo info = getImportElementInfo(prefixNode);
    return info != null ? info.element : null;
  }

  /**
   * @return the {@link ImportElementInfo} with {@link ImportElement} that is referenced by this
   *         node with {@link PrefixElement}, may be {@code null}.
   */
  public static ImportElementInfo getImportElementInfo(SimpleIdentifier prefixNode) {
    ImportElementInfo info = new ImportElementInfo();
    // prepare environment
    AstNode parent = prefixNode.getParent();
    CompilationUnit unit = prefixNode.getAncestor(CompilationUnit.class);
    LibraryElement libraryElement = unit.getElement().getLibrary();
    // prepare used element
    Element usedElement = null;
    if (parent instanceof PrefixedIdentifier) {
      PrefixedIdentifier prefixed = (PrefixedIdentifier) parent;
      if (prefixed.getPrefix() == prefixNode) {
        usedElement = prefixed.getStaticElement();
        info.periodEnd = prefixed.getPeriod().getEnd();
      }
    }
    if (parent instanceof MethodInvocation) {
      MethodInvocation invocation = (MethodInvocation) parent;
      if (invocation.getTarget() == prefixNode) {
        usedElement = invocation.getMethodName().getStaticElement();
        info.periodEnd = invocation.getPeriod().getEnd();
      }
    }
    // we need used Element
    if (usedElement == null) {
      return null;
    }
    // find ImportElement
    String prefix = prefixNode.getName();
    Map<ImportElement, Set<Element>> importElementsMap = Maps.newHashMap();
    info.element = internalGetImportElement(libraryElement, prefix, usedElement, importElementsMap);
    if (info.element == null) {
      return null;
    }
    return info;
  }

  /**
   * If the given expression has resolved type, returns the new location with this type.
   * 
   * @param location the base location
   * @param expression the expression assigned at the given location
   */
  private static Location getLocationWithExpressionType(Location location, Expression expression) {
    if (expression != null) {
      return new LocationWithData<Type>(location, expression.getBestType());
    }
    return location;
  }

  /**
   * If the given node is the part of the {@link ConstructorFieldInitializer}, returns location with
   * type of the initializer expression.
   */
  private static Location getLocationWithInitializerType(SimpleIdentifier node, Location location) {
    if (node.getParent() instanceof ConstructorFieldInitializer) {
      ConstructorFieldInitializer initializer = (ConstructorFieldInitializer) node.getParent();
      if (initializer.getFieldName() == node) {
        location = getLocationWithExpressionType(location, initializer.getExpression());
      }
    }
    return location;
  }

  /**
   * If the given identifier has a synthetic {@link PropertyAccessorElement}, i.e. accessor for
   * normal field, and it is LHS of assignment, then include {@link Type} of the assigned value into
   * the {@link Location}.
   * 
   * @param identifier the identifier to record location
   * @param element the element of the identifier
   * @param location the raw location
   * @return the {@link Location} with the type of the assigned value
   */
  private static Location getLocationWithTypeAssignedToField(SimpleIdentifier identifier,
      Element element, Location location) {
    // we need accessor
    if (!(element instanceof PropertyAccessorElement)) {
      return location;
    }
    PropertyAccessorElement accessor = (PropertyAccessorElement) element;
    // should be setter
    if (!accessor.isSetter()) {
      return location;
    }
    // accessor should be synthetic, i.e. field normal
    if (!accessor.isSynthetic()) {
      return location;
    }
    // should be LHS of assignment
    AstNode parent;
    {
      AstNode node = identifier;
      parent = node.getParent();
      // new T().field = x;
      if (parent instanceof PropertyAccess) {
        PropertyAccess propertyAccess = (PropertyAccess) parent;
        if (propertyAccess.getPropertyName() == node) {
          node = propertyAccess;
          parent = propertyAccess.getParent();
        }
      }
      // obj.field = x;
      if (parent instanceof PrefixedIdentifier) {
        PrefixedIdentifier prefixedIdentifier = (PrefixedIdentifier) parent;
        if (prefixedIdentifier.getIdentifier() == node) {
          node = prefixedIdentifier;
          parent = prefixedIdentifier.getParent();
        }
      }
    }
    // OK, remember the type
    if (parent instanceof AssignmentExpression) {
      AssignmentExpression assignment = (AssignmentExpression) parent;
      Expression rhs = assignment.getRightHandSide();
      location = getLocationWithExpressionType(location, rhs);
    }
    // done
    return location;
  }

  /**
   * @return the {@link ImportElement} that declares given {@link PrefixElement} and imports library
   *         with given "usedElement".
   */
  private static ImportElement internalGetImportElement(LibraryElement libraryElement,
      String prefix, Element usedElement, Map<ImportElement, Set<Element>> importElementsMap) {
    // validate Element
    if (usedElement == null) {
      return null;
    }
    if (!(usedElement.getEnclosingElement() instanceof CompilationUnitElement)) {
      return null;
    }
    LibraryElement usedLibrary = usedElement.getLibrary();
    // find ImportElement that imports used library with used prefix
    List<ImportElement> candidates = null;
    for (ImportElement importElement : libraryElement.getImports()) {
      // required library
      if (!Objects.equal(importElement.getImportedLibrary(), usedLibrary)) {
        continue;
      }
      // required prefix
      PrefixElement prefixElement = importElement.getPrefix();
      if (prefix == null) {
        if (prefixElement != null) {
          continue;
        }
      } else {
        if (prefixElement == null) {
          continue;
        }
        if (!prefix.equals(prefixElement.getName())) {
          continue;
        }
      }
      // no combinators => only possible candidate
      if (importElement.getCombinators().length == 0) {
        return importElement;
      }
      // OK, we have candidate
      if (candidates == null) {
        candidates = Lists.newArrayList();
      }
      candidates.add(importElement);
    }
    // no candidates, probably element is defined in this library
    if (candidates == null) {
      return null;
    }
    // one candidate
    if (candidates.size() == 1) {
      return candidates.get(0);
    }
    // ensure that each ImportElement has set of elements
    for (ImportElement importElement : candidates) {
      if (importElementsMap.containsKey(importElement)) {
        continue;
      }
      Namespace namespace = new NamespaceBuilder().createImportNamespaceForDirective(importElement);
      Set<Element> elements = Sets.newHashSet(namespace.getDefinedNames().values());
      importElementsMap.put(importElement, elements);
    }
    // use import namespace to choose correct one
    for (Entry<ImportElement, Set<Element>> entry : importElementsMap.entrySet()) {
      if (entry.getValue().contains(usedElement)) {
        return entry.getKey();
      }
    }
    // not found
    return null;
  }

  /**
   * @return {@code true} if given "node" is part of an import {@link Combinator}.
   */
  private static boolean isIdentifierInImportCombinator(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    return parent instanceof Combinator;
  }

  /**
   * @return {@code true} if given "node" is part of {@link PrefixedIdentifier} "prefix.node".
   */
  private static boolean isIdentifierInPrefixedIdentifier(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    return parent instanceof PrefixedIdentifier
        && ((PrefixedIdentifier) parent).getIdentifier() == node;
  }

  private final IndexStore store;

  private LibraryElement libraryElement;

  private final Map<ImportElement, Set<Element>> importElementsMap = Maps.newHashMap();

  /**
   * A stack whose top element (the element with the largest index) is an element representing the
   * inner-most enclosing scope.
   */
  private LinkedList<Element> elementStack = Lists.newLinkedList();

  public IndexContributor(IndexStore store) {
    this.store = store;
  }

  /**
   * Enter a new scope represented by the given {@link Element}.
   */
  public void enterScope(Element element) {
    elementStack.addFirst(element);
  }

  /**
   * @return the inner-most enclosing {@link Element}, may be {@code null}.
   */
  @VisibleForTesting
  public Element peekElement() {
    for (Element element : elementStack) {
      if (element != null) {
        return element;
      }
    }
    return null;
  }

  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    recordOperatorReference(node.getOperator(), node.getBestElement());
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    recordOperatorReference(node.getOperator(), node.getBestElement());
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement element = node.getElement();
    enterScope(element);
    try {
      recordElementDefinition(element, IndexConstants.DEFINES_CLASS);
      {
        ExtendsClause extendsClause = node.getExtendsClause();
        if (extendsClause != null) {
          TypeName superclassNode = extendsClause.getSuperclass();
          recordSuperType(superclassNode, IndexConstants.IS_EXTENDED_BY);
        } else {
          InterfaceType superType = element.getSupertype();
          if (superType != null) {
            ClassElement objectElement = superType.getElement();
            recordRelationship(
                objectElement,
                IndexConstants.IS_EXTENDED_BY,
                createLocationFromOffset(node.getName().getOffset(), 0));
          }
        }
      }
      {
        WithClause withClause = node.getWithClause();
        if (withClause != null) {
          for (TypeName mixinNode : withClause.getMixinTypes()) {
            recordSuperType(mixinNode, IndexConstants.IS_MIXED_IN_BY);
          }
        }
      }
      {
        ImplementsClause implementsClause = node.getImplementsClause();
        if (implementsClause != null) {
          for (TypeName interfaceNode : implementsClause.getInterfaces()) {
            recordSuperType(interfaceNode, IndexConstants.IS_IMPLEMENTED_BY);
          }
        }
      }
      return super.visitClassDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    ClassElement element = node.getElement();
    enterScope(element);
    try {
      recordElementDefinition(element, IndexConstants.DEFINES_CLASS_ALIAS);
      {
        TypeName superclassNode = node.getSuperclass();
        if (superclassNode != null) {
          recordSuperType(superclassNode, IndexConstants.IS_EXTENDED_BY);
        }
      }
      {
        WithClause withClause = node.getWithClause();
        if (withClause != null) {
          for (TypeName mixinNode : withClause.getMixinTypes()) {
            recordSuperType(mixinNode, IndexConstants.IS_MIXED_IN_BY);
          }
        }
      }
      {
        ImplementsClause implementsClause = node.getImplementsClause();
        if (implementsClause != null) {
          for (TypeName interfaceNode : implementsClause.getInterfaces()) {
            recordSuperType(interfaceNode, IndexConstants.IS_IMPLEMENTED_BY);
          }
        }
      }
      return super.visitClassTypeAlias(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    CompilationUnitElement unitElement = node.getElement();
    if (unitElement != null) {
      elementStack.add(unitElement);
      libraryElement = unitElement.getEnclosingElement();
      if (libraryElement != null) {
        return super.visitCompilationUnit(node);
      }
    }
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ConstructorElement element = node.getElement();
    // define
    {
      Location location;
      if (node.getName() != null) {
        int start = node.getPeriod().getOffset();
        int end = node.getName().getEnd();
        location = createLocationFromOffset(start, end - start);
      } else {
        int start = node.getReturnType().getEnd();
        location = createLocationFromOffset(start, 0);
      }
      recordRelationship(element, IndexConstants.IS_DEFINED_BY, location);
    }
    // visit children
    enterScope(element);
    try {
      return super.visitConstructorDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    ConstructorElement element = node.getStaticElement();
    // in 'class B = A;' actually A constructors are invoked
    if (element != null && element.isSynthetic() && element.getRedirectedConstructor() != null) {
      element = element.getRedirectedConstructor();
    }
    // prepare location
    Location location;
    if (node.getName() != null) {
      int start = node.getPeriod().getOffset();
      int end = node.getName().getEnd();
      location = createLocationFromOffset(start, end - start);
    } else {
      int start = node.getType().getEnd();
      location = createLocationFromOffset(start, 0);
    }
    // record relationship
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitConstructorName(node);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    ExportElement element = node.getElement();
    if (element != null) {
      LibraryElement expLibrary = element.getExportedLibrary();
      recordLibraryReference(node, expLibrary);
    }
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitFormalParameter(FormalParameter node) {
    ParameterElement element = node.getElement();
    enterScope(element);
    try {
      return super.visitFormalParameter(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    Element element = node.getElement();
    recordElementDefinition(element, IndexConstants.DEFINES_FUNCTION);
    enterScope(element);
    try {
      return super.visitFunctionDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    Element element = node.getElement();
    recordElementDefinition(element, IndexConstants.DEFINES_FUNCTION_TYPE);
    return super.visitFunctionTypeAlias(node);
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    ImportElement element = node.getElement();
    if (element != null) {
      LibraryElement impLibrary = element.getImportedLibrary();
      recordLibraryReference(node, impLibrary);
    }
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    MethodElement element = node.getBestElement();
    if (element instanceof MethodElement) {
      Token operator = node.getLeftBracket();
      Location location = createLocationFromToken(operator);
      recordRelationship(element, IndexConstants.IS_INVOKED_BY_QUALIFIED, location);
    }
    return super.visitIndexExpression(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement element = node.getElement();
    enterScope(element);
    try {
      return super.visitMethodDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    SimpleIdentifier name = node.getMethodName();
    Element element = name.getBestElement();
    if (element instanceof MethodElement || element instanceof PropertyAccessorElement) {
      Location location = createLocationFromNode(name);
      Relationship relationship;
      if (node.getTarget() != null) {
        relationship = IndexConstants.IS_INVOKED_BY_QUALIFIED;
      } else {
        relationship = IndexConstants.IS_INVOKED_BY_UNQUALIFIED;
      }
      recordRelationship(element, relationship, location);
    }
    if (element instanceof FunctionElement || element instanceof VariableElement) {
      Location location = createLocationFromNode(name);
      recordRelationship(element, IndexConstants.IS_INVOKED_BY, location);
    }
    // name invocation
    {
      Element nameElement = new NameElementImpl(name.getName());
      Location location = createLocationFromNode(name);
      Relationship kind = element != null ? IndexConstants.NAME_IS_INVOKED_BY_RESOLVED
          : IndexConstants.NAME_IS_INVOKED_BY_UNRESOLVED;
      store.recordRelationship(nameElement, kind, location);
    }
    recordImportElementReferenceWithoutPrefix(name);
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    Element element = node.getElement();
    Location location = createLocationFromNode(node.getUri());
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitPartDirective(node);
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    Location location = createLocationFromNode(node.getLibraryName());
    recordRelationship(node.getElement(), IndexConstants.IS_REFERENCED_BY, location);
    return null;
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    recordOperatorReference(node.getOperator(), node.getBestElement());
    return super.visitPostfixExpression(node);
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    recordOperatorReference(node.getOperator(), node.getBestElement());
    return super.visitPrefixExpression(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    Element nameElement = new NameElementImpl(node.getName());
    Location location = createLocationFromNode(node);
    // name in declaration
    if (node.inDeclarationContext()) {
      recordRelationship(nameElement, IndexConstants.IS_DEFINED_BY, location);
      return null;
    }
    // prepare information
    Element element = node.getBestElement();
    // qualified name reference
    recordQualifiedMemberReference(node, element, nameElement, location);
    // stop if already handled
    if (isAlreadyHandledName(node)) {
      return null;
    }
    // record name read/write
    {
      boolean inGetterContext = node.inGetterContext();
      boolean inSetterContext = node.inSetterContext();
      if (inGetterContext && inSetterContext) {
        Relationship kind = element != null ? IndexConstants.NAME_IS_READ_WRITTEN_BY_RESOLVED
            : IndexConstants.NAME_IS_READ_WRITTEN_BY_UNRESOLVED;
        store.recordRelationship(nameElement, kind, location);
      } else if (inGetterContext) {
        Relationship kind = element != null ? IndexConstants.NAME_IS_READ_BY_RESOLVED
            : IndexConstants.NAME_IS_READ_BY_UNRESOLVED;
        store.recordRelationship(nameElement, kind, location);
      } else if (inSetterContext) {
        Relationship kind = element != null ? IndexConstants.NAME_IS_WRITTEN_BY_RESOLVED
            : IndexConstants.NAME_IS_WRITTEN_BY_UNRESOLVED;
        store.recordRelationship(nameElement, kind, location);
      }
    }
    // record specific relations
    if (element instanceof ClassElement || element instanceof FunctionElement
        || element instanceof FunctionTypeAliasElement || element instanceof LabelElement
        || element instanceof TypeParameterElement) {
      recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    } else if (element instanceof FieldElement) {
      location = getLocationWithInitializerType(node, location);
      recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    } else if (element instanceof FieldFormalParameterElement) {
      FieldFormalParameterElement fieldParameter = (FieldFormalParameterElement) element;
      FieldElement field = fieldParameter.getField();
      recordRelationship(field, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
    } else if (element instanceof PrefixElement) {
      recordImportElementReferenceWithPrefix(node);
    } else if (element instanceof PropertyAccessorElement || element instanceof MethodElement) {
      location = getLocationWithTypeAssignedToField(node, element, location);
      if (node.isQualified()) {
        recordRelationship(element, IndexConstants.IS_REFERENCED_BY_QUALIFIED, location);
      } else {
        recordRelationship(element, IndexConstants.IS_REFERENCED_BY_UNQUALIFIED, location);
      }
    } else if (element instanceof ParameterElement || element instanceof LocalVariableElement) {
      boolean inGetterContext = node.inGetterContext();
      boolean inSetterContext = node.inSetterContext();
      if (inGetterContext && inSetterContext) {
        recordRelationship(element, IndexConstants.IS_READ_WRITTEN_BY, location);
      } else if (inGetterContext) {
        recordRelationship(element, IndexConstants.IS_READ_BY, location);
      } else if (inSetterContext) {
        recordRelationship(element, IndexConstants.IS_WRITTEN_BY, location);
      } else {
        recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
      }
    }
    recordImportElementReferenceWithoutPrefix(node);
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    ConstructorElement element = node.getStaticElement();
    Location location;
    if (node.getConstructorName() != null) {
      int start = node.getPeriod().getOffset();
      int end = node.getConstructorName().getEnd();
      location = createLocationFromOffset(start, end - start);
    } else {
      int start = node.getKeyword().getEnd();
      location = createLocationFromOffset(start, 0);
    }
    recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
    return super.visitSuperConstructorInvocation(node);
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    VariableDeclarationList variables = node.getVariables();
    for (VariableDeclaration variableDeclaration : variables.getVariables()) {
      Element element = variableDeclaration.getElement();
      recordElementDefinition(element, IndexConstants.DEFINES_VARIABLE);
    }
    return super.visitTopLevelVariableDeclaration(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    TypeParameterElement element = node.getElement();
    enterScope(element);
    try {
      return super.visitTypeParameter(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    VariableElement element = node.getElement();
    // record declaration
    {
      SimpleIdentifier name = node.getName();
      Location location = createLocationFromNode(name);
      location = getLocationWithExpressionType(location, node.getInitializer());
      recordRelationship(element, IndexConstants.IS_DEFINED_BY, location);
    }
    // visit
    enterScope(element);
    try {
      return super.visitVariableDeclaration(node);
    } finally {
      exitScope();
    }
  }

  @Override
  public Void visitVariableDeclarationList(VariableDeclarationList node) {
    NodeList<VariableDeclaration> variables = node.getVariables();
    if (variables != null) {
      // use first VariableDeclaration as Element for Location(s) in type
      {
        TypeName type = node.getType();
        if (type != null) {
          for (VariableDeclaration variableDeclaration : variables) {
            enterScope(variableDeclaration.getElement());
            try {
              type.accept(this);
            } finally {
              exitScope();
            }
            // only one iteration
            break;
          }
        }
      }
      // visit variables
      variables.accept(this);
    }
    return null;
  }

  /**
   * Record the given relationship between the given {@link Element} and {@link Location}.
   */
  protected void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element != null && location != null) {
      store.recordRelationship(element, relationship, location);
    }
  }

  /**
   * @return the {@link Location} representing location of the {@link AstNode}.
   */
  private Location createLocationFromNode(AstNode node) {
    return createLocationFromOffset(node.getOffset(), node.getLength());
  }

  /**
   * @param offset the offset of the location within {@link Source}
   * @param length the length of the location
   * @return the {@link Location} representing the given offset and length within the inner-most
   *         {@link Element}.
   */
  private Location createLocationFromOffset(int offset, int length) {
    Element element = peekElement();
    return new Location(element, offset, length);
  }

  /**
   * @return the {@link Location} representing location of the {@link Token}.
   */
  private Location createLocationFromToken(Token token) {
    return createLocationFromOffset(token.getOffset(), token.getLength());
  }

  /**
   * Exit the current scope.
   */
  private void exitScope() {
    elementStack.removeFirst();
  }

  /**
   * @return {@code true} if given node already indexed as more interesting reference, so it should
   *         not be indexed again.
   */
  private boolean isAlreadyHandledName(SimpleIdentifier node) {
    AstNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      return ((MethodInvocation) parent).getMethodName() == node;
    }
    return false;
  }

  /**
   * Records the {@link Element} definition in the library and universe.
   */
  private void recordElementDefinition(Element element, Relationship relationship) {
    Location location = createLocation(element);
    recordRelationship(libraryElement, relationship, location);
    recordRelationship(IndexConstants.UNIVERSE, relationship, location);
  }

  /**
   * Records {@link ImportElement} reference if given {@link SimpleIdentifier} references some
   * top-level element and not qualified with import prefix.
   */
  private void recordImportElementReferenceWithoutPrefix(SimpleIdentifier node) {
    if (isIdentifierInImportCombinator(node)) {
      return;
    }
    if (isIdentifierInPrefixedIdentifier(node)) {
      return;
    }
    Element element = node.getStaticElement();
    ImportElement importElement = internalGetImportElement(
        libraryElement,
        null,
        element,
        importElementsMap);
    if (importElement != null) {
      Location location = createLocationFromOffset(node.getOffset(), 0);
      recordRelationship(importElement, IndexConstants.IS_REFERENCED_BY, location);
    }
  }

  /**
   * Records {@link ImportElement} that declares given prefix and imports library with element used
   * with given prefix node.
   */
  private void recordImportElementReferenceWithPrefix(SimpleIdentifier prefixNode) {
    ImportElementInfo info = getImportElementInfo(prefixNode);
    if (info != null) {
      int offset = prefixNode.getOffset();
      int length = info.periodEnd - offset;
      Location location = createLocationFromOffset(offset, length);
      recordRelationship(info.element, IndexConstants.IS_REFERENCED_BY, location);
    }
  }

  /**
   * Records reference to defining {@link CompilationUnitElement} of the given
   * {@link LibraryElement}.
   */
  private void recordLibraryReference(UriBasedDirective node, LibraryElement library) {
    if (library != null) {
      Location location = createLocationFromNode(node.getUri());
      recordRelationship(
          library.getDefiningCompilationUnit(),
          IndexConstants.IS_REFERENCED_BY,
          location);
    }
  }

  /**
   * Record reference to the given operator {@link Element} and name.
   */
  private void recordOperatorReference(Token operator, Element element) {
    // prepare location
    Location location = createLocationFromToken(operator);
    // record name reference
    {
      String name = operator.getLexeme();
      if (name.equals("++")) {
        name = "+";
      }
      if (name.equals("--")) {
        name = "-";
      }
      if (StringUtilities.endsWithChar(name, '=') && !name.equals("==")) {
        name = name.substring(0, name.length() - 1);
      }
      Element nameElement = new NameElementImpl(name);
      Relationship relationship = element != null
          ? IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED
          : IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED;
      recordRelationship(nameElement, relationship, location);
    }
    // record element reference
    if (element != null) {
      recordRelationship(element, IndexConstants.IS_INVOKED_BY_QUALIFIED, location);
    }
  }

  /**
   * Records reference if the given {@link SimpleIdentifier} looks like a qualified property access
   * or method invocation.
   */
  private void recordQualifiedMemberReference(SimpleIdentifier node, Element element,
      Element nameElement, Location location) {
    if (node.isQualified()) {
      Relationship relationship = element != null
          ? IndexConstants.IS_REFERENCED_BY_QUALIFIED_RESOLVED
          : IndexConstants.IS_REFERENCED_BY_QUALIFIED_UNRESOLVED;
      recordRelationship(nameElement, relationship, location);
    }
  }

  /**
   * Records extends/implements relationships between given {@link ClassElement} and {@link Type} of
   * "superNode".
   */
  private void recordSuperType(TypeName superNode, Relationship relationship) {
    if (superNode != null) {
      Identifier superName = superNode.getName();
      if (superName != null) {
        Element superElement = superName.getStaticElement();
        recordRelationship(superElement, relationship, createLocationFromNode(superNode));
      }
    }
  }
}
