#!/usr/bin/env python
#
# Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import imp
import socket
import sys
import os
import subprocess
from os.path import join

DART_DIR = os.path.abspath(os.path.join(__file__, '..', '..', '..'))
utils = imp.load_source('utils', os.path.join(DART_DIR, 'tools', 'utils.py'))

# Define envar DEBUG_SWTBOT_RUNNER to print output from the test runner.

def IsWindows():
  return utils.GuessOS() == 'win32'


def ExtractTestName(line):
  (type, name) = line.split(',')
  return name.strip('\n')


def ExecTestRunner(tempDir):
  """
  Exec the SWTBot test runner. Return (status, filename). Status will be
  zero on success, non-zero if some essential component cannot be found.
  Filename will be None on error, or the name of a file in the tempDir
  to which output from the test runner was saved.
  """
  buildroot = utils.GetBuildRoot(utils.GuessOS(), 'release', 'x64')
  build_dir = join(DART_DIR, buildroot, 'editor')
  
  # Create a work file to put socket data into while tests run
  tmpfile = open(join(str(tempDir), 'output'), 'w')
  tmpfile_name = tmpfile.name
  
  # Create a TCP/IP socket
  sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
  # Bind the socket to the port
  port = 11104
  server_address = ('localhost', port)
  sock.bind(server_address)
  # Listen for incoming connections
  sock.listen(1)

  java = '/usr/bin/java'
  try:
    javahome = os.environ('JAVA_HOME')
    java = join(java, 'bin', 'java')
  except:
    pass
  if not os.path.exists(java):
    print 'Cannot find java vm'
    return 2, None
  jarpath = 'plugins/org.eclipse.equinox.launcher_1.3.0.v20120522-1813.jar'
  launcher = join(build_dir, jarpath)
  if not os.path.exists(launcher):
    print 'Cannot find eclipse launcher'
    return 3, None
  
  with utils.TempDir('swtbot') as workspace:
  
    # Start DartEditor in a sub process.
    # SWTBot test results will be sent to the socket on the given port.
    cmd = [
         java, '-Xms256M', '-Xmx2048M', '-XX:MaxPermSize=256M',
         '-XstartOnFirstThread',
         '-Dorg.eclipse.swt.internal.carbon.smallFonts',
         '-jar', str(launcher),
         '-testLoaderClass',
         'org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader',
         '-loaderpluginname', 'org.eclipse.jdt.junit4.runtime',
         '-application',
         'org.eclipse.pde.junit.runtime.nonuithreadtestapplication',
         '-testpluginname', 'com.google.dart.tools.tests.swtbot_test',
         '-data', str(workspace),
         '-os', 'macosx', '-ws', 'cocoa', '-arch', 'x86_64',
         '-port', str(port),
         '-product', 'com.google.dart.tools.deploy.product',
         '-testApplication', 'com.google.dart.tools.deploy.application',
         '-classNames', 'com.google.dart.tools.tests.swtbot.test.TestAll'
         ]
    editorOutputFile = open(join(str(tempDir), 'editorOutput'), 'w')
    editor = subprocess.Popen(cmd, stdout=editorOutputFile.fileno(),
                              stderr=subprocess.STDOUT, shell=IsWindows())
  
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
      tmpfile.close()
      editorOutputFile.close()
      editor.terminate()

  if os.getenv('DEBUG_SWTBOT_RUNNER'):
    out = open(editorOutputFile.name, 'r')
    print out.readlines()
    out.close()
  return 0, tmpfile_name
    

def ProcessTestOutput(tmpfile_name):
  """
  Read the saved test results and print it in the format
  expected by the bot test harness.
  """
  status = 0
  skip_lines = False
  with open(tmpfile_name) as file:
    for line in file:
      if line.startswith('%'):
        if line.startswith('%ERROR') or line.startswith('%FAILED'):
          status = 1
          print ExtractTestName(line)
        if line.startswith('%TESTE'):
          print ExtractTestName(line),'pass'
        if line.startswith('%TESTF'):
          status = 1
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


def Main():
  with utils.TempDir('swtbottest') as tmpdir:
    (status, filename) = ExecTestRunner(tmpdir)
    if status:
      return status
    return ProcessTestOutput(filename)


if __name__ == '__main__':
  sys.exit(Main())
