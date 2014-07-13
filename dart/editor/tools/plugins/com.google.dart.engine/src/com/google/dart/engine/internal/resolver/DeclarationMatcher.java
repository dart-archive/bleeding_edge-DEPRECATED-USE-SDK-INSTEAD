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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.EnumConstantDeclaration;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;

import java.util.HashSet;

/**
 * Instances of the class {@code DeclarationMatcher} determine whether the element model defined by
 * a given AST structure matches an existing element model.
 */
public class DeclarationMatcher extends RecursiveAstVisitor<Void> {
  /**
   * Instances of the class {@code DeclarationMismatchException} represent an exception that is
   * thrown when the element model defined by a given AST structure does not match an existing
   * element model.
   */
  private static class DeclarationMismatchException extends RuntimeException {
    public DeclarationMismatchException() {
      super();
    }
  }

  /**
   * The compilation unit containing the AST nodes being visited.
   */
  private CompilationUnitElement enclosingUnit;

  /**
   * The function type alias containing the AST nodes being visited, or {@code null} if we are not
   * in the scope of a function type alias.
   */
  private FunctionTypeAliasElement enclosingAlias;

  /**
   * The class containing the AST nodes being visited, or {@code null} if we are not in the scope of
   * a class.
   */
  private ClassElement enclosingClass;

  /**
   * The method or function containing the AST nodes being visited, or {@code null} if we are not in
   * the scope of a method or function.
   */
  private ExecutableElement enclosingExecutable;

  /**
   * The parameter containing the AST nodes being visited, or {@code null} if we are not in the
   * scope of a parameter.
   */
  private ParameterElement enclosingParameter;

  /**
   * A set containing all of the elements in the element model that were defined by the old AST node
   * corresponding to the AST node being visited.
   */
  private HashSet<Element> allElements = new HashSet<Element>();

  /**
   * A set containing all of the elements in the element model that were defined by the old AST node
   * corresponding to the AST node being visited that have not already been matched to nodes in the
   * AST structure being visited.
   */
  private HashSet<Element> unmatchedElements = new HashSet<Element>();

  /**
   * Initialize a newly created visitor.
   */
  public DeclarationMatcher() {
    super();
  }

  /**
   * Return {@code true} if the declarations within the given AST structure define an element model
   * that is equivalent to the corresponding elements rooted at the given element.
   * 
   * @param node the AST structure being compared to the element model
   * @param element the root of the element model being compared to the AST structure
   * @return {@code true} if the AST structure defines the same elements as those in the given
   *         element model
   */
  public boolean matches(AstNode node, Element element) {
    captureEnclosingElements(element);
    gatherElements(element);
    try {
      node.accept(this);
    } catch (DeclarationMismatchException exception) {
      return false;
    }
    return unmatchedElements.isEmpty();
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    SimpleIdentifier exceptionParameter = node.getExceptionParameter();
    if (exceptionParameter != null) {
      LocalVariableElement[] localVariables = enclosingExecutable.getLocalVariables();
      LocalVariableElement exceptionElement = findIdentifier(localVariables, exceptionParameter);
      processElement(exceptionElement);

      SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
      if (stackTraceParameter != null) {
        LocalVariableElement stackTraceElement = findIdentifier(localVariables, stackTraceParameter);
        processElement(stackTraceElement);
      }
    }
    return super.visitCatchClause(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement outerClass = enclosingClass;
    try {
      SimpleIdentifier className = node.getName();
      enclosingClass = findIdentifier(enclosingUnit.getTypes(), className);
      processElement(enclosingClass);
      if (!hasConstructor(node)) {
        ConstructorElement constructor = enclosingClass.getUnnamedConstructor();
        if (constructor.isSynthetic()) {
          processElement(constructor);
        }
      }
      return super.visitClassDeclaration(node);
    } finally {
      enclosingClass = outerClass;
    }
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    ClassElement outerClass = enclosingClass;
    try {
      SimpleIdentifier className = node.getName();
      enclosingClass = findIdentifier(enclosingUnit.getTypes(), className);
      processElement(enclosingClass);
      return super.visitClassTypeAlias(node);
    } finally {
      enclosingClass = outerClass;
    }
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    processElement(enclosingUnit);
    return super.visitCompilationUnit(node);
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    ExecutableElement outerExecutable = enclosingExecutable;
    try {
      SimpleIdentifier constructorName = node.getName();
      if (constructorName == null) {
        enclosingExecutable = enclosingClass.getUnnamedConstructor();
      } else {
        enclosingExecutable = enclosingClass.getNamedConstructor(constructorName.getName());
      }
      processElement(enclosingExecutable);
      return super.visitConstructorDeclaration(node);
    } finally {
      enclosingExecutable = outerExecutable;
    }
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    SimpleIdentifier variableName = node.getIdentifier();
    LocalVariableElement element = findIdentifier(
        enclosingExecutable.getLocalVariables(),
        variableName);
    processElement(element);
    return super.visitDeclaredIdentifier(node);
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    SimpleIdentifier parameterName = node.getParameter().getIdentifier();
    ParameterElement element = getElementForParameter(node, parameterName);
    Expression defaultValue = node.getDefaultValue();
    if (defaultValue != null) {
      ExecutableElement outerExecutable = enclosingExecutable;
      try {
        if (element == null) {
          // TODO(brianwilkerson) Report this internal error.
        } else {
          enclosingExecutable = element.getInitializer();
        }
        defaultValue.accept(this);
      } finally {
        enclosingExecutable = outerExecutable;
      }
      processElement(enclosingExecutable);
    }
    ParameterElement outerParameter = enclosingParameter;
    try {
      enclosingParameter = element;
      processElement(enclosingParameter);
      return super.visitDefaultFormalParameter(node);
    } finally {
      enclosingParameter = outerParameter;
    }
  }

  @Override
  public Void visitEnumDeclaration(EnumDeclaration node) {
    ClassElement enclosingEnum = findIdentifier(enclosingUnit.getEnums(), node.getName());
    processElement(enclosingEnum);
    FieldElement[] constants = enclosingEnum.getFields();
    for (EnumConstantDeclaration constant : node.getConstants()) {
      FieldElement constantElement = findIdentifier(constants, constant.getName());
      processElement(constantElement);
    }
    return super.visitEnumDeclaration(node);
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    String uri = getStringValue(node.getUri());
    if (uri != null) {
      LibraryElement library = enclosingUnit.getLibrary();
      ExportElement exportElement = findExport(
          library.getExports(),
          enclosingUnit.getContext().getSourceFactory().resolveUri(enclosingUnit.getSource(), uri));
      processElement(exportElement);
    }
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElement element = getElementForParameter(node, parameterName);
      ParameterElement outerParameter = enclosingParameter;
      try {
        enclosingParameter = element;
        processElement(enclosingParameter);
        return super.visitFieldFormalParameter(node);
      } finally {
        enclosingParameter = outerParameter;
      }
    } else {
      return super.visitFieldFormalParameter(node);
    }
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerExecutable = enclosingExecutable;
    try {
      SimpleIdentifier functionName = node.getName();
      Token property = node.getPropertyKeyword();
      if (property == null) {
        if (enclosingExecutable != null) {
          enclosingExecutable = findIdentifier(enclosingExecutable.getFunctions(), functionName);
        } else {
          enclosingExecutable = findIdentifier(enclosingUnit.getFunctions(), functionName);
        }
      } else {
        PropertyAccessorElement accessor = findIdentifier(
            enclosingUnit.getAccessors(),
            functionName);
        if (((KeywordToken) property).getKeyword() == Keyword.SET) {
          accessor = accessor.getVariable().getSetter();
        }
        enclosingExecutable = accessor;
      }
      processElement(enclosingExecutable);
      return super.visitFunctionDeclaration(node);
    } finally {
      enclosingExecutable = outerExecutable;
    }
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    if (!(node.getParent() instanceof FunctionDeclaration)) {
      FunctionElement element = findAtOffset(
          enclosingExecutable.getFunctions(),
          node.getBeginToken().getOffset());
      processElement(element);
    }
    ExecutableElement outerExecutable = enclosingExecutable;
    try {
      enclosingExecutable = node.getElement();
      processElement(enclosingExecutable);
      return super.visitFunctionExpression(node);
    } finally {
      enclosingExecutable = outerExecutable;
    }
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    FunctionTypeAliasElement outerAlias = enclosingAlias;
    try {
      SimpleIdentifier aliasName = node.getName();
      enclosingAlias = findIdentifier(enclosingUnit.getFunctionTypeAliases(), aliasName);
      processElement(enclosingAlias);
      return super.visitFunctionTypeAlias(node);
    } finally {
      enclosingAlias = outerAlias;
    }
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElement element = getElementForParameter(node, parameterName);
      ParameterElement outerParameter = enclosingParameter;
      try {
        enclosingParameter = element;
        processElement(enclosingParameter);
        return super.visitFunctionTypedFormalParameter(node);
      } finally {
        enclosingParameter = outerParameter;
      }
    } else {
      return super.visitFunctionTypedFormalParameter(node);
    }
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    String uri = getStringValue(node.getUri());
    if (uri != null) {
      LibraryElement library = enclosingUnit.getLibrary();
      ImportElement importElement = findImport(
          library.getImports(),
          enclosingUnit.getContext().getSourceFactory().resolveUri(enclosingUnit.getSource(), uri),
          node.getPrefix());
      processElement(importElement);
    }
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElement element = findIdentifier(enclosingExecutable.getLabels(), labelName);
      processElement(element);
    }
    return super.visitLabeledStatement(node);
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement outerExecutable = enclosingExecutable;
    try {
      Token property = node.getPropertyKeyword();
      SimpleIdentifier methodName = node.getName();
      String nameOfMethod = methodName.getName();
      if (nameOfMethod.equals(TokenType.MINUS.getLexeme())
          && node.getParameters().getParameters().size() == 0) {
        nameOfMethod = "unary-";
      }
      if (property == null) {
        enclosingExecutable = findWithNameAndOffset(
            enclosingClass.getMethods(),
            nameOfMethod,
            methodName.getOffset());
        methodName.setStaticElement(enclosingExecutable);
      } else {
        PropertyAccessorElement accessor = findIdentifier(enclosingClass.getAccessors(), methodName);
        if (((KeywordToken) property).getKeyword() == Keyword.SET) {
          accessor = accessor.getVariable().getSetter();
          methodName.setStaticElement(accessor);
        }
        enclosingExecutable = accessor;
      }
      processElement(enclosingExecutable);
      return super.visitMethodDeclaration(node);
    } finally {
      enclosingExecutable = outerExecutable;
    }
  }

  @Override
  public Void visitPartDirective(PartDirective node) {
    String uri = getStringValue(node.getUri());
    if (uri != null) {
      Source partSource = enclosingUnit.getContext().getSourceFactory().resolveUri(
          enclosingUnit.getSource(),
          uri);
      CompilationUnitElement element = findPart(enclosingUnit.getLibrary().getParts(), partSource);
      processElement(element);
    }
    return super.visitPartDirective(node);
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElement element = getElementForParameter(node, parameterName);
      ParameterElement outerParameter = enclosingParameter;
      try {
        enclosingParameter = element;
        processElement(enclosingParameter);
        return super.visitSimpleFormalParameter(node);
      } finally {
        enclosingParameter = outerParameter;
      }
    } else {
    }
    return super.visitSimpleFormalParameter(node);
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElement element = findIdentifier(enclosingExecutable.getLabels(), labelName);
      processElement(element);
    }
    return super.visitSwitchCase(node);
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      LabelElement element = findIdentifier(enclosingExecutable.getLabels(), labelName);
      processElement(element);
    }
    return super.visitSwitchDefault(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    SimpleIdentifier parameterName = node.getName();
    TypeParameterElement element = null;
    if (enclosingClass != null) {
      element = findIdentifier(enclosingClass.getTypeParameters(), parameterName);
    } else if (enclosingAlias != null) {
      element = findIdentifier(enclosingAlias.getTypeParameters(), parameterName);
    }
    processElement(element);
    return super.visitTypeParameter(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    VariableElement element = null;
    SimpleIdentifier variableName = node.getName();
    if (enclosingExecutable != null) {
      element = findIdentifier(enclosingExecutable.getLocalVariables(), variableName);
    }
    if (element == null && enclosingClass != null) {
      element = findIdentifier(enclosingClass.getFields(), variableName);
    }
    if (element == null && enclosingUnit != null) {
      element = findIdentifier(enclosingUnit.getTopLevelVariables(), variableName);
    }
    Expression initializer = node.getInitializer();
    if (initializer != null) {
      ExecutableElement outerExecutable = enclosingExecutable;
      try {
        if (element == null) {
          // TODO(brianwilkerson) Report this internal error.
        } else {
          enclosingExecutable = element.getInitializer();
        }
        processElement(element);
        processElement(enclosingExecutable);
        return super.visitVariableDeclaration(node);
      } finally {
        enclosingExecutable = outerExecutable;
      }
    }
    return super.visitVariableDeclaration(node);
  }

  protected void processElement(Element element) {
    if (element == null) {
      throw new DeclarationMismatchException();
    }
    if (!allElements.contains(element)) {
      throw new DeclarationMismatchException();
    }
    unmatchedElements.remove(element);
  }

  /**
   * Given that the comparison is to begin with the given element, capture the enclosing elements
   * that might be used while performing the comparison.
   * 
   * @param element the element corresponding to the AST structure to be compared
   */
  private void captureEnclosingElements(Element element) {
    Element parent = element instanceof CompilationUnitElement ? element
        : element.getEnclosingElement();
    while (parent != null) {
      if (parent instanceof CompilationUnitElement) {
        enclosingUnit = (CompilationUnitElement) parent;
      } else if (parent instanceof ClassElement) {
        if (enclosingClass == null) {
          enclosingClass = (ClassElement) parent;
        }
      } else if (parent instanceof FunctionTypeAliasElement) {
        if (enclosingAlias == null) {
          enclosingAlias = (FunctionTypeAliasElement) parent;
        }
      } else if (parent instanceof ExecutableElement) {
        if (enclosingExecutable == null) {
          enclosingExecutable = (ExecutableElement) parent;
        }
      } else if (parent instanceof ParameterElement) {
        if (enclosingParameter == null) {
          enclosingParameter = (ParameterElement) parent;
        }
      }
      parent = parent.getEnclosingElement();
    }
  }

  /**
   * Return the element in the given array of elements that was created for the declaration at the
   * given offset. This method should only be used when there is no name
   * 
   * @param elements the elements of the appropriate kind that exist in the current context
   * @param offset the offset of the name of the element to be returned
   * @return the element at the given offset
   */
  private <E extends Element> E findAtOffset(E[] elements, int offset) {
    return findWithNameAndOffset(elements, "", offset);
  }

  /**
   * Return the export element from the given array whose library has the given source, or
   * {@code null} if there is no such export.
   * 
   * @param exports the export elements being searched
   * @param source the source of the library associated with the export element to being searched
   *          for
   * @return the export element whose library has the given source
   */
  private ExportElement findExport(ExportElement[] exports, Source source) {
    for (ExportElement export : exports) {
      if (export.getExportedLibrary().getSource().equals(source)) {
        return export;
      }
    }
    return null;
  }

  /**
   * Return the element in the given array of elements that was created for the declaration with the
   * given name.
   * 
   * @param elements the elements of the appropriate kind that exist in the current context
   * @param identifier the name node in the declaration of the element to be returned
   * @return the element created for the declaration with the given name
   */
  private <E extends Element> E findIdentifier(E[] elements, SimpleIdentifier identifier) {
    return findWithNameAndOffset(elements, identifier.getName(), identifier.getOffset());
  }

  /**
   * Return the import element from the given array whose library has the given source and that has
   * the given prefix, or {@code null} if there is no such import.
   * 
   * @param imports the import elements being searched
   * @param source the source of the library associated with the import element to being searched
   *          for
   * @param prefix the prefix with which the library was imported
   * @return the import element whose library has the given source and prefix
   */
  private ImportElement findImport(ImportElement[] imports, Source source, SimpleIdentifier prefix) {
    for (ImportElement element : imports) {
      if (element.getImportedLibrary().getSource().equals(source)) {
        PrefixElement prefixElement = element.getPrefix();
        if (prefix == null) {
          if (prefixElement == null) {
            return element;
          }
        } else {
          if (prefixElement != null && prefix.getName().equals(prefixElement.getDisplayName())) {
            return element;
          }
        }
      }
    }
    return null;
  }

  /**
   * Return the element for the part with the given source, or {@code null} if there is no element
   * for the given source.
   * 
   * @param parts the elements for the parts
   * @param partSource the source for the part whose element is to be returned
   * @return the element for the part with the given source
   */
  private CompilationUnitElement findPart(CompilationUnitElement[] parts, Source partSource) {
    for (CompilationUnitElement part : parts) {
      if (part.getSource().equals(partSource)) {
        return part;
      }
    }
    return null;
  }

  /**
   * Return the element in the given array of elements that was created for the declaration with the
   * given name at the given offset.
   * 
   * @param elements the elements of the appropriate kind that exist in the current context
   * @param name the name of the element to be returned
   * @param offset the offset of the name of the element to be returned
   * @return the element with the given name and offset
   */
  private <E extends Element> E findWithNameAndOffset(E[] elements, String name, int offset) {
    for (E element : elements) {
      if (element.getDisplayName().equals(name) && element.getNameOffset() == offset) {
        return element;
      }
    }
    return null;
  }

  private void gatherElements(Element element) {
    element.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(Element element) {
        allElements.add(element);
        unmatchedElements.add(element);
        return super.visitElement(element);
      }
    });
  }

  /**
   * Search the most closely enclosing list of parameters for a parameter with the given name.
   * 
   * @param node the node defining the parameter with the given name
   * @param parameterName the name of the parameter being searched for
   * @return the element representing the parameter with that name
   */
  private ParameterElement getElementForParameter(FormalParameter node,
      SimpleIdentifier parameterName) {
    ParameterElement[] parameters = null;
    if (enclosingParameter != null) {
      parameters = enclosingParameter.getParameters();
    }
    if (parameters == null && enclosingExecutable != null) {
      parameters = enclosingExecutable.getParameters();
    }
    if (parameters == null && enclosingAlias != null) {
      parameters = enclosingAlias.getParameters();
    }
    return parameters == null ? null : findIdentifier(parameters, parameterName);
  }

  /**
   * Return the value of the given string literal, or {@code null} if the string is not a constant
   * string without any string interpolation.
   * 
   * @param literal the string literal whose value is to be returned
   * @return the value of the given string literal
   */
  private String getStringValue(StringLiteral literal) {
    if (literal instanceof StringInterpolation) {
      return null;
    }
    return literal.getStringValue();
  }

  /**
   * Return {@code true} if the given class defines at least one constructor.
   * 
   * @param node the class being tested
   * @return {@code true} if the class defines at least one constructor
   */
  private boolean hasConstructor(ClassDeclaration node) {
    for (ClassMember member : node.getMembers()) {
      if (member instanceof ConstructorDeclaration) {
        return true;
      }
    }
    return false;
  }
}
