#!/usr/bin/env python
#
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
import socket
import sys
import os
import tempfile
import subprocess
import imp
from os.path import join

DART_DIR = os.path.abspath(os.path.join(__file__, '..', '..', '..'))
utils = imp.load_source('utils', os.path.join(DART_DIR, 'tools', 'utils.py'))
build_dir = join(DART_DIR, 'xcodebuild/ReleaseX64/editor/') # TODO(messick) Generalize this
#build_dir = "/Users/messick/Desktop/editor-build-copy"


def ExtractTestName(line):
  (type, name) = line.split(',')
  return name.strip('\n')


# Create a work file to put socket data into while tests run
tmpfile = tempfile.NamedTemporaryFile(delete=False)
tmpfile_name = tmpfile.name

# Create a TCP/IP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# Bind the socket to the port
port = 11104
server_address = ('localhost', port)
sock.bind(server_address)
# Listen for incoming connections
sock.listen(1)

java = '/usr/bin/java' #TODO(messick) Generalize this
if not os.path.exists(java):
  sys.exit(2)
launcher = join(build_dir, 'plugins/org.eclipse.equinox.launcher_1.3.0.v20120522-1813.jar')
if not os.path.exists(launcher):
  sys.exit(3)

with utils.TempDir('swtbot') as workspace:

  # Start DartEditor in a sub process. Yes, all these args are needed to make SWTBot run.
  # SWTBot test results will be sent to the socket on the given port.
  cmd = [
       java, '-Xms256M', '-Xmx2048M', '-XX:MaxPermSize=256M', '-XstartOnFirstThread',
       '-Dorg.eclipse.swt.internal.carbon.smallFonts',
       '-jar', str(launcher),
       '-testLoaderClass', 'org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader',
       '-loaderpluginname', 'org.eclipse.jdt.junit4.runtime',
       '-application', 'org.eclipse.pde.junit.runtime.nonuithreadtestapplication',
       '-testpluginname', 'com.google.dart.tools.tests.swtbot_test',
       '-data', str(workspace),
       '-os', 'macosx', '-ws', 'cocoa', '-arch', 'x86_64',
       '-port', str(port),
       '-product', 'com.google.dart.tools.deploy.product',
       '-testApplication', 'com.google.dart.tools.deploy.application',
       '-classNames', 'com.google.dart.tools.tests.swtbot.test.TestAll'
       ]
  editor = subprocess.Popen(cmd)

  # Wait for a connection
  connection, client_address = sock.accept()
  try:
    # Receive the data in small chunks and save it
    while True:
      data = connection.recv(32)
      if data:
        tmpfile.write(data)
      else:
        break
          
  finally:
    # Clean up the connection
    connection.close()
    editor.terminate()

# Read the saved test results and print it in the format expected by the bot test harness.
#TODO(messck) This still needs some work.
tmpfile.close()
skip_lines = False
with open(tmpfile_name) as file:
  for line in file:
    if line.startswith('%'):
      if line.startswith('%ERROR') or line.startswith('%FAILED'):
        print ExtractTestName(line)
      if line.startswith('%TESTE'):
        print ExtractTestName(line),'pass'
      if line.startswith('%TESTF'):
        print ExtractTestName(line),'fail'
      continue
    if skip_lines:
      if line.startswith('\tat'):
        continue
      else:
        skip_lines = False
    else:
      # Ignore stack traces for things we don't care about
      if line.startswith('\tat sun.reflect.NativeMethod'):
        skip_lines = True
        continue
      if line.startswith('\tat java.lang.reflect.Method'):
        skip_lines = True
        continue
      if line.startswith('\tat junit.framework.TestCase.runTest'):
        skip_lines = True
        continue
      if line.startswith('\tat junit.framework.TestCase.runBare'):
        skip_lines = True
        continue
    print line.strip('\n')

try:
  os.remove(tmpfile_name)
except OSError:
  pass
