// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(nweiz): move this to the same place as unittest_html.dart and
// unittest_vm.dart. Currently, that leads to import conflicts relating to the
// node library.
#library("unittest");

#import('dart:isolate');
#import('../../../lib/node/node.dart');

#source('../../../../lib/unittest/config.dart');
#source('../../../../lib/unittest/shared.dart');

class PlatformConfiguration extends Configuration {
  void onDone(int passed, int failed, int errors, List<TestCase> results) {
    try {
      super.onDone(passed, failed, errors, results);
    } catch (Exception e) {
      process.exit(1);
    }
  }
}
