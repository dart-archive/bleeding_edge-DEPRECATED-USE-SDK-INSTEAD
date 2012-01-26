// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
/**
 * A graphical picker for a set of predefined colors.
 */
class ColorPicker extends Picker {

  List<String> _colors;
  int _columns;
  int _margin;
  int _rows;
  int _width;

  ColorPicker(Element root, this._colors, this._rows, this._columns, this._width, this._margin)
      : super("color-picker") {
    if (_colors.length < _rows * _columns) {
      throw new RuntimeException("colors.length < rows * columns");
    }

    setParent(root);

    int w = _columns * (_width + _margin) + _margin;
    int h = _rows * (_width + _margin) + _margin;

    root.attributes["class"] = "color-picker";
    root.style.setProperty("width", HtmlUtils.toPx(w));
    root.style.setProperty("height", HtmlUtils.toPx(h));
    root.classes.add("fadeOut");

    Document doc = root.document;

    int idx = 0;
    for (int j = 0; j < _rows; j++) {
      int y = _margin + j * (_width + _margin);
      for (int i = 0; i < _columns; i++) {
        int x = _margin + i * (_width + _margin);

        DivElement div = new Element.tag("div");
        div.attributes["class"] = "color-picker-item";
        div.id = "color-picker-${idx}";
        div.style.setProperty("position", "absolute");
        div.style.setProperty("left", HtmlUtils.toPx(x));
        div.style.setProperty("top", HtmlUtils.toPx(y));
        div.style.setProperty("width", HtmlUtils.toPx(_width));
        div.style.setProperty("height", HtmlUtils.toPx(_width));
        if (idx == 0) {
          div.style.setProperty("background", "url(/img/inner-menu-bg.png)");
        } else {
          div.style.setProperty("background-color", _colors[idx]);
        }
        div.on.click.add(_clickHandler);

        root.nodes.add(div);
        idx++;
      }
    }
  }
}
