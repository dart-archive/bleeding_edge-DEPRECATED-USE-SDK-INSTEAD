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

  # VM Checked/CompileAll, produces checked
  print 'Started'
  start = time.time()
  RunCommand('./frog.py',
             '--vm_flags=--compile_all --enable_type_checks --enable_asserts',
             '--', '--enable_type_checks', '--out=frogsh', 'frog.dart')
  elapsed = time.time() - start
  mode = 'in checked mode + compile all'
  print 'Compiling on Dart VM took %s seconds %s' % (b(elapsed), b(mode))

  # VM Checked, produces checked
  start = time.time()
  RunCommand('./frog.py',
             '--vm_flags=--enable_type_checks --enable_asserts',
             '--', '--enable_type_checks', '--out=frogsh', 'frog.dart')
  elapsed = time.time() - start
  mode = 'in checked mode'
  print 'Compiling on Dart VM took %s seconds %s' % (b(elapsed), b(mode))

  # VM Normal, produces checked
  start = time.time()
  RunCommand('./frog.py', '--out=frogsh', '--enable_type_checks', 'frog.dart')
  elapsed = time.time() - start
  print 'Compiling on Dart VM took %s seconds' % b(elapsed)

  # Selfhost Checked
  start = time.time()
  RunCommand('./frogsh', '--out=frogsh', '--enable_type_checks', 'frog.dart',
             '--enable_type_checks', 'tests/hello.dart')
  elapsed = time.time() - start
  size = os.path.getsize('./frogsh') / 1024
  print 'Bootstrapping took %s seconds %s' % (b(elapsed), b('in checked mode'))
  print 'Generated %s frogsh is %s kB' % (b('checked'), b(size))

  RunCommand('../tools/build.py', '--mode=release')
  test_cmd = ['../tools/test.py', '--component=frog,frogsh,leg',
              '--report', '--timeout=5', '--progress=color',
              '--mode=release']
  if args[1:]:
    test_cmd.extend(args[1:])
  else:
    test_cmd.extend(['language', 'corelib', 'leg',
                     'isolate', 'peg', 'leg_only'])
  RunCommand(*test_cmd, verbose=True)

if __name__ == '__main__':
  try:
    sys.exit(main(sys.argv))
  except Error as e:
    sys.stderr.write('%s\n' % e)
    sys.exit(1)
