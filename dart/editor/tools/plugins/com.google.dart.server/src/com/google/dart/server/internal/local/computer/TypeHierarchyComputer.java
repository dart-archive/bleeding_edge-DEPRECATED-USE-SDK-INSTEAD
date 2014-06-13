/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.visitor.GeneralizingElementVisitor;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.utilities.translation.DartOmit;
import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;
import com.google.dart.server.TypeHierarchyItem;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A computer for {@link TypeHierarchyItem}s for an {@link Element}.
 * 
 * @coverage dart.server.local
 */
@DartOmit
public class TypeHierarchyComputer {
  private final SearchEngine searchEngine;
  private final String contextId;
  private final CompilationUnitElement unitElement;
  private final Element element;

  public TypeHierarchyComputer(SearchEngine searchEngine, String contextId,
      CompilationUnitElement unitElement, Element element) {
    this.searchEngine = searchEngine;
    this.contextId = contextId;
    this.unitElement = unitElement;
    this.element = element;
  }

  /**
   * Returns the computed {@link TypeHierarchyItem}.
   */
  public TypeHierarchyItem compute() {
    com.google.dart.engine.element.Element engineElement = findEngineElement(element.getOffset());
    if (engineElement instanceof ExecutableElement
        && engineElement.getEnclosingElement() instanceof ClassElement) {
      engineElement = engineElement.getEnclosingElement();
    }
    if (engineElement instanceof ClassElement) {
      Set<ClassElement> processed = Sets.newHashSet();
      InterfaceType type = ((ClassElement) engineElement).getType();
      TypeHierarchyItemImpl item = createSuperItem(type, processed);
      createSubItems(item, type, processed);
      return item;
    }
    return null;
  }

  private void createSubItems(TypeHierarchyItemImpl item, InterfaceType type,
      Set<ClassElement> processed) {
    List<TypeHierarchyItem> subItems = Lists.newArrayList();
    List<SearchMatch> matches = searchEngine.searchSubtypes(type.getElement(), null, null);
    for (SearchMatch match : matches) {
      com.google.dart.engine.element.Element engineElement = match.getElement();
      if (engineElement instanceof ClassElement) {
        ClassElement engineClassElement = (ClassElement) engineElement;
        // check for recursion
        if (!processed.add(engineClassElement)) {
          continue;
        }
        // create a subtype item
        ExecutableElement engineMemberElement = findMemberElement(engineClassElement);
        // Element API has changed
//        Element classElement = ElementImpl.create(contextId, engineClassElement);
//        ElementImpl memberElement = ElementImpl.create(contextId, engineMemberElement);
//        TypeHierarchyItemImpl subItem = new TypeHierarchyItemImpl(
//            engineElement.getDisplayName(),
//            classElement,
//            memberElement,
//            null,
//            null,
//            null);
//        subItems.add(subItem);
        // fill its subtypes
//        createSubItems(subItem, engineClassElement.getType(), processed);
      }
    }
    // sort by name
    Collections.sort(subItems, new Comparator<TypeHierarchyItem>() {
      @Override
      public int compare(TypeHierarchyItem o1, TypeHierarchyItem o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    // set as array
    item.setSubTypes(subItems.toArray(new TypeHierarchyItem[subItems.size()]));
  }

  private TypeHierarchyItemImpl createSuperItem(InterfaceType type, Set<ClassElement> processed) {
    // check for recursion
    if (!processed.add(type.getElement())) {
      return null;
    }
    // extends
    TypeHierarchyItem extendedItem = null;
    {
      InterfaceType superType = type.getSuperclass();
      if (superType != null) {
        extendedItem = createSuperItem(superType, processed);
      }
    }
    // mixed
    TypeHierarchyItem[] mixedItems = {};
    {
      InterfaceType[] mixedTypes = type.getMixins();
      mixedItems = new TypeHierarchyItem[mixedTypes.length];
      for (int i = 0; i < mixedTypes.length; i++) {
        InterfaceType mixedType = mixedTypes[i];
        mixedItems[i] = createSuperItem(mixedType, processed);
      }
    }
    // implemented
    TypeHierarchyItem[] implementedItems;
    {
      InterfaceType[] implementedTypes = type.getInterfaces();
      implementedItems = new TypeHierarchyItem[implementedTypes.length];
      for (int i = 0; i < implementedTypes.length; i++) {
        InterfaceType mixedType = implementedTypes[i];
        implementedItems[i] = createSuperItem(mixedType, processed);
      }
    }
    // done
    String name = type.toString();
    ClassElement engineClassElement = type.getElement();
    ExecutableElement engineMemberElement = findMemberElement(engineClassElement);
    // Element API has changed
//    ElementImpl classElement = ElementImpl.create(contextId, engineClassElement);
//    ElementImpl memberElement = ElementImpl.create(contextId, engineMemberElement);
//    return new TypeHierarchyItemImpl(
//        name,
//        classElement,
//        memberElement,
//        extendedItem,
//        mixedItems,
//        implementedItems);
    return null;
  }

  private com.google.dart.engine.element.Element findEngineElement(final int nameOffset) {
    final com.google.dart.engine.element.Element[] result = {null};
    unitElement.accept(new GeneralizingElementVisitor<Void>() {
      @Override
      public Void visitElement(com.google.dart.engine.element.Element element) {
        if (element.getNameOffset() == nameOffset) {
          result[0] = element;
        }
        return super.visitElement(element);
      }
    });
    return result[0];
  }

  private ExecutableElement findMemberElement(ClassElement engineClassElement) {
    String name = element.getName();
    if (element.getKind() == ElementKind.METHOD) {
      return engineClassElement.getMethod(name);
    }
    if (element.getKind() == ElementKind.GETTER) {
      return engineClassElement.getGetter(name);
    }
    if (element.getKind() == ElementKind.SETTER) {
      return engineClassElement.getSetter(name);
    }
    return null;
  }
}
