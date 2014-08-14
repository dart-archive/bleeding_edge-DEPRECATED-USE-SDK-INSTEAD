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

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldFormalParameter;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.FunctionTypedFormalParameter;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.UnifyingAstVisitor;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.FieldFormalParameterElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TypeParameterElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.ElementAnnotationImpl;
import com.google.dart.engine.internal.element.ExecutableElementImpl;
import com.google.dart.engine.internal.element.FunctionTypeAliasElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.PropertyInducingElementImpl;
import com.google.dart.engine.internal.element.TypeParameterElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeImpl;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;

/**
 * Instances of the class {@code TypeResolverVisitor} are used to resolve the types associated with
 * the elements in the element model. This includes the types of superclasses, mixins, interfaces,
 * fields, methods, parameters, and local variables. As a side-effect, this also finishes building
 * the type hierarchy.
 * 
 * @coverage dart.engine.resolver
 */
public class TypeResolverVisitor extends ScopedVisitor {
  /**
   * Kind of the redirecting constructor.
   */
  private static enum RedirectingConstructorKind {
    CONST,
    NORMAL
  }

  /**
   * @return {@code true} if the name of the given {@link TypeName} is an built-in identifier.
   */
  private static boolean isBuiltInIdentifier(TypeName node) {
    Token token = node.getName().getBeginToken();
    return token.getType() == TokenType.KEYWORD;
  }

  /**
   * @return {@code true} if given {@link TypeName} is used as a type annotation.
   */
  private static boolean isTypeAnnotation(TypeName node) {
    AstNode parent = node.getParent();
    if (parent instanceof VariableDeclarationList) {
      return ((VariableDeclarationList) parent).getType() == node;
    }
    if (parent instanceof FieldFormalParameter) {
      return ((FieldFormalParameter) parent).getType() == node;
    }
    if (parent instanceof SimpleFormalParameter) {
      return ((SimpleFormalParameter) parent).getType() == node;
    }
    return false;
  }

  /**
   * The type representing the type 'dynamic'.
   */
  private Type dynamicType;

  /**
   * The flag specifying if currently visited class references 'super' expression.
   */
  private boolean hasReferenceToSuper;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public TypeResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
    dynamicType = typeProvider.getDynamicType();
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
  public TypeResolverVisitor(LibraryElement definingLibrary, Source source,
      TypeProvider typeProvider, AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, errorListener);
    dynamicType = typeProvider.getDynamicType();
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in an AST node.
   * 
   * @param definingLibrary the element for the library containing the node being visited
   * @param source the source representing the compilation unit containing the node being visited
   * @param typeProvider the object used to access the types from the core library
   * @param nameScope the scope used to resolve identifiers in the node that will first be visited
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   */
  public TypeResolverVisitor(LibraryElement definingLibrary, Source source,
      TypeProvider typeProvider, Scope nameScope, AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, nameScope, errorListener);
    dynamicType = typeProvider.getDynamicType();
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public TypeResolverVisitor(ResolvableLibrary library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
    dynamicType = typeProvider.getDynamicType();
  }

  @Override
  public Void visitAnnotation(Annotation node) {
    //
    // Visit annotations, if the annotation is @proxy, on a class, and "proxy" resolves to the proxy
    // annotation in dart.core, then create create the ElementAnnotationImpl and set it as the
    // metadata on the enclosing class.
    //
    // Element resolution is done in the ElementResolver, and this work will be done in the general
    // case for all annotations in the ElementResolver. The reason we resolve this particular
    // element early is so that ClassElement.isProxy() returns the correct information during all
    // phases of the ElementResolver.
    //
    super.visitAnnotation(node);
    Identifier identifier = node.getName();
    if (identifier.getName().endsWith(ElementAnnotationImpl.PROXY_VARIABLE_NAME)
        && node.getParent() instanceof ClassDeclaration) {
      Element element = getNameScope().lookup(identifier, getDefiningLibrary());
      if (element != null && element.getLibrary().isDartCore()
          && element instanceof PropertyAccessorElement) {
        // This is the @proxy from dart.core
        ClassDeclaration classDeclaration = (ClassDeclaration) node.getParent();
        ElementAnnotationImpl elementAnnotation = new ElementAnnotationImpl(element);
        node.setElementAnnotation(elementAnnotation);
        ((ClassElementImpl) classDeclaration.getElement()).setMetadata(new ElementAnnotationImpl[] {elementAnnotation});
      }
    }
    return null;
  }

  @Override
  public Void visitCatchClause(CatchClause node) {
    super.visitCatchClause(node);
    SimpleIdentifier exception = node.getExceptionParameter();
    if (exception != null) {
      // If an 'on' clause is provided the type of the exception parameter is the type in the 'on'
      // clause. Otherwise, the type of the exception parameter is 'Object'.
      TypeName exceptionTypeName = node.getExceptionType();
      Type exceptionType;
      if (exceptionTypeName == null) {
        exceptionType = getTypeProvider().getDynamicType();
      } else {
        exceptionType = getType(exceptionTypeName);
      }
      recordType(exception, exceptionType);
      Element element = exception.getStaticElement();
      if (element instanceof VariableElementImpl) {
        ((VariableElementImpl) element).setType(exceptionType);
      } else {
        // TODO(brianwilkerson) Report the internal error
      }
    }
    SimpleIdentifier stackTrace = node.getStackTraceParameter();
    if (stackTrace != null) {
      recordType(stackTrace, getTypeProvider().getStackTraceType());
    }
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ExtendsClause extendsClause = node.getExtendsClause();
    WithClause withClause = node.getWithClause();
    ImplementsClause implementsClause = node.getImplementsClause();

    hasReferenceToSuper = false;
    super.visitClassDeclaration(node);

    ClassElementImpl classElement = getClassElement(node.getName());
    InterfaceType superclassType = null;
    if (extendsClause != null) {
      ErrorCode errorCode = withClause == null ? CompileTimeErrorCode.EXTENDS_NON_CLASS
          : CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS;
      superclassType = resolveType(
          extendsClause.getSuperclass(),
          errorCode,
          CompileTimeErrorCode.EXTENDS_ENUM,
          errorCode);
      if (superclassType != getTypeProvider().getObjectType()) {
        classElement.setValidMixin(false);
      }
    }
    if (classElement != null) {
      if (superclassType == null) {
        InterfaceType objectType = getTypeProvider().getObjectType();
        if (classElement.getType() != objectType) {
          superclassType = objectType;
        }
      }
      classElement.setSupertype(superclassType);
      classElement.setHasReferenceToSuper(hasReferenceToSuper);
    }
    resolve(classElement, withClause, implementsClause);
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    super.visitClassTypeAlias(node);
    ClassElementImpl classElement = getClassElement(node.getName());
    ErrorCode errorCode = CompileTimeErrorCode.MIXIN_WITH_NON_CLASS_SUPERCLASS;
    InterfaceType superclassType = resolveType(
        node.getSuperclass(),
        errorCode,
        CompileTimeErrorCode.EXTENDS_ENUM,
        errorCode);
    if (superclassType == null) {
      superclassType = getTypeProvider().getObjectType();
    }
    if (classElement != null && superclassType != null) {
      classElement.setSupertype(superclassType);
      ClassElement superclassElement = superclassType.getElement();
      if (superclassElement != null) {
        ConstructorElement[] constructors = superclassElement.getConstructors();
        int count = constructors.length;
        if (count > 0) {
          Type[] parameterTypes = TypeParameterTypeImpl.getTypes(superclassType.getTypeParameters());
          Type[] argumentTypes = getArgumentTypes(
              node.getSuperclass().getTypeArguments(),
              parameterTypes);
          InterfaceType classType = classElement.getType();
          ArrayList<ConstructorElement> implicitConstructors = new ArrayList<ConstructorElement>(
              count);
          for (int i = 0; i < count; i++) {
            ConstructorElement explicitConstructor = constructors[i];
            if (!explicitConstructor.isFactory()) {
              implicitConstructors.add(createImplicitContructor(
                  classType,
                  explicitConstructor,
                  parameterTypes,
                  argumentTypes));
            }
          }
          classElement.setConstructors(implicitConstructors.toArray(new ConstructorElement[implicitConstructors.size()]));
        }
      }
    }
    resolve(classElement, node.getWithClause(), node.getImplementsClause());
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    super.visitConstructorDeclaration(node);
    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
    if (element == null) {
      ClassDeclaration classNode = node.getAncestor(ClassDeclaration.class);
      StringBuilder builder = new StringBuilder();
      builder.append("The element for the constructor ");
      builder.append(node.getName() == null ? "<unnamed>" : node.getName().getName());
      builder.append(" in ");
      if (classNode == null) {
        builder.append("<unknown class>");
      } else {
        builder.append(classNode.getName().getName());
      }
      builder.append(" in ");
      builder.append(getSource().getFullName());
      builder.append(" was not set while trying to resolve types.");
      AnalysisEngine.getInstance().getLogger().logError(builder.toString(), new AnalysisException());
    } else {
      ClassElement definingClass = (ClassElement) element.getEnclosingElement();
      element.setReturnType(definingClass.getType());
      FunctionTypeImpl type = new FunctionTypeImpl(element);
      type.setTypeArguments(definingClass.getType().getTypeArguments());
      element.setType(type);
    }
    return null;
  }

  @Override
  public Void visitDeclaredIdentifier(DeclaredIdentifier node) {
    super.visitDeclaredIdentifier(node);
    Type declaredType;
    TypeName typeName = node.getType();
    if (typeName == null) {
      declaredType = dynamicType;
    } else {
      declaredType = getType(typeName);
    }
    LocalVariableElementImpl element = (LocalVariableElementImpl) node.getElement();
    element.setType(declaredType);
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    super.visitFieldFormalParameter(node);
    Element element = node.getIdentifier().getStaticElement();
    if (element instanceof ParameterElementImpl) {
      ParameterElementImpl parameter = (ParameterElementImpl) element;
      FormalParameterList parameterList = node.getParameters();
      if (parameterList == null) {
        Type type;
        TypeName typeName = node.getType();
        if (typeName == null) {
          type = dynamicType;
          if (parameter instanceof FieldFormalParameterElement) {
            FieldElement fieldElement = ((FieldFormalParameterElement) parameter).getField();
            if (fieldElement != null) {
              type = fieldElement.getType();
            }
          }
        } else {
          type = getType(typeName);
        }
        parameter.setType(type);
      } else {
        setFunctionTypedParameterType(parameter, node.getType(), node.getParameters());
      }
    } else {
      // TODO(brianwilkerson) Report this internal error
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    super.visitFunctionDeclaration(node);
    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
    if (element == null) {
      StringBuilder builder = new StringBuilder();
      builder.append("The element for the top-level function ");
      builder.append(node.getName());
      builder.append(" in ");
      builder.append(getSource().getFullName());
      builder.append(" was not set while trying to resolve types.");
      AnalysisEngine.getInstance().getLogger().logError(builder.toString(), new AnalysisException());
    }
    element.setReturnType(computeReturnType(node.getReturnType()));
    FunctionTypeImpl type = new FunctionTypeImpl(element);
    ClassElement definingClass = element.getAncestor(ClassElement.class);
    if (definingClass != null) {
      type.setTypeArguments(definingClass.getType().getTypeArguments());
    }
    element.setType(type);
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    super.visitFunctionTypeAlias(node);
    FunctionTypeAliasElementImpl element = (FunctionTypeAliasElementImpl) node.getElement();
    element.setReturnType(computeReturnType(node.getReturnType()));
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    super.visitFunctionTypedFormalParameter(node);
    Element element = node.getIdentifier().getStaticElement();
    if (element instanceof ParameterElementImpl) {
      setFunctionTypedParameterType(
          (ParameterElementImpl) element,
          node.getReturnType(),
          node.getParameters());
    } else {
      // TODO(brianwilkerson) Report this internal error
    }
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    super.visitMethodDeclaration(node);
    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
    if (element == null) {
      ClassDeclaration classNode = node.getAncestor(ClassDeclaration.class);
      StringBuilder builder = new StringBuilder();
      builder.append("The element for the method ");
      builder.append(node.getName().getName());
      builder.append(" in ");
      if (classNode == null) {
        builder.append("<unknown class>");
      } else {
        builder.append(classNode.getName().getName());
      }
      builder.append(" in ");
      builder.append(getSource().getFullName());
      builder.append(" was not set while trying to resolve types.");
      AnalysisEngine.getInstance().getLogger().logError(builder.toString(), new AnalysisException());
    }
    element.setReturnType(computeReturnType(node.getReturnType()));
    FunctionTypeImpl type = new FunctionTypeImpl(element);
    ClassElement definingClass = element.getAncestor(ClassElement.class);
    if (definingClass != null) {
      type.setTypeArguments(definingClass.getType().getTypeArguments());
    }
    element.setType(type);
    if (element instanceof PropertyAccessorElement) {
      PropertyAccessorElement accessor = (PropertyAccessorElement) element;
      PropertyInducingElementImpl variable = (PropertyInducingElementImpl) accessor.getVariable();
      if (accessor.isGetter()) {
        variable.setType(type.getReturnType());
      } else if (variable.getType() == null) {
        Type[] parameterTypes = type.getNormalParameterTypes();
        if (parameterTypes != null && parameterTypes.length > 0) {
          variable.setType(parameterTypes[0]);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    super.visitSimpleFormalParameter(node);
    Type declaredType;
    TypeName typeName = node.getType();
    if (typeName == null) {
      declaredType = dynamicType;
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getIdentifier().getStaticElement();
    if (element instanceof ParameterElement) {
      ((ParameterElementImpl) element).setType(declaredType);
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
  }

  @Override
  public Void visitSuperExpression(SuperExpression node) {
    hasReferenceToSuper = true;
    return super.visitSuperExpression(node);
  }

  @Override
  public Void visitTypeName(TypeName node) {
    super.visitTypeName(node);
    Identifier typeName = node.getName();
    TypeArgumentList argumentList = node.getTypeArguments();

    Element element = getNameScope().lookup(typeName, getDefiningLibrary());
    if (element == null) {
      //
      // Check to see whether the type name is either 'dynamic' or 'void', neither of which are in
      // the name scope and hence will not be found by normal means.
      //
      if (typeName.getName().equals(dynamicType.getName())) {
        setElement(typeName, dynamicType.getElement());
        if (argumentList != null) {
          // TODO(brianwilkerson) Report this error
          // reporter.reportError(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS, node, dynamicType.getName(), 0, argumentList.getArguments().size());
        }
        typeName.setStaticType(dynamicType);
        node.setType(dynamicType);
        return null;
      }
      VoidTypeImpl voidType = VoidTypeImpl.getInstance();
      if (typeName.getName().equals(voidType.getName())) {
        // There is no element for 'void'.
        if (argumentList != null) {
          // TODO(brianwilkerson) Report this error
          // reporter.reportError(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS, node, voidType.getName(), 0, argumentList.getArguments().size());
        }
        typeName.setStaticType(voidType);
        node.setType(voidType);
        return null;
      }
      //
      // If not, the look to see whether we might have created the wrong AST structure for a
      // constructor name. If so, fix the AST structure and then proceed.
      //
      AstNode parent = node.getParent();
      if (typeName instanceof PrefixedIdentifier && parent instanceof ConstructorName
          && argumentList == null) {
        ConstructorName name = (ConstructorName) parent;
        if (name.getName() == null) {
          PrefixedIdentifier prefixedIdentifier = (PrefixedIdentifier) typeName;
          SimpleIdentifier prefix = prefixedIdentifier.getPrefix();
          element = getNameScope().lookup(prefix, getDefiningLibrary());
          if (element instanceof PrefixElement) {
            if (parent.getParent() instanceof InstanceCreationExpression
                && ((InstanceCreationExpression) parent.getParent()).isConst()) {
              // If, if this is a const expression, then generate a
              // CompileTimeErrorCode.CONST_WITH_NON_TYPE error.
              reportErrorForNode(
                  CompileTimeErrorCode.CONST_WITH_NON_TYPE,
                  prefixedIdentifier.getIdentifier(),
                  prefixedIdentifier.getIdentifier().getName());
            } else {
              // Else, if this expression is a new expression, report a NEW_WITH_NON_TYPE warning.
              reportErrorForNode(
                  StaticWarningCode.NEW_WITH_NON_TYPE,
                  prefixedIdentifier.getIdentifier(),
                  prefixedIdentifier.getIdentifier().getName());
            }
            setElement(prefix, element);
            return null;
          } else if (element != null) {
            //
            // Rewrite the constructor name. The parser, when it sees a constructor named "a.b",
            // cannot tell whether "a" is a prefix and "b" is a class name, or whether "a" is a
            // class name and "b" is a constructor name. It arbitrarily chooses the former, but
            // in this case was wrong.
            //
            name.setName(prefixedIdentifier.getIdentifier());
            name.setPeriod(prefixedIdentifier.getPeriod());
            node.setName(prefix);
            typeName = prefix;
          }
        }
      }
    }
    // check element
    boolean elementValid = !(element instanceof MultiplyDefinedElement);
    if (elementValid && !(element instanceof ClassElement)
        && isTypeNameInInstanceCreationExpression(node)) {
      SimpleIdentifier typeNameSimple = getTypeSimpleIdentifier(typeName);
      InstanceCreationExpression creation = (InstanceCreationExpression) node.getParent().getParent();
      if (creation.isConst()) {
        if (element == null) {
          reportErrorForNode(CompileTimeErrorCode.UNDEFINED_CLASS, typeNameSimple, typeName);
        } else {
          reportErrorForNode(CompileTimeErrorCode.CONST_WITH_NON_TYPE, typeNameSimple, typeName);
        }
        elementValid = false;
      } else {
        if (element != null) {
          reportErrorForNode(StaticWarningCode.NEW_WITH_NON_TYPE, typeNameSimple, typeName);
          elementValid = false;
        }
      }
    }
    if (elementValid && element == null) {
      // We couldn't resolve the type name.
      // TODO(jwren) Consider moving the check for CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE
      // from the ErrorVerifier, so that we don't have two errors on a built in identifier being
      // used as a class name. See CompileTimeErrorCodeTest.test_builtInIdentifierAsType().
      SimpleIdentifier typeNameSimple = getTypeSimpleIdentifier(typeName);
      RedirectingConstructorKind redirectingConstructorKind;
      if (isBuiltInIdentifier(node) && isTypeAnnotation(node)) {
        reportErrorForNode(
            CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE,
            typeName,
            typeName.getName());
      } else if (typeNameSimple.getName().equals("boolean")) {
        reportErrorForNode(StaticWarningCode.UNDEFINED_CLASS_BOOLEAN, typeNameSimple);
      } else if (isTypeNameInCatchClause(node)) {
        reportErrorForNode(StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE, typeName, typeName.getName());
      } else if (isTypeNameInAsExpression(node)) {
        reportErrorForNode(StaticWarningCode.CAST_TO_NON_TYPE, typeName, typeName.getName());
      } else if (isTypeNameInIsExpression(node)) {
        reportErrorForNode(StaticWarningCode.TYPE_TEST_NON_TYPE, typeName, typeName.getName());
      } else if ((redirectingConstructorKind = getRedirectingConstructorKind(node)) != null) {
        ErrorCode errorCode = redirectingConstructorKind == RedirectingConstructorKind.CONST
            ? CompileTimeErrorCode.REDIRECT_TO_NON_CLASS : StaticWarningCode.REDIRECT_TO_NON_CLASS;
        reportErrorForNode(errorCode, typeName, typeName.getName());
      } else if (isTypeNameInTypeArgumentList(node)) {
        reportErrorForNode(
            StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT,
            typeName,
            typeName.getName());
      } else {
        reportErrorForNode(StaticWarningCode.UNDEFINED_CLASS, typeName, typeName.getName());
      }
      elementValid = false;
    }
    if (!elementValid) {
      if (element instanceof MultiplyDefinedElement) {
        setElement(typeName, element);
      } else {
        setElement(typeName, dynamicType.getElement());
      }
      typeName.setStaticType(dynamicType);
      node.setType(dynamicType);
      return null;
    }
    Type type = null;
    if (element instanceof ClassElement) {
      setElement(typeName, element);
      type = ((ClassElement) element).getType();
    } else if (element instanceof FunctionTypeAliasElement) {
      setElement(typeName, element);
      type = ((FunctionTypeAliasElement) element).getType();
    } else if (element instanceof TypeParameterElement) {
      setElement(typeName, element);
      type = ((TypeParameterElement) element).getType();
      if (argumentList != null) {
        // Type parameters cannot have type arguments.
        // TODO(brianwilkerson) Report this error.
//      resolver.reportError(ResolverErrorCode.?, keyType);
      }
    } else if (element instanceof MultiplyDefinedElement) {
      Element[] elements = ((MultiplyDefinedElement) element).getConflictingElements();
      type = getTypeWhenMultiplyDefined(elements);
      if (type != null) {
        node.setType(type);
      }
    } else {
      // The name does not represent a type.
      RedirectingConstructorKind redirectingConstructorKind;
      if (isTypeNameInCatchClause(node)) {
        reportErrorForNode(StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE, typeName, typeName.getName());
      } else if (isTypeNameInAsExpression(node)) {
        reportErrorForNode(StaticWarningCode.CAST_TO_NON_TYPE, typeName, typeName.getName());
      } else if (isTypeNameInIsExpression(node)) {
        reportErrorForNode(StaticWarningCode.TYPE_TEST_NON_TYPE, typeName, typeName.getName());
      } else if ((redirectingConstructorKind = getRedirectingConstructorKind(node)) != null) {
        ErrorCode errorCode = redirectingConstructorKind == RedirectingConstructorKind.CONST
            ? CompileTimeErrorCode.REDIRECT_TO_NON_CLASS : StaticWarningCode.REDIRECT_TO_NON_CLASS;
        reportErrorForNode(errorCode, typeName, typeName.getName());
      } else if (isTypeNameInTypeArgumentList(node)) {
        reportErrorForNode(
            StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT,
            typeName,
            typeName.getName());
      } else {
        AstNode parent = typeName.getParent();
        while (parent instanceof TypeName) {
          parent = parent.getParent();
        }
        if (parent instanceof ExtendsClause || parent instanceof ImplementsClause
            || parent instanceof WithClause || parent instanceof ClassTypeAlias) {
          // Ignored. The error will be reported elsewhere.
        } else {
          reportErrorForNode(StaticWarningCode.NOT_A_TYPE, typeName, typeName.getName());
        }
      }
      setElement(typeName, dynamicType.getElement());
      typeName.setStaticType(dynamicType);
      node.setType(dynamicType);
      return null;
    }
    if (argumentList != null) {
      NodeList<TypeName> arguments = argumentList.getArguments();
      int argumentCount = arguments.size();
      Type[] parameters = getTypeArguments(type);
      int parameterCount = parameters.length;
      Type[] typeArguments = new Type[parameterCount];
      if (argumentCount == parameterCount) {
        for (int i = 0; i < parameterCount; i++) {
          TypeName argumentTypeName = arguments.get(i);
          Type argumentType = getType(argumentTypeName);
          if (argumentType == null) {
            argumentType = dynamicType;
          }
          typeArguments[i] = argumentType;
        }
      } else {
        reportErrorForNode(
            getInvalidTypeParametersErrorCode(node),
            node,
            typeName.getName(),
            parameterCount,
            argumentCount);
        for (int i = 0; i < parameterCount; i++) {
          typeArguments[i] = dynamicType;
        }
      }
      if (type instanceof InterfaceTypeImpl) {
        InterfaceTypeImpl interfaceType = (InterfaceTypeImpl) type;
        type = interfaceType.substitute(typeArguments);
      } else if (type instanceof FunctionTypeImpl) {
        FunctionTypeImpl functionType = (FunctionTypeImpl) type;
        type = functionType.substitute(typeArguments);
      } else {
        // TODO(brianwilkerson) Report this internal error.
      }
    } else {
      //
      // Check for the case where there are no type arguments given for a parameterized type.
      //
      Type[] parameters = getTypeArguments(type);
      int parameterCount = parameters.length;
      if (parameterCount > 0) {
        DynamicTypeImpl dynamicType = DynamicTypeImpl.getInstance();
        Type[] arguments = new Type[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
          arguments[i] = dynamicType;
        }
        type = type.substitute(arguments, parameters);
      }
    }
    typeName.setStaticType(type);
    node.setType(type);
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameter node) {
    super.visitTypeParameter(node);
    TypeName bound = node.getBound();
    if (bound != null) {
      TypeParameterElementImpl typeParameter = (TypeParameterElementImpl) node.getName().getStaticElement();
      if (typeParameter != null) {
        typeParameter.setBound(bound.getType());
      }
    }
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    Type declaredType;
    TypeName typeName = ((VariableDeclarationList) node.getParent()).getType();
    if (typeName == null) {
      declaredType = dynamicType;
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getName().getStaticElement();
    if (element instanceof VariableElement) {
      ((VariableElementImpl) element).setType(declaredType);
      if (element instanceof PropertyInducingElement) {
        PropertyInducingElement variableElement = (PropertyInducingElement) element;
        PropertyAccessorElementImpl getter = (PropertyAccessorElementImpl) variableElement.getGetter();
        getter.setReturnType(declaredType);
        FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
        ClassElement definingClass = element.getAncestor(ClassElement.class);
        if (definingClass != null) {
          getterType.setTypeArguments(definingClass.getType().getTypeArguments());
        }
        getter.setType(getterType);

        PropertyAccessorElementImpl setter = (PropertyAccessorElementImpl) variableElement.getSetter();
        if (setter != null) {
          ParameterElement[] parameters = setter.getParameters();
          if (parameters.length > 0) {
            ((ParameterElementImpl) parameters[0]).setType(declaredType);
          }
          setter.setReturnType(VoidTypeImpl.getInstance());
          FunctionTypeImpl setterType = new FunctionTypeImpl(setter);
          if (definingClass != null) {
            setterType.setTypeArguments(definingClass.getType().getTypeArguments());
          }
          setter.setType(setterType);
        }
      }
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
  }

  @Override
  protected void visitClassMembersInScope(ClassDeclaration node) {
    //
    // Process field declarations before constructors and methods so that the types of field formal
    // parameters can be correctly resolved.
    //
    final ArrayList<ClassMember> nonFields = new ArrayList<ClassMember>();
    node.visitChildren(new UnifyingAstVisitor<Void>() {
      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        nonFields.add(node);
        return null;
      }

      @Override
      public Void visitExtendsClause(ExtendsClause node) {
        // This node was already visited.
        return null;
      }

      @Override
      public Void visitImplementsClause(ImplementsClause node) {
        // This node was already visited.
        return null;
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        nonFields.add(node);
        return null;
      }

      @Override
      public Void visitNode(AstNode node) {
        return node.accept(TypeResolverVisitor.this);
      }

      @Override
      public Void visitWithClause(WithClause node) {
        // This node was already visited.
        return null;
      }
    });
    int count = nonFields.size();
    for (int i = 0; i < count; i++) {
      nonFields.get(i).accept(this);
    }
  }

  /**
   * Given a type name representing the return type of a function, compute the return type of the
   * function.
   * 
   * @param returnType the type name representing the return type of the function
   * @return the return type that was computed
   */
  private Type computeReturnType(TypeName returnType) {
    if (returnType == null) {
      return dynamicType;
    } else {
      return returnType.getType();
    }
  }

  /**
   * Create an implicit constructor that is copied from the given constructor, but that is in the
   * given class.
   * 
   * @param classType the class in which the implicit constructor is defined
   * @param explicitConstructor the constructor on which the implicit constructor is modeled
   * @param parameterTypes the types to be replaced when creating parameters
   * @param argumentTypes the types with which the parameters are to be replaced
   * @return the implicit constructor that was created
   */
  private ConstructorElement createImplicitContructor(InterfaceType classType,
      ConstructorElement explicitConstructor, Type[] parameterTypes, Type[] argumentTypes) {
    ConstructorElementImpl implicitConstructor = new ConstructorElementImpl(
        explicitConstructor.getName(),
        -1);
    implicitConstructor.setSynthetic(true);
    implicitConstructor.setRedirectedConstructor(explicitConstructor);
    implicitConstructor.setConst(explicitConstructor.isConst());
    implicitConstructor.setReturnType(classType);
    ParameterElement[] explicitParameters = explicitConstructor.getParameters();
    int count = explicitParameters.length;
    if (count > 0) {
      ParameterElement[] implicitParameters = new ParameterElement[count];
      for (int i = 0; i < count; i++) {
        ParameterElement explicitParameter = explicitParameters[i];
        ParameterElementImpl implicitParameter = new ParameterElementImpl(
            explicitParameter.getName(),
            -1);
        implicitParameter.setConst(explicitParameter.isConst());
        implicitParameter.setFinal(explicitParameter.isFinal());
        implicitParameter.setParameterKind(explicitParameter.getParameterKind());
        implicitParameter.setSynthetic(true);
        implicitParameter.setType(explicitParameter.getType().substitute(
            argumentTypes,
            parameterTypes));
        implicitParameters[i] = implicitParameter;
      }
      implicitConstructor.setParameters(implicitParameters);
    }
    FunctionTypeImpl type = new FunctionTypeImpl(implicitConstructor);
    type.setTypeArguments(classType.getTypeArguments());
    implicitConstructor.setType(type);
    return implicitConstructor;
  }

  /**
   * Return an array of argument types that corresponds to the array of parameter types and that are
   * derived from the given list of type arguments.
   * 
   * @param typeArguments the type arguments from which the types will be taken
   * @param parameterTypes the parameter types that must be matched by the type arguments
   * @return the argument types that correspond to the parameter types
   */
  private Type[] getArgumentTypes(TypeArgumentList typeArguments, Type[] parameterTypes) {
    DynamicTypeImpl dynamic = DynamicTypeImpl.getInstance();
    int parameterCount = parameterTypes.length;
    Type[] types = new Type[parameterCount];
    if (typeArguments == null) {
      for (int i = 0; i < parameterCount; i++) {
        types[i] = dynamic;
      }
    } else {
      NodeList<TypeName> arguments = typeArguments.getArguments();
      int argumentCount = Math.min(arguments.size(), parameterCount);
      for (int i = 0; i < argumentCount; i++) {
        types[i] = arguments.get(i).getType();
      }
      for (int i = argumentCount; i < parameterCount; i++) {
        types[i] = dynamic;
      }
    }
    return types;
  }

  /**
   * Return the class element that represents the class whose name was provided.
   * 
   * @param identifier the name from the declaration of a class
   * @return the class element that represents the class
   */
  private ClassElementImpl getClassElement(SimpleIdentifier identifier) {
    // TODO(brianwilkerson) Seems like we should be using ClassDeclaration.getElement().
    if (identifier == null) {
      // TODO(brianwilkerson) Report this
      // Internal error: We should never build a class declaration without a name.
      return null;
    }
    Element element = identifier.getStaticElement();
    if (!(element instanceof ClassElementImpl)) {
      // TODO(brianwilkerson) Report this
      // Internal error: Failed to create an element for a class declaration.
      return null;
    }
    return (ClassElementImpl) element;
  }

  /**
   * Return an array containing all of the elements associated with the parameters in the given
   * list.
   * 
   * @param parameterList the list of parameters whose elements are to be returned
   * @return the elements associated with the parameters
   */
  private ParameterElement[] getElements(FormalParameterList parameterList) {
    ArrayList<ParameterElement> elements = new ArrayList<ParameterElement>();
    for (FormalParameter parameter : parameterList.getParameters()) {
      ParameterElement element = (ParameterElement) parameter.getIdentifier().getStaticElement();
      // TODO(brianwilkerson) Understand why the element would be null.
      if (element != null) {
        elements.add(element);
      }
    }
    return elements.toArray(new ParameterElement[elements.size()]);
  }

  /**
   * The number of type arguments in the given type name does not match the number of parameters in
   * the corresponding class element. Return the error code that should be used to report this
   * error.
   * 
   * @param node the type name with the wrong number of type arguments
   * @return the error code that should be used to report that the wrong number of type arguments
   *         were provided
   */
  private ErrorCode getInvalidTypeParametersErrorCode(TypeName node) {
    AstNode parent = node.getParent();
    if (parent instanceof ConstructorName) {
      parent = parent.getParent();
      if (parent instanceof InstanceCreationExpression) {
        if (((InstanceCreationExpression) parent).isConst()) {
          return CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS;
        } else {
          return StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS;
        }
      }
    }
    return StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS;
  }

  /**
   * Checks if the given type name is the target in a redirected constructor.
   * 
   * @param typeName the type name to analyze
   * @return some {@link RedirectingConstructorKind} if the given type name is used as the type in a
   *         redirected constructor, or {@code null} otherwise
   */
  private RedirectingConstructorKind getRedirectingConstructorKind(TypeName typeName) {
    AstNode parent = typeName.getParent();
    if (parent instanceof ConstructorName) {
      ConstructorName constructorName = (ConstructorName) parent;
      parent = constructorName.getParent();
      if (parent instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) parent;
        if (constructorDeclaration.getRedirectedConstructor() == constructorName) {
          if (constructorDeclaration.getConstKeyword() != null) {
            return RedirectingConstructorKind.CONST;
          }
          return RedirectingConstructorKind.NORMAL;
        }
      }
    }
    return null;
  }

  /**
   * Return the type represented by the given type name.
   * 
   * @param typeName the type name representing the type to be returned
   * @return the type represented by the type name
   */
  private Type getType(TypeName typeName) {
    Type type = typeName.getType();
    if (type == null) {
      return dynamicType;
    }
    return type;
  }

  /**
   * Return the type arguments associated with the given type.
   * 
   * @param type the type whole type arguments are to be returned
   * @return the type arguments associated with the given type
   */
  private Type[] getTypeArguments(Type type) {
    if (type instanceof InterfaceType) {
      return ((InterfaceType) type).getTypeArguments();
    } else if (type instanceof FunctionType) {
      return ((FunctionType) type).getTypeArguments();
    }
    return TypeImpl.EMPTY_ARRAY;
  }

  /**
   * Returns the simple identifier of the given (may be qualified) type name.
   * 
   * @param typeName the (may be qualified) qualified type name
   * @return the simple identifier of the given (may be qualified) type name.
   */
  private SimpleIdentifier getTypeSimpleIdentifier(Identifier typeName) {
    if (typeName instanceof SimpleIdentifier) {
      return (SimpleIdentifier) typeName;
    } else {
      return ((PrefixedIdentifier) typeName).getIdentifier();
    }
  }

  /**
   * Given the multiple elements to which a single name could potentially be resolved, return the
   * single interface type that should be used, or {@code null} if there is no clear choice.
   * 
   * @param elements the elements to which a single name could potentially be resolved
   * @return the single interface type that should be used for the type name
   */
  private InterfaceType getTypeWhenMultiplyDefined(Element[] elements) {
    InterfaceType type = null;
    for (Element element : elements) {
      if (element instanceof ClassElement) {
        if (type != null) {
          return null;
        }
        type = ((ClassElement) element).getType();
      }
    }
    return type;
  }

  /**
   * Checks if the given type name is used as the type in an as expression.
   * 
   * @param typeName the type name to analyzer
   * @return {@code true} if the given type name is used as the type in an as expression
   */
  private boolean isTypeNameInAsExpression(TypeName typeName) {
    AstNode parent = typeName.getParent();
    if (parent instanceof AsExpression) {
      AsExpression asExpression = (AsExpression) parent;
      return asExpression.getType() == typeName;
    }
    return false;
  }

  /**
   * Checks if the given type name is used as the exception type in a catch clause.
   * 
   * @param typeName the type name to analyzer
   * @return {@code true} if the given type name is used as the exception type in a catch clause
   */
  private boolean isTypeNameInCatchClause(TypeName typeName) {
    AstNode parent = typeName.getParent();
    if (parent instanceof CatchClause) {
      CatchClause catchClause = (CatchClause) parent;
      return catchClause.getExceptionType() == typeName;
    }
    return false;
  }

  /**
   * Checks if the given type name is used as the type in an instance creation expression.
   * 
   * @param typeName the type name to analyzer
   * @return {@code true} if the given type name is used as the type in an instance creation
   *         expression
   */
  private boolean isTypeNameInInstanceCreationExpression(TypeName typeName) {
    AstNode parent = typeName.getParent();
    if (parent instanceof ConstructorName
        && parent.getParent() instanceof InstanceCreationExpression) {
      ConstructorName constructorName = (ConstructorName) parent;
      return constructorName != null && constructorName.getType() == typeName;
    }
    return false;
  }

  /**
   * Checks if the given type name is used as the type in an is expression.
   * 
   * @param typeName the type name to analyzer
   * @return {@code true} if the given type name is used as the type in an is expression
   */
  private boolean isTypeNameInIsExpression(TypeName typeName) {
    AstNode parent = typeName.getParent();
    if (parent instanceof IsExpression) {
      IsExpression isExpression = (IsExpression) parent;
      return isExpression.getType() == typeName;
    }
    return false;
  }

  /**
   * Checks if the given type name used in a type argument list.
   * 
   * @param typeName the type name to analyzer
   * @return {@code true} if the given type name is in a type argument list
   */
  private boolean isTypeNameInTypeArgumentList(TypeName typeName) {
    return typeName.getParent() instanceof TypeArgumentList;
  }

  /**
   * Record that the static type of the given node is the given type.
   * 
   * @param expression the node whose type is to be recorded
   * @param type the static type of the node
   */
  private Void recordType(Expression expression, Type type) {
    if (type == null) {
      expression.setStaticType(dynamicType);
    } else {
      expression.setStaticType(type);
    }
    return null;
  }

  /**
   * Resolve the types in the given with and implements clauses and associate those types with the
   * given class element.
   * 
   * @param classElement the class element with which the mixin and interface types are to be
   *          associated
   * @param withClause the with clause to be resolved
   * @param implementsClause the implements clause to be resolved
   */
  private void resolve(ClassElementImpl classElement, WithClause withClause,
      ImplementsClause implementsClause) {
    if (withClause != null) {
      InterfaceType[] mixinTypes = resolveTypes(
          withClause.getMixinTypes(),
          CompileTimeErrorCode.MIXIN_OF_NON_CLASS,
          CompileTimeErrorCode.MIXIN_OF_ENUM,
          CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
      if (classElement != null) {
        classElement.setMixins(mixinTypes);
      }
    }
    if (implementsClause != null) {
      NodeList<TypeName> interfaces = implementsClause.getInterfaces();
      InterfaceType[] interfaceTypes = resolveTypes(
          interfaces,
          CompileTimeErrorCode.IMPLEMENTS_NON_CLASS,
          CompileTimeErrorCode.IMPLEMENTS_ENUM,
          CompileTimeErrorCode.IMPLEMENTS_DYNAMIC);
      if (classElement != null) {
        classElement.setInterfaces(interfaceTypes);
      }
      // TODO(brianwilkerson) Move the following checks to ErrorVerifier.
      TypeName[] typeNames = interfaces.toArray(new TypeName[interfaces.size()]);
      boolean[] detectedRepeatOnIndex = new boolean[typeNames.length];
      for (int i = 0; i < detectedRepeatOnIndex.length; i++) {
        detectedRepeatOnIndex[i] = false;
      }
      for (int i = 0; i < typeNames.length; i++) {
        TypeName typeName = typeNames[i];
        if (!detectedRepeatOnIndex[i]) {
          Element element = typeName.getName().getStaticElement();
          for (int j = i + 1; j < typeNames.length; j++) {
            TypeName typeName2 = typeNames[j];
            Identifier identifier2 = typeName2.getName();
            String name2 = identifier2.getName();
            Element element2 = identifier2.getStaticElement();
            if (element != null && element.equals(element2)) {
              detectedRepeatOnIndex[j] = true;
              reportErrorForNode(CompileTimeErrorCode.IMPLEMENTS_REPEATED, typeName2, name2);
            }
          }
        }
      }
    }
  }

  /**
   * Return the type specified by the given name.
   * 
   * @param typeName the type name specifying the type to be returned
   * @param nonTypeError the error to produce if the type name is defined to be something other than
   *          a type
   * @param enumTypeError the error to produce if the type name is defined to be an enum
   * @param dynamicTypeError the error to produce if the type name is "dynamic"
   * @return the type specified by the type name
   */
  private InterfaceType resolveType(TypeName typeName, ErrorCode nonTypeError,
      ErrorCode enumTypeError, ErrorCode dynamicTypeError) {
    Type type = typeName.getType();
    if (type instanceof InterfaceType) {
      ClassElement element = ((InterfaceType) type).getElement();
      if (element != null && element.isEnum()) {
        reportErrorForNode(enumTypeError, typeName);
        return null;
      }
      return (InterfaceType) type;
    }
    // If the type is not an InterfaceType, then visitTypeName() sets the type to be a DynamicTypeImpl
    Identifier name = typeName.getName();
    if (name.getName().equals(Keyword.DYNAMIC.getSyntax())) {
      reportErrorForNode(dynamicTypeError, name, name.getName());
    } else {
      reportErrorForNode(nonTypeError, name, name.getName());
    }
    return null;
  }

  /**
   * Resolve the types in the given list of type names.
   * 
   * @param typeNames the type names to be resolved
   * @param nonTypeError the error to produce if the type name is defined to be something other than
   *          a type
   * @param enumTypeError the error to produce if the type name is defined to be an enum
   * @param dynamicTypeError the error to produce if the type name is "dynamic"
   * @return an array containing all of the types that were resolved.
   */
  private InterfaceType[] resolveTypes(NodeList<TypeName> typeNames, ErrorCode nonTypeError,
      ErrorCode enumTypeError, ErrorCode dynamicTypeError) {
    ArrayList<InterfaceType> types = new ArrayList<InterfaceType>();
    for (TypeName typeName : typeNames) {
      InterfaceType type = resolveType(typeName, nonTypeError, enumTypeError, dynamicTypeError);
      if (type != null) {
        types.add(type);
      }
    }
    return types.toArray(new InterfaceType[types.size()]);
  }

  private void setElement(Identifier typeName, Element element) {
    if (element != null) {
      if (typeName instanceof SimpleIdentifier) {
        ((SimpleIdentifier) typeName).setStaticElement(element);
      } else if (typeName instanceof PrefixedIdentifier) {
        PrefixedIdentifier identifier = (PrefixedIdentifier) typeName;
        identifier.getIdentifier().setStaticElement(element);
        SimpleIdentifier prefix = identifier.getPrefix();
        Element prefixElement = getNameScope().lookup(prefix, getDefiningLibrary());
        if (prefixElement != null) {
          prefix.setStaticElement(prefixElement);
        }
      }
    }
  }

  /**
   * Given a parameter element, create a function type based on the given return type and parameter
   * list and associate the created type with the element.
   * 
   * @param element the parameter element whose type is to be set
   * @param returnType the (possibly {@code null}) return type of the function
   * @param parameterList the list of parameters to the function
   */
  private void setFunctionTypedParameterType(ParameterElementImpl element, TypeName returnType,
      FormalParameterList parameterList) {
    ParameterElement[] parameters = getElements(parameterList);
    FunctionTypeAliasElementImpl aliasElement = new FunctionTypeAliasElementImpl(null);
    aliasElement.setSynthetic(true);
    aliasElement.shareParameters(parameters);
    aliasElement.setReturnType(computeReturnType(returnType));
    // FunctionTypeAliasElementImpl assumes the enclosing element is a
    // CompilationUnitElement (because non-synthetic function types can only be declared
    // at top level), so to avoid breaking things, go find the compilation unit element.
    aliasElement.setEnclosingElement(element.getAncestor(CompilationUnitElement.class));
    FunctionTypeImpl type = new FunctionTypeImpl(aliasElement);
    ClassElement definingClass = element.getAncestor(ClassElement.class);
    if (definingClass != null) {
      aliasElement.shareTypeParameters(definingClass.getTypeParameters());
      type.setTypeArguments(definingClass.getType().getTypeArguments());
    } else {
      FunctionTypeAliasElement alias = element.getAncestor(FunctionTypeAliasElement.class);
      while (alias != null && alias.isSynthetic()) {
        alias = alias.getAncestor(FunctionTypeAliasElement.class);
      }
      if (alias != null) {
        aliasElement.setTypeParameters(alias.getTypeParameters());
        type.setTypeArguments(alias.getType().getTypeArguments());
      } else {
        type.setTypeArguments(TypeImpl.EMPTY_ARRAY);
      }
    }
    element.setType(type);
  }
}
