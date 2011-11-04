#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import os
import subprocess
import sys
import time


class Error(Exception):
  pass


def RunCommand(*arguments, **kwargs):
  pattern = None
  if 'pattern' in kwargs:
    pattern = kwargs['pattern']
  expected_exit_code = 0
  if 'exit_code' in kwargs:
    expected_exit_code = kwargs['exit_code']
  stdout = subprocess.PIPE
  if 'verbose' in kwargs and kwargs['verbose']:
    stdout = None
  try:
    proc = subprocess.Popen(arguments,
                            stdout=stdout,
                            stderr=subprocess.STDOUT)
    stdout = proc.communicate()[0]
    exit_code = proc.wait()
  except OSError as e:
    raise Error('%s: %s' % (arguments[0], e.strerror))
  if exit_code != expected_exit_code:
    DiagnoseError(arguments, stdout)
    raise Error('%s returned %s' % (arguments[0], exit_code))
  if pattern and not pattern in stdout:
    DiagnoseError(arguments, stdout)
    raise Error('%s failed' % arguments[0])


def DiagnoseError(arguments, stdout):
  quoted_arguments = ' '.join([repr(s) for s in arguments])
  sys.stderr.write('Command failed:\n%s\n' % quoted_arguments)
  if stdout:
    sys.stderr.write(stdout)


def main(args):
  def b(s):
    """Adds ANSI escape-code for bold-face"""
    return "\033[1m%s\033[0m" % s

  print 'Started'
  start = time.time()
  RunCommand('./frog.py', '--js_out=frogsh',
             '--vm_flags=--compile_all --enable_type_checks --enable_asserts',
             '--', 'frog.dart')
  elapsed = time.time() - start
  print 'Compiling on Dart VM took %s seconds %s' % (b(elapsed),
                                                     b('in checked mode'))
  start = time.time()
  RunCommand('./frog.py', '--js_out=frogsh', '--', 'frog.dart')
  elapsed = time.time() - start
  print 'Compiling on Dart VM took %s seconds' % b(elapsed)
  start = time.time()
  RunCommand('./frogsh', '--out=frogsh', 'frog.dart', 'tests/hello.dart')
  elapsed = time.time() - start
  size = os.path.getsize('./frogsh') / 1024
  print 'Bootstrapping took %s seconds' % b(elapsed)
  print 'Generated frogsh is \033[1m%d\033[0m kB' % size

  RunCommand('../tools/build.py', '--mode=release')
  test_cmd = ['../tools/test.py', '--component=frog,frogsh,leg',
              '--report', '--timeout=5', '--progress=color',
              '--mode=release']
  if args[1:]:
    test_cmd.extend(args[1:])
  else:
    test_cmd.extend(['language', 'corelib', 'leg', 'peg'])
  RunCommand(*test_cmd, verbose=True)

  leg_test_dir = os.path.join('leg', 'tests')
  for current_dir, directories, filenames in os.walk(leg_test_dir):
    for filename in filenames:
      if filename.endswith('.dart'):
        pattern = 'info: [leg] compilation succeeded'
        node_exit_code = 0
        vm_exit_code = 0
        if filename == 'empty.dart':
          pattern = 'info: [leg] compiler cancelled: Could not find main'
          node_exit_code = 1
          vm_exit_code = 255 # Sigh.
        filename = os.path.join(current_dir, filename)
        frog_bin = os.path.join('.', 'frogsh')
        RunCommand(frog_bin, '--enable_leg', '--verbose', '--throw_on_errors',
                   filename, pattern=pattern, exit_code=node_exit_code)
        frog_bin = os.path.join('.', 'frog.py')
        RunCommand(frog_bin,
                   '--vm_flags=--enable_type_checks --enable_asserts',
                   '--', '--enable_leg', '--verbose', '--throw_on_errors',
                   filename, pattern=pattern, exit_code=vm_exit_code)

if __name__ == '__main__':
  try:
    sys.exit(main(sys.argv))
  except Error as e:
    sys.stderr.write('%s\n' % e)
    sys.exit(1)
