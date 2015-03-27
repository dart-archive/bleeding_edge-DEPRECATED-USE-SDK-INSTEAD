// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library computer.overrides;

import 'package:analysis_server/src/collections.dart';
import 'package:analysis_server/src/protocol_server.dart';
import 'package:analyzer/src/generated/ast.dart';
import 'package:analyzer/src/generated/element.dart' as engine;

/**
 * A computer for class member overrides in a Dart [CompilationUnit].
 */
class DartUnitOverridesComputer {
  final CompilationUnit _unit;

  final List<Override> _overrides = <Override>[];
  engine.ClassElement _currentClass;

  DartUnitOverridesComputer(this._unit);

  /**
   * Returns the computed occurrences, not `null`.
   */
  List<Override> compute() {
    for (CompilationUnitMember unitMember in _unit.declarations) {
      if (unitMember is ClassDeclaration) {
        _currentClass = unitMember.element;
        for (ClassMember classMember in unitMember.members) {
          if (classMember is MethodDeclaration) {
            SimpleIdentifier nameNode = classMember.name;
            _addOverride(nameNode.offset, nameNode.length, nameNode.name);
          }
          if (classMember is FieldDeclaration) {
            List<VariableDeclaration> fields = classMember.fields.variables;
            for (VariableDeclaration field in fields) {
              SimpleIdentifier nameNode = field.name;
              _addOverride(nameNode.offset, nameNode.length, nameNode.name);
            }
          }
        }
      }
    }
    return _overrides;
  }

  void _addInterfaceOverrides(List<engine.Element> elements, String name,
      engine.InterfaceType type, bool checkType,
      Set<engine.InterfaceType> visited) {
    if (type == null) {
      return;
    }
    if (!visited.add(type)) {
      return;
    }
    // check type
    if (checkType) {
      engine.Element element = _lookupMember(type.element, name);
      if (element != null) {
        elements.add(element);
      }
    }
    // check interfaces
    for (engine.InterfaceType interfaceType in type.interfaces) {
      _addInterfaceOverrides(elements, name, interfaceType, true, visited);
    }
    // check super
    _addInterfaceOverrides(elements, name, type.superclass, checkType, visited);
  }

  void _addOverride(int offset, int length, String name) {
    // super
    engine.Element superEngineElement;
    {
      engine.InterfaceType superType = _currentClass.supertype;
      if (superType != null) {
        superEngineElement = _lookupMember(superType.element, name);
      }
    }
    // interfaces
    List<engine.Element> interfaceEngineElements = <engine.Element>[];
    _addInterfaceOverrides(interfaceEngineElements, name, _currentClass.type,
        false, new Set<engine.InterfaceType>());
    // is there any override?
    if (superEngineElement != null || interfaceEngineElements.isNotEmpty) {
      OverriddenMember superMember = superEngineElement != null
          ? newOverriddenMember_fromEngine(superEngineElement)
          : null;
      List<OverriddenMember> interfaceMembers = interfaceEngineElements
          .map((member) => newOverriddenMember_fromEngine(member))
          .toList();
      _overrides.add(new Override(offset, length,
          superclassMember: superMember,
          interfaceMembers: nullIfEmpty(interfaceMembers)));
    }
  }

  static engine.Element _lookupMember(
      engine.ClassElement classElement, String name) {
    if (classElement == null) {
      return null;
    }
    engine.LibraryElement library = classElement.library;
    // method
    engine.Element member = classElement.lookUpMethod(name, library);
    if (member != null) {
      return member;
    }
    // getter
    member = classElement.lookUpGetter(name, library);
    if (member != null) {
      return member;
    }
    // setter
    member = classElement.lookUpSetter(name + '=', library);
    if (member != null) {
      return member;
    }
    // not found
    return null;
  }
}
