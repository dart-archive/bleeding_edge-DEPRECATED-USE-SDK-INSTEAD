#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# If we're on Windows, we don't have d8 as a dependency for frog, we'll only
# run with the VM.
import platform

def main(argv):
  if platform.system() != 'Windows':
    print '../third_party/v8/src/d8.gyp:d8'
if __name__ == '__main__':
  sys.exit(main(sys.argv))
