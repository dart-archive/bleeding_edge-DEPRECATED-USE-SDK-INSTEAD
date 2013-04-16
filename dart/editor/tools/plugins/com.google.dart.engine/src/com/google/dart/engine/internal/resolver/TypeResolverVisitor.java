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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.DeclaredIdentifier;
import com.google.dart.engine.ast.DefaultFormalParameter;
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
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MultiplyDefinedElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.element.PropertyInducingElement;
import com.google.dart.engine.element.TypeVariableElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ExecutableElementImpl;
import com.google.dart.engine.internal.element.FunctionTypeAliasElementImpl;
import com.google.dart.engine.internal.element.LocalVariableElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.PropertyInducingElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.internal.type.TypeImpl;
import com.google.dart.engine.internal.type.VoidTypeImpl;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;

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
   * The type representing the type 'dynamic'.
   */
  private Type dynamicType;

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
        exceptionType = getTypeProvider().getObjectType();
      } else {
        exceptionType = getType(exceptionTypeName);
      }
      recordType(exception, exceptionType);
      Element element = exception.getElement();
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
    super.visitClassDeclaration(node);
    ClassElementImpl classElement = getClassElement(node.getName());
    InterfaceType superclassType = null;
    ExtendsClause extendsClause = node.getExtendsClause();
    if (extendsClause != null) {
      superclassType = resolveType(
          extendsClause.getSuperclass(),
          CompileTimeErrorCode.EXTENDS_NON_CLASS);
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
    }
    resolve(classElement, node.getWithClause(), node.getImplementsClause());
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    super.visitClassTypeAlias(node);
    ClassElementImpl classElement = getClassElement(node.getName());
    InterfaceType superclassType = resolveType(
        node.getSuperclass(),
        CompileTimeErrorCode.EXTENDS_NON_CLASS);
    if (superclassType == null) {
      superclassType = getTypeProvider().getObjectType();
    }
    if (classElement != null && superclassType != null) {
      classElement.setSupertype(superclassType);
    }
    resolve(classElement, node.getWithClause(), node.getImplementsClause());
    return null;
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    super.visitConstructorDeclaration(node);
    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
    FunctionTypeImpl type = new FunctionTypeImpl(element);
    setTypeInformation(type, null, element.getParameters());
    type.setReturnType(((ClassElement) element.getEnclosingElement()).getType());
    element.setType(type);
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

//  @Override
//  public Void visitFunctionExpression(FunctionExpression node) {
//    super.visitFunctionExpression(node);
//    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
//    FunctionTypeImpl type = new FunctionTypeImpl(element);
//    setTypeInformation(type, null, element.getParameters());
//    element.setType(type);
//    return null;
//  }

  @Override
  public Void visitDefaultFormalParameter(DefaultFormalParameter node) {
    super.visitDefaultFormalParameter(node);
//    Expression defaultValue = node.getDefaultValue();
//    if (defaultValue != null) {
//      Type valueType = getType(defaultValue);
//      Type parameterType = getType(node.getParameter());
//      if (!valueType.isAssignableTo(parameterType)) {
    // TODO(brianwilkerson) Determine whether this is really an error. I can't find in the spec
    // anything that says it is, but a side comment from Gilad states that it should be a static
    // warning.
//        resolver.reportError(ResolverErrorCode.?, defaultValue);
//      }
//    }
    return null;
  }

  @Override
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    super.visitFieldFormalParameter(node);
    Element element = node.getIdentifier().getElement();
    if (element instanceof ParameterElementImpl) {
      ParameterElementImpl parameter = (ParameterElementImpl) element;
      Type type;
      TypeName typeName = node.getType();
      if (typeName == null) {
        // TODO(brianwilkerson) Find the field's declaration and use it's type.
        type = dynamicType;
      } else {
        type = getType(typeName);
      }
      parameter.setType(type);
    } else {
      // TODO(brianwilkerson) Report this internal error
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    super.visitFunctionDeclaration(node);
    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
    FunctionTypeImpl type = new FunctionTypeImpl(element);
    setTypeInformation(type, node.getReturnType(), element.getParameters());
    element.setType(type);
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
    super.visitFunctionTypeAlias(node);
    FunctionTypeAliasElementImpl element = (FunctionTypeAliasElementImpl) node.getElement();
    FunctionTypeImpl type = (FunctionTypeImpl) element.getType();
    setTypeInformation(type, node.getReturnType(), element.getParameters());
    return null;
  }

  @Override
  public Void visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    super.visitFunctionTypedFormalParameter(node);
    ParameterElementImpl element = (ParameterElementImpl) node.getIdentifier().getElement();
    FunctionTypeImpl type = new FunctionTypeImpl((ExecutableElement) null);
    setTypeInformation(type, node.getReturnType(), getElements(node.getParameters()));
    element.setType(type);
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    super.visitMethodDeclaration(node);
    ExecutableElementImpl element = (ExecutableElementImpl) node.getElement();
    FunctionTypeImpl type = new FunctionTypeImpl(element);
    setTypeInformation(type, node.getReturnType(), element.getParameters());
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
    Element element = node.getIdentifier().getElement();
    if (element instanceof ParameterElement) {
      ((ParameterElementImpl) element).setType(declaredType);
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
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
      ASTNode parent = node.getParent();
      if (typeName instanceof PrefixedIdentifier && parent instanceof ConstructorName
          && argumentList == null) {
        ConstructorName name = (ConstructorName) parent;
        if (name.getName() == null) {
          SimpleIdentifier prefix = ((PrefixedIdentifier) typeName).getPrefix();
          element = getNameScope().lookup(prefix, getDefiningLibrary());
          if (element instanceof PrefixElement) {
            // TODO(brianwilkerson) Report this error.
//            resolver.reportError(ResolverErrorCode.UNDECLARED, ((PrefixedIdentifier) typeName).getIdentifier());
            return null;
          } else if (element != null) {
            //
            // Rewrite the constructor name. The parser, when it sees a constructor named "a.b",
            // cannot tell whether "a" is a prefix and "b" is a class name, or whether "a" is a
            // class name and "b" is a constructor name. It arbitrarily chooses the former, but
            // in this case was wrong.
            //
            name.setName(((PrefixedIdentifier) typeName).getIdentifier());
            name.setPeriod(((PrefixedIdentifier) typeName).getPeriod());
            node.setName(prefix);
            typeName = prefix;
          }
        }
      }
    }
    if (element == null) {
      // We couldn't resolve the type name.
      // TODO(jwren) Consider moving the check for CompileTimeErrorCode.BUILT_IN_IDENTIFIER_AS_TYPE
      // from the ErrorVerifier, so that we don't have two errors on a built in identifier being
      // used as a class name. See CompileTimeErrorCodeTest.test_builtInIdentifierAsType().
      Identifier simpleIdentifier;
      if (typeName instanceof SimpleIdentifier) {
        simpleIdentifier = typeName;
      } else {
        simpleIdentifier = ((PrefixedIdentifier) typeName).getPrefix();
      }
      reportError(StaticWarningCode.UNDEFINED_CLASS, simpleIdentifier, simpleIdentifier.getName());
      setElement(typeName, dynamicType.getElement());
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
    } else if (element instanceof TypeVariableElement) {
      setElement(typeName, element);
      type = ((TypeVariableElement) element).getType();
      if (argumentList != null) {
        // Type variables cannot have type arguments.
        // TODO(brianwilkerson) Report this error.
//      resolver.reportError(ResolverErrorCode.?, keyType);
      }
    } else if (element instanceof MultiplyDefinedElement) {
      Element[] elements = ((MultiplyDefinedElement) element).getConflictingElements();
      type = getType(elements);
      if (type != null) {
        node.setType(type);
      }
    } else {
      // The name does not represent a type.
      // TODO(brianwilkerson) Report this error
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
      int count = Math.min(argumentCount, parameterCount);
      ArrayList<Type> typeArguments = new ArrayList<Type>(count);
      for (int i = 0; i < count; i++) {
        Type argumentType = getType(arguments.get(i));
        if (argumentType != null) {
          typeArguments.add(argumentType);
        }
      }
      if (argumentCount != parameterCount) {
        reportError(
            getInvalidTypeParametersErrorCode(node),
            node,
            typeName.getName(),
            parameterCount,
            argumentCount);
      }
      argumentCount = typeArguments.size();
      if (argumentCount < parameterCount) {
        //
        // If there were too many arguments, we already handled it by not adding the values of the
        // extra arguments to the list. If there are too few, we handle it by adding 'dynamic'
        // enough times to make the count equal.
        //
        for (int i = argumentCount; i < parameterCount; i++) {
          typeArguments.add(dynamicType);
        }
      }
      if (type instanceof InterfaceTypeImpl) {
        InterfaceTypeImpl interfaceType = (InterfaceTypeImpl) type;
        type = interfaceType.substitute(typeArguments.toArray(new Type[typeArguments.size()]));
      } else if (type instanceof FunctionTypeImpl) {
        FunctionTypeImpl functionType = (FunctionTypeImpl) type;
        type = functionType.substitute(typeArguments.toArray(new Type[typeArguments.size()]));
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
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    Type declaredType;
    TypeName typeName = ((VariableDeclarationList) node.getParent()).getType();
    if (typeName == null) {
      declaredType = dynamicType;
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getName().getElement();
    if (element instanceof VariableElement) {
      ((VariableElementImpl) element).setType(declaredType);
      if (element instanceof PropertyInducingElement) {
        PropertyInducingElement variableElement = (PropertyInducingElement) element;
        PropertyAccessorElementImpl getter = (PropertyAccessorElementImpl) variableElement.getGetter();
        FunctionTypeImpl getterType = new FunctionTypeImpl(getter);
        getterType.setReturnType(declaredType);
        getter.setType(getterType);

        PropertyAccessorElementImpl setter = (PropertyAccessorElementImpl) variableElement.getSetter();
        if (setter != null) {
          FunctionTypeImpl setterType = new FunctionTypeImpl(setter);
          setterType.setReturnType(VoidTypeImpl.getInstance());
          setterType.setNormalParameterTypes(new Type[] {declaredType});
          setter.setType(setterType);
        }
      }
    } else {
      // TODO(brianwilkerson) Report the internal error.
    }
    return null;
  }

  /**
   * Return the class element that represents the class whose name was provided.
   * 
   * @param identifier the name from the declaration of a class
   * @return the class element that represents the class
   */
  private ClassElementImpl getClassElement(SimpleIdentifier identifier) {
    if (identifier == null) {
      // TODO(brianwilkerson) Report this
      // Internal error: We should never build a class declaration without a name.
      return null;
    }
    Element element = identifier.getElement();
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
      ParameterElement element = (ParameterElement) parameter.getIdentifier().getElement();
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
    ASTNode parent = node.getParent();
    if (parent instanceof ConstructorName) {
      parent = parent.getParent();
      if (parent instanceof InstanceCreationExpression) {
        if (((InstanceCreationExpression) parent).isConst()) {
          return CompileTimeErrorCode.CONST_WITH_INVALID_TYPE_PARAMETERS;
        } else {
          return CompileTimeErrorCode.NEW_WITH_INVALID_TYPE_PARAMETERS;
        }
      }
    }
    return StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS;
  }

  /**
   * Given the multiple elements to which a single name could potentially be resolved, return the
   * single interface type that should be used, or {@code null} if there is no clear choice.
   * 
   * @param elements the elements to which a single name could potentially be resolved
   * @return the single interface type that should be used for the type name
   */
  private InterfaceType getType(Element[] elements) {
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
          CompileTimeErrorCode.MIXIN_OF_NON_CLASS);
      if (classElement != null) {
        classElement.setMixins(mixinTypes);
      }
    }
    if (implementsClause != null) {
      NodeList<TypeName> interfaces = implementsClause.getInterfaces();
      InterfaceType[] interfaceTypes = resolveTypes(
          interfaces,
          CompileTimeErrorCode.IMPLEMENTS_NON_CLASS);
      TypeName[] typeNames = interfaces.toArray(new TypeName[interfaces.size()]);
      String dynamicKeyword = Keyword.DYNAMIC.getSyntax();
      boolean[] detectedRepeatOnIndex = new boolean[typeNames.length];
      for (int i = 0; i < detectedRepeatOnIndex.length; i++) {
        detectedRepeatOnIndex[i] = false;
      }
      for (int i = 0; i < typeNames.length; i++) {
        TypeName typeName = typeNames[i];
        String name = typeName.getName().getName();
        if (name.equals(dynamicKeyword)) {
          reportError(CompileTimeErrorCode.IMPLEMENTS_DYNAMIC, typeName);
        } else {
          Element element = typeName.getName().getElement();
          if (element != null && element.equals(classElement)) {
            reportError(CompileTimeErrorCode.IMPLEMENTS_SELF, typeName, name);
          }
        }
        if (!detectedRepeatOnIndex[i]) {
          for (int j = i + 1; j < typeNames.length; j++) {
            Element element = typeName.getName().getElement();
            TypeName typeName2 = typeNames[j];
            Identifier identifier2 = typeName2.getName();
            String name2 = identifier2.getName();
            Element element2 = identifier2.getElement();
            if (element != null && element.equals(element2)) {
              detectedRepeatOnIndex[j] = true;
              reportError(CompileTimeErrorCode.IMPLEMENTS_REPEATED, typeName2, name2);
            }
          }
        }
      }
      if (classElement != null) {
        classElement.setInterfaces(interfaceTypes);
      }
    }
  }

  /**
   * Return the type specified by the given name.
   * 
   * @param typeName the type name specifying the type to be returned
   * @param nonTypeError the error to produce if the type name is defined to be something other than
   *          a type
   * @return the type specified by the type name
   */
  private InterfaceType resolveType(TypeName typeName, ErrorCode nonTypeError) {
    Type type = typeName.getType();
    if (type instanceof InterfaceType) {
      return (InterfaceType) type;
    }
    // If the type is not an InterfaceType, then visitTypeName() sets the type to be a DynamicTypeImpl
    Identifier name = typeName.getName();
    if (!name.getName().equals(Keyword.DYNAMIC.getSyntax())) {
      reportError(nonTypeError, name, name.getName());
    }
    return null;
  }

  /**
   * Resolve the types in the given list of type names.
   * 
   * @param typeNames the type names to be resolved
   * @param nonTypeError the error to produce if the type name is defined to be something other than
   *          a type
   * @return an array containing all of the types that were resolved.
   */
  private InterfaceType[] resolveTypes(NodeList<TypeName> typeNames, ErrorCode nonTypeError) {
    ArrayList<InterfaceType> types = new ArrayList<InterfaceType>();
    for (TypeName typeName : typeNames) {
      InterfaceType type = resolveType(typeName, nonTypeError);
      if (type != null) {
        types.add(type);
      }
    }
    return types.toArray(new InterfaceType[types.size()]);
  }

  private void setElement(Identifier typeName, Element element) {
    if (element != null) {
      if (typeName instanceof SimpleIdentifier) {
        ((SimpleIdentifier) typeName).setElement(element);
      } else if (typeName instanceof PrefixedIdentifier) {
        PrefixedIdentifier identifier = (PrefixedIdentifier) typeName;
        identifier.getIdentifier().setElement(element);
        SimpleIdentifier prefix = identifier.getPrefix();
        Element prefixElement = getNameScope().lookup(prefix, getDefiningLibrary());
        if (prefixElement != null) {
          prefix.setElement(prefixElement);
        }
      }
    }
  }

  /**
   * Set the return type and parameter type information for the given function type based on the
   * given return type and parameter elements.
   * 
   * @param functionType the function type to be filled in
   * @param returnType the return type of the function, or {@code null} if no type was declared
   * @param parameters the elements representing the parameters to the function
   */
  private void setTypeInformation(FunctionTypeImpl functionType, TypeName returnType,
      ParameterElement[] parameters) {
    ArrayList<Type> normalParameterTypes = new ArrayList<Type>();
    ArrayList<Type> optionalParameterTypes = new ArrayList<Type>();
    LinkedHashMap<String, Type> namedParameterTypes = new LinkedHashMap<String, Type>();
    for (ParameterElement parameter : parameters) {
      switch (parameter.getParameterKind()) {
        case REQUIRED:
          normalParameterTypes.add(parameter.getType());
          break;
        case POSITIONAL:
          optionalParameterTypes.add(parameter.getType());
          break;
        case NAMED:
          namedParameterTypes.put(parameter.getName(), parameter.getType());
          break;
      }
    }
    if (!normalParameterTypes.isEmpty()) {
      functionType.setNormalParameterTypes(normalParameterTypes.toArray(new Type[normalParameterTypes.size()]));
    }
    if (!optionalParameterTypes.isEmpty()) {
      functionType.setOptionalParameterTypes(optionalParameterTypes.toArray(new Type[optionalParameterTypes.size()]));
    }
    if (!namedParameterTypes.isEmpty()) {
      functionType.setNamedParameterTypes(namedParameterTypes);
    }
    if (returnType == null) {
      functionType.setReturnType(dynamicType);
    } else {
      functionType.setReturnType(returnType.getType());
    }
  }
}
