// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'gl_driver.dart';
// TODO(vsm): Make this a package URL.
import 'dart:html';
import '../src/openglui_raytrace.dart';

main() {
  wrapVertexArray = true;
  glMain(setup, resize);
}

