// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library path_input;

import 'dart:html';
import 'package:polymer/polymer.dart';

/**
 * Polymer element for selecting input files.
 */
@CustomTag('path-input')
class PathInput extends InputElement with Polymer, Observable {

  factory PathInput() => new Element.tag('input', 'path-input');
  PathInput.created(): super.created() {
    polymerCreated();
  }
}
