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
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.type.InterfaceType;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Instances of this class manage the knowledge of what the set of subtypes are for a given type.
 */
public class SubtypeManager {
  /**
   * A map between {@link ClassElement}s and a set of {@link ClassElement}s that are subtypes of the
   * key.
   */
  private HashMap<ClassElement, HashSet<ClassElement>> subtypeMap = new HashMap<ClassElement, HashSet<ClassElement>>();

  /**
   * The set of all {@link LibraryElement}s that have been visited by the manager. This is used both
   * to prevent infinite loops in the recursive methods, and also as a marker for the scope of the
   * libraries visited by this manager.
   */
  private HashSet<LibraryElement> visitedLibraries = new HashSet<LibraryElement>();

  /**
   * Given some {@link ClassElement}, return the set of all subtypes, and subtypes of subtypes.
   * 
   * @param classElement the class to recursively return the set of subtypes of
   */
  public HashSet<ClassElement> computeAllSubtypes(ClassElement classElement) {
    // Ensure that we have generated the subtype map for the library
    computeSubtypesInLibrary(classElement.getLibrary());

    // use the subtypeMap to compute the set of all subtypes and subtype's subtypes
    HashSet<ClassElement> allSubtypes = new HashSet<ClassElement>();
    safelyComputeAllSubtypes(classElement, new HashSet<ClassElement>(), allSubtypes);
    return allSubtypes;
  }

  /**
   * Given some {@link LibraryElement}, visit all of the types in the library, the passed library,
   * and any imported libraries, will be in the {@link #visitedLibraries} set.
   * 
   * @param libraryElement the library to visit, it it hasn't been visited already
   */
  public void ensureLibraryVisited(LibraryElement libraryElement) {
    computeSubtypesInLibrary(libraryElement);
  }

  /**
   * Given some {@link ClassElement}, this method adds all of the pairs combinations of itself and
   * all of its supertypes to the {@link #subtypeMap} map.
   * 
   * @param classElement the class element
   */
  private void computeSubtypesInClass(ClassElement classElement) {
    InterfaceType supertypeType = classElement.getSupertype();
    if (supertypeType != null) {
      ClassElement supertypeElement = supertypeType.getElement();
      if (supertypeElement != null) {
        putInSubtypeMap(supertypeElement, classElement);
      }
    }
    InterfaceType[] interfaceTypes = classElement.getInterfaces();
    for (InterfaceType interfaceType : interfaceTypes) {
      ClassElement interfaceElement = interfaceType.getElement();
      if (interfaceElement != null) {
        putInSubtypeMap(interfaceElement, classElement);
      }
    }
    InterfaceType[] mixinTypes = classElement.getMixins();
    for (InterfaceType mixinType : mixinTypes) {
      ClassElement mixinElement = mixinType.getElement();
      if (mixinElement != null) {
        putInSubtypeMap(mixinElement, classElement);
      }
    }
  }

  /**
   * Given some {@link CompilationUnitElement}, this method calls
   * {@link #computeAllSubtypes(ClassElement)} on all of the {@link ClassElement}s in the
   * compilation unit.
   * 
   * @param unitElement the compilation unit element
   */
  private void computeSubtypesInCompilationUnit(CompilationUnitElement unitElement) {
    ClassElement[] classElements = unitElement.getTypes();
    for (ClassElement classElement : classElements) {
      computeSubtypesInClass(classElement);
    }
  }

  /**
   * Given some {@link LibraryElement}, this method calls
   * {@link #computeAllSubtypes(CompilationUnitElement)} on all of the {@link ClassElement}s in the
   * compilation unit, and itself for all imported and exported libraries. All visited libraries are
   * added to the {@link #visitedLibraries} set.
   * 
   * @param libraryElement the library element
   */

  private void computeSubtypesInLibrary(LibraryElement libraryElement) {
    if (libraryElement == null || visitedLibraries.contains(libraryElement)) {
      return;
    }
    visitedLibraries.add(libraryElement);
    computeSubtypesInCompilationUnit(libraryElement.getDefiningCompilationUnit());
    CompilationUnitElement[] parts = libraryElement.getParts();
    for (CompilationUnitElement part : parts) {
      computeSubtypesInCompilationUnit(part);
    }
    LibraryElement[] imports = libraryElement.getImportedLibraries();
    for (LibraryElement importElt : imports) {
      computeSubtypesInLibrary(importElt.getLibrary());
    }
    LibraryElement[] exports = libraryElement.getExportedLibraries();
    for (LibraryElement exportElt : exports) {
      computeSubtypesInLibrary(exportElt.getLibrary());
    }
  }

  /**
   * Add some key/ value pair into the {@link #subtypeMap} map.
   * 
   * @param supertypeElement the key for the {@link #subtypeMap} map
   * @param subtypeElement the value for the {@link #subtypeMap} map
   */
  private void putInSubtypeMap(ClassElement supertypeElement, ClassElement subtypeElement) {
    HashSet<ClassElement> subtypes = subtypeMap.get(supertypeElement);
    if (subtypes == null) {
      subtypes = new HashSet<ClassElement>();
      subtypeMap.put(supertypeElement, subtypes);
    }
    subtypes.add(subtypeElement);
  }

  /**
   * Given some {@link ClassElement} and a {@link HashSet<ClassElement>}, this method recursively
   * adds all of the subtypes of the {@link ClassElement} to the passed array.
   * 
   * @param classElement the type to compute the set of subtypes of
   * @param visitedClasses the set of class elements that this method has already recursively seen
   * @param allSubtypes the computed set of subtypes of the passed class element
   */
  private void safelyComputeAllSubtypes(ClassElement classElement,
      HashSet<ClassElement> visitedClasses, HashSet<ClassElement> allSubtypes) {
    if (!visitedClasses.add(classElement)) {
      // if this class has already been called on this class element
      return;
    }
    HashSet<ClassElement> subtypes = subtypeMap.get(classElement);
    if (subtypes == null) {
      return;
    }
    for (ClassElement subtype : subtypes) {
      safelyComputeAllSubtypes(subtype, visitedClasses, allSubtypes);
    }
    allSubtypes.addAll(subtypes);
  }

}
