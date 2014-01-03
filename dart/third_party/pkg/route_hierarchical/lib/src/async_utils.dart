// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library route.async_utils;

import 'dart:async';

Future doWhile(Iterable iterable, Future<bool> action(i)) =>
    _doWhile(iterable.iterator, action);

Future _doWhile(Iterator iterator, Future<bool> action(i)) =>
  (iterator.moveNext())
      ? action(iterator.current).then((bool result) =>
        (result)
            ? _doWhile(iterator, action)
            : new Future.value(false))
      : new Future.value(false);
