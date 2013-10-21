// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.test.markdone_test;

import 'dart:async';
import 'dart:html';
import 'package:polymer/platform.dart' show endOfMicrotask;
import 'package:polymer/polymer.dart';
import 'package:unittest/unittest.dart';
import 'package:unittest/html_config.dart';
import '../web/model.dart';

Node findWithText(Node node, String text) {
  if (node.text == text) return node;
  if (node is Element && (node as Element).localName == 'polymer-element') {
    return null;
  }
  if (node is Element && (node as Element).shadowRoot != null) {
    var r = findWithText((node as Element).shadowRoot, text);
    if (r != null) return r;
  }
  for (var n in node.nodes) {
    var r = findWithText(n, text);
    if (r != null) return r;
  }
  return null;
}

Node findShadowHost(Node node, ShadowRoot root) {
  if (node is Element) {
    var shadowRoot = (node as Element).shadowRoot;
    if (shadowRoot == root) return node;
    if (shadowRoot != null) {
      var r = findShadowHost(shadowRoot, root);
      if (r != null) return r;
    }
  }
  for (var n in node.nodes) {
    var r = findShadowHost(n, root);
    if (r != null) return r;
  }
  return null;
}

/**
 * This test runs the TodoMVC app, adds a few todos, marks some as done
 * programatically, and clicks on a checkbox to mark others via the UI.
 */
main() {
  initPolymer();
  useHtmlConfiguration();

  setUp(() => Polymer.onReady);

  test('mark done', () {
    appModel.todos.add(new Todo('one (unchecked)'));
    appModel.todos.add(new Todo('two (unchecked)'));
    appModel.todos.add(new Todo('three (checked)')..done = true);
    appModel.todos.add(new Todo('four (checked)'));

    endOfMicrotask(expectAsync0(() {
      var body = query('body');

      var label = findWithText(body, 'four (checked)');
      expect(label is LabelElement, true, reason: 'text is in a label: $label');

      var host = findShadowHost(body, label.parentNode);
      var node = host.parent.query('input');
      expect(node is InputElement, true, reason: 'node is a checkbox');
      expect(node.type, 'checkbox', reason: 'node type is checkbox');
      expect(node.checked, isFalse, reason: 'element is unchecked');

      node.dispatchEvent(new MouseEvent('click', detail: 1));
      expect(node.checked, true, reason: 'element is checked');
    }));
  });
}
