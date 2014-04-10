// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library coverage_model;

import 'dart:html';
import 'dart:collection';
import 'package:polymer/polymer.dart';

import 'coverage_reader.dart' as cov;
import 'coverage_data.dart';

/**
 *  Polymer element representing the coverage data model.
 */
@CustomTag('cov-model')
class CoverageModel extends PolymerElement {
  @published
  ObservableList<CoverageData> items;
  Map<String, CoverageData> allItemsMap = new HashMap<String, CoverageData>();

  factory CoverageModel() => new Element.tag('cov-model');
  CoverageModel.created(): super.created();

  void ready() {
    async((_) {
      if (items == null) items = new ObservableList<CoverageData>();
    });
  }

  void clear() {
    items.clear();
  }

  /// Add data from [map] to the model.
  void addMap(Map map) {
    items.clear();
    cov.merge(allItemsMap, map);
    var names = new List.from(allItemsMap.keys);
    names.sort();
    for (var name in names) {
      items.add(allItemsMap[name]);
    }
  }
}
