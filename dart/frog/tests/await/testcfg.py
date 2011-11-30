# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

from testing import test_configuration
import os.path

FROG_DIR = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
AWAITC_LOCATION = os.path.join(FROG_DIR, 'await', 'awaitc.dart')

class AwaitConfiguration(test_configuration.StandardTestConfiguration):
  """
    Defines a configuration that runs an await-aware frog compiler on
    each test case.
  """
  # TODO(sigmund): This configuration should eventually be removed when we have
  # a generic way to run preprocessors in the test infrastructure.
  def __init__(self, context, root, fatal_static_type_errors=False):
    super(AwaitConfiguration, self).__init__(context, root)
    self.fatal_static_type_errors = fatal_static_type_errors

  def ListTests(self, current_path, path, mode, arch, component):
    """Uses the default listing of test cases, but modifies the architecture to
    use the await-aware frog compiler."""
    tests = super(AwaitConfiguration, self).ListTests(
        current_path, path, mode, arch, component)
    for test in tests:
      test.run_arch.vm_options.append(AWAITC_LOCATION)
    return tests

def GetConfiguration(context, root):
  return AwaitConfiguration(context, root)
