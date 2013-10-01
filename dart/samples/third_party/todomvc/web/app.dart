// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.web.app;

import 'dart:html';
import 'package:polymer/polymer.dart';
import 'model.dart';

@CustomTag("todo-app")
class TodoApp extends PolymerElement {
  @observable AppModel app;
  bool get applyAuthorStyles => true;

  void created() {
    super.created();
    app = appModel;
  }

  void addTodo(Event e) {
    e.preventDefault(); // don't submit the form
    var input = getShadowRoot('todo-app').query('#new-todo');
    if (input.value == '') return;
    app.todos.add(new Todo(input.value));
    input.value = '';
  }

  void clear() => app.clearDone();
}
