// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS d.file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import 'package:scheduled_test/scheduled_test.dart';

import '../../descriptor.dart' as d;
import '../../test_pub.dart';
import '../../serve/utils.dart';

main() {
  initConfig();
  integration("works on the dart2js transformer", () {
    d.dir(appPath, [
      d.pubspec({
        "name": "myapp",
        "transformers": [
          {
            "\$dart2js": {
              "\$include": ["web/a.dart", "web/b.dart"],
              "\$exclude": "web/a.dart"
            }
          }
        ]
      }),
      d.dir("web", [
        d.file("a.dart", "void main() => print('hello');"),
        d.file("b.dart", "void main() => print('hello');"),
        d.file("c.dart", "void main() => print('hello');")
      ])
    ]).create();

    createLockFile('myapp', pkg: ['barback']);

    pubServe();
    requestShould404("a.dart.js");
    requestShouldSucceed("b.dart.js", isNot(isEmpty));
    requestShould404("c.dart.js");
    endPubServe();
  });
}
