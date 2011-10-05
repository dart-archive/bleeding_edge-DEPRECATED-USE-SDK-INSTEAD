// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * A graphical picker for a set of predefined values.
 */
class ValuePicker extends Picker {

  int _height;
  int _margin;
  List<String> _values;

  ValuePicker(DivElement root, this._values) : super("value-picker") {
    root.classes = ["value-picker", "fadeOut"];

    Document doc = root.document;
    Element _ul = doc.createElement("ul");
    _ul.attributes["class"] = "value-picker-ul";
    root.nodes.add(_ul);

    // Use the <ul> as the parent node
    setParent(_ul);

    int idx = 0;
    for (int i = 0; i < _values.length; i++) {
      LIElement li = doc.createElement("li");
      li.classes = ["value-picker-item", "value-picker-item-enabled"];
      li.id = "value-picker-${i}";
      li.text = _values[i];
      li.on.click.add(_clickHandler);
      _ul.nodes.add(li);
    }
  }
}
