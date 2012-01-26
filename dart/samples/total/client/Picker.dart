// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

typedef void PickerListener(int selectedIndex);

/**
 * Superclass for dropdown menus that allow and maintain a selection.
 * The picker consists of a parent [Element] and a list of children.
 * The parent is assigned a particular class name (say, [:my-picker:]).
 * The items will each be assigned the class name [:my-picker-item:] or
 * [:my-picker-item-selected:].  They may also have class names [:my-picker-item-enabled:]
 * and [:my-picker-item-disabled:].  Each child will be assigned an id [:my-picker-###:]
 * where [:###:] represents a sequence of integers starting with 0.
 *
 * One or more [PickerListener] functions may be supplied, which will be called with
 * the index of the selected item whenever the selection changes.
 */
class Picker {

  /**
   * A class name prefix used for CSS styling.
   */
  String _className;

  /**
   * A list of [PickerListener]s.
   */
  List<PickerListener> _listeners;

  /**
   * The parent [Element] for the items being selected.  The items must be
   * the (only) children of the element.
   */
  Element _parent;

  /**
   * The index of the currently selected item.
   */
  int _selectedIndex = 0;

  int get selectedIndex() => _selectedIndex;

  /**
   * Sets the selected index of the [Picker] and modifies the item class names as needed.
   */
  void set selectedIndex(int selectedIndex) {
    Element oldSelection = _parent.nodes[_selectedIndex];
    _selectedIndex = selectedIndex;
    Element newSelection = _parent.nodes[_selectedIndex];

    oldSelection.classes = ["${_className}-item", "${_className}-item-enabled"];
    newSelection.classes = ["${_className}-item", "${_className}-item-enabled",
                            "${_className}-item-selected"];
  }

  /**
   * Constructs a [Picker] with a given class name.
   */
  Picker(this._className) {
    _listeners = new List<PickerListener>();
  }

  /**
   * Adds a [PickerListener] to receive updates.
   */
  void addListener(PickerListener listener) {
    _listeners.add(listener);
  }

  /**
   * Sets the parent element, to allow subclass constructors to generate their own parent.
   */
  void setParent(Element parent) {
    _parent = parent;
  }

  void _clickHandler(Event e) {
    Element elt = e.target;
    e.cancelBubble = true;

    // Strip (_className + "-") prefix
    String sidx = elt.id.substring(_className.length + 1, elt.id.length);
    int idx = Math.parseInt(sidx);
    selectedIndex = idx;
    _listeners.forEach((PickerListener listener) {
      listener(idx);
    });
  }
}
