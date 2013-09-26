// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.test.model_test;

import 'package:unittest/unittest.dart';
import 'package:tracker/models.dart';
import 'package:unittest/html_config.dart';

/**
 *  Tests for the Tracker and Task models.
 */
void main() {
  useHtmlConfiguration();

  group('Tracker', () {
    test('tracker', () {
      var tracker = new Tracker();
      expect(tracker.tasks, isNull);
    });
  });

  group('Task', () {
    test('unsaved', () {
      var task = new Task.unsaved();
      expect(task.title, '');
      expect(task.description, '');
      expect(task.status, Task.PENDING);
      expect(task.taskID, isNull);
      expect(task.saved, false);
    });

    test('saved', () {
      var task = new Task('t', 'd', Task.PENDING);
      expect(task.isValid, isTrue);
    });

    test('with title too short', () {
      var task = new Task.unsaved();
      expect(task.isValid, isFalse);
      task.title = 'x';
      expect(task.isValid, isTrue);
    });

    test('with title too long', () {
      var task = new Task.unsaved();
      task.title = new List.filled(
          Task.MAX_TITLE_LENGTH, 'x').join('');
      expect(task.isValid, isFalse);
      task.title = task.title.substring(0, task.title.length - 1);
      expect(task.isValid, isTrue);
    });

    test('with description too long', () {
      var task = new Task.unsaved();
      task.title = 't';
      task.description = new List.filled(
          Task.MAX_DESCRIPTION_LENGTH, 'x').join('');
      expect(task.isValid, isFalse);
      task.description = task.description.substring(0,
          task.description.length - 1);
      expect(task.isValid, isTrue);
    });
  });
}