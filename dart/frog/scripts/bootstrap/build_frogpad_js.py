# Copyright 2011 Google Inc. All Rights Reserved.

import os
import platform
import shutil
import stat
import sys

HOME = os.path.dirname(os.path.realpath(__file__))
HOME = os.path.join(HOME, os.pardir, os.pardir)

sys.path.append(HOME)
import frog

# Runs frog.dart on the dart vm to compile frogpad.dart to frogpad.js
def main(args):
  if len(args) >= 2:
    product_dir = args[1]
  else:
    product_dir = os.path.abspath(os.path.join(os.path.dirname(__file__),
        '..', '..', '..', 'out/Release_ia32'))
    print product_dir

  vm = os.path.join(product_dir, 'dart')
  frogpad_dart = os.path.join(product_dir, '..', '..',
      'tools', 'testing',  'frogpad', 'frogpad.dart')
  frogpad_js = os.path.join(product_dir, 'frog', 'bin', 'frogpad.js')

  if not os.path.exists(vm):
    raise Exception("cannot find dart vm '%s'" % vm)

  args = ['frog.py',
          '--verbose',
          '--vm=' + vm,
          '--',
          '--out=' + frogpad_js,
          frogpad_dart]

  exit_code = frog.main(args)
  if exit_code:
    return exit_code

  if not os.path.exists(frogpad_js):
    raise Exception("didn't generate '%s'" % frogpad_js)

  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
