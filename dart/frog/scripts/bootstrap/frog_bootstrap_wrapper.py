# Copyright 2012 Google Inc. All Rights Reserved.

import os
import platform
import shutil
import stat
import sys

HOME = os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.realpath(__file__))))
BUILDER_NAME = os.environ.get('BUILDBOT_BUILDERNAME')

END_SCRIPT = '''
VM = r'%s'
D8 = r'%s'
HOME = r'%s'
if __name__ == '__main__':
  sys.exit(main(sys.argv))
'''

def main(args):
  product_dir = args[1]
  vm = os.path.join(product_dir, 'dart')
  d8 = os.path.join(product_dir, 'd8')
  id = platform.system()
  if id == 'Windows' or id == 'Microsoft':
    vm = vm + '.exe'
  frog = os.path.join(product_dir, 'frog', 'bin', 'frog')

  shutil.copy(os.path.join(HOME, 'scripts', 'bootstrap', 'frog_wrapper.py'),
              frog)
  shutil.copy(os.path.join(HOME, 'scripts', 'bootstrap', 'frog.bat'),
              frog + '.bat')

  with open(frog, 'a+') as f:
    f.write(END_SCRIPT % (vm, d8, HOME))

  os.chmod(frog, stat.S_IXUSR | stat.S_IXGRP | stat.S_IRUSR |
           stat.S_IRGRP | stat.S_IWUSR)
  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
