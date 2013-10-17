// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.test.mainpage_test;

import 'dart:html';
import 'package:polymer/polymer.dart';
import 'package:unittest/html_config.dart';
import 'package:unittest/unittest.dart';
import '../web/app.dart';
import '../web/model.dart';

/**
 * This test runs the TodoMVC app and checks the state of the initial page.
 */
// TODO(jmesserly): verify some styles (colors, fonts, relative size) as well.
@initMethod _main() {
  useHtmlConfiguration();

  setUp(() => Polymer.onReady);

  test('initial state', () {
    final todoApp = query('todo-app');
    expect(appModel.todos.length, 0);
    expect(todoApp.xtag is TodoApp, true, reason: 'TodoApp should be created');

    final root = todoApp.shadowRoot;
    final newTodo = root.query('#new-todo');
    expect(newTodo.placeholder, "What needs to be done?");

    // TODO(jmesserly): re-enable this. It fails on Firefox with ShadowDOM.
    // The issue appears to be that
    // Wait for setTimeout 0 for focus to activate.
    /*Timer.run(expectAsync0(() {
      expect(document.activeElement, todoApp, reason: 'app should have focus');
      expect(root.activeElement, newTodo, reason: 'New todo should have focus');
      expect(root.queryAll('[is=todo-row]').length, 0, reason: 'no items yet');
    }));*/
  });
}
