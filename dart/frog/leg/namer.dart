// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Assigns JavaScript identifiers to Dart variables, class-names and members.
 */
class Namer {
  final Compiler compiler;

  static final CLOSURE_INVOCATION_NAME = const SourceString('\$call');
  static final OPERATOR_EQUALS = const SourceString('operator\$eq');

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


  String closureInvocationName(Selector selector) {
    // TODO(floitsch): mangle, while not conflicting with instance names.
    return instanceMethodInvocationName(CLOSURE_INVOCATION_NAME, selector);
  }

  String instanceMethodName(SourceString name, int arity) {
    // TODO(floitsch): mangle, while preserving uniqueness.
    return '${name.slowToString()}\$$arity';
  }

  String instanceMethodInvocationName(SourceString name, Selector selector) {
    // TODO(floitsch): mangle, while preserving uniqueness.
    StringBuffer buffer = new StringBuffer();
    List<SourceString> names = selector.getOrderedNamedArguments();
    for (SourceString argumentName in names) {
      buffer.add(@'$');
      argumentName.printOn(buffer);
    }
    return '${name.slowToString()}\$${selector.argumentCount}$buffer';
  }

  String instanceFieldName(SourceString name) {
    return name.slowToString();
  }

  String setterName(SourceString name) {
    return 'set\$${name.slowToString()}';
  }

  String getterName(SourceString name) {
    return 'get\$${name.slowToString()}';
  }

  String getFreshGlobalName(String proposedName) {
    int usedCount = usedGlobals[proposedName];
    if (usedCount === null) {
      // No element with this name has been used before.
      usedGlobals[proposedName] = 1;
      return proposedName;
    } else {
      // Not the first time we see this name. Append a number to make it unique.
      String name;
      do {
        usedCount++;
        name = '$proposedName$usedCount';
      } while (usedGlobals[name] !== null);
      usedGlobals[proposedName] = usedCount;
      return name;
    }
  }

  /**
   * Returns a preferred JS-id for the given top-level or static element.
   * The returned id is guaranteed to be a valid JS-id.
   */
  String _computeGuess(Element element) {
    assert(!element.isInstanceMember());
    if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR) {
      FunctionElement functionElement = element;
      return instanceMethodName(
          element.name, functionElement.parameterCount(compiler));
    } else {
      // TODO(floitsch): deal with named constructors.
      String name;
      if (element.kind == ElementKind.GETTER) {
        name = getterName(element.name);
      } else if (element.kind == ElementKind.SETTER) {
        name = setterName(element.name);
      } else if (element.kind == ElementKind.FUNCTION) {
        FunctionElement functionElement = element;
        name = element.name.slowToString();
        name = '$name\$${functionElement.parameterCount(compiler)}';
      } else {
        name = '${element.name.slowToString()}';
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
      if (element.kind == ElementKind.GENERATIVE_CONSTRUCTOR_BODY) {
        ConstructorBodyElement bodyElement = element;
        SourceString name = bodyElement.constructor.name;
        return instanceMethodName(name, bodyElement.parameterCount(compiler));
      } else if (element.kind == ElementKind.FUNCTION) {
        FunctionElement functionElement = element;
        return instanceMethodName(
            element.name, functionElement.parameterCount(compiler));
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
        case ElementKind.GETTER:
        case ElementKind.SETTER:
        case ElementKind.TYPEDEF:
          String result = getFreshGlobalName(guess);
          globals[element] = result;
          return result;

        default:
          compiler.internalError('getName for unknown kind: ${element.kind}',
                                 node: element.parseNode(compiler));
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

  String operatorIs(Element element) {
    return 'is\$${getName(element)}';
  }
}
