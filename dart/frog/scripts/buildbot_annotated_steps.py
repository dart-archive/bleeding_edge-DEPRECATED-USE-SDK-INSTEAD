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

BUILDER_PATTERN = r'(frog|frogsh|frogium)-(linux|mac|windows)-(debug|release)'

NO_COLOR_ENV = dict(os.environ)
NO_COLOR_ENV['TERM'] = 'nocolor'

def GetBuildInfo():
  """Returns a tuple (name, mode, system) where:
    - name: 'frog', 'frogsh', or None when the builder has an incorrect name
    - mode: 'debug' or 'release'
    - system: 'linux', 'mac', or 'windows'
  """
  name = None
  mode = None
  system = None
  builder_name = os.environ.get(BUILDER_NAME)
  if builder_name:
    pattern = re.match(BUILDER_PATTERN, builder_name)
    if pattern:
      name = pattern.group(1)
      system = pattern.group(2)
      mode = pattern.group(3)
  return (name, mode, system)


def TestStep(name, mode, system, component, targets, flags):
  print '@@@BUILD_STEP %s tests: %s %s@@@' % (name, component, flags)
  sys.stdout.flush()
  if (component == 'frogium' or component == 'webdriver') and system == 'linux':
    cmd = ['xvfb-run', '-a']
  else:
    cmd = []

  num_tasks = 8
  if system == 'windows':
    # TODO(efortuna): This is a temporary measure because webdriver isn't
    # threadsafe(!), and the results seem to be most obvious/problematic on 
    # Windows.
    num_tasks = 1
  
  cmd = (cmd
      + [sys.executable,
          os.path.join('..', 'tools', 'test_wrapper.py'),
          '--mode=' + mode,
          '--component=' + component,
          '--time',
          '--report',
          '-j%d' % num_tasks,
          '--progress=buildbot',
          '-v']
      + targets)
  if flags:
    cmd.extend(flags)

  exit_code = subprocess.call(cmd, env=NO_COLOR_ENV)
  if exit_code != 0:
    print '@@@STEP_FAILURE@@@'
  return exit_code


def BuildFrog(arch, mode, system):
  """ build frog.
   Args:
     - arch: either 'leg', 'frog', 'frogsh' (frog self-hosted), or 'frogium'
     - mode: either 'debug' or 'release'
     - system: either 'linux', 'mac', or 'windows'
  """

  # Make sure we are in the frog directory
  os.chdir(FROG_PATH)

  print '@@@BUILD_STEP build frog@@@'
  return subprocess.call(
      [sys.executable, '../tools/build.py', '--mode=' + mode],
      env=NO_COLOR_ENV)


def TestFrog(arch, mode, system, flags):
  """ test frog.
   Args:
     - arch: either 'leg', 'frog', 'frogsh' (frog self-hosted), or 'frogium'
     - mode: either 'debug' or 'release'
     - system: either 'linux', 'mac', or 'windows'
     - flags: extra flags to pass to test.dart
  """

  # Make sure we are in the frog directory
  os.chdir(FROG_PATH)

  if arch != 'frogium': # frog and frogsh
    TestStep("frog", mode, system, arch, [], flags)
    TestStep("frog_extra", mode, system,
        arch, ['frog', 'peg', 'css'], flags)

    if arch == 'frogsh':
      # There is no need to run these tests both for frog and frogsh.

      TestStep("leg", mode, system, 'leg', [], flags)
      TestStep("leg_extra", mode, system, 'leg', ['leg_only'], flags)
      # Leg isn't self-hosted (yet) so we run the leg unit tests on the VM.
      TestStep("leg_extra", mode, system, 'vm', ['leg'], flags)

  else:
    tests = ['client', 'language', 'corelib', 'isolate', 'frog', 'peg', 'css']
    # TODO(efortuna): Eventually we want DumpRenderTree to run on all systems,
    # but for test turnaround time, currently it is only running on linux.
    if system == 'linux':
      # DumpRenderTree tests (DRT is currently not available on Windows):
      TestStep("browser", mode, system, 'frogium', tests, flags)

    # Webdriver tests. Even though the browsers can run on more than one OS, we
    # found identical browser behavior across OS, so we're not running
    # everywhere for faster turnaround time.
    if system == 'linux':
      browsers = ['ff']
    elif system == 'mac':
      browsers = ['safari']
    else:
      # TODO(efortuna): Use both ff and ie once we have additional buildbots.
      # We're using just IE for speed on our testing right now.
      browsers = ['ie'] #['ff', 'ie']
      

    for browser in browsers:
      TestStep(browser, mode, system, 'webdriver', tests,
          flags + ['--browser=' + browser])

  return 0


def main():
  print 'main'
  if len(sys.argv) == 0:
    print 'Script pathname not known, giving up.'
    return 1

  arch, mode, system = GetBuildInfo()
  print "arch: %s, mode: %s, system: %s" % (arch, mode, system)
  if arch is None:
    return 1

  status = BuildFrog(arch, mode, system)
  if status != 0:
    print '@@@STEP_FAILURE@@@'
    return status

  if arch != 'frogium':
    status = TestFrog(arch, mode, system, [])
    if status != 0:
      print '@@@STEP_FAILURE@@@'
      return status

  status = TestFrog(arch, mode, system, ['--checked'])
  if status != 0:
    print '@@@STEP_FAILURE@@@'

  return status


if __name__ == '__main__':
  sys.exit(main())
