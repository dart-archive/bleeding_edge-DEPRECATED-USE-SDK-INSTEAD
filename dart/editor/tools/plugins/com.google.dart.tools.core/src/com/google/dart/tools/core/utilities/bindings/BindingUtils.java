/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.common.Symbol;
import com.google.dart.compiler.resolver.ClassElement;
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
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.utilities.net.URIUtilities;

import org.eclipse.core.resources.IResource;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
    private HashMap<String, List<com.google.dart.tools.core.model.DartFunction>> functionMap = new HashMap<String, List<com.google.dart.tools.core.model.DartFunction>>();

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

  /**
   * A table mapping the handle identifiers of libraries to a cache entry holding a type map for
   * that library.
   */
  private static final HashMap<String, CacheEntry> libraryCache = new HashMap<String, CacheEntry>();

  public static DartFunctionTypeAlias getDartElement(CompilationUnit unit,
      com.google.dart.compiler.ast.DartFunctionTypeAlias node) {
    if (node == null) {
      return null;
    }
    String typeName = node.getName().getTargetName();
    try {
      for (com.google.dart.tools.core.model.DartFunctionTypeAlias alias : unit.getFunctionTypeAliases()) {
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
    Symbol symbol = node.getSymbol();
    if (symbol == null) {
      return null;
    }
    String fieldName = symbol.getOriginalSymbolName();
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

  public static com.google.dart.tools.core.model.DartFunction getDartElement(CompilationUnit unit,
      DartFunction node) {
    if (node == null) {
      return null;
    }
    DartCore.notYetImplemented();
    return null;
  }

  public static Method getDartElement(CompilationUnit unit, DartMethodDefinition node) {
    if (node == null) {
      return null;
    }
    Symbol symbol = node.getSymbol();
    if (symbol == null) {
      return null;
    }
    DartClass enclosingType = getEnclosingType(node);
    if (enclosingType == null) {
      return null;
    }
    Type definingType = getDartElement(unit, enclosingType);
    if (definingType == null) {
      return null;
    }
    String methodName = symbol.getOriginalSymbolName();
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
      return getDartElement(library, ((MethodElement) element));
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
    if (fieldBinding == null) {
      return null;
    }
    EnclosingElement parent = fieldBinding.getEnclosingElement();
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
            } else if (child instanceof com.google.dart.tools.core.model.DartFunction) {
              com.google.dart.tools.core.model.DartFunction function = (com.google.dart.tools.core.model.DartFunction) child;
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
    Type declaringType = getDartElement(library, (InterfaceType) enclosingType);
    if (declaringType == null) {
      return null;
    }
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
    try {
      LibraryElement declaringLibraryElement = element.getLibrary();
      if (declaringLibraryElement == null) {
        DartCore.logError("Could not access declaring library for type " + typeName,
            new Throwable());
        return null;
      }
      DartLibrary declaringLibrary = getDartElement(library, declaringLibraryElement);
      List<DartFunctionTypeAlias> matchingTypes = getFunctionTypeAliases(declaringLibrary, typeName);
      if (matchingTypes.size() == 1) {
        return matchingTypes.get(0);
      }
    } catch (DartModelException exception) {
      DartCore.logError(exception);
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
  public static Type getDartElement(DartLibrary library, InterfaceType typeBinding) {
    if (typeBinding == null) {
      return null;
    } else if (library == null) {
      return getDartElement(typeBinding);
    }
    ClassElement element = typeBinding.getElement();
    if (element.isDynamic()) {
      return null;
    }
    String typeName = element.getName();
    try {
      LibraryElement declaringLibraryElement = element.getLibrary();
      if (declaringLibraryElement == null) {
        DartCore.logError("Could not access declaring library for type " + typeName,
            new Throwable());
        return null;
      }
      DartLibrary declaringLibrary = getDartElement(library, declaringLibraryElement);
//      long time1 = System.nanoTime();
      List<Type> matchingTypes = getImmediateTypes(declaringLibrary, typeName);
//      long time2 = System.nanoTime();
//      List<Type> matchingTypes2 = new ArrayList<Type>();
//      addImmediateTypesUncached(matchingTypes2, declaringLibrary, typeName);
//      long time3 = System.nanoTime();
//      long duration1 = time2 - time1;
//      long duration2 = time3 - time2;
//      if (duration1 <= duration2) {
//        System.out.println("C : " + (duration2 - duration1) + " [" + duration1 + ", " + duration2
//            + "] - " + library.getDisplayName() + " -> " + typeName + " in "
//            + declaringLibrary.getDisplayName());
//      } else {
//        System.out.println("U : " + (duration1 - duration2) + " [" + duration1 + ", " + duration2
//            + "] - " + library.getDisplayName() + " -> " + typeName + " in "
//            + declaringLibrary.getDisplayName());
//      }
      if (matchingTypes.size() == 1) {
        return matchingTypes.get(0);
      }
    } catch (DartModelException exception) {
      DartCore.logError(exception);
    }
    return null;
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
    URI libraryUri = URIUtilities.safelyResolveDartUri(((DartLibraryImpl) library).getLibrarySourceFile().getUri());
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
  public static com.google.dart.tools.core.model.DartFunction getDartElement(DartLibrary library,
      MethodElement methodBinding) {
    if (methodBinding == null) {
      return null;
    }
    EnclosingElement enclosingElement = methodBinding.getEnclosingElement();
    if (enclosingElement == null) {
      // We don't have enough information to find the method or function.
      return null;
    } else if (enclosingElement instanceof LibraryElement) {
      // This is a top-level function.
      DartLibrary definingLibrary = getDartElement(library, (LibraryElement) enclosingElement);
      if (definingLibrary == null) {
        definingLibrary = library;
      }
      List<com.google.dart.tools.core.model.DartFunction> matchingFunctions = getImmediateFunctions(
          definingLibrary, methodBinding.getName());
      if (matchingFunctions.size() == 1) {
        return matchingFunctions.get(0);
      }
      DartCore.notYetImplemented();
      return null;
    }
    com.google.dart.compiler.type.Type enclosingType = enclosingElement.getType();
    if (!(enclosingType instanceof InterfaceType)) {
      return null;
    }
    Type declaringType = getDartElement(library, (InterfaceType) enclosingType);
    if (declaringType == null) {
      return null;
    }
    String methodName = methodBinding.getName();
    if (methodName == null) {
      return null;
    } else if (methodName.length() == 0) {
      methodName = declaringType.getElementName();
    }
    try {
      for (Method method : declaringType.getMethods()) {
        if (methodName.equals(method.getElementName())) {
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
  public static Field getDartElement(DartLibrary library, TypeVariableElement variableBinding) {
    if (variableBinding == null) {
      return null;
    }
    DartCore.notYetImplemented();
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
    DartNode node = variableBinding.getNode();
    DartFunction functionNode = getEnclosingFunction(node);
    if (functionNode == null) {
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
    com.google.dart.tools.core.model.DartFunction functionElement = getDartElement(library,
        (MethodElement) functionNode.getParent().getSymbol());
    try {
      for (DartVariableDeclaration variable : functionElement.getLocalVariables()) {
        if (variable.getElementName().equals(variableName)) {
          return variable;
        }
      }
    } catch (DartModelException exception) {
      DartCore.logError("Cannot access local variables within function", exception);
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved type.
   * 
   * @param typeBinding the resolved type used to locate the model element
   * @return the Dart model element corresponding to the resolved type
   */
  public static Type getDartElement(InterfaceType typeBinding) {
    if (typeBinding == null) {
      return null;
    }
    String typeName = typeBinding.getElement().getName();
    try {
      List<Type> matchingTypes = new ArrayList<Type>();
      addTypes(matchingTypes, typeName);
      if (matchingTypes.size() == 1) {
        return matchingTypes.get(0);
      }
    } catch (DartModelException exception) {
      DartCore.logError(exception);
    }
    return null;
  }

  /**
   * Return the Dart model element corresponding to the given resolved method.
   * 
   * @param methodBinding the resolved method used to locate the model element
   * @return the Dart model element corresponding to the resolved method
   */
  public static com.google.dart.tools.core.model.DartFunction getDartElement(
      MethodElement methodBinding) {
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
   * Return the nearest function that is a parent of the given node.
   * 
   * @param node the node from which the search will begin
   * @return the nearest function that is a parent of the given node
   */
  public static DartFunction getEnclosingFunction(DartNode node) {
    if (node == null) {
      return null;
    }
    DartNode parent = node.getParent();
    while (parent != null) {
      if (parent instanceof DartFunction) {
        return (DartFunction) parent;
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
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static void addImmediateFunctionTypeAliasesUncached(
      List<DartFunctionTypeAlias> matchingAliases, DartLibrary library, String typeName)
      throws DartModelException {
    for (CompilationUnit unit : library.getCompilationUnits()) {
      for (DartFunctionTypeAlias type : unit.getFunctionTypeAliases()) {
        if (type.getElementName().equals(typeName)) {
          matchingAliases.add(type);
        }
      }
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
  private static void addImmediateTypes(List<Type> matchingTypes, DartLibrary library,
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
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static void addImmediateTypesUncached(List<Type> matchingTypes, DartLibrary library,
      String typeName) throws DartModelException {
    for (CompilationUnit unit : library.getCompilationUnits()) {
      for (Type type : unit.getTypes()) {
        if (type.getElementName().equals(typeName)) {
          matchingTypes.add(type);
        }
      }
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
  private static void addTypes(List<Type> matchingTypes, DartLibrary library, String typeName)
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
  private static void addTypes(List<Type> matchingTypes, String typeName) throws DartModelException {
    for (DartProject project : DartModelManager.getInstance().getDartModel().getDartProjects()) {
      for (DartElement child : project.getChildren()) {
        if (child instanceof DartLibrary) {
          addTypes(matchingTypes, (DartLibrary) child, typeName);
        }
      }
    }
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
          DartLibrary foundLibrary = findLibrary(importedLibrary, importedLibraryUri, targetUri,
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

  /**
   * Traverse the entire workspace looking for all of the function type aliases with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the type in which the method is declared
   * @param typeName the name of the types to be returned
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static List<DartFunctionTypeAlias> getFunctionTypeAliases(DartLibrary library,
      String typeName) throws DartModelException {
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
  private static List<com.google.dart.tools.core.model.DartFunction> getImmediateFunctions(
      DartLibrary library, String functionName) {
    if (library == null) {
      return new ArrayList<com.google.dart.tools.core.model.DartFunction>();
    }
    CacheEntry entry = getLibraryCache(library);
    if (entry == null) {
      return new ArrayList<com.google.dart.tools.core.model.DartFunction>();
    }
    HashMap<String, List<com.google.dart.tools.core.model.DartFunction>> functionMap = entry.functionMap;
    if (functionMap != null) {
      List<com.google.dart.tools.core.model.DartFunction> functionList = functionMap.get(functionName);
      if (functionList != null) {
        return functionList;
      }
    }
    List<com.google.dart.tools.core.model.DartFunction> matchingFunctions = new ArrayList<com.google.dart.tools.core.model.DartFunction>();
//    addImmediateFunctionsUncached(matchingFunctions, library, functionName);
    return matchingFunctions;
  }

  /**
   * Traverse the entire workspace looking for all of the types with the given name.
   * 
   * @param matchingTypes the list to which matching types are to be added
   * @param library the library containing the type in which the method is declared
   * @param typeName the name of the types to be returned
   * @throws DartModelException if some portion of the workspace cannot be traversed
   */
  private static List<Type> getImmediateTypes(DartLibrary library, String typeName)
      throws DartModelException {
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
              } else if (child instanceof com.google.dart.tools.core.model.DartFunction) {
                com.google.dart.tools.core.model.DartFunction function = (com.google.dart.tools.core.model.DartFunction) child;
                String functionName = function.getElementName();
                List<com.google.dart.tools.core.model.DartFunction> functionList = entry.functionMap.get(functionName);
                if (functionList == null) {
                  functionList = new ArrayList<com.google.dart.tools.core.model.DartFunction>();
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
