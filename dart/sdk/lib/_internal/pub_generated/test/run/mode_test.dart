// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import '../descriptor.dart' as d;
import '../test_pub.dart';

const TRANSFORMER = """
import 'dart:async';

import 'package:barback/barback.dart';

class DartTransformer extends Transformer {
  final BarbackSettings _settings;

  DartTransformer.asPlugin(this._settings);

  String get allowedExtensions => '.in';

  void apply(Transform transform) {
    transform.addOutput(new Asset.fromString(
        new AssetId(transform.primaryInput.id.package, "bin/script.dart"),
        "void main() => print('\${_settings.mode.name}');"));
  }
}
""";

main() {
  initConfig();
  withBarbackVersions("any", () {
    integration('runs a local script with customizable modes', () {
      d.dir(appPath, [d.pubspec({
          "name": "myapp",
          "transformers": ["myapp/src/transformer"]
        }),
            d.dir(
                "lib",
                [
                    d.dir(
                        "src",
                        [
                            d.file("transformer.dart", TRANSFORMER),
                            d.file("primary.in", "")])])]).create();

      createLockFile('myapp', pkg: ['barback']);

      // By default it should run in debug mode.
      var pub = pubRun(args: ["script"]);
      pub.stdout.expect("debug");
      pub.shouldExit();

      // A custom mode should be specifiable.
      pub = pubRun(args: ["--mode", "custom-mode", "script"]);
      pub.stdout.expect("custom-mode");
      pub.shouldExit();
    });

    integration('runs a dependency script with customizable modes', () {
      d.dir("foo", [d.pubspec({
          "name": "foo",
          "version": "1.2.3",
          "transformers": ["foo/src/transformer"]
        }),
            d.dir(
                "lib",
                [
                    d.dir(
                        "src",
                        [
                            d.file("transformer.dart", TRANSFORMER),
                            d.file("primary.in", "")])])]).create();

      d.appDir({
        "foo": {
          "path": "../foo"
        }
      }).create();

      createLockFile('myapp', sandbox: ['foo'], pkg: ['barback']);

      // By default it should run in release mode.
      var pub = pubRun(args: ["foo:script"]);
      pub.stdout.expect("release");
      pub.shouldExit();

      // A custom mode should be specifiable.
      pub = pubRun(args: ["--mode", "custom-mode", "foo:script"]);
      pub.stdout.expect("custom-mode");
      pub.shouldExit();
    });
  });
}
