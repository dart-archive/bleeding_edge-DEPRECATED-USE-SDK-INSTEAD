// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.test.mainpage_test;

import 'dart:async';
import 'dart:html';
import 'dart:js' as js;
import 'package:polymer/polymer.dart';
import 'package:unittest/html_config.dart';
import 'package:unittest/unittest.dart';
import '../web/elements/td_model.dart';
import '../web/elements/td_todos.dart';
import 'utils.dart';

/**
 * This test runs the TodoMVC app and checks the state of the initial page.
 */
main() {
  initPolymer();
  useHtmlConfiguration();

  setUp(() => Polymer.onReady);

  test('initial state', () {
    final model = querySelector('td-model');
    expect(model is TodoModel, true, reason: 'TodoModel should be created');

    final app = querySelector('td-todos');
    expect(app is TodoList, true, reason: 'TodoList should be created');

    final root = app.shadowRoot;
    final newTodo = root.querySelector('#new-todo');
    expect(newTodo.placeholder, "What needs to be done?");

    expect(app.modelId, 'model', reason: 'modelId is set via attribute');

    // Validate the stylesheet was loaded
    if (js.context['ShadowDOMPolyfill'] != null) {
      final style = document.head.querySelector('style[shim-shadowdom-css]');
      expect(style.text, contains('\ntd-todos #todoapp {'));
    } else {
      final style = root.querySelector('style');
      expect(style, isNotNull, reason: '<link> was replaced with <style>');
      expect(style.text, contains('\n#todoapp {'));
    }

    // Validate a style got applied
    var color = root.querySelector('#footer').getComputedStyle().color;
    expect(color, 'rgb(119, 119, 119)');

    return onPropertyInit(model, 'items').then((_) {
      expect(model.items, [], reason: 'no items yet');
      expect(app.model, model, reason: 'model should be data-bound');
    });
  });
}
