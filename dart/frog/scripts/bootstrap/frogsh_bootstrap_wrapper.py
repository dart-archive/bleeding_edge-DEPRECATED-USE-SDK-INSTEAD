# Copyright 2011 Google Inc. All Rights Reserved.

import os
import shutil
import stat
import sys

HOME = os.path.dirname(os.path.realpath(__file__))
HOME = os.path.join(HOME, os.pardir, os.pardir)

sys.path.append(HOME)
import frog

def main(args):
  for arg in args:
    index = arg.find('--out=')
    if index != -1:
      js_out = arg[6:len(arg)];

  # TODO(ngeoffray): Compile frogsh without checks integrated.
  # if js_out.find('Release') != -1:
  exit_code = frog.main(args)
  if exit_code:
    return exit_code

  os.chmod(js_out, stat.S_IXUSR | stat.S_IXGRP | stat.S_IRUSR |
           stat.S_IRGRP | stat.S_IWUSR)
  return exit_code


if __name__ == '__main__':
  sys.exit(main(sys.argv))
