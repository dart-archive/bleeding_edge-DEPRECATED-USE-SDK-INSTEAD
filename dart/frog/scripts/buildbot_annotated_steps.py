#!/usr/bin/python

# Copyright (c) 2011 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""Dart frog buildbot steps

Runs tests for the frog compiler (running on the vm or the self-hosting version)
"""

import os
import re
import subprocess
import sys

BUILDER_NAME = 'BUILDBOT_BUILDERNAME'

FROG_PATH = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

BUILDER_PATTERN = r'(frog|frogsh|frogium)-linux-(debug|release)'

NO_COLOR_ENV = dict(os.environ)
NO_COLOR_ENV['TERM'] = 'nocolor'

def GetBuildInfo():
  """Returns a tuple (name, mode) where:
    - name: 'frog', 'frogsh', or None when the builder has an incorrect name
    - mode: 'debug' or 'release'
  """
  name = None
  mode = None
  builder_name = os.environ.get(BUILDER_NAME)
  if builder_name:
    pattern = re.match(BUILDER_PATTERN, builder_name)
    if pattern:
      name = pattern.group(1)
      mode = pattern.group(2)
  return (name, mode)

# TODO(sigmund): delete this convertion when test.py uses the same
# configuration we do here.
def ConvertConfiguration(arch, mode):
  ''' Convert arch/mode into modes/flags for test.py '''
  # TODO(ngeoffray): do something meaningful for debug.
  testpy_mode = 'release'
  flags = None
  if mode == 'debug':
    flags = '--checked'
  return (testpy_mode, flags)

def TestStep(name, mode, component, targets, flags):
  print '@@@BUILD_STEP %s tests: %s@@@' % (name, component)
  if component == 'frogium':
    cmd = ['xvfb-run']
  else:
    cmd = []

  cmd = (cmd
      + [sys.executable,
          '../tools/test_wrapper.py',
          '--mode=' + mode,
          '--component=' + component,
          '--time',
          '--report',
          '--progress=buildbot',
          '-v']
      + targets)
  if flags:
    cmd.append(flags)
  exit_code = subprocess.call(cmd, env=NO_COLOR_ENV)
  if exit_code != 0:
    print '@@@STEP_FAILURE@@@'
  return exit_code

def TestFrog(arch, mode):
  """ build and test frog.
   Args:
     - arch: either 'frog', 'frogsh' (frog self-hosted), or 'frogium'
     - mode: either 'debug' (with type checks) or 'release' (without)
  """

  # Make sure we are in the frog directory
  os.chdir(FROG_PATH)
  testpy_mode, flags = ConvertConfiguration(arch, mode)

  print '@@@BUILD_STEP build frog@@@'
  if subprocess.call(
      [sys.executable, '../tools/build.py', '--mode=' + testpy_mode],
      env=NO_COLOR_ENV) != 0:
    return 1

  if arch != 'frogium': # frog and frogsh
    TestStep("frog", testpy_mode, arch, [], flags)
    TestStep("frog_extra", testpy_mode, arch, ['frog', 'peg', 'css'], flags)

    TestStep("leg_extra", testpy_mode, arch, ['leg', 'leg_only'], flags)

    if arch == 'frogsh':
      # There is no need to run these tests both for frog and frogsh.

      TestStep("leg", testpy_mode, 'leg', [], flags)
      TestStep("leg_extra", testpy_mode, 'leg', ['leg_only'], flags)
      # Leg isn't self-hosted (yet) so we run the leg unit tests on the VM.
      TestStep("leg_extra", testpy_mode, 'vm', ['leg'], flags)

  else:
    if (TestStep("browser", testpy_mode, 'frogium',
          ['client', 'language', 'corelib', 'isolate', 'frog',
           'leg', 'peg', 'css'], flags) != 0):
      return 1

  return 0

def main():
  print 'main'
  if len(sys.argv) == 0:
    print 'Script pathname not known, giving up.'
    return 1

  arch, mode = GetBuildInfo()
  print "arch: %s, mode: %s" % (arch, mode)
  if arch is None:
    return 1

  status = TestFrog(arch, mode)
  if status != 0:
    print '@@@STEP_FAILURE@@@'
  return status


if __name__ == '__main__':
  sys.exit(main())
