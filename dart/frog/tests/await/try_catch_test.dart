// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Await within a try-catch block, no error occurs.

#import("await_test_helper.dart");

noerror() {
  try {
    final t = await futureOf(0);
    return t;
  } catch (e) {
    return -1;
  }
}

main() {
  int t = await noerror();
  Expect.equals(0, t);
}

// The transformation is roughly as follows:
// noerror() {
//   final _ret = new Completer();
//   _catch(Ex1 ex) {
//     _ret.complete(-1);
//   }
//   try {
//     final f = futureOf(0);
//     f.then((t) {
//       _ret.complete(t);
//     });
//     f.addExceptionHandler((ex) {
//       if (ex is Ex1) {
//         _catch(ex);
//         return true;
//       }
//       return false;
//     });
//     Futures.propagateError(f, _ret);
//     return _ret;
//   } catch(Ex1 ex) {
//      _catch(ex);
//      return _ret;
//   }
// }
