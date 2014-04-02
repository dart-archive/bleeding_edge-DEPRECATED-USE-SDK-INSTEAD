// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:convert';

import 'package:scheduled_test/scheduled_test.dart';
import 'package:scheduled_test/scheduled_server.dart';

import '../descriptor.dart' as d;
import '../test_pub.dart';
import 'utils.dart';

main() {
  initConfig();
  setUp(d.validPackage.create);

  integration('package creation provides a malformed success', () {
    var server = new ScheduledServer();
    d.credentialsFile(server, 'access token').create();
    var pub = startPublish(server);

    confirmPublish(pub);
    handleUploadForm(server);
    handleUpload(server);

    var body = {'success': 'Your package was awesome.'};
    server.handle('GET', '/create', (request) {
      request.response.write(JSON.encode(body));
      request.response.close();
    });

    pub.stderr.expect('Invalid server response:');
    pub.stderr.expect(JSON.encode(body));
    pub.shouldExit(1);
  });
}
