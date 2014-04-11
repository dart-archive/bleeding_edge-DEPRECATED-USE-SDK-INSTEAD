// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library coverage_list;

import 'dart:html';
import 'package:polymer/polymer.dart';

import 'coverage_reader.dart' as cov;
import 'coverage_model.dart';
import 'path_input.dart';

/**
 * Polymer element that controls the main UI in the browser.
 */
@CustomTag('cov-list')
class CoverageList extends PolymerElement {
  @published
  String modelId;

  @observable
  CoverageModel model;

  factory CoverageList() => new Element.tag('cov-list');
  CoverageList.created(): super.created();

  PathInput get _newPath => $['new-path'];

  void modelIdChanged() {
    model = document.querySelector('#$modelId');
  }

  void addFileAction() {
    // Typical coverage data files are about a megabyte of XML.
    // Displaying two or three is really slow, but parsing isn't the problem.
    if (_newPath.files.isNotEmpty) {
      model.clear();
      for (var file in _newPath.files) {
        var reader = new FileReader();
        reader.onError.listen(_handleError);
        reader.onLoad.listen(_handleLoad);
        reader.readAsText(file);
      }
    }
  }

  void exportDataAction() {
    if (model.items == null || model.items.isEmpty) {
      return;
    }
    StringBuffer buffer = new StringBuffer();
    for (var data in model.items) {
      buffer
          ..write(data.className)
          ..write(',')
          ..write(data.percentCovered)
          ..writeln();
    }
    InputElement text = document.querySelector('#output');
    text.setRangeText(buffer.toString());
  }

  void _handleError(evt) => print("error $evt");

  void _handleLoad(evt) {
    String xml = evt.target.result;
    var map = cov.processXml(xml);
    model.addMap(map);
  }
}
