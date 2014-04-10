// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library coverage_item;

import 'dart:html';
import 'package:polymer/polymer.dart';

import 'coverage_data.dart';

/**
 * Polymer element wrapping a single [CoverageData].
 */
@CustomTag('cov-item')
class CoverageItem extends LIElement with Polymer, Observable {
  @published
  CoverageData data;

  factory CoverageItem() => new Element.tag('li', 'cov-item');
  CoverageItem.created(): super.created() {
    polymerCreated();
  }
}
