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
package com.google.dart.tools.core.internal.hierarchy;

import com.google.dart.tools.core.internal.model.DartElementImpl;
import com.google.dart.tools.core.internal.model.delta.SimpleDelta;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartElementDelta;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Instances of the class <code>ChangeCollector</code> collects changes (reported through
 * fine-grained deltas) that can affect a type hierarchy.
 */
public class ChangeCollector {
  /**
   * A table from Types to TypeDeltas (this was the original comment, but if the key type is Type,
   * there is a compilation error).
   */
  private HashMap<DartElement, SimpleDelta> changes = new HashMap<DartElement, SimpleDelta>();

  private TypeHierarchyImpl hierarchy;

  public ChangeCollector(TypeHierarchyImpl hierarchy) {
    this.hierarchy = hierarchy;
  }

  /*
   * Adds the given delta on a compilation unit to the list of changes.
   */
  public void addChange(CompilationUnit cu, DartElementDelta newDelta) throws DartModelException {
    int newKind = newDelta.getKind();
    switch (newKind) {
      case DartElementDelta.ADDED:
        ArrayList<Type> allTypes = new ArrayList<Type>();
        getAllTypesFromElement(cu, allTypes);
        for (int i = 0, length = allTypes.size(); i < length; i++) {
          Type type = allTypes.get(i);
          addTypeAddition(type, changes.get(type));
        }
        break;
      case DartElementDelta.REMOVED:
        allTypes = new ArrayList<Type>();
        getAllTypesFromHierarchy((DartElementImpl) cu, allTypes);
        for (int i = 0, length = allTypes.size(); i < length; i++) {
          Type type = allTypes.get(i);
          addTypeRemoval(type, changes.get(type));
        }
        break;
      case DartElementDelta.CHANGED:
        addAffectedChildren(newDelta);
        break;
    }
  }

  /*
   * Whether the hierarchy needs refresh according to the changes collected so far.
   */
  public boolean needsRefresh() {
    return changes.size() != 0;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    Iterator<Map.Entry<DartElement, SimpleDelta>> iterator = changes.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<DartElement, SimpleDelta> entry = iterator.next();
      buffer.append(((DartElementImpl) entry.getKey()).toDebugString());
      buffer.append(entry.getValue());
      if (iterator.hasNext()) {
        buffer.append('\n');
      }
    }
    return buffer.toString();
  }

//  private void addChange(IImportDeclaration importDecl, DartElementDelta newDelta) {
//    SimpleDelta existingDelta = (SimpleDelta)changes.get(importDecl);
//    int newKind = newDelta.getKind();
//    if (existingDelta != null) {
//      switch (newKind) {
//        case DartElementDelta.ADDED:
//          if (existingDelta.getKind() == DartElementDelta.REMOVED) {
//            // REMOVED then ADDED
//            changes.remove(importDecl);
//          }
//          break;
//        case DartElementDelta.REMOVED:
//          if (existingDelta.getKind() == DartElementDelta.ADDED) {
//            // ADDED then REMOVED
//            changes.remove(importDecl);
//          }
//          break;
//        // CHANGED cannot happen for import declaration
//      }
//    } else {
//      SimpleDelta delta = new SimpleDelta();
//      switch (newKind) {
//        case DartElementDelta.ADDED:
//          delta.added();
//          break;
//        case DartElementDelta.REMOVED:
//          delta.removed();
//          break;
//      }
//      changes.put(importDecl, delta);
//    }
//  }

  /*
   * Adds the children of the given delta to the list of changes.
   */
  private void addAffectedChildren(DartElementDelta delta) throws DartModelException {
    DartElementDelta[] children = delta.getAffectedChildren();
    for (int i = 0, length = children.length; i < length; i++) {
      DartElementDelta child = children[i];
      DartElement childElement = child.getElement();
      switch (childElement.getElementType()) {
        case DartElement.TYPE:
          addChange((Type) childElement, child);
          break;
//        case DartElement.INITIALIZER:
        case DartElement.FIELD:
        case DartElement.METHOD:
          addChange((TypeMember) childElement, child);
          break;
      }
    }
  }

  /*
   * Adds a change for the given type and the types it defines.
   */
  private void addChange(Type type, DartElementDelta newDelta) throws DartModelException {
    int newKind = newDelta.getKind();
    SimpleDelta existingDelta = changes.get(type);
    switch (newKind) {
      case DartElementDelta.ADDED:
        addTypeAddition(type, existingDelta);
        ArrayList<Type> allTypes = new ArrayList<Type>();
        getAllTypesFromElement(type, allTypes);
        for (int i = 0, length = allTypes.size(); i < length; i++) {
          Type innerType = allTypes.get(i);
          addTypeAddition(innerType, changes.get(innerType));
        }
        break;
      case DartElementDelta.REMOVED:
        addTypeRemoval(type, existingDelta);
        allTypes = new ArrayList<Type>();
        getAllTypesFromHierarchy((DartElementImpl) type, allTypes);
        for (int i = 0, length = allTypes.size(); i < length; i++) {
          Type innerType = allTypes.get(i);
          addTypeRemoval(innerType, changes.get(innerType));
        }
        break;
      case DartElementDelta.CHANGED:
        addTypeChange(type, newDelta.getFlags(), existingDelta);
        addAffectedChildren(newDelta);
        break;
    }
  }

  /*
   * Adds a change for the given member (a method, a field or an initializer) and the types it
   * defines.
   */
  private void addChange(TypeMember member, DartElementDelta newDelta) throws DartModelException {
    int newKind = newDelta.getKind();
    switch (newKind) {
      case DartElementDelta.ADDED:
        ArrayList<Type> allTypes = new ArrayList<Type>();
        getAllTypesFromElement(member, allTypes);
        for (int i = 0, length = allTypes.size(); i < length; i++) {
          Type innerType = allTypes.get(i);
          addTypeAddition(innerType, changes.get(innerType));
        }
        break;
      case DartElementDelta.REMOVED:
        allTypes = new ArrayList<Type>();
        getAllTypesFromHierarchy((DartElementImpl) member, allTypes);
        for (int i = 0, length = allTypes.size(); i < length; i++) {
          Type type = allTypes.get(i);
          addTypeRemoval(type, changes.get(type));
        }
        break;
      case DartElementDelta.CHANGED:
        addAffectedChildren(newDelta);
        break;
    }
  }

  private void addTypeAddition(Type type, SimpleDelta existingDelta) throws DartModelException {
    if (existingDelta != null) {
      switch (existingDelta.getKind()) {
        case DartElementDelta.REMOVED:
          // REMOVED then ADDED
          boolean hasChange = false;
          if (hasSuperTypeChange(type)) {
            existingDelta.superTypes();
            hasChange = true;
          }
//          if (hasVisibilityChange(type)) {
//            existingDelta.modifiers();
//            hasChange = true;
//          }
          if (!hasChange) {
            changes.remove(type);
          }
          break;
      // CHANGED then ADDED
      // or ADDED then ADDED: should not happen
      }
    } else {
      // check whether the type addition affects the hierarchy
      String typeName = type.getElementName();
      if (hierarchy.hasSupertype(typeName) || hierarchy.subtypesIncludeSupertypeOf(type)
          || hierarchy.missingTypes.contains(typeName)) {
        SimpleDelta delta = new SimpleDelta();
        delta.added();
        changes.put(type, delta);
      }
    }
  }

  private void addTypeChange(Type type, int newFlags, SimpleDelta existingDelta)
      throws DartModelException {
    if (existingDelta != null) {
      switch (existingDelta.getKind()) {
        case DartElementDelta.CHANGED:
          // CHANGED then CHANGED
          int existingFlags = existingDelta.getFlags();
          boolean hasChange = false;
          if ((existingFlags & DartElementDelta.F_SUPER_TYPES) != 0 && hasSuperTypeChange(type)) {
            existingDelta.superTypes();
            hasChange = true;
          }
          if ((existingFlags & DartElementDelta.F_MODIFIERS) != 0
          /* && hasVisibilityChange(type) */) {
            existingDelta.modifiers();
            hasChange = true;
          }
          if (!hasChange) {
            // super types and visibility are back to the ones in the existing hierarchy
            changes.remove(type);
          }
          break;
      // ADDED then CHANGED: leave it as ADDED
      // REMOVED then CHANGED: should not happen
      }
    } else {
      // check whether the type change affects the hierarchy
      SimpleDelta typeDelta = null;
      if ((newFlags & DartElementDelta.F_SUPER_TYPES) != 0
          && hierarchy.includesTypeOrSupertype(type)) {
        typeDelta = new SimpleDelta();
        typeDelta.superTypes();
      }
      if ((newFlags & DartElementDelta.F_MODIFIERS) != 0
          && (hierarchy.hasSupertype(type.getElementName()) || type.equals(hierarchy.focusType))) {
        if (typeDelta == null) {
          typeDelta = new SimpleDelta();
        }
        typeDelta.modifiers();
      }
      if (typeDelta != null) {
        changes.put(type, typeDelta);
      }
    }
  }

  private void addTypeRemoval(Type type, SimpleDelta existingDelta) {
    if (existingDelta != null) {
      switch (existingDelta.getKind()) {
        case DartElementDelta.ADDED:
          // ADDED then REMOVED
          changes.remove(type);
          break;
        case DartElementDelta.CHANGED:
          // CHANGED then REMOVED
          existingDelta.removed();
          break;
      // REMOVED then REMOVED: should not happen
      }
    } else {
      // check whether the type removal affects the hierarchy
      if (hierarchy.contains(type)) {
        SimpleDelta typeDelta = new SimpleDelta();
        typeDelta.removed();
        changes.put(type, typeDelta);
      }
    }
  }

  /*
   * Returns all types defined in the given element excluding the given element.
   */
  private void getAllTypesFromElement(DartElement element, ArrayList<Type> allTypes)
      throws DartModelException {
    switch (element.getElementType()) {
      case DartElement.COMPILATION_UNIT:
        Type[] types = ((CompilationUnit) element).getTypes();
        for (int i = 0, length = types.length; i < length; i++) {
          Type type = types[i];
          allTypes.add(type);
          getAllTypesFromElement(type, allTypes);
        }
        break;
//      case DartElement.TYPE:
//        types = ((Type)element).getTypes();
//        for (int i = 0, length = types.length; i < length; i++) {
//          Type type = types[i];
//          allTypes.add(type);
//          getAllTypesFromElement(type, allTypes);
//        }
//        break;
//      case DartElement.INITIALIZER:
//      case DartElement.FIELD:
//      case DartElement.METHOD:
//        DartElement[] children = ((IMember)element).getChildren();
//        for (int i = 0, length = children.length; i < length; i++) {
//          Type type = (Type)children[i];
//          allTypes.add(type);
//          getAllTypesFromElement(type, allTypes);
//        }
//        break;
    }
  }

//  private boolean hasVisibilityChange(Type type) throws DartModelException {
//    int existingFlags = hierarchy.getCachedFlags(type);
//    int newFlags = type.getFlags();
//    return existingFlags != newFlags;
//  }

  /*
   * Returns all types in the existing hierarchy that have the given element as a parent.
   */
  private void getAllTypesFromHierarchy(DartElementImpl element, ArrayList<Type> allTypes) {
    switch (element.getElementType()) {
      case DartElement.COMPILATION_UNIT:
        ArrayList<Type> types = hierarchy.files.get(element);
        if (types != null) {
          allTypes.addAll(types);
        }
        break;
//      case DartElement.TYPE:
//      case DartElement.FIELD:
//      case DartElement.METHOD:
//        types = hierarchy.files.get(((TypeMember)element).getCompilationUnit());
//        if (types != null) {
//          for (int i = 0, length = types.size(); i < length; i++) {
//            Type type = (Type)types.get(i);
//            if (element.isAncestorOf(type)) {
//              allTypes.add(type);
//            }
//          }
//        }
//        break;
    }
  }

  private boolean hasSuperTypeChange(Type type) throws DartModelException {
    // check super class
    Type superclass = hierarchy.getSuperclass(type);
    String existingSuperclassName = superclass == null ? null : superclass.getElementName();
    String newSuperclassName = type.getSuperclassName();
    if (existingSuperclassName != null && !existingSuperclassName.equals(newSuperclassName)) {
      return true;
    }

    // check super interfaces
    Type[] existingSuperInterfaces = hierarchy.getSuperInterfaces(type);
    String[] newSuperInterfaces = type.getSuperInterfaceNames();
    if (existingSuperInterfaces.length != newSuperInterfaces.length) {
      return true;
    }
    for (int i = 0, length = newSuperInterfaces.length; i < length; i++) {
      String superInterfaceName = newSuperInterfaces[i];
      if (!superInterfaceName.equals(newSuperInterfaces[i])) {
        return true;
      }
    }

    return false;
  }
}
