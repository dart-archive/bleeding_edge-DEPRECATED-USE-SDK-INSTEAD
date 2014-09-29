package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.EnumDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.TypeParameterTypeImpl;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;

/**
 * Instances of the class {@code SecondTypeResolverVisitor} are used to finish any resolve steps
 * after the {@link TypeResolverVisitor} that cannot happen in the {@link TypeResolverVisitor}, but
 * should happen before the next tasks.
 * <p>
 * Currently this visitor only finishes the resolution of {@link ClassTypeAlias}s, thus the scopes
 * of other top level AST nodes do not currently have to be built.
 * 
 * @coverage dart.engine.resolver
 */
public class ImplicitConstructorBuilder extends ScopedVisitor {

  /**
   * Initialize a newly created visitor to finish resolution in the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ImplicitConstructorBuilder(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
  }

  /**
   * Initialize a newly created visitor to finish resolution in the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ImplicitConstructorBuilder(ResolvableLibrary library, Source source,
      TypeProvider typeProvider) {
    super(library, source, typeProvider);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    super.visitClassTypeAlias(node);
    InterfaceType superclassType = null;

    Type type = node.getSuperclass().getType();
    if (type instanceof InterfaceType) {
      superclassType = (InterfaceType) type;
    } else {
      superclassType = getTypeProvider().getObjectType();
    }

    ClassElementImpl classElement = (ClassElementImpl) node.getElement();
    if (classElement != null) {
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
    return null;
  }

  @Override
  public Void visitEnumDeclaration(EnumDeclaration node) {
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    return null;
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    return null;
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
}
