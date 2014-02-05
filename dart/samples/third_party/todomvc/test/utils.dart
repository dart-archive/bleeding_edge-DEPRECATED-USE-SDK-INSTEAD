// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.test.utils;

import 'dart:async';
import 'package:observe/observe.dart';

Future onPropertyInit(Observable obj, String path) {
  final obs = new PathObserver(obj, path);
  if (obs.value != null) return new Future.value(obs.value);

  final c = new Completer();
  obs.open((value) {
    if (value != null) {
      obs.close();
      c.complete(value);
    }
  });
  return c.future;
}
