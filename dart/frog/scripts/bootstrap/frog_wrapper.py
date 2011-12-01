#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import imp
import os
import sys

def main(args):
  # Try to find frog.py from the current location.
  home = os.path.join(os.curdir, 'frog.py')
  if not os.path.exists(home):
    home = os.path.join(os.curdir, 'frog', 'frog.py')

  if not os.path.exists(home):
    print "Could not find frog"
    return 1

  frog_args = ['frog.py', '--vm=' + VM]
  frog_args += args[2:len(args)]

  filename = None
  exit_code = 1
  try:
    # Load frog.py and invoke it.
    paths = [os.path.dirname(home)]
    (filename, pathname, description) = imp.find_module('frog', paths)
    module = imp.load_module('frog', filename, pathname, description)
    exit_code = module.main(args)
  finally:
    if filename:
      filename.close()

  return exit_code
