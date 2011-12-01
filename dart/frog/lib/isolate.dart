// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A native object that is shared across isolates. This object is visible to all
 * isolates running on the same worker (either UI or background web worker).
 *
 * This is code that is intended to 'escape' the isolate boundaries in order to
 * implement the semantics of friendly isolates in JavaScript. Without this we
 * would have been forced to implement more code (including the top-level event
 * loop) in JavaScript itself.
 */
GlobalState get _globalState() native "return \$globalState;";
set _globalState(GlobalState val) native "\$globalState = val;";

/**
 * Wrapper that takes the dart entry point and runs it within an isolate. The
 * frog compiler will inject a call of the form [: startRootIsolate(main); :]
 * when it determines that this wrapping is needed. For single-isolate
 * applications (e.g. hello world), this call is not emitted.
 */
void startRootIsolate(entry) {
  _globalState = new GlobalState();

  // Don't start the main loop again, if we are in a worker.
  if (_globalState.isWorker) return;
  final rootContext = new IsolateContext();
  _globalState.rootContext = rootContext;
  _fillStatics(rootContext);

  // BUG(5151491): Setting currentContext should not be necessary, but
  // because closures passed to the DOM as event handlers do not bind their
  // isolate automatically we try to give them a reasonable context to live in
  // by having a "default" isolate (the first one created).
  _globalState.currentContext = rootContext;

  rootContext.eval(entry);
  _globalState.topEventLoop.run();
}

void _fillStatics(context) native @"""
  $globals = context.isolateStatics;
  $static_init();
""";

/** Global state associated with the current worker. See [_globalState]. */
// TODO(sigmund): split in multiple classes: global, thread, main-worker states?
class GlobalState {

  /** Next available isolate id. */
  int nextIsolateId = 0;

  /** Worker id associated with this worker. */
  int currentWorkerId = 0;

  /**
   * Next available worker id. Only used by the main worker to assign a unique
   * id to each worker created by it.
   */
  int nextWorkerId = 1;

  /** Context for the currently running [Isolate]. */
  IsolateContext currentContext = null;

  /** Context for the root [Isolate] that first run in this worker. */
  IsolateContext rootContext = null;

  /** The top-level event loop. */
  EventLoop topEventLoop;

  /** Whether this program is running in a background worker. */
  bool isWorker;

  /** Whether this program is running in a UI worker. */
  bool inWindow;

  /** Whether we support spawning workers. */
  bool supportsWorkers;

  /**
   * Whether to use web workers when implementing isolates. Set to false for
   * debugging/testing.
   */
  bool get useWorkers() => supportsWorkers;

  /**
   * Whether to use the web-worker JSON-based message serialization protocol. By
   * default this is only used with web workers. For debugging, you can force
   * using this protocol by changing this field value to [true].
   */
  bool get needSerialization() => useWorkers;

  /**
   * Registry of isolates. Isolates must be registered if, and only if, receive
   * ports are alive.  Normally no open receive-ports means that the isolate is
   * dead, but DOM callbacks could resurrect it.
   */
  Map<int, IsolateContext> isolates;

  /** Reference to the main worker. */
  MainWorker mainWorker;

  /** Registry of active workers. Only used in the main worker. */
  Map<int, var> workers;

  GlobalState() {
    topEventLoop = new EventLoop();
    isolates = {};
    workers = {};
    mainWorker = new MainWorker();
    _nativeInit();
  }

  void _nativeInit() native @"""
    this.isWorker = typeof ($globalThis['importScripts']) != 'undefined';
    this.inWindow = typeof(window) !== 'undefined';
    this.supportsWorkers = this.isWorker ||
        ((typeof $globalThis['Worker']) != 'undefined');

    // if workers are supported, treat this as a main worker:
    if (this.supportsWorkers) {
      $globalThis.onmessage = function(e) {
        IsolateNatives._processWorkerMessage(this.mainWorker, e);
      };
    }
  """;

  /**
   * Close the worker running this code, called when there is nothing else to
   * run.
   */
  void closeWorker() {
    if (isWorker) {
      if (!isolates.isEmpty()) return;
      mainWorker.postMessage(
          _serializeMessage({'command': 'close'}));
    } else if (isolates.containsKey(rootContext.id) && workers.isEmpty() &&
               !supportsWorkers && !inWindow) {
      // This should only trigger when running on the command-line.
      // We don't want this check to execute in the browser where the isolate
      // might still be alive due to DOM callbacks.
      throw new Exception("Program exited with open ReceivePorts.");
    }
  }
}

_serializeMessage(message) {
  if (_globalState.needSerialization) {
    return new Serializer().traverse(message);
  } else {
    return new Copier().traverse(message);
  }
}

_deserializeMessage(message) {
  if (_globalState.needSerialization) {
    return new Deserializer().deserialize(message);
  } else {
    // Nothing more to do.
    return message;
  }
}

/** Default worker. */
class MainWorker {
  int id = 0;
  void postMessage(msg) native "return \$globalThis.postMessage(msg);";
  void onmessage(f) native "\$globalThis.onmessage = f;";
  void terminate() {}
}

/** Context information tracked for each isolate. */
class IsolateContext {
  /** Current isolate id. */
  int id;

  /** Registry of receive ports currently active on this isolate. */
  Map<int, ReceivePort> ports;

  /** Holds isolate globals (statics and top-level properties). */
  var isolateStatics; // native object containing all globals of an isolate.

  IsolateContext() {
    id = _globalState.nextIsolateId++;
    ports = {};
    initGlobals();
  }

  // these are filled lazily the first time the isolate starts running.
  void initGlobals() native 'this.isolateStatics = {};';

  /**
   * Run [code] in the context of the isolate represented by [this]. Note this
   * is called from JavaScript (see $wrap_call in corejs.dart).
   */
  void eval(Function code) {
    var old = _globalState.currentContext;
    _globalState.currentContext = this;
    this._setGlobals();
    var result = null;
    try {
      result = code();
    } finally {
      _globalState.currentContext = old;
      old._setGlobals();
    }
    return result;
  }

  void _setGlobals() native @'$globals = this.isolateStatics;';

  /** Lookup a port registered for this isolate. */
  ReceivePort lookup(int id) => ports[id];

  /** Register a port on this isolate. */
  void register(int portId, ReceivePort port)  {
    if (ports.containsKey(portId)) {
      throw new Exception("Registry: ports must be registered only once.");
    }
    ports[portId] = port;
    _globalState.isolates[id] = this; // indicate this isolate is active
  }

  /** Unregister a port on this isolate. */
  void unregister(int portId) {
    ports.remove(portId);
    if (ports.isEmpty()) {
      _globalState.isolates.remove(id); // indicate this isolate is not active
    }
  }
}

/** Represent the event loop on a javascript thread (DOM or worker). */
class EventLoop {
  Queue<IsolateEvent> events;

  EventLoop() : events = new Queue<IsolateEvent>();

  void enqueue(isolate, fn, msg) {
    events.addLast(new IsolateEvent(isolate, fn, msg));
  }

  IsolateEvent dequeue() {
    if (events.isEmpty()) return null;
    return events.removeFirst();
  }

  /** Process a single event, if any. */
  bool runIteration() {
    final event = dequeue();
    if (event == null) {
      _globalState.closeWorker();
      return false;
    }
    event.process();
    return true;
  }

  /** Function equivalent to [:window.setTimeout:] when available, or null. */
  static Function _wrapSetTimeout() native """
      return typeof window != 'undefined' ?
          function(a, b) { window.setTimeout(a, b); } : undefined;
  """;

  /**
   * Runs multiple iterations of the run-loop. If possible, each iteration is
   * run asynchronously.
   */
  void _runHelper() {
    final setTimeout = _wrapSetTimeout();
    if (setTimeout != null) {
      // Run each iteration from the browser's top event loop.
      void next() {
        if (!runIteration()) return;
        setTimeout(next, 0);
      }
      next();
    } else {
      // Run synchronously until no more iterations are available.
      while (runIteration()) {}
    }
  }

  /**
   * Call [_runHelper] but ensure that worker exceptions are propragated. Note
   * this is called from JavaScript (see $wrap_call in corejs.dart).
   */
  void run() {
    if (!_globalState.isWorker) {
      _runHelper();
    } else {
      try {
        _runHelper();
      } catch(e) {
        // TODO(floitsch): try to send stack-trace to the other side.
        _globalState.mainWorker.postMessage(_serializeMessage(
            {'command': 'error', 'msg': "" + e }));
      }
    }
  }
}

/** An event in the top-level event queue. */
class IsolateEvent {
  IsolateContext isolate;
  Function fn;
  String message;

  IsolateEvent(this.isolate, this.fn, this.message);

  void process() {
    isolate.eval(fn);
  }
}

/** Implementation of a send port on top of JavaScript. */
class SendPortImpl implements SendPort {

  const SendPortImpl(this._workerId, this._isolateId, this._receivePortId);

  void send(var message, [SendPort replyTo = null]) {
    if (replyTo !== null && !(replyTo is SendPortImpl)) {
      throw "SendPort::send: Illegal replyTo type.";
    }
    IsolateNatives._sendMessage(_workerId, _isolateId, _receivePortId,
        _serializeMessage(message), _serializeMessage(replyTo));
  }

  // TODO(sigmund): get rid of _sendNow (still used in corelib code)
  void _sendNow(var message, replyTo) { send(message, replyTo); }

  ReceivePortSingleShotImpl call(var message) {
    final result = new ReceivePortSingleShotImpl();
    this.send(message, result.toSendPort());
    return result;
  }

  ReceivePortSingleShotImpl _callNow(var message) {
    final result = new ReceivePortSingleShotImpl();
    send(message, result.toSendPort());
    return result;
  }

  bool operator==(var other) {
    return (other is SendPortImpl) &&
        (_workerId == other._workerId) &&
        (_isolateId == other._isolateId) &&
        (_receivePortId == other._receivePortId);
  }

  int hashCode() {
    return (_workerId << 16) ^ (_isolateId << 8) ^ _receivePortId;
  }

  final int _receivePortId;
  final int _isolateId;
  final int _workerId;
}

/** Default factory for receive ports. */
class ReceivePortFactory {

  factory ReceivePort() {
    return new ReceivePortImpl();
  }

  factory ReceivePort.singleShot() {
    return new ReceivePortSingleShotImpl();
  }
}

/** Implementation of a multi-use [ReceivePort] on top of JavaScript. */
class ReceivePortImpl implements ReceivePort {
  ReceivePortImpl()
      : _id = _nextFreeId++ {
    _globalState.currentContext.register(_id, this);
  }

  void receive(void onMessage(var message, SendPort replyTo)) {
    _callback = onMessage;
  }

  void close() {
    _callback = null;
    _globalState.currentContext.unregister(_id);
  }

  /**
   * Returns a fresh [SendPort]. The implementation is not allowed to cache
   * existing ports.
   */
  SendPort toSendPort() {
    return new SendPortImpl(
        _globalState.currentWorkerId, _globalState.currentContext.id, _id);
  }

  int _id;
  Function _callback;

  static int _nextFreeId = 1;
}

/** Implementation of a single-shot [ReceivePort]. */
class ReceivePortSingleShotImpl implements ReceivePort {

  ReceivePortSingleShotImpl() : _port = new ReceivePortImpl() { }

  void receive(void callback(var message, SendPort replyTo)) {
    _port.receive((var message, SendPort replyTo) {
      _port.close();
      callback(message, replyTo);
    });
  }

  void close() {
    _port.close();
  }

  SendPort toSendPort() => _port.toSendPort();

  final ReceivePortImpl _port;
}

final String _SPAWNED_SIGNAL = "spawned";

class IsolateNatives {

  /** JavaScript-specific implementation to spawn an isolate. */
  static Future<SendPort> spawn(Isolate isolate, bool isLight) {
    Completer<SendPort> completer = new Completer<SendPort>();
    ReceivePort port = new ReceivePort.singleShot();
    port.receive((msg, SendPort replyPort) {
      assert(msg == _SPAWNED_SIGNAL);
      completer.complete(replyPort);
    });

    // TODO(floitsch): throw exception if isolate's class doesn't have a
    // default constructor.
    if (_globalState.useWorkers && !isLight) {
      _startWorker(isolate, port.toSendPort());
    } else {
      _startNonWorker(isolate, port.toSendPort());
    }

    return completer.future;
  }

  static SendPort _startWorker(Isolate runnable, SendPort replyPort) {
    var factoryName = _getJSConstructorName(runnable);
    if (_globalState.isWorker) {
      _globalState.mainWorker.postMessage(_serializeMessage({
          'command': 'spawn-worker',
          'factoryName': factoryName,
          'replyPort': replyPort}));
    } else {
      _spawnWorker(factoryName, _serializeMessage(replyPort));
    }
  }


  /**
   * The src url for the script tag that loaded this code. Used to create
   * JavaScript workers.
   */
  static String get _thisScript() =>
      _thisScriptCache != null ? _thisScriptCache : _computeThisScript();

  static String _thisScriptCache;

  // TODO(sigmund): fix - this code should be run synchronously when loading the
  // script. Running lazily on DOMContentLoaded will yield incorrect results.
  static String _computeThisScript() native @"""
    if (!$globalState.supportsWorkers || $globalState.isWorker) return null;

    // TODO(5334778): Find a cross-platform non-brittle way of getting the
    // currently running script.
    var scripts = document.getElementsByTagName('script');
    // The scripts variable only contains the scripts that have already been
    // executed. The last one is the currently running script.
    var script = scripts[scripts.length - 1];
    var src = script && script.src;
    if (!src) {
      // TODO()
      src = "FIXME:5407062" + "_" + Math.random().toString();
      if (script) script.src = src;
    }
    IsolateNatives._thisScriptCache = src;
    return src;
  """;

  /** Starts a new worker with the given URL. */
  static _newWorker(url) native "return new Worker(url)";

  /**
   * Spawns an isolate in a worker. [factoryName] is the Javascript constructor
   * name for the isolate entry point class.
   */
  static void _spawnWorker(factoryName, serializedReplyPort) {
    var worker = _newWorker(_thisScript);
    // TODO(sigmund): make this work.
    worker.onmessage = function(e) {
      _processWorkerMessage(worker, e);
    };
    var workerId = _globalState.nextWorkerId++;
    // We also store the id on the worker itself so that we can unregister it.
    worker.id = workerId;
    _globalState.workers[workerId] = worker;
    worker.postMessage(_serializeMessage({
      'command': 'start',
      'id': workerId,
      'replyTo': serializedReplyPort,
      'factoryName': factoryName }));
  }

  /**
   * Assume that [e] is a browser message event and extract its message data.
   * We don't import the dom explicitly so, when workers are disabled, this
   * library can also run on top of nodejs.
   */
  static _getEventData(e) native "return e.data";

  /**
   * Process messages on a worker, either to control the worker instance or to
   * pass messages along to the isolate running in the worker.
   */
  static void _processWorkerMessage(sender, e) {
    var msg = _deserializeMessage(_getEventData(e));
    switch (msg['command']) {
      case 'start':
        _globalState.currentWorkerId = msg['id'];
        var runnerObject =
            _allocate(_getJSConstructorFromName(msg['factoryName']));
        var serializedReplyTo = msg['replyTo'];
        _globalState.topEventLoop.enqueue(new IsolateContext(), function() {
          var replyTo = _deserializeMessage(serializedReplyTo);
          IsolateNatives._startIsolate(runnerObject, replyTo);
        }, 'worker-start');
        _globalState.topEventLoop.run();
        break;
      case 'spawn-worker':
        _spawnWorker(msg['factoryName'], msg['replyPort']);
        break;
      case 'message':
        _sendMessage(msg['workerId'], msg['isolateId'], msg['portId'],
            msg['msg'], msg['replyTo']);
        _globalState.topEventLoop.run();
        break;
      case 'close':
        _log("Closing Worker");
        _globalState.workers.remove(sender.id);
        sender.terminate();
        _globalState.topEventLoop.run();
        break;
      case 'log':
        _log(msg['msg']);
        break;
      case 'print':
        if (_globalState.isWorker) {
          _globalState.mainWorker.postMessage(
              _serializeMessage({'command': 'print', 'msg': msg}));
        } else {
          print(msg['msg']);
        }
        break;
      case 'error':
        throw msg['msg'];
    }
  }

  /** Log a message, forwarding to the main worker if appropriate. */
  static _log(msg) {
    if (_globalState.isWorker) {
      _globalState.mainWorker.postMessage(
          _serializeMessage({'command': 'log', 'msg': msg }));
    } else {
      try {
        _consoleLog(msg);
      } catch(e, trace) {
        throw new Exception(trace);
      }
    }
  }

  static void _consoleLog(msg) native "\$globalThis.console.log(msg);";


  /**
   * Extract the constructor of runnable, so it can be allocated in another
   * isolate.
   */
  static var _getJSConstructor(Isolate runnable) native """
    return runnable.constructor;
  """;

  /** Extract the constructor name of a runnable */
  // TODO(sigmund): find a browser-generic way to support this.
  static var _getJSConstructorName(Isolate runnable) native """
    return runnable.constructor.name;
  """;

  /** Find a constructor given it's name. */
  static var _getJSConstructorFromName(String factoryName) native """
    return \$globalThis[factoryName];
  """;

  /** Create a new JavasSript object instance given it's constructor. */
  static var _allocate(var ctor) native "return new ctor();";

  /** Starts a non-worker isolate. */
  static SendPort _startNonWorker(Isolate runnable, SendPort replyTo) {
    // Spawn a new isolate and create the receive port in it.
    final spawned = new IsolateContext();

    // Instead of just running the provided runnable, we create a
    // new cloned instance of it with a fresh state in the spawned
    // isolate. This way, we do not get cross-isolate references
    // through the runnable.
    final ctor = _getJSConstructor(runnable);
    _globalState.topEventLoop.enqueue(spawned, function() {
      _startIsolate(_allocate(ctor), replyTo);
    }, 'nonworker start');
  }

  /** Given a ready-to-start runnable, start running it. */
  static void _startIsolate(Isolate isolate, SendPort replyTo) {
    _fillStatics(_globalState.currentContext);
    ReceivePort port = new ReceivePort();
    replyTo.send(_SPAWNED_SIGNAL, port.toSendPort());
    isolate._run(port);
  }

  static void _sendMessage(int workerId, int isolateId, int receivePortId,
      message, replyTo) {
    // Both the message and the replyTo are already serialized.
    if (workerId == _globalState.currentWorkerId) {
      var isolate = _globalState.isolates[isolateId];
      if (isolate == null) return;  // Isolate has been closed.
      var receivePort = isolate.lookup(receivePortId);
      if (receivePort == null) return;  // ReceivePort has been closed.
      _globalState.topEventLoop.enqueue(isolate, () {
        if (receivePort._callback != null) {
          receivePort._callback(
            _deserializeMessage(message), _deserializeMessage(replyTo));
        }
      }, 'receive ' + message);
    } else {
      var worker;
      // communication between workers go through the main worker
      if (_globalState.isWorker) {
        worker = _globalState.mainWorker;
      } else {
        // TODO(sigmund): make sure this works
        worker = _globalState.workers[workerId];
      }
      worker.postMessage(_serializeMessage({
          'command': 'message',
          'workerId': workerId,
          'isolateId': isolateId,
          'portId': receivePortId,
          'msg': message,
          'replyTo': replyTo }));
    }
  }
}
