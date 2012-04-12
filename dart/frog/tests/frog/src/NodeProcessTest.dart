// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeProcessTest');

#import('node_config.dart');
#import('../../../../lib/unittest/unittest.dart');
#import('../../../lib/node/node.dart');
#import('NodeTestHelper.dart');

void main() {
  useNodeConfiguration();
  group('process', () {
    final helperProgram = getFilename('NodeProcessHelper.dart');
    asyncTest('nextTick', 1,
     () {
      process.nextTick(() {
        callbackDone();
      });
    });
    
    asyncTestExitCode('exitCode', helperProgram, ['exitCode'], 42);

    asyncSimpleTests(helperProgram,
    // name-of-test, argument-to-child, stdin-to-child, expected-stdout,
    // expected-stderr
      [
        ['event-exit', 'event-exit', null, 'exit\n', ''],
        ['event-uncaughtException', 'event-uncaughtException', null,
          'uncaughtException: Exception: exception2\n', ''],
        ['event-catchsignal', 'event-catchsignal', null,
          'caught signal\n', ''],
        ['stdout', 'stdout', null, 'stdout\n', ''],
        ['stderr', 'stderr', null, '', 'stderr\n'],
        ['stdin', 'stdin', 'some text\nsome more text\n',
          'some text\nsome more text\n', ''],
        ['argv', 'argv', null, 'argv\n', ''],
        ['chdir-cwd', 'chdir-cwd', null, '/\n', ''],
      ]);

    asyncFuzzyTests(helperProgram,
      // name-of-test, argument-to-child, stdin-to-child,
      // expected-error-pattern, expected-stdout-pattern,
      // expected-stderr-pattern
      [
        ['execPath', 'execPath', null, null, 'node', null],
      ]);
  });
}
