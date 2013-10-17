// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library survey.web.selection;

import 'dart:html';

import 'package:polymer/polymer.dart';

/**
 * The SelectionElement view. A selection element can be used as a substitute
 * for radios and checkboxes. When passed with a [multi] set to true, this
 * element permits the user to select 0 or more options, otherwise it permits
 * the user to select 0 or 1 option.
 */
@CustomTag('selection-element')
class SelectionElement extends PolymerElement with Observable {
  bool get applyAuthorStyles => true;

  @observable List<String> values = toObservable([]);
  @observable List<int> selectedIndices = toObservable([]);
  @observable bool multi = false;

  inserted() {
    super.inserted();
    var root = getShadowRoot('selection-element');
    var items = root.queryAll('li');
    markSelected(items);
  }

  markSelected(items) {
    for (var i = 0; i < items.length; i++) {
      if (selectedIndices.contains(i)) {
        items[i].classes.add('selected');
      } else {
        items[i].classes.remove('selected');
      }
    }
  }

  makeSelection(Event e, var detail, Element sender) {
    e.preventDefault();
    var root = getShadowRoot('selection-element');
    var items = root.queryAll('li');
    var index = items.indexOf(sender);

    if (!multi && items.length > 1) {
      selectedIndices.clear();
      selectedIndices.add(index);
    } else {
      if (selectedIndices.contains(index)) {
        selectedIndices.remove(index);
      } else {
        selectedIndices.add(index);
      }
    }

    markSelected(items);
    dispatchSelectionResults();
  }

  dispatchSelectionResults() {
    List<String> results = [];
    for (var index in selectedIndices) {
      results.add(values[index]);
    }
    dispatchEvent(new CustomEvent('selectionmade', detail: results));
  }
}