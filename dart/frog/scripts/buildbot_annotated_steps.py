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

# TODO(jmesserly): remove 'frogium' once builder is renamed
FROG_BUILDER = r'(frog|frogsh|frogium)-(linux|mac|windows)-(debug|release)'
WEB_BUILDER = r'web-(ie|ff|safari|chrome)-(win7|win8|mac|linux)(-(\d+))?'

NO_COLOR_ENV = dict(os.environ)
NO_COLOR_ENV['TERM'] = 'nocolor'

# Patterns are of the form "dart_client-linux-chromium-debug"
OLD_CLIENT_BUILDER = r'dart_client-(\w+)-chromium-(\w+)'

def GetBuildInfo():
  """Returns a tuple (name, mode, system) where:
    - name: 'frog', 'frogsh', 'frogium' or None when the builder has an
      incorrect name
    - mode: 'debug' or 'release'
    - system: 'linux', 'mac', or 'win7'
    - browser: 'ie', 'ff', 'safari', 'chrome'
  """
  name = None
  mode = None
  system = None
  browser = None
  builder_name = os.environ.get(BUILDER_NAME)
  if builder_name:

    frog_pattern = re.match(FROG_BUILDER, builder_name)
    web_pattern = re.match(WEB_BUILDER, builder_name)

    # TODO(jmesserly): remove this once builder is renamed
    old_client_pattern = re.match(OLD_CLIENT_BUILDER, builder_name)

    if frog_pattern:
      name = frog_pattern.group(1)
      system = frog_pattern.group(2)
      mode = frog_pattern.group(3)

      # TODO(jmesserly): remove this once builder is renamed
      if name == 'frogium':
        # Note: even though the browsers can run on more than one OS, we
        # found identical browser behavior across OS, so we're not running
        # everywhere for faster turnaround time. We're going to split different
        # browser+OS combinations into different bots.
        browsers = { 'windows': 'ie', 'mac': 'safari', 'linux': 'ff' }
        browser = browsers[system]
    elif web_pattern:
      name = 'frogium'
      mode = 'release'
      browser = web_pattern.group(1)
      system = web_pattern.group(2)

      # TODO(jmesserly): do we want to do anything different for the second IE
      # bot? For now we're using it to track down flakiness.
      number = web_pattern.group(4)

    elif old_client_pattern:
      name = 'frogium'
      system = old_client_pattern.group(1)
      mode = old_client_pattern.group(2)
      browser = 'chrome'

  # TODO(jmesserly): remove this once builder is renamed
  if name == 'frogium':
    mode = 'release'
  if system == 'windows':
    system = 'win7'

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
          os.path.join('..', 'tools', 'test.py'),
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
     - system: either 'linux', 'mac', or 'win7'
  """

  # Make sure we are in the frog directory
  os.chdir(FROG_PATH)

  print '@@@BUILD_STEP build frog@@@'

  args = [sys.executable, '../tools/build.py', '--mode=' + mode]

  # TODO(jmesserly): until "frogsh" is gone, make sure we don't try to build it
  # on bots. (They don't have node.js installed, so the build doesn't work.)
  if system == 'frogium':
    args.append('frog')

  return subprocess.call(args, env=NO_COLOR_ENV)


def TestFrog(arch, mode, system, browser, flags):
  """ test frog.
   Args:
     - arch: either 'leg', 'frog', 'frogsh' (frog self-hosted), or 'frogium'
     - mode: either 'debug' or 'release'
     - system: either 'linux', 'mac', or 'win7'
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

    # TODO(jmesserly): make DumpRenderTree more like other browser tests, so
    # we don't have this translation step. See dartbug.com/1158.
    # Ideally we can run most Chrome tests in DumpRenderTree because it's more
    # debuggable, but still have some tests run the full browser.
    if browser == 'chrome':
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
