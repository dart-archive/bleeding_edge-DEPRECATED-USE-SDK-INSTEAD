#!/usr/bin/env dart
// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library build;

import 'tools/create_bootstrap.dart' as createBootstrap;
import 'dart:io';

void main() {
  final options = new Options();
  final scriptPath = new Path(options.script).directoryPath;
  final libPath = scriptPath.append('lib');

  final changedOpt = "--changed=${libPath.append('js.dart')}";
  for (String arg in new Options().arguments) {
    if (arg == changedOpt) {
      createBootstrap.create(libPath);
    }
  }
}
