// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.web.editable_label;

import 'dart:html';
import 'package:polymer/polymer.dart';

/**
 * Label whose [value] can be edited by double clicking. When editing, it
 * displays a form and input element, otherwise it displays the label.
 */
class EditableLabel extends PolymerElement {
  @observable bool editing = false;
  @published String value = '';

  bool get applyAuthorStyles => true;

  InputElement get _editBox => getShadowRoot("editable-label").query('#edit');

  void edit() {
    editing = true;

    // This causes _editBox to be inserted.
    performMicrotaskCheckpoint();

    // For IE and Firefox: use .focus(), then reset the value to move the
    // cursor to the end.
    _editBox.focus();
    _editBox.value = '';
    _editBox.value = value;
  }

  void update(Event e) {
    e.preventDefault(); // don't submit the form
    if (!editing) return; // bail if user canceled
    value = _editBox.value;
    editing = false;
  }

  void maybeCancel(KeyboardEvent e) {
    if (e.keyCode == KeyCode.ESC) {
      editing = false;
    }
  }
}

void main() {
  Polymer.register('editable-label', EditableLabel);
}
