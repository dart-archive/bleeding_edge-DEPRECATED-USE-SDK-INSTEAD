// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library sample_asynchronous_extension;

import 'dart:async';
import 'dart:isolate';
import 'dart-ext:sample_extension';

// A class caches the native port used to call an asynchronous extension.
class RandomArray {
  static SendPort _port;

  Future<List<int>> randomArray(int seed, int length) {
    var args = new List(2);
    args[0] = seed;
    args[1] = length;
    ReceivePort receivePort = new ReceivePort();
    _servicePort.send(args, receivePort.sendPort);
    return receivePort.first.then((result) {
      receivePort.close();
      if (result != null) {
        return result;
      } else {
        throw new Exception("Random array creation failed");
      }
    });
  }

  SendPort get _servicePort {
    if (_port == null) {
      _port = _newServicePort();
    }
    return _port;
  }

  SendPort _newServicePort() native "RandomArray_ServicePort";
}
