// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'package:scheduled_test/scheduled_test.dart';

import 'descriptor.dart' as d;
import 'test_pub.dart';

main() {
  initConfig();

  setUp(() {
    servePackages([
      packageMap("normal", "1.2.3", {
        "transitive": "any",
        "circular_a": "any"
      }),
      packageMap("transitive", "1.2.3", {
        "shared": "any"
      }),
      packageMap("shared", "1.2.3", {
        "other": "any"
      }),
      packageMap("unittest", "1.2.3", {
        "shared": "any"
      }),
      packageMap("other", "1.0.0"),
      packageMap("overridden", "1.0.0"),
      packageMap("overridden", "2.0.0"),
      packageMap("override_only", "1.2.3"),
      packageMap("circular_a", "1.2.3", {
        "circular_b": "any"
      }),
      packageMap("circular_b", "1.2.3", {
        "circular_a": "any"
      })
    ]);

    d.dir("from_path", [
      d.libDir("from_path"),
      d.libPubspec("from_path", "1.2.3")
    ]).create();

    d.dir(appPath, [
      d.pubspec({
        "name": "myapp",
        "dependencies": {
          "normal": "any",
          "overridden": "1.0.0",
          "from_path": {"path": "../from_path"}
        },
        "dev_dependencies": {
          "unittest": "any"
        },
        "dependency_overrides": {
          "overridden": "2.0.0",
          "override_only": "any"
        }
      })
    ]).create();
  });

  integration("lists dependencies in compact form", () {
    pubGet();
    schedulePub(args: ['deps', '-s', 'compact'], output: '''
        myapp 0.0.0

        dependencies:
        - from_path 1.2.3
        - normal 1.2.3 [circular_a transitive]
        - overridden 2.0.0

        dev dependencies:
        - unittest 1.2.3 [shared]

        dependency overrides:
        - overridden 2.0.0
        - override_only 1.2.3

        transitive dependencies:
        - circular_a 1.2.3 [circular_b]
        - circular_b 1.2.3 [circular_a]
        - other 1.0.0
        - shared 1.2.3 [other]
        - transitive 1.2.3 [shared]
        ''');
  });

  integration("lists dependencies in list form", () {
    pubGet();
    schedulePub(args: ['deps', '--style', 'list'], output: '''
        myapp 0.0.0

        dependencies:
        - from_path 1.2.3
        - overridden 2.0.0
        - normal 1.2.3
          - circular_a any
          - transitive any

        dev dependencies:
        - unittest 1.2.3
          - shared any

        dependency overrides:
        - override_only 1.2.3
        - overridden 2.0.0

        transitive dependencies:
        - circular_a 1.2.3
          - circular_b any
        - circular_b 1.2.3
          - circular_a any
        - other 1.0.0
        - shared 1.2.3
          - other any
        - transitive 1.2.3
          - shared any
        ''');
  });

  integration("lists dependencies in tree form", () {
    pubGet();
    schedulePub(args: ['deps'], output: '''
        myapp 0.0.0
        |-- from_path 1.2.3
        |-- normal 1.2.3
        |   |-- circular_a 1.2.3
        |   |   '-- circular_b 1.2.3
        |   |       '-- circular_a...
        |   '-- transitive 1.2.3
        |       '-- shared...
        |-- overridden 2.0.0
        |-- override_only 1.2.3
        '-- unittest 1.2.3
            '-- shared 1.2.3
                '-- other 1.0.0
        ''');
  });
}