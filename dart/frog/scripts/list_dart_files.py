#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import os
import sys

def main(argv):
  for root, directories, files in os.walk(os.curdir):
    if root == os.curdir:
      directories[0:] = ['leg']
    for filename in files:
      if filename.endswith('.dart'):
        print os.path.relpath(os.path.join(root, filename))

if __name__ == '__main__':
  sys.exit(main(sys.argv))
