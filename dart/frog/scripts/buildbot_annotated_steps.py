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

# Patterns are of the form "dart_client-linux-chromium-debug"
OLD_BUILDER = r'dart_client-(\w+)-chromium-(\w+)'

def GetBuildInfo():
  """Returns a tuple (name, mode, system) where:
    - name: 'frog', 'frogsh', 'frogium' or None when the builder has an
      incorrect name
    - mode: 'debug' or 'release'
    - system: 'linux', 'mac', or 'windows'
    - browser: 'ie', 'ff', 'safari', 'chrome', 'chrome_drt'
  """
  name = None
  mode = None
  system = None
  browser = None
  builder_name = os.environ.get(BUILDER_NAME)
  if builder_name:
    pattern = re.match(BUILDER_PATTERN, builder_name)
    if pattern:
      name = pattern.group(1)
      system = pattern.group(2)
      mode = pattern.group(3)

      # TODO(jmesserly): move this logic into the builder names
      if name == 'frogium':
        # Note: even though the browsers can run on more than one OS, we
        # found identical browser behavior across OS, so we're not running
        # everywhere for faster turnaround time. We're going to split different
        # browser+OS combinations into different bots.
        browsers = { 'windows': 'ie', 'mac': 'safari', 'linux': 'ff' }
        browser = browsers[system]
    else:
      # TODO(jmesserly): remove this once builder is renamed
      pattern = re.match(OLD_BUILDER, builder_name)
      if pattern:
        name = 'frogium'
        system = pattern.group(1)
        mode = pattern.group(2)
        browser = 'chrome_drt'

  # TODO(jmesserly): rename the frogium bots so we don't need this
  if name == 'frogium':
    mode = 'release'

  return (name, mode, system, browser)


def TestStep(name, mode, system, component, targets, flags):
  print '@@@BUILD_STEP %s tests: %s %s@@@' % (name, component, flags)
  sys.stdout.flush()
  if (component == 'frogium' or component == 'webdriver') and system == 'linux':
    cmd = ['xvfb-run', '-a']
  else:
    cmd = []

  cmd = (cmd
      + [sys.executable,
          os.path.join('..', 'tools', 'test_wrapper.py'),
          '--mode=' + mode,
          '--component=' + component,
          '--time',
          '--report',
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


def TestFrog(arch, mode, system, browser, flags):
  """ test frog.
   Args:
     - arch: either 'leg', 'frog', 'frogsh' (frog self-hosted), or 'frogium'
     - mode: either 'debug' or 'release'
     - system: either 'linux', 'mac', or 'windows'
     - browser: one of the browsers, see GetBuildInfo
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

    if browser == 'chrome_drt':
      # TODO(jmesserly): make DumpRenderTree more like other browser tests, so
      # we don't have this translation step. See dartbug.com/1158
      TestStep('browser', mode, system, 'frogium', tests, flags)
    else:
      TestStep(browser, mode, system, 'webdriver', tests,
          flags + ['--browser=' + browser])

  return 0


def main():
  print 'main'
  if len(sys.argv) == 0:
    print 'Script pathname not known, giving up.'
    return 1

  arch, mode, system, browser = GetBuildInfo()
  print "arch: %s, mode: %s, system: %s, browser %s" % (arch, mode, system,
      browser)
  if arch is None:
    return 1

  status = BuildFrog(arch, mode, system)
  if status != 0:
    print '@@@STEP_FAILURE@@@'
    return status

  if arch != 'frogium':
    status = TestFrog(arch, mode, system, browser, [])
    if status != 0:
      print '@@@STEP_FAILURE@@@'
      return status

  status = TestFrog(arch, mode, system, browser, ['--checked'])
  if status != 0:
    print '@@@STEP_FAILURE@@@'

  return status


if __name__ == '__main__':
  sys.exit(main())
