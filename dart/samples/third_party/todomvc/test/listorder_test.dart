// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.test.listorder_test;

import 'dart:async';
import 'dart:html';
import 'package:polymer/polymer.dart';
import 'package:unittest/unittest.dart';
import 'package:unittest/html_config.dart';
import '../web/model.dart';

/**
 * This test runs the TodoMVC app, adds a few elements, marks some as done, and
 * switches from back and forth between "Active" and "All". This will make some
 * nodes to be hidden and readded to the page.
 */
main() {
  initPolymer();
  useHtmlConfiguration();

  ShadowRoot root;

  setUp(() => Polymer.onReady.then((_) {
    root = query('todo-app').shadowRoot;
  }));

  test('programmatically add items to model', () {
    appModel.todos.addAll([
      new Todo('one (unchecked)'),
      new Todo('two (checked)')..done = true,
      new Todo('three (unchecked)')
    ]);
    Observable.dirtyCheck();
    return window.animationFrame.then((_) {
      expect(root.queryAll('#todo-list li[is=todo-row]').length, 3);

      // TODO(jmesserly): HTML Imports breaks relative hash links when the
      // component is at a different path from the main HTML document. For now
      // fix it programmatically.
      for (var a in root.queryAll('#filters > li > a')) {
        a.href = '#${Uri.parse(a.href).fragment}';
      }
    });
  });

  test('navigate to #/active', () {
    windowLocation.hash = '#/active';
    return window.animationFrame.then((_) {
      expect(root.queryAll('#todo-list li[is=todo-row]').length, 2);
    });
  });

  test('navigate to #/completed', () {
    windowLocation.hash = '#/completed';
    return window.animationFrame.then((_) {
      expect(root.queryAll('#todo-list li[is=todo-row]').length, 1);
    });
  });

  test('navigate back to #/', () {
    windowLocation.hash = '#/';
    return window.animationFrame.then((_) {
      expect(root.queryAll('#todo-list li[is=todo-row]').length, 3);
    });
  });
}
