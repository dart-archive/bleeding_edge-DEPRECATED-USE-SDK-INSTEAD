// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todo_row;

import 'package:polymer/polymer.dart';
import 'model.dart';

@CustomTag('todo-row')
class TodoRow extends PolymerElement with ObservableMixin {
  @observable Todo todo;

  bool get applyAuthorStyles => true;

  // TODO(sigmund,terry): bring back scoped-css polyfills

  void created() {
    super.created();
    var root = getShadowRoot("todo-row");
    var label = root.query('#label').xtag;
    var item = root.query('.todo-item');

    bindCssClass(item, 'completed', this, 'todo.done');
    bindCssClass(item, 'editing', label, 'editing');
  }

  void removeTodo() => appModel.todos.remove(todo);
}
