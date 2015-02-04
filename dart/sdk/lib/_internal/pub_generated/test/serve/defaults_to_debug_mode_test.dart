// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS d.file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import '../descriptor.dart' as d;
import '../test_pub.dart';
import '../serve/utils.dart';

const TRANSFORMER = """
import 'dart:async';

import 'package:barback/barback.dart';

class ModeTransformer extends Transformer {
  final BarbackSettings settings;
  ModeTransformer.asPlugin(this.settings);

  String get allowedExtensions => '.txt';

  Future apply(Transform transform) {
    return new Future.value().then((_) {
      var id = transform.primaryInput.id.changeExtension(".out");
      transform.addOutput(new Asset.fromString(id, settings.mode.toString()));
    });
  }
}
""";

main() {
  initConfig();
  withBarbackVersions("any", () {
    integration("defaults to debug mode", () {
      d.dir(appPath, [d.pubspec({
          "name": "myapp",
          "transformers": ["myapp/src/transformer"]
        }),
            d.dir("lib", [d.dir("src", [d.file("transformer.dart", TRANSFORMER)])]),
            d.dir("web", [d.file("foo.txt", "foo")])]).create();

      createLockFile('myapp', pkg: ['barback']);

      pubServe();
      requestShouldSucceed("foo.out", "debug");
      endPubServe();
    });
  });
}
