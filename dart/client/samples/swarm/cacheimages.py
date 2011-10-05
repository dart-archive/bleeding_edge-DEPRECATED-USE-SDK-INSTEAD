#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
'''
This script finds all HTML pages in a folder and downloads all images, replacing
the urls with local ones.
'''
import os, sys
from os.path import abspath, basename, dirname, join

SWARM_PATH = dirname(abspath(__file__))
CLIENT_PATH = dirname(dirname(SWARM_PATH))
CLIENT_TOOLS_PATH = join(CLIENT_PATH, 'tools')

# Add the client tools directory so we can find htmlconverter.py.
sys.path.append(CLIENT_TOOLS_PATH)
import htmlconverter

def main():
  if len(sys.argv) < 2 or 'help' in sys.argv[1]:
    print 'Usage: %s DIRECTORY' % basename(sys.argv[0])
    return 1

  dirname = sys.argv[1]
  print 'Searching directory ' + dirname

  for root, dirs, fnames in os.walk(dirname):
    for fname in fnames:
      if fname.endswith('.html'):
        infile = join(root, fname)
        print 'Converting ' + infile
        htmlconverter.convertForOffline(infile, infile, False)

if __name__ == '__main__':
  main()
