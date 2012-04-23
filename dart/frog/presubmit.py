#!/usr/bin/env python
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import optparse
import os
import stat
import subprocess
import sys
import time


class Error(Exception):
  pass


def BuildOptions():
  """Configures an option parser for this script"""
  result = optparse.OptionParser()
  result.add_option(
      '--notest',
      help='Skip running test.py',
      default=False,
      action='store_true')
  result.add_option(
      '--leg-only',
      help='Only run leg tests',
      default=False,
      action='store_true')
  return result


def RunCommand(*arguments, **kwargs):
  pattern = None
  if 'pattern' in kwargs:
    pattern = kwargs['pattern']
  expected_exit_code = 0
  if 'exit_code' in kwargs:
    expected_exit_code = kwargs['exit_code']
  stdout = subprocess.PIPE
  if 'verbose' in kwargs and kwargs['verbose']:
    print ' '.join(arguments)
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


EXECUTABLE = (stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR |
              stat.S_IRGRP | stat.S_IXGRP |
              stat.S_IROTH | stat.S_IXOTH)

def main():
  dart_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
  os.chdir(dart_dir)

  (options, args) = BuildOptions().parse_args()

  RunCommand('./tools/build.py', '--mode=release', 'dart2js')

  test_cmd = ['./tools/test.py', '--report', '--timeout=30',
              '--progress=color', '--mode=release', '--checked']

  if options.notest: return

  if args:
    if options.leg_only:
      test_cmd.extend('--compiler=dart2js', '--runtime=d8')
    else:
      test_cmd.extend('--compiler=frog,dart2js', '--runtime=d8')
    test_cmd.extend(args)
    RunCommand(*test_cmd, verbose=True)
  else:
    if not options.leg_only:
      # Run frog.py on the corelib tests, so we get some frog.py coverage.
      cmd = test_cmd + ['--compiler=frog', '--runtime=d8', 'corelib']
      RunCommand(*cmd, verbose=True)

      # Run frogium client tests. This is a pretty quick test but
      # tends to uncover different issues due to the size/complexity
      # of the DOM APIs.
      cmd = test_cmd + ['--compiler=frog', '--runtime=drt', 'client']
      RunCommand(*cmd, verbose=True)

      # Run frog on most of the tests.
      cmd = test_cmd + ['--compiler=frog', '--runtime=d8',
                        'language', 'corelib',
                        'isolate', 'peg', 'frog', 'css', 'frog_native']
      RunCommand(*cmd, verbose=True)

    # Run the "utils" tests which includes dartdoc. Frog/leg changes often
    # break dartdoc and this tries to catch those.
    cmd = test_cmd + ['--compiler=none', '--runtime=vm', 'utils']
    RunCommand(*cmd, verbose=True)

    # Run leg unit tests.
    cmd = test_cmd + ['--compiler=none', '--runtime=vm', 'leg']
    RunCommand(*cmd, verbose=True)

    # Leg does not implement checked mode yet.
    test_cmd.remove('--checked')

    cmd = test_cmd + ['--compiler=dart2js', '--runtime=d8',
                      'leg_only', 'frog_native']
    RunCommand(*cmd, verbose=True)

    # Run dart2js and legium on "built-in" tests.
    cmd = test_cmd + ['--compiler=dart2js', '--runtime=d8,drt']
    RunCommand(*cmd, verbose=True)


if __name__ == '__main__':
  try:
    sys.exit(main())
  except Error as e:
    sys.stderr.write('%s\n' % e)
    sys.exit(1)
