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
  @published
  int overallPercentage;
  Map<String, CoverageData> allItemsMap = new HashMap<String, CoverageData>();
  int filesToAdd;
  int filesAdded;

  factory CoverageModel() => new Element.tag('cov-model');
  CoverageModel.created(): super.created();

  void ready() {
    async((_) {
      if (items == null) {
        items = new ObservableList<CoverageData>();
        overallPercentage = 0;
      }
    });
  }

  void clear() {
    items.clear();
    allItemsMap.clear();
    filesToAdd = 0;
    filesAdded = 0;
  }

  /// Add data from [map] to the model.
  void addData(Map map) {
    if (filesToAdd == 0) {
      throw new Exception("Need to set file counter");
    }
    cov.merge(allItemsMap, map);
    if (++filesAdded == filesToAdd) {
      _refresh();
    }
  }

  void _refresh() {
    items.clear();
    var names = new List.from(allItemsMap.keys);
    names.sort();
    for (var name in names) {
      items.add(allItemsMap[name]);
    }
    _updatePercentage();
  }

  void _updatePercentage() {
    int n = 0;
    for (var map in allItemsMap.values) {
      n += map.percentCovered;
    }
    overallPercentage = n == 0 ? 0 : (n / allItemsMap.length).round();
  }
}
