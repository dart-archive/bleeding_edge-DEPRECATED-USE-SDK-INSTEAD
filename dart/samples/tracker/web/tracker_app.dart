// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.web.tracker_app;

import 'package:polymer/polymer.dart';
import 'package:tracker/models.dart';
import 'package:tracker/seed.dart' as seed;

@CustomTag('tracker-app')
class TrackerApp extends PolymerElement with ObservableMixin {
  bool get applyAuthorStyles => true;
  @observable final ObservableList<Task> tasks = toObservable([]);
  @observable Tracker app;
  @observable Task newTask = new Task.unsaved();
  @observable String searchParam = '';
  @observable bool usingForm = false;

  // Buckets for grouping tasks by status.
  @observable List<Task> current = toObservable([]);
  @observable List<Task> pending = toObservable([]);
  @observable List<Task> completed = toObservable([]);

  // Used in search for stashing elements from tasks that don't match the
  // search param.
  List<Task> filteredOutTasks = [];

  TrackerApp() {
    app = appModel;
    appModel.tasks = tasks;

    tasks.changes.listen((List<ChangeRecord> changes) {
      _filterTasksByStatus();
    });

    _addSeedData();
  }

  toggleFormDisplay() {
    usingForm = !usingForm;
  }

  showEmptyForm() {
    newTask = new Task.unsaved();
    if (!usingForm) {
      toggleFormDisplay();
    }
  }

  search() {
    tasks.addAll(filteredOutTasks);
    filteredOutTasks.clear();

    for (Task task in tasks) {
      if (!taskMatchesSearchParam(task)) {
        filteredOutTasks.add(task);
      }
    }

    for (Task task in filteredOutTasks) {
      tasks.remove(task);
    }
  }

  bool taskMatchesSearchParam(Task task) {
    var param = searchParam.toLowerCase();
    if (param.isEmpty) return true;
    return task.title.toLowerCase().contains(param) ||
        task.description.toLowerCase().contains(param);
  }

  _addSeedData() {
    for (var i = 0; i < seed.data.length; i++) {
      // Assign IDs to the seed data, so that task.saved == true.
      seed.data[i].taskID = i;
      tasks.add(seed.data[i]);
    }
  }

  _filterTasksByStatus() {
    current = [];
    pending = [];
    completed = [];
    for (Task task in tasks) {
      if (task.status == Task.CURRENT) {
        current.add(task);
      } else if (task.status == Task.PENDING) {
        pending.add(task);
      } else {
        completed.add(task);
      }
    }
  }
}