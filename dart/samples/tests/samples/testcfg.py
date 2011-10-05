# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import os
import test
from os.path import join, exists
import testing

class SamplesTestConfiguration(testing.StandardTestConfiguration):
  def __init__(self, context, root):
    super(SamplesTestConfiguration, self).__init__(context, root)

  def ListTests(self, current_path, path, mode, arch):
    tests = []
    src_dir = join(self.root[:-7], "src")
    for root, dirs, files in os.walk(src_dir):
      ignore_dirs = [d for d in dirs if d.startswith('.')]
      for d in ignore_dirs:
        dirs.remove(d)
      for f in [x for x in files if self.IsTest(x)]:
        test_path = current_path + [ f[:-5] ]  # Remove .dart suffix.
        if not self.Contains(path, test_path):
          continue
        tests.append(testing.StandardTestCase(self.context,
                                              test_path,
                                              join(root, f),
                                              mode,
                                              arch))
    return tests

  def IsTest(self, name):
    return name.endswith('_test.dart')

def GetConfiguration(context, root):
  return SamplesTestConfiguration(context, root)
