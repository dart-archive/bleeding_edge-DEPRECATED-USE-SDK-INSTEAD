// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Assigns JavaScript identifiers to Dart variables, class-names and members.
 */
class Namer {
  static Set<String> _jsReserved = null;
  Set<String> get jsReserved() {
    if (_jsReserved === null) {
      _jsReserved = new Set<String>();
      _jsReserved.addAll(JsNames.javaScriptKeywords);
      _jsReserved.addAll(JsNames.reservedPropertySymbols);
    }
    return _jsReserved;
  }

  Map<Element, String> globals;
  Map<String, int> usedGlobals;

  Namer()
      : globals = new Map<Element, String>(),
        usedGlobals = new Map<String, int>();

  String get currentIsolate() => "\$";
  String get isolate() => "Isolate";


  String instanceName(SourceString instanceName) {
    String candidate = '$instanceName';
    // TODO(floitsch): mangle, while preserving uniqueness.
    return candidate;
  }

  /**
   * The constructor-body name is computed from the corresponding
   * constructor element because, in the case of a super-initialization, the
   * body element is not accessible.
   */
  String constructorBodyName(Element element) {
    assert(element.kind == ElementKind.GENERATIVE_CONSTRUCTOR);
    // TODO(floitsch): the constructor-body name must not conflict with other
    // instance fields.
    // TOD(floitsch): deal with named constructors.
    return instanceName(element.name);
  }

  /**
   * Returns a preferred JS-id for the given element. The returned id is
   * guaranteed to be a valid JS-id.
   *
   * For instance-members the returned strings are guaranteed not to clash. For
   * static variables there might be clashes. In the latter case the caller
   * needs to ensure uniqueness.
   */
  String getName(Element element) {
    switch (element.kind) {
      case ElementKind.GENERATIVE_CONSTRUCTOR_BODY:
        ConstructorBodyElement bodyElement = element;
        return constructorBodyName(bodyElement.constructor);

      case ElementKind.GENERATIVE_CONSTRUCTOR:
      default:
        if (element.isInstanceMember()) {
          return instanceName(element.name);
        }
        // TODO(floitsch): deal with named constructors.
        String name = '${element.name}';
        // Prefix the name with '$' if it is reserved.
        if (jsReserved.contains(name)) {
          name = "\$$name";
          assert(!jsReserved.contains(name));
        }
        return name;
    }
  }

  String setterName(SourceString name) {
    return 'set\$$name';
  }

  String getterName(SourceString name) {
    return 'get\$$name';
  }

  /**
    * Don't use this method from the outside. Go through [isolateAccess] or
    * [isolatePropertyAccess] instead.
    */
  String define(Element element) {
    assert(globals[element] === null);

    String name = getName(element);
    int usedCount = usedGlobals[name];
    if (usedCount === null) {
      // No element with this name has been used before.
      usedGlobals[name] = 1;
      globals[element] = name;
      return name;
    } else {
      // Not the first time we see an element with this name. Append a number
      // to make it unique.
      String id;
      do {
        usedCount++;
        id = '$name$usedCount';
      } while (usedGlobals[id] !== null);
      usedGlobals[name] = usedCount;
      globals[element] = id;
      return id;
    }
  }

  String isolateAccess(Element element) {
    String jsId = globals[element];
    if (jsId === null) jsId = define(element);
    return "$currentIsolate.$jsId";
  }

  String isolatePropertyAccess(Element element) {
    String jsId = globals[element];
    if (jsId === null) jsId = define(element);
    return "$isolate.prototype.$jsId";
  }
}
