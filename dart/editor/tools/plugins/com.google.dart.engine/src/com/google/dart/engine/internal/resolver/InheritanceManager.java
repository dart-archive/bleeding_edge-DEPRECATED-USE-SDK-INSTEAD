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
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.type.InterfaceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    ExecutableElement executable = lookupMemberInSuperclassChainAndMixins(classElt, memberName);
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
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @return
   */
  private HashMap<String, ExecutableElement> computeClassChainLookupMap(ClassElement classElt) {
    if (classLookup.containsKey(classElt)) {
      return classLookup.get(classElt);
    }
    ClassElement superclassElt = null;
    InterfaceType supertype = classElt.getSupertype();
    if (supertype != null) {
      superclassElt = supertype.getElement();
    }
    HashMap<String, ExecutableElement> resultMap;
    if (superclassElt != null) {
      if (library.equals(superclassElt.getLibrary())) {
        // if the supertype is in the same library, copy down all the members from the superclass
        resultMap = new HashMap<String, ExecutableElement>(
            computeClassChainLookupMap(superclassElt));
      } else {
        // if the supertype is not the same library, copy down only the accessible members from the superclass
        HashMap<String, ExecutableElement> superClassMap = computeClassChainLookupMap(superclassElt);
        resultMap = new HashMap<String, ExecutableElement>();
        for (Map.Entry<String, ExecutableElement> entry : superClassMap.entrySet()) {
          if (entry.getValue().isAccessibleIn(library)) {
            resultMap.put(entry.getKey(), entry.getValue());
          }
        }
      }
      // put the members from the superclass and mixins in to the resultMap
      populateMapWithClassMembers(resultMap, superclassElt);

      InterfaceType[] mixins = supertype.getMixins();
      for (int i = mixins.length - 1; i >= 0; i--) {
        ClassElement mixinElement = mixins[i].getElement();
        if (mixinElement != null) {
          populateMapWithClassMembers(resultMap, mixinElement);
        }
      }
    } else {
      // classElt is Object
      resultMap = new HashMap<String, ExecutableElement>(0);
    }
    classLookup.put(classElt, resultMap);
    return resultMap;
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @return
   */
  private HashMap<String, ExecutableElement> computeInterfaceLookupMap(ClassElement classElt,
      HashSet<ClassElement> visitedInterfaces) {
    if (interfaceLookup.containsKey(classElt)) {
      return interfaceLookup.get(classElt);
    }
    InterfaceType[] interfaces = classElt.getInterfaces();
    if (interfaces.length == 0) {
      return populateMapWithClassMembers(new HashMap<String, ExecutableElement>(), classElt);
    }
    // Recursively collect the list of mappings from all of the interface types
    ArrayList<HashMap<String, ExecutableElement>> lookupMaps = new ArrayList<HashMap<String, ExecutableElement>>(
        interfaces.length);
    for (InterfaceType interfaceType : interfaces) {
      if (classElt.getType().equals(interfaceType)) {
        continue;
      }
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null && !visitedInterfaces.contains(interfaceElement)) {
        lookupMaps.add(computeInterfaceLookupMap(interfaceElement, visitedInterfaces));
        visitedInterfaces.add(interfaceElement);
      }
    }
    if (lookupMaps.size() == 0) {
      return populateMapWithClassMembers(new HashMap<String, ExecutableElement>(), classElt);
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
    // Loop through the entries in the union map, adding them to the finalMap appropriately.
    HashMap<String, ExecutableElement> finalMap = new HashMap<String, ExecutableElement>();
    for (Entry<String, HashSet<ExecutableElement>> entry : unionMap.entrySet()) {
      HashSet<ExecutableElement> set = entry.getValue();
      if (set.size() == 1) {
        finalMap.put(entry.getKey(), set.iterator().next());
      } else {
        boolean allMethods = true;
        boolean allGetters = true;
        for (ExecutableElement executableElement : set) {
          if (executableElement instanceof PropertyAccessorElement) {
            allMethods = false;
          } else {
            allGetters = false;
          }
        }
        if (allMethods || allGetters) {
          // Compute the element whose type is the subtype of all of the other enclosing types.
          ExecutableElement[] elements = set.toArray(new ExecutableElement[set.size()]);
          InterfaceType[] enclosingElementTypes = new InterfaceType[elements.length];
          for (int i = 0; i < elements.length; i++) {
            Element enclosingElement = elements[i].getEnclosingElement();
            if (enclosingElement instanceof ClassElement) {
              enclosingElementTypes[i] = ((ClassElement) enclosingElement).getType();
            } else {
              enclosingElementTypes[i] = null;
            }
          }
          for (int i = 0; i < enclosingElementTypes.length; i++) {
            InterfaceType subtype = enclosingElementTypes[i];
            if (subtype == null) {
              continue;
            }
            boolean subtypeOfAllTypes = true;
            for (int j = 0; j < enclosingElementTypes.length && subtypeOfAllTypes; j++) {
              if (i != j) {
                if (!subtype.isSubtypeOf(enclosingElementTypes[j])) {
                  subtypeOfAllTypes = false;
                  break;
                }
              }
            }
            if (subtypeOfAllTypes) {
              finalMap.put(entry.getKey(), elements[i]);
              break;
            }
          }
        } else {
          // TODO (jwren) Report error.
          //finalMap.put(entry.getKey(), null);
        }
      }
    }
    finalMap = populateMapWithClassMembers(finalMap, classElt);
    interfaceLookup.put(classElt, finalMap);
    return finalMap;
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
      if (memberName.equals(method.getName()) && method.isAccessibleIn(library)) {
        return method;
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (memberName.equals(accessor.getName()) && accessor.isAccessibleIn(library)) {
        return accessor;
      }
    }
    return null;
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @param memberName
   * @return
   */
  private ExecutableElement lookupMemberInMixins(ClassElement classElt, String memberName) {
    InterfaceType[] mixins = classElt.getMixins();
    for (int i = mixins.length - 1; i >= 0; i--) {
      ClassElement mixinElement = mixins[i].getElement();
      if (mixinElement != null) {
        ExecutableElement element = lookupMemberInClass(mixinElement, memberName);
        if (element != null) {
          return element;
        }
      }
    }
    return null;
  }

  /**
   * TODO (jwren) add missing javadoc
   * 
   * @param classElt
   * @param memberName
   * @return
   */
  private ExecutableElement lookupMemberInSuperclassChainAndMixins(ClassElement classElt,
      String memberName) {
    ExecutableElement executable = lookupMemberInMixins(classElt, memberName);
    if (executable != null) {
      return executable;
    }
    return computeClassChainLookupMap(classElt).get(memberName);
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
      if (method.isAccessibleIn(library)) {
        map.put(method.getName(), method);
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isAccessibleIn(library)) {
        map.put(accessor.getName(), accessor);
      }
    }
    return map;
  }
}
