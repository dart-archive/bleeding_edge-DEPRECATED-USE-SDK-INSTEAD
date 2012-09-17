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
package com.google.dart.tools.core.internal.index.contributor;

import com.google.common.base.Objects;
import com.google.dart.compiler.ast.ASTNodes;
import com.google.dart.compiler.ast.ASTVisitor;
import com.google.dart.compiler.ast.DartArrayAccess;
import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartDeclaration;
import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFieldDefinition;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionObjectInvocation;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartImportDirective;
import com.google.dart.compiler.ast.DartInvocation;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartMethodInvocation;
import com.google.dart.compiler.ast.DartNamedExpression;
import com.google.dart.compiler.ast.DartNewExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartPropertyAccess;
import com.google.dart.compiler.ast.DartRedirectConstructorInvocation;
import com.google.dart.compiler.ast.DartSourceDirective;
import com.google.dart.compiler.ast.DartStringLiteral;
import com.google.dart.compiler.ast.DartSuperConstructorInvocation;
import com.google.dart.compiler.ast.DartTypeNode;
import com.google.dart.compiler.ast.DartUnaryExpression;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.DartUnqualifiedInvocation;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.parser.Token;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.ConstructorElement;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.compiler.util.apache.StringUtils;
import com.google.dart.engine.utilities.io.PrintStringWriter;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.dom.PropertyDescriptorHelper;
import com.google.dart.tools.core.index.Element;
import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Relationship;
import com.google.dart.tools.core.index.Resource;
import com.google.dart.tools.core.internal.index.store.IndexStore;
import com.google.dart.tools.core.internal.index.util.ElementFactory;
import com.google.dart.tools.core.internal.index.util.ResourceFactory;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.utilities.bindings.BindingUtils;
import com.google.dart.tools.core.utilities.collections.IntStack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;

import java.util.ArrayList;

import javax.lang.model.element.TypeElement;

/**
 * Instances of the class <code>IndexContributor</code> visit an AST structure to compute the data
 * and relationships to be contributed to an index.
 */
public class IndexContributor extends ASTVisitor<Void> {
  /*
   * TODO(brianwilkerson) This class does not find or record implicit references, such as the
   * implicit invocation of toString() when an expression is embedded in a string interpolation
   * expression.
   */

  /**
   * @return the library import prefix name, may be <code>null</code>.
   */
  private static String getLibraryImportPrefix(DartNode node) {
    // "prefix.topLevelFunction()" looks as method invocation
    if (node.getParent() instanceof DartMethodInvocation) {
      DartMethodInvocation invocation = (DartMethodInvocation) node.getParent();
      if (invocation.getTarget() instanceof DartIdentifier) {
        DartIdentifier target = (DartIdentifier) invocation.getTarget();
        if (target.getElement() instanceof LibraryElement) {
          return target.getName();
        }
      }
    }
    // "prefix.Type"
    if (node.getParent() instanceof DartPropertyAccess) {
      DartPropertyAccess propertyAccess = (DartPropertyAccess) node.getParent();
      DartNode qualifier = propertyAccess.getQualifier();
      if (qualifier instanceof DartIdentifier && qualifier.getElement() instanceof LibraryElement) {
        return ((DartIdentifier) qualifier).getName();
      }
    }
    // no prefix
    return null;
  }

  /**
   * The index to which data and relationships will be contributed.
   */
  private IndexStore index;

  /**
   * The library containing the compilation unit that is being visited.
   */
  private DartLibrary library;

  /**
   * The compilation unit that is being visited.
   */
  private CompilationUnit compilationUnit;

  /**
   * A resource representing the compilation unit that defines the library containing the
   * compilation unit being visited. This can be the same as the compilation unit being visited.
   */
  private Resource libraryResource;

  /**
   * An element representing the compilation unit that defines the library containing the
   * compilation unit being visited.
   */
  private Element libraryElement;

  /**
   * A resource representing the compilation unit being visited.
   */
  private Resource compilationUnitResource;

  /**
   * An element representing the compilation unit being visited.
   */
  private Element compilationUnitElement;

  /**
   * A stack whose top element (the element with the largest index) is an element representing the
   * inner-most enclosing scope.
   */
  private ArrayList<Element> elementStack = new ArrayList<Element>();

  /**
   * The number of milliseconds taken to convert AST nodes or elements into core model elements.
   */
  private long bindingTime = 0L;

  /**
   * The number of relationships that have been added to the index by this contributor. Used for
   * debugging purposes.
   */
  private int relationshipCount = 0;

  /**
   * A writer used to produce tracing output. This is used so that all of the tracing output will
   * appear together in a single entry in the log.
   */
  private PrintStringWriter traceWriter;

  /**
   * The element id used for library elements.
   */
  private static final String LIBRARY_ELEMENT_ID = "#library";

  /**
   * A stack containing one value for each name scope that has been entered, where the values are a
   * count of the number of unnamed functions that have been found within that scope. These counts
   * are used to synthesize a name for those functions. The innermost scope is at the top of the
   * stack.
   */
  private IntStack unnamedFunctionCount = new IntStack();

  private DartImport[] libraryImports;

  /**
   * Initialize a newly created contributor to contribute data and relationships to the given index
   * while processing the AST structure associated with the given compilation unit.
   * 
   * @param index the index to which data and relationships will be contributed
   * @param compilationUnit the compilation unit that will be visited
   * @throws DartModelException if the library associated with the compilation unit does not have a
   *           defining compilation unit
   */
  public IndexContributor(IndexStore index, CompilationUnit compilationUnit)
      throws DartModelException {
    this.index = index;
    library = compilationUnit.getLibrary();
    this.compilationUnit = compilationUnit;
    libraryResource = getResource(library.getDefiningCompilationUnit());
    libraryElement = new Element(libraryResource, LIBRARY_ELEMENT_ID);
    libraryImports = library.getImports();
    compilationUnitResource = getResource(compilationUnit);
    compilationUnitElement = new Element(
        compilationUnitResource,
        ElementFactory.composeElementId(compilationUnit.getElementName()));
    if (DartCoreDebug.TRACE_INDEX_CONTRIBUTOR) {
      traceWriter = new PrintStringWriter();
      traceWriter.print("Contributions from ");
      traceWriter.println(compilationUnit.getElementName());
      traceWriter.println();
    }
  }

  public long getBindingTime() {
    return bindingTime;
  }

  /**
   * Return the number of relationships that have been added to the index by this contributor.
   * 
   * @return the number of relationships that have been added to the index by this contributor
   */
  public int getRelationshipCount() {
    return relationshipCount;
  }

  /**
   * Write any accumulated tracing information to the log.
   */
  public void logTrace() {
    if (traceWriter != null) {
      DartCore.logInformation(traceWriter.toString());
    }
  }

  @Override
  public Void visitArrayAccess(DartArrayAccess node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      // TODO(brianwilkerson) Find the real source range associated with the operator.
      DartExpression index = node.getKey();
      processMethodInvocation(
          index.getSourceInfo().getOffset() - 1,
          index.getSourceInfo().getLength() + 2,
          null,
          (MethodElement) element);
    } else {
      notFound("array access", node);
    }
    return super.visitArrayAccess(node);
  }

  @Override
  public Void visitBinaryExpression(DartBinaryExpression node) {
    Token operatorToken = node.getOperator();
    if (operatorToken.isUserDefinableOperator()) {
      com.google.dart.compiler.resolver.Element element = node.getElement();
      if (element instanceof MethodElement) {
        int offset = node.getOperatorOffset();
        int length = operatorToken.getSyntax().length();
        processMethodInvocation(offset, length, null, (MethodElement) element);
      } else {
        notFound("binary expression: " + operatorToken.getSyntax(), node);
      }
    }
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitClass(DartClass node) {
    enterScope(getElement(node));
    try {
      processClass(node);
      super.visitClass(node);
    } finally {
      exitScope();
    }
    return null;
  }

  @Override
  public Void visitField(DartField node) {
    enterScope(getElement(node));
    try {
      processField(node.getName(), node.getElement());
      super.visitField(node);
    } finally {
      exitScope();
    }
    return null;
  }

  @Override
  public Void visitFieldDefinition(DartFieldDefinition node) {
    // TODO(brianwilkerson) We need to decide how we're going to represent references to the type:
    // one reference from the class that contains the field declaration, or one reference for each
    // field being declared.
    DartTypeNode type = node.getTypeNode();
    if (type != null) {
      type.accept(this);
    }
    for (DartField field : node.getFields()) {
      visitField(field);
    }
    return null;
  }

  @Override
  public Void visitFunction(DartFunction node) {
    if (node.getParent() instanceof DartMethodDefinition) {
      super.visitFunction(node);
      return null;
    }
    enterScope(getElement(node));
    try {
      processFunction(node);
      super.visitFunction(node);
    } finally {
      exitScope();
    }
    return null;
  }

  @Override
  public Void visitFunctionObjectInvocation(DartFunctionObjectInvocation node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      processMethodInvocation(getIdentifier(node.getTarget()), (MethodElement) element);
    } else {
      notFound("function invocation: [" + (element == null ? "null" : element.toString()) + "] "
          + node.toString(), node);
    }
    return super.visitFunctionObjectInvocation(node);
  }

  @Override
  public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    enterScope(getElement(node));
    try {
      processFunctionTypeAlias(node);
      super.visitFunctionTypeAlias(node);
    } finally {
      exitScope();
    }
    return null;
  }

  @Override
  public Void visitIdentifier(DartIdentifier node) {
    if (isNameInDeclaration(node)) {
      return null;
    }
    com.google.dart.compiler.resolver.Element element = node.getElement();
    // no resolved Element, potential match
    if (element == null) {
      DartNode parent = node.getParent();
      if (parent instanceof DartMethodInvocation
          && ((DartMethodInvocation) parent).getFunctionName() == node) {
        Element indexElement = new Element(IndexConstants.DYNAMIC, node.getName());
        recordRelationship(
            indexElement,
            IndexConstants.IS_INVOKED_BY_QUALIFIED,
            createLocation(node));
      }
      if (parent instanceof DartPropertyAccess && ((DartPropertyAccess) parent).getName() == node) {
        DartPropertyAccess propertyAccess = (DartPropertyAccess) parent;
        boolean inGetterContext = ASTNodes.inGetterContext(propertyAccess);
        Element indexElement = new Element(IndexConstants.DYNAMIC, node.getName());
        Location location = createLocation(node);
        if (inGetterContext) {
          recordRelationship(indexElement, IndexConstants.IS_ACCESSED_BY_QUALIFIED, location);
        } else {
          recordRelationship(indexElement, IndexConstants.IS_MODIFIED_BY_QUALIFIED, location);
        }
      }
    }
    // analyze Element
    if (element instanceof ClassElement) {
      processTypeReference(node, ((ClassElement) element).getType());
    } else if (element instanceof FieldElement) {
      FieldElement fieldElement = (FieldElement) element;
      DartNode propertyAccess = ASTNodes.getPropertyAccessNode(node);
      boolean inGetterContext = ASTNodes.inGetterContext(propertyAccess);
      Element indexElement = getElement(fieldElement, inGetterContext, true);
      Location location = createLocation(node);
      if (inGetterContext) {
        if (fieldElement.getGetter() != null) {
          processMethodInvocation(node, fieldElement.getGetter());
        } else if (isQualified(node)) {
          recordRelationship(indexElement, IndexConstants.IS_ACCESSED_BY_QUALIFIED, location);
        } else {
          recordRelationship(indexElement, IndexConstants.IS_ACCESSED_BY_UNQUALIFIED, location);
        }
      } else {
        if (fieldElement.getSetter() != null) {
          processMethodInvocation(node, fieldElement.getSetter());
        } else if (isQualified(node)) {
          recordRelationship(indexElement, IndexConstants.IS_MODIFIED_BY_QUALIFIED, location);
        } else {
          recordRelationship(indexElement, IndexConstants.IS_MODIFIED_BY_UNQUALIFIED, location);
        }
      }
    } else if (element instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) element;
      // record only getter/setter here, there are special handlers for explicit invocations
      if (methodElement.getModifiers().isGetter() || methodElement.getModifiers().isSetter()) {
        processMethodInvocation(node, (MethodElement) element);
      } else if (element instanceof ConstructorElement) {
        // constructors references are recorded in DartNewExpression
      } else if (!isExplicitInvocation(node)) {
        Element indexElement = getElement(methodElement);
        Location location = createLocation(node);
        Relationship relationship = isQualified(node) ? IndexConstants.IS_ACCESSED_BY_QUALIFIED
            : IndexConstants.IS_ACCESSED_BY_UNQUALIFIED;
        recordRelationship(indexElement, relationship, location);
      }
    } else if (element instanceof LibraryElement) {
      LibraryElement importLibraryElement = (LibraryElement) element;
      recordImportReference(importLibraryElement, node.getName(), node.getSourceInfo().getOffset());
    }
    recordImportReference_noPrefix(node);

    return super.visitIdentifier(node);
  }

  @Override
  public Void visitImportDirective(DartImportDirective node) {
    recordResourceReference(node.getLibraryUri());
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    if (node.getParent() instanceof DartField) {
      super.visitMethodDefinition(node);
      return null;
    }
    enterScope(getElement(node));
    try {
      processMethodDefinition(node);
      super.visitMethodDefinition(node);
    } finally {
      exitScope();
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(DartMethodInvocation node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element == null) {
      element = node.getElement();
    }
    if (element instanceof MethodElement) {
      processMethodInvocation(node.getFunctionName(), (MethodElement) element);
    } else {
      notFound("method invocation: " + node.toString(), node);
    }
    return super.visitMethodInvocation(node);
  }

  @Override
  public Void visitNamedExpression(DartNamedExpression node) {
    if (node.getParent() instanceof DartInvocation
        && node.getParent().getElement() instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) node.getParent().getElement();
      Object parameterId = node.getInvocationParameterId();
      if (parameterId instanceof VariableElement) {
        String name = ((VariableElement) parameterId).getName();
        try {
          Element indexElement = ElementFactory.getParameterElement(methodElement, name);
          Location location = createLocation(node.getName());
          recordRelationship(indexElement, IndexConstants.IS_REFERENCED_BY, location);
        } catch (DartModelException exception) {
        }
      }
    }
    return super.visitNamedExpression(node);
  }

  @Override
  public Void visitNewExpression(DartNewExpression node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) element;
      DartNode name = node.getConstructor();
      recordRelationship(
          getElement(methodElement),
          IndexConstants.IS_INVOKED_BY_UNQUALIFIED,
          getConstructorNameLocation(name));
    } else {
      notFound("new expression", node);
    }
    return super.visitNewExpression(node);
  }

  @Override
  public Void visitRedirectConstructorInvocation(DartRedirectConstructorInvocation node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      processMethodInvocation(node.getName(), (MethodElement) element);
    } else {
      notFound("redirect constructor invocation", node);
    }
    return super.visitRedirectConstructorInvocation(node);
  }

  @Override
  public Void visitSourceDirective(DartSourceDirective node) {
    recordResourceReference(node.getSourceUri());
    return super.visitSourceDirective(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(DartSuperConstructorInvocation node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      // TODO(brianwilkerson) The name is always null, so we can't record references to the invoked
      // constructor.
      processMethodInvocation(node.getName(), (MethodElement) element);
    } else {
      notFound("super constructor invocation", node);
    }
    return super.visitSuperConstructorInvocation(node);
  }

  @Override
  public Void visitUnaryExpression(DartUnaryExpression node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      int offset = node.getOperatorOffset();
      int length = node.getOperator().getSyntax().length();
      String prefix = getLibraryImportPrefix(node);
      processMethodInvocation(offset, length, prefix, (MethodElement) element);
    } else {
      notFound("unary expression: " + node.getOperator().getSyntax(), node);
    }
    return super.visitUnaryExpression(node);
  }

  @Override
  public Void visitUnit(DartUnit node) {
    unnamedFunctionCount.push(0);
    try {
      super.visitUnit(node);
    } finally {
      unnamedFunctionCount.pop();
      if (!unnamedFunctionCount.isEmpty()) {
        DartCore.logError(unnamedFunctionCount.size()
            + " scopes entered but not exited while visiting " + compilationUnit.getElementName());
        unnamedFunctionCount.clear();
      }
    }
    return null;
  }

  @Override
  public Void visitUnqualifiedInvocation(DartUnqualifiedInvocation node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      processMethodInvocation(node.getTarget(), (MethodElement) element);
    } else if (element instanceof FieldElement) {
      processField(node.getTarget(), (FieldElement) element);
    } else if (element instanceof VariableElement) {
      processVariable(node.getTarget(), (VariableElement) element);
    } else {
      notFound("unqualified invocation", node);
    }
    return super.visitUnqualifiedInvocation(node);
  }

  /**
   * Create a location representing the location of the given node within the inner-most element.
   * 
   * @param node the node representing the source range of the location
   * @return the location that was created
   */
  private Location createLocation(DartNode node) {
    String prefix = getLibraryImportPrefix(node);
    return createLocation(
        node.getSourceInfo().getOffset(),
        node.getSourceInfo().getLength(),
        prefix);
  }

  /**
   * Create a location representing the given offset and length within the inner-most element.
   * 
   * @param offset the offset of the location
   * @param length the length of the location
   * @param prefix the import prefix of top-level element, may be <code>null</code>
   * @return the location that was created
   */
  private Location createLocation(int offset, int length, String prefix) {
    Element element = peekElement();
    if (element == null) {
      element = compilationUnitElement;
    }
    return new Location(element, offset, length, prefix);
  }

  /**
   * Return a location representing the location of the name of the given type.
   * 
   * @param node the node representing the declaration of the type
   * @return a location representing the location of the name of the given type
   */
  private Location createNameLocation(DartClass node) {
    return createNameLocation(node.getName());
  }

  /**
   * @return the {@link Location} representing location of the {@link DartExpression} used as name.
   */
  private Location createNameLocation(DartExpression name) {
    SourceInfo nameSourceInfo = name.getSourceInfo();
    return createLocation(nameSourceInfo.getOffset(), nameSourceInfo.getLength(), null);
  }

  /**
   * Return a location representing the location of the name of the given function.
   * 
   * @param node the node representing the declaration of the function
   * @return a location representing the location of the name of the given function
   */
  private Location createNameLocation(DartFunction node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      MethodElement method = (MethodElement) element;
      SourceInfo location = method.getNameLocation();
      return createLocation(location.getOffset(), location.getLength(), null);
    }
    return null;
  }

  /**
   * Return a location representing the location of the name of the given function type.
   * 
   * @param node the node representing the declaration of the function type
   * @return a location representing the location of the name of the given function type
   */
  private Location createNameLocation(DartFunctionTypeAlias node) {
    return createNameLocation(node.getName());
  }

  /**
   * Return a location representing the location of the name of the given method.
   * 
   * @param node the node representing the declaration of the method
   * @return a location representing the location of the name of the given method
   */
  private Location createNameLocation(DartMethodDefinition node) {
    return createNameLocation(node.getName());
  }

  /**
   * Enter a new scope represented by the given element.
   * 
   * @param element the element of the scope being entered
   */
  private void enterScope(Element element) {
    elementStack.add(element);
    unnamedFunctionCount.push(0);
  }

  /**
   * Exit the current scope.
   */
  private void exitScope() {
    elementStack.remove(elementStack.size() - 1);
    unnamedFunctionCount.pop();
  }

  /**
   * Return the method defined in the given type that matches the given method.
   * 
   * @param method the method being matched against
   * @param type the type in which the candidate methods are declared
   * @return the method defined in the type that matches the method
   */
  private MethodElement findMatchingMethod(MethodElement method, ClassElement type) {
    if (method instanceof ConstructorElement) {
      for (ConstructorElement candidateMethod : type.getConstructors()) {
        if (matches(method, candidateMethod)) {
          return candidateMethod;
        }
      }
    } else {
      for (com.google.dart.compiler.resolver.Element member : type.getMembers()) {
        if (member instanceof MethodElement) {
          MethodElement candidateMethod = (MethodElement) member;
          if (matches(method, candidateMethod)) {
            return candidateMethod;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return the method that the given method overrides, or <code>null</code> if the given method
   * does not override another method.
   * 
   * @param method the method that might override another method
   * @return the method that the given method overrides
   */
  private MethodElement findOverriddenMethod(MethodElement method) {
    com.google.dart.compiler.resolver.Element enclosingElement = method.getEnclosingElement();
    if (!(enclosingElement instanceof ClassElement)) {
      // The element represents a function, and functions cannot override other functions.
      return null;
    }
    ClassElement superclass = getSuperclass((ClassElement) enclosingElement);
    while (superclass != null) {
      MethodElement matchingMethod = findMatchingMethod(method, superclass);
      if (matchingMethod != null) {
        return matchingMethod;
      }
      superclass = getSuperclass(superclass);
    }
    return null;
  }

  /**
   * @return the "Type[.name]" location for given "Type", "Type.name" or "prefix.Type.name".
   */
  private Location getConstructorNameLocation(DartNode constructorName) {
    // prepare Type or prefix.Type
    DartTypeNode typeNode;
    if (constructorName instanceof DartTypeNode) {
      typeNode = (DartTypeNode) constructorName;
    } else {
      DartPropertyAccess pa = (DartPropertyAccess) constructorName;
      typeNode = (DartTypeNode) pa.getQualifier();
    }
    // prepare Type name start
    int typeNameStart;
    {
      DartNode typeNameNode = typeNode.getIdentifier();
      if (typeNameNode instanceof DartIdentifier) {
        typeNameStart = typeNameNode.getSourceInfo().getOffset();
      } else {
        DartPropertyAccess pa = (DartPropertyAccess) typeNameNode;
        typeNameStart = pa.getName().getSourceInfo().getOffset();
      }
    }
    // create location
    int constructorNameEnd = constructorName.getSourceInfo().getEnd();
    String prefix = getLibraryImportPrefix(typeNode);
    return createLocation(typeNameStart, constructorNameEnd - typeNameStart, prefix);
  }

  /**
   * Return an element representing the given type, or <code>null</code> if the given type cannot be
   * represented as an element.
   * 
   * @param element the type element to be represented
   * @return an element representing the given type
   */
  private Element getElement(ClassElement element) {
    try {
      return ElementFactory.getElement(element);
    } catch (DartModelException exception) {
      DartCore.logError("Could not getElement for class element " + element.getName(), exception);
    }
    return null;
  }

  /**
   * Return an element representing the given type.
   * 
   * @param node the node representing the declaration of the type
   * @return an element representing the given type
   */
  private Element getElement(DartClass node) {
    return new Element(
        compilationUnitResource,
        ElementFactory.composeElementId(node.getClassName()));
  }

  /**
   * Return an element representing the given field.
   * 
   * @param node the node representing the declaration of the field
   * @return an element representing the given field
   */
  private Element getElement(DartField node) {
    return new Element(compilationUnitResource, ElementFactory.composeElementId(
        peekElement(),
        node.getName().getName()));
  }

  /**
   * Return an element representing the given function.
   * 
   * @param node the node representing the declaration of the function
   * @return an element representing the given function
   */
  private Element getElement(DartFunction node) {
    String functionName = null;
    DartNode parent = node.getParent();
    if (parent instanceof DartFunctionExpression) {
      functionName = ((DartFunctionExpression) parent).getFunctionName();
    }
    if (functionName == null) {
      functionName = Integer.toString(unnamedFunctionCount.peek());
      unnamedFunctionCount.increment(1);
    }
    return new Element(compilationUnitResource, ElementFactory.composeElementId(
        peekElement(),
        functionName));
  }

  /**
   * Return an element representing the given function type.
   * 
   * @param node the node representing the declaration of the function type
   * @return an element representing the given function type
   */
  private Element getElement(DartFunctionTypeAlias node) {
    return new Element(
        compilationUnitResource,
        ElementFactory.composeElementId(node.getName().getName()));
  }

  /**
   * Return an element representing the given method.
   * 
   * @param node the node representing the declaration of the method
   * @return an element representing the given method
   */
  private Element getElement(DartMethodDefinition node) {
    return new Element(compilationUnitResource, ElementFactory.composeElementId(
        peekElement(),
        toString(node.getName())));
  }

  /**
   * Return an element representing the given field.
   * 
   * @param element the field element to be represented
   * @param allowGetter <code>true</code> if a getter is allowed to be returned
   * @param allowSetter <code>true</code> if a setter is allowed to be returned
   * @return an element representing the given field
   */
  private Element getElement(FieldElement element, boolean allowGetter, boolean allowSetter) {
    try {
      return ElementFactory.getElement(element, allowGetter, allowSetter);
    } catch (DartModelException exception) {
      DartCore.logInformation(
          "Could not getElement for field element " + element.getName(),
          exception);
    }
    return null;
  }

  /**
   * Return an element representing the given type, or <code>null</code> if the given type cannot be
   * represented as an element.
   * 
   * @param node the node representing the declaration of the type
   * @return an element representing the given type
   */
  private Element getElement(InterfaceType type) {
    return getElement(type.getElement());
  }

  /**
   * Return an element representing the given method.
   * 
   * @param element the element representing the method
   * @return an element representing the given method
   */
  private Element getElement(MethodElement element) {
    try {
      return ElementFactory.getElement(element);
    } catch (DartModelException exception) {
      DartCore.logInformation(
          "Could not getElement for method element " + element.getName(),
          exception);
    }
    return null;
  }

  /**
   * Return the identifier associated with the given node, or <code>null</code> if there is no
   * identifier associated with the node.
   * 
   * @param node the node with which the identifier is associated
   * @return the identifier associated with the given node
   */
  private DartIdentifier getIdentifier(DartNode node) {
    if (node instanceof DartIdentifier) {
      return (DartIdentifier) node;
    } else if (node instanceof DartPropertyAccess) {
      return ((DartPropertyAccess) node).getName();
    } else if (node instanceof DartTypeNode) {
      return getIdentifier(((DartTypeNode) node).getIdentifier());
    }
    DartCore.logInformation("Could not getIdentifier for " + node.getClass().getName());
    return null;
  }

  /**
   * Return the resource representing the given compilation unit.
   * 
   * @param compilationUnit the compilation unit to be represented as a resource
   * @return the resource representing the given compilation unit
   */
  private Resource getResource(CompilationUnit compilationUnit) {
    if (compilationUnit != null) {
      try {
        return ResourceFactory.getResource(compilationUnit);
      } catch (DartModelException exception) {
        DartCore.logInformation("Could not get underlying resource for compilation unit "
            + compilationUnit.getElementName(), exception);
      }
    }
    return libraryResource;
  }

  /**
   * Return the superclass of the given class, or <code>null</code> if the given class does not have
   * a superclass or if the superclass cannot be determined.
   * 
   * @param classElement the class being accessed
   * @return the superclass of the given class
   */
  private ClassElement getSuperclass(ClassElement classElement) {
    InterfaceType superType = classElement.getSupertype();
    if (superType == null) {
      return null;
    }
    return superType.getElement();
  }

  private boolean isExplicitInvocation(DartIdentifier identifier) {
    DartNode parent = identifier.getParent();
    return (parent instanceof DartFunctionObjectInvocation && ((DartFunctionObjectInvocation) parent).getTarget() == identifier)
        || (parent instanceof DartMethodInvocation && ((DartMethodInvocation) parent).getFunctionName() == identifier)
        || (parent instanceof DartNewExpression && ((DartNewExpression) parent).getConstructor() == identifier)
        || (parent instanceof DartRedirectConstructorInvocation && ((DartRedirectConstructorInvocation) parent).getName() == identifier)
        || (parent instanceof DartSuperConstructorInvocation && ((DartSuperConstructorInvocation) parent).getName() == identifier)
        || (parent instanceof DartUnqualifiedInvocation && ((DartUnqualifiedInvocation) parent).getTarget() == identifier);
  }

  /**
   * Return <code>true</code> if the given identifier represents the name in a declaration of that
   * name.
   * 
   * @param node the identifier being tested
   * @return <code>true</code> if the given identifier is the name in a declaration
   */
  private boolean isNameInDeclaration(DartIdentifier node) {
    DartNode parent = node.getParent();
    if (parent instanceof DartDeclaration) {
      return ((DartDeclaration<?>) parent).getName() == node;
    }
    if (parent instanceof DartFunctionExpression) {
      return ((DartFunctionExpression) parent).getName() == node;
    }
    if (parent instanceof DartLabel) {
      return ((DartLabel) parent).getLabel() == node;
    }
    return false;
  }

  /**
   * @return <code>true</code> if given {@link DartIdentifier} is "name" part of qualified property
   *         access or method invocation.
   */
  private boolean isQualified(DartIdentifier node) {
    if (node.getParent() instanceof DartPropertyAccess) {
      return ((DartPropertyAccess) node.getParent()).getName() == node;
    }
    if (node.getParent() instanceof DartMethodInvocation) {
      return ((DartMethodInvocation) node.getParent()).getFunctionName() == node;
    }
    return false;
  }

  /**
   * Return <code>true</code> if the given candidate matches the given target.
   * 
   * @param targetMethod the method being matched against
   * @param candidateMethod the candidate being compared to the target
   * @return <code>true</code> if the candidate matches the target
   */
  private boolean matches(MethodElement targetMethod, MethodElement candidateMethod) {
    return targetMethod.getName().equals(candidateMethod.getName());
  }

  /**
   * If debugging is enabled, report that we were not able to find an element corresponding to the
   * given node.
   * 
   * @param string a description of what the node represents
   * @param node the node that should have had an element associated with it
   */
  private void notFound(String string, DartNode node) {
    if (node == null) {
      notFound(string, -1, 0);
    } else {
      notFound(string, node.getSourceInfo().getOffset(), node.getSourceInfo().getLength());
    }
  }

  /**
   * If debugging is enabled, report that we were not able to find an element corresponding to a
   * node at the given location.
   * 
   * @param string a description of what the node represents
   * @param offset the offset of the node that should have had an element associated with it
   * @param length the length of the node that should have had an element associated with it
   */
  private void notFound(String string, int offset, int length) {
    if (DartCoreDebug.DEBUG_INDEX_CONTRIBUTOR) {
      PrintStringWriter writer = new PrintStringWriter();
      writer.print(string);
      writer.print(" in ");
      writer.print(compilationUnit.getElementName());
      if (offset >= 0) {
        writer.print(" [");
        writer.print(offset);
        writer.print(", ");
        writer.print(offset + length - 1);
        writer.print("]");
      }
      DartCore.logInformation(writer.toString());
    }
  }

  /**
   * Return the element representing the inner-most enclosing scope, or <code>null</code> if we are
   * at the top-level of the compilation unit.
   * 
   * @return the element representing the inner-most enclosing scope
   */
  private Element peekElement() {
    for (int i = elementStack.size() - 1; i >= 0; i--) {
      Element element = elementStack.get(i);
      if (element != null) {
        return element;
      }
    }
    return null;
  }

  /**
   * Record any information implied by the given class definition.
   * 
   * @param node the node representing the definition of the class
   */
  private void processClass(DartClass node) {
    //
    // Record the class as being contained by the workspace and the library.
    //
    if (node.isInterface()) {
      recordRelationship(
          IndexConstants.UNIVERSE,
          IndexConstants.DEFINES_INTERFACE,
          createNameLocation(node));
      recordRelationship(libraryElement, IndexConstants.DEFINES_INTERFACE, createNameLocation(node));
    } else {
      recordRelationship(
          IndexConstants.UNIVERSE,
          IndexConstants.DEFINES_CLASS,
          createNameLocation(node));
      recordRelationship(libraryElement, IndexConstants.DEFINES_CLASS, createNameLocation(node));
    }
    //
    // Record the class as being a subtype of it's supertypes.
    //
    com.google.dart.compiler.resolver.Element binding = node.getElement();
    if (binding instanceof ClassElement) {
      ClassElement classElement = (ClassElement) binding;
      InterfaceType superclass = classElement.getSupertype();
      if (superclass != null) {
        processSupertype(node, superclass);
      }
      for (InterfaceType type : classElement.getInterfaces()) {
        processSupertype(node, type);
      }
    } else {
      notFound("unqualified invocation", node);
    }
  }

  /**
   * Record any information implied by the given field definition.
   * 
   * @param node the node representing the name of the field
   * @param element the element describing the field
   */
  private void processField(DartIdentifier node, FieldElement element) {
    Location location = createLocation(node);
    if (element.getEnclosingElement() instanceof TypeElement) {
      recordRelationship(libraryElement, IndexConstants.DEFINES_FIELD, location);
    } else {
      recordRelationship(libraryElement, IndexConstants.DEFINES_VARIABLE, location);
    }
  }

  /**
   * Record any information implied by the given function definition.
   * 
   * @param node the node representing the definition of the function
   */
  private void processFunction(DartFunction node) {
    recordRelationship(libraryElement, IndexConstants.DEFINES_FUNCTION, createNameLocation(node));
  }

  /**
   * Record any information implied by the given function type definition.
   * 
   * @param node the node representing the definition of the function type
   */
  private void processFunctionTypeAlias(DartFunctionTypeAlias node) {
    //
    // Record the function type as being contained by the workspace and the library.
    //
    recordRelationship(
        IndexConstants.UNIVERSE,
        IndexConstants.DEFINES_FUNCTION_TYPE,
        createNameLocation(node));
    recordRelationship(
        libraryElement,
        IndexConstants.DEFINES_FUNCTION_TYPE,
        createNameLocation(node));
  }

  /**
   * Record any information implied by the given method definition.
   * 
   * @param node the node representing the definition of the method
   */
  private void processMethodDefinition(DartMethodDefinition node) {
    recordRelationship(libraryElement, IndexConstants.DEFINES_METHOD, createNameLocation(node));
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) element;
      MethodElement overridenMethodElement = findOverriddenMethod(methodElement);
      if (overridenMethodElement != null) {
        recordRelationship(
            getElement(overridenMethodElement),
            IndexConstants.IS_OVERRIDDEN_BY,
            createNameLocation(node));
      }
      // add reference from unnamed constructor name to the ClassElement
      if (element instanceof ConstructorElement && "".equals(element.getName())) {
        ConstructorElement constructorElement = (ConstructorElement) element;
        ClassElement classElement = constructorElement.getConstructorType();
        if (Objects.equal(constructorElement.getRawName(), classElement.getName())) {
          processTypeReference((DartIdentifier) node.getName(), classElement.getType());
        }
      }
    } else {
      notFound("unqualified invocation", node);
    }
  }

  /**
   * Record the invocation of a method or function.
   * 
   * @param methodName the node representing the name of the method being invoked
   * @param binding the element representing the method being invoked
   */
  private void processMethodInvocation(DartIdentifier methodName, MethodElement binding) {
    if (methodName == null || binding == null) {
      notFound("method invocation", methodName);
      return;
    }
    Relationship relationship = isQualified(methodName) ? IndexConstants.IS_INVOKED_BY_QUALIFIED
        : IndexConstants.IS_INVOKED_BY_UNQUALIFIED;
    recordRelationship(getElement(binding), relationship, createLocation(methodName));
  }

  /**
   * Record the invocation of a method or function.
   * 
   * @param offset the offset of the name of the method being invoked
   * @param length the length of the name of the method being invoked
   * @param binding the element representing the method being invoked
   */
  private void processMethodInvocation(int offset, int length, String prefix, MethodElement binding) {
    if (binding == null) {
      notFound("method invocation", offset, length);
      return;
    }
    recordRelationship(
        getElement(binding),
        IndexConstants.IS_INVOKED_BY_QUALIFIED,
        createLocation(offset, length, prefix));
  }

  private void processSupertype(DartClass node, InterfaceType binding) {
    if (node.isInterface() == binding.getElement().isInterface()) {
      recordRelationship(
          getElement(binding),
          IndexConstants.IS_EXTENDED_BY,
          createNameLocation(node));
      recordRelationship(getElement(node), IndexConstants.EXTENDS, createNameLocation(node));
    } else {
      recordRelationship(
          getElement(binding),
          IndexConstants.IS_IMPLEMENTED_BY,
          createNameLocation(node));
      recordRelationship(getElement(node), IndexConstants.IMPLEMENTS, createNameLocation(node));
    }
  }

  /**
   * Process the fact that the given node is a reference to the given type.
   * 
   * @param node the node that is a reference to the type
   * @param type the type that is referenced by the node
   */
  private void processTypeReference(DartIdentifier node, InterfaceType type) {
    recordRelationship(getElement(type), IndexConstants.IS_REFERENCED_BY, createLocation(node));
  }

  /**
   * Record any information implied by the given variable definition.
   * 
   * @param node the node representing the name of the variable
   * @param element the element describing the variable
   */
  private void processVariable(DartIdentifier node, VariableElement element) {
  }

  /**
   * Records reference to the imported library from given location.
   * 
   * @param importLibraryElement the referenced {@link LibraryElement}, may be <code>null</code>
   * @param prefix the prefix of the import, may be <code>null</code>
   * @param offset the offset of the prefix
   */
  private void recordImportReference(LibraryElement importLibraryElement, String prefix, int offset) {
    try {
      DartLibrary importLibraryModel = BindingUtils.getDartElement(importLibraryElement);
      for (DartImport imprt : libraryImports) {
        if (Objects.equal(imprt.getLibrary(), importLibraryModel)
            && Objects.equal(imprt.getPrefix(), prefix)) {
          String imprtId = ElementFactory.composeElementId(imprt.getPrefix() + ":"
              + imprt.getLibrary().getElementName());
          Element imprtElement = new Element(libraryResource, imprtId);
          int length = StringUtils.length(prefix);
          Location location = createLocation(offset, length, null);
          recordRelationship(imprtElement, IndexConstants.IS_REFERENCED_BY, location);
          break;
        }
      }
    } catch (Throwable e) {
      DartCore.logInformation("Could not record reference to library import "
          + importLibraryElement, e);
    }
  }

  /**
   * Records {@link DartImport} reference if given {@link DartNode} references some top-level
   * element and not qualified with import prefix.
   */
  private void recordImportReference_noPrefix(DartIdentifier node) {
    com.google.dart.compiler.resolver.Element element = node.getElement();
    if (element != null
        && element.getEnclosingElement() instanceof LibraryElement
        && PropertyDescriptorHelper.getLocationInParent(node) != PropertyDescriptorHelper.DART_PROPERTY_ACCESS_NAME) {
      LibraryElement importLibraryElement = (LibraryElement) element.getEnclosingElement();
      recordImportReference(importLibraryElement, null, node.getSourceInfo().getOffset());
    }
  }

  /**
   * Record the given relationship between the given element and the given location.
   * 
   * @param element the element that has the given relationship with the location
   * @param relationship the relationship between the element and the location
   * @param location the location that is related to the element
   */
  private void recordRelationship(Element element, Relationship relationship, Location location) {
    if (element != null) {
      index.recordRelationship(compilationUnitResource, element, relationship, location);
      relationshipCount++;
      if (traceWriter != null) {
        traceWriter.print("   ");
        traceWriter.print(element);
        traceWriter.print(" ");
        traceWriter.print(relationship);
        traceWriter.print(" ");
        traceWriter.print(location);
      }
    }
  }

  private void recordResourceReference(DartStringLiteral uriLiteral) {
    if (uriLiteral != null) {
      try {
        String uriString = uriLiteral.getValue();
        Path uriPath = new Path(uriString);
        if (uriPath.isAbsolute()) {
          // If the resource isn't in the workspace, then we can't record the reference.
          DartCore.logInformation("Could not record resource reference \"" + uriLiteral + "\"");
          return;
        }
        IFile libraryFile = (IFile) compilationUnit.getLibrary().getCorrespondingResource();
        if (libraryFile != null) {
          IFile resourceFile = libraryFile.getParent().getFile(uriPath);
          if (resourceFile != null && resourceFile.exists()) {
            Element element = new Element(ResourceFactory.getResource(resourceFile), "");
            Location location = createLocation(uriLiteral);
            recordRelationship(element, IndexConstants.IS_REFERENCED_BY, location);
          }
        }
      } catch (Throwable e) {
        // If the resource isn't in the workspace, then we can't record the reference.
        DartCore.logInformation("Could not record resource reference \"" + uriLiteral + "\"", e);
      }
    }
  }

  /**
   * Convert the given node, which is assumed to represent either a simple or qualified name, into a
   * string corresponding to that name.
   * 
   * @param name the node representing the name to be converted
   * @return a string representing the given name
   */
  private String toString(DartNode name) {
    if (name instanceof DartIdentifier) {
      return ((DartIdentifier) name).getName();
    } else if (name instanceof DartPropertyAccess) {
      DartPropertyAccess access = (DartPropertyAccess) name;
      return toString(access.getQualifier()) + "." + access.getPropertyName();
    }
    return "";
  }
}
