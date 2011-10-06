#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
'''
This script finds all HTML pages in a folder and downloads all images, replacing
the urls with local ones.
'''
import optparse
import os
from os.path import abspath, basename, dirname, join
import sys

SWARM_PATH = dirname(abspath(__file__))
CLIENT_PATH = dirname(dirname(SWARM_PATH))
CLIENT_TOOLS_PATH = join(CLIENT_PATH, 'tools')

# Add the client tools directory so we can find htmlconverter.py.
sys.path.append(CLIENT_TOOLS_PATH)
import htmlconverter

def Flags():
  """ Constructs a parser for extracting flags from the command line. """
  parser = optparse.OptionParser()
  parser.add_option("--inline_images",
      help=("Encode img payloads as data:// URLs rather than local files."),
      default=False,
      action='store_true')
  parser.add_option("--verbose",
      help="Print verbose output",
      default=False,
      action="store_true")
  return parser

def main():
  parser = Flags()
  options, args = parser.parse_args()
  print "args: %s" % args
  if len(args) < 1 or 'help' in args[0]:
    print 'Usage: %s DIRECTORY' % basename(sys.argv[0])
    return 1

  dirname = args[0]
  print 'Searching directory ' + dirname

  for root, dirs, fnames in os.walk(dirname):
    for fname in fnames:
      if fname.endswith('.html'):
        infile = join(root, fname)
        print 'Converting ' + infile
        htmlconverter.convertForOffline(
            infile, infile,
            verbose = options.verbose,
            encode_images = options.inline_images)

if __name__ == '__main__':
  main()
