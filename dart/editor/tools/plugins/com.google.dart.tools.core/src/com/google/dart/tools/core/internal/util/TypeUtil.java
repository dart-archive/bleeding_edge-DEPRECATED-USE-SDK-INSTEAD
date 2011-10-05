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
package com.google.dart.tools.core.internal.util;

import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.compiler.type.InterfaceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiler type system utilities. Might eventually be moved to the compiler package.
 */
public class TypeUtil {

  /**
   * Return a list of all the direct super classes of the given type, including the given type, in
   * hierarchy order. For a class, the returned list contains the class and all its super classes.
   * For an interface, the returned list contains only the interface.
   * <p>
   * TODO Determine if Object should be included for interfaces
   * 
   * @param base the type for which super types is to be determined
   * @return the list of direct super classes (which includes the base type)
   */
  public static List<InterfaceType> allDirectSuperclasses(InterfaceType base) {
    InterfaceType type = base;
    List<InterfaceType> types = new ArrayList<InterfaceType>();
    while (type != null) {
      types.add(type);
      ClassElement elem = type.getElement();
      if (elem != null) {
        type = elem.getSupertype();
      } else {
        type = null;
      }
    }
    return types;
  }

  /**
   * Return a list of all the super interfaces of the given type.
   * 
   * @param base the type for which super interfaces is desired
   * @return the list of super interfaces of the base type, not including the base type
   */
  public static List<InterfaceType> allSuperinterfaces(InterfaceType base) {
    List<InterfaceType> classes = allDirectSuperclasses(base);
    List<InterfaceType> types = new ArrayList<InterfaceType>();
    for (InterfaceType cl : classes) {
      findSuperinterfaces(cl, types);
    }
    return types;
  }

  /**
   * Return a list of all super types of the given type. The list begins with the base type,
   * followed by its super classes in hierarchy order (if any), then all the interfaces in no
   * specific order.
   * 
   * @param base the type for which super types is desired
   * @return the list of super types, including the base type
   */
  public static List<InterfaceType> allSupertypes(InterfaceType base) {
    // TODO Improve efficiency by avoiding recomputation of direct superclasses
    List<InterfaceType> direct = allDirectSuperclasses(base);
    List<InterfaceType> interfaces = allSuperinterfaces(base);
    direct.addAll(interfaces);
    return direct;
  }

  private static void findSuperinterfaces(InterfaceType type, List<InterfaceType> types) {
    ClassElement elem = type.getElement();
    if (elem != null) {
      List<InterfaceType> intfs = elem.getInterfaces();
      for (InterfaceType intf : intfs) {
        if (!types.contains(intf)) {
          types.add(intf);
          findSuperinterfaces(intf, types);
        }
      }
    }
  }
}
