// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeConsoleTest');

#import('node_config.dart');
#import('../../../../lib/unittest/unittest.dart');
#import('../../../lib/node/node.dart');
#import('NodeTestHelper.dart');

void main() {
  useNodeConfiguration();
  group('console', () {
    final helperProgram = getFilename('NodeConsoleHelper.dart');
    asyncSimpleTests(helperProgram,
    // name-of-test, argument-to-child, stdin-to-child, expected-stdout,
    // expected-stderr
      [
        ['log', 'log', null, 'log\n', ''],
        ['info', 'info', null,'info\n', ''],
        ['warn', 'warn', null,'', 'warn\n'],
        ['error', 'error', null,'', 'error\n'],
        ['dir', 'dir', null, '[ \'dir1\', \'dir2\' ]\n', ''],
        ['assert-true', 'assert-true', null, '', ''],
        ['assert-true2', 'assert-true', null, '', ''],
      ]);

    // name-of-test, argument-to-child, stdin-to-child, expected-error-pattern,
    // expected-stdout-pattern, expected-stderr-pattern
    asyncFuzzyTests(helperProgram,
      [
        ['time', 'time', null, null, @'a: \d+ms', null],
        ['trace', 'trace', null, null, null, @'Trace:'],
        ['assert-false', 'assert-false', null,
          @'AssertionError:', null, null],
        ['assert-false2', 'assert-false2', null,
          @'AssertionError: assert-false2', null, null],
      ]);
  });
}
