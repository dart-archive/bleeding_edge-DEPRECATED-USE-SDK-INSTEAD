// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Abstract visitor for dart objects that can be passed as messages between any
 * isolates.
 */
class MessageTraverser {
  static bool isPrimitive(x) {
    return (x === null) || (x is String) || (x is num) || (x is bool);
  }

  MessageTraverser();

  /** Visitor's entry point. */
  traverse(var x) {
    if (isPrimitive(x)) return visitPrimitive(x);
    _taggedObjects = new List();
    var result;
    try {
      result = _dispatch(x);
    } finally {
      _cleanup();
    }
    return result;
  }

  /** Remove all information injected in the native objects by this visitor. */
  void _cleanup() {
    int len = _taggedObjects.length;
    for (int i = 0; i < len; i++) {
      _clearAttachedInfo(_taggedObjects[i]);
    }
    _taggedObjects = null;
  }

  /** Injects into the native object some information used by the visitor. */
  void _attachInfo(var o, var info) {
    _taggedObjects.add(o);
    _setAttachedInfo(o, info);
  }

  /** Retrieves any information stored in the native object [o]. */
  _getInfo(var o) {
    return _getAttachedInfo(o);
  }

  _dispatch(var x) {
    if (isPrimitive(x)) return visitPrimitive(x);
    if (x is List) return visitList(x);
    if (x is Map) return visitMap(x);
    if (x is SendPortImpl) return visitSendPort(x);
    if (x is ReceivePortImpl) return visitReceivePort(x);
    if (x is ReceivePortSingleShotImpl) return visitReceivePortSingleShot(x);
    // TODO(floitsch): make this a real exception. (which one)?
    throw "Message serialization: Illegal value $x passed";
  }

  abstract visitPrimitive(x);
  abstract visitList(List x);
  abstract visitMap(Map x);
  abstract visitSendPort(SendPortImpl x);
  abstract visitReceivePort(ReceivePortImpl x);
  abstract visitReceivePortSingleShot(ReceivePortSingleShotImpl x);

  List _taggedObjects;

  _clearAttachedInfo(var o) native
      "o['__MessageTraverser__attached_info__'] = (void 0);";

  _setAttachedInfo(var o, var info) native
      "o['__MessageTraverser__attached_info__'] = info;";

  _getAttachedInfo(var o) native
      "return o['__MessageTraverser__attached_info__'];";
}

/** A visitor that recursively copies a message. */
class Copier extends MessageTraverser {
  Copier() : super();

  visitPrimitive(x) => x;

  List visitList(List list) {
    List copy = _getInfo(list);
    if (copy !== null) return copy;

    int len = list.length;

    // TODO(floitsch): we loose the generic type of the List.
    copy = new List(len);
    _attachInfo(list, copy);
    for (int i = 0; i < len; i++) {
      copy[i] = _dispatch(list[i]);
    }
    return copy;
  }

  Map visitMap(Map map) {
    Map copy = _getInfo(map);
    if (copy !== null) return copy;

    // TODO(floitsch): we loose the generic type of the map.
    copy = new Map();
    _attachInfo(map, copy);
    map.forEach((key, val) {
      copy[_dispatch(key)] = _dispatch(val);
    });
    return copy;
  }

  SendPort visitSendPort(SendPortImpl port) {
    return new SendPortImpl(port._workerId,
                            port._isolateId,
                            port._receivePortId);
  }

  SendPort visitReceivePort(ReceivePortImpl port) {
    return port._toNewSendPort();
  }

  SendPort visitReceivePortSingleShot(ReceivePortSingleShotImpl port) {
    return port._toNewSendPort();
  }
}

/** Visitor that serializes a message as a JSON array. */
class Serializer extends MessageTraverser {
  Serializer() : super();

  visitPrimitive(x) => x;

  visitList(List list) {
    int copyId = _getInfo(list);
    if (copyId !== null) return ['ref', copyId];

    int id = _nextFreeRefId++;
    _attachInfo(list, id);
    var jsArray = _serializeList(list);
    // TODO(floitsch): we are losing the generic type.
    return ['list', id, jsArray];
  }

  visitMap(Map map) {
    int copyId = _getInfo(map);
    if (copyId !== null) return ['ref', copyId];

    int id = _nextFreeRefId++;
    _attachInfo(map, id);
    var keys = _serializeList(map.getKeys());
    var values = _serializeList(map.getValues());
    // TODO(floitsch): we are losing the generic type.
    return ['map', id, keys, values];
  }

  visitSendPort(SendPortImpl port) {
    return ['sendport', port._workerId, port._isolateId, port._receivePortId];
  }

  visitReceivePort(ReceivePortImpl port) {
    return visitSendPort(port.toSendPort());;
  }

  visitReceivePortSingleShot(ReceivePortSingleShotImpl port) {
    return visitSendPort(port.toSendPort());
  }

  _serializeList(List list) {
    int len = list.length;
    var result = new List(len);
    for (int i = 0; i < len; i++) {
      result[i] = _dispatch(list[i]);
    }
    return result;
  }

  int _nextFreeRefId = 0;
}

/** Deserializes arrays created with [Serializer]. */
class Deserializer {
  Deserializer();

  static bool isPrimitive(x) {
    return (x === null) || (x is String) || (x is num) || (x is bool);
  }

  deserialize(x) {
    if (isPrimitive(x)) return x;
    // TODO(floitsch): this should be new HashMap<int, var|Dynamic>()
    _deserialized = new HashMap();
    return _deserializeHelper(x);
  }

  _deserializeHelper(x) {
    if (isPrimitive(x)) return x;
    assert(x is List);
    switch (x[0]) {
      case 'ref': return _deserializeRef(x);
      case 'list': return _deserializeList(x);
      case 'map': return _deserializeMap(x);
      case 'sendport': return _deserializeSendPort(x);
      // TODO(floitsch): Use real exception (which one?).
      default: throw "Unexpected serialized object";
    }
  }

  _deserializeRef(List x) {
    int id = x[1];
    var result = _deserialized[id];
    assert(result !== null);
    return result;
  }

  List _deserializeList(List x) {
    int id = x[1];
    // We rely on the fact that Dart-lists are directly mapped to Js-arrays.
    List dartList = x[2];
    _deserialized[id] = dartList;
    int len = dartList.length;
    for (int i = 0; i < len; i++) {
      dartList[i] = _deserializeHelper(dartList[i]);
    }
    return dartList;
  }

  Map _deserializeMap(List x) {
    Map result = new Map();
    int id = x[1];
    _deserialized[id] = result;
    List keys = x[2];
    List values = x[3];
    int len = keys.length;
    assert(len == values.length);
    for (int i = 0; i < len; i++) {
      var key = _deserializeHelper(keys[i]);
      var value = _deserializeHelper(values[i]);
      result[key] = value;
    }
    return result;
  }

  SendPort _deserializeSendPort(List x) {
    int workerId = x[1];
    int isolateId = x[2];
    int receivePortId = x[3];
    return new SendPortImpl(workerId, isolateId, receivePortId);
  }

  // TODO(floitsch): this should by Map<int, var> or Map<int, Dynamic>.
  Map<int, Dynamic> _deserialized;
}
