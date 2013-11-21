// Copyright 2013 The Polymer Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

library todomvc.web.lib_elements.polymer_selector;

import 'dart:html';
import 'package:polymer/polymer.dart';
import 'package:template_binding/template_binding.dart' show
    nodeBind, isSemanticTemplate;
import 'polymer_selection.dart';

// TODO(jmesserly): get this from polymer_ui_elements package.
@CustomTag('polymer-selector')
class PolymerSelector extends PolymerElement {
  /**
   * Gets or sets the selected element.  Default is to use the index
   * of the currently selected element.
   *
   * If you want a specific attribute value of the selected element to be
   * used instead of index, set "valueattr" to that attribute name.
   *
   * Example:
   *
   *     <polymer-selector valueattr="label" selected="foo">
   *       <div label="foo"></div>
   *       <div label="bar"></div>
   *       <div label="zot"></div>
   *     </polymer-selector>
   */
  @published String selected;

  /** If true, multiple selections are allowed. */
  @published bool multi = false;

  /** Specifies the attribute to be used for "selected" attribute. */
  @published String valueattr = 'name';

  /** Specifies the CSS class to be used to add to the selected element. */
  @published String selectedClass = 'polymer-selected';

  /**
   * Specifies the property to be used to set on the selected element
   * to indicate its active state.
   */
  @published String selectedProperty = 'active';

  /** Returns the model associated with the selected element. */
  @published var selectedModel;

  @published bool notap = false;

  @observable var selectedItem;

  factory PolymerSelector() => new Element.tag('polymer-selector');
  PolymerSelector.created() : super.created();

  List<Node> get items => ($['items'] as ContentElement).getDistributedNodes()
      .where((n) => !isSemanticTemplate(n)).toList();

  get selection => ($['selection'] as PolymerSelection).getSelection();

  void selectedChanged() {
    valueToSelection(selected);
  }

  void valueToSelection(value) {
    var item = items.firstWhere((i) => valueForNode(i) == value,
        orElse: () => null);

    selectedItem = item;
    ($['selection'] as PolymerSelection).select(item);
    updateSelectedModel();
  }

  void updateSelectedModel() {
    if (selectedItem != null) {
      var t = nodeBind(selectedItem).templateInstance;
      selectedModel = t != null ? t.model : null;
    } else {
      selectedModel = null;
    }
  }

  String valueForNode(node) {
    // TODO(jmesserly): faster way to do this
    var value = new PathObserver(node, valueattr).value;
    return value != null ? value : node.attributes[valueattr];
  }

  // events fired from <polymer-selection> object
  void selectionSelect(e, PolymerSelectEventDetail detail) {
    if (detail.item != null) {
      if (selectedClass != null) {
        detail.item.classes.toggle(selectedClass, detail.isSelected);
      }
      if (selectedProperty != null) {
        new PathObserver(detail.item, selectedProperty).value =
            detail.isSelected;
      }
    }
  }

  // event fired from host
  void activateHandler(e) {
    if (!notap) {
      var i = findDistributedTarget(e.target, items);
      if (i >= 0) {
        var selected = valueForNode(items[i]);
        if (selected == null) selected = i;
        if (multi) {
          valueToSelection(selected);
        } else {
          this.selected = selected;
        }
        asyncFire('polymer-activate', detail:
            new PolymerActivateEventDetail(items[i]));
      }
    }
  }

  int findDistributedTarget(Node target, List<Node> nodes) {
    // find first ancestor of target (including itself) that
    // is in inNodes, if any
    while (target != null && target != this) {
      var i = nodes.indexOf(target);
      if (i >= 0) return i;
      target = target.parentNode;
    }
    return -1;
  }
}

class PolymerActivateEventDetail {
  final item;

  PolymerActivateEventDetail(this.item);
}
