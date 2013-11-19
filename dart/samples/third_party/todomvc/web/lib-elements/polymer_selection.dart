// Copyright 2013 The Polymer Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
library todomvc.web.lib_elements.polymer_selection;

import 'dart:html';
import 'package:polymer/polymer.dart';

@CustomTag('polymer-selection')
class PolymerSelection extends PolymerElement {
  @published bool multi = false;
  @observable final selection = new ObservableList();

  factory PolymerSelection() => new Element.tag('polymer-selection');
  PolymerSelection.created() : super.created();

  void ready() {
    clear();
  }

  void clear() {
    selection.clear();
  }

  getSelection() {
    if (multi) return selection;
    if (selection.isNotEmpty) return selection[0];
    return null;
  }

  bool isSelected(item) => selection.contains(item);

  void setItemSelected(item, bool isSelected) {
    if (item != null) {
      if (isSelected) {
        selection.add(item);
      } else {
        selection.remove(item);
      }
      // TODO(sjmiles): consider replacing with summary
      // notifications (asynchronous job)
      asyncFire("polymer-select", detail:
          new PolymerSelectEventDetail(isSelected, item));
    }
  }

  select(item) {
    if (multi) {
      toggle(item);
    } else if (getSelection() != item) {
      setItemSelected(getSelection(), false);
      setItemSelected(item, true);
    }
  }

  toggle(item) {
    setItemSelected(item, !isSelected(item));
  }
}

class PolymerSelectEventDetail {
  final bool isSelected;
  final item;

  PolymerSelectEventDetail(this.isSelected, this.item);
}
