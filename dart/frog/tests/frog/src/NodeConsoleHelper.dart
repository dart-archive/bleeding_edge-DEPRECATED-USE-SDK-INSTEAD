// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('ConsoleHelper');

#import('../../../lib/node/node.dart');
#import('NodeTestHelper.dart');

// This program is designed to be executed as a sub-process by NodeConsoleTest
// (Node doesn't expose "dup" or "dup2", so this is the only way to capture
// the output of stderr, stdout during testing.)

main() {
  printTestOutputBanner();
  var argv = process.argv;
  switch (argv[2]) {
    case 'log':
      console.log('log');
      break;
    case 'info':
      console.info('info');
      break;
    case 'warn':
      console.warn('warn');
      break;
    case 'error':
      console.error('error');
      break;
    case 'dir':
      console.dir(['dir1', 'dir2']);
      break;
    case 'time':
      console.time('a');
      console.timeEnd('a');
      break;
    case 'trace':
      console.trace();
      break;
    case 'assert-true':
      console.assert(true);
      break;
    case 'assert-true2':
        console.assert(true, "assert-true2");
        break;
    case 'assert-false':
      console.assert(false);
      break;
    case 'assert-false2':
      console.assert(false, "assert-false2");
      break;
    default:
      console.error("unknown argument. argv=${argv}");
      break;
  }
}
