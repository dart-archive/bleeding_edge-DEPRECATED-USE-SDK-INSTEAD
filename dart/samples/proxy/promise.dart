// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/** A library to illustrate pipelining. */
#library("promise");
#import("dart:isolate");

/** A promise to value of type [T] that may be computed asynchronously. */
// TODO(sigmund,benl): remove Promise<T> use Future<T> instead.
interface Promise<T> default PromiseImpl<T> {

  Promise();

  /** A promise that already has a computed value. */
  Promise.fromValue(T value);

  /**
   * The value once it is computed. It will be null when the promise is in
   * progress ([:!isDone():]), when it was cancelled ([:isCancelled():]), or
   * when the computed value is actually null.
   */
  T get value();

  /**
   * Provide the computed value; throws an exception if a value has already been
   * provided or the promise previously completed with an error; ignored if the
   * promise was cancelled.
   */
  void complete(T value);

  /** Error that occurred while computing the value, if any; null otherwise. */
  get error();

  /** Indicate that an error was found while computing this value. */
  void fail(var error);

  /** Whether the asynchronous work is done (normally or with errors). */
  bool isDone();

  /** Whether the work represented by this promise has been cancelled. */
  bool isCancelled();

  /** Whether the work represented by this promise has computed a value. */
  bool hasValue();

  /** Whether the work represented by this promise has finished in an error. */
  bool hasError();

  /** Cancel the asynchronous work of this promise, if possible. */
  bool cancel();

  /** Register a normal continuation to execute when the value is available. */
  void addCompleteHandler(void completeHandler(T result));

  /** Register an error continuation to execute if an error is found. */
  void addErrorHandler(void errorHandler(var error));

  /** Register a handler to execute when [cancel] is called. */
  void addCancelHandler(void cancelHandler());

  /**
   * When this promise completes, execute [callback]. The result of [callback]
   * will be exposed through the returned promise. This promise, and the
   * resulting promise (r) are connected as follows:
   *  - this.complete --> r.complete (with the result of [callback])
   *  - this.error    --> r.error (the same error is propagated to r)
   *  - this.cancel   --> r.error (the cancellation is shown as an error to r)
   *  - r.cancel      --> this continues executing regardless
   */
  Promise then(callback(T value));

  /**
   * Converts this promise so that its result is a non-promise value. For
   * instance, if this promise is of type Promise<Promise<Promise<T>>>,
   * flatten returns a Promise<T>.
   */
  Promise flatten();

  /**
   * Mark this promise as complete when some or all values in [arr] are
   * computed. Every time one of the promises is computed, it is passed to
   * [joinDone]. When [joinDone] returns true, this instance is marked as
   * complete with the last value that was computed.
   */
  void join(Collection<Promise> arr, bool joinDone(Promise completed));

  /**
   * Mark this promise as complete when [n] promises in [arr] complete, then
   * cancel the rest of the promises in [arr] that didn't complete.
   */
  void waitFor(Collection<Promise> arr, int n);
}


interface Proxy extends Promise<bool> default ProxyImpl {

  Proxy.forPort(SendPort port);
  Proxy.forIsolate(Isolate isolate);
  Proxy._forIsolateWithPromise(Isolate isolate, Promise<SendPort> promise);
  /*
   * The [Proxy.forReply] constructor is used to create a proxy for
   * the object that will be the reply to a message send.
   */
  Proxy.forReply(Promise<SendPort> port);

  void send(List message);
  Promise call(List message);

}


class ProxyImpl extends ProxyBase implements Proxy {

  ProxyImpl.forPort(SendPort port)
      : super.forPort(port) { }

  ProxyImpl.forIsolate(Isolate isolate)
      : this._forIsolateWithPromise(isolate, new Promise<SendPort>());

  ProxyImpl._forIsolateWithPromise(Isolate isolate, Promise<SendPort> promise)
      // TODO(floitsch): it seems wrong to call super.forReply here.
      : super.forReply(promise) {
    isolate.spawn().then((SendPort port) {
      promise.complete(port);
    });
  }

  /*
   * The [Proxy.forReply] constructor is used to create a proxy for
   * the object that will be the reply to a message send.
   */
  ProxyImpl.forReply(Promise<SendPort> port)
      : super.forReply(port) { }

}


class Dispatcher<T> {

  Dispatcher(this.target) { }

  void _serve(ReceivePort port) {
    port.receive((var message, SendPort replyTo) {
      this.process(message, void reply(var response) {
        Proxy proxy = new Proxy.forPort(replyTo);
        proxy.send([response]);
      });
    });
  }

  static SendPort serve(Dispatcher dispatcher) {
    ReceivePort port = ProxyBase.register(dispatcher);
    dispatcher._serve(port);
    return port.toSendPort();
  }

  // BUG(5015671): DartC doesn't support 'abstract' yet.
  /* abstract */ void process(var message, void reply(var response)) {
    throw "Abstract method called";
  }

  T target;

}

// When a promise is sent across a port, it is converted to a
// Promise<SendPort> down which we must send a port to receive the
// completion value. Hand the Promise<SendPort> to this class to deal
// with it.

class PromiseProxy<T> extends PromiseImpl<T> {
  PromiseProxy(Promise<SendPort> sendCompleter) {
    ReceivePort completer = new ReceivePort();
    completer.receive((var msg, _) {
      completer.close();
      complete(msg[0]);
    });
    sendCompleter.addCompleteHandler((SendPort port) {
      port.send([completer.toSendPort()], null);
    });
  }
}

class PromiseImpl<T> implements Promise<T> {

  // Enumeration of possible states:
  static final int CREATED = 0;
  static final int RUNNING = 1;
  static final int COMPLETE_NORMAL = 2;
  static final int COMPLETE_ERROR = 3;
  static final int CANCELLED = 4;

  // TODO(sigmund): consider whether this is what people want, or if we should
  // discard values/errors after cancellation.
  static final int COMPLETE_NORMAL_AFTER_CANCELLED = 5;
  static final int COMPLETE_ERROR_AFTER_CANCELLED = 6;


  /** Internal state, one of the above constants. */
  int _state;

  /** Value that was provided, if any. */
  T _value;

  /** Error that was provided, if any. */
  var _error;

  /** Listeners waiting for a normal completion of this promise. */
  Queue<Function> _normalListeners;

  /** Error listeners. */
  Queue<Function> _errorListeners;

  /** Cancellation listeners. */
  Queue<Function> _cancelListeners;

  PromiseImpl()
      : _state = CREATED,
        _value = null,
        _error = null,
        _normalListeners = null,
        _errorListeners = null,
        _cancelListeners = null {}

  PromiseImpl.fromValue(T val)
      : _state = COMPLETE_NORMAL,
        _value = val,
        _error = null,
        _normalListeners = null,
        _errorListeners = null,
        _cancelListeners = null {}

  // Properties and methods from Promise:

  T get value() {
    if (!isDone()) {
      // TODO(kasperl): Turn this into a proper exception object.
      throw new Exception("Attempted to get the value of an uncompleted promise.");
    }
    if (hasError()) {
      throw _error;
    } else {
      return _value;
    }
  }

  get error() {
    if (!isDone()) {
      // TODO(kasperl): Turn this into a proper exception object.
      throw "Attempted to examine the state of an uncompleted promise.";
    }
    return _error;
  }

  bool isDone() {
    return _state != CREATED && _state != RUNNING;
  }

  bool isCancelled() {
    return _state == CANCELLED
      || _state == COMPLETE_NORMAL_AFTER_CANCELLED
      || _state == COMPLETE_ERROR_AFTER_CANCELLED;
  }

  bool hasValue() {
    return _state == COMPLETE_NORMAL
        || _state == COMPLETE_NORMAL_AFTER_CANCELLED;
  }

  bool hasError() {
    return _state == COMPLETE_ERROR
        || _state == COMPLETE_ERROR_AFTER_CANCELLED;
  }

  void complete(T newVal) {
    if (_state == CANCELLED) {
      _value = newVal;
      _state = COMPLETE_NORMAL_AFTER_CANCELLED;
      return;
    }

    if (isDone()) {
      throw "Attempted to complete an already completed promise.";
    }

    _value = newVal;
    _state = COMPLETE_NORMAL;
    if (_normalListeners !== null) {
      _normalListeners.forEach((listener) {
        listener(newVal);
      });
    }
    _clearListeners();
  }

  void _clearListeners() {
    _normalListeners = null;
    _errorListeners = null;
    _cancelListeners = null;
  }

  void fail(var err) {
    if (_state == CANCELLED) {
      _error = err;
      _state = COMPLETE_ERROR_AFTER_CANCELLED;
      return;
    }

    if (isDone()) {
      throw "Can't fail an already completed promise.";
    }

    _error = err;
    _state = COMPLETE_ERROR;
    if (_errorListeners !== null) {
      _errorListeners.forEach((listener) {
        listener(err);
      });
    }
    _clearListeners();
  }

  bool cancel() {
    if (!isDone()) {
      _state = CANCELLED;
      if (_cancelListeners !== null) {
        _cancelListeners.forEach((listener) {
          listener();
        });
      }
      _clearListeners();
      return true;
    }
    return false;
  }

  void addCompleteHandler(void completeHandler(T result)) {
    if (_state == COMPLETE_NORMAL) {
      completeHandler(_value);
    } else if (!isDone()) {
      if (_normalListeners === null) {
        _normalListeners = new Queue<Function>();
      }
      _normalListeners.addLast(completeHandler);
    }
  }

  void addErrorHandler(void errorHandler(err)) {
    if (_state == COMPLETE_ERROR) {
      errorHandler(_error);
    } else if (!isDone()) {
      if (_errorListeners === null) {
        _errorListeners = new Queue<Function>();
      }
      _errorListeners.addLast(errorHandler);
    }
  }

  void addCancelHandler(void cancelHandler()) {
    if (isCancelled()) {
      cancelHandler();
    } else if (!isDone()) {
      if (_cancelListeners === null) {
        _cancelListeners = new Queue<Function>();
      }
      _cancelListeners.addLast(cancelHandler);
    }
  }

  // TODO(sigmund): consider adding to the API a method that does the following:
  // Promise chain(callback(T r)) {
  //   return then(callback).flatten();
  // }

  Promise then(callback(T result)) {
    Promise promise = new Promise();
    addCompleteHandler((T val) {
      promise.complete(callback(val));
    });
    addErrorHandler((err) { promise.fail(err); });
    addCancelHandler(() {
      promise.fail("Source promise was cancelled");
    });
    return promise;
  }

  // TODO(sigmund): consider adding to the API a method to represent
  // containment. For instance, promiseA spawns internally promiseB, where
  // promiseB is not visible to the user. Like [then] this creates a relation
  // between A, and B:
  //  - b.complete -> a.complete (as above)
  //  - b.error -> a.error (as above)
  //  - b.cancel -> a.error (as above)
  //  - a.cancel -> b.cancel (unlike [then])

  Promise flatten() {
    Promise res = new Promise();
    then((T thisVal) {
      if (thisVal is Promise) {
        Promise thisPromise = thisVal.dynamic;
        thisPromise.flatten().then((lastVal) {
          res.complete(lastVal);
        });
      } else {
        res.complete(thisVal);
      }
    });
    return res;
  }

  void join(Collection<Promise> promises, bool joinDone(Promise completed)) {
    promises.forEach((promise) {
      promise.addCompleteHandler((value) {
        if (joinDone(promise)) {
          complete(value);
        }
      });
      promise.addErrorHandler((err) {
        fail(err);
      });
    });
    addCancelHandler(() {
      promises.forEach((promise) {
        promise.cancel();
      });
    });
  }

  void waitFor(Collection<Promise> promises, int n) {
    int counter = 0;
    join(promises, (p) => (++counter == n));
    addCompleteHandler((val) {
      promises.forEach((promise) {
        if (!promise.isDone()) {
          promise.cancel();
        }
      });
    });
  }
}

// For now, extend Promise<bool> rather than either
// a) create a new base, Completable, for Promise and Proxy, or
// b) extend Promise<SendPort> which would expose the port.
class ProxyBase extends PromiseImpl<bool> {

  ProxyBase.forPort(SendPort port) {
    _promise = new Promise<SendPort>();
    _promise.complete(port);
    complete(true);
  }

  // Construct a proxy for a message reply; see the [Proxy.forReply]
  // documentation for more details.
  ProxyBase.forReply(Promise<SendPort> port) {
    _promise = port;
    port.addCompleteHandler((_) => complete(true));
  }

  // Note that comparing proxies or using them in maps or sets is
  // illegal until they complete.
  bool operator ==(var other) {
    return (other is ProxyBase) && _promise.value == other._promise.value;
  }

  int hashCode() => _promise.value.hashCode();

  static ReceivePort register(Dispatcher dispatcher) {
    if (_dispatchers === null) {
      _dispatchers = new Map<SendPort, Dispatcher>();
    }
    ReceivePort result = new ReceivePort();
    _dispatchers[result.toSendPort()] = dispatcher;
    return result;
  }

  get local() {
    if (_dispatchers !== null) {
      Dispatcher dispatcher = _dispatchers[_promise.value];
      if (dispatcher !== null) return dispatcher.target;
    }
    throw new Exception("Cannot access object of non-local proxy.");
  }

  void send(List message) {
    _marshal(message, (List marshalled) {
      SendPort port = _promise.value;
      port.send(marshalled, null);
    });
  }

  Promise call(List message) {
    return _marshal(message, (List marshalled) {
      // TODO(kasperl): For now, the [Promise.then] implementation allows
      // me to return a promise and it will do the promise chaining.
      final result = new Promise();
      // The promise queue implementation guarantees that promise is
      // resolved at this point.
      SendPort outgoing = _promise.value;
      outgoing.call(marshalled).then((List receiveMessage) {
        result.complete(receiveMessage[0]);
      });
      return result;
    });
  }

  // Marshal the [message] and pass it to the [process] callback
  // function. Any promises are converted to a port which expects to
  // receive a port from the other side down which the remote promise
  // can be completed by sending the promise's completion value.
  Promise _marshal(List message, process(List marshalled)) {
    return _promise.then((SendPort port) {
      List marshalled = new List(message.length);

      for (int i = 0; i < marshalled.length; i++) {
        var entry = message[i];
        if (entry is Proxy) {
          entry = entry._promise;
        }
        // Obviously this will be true if [entry] was a Proxy.
        if (entry is Promise) {
          // Note that we could optimise this by just sending the value
          // if the promise is already complete. Let's get this working
          // first!

          // This port will receive a SendPort that can be used to
          // signal completion of this promise to the corresponding
          // promise that the other end has created.
          ReceivePort receiveCompleter = new ReceivePort();
          marshalled[i] = receiveCompleter.toSendPort();
          Promise<SendPort> completer = new Promise<SendPort>();
          receiveCompleter.receive((var msg, SendPort replyPort) {
            receiveCompleter.close();
            completer.complete(msg[0]);
          });
          entry.addCompleteHandler((value) {
            completer.addCompleteHandler((SendPort completePort) {
              _marshal([value], (List completeMessage) => completePort.send(completeMessage, null));
            });
          });
        } else {
          // FIXME(kasperl, benl): this should probably be a copy?
          marshalled[i] = entry;
        }
        if (marshalled[i] is ReceivePort) {
          throw new Exception("Despite the documentation, you cannot send a ReceivePort");
        } 
      }
      return process(marshalled);
    }).flatten();
  }

  Promise<SendPort> _promise;
  static Map<SendPort, Dispatcher> _dispatchers;

}
