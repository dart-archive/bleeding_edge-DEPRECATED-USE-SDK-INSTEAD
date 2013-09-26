// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.test.model_test;

import 'dart:async';
import 'dart:html';
import 'package:unittest/unittest.dart';
import 'package:unittest/html_config.dart';

/**
 *  Tests for using the new task form.
 */
void main() {
  useHtmlConfiguration();

  new Timer(new Duration(milliseconds: 100), () {
    var trackerApp;
    var root;
    var button;
    var form;

    setUp(() {
      trackerApp = query('tracker-app');
      root = trackerApp.shadowRoot;
      button = root.query('#new-task-button');
    });

    test('clicking task button shows new task form', () {
      button.click();
      var form = root.query('task-form-element');

      var textareas = form.shadowRoot.queryAll('textarea');
      expect(textareas.first.placeholder, 'Add title here');
      expect(textareas.last.placeholder, 'Add description here');

      var submitButton = form.shadowRoot.query('button');
      var errorDiv = form.shadowRoot.query('.error');
      expect(errorDiv.text.trim(), '');

      submitButton.click();
      errorDiv = form.shadowRoot.query('.error');
      expect(errorDiv.text.trim(), 'Title is required');
    });
  });
}