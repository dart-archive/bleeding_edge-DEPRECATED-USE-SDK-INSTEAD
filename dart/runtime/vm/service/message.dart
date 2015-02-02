// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of vmservice;

class Message {
  final Completer _completer = new Completer.sync();
  bool get completed => _completer.isCompleted;
  /// Future of response.
  Future<String> get response => _completer.future;

  final bool isOld;

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

  Message.fromUri(Uri uri) : isOld = true {
    var split = uri.path.split('/');
    if (split.length == 0) {
      setErrorResponse('Invalid uri: $uri.');
      return;
    }
    _setPath(split);
    params.addAll(uri.queryParameters);
  }

  Message.fromJsonRpc(this.method, Map rpcParams)
      : isOld = false {
    params.addAll(rpcParams);
  }

  Message.fromMap(Map map) : isOld = true {
    _setPath(map['path']);
    // TODO - turnidge - change this to params in sender.
    if (map['options'] != null) {
      params.addAll(map['options']);
    }
  }

  dynamic toJson() {
    return {
      'path': path,
      'params': params
    };
  }

  Future<String> send(SendPort sendPort) {
    final receivePort = new RawReceivePort();
    receivePort.handler = (value) {
      receivePort.close();
      if (value is Exception) {
        _completer.completeError(value);
      } else {
        _completer.complete(value);
      }
    };
    var keys = params.keys.toList(growable:false);
    var values = params.values.toList(growable:false);
    var request = new List(5)
        ..[0] = 0  // Make room for OOB message type.
        ..[1] = receivePort.sendPort
        ..[2] = (isOld ? path : method)
        ..[3] = keys
        ..[4] = values;
    sendIsolateServiceMessage(sendPort, request);
    return _completer.future;
  }

  Future<String> sendToVM() {
    final receivePort = new RawReceivePort();
    receivePort.handler = (value) {
      receivePort.close();
      if (value is Exception) {
        _completer.completeError(value);
      } else {
        _completer.complete(value);
      }
    };
    var keys = params.keys.toList(growable:false);
    var values = params.values.toList(growable:false);
    var request = new List(5)
        ..[0] = 0  // Make room for OOB message type.
        ..[1] = receivePort.sendPort
        ..[2] = (isOld ? path : method)
        ..[3] = keys
        ..[4] = values;
    sendRootServiceMessage(request);
    return _completer.future;
  }

  void setResponse(String response) {
    _completer.complete(response);
  }

  void setErrorResponse(String error) {
    _completer.complete(JSON.encode({
        'type': 'ServiceError',
        'id': '',
        'kind': 'RequestError',
        'message': error,
        'path': path,
        'params': params
    }));
  }
}

void sendIsolateServiceMessage(SendPort sp, List m)
    native "VMService_SendIsolateServiceMessage";

void sendRootServiceMessage(List m)
    native "VMService_SendRootServiceMessage";
