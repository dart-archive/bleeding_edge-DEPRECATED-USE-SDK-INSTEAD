#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import imp
import os
import sys

def main(args):
  home = os.path.join(HOME, 'frog.py')

  if not os.path.exists(home):
    print "Could not find %s" % home
    return 1

  frog_args = [ 'frog.py', '--vm=%s' % VM]
  js_cmd_flag = '--js_cmd=%s --crankshaft' % D8
  vm_flags = None
  for arg in args:
    if arg.startswith('--js_cmd'):
      js_cmd_flag = arg
    if arg.startswith('--vm_flags'):
      vm_flags = arg
  if js_cmd_flag in args:
    args.remove(js_cmd_flag)
  if vm_flags in args:
    args.remove(vm_flags)
  frog_args.append(js_cmd_flag)
  if vm_flags:
    frog_args.append(vm_flags)
  frog_args.append('--')
  frog_args.extend(args[1:])

  filename = None
  exit_code = 1
  try:
    # Load frog.py and invoke it.
    paths = [os.path.dirname(home)]
    (filename, pathname, description) = imp.find_module('frog', paths)
    module = imp.load_module('frog', filename, pathname, description)
    exit_code = module.main(frog_args)
  finally:
    if filename:
      filename.close()

  return exit_code
