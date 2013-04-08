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

import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DefaultFormalParameter;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.LocalVariableElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code DeclarationResolver} are used to resolve declarations in an AST
 * structure to already built elements.
 */
public class DeclarationResolver extends RecursiveASTVisitor<Void> {
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
   * Initialize a newly created resolver.
   */
  public DeclarationResolver() {
    super();
  }

  /**
   * Resolve the declarations within the given compilation unit to the elements rooted at the given
   * element.
   * 
   * @param unit the compilation unit to be resolved
   * @param element the root of the element model used to resolve the AST nodes
   */
  public void resolve(CompilationUnit unit, CompilationUnitElement element) {
    enclosingUnit = element;
    unit.setElement(element);
    unit.accept(this);
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    SimpleIdentifier exceptionParameter = node.getExceptionParameter();
    if (exceptionParameter != null) {
      LocalVariableElement[] localVariables = enclosingExecutable.getLocalVariables();
      find(localVariables, exceptionParameter);

      SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
      if (stackTraceParameter != null) {
        find(localVariables, stackTraceParameter);
      }
    }
    return super.visitCatchClause(node);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement outerClass = enclosingClass;
    try {
      SimpleIdentifier className = node.getName();
      enclosingClass = find(enclosingUnit.getTypes(), className);
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
      enclosingClass = find(enclosingUnit.getTypes(), className);
      return super.visitClassTypeAlias(node);
    } finally {
      enclosingClass = outerClass;
    }
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
        constructorName.setElement(enclosingExecutable);
      }
      node.setElement((ConstructorElement) enclosingExecutable);
      return super.visitConstructorDeclaration(node);
    } finally {
      enclosingExecutable = outerExecutable;
    }
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    SimpleIdentifier variableName = node.getIdentifier();
    find(enclosingExecutable.getLocalVariables(), variableName);
    return super.visitDeclaredIdentifier(node);
  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    SimpleIdentifier parameterName = node.getParameter().getIdentifier();
    ParameterElement element = find(enclosingExecutable.getParameters(), parameterName);
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
    }
    ParameterElement outerParameter = enclosingParameter;
    try {
      enclosingParameter = element;
      return super.visitDefaultFormalParameter(node);
    } finally {
      enclosingParameter = outerParameter;
    }
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    String uri = getStringValue(node.getUri());
    if (uri != null) {
      LibraryElement library = enclosingUnit.getLibrary();
      ExportElement exportElement = find(
          library.getExports(),
          enclosingUnit.getContext().getSourceFactory().resolveUri(enclosingUnit.getSource(), uri));
      node.setElement(exportElement);
    }
    return super.visitExportDirective(node);
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElement element = find(enclosingExecutable.getParameters(), parameterName);
      ParameterElement outerParameter = enclosingParameter;
      try {
        enclosingParameter = element;
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
          enclosingExecutable = find(enclosingExecutable.getFunctions(), functionName);
        } else {
          enclosingExecutable = find(enclosingUnit.getFunctions(), functionName);
        }
      } else {
        PropertyAccessorElement accessor = find(enclosingUnit.getAccessors(), functionName);
        if (((KeywordToken) property).getKeyword() == Keyword.SET) {
          accessor = accessor.getVariable().getSetter();
          functionName.setElement(accessor);
        }
        enclosingExecutable = accessor;
      }
      node.getFunctionExpression().setElement(enclosingExecutable);
      return super.visitFunctionDeclaration(node);
    } finally {
      enclosingExecutable = outerExecutable;
    }
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    if (!(node.getParent() instanceof FunctionDeclaration)) {
      FunctionElement element = find(
          enclosingExecutable.getFunctions(),
          node.getBeginToken().getOffset());
      node.setElement(element);
    }
    ExecutableElement outerExecutable = enclosingExecutable;
    try {
      enclosingExecutable = node.getElement();
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
      enclosingAlias = find(enclosingUnit.getFunctionTypeAliases(), aliasName);
      return super.visitFunctionTypeAlias(node);
    } finally {
      enclosingAlias = outerAlias;
    }
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElement element = find(enclosingExecutable.getParameters(), parameterName);
      ParameterElement outerParameter = enclosingParameter;
      try {
        enclosingParameter = element;
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
      ImportElement importElement = find(
          library.getImports(),
          enclosingUnit.getContext().getSourceFactory().resolveUri(enclosingUnit.getSource(), uri),
          node.getPrefix());
      node.setElement(importElement);
    }
    return super.visitImportDirective(node);
  }

  @Override
  public Void visitLabeledStatement(LabeledStatement node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      find(enclosingExecutable.getLabels(), labelName);
    }
    return super.visitLabeledStatement(node);
  }

  @Override
  public Void visitLibraryDirective(LibraryDirective node) {
    node.setElement(enclosingUnit.getLibrary());
    return super.visitLibraryDirective(node);
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
        enclosingExecutable = find(
            enclosingClass.getMethods(),
            nameOfMethod,
            methodName.getOffset());
        methodName.setElement(enclosingExecutable);
      } else {
        PropertyAccessorElement accessor = find(enclosingClass.getAccessors(), methodName);
        if (((KeywordToken) property).getKeyword() == Keyword.SET) {
          accessor = accessor.getVariable().getSetter();
          methodName.setElement(accessor);
        }
        enclosingExecutable = accessor;
      }
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
      node.setElement(find(enclosingUnit.getLibrary().getParts(), partSource));
    }
    return super.visitPartDirective(node);
  }

  @Override
  public Void visitPartOfDirective(PartOfDirective node) {
    node.setElement(enclosingUnit.getLibrary());
    return super.visitPartOfDirective(node);
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    if (!(node.getParent() instanceof DefaultFormalParameter)) {
      SimpleIdentifier parameterName = node.getIdentifier();
      ParameterElement element = null;
      if (enclosingParameter != null) {
        element = find(enclosingParameter.getParameters(), parameterName);
      } else if (enclosingExecutable != null) {
        element = find(enclosingExecutable.getParameters(), parameterName);
      } else if (enclosingAlias != null) {
        element = find(enclosingAlias.getParameters(), parameterName);
      } else {
        // Report this internal error.
      }
      ParameterElement outerParameter = enclosingParameter;
      try {
        enclosingParameter = element;
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
      find(enclosingExecutable.getLabels(), labelName);
    }
    return super.visitSwitchCase(node);
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    for (Label label : node.getLabels()) {
      SimpleIdentifier labelName = label.getLabel();
      find(enclosingExecutable.getLabels(), labelName);
    }
    return super.visitSwitchDefault(node);
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    SimpleIdentifier parameterName = node.getName();
    if (enclosingClass != null) {
      find(enclosingClass.getTypeVariables(), parameterName);
    } else if (enclosingAlias != null) {
      find(enclosingAlias.getTypeVariables(), parameterName);
    }
    return super.visitTypeParameter(node);
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    VariableElement element = null;
    SimpleIdentifier variableName = node.getName();
    if (enclosingExecutable != null) {
      element = find(enclosingExecutable.getLocalVariables(), variableName);
    }
    if (element == null && enclosingClass != null) {
      element = find(enclosingClass.getFields(), variableName);
    }
    if (element == null && enclosingUnit != null) {
      element = find(enclosingUnit.getTopLevelVariables(), variableName);
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
        return super.visitVariableDeclaration(node);
      } finally {
        enclosingExecutable = outerExecutable;
      }
    }
    return super.visitVariableDeclaration(node);
  }

  /**
   * Append the value of the given string literal to the given string builder.
   * 
   * @param builder the builder to which the string's value is to be appended
   * @param literal the string literal whose value is to be appended to the builder
   * @throws IllegalArgumentException if the string is not a constant string without any string
   *           interpolation
   */
  private void appendStringValue(StringBuilder builder, StringLiteral literal)
      throws IllegalArgumentException {
    if (literal instanceof SimpleStringLiteral) {
      builder.append(((SimpleStringLiteral) literal).getValue());
    } else if (literal instanceof AdjacentStrings) {
      for (StringLiteral stringLiteral : ((AdjacentStrings) literal).getStrings()) {
        appendStringValue(builder, stringLiteral);
      }
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Return the element for the part with the given source, or {@code null} if there is no element
   * for the given source.
   * 
   * @param parts the elements for the parts
   * @param partSource the source for the part whose element is to be returned
   * @return the element for the part with the given source
   */
  private CompilationUnitElement find(CompilationUnitElement[] parts, Source partSource) {
    for (CompilationUnitElement part : parts) {
      if (part.getSource().equals(partSource)) {
        return part;
      }
    }
    return null;
  }

  /**
   * Return the element in the given array of elements that was created for the declaration at the
   * given offset. This method should only be used when there is no name
   * 
   * @param elements the elements of the appropriate kind that exist in the current context
   * @param offset the offset of the name of the element to be returned
   * @return the element at the given offset
   */
  private <E extends Element> E find(E[] elements, int offset) {
    return find(elements, "", offset);
  }

  /**
   * Return the element in the given array of elements that was created for the declaration with the
   * given name.
   * 
   * @param elements the elements of the appropriate kind that exist in the current context
   * @param identifier the name node in the declaration of the element to be returned
   * @return the element created for the declaration with the given name
   */
  private <E extends Element> E find(E[] elements, SimpleIdentifier identifier) {
    E element = find(elements, identifier.getName(), identifier.getOffset());
    identifier.setElement(element);
    return element;
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
  private <E extends Element> E find(E[] elements, String name, int offset) {
    for (E element : elements) {
      if (element.getName().equals(name) && element.getNameOffset() == offset) {
        return element;
      }
    }
    return null;
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
  private ExportElement find(ExportElement[] exports, Source source) {
    for (ExportElement export : exports) {
      if (export.getExportedLibrary().getSource().equals(source)) {
        return export;
      }
    }
    return null;
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
  private ImportElement find(ImportElement[] imports, Source source, SimpleIdentifier prefix) {
    for (ImportElement element : imports) {
      if (element.getImportedLibrary().getSource().equals(source)) {
        PrefixElement prefixElement = element.getPrefix();
        if (prefix == null) {
          if (prefixElement == null) {
            return element;
          }
        } else {
          if (prefixElement != null && prefix.getName().equals(prefixElement.getName())) {
            return element;
          }
        }
      }
    }
    return null;
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
    StringBuilder builder = new StringBuilder();
    try {
      appendStringValue(builder, literal);
    } catch (IllegalArgumentException exception) {
      return null;
    }
    return builder.toString().trim();
  }
}
