# Copyright 2011 Google Inc. All Rights Reserved.

import os
import platform
import shutil
import stat
import sys

DART_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
    os.pardir, os.pardir, os.pardir)
FROG_DIR = os.path.join(DART_DIR, "frog")

sys.path.append(FROG_DIR)
import frog

# Runs frog.dart on the dart vm to compile frogpad.dart to frogpad.js
def main(args):
  product_dir = args[1]
  frogpad_js = os.path.join(product_dir, 'frog', 'bin', 'frogpad.js')

  if platform.system() == "Windows":
    with open(frogpad_js, 'w') as f:
      f.write("frogpad is not supported on Windows")
      return 0

  vm = os.path.join(product_dir, 'dart')
  frogpad_dart = os.path.join(
     DART_DIR, 'tools', 'testing', 'frogpad', 'frogpad.dart')

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
