// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library todomvc.test.markdone_test;

import 'dart:async';
import 'dart:html';
import 'package:polymer/polymer.dart';
import 'package:unittest/unittest.dart';
import 'package:unittest/html_config.dart';
import '../web/elements/td_model.dart';
import 'utils.dart';

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

/**
 * This test runs the TodoMVC app, adds a few todos, marks some as done
 * programatically, and clicks on a checkbox to mark others via the UI.
 */
main() {
  initPolymer();
  useHtmlConfiguration();

  TodoModel model;

  setUp(() => Polymer.onReady.then((_) {
    model = querySelector('td-model');
    return onPropertyInit(model, 'items');
  }));

  test('mark done', () {
    model.items
        ..add(new Todo('one (unchecked)'))
        ..add(new Todo('two (unchecked)'))
        ..add(new Todo('three (checked)')..completed = true)
        ..add(new Todo('four (checked)'));

    return new Future(() {
      var body = querySelector('body');

      var label = findWithText(body, 'four (checked)');
      expect(label is LabelElement, true, reason: 'text is in a label: $label');

      ShadowRoot host = label.parentNode.parentNode;
      var node = host.querySelector('input');
      expect(node is InputElement, true, reason: 'node is a checkbox');
      expect(node.type, 'checkbox', reason: 'node type is checkbox');
      expect(node.checked, isFalse, reason: 'element is unchecked');

      node.dispatchEvent(new MouseEvent('click', detail: 1));
      expect(node.checked, true, reason: 'element is checked');
    });
  });
}
