// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.web.todo_row;

import 'dart:html';
import 'package:polymer/polymer.dart';
import 'model.dart';

@CustomTag('todo-row')
class TodoRow extends LIElement with Polymer, Observable {
  @published Todo todo;

  bool get applyAuthorStyles => true;

  factory TodoRow() => new Element.tag('todo-row');

  TodoRow.created() : super.created() {
    polymerCreated();
  }

  void ready() {
    var root = getShadowRoot("todo-row");
    var label = root.query('#label');
    var item = root.query('.todo-item');

    bindCssClass(item, 'completed', this, 'todo.done');
    bindCssClass(item, 'editing', label, 'editing');
  }

  void removeTodo() { appModel.todos.remove(todo); }
}
