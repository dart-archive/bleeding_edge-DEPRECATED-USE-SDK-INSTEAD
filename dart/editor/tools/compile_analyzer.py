#!/usr/bin/env python
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
#
# Script to compile the analyzer.
#
# Usage: compile_analyzer.py OPTIONS files
#

import imp
import optparse
import os
import platform
import shutil
import subprocess
import sys

from os.path import join

def GetUtils():
  '''Dynamically load the tools/utils.py python module.'''
  dart_dir = os.path.abspath(os.path.join(__file__, '..', '..', '..'))
  return imp.load_source('utils', os.path.join(dart_dir, 'tools', 'utils.py'))

utils = GetUtils()

def GetOptions():
  options = optparse.OptionParser(usage='usage: %prog [options] <output>')
  options.add_option("--class_path_file",
      help='File describing the classpath in manifest style')
  options.add_option("--output_dir",
      help='Where to output files')
  options.add_option("--jar_file_name",
      help='Name of the resulting jar file')
  options.add_option("--jar_entry_directory",
      help='Which directory within output to pack into the jar files')
  options.add_option("--entry_point",
      help='The entry point for running the program.')
  options.add_option("--dependent_jar_files",
      help='The jar files that we link against, space separated.')
  return options.parse_args()

def CompileAnalyzer(options, args):
  # We rely on all jar files being copied to the output dir.
  if sys.platform == 'win32':
    class_path = options.output_dir + '*;'
  else:
    class_path = options.output_dir + '*'
  javac_path = VerifyJavacGetPath()
  cmd = [javac_path,
         '-sourcepath', 'foobar',
         '-source', '6',
         '-target', '6',
         '-implicit:none',
         '-encoding', 'UTF-8',
         '-d', options.output_dir,
         '-cp', class_path,
         ]
  cmd.extend(args)
  exit_code = subprocess.call(cmd)
  if exit_code:
    raise Exception("Executing command [%s] failed" % cmd)

def CreateJarFile(options):
  class_path_file_name = options.output_dir + options.class_path_file
  jar_file_name = options.output_dir + options.jar_file_name
  cmd = [GetJarToolPath(), 'cfem', jar_file_name, options.entry_point,
         class_path_file_name,
         '-C', options.output_dir, options.jar_entry_directory]
  exit_code = subprocess.call(cmd)
  if exit_code:
    raise Exception("Executing command [%s] failed" % cmd)

def CopyFiles(options):
  # Strip " from the string
  files = options.dependent_jar_files.replace('"', '')
  for f in files.split(" "):
    shutil.copy(f, options.output_dir)

def CreateManifestFile(options):
  class_path_file_name = options.output_dir + options.class_path_file
  with open(class_path_file_name, 'w') as output:
    # classpath
    print >> output, 'Class-Path:', '.',
    for r,d,f in os.walk(options.output_dir):
      for file in f:
        if file.endswith('.jar'):
          print >> output, file,
    print >> output

    # version
    print >> output, 'Implementation-Version: %s' % GetDartVersion()

def VerifyJavacGetPath():
  javac_path = GetJavacPath()
  try:
    subprocess.check_output([javac_path, "-version"])
  except:
    print "You do not have JDK installed, can't build the analyzer"
    exit(1)
  return javac_path
  
def GetJavacPath():
  if 'JAVA_HOME' in os.environ:
    return join(os.environ['JAVA_HOME'], 'bin',
                'javac' + GetExecutableExtension())
  else:
    return "javac"

def GetJarToolPath():
  if 'JAVA_HOME' in os.environ:
    return join(os.environ['JAVA_HOME'], 'bin', 'jar' + GetExecutableExtension())
  else:
    return "jar"

def GetExecutableExtension():
  id = platform.system()
  if id == "Windows" or id == "Microsoft":
    return '.exe'
  else:
    return ''

def GetDartVersion():
  # 0.1.2.0_r13661
  return utils.GetVersion()

def main():
  (options, args) = GetOptions()
  # Clean out everything whenever we do a build, guarantees that we don't have
  # any leftover jar files.
  shutil.rmtree(options.output_dir, ignore_errors=True)
  os.makedirs(options.output_dir)

  CopyFiles(options)
  CreateManifestFile(options)
  CompileAnalyzer(options, args)
  CreateJarFile(options)


if __name__ == '__main__':
  main()
