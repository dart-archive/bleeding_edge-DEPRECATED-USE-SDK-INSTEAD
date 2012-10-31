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
package com.google.dart.tools.core.utilities.bindings;

import com.google.common.base.Objects;
import com.google.common.collect.MapMaker;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.PackageLibraryManager;
import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.ast.LibraryUnit;
import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.resolver.ConstructorElement;
import com.google.dart.compiler.resolver.Element;
import com.google.dart.compiler.resolver.EnclosingElement;
import com.google.dart.compiler.resolver.FieldElement;
import com.google.dart.compiler.resolver.FunctionAliasElement;
import com.google.dart.compiler.resolver.LabelElement;
import com.google.dart.compiler.resolver.LibraryElement;
import com.google.dart.compiler.resolver.MethodElement;
import com.google.dart.compiler.resolver.TypeVariableElement;
import com.google.dart.compiler.resolver.VariableElement;
import com.google.dart.compiler.type.InterfaceType;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartLibraryImpl;
import com.google.dart.tools.core.internal.model.DartModelManager;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import org.eclipse.core.resources.IResource;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>BindingUtils</code> defines a number of utility methods. These methods ought to
 * be divided into multiple utility classes.
 */
public class BindingUtils {
  /**
   * Instances of the class <code>CacheEntry</code> maintain type maps for libraries and the
   * modification stamp of the library so that we can tell when the type map is stale and needs to
   * be re-created.
   */
  private static class CacheEntry {
    /**
     * The modification stamp of the library file.
     */
    private long modificationStamp;

    /**
     * A table mapping type names to a list of types defined with that name.
     */
    private HashMap<String, List<Type>> typeMap = new HashMap<String, List<Type>>();

    /**
     * A table mapping function type alias names to a list of function type aliases defined with
     * that name.
     */
    private HashMap<String, List<DartFunctionTypeAlias>> functionTypeAliasMap = new HashMap<String, List<DartFunctionTypeAlias>>();

    /**
     * A table mapping function names to a list of top-level functions defined with that name.
     */
    private HashMap<String, List<DartFunction>> functionMap = new HashMap<String, List<DartFunction>>();

    /**
     * Initialize a newly created cache entry to have the given modification stamp.
     * 
     * @param modificationStamp the modification stamp of the library file
     */
    public CacheEntry(long modificationStamp) {
      this.modificationStamp = modificationStamp;
    }

    /**
     * Given the current modification stamp of the library file, return <code>true</code> if the
     * cached type map is out of date.
     * 
     * @param modificationStamp the current modification stamp of the library file
     * @return <code>true</code> if the cached type map is out of date
     */
    public boolean isOutOfDate(long modificationStamp) {
      return this.modificationStamp != modificationStamp;
    }
  }

  private static final Map<LibraryElement, DartLibrary> libraryElementToModel = new MapMaker().weakKeys().softValues().makeMap();

  /**
   * A table mapping the handle identifiers of libraries to a cache entry holding a type map for
   * that library.
   */
  private static final HashMap<String, CacheEntry> libraryCache = new HashMap<String, CacheEntry>();

  /**
   * Return the Dart model element corresponding to the given type element.
   * 
   * @param element the type element used to locate the model element
   * @return the Dart model element corresponding to the resolved type
   */
  public static CompilationUnitElement getDartElement(ClassElement element) {
    if (element == null) {
      return null;
    }
    Element parent = element.getEnclosingElement();
    if (parent instanceof LibraryElement) {
      DartLibrary library = getDartElement((LibraryElement) parent);
      if (library != null) {
        return getDartElement(library, element);
      }
    }
    String typeName = element.getName();
    try {
      Set<Type> matchingTypes = new HashSet<Type>();
      addTypes(matchingTypes, typeName);
      if (matchingTypes.size() == 1) {
        return matchingTypes.iterator().next();
      }
    } catch (DartModelException exception) {
      DartCore.logError(exception);
    }
    return null;
  }

  public static DartFunction getDartElement(CompilationUnit unit,
      com.google.dart.compiler.ast.DartFunction node) {
    if (node == null) {
      return null;
    }
    DartNode parent = node.getParent();
    if (parent instanceof DartMethodDefinition) {
      // This function is essentially the body of the method.
      return getDartElement(unit, (DartMethodDefinition) parent);
    }
    while (parent != null) {
      if (parent instanceof DartUnit) {
        try {
          return findFunction(node, unit.getChildren());
        } catch (DartModelException exception) {
          DartCore.logError("Could not get children of " + unit.getElementName(), exception);
        }
        return null;
      } else if (parent instanceof DartMethodDefinition) {
        DartFunction method = getDartElement(unit, (DartMethodDefinition) parent);
        if (method != null) {
          try {
            return findFunction(node, method.getChildren());
          } catch (DartModelException exception) {
            DartCore.logError("Could not get children of " + method.getElementName(), exception);
          }
        }
        return null;
      } else if (parent instanceof com.google.dart.compiler.ast.DartFunction
          && !(parent.getParent() instanceof com.google.dart.compiler.ast.DartFunction)) {
        DartFunction function = getDartElement(
            unit,
            (com.google.dart.compiler.ast.DartFunction) parent);
        if (function != null) {
          try {
            return findFunction(node, function.getChildren());
          } catch (DartModelException exception) {
            DartCore.logError("Could not get children of " + function.getElementName(), exception);
          }
        }
        return null;
      }
      // TODO(brianwilkerson) There are other places that can contain functions that are not yet
      // being handled, such as field declarations.
      DartCore.notYetImplemented();
      parent = parent.getParent();
    }
    return null;
  }

  public static DartFunctionTypeAlias getDartElement(CompilationUnit unit,
      com.google.dart.compiler.ast.DartFunctionTypeAlias node) {
    if (node == null) {
      return null;
    }
    String typeName = node.getName().getName();
    try {
      for (DartFunctionTypeAlias alias : unit.getFunctionTypeAliases()) {
        if (alias.getElementName().equals(typeName)) {
          return alias;
        }
      }
    } catch (DartModelException exception) {
      // Fall through to return null
    }
    return null;
  }

  public static Type getDartElement(CompilationUnit unit, DartClass node) {
    if (node == null) {
      return null;
    }
    String className = node.getClassName();
    try {
      for (Type type : unit.getTypes()) {
        if (type.getElementName().equals(className)) {
          return type;
        }
      }
    } catch (DartModelException exception) {
      // Fall through to return null
    }
    return null;
  }

  public static Field getDartElement(CompilationUnit unit, DartField node) {
    if (node == null) {
      return null;
    }
    Element binding = node.getElement();
    if (binding == null) {
      return null;
    }
    String fieldName = binding.getOriginalName();
    DartClass enclosingType = getEnclosingType(node);
    Type definingType = getDartElement(unit, enclosingType);
    if (definingType != null) {
      try {
        for (Field field : definingType.getFields()) {
          if (field.getElementName().equals(fieldName)) {
            return field;
          }
        }
      } catch (DartModelException exception) {
        // Fall through to return null
      }
    }
    return null;
  }

  public static DartFunction getDartElement(CompilationUnit unit, DartMethodDefinition node) {
    if (node == null) {
      return null;
    }
    Element binding = node.getElement();
    if (binding == null) {
      return null;
    }
    String methodName = binding.getOriginalName();
    DartClass enclosingType = getEnclosingType(node);
    if (enclosingType == null) {
      try {
        for (DartElement element : unit.getChildren()) {
          if (element instanceof DartFunction) {
            if (element.getElementName().equals(methodName)) {
              return (DartFunction) element;
            }
          }
        }
      } catch (DartModelException exception) {
        DartCore.logError("Could not get children of " + unit.getElementName(), exception);
      }
      return null;
    }
    Type definingType = getDartElement(unit, enclosingType);
    if (definingType == null) {
      return null;
    }
    if (methodName.isEmpty()) {
      methodName = definingType.getElementName();
    }
    try {
      for (Method method : definingType.getMethods()) {
        if (method.getElementName().equals(methodName)) {
          return method;
        }
      }
    } catch (DartModelException exception) {
      // Fall through to return null
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given type element.
   * 
   * @param library the library containing the type in which the method is declared
   * @param element the type element used to locate the model element
   * @return the Dart model element corresponding to the resolved type
   */
  public static CompilationUnitElement getDartElement(DartLibrary library, ClassElement element) {
    if (element == null) {
      return null;
    } else if (library == null) {
      return getDartElement(element);
    }
    if (element.isDynamic()) {
      return null;
    }
    String typeName = element.getName();
    LibraryElement declaringLibraryElement = element.getLibrary();
    if (declaringLibraryElement == null) {
      DartCore.logError("Could not access declaring library for type " + typeName, new Throwable());
      return null;
    }
    DartLibrary declaringLibrary = getDartElement(library, declaringLibraryElement);
    List<Type> matchingTypes = getImmediateTypes(declaringLibrary, typeName);
    if (matchingTypes.size() == 1) {
      return matchingTypes.get(0);
    }
    List<DartFunctionTypeAlias> matchingAliases = getFunctionTypeAliases(declaringLibrary, typeName);
    if (matchingAliases.size() == 1) {
      return matchingAliases.get(0);
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given Dart compiler element.
   * 
   * @param library the library containing the element
   * @param element the compiler element used to locate the model element
   * @return the Dart model element corresponding to the compiler element or <code>null</code> if
   *         none was found
   */
  public static DartElement getDartElement(DartLibrary library, Element element) {
    if (element instanceof FunctionAliasElement) {
      return getDartElement(library, (FunctionAliasElement) element);
    } else if (element instanceof ClassElement) {
      return getDartElement(library, ((ClassElement) element).getType());
    } else if (element instanceof FieldElement) {
      return getDartElement(library, (FieldElement) element);
    } else if (element instanceof LabelElement) {
      return getDartElement(library, (LabelElement) element);
    } else if (element instanceof LibraryElement) {
      return getDartElement(library, (LibraryElement) element);
    } else if (element instanceof MethodElement) {
      return getDartElement(library, (MethodElement) element);
    } else if (element instanceof TypeVariableElement) {
      return getDartElement(library, (TypeVariableElement) element);
    } else if (element instanceof VariableElement) {
      return getDartElement(library, (VariableElement) element);
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved field.
   * 
   * @param library the library containing the type in which the field is declared
   * @param fieldBinding the resolved field used to locate the model element
   * @return the Dart model element corresponding to the resolved field
   */
  public static CompilationUnitElement getDartElement(DartLibrary library, FieldElement fieldBinding) {
    return getDartElement(library, fieldBinding, true, true);
  }

  /**
   * Return the Dart model element corresponding to the given resolved field.
   * 
   * @param library the library containing the type in which the field is declared
   * @param fieldBinding the resolved field used to locate the model element
   * @param allowGetter <code>true</code> if a getter is allowed to be returned
   * @param allowSetter <code>true</code> if a setter is allowed to be returned
   * @return the Dart model element corresponding to the resolved field
   */
  public static CompilationUnitElement getDartElement(DartLibrary library,
      FieldElement fieldBinding, boolean allowGetter, boolean allowSetter) {
    if (library == null || fieldBinding == null) {
      return null;
    }
    Element parent = fieldBinding.getEnclosingElement();
    if (parent instanceof LibraryElement) {
      String fieldName = fieldBinding.getName();
      try {
        for (CompilationUnit unit : library.getCompilationUnits()) {
          for (DartElement child : unit.getChildren()) {
            if (child instanceof DartVariableDeclaration) {
              DartVariableDeclaration globalVariable = (DartVariableDeclaration) child;
              if (globalVariable.getElementName().equals(fieldName)) {
                return globalVariable;
              }
            } else if (child instanceof DartFunction) {
              DartFunction function = (DartFunction) child;
              if (function.getElementName().equals(fieldName)) {
                return function;
              }
            }
          }
        }
      } catch (DartModelException exception) {
        // Fall through to return null.
      }
      return null;
    }
    com.google.dart.compiler.type.Type enclosingType = parent.getType();
    if (!(enclosingType instanceof InterfaceType)) {
      return null;
    }
    CompilationUnitElement member = getDartElement(library, (InterfaceType) enclosingType);
    if (!(member instanceof Type)) {
      return null;
    }
    Type declaringType = (Type) member;
    String fieldName = fieldBinding.getName();
    if (fieldName == null) {
      return null;
    }
    try {
      for (Field field : declaringType.getFields()) {
        if (fieldName.equals(field.getElementName())) {
          return field;
        }
      }
      for (Method method : declaringType.getMethods()) {
        if (fieldName.equals(method.getElementName())
            && (allowGetter && method.isGetter() || allowSetter && method.isSetter())) {
          return method;
        }
      }
    } catch (DartModelException exception) {
      // Could not access the fields, so there's nothing we can do to find the right one.
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved function alias.
   * 
   * @param library the library containing the compilation unit in which the function alias is
   *          declared
   * @param aliasBinding the resolved function alias used to locate the model element
   * @return the Dart model element corresponding to the resolved function alias
   */
  public static DartFunctionTypeAlias getDartElement(DartLibrary library,
      FunctionAliasElement aliasBinding) {
    if (aliasBinding == null) {
      return null;
    } else if (library == null) {
      return null;
    }
    FunctionAliasElement element = aliasBinding.getType().getElement();
    String typeName = element.getName();
    LibraryElement declaringLibraryElement = element.getLibrary();
    if (declaringLibraryElement == null) {
      DartCore.logError("Could not access declaring library for type " + typeName, new Throwable());
      return null;
    }
    DartLibrary declaringLibrary = getDartElement(library, declaringLibraryElement);
    List<DartFunctionTypeAlias> matchingTypes = getFunctionTypeAliases(declaringLibrary, typeName);
    if (matchingTypes.size() == 1) {
      return matchingTypes.get(0);
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved type.
   * 
   * @param library the library containing the type in which the method is declared
   * @param typeBinding the resolved type used to locate the model element
   * @return the Dart model element corresponding to the resolved type
   */
  public static CompilationUnitElement getDartElement(DartLibrary library, InterfaceType typeBinding) {
    if (typeBinding == null) {
      return null;
    } else if (library == null) {
      return getDartElement(typeBinding);
    }
    return getDartElement(library, typeBinding.getElement());
  }

  /**
   * Return the Dart model element corresponding to the given resolved label.
   * 
   * @param library the library containing the method in which the label is declared
   * @param labelBinding the resolved label used to locate the model element
   * @return the Dart model element corresponding to the resolved label
   */
  public static Field getDartElement(DartLibrary library, LabelElement labelBinding) {
    if (labelBinding == null) {
      return null;
    }
    DartCore.notYetImplemented();
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved library.
   * 
   * @param library the library from which the resolved library must be reachable, either directly
   *          (because it is explicitly imported), or indirectly
   * @param libraryBinding the resolved library used to locate the model element
   * @return the Dart model element corresponding to the resolved library
   */
  public static DartLibrary getDartElement(DartLibrary library, LibraryElement libraryBinding) {
    if (library == null) {
      return null;
    }
    LibrarySource librarySource = ((DartLibraryImpl) library).getLibrarySourceFile();
    if (librarySource == null) {
      return null;
    }
    URI libraryUri = URIUtilities.safelyResolveDartUri(librarySource.getUri());
    URI targetUri = URIUtilities.safelyResolveDartUri(libraryBinding.getLibraryUnit().getSource().getUri());
    HashSet<URI> visitedLibraries = new HashSet<URI>();
    return findLibrary(library, libraryUri, targetUri, visitedLibraries);
    // TODO(brianwilkerson) If we could not find the library it might be because we could not access
    // the imported libraries at some point. We could try enumerating all of the libraries in the
    // workspace as a fall-back.
  }

  /**
   * Return the Dart model element corresponding to the given resolved method.
   * 
   * @param library the library containing the type in which the method is declared
   * @param methodBinding the resolved method used to locate the model element
   * @return the Dart model element corresponding to the resolved method
   */
  public static DartFunction getDartElement(DartLibrary library, MethodElement methodBinding) {
    if (methodBinding == null) {
      return null;
    }
    int methodNumParameters = methodBinding.getParameters().size();
    String methodName = methodBinding.getName();
    if ("-binary".equals(methodName)) {
      methodName = "-";
    }
    Element enclosingElement = methodBinding.getEnclosingElement();
    if (enclosingElement == null) {
      // We don't have enough information to find the method or function.
      return null;
    } else if (enclosingElement instanceof LibraryElement) {
      // This is a top-level function.
      DartLibrary definingLibrary = getDartElement(library, (LibraryElement) enclosingElement);
      if (definingLibrary == null) {
        definingLibrary = library;
      }
      List<DartFunction> matchingFunctions = getImmediateFunctions(
          definingLibrary,
          methodBinding.getName());
      try {
        for (DartFunction function : matchingFunctions) {
          if (Objects.equal(methodName, function.getElementName())
              && function.getParameterNames().length == methodNumParameters) {
            return function;
          }
        }
      } catch (DartModelException exception) {
      }
      if (matchingFunctions.size() == 1) {
        return matchingFunctions.get(0);
      }
      DartCore.notYetImplemented();
      return null;
    } else if (enclosingElement instanceof MethodElement) {
      DartFunction method = getDartElement(library, (MethodElement) enclosingElement);
      return getFunction(method, methodBinding);
    }
    com.google.dart.compiler.type.Type enclosingType = enclosingElement.getType();
    if (!(enclosingType instanceof InterfaceType)) {
      return null;
    }
    CompilationUnitElement member = getDartElement(library, (InterfaceType) enclosingType);
    if (!(member instanceof Type)) {
      return null;
    }
    Type declaringType = (Type) member;
    if (methodName == null) {
      return null;
    } else if (methodBinding.isConstructor()) {
      String typeName;
      if (methodBinding instanceof ConstructorElement) {
        typeName = ((ConstructorElement) methodBinding).getConstructorType().getName();
      } else {
        typeName = declaringType.getElementName();
      }
      if (methodName.length() == 0) {
        methodName = typeName;
      } else {
        methodName = typeName + "." + methodName;
      }
    }
    try {
      for (Method method : declaringType.getMethods()) {
        if (Objects.equal(methodName, method.getElementName())
            && method.getParameterNames().length == methodNumParameters) {
          return method;
        }
      }
    } catch (DartModelException exception) {
      // Could not access the methods, so there's nothing we can do to find the right one.
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved type variable.
   * 
   * @param library the library containing the method in which the type variable is declared
   * @param variableBinding the resolved type variable used to locate the model element
   * @return the Dart model element corresponding to the resolved type variable
   */
  public static DartTypeParameter getDartElement(DartLibrary library,
      TypeVariableElement variableBinding) {
    if (variableBinding == null) {
      return null;
    }
    DartTypeParameter[] typeParameters = null;
    try {
      EnclosingElement enclosingElement = variableBinding.getEnclosingElement();
      if (enclosingElement instanceof FunctionAliasElement) {
        FunctionAliasElement aliasElement = (FunctionAliasElement) enclosingElement;
        DartFunctionTypeAlias aliasModel = getDartElement(library, aliasElement);
        typeParameters = aliasModel.getTypeParameters();
      } else if (enclosingElement instanceof ClassElement) {
        ClassElement classElement = (ClassElement) enclosingElement;
        Type typeModel = (Type) getDartElement(library, classElement);
        typeParameters = typeModel.getTypeParameters();
      }
    } catch (DartModelException exception) {
    }
    if (typeParameters != null) {
      for (DartTypeParameter typeParameter : typeParameters) {
        if (typeParameter.getElementName().equals(variableBinding.getName())) {
          return typeParameter;
        }
      }
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved variable.
   * 
   * @param library the library containing the method or function in which the variable is declared
   * @param variableBinding the resolved variable used to locate the model element
   * @return the Dart model element corresponding to the resolved variable
   */
  public static DartVariableDeclaration getDartElement(DartLibrary library,
      VariableElement variableBinding) {
    if (variableBinding == null) {
      return null;
    }
    String variableName = variableBinding.getName();
    if (variableBinding.getEnclosingElement() instanceof LibraryElement) {
      try {
        for (CompilationUnit unit : library.getCompilationUnits()) {
          for (DartVariableDeclaration variable : unit.getGlobalVariables()) {
            if (variable.getElementName().equals(variableName)) {
              return variable;
            }
          }
        }
      } catch (DartModelException exception) {
        DartCore.logError("Could not find global variable '" + variableName + "' in library "
            + library.getElementName(), exception);
      }
      return null;
    }
    if (variableBinding.getEnclosingElement() instanceof MethodElement) {
      MethodElement methodElement = (MethodElement) variableBinding.getEnclosingElement();
      DartFunction functionElement = getDartElement(library, methodElement);
      if (functionElement != null) {
        try {
          for (DartVariableDeclaration variable : functionElement.getLocalVariables()) {
            if (variable.getElementName().equals(variableName)) {
              return variable;
            }
          }
        } catch (DartModelException exception) {
          DartCore.logError("Cannot access local variables within function", exception);
        }
      }
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved type.
   * 
   * @param typeBinding the resolved type used to locate the model element
   * @return the Dart model element corresponding to the resolved type
   */
  public static CompilationUnitElement getDartElement(InterfaceType typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    return getDartElement(typeBinding.getElement());
  }

  /**
   * Return the library corresponding to the given library element.
   * 
   * @param library the library element representing the library to be returned
   * @return the library corresponding to the given library element
   */
  public static DartLibrary getDartElement(LibraryElement library) {
    DartLibrary dartLibrary = libraryElementToModel.get(library);
    if (dartLibrary == null) {
      dartLibrary = getDartElement0(library);
      if (dartLibrary != null) {
        libraryElementToModel.put(library, dartLibrary);
      }
    }
    return dartLibrary;
  }

  /**
   * Return the Dart model element corresponding to the given resolved method.
   * 
   * @param methodBinding the resolved method used to locate the model element
   * @return the Dart model element corresponding to the resolved method
   */
  public static DartFunction getDartElement(MethodElement methodBinding) {
    return getDartElement(null, methodBinding);
  }

  /**
   * Return the Dart model element corresponding to the given resolved field.
   * 
   * @param fieldBinding the resolved field used to locate the model element
   * @return the Dart model element corresponding to the resolved field
   */
  public static DartVariableDeclaration getDartElement(VariableElement fieldBinding) {
    return getDartElement(null, fieldBinding);
  }

  /**
   * Return the class element representing the type in which the given method is declared, or
   * <code>null</code> if the method is not defined in a type.
   * 
   * @param method the method whose declaring class is to be returned
   * @return the type in which the given method is declared
   */
  public static ClassElement getDeclaringType(MethodElement method) {
    Element element = method.getEnclosingElement();
    if (element instanceof ClassElement) {
      return (ClassElement) element;
    }
    return null;
  }

  /**
   * Return the nearest function that is a parent of the given node.
   * 
   * @param node the node from which the search will begin
   * @return the nearest function that is a parent of the given node
   */
  public static com.google.dart.compiler.ast.DartFunction getEnclosingFunction(DartNode node) {
    if (node == null) {
      return null;
    }
    DartNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof com.google.dart.compiler.ast.DartFunction) {
        return (com.google.dart.compiler.ast.DartFunction) parent;
      }
      parent = parent.getParent();
    }
    return null;
  }

  /**
   * Return the nearest type declaration that is a parent of the given node.
   * 
   * @param node the node from which the search will begin
   * @return the nearest type declaration that is a parent of the given node
   */
  public static DartClass getEnclosingType(DartNode node) {
    if (node == null) {
      return null;
    }
    DartNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof DartClass) {
        return (DartClass) parent;
      }
      parent = parent.getParent();
    }
    return null;
  }

  /**
   * Return the element for the library containing the given element, which can be the given element
   * if it is a library.
   * 
   * @param element the element whose library is to be returned
   * @return the element for the library containing the given element
   */
  public static LibraryElement getLibrary(Element element) {
    Element candidate = element;
    while (candidate != null) {
      if (candidate instanceof LibraryElement) {
        return (LibraryElement) candidate;
      }
      candidate = candidate.getEnclosingElement();
    }
    return null;
  }

  /**
   * Search the supertypes of the given method's declaring type for any types that define a method
   * that is overridden by the given method. Return an array containing all of the overridden
   * methods, or an empty array if there are no overridden methods. The methods in the array are not
   * guaranteed to be in any particular order.
   * <p>
   * The result will contain only immediately overridden methods. For example, given a class
   * <code>A</code>, a class <code>B</code> that extends <code>A</code>, and a class <code>C</code>
   * that extends <code>B</code>, all three of which define a method <code>m</code>, asking the
   * method defined in class <code>C</code> for it's overridden methods will return an array
   * containing only the method defined in <code>B</code>.
   * 
   * @param methodElement the method that overrides the methods to be returned
   * @return an array containing all of the methods declared in supertypes of the given method's
   *         declaring type that are overridden by the given method
   */
  public static MethodElement[] getOverriddenMethods(MethodElement methodElement) {
    List<MethodElement> overriddenMethods = new ArrayList<MethodElement>();
    String methodName = methodElement.getName();
    Element enclosingElement = methodElement.getEnclosingElement();
    if (enclosingElement instanceof ClassElement) {
      Set<ClassElement> visitedTypes = new HashSet<ClassElement>();
      List<ClassElement> targetTypes = new ArrayList<ClassElement>();
      targetTypes.add((ClassElement) enclosingElement);
      while (!targetTypes.isEmpty()) {
        ClassElement targetType = targetTypes.remove(0);
        for (InterfaceType supertype : getImmediateSupertypes(targetType)) {
          if (supertype != null) {
            ClassElement supertypeElement = supertype.getElement();
            Iterator<? extends Element> members = supertypeElement.getMembers().iterator();
            if (members.hasNext()) {
              while (members.hasNext()) {
                Element member = members.next();
                if (member instanceof MethodElement && member.getName().equals(methodName)) {
                  overriddenMethods.add((MethodElement) member);
                }
              }
            } else if (!visitedTypes.contains(supertypeElement)) {
              visitedTypes.add(supertypeElement);
              targetTypes.add(supertypeElement);
            }
          }
        }
      }
    }
    return overriddenMethods.toArray(new MethodElement[overriddenMethods.size()]);
  }

  /**
   * Return <code>true</code> if the given method is an abstract method (either explicitly declared
   * as to be abstract or defined in an interface).
   * 
   * @param method the method whose declaring class is to be returned
   * @return the type in which the given method is declared
   */
  public static boolean isAbstract(MethodElement method) {
    if (method.getModifiers().isAbstract()) {
      return true;
    }
    ClassElement declaringType = getDeclaringType(method);
    return declaringType != null && declaringType.isInterface();
  }

  /**
   * Replace all occurrence of the character to be replaced with the replacement character in a copy
   * of the given array. Returns the given array if no occurrences of the character to be replaced
   * are found. <br>
   * <br>
   * For example:
   * <ol>
   * <li>
   * 
   * <pre>
	 *	array = { 'a', 'b', 'b', 'a', 'b', 'a' }
	 *	toBeReplaced = 'b'
	 *	replacementChar = 'a'
	 *	result => A new array that is equals to { 'a', 'a', 'a', 'a', 'a', 'a' }
	 * </pre>
   * </li>
   * <li>
   * 
   * <pre>
	 *	array = { 'a', 'b', 'b', 'a', 'b', 'a' }
	 *	toBeReplaced = 'c'
	 *	replacementChar = 'a'
	 *	result => The original array that remains unchanged.
	 * </pre>
   * </li>
   * </ol>
   * 
   * @param array the given array
   * @param toBeReplaced the character to be replaced
   * @param replacementChar the replacement character
   * @throws NullPointerException if the given array is null
   */
  public static final char[] replaceOnCopy(char[] array, char toBeReplaced, char replacementChar) {
    char[] result = null;
    for (int i = 0, length = array.length; i < length; i++) {
      char c = array[i];
      if (c == toBeReplaced) {
        if (result == null) {
          result = new char[length];
          System.arraycopy(array, 0, result, 0, i);
        }
        result[i] = replacementChar;
      } else if (result != null) {
        result[i] = c;
      }
    }
    if (result == null) {
      return array;
    }
    return result;
  }

  /**
   * Traverse the given library looking for all of the function type aliases with the given name.
   * 
   * @param matchingAliases the list to which matching function type aliases are to be added
   * @param library the library containing the function type aliases to be returned
   * @param typeName the name of the function type aliases to be returned
   */
  private static void addImmediateFunctionTypeAliasesUncached(
      List<DartFunctionTypeAlias> matchingAliases, DartLibrary library, String typeName) {
    try {
      for (CompilationUnit unit : library.getCompilationUnits()) {
        try {
          for (DartFunctionTypeAlias type : unit.getFunctionTypeAliases()) {
            if (type.getElementName().equals(typeName)) {
              matchingAliases.add(type);
            }
          }
        } catch (DartModelException exception) {
//        DartCore.logInformation("Could not get function type aliases defined in " + unit.getElementName(),
//          exception);
        }
      }
    } catch (DartModelException exception) {
//    DartCore.logInformation(
//        "Could not get compilation units defined in " + library.getElementName(), exception);
    }
  }

  /**
   * Traverse the entire workspace looking for all of the types with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the type in which the method is declared
   * @param typeName the name of the types to be returned
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static void addImmediateTypes(Set<Type> matchingTypes, DartLibrary library,
      String typeName) throws DartModelException {
    CacheEntry entry = getLibraryCache(library);
    if (entry != null) {
      HashMap<String, List<Type>> typeMap = entry.typeMap;
      if (typeMap != null) {
        List<Type> typeList = typeMap.get(typeName);
        if (typeList != null) {
          matchingTypes.addAll(typeList);
        }
        return;
      }
      for (CompilationUnit unit : library.getCompilationUnits()) {
        for (Type type : unit.getTypes()) {
          if (type.getElementName().equals(typeName)) {
            matchingTypes.add(type);
          }
        }
      }
    }
  }

  /**
   * Traverse the given library looking for all of the types with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the types to be returned
   * @param typeName the name of the types to be returned
   */
  private static void addImmediateTypesUncached(List<Type> matchingTypes, DartLibrary library,
      String typeName) {
    try {
      for (CompilationUnit unit : library.getCompilationUnits()) {
        try {
          for (Type type : unit.getTypes()) {
            if (type.getElementName().equals(typeName)) {
              matchingTypes.add(type);
            }
          }
        } catch (DartModelException exception) {
//          DartCore.logInformation("Could not get types defined in " + unit.getElementName(),
//              exception);
        }
      }
    } catch (DartModelException exception) {
//      DartCore.logInformation(
//          "Could not get compilation units defined in " + library.getElementName(), exception);
    }
  }

  /**
   * Traverse the entire workspace looking for all of the types with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the type in which the method is declared
   * @param typeName the name of the types to be returned
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static void addTypes(Set<Type> matchingTypes, DartLibrary library, String typeName)
      throws DartModelException {
    addImmediateTypes(matchingTypes, library, typeName);
    for (DartLibrary importedLibrary : getAllImportedLibraries(library)) {
      addImmediateTypes(matchingTypes, importedLibrary, typeName);
    }
  }

  /**
   * Traverse the entire workspace looking for all of the types with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param typeName the name of the types to be returned
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static void addTypes(Set<Type> matchingTypes, String typeName) throws DartModelException {
    for (DartProject project : DartModelManager.getInstance().getDartModel().getDartProjects()) {
      for (DartElement child : project.getChildren()) {
        if (child instanceof DartLibrary) {
          addTypes(matchingTypes, (DartLibrary) child, typeName);
        }
      }
    }
  }

  private static DartFunction findFunction(com.google.dart.compiler.ast.DartFunction node,
      DartElement[] elements) {
    String targetName = null;
    Element binding = node.getElement();
    if (binding == null) {
      DartNode parent = node.getParent();
      if (parent instanceof DartFunctionExpression) {
        targetName = ((DartFunctionExpression) parent).getFunctionName();
      }
    } else {
      targetName = binding.getOriginalName();
    }
    if (targetName == null) {
      // We cannot locate unnamed functions
      return null;
    }
    for (DartElement element : elements) {
      if (element instanceof DartFunction) {
        DartFunction function = (DartFunction) element;
        String functionName = function.getElementName();
        if (functionName != null && functionName.equals(targetName)) {
          return function;
        }
      }
    }
    return null;
  }

  /**
   * Search the given library and all libraries imported by it for a library with the target URI.
   * 
   * @param library the library from which the resolved library must be reachable, either directly
   *          (because it is explicitly imported), or indirectly
   * @param libraryUri the URI of the first argument
   * @param targetUri the URI of the library being searched for
   * @param visitedLibraries a set containing the URI's of the libraries that have already been
   *          visited, used to prevent infinite recursion
   * @return the library with the target URI
   */
  private static DartLibrary findLibrary(DartLibrary library, URI libraryUri, URI targetUri,
      HashSet<URI> visitedLibraries) {
    if (libraryUri.equals(targetUri)) {
      return library;
    }
    visitedLibraries.add(libraryUri);
    try {
      for (DartLibrary importedLibrary : getAllImportedLibraries(library)) {
        URI importedLibraryUri = URIUtilities.safelyResolveDartUri(((DartLibraryImpl) importedLibrary).getLibrarySourceFile().getUri());
        if (!visitedLibraries.contains(importedLibraryUri)) {
          DartLibrary foundLibrary = findLibrary(
              importedLibrary,
              importedLibraryUri,
              targetUri,
              visitedLibraries);
          if (foundLibrary != null) {
            return foundLibrary;
          }
        }
      }
    } catch (DartModelException exception) {
      DartCore.logError("Could not get imported libraries for " + libraryUri, exception);
    }
    return null;
  }

  /**
   * Search the libraries defined in the given project for a library with the given URI.
   * 
   * @param project the project to be searched
   * @param targetUri the URI of the library being searched for
   * @return the library that was found, or <code>null</code> if there is no library in the project
   *         with the given URI
   */
  private static DartLibrary findLibrary(DartProject project, String targetUri) {
    if (project == null) {
      return null;
    }
    try {
      for (DartElement child : project.getChildren()) {
        if (child instanceof DartLibrary) {
          String libraryUri = ((DartLibraryImpl) child).getLibrarySourceFile().getUri().toString();
          if (targetUri.equals(libraryUri)) {
            return (DartLibrary) child;
          }
        }
      }
    } catch (DartModelException exception) {
      DartCore.logInformation("Could not get children of project " + project.getElementName()
          + " while trying to find library with URI " + targetUri, exception);
    }
    return null;
  }

  /**
   * Return a list containing all of the libraries that are imported by the given library, either
   * implicitly or explicitly.
   * 
   * @param library the library whose imported libraries are to be returned
   * @return all of the libraries that are imported by the given library
   * @throws DartModelException
   */
  private static List<DartLibrary> getAllImportedLibraries(DartLibrary library)
      throws DartModelException {
    List<DartLibrary> libraries = new ArrayList<DartLibrary>();
    boolean coreImported = false;
    for (DartLibrary importedLibrary : library.getImportedLibraries()) {
      libraries.add(importedLibrary);
      DartCore.notYetImplemented();
//      if (importedLibrary.isCore()) {
//        coreImported = true;
//      }
    }
    if (!coreImported) {
      libraries.add(DartModelManager.getInstance().getDartModel().getCoreLibrary());
    }
    return libraries;
  }

  private static DartLibrary getDartElement0(LibraryElement library) {
    if (library == null) {
      return null;
    }
    if (library.isDynamic()) {
      return null;
    }
    LibraryUnit libraryUnit = library.getLibraryUnit();
    if (libraryUnit == null) {
      return null;
    }
    LibrarySource librarySource = libraryUnit.getSource();
    URI uri = librarySource.getUri();
    if (PackageLibraryManager.isDartUri(uri)) {
      return new DartLibraryImpl(librarySource);
    }
    if (PackageLibraryManager.isPackageUri(uri)) {
      uri = PackageLibraryManagerProvider.getPackageLibraryManager().resolveDartUri(uri);
    }
    String targetUri = uri.toString();
    IResource file = com.google.dart.tools.core.internal.util.ResourceUtil.getResource(uri);
    if (file != null) {
      return findLibrary(DartCore.create(file.getProject()), targetUri);
    }
    try {
      for (DartProject project : DartModelManager.getInstance().getDartModel().getDartProjects()) {
        DartLibrary foundLibrary = findLibrary(project, targetUri);
        if (foundLibrary != null) {
          return foundLibrary;
        }
      }
    } catch (DartModelException exception) {
      DartCore.logError("Could not access Dart projects while trying to find library with URI "
          + targetUri, exception);
    }
    return null;
  }

  /**
   * Return the function within the given function with the given {@link MethodElement}, or
   * <code>null</code> if there is no such function.
   * 
   * @param parent the function within which to search
   * @param functionName the name of the function to be returned
   * @return the function within the given function with the given name
   */
  private static DartFunction getFunction(DartFunction parent, MethodElement binding) {
    if (parent == null || binding == null) {
      return null;
    }
    try {
      SourceInfo bindingInfo = binding.getSourceInfo();
      List<DartFunction> children = parent.getChildrenOfType(DartFunction.class);
      for (DartFunction child : children) {
        SourceRange childRange = child.getSourceRange();
        if (childRange.getOffset() == bindingInfo.getOffset()
            && childRange.getLength() == bindingInfo.getLength()) {
          return child;
        }
      }
    } catch (DartModelException exception) {
      DartCore.logError("Could not get children of method " + parent.getElementName(), exception);
    }
    return null;
  }

  /**
   * Traverse the entire workspace looking for all of the function type aliases with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the type in which the method is declared
   * @param typeName the name of the types to be returned
   */
  private static List<DartFunctionTypeAlias> getFunctionTypeAliases(DartLibrary library,
      String typeName) {
    if (library == null) {
      return new ArrayList<DartFunctionTypeAlias>();
    }
    CacheEntry entry = getLibraryCache(library);
    if (entry == null) {
      return new ArrayList<DartFunctionTypeAlias>();
    }
    HashMap<String, List<DartFunctionTypeAlias>> typeMap = entry.functionTypeAliasMap;
    if (typeMap != null) {
      List<DartFunctionTypeAlias> typeList = typeMap.get(typeName);
      if (typeList != null) {
        return typeList;
      }
    }
    List<DartFunctionTypeAlias> matchingAliases = new ArrayList<DartFunctionTypeAlias>();
    addImmediateFunctionTypeAliasesUncached(matchingAliases, library, typeName);
    return matchingAliases;
  }

  /**
   * Traverse the entire library looking for all of the top-level functions with the given name.
   * 
   * @param library the library containing the functions to be searched
   * @param functionName the name of the functions to be returned
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static List<DartFunction> getImmediateFunctions(DartLibrary library, String functionName) {
    if (library == null) {
      return new ArrayList<DartFunction>();
    }
    CacheEntry entry = getLibraryCache(library);
    if (entry == null) {
      return new ArrayList<DartFunction>();
    }
    HashMap<String, List<DartFunction>> functionMap = entry.functionMap;
    if (functionMap != null) {
      List<DartFunction> functionList = functionMap.get(functionName);
      if (functionList != null) {
        return functionList;
      }
    }
    List<DartFunction> matchingFunctions = new ArrayList<DartFunction>();
//    addImmediateFunctionsUncached(matchingFunctions, library, functionName);
    return matchingFunctions;
  }

  /**
   * Return the immediate supertypes of the given class element.
   * 
   * @param targetType the type whose supertypes are to be returned.
   * @return the immediate supertypes of the given class element
   */
  private static Set<InterfaceType> getImmediateSupertypes(ClassElement targetType) {
    Set<InterfaceType> supertypes = new HashSet<InterfaceType>();
    InterfaceType supertype = targetType.getSupertype();
    if (supertype != null) {
      supertypes.add(supertype);
    }
    supertypes.addAll(targetType.getInterfaces());
    return supertypes;
  }

  /**
   * Traverse the entire workspace looking for all of the types with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the type in which the method is declared
   * @param typeName the name of the types to be returned
   */
  private static List<Type> getImmediateTypes(DartLibrary library, String typeName) {
    if (library == null) {
      return new ArrayList<Type>();
    }
    CacheEntry entry = getLibraryCache(library);
    if (entry == null) {
      return new ArrayList<Type>();
    }
    HashMap<String, List<Type>> typeMap = entry.typeMap;
    if (typeMap != null) {
      List<Type> typeList = typeMap.get(typeName);
      if (typeList != null) {
        return typeList;
      }
    }
    List<Type> matchingTypes = new ArrayList<Type>();
    addImmediateTypesUncached(matchingTypes, library, typeName);
    return matchingTypes;
  }

  private static CacheEntry getLibraryCache(DartLibrary library) {
    try {
      long modificationStamp = 0;
      if (library.isLocal()) {
        IResource resource = library.getCorrespondingResource();
        if (resource == null) {
          return null;
        }
        modificationStamp = resource.getModificationStamp();
      }
      String id = library.getHandleIdentifier();
      synchronized (libraryCache) {
        CacheEntry entry = libraryCache.get(id);
        if (entry == null || entry.isOutOfDate(modificationStamp)) {
          entry = new CacheEntry(modificationStamp);
          libraryCache.put(id, entry);
          for (CompilationUnit unit : library.getCompilationUnits()) {
            for (DartElement child : unit.getChildren()) {
              if (child instanceof Type) {
                Type type = (Type) child;
                String typeName = type.getElementName();
                List<Type> typeList = entry.typeMap.get(typeName);
                if (typeList == null) {
                  typeList = new ArrayList<Type>();
                  entry.typeMap.put(typeName, typeList);
                }
                typeList.add(type);
              } else if (child instanceof DartFunctionTypeAlias) {
                DartFunctionTypeAlias alias = (DartFunctionTypeAlias) child;
                String aliasName = alias.getElementName();
                List<DartFunctionTypeAlias> aliasList = entry.functionTypeAliasMap.get(aliasName);
                if (aliasList == null) {
                  aliasList = new ArrayList<DartFunctionTypeAlias>();
                  entry.functionTypeAliasMap.put(aliasName, aliasList);
                }
                aliasList.add(alias);
              } else if (child instanceof DartFunction) {
                DartFunction function = (DartFunction) child;
                String functionName = function.getElementName();
                List<DartFunction> functionList = entry.functionMap.get(functionName);
                if (functionList == null) {
                  functionList = new ArrayList<DartFunction>();
                  entry.functionMap.put(functionName, functionList);
                }
                functionList.add(function);
              }
            }
          }
        }
        return entry;
      }
    } catch (DartModelException exception) {
      return null;
    }
  }

//  /**
//   * Return <code>true</code> if the given library has the given URI.
//   * 
//   * @param library the library being tested
//   * @param libraryUri the URI being tested for
//   * @return <code>true</code> if the given library has the given URI
//   */
//  private static boolean hasUri(DartLibrary library, URI libraryUri) {
//    return ((DartLibraryImpl) library).getLibrarySourceFile().getUri().equals(libraryUri);
//  }
}
