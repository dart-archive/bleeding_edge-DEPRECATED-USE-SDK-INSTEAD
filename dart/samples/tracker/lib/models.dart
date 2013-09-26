// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.models;

import 'package:polymer/polymer.dart';

final appModel = new Tracker();

/**
 * A model for the tracker app.
 *
 * [tasks] contains all tasks used in this app.
 */
class Tracker extends ObservableBase {
  @observable List<Task> tasks;
}


/**
 * A model for creating a single task.
 *
 * A task can be saved or unsaved. Only a saved task has a taskID.
 *
 * This model defines validation rules for a Task. It is the responsibility of
 * the view layer to validate a task before assigning a taskID to the task. A
 * task with a taskID is considered saved.
 */
class Task extends ObservableBase {
  static bool TITLE_REQUIRED = true;
  static const MAX_TITLE_LENGTH = 40;
  static const MAX_DESCRIPTION_LENGTH = 200;
  static const CURRENT = 'current';
  static const PENDING = 'pending';
  static const COMPLETED = 'completed';

  @observable int taskID;
  @observable String title = '';
  @observable String description = '';
  @observable String status = PENDING;
  @observable DateTime createdAt;
  @observable DateTime updatedAt;

  Task.unsaved();

  Task(this.title, this.description, this.status);

  bool get isValid {
    var minTitleLength = TITLE_REQUIRED ? 1 : 0;
    return (title.length >= minTitleLength &&
        title.length < MAX_TITLE_LENGTH &&
        description.length < MAX_DESCRIPTION_LENGTH);
  }

  bool get saved => taskID != null;
}

