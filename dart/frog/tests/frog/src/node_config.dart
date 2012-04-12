// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("node_config");

#import('../../../../lib/unittest/unittest.dart');
#import('../../../lib/node/node.dart');


class NodeConfiguration extends Configuration {
  void onDone(int passed, int failed, int errors, List<TestCase> results) {
    try {
      super.onDone(passed, failed, errors, results);
    } catch (Exception e) {
      process.exit(1);
    }
  }
}

void useNodeConfiguration() {
  configure(new NodeConfiguration());
}
