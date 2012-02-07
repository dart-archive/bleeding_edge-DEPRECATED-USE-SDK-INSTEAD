# Copyright 2011 Google Inc. All Rights Reserved.

import os
import platform
import shutil
import stat
import sys

HOME = os.path.dirname(os.path.realpath(__file__))
HOME = os.path.join(HOME, os.pardir, os.pardir)

END_SCRIPT = '''
VM = '%s'
VM_FLAGS = '%s'
if __name__ == '__main__':
  sys.exit(main(sys.argv))
'''

def main(args):
  product_dir = args[1]
  vm = os.path.join(product_dir, 'dart')
  id = platform.system()
  if id == 'Windows' or id == 'Microsoft':
    vm = vm + '.exe'
  frog = os.path.join(product_dir, 'frog', 'bin', 'frog')

  shutil.copy(os.path.join(HOME, 'scripts', 'bootstrap', 'frog_wrapper.py'),
              frog)

  if 'Release' in product_dir:
    # We want to run our tests on the VM in production mode and in
    # developer mode. It turns out that developer mode is really slow
    # on the debug VM, so it makes sense to make the pairings:
    # (release VM, developer mode) and (debug VM, production mode).
    vm_flags = '--vm_flags=--enable_asserts --enable_type_checks'
  else:
    vm_flags = ''

  with open(frog, 'a+') as f:
    f.write(END_SCRIPT % (vm, vm_flags))

  os.chmod(frog, stat.S_IXUSR | stat.S_IXGRP | stat.S_IRUSR |
           stat.S_IRGRP | stat.S_IWUSR)
  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
