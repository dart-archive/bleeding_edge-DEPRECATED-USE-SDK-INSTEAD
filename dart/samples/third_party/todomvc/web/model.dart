// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library model;

import 'package:web_ui/observe.dart';
import 'package:web_ui/observe/html.dart';

@observable
class ViewModel {
  bool isVisible(Todo todo) => todo != null &&
      ((showIncomplete && !todo.done) || (showDone && todo.done));

  bool get showIncomplete => locationHash != '#/completed';

  bool get showDone => locationHash != '#/active';
}

final ViewModel viewModel = new ViewModel();

// The real model:

@observable
class AppModel {
  final ObservableList<Todo> todos = new ObservableList<Todo>();

  bool get allChecked => todos.length > 0 && todos.every((t) => t.done);

  set allChecked(bool value) => todos.forEach((t) { t.done = value; });

  int get doneCount =>
      todos.fold(0, (count, t) => count + (t.done ? 1 : 0));

  int get remaining => todos.length - doneCount;

  void clearDone() => todos.removeWhere((t) => t.done);
}

final AppModel app = new AppModel();

@observable
class Todo {
  String task;
  bool done = false;

  Todo(this.task);

  String toString() => "$task ${done ? '(done)' : '(not done)'}";
}
