// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Assigns JavaScript identifiers to Dart variables, class-names and members.
 */
class Namer {
  final Compiler compiler;

  static final CLOSURE_INVOCATION_NAME = const SourceString('\$call');

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


  String closureInvocationName(int arity) {
    // TODO(floitsch): mangle, while not conflicting with instance names.
    return instanceMethodName(CLOSURE_INVOCATION_NAME, arity);
  }

  String instanceMethodName(SourceString name, int arity) {
    // TODO(floitsch): mangle, while preserving uniqueness.
    return '$name\$$arity';
  }

  String instanceFieldName(SourceString name) {
    return '$name';
  }

  String setterName(SourceString name) {
    return 'set\$$name';
  }

  String getterName(SourceString name) {
    return 'get\$$name';
  }

  /**
   * Returns a preferred JS-id for the given top-level or static element.
   * The returned id is guaranteed to be a valid JS-id.
   */
  String _computeGuess(Element element) {
    assert(!element.isInstanceMember());
    if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR) {
      SourceString name = getConstructorName(element);
      FunctionElement functionElement = element;
      return instanceMethodName(name, functionElement.parameterCount(compiler));
    } else {
      // TODO(floitsch): deal with named constructors.
      String name = '${element.name}';
      if (element.kind == ElementKind.FUNCTION) {
        FunctionElement functionElement = element;
        name = '$name\$${functionElement.parameterCount(compiler)}';
      }
      // Prefix the name with '$' if it is reserved.
      if (jsReserved.contains(name)) {
        name = "\$$name";
        assert(!jsReserved.contains(name));
      }
      return name;
    }
  }

  String getBailoutName(Element element) {
    return '${getName(element)}\$bailout';
  }

  SourceString getConstructorName(FunctionElement constructor) {
    String dartName = constructor.name.stringValue;
    return new SourceString(dartName.replaceFirst('\.', '\$'));
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
      SourceString name;
      if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR_BODY) {
        ConstructorBodyElement bodyElement = element;
        SourceString name = getConstructorName(bodyElement.constructor);
        return instanceMethodName(name, bodyElement.parameterCount(compiler));
      } else if (element.kind == ElementKind.FUNCTION) {
        FunctionElement functionElement = element;
        int parameterCount = functionElement.parameterCount(compiler);
        return instanceMethodName(element.name, parameterCount);
      } else if (element.kind == ElementKind.GETTER) {
        return getterName(element.name);
      } else if (element.kind == ElementKind.SETTER) {
        return setterName(element.name);
      } else {
        return instanceFieldName(element.name);
      }
    } else {
      // Dealing with a top-level or static element.
      String cached = globals[element];
      if (cached !== null) return cached;

      String guess = _computeGuess(element);
      switch (element.kind) {
        case ElementKind.VARIABLE:
        case ElementKind.PARAMETER:
          // The name is not guaranteed to be unique.
          return guess;

        case ElementKind.GENERATIVE_CONSTRUCTOR:
        case ElementKind.FUNCTION:
        case ElementKind.CLASS:
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
          compiler.internalError('getName for unknown kind: ${element.kind}',
                                 node: element.parseNode(compiler, compiler));
      }
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

  String operatorIs(ClassElement element) {
    return 'is\$${getName(element)}';
  }
}
