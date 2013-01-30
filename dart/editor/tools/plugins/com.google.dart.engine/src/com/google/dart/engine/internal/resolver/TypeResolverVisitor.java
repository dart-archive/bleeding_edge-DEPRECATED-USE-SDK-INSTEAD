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

import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.ConstructorDeclaration;
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
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.TypeAliasElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ExecutableElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.TypeAliasElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
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
 */
public class TypeResolverVisitor extends ScopedVisitor {
  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public TypeResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
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
      // TODO(brianwilkerson) Report these errors.
      superclassType = resolveType(extendsClause.getSuperclass(), null, null, null);
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
    // TODO(brianwilkerson) Report these errors.
    InterfaceType superclassType = resolveType(node.getSuperclass(), null, null, null);
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
    // TODO(brianwilkerson) Get the return type of the function
    setTypeInformation(type, null, element.getParameters());
    element.setType(type);
    return null;
  }

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
  public Void visitFieldFormalParameter(FieldFormalParameter node) {
    super.visitFieldFormalParameter(node);
    Element element = node.getIdentifier().getElement();
    if (element instanceof ParameterElementImpl) {
      ParameterElementImpl parameter = (ParameterElementImpl) element;
      Type type;
      TypeName typeName = node.getType();
      if (typeName == null) {
        // TODO(brianwilkerson) Find the field's declaration and use it's type.
        type = getTypeProvider().getDynamicType();
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
    TypeAliasElementImpl element = (TypeAliasElementImpl) node.getElement();
    FunctionTypeImpl type = new FunctionTypeImpl(element);
    setTypeInformation(type, node.getReturnType(), element.getParameters());
    element.setType(type);
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
    return null;
  }

  @Override
  public Void visitSimpleFormalParameter(SimpleFormalParameter node) {
    super.visitSimpleFormalParameter(node);
    Type declaredType;
    TypeName typeName = node.getType();
    if (typeName == null) {
      declaredType = getTypeProvider().getDynamicType();
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
    Element element = getNameScope().lookup(typeName, getDefiningLibrary());
    Type type;
    if (element == null) {
      // TODO(brianwilkerson) Report this error
      return null;
    } else if (element instanceof ClassElement) {
      setElement(typeName, element);
      type = ((ClassElement) element).getType();
    } else if (element instanceof TypeAliasElement) {
      setElement(typeName, element);
      type = ((TypeAliasElement) element).getType();
    } else {
      // TODO(brianwilkerson) Report this error
      return null;
    }
    if (type == null) {
      return null;
    }
    TypeArgumentList argumentList = node.getTypeArguments();
    if (argumentList != null) {
      NodeList<TypeName> arguments = argumentList.getArguments();
      int argumentCount = arguments.size();
      int parameterCount = (type instanceof InterfaceType)
          ? ((InterfaceType) type).getTypeArguments().length
          : ((FunctionType) type).getTypeArguments().length;
      if (argumentCount != parameterCount) {
        // TODO(brianwilkerson) Report this error.
//      resolver.reportError(ResolverErrorCode.?, keyType);
      }
      ArrayList<Type> typeArguments = new ArrayList<Type>(argumentCount);
      for (int i = 0; i < argumentCount; i++) {
        Type argumentType = getType(arguments.get(i));
        if (argumentType != null) {
          typeArguments.add(argumentType);
        }
      }
      if (type instanceof InterfaceTypeImpl) {
        InterfaceTypeImpl interfaceType = (InterfaceTypeImpl) type;
        argumentCount = typeArguments.size(); // Recomputed in case any argument type was null
        if (interfaceType.getTypeArguments().length == argumentCount) {
          type = interfaceType.substitute(typeArguments.toArray(new Type[argumentCount]));
        } else {
          // TODO(brianwilkerson) Report this error (unless it already was).
//        resolver.reportError(ResolverErrorCode.?, keyType);
        }
      } else if (type instanceof FunctionTypeImpl) {
        FunctionTypeImpl functionType = (FunctionTypeImpl) type;
        argumentCount = typeArguments.size(); // Recomputed in case any argument type was null
        if (functionType.getTypeArguments().length == argumentCount) {
          type = functionType.substitute(typeArguments.toArray(new Type[argumentCount]));
        } else {
          // TODO(brianwilkerson) Report this error (unless it already was).
//          resolver.reportError(ResolverErrorCode.?, keyType);
        }
      } else {
        // TODO(brianwilkerson) Report this error.
//      resolver.reportError(ResolverErrorCode.?, keyType);
      }
    }
    node.setType(type);
    return null;
  }

  @Override
  public Void visitVariableDeclaration(VariableDeclaration node) {
    super.visitVariableDeclaration(node);
    Type declaredType;
    TypeName typeName = ((VariableDeclarationList) node.getParent()).getType();
    if (typeName == null) {
      declaredType = getTypeProvider().getDynamicType();
    } else {
      declaredType = getType(typeName);
    }
    Element element = node.getName().getElement();
    if (element instanceof ParameterElement) {
      ((ParameterElementImpl) element).setType(declaredType);
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
   * Return the type represented by the given type name.
   * 
   * @param typeName the type name representing the type to be returned
   * @return the type represented by the type name
   */
  private Type getType(TypeName typeName) {
    return typeName.getType();
  }

  /**
   * Record that the static type of the given node is the given type.
   * 
   * @param expression the node whose type is to be recorded
   * @param type the static type of the node
   */
  private Void recordType(Expression expression, Type type) {
    if (type == null) {
      expression.setStaticType(getTypeProvider().getDynamicType());
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
      // TODO(brianwilkerson) Report these errors.
      InterfaceType[] mixinTypes = resolveTypes(withClause.getMixinTypes(), null, null, null);
      if (classElement != null) {
        classElement.setMixins(mixinTypes);
      }
    }
    if (implementsClause != null) {
      // TODO(brianwilkerson) Report these errors.
      InterfaceType[] interfaceTypes = resolveTypes(
          implementsClause.getInterfaces(),
          null,
          null,
          null);
      if (classElement != null) {
        classElement.setInterfaces(interfaceTypes);
      }
    }
  }

  /**
   * Return the type specified by the given name.
   * 
   * @param typeName the type name specifying the type to be returned
   * @param undefinedError the error to produce if the type name is not defined
   * @param nonTypeError the error to produce if the type name is defined to be something other than
   *          a type
   * @param nonInterfaceType the error to produce if the type is not an interface type
   * @return the type specified by the type name
   */
  private InterfaceType resolveType(TypeName typeName, ResolverErrorCode undefinedError,
      ResolverErrorCode nonTypeError, ResolverErrorCode nonInterfaceType) {
    // TODO(brianwilkerson) Share code with StaticTypeAnalyzer
    Identifier name = typeName.getName();
    Element element = getNameScope().lookup(name, getDefiningLibrary());
    if (element == null) {
      // TODO(brianwilkerson) Handle the case where a prefixed identifier is not referencing a prefix.
      reportError(undefinedError, name);
    } else if (element instanceof ClassElement) {
      Type classType = ((ClassElement) element).getType();
      // TODO(brianwilkerson) This does not handle the type arguments.
      typeName.setType(classType);
      if (classType instanceof InterfaceType) {
        return (InterfaceType) classType;
      }
      reportError(nonInterfaceType, name);
    } else {
      reportError(nonTypeError, name);
    }
    return null;
  }

  /**
   * Resolve the types in the given list of type names.
   * 
   * @param typeNames the type names to be resolved
   * @param undefinedError the error to produce if the type name is not defined
   * @param nonTypeError the error to produce if the type name is defined to be something other than
   *          a type
   * @param nonInterfaceType the error to produce if the type is not an interface type
   * @return an array containing all of the types that were resolved.
   */
  private InterfaceType[] resolveTypes(NodeList<TypeName> typeNames,
      ResolverErrorCode undefinedError, ResolverErrorCode nonTypeError,
      ResolverErrorCode nonInterfaceType) {
    ArrayList<InterfaceType> types = new ArrayList<InterfaceType>();
    for (TypeName typeName : typeNames) {
      InterfaceType type = resolveType(typeName, undefinedError, nonTypeError, nonInterfaceType);
      if (type != null) {
        types.add(type);
      }
    }
    return types.toArray(new InterfaceType[types.size()]);
  }

  private void setElement(Identifier typeName, Element element) {
    if (element != null) {
      typeName.setElement(element);
      if (typeName instanceof PrefixedIdentifier) {
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
    functionType.setNormalParameterTypes(normalParameterTypes.toArray(new Type[normalParameterTypes.size()]));
    functionType.setOptionalParameterTypes(optionalParameterTypes.toArray(new Type[optionalParameterTypes.size()]));
    functionType.setNamedParameterTypes(namedParameterTypes);
    if (returnType != null) {
      functionType.setReturnType(returnType.getType());
    } else {
      functionType.setReturnType(getTypeProvider().getDynamicType());
    }
  }
}
