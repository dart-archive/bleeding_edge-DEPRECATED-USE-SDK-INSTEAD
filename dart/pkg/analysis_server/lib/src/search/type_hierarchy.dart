// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library analysis.type_hierarhy;

import 'dart:async';
import 'dart:collection';

import 'package:analysis_server/src/protocol_server.dart'
    show TypeHierarchyItem, newElement_fromEngine;
import 'package:analysis_server/src/services/search/hierarchy.dart';
import 'package:analysis_server/src/services/search/search_engine.dart';
import 'package:analyzer/src/generated/element.dart';

/**
 * A computer for a type hierarchy of an [Element].
 */
class TypeHierarchyComputer {
  final SearchEngine _searchEngine;

  LibraryElement _pivotLibrary;
  ElementKind _pivotKind;
  bool _pivotFieldFinal;
  String _pivotName;

  final List<TypeHierarchyItem> _items = <TypeHierarchyItem>[];
  final List<ClassElement> _itemClassElements = <ClassElement>[];
  final Map<Element, TypeHierarchyItem> _elementItemMap =
      new HashMap<Element, TypeHierarchyItem>();

  TypeHierarchyComputer(this._searchEngine);

  /**
   * Returns the computed type hierarchy, maybe `null`.
   */
  Future<List<TypeHierarchyItem>> compute(Element element) {
    _pivotLibrary = element.library;
    _pivotKind = element.kind;
    _pivotName = element.name;
    if (element is FieldElement) {
      _pivotFieldFinal = (element as FieldElement).isFinal;
      element = element.enclosingElement;
    }
    if (element is ExecutableElement &&
        element.enclosingElement is ClassElement) {
      element = element.enclosingElement;
    }
    if (element is ClassElement) {
      InterfaceType type = element.type;
      _createSuperItem(type);
      return _createSubclasses(_items[0], 0, type).then((_) {
        return new Future.value(_items);
      });
    }
    return new Future.value(null);
  }

  Future _createSubclasses(
      TypeHierarchyItem item, int itemId, InterfaceType type) {
    var future = getDirectSubClasses(_searchEngine, type.element);
    return future.then((Set<ClassElement> subElements) {
      List<int> subItemIds = <int>[];
      for (ClassElement subElement in subElements) {
        // check for recursion
        TypeHierarchyItem subItem = _elementItemMap[subElement];
        if (subItem != null) {
          int id = _items.indexOf(subItem);
          subItem.subclasses.add(id);
          continue;
        }
        // create a subclass item
        ExecutableElement subMemberElement = _findMemberElement(subElement);
        subItem = new TypeHierarchyItem(newElement_fromEngine(subElement),
            memberElement: subMemberElement != null
                ? newElement_fromEngine(subMemberElement)
                : null,
            superclass: itemId);
        int subItemId = _items.length;
        // remember
        _elementItemMap[subElement] = subItem;
        _items.add(subItem);
        _itemClassElements.add(subElement);
        // add to hierarchy
        item.subclasses.add(subItemId);
        subItemIds.add(subItemId);
      }
      // compute subclasses of subclasses
      return Future.forEach(subItemIds, (int subItemId) {
        TypeHierarchyItem subItem = _items[subItemId];
        ClassElement subItemElement = _itemClassElements[subItemId];
        InterfaceType subType = subItemElement.type;
        return _createSubclasses(subItem, subItemId, subType);
      });
    });
  }

  int _createSuperItem(InterfaceType type) {
    // check for recursion
    TypeHierarchyItem item = _elementItemMap[type.element];
    if (item != null) {
      return _items.indexOf(item);
    }
    // create an empty item now
    int itemId;
    {
      String displayName = null;
      if (type.typeArguments.isNotEmpty) {
        displayName = type.toString();
      }
      ClassElement classElement = type.element;
      ExecutableElement memberElement = _findMemberElement(classElement);
      item = new TypeHierarchyItem(newElement_fromEngine(classElement),
          displayName: displayName,
          memberElement: memberElement != null
              ? newElement_fromEngine(memberElement)
              : null);
      _elementItemMap[classElement] = item;
      itemId = _items.length;
      _items.add(item);
      _itemClassElements.add(classElement);
    }
    // superclass
    {
      InterfaceType superType = type.superclass;
      if (superType != null) {
        item.superclass = _createSuperItem(superType);
      }
    }
    // mixins
    type.mixins.forEach((InterfaceType type) {
      int id = _createSuperItem(type);
      item.mixins.add(id);
    });
    // interfaces
    type.interfaces.forEach((InterfaceType type) {
      int id = _createSuperItem(type);
      item.interfaces.add(id);
    });
    // done
    return itemId;
  }

  ExecutableElement _findMemberElement(ClassElement clazz) {
    ExecutableElement result;
    // try to find in the class itself
    if (_pivotKind == ElementKind.METHOD) {
      result = clazz.getMethod(_pivotName);
    } else if (_pivotKind == ElementKind.GETTER) {
      result = clazz.getGetter(_pivotName);
    } else if (_pivotKind == ElementKind.SETTER) {
      result = clazz.getSetter(_pivotName);
    } else if (_pivotKind == ElementKind.FIELD) {
      result = clazz.getGetter(_pivotName);
      if (result == null && !_pivotFieldFinal) {
        result = clazz.getSetter(_pivotName);
      }
    }
    if (result != null) {
      return result;
    }
    // try to find in the class mixin
    for (InterfaceType mixin in clazz.mixins.reversed) {
      ClassElement mixinElement = mixin.element;
      if (_pivotKind == ElementKind.METHOD) {
        result = mixinElement.lookUpMethod(_pivotName, _pivotLibrary);
      } else if (_pivotKind == ElementKind.GETTER) {
        result = mixinElement.lookUpGetter(_pivotName, _pivotLibrary);
      } else if (_pivotKind == ElementKind.SETTER) {
        result = mixinElement.lookUpSetter(_pivotName, _pivotLibrary);
      } else if (_pivotKind == ElementKind.FIELD) {
        result = mixinElement.lookUpGetter(_pivotName, _pivotLibrary);
        if (result == null && !_pivotFieldFinal) {
          result = mixinElement.lookUpSetter(_pivotName, _pivotLibrary);
        }
      }
      if (result != null) {
        return result;
      }
    }
    // not found
    return null;
  }
}
