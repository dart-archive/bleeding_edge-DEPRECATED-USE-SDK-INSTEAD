# Copyright 2011 Google Inc. All Rights Reserved.

import os
import shutil
import stat
import sys

HOME = os.path.dirname(os.path.realpath(__file__))
HOME = os.path.join(HOME, os.pardir, os.pardir)

END_SCRIPT = '''
VM = '%s'
if __name__ == '__main__':
  sys.exit(main(sys.argv))
'''

def main(args):
  product_dir = args[1]
  vm = os.path.join(product_dir, 'dart')
  frog = os.path.join(product_dir, 'frog', 'bin', 'frog')

  # TODO(ngeoffray): Create a script that will run the VM in production mode.
  # if js_out.find('Release') != -1:
  shutil.copy(os.path.join(HOME, 'scripts', 'bootstrap', 'frog_wrapper.py'),
              frog)

  with open(frog, 'a+') as f:
    f.write(END_SCRIPT % vm)

  os.chmod(frog, stat.S_IXUSR | stat.S_IXGRP | stat.S_IRUSR |
           stat.S_IRGRP | stat.S_IWUSR)
  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
