// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dart._js_names;

import 'dart:_js_embedded_names' show
    JsGetName,
    MANGLED_GLOBAL_NAMES,
    MANGLED_NAMES;

import 'dart:_foreign_helper' show
    JS,
    JS_EMBEDDED_GLOBAL,
    JS_GET_NAME;

import 'dart:_js_helper' show
    JsCache,
    NoInline;

import 'dart:_interceptors' show JSArray;

/// No-op method that is called to inform the compiler that unmangled named
/// must be preserved.
preserveNames() {}

/// A map from mangled names to "reflective" names, that is, unmangled names
/// with some additional information, such as, number of required arguments.
/// This map is for mangled names used as instance members.
final Map<String, String> mangledNames =
    computeMangledNames(
        JS_EMBEDDED_GLOBAL('=Object', MANGLED_NAMES),
        false);

/// A map from "reflective" names to mangled names (the reverse of
/// [mangledNames]).
final Map<String, String> reflectiveNames =
    computeReflectiveNames(mangledNames);

/// A map from mangled names to "reflective" names (see [mangledNames]).  This
/// map is for globals, that is, static and top-level members.
final Map<String, String> mangledGlobalNames = computeMangledNames(
        JS_EMBEDDED_GLOBAL('=Object', MANGLED_GLOBAL_NAMES),
        true);

/// A map from "reflective" names to mangled names (the reverse of
/// [mangledGlobalNames]).
final Map<String, String> reflectiveGlobalNames =
    computeReflectiveNames(mangledGlobalNames);

/// [jsMangledNames] is a JavaScript object literal.  The keys are the mangled
/// names, and the values are the "reflective" names.
Map<String, String> computeMangledNames(jsMangledNames, bool isGlobal) {
  preserveNames();
  var keys = extractKeys(jsMangledNames);
  var result = <String, String>{};
  String getterPrefix = JS_GET_NAME(JsGetName.GETTER_PREFIX);
  int getterPrefixLength = getterPrefix.length;
  String setterPrefix = JS_GET_NAME(JsGetName.SETTER_PREFIX);
  for (String key in keys) {
    String value = JS('String', '#[#]', jsMangledNames, key);
    result[key] = value;
    if (!isGlobal) {
      if (key.startsWith(getterPrefix)) {
        result['$setterPrefix${key.substring(getterPrefixLength)}'] = '$value=';
      }
    }
  }
  return result;
}

Map<String, String> computeReflectiveNames(Map<String, String> map) {
  preserveNames();
  var result = <String, String>{};
  map.forEach((String mangledName, String reflectiveName) {
    result[reflectiveName] = mangledName;
  });
  return result;
}

@NoInline()
List extractKeys(victim) {
  var result = JS('', '# ? Object.keys(#) : []', victim, victim);
  return new JSArray.markFixed(result);
}

/**
 * Returns the (global) unmangled version of [name].
 *
 * Normally, you should use [mangledGlobalNames] directly, but this method
 * doesn't tell the compiler to preserve names. So this method only returns a
 * non-null value if some other component has made the compiler preserve names.
 *
 * This is used, for example, to return unmangled names from TypeImpl.toString
 * *if* names are being preserved for other reasons (use of dart:mirrors, for
 * example).
 */
String unmangleGlobalNameIfPreservedAnyways(String name) {
  var names = JS_EMBEDDED_GLOBAL('=Object', MANGLED_GLOBAL_NAMES);
  return JsCache.fetch(names, name);
}

String unmangleAllIdentifiersIfPreservedAnyways(String str) {
  return JS("String",
            r"(#).replace(/[^<,> ]+/g,"
            r"function(m) { return #[m] || m; })",
            str,
            JS_EMBEDDED_GLOBAL('', MANGLED_GLOBAL_NAMES));
}