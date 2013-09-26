// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.test.model_test;

import 'dart:html';
import 'dart:async';
import 'package:unittest/unittest.dart';
import 'package:tracker/models.dart';
import 'package:unittest/html_config.dart';
import '../web/tracker_app.dart';

/**
 *  Tests for when the page loads.
 */
void main() {
  useHtmlConfiguration();

  new Timer(new Duration(milliseconds: 100), () {
    var trackerApp;
    var root;

    setUp(() {
      trackerApp = query('tracker-app');
      root = trackerApp.shadowRoot;
    });

    test('Tracker app is loaded', () {
      expect(trackerApp.xtag is TrackerApp, isTrue);
    });

    test('seed data is loaded', () {
      expect(appModel.tasks.length, 6);
    });

    test('task button is displayed', () {
      final el = root.query('#new-task-button');
      expect(el.innerHtml.trim(), "Add task");
    });

    test('no form is displayed', () {
      expect(root.query('task-form-element'), isNull);
    });

    test('search bar is displayed', () {
      final el = root.query('#search-input');
      expect(el.placeholder.trim(), "Search");
    });
  });
}