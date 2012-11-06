// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library sample_asynchronous_extension;

import "dart-ext:sample_extension";

// A class caches the native port used to call an asynchronous extension.
class RandomArray {
  static SendPort _port;

  void randomArray(int seed, int length, void callback(List result)) {
    var args = new List(2);
    args[0] = seed;
    args[1] = length;
    _servicePort.call(args).then((result) {
      if (result != null) {
        callback(result);
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
