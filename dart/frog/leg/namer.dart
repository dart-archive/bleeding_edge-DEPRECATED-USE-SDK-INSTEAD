// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Assigns JavaScript identifiers to Dart variables, class-names and members.
 */
class Namer {
  final Compiler compiler;

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

  Namer(this.compiler)
      : globals = new Map<Element, String>(),
        usedGlobals = new Map<String, int>();

  final String CURRENT_ISOLATE = "\$";
  final String ISOLATE = "Isolate";


  String closureInvocationName() {
    // TODO(floitsch): mangle, while not conflicting with instance names.
    return '\$call';
  }

  String instanceName(SourceString instanceName) {
    String candidate = '$instanceName';
    // TODO(floitsch): mangle, while preserving uniqueness.
    return candidate;
  }

  String setterName(SourceString name) {
    return 'set\$$name';
  }

  String getterName(SourceString name) {
    return 'get\$$name';
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
  String _computeGuess(Element element) {
    if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR_BODY) {
      ConstructorBodyElement bodyElement = element;
      return constructorBodyName(bodyElement.constructor);
    }

    if (element.isInstanceMember()) return instanceName(element.name);

    // TODO(floitsch): deal with named constructors.
    String name = '${element.name}';
    // Prefix the name with '$' if it is reserved.
    if (jsReserved.contains(name)) {
      name = "\$$name";
      assert(!jsReserved.contains(name));
    }
    return name;
  }

  String getBailoutName(Element element) {
    return '${getName(element)}\$bailout';
  }

  /**
   * Returns a preferred JS-id for the given element. The returned id is
   * guaranteed to be a valid JS-id. Globals and static fields are furthermore
   * guaranteed to be unique.
   *
   * For accessing statics consider calling
   * [isolateAccess]/[isolateBailoutAccess] or [isolatePropertyAccess] instead.
   */
  String getName(Element element) {
    if (element.isInstanceMember()) {
      return instanceName(element.name);
    }

    String name = globals[element];
    if (name !== null) return name;

    String guess = _computeGuess(element);
    switch (element.kind) {
      case ElementKind.VARIABLE:
      case ElementKind.PARAMETER:
        // The name is not guaranteed to be unique.
        return guess;

      case ElementKind.FUNCTION:
      case ElementKind.CLASS:
      case ElementKind.GENERATIVE_CONSTRUCTOR:
      case ElementKind.FIELD:
        // We need to make sure the name is unique.
        int usedCount = usedGlobals[guess];
        if (usedCount === null) {
          // No element with this name has been used before.
          usedGlobals[guess] = 1;
          globals[element] = guess;
          return guess;
        } else {
          // Not the first time we see an element with this name. Append a
          // number to make it unique.
          String name;
          do {
            usedCount++;
            name = '$guess$usedCount';
          } while (usedGlobals[name] !== null);
          usedGlobals[guess] = usedCount;
          globals[element] = name;
          return name;
        }
      
      default:
        compiler.unreachable('getName for unknown kind: ${element.kind}',
                             node: element.parseNode(compiler, compiler));
    }
  }

  String isolateAccess(Element element) {
    return "$CURRENT_ISOLATE.${getName(element)}";
  }

  String isolatePropertyAccess(Element element) {
    return "$ISOLATE.prototype.${getName(element)}";
  }

  String isolateBailoutPropertyAccess(Element element) {
    return '${isolatePropertyAccess(element)}\$bailout';
  }

  String isolateBailoutAccess(Element element) {
    return '${isolateAccess(element)}\$bailout';
  }
}
