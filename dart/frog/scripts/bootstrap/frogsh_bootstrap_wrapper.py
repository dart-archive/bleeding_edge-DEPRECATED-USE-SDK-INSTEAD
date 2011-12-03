# Copyright 2011 Google Inc. All Rights Reserved.

import os
import shutil
import stat
import sys

HOME = os.path.dirname(os.path.realpath(__file__))
HOME = os.path.join(HOME, os.pardir, os.pardir)

FROGSH_FALLBACK = """#!/bin/sh
echo -en "\033[31mERROR\033[0m: Building frogsh in 'debug' mode requires "
echo "building the dart"
echo "runtime in 'release' mode. Retry building frogsh as follows:"
echo " > rm $0"
echo " > ./tools/build.py -m release runtime"
echo " > ./tools/build.py -m debug frogsh"
exit 1
"""

sys.path.append(HOME)
import frog

def main(args):
  product_dir = args[1]
  js_out = os.path.join(product_dir, 'frog', 'bin', 'frogsh')
  vm = os.path.join(product_dir, 'dart')
  frog_args = ['frog.py', '--vm=' + vm, '--', '--out=' + js_out, 'frog.dart']

  # TODO(ngeoffray): Compile frogsh without checks integrated.
  # if js_out.find('Release') != -1:
  exit_code = frog.main(frog_args)
  if exit_code:
    if js_out.find('Release') != -1:
      return exit_code
    else:
      with open(js_out, 'w') as f:
        f.write(FROGSH_FALLBACK)

  os.chmod(js_out, stat.S_IXUSR | stat.S_IXGRP | stat.S_IRUSR |
           stat.S_IRGRP | stat.S_IWUSR)
  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
