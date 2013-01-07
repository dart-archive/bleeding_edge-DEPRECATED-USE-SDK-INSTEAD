// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library model;

class ViewModel {
  bool isVisible(Todo todo) => todo != null &&
      ((showIncomplete && !todo.done) || (showDone && todo.done));

  bool showIncomplete = true;

  bool showDone = true;
}

final ViewModel viewModel = new ViewModel();

// The real model:

class AppModel {
  List<Todo> todos = <Todo>[];

  // TODO(jmesserly): remove this once List has a remove method.
  void removeTodo(Todo todo) {
    var index = todos.indexOf(todo);
    if (index != -1) {
      todos.removeRange(index, 1);
    }
  }

  bool get allChecked => todos.length > 0 && todos.every((t) => t.done);

  set allChecked(bool value) => todos.forEach((t) { t.done = value; });

  int get doneCount {
    int res = 0;
    todos.forEach((t) { if (t.done) res++; });
    return res;
  }

  int get remaining => todos.length - doneCount;

  void clearDone() {
    todos = todos.where((t) => !t.done).toList();
  }
}

final AppModel app = new AppModel();

class Todo {
  String task;
  bool done = false;

  Todo(this.task);

  String toString() => "$task ${done ? '(done)' : '(not done)'}";
}
