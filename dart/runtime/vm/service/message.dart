// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of vmservice;

class Message {
  final Completer _completer = new Completer.sync();
  bool get completed => _completer.isCompleted;
  /// Future of response.
  Future<String> get response => _completer.future;

  // Client-side identifier for this message.
  final serial;

  // In new messages.
  final String method;

  // In old messages.
  final List path = new List();

  final Map params = new Map();

  void _setPath(List<String> pathSegments) {
    if (pathSegments == null) {
      return;
    }
    pathSegments.forEach((String segment) {
      if (segment == null || segment == '') {
        return;
      }
      path.add(segment);
    });
  }

  Message.fromJsonRpc(Map map)
      : serial = map['id'], method = map['method'] {
    params.addAll(map['params']);
  }

  static String _methodNameFromUri(Uri uri) {
    if (uri == null) {
      return '';
    }
    if (uri.pathSegments.length == 0) {
      return '';
    }
    return uri.pathSegments[0];
  }

  Message.fromUri(Uri uri)
      : method = _methodNameFromUri(uri) {
    params.addAll(uri.queryParameters);
  }

  Message.forIsolate(Uri uri, RunningIsolate isolate)
      : method = _methodNameFromUri(uri) {
    params.addAll(uri.queryParameters);
    params['isolateId'] = isolate.serviceId;
  }

  Uri toUri() {
    return new Uri(path: method, queryParameters: params);
  }

  dynamic toJson() {
    return {
      'path': path,
      'params': params
    };
  }

  // Calls toString on all non-String elements of [list]. We do this so all
  // elements in the list are strings, making consumption by C++ simpler.
  // This has a side effect that boolean literal values like true become 'true'
  // and thus indistinguishable from the string literal 'true'.
  List _makeAllString(List list) {
    if (list == null) {
      return null;
    }
    for (var i = 0; i < list.length; i++) {
      if (list[i] is String) {
        continue;
      }
      list[i] = list[i].toString();
    }
    return list;
  }

  Future<String> send(SendPort sendPort) {
    final receivePort = new RawReceivePort();
    receivePort.handler = (value) {
      receivePort.close();
      assert(value is String);
      _completer.complete(value);
    };
    var keys = _makeAllString(params.keys.toList(growable:false));
    var values = _makeAllString(params.values.toList(growable:false));
    var request = new List(6)
        ..[0] = 0  // Make room for OOB message type.
        ..[1] = receivePort.sendPort
        ..[2] = serial.toString()
        ..[3] = method
        ..[4] = keys
        ..[5] = values;
    if (!sendIsolateServiceMessage(sendPort, request)) {
      _completer.complete(JSON.encode({
          'type': 'ServiceError',
          'id': '',
          'kind': 'InternalError',
          'message': 'could not send message [${serial}] to isolate',
      }));
    }
    return _completer.future;
  }

  Future<String> sendToVM() {
    final receivePort = new RawReceivePort();
    receivePort.handler = (value) {
      receivePort.close();
      assert(value is String);
      _completer.complete(value);
    };
    var keys = _makeAllString(params.keys.toList(growable:false));
    var values = _makeAllString(params.values.toList(growable:false));
    var request = new List(6)
        ..[0] = 0  // Make room for OOB message type.
        ..[1] = receivePort.sendPort
        ..[2] = serial.toString()
        ..[3] = method
        ..[4] = keys
        ..[5] = values;
    sendRootServiceMessage(request);
    return _completer.future;
  }

  void setResponse(String response) {
    _completer.complete(response);
  }

  void setErrorResponse(String message) {
    var response = {
      'id': serial,
      'result' : {
        'type': 'Error',
        'message': message,
        'request': {
          'method': method,
          'params': params
        }
      }
    };
    _completer.complete(JSON.encode(response));
  }
}

bool sendIsolateServiceMessage(SendPort sp, List m)
    native "VMService_SendIsolateServiceMessage";

void sendRootServiceMessage(List m)
    native "VMService_SendRootServiceMessage";
