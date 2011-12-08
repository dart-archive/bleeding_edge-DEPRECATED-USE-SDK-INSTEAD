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

  String get currentIsolate() => "currentIsolate";
  String get isolate() => "Isolate";

  String define(Element element) {
    assert(globals[element] === null);

    String dartId = '${element.name}';
    // Prefix the dartId with '$' if the name is reserved.
    if (jsReserved.contains(dartId)) {
      dartId = "\$$dartId";
      assert(!jsReserved.contains(dartId));
    }

    int usedCount = usedGlobals[dartId];
    if (usedCount === null) {
      // No element with this name has been used before.
      usedGlobals[dartId] = 1;
      globals[element] = dartId;
      return dartId;
    } else {
      // Not the first time we see an element with this name. Append a number
      // to make it unique.
      String id;
      do {
        usedCount++;
        id = '$dartId$usedCount';
      } while (usedGlobals[id] !== null);
      usedGlobals[dartId] = usedCount;
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

  String methodName(Element element) {
    // TODO(floitsch): mangle if necessary.
    return '${element.name}';
  }
}
