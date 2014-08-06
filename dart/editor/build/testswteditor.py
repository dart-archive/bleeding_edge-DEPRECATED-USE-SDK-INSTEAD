#!/usr/bin/env python
#
# Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import imp
import os
import os.path
import shutil
import subprocess
import sys
from os.path import join, abspath

DART_DIR = os.path.abspath(os.path.join(__file__, '..', '..', '..'))
utils = imp.load_source('utils', os.path.join(DART_DIR, 'tools', 'utils.py'))


def StartBuildStep(name):
  print "@@@BUILD_STEP %s@@@" % name
  sys.stdout.flush()


def BuildStepFailure():
  print '@@@STEP_FAILURE@@@'
  sys.stdout.flush()


def IsWindows():
  return utils.GuessOS() == 'win32'


def ExecuteCommandWithoutException(cmd):
  """Execute a command in a subprocess."""
  print 'Executing: ' + ' '.join(cmd)
  pipe = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
      shell=IsWindows())
  output = pipe.communicate()
#   if pipe.returncode != 0:
#     raise Exception('Execution failed: ' + str(output))
  return pipe.returncode, output


def edit_product(oldname, newname):
  # Modify the product name in the build.properties file of the releng feature
  relengpath = join(DART_DIR, 'editor', 'tools', 'features',
                            'com.google.dart.tools.deploy.feature_releng')
  propertiesfile = join(relengpath, 'build.properties')
  file = open(propertiesfile, 'r')
  text = file.read()
  file.close()
  file = open(propertiesfile, 'w')
  file.write(text.replace(oldname, newname))
  file.flush()
  file.close()


def build():
  # Run the build script and wait for it to finish
  StartBuildStep('build-editor-swt')
  cwd = os.getcwd()
  os.chdir(DART_DIR)
  buildScript = join(DART_DIR, 'tools', 'build.py')
  # Build takes way too long if arch is not defined.
  # Can't use utils.GuessArchitecture() because it returns the wrong value.
  cmd = [sys.executable, buildScript, '-mrelease', '-ax64', 'editor']
  rtncode, _ = utils.ExecuteCommand(cmd)
  os.chdir(cwd)
  if rtncode:
    StepFailure()
  return rtncode


def run():
  # Run the runswtbot script and wait for it to finish
  StartBuildStep('run-editor-swt')
  builddir = join(DART_DIR, 'editor', 'build')
  runScript = join(builddir, 'runswtbot.py')
  cmd = [sys.executable, runScript]
  # Need to print the output even if script fails
  rtncode, results = ExecuteCommandWithoutException(cmd)
  stdout, stderr = results
  print stdout.replace('\\n', '\n')
  if rtncode:
    # TODO(messick) Preserve the screenshots
    shutil.rmtree('screenshots', ignore_errors=True)
    BuildStepFailure()
  return rtncode


def main():
  # Build and run the SWTBot version of Dart Editor.
  edit_product('dart_feature.product', 'dart_test.product')
  rtncode = build()
  edit_product('dart_test.product', 'dart_feature.product')
  if rtncode == 0:
    return run()


if __name__ == '__main__':
  sys.exit(main())
