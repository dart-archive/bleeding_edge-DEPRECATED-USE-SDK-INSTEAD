// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * The js.dart library provides simple JavaScript invocation from Dart that
 * works on both Dartium and on other modern browsers via Dart2JS.
 *
 * It provides a model based on scoped [Proxy] objects.  Proxies give Dart
 * code access to JavaScript objects, fields, and functions as well as the
 * ability to pass Dart objects and functions to JavaScript functions.  Scopes
 * enable developers to use proxies without memory leaks - a common challenge
 * with cross-runtime interoperation.
 *
 * The top-level [context] getter provides a [Proxy] to the global JavaScript
 * context for the page your Dart code is running on.  In the following example:
 *
 *     import 'package:js/js.dart' as js;
 *
 *     void main() {
 *       js.context.alert('Hello from Dart via JavaScript');
 *     }
 *
 * js.context.alert creates a proxy to the top-level alert function in
 * JavaScript.  It is invoked from Dart as a regular function that forwards to
 * the underlying JavaScript one.  By default, proxies are released when
 * the currently executing event completes, e.g., when main is completes
 * in this example.
 *
 * The library also enables JavaScript proxies to Dart objects and functions.
 * For example, the following Dart code:
 *
 *     js.context.dartCallback = new Callback.once((x) => print(x*2));
 *
 * defines a top-level JavaScript function 'dartCallback' that is a proxy to
 * the corresponding Dart function.  The [Callback.once] constructor allows the
 * proxy to the Dart function to be retained across multiple events;
 * instead it is released after the first invocation.  (This is a common
 * pattern for asychronous callbacks.)
 *
 * Note, parameters and return values are intuitively passed by value for
 * primitives and by reference for non-primitives.  In the latter case, the
 * references are automatically wrapped and unwrapped as proxies by the library.
 *
 * This library also allows construction of JavaScripts objects given a [Proxy]
 * to a corresponding JavaScript constructor.  For example, if the following
 * JavaScript is loaded on the page:
 *
 *     function Foo(x) {
 *       this.x = x;
 *     }
 *
 *     Foo.prototype.add = function(other) {
 *       return new Foo(this.x + other.x);
 *     }
 *
 * then, the following Dart:
 *
 *     var foo = new js.Proxy(js.context.Foo, 42);
 *     var foo2 = foo.add(foo);
 *     print(foo2.x);
 *
 * will construct a JavaScript Foo object with the parameter 42, invoke its
 * add method, and return a [Proxy] to a new Foo object whose x field is 84.
 *
 * See [samples](http://dart-lang.github.com/js-interop/example) for more
 * examples of usage.
 *
 * See this [article](http://www.dartlang.org/articles/js-dart-interop) for
 * more detailed discussion.
 */

library js;

import 'dart:async';
import 'dart:html';
import 'dart:isolate';
import 'dart:mirrors';

// JavaScript bootstrapping code.
// TODO(vsm): Migrate this to use a builtin resource mechanism once we have
// one.

// NOTE: Please re-run tools/create_bootstrap.dart on any modification of
// this bootstrap string.
final _JS_BOOTSTRAP = r"""
(function() {
  // Proxy support for js.dart.

  var globalContext = window;

  // Support for binding the receiver (this) in proxied functions.
  function bindIfFunction(f, _this) {
    if (typeof(f) != "function") {
      return f;
    } else {
      return new BoundFunction(_this, f);
    }
  }

  function unbind(obj) {
    if (obj instanceof BoundFunction) {
      return obj.object;
    } else {
      return obj;
    }
  }

  function getBoundThis(obj) {
    if (obj instanceof BoundFunction) {
      return obj._this;
    } else {
      return globalContext;
    }
  }

  function BoundFunction(_this, object) {
    this._this = _this;
    this.object = object;
  }

  // Table for local objects and functions that are proxied.
  function ProxiedObjectTable() {
    // Name for debugging.
    this.name = 'js-ref';

    // Table from IDs to JS objects.
    this.map = {};

    // Generator for new IDs.
    this._nextId = 0;

    // Counter for deleted proxies.
    this._deletedCount = 0;

    // Flag for one-time initialization.
    this._initialized = false;

    // Ports for managing communication to proxies.
    this.port = new ReceivePortSync();
    this.sendPort = this.port.toSendPort();

    // Set of IDs that are global.
    // These will not be freed on an exitScope().
    this.globalIds = {};

    // Stack of scoped handles.
    this.handleStack = [];

    // Stack of active scopes where each value is represented by the size of
    // the handleStack at the beginning of the scope.  When an active scope
    // is popped, the handleStack is restored to where it was when the
    // scope was entered.
    this.scopeIndices = [];
  }

  // Number of valid IDs.  This is the number of objects (global and local)
  // kept alive by this table.
  ProxiedObjectTable.prototype.count = function () {
    return Object.keys(this.map).length;
  }

  // Number of total IDs ever allocated.
  ProxiedObjectTable.prototype.total = function () {
    return this.count() + this._deletedCount;
  }

  // Adds an object to the table and return an ID for serialization.
  ProxiedObjectTable.prototype.add = function (obj) {
    if (this.scopeIndices.length == 0) {
      throw "Cannot allocate a proxy outside of a scope.";
    }
    // TODO(vsm): Cache refs for each obj?
    var ref = this.name + '-' + this._nextId++;
    this.handleStack.push(ref);
    this.map[ref] = obj;
    return ref;
  }

  ProxiedObjectTable.prototype._initializeOnce = function () {
    if (!this._initialized) {
      this._initialize();
      this._initialized = true;
    }
  }

  // Enters a new scope for this table.
  ProxiedObjectTable.prototype.enterScope = function() {
    this._initializeOnce();
    this.scopeIndices.push(this.handleStack.length);
  }

  // Invalidates all non-global IDs in the current scope and
  // exit the current scope.
  ProxiedObjectTable.prototype.exitScope = function() {
    var start = this.scopeIndices.pop();
    for (var i = start; i < this.handleStack.length; ++i) {
      var key = this.handleStack[i];
      if (!this.globalIds.hasOwnProperty(key)) {
        delete this.map[this.handleStack[i]];
        this._deletedCount++;
      }
    }
    this.handleStack = this.handleStack.splice(0, start);
  }

  // Makes this ID globally scope.  It must be explicitly invalidated.
  ProxiedObjectTable.prototype.globalize = function(id) {
    this.globalIds[id] = true;
  }

  // Invalidates this ID, potentially freeing its corresponding object.
  ProxiedObjectTable.prototype.invalidate = function(id) {
    var old = this.get(id);
    delete this.globalIds[id];
    delete this.map[id];
    this._deletedCount++;
  }

  // Gets the object or function corresponding to this ID.
  ProxiedObjectTable.prototype.get = function (id) {
    if (!this.map.hasOwnProperty(id)) {
      throw 'Proxy ' + id + ' has been invalidated.'
    }
    return this.map[id];
  }

  ProxiedObjectTable.prototype._initialize = function () {
    // Configure this table's port to forward methods, getters, and setters
    // from the remote proxy to the local object.
    var table = this;

    this.port.receive(function (message) {
      // TODO(vsm): Support a mechanism to register a handler here.
      try {
        var object = table.get(message[0]);
        var receiver = unbind(object);
        var member = message[1];
        var kind = message[2];
        var args = message[3].map(deserialize);
        if (kind == 'get') {
          // Getter.
          var field = member;
          if (field in receiver && args.length == 0) {
            var result = bindIfFunction(receiver[field], receiver);
            return [ 'return', serialize(result) ];
          }
        } else if (kind == 'set') {
          // Setter.
          var field = member;
          if (args.length == 1) {
            return [ 'return', serialize(receiver[field] = args[0]) ];
          }
        } else if (kind == 'apply') {
          // Direct function invocation.
          var _this = getBoundThis(object);
          return [ 'return', serialize(receiver.apply(_this, args)) ];
        } else if (member == '[]' && args.length == 1) {
          // Index getter.
          var result = bindIfFunction(receiver[args[0]], receiver);
          return [ 'return', serialize(result) ];
        } else if (member == '[]=' && args.length == 2) {
          // Index setter.
          return [ 'return', serialize(receiver[args[0]] = args[1]) ];
        } else {
          // Member function invocation.
          var f = receiver[member];
          if (f) {
            var result = f.apply(receiver, args);
            return [ 'return', serialize(result) ];
          }
        }
        return [ 'none' ];
      } catch (e) {
        return [ 'throws', e.toString() ];
      }
    });
  }

  // Singleton for local proxied objects.
  var proxiedObjectTable = new ProxiedObjectTable();

  // DOM element serialization code.
  var _localNextElementId = 0;
  var _DART_ID = 'data-dart_id';
  var _DART_TEMPORARY_ATTACHED = 'data-dart_temporary_attached';

  function serializeElement(e) {
    // TODO(vsm): Use an isolate-specific id.
    var id;
    if (e.hasAttribute(_DART_ID)) {
      id = e.getAttribute(_DART_ID);
    } else {
      id = (_localNextElementId++).toString();
      e.setAttribute(_DART_ID, id);
    }
    if (e !== document.documentElement) {
      // Element must be attached to DOM to be retrieve in js part.
      // Attach top unattached parent to avoid detaching parent of "e" when
      // appending "e" directly to document. We keep count of elements
      // temporarily attached to prevent detaching top unattached parent to
      // early. This count is equals to the length of _DART_TEMPORARY_ATTACHED
      // attribute. There could be other elements to serialize having the same
      // top unattached parent.
      var top = e;
      while (true) {
        if (top.hasAttribute(_DART_TEMPORARY_ATTACHED)) {
          var oldValue = top.getAttribute(_DART_TEMPORARY_ATTACHED);
          var newValue = oldValue + "a";
          top.setAttribute(_DART_TEMPORARY_ATTACHED, newValue);
          break;
        }
        if (top.parentNode == null) {
          top.setAttribute(_DART_TEMPORARY_ATTACHED, "a");
          document.documentElement.appendChild(top);
          break;
        }
        if (top.parentNode === document.documentElement) {
          // e was already attached to dom
          break;
        }
        top = top.parentNode;
      }
    }
    return id;
  }

  function deserializeElement(id) {
    // TODO(vsm): Clear the attribute.
    var list = document.querySelectorAll('[' + _DART_ID + '="' + id + '"]');

    if (list.length > 1) throw 'Non unique ID: ' + id;
    if (list.length == 0) {
      throw 'Element must be attached to the document: ' + id;
    }
    var e = list[0];
    if (e !== document.documentElement) {
      // detach temporary attached element
      var top = e;
      while (true) {
        if (top.hasAttribute(_DART_TEMPORARY_ATTACHED)) {
          var oldValue = top.getAttribute(_DART_TEMPORARY_ATTACHED);
          var newValue = oldValue.substring(1);
          top.setAttribute(_DART_TEMPORARY_ATTACHED, newValue);
          // detach top only if no more elements have to be unserialized
          if (top.getAttribute(_DART_TEMPORARY_ATTACHED).length === 0) {
            top.removeAttribute(_DART_TEMPORARY_ATTACHED);
            document.documentElement.removeChild(top);
          }
          break;
        }
        if (top.parentNode === document.documentElement) {
          // e was already attached to dom
          break;
        }
        top = top.parentNode;
      }
    }
    return e;
  }


  // Type for remote proxies to Dart objects.
  function DartProxy(id, sendPort) {
    this.id = id;
    this.port = sendPort;
  }

  // Serializes JS types to SendPortSync format:
  // - primitives -> primitives
  // - sendport -> sendport
  // - DOM element -> [ 'domref', element-id ]
  // - Function -> [ 'funcref', function-id, sendport ]
  // - Object -> [ 'objref', object-id, sendport ]
  function serialize(message) {
    if (message == null) {
      return null;  // Convert undefined to null.
    } else if (typeof(message) == 'string' ||
               typeof(message) == 'number' ||
               typeof(message) == 'boolean') {
      // Primitives are passed directly through.
      return message;
    } else if (message instanceof SendPortSync) {
      // Non-proxied objects are serialized.
      return message;
    } else if (message instanceof Element &&
        (message.ownerDocument == null || message.ownerDocument == document)) {
      return [ 'domref', serializeElement(message) ];
    } else if (message instanceof BoundFunction &&
               typeof(message.object) == 'function') {
      // Local function proxy.
      return [ 'funcref',
               proxiedObjectTable.add(message),
               proxiedObjectTable.sendPort ];
    } else if (typeof(message) == 'function') {
      if ('_dart_id' in message) {
        // Remote function proxy.
        var remoteId = message._dart_id;
        var remoteSendPort = message._dart_port;
        return [ 'funcref', remoteId, remoteSendPort ];
      } else {
        // Local function proxy.
        return [ 'funcref',
                 proxiedObjectTable.add(message),
                 proxiedObjectTable.sendPort ];
      }
    } else if (message instanceof DartProxy) {
      // Remote object proxy.
      return [ 'objref', message.id, message.port ];
    } else {
      // Local object proxy.
      return [ 'objref',
               proxiedObjectTable.add(message),
               proxiedObjectTable.sendPort ];
    }
  }

  function deserialize(message) {
    if (message == null) {
      return null;  // Convert undefined to null.
    } else if (typeof(message) == 'string' ||
               typeof(message) == 'number' ||
               typeof(message) == 'boolean') {
      // Primitives are passed directly through.
      return message;
    } else if (message instanceof SendPortSync) {
      // Serialized type.
      return message;
    }
    var tag = message[0];
    switch (tag) {
      case 'funcref': return deserializeFunction(message);
      case 'objref': return deserializeObject(message);
      case 'domref': return deserializeElement(message[1]);
    }
    throw 'Unsupported serialized data: ' + message;
  }

  // Create a local function that forwards to the remote function.
  function deserializeFunction(message) {
    var id = message[1];
    var port = message[2];
    // TODO(vsm): Add a more robust check for a local SendPortSync.
    if ("receivePort" in port) {
      // Local function.
      return unbind(proxiedObjectTable.get(id));
    } else {
      // Remote function.  Forward to its port.
      var f = function () {
        var depth = enterScope();
        try {
          var args = Array.prototype.slice.apply(arguments);
          args.splice(0, 0, this);
          args = args.map(serialize);
          var result = port.callSync([id, '#call', args]);
          if (result[0] == 'throws') throw deserialize(result[1]);
          return deserialize(result[1]);
        } finally {
          exitScope(depth);
        }
      };
      // Cache the remote id and port.
      f._dart_id = id;
      f._dart_port = port;
      return f;
    }
  }

  // Creates a DartProxy to forwards to the remote object.
  function deserializeObject(message) {
    var id = message[1];
    var port = message[2];
    // TODO(vsm): Add a more robust check for a local SendPortSync.
    if ("receivePort" in port) {
      // Local object.
      return proxiedObjectTable.get(id);
    } else {
      // Remote object.
      return new DartProxy(id, port);
    }
  }

  // Remote handler to construct a new JavaScript object given its
  // serialized constructor and arguments.
  function construct(args) {
    args = args.map(deserialize);
    var constructor = unbind(args[0]);
    args = Array.prototype.slice.call(args, 1);

    // Until 10 args, the 'new' operator is used. With more arguments we use a
    // generic way that may not work, particulary when the constructor does not
    // have an "apply" method.
    var ret = null;
    if (args.length === 0) {
      ret = new constructor();
    } else if (args.length === 1) {
      ret = new constructor(args[0]);
    } else if (args.length === 2) {
      ret = new constructor(args[0], args[1]);
    } else if (args.length === 3) {
      ret = new constructor(args[0], args[1], args[2]);
    } else if (args.length === 4) {
      ret = new constructor(args[0], args[1], args[2], args[3]);
    } else if (args.length === 5) {
      ret = new constructor(args[0], args[1], args[2], args[3], args[4]);
    } else if (args.length === 6) {
      ret = new constructor(args[0], args[1], args[2], args[3], args[4],
                            args[5]);
    } else if (args.length === 7) {
      ret = new constructor(args[0], args[1], args[2], args[3], args[4],
                            args[5], args[6]);
    } else if (args.length === 8) {
      ret = new constructor(args[0], args[1], args[2], args[3], args[4],
                            args[5], args[6], args[7]);
    } else if (args.length === 9) {
      ret = new constructor(args[0], args[1], args[2], args[3], args[4],
                            args[5], args[6], args[7], args[8]);
    } else if (args.length === 10) {
      ret = new constructor(args[0], args[1], args[2], args[3], args[4],
                            args[5], args[6], args[7], args[8], args[9]);
    } else {
      // Dummy Type with correct constructor.
      var Type = function(){};
      Type.prototype = constructor.prototype;
  
      // Create a new instance
      var instance = new Type();
  
      // Call the original constructor.
      ret = constructor.apply(instance, args);
      ret = Object(ret) === ret ? ret : instance;
    }
    return serialize(ret);
  }

  // Remote handler to return the top-level JavaScript context.
  function context(data) {
    return serialize(globalContext);
  }

  // Remote handler to track number of live / allocated proxies.
  function proxyCount() {
    var live = proxiedObjectTable.count();
    var total = proxiedObjectTable.total();
    return [live, total];
  }

  // Return true if two JavaScript proxies are equal (==).
  function proxyEquals(args) {
    return deserialize(args[0]) == deserialize(args[1]);
  }

  // Return true if a JavaScript proxy is instance of a given type (instanceof).
  function proxyInstanceof(args) {
    var obj = unbind(deserialize(args[0]));
    var type = unbind(deserialize(args[1]));
    return obj instanceof type;
  }

  // Return true if a JavaScript proxy has a given property.
  function proxyHasProperty(args) {
    var obj = unbind(deserialize(args[0]));
    var member = unbind(deserialize(args[1]));
    return member in obj;
  }

  // Delete a given property of object.
  function proxyDeleteProperty(args) {
    var obj = unbind(deserialize(args[0]));
    var member = unbind(deserialize(args[1]));
    delete obj[member];
  }

  function proxyConvert(args) {
    return serialize(deserializeDataTree(args));
  }

  function deserializeDataTree(data) {
    var type = data[0];
    var value = data[1];
    if (type === 'map') {
      var obj = {};
      for (var i = 0; i < value.length; i++) {
        obj[value[i][0]] = deserializeDataTree(value[i][1]);
      }
      return obj;
    } else if (type === 'list') {
      var list = [];
      for (var i = 0; i < value.length; i++) {
        list.push(deserializeDataTree(value[i]));
      }
      return list;
    } else /* 'simple' */ {
      return deserialize(value);
    }
  }

  function makeGlobalPort(name, f) {
    var port = new ReceivePortSync();
    port.receive(f);
    window.registerPort(name, port.toSendPort());
  }

  // Enters a new scope in the JavaScript context.
  function enterJavaScriptScope() {
    proxiedObjectTable.enterScope();
  }

  // Enters a new scope in both the JavaScript and Dart context.
  var _dartEnterScopePort = null;
  function enterScope() {
    enterJavaScriptScope();
    if (!_dartEnterScopePort) {
      _dartEnterScopePort = window.lookupPort('js-dart-interop-enter-scope');
    }
    return _dartEnterScopePort.callSync([]);
  }

  // Exits the current scope (and invalidate local IDs) in the JavaScript
  // context.
  function exitJavaScriptScope() {
    proxiedObjectTable.exitScope();
  }

  // Exits the current scope in both the JavaScript and Dart context.
  var _dartExitScopePort = null;
  function exitScope(depth) {
    exitJavaScriptScope();
    if (!_dartExitScopePort) {
      _dartExitScopePort = window.lookupPort('js-dart-interop-exit-scope');
    }
    return _dartExitScopePort.callSync([ depth ]);
  }

  makeGlobalPort('dart-js-interop-context', context);
  makeGlobalPort('dart-js-interop-create', construct);
  makeGlobalPort('dart-js-interop-proxy-count', proxyCount);
  makeGlobalPort('dart-js-interop-equals', proxyEquals);
  makeGlobalPort('dart-js-interop-instanceof', proxyInstanceof);
  makeGlobalPort('dart-js-interop-has-property', proxyHasProperty);
  makeGlobalPort('dart-js-interop-delete-property', proxyDeleteProperty);
  makeGlobalPort('dart-js-interop-convert', proxyConvert);
  makeGlobalPort('dart-js-interop-enter-scope', enterJavaScriptScope);
  makeGlobalPort('dart-js-interop-exit-scope', exitJavaScriptScope);
  makeGlobalPort('dart-js-interop-globalize', function(data) {
    if (data[0] == "objref" || data[0] == "funcref") return proxiedObjectTable.globalize(data[1]);
    throw 'Illegal type: ' + data[0];
  });
  makeGlobalPort('dart-js-interop-invalidate', function(data) {
    if (data[0] == "objref" || data[0] == "funcref") return proxiedObjectTable.invalidate(data[1]);
    throw 'Illegal type: ' + data[0];
  });
})();
""";

// Injects JavaScript source code onto the page.
// This is only used to load the bootstrapping code above.
void _inject(code) {
  final script = new ScriptElement();
  script.type = 'text/javascript';
  script.innerHtml = code;
  document.body.nodes.add(script);
}

// Global ports to manage communication from Dart to JS.
SendPortSync _jsPortSync = null;
SendPortSync _jsPortCreate = null;
SendPortSync _jsPortProxyCount = null;
SendPortSync _jsPortEquals = null;
SendPortSync _jsPortInstanceof = null;
SendPortSync _jsPortHasProperty = null;
SendPortSync _jsPortDeleteProperty = null;
SendPortSync _jsPortConvert = null;
SendPortSync _jsEnterJavaScriptScope = null;
SendPortSync _jsExitJavaScriptScope = null;
SendPortSync _jsGlobalize = null;
SendPortSync _jsInvalidate = null;

// Global ports to manage communication from JS to Dart.
ReceivePortSync _dartEnterDartScope = null;
ReceivePortSync _dartExitDartScope = null;

// Initializes bootstrap code and ports.
void _initialize() {
  if (_jsPortSync != null) return;

  // Test if the port is already defined.
  try {
    _jsPortSync = window.lookupPort('dart-js-interop-context');
  } catch (e) {
    // TODO(vsm): Suppress the exception until dartbug.com/5854 is fixed.
  }

  // If not, try injecting the script.
  if (_jsPortSync == null) {
    _inject(_JS_BOOTSTRAP);
    _jsPortSync = window.lookupPort('dart-js-interop-context');
  }

  _jsPortCreate = window.lookupPort('dart-js-interop-create');
  _jsPortProxyCount = window.lookupPort('dart-js-interop-proxy-count');
  _jsPortEquals = window.lookupPort('dart-js-interop-equals');
  _jsPortInstanceof = window.lookupPort('dart-js-interop-instanceof');
  _jsPortHasProperty = window.lookupPort('dart-js-interop-has-property');
  _jsPortDeleteProperty = window.lookupPort('dart-js-interop-delete-property');
  _jsPortConvert = window.lookupPort('dart-js-interop-convert');
  _jsEnterJavaScriptScope = window.lookupPort('dart-js-interop-enter-scope');
  _jsExitJavaScriptScope = window.lookupPort('dart-js-interop-exit-scope');
  _jsGlobalize = window.lookupPort('dart-js-interop-globalize');
  _jsInvalidate = window.lookupPort('dart-js-interop-invalidate');

  _dartEnterDartScope = new ReceivePortSync()
    ..receive((_) => _enterScope());
  _dartExitDartScope = new ReceivePortSync()
    ..receive((args) => _exitScope(args[0]));
  window.registerPort('js-dart-interop-enter-scope', _dartEnterDartScope.toSendPort());
  window.registerPort('js-dart-interop-exit-scope', _dartExitDartScope.toSendPort());
}

/**
 * Returns a proxy to the global JavaScript context for this page.
 */
Proxy get context {
  _enterScopeIfNeeded();
  return _deserialize(_jsPortSync.callSync([]));
}

// Depth of current scope.  Return 0 if no scope.
get _depth => _proxiedObjectTable._scopeIndices.length;

// If we are not already in a scope, enter one and register a
// corresponding exit once we return to the event loop.
void _enterScopeIfNeeded() {
  if (_depth == 0) {
    var depth = _enterScope();
    scheduleMicrotask(() => _exitScope(depth));
  }
}

/**
 * Executes the closure [f] within a scope.  Any proxies created within this
 * scope are invalidated afterward unless they are converted to a global proxy.
 */
scoped(f) {
  var depth = _enterScope();
  try {
    return f();
  } finally {
    _exitScope(depth);
  }
}

int _enterScope() {
  _initialize();
  _proxiedObjectTable.enterScope();
  _jsEnterJavaScriptScope.callSync([]);
  return _proxiedObjectTable._scopeIndices.length;
}

void _exitScope(int depth) {
  assert(_proxiedObjectTable._scopeIndices.length == depth);
  _jsExitJavaScriptScope.callSync([]);
  _proxiedObjectTable.exitScope();
}

/*
 * Enters a scope and returns the depth of the scope stack.
 */
/// WARNING: This API is experimental and may be removed.
int $experimentalEnterScope() {
  return _enterScope();
}

/*
 * Exits a scope.  The [depth] must match that returned by the corresponding
 * enter scope call.
 */
/// WARNING: This API is experimental and may be removed.
void $experimentalExitScope(int depth) {
  _exitScope(depth);
}

/**
 * Retains the given [object] beyond the current scope.
 * Instead, it will need to be explicitly released.
 * The given [object] is returned for convenience.
 */
// TODO(aa) : change dynamic to Serializable<Proxy> if http://dartbug.com/9023
// is fixed.
// TODO(aa) : change to "<T extends Serializable<Proxy>> T retain(T object)"
// once generic methods have landed.
dynamic retain(Serializable<Proxy> object) {
  _jsGlobalize.callSync(_serialize(object.toJs()));
  return object;
}

/**
 * Releases a retained [object].
 */
void release(Serializable<Proxy> object) {
  _jsInvalidate.callSync(_serialize(object.toJs()));
}

/**
 * Check if [proxy] is instance of [type].
 */
bool instanceof(Serializable<Proxy> proxy, Serializable<FunctionProxy> type) =>
    _jsPortInstanceof.callSync([proxy.toJs(), type.toJs()].map(_serialize)
        .toList());

/**
 * Check if [proxy] has a [name] property.
 */
bool hasProperty(Serializable<Proxy> proxy, String name) =>
  _jsPortHasProperty.callSync([proxy.toJs(), name].map(_serialize).toList());

/**
 * Delete the [name] property of [proxy].
 */
void deleteProperty(Serializable<Proxy> proxy, String name) {
  _jsPortDeleteProperty.callSync([proxy.toJs(), name].map(_serialize).toList());
}

/**
 * Converts a Dart map [data] to a JavaScript map and return a [Proxy] to it.
 */
Proxy map(Map data) => new Proxy._json(data);

/**
 * Converts a Dart [Iterable] to a JavaScript array and return a [Proxy] to it.
 */
Proxy array(Iterable data) => new Proxy._json(data);

/**
 * Converts a local Dart function to a callback that can be passed to
 * JavaScript.
 *
 * A callback can either be:
 *
 * - single-fire, in which case it is automatically invalidated after the first
 *   invocation, or
 * - multi-fire, in which case it must be explicitly disposed.
 */
class Callback implements Serializable<FunctionProxy> {
  var _manualDispose;
  var _id;
  var _callback;

  _initialize(manualDispose) {
    _manualDispose = manualDispose;
    _id = _proxiedObjectTable.add(_callback);
    _proxiedObjectTable.globalize(_id);
  }

  _dispose() {
    var c = _proxiedObjectTable.invalidate(_id);
  }

  FunctionProxy toJs() =>
      new FunctionProxy._internal(_proxiedObjectTable.sendPort, _id);

  /**
   * Disposes this [Callback] so that it may be collected.
   * Once a [Callback] is disposed, it is an error to invoke it from JavaScript.
   */
  dispose() {
    assert(_manualDispose);
    _dispose();
  }

  /**
   * Creates a single-fire [Callback] that invokes [f].  The callback is
   * automatically disposed after the first invocation.
   */
  Callback.once(Function f, {bool withThis: false}) {
    _callback = (List args) {
      try {
        return Function.apply(f, withThis ? args : args.skip(1).toList());
      } finally {
        _dispose();
      }
    };
    _initialize(false);
  }

  /**
   * Creates a multi-fire [Callback] that invokes [f].  The callback must be
   * explicitly disposed to avoid memory leaks.
   */
  Callback.many(Function f, {bool withThis: false}) {
    _callback = (List args) =>
        Function.apply(f, withThis ? args : args.skip(1).toList());
    _initialize(true);
  }
}

// Detect unspecified arguments.
class _Undefined {
  const _Undefined();
}
const _undefined = const _Undefined();
List _pruneUndefined(arg1, arg2, arg3, arg4, arg5, arg6) {
  // This assumes no argument
  final args = [arg1, arg2, arg3, arg4, arg5, arg6];
  final index = args.indexOf(_undefined);
  if (index < 0) return args;
  return args.sublist(0, index);
}

/**
 * Proxies to JavaScript objects.
 */
class Proxy implements Serializable<Proxy> {
  SendPortSync _port;
  final _id;

  /**
   * Constructs a [Proxy] to a new JavaScript object by invoking a (proxy to a)
   * JavaScript [constructor].  The arguments should be either
   * primitive values, DOM elements, or Proxies.
   */
  factory Proxy(Serializable<FunctionProxy> constructor,
      [arg1 = _undefined,
       arg2 = _undefined,
       arg3 = _undefined,
       arg4 = _undefined,
       arg5 = _undefined,
       arg6 = _undefined]) {
      var arguments = _pruneUndefined(arg1, arg2, arg3, arg4, arg5, arg6);
      return new Proxy.withArgList(constructor, arguments);
  }

  /**
   * Constructs a [Proxy] to a new JavaScript object by invoking a (proxy to a)
   * JavaScript [constructor].  The [arguments] list should contain either
   * primitive values, DOM elements, or Proxies.
   */
  factory Proxy.withArgList(Serializable<FunctionProxy> constructor,
      List arguments) {
    _enterScopeIfNeeded();
    final serialized = ([constructor]..addAll(arguments)).map(_serialize).
        toList();
    final result = _jsPortCreate.callSync(serialized);
    return _deserialize(result);
  }

  /**
   * Constructs a [Proxy] to a new JavaScript map or list created defined via
   * Dart map or list.
   */
  factory Proxy._json(data) {
    _enterScopeIfNeeded();
    return _convert(data);
  }

  static _convert(data) {
    return _deserialize(_jsPortConvert.callSync(_serializeDataTree(data)));
  }

  static _serializeDataTree(data) {
    if (data is Map) {
      final entries = new List();
      for (var key in data.keys) {
        entries.add([key, _serializeDataTree(data[key])]);
      }
      return ['map', entries];
    } else if (data is Iterable) {
      return ['list', data.map(_serializeDataTree).toList()];
    } else {
      return ['simple', _serialize(data)];
    }
  }

  Proxy._internal(this._port, this._id);

  Proxy toJs() => this;

  // Resolve whether this is needed.
  operator[](arg) => _forward(this, '[]', 'method', [ arg ]);

  // Resolve whether this is needed.
  operator[]=(key, value) => _forward(this, '[]=', 'method', [ key, value ]);

  // Test if this is equivalent to another Proxy.  This essentially
  // maps to JavaScript's == operator.
  // TODO(vsm): Can we avoid forwarding to JS?
  operator==(other) => identical(this, other)
      ? true
      : (other is Proxy &&
         _jsPortEquals.callSync([_serialize(this), _serialize(other)]));

  String toString() {
    try {
      return _forward(this, 'toString', 'method', []);
    } catch(e) {
      return super.toString();
    }
  }

  // Forward member accesses to the backing JavaScript object.
  noSuchMethod(Invocation invocation) {
    String member = MirrorSystem.getName(invocation.memberName);
    // If trying to access a JavaScript field/variable that starts with
    // _ (underscore), Dart treats it a library private and member name
    // it suffixed with '@internalLibraryIdentifier' which we have to
    // strip before sending over to the JS side.
    if (member.indexOf('@') != -1) {
      member = member.substring(0, member.indexOf('@'));
    }
    String kind;
    List args = invocation.positionalArguments;
    if (args == null) args = [];
    if (invocation.isGetter) {
      kind = 'get';
    } else if (invocation.isSetter) {
      kind = 'set';
      if (member.endsWith('=')) {
        member = member.substring(0, member.length - 1);
      }
    } else if (member == 'call') {
      // A 'call' (probably) means that this proxy was invoked directly
      // as if it was a function.  Map this to JS function application.
      kind = 'apply';
    } else {
      kind = 'method';
    }
    return _forward(this, member, kind, args);
  }

  // Forward member accesses to the backing JavaScript object.
  static _forward(Proxy receiver, String member, String kind, List args) {
    _enterScopeIfNeeded();
    var result = receiver._port.callSync([receiver._id, member, kind,
                                          args.map(_serialize).toList()]);
    switch (result[0]) {
      case 'return': return _deserialize(result[1]);
      case 'throws': throw _deserialize(result[1]);
      case 'none': throw new NoSuchMethodError(receiver, member, args, {});
      default: throw 'Invalid return value';
    }
  }
}

// TODO(aa) make FunctionProxy implements Function once it is allowed
/// A [Proxy] subtype to JavaScript functions.
class FunctionProxy extends Proxy
    implements Serializable<FunctionProxy> /*,Function*/ {
  FunctionProxy._internal(SendPortSync port, id) : super._internal(port, id);

  // TODO(vsm): This allows calls with a limited number of arguments
  // in the context of dartbug.com/9283.  Eliminate pending the resolution
  // of this bug.  Note, if this Proxy is called with more arguments then
  // allowed below, it will trigger the 'call' path in Proxy.noSuchMethod
  // - and still work correctly in unminified mode.
  call([arg1 = _undefined, arg2 = _undefined,
        arg3 = _undefined, arg4 = _undefined,
        arg5 = _undefined, arg6 = _undefined]) {
    var arguments = _pruneUndefined(arg1, arg2, arg3, arg4, arg5, arg6);
    return Proxy._forward(this, '', 'apply', arguments);
  }
}

/// Marker class used to indicate it is serializable to js. If a class is a
/// [Serializable] the "toJs" method will be called and the result will be used
/// as value.
abstract class Serializable<T> {
  T toJs();
}

// A table to managed local Dart objects that are proxied in JavaScript.
class _ProxiedObjectTable {
  // Debugging name.
  final String _name;

  // Generator for unique IDs.
  int _nextId;

  // Counter for invalidated IDs for debugging.
  int _deletedCount;

  // Table of IDs to Dart objects.
  final Map<String, Object> _registry;

  // Port to handle and forward requests to the underlying Dart objects.
  // A remote proxy is uniquely identified by an ID and SendPortSync.
  final ReceivePortSync _port;

  // The set of IDs that are global.  These must be explicitly invalidated.
  final Set<String> _globalIds;

  // The stack of valid IDs.
  final List<String> _handleStack;

  // The stack of scopes, where each scope is represented by an index into the
  // handleStack.
  final List<int> _scopeIndices;

  // Enters a new scope.
  enterScope() {
    _scopeIndices.add(_handleStack.length);
  }

  // Invalidates non-global IDs created in the current scope and
  // restore to the previous scope.
  exitScope() {
    int start = _scopeIndices.removeLast();
    for (int i = start; i < _handleStack.length; ++i) {
      String key = _handleStack[i];
      if (!_globalIds.contains(key)) {
        _registry.remove(_handleStack[i]);
        _deletedCount++;
      }
    }
    if (start != _handleStack.length) {
      _handleStack.removeRange(start, _handleStack.length - start);
    }
  }

  // Converts an ID to a global.
  globalize(id) => _globalIds.add(id);

  // Invalidates an ID.
  invalidate(id) {
    var old = _registry[id];
    _globalIds.remove(id);
    _registry.remove(id);
    _deletedCount++;
    return old;
  }

  // Replaces the object referenced by an ID.
  _replace(id, x) {
    _registry[id] = x;
  }

  _ProxiedObjectTable() :
      _name = 'dart-ref',
      _nextId = 0,
      _deletedCount = 0,
      _registry = {},
      _port = new ReceivePortSync(),
      _handleStack = new List<String>(),
      _scopeIndices = new List<int>(),
      _globalIds = new Set<String>() {
        _port.receive((msg) {
          try {
            final receiver = _registry[msg[0]];
            final method = msg[1];
            final args = msg[2].map(_deserialize).toList();
            if (method == '#call') {
              final func = receiver as Function;
              var result = _serialize(func(args));
              return ['return', result];
            } else {
              // TODO(vsm): Support a mechanism to register a handler here.
              throw 'Invocation unsupported on non-function Dart proxies';
            }
          } catch (e) {
            // TODO(vsm): callSync should just handle exceptions itself.
            return ['throws', '$e'];
          }
        });
      }

  // Adds a new object to the table and return a new ID for it.
  String add(x) {
    _enterScopeIfNeeded();
    // TODO(vsm): Cache x and reuse id.
    final id = '$_name-${_nextId++}';
    _registry[id] = x;
    _handleStack.add(id);
    return id;
  }

  // Gets an object by ID.
  Object get(String id) {
    return _registry[id];
  }

  // Gets the current number of objects kept alive by this table.
  get count => _registry.length;

  // Gets the total number of IDs ever allocated.
  get total => count + _deletedCount;

  // Gets a send port for this table.
  get sendPort => _port.toSendPort();
}

// The singleton to manage proxied Dart objects.
_ProxiedObjectTable _proxiedObjectTable = new _ProxiedObjectTable();

/// End of proxy implementation.

// Dart serialization support.

_serialize(var message) {
  if (message == null) {
    return null;  // Convert undefined to null.
  } else if (message is String ||
             message is num ||
             message is bool) {
    // Primitives are passed directly through.
    return message;
  } else if (message is SendPortSync) {
    // Non-proxied objects are serialized.
    return message;
  } else if (message is Element &&
      (message.document == null || message.document == document)) {
    return [ 'domref', _serializeElement(message) ];
  } else if (message is FunctionProxy) {
    // Remote function proxy.
    return [ 'funcref', message._id, message._port ];
  } else if (message is Proxy) {
    // Remote object proxy.
    return [ 'objref', message._id, message._port ];
  } else if (message is Serializable) {
    // use of result of toJs()
    return _serialize(message.toJs());
  } else {
    // Local object proxy.
    return [ 'objref',
             _proxiedObjectTable.add(message),
             _proxiedObjectTable.sendPort ];
  }
}

_deserialize(var message) {
  deserializeFunction(message) {
    var id = message[1];
    var port = message[2];
    if (port == _proxiedObjectTable.sendPort) {
      // Local function.
      return _proxiedObjectTable.get(id);
    } else {
      // Remote function.  Forward to its port.
      return new FunctionProxy._internal(port, id);
    }
  }

  deserializeObject(message) {
    var id = message[1];
    var port = message[2];
    if (port == _proxiedObjectTable.sendPort) {
      // Local object.
      return _proxiedObjectTable.get(id);
    } else {
      // Remote object.
      return new Proxy._internal(port, id);
    }
  }


  if (message == null) {
    return null;  // Convert undefined to null.
  } else if (message is String ||
             message is num ||
             message is bool) {
    // Primitives are passed directly through.
    return message;
  } else if (message is SendPortSync) {
    // Serialized type.
    return message;
  }
  var tag = message[0];
  switch (tag) {
    case 'funcref': return deserializeFunction(message);
    case 'objref': return deserializeObject(message);
    case 'domref': return _deserializeElement(message[1]);
  }
  throw 'Unsupported serialized data: $message';
}

// DOM element serialization.

int _localNextElementId = 0;

const _DART_ID = 'data-dart_id';
const _DART_TEMPORARY_ATTACHED = 'data-dart_temporary_attached';

_serializeElement(Element e) {
  // TODO(vsm): Use an isolate-specific id.
  var id;
  if (e.attributes.containsKey(_DART_ID)) {
    id = e.attributes[_DART_ID];
  } else {
    id = 'dart-${_localNextElementId++}';
    e.attributes[_DART_ID] = id;
  }
  if (!identical(e, document.documentElement)) {
    // Element must be attached to DOM to be retrieve in js part.
    // Attach top unattached parent to avoid detaching parent of "e" when
    // appending "e" directly to document. We keep count of elements
    // temporarily attached to prevent detaching top unattached parent to
    // early. This count is equals to the length of _DART_TEMPORARY_ATTACHED
    // attribute. There could be other elements to serialize having the same
    // top unattached parent.
    var top = e;
    while (true) {
      if (top.attributes.containsKey(_DART_TEMPORARY_ATTACHED)) {
        final oldValue = top.attributes[_DART_TEMPORARY_ATTACHED];
        final newValue = oldValue + 'a';
        top.attributes[_DART_TEMPORARY_ATTACHED] = newValue;
        break;
      }
      if (top.parent == null) {
        top.attributes[_DART_TEMPORARY_ATTACHED] = 'a';
        document.documentElement.children.add(top);
        break;
      }
      if (identical(top.parent, document.documentElement)) {
        // e was already attached to dom
        break;
      }
      top = top.parent;
    }
  }
  return id;
}

Element _deserializeElement(var id) {
  var list = queryAll('[$_DART_ID="$id"]');
  if (list.length > 1) throw 'Non unique ID: $id';
  if (list.length == 0) {
    throw 'Only elements attached to document can be serialized: $id';
  }
  final e = list[0];
  if (!identical(e, document.documentElement)) {
    // detach temporary attached element
    var top = e;
    while (true) {
      if (top.attributes.containsKey(_DART_TEMPORARY_ATTACHED)) {
        final oldValue = top.attributes[_DART_TEMPORARY_ATTACHED];
        final newValue = oldValue.substring(1);
        top.attributes[_DART_TEMPORARY_ATTACHED] = newValue;
        // detach top only if no more elements have to be unserialized
        if (top.attributes[_DART_TEMPORARY_ATTACHED].length == 0) {
          top.attributes.remove(_DART_TEMPORARY_ATTACHED);
          top.remove();
        }
        break;
      }
      if (identical(top.parent, document.documentElement)) {
        // e was already attached to dom
        break;
      }
      top = top.parent;
    }
  }
  return e;
}

// Fetch the number of proxies to JavaScript objects.
// This returns a 2 element list.  The first is the number of currently
// live proxies.  The second is the total number of proxies ever
// allocated.
List _proxyCountJavaScript() => _jsPortProxyCount.callSync([]);

/**
 * Returns the number of allocated proxy objects matching the given
 * conditions.  By default, the total number of live proxy objects are
 * return.  In a well behaved program, this should stay below a small
 * bound.
 *
 * Set [all] to true to return the total number of proxies ever allocated.
 * Set [dartOnly] to only count proxies to Dart objects (live or all).
 * Set [jsOnly] to only count proxies to JavaScript objects (live or all).
 */
int proxyCount({all: false, dartOnly: false, jsOnly: false}) {
  final js = !dartOnly;
  final dart = !jsOnly;
  final jsCounts = js ? _proxyCountJavaScript() : null;
  var sum = 0;
  if (!all) {
    if (js)
      sum += jsCounts[0];
    if (dart)
      sum += _proxiedObjectTable.count;
  } else {
    if (js)
      sum += jsCounts[1];
    if (dart)
      sum += _proxiedObjectTable.total;
  }
  return sum;
}

// Prints the number of live handles in Dart and JavaScript.  This is for
// debugging / profiling purposes.
void _proxyDebug([String message = '']) {
  print('Proxy status $message:');
  var dartLive = proxyCount(dartOnly: true);
  var dartTotal = proxyCount(dartOnly: true, all: true);
  var jsLive = proxyCount(jsOnly: true);
  var jsTotal = proxyCount(jsOnly: true, all: true);
  print('  Dart objects Live : $dartLive (out of $dartTotal ever allocated).');
  print('  JS objects Live : $jsLive (out of $jsTotal ever allocated).');
}
