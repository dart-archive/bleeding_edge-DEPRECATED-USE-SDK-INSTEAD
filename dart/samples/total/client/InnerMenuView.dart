// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO: RE: implements RequestAnimationFrameCallback. File bug
// against dom libs because it should be possible to pass a function
// to webkitRequestAnimationFrame just like addEventListener.
class InnerMenuView {
  static final List<String> _textAlignmentClassNames = const <String>[ "l", "c", "r" ];
  static final List<int> _textAlignmentValues = const <int>[
    Style.LEFT, Style.CENTER, Style.RIGHT
  ];
  static final List<String> _textStyleClassNames = const <String>[ "b", "i", "u", "s" ];
  static final List<int> _textStyleValues = const <int>[
    Style.BOLD, Style.ITALIC, Style.UNDERLINE, Style.STRIKETHROUGH
  ];

  // Return the height of the menu
  static int getInnerMenuHeight() => CssStyles.INNER_MENU_HEIGHT;

  static DivElement _addButton(Element parent,
      String classOfButton,
      String classOfDiv,
      void clickedFunc(DivElement div)) {
    Document document = parent.document;
    ButtonElement button = new Element.tag("button");
    button.attributes["class"] = classOfButton;
    Element div = new Element.tag("div");
    div.attributes["class"] = classOfDiv;
    button.nodes.add(div);
    parent.nodes.add(button);

    // Don't allow click to bubble to the button container
    button.on.click.add((Event e) {
      e.cancelBubble = true;
      clickedFunc(div);
    });
    return div;
  }

  // TODO: HACK. When we expand the border on our table cells,
  // the sizes for the table will be contradictory since the row is
  // set to a height that is smaller than the offsetHeight of row. This
  // will cause the cells to become smaller (visual jitter). To work around
  // we set the height of the cell explicitly. Sadly this requires us
  // to use the computed style to determine the current padding of the
  // cell.
  static void _pinHeight(Window window, TableRowElement row) {
    Element firstCell = row.cells[0];
    final firstCellRect = firstCell.rect;
    final style = firstCell.computedStyle;
    window.requestLayoutFrame(() {
      int height = firstCellRect.value.client.height
          - HtmlUtils.fromPx(style.value.getPropertyValue('padding-top'))
          - HtmlUtils.fromPx(style.value.getPropertyValue('padding-bottom'));
      firstCell.style.setProperty('height', HtmlUtils.toPx(height));
    });
  }

  // Reverses the damage done by _pinHeight.
  static void _unpinHeight(TableRowElement row) {
    Element firstCell = row.cells[0];
    firstCell.style.removeProperty('height');
  }

  ColorPicker _backgroundColorPicker;
  // The interior UI of the menu.
  DivElement _bar;
  Function _callToHide;
  int _currentRowHeight;
  int _initialRowHeight;
  Function _innerMenuMoved;
  ValuePicker _numericFormatPicker;
  List<Element> _pickerElts;

  // The row to which the menu is attached.
  TableRowElement _row;

  // Needed to update the selection to make sure it tracks during animations.
  SelectionManager _selectionManager;
  List<Element> _textAlignmentButtons;
  ColorPicker _textColorPicker;
  List<Element> _textStyleButtons;
  bool _transitionDidComplete;

  // Needed to access webkitRequestAnimationFrame and getComputedStyle.
  Window _window;

  int get currentRowHeight() => _currentRowHeight;

  TableRowElement get row() => _row;

  /**
   * Constructs and shows an InnerMenu. Use hide() to make it disappear
   * It will automatically hide if the row becomes detached from its table
   * (e.g. due to scrolling).
   */
  InnerMenuView(this._window, this._row, this._selectionManager, Style style, int initialHeight,
      this._innerMenuMoved, this._callToHide) {
    // Ensure statics are initialized
    Formats formats = new Formats();

    _row.classes.add('with-inner-menu');
    _pinHeight(_window, _row);

    Document document = _row.document;
    _bar = new Element.tag("div");
    _bar.classes.add("inner-menu");

    // Close the menu when the non-button area is clicked
    // We use the supplied _callToHide function to initiate the hiding
    Element buttons = new Element.tag("div");
    buttons.classes.add("inner-menu-buttons");
    buttons.on.click.add((Event e) { _callToHide(); });

    _textStyleButtons = new List<Element>(4);
    _textStyleButtons[0] = _addButton(buttons, "inner-menu-button", "b", _textStyle);
    _textStyleButtons[1] = _addButton(buttons, "inner-menu-button", "i", _textStyle);
    _textStyleButtons[2] = _addButton(buttons, "inner-menu-button", "u", _textStyle);
    _textStyleButtons[3] = _addButton(buttons, "inner-menu-button", "s", _textStyle);

    _textAlignmentButtons = new List<Element>(3);
    _textAlignmentButtons[0] = _addButton(buttons, "inner-menu-button", "l", _textAlign);
    _textAlignmentButtons[1] = _addButton(buttons, "inner-menu-button", "c", _textAlign);
    _textAlignmentButtons[2] = _addButton(buttons, "inner-menu-button", "r", _textAlign);

    _textColorPicker = _createColorPicker(buttons, "textColor", "t",
      Style _(Style s, int selectedIndex) => s.setTextColor(selectedIndex));

    _backgroundColorPicker = _createColorPicker(buttons, "textColor", "k",
      Style _(Style s, int selectedIndex) => s.setBackgroundColor(selectedIndex));

    _numericFormatPicker = _createValuePicker(buttons, "numericFormat", "n",
      formats.numericFormatDescriptions,
      Style _(Style s, int selectedIndex) => s.setNumericFormatByIndex(selectedIndex));

    // Set the style dropdowns
    updateStyleUI(style);

    _bar.nodes.add(buttons);
    // Attach the menu bar to the <table> node
    _row.parent.parent.nodes.add(_bar);

    // Cancelling bubbles on mouseup and mousedown allows the user
    // to click on the inner menu without triggering any selection
    // logic on the table.
    _bar.on.mouseDown.add((MouseEvent e) {
      e.cancelBubble = true;
    });
    _bar.on.mouseUp.add((MouseEvent e) {
      e.cancelBubble = true;
    });

    // As a shortcut, capture the row height before we start changing
    // the borders so we know how far to offset the _bar.
    // FIXME this approach sometimes grabs a bad value -- hardcode for now
    _initialRowHeight = initialHeight; // _row.getBoundingClientRect().height;
    _currentRowHeight = _initialRowHeight;

    EventListener f = myfunc(Event event) {
      _transitionDidComplete = true;
      _row.on.transitionEnd.remove(myfunc);
      _bar.style.setProperty("overflow", "visible");
    };
    _row.on.transitionEnd.add(f);
    _window.webkitRequestAnimationFrame((int time) {
      _onRequestAnimationFrame(time);
    });

    _row.on['DOMNodeRemoved'].add(void g(Event event) {
      if (_row == event.target) {
        _finishHide();
        _row.on['DOMNodeRemoved'].remove(g);
      }
    });

    // Initialize boolean
    _transitionDidComplete = false;
  }

  // Hides the InnerMenu
  void hide(void callAtEnd()) {
    _hideAllPickers();

    _bar.style.setProperty("overflow", "hidden");
    _transitionDidComplete = false;
    _row.classes.remove('with-inner-menu');
    if (_row.classes.isEmpty()) {
      _row.attributes.remove("class");
    }
    EventListener f;
    f = (Event event) {
      _finishHide();
      _row.on.transitionEnd.remove(f);
      if (callAtEnd != null) {
        callAtEnd();
      }
    };
    _row.on.transitionEnd.add(f);
    _window.webkitRequestAnimationFrame((int time) {
      _onRequestAnimationFrame(time);
    });
  }

  bool isAttachedTo(TableRowElement row) => _row == row;

  void updateSize() {
    if (_row.parent == null) {
      // The row we were attached to has disappeared (e.g., by scrolling), so clean up the menu bar
      _bar.remove();
      return;
    }

    // Must take into account the top of the table due to scrolling.
    final offsetParentRect = _row.offsetParent.rect;
    final rowRect = _row.rect;
    window.requestLayoutFrame(() {
      int tableTop = offsetParentRect.value.bounding.top.toInt();

      // Get the current bounding box of the row we're attached to.
      ClientRect boundingRowRect = rowRect.value.bounding;
      CSSStyleDeclaration style = _bar.style;

      int top = boundingRowRect.top.toInt() + _initialRowHeight - tableTop;
      int height = boundingRowRect.height.toInt() - _initialRowHeight;

      _currentRowHeight = height;

      style.setProperty("top", HtmlUtils.toPx(top));
      style.setProperty("height", HtmlUtils.toPx(height));

      _innerMenuMoved();
    });
  }

  // Update the style buttons for the selected style.
  void updateStyleUI(Style style) {
    // Text format buttons
    for (int i = 0; i < 4; i++) {
      int mask = _textStyleValues[i];
      _selectTextStyle(i, _textStyleClassNames[i], (style.textFormatIndex & mask) != 0);
    }

    for (int i = 0; i < 3; i++) {
      Element element = _textAlignmentButtons[i];
      if (style.textAlignment == _textAlignmentValues[i]) {
        element.attributes["class"] = "${_textAlignmentClassNames[i]}-selected";
        element.parent.style.setProperty("border", "1px solid black");
      } else {
        element.attributes["class"] = _textAlignmentClassNames[i];
        element.parent.style.removeProperty("border");
      }
    }

    _textColorPicker.selectedIndex = style.textColor;
    _backgroundColorPicker.selectedIndex = style.backgroundColor;
    _numericFormatPicker.selectedIndex = style.numericFormatIndex;
  }

  ColorPicker _createColorPicker(Element buttons, String name, String className,
      SetStyleFunction action) {
    DivElement div = new Element.tag("div");
    div.id = "${name}Div";

    ColorPicker picker = new ColorPicker(div, Formats.htmlColors, 8, 6, 18, 3);
    picker.addListener((int index) {
      _showHide(div, false);
      _execute(action, index);
    });

    DivElement button = _addButton(buttons, "inner-menu-button", className,
      (DivElement elt) {
        bool show = div.classes.contains("fadeOut");
        _showHide(div, show);
      });
    button.nodes.add(div);

    if (_pickerElts == null) {
      _pickerElts = new List<Element>();
    }
    _pickerElts.add(div);

    return picker;
  }

  ValuePicker _createValuePicker(Element buttons, String name, String className,
      List<String> values, SetStyleFunction action) {
    DivElement div = new Element.tag("div");
    div.id = "${name}Div";

    DivElement button = _addButton(buttons, "inner-menu-button", "n",
      (DivElement elt) {
        bool show = div.classes.contains("fadeOut");
        _showHide(div, show);
    });
    button.nodes.add(div);

    ValuePicker picker = new ValuePicker(div, values);
    picker.addListener((int index) {
      _showHide(div, false);
      _execute(action, index);
    });

    if (_pickerElts == null) {
      _pickerElts = new List<Element>();
    }
    _pickerElts.add(div);

    return picker;
  }

  void _execute(SetStyleFunction action, int index) {
    Spreadsheet spreadsheet = _selectionManager.spreadsheet;
    spreadsheet.clearDirtyCells();
    CellRange range = _selectionManager.getSelectionRange();
    spreadsheet.execute(new SetStyleCommand(range, index, action));
    spreadsheet.refresh();
    // Allow the styled cell contents to show
    // _presenter.hideFormula();
  }

  void _finishHide() {
    _transitionDidComplete = true;
    _unpinHeight(_row);
    if (_bar.parent != null) {
      _bar.remove();
    }
  }

  void _hideAllPickers() {
    // Show 'null' will hide all others
    _showHide(null, true);
  }

  // During CSS transitions, we use a webkitRequestAnimationFrame based
  // animation loop to keep visual state in sync.
  void _onRequestAnimationFrame(int time) {
    updateSize();

    _selectionManager.updateSelection();

    if (!_transitionDidComplete) {
      _window.webkitRequestAnimationFrame((int time_) {
        _onRequestAnimationFrame(time_);
      });
    }
  }

  void _selectTextStyle(int index, String className, bool selected) {
    Element element = _textStyleButtons[index];
    if (selected) {
      element.parent.style.setProperty("border", "1px solid black");
      className = "${className}-selected";
    } else {
      element.parent.style.removeProperty("border");
    }
    element.attributes["class"] = className;
  }

  void _showHide(Element e, bool show) {
    if (show) {
      if (e != null) {
        e.classes.remove("fadeOut");
        e.classes.add("fadeIn");
      }
      // Hide other pickers
      _pickerElts.forEach((DivElement elt) {
        if (elt !== e) {
          elt.classes.remove("fadeIn");
          elt.classes.add("fadeOut");
        }
      });
    } else {
      e.classes.remove("fadeIn");
      e.classes.add("fadeOut");
    }
  }

  void _textAlign(DivElement div) {
    _hideAllPickers();

    bool selected = div.attributes["class"].length > 1;
    String className = div.attributes["class"].substring(0, 1);
    int align = 0;
    for (int i = 0; i < 3; i++) {
      if (!selected && className == _textAlignmentClassNames[i]) {
        align = _textAlignmentValues[i];
        div.attributes["class"] = "${className}-selected";
        div.parent.style.setProperty("border", "1px solid black");
      } else {
        _textAlignmentButtons[i].attributes["class"] = _textAlignmentClassNames[i];
        _textAlignmentButtons[i].parent.style.removeProperty("border");
      }
    }

    _execute(Style _(Style s, int alignment) => s.setTextAlignment(alignment), align);
  }

  void _textStyle(DivElement div) {
    _hideAllPickers();

    _selectionManager.spreadsheet.clearDirtyCells();
    CellRange range = _selectionManager.getSelectionRange();

    // Toggle "-selected" class name suffix
    String className = div.attributes["class"];
    if (className.length == 1) {
      div.attributes["class"] = "${className}-selected";
      div.parent.style.setProperty("border", "1px solid black");
    } else {
      div.attributes["class"] = className.substring(0, 1);
      div.parent.style.removeProperty("border");
    }

    int textStyle = 0;
    for (int i = 0; i < 4; i++) {
      if (_textStyleButtons[i].attributes["class"].length > 1) {
        textStyle += _textStyleValues[i];
      }
    }

    _execute(Style _(Style s, int selectedIndex)
        => s.setTextFormatByIndex(selectedIndex), textStyle);
  }
}
