// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.web.task_form_element;

import 'package:polymer/polymer.dart';
import 'package:tracker/models.dart';
import 'dart:html';
import 'dart:math';

@CustomTag('task-form-element')
class TaskFormElement extends PolymerElement {
  bool get applyAuthorStyles => true;
  @observable Task task;
  @observable String titleErrorMessage = '';
  @observable int maxTitleLength = Task.MAX_TITLE_LENGTH;
  @observable String descriptionErrorMessage = '';
  @observable int maxDescriptionLength = Task.MAX_DESCRIPTION_LENGTH;
  @observable final List<String> taskStatusOptions = toObservable([
      Task.CURRENT, Task.PENDING, Task.COMPLETED]);
  @observable int statusSelectedIndex = 1;
  @observable String previousStatus = '';
  @observable String submitLabel = '';

  TaskFormElement.created() : super.created();

  attached() {
    super.attached();
    submitLabel = task.saved ? 'Update' : 'Create';
    statusSelectedIndex = taskStatusOptions.indexOf(task.status);
    if (!task.saved) {
      statusSelectedIndex = taskStatusOptions.indexOf(task.status);
      previousStatus = task.status;
    }
  }

  bool validateTitle() {
    int len = task.title.length;
    bool valid = false;
    if (len == 0 && Task.TITLE_REQUIRED) {
      titleErrorMessage = 'Title is required';
    } else if (len > maxTitleLength) {
      titleErrorMessage = 'Title must be less than $maxTitleLength characters';
    } else {
      titleErrorMessage = '';
      valid = true;
    }
    return valid;
  }

  bool validateDescription() {
    int len = task.description.length;
    bool valid = false;
    if (len >= maxDescriptionLength) {
      descriptionErrorMessage = 'Description must be less than '
          '$maxDescriptionLength characters';
    } else {
      descriptionErrorMessage = '';
      valid = true;
    }
    return valid;
  }

  createOrUpdateTask(Event event, detail, sender) {
    event.preventDefault();
    event.stopPropagation();

    // Do view validation to trigger error messages, and model validation
    // to ensure that a task is never in an invalid state.
    if (!validateTitle() || !validateDescription() || !task.isValid) return;

    previousStatus = task.status;
    task.status = taskStatusOptions[statusSelectedIndex];

    if (task.saved) {
      updateTask();
    } else {
      createTask();
    }
    dispatchNotNeeded();
  }

  void dispatchNotNeeded() {
    dispatchEvent(new CustomEvent('notneeded'));
  }

  void dispatchCancelForm() {
    dispatchEvent(new CustomEvent('cancelform'));
  }

  createTask() {
    DateTime now = new DateTime.now();
    var random = new Random();
    task.taskID = random.nextInt(1000 * 1000);
    task.createdAt = now;
    task.updatedAt = now;
    appModel.tasks.add(task);
  }

  updateTask() {
    DateTime now = new DateTime.now();
    task.updatedAt = now;
    if (previousStatus != task.status) {
      appModel.tasks.remove(task);
      appModel.tasks.add(task);
    }
  }

  deleteTask() {
    if (window.confirm('Are you sure you want to delete this?')) {
      appModel.tasks.remove(task);
    }
  }
}
