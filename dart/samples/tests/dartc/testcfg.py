# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

from testing import test_configuration

def GetConfiguration(context, root):
  return SampleCompilationTestConfiguration(context, root)


class  SampleCompilationTestConfiguration(test_configuration.CompilationTestConfiguration):
 def __init__(self, context, root):
    super(SampleCompilationTestConfiguration, self).__init__(context, root)

 def SourceDirs(self):
   """ Returns a list of directories to scan for files to compile """
   return [ 'chat', 'clock', 'socket' ]
