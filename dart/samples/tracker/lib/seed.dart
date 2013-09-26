// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library tracker.seed;

import 'models.dart';

List data = [
    new Task('Create a skeletal polymer app',
             'This should just be a simple coat-hanger. '
             'Just enough to get started.',
             Task.COMPLETED),

    new Task('Create a seeds file',
             'Put all data for the project into this file '
             'and import the data when initializing the app',
             Task.COMPLETED),

    new Task('Sort tasks by status',
             'User a 3-column layout. column1 shows current tasks, '
             'column2 shows pending tasks, column3 show completed tasks',
             Task.COMPLETED),

    new Task('Use Bootstrap 3 for layout',
             'User a 3-column layout that collapses into a '
             'single column on a small mobile device',
             Task.PENDING),

    new Task('Show task details',
             'Right now, only the title is displayed. Let users '
             'click on a link to toggle between full display and title-only '
             'display.',
             Task.CURRENT),

    new Task('On-focus and on-blur do not work',
             'Trying to show error messages when a user fills an '
             'input incorrectly. Callback fires on keypress, etc., but not '
             'when using on-blur or on-focus.',
             Task.CURRENT)
    ];