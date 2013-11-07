// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.web.app;

import 'dart:html';
import 'package:polymer/polymer.dart';
import 'model.dart';

@CustomTag('todo-app')
class TodoApp extends PolymerElement {
  @observable AppModel app;
  bool get applyAuthorStyles => true;

  factory TodoApp() => new Element.tag('TodoApp');

  TodoApp.created() : super.created() {
    app = appModel;
  }

  void addTodo(Event e) {
    e.preventDefault(); // don't submit the form
    InputElement input = getShadowRoot('todo-app').querySelector('#new-todo');
    if (input.value == '') return;
    app.todos.add(new Todo(input.value));
    input.value = '';
  }

  void clear() => app.clearDone();
}
