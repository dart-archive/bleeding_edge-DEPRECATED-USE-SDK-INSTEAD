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

import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.WithClause;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.resolver.scope.ClassScope;
import com.google.dart.engine.resolver.scope.LibraryScope;
import com.google.dart.engine.resolver.scope.Scope;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;

/**
 * Instances of the class {@code TypeResolverVisitor} are used to resolve the types associated with
 * supertypes, mixins and interfaces and finishes building the type hierarchy.
 */
public class TypeResolverVisitor extends RecursiveASTVisitor<Void> {
  /**
   * The element for the library containing the compilation unit being visited.
   */
  private LibraryElement definingLibrary;

  /**
   * The source representing the compilation unit being visited.
   */
  private Source source;

  /**
   * The error listener that will be informed of any errors that are found during resolution.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The object providing access to the types defined by the language.
   */
  private TypeProvider typeProvider;

  /**
   * The scope used to resolve identifiers.
   */
  private Scope nameScope;

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public TypeResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    this.definingLibrary = library.getLibraryElement();
    this.source = source;
    LibraryScope libraryScope = library.getLibraryScope();
    this.errorListener = libraryScope.getErrorListener();
    this.nameScope = libraryScope;
    this.typeProvider = typeProvider;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElementImpl classElement = getClassElement(node.getName());
    Scope outerScope = nameScope;
    try {
      if (classElement != null) {
        // TODO(brianwilkerson) This could (and probably should) be the type parameter scope rather
        // than the class scope.
        nameScope = new ClassScope(nameScope, classElement);
      }
      InterfaceType superclassType = null;
      ExtendsClause extendsClause = node.getExtendsClause();
      if (extendsClause != null) {
        // TODO(brianwilkerson) Report these errors.
        superclassType = resolveType(extendsClause.getSuperclass(), null, null, null);
      }
      if (classElement != null) {
        if (superclassType == null) {
          InterfaceType objectType = typeProvider.getObjectType();
          if (classElement.getType() != objectType) {
            superclassType = objectType;
          }
        }
        classElement.setSupertype(superclassType);
      }
      resolve(classElement, node.getWithClause(), node.getImplementsClause());
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitClassTypeAlias(ClassTypeAlias node) {
    ClassElementImpl classElement = getClassElement(node.getName());
    Scope outerScope = nameScope;
    try {
      if (classElement != null) {
        // TODO(brianwilkerson) This could (and probably should) be the type parameter scope rather
        // than the class scope.
        nameScope = new ClassScope(nameScope, classElement);
      }
      // TODO(brianwilkerson) Report these errors.
      InterfaceType superclassType = resolveType(node.getSuperclass(), null, null, null);
      if (superclassType == null) {
        superclassType = typeProvider.getObjectType();
      }
      if (classElement != null && superclassType != null) {
        classElement.setSupertype(superclassType);
      }
      resolve(classElement, node.getWithClause(), node.getImplementsClause());
    } finally {
      nameScope = outerScope;
    }
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    //
    // This optimization is only valid as long as neither class declarations nor class type aliases
    // can be nested inside any other top-level member.
    //
    for (CompilationUnitMember member : node.getDeclarations()) {
      if (member instanceof ClassDeclaration || member instanceof ClassTypeAlias) {
        member.accept(this);
      }
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
   * Report an error with the given error code and arguments.
   * 
   * @param errorCode the error code of the error to be reported
   * @param identifier the identifier specifying the location of the error
   * @param arguments the arguments to the error, used to compose the error message
   */
  private void reportError(ResolverErrorCode errorCode, Identifier identifier, Object... arguments) {
    errorListener.onError(new AnalysisError(
        source,
        identifier.getOffset(),
        identifier.getLength(),
        errorCode,
        arguments));
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
    Element element = nameScope.lookup(name, definingLibrary);
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
}
