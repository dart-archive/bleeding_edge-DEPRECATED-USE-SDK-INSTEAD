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

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.type.TypeVariableTypeImpl;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * Instances of the class {@code InheritanceManager} manage the knowledge of where class members
 * (methods, getters & setters) are inherited from.
 * 
 * @coverage dart.engine.resolver
 */
public class InheritanceManager {

  /**
   * The {@link LibraryElement} that is managed by this manager.
   */
  private LibraryElement library;

  /**
   * This is a mapping between each {@link ClassElement} and a map between the {@link String} member
   * names and the associated {@link ExecutableElement} in the mixin and superclass chain.
   */
  private HashMap<ClassElement, HashMap<String, ExecutableElement>> classLookup;

  /**
   * This is a mapping between each {@link ClassElement} and a map between the {@link String} member
   * names and the associated {@link ExecutableElement} in the interface set.
   */
  private HashMap<ClassElement, HashMap<String, ExecutableElement>> interfaceLookup;

  /**
   * 
   */
  private HashMap<ClassElement, HashSet<AnalysisError>> errorsInClassElement = new HashMap<ClassElement, HashSet<AnalysisError>>();

  /**
   * Initialize a newly created inheritance manager.
   * 
   * @param library the library element context that the inheritance mappings are being generated
   */
  public InheritanceManager(LibraryElement library) {
    this.library = library;
    classLookup = new HashMap<ClassElement, HashMap<String, ExecutableElement>>();
    interfaceLookup = new HashMap<ClassElement, HashMap<String, ExecutableElement>>();
  }

  /**
   * TODO(jwren) missing javadoc
   * 
   * @param classElt
   * @return
   */
  public HashSet<AnalysisError> getErrors(ClassElement classElt) {
    return errorsInClassElement.get(classElt);
  }

  public HashMap<String, ExecutableElement> getMapOfMembersInheritedFromClasses(
      ClassElement classElt) {
    return computeClassChainLookupMap(classElt, new HashSet<ClassElement>());
  }

  public HashMap<String, ExecutableElement> getMapOfMembersInheritedFromInterfaces(
      ClassElement classElt) {
    return computeInterfaceLookupMap(classElt, new HashSet<ClassElement>());
  }

  /**
   * Given some {@link ClassElement class element} and some member name, this returns the
   * {@link ExecutableElement executable element} that the class inherits from the mixins,
   * superclasses or interfaces, that has the member name, if no member is inherited {@code null} is
   * returned.
   * 
   * @param classElt the class element to query
   * @param memberName the name of the executable element to find and return
   * @return the inherited executable element with the member name, or {@code null} if no such
   *         member exists
   */
  public ExecutableElement lookupInheritance(ClassElement classElt, String memberName) {
    if (memberName == null || memberName.isEmpty()) {
      return null;
    }
    ExecutableElement executable = computeClassChainLookupMap(classElt, new HashSet<ClassElement>()).get(
        memberName);
    if (executable == null) {
      return computeInterfaceLookupMap(classElt, new HashSet<ClassElement>()).get(memberName);
    }
    return executable;
  }

  /**
   * Given some {@link ClassElement class element} and some member name, this returns the
   * {@link ExecutableElement executable element} that the class either declares itself, or
   * inherits, that has the member name, if no member is inherited {@code null} is returned.
   * 
   * @param classElt the class element to query
   * @param memberName the name of the executable element to find and return
   * @return the inherited executable element with the member name, or {@code null} if no such
   *         member exists
   */
  public ExecutableElement lookupMember(ClassElement classElt, String memberName) {
    ExecutableElement element = lookupMemberInClass(classElt, memberName);
    if (element != null) {
      return element;
    }
    return lookupInheritance(classElt, memberName);
  }

  /**
   * Set the new library element context.
   * 
   * @param library the new library element
   */
  public void setLibraryElement(LibraryElement library) {
    this.library = library;
  }

  /**
   * This method takes some inherited {@link FunctionType}, and resolves all the parameterized types
   * in the function type, dependent on the class in which it is being overridden.
   * 
   * @param baseFunctionType the function type that is being overridden
   * @param memberName the name of the member, this is used to lookup the inheritance path of the
   *          override
   * @param definingType the type that is overriding the member
   * @return the passed function type with any parameterized types substituted
   */
  public FunctionType substituteTypeArgumentsInMemberFromInheritance(FunctionType baseFunctionType,
      String memberName, InterfaceType definingType) {
    if (baseFunctionType == null) {
      return baseFunctionType;
    }
    // TODO (jwren) add optimization: first check to see if the baseFunctionType has any
    // parameterized types, if it doesn't have any, return the baseFuntionType

    // First, generate the path from the defining type to the overridden member
    LinkedList<InterfaceType> inheritancePath = new LinkedList<InterfaceType>();
    computeInheritancePath(inheritancePath, definingType, memberName);

    if (inheritancePath == null || inheritancePath.size() < 2) {
      // TODO(jwren) log analysis engine error
      return baseFunctionType;
    }
    FunctionType functionTypeToReturn = baseFunctionType;
    // loop backward through the list substituting as we go:
    InterfaceType lastType = inheritancePath.removeLast();
    while (inheritancePath.size() > 0) {
      Type[] paramTypes = TypeVariableTypeImpl.getTypes(lastType.getElement().getTypeVariables());
      Type[] argTypes = lastType.getTypeArguments();
      functionTypeToReturn = functionTypeToReturn.substitute(argTypes, paramTypes);
      lastType = inheritancePath.removeLast();
    }
    return functionTypeToReturn;
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @return
   */
  private HashMap<String, ExecutableElement> computeClassChainLookupMap(ClassElement classElt,
      HashSet<ClassElement> visitedClasses) {
    HashMap<String, ExecutableElement> resultMap = classLookup.get(classElt);
    if (resultMap != null) {
      return resultMap;
    } else {
      resultMap = new HashMap<String, ExecutableElement>();
    }
    ClassElement superclassElt = null;
    InterfaceType supertype = classElt.getSupertype();
    if (supertype != null) {
      superclassElt = supertype.getElement();
    } else {
      // classElt is Object
      classLookup.put(classElt, resultMap);
      return resultMap;
    }
    if (superclassElt != null) {
      if (!visitedClasses.contains(superclassElt)) {
        visitedClasses.add(classElt);
        resultMap = new HashMap<String, ExecutableElement>(computeClassChainLookupMap(
            superclassElt,
            visitedClasses));
      } else {
        // This case happens only when the superclass was previously visited and not in the lookup,
        // meaning this is meant to shorten the compute for recursive cases.
        classLookup.put(superclassElt, resultMap);
        return resultMap;
      }
      // put the members from the superclass
      populateMapWithClassMembers(resultMap, superclassElt);
    }

    InterfaceType[] mixins = classElt.getMixins();
    for (int i = mixins.length - 1; i >= 0; i--) {
      ClassElement mixinElement = mixins[i].getElement();
      if (mixinElement != null) {
        populateMapWithClassMembers(resultMap, mixinElement);
      }
    }

    classLookup.put(classElt, resultMap);
    return resultMap;
  }

  /**
   * Compute and return the inheritance path given the context of a type and a member that is
   * overridden in the inheritance path (for which the type is in the path).
   * 
   * @param chain the inheritance path that is built up as this method calls itself recursively,
   *          when this method is called an empty {@link LinkedList} should be provided
   * @param currentType the current type in the inheritance path
   * @param memberName the name of the member that is being looked up the inheritance path
   */
  private void computeInheritancePath(LinkedList<InterfaceType> chain, InterfaceType currentType,
      String memberName) {
    // TODO (jwren) create a public version of this method which doesn't require the initial chain
    // to be provided, then provided tests for this functionality in InheritanceManagerTest
    chain.add(currentType);
    ClassElement classElt = currentType.getElement();
    InterfaceType supertype = classElt.getSupertype();
    // Base case- reached Object
    if (supertype == null) {
      // Looked up the chain all the way to Object, return null.
      // This should never happen.
      return;
    }

    // If we are done, return the chain
    // We are not done if this is the first recursive call on this method.
    if (chain.size() != 1) {
      // We are done however if the member is in this classElt
      if (lookupMemberInClass(classElt, memberName) != null) {
        return;
      }
    }

    // Otherwise, determine the next type (up the inheritance graph) to search for our member, start
    // with the mixins, followed by the superclass, and finally the interfaces:

    // Mixins- note that mixins call lookupMemberInClass, not lookupMember
    InterfaceType[] mixins = classElt.getMixins();
    for (int i = mixins.length - 1; i >= 0; i--) {
      ClassElement mixinElement = mixins[i].getElement();
      if (mixinElement != null) {
        ExecutableElement elt = lookupMemberInClass(mixinElement, memberName);
        if (elt != null) {
          // this is equivalent (but faster than) calling this method recursively
          // (return computeInheritancePath(chain, mixins[i], memberName);)
          chain.add(mixins[i]);
          return;
        }
      }
    }

    // Superclass
    ClassElement superclassElt = supertype.getElement();
    if (lookupMember(superclassElt, memberName) != null) {
      computeInheritancePath(chain, supertype, memberName);
      return;
    }

    // Interfaces
    InterfaceType[] interfaces = classElt.getInterfaces();
    for (InterfaceType interfaceType : interfaces) {
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null && lookupMember(interfaceElement, memberName) != null) {
        computeInheritancePath(chain, interfaceType, memberName);
        return;
      }
    }
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @return
   */
  private HashMap<String, ExecutableElement> computeInterfaceLookupMap(ClassElement classElt,
      HashSet<ClassElement> visitedInterfaces) {
    HashMap<String, ExecutableElement> resultMap = interfaceLookup.get(classElt);
    if (resultMap != null) {
      return resultMap;
    } else {
      resultMap = new HashMap<String, ExecutableElement>();
    }
    InterfaceType[] interfaces = classElt.getInterfaces();
    if (interfaces.length == 0) {
      interfaceLookup.put(classElt, resultMap);
      return resultMap;
    }
    // Recursively collect the list of mappings from all of the interface types
    ArrayList<HashMap<String, ExecutableElement>> lookupMaps = new ArrayList<HashMap<String, ExecutableElement>>(
        interfaces.length);
    for (InterfaceType interfaceType : interfaces) {
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null) {
        if (!visitedInterfaces.contains(interfaceElement)) {
          visitedInterfaces.add(interfaceElement);
          lookupMaps.add(computeInterfaceLookupMap(interfaceElement, visitedInterfaces));
        } else {
          HashMap<String, ExecutableElement> map = interfaceLookup.get(classElt);
          if (map != null) {
            lookupMaps.add(map);
          } else {
            interfaceLookup.put(interfaceElement, resultMap);
            return resultMap;
          }
        }
      }
    }
    if (lookupMaps.size() == 0) {
      interfaceLookup.put(classElt, resultMap);
      return resultMap;
    }
    // Union all of the maps together, grouping the ExecutableElements into sets.
    HashMap<String, HashSet<ExecutableElement>> unionMap = new HashMap<String, HashSet<ExecutableElement>>();
    for (HashMap<String, ExecutableElement> lookupMap : lookupMaps) {
      for (Entry<String, ExecutableElement> entry : lookupMap.entrySet()) {
        String key = entry.getKey();
        if (!unionMap.containsKey(key)) {
          HashSet<ExecutableElement> set = new HashSet<ExecutableElement>(4);
          set.add(entry.getValue());
          unionMap.put(key, set);
        } else {
          unionMap.get(key).add(entry.getValue());
        }
      }
    }
    // Next loop through all of the members in the interfaces themselves, adding them to the unionMap
    for (InterfaceType interfaceType : interfaces) {
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null) {
        MethodElement[] methods = interfaceElement.getMethods();
        for (MethodElement method : methods) {
          if (method.isAccessibleIn(library) && !method.isStatic()) {
            String key = method.getName();
            if (!unionMap.containsKey(key)) {
              HashSet<ExecutableElement> set = new HashSet<ExecutableElement>(4);
              set.add(method);
              unionMap.put(key, set);
            } else {
              unionMap.get(key).add(method);
            }
          }
        }
        PropertyAccessorElement[] accessors = interfaceElement.getAccessors();
        for (PropertyAccessorElement accessor : accessors) {
          if (accessor.isAccessibleIn(library) && !accessor.isStatic()) {
            String key = accessor.getName();
            if (!unionMap.containsKey(key)) {
              HashSet<ExecutableElement> set = new HashSet<ExecutableElement>(4);
              set.add(accessor);
              unionMap.put(key, set);
            } else {
              unionMap.get(key).add(accessor);
            }
          }
        }
      }
    }
    // Loop through the entries in the union map, adding them to the resultMap appropriately.
    for (Entry<String, HashSet<ExecutableElement>> entry : unionMap.entrySet()) {
      String key = entry.getKey();
      HashSet<ExecutableElement> set = entry.getValue();
      int numOfEltsWithMatchingNames = set.size();
      if (numOfEltsWithMatchingNames == 1) {
        resultMap.put(key, set.iterator().next());
      } else {
        boolean allMethods = true;
        boolean allSetters = true;
        boolean allGetters = true;
        for (ExecutableElement executableElement : set) {
          if (executableElement instanceof PropertyAccessorElement) {
            allMethods = false;
            if (((PropertyAccessorElement) executableElement).isSetter()) {
              allGetters = false;
            } else {
              allSetters = false;
            }
          } else {
            allGetters = false;
            allSetters = false;
          }
        }
        if (allMethods || allGetters || allSetters) {
          // Compute the element whose type is the subtype of all of the other types.
          ExecutableElement[] elements = set.toArray(new ExecutableElement[numOfEltsWithMatchingNames]);
          FunctionType[] executableElementTypes = new FunctionType[numOfEltsWithMatchingNames];
          for (int i = 0; i < numOfEltsWithMatchingNames; i++) {
            executableElementTypes[i] = elements[i].getType();
          }
          boolean foundSubtypeOfAllTypes = true;
          for (int i = 0; i < numOfEltsWithMatchingNames; i++) {
            FunctionType subtype = executableElementTypes[i];
            if (subtype == null) {
              continue;
            }
            boolean subtypeOfAllTypes = true;
            for (int j = 0; j < numOfEltsWithMatchingNames && subtypeOfAllTypes; j++) {
              if (i != j) {
                if (!subtype.isSubtypeOf(executableElementTypes[j])) {
                  subtypeOfAllTypes = false;
                  foundSubtypeOfAllTypes = false;
                  break;
                }
              }
            }
            if (subtypeOfAllTypes) {
              resultMap.put(key, elements[i]);
              break;
            }
          }
          if (!foundSubtypeOfAllTypes) {
            reportError(
                classElt,
                StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE,
                classElt.getNameOffset(),
                classElt.getDisplayName().length(),
                key);
          }
        } else {
          if (!allMethods && !allGetters) {
            reportError(
                classElt,
                StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD,
                classElt.getNameOffset(),
                classElt.getDisplayName().length(),
                key);
          }
          resultMap.remove(entry.getKey());
        }
      }
    }
    interfaceLookup.put(classElt, resultMap);
    return resultMap;
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @param memberName
   * @return
   */
  private ExecutableElement lookupMemberInClass(ClassElement classElt, String memberName) {
    MethodElement[] methods = classElt.getMethods();
    for (MethodElement method : methods) {
      if (memberName.equals(method.getName()) && method.isAccessibleIn(library)
          && !method.isStatic()) {
        return method;
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (memberName.equals(accessor.getName()) && accessor.isAccessibleIn(library)
          && !accessor.isStatic()) {
        return accessor;
      }
    }
    return null;
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param map
   * @param classElt
   * @return
   */
  private HashMap<String, ExecutableElement> populateMapWithClassMembers(
      HashMap<String, ExecutableElement> map, ClassElement classElt) {
    MethodElement[] methods = classElt.getMethods();
    for (MethodElement method : methods) {
      if (method.isAccessibleIn(library) && !method.isStatic()) {
        map.put(method.getName(), method);
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isAccessibleIn(library) && !accessor.isStatic()) {
        map.put(accessor.getName(), accessor);
      }
    }
    return map;
  }

  private void reportError(ClassElement classElt, ErrorCode errorCode, int nameOffset, int length,
      Object... arguments) {
    HashSet<AnalysisError> errorSet = errorsInClassElement.get(classElt);
    if (errorSet == null) {
      errorSet = new HashSet<AnalysisError>();
      errorsInClassElement.put(classElt, errorSet);
    }
    errorSet.add(new AnalysisError(classElt.getSource(), errorCode, arguments));
  }
}
