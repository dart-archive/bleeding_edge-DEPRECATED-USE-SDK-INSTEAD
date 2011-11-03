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
  js_out = args[1]

  # TODO(ngeoffray): Create a script that will run the VM in production mode.
  # if js_out.find('Release') != -1:
  shutil.copy(os.path.join(HOME, 'scripts', 'bootstrap', 'frog_wrapper.py'),
              js_out)
  os.chmod(js_out, stat.S_IXUSR | stat.S_IXGRP | stat.S_IRUSR |
           stat.S_IRGRP | stat.S_IWUSR)
  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
