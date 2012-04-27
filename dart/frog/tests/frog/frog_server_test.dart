// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('FrogServerTest');

#import('dart:io');

#import('../../server/frog_server.dart', prefix: 'frogserver');

// TODO(jmesserly): more sane way to import JSON on the VM
#import('../../../lib/json/json.dart');

main() {
  // TODO(jmesserly): This test must be run from 'frog' working directory.
  var homedir = new File('.').fullPathSync();
  print('test: setting Frog home directory to $homedir');

  // Start the compiler server. 0 causes it to grab any free port.
  var host = '127.0.0.1';
  var compileSocket = frogserver.startServer(homedir, host, 0);

  // Connect to the compiler
  var testSocket = new Socket(host, compileSocket.port);

  final testId = 'abc' + new Date.now().value;

  var bytes = new List<int>();
  testSocket.onData = () {
    var pos = bytes.length;
    var len = testSocket.available();
    bytes.insertRange(pos, len);
    testSocket.readList(bytes, pos, len);
    var response = frogserver.tryReadJson(bytes);
    if (response == null) return; // wait for more data

    // Verify ID
    Expect.equals(testId, response['id']);
    if (response['kind'] == 'message') {
      // Info and warnings are okay. But we shouldn't get errors!
      print('test: got message ${JSON.stringify(response)}');
      Expect.notEquals(null, response['message']);
      Expect.notEquals('error', response['prefix']);
      return;
    }

    Expect.equals('done', response['kind']);
    Expect.equals('compile', response['command']);
    Expect.equals(true, response['result']);
    print('test: PASS. Compile successful!');

    // Trigger a clean shutdown
    frogserver.writeJson(testSocket.outputStream, { 'command': 'close' });
    testSocket.close();
    compileSocket.close();
  };

  // Try a hello world compile
  frogserver.writeJson(testSocket.outputStream, {
    'command': 'compile',
    'id': testId,
    'input': 'tests/hello.dart'
    // Note: intentionally don't specify 'output' so nothing is written to disk
  });
}
