#!/usr/bin/env dart
// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:io';
import 'package:polymer/component_build.dart';
import 'package:polymer/deploy.dart' as deploy;

void main() {
  build(new Options().arguments, ['web/index.html']).then((_) => deploy.main());
}
