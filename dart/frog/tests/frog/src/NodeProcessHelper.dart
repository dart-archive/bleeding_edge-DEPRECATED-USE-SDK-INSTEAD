// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeProcessHelper');

#import('../../../lib/node/node.dart');
#import('NodeTestHelper.dart');

// This program is designed to be executed as a sub-process by NodeProcessTest
// (Node doesn't expose "dup" or "dup2", so this is the only way to capture
// the output of stderr, stdout during testing.)

main() {
  printTestOutputBanner();
  var argv = process.argv;
  switch (argv[2]) {
    case 'event-exit':
      process.onExit(() => console.log('exit'));
      break;

    case 'event-uncaughtException':
      process.onUncaughtException((Exception err) =>
         console.log('uncaughtException: ' + err));
      /*
      try {
        throw new Exception('exception1');
      } catch (Exception e) {
        // ignore
      }
      */
      throw new Exception('exception2');
      
    case 'event-catchsignal':
      process.onSignal('SIGUSR1', () => console.log('caught signal'));
      process.kill(process.pid, 'SIGUSR1');
      break;

    case 'stdout':
      process.stdout.write('stdout\n');
      break;

    case 'stderr':
      process.stderr.write('stderr\n');
      break;

    case 'stdin':
      // copy stdin to stdout, assumes caller will write to stdin
      process.stdin.resume();
      process.stdin.pipe(process.stdout);
      break;
    
    case 'argv':
      console.log(process.argv[2]);
      break;

    case 'execPath':
      console.log(process.execPath);
      break;

    case 'chdir-cwd':
      process.chdir('/');
      console.log(process.cwd());
      break;

    // case 'env':
    //  TODO(jackpal): env not implemented.
    //  console.log(process.env['PWD']);
    //  break;
    
    case 'exitCode':
      process.exit(42);
      break;
    
    case 'gid':
      console.log(process.getgid().toString());
      break;

    case 'uid':
      console.log(process.getuid().toString());
      break;

    default:
      console.error('unknown argument. argv=${argv}');
      break;
  }
}
