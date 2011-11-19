// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// JavaScript implementation of the native methods declared in json.dart.

// TODO(jmesserly): this needs lots of cleanup.
// Ideally JS objects could be treated as Dart Maps directly, and then we can
// really use native JSON.parse and JSON.stringify.


var $JSON = JSON; // Save the real JSON

JSON = function() {};
JSON.parse = function(str) {
  return $JSON.parse(str, function(k, v) { return $convertJsToDart(v); });
}

// Shallow-converts the parsed JavaScript value into a Dart object, and
// returns the Dart object.
// Any component values have already been converted.
function $convertJsToDart(obj) {
  if (obj != null && typeof obj == 'object' && !(obj instanceof Array)) {
    return $fixupJsObjectToDartMap(obj);
  }
  return obj;
}

// Converts the parsed JavaScript Object into a Dart Map.
function $fixupJsObjectToDartMap(obj) {
  var map = new HashMapImplementation();
  var keys = Object.keys(obj);
  for (var i = 0; i < keys.length; i++) {
    map.$setindex(keys[i], obj[keys[i]]);
  }
  return map;
}

///////////////////////////////////////////////////////////////////////////////

JSON.stringify = function(obj) {
  return $JSON.stringify(obj, $convertDartToJs);
}

// TODO(jmesserly): better error message!
function UnconvertibleException() {}

// Converts the Dart object into a JavaScript value, suitable for applying
// JSON.stringify to.
// Throws UnconvertibleException if the Dart value is not convertible.
function $convertDartToJs(key, obj) {
  // Only Dart Maps need to be converted into JavaScript objects.
  if (obj === null || obj === undefined) {
    return null;
  }
  switch (typeof obj) {
    case 'boolean':
    case 'number':
    case 'string':
      return obj;
    case 'object':
      if (obj instanceof Array) {
        return obj;
      } else {
        try {
          return $convertDartMapToJsObject(obj);
        } catch (e) {
          // TODO(jmesserly): HACK: assume a failure means it's not a Map, and
          // throw UnconvertibleException below...
        }
      }
  }
  $throw(new UnconvertibleException());
}

// Converts the Dart Map into a JavaScript object.
// Converts only shallowly.
function $convertDartMapToJsObject(map) {
  var obj = {};
  var propertyNames = map.getKeys();
  for (var i = 0, len = propertyNames.length; i < len; i++) {
    var propertyName = propertyNames[i];
    obj[propertyName] = map.$index(propertyName);
  }
  return obj;
}
