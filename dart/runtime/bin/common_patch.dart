// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

patch class _IOCrypto {
  /* patch */ static Uint8List getRandomBytes(int count)
      native "Crypto_GetRandomBytes";
}


// Provide a closure which will allocate a Timer object to be able to hook
// up the Timer interface in dart:isolate with the implementation here.
_getTimerFactoryClosure() {
  runTimerClosure = _Timer._handleTimeout;
  return _Timer._factory;
}
