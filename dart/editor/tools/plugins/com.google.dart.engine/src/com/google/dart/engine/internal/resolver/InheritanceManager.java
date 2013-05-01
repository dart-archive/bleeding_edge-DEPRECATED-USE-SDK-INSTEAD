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
            String key = method.getDisplayName();
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
            String key = accessor.getDisplayName();
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
      HashSet<ExecutableElement> set = entry.getValue();
      if (set.size() == 1) {
        resultMap.put(entry.getKey(), set.iterator().next());
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
              resultMap.put(entry.getKey(), elements[i]);
              break;
            }
          }
        } else {
          // TODO (jwren) Report error.
          //finalMap.put(entry.getKey(), null);
        }
      }
    }
    resultMap = populateMapWithClassMembers(resultMap, classElt);
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
      if (memberName.equals(method.getDisplayName()) && method.isAccessibleIn(library)
          && !method.isStatic()) {
        return method;
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (memberName.equals(accessor.getDisplayName()) && accessor.isAccessibleIn(library)
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
        map.put(method.getDisplayName(), method);
      }
    }
    PropertyAccessorElement[] accessors = classElt.getAccessors();
    for (PropertyAccessorElement accessor : accessors) {
      if (accessor.isAccessibleIn(library) && !accessor.isStatic()) {
        map.put(accessor.getDisplayName(), accessor);
      }
    }
    return map;
  }
}
