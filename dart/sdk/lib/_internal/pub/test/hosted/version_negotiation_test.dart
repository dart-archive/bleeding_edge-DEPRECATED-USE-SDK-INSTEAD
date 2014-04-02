// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library pub_tests;

import 'package:scheduled_test/scheduled_server.dart';
import 'package:scheduled_test/scheduled_test.dart';

import '../descriptor.dart' as d;
import '../test_pub.dart';

main() {
  initConfig();

  forBothPubGetAndUpgrade((command) {
    integration('sends the correct Accept header', () {
      var server = new ScheduledServer();

      d.appDir({
        "foo": {
          "hosted": {
            "name": "foo",
            "url": server.url.then((url) => url.toString())
          }
        }
      }).create();

      var pub = startPub(args: [command.name]);

      server.handle('GET', '/api/packages/foo', (request) {
        expect(request.headers['Accept'], ['application/vnd.pub.v2+json']);
      });

      pub.kill();
    });

    integration('prints a friendly error if the version is out-of-date', () {
      var server = new ScheduledServer();

      d.appDir({
        "foo": {
          "hosted": {
            "name": "foo",
            "url": server.url.then((url) => url.toString())
          }
        }
      }).create();

      var pub = startPub(args: [command.name]);

      server.handle('GET', '/api/packages/foo', (request) {
        request.response.statusCode = 406;
        request.response.close();
      });

      // TODO(nweiz): this shouldn't request the versions twice (issue 11077).
      server.handle('GET', '/api/packages/foo', (request) {
        request.response.statusCode = 406;
        request.response.close();
      });

      pub.shouldExit(1);

      pub.stderr.expect(emitsLines(
          "Pub 0.1.2+3 is incompatible with the current version of 127.0.0.1.\n"
          "Upgrade pub to the latest version and try again."));
    });
  });
}
