#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# traverses frog and lists all possible sources that should be marked as
# dependencies for building frog.

import os
import sys

def main(argv):
  if len(argv) == 1:
    dir = os.curdir
  else:
    dir = argv[1]
  for root, directories, files in os.walk(dir):
    if root == os.curdir:
      directories[0:] = ['leg', 'lib',
                         os.path.join('..', 'client', 'dom', 'frog'),
                         os.path.join('..', 'client', 'html', 'release')]
    for filename in files:
      if filename.endswith(('.dart', '.gypi', '.js')):
        print os.path.relpath(os.path.join(root, filename))

if __name__ == '__main__':
  sys.exit(main(sys.argv))
