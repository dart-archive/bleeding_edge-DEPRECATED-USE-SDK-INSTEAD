#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import os
import sys

def main(argv):
  dest = argv[1]
  ins = argv[2].split(' ')
  outs = [os.path.join(dest, x) for x in ins]
  print ' '.join(outs)
  return 0


if __name__ == '__main__':
  sys.exit(main(sys.argv))
