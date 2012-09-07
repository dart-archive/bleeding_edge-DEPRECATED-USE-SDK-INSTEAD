// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// The js.dart library provides simple synchronous JS invocation from
// Dart in a browser setting (Dartium or via dart2js).
//
// Methods
//   (1) js.invoke invokes a JS method (specified by string) on a
//       list of serializable parameters in the DOM window's JS context and
//       returns a deserialized result.  E.g.:
//
//   #import('js.dart', prefix: 'js');
//   #import('dart:html');
//   void main() {
//     final div = new DivElement();
//     div.innerHTML = js.invoke('decodeURI', ['%C3%A0%20la%20carte']);
//     document.body.nodes.add(div);
//   }
//
//   (2) js.eval evaluates an arbitrary string in the DOM window's JS context
//       and returns a deserialized result.  E.g.:
//
//   #import('js.dart', prefix: 'js');
//   main() {
//     js.eval('window.alert("Hello World")');
//   }
//
// Allowable Data Types
//   Parameters and return values be valid JSON types: String,
//   num (int/double), bool, List (of JSON), or Map (of JSON). 
//
//   To ease marshalling of user types, we provide additional convenience
//   mechanisms.  First, we provide a js.callback method to allow wrapped
//   callback functions to be used as parameters.  E.g.,
//
//   js.invoke('function (f) { setTimeout(f, 0) }', js.callback(f));
//
//
//   Second, we provide an abstract Serializable base class is provided
//   that converts the user type into JSON on demand.  E.g.,
//
//   class LatLng extends Serializable {
//     final num _lat;
//     final num _lng;
//     LatLng(this._lat, this._lng);
//
//     serialize() => encode(
//             'function (lat, lng) { return new LatLng(lat, lng); }',
//             [_lat, _lng]);
//   }
//
//   The user must define the serialize method to encode the user type.
//
// Implementation Overview
//
//   The APIs above are implemented via the DOM's synchronous dispatchEvent
//   mechanism.  All calls to JS are converted to 
//     1. Serialize parameters to a JSON string. 
//     2. Encode function string and parameters as a DOM Event and
//        dispatch to JS.
//     3. In JS, handle DOM Event:
//        (a) Deserialize the JSON string into JS objects.
//        (b) Evaluate the function string into a JS function.
//        (c) Invoke the JS function.
//        (d) Send the result back to Dart via a dispatchEvent in the
//            reverse direction.  Note, although Dart is blocked on stack,
//            it will still handle this Event synchronously.
//     4. Deserialize the returned result (if any) to Dart and return it.
//
// Caveats
// - When compiling to JS, invoked JS code may conflict with generated JS.
// - It is slow.  It channels through the DOM.
// - It is limited to JSON serializable data for parameters and return values.
// - It does not support easy invocation from JS to Dart (only Dart to JS).

#library('js');
#import('dart:html');
#import('dart:json');

// Tags in JSON sent to JS.
const String _DESERIALIZER = '_js_dart_deserialize_function';
const String _ARGUMENTS = '_js_dart_deserialize_arguments';
const String _CALLBACK = '_js_dart_callback';

// Tags in JSON returned from JS.
const String _TYPEID = '_js_dart_type';

// JS support code.
const String _jscode = '''
(function () {
  function createEvent(name, data) {
    var event = document.createEvent('TextEvent');
    event.initTextEvent(name, false, false, window, data);
    return event;
  }

  function dispatch(name, args) {
    try {
      var args = Array.prototype.slice.call(args);
      return eval('('+name+')').apply(this, args);
    } catch (e) {
      console.log('Dispatch Error: ' + e);
    }
  }

  function callback(id) {
    var f = function() {
      var data = JSON.stringify({'id': id, 'arguments': serialize(arguments)});
      window.dispatchEvent(createEvent('js-dart-callback', data));
    }
    return f;
  }

  function deserialize(obj) {
    if (obj && typeof(obj) == 'object') {
      if ('$_DESERIALIZER' in obj) {
        var f = obj['$_DESERIALIZER'];
        var args = deserialize(obj['$_ARGUMENTS']);
        return dispatch(f, args); 
      } else if ('$_CALLBACK' in obj) {
        var id = obj['$_CALLBACK'];
        return callback(id);
      }
      for (var key in obj) {
        obj[key] = deserialize(obj[key]);
      }
    }
    return obj;
  }

  var serializedTypes = {};
  var serializedNames = {};
  window.serializedNames = serializedNames;

  function name(obj) {
    // TODO(vsm): Make this portable.
    return obj.constructor.name;
  }

  function serialize(obj) {
    if (typeof(obj) == 'object') {
      var n = name(obj);
      if (n in serializedTypes) {
        var map = serializedTypes[n];
        var result = { '$_TYPEID': serializedNames[n] };
        for (var key in map) {
          var value = dispatch(map[key], [obj]);
          result[key] = serialize(value);
        }
        return result;
      }
      for (var key in obj) {
        obj[key] = serialize(obj[key]);
      }
    }
    return obj;
  }

  function handleInvoke(e) {
    var data = deserialize(JSON.parse(e.data));
    try {
      var name = data.method;
      var args = data.arguments;
      var ret = dispatch(name, args);
      var result = serialize(JSON.stringify({ 'return': ret }));
      window.dispatchEvent(createEvent('js-dart-result', result));
    } catch (e) {
      window.console.error('js.dart: ' + e);
      var error = JSON.stringify({ 'error': e });
      window.dispatchEvent(createEvent('js-dart-result', error));
    }
  }

  function registerType(e) {
    var data = JSON.parse(e.data);
    var id = eval(data.type).prototype.constructor.name;
    var map = data.fields;
    serializedTypes[id] = map;
    serializedNames[id] = data.type;
  }

  window.addEventListener('js-dart-invoke', handleInvoke, false);
  window.addEventListener('js-dart-register', registerType, false);
})();
''';

Event _createEvent(String name, final data) {
  // TODO(vsm): Use TextEvent constructor when that's available.
  TextEvent event = document.$dom_createEvent('TextEvent');
  event.initTextEvent(name, false, false, window, data);
  return event;
}

Event _createMethodEvent(String methodName, List arguments) {
  final data = { 'method': methodName, 'arguments': arguments };
  return _createEvent('js-dart-invoke', JSON.stringify(data));
}

Event _createRegistrationEvent(String typeName, Map<String,String> fields) {
  final data = { 'type': typeName, 'fields': fields };
  return _createEvent('js-dart-register', JSON.stringify(data));
}

var _result;

void _resultHandler(TextEvent e) {
  _result = _construct(JSON.parse(e.data));
}

/**
 * Invoke the given function in JavaScript in the same window with the
 * given arguments, and return the result.  All arguments (and the
 * result) must be valid JSON types or extend the Serializable class
 * below.
 */
invoke(String methodName, List arguments) {
  _initialize();
  if (arguments is! List) {
    throw new Exception('Invalid arguments: $arguments');
  }
  final methodEvent = _createMethodEvent(methodName, arguments);
  _result = null;
  window.$dom_dispatchEvent(methodEvent);
  if (null == _result || _result is! Map) {
    throw new Exception('Invalid result invoking: $methodName($arguments): $_result.');
  } else if (_result.containsKey('error')) {
    throw new Exception(_result['error']);
  }
  return _result['return'];
}

/**
 * Evaluate the given expression and return the result.
 */
eval(String command) {
  return invoke('eval', [command]);
}

// TODO(vsm): Fix this memory leak.  Alternatively, use a better
// abstraction (e.g., futures).
Map<int, Function> _callbacks;
int _callbackCounter = 0;

/**
 * Return a JSON serializable version of f that can be invoked from JS
 * as a callback.
 */
Map callback(Function f) {
  int id = _callbackCounter++;
  _callbacks[id] = f;
  var result = {};
  result[_CALLBACK] = id;
  return result;
}

_callbackHandler(TextEvent e) {
  var data = _construct(JSON.parse(e.data));
  int id = data['id'];
  var arguments = data['arguments'];
  Function f = _callbacks[id];
  switch (arguments.length) {
    case 0: return f();
    case 1: return f(arguments['0']); 
    case 2: return f(arguments['0'], arguments['1']);
    case 3: return f(arguments['0'], arguments['1'], arguments['2']); 
    default: throw new Exception('Unsupported number of arguments');
  }
}

bool _initialized = false;

void _initialize() {
  if (_initialized) {
    return;
  }
  _initialized = true;
  _callbacks = new Map<int, Function>();
  _constructors = new Map<String, Function>();

  window.on['js-dart-result'].add(_resultHandler, false);
  window.on['js-dart-callback'].add(_callbackHandler, false);

  final script = new ScriptElement();
  script.type = 'text/javascript';
  script.innerHTML = _jscode;
  document.body.nodes.add(script);
}

Map<String, Function> _constructors;

_construct(var obj) {
  if (obj is Map) {
    obj.forEach((key, value) {
        obj[key] = _construct(value);
      });
    if (obj.containsKey(_TYPEID)) {
      String id = obj[_TYPEID];
      Function constructor = _constructors[id];
      return constructor(obj);
    } 
  } else if (obj is List) {
    for (int i = 0; i < obj.length; ++i) {
      obj[i] = _construct(obj[i]);
    }
  }
  return obj;
}

/**
 * Register a JS type via its JavaScript [name] as serializable
 * when passed to Dart.
 *
 * [fields] declares how the JS object is converted to a map
 * representation.  Each key is a field name, and the value is the
 * text of a JS function that takes a JS object and returns this
 * field's value.  (I.e. value is a projection function for that
 * field.)
 *
 * [constructor] says how to go from a map representation to a Dart object.  It
 * takes a map arg, expects to see certain fields, and uses their values to
 * construct an object.
 */
void register(String name, Map<String, String> fields,
              Function constructor) {
  _initialize();
  final registerEvent = _createRegistrationEvent(name, fields);
  window.$dom_dispatchEvent(registerEvent);
  _constructors[name] = constructor;
}

// TODO(vsm): Eliminate once we have a Map mixin.
class _SerializableMap implements Map<String, Object> {
  // Just throw.  Everything required to keep the JSON library happy is implemented in Serializable below.
  _fail() { throw new NotImplementedException(); }
  
  operator [](key) => _fail();
  operator []=(key, value) => _fail();
  clear() => _fail();
  containsKey(key) => _fail();
  containsValue(value) => _fail();
  forEach(f) => _fail();
  getKeys() => _fail();
  getValues() => _fail();
  isEmpty() => _fail();
  get length => _fail();
  putIfAbsent(key, ifAbsent) => _fail();
  remove(key) => _fail();
}

/**
 * An abstract base type for Dart types that automatically serialize
 * to JavaScript via JSON.
 */
class Serializable extends _SerializableMap {

  void forEach(void f(String key, Object value)) {
    // Forward forEach to operate on the serialized copy instead.
    serialize().forEach(f);
  }

  /**
   * A helper for subclasses to encode as JSON.
   *
   * [jsDeserializer] defines (as a String) a JS function that,
   * given [arguments], will create a JS object.
   */
  Map<String, Object> encode(String jsDeserializer, List arguments) {
    var result = {};
    result[_DESERIALIZER] = jsDeserializer;
    result[_ARGUMENTS] = arguments;
    return result;
  }

  /**
   * Create a legal JSON Map from this object.
   */
  abstract Map<String, Object> serialize();
}
